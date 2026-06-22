# Payment Quality Engineering Lab — Analiza i Strategia Rozwoju pod kątem nauki Playwright/SDET

> Dokument strategiczny. Powstał na podstawie faktycznej inspekcji repozytorium (kod, konfiguracje,
> realm Keycloak, testy). Tam, gdzie czegoś nie dało się potwierdzić, oznaczono **UNKNOWN** wraz ze
> sposobem weryfikacji. Zgodnie z założeniem: ten dokument nie implementuje funkcji ani testów
> Playwright — opisuje kierunek i plan.

**Korekta wersji na wstępie:** w żądaniu wskazano **Playwright 1.61** i **Nuxt 4.4.8**, natomiast w
repo faktycznie jest **Playwright 1.60.0** (`package.json`) i **Nuxt 4.4.6**. Drobna rozbieżność, ale
istotna przy planowaniu (dostępność konkretnych API Playwright). Zalecane ujednolicenie wersji przed
startem lekcji.

---

## 1. Executive Summary

Repozytorium to **dojrzały, wąski wertykał**: backoffice operacji płatniczych (Merchants + Payment
Orders + lifecycle) zbudowany bardzo solidnie pod kątem **kontraktu HTTP** (ETag/If-Match,
Idempotency-Key, problem+json, Vary, X-Correlation-ID) i **bezpieczeństwa opartego o JWT/authorities**.
Backend (Spring Boot 4 / Modulith / PostgreSQL 18) jest mocno przetestowany (REST Assured, macierze
bezpieczeństwa, testy architektury). Frontend (Nuxt 4 + Nuxt UI Dashboard) ma już warstwę „HTTP
learning” (Error Lab, panele nagłówków, raw JSON).

**Z perspektywy nauki Playwright to świetny fundament, ale wąski.** Aplikacja uczy doskonale jednego
obszaru (kontrakt REST + protokół), a prawie wcale całych klas scenariuszy UI/SDET: **multi-role RBAC
w UI, zarządzanie użytkownikami, audyt, file upload/download, bulk actions, date pickers, command
palette, notyfikacje, responsywność/mobile, multi-project Playwright, POM/fixtures, deterministyczny
seed pod testy E2E.**

**Największa pojedyncza luka:** testowalność wielorolowa. Realm Keycloak ma tylko drobnoziarniste
`authorities`, brak nazwanych ról biznesowych (PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER,
SUPPORT_AGENT, READ_ONLY_USER), a `auth.setup.ts` generuje **jeden** pusty storage state. Bez tego nie
da się nauczyć macierzy uprawnień, permission-based rendering ani auth bypass — rdzenia pracy Senior
SDET.

**Rekomendacja kierunkowa:** nie przepisywać aplikacji. Rozszerzyć ją w stronę realistycznego
**multi-tenant payment backoffice** przez dołożenie warstwy **IAM (tenants/users/roles) + Audit Log +
Operacje plikowe (import/export) + Notifications**, a całość oprzeć na **5 nazwanych rolach**. To
odblokowuje ~90% brakujących technik Playwright bez psucia istniejącego, dobrze przetestowanego rdzenia.

---

## 2. Current Repository Analysis

### 2.1 Stos technologiczny (zweryfikowany)

| Warstwa | Technologia | Źródło |
|---|---|---|
| Backend język | Java 25 | steering `tech.md`, AGENTS.md |
| Backend framework | Spring Boot 4.0.x / Spring Framework 7, Spring Modulith 2.0.6 | steering + struktura modułów |
| Build | Maven Wrapper 3.9.11 (`apps/backend/mvnw`, `pom.xml`) | inspekcja |
| Baza | PostgreSQL 18, Flyway (V1–V5), JPA `ddl-auto: validate` | `resources/db/migration/**` |
| Security | Spring Security JWT resource server, `KeycloakRealmRoleConverter` | `shared/security/SecurityConfig.java` |
| Frontend | Nuxt **4.4.6**, @nuxt/ui **4.7.1**, TypeScript **6.0.3** | `apps/frontend/package.json` |
| State / walidacja | Pinia 3.0.4, Zod 4.4.3 | `package.json` |
| Auth FE | nuxt-auth-utils 0.5.0 (OIDC, sesja serwerowa) | `package.json` |
| Testy FE | Playwright **1.60.0**, Vitest 3, fast-check 3.22, @nuxt/test-utils | `package.json` |
| Pakiety | pnpm 10.33.4 (corepack) | `package.json` |

**Uwaga o strukturze monorepo:** to **nie** jest pnpm workspace. `apps/frontend` jest samodzielnym
pakietem pnpm, a `apps/backend` osobnym projektem Maven. Brak głównego `pnpm-workspace.yaml` w korzeniu
(UNKNOWN czy zamierzone — weryfikacja: `file_search` na `pnpm-workspace.yaml` w korzeniu; nie wykryto
w drzewie).

### 2.2 Backend — struktura

```
lab/paymentquality/
├── foundation/status/        # GET /api/status (public)
├── merchant/                 # Modulith: public API + internal/{application,domain,web}
│   ├── MerchantPaymentEligibility(Service)   # public inter-module API
│   └── internal/...
├── payment/                  # Modulith: internal/{application,domain,web}
└── shared/
    ├── security/             # SecurityConfig, KeycloakRealmRoleConverter
    └── web/                  # CorrelationIdFilter, ApiRequestRejectionFilter
```

- **Authorities (faktyczne, z `SecurityConfig`)**: `platform:merchants:create|read|update-status`,
  `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:lifecycle`,
  `platform:payments:read|lifecycle|audit`.
- **Kontrakt HTTP**: dojrzały — 201+Location, ETag/If-Match (412/428), Idempotency-Key (409),
  problem+json dla 400/401/403/404/406/409/412/415/422/428, Vary, Cache-Control, X-Correlation-ID,
  OPTIONS/HEAD.
- **Testowalność backendu: bardzo wysoka.** REST Assured (kontraktowe + negatywne), `security/`
  (macierze autoryzacji), `architecture/ModulithArchitectureTest`, testy domenowe value objects,
  `PaymentOrderIdempotencyConcurrencyTest`, RestKit + Testcontainers.

### 2.3 Frontend — struktura

- **Pages**: `index.vue` (Overview), `login.vue`, `error-lab.vue`, `admin/merchants/index.vue`,
  `admin/merchants/[merchantId]/payments/{index,new,[paymentOrderId]}.vue`.
