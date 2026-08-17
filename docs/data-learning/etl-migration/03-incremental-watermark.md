# Incremental load and watermarks

Column: `payment_orders.updated_at`.

## Boundary contract

| Load | Extract predicate | `watermark_from` | `watermark_to` |
|---|---|---|---|
| FULL | all rows | `NULL` | clock at batch **start** |
| INCREMENTAL | `updated_at > watermark_from AND updated_at <= watermark_to` | last **SUCCEEDED** `watermark_to` | clock at batch **start** |

Why the operators matter:

- `>` on `from` — a row **equal** to the previous success bound is **not** extracted again.
- `<=` on `to` — a row **equal** to this run’s bound **is** included.
- `FAILED` runs do **not** advance the watermark. Incremental always reads the last **SUCCEEDED** `watermark_to`. A FAILED row may still store its own `watermark_to`; the next incremental ignores it.

Proof: SUCCEEDED full at T0; FAILED full at T1; touch three rows at T1; incremental extracts **3** (`from = T0`, `to = T1`). If FAILED had advanced the bound, extract would be empty (`from = T1`, `to = T1`).

Incremental with no prior SUCCEEDED run fails (HTTP 409).

## Demo

1. Full load (records `watermark_to`).
2. Touch three source rows with `updated_at` after that bound: [sql/06-touch-source-incremental.sql](sql/06-touch-source-incremental.sql).
3. `POST /api/test/etl/payments/incremental`.
4. Staging for the new batch = 3. Target still = 10 000 (upsert).

`watermark_to` is decided **before** extract, so rows that change during the run wait for the next batch.
