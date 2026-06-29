-- Composite index for paginated export queries (ordered by occurred_at + id for stable cursor)
CREATE INDEX idx_audit_event_export ON audit_event (tenant_id, occurred_at, id);
