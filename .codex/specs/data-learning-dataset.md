# Data Learning Dataset (SMALL)

## Problem Statement

I can practise HTTP and UI contracts on 104 deterministic payment fixtures, but I cannot practise SQL, data quality, or reconciliation on realistic volumes. Seeded payments often sit in a terminal status while history only records `CREATED`. Checkout, audit, and event-publication tables are empty or incidental after a normal seed. I need a second, fully deterministic OLTP world I can load on purpose, with exact expected counts, without breaking the 104-row test contract or production startup.

## Solution

Add a sibling learning seed (`DataLearningDataset`, profile `SMALL`) next to the existing deterministic seed. One explicit test-only HTTP call replaces OLTP with a skewed, time-ranged population: five tenants, twenty merchants, 10 000 payments with full legal status histories, checkout sessions/events/fulfillments/anomalies, audit events with before/after JSON, and synthetic event-publication rows. A `DataLearningTruth` document is the reconciliation oracle. Known lab tenant/merchant identities stay so Keycloak and the dashboard still work. The 104-row seed, payment domain APIs, and prod boot stay unchanged.

## User Stories

1. As an SDET learner, I want 10 000 payment orders with complete legal status histories, so that I can write `LAG`/`LEAD`/`ROW_NUMBER` queries and find illegal transitions.
2. As an SDET learner, I want uneven tenant and status distributions, so that I see data skew in aggregates and plans.
3. As an SDET learner, I want checkout sessions, events, fulfillments, and about fifty controlled anomalies, so that I can practise protocol reconciliation without calling the checkout HTTP API.
4. As an SDET learner, I want 10 000 audit events including `before_state`/`after_state` JSON, so that I can unnest field-level changes.
5. As an SDET learner, I want 10 000 event-publication rows with success/retry/incomplete mix, so that I can compute failure rate, retry rate, and unfinished work.
6. As an SDET learner, I want a truth manifest of exact counts, so that reconciliation is expected-minus-actual, not a guess.
7. As a contract tester, I want the existing 104-row seed and `/api/test/seed` behaviour unchanged, so that REST Assured and Playwright keep a stable oracle.
8. As a platform operator, I want the learning seed unavailable in prod and off the default boot path, so that a large dataset cannot appear in a real run.
9. As a dashboard user in the lab, I want `TENANT_ALPHA` / `MERCHANT_ALPHA_001` to still exist after a learning seed, so that my usual Keycloak login shows a full payment list instead of an empty tenant.
10. As a maintainer, I want two consecutive learning seeds to produce identical identities and aggregates, so that the dataset is a stable teaching fixture.

## Implementation Decisions

### Two worlds

- `DeterministicDataset` remains the only payload of `POST /api/test/seed` and `POST /api/test/reset`. Do not add history rows to the 104 fixtures.
- `DataLearningDataset` is a sibling in the testing module. `POST /api/test/seed-learning` clears business tables (same family as reset, plus `audit_event` and `event_publication`) and loads `SMALL`. It does not overlay the 104 payments.
- No Flyway data, no `dev` startup hook, no `MEDIUM`/`SCALE` implementation.

### Identities (known doors + synthetic crowd)

- Keep fixture tenant and merchant UUIDs and references (three tenants, four merchants).
- Add two `LEARN_*` tenants and sixteen `LEARN_*` merchants (stable UUIDs from name-based bytes). Total: 5 tenants, 20 merchants.
- Tenant payment skew: `TENANT_ALPHA` 55%, `PLATFORM_TENANT` 20%, first `LEARN_*` tenant 15%, second `LEARN_*` tenant 8%, `PLACEHOLDER_TENANT` (SUSPENDED) 2%.
- About 55% of payments belong to `MERCHANT_ALPHA_001` under `TENANT_ALPHA`.
- Re-seed the existing two RLS lab rows after a learning load so that lab is not emptied.

### Payments and history

- 10 000 orders. Status share by index `i % 100`: `CAPTURED` 60, `REFUNDED` 12, `CANCELLED` 8, `AUTHORIZED` 8, `EXPIRED` 4, `CREATED` 8.
- Exact status counts: 6000 / 1200 / 800 / 800 / 400 / 800.
- History is the legal machine only:
  - `CREATED`
  - `CREATED → AUTHORIZED`
  - `CREATED → AUTHORIZED → CAPTURED`
  - `CREATED → AUTHORIZED → CAPTURED → REFUNDED`
  - `CREATED → CANCELLED`
  - `CREATED → AUTHORIZED → EXPIRED`
- Exact history row count: 28 000.
- `from_status` is null only on the creation row; later rows set `from_status`, `to_status`, timestamps, and allowed `action` values to satisfy CHECKs.
- No `payment_refund_approvals` in `SMALL`.
- Amounts/currencies only `PLN`/`EUR`/`USD` and existing amount CHECKs. No illegal currency.
- Created-at range: 2025-01-01 through 2026-08-15, deterministic offset from a fixed base plus index.

### Checkout

