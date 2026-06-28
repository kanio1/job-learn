# Payment Quality Engineering Lab — Business & Technical Cases Architecture Analysis

> Wygenerowano: 2026-06-28. Bazuje na inspekcji repo (branch `001-project-foundation`)
> i dokumencie `playwright-sdet-feature-roadmap.md` (2026-06-26).
> Język: polski. Podejście: discovery-first, prefer-minimal-change.

---

## 1. Executive Summary

System jest w dojrzałym stanie. Backend ma 8 Spring Modulith modules, pełny lifecycle
płatności z idempotencją, ETag/If-Match, paginated queries, audit trail i security matrix.
Frontend ma działający Error Lab z 9 triggerami, ApiDebugPanel, HeaderKeyValuePanel,
ProblemDetailsCard, RawJsonViewer i pełne proxy BFF. Playwright ma 9 spec files z mockami,
ale brakuje multi-role setup, APIRequestContext, network assertions i zaawansowanych technik.

**Kluczowe wnioski inspekcji:**

1. `IdempotencyRecord`, `PaymentVersionPrecondition`, `MockPspClient`, `TenantStatus.SUSPENDED`
   już istnieją — żaden z tych elementów nie wymaga nowego modułu.
2. `PaymentRateLimitContractRestKitTest` w `restkit/` sugeruje, że rate limiting jest już
   na etapie kontraktowym — brakuje tylko backend implementacji i Error Lab UI triggera (429).
3. `PLACEHOLDER_TENANT_ID` (`...a3`) to idealny kandydat na TENANT_BETA SUSPENDED —
   bez tworzenia nowych encji, tylko rozszerzenie seed.
4. Hipoteza 3 modułów (Payment Core / Support-Risk-Compliance / HTTP Lab) jest w ~50%
   słuszna, ale "HTTP Contract Lab" nie powinien być osobnym backend module.
5. `waitForTimeout(500)` w `payment-order-create.spec.ts:16` to jedyny zidentyfikowany
   anti-pattern do natychmiastowego usunięcia.

**Rekomendacja nadrzędna:** Rozwijaj istniejące moduły. Nie twórz nowych modułów dla
HTTP method education — Error Lab + istniejące endpointy + frontend panels wystarczą.
Dodaj jeden opcjonalny `support` moduł (Phase 2) dla cross-merchant search.

---

## 2. Source Document and Repository Findings

### 2.1 Dokument wejściowy

`playwright-sdet-feature-roadmap.md` (2026-06-26, 2242 linie) — solidny, zweryfikowany
przez Context7. Zawiera: stan repo, 35+ funkcji, capability matrix, POM roadmap, plan nauki
60 lekcji, expert review addendum z korektami (expect.soft.poll nie istnieje, ConfirmActionModal
to DOM modal a nie native dialog, API 1.61 gated).

**Co dokument mówi poprawnie:**
- Playwright 1.60.0 (nie 1.61) — wymaga upgrade przed lekcjami z localStorage/sessionStorage
- Braki Playwright: multi-role, APIRequestContext, network assertions, download, upload, iframe
- `waitForTimeout` jako anti-pattern do usunięcia
- Zasada: POM rośnie razem z systemem, nie wyprzedza go

**Co wymaga korekty lub uzupełnienia w tej analizie:**
- Hipoteza 3 modułów — do weryfikacji poniżej
- HTTP cases A–T — muszą być sklasyfikowane, nie tylko opisane
- Tenanty — PLACEHOLDER_TENANT_ID może pełnić rolę TENANT_BETA bez nowej encji

### 2.2 Wyniki inspekcji repozytorium

**Backend (zweryfikowane klasy):**

| Obszar | Plik | Stan |
|---|---|---|
| Idempotency domain | `IdempotencyRecord`, `IdempotencyKey`, `IdempotencyConflictException`, `IdempotencyCreateInProgressException` | ISTNIEJE |
| ETag/If-Match | `PaymentVersionPrecondition`, `PaymentEtag`, `PaymentHttpHeaders` | ISTNIEJE |
| PSP symulacja | `MockPspClient`, `PspClient` (interfejs) | ISTNIEJE |
| Rate limit contract | `PaymentRateLimitContractRestKitTest` (restkit — skip w ./mvnw test) | ISTNIEJE (contract only) |
| Lifecycle | `PaymentLifecycleAction`, `PaymentLifecycleService`, `PaymentStatus` (6 stanów) | ISTNIEJE |
| Tenant suspension | `TenantStatus.SUSPENDED` | ISTNIEJE |
| Seed — 3 tenants | PLATFORM_TENANT, TENANT_ALPHA, PLACEHOLDER_TENANT_ID | ISTNIEJE |
| Seed — 3 merchants | MERCHANT_ALPHA_001, MERCHANT_ALPHA_002, MERCHANT_BETA_001 | ISTNIEJE |
| CORS bean | `SecurityConfig.java` (dev profile) | ISTNIEJE |
| Audit | `AuditEvent`, `AuditableActionOccurred`, listener, controller | ISTNIEJE |

**Frontend (zweryfikowane):**

| Komponent | Plik | Stan |
|---|---|---|
| Error Lab | `pages/error-lab.vue` — 9 triggerów z data-testid | ISTNIEJE |
| API Debug Panel | `shared/ApiDebugPanel.vue` | ISTNIEJE |
| Header Panel | `shared/HeaderKeyValuePanel.vue` — maskuje Authorization | ISTNIEJE |
| Idempotency Input | `shared/IdempotencyKeyInput.vue` | ISTNIEJE |
| If-Match Input | `shared/IfMatchInput.vue` | ISTNIEJE |
| ETag Display | `shared/EtagDisplay.vue` | ISTNIEJE |
| Problem Details | `shared/ProblemDetailsCard.vue` | ISTNIEJE |
| Raw JSON Viewer | `shared/RawJsonViewer.vue` | ISTNIEJE |
| Confirm Modal | `shared/ConfirmActionModal.vue` (DOM modal, NIE native dialog) | ISTNIEJE |

**Testy (zweryfikowane):**

| Suite | Lokalizacja | Stan |
|---|---|---|
| restkit (skip w test) | `restkit/contract/create/` — rate limit, lifecycle, security | ISTNIEJE (compile issue) |
| paymentsupport | `paymentsupport/` — brak plików w listingu | BRAK |
| Playwright E2E | 9 spec files, wszystkie z `page.route()` mock | ISTNIEJE |
| Vitest property | 13 plików, 498 testów | ISTNIEJE |
| waitForTimeout | `payment-order-create.spec.ts:16` | ANTI-PATTERN obecny |

---

## 3. Agent / Skill Review Summary

### Agent 1 — Orchestrator

**Obserwacje:** System jest na poziomie production-grade dla domeny płatności.
Największy gap: Playwright ecosystem (multi-role, APIRequestContext, advanced patterns).
HTTP cases są już częściowo zaimplementowane — zadaniem jest wyeksponowanie ich w UI
i dopełnienie brakujących kontraktów, nie budowanie od zera.

**Decyzja koordynacyjna:** Odrzucam hipotezę osobnego HTTP Contract backend module.
Zatwierdzam rozszerzenie Error Lab jako głównej powierzchni edukacyjnej HTTP. Zatwierdzam
opcjonalny `support` moduł w Phase 2 (cross-merchant search). Odkładam Risk module do
Phase 3.

### Agent 2 — Senior QA Automation / SDET Mentor

**Obserwacje:** 9 capability areas not covered (multi-role, APIRequestContext, download,
upload, iframe, dialog handling, clock mocking, parallel, visual). Pełny POM dopiero
po implementacji docelowych stron. Base fixture z console/pageerror guard powinien być
krytyczną Fazą 1.

**Rekomendacja:**
- ACCEPT: upgrade Playwright 1.61, base fixture, multi-role setup, APIRequestContext (Tier A)
- ACCEPT: `waitForTimeout` → `waitForResponse`/`expect.poll`
- DEFER (Phase 2): download, upload, debounce, multi-role filtering
- DEFER (Phase 3): clock, iframe, multi-tab, visual regression, ARIA snapshot

**Ryzyko:** Framework-building trap. POM nie może wyprzedzać stron, które testuje.

### Agent 3 — Business Analyst PSP / Payments / Backoffice

**Obserwacje:** Lifecycle płatności jest realistyczny. Risk module (F-21) ma sens
biznesowy — velocity rules + manual review to standard w PSP. Support search (F-22)
jest kluczowy — operatorzy szukają po clientOrderReference bez znajomości merchantId.
CSV export jest compliance requirement. Webhook simulation jest edukacyjny ale wymaga
sporych zmian infrastrukturalnych.

**Rekomendacja:**
- ACCEPT as business feature: partial capture/refund UI (F-11), support search (F-22),
  CSV export (F-23), upload evidence (F-24), risk flags (F-08)
- DEFER: webhook duplicate handling (wymaga PSP callback endpoint) → Phase 3
- REJECT: reconciliation drift dashboard bez backendu PSP — overengineering for MVP
- REJECT: fake KPI metrics — nie wynikają z domeny

### Agent 4 — System Architect / Spring Modulith / Multi-Tenant SaaS

**Obserwacje:**
- 8 modułów jest właściwą granularnością. Nie ma potrzeby nowego modułu dla HTTP Lab.
- `payment` moduł może przyjąć rate limiting i support search query via `platform:payments:read`
- Osobny `support` moduł (Phase 2) ma sens gdy cross-merchant operations przekroczą 3–4 endpointy
- `testing` moduł jest prawidłowo izolowany za feature flagą

**Rekomendacja:**
- Phase 1: rozszerz `payment` i `shared` (rate limit header, 304 Not Modified)
- Phase 2: dodaj opcjonalny `support` moduł dla cross-merchant search + notes
- Phase 3: dodaj `risk` moduł lub rozszerz `merchant` o risk flags + review queue
- REJECT: `IdempotencyModule`, `ETagModule`, `BatchModule` — sztuczne

### Agent 5 — REST/HTTP API Contract Reviewer

**Obserwacje:**
- ETag na GET /payment-orders/{id} — istnieje. Brakuje 304 Not Modified response.
- HEAD na payment order — istnieje. Brakuje HEAD na reports (Phase 2/3).
- Idempotency-Key na lifecycle — istnieje. Brakuje UI replay demo (Error Lab).
- 429 — contract test istnieje (restkit), brak implementacji backend i Error Lab triggera.
- PATCH MetadataPatchRequest — idempotentny (set semantics). Poprawny.

**Rekomendacja:**
- ACCEPT: 304 Not Modified extension do istniejącego GET
- ACCEPT: 429 + Retry-After backend impl + Error Lab trigger (Phase 1)
- DEFER: conditional GET header panel w UI (Phase 2 — wymaga nowej strony)
- DOCUMENT ONLY: QUERY method (nie wspierany przez Spring 7), Batch POST, Method Override

### Agent 6 — Security / RBAC / Tenant Isolation Reviewer

**Obserwacje:**
- Cross-tenant masked 404 jest już zaimplementowane — brakuje UI demo w Error Lab.
- `MERCHANT_MANAGER` nie powinien widzieć cross-merchant data — enforcement jest poprawny.
- Token nie powinien być w browser storage — architektura sealed session jest prawidłowa.
- F-36 Token Leakage Guard: krytyczne jako base fixture, nie jako osobna funkcja.

**Rekomendacja:**
- ACCEPT: cross-tenant 403/masked-404 triggery w Error Lab (Phase 1, minimal change)
- ACCEPT: Token leak guard jako część base.fixture.ts (Phase 1)
- DEFER: Risk module RBAC (Phase 3)
- DOCUMENT: „403 vs masked 404" jako wymaganie security dla każdej nowej funkcji

### Agent 7 — Reliability Architect

**Obserwacje:**
- `IdempotencyCreateInProgressException` (425 Too Early) — istnieje w domenie,
  prawdopodobnie nie jest wyeksponowany jako HTTP 425. Weryfikacja potrzebna.
- `PaymentVersionPrecondition` — obsługuje ETag mismatch (412) i brak If-Match (428).
- Workflow recovery: crash po MockPspClient call jest trudny do symulacji bez prawdziwego PSP.
  Można zasymulować przez WireMock stub w testach.

**Rekomendacja:**
- ACCEPT: Workflow-level idempotency Error Lab demo (Phase 1 extension)
- ACCEPT: Sequential route mocks w Playwright (503 → 200) dla retry demo (Phase 2)
- DEFER: Webhook out-of-order handling → Phase 3
- DOCUMENT ONLY: Reconciliation drift — bez backend PSP integration to tylko UI show

### Agent 8 — Compliance / Audit / Risk Reviewer

**Obserwacje:**
- `AuditEvent` istnieje, ale brakuje `before_state`/`after_state` JSONB fields.
  Bez nich audit diff view (F-26) nie działa.
- CSV audit export: `GET /api/audit?format=json` — łatwa rozbudowa istniejącego endpointu.
- Internal notes (F-27) mają sens compliance: support dołącza notatki do sprawy.

