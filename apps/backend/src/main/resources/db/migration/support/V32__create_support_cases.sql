-- Support work queue. Not payment_orders.status.

CREATE TABLE support_cases (
    case_id             UUID PRIMARY KEY,
    case_reference      VARCHAR(32)  NOT NULL,
    tenant_id           UUID         NOT NULL REFERENCES tenants (tenant_id),
    merchant_id         UUID         NOT NULL REFERENCES merchants (merchant_id),
    payment_order_id    UUID         REFERENCES payment_orders (payment_order_id),
    status              VARCHAR(20)  NOT NULL,
    priority            VARCHAR(10)  NOT NULL,
    assignee_subject    VARCHAR(255),
    title               VARCHAR(200) NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_support_cases_reference UNIQUE (case_reference),
    CONSTRAINT chk_support_cases_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING', 'RESOLVED')),
    CONSTRAINT chk_support_cases_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH'))
);

CREATE INDEX IF NOT EXISTS idx_support_cases_tenant_status_updated
    ON support_cases (tenant_id, status, updated_at DESC, case_id ASC);

CREATE INDEX IF NOT EXISTS idx_support_cases_assignee
    ON support_cases (assignee_subject, status)
    WHERE assignee_subject IS NOT NULL;
