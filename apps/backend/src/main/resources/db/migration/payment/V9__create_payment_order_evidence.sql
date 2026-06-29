CREATE TABLE payment_order_evidence (
    evidence_id       UUID PRIMARY KEY,
    payment_order_id  UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT NOT NULL,
    storage_key       VARCHAR(200) NOT NULL,
    uploaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order_evidence_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT uk_payment_order_evidence_storage_key
        UNIQUE (storage_key),
    CONSTRAINT chk_payment_order_evidence_size
        CHECK (size_bytes BETWEEN 1 AND 2097152),
    CONSTRAINT chk_payment_order_evidence_content_type
        CHECK (content_type IN ('application/pdf', 'image/png', 'image/jpeg', 'text/plain', 'text/csv'))
);

CREATE INDEX idx_payment_order_evidence_order_uploaded
    ON payment_order_evidence (payment_order_id, uploaded_at DESC, evidence_id ASC);
