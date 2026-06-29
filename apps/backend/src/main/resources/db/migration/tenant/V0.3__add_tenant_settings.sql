ALTER TABLE tenants
    ADD COLUMN contact_email    VARCHAR(320),
    ADD COLUMN timezone         VARCHAR(64)    NOT NULL DEFAULT 'UTC',
    ADD COLUMN webhook_base_url VARCHAR(500),
    ADD COLUMN settings_version BIGINT         NOT NULL DEFAULT 0;
