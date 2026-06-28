# Payment Quality Engineering Lab — Playwright/SDET Feature Roadmap

> Analiza architektoniczna i roadmapa funkcjonalna. Data: 2026-06-26.
> Nie implementuje kodu. Nie generuje testów Playwright. Opisuje kierunek, funkcje i plan nauki.
> Źródła: inspekcja repo + Context7 Playwright docs + lokalne pliki spec/steering.

---

## 1. Executive Summary

Repozytorium to dojrzały, wielowarstwowy backoffice płatniczy ze zrealizowanymi specami:
`iam-roles-and-keycloak-login`, `tenant-model-and-isolation`, `user-management`,
`audit-log-dashboard`, `deterministic-seed-and-test-isolation`. Backend jest solidnie
przetestowany (REST Assured, macierze bezpieczeństwa, modulith, property tests). Frontend
posiada Error Lab, HTTP-learning panels, Zod validation, i pełne proxy serwerowe.

**Krytyczna korekta wersji:** repozytorium używa **Playwright 1.60.0**, nie 1.61.
Przed planowaniem lekcji z nowymi API 1.61 (`Page.localStorage`, `Page.sessionStorage`)
wymagana jest aktualizacja. Roadmapa wskazuje, co zyska uaktualniona wersja.

**Największe luki z perspektywy SDET:**

1. Playwright używany tylko z jedną rolą, z `fullyParallel: false`, bez POM, bez fixtures.
2. Brak wielorolowego auth setup (5 ról = 5 storage states).
3. Brak APIRequestContext w testach (setup via API, nie przez UI).
4. Brak Network/Response assertions (ETag, X-Correlation-ID, Vary w UI).
5. Brak dialog/modal assertions (ConfirmActionModal istnieje, nie jest testowany E2E).
6. Brak testów file download/upload, iframe, multi-tab, clock.
7. `page.waitForTimeout()` pojawia się w kodzie — anti-pattern do usunięcia.

**Rekomendacja:** nie budować sztucznego frameworka testowego. Rozszerzyć **system domenowy**
tak, aby naturalnie wymuszał naukę zaawansowanego Playwright. Priorytet: `payment-operations-dashboard`
(już specowany) → multi-role auth setup → export CSV/download → PSP simulator (iframe) → advanced
lifecycle flows → risk queue → support search.

---

## 2. Aktualny stan repo

### 2.1 Stack technologiczny (zweryfikowany 2026-06-26)

| Warstwa | Technologia | Wersja | Plik |
|---|---|---|---|
| Backend | Java | 25 | `pom.xml` |
| Framework | Spring Boot | 4.0.6 | `pom.xml` |
| Modulith | Spring Modulith | 2.0.6 | `pom.xml` |
| Build | Maven Wrapper | 3.9.11 | `pom.xml` |
| Baza | PostgreSQL | 18 (Flyway) | `compose.yml` |
| Security | Spring Security JWT + Keycloak | — | `SecurityConfig.java` |
| IAM | Keycloak | 26.6.1 | `compose.yml` |
| Testy backend | JUnit 6.0.3 + REST Assured 6.0.0 + jqwik 1.9.2 | — | `pom.xml` |
| Testcontainers | TC core 2.0.5 (Podman) | — | `pom.xml` |
| WireMock | 3.13.2 | — | `pom.xml` |
| Frontend | Nuxt | 4.4.6 | `package.json` |
| UI | @nuxt/ui | 4.7.1 | `package.json` |
| TypeScript | 6.0.3 | — | `package.json` |
| State | Pinia 3.0.4 + Zod 4.4.3 | — | `package.json` |
| E2E | **Playwright 1.60.0** ← nie 1.61 | — | `package.json` |
| Unit/Property | Vitest 3 + fast-check 3.22 | — | `package.json` |

### 2.2 Backend — zrealizowane moduły i API

**Spring Modulith modules (produkcja):**

| Moduł | Typ | PUBLIC API | Kluczowe klasy |
|---|---|---|---|
| `shared` | OPEN | n/a | SecurityConfig, Authorities, CorrelationIdFilter, GlobalExceptionHandler |
| `foundation` | standalone | — | StatusController |
| `tenant` | standalone | TenantResolver, TenantContext, TenantReference | TenantResolverService, JpaTenantRepository |
| `merchant` | depends→tenant public | MerchantPaymentEligibility | MerchantController, MerchantService |
| `payment` | depends→merchant public | — | PaymentOrderController, PaymentOrderService |
| `iam` | standalone | — | UserManagementController, UserManagementService |
| `audit` | standalone (event-driven) | AuditableActionOccurred | AuditController, AuditListener |
| `testing` | seed-gated | SeedCapability per moduł | TestController, DeterministicDataset |

**REST API (pełna lista):**

```
GET   /api/status                                                           (public)
POST  /api/merchants                                                        PLATFORM_MERCHANTS_CREATE
GET   /api/merchants                                                        PLATFORM_MERCHANTS_READ
GET   /api/merchants/{id}                                                   PLATFORM_MERCHANTS_READ
POST  /api/merchants/{id}/activate                                          PLATFORM_MERCHANTS_UPDATE_STATUS
POST  /api/merchants/{id}/suspend                                           PLATFORM_MERCHANTS_UPDATE_STATUS
POST  /api/merchants/{merchantId}/payment-orders                            MERCHANT_PAYMENTS_CREATE + Idempotency-Key
GET   /api/merchants/{merchantId}/payment-orders                            MERCHANT_PAYMENTS_READ (paginated, filtered)
GET   /api/merchants/{merchantId}/payment-orders/{paymentOrderId}           MERCHANT_PAYMENTS_READ → ETag
HEAD  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}           MERCHANT_PAYMENTS_READ
PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}           MERCHANT_PAYMENTS_LIFECYCLE + If-Match
GET   /api/merchants/{merchantId}/payment-orders/summary                    MERCHANT_PAYMENTS_READ
GET   /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history   MERCHANT_PAYMENTS_READ
POST  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize MERCHANT_PAYMENTS_LIFECYCLE + If-Match + Idempotency-Key
POST  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture   MERCHANT_PAYMENTS_LIFECYCLE + If-Match + Idempotency-Key
POST  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel    MERCHANT_PAYMENTS_LIFECYCLE + If-Match + Idempotency-Key
POST  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund    MERCHANT_PAYMENTS_LIFECYCLE + If-Match + Idempotency-Key
GET   /api/audit                                                            PLATFORM_AUDIT_READ / TENANT_AUDIT_READ
GET   /api/audit/{id}                                                       PLATFORM_AUDIT_READ / TENANT_AUDIT_READ
GET   /api/users                                                            canManageUsers
POST  /api/users                                                            canManageUsers
GET   /api/users/{id}                                                       canManageUsers
PATCH /api/users/{id}                                                       canManageUsers
POST  /api/users/{id}/roles                                                 canAssignRoles
POST  /api/test/reset                                                       (testing flag = true only)
POST  /api/test/seed                                                        (testing flag = true only)
```

**Keycloak role → authorities mapping (5 composite roles):**

| Realm role | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY_USER |
|---|:---:|:---:|:---:|:---:|:---:|
| platform:merchants:create | ✓ | | | | |
| platform:merchants:read | ✓ | ✓ | | ✓ | ✓ |
| platform:merchants:update-status | ✓ | | | | |
| merchant:payments:create | ✓ | ✓ | ✓ | | |
| merchant:payments:read | ✓ | ✓ | ✓ | ✓ | ✓ |
| merchant:payments:lifecycle | ✓ | ✓ | ✓ | | |
| platform:payments:read | ✓ | | | ✓ | |
| platform:audit:read | ✓ | | | ✓ | |
| tenant:audit:read | | ✓ | | | |
| canManageUsers | ✓ | ✓ | | | |
| canAssignRoles | ✓ | ✓ | | | |

**Flyway schema (9 migrations):**
- `V0.1__create_tenants.sql` — tenants table
- `V1__create_merchants.sql` — merchants table
- `V1.1__add_tenant_to_merchants.sql` — merchants.tenant_id FK
- `V2__create_payment_orders.sql` — payment_orders table
- `V3__add_payment_order_list_indexes.sql` — query indexes
- `V4__add_payment_lifecycle.sql` — lifecycle columns
- `V5__harden_payment_http_contract.sql` — ETag/idempotency columns
- `V6__create_event_publication.sql` — Spring Modulith events (shared)
- `V7__create_audit_event.sql` — audit_event table

**Deterministic seed dataset (Wave 1R–3R ukończone):**
- Tenants: PLATFORM_TENANT (`...a1`), TENANT_ALPHA (`...a2`), PLACEHOLDER_TENANT_ID (`...a3`)
- Merchants: MERCHANT_ALPHA_001 (`...b1`), MERCHANT_ALPHA_002 (`...b2`), MERCHANT_BETA_001 (`...b3`)
- Payment orders: 6 base (`...c1..c6`) + 98-order pagination block (`c101..c198`)
- Endpoints: `POST /api/test/reset`, `POST /api/test/seed` (flag: `app.testing.enabled=true`)

### 2.3 Frontend — zrealizowane components i routes

**Pages:**
```
/                                          → redirect → /admin/merchants
/login                                     → Keycloak PKCE flow
/forbidden                                 → 403 page
/error-lab                                 → Error Lab (9 triggers: 400/401/403/404/406/409/412/415/428)
/admin/merchants                           → MerchantTable + CreateMerchantForm
/admin/merchants/[merchantId]/payments     → PaymentOrderListTable + filters + pagination
/admin/merchants/[merchantId]/payments/new → CreatePaymentOrderForm
/admin/merchants/[merchantId]/payments/[paymentOrderId] → PaymentOrderDetail + lifecycle
/admin/users                               → UserTable + CRUD (PLATFORM_ADMIN / TENANT_ADMIN)
/admin/audit                               → AuditTable + AuditFilters + AuditEntryDrawer
```

**Shared components (reusable):**
ApiDebugPanel, BusinessStatusBadge, ConfirmActionModal, EmptyStateCard, ErrorState,
EtagDisplay, HeaderKeyValuePanel, HttpStatusBadge, IdempotencyKeyInput, IfMatchInput,
LoadingState, MerchantStatusCard, PaymentOrderLifecycleActions, ProblemDetailsCard, RawJsonViewer

**Server proxy routes:** Pełne pokrycie wszystkich endpointów backendowych + 9 error-lab triggers.

**Composables:** useApiClient (nagłówki, Zod, problem+json), useMerchantsApi, usePaymentOrdersApi,
usePaymentLifecycleApi, useUsersApi, useAuditApi, useAuthorization, useAuthError.

**Schemas (Zod):** merchant, payment-order, audit, user, problem-details, app-shell.

### 2.4 Playwright — aktualny stan (1.60.0)

**Konfiguracja (`playwright.config.ts`):**
```ts
fullyParallel: false          // ← brak paralelizmu
workers: CI ? 2 : undefined
retries: CI ? 1 : 0
trace: 'on-first-retry'       // ← trace tylko przy retry, brak video
projects: [
  { name: 'auth-setup', testMatch: /auth\/.*\.setup\.ts/ },
  { name: 'chromium', dependencies: ['auth-setup'],
    storageState: 'tests/.auth/platform-operator.json' }  // ← 1 rola
]
```

**Istniejące testy E2E (`tests/e2e/`):**

| Plik | Techniki | Pokrycie |
|---|---|---|
| `auth-deny.spec.ts` | route mock, getByRole | auth redirect |
| `foundation.spec.ts` | page.goto, expect | /api/status |
| `merchant-create.spec.ts` | route mock, fill, click | create form |
| `merchant-lifecycle.spec.ts` | route mock, getByTestId, getByRole | activate/suspend |
| `merchant-feedback.spec.ts` | route mock | loading/empty/error states |
| `payment-order-create.spec.ts` | route mock, getByLabel, getByText | form validation, idempotency |
| `payment-order-read.spec.ts` | route mock | detail page |
| `payment-orders-panel.spec.ts` | route mock | list/empty state |
| `payment-order-auth-deny.spec.ts` | route mock | 403 guard |

**Istniejące testy Vitest/property (`tests/unit/`):** 13 plików, 498 testów.

**Luki Playwright:**

| Kategoria | Luka |
|---|---|
| Role | 1 rola (platform-operator z pustym storage state), brak 5 real-auth storage states |
| Fixtures | Brak custom fixtures, brak worker fixtures |
| POM | Brak Page Objects, Component Objects, App Object |
| APIRequestContext | Nie używany (brak API-driven setup/cleanup) |
| Network | Brak response header assertions w UI (ETag, Vary, X-Correlation-ID) |
| Dialog | ConfirmActionModal istnieje, brak E2E testów dialogów |
| Downloads | Brak testów file download |
| Uploads | Brak testów file upload |
| Multi-tab | Brak testów nowych kart/okien |
| Iframe | Brak frameLocator |
| Clock | Brak time mocking |
| ARIA | Brak ARIA snapshot assertions |
| Visual | Brak screenshot/visual comparison |
| a11y | Brak accessibility assertions |
| Parallel | fullyParallel: false, brak sharding |
| HAR | Brak HAR recording/replay |
| Trace | Skonfigurowane `on-first-retry`, brak jawnych testów z trace |
| Anti-pattern | `waitForTimeout(500)` w payment-order-create.spec.ts |

---

## 3. Docelowa wizja systemu

**Nazwa:** Payment Quality Engineering Lab — Multi-Tenant Payment Operations Console

System symuluje realistyczny backoffice operatora płatniczego (PayU-like). Zawiera:
- **5 ról** z odrębnym UI i uprawnieniami
- **3 warstwy tenantów** (platforma, tenant, merchant)
- **Pełny lifecycle płatności** z concurrency, idempotency, audit trail
- **HTTP Learning Surface** (Error Lab, header panels, raw JSON, problem+json)
- **PSP Simulator** (iframe-sandboxed, bez PAN/PCI)
- **Risk/fraud simulation** (velocity rules, review queue)
- **Support tools** (cross-merchant search, export, download, upload evidence)
- **Notification system** (polling lub SSE-like status updates)

Użytkownicy testowi (z realm Keycloak):
- `platform.admin` → PLATFORM_ADMIN → pełny dostęp + IAM
- `tenant.admin` → TENANT_ADMIN → tenant-scoped merchants + users + audit
- `merchant.manager` → MERCHANT_MANAGER → payment orders lifecycle (MERCHANT_ALPHA_001)
- `support.agent` → SUPPORT_AGENT → read-only + cross-merchant + audit
- `readonly.user` → READ_ONLY_USER → tylko odczyt

---

## 4. Zasady projektowania funkcji edukacyjnych

1. **Funkcja musi istnieć w realnym systemie PayU-like.** Nie dodajemy paneli tylko po to, żeby kliknąć locator.
2. **Funkcja musi naturalnie wymuszać technikę Playwright.** Uzasadnienie: „To ćwiczy `frameLocator`", „To ćwiczy `dialog`", „To ćwiczy `expect.poll`".
3. **Funkcja musi umożliwiać test UI + test REST.** Minimum: 1 scenariusz Playwright + 1 scenariusz REST Assured.
4. **Funkcja nie może wymagać PSP produkcyjnego.** Symulacje, stany, placeholdery zamiast realnych transakcji.
5. **Nie budować sztucznego frameworka.** Testy rosną naturalnie razem z funkcjami systemu.
6. **Dane muszą być deterministyczne.** Każda funkcja opiera się na seeded dataset lub API-driven setup.
7. **Nie udostępniać tokenów w UI.** Authorization header zawsze zamaskowany.
8. **Nie dodawać Kafki/settlement/KYC/3DS** bez świadomej decyzji.

---

## 5. Minimum 30 proponowanych funkcji (z mapowaniem SDET)

### Obszar A — Multi-tenant foundation (5 funkcji)

---

**F-01: Tenant Context Indicator**

| | |
|---|---|
| Opis biznesowy | Widoczny wskaźnik aktywnego tenanta w dashboardzie (breadcrumb lub header badge). |
| Realizm PSP | Platform operator widzi „działam jako TENANT_ALPHA" — standard w multi-tenant backofficach. |
| Wartość SDET | Najszczegławsza weryfikacja izolacji: po zmianie tenanta inne dane. |
| Testy UI | Asercja na badge/breadcrumb; zmiana tenanta zmienia listę merchantów. |
| Testy REST | Header Tenant-Context w response; brak danych cross-tenant. |
| Playwright capabilities | `getByRole`, URL params, `page.waitForURL`, multi-project fixtures z różnymi tenantami. |
| REST Assured | Tenant isolation assertions już istniejące w `TenantIsolationIT`. |
| Role potrzebne | PLATFORM_ADMIN (wszystkie tenants), TENANT_ADMIN (swój tenant), SUPPORT_AGENT (read-only). |
| Backend endpointy | Brak nowych (filtrowanie po tenant_id w JWT). |
| Frontend | Header/breadcrumb component w `dashboard.vue`. |
| Flyway | Brak nowych migracji. |
| Poziom | medium |
| MVP | TAK |

