---
type: learning-os
status: active
date: 2026-06-02
tags:
  - learning-os
  - current-sprint
---

# Current Sprint

> **Active Sprint:** Sprint 11 - REST Assured Framework Architecture and Test Organization
>
> **Phase:** 2 — Payment Orders
>
> **Spec:** Lesson extension — framework maturity slice after Lesson 10 completion.
>
> **Status:** Planned - next after Lesson 10 completion

## Sprint Scope

REST Assured framework maturity for existing test suite: API client wrappers, test data builders, reusable error specs, secret masking, test organization.

**In scope:**
- API client wrappers: `PaymentOrderApi.java`, `MerchantApi.java` with business-readable methods.
- Test data builders: `PaymentOrderBuilder.java`, `MerchantBuilder.java` with fluent API.
- Reusable error specs: `PaymentErrorSpecs.java` with `ResponseSpecification` for error contracts.
- Secret masking: `blacklistHeader("Authorization")` in `RestAssuredLoggingConfig.java`.
- Test organization: `@Nested` groups and `@Tag` labels in existing tests.
- Scenario flows: multi-step tests (create → list → summary).

**NOT in scope (deferred):**
- Authorize, capture, cancel lifecycle actions.
- New payment statuses.
- PSP integration or PSP mock flows.
- `If-Match` / `412` optimistic concurrency.
- Kafka, webhooks, event pipeline.
- Pact/WireMock/OpenAPI automation.
- Frontend changes.
- Full business dashboard or fake analytics KPIs.

## Planned Tasks

Lesson 11 execution checklist:

| Task | Status |
|---|---|
| L11-001 - Read Lesson 11 note and prompt | `[TESTER-ANALYZE]` pending |
| L11-002 - Implement API client wrappers (PaymentOrderApi, MerchantApi) | `[AGENT-IMPLEMENT]` pending |
| L11-003 - Implement test data builders (PaymentOrderBuilder, MerchantBuilder) | `[AGENT-IMPLEMENT]` pending |
| L11-004 - Implement reusable error specs (PaymentErrorSpecs) | `[AGENT-IMPLEMENT]` pending |
| L11-005 - Add secret masking to RestAssuredLoggingConfig | `[AGENT-IMPLEMENT]` pending |
| L11-006 - Refactor existing tests with @Nested and @Tag | `[TESTER-AUTOMATE]` pending |
| L11-007 - Optional scenario flows (create → list → summary) | `[TESTER-AUTOMATE]` pending |
| L11-008 - Run regression and Modulith verification | `[AGENT-REVIEW]` pending |
| L11-009 - Update vault evidence after implementation | `[AGENT-EXPLAIN]` pending |

## Analysis Snapshot

| Area | Finding |
|---|---|
| Lessons 06-10 | Create/read, list/filter, summary, frontend consumer and HTTP hardening are implemented and evidenced. |
| Backend REST | Summary/list/create/read endpoints exist with Keycloak-style JWT role and merchant claim enforcement. |
| REST Assured | HTTP edge tests, parameterized auth matrix, contract/business/security tests all pass. Framework maturity is next gap. |
| Frontend | Nuxt Dashboard consumes list/summary via typed Zod/Pinia and Playwright UI state tests. |
| Senior REST gaps | API client wrappers, test data builders, reusable error specs, secret masking, test organization. |

## Next Sprint Options

| Option | Description | Requires Spec Kit? |
|---|---|---|
| Sprint 11 | REST Assured Framework Architecture and Test Organization | No |
| Sprint 12 | Advanced Assertions, Type-Safe Extraction, and Parameterized Testing | No |
| Sprint 13 | Spring Testing Layers, Concurrency, Observability, and Test Reliability | No |
| Sprint 14 | Contract documentation/OpenAPI readiness review | Maybe later |
| Future | Payment lifecycle discovery | Yes, only after guardrails are explicitly updated |

## Verification Commands

```bash
cd apps/backend
./mvnw test
./mvnw -Dtest=PaymentOrderScenarioFlowTest test
./mvnw -Dtest="*Test" -Dgroups="security" test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

## Navigation

- [[Current Lesson]] — what to study/practice NOW
- [[Current Learning Flow]] — process and flow
- [[Spec Kit Decision Guide]] — when to use Spec Kit