**Rekomendacja:**
- ACCEPT: audit export JSON (Phase 2, małe rozszerzenie)
- ACCEPT: before/after audit diff — wymaga Flyway migration (Phase 2)
- DEFER: internal notes → Phase 2
- REJECT: reconciliation drift dashboard bez settlement backendu

### Agent 9 — Frontend Architect Nuxt.js / Vue / TypeScript

**Obserwacje:**
- Error Lab jest rozszerzalny — architektura scenarios array z computed state pozwala
  dodawać triggery bez refactoringu.
- Brak `/admin/merchants/[id]` strony — navigacja merchant → payments omija merchant detail.
- `usePaymentOrdersApi` i `usePaymentLifecycleApi` są gotowe, brakuje composable do pollingu.
- `PaymentOrderLifecycleActions` istnieje — partial capture/refund to extension pola amount.

**Rekomendacja:**
- ACCEPT Phase 1: Error Lab extensions (429, cross-tenant, idempotency demo), merchant detail page
- ACCEPT Phase 2: Support search page, polling composable, export buttons, upload dropzone
- ACCEPT Phase 3: PSP simulator iframe page, date range picker
- DEFER: Command palette → Phase 3 (expert level)

### Agent 10 — UI/UX Designer backoffice

**Obserwacje:**
- Nuxt UI Dashboard Template jest użyty poprawnie. Error Lab jest czytelny.
- Brakuje tenant context indicatora w header — operator nie widzi aktywnie, dla jakiego
  tenanta działa.
- Risk queue i Support search powinny być osobnymi stronami, nie zakładkami na istniejących.

**Rekomendacja:**
- ACCEPT: tenant badge w `dashboard.vue` header (Phase 1, minimal HTML change)
- ACCEPT: dedykowane strony `/admin/support/search` i `/admin/risk` (Phase 2)
- REJECT: wielu drawer-level wizardów na jednej stronie — nawigacja tabami jest prostsza

### Agent 11 — Nuxt UI Dashboard Template Reviewer

**Obserwacje:**
- `UCard`, `UTable`, `UBadge`, `USlideover`, `UModal` — wszystkie komponenty są poprawnie
  używane. Template constraints są respektowane.
- `data-testid` jest na Error Lab triggerach — dobre. Brakuje na toast notifications.

**Rekomendacja:**
- ACCEPT: toast data-testid extension (F-32) — 1 linia per toast komponent
- REJECT: custom design poza Nuxt UI Dashboard Template bez bardzo silnego uzasadnienia

### Agent 12 — Zod / Form Validation Reviewer

**Obserwacje:**
- Zod schemas: merchant, payment-order, audit, user, problem-details, app-shell — kompletne.
- Brakuje schematów dla: support search response, risk review, tenant settings PATCH.

**Rekomendacja:**
- ACCEPT Phase 2: `TenantSettingsSchema`, `SupportSearchResultSchema`, `RiskReviewSchema`
- FOLLOW: wzorzec `useApiClient` z Zod validation — nie zmieniaj go

### Agent 13 — API Client / Server Proxy / BFF Reviewer

**Obserwacje:**
- Nuxt server proxy: pełne pokrycie obecnych endpointów. Token nigdy nie dociera do browser JS.
- Error Lab triggery przechodzą przez `server/api/` — poprawne.
- Nowe endpointy (support search, CSV export, evidence upload) muszą mieć odpowiedniki
  w `server/api/` przed użyciem w composables.

**Rekomendacja:**
- ENFORCE: każdy nowy endpoint backendowy → nowa `server/api/` route w Nuxt
- ACCEPT: streaming CSV przez `sendStream()` w Nuxt server route (Phase 2)
- ACCEPT: multipart form-data forwarding przez Nuxt proxy (Phase 2)

### Agent 14 — Playwright E2E Architect

**Obserwacje:**
- 9 spec files z `page.route()` — Tier 1 mocked. Brak Tier 3/4 (real backend).
- `fullyParallel: false` blokuje naukę parallel testing.
- Multi-role potrzebuje workerStorageState pattern, nie test-scoped.
- `waitForTimeout(500)` w `payment-order-create.spec.ts:16` → natychmiastowa naprawa.

**Rekomendacja:**
- CRITICAL Phase 1: upgrade 1.61, base fixture, fix waitForTimeout, multi-role setup
- ACCEPT Phase 1: APIRequestContext (Tryb A — tylko reset/seed, bez tokena)
- DEFER Phase 2: Tier 3/4 tests (real backend), download, upload
- DEFER Phase 3: frameLocator, clock, multi-tab, visual

### Agent 15 — Product Strategist

**Odrzucenia (brutal):**
- Batch POST: brak sensu biznesowego w backoffice PSP — odrzucony
- Method Override: tylko legacy workaround dokumentacja — bez implementacji
- DELETE z body: tylko kontrolowany lab case — bez produkcyjnego API
- Reconciliation drift dashboard: bez backend PSP to fake metrics — odrzucone w MVP
- GET unsafe actions: tylko anti-pattern dokumentacja — bez `/api/http-lab/` endpointu
- Command palette: ergonomika operatora, ale Phase 3 (niska wartość SDET)
- QUERY method: nie wspierany przez Spring, ryzyko tooling compat — tylko dokumentacja
- WebSocket real-time: polling wystarczy dla lekcji Playwright
- Settlement/KYC/3DS/Kafka: poza scope guardrails

**Akceptacje (business-justified):**
- Rate limiting 429: każdy PSP API ma rate limit — implement
- Support search: operatorzy muszą szukać bez merchantId — implement (Phase 2)
- CSV export: compliance + reconciliation — implement (Phase 2)
- Evidence upload: dispute resolution — implement (Phase 2)
- Partial capture/refund UI: standard PSP feature — implement (Phase 1/2)
- Risk flags: PSP flaguje ryzykownych merchantów — implement (Phase 2)

### Consensus zespołu

**Zgoda:**
1. Rozszerzaj istniejące moduły, nie twórz nowych dla HTTP education
2. Error Lab jest główną powierzchnią edukacji HTTP — rozszerz go
3. Playwright Phase 1 = fundamenty (upgrade, base fixture, multi-role, APIRequestContext)
4. Phase 2 = senior SDET features (search, download, upload, risk queue)
5. Phase 3 = expert SDET (iframe, clock, multi-tab, visual)

**Sporne punkty:**
- Support search jako osobny moduł vs rozszerzenie payment — decyzja: osobny moduł Phase 2
  gdy >3 cross-merchant endpoints
- Risk module Phase 2 vs Phase 3 — decyzja: risk flags (merchant) Phase 2,
  risk review queue Phase 3 (wymaga nowej strony + backend queue + multi-role)
- 304 Not Modified: Agent 5 chce natychmiast, Agent 15 mówi niska wartość MVP —
  decyzja: Phase 1 (backend 1 linia, Error Lab demo)

**Decyzje wymagające ADR:**
1. ADR-01: Czy tworzyć `support` moduł w Phase 2 czy rozszerzać `payment`?
2. ADR-02: Jak obsługiwać cross-merchant search — GET z query params vs POST body?
3. ADR-03: Kiedy włączyć `fullyParallel: true` (data isolation readiness)?
4. ADR-04: Czy risk flags należą do `merchant` czy nowego `risk` modułu?

---

## 4. Current System Baseline

### 4.1 Moduły Spring Modulith (8 produkcyjnych)

```
shared      — OPEN; SecurityConfig, Authorities, CorrelationIdFilter, GlobalExceptionHandler
foundation  — standalone; GET /api/status (public)
tenant      — standalone; TenantResolver (PUBLIC API), TenantStatus: ACTIVE|SUSPENDED
merchant    — depends→tenant public; MerchantStatus: PENDING|ACTIVE|SUSPENDED
payment     — depends→merchant public; PaymentStatus: CREATED|AUTHORIZED|CAPTURED|CANCELLED|EXPIRED|REFUNDED
             Zawiera: IdempotencyRecord, PaymentVersionPrecondition, MockPspClient
iam         — standalone; Keycloak admin integration (WireMock w testach)
audit       — standalone, event-driven; AuditEvent + AuditableActionOccurred
testing     — seed-gated; reset + seed endpoints (app.testing.enabled=true)
```

### 4.2 HTTP Contract Stan

| Pattern | Backend | Frontend Panel | Error Lab | REST Assured | Playwright |
|---|:---:|:---:|:---:|:---:|:---:|
| Idempotency-Key | ✓ | ✓ (IdempotencyKeyInput) | BRAK demo | ✓ (restkit) | BRAK |
| ETag / If-Match | ✓ | ✓ (EtagDisplay, IfMatchInput) | BRAK | ✓ | BRAK |
| 304 Not Modified | BRAK | BRAK | BRAK | BRAK | BRAK |
| HEAD metadata | ✓ | BRAK assertion | BRAK | BRAK | BRAK |
| 412 Precondition Failed | ✓ | ✓ (ProblemDetailsCard) | ✓ (trigger) | ✓ | BRAK |
| 428 Precondition Required | ✓ | ✓ | ✓ (trigger) | ✓ | BRAK |
| 409 Conflict (idempotency) | ✓ | ✓ | ✓ (trigger) | ✓ | BRAK |
| 429 Rate Limit | BRAK impl | BRAK | BRAK | contract only | BRAK |
| X-Correlation-ID | ✓ | ✓ (HeaderKeyValuePanel) | ✓ implicit | ✓ | BRAK |
| application/problem+json | ✓ | ✓ (ProblemDetailsCard) | ✓ all | ✓ | ✓ (partial) |
| Retry-After header | BRAK | BRAK | BRAK | BRAK | BRAK |
| CORS / preflight | ✓ (dev profile) | BRAK | BRAK | BRAK | BRAK |

### 4.3 Seed Data (Deterministyczne)

```
Tenants:
  PLATFORM_TENANT   (ID: ...a1) — ACTIVE, typ PLATFORM
  TENANT_ALPHA      (ID: ...a2) — ACTIVE, typ REGULAR
  PLACEHOLDER_TENANT_ID (ID: ...a3) — ACTIVE (kandydat → SUSPENDED = TENANT_BETA)

Merchants:
  MERCHANT_ALPHA_001 (ID: ...b1) — ACTIVE, tenant: TENANT_ALPHA
  MERCHANT_ALPHA_002 (ID: ...b2) — ACTIVE, tenant: TENANT_ALPHA
  MERCHANT_BETA_001  (ID: ...b3) — ACTIVE, tenant: PLACEHOLDER_TENANT_ID

PaymentOrders: 6 base (c1-c6) + 98 pagination block (c101-c198)
```

---

## 5. Architecture Hypothesis Evaluation

### Hipoteza 3 modułów — werdykt

**Hipoteza:** Payment Operations Core / Support-Risk-Compliance / HTTP Contract Lab

| Ocena | Wynik |
|---|---|
| Czy hipoteza jest dobra? | Częściowo |
| Co jest dobre? | Separacja Support od Payment Core ma sens gdy cross-merchant search urośnie |
| Co jest zbyt sztywne? | "HTTP Contract Lab" jako moduł backend jest sztuczne — to frontend Extension |
| Co kod sugeruje zamiast? | Error Lab + istniejące endpointy + panels = kompletny HTTP Lab bez nowego modułu |
| Minimalny sensowny podział? | 8 istniejących + opcjonalny `support` moduł w Phase 2 |
| Docelowy podział? | 8 modułów + `support` + `risk` (Phase 3) = 10 modułów max |

**Decyzja:**
- NIE tworzyć "HTTP Contract Lab" jako backend module
- NIE tworzyć "HTTP Lab" endpointów (`/api/http-lab/`) w produkcyjnym kodzie
  (wyjątek: kontrolowane lab case clearly marked jako `@HttpLabOnly`)
- TAK dla `support` modułu w Phase 2 gdy cross-merchant operations przekroczą 3 endpointy
- TAK dla rozszerzenia `merchant` o risk flags (Phase 2), ewentualnie `risk` moduł w Phase 3

### Hipoteza tenantów — werdykt

| Kandydat | Decyzja | Uzasadnienie |
|---|---|---|
| TENANT_ALPHA (istniejący) | ACCEPT as-is | Aktywny normalny tenant |
| TENANT_BETA (nowy?) | REDIRECT → PLACEHOLDER_TENANT_ID zmień status na SUSPENDED | Seed już ma 3 tenantów — tylko zmiana statusu |
| TENANT_GAMMA_OR_SUSPENDED | REJECT jako osobny tenant | Wystarczy SUSPENDED status na istniejącym |

**Decyzja:** Nie dodawaj nowych tenantów w MVP. Zaktualizuj seed: `PLACEHOLDER_TENANT_ID`
→ wyraźna nazwa (np. `TENANT_BETA_SUSPENDED`) ze statusem `SUSPENDED`. Dodaj 1-2 Flyway
kolumny (contact_email, webhook_base_url) do `tenants` table w Phase 2.

---

## 6. Recommended Module Strategy

### Phase 1 (MVP) — bez nowych modułów

Rozszerz istniejące:
- `payment` moduł: dodaj 429 rate limiting z `Retry-After` header
- `shared` moduł: dodaj 304 Not Modified support w base response logic
- `testing` moduł: brak zmian potrzebnych (seed już obsługuje SUSPENDED tenant po seed update)

