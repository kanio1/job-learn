# Deep Review — commit `e18ebb1` vs prompt / strategia / repozytorium

> Przegląd krytyczny, oparty na dowodach (git, realm, migracje Flyway, frontend, testy).
> **To jest wyłącznie analiza.** Nie implementuje kodu, testów Playwright ani zmian w
> spec/realm/migracjach. Wszystkie twierdzenia poparte konkretnym dowodem z repo.

Branch: `018-rest-security-p1-error-auth-method-hardening`
Working tree: **czyste** (brak zmian niezacommitowanych).
Najnowszy commit: `e18ebb1 feat: add comprehensive specs and security infrastructure for payment quality roadmap`

---

## Step 0 — Przegląd commita (atomowość, higiena)

**Dowód:** `git show --stat HEAD` → **87 plików, +22 530 / −66**.

Commit `e18ebb1` jest **monolityczny i nieatomowy**. Łączy w jednym commicie co najmniej 6 niezależnych logicznie zmian:

| Grupa zmian | Pliki | Powinno być osobnym commitem |
|---|---|---|
| 7 speców Kiro (req/design/tasks + .config) | `.kiro/specs/**` (28 plików) | tak — 1 commit „docs(specs)” |
| Kopie analityczne | `docs/specs-analysis/**` (29 plików) | tak — 1 commit „docs(analysis)” |
| Steering MWG + a11y baseline | `.kiro/steering/*.md`, `AGENTS.md` | tak — 1 commit „docs(steering)” |
| **Implementacja** backend-authority-refactor | `shared/security/**`, `GlobalExceptionHandler`, `MerchantController`, `application.yml`, `pom.xml` + testy | tak — to JEDYNA realna zmiana produkcyjna |
| Zmiana realmu Keycloak | `infra/keycloak/realms/payment-quality-realm.json` | tak — częściowa implementacja iam-roles |
| **Orphan code** + binarka | `apps/frontend/app/utils/rbacMatrix.ts`, `apps/backend/.jqwik-database` | nie powinny tu być wcale |

**Problemy higieny (Must fix):**
1. **Nieatomowy commit** — miesza dokumentację, implementację jednego speca, częściową implementację drugiego i kod osierocony. Niemożliwy do sensownego review/rollbacku per-zmiana.
2. **`apps/backend/.jqwik-database` jest TRACKOWANY** (dowód: „NOT IGNORED (tracked)”). To plik wyjściowy silnika property-based (jqwik), zmienny artefakt buildu. Powinien być w `.gitignore`.
3. **`rbacMatrix.ts` to kod osierocony** — zacommitowany przed swoim specem i bez konsumenta (szczegóły w Step 2).
4. **Niespójność:** `docs/specs-analysis/` ma podfolder `03-payment-operations-dashboard` (z pełnym req/design/tasks), ale `payment-operations-dashboard` **nie istnieje** w `.kiro/specs/` w tym commicie (na liście diff jest tylko 7 innych speców). Kopia żyje bez oryginału Kiro — albo oryginał jest spoza tego commita, albo kopia wyprzedza spec.

---

## Step 1 — Traceability: prompt/strategia → faktyczna implementacja

Strategia (`docs/playwright-sdet-learning-strategy.md`, §6–§8, §17) definiuje roadmapę 5 pierwszych speców. Stan faktyczny:

