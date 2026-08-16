CREATE TABLE payment_export_jobs (
    job_id         UUID PRIMARY KEY,
    merchant_id    UUID NOT NULL,
    status         VARCHAR(20) NOT NULL,
    csv_content    TEXT,
    error_message  VARCHAR(500),
    idempotency_key VARCHAR(255),
    created_by     VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ,
    CONSTRAINT chk_payment_export_jobs_status
        CHECK (status IN ('PENDING', 'READY', 'FAILED')),
    CONSTRAINT uk_payment_export_jobs_idempotency
        UNIQUE (merchant_id, idempotency_key)
);

CREATE INDEX idx_payment_export_jobs_pending
    ON payment_export_jobs (status, created_at ASC)
    WHERE status = 'PENDING';