- **Components**: `merchant/*`, `payment/*`, oraz bogaty zestaw `shared/*` (ApiDebugPanel,
  HeaderKeyValuePanel, HttpStatusBadge, ProblemDetailsCard, RawJsonViewer, EtagDisplay, IfMatchInput,
  IdempotencyKeyInput, LoadingState/EmptyStateCard/ErrorState, ConfirmActionModal, BusinessStatusBadge,
  MerchantStatusCard, PaymentOrderLifecycleActions).
- **Composables**: `useApiClient` (header-aware `$fetch.raw`), `useMerchantsApi`, `usePaymentOrdersApi`,
  `usePaymentLifecycleApi`.
- **Stores**: `auth`, `payment-orders` (versionMarker/history/lifecycle), `app-shell`.
- **Schemas**: `merchant`, `payment-order`, `problem-details`, `app-shell`.
- **Middleware**: `auth.global.ts` (route guard).
- **Proxy**: `server/api/**` + `server/utils/backendApi.ts` (token server-side, forward nagłówków).
- **Dostępność/testowalność UI: dobra w obrębie istniejących ekranów** — badge'y rozróżnialne bez
  koloru, `UFormField` z labelami, focus trap w modal/slideover, stabilne `data-testid`. Solidna baza
  locator-friendly.

### 2.4 Bezpieczeństwo (faktyczny stan)

- Resource server JWT; role realmu → authorities. **Brak nazwanych ról biznesowych.** Jest claim
  `merchant_id` (mapper w realmie), ale część użytkowników ma `PLACEHOLDER_MERCHANT_ID` i jest
  `enabled: false`.
- `auth.setup.ts`: w trybie domyślnym zapisuje **pusty** storage state; realny login przez Keycloak
  tylko przy `PLAYWRIGHT_USE_REAL_KEYCLOAK=true`. Obecne testy E2E nie ćwiczą realnego logowania ani
  realnych ról.

### 2.5 Testy — stan i pokrycie

- **Backend**: bardzo szerokie (kontrakt, bezpieczeństwo, architektura, domena,
  idempotencja/współbieżność).
- **Frontend Vitest/fast-check**: property-based testy (status mapping, masking, if-match round-trip,
  history ordering, raw-json round-trip, gating).
- **Playwright E2E**: `auth-deny`, `foundation`, `merchant-create/feedback/lifecycle`,
  `payment-order-auth-deny/create/read`, `payment-orders-panel`. **Płaskie spec-y, bez POM, bez
  fixtures, jeden projekt `chromium`, jeden role storage state, `fullyParallel: false`.**

### 2.6 Stan istniejącego spec-a

`.kiro/specs/payment-operations-dashboard` jest praktycznie ukończony (większość zadań `[x]`; zadania
Playwright świadomie pominięte „per user decision”). Kolejny kierunek to **nowe specs**, a nie
kontynuacja tego.

---

## 3. Existing Feature Map

**Current features**
- Merchant Registry: create/list/get/activate/suspend (status PENDING→ACTIVE→SUSPENDED).
- Payment Orders: create (Idempotency-Key, Location, ETag), read (ETag), list (filtry+paginacja+sort),
  summary.
- Lifecycle: authorize/capture/cancel/refund + PATCH metadata + history; ETag/If-Match + idempotencja.
- Error Lab: 9 scenariuszy błędów (400/401/403/404/406/409/412/415/428).
- HTTP learning panels (nagłówki, problem+json, raw JSON, debug).

**Current UI flows**: Login → Overview → Merchants (CRUD + activate/suspend) → Payment Orders
(lista/filtry/paginacja) → Detail (taby Business/HTTP/Raw/History + lifecycle drawer + confirm modal)
→ Error Lab.

**Current API flows**: wszystko przez proxy Nuxt (`server/api/**`) z server-side token; backend
egzekwuje authorities + ownership (`merchant_id`).

**Major gaps**: brak IAM/użytkowników/ról nazwanych, brak audytu globalnego, brak operacji plikowych,
brak bulk actions, brak notyfikacji, brak date-pickerów/combobox/command palette, brak multi-role
storage states, brak multi-project (mobile/cross-browser), brak POM/fixtures, brak deterministycznego
seeda E2E.

---

## 4. Biggest Learning Gaps (z oceną wartości)

Format: **luka → dlaczego ważna dla SDET → jaka funkcja ją wprowadza → kiedy**.

**High learning value (dodać teraz):**
1. **Nazwane role + multi-role storage state** → fundament RBAC/macierzy uprawnień, permission-based
   rendering, auth bypass. Funkcja: warstwa IAM (5 ról) + setup project per rola. → **Teraz.**
2. **User Management (CRUD + role assignment)** → formularze, modale, walidacja FE/BE, 403 vs 401,
   admin-only. Funkcja: `/admin/users`. → **Teraz.**
3. **Audit Log Dashboard** → tabela z filtrami/paginacją/datami, deep-linki, weryfikacja śladu akcji.
   Funkcja: `/admin/audit`. → **Teraz.**
4. **Deterministyczny seed + reset danych dla E2E** → test isolation, API-driven setup, data lifecycle.
   Funkcja: profil `seed`/skrypt + (opcjonalnie) test-only endpoint za feature flagą. → **Teraz.**

**Medium learning value (dodać później):**
5. **File import (upload CSV) + export (download)** → `setInputFiles`, download events, progres/stany
   błędów. → **Później.**
6. **Bulk actions na tabeli** → zaznaczanie wierszy, mouse/keyboard, optimistic UI, batch errors.
   → **Później.**
7. **Notifications center** → drawer/popover, badge licznika, stale data, oznaczanie jako przeczytane.
   → **Później.**
8. **Settings/Profile + „unsaved changes” guard** → `beforeunload`/route leave, dirty state.
   → **Później.**
9. **Responsywność/mobile + multi-project Playwright** → viewport matrix, sidebar collapse.
   → **Później.**

**Low learning value:**
10. **Command palette** (Cmd+K) → ładny do nauki keyboard nav, ale wąski; jako dodatek do istniejącego
    `UDashboardSearch`. → **Później/opcjonalnie.**
11. **System Health page** → low; częściowo pokryte przez `/api/status`. → **Postpone.**

