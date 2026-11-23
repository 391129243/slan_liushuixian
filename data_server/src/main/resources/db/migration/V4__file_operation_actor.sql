ALTER TABLE uploaded_files ADD COLUMN uploaded_by_username VARCHAR(160);
ALTER TABLE uploaded_files ADD COLUMN upload_source VARCHAR(32) DEFAULT 'LEGACY_OR_UNKNOWN' NOT NULL;
ALTER TABLE analysis_runs ADD COLUMN saved_by_username VARCHAR(160);