---

**F-02: Tenant Suspension Evidence Page**

| | |
|---|---|
| Opis | Dedykowana strona/banner informujący, że tenant jest SUSPENDED (wszystkie operacje zablokowane). |
| Realizm | W PayU/PSP zawieszony tenant blokuje wszystkich merchantów pod nim. |
| Wartość SDET | Ćwiczy RBAC error states: zablokowany kontekst widoczny w UI. |
| Testy UI | Nawigacja do strony merchantów → banner SUSPENDED, brak przycisków create/activate. |
| Testy REST | `TenantIsolationIT` już pokrywa 403. Brakuje UI assertion. |
| Playwright capabilities | `toBeDisabled()`, `not.toBeVisible()`, conditional guards. |
| REST Assured | Brak nowych; istniejące testy. |
| Backend endpointy | Brak nowych. |
| Frontend | Tenant status guard w middleware lub dashboard layout. |
| Flyway | Brak. |
| Poziom | medium |
| MVP | TAK |

---

**F-03: Tenant Settings Page**

| | |
|---|---|
| Opis | Strona ustawień tenanta (displayName, contactEmail, webhookBaseUrl, timezone). |
| Realizm | Każdy tenant w PSP konfiguruje własne callbacki i ustawienia regionalne. |
| Wartość SDET | Form z walidacją + PATCH endpoint + ETag/If-Match — ćwiczy pełny cykl optimistic locking. |
| Testy UI | Fill, submit, etag display, stale 412, missing 428. |
| Testy REST | PATCH `/api/tenants/{id}/settings` z ETag flow. |
| Playwright capabilities | `getByLabel`, `fill`, `expect(page.getByTestId('etag-display'))`, dialog dla unsaved changes guard. |
| REST Assured | ETag / If-Match pattern (jak w payment orders). |
| Backend endpointy | PATCH `/api/tenants/{id}/settings` (nowy), GET `/api/tenants/{id}`. |
| Frontend | `/admin/tenant/settings` page + TenantSettingsForm. |
| Flyway | Kolumny: contact_email, webhook_base_url, timezone w `tenants`. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-04: Cross-Tenant Negative Tests Demo**

| | |
|---|---|
| Opis | Error Lab rozszerzony o przyciski: „Read Cross-Tenant" → masked 404, „Write Cross-Tenant" → 403. |
| Realizm | Izolacja tenantów — critical w każdym multi-tenant SaaS. |
| Wartość SDET | Ćwiczy negative path UI: widzimy 403/404 w ProblemDetailsCard. |
| Testy UI | Click trigger → HttpStatusBadge = 403 → ProblemDetailsCard visible. |
| Testy REST | Już w `TenantIsolationIT`; UI test jest brakującym pokryciem. |
| Playwright capabilities | `getByTestId`, text assertion on status badge. |
| REST Assured | Istniejące. |
| Backend endpointy | Brak nowych (Error Lab wywołuje istniejące endpointy z celowo złym tenant context). |
| Frontend | 2 nowe triggery w `/error-lab`. |
| Flyway | Brak. |
| Poziom | medium |
| MVP | TAK |

---

**F-05: Tenant-Scoped Audit Boundary Demo**

| | |
|---|---|
| Opis | TENANT_ADMIN widzi tylko audit logi swojego tenanta; PLATFORM_ADMIN widzi wszystkie. |
| Realizm | Compliance: tenant nie powinien widzieć zdarzeń innych tenantów. |
| Wartość SDET | Ćwiczy multi-role fixtures: ten sam URL, inne dane zależnie od roli. |
| Testy UI | Playwright test z `tenantAdminPage` (fixture) i `platformAdminPage` — różne wyniki. |
| Testy REST | `AuditSecurityTest` (istniejące). |
| Playwright capabilities | Multi-project fixtures, worker-scoped auth, `.filter()` na result set. |
| REST Assured | Istniejące testy bezpieczeństwa audit. |
| Poziom | senior |
| MVP | NIE (faza 2, po multi-role setup) |

---

### Obszar B — Merchant operations (5 funkcji)

---

**F-06: Merchant Onboarding Wizard (multi-step form)**

| | |
|---|---|
| Opis | Wielokrokowy formularz tworzenia merchantów (krok 1: dane podstawowe, krok 2: konfiguracja, krok 3: przegląd i zapisz). |
| Realizm | W PSP onboarding merchantów ma wiele kroków i walidację cross-step. |
| Wartość SDET | Ćwiczy nawigację między krokami, stan formularza, guard przy cofnięciu (unsaved changes). |
| Testy UI | Playwright: krok 1 → walidacja → krok 2 → cofnięcie → state preserved → submit. |
| Testy REST | POST /api/merchants z walidacją. |
| Playwright capabilities | Multi-step form navigation, `page.dialog()` (unsaved changes confirm), `expect(step-indicator)`. |
| REST Assured | Create merchant assertions (istniejące). |
| Backend endpointy | POST /api/merchants (istniejący). |
| Frontend | `/admin/merchants/new` wizard page, MerchantOnboardingWizard component. |
| Flyway | Opcjonalnie: `description` kolumna w `merchants`. |
| Poziom | medium |
| MVP | NIE (faza 2) |

---

**F-07: Merchant API Credentials Panel**

| | |
|---|---|
| Opis | Sekcja merchantów z wygenerowanym API key (show/hide toggle, copy to clipboard, regenerate). |
| Realizm | Każdy PSP wydaje API credentials merchantom dla integracji. |
| Wartość SDET | Ćwiczy clipboard API (`page.evaluate → navigator.clipboard`), reveal/hide pattern, confirmation dialog przed regeneracją. |
| Testy UI | Toggle visibility → text type=password → click show → text visible. Copy → verify clipboard. Regenerate → confirm dialog. |
| Testy REST | POST /api/merchants/{id}/credentials. |
| Playwright capabilities | `page.evaluate`, clipboard, ConfirmModal dialog, `input[type=password]` assertions. |
| Backend endpointy | GET/POST `/api/merchants/{id}/credentials` (nowy, wartości są masked). |
| Frontend | MerchantCredentialsPanel w merchant detail view. |
| Flyway | `merchant_credentials` table (hashed API key). |
| Poziom | senior |
| MVP | NIE (faza 3) |

---

**F-08: Merchant Risk Flags Display**

| | |
|---|---|
| Opis | Badge „HIGH RISK" / „FLAGGED" na karcie merchantów, możliwość oznaczania przez PLATFORM_ADMIN. |
| Realizm | PSP flaguje ryzykownych merchantów dla compliance. |
| Wartość SDET | Ćwiczy warunkowe renderowanie, RBAC (tylko PLATFORM_ADMIN widzi flag controls), badge assertions. |
| Testy UI | Zalogowany MERCHANT_MANAGER nie widzi flag toggle. PLATFORM_ADMIN widzi i może kliknąć. |
| Testy REST | PATCH `/api/merchants/{id}/risk-flag`. |
| Playwright capabilities | `not.toBeVisible()` dla RBAC-hidden elements, `getByRole('checkbox')`, multi-role projects. |
| Backend endpointy | PATCH `/api/merchants/{id}` (extended) lub dedykowany endpoint. |
| Frontend | RiskFlagBadge component, risk flag toggle dla PLATFORM_ADMIN. |
| Flyway | `risk_flag` kolumna w `merchants`. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-09: Idempotency Demo Panel (Merchant Level)**

| | |
|---|---|
| Opis | Dedykowany sub-panel w Error Lab pokazujący idempotency replay: send same request twice z tym samym/różnym body. |
| Realizm | Idempotency-Key jest krytyczna w PSP (double-charge prevention). |
| Wartość SDET | Ćwiczy network request capture, `page.waitForResponse`, header assertions na powtórzonych requestach. |
| Testy UI | Click „Replay Same" → 200 + body identical. Click „Conflict" → 409 + ProblemDetailsCard. |
| Testy REST | Już pokryte w PaymentOrderRestAssuredTest. Brakuje UI demo. |
| Playwright capabilities | `page.waitForResponse()`, response body comparison, `route.fulfill` dla idempotency mock. |
| Backend endpointy | Brak nowych (Error Lab wywołuje istniejące). |
| Frontend | 2 nowe triggery w Error Lab: „Idempotent Replay" + „Idempotency Conflict". |
| Poziom | medium |
| MVP | TAK (rozszerzenie Error Lab) |

---

**F-10: Merchant Detail Page (pełna)**

| | |
|---|---|
| Opis | Osobna strona `/admin/merchants/{id}` z pełnymi danymi merchantów, historią statusów, summary płatności. |
| Realizm | W każdym PSP merchant detail to kluczowy widok operatora. |
| Wartość SDET | Kompleksowy scenariusz: nawigacja, zagnieżdżone data, tabs, async loading. |
| Testy UI | Navigate → merchant detail loads → summary cards → status history tab → payment orders tab. |
| Testy REST | GET /api/merchants/{id} + GET /api/merchants/{id}/payment-orders/summary. |
| Playwright capabilities | `page.goto()`, nested tabs (`UTabs`), async data loading assertions, `test.step()`. |
| Backend endpointy | GET /api/merchants/{id} (istniejący). |
| Frontend | `/admin/merchants/[id]/index.vue` + MerchantDetailTabs. |
| Flyway | Brak nowych. |
| Poziom | medium |
| MVP | TAK (bliskie istniejącej funkcjonalności) |

---

### Obszar C — Payment lifecycle advanced (6 funkcji)

---

**F-11: Partial Capture and Partial Refund UI**

| | |
|---|---|
| Opis | UI dla capture/refund z opcjonalnym `amountMinor` (poniżej oryginalnej kwoty). |
| Realizm | W PSP partial capture/refund to standard (np. refund tylko jednej pozycji). |
| Wartość SDET | Ćwiczy walidację pól numerycznych, conditional inputs, bounds checking. |
| Testy UI | Playwright: kliknij capture → wpisz amount > authorized → błąd walidacji. Wpisz poprawne → sukces. |
| Testy REST | POST /authorize → POST /capture z partial amount → assert CAPTURED amount < authorized. |
| Playwright capabilities | `getByLabel('Amount')`, `fill`, validation error assertions, conditional input visibility. |
| REST Assured | Istniejące lifecycle tests z opcjonalnym amount. |
| Backend endpointy | POST /capture i /refund z opcjonalnym `amountMinor` body (istniejące). |
| Frontend | Rozszerzenie PaymentOrderLifecycleActions o amount input (jest w Req 5.4). |
| Flyway | Brak (lifecycle columns już istnieją). |
| Poziom | medium |
| MVP | TAK (spec payment-operations-dashboard już to zawiera) |

---

**F-12: Concurrency Conflict Demo (Two-Window Fight)**

| | |
|---|---|
| Opis | Demo w 2 oknach: oba ładują ten sam payment order, jedno autoryzuje → ETag zmienia się → drugie dostaje 412. |
| Realizm | Optimistic locking to core wzorzec w systemach finansowych. |
| Wartość SDET | Ćwiczy multi-page Playwright, `browser.newPage()`, cross-page state. |
| Testy UI | Otwórz 2 strony → oba widzą ETag v1. Page 1: autoryzuj → ETag v2. Page 2: próba z v1 → 412 ProblemDetailsCard. |
| Testy REST | ETag/If-Match pattern już w PaymentOrderRestAssuredTest. |
| Playwright capabilities | `browser.newContext()`, `context.newPage()`, multi-page coordination, parallel assertions. |
| Backend endpointy | Istniejące. |
| Frontend | Brak nowych stron; same strony otwarte w 2 oknach. |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-13: Payment Order Expiration Flow (Clock Mock)**

| | |
|---|---|
| Opis | Payment order w stanie CREATED expiruje po skonfigurowanym TTL → status → EXPIRED. Frontend wyświetla pozostały czas + status po wygaśnięciu. |
| Realizm | Autoryzacja w PSP ma TTL (typowo 15 min). |
| Wartość SDET | Ćwiczy Playwright clock mocking: `page.clock.setFixedTime()` lub `page.clock.fastForward()`. |
| Testy UI | Mock clock → advance time → reload → status = EXPIRED → lifecycle buttons hidden. |
| Testy REST | GET /payment-orders/{id} → status EXPIRED (backend ma scheduled job lub TTL kolumnę). |
| Playwright capabilities | `page.clock.fastForward()` (nowe API!), `page.clock.setFixedTime()`. |
| Backend endpointy | Nowy: scheduled expiration job lub `GET /payment-orders/{id}` sprawdza TTL. |
| Frontend | ExpirationCountdown component, status badge EXPIRED. |
| Flyway | `expires_at` kolumna w `payment_orders`. |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-14: Retry Pattern Demo (Retry Failed Lifecycle Action)**

| | |
|---|---|
| Opis | Lifecycle action zwraca 503 (backend niedostępny) → UI pokazuje retry button → kliknięcie ponawia z tym samym Idempotency-Key. |
| Realizm | Retry w PSP: nie zmieniamy klucza idempotencji przy powtórzeniu. |
| Wartość SDET | Ćwiczy network interception (symulacja 503), `expect.poll`, retry assertions. |
| Testy UI | Mock 503 → error state z retry button → click retry → mock 200 → AUTHORIZED. |
| Testy REST | Retry pattern z tym samym kluczem → idempotent replay. |
| Playwright capabilities | `page.route()` + sequential fulfillments (503 then 200), `expect.poll`. |
| Backend endpointy | Istniejące. |
| Frontend | RetryButton w ErrorState lifecycle context. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-15: Payment Status Live Updates (Polling)**

| | |
|---|---|
| Opis | Strona detalu payment order polling-based refresh co N sekund → badge zmienia status bez przeładowania strony. |
| Realizm | W PSP statusy zmieniają się asynchronicznie (np. capture przez bank). |
| Wartość SDET | Ćwiczy `expect.poll`, `page.waitForResponse()`, mock response sequencing (status changes). |
| Testy UI | Mock: 1st poll = AUTHORIZED, 2nd poll = CAPTURED → assert badge transitions. |
| Testy REST | GET /payment-orders/{id} idempotent, coverage już istnieje. |
| Playwright capabilities | `expect.poll(() => ..., {timeout})`, `page.waitForResponse()`, route.fulfill sequencing. |
| Backend endpointy | Istniejące (GET /payment-orders/{id}). |
| Frontend | Polling composable z configurowalnym interwałem. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-16: Payment Order Summary Dashboard**

| | |
|---|---|
| Opis | Strona główna `/admin` z kartami: liczba merchantów, liczba zamówień, kwoty wg statusu i waluty. |
| Realizm | Operator widzi agregaty stanu platformy na głównym widoku. |
| Wartość SDET | Ćwiczy: loading states, empty states, multi-request orchestration, `Promise.all` w composable. |
| Testy UI | Mock responses → assert każda karta z prawidłową wartością; mock timeout → error state z retry. |
| Testy REST | GET /api/merchants/{merchantId}/payment-orders/summary (istniejący). |
| Playwright capabilities | `expect().toContainText()`, parallel request assertions, `page.waitForResponse()`. |
| Backend endpointy | Istniejące summary endpoint. Opcjonalnie: GET /api/platform/summary (nowy). |
| Frontend | `/admin/index.vue` z SummaryCards. Req 1 z payment-operations-dashboard spec. |
| Flyway | Brak. |
| Poziom | medium |
| MVP | TAK (jest w specyfikacji payment-operations-dashboard) |

---

### Obszar D — Error Lab i HTTP Lab rozszerzenia (4 funkcje)

---

**F-17: Error Lab — Rate Limit (429)**

| | |
|---|---|
| Opis | Trigger w Error Lab: 429 Too Many Requests z Retry-After header. |
| Realizm | Rate limiting to standard w każdym PSP API. |
| Wartość SDET | Ćwiczy: niestandarowe kody statusu, header Retry-After, `HttpStatusBadge`. |
| Testy UI | Click trigger → badge 429 → HeaderKeyValuePanel pokazuje Retry-After. |
| Testy REST | Mocked lub prawdziwy rate-limiting endpoint. |
| Playwright capabilities | `getByTestId('http-status-badge')`, `toContainText('429')`, header assertions. |
| Backend endpointy | GET/POST `/api/error-lab/trigger-429` + `Retry-After` header. |
| Frontend | 1 nowy trigger w Error Lab. |
| Poziom | medium |
| MVP | TAK (mała zmiana) |

