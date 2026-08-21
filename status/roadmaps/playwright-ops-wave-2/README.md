---
name: playwright-ops-wave-2
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
related_gate: Ops Interaction Lab on live Keycloak + BFF + Spring + Postgres (no mocks)
last_updated: 2026-08-20
---

# Milestone-PW — Ops Interaction Lab (Wave 2)

Następny milestone Playwright po [playwright-merchant-360](../playwright-merchant-360/) (DESIGNED_NOT_STARTED) i [playwright-real-stack-learning](../playwright-real-stack-learning/) (IMPLEMENTED).

To **nie** jest drugi CRM i **nie** jest powtórka Merchant 360. To curriculum **12 klas problemów testowych** (drag/optimistic, WebSocket, dwa contexty + conflict UI, persistence, partial bulk, dirty guard, PIN/429, BVA formularza, badge async, global search, i18n, carousel) na istniejącym Merchant Registry + Payment Orders + Support.

Wave 1 (10 zaakceptowanych feature’ów: simulator, import, virtualized history, tenant tree, autocomplete, stepper, editor, context menu, datetime, resizable inspector) **zostaje** — nie duplikujemy ich tutaj. Świadomie **nie** dodajemy: Pagination Lab, Download Lab, Offline Lab jako nowy ekran, iframe `frameLocator`, kolejnego Slideover/Modal labu.

To **nie** jest Kiro spec. Nie edytować `.kiro/**`. Implementacja tylko po osobnym zleceniu fali. Ten katalog = backlog wykonawczy.

Katalog testów (BC / UC / BF / TC): [docs/testing/ops-wave-2-interaction-lab/](../../../docs/testing/ops-wave-2-interaction-lab/) — start od [00-business-flows](../../../docs/testing/ops-wave-2-interaction-lab/00-business-flows.md).  
Para z M360 (co dają, czego uczą E2E/REST): [docs/testing/m360-ops-wave-2-value-and-learning.md](../../../docs/testing/m360-ops-wave-2-value-and-learning.md).  
Research wersji: [.codex/research/ops-wave-2-versioned-stack.md](../../../.codex/research/ops-wave-2-versioned-stack.md).

## Jak czytać

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, fale, granice, relacja do M360 |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR, NFR, non-goals |
| [01-infra-postgres-keycloak-stack.md](./01-infra-postgres-keycloak-stack.md) | Flyway V31+, moduły support/ops, Keycloak, `dev-stack.sh` |
| [02-versioned-research.md](./02-versioned-research.md) | Playwright 1.61 / Nuxt UI 4.7.1 / PG 18 — cytaty i korekty |
| [learning-map.md](./learning-map.md) | Lekcja TS/PW → story |
| [task-board.md](./task-board.md) | `PW-OPS-T00`… kolejność implementacji |
| [epics/](./epics/) | E0–E12: stories, AC, SQL, ID testów |

## Fale (kolejność implementacji)

0. Dokumentacja (ten katalog) — **ta sesja**
1. Merchant write surface — PATCH + ETag 412 + conflict tabs; unsaved guard
2. Support module + Kanban queue (cases, nie payment status)
3. Bulk assign z częściowym sukcesem
4. Step-up refund PIN (extend dual-control; równolegle do fali 2)
5. Live Operations WebSocket + Notification Center
6. Saved views / column profiles + global entity search
7. Tenant payment rule configurator (BVA / decision table)
8. Locale / i18n workspace (osobna zgoda na `@nuxtjs/i18n`)
9. Evidence gallery (`UCarousel` jeśli 4.7.1 to eksportuje)

## Relacja do Merchant 360

| Temat | Merchant 360 | Ten milestone |
|---|---|---|
| Payment status Kanban | E5 (`POST .../authorize` itd.) | **nie** — inny board |
| Support case Kanban | brak | E3 |
| Merchant ETag 412 | E4 podstawowy Reload | E1 conflict workspace (wspólny backend — **raz**) |
| Ctrl+K entity search | E6 | E9 pogłębienie RBAC / last-wins |
| Bulk activate merchants | E2 | **nie**; tu bulk **cases** partial |
| i18n / WS / PIN / unsaved / gallery | non-goal / brak | ten milestone |

