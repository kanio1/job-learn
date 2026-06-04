---
type: lesson
status: ready
project: Payment Quality Engineering Lab
phase: 2
lesson: 9
area: Payment Orders
module: Payment Orders Frontend Consumer and Contract Alignment
date: 2026-05-31
tags:
  - lesson
  - lesson-09
  - payment-quality-lab
  - payment-order
  - nuxt
  - zod
  - pinia
  - playwright
  - rest-assured
  - keycloak
  - security-testing
  - senior-sdet
---

# Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment

> **Status:** READY - frontend consumer slice implemented and verified
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 09 is not a new payment lifecycle sprint. It turns existing backend list + summary APIs into a typed Nuxt Dashboard consumer with role-aware UI states and frontend/E2E test coverage.

## 1. Cel Lekcji

Lekcja 09 uczy, jak połączyć gotowy backend REST API z frontendowym dashboardem bez dopisywania fałszywego biznesu. Po Lessons 06-08 mamy create/read, list/filter i summary po stronie backendu, ale frontend obsługuje tylko create/read i używa zbyt luźnego `any` w store.

Celem jest zbudowanie małego, edukacyjnego panelu płatności dla istniejących danych: summary cards + lista payment orderów + role-aware empty/error states + testy Playwright/typecheck. REST Assured pozostaje backendowym kontraktem regresji, a frontend uczy się konsumować ten kontrakt.

## 2. Co Budujemy / Co Cwiczymy

Capability:

- Merchant-scoped payments panel w Nuxt Dashboard dla istniejących payment orders.
- Server proxy dla istniejących endpointów:
  - `GET /api/merchants/{merchantId}/payment-orders`
  - `GET /api/merchants/{merchantId}/payment-orders/summary`
- Zod schemas i TypeScript typy dla listy, pojedynczego ordera i summary.
- Typed Pinia store zamiast `any` dla `loading`, `error`, `list`, `summary`, `currentOrder`.
- UI states: loading, empty merchant, forbidden/insufficient authority, backend unavailable.
- Playwright tests dla renderowania summary/list i denied state.

Nie budujemy lifecycle actions, PSP, Kafka, refunds, settlement ani kompletnego dashboardu biznesowego.

## 3. Learning Delta Wzgledem Poprzednich Lekcji

| Temat | Status |
|---|---|
| Nuxt server proxy dla `GET` z query params | Nowy |
| Zod response schema jako frontendowy contract guard | Nowy |
| Typed Pinia store zamiast `any` | Nowy |
| UI jako consumer backend security: `401`, `403`, `503` | Rozszerzenie Lessons 06-08 |
| Consumer-driven contract thinking bez Pact | Nowy |
| Playwright route mocking dla dashboard states | Nowy |
| Role-aware UI feedback, backend nadal egzekwuje security | Rozszerzenie Lesson 06 |
| REST Assured summary/list tests jako backend regression safety net | Uzywamy, nie powtarzamy od zera |
| `GROUP BY`, `COUNT`, `SUM` | Prerequisite z Lesson 08 |
| Idempotency create flow | Prerequisite z Lesson 06 |

## 4. Mapa Kodu

Zaimplementowane pliki frontendowe:

| Plik | Po co istnieje |
|---|---|
| `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts` | Nuxt server proxy do backend list endpoint z query params i access token forwarding |
| `apps/frontend/server/api/merchants/[merchantId]/payment-orders/summary.get.ts` | Nuxt server proxy do backend summary endpoint |
| `apps/frontend/app/schemas/payment-order.schema.ts` | Zod schemas i typy dla create, read, list, summary |
| `apps/frontend/app/stores/payment-orders.ts` | Typed state i actions dla list/summary/read |
| `apps/frontend/app/components/payment/PaymentOrderSummaryCards.vue` | Operacyjne summary cards bez fake KPI |
| `apps/frontend/app/components/payment/PaymentOrderListTable.vue` | Lista orderow z podstawowymi filtrami i linkiem do detail |
| `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` | Merchant-scoped payments panel |
| `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` | Playwright coverage dla summary, list, empty i forbidden states |

