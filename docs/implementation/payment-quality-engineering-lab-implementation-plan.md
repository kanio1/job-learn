# Payment Quality Engineering Lab — Implementation Plan for DB, Backend and Frontend

> **Data:** 2026-06-28  
> **Branch:** `001-project-foundation`  
> **Autorzy (role):** System Architect, Backend Architect, Frontend Architect, Database Architect, Business Analyst PSP, Security/RBAC Reviewer, Playwright Testability Reviewer, Product Strategist  
> **Język:** Polski  
> **Wersja:** 1.0

---

## 1. Executive Summary

Plan implementacji obejmuje 21 zadań MVP, 9 zadań Phase 2 i 5 zadań Phase 3, uzupełnionych o listę odrzuconych funkcji.

**Kluczowe odkrycia z discovery:**

1. **BRAK merchant detail page** — nie istnieje `/admin/merchants/[merchantId].vue`. Istnieje tylko katalog z payments.
2. **PLACEHOLDER_TENANT_ID jest ACTIVE** — tenant seed w `V0.1__create_tenants.sql` ustawia status='ACTIVE'. Zmiana na SUSPENDED wymaga migracji Flyway.
3. **`Idempotency-Replayed` header nie istnieje** — `PaymentCreateResult.created()` vs `.replayed()` jest gotowe w serwisie, ale kontroler nie emituje headeru HTTP.
4. **`If-None-Match` → 304** nie jest obsługiwany — backend produkuje ETag, ale GET nie sprawdza `If-None-Match`.
5. **`WWW-Authenticate` nie jest forwardowany** przez `backendApi.ts` do Nuxt response.
6. **`ProblemDetailsCard`** wyświetla tylko pola RFC 7807 standard — pola `correlationId`, `fieldErrors`, `requiredHeader` są w body ale nie wyświetlane w karcie.
7. **Error Lab brakuje triggerów**: 429, 304, idempotency-replay.
8. **CORS `exposedHeaders`** brakuje: `Retry-After`, `WWW-Authenticate`, `Idempotency-Replayed`, `If-None-Match`.
9. **Playwright 1.60.0** — nie 1.61. Upgrade potrzebny dla `page.localStorage`/`page.sessionStorage`.
10. **`waitForTimeout(500)`** w `payment-order-create.spec.ts:16` — anty-wzorzec do usunięcia.

---

## 2. Source Documents and Repository Discovery

### 2.1 Dokumenty wejściowe

| Dokument | Rola | Priorytet |
|----------|------|-----------|
| Aktualny kod repozytorium | Najwyższy autorytet | 1 |
| `playwright-161-http-api-properties-test-strategy.md` | Główny dokument HTTP/API/Playwright | 2 |
| `payment-quality-engineering-lab-business-technical-cases.md` | Business cases, fazy, decyzje arch. | 3 |
| `playwright-sdet-feature-roadmap(1).md` | Baseline funkcji, luki SDET | 4 |

### 2.2 Stan kodu — podsumowanie discovery

**Backend (Spring Boot 4 / Spring Modulith):**
- 8 modułów: shared, foundation, tenant, merchant, payment, iam, audit, testing
- Kontrolery: `MerchantController`, `PaymentOrderController`, `AuditController`, `UserManagementController`, `StatusController`
- Obsługa błędów: `GlobalExceptionHandler` (ogólny), `PaymentExceptionHandler` (scoped do `PaymentOrderController`)
- Idempotency: `IdempotencyKey`, `IdempotencyRecord`, `PaymentCreateResult.created()`/`.replayed()` — GOTOWE
- ETag: `PaymentEtag.from()` generuje `"v1"`, `"v2"` — GOTOWE; `If-None-Match` — BRAK
- Tenant isolation: `TenantResolver.resolve()` rzuca 403 dla SUSPENDED — GOTOWE
- CORS: exposedHeaders = `[ETag, Cache-Control, Vary, X-Correlation-ID, Location, Allow, Accept-Patch]`
- CORS: allowedHeaders = `[Authorization, Content-Type, Idempotency-Key, If-Match, X-Correlation-ID]`

**Database (PostgreSQL 18 / Flyway):**
- Migracje: V0.1 (tenants), V1, V1.1 (merchants), V2–V5 (payment), V6 (event publication), V7 (audit)
- Tenant: status CHECK `('ACTIVE', 'SUSPENDED')` — constraint już istnieje. PLACEHOLDER_TENANT_ID = ACTIVE.
- Payment orders: kolumny `captured_amount_minor`, `refunded_amount_minor` — ISTNIEJĄ

**Frontend (Nuxt 4):**
- Strony admin: `/admin/merchants/index`, `/admin/merchants/[merchantId]/payments/`, `/admin/users`, `/admin/audit`, `/error-lab`
- BRAK: `/admin/merchants/[merchantId].vue` (merchant detail page)
- `ApiHeaders` type: 7 pól — BRAK `retryAfter`, `wwwAuthenticate`, `idempotencyReplayed`
- `backendApi.ts`: forwards 7 headerów — BRAK `Retry-After`, `WWW-Authenticate`, `Idempotency-Replayed`
- Error Lab: 9 triggerów — BRAK: 429, 304, idempotency-replay
- `ProblemDetailsCard.vue`: tylko standard RFC 7807 — BRAK `correlationId`, `fieldErrors`, `requiredHeader`

**Playwright:**
- Wersja: 1.60.0 (nie 1.61)
- `waitForTimeout(500)` w `payment-order-create.spec.ts:16`
- Brak `APIRequestContext` w testach
- Jeden projekt/rola: `platform-operator`

---

## 3. Which Documents Are Authoritative and Why

### 3.1 Hierarchia autorytetów

**Kod repozytorium > playwright-161-http-api-properties-test-strategy.md > payment-quality-engineering-lab-business-technical-cases.md > playwright-sdet-feature-roadmap(1).md**

**Dlaczego:** Roadmap to baseline, analizy to decyzje architektoniczne, ale kod pokazuje stan aktualny. Plan musi wychodzić od stanu aktualnego.

### 3.2 Konflikty między dokumentami — Decision Notes

| Konflikt | Dokument starszy | Dokument nowszy | Decyzja |
|----------|-----------------|-----------------|---------|
| `IdempotencyCreateInProgressException` → 425 vs 409 | roadmap (425) | kod (`HttpStatus.CONFLICT` = 409) | **409 CONFLICT** — kod wygrywa |
| 3 moduły backendowe vs aktualna lista | business-cases | kod | **8 modułów jak w kodzie** — brak HTTP Contract Lab jako modułu backend |
| `expect.soft.poll()` | roadmap | korekta w roadmap addendum | **nie istnieje** — użyj `expect.soft()` i `expect.poll()` osobno |
| PLACEHOLDER_TENANT → SUSPENDED vs ACTIVE | business-cases | kod (V0.1 migration) | **wymagana migracja** — zmień via Flyway |

---

## 4. Skills and MCP Usage Summary

### 4.1 Użyte role (symulacja agentów)

| Agent | Wkład |
|-------|-------|
| Repo Discovery Agent | Inspekcja kodu: migracje, kontrolery, komponenty, CORS, Error Lab, seed data |
| Business Analyst PSP | Weryfikacja sensu biznesowego: idempotency replay demo, suspended tenant banery, partial capture |
| System Architect | Moduły Spring Modulith, hard dependency rules, brak sztucznych modułów |
| Database Architect | Analiza migracji Flyway, indeksów, statusów, wersji |
| Backend Architect | Kontrolery, serwisy, DTO, wyjątki, CORS, PaymentExceptionHandler |
| REST/HTTP Contract Reviewer | Headery, status codes, Problem Details, idempotency, ETag, CORS expose |
| Security/RBAC Reviewer | Authorities, tenant isolation, masked 404 vs 403, no token leakage |
| Nuxt Frontend Architect | Pages, composables, BFF routes, stores, ApiHeaders, backendApi.ts |
| Nuxt UI Dashboard Reviewer | Dashboard template compliance, Nuxt UI components, data-testid |
| Zod/Validation Reviewer | Schemas, passthrough, problem-details schema extensions |
| Playwright Testability Reviewer | Future testability, data-testid, locators, accessibility |
| Product Strategist | Odrzucenie overengineering, priorytetyzacja MVP |
| MCP Orchestrator | Filesystem tools, grep, git |

### 4.2 MCP / narzędzia użyte w discovery

- **Filesystem MCP / Bash**: `find`, `cat`, `ls`, `grep` — inspekcja kodu
- **Git**: branch state, recent commits
- **Context7**: nie użyty bezpośrednio (wystarczył kod)

---

## 5. Implementation Strategy

### 5.1 Zasady

1. **Discovery-first** — każde zadanie zweryfikowano przed zaproponowaniem
2. **Nuxt UI Dashboard Template** — wszystkie strony używają `UDashboardPanel`, `UCard`, `UButton`, istniejących patterns
3. **Bez sztucznych modułów** — brak `ETagModule`, `IdempotencyModule`, `RateLimitModule`
4. **Bez testów** — plan jest bazą dla przyszłego frameworka testowego
5. **Spring Modulith hard dependency rules** — bez importu `*.internal.*` między modułami
6. **Flyway zamiast seed kodu** dla persistent data (tenant status)
7. **`data-testid` tylko gdy role/label nie wystarczają** — Playwright preferuje dostępne selektory
8. **No token leakage** — `Authorization` nigdy w response, nigdy w DOM

### 5.2 Fazy

| Faza | Zakres | Uzasadnienie |
|------|--------|-------------|
| **MVP** | Headery HTTP, Error Lab, ProblemDetailsCard, merchant detail, tenant SUSPENDED | Wysokie value/cost ratio, brak złożonych zależności |
| **Phase 2** | Playwright upgrade, multi-role, Last-Modified, retryable, support search | Wymaga Phase 1 jako podstawy |
| **Phase 3** | traceparent, CSV export, pagination, Server-Timing | Wymaga dodatkowej infrastruktury |

---

## 6. MVP Scope

21 zadań MVP objętych planem:

1. DB-MVP-001 — Flyway: PLACEHOLDER_TENANT_ID → SUSPENDED
2. BE-MVP-001 — If-None-Match → 304 Not Modified
3. BE-MVP-002 — Idempotency-Replayed response header
4. BE-MVP-003 — requiredHeader w Problem Details 428
5. BE-MVP-004 — CORS: rozszerzenie exposedHeaders i allowedHeaders
6. BFF-MVP-001 — backendApi.ts: forward WWW-Authenticate, Retry-After, Idempotency-Replayed
7. BFF-MVP-002 — backendApi.ts: forward If-None-Match w request
8. BFF-MVP-003 — Error Lab: trigger-429
9. BFF-MVP-004 — Error Lab: trigger-304
10. BFF-MVP-005 — Error Lab: trigger-idempotency-replay
11. BFF-MVP-006 — Error Lab: trigger-401 — forward WWW-Authenticate
12. BFF-MVP-007 — Error Lab: trigger-428 — requiredHeader w body
13. FE-MVP-001 — ApiHeaders + useApiClient: nowe pola
14. FE-MVP-002 — problem-details.schema.ts: znane extensions
15. FE-MVP-003 — ProblemDetailsCard: correlationId + fieldErrors + requiredHeader
16. FE-MVP-004 — error-lab.vue: nowe scenariusze (429, 304, replay)
17. FE-MVP-005 — error-lab.vue: responseHeaders building dla nowych headerów
18. FE-MVP-006 — Merchant detail page: /admin/merchants/[merchantId].vue
19. FE-MVP-007 — Dashboard: tenant context badge
20. FE-MVP-008 — PaymentOrderLifecycleActions: pola kwotowe dla capture/refund
21. SEED-MVP-001 — Seed: suspended merchant dla PLACEHOLDER tenanta

---

## 7. Phase 2 Scope

9 zadań Phase 2:

1. FE-P2-001 — Playwright upgrade 1.60.0 → 1.61.0
2. FE-P2-002 — Multi-role Playwright config (bez testów)
3. BE-P2-001 — Last-Modified header na GET payment order
4. BFF-P2-001 — backendApi.ts: forward Last-Modified
5. BE-P2-002 — Problem Details: retryable + retryAfterSeconds (dla 429)
6. FE-P2-003 — ProblemDetailsCard: retryable badge + retryAfterSeconds
7. FE-P2-004 — RateLimit headers w ApiHeaders (przygotowanie bez backend impl)
8. FE-P2-005 — Support search: basic strona `/admin/support`
9. DB-P2-001 — Indeks na audit_events dla export query

---

## 8. Phase 3 Scope

5 zadań Phase 3:

1. BE-P3-001 — traceparent (W3C Trace Context) via Micrometer Tracing
2. BE-P3-002 — Server-Timing header (dev profile only)
3. BE-P3-003 — Content-Disposition dla CSV export payment orders
4. BE-P3-004 — RateLimit-Limit/Remaining/Reset (wymaga Bucket4j lub Spring Rate Limiter)
5. FE-P3-001 — Link header pagination (wymaga backend paginacji cursor)

