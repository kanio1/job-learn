ALTER TABLE payment_order_evidence
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN content_bytes BYTEA;

ALTER TABLE payment_order_evidence
    ADD CONSTRAINT chk_payment_order_evidence_category
        CHECK (category IN ('INVOICE', 'RECEIPT', 'OTHER'));
