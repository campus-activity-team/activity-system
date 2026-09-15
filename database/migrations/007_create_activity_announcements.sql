USE activity_system;

CREATE TABLE IF NOT EXISTS activity_announcements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  publisher_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_activity_announcements_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
  CONSTRAINT fk_activity_announcements_publisher FOREIGN KEY (publisher_id) REFERENCES users (id),
  INDEX idx_activity_announcements_activity_time (activity_id, created_at)
) ENGINE=InnoDB;