**Postpone / Not recommended dla tej aplikacji:**
- **WebSocket/SSE realtime**, **background jobs/scheduler**, **rate limiting (429)**, **API versioning**
  → ciekawe, ale drogie względem wartości; dołożyć tylko punktowo (np. 429 jako pojedynczy scenariusz
  w Error Lab). → **Postpone.**
- **Drag-and-drop** jako duży feature → niski zwrot; ewentualnie mały „reorder” w jednym miejscu.
  → **Postpone.**
- **Real PSP / Kafka / settlement / KYC / 3DS** → poza celem nauki i jawnie w Non-Goals. → **Skip.**

---

## 5. Missing Modern Application Topics Review

**Frontend/UI**
- SSR vs CSR / hydration: częściowo (steering: `/admin/**` CSR). Brak ekranu *SSR-first* do kontrastu
  hydration. → dodać 1 publiczny SSR widok (np. read-only status/landing) — **Medium**.
- Layout switching, protected/role-specific pages: słabe (jeden layout, brak ról) → **High** (IAM).
- Loading/empty/error/disabled/optimistic/stale: loading/empty/error są; **optimistic UI i stale data**
  prawie nieobecne → **Medium** (bulk actions, notifications).
- Date pickers, combobox, command palette, file upload/download, drag-drop, toasts: toasts są; reszta
  brak → **Medium**.
- URL query params / deep links / back-forward / unsaved-changes: filtry już używają query (dobrze);
  deep-linki audytu i guard „unsaved” → **Medium**.
- Accessibility/ARIA/semantic: dobra baza, do utrwalenia w nowych ekranach → **High** (utrzymać standard).

**Backend/API**
- REST modeling/DTO/validation/problem+json/pagination/sorting/filtering/idempotency/optimistic
  locking/transactions/constraints: **bardzo mocne**.
- Audit logging (globalny, przeszukiwalny): brak → **High**.
- Import/export: brak → **Medium**.
- Background jobs/scheduler/rate limiting/versioning/OpenAPI: brak; OpenAPI warto rozważyć dla nauki
  contract-checks → **Medium (OpenAPI)**, reszta **Postpone**.

**Security**
- Keycloak login flow w testach, JWT claims, RBAC, role-based rendering, 401 vs 403, tenant isolation:
  401/403 i authorities są; **realny login, nazwane role, tenant isolation, role-based rendering** brak
  → **High**.
- Session expiration / token refresh / logout: częściowo (sesja nuxt-auth-utils) — brak jawnych
  scenariuszy wygaśnięcia → **Medium**.
- Sensitive data masking + audit trail dla akcji wrażliwych: masking tokenu jest; audyt akcji security
  brak → **High** (audit).

**QA/SDET architecture**
- Stable locators / a11y-first: dobre → utrzymać.
- Deterministic seed / API-driven setup / cleanup / test isolation / predictable users-roles-tenants:
  **brak** → **High**.
- Feature flags / mockable deps / CI-friendly startup / Docker-friendly: compose jest; feature flags
  i seed brak → **High/Medium**.
- Wyraźny rozdział kodu produkcyjnego od testowego: utrzymany (Playwright pisany osobno) → **utrzymać**.

---

## 6. Recommended Product Direction

**„Multi-Tenant Payment Operations Backoffice”** — rozszerzenie obecnego rdzenia o warstwę IAM i
obserwowalność, bez przepisywania.

Model docelowy:
- **Tenant** (organizacja) → posiada wielu **Merchants** → każdy ma **Payment Orders**.
- **User** należy do tenanta, ma jedną lub więcej **ról nazwanych**.
- **Audit Log** rejestruje akcje (kto, co, kiedy, na czym, correlationId).
- **Notifications** i **File import/export** jako warstwy „operacyjne”.

Role (mapowane na istniejące authorities + nowe):

| Rola | Zakres | Przykłady uprawnień |
|---|---|---|
| PLATFORM_ADMIN | globalny | zarządza tenantami i userami globalnie; wszystkie odczyty |
| TENANT_ADMIN | własny tenant | zarządza userami w tenancie; merchants w tenancie |
| MERCHANT_MANAGER | własny merchant/tenant | tworzy i prowadzi payment orders + lifecycle |
| SUPPORT_AGENT | operacyjny | podgląd danych + dodawanie notatek; **bez** zmian pól finansowych |
| READ_ONLY_USER | podgląd | listy/detale; **żadnych** write |

Domena ta naturalnie generuje: dashboards, CRUD, admin flows, role-based UI, protected routes,
formularze, złożone tabele, detale, workflowy, audyt, operacje plikowe, realistyczne błędy, walidację
FE+BE, scenariusze bezpieczeństwa.

---

## 7. Playwright Learning Feature Matrix

Kolumny: **Feature | Cel biznesowy/UI | Future Playwright skill | Frontend concept | Backend/API/Security | Difficulty | Future test scenarios | Build now?**

