-- V25__merchant_import_previews.sql
-- Preview rows for CSV import. Merchants are inserted only on commit.

CREATE TABLE merchant_import_previews (
    preview_id UUID PRIMARY KEY,
    checksum VARCHAR(64) NOT NULL,
    created_by VARCHAR(160) NOT NULL,
    tenant_id UUID NOT NULL,
    platform_scoped BOOLEAN NOT NULL,
    payload JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    committed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
