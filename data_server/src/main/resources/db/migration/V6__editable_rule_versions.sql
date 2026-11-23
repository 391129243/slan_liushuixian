CREATE TABLE rule_versions (
  id VARCHAR(36) PRIMARY KEY,
  base_bundle_id VARCHAR(36),
  parent_version_id VARCHAR(36),
  label VARCHAR(120) NOT NULL,
  snapshot_json LONGTEXT NOT NULL,
  created_by VARCHAR(160) NOT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_rule_bundle FOREIGN KEY (base_bundle_id) REFERENCES result_bundles(id),
  CONSTRAINT fk_rule_parent FOREIGN KEY (parent_version_id) REFERENCES rule_versions(id)
);
ALTER TABLE pcr_read_sessions ADD COLUMN rule_version_id VARCHAR(36);
ALTER TABLE analysis_runs ADD COLUMN rule_version_id VARCHAR(36);
ALTER TABLE pcr_read_sessions ADD CONSTRAINT fk_read_rule_version FOREIGN KEY (rule_version_id) REFERENCES rule_versions(id);
ALTER TABLE analysis_runs ADD CONSTRAINT fk_analysis_rule_version FOREIGN KEY (rule_version_id) REFERENCES rule_versions(id);
CREATE INDEX ix_rule_version_bundle ON rule_versions(base_bundle_id,created_at);