| # | Spec | Deklarowany status (README) | **Faktyczny stan (dowód)** |
|---|---|---|---|
| 1 | backend-authority-refactor | ✅ Ukończony | ✅ **Potwierdzone** — `Authorities.java`, `AuthorizedPartyValidator`, converter allowlist, `GlobalExceptionHandler`, 266 testów zielonych. |
| 2 | iam-roles-and-keycloak-login | ⏳ Gotowy do implementacji | 🟡 **CZĘŚCIOWO ZAIMPLEMENTOWANY** wbrew deklaracji — realm ma 5 ról kompozytowych + `tenant-id-mapper`, ale brak użytkowników ról i frontendu (Step 2). |
| 3 | payment-operations-dashboard | 🔶 ~85% | 🟡 ~85% (z wcześniejszych commitów lekcyjnych), brak w `.kiro/specs/` tego commita. |
| 4 | tenant-model-and-isolation | 🔲 Spec gotowa | ❌ **Tylko spec** — brak migracji Flyway `tenant`, brak modułu. |
| 5 | user-management | 🔲 Spec gotowa | ❌ **Tylko spec** — brak modułu `iam`, brak stron `/admin/users`. |
| 6 | audit-log-dashboard | 🔲 Spec gotowa | ❌ **Tylko spec** — brak migracji `audit_event`, brak modułu `audit`. |
| 7 | deterministic-seed-and-test-isolation | 🔲 Spec gotowa | ❌ **Tylko spec** — brak modułu `testing`, brak profilu `seed`. |

**Wniosek:** funkcjonalnie domknięty jest **tylko #1**. #2 jest w stanie „pół-kroku” (najgroźniejszy stan — patrz niżej). #4–#7 to czyste plany. Łańcuch zależności speców jest poprawny (authorities → role → tenant → users/audit → seed), ale **żaden z #2–#7 nie jest end-to-end działający**.

---

## Step 2 — Pełny przegląd implementacji (krytyczny)

### 2.1 backend-authority-refactor — solidny
- `Authorities.java` (typed catalog), `KeycloakRealmRoleConverter` z jawną allowlistą (fail-closed na nieznane role), `AuthorizedPartyValidator` (azp) komponowany z domyślnymi walidatorami issuer/timestamp.
- `GlobalExceptionHandler` dodaje 405/415/406 (poza per-module handlerami). Dobrze — ale zweryfikować, że nie nadpisuje problem+json z modułów (ryzyko podwójnego mapowania).
- Testy: characterization PBT konwertera, `AuthorityCatalogDriftTest`, `JwtPrincipalNameTest`, azp security test. **Mocne.**

### 2.2 iam-roles — NIEBEZPIECZNY STAN POŁOWICZNY 🔴
Realm (`payment-quality-realm.json`) **dostał**:
- 5 ról kompozytowych: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `MERCHANT_MANAGER`, `SUPPORT_AGENT`, `READ_ONLY_USER`.
- `tenant-id-mapper` (protocol mapper `tenant_id`).

Realm **NIE dostał** (dowód z `_git_review3.txt`):
- **Żadnego z 5 użytkowników ról.** Istniejący userzy to stare konta payment-spec (`platform.operator`, `merchant.payment.reader`, `merchant.payment.lifecycle`, …). Żaden nie ma przypisanej roli kompozytowej.
- **Żadnego `tenant_id` na poziomie usera** — `tenant_id` występuje wyłącznie w *konfiguracji mappera* (linie 112–113), nie jako `user.attribute` żadnego użytkownika. Mapper nie ma czego zmapować → claim `tenant_id` będzie pusty dla wszystkich.

Frontend iam-roles **nie istnieje**:
- Brak `forbidden.vue`, brak `admin/users`, brak `admin/audit` (dowód: `ls: nie ma takiego pliku`).
- `auth.global.ts` robi **tylko** bramkę zalogowany/niezalogowany — **zero świadomości ról**. Brak permission-based rendering, brak role-aware sidebar.

> **Skutek:** role kompozytowe istnieją w realmie, ale nic ich nie używa ani nie przypisuje. Login „role-aware” z założenia speca **nie zadziała** — nie ma użytkownika, który po zalogowaniu dostałby `PLATFORM_ADMIN`. To gorsze niż „nie zrobione”: realm sugeruje gotowość, której nie ma.

