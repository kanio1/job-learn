-- V23__merchant_list_query_indexes.sql
-- List + tenant isolation. B-tree is PostgreSQL default (PG 18).

CREATE INDEX IF NOT EXISTS idx_merchants_tenant_status_updated
    ON merchants (tenant_id, status, updated_at DESC, merchant_id ASC);

CREATE INDEX IF NOT EXISTS idx_merchants_tenant_updated
    ON merchants (tenant_id, updated_at DESC, merchant_id ASC);

-- Case-insensitive exact/prefix search used by q=
CREATE INDEX IF NOT EXISTS idx_merchants_normalized_reference_lower
    ON merchants ((lower(normalized_reference)));

CREATE INDEX IF NOT EXISTS idx_merchants_display_name_lower
    ON merchants ((lower(display_name)));
