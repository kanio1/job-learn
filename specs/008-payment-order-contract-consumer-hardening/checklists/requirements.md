# Requirements Checklist: Payment Order Contract and Consumer Hardening

**Feature**: `008-payment-order-contract-consumer-hardening`

## Spec Quality

- [x] Feature purpose is stated as production/system hardening, not lesson/test implementation.
- [x] Backend list validation behavior is explicit.
- [x] Backend create protocol behavior is explicit.
- [x] Error response semantics are explicit.
- [x] Frontend dashboard/store/Zod/error behavior is explicit.
- [x] Database no-change decision is explicit.
- [x] Keycloak/security no-change decision is explicit.
- [x] Phase 0 guardrails are captured as non-goals.
- [x] No planning-critical clarification remains unresolved.

## Acceptance Coverage

- [x] Invalid page and size cases are covered.
- [x] Unsupported status and currency cases are covered.
- [x] Invalid date and date range cases are covered.
- [x] Invalid amount and amount range cases are covered.
- [x] Unsupported sort case is covered.
- [x] Page beyond last page success case is covered.
- [x] Malformed JSON case is covered.
- [x] Unsupported media type case is covered.
- [x] Missing `Idempotency-Key` case is covered.
- [x] Existing create/replay headers and idempotency behavior are preserved.
- [x] Existing authorization behavior is preserved.
- [x] Frontend create/detail dashboard and navigation behavior is covered.
- [x] Frontend detail/create store ownership and Zod parsing are covered.
- [x] Frontend `403`, detail `404`, backend unavailable and create failure states are covered.

## Scope Guardrails

- [x] No `POST /payments` scope is introduced.
- [x] No lifecycle actions or statuses are introduced.
- [x] No PSP, Kafka, webhook, outbox, event or async behavior is introduced.
- [x] No complete OAuth/OIDC application integration is introduced.
- [x] No new Keycloak role or realm JSON change is planned.
- [x] No database migration is planned by default.
- [x] No fake dashboard analytics/KPIs are planned.
- [x] No new REST Assured tests are planned.
- [x] No new Playwright specs are planned.
- [x] No test-support clients/builders/specs are planned.

## Task Generation Readiness

- [x] `/speckit.tasks` can generate implementation-only tasks.
- [x] Regression verification uses existing commands only.
- [x] New test creation is explicitly excluded from task generation.
- [x] Lesson 13 work is deferred as future quality-engineering work.
