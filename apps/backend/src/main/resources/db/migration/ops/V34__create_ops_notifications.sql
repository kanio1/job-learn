-- V34__create_ops_notifications.sql
-- Inbox + optional feed replay. Not Spring Modulith event_publication.

CREATE TABLE ops_notifications (
    notification_id   UUID PRIMARY KEY,
    recipient_subject VARCHAR(255) NOT NULL,
    event_id          UUID         NOT NULL,
    event_type        VARCHAR(64)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(500) NOT NULL,
    payload           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ops_notifications_recipient_event
        UNIQUE (recipient_subject, event_id)
);

CREATE INDEX IF NOT EXISTS idx_ops_notifications_recipient_unread
    ON ops_notifications (recipient_subject, created_at DESC)
    WHERE read_at IS NULL;

CREATE TABLE ops_feed_event (
    event_id          UUID PRIMARY KEY,
    occurred_at       TIMESTAMPTZ  NOT NULL,
    merchant_id       UUID,
    payment_order_id  UUID,
    event_type        VARCHAR(64)  NOT NULL,
    label             VARCHAR(200) NOT NULL,
    raw_payload       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ops_feed_event_occurred
    ON ops_feed_event (occurred_at DESC, event_id ASC);
