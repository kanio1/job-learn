# Reconciliation

Do not hide this in Java. Run the SQL in `psql` after a full load.

## Level 1 — counts

[sql/01-count-recon.sql](sql/01-count-recon.sql)

Source count vs target count. Fast, weak: 10 000 = 10 000 can still hide wrong amounts.

## Level 2 — aggregates

[sql/02-aggregate-recon.sql](sql/02-aggregate-recon.sql)

Compares `COUNT(*)`, `SUM(amount_minor)`, `SUM(captured_amount_minor)`, `SUM(refunded_amount_minor)` by tenant, currency, and status.

Expect **zero rows**. Aggregates catch a wrong amount that counts miss.

## Level 3 — record

[sql/03-record-recon.sql](sql/03-record-recon.sql) classifies each `payment_order_id`:

- `MATCH`
- `MISSING_SOURCE`
- `MISSING_TARGET`
- `VALUE_MISMATCH`

[sql/03b-duplicate-recon.sql](sql/03b-duplicate-recon.sql) is `DUPLICATE` (expect 0; PK forbids it).

## Controlled defects (target only)

[sql/04-inject-target-defects.sql](sql/04-inject-target-defects.sql) deletes one fact, bumps one amount, flips one status. Level 3 then shows `MISSING_TARGET` and `VALUE_MISMATCH`.

Reset: [sql/05-reset-target.sql](sql/05-reset-target.sql) then `POST /api/test/etl/payments/rebuild`. Never corrupt `payment_orders`.
