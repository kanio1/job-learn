# Full load

`POST /api/test/etl/payments/full` (same gates as `seed-learning`: `app.testing.enabled=true`, not prod).

Or call `PaymentEtlPipeline.runFull()`.

## What a batch is

One `learning_etl.batch_run` row. Staging rows for that run share `batch_id`. Target upserts by `payment_order_id`.

## SMALL expectation

After `POST /api/test/seed-learning` then a full load:

| Layer | Count |
|---|---|
| SOURCE `payment_orders` | 10 000 |
| STAGING rows for this batch | 10 000 |
| TARGET `fact_payment` | 10 000 |
| `batch_run.load_type` | `FULL` |
| `batch_run.status` | `SUCCEEDED` |

Full extract ignores watermarks (all source rows). It still records `watermark_to = clock at batch start` so the next incremental run has a bound.

## SQL

```sql
SELECT COUNT(*) FROM payment_orders;
SELECT COUNT(*) FROM learning_staging.payment WHERE batch_id = '<batch>';
SELECT COUNT(*) FROM learning_dwh.fact_payment;
SELECT load_type, status, source_rows, staged_rows, loaded_rows, rejected_rows
  FROM learning_etl.batch_run
 ORDER BY started_at DESC
 LIMIT 1;
```

Also run [sql/01-count-recon.sql](sql/01-count-recon.sql).

## How to run the IT

Surefire (`./mvnw test`) includes only `*Test.java`. `PaymentEtlIT` is `*IT.java`, so the default `test` goal skips it. Failsafe runs it on `./mvnw verify`.

From `apps/backend`:

```bash
./mvnw -Dtest=PaymentEtlIT test
./mvnw -Dit.test=PaymentEtlIT verify
```

`-Dtest=PaymentEtlIT` still works: it overrides Surefire includes for that named class.

`PaymentEtlIT` seeds SMALL in `@BeforeEach` (static `truth`, wipe warehouse each test). Do not use `@TestInstance(PER_CLASS)` + `@BeforeAll` seed: Testcontainers has not mapped the Postgres port yet when `@BeforeAll` runs.
