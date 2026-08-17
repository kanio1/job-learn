-- Deterministic source change for an incremental demo.
-- Set updated_at after the previous SUCCEEDED watermark_to.
UPDATE payment_orders
   SET amount_minor = amount_minor + 1,
       updated_at = TIMESTAMPTZ '2026-08-17 13:00:00+00'
 WHERE client_order_reference IN (
        'LEARN-PAY-000003',
        'LEARN-PAY-000004',
        'LEARN-PAY-000005'
 );
