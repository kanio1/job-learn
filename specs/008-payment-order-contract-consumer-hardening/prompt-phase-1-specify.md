# Prompt Phase 1 - Spec Kit Specify

Use this prompt for the first Spec Kit phase for feature 008.

```text
Jesteś Kilo działający jako Spec Kit Product/Architecture Lead, Senior Java Backend Architect, Security Architect, PostgreSQL Data Architect, Nuxt Frontend Architect i QA Architecture Lead.

Repozytorium:

/home/suso/job-learn

## Cel fazy

Uruchom fazę `/speckit.specify` dla funkcji:

Payment Order Contract and Consumer Hardening

Wygeneruj formalny `spec.md` dla:

specs/008-payment-order-contract-consumer-hardening/spec.md

Ta faza ma doprecyzować wymagania systemowe. Nie implementuj kodu. Nie pisz nowych testów.

## Najważniejszy kontekst

Przeczytaj najpierw:

- `AGENTS.md`
- `specs/008-payment-order-contract-consumer-hardening/design.md`
- `specs/008-payment-order-contract-consumer-hardening/plan.md`
- `specs/004-payment-order-list-filter/plan.md`
- `specs/006-payment-orders-frontend-consumer/plan.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/plan.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`

Przeczytaj kod orientacyjnie, tylko aby wymagania były realistyczne:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql`
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`

## Zakres funkcji

To jest implementacja systemu, nie materiał lekcyjny i nie zadanie testowe.

Spec ma opisać tylko produkcyjne/systemowe zmiany:

1. Backend list query validation:
   - `GET /api/merchants/{merchantId}/payment-orders`
   - `PaymentOrderListRequest` ma być realną walidowaną granicą requestu.
   - Invalid `page`, `size`, `status`, `currency`, dates, amount range, `sort` mają dawać stabilny validation error.
   - Page beyond last page pozostaje successful empty page.

2. Backend create HTTP protocol handling:
   - `POST /api/merchants/{merchantId}/payment-orders`
   - create ma jawnie konsumować JSON.
   - malformed JSON ma stabilny error response.
   - unsupported media type ma stabilny `415` response.
   - missing `Idempotency-Key` ma stabilny validation response.
   - istniejące `201`, replay `200`, `Location`, `ETag`, `X-Correlation-ID`, idempotency i ownership pozostają.

3. Backend error response consistency:
   - payment-owned errors używają `PaymentErrorResponse`.
   - `error`, `message`, `correlationId` są zachowane.
   - validation details pozostają tam, gdzie Spring field validation je daje.
   - nie wycieka raw token/request/stack trace.

4. Database:
   - brak nowej migracji domyślnie.
   - istniejące constraints i indexes pozostają.
   - cross-field validation dzieje się przed repository/query execution.
   - transaction boundaries pozostają: list read-only, create unchanged.

5. Keycloak/security:
   - brak nowych realm roles.
   - brak zmian realm JSON.
   - brak zmian `KeycloakRealmRoleConverter`, chyba że spec wskaże tylko review/no-change.
   - backend pozostaje source of truth dla authorization.
   - merchant reader wymaga matching `merchant_id`; platform payment reader może czytać wskazany merchant path.
   - frontend nie implementuje complete role model; reaguje na backend `403`.

6. Frontend:
   - payment create/detail pages używają dashboard layout.
   - back links prowadzą do `/admin/merchants/{merchantId}/payments`, nie do nieistniejącego merchant detail route.
   - `usePaymentOrdersStore` przejmuje `loadDetail` i `createOrder`.
   - create form nie mutuje bezpośrednio `store.loading` / `store.error`.
   - detail/create responses są parsowane Zod schema.
   - `403`, `404`, backend unavailable i create failure mają spójne dashboard alert states.

7. Lesson 13 readiness:
   - opisz w spec jako future readiness, nie jako implementację Lesson 13 tests.
   - ta funkcja przygotowuje stabilne controller/error/frontend consumer behavior pod późniejsze Lesson 13 testing/reliability work.

## Twarde non-goals

W spec wpisz jawnie:

- No new tests as deliverables.
- No REST Assured test classes.
- No Playwright specs.
- No test-support clients/builders/specs.
- No `POST /payments`.
- No payment lifecycle actions: authorize/capture/cancel/refund.
- No new statuses: `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED`.
- No PSP integration or PSP mock.
- No Kafka, webhooks, outbox, events or async processing.
- No complete OAuth/OIDC application integration.
- No new Keycloak roles or realm changes.
- No new DB migration unless a real production need is explicitly discovered and approved later.
- No fake dashboard analytics/KPIs.
- No OpenAPI/Pact/WireMock/JSON Schema automation.
- No performance/load testing thresholds.

## Wymagany format spec.md

Utwórz `spec.md` z sekcjami:

1. Feature Summary
2. Business/System Goal
3. Actors
4. Current Problems
5. Functional Requirements
6. Backend Requirements
7. Database Requirements
8. Keycloak/Security Requirements
9. Frontend Requirements
10. Lesson 13 Readiness Requirements
11. Non-Goals
12. Acceptance Criteria
13. Edge Cases
14. Assumptions
15. Open Questions / Clarifications
16. Definition of Done

## Wymagania funkcjonalne do zachowania w spec

Backend list:

- FR-LIST-001: Default list params remain `page=0`, `size=20`, `sort=createdAt,desc`.
- FR-LIST-002: Invalid page/size fail with validation error.
- FR-LIST-003: Unsupported status/currency fail with validation error.
- FR-LIST-004: Invalid date format fails with validation error.
- FR-LIST-005: `fromDate > toDate` fails with validation error.
- FR-LIST-006: Negative min/max amount fails with validation error.
- FR-LIST-007: `minAmount > maxAmount` fails with validation error.
- FR-LIST-008: Unsupported sort fails with validation error.
- FR-LIST-009: Page beyond last page returns successful empty page.
- FR-LIST-010: Existing authorization and merchant ownership policy remains unchanged.

Backend create:

- FR-CREATE-001: Create endpoint explicitly consumes JSON.
- FR-CREATE-002: Malformed JSON returns stable payment error response.
- FR-CREATE-003: Unsupported media type returns stable `415` response.
- FR-CREATE-004: Missing `Idempotency-Key` returns stable validation response.
- FR-CREATE-005: Existing idempotent create/replay behavior remains unchanged.
- FR-CREATE-006: Existing `Location`, `ETag`, and `X-Correlation-ID` behavior remains unchanged.

Frontend:

- FR-FE-001: Payment create page uses dashboard layout.
- FR-FE-002: Payment detail page uses dashboard layout.
- FR-FE-003: Create/detail back links return to payment orders list route.
- FR-FE-004: Payment store owns detail load.
- FR-FE-005: Payment store owns create API call.
- FR-FE-006: Create form owns form state only and does not mutate store API state directly.
- FR-FE-007: Detail/create responses parse through Zod schemas.
- FR-FE-008: `403`, `404`, backend unavailable and create errors render consistent dashboard alerts.

Database/security:

- FR-DB-001: No new table/column/index migration by default.
- FR-DB-002: Existing constraints and indexes remain valid.
- FR-SEC-001: No new Keycloak roles or realm config.
- FR-SEC-002: Backend remains authorization source of truth.
- FR-SEC-003: Existing merchant/platform payment access policy is preserved.

## Acceptance criteria style

Acceptance criteria should describe observable production behavior and implementation state, not new automated tests.

Good:

- “Invalid `size=0` is rejected before repository query execution and returns a payment validation error.”
- “Payment create/detail pages render within the dashboard layout and navigate back to payment orders.”

Bad:

- “Add REST Assured test for size=0.”
- “Add Playwright spec for detail route.”

## Final response required

After generating the spec, summarize:

1. Path of created spec.
2. Key scope decisions.
3. Open questions that need clarification before `/speckit.plan`.
4. Confirmation that no implementation and no new tests were written.
```