Implementation status: all files above are now implemented. The merchant registry route was moved from `apps/frontend/app/pages/admin/merchants.vue` to `apps/frontend/app/pages/admin/merchants/index.vue` so nested merchant payment panel routes resolve correctly.

Istniejacy backend do regresji:

| Plik | Rola w Lesson 09 |
|---|---|
| `PaymentOrderListRestAssuredTest.java` | Chroni list contract konsumowany przez UI |
| `PaymentOrderSummaryRestAssuredTest.java` | Chroni summary contract konsumowany przez UI |
| `PaymentOrderSummaryBusinessFlowRestAssuredTest.java` | Chroni agregacyjny oracle |
| `PaymentOrderSummarySecurityTest.java` | Chroni 401/403/ownership matrix |
| `PaymentModuleTest.java` | Chroni Spring Modulith boundary |

## 5. Architecture Walkthrough

Module owner backendu nie zmienia sie: `payment` pozostaje wlascicielem payment orders, listy i summary. Frontend nie omija backendu i nie replikuje reguł autoryzacji.

Decyzje:

- Nuxt server routes sa BFF/proxy: przekazuja access token z sesji do backendu.
- Backend pozostaje zrodlem prawdy dla ról i `merchant_id` ownership.
- UI moze ukryc akcje lub pokazac komunikat, ale nie jest security boundary.
- Zod sluzy do walidacji ksztaltu odpowiedzi po stronie frontendowej, nie do zmiany kontraktu backendu.
- No fake completeness: nie pokazujemy przyciskow `Authorize`, `Capture`, `Cancel`, bo lifecycle nie istnieje.
- No fake metrics: summary pokazuje tylko `totalOrders`, `totalAmountMinor`, breakdown by currency/status.

Spec Kit decision:

- Full Spec Kit nie jest wymagany, bo Lesson 09 nie dodaje nowego backend resource lifecycle ani nowego modelu bezpieczeństwa.
- To jest lesson extension / frontend consumer slice na istniejacym API.

## 6. HTTP I REST API

Frontend konsumuje istniejace backend endpoints:

```http
GET /api/merchants/{merchantId}/payment-orders?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <token>
Accept: application/json
```

```http
GET /api/merchants/{merchantId}/payment-orders/summary
Authorization: Bearer <token>
Accept: application/json
```

UI-relevant status codes:

| Backend status | Frontend behavior |
|---|---|
| `200` | Render list/summary |
| `401` | Session/token problem, redirect/login-safe message |
| `403` | Insufficient authority panel, no payment data rendered |
| `400` | Filter validation message |
| `503` / network error | Backend unavailable state |

Headers:

- Backend nadal zwraca `X-Correlation-ID`.
- Frontend proxy powinien nie logowac `Authorization`.
- Summary nie ma `ETag`; detail/create nadal moga miec `ETag` z poprzednich lekcji.

## 7. Java 25 I Backend Reading

W Lesson 09 nie dodajemy backend behavior. Czytamy backend tylko po to, zeby frontend nie zgadywal kontraktu:

1. `PaymentOrderListResponse` - shape listy i pagination metadata.
2. `PaymentOrderSummaryResponse` - shape summary cards.
3. `PaymentExceptionHandler` - stabilne `error` i `message`.
4. `SecurityConfig` - ktore role backend faktycznie honoruje.
5. `TestJwtSupport` - jak testy modeluja role i `merchant_id`.

Pytanie QA:

- Czy UI testuje komunikat dla `403`, ale nie zaklada, ze ukrycie elementu w UI wystarcza jako security?

## 8. SQL, PostgreSQL I Flyway

Brak nowych migracji w Lesson 09.

SQL pozostaje po stronie backendu z Lesson 07-08:

