-- Dedup reconcile anomalies and enforce one row per (session_id, kind).

DELETE FROM checkout_anomaly a
 USING checkout_anomaly b
 WHERE a.anomaly_id > b.anomaly_id
   AND a.session_id IS NOT NULL
   AND a.session_id = b.session_id
   AND a.kind = b.kind;

CREATE UNIQUE INDEX uk_checkout_anomaly_session_kind
    ON checkout_anomaly (session_id, kind)
    WHERE session_id IS NOT NULL;

ALTER TABLE checkout_event
    ADD CONSTRAINT chk_checkout_event_ack_status
        CHECK (ack_status IS NULL OR ack_status IN (200, 202, 400, 503));
