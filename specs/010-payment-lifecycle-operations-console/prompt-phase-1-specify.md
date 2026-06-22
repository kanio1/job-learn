# Prompt Phase 1 - Spec Kit Specify: Payment Lifecycle Operations Console

```text
Jesteś Kilo działający jako Spec Kit Product/Architecture Lead, Senior Java Backend Architect, Nuxt Frontend Architect, Security Architect i QA-aware Product Lead.

Repozytorium:

/home/suso/job-learn

## Cel fazy

Uruchom fazę `/speckit.specify` dla funkcji:

Payment Lifecycle Operations Console

Wygeneruj formalny `spec.md` dla:

specs/010-payment-lifecycle-operations-console/spec.md

Ta faza ma doprecyzować wymagania aplikacyjne. Nie implementuj kodu. Nie pisz nowych testów. Nie twórz zakresu testowego jako deliverable.

## Najważniejszy kontekst

Przeczytaj przed generacją spec:

- `AGENTS.md`
- `specs/010-payment-lifecycle-operations-console/BA_DISCOVERY_PACK.md`
- `specs/009-payment-lifecycle-foundation/spec.md`
- `specs/009-payment-lifecycle-foundation/plan.md`
- `specs/009-payment-lifecycle-foundation/quickstart.md`
- `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`

## Kod orientacyjny

Backend lifecycle/API:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentStatus.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrderStatusHistory.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`

Frontend application surface:

- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- `apps/frontend/app/components/payment/PaymentOrderDetail.vue`
- `apps/frontend/app/server/utils/backendApi.ts`
- `apps/frontend/app/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts`
- `apps/frontend/app/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts`
- `apps/frontend/app/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts`
- `apps/frontend/app/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts`
- `apps/frontend/app/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/history.get.ts`

## Lesson 14 / Feature 009 class labels

When generating or updating planning artifacts, explicitly label the existing lifecycle foundation as **Lesson 14 / Feature 009**.

Reference classes/files to label in explanations and planning context:

Backend:

- `PaymentOrderController` - Lesson 14 lifecycle REST surface.
- `PaymentLifecycleService` - Lesson 14 lifecycle application service.
- `PaymentOrder` - Lesson 14 lifecycle domain state.
- `PaymentStatus` - Lesson 14 lifecycle status vocabulary.
- `PaymentOrderStatusHistory` - Lesson 14 lifecycle history/audit model.
- `PspClient` and `MockPspClient` - Lesson 14 PSP boundary and mock.
- `PaymentExceptionHandler` - Lesson 14 lifecycle HTTP error mapping.
- `SecurityConfig` - Lesson 14 lifecycle role/CORS/security support.

Frontend:

- `payment-order.schema.ts` - Lesson 14 lifecycle schema vocabulary.
- `PaymentOrderDetail.vue` - Lesson 14 detail component expanded by feature 010.
- `backendApi.ts` - Lesson 14/010 Nuxt proxy boundary.
- `authorize.post.ts`, `capture.post.ts`, `cancel.post.ts`, `refund.post.ts`, `history.get.ts` - Lesson 14 lifecycle proxy routes.

## Co nowego technicznie dochodzi

Najważniejsze nowe obszary, które mają być widoczne w planie jako aplikacyjne wymagania i przyszły materiał edukacyjny, bez tworzenia testowego deliverable w tym feature:

1. Specific HTTP headers:
   - `ETag`,
   - `If-Match`,
   - `Idempotency-Key`,
   - `X-Correlation-ID`,
   - `Cache-Control`,
   - `Vary`,
   - `Authorization`,
   - CORS headers: `Access-Control-Allow-Origin`, `Access-Control-Allow-Headers`, `Access-Control-Expose-Headers`.

2. Specific HTTP response codes:
   - `200 OK`,
   - `400 Bad Request`,
   - `401 Unauthorized`,
   - `403 Forbidden`,
   - `404 Not Found`,
   - `409 Conflict`,
   - `412 Precondition Failed`,
   - `415 Unsupported Media Type`,
   - `422 Unprocessable Entity`,
   - optionally `406 Not Acceptable`,
   - `OPTIONS 200 OK` for CORS preflight.

3. Business-technical flows:
   - create payment order -> read `ETag` -> authorize with `If-Match`,
   - authorize -> capture -> read history,
   - stale `ETag` -> `412` -> refresh state,
   - retry same lifecycle action with the same `Idempotency-Key`,
   - forbidden action by wrong role,
   - platform operator vs merchant operator,
   - frontend -> Nuxt proxy -> backend -> database/history.

4. Modern REST/application boundary concepts:
   - API auth failure should remain `401`/`403`, while browser redirect/login UX belongs to the app layer,
   - Nuxt proxy must preserve backend status codes and error body shape,
   - lifecycle actions must carry conditional update and idempotency data,
   - history/audit makes database state observable through the application,
   - Keycloak roles and merchant ownership shape visible affordances but backend authorization remains final.

## Zakres funkcji

To jest część aplikacji, nie część testowa.

