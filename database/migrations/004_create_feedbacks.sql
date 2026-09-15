USE activity_system;

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
