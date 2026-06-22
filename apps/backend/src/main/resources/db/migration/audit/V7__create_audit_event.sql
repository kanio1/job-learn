CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_subject VARCHAR(160) NOT NULL,
    actor_display VARCHAR(160) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    outcome VARCHAR(20) NOT NULL,
    CONSTRAINT chk_audit_event_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILED'))
);

CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at);
CREATE INDEX idx_audit_event_tenant_id ON audit_event (tenant_id);
CREATE INDEX idx_audit_event_actor_subject ON audit_event (actor_subject);
CREATE INDEX idx_audit_event_action ON audit_event (action);
