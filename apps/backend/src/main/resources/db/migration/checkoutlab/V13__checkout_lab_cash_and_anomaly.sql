-- Cash bookings may confirm without a PSP session. Session vs fulfillment
-- spelling (CANCELED vs CANCELLED) is intentional — two dictionaries.

ALTER TABLE checkout_fulfillment
    ALTER COLUMN session_id DROP NOT NULL;

ALTER TABLE checkout_event
    ADD COLUMN ack_status INT;

CREATE TABLE checkout_anomaly (
    anomaly_id   UUID PRIMARY KEY,
    session_id   UUID,
    kind         VARCHAR(64) NOT NULL,
    detail       TEXT NOT NULL,
    detected_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checkout_anomaly_detected
    ON checkout_anomaly (detected_at DESC);