Jeśli M360 T13 (merchant ETag) już istnieje w kodzie, E1 robi tylko conflict UI. Jeśli M360 T17 (live search) już istnieje, E9 dodaje tylko RBAC grup i race last-wins.

## Granice (MUST)

- Stos żywy: `scripts/dev-stack.sh` (domyślnie host) albo `--app` (POM/TS). Hasła POM tylko z env.
- **Zero** `page.route` / `route.fulfill` / `page.routeWebSocket` / `browserContext.routeWebSocket` / HAR / MSW w `tests-pom`. Oracle HTTP = `waitForResponse` + `BffClient` + REST Assured. Oracle WS = `page.on('websocket')` + `waitForFrameReceived` na prawdziwym kanale. Duplicate/out-of-order/malformed = `POST /api/ops/feed/inject`.
- Optimistic lock merchant/case = **412** + `If-Match`. `409` zostaje dla duplikatu / idempotency / dual-control self-approve / **nielegalnej** transycji case.
- `Modulith`: nowe `support` i `ops`. Merchant PATCH w `merchant`. PIN w `payment`. Policy w `tenant`. Saved views w `iam`. Bez modułu `crm`.
- JPA `ddl-auto: validate`. Schema tylko Flyway **V31+** (M360 rezerwuje V23–V30).
- Token nigdy w przeglądarce. WS same-origin przez Nitro.
- Pin `@nuxt/ui` **4.7.1**. Nie używać `UPinInput separator` (4.9+). Nie bumpować UI.
- PIN step-up **nie** jest Keycloak OTP.
- Unique merchant reference nadal **globalny**.

## Testy (warstwy)

| Warstwa | Prefiks | Gdzie |
|---|---|---|
| REST Assured (Spring) | `RA-OPS-###` | `apps/backend/src/test/java/lab/paymentquality/rest` |
| Playwright REST (żywy BFF) | `PW-OPS-API-###` | `tests-pom` + `BffClient` / `page.request` |
| Live POM E2E | `PW-OPS-E2E-###` | `tests-pom/specs/*` |
| Security / dwa contexty | `PW-OPS-SEC-###` | `storageState` Keycloak |
| Use case / business | `UC-OPS-##` / `BC-OPS-##` | katalog `docs/testing/ops-wave-2-interaction-lab/` |

REST Assured zostaje ownerem pełnej macierzy kontraktu. Playwright nie powiela 1:1 każdego 400.

## POM / TypeScript (docelowo, nie w tej sesji)

Jedno drzewo `apps/frontend/tests-pom/`. Import `test` / `expect` z `fixtures/index.ts`.

```text
pages/
  SupportPage.ts                 # + tab Work Queue
  MerchantEditPage.ts
components/
  KanbanBoardComponent.ts
  SupportCaseCard.ts
  ConflictDiffComponent.ts
  NotificationCenterComponent.ts
  SavedViewsComponent.ts
  PaymentFiltersComponent.ts
  PinChallengeComponent.ts
  RuleConfiguratorComponent.ts
  EvidenceCarouselComponent.ts
  OpsFeedComponent.ts
fixtures/
  multi-user.fixture.ts          # platform.admin + platform.operator
```

Discriminated unions: `SupportCaseStatus`, `OpsFeedEvent`. Zod w `app/schemas/`. Page class **bez** `if (role)`.

## Stos (operator)

Z [docs/setup/run-stack-and-pom.md](../../../docs/setup/run-stack-and-pom.md):

```bash
cp infra/compose/.env.example infra/compose/.env   # raz
scripts/dev-stack.sh --app                         # nauka POM/TS, wszystko w Podman
# albo
scripts/dev-stack.sh                               # Postgres+Keycloak w Podman; Spring/Nuxt na hoście
```

POM: `PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD` / `PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD` (i role pomocnicze, w tym `platform.operator`) **tylko env**.  
`--app` i `--full` się wykluczają. HTTPS nie jest wymagany do Fal 1–7.
