-- Corrupt TARGET only. Never run this against payment_orders.
DELETE FROM learning_dwh.fact_payment
 WHERE client_order_reference = 'LEARN-PAY-000000';

UPDATE learning_dwh.fact_payment
   SET amount_minor = amount_minor + 1
 WHERE client_order_reference = 'LEARN-PAY-000001';

UPDATE learning_dwh.fact_payment
   SET source_status = 'CREATED'
 WHERE client_order_reference = 'LEARN-PAY-000002';
