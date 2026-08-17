WITH compared AS (
    SELECT
        COALESCE(po.payment_order_id, f.payment_order_id) AS payment_order_id,
        CASE
            WHEN po.payment_order_id IS NULL THEN 'MISSING_SOURCE'
            WHEN f.payment_order_id IS NULL THEN 'MISSING_TARGET'
            WHEN po.amount_minor IS DISTINCT FROM f.amount_minor
              OR po.status IS DISTINCT FROM f.source_status
              OR po.captured_amount_minor IS DISTINCT FROM f.captured_amount_minor
              OR po.refunded_amount_minor IS DISTINCT FROM f.refunded_amount_minor
            THEN 'VALUE_MISMATCH'
            ELSE 'MATCH'
        END AS result_class
    FROM payment_orders po
    FULL OUTER JOIN learning_dwh.fact_payment f
        ON f.payment_order_id = po.payment_order_id
)
SELECT result_class, COUNT(*) AS row_count
FROM compared
GROUP BY result_class
ORDER BY result_class;
