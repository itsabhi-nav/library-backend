-- Daily attendance summaries: cumulative minutes per student per calendar day.
-- Monthly totals are derived by summing rows for the month (resets each new month).

CREATE TABLE daily_attendance_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    total_minutes INT NOT NULL DEFAULT 0,
    last_punch_out_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_attendance_user_date UNIQUE (user_id, attendance_date)
);

CREATE INDEX idx_daily_attendance_user_date ON daily_attendance_summary (user_id, attendance_date);
CREATE INDEX idx_daily_attendance_date ON daily_attendance_summary (attendance_date);

-- Backfill from completed attendance sessions (Asia/Kolkata calendar day).
INSERT INTO daily_attendance_summary (user_id, attendance_date, total_minutes, last_punch_out_time)
SELECT
    a.user_id,
    (a.check_in_time AT TIME ZONE 'Asia/Kolkata')::date AS attendance_date,
    SUM(GREATEST(0, EXTRACT(EPOCH FROM (a.check_out_time - a.check_in_time)) / 60)::int) AS total_minutes,
    MAX(a.check_out_time) AS last_punch_out_time
FROM attendance a
WHERE a.check_out_time IS NOT NULL
GROUP BY a.user_id, (a.check_in_time AT TIME ZONE 'Asia/Kolkata')::date
ON CONFLICT (user_id, attendance_date) DO UPDATE SET
    total_minutes = EXCLUDED.total_minutes,
    last_punch_out_time = EXCLUDED.last_punch_out_time,
    updated_at = CURRENT_TIMESTAMP;
