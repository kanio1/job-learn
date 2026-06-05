ALTER TABLE payment_orders
    ADD COLUMN authorized_at     TIMESTAMPTZ,
    ADD COLUMN expires_at        TIMESTAMPTZ,
    ADD COLUMN captured_at       TIMESTAMPTZ,
    ADD COLUMN cancelled_at      TIMESTAMPTZ,
    ADD COLUMN refunded_at       TIMESTAMPTZ,
    ADD COLUMN captured_amount_minor BIGINT,
    ADD COLUMN refunded_amount_minor BIGINT,
    ADD COLUMN cancellation_reason   VARCHAR(200),
    ADD COLUMN refund_reason         VARCHAR(200),
    ADD COLUMN metadata              TEXT;

ALTER TABLE payment_orders
    DROP CONSTRAINT chk_payment_orders_status;

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_status
        CHECK (status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED'));

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_captured_amount_minor
        CHECK (captured_amount_minor IS NULL OR captured_amount_minor BETWEEN 1 AND 100000000);

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_refunded_amount_minor
        CHECK (refunded_amount_minor IS NULL OR refunded_amount_minor BETWEEN 1 AND 100000000);

ALTER TABLE payment_order_status_history
    DROP CONSTRAINT chk_payment_order_status_history_from_status;

ALTER TABLE payment_order_status_history
    ADD CONSTRAINT chk_payment_order_status_history_from_status
        CHECK (from_status IS NULL OR from_status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED'));

ALTER TABLE payment_order_status_history
    DROP CONSTRAINT chk_payment_order_status_history_to_status;

ALTER TABLE payment_order_status_history
    ADD CONSTRAINT chk_payment_order_status_history_to_status
        CHECK (to_status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED'));

ALTER TABLE payment_order_status_history
    ADD COLUMN action              VARCHAR(20),
    ADD COLUMN idempotency_key_hash VARCHAR(64),
    ADD COLUMN reason              VARCHAR(200),
    ADD COLUMN amount_minor        BIGINT,
    ADD COLUMN psp_reference       VARCHAR(200);

ALTER TABLE payment_order_status_history
    ADD CONSTRAINT chk_payment_order_status_history_action
        CHECK (action IS NULL OR action IN ('AUTHORIZE', 'CAPTURE', 'CANCEL', 'REFUND', 'EXPIRE'));

ALTER TABLE payment_order_status_history
    ADD CONSTRAINT chk_payment_order_status_history_idempotency_key_hash
        CHECK (idempotency_key_hash IS NULL OR idempotency_key_hash ~ '^[0-9a-f]{64}$');

ALTER TABLE idempotency_records
    DROP CONSTRAINT uk_idempotency_records_payment_order;

ALTER TABLE idempotency_records
    ADD COLUMN action VARCHAR(20);

ALTER TABLE idempotency_records
    ADD CONSTRAINT chk_idempotency_records_action
        CHECK (action IS NULL OR action IN ('CREATE', 'AUTHORIZE', 'CAPTURE', 'CANCEL', 'REFUND'));
