# Implementation Plan: Payment Lifecycle Foundation

**Branch**: `009-payment-lifecycle-foundation` | **Date**: 2026-06-04 | **Spec**: `specs/009-payment-lifecycle-foundation/spec.md`

**Input**: Feature specification from `specs/009-payment-lifecycle-foundation/spec.md`

**Note**: This file is the `/speckit.plan` output for Lesson 14 Payment Lifecycle Foundation.

## Summary

Extend the existing payment Spring Modulith module from create/read/list/summary into a realistic payment lifecycle foundation. The implementation adds authorize, capture, cancel, refund, metadata PATCH, and status-history contracts over the current payment order aggregate, reusing existing idempotency, JWT role mapping, correlation ID, PostgreSQL/Flyway, and Nuxt payment-detail foundations while adding state-machine validation, conditional updates with `If-Match`, lifecycle audit records, HTTP protocol headers, and dev/test CORS.

The design intentionally remains synchronous and bounded for Lesson 14: no Kafka, webhooks, scheduled expiration job, PSP failure simulation, complete OAuth/OIDC integration, complete dashboard, API versioning, HATEOAS, or load/performance testing.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4, Spring Framework 7, TypeScript 6, Nuxt 4

**Primary Dependencies**: Spring Modulith 2.0.6, Spring Security resource server, Spring Data JPA, Flyway, Maven 3.9.11, Nuxt UI, Pinia, Zod

**Storage**: PostgreSQL 18 via existing Flyway-managed `payment` migrations

**Testing**: JUnit 6, AssertJ, REST Assured 6, Testcontainers 2.0.5, Spring Modulith verification, Playwright 1.60, frontend `corepack pnpm typecheck`

**Target Platform**: Local Linux development and test environment, JVM backend service, Nuxt dashboard frontend, PostgreSQL container-backed integration tests

**Project Type**: Modular monolith backend plus Nuxt dashboard frontend

**Performance Goals**: Lifecycle actions respond within 500ms under normal single-user local development conditions; no load/performance test deliverable in this feature

**Constraints**: Preserve Phase 0 guardrails outside approved Lesson 14 scope; no `POST /payments`, Kafka, webhooks, scheduled expiration job, PSP failure simulation, complete OAuth/OIDC integration, complete business dashboard, API versioning, HATEOAS, or rate limiting; CORS is dev/test profile only; lifecycle transitions and audit inserts are transactional

**Scale/Scope**: Four lifecycle actions, six statuses, one metadata PATCH contract, one status-history contract, one payment module owner, one frontend detail/history display path, existing regression suite retained

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Tester-Led Product Learning**: PASS. Feature exposes state-machine, concurrency, idempotency, CORS, cache-header, audit, and amount-boundary risks; tester analysis is captured in `spec.md`, `research.md`, and future `/speckit.tasks` labels.
- **Spec-Driven Delivery**: PASS. `spec.md` contains business purpose, actors, scope, requirements, non-functional requirements, acceptance scenarios, assumptions, and measurable success criteria; no `NEEDS CLARIFICATION` items remain.
- **Modular Monolith Boundaries**: PASS. Ownership remains in the existing `payment` module; merchant eligibility stays through the existing public merchant port; no new cross-module dependency or event pipeline is introduced.
- **Parallel-Ready Quality Engineering**: PASS. Planned tests use isolated merchants, UUID references, idempotency keys, correlation IDs, and Testcontainers database isolation; tasks marked parallel must touch independent files or data scopes.
- **Security, Data Integrity, and Observability**: PASS. Plan includes lifecycle roles, merchant ownership, structured errors, optimistic locking, transaction boundaries, status-history audit, correlation IDs, and INFO-level lifecycle logs.

## Project Structure

### Documentation (this feature)

