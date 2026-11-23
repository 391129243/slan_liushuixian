package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleVersionService {
    public record DraftFile(String fileName,JsonNode rules) { }
    public record Create(String baseBundleId,String parentVersionId,String label,List<DraftFile> files,JsonNode combinationRules) { }
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final ResultBundleService bundles;
    private final FileStore store;private final AndroidRuleEvaluator evaluator;
    public RuleVersionService(JdbcTemplate jdbc,ObjectMapper mapper,ResultBundleService bundles,FileStore store,AndroidRuleEvaluator evaluator) {
        this.jdbc=jdbc;this.mapper=mapper;this.bundles=bundles;this.store=store;this.evaluator=evaluator;
    }
    public Map<String,Object> get(String id) {
        var rows=jdbc.queryForList("SELECT * FROM rule_versions WHERE id=?",id);
        if(rows.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"Rule version not found");
        var row=rows.getFirst();try{row.put("snapshot",mapper.readTree((String)row.remove("snapshot_json")));}catch(IOException e){throw new UncheckedIOException(e);}return row;
    }
    public List<Map<String,Object>> list(String bundleId) {
        return bundleId==null?jdbc.queryForList("SELECT id,label,parent_version_id,created_by,created_at FROM rule_versions WHERE base_bundle_id IS NULL ORDER BY created_at DESC"):
            jdbc.queryForList("SELECT id,label,parent_version_id,created_by,created_at FROM rule_versions WHERE base_bundle_id=? ORDER BY created_at DESC",bundleId);
    }
    public ObjectNode draft(String bundleId,String versionId) {
        if(versionId!=null)return (ObjectNode)((JsonNode)get(versionId).get("snapshot")).deepCopy();
        var result=mapper.createObjectNode().put("baseBundleId",bundleId);var files=result.putArray("files");result.putArray("combinationRules");
        if(bundleId!=null)for(var source:bundles.ruleSources(bundleId)) {
            var file=files.addObject().put("fileName",source.fileName()).put("role",source.role()).put("sha256",source.sha256());
            try{file.set("rules",mapper.readTree(source.bytes()));}catch(IOException e){file.set("rules",mapper.nullNode());file.put("error","Original file is not valid JSON; replace its rules before saving a version");}
        }
        return result;
    }
    @Transactional
    public Map<String,Object> create(Create request,String username) {
        if(request==null || request.files()==null || request.files().isEmpty() || request.files().size()>64 || request.label()==null || request.label().isBlank() || request.label().length()>120)throw ApiException.bad("A label and 1 to 64 rule files are required");
        String bundleId=request.baseBundleId();if(bundleId!=null)bundles.get(bundleId);
        if(request.parentVersionId()!=null && !Objects.equals(bundleId,get(request.parentVersionId()).get("base_bundle_id")))throw ApiException.bad("Parent version belongs to another bundle");
        var snapshot=mapper.createObjectNode().put("baseBundleId",bundleId);var files=snapshot.putArray("files");var sources=new ArrayList<AndroidRuleEvaluator.Source>();var names=new HashSet<String>();int total=0;
        for(var input:request.files()) {
            if(input==null)throw ApiException.bad("Missing rule file");String role=ResultBundleService.role(input.fileName());
            if(!Set.of("CT_RULE","PEAK_RULE").contains(role) || !names.add(input.fileName().toLowerCase(Locale.ROOT)))throw ApiException.bad("Only unique .ct/.peak filenames are allowed");
            byte[] bytes=bytes(input.rules());total+=bytes.length;
            if(bytes.length>2*1024*1024 || total>4*1024*1024)throw ApiException.bad("Rule version size limit exceeded");
            String hash=Digests.hash("SHA-256",bytes);sources.add(new AndroidRuleEvaluator.Source(input.fileName(),role,hash,bytes));
            var descriptor=files.addObject().put("fileName",input.fileName()).put("role",role).put("sha256",hash).put("size",bytes.length);descriptor.set("rules",input.rules());
        }
        var emptyExperiment=mapper.createObjectNode();emptyExperiment.putArray("WellResult").addObject().put("ID","validation").putArray("TargetResults");
        var validation=evaluator.evaluate(emptyExperiment,sources);
        // Build one synthetic empty well solely to validate rule syntax and index references, never as an experiment result.
        if(!validation.path("errors").isEmpty() || validation.path("rows").isEmpty())throw ApiException.bad("Invalid Android rule format: "+validation.path("errors"));
        JsonNode policies=request.combinationRules()==null?mapper.createArrayNode():request.combinationRules();
        validatePolicies(policies,validation);snapshot.set("combinationRules",policies);
        for(var source:sources)try{store.put(source.bytes());}catch(IOException e){throw new UncheckedIOException(e);}
        String id=UUID.randomUUID().toString();snapshot.put("ruleVersionId",id);
        jdbc.update("INSERT INTO rule_versions(id,base_bundle_id,parent_version_id,label,snapshot_json,created_by,created_at) VALUES(?,?,?,?,?,?,?)",
            id,bundleId,request.parentVersionId(),request.label(),new String(bytes(snapshot),java.nio.charset.StandardCharsets.UTF_8),username,System.currentTimeMillis());
        return get(id);
    }
    private void validatePolicies(JsonNode policies,JsonNode validation) {
        if(!policies.isArray() || policies.size()>256)throw ApiException.bad("combinationRules must be an array of up to 256 policies");
        var names=new HashSet<String>();
        for(var policy:policies) {
            String name=policy.path("name").asText();if(name.isBlank() || name.length()>120 || !names.add(name) || !Set.of("ALL","ANY").contains(policy.path("mode").asText()))throw ApiException.bad("Invalid combination name or mode");
            for(String type:List.of("ct","peak")) {
                var ref=policy.path(type);boolean found=false;
                for(var row:validation.path("rows"))if(row.path("fileName").equals(ref.path("fileName")) && row.path("ruleIndex").equals(ref.path("ruleIndex")) && row.path("type").asText().equals(type.toUpperCase(Locale.ROOT)))found=true;
                if(!found)throw ApiException.bad("Combination must reference an existing "+type+" rule index");
            }
        }
    }
    public JsonNode snapshot(String id,String bundleId) {
        var version=get(id);if(!Objects.equals(bundleId,version.get("base_bundle_id")))throw ApiException.bad("Rule version belongs to another result bundle");return (JsonNode)version.get("snapshot");
    }
    public List<AndroidRuleEvaluator.Source> sources(JsonNode snapshot) {
        var result=new ArrayList<AndroidRuleEvaluator.Source>();
        for(var file:snapshot.path("files"))result.add(new AndroidRuleEvaluator.Source(file.path("fileName").asText(),file.path("role").asText(),file.path("sha256").asText(),bytes(file.path("rules"))));return result;
    }
    public List<PcrAnalysisGateway.RuleAttachment> attachments(JsonNode snapshot,String bundleId) {
        var result=new ArrayList<PcrAnalysisGateway.RuleAttachment>();
        for(var source:sources(snapshot))result.add(new PcrAnalysisGateway.RuleAttachment(source.fileName(),store.path(source.sha256()).toString(),source.sha256(),source.role()));
        if(bundleId!=null)for(var item:bundles.attachments(bundleId))if(!item.role().endsWith("_RULE"))result.add(item);return result;
    }
    private byte[] bytes(JsonNode value) { try{return mapper.writeValueAsBytes(value);}catch(IOException e){throw new UncheckedIOException(e);} }
}
