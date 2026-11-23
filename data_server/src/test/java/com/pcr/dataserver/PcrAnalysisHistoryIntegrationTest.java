package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:analysis-history;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "dataserver.storage=./target/history-test-files","dataserver.admin-password=12345678"})
class PcrAnalysisHistoryIntegrationTest {
    @TestConfiguration static class Configuration {
        @Bean @Primary RecordingGateway recordingGateway(ObjectMapper mapper) { return new RecordingGateway(mapper); }
    }
    static class RecordingGateway implements PcrAnalysisGateway {
        final ObjectMapper mapper;
        final AtomicInteger analyses=new AtomicInteger(),saves=new AtomicInteger(),reads=new AtomicInteger();
        volatile boolean failAnalysis,badChecksum;
        volatile CountDownLatch entered,release;
        volatile SourceFile lastRead;
        final byte[] output="FAKE-SDK-SAVED-PCR-HISTORY-FIXTURE".getBytes(StandardCharsets.UTF_8);
        RecordingGateway(ObjectMapper mapper) { this.mapper=mapper; }
        String id() { return UUID.randomUUID().toString().replace("-",""); }
        public JsonNode capabilities() { return mapper.createObjectNode().put("sdkReady",true).put("vendorVersion","9.0.1.fixture"); }
        public JsonNode read(SourceFile source) {
            lastRead=source;
            reads.incrementAndGet();
            try {
                return mapper.createObjectNode().put("experimentId",id()).put("sourceSha256",Digests.hash("SHA-256",Files.readAllBytes(Path.of(source.sourcePath()))))
                    .set("experiment",mapper.createObjectNode().put("Name","原始实验"));
            } catch(Exception e) { throw new IllegalStateException(e); }
        }
        public JsonNode reanalyze(Reanalyze request) {
            analyses.incrementAndGet();
            if(entered!=null) {
                entered.countDown();
                try { if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Fixture wait timed out"); }
                catch(InterruptedException error) { Thread.currentThread().interrupt();throw new IllegalStateException(error); }
            }
            if(failAnalysis) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Fixture SDK unavailable");
            ObjectNode result=mapper.createObjectNode().put("analysisId",id()).put("experimentId",request.experimentId()).put("mock",false);
            var experiment=result.putObject("experiment").put("Name","二次分析结果");
            var target=experiment.putArray("WellResult").addObject().put("ID","A1").putArray("TargetResults").addObject().put("Name","FAM").put("Ct",28.23456789012345);
            target.putArray("PeakValues").addObject().put("Tm",80).put("Rm",500);
            target.putArray("AMPCurve").addObject().put("X",1).put("Y",123.456);
            target.putObject("VendorExtra").put("preserved",true);
            return result;
        }
        public JsonNode save(Save request) {
            saves.incrementAndGet();
            return mapper.createObjectNode().put("outputId",id()).put("sha256",badChecksum?"invalid":Digests.hash("SHA-256",output));
        }
        public byte[] download(String id) { return output; }
        void reset() { analyses.set(0);saves.set(0);reads.set(0);failAnalysis=false;badChecksum=false;entered=null;release=null;lastRead=null; }
    }
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired RecordingGateway sdk;
    @BeforeEach void reset() {
        jdbc.update("UPDATE pcr_read_sessions SET parent_analysis_id=NULL");
        jdbc.update("UPDATE analysis_runs SET parent_analysis_id=NULL");
        jdbc.update("DELETE FROM analysis_runs");jdbc.update("DELETE FROM pcr_read_sessions");
        jdbc.update("DELETE FROM reports");jdbc.update("DELETE FROM uploaded_files");jdbc.update("DELETE FROM devices");sdk.reset();
    }
    TestRestTemplate admin() { return http.withBasicAuth("admin","12345678"); }
    ResponseEntity<JsonNode> post(String path,JsonNode body) { return admin().postForEntity("/api/admin/pcr/"+path,body,JsonNode.class); }
    JsonNode ok(ResponseEntity<JsonNode> response) { assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);return response.getBody(); }
    JsonNode get(String path) { return ok(admin().getForEntity("/api/admin/pcr/"+path,JsonNode.class)); }
    ObjectNode parameters() { return mapper.createObjectNode().put("autoBaseline",false).put("baselineStart",3).put("baselineEnd",15).put("threshold",500.0); }
    ObjectNode analyzeBody(String session,String requestId) {
        ObjectNode body=mapper.createObjectNode().put("experimentId",session).put("requestId",requestId);
        body.set("parameters",parameters());return body;
    }
    String file(String name) {
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();
        form.add("file",new ByteArrayResource(("FAKE-INPUT-PCR:"+name).getBytes(StandardCharsets.UTF_8)) { @Override public String getFilename() { return name; } });
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return ok(http.postForEntity("/upload",new HttpEntity<>(form,headers),JsonNode.class)).path("FileID").asText();
    }
    String report(String file,String name) {
        var body=mapper.createObjectNode().put("Name",name).put("SerialNo","PCR-HISTORY").put("Operator","原始设备人员");body.putArray("WellResult");
        String id=ok(http.postForEntity("/upload",body,JsonNode.class)).path("ReportID").asText();
        assertThat(admin().exchange("/api/admin/reports/"+id+"/file/"+file,HttpMethod.PUT,HttpEntity.EMPTY,Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);return id;
    }
    JsonNode read(String file) { return ok(post("read",mapper.createObjectNode().put("fileId",file))); }
    JsonNode analyze(String session,String key) { return ok(post("reanalyze",analyzeBody(session,key))); }
    JsonNode save(String run,String name) { return ok(post("save",mapper.createObjectNode().put("analysisId",run).put("fileName",name))); }

    @Test void persistsResultBeforeFileSaveAndKeepsOriginalReportIntact() {
        String file=file("source.pcr"),report=report(file,"原始实验");
        String original=jdbc.queryForObject("SELECT payload_json FROM reports WHERE id=?",String.class,report);
        JsonNode loaded=read(file);assertThat(loaded.path("reportId").asText()).isEqualTo(report);
        var body=analyzeBody(loaded.path("experimentId").asText(),"client-request-1").put("operator_username","forged");
        JsonNode analyzed=ok(post("reanalyze",body));String id=analyzed.path("analysisRunId").asText();
        JsonNode detail=get("analyses/"+id);
        assertThat(detail.path("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(detail.path("file_save_status").asText()).isEqualTo("NOT_SAVED");
        assertThat(detail.path("output_file_id").isNull()).isTrue();
        assertThat(detail.path("operator_username").asText()).isEqualTo("admin");
        assertThat(detail.path("input_file_id").asText()).isEqualTo(file);
        assertThat(detail.path("report_id").asText()).isEqualTo(report);
        assertThat(detail.path("sdk_version").asText()).isEqualTo("9.0.1.fixture");
        assertThat(detail.path("parameters")).isEqualTo(parameters());
        assertThat(detail.path("result").path("experiment")).isEqualTo(analyzed.path("experiment"));
        assertThat(get("analyses/"+id+"/result").path("experiment")).isEqualTo(analyzed.path("experiment"));
        JsonNode listing=get("analyses?reportId="+report);assertThat(listing.path("total").asInt()).isEqualTo(1);
        assertThat(listing.path("items").get(0).has("result_json")).isFalse();
        JsonNode saved=save(id,"result.pcr");
        assertThat(get("analyses/"+id).path("output_file_id").asText()).isEqualTo(saved.path("FileID").asText());
        assertThat(get("analyses/"+id).path("file_save_status").asText()).isEqualTo("SAVED");
        assertThat(get("analyses/"+id).path("saved_by_username").asText()).isEqualTo("admin");
        var outputMetadata=jdbc.queryForMap("SELECT uploaded_by_username,upload_source FROM uploaded_files WHERE id=?",saved.path("FileID").asText());
        assertThat(outputMetadata.get("uploaded_by_username")).isEqualTo("admin");
        assertThat(outputMetadata.get("upload_source")).isEqualTo("SDK_ANALYSIS");
        JsonNode retry=save(analyzed.path("analysisId").asText(),"result.pcr");
        assertThat(retry.path("FileID")).isEqualTo(saved.path("FileID"));assertThat(retry.path("Duplicate").asBoolean()).isTrue();assertThat(sdk.saves.get()).isEqualTo(1);
        assertThat(post("save",mapper.createObjectNode().put("analysisId",id).put("fileName","different.pcr")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT payload_json FROM reports WHERE id=?",String.class,report)).isEqualTo(original);
        assertThat(jdbc.queryForObject("SELECT file_id FROM reports WHERE id=?",String.class,report)).isEqualTo(file);
    }
    @Test void failedAnalysisRemainsQueryableAndDoesNotInventAResult() {
        String session=read(file("failure.pcr")).path("experimentId").asText();sdk.failAnalysis=true;
        assertThat(post("reanalyze",analyzeBody(session,"failed-request")).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        JsonNode item=get("analyses").path("items").get(0);String id=item.path("id").asText();
        assertThat(item.path("status").asText()).isEqualTo("FAILED");
        assertThat(item.path("error_message").asText()).isEqualTo("Fixture SDK unavailable");
        assertThat(get("analyses/"+id).path("result").isNull()).isTrue();
        assertThat(admin().getForEntity("/api/admin/pcr/analyses/"+id+"/result",JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("reanalyze",analyzeBody(session,"failed-request")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sdk.analyses.get()).isEqualTo(1);
    }
    @Test void checksumFailureKeepsAnalysisJsonAndCanRetrySaving() {
        JsonNode analyzed=analyze(read(file("checksum.pcr")).path("experimentId").asText(),"checksum-request");String id=analyzed.path("analysisRunId").asText();
        sdk.badChecksum=true;
        assertThat(post("save",mapper.createObjectNode().put("analysisId",id).put("fileName","output.pcr")).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        JsonNode detail=get("analyses/"+id);
        assertThat(detail.path("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(detail.path("file_save_status").asText()).isEqualTo("FAILED");assertThat(detail.path("output_file_id").isNull()).isTrue();
        assertThat(detail.path("result").path("experiment")).isEqualTo(analyzed.path("experiment"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files",Long.class)).isEqualTo(1);
        sdk.badChecksum=false;save(id,"output.pcr");assertThat(get("analyses/"+id).path("file_save_status").asText()).isEqualTo("SAVED");
    }
    @Test void databaseFailureRollsBackFileMetadataAndAnalysisLinkTogether() {
        JsonNode analyzed=analyze(read(file("transaction.pcr")).path("experimentId").asText(),"transaction-request");
        String id=analyzed.path("analysisRunId").asText();
        jdbc.execute("ALTER TABLE analysis_runs ADD CONSTRAINT fixture_refuse_saved_output CHECK (output_file_id IS NULL)");
        try {
            assertThat(post("save",mapper.createObjectNode().put("analysisId",id).put("fileName","transaction-output.pcr")).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files",Long.class)).isEqualTo(1);
            JsonNode detail=get("analyses/"+id);assertThat(detail.path("output_file_id").isNull()).isTrue();
            assertThat(detail.path("file_save_status").asText()).isEqualTo("FAILED");
            assertThat(detail.path("result").path("experiment")).isEqualTo(analyzed.path("experiment"));
        } finally { jdbc.execute("ALTER TABLE analysis_runs DROP CONSTRAINT fixture_refuse_saved_output"); }
        save(id,"transaction-output.pcr");assertThat(get("analyses/"+id).path("file_save_status").asText()).isEqualTo("SAVED");
    }
    @Test void lineageUsesSavedParentOutputAndInheritsReportWithoutOverwritingIt() {
        String input=file("parent.pcr"),report=report(input,"parent");
        JsonNode parent=analyze(read(input).path("experimentId").asText(),"parent-request");String parentId=parent.path("analysisRunId").asText();
        String output=save(parentId,"parent-output.pcr").path("FileID").asText();
        JsonNode childRead=ok(post("read",mapper.createObjectNode().put("fileId",output).put("parentAnalysisId",parentId)));
        JsonNode child=analyze(childRead.path("experimentId").asText(),"child-request");
        JsonNode childDetail=get("analyses/"+child.path("analysisRunId").asText());
        assertThat(childDetail.path("parent_analysis_id").asText()).isEqualTo(parentId);
        assertThat(childDetail.path("report_id").asText()).isEqualTo(report);
        assertThat(childDetail.path("input_file_id").asText()).isEqualTo(output);
        assertThat(get("analyses?reportId="+report).path("total").asInt()).isEqualTo(2);
        assertThat(post("read",mapper.createObjectNode().put("fileId",input).put("parentAnalysisId",parentId)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT file_id FROM reports WHERE id=?",String.class,report)).isEqualTo(input);
    }
    @Test void ambiguousReportAndForeignReadSessionAreRejectedBeforeSdkAnalysis() {
        String input=file("ambiguous.pcr"),first=report(input,"first");report(input,"second");
        assertThat(post("read",mapper.createObjectNode().put("fileId",input)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);assertThat(sdk.reads.get()).isZero();
        JsonNode loaded=ok(post("read",mapper.createObjectNode().put("fileId",input).put("reportId",first)));
        jdbc.update("UPDATE pcr_read_sessions SET operator_username='another-user'");
        assertThat(post("reanalyze",analyzeBody(loaded.path("experimentId").asText(),"foreign-session")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);assertThat(sdk.analyses.get()).isZero();
        assertThat(http.getForEntity("/api/admin/pcr/analyses",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(admin().getForEntity("/api/admin/pcr/analyses?limit=101",String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test void concurrentRequestIdDoesNotRunSdkTwiceAndCompletedRetryReturnsStoredResult() throws Exception {
        String session=read(file("concurrent.pcr")).path("experimentId").asText();var body=analyzeBody(session,"same-request");
        sdk.entered=new CountDownLatch(1);sdk.release=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var pending=executor.submit(()->post("reanalyze",body));
            try {
                assertThat(sdk.entered.await(5,TimeUnit.SECONDS)).isTrue();
                assertThat(post("reanalyze",body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            } finally { sdk.release.countDown(); }
            JsonNode first=ok(pending.get(5,TimeUnit.SECONDS)),retry=ok(post("reanalyze",body));
            assertThat(retry.path("analysisRunId")).isEqualTo(first.path("analysisRunId"));
            assertThat(sdk.analyses.get()).isEqualTo(1);assertThat(get("analyses").path("total").asInt()).isEqualTo(1);
            ((ObjectNode)body.path("parameters")).put("threshold",600);
            assertThat(post("reanalyze",body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
    @Test void fractionalThresholdIsPersistedAndNonPositiveOrNonFiniteValuesAreRejected() {
        String session=read(file("fractional.pcr")).path("experimentId").asText();
        var valid=analyzeBody(session,"fractional-request");((ObjectNode)valid.path("parameters")).put("threshold",0.24);
        String id=ok(post("reanalyze",valid)).path("analysisRunId").asText();
        assertThat(get("analyses/"+id).path("parameters").path("threshold").asDouble()).isEqualTo(0.24);
        for(double threshold:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            var invalid=analyzeBody(session,"invalid-threshold");((ObjectNode)invalid.path("parameters")).put("threshold",threshold);
            assertThat(post("reanalyze",invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(sdk.analyses.get()).isEqualTo(1);
    }
}