---

## 9. Rejected / Document-Only Scope

| Funkcja | Powód odrzucenia |
|---------|-----------------|
| `X-Tenant-ID` / `X-Merchant-ID` w response | Security risk: ujawnia internal mapping |
| `X-User-ID` / `X-Actor-ID` | Security risk: ujawnia tożsamość aktora |
| Stack trace w Problem Details | Security risk |
| `RateLimit-*` w MVP | Martwy kod bez backend rate limitera |
| `traceparent` w MVP | Wymaga Otel infrastructure |
| Multi-role Playwright (5 projektów) w MVP | Data isolation nie gotowa |
| `POST /api/batch` | Out of scope |
| Produkcyjny method override (`X-HTTP-Method-Override`) | Anti-pattern |
| DELETE z body jako wzorzec domyślny | RFC niezalecane |
| Unsafe GET actions | Anti-pattern REST |
| Cache dla danych finansowych/PII | Security risk |
| Fake KPI dashboard | Bez realnego backendu |
| Custom UI poza Nuxt UI Dashboard Template | Naruszenie design system |
| `expect.soft.poll()` | Nie istnieje w Playwright |
| `page.on('dialog')` dla `ConfirmActionModal` | To DOM modal (UModal), nie native dialog |
| Arbitrary `X-*` headers bez edukacyjnej wartości | Overengineering |
| `Content-MD5` | Deprecated |
| `Age` / `Surrogate-Control` | Brak CDN layer |
| `Pragma` | HTTP/1.0 legacy |

---

## 10. Database Implementation Tasks

---

### DB-MVP-001 — Flyway: Zmiana statusu PLACEHOLDER_TENANT_ID na SUSPENDED

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §17 Kategoria K, Tenant context
- `payment-quality-engineering-lab-business-technical-cases.md` — Tenant hypothesis, PLACEHOLDER_TENANT_ID
**Functionality ID:** DB-TENANT-SUSPENDED  
**Implementation Area:** Database / Flyway Migration  
**Module / Route / Component:** `db/migration/tenant/`  
**Business Case:** Platforma powinna demonstrować scenariusz zawieszonego tenanta — `TenantResolver.resolve()` już rzuca 403 dla SUSPENDED, więc zmiana statusu w DB aktywuje tę ścieżkę bez zmiany kodu.  
**Technical Case:** Constraint `CHECK (status IN ('ACTIVE', 'SUSPENDED'))` istnieje w V0.1. Zmiana wymaga tylko UPDATE w nowej migracji.  
**Why now:** Blokuje SEED-MVP-001 i FE-MVP-007 (tenant badge). Bez tego nie ma demonstrowalnego suspended tenant.  
**Dependencies:** V0.1 migracja musi być już zastosowana  
**Files to inspect before coding:**
- `apps/backend/src/main/resources/db/migration/tenant/V0.1__create_tenants.sql`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantResolver.java`
**Files likely to change:**
- `apps/backend/src/main/resources/db/migration/tenant/V0.2__suspend_placeholder_tenant.sql` (NOWY)

#### Database work
- Stwórz `V0.2__suspend_placeholder_tenant.sql`
- SQL: `UPDATE tenants SET status = 'SUSPENDED', name = 'Suspended Demo Tenant' WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID';`
- Zmień `name` na czytelny: `Suspended Demo Tenant`

#### Backend work
- Żadnych zmian — `TenantResolver` już obsługuje SUSPENDED → 403

#### RBAC / Security / Tenant isolation
- `TenantResolver.resolve()` dla JWT z `tenant_id` mapping do PLACEHOLDER_TENANT_ID rzuci 403
- Użytkownicy z tym tenantem nie mogą już tworzyć zasobów (zgodnie z istniejącą logiką)
- PLATFORM_ADMIN (bez tenant claim) nie jest blokowany przez TenantResolver

#### Acceptance criteria without tests
- `SELECT status FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'` → `SUSPENDED`
- `SELECT name FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'` → `Suspended Demo Tenant`
- `./mvnw flyway:info` pokazuje V0.2 jako applied

#### Future Playwright Coverage Note
- Future test type: Playwright API (via APIRequestContext) + Playwright UI
- Future Playwright capabilities: `APIRequestContext.get()`, `expect(response).toBe(403)`
- Future POM: `TenantContextBadge` locator
- Future locators: `getByTestId('tenant-status-badge')`
- Future API assertions: GET /api/merchants z tokenem z SUSPENDED tenant → 403 z `error = "tenant_suspended"`
- Future UI assertions: Banner "Suspended Demo Tenant" widoczny dla użytkownika z tym tenantem

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### DB-P2-001 — Indeks na audit_events dla query export

**Phase:** Phase 2  
**Source Document:**
- `payment-quality-engineering-lab-business-technical-cases.md` — Support/Risk/Compliance, §19
**Functionality ID:** DB-AUDIT-EXPORT-INDEX  
**Implementation Area:** Database / Flyway  
**Dependencies:** V7__create_audit_event.sql

#### Database work
- Stwórz `V8__add_audit_event_export_index.sql`
- `CREATE INDEX idx_audit_events_merchant_occurred ON audit_events (merchant_id, occurred_at DESC);`
- Komentarz: dla zapytań eksportowych (CSV, paginacja, filtrowanie)

#### Future Playwright Coverage Note
- Future test type: Playwright API
- Future API assertions: GET /audit?merchantId=X&from=Y — asercja czasu odpowiedzi < 500ms via `Server-Timing`

#### Do not implement
- tests, fixtures, page objects

---

## 11. Backend Implementation Tasks

---

### BE-MVP-001 — If-None-Match → 304 Not Modified dla GET Payment Order

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §9 Kategoria C, M3, Scenariusz 4
- `payment-quality-engineering-lab-business-technical-cases.md` — HTTP Contract
**Functionality ID:** HTTP-MVP-CONDITIONAL-GET-304  
**Implementation Area:** Backend  
**Module / Route / Component:** `payment/internal/web/PaymentOrderController.java`  
**Business Case:** `If-None-Match` / 304 Not Modified to podstawowy wzorzec warunkowy HTTP — kluczowy dla edukacji SDET. ETag jest już generowany, brakuje tylko drugiej strony.  
**Technical Case:** `getPaymentOrder()` i `headPaymentOrder()` ignorują nagłówek `If-None-Match`. Zmiana wymaga dodania `@RequestHeader` i sprawdzenia ETag.  
**Why now:** ETag jest gotowy. Zmiana to ~10 linii kodu. Odblokuje Error Lab trigger-304 i Playwright Scenariusz 4.  
**Dependencies:** — (ETag już istnieje)  
**Files to inspect before coding:**
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentEtag.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentHttpHeaders.java`
**Files likely to change:**
- `PaymentOrderController.java` — `getPaymentOrder()` i `headPaymentOrder()`

#### Backend work
- W `getPaymentOrder()` dodaj parametr: `@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch`
- Pobierz `PaymentOrder order` jak dotychczas
- Oblicz `String etag = PaymentEtag.from(order)`
- Jeśli `etag.equals(ifNoneMatch)` → zwróć:
  ```java
  return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
      .header("ETag", etag)
      .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
      .build();
  ```
- Reszta przepływu bez zmian
- Analogicznie w `headPaymentOrder()` — ta sama logika (HEAD już zwraca ETag)
- **Uwaga:** 304 NIE ma body — `build()` zamiast `.body(response)`
- **Uwaga:** `Cache-Control: no-store` NA 304 jest dyskusyjne (no-store technicznie koliduje z conditional GET). Zachowaj `no-store` dla spójności modelu edukacyjnego.

#### Headers / HTTP contract
- `304 Not Modified` — ETag, X-Correlation-ID, Cache-Control: no-store, Vary: Authorization
- Brak body (Content-Length: 0)

#### Error handling / Problem Details
- Brak — 304 nie jest błędem

#### RBAC / Security / Tenant isolation
- Identyczne reguły dostępu jak dla 200 — sprawdzenie `merchant_id` claim przed porównaniem ETag

#### Acceptance criteria without tests
- `GET /api/merchants/{id}/payment-orders/{orderId}` → ETag: `"v1"` (lub wyższy)
- `GET /api/merchants/{id}/payment-orders/{orderId}` z `If-None-Match: "v1"` → `304`, brak body
- `GET` z `If-None-Match: "v0"` (stary ETag) → `200` z aktualnym body
- `HEAD` z `If-None-Match: "v1"` → `304`, brak body

#### Future Playwright Coverage Note
- Future test type: Playwright API (APIRequestContext) + hybrid
- Future Playwright capabilities: `APIRequestContext.get()`, `response.status()`, `response.text()`
- Future POM: `NetworkAssertions.expectNotModified()`
- Future locators/testids: Error Lab `data-testid="error-lab-trigger-304"`
- Future API assertions: GET → capture ETag → GET z If-None-Match → status 304, body empty
- Future UI assertions: Error Lab karta 304 pokazuje status badge 304

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BE-MVP-002 — Idempotency-Replayed response header

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §8 Kategoria B, M2, Scenariusz 5
- `payment-quality-engineering-lab-business-technical-cases.md` — Idempotency replay demo
**Functionality ID:** HTTP-MVP-IDEMPOTENCY-REPLAY  
**Implementation Area:** Backend  
**Module / Route / Component:** `payment/internal/web/PaymentOrderController.java`  
**Business Case:** Replay idempotentny zwraca 200 zamiast 201 — ale klient nie wie, czy to replay. `Idempotency-Replayed: true/false` to jedyna sieciowa sygnalizacja replay bez parsowania body.  
**Technical Case:** `PaymentCreateResult.created()` zwraca `created=true`, `.replayed()` zwraca `created=false`. Kontroler już rozgałęzia na 201 vs 200, ale nie ustawia headeru.  
**Why now:** Bardzo niski koszt (1 linia kodu). Wysokie wartość edukacyjna. Odblokuje Error Lab trigger-idempotency-replay.  
**Dependencies:** BFF-MVP-001 (forward headeru przez proxy)  
**Files to inspect before coding:**
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentCreateResult.java`
**Files likely to change:**
- `PaymentOrderController.java` — metoda `createPaymentOrder()`

#### Backend work
- W gałęzi `if (result.created())`:
  ```java
  return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location), ...)
      .header("ETag", etag)
      .header("Idempotency-Replayed", "false")
      .body(response);
  ```
- W gałęzi `else` (replay):
  ```java
  return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(), ...)
      .header("ETag", etag)
      .header("Idempotency-Replayed", "true")
      .body(response);
  ```

#### Headers / HTTP contract
- `Idempotency-Replayed: true` przy replay (200)
- `Idempotency-Replayed: false` przy nowym zasobie (201)
- Header musi być w CORS `exposedHeaders` (BE-MVP-004)

#### RBAC / Security / Tenant isolation
- Brak nowych ograniczeń — header jest value-neutral (true/false)

#### Acceptance criteria without tests
- POST z kluczem X → 201, `Idempotency-Replayed: false`, body = nowy payment order
- POST z tym samym kluczem X i tym samym body → 200, `Idempotency-Replayed: true`, body identyczny
- POST z tym samym kluczem X i innym body → 409, brak `Idempotency-Replayed`

#### Future Playwright Coverage Note
- Future test type: Playwright API + hybrid UI+API
- Future Playwright capabilities: `page.waitForResponse()`, header assertions, status assertions
- Future POM: `NetworkAssertions.expectIdempotencyReplayed()`
- Future locators: Error Lab `data-testid="error-lab-trigger-idempotency-replay"`
- Future API assertions: `response.headers()['idempotency-replayed']` === 'true'
- Future UI assertions: Error Lab karta pokazuje `Idempotency-Replayed: true` w HeaderKeyValuePanel

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BE-MVP-003 — requiredHeader w Problem Details 428

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M10, Scenariusz 8
- `playwright-sdet-feature-roadmap(1).md` — Error Lab 428
**Functionality ID:** HTTP-MVP-REQUIRED-HEADER-428  
**Implementation Area:** Backend  
**Module / Route / Component:** `payment/internal/web/PaymentExceptionHandler.java`  
**Business Case:** 428 Precondition Required mówi, że brakuje headeru — ale który? `requiredHeader` w body eliminuje zgadywanie po stronie klienta i SDET-a.  
**Technical Case:** `handlePreconditionRequired()` w `PaymentExceptionHandler` wywołuje `problem()` bez pola `requiredHeader`. `PaymentErrorResponse` ma `[key: string]: unknown` — brak typowanego pola.  
**Why now:** Jeden handler, jeden dodatkowy parametr. Niska złożoność, wysoka wartość edukacyjna.  
**Dependencies:** FE-MVP-003 (wyświetlenie w ProblemDetailsCard)  
**Files to inspect before coding:**
- `PaymentExceptionHandler.java` — `handlePreconditionRequired()`, `handleMissingRequestHeader()`
- `PaymentErrorResponse.java` — struktura rekordu
**Files likely to change:**
- `PaymentErrorResponse.java` — dodanie opcjonalnego pola `requiredHeader`
- `PaymentExceptionHandler.java` — handlery 428

