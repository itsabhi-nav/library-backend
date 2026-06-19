-- Exam countdown targets (DB-driven exam catalog)
CREATE TABLE exam_definitions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    exam_label VARCHAR(128) NOT NULL,
    exam_date DATE NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_exam_targets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    exam_definition_id BIGINT NOT NULL REFERENCES exam_definitions(id) ON DELETE RESTRICT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Daily study log (subject-wise self-reported study)
CREATE TABLE daily_study_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date DATE NOT NULL,
    subject VARCHAR(128) NOT NULL,
    minutes_studied INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_study_logs_user_date ON daily_study_logs (user_id, log_date DESC);

-- Next upcoming exam dates (Asia/Kolkata calendar; update annually via migration or admin)
INSERT INTO exam_definitions (code, name, exam_label, exam_date, sort_order) VALUES
('UPSC', 'UPSC', 'UPSC Prelims', '2027-05-30', 1),
('SSC_CGL', 'SSC CGL', 'SSC CGL Tier-I', '2026-09-14', 2),
('BANKING', 'Banking', 'IBPS PO Prelims', '2026-10-11', 3),
('JEE', 'JEE', 'JEE Main Session 1', '2027-01-24', 4),
('NEET', 'NEET', 'NEET UG', '2027-05-03', 5);