- list: `WHERE`, `ORDER BY`, `LIMIT/OFFSET`, count dla paginacji,
- summary: `COUNT(*)`, `SUM(amount_minor)`, `GROUP BY currency/status`.

Cwiczenie edukacyjne:

- Porownaj payload summary z UI z oczekiwanym seed dataset z `PaymentOrderSummaryApiTestSupport`.
- Wytlumacz, dlaczego UI nie powinien liczyc summary przez pobranie calej listy i sumowanie w przegladarce.

## 9. Security I Tenant Isolation

Security matrix dla UI consumer:

| Actor | Backend result | UI behavior |
|---|---|---|
| Unauthenticated | `401` | Login/session-required state |
| Denied identity | `403` | Insufficient authority, no data |
| `merchant:payments:create` only | `403` | Insufficient authority, no data |
| `merchant:payments:operate` only | `403` | Insufficient authority, no data |
| `merchant:payments:read` + matching `merchant_id` | `200` | Summary + list for own merchant |
| `merchant:payments:read` + mismatched `merchant_id` | `403` | Insufficient authority, no data |
| `platform:payments:read` | `200` | Summary + list for selected merchant |

Keycloak learning focus:

- Understand public PKCE client/session enough to know where access token is stored and forwarded.
- Do not build complete OAuth/OIDC application integration beyond the existing project pattern.
- Do not create new roles for UI-only convenience.

## 10. REST Assured Learning Path

Lesson 09 nie dodaje nowych backend endpoints, wiec REST Assured jest regresyjnym guardrailem:

| Command/Test | Co chroni |
|---|---|
| `PaymentOrderListRestAssuredTest` | List shape, filters, pagination, correlation header |
| `PaymentOrderSummaryRestAssuredTest` | Summary shape, empty state, filters, validation, no `ETag` |
| `PaymentOrderSummaryBusinessFlowRestAssuredTest` | Controlled seed data aggregate oracle |
| `PaymentOrderSummarySecurityTest` | 401/403/own/platform matrix |
| `PaymentOrderSecurityTest` | Existing create/read ownership behavior |

Nowy test backendowy dodaj tylko wtedy, gdy frontend ujawni brak kontraktu, ktorego nie pokrywa obecna suite.

## 11. Assertion Strategy

| Ryzyko | Najlepszy oracle |
|---|---|
| Backend summary liczy zle | REST Assured + AssertJ + controlled seed data |
| UI mapuje response zle | Playwright component/page assertions lub route-mocked E2E |
| Zod schema nie pasuje do backendu | Typecheck + test proxy/fixture payloadu |
| Brak uprawnien pokazuje dane | Playwright denied-state test + backend security tests |
| Query params zgubione w proxy | Playwright/network assertion lub unit/server route test |

Nie duplikuj wszystkich backend contract assertions w Playwright. Frontend testuje decyzje UI i integracje z proxy, backend testuje prawde API.

## 12. Test Data Ownership

Backend REST Assured:

- per-test merchant,
- per-test idempotency key,
- deterministic seed dataset,
- no shared mutable fixtures.

Frontend Playwright:

- prefer route-mocked responses dla UI state coverage,
- fixture names powinny zawierac test/worker-safe prefix, jesli test idzie przez realny backend,
- nie polegaj na kolejnosci testow,
- denied-state tests nie moga zalezec od danych z happy-path tests.

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Dlaczego Lesson 09 nie powinna dodawac `authorize/capture/cancel`?
2. Dlaczego summary nie powinno byc liczone w przegladarce z pelnej listy orderow?
3. Co daje Zod schema dla response, skoro backend ma juz DTO record?
4. Czym rozni sie backend security test od frontend denied-state test?
5. Kiedy Playwright powinien mockowac API, a kiedy uderzac w realny backend?
6. Jak frontend powinien pokazac `403`, zeby nie sugerowac, ze merchant nie istnieje?
7. Dlaczego `platform:payments:read` moze ogladac selected merchant summary?
8. Jakie ryzyko tworzy store z `any` dla payment order response?
9. Ktore header/body/status assertions pozostaja odpowiedzialnoscia REST Assured?
10. Jak zachowac parallel safety w testach UI i REST jednoczesnie?

