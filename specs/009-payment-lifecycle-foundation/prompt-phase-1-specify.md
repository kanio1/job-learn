# Prompt Phase 1 - Spec Kit Specify: Lesson 14 - Payment Lifecycle Foundation

```text
Jesteś Kilo działający jako Spec Kit Product/Architecture Lead, Senior Java Backend Architect,
Security Architect, PostgreSQL Data Architect, Nuxt Frontend Architect i QA Architecture Lead.

Repozytorium:

/home/suso/job-learn

## Cel fazy

Uruchom fazę `/speckit.specify` dla funkcji:

Payment Lifecycle Foundation with HTTP Protocol Hardening

Wygeneruj formalny `spec.md` dla:

specs/009-payment-lifecycle-foundation/spec.md

Ta faza ma doprecyzować wymagania systemowe. Nie implementuj kodu. Nie pisz nowych testów.

## Najważniejszy kontekst (przeczytaj przed generacją spec)

### Dokumenty analityczne (BA Discovery + Gap Analysis)

- `specs/009-payment-lifecycle-foundation/BA_DISCOVERY_PACK.md`
- `specs/009-payment-lifecycle-foundation/COMPREHENSIVE_GAP_ANALYSIS.md`
- `specs/009-payment-lifecycle-foundation/EXECUTIVE_SUMMARY.md`

### Istniejące plany i specyfikacje (zachowanie ciągłości)

- `AGENTS.md`
- `specs/008-payment-order-contract-consumer-hardening/spec.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/spec.md`
- `specs/005-payment-order-summary/spec.md`
- `specs/004-payment-order-list-filter/spec.md`
- `specs/003-payment-order-access-lifecycle/spec.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`

### Kod orientacyjny (tylko aby wymagania były realistyczne)

Backend:
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/*.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql`

Frontend (jeśli dotyczy):
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`

## Zakres funkcji

To jest implementacja systemu — Payment Lifecycle Foundation. Spec ma opisać tylko produkcyjne/systemowe zmiany.

### 1. Payment Lifecycle Actions (NOWE)

- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`
  - Przejście: CREATED → AUTHORIZED
  - Ustawia `authorized_at` i `expires_at` (7 dni)
  - Wymaga `Idempotency-Key` i `If-Match` headers
  - Zwraca `200 OK` z zaktualizowanym ETag (wersja+1)
  - Zwraca `Cache-Control: no-store` i `Vary: Authorization, If-Match`

- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`
  - Przejście: AUTHORIZED → CAPTURED
  - Opcjonalny `amountMinor` dla partial capture (≤ authorized amount)
  - Wywołuje PSP mock (always succeeds w Lesson 14)
  - Wymaga `Idempotency-Key` i `If-Match`
  - Zwraca `200 OK` z zaktualizowanym ETag

- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`
  - Przejście: CREATED → CANCELLED lub AUTHORIZED → CANCELLED
  - Dla AUTHORIZED: void's authorization (release funds)
  - Wymaga `Idempotency-Key` i `If-Match`
  - Zwraca `200 OK` z zaktualizowanym ETag

- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund`
  - Przejście: CAPTURED → REFUNDED
  - Opcjonalny `amountMinor` dla partial refund (≤ captured amount)
  - Wywołuje PSP mock dla refund
  - Wymaga `Idempotency-Key` i `If-Match`
  - Zwraca `200 OK` z zaktualizowanym ETag

### 2. State Machine Rules

Spec musi zdefiniować dokładną tabelę przejść:

| Current Status | authorize | capture | cancel | refund |
|---|---|---|---|---|
| CREATED | ✅ | ❌ | ✅ | ❌ |
| AUTHORIZED | ❌ | ✅ | ✅ | ❌ |
| CAPTURED | ❌ | ❌ | ❌ | ✅ |
| CANCELLED | ❌ | ❌ | ❌ | ❌ |
| EXPIRED | ❌ | ❌ | ❌ | ❌ |
| REFUNDED | ❌ | ❌ | ❌ | ❌ |

Każda niedozwolona tranzycja zwraca `422 Unprocessable Entity` z `error=invalid_transition`.

### 3. Optimistic Locking (NOWE)

- Każda lifecycle action wymaga `If-Match` header z bieżącym ETag orderu
- ETag jest budowany jako `"v{version}"` gdzie `version` to pole w `payment_orders`
- Version inkrementuje się przy każdej udanej lifecycle action
- Stale ETag → `412 Precondition Failed` z `error=concurrency_conflict`
- Brak `If-Match` → `400 Bad Request` z `error=missing_required_header`

### 4. Idempotency dla Lifecycle Actions (NOWE)

- Każda lifecycle action wymaga `Idempotency-Key` header
- Ten sam klucz + ta sama akcja = idempotentny replay (zwraca cached result, 200 OK)
- Ten sam klucz + inna akcja = `409 Conflict` z `error=idempotency_conflict`
- Brak `Idempotency-Key` → `400 Bad Request` z `error=validation`

### 5. Audit Trail (NOWE)

- Nowa tabela `payment_order_status_history`
- Loguje każde przejście statusu: `from_status`, `to_status`, `actor`, `timestamp`, `reason`, `idempotency_key`, `correlation_id`
- Insert-only (immutable)
- FK do `payment_orders`

### 6. Authorization Expiration

- Lazy expiration: sprawdzane przy próbie capture (nie automatyczny job)
- Jeśli `expires_at < now()` → authorization wygasło → capture rejected z `422 Unprocessable Entity`, `error=authorization_expired`
- Expired authorization NIE przechodzi automatycznie do EXPIRED — tylko próba capture trigger'uje przejście do EXPIRED

### 7. HTTP Protocol Hardening (NOWE)

CORS (tylko w dev/test profile):
- `Access-Control-Allow-Origin: http://localhost:3000`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Authorization, Content-Type, Idempotency-Key, If-Match, X-Correlation-ID`
- `Access-Control-Expose-Headers: ETag, Location, X-Correlation-ID`
- OPTIONS preflight → 200 OK z powyższymi headers

Cache-Control:
- Wszystkie lifecycle responses: `Cache-Control: no-store` (sensitive financial data)
- Wszystkie lifecycle responses: `Vary: Authorization, If-Match`