| Feature | Cel biznesowy/UI | Playwright skill (future) | Frontend | Backend/Security | Diff | Future scenarios | Build? |
|---|---|---|---|---|---|---|---|
| Login (real Keycloak OIDC) | wejście do systemu | Keycloak login flow, storageState, setup project | route middleware, sesja | OIDC/JWT claims | Basic→Adv | login happy/zły hasło; storageState reuse | **Now** |
| Forbidden (403) page | informacja o braku uprawnień | getByRole, expect text | permission render | 403 vs 401 | Basic | wejście roli bez prawa → 403 view | **Now** |
| Dashboard Overview (role-aware) | przegląd KPI z backendu | getByRole, expect count | permission render, loading/empty | summary endpoints | Basic→Int | różne karty per rola | **Now** (extend) |
| Sidebar nav (role-specific) | nawigacja | locator chaining, nth, keyboard nav | layout, conditional render | RBAC render | Basic→Int | widoczność linków per rola | **Now** |
| Users list + filtry | zarządzanie userami | tables, pagination, filtering, query params | data table, URL state | pagination/sort API, RBAC | Int | filtruj po roli; deep-link; pusty stan | **Now** |
| Create User (modal + Zod) | zakładanie usera | modale, form submit, validation errors | form state, Zod | walidacja FE+BE, 409 duplicate | Int | walidacja pól; duplikat→problem+json | **Now** |
| Assign roles (multi-select/combobox) | RBAC w praktyce | combobox, dropdown, keyboard | controlled input | role change → audit | Int→Adv | dodaj/odejmij rolę; efekt w UI innej roli | **Now** |
| Edit User (drawer) + unsaved guard | edycja | drawers, dialog on leave | dirty state, beforeunload | optimistic locking (ETag) | Adv | „unsaved changes” przy wyjściu | Later |
| Audit Log dashboard | compliance | tables, date pickers, filtering, deep links | virtualized list (opcj.) | audit query, RBAC (audit) | Int→Adv | filtr po dacie/akcji; weryfikacja śladu po akcji | **Now** |
| Tenant switching (PLATFORM_ADMIN) | multi-tenant | role fixture, UI+API combined | scoped state | tenant isolation | Adv→Senior | admin widzi wiele tenantów; tenant admin tylko swój | **Now** |
| RBAC access matrix | bezpieczeństwo | role-based access matrix tests, auth bypass | permission render | 403 enforcement BE | Senior | macierz rola×akcja; bypass próbą bezpośredniego URL/API | **Now** |
| Payment Orders (istn.) | rdzeń | filtering/sorting/pagination, tabs | data table, tabs | filtry/sort API | Int | (rozszerzyć o role) | Extend |
| Lifecycle drawer + confirm (istn.) | operacje | drawers, modale, retry | optimistic vs real state | ETag/If-Match 412/428, idempotency 409 | Adv | stale If-Match→412; replay key→200 | Extend |
| Bulk actions (orders/users) | masowe operacje | mouse/keyboard multiselect, request assertions | optimistic UI, batch error | batch endpoint, transakcje | Adv | zaznacz N→akcja; częściowy błąd | Later |
| File import (CSV upload) | wsad | setInputFiles, waiting for network, toast | upload progress/states | walidacja pliku, problem+json | Adv | zły format→error; duży plik→loading | Later |
| Export (download) | raporty | download event, save | disabled while empty | streaming/Content-Disposition | Int→Adv | pobierz CSV; pusty→disabled | Later |
| Notifications center | powiadomienia | drawer/popover, polling, stale data | badge count, optimistic read | unread API | Int→Adv | oznacz przeczytane; licznik maleje | Later |
| Settings/Profile | konto | form submit, validation, a11y | dirty state | self-service vs admin | Int | zmiana profilu; walidacja | Later |
| Network error/slow (Error Lab+) | odporność | network interception, route fulfill, mocking errors | error/retry states | 5xx/timeout/429 | Adv→Senior | mock 500/timeout; retry przywraca | Extend |
| Responsive/mobile | mobile UX | mobile/desktop projects, viewport | responsive layout | — | Adv | sidebar collapse na mobile | Later |
| Cross-browser | kompatybilność | cross-browser projects | hydration diff | — | Senior | te same flow w 3 silnikach | Later |
| Command palette (Cmd+K) | szybka nawigacja | keyboard nav, getByRole | focus mgmt | — | Int | otwórz Cmd+K, nawiguj klawiaturą | Later/opcj. |

(Macierz reprezentatywna; w §12 każda pozycja zmapowana na lekcję.)

---

## 8. Prioritized Feature Backlog (fazowo)

Dla każdego itemu: opis · UI · endpointy · security · encje · przyszłe techniki Playwright · sugerowane
przyszłe testy · złożoność · wartość · rekomendacja.

### Phase 1 — Foundation for Playwright Basics

**1.1 Real Keycloak Login + Role Setup Projects**
- Opis: realne logowanie OIDC + 5 nazwanych ról; per-rola storage state.
- UI: login.vue (rozszerzyć), forbidden.vue (nowy).
- Endpointy: istniejące + rozszerzenie claimów (role nazwane).
- Security: realm: nowe role; mapowanie ról do authorities w backendzie.
- Encje: User/Role (Keycloak) + ewentualne lokalne `app_user`.
- Playwright (future): storageState, setup project, getByRole/getByLabel, Keycloak flow.
- Testy (future): login happy/negatyw; reuse storage; 403 view.
- Złożoność: M · Wartość: 5 · **Build now.**

**1.2 Role-aware Sidebar + Overview**
- UI: `dashboard.vue` (warunkowe linki), `index.vue`.
- Playwright: locator chaining, expect count/visibility per rola.
- Złożoność: S · Wartość: 4 · **Build now.**

### Phase 2 — Realistic Admin Dashboard

**2.1 User Management (`/admin/users`)**
- Opis: lista + create (modal) + edit (drawer) + assign roles (combobox).
- UI: UTable, UModal, USlideover, UForm/UFormField, USelectMenu/combobox, UToast.
- Endpointy: `GET/POST /api/users`, `GET/PATCH /api/users/{id}`, `POST /api/users/{id}/roles`.
- Security: PLATFORM_ADMIN (global), TENANT_ADMIN (own tenant); READ_ONLY → 403 na write.
- Encje: `app_user`, `user_role`, FK do `tenant`.
- Playwright: modale, drawers, combobox, tables, pagination, filtering, validation, query params.
- Testy: create+walidacja+duplikat(409); filtr po roli; deep-link; RBAC (readonly bez przycisków).
- Złożoność: L · Wartość: 5 · **Build now.**

**2.2 Tenant model + Tenant switching**
- UI: tenant selector (navbar) dla PLATFORM_ADMIN.
- Endpointy: `GET /api/tenants`, scoping merchants/orders po `tenant_id`.
- Security: tenant isolation (TENANT_ADMIN widzi tylko swój).
- Encje: `tenant`; `merchant.tenant_id`.
- Playwright: role fixture, UI+API combined, auth bypass (próba cudzego tenant_id → 403).
- Złożoność: L · Wartość: 5 · **Build now** (kluczowe dla bezpieczeństwa).

### Phase 3 — API + UI + Security Learning

**3.1 Audit Log (`/admin/audit`)**
- Opis: zapisywane akcje (user, action, target, tenant, correlationId, timestamp).
- UI: UTable + date pickers + filtry + paginacja + deep link do wpisu.
- Endpointy: `GET /api/audit` (filtry: actor, action, from/to, target), RBAC `:audit`.
- Security: PLATFORM_ADMIN/auditor pełny; TENANT_ADMIN tylko swój tenant; SUPPORT_AGENT read.
- Encje: `audit_event`.
- Playwright: filtering, date pickers, pagination, audit trail verification (akcja→wpis), query
  params/deep links.
- Złożoność: M · Wartość: 5 · **Build now.**