### Phase 2 — opcjonalny `support` moduł

Warunek: >3 cross-merchant endpointy lub cross-merchant query przekracza `payment` moduł scope.

```
support (nowy, Phase 2)
  ├─ cross-merchant payment search (platform:payments:read)
  ├─ internal notes per payment order
  └─ audit export endpoint (lub rozszerzenie audit modułu)
```

Zależności: `support` → `payment` PUBLIC API, `merchant` PUBLIC API, `audit` PUBLIC API

### Phase 3 — opcjonalny `risk` moduł lub rozszerzenie `merchant`

```
risk (opcjonalny, Phase 3)
  ├─ risk flags na merchantach
  ├─ risk review queue (flagged payment orders)
  └─ velocity rules engine (simple, configurable thresholds)
```

Alternatywnie: risk flags jako pole w `merchant` module bez osobnego modułu.
Decyzja: ADR-04.

### Moduły do odrzucenia

| Pomysł | Decyzja |
|---|---|
| IdempotencyModule | REJECT — idempotency żyje w payment domain |
| ETagModule | REJECT — ETag jest HTTP contract, nie moduł |
| BatchModule | REJECT — brak sensu biznesowego |
| HttpLabModule (backend) | REJECT — Error Lab frontend wystarczy |
| MethodOverrideModule | REJECT — tylko dokumentacja anti-pattern |

---

## 7. Recommended Tenant Strategy

### MVP (Phase 1)

```sql
-- Zmiana w seed data: PLACEHOLDER_TENANT_ID → TENANT_BETA
-- Status: SUSPENDED (TenantStatus.SUSPENDED już istnieje w kodzie)
-- Merchant pod nim: MERCHANT_BETA_001 (już istnieje w seed)
-- Efekt: testy tenant isolation już działają, UI pokazuje suspended banner
```

**Nie dodawaj nowych tenantów.** Istniejące 3 tenanty + zmiana statusu wystarczą dla:
- F-02: Tenant Suspension Evidence Page (TENANT_BETA jest SUSPENDED)
- F-04: Cross-Tenant Negative Tests (TENANT_ALPHA vs TENANT_BETA)
- F-05: Tenant-Scoped Audit Boundary (TENANT_ADMIN widzi tylko swój tenant)

### Phase 2

Dodaj do tabeli `tenants`:
- `contact_email VARCHAR(255)` — walidacja i webhook notifications
- `webhook_base_url VARCHAR(512)` — PSP callback URL
- `timezone VARCHAR(64)` — dla export formatowania dat

Flyway: `V8__add_tenant_settings_fields.sql`

---

## 8. Business Cases vs Technical Cases

### Kryteria klasyfikacji

| Kategoria | Krótki opis |
|---|---|
| **Production best practice** | Wzorzec stosowany w realnych systemach PSP/SaaS |
| **Acceptable compromise** | Kompromis akceptowalny gdy odpowiednio udokumentowany |
| **Legacy workaround** | Spotykany w praktyce, wynikający z ograniczeń infrastruktury |
| **Controlled lab case** | Celowo pokazany przypadek edukacyjny, jasno oznaczony |
| **Anti-pattern documentation** | Opis złego wzorca, BEZ implementacji produkcyjnej |
| **Reject as overengineering** | Koszt przewyższa wartość dla zakresu projektu |

### 8.1 Business Cases (mają sens w realnym PSP)

| Case | Klasyfikacja | Faza | Moduł |
|---|---|---|---|
| Partial capture/refund UI | Production best practice | Phase 1 | payment (extend) |
| Rate limiting 429 + Retry-After | Production best practice | Phase 1 | shared/payment |
| Tenant suspension banner | Production best practice | Phase 1 | frontend only |
| Tenant context indicator | Production best practice | Phase 1 | frontend only |
| Cross-tenant masked 404 | Production best practice | Phase 1 | frontend Error Lab |
| Merchant detail page | Production best practice | Phase 1 | frontend only |
| 304 Not Modified on GET | Production best practice | Phase 1 | payment (extend) |
| Payment status polling | Production best practice | Phase 2 | frontend only |
| Support cross-merchant search | Production best practice | Phase 2 | support (new) |
| CSV export payment orders | Production best practice | Phase 2 | payment (extend) |
| Upload evidence file | Production best practice | Phase 2 | support (new) |
| Internal notes per order | Production best practice | Phase 2 | support (new) |
| Audit before/after diff | Production best practice | Phase 2 | audit (extend) |
| Merchant risk flags | Production best practice | Phase 2/3 | merchant/risk |
| Risk review queue | Production best practice | Phase 3 | risk (new) |
| Payment Challenge Simulator | Controlled lab case | Phase 3 | frontend only |
| PSP iframe simulator | Controlled lab case | Phase 3 | frontend only |
| PSP new tab redirect | Controlled lab case | Phase 3 | frontend only |

### 8.2 Technical Cases (HTTP/REST education — z uzasadnieniem)

| Case | Klasyfikacja | Faza | Gdzie żyje |
|---|---|---|---|
| Idempotency replay demo | Controlled lab case | Phase 1 | Error Lab extension |
| Workflow-level idempotency (425) | Controlled lab case | Phase 1 | Error Lab extension |
| ETag capture w UI | Production best practice | Phase 1 | Error Lab/payment detail |
| If-None-Match / 304 | Production best practice | Phase 1 | Backend extension |
| PUT jako relation setter (role/watchlist) | Production best practice | Phase 2/3 | iam/risk extend |
| POST as read-only search | Acceptable compromise | Phase 2 | support module |
| PATCH idempotent (tenant settings) | Production best practice | Phase 2 | tenant extend |
| PATCH non-idempotent (risk score increment) | Controlled lab case | Phase 3 | Error Lab |
| RPC-like POST actions | Production best practice | existing+Phase 2 | payment/support |
| Webhook duplicate handling | Controlled lab case | Phase 3 | support/payment |
| DELETE vs POST /archive | Production best practice | Phase 2/3 | support (evidence) |
| CORS / OPTIONS / preflight | Production best practice | Document now | Error Lab future |
| GET side effect (view tracking) | Controlled lab case | Phase 3 | controlled scope only |
| Batch POST | Anti-pattern documentation | Never | DOCUMENT ONLY |
| Method Override | Anti-pattern documentation | Never | DOCUMENT ONLY |
| DELETE z body | Anti-pattern documentation | Never | DOCUMENT ONLY |
| GET unsafe action | Anti-pattern documentation | Never | DOCUMENT ONLY |
| QUERY method | Acceptable compromise (future) | Phase 3+ | Future consideration |
| Reconciliation drift | Reject as overengineering | Never for MVP | Needs settlement BE |

---

## 9. Production Patterns vs Lab Cases vs Anti-Patterns

### 9.1 Production Patterns (implement as real features)

**Idempotent POST lifecycle** — już istnieje. Eduacja: Error Lab replay demo.
**ETag + If-Match optimistic locking** — już istnieje. Edukacja: UI header panel + 412 trigger.
**304 Not Modified** — extend `PaymentOrderController.getPaymentOrder()` aby zwracał 304
gdy `If-None-Match` matches current ETag. Minimal change.
**PUT dla relacji** — `PUT /api/users/{id}/roles/{role}` zamiast `POST /api/users/{id}/roles`.
Idempotent: PUT drugiego razu tej samej roli nie zmienia stanu. Phase 2.
**POST /search** — `POST /api/support/payment-orders/search` — nie GET bo:
(1) kryteria wyszukiwania mogą mieć wrażliwe pola, (2) URL limit z wieloma filtrami.
Musi być: non-cacheable, dokumentacja że read-only, OpenAPI `summary: "Cross-merchant search"`.
**Archive command** — `POST /api/support/evidence/files/{id}/archive` zamiast DELETE.
Idempotent, audytowalny, reversible. Dla twardego usunięcia: `POST /api/support/evidence/files/{id}/deletion-jobs`.
**Rate limiting** — `429 Too Many Requests` + `Retry-After: <seconds>` + opcjonalnie
`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

### 9.2 Controlled Lab Cases (implement with explicit marking)

**Idempotency conflict (409)** — Error Lab trigger: ten sam klucz, inne body.
**Workflow idempotency (425)** — Error Lab trigger: request gdy poprzedni jest in-progress.
**Concurrency ETag fight (F-12)** — dwa okna Playwright, jeden dostaje 412 (Phase 3).
**Payment Challenge Simulator (F-37)** — mock OTP/bank-redirect/iframe bez PAN/PCI (Phase 3).
**GET side effect** — tylko jeśli dodany `view_count` / `last_seen_at` do audit (Phase 3, jasno dokumentowany).
**PATCH non-idempotent** — Error Lab: `"operation": "increment"` pokazuje dlaczego PATCH ≠ zawsze idempotentny.

### 9.3 Anti-Pattern Documentation (TYLKO dokumentacja, BEZ implementacji)

| Anti-pattern | Dlaczego odrzucony |
|---|---|
| Batch POST `/api/batch` | Brak sensu biznesowego; tooling compat problemy; debugowanie koszmarne |
| `X-HTTP-Method-Override` | WAF/gateway block risk; audit security gap; legacy workaround tylko |
| DELETE z body | Wiele proxy/libraries ignoruje body przy DELETE; użyj POST command zamiast |
| `GET /api/payment-orders/{id}/capture-by-get` | Łamie safe/idempotent semantics; cacheable operation z side effect |
| `GET /api/http-lab/unsafe-action?do=delete` | Nie implementować w żadnym endpoincie poza izolowanym lab route |
| `QUERY` HTTP method | Nie wspierany przez Spring 7; proxy compat niezgwarantowana; dokument jako future |

---

## 10. Feature Catalogue

### Priority Matrix

| ID | Nazwa | Faza | Koszt | Wartość SDET | Wartość Biznesowa |
|---|---|---|---|---|---|
| MVP-01 | Upgrade Playwright 1.61 | Phase 1 | Niski | Krytyczny | Niski |
| MVP-02 | Base fixture (console/pageerror guard, token leak) | Phase 1 | Niski | Krytyczny | Niski |
| MVP-03 | Fix `waitForTimeout` → `waitForResponse` | Phase 1 | Niski | Wysoki | Niski |
| MVP-04 | Multi-role auth setup (5 projektów) | Phase 1 | Średni | Wysoki | Niski |
| MVP-05 | APIRequestContext fixture (Tryb A: reset/seed) | Phase 1 | Niski | Wysoki | Niski |
| MVP-06 | Toast `data-testid` | Phase 1 | Niski | Średni | Niski |
| MVP-07 | 429 backend impl + Retry-After + Error Lab trigger | Phase 1 | Średni | Wysoki | Wysoki |
| MVP-08 | 304 Not Modified extension do GET /payment-orders/{id} | Phase 1 | Niski | Wysoki | Wysoki |
| MVP-09 | Error Lab: cross-tenant 403/masked-404 triggery | Phase 1 | Niski | Wysoki | Średni |
| MVP-10 | Error Lab: Idempotency replay + conflict triggery | Phase 1 | Niski | Wysoki | Wysoki |
| MVP-11 | Seed: PLACEHOLDER_TENANT_ID → TENANT_BETA SUSPENDED | Phase 1 | Niski | Średni | Wysoki |
| MVP-12 | Merchant Detail Page `/admin/merchants/[id]` | Phase 1 | Średni | Wysoki | Wysoki |
| MVP-13 | Tenant Context Indicator (header badge) | Phase 1 | Niski | Średni | Wysoki |
| MVP-14 | Partial capture/refund UI (amount input w lifecycle actions) | Phase 1 | Niski | Wysoki | Wysoki |
| P2-01 | PATCH `/api/tenants/{id}/settings` + Tenant Settings Page | Phase 2 | Wysoki | Wysoki | Wysoki |
| P2-02 | Support Search `POST /api/support/payment-orders/search` | Phase 2 | Wysoki | Wysoki | Wysoki |
| P2-03 | CSV Export `GET /api/merchants/{id}/payment-orders?format=csv` | Phase 2 | Średni | Wysoki | Wysoki |
| P2-04 | Upload Evidence `POST /api/support/evidence` | Phase 2 | Wysoki | Wysoki | Wysoki |
| P2-05 | Polling composable dla payment status | Phase 2 | Niski | Wysoki | Wysoki |
| P2-06 | Audit before/after diff (Flyway migration) | Phase 2 | Średni | Średni | Wysoki |
| P2-07 | Audit export `GET /api/audit?format=json` | Phase 2 | Niski | Wysoki | Wysoki |
| P2-08 | Internal notes per payment order | Phase 2 | Wysoki | Wysoki | Wysoki |
| P2-09 | Merchant risk flags | Phase 2 | Średni | Wysoki | Wysoki |
| P2-10 | Date Range Picker w filtrach | Phase 2 | Niski | Wysoki | Wysoki |
| P3-01 | Risk review queue + multi-role Playwright | Phase 3 | Wysoki | Expert | Wysoki |
| P3-02 | Concurrency ETag fight (F-12: 2 okna) | Phase 3 | Niski | Expert | Wysoki |
| P3-03 | Payment Challenge Simulator (iframe/OTP/redirect) | Phase 3 | Wysoki | Expert | Średni |
| P3-04 | Payment Expiration + Clock Mock | Phase 3 | Średni | Expert | Wysoki |
| P3-05 | PSP new tab redirect simulator | Phase 3 | Średni | Expert | Niski |
| P3-06 | ARIA Snapshot Tests | Phase 3 | Niski | Expert | Niski |
| P3-07 | Visual Regression (status badges) | Phase 3 | Niski | Expert | Niski |
| P3-08 | Token Leakage Guard (F-36) | Phase 3 | Niski | Expert | Wysoki |
| P3-09 | Merchant API Credentials Panel | Phase 3 | Wysoki | Expert | Wysoki |
| REJECT | Batch POST, Method Override, Reconciliation drift, Fake KPI | Never | — | — | — |

---

## 11. HTTP/REST Case Mapping

### A. Idempotentny POST dla płatności

**Klasyfikacja:** Production best practice

**Stan:** Już zaimplementowany (`IdempotencyRecord`, `IdempotencyKey`, 409/425).

**Brakuje:**
- UI demo w Error Lab (replay + conflict) — Phase 1, minimal
- Playwright test `waitForResponse` na replay scenario — Phase 1
- REST Assured test dla 425 Too Early (równoległe requesty) — Phase 1 lub restkit extension

**Playbook idempotency edge cases:**

| Case | HTTP Status | Implementacja |
|---|---|---|
| Pierwszy request | 201 Created | ✓ |
| Replay z tym samym kluczem i body | 200 OK + cache body | ✓ |
| Conflict: ten sam klucz, inne body | 409 Conflict | ✓ |
| In-progress: dwa równoległe requesty | 425 Too Early | ✓ (domain) — weryfikować HTTP |
| Expired idempotency record | 201 (new) | Weryfikować TTL logikę |

**Tooling compatibility:** ✓ OpenAPI OK, ✓ proxy OK, ✓ cache nie cachuje POST.

### B. POST jako read-only search/query

**Klasyfikacja:** Acceptable compromise

**Dlaczego nie GET:**
- Kryteria wyszukiwania mogą zawierać dane PII (email, reference) → nie w URL
- Więcej niż ~20 filtrów → URL limit 2083 znaki
- Możliwość future POST-body encryption

**Ryzyka:**
- CDN/cache ignoruje body → nigdy nie cachować POST search
- Reverse proxy może odcinać body przy GET-like headers
- OpenAPI: musi mieć `requestBody`, brak `parameters`

**Implementacja Phase 2:**
```
POST /api/support/payment-orders/search
Authorization: SUPPORT_AGENT lub PLATFORM_ADMIN (platform:payments:read)
Body: { "clientOrderReference": "...", "dateFrom": ..., "dateTo": ..., "status": [...], "amountMin": ..., "amountMax": ... }
Response: { "content": [...], "totalElements": ..., "page": ..., "size": ... }
Headers: X-Correlation-ID, Cache-Control: no-store
```

**Cache strategy:** `Cache-Control: no-store` — wymagane.
**Idempotency:** POST search nie wymaga Idempotency-Key (idempotent z natury — read only).

**Future:** Metoda `QUERY` (RFC 9110 extension) — nie wspierana przez Spring 7.
Dokumentuj że design jest gotowy na przyszłość ale nie implementuj teraz.

### C. Conditional GET / ETag / 304 Not Modified

**Klasyfikacja:** Production best practice

**Stan backend:**
- ETag generowany na GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId} ✓
- If-None-Match walidowany dla lifecycle actions ✓
- 304 Not Modified — NIE zwracany (brakuje sprawdzenia `If-None-Match` w GET)

**Zmiana Phase 1 (1 miejsce):**
```java
// PaymentOrderController.getPaymentOrder()
// Dodaj: sprawdzenie If-None-Match header i return 304 if matches
```

**Frontend panel edukacyjny:**
- `HeaderKeyValuePanel` pokazuje ETag w response
- Error Lab: trigger „304 Not Modified" — GET z If-None-Match = aktualny ETag → 304

**Playwright assertion:**
```ts
const response = await page.waitForResponse(r => r.url().includes('/payment-orders/'))
expect(response.headers()['etag']).toBeTruthy()
// Retry z If-None-Match → 304
expect(response.status()).toBe(304)
expect(await response.body()).toHaveLength(0) // brak body
```

**Różnica If-Match vs If-None-Match:**
- `If-Match: "v3"` — „zmodyfikuj tylko jeśli wciąż v3" → optymistyczne locking
- `If-None-Match: "v3"` — „daj mi dane tylko jeśli zmieniły się od v3" → conditional GET

### D. HEAD dla metadanych

**Klasyfikacja:** Production best practice

**Stan:** HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId} ✓

**Rozszerzenia Phase 2/3:**
- `HEAD /api/support/evidence/files/{id}` — Content-Length, Content-Type, Last-Modified
  (preflight przed download)
- `HEAD /api/reports/{id}/export` — czy raport jest gotowy do download

**Contract rules:**
- HEAD response headers muszą być identyczne z GET
- HEAD NIE może mieć body (nawet body {})
- Status: 200 (exists), 404 (not found), 403 (no access)

**REST Assured test:**
```java
given().header("Authorization", tokenFor(SUPPORT_AGENT))
  .when().head("/api/merchants/{m}/payment-orders/{p}", merchantId, orderId)
  .then().statusCode(200)
  .header("ETag", notNullValue())
  .header("Content-Length", greaterThan("0"))
  .body(emptyString());
