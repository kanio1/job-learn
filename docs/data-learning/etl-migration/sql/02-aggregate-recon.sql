WITH source AS (
    SELECT
        t.tenant_reference,
        po.currency,
        po.status,
        COUNT(*)::bigint AS row_count,
        COALESCE(SUM(po.amount_minor), 0)::bigint AS sum_amount_minor,
        COALESCE(SUM(po.captured_amount_minor), 0)::bigint AS sum_captured_amount_minor,
        COALESCE(SUM(po.refunded_amount_minor), 0)::bigint AS sum_refunded_amount_minor
    FROM payment_orders po
    JOIN merchants m ON m.merchant_id = po.merchant_id
    JOIN tenants t ON t.tenant_id = m.tenant_id
    GROUP BY t.tenant_reference, po.currency, po.status
),
target AS (
    SELECT
        tenant_reference,
        currency,
        source_status AS status,
        COUNT(*)::bigint AS row_count,
        COALESCE(SUM(amount_minor), 0)::bigint AS sum_amount_minor,
        COALESCE(SUM(captured_amount_minor), 0)::bigint AS sum_captured_amount_minor,
        COALESCE(SUM(refunded_amount_minor), 0)::bigint AS sum_refunded_amount_minor
    FROM learning_dwh.fact_payment
    GROUP BY tenant_reference, currency, source_status
)
SELECT
    COALESCE(s.tenant_reference, t.tenant_reference) AS tenant_reference,
    COALESCE(s.currency, t.currency) AS currency,
    COALESCE(s.status, t.status) AS status,
    COALESCE(s.row_count, 0) AS source_row_count,
    COALESCE(t.row_count, 0) AS target_row_count,
    COALESCE(s.sum_amount_minor, 0) AS source_sum_amount_minor,
    COALESCE(t.sum_amount_minor, 0) AS target_sum_amount_minor,
    COALESCE(s.sum_captured_amount_minor, 0) AS source_sum_captured_amount_minor,
    COALESCE(t.sum_captured_amount_minor, 0) AS target_sum_captured_amount_minor,
    COALESCE(s.sum_refunded_amount_minor, 0) AS source_sum_refunded_amount_minor,
    COALESCE(t.sum_refunded_amount_minor, 0) AS target_sum_refunded_amount_minor
FROM source s
FULL OUTER JOIN target t
    ON s.tenant_reference = t.tenant_reference
   AND s.currency = t.currency
   AND s.status = t.status
WHERE COALESCE(s.row_count, 0) <> COALESCE(t.row_count, 0)
   OR COALESCE(s.sum_amount_minor, 0) <> COALESCE(t.sum_amount_minor, 0)
   OR COALESCE(s.sum_captured_amount_minor, 0) <> COALESCE(t.sum_captured_amount_minor, 0)
   OR COALESCE(s.sum_refunded_amount_minor, 0) <> COALESCE(t.sum_refunded_amount_minor, 0);
