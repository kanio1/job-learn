# Code Review Checklist

Use this checklist for backend/API/security/test changes in this repository.

## Scope

- Does the change stay inside the current merchant/payment order/lifecycle scope?
- Does it avoid top-level `POST /payments`, PSP provider work, PSP failure simulation, Kafka, webhooks, settlement, reconciliation, KYC, and fake dashboard metrics?
- Does it avoid unrelated refactors and dependency changes?

## Spring Modulith

- Are merchant and payment boundaries preserved?
- Does payment depend only on merchant public API, not `merchant.internal`?
- Are new internals placed under the owning module `internal` package?
- Do `ModulithArchitectureTest`, `MerchantModuleTest`, and `PaymentModuleTest` remain meaningful?

## Merchant Behavior

- Merchant reference normalization remains trim + uppercase.
- Reference validation and PostgreSQL unique constraint behavior agree.
- Duplicate reference returns a stable `409` with `duplicate_merchant_reference`.
- Lifecycle stays `DRAFT -> ACTIVE -> SUSPENDED`; invalid transitions return the current conflict shape.
- Create/list/read/activate/suspend authorities remain separated.

## Payment API Behavior

- Merchant ownership and platform role exceptions are explicit.
- Create requires `Idempotency-Key`; lifecycle mutations require the headers currently implemented by the code/spec.
- `ETag`, `If-Match`, `X-Correlation-ID`, `Cache-Control`, `Vary`, `Location`, `Allow`, and `Accept-Patch` are asserted where they are part of the current contract.
- Status codes match behavior: `400` validation/malformed input, `401` unauthenticated, `403` forbidden, `404` masked/not found, `409` conflict, `412` stale version, `415` unsupported media, `422` invalid lifecycle state.
- Error bodies keep stable machine-readable fields and do not leak secrets.

## Persistence

- Flyway migrations and JPA mappings agree.
- Constraints cover important invariants: merchant reference uniqueness, payment status vocabulary, amount/currency ranges, idempotency scope.
- Repository/integration tests verify persisted state, not only HTTP responses, when persistence is the risk.
- Test data uses unique references/keys and does not depend on global shared rows.

## Security

- JWT tests use `TestJwtConfiguration` and local signed tokens.
- Authority names match `SecurityConfig` and `KeycloakRealmRoleConverter`.
- Missing/invalid/expired tokens produce `401`.
- Authenticated users without required authority produce `403`.
- Cross-merchant payment reads stay masked where the code intentionally masks enumeration.
- Browser/frontend code never exposes raw access tokens.

## REST Assured Tests

- Assertions cover status, content type, stable error code, headers, and key response fields.
- Tests use support builders/clients when that improves readability without hiding the oracle.
- Avoid learner copies (`My*`, `Lesson*`) as production regression evidence.
- Keep DB-backed tests on Testcontainers PostgreSQL 18 through `PostgresContainerSupport`.

## Frontend And Playwright

- Nuxt server proxies preserve auth and relevant backend headers/statuses.
- Zod schemas match backend response shape.
- Playwright uses stable role/label locators and deterministic route mocks or real auth setup.
- Tests cover loading, empty, validation, forbidden, backend-unavailable, success, and lifecycle feedback states when touched.
- Avoid flaky timing and shared mutable data.