```

### E. Webhook duplicate handling

**Klasyfikacja:** Controlled lab case (Phase 3)

**Dlaczego nie MVP:** Wymaga nowego endpointu webhook receiver + deduplikacja logika
+ PSP callback simulator. MockPspClient istnieje ale jest client-side, nie server-side.

**Design Phase 3:**
```
POST /api/webhooks/payment-events
Body: { "eventId": "...", "pspReference": "...", "eventCode": "CAPTURE", "status": "success" }
Headers: X-Webhook-Signature (HMAC-SHA256)
```

**Deduplikacja:** sprawdź czy eventId już przetworzony w `idempotency_records`.
**Signature validation:** przed deduplikacją — invalid signature = 401, duplicate = 200 (idempotent).

**Playwright test:**
```ts
// Wyślij webhook dwa razy z tym samym eventId
// Sprawdź: drugi request → 200 (idempotent), status zamówienia nie zmienił się drugi raz
```

**Ostrzeżenie:** Nie implementuj jako produkcyjny Kafka/outbox. Prosty POST endpoint
z HMAC validation i tabela idempotency records.

### F. Workflow-level idempotency

**Klasyfikacja:** Controlled lab case (Error Lab Phase 1)

**Istniejące elementy:**
- `IdempotencyCreateInProgressException` → 425 Too Early
- `MockPspClient` — symuluje call do PSP
- `IdempotencyRecord` — zapisuje result po sukcesie

**Scenariusze Error Lab (Phase 1):**
1. Normal flow: POST authorize → 200 + record
2. Replay: POST authorize (same key) → 200 + cached record
3. In-progress: POST authorize (same key, concurrent) → 425 Too Early
4. Conflict: POST authorize (same key, different body) → 409 Conflict

**Playwright:**
```ts
// Trigger 1: kliknij Replay Same → waitForResponse → sprawdź 200 + "cached: true"
// Trigger 2: kliknij Conflict → waitForResponse → sprawdź 409 + ProblemDetailsCard
```

**Crash recovery:** Poza scope MVP — MockPspClient zawsze succeeds. Do Phase 3.

### G. Reconciliation drift / transactional integrity

**Klasyfikacja:** Reject as overengineering dla MVP

**Dlaczego:** Wymaga:
- Real PSP (lub rozbudowanego MockPspClient ze stanem)
- Settlement scheduler
- Dedykowanego raportu rekoncyliacyjnego
- Cross-system state comparison

**Alternatywa edukacyjna (Phase 3):** Pokaż sceniariusz gdzie MockPspClient zwraca
inny status niż oczekiwano → audit log pokazuje rozbieżność. Bez osobnego dashboard.

### H. Replay-safe vs safe vs idempotent

**Klasyfikacja:** Controlled lab case — dokumentacja + Error Lab panels

**Terminologia (bez implementacji TLS 0-RTT):**

| Właściwość | Definicja | Przykład HTTP |
|---|---|---|
| Safe | Nie modyfikuje stanu serwera | GET, HEAD, OPTIONS |
| Idempotent | N wywołań = 1 wywołanie z perspektywy stanu | GET, HEAD, PUT, DELETE, POST z idempotency-key |
| Cacheable | Response może być cachowana | GET (bez Authorization), HEAD |
| Replay-safe | Duplikat nie tworzy duplikatu efektu | POST + Idempotency-Key |
| Side effect | Zmiana stanu poza głównym response | GET z view tracking (dopuszczalny jeśli celowy) |

**Frontend:** Tabela porównawcza w Error Lab lub sekcja "HTTP Concepts" (Phase 2).

### I. Batch endpoint

**Klasyfikacja:** Reject as overengineering

**Dlaczego:**
- Brak sensu biznesowego w backoffice PSP
- Atomicity problem: co gdy jeden subrequest fail?
- Tooling: OpenAPI nie ma standardu dla batch
- Debugging: trace single request vs batch jest nieporównywalny
- WAF: body inspection dla batch jest trudne

**Alternatywa:** `APIRequestContext` w Playwright dla API-driven setup — to nie batch,
ale spełnia potrzebę „wielu requestów".

### J. Method override (X-HTTP-Method-Override)

**Klasyfikacja:** Anti-pattern documentation + Legacy workaround documentation

**Nie implementować w produkcji.** Ryzyka:
- WAF może blokować lub ignorować override header
- Audit musi logować "effective method" a nie "transport method"
- Security: ukrywa DELETE/PATCH za POST — WAF bypass vector

**Jeśli naprawdę potrzebny** (stary klient nie obsługuje PATCH): dodaj jako explicitly
documented legacy gateway workaround, nie jako feature.

**Error Lab kontrolowany przypadek (Phase 3):**
```
POST /api/http-lab/method-override-demo
X-HTTP-Method-Override: PATCH
Body: { "field": "value" }
→ Response pokazuje: "Transport method: POST, Effective method: PATCH, Security risk: HIGH"
```

### K. DELETE vs archive vs tombstone vs deletion job

**Klasyfikacja zależna od kontekstu:**

| Wariant | Klasyfikacja | Kiedy użyć |
|---|---|---|
| `DELETE /evidence/files/{id}` | Production best practice | Hard delete — nie potrzebne w PSP |
| `POST /evidence/files/{id}/archive` | Production best practice | Soft archiwizacja, reversible |
| `DELETE ?mode=soft` | Anti-pattern documentation | Query param zmienia semantykę DELETE |
| `POST /evidence/files/{id}/deletion-jobs` | Production best practice | Async, audytowalny, z rate |
| DELETE z body | Anti-pattern documentation | Proxy ignorują body |

**Rekomendacja dla evidence files (Phase 2):**
`POST /api/support/evidence/{id}/archive` — idempotentny, audytowalny, bezpieczny.
Twardy DELETE wyłącznie przez `deletion-jobs` dla compliance records.

### L. PUT jako relation/state setter

**Klasyfikacja:** Production best practice

**Istniejące:** `POST /api/users/{id}/roles` — nie-idempotentny (?)

**Rekomendacja Phase 2:** Zmień na:
```
PUT  /api/users/{id}/roles/{role}    → idempotentne przypisanie roli (role już jest = 200, nie 409)
DELETE /api/users/{id}/roles/{role}  → idempotentne usunięcie roli
```

**Watchlist (Phase 3):**
```
PUT  /api/risk/watchlist/merchants/{merchantId}    → dodaj do watchlist
DELETE /api/risk/watchlist/merchants/{merchantId}  → usuń z watchlist
```

**Dlaczego PUT jest idempotentny tu:** PUT "merchant X jest na watchlist" = zawsze
ten sam stan końcowy niezależnie ile razy wywołane.

### M. PATCH idempotentny i nie-idempotentny

**Klasyfikacja:** Zależy od przypadku

**Idempotentny PATCH (Phase 2):**
```
PATCH /api/tenants/{id}/settings
Body: { "timezone": "Europe/Warsaw", "contactEmail": "admin@example.com" }
```
Jest idempotentny bo: ustawia konkretne wartości, nie inkrementuje.
Wymaga: `If-Match` + ETag validation (lub optimistic locking).
REST Assured: analogiczny pattern do payment order PATCH.

**Nie-idempotentny PATCH (Phase 3 — controlled lab case):**
```
PATCH /api/risk/payment-orders/{id}/score
Body: { "operation": "increment", "by": 10 }
```
NIE jest idempotentny — każde wywołanie zmienia wynik.
Edukacja: pokaż że PATCH to nie gwarancja idempotencji.
Rekomendacja: użyj zamiast tego `POST /api/risk/payment-orders/{id}/score-adjustments`.

**When `If-Match` is required:**
- PATCH z `@Version` field lub ETag → wymagaj `If-Match`
- PATCH bez state conflict risk (ustawienie timezone) → `If-Match` zalecany ale nie wymagany

### N. GET z side effectem

**Klasyfikacja:** Controlled lab case (Phase 3) jeśli bardzo celowy

**Przypadki dopuszczalne w realnym systemie:**
- Zmiana `last_accessed_at` na encji (analityka)
- Inkrementacja `view_count` (analityka, nie stan biznesowy)
- Zapis do access log (nie do stanu domenowego)

**Granica:** Side effect NIE może:
- Zmieniać stanu biznesowego (statusu, kwoty, flag)
- Być widzialny przez idempotentne powtórzenie GET
- Łamać cacheability gdy response jest cachowany

**Decyzja dla tego projektu:** Nie dodawaj `view_count` / `last_seen_at` do MVP.
Jeśli w Phase 3: tylko `support_last_viewed_by` na payment_orders, z dokumentacją
że to analityczny side effect, nie zmiana stanu.

### O. GET jako antywzorzec (unsafe GET)

**Klasyfikacja:** Anti-pattern documentation

**Nie implementować** `GET /api/payment-orders/{id}/capture-by-get`.
**Error Lab opcja (Phase 3, clearly marked):**
```
GET /api/http-lab/unsafe-action-demo?action=simulate-delete-attempt
Response: {
  "educationalNote": "Ten endpoint symuluje czego NIE robić",
  "requestedAction": "delete",
  "result": "REFUSED — unsafe actions rejected",
  "httpConcept": "GET must be safe and idempotent per RFC 9110"
}
```

Endpoint MUSI być bezpieczny (nie wykonuje akcji) — tylko zwraca edukacyjny response.

### P. OPTIONS / CORS / preflight

**Klasyfikacja:** Production best practice

**Stan:** CORS bean istnieje w `SecurityConfig.java` (dev profile).

**Luki:**
- Brak weryfikacji że `Idempotency-Key` jest w `Access-Control-Allow-Headers`
- Brak Error Lab triggera dla OPTIONS
- Brak REST Assured testu dla preflight

**Rekomendacja Phase 2:**
- Dodaj `Idempotency-Key` i `If-Match` do CORS allow-headers
- Error Lab trigger: OPTIONS request → pokazuje `Allow` header
- REST Assured: preflight test dla `/api/merchants` z `Origin: https://example.com`

