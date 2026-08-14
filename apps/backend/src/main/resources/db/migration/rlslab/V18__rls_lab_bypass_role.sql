-- Supersedes the V17 USING clause that trusted app.rls_bypass.
-- V17 is left as-is (Flyway history). This migration is the runtime contract:
--   rls_lab_app  — NOBYPASSRLS, tenant_id GUC only
--   rls_lab_bypass — BYPASSRLS, never exposed to the client; Java opens this pool
--                    only when TenantContext.isPlatformScoped() is true.
-- jdbcFakeBypassGucDoesNotLeakRows proves SET app.rls_bypass is a no-op after this.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rls_lab_bypass') THEN
        CREATE ROLE rls_lab_bypass LOGIN PASSWORD 'rls_lab_bypass' NOSUPERUSER BYPASSRLS;
    END IF;
END
$$;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO rls_lab_bypass', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO rls_lab_bypass;
GRANT SELECT ON TABLE rls_lab_item TO rls_lab_bypass;
GRANT SELECT ON TABLE rls_lab_item_unprotected TO rls_lab_bypass;

DROP POLICY IF EXISTS rls_lab_item_isolation ON rls_lab_item;

CREATE POLICY rls_lab_item_isolation ON rls_lab_item
    FOR ALL
    TO rls_lab_app
    USING (
        tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid
    );
