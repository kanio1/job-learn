CREATE TABLE tenants (
    tenant_id        UUID         PRIMARY KEY,
    tenant_reference VARCHAR(64)  NOT NULL,
    name             VARCHAR(120) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    tenant_type      VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tenants_tenant_reference UNIQUE (tenant_reference),
    CONSTRAINT chk_tenants_status     CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_tenants_tenant_type CHECK (tenant_type IN ('PLATFORM', 'STANDARD'))
);

INSERT INTO tenants (tenant_id, tenant_reference, name, status, tenant_type, created_at)
VALUES (gen_random_uuid(), 'PLATFORM_TENANT', 'Platform Tenant', 'ACTIVE', 'PLATFORM', NOW());

INSERT INTO tenants (tenant_id, tenant_reference, name, status, tenant_type, created_at)
VALUES (gen_random_uuid(), 'TENANT_ALPHA', 'Alpha Tenant', 'ACTIVE', 'STANDARD', NOW());

INSERT INTO tenants (tenant_id, tenant_reference, name, status, tenant_type, created_at)
VALUES (gen_random_uuid(), 'PLACEHOLDER_TENANT_ID', 'Placeholder Tenant', 'ACTIVE', 'STANDARD', NOW());
