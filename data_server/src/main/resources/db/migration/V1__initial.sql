CREATE TABLE devices (
  device_id VARCHAR(96) PRIMARY KEY,
  device_type VARCHAR(32) NOT NULL,
  position_no INT NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  key_hash VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL,
  legacy_upload BOOLEAN NOT NULL,
  created_at BIGINT NOT NULL,
  last_seen BIGINT
);
CREATE TABLE uploaded_files (
  id VARCHAR(36) PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  file_sha256 VARCHAR(64) NOT NULL,
  file_size BIGINT NOT NULL,
  fingerprint VARCHAR(64) NOT NULL UNIQUE,
  received_at BIGINT NOT NULL
);
CREATE TABLE reports (
  id VARCHAR(36) PRIMARY KEY,
  device_id VARCHAR(96) NOT NULL,
  experiment_name VARCHAR(255) NOT NULL,
  start_time VARCHAR(128),
  end_time VARCHAR(128),
  received_at BIGINT NOT NULL,
  fingerprint VARCHAR(64) NOT NULL UNIQUE,
  payload_json LONGTEXT NOT NULL,
  well_count INT NOT NULL,
  target_count INT NOT NULL,
  file_id VARCHAR(36),
  analysis_status VARCHAR(32) NOT NULL,
  CONSTRAINT fk_report_device FOREIGN KEY (device_id) REFERENCES devices(device_id),
  CONSTRAINT fk_report_file FOREIGN KEY (file_id) REFERENCES uploaded_files(id)
);
CREATE INDEX ix_reports_device_time ON reports(device_id, received_at);
