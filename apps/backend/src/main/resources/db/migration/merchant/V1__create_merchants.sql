-- V1__create_merchants.sql
-- Phase 1: Merchant Registry — initial merchants table
CREATE TABLE merchants (
    merchant_id          UUID PRIMARY KEY,
    normalized_reference VARCHAR(64) NOT NULL,
    display_name         VARCHAR(120) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_merchants_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT uk_merchants_normalized_reference UNIQUE (normalized_reference)
);

CREATE INDEX idx_merchants_status ON merchants (status);
CREATE INDEX idx_merchants_created_at ON merchants (created_at DESC, merchant_id ASC);