#### Backend work
- W `PaymentErrorResponse` dodaj opcjonalne pole `requiredHeader`:
  ```java
  public record PaymentErrorResponse(
      String type, String title, int status, String detail,
      String code, String correlationId, String error, String message,
      List<FieldError> details, String requiredHeader  // NOWE
  ) { ... }
  ```
- Dodaj fabrykę `of()` z `requiredHeader` parametrem lub ustaw via builder
- W `handlePreconditionRequired()`:
  ```java
  // PaymentPreconditionRequiredException powinien znać nazwę headeru
  return problem(HttpStatus.PRECONDITION_REQUIRED, ERROR_PRECONDITION_REQUIRED, 
      ex.getMessage(), "If-Match", preconditionHeaders());
  ```
- Sprawdź `PaymentPreconditionRequiredException` — czy zawiera `headerName`; jeśli nie, dodaj pole lub hardcode `"If-Match"`
- `handleMissingRequestHeader()` gdy header to `If-Match`:
  - Dodaj `requiredHeader: "If-Match"` do body
  - Ustaw status 428 zamiast 400 dla headerów warunkowych (`If-Match`, `Idempotency-Key` dla lifecycle)

#### Error handling / Problem Details
Wynikowy body 428:
```json
{
  "type": "https://api.payment-quality.local/problems/precondition-required",
  "status": 428,
  "error": "precondition_required",
  "correlationId": "...",
  "requiredHeader": "If-Match"
}
```

#### Acceptance criteria without tests
- POST `/authorize` bez `If-Match` → 428, body zawiera `"requiredHeader": "If-Match"`
- POST `/capture` bez `If-Match` → 428, body zawiera `"requiredHeader": "If-Match"`
- Error Lab trigger-428 zwraca body z `requiredHeader`

#### Future Playwright Coverage Note
- Future test type: Playwright API + UI
- Future POM: `ProblemDetailsCardObject.expectRequiredHeader("If-Match")`
- Future locators: `getByTestId('required-header-value')`
- Future UI assertions: ProblemDetailsCard pokazuje "Required Header: If-Match"

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BE-MVP-004 — CORS: rozszerzenie exposedHeaders i allowedHeaders

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §14 Kategoria H, M9
- `playwright-sdet-feature-roadmap(1).md` — CORS setup
**Functionality ID:** HTTP-MVP-CORS-HEADERS  
**Implementation Area:** Backend / SecurityConfig  
**Module / Route / Component:** `shared/security/SecurityConfig.java`  
**Business Case:** Playwright nie może odczytać custom response headerów (`Retry-After`, `Idempotency-Replayed`) jeśli nie są w `Access-Control-Expose-Headers`. To blokuje network-level assertions w testach.  
**Technical Case:** `corsConfigurationSource()` jest w `@Profile("dev", "test")` — poprawne. Wystarczy rozszerzyć listy.  
**Why now:** Bez tego MVP Playwright assertions na headerach nie będą działać. Blokuje wszystkie scenariusze network-level.  
**Dependencies:** — (zmiana standalone)  
**Files to inspect before coding:**
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
**Files likely to change:**
- `SecurityConfig.java` — `corsConfigurationSource()`

#### Backend work
Zmień `corsConfigurationSource()`:

```java
// allowedHeaders — dodaj If-None-Match
config.setAllowedHeaders(List.of(
    "Authorization", "Content-Type",
    "Idempotency-Key", "If-Match",
    "If-None-Match",      // ← NOWE (wymagane dla conditional GET)
    "X-Correlation-ID"
));

// exposedHeaders — dodaj Retry-After, WWW-Authenticate, Idempotency-Replayed
config.setExposedHeaders(List.of(
    "ETag", "Cache-Control", "Vary",
    "X-Correlation-ID", "Location",
    "Allow", "Accept-Patch",
    "Retry-After",           // ← NOWE
    "WWW-Authenticate",      // ← NOWE
    "Idempotency-Replayed"   // ← NOWE
));
```

#### Security / RBAC
- `Access-Control-Expose-Headers` jest safe — nie ujawnia credentials, tylko nazwy headerów
- NIE dodawaj `Authorization` do `exposedHeaders` — nigdy

#### Acceptance criteria without tests
- OPTIONS preflight na `/api/merchants/{id}/payment-orders` z `Access-Control-Request-Headers: If-None-Match` → `Access-Control-Allow-Headers` zawiera `If-None-Match`
- Zwykły GET → response zawiera `Access-Control-Expose-Headers` z `Retry-After`, `Idempotency-Replayed`

#### Future Playwright Coverage Note
- Future test type: Playwright API (CORS preflight test)
- Future Playwright capabilities: `APIRequestContext.fetch()` z OPTIONS method
- Future API assertions: `response.headers()['access-control-expose-headers']` zawiera `ETag`, `X-Correlation-ID`, `Retry-After`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BE-P2-001 — Last-Modified header na GET payment order

**Phase:** Phase 2  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §13 Kategoria G
**Functionality ID:** HTTP-P2-LAST-MODIFIED  
**Implementation Area:** Backend  
**Dependencies:** BE-MVP-001 (conditional GET pattern już znany)

#### Backend work
- W `getPaymentOrder()` i `headPaymentOrder()` dodaj:
  ```java
  DateTimeFormatter.RFC_1123_DATE_TIME.format(order.getUpdatedAt().atOffset(ZoneOffset.UTC))
  ```
- Ustaw `Last-Modified` header na response
- Dodaj `If-Modified-Since` do sprawdzania (analogia do If-None-Match)
- Dodaj `Last-Modified` do CORS `exposedHeaders`

#### Future Playwright Coverage Note
- Future test type: Playwright API
- Future API assertions: `response.headers()['last-modified']` jest RFC 1123 date

#### Do not implement
- tests, fixtures, page objects

---

### BE-P2-002 — Problem Details: retryable + retryAfterSeconds (dla 429)

**Phase:** Phase 2  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — Phase 2 list
**Functionality ID:** HTTP-P2-RETRYABLE  
**Implementation Area:** Backend / GlobalExceptionHandler  
**Dependencies:** BE-P3-004 (backend rate limiter) — Phase 2 możliwy bez rate limitera jeśli Error Lab robi mock

#### Backend work
- Dodaj `retryable: boolean` i `retryAfterSeconds: int` do `GlobalExceptionHandler.problemBody()` przy 429

#### Do not implement
- tests, fixtures, page objects

---

## 12. Nuxt BFF / Server Proxy Implementation Tasks

---

### BFF-MVP-001 — backendApi.ts: forward WWW-Authenticate, Retry-After, Idempotency-Replayed

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §22, M6, M1, M2
**Functionality ID:** BFF-MVP-HEADER-FORWARD  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `apps/frontend/server/utils/backendApi.ts`  
**Business Case:** Bez tego forwarding headery istnieją w backend response ale nie docierają do przeglądarki/UI.  
**Technical Case:** `forwardBackendHeaders()` ma hardcoded listę 7 headerów. Rozszerzenie listy to 3 linijki.  
**Why now:** Blokuje Error Lab trigger-429 (Retry-After) i trigger-401 (WWW-Authenticate) wyświetlanie.  
**Dependencies:** BE-MVP-004 (CORS expose headers)  
**Files to inspect before coding:**
- `apps/frontend/server/utils/backendApi.ts`
**Files likely to change:**
- `server/utils/backendApi.ts` — `forwardBackendHeaders()`

#### Nuxt BFF work
```typescript
function forwardBackendHeaders(event: H3Event, headers: Headers) {
  for (const name of [
    'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID',
    'Location', 'Accept-Patch', 'Allow',
    // MVP additions:
    'Retry-After',
    'WWW-Authenticate',
    'Idempotency-Replayed',
  ]) {
    const value = headers.get(name) || headers.get(name.toLowerCase())
    if (value) {
      setHeader(event, name, value)
    }
  }
}
```

#### Acceptance criteria without tests
- GET payment order → `Idempotency-Replayed` header widoczny w Nuxt response (gdy backend go wyśle)
- Error Lab 429 → `Retry-After: 30` w Nuxt response headers
- Error Lab 401 → `WWW-Authenticate: Bearer ...` w Nuxt response headers

#### Future Playwright Coverage Note
- Future test type: Playwright API (przez `/server/api/**`)
- Future API assertions: `response.headers()['retry-after']`, `response.headers()['www-authenticate']`, `response.headers()['idempotency-replayed']`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-002 — backendApi.ts: forward If-None-Match w request

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §9, M3
**Functionality ID:** BFF-MVP-IF-NONE-MATCH-FORWARD  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/utils/backendApi.ts`, `server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`  
**Technical Case:** `backendApi.ts` forwarduje `If-Match` przez `opts.forwardIfMatch`. Analogicznie potrzeba forwarding `If-None-Match`.  
**Dependencies:** BE-MVP-001 (backend 304 support)  
**Files to inspect before coding:**
- `server/utils/backendApi.ts`
- `server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`
**Files likely to change:**
- `server/utils/backendApi.ts` — dodanie `forwardIfNoneMatch?: string` do opts
- `server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`

#### Nuxt BFF work
- Dodaj `forwardIfNoneMatch?: string` do `opts` w `backendApi()`
- W `headers`:
  ```typescript
  if (opts.forwardIfNoneMatch) {
    headers['If-None-Match'] = opts.forwardIfNoneMatch
  }
  ```
- W route handlera GET:
  ```typescript
  const ifNoneMatch = getHeader(event, 'If-None-Match')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, {
    forwardIfNoneMatch: ifNoneMatch || undefined,
  })
  ```

#### Acceptance criteria without tests
- GET `/server/api/merchants/{id}/payment-orders/{orderId}` z `If-None-Match: "v1"` → backend odpowiada 304 → Nuxt forwarduje 304 do klienta

#### Future Playwright Coverage Note
- Future test type: Playwright API
- Future API assertions: `APIRequestContext.get(url, { headers: { 'If-None-Match': etag } })` → status 304

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-003 — Error Lab: trigger-429.post.ts (nowy)

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M8, §10, Scenariusz 7
**Functionality ID:** ERRORLAB-MVP-429  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/api/error-lab/trigger-429.post.ts` (NOWY)  
**Business Case:** 429 Too Many Requests + Retry-After to kluczowy wzorzec rate limiting. Error Lab musi to demonstrować.  
**Technical Case:** Brak backend rate limitera w MVP. Trigger jest **mockiem Nuxt-side** — symuluje 429 response bez wywołania backendu. Edukacyjna wartość jest taka sama — klient widzi 429 z Retry-After.  
**Why now:** Error Lab jest niekompletny bez 429.  
**Dependencies:** BFF-MVP-001 (Retry-After forwarding)  
**Files to inspect before coding:**
- `server/api/error-lab/trigger-400.post.ts` (wzorzec)
**Files likely to change:**
- `server/api/error-lab/trigger-429.post.ts` (NOWY)

#### Nuxt BFF work
```typescript
// trigger-429.post.ts
export default defineEventHandler((event) => {
  setResponseStatus(event, 429)
  setHeader(event, 'Retry-After', '30')
  setHeader(event, 'Cache-Control', 'no-store')
  setHeader(event, 'X-Correlation-ID', crypto.randomUUID())
  setHeader(event, 'Content-Type', 'application/problem+json')

  return {
    type: 'https://api.payment-quality.local/problems/rate-limit-exceeded',
    title: 'Too Many Requests',
    status: 429,
    detail: 'Rate limit exceeded. Retry after the indicated delay.',
    correlationId: crypto.randomUUID(),
    error: 'rate_limit_exceeded',
  }
})
```

**Ważne:** Dwa oddzielne `crypto.randomUUID()` w headerze i body — w produkcji byłyby takie same. Dla lab celów edukacyjnych możesz użyć jednej zmiennej.

#### Acceptance criteria without tests
- POST `/api/error-lab/trigger-429` → status 429
- `Retry-After: 30` w response headers
- `Content-Type: application/problem+json`
- Body: `{ type, title, status: 429, detail, correlationId, error }`

