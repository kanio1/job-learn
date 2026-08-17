-- Educational Payment ETL lab. Separate schemas from public OLTP on purpose:
-- staging = source copy + batch metadata, dwh = one-row-per-payment facts,
-- etl = batch/watermark bookkeeping. No FKs to payment_orders (snapshot, not live).

CREATE SCHEMA learning_staging;
CREATE SCHEMA learning_dwh;
CREATE SCHEMA learning_etl;

CREATE TABLE learning_staging.payment (
    batch_id                  UUID         NOT NULL,
    source_payment_order_id   UUID         NOT NULL,
    source_merchant_id        UUID         NOT NULL,
    source_tenant_id          UUID         NOT NULL,
    source_tenant_reference   VARCHAR(64)  NOT NULL,
    client_order_reference    VARCHAR(120) NOT NULL,
    amount_minor              BIGINT       NOT NULL,
    currency                  VARCHAR(3)   NOT NULL,
    status                    VARCHAR(20)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,
    authorized_at             TIMESTAMPTZ,
    captured_at               TIMESTAMPTZ,
    cancelled_at              TIMESTAMPTZ,
    refunded_at               TIMESTAMPTZ,
    captured_amount_minor     BIGINT,
    refunded_amount_minor     BIGINT,
    extracted_at              TIMESTAMPTZ  NOT NULL,
    source_updated_at         TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (batch_id, source_payment_order_id)
);

CREATE INDEX idx_learning_staging_payment_batch
    ON learning_staging.payment (batch_id);

-- Grain: one row = one payment order.
CREATE TABLE learning_dwh.fact_payment (
    payment_order_id          UUID         PRIMARY KEY,
    merchant_id               UUID         NOT NULL,
    tenant_id                 UUID         NOT NULL,
    tenant_reference          VARCHAR(64)  NOT NULL,
    client_order_reference    VARCHAR(120) NOT NULL,
    amount_minor              BIGINT       NOT NULL,
    currency                  VARCHAR(3)   NOT NULL,
    source_status             VARCHAR(20)  NOT NULL,
    captured_amount_minor     BIGINT,
    refunded_amount_minor     BIGINT,
    amount_major              NUMERIC(12, 2) NOT NULL,
    is_captured               BOOLEAN      NOT NULL,
    is_refunded               BOOLEAN      NOT NULL,
    is_cancelled              BOOLEAN      NOT NULL,
    is_terminal               BOOLEAN      NOT NULL,
    lifecycle_step_count      INTEGER      NOT NULL,
    capture_duration_seconds  BIGINT,
    source_updated_at         TIMESTAMPTZ  NOT NULL,
    loaded_at                 TIMESTAMPTZ  NOT NULL,
    batch_id                  UUID         NOT NULL
);

CREATE TABLE learning_etl.batch_run (
    batch_id        UUID         PRIMARY KEY,
    pipeline_name   VARCHAR(64)  NOT NULL,
    load_type       VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ,
    watermark_from  TIMESTAMPTZ,
    watermark_to    TIMESTAMPTZ  NOT NULL,
    source_rows     INTEGER,
    staged_rows     INTEGER,
    loaded_rows     INTEGER,
    rejected_rows   INTEGER,
    error_message   TEXT,
    CONSTRAINT chk_learning_etl_batch_run_load_type
        CHECK (load_type IN ('FULL', 'INCREMENTAL')),
    CONSTRAINT chk_learning_etl_batch_run_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_learning_etl_batch_run_pipeline_finished
    ON learning_etl.batch_run (pipeline_name, status, finished_at DESC);
