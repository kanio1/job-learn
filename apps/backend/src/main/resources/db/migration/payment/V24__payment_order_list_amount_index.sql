-- V24__payment_order_list_amount_index.sql
-- List filter by status + ORDER BY amount_minor / created_at. B-tree is PostgreSQL default (PG 18).

CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_amount
    ON payment_orders (merchant_id, amount_minor, payment_order_id);

CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_status_created
    ON payment_orders (merchant_id, status, created_at DESC, payment_order_id);
