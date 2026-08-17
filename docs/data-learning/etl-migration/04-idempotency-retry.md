# Idempotency and retry

## Two different ideas

**Deterministic generation** (`DataLearningDataset`): the same index always produces the same UUID. Seed twice → the same 10 000 identities.

**ETL idempotency**: loading the same business keys twice must not duplicate warehouse facts. Target uses:

```sql
INSERT INTO learning_dwh.fact_payment (...)
SELECT ...
ON CONFLICT (payment_order_id) DO UPDATE SET ...
```

Full load twice over unchanged source:

| Run | Staging (all batches) | `fact_payment` |
|---|---|---|
| 1 | 10 000 | 10 000 |
| 2 | 20 000 | 10 000 |

Staging **appends per `batch_id`**. Target **upserts by business key**.

## Restart

A run inserts `RUNNING`, extracts, then loads. If it fails after staging:

- `status = FAILED`
- `loaded_rows = 0`
- `rejected_rows = 0` (a crash is not a row reject)
- watermark does not advance

A later `runFull()` / `runIncremental()` uses a **new** `batch_id` and upserts. Facts stay unique.

Lab seams (testing profile, `app.testing.enabled=true`, not prod):

- `PaymentEtlClock` — freeze `watermark_to` (same idea as `CheckoutLabClock`).
- `PaymentEtlFault` — one-shot fail after staging (`armAfterStaging()` / `consumeAfterStaging()`).

Do not inject `java.time.Clock` into the pipeline constructor or add `runFull(boolean failAfterStaging)` on the pipeline.

`POST /api/test/etl/payments/rebuild` deletes this pipeline’s staging, facts, and `batch_run` rows, then full-loads again.
