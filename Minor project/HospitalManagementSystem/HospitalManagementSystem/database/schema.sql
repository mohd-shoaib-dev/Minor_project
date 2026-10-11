CREATE DATABASE IF NOT EXISTS hospital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital;

CREATE TABLE IF NOT EXISTS users (
  username VARCHAR(50) PRIMARY KEY,
  password_hash CHAR(64) NOT NULL,
  role ENUM('admin','reception','viewer') NOT NULL
);

CREATE TABLE IF NOT EXISTS patients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  age INT NOT NULL CHECK (age BETWEEN 0 AND 120),
  gender VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS doctors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  specialization VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  doctor_id INT NOT NULL,
  appointment_date DATE NOT NULL,
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT uq_doctor_day UNIQUE (doctor_id, appointment_date)
);

-- Demo passwords are: admin123, reception123, viewer123.
-- SHA-256 hashes are used for this classroom demo. Use a password-hashing library such as BCrypt in production.
INSERT IGNORE INTO users(username,password_hash,role) VALUES
('admin', SHA2('admin123',256), 'admin'),
('reception', SHA2('reception123',256), 'reception'),
('viewer', SHA2('viewer123',256), 'viewer');

INSERT IGNORE INTO doctors(id,name,specialization) VALUES
(1,'Dr. Aditi Sharma','Cardiology'),
(2,'Dr. Rahul Verma','Orthopedics'),
(3,'Dr. Neha Singh','Dermatology'),
(4,'Dr. Arjun Mehta','General Medicine');