---

**F-18: Network Response Timing Panel**

| | |
|---|---|
| Opis | Panel pokazujący czas odpowiedzi serwera (ms) dla ostatniego żądania. |
| Realizm | Ops/Support monitoruje czasy odpowiedzi. |
| Wartość SDET | Ćwiczy `performance.timing` w composable + Playwright `response.timing()`. |
| Testy UI | Assert timing panel shows a number after API call. |
| Testy REST | `HeaderAssertions.assertCorrelationIdPresent()` (istniejące). |
| Playwright capabilities | `page.on('response', r => r.timing())`, network timing assertions. |
| Backend endpointy | Brak nowych (X-Response-Time header opcjonalnie). |
| Frontend | ResponseTimingBadge component w ApiDebugPanel. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-19: HAR Export Button**

| | |
|---|---|
| Opis | Przycisk „Export HAR" zapisuje przechwycone requesty do pliku `.har` do pobrania. |
| Realizm | Debugging flows: eksport do narzędzi analizy sieciowej. |
| Wartość SDET | Ćwiczy file download w Playwright (`page.waitForEvent('download')`). |
| Testy UI | Click Export HAR → `download.suggestedFilename()` ends with `.har`. |
| Testy REST | Brak. |
| Playwright capabilities | `page.on('download')`, `download.path()`, `download.suggestedFilename()`. |
| Frontend | ExportHarButton component. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-20: Correlation ID Trace Panel**

| | |
|---|---|
| Opis | Panel w Error Lab i detail pages pokazujący X-Correlation-ID każdego żądania z copy-to-clipboard. |
| Realizm | Debugowanie: operator bierze correlation ID i szuka go w logach. |
| Wartość SDET | Ćwiczy header capture assertion + clipboard. Istniejący `HeaderKeyValuePanel` już to częściowo robi. |
| Testy UI | Assert X-Correlation-ID widoczny po triggerze. |
| Testy REST | `HeaderAssertions.assertCorrelationIdPresent()` (istniejące). |
| Playwright capabilities | `toContainText`, clipboard read assertion. |
| Frontend | Rozszerzenie istniejącego HeaderKeyValuePanel (minimalna zmiana). |
| Poziom | medium |
| MVP | TAK (uzupełnienie istniejącej funkcji) |

---

### Obszar E — Risk i Support (4 funkcje)

---

**F-21: Risk Review Queue**

| | |
|---|---|
| Opis | Lista payment orders FLAGGED (powyżej velocity threshold). PLATFORM_ADMIN / SUPPORT_AGENT może approve/reject manualnie. |
| Realizm | Risk team manualnie review'uje podejrzane transakcje. |
| Wartość SDET | Ćwiczy: filtrowanie, table pagination, bulk action, RBAC conditional buttons, confirmation dialog. |
| Testy UI | Login as SUPPORT_AGENT → widzi risk queue. Login as MERCHANT_MANAGER → nie widzi. Approve → status zmienia się. |
| Testy REST | POST /api/payment-orders/{id}/review (approve/reject). |
| Playwright capabilities | Multi-role auth setup, `getByRole('row').filter()`, `ConfirmModal` assertions. |
| Backend endpointy | GET/POST `/api/risk/review-queue`, POST `/api/payment-orders/{id}/review`. |
| Frontend | `/admin/risk` page, RiskReviewTable. |
| Flyway | `risk_flag`, `risk_score` kolumny. |
| Keycloak | Nowa rola lub istniejące authority. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-22: Support Search (Cross-Merchant)**

| | |
|---|---|
| Opis | Wyszukiwarka cross-merchant po `clientOrderReference`, `paymentOrderId`, kwocie, dacie. Dostępna dla PLATFORM_ADMIN i SUPPORT_AGENT. |
| Realizm | Support szuka konkretnej transakcji bez znajomości merchantId. |
| Wartość SDET | Ćwiczy: search input, debounce, empty/results/error state, cross-merchant auth (platform:payments:read). |
| Testy UI | Type search query → debounce → results appear → click result → detail page. |
| Testy REST | GET /api/payment-orders?clientOrderReference=... (cross-merchant). |
| Playwright capabilities | `fill`, `page.waitForResponse`, `expect.poll` (debounce), `click` on result row. |
| Backend endpointy | GET `/api/payment-orders` platform-scoped (nowy endpoint lub rozszerzenie istniejącego). |
| Frontend | `/admin/support/search` page, SupportSearchBox component. |
| Flyway | Index na `client_order_reference`. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-23: Export Payment Orders to CSV**

| | |
|---|---|
| Opis | Przycisk „Export CSV" na liście payment orders — pobiera plik CSV z aktywnymi filtrami. |
| Realizm | Rekoncyliacja: operatorzy eksportują transakcje do arkuszy. |
| Wartość SDET | Ćwiczy file download w Playwright: `page.waitForEvent('download')`, `fs.readFileSync(path)`, CSV parse. |
| Testy UI | Apply filters → click Export → download.csv → assert headers = ['id','amount','status',...]. |
| Testy REST | GET /api/merchants/{id}/payment-orders?format=csv. |
| Playwright capabilities | `page.on('download')`, `download.path()`, Node.js CSV parsing w teście. |
| Backend endpointy | GET `/api/merchants/{merchantId}/payment-orders?format=csv` (nowy, streaming). |
| Frontend | ExportCsvButton component. |
| Flyway | Brak. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-24: Upload Evidence File**

| | |
|---|---|
| Opis | Support może dołączyć dowód (PDF/IMG) do payment order (np. screenshot błędu, korespondencja z merchantem). |
| Realizm | Dispute resolution: support dołącza dowody do sprawy. |
| Wartość SDET | Ćwiczy file upload w Playwright: `page.setInputFiles()`, `fileChooser` events. |
| Testy UI | Click „Upload Evidence" → fileChooser event → setInputFiles(pdf) → assert filename visible → submit → success toast. |
| Testy REST | POST /api/payment-orders/{id}/evidence (multipart/form-data). |
| Playwright capabilities | `page.on('filechooser')`, `fileChooser.setFiles()`, `page.setInputFiles()`. |
| Backend endpointy | POST `/api/payment-orders/{id}/evidence`. |
| Frontend | EvidenceUploadDropzone component. |
| Flyway | `evidence_files` table. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

### Obszar F — Audit i Compliance (3 funkcje)

---

**F-25: Audit Log Export**

| | |
|---|---|
| Opis | Export audit logu do CSV/JSON z aktywnymi filtrami (actor, action, date range). |
| Realizm | Compliance export: audyt musi być eksportowalny. |
| Wartość SDET | Ćwiczy download + content assertions. |
| Testy UI | Apply audit filters → Export JSON → download → parse → assert entries match filters. |
| Playwright capabilities | `page.on('download')`, `download.path()`, JSON.parse w teście. |
| Backend endpointy | GET `/api/audit?format=json` (rozszerzenie istniejącego). |
| Frontend | ExportAuditButton w AuditTable. |
| Poziom | medium |
| MVP | NIE (faza 2) |

---

**F-26: Audit Before/After Diff View**

| | |
|---|---|
| Opis | Drawer detalu wpisu audytu pokazuje diff: przed zmianą vs po zmianie (np. displayName). |
| Realizm | Compliance: kto co zmienił i z czego na co. |
| Wartość SDET | Ćwiczy: drawer navigation, conditional content, diff rendering assertions. |
| Testy UI | Click audit entry → drawer opens → before/after diff visible. |
| Backend endpointy | Rozszerzenie `AuditEvent` o `before`/`after` JSON fields. |
| Frontend | AuditEntryDrawer (już istnieje) → rozszerzenie o diff panel. |
| Flyway | `before_state` i `after_state` JSONB w `audit_event`. |
| Playwright capabilities | `getByTestId('audit-entry-drawer')`, `.toBeVisible()`, `.toContainText()`. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-27: Comments/Internal Notes on Payment Orders**

| | |
|---|---|
| Opis | Support może dodawać wewnętrzne notatki do payment order (nie widoczne dla merchantów). |
| Realizm | CRM / dispute management: notatki do sprawy. |
| Wartość SDET | Ćwiczy: textarea, POST + GET notes, RBAC (tylko SUPPORT_AGENT i PLATFORM_ADMIN). |
| Testy UI | Login as MERCHANT_MANAGER → brak notatek. Login as SUPPORT_AGENT → widzi i dodaje notatkę. |
| Playwright capabilities | Multi-role fixture, `fill(textarea)`, `getByRole('listitem')` assertions. |
| Backend endpointy | GET/POST `/api/payment-orders/{id}/notes`. |
| Frontend | PaymentOrderNotes component w detail page. |
| Flyway | `payment_order_notes` table. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

### Obszar G — UI Complexity (4 funkcje)

---

**F-28: PSP Redirect Simulator (New Tab / External Redirect)**

| | |
|---|---|
| Opis | Przycisk „Simulate PSP Redirect" otwiera nową kartę z mockowym formularzem płatności. User wypełnia, wraca do oryginału. |
| Realizm | W PSP użytkownik jest redirectowany do bramki (np. Trustly, BLIK). |
| Wartość SDET | Ćwiczy: `context.waitForPage()`, multi-tab coordination, `page.close()`, inter-tab communication. |
| Testy UI | Click redirect → new tab opens → fill mock form → submit → close → original page updates. |
| Playwright capabilities | `context.waitForPage()`, `page.close()`, multi-page assertions. |
| Frontend | PSP Redirect Simulator page (osobna, bez auth, mock HTML). |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-29: PSP iframe Simulator**

| | |
|---|---|
| Opis | Sandboxed iframe w stronie detalu payment order symulujący formularz płatności PSP (bez PAN). |
| Realizm | Hosted Payment Page: PSP daje iframe z formularzem klientowi merchantów. |
| Wartość SDET | Ćwiczy `frameLocator()` — jedyna technika Playwright dla iframe. |
| Testy UI | `page.frameLocator('#psp-simulator-iframe').getByLabel('Card Last 4')`. |
| Playwright capabilities | `frameLocator()`, `frame.getByRole()`, `frame.fill()`, cross-frame assertions. |
| Frontend | PSP Simulator iframe (sandbox page bez PAN, mock data only). |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-30: Date Range Picker Filter**

| | |
|---|---|
| Opis | Datepicker (from/to) w filtrach payment order list i audit log. |
| Realizm | Operatorzy filtrują transakcje po datach. |
| Wartość SDET | Ćwiczy: kompleks combobox/calendar interactions, Date parsing, `page.clock`. |
| Testy UI | Open date picker → select date → calendar closes → filter applied → URL query updated. |
| Playwright capabilities | `getByRole('dialog')`, calendar grid navigation, `press('Enter')`, URL assertion. |
| Backend endpointy | Istniejące (fromDate/toDate query params). |
| Frontend | DateRangePicker component. |
| Poziom | senior |
| MVP | NIE (faza 2) |

---

**F-31: Keyboard Navigation and Command Palette**

| | |
|---|---|
| Opis | `Ctrl+K` otwiera command palette (search, navigate, actions). Tab navigation przez wszystkie kontrolki. |
| Realizm | Operator efficiency. |
| Wartość SDET | Ćwiczy keyboard interactions: `page.keyboard.press('Control+k')`, `Tab`, `Enter`, a11y. |
| Testy UI | `keyboard.press('Control+k')` → palette opens → type 'merchant' → Enter → navigate. |
| Playwright capabilities | `page.keyboard.press()`, `page.keyboard.type()`, ARIA snapshot assertions. |
| Frontend | CommandPalette component. |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-32: Toast Notification Assertions**

| | |
|---|---|
| Opis | Istniejące toasty (UToast) po operacjach — spójne `data-testid`, auto-dismiss. |
| Realizm | Istniejąca funkcja, brak E2E assertions. |
| Wartość SDET | Ćwiczy `expect().toBeVisible()` + `expect().not.toBeVisible()` na krótkotrwałych elementach; `expect.poll`. |
| Testy UI | Click activate → `data-testid="toast-success"` visible → auto-dismiss → not visible. |
| Playwright capabilities | `expect.poll(() => ..., { timeout })`, toast auto-dismiss timing, `toBeHidden()`. |
| Frontend | Rozszerzenie istniejących toastów o `data-testid`. |
| Poziom | medium |
| MVP | TAK (mała zmiana) |

---

**F-33: Mobile/Responsive Layout**

| | |
|---|---|
| Opis | Dashboard działa na mobile viewport (375px). |
| Realizm | Support może używać telefonu. |
| Wartość SDET | Ćwiczy `page.setViewportSize()`, `devices['iPhone 14']`, mobile-specific locators. |
| Testy UI | Set mobile viewport → assert sidebar collapsed → hamburger menu → navigation works. |
| Playwright capabilities | `devices['iPhone 14']`, `page.setViewportSize()`, `page.emulateMedia()`. |
| Poziom | senior |
| MVP | NIE (faza 3) |

---

### Obszar H — Accessibility i Visual (2 funkcje)

---

**F-34: ARIA Snapshot Tests**

| | |
|---|---|
| Opis | Snapshot semantyczny drzewa ARIA kluczowych stron (nie pixel screenshot). |
| Realizm | A11y compliance w systemach finansowych. |
| Wartość SDET | Ćwiczy `expect(locator).toMatchAriaSnapshot()` — API z Playwright 1.49+. |
| Testy | `await expect(page.getByTestId('merchant-table')).toMatchAriaSnapshot()` — snapshot ARIA structure. |
| Playwright capabilities | `toMatchAriaSnapshot()`, `aria-*` attribute assertions, `getByRole`. |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

**F-35: Visual Regression for Status Badges**

| | |
|---|---|
| Opis | Screenshot comparison dla BusinessStatusBadge (PENDING/ACTIVE/SUSPENDED/CREATED/...). |
| Realizm | CI gate: nie zmienić wizualnie stanu statusu bez review. |
| Wartość SDET | Ćwiczy `toHaveScreenshot()`, snapshot update workflow, `--update-snapshots`. |
| Playwright capabilities | `toHaveScreenshot()`, `page.screenshot()`, screenshot comparison config. |
| Poziom | expert |
| MVP | NIE (faza 3) |

---

## 6. Playwright 1.61 Capability Matrix

> Bieżąca wersja w repo: **1.60.0**. Capability matrix uwzględnia 1.60 + delta 1.61.
> `✓` = używane, `○` = można ćwiczyć na istniejącym systemie, `F-xx` = wymaga nowej funkcji.

