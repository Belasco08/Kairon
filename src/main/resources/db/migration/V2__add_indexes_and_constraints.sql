USE kairon;

-- =====================================================
-- INDEXES (SEM FUNÇÕES, COMPATÍVEL COM MARIADB 10.4)
-- =====================================================

CREATE INDEX idx_appointments_company_start_time
ON appointments (company_id, start_time);

CREATE INDEX idx_financial_records_company_reference_type
ON financial_records (company_id, reference_date, type);

CREATE INDEX idx_services_company_active
ON services (company_id, is_active, online_booking);

CREATE INDEX idx_professionals_company_active
ON professionals (company_id, is_active);

CREATE INDEX idx_clients_company_phone
ON clients (company_id, phone);

CREATE INDEX idx_users_company_active
ON users (company_id, is_active, role);

-- =====================================================
-- CHECK CONSTRAINTS
-- (MariaDB 10.4 aceita sintaxe, mas NÃO valida)
-- =====================================================

ALTER TABLE appointments
ADD CONSTRAINT chk_appointment_times
CHECK (end_time > start_time);

ALTER TABLE services
ADD CONSTRAINT chk_service_price
CHECK (price >= 0);

ALTER TABLE services
ADD CONSTRAINT chk_service_duration
CHECK (duration > 0);

ALTER TABLE financial_records
ADD CONSTRAINT chk_financial_amount
CHECK (amount <> 0);

-- =====================================================
-- TRIGGER: BEFORE UPDATE (appointments)
-- =====================================================

DELIMITER $$

CREATE TRIGGER before_appointment_update
BEFORE UPDATE ON appointments
FOR EACH ROW
BEGIN
    -- Quando virar COMPLETED, seta completed_at
    IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' THEN
        SET NEW.completed_at = NOW();
    END IF;

    -- Quando sair de COMPLETED, limpa completed_at
    IF NEW.status <> 'COMPLETED' AND OLD.status = 'COMPLETED' THEN
        SET NEW.completed_at = NULL;
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- TRIGGER: AFTER UPDATE (appointments)
-- =====================================================

DELIMITER $$

CREATE TRIGGER after_appointment_completed
AFTER UPDATE ON appointments
FOR EACH ROW
BEGIN
    -- Registro financeiro ao completar
    IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' THEN
        INSERT INTO financial_records (
            id, type, amount, description,
            appointment_id, company_id, professional_id,
            reference_date, created_at, updated_at
        ) VALUES (
            UUID(),
            'APPOINTMENT',
            NEW.total_price,
            CONCAT('Agendamento ', NEW.id),
            NEW.id,
            NEW.company_id,
            NEW.professional_id,
            NEW.completed_at,
            NOW(),
            NOW()
        );
    END IF;

    -- Registro financeiro ao cancelar
    IF NEW.status = 'CANCELLED' AND OLD.status <> 'CANCELLED' THEN
        INSERT INTO financial_records (
            id, type, amount, description,
            appointment_id, company_id, professional_id,
            reference_date, created_at, updated_at
        ) VALUES (
            UUID(),
            'REFUND',
            -NEW.total_price,
            CONCAT('Cancelamento ', NEW.id),
            NEW.id,
            NEW.company_id,
            NEW.professional_id,
            NOW(),
            NOW(),
            NOW()
        );
    END IF;
END$$

DELIMITER ;