### 2.3 `rbacMatrix.ts` — KOD OSIEROCONY 🔴
**Dowód:** grep „RBACMATRIX CONSUMERS” → wyłącznie self-referencje wewnątrz `rbacMatrix.ts`. Zero importów z zewnątrz.
- Komentarz w pliku twierdzi, że jest „consumed by `useAuthorization` and its property test” — **`useAuthorization` nie istnieje** (composables to tylko `useApiClient`, `useMerchantsApi`, `usePaymentOrdersApi`, `usePaymentLifecycleApi`).
- Macierz ma `canReadAudit`, ale **nie ma** `canManageUsers` / `canAssignRoles` / `canViewAuditLog` (capabilities z późniejszych speców user-management/audit). Czyli jest jednocześnie osierocona **i** niekompletna względem ról, które miałaby obsłużyć.

> **Rekomendacja:** usunąć z mainline albo przenieść do gałęzi implementacyjnej iam-roles. Kod produkcyjny bez konsumenta i bez testu to dług + mylący sygnał „RBAC FE gotowe”.

### 2.4 Migracje Flyway — potwierdzają zakres
**Dowód:** tylko `merchant/V1`, `payment/V2–V5`. Brak `tenant`, brak `audit`, brak struktur user/seed. To twardo potwierdza, że #4/#6 nie ruszone na poziomie schematu.

---

## Step 3 — Luki „Modern SPA / Playwright” (z perspektywy nauki SDET)

Zgodnie ze strategią §4–§5 i §7, rdzeń HTTP/contract jest świetny, ale klasy scenariuszy UI/SDET pozostają niepokryte, bo zależą od #2–#7:

| Technika Playwright/SDET | Blokowana przez | Priorytet |
|---|---|---|
| Multi-role storageState, setup project per rola | iam-roles (użytkownicy ról) | **High** |
| Permission-based rendering, 401 vs 403, auth bypass | iam-roles (role-aware UI + forbidden) | **High** |
| Tabele: filtry/sort/paginacja/deep-link, modale, drawery, combobox | user-management | **High** |
| Date pickers, audit trail verification | audit-log-dashboard | **High** |
| Deterministyczny seed / API-driven setup / isolation | deterministic-seed | **High** |
| File upload/download, bulk actions, notifications | fazy 4+ (Later) | Medium |
| Multi-project (mobile/cross-browser), POM/fixtures | architektura testów (Later) | Medium |

Obecne E2E (per strategia §2.5): płaskie spec-y, **jeden** projekt chromium, jeden pusty storageState, `fullyParallel: false`. Niezdolne do nauki RBAC dopóki #2 nie jest domknięte.

---

## Step 4 — Luki HTTP / REST

Rdzeń jest dojrzały (ETag/If-Match 412/428, Idempotency 409, problem+json, Vary, Cache-Control, X-Correlation-ID, OPTIONS/HEAD). Realne luki:
1. **Nowy `GlobalExceptionHandler` 405/415/406** — zweryfikować brak konfliktu z per-module `@RestControllerAdvice` (kolejność, podwójne problem+json). Test integracyjny potwierdzający pojedyncze, spójne body.
2. **429 (rate limiting)** — w kontrakcie oznaczone jako „future / document the gap”. OK jako pojedynczy scenariusz Error Lab, nie pełna funkcja.
3. **Brak OpenAPI** — strategia (§10) sugeruje rozważenie dla contract-checks. Medium, opcjonalne.
4. **tenant isolation w warstwie HTTP** — zaplanowane (masked-404-read / 403-write), ale niezaimplementowane; brak testów kontraktowych izolacji.

---

## Step 5 — Rozszerzenie SQL / DB (świadomie wąsko, bez bloatu)

Obecnie: `merchants`, `payment_orders` (+ indeksy, lifecycle, hardening). Roadmapa wymaga **maks. 3–5 nowych tabel** — i tyle wystarczy, nie dokładać więcej:

