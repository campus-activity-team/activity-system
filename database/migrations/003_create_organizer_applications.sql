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
