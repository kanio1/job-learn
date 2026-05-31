CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_status ON payment_orders(merchant_id, status);

CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_currency ON payment_orders(merchant_id, currency);