- 2000 sessions; 1950 fulfillments (50 sessions missing fulfillment = business anomaly); 50 `checkout_anomaly` rows; 5000 events.
- Session status values must match CHECK: `CREATED`, `PENDING`, `COMPLETED`, `CANCELED`, `EXPIRED`, `REFUNDED` (American `CANCELED` on the session).
- Scenarios mixed by index: happy completed, canceled, expired, refunded, event retry, 503 ACK, duplicate event, missing fulfillment, wrong fulfillment. All rows remain constraint-legal.
- Do not drive checkout HTTP or pollers to build this data.

### Audit

- 10 000 rows. Actions from `MERCHANT_CREATED`, `MERCHANT_ACTIVATED`, `MERCHANT_SUSPENDED`, `MERCHANT_RISK_FLAGGED`, `PAYMENT_CREATED`, `PAYMENT_CAPTURED`, `PAYMENT_REFUNDED`.
- `tenant_id` on audit is the string tenant reference (existing column type), skewed like payments.
- A representative subset includes both `before_state` and `after_state` JSON objects (not all rows must have both).
- Outcomes only `SUCCESS`, `DENIED`, `FAILED`.

### Event publication

- 10 000 JDBC inserts into `event_publication`. Do not publish application events.
- Mix by index: 9000 completed on first attempt, 700 one retry, 200 multiple retries, 100 not completed (`completion_date` null).
- Fill `publication_date`, `completion_attempts`, `status`, `event_type`, and `serialized_event` with small deterministic payloads.

### Persistence and module boundary

- Generate and insert from the testing module with JDBC batches. Do not extend `PaymentSeedCapability` / `PaymentOrderSeed`. Do not call authorize/capture/refund services.
- Public payment/merchant/tenant APIs stay as they are. Learning code stays under the testing module’s internal seed package.
- Determinism: no unseeded `Random`; name-based UUIDs; fixed epoch and index rules. Seed N equals seed N.

### HTTP and safety

- `POST /api/test/seed-learning` on the existing test controller (testing flag + not prod).
- Permit the new POST in the test-endpoint security pass-through, same as `/seed` and `/reset`.
- 200 body: operation `seed-learning`, status `completed`, plus the truth document (counts and key aggregates). Do not change the `/seed` body.
- Optional query `profile=SMALL` only; other profiles 400. Default SMALL.
- `/api/test/seed` and `/api/test/reset` behaviour, status codes, and 104-row oracle unchanged.

### DataLearningTruth (SMALL)

Must match the generator exactly:

| Metric | Expected |
|---|---|
| tenants | 5 |
| merchants | 20 |
| payments | 10000 |
| paymentHistoryRows | 28000 |
| capturedPayments | 6000 |
| refundedPayments | 1200 |
| cancelledPayments | 800 |
| authorizedPayments | 800 |
| expiredPayments | 400 |
| createdPayments | 800 |
| tenantAlphaPayments | 5500 |
| checkoutSessions | 2000 |
| checkoutEvents | 5000 |
| checkoutFulfillments | 1950 |
| checkoutAnomalies | 50 |
| auditEvents | 10000 |
| publicationEvents | 10000 |
| failedPublications (incomplete) | 100 |

## Testing Decisions

What a good test is: observable counts, identities, and HTTP safety — not generator internals. Two loads of `SMALL` must yield the same truth, the same payment ids for index 0 and 9999, and the same history length per those ids.

Seams:

- **Domain / Spring Testcontainers** (primary): load `SMALL` twice against Postgres; assert `DataLearningTruth` and pinned identities. Assert every payment’s history chain is a legal prefix of the machine. Assert no CHECK/FK violation on insert.
- **REST Assured HTTP** (existing testing-support tests): `POST /api/test/seed-learning` is 200 when testing is on and profile is not prod; 404 when testing is off; 404 on prod profile even if the flag is true. `POST /api/test/seed` still 200 and still 104 payments. Learning seed must not be callable without the same gates as `/seed`.
- **Not Playwright E2E / Playwright REST** for the 10 000-row load (too slow, not the teaching seam).

Prior art: `DeterministicDatasetTest`, `TestEndpointsEnabledIT`, `TestEndpointsDisabledIT`, `TestEndpointsProdSafetyIT`, `TestEndpointSecurityChainTest`, `FixturesTest`.

Existing `DeterministicDataset` and `Fixtures` tests must stay green with no assertion changes.

## Out of Scope

- Iceberg, Spark, Kafka, Flink, Polaris, bronze/staging files, technically invalid OLTP rows
- `MEDIUM` / `SCALE` profiles
- Changing 104 fixtures or adding full history to `DeterministicDataset`
- Dual-control refund approval rows
- Changing payment/checkout/audit production behaviour to make seeding easier
- Top-level `POST /payments`, real PSP, settlement, KYC
- Auto-seed on `dev` or prod boot
- Frontend pages or Playwright journeys for the learning world

## Further Notes

Grill closed 2026-08-17. ADR `0001-data-learning-dataset`. Glossary: `.codex/CONTEXT.md`.

Checkout session spelling is `CANCELED`; fulfillment may use `CANCELLED` — that split already exists in the schema and is intentional.

Next process step: `to-tickets` (vertical slices), then `implement` per ticket with `tdd` and `spring-modulith` (testing module stays behind public seed capabilities; JDBC is the learning adapter).