| Playwright Capability | Poziom | Obecny stan | Funkcja systemu | POM/fixture | Mock? | Real BE? |
|---|---|---|---|---|---|---|
| `getByRole` | medium | ✓ | Merchant actions, lifecycle buttons | ButtonComponent | NIE | NIE |
| `getByLabel` | medium | ✓ | Form inputs (amount, currency) | FormComponent | NIE | NIE |
| `getByText` | medium | ✓ | Status text, toast messages | — | NIE | NIE |
| `getByTestId` | medium | ✓ | error-state, payment-order-table | — | NIE | NIE |
| Locator chaining | medium | brak | Merchant table → row → button | DataTable | NIE | NIE |
| `locator.filter()` | medium | brak | Risk queue filter by status | RiskTable | TAK | NIE |
| `expect().toBeVisible()` | medium | ✓ | Empty/error states | — | TAK | NIE |
| `expect().toBeHidden()` | medium | brak | RBAC-hidden elements | — | TAK | NIE |
| `expect().toBeDisabled()` | medium | brak | Lifecycle buttons for wrong status | — | TAK | NIE |
| `expect().toContainText()` | medium | ✓ | Problem details card text | — | TAK | NIE |
| `expect().toHaveValue()` | medium | brak | Form field values after reset | FormComponent | TAK | NIE |
| Soft assertions | senior | brak | Multi-field validation | — | TAK | NIE |
| `expect.poll` | senior | brak | F-15 (polling updates), toast dismiss | — | TAK | NIE |
| `page.route()` | medium | ✓ | Mock server/api responses | — | TAK | NIE |
| `route.fulfill()` | medium | ✓ | Mock merchant list, payment order | — | TAK | NIE |
| `route.fallback()` | medium | ✓ | Selective mocking | — | TAK | NIE |
| `route.fulfill` sequential | senior | brak | F-14 (503 then 200 retry) | NetworkSpy | TAK | NIE |
| `page.waitForResponse()` | senior | brak | F-15, F-22 (search debounce) | — | TAK | NIE |
| `page.waitForURL()` | medium | ✓ | Login redirect | — | NIE | NIE |
| `page.on('response')` | senior | brak | F-18 (timing), network monitoring | NetworkSpy | NIE | TAK |
| `page.on('download')` | senior | brak | F-23 (CSV), F-24 (evidence), F-25 | — | NIE | TAK |
| `page.on('filechooser')` | senior | brak | F-24 (upload evidence) | FileUpload | NIE | TAK |
| `page.setInputFiles()` | senior | brak | F-24 | — | NIE | TAK |
| `page.waitForEvent('download')` | senior | brak | F-23, F-25 | — | NIE | TAK |
| Multi-page / `browser.newPage()` | expert | brak | F-12 (2-window ETag fight), F-28 (PSP redirect) | — | NIE | TAK |
| `context.waitForPage()` | expert | brak | F-28 (PSP new tab) | — | NIE | TAK |
| `page.on('dialog')` | senior | brak | ConfirmActionModal (native dialogs) | — | TAK | NIE |
| `page.dialog().accept()` | senior | brak | Cancel/refund confirmation | — | TAK | NIE |
| `frameLocator()` | expert | brak | F-29 (PSP iframe simulator) | PSPSimulatorFrame | NIE | NIE |
| `frame.getByRole()` | expert | brak | F-29 | — | NIE | NIE |
| `APIRequestContext` | senior | brak | API-driven setup (reset/seed), hybrid tests | apiClient fixture | NIE | TAK |
| `request.post()` | senior | brak | Call /api/test/reset before suite | — | NIE | TAK |
| `request.get()` | senior | brak | Verify backend state after UI action | — | NIE | TAK |
| `storageState` | medium | ✓ | platform-operator.json (1 rola) | authFixture | NIE | TAK |
| Multi-role storageState | senior | brak | F-05, F-08, F-21, F-22 | workerStorageState | NIE | TAK |
| Worker fixtures | expert | brak | 5 ról × N workers | workerStorageState | NIE | TAK |
| `test.extend()` | senior | brak | Custom fixtures (apiClient, dataFactory) | baseFixture | NIE | NIE |
| `test.step()` | medium | brak | Lifecycle flow steps | — | TAK | NIE |
| `test.describe.configure()` | senior | brak | Serial lifecycle tests | — | TAK | NIE |
| Test annotations `@slow` `@skip` | medium | brak | Skip docker-dependent tests locally | — | NIE | NIE |
| `testInfo.attach()` | senior | brak | Attach API response body to report | — | TAK | NIE |
| Trace viewer | senior | ✓ (config) | All failing tests | — | TAK | NIE |
| Screenshot on failure | medium | brak (config) | Automatic on CI | — | NIE | NIE |
| Video recording | medium | brak (config) | Complex lifecycle flows | — | NIE | NIE |
| `toHaveScreenshot()` | expert | brak | F-35 (status badge visual) | — | NIE | NIE |
| `toMatchAriaSnapshot()` | expert | brak | F-34 (ARIA snapshots) | — | NIE | NIE |
| `page.keyboard.press()` | senior | brak | F-31 (Command palette, Ctrl+K) | — | NIE | NIE |
| `page.keyboard.type()` | senior | brak | F-31 (search in palette) | — | NIE | NIE |
| `page.evaluate()` | senior | brak | F-07 (clipboard), localStorage read | — | NIE | NIE |
| `page.clock.fastForward()` | expert | brak | F-13 (payment expiration) | — | NIE | NIE |
| `page.clock.setFixedTime()` | expert | brak | F-13 (deterministic date tests) | — | NIE | NIE |
| `page.setViewportSize()` | senior | brak | F-33 (mobile layout) | mobilePage | NIE | NIE |
| `devices['iPhone 14']` | senior | brak | F-33 (mobile project) | — | NIE | NIE |
| `page.emulateMedia()` | expert | brak | Print/PDF mode | — | NIE | NIE |
| `fullyParallel: true` | senior | brak | Multi-suite parallelism | workerData | NIE | NIE |
| Project sharding | expert | brak | CI split across N runners | — | NIE | NIE |
| HAR recording | expert | brak | F-19 (HAR export) | — | NIE | TAK |
| `page.localStorage` | expert | brak | **Playwright 1.61 NEW** — session state | — | NIE | NIE |
| `page.sessionStorage` | expert | brak | **Playwright 1.61 NEW** — session data | — | NIE | NIE |
| Console events `page.on('console')` | senior | brak | Assert no leaked tokens in console | — | TAK | NIE |
| Page error events | senior | brak | Detect JS errors during navigation | — | NIE | NIE |
| `expect().toHaveURL()` | medium | brak | Filter sync to URL params | — | TAK | NIE |
| `page.waitForLoadState()` | medium | brak | SPA navigation stabilization | — | NIE | NIE |
| Geolocation/locale | expert | brak | Locale-specific payment formats | geoFixture | NIE | NIE |
| Service workers | expert | brak | Offline mode simulation | — | NIE | NIE |
| Permissions API | expert | brak | Clipboard, notifications permissions | — | NIE | NIE |

---

## 7. REST Assured Capability Matrix

| REST Assured capability | Pokrycie | Klasy testowe | Funcja do ćwiczenia |
|---|---|---|---|
| `statusCode(201)` | ✓ | PaymentOrderRestAssuredTest | Create payment order |
| `statusCode(200)` | ✓ | Merchant + Payment | List, retrieve |
| `statusCode(400)` | ✓ | Validation tests | Malformed request |
| `statusCode(401)` | ✓ | Security tests | Missing JWT |
| `statusCode(403)` | ✓ | MerchantSecurityTest | Wrong authority |
| `statusCode(404)` | ✓ | TenantIsolationIT | Cross-tenant masked |
| `statusCode(409)` | ✓ | Idempotency tests | Conflict key |
| `statusCode(412)` | ✓ | ETag/If-Match tests | Stale precondition |
| `statusCode(415)` | brak | — | Unsupported media type |
| `statusCode(428)` | ✓ | PaymentOrderSecurity | Missing If-Match |
| `statusCode(429)` | brak | — | Rate limit (F-17) |
| `header("ETag", ...)` | ✓ | PaymentOrderRestAssured | ETag capture |
| `header("Location", ...)` | ✓ | Create assertions | Verify URI |
| `header("Vary", ...)` | ✓ | HTTP contract tests | Vary header |
| `header("X-Correlation-ID", ...)` | ✓ | All tests | Correlation |
| `header("Cache-Control", ...)` | ✓ | Payment detail | No-store |
| `header("Retry-After", ...)` | brak | — | F-17 (rate limit) |
| `header("Content-Type", "application/problem+json")` | ✓ | Error tests | Problem content type |
| `body("type", ...)` | ✓ | ProblemDetailsAssertions | Problem type field |
| `body("status", ...)` | ✓ | ProblemDetailsAssertions | Status code match |
| `body("detail", ...)` | ✓ | ProblemDetailsAssertions | Error detail |
| `body("content", ...)` | ✓ | List assertions | Paginated list |
| `body("totalElements", ...)` | ✓ | Pagination tests | Count assertion |
| TypeRef list extraction | ✓ | PaymentOrderList | Generic list |
| Extract + reuse (ETag chain) | ✓ | Lifecycle tests | Chained assertions |
| DB state after write | ✓ | Persistence IT | Repository verify |
| Testcontainers per-class | ✓ | PostgresContainerSupport | Isolation |
| WireMock stubs | ✓ | IAM tests | Keycloak mock |
| Idempotency pattern | ✓ | Create + replay | Same key = same response |
| Property-based (jqwik) | ✓ | TenantIsolationPropertyTest | P1-P6 properties |

---

## 8. Hybrid UI + REST Testing Matrix

Wzorce testów hybrydowych (Playwright APIRequestContext + REST Assured):

| Scenariusz | REST Assured setup | Playwright assertion | Wymagana funkcja |
|---|---|---|---|
| Create via API → assert in UI | POST /payment-orders via APIRequestContext | Lista UI pokazuje nowe zamówienie | Deterministyczny seed |
| Lifecycle via UI → verify via API | Click authorize w UI | GET /payment-orders/{id} → status = AUTHORIZED | Lifecycle pages |
| ETag conflict via API | Dwa równoległe PUT via REST Assured | 412 w UI widoczny | Concurrency demo |
| Reset + seed via API → Playwright | POST /api/test/reset + /api/test/seed | Playwright widzi deterministyczne dane | Spec #5 (done) |
| Authorization matrix via API | JWT z różnymi claims | UI pokazuje/ukrywa elementy | Multi-role fixtures |

---

## 9. Multi-Role/RBAC Testing Matrix

| Funkcja | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY_USER |
|---|:---:|:---:|:---:|:---:|:---:|
| Merchant list | Wszyscy | Swój tenant | NIE | Wszyscy (read) | Wszyscy (read) |
| Create merchant | TAK | NIE | NIE | NIE | NIE |
| Activate/suspend | TAK | NIE | NIE | NIE | NIE |
| Create payment order | TAK | TAK | TAK (własny merchant) | NIE | NIE |
| Lifecycle actions | TAK | TAK | TAK (własny) | NIE | NIE |
| Payment list (all) | TAK | TAK | Własny | TAK | Własny |
| Audit log | TAK | Własny tenant | NIE | TAK | NIE |
| User management | TAK | TAK (swój tenant) | NIE | NIE | NIE |
| Risk queue | TAK | NIE | NIE | TAK | NIE |
| Support search | TAK | NIE | NIE | TAK | NIE |
| Error Lab | TAK | TAK | TAK | TAK | TAK |

---

## 10. POM/Test Architecture Roadmap

> Nie implementuj teraz. Architektura rośnie naturalnie wraz z funkcjami.

### 10.1 Page Objects

```
tests/
├── pages/
│   ├── LoginPage.ts                    # login flow (real + mock)
│   ├── DashboardShell.ts               # nav, sidebar, user menu
│   ├── MerchantsPage.ts                # list, create, filter
│   ├── MerchantDetailPage.ts           # tabs: details, payments, settings
│   ├── PaymentOrdersPage.ts            # list, filters, pagination, export
│   ├── PaymentOrderDetailPage.ts       # detail, lifecycle actions, history
│   ├── UsersPage.ts                    # user CRUD, role assignment
│   ├── AuditLogPage.ts                 # filters, table, drawer, export
│   ├── ErrorLabPage.ts                 # all error triggers
│   ├── RiskReviewPage.ts               # flagged queue, approve/reject
│   ├── SupportSearchPage.ts            # cross-merchant search
│   └── TenantSettingsPage.ts           # tenant config
├── components/
│   ├── NavigationSidebar.ts            # nav links, active states
│   ├── DataTable.ts                    # generic: rows, pagination, sort
│   ├── FilterBar.ts                    # filters, clear, apply
│   ├── ProblemDetailsCard.ts           # assert type/status/detail
│   ├── ApiDebugPanel.ts                # request/response headers
│   ├── HeaderKeyValuePanel.ts          # ETag, X-Correlation-ID, Vary
│   ├── StatusBadge.ts                  # text label assertions (never color)
│   ├── ConfirmationModal.ts            # accept/cancel dialog
│   ├── ToastNotification.ts            # visible + auto-dismiss
│   ├── DrawerComponent.ts              # open, navigate, close
│   ├── TabsComponent.ts                # select, active
│   ├── DateRangePicker.ts              # calendar, date selection
│   ├── IdempotencyKeyInput.ts          # generated key, editable
│   └── EtagIfMatchInputs.ts            # read ETag, write If-Match
└── fixtures/
    ├── base.fixture.ts                 # test.extend base
    ├── auth.fixture.ts                 # workerStorageState per role
    ├── api.fixture.ts                  # APIRequestContext client
    ├── data.fixture.ts                 # dataFactory, merchantFactory, paymentFactory
    └── roles/
        ├── platform-admin.fixture.ts
        ├── tenant-admin.fixture.ts
        ├── merchant-manager.fixture.ts
        ├── support-agent.fixture.ts
        └── read-only-user.fixture.ts
```

### 10.2 Fixture Architecture

```ts
// Wzorzec worker-scoped auth (docelowy)
export const test = base.extend<{}, { workerStorageState: string }>({
  storageState: ({ workerStorageState }, use) => use(workerStorageState),
  workerStorageState: [async ({ browser }, use) => {
    const role = process.env.ROLE || 'platform-admin'
    const stateFile = `tests/.auth/${role}.json`
    // Auth once per worker via real Keycloak or mock
    await use(stateFile)
  }, { scope: 'worker' }]
})

// Wzorzec API-driven setup
export const test = base.extend<{ apiClient: APIRequestContext }>({
  apiClient: async ({ playwright }, use) => {
    const ctx = await playwright.request.newContext({ baseURL: 'http://localhost:8080' })
    await ctx.post('/api/test/reset')
    await ctx.post('/api/test/seed')
    await use(ctx)
    await ctx.dispose()
  }
})
```

### 10.3 Data Strategy (docelowa)

1. **Deterministyczny seed** via `POST /api/test/seed` przed każdym suitem.
2. **Worker-aware data ownership**: każdy worker operuje na innym merchantId (np. MERCHANT_ALPHA_001 dla worker 0, MERCHANT_ALPHA_002 dla worker 1).
3. **No test-order dependency**: każdy test resetuje dane lub tworzy własne.
4. **Stable IDs z Fixtures Catalog**: testy używają `MERCHANT_ALPHA_001_ID` z shared constants, nigdy hardcoded UUIDs.
5. **Cleanup by reset**: koniec suitów = `POST /api/test/reset`, nie `deleteAll()` przez JDBC.

### 10.4 Anti-patterns do unikania

- `waitForTimeout()` — zastąp `waitForResponse()` lub `expect.poll()`
- CSS selectors — używaj role/label/testid
- Shared mutable state między testami
- Business logic w testach (obliczenia kwot, daty)
- Jeden gigantyczny POM per strona
- Over-mocking (testy, które nic nie testują bo wszystko jest mockowane)
- Hardcoded polskie/angielskie texty zamiast `getByRole`
- Color-only assertions (nigdy `.toHaveCSS('color', 'red')`)
- Screenshots na wszystko (tylko dla visual regression, nie dla stanu)

---

## 11. Plan nauki — 60 lekcji

### 11.1 Lekcje medium (20 lekcji)

| # | Nazwa | Funkcja systemu | Playwright concept | REST concept | Pułapka |
|---|---|---|---|---|---|
| M-01 | Locator Hierarchy | Merchant list | `getByRole → getByText → getByTestId` priority | GET /api/merchants | CSS selector temptation |
| M-02 | Route Mock Basics | Error Lab 400 | `page.route()` + `route.fulfill()` | Problem+json contract | Forgetting content-type |
| M-03 | Form Fill & Validate | Create payment order | `fill`, `click`, schema error assertions | POST validation | `waitForTimeout` instead of `expect` |
| M-04 | Loading/Empty/Error States | Merchant list states | `toBeVisible`, `not.toBeVisible()` | Async data states | Race conditions without `await` |
| M-05 | Auth Setup Pattern | 5-role Keycloak login | `storageState`, `auth.setup.ts` | JWT claims | Re-login in every test |
| M-06 | Status Badge Assertions | Payment status | `getByRole('cell')`, `toContainText` | Lifecycle status values | Color-only assertions |
| M-07 | Toast Auto-Dismiss | Merchant activate success | `expect.poll`, `toBeHidden()` | — | Timing issues with fixed delay |
| M-08 | URL Sync Test | Payment filter to URL | `toHaveURL`, `page.goto(url?status=CREATED)` | Filter query params | URL encoding surprises |
| M-09 | Mock Auth Session | Protected route | `page.route('**/api/_auth/session')` | Session structure | 401 redirect loops |
| M-10 | Select/Combobox | Currency select | `page.selectOption`, `getByText('PLN')` | Enum validation | Native vs custom select |
| M-11 | Error State with ProblemDetails | Merchant 500 | `getByTestId('problem-details-card')` | application/problem+json | Response body structure |
| M-12 | Pagination Assertions | Payment order list | `getByRole('button', { name: /next page/i })` | page/size/totalElements | Off-by-one in pagination |
| M-13 | Header Panel Assertions | ETag in UI | `getByTestId('http-headers-panel').toContainText(etag)` | ETag header | Mocking both request and response |
| M-14 | Empty State Navigation | No merchants → create | `getByRole('link', { name: 'Create' })` | — | Empty vs loading confusion |
| M-15 | Soft Assertions | Multi-field validation | `expect.soft()` | Validation contract | Missing `expect.soft().toPass()` |
| M-16 | Step Annotations | Full create-activate flow | `test.step('Create merchant')` | Full flow | Step granularity |
| M-17 | Forbidden State | MERCHANT_MANAGER → audit | Auth fixture (merchant role) | 403 body | Token exposure in UI |
| M-18 | Network Request Count | Mock guards | `let count = 0; route: count++` | Idempotency | Race in async assertions |
| M-19 | idempotency-key-input | Create form resubmit | `getByTestId('idempotency-key-input')` | Same key = same response | Key changes on re-render |
| M-20 | Correlation ID visible | Error Lab trigger | `toContainText('X-Correlation-ID')` | X-Correlation-ID header | Masked vs visible |

