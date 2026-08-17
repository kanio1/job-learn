# 0001 — Separate DataLearningDataset from DeterministicDataset

Status: accepted
Date: 2026-08-17

## Context

The lab seed loads 104 payment orders whose current status is often terminal (`CAPTURED`, `REFUNDED`, …) but `payment_order_status_history` contains only `CREATED`. That is enough for HTTP/UI contract tests and too little for SQL, window functions, and reconciliation practice.

Changing those 104 rows or their history would break tests that depend on exact counts and identities. Overlaying 10 000 extra payments on the same seed would also break those tests if the wrong seed ran. Generating learning data through payment domain services would be slow, non-deterministic in time, and would widen the public payment seed API for a teaching concern.

## Decision

Keep `DeterministicDataset` untouched. Add a sibling `DataLearningDataset` (`SMALL` first) loaded only by an explicit learning seed HTTP operation, gated like the existing test seed (testing flag, not prod). Loading it replaces OLTP contents; it does not overlay the 104 fixtures.

Persist learning rows with JDBC batches from the testing module. Do not extend `PaymentSeedCapability` and do not publish application events to fill `event_publication`.

Keep known tenant/merchant identities so a normal lab login still sees data; add a small synthetic crowd for skew.

## Consequences

- HTTP/UI tests keep using the existing seed; learning SQL uses the learning seed. Mixing them in one DB state is unsupported.
- After a learning seed the dashboard is full (mostly `TENANT_ALPHA`) and no longer matches 104-row expectations.
- `event_publication` learning rows are synthetic inserts, not a live Modulith log.
- Dual-control refund approval rows are omitted in `SMALL`; refunded payments still have a legal status history.
- Technical defects stay out of OLTP until a later staging dataset exists.