#### Future Playwright Coverage Note
- Future test type: Playwright API + UI
- Future Playwright capabilities: `APIRequestContext.post()`, header assertions
- Future POM: `HeaderPanelObject.expectRetryAfter()`
- Future locators: `data-testid="error-lab-trigger-429"`
- Future UI assertions: Error Lab karta 429 pokazuje `Retry-After: 30` w HeaderKeyValuePanel, ProblemDetailsCard ze status 429

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-004 — Error Lab: trigger-304.get.ts (nowy)

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §9, M3, Scenariusz 4
**Functionality ID:** ERRORLAB-MVP-304  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/api/error-lab/trigger-304.get.ts` (NOWY)  
**Business Case:** 304 Not Modified to trudny do zademonstrowania wzorzec — wymaga dwóch kroków (GET → If-None-Match). Error Lab trigger robi to automatycznie.  
**Technical Case:** Trigger realizuje dwuetapowy przepływ: (1) GET payment order → ETag, (2) GET z If-None-Match: ETag → 304. Wymaga działającego BE-MVP-001 i BFF-MVP-002.  
**Dependencies:** BE-MVP-001, BFF-MVP-002  
**Files to inspect before coding:**
- `server/api/error-lab/trigger-428.post.ts` (wzorzec dwuetapowy)
- `server/api/error-lab/trigger-412.post.ts` (wzorzec dwuetapowy)
**Files likely to change:**
- `server/api/error-lab/trigger-304.get.ts` (NOWY)

#### Nuxt BFF work
Strategia dwuetapowa:
1. GET `/api/merchants` → znajdź aktywny merchant → znajdź payment order
2. GET `/api/merchants/{id}/payment-orders/{orderId}` → zapamiętaj ETag
3. GET z `If-None-Match: {etag}` → backend zwraca 304

```typescript
export default defineEventHandler(async (event) => {
  // Step 1: Find merchant and payment order
  // Step 2: GET order → capture ETag
  // Step 3: GET z If-None-Match → forward 304
  
  // Forward headers: ETag, X-Correlation-ID, Cache-Control
  setResponseStatus(event, result.status)  // 304
  return result.data  // null/empty for 304
})
```

#### Acceptance criteria without tests
- GET `/api/error-lab/trigger-304` → status 304 (jeśli payment order istnieje)
- ETag header w response
- Puste body

#### Future Playwright Coverage Note
- Future test type: Playwright UI + API
- Future locators: `data-testid="error-lab-trigger-304"`
- Future UI assertions: Error Lab karta 304 pokazuje status badge 304 i ETag

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-005 — Error Lab: trigger-idempotency-replay.post.ts (nowy)

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §8, Scenariusz 5
**Functionality ID:** ERRORLAB-MVP-IDEMPOTENCY-REPLAY  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/api/error-lab/trigger-idempotency-replay.post.ts` (NOWY)  
**Business Case:** Idempotency replay (201 → 200 z `Idempotency-Replayed: true`) to edukacyjnie unikalne. Klient widzi różnicę między nowym a replayed zasobem.  
**Technical Case:** Dwuetapowy trigger: (1) POST create z kluczem X → 201 + `Idempotency-Replayed: false`, (2) POST z tym samym kluczem X i body → 200 + `Idempotency-Replayed: true`.  
**Dependencies:** BE-MVP-002 (backend header), BFF-MVP-001 (forward header)  
**Files to inspect before coding:**
- `server/api/error-lab/trigger-409.post.ts` (wzorzec stored key)
**Files likely to change:**
- `server/api/error-lab/trigger-idempotency-replay.post.ts` (NOWY)

#### Nuxt BFF work
- Przechowuj `storedKey` na poziomie modułu (jak trigger-409)
- Pierwsze wywołanie: POST z nowym kluczem → 201 → response pokazuje 201 + `Idempotency-Replayed: false`
- Drugie wywołanie (ten sam klucz, to samo body): → 200 + `Idempotency-Replayed: true`
- Po pokazaniu replay — zresetuj klucz (następne wywołanie znów zaczyna cykl od 201)
- Forward `Idempotency-Replayed`, `ETag`, `X-Correlation-ID`, `Cache-Control`

#### Acceptance criteria without tests
- Pierwsze POST `/api/error-lab/trigger-idempotency-replay` → 201, `Idempotency-Replayed: false`
- Drugie POST → 200, `Idempotency-Replayed: true`
- Trzecie POST → 201 (nowy cykl)

#### Future Playwright Coverage Note
- Future test type: Playwright API + UI
- Future locators: `data-testid="error-lab-trigger-idempotency-replay"`
- Future UI assertions: HeaderKeyValuePanel shows `Idempotency-Replayed: true`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-006 — Error Lab: trigger-401.get.ts — forward WWW-Authenticate

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M6, Scenariusz 9
**Functionality ID:** ERRORLAB-MVP-401-WWW-AUTH  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/api/error-lab/trigger-401.get.ts`  
**Business Case:** 401 bez `WWW-Authenticate` jest niekompletny — standard wymaga tego headeru w 401. SDET powinien testować ten header.  
**Technical Case:** Trigger-401 nie forwarduje `WWW-Authenticate` (nie ma go w liście). Spring Security wysyła `WWW-Authenticate: Bearer realm="..."` w 401.  
**Dependencies:** BFF-MVP-001 (WWW-Authenticate w centralnej liście)  
**Files to inspect before coding:**
- `server/api/error-lab/trigger-401.get.ts`
**Files likely to change:**
- `server/api/error-lab/trigger-401.get.ts`

#### Nuxt BFF work
W bloku forward headerów (w obu try i catch):
```typescript
for (const name of [
  'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type',
  'WWW-Authenticate',  // ← NOWE
]) {
  const val = response.headers.get(name)
  if (val) setHeader(event, name, val)
}
```

#### Future Playwright Coverage Note
- Future locators: `data-testid="error-lab-trigger-401"`
- Future UI assertions: HeaderKeyValuePanel shows `WWW-Authenticate: Bearer ...`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-MVP-007 — Error Lab: trigger-428.post.ts — requiredHeader w response body

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M10, Scenariusz 8
**Functionality ID:** ERRORLAB-MVP-428-REQUIRED-HEADER  
**Implementation Area:** Nuxt BFF  
**Module / Route / Component:** `server/api/error-lab/trigger-428.post.ts`  
**Business Case:** Trigger-428 już istnieje i wywołuje backend poprawnie. Po BE-MVP-003 backend wyśle `requiredHeader` w body. Trigger musi jedynie forwardować ten body bez zmian.  
**Technical Case:** Trigger-428 używa `return result.data` — body jest forwardowane automatycznie. Jedyna zmiana to forward `Content-Type: application/problem+json` poprawnie.  
**Dependencies:** BE-MVP-003 (backend 428 z requiredHeader)  
**Files to inspect before coding:**
- `server/api/error-lab/trigger-428.post.ts`
**Files likely to change:**
- `server/api/error-lab/trigger-428.post.ts` — upewnij się że `Content-Type` jest forwardowany

#### Nuxt BFF work
- Upewnij się że `Content-Type: application/problem+json` jest w liście forwarding (już jest)
- Brak innych zmian — body jest forwardowane automatycznie przez `return result.data`

#### Future Playwright Coverage Note
- Future POM: `ProblemDetailsCardObject.expectRequiredHeader("If-Match")`
- Future UI assertions: ProblemDetailsCard shows `requiredHeader: "If-Match"`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### BFF-P2-001 — backendApi.ts: forward Last-Modified

**Phase:** Phase 2  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §13 Kategoria G
**Functionality ID:** BFF-P2-LAST-MODIFIED  
**Dependencies:** BE-P2-001

#### Nuxt BFF work
Dodaj `'Last-Modified'` do listy w `forwardBackendHeaders()`

#### Do not implement
- tests, fixtures, page objects

---

## 13. Frontend / Nuxt UI Dashboard Implementation Tasks

---

### FE-MVP-001 — ApiHeaders + useApiClient: nowe pola (retryAfter, wwwAuthenticate, idempotencyReplayed)

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M7, M6, M2, §24
**Functionality ID:** FE-MVP-API-HEADERS  
**Implementation Area:** Frontend / Nuxt  
**Module / Route / Component:** `app/types/api.ts`, `app/composables/useApiClient.ts`  
**Business Case:** UI musi znać `Retry-After` (wyświetlenie informacji "czekaj X sekund"), `WWW-Authenticate` (wyświetlenie w Error Lab), `Idempotency-Replayed` (rozróżnienie replay w UI).  
**Technical Case:** `ApiHeaders` jest centralnym source of truth dla typów headerów. `extractHeaders()` jest jedynym miejscem gdzie headery są wyciągane z raw response.  
**Dependencies:** BFF-MVP-001 (forward headerów)  
**Files to inspect before coding:**
- `app/types/api.ts`
- `app/composables/useApiClient.ts`
**Files likely to change:**
- `app/types/api.ts` — `ApiHeaders` interface
- `app/composables/useApiClient.ts` — `extractHeaders()`

#### Frontend work

**`app/types/api.ts`** — rozszerz `ApiHeaders`:
```typescript
export interface ApiHeaders {
  etag?: string
  location?: string
  vary?: string
  cacheControl?: string
  correlationId?: string
  allow?: string
  acceptPatch?: string
  // MVP additions:
  retryAfter?: string
  wwwAuthenticate?: string
  idempotencyReplayed?: string
}
```

**`app/composables/useApiClient.ts`** — rozszerz `extractHeaders()`:
```typescript
function extractHeaders(headers: Headers | undefined): ApiHeaders {
  if (!headers) return {}
  return {
    etag: headers.get('etag') ?? undefined,
    location: headers.get('location') ?? undefined,
    vary: headers.get('vary') ?? undefined,
    cacheControl: headers.get('cache-control') ?? undefined,
    correlationId: headers.get('x-correlation-id') ?? undefined,
    allow: headers.get('allow') ?? undefined,
    acceptPatch: headers.get('accept-patch') ?? undefined,
    // MVP additions:
    retryAfter: headers.get('retry-after') ?? undefined,
    wwwAuthenticate: headers.get('www-authenticate') ?? undefined,
    idempotencyReplayed: headers.get('idempotency-replayed') ?? undefined,
  }
}
```

**`error-lab.vue`** — rozszerz `responseHeaders` building:
```typescript
if (h.retryAfter) responseHeaders['Retry-After'] = h.retryAfter
if (h.wwwAuthenticate) responseHeaders['WWW-Authenticate'] = h.wwwAuthenticate
if (h.idempotencyReplayed) responseHeaders['Idempotency-Replayed'] = h.idempotencyReplayed
```

#### Nuxt UI Dashboard Template usage
- Brak nowych stron/komponentów — tylko typy i extractors

#### Acceptance criteria without tests
- Gdy backend wysyła `Retry-After: 30`, `response.headers.retryAfter === '30'`
- Gdy `WWW-Authenticate` w backend response, `response.headers.wwwAuthenticate` jest obecny
- Gdy `Idempotency-Replayed: true`, `response.headers.idempotencyReplayed === 'true'`
- Error Lab 429: `h.retryAfter` poprawnie buduje `responseHeaders['Retry-After']`

#### Future Playwright Coverage Note
- Future test type: Playwright API + UI
- Future API assertions: `response.headers.retryAfter` via `useApiClient` captured state
- Future UI assertions: HeaderKeyValuePanel renders `Retry-After: 30`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-002 — problem-details.schema.ts: znane extensions

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §23, M4, M5, M10
**Functionality ID:** FE-MVP-PROBLEM-SCHEMA  
**Implementation Area:** Frontend / Zod  
**Module / Route / Component:** `app/schemas/problem-details.schema.ts`  
**Business Case:** Zod schema z typowanymi extensions pozwala na type-safe dostęp do `correlationId`, `fieldErrors`, `requiredHeader` — zamiast `(problem as any).correlationId`.  
**Technical Case:** Schema ma już `.passthrough()` — nie gubimy unknown pól. Dodajemy typed hints dla known extensions.  
**Dependencies:** BE-MVP-003 (requiredHeader w body)  
**Files to inspect before coding:**
- `app/schemas/problem-details.schema.ts`
**Files likely to change:**
- `app/schemas/problem-details.schema.ts`

#### Zod Schema work
```typescript
export const problemDetailsSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().int().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  // Known backend extensions:
  correlationId: z.string().optional(),
  error: z.string().optional(),
  requiredHeader: z.string().optional(),
  details: z.array(z.object({
    field: z.string(),
    message: z.string(),
  })).optional(),
}).passthrough()  // preserve unknown extensions for RawJsonViewer
```

**`app/types/api.ts`** — zaktualizuj `ProblemDetails` type:
```typescript
export type ProblemDetails = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  correlationId?: string
  error?: string
  requiredHeader?: string
  details?: Array<{ field: string; message: string }>
  [key: string]: unknown
}
```

#### Acceptance criteria without tests
- `problemDetailsSchema.parse({ status: 428, requiredHeader: "If-Match" })` → `.requiredHeader === "If-Match"`
- `problemDetailsSchema.parse({ status: 400, details: [{ field: "amount", message: "..." }] })` → `.details[0].field === "amount"`
- Unknown extensions nie są gubione (`.passthrough()`)

#### Future Playwright Coverage Note
- Future test type: Playwright API + UI
- Future API assertions: `body.requiredHeader`, `body.details[0].field` z parsed ProblemDetails
- Future UI assertions: ProblemDetailsCard renders typed extension fields

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-003 — ProblemDetailsCard.vue: correlationId + fieldErrors + requiredHeader

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — M4, M5, M10, §21
**Functionality ID:** FE-MVP-PROBLEM-CARD  
**Implementation Area:** Frontend / Vue Component  
**Module / Route / Component:** `app/components/shared/ProblemDetailsCard.vue`  
**Business Case:** SDET musi widzieć `correlationId` i `fieldErrors` w UI — aby potwierdzić, że to co widoczne w sieci jest takie samo co w UI. Bez tego karta jest niepełna dla celów edukacyjnych.  
**Technical Case:** Komponent wyświetla tylko 5 pól RFC 7807. Pola `correlationId`, `details` (fieldErrors), `requiredHeader` przychodzą w body (znane nam po FE-MVP-002) ale nie są renderowane.  
**Dependencies:** FE-MVP-002 (typed ProblemDetails), BE-MVP-003 (requiredHeader)  
**Files to inspect before coding:**
- `app/components/shared/ProblemDetailsCard.vue`
- `app/types/api.ts` — `ProblemDetails` type
**Files likely to change:**
- `app/components/shared/ProblemDetailsCard.vue`

#### Frontend work
Dodaj sekcje extensions do `ProblemDetailsCard.vue`:

```vue
<!-- Po sekcji instance, dodaj: -->