### 11.2 Lekcje senior (20 lekcji)

| # | Nazwa | Funkcja systemu | Playwright concept | REST concept | Pułapka |
|---|---|---|---|---|---|
| S-01 | Worker Fixtures Auth | Multi-role test suite | `workerStorageState`, `test.extend` | Multi-role matrix | Sharing state between workers |
| S-02 | APIRequestContext Setup | Reset+seed przed testem | `request.post('/api/test/reset')` | Reset endpoint contract | Testing flag not enabled |
| S-03 | Hybrid UI+API Test | Create via API → verify in UI | `request.post()` + `page.goto()` | REST contract | Data isolation between workers |
| S-04 | ETag Capture | Lifecycle flow | `page.waitForResponse` → `.headers()['etag']` | If-Match flow | ETag with quotes |
| S-05 | Stale If-Match 412 | ConfirmActionModal + 412 | Fill If-Match with stale value | 412 vs 428 distinction | Retaining stale value in UI |
| S-06 | Multi-Project Setup | 5 roles × chromium | `playwright.config.ts` multi-project | Role-based expectations | Dependencies order |
| S-07 | `expect.poll` Debounce | Support search debounce | `expect.poll(() => page.locator(...).count(), { timeout })` | GET debounce | Poll interval too short |
| S-08 | File Download | CSV export | `page.waitForEvent('download')`, `download.path()` | Streaming response | Download path doesn't exist |
| S-09 | File Upload | Evidence upload | `page.on('filechooser')`, `.setFiles()` | multipart/form-data | File size limits |
| S-10 | Dialog Handling | Confirm cancel/refund | `page.on('dialog', d => d.accept())` | Destructive lifecycle | Dialog fires before `click()` |
| S-11 | `testInfo.attach()` | API response debugging | `testInfo.attach('response', { body })` | — | Large attachments slow reports |
| S-12 | Locator Filter | Risk queue by FLAGGED | `page.getByRole('row').filter({ hasText: 'FLAGGED' })` | Status filter | Filter before or after data load |
| S-13 | Console Event Monitoring | Token leak prevention | `page.on('console', msg => check(msg))` | — | Async log timing |
| S-14 | `page.waitForResponse` Pattern | Lifecycle submit | `await page.waitForResponse(r => r.url().includes('/authorize'))` | Lifecycle endpoint | Response before assertion |
| S-15 | Sequential Route Mocks | Retry demo (503 → 200) | Stateful `route.fulfill()` | Retry pattern | Mock state persistence |
| S-16 | Keyboard Navigation | Tab through form | `page.keyboard.press('Tab')` × N | — | Focus trap in modals |
| S-17 | Date Picker Interaction | Audit log date filter | Calendar navigation, `press('Enter')` | fromDate/toDate params | Locale date formats |
| S-18 | Mobile Viewport | Dashboard responsive | `page.setViewportSize({ width: 375, height: 812 })` | — | Hamburger hidden in full |
| S-19 | Network Timing | Response time panel | `response.timing()`, `.request().timing()` | X-Response-Time | No timing before response |
| S-20 | Parallel-Safe Data | Worker isolation | Merchant per worker, reset/seed strategy | — | Global reset in parallel |

### 11.3 Lekcje expert (20 lekcji)

| # | Nazwa | Funkcja systemu | Playwright concept | REST concept | Pułapka |
|---|---|---|---|---|---|
| E-01 | Multi-Context Auth | PLATFORM_ADMIN + MERCHANT_MANAGER w 1 teście | `browser.newContext()` per role | Role separation | Shared context leak |
| E-02 | Two-Window ETag Fight | Concurrency conflict F-12 | 2× `context.newPage()`, cross-page assertions | Optimistic locking 412 | Race condition between pages |
| E-03 | PSP New Tab | PSP redirect simulator F-28 | `context.waitForPage()` | External redirect | Page close before assertion |
| E-04 | iframe Interaction | PSP iframe simulator F-29 | `page.frameLocator('#psp-iframe')` | — | Cross-origin iframe restrictions |
| E-05 | Clock Mocking | Payment expiration F-13 | `page.clock.fastForward(15 * 60 * 1000)` | expires_at TTL | Real timers vs fake |
| E-06 | ARIA Snapshot | Audit table a11y | `expect(locator).toMatchAriaSnapshot()` | — | Snapshot drift with data changes |
| E-07 | Visual Screenshot | Status badge regression F-35 | `expect(badge).toHaveScreenshot('active-badge.png')` | — | Platform pixel differences |
| E-08 | HAR Recording | Network trace | `browser.newContext({ recordHar })` | Full HAR capture | HAR size |
| E-09 | Trace Viewer | Debugging failing test | `trace: 'on'`, `npx playwright show-trace` | — | Trace file too large |
| E-10 | Worker Sharding | CI parallel | `--shard=1/4`, project dependencies | — | Test interdependency |
| E-11 | localStorage Inspect | Session state (1.61 API) | `page.localStorage.get('key')` | — | Only available in 1.61+ |
| E-12 | Custom Reporter | Test results dashboard | Custom reporter class | — | Reporter async timing |
| E-13 | Clipboard Assertions | Copy API key F-07 | `page.evaluate(() => navigator.clipboard.readText())` | — | Clipboard permission denied |
| E-14 | Service Worker Mock | Offline simulation | `page.context().setOffline(true)` | — | SW not intercepting all requests |
| E-15 | Geolocation/Locale | Payment locale format | `context.grantPermissions(['geolocation'])` | — | Locale-specific date formats |
| E-16 | `expect.soft()` Full Suite | Audit entry details | Multiple soft assertions + `.toPass()` | — | Error swallowing |
| E-17 | Flaky Test Diagnosis | Retry/waitForTimeout | Replace `waitForTimeout` with `waitForResponse` | — | Masking real timing issues |
| E-18 | Property Test + Playwright | Fast-check + page state | `fc.assert(fc.property(..., async () => page.evaluate(...)))` | — | Async property cleanup |
| E-19 | `testInfo.project` | Role-conditional assertions | `test.info().project.name` → conditional | — | Hardcoded project name |
| E-20 | Full POM Architecture | Multi-feature test suite | All POMs + fixtures + data strategy | All REST patterns | POM becoming too large |

---

## 12. MCP Usage Plan

### 12.1 Playwright Browser Automation MCP (deferred tools)

Dostępne MCP Playwright (`mcp__playwright__browser_*`) mogą być używane do:
- **Eksploracji działającej aplikacji** bez pisania testów: `browser_navigate`, `browser_snapshot`, `browser_take_screenshot`.
- **Zbierania ARIA tree** istniejących stron: `browser_snapshot` zwraca ARIA tree → podstawa dla `toMatchAriaSnapshot()`.
- **Testowania locatorów**: `browser_click`, `browser_fill` → weryfikacja, że locator działa.
- **Zbierania request/response**: `browser_network_requests` → mapowanie endpointów.

**Kiedy używać:** przed pisaniem nowego testu — zbadaj stronę przez MCP, zbierz ARIA tree, zidentyfikuj testId-y. Nie zastępuje Playwright w CI.

### 12.2 Context7 dla Playwright

Przed użyciem API Playwright:
1. `mcp__context7__resolve-library-id` → `/microsoft/playwright`
2. `mcp__context7__query-docs` dla konkretnego API (np. `page.clock`, `frameLocator`, `localStorage`)

Krytyczne obszary do weryfikacji przez Context7 przed implementacją:
- `page.clock.*` — API dostępne od Playwright 1.45+
- `page.localStorage` / `page.sessionStorage` — **TYLKO Playwright 1.61+** (current: 1.60.0 → wymaga upgrade)
- `toMatchAriaSnapshot()` — dostępne od Playwright 1.49+
- `test.describe.configure({ mode: 'serial' })` — weryfikacja składni

### 12.3 Official Playwright docs fallback

Używaj gdy Context7 jest niejednoznaczny dla:
- Release notes konkretnej wersji (co nowego w 1.61)
- Breaking changes między wersjami

---

## 13. Claude Code / Codex Skill Alignment

### 13.1 Dostępne skills i mapowanie do ról płatniczo-SDET

| Skill (Claude Code / Kiro) | Odpowiednik z listy użytkownika | Użycie |
|---|---|---|
| `typescript6-playwright-engineering` | playwright-expert + playwright-pom-architect | Projekt POM, fixtures, auth, parallel |
| `parallel-test-architecture-and-data-isolation` | playwright-pom-architect (część) | Worker isolation, data strategy |
| `payment-quality-lab-orchestrator` | psp-architect + fintech-product-architect | Koordynacja całego roadmapy |
| `business-analysis-and-product-discovery-for-payment-lab` | psp-architect + multi-tenant-saas-architect | Nowe funkcje domenowe |
| `spring-boot4-spring7-backend-architect` | spring-boot-architect | Backend endpointy dla nowych funkcji |
| `spring-modulith-2-0-6-modular-monolith-testing` | spring-modulith-architect | Moduły, testy architektoniczne |
| `nuxt-dashboard-zod-pinia-frontend-engineering` | nuxt-ui-architect + frontend-ux-architect | Frontend nowych funkcji |
| `test-analysis-design-and-data` | sdet-test-architect + test-design-architect | Scenariusze testowe, dane |
| `rapid-software-testing-risk-thinking` | sdet-test-architect (ryzyko) | Ryzyka, heurystyki |
| `junit6-assertj-restassured-testcraft` | rest-assured-expert + api-contract-architect | REST Assured coverage |
| `rest-api-security-oauth-testing` | rest-assured-expert (security) + keycloak-security-architect | Security tests |
| `postgres18-data-architecture-and-risk` | postgresql-architect | Flyway, schema, data risks |
| `java25-effective-java-mentor` | qa-mentor (Java) | Jakość kodu Java |
| `java-rest-api-testing-effective-java-mentor` | rest-assured-expert + qa-mentor | REST Assured design |
| `official-docs-and-versioned-research` | (wszystkie architekci) | Weryfikacja wersji API |
| `obsidian-learning-os` | docs-architect | Notatki Obsidian, MOC |
| `bpmn-uml-dmn-for-testers` | (brak odpowiednika) | Diagramy przepływów |
| `spec-kit-feature-workflow` | (wewnętrzny) | Cykl spec → implementacja |

**Braki skills (nie istnieją ani w Claude Code ani w Kiro):**
- `playwright-mcp-expert` — obsługę MCP Playwright przejmuje `typescript6-playwright-engineering`
- `visual-regression-testing-expert` — brak; tę wiedzę trzeba dołączyć do `typescript6-playwright-engineering`
- `accessibility-testing-expert` — brak; jest tylko ARIA snapshot w Playwright skill

### 13.2 Proponowane ujednolicenie (bez wdrożenia)

**Zasada 1 — Nazwy skills identyczne w Claude Code i Kiro.**
Obecnie nazwy plików w `.kiro/skills/*/SKILL.md` są identyczne z zarejestrowaniami w Claude Code. To dobry stan.

**Zasada 2 — Każdy skill musi opisać: kiedy wywołać, czego NIE robić, jak raportować.**
Propozycja rozszerzenia każdego SKILL.md o sekcje:
```
## Trigger
## Do NOT use for
## Report format (what agent should output)
## Context7 deps required
## Playwright MCP usage
```

**Zasada 3 — Codex (przez AGENTS.md) i Claude Code (przez system prompt) korzystają z tych samych skills.**
Codex → `.kiro/skills/` przez explicit `#skill-name` wzmianki w promptach. Claude Code → `/skill-name` command.

**Zasada 4 — Scope guardrails w każdym skiciu.**
Każdy skill musi jawnie mówić co jest poza zakresem (scope creep prevention):
```
## Out of scope
- Nie implementuj bez osobnego polecenia
- Nie generuj testów Playwright
- Nie zmieniaj backend contracts
```

**Zasada 5 — Nowe skills potrzebne:**
- `playwright-1-60-testing` (rozszerzenie `typescript6-playwright-engineering` o POM/fixtures)
- `visual-a11y-testing` (ARIA, screenshot, visual regression)
- `playwright-mcp-integration` (jak używać MCP Playwright do eksploracji przed pisaniem testów)

### 13.3 Jak agenty mają raportować użyte skills

Po każdej odpowiedzi zawierającej użycie skilla, agent powinien dołączyć sekcję:
```
## Skills użyte w tej odpowiedzi
- typescript6-playwright-engineering (fixtures design)
- parallel-test-architecture-and-data-isolation (worker isolation)
```

---

## 14. Roadmapa fazowa

### Faza 1 — Quick Wins (wartość Playwright + niski koszt)

**Cel:** Odblokowanie 5 ról, APIRequestContext, network assertions — bez nowych stron.

| # | Funkcja | Playwright | REST Assured | Backend | Frontend | DB |
|---|---|---|---|---|---|---|
| 1 | Multi-role auth setup (5 storage states) | `workerStorageState`, multi-project | — | Brak | auth.setup.ts rozszerzenie | — |
| 2 | Upgrade Playwright do 1.61 | `page.localStorage` unlock | — | — | package.json | — |
| 3 | F-01: Tenant Context Indicator | `getByRole` + text assertion | Brak | Brak | `dashboard.vue` badge | Brak |
| 4 | F-04: Cross-Tenant triggers w Error Lab | `toContainText('403')`, status badge | Brak | 2 nowe error-lab triggery | 2 triggery | Brak |
| 5 | F-09: Idempotency Demo Panel | `waitForResponse`, request capture | Brak | 2 nowe error-lab triggery | 2 triggery | Brak |
| 6 | F-17: Error Lab 429 | `HttpStatusBadge` 429, Retry-After | Brak | GET trigger-429 | 1 trigger | Brak |
| 7 | F-20: Correlation ID copy | `toContainText(correlationId)`, clipboard | Brak | Brak | HeaderKeyValuePanel copy | Brak |
| 8 | F-32: Toast testids | `expect.poll`, `toBeHidden()` | Brak | Brak | data-testid on toasts | Brak |
| 9 | F-10: Merchant Detail Page | `test.step`, tabs, async data | Brak | GET /merchants/{id} | `/admin/merchants/[id]` | Brak |
| 10 | payment-operations-dashboard spec | Wszystkie Req 1–12 | Brak | Brak | Dashboard enhancement | Brak |

**Czas:** 2-4 sprinty. Playwright concepts: locators, mocking, multi-project, auth fixtures, network assertions.

---

### Faza 2 — Senior SDET (multi-role, API setup, POM, network)

**Cel:** Pełna wielorolowość, APIRequestContext, file ops, search, risk.

| # | Funkcja | Playwright | Backend | Frontend | DB |
|---|---|---|---|---|---|
| 11 | F-02: Tenant Suspension Evidence | `not.toBeVisible()`, RBAC guards | Brak | tenant guard | Brak |
| 12 | F-03: Tenant Settings Page | ETag full flow w UI | PATCH /api/tenants/{id}/settings | settings page | tenants.contact_email |
| 13 | F-05: Tenant-Scoped Audit Demo | Multi-role, `.filter()` | Brak | role conditional | Brak |
| 14 | F-08: Merchant Risk Flags | `toBeChecked`, RBAC toggle | PATCH /merchants/{id} | RiskFlagBadge | risk_flag |
| 15 | F-14: Retry Pattern | Sequential route mocks | Brak | RetryButton | Brak |
| 16 | F-15: Payment Status Polling | `expect.poll` | Brak | polling composable | Brak |
| 17 | F-21: Risk Review Queue | Multi-role, `filter()`, ConfirmModal | POST /risk/review | `/admin/risk` | risk_score |
| 18 | F-22: Support Search | debounce, `waitForResponse` | GET /api/payment-orders (cross) | `/admin/support/search` | index |
| 19 | F-23: Export CSV | file download | GET ...?format=csv | ExportCsvButton | Brak |
| 20 | F-24: Upload Evidence | file upload, filechooser | POST /evidence | DropZone | evidence_files |
| 21 | F-25: Audit Export | file download + JSON parse | GET /api/audit?format=json | ExportAuditButton | Brak |
| 22 | F-26: Audit Before/After Diff | drawer + diff panel | audit_event.before/after | AuditEntryDrawer | before_state |
| 23 | F-27: Support Notes | textarea, multi-role | POST/GET /payment-orders/{id}/notes | PaymentOrderNotes | notes table |
| 24 | F-30: Date Range Picker | calendar, keyboard | Istniejące fromDate/toDate | DateRangePicker | Brak |
| 25 | F-33: Mobile Viewport | `setViewportSize`, hamburger | Brak | responsive layout | Brak |

