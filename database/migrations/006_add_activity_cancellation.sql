USE activity_system;

ALTER TABLE activities
  ADD COLUMN cancellation_reason VARCHAR(500),
  ADD COLUMN cancelled_at DATETIME,
  ADD COLUMN cancelled_by BIGINT,
  ADD CONSTRAINT fk_activities_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id);
