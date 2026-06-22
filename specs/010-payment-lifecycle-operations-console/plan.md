# Implementation Plan: Payment Lifecycle Operations Console

**Branch**: `010-payment-lifecycle-operations-console` | **Date**: 2026-06-05 | **Spec**: `specs/010-payment-lifecycle-operations-console/spec.md`

**Input**: Feature specification from `specs/010-payment-lifecycle-operations-console/spec.md`

**Note**: This file is the `/speckit.plan` output for Lesson 14 / Feature 010, building on Lesson 14 / Feature 009 Payment Lifecycle Foundation.

## Summary

Deliver the application-facing operations console for the Feature 009 payment lifecycle foundation. The plan focuses on a Nuxt detail page that shows lifecycle summary, chronological history, state-aware action controls, separate metadata editing, stale-state feedback, role-aware affordances, and safe lifecycle error handling.

The implementation approach is frontend-led: extend existing Nuxt payment schemas, Pinia store state/actions, detail components/page behavior, and Nuxt server API proxy routes so the browser-facing application preserves `Authorization`, `ETag`/version, `If-Match`, `Idempotency-Key`, `X-Correlation-ID`, backend status codes, and backend error body shape. Backend changes are allowed only if existing Feature 009 responses are insufficient for the application display contract; no new lifecycle semantics, REST Assured framework work, new test classes, PSP failures, Kafka/webhooks, full dashboard, or complete OAuth/OIDC integration are part of this feature.

## Lesson 14 / Feature 009 Context

This feature carries forward the Lesson 14 / Feature 009 labels and lifecycle foundation. It assumes the lifecycle REST surface, lifecycle application service, lifecycle domain state, lifecycle status vocabulary, status history/audit model, PSP mock boundary, lifecycle HTTP error mapping, security/CORS support, frontend lifecycle schemas, payment detail component, Nuxt proxy boundary, and lifecycle proxy routes exist or are the immediate dependency.

### Co nowego technicznie dochodzi

- Specific HTTP headers: `ETag`, `If-Match`, `Idempotency-Key`, `X-Correlation-ID`, `Cache-Control`, `Vary`, `Authorization`, `Access-Control-Allow-Origin`, `Access-Control-Allow-Headers`, and `Access-Control-Expose-Headers`.
- Specific response codes and error categories: `200`, `400`, `401`, `403`, `404`, `409`, `412`, `415`, `422`, optional `406`, and `OPTIONS 200` for CORS preflight behavior.
- Business-technical flows: create order -> read version marker -> submit lifecycle action with conditional update, authorize -> capture -> history, stale version -> `412` -> refresh state, retry-safe action with idempotency key, forbidden action by wrong role, platform operator vs merchant operator, and frontend -> Nuxt proxy -> backend -> database/history visibility.
- Modern application boundary behavior: API auth failures remain backend `401`/`403` categories, browser login/redirect behavior belongs to the app layer, proxy requests preserve lifecycle headers and backend error shape, and history/audit makes database effects visible without exposing secrets.

## Technical Context

**Language/Version**: TypeScript 6, Nuxt 4, Java 25 and Spring Boot 4 only for minimal response-contract adjustments if required

**Primary Dependencies**: Nuxt UI, Pinia, Zod, existing Nuxt server API routes, existing Spring Modulith payment module, existing Feature 009 lifecycle REST API

**Storage**: PostgreSQL 18 through existing backend payment tables; no new database table or lifecycle schema is planned for Feature 010

**Testing**: Verification commands only for this planning scope: frontend typecheck/build where available, backend compile/test regression where touched; no REST Assured framework work, no new backend test classes, and no frontend E2E feature deliverable

**Target Platform**: Local Linux development, Nuxt dashboard frontend, JVM backend service, browser through Nuxt server proxy

**Project Type**: Modular monolith backend plus Nuxt dashboard frontend; Feature 010 is application-only with Nuxt as the primary implementation surface

**Performance Goals**: Primary lifecycle detail content should be understandable within 2 seconds under normal local development once backend responses are available; action availability must be deterministic for the same status/role context

**Constraints**: Preserve explicit non-goals: no REST Assured framework work, no new test classes as feature deliverables, no multi-capture, no multi-refund, no PSP failures, no PSP provider integration, no Kafka/webhooks, no full dashboard, no complete OAuth/OIDC integration, no new lifecycle states/transitions, and no `POST /payments` scope

