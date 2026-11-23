package com.pcr.dataserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Persistent history around the process-local C# SDK handles. No HTTP calls hold a DB transaction. */
@Service
public class PcrAnalysisService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PcrAnalysisGateway gateway;
    private final ReportService reports;
    private final UploadService uploads;
    private final TransactionTemplate transaction;
    private final ResultBundleService bundles;
    private final AndroidRuleEvaluator ruleEvaluator;
    private final RuleVersionService ruleVersions;
    private final CombinedRuleEvaluator combinedRules;
    private static final String METADATA="id,bridge_analysis_id,read_session_id,parent_analysis_id,report_id,bundle_id,rule_version_id,input_file_id,output_file_id,operator_username,saved_by_username,request_id,sdk_version,status,file_save_status,error_message,save_error_message,created_at,completed_at,save_started_at,saved_at";

    public PcrAnalysisService(JdbcTemplate jdbc,ObjectMapper mapper,PcrAnalysisGateway gateway,
            ReportService reports,UploadService uploads,PlatformTransactionManager manager,ResultBundleService bundles,AndroidRuleEvaluator ruleEvaluator,RuleVersionService ruleVersions,CombinedRuleEvaluator combinedRules) {
        this.jdbc=jdbc;this.mapper=mapper;this.gateway=gateway;this.reports=reports;this.uploads=uploads;
        this.transaction=new TransactionTemplate(manager);
        this.bundles=bundles;
        this.ruleEvaluator=ruleEvaluator;
        this.ruleVersions=ruleVersions;this.combinedRules=combinedRules;
    }

    public JsonNode read(String fileId,String reportId,String parentAnalysisId,String bundleId,String username) {
        var source=reports.sourceFile(fileId);
        String parentId=null,ruleVersionId=null;
        if(bundleId!=null && parentAnalysisId==null) {
            var bundle=bundles.get(bundleId);
            if(!fileId.equals(bundle.get("pcr_file_id")) || (reportId!=null && !reportId.equals(bundle.get("report_id")))) throw conflict("Bundle does not match input file or report");
            reportId=(String)bundle.get("report_id");
        }
        if(parentAnalysisId!=null) {
            var parent=run(parentAnalysisId);
            if(!fileId.equals(parent.get("output_file_id"))) throw conflict("Parent analysis output must match the input file");
            String parentReport=(String)parent.get("report_id");
            if(reportId!=null && !Objects.equals(reportId,parentReport)) throw conflict("Parent analysis belongs to a different report");
            reportId=parentReport;parentId=(String)parent.get("id");
            if(bundleId!=null && !Objects.equals(bundleId,parent.get("bundle_id"))) throw conflict("Parent analysis used a different rule bundle");
            bundleId=(String)parent.get("bundle_id");
            ruleVersionId=(String)parent.get("rule_version_id");
        } else if(reportId!=null) {
            var rows=jdbc.queryForList("SELECT file_id FROM reports WHERE id=?",reportId);
            if(rows.isEmpty()) throw missing("Report");
            if(!fileId.equals(rows.getFirst().get("file_id"))) throw conflict("Attach this input file to the report first");
        } else {
            var candidates=jdbc.queryForList("SELECT id FROM reports WHERE file_id=?",fileId);
            if(candidates.size()>1) throw conflict("Multiple reports reference this file; specify reportId");
            if(candidates.size()==1) reportId=(String)candidates.getFirst().get("id");
            if(candidates.isEmpty() && jdbc.queryForObject("SELECT COUNT(*) FROM analysis_runs WHERE output_file_id=? AND (bundle_id IS NOT NULL OR rule_version_id IS NOT NULL)",Long.class,fileId)>0)
                throw conflict("This derived PCR has an archived rule snapshot; select its parentAnalysisId from analysis history");
        }
        if(bundleId==null && reportId!=null) bundleId=jdbc.queryForObject("SELECT bundle_id FROM reports WHERE id=?",String.class,reportId);
        if(bundleId!=null) {
            var bundle=bundles.get(bundleId);
            if(parentId==null && !fileId.equals(bundle.get("pcr_file_id"))) throw conflict("PCR does not match archived bundle");
            source=new PcrAnalysisGateway.SourceFile(source.fileId(),source.fileName(),source.sourcePath(),bundles.attachments(bundleId));
        }
        if(ruleVersionId!=null)source=new PcrAnalysisGateway.SourceFile(source.fileId(),source.fileName(),source.sourcePath(),ruleVersions.attachments(ruleVersions.snapshot(ruleVersionId,bundleId),bundleId));
        JsonNode capabilities=gateway.capabilities();
        JsonNode versionNode=capabilities.get("vendorVersion");
        String version=versionNode==null || versionNode.isNull() ? null : versionNode.isTextual()?versionNode.textValue():versionNode.toString();
        if(version!=null && version.length()>255) throw upstream("Invalid SDK version");
        JsonNode response=gateway.read(source);
        String bridgeId=handle(response,"experimentId");
        String hash=jdbc.queryForObject("SELECT file_sha256 FROM uploaded_files WHERE id=?",String.class,fileId);
        if(!hash.equals(response.path("sourceSha256").asText())) throw upstream("Read PCR source checksum mismatch");
        if(!response.path("experiment").isObject()) throw upstream("Missing read experiment");
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO pcr_read_sessions(id,bridge_experiment_id,report_id,parent_analysis_id,input_file_id,source_sha256,operator_username,sdk_version,created_at,bundle_id,rule_version_id) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id,bridgeId,reportId,parentId,fileId,hash,username,version,System.currentTimeMillis(),bundleId,ruleVersionId);
        ObjectNode reply=(ObjectNode)response.deepCopy();
        reply.put("readSessionId",id);reply.put("reportId",reportId);reply.put("parentAnalysisId",parentId);reply.put("sdkVersion",version);
        reply.set("ruleContext",bundleId==null?mapper.nullNode():bundles.context(bundleId));
        reply.put("ruleVersionId",ruleVersionId);
        return reply;
    }

    public JsonNode reanalyze(String experimentId,PcrAnalysisGateway.AnalysisParameters parameters,String requestId,String username) {
        return reanalyze(experimentId,parameters,requestId,username,null);
    }
    public JsonNode reanalyze(String experimentId,PcrAnalysisGateway.AnalysisParameters parameters,String requestId,String username,String requestedRuleVersion) {
        String parametersJson=json(mapper.valueToTree(parameters));
        var sessions=jdbc.queryForList("SELECT * FROM pcr_read_sessions WHERE bridge_experiment_id=?",experimentId);
        if(sessions.isEmpty()) throw missing("Read session; read the file through dataserver first");
        var session=sessions.getFirst();
        if(!username.equals(session.get("operator_username"))) throw new ApiException(HttpStatus.FORBIDDEN,"Read session belongs to another user");
        String ruleVersionId="ORIGINAL".equals(requestedRuleVersion)?null:requestedRuleVersion==null?(String)session.get("rule_version_id"):requestedRuleVersion;
        String sourceBundleId=(String)session.get("bundle_id");
        JsonNode ruleSnapshot=ruleVersionId==null?null:ruleVersions.snapshot(ruleVersionId,sourceBundleId);
        if(requestId!=null) { var prior=priorRequest(username,requestId);if(prior!=null)return replay(prior,experimentId,parametersJson,ruleVersionId); }
        ObjectNode context=sourceBundleId==null?mapper.createObjectNode():bundles.context(sourceBundleId);
        if(ruleSnapshot!=null)context.set("ruleVersion",ruleSnapshot);
        String id=UUID.randomUUID().toString();
        try {
            String bundleId=(String)session.get("bundle_id");
            jdbc.update("INSERT INTO analysis_runs(id,read_session_id,parent_analysis_id,report_id,input_file_id,operator_username,request_id,parameters_json,sdk_version,status,file_save_status,created_at,bundle_id,rule_snapshot_json,rule_version_id) VALUES(?,?,?,?,?,?,?,?,?,'RUNNING','NOT_SAVED',?,?,?,?)",
                id,session.get("id"),session.get("parent_analysis_id"),session.get("report_id"),session.get("input_file_id"),username,requestId,parametersJson,session.get("sdk_version"),System.currentTimeMillis(),bundleId,context.isEmpty()?null:json(context),ruleVersionId);
        } catch(DuplicateKeyException error) {
            var prior=requestId==null?null:priorRequest(username,requestId);
            if(prior==null) throw error;
            return replay(prior,experimentId,parametersJson,ruleVersionId);
        }
        try {
            JsonNode result=gateway.reanalyze(new PcrAnalysisGateway.Reanalyze(experimentId,parameters,ruleSnapshot==null?null:ruleVersions.attachments(ruleSnapshot,sourceBundleId)));
            if(result instanceof ObjectNode object) object.set("ruleContext",parse((String)run(id).get("rule_snapshot_json")));
            String bridgeId=handle(result,"analysisId");
            if(!result.path("experiment").isObject()) throw upstream("Missing analyzed experiment");
            if(!experimentId.equals(result.path("experimentId").asText())) throw upstream("Analysis read session mismatch");
            if(result instanceof ObjectNode object) {
                String bundleId=(String)session.get("bundle_id");
                var evaluation=ruleEvaluator.evaluate(result.path("experiment"),ruleSnapshot!=null?ruleVersions.sources(ruleSnapshot):bundleId==null?List.of():bundles.ruleSources(bundleId));
                evaluation.put("sourceBundleId",bundleId).put("analysisRunId",id);
                evaluation.put("ruleVersionId",ruleVersionId);
                object.set("applicationRuleEvaluation",evaluation);
                object.set("combinedConclusions",combinedRules.evaluate(evaluation,ruleSnapshot==null?null:ruleSnapshot.path("combinationRules")));
                object.put("ruleVersionId",ruleVersionId);
                // This is evaluated by Java after SDK recomputation; the C# bridge only restores attachments.
                object.put("applicationRulesApplied",evaluation.path("applicationRulesApplied").asBoolean());
            }
            if(jdbc.update("UPDATE analysis_runs SET bridge_analysis_id=?,result_json=?,status='SUCCEEDED',completed_at=? WHERE id=? AND status='RUNNING'",
                    bridgeId,json(result),System.currentTimeMillis(),id)!=1) throw new IllegalStateException("Analysis state changed");
            return resultReply(result,run(id));
        } catch(RuntimeException error) {
            jdbc.update("UPDATE analysis_runs SET status='FAILED',error_message=?,completed_at=? WHERE id=? AND status='RUNNING'",
                failure(error),System.currentTimeMillis(),id);
            throw error;
        }
    }

    public Map<String,Object> save(String analysisId,String filename,String username) {
        var row=run(analysisId);
        if("SAVED".equals(row.get("file_save_status"))) return savedReply(row,filename,true);
        if(!"SUCCEEDED".equals(row.get("status"))) throw conflict("Only a successful analysis can be saved");
        String id=(String)row.get("id");
        int claimed=jdbc.update("UPDATE analysis_runs SET file_save_status='SAVING',save_started_at=?,save_error_message=NULL WHERE id=? AND status='SUCCEEDED' AND file_save_status IN ('NOT_SAVED','FAILED') AND output_file_id IS NULL",
            System.currentTimeMillis(),id);
        if(claimed!=1) {
            row=run(id);
            if("SAVED".equals(row.get("file_save_status"))) return savedReply(row,filename,true);
            throw conflict("File save is already in progress; check analysis history before retrying");
        }
        try {
            JsonNode saved=gateway.save(new PcrAnalysisGateway.Save((String)row.get("bridge_analysis_id"),filename));
            byte[] bytes=gateway.download(saved.path("outputId").asText());
            if(!Digests.hash("SHA-256",bytes).equals(saved.path("sha256").asText())) throw upstream("Saved PCR output checksum mismatch");
            transaction.executeWithoutResult(status -> {
                try {
                    var stored=uploads.file(filename,bytes,username,"SDK_ANALYSIS");
                    if(jdbc.update("UPDATE analysis_runs SET output_file_id=?,file_save_status='SAVED',saved_at=?,saved_by_username=?,save_error_message=NULL WHERE id=? AND file_save_status='SAVING'",
                            stored.FileID(),System.currentTimeMillis(),username,id)!=1) throw new IllegalStateException("Save state changed");
                } catch(IOException error) { throw new UncheckedIOException(error); }
            });
            return savedReply(run(id),filename,false);
        } catch(RuntimeException error) {
            jdbc.update("UPDATE analysis_runs SET file_save_status='FAILED',save_error_message=? WHERE id=? AND file_save_status='SAVING'",failure(error),id);
            throw error;
        }
    }

    public Map<String,Object> list(String reportId,String inputFileId,int offset,int limit) {
        var conditions=new ArrayList<String>();var args=new ArrayList<Object>();
        if(reportId!=null){conditions.add("report_id=?");args.add(reportId);}
        if(inputFileId!=null){conditions.add("input_file_id=?");args.add(inputFileId);}
        String where=conditions.isEmpty()?"":" WHERE "+String.join(" AND ",conditions);
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM analysis_runs"+where,Long.class,args.toArray());
        args.add(limit);args.add(offset);
        var items=jdbc.queryForList("SELECT "+METADATA+" FROM analysis_runs"+where+" ORDER BY created_at DESC,id LIMIT ? OFFSET ?",args.toArray());
        return Map.of("total",total,"offset",offset,"limit",limit,"items",items);
    }
    public Map<String,Object> detail(String id) {
        var row=run(id);
        row.put("parameters",parse((String)row.remove("parameters_json")));
        row.put("result",parse((String)row.remove("result_json")));
        row.put("ruleSnapshot",parse((String)row.remove("rule_snapshot_json")));
        return row;
    }
    public JsonNode result(String id) {
        String result=(String)run(id).get("result_json");
        if(result==null) throw conflict("Analysis has no stored result");
        return parse(result);
    }
    private Map<String,Object> run(String id) {
        var rows=jdbc.queryForList("SELECT * FROM analysis_runs WHERE id=? OR bridge_analysis_id=?",id,id);
        if(rows.isEmpty()) throw missing("Analysis");
        return rows.getFirst();
    }
    private Map<String,Object> priorRequest(String username,String requestId) {
        var rows=jdbc.queryForList("SELECT a.*,s.bridge_experiment_id FROM analysis_runs a JOIN pcr_read_sessions s ON s.id=a.read_session_id WHERE a.operator_username=? AND a.request_id=?",username,requestId);
        return rows.isEmpty()?null:rows.getFirst();
    }
    private JsonNode replay(Map<String,Object> row,String experimentId,String parameters,String ruleVersionId) {
        if(!experimentId.equals(row.get("bridge_experiment_id")) || !parameters.equals(row.get("parameters_json")) || !Objects.equals(ruleVersionId,row.get("rule_version_id")))
            throw conflict("requestId was already used with a different session or parameters");
        if(!"SUCCEEDED".equals(row.get("status"))) throw conflict("Existing analysis is "+row.get("status")+"; inspect history before submitting a new requestId");
        return resultReply(parse((String)row.get("result_json")),row);
    }
    private JsonNode resultReply(JsonNode result,Map<String,Object> row) {
        ObjectNode reply=(ObjectNode)result.deepCopy();
        reply.put("analysisRunId",(String)row.get("id"));reply.put("reportId",(String)row.get("report_id"));
        reply.put("inputFileId",(String)row.get("input_file_id"));reply.put("analysisStatus",(String)row.get("status"));
        return reply;
    }
    private Map<String,Object> savedReply(Map<String,Object> row,String filename,boolean duplicate) {
        String fileId=(String)row.get("output_file_id");
        String storedName=jdbc.queryForObject("SELECT file_name FROM uploaded_files WHERE id=?",String.class,fileId);
        if(!filename.equals(storedName)) throw conflict("This analysis is already saved with a different filename");
        return Map.of("Result","1","FileID",fileId,"analysisId",row.get("bridge_analysis_id"),"analysisRunId",row.get("id"),"Duplicate",duplicate,"mock",false);
    }
    private String handle(JsonNode node,String field) {
        String value=node.path(field).asText();
        if(!value.matches("[a-f0-9]{32}")) throw upstream("Invalid bridge "+field);
        return value;
    }
    private String json(JsonNode node) {
        try { return mapper.writeValueAsString(node); } catch(JsonProcessingException error) { throw new IllegalStateException("Cannot serialize analysis JSON",error); }
    }
    private JsonNode parse(String json) {
        if(json==null) return mapper.nullNode();
        try { return mapper.readTree(json); } catch(JsonProcessingException error) { throw new IllegalStateException("Cannot read stored analysis JSON",error); }
    }
    private String failure(RuntimeException error) {
        String message=error instanceof ApiException ? error.getMessage() : "Operation failed: "+error.getClass().getSimpleName();
        return message.substring(0,Math.min(message.length(),512));
    }
    private ApiException missing(String name) { return new ApiException(HttpStatus.NOT_FOUND,name+" not found"); }
    private ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT,message); }
    private ApiException upstream(String message) { return new ApiException(HttpStatus.BAD_GATEWAY,message); }
}
