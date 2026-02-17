-- ==============================
-- DATABASE
-- ==============================
CREATE DATABASE IF NOT EXISTS kairon
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE kairon;

-- ==============================
-- COMPANIES
-- ==============================
CREATE TABLE companies (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    business_type VARCHAR(50) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo',
    work_hours JSON,
    slot_duration INT DEFAULT 30,
    buffer_time INT DEFAULT 10,
    currency VARCHAR(3) DEFAULT 'BRL',
    logo_url VARCHAR(500),
    website VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_slug (slug),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- USERS
-- ==============================
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    company_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    refresh_token VARCHAR(500),
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email),
    INDEX idx_company_id (company_id),
    INDEX idx_role (role),
    INDEX idx_professional_id (professional_id),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- PROFESSIONALS
-- ==============================
CREATE TABLE professionals (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    photo_url VARCHAR(500),
    phone VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    work_hours JSON,
    days_off JSON,
    company_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_company_id (company_id),
    INDEX idx_is_active (is_active),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- USERS ↔ PROFESSIONALS (FK)
-- ==============================
ALTER TABLE users
ADD CONSTRAINT fk_users_professional
FOREIGN KEY (professional_id)
REFERENCES professionals(id)
ON DELETE SET NULL;

-- ==============================
-- SERVICES
-- ==============================
CREATE TABLE services (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    duration INT NOT NULL,
    color VARCHAR(7) DEFAULT '#3B82F6',
    is_active BOOLEAN DEFAULT TRUE,
    online_booking BOOLEAN DEFAULT TRUE,
    company_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36),
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE SET NULL,
    INDEX idx_company_id (company_id),
    INDEX idx_professional_id (professional_id),
    INDEX idx_is_active (is_active),
    INDEX idx_category (category),
    INDEX idx_online_booking (online_booking)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- CLIENTS
-- ==============================
CREATE TABLE clients (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    birth_date DATE,
    notes TEXT,
    company_id VARCHAR(36) NOT NULL,
    external_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_company_phone (company_id, phone),
    INDEX idx_company_id (company_id),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- APPOINTMENTS
-- ==============================
CREATE TABLE appointments (
    id VARCHAR(36) PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    timezone VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    cancellation_reason VARCHAR(100),
    notes TEXT,
    total_price DECIMAL(10,2) NOT NULL,
    actual_price DECIMAL(10,2),
    actual_duration INT,
    completed_at TIMESTAMP NULL,
    company_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36) NOT NULL,
    client_id VARCHAR(36) NOT NULL,
    notification_sent_at TIMESTAMP NULL,
    reminder_sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    INDEX idx_company_id (company_id),
    INDEX idx_professional_id (professional_id),
    INDEX idx_client_id (client_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- APPOINTMENT SERVICES
-- ==============================
CREATE TABLE appointment_services (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL,
    service_id VARCHAR(36) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    duration INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_appointment_service (appointment_id, service_id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
    INDEX idx_appointment_id (appointment_id),
    INDEX idx_service_id (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================
-- FINANCIAL RECORDS
-- ==============================
CREATE TABLE financial_records (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(200) NOT NULL,
    appointment_id VARCHAR(36),
    company_id VARCHAR(36) NOT NULL,
    professional_id VARCHAR(36),
    payment_method VARCHAR(50),
    reference_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE SET NULL,
    INDEX idx_company_id (company_id),
    INDEX idx_professional_id (professional_id),
    INDEX idx_reference_date (reference_date),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