<!-- Correlation ID -->
<div v-if="problem.correlationId" class="flex gap-2">
  <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Correlation ID</dt>
  <dd 
    data-testid="correlation-id-value"
    class="break-all font-mono text-xs text-gray-900 dark:text-gray-100"
  >
    {{ problem.correlationId }}
  </dd>
</div>

<!-- Required Header (428) -->
<div v-if="problem.requiredHeader" class="flex gap-2">
  <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Required Header</dt>
  <dd 
    data-testid="required-header-value"
    class="font-mono text-gray-900 dark:text-gray-100"
  >
    {{ problem.requiredHeader }}
  </dd>
</div>

<!-- Field Errors (400 validation) -->
<div v-if="problem.details?.length" class="space-y-1">
  <dt class="font-medium text-gray-500 dark:text-gray-400">Field Errors</dt>
  <ul data-testid="field-errors-list" class="space-y-1 pl-2">
    <li
      v-for="err in problem.details"
      :key="err.field"
      :data-field="err.field"
      class="text-xs"
    >
      <span class="font-mono font-medium">{{ err.field }}</span>:
      <span class="text-gray-700 dark:text-gray-300">{{ err.message }}</span>
    </li>
  </ul>
</div>
```

#### Nuxt UI Dashboard Template usage
- Rozszerza istniejący komponent `UCard` — bez nowych komponentów UI
- `data-testid` dodane zgodnie z konwencją projektu

#### RBAC / Security
- Żadnych danych sensitywnych — `correlationId` to UUID, nie token
- NIE wyświetlaj `error?.response?.headers?.authorization`

#### Acceptance criteria without tests
- Error Lab 400: ProblemDetailsCard wyświetla `fieldErrors` list z polami
- Error Lab 428: ProblemDetailsCard wyświetla `Required Header: If-Match`
- Każdy błąd z `correlationId`: ProblemDetailsCard wyświetla UUID pod "Correlation ID"
- Błędy bez tych pól: sekcje nie są renderowane (warunkowe `v-if`)

#### Future Playwright Coverage Note
- Future test type: Playwright UI + hybrid
- Future Playwright capabilities: `page.getByTestId()`, `expect().toContainText()`
- Future POM: `ProblemDetailsCardObject` z metodami `expectCorrelationId()`, `expectFieldError()`, `expectRequiredHeader()`
- Future locators/testids:
  - `data-testid="problem-details-card"` (już istnieje)
  - `data-testid="correlation-id-value"` (NOWE)
  - `data-testid="required-header-value"` (NOWE)
  - `data-testid="field-errors-list"` (NOWE)
  - `data-field="amountMinor"` (atrybut na `<li>`)

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-004 — error-lab.vue: nowe scenariusze (429, 304, idempotency-replay)

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §20, Scenariusze 4, 5, 7
**Functionality ID:** FE-MVP-ERRORLAB-NEW-SCENARIOS  
**Implementation Area:** Frontend / Nuxt Page  
**Module / Route / Component:** `app/pages/error-lab.vue`  
**Business Case:** Error Lab jest główną powierzchnią edukacyjną dla HTTP. Brakuje 3 kluczowych scenariuszy: 429 (rate limit), 304 (conditional GET), replay (idempotency).  
**Dependencies:** BFF-MVP-003, BFF-MVP-004, BFF-MVP-005  
**Files to inspect before coding:**
- `app/pages/error-lab.vue` — lista `scenarios`
**Files likely to change:**
- `app/pages/error-lab.vue`

#### Frontend work
Dodaj do listy `scenarios[]`:

```typescript
{
  status: 429,
  title: 'Too Many Requests',
  description: 'Wywołaj endpoint, który zwraca 429 z nagłówkiem Retry-After. Używane, gdy klient wysyła zbyt wiele żądań w krótkim czasie. Retry-After mówi, ile sekund czekać.',
  proxyPath: '/api/error-lab/trigger-429',
  proxyMethod: 'POST',
  requestLabel: {
    method: 'POST',
    path: '/api/payment-orders (rate limited)',
    headers: { Authorization: 'Bearer ••••••••' },
  },
  state: makeScenarioState(),
},
{
  status: 304,
  title: 'Not Modified (Conditional GET)',
  description: 'Realizuje dwuetapowy przepływ: najpierw GET payment order → ETag, następnie GET z If-None-Match: ETag → 304. Serwer potwierdza, że zasób się nie zmienił.',
  proxyPath: '/api/error-lab/trigger-304',
  proxyMethod: 'GET',
  requestLabel: {
    method: 'GET',
    path: '/api/merchants/{id}/payment-orders/{orderId}',
    headers: { Authorization: 'Bearer ••••••••', 'If-None-Match': '(previous ETag)' },
  },
  state: makeScenarioState(),
},
{
  status: 200,  // replay zwraca 200
  title: 'Idempotency Replay (200 vs 201)',
  description: 'Wywołaj dwa razy z tym samym Idempotency-Key. Pierwsze żądanie zwraca 201 (Created). Drugie żądanie zwraca 200 (Replayed) z nagłówkiem Idempotency-Replayed: true.',
  proxyPath: '/api/error-lab/trigger-idempotency-replay',
  proxyMethod: 'POST',
  requestLabel: {
    method: 'POST',
    path: '/api/merchants/{id}/payment-orders',
    headers: { 
      Authorization: 'Bearer ••••••••',
      'Idempotency-Key': '(same key, second time)',
    },
  },
  state: makeScenarioState(),
},
```

#### Nuxt UI Dashboard Template usage
- Używa istniejącego wzorca kart `UCard` w `scenarios` loop
- `data-testid="error-lab-trigger-429"`, `error-lab-trigger-304`, `error-lab-trigger-idempotency-replay` (automatycznie z `scenario.status` ale dla `200` użyj `idempotency-replay` jako key)
- **Uwaga:** Scenariusz idempotency-replay ma `status: 200` — `data-testid` potrzebuje osobnego klucza. Rozważ dodanie `key` field do `ScenarioConfig`:
  ```typescript
  interface ScenarioConfig {
    key: string  // dla data-testid
    status: number
    ...
  }
  // i w template: :data-testid="`error-lab-trigger-${scenario.key}`"
  ```

#### RBAC / Security
- Error Lab jest dostępny dla wszystkich authenticated users (bez role gate) — poprawne
- Token zawsze maskowany przez `HeaderKeyValuePanel`

#### Acceptance criteria without tests
- Error Lab strona zawiera karty: 429, 304, Idempotency Replay
- Kliknięcie "Trigger 429" → karta pokazuje status badge 429, `Retry-After: 30` w HeaderKeyValuePanel
- Kliknięcie "Trigger 304" → karta pokazuje status badge 304
- Kliknięcie "Trigger Idempotency Replay" → pierwsze: status 201, drugie: status 200 + `Idempotency-Replayed: true`

#### Future Playwright Coverage Note
- Future test type: Playwright UI + hybrid API+UI
- Future Playwright capabilities: `page.click()`, `page.waitForResponse()`, `expect().toContainText()`
- Future POM: `ErrorLabPage` z metodami `triggerScenario(status)`, `expectResult(status)`
- Future locators/testids:
  - `data-testid="error-lab-trigger-429"`
  - `data-testid="error-lab-trigger-304"`
  - `data-testid="error-lab-trigger-idempotency-replay"`
- Future UI assertions: HeaderKeyValuePanel po trigger 429 zawiera `Retry-After`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-005 — error-lab.vue: responseHeaders building dla nowych headerów

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §21
**Functionality ID:** FE-MVP-ERRORLAB-HEADERS  
**Implementation Area:** Frontend / Nuxt Page  
**Module / Route / Component:** `app/pages/error-lab.vue` — `triggerScenario()` function  
**Business Case:** Po FE-MVP-001 nowe headery są w `ApiHeaders`, ale `error-lab.vue` buduje `responseHeaders: Record<string, string>` ręcznie — trzeba dodać nowe mapowania.  
**Dependencies:** FE-MVP-001  
**Files likely to change:**
- `app/pages/error-lab.vue` — blok `responseHeaders` w `triggerScenario()`

#### Frontend work
```typescript
// W triggerScenario(), blok budowania responseHeaders:
const responseHeaders: Record<string, string> = {}
const h = response.headers
if (h.etag) responseHeaders['ETag'] = h.etag
if (h.cacheControl) responseHeaders['Cache-Control'] = h.cacheControl
if (h.vary) responseHeaders['Vary'] = h.vary
if (h.correlationId) responseHeaders['X-Correlation-ID'] = h.correlationId
if (h.location) responseHeaders['Location'] = h.location
if (h.allow) responseHeaders['Allow'] = h.allow
if (h.acceptPatch) responseHeaders['Accept-Patch'] = h.acceptPatch
// MVP additions:
if (h.retryAfter) responseHeaders['Retry-After'] = h.retryAfter
if (h.wwwAuthenticate) responseHeaders['WWW-Authenticate'] = h.wwwAuthenticate
if (h.idempotencyReplayed) responseHeaders['Idempotency-Replayed'] = h.idempotencyReplayed
```

#### Acceptance criteria without tests
- Po trigger-429: `HeaderKeyValuePanel` wyświetla `Retry-After: 30`
- Po trigger-401: `HeaderKeyValuePanel` wyświetla `WWW-Authenticate: Bearer ...`
- Po trigger-idempotency-replay: `HeaderKeyValuePanel` wyświetla `Idempotency-Replayed: true`

#### Future Playwright Coverage Note
- Future UI assertions: `HeaderPanelObject.expectHeader('Retry-After')` dla Error Lab 429

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-006 — Merchant Detail Page: /admin/merchants/[merchantId].vue

**Phase:** MVP  
**Source Document:**
- `playwright-sdet-feature-roadmap(1).md` — F-05 Merchant Detail
- `payment-quality-engineering-lab-business-technical-cases.md` — Payment Operations Core
**Functionality ID:** FE-MVP-MERCHANT-DETAIL  
**Implementation Area:** Frontend / Nuxt Page  
**Module / Route / Component:** `app/pages/admin/merchants/[merchantId].vue` (NOWY)  
**Business Case:** Brakuje strony szczegółów merchanty. Kliknięcie na merchantę w liście nie ma celu nawigacyjnego. Merchant detail jest prerequisitem dla payment lifecycle UI.  
**Technical Case:** Route `/admin/merchants/[merchantId]/payments/` istnieje (katalog), ale nie ma indeksowej strony dla samego merchanty. Serwer API `GET /api/merchants/{id}` istnieje i jest obsługiwany przez `server/api/merchants/[id].get.ts`.  
**Why now:** Blokuje nawigację z merchant list do merchant context. Bez tego użytkownik nie może zobaczyć szczegółów i statusu merchanty.  
**Dependencies:** —  
**Files to inspect before coding:**
- `server/api/merchants/[id].get.ts`
- `app/pages/admin/merchants/index.vue` (wzorzec strony)
- `app/schemas/merchant.schema.ts`
- `app/composables/useMerchantsApi.ts`
- `app/components/merchant/MerchantTable.vue`
- `app/components/shared/MerchantStatusCard.vue`
**Files likely to change:**
- `app/pages/admin/merchants/[merchantId].vue` (NOWY)
- `app/pages/admin/merchants/index.vue` — dodaj link/nawigację do detail page

#### Frontend work
Nowa strona `[merchantId].vue`:

**Layout:** `UDashboardPanel` + `UDashboardNavbar` (tytuł: "Merchant Details")

**Sekcje:**
1. **Merchant Info Card** (`UCard`): nazwa, reference, status badge, tenantId, created_at
2. **Status Actions** (jeśli `canUpdateMerchantStatus`): przyciski Activate / Suspend (istniejące przez API)
3. **ETag Display**: `EtagDisplay` komponent ze stanem wersji zasobu
4. **X-Correlation-ID Display**: ostatni correlation ID requestu
5. **Response Headers Panel**: `HeaderKeyValuePanel` dla ostatniego response
6. **Link to Payment Orders**: `UButton` → `/admin/merchants/{merchantId}/payments`

**Data loading:**
```typescript
const { getMerchant } = useMerchantsApi()
const response = await getMerchant(merchantId)
```

**`data-testid` wymagane:**
- `data-testid="merchant-detail-panel"`
- `data-testid="merchant-name"` (element z nazwą)
- `data-testid="merchant-status-badge"`
- `data-testid="merchant-reference"`
- `data-testid="action-activate-merchant"` (jeśli `canUpdateMerchantStatus`)
- `data-testid="action-suspend-merchant"` (jeśli `canUpdateMerchantStatus`)
- `data-testid="merchant-payment-orders-link"`

**Nawigacja z listy:**
W `app/components/merchant/MerchantTable.vue` lub `index.vue` — kliknięcie wiersza/merchanty → `navigateTo(/admin/merchants/${merchantId})`

#### Nuxt UI Dashboard Template usage
- `UDashboardPanel`, `UDashboardNavbar`, `UCard` — zgodne z istniejącym template
- `MerchantStatusBadge` — istniejący komponent
- `EtagDisplay` — istniejący komponent (do reuse)
- `HeaderKeyValuePanel` — istniejący komponent

#### RBAC / Security
- `canReadMerchants` → strona dostępna
- `canUpdateMerchantStatus` → przyciski Activate/Suspend widoczne
- `canCreatePaymentOrder` → link do tworzenia płatności

#### Acceptance criteria without tests
- `/admin/merchants/{uuid}` ładuje i wyświetla dane merchanty
- Status badge wyświetla ACTIVE / SUSPENDED / PENDING
- Jeśli `canUpdateMerchantStatus`: przyciski Activate/Suspend są widoczne
- Link do payment orders istnieje

#### Future Playwright Coverage Note
- Future test type: Playwright UI + hybrid
- Future Playwright capabilities: `page.goto()`, `page.getByTestId()`, `page.getByRole('button')`
- Future POM: `MerchantDetailPage` z metodami `expectMerchantName()`, `expectStatus()`, `clickActivate()`
- Future locators/testids: wszystkie `data-testid` wymienione powyżej
- Future UI assertions:
  - Merchant name visible: `getByTestId('merchant-name')`
  - Status badge: `getByTestId('merchant-status-badge').toContainText('ACTIVE')`
  - RBAC: `getByTestId('action-activate-merchant').toBeVisible()` tylko dla authorized role

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-007 — Dashboard: tenant context badge/indicator

**Phase:** MVP  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §17 Kategoria K
- `payment-quality-engineering-lab-business-technical-cases.md` — Tenant context indicator
**Functionality ID:** FE-MVP-TENANT-BADGE  
**Implementation Area:** Frontend / Vue Component + Layout  
**Module / Route / Component:** `app/layouts/dashboard.vue`, `app/components/AppUserMenu.vue`  
**Business Case:** Użytkownik musi wiedzieć, z jakim tenantem pracuje — szczególnie gdy tenant jest SUSPENDED (banner ostrzegawczy). Bez tego nie da się czytelnie zademonstrować scenariusza suspended tenant.  
**Technical Case:** `useUserSession()` dostarcza `user` z session. Tenant info jest dostępna w session jeśli jest w JWT claims (`tenant_id`, `tenant_reference`). Alternatywnie fetch `/api/status` lub endpoint tenanta.  
**Dependencies:** DB-MVP-001 (SUSPENDED tenant), SEED-MVP-001  
**Files to inspect before coding:**
- `app/layouts/dashboard.vue`
- `app/components/AppUserMenu.vue`
- `app/components/AppTeamsMenu.vue`
- `shared/types/auth.d.ts` — sprawdź co jest w user session
**Files likely to change:**
- `app/layouts/dashboard.vue` (dodanie bannera)
- `app/components/AppTeamsMenu.vue` (wyświetlenie tenant name) lub nowy `TenantContextBadge.vue`

#### Frontend work

**`TenantContextBadge.vue`** (NOWY komponent, opcjonalny):
```vue
<template>
  <div v-if="tenantStatus === 'SUSPENDED'" data-testid="tenant-suspended-banner">
    <UAlert
      color="error"
      icon="i-lucide-ban"
      title="Tenant suspended"
      description="This tenant account is suspended. Payment operations are unavailable."
    />
  </div>
  <div v-else-if="tenantName" data-testid="tenant-context-badge">
    <UBadge color="neutral" variant="subtle">{{ tenantName }}</UBadge>
  </div>