---

### Faza 3 — Expert SDET (concurrency, iframe, clock, visual, a11y)

| # | Funkcja | Playwright | Backend | Frontend | DB |
|---|---|---|---|---|---|
| 26 | F-06: Merchant Onboarding Wizard | multi-step, dialog | POST /merchants | wizard | Brak |
| 27 | F-11: Partial Capture/Refund UI | conditional input, bounds | Istniejące | lifecycle actions extend | Brak |
| 28 | F-12: Concurrency Conflict Demo | multi-page, cross-page state | Istniejące | Brak (2 okna) | Brak |
| 29 | F-13: Payment Expiration (Clock) | `page.clock.fastForward()` | scheduled job | ExpirationCountdown | expires_at |
| 30 | F-16: Summary Dashboard | multi-request, `waitForResponse` | GET /summary | `/admin/index.vue` | Brak |
| 31 | F-18: Network Timing | `response.timing()` | optional X-Response-Time | ResponseTimingBadge | Brak |
| 32 | F-19: HAR Export | HAR recording | Brak | ExportHarButton | Brak |
| 33 | F-28: PSP New Tab | `context.waitForPage()` | Brak | PSP redirect mock page | Brak |
| 34 | F-29: PSP iframe | `frameLocator()` | Brak | iframe PSP simulator | Brak |
| 35 | F-31: Command Palette | `keyboard.press('Control+k')` | Brak | CommandPalette | Brak |
| 36 | F-34: ARIA Snapshots | `toMatchAriaSnapshot()` | Brak | ARIA improvements | Brak |
| 37 | F-35: Visual Regression | `toHaveScreenshot()` | Brak | Brak | Brak |
| 38 | F-07: API Credentials | clipboard, password toggle | GET/POST /credentials | MerchantCredentialsPanel | credentials |

---

### Faza 4 — Architecture Excellence

| Cel | Działanie |
|---|---|
| Unified POM | Implementacja pełnego drzewa pages/components/fixtures |
| Test data strategy | Worker-aware isolation z reset/seed API |
| CI strategy | Multi-project sharding na 4 runnery |
| CLAUDE.md/AGENTS.md alignment | Skill contract unification |
| Context7 automation | Pre-commit check dla wersji API |
| Obsidian integration | Lekcje Playwright → Obsidian notes |
| Definition of Done | Per-feature DoD checklist |

---

## 15. Playwright 1.61 Gap Analysis (Checklist)

