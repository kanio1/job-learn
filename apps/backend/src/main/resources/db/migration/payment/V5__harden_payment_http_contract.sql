ALTER TABLE idempotency_records
    DROP CONSTRAINT uk_idempotency_records_merchant_key;

UPDATE idempotency_records
SET action = 'CREATE'
WHERE action IS NULL;

ALTER TABLE idempotency_records
    ALTER COLUMN action SET NOT NULL;

CREATE UNIQUE INDEX uk_idempotency_records_create_scope
    ON idempotency_records (merchant_id, idempotency_key_hash, action)
    WHERE action = 'CREATE';

CREATE UNIQUE INDEX uk_idempotency_records_lifecycle_scope
    ON idempotency_records (merchant_id, payment_order_id, action, idempotency_key_hash)
    WHERE action <> 'CREATE';
