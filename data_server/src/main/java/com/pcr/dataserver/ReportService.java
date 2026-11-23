package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final FileStore files;
    public ReportService(JdbcTemplate jdbc,ObjectMapper mapper,FileStore files) { this.jdbc=jdbc; this.mapper=mapper; this.files=files; }
    public Map<String,Object> summary() {
        return Map.of("devices",jdbc.queryForObject("SELECT COUNT(*) FROM devices",Long.class),
            "reports",jdbc.queryForObject("SELECT COUNT(*) FROM reports",Long.class),
            "files",jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files",Long.class),
            "sdkAvailable",false);
    }
    public Map<String,Object> list(String serial,int offset,int limit) {
        String where=serial==null ? "" : " WHERE device_id=?";
        List<Object> args=new ArrayList<>(); if(serial!=null) args.add(serial);
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM reports"+where,Long.class,args.toArray());
        args.add(limit); args.add(offset);
        var rows=jdbc.queryForList("SELECT id,device_id,experiment_name,start_time,end_time,received_at,well_count,target_count,file_id,analysis_status,bundle_id,operator_user_id,operator_name,upload_source,uploaded_by_device_id,uploaded_by_username FROM reports"+where+" ORDER BY received_at DESC,id LIMIT ? OFFSET ?",args.toArray());
        return Map.of("total",count,"offset",offset,"limit",limit,"items",rows);
    }
    public Map<String,Object> detail(String id) throws IOException {
        var rows=jdbc.queryForList("SELECT id,device_id,experiment_name,start_time,end_time,received_at,well_count,target_count,file_id,analysis_status,bundle_id,operator_user_id,operator_name,upload_source,uploaded_by_device_id,uploaded_by_username,payload_json FROM reports WHERE id=?",id);
        if(rows.isEmpty()) throw missing("Report");
        var row=rows.getFirst(); row.put("Experiment",mapper.readTree(row.remove("payload_json").toString())); return row;
    }
    public String source(String id) {
        var rows=jdbc.query("SELECT payload_json FROM reports WHERE id=?",(rs,n) -> rs.getString(1),id);
        if(rows.isEmpty()) throw missing("Report"); return rows.getFirst();
    }
    public List<Map<String,Object>> files(int offset,int limit) {
        return jdbc.queryForList("SELECT id,file_name,file_sha256,file_size,received_at,uploaded_by_username,upload_source FROM uploaded_files ORDER BY received_at DESC,id LIMIT ? OFFSET ?",limit,offset);
    }
    public record Download(String name,byte[] content) { }
    public PcrAnalysisGateway.SourceFile sourceFile(String id) {
        var rows=jdbc.queryForList("SELECT file_name,file_sha256 FROM uploaded_files WHERE id=?",id);
        if(rows.isEmpty()) throw missing("File");
        Path path=files.path(rows.getFirst().get("file_sha256").toString());
        if(!Files.isRegularFile(path)) throw new ApiException(HttpStatus.GONE,"Stored file missing");
        return new PcrAnalysisGateway.SourceFile(id,rows.getFirst().get("file_name").toString(),path.toString());
    }
    public Download download(String id) throws IOException {
        var rows=jdbc.queryForList("SELECT file_name,file_sha256 FROM uploaded_files WHERE id=?",id);
        if(rows.isEmpty()) throw missing("File");
        String hash=rows.getFirst().get("file_sha256").toString(); Path path=files.path(hash);
        if(!Files.isRegularFile(path)) throw new ApiException(HttpStatus.GONE,"Stored file missing");
        byte[] bytes=Files.readAllBytes(path);
        if(!Digests.hash("SHA-256",bytes).equals(hash)) throw new IOException("Stored file integrity check failed");
        return new Download(rows.getFirst().get("file_name").toString(),bytes);
    }
    @Transactional
    public void attach(String report,String file) {
        var rows=jdbc.queryForList("SELECT file_id FROM reports WHERE id=? FOR UPDATE",report);
        if(rows.isEmpty()) throw missing("Report");
        Object current=rows.getFirst().get("file_id");
        if(current!=null && !current.equals(file)) throw new ApiException(HttpStatus.CONFLICT,"Report already has a different file");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM uploaded_files WHERE id=?",Long.class,file)==0) throw missing("File");
        jdbc.update("UPDATE reports SET file_id=?,analysis_status='WAITING_SDK' WHERE id=?",file,report);
    }
    private ApiException missing(String resource) { return new ApiException(HttpStatus.NOT_FOUND,resource+" not found"); }
}