**3.2 Permission-based rendering polish + 403/401 flows**
- Playwright: permission-based UI, 401 vs 403, auth bypass.
- Złożoność: S · Wartość: 4 · **Build now.**

### Phase 4 — Advanced SDET Scenarios

**4.1 File Import (CSV) + Export**
- UI: upload (`UInput type=file`), progress, error summary; export button (download).
- Endpointy: `POST /api/.../import` (multipart), `GET /api/.../export` (CSV/Content-Disposition).
- Playwright: setInputFiles, download event, waiting for network, error states.
- Złożoność: M-L · Wartość: 4 · **Build later.**

**4.2 Bulk actions**
- UI: row selection, bulk bar, optimistic update.
- Endpointy: batch endpoint (transakcyjny, częściowy sukces → problem+json z detalami).
- Playwright: mouse/keyboard multiselect, request/response assertions, optimistic vs real.
- Złożoność: L · Wartość: 4 · **Build later.**

**4.3 Network resilience lab (mock-friendly)**
- UI: jawne retry/slow/error states.
- Playwright: route interception/fulfill, mocking 5xx/timeout/429, tracing.
- Złożoność: M · Wartość: 5 · **Build later** (głównie wsparcie testów, ale FE musi mieć stany).

**4.4 Notifications center + Settings/Profile (unsaved guard)**
- Playwright: drawer/popover, stale data, dialog-on-leave.
- Złożoność: M · Wartość: 3 · **Build later.**

**4.5 Responsive/mobile readiness**
- Playwright: multi-project (mobile/desktop), cross-browser.
- Złożoność: S (głównie konfiguracja FE) · Wartość: 3 · **Build later.**

### Phase 5 — Senior QA Architecture Support (po stronie APLIKACJI)

To **nie** są testy, lecz cechy aplikacji, które umożliwią późniejszą architekturę testów:
- **Deterministyczny seed** (profil `seed` / Flyway-test / skrypt) → predictable users/roles/tenants/orders.
- **Reset/isolation hook** — opcjonalny **test-only endpoint** `POST /api/test/reset` **za feature
  flagą** `app.testing.enabled=false` domyślnie.
- **Stabilne `data-testid`** i a11y w nowych ekranach.
- **Feature flags** (np. włączanie import/bulk) → testy warunkowe.
- **CI-friendly startup** (compose już jest) + jasna separacja kodu prod/test.
- Wartość: 5 · **Build now (seed + a11y standard), reszta progresywnie.**

---

## 9. UI/UX Recommendations (Nuxt UI Dashboard Template)

Dla każdego ekranu: komponenty · stany UI · a11y · przyszłe testy · poziom lekcji.

- **Login** — `UForm/UFormField/UInput/UButton`; stany: idle/submitting/error; a11y: label
  „Username/Password”, `aria-invalid`, focus na pierwszym polu; testy: getByLabel, Keycloak flow;
  **Basic→Adv**.
- **Forbidden (403)** — `UAlert` + `UButton` „Back”; stan: statyczny; a11y: `role="alert"`, heading h1;
  testy: getByRole, expect text; **Basic**.
- **Dashboard Home** — `UDashboardPanel`, `UPageCard`, summary cards; stany:
  loading(skeleton)/empty/error/role-variant; a11y: nagłówki sekcji, karty z accessible name; testy:
  expect count per rola; **Basic→Int**.
- **Users list** — `UTable`, `UInput` (search), `USelectMenu` (filtr roli), `UPagination`; stany:
  loading/empty/error; a11y: `<table>` semantyka, kolumny z nagłówkami, sortowalne nagłówki jako
  `button`; testy: filtering/sorting/pagination, query params; **Int**.
- **Create/Edit User** — `UModal`/`USlideover`, `UForm`, combobox ról; stany:
  walidacja/duplikat/sukces toast/dirty; a11y: focus trap, `aria-describedby` dla błędów, label; testy:
  modale/drawers, validation errors, combobox; **Int→Adv**.
- **Audit Log** — `UTable`, date picker (`UPopover`+kalendarz / `UInput type=date`), filtry; stany:
  loading/empty/error/deep-link; a11y: daty z accessible name, filtry z labelami; testy: date pickers,
  filtering, deep links, audit verification; **Int→Adv**.
- **Tenant switcher** — `USelectMenu`/`UDropdownMenu` w navbarze; a11y: `aria-label="Select tenant"`;
  testy: role fixture, UI+API combined, isolation; **Adv→Senior**.
- **Payment list/detail (istn.)** — utrzymać taby (`UTabs`), drawer lifecycle (`USlideover`), confirm
  (`UModal`); rozszerzyć o role-variant (READ_ONLY ukrywa akcje); **Int→Adv**.
- **File Import** — `UInput type=file` + lista błędów (`UTable`/`UAlert`) + progress; a11y: label inputu
  pliku, komunikaty błędów jako tekst; testy: setInputFiles, network wait, errors; **Adv**.
- **Notifications center** — `USlideover`/`UPopover`, badge licznika; stany: empty/stale; testy:
  drawer/popover, optimistic read; **Int→Adv**.
- **Settings/Profile** — `UForm` + „unsaved changes”; a11y: `aria-live` dla statusu zapisu; testy:
  dialog-on-leave; **Int**.
- **System Health** — `UPageCard` + status z `/api/status`; **Basic** (opcjonalnie).

Zasada przewodnia: **Nuxt UI primitives przed custom CSS**, semantyczny HTML, badge'y rozróżnialne
tekstem (nie kolorem), stabilne `data-testid` tylko tam, gdzie role/label nie wystarczą.

---

## 10. Backend/API/Security Design Recommendations

**REST/DTO/validation/error**: kontynuować obecny styl (problem+json, walidacja Bean Validation +
value objects, dedykowane `@RestControllerAdvice` per moduł). Nowe zasoby:
- `tenant`, `user`, `role-assignment`, `audit_event`, (opcjonalnie) `import_job`.
- Endpointy: `/api/tenants`, `/api/users`, `/api/users/{id}/roles`, `/api/audit`, `/api/.../import`,
  `/api/.../export`.
- Paginacja/sort/filtry spójne z istniejącym list endpointem (page/size≤100/sort).

**Security/Keycloak (kluczowa zmiana):**
- Wprowadzić **role kompozytowe** w realmie: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `MERCHANT_MANAGER`,
  `SUPPORT_AGENT`, `READ_ONLY_USER`, mapowane na istniejące authorities + nowe (`platform:users:*`,
  `tenant:users:*`, `platform:tenants:*`, `audit:read`).