</template>
```

**W `dashboard.vue`** — dodaj pod nawigacją:
```vue
<TenantContextBadge 
  :tenant-name="user?.tenantName" 
  :tenant-status="user?.tenantStatus"
/>
```

**Ważna uwaga:** Wyświetlaj tylko tenant NAME i status — nigdy `tenant_id` UUID ani `tenant_reference` internal key.

**`data-testid` wymagane:**
- `data-testid="tenant-suspended-banner"` (widoczny gdy SUSPENDED)
- `data-testid="tenant-context-badge"` (widoczny gdy ACTIVE)

#### RBAC / Security
- NIE wyświetlaj `tenant_id` UUID — to internal identifier
- NIE wyświetlaj `tenant_reference` — to internal key
- Wyświetlaj tylko `name` (np. "Suspended Demo Tenant") i `status`

#### Acceptance criteria without tests
- Użytkownik z ACTIVE tenantem: badge z nazwą tenanta widoczny
- Użytkownik z SUSPENDED tenantem: czerwony banner "Tenant suspended" widoczny
- PLATFORM_ADMIN (bez tenant scope): badge nie wyświetlany (lub "Platform")

#### Future Playwright Coverage Note
- Future test type: Playwright UI + multi-role
- Future POM: `DashboardLayout` z `expectTenantSuspendedBanner()`, `expectTenantBadge(name)`
- Future locators/testids: `data-testid="tenant-suspended-banner"`, `data-testid="tenant-context-badge"`
- Future UI assertions: suspended user widzi banner, nie może kliknąć Create Payment Order

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-MVP-008 — PaymentOrderLifecycleActions: pola kwotowe dla capture i refund

**Phase:** MVP  
**Source Document:**
- `payment-quality-engineering-lab-business-technical-cases.md` — Payment Operations Core, partial capture/refund
- `playwright-sdet-feature-roadmap(1).md` — F-07 Partial Capture/Refund
**Functionality ID:** FE-MVP-PARTIAL-LIFECYCLE  
**Implementation Area:** Frontend / Vue Component  
**Module / Route / Component:** `app/components/shared/PaymentOrderLifecycleActions.vue`  
**Business Case:** `CaptureRequest` i `RefundRequest` przyjmują opcjonalny `amountMinor` — ale UI nie ma inputu kwotowego. Bez tego użytkownik nie może wykonać partial capture/refund z UI.  
**Technical Case:** Backend `PaymentLifecycleService.capture()` i `refund()` przyjmują `Long amountMinor` (null = full capture/refund). Frontend API calls w `usePaymentLifecycleApi.ts` muszą wysłać kwotę.  
**Dependencies:** —  
**Files to inspect before coding:**
- `app/components/shared/PaymentOrderLifecycleActions.vue`
- `app/composables/usePaymentLifecycleApi.ts`
- `app/stores/payment-orders.ts`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/CaptureRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/RefundRequest.java`
**Files likely to change:**
- `app/components/shared/PaymentOrderLifecycleActions.vue`
- `app/composables/usePaymentLifecycleApi.ts` (jeśli brak amount param)

#### Frontend work

W `PaymentOrderLifecycleActions.vue`:
- Dla przycisku **Capture**: dodaj `UInput` z `type="number"` przed przyciskiem (lub w modal), `placeholder="Partial amount (leave empty for full)"`, `aria-label="Capture amount"`
- Dla przycisku **Refund**: analogicznie `UInput`, `aria-label="Refund amount"`
- Wartość pusta = full capture/refund (null sent to backend)
- Lokalna zmienna reaktywna: `captureAmount = ref<number | null>(null)`, `refundAmount = ref<number | null>(null)`

**UI patterns:** `UFormField` + `UInput` + `UButton` — zgodne z Nuxt UI Dashboard Template

**`data-testid` wymagane:**
- `data-testid="capture-amount-input"`
- `data-testid="refund-amount-input"`
- istniejące: `data-testid="lifecycle-capture"`, `data-testid="lifecycle-refund"`

#### Acceptance criteria without tests
- Formularz capture wyświetla input kwotowy
- Pusty input = full capture (wysyła `amountMinor: null` lub pomija pole)
- Wypełniony input = partial capture (wysyła `amountMinor: value`)
- Analogicznie dla refund

#### Future Playwright Coverage Note
- Future test type: Playwright UI
- Future POM: `LifecycleActionsPanel` z `capturePartial(amount)`, `refundPartial(amount)`
- Future locators/testids: `data-testid="capture-amount-input"`, `data-testid="refund-amount-input"`
- Future UI assertions: po partial capture `capturedAmountMinor` w detail ≠ `amountMinor`

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

### FE-P2-001 — Playwright upgrade 1.60.0 → 1.61.0

**Phase:** Phase 2  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §3
**Functionality ID:** FE-P2-PLAYWRIGHT-UPGRADE  
**Implementation Area:** Frontend / Dependencies  
**Module / Route / Component:** `apps/frontend/package.json`, `playwright.config.ts`  
**Business Case:** Playwright 1.61 wprowadza `page.localStorage`, `page.sessionStorage`, `expect(response).toMatchObject()` — kluczowe dla asercji bezpieczeństwa tokenów.  
**Technical Case:** Bez breaking changes. Upgrade jest safe.  
**Dependencies:** FE-MVP-001 (ApiHeaders completed)

#### Frontend work
1. Zmień `package.json`: `"@playwright/test": "1.61.0"` (lub latest stable 1.61.x)
2. Uruchom `corepack pnpm install`
3. Zaktualizuj przeglądarki: `corepack pnpm exec playwright install chromium`
4. Sprawdź typy: `corepack pnpm typecheck`
5. Usuń `waitForTimeout(500)` z `payment-order-create.spec.ts:16` — zamień na `waitForResponse`
6. Dodaj konfigurację screenshot w `playwright.config.ts`:
   ```typescript
   screenshot: 'only-on-failure',
   video: 'retain-on-failure',
   ```

#### Acceptance criteria without tests
- `package.json` zawiera `"@playwright/test": "1.61.0"`
- `corepack pnpm typecheck` przechodzi
- Istniejące testy Playwright przechodzą

#### Future Playwright Coverage Note
- Odblokuje: `page.localStorage`, `page.sessionStorage`, `expect(response).toMatchObject()`
- Future test: `await page.localStorage()` → asercja brak JWT tokenu

#### Do not implement
- tests, fixtures, page objects, Playwright specs

---

### FE-P2-002 — Multi-role Playwright config (bez testów)

**Phase:** Phase 2  
**Source Document:**
- `playwright-161-http-api-properties-test-strategy.md` — §27
**Functionality ID:** FE-P2-MULTI-ROLE  
**Implementation Area:** Frontend / Playwright Config  
**Dependencies:** FE-P2-001, data isolation strategy

#### Frontend work
- Dodaj auth setup dla merchant-admin roli w `playwright.config.ts`
- `tests/.auth/merchant-admin.json` — storage state placeholder
- `tests/auth/merchant-admin.setup.ts` — analogia do `auth.setup.ts`
- **NIE uruchamiaj** `fullyParallel: true` — data isolation musi być gotowa

#### Do not implement
- tests, fixtures, page objects, Playwright specs

---

## 14. Seed Data Implementation Tasks

---

### SEED-MVP-001 — Suspended merchant dla PLACEHOLDER tenanta

