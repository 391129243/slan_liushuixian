package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Immutable PCR + application-rule snapshot. Android CT/PEAK rules are not SLAN SDK analysis parameters. */
@Service
public class ResultBundleService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ExperimentParser parser;
    private final DeviceService devices;
    private final UploadService uploads;
    private final FileStore store;
    private final ServerSettings settings;
    static final int MAX_ATTACHMENTS=64,MAX_ATTACHMENT_BYTES=2*1024*1024;
    public ResultBundleService(JdbcTemplate jdbc,ObjectMapper mapper,ExperimentParser parser,DeviceService devices,
            UploadService uploads,FileStore store,ServerSettings settings) {
        this.jdbc=jdbc;this.mapper=mapper;this.parser=parser;this.devices=devices;this.uploads=uploads;this.store=store;this.settings=settings;
    }
    public record Reply(String Result,String Message,String ReportID,String FileID,String BundleID,boolean Duplicate,int RuleFileCount,int ConclusionFileCount) { }
    record Input(String name,String role,byte[] bytes,String hash) { }
    public static String role(String filename) {
        safeName(filename);String name=filename.toLowerCase(Locale.ROOT);
        if(name.endsWith(".ct")) return "CT_RULE";
        if(name.endsWith(".peak")) return "PEAK_RULE";
        if(name.endsWith(".conclusion")) return "CONCLUSION_ARCHIVE";
        if(name.equals("judgeconclusion.result")) return "DEVICE_CONCLUSION";
        throw ApiException.bad("Unsupported result attachment type");
    }
    public static void safeName(String name) {
        if(name==null || name.isBlank() || name.length()>255 || name.contains("/") || name.contains("\\") || name.contains(":")
            || name.endsWith(".") || name.endsWith(" ") || name.matches(".*[\"<>|?*].*") || name.chars().anyMatch(c->c<32)) throw ApiException.bad("Unsafe result filename");
        String base=name.split("\\.",2)[0].strip().toUpperCase(Locale.ROOT);
        if(base.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) throw ApiException.bad("Reserved result filename");
    }
    @Transactional(rollbackFor=Exception.class)
    public Reply upload(String experimentJson,String manifestJson,MultipartFile pcr,List<MultipartFile> attachments,
            String authenticatedDevice,String adminUsername) throws IOException {
        if(experimentJson==null || manifestJson==null) throw ApiException.bad("experiment and manifest are required for a result bundle");
        byte[] original=experimentJson.getBytes(StandardCharsets.UTF_8);var experiment=parser.parse(original);
        if(authenticatedDevice!=null && !authenticatedDevice.equals(experiment.serial())) throw new ApiException(HttpStatus.FORBIDDEN,"SerialNo does not match credentials");
        if(manifestJson.getBytes(StandardCharsets.UTF_8).length>65536) throw ApiException.bad("Manifest too large");
        JsonNode declared;
        try { declared=mapper.readTree(manifestJson); } catch(IOException error) { throw ApiException.bad("Invalid result manifest JSON"); }
        if(!declared.isObject() || !declared.path("version").isIntegralNumber() || !declared.path("version").canConvertToInt() || declared.path("version").intValue()!=1 || !declared.path("files").isArray()) throw ApiException.bad("Result manifest version 1 required");
        List<MultipartFile> sidecars=attachments==null?List.of():attachments;
        if(sidecars.size()>MAX_ATTACHMENTS) throw ApiException.bad("Too many result attachments");
        String pcrName=pcr.getOriginalFilename();safeName(pcrName);
        if(!pcrName.toLowerCase(Locale.ROOT).endsWith(".pcr")) throw ApiException.bad("Main file must be a PCR");
        var inputs=new ArrayList<Input>();inputs.add(input(pcr,pcrName,"PCR",settings.maxFileBytes()));
        long total=inputs.getFirst().bytes().length;
        Set<String> names=new HashSet<>();names.add(pcrName.toLowerCase(Locale.ROOT));
        for(var part:sidecars) {
            String name=part.getOriginalFilename(),role=role(name);
            if(!names.add(name.toLowerCase(Locale.ROOT))) throw ApiException.bad("Duplicate result filename");
            var item=input(part,name,role,MAX_ATTACHMENT_BYTES);total+=item.bytes().length;
            if(total>64L*1024*1024) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Result bundle exceeds 64 MiB");
            inputs.add(item);
        }
        if(declared.path("files").size()!=inputs.size()) throw ApiException.bad("Manifest must describe every file exactly once");
        Map<String,JsonNode> expected=new HashMap<>();
        for(JsonNode item:declared.path("files")) {
            if(!item.isObject() || !item.path("fileName").isTextual() || expected.put(item.path("fileName").textValue(),item)!=null) throw ApiException.bad("Duplicate or invalid manifest filename");
        }
        var canonical=mapper.createObjectNode().put("version",1);ArrayNode descriptors=canonical.putArray("files");
        inputs.sort(Comparator.comparing(Input::name));
        for(Input input:inputs) {
            JsonNode entry=expected.remove(input.name());
            if(entry==null || !entry.path("size").isIntegralNumber() || !entry.path("size").canConvertToLong()
                || entry.path("size").longValue()!=input.bytes().length || !input.hash().equals(entry.path("sha256").asText()) || !input.role().equals(entry.path("role").asText()))
                throw ApiException.bad("Manifest size, hash, or role does not match uploaded file");
            descriptors.addObject().put("fileName",input.name()).put("role",input.role()).put("sha256",input.hash()).put("size",input.bytes().length);
        }
        String normalized=mapper.writeValueAsString(canonical),fingerprint=Digests.sha256("result-bundle-v1\n"+experiment.fingerprint()+"\n"+normalized);
        // Authorize and lock the device even for a duplicate; revoked credentials must not get a success reply.
        if(authenticatedDevice==null) devices.registerLegacy(experiment.serial());
        var result=uploads.result(experiment.withFingerprint(fingerprint),original,authenticatedDevice,adminUsername);
        var prior=jdbc.queryForList("SELECT id,pcr_file_id FROM result_bundles WHERE fingerprint=?",fingerprint);
        if(!prior.isEmpty()) return reply((String)prior.getFirst().get("id"),result.ReportID(),(String)prior.getFirst().get("pcr_file_id"),inputs,true);
        var main=inputs.stream().filter(i->i.role().equals("PCR")).findFirst().orElseThrow();
        var file=uploads.file(main.name(),main.bytes(),adminUsername,adminUsername==null?"DEVICE_BUNDLE":"ADMIN_BUNDLE");
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO result_bundles(id,report_id,pcr_file_id,fingerprint,manifest_json,uploaded_by_username,received_at) VALUES(?,?,?,?,?,?,?)",
            id,result.ReportID(),file.FileID(),fingerprint,normalized,adminUsername,System.currentTimeMillis());
        for(Input input:inputs) if(!input.role().equals("PCR")) {
            store.put(input.bytes());
            jdbc.update("INSERT INTO result_bundle_attachments(bundle_id,file_name,file_role,file_sha256,file_size) VALUES(?,?,?,?,?)",id,input.name(),input.role(),input.hash(),input.bytes().length);
        }
        jdbc.update("UPDATE reports SET file_id=?,bundle_id=?,analysis_status='WAITING_SDK' WHERE id=?",file.FileID(),id,result.ReportID());
        return reply(id,result.ReportID(),file.FileID(),inputs,false);
    }
    private Input input(MultipartFile part,String name,String role,int limit) throws IOException {
        try(InputStream stream=part.getInputStream()) { byte[] bytes=UploadController.readBounded(stream,limit);return new Input(name,role,bytes,Digests.hash("SHA-256",bytes)); }
    }
    private Reply reply(String id,String report,String file,List<Input> inputs,boolean duplicate) {
        int rules=(int)inputs.stream().filter(i->i.role().endsWith("_RULE")).count(),conclusions=(int)inputs.stream().filter(i->i.role().contains("CONCLUSION")).count();
        return new Reply("1","succeed",report,file,id,duplicate,rules,conclusions);
    }
    public Map<String,Object> get(String id) {
        var rows=jdbc.queryForList("SELECT * FROM result_bundles WHERE id=?",id);
        if(rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Result bundle not found");
        var row=rows.getFirst();
        try { row.put("manifest",mapper.readTree((String)row.remove("manifest_json"))); } catch(IOException error) { throw new IllegalStateException(error); }
        return row;
    }
    public List<Map<String,Object>> forReport(String report) {
        return jdbc.queryForList("SELECT id,pcr_file_id,received_at FROM result_bundles WHERE report_id=? ORDER BY received_at DESC",report);
    }
    public List<PcrAnalysisGateway.RuleAttachment> attachments(String id) {
        get(id);
        return jdbc.query("SELECT file_name,file_role,file_sha256 FROM result_bundle_attachments WHERE bundle_id=? ORDER BY file_name",(rs,n)->
            new PcrAnalysisGateway.RuleAttachment(rs.getString(1),store.path(rs.getString(3)).toString(),rs.getString(3),rs.getString(2)),id);
    }
    public ObjectNode context(String id) {
        var bundle=get(id);var context=mapper.createObjectNode().put("bundleId",id).put("evaluationStatus","ARCHIVED_NOT_EVALUATED");
        context.set("manifest",(JsonNode)bundle.get("manifest"));
        int rules=0;for(var file:((JsonNode)bundle.get("manifest")).path("files"))if(file.path("role").asText().endsWith("_RULE"))rules++;
        context.put("rulesAvailable",rules>0);if(rules==0)context.put("evaluationStatus","MISSING_RULES");
        return context;
    }
    public List<AndroidRuleEvaluator.Source> ruleSources(String id) {
        var sources=new ArrayList<AndroidRuleEvaluator.Source>();
        for(var item:attachments(id))if(Set.of("CT_RULE","PEAK_RULE").contains(item.role())) {
            try { sources.add(new AndroidRuleEvaluator.Source(item.fileName(),item.role(),item.sha256(),verified(item.sha256()))); }
            catch(IOException error) { throw new java.io.UncheckedIOException(error); }
        }
        return sources;
    }
    public ReportService.Download attachment(String id,String name) throws IOException {
        safeName(name);get(id);
        var hashes=jdbc.query("SELECT file_sha256 FROM result_bundle_attachments WHERE bundle_id=? AND file_name=?",(rs,n)->rs.getString(1),id,name);
        if(hashes.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Bundle attachment not found");
        return new ReportService.Download(name,verified(hashes.getFirst()));
    }
    public ReportService.Download download(String id) throws IOException {
        var bundle=get(id);String report=(String)bundle.get("report_id"),file=(String)bundle.get("pcr_file_id");
        var rows=jdbc.queryForList("SELECT file_name,file_sha256 FROM uploaded_files WHERE id=?",file);var main=rows.getFirst();
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)) {
            zip(zip,"manifest.json",mapper.writeValueAsBytes(bundle.get("manifest")));
            zip(zip,"experiment.json",jdbc.queryForObject("SELECT payload_json FROM reports WHERE id=?",String.class,report).getBytes(StandardCharsets.UTF_8));
            zip(zip,(String)main.get("file_name"),verified((String)main.get("file_sha256")));
            for(var item:attachments(id)) zip(zip,item.fileName(),verified(item.sha256()));
        }
        return new ReportService.Download("result-bundle-"+id+".zip",output.toByteArray());
    }
    public ReportService.Download downloadAnalysis(String analysisId) throws IOException {
        var rows=jdbc.queryForList("SELECT id,bundle_id,output_file_id,result_json,rule_snapshot_json,rule_version_id FROM analysis_runs WHERE id=? OR bridge_analysis_id=?",analysisId,analysisId);
        if(rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Analysis not found");
        var analysis=rows.getFirst();
        if(analysis.get("output_file_id")==null) throw new ApiException(HttpStatus.CONFLICT,"Save the analyzed PCR before exporting its package");
        var main=jdbc.queryForMap("SELECT file_name,file_sha256,file_size FROM uploaded_files WHERE id=?",analysis.get("output_file_id"));
        JsonNode result=mapper.readTree((String)analysis.get("result_json"));
        var evaluation=result.path("applicationRuleEvaluation");
        var ruleSnapshot=analysis.get("rule_snapshot_json")==null?mapper.nullNode():mapper.readTree((String)analysis.get("rule_snapshot_json"));
        var editedRules=ruleSnapshot.path("ruleVersion");
        var manifest=mapper.createObjectNode().put("format","analysis-result-bundle-v1").put("analysisRunId",(String)analysis.get("id"))
            .put("sourceBundleId",(String)analysis.get("bundle_id")).put("applicationRulesApplied",evaluation.path("applicationRulesApplied").asBoolean(false));
        var descriptors=manifest.putArray("files");
        descriptors.addObject().put("fileName",(String)main.get("file_name")).put("role","PCR").put("sha256",(String)main.get("file_sha256")).put("size",((Number)main.get("file_size")).longValue());
        var sidecars=analysis.get("bundle_id")==null?List.<PcrAnalysisGateway.RuleAttachment>of():attachments((String)analysis.get("bundle_id"));
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)) {
            zip(zip,(String)main.get("file_name"),verified((String)main.get("file_sha256")));
            for(var item:sidecars) {
                byte[] bytes=verified(item.sha256());
                String name=editedRules.isMissingNode()?item.fileName():"original-attachments/"+item.fileName();
                descriptors.addObject().put("fileName",name).put("role",item.role()).put("sha256",item.sha256()).put("size",bytes.length);
                zip(zip,name,bytes);
            }
            if(!editedRules.isMissingNode()) {
                manifest.put("ruleVersionId",(String)analysis.get("rule_version_id"));
                for(var file:editedRules.path("files")) {
                    byte[] bytes=mapper.writeValueAsBytes(file.path("rules"));String name=file.path("fileName").asText();safeName(name);
                    if(!Digests.hash("SHA-256",bytes).equals(file.path("sha256").asText()))throw new IOException("Edited rule snapshot checksum mismatch");
                    descriptors.addObject().put("fileName",name).put("role",file.path("role").asText()).put("sha256",file.path("sha256").asText()).put("size",bytes.length);
                    zip(zip,name,bytes);
                }
                zip(zip,"rule-version.json",mapper.writeValueAsBytes(editedRules));
            }
            zip(zip,"analysis.json",((String)analysis.get("result_json")).getBytes(StandardCharsets.UTF_8));
            if(!evaluation.isMissingNode()) {
                byte[] bytes=mapper.writeValueAsBytes(evaluation);
                descriptors.addObject().put("fileName","application-conclusions.json").put("role","ANALYSIS_CONCLUSION").put("sha256",Digests.hash("SHA-256",bytes)).put("size",bytes.length);
                zip(zip,"application-conclusions.json",bytes);
            }
            if(result.has("combinedConclusions"))zip(zip,"combined-conclusions.json",mapper.writeValueAsBytes(result.path("combinedConclusions")));
            zip(zip,"manifest.json",mapper.writeValueAsBytes(manifest));
        }
        return new ReportService.Download("analysis-result-"+analysis.get("id")+".zip",output.toByteArray());
    }
    private byte[] verified(String hash) throws IOException {
        if(!Files.isRegularFile(store.path(hash))) throw new ApiException(HttpStatus.GONE,"Stored bundle file missing");
        byte[] bytes=Files.readAllBytes(store.path(hash));
        if(!Digests.hash("SHA-256",bytes).equals(hash)) throw new IOException("Bundle file checksum mismatch");return bytes;
    }
    private void zip(ZipOutputStream stream,String name,byte[] bytes) throws IOException { stream.putNextEntry(new ZipEntry(name));stream.write(bytes);stream.closeEntry(); }
}