- Zachować claim `merchant_id`; dodać `tenant_id` claim + mapper.
- Egzekwować **tenant isolation** w serwisach (nie tylko authority) — TENANT_ADMIN nie widzi cudzego
  tenanta (403 lub masked 404, spójnie z obecnym wzorcem maskowania 404).
- Przykłady zachowań: PLATFORM_ADMIN tworzy userów globalnie; TENANT_ADMIN tylko w swoim tenancie;
  MERCHANT_MANAGER prowadzi lifecycle; SUPPORT_AGENT dodaje notatki ale **nie** zmienia pól
  finansowych (pole-level authorization); READ_ONLY tylko GET; brak tokenu → 401; zły zakres → 403;
  **backend zawsze egzekwuje niezależnie od UI**.

**Audit model**: `audit_event(id, occurred_at, actor_subject, actor_display, action, target_type,
target_id, tenant_id, correlation_id, outcome)`; nie zapisywać danych wrażliwych/tokenów.

**Seed / deterministyczne dane testowe**:
- Dedykowany profil `seed` lub Flyway repeatable dla danych dev (NIE w `validate`-only prod), z
  **predictable IDs** (stałe UUID/ref), znani userzy per rola, 2 tenanty, kilku merchantów, zestaw
  orderów w różnych statusach.
- **Test-only endpoints**: **dozwolone warunkowo** — `POST /api/test/reset`, `POST /api/test/seed`
  **tylko** przy `app.testing.enabled=true` (domyślnie false, wyłączone w prod profilu), zabezpieczone
  osobnym filterem/profilem. To realistyczne podejście SDET (API-driven setup/cleanup) i uczy test
  isolation. Alternatywa bez endpointu: reset DB per uruchomienie (Testcontainers/compose) + seed na
  starcie.
- **Rekomendacja**: seed deterministyczny **teraz**; test-reset endpoint za flagą **gdy** zaczniecie
  pisać E2E (faza testów).

---

## 11. Conceptual Future Playwright Framework Architecture (tylko koncepcja)

Docelowa struktura (do zbudowania **później**, w lekcjach — nie teraz):

```
apps/frontend/tests/
├── e2e/            # scenariusze user journey (UI)
├── api/            # testy kontraktowe API (APIRequestContext)
├── smoke/          # krytyczna ścieżka, szybkie
├── regression/     # pełne pokrycie
├── security/       # macierze RBAC, 401/403, bypass
├── pages/          # Page Objects (BasePage, LoginPage, DashboardPage, UsersPage, AuditPage)
├── components/     # Component Objects (TableCO, ModalCO, DrawerCO, ToastCO, SidebarCO)
├── fixtures/       # auth fixture, role fixture, test-data fixture
├── api-clients/    # cienkie klienty REST (UserApiClient, AuditApiClient)
├── test-data/      # buildery danych (userBuilder, orderBuilder)
├── assertions/     # custom matchers (expectProblemDetails, expectMaskedAuth)
├── utils/          # helpers (waitFor..., parsers)
├── auth/           # *.setup.ts per rola → storageState
└── playwright.config.ts  # projekty: setup, chromium/firefox/webkit, mobile
```

Koncepcyjne role obiektów:
- **BasePage** — wspólny `goto`, oczekiwania na hydration, dostęp do nawigacji.
- **LoginPage / DashboardPage / UsersPage / AuditPage** — akcje biznesowe, nie selektory rozsiane po
  testach.
- **Component Objects** (Table/Modal/Drawer/Toast/Sidebar) — reużywalne fragmenty UI; **Page Component
  Object Model** zamiast duplikacji.
- **API client** — szybkie precondition/cleanup (np. „stwórz usera przez API, testuj UI”).
- **auth fixture / role fixture** — wstrzykują kontekst zalogowanej roli (storageState).
- **test data builder / cleanup helper** — deterministyczne dane + sprzątanie (lub reset endpoint).

Kiedy POM ma sens: gdy ekran ma >~3 akcje używane w wielu testach. Kiedy to over-engineering: dla
jednorazowego smoke testu jednego elementu — wtedy inline locator. **KISS/DRY: abstrakcja po 2–3
powtórzeniu, nie wcześniej.**

---

## 12. Learning Lesson Roadmap (każda funkcja → przyszła lekcja)

Skrótowy format: **EN/PL · feature · Playwright methods · TS · FE · BE/API · Security · scenariusz ·
asercje · błędy · debug · junior · senior**. Roadmap rośnie od basic do senior.

1. **Login & Storage State / Logowanie i stan sesji** — login.vue · `page.goto`, `getByLabel`,
   `getByRole`, storageState · TS: typy fixture · FE: route middleware · BE: OIDC · Sec: JWT ·
   scenariusz: zaloguj, zapisz state · asercje: URL po loginie, brak tokenu w DOM · błędy: czekanie na
   hydration · debug: trace viewer · junior: „logujemy się raz” · senior: „setup project + reuse stanu,
   izolacja per rola”.
2. **Forbidden vs Unauthorized / 403 vs 401** — forbidden.vue · `getByRole`, expect text · Sec:
   401 vs 403 · scenariusz: rola bez prawa → 403 view, brak sesji → redirect/401.
3. **Dashboard counts / Liczniki dashboardu** — Overview · `expect(...).toHaveCount` · FE:
   loading/empty.
4. **Sidebar per role / Nawigacja wg roli** — `locator chaining`, `nth` · FE: conditional render · Sec:
   RBAC render.
5. **Users table: filter/sort/paginate / Tabela userów** — `UTable` · filtering/sorting/pagination,
   query params.
6. **Create user modal / Modal tworzenia** — modale, `getByLabel`, validation errors · BE: 409
   duplicate problem+json.
7. **Assign roles combobox / Przypisywanie ról** — combobox, keyboard nav · Sec: efekt w UI innej roli.
8. **Edit user drawer + unsaved guard** — drawers, dialog-on-leave · FE: dirty state · BE: ETag
   optimistic locking.
9. **Audit filtering + date pickers / Audyt** — date pickers, deep links · audit trail verification.
10. **Tenant isolation / Izolacja tenantów** — role fixture, UI+API combined · Sec: bypass cudzego
    tenanta → 403.
