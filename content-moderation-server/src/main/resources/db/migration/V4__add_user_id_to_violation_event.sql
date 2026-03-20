ALTER TABLE violation_event
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_violation_event_user_id ON violation_event(user_id);

