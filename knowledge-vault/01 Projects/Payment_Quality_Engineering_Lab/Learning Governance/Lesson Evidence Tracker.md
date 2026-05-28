---
type: tracker
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-27
tags:
  - lesson-evidence
  - learning-delta
  - sdet
  - qa-architecture
---

# Lesson Evidence Tracker

Cel: każda lekcja lub sprint ma mieć dowód, że temat został przerobiony w kodzie, testach, analizie albo świadomie odłożony.

## Template Dla Nowej Lekcji/Sprintu

```text
## Lesson NN - Title

Status:
Prompt:
Business capability:
Learning delta:
Skills expected:
Skills actually used:
Production code evidence:
Test code evidence:
Vault notes:
Spec Kit artifacts:
Commands run:
Competency matrix updates:
Open risks:
Interview answer EN:
Next lesson/sprint handoff:
```

## Lesson 01-05 Summary

| Lesson | Status | Evidence | Notes |
|---:|---|---|---|
| 01 | Introduced | REST API request/response flow prompt and vault note | Foundation; no need to repeat in Lesson 6 |
| 02 | Introduced | REST Assured entry prompt | Foundation; no need to repeat in Lesson 6 |
| 03 | Practiced | REST Assured foundations doc | HTTP method/endpoint/content-type basics |
| 04 | Practiced | Lesson 4 prompt + lesson-pack expansion | Path/query/header basics, `Authorization`, `X-Correlation-ID` context |
| 05 | Practiced | Lesson 5 prompt + lesson-pack expansion | request body, JSON, `Map.of`, DTO, serialization |

## Lesson 06 - Payment Order Create/Read Foundation

Status: `Ready`

Prompt: `../Learning Prompts/Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md` and current interactive prompt for Payment Order create/read lesson.

Business capability: Payment Order create/read foundation with idempotent creation, merchant-scoped access, platform read access, PostgreSQL/Flyway persistence, Keycloak role/claim model, minimal frontend consumer and REST Assured tests.

Learning delta:

- no repetition of `given/when/then`, path params, basic headers and request body basics,
- first payment-specific REST resource,
- `Idempotency-Key` and request fingerprint,
- `Location`, `ETag`, `X-Correlation-ID`, `201`, replay `200`, `403`, masked `404`, `409`,
- role authorization plus `merchant_id` ownership,
- SQL constraints for amount, currency, status and idempotency uniqueness,
- Flyway migration as executable DB contract,
- REST Assured contract tests for headers/body/status/error code,
- security matrix tests for create/read access,
- frontend as API consumer preserving stable idempotency key.

Skills expected:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Skills actually used:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Production code evidence:

- `apps/backend/src/main/java/lab/paymentquality/payment/`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibility.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibilityService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapter.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/frontend/app/components/payment/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`
- `infra/keycloak/realms/payment-quality-realm.json`

Test code evidence:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderIdempotencyConcurrencyTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java`

Vault notes:

- `../02 Phase 2 - Payment Orders/Phase 2 - Payment Orders.md`
- `../02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`

Spec Kit artifacts:

- `specs/003-payment-order-access-lifecycle/spec.md`
- `specs/003-payment-order-access-lifecycle/plan.md`
- `specs/003-payment-order-access-lifecycle/data-model.md`
- `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md`
- `specs/003-payment-order-access-lifecycle/quickstart.md`
- `specs/003-payment-order-access-lifecycle/tasks.md`

Commands run:

- `./mvnw test -q` in `apps/backend` - passed.
- `corepack pnpm typecheck` in `apps/frontend` - passed.

Competency matrix updates: updated after Payment Order create/read scope materialized.

Open risks:

- current lesson note is ready, but REST Assured foundation pack can still be extended with a cross-link instead of duplicating content,
- future lifecycle topics remain deferred: authorize/capture/cancel, `If-Match`, `412`, PSP integration, Kafka, webhooks and settlement,
- frontend E2E is optional verification and was not run during this lesson note capture.

Interview answer EN:

> In Lesson 6 I moved from syntax-driven REST Assured practice to product-risk-driven API testing. I can explain and test an idempotent Payment Order create/read API, including retry behavior, tenant isolation, role and claim checks, database constraints, Flyway migrations, HTTP headers and REST Assured contract assertions.

Next lesson/sprint handoff: deepen REST Assured reusable response/error specifications or continue with payment lifecycle only after a new Spec Kit scope explicitly allows transitions and optimistic concurrency.
