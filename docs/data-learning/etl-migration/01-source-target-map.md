# Source-to-target map — payment ETL

Pipeline: `payment_orders` → `learning_staging.payment` → `learning_dwh.fact_payment`.

**Grain of the target:** one row = one payment order (`payment_order_id`).

Tenant is not on `payment_orders`. Extract joins `merchants` and `tenants`.

Staging is a **source copy + ETL metadata**, not a second domain model.

## Mapped fields

| SOURCE FIELD | RULE | TARGET FIELD |
|---|---|---|
| `payment_orders.payment_order_id` | copy | `fact_payment.payment_order_id` |
| `payment_orders.merchant_id` | copy | `fact_payment.merchant_id` |
| `merchants.tenant_id` | join | `fact_payment.tenant_id` |
| `tenants.tenant_reference` | join | `fact_payment.tenant_reference` |
| `payment_orders.client_order_reference` | copy | `fact_payment.client_order_reference` |
| `payment_orders.amount_minor` | copy | `fact_payment.amount_minor` |
| `payment_orders.currency` | copy | `fact_payment.currency` |
| `payment_orders.status` | copy | `fact_payment.source_status` |
| `payment_orders.captured_amount_minor` | copy | `fact_payment.captured_amount_minor` |
| `payment_orders.refunded_amount_minor` | copy | `fact_payment.refunded_amount_minor` |
| `payment_orders.updated_at` | copy | `fact_payment.source_updated_at` |

## Derived fields

| SOURCE FIELD | RULE | TARGET FIELD |
|---|---|---|
| `amount_minor` | `/ 100.0`, `NUMERIC(12,2)` | `amount_major` (12345 → 123.45) |
| `captured_amount_minor` | `IS NOT NULL` | `is_captured` (true for `CAPTURED` and `REFUNDED`) |
| `refunded_amount_minor` | `IS NOT NULL` | `is_refunded` |
| `status` | `= 'CANCELLED'` | `is_cancelled` |
| `status` | `IN ('REFUNDED','CANCELLED','EXPIRED')` | `is_terminal` (`CAPTURED` is not terminal: refund is still legal) |
| `payment_order_status_history` | `COUNT(*)` per payment | `lifecycle_step_count` |
| `captured_at`, `authorized_at` | epoch seconds of the difference | `capture_duration_seconds` (180 on SMALL captured/refunded rows) |

Expected step counts on SMALL: CREATED=1, AUTHORIZED=2, CAPTURED=3, REFUNDED=4, CANCELLED=2, EXPIRED=3.

Representative references: `LEARN-PAY-000000` CAPTURED, `LEARN-PAY-000060` REFUNDED, `LEARN-PAY-000072` CANCELLED, `LEARN-PAY-000092` CREATED.

## ETL metadata

| Field | Meaning |
|---|---|
| `batch_id` | one extract/load attempt |
| `extracted_at` / `loaded_at` | when this batch copied/wrote the row |
| `learning_etl.batch_run` | load type, status, watermarks, row counts |
