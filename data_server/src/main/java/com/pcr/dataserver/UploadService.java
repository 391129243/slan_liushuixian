package com.pcr.dataserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadService {
    private final JdbcTemplate jdbc;
    private final FileStore files;
    public UploadService(JdbcTemplate jdbc,FileStore files) { this.jdbc=jdbc; this.files=files; }
    // Capitalized response names match the old ResultMsg convention.
    public record UploadReply(String Result,String Message,String ReportID,String FileID,boolean Duplicate) { }

    @Transactional(rollbackFor=Exception.class)
    public UploadReply result(ExperimentParser.Parsed experiment,byte[] original,String authenticatedDevice) {
        return result(experiment,original,authenticatedDevice,null);
    }
    @Transactional(rollbackFor=Exception.class)
    public UploadReply result(ExperimentParser.Parsed experiment,byte[] original,String authenticatedDevice,String adminUsername) {
        if(authenticatedDevice!=null && !authenticatedDevice.equals(experiment.serial()))
            throw new ApiException(HttpStatus.FORBIDDEN,"SerialNo does not match credentials");
        var device=jdbc.queryForList("SELECT enabled,legacy_upload FROM devices WHERE device_id=? FOR UPDATE",experiment.serial());
        if(device.isEmpty() || !Boolean.TRUE.equals(device.getFirst().get("enabled")))
            throw new ApiException(HttpStatus.FORBIDDEN,"Device not registered or disabled");
        if(authenticatedDevice==null && adminUsername==null && !Boolean.TRUE.equals(device.getFirst().get("legacy_upload")))
            throw new ApiException(HttpStatus.FORBIDDEN,"This device requires authenticated upload");
        long now=System.currentTimeMillis();
        jdbc.update("UPDATE devices SET last_seen=? WHERE device_id=?",now,experiment.serial());
        var existing=jdbc.queryForList("SELECT id FROM reports WHERE fingerprint=?",experiment.fingerprint());
        if(!existing.isEmpty()) return new UploadReply("1","succeed",existing.getFirst().get("id").toString(),null,true);
        String id=UUID.randomUUID().toString();
        // Operator is device-supplied text, not an authenticated platform user identity.
        // Human user IDs must be assigned server-side after platform accounts are implemented.
        jdbc.update("INSERT INTO reports(id,device_id,experiment_name,start_time,end_time,received_at,fingerprint,payload_json,well_count,target_count,analysis_status,operator_name,upload_source,uploaded_by_device_id,uploaded_by_username) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id,experiment.serial(),experiment.name(),experiment.start(),experiment.end(),now,experiment.fingerprint(),
            new String(original,StandardCharsets.UTF_8),experiment.wells(),experiment.targets(),"DEVICE_RESULT_RECEIVED",
            experiment.operator(),adminUsername!=null ? "ADMIN_BUNDLE" : authenticatedDevice==null ? "LEGACY_HTTP" : "DEVICE_AUTH",authenticatedDevice,adminUsername);
        return new UploadReply("1","succeed",id,null,false);
    }

    @Transactional(rollbackFor=Exception.class)
    public UploadReply file(String filename,byte[] bytes) throws IOException {
        return file(filename,bytes,null,"LEGACY_HTTP");
    }

    @Transactional(rollbackFor=Exception.class)
    public UploadReply file(String filename,byte[] bytes,String username,String source) throws IOException {
        if(filename==null || filename.isBlank() || filename.length()>255 || filename.contains("/") || filename.contains("\\")
            || filename.contains(":") || filename.chars().anyMatch(c -> c<32)
            || !filename.toLowerCase(Locale.ROOT).endsWith(".pcr")) throw ApiException.bad("A safe .pcr filename is required");
        String hash=files.put(bytes),fingerprint=Digests.sha256(filename+"\n"+hash),id=UUID.randomUUID().toString();
        try {
            jdbc.update("INSERT INTO uploaded_files(id,file_name,file_sha256,file_size,fingerprint,received_at,uploaded_by_username,upload_source) VALUES(?,?,?,?,?,?,?,?)",
                id,filename,hash,bytes.length,fingerprint,System.currentTimeMillis(),username,source);
        } catch(DuplicateKeyException e) {
            String prior=jdbc.queryForObject("SELECT id FROM uploaded_files WHERE fingerprint=?",String.class,fingerprint);
            return new UploadReply("1","succeed",null,prior,true);
        }
        return new UploadReply("1","succeed",null,id,false);
    }
}
