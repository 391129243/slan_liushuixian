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
    "dataserver.storage=./target/test-files", "dataserver.admin-password=integration-test-password",
    "dataserver.max-json-bytes=4096", "dataserver.max-file-bytes=1024"})
class HttpUploadIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceService devices;
    @BeforeEach void reset() {
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
    TestRestTemplate admin() { return http.withBasicAuth("admin","integration-test-password"); }
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
        String raw=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(experiment("PCR-001"));
        String report=id(post(raw),"ReportID");
        var detail=admin().getForObject("/api/admin/reports/"+report,JsonNode.class);
        assertThat(detail.path("Experiment")).isEqualTo(mapper.readTree(raw));
        assertThat(detail.path("well_count").asInt()).isEqualTo(1);
        assertThat(detail.path("target_count").asInt()).isEqualTo(1);
        assertThat(detail.path("analysis_status").asText()).isEqualTo("DEVICE_RESULT_RECEIVED");
        byte[] source=admin().getForObject("/api/admin/reports/"+report+"/source",byte[].class);
        assertThat(new String(source,StandardCharsets.UTF_8)).isEqualTo(raw);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM devices",Long.class)).isEqualTo(1);
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
        assertThat(admin().getForEntity("/api/admin/reports?limit=1000",String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(admin().getForEntity("/api/admin/reports/nonexistent",String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
