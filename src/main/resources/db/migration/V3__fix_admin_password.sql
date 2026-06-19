-- Update admin user password hash to correct SHA-256 for 'admin123'
UPDATE users
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE email = 'admin@study.com';
