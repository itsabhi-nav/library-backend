-- Roles: ADMIN, LIBRARIAN, MEMBER
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone_number VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Subscriptions / Membership plans
CREATE TABLE membership_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration_days INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    plan_id BIGINT REFERENCES membership_plans(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, EXPIRED, CANCELLED
    paid_amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50),  -- CASH, UPI, CARD
    payment_status VARCHAR(50) NOT NULL, -- PAID, PENDING
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seats and shifts
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    seat_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL, -- AVAILABLE, MAINTENANCE
    has_power_outlet BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL, -- e.g., Morning, Evening, Night, Full Day
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bookings link seats, shifts, and users together
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    seat_id BIGINT REFERENCES seats(id) ON DELETE CASCADE,
    shift_id BIGINT REFERENCES shifts(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE CASCADE,
    booking_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, CANCELLED, COMPLETED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_seat_shift_date UNIQUE (seat_id, shift_id, booking_date)
);

-- Lockers
CREATE TABLE lockers (
    id BIGSERIAL PRIMARY KEY,
    locker_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL, -- AVAILABLE, OCCUPIED, MAINTENANCE
    price_per_month DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE locker_rentals (
    id BIGSERIAL PRIMARY KEY,
    locker_id BIGINT REFERENCES lockers(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, COMPLETED, OVERDUE
    paid_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Daily Attendance Log
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    check_in_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    check_out_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
