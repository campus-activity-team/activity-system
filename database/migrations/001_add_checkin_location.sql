ALTER TABLE activities
  ADD COLUMN checkin_latitude DECIMAL(10,7),
  ADD COLUMN checkin_longitude DECIMAL(10,7),
  ADD COLUMN checkin_radius_meters INT;