### Odpowiedzi

1. Lesson 09 jest frontend consumer slice dla istniejących kontraktów create/read/list/summary. `authorize/capture/cancel` byłyby nową logiką lifecycle, która nie jest jeszcze wyspecyfikowana.
2. Summary liczone w przeglądarce wymagałoby pobrania pełnej listy orderów i mogłoby ominąć backendowe filtry/security. Backend powinien być źródłem prawdy dla agregacji.
3. Zod waliduje runtime JSON na granicy frontendu, a DTO record chroni tylko backend. Dzięki temu frontend wykrywa contract drift zamiast ufać `unknown`/`any`.
4. Backend security test dowodzi egzekwowania autoryzacji. Frontend denied-state test dowodzi, że UI nie pokazuje danych i komunikuje odmowę w czytelny sposób.
5. Playwright mockuje API, gdy testujesz tylko stany UI i chcesz stabilności. Realny backend wybierz dla smoke/integration flow, gdzie ważne jest połączenie frontend-proxy-backend.
6. UI powinno pokazać forbidden/insufficient authority, nie empty merchant. `403` oznacza brak prawa dostępu, a nie brak merchanta.
7. `platform:payments:read` jest rolą cross-merchant read dla payment resources. Platform reader może wybrać merchanta, ale nadal tylko czyta istniejący summary endpoint.
8. `any` ukrywa błędy shape response i pozwala UI odwołać się do nieistniejących pól. Ryzykiem jest cichy runtime bug zamiast błędu typecheck/schema validation.
9. REST Assured nadal odpowiada za status codes, headers, error body i dokładny API contract. Playwright powinien sprawdzać zachowanie UI, a nie powielać pełną macierz HTTP.

```java
then()
    .statusCode(200)
    .header("Content-Type", containsString("application/json"))
    .body("totalOrders", equalTo(3));
```

10. Każdy test UI i REST musi mieć własne dane lub mocki niezależne od innych testów. Unikaj shared merchantów, stałych idempotency keys i zależności od kolejności wykonania.

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Dodaj Zod response schemas dla list/summary | `payment-order.schema.ts` | `corepack pnpm typecheck` | Types compile |
| Dodaj Nuxt proxy dla listy i summary | `server/api/merchants/...` | `corepack pnpm typecheck` | No TS errors |
| Zmien store z `any` na typy domenowe | `payment-orders.ts` | `corepack pnpm typecheck` | No `any` for list/summary/current order |
| Dodaj payments panel | `pages/admin/merchants/[merchantId]/payments/index.vue` | `corepack pnpm typecheck` | Summary + list states render |
| Dodaj Playwright happy/empty/403 tests | `tests/e2e/payment-orders-panel.spec.ts` | `corepack pnpm test:e2e` | UI states covered |
| Uruchom backend regression guardrails | backend tests | `./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` | REST contract green |

### Rozwiązania / wskazówki

1. Zod schemas powinny opisywać dokładnie to, co frontend renderuje: list item, summary groups i backend error. Nie dodawaj pól przyszłego lifecycle, jeśli backend ich nie zwraca.
2. Nuxt proxy powinien przekazać access token i query params bez logowania sekretów. Proxy nie powinien zmieniać semantyki statusów backendu.
3. Store bez `any` powinien mieć typowane `list`, `summary`, `currentOrder`, `loading`, `error` i `forbidden`. Typecheck ma złapać błędne pola zanim trafią do UI.
4. Payments panel powinien pokazywać loading, empty, forbidden i data state. Nie dodawaj przycisków `authorize/capture/cancel`, bo to sugeruje nieistniejące funkcje.
5. Playwright powinien asercjonować teksty/stany widoczne dla użytkownika, np. summary cards, pustą listę i forbidden message. Nie musi sprawdzać wszystkich headerów HTTP.

