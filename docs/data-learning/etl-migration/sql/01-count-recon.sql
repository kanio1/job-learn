SELECT
    (SELECT COUNT(*) FROM payment_orders) AS source_count,
    (SELECT COUNT(*) FROM learning_dwh.fact_payment) AS target_count;
