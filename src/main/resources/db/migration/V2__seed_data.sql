-- Insert Default Admin User (Password is 'admin123' hashed using SHA-256: '2407891877f68c9b007053e16954a2042407d7221e85f409ed5136894c25c38d')
INSERT INTO users (email, password_hash, full_name, role, phone_number)
VALUES ('admin@study.com', '2407891877f68c9b007053e16954a2042407d7221e85f409ed5136894c25c38d', 'Admin Manager', 'ADMIN', '1234567890');

-- Insert Default Shifts
INSERT INTO shifts (name, start_time, end_time) VALUES
('Morning Shift', '08:00:00', '14:00:00'),
('Evening Shift', '14:00:00', '20:00:00'),
('Full Day Shift', '08:00:00', '22:00:00');

-- Insert Default Membership Plans
INSERT INTO membership_plans (name, description, duration_days, price) VALUES
('1 Month Basic (Single Shift)', 'Access to morning or evening shift for 30 days', 30, 1000.00),
('1 Month Premium (Full Day)', 'Access to full day shift for 30 days', 30, 1800.00),
('3 Month Premium (Full Day)', 'Access to full day shift for 90 days with discount', 90, 4800.00);

-- Insert 30 Study Seats (half with power outlet, half without)
INSERT INTO seats (seat_number, status, has_power_outlet) VALUES
('Seat-01', 'AVAILABLE', true),
('Seat-02', 'AVAILABLE', true),
('Seat-03', 'AVAILABLE', true),
('Seat-04', 'AVAILABLE', true),
('Seat-05', 'AVAILABLE', true),
('Seat-06', 'AVAILABLE', true),
('Seat-07', 'AVAILABLE', true),
('Seat-08', 'AVAILABLE', true),
('Seat-09', 'AVAILABLE', true),
('Seat-10', 'AVAILABLE', true),
('Seat-11', 'AVAILABLE', true),
('Seat-12', 'AVAILABLE', true),
('Seat-13', 'AVAILABLE', true),
('Seat-14', 'AVAILABLE', true),
('Seat-15', 'AVAILABLE', true),
('Seat-16', 'AVAILABLE', false),
('Seat-17', 'AVAILABLE', false),
('Seat-18', 'AVAILABLE', false),
('Seat-19', 'AVAILABLE', false),
('Seat-20', 'AVAILABLE', false),
('Seat-21', 'AVAILABLE', false),
('Seat-22', 'AVAILABLE', false),
('Seat-23', 'AVAILABLE', false),
('Seat-24', 'AVAILABLE', false),
('Seat-25', 'AVAILABLE', false),
('Seat-26', 'AVAILABLE', false),
('Seat-27', 'AVAILABLE', false),
('Seat-28', 'AVAILABLE', false),
('Seat-29', 'AVAILABLE', false),
('Seat-30', 'AVAILABLE', false);

-- Insert 10 Lockers
INSERT INTO lockers (locker_number, status, price_per_month) VALUES
('Locker-01', 'AVAILABLE', 200.00),
('Locker-02', 'AVAILABLE', 200.00),
('Locker-03', 'AVAILABLE', 200.00),
('Locker-04', 'AVAILABLE', 200.00),
('Locker-05', 'AVAILABLE', 200.00),
('Locker-06', 'AVAILABLE', 200.00),
('Locker-07', 'AVAILABLE', 200.00),
('Locker-08', 'AVAILABLE', 200.00),
('Locker-09', 'AVAILABLE', 200.00),
('Locker-10', 'AVAILABLE', 200.00);
