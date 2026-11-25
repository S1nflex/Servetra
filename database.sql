-- WARNING: this script recreates the servetra schema from scratch.
-- [ETAP 1] Database schema and relations for coursework variant #3.
DROP DATABASE IF EXISTS servetra;
CREATE DATABASE servetra CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE servetra;

CREATE TABLE equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inventory_number VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(120),
    location VARCHAR(160) NOT NULL,
    department VARCHAR(120),
    criticality_score TINYINT UNSIGNED NOT NULL,
    next_service_due DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_equipment_criticality CHECK (criticality_score BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE specialist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    specialization VARCHAR(120),
    phone VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE maintenance_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_number VARCHAR(32) UNIQUE,
    equipment_id BIGINT NOT NULL,
    specialist_id BIGINT,
    summary VARCHAR(180) NOT NULL,
    description TEXT,
    diagnostic_notes TEXT,
    status VARCHAR(32) NOT NULL,
    criticality_snapshot TINYINT UNSIGNED NOT NULL,
    severity_score TINYINT UNSIGNED NOT NULL,
    downtime_impact_score TINYINT UNSIGNED NOT NULL,
    priority_index DECIMAL(5,2) NOT NULL,
    due_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    CONSTRAINT fk_request_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT fk_request_specialist FOREIGN KEY (specialist_id) REFERENCES specialist(id),
    CONSTRAINT chk_request_status CHECK (status IN ('NEW','DIAGNOSED','IN_SERVICE','COMPLETED')),
    CONSTRAINT chk_request_f1 CHECK (criticality_snapshot BETWEEN 0 AND 100),
    CONSTRAINT chk_request_f2 CHECK (severity_score BETWEEN 0 AND 100),
    CONSTRAINT chk_request_f3 CHECK (downtime_impact_score BETWEEN 0 AND 100),
    CONSTRAINT chk_request_priority CHECK (priority_index BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE INDEX idx_request_status_priority ON maintenance_request(status, priority_index);
CREATE INDEX idx_request_equipment ON maintenance_request(equipment_id);
CREATE INDEX idx_request_specialist ON maintenance_request(specialist_id);

CREATE TABLE service_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    specialist_id BIGINT NOT NULL,
    work_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    performed_at DATETIME NOT NULL,
    duration_minutes INT,
    cost_amount DECIMAL(12,2),
    CONSTRAINT fk_record_request FOREIGN KEY (request_id) REFERENCES maintenance_request(id),
    CONSTRAINT fk_record_specialist FOREIGN KEY (specialist_id) REFERENCES specialist(id),
    CONSTRAINT chk_record_type CHECK (work_type IN ('DIAGNOSTICS','REPAIR','PREVENTIVE','INSPECTION')),
    CONSTRAINT chk_record_duration CHECK (duration_minutes IS NULL OR duration_minutes >= 0),
    CONSTRAINT chk_record_cost CHECK (cost_amount IS NULL OR cost_amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_record_request_time ON service_record(request_id, performed_at);

INSERT INTO equipment
    (inventory_number, name, category, manufacturer, model, serial_number, location, department, criticality_score, next_service_due, active)
VALUES
    ('LAB-SP-001', 'Спектрофотометр UV-Vis', 'LAB', 'Shimadzu', 'UV-1900i', 'UV1900-2401', 'Лабораторія 204', 'Хімічна лабораторія', 90, '2027-02-01', TRUE),
    ('LAB-MIC-002', 'Цифровий мікроскоп', 'LAB', 'Leica', 'DM750', 'DM750-1188', 'Лабораторія 112', 'Біологічна лабораторія', 75, '2027-01-20', TRUE),
    ('LAB-PC-003', 'Робоча станція аналізу', 'COMPUTER', 'Dell', 'Precision 3660', 'PC-3660-77', 'Лабораторія 204', 'ІТ', 60, NULL, TRUE);

INSERT INTO specialist(full_name, email, specialization, phone, active) VALUES
    ('Олена Коваль', 'olena.koval@example.edu', 'Лабораторна електроніка', '+380501112233', TRUE),
    ('Андрій Мельник', 'andrii.melnyk@example.edu', 'Комп’ютерні системи', '+380671234567', TRUE);

INSERT INTO maintenance_request
    (request_number, equipment_id, specialist_id, summary, description, diagnostic_notes, status,
     criticality_snapshot, severity_score, downtime_impact_score, priority_index, due_at)
VALUES
    ('REQ-000001', 1, 1, 'Не проходить самотестування',
     'Після запуску прилад завершує самотестування з кодом помилки E17.',
     NULL, 'NEW', 90, 80, 60, 79.00, '2026-11-22 15:00:00'),
    ('REQ-000002', 2, 1, 'Нечітке зображення',
     'Зображення в окулярах втрачає різкість після прогріву.',
     'Перевірено освітлювач та механіку фокусування.', 'DIAGNOSED', 75, 55, 40, 59.25, '2026-11-25 12:00:00');

INSERT INTO service_record
    (request_id, specialist_id, work_type, description, performed_at, duration_minutes, cost_amount)
VALUES
    (2, 1, 'DIAGNOSTICS', 'Очищено оптичний тракт і перевірено механізм фокусування.', '2026-11-18 10:30:00', 45, 0.00);

