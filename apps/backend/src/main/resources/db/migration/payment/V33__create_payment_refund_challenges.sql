-- V33__create_payment_refund_challenges.sql
CREATE TABLE payment_refund_challenges (
    challenge_id      UUID PRIMARY KEY,
    approval_id       UUID         NOT NULL REFERENCES payment_refund_approvals (approval_id),
    payment_order_id  UUID         NOT NULL REFERENCES payment_orders (payment_order_id),
    pin_hash          VARCHAR(128) NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    attempt_count     INTEGER      NOT NULL DEFAULT 0,
    locked_until      TIMESTAMPTZ,
    verified_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refund_challenge_attempts CHECK (attempt_count >= 0 AND attempt_count <= 20)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refund_challenge_open
    ON payment_refund_challenges (approval_id)
    WHERE verified_at IS NULL;
