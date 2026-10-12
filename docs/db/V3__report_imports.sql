-- MySQL migration for report imports. Apply once if Hibernate ddl-auto is disabled.
-- Existing rows are retained. Do not rerun ADD COLUMN if Hibernate already added it.
ALTER TABLE health_metrics
  ADD COLUMN units_json VARCHAR(4000) NULL,
  MODIFY COLUMN blood_glucose_level DOUBLE NULL,
  MODIFY COLUMN total_cholesterol DOUBLE NULL,
  MODIFY COLUMN hdl_cholesterol DOUBLE NULL,
  MODIFY COLUMN ldl_cholesterol DOUBLE NULL,
  MODIFY COLUMN triglycerides DOUBLE NULL;

CREATE TABLE IF NOT EXISTS report_import_drafts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  doctor_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  filename VARCHAR(160) NOT NULL,
  source_sha256 VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  confirmed_at DATETIME(6) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  draft_json LONGTEXT NULL,
  confirmed_request_json LONGTEXT NULL,
  measurement_id BIGINT NULL
);
