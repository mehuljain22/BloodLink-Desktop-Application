package com.bloodlink.db;

import java.sql.*;

public final class Database {

    private Database() {}

    public static boolean isConfigured() { return System.getenv("BLOODLINK_DB_URL") != null; }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("BLOODLINK_DB_URL", "jdbc:mysql://localhost:3306/bloodlink");
        String user = System.getenv().getOrDefault("BLOODLINK_DB_USER", "root");
        String pass = System.getenv().getOrDefault("BLOODLINK_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, pass);
    }

    public static void initializeSchema() throws SQLException {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  name VARCHAR(120) NOT NULL,
                  email VARCHAR(150) NOT NULL UNIQUE,
                  password_hash VARCHAR(512) NOT NULL,
                  role VARCHAR(20) NOT NULL)""");

            s.executeUpdate("""
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
                  INDEX idx_donor_city (city))""");

            // Bag level inventory. Replaces the old single-counter inventory table.
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS blood_bags (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  bag_code VARCHAR(32) NOT NULL UNIQUE,
                  blood_group VARCHAR(8) NOT NULL,
                  component VARCHAR(20) NOT NULL,
                  donor_id INT NOT NULL,
                  donor_name VARCHAR(120) NOT NULL,
                  collection_date DATE NOT NULL,
                  expiry_date DATE NOT NULL,
                  status VARCHAR(20) NOT NULL,
                  reserved_for_request INT NULL,
                  INDEX idx_bag_group_status (blood_group, status),
                  INDEX idx_bag_expiry (expiry_date),
                  INDEX idx_bag_reserved (reserved_for_request))""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bag_screening (
                  bag_id INT NOT NULL,
                  marker VARCHAR(20) NOT NULL,
                  result VARCHAR(20) NOT NULL,
                  PRIMARY KEY (bag_id, marker),
                  FOREIGN KEY (bag_id) REFERENCES blood_bags(id) ON DELETE CASCADE)""");

            s.executeUpdate("""
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
                  INDEX idx_request_created (created_at))""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS appointments (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  donor_name VARCHAR(120) NOT NULL,
                  blood_group VARCHAR(8) NOT NULL,
                  appointment_date DATE NOT NULL,
                  appointment_time TIME NOT NULL,
                  branch VARCHAR(120) NOT NULL,
                  status VARCHAR(30) NOT NULL)""");

            // Append only audit trail. No update or delete path exists in the application.
            s.executeUpdate("""
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
                  INDEX idx_audit_entity (entity_type, entity_id))""");
        }
    }
}
