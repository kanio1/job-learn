SELECT payment_order_id, COUNT(*) AS copies
FROM learning_dwh.fact_payment
GROUP BY payment_order_id
HAVING COUNT(*) > 1;
