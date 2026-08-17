# Migration validation (not ETL)

ETL tests a **pipeline** (source snapshot → transform → target). Migration validation tests a **schema change** already applied by Flyway. Do not edit historical migrations.

## A. Merchant `tenant_id` — V1.1

File: `apps/backend/src/main/resources/db/migration/merchant/V1.1__add_tenant_to_merchants.sql`

Sequence: ADD nullable → BACKFILL → SET NOT NULL → FK → index.

Run [sql/07-merchant-tenant-migration-checks.sql](sql/07-merchant-tenant-migration-checks.sql):

- before/after row count (now: current `merchants` count)
- zero `NULL` `tenant_id`
- zero orphan merchants
- FK `fk_merchants_tenant_id` present
- index `idx_merchants_tenant_id` present

You cannot re-run V1.1; you assert the **invariants it left behind**.

## B. Checkout anomaly UNIQUE — V13 + V14

V13 created `checkout_anomaly`. V14 deleted duplicate `(session_id, kind)` then added partial UNIQUE `uk_checkout_anomaly_session_kind`.

[sql/08-checkout-anomaly-uniqueness-checks.sql](sql/08-checkout-anomaly-uniqueness-checks.sql): live table has no duplicates; TEMP table reconstructs “duplicates before cleanup” without touching OLTP. Survivors = one row per pair.

## C. RLS — V17 + V18

V17 created the lab and an anti-pattern policy that trusted `app.rls_bypass`. V18 is the runtime contract: tenant GUC only + `rls_lab_bypass` (`BYPASSRLS`).

[sql/09-rls-migration-checks.sql](sql/09-rls-migration-checks.sql) checks `FORCE ROW LEVEL SECURITY`, policy text, and roles.

Isolation behaviour (no cross-tenant leakage, bypass role) is already proven by `RlsLabRestAssuredTest` — do not duplicate that suite here.
