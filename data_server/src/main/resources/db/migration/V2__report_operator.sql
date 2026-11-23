ALTER TABLE reports ADD COLUMN operator_user_id VARCHAR(36);
ALTER TABLE reports ADD COLUMN operator_name VARCHAR(160);
ALTER TABLE reports ADD COLUMN upload_source VARCHAR(32) DEFAULT 'LEGACY_OR_UNKNOWN' NOT NULL;
ALTER TABLE reports ADD COLUMN uploaded_by_device_id VARCHAR(96);
CREATE INDEX ix_reports_operator_time ON reports(operator_user_id, received_at);