Spec ma opisać tylko funkcjonalność systemu/aplikacji potrzebną do używania lifecycle foundation z feature 009.

### 1. Payment Detail Lifecycle Summary

Spec ma wymagać, aby szczegóły payment order pokazywały:

- current lifecycle status,
- status badge for `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`,
- amount and currency,
- authorized timestamp when present,
- expiration timestamp when present,
- captured timestamp and captured amount when present,
- cancelled timestamp and cancellation reason when present,
- refunded timestamp, refunded amount and refund reason when present,
- current metadata.

### 2. Lifecycle History Display

Spec ma wymagać aplikacyjnego wyświetlania historii statusów:

- load `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history`,
- render status history as timeline or chronological list,
- show from/to status,
- show action timestamp,
- show actor when safe and available,
- show reason/amount/PSP reference when returned by backend,
- handle empty history state,
- handle history loading and error states.

Spec must decide whether history order is oldest-first or newest-first.

### 3. State-Aware Lifecycle Action Controls

Spec ma wymagać akcji dostępnych według aktualnego statusu:

| Current status | Available actions |
|---|---|
| `CREATED` | authorize, cancel |
| `AUTHORIZED` | capture, cancel |
| `CAPTURED` | refund |
| `CANCELLED` | none |
| `EXPIRED` | none |
| `REFUNDED` | none |

Requirements:

- controls must not offer impossible state transitions,
- destructive/irreversible actions must use confirmation UX,
- capture/refund/cancel must collect amount or reason only where supported by the backend contract,
- after success, reload payment detail and history,
- after failure, show a clear user-facing message without pretending success.

### 4. Nuxt Server Proxy Header Forwarding

Spec ma wymagać aplikacyjnego proxy behavior dla lifecycle routes:

- forward `Authorization`,
- forward or generate `Idempotency-Key` for lifecycle mutation requests,
- forward `If-Match` using the current order ETag,
- forward `X-Correlation-ID` when present or generate one if the application convention supports it,
- preserve backend status code and error body shape for lifecycle errors.

Important: this is application behavior, not a test framework requirement.

### 5. Metadata Update Flow

Spec ma wymagać osobnej aplikacyjnej ścieżki dla metadata update:

- edit metadata key/value pairs,
- submit PATCH through Nuxt server proxy,
- use current ETag/conditional update rules required by backend,
- make clear that metadata update is not a lifecycle action,
- refresh detail after successful metadata update.

### 6. Role and Permission UX

Spec ma wymagać ostrożnego podejścia do ról:

- if frontend has role information, hide or disable lifecycle mutation controls for read-only/audit actors,
- if frontend does not have enough role information, avoid claiming permission and let backend enforce final authorization,
- forbidden backend responses must be shown as access-denied feedback,
- do not implement complete OAuth/OIDC integration in this feature.

### 7. Error and Stale-State UX

Spec ma wymagać user-facing behavior for:

- invalid transition (`422 invalid_transition`),
- validation error,
- forbidden access,
- not found,
- stale ETag / precondition failure (`412 concurrency_conflict`),
- idempotency conflict,
- backend unavailable/error state.

Stale ETag behavior:

- inform user that the payment changed,
- reload detail/history,
- do not retry automatically in a way that could execute a different business action unexpectedly.

## Twarde non-goals

W spec wpisz jawnie:

- No test suite implementation as feature deliverable.
- No REST Assured framework work.
- No new backend test classes as feature scope.
- No frontend E2E test scope.
- No multi-capture.
- No multi-refund.
- No PSP failure scenarios.
- No PSP provider integration.
- No webhooks.
- No Kafka.
- No scheduled expiration job.
- No rate limiting.
- No HATEOAS redesign.
- No complete business dashboard.
- No fake KPIs or fake operational metrics.
- No complete OAuth/OIDC app integration.
- No new payment lifecycle behavior beyond feature 009 semantics.

## Candidate acceptance criteria

Spec should include acceptance criteria for application behavior:

1. Payment detail displays lifecycle status, lifecycle timestamps and relevant lifecycle amounts/reasons.
2. Payment detail loads and renders lifecycle history.
3. Available actions are derived from current lifecycle status.
4. Terminal statuses show no mutation action.
5. Lifecycle actions submit through Nuxt server proxy with required lifecycle headers.
6. Successful action refreshes payment detail and history.
7. Stale ETag/precondition failure causes user feedback and refresh, not silent retry.
8. Forbidden response is shown as access-denied feedback.
9. Metadata update is separate from lifecycle action and refreshes detail after success.
10. The feature adds no new payment business capability beyond existing lifecycle endpoints.

## Output requirements for `/speckit.specify`

Generate:

- `specs/010-payment-lifecycle-operations-console/spec.md`
- requirements checklist under `specs/010-payment-lifecycle-operations-console/checklists/requirements.md`

The generated spec must:

- be application-focused,
- avoid test deliverables,
- preserve feature 009 lifecycle semantics,
- keep frontend/backend requirements realistic,
- distinguish UI/proxy/application behavior from future payment business features,
- keep all non-goals explicit.
```