| Capability | Stan | Co potrzeba | Realistyczna? | Warta budowania? |
|---|---|---|---|---|
| `getByRole/Label/Text/TestId` | ✓ COVERED | — | — | — |
| `page.route()` + `route.fulfill()` | ✓ COVERED | — | — | — |
| `storageState` (1 rola) | PARTIAL | Multi-role setup (Faza 1) | TAK | TAK |
| `workerStorageState` | NOT COVERED | Worker fixtures (Faza 1) | TAK | TAK |
| `APIRequestContext` | NOT COVERED | Deterministyczny seed (spec #5 done) | TAK | TAK |
| `page.waitForResponse()` | NOT COVERED | Lifecycle submit, search debounce | TAK | TAK |
| `page.on('download')` | NOT COVERED | CSV export (F-23) | TAK | TAK |
| `page.on('filechooser')` | NOT COVERED | Evidence upload (F-24) | TAK | TAK |
| `context.waitForPage()` | NOT COVERED | PSP redirect (F-28) | TAK | TAK (expert) |
| `frameLocator()` | NOT COVERED | PSP iframe (F-29) | TAK | TAK (expert) |
| `page.on('dialog')` | NOT COVERED | ConfirmModal (istniejące) | TAK | TAK |
| `page.clock.*` | NOT COVERED | Payment expiration (F-13) | TAK | TAK (expert) |
| `page.localStorage` | NOT COVERED | **Wymaga Playwright 1.61** | TAK | TAK po upgrade |
| `page.sessionStorage` | NOT COVERED | **Wymaga Playwright 1.61** | TAK | TAK po upgrade |
| `toMatchAriaSnapshot()` | NOT COVERED | ARIA test (F-34), dostępne w 1.49+ | TAK | TAK (expert) |
| `toHaveScreenshot()` | NOT COVERED | Visual regression (F-35) | TAK | TAK (expert) |
| `page.keyboard.press()` | NOT COVERED | Command palette (F-31) | TAK | TAK (expert) |
| `page.evaluate()` | NOT COVERED | Clipboard, localStorage | TAK | TAK |
| `expect.poll()` | NOT COVERED | Polling updates (F-15), toast | TAK | TAK |
| Soft assertions | NOT COVERED | Multi-field validation forms | TAK | TAK |
| `testInfo.attach()` | NOT COVERED | Debug attachments | TAK | TAK |
| `test.step()` | NOT COVERED | Complex flow steps | TAK | TAK |
| `test.describe.configure({serial})` | NOT COVERED | Lifecycle chained tests | TAK | TAK |
| Multi-project config | PARTIAL | 5 projects × 5 roles | TAK | TAK |
| `fullyParallel: true` | NOT COVERED | Równoległe testy | TAK | TAK (Faza 3) |
| Project sharding | NOT COVERED | CI split | TAK | TAK (Faza 4) |
| Video recording | NOT COVERED | CI debugging | TAK | TAK (konfiguracja) |
| Trace viewer usage | PARTIAL (config) | Jawna edukacja z trace | TAK | TAK |
| HAR recording | NOT COVERED | F-19 (HAR export) | TAK | TAK |
| `page.on('console')` | NOT COVERED | Token leak prevention | TAK | TAK |
| `response.timing()` | NOT COVERED | F-18 (network timing) | TAK | TAK |
| `page.setViewportSize()` | NOT COVERED | Mobile layout (F-33) | TAK | TAK |
| Service workers / offline | NOT COVERED | Offline demo | NIE (sztuczne) | NIE |
| WebAuthn/passkeys | NOT COVERED | Keycloak passkeys | NIE (za duże) | NIE |
| Geolocation | NOT COVERED | Locale payment formats | TAK | NIE (niski prio) |

**Skrót: 8 capabilities covered, 12 partially, ~20 not covered. Wszystkie realnie ćwiczalne bez fake features.**

---

## 16. Funkcje i pomysły do odrzucenia

| Pomysł | Dlaczego odrzucić |
|---|---|
| Fake KPI dashboard (revenue, conversion rate) | Nie wynika z backendu; sztuczne metryki |
| Top-level POST /payments API | W scope guardrails — PSP integration excluded |
| Kafka/webhooks/outbox | Za duże; uczy innej technologii, nie Playwright |
| Settlement/reconciliation | Nie ma backendu; sztuczne |
| Microservice split | Niszczy Spring Modulith; nic nie wnosi dla SDET |
| KYC / PAN / 3DS | Wymaga PCI DSS; zbyt produkcyjne |
| Separate test framework jako produkt | Framework-building trap; testy rosną z systemu |
| WebSocket real-time (bez uzasadnienia) | Overengineering; polling wystarczy dla lekcji |
| GraphQL API | Nie pasuje do istniejącego REST kontraktu |
| Playwright jako przeglądarka PSP (iframe prawdziwy) | Wymaga PCI compliance; użyj mock iframe |
| Visual snapshot na WSZYSTKO | Over-snapshotting = kruche testy |
| Dodanie 3 kolejnych DB (Redis, Mongo, etc.) | Nie ma wartości Playwright |
| Testy wydajnościowe w Playwright | Playwright nie jest do performance testing |

---

## 17. Ryzyka i trade-offs

| Ryzyko | Poziom | Mitigacja |
|---|---|---|
| Playwright 1.60 vs. 1.61 gap | WYSOKI | Upgrade do 1.61 w Fazie 1 (1 commit w package.json) |
| `fullyParallel: false` ogranicza lekcje o parallel | ŚREDNI | Faza 3: włączyć, po data isolation strategy |
| Keycloak wymagany dla real auth | ŚREDNI | Dwa tryby: PLAYWRIGHT_USE_REAL_KEYCLOAK + mock |
| Worker-aware data isolation trudna | ŚREDNI | Reset/seed API (spec #5 done!) |
| Feature creep (chcemy budować PSP) | WYSOKI | Scope guardrails w CLAUDE.md/AGENTS.md |
| Framework-building trap | WYSOKI | Zasada: POM rośnie RAZEM z systemem, nie wyprzedza |
| restkit/paymentsupport compile blocker | NISKI | Znany, udokumentowany w .codex |
| Podman vs Docker dla Testcontainers | NISKI | Skonfigurowane, zielone |

---

## 18. Rekomendowane pierwsze 10 kroków

1. **Upgrade Playwright 1.60 → 1.61** (`apps/frontend/package.json`): `@playwright/test: "1.61.0"`. Odblokuje `page.localStorage`, `page.sessionStorage`. Niski koszt.

2. **Multi-role auth setup** (5 projektów Playwright w `playwright.config.ts`): dodaj `platform-admin`, `tenant-admin`, `merchant-manager`, `support-agent`, `read-only-user` jako projekty z osobnymi storage states.

3. **Zaimplementuj `payment-operations-dashboard` spec** (już specowane, w `.kiro/specs/`): pełna dashboard strona główna + udoskonalone merchant/payment pages. Odblokuje 15+ Playwright scenarios.

4. **Dodaj Error Lab triggery dla 429 i cross-tenant (403)** (F-04, F-17): małe zmiany backend + frontend, duże pokrycie testowe.

5. **Zastąp `waitForTimeout(500)` w testach** przez `waitForResponse()` lub `expect.poll()`: usuń anti-pattern z `payment-order-create.spec.ts`.

6. **Zaimplementuj APIRequestContext fixture** (`tests/fixtures/api.fixture.ts`): `POST /api/test/reset` + `POST /api/test/seed` przed każdym suitem. Spec #5 backend jest ukończony — potrzebuje tylko Playwright client.

7. **Dodaj Toast data-testid** (F-32): małe rozszerzenie komponentów, duże odblokowanie dla `expect.poll` lekcji.

8. **Zaimplementuj Merchant Detail Page** (`/admin/merchants/[id]`) z tabs: summary/payments/settings. Odblokuje `test.step`, tab navigation.

9. **CSV Export** (F-23): `GET /payment-orders?format=csv` na backendzie + `ExportCsvButton`. Odblokuje file download lekcje.

10. **Zaprojektuj POM dla 3 pierwszych stron** (MerchantsPage, PaymentOrdersPage, ErrorLabPage) jako pierwszy krok do `world-class POM architecture`.

---

## 19. Definition of Done dla przyszłych funkcji

Każda nowa funkcja systemu jest gotowa, gdy:

**Backend:**
- [ ] Nowy endpoint udokumentowany w `CLAUDE.md`/`AGENTS.md` API section
- [ ] REST Assured test dla happy path + co najmniej 2 negative paths
- [ ] Security test: unauthenticated (401), wrong role (403), wrong scope (403/404)
- [ ] `ModulithArchitectureTest` przechodzi
- [ ] `X-Correlation-ID` w każdej odpowiedzi
- [ ] `application/problem+json` dla każdego błędu
- [ ] Flyway migration jeśli nowe tabele/kolumny
- [ ] `./mvnw verify` GREEN (z wyłączeniem restkit/paymentsupport per regułę)

**Frontend:**
- [ ] Strona/komponent z pełnymi stanami: loading, empty, error, success
- [ ] `data-testid` na wszystkich kluczowych elementach
- [ ] Zod schema dla response
- [ ] Authorization header zawsze zamaskowany w `HeaderKeyValuePanel`
- [ ] Brak tokenów/sekretów w browser DOM, cookies, local storage
- [ ] `corepack pnpm typecheck` GREEN
- [ ] `corepack pnpm test:unit` GREEN

**Playwright:**
- [ ] Co najmniej 1 E2E test dla happy path
- [ ] Co najmniej 1 E2E test dla error state
- [ ] Locatory używają `getByRole`/`getByLabel`/`getByTestId` (nie CSS)
- [ ] Brak `waitForTimeout()`
- [ ] Test izolowany: nie zależy od innych testów ani kolejności

---

## 20. Jak mierzyć wartość edukacyjną

| Metryka | Cel | Sposób pomiaru |
|---|---|---|
| Playwright capabilities coverage | 30+ z 50 w mapie | Checklist w tym dokumencie |
| Test scenarios per feature | ≥ 3 (happy/negative/edge) | PR review |
| REST Assured assertions per endpoint | ≥ 4 (status/body/headers/DB) | Code review |
| No `waitForTimeout` | 0 occurrences | `grep -r waitForTimeout tests/` |
| RBAC matrix coverage (Playwright) | 5 ról × N endpoints | Multi-role fixture coverage |
| Anti-patterns absent | 0 CSS selectors in tests | linter / code review |
| Deterministic test rate | 100% (no flaky) | CI pass rate |

---

## 21. Jak uniknąć Framework-Building Trap

1. **Nie twórz POM przed funkcją.** `MerchantsPage.ts` powstaje gdy `/admin/merchants` istnieje i jest testowane.
2. **Nie twórz abstraction przed 2+ użyciami.** Helper po 2 powieleniach, nie przed.
3. **Nie generalizuj lokatorów.** `DataTable` jako class rośnie z istniejących tabel — nie jest z góry zaprojektowana.
4. **Testy dokumentują kontrakt.** Jeśli test jest długi, to kontrakt jest złożony — nie ukrywaj go abstakcją.
5. **Fixture tylko dla cross-cutting concerns.** Auth (worker scope), API setup, viewport. Nie dla każdej strony.
6. **Nie testuj testów.** `DataTable` to helper, nie produkt do testowania.

---

## 22. Appendix — Aktualny skill inventory (Claude Code vs Kiro)

### Dostępne w Claude Code (system-reminder skills):

```
bpmn-uml-dmn-for-testers
business-analysis-and-product-discovery-for-payment-lab
java-practitioner-craftsmanship-and-conference-insights
java-rest-api-testing-effective-java-mentor
java25-effective-java-mentor
junit6-assertj-restassured-testcraft
maven-3-9-11-build-engineering
nuxt-dashboard-zod-pinia-frontend-engineering
obsidian-learning-os
official-docs-and-versioned-research
parallel-test-architecture-and-data-isolation
payment-quality-lab-orchestrator
postgres18-data-architecture-and-risk
project-skill-governance-and-quality-review
rapid-software-testing-risk-thinking
rest-api-security-oauth-testing
spec-kit-feature-workflow
spring-boot4-spring7-backend-architect
spring-modulith-2-0-6-modular-monolith-testing
test-analysis-design-and-data
typescript6-playwright-engineering
web-research-and-data-extraction
```

### Dostępne w Kiro (`.kiro/skills/*`):
Identyczne z powyższymi (ta sama lista).

### Brakujące (nie istnieją):
- `playwright-visual-a11y` (ARIA snapshots, screenshots, a11y assertions)
- `playwright-network-advanced` (HAR, timing, multi-page, iframe, clock)
- `playwright-mcp-integration` (jak używać MCP browser automation do eksploracji)

### Skille użyte do przygotowania tego raportu:
- `typescript6-playwright-engineering` — capability matrix, POM design
- `payment-quality-lab-orchestrator` — koordynacja roadmapy
- `test-analysis-design-and-data` — scenariusze, ryzyka
- `parallel-test-architecture-and-data-isolation` — worker fixtures, data strategy
- `rapid-software-testing-risk-thinking` — co odrzucić, scope guardrails
- `official-docs-and-versioned-research` — weryfikacja Playwright 1.61 przez Context7
- `business-analysis-and-product-discovery-for-payment-lab` — 35 proponowanych funkcji

---

*Wygenerowano: 2026-06-26. Następna aktualizacja: po implementacji Fazy 1 lub po zmianie głównego specyfikacji.*

---

## 23. Expert Review Addendum — Playwright 1.61 / POM / SDET Corrections

> Review patch dodany: 2026-06-26.
> Weryfikacja: `@playwright/test: "1.60.0"` potwierdzone w `apps/frontend/package.json` (linia 28).
> Poniższe korekty nie zmieniają istniejącej roadmapy — precyzują, poprawiają i rozszerzają.
> Żaden kod aplikacji nie został zmieniony. Żadne testy nie zostały uruchomione.

---

### 23.1 Upgrade Gate: Playwright 1.60 → 1.61

**Status na dzień 2026-06-26:** repozytorium używa `@playwright/test: "1.60.0"`.

**Zasada:** Nie wolno projektować ani implementować testów zależnych od API Playwright 1.61,
dopóki repozytorium nadal używa wersji 1.60.0. Testy takie nie skompilują się lub
zasygnalizują błąd runtime.

**Potwierdzone API zablokowane do upgrade (zweryfikowane przez Context7):**

| API | Wprowadzone | Opis | Stan |
|---|---|---|---|
| `page.localStorage.setItem/getItem/items()` | **1.61** | Bezpośredni dostęp do localStorage na bieżącym origin | 🔒 GATED |
| `page.sessionStorage.setItem/getItem/items()` | **1.61** | Bezpośredni dostęp do sessionStorage | 🔒 GATED |
| `browserContext.credentials` | **1.61** | Wirtualny authenticator WebAuthn/passkeys; rejestracja passkey bez sprzętowego klucza | 🔒 GATED |
| `video: 'on-all-retries'` | **1.61** | Nagrywaj wideo dla każdego retry | 🔒 GATED |
| `video: 'retain-on-first-failure'` | **1.61** | Zachowaj wideo tylko dla pierwszego nieudanego run (bez retries) | 🔒 GATED |
| `video: 'retain-on-failure-and-retries'` | **1.61** | Zachowaj wideo dla każdego run, który był retry lub zakończył się błędem | 🔒 GATED |
| `video.show.actions / video.show.test` | **1.61** | Wizualne adnotacje w wideo (highlight kliknięć, overlay statusu testu) | 🔒 GATED |

**Errata — nieistniejące API:**

`expect.soft.poll()` — **to API nie istnieje** w Playwright 1.60 ani 1.61.
Dostępne są `expect.soft()` i `expect.poll()` jako oddzielne, niezwiązane API.
Nie ma ich kombinacji jako `expect.soft.poll`. Wszelkie plany testowe odwołujące się
do `expect.soft.poll` muszą zostać skorygowane. Użyj:
- `expect.poll(() => asyncCondition())` — retry warunku async z timeout,
- `expect.soft(value).assertion()` — soft assertion bez przerywania testu.

**Rekomendowany krok (osobny PR/commit, nie w tym zadaniu):**

```bash
# 1. Zaktualizuj package.json
"@playwright/test": "1.61.x"

# 2. Zaktualizuj lockfile
corepack pnpm install

# 3. Smoke suite
corepack pnpm exec playwright test tests/e2e/foundation.spec.ts tests/e2e/auth-deny.spec.ts

# 4. Typecheck
corepack pnpm typecheck

# 5. Commit jako: "chore: upgrade Playwright 1.60.0 → 1.61.x"
```

---

### 23.2 Correction: Native Browser Dialogs vs Application Modals

Sekcja 6 capability matrix (pozycja `page.on('dialog')`) i sekcja 10 POM roadmap
zawierają potencjalnie mylące mapowanie. Korekta poniżej.

**Native browser dialogs (JS engine):**
- `window.alert('message')` — informacja
- `window.confirm('question')` → `true`/`false`
- `window.prompt('label', 'default')` → `string | null`
- Zdarzenie `beforeunload` → dialog "Czy na pewno chcesz opuścić stronę?"
- **Playwright API:** `page.on('dialog', dialog => dialog.accept() | dialog.dismiss())`
- Charakter: modal blokujący wątek przeglądarki, brak DOM, steruje się wyłącznie przez event.

**Application modals (Nuxt UI / DOM):**
- `ConfirmActionModal` — komponent Nuxt UI zbudowany z `UModal`
- `AuditEntryDrawer` — `USlideover` komponent
- Inne drawery i dialogi własne
- **Playwright API:** `page.getByRole('dialog')`, `getByRole('button', { name: '...' })`,
  `expect(modal).toBeVisible()`, `expect(modal).toBeHidden()`
- Charakter: elementy DOM, reagują na kliknięcia, pełna ARIA accessibility tree.

**Ostrzeżenie:** `ConfirmActionModal` to komponent DOM, **nie** native browser dialog.
Mapowanie go na `page.on('dialog')` jest błędem — event nigdy nie zostanie wyemitowany.
Testy muszą używać `getByRole('dialog')` i klikać przyciski wewnątrz komponentu.

**Kiedy używać `page.on('dialog')`:**
Tylko wtedy, gdy w kodzie aplikacji pojawi się świadome wywołanie:
- `window.confirm()` — np. "unsaved changes" guard implementowany jako native confirm,
- `window.alert()` — np. komunikat o błędzie krytycznym,
- zdarzenie `beforeunload` — wymagające akceptacji.

**Rekomendacja:** Jeśli celem jest lekcja `page.on('dialog')`, zbuduj ją jako
oddzielną funkcję systemu — **Native Dialog Lab** — z dokumentowanym uzasadnieniem:

```ts
// F-37: Native Dialog Lab (explicit lesson only)
// Wymaga świadomego dodania window.confirm() lub beforeunload w kodzie aplikacji.
// NIE jest powiązana z istniejącym ConfirmActionModal.
page.on('dialog', async dialog => {
  expect(dialog.type()).toBe('confirm')
  expect(dialog.message()).toContain('unsaved changes')
  await dialog.accept()
})
```

**Zaktualizuj capability matrix (sekcja 6):**

| Wiersz | Przed | Po |
|---|---|---|
| `page.on('dialog')` | `ConfirmActionModal (native dialogs)` | `Native JS dialog — wymaga oddzielnej lekcji (F-37)` |
| `page.dialog().accept()` | `Cancel/refund confirmation` | `Tylko dla native confirm, NIE dla UModal` |
| **nowy wiersz** | `getByRole('dialog')` | `ConfirmActionModal, UModal — używaj zamiast page.on('dialog')` |

---

### 23.3 POM Contract Rules

Poniższe zasady obowiązują przy projektowaniu i review każdego Page Object, Component
Object lub Fixture w tym repozytorium.

**Hierarchia obiektów testowych (sugerowana):**

```
App Object
  └─ Page Objects (DashboardShell, MerchantsPage, ...)
       └─ Component Objects (DataTable, ProblemDetailsCard, ...)
            └─ Workflow/Task Objects (authorizePaymentFromUi, ...)
Fixtures (auth, apiClient, dataFactory, ...)
```

**Reguły jakości:**

1. **Page Object nie ukrywa ważnego kontraktu biznesowego.** Jeśli test potrzebuje
   sprawdzić ETag — niech to będzie widoczne w teście, nie pochowane w metodzie POM.

2. **Page Object może zawierać:** `goto()`, `isLoaded()`, akcje biznesowe użytkownika
   (`createMerchant()`, `activateMerchant()`), podstawowe nawigacje.

3. **Component Object opisuje powtarzalny komponent UI:** `ProblemDetailsCard`,
   `DataTable`, `ApiDebugPanel`, `StatusBadge`. Tworzyć dopiero gdy komponent pojawia
   się w ≥ 2 testach z identycznym zachowaniem.

4. **Workflow/Task Object opisuje proces biznesowy:**
   ```ts
   // Dobre
   await paymentWorkflow.authorizeWithEtag(orderId, etag)
   // Złe: ukrywa co się dzieje
   await page.doEverything(orderId)
   ```

5. **Fixture tworzy stan testowy — nie klika UI bez potrzeby.**
   Dane tworzone przez API (`POST /api/test/seed`), nie przez serię kliknięć.

6. **Nazwy metod są domenowe, nie techniczne:**
   ```ts
   ✓ merchants.createDraftMerchant({ reference: 'REF-001' })
   ✗ merchants.clickButtonAndFillForm('REF-001')

   ✓ paymentDetail.captureEtag()
   ✗ paymentDetail.getH3TextFromThirdDiv()
   ```

7. **Test czyta się jak specyfikacja zachowania:**
   ```ts
   // Dobre — specyfikacja
   await merchantsPage.goto()
   await merchantsPage.create({ reference: 'M-001', name: 'Test' })
   await expect(merchantsPage.statusBadge('M-001')).toContainText('PENDING')

   // Złe — techniczne
   await page.goto('/admin/merchants')
   await page.locator('form input[name="reference"]').fill('M-001')
   await page.locator('button[type="submit"]').click()
   ```

8. **Nie tworzyć `DataTable` za wcześnie.** Wzorzec tabelaryczny musi pojawić się w
   ≥ 2–3 realnych testach, zanim wyciągamy generyczną abstrakcję.

9. **Nie tworzyć jednego gigantycznego POM.** `MerchantsPage` obsługuje TYLKO
   `/admin/merchants`. Szczegóły merchantów w `MerchantDetailPage`.

10. **Fixture scope:**
    - `test` scope — stan izolowany per test (page, apiClient, merchant-specific data)
    - `worker` scope — auth/storageState (drogie, raz per worker)

---

### 23.4 Test Pyramid dla tego repozytorium

Playwright testy powinny być świadomie przypisywane do jednego z czterech tierów.
**Nie wszystkie testy powinny iść przez real Keycloak i real backend.**

```
Tier 1 — Fast Mocked UI (większość coverage)
Tier 2 — Contract-Backed UI (integracyjne frontend)
Tier 3 — Live Local Stack (smoke/critical journeys)
Tier 4 — Hybrid UI + API (lifecycle, audit, security)
```

**Tier 1 — Fast Mocked UI:**
- Mockowane Nuxt `server/api/**` routes przez `page.route()`
- Brak real Keycloak, brak real backend, brak real DB
- Testuje: UI states (loading/empty/error), walidację formularzy, RBAC rendering
- Czas: < 5s per test
- Przykład: `merchant-lifecycle.spec.ts`, `payment-order-create.spec.ts`
- Auth: mockowany przez `page.route('**/api/_auth/session', ...)`

**Tier 2 — Contract-Backed UI:**
- Frontend działa realnie, backend kontrolowany przez mock lub seeded API
- Testuje: Zod contract validation, header capture, ProblemDetails rendering
- Auth: storage state z Keycloak (lub pełny mock session)
- Przykład: ETag display po API call, X-Correlation-ID w HeaderKeyValuePanel

**Tier 3 — Live Local Stack (smoke):**
- Real PostgreSQL + real Keycloak + real Spring Boot + real Nuxt proxy
- Tylko krytyczne ścieżki: login, create merchant, create payment order, lifecycle
- Ilość: < 10 testów, CI smoke gate
- Auth: storage state z prawdziwego Keycloak (PLAYWRIGHT_USE_REAL_KEYCLOAK=true)
- Czas: 60–120s per suite

**Tier 4 — Hybrid UI + API:**
- Setup przez `APIRequestContext` (`POST /api/test/seed`)
- Akcja przez UI (kliknięcia, formularze)
- Asercja przez API (`GET /api/payment-orders/{id}`) LUB przez UI (badge, panel)
- Testuje: audit trail po UI actions, ETag po lifecycle, tenant isolation z UI trigger
- Oznaczenie: `@hybrid`, wymaga backendu + testing flag

**Ostrzeżenie:** Tier 3 i Tier 4 wymagają działającego localstack (PostgreSQL, Keycloak,
Spring Boot). Nie uruchamiać w CI bez strategii infrastruktury. Zacząć od Tier 1 i Tier 2.

---

### 23.5 Multi-role Auth Strategy: Selective Assignment

**Zasada:** Nie mnożyć automatycznie całej suite przez 5 ról.

Strategia 5 projektów Playwright nie oznacza, że każdy test działa dla każdej roli.
Cross-product 5 ról × N testów = O(5N) testów bez proporcjonalnej wartości.

**Właściwe podejście:**

```ts
// playwright.config.ts
projects: [
  // Tier 1 smoke — każda rola ma własny projekt
  { name: 'platform-admin', use: { storageState: '.auth/platform-admin.json' } },
  { name: 'tenant-admin',   use: { storageState: '.auth/tenant-admin.json' } },
  { name: 'merchant-manager', use: { storageState: '.auth/merchant-manager.json' } },
  { name: 'support-agent', use: { storageState: '.auth/support-agent.json' } },
  { name: 'readonly-user', use: { storageState: '.auth/readonly-user.json' } },
]

// W teście: selektywne przypisanie przez annotation lub project filter
test('platform admin can create merchant', { annotation: { type: '@platform-admin' } }, async ({ page }) => { ... })
test('merchant manager can create payment order', { annotation: { type: '@merchant-manager' } }, async ({ page }) => { ... })
```

**Tagi testów (sugerowane):**
- `@platform-admin` — test musi działać jako PLATFORM_ADMIN
- `@tenant-admin` — test sprawdza TENANT_ADMIN scope
- `@merchant-manager` — test sprawdza MERCHANT_MANAGER access
- `@support-agent` — test sprawdza SUPPORT_AGENT read-only + risk queue
- `@readonly-user` — test sprawdza ograniczenia READ_ONLY_USER
- `@any-role` — test jest role-agnostic (np. Error Lab 400)
- `@mocked` — Tier 1, bez real BE
- `@live` — Tier 3, wymaga localstack
- `@hybrid` — Tier 4, APIRequestContext + UI
- `@rbac-matrix` — dedykowane testy macierzy uprawnień

**RBAC matrix coverage strategy:**
Macierz RBAC (sekcja 9) testuje **reprezentatywne zachowania**, nie permutację pełną.
Dla każdej funkcji biznesowej wystarczają testy:
1. Rola mająca uprawnienie → asercja dostępu (happy path)
2. Rola bez uprawnienia → `toBeHidden()` lub `toBeDisabled()` lub `403 ProblemDetailsCard`

---

### 23.6 APIRequestContext Auth Strategy

`APIRequestContext` w testach Playwright może działać w trzech trybach. Każdy ma inne
implikacje bezpieczeństwa i dostępności.

**Tryb A — Test-only endpoints (reset/seed):**
```ts
// Wywołuje POST /api/test/reset i POST /api/test/seed
// Wymaga: app.testing.enabled=true w backendzie
// NIE wymaga tokena (endpointy są permitAll za filtrem feature flag)
// Używany w globalSetup lub beforeAll fixture

const request = await playwright.request.newContext({
  baseURL: 'http://localhost:8080',
})
await request.post('/api/test/reset')
await request.post('/api/test/seed')
```

**Tryb B — Backend API z role token:**
```ts
// Używany dla API-driven setup: CREATE merchant, payment order przed testem UI
// Token pozyskiwany przez OAuth Client Credentials lub password flow w setupie
// Token NIGDY nie pochodzi z browser DOM, localStorage, sessionStorage

const token = await acquireTestToken('platform-admin')  // custom helper
const request = await playwright.request.newContext({
  baseURL: 'http://localhost:8080',
  extraHTTPHeaders: { 'Authorization': `Bearer ${token}` },
})
await request.post('/api/merchants', { data: { merchantReference: 'SETUP-001', ... } })
```

**Tryb C — Nuxt server proxy (browser context):**
```ts
// Browser posiada sealed session cookie (httpOnly, server-side)
// Token pozostaje po stronie serwera Nuxt — nigdy w browser DOM
// Dobre dla: UI smoke weryfikujący, że proxy działa
// Nie nadaje się do: bezpośredniego setupu stanu backendu
// page.request() dziedziczy cookie z context storageState

const response = await page.request.get('/api/merchants')  // → Nuxt proxy → backend
```

**Zasada:** Nie projektować testów zakładających, że access token jest w localStorage,
sessionStorage, cookies klienckich ani window obiekcie przeglądarki. Architektura
aplikacji celowo tego unika (`nuxt-auth-utils` sealed session po stronie serwera).
Testy, które próbują odczytać token z browser storage, testują **wyciek** — i powinny
failować, nie sukcedować.

---

### 23.7 Playwright Observability and Failure Forensics

**Bieżąca konfiguracja (`playwright.config.ts`):**
```ts
trace: 'on-first-retry'     // ✓ trace aktywny
// Brak: screenshot, video, testInfo.attach policy
```

**Rekomendowana konfiguracja docelowa (po upgrade do 1.61):**
```ts
use: {
  trace: 'on-first-retry',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure-and-retries',  // 🔒 WYMAGA 1.61
  // lub dla 1.60:
  // video: 'retain-on-failure',
},
```

**Kompletna polityka failure forensics:**

| Artefakt | Tryb | Uwaga |
|---|---|---|
| Trace | `on-first-retry` | ✓ już skonfigurowane |
| Screenshot | `only-on-failure` | Dodać do config |
| Video | `retain-on-failure` (1.60) / `retain-on-failure-and-retries` (1.61) | Dodać do config |
| `testInfo.attach()` | Per test, response body | Maskować Authorization |
| Console collector | `page.on('console')` | W base fixture |
| Page error collector | `page.on('pageerror')` | W base fixture |
| Network request log | `page.on('request')` | Opcjonalnie, maskować tokeny |
| Header masking | Polityka: Authorization → `Bearer ••••` | Obowiązkowe w attachmentach |

**Base fixture pattern dla observability:**
```ts
export const test = base.extend({
  page: async ({ page }, use) => {
    const consoleMessages: string[] = []
    const pageErrors: string[] = []

    page.on('console', msg => {
      const text = msg.text()
      // Nie loguj tokenów w żadnej postaci
      if (/eyJ[A-Za-z0-9._-]{10,}/.test(text)) {
        throw new Error('TOKEN LEAK IN CONSOLE: ' + text.slice(0, 40))
      }
      consoleMessages.push(`[${msg.type()}] ${text}`)
    })

    page.on('pageerror', err => pageErrors.push(err.message))

    await use(page)

    if (consoleMessages.length > 0) {
      await test.info().attach('console-log', {
        body: consoleMessages.join('\n'),
        contentType: 'text/plain',
      })
    }
  }
})
```

**Zasada: popraw diagnostykę zanim suite urośnie.**
Narzędzia observability dodaje się raz, na początku — nie po fakcie, gdy testy zaczną
failować w niezrozumiały sposób.

---

### 23.8 F-36: Token Leakage Guard (nowa funkcja roadmapy)

Uzupełnienie roadmapy o dodatkową funkcję edukacyjną o wysokiej wartości security/SDET.

**F-36: Token Leakage Guard**

| | |
|---|---|
| Opis | Zestaw guard assertions sprawdzających, że access token (Bearer JWT) **nigdy** nie pojawia się w żadnym miejscu dostępnym przez przeglądarkę. |
| Realizm | Security requirement dla każdego systemu OAuth; regulatory audit trail dla PSP. |
| Wartość SDET | Łączy security testing, UI testing, observability i architekturę auth w jednej lekcji. |
| Sprawdzane powierzchnie | DOM text, localStorage, sessionStorage, console.log, page errors, ApiDebugPanel, HeaderKeyValuePanel, RawJsonViewer, testInfo attachments, screenshots. |
| Playwright concepts | `page.on('console')`, `page.on('pageerror')`, `page.evaluate(() => document.body.innerText)`, `page.localStorage.items()` (🔒 wymaga 1.61), `testInfo.attach` masking policy. |
| Backend endpointy | Brak nowych. Używa istniejących API. |
| Frontend | Brak nowych komponentów. Testy weryfikują istniejącą maskowanie w `HeaderKeyValuePanel`. |
| Flyway | Brak. |
| Poziom | expert |
| MVP | NIE (Faza 3, po upgrade do 1.61 dla localStorage) |

**Przykładowe asercje:**
```ts
// JWT pattern (uproszczony): header.payload.signature = eyJ...
const JWT_PATTERN = /eyJ[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+/

// 1. DOM scan
const bodyText = await page.evaluate(() => document.body.innerText)
expect(bodyText).not.toMatch(JWT_PATTERN)

// 2. Console scan (w base fixture)
page.on('console', msg => expect(msg.text()).not.toMatch(JWT_PATTERN))

// 3. ApiDebugPanel masking assertion
const authHeader = page.getByTestId('http-headers-panel').getByText('Authorization')
await expect(authHeader.locator('..').getByText(/Bearer/)).toContainText('Bearer ••••••••')

// 4. localStorage scan (tylko po upgrade do Playwright 1.61)
// const items = await page.localStorage.items()
// for (const [key, value] of Object.entries(items)) {
//   expect(String(value)).not.toMatch(JWT_PATTERN)
// }
```

**Dodaj do gap analysis (sekcja 15):**

| Capability | Stan | Co potrzeba | Realistyczna? | Warta budowania? |
|---|---|---|---|---|
| Token leak DOM scan | NOT COVERED | F-36 base fixture | TAK | TAK (security) |
| localStorage token scan | NOT COVERED | F-36 + Playwright 1.61 | TAK po upgrade | TAK |
| Console token guard | NOT COVERED | F-36 base fixture | TAK | TAK |
| Auth header masking test | NOT COVERED | F-36 HeaderKeyValuePanel | TAK | TAK |

---

### 23.9 RBAC UI Patterns: Hidden vs Disabled vs Visible-403

Każda nowa funkcja RBAC musi świadomie wybrać jeden z trzech wzorców. Nie mieszaj ich
bez dokumentacji decyzji.

**Pattern 1 — Hidden action (element niewidoczny w DOM):**
```ts
// Użytkownik nie powinien wiedzieć, że akcja istnieje
// Wymaga: v-if / conditional rendering (nie v-show!)
await expect(page.getByRole('button', { name: 'Create Merchant' })).toBeHidden()
// lub: .not.toBeVisible() gdy element jest w DOM ale ukryty
```

Kiedy używać: akcje administracyjne niedostępne dla ról operacyjnych
(np. MERCHANT_MANAGER nie widzi "Create Merchant").

**Pattern 2 — Disabled action (element widoczny, interakcja zablokowana):**
```ts
// Użytkownik widzi, że coś istnieje, ale nie może tego użyć
// Uzasadnienie: UX feedback (np. "Authorize" dla CAPTURED order)
await expect(page.getByRole('button', { name: 'Authorize' })).toBeDisabled()
await expect(page.getByRole('button', { name: 'Authorize' })).toHaveAttribute('aria-disabled', 'true')
```

Kiedy używać: lifecycle buttons dla niemożliwych przejść statusów;
akcje niedostępne z powodu stanu zasobu (merchant SUSPENDED, order EXPIRED).

**Pattern 3 — Visible action leading to 403 (edukacyjny / Error Lab):**
```ts
// Użytkownik widzi trigger, backend odrzuca — celowe demo błędu
// Stosowany w Error Lab i Permission Demo scenarios
await page.getByTestId('error-lab-trigger-403').click()
await expect(page.getByTestId('http-status-badge')).toContainText('403')
await expect(page.getByTestId('problem-details-card')).toBeVisible()
await expect(page.getByTestId('problem-details-card')).toContainText('Forbidden')
// Authorization header MUSI być zamaskowany
await expect(page.getByTestId('http-headers-panel')).not.toContainText(/Bearer eyJ/)
```

Kiedy używać: Error Lab, Permission Demo, educational flows gdzie chcemy pokazać
backend enforcement.

**Zasada review:** W PR review każdej funkcji RBAC zadaj pytanie:
*„Który z 3 wzorców tu zastosowaliśmy i czy test to weryfikuje?"*

---

### 23.10 Payment Challenge Simulator (zamiast real 3DS)

Repozytorium jawnie wyklucza 3DS, PAN i PCI. Edukacyjną alternatywą jest:

**Payment Challenge Simulator** — bez żadnych realnych kart ani danych wrażliwych.

Scenariusze do zaimplementowania (żaden nie wymaga PCI compliance):

| Scenariusz | Opis | Playwright concept | Trudność |
|---|---|---|---|
| OTP Challenge | Modal z 6-cyfrowym kodem (mock) | `fill`, `getByLabel`, timer mock | medium |
| Bank Redirect | Nowa karta z mock "bank login page" | `context.waitForPage()` | senior |
| iframe Challenge | Sandboxed iframe z mock formularzem | `frameLocator()` | expert |
| Failed Challenge | Wpisanie złego kodu → error state | `ProblemDetailsCard`, retry | medium |
| Expired Challenge | Challenge timeout → status EXPIRED | `page.clock.fastForward()` | expert |
| User Cancelled | Zamknięcie modala/karty → status CANCELLED | `context.close()`, polling | senior |
| Accessibility | Dostępność klawiszem Tab przez OTP input | `page.keyboard.press('Tab')` | senior |
| Mobile Challenge | Iframe na mobile viewport | `devices['iPhone 14']` + `frameLocator` | expert |

**Klasyfikacja:** F-37 (Payment Challenge Simulator)
Zastępuje odrzuconą opcję "real 3DS". Bezpieczna edukacyjna alternatywa.
Nie wymaga PSP, PAN, CVV, Keycloak FIDO2, ani czegokolwiek z zakresu PCI DSS.

**Dodaj do roadmapy Fazy 3.**

---

### 23.11 MCP Policy: Exploration, Not Coverage

MCP (`mcp__playwright__browser_*`) jest narzędziem eksploracyjnym, nie zastępstwem
testów w repozytorium.

**Dozwolone zastosowania MCP Playwright:**
- Eksploracja działającej aplikacji: `browser_navigate`, `browser_snapshot`
- Zbieranie ARIA tree jako podstawy dla `toMatchAriaSnapshot()`: `browser_snapshot`
- Diagnoza console/network bez pisania testu: `browser_console_messages`, `browser_network_requests`
- Walidacja locatora przed użyciem w teście: `browser_click` + `browser_snapshot`
- Generowanie hipotez: "czy ten element ma właściwy role/label?"
- Smoke manual: `browser_navigate` → `browser_take_screenshot`

**Niedozwolone zastosowania:**
- Używanie MCP jako substytutu testów Playwright w CI
- Liczenie MCP interactions jako "coverage"
- Uruchamianie MCP bez działającego localstack (Tier 3+)

**Zasada przepływu odkrycia:**
```
MCP exploration → znajdź scenariusz/locator/problem
     ↓
Dokument: "MCP odkryło, że element X ma label Y i powinien być testowany tak Z"
     ↓
Dodaj do test design backlog lub bezpośrednio do kodu testu (oddzielne zadanie)
```

MCP to narzędzie **research**, nie narzędzie **implementation**. Wyniki MCP
powinny trafić do dokumentacji, test case design lub kodu Playwright w późniejszym
oddzielnym zadaniu.

---

### 23.12 Revised First 10 Steps

Zaktualizowana lista pierwszych 10 kroków. Zastępuje sekcję 18 (oryginalna lista).

| # | Krok | Typ | Zależy od |
|---|---|---|---|
| 1 | **Zamrozić roadmapę i dodać Expert Review Addendum** | dokumentacja | — |
| 2 | **Upgrade Playwright 1.60 → 1.61 (osobny PR)** | `package.json` + lockfile | krok 1 |
| 3 | **Po upgrade: smoke suite** `foundation.spec.ts` + `auth-deny.spec.ts` + typecheck | weryfikacja | krok 2 |
| 4 | **Poprawić diagnostykę Playwright**: `screenshot: 'only-on-failure'` + `video: 'retain-on-failure'` | `playwright.config.ts` | krok 3 |
| 5 | **Usunąć `waitForTimeout`** — zidentyfikowane w `payment-order-create.spec.ts:16` — zastąpić `waitForResponse` lub `expect.poll` | test fix | krok 4 |
| 6 | **Zbudować base fixture** z console/pageerror guard i maskowaniem tokenów | `tests/fixtures/base.fixture.ts` | krok 5 |
| 7 | **Dodać `APIRequestContext` fixture** tylko dla reset/seed (Tryb A) | `tests/fixtures/api.fixture.ts` | krok 6 |
| 8 | **Multi-role auth setup** — 5 projektów Playwright, selektywne przypisanie testów (nie cross-product) | `playwright.config.ts` + `auth.setup.ts` | krok 7 |
| 9 | **Minimalny POM**: `DashboardShell`, `ErrorLabPage`, `ProblemDetailsCard` — tylko po realnych użyciach | `tests/pages/` + `tests/components/` | krok 8 |
| 10 | **Rozwijać `payment-operations-dashboard` spec** + CSV export + upload + iframe + clock po ugruntowaniu fundamentów | implementacja | krok 9 |

---

### 23.13 Extended Playwright DoD

Rozszerzenie istniejącej sekcji 19 (Definition of Done) o punkty specyficzne dla
jakości testów Playwright:

**Punkty dodatkowe dla każdego testu Playwright:**

- [ ] Test ma jeden jasny cel (jedno zachowanie, jedna weryfikacja)
- [ ] Test z ≥ 3 krokami używa `test.step('...')` dla czytelności trace
- [ ] **Brak `page.waitForTimeout()`** — zastąp `page.waitForResponse()`, `expect.poll()` lub auto-waiting assertion
- [ ] Preferencja locatorów: `getByRole` / `getByLabel` > `getByTestId` > `locator(CSS)` — CSS tylko jako ostateczność
- [ ] Każdy mockowany route ma nazwany kontrakt (docstring lub komentarz z opisem response)
- [ ] Test nie loguje ani nie eksponuje tokenów (konsola, DOM, attachment)
- [ ] `testInfo.attach()` maskuje Authorization header (`Bearer ••••••••`)
- [ ] Test działa **pojedynczo** i w pełnej suite (brak order-dependency)
- [ ] Test nie zależy od danych tworzonych przez poprzedni test UI
- [ ] Przy failure generuje trace/screenshot/video zgodnie z aktualnym configiem
- [ ] Dla Tier 3/4 (real backend): dane tworzone przez API (`APIRequestContext`), nie przez UI poprzedniego testu
- [ ] RBAC test jawnie deklaruje którego z 3 wzorców używa (hidden/disabled/visible-403)
- [ ] Attachment screenshot/video nie zawiera pełnego wartości tokena

---

### 23.14 Errata Table

Kompletna tabela korekt do sekcji 1–22 raportu:

| Obszar | Sekcja | Oryginalna sugestia | Korekta | Priorytet |
|---|---|---|---|---|
| `expect.soft.poll` | 6 (matrix), 11 | Planowane jako API Playwright | **Nie istnieje** — użyj `expect.soft()` LUB `expect.poll()` oddzielnie | KRYTYCZNY |
| `ConfirmActionModal` → `page.on('dialog')` | 6 (matrix), 11.1 M-16 | Mapowanie na native dialog | `ConfirmActionModal` = DOM component → `getByRole('dialog')` + button click | WYSOKI |
| Playwright 1.61 APIs | 6, 11, 18 | Planowane w roadmapie | Wszystkie 1.61 API są **gated** do czasu upgrade (`1.60.0` jest current) | WYSOKI |
| `video: 'retain-on-failure-and-retries'` | 23.7 | — | Nowy tryb video **dostępny od 1.61** — dla 1.60 używaj `'retain-on-failure'` | WYSOKI |
| `video.show.actions` / `video.show.test` | 23.7 | — | Annotacje w video — **tylko 1.61+** | WYSOKI |
| Multi-role: 5 ról × cała suite | 9, 10, 18 | 5 projektów → 5× każdy test | Selektywne przypisanie — nie cross-product; tagi `@platform-admin` etc. | ŚREDNI |
| APIRequestContext auth | 6, 10.2 | Proste `baseURL` + reset/seed | 3 tryby: test endpoints / role token / Nuxt proxy; strategia tokena inna dla każdego | ŚREDNI |
| POM: pełne drzewo z góry | 10.1 | Cały POM tree jako cel | Zaczynać od minimalnego POM po ≥ 2 realnych użyciach; nie wyprzedzać systemu | ŚREDNI |
| 3DS odrzucone | 16 | Bez alternatywy | Zastąp **Payment Challenge Simulator** (F-37) — bezpieczna edukacyjna alternatywa | NISKI |
| MCP jako coverage | 12 | Eksploracja i coverage | MCP = **exploration only**, wyniki przenosić do dokumentacji/testów ręcznie | NISKI |
| Token leak prevention | 6, 10, 19 | Wzmianka w security | Dodano **F-36 Token Leakage Guard** jako oddzielną funkcję + base fixture pattern | NISKI |

---

### 23.15 Verified Context7 Sources (dla tego addendum)

Poniższe API zostały zweryfikowane przez Context7 (`/microsoft/playwright`, wersja 1.61):

| API | Status | Źródło |
|---|---|---|
| `page.localStorage.setItem/getItem/items()` | ✅ Potwierdzone w 1.61 | release-notes-js.md |
| `page.sessionStorage.setItem/getItem/items()` | ✅ Potwierdzone w 1.61 | release-notes-js.md |
| `browserContext.credentials` (WebAuthn) | ✅ Potwierdzone w 1.61 | release-notes-python.md |
| `video: 'on-all-retries'` | ✅ Potwierdzone jako nowy tryb | test-api/class-testoptions.md |
| `video: 'retain-on-first-failure'` | ✅ Potwierdzone jako nowy tryb | test-api/class-testoptions.md |
| `video: 'retain-on-failure-and-retries'` | ✅ Potwierdzone jako nowy tryb | test-api/class-testoptions.md |
| `video.show.actions / video.show.test` | ✅ Potwierdzone jako nowa opcja | docs/src/videos.md |
| `expect.soft.poll()` | ❌ **NIE ISTNIEJE** w 1.60 ani 1.61 | Brak w dokumentacji |
| `page.on('dialog')` | ✅ Istnieje, ale dla native JS dialogs only | release-notes-js.md |

*Addendum wygenerowano: 2026-06-26. Weryfikacja wersji Playwright: `1.60.0` (package.json:28).*
