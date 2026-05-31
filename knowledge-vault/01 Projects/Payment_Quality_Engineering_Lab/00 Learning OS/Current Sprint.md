---
type: learning-os
status: active
date: 2026-05-31
tags:
  - learning-os
  - current-sprint
---

# Current Sprint

> **Active Sprint:** Sprint 9 - Payment Orders Frontend Consumer and Contract Alignment
>
> **Phase:** 2 — Payment Orders
>
> **Spec:** Light lesson-extension artifact in `specs/006-payment-orders-frontend-consumer/`, consuming existing backend specs `004` and `005`
>
> **Status:** Ready - Lesson 09 frontend consumer gap implemented and verified

## Sprint Scope

Typed Nuxt Dashboard consumer slice for existing payment order list and summary APIs.

**In scope:**
- Nuxt server proxy for `GET /api/merchants/{merchantId}/payment-orders`
- Nuxt server proxy for `GET /api/merchants/{merchantId}/payment-orders/summary`
- Zod schemas and TypeScript types for payment list and summary responses
- Typed Pinia state/actions for list, summary, current order and errors
- Minimal payments panel with summary cards, list table, empty/loading/forbidden states
- Playwright tests for happy, empty, `403` and backend-unavailable UI states
- Backend REST Assured regression commands for list/summary/security contracts

**NOT in scope (deferred):**
- Authorize, capture, cancel lifecycle actions
- PSP integration
- `If-Match` / `412` optimistic concurrency
- Kafka, webhooks, event pipeline
- GraphQL, gRPC
- Full business dashboard or fake analytics KPIs

## Completed Tasks

Lesson 09 execution checklist:

| Task | Status |
|---|---|
| L09-001 - Add Zod response schemas/types for payment list and summary | `[AGENT-IMPLEMENT]` done |
| L09-002 - Add Nuxt server proxy for payment list | `[AGENT-IMPLEMENT]` done |
| L09-003 - Add Nuxt server proxy for payment summary | `[AGENT-IMPLEMENT]` done |
| L09-004 - Replace `any` in payment order store with typed state/actions | `[AGENT-IMPLEMENT]` done |
| L09-005 - Add summary cards and list table components | `[AGENT-IMPLEMENT]` done |
| L09-006 - Add merchant-scoped payments panel page | `[AGENT-IMPLEMENT]` done |
| L09-007 - Add Playwright happy/empty/forbidden/backend-unavailable UI tests | `[TESTER-AUTOMATE]` done |
| L09-008 - Run frontend typecheck and targeted E2E tests | `[AGENT-REVIEW]` done |
| L09-009 - Run backend REST Assured regression and Modulith commands | `[AGENT-REVIEW]` done |
| L09-010 - Update vault evidence after implementation | `[AGENT-EXPLAIN]` done |

## Evidence Snapshot

| Evidence | Result |
|---|---|
| Frontend typecheck | `cd apps/frontend && corepack pnpm typecheck` passed. |
| Payment panel Playwright tests | `cd apps/frontend && corepack pnpm test:e2e -- payment-orders-panel.spec.ts` passed, 4 tests. |
| Backend package | `cd apps/backend && ./mvnw -DskipTests package` passed. |
| Modulith architecture | `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test` passed, 2 tests. |
| Summary REST/security/business-flow tests | `cd apps/backend && ./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` passed, 20 tests. |
| Combined list/summary backend guardrail | Command completed without failure report; captured output was truncated. |

## Next Sprint Options

| Option | Description | Requires Spec Kit? |
|---|---|---|
| Sprint 9a | Post-implementation review of the Lesson 09 frontend consumer and learning evidence | No |
| Sprint 9b | Auth ownership/BOLA/BFLA deep dive across backend + UI denied states | Maybe - depends on scope |
| Sprint 9c | DB oracle and EXPLAIN deep dive with repository/query diagnostics | No - practice extension |
| Sprint 10 | Contract documentation/OpenAPI or service virtualization | Maybe - after UI consumer is stable |

## Verification Commands

```bash
cd apps/backend
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test

cd ../frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Navigation

- [[Current Lesson]] — what to study/practice NOW
- [[Current Learning Flow]] — process and flow
- [[Spec Kit Decision Guide]] — when to use Spec Kit
