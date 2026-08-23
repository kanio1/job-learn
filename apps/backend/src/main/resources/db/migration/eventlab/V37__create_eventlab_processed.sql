CREATE TABLE eventlab_processed (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    consumer_group VARCHAR(128) NOT NULL,
    event_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(160) NOT NULL,
    tenant_ref VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSED','RETRYING','DEAD')),
    attempts INT NOT NULL DEFAULT 1,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    topic VARCHAR(255) NOT NULL,
    partition_no INT NOT NULL,
    record_offset BIGINT NOT NULL,
    record_key VARCHAR(160),
    CONSTRAINT uq_eventlab_group_event UNIQUE (consumer_group, event_id)
);
CREATE INDEX idx_eventlab_target_id ON eventlab_processed (target_id);
CREATE INDEX idx_eventlab_consumed_at ON eventlab_processed (consumed_at);
