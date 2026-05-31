---
type: learning-os
status: active
date: 2026-05-30
tags:
  - learning-os
  - current-sprint
---

# Current Sprint

> **Active Sprint:** Sprint 8 — Payment Aggregation Summary
>
> **Phase:** 2 — Payment Orders
>
> **Spec:** `specs/005-payment-order-summary/`
>
> **Status:** In progress — backend system slice complete, verification blocked by existing testCompile errors

## Sprint Scope

Read-only aggregation slice after payment order create/read and list/filter/search.

**In scope:**
- Read-only `GET /api/merchants/{merchantId}/payment-orders/summary`
- `GROUP BY currency/status`, `COUNT`, `SUM(amount_minor)`
- `X-Correlation-ID` header on summary response
- Role + `merchant_id` ownership isolation for summary
- Compile/package/modulith verification commands for system implementation evidence

**NOT in scope (deferred):**
- Authorize, capture, cancel lifecycle actions
- PSP integration
- `If-Match` / `412` optimistic concurrency
- Kafka, webhooks, event pipeline
- GraphQL, gRPC
- Full business dashboard or fake analytics KPIs

## Remaining Tasks

Lesson 08 execution checklist:

| Task | Status |
|---|---|
| L08-001 — Implement summary response DTOs | `[AGENT-IMPLEMENT]` done |
| L08-002 — Implement repository/service aggregation queries | `[AGENT-IMPLEMENT]` done |
| L08-003 — Add controller endpoint and security matcher | `[AGENT-IMPLEMENT]` done |
| L08-004 — Add REST Assured summary contract tests | `[TESTER-AUTOMATE]` deferred (out of scope for system slice) |
| L08-005 — Add summary security matrix tests | `[TESTER-AUTOMATE]` deferred (out of scope for system slice) |
| L08-006 — Verify Modulith boundary and payment test suite | `[AGENT-REVIEW]` blocked (`testCompile` fails before `PaymentModuleTest`) |
| L08-007 — Optional Nuxt summary panel and typecheck | `[AGENT-IMPLEMENT]` deferred |
| L08-008 — Update evidence tracker and backlog after tests pass | `[AGENT-EXPLAIN]` done (with blocker noted) |
| L08-009 — Create descriptive lessons (Java/REST/SQL/HTTP/Business Logic) | `[AGENT-EXPLAIN]` done — 5 files across 4 Technical Learning areas + Phase 2 |
| L08-010 — Update README/MOC indexes with new lesson links | `[AGENT-EXPLAIN]` done — Java25, REST Assured, PostgreSQL, REST API From Zero READMEs updated |

## Next Sprint Options

| Option | Description | Requires Spec Kit? |
|---|---|---|
| Sprint 8a | Backend summary endpoint + REST/security tests | No — lesson extension |
| Sprint 8b | DB oracle and EXPLAIN deep dive | No — practice extension |
| Sprint 8c | Minimal Nuxt summary/list panel | No — optional UI extension |
| Sprint 9 | Auth ownership/BOLA/BFLA deep dive | Maybe — depends on scope |

## Verification Commands

```bash
# System compile check
cd apps/backend && ./mvnw clean compile

# Package check without test execution
./mvnw -DskipTests package

# Architecture
./mvnw -Dtest=PaymentModuleTest test
```

## Navigation

- [[Current Lesson]] — what to study/practice NOW
- [[Current Learning Flow]] — process and flow
- [[Spec Kit Decision Guide]] — when to use Spec Kit
