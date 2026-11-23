package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:http-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "dataserver.storage=./target/test-files", "dataserver.admin-password=12345678",
    "dataserver.max-json-bytes=4096", "dataserver.max-file-bytes=1024"})
class HttpUploadIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceService devices;
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM analysis_runs"); jdbc.update("DELETE FROM pcr_read_sessions");
        jdbc.update("DELETE FROM reports"); jdbc.update("DELETE FROM uploaded_files"); jdbc.update("DELETE FROM devices");
    }
    ObjectNode experiment(String serial) {
        var body=mapper.createObjectNode().put("Name","中文实验").put("SerialNo",serial).put("StartTime","2026-11-16 10:00:00");
        var well=body.putArray("WellResult").addObject().put("ID","S1");
        well.putArray("SampleWellNames").add("A1");
        var target=well.putArray("TargetResults").addObject().put("Name","FAM").put("Ct",28.5);
        target.putArray("RawCurve").addObject().put("X",1).put("Y",123.0);
        body.putObject("VendorFutureField").put("keep",true);
        return body;
    }
    ResponseEntity<JsonNode> post(String path,String body,HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity(path,new HttpEntity<>(body.getBytes(StandardCharsets.UTF_8),headers),JsonNode.class);
    }
    ResponseEntity<JsonNode> post(String body) { return post("/upload",body,new HttpHeaders()); }
    TestRestTemplate admin() { return http.withBasicAuth("admin","12345678"); }
    String id(ResponseEntity<JsonNode> response,String field) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("Result").asText()).isEqualTo("1");
        return response.getBody().path(field).asText();
    }
    ResponseEntity<JsonNode> multipart(String filename,byte[] bytes) {
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();
        form.add("file",new ByteArrayResource(bytes) { @Override public String getFilename() { return filename; } });
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.postForEntity("/upload",new HttpEntity<>(form,headers),JsonNode.class);
    }

    @Test void acceptsActualGsonShapePreservesCurvesAndOriginalJson() throws Exception {
        String raw=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(experiment("PCR-001").put("Operator","实验员甲"));
        String report=id(post(raw),"ReportID");
        var detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
        assertThat(detail.path("Experiment")).isEqualTo(mapper.readTree(raw));
        assertThat(detail.path("well_count").asInt()).isEqualTo(1);
        assertThat(detail.path("target_count").asInt()).isEqualTo(1);
        assertThat(detail.path("analysis_status").asText()).isEqualTo("DEVICE_RESULT_RECEIVED");
        assertThat(detail.path("operator_name").asText()).isEqualTo("实验员甲");
        assertThat(detail.path("operator_user_id").isNull()).isTrue();
        assertThat(detail.path("upload_source").asText()).isEqualTo("LEGACY_HTTP");
        assertThat(detail.path("uploaded_by_device_id").isNull()).isTrue();
        var listed=admin().getForObject("/api/admin/reports?serial=PCR-001",JsonNode.class).path("items").get(0);
        assertThat(listed.path("operator_name").asText()).isEqualTo("实验员甲");
        byte[] source=admin().getForObject("/api/admin/reports/"+report+"/source",byte[].class);
        assertThat(new String(source,StandardCharsets.UTF_8)).isEqualTo(raw);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM devices",Long.class)).isEqualTo(1);
    }
    @Test void authenticatedDeviceCannotClaimPlatformOperatorOrUploadIdentity() {
        var created=devices.create(new DeviceService.CreateDevice("PCR-OPERATOR","PCR",1,"operator test"));
        var body=experiment("PCR-OPERATOR").put("Operator","设备填写的姓名")
            .put("operator_user_id","forged-user").put("upload_source","USER")
            .put("uploaded_by_device_id","OTHER");
        HttpHeaders headers=new HttpHeaders();
        headers.set("X-Device-Id","PCR-OPERATOR"); headers.set("X-Device-Key",created.get("deviceKey"));
        String report=id(post("/api/device/reports",body.toString(),headers),"ReportID");
        var detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
        assertThat(detail.path("operator_name").asText()).isEqualTo("设备填写的姓名");
        assertThat(detail.path("operator_user_id").isNull()).isTrue();
        assertThat(detail.path("upload_source").asText()).isEqualTo("DEVICE_AUTH");
        assertThat(detail.path("uploaded_by_device_id").asText()).isEqualTo("PCR-OPERATOR");
        assertThat(detail.path("Experiment")).isEqualTo(body);
        assertThat(id(post("/api/device/reports",body.toString(),headers),"ReportID")).isEqualTo(report);
    }
    @Test void operatorIsOptionalAndInvalidNamesAreRejected() {
        for(String serial:List.of("PCR-NO-OPERATOR","PCR-BLANK-OPERATOR","PCR-NULL-OPERATOR")) {
            var body=experiment(serial);
            if(serial.equals("PCR-BLANK-OPERATOR")) body.put("Operator","   ");
            if(serial.equals("PCR-NULL-OPERATOR")) body.putNull("Operator");
            String report=id(post(body.toString()),"ReportID");
            var detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
            assertThat(detail.path("operator_name").isNull()).isTrue();
            assertThat(detail.path("operator_user_id").isNull()).isTrue();
        }
        for(JsonNode value:List.of(mapper.getNodeFactory().numberNode(123),
                mapper.getNodeFactory().textNode("x".repeat(161)),mapper.getNodeFactory().textNode("invalid\nname"))) {
            var body=experiment("PCR-INVALID-OPERATOR"); body.set("Operator",value);
            assertThat(post(body.toString()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class)).isEqualTo(3);
    }
    @Test void retriesDeduplicateEvenWithFieldOrderAndNumberFormattingChanges() {
        var first=experiment("PCR-002");
        String report=id(post(first.toString()),"ReportID");
        var reordered=mapper.createObjectNode(); first.fields().forEachRemaining(e -> reordered.set(e.getKey(),e.getValue()));
        ((ObjectNode)reordered.path("WellResult").get(0).path("TargetResults").get(0).path("RawCurve").get(0)).put("Y",123);
        var reply=post(mapper.createObjectNode().setAll(reordered).toString());
        assertThat(id(reply,"ReportID")).isEqualTo(report);
        assertThat(reply.getBody().path("Duplicate").asBoolean()).isTrue();
        first.put("EndTime","2026-11-16 11:00:00");
        assertThat(id(post(first.toString()),"ReportID")).isNotEqualTo(report);
    }
    @Test void rejectsOldEnvelopeMalformedJsonAndMissingSerial() {
        for(String raw:List.of("[]","{bad","{\"Type\":\"SLANPCRFinish\"}","{\"Name\":\"x\",\"WellResult\":[]}",experiment("P").toString()+" {}")) {
            var response=post(raw);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().path("Result").asText()).isEqualTo("0");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class)).isZero();
    }
    @Test void boundsUploadsAndRejectsWrongContentType() {
        var large=experiment("P").put("large","x".repeat(5000));
        assertThat(post(large.toString()).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(multipart("large.pcr",new byte[1025]).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.TEXT_PLAIN);
        assertThat(http.postForEntity("/upload",new HttpEntity<>("text",headers),String.class).getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    @Test void secureProvisionedDevicesCannotBypassAuthenticationViaLegacyRoute() {
        var created=devices.create(new DeviceService.CreateDevice("PCR-SECURE","PCR",1,"secure"));
        String raw=experiment("PCR-SECURE").toString();
        assertThat(post(raw).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/device/reports",raw,new HttpHeaders()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders headers=new HttpHeaders(); headers.set("X-Device-Id","PCR-SECURE"); headers.set("X-Device-Key",created.get("deviceKey"));
        id(post("/api/device/reports",raw,headers),"ReportID");
        assertThat(post("/api/device/reports",experiment("OTHER").toString(),headers).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        devices.enabled("PCR-SECURE",false);
        assertThat(post("/api/device/reports",raw,headers).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void disabledLegacyDeviceDoesNotReceiveSuccess() {
        String raw=experiment("PCR-DISABLED").toString(); id(post(raw),"ReportID");
        devices.enabled("PCR-DISABLED",false);
        assertThat(post(raw).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    @Test void fileUploadsRemainUnassociatedUntilExplicitAdminLink() {
        byte[] bytes="fake-pcr-test-not-analysis".getBytes(StandardCharsets.UTF_8);
        String file=id(multipart("示例.pcr",bytes),"FileID");
        assertThat(id(multipart("示例.pcr",bytes),"FileID")).isEqualTo(file);
        String report=id(post(experiment("PCR-FILE").toString()),"ReportID");
        var detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
        assertThat(detail.path("file_id").isNull()).isTrue();
        assertThat(admin().exchange("/api/admin/reports/"+report+"/file/"+file,HttpMethod.PUT,HttpEntity.EMPTY,Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(admin().getForObject("/api/admin/files/"+file+"/download",byte[].class)).isEqualTo(bytes);
        detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
        assertThat(detail.path("analysis_status").asText()).isEqualTo("WAITING_SDK");
        assertThat(multipart("../escape.pcr",bytes).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(multipart("payload.exe",bytes).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test void adminEndpointsRequireAuthenticationAndPaginationIsBounded() {
        assertThat(http.getForEntity("/api/admin/reports",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.withBasicAuth("admin","wrong-password").getForEntity("/api/admin/reports",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(admin().getForEntity("/api/admin/reports?limit=1000",String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(admin().getForEntity("/api/admin/reports/nonexistent",String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test void administratorFileUploadRequiresLoginAndRecordsTrustedActor() {
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();
        form.add("file",new ByteArrayResource("admin-upload-fixture".getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "admin.pcr"; }
        });
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var request=new HttpEntity<>(form,headers);
        assertThat(http.postForEntity("/api/admin/files",request,JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String file=id(admin().postForEntity("/api/admin/files",request,JsonNode.class),"FileID");
        var metadata=jdbc.queryForMap("SELECT uploaded_by_username,upload_source FROM uploaded_files WHERE id=?",file);
        assertThat(metadata.get("uploaded_by_username")).isEqualTo("admin");
        assertThat(metadata.get("upload_source")).isEqualTo("ADMIN_UPLOAD");
        assertThat(id(admin().postForEntity("/api/admin/files",request,JsonNode.class),"FileID")).isEqualTo(file);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files",Long.class)).isEqualTo(1);
    }
    @Test void concurrentRetriesCreateOnlyOneDurableReport() throws Exception {
        String raw=experiment("PCR-CONCURRENT").toString();
        try(var executor=Executors.newFixedThreadPool(6)) {
            List<Callable<String>> requests=new ArrayList<>();
            for(int i=0;i<12;i++) requests.add(() -> id(post(raw),"ReportID"));
            Set<String> ids=new HashSet<>();
            for(Future<String> future:executor.invokeAll(requests)) ids.add(future.get());
            assertThat(ids).hasSize(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class)).isEqualTo(1);
    }
}
