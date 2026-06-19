-- Achievement definitions (DB-driven badge catalog)
CREATE TABLE achievement_definitions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    threshold_value INT NOT NULL,
    threshold_unit VARCHAR(32) NOT NULL,
    icon_key VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_definition_id BIGINT NOT NULL REFERENCES achievement_definitions(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_definition_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements (user_id);

CREATE TABLE user_monthly_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year INT NOT NULL,
    month INT NOT NULL,
    target_minutes INT NOT NULL DEFAULT 9000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_monthly_goal UNIQUE (user_id, year, month)
);

-- Streak badges
INSERT INTO achievement_definitions (code, category, title, description, threshold_value, threshold_unit, icon_key, sort_order) VALUES
('STREAK_7', 'STREAK', '7 Day Streak', 'Study at the library 7 days in a row', 7, 'STREAK_DAYS', 'streak', 10),
('STREAK_15', 'STREAK', '15 Day Streak', 'Study at the library 15 days in a row', 15, 'STREAK_DAYS', 'streak', 11),
('STREAK_30', 'STREAK', '30 Day Streak', 'Study at the library 30 days in a row', 30, 'STREAK_DAYS', 'streak', 12),
('STREAK_100', 'STREAK', '100 Day Streak', 'Study at the library 100 days in a row', 100, 'STREAK_DAYS', 'streak', 13);

-- Lifetime hour badges (threshold in minutes)
INSERT INTO achievement_definitions (code, category, title, description, threshold_value, threshold_unit, icon_key, sort_order) VALUES
('HOURS_1', 'HOURS', 'First Hour', 'Complete 1 hour of study at the library', 60, 'MINUTES', 'hours', 20),
('HOURS_3', 'HOURS', '3 Hours Studied', 'Complete 3 hours of study at the library', 180, 'MINUTES', 'hours', 21),
('HOURS_5', 'HOURS', '5 Hours Studied', 'Complete 5 hours of study at the library', 300, 'MINUTES', 'hours', 22),
('HOURS_10', 'HOURS', '10 Hours Studied', 'Complete 10 hours of study at the library', 600, 'MINUTES', 'hours', 23),
('HOURS_20', 'HOURS', '20 Hours Studied', 'Complete 20 hours of study at the library', 1200, 'MINUTES', 'hours', 24),
('HOURS_30', 'HOURS', '30 Hours Studied', 'Complete 30 hours of study at the library', 1800, 'MINUTES', 'hours', 25),
('HOURS_40', 'HOURS', '40 Hours Studied', 'Complete 40 hours of study at the library', 2400, 'MINUTES', 'hours', 26),
('HOURS_50', 'HOURS', '50 Hours Studied', 'Complete 50 hours of study at the library', 3000, 'MINUTES', 'hours', 27),
('HOURS_60', 'HOURS', '60 Hours Studied', 'Complete 60 hours of study at the library', 3600, 'MINUTES', 'hours', 28),
('HOURS_70', 'HOURS', '70 Hours Studied', 'Complete 70 hours of study at the library', 4200, 'MINUTES', 'hours', 29),
('HOURS_80', 'HOURS', '80 Hours Studied', 'Complete 80 hours of study at the library', 4800, 'MINUTES', 'hours', 30),
('HOURS_90', 'HOURS', '90 Hours Studied', 'Complete 90 hours of study at the library', 5400, 'MINUTES', 'hours', 31),
('HOURS_100', 'HOURS', '100 Hours Studied', 'Complete 100 hours of study at the library', 6000, 'MINUTES', 'hours', 32);

-- Attendance badges
INSERT INTO achievement_definitions (code, category, title, description, threshold_value, threshold_unit, icon_key, sort_order) VALUES
('FIRST_DAY', 'ATTENDANCE', 'First Day at Library', 'Complete your first day of study at the library', 1, 'DAYS', 'first_day', 40),
('DAYS_30', 'ATTENDANCE', '30 Days Attendance', 'Attend the library on 30 different days', 30, 'DAYS', 'calendar', 41);

-- Time pattern badges
INSERT INTO achievement_definitions (code, category, title, description, threshold_value, threshold_unit, icon_key, sort_order) VALUES
('EARLY_BIRD', 'TIME_PATTERN', 'Early Bird', 'Punch in before 10 AM', 600, 'TIME_BEFORE', 'sunrise', 50),
('NIGHT_OWL', 'TIME_PATTERN', 'Night Owl', 'Punch in after 7 PM', 1140, 'TIME_AFTER', 'moon', 51);