**Ryzyko zbyt szerokiego CORS:**
Nie używać `Access-Control-Allow-Origin: *` dla authenticated endpoints.
Whitelist specific origins.

### Q. 429 / Retry-After / rate limit

**Klasyfikacja:** Production best practice

**Stan:** `PaymentRateLimitContractRestKitTest` istnieje w restkit — contract jest
zdefiniowany. Brakuje implementacji backend i Error Lab UI triggera.

**Implementacja Phase 1 (backend):**
```java
// Option A: Simple in-memory counter (edukacyjny, nie production-grade)
// Option B: @RateLimiter z Resilience4j
// Recommendation: Prosta implementacja dla labu — nie blokować MVP na production-grade rate limiter
// Endpoint: GET /api/error-lab/trigger-429 → zawsze zwraca 429
//           lub: rate limit na real endpoint (np. /api/merchants z low threshold)

// Response headers:
HTTP/1.1 429 Too Many Requests
Retry-After: 30
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1751104800
Content-Type: application/problem+json
{
  "type": "https://pay-lab.example.com/problems/rate-limit-exceeded",
  "status": 429,
  "title": "Too Many Requests",
  "detail": "Rate limit of 100 requests per minute exceeded."
}
```

**Error Lab trigger (Phase 1):**
Nowy scenario w scenarios array: `{ status: 429, title: 'Rate Limit', ... }`.
Frontend pokazuje: Retry-After w HeaderKeyValuePanel.

**Playwright assertion:**
```ts
await page.getByTestId('error-lab-trigger-429').click()
await expect(page.getByTestId('http-status-badge')).toContainText('429')
await expect(page.getByTestId('http-headers-panel')).toContainText('Retry-After')
```

### R. Reports Query / QUERY method

**Klasyfikacja:** POST jako search = Acceptable compromise. QUERY method = Future consideration.

**Phase 2:** `POST /api/reports/query` — raport generowany na żądanie z filtrami.
**Phase 3+:** Jeśli Spring 7 doda wsparcie dla QUERY method — prosta zmiana.

**QUERY method risks:**
- Nie wspierany przez Spring 7 (wymaga custom DispatcherServlet override)
- Niektóre proxy odrzucają nieznane HTTP methods
- OpenAPI 3.x nie ma oficjalnego QUERY support (draft)
- Decyzja: DOCUMENT jako future option, nie implementuj

### S. RPC-like POST actions

**Klasyfikacja:** Production best practice

**Już istnieje:** `/authorize`, `/capture`, `/cancel`, `/refund` — prawidłowe RPC-style commands.

**Reguła kiedy POST `/actions/...` jest uczciwy:**
- Operacja nie jest prostym CRUD
- Ma wyraźną intencję/semantykę biznesową
- Może generować audit event
- Nie pasuje do PUT/PATCH/DELETE semantics

**Phase 2 extensions:**
```
POST /api/support/payment-orders/{id}/notes              → add note
POST /api/support/evidence/{id}/archive                  → archive evidence
```

**Phase 3 extensions:**
```
POST /api/risk/payment-orders/{id}/review                → manual review decision
POST /api/risk/payment-orders/{id}/score-adjustments     → zamiast PATCH non-idempotent
```

### T. Tooling compatibility matrix

| Endpoint | OpenAPI | SDK gen | Reverse proxy | CDN/cache | WAF | CORS |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| `GET /payment-orders/{id}` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `POST /authorize` (RPC) | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ |
| `POST /support/payment-orders/search` | ✓ (requestBody) | ✓ | ✓ | ✗ (no cache) | Monitor | ✓* |
| `HEAD /payment-orders/{id}` | ✓ | Partial | ✓ | ✓ | ✓ | ✓ |
| `PATCH /tenants/{id}/settings` | ✓ | ✓ | ✓ | ✗ | ✓ | ✓* |
| `PUT /users/{id}/roles/{role}` | ✓ | ✓ | ✓ | ✗ | ✓ | ✓* |
| `DELETE /evidence/{id}` | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ |
| `GET /payment-orders?format=csv` | ✓ | ✓ | ✓ | ✗ (streaming) | ✓ | ✓ |
| Batch POST | ✗ | ✗ | Partial | ✗ | ✗ | Risk |
| X-HTTP-Method-Override | ✗ | ✗ | Risk | ✗ | BLOCK RISK | ✗ |

`*` = wymaga `Content-Type` i `Authorization` w CORS allow-headers

---

## 12. Frontend Architecture Impact

### 12.1 Phase 1 Frontend Changes

**Error Lab Extensions (minimal cost, high educational value):**

| Nowy trigger | Status | Route | Frontend component |
|---|---|---|---|
| Idempotency Replay (200) | NEW | `/api/error-lab/idempotency-replay` | scenarios array extension |
| Idempotency Conflict (409) | NEW | `/api/error-lab/idempotency-conflict` | scenarios array extension |
| Rate Limit (429) | NEW | `/api/error-lab/trigger-429` | scenarios array extension |
| Cross-tenant Read (masked 404) | NEW | existing endpoint, wrong tenant | scenarios array extension |
| Cross-tenant Write (403) | NEW | existing endpoint, wrong tenant | scenarios array extension |
| Workflow In-Progress (425) | NEW | `/api/error-lab/idempotency-in-progress` | scenarios array extension |

**Merchant Detail Page (`/admin/merchants/[id]`):**
- Route: `/admin/merchants/[merchantId]/index.vue`
- Components: `MerchantDetailTabs`, `PaymentOrderSummaryCards` (reuse existing)
- Composables: `useMerchantsApi` (reuse), nowy `useMerchantDetail`
- Zod: rozszerz istniejący schema o payment summary
- Playwright: `test.step`, tab navigation, async data loading

**Tenant Context Indicator:**
- Route: `layouts/dashboard.vue` — dodaj badge w header/sidebar
- Component: `TenantContextBadge` — prosty badge z tenant name z JWT claim
- Composable: rozszerz `useAuthorization` o `currentTenantName`
- Playwright: `getByTestId('tenant-context-badge')`, text assertion

**Partial capture/refund input:**
- Component: `PaymentOrderLifecycleActions.vue` — dodaj conditional amount input
- Composable: rozszerz `usePaymentLifecycleApi` o `amountMinor` param
- Zod: `CaptureRequest`, `RefundRequest` z opcjonalnym `amountMinor`

**Toast data-testid:**
- 1 linia per toast call: `{ id: 'toast-success', ... }`

### 12.2 Phase 2 Frontend Changes

**Support Search Page:**
- Route: `/admin/support/search/index.vue`
- Components: `SupportSearchBox`, `SupportSearchResultsTable`, `SearchFiltersPanel`
- Composable: `useSupportSearchApi` — POST body, debounce 300ms
- Zod: `SupportSearchRequestSchema`, `SupportSearchResultSchema`
- Server proxy: `server/api/support/payment-orders/search.post.ts`
- UI states: loading (debounce), empty (no query), no results, results table, error
- RBAC: PLATFORM_ADMIN lub SUPPORT_AGENT tylko
- Playwright: `fill` → debounce → `expect.poll(() => table.count())` → click result

**Polling composable:**
- `usePaymentStatusPolling` — configurowalny interval, `onUpdate` callback
- Playwright: `expect.poll(() => statusBadge.textContent())` → sekwencja: AUTHORIZED → CAPTURED

**CSV Export:**
- `ExportCsvButton.vue` — przyciski w `PaymentOrderListTable`
- Server proxy: `server/api/merchants/[merchantId]/payment-orders/export.get.ts`
- Playwright: `page.waitForEvent('download')`, `download.path()`, CSV parse in test

**Evidence Upload:**
- `EvidenceUploadDropzone.vue` — dropzone z `<input type="file">`
- Server proxy: `server/api/support/evidence.post.ts` (multipart forward)
- Playwright: `page.on('filechooser')`, `fileChooser.setFiles('test-file.pdf')`

**Audit before/after diff:**
- Rozszerz `AuditEntryDrawer.vue` o diff panel
- `AuditDiffPanel.vue` — before/after JSON side-by-side

**Tenant Settings Page:**
- Route: `/admin/tenant/settings/index.vue`
- Component: `TenantSettingsForm.vue`
- ETag flow: GET → ETag → PATCH + If-Match → 412/200
- Playwright: fill, submit, stale 412 scenario, `EtagDisplay` assertion

### 12.3 Phase 3 Frontend Changes

**PSP Challenge Simulator:**
- Route: `/admin/payment-challenge-simulator` (lub jako modal w payment detail)
- Iframe: sandboxed page `public/psp-challenge-simulator.html`
- Playwright: `page.frameLocator('#psp-challenge-iframe')`, `fill`, `click`

**PSP New Tab Redirect:**
- Route: `public/psp-redirect-simulator.html` (brak auth)
- Playwright: `context.waitForPage()`, multi-tab assertions

**Date Range Picker:**
- `DateRangePicker.vue` — Nuxt UI `UPopover` + mini-calendar
- Playwright: `getByRole('dialog')`, keyboard navigation, URL sync assertion

### 12.4 Frontend Architecture Checklist per Feature

Dla każdej nowej funkcji frontendowej:

- [ ] Route/page: czy strona istnieje czy nowa?
- [ ] Components: rozszerz istniejące zamiast tworzyć nowe
- [ ] Composables: jeden composable per domain area, nie per endpoint
- [ ] Zod schemas: waliduj response przed użyciem w UI
- [ ] Server proxy routes: każdy backend endpoint ma `server/api/` odpowiednik
- [ ] UI states: loading, empty, error, forbidden, conflict, success
- [ ] RBAC visibility: `v-if` dla role-gated elements (nie `v-show`)
- [ ] HTTP learning panels: `HeaderKeyValuePanel`, `ProblemDetailsCard` tam gdzie edukcja HTTP
- [ ] `data-testid`: na triggerach, tabelach, badge'ach, formularzach, toast'ach
- [ ] Authorization: Header zawsze zamaskowany w `HeaderKeyValuePanel`
- [ ] Playwright testability: minimum getByRole/getByLabel/getByTestId — bez CSS selectors
- [ ] A11y: `aria-label` na icon-only buttons, `aria-live` na async updates
- [ ] Token leak: brak tokenów w DOM, localStorage, console.log

### 12.5 RBAC UI Pattern Decision Matrix

Dla każdego nowego elementu UI z RBAC — wybierz jeden z 3 wzorców:

| Pattern | Kiedy | Playwright assertion |
|---|---|---|
| Hidden (`v-if`) | Rola nie powinna wiedzieć że akcja istnieje | `expect(button).toBeHidden()` |
| Disabled | Rola widzi ale nie może użyć (stan, lifecycle) | `expect(button).toBeDisabled()` |
| Visible-403 (Error Lab) | Celowe demo enforcement backend — edukacja | `expect(badge).toContainText('403')` |