```ts
await expect(page.getByText('Insufficient authority')).toBeVisible()
await expect(page.getByRole('table')).not.toBeVisible()
```

6. Backend regression guardrails potwierdzają, że frontend nie wymusił zmiany kontraktu API. Jeśli frontend test przechodzi, ale REST Assured failuje, źródłem prawdy jest backend contract test.

## 15. Mini Interview Prep

**Q: Why did you add a frontend consumer slice instead of a new payment lifecycle action?**

A: Because the backend already had create/read/list/summary behavior, but the frontend only consumed create/read and used loose types. Lesson 09 closes a real system gap without inventing unsupported lifecycle behavior. It makes the UI a typed consumer of existing contracts while backend REST Assured tests remain the source of API truth.

**Q: What is the difference between backend authorization tests and frontend authorization UI tests?**

A: Backend tests prove enforcement: roles, ownership and status codes. Frontend tests prove user experience: forbidden states do not render sensitive data and explain why the page cannot be used. UI tests never replace backend security tests.

**Q: Why use Zod for response schemas?**

A: Zod gives the frontend an explicit runtime boundary for data received through the Nuxt server proxy. It catches consumer drift and documents the shape the UI depends on, while TypeScript types alone cannot validate runtime JSON.

## 16. Verification Commands

Implemented evidence:

| Area | Evidence |
|---|---|
| Zod contract guard | `apps/frontend/app/schemas/payment-order.schema.ts` now defines response, list, summary and backend error schemas with inferred TypeScript types. |
| Nuxt BFF/proxy | `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts` and `summary.get.ts` forward query params and backend access token. |
| Typed Pinia state | `apps/frontend/app/stores/payment-orders.ts` manages typed list, summary, current order, last created order, loading/error/forbidden state, `loadList`, `loadSummary`, `reset` and `clearError`. |
| UI components | `PaymentOrderSummaryCards.vue` and `PaymentOrderListTable.vue` render existing backend data without fake lifecycle actions or fake KPIs. |
| Payments panel | `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` renders loading, empty, backend error and `403` insufficient-authority states. |
| Route fix | `apps/frontend/app/pages/admin/merchants/index.vue` replaces the flat `merchants.vue` route to avoid intercepting nested payment panel URLs. |
| Create form typing | `CreatePaymentOrderForm.vue` parses create responses with `paymentOrderResponseSchema` before updating store state. |
| Playwright coverage | `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` covers summary/list rendering, empty merchant state, forbidden no-data state and backend-unavailable state. |

Command results captured during implementation:

| Command | Result |
|---|---|
| `cd apps/backend && ./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` | Passed, 20 tests. |
| `cd apps/backend && ./mvnw -DskipTests package` | Passed. |
| `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test` | Passed, 2 tests. |
| `cd apps/backend && ./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` | Command completed without failure report; output was truncated by tool capture. |
| `cd apps/frontend && corepack pnpm typecheck` | Passed. |
| `cd apps/frontend && corepack pnpm test:e2e -- payment-orders-panel.spec.ts` | Passed, 4 tests. |

Backend:

```bash
cd apps/backend
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
```

Frontend:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [x] Wyjasnic roznice miedzy backend API contract a frontend consumer contract.
- [x] Dodac Nuxt server proxy, ktory bezpiecznie przekazuje access token i query params.
- [x] Zastapic `any` typed Zod/TypeScript models dla response.
- [x] Zaprojektowac UI empty/loading/forbidden states bez fake business features.
- [x] Uzasadnic, ktore asercje naleza do REST Assured, a ktore do Playwright.
- [x] Utrzymac parallel-safe test data strategy dla backendu i frontendu.

## 18. Powiazane Notatki W Vault

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Payment Aggregation Summary]]
- [[Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [[Lesson Evidence Tracker]]
- [[Learning Coverage Backlog]]
- [[Current Sprint]]