**Phase:** MVP  
**Source Document:**
- `payment-quality-engineering-lab-business-technical-cases.md` — Tenant seed
- `playwright-161-http-api-properties-test-strategy.md` — §17
**Functionality ID:** SEED-MVP-SUSPENDED-MERCHANT  
**Implementation Area:** Seed Data / Backend  
**Module / Route / Component:** `payment/internal/application/PaymentSeedService.java`, `merchant/internal/application/MerchantSeedService.java`  
**Business Case:** Po zmianie PLACEHOLDER_TENANT_ID na SUSPENDED (DB-MVP-001), potrzebny jest merchant przypisany do tego tenanta — do demonstrowania scenariusza suspended tenant w Error Lab i UI.  
**Technical Case:** Seed data jest ładowana przez `POST /api/test/seed` endpoint (oddzielnie) lub przez SpringBoot aplikacyjny init. Sprawdź istniejące seed classes.  
**Dependencies:** DB-MVP-001 (SUSPENDED tenant migration)  
**Files to inspect before coding:**
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentSeedService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantSeedService.java`
- Znajdź klasy konfigurujące seed seeds (np. `@Component` inicjujące seed)
**Files likely to change:**
- Plik konfiguracyjny seed data (do ustalenia po inspekcji)

#### Data seeding
Dodaj suspended merchant:
```java
// MerchantSeed dla SUSPENDED tenanta:
new MerchantSeed(
    UUID.fromString("33333333-3333-3333-3333-333333333333"),  // stały UUID dla powtarzalności
    "SUSPENDED-DEMO-MERCHANT",
    "Suspended Demo Merchant",
    PLACEHOLDER_TENANT_UUID,  // UUID z PLACEHOLDER_TENANT_ID
    "ACTIVE"  // merchant ACTIVE, ale jego tenant SUSPENDED
)
```

**Uwaga:** Merchant może być ACTIVE — to tenant jest SUSPENDED. `TenantResolver` blokuje na poziomie JWT → tenant lookup.

#### Acceptance criteria without tests
- Baza zawiera merchant z `merchant_reference = 'SUSPENDED-DEMO-MERCHANT'`
- GET `/api/merchants` z tokenem PLATFORM_ADMIN widzi ten merchant
- POST `/api/merchants/{suspended-merchant-id}/payment-orders` z tokenem tenantowym → 403 (TenantResolver blokuje)

#### Future Playwright Coverage Note
- Future test type: Playwright API multi-role
- Future API assertions: `expect(response.status()).toBe(403)` dla operacji na suspended tenant
- Future UI assertions: Banner "Tenant suspended" widoczny dla użytkownika z PLACEHOLDER_TENANT_ID

#### Do not implement
- tests, fixtures, page objects, REST Assured tests, Playwright specs

---

## 15. Problem Details and Headers Implementation Tasks

*(Zadania z tej sekcji są wkomponowane w §11 Backend i §13 Frontend jako BE-MVP-001 do FE-MVP-003.)*

### Podsumowanie headerów — Decision Matrix

| Header | Backend | BFF forward | ApiHeaders | ProblemDetailsCard | CORS expose | Status |
|--------|---------|-------------|------------|-------------------|-------------|--------|
| `ETag` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `X-Correlation-ID` | ✅ | ✅ | ✅ | ✅ (FE-MVP-003) | ✅ | MVP |
| `Cache-Control: no-store` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `Vary` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `Location` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `Allow` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `Accept-Patch` | ✅ | ✅ | ✅ | N/A | ✅ | OK |
| `Idempotency-Replayed` | BE-MVP-002 | BFF-MVP-001 | FE-MVP-001 | N/A | BE-MVP-004 | **MVP** |
| `304 Not Modified` | BE-MVP-001 | BFF-MVP-002 | N/A | N/A | N/A | **MVP** |
| `Retry-After` | BFF-mock only | BFF-MVP-001 | FE-MVP-001 | N/A | BE-MVP-004 | **MVP** |
| `WWW-Authenticate` | ✅ (Spring) | BFF-MVP-001 | FE-MVP-001 | N/A | BE-MVP-004 | **MVP** |
| `requiredHeader` (body) | BE-MVP-003 | auto | FE-MVP-002 | FE-MVP-003 | N/A | **MVP** |
| `fieldErrors` (body) | ✅ | auto | FE-MVP-002 | FE-MVP-003 | N/A | **MVP** |
| `Last-Modified` | BE-P2-001 | BFF-P2-001 | P2 | N/A | P2 | Phase 2 |
| `RateLimit-*` | Phase 3 | Phase 3 | Phase 3 | N/A | Phase 3 | Phase 3 |

---

## 16. Error Lab / HTTP Learning Surface Tasks

### Stan Error Lab — before/after

| Status | Przed MVP | Po MVP | Trigger |
|--------|-----------|--------|---------|
| 400 | ✅ | ✅ fieldErrors w karcie | FE-MVP-003 |
| 401 | ✅ | ✅ + WWW-Authenticate header | BFF-MVP-006 |
| 403 | ✅ | ✅ | — |
| 404 | ✅ | ✅ | — |
| 406 | ✅ | ✅ | — |
| 409 | ✅ (conflict) | ✅ | — |
| 412 | ✅ | ✅ | — |
| 415 | ✅ | ✅ | — |
| 428 | ✅ | ✅ + requiredHeader w karcie | BFF-MVP-007 + FE-MVP-003 |
| **429** | ❌ | ✅ + Retry-After | **BFF-MVP-003** |
| **304** | ❌ | ✅ + ETag/If-None-Match | **BFF-MVP-004** |
| **Idempotency Replay** | ❌ | ✅ + Idempotency-Replayed | **BFF-MVP-005** |

### Error Lab page changes checklist

- [ ] Dodaj `ScenarioConfig.key` field dla unikalnych `data-testid` (szczególnie dla `status: 200` replay)
- [ ] Dodaj 3 nowe scenariusze do `scenarios[]` (FE-MVP-004)
- [ ] Rozszerz `responseHeaders` building (FE-MVP-005)
- [ ] Upewnij się że `ProblemDetailsCard` dostaje pełny body (FE-MVP-003)

---

## 17. RBAC and Tenant Isolation Tasks

### Obecny stan RBAC

| Capability | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY |
|-----------|:-:|:-:|:-:|:-:|:-:|
| canCreateMerchant | ✅ | ✅ | ❌ | ❌ | ❌ |
| canReadMerchants | ✅ | ✅ | ❌ | ✅ | ✅ |
| canUpdateMerchantStatus | ✅ | ✅ | ❌ | ❌ | ❌ |
| canCreatePaymentOrder | ❌ | ❌ | ✅ | ❌ | ❌ |
| canReadMerchantPayments | ❌ | ✅ | ✅ | ❌ | ❌ |
| canReadPlatformPayments | ✅ | ❌ | ❌ | ✅ | ✅ |
| canRunLifecycle | ✅ | ❌ | ✅ | ❌ | ❌ |
| canViewAuditLog | ✅ | ✅ | ❌ | ✅ | ❌ |
| canManageUsers | ✅ | ✅ | ❌ | ❌ | ❌ |

### RBAC tasks MVP

**FE-MVP-006** (Merchant Detail Page): RBAC applied via `can.value.*`

**FE-MVP-007** (Tenant Badge): PLATFORM_ADMIN nie ma tenant scope → badge nie wyświetlany

**Masked 404 vs 403 — existing policy:**
Backend już implementuje masked 404 dla payment orders cross-merchant (404 zamiast 403, linia 109 w PaymentOrderController). Brak zmian wymaganych.

### Error Lab cross-tenant scenario

**Odrzucono w MVP** — wymaga multi-role Playwright setup (Phase 2). Dodać jako Error Lab scenariusz w Phase 2.

---

## 18. Upload / Download Tasks

Wszystkie zadania upload/download → **Phase 2/3** (brak backend endpoint download):

| Task | Phase | Dependency |
|------|-------|-----------|
| `Content-Disposition` dla CSV export payment orders | Phase 3 | BE-P3-003 |
| `Content-Length` forward | Phase 3 | CSV export |
| `Last-Modified` header | Phase 2 | BE-P2-001 |
| `HEAD /api/merchants/{id}/payment-orders/{id}` — już istnieje | OK | — |

**Uwaga:** `HEAD` endpoint już istnieje w `PaymentOrderController.headPaymentOrder()`. Forward przez Nuxt — brak dedykowanego route w `server/api/`, dodać `server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].head.ts`.

---

## 19. Support / Risk / Compliance Tasks

**Phase 2** — support search (basic):

### Support-P2-001 — Strona /admin/support (basic search)

**Phase:** Phase 2  
**Source Document:**
- `payment-quality-engineering-lab-business-technical-cases.md` — Support/Risk/Compliance Phase 2
**Functionality ID:** FE-P2-SUPPORT-SEARCH

#### Frontend work
- Nowa route: `app/pages/admin/support/index.vue`
- Search form: UInput dla `merchantReference` lub `clientOrderReference`
- Wyniki: `UTable` z payment orders
- Backend: `GET /api/merchants/{id}/payment-orders?clientOrderReference=X` (istniejący endpoint)

**Warunek:** Dodaj dopiero gdy `SUPPORT_AGENT` rola ma real use case w systemie. W MVP SUPPORT_AGENT już istnieje w RBAC matrix.

#### Do not implement
- tests, fixtures, page objects

---

## 20. Future Playwright Coverage Map

| Functionality | Implementation Tasks | Future Playwright API Coverage | Future Playwright UI Coverage | Future POM / Component Object | Future TS Concepts |
|--------------|---------------------|-------------------------------|------------------------------|------------------------------|--------------------|
| X-Correlation-ID korelacja | FE-MVP-001, BFF-MVP-001 | `waitForResponse()` → `response.headers()['x-correlation-id']` | `HeaderKeyValuePanel` zawiera UUID | `HeaderPanelObject.expectCorrelationId()` | UUID regex validation |
| 304 Not Modified | BE-MVP-001, BFF-MVP-002, BFF-MVP-004 | `APIRequestContext.get()` z `If-None-Match` → status 304 | Error Lab karta 304 | `NetworkAssertions.expectNotModified()` | Conditional requests |
| Idempotency replay | BE-MVP-002, BFF-MVP-005, FE-MVP-001 | POST×2 → `Idempotency-Replayed: true`, status 200 vs 201 | Error Lab karta replay | `NetworkAssertions.expectIdempotencyReplayed()` | Idempotency pattern |
| 429 + Retry-After | BFF-MVP-003, FE-MVP-001, FE-MVP-004 | Status 429, `retry-after` header | Error Lab karta 429, HeaderKeyValuePanel | `HeaderPanelObject.expectRetryAfter()` | Rate limit pattern |
| Problem Details extensions | FE-MVP-002, FE-MVP-003, BE-MVP-003 | `response.json().correlationId`, `.requiredHeader`, `.details[]` | ProblemDetailsCard sekcje extension | `ProblemDetailsCardObject.expectCorrelationId()`, `expectFieldError()`, `expectRequiredHeader()` | RFC 9457 extensions |
| WWW-Authenticate | BFF-MVP-006, FE-MVP-001 | 401 response → `www-authenticate` header starts with `Bearer` | Error Lab karta 401 shows header | `NetworkAssertions.expectWwwAuthenticate()` | Bearer auth challenge |
| 428 + requiredHeader | BE-MVP-003, BFF-MVP-007, FE-MVP-003 | 428 body.requiredHeader === "If-Match" | ProblemDetailsCard: "Required Header: If-Match" | `ProblemDetailsCardObject.expectRequiredHeader()` | Precondition required |
| Authorization token hygiene | FE-MVP-006, FE-MVP-007, FE-P2-001 | `page.localStorage()` no JWT (1.61+), `sessionStorage()` no JWT | `HeaderKeyValuePanel` shows `Bearer ••••••••` | `TokenLeakAssertions.assertNoTokenInStorage()` | JWT security |
| Merchant detail page | FE-MVP-006 | GET `/server/api/merchants/{id}` → 200 | Merchant detail UI elements | `MerchantDetailPage` | Navigation, RBAC |
| Partial capture/refund | FE-MVP-008 | POST capture z `amountMinor` | Input field, value in detail | `LifecycleActionsPanel.capturePartial()` | Business domain |
| Suspended tenant | DB-MVP-001, SEED-MVP-001, FE-MVP-007 | POST z tenant token → 403 | Banner "Tenant suspended" | `DashboardLayout.expectTenantSuspendedBanner()` | Tenant isolation |
| CORS expose headers | BE-MVP-004 | OPTIONS preflight → expose list includes `X-Correlation-ID` | N/A | N/A | CORS preflight |
| ETag + If-Match → 412 | (istniejące) | Lifecycle z stale ETag → 412 | ProblemDetailsCard status 412 | `NetworkAssertions.expectPreconditionFailed()` | Optimistic locking |
| Last-Modified (P2) | BE-P2-001, BFF-P2-001 | GET → `last-modified` header is RFC 1123 | N/A | N/A | Conditional GET |
| CSV export (P3) | BE-P3-003 | GET export → `content-disposition: attachment` | File download trigger | N/A | Download headers |

---

## 21. Future POM / Component Object Map

| Component Object | Maps to | Key Methods | Key Locators/testids |
|-----------------|---------|-------------|---------------------|
| `HeaderPanelObject` | `HeaderKeyValuePanel.vue` | `expectHeader(name)`, `expectHeaderValue(name, val)`, `expectAuthorizationMasked()`, `expectCorrelationId()`, `expectRetryAfter()` | `getByTestId('http-headers-panel')` |
| `ProblemDetailsCardObject` | `ProblemDetailsCard.vue` | `expectVisible()`, `expectStatus(code)`, `expectDetail(text)`, `expectCorrelationId()`, `expectFieldError(field, msg)`, `expectRequiredHeader(name)` | `getByTestId('problem-details-card')`, `getByTestId('correlation-id-value')`, `getByTestId('field-errors-list')`, `getByTestId('required-header-value')` |
| `ErrorLabPage` | `error-lab.vue` | `triggerScenario(key)`, `expectResultStatus(key, status)`, `getHeaderPanel(key)`, `getProblemCard(key)` | `getByTestId('error-lab-trigger-{key}')` |
| `MerchantDetailPage` | `/admin/merchants/[merchantId].vue` | `expectMerchantName(name)`, `expectStatus(status)`, `clickActivate()`, `clickSuspend()`, `clickPaymentOrders()` | `getByTestId('merchant-detail-panel')`, `getByTestId('merchant-name')`, `getByTestId('merchant-status-badge')` |
| `MerchantListPage` | `/admin/merchants/index.vue` | `expectMerchantsCount(n)`, `clickMerchant(name)`, `clickCreate()` | `getByTestId('action-create-merchant')` |
| `DashboardLayout` | `layouts/dashboard.vue` | `expectNavLink(label)`, `expectTenantBadge(name)`, `expectTenantSuspendedBanner()` | `getByTestId('tenant-context-badge')`, `getByTestId('tenant-suspended-banner')` |
| `LifecycleActionsPanel` | `PaymentOrderLifecycleActions.vue` | `authorize()`, `capturePartial(amount)`, `captureAll()`, `refundPartial(amount)`, `cancel()` | `getByTestId('lifecycle-authorize')`, `getByTestId('lifecycle-capture')`, `getByTestId('capture-amount-input')` |
| `NetworkAssertions` | (utility, no UI) | `captureEtag(url)`, `captureCorrelationId(url)`, `expectNotModified(etag)`, `expectIdempotencyReplayed(expected)`, `expectRateLimited()`, `expectNoTokenInStorage()` | — |

---

## 22. Implementation Order

### Sprint 1 (MVP — tydzień 1-2)

Kolejność według zależności:

```
1. DB-MVP-001  ← niezależny, fundament dla tenanta
2. BE-MVP-004  ← CORS, niezależny, odblokuje network tests
3. BE-MVP-001  ← If-None-Match/304, niezależny od pozostałych
4. BE-MVP-002  ← Idempotency-Replayed, niezależny
5. BE-MVP-003  ← requiredHeader 428, niezależny
   ↓
