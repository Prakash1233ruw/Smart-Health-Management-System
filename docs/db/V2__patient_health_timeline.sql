-- Additive MySQL migration. Apply once before starting the upgraded application.
-- No existing patient, metric, or symptom-analysis rows are modified.
CREATE TABLE IF NOT EXISTS health_change_analyses (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  symptoms TEXT NULL,
  response_json MEDIUMTEXT NOT NULL,
  model_name VARCHAR(100) NOT NULL,
  review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_change_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS doctor_reviews (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  analysis_id BIGINT NOT NULL,
  doctor_id BIGINT NULL,
  reviewer_doctor_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL,
  internal_notes VARCHAR(3000) NULL,
  follow_up_recommendation VARCHAR(1000) NULL,
  recommended_follow_up_date DATE NULL,
  reviewed_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_review_analysis FOREIGN KEY (analysis_id) REFERENCES health_change_analyses(id) ON DELETE CASCADE,
  CONSTRAINT fk_review_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL
);
CREATE INDEX idx_change_patient_time ON health_change_analyses(patient_id, created_at, id);
CREATE INDEX idx_review_analysis_time ON doctor_reviews(analysis_id, reviewed_at, id);
