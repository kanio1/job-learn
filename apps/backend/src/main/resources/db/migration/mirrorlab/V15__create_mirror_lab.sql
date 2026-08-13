-- Educational Mirror Lab tables. Does not alter payment_orders.

CREATE TABLE mrl_disputes (
    dispute_id  UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_mrl_dispute_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE mrl_dispute_evidence (
    evidence_id  UUID PRIMARY KEY,
    dispute_id   UUID NOT NULL REFERENCES mrl_disputes (dispute_id),
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mrl_refund_approvals (
    approval_id      UUID PRIMARY KEY,
    merchant_id      UUID NOT NULL,
    amount_minor     BIGINT NOT NULL,
    status           VARCHAR(32) NOT NULL,
    maker_subject    VARCHAR(128) NOT NULL,
    checker_subject  VARCHAR(128),
    step_up_until    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_mrl_approval_status CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED'))
);

CREATE TABLE mrl_consents (
    consent_id     UUID PRIMARY KEY,
    access_token   VARCHAR(64) NOT NULL UNIQUE,
    status         VARCHAR(32) NOT NULL,
    owner_subject  VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at     TIMESTAMPTZ,
    CONSTRAINT chk_mrl_consent_status CHECK (status IN ('GRANTED', 'REVOKED'))
);
