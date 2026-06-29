-- V10__create_payment_order_notes.sql
-- Phase 3B-6: Internal Notes on Payment Orders (F-C7)
CREATE TABLE payment_order_note (
    id                UUID         PRIMARY KEY,
    payment_order_id  UUID         NOT NULL,
    body              TEXT         NOT NULL,
    author_display    VARCHAR(200) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order_note_payment_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id)
);

CREATE INDEX idx_payment_order_note_order_created
    ON payment_order_note (payment_order_id, created_at ASC);