**Zakaz:** Nie używaj Pattern 3 dla produkcyjnych chronionych akcji — to Error Lab only.

---

## 13. Backend / Modulith Impact

### 13.1 Phase 1 Backend Changes

**Rate limiting (payment lub shared moduł):**
```java
// Option: Simple @RateLimiter(name = "errorLab") from Resilience4j
// lub: dedykowany /api/error-lab/trigger-429 endpoint który zawsze zwraca 429

// Nowy endpoint w error-lab context:
// GET /api/error-lab/trigger-429
// Response: 429 + Retry-After: 30 + X-RateLimit-* headers + problem+json
```

**304 Not Modified extension:**
```java
// PaymentOrderController.getPaymentOrder():
// Dodaj sprawdzenie:
// if (request.getHeader("If-None-Match") != null && etagMatches) {
//   return ResponseEntity.status(304).build();
// }
```

**Seed update (TENANT_BETA SUSPENDED):**
```java
// TenantSeedService lub DeterministicDataset:
// Zmień PLACEHOLDER_TENANT_ID z ACTIVE na SUSPENDED
// Wyraźna nazwa stała: TENANT_BETA_ID lub SUSPENDED_TENANT_ID
```

### 13.2 Phase 2 Backend Changes

**Nowe endpointy:**
```
PATCH /api/tenants/{id}/settings          → tenant moduł extension
POST  /api/support/payment-orders/search  → support moduł (nowy)
GET   /api/merchants/{id}/payment-orders?format=csv → payment moduł extension
POST  /api/support/evidence               → support moduł (nowy)
GET   /api/support/evidence/{id}          → support moduł
POST  /api/support/evidence/{id}/archive  → support moduł
GET   /api/payment-orders/{id}/notes      → support moduł
POST  /api/payment-orders/{id}/notes      → support moduł
GET   /api/audit?format=json              → audit moduł extension
```

**Nowe Flyway migrations:**
```
V8__add_tenant_settings_fields.sql       → contact_email, webhook_base_url, timezone
V9__create_evidence_files.sql            → evidence_files table
V10__create_payment_order_notes.sql      → payment_order_notes table
V11__add_audit_before_after_state.sql    → before_state, after_state JSONB
V12__add_merchant_risk_flag.sql          → risk_flag, risk_flagged_at, risk_flagged_by
```

### 13.3 Spring Modulith Architecture Rules

- `support` moduł → może zależeć od: `payment` PUBLIC API, `merchant` PUBLIC API, `audit` PUBLIC API
- `support` moduł → NIE może importować `payment.internal.*`
- `risk` moduł (Phase 3) → może zależeć od: `merchant` PUBLIC API, `payment` PUBLIC API
- `ModulithArchitectureTest` musi przechodzić po każdym nowym module
- Nowe eventy domenowe: `EvidenceArchivedEvent`, `RiskFlagAddedEvent` → przez `AuditableActionOccurred`

---

## 14. Security, RBAC and Tenant Isolation Matrix

### 14.1 Authority → Endpoint Matrix (Phase 1 + Phase 2)

| Endpoint | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY_USER |
|---|:---:|:---:|:---:|:---:|:---:|
| GET /api/status | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/merchants | ✓ | | | | |
| GET /api/merchants | ✓ | ✓(own) | | ✓ | ✓ |
| POST /api/merchants/{id}/activate | ✓ | | | | |
| POST /api/merchants/{id}/suspend | ✓ | | | | |
| POST /api/merchants/{id}/payment-orders | ✓ | ✓ | ✓(own) | | |
| GET /api/merchants/{id}/payment-orders | ✓ | ✓(own) | ✓(own) | ✓ | ✓(own) |
| POST /api/merchants/{id}/payment-orders/{id}/authorize | ✓ | ✓ | ✓(own) | | |
| GET /api/audit | ✓ | ✓(own tenant) | | ✓ | |
| GET /api/users | ✓ | ✓(own tenant) | | | |
| **Phase 2:** PATCH /api/tenants/{id}/settings | ✓ | ✓(own) | | | |
| **Phase 2:** POST /api/support/payment-orders/search | ✓ | | | ✓ | |
| **Phase 2:** GET ...?format=csv | ✓ | ✓(own) | ✓(own) | ✓ | |
| **Phase 2:** POST /api/support/evidence | ✓ | | | ✓ | |
| **Phase 2:** POST /api/payment-orders/{id}/notes | ✓ | | | ✓ | |
| **Phase 3:** POST /api/risk/payment-orders/{id}/review | ✓ | | | ✓ | |

### 14.2 Tenant Isolation Rules

Wszystkie endpointy z tenant-scoped data:
1. Sprawdź `tenantId` z JWT claim
2. Dla PLATFORM_ADMIN: brak filtra tenant (dostęp do wszystkich)
3. Dla TENANT_ADMIN: filtr `WHERE tenant_id = :jwtTenantId`
4. Cross-tenant read: masked 404 (nie 403) — nie ujawniaj istnienia zasobu

**Reguła masked 404:**
- Zasób istnieje ale należy do innego tenanta → 404 (nie 403)
- Zasób nie istnieje → 404
- UI: ProblemDetailsCard z typem `resource-not-found`, NIE `forbidden`

**Reguła `support` module:**
- `SUPPORT_AGENT` używa `platform:payments:read` — dostęp cross-merchant
- Ale `SUPPORT_AGENT` NIE ma dostępu cross-tenant (tylko w ramach platformy)

### 14.3 Security Test Requirements per Feature

Każda nowa funkcja wymaga:
1. Unauthenticated (401): brak JWT
2. Wrong role (403): JWT z rolą bez authority
3. Cross-tenant (masked 404): JWT z innym tenantId
4. Authorization header masking w UI (never expose token)
5. Token leak assertion w base fixture (F-36 guard)

---

## 15. API Contract Checklist

### Dla każdego nowego endpointu:

**Request:**
- [ ] HTTP method jest poprawny semantycznie (safe/idempotent/cacheable)
- [ ] Path używa rzeczowników (nie czasowników), wyjątek: `/actions/`, `/commands/`
- [ ] Body schema jest zwalidowany (`@Valid`, Zod na frontendzie)
- [ ] Idempotency-Key header wymagany dla non-idempotent POST lifecycle actions
- [ ] If-Match wymagany dla operacji z ETag (PATCH, lifecycle actions)
- [ ] Content-Type jest sprawdzany (415 jeśli niezgodny)

**Response:**
- [ ] 2xx z Location header dla 201 Created
- [ ] ETag w response dla zasobów z versioning
- [ ] Cache-Control: no-store dla danych finansowych i PII
- [ ] X-Correlation-ID w każdej odpowiedzi
- [ ] application/problem+json dla błędów (nie ad-hoc JSON)
- [ ] Problem type URI jest stabilny i dokumentowany
- [ ] Retry-After w 429 odpowiedziach
- [ ] Allow header w 405 Method Not Allowed
- [ ] Vary header gdy response zależy od Accept/Accept-Language

**Security:**
- [ ] 401 dla brakującego/expired JWT
- [ ] 403 dla prawidłowego JWT ale brak authority
- [ ] 404 (masked) dla cross-tenant access
- [ ] Rate limiting jeśli endpoint jest public-facing lub drogi

**OpenAPI:**
- [ ] Endpoint jest udokumentowany w OpenAPI spec (jeśli generowana)
- [ ] Request/response schemas są kompletne
- [ ] Security requirements są zaznaczone

---

## 16. Frontend Architecture Checklist

### Dla każdej nowej strony/komponentu:

**Dostępność (A11y):**
- [ ] `aria-label` na icon-only buttons
- [ ] `aria-live="polite"` na async status updates (loading → loaded)
- [ ] Właściwe heading hierarchy (h1 → h2 → h3)
- [ ] Tab navigation przez wszystkie interaktywne elementy
- [ ] Focus trap w modal/drawer (Nuxt UI obsługuje automatycznie)

**Testability:**
- [ ] `data-testid` na: tabelach, badge'ach, triggerach, formularzach, przyciskach akcji
- [ ] `getByRole` możliwy dla: button, link, heading, cell, row
- [ ] `getByLabel` możliwy dla: wszystkich form inputs
- [ ] Brak CSS-only locators w komponentach (nie `className="status-badge"` jako jedyny identifier)

**HTTP Learning Panels:**
- [ ] `HeaderKeyValuePanel` pokazuje response headers po każdym API call
- [ ] `ProblemDetailsCard` pojawia się dla każdego błędu 4xx/5xx
- [ ] `HttpStatusBadge` jest widoczny z numerem statusu
- [ ] `EtagDisplay` jest widoczny gdy zasób ma ETag
- [ ] Authorization header zawsze zamaskowany (`Bearer ••••••••`)

**States:**
- [ ] Loading state: `LoadingState.vue` lub `USkeleton`
- [ ] Empty state: `EmptyStateCard.vue` z call-to-action
- [ ] Error state: `ErrorState.vue` z retry button
- [ ] Forbidden state: redirect do `/forbidden` lub inline message
- [ ] Conflict state (412): jasny komunikat + jak odświeżyć ETag

---

## 17. REST Assured Learning Matrix

| Capability | Obecny stan | Phase 1 extension | Phase 2 extension |
|---|---|---|---|
| `statusCode(200,201,204)` | ✓ | — | — |
| `statusCode(304)` | BRAK | Dodaj do payment order GET test | — |
| `statusCode(400,401,403,404)` | ✓ | — | — |
| `statusCode(409,412,428)` | ✓ | — | — |
| `statusCode(415)` | BRAK | Error Lab test | — |
| `statusCode(425)` | BRAK | Idempotency in-progress test | — |
| `statusCode(429)` | Contract (restkit) | Backend impl test | — |
| `header("ETag")` | ✓ | — | — |
| `header("Retry-After")` | BRAK | 429 impl test | — |
| `header("Cache-Control: no-store")` | ✓ | — | — |
| `header("Allow")` | BRAK | 405 test | — |
| `header("Vary")` | ✓ | — | — |
| `header("Location")` | ✓ | — | — |
| Conditional GET (If-None-Match) | BRAK | 304 test | — |
| POST as search body validation | BRAK | — | Support search test |
| File download (content-type CSV) | BRAK | — | CSV export test |
| Multipart upload | BRAK | — | Evidence upload test |
| ETag chain (GET → PATCH) | ✓ | — | PATCH settings |
| Idempotency chain | ✓ | 425/conflict | — |
| Property-based (jqwik) | ✓ | — | — |
| DB state after write | ✓ | — | — |
| WireMock PSP stub | ✓ (IAM) | — | — |
| Security matrix annotations | ✓ | — | — |

---

## 18. Playwright Learning Matrix

### Tier przypisanie

| Test | Tier | Nowe API | Phase |
|---|---|---|---|
| Fix waitForTimeout → waitForResponse | Tier 1 | `waitForResponse` | Phase 1 |
| Base fixture (console/pageerror) | All Tiers | `page.on('console')`, `page.on('pageerror')` | Phase 1 |
| Multi-role storageState (5 projektów) | Tier 1+3 | `workerStorageState`, `test.extend` | Phase 1 |
| APIRequestContext (reset/seed) | Tier 4 | `request.post()`, `playwright.request.newContext` | Phase 1 |
| Toast auto-dismiss | Tier 1 | `expect.poll`, `toBeHidden()` | Phase 1 |
| Error Lab 429 + header assert | Tier 1 | `toContainText`, `waitForResponse` | Phase 1 |
| Error Lab idempotency replay | Tier 1 | `waitForResponse`, response body comparison | Phase 1 |
| Error Lab cross-tenant | Tier 1 | `toContainText('403')`, `ProblemDetailsCard` | Phase 1 |
| 304 Not Modified header assert | Tier 2 | `response.status() === 304`, `response.body().length === 0` | Phase 1 |
| Merchant detail tabs | Tier 1 | `getByRole('tab')`, `expect(panel).toBeVisible()` | Phase 1 |
| Dialog handling (ConfirmActionModal) | Tier 1 | `getByRole('dialog')`, button click in DOM modal | Phase 1 |
| Support search debounce | Tier 2 | `expect.poll(() => table.count())` | Phase 2 |
| CSV download | Tier 3 | `page.waitForEvent('download')`, `download.path()` | Phase 2 |
| Evidence file upload | Tier 3 | `page.on('filechooser')`, `fileChooser.setFiles()` | Phase 2 |
| Payment status polling | Tier 2 | Sequential `route.fulfill()`, `expect.poll` | Phase 2 |
| Retry 503→200 | Tier 1 | Stateful `route.fulfill()` | Phase 2 |
| Audit export download + JSON parse | Tier 3 | `download.path()`, Node.js JSON.parse | Phase 2 |
| Date picker calendar | Tier 1 | `getByRole('dialog')`, keyboard nav | Phase 2 |
| Mobile viewport | Tier 1 | `page.setViewportSize()` | Phase 2 |
| ETag capture from response | Tier 2 | `page.waitForResponse()` → `.headers()['etag']` | Phase 2 |
| Concurrency ETag fight (2 pages) | Tier 3 | `browser.newContext()`, multi-page | Phase 3 |
| PSP iframe simulator | Tier 3 | `frameLocator('#psp-iframe')`, `frame.fill()` | Phase 3 |
| PSP new tab redirect | Tier 3 | `context.waitForPage()`, `page.close()` | Phase 3 |
| Clock mock (payment expiration) | Tier 3 | `page.clock.fastForward()` | Phase 3 |
| ARIA snapshot | Tier 1 | `toMatchAriaSnapshot()` (dostępne od 1.49+) | Phase 3 |
| Visual regression (badge) | Tier 1 | `toHaveScreenshot()` | Phase 3 |
| Token leak guard | All Tiers | `page.evaluate(() => document.body.innerText)`, JWT pattern | Phase 3 |
| localStorage scan | All Tiers | `page.localStorage.items()` (🔒 wymaga 1.61) | Phase 3 |
| console token guard | All Tiers | `page.on('console', msg => JWT_check)` | Phase 1 (base fixture) |
| Worker parallel (fullyParallel: true) | All Tiers | Worker-aware data isolation | Phase 3 |
| Project sharding (--shard) | CI | `--shard=1/4` | Phase 4 |

