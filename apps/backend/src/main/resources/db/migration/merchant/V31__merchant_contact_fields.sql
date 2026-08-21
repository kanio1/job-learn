-- V31__merchant_contact_fields.sql
-- Ops Wave 2 Fala 1: contact fields for concurrent merchant edit.
-- merchants.version already exists (V1). No index (not a list filter).

ALTER TABLE merchants
    ADD COLUMN contact_phone   VARCHAR(32),
    ADD COLUMN contact_address VARCHAR(200);
