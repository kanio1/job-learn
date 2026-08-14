-- Educational RLS lab. Does not change merchants / payment_orders isolation.
-- Role is cluster-wide (Testcontainers): create only when missing.
--
-- The isolation policy below includes current_setting('app.rls_bypass'). That is the
-- *anti-pattern* this lab exists to show: any client on rls_lab_app can SET the GUC.
-- V18 DROPs this policy and replaces it with tenant_id-only USING, plus a separate
-- BYPASSRLS role that Java selects after TenantContext.isPlatformScoped().
-- Do not copy the V17 USING clause into new policies. After Flyway, runtime = V18.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rls_lab_app') THEN
        CREATE ROLE rls_lab_app LOGIN PASSWORD 'rls_lab_app' NOSUPERUSER NOBYPASSRLS;
    END IF;
END
$$;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO rls_lab_app', current_database());
END
$$;

CREATE TABLE rls_lab_item (
    item_id      UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES tenants (tenant_id),
    label        VARCHAR(120) NOT NULL,
    amount_minor BIGINT       NOT NULL
);

CREATE TABLE rls_lab_item_unprotected (
    item_id      UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES tenants (tenant_id),
    label        VARCHAR(120) NOT NULL,
    amount_minor BIGINT       NOT NULL
);

INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
SELECT '00000000-0000-0000-0000-0000000000a1', tenant_id, 'Alpha secret', 100
FROM tenants
WHERE tenant_reference = 'TENANT_ALPHA';

INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
SELECT '00000000-0000-0000-0000-0000000000a2', tenant_id, 'Other tenant secret', 200
FROM tenants
WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID';

INSERT INTO rls_lab_item_unprotected (item_id, tenant_id, label, amount_minor)
SELECT item_id, tenant_id, label, amount_minor
FROM rls_lab_item;

ALTER TABLE rls_lab_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE rls_lab_item FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_lab_item_isolation ON rls_lab_item
    FOR ALL
    TO rls_lab_app
    USING (
        current_setting('app.rls_bypass', true) = 'on'
        OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid
    );

GRANT USAGE ON SCHEMA public TO rls_lab_app;
GRANT SELECT ON TABLE rls_lab_item TO rls_lab_app;
GRANT SELECT ON TABLE rls_lab_item_unprotected TO rls_lab_app;
