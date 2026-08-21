---
name: playwright-merchant-360
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
related_gate: Merchant 360 ERP/CRM UI on live Keycloak + BFF + Spring + Postgres (no mocks)
last_updated: 2026-08-20
---

# Milestone-PW — Merchant 360 (ERP/CRM lab)

Następny milestone Playwright po [playwright-real-stack-learning](../playwright-real-stack-learning/) (IMPLEMENTED) i katalogu [live-pom-wave-2](../../../docs/testing/live-pom-wave-2/).

**Sibling (Wave 2 ops interaction, DESIGNED_NOT_STARTED):** [playwright-ops-wave-2](../playwright-ops-wave-2/) — support Kanban, live WS, conflict UI, PIN, itd. Flyway **V23–V30** zostaje dla tego katalogu; Wave 2 startuje od **V31**. Payment-status Kanban zostaje tutaj (E5); entity search E6 współdzielony z Wave 2 E9.

**Nie** jest to nowy produkt CRM (Customers/Deals). **Tak** — pogłębienie Merchant Registry + Payment Orders do wzorców ERP: zaawansowany `UTable`, slideover 360, RBAC kolumn, If-Match, import CSV, Kanban lifecycle, drzewo tenant→merchant, wyszukiwanie, summary charts.

To **nie** jest Kiro spec. Nie edytować `.kiro/**`. Implementacja tylko po osobnym zleceniu fali. Ten katalog = backlog wykonawczy.

Katalog testów (BC / UC / BF / EP / AT / plan POM): [docs/testing/merchant-360-erp-lab/](../../../docs/testing/merchant-360-erp-lab/) — start od [00-business-flows](../../../docs/testing/merchant-360-erp-lab/00-business-flows.md) i [09-agent-tests-pom-plan](../../../docs/testing/merchant-360-erp-lab/09-agent-tests-pom-plan.md).  
Para z Wave 2 (co dają, czego uczą E2E/REST): [docs/testing/m360-ops-wave-2-value-and-learning.md](../../../docs/testing/m360-ops-wave-2-value-and-learning.md).  
Research wersji: [.codex/research/merchant-360-versioned-stack.md](../../../.codex/research/merchant-360-versioned-stack.md).  
Implementacja (Grok 4.6 cache): [.codex/prompts/merchant-360-implement.md](../../../.codex/prompts/merchant-360-implement.md) · overlay [.codex/merchant-360-slice.md](../../../.codex/merchant-360-slice.md) · skill `merchant-360-implement`.  
Review WIP T01–T20: [.codex/prompts/merchant-360-review.md](../../../.codex/prompts/merchant-360-review.md) · skill `merchant-360-review`.

## Jak czytać

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, fale, granice |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR, NFR, non-goals |
| [01-infra-postgres-keycloak-stack.md](./01-infra-postgres-keycloak-stack.md) | Flyway V23+, indeksy, Keycloak, `dev-stack.sh` |
| [02-versioned-research.md](./02-versioned-research.md) | Playwright 1.61 / Nuxt UI 4.7.1 / PG 18 — cytaty |
| [learning-map.md](./learning-map.md) | Lekcja TS/PW → story |
| [task-board.md](./task-board.md) | `PW-M360-T01`… kolejność implementacji |
| [epics/](./epics/) | E0–E7: stories, AC, SQL, ID testów |

## Fale (kolejność implementacji)

0. Dokumentacja (ten katalog) — **ta sesja**
1. Kontrakty list (Spring + Flyway + REST Assured) — merchant page + payment status/sort
2. Advanced `UTable` (merchants + payments) + live POM
3. Slideover 360 + formularz na prawdziwych polach
4. RBAC kolumn + merchant ETag `412`
5. Import CSV + Kanban lifecycle
6. Tree + live command search + charts z `summary`
7. Calendar / Timeline / Stepper / Editor (tylko jeśli pin 4.7.1 to ma)

## Granice (MUST)

- Stos żywy: `scripts/dev-stack.sh` (domyślnie host) albo `--app` (POM/TS). Hasła POM tylko z env.
- **Zero** `page.route` / `route.fulfill` / HAR mock / MSW w `tests-pom`. Oracle HTTP = `waitForResponse` + `BffClient` + REST Assured.
- Zero encji Customer/Deal. Zero fałszywego Revenue. Zero bumpa `@nuxt/ui`.
- Optimistic lock merchant = **412** + `If-Match`, jak płatności. `409` zostaje dla duplikatu / idempotency.
- `Modulith`: zmiany w `merchant` / `payment` / `audit` / `tenant`. Bez modułu `crm`.
- JPA `ddl-auto: validate`. Schema tylko Flyway.
- Unique merchant reference jest **globalny** (`uk_merchants_normalized_reference`) — import i test data muszą to respektować.

## Testy (warstwy)

| Warstwa | Prefiks | Gdzie |
|---|---|---|
| REST Assured (Spring) | `RA-M360-###` | `apps/backend/src/test/java/lab/paymentquality/rest` |
| Playwright REST (żywy BFF) | `PW-M360-API-###` | `tests-pom` + `BffClient` / `page.request` |
| Live POM E2E | `PW-M360-E2E-###` | `tests-pom/specs/*` |
| Security / dwa contexty | `PW-M360-SEC-###` | `storageState` Keycloak |
| Use case / business | `UC-M360-##` / `BC-M360-##` | katalog `docs/testing/merchant-360-erp-lab/` |

REST Assured zostaje ownerem pełnej macierzy kontraktu. Playwright nie powiela 1:1 każdego 400.

## Stos (operator)

Z [docs/setup/run-stack-and-pom.md](../../../docs/setup/run-stack-and-pom.md):

```bash
cp infra/compose/.env.example infra/compose/.env   # raz
scripts/dev-stack.sh --app                         # nauka POM/TS, wszystko w Podman
# albo
scripts/dev-stack.sh                               # Postgres+Keycloak w Podman; Spring/Nuxt na hoście
```

POM: `PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD` / `PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD` (i role pomocnicze) **tylko env**.  
`--app` i `--full` się wykluczają. HTTPS nie jest wymagany do Fal 1–5.