---

## 19. Roadmap: MVP (Phase 1)

**Cel:** Fundamenty Playwright + HTTP Lab extensions + partial capture UI + multi-role

**Czas szacowany:** 2–4 sprinty

### P1-01: Playwright Fundamenty (brak zmian backend/frontend)

- [ ] Upgrade `@playwright/test: "1.60.0"` → `"1.61.x"` w `package.json`
- [ ] Smoke: `foundation.spec.ts` + `auth-deny.spec.ts` + `corepack pnpm typecheck`
- [ ] `playwright.config.ts`: dodaj `screenshot: 'only-on-failure'`, `video: 'retain-on-failure'`
- [ ] Napraw `waitForTimeout(500)` → `waitForResponse` w `payment-order-create.spec.ts:16`
- [ ] Stwórz `tests/fixtures/base.fixture.ts` z console/pageerror guard + JWT token leak detection
- [ ] Stwórz `tests/fixtures/api.fixture.ts` z APIRequestContext (Tryb A: reset/seed bez tokena)
- [ ] Multi-role auth setup: 5 projektów w `playwright.config.ts`, selektywne tagi (@platform-admin etc.)
- [ ] Stwórz `tests/.auth/` dla 5 ról (początkowo empty JSON, wypełnione przez auth.setup.ts)

### P1-02: Error Lab Extensions (backend + frontend)

- [ ] Backend: `GET /api/error-lab/trigger-429` → 429 + Retry-After + X-RateLimit-* + problem+json
- [ ] Backend: `GET /api/error-lab/trigger-425` → 425 + problem+json (idempotency in-progress)
- [ ] Backend: Error Lab idempotency replay endpoint
- [ ] Backend: Error Lab idempotency conflict endpoint
- [ ] Backend: Error Lab cross-tenant endpoints (wywołaj istniejące z celowo złym tokenem)
- [ ] Frontend: 5 nowych scenarios w `error-lab.vue` scenarios array
- [ ] Frontend: Nuxt server proxy routes dla nowych triggerów
- [ ] Playwright test: nowe testy dla 429, 425, idempotency replay, cross-tenant

### P1-03: 304 Not Modified (backend)

- [ ] Backend: `PaymentOrderController.getPaymentOrder()` — sprawdź `If-None-Match`, zwróć 304
- [ ] REST Assured: test conditional GET → 304 + empty body
- [ ] Error Lab trigger: conditional GET demo (Phase 1 lub Phase 2)

### P1-04: Seed Update (TENANT_BETA SUSPENDED)

- [ ] `TenantSeedService` / `DeterministicDataset`: zmień PLACEHOLDER_TENANT_ID na SUSPENDED
- [ ] Stałe: `TENANT_BETA_ID` zamiast `PLACEHOLDER_TENANT_ID`
- [ ] Test izolacji tenant: weryfikacja że MERCHANT_BETA_001 jest under SUSPENDED tenant
- [ ] Frontend: tenant suspension banner (`TenantSuspensionBanner.vue` lub middleware guard)

### P1-05: Frontend Quick Wins

- [ ] Toast `data-testid` (1 linia per toast)
- [ ] Tenant Context Indicator badge w `dashboard.vue`
- [ ] Merchant Detail Page `/admin/merchants/[merchantId]/index.vue`
- [ ] Partial capture/refund amount input w `PaymentOrderLifecycleActions.vue`
- [ ] Minimalny POM: `tests/pages/ErrorLabPage.ts`, `tests/components/ProblemDetailsCard.ts`

---

## 20. Roadmap: Phase 2

**Cel:** Senior SDET features — multi-role, file ops, search, risk flags, tenant settings

**Czas szacowany:** 4–6 sprintów

### P2-A: Tenant Settings (backend + frontend)

- [ ] Flyway: `V8__add_tenant_settings_fields.sql`
- [ ] Backend: `PATCH /api/tenants/{id}/settings` z ETag + If-Match
- [ ] Backend: `GET /api/tenants/{id}` (public info)
- [ ] REST Assured: ETag flow, 412 stale, 428 missing
- [ ] Frontend: `/admin/tenant/settings` + `TenantSettingsForm.vue`
- [ ] Playwright: fill, submit, ETag stale scenario, RBAC: TENANT_ADMIN vs MERCHANT_MANAGER

### P2-B: Support Module (nowy moduł)

- [ ] `support` moduł Spring Modulith
- [ ] `POST /api/support/payment-orders/search` (platform:payments:read)
- [ ] `POST /api/support/evidence` (multipart/form-data)
- [ ] `GET/POST /api/payment-orders/{id}/notes`
- [ ] Flyway: `V9__create_evidence_files.sql`, `V10__create_payment_order_notes.sql`
- [ ] Frontend: `/admin/support/search`, `EvidenceUploadDropzone.vue`, `PaymentOrderNotes.vue`
- [ ] REST Assured: search security matrix, upload validation
- [ ] Playwright: search debounce (`expect.poll`), file upload (`filechooser`), multi-role notes

### P2-C: CSV Export (payment moduł extension)

- [ ] Backend: `GET /api/merchants/{id}/payment-orders?format=csv` (streaming)
- [ ] Frontend: `ExportCsvButton.vue`, server proxy streaming route
- [ ] Playwright: `page.waitForEvent('download')`, CSV content assertion

### P2-D: Audit Extensions

- [ ] Flyway: `V11__add_audit_before_after_state.sql`
- [ ] Backend: populate `before_state`/`after_state` w `AuditEventListener`
- [ ] Backend: `GET /api/audit?format=json` (export)
- [ ] Frontend: `AuditDiffPanel.vue` w `AuditEntryDrawer`, `ExportAuditButton.vue`
- [ ] Playwright: drawer navigation, diff panel visibility, download JSON parse

### P2-E: Merchant Risk Flags

- [ ] Flyway: `V12__add_merchant_risk_flag.sql`
- [ ] Backend: `PATCH /api/merchants/{id}/risk-flag` (PLATFORM_ADMIN only)
- [ ] Frontend: `RiskFlagBadge.vue`, RBAC-conditional toggle
- [ ] Playwright: multi-role — PLATFORM_ADMIN widzi toggle, MERCHANT_MANAGER nie

### P2-F: Advanced Playwright Patterns

- [ ] Payment status polling test (sequential mocks → status transitions)
- [ ] Retry 503→200 test (stateful route.fulfill)
- [ ] ETag capture z response headers w Playwright
- [ ] Audit export download test + JSON parse
- [ ] Date Range Picker test (calendar keyboard navigation)
- [ ] Mobile viewport test (`page.setViewportSize`)
- [ ] Full POM: `MerchantsPage.ts`, `PaymentOrdersPage.ts`, `SupportSearchPage.ts`

---

## 21. Roadmap: Phase 3

**Cel:** Expert SDET — concurrency, iframe, clock, visual, token security

**Czas szacowany:** 4–6 sprintów

### P3-A: Risk Review Queue

- [ ] `risk` moduł (lub rozszerzenie `merchant`)
- [ ] `GET /api/risk/review-queue`, `POST /api/risk/payment-orders/{id}/review`
- [ ] Frontend: `/admin/risk`, `RiskReviewTable.vue`
- [ ] Playwright: multi-role (SUPPORT_AGENT vs MERCHANT_MANAGER), filter, ConfirmModal

### P3-B: Expert Playwright Scenarios

- [ ] Concurrency ETag fight: `browser.newContext()`, 2 strony, 412 assertion
- [ ] Payment Challenge Simulator: iframe (`frameLocator`), OTP mock
- [ ] PSP new tab redirect: `context.waitForPage()`, multi-tab
- [ ] Payment Expiration: `page.clock.fastForward()`, EXPIRED status assertion
- [ ] Token Leakage Guard (F-36): DOM scan, console scan, localStorage scan (1.61)
- [ ] ARIA Snapshot: `toMatchAriaSnapshot()` dla kluczowych komponentów
- [ ] Visual Regression: `toHaveScreenshot()` dla status badges

### P3-C: Advanced HTTP Cases

- [ ] Webhook duplicate handling endpoint (`POST /api/webhooks/payment-events`)
- [ ] Non-idempotent PATCH controlled lab case
- [ ] GET side effect demo (Phase 3, celowy)
- [ ] OPTIONS/CORS Error Lab trigger
- [ ] `PUT /api/users/{id}/roles/{role}` → `DELETE /api/users/{id}/roles/{role}` migration

### P3-D: Test Infrastructure

- [ ] `fullyParallel: true` (po data isolation strategy)
- [ ] Worker-aware data ownership (Merchant per worker)
- [ ] `test.describe.configure({ mode: 'serial' })` dla lifecycle chain
- [ ] `testInfo.attach()` pattern z maskowaniem tokena
- [ ] HAR recording w `browser.newContext({ recordHar })`

---

## 22. ADR-Style Decisions

### ADR-01: Support module — osobny moduł vs rozszerzenie payment

**Status:** ZATWIERDZONO (Phase 2)

**Kontekst:** Cross-merchant search, evidence upload, notes — wszystkie wymagają dostępu
do `payment` i `merchant` danych, ale z inną authority (`platform:payments:read`).

**Decyzja:** Utwórz osobny `support` moduł gdy cross-merchant endpointów będzie ≥3.
Zależności: `support` → `payment` PUBLIC API + `merchant` PUBLIC API.
NIE importuj `payment.internal.*`.

**Konsekwencje:** Nowy `package-info.java`, nowe `server/api/support/` routes w Nuxt,
`ModulithArchitectureTest` musi przejść.

---

### ADR-02: Cross-merchant search — GET vs POST

**Status:** ZATWIERDZONO (Phase 2)

**Kontekst:** Support szuka płatności po wielu filtrach bez merchantId.

**Decyzja:** `POST /api/support/payment-orders/search` z body.
Uzasadnienie: potencjalne PII w URL, >10 filtrów, łatwiejsze audytowanie.
`Cache-Control: no-store` obowiązkowe. Brak Idempotency-Key (read-only).

**Dokumentacja:** Komentarz w OpenAPI: `"This endpoint is read-only despite using POST."`

---

### ADR-03: fullyParallel: true — kiedy włączyć

**Status:** ODROCZONE (Phase 3)

**Kontekst:** Testy Playwright z `fullyParallel: false` nie ćwiczą parallel patterns.

**Decyzja:** Włącz `fullyParallel: true` dopiero gdy:
1. Worker-aware data isolation strategy jest wdrożona (merchant per worker)
2. APIRequestContext reset/seed fixture jest stabilny
3. Żaden test nie dzieli mutable state z innym testem

**Warunek blokujący:** Shared seed data jest dostępna do zapisu przez wiele workerów.

---

### ADR-04: Risk flags — merchant moduł vs risk moduł

**Status:** OTWARTE

**Opcja A:** Risk flags jako pola w `merchant` moduł (risk_flag, risk_score kolumny).
Pro: prosta implementacja, brak nowego modułu.
Con: `merchant` moduł staje się zbyt szeroki gdy dojdzie risk review queue.

**Opcja B:** Nowy `risk` moduł zależny od `merchant` PUBLIC API.
Pro: czysty bounded context, risk rośnie niezależnie.
Con: overhead modułu dla 2–3 pól w Phase 2.

**Rekomendacja:** Opcja A dla Phase 2 (risk_flag na merchant). Opcja B dla Phase 3
gdy risk review queue + velocity rules przekraczają `merchant` zakres.

---

### ADR-05: Playwright base fixture — co obowiązkowe

**Status:** ZATWIERDZONO (Phase 1)

Obowiązkowe w `base.fixture.ts`:
1. `page.on('console')` — detection tokena JWT w console
2. `page.on('pageerror')` — zbieranie błędów JS
3. `testInfo.attach('console-log', ...)` — per-test attachment
4. Masowanie Authorization w każdym attachment

