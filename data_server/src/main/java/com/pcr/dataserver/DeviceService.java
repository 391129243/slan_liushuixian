package com.pcr.dataserver;

import jakarta.validation.constraints.*;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
    private final JdbcTemplate jdbc;
    private final ServerSettings settings;
    private final SecureRandom random = new SecureRandom();
    public DeviceService(JdbcTemplate jdbc, ServerSettings settings) { this.jdbc = jdbc; this.settings = settings; }
    public record CreateDevice(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{1,96}") String deviceId,
        @NotBlank @Pattern(regexp="PCR|RNA|ARM|PIPELINE") String deviceType,
        @Min(0) int position, @NotBlank @Size(max=160) String displayName) { }

    public Map<String, String> create(CreateDevice input) {
        String key = newKey();
        try {
            jdbc.update("INSERT INTO devices(device_id,device_type,position_no,display_name,key_hash,enabled,legacy_upload,created_at) VALUES(?,?,?,?,?,?,?,?)",
                input.deviceId(), input.deviceType(), input.position(), input.displayName(), Digests.sha256(key), true, false, System.currentTimeMillis());
        } catch (DuplicateKeyException e) { throw new ApiException(HttpStatus.CONFLICT, "Device already exists"); }
        return Map.of("deviceId", input.deviceId(), "deviceKey", key);
    }
    private String newKey() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }

    public boolean authenticate(String id, String key) {
        if (id == null || key == null || id.length() > 96 || key.length() > 256) return false;
        List<String> hashes = jdbc.query("SELECT key_hash FROM devices WHERE device_id=? AND enabled=TRUE", (rs,n) -> rs.getString(1), id);
        return !hashes.isEmpty() && java.security.MessageDigest.isEqual(
            hashes.getFirst().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            Digests.sha256(key).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
    public List<Map<String,Object>> list(int offset, int limit) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT device_id,device_type,position_no,display_name,enabled,legacy_upload,created_at,last_seen FROM devices ORDER BY device_id LIMIT ? OFFSET ?", limit, offset);
        long cutoff = System.currentTimeMillis() - settings.onlineSeconds() * 1000L;
        for (Map<String,Object> row : rows) row.put("recent_upload", Boolean.TRUE.equals(row.get("enabled"))
            && row.get("last_seen") instanceof Number seen && seen.longValue() >= cutoff);
        return rows;
    }

    public void registerLegacy(String id) {
        if (!jdbc.queryForList("SELECT device_id FROM devices WHERE device_id=?", id).isEmpty()) return;
        try {
            jdbc.update("INSERT INTO devices(device_id,device_type,position_no,display_name,key_hash,enabled,legacy_upload,created_at) VALUES(?,?,?,?,?,?,?,?)",
                id, "PCR", 0, id, Digests.sha256(newKey()), true, true, System.currentTimeMillis());
        } catch (DuplicateKeyException ignored) { /* concurrent registration of the same serial */ }
    }
    public void enabled(String id, boolean enabled) {
        if (jdbc.update("UPDATE devices SET enabled=? WHERE device_id=?", enabled, id) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "Device not found");
    }
    public Map<String,String> rotate(String id) {
        String key = newKey();
        if (jdbc.update("UPDATE devices SET key_hash=? WHERE device_id=?", Digests.sha256(key), id) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "Device not found");
        return Map.of("deviceId", id, "deviceKey", key);
    }
}
