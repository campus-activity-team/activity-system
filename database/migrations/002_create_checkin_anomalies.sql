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