| Tabela | Spec | Uzasadnienie | Rekomendacja |
|---|---|---|---|
| `tenant` | tenant-model | encja organizacji, FK z `merchant.tenant_id` | **Build now** (#4) |
| `audit_event` | audit-log | ślad akcji (actor/action/target/tenant/correlation/outcome) | **Build now** (#6) |
| (`merchant.tenant_id` kolumna+FK) | tenant-model | scoping izolacji | **Build now** (#4) |
| `app_user` / `user_role` | user-management | **NIE** — wybrano fasadę Keycloak Admin API, brak lokalnej tabeli | **Skip** (zgodnie z decyzją) |
| durable event log (`spring-modulith-events-jpa`) | audit-log | trwały log zdarzeń | **Later / wymaga zgody** (dependency) |

> **Anty-bloat:** trzymać się 2 realnych tabel (`tenant`, `audit_event`) + 1 kolumny FK. Nie dodawać tabel pod userów (fasada KC) ani pod seed (profil/flagi, nie schemat).

---

## Step 6 — Macierz pokrycia Playwright (stan vs cel)

**Uwaga wersji (potwierdzona):** repo ma **Playwright 1.60.0**, prompt/strategia mówiły 1.61; Nuxt **4.4.6** vs deklarowane 4.4.8 / 4.7.x w steering. **Ujednolicić wersje przed startem lekcji** (steering `tech.md` mówi 4.4.x/4.7.x — niespójne z faktem).

| Obszar | Pokrycie teraz | Cel | Luka |
|---|---|---|---|
| Login/OIDC real | brak (pusty storageState) | per-rola setup | **#2** |
| RBAC matrix / bypass | brak | senior-level | **#2 + #4** |
| Tabele/filtry/modale/drawery | częściowo (payment) | users + audit | **#5/#6** |
| Date pickers / audit trail | brak | audit | **#6** |
| Seed / isolation | brak | API-driven | **#7** |
| File/bulk/notifications | brak | Later | fazy 4+ |
| Multi-project/mobile/cross-browser | brak (1× chromium) | Later | architektura testów |

---

## Step 7 — Brakujące funkcje (względem produkt-direction §6)

Niezrobione, w kolejności wartości: (1) nazwane role + multi-role login [#2], (2) user management [#5], (3) tenant model + isolation [#4 — faktycznie warunek wstępny dla #5], (4) audit log [#6], (5) deterministyczny seed [#7]. Później: file import/export, bulk actions, notifications, settings/unsaved-guard, responsive/multi-project. Pominąć: realtime WS/SSE, background jobs, real PSP/Kafka/settlement/KYC/3DS (jawne Non-Goals).

---

## Step 8 — Modern Web Guidance (stan warstwy doradczej)

- MWG skonfigurowane jako **manual inclusion** (`.kiro/steering/modern-web-guidance.md`) + bramka review (`docs/ai/modern-web-guidance-spec-review-gate.md`) + workflow + prompt. To **dobry, lekki kształt** (5 plików zamiast 8 z oryginalnego promptu — uniknięto duplikacji).
- A11y/testability baseline dopisany do `frontend-nuxt-ui.md`. Spójny z locator-strategy (role → label → text → testid).
- **Uwaga:** narzędzie CLI MWG w sandboxie zwraca pusto (brak sieci). Wartość realna dopiero przy frontendach #2/#5/#6 — czyli jeszcze nieaktywowana w praktyce.

---

## Step 9 — Ryzyka i over-engineering

| Ryzyko | Waga | Komentarz |
|---|---|---|
| **Połowiczny realm iam-roles** (role bez userów/atrybutów) | 🔴 High | Sugeruje gotowość, której nie ma; login role-aware nie zadziała. |
| **Orphan `rbacMatrix.ts`** | 🟠 Med | Mylący sygnał „RBAC FE gotowe”, dług bez konsumenta/testu. |
| **`.jqwik-database` trackowany** | 🟠 Med | Artefakt buildu w VCS; szum w diffach, możliwe konflikty. |
| **Nieatomowy commit 87 plików** | 🟠 Med | Brak możliwości selektywnego review/rollbacku. |
| **Niespójność wersji** (PW 1.60 vs 1.61, Nuxt 4.4.6 vs steering 4.4.x/4.7.x) | 🟡 Low-Med | Ujednolicić przed lekcjami PW. |
| **`docs/specs-analysis/03-...` bez oryginału w `.kiro/specs/`** | 🟡 Low | Kopia bez źródła — zsynchronizować. |
| Over-engineering | — | **Nie wykryto rażącego.** Specy są obszerne (PBT P1–P6 każdy), ale spójne z celem edukacyjnym. Uwaga: durable event log (modulith-events-jpa) słusznie oznaczony „wymaga zgody” — nie dokładać przedwcześnie. |

---

## Step 10 — Roadmapa priorytetowa

### 🔴 Must fix (higiena, teraz, tanie)
1. Dodać `apps/backend/.jqwik-database` do `.gitignore` i usunąć z trackowania.
2. Usunąć `rbacMatrix.ts` z mainline (albo przenieść do gałęzi iam-roles) — kod osierocony bez konsumenta/testu.
3. Zsynchronizować `docs/specs-analysis/03-payment-operations-dashboard` z oryginałem w `.kiro/specs/` (lub udokumentować, czemu kopia żyje bez źródła).
4. Ujednolicić deklaracje wersji: steering `tech.md` (Nuxt/Playwright) vs faktyczny `package.json` (Nuxt 4.4.6, PW 1.60).
5. **Na przyszłość:** rozbijać commity atomowo (docs / steering / impl per-spec osobno).

### 🟢 Build now (odblokowuje 90% nauki SDET — łańcuch zależności)
6. **Dokończyć iam-roles (#2)** — to najpilniejsze, bo realm jest w stanie połowicznym:
   - dodać 5 użytkowników ról + przypisać role kompozytowe,
   - ustawić `tenant_id` jako `user.attribute` na userach (inaczej mapper jest pusty),
   - frontend: `forbidden.vue`, role-aware sidebar, permission-based rendering, rozbudować `auth.global.ts` o role,
   - dopiero wtedy `rbacMatrix.ts` + `useAuthorization` mają sens (z testem).
7. **tenant-model-and-isolation (#4)** — warunek wstępny izolacji i user-mgmt: migracja `tenant` + `merchant.tenant_id` FK, enforcement (masked-404/403), PBT izolacji.

### 🟡 Build soon
8. **user-management (#5)** — fasada Keycloak Admin API, moduł `iam`, `/admin/users`, RBAC write/read.
9. **audit-log-dashboard (#6)** — `audit_event` (Flyway), moduł `audit`, Spring Modulith events, `/admin/audit` z filtrami dat.
10. **deterministic-seed (#7)** — profil `seed` + reset za flagą (`app.testing.enabled=false` domyślnie), defense-in-depth `@Profile("!prod")`.

### ⚪ Later
11. File import/export, bulk actions, notifications, settings + unsaved-guard.
12. Architektura testów Playwright: POM/fixtures, multi-project (mobile/cross-browser), API clients.
13. Durable event log (`spring-modulith-events-jpa`) — tylko po świadomej zgodzie (nowa zależność).

### 🚫 Postpone / Skip
14. Postpone: WebSocket/SSE realtime, background jobs, 429 jako pełna funkcja (OK jako 1 scenariusz Error Lab), API versioning, drag-and-drop jako duży feature.
15. Skip (Non-Goals): real PSP, Kafka, settlement/payout/reconciliation, KYC, card/PAN/PCI, 3DS, microservice split, fake KPI dashboard.

---

## Werdykt

Solidny fundament (#1 domknięty, kontrakt HTTP dojrzały), ale commit `e18ebb1` **przedwcześnie wprowadza sygnały gotowości, których nie ma**: połowiczny realm iam-roles (role bez userów), osierocony `rbacMatrix.ts`, trackowana binarka jqwik. Z 7 speców funkcjonalnie kompletny jest **1**, jeden (#2) jest w ryzykownym stanie „pół-kroku”, reszta to czyste plany. Najpierw higiena (Must fix), potem **dokończyć #2 i #4** — to odblokowuje rdzeń nauki SDET (multi-role RBAC, isolation), zanim ruszą #5/#6/#7.
