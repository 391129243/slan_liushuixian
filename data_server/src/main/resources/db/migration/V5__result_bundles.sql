CREATE TABLE result_bundles (
  id VARCHAR(36) PRIMARY KEY,
  report_id VARCHAR(36) NOT NULL UNIQUE,
  pcr_file_id VARCHAR(36) NOT NULL,
  fingerprint VARCHAR(64) NOT NULL UNIQUE,
  manifest_json LONGTEXT NOT NULL,
  uploaded_by_username VARCHAR(160),
  received_at BIGINT NOT NULL,
  CONSTRAINT fk_bundle_report FOREIGN KEY (report_id) REFERENCES reports(id),
  CONSTRAINT fk_bundle_pcr FOREIGN KEY (pcr_file_id) REFERENCES uploaded_files(id)
);
CREATE TABLE result_bundle_attachments (
  bundle_id VARCHAR(36) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_role VARCHAR(32) NOT NULL,
  file_sha256 VARCHAR(64) NOT NULL,
  file_size BIGINT NOT NULL,
  PRIMARY KEY (bundle_id, file_name),
  CONSTRAINT fk_bundle_attachment FOREIGN KEY (bundle_id) REFERENCES result_bundles(id)
);
ALTER TABLE reports ADD COLUMN bundle_id VARCHAR(36);
ALTER TABLE reports ADD COLUMN uploaded_by_username VARCHAR(160);
ALTER TABLE reports ADD CONSTRAINT fk_report_bundle FOREIGN KEY (bundle_id) REFERENCES result_bundles(id);
ALTER TABLE pcr_read_sessions ADD COLUMN bundle_id VARCHAR(36);
ALTER TABLE pcr_read_sessions ADD CONSTRAINT fk_read_bundle FOREIGN KEY (bundle_id) REFERENCES result_bundles(id);
ALTER TABLE analysis_runs ADD COLUMN bundle_id VARCHAR(36);
ALTER TABLE analysis_runs ADD COLUMN rule_snapshot_json LONGTEXT;
ALTER TABLE analysis_runs ADD CONSTRAINT fk_analysis_bundle FOREIGN KEY (bundle_id) REFERENCES result_bundles(id);