PATCH dla metadata:
- `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- Umożliwia aktualizację metadata (key-value pairs) bez zmiany statusu
- Wymaga `If-Match`
- Nie zmienia version (metadata update nie jest lifecycle action)

### 8. Security (NOWE role)

- Dodaj rolę `merchant:payments:lifecycle` — dostęp do authorize/capture/cancel/refund
- Dodaj rolę `platform:payments:lifecycle` — platform override dla lifecycle actions
- Istniejące role `merchant:payments:create`, `merchant:payments:read`, `platform:payments:read` pozostają
- Ownership check: lifecycle actions wymagają matching `merchant_id` (chyba że platform override)
- Platform admin (`platform:payments:lifecycle`) może wykonywać lifecycle actions na dowolnym merchant bez ownership check
- PATCH metadata: wymaga `merchant:payments:lifecycle` i ownership check

### 9. PSP Mock (NOWE - prosty)

- Interfejs `PspClient` z metodami: `authorize()`, `capture()`, `void()`, `refund()`
- Implementacja `MockPspClient` — zawsze zwraca sukces
- Integracja z `PaymentLifecycleService` przez interface (DI)
- PSP mock NIE zwraca failures w Lesson 14 (deferred do Lesson 16)

### 10. Frontend (JEDYNIE jeśli dotyczy)

Spec NIE powinien wymagać frontend changes dla Lesson 14, chyba że:
- Status lifecycle actions powinien być widoczny w payment detail page
- Status history powinien być renderowany jako timeline

Frontend scope dla Lesson 14 jest bardzo ograniczony — głównie backend.

### 11. Database Migration (NOWE)

`V4__add_payment_lifecycle.sql`:
- Dodaj kolumnę `status` enum: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED
- Zmodyfikuj istniejący check constraint `chk_payment_orders_status` aby zawierał nowe statusy
- Dodaj kolumny: `authorized_at TIMESTAMPTZ`, `expires_at TIMESTAMPTZ`, `captured_at TIMESTAMPTZ`, `cancelled_at TIMESTAMPTZ`, `refunded_at TIMESTAMPTZ`, `cancellation_reason VARCHAR(500)`, `refund_reason VARCHAR(500)`
- Dodaj kolumnę `version BIGINT NOT NULL DEFAULT 0` (optimistic locking)
- Dodaj kolumnę `captured_amount_minor BIGINT` (partial capture tracking)
- Nowa tabela `payment_order_status_history`:
  ```sql
  CREATE TABLE payment_order_status_history (
      status_history_id UUID PRIMARY KEY,
      payment_order_id UUID NOT NULL,
      from_status VARCHAR(20),
      to_status VARCHAR(20) NOT NULL,
      actor_subject VARCHAR(200) NOT NULL,
      reason VARCHAR(500),
      idempotency_key_hash VARCHAR(64) NOT NULL,
      correlation_id VARCHAR(128) NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT fk_status_history_order
          FOREIGN KEY (payment_order_id) REFERENCES payment_orders(payment_order_id)
  );
  CREATE INDEX idx_status_history_order_created
      ON payment_order_status_history(payment_order_id, created_at DESC);
  ```

## Twarde non-goals (Lesson 14)

W spec wpisz jawnie:

### Zakazane w Lesson 14
- No new tests as deliverables (tylko production code w tej fazie)
- No partial authorization (always full amount = payment order amount)
- No multi-capture (tylko jedna capture na authorization)
- No multi-refund (tylko jeden refund na capture)
- No PSP failure scenarios (PSP mock zawsze sukces)
- No automatic expiration job (tylko lazy expiration)
- No dispute handling (chargebacks — Lesson 15)
- No webhooks/Kafka/event pipeline (Lesson 17)
- No scheduled jobs (Lesson 16)
- No rate limiting (Lesson 16)
- No API versioning (Lesson 15)
- No HATEOAS links (Lesson 15)
- No complete OAuth/OIDC application integration (permanent guardrail)
- No complete business dashboards (permanent guardrail)
- No fake dashboard analytics/KPIs
- No performance/load testing

### Dozwolone ale ostrożnie
- Nowa migracja DB (V4) — WYMAGANA dla lifecycle
- Nowe role Keycloak (`merchant:payments:lifecycle`, `platform:payments:lifecycle`) — WYMAGANE dla lifecycle
- Nowy moduł PSP client (interface + mock only)
- CORS configuration (tylko dev/test profile)
- Cache-Control headers (dla wszystkich lifecycle responses)

## Wymagany format spec.md

Utwórz `spec.md` z sekcjami:

1. Feature Summary — 1-2 paragrafy
2. Business/System Goal — dlaczego to jest potrzebne
3. Actors — kto korzysta (merchant operator, platform admin, auditor)
4. Current State — co już mamy (Lessons 06-13) a czego brakuje
5. Functional Requirements — szczegółowe FR dla każdego obszaru:
   - FR-LIFECYCLE-xxx: Lifecycle actions (authorize, capture, cancel, refund)
   - FR-STATE-xxx: State machine rules (valid/invalid transitions)
   - FR-LOCKING-xxx: Optimistic locking (If-Match, 412, version)
   - FR-IDEMPOTENCY-xxx: Idempotency (replay, conflict)
   - FR-AUDIT-xxx: Audit trail (status history)
   - FR-HTTP-xxx: HTTP hardening (CORS, caching, PATCH)
   - FR-DB-xxx: Database (migration, constraints, indexes)
   - FR-SEC-xxx: Security (roles, ownership, platform override)
   - FR-PSP-xxx: PSP mock (interface, mock implementation)
   - FR-FE-xxx: Frontend (tylko jeśli dotyczy — limited scope)
6. Backend Requirements — szczegółowe wymagania implementacyjne
7. Database Requirements — V4 migration, constraints, indexes
8. Keycloak/Security Requirements — nowe role, ownership, platform override
9. Frontend Requirements — tylko jeśli dotyczy (limited scope dla Lesson 14)
10. Lesson 14-17 Readiness Requirements — co ta funkcja przygotowuje dla kolejnych lekcji
11. Non-Goals — twarda lista czego NIE implementować
12. Acceptance Criteria — 40-60 observable, testable ACs
13. Edge Cases — 20-30 edge cases z oczekiwanym zachowaniem
14. Assumptions — założenia
15. Open Questions / Clarifications — nierozstrzygnięte decyzje
16. Definition of Done — kiedy funkcja jest ukończona

## Wymagania funkcjonalne do zachowania w spec

### Lifecycle Actions
- FR-LIFECYCLE-001: `POST /{id}/authorize` przechodzi CREATED → AUTHORIZED, ustawia expiration 7 dni
- FR-LIFECYCLE-002: `POST /{id}/capture` przechodzi AUTHORIZED → CAPTURED, obsługuje partial amount
- FR-LIFECYCLE-003: `POST /{id}/cancel` przechodzi CREATED/AUTHORIZED → CANCELLED
- FR-LIFECYCLE-004: `POST /{id}/refund` przechodzi CAPTURED → REFUNDED, obsługuje partial amount
- FR-LIFECYCLE-005: Wszystkie lifecycle actions wymagają `Idempotency-Key` i `If-Match` headers
- FR-LIFECYCLE-006: Wszystkie lifecycle actions zwracają updated ETag (version+1)
- FR-LIFECYCLE-007: Wszystkie lifecycle actions zwracają `Cache-Control: no-store` i `Vary: Authorization, If-Match`
- FR-LIFECYCLE-008: Wszystkie lifecycle actions logują do `payment_order_status_history`

### State Machine
- FR-STATE-001: Tylko dozwolone przejścia są akceptowane (zgodnie z tabelą przejść)
- FR-STATE-002: Niedozwolone przejście → `422 Unprocessable Entity` z `error=invalid_transition`
- FR-STATE-003: Authorization expiration: capture na wygasłej authorization → `422` z `error=authorization_expired`
- FR-STATE-004: Amount validation: captured/refunded amount ≤ available amount → `422` z `error=capture_amount_exceeds_authorized` lub `refund_amount_exceeds_captured`
- FR-STATE-005: CREATED pozostaje jako początkowy status (create nie zmienia się)

### Optimistic Locking
- FR-LOCKING-001: Każda lifecycle action inkrementuje `version` w `payment_orders`
- FR-LOCKING-002: ETag = `"v{version}"` (np. `"v1"`, `"v2"`)
- FR-LOCKING-003: Stale `If-Match` → `412 Precondition Failed` z `error=concurrency_conflict`
- FR-LOCKING-004: Brak `If-Match` → `400 Bad Request` z `error=missing_required_header`
- FR-LOCKING-005: Version NIE zmienia się dla PATCH metadata updates (NIE są lifecycle actions)

### Idempotency
- FR-IDEMPOTENCY-001: Ten sam `Idempotency-Key` + ta sama akcja = idempotentny replay (cached result, 200 OK)
- FR-IDEMPOTENCY-002: Ten sam `Idempotency-Key` + różna akcja = `409 Conflict` z `error=idempotency_conflict`
- FR-IDEMPOTENCY-003: Brak `Idempotency-Key` → `400 Bad Request` z `error=validation` (header required)
- FR-IDEMPOTENCY-004: Create idempotency (Lesson 06) NIE zmienia się

### Audit Trail
- FR-AUDIT-001: Każda status transition tworzy rekord w `payment_order_status_history`
- FR-AUDIT-002: History record zawiera: `payment_order_id`, `from_status`, `to_status`, `actor_subject`, `idempotency_key_hash`, `correlation_id`, `timestamp`
- FR-AUDIT-003: History jest insert-only (immutable)
- FR-AUDIT-004: History endpoint (optional): `GET /{id}/history` zwraca chronologiczną listę przejść

### HTTP Hardening
- FR-HTTP-001: CORS configuration enabled w dev/test profiles (disabled w production)
- FR-HTTP-002: OPTIONS preflight zwraca CORS headers (`Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`)
- FR-HTTP-003: Wszystkie lifecycle responses zawierają `Cache-Control: no-store`
- FR-HTTP-004: Wszystkie lifecycle responses zawierają `Vary: Authorization, If-Match`
- FR-HTTP-005: `PATCH /{id}` umożliwia aktualizację metadata bez zmiany statusu
- FR-HTTP-006: PATCH wymaga `If-Match` (optimistic locking dla metadata)
- FR-HTTP-007: PATCH NIE zmienia version (metadata update nie jest lifecycle action)

### Database
- FR-DB-001: Nowa migracja V4 dodaje kolumny dla lifecycle tracking
- FR-DB-002: Nowa tabela `payment_order_status_history` dla audit trail
- FR-DB-003: Istniejące constraints i indexes z V2/V3 pozostają
- FR-DB-004: `version` kolumna dodana do `payment_orders` dla optimistic locking
- FR-DB-005: `captured_amount_minor` kolumna dla partial capture tracking
- FR-DB-006: Nowe check constraint dla rozszerzonego `status` enum

### Security
- FR-SEC-001: Rola `merchant:payments:lifecycle` dla lifecycle actions
- FR-SEC-002: Rola `platform:payments:lifecycle` dla platform override
- FR-SEC-003: Lifecycle actions sprawdzają matching `merchant_id` (ownership)
- FR-SEC-004: Platform admin (`platform:payments:lifecycle`) może override ownership
- FR-SEC-005: Istniejące role i permissions NIE zmieniają się
- FR-SEC-006: `SecurityConfig` zaktualizowany z nowymi matchers dla lifecycle endpoints
- FR-SEC-007: Backend pozostaje authorization source of truth (frontend reaguje na 403)

### PSP Mock
- FR-PSP-001: Interface `PspClient` definiuje kontrakt dla PSP operacji
- FR-PSP-002: `MockPspClient` implementuje interface (always succeeds)
- FR-PSP-003: PSP mock jest wstrzykiwany przez DI do `PaymentLifecycleService`
- FR-PSP-004: PSP mock NIE zwraca failures w Lesson 14 (deferred do Lesson 16)

## Acceptance criteria style

Acceptance criteria should describe observable production behavior and implementation state, not new automated tests.

Good:
- "POST /authorize na CREATED order zwraca 200 OK, status AUTHORIZED, ETag wersja+1"
- "POST /capture na AUTHORIZED order bez If-Match zwraca 400 Bad Request z error=missing_required_header"
- "Stale If-Match zwraca 412 Precondition Failed z error=concurrency_conflict"

Bad:
- "Create test class PaymentOrderLifecycleTest" (to nie jest acceptance criterion, to implementacja testu)
- "Write 24 parameterized tests" (to nie jest production behavior)
- "Coverage should be 90%" (to nie jest observable behavior)

## Ważne decyzje do uwzględnienia w spec

1. **Single capture only**: jedna capture na authorization (nie multi-capture w Lesson 14)
2. **Single refund only**: jeden refund na capture (nie multi-refund w Lesson 14)
3. **Lazy expiration**: sprawdzane przy capture, nie automatyczny job
4. **Full authorization only**: authorize amount = payment order amount (nie partial authorize)
5. **PSP always succeeds**: mock PSP nigdy nie fail'uje (deferred do Lesson 16)
6. **PATCH metadata only**: PATCH nie zmienia statusu ani amount, tylko metadata
7. **Frontend limited scope**: tylko renderowanie lifecycle statusu w detail page (bez lifecycle actions w UI)

## Sekcja Lesson 14-17 Readiness w spec

Spec powinien zawierać sekcję opisującą co ta funkcja przygotowuje dla:
- Lesson 15: Partial authorization, multi-capture, multi-refund, disputes, API versioning, HATEOAS
- Lesson 16: PSP failure scenarios, rate limiting, scheduled expiration job
- Lesson 17: Webhooks, event pipeline, async notifications

Ale NIE implementuj tych rzeczy w Lesson 14.

Spec powinien zakończyć się stwierdzeniem:
- "No unresolved clarification blocks /speckit.plan" (wszystkie decyzje są rozstrzygnięte)
- "Spec jest gotowy pod /speckit.plan"
```
