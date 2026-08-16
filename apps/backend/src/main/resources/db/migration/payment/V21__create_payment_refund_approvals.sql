CREATE TABLE payment_refund_approvals (
    approval_id       UUID PRIMARY KEY,
    merchant_id       UUID NOT NULL,
    payment_order_id  UUID NOT NULL,
    amount_minor      BIGINT,
    reason            VARCHAR(500),
    maker_subject     VARCHAR(255) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_at       TIMESTAMPTZ,
    CONSTRAINT fk_payment_refund_approvals_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT chk_payment_refund_approvals_status
        CHECK (status IN ('PENDING', 'APPROVED'))
);

CREATE INDEX idx_payment_refund_approvals_order
    ON payment_refund_approvals (payment_order_id, created_at DESC);
