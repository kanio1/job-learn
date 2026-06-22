CREATE TABLE payment_orders (
    payment_order_id       UUID PRIMARY KEY,
    merchant_id            UUID NOT NULL,
    client_order_reference VARCHAR(120) NOT NULL,
    amount_minor           BIGINT NOT NULL,
    currency               VARCHAR(3) NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_orders_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id),
    CONSTRAINT chk_payment_orders_amount_minor
        CHECK (amount_minor BETWEEN 1 AND 100000000),
    CONSTRAINT chk_payment_orders_currency
        CHECK (currency IN ('PLN', 'EUR', 'USD')),
    CONSTRAINT chk_payment_orders_status
        CHECK (status IN ('CREATED'))
);

CREATE INDEX idx_payment_orders_merchant_created
    ON payment_orders (merchant_id, created_at DESC, payment_order_id ASC);

CREATE TABLE idempotency_records (
    idempotency_record_id    UUID PRIMARY KEY,
    merchant_id              UUID NOT NULL,
    idempotency_key_hash     VARCHAR(64) NOT NULL,
    request_fingerprint_hash VARCHAR(64) NOT NULL,
    payment_order_id         UUID,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMPTZ,
    CONSTRAINT fk_idempotency_records_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id),
    CONSTRAINT fk_idempotency_records_payment_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT uk_idempotency_records_merchant_key
        UNIQUE (merchant_id, idempotency_key_hash),
    CONSTRAINT uk_idempotency_records_payment_order
        UNIQUE (payment_order_id),
    CONSTRAINT chk_idempotency_records_key_hash
        CHECK (idempotency_key_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_idempotency_records_fingerprint_hash
        CHECK (request_fingerprint_hash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE payment_order_status_history (
    status_history_id UUID PRIMARY KEY,
    payment_order_id  UUID NOT NULL,
    from_status       VARCHAR(20),
    to_status         VARCHAR(20) NOT NULL,
    actor_subject     VARCHAR(200) NOT NULL,
    correlation_id    VARCHAR(128) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order_status_history_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT chk_payment_order_status_history_from_status
        CHECK (from_status IS NULL OR from_status IN ('CREATED')),
    CONSTRAINT chk_payment_order_status_history_to_status
        CHECK (to_status IN ('CREATED'))
);

CREATE INDEX idx_payment_order_status_history_order_created
    ON payment_order_status_history (payment_order_id, created_at ASC, status_history_id ASC);
