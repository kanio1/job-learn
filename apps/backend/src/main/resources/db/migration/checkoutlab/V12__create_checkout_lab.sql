-- Learning SQL: UUID PK, TIMESTAMPTZ, CHECK, UNIQUE, poller index

CREATE TABLE checkout_session (
    session_id           UUID PRIMARY KEY,
    ext_order_id         VARCHAR(120) NOT NULL,
    amount_minor         BIGINT NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    continue_url         TEXT NOT NULL,
    notify_url           TEXT NOT NULL,
    redirect_uri         TEXT NOT NULL,
    validity_until       TIMESTAMPTZ,
    idempotency_key_hash VARCHAR(64),
    correlation_id       VARCHAR(128) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_checkout_session_amount
        CHECK (amount_minor BETWEEN 1 AND 100000000),
    CONSTRAINT chk_checkout_session_currency
        CHECK (currency IN ('PLN', 'EUR', 'USD')),
    CONSTRAINT chk_checkout_session_status
        CHECK (status IN (
            'CREATED', 'PENDING', 'COMPLETED', 'CANCELED', 'EXPIRED'
        )),
    CONSTRAINT chk_checkout_session_idem_hash
        CHECK (idempotency_key_hash IS NULL
            OR idempotency_key_hash ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX uk_checkout_session_idem
    ON checkout_session (idempotency_key_hash)
    WHERE idempotency_key_hash IS NOT NULL;

CREATE TABLE checkout_event (
    id               UUID PRIMARY KEY,
    event_id         VARCHAR(64) NOT NULL,
    session_id       UUID NOT NULL
        REFERENCES checkout_session (session_id),
    event_type       VARCHAR(64) NOT NULL,
    payload          JSONB NOT NULL,
    signature_header VARCHAR(512),
    received_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    process_status   VARCHAR(32) NOT NULL,
    attempts         INT NOT NULL DEFAULT 0,
    last_error       TEXT,
    CONSTRAINT uk_checkout_event_event_id UNIQUE (event_id),
    CONSTRAINT chk_checkout_event_process_status
        CHECK (process_status IN (
            'RECEIVED', 'PROCESSING', 'DONE', 'FAILED', 'DUPLICATE'
        ))
);

CREATE INDEX idx_checkout_event_poll
    ON checkout_event (process_status, received_at);

CREATE TABLE checkout_fulfillment (
    fulfillment_id   UUID PRIMARY KEY,
    session_id       UUID NOT NULL
        REFERENCES checkout_session (session_id),
    status           VARCHAR(32) NOT NULL,
    source_event_id  VARCHAR(64),
    confirmed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_checkout_fulfillment_session UNIQUE (session_id),
    CONSTRAINT chk_checkout_fulfillment_status
        CHECK (status IN (
            'AWAITING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
        ))
);
