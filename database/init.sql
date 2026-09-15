CREATE DATABASE IF NOT EXISTS activity_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE activity_system;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(64) NOT NULL,
  student_id VARCHAR(64) UNIQUE,
  email VARCHAR(128),
  phone VARCHAR(32),
  avatar VARCHAR(512),
  role ENUM('USER', 'ORGANIZER', 'ADMIN') NOT NULL DEFAULT 'USER',
  status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_users_role (role),
  INDEX idx_users_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS organizer_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  review_comment VARCHAR(1000),
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_organizer_applications_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_organizer_applications_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
  UNIQUE KEY uk_organizer_applications_user (user_id),
  INDEX idx_organizer_applications_status_time (status, updated_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  cover_image VARCHAR(512),
  organizer_id BIGINT NOT NULL,
  location VARCHAR(255) NOT NULL,
  checkin_latitude DECIMAL(10,7),
  checkin_longitude DECIMAL(10,7),
  checkin_radius_meters INT,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  registration_start_time DATETIME NOT NULL,
  registration_end_time DATETIME NOT NULL,
  capacity INT NOT NULL,
  current_registered_count INT NOT NULL DEFAULT 0,
  status ENUM('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'APPROVED', 'PUBLISHED', 'ONGOING', 'ENDED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
  review_comment VARCHAR(1000),
  require_feedback BOOLEAN NOT NULL DEFAULT FALSE,
  feedback_deadline DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_activities_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
  CONSTRAINT chk_activity_capacity CHECK (capacity > 0),
  INDEX idx_activities_status_time (status, start_time),
  INDEX idx_activities_organizer (organizer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status ENUM('REGISTERED', 'CANCELLED') NOT NULL DEFAULT 'REGISTERED',
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at DATETIME,
  CONSTRAINT fk_registrations_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE KEY uk_registrations_activity_user (activity_id, user_id),
  INDEX idx_registrations_user_status (user_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS checkin_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(128) NOT NULL UNIQUE,
  activity_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_checkin_tokens_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  INDEX idx_checkin_tokens_activity_expiry (activity_id, expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attendances (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  checkin_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  checkin_method ENUM('QR_CODE', 'MANUAL') NOT NULL DEFAULT 'QR_CODE',
  status ENUM('SUCCESS', 'CANCELLED') NOT NULL DEFAULT 'SUCCESS',
  CONSTRAINT fk_attendances_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  CONSTRAINT fk_attendances_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_attendances_registration FOREIGN KEY (registration_id) REFERENCES registrations (id),
  UNIQUE KEY uk_attendances_activity_user (activity_id, user_id),
  INDEX idx_attendances_activity_time (activity_id, checkin_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS checkin_anomalies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reason VARCHAR(40) NOT NULL,
  message VARCHAR(255) NOT NULL,
  latitude DECIMAL(10,7),
  longitude DECIMAL(10,7),
  distance_meters INT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_checkin_anomalies_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  CONSTRAINT fk_checkin_anomalies_user FOREIGN KEY (user_id) REFERENCES users (id),
  INDEX idx_checkin_anomalies_activity_time (activity_id, created_at),
  INDEX idx_checkin_anomalies_user_time (user_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedbacks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  overall_rating TINYINT NOT NULL,
  content_rating TINYINT NOT NULL,
  service_rating TINYINT NOT NULL,
  comment VARCHAR(2000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_feedbacks_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  CONSTRAINT fk_feedbacks_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_feedback_rating CHECK (overall_rating BETWEEN 1 AND 5 AND content_rating BETWEEN 1 AND 5 AND service_rating BETWEEN 1 AND 5),
  UNIQUE KEY uk_feedbacks_activity_user (activity_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  operation VARCHAR(128) NOT NULL,
  target_type VARCHAR(64),
  target_id BIGINT,
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_operation_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
  INDEX idx_operation_logs_user_time (user_id, created_at),
  INDEX idx_operation_logs_target (target_type, target_id)
) ENGINE=InnoDB;