6. BFF-MVP-001 ← forward headery (wymaga: BE-MVP-004 gotowy)
7. BFF-MVP-002 ← forward If-None-Match (wymaga: BE-MVP-001)
   ↓
8. FE-MVP-001  ← ApiHeaders + useApiClient (wymaga: BFF-MVP-001)
9. FE-MVP-002  ← Zod schema extensions (niezależny)
   ↓
10. FE-MVP-003 ← ProblemDetailsCard (wymaga: FE-MVP-002, BE-MVP-003)
    ↓
11. BFF-MVP-003 ← Error Lab 429 (niezależny)
12. BFF-MVP-004 ← Error Lab 304 (wymaga: BE-MVP-001, BFF-MVP-002)
13. BFF-MVP-005 ← Error Lab replay (wymaga: BE-MVP-002, BFF-MVP-001)
14. BFF-MVP-006 ← Error Lab 401 WWW-Auth (wymaga: BFF-MVP-001)
15. BFF-MVP-007 ← Error Lab 428 (wymaga: BE-MVP-003)
    ↓
16. FE-MVP-004 ← error-lab.vue scenarios (wymaga: BFF-MVP-003/4/5)
17. FE-MVP-005 ← error-lab.vue headers (wymaga: FE-MVP-001, FE-MVP-004)
    ↓
18. FE-MVP-006 ← Merchant detail page (niezależny)
    ↓
19. SEED-MVP-001 ← Suspended merchant (wymaga: DB-MVP-001)
20. FE-MVP-007 ← Tenant badge (wymaga: DB-MVP-001, SEED-MVP-001)
21. FE-MVP-008 ← Lifecycle partial amounts (niezależny, na końcu)
```

### Sprint 2 (Phase 2 — tydzień 3-4)

```
1. FE-P2-001  ← Playwright upgrade (niezależny)
2. BE-P2-001  ← Last-Modified backend
3. BFF-P2-001 ← Last-Modified forward
4. BE-P2-002  ← retryable/retryAfterSeconds backend
5. FE-P2-003  ← ProblemDetailsCard retryable
6. FE-P2-002  ← Multi-role config
7. DB-P2-001  ← Audit export index
8. FE-P2-004  ← RateLimit przygotowanie
9. FE-P2-005  ← Support page
```

---

## 23. Dependency Graph

```
DB-MVP-001
│
├──→ SEED-MVP-001
│    └──→ FE-MVP-007
│
BE-MVP-001 ──→ BFF-MVP-002 ──→ BFF-MVP-004
BE-MVP-002 ──→ BFF-MVP-005
BE-MVP-003 ──→ BFF-MVP-007 ──→ FE-MVP-003
BE-MVP-004 ──→ BFF-MVP-001 ──→ FE-MVP-001 ──→ FE-MVP-004
                                              FE-MVP-005
FE-MVP-002 ──→ FE-MVP-003

BFF-MVP-003 (niezależny, mock)
BFF-MVP-006 (wymaga: BFF-MVP-001)

FE-MVP-006 (niezależny)
FE-MVP-008 (niezależny)

FE-P2-001 ──→ FE-P2-002
BE-P2-001 ──→ BFF-P2-001
```

---

## 24. Risks and Mitigations

### Ryzyko 1 — 304 i `Cache-Control: no-store` — semantyczna kolizja

**Opis:** `no-store` sugeruje "nie przechowuj w cache", ale `If-None-Match`/304 zakłada że klient MA ETag (czyli coś przechował).  
**Wpływ:** Potencjalne pytanie SDET-a: "dlaczego 304 jeśli no-store?".  
**Mitygacja:** Zachowaj `no-store` w 304 response dla spójności modelu edukacyjnego. Dodaj komentarz w kodzie i w dokumentacji Error Lab opisujący tę świadomą decyzję. Wartość edukacyjna: SDET rozumie, że ETag może origin klient trzymać w pamięci sesji bez cache storage.

### Ryzyko 2 — TenantResolver blokuje SUSPENDED tenant ale UI nie informuje dlaczego

**Opis:** Gdy użytkownik z SUSPENDED tenantem dostaje 403, UI może pokazać generyczny błąd zamiast czytelnej informacji.  
**Wpływ:** Zdezorientowany użytkownik.  
**Mitygacja:** FE-MVP-007 (tenant badge) i `ProblemDetailsCard` z `correlationId`. Sprawdź czy `TenantResolver` rzuca Problem Details z czytelnym `detail`. Jeśli nie — dodaj do backendu.

### Ryzyko 3 — Error Lab trigger-304 zawodzi gdy brak payment orders w seed

**Opis:** Trigger-304 wymaga istniejącego payment order. Jeśli seed jest pusty, trigger nie może wykonać GET.  
**Wpływ:** Trigger zwraca błąd zamiast 304.  
**Mitygacja:** Trigger powinien najpierw spróbować znaleźć/stworzyć payment order (analogia do trigger-428). Alternatywnie — trigger robi pełny flow: create → GET → GET z If-None-Match. Dokumentuj to w opisie scenariusza.

### Ryzyko 4 — `waitForTimeout(500)` w `payment-order-create.spec.ts:16` nie jest częścią tego planu

**Opis:** Plan nie tworzy testów. Istniejący anty-wzorzec pozostaje w pliku testowym.  
**Wpływ:** Flaky test w istniejącej suite.  
**Mitygacja:** Oznacz jako osobne zadanie do naprawy przez zespół testowy. Technicznie: zamień `waitForTimeout(500)` na `page.waitForResponse(url => ...)` lub `expect.poll()`.

### Ryzyko 5 — Tenant context badge — brak tenant name/status w user session

**Opis:** `user` session może nie zawierać `tenantName` ani `tenantStatus`. JWT zwykle ma `tenant_id` (UUID) ale nie name.  
**Wpływ:** FE-MVP-007 nie ma danych do wyświetlenia.  
**Mitygacja:** Opcja A: Dodaj `/api/tenant/context` endpoint (GET, public dla zalogowanych). Opcja B: Sesja Nuxt przy logowaniu lookupuje tenant i przechowuje `tenantName`. Opcja C: Tylko SUSPENDED check — sprawdź `/api/status` odpowiedź. **Zalecam opcję B** — przy `onSuccess` OIDC handler zapisz tenant info do session.

---

## 25. What Not To Implement

*(Pełna lista z uzasadnieniami)*

| Nie implementuj | Powód |
|----------------|-------|
| Testy jednostkowe, integracyjne, E2E | Plan to tylko fundament |
| Fixtures Playwright | Nie są zadaniami implementacyjnymi systemu |
| Page Object Implementation | Dopiero po implementacji UI |
| REST Assured test classes | Out of scope tego planu |
| `X-Tenant-ID` / `X-Merchant-ID` w response headers | Security risk: ujawnia internal mapping |
| Stack trace w Problem Details | Security risk |
| `RateLimit-*` headers bez backend rate limitera | Martwy kod, fałszywe zielone testy |
| Custom UI poza Nuxt UI Dashboard Template | Naruszenie design system |
| `traceparent` bez Otel | Wymaga dodatkowej infrastruktury |
| Moduł `ETagModule`, `IdempotencyModule` | Sztuczne moduły, naruszenie Modulith |
| Produkcyjny `X-HTTP-Method-Override` | Anti-pattern |
| `DELETE` z body jako wzorzec domyślny | RFC nie zaleca |
| `QUERY` HTTP method | Eksperymentalny, nie wspierany |
| Batch endpoint `POST /api/batch` | Out of scope |
| Cursor paginacja z `Link` header | Wymaga backend paginacji |
| Reconciliation/settlement UI | Bez backend foundation |
| Fake KPI dashboard | Bez realnych danych |
| Cache dla payment responses (poza no-store) | Security risk dla danych finansowych |
| `Surrogate-Control`, `Age` | Brak CDN layer |
| `Content-MD5` | Deprecated |
| `Pragma: no-cache` | HTTP/1.0 legacy |
| `expect.soft.poll()` | Nie istnieje w Playwright |
| `page.on('dialog')` dla `ConfirmActionModal` | Dom modal, nie native dialog |
| `Authorization` w `exposedHeaders` | Security risk: token leakage |

---

## 26. Final Recommendation

### Priorytety absolutne (blokujące testability)

1. **BE-MVP-004** (CORS expose headers) — bez tego żaden Playwright network assertion nie działa przez sieć
2. **FE-MVP-001** (ApiHeaders) — fundament dla wszystkich header assertions w UI
3. **FE-MVP-003** (ProblemDetailsCard extensions) — kluczowy element edukacyjny
4. **BFF-MVP-003** (Error Lab 429) — kompletuje Error Lab

### Priorytety drugorzędne (wysoka wartość edukacyjna)

5. **BE-MVP-001** (304 Not Modified) — wzorzec conditional GET
6. **BE-MVP-002** (Idempotency-Replayed) — unikalny wzorzec idempotency
7. **FE-MVP-006** (Merchant detail page) — brakujący element nawigacji
8. **DB-MVP-001** (SUSPENDED tenant) — odblokuje tenant isolation demo

### Priorytety trzeciorędne (completion)

9. **BFF-MVP-001** (header forward)
10. **BFF-MVP-004/005** (Error Lab 304/replay)
11. **FE-MVP-007** (tenant badge)
12. **FE-MVP-008** (partial capture/refund)

### Zlecenie Phase 2

Dopiero po zakończeniu wszystkich 21 zadań MVP, dobrze przetestowanych ręcznie:
- Upgrade Playwright 1.61 (FE-P2-001)
- Multi-role config (FE-P2-002)
- Last-Modified (BE-P2-001)

### Uwaga końcowa na temat bezpieczeństwa

**Niezmiennik bezpieczeństwa:** `Authorization` nigdy nie wraca jako response header, nigdy nie ląduje w DOM, nigdy nie jest w localStorage/sessionStorage. `HeaderKeyValuePanel.vue` maskuje go jako `Bearer ••••••••` — ta logika musi być zachowana we wszystkich nowych komponentach.

---

*Plan weryfikowany względem aktualnego kodu w repozytorium. Wszystkie zadania mają potwierdzony stan wyjściowy w kodzie i dokumentach. Nie ma zadań bez źródła.*
