-- BloodLink v2.0 schema
-- Changes from v1.0:
--   * `inventory` (one integer per blood group) is replaced by `blood_bags`,
--     where every physical unit is its own row with its own expiry date.
--   * `bag_screening` holds the TTI panel result per bag per marker.
--   * `audit_log` is an append only trail of every state change.

CREATE DATABASE IF NOT EXISTS bloodlink CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bloodlink;

CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(512) NOT NULL,
  role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS donors (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(150) NOT NULL,
  blood_group VARCHAR(8) NOT NULL,
  age INT NOT NULL,
  phone VARCHAR(20),
  city VARCHAR(80),
  last_donation_date DATE NULL,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  total_donations INT NOT NULL DEFAULT 0,
  INDEX idx_donor_group (blood_group),
  INDEX idx_donor_city (city)
);

-- Feature 1: bag level inventory.
-- Stock levels are derived by counting rows here, never stored as a number.
CREATE TABLE IF NOT EXISTS blood_bags (
  id INT PRIMARY KEY AUTO_INCREMENT,
  bag_code VARCHAR(32) NOT NULL UNIQUE,
  blood_group VARCHAR(8) NOT NULL,
  component VARCHAR(20) NOT NULL,          -- WHOLE_BLOOD | PRBC | PLATELETS | PLASMA
  donor_id INT NOT NULL,
  donor_name VARCHAR(120) NOT NULL,
  collection_date DATE NOT NULL,
  expiry_date DATE NOT NULL,               -- collection_date + component shelf life
  status VARCHAR(20) NOT NULL,             -- QUARANTINED | AVAILABLE | RESERVED | ISSUED | EXPIRED | DISCARDED
  reserved_for_request INT NULL,
  INDEX idx_bag_group_status (blood_group, status),
  INDEX idx_bag_expiry (expiry_date),
  INDEX idx_bag_reserved (reserved_for_request)
);

-- Feature 2: TTI screening results, one row per bag per marker.
CREATE TABLE IF NOT EXISTS bag_screening (
  bag_id INT NOT NULL,
  marker VARCHAR(20) NOT NULL,             -- HIV | HBV | HCV | SYPHILIS | MALARIA
  result VARCHAR(20) NOT NULL,             -- PENDING | NON_REACTIVE | REACTIVE
  PRIMARY KEY (bag_id, marker),
  FOREIGN KEY (bag_id) REFERENCES blood_bags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blood_requests (
  id INT PRIMARY KEY AUTO_INCREMENT,
  patient_name VARCHAR(120) NOT NULL,
  blood_group VARCHAR(8) NOT NULL,
  units INT NOT NULL,
  hospital VARCHAR(160) NOT NULL,
  doctor VARCHAR(120),
  priority VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  INDEX idx_request_status (status),
  INDEX idx_request_created (created_at)
);

CREATE TABLE IF NOT EXISTS appointments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  donor_name VARCHAR(120) NOT NULL,
  blood_group VARCHAR(8) NOT NULL,
  appointment_date DATE NOT NULL,
  appointment_time TIME NOT NULL,
  branch VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL
);

-- Feature 3: append only audit trail.
-- The application never issues UPDATE or DELETE against this table.
CREATE TABLE IF NOT EXISTS audit_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  occurred_at DATETIME NOT NULL,
  actor VARCHAR(150) NOT NULL,
  actor_role VARCHAR(20) NOT NULL,
  action VARCHAR(40) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id VARCHAR(64),
  details VARCHAR(600),
  INDEX idx_audit_time (occurred_at),
  INDEX idx_audit_action (action),
  INDEX idx_audit_entity (entity_type, entity_id)
);

-- Optional hardening for an existing v1.0 database:
-- DROP TABLE IF EXISTS inventory;