11. **RBAC access matrix / Macierz dostępu** — data-driven, role-based access matrix · Senior.
12. **Lifecycle concurrency / Współbieżność lifecycle** — request/response assertions · BE: 412/428/409.
13. **Network mocking & resilience / Mockowanie sieci** — `route.fulfill`, mocking 5xx/timeout · FE:
    retry/stale · Senior.
14. **File import/export / Import-eksport** — `setInputFiles`, download event, waiting for network.
15. **Bulk actions / Operacje masowe** — multiselect, optimistic vs real, partial failure.
16. **Notifications / Powiadomienia** — drawer/popover, polling, stale data.
17. **Responsive & cross-browser / Responsywność** — multi-project, viewport, mobile/desktop matrix.
18. **Architecture capstone / Architektura** — POM, fixtures, api-clients, test-data builders,
    smoke/regression split, trace-on-failure policy, sharding · Senior interview-level.

Każda lekcja kończy się „senior interview explanation” (np. dlaczego storageState > logowanie przez UI
w każdym teście; dlaczego API-driven setup zmniejsza flakiness; test pyramid: dużo API-contract, mniej
E2E).

---

## 13. Kiro Spec Mode Plan

Proponowane specs (w stylu Kiro: requirements/design/tasks), bez testów Playwright.

**SPEC A — `iam-roles-and-keycloak-login`**
- Business goal: realistyczny multi-role IAM + realny login OIDC.
- Learning goal: RBAC, role-based rendering, storageState, 401/403.
- User stories: jako PLATFORM_ADMIN/TENANT_ADMIN/.../READ_ONLY widzę i robię tylko to, co wolno.
- Acceptance (EARS): WHEN rola X bez authority Y próbuje akcji Z, THE system SHALL zwrócić 403 i UI
  SHALL ukryć/wyłączyć akcję.
- Backend tasks: role kompozytowe w realmie, mapowanie do authorities, tenant_id claim, egzekwowanie
  isolation.
- Frontend tasks: forbidden page, role-aware sidebar/overview, permission helpers.
- Security/DB tasks: realm update, `tenant`, `app_user`, `user_role`.
- A11y/testability: stabilne testid, semantyczne nagłówki, badge tekstowe.
- Future Playwright scenarios: login, storageState, RBAC matrix, bypass.
- Risks: zmiany w realmie i kontrakcie security mogą dotknąć istniejące testy bezpieczeństwa backendu.
- Open questions: czy role nazwane zastępują, czy uzupełniają obecne authorities?

**SPEC B — `user-management`** (zależne od A): CRUD userów, modale/drawer/combobox, walidacja FE+BE,
audyt akcji.

**SPEC C — `audit-log-dashboard`** (zależne od A): zapis i przegląd zdarzeń, filtry/daty/deep-links,
RBAC audit.

**SPEC D — `deterministic-seed-and-test-isolation`**: profil seed, predictable data, opcjonalny
test-reset endpoint za flagą.

**SPEC E — `file-import-export`** (później): multipart import + CSV export + stany błędów/progres.

(Phase 4/5 jako kolejne specs: bulk-actions, network-resilience-lab, notifications-and-settings,
responsive-readiness.)

---

## 14. Scoring and Prioritization

Skala 1–5 (PW=Playwright, UI, BE, Sec, Cx=złożoność [5=wysoka], Real=realizm, Maint=utrzymywalność,
Test=testowalność).

| Feature | PW | UI | BE | Sec | Cx | Real | Maint | Test | Rekomendacja |
|---|---|---|---|---|---|---|---|---|---|
| IAM + role + login | 5 | 4 | 4 | 5 | 4 | 5 | 4 | 5 | **Build now** |
| User Management | 5 | 5 | 4 | 4 | 4 | 5 | 4 | 5 | **Build now** |
| Tenant isolation | 5 | 3 | 4 | 5 | 4 | 5 | 4 | 5 | **Build now** |
| Audit Log | 5 | 4 | 4 | 4 | 3 | 5 | 4 | 5 | **Build now** |
| Deterministic seed | 4 | 2 | 3 | 3 | 3 | 4 | 5 | 5 | **Build now** |
| File import/export | 4 | 4 | 3 | 2 | 4 | 4 | 3 | 4 | Build later |
| Bulk actions | 4 | 4 | 4 | 3 | 4 | 4 | 3 | 4 | Build later |
| Network resilience lab | 5 | 3 | 3 | 3 | 3 | 3 | 4 | 5 | Build later |
| Notifications center | 3 | 4 | 2 | 2 | 3 | 3 | 3 | 4 | Build later |
| Settings/Profile + guard | 3 | 3 | 2 | 2 | 2 | 4 | 4 | 4 | Build later |
| Responsive/mobile + multi-project | 3 | 4 | 1 | 1 | 2 | 3 | 4 | 4 | Build later |
| Command palette | 2 | 3 | 1 | 1 | 3 | 3 | 3 | 3 | Postpone |
| System health page | 2 | 2 | 2 | 1 | 1 | 2 | 4 | 3 | Postpone |
| WebSocket/SSE, jobs, rate-limit, versioning | 3 | 3 | 4 | 2 | 5 | 3 | 2 | 3 | Postpone |
| Drag-and-drop (duży) | 2 | 3 | 1 | 1 | 4 | 2 | 2 | 3 | Postpone |
| PSP/Kafka/KYC/3DS | 1 | 1 | 3 | 2 | 5 | 4 | 1 | 1 | **Skip** |

Priorytet: maksymalna wartość nauki przy rozsądnej złożoności → **IAM, User Management, Tenant
isolation, Audit, Seed** jako pierwsze.

---

## 15. Practical Risk Review

**Najwyższa wartość nauki:** IAM/role + permission-based rendering + audit + tenant isolation.

**Niska wartość:** system health page, command palette, drag-and-drop jako osobny feature.

**Drogie względem wartości:** WebSocket/SSE, background jobs/scheduler, rate limiting, API versioning —
dużo kodu, mało nowych unikalnych technik UI testowych. Punktowo (np. 429 w Error Lab) OK; jako pełne
moduły — nie.

**Do postponed:** notifications, settings, responsive/mobile, file ops — wartościowe, ale po rdzeniu IAM.

**Do skip:** realne integracje płatnicze (jawnie Non-Goals).

