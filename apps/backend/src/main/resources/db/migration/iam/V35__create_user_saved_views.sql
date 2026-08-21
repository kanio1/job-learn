-- V35__create_user_saved_views.sql
-- Saved payment-list views. Owner is JWT sub. IAM module.

CREATE TABLE user_saved_views (
    view_id        UUID PRIMARY KEY,
    owner_subject  VARCHAR(255) NOT NULL,
    resource       VARCHAR(32)  NOT NULL,
    name           VARCHAR(80)  NOT NULL,
    filters        JSONB        NOT NULL,
    columns        JSONB        NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_saved_views_owner_name UNIQUE (owner_subject, resource, name),
    CONSTRAINT chk_user_saved_views_resource CHECK (resource IN ('PAYMENT_ORDERS'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_saved_views_default
    ON user_saved_views (owner_subject, resource)
    WHERE is_default;