**Scale/Scope**: One payment detail route, one lifecycle summary/action/history UI area, one metadata edit flow, six Feature 009 statuses, four lifecycle actions, existing Nuxt lifecycle proxy routes, and minimal backend response-contract fallback only if required

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Tester-Led Product Learning**: PASS. The plan exposes application risks around stale state, header forwarding, state-to-action mapping, role affordances, metadata confusion, history interpretation, and error flattening without turning the feature into a test-framework deliverable.
- **Spec-Driven Delivery**: PASS. `spec.md` contains business purpose, actors, scope, functional and non-functional requirements, acceptance scenarios, assumptions, measurable success criteria, and no unresolved clarification block.
- **Modular Monolith Boundaries**: PASS. Existing payment module owns lifecycle behavior. Feature 010 should consume existing contracts through Nuxt; backend impact is limited to existing payment API response fields if the UI cannot otherwise satisfy the display contract.
- **Parallel-Ready Quality Engineering**: PASS. No automated tests are planned as deliverables. Future implementation tasks should be separable across Nuxt schema/store, UI components/page, proxy routes, and optional backend response-contract work.
- **Security, Data Integrity, and Observability**: PASS. Plan preserves backend authorization as final enforcement, forwards user authorization context, preserves conditional/idempotency/correlation headers, avoids token/secret display, and keeps stale-state handling non-retrying.

## Project Structure

### Documentation (this feature)

```text
specs/010-payment-lifecycle-operations-console/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── operations-console-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
apps/frontend/app/
├── schemas/payment-order.schema.ts
├── stores/payment-orders.ts
├── components/payment/
└── pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue

apps/frontend/server/
├── utils/backendApi.ts
└── api/merchants/[merchantId]/payment-orders/
    ├── [paymentOrderId].get.ts
    ├── [paymentOrderId].patch.ts
    └── [paymentOrderId]/
        ├── history.get.ts
        ├── authorize.post.ts
        ├── capture.post.ts
        ├── cancel.post.ts
        └── refund.post.ts

apps/backend/src/main/java/lab/paymentquality/payment/
└── internal/             # Touch only if existing API responses omit required display fields
```

**Structure Decision**: Use the existing Nuxt payment feature folders and existing Nuxt server proxy routes. Do not add a new dashboard area, backend module, test framework package, REST Assured structure, event pipeline, PSP integration, or payment creation flow.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Phase 0: Research Summary

Research decisions are documented in `specs/010-payment-lifecycle-operations-console/research.md`.

Key resolved decisions:
- Keep Feature 010 application-only and Nuxt-first.
- Reuse Feature 009 lifecycle API and add backend work only as response-contract fallback.
- Represent current version from backend `ETag`/version marker and submit it as `If-Match` for lifecycle and metadata mutations.
- Generate per-attempt idempotency keys at the application boundary when the browser does not provide one.
- Preserve backend lifecycle error shape and status codes through the Nuxt server proxy.
- Render lifecycle history oldest-first and keep history loading/error state separate from detail loading/error state.
- Keep metadata editing visually and behaviorally separate from lifecycle actions.

## Phase 1: Design Summary

Design artifacts are documented in:
- `specs/010-payment-lifecycle-operations-console/data-model.md`
- `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`
- `specs/010-payment-lifecycle-operations-console/quickstart.md`

Implementation-sensitive design constraints:
- The Nuxt application must retain the current version marker from the detail response. If the backend exposes only the HTTP `ETag`, the Nuxt proxy/store must make that marker available to the UI without leaking unrelated response internals.
- Lifecycle mutations must use the current version marker as `If-Match` and must not auto-retry after `412` or `422` stale/invalid-transition responses.
- Lifecycle action idempotency keys are single-attempt keys. Reusing a key across different action bodies must surface `409 idempotency_conflict`, not success.
- The Nuxt proxy must not flatten backend `401`, `403`, `404`, `409`, `412`, `422`, or backend-unavailable responses into a single generic success/failure category.
- History display must show only safe backend fields and must not display raw tokens, credentials, idempotency key hashes, or internal secrets.
- Existing `index.post.ts` payment creation may remain in the repository from earlier features, but Feature 010 tasks must not add new payment creation capability or scope.

## Post-Design Constitution Check

- **Tester-Led Product Learning**: PASS. Design artifacts preserve risks and learning prompts around HTTP headers, stale state, idempotency, role affordances, history/audit, and error categories while keeping implementation tasks application-focused.
- **Spec-Driven Delivery**: PASS. Plan artifacts trace Feature 010 user stories and requirements to Nuxt schema/store/page/proxy surfaces and optional backend response-contract checks.
- **Modular Monolith Boundaries**: PASS. Backend lifecycle ownership remains in the existing payment module; no module dependency, event, Kafka, webhook, PSP failure, or OAuth/OIDC completion work is planned.
- **Parallel-Ready Quality Engineering**: PASS. Future tasks can be split by independent frontend schema/store, UI, proxy, docs/verification, and optional backend response-contract files. No new test classes are feature deliverables.
- **Security, Data Integrity, and Observability**: PASS. Contracts require authorization forwarding, safe correlation display, non-secret history/error rendering, conditional mutation headers, and backend-enforced permission outcomes.

## Phase 2 Boundary

This `/speckit.plan` output stops before task generation. `specs/010-payment-lifecycle-operations-console/tasks.md` must be created by `/speckit.tasks` and should preserve the Lesson 14 / Feature 009 context plus lab labels `AGENT-IMPLEMENT`, `AGENT-EXPLAIN`, `TESTER-ANALYZE`, `TESTER-DESIGN`, `TESTER-AUTOMATE`, `AGENT-REVIEW`, and `DISCUSS`.
