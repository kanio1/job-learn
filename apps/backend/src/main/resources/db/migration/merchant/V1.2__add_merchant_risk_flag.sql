-- V1.2__add_merchant_risk_flag.sql
-- Phase 3B-5: Risk Flags RBAC-Gated Merchant Review (F-C6)
ALTER TABLE merchants ADD COLUMN risk_flagged BOOLEAN NOT NULL DEFAULT FALSE;
