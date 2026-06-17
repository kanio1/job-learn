-- Step 1: add column nullable to allow backfill
ALTER TABLE merchants ADD COLUMN tenant_id UUID;

-- Step 2: backfill — assign all existing merchants to PLACEHOLDER_TENANT_ID
UPDATE merchants
SET tenant_id = (
    SELECT tenant_id FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'
);

-- Step 3: enforce NOT NULL now that backfill is complete
ALTER TABLE merchants ALTER COLUMN tenant_id SET NOT NULL;

-- Step 4: add FK constraint referencing tenants.tenant_id
ALTER TABLE merchants
    ADD CONSTRAINT fk_merchants_tenant_id
    FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id);

-- Step 5: index for tenant-filtered list queries
CREATE INDEX idx_merchants_tenant_id ON merchants (tenant_id);
