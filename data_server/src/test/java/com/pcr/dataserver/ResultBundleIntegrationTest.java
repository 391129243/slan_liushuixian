package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:result-bundles;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "dataserver.storage=./target/bundle-test-files","dataserver.admin-password=12345678","dataserver.max-file-bytes=1048576"})
@Import(PcrAnalysisHistoryIntegrationTest.Configuration.class)
class ResultBundleIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceService devices;
    @Autowired PcrAnalysisHistoryIntegrationTest.RecordingGateway sdk;
    @BeforeEach void reset() {
        jdbc.update("UPDATE pcr_read_sessions SET parent_analysis_id=NULL");jdbc.update("UPDATE analysis_runs SET parent_analysis_id=NULL");
        jdbc.update("DELETE FROM analysis_runs");jdbc.update("DELETE FROM pcr_read_sessions");
        jdbc.update("UPDATE rule_versions SET parent_version_id=NULL");jdbc.update("DELETE FROM rule_versions");
        jdbc.update("UPDATE reports SET bundle_id=NULL");jdbc.update("DELETE FROM result_bundle_attachments");jdbc.update("DELETE FROM result_bundles");
        jdbc.update("DELETE FROM reports");jdbc.update("DELETE FROM uploaded_files");jdbc.update("DELETE FROM devices");sdk.reset();
    }
    TestRestTemplate admin() { return http.withBasicAuth("admin","12345678"); }
    String experiment() { return "{\"Name\":\"规则快照实验\",\"SerialNo\":\"PCR-BUNDLE\",\"Operator\":\"设备人员\",\"WellResult\":[{\"ID\":\"A1\",\"TargetResults\":[{\"Ct\":27.1}]}]}"; }
    Map<String,byte[]> files() {
        var files=new LinkedHashMap<String,byte[]>();
        files.put("source.pcr","EXPLICIT-FAKE-PCR-FIXTURE".getBytes(StandardCharsets.UTF_8));
        files.put("判读.ct",AndroidRuleEvaluatorTest.CT.getBytes(StandardCharsets.UTF_8));
        files.put("first.peak",AndroidRuleEvaluatorTest.PEAK.replace("HEX","FAM").getBytes(StandardCharsets.UTF_8));
        files.put("first.conclusion","original conclusion configuration".getBytes(StandardCharsets.UTF_8));
        files.put("judgeConclusion.result","{\"original\":true}".getBytes(StandardCharsets.UTF_8));return files;
    }
    ObjectNode manifest(Map<String,byte[]> files) {
        ObjectNode node=mapper.createObjectNode().put("version",1);var list=node.putArray("files");
        files.forEach((name,bytes)->list.addObject().put("fileName",name).put("role",name.endsWith(".pcr")?"PCR":name.endsWith(".ct")?"CT_RULE":name.endsWith(".peak")?"PEAK_RULE":name.endsWith(".conclusion")?"CONCLUSION_ARCHIVE":"DEVICE_CONCLUSION").put("size",bytes.length).put("sha256",Digests.hash("SHA-256",bytes)));
        return node;
    }
    HttpEntity<MultiValueMap<String,Object>> request(Map<String,byte[]> files,ObjectNode manifest,HttpHeaders headers) {
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();form.add("experiment",experiment());form.add("manifest",manifest.toString());
        files.forEach((name,bytes)->form.add(name.endsWith(".pcr")?"file":"attachments",new ByteArrayResource(bytes){@Override public String getFilename(){return name;}}));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);return new HttpEntity<>(form,headers);
    }
    ResponseEntity<JsonNode> upload(Map<String,byte[]> files) { return http.postForEntity("/upload",request(files,manifest(files),new HttpHeaders()),JsonNode.class); }
    JsonNode ok(ResponseEntity<JsonNode> response) { assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);return response.getBody(); }
    JsonNode get(String path) { return ok(admin().getForEntity("/api/admin/"+path,JsonNode.class)); }
    JsonNode post(String path,JsonNode body) { return ok(admin().postForEntity("/api/admin/pcr/"+path,body,JsonNode.class)); }

    @Test void wholeBundleIsIdempotentDownloadableAndFrozenIntoAnalysis() throws Exception {
        var inputs=files();var reply=ok(upload(inputs));String bundle=reply.path("BundleID").asText(),report=reply.path("ReportID").asText(),file=reply.path("FileID").asText();
        assertThat(reply.path("RuleFileCount").asInt()).isEqualTo(2);assertThat(reply.path("ConclusionFileCount").asInt()).isEqualTo(2);
        assertThat(ok(upload(inputs)).path("BundleID").asText()).isEqualTo(bundle);
        assertThat(get("reports/"+report).path("bundle_id").asText()).isEqualTo(bundle);
        assertThat(get("bundles/"+bundle).path("manifest").path("files").size()).isEqualTo(5);
        assertThat(http.getForEntity("/api/admin/bundles/"+bundle+"/download",byte[].class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var response=admin().getForEntity("/api/admin/bundles/"+bundle+"/download",byte[].class);assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String,byte[]> zip=new HashMap<>();try(var stream=new ZipInputStream(new ByteArrayInputStream(response.getBody()),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=stream.getNextEntry())!=null)zip.put(entry.getName(),stream.readAllBytes());}
        assertThat(zip).hasSize(7);inputs.forEach((name,bytes)->assertThat(zip.get(name)).isEqualTo(bytes));
        assertThat(new String(zip.get("experiment.json"),StandardCharsets.UTF_8)).isEqualTo(experiment());
        JsonNode read=post("read",mapper.createObjectNode().put("fileId",file).put("reportId",report));
        assertThat(sdk.lastRead.attachments()).hasSize(4);
        for(var attachment:sdk.lastRead.attachments())assertThat(Files.readAllBytes(Path.of(attachment.sourcePath()))).isEqualTo(inputs.get(attachment.fileName()));
        var params=mapper.createObjectNode().put("autoBaseline",true).put("baselineStart",6).put("baselineEnd",12).put("threshold",0.24);
        var body=mapper.createObjectNode().put("experimentId",read.path("experimentId").asText());body.set("parameters",params);
        var analyzed=post("reanalyze",body);String analysis=analyzed.path("analysisRunId").asText();
        assertThat(get("pcr/analyses/"+analysis).path("ruleSnapshot").path("bundleId").asText()).isEqualTo(bundle);
        assertThat(analyzed.path("ruleContext").path("evaluationStatus").asText()).isEqualTo("ARCHIVED_NOT_EVALUATED");
        assertThat(analyzed.path("applicationRuleEvaluation").path("status").asText()).isEqualTo("EVALUATED");
        assertThat(analyzed.path("applicationRuleEvaluation").path("rows").findValuesAsText("status")).containsExactly("POSITIVE","NEGATIVE");
        assertThat(get("pcr/analyses/"+analysis).path("result").path("applicationRuleEvaluation")).isEqualTo(analyzed.path("applicationRuleEvaluation"));
        var saved=post("save",mapper.createObjectNode().put("analysisId",analysis).put("fileName","child-source.pcr"));
        assertThat(admin().postForEntity("/api/admin/pcr/read",mapper.createObjectNode().put("fileId",saved.path("FileID").asText()),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var exported=admin().getForEntity("/api/admin/pcr/analyses/"+analysis+"/bundle",byte[].class);
        assertThat(exported.getStatusCode()).isEqualTo(HttpStatus.OK);
        var exportFiles=new HashMap<String,byte[]>();try(var stream=new ZipInputStream(new ByteArrayInputStream(exported.getBody()),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=stream.getNextEntry())!=null)exportFiles.put(entry.getName(),stream.readAllBytes());}
        assertThat(exportFiles.get("child-source.pcr")).isEqualTo(sdk.output);
        for(var entry:inputs.entrySet())if(!entry.getKey().endsWith(".pcr"))assertThat(exportFiles.get(entry.getKey())).isEqualTo(entry.getValue());
        assertThat(mapper.readTree(exportFiles.get("manifest.json")).path("applicationRulesApplied").asBoolean()).isTrue();
        assertThat(mapper.readTree(exportFiles.get("application-conclusions.json"))).isEqualTo(analyzed.path("applicationRuleEvaluation"));
        var childRead=post("read",mapper.createObjectNode().put("fileId",saved.path("FileID").asText()).put("parentAnalysisId",analysis));
        assertThat(childRead.path("ruleContext").path("bundleId").asText()).isEqualTo(bundle);
        assertThat(sdk.lastRead.attachments()).hasSize(4);
        inputs.put("判读.ct","[{\"CTvalue\":30}]".getBytes(StandardCharsets.UTF_8));var changed=ok(upload(inputs));
        assertThat(changed.path("BundleID").asText()).isNotEqualTo(bundle);assertThat(changed.path("ReportID").asText()).isNotEqualTo(report);
        assertThat(get("pcr/analyses/"+analysis).path("ruleSnapshot").path("bundleId").asText()).isEqualTo(bundle);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files",Long.class)).isEqualTo(2);
    }
    @Test void invalidHashMissingFileUnsafeFilenameAndOversizedRuleNeverCreatePartialReports() {
        var inputs=files();var bad=manifest(inputs);((ObjectNode)bad.path("files").get(1)).put("sha256","0".repeat(64));
        assertThat(http.postForEntity("/upload",request(inputs,bad,new HttpHeaders()),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var incomplete=manifest(inputs);inputs.remove("判读.ct");
        assertThat(http.postForEntity("/upload",request(inputs,incomplete,new HttpHeaders()),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        inputs=files();inputs.put("../escape.ct","{}".getBytes(StandardCharsets.UTF_8));
        assertThat(upload(inputs).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        inputs=files();inputs.put("large.ct",new byte[ResultBundleService.MAX_ATTACHMENT_BYTES+1]);
        assertThat(upload(inputs).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM result_bundles",Long.class)).isZero();
    }
    @Test void editedRulesAreVersionedCombinedExportedInheritedAndPartOfIdempotency() throws Exception {
        var uploaded=ok(upload(files()));String bundle=uploaded.path("BundleID").asText(),file=uploaded.path("FileID").asText();
        var create=mapper.createObjectNode().put("baseBundleId",bundle).put("label","复算规则 v1");
        var draftFiles=create.putArray("files");draftFiles.addObject().put("fileName","edited.ct").set("rules",mapper.readTree(AndroidRuleEvaluatorTest.CT.replace("\"CTvalue\":35","\"CTvalue\":28").replace("\"CTvalueDown\":5","\"CTvalueDown\":28")));
        draftFiles.addObject().put("fileName","edited.peak").set("rules",mapper.readTree(AndroidRuleEvaluatorTest.PEAK.replace("HEX","FAM")));
        var policies=create.putArray("combinationRules");
        for(String mode:List.of("ALL","ANY")) {
            var p=policies.addObject().put("name",mode+" 综合项目").put("mode",mode);
            p.putObject("ct").put("fileName","edited.ct").put("ruleIndex",0);p.putObject("peak").put("fileName","edited.peak").put("ruleIndex",0);
        }
        var version=post("rules",create);String versionId=version.path("id").asText();
        assertThat(version.path("created_by").asText()).isEqualTo("admin");
        var loaded=post("read",mapper.createObjectNode().put("fileId",file).put("reportId",uploaded.path("ReportID").asText()));
        var body=mapper.createObjectNode().put("experimentId",loaded.path("experimentId").asText()).put("ruleVersionId",versionId).put("requestId","edited-rules-request");
        body.set("parameters",mapper.createObjectNode().put("autoBaseline",true).put("baselineStart",6).put("baselineEnd",12).put("threshold",0.24));
        var analyzed=post("reanalyze",body);String id=analyzed.path("analysisRunId").asText();
        assertThat(analyzed.path("combinedConclusions").path("rows").findValuesAsText("status")).containsExactly("POSITIVE","POSITIVE");
        assertThat(analyzed.path("ruleVersionId").asText()).isEqualTo(versionId);
        assertThat(post("reanalyze",body).path("analysisRunId").asText()).isEqualTo(id);
        var original=body.deepCopy().put("ruleVersionId","ORIGINAL");
        assertThat(admin().postForEntity("/api/admin/pcr/reanalyze",original,JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var saved=post("save",mapper.createObjectNode().put("analysisId",id).put("fileName","versioned.pcr"));
        var child=post("read",mapper.createObjectNode().put("fileId",saved.path("FileID").asText()).put("parentAnalysisId",id));
        assertThat(child.path("ruleVersionId").asText()).isEqualTo(versionId);
        assertThat(sdk.lastRead.attachments().stream().map(PcrAnalysisGateway.RuleAttachment::fileName).toList()).contains("edited.ct","edited.peak").doesNotContain("判读.ct");
        var zipBytes=admin().getForEntity("/api/admin/pcr/analyses/"+id+"/bundle",byte[].class).getBody();var zip=new HashMap<String,byte[]>();
        try(var stream=new ZipInputStream(new ByteArrayInputStream(zipBytes),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=stream.getNextEntry())!=null)zip.put(entry.getName(),stream.readAllBytes());}
        assertThat(zip).containsKeys("edited.ct","edited.peak","rule-version.json","combined-conclusions.json","original-attachments/判读.ct","original-attachments/judgeConclusion.result");
        assertThat(mapper.readTree(zip.get("combined-conclusions.json"))).isEqualTo(analyzed.path("combinedConclusions"));
        var edited=create.deepCopy().put("parentVersionId",versionId).put("label","复算规则 v2");
        ((ObjectNode)edited.path("files").get(0).path("rules").get(0)).put("CTvalue",1).put("CTvalueDown",1).put("CTvalueUP",1);
        var second=post("rules",edited);assertThat(second.path("id").asText()).isNotEqualTo(versionId);
        var secondBody=body.deepCopy().put("ruleVersionId",second.path("id").asText()).put("requestId","second-edited-request");
        assertThat(post("reanalyze",secondBody).path("combinedConclusions").path("rows").findValuesAsText("status")).containsExactly("NEGATIVE","POSITIVE");
        assertThat(get("pcr/analyses/"+id).path("result").path("combinedConclusions")).isEqualTo(analyzed.path("combinedConclusions"));
        ((ObjectNode)edited.path("combinationRules").get(0).path("ct")).put("ruleIndex",99);
        assertThat(admin().postForEntity("/api/admin/pcr/rules",edited,JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("bundles/"+bundle).path("manifest").path("files").toString()).contains("判读.ct").doesNotContain("edited.ct");
    }
    @Test void provisionedDevicesRequireCredentialsAndAdminIdentityCannotBeSpoofed() {
        var device=devices.create(new DeviceService.CreateDevice("PCR-BUNDLE","PCR",0,"secure"));var inputs=files();
        assertThat(upload(inputs).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        HttpHeaders headers=new HttpHeaders();headers.set("X-Device-Id","PCR-BUNDLE");headers.set("X-Device-Key",device.get("deviceKey"));
        var reply=ok(http.postForEntity("/api/device/bundles",request(inputs,manifest(inputs),headers),JsonNode.class));
        devices.enabled("PCR-BUNDLE",false);
        assertThat(http.postForEntity("/api/device/bundles",request(inputs,manifest(inputs),headers),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        devices.enabled("PCR-BUNDLE",true);inputs.put("first.ct","new".getBytes(StandardCharsets.UTF_8));
        var adminReply=ok(admin().postForEntity("/api/admin/bundles",request(inputs,manifest(inputs),new HttpHeaders()),JsonNode.class));
        var row=get("reports/"+adminReply.path("ReportID").asText());
        assertThat(row.path("uploaded_by_username").asText()).isEqualTo("admin");assertThat(row.path("uploaded_by_device_id").isNull()).isTrue();
        assertThat(reply.path("BundleID").asText()).isNotEqualTo(adminReply.path("BundleID").asText());
    }
    @Test void databaseFailureRollsBackEveryBundleMetadataRowAndAllowsRetry() {
        jdbc.execute("ALTER TABLE result_bundle_attachments ADD CONSTRAINT fixture_refuse_bundle CHECK (file_size<1)");
        try {
            assertThat(upload(files()).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            for(String table:List.of("reports","uploaded_files","result_bundles","result_bundle_attachments","devices"))assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class)).isZero();
        } finally { jdbc.execute("ALTER TABLE result_bundle_attachments DROP CONSTRAINT fixture_refuse_bundle"); }
        ok(upload(files()));
    }
    @Test void concurrentIdenticalBundlesProduceOneReportAndOneRuleSnapshot() throws Exception {
        // Register before the parallel requests so this test focuses on locked bundle idempotency.
        devices.registerLegacy("PCR-BUNDLE");
        try(var executor=Executors.newFixedThreadPool(4)) {
            List<Callable<String>> jobs=new ArrayList<>();for(int i=0;i<6;i++)jobs.add(()->ok(upload(files())).path("BundleID").asText());
            Set<String> ids=new HashSet<>();for(var future:executor.invokeAll(jobs))ids.add(future.get());assertThat(ids).hasSize(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM result_bundle_attachments",Long.class)).isEqualTo(4);
    }
}
