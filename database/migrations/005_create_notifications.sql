USE activity_system;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  target_type VARCHAR(64),
  target_id BIGINT,
  dedup_key VARCHAR(191) UNIQUE,
  read_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
  INDEX idx_notifications_user_read_time (user_id, read_at, created_at)
) ENGINE=InnoDB;
