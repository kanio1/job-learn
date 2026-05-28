---
type: project-phase
status: active
project: Payment Quality Engineering Lab
phase: 2
area: Payment Orders
date: 2026-05-27
tags:
  - payment-quality-lab
  - payment-order
  - phase-2
  - sdet
  - rest-assured
  - java-25
  - postgresql
  - flyway
  - security-testing
---

# Phase 2 - Payment Orders

Phase 2 introduces the first payment-specific vertical slice: create/read payment order foundation with idempotent creation, merchant-scoped access, platform read access, PostgreSQL/Flyway persistence, Keycloak role/claim behavior, Nuxt API consumption and REST Assured tests.

## Links

- Lesson 6: `Lesson 06 - Payment Order Create Read Foundation.md`
- Spec: `specs/003-payment-order-access-lifecycle/spec.md`
- Plan: `specs/003-payment-order-access-lifecycle/plan.md`
- Tasks: `specs/003-payment-order-access-lifecycle/tasks.md`
- API contract: `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md`
- Data model: `specs/003-payment-order-access-lifecycle/data-model.md`
- Quickstart: `specs/003-payment-order-access-lifecycle/quickstart.md`
- Learning plan: `../Payment Gateway SDET Learning Plan.md`
- Lesson evidence tracker: `../Learning Governance/Lesson Evidence Tracker.md`
- Competency matrix: `../Learning Governance/Senior SDET Competency Coverage Matrix.md`

## Learning Themes

- Payment Order REST API create/read as the first payment-specific capability.
- `Idempotency-Key` and request fingerprint as retry-safety mechanisms.
- Tenant isolation through roles plus `merchant_id` ownership claim.
- PostgreSQL constraints as business/data safety net.
- Flyway migration as executable schema contract.
- REST Assured assertions for status, headers, body and error contracts.
- Spring Modulith boundary from payment to public merchant eligibility API.
- Frontend as a real API consumer that must preserve idempotency semantics.

## Deferred Scope

- No authorize/capture/cancel actions.
- No PSP integration.
- No Kafka, webhooks, refunds, settlement or reconciliation.
- No `If-Match`, `412` or optimistic action concurrency yet.
- No complete payment dashboard or list/reporting API.
