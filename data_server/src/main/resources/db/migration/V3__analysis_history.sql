CREATE TABLE pcr_read_sessions (
  id VARCHAR(36) PRIMARY KEY,
  bridge_experiment_id VARCHAR(64) NOT NULL UNIQUE,
  report_id VARCHAR(36),
  parent_analysis_id VARCHAR(36),
  input_file_id VARCHAR(36) NOT NULL,
  source_sha256 VARCHAR(64) NOT NULL,
  operator_username VARCHAR(160) NOT NULL,
  sdk_version VARCHAR(255),
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_read_report FOREIGN KEY (report_id) REFERENCES reports(id),
  CONSTRAINT fk_read_file FOREIGN KEY (input_file_id) REFERENCES uploaded_files(id)
);
CREATE TABLE analysis_runs (
  id VARCHAR(36) PRIMARY KEY,
  bridge_analysis_id VARCHAR(64) UNIQUE,
  read_session_id VARCHAR(36) NOT NULL,
  parent_analysis_id VARCHAR(36),
  report_id VARCHAR(36),
  input_file_id VARCHAR(36) NOT NULL,
  output_file_id VARCHAR(36),
  operator_username VARCHAR(160) NOT NULL,
  request_id VARCHAR(64),
  parameters_json LONGTEXT NOT NULL,
  result_json LONGTEXT,
  sdk_version VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  file_save_status VARCHAR(32) NOT NULL,
  error_message VARCHAR(512),
  save_error_message VARCHAR(512),
  created_at BIGINT NOT NULL,
  completed_at BIGINT,
  save_started_at BIGINT,
  saved_at BIGINT,
  CONSTRAINT uq_analysis_request UNIQUE (operator_username, request_id),
  CONSTRAINT fk_analysis_session FOREIGN KEY (read_session_id) REFERENCES pcr_read_sessions(id),
  CONSTRAINT fk_analysis_parent FOREIGN KEY (parent_analysis_id) REFERENCES analysis_runs(id),
  CONSTRAINT fk_analysis_report FOREIGN KEY (report_id) REFERENCES reports(id),
  CONSTRAINT fk_analysis_input FOREIGN KEY (input_file_id) REFERENCES uploaded_files(id),
  CONSTRAINT fk_analysis_output FOREIGN KEY (output_file_id) REFERENCES uploaded_files(id)
);
ALTER TABLE pcr_read_sessions ADD CONSTRAINT fk_read_parent FOREIGN KEY (parent_analysis_id) REFERENCES analysis_runs(id);
CREATE INDEX ix_analysis_report_time ON analysis_runs(report_id, created_at);
CREATE INDEX ix_analysis_input_time ON analysis_runs(input_file_id, created_at);
CREATE INDEX ix_analysis_operator_time ON analysis_runs(operator_username, created_at);