```text
specs/009-payment-lifecycle-foundation/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── payment-lifecycle-api.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
apps/backend/src/main/java/lab/paymentquality/payment/
├── package-info.java
└── internal/
    ├── application/
    ├── domain/
    ├── infrastructure/
    └── web/

apps/backend/src/main/java/lab/paymentquality/shared/security/
├── SecurityConfig.java
└── KeycloakRealmRoleConverter.java

apps/backend/src/main/resources/db/migration/payment/
├── V2__create_payment_orders.sql
├── V3__add_payment_order_list_indexes.sql
└── V4__add_payment_lifecycle.sql

apps/backend/src/test/java/lab/paymentquality/
├── architecture/
├── payment/
├── rest/
└── security/

apps/frontend/app/
├── schemas/payment-order.schema.ts
├── stores/payment-orders.ts
├── components/payment/
└── pages/admin/merchants/[merchantId]/payments/

apps/frontend/server/api/merchants/[merchantId]/payment-orders/
├── [paymentOrderId].get.ts
├── [paymentOrderId].patch.ts
├── [paymentOrderId]/history.get.ts
├── [paymentOrderId]/authorize.post.ts
├── [paymentOrderId]/capture.post.ts
├── [paymentOrderId]/cancel.post.ts
└── [paymentOrderId]/refund.post.ts
```

**Structure Decision**: Use the existing backend `payment` module and existing frontend payment feature folders. New lifecycle behavior stays inside `lab.paymentquality.payment.internal` except role matchers in shared security and Nuxt server proxy routes. No new backend module, event module, external PSP service module, or test-support package is introduced by the plan.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Phase 0: Research Summary

Research decisions are documented in `specs/009-payment-lifecycle-foundation/research.md`.

Key resolved decisions:
- Use the existing payment aggregate and payment module ownership.
- Extend the existing PostgreSQL schema with V4 instead of modifying V2/V3.
- Use synchronous lifecycle service methods and an in-process always-success PSP mock.
- Use `If-Match` plus payment-order version for lifecycle updates.
- Reuse idempotency key hashing and fingerprint storage patterns for lifecycle actions.
- Keep frontend scope display-only: status, timestamps, and lifecycle history timeline.

## Phase 1: Design Summary

Design artifacts are documented in:
- `specs/009-payment-lifecycle-foundation/data-model.md`
- `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`
- `specs/009-payment-lifecycle-foundation/quickstart.md`

Implementation-sensitive design constraints:
- Existing payment-order ETags currently include the payment order ID; feature 009 requires lifecycle `If-Match` tokens in the `"v{version}"` format. Implementation tasks must update payment-order ETag generation consistently or explicitly translate existing read/create ETags into the lifecycle contract.
- Existing status history currently has creation-entry support; the feature 009 history endpoint is scoped to lifecycle transitions only. Implementation tasks must either stop exposing creation entries through the lifecycle history contract or filter them from `GET /history`.
- Existing `PaymentOrder.version` already exists; V4 should extend constraints and behavior, not add a duplicate version column.
- Existing `payment_order_status_history` already exists; V4 should extend allowed statuses and required lifecycle audit fields, not create a duplicate table.

## Post-Design Constitution Check

- **Tester-Led Product Learning**: PASS. Data model and contracts expose state transitions, boundary values, concurrency risks, idempotency replay/conflict behavior, denial paths, and HTTP headers for tester design.
- **Spec-Driven Delivery**: PASS. All design artifacts trace back to `FR-LIFECYCLE`, `FR-STATE`, `FR-LOCKING`, `FR-IDEMPOTENCY`, `FR-AUDIT`, `FR-HTTP`, `FR-DB`, `FR-SEC`, and `FR-PSP` requirements.
- **Modular Monolith Boundaries**: PASS. `payment` owns lifecycle behavior; shared security only enforces route authorization; no new outbound module dependency is planned.
- **Parallel-Ready Quality Engineering**: PASS. Quickstart and future tasks should use unique merchants, order references, idempotency keys, and correlation IDs; no shared static lifecycle data is required.
- **Security, Data Integrity, and Observability**: PASS. Design includes role matrix, ownership checks, transactional status/audit persistence, structured error contracts, and correlation IDs.

## Phase 2 Boundary

This `/speckit.plan` output stops before task generation. `specs/009-payment-lifecycle-foundation/tasks.md` must be created by `/speckit.tasks` and should preserve the lab labels `AGENT-IMPLEMENT`, `AGENT-EXPLAIN`, `TESTER-ANALYZE`, `TESTER-DESIGN`, `TESTER-AUTOMATE`, `AGENT-REVIEW`, and `DISCUSS`.
