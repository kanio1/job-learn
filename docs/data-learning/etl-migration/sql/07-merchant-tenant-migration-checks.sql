-- Post-migration invariants for V1.1 merchant tenant_id (do not re-run V1.1).
SELECT COUNT(*) AS merchant_count FROM merchants;

SELECT COUNT(*) AS null_tenant_id
  FROM merchants
 WHERE tenant_id IS NULL;

SELECT COUNT(*) AS orphan_merchants
  FROM merchants m
  LEFT JOIN tenants t ON t.tenant_id = m.tenant_id
 WHERE t.tenant_id IS NULL;

SELECT COUNT(*) AS fk_present
  FROM information_schema.table_constraints
 WHERE table_schema = 'public'
   AND table_name = 'merchants'
   AND constraint_name = 'fk_merchants_tenant_id'
   AND constraint_type = 'FOREIGN KEY';

SELECT COUNT(*) AS index_present
  FROM pg_indexes
 WHERE schemaname = 'public'
   AND indexname = 'idx_merchants_tenant_id';
