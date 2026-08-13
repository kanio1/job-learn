ALTER TABLE checkout_session DROP CONSTRAINT chk_checkout_session_status;
ALTER TABLE checkout_session ADD CONSTRAINT chk_checkout_session_status
    CHECK (status IN (
        'CREATED', 'PENDING', 'COMPLETED', 'CANCELED', 'EXPIRED', 'REFUNDED'
    ));
