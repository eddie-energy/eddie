-- Add reason column for UnableToSendEvent details
ALTER TABLE IF EXISTS de_eta.permission_event
    ADD COLUMN IF NOT EXISTS reason text;
