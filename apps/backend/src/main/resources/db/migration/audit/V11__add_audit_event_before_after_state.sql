ALTER TABLE audit_event
    ADD COLUMN before_state JSONB,
    ADD COLUMN after_state JSONB;
