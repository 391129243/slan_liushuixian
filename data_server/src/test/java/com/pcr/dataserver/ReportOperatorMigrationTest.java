package com.pcr.dataserver;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class ReportOperatorMigrationTest {
    @Test void upgradesExistingReportsWithoutInventingUserIdentityOrChangingSource() {
        String url="jdbc:h2:mem:migration-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("1").load().migrate();
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
        jdbc.update("INSERT INTO devices(device_id,device_type,position_no,display_name,key_hash,enabled,legacy_upload,created_at) VALUES('PCR-OLD','PCR',0,'old','hash',TRUE,TRUE,1)");
        jdbc.update("INSERT INTO uploaded_files(id,file_name,file_sha256,file_size,fingerprint,received_at) VALUES('old-file','old.pcr','hash',1,'file-fingerprint',1)");
        String source="{\"Operator\":\"历史操作者\"}";
        jdbc.update("INSERT INTO reports(id,device_id,experiment_name,received_at,fingerprint,payload_json,well_count,target_count,analysis_status) VALUES('old-report','PCR-OLD','old experiment',1,'fingerprint',?,0,0,'DEVICE_RESULT_RECEIVED')",source);
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        var row=jdbc.queryForMap("SELECT * FROM reports WHERE id='old-report'");
        assertThat(row.get("payload_json")).isEqualTo(source);
        assertThat(row.get("operator_user_id")).isNull();
        assertThat(row.get("operator_name")).isNull();
        assertThat(row.get("uploaded_by_device_id")).isNull();
        assertThat(row.get("upload_source")).isEqualTo("LEGACY_OR_UNKNOWN");
        var file=jdbc.queryForMap("SELECT * FROM uploaded_files WHERE id='old-file'");
        assertThat(file.get("uploaded_by_username")).isNull();
        assertThat(file.get("upload_source")).isEqualTo("LEGACY_OR_UNKNOWN");
    }
}