**Ryzyka architektury:** zmiany w realmie i kontrakcie security mogą zepsuć istniejące testy backendu
(`security/`, macierze) — ostrożna migracja ról (role nazwane jako kompozyty mapujące na obecne
authorities, by nie ruszać reguł w `SecurityConfig`).

**Ryzyka frontendu:** rozrost ekranów grozi niespójnością a11y/`data-testid` → utrzymać dyscyplinę
steeringu na każdym nowym ekranie; ryzyko hydration mismatch przy mieszaniu SSR/CSR — trzymać
`/admin/**` jako CSR.

**Ryzyka backendu:** audit logging dodaje zapisy w transakcjach — robić przez zdarzenia Modulith, nie
bezpośrednie zależności między modułami; nie naruszać granic (`payment` nie zależy od `merchant.internal`).

**Ryzyka bezpieczeństwa:** test-reset endpoint to potencjalny backdoor — bezwzględnie za feature flagą
wyłączoną w prod, izolowany filterem/profilem; nigdy nie logować tokenów (utrzymać masking).

**Ryzyka testowe:** brak deterministycznego seeda = flaky E2E → seed/isolation **przed** dużymi
zestawami E2E.

**Ryzyko over-engineering:** budowanie POM/fixtures „na zapas” teraz — zostawić na lekcje; aplikacja ma
być testowalna, nie „otestowana z góry”.

**Gdzie aplikacja może stać się zbyt złożona:** jednoczesne wprowadzanie tenant model + role + audit +
file ops. Dlatego fazowanie: A→B→C, dopiero potem D/E.

---

## 16. Final Review: Did We Miss Anything Important?

**Brakujące nowoczesne koncepcje aplikacji:** real-time (SSE) — świadomie **postpone**; OpenAPI/Swagger
jako źródło contract-checks — **warto dodać** (Medium); „unsaved changes” guard — w Settings (Later).

**Brakujące umiejętności Senior QA/SDET:** API-driven setup/cleanup, test data lifecycle, multi-role
matrix, trace/video policy, sharding — pokryte przez Seed + IAM + koncepcyjną architekturę (§11).
**Visual regression / screenshot testing** — nie poruszone; opcjonalnie **Later** (niski priorytet
wobec a11y-first).

**Brakujące obszary Playwright 1.61:** clock/time mocking (`page.clock`) — przydatne przy date
pickers/stale data → lekcja przy Audit/Notifications; component testing (eksperymentalne) — **skip**
(mamy Vitest); ARIA snapshot (`toMatchAriaSnapshot`) — **dodać** do lekcji a11y. Zsynchronizować wersję
do 1.61.

**Brakujące Nuxt 4 / TS / Pinia / Zod:** SSR-first widok dla kontrastu hydration (Medium); Pinia —
stale/optimistic w notifications/bulk; Zod — `superRefine`/cross-field validation w user/roles; TS —
typowanie fixtures/buildersów (w lekcjach).

**Brakujące Spring Boot 4 / Spring 7 / JDK 25 / PostgreSQL 18:** audit przez events (Modulith) — dodać;
transakcje/constraints przy user_role i tenant FK — dodać; **PostgreSQL 18**: partycjonowanie/
`generated columns`/indeksy pod audit (Medium, opcjonalnie); background jobs/scheduler — **skip/postpone**.

**Brakujące Keycloak 26.6.x / security:** **Keycloak groups** (nie tylko role) — warto dla mapowania
tenant→group (Medium); token refresh/session expiration scenariusze — lekcja (Medium); składowe
role/kompozyty — w SPEC A.

**Do dodania do roadmapy:** OpenAPI doc (contract-checks), ARIA snapshot + `page.clock` (lekcje
a11y/czas), Keycloak groups → tenant, token-expiry lesson.

**Świadomie pominięte:** PSP/Kafka/settlement/KYC/3DS, real-time trading, visual-regression jako rdzeń,
component testing w Playwright, mikroserwisy.

---

## 17. Recommended First 5 Specs to Build

1. **`iam-roles-and-keycloak-login`** — 5 nazwanych ról (kompozyty nad istniejącymi authorities), realny
   login OIDC, forbidden page, role-aware nawigacja/overview, tenant_id claim. *(odblokowuje multi-role,
   storageState, RBAC, 401/403)*
2. **`tenant-model-and-isolation`** — encja tenant, scoping merchants/orders, tenant switcher dla
   PLATFORM_ADMIN, egzekwowanie isolation w serwisach. *(bezpieczeństwo, UI+API combined, auth bypass)*
3. **`user-management`** — CRUD userów + przypisywanie ról (modal/drawer/combobox), walidacja FE+BE,
   emisja zdarzeń audytu. *(modale, tabele, walidacja, RBAC w UI)*
4. **`audit-log-dashboard`** — `audit_event` + `/api/audit` + ekran z filtrami/datami/deep-links.
   *(date pickers, filtering, audit trail verification)*
5. **`deterministic-seed-and-test-isolation`** — profil seed z predictable users/roles/tenants/orders +
   opcjonalny test-reset endpoint za feature flagą. *(API-driven setup, test isolation, data lifecycle)*

Kolejność wymuszona zależnościami: 1 → 2 → 3/4 (równolegle) → 5 (najlepiej równolegle z 3/4).

---

## 18. Final Recommendation

Nie przepisuj aplikacji — masz solidny, świetnie przetestowany rdzeń kontraktu REST i protokołu HTTP.
**Zbuduj wokół niego warstwę IAM (role nazwane + tenant isolation) + User Management + Audit +
deterministyczny seed.** To odblokowuje brakujące ~90% technik Playwright/SDET (multi-role,
permission-based rendering, RBAC matrix, auth bypass, tabele/filtry/daty, modale/drawers/combobox,
audit verification, API-driven setup), trzymając się Non-Goals i nie psując istniejących testów.

Każda rekomendowana funkcja odpowiada na pytanie „czego się dzięki niej nauczę jako SDET”: role → RBAC
i bezpieczeństwo; users → formularze/modale/walidacja; audit → tabele/daty/ślad; seed → izolacja i
stabilność testów. Funkcje o niskim zwrocie (real-time, jobs, rate-limit, PSP) odłożone lub pominięte.

**Przypomnienie:** Kiro buduje aplikację (testowalną, dostępną, locator-friendly), a testy Playwright
powstaną później w osobnych lekcjach. Architektura testów opisana wyłącznie koncepcyjnie (§11).