Nie-obowiązkowe (dodawać później):
- `page.on('request')` — zbyt głośne, opt-in w konkretnych testach
- Video/screenshot — konfiguracja playwright.config.ts, nie fixture

---

### ADR-06: Tenant SUSPENDED handling w froncie

**Status:** ZATWIERDZONO (Phase 1)

**Opcja A:** Middleware guard w `auth.global.ts` — blokuje wszystkie strony dla SUSPENDED tenant.
**Opcja B:** Inline banner na każdej stronie admin.
**Opcja C:** Dashboard layout guard — pokazuje banner, nie blokuje nawigacji.

**Decyzja:** Opcja C — `TenantSuspensionBanner.vue` w `layouts/dashboard.vue`.
Merchant create/activate buttons są disabled gdy tenant suspended (nie hidden).
RBAC pattern: Disabled (Pattern 2), NIE Hidden (Pattern 1).

---

## 23. Recommended Implementation Order

### Sprint 1 — Playwright Infrastructure (brak zmian app)

1. Upgrade Playwright 1.61
2. `playwright.config.ts`: screenshot/video config
3. `tests/fixtures/base.fixture.ts` z token leak guard
4. `tests/fixtures/api.fixture.ts` (Tryb A)
5. Fix `waitForTimeout` → `waitForResponse`
6. Multi-role auth setup skeleton (5 projektów, empty storage states)
7. Toast `data-testid`

### Sprint 2 — HTTP Lab Extensions (backend + Error Lab frontend)

1. Backend: 429 endpoint + headers
2. Backend: 304 Not Modified extension
3. Backend: Error Lab idempotency triggery
4. Frontend: 5 nowych Error Lab scenarios
5. Playwright: testy Error Lab (429, 425, idempotency, cross-tenant)

### Sprint 3 — Domain UI (frontend extensions)

1. Seed: PLACEHOLDER → TENANT_BETA SUSPENDED
2. Frontend: Tenant Context Indicator
3. Frontend: Tenant Suspension Banner
4. Frontend: Merchant Detail Page
5. Frontend: Partial capture/refund amount input
6. Playwright: merchant detail tabs, partial capture validation

### Sprint 4 — Tenant Settings (PATCH + ETag)

1. Flyway: V8
2. Backend: PATCH /api/tenants/{id}/settings
3. Frontend: /admin/tenant/settings
4. REST Assured: ETag tenant settings tests
5. Playwright: ETag capture, 412 stale, RBAC tenant admin vs merchant manager

### Sprint 5+ — Phase 2 Support Features

Kolejność: support module → CSV export → evidence upload → audit extensions → risk flags

---

## 24. Files and Modules to Inspect Before Coding

### Przed każdą implementacją — obowiązkowa inspekcja:

**Backend:**

| Co implementujesz | Pliki do przeczytania |
|---|---|
| Rate limiting 429 | `shared/security/SecurityConfig.java`, `GlobalExceptionHandler.java`, `PaymentRateLimitContractRestKitTest.java` |
| 304 Not Modified | `payment/internal/web/PaymentOrderController.java`, `PaymentHttpHeaders.java`, `PaymentEtag.java`, `V5__harden_payment_http_contract.sql` |
| Tenant settings PATCH | `tenant/internal/domain/Tenant.java`, `tenant/TenantResolver.java`, `V0.1__create_tenants.sql` |
| Support search | `payment/internal/web/PaymentOrderListService.java`, `PaymentOrderSpecification.java`, `Authorities.java` |
| Idempotency edge cases | `payment/internal/domain/IdempotencyRecord.java`, `IdempotencyCreateInProgressException.java`, `PaymentOrderService.java` |
| New Flyway migration | Ostatnia migracja w `db/migration/` per moduł — numeracja musi być sekwencyjna |
| New module | `package-info.java` wzorzec z istniejących modułów, `ModulithArchitectureTest.java` |
| Audit events | `shared/events/AuditableActionOccurred.java`, `AuditableActionEventFactory.java` |

**Frontend:**

| Co implementujesz | Pliki do przeczytania |
|---|---|
| Error Lab trigger | `pages/error-lab.vue` (scenarios array structure), `useApiClient.ts` |
| Nowa strona admin | `layouts/dashboard.vue`, `middleware/auth.global.ts`, `composables/useAuthorization.ts` |
| Nowy composable | `composables/useApiClient.ts`, `composables/useMerchantsApi.ts` (wzorzec) |
| Nowy server proxy | `server/api/` istniejące routes — wzorzec `backendApi.ts` |
| Nowy Zod schema | `app/types/api.ts`, istniejące schemas w `app/schemas/` lub `app/types/` |
| RBAC conditional | `composables/useAuthorization.ts`, `rbacMatrix` |

**Playwright:**

| Co implementujesz | Pliki do przeczytania |
|---|---|
| Nowy test | `tests/e2e/` dowolny istniejący spec — wzorzec mock session |
| Multi-role test | `tests/auth/auth.setup.ts`, `playwright.config.ts` |
| Download test | Sprawdź Node.js `fs` import pattern w projekcie |
| Upload test | Sprawdź `<input type="file">` data-testid w komponentach |

---

## 25. What Not To Build

### Odrzucone na zawsze (Never)

| Pomysł | Powód odrzucenia |
|---|---|
| Batch POST `/api/batch` | Brak sensu biznesowego; tooling hell; debugging nightmare |
| `X-HTTP-Method-Override` support | WAF bypass risk; audit gap; legacy workaround tylko |
| `GET /api/http-lab/unsafe-action?do=delete` | Nie implementuj nawet w labie (można zasymulować inaczej) |
| `DELETE` z body jako pattern | Proxy ignorują body; użyj POST command |
| Fake KPI dashboard | Dane nie wynikają z backendu; fikcja |
| Kafka/webhooks/outbox | Zmiana zakresu; uczy innej technologii |
| Settlement/reconciliation/KYC/3DS | Out of scope per CLAUDE.md |
| Microservice split | Niszczy Spring Modulith wartość edukacyjną |
| GraphQL API | Nie pasuje do istniejącego REST kontraktu |
| Redis/Mongo jako dodatkowe bazy | Brak wartości dla Playwright/REST Assured nauki |
| Visual snapshot na wszystko | Over-snapshotting = kruche testy |
| POM zanim strona istnieje | Framework-building trap |
| `IdempotencyModule`, `ETagModule` | Sztuczne moduły dla HTTP patterns |

### Odrzucone dla MVP (możliwe w Phase 3+)

| Pomysł | Kiedy reconsider |
|---|---|
| Webhook duplicate handling | Phase 3, jeśli MockPspClient zostanie rozbudowany |
| Reconciliation drift | Tylko jako edukacyjny lab, gdy settlement BE istnieje |
| QUERY method | Gdy Spring 7 doda wsparcie (nie teraz) |
| Service workers / offline mode | Brak wartości SDET dla tego projektu |
| Geolocation/locale | Niska priorytetowość dla SDET |
| `expect.soft.poll()` | Nie istnieje w Playwright — NIE planować |
| HAR recording/export | Phase 3 gdy inne podstawy są gotowe |

---

## 26. Top Interview Questions This System Will Teach

### HTTP/REST (Senior Level)

1. **Czym różni się idempotent od safe?** — GET jest oba, DELETE jest tylko idempotent, PATCH może być żadnym.
2. **Co powinien zwrócić serwer gdy klient wysyła If-None-Match z aktualnym ETag?** — 304 Not Modified bez body.
3. **Dlaczego POST jako search jest acceptable compromise, a nie anti-pattern?** — PII w URL, URL length limits, ale: Cache-Control: no-store i dokumentacja.
4. **Kiedy PATCH nie jest idempotentny?** — Gdy body zawiera operację `increment/decrement` zamiast ustalonej wartości.
5. **Co zwrócić gdy cross-tenant zasób istnieje ale należy do innego tenanta — 403 czy 404?** — Masked 404, żeby nie ujawniać istnienia zasobu.
6. **Co powinien zwrócić serwer gdy dwa równoległe POST lifecycle z tym samym Idempotency-Key?** — 425 Too Early dla drugiego.
7. **Czym różni się `If-Match` od `If-None-Match`?** — If-Match: "zmodyfikuj jeśli wciąż ta wersja"; If-None-Match: "daj mi dane tylko jeśli zmieniły się".
8. **Dlaczego DELETE z body jest anti-pattern?** — Proxy i biblioteki HTTP ignorują body przy DELETE.
9. **Kiedy używać `POST /actions/...` zamiast CRUD?** — Gdy operacja ma wyraźną semantykę biznesową, zmienia stan, wymaga audytu i nie pasuje do PUT/PATCH/DELETE.
10. **Co powinien zawierać 429 response w production API?** — Retry-After, X-RateLimit-*, problem+json body.

### Playwright (Senior/Expert Level)

11. **Czym różni się `page.on('dialog')` od `getByRole('dialog')`?** — Native JS `window.confirm()` vs DOM component (UModal). ConfirmActionModal to DOM, nie native dialog.
12. **Jak testujesz toast który automatycznie znika po 3 sekundach?** — `expect.poll(() => toast.count() === 0, { timeout: 5000 })`.
13. **Jak unikasz `waitForTimeout`?** — `page.waitForResponse(url)`, `expect.poll()`, auto-waiting assertions.
14. **Jak testujesz file download bez czekania na timeout?** — `page.waitForEvent('download')` przed kliknięciem przycisku.
15. **Jak budujesz multi-role test suite bez 5x każdego testu?** — Selektywne przypisanie przez tagi/projekty, nie cross-product wszystkich ról.
16. **Co to worker-scoped fixture i kiedy go używasz?** — Fixture dzielony przez testy w tym samym workerze; auth/storageState (drogi setup, raz per worker).
17. **Jak zapewnić izolację danych gdy `fullyParallel: true`?** — Worker-aware data ownership: każdy worker ma własnego merchantId z seed.
18. **Jak asercjonujesz że token JWT nie wyciekł do DOM?** — `page.evaluate(() => document.body.innerText)` + regex pattern JWT.
19. **Czym różni się Tier 1 od Tier 4 w test pyramid?** — Tier 1: mocked UI (< 5s), Tier 4: hybrid UI+API z real backend.
20. **Jak testujesz element który ma `v-if` zależny od roli?** — `expect(button).toBeHidden()` (nie `not.toBeVisible()` gdy hidden = nie w DOM).

### Spring Modulith / Architecture

21. **Jak Spring Modulith wymusza granicę modułu?** — `ModulithArchitectureTest` sprawdza że `*.internal.*` nie jest importowane przez inne moduły.
22. **Co to `AuditableActionOccurred` i jak jest publikowany?** — Domain event publishowany przez `ApplicationEventPublisher`, słuchany przez `AuditEventListener` w osobnym module.
23. **Dlaczego seed data używa deterministycznych UUID zamiast auto-increment?** — Testy mogą odwoływać się do konkretnych ID bez znajomości kolejności insercji.

---

## 27. Final Recommendation

### Priorytet absolutny Phase 1 (robić teraz)

1. **Upgrade Playwright 1.61** — odblokuje localStorage/sessionStorage, nowe video modes
2. **Base fixture z token leak guard** — security baseline, jeden raz, działający zawsze
3. **Fix `waitForTimeout`** — jedna linia, eliminuje jedyny udokumentowany anti-pattern
4. **429 + Retry-After** — duża wartość biznesowa i edukacyjna, mały koszt backend
5. **Error Lab extensions** (idempotency replay/conflict, cross-tenant, 304 demo) — minimalna zmiana, maksymalny Playwright learning surface

### Gdzie NIE marnować czasu

- Nie twórz "HTTP Lab" jako osobny backend module — Error Lab frontend wystarczy
- Nie twórz POM zanim strony są stabilne — poczekaj na Phase 1/2 features
- Nie włączaj `fullyParallel: true` przed data isolation strategy
- Nie implementuj Batch POST, Method Override, DELETE z body — dokumentacja wystarczy
- Nie buduj reconciliation dashboard bez PSP backend

### Architektoniczna zasada nadrzędna

> **Funkcja edukacyjna musi istnieć w realnym systemie PSP-like.**
> Jeśli nie masz dobrego uzasadnienia biznesowego → Error Lab extension.
> Jeśli nie masz dobrego uzasadnienia SDET → nie buduj.
> Jeśli oba uzasadnienia są słabe → dokumentuj jako anti-pattern.

System jest w doskonałym stanie do ewolucji. Nie psuj architektury sztucznymi modułami.
Rozwijaj istniejące wzorce. Playwright nauka będzie naturalna gdy domainowe funkcje dorosną.

---

*Analiza wygenerowana: 2026-06-28 | Branch: `001-project-foundation` | Wersja: 1.0*
*Skills użyte: payment-quality-lab-orchestrator, typescript6-playwright-engineering,
spring-modulith-2-0-6-modular-monolith-testing, nuxt-dashboard-zod-pinia-frontend-engineering,
test-analysis-design-and-data, rapid-software-testing-risk-thinking, rest-api-security-oauth-testing,
java-rest-api-testing-effective-java-mentor, spec-kit-feature-workflow*
