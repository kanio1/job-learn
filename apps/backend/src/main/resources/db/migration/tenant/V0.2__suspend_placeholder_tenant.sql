-- DB-MVP-001: Suspend PLACEHOLDER_TENANT_ID for demo purposes.
-- TenantResolver already rejects SUSPENDED non-platform tenants with 403,
-- so this activates that path without any code change.
UPDATE tenants
    SET status = 'SUSPENDED',
        name   = 'Suspended Demo Tenant'
WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID';
