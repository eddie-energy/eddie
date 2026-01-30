-- Create table to track latest meter readings for each permission request
-- This is useful for permission requests that are active in the future
CREATE TABLE de_eta.meter_reading_tracking
(
    id                  BIGSERIAL PRIMARY KEY,
    permission_id       UUID        NOT NULL UNIQUE,
    latest_reading_time TIMESTAMP   NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Create index for fast lookups by permission_id
CREATE INDEX idx_meter_reading_permission_id ON de_eta.meter_reading_tracking (permission_id);

-- Add foreign key constraint (soft reference, as permission_event is event sourced)
-- This helps maintain referential integrity
COMMENT ON TABLE de_eta.meter_reading_tracking IS 'Tracks the latest meter reading timestamp for each permission request';
COMMENT ON COLUMN de_eta.meter_reading_tracking.latest_reading_time IS 'Timestamp of the most recent meter reading received';
