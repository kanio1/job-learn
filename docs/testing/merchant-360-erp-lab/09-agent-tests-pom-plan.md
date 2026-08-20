# 09 — Plan: co agent napisze w `tests-pom`

**Zakaz:** `tests-pom-learner/` (copy-map, agent nie pisze), `page.route`, SQL z Node, ETL/`seed-learning`, nowy POM tree.

Kanon: [playwright-pom](../../../.agents/skills/playwright-pom/SKILL.md), [methods/README.md](../../../apps/frontend/tests-pom/methods/README.md), [playbook 02/04](../playwright-method-playbook/02-tech-lead-layers.md).

Stos: `scripts/dev-stack.sh --app`. Import `test`/`expect` z `tests-pom/fixtures/index.ts`.

---

## 1. Reguła warstwy (agent czyta przed `test()`)

| Jeśli asercja to… | Piszesz | Prefiks |
|---|---|---|
| Widoczność, klawiatura, dialog, ARIA, Back, drag/menu | E2E UI w `specs/*.spec.ts` | `PW-M360-E2E-*` |
| Ten sam klik **oraz** status/query HTTP | E2E + `waitForBff` / `waitForResponse` (hybryda, nadal E2E) | `PW-M360-E2E-*` |
| Body/query/400/403 **bez** UI | `BffClient` w specu (może ten sam plik, osobny `test`) | `PW-M360-API-*` |
| Pełna macierz sort/size/UK/ETag/DB | Java REST Assured | `RA-M360-*` |
| Dwie sesje | `browser.newContext({ storageState })` | `PW-M360-SEC-*` |

Stop-gate: modal/badge/kolumna = E2E. Header `ETag`/`If-Match` = `waitForRequest` albo RA, nie mock.

---

## 2. Page objects (rozszerz, nie god-App)

| Plik | Zmiana |
|---|---|
| [MerchantsListPage.ts](../../../apps/frontend/tests-pom/pages/MerchantsListPage.ts) | `rowByReference`, `sortBy(column)`, `applyFilters`, `selectRow`, `bulkActivate`, `open360(ref)`, query URL helpers |
| [PaymentsListPage.ts](../../../apps/frontend/tests-pom/pages/PaymentsListPage.ts) | sort amount, board toggle, `cardByOrderId`, `moveTo(status)` |
| **nowy** `pages/components/MerchantSlideover.ts` | `expectOpen`, Escape, sections — **nie** asercja biznesu 201 |
| [CommandPalette.ts](../../../apps/frontend/tests-pom/pages/components/CommandPalette.ts) | entity option by ref (nav już jest) |
| [App.ts](../../../apps/frontend/tests-pom/pages/App.ts) | `readonly merchantSlideover` |
| [bff-client.ts](../../../apps/frontend/tests-pom/api/bff-client.ts) | list page DTO `content`; search; import multipart; PATCH merchant; org-tree |

JSDoc methods: **co / co się zmienia / e2e\|rest / seed**.

---

## 3. Method classes (ISTQB rows, nie runner)

| Nowy plik | Technika | e2e vs rest | Seed |
|---|---|---|---|
| `methods/ep-bva/MerchantListQueryPartitions.ts` | EP/BVA size/sort/status/q | **rest** | unique q |
| `methods/ep-bva/PaymentListStatusPartitions.ts` | EP 6 statusów + amount sort | **rest** | worker merchant |
| `methods/ep-bva/MerchantCsvPartitions.ts` | EP fixture files | e2e upload + rest preview | `fixtures/import/` |
| `methods/decision-table/MerchantColumnAccessMatrix.ts` | DT rola × kolumna | **e2e** 3–4 wiersze | storageState |
| `methods/decision-table/MerchantIfMatchMatrix.ts` | DT akcja × If-Match | **rest** | E4 |
| `methods/state/PaymentKanbanEdges.ts` | ST legal/illegal drop | e2e menu P0 | `IllegalStDt` reuse where possible |
| `methods/use-case/Merchant360Journey.ts` | UC list→360→Escape | **e2e** | unique merchant |
| `methods/use-case/MerchantImportJourney.ts` | UC preview→commit | **e2e** | unique CSV refs |
| `methods/metamorphic/MerchantQueryInclusion.ts` | MR q/status ⊆ | **rest** | |
| `methods/combinations/MerchantBulkStDt.ts` | ST+DT bulk mixed | e2e | 2 DRAFT |

**Reuse bez duplikatu:** `MerchantReferencePartitions`, `MerchantAccessMatrix` (dopisz kolumny), `MerchantStatusMachine`, `PaymentStatusMachine`, `IllegalStDt`, `CreateMerchantJourney`, `FilterInclusion` / `SummaryInclusion`, `EtagStability`, `CreateUcEpRest`.

Nie: pairwise checkout w tym milestone.

---

## 4. Specs (nowe vs rozszerz)

| Spec | Fala | Testy (ID) | Warstwa w pliku |
|---|---|---|---|
| **rozszerz** `merchants.spec.ts` | 1 | E2E-001 overview `totalElements` | hybryda |
| **nowy** `merchants-table.spec.ts` | 2 | E2E-020…032, 040…052 | E2E + waitForBff |
| **rozszerz** `payments-filters.spec.ts` | 2 | E2E-025…028 | E2E; nie psuć RFC-020 |
| **nowy** `merchants-slideover.spec.ts` | 3 | E2E-060…063 | E2E `@a11y` |
| **rozszerz** `merchants.spec.ts` / `aria-snapshots.spec.ts` | 3 | E2E-070…073 | E2E + API 409 |
| **nowy** `merchants-rbac-columns.spec.ts` | 4 | SEC-010…014 | E2E `test.use` |
| **rozszerz** `readonly-rbac.spec.ts` | 4 | API-040 | REST w istniejącym RBAC |
| **nowy** `merchants-concurrency.spec.ts` | 4 | SEC-020 | dwa contexty |
| **nowy** `merchants-import.spec.ts` | 5 | E2E-080…085 | E2E `setInputFiles` |
| **nowy** `payments-kanban.spec.ts` | 5 | E2E-090…094 | E2E `@kanban` worker |
| **nowy** `merchants-tree.spec.ts` | 6 | E2E-100…104 | E2E `@a11y` |
| **rozszerz** `command-palette.spec.ts` | 6 | E2E-110…112 | E2E |
| **rozszerz** `payments-summary.spec.ts` | 6 | E2E-120, API-052 | hybryda + REST |
| **nowy** `merchants-list.api.spec.ts` **lub** `admin-bff.spec.ts` | 1 | API-001…004, MR-01…04 | **tylko REST** |
| Fala 7 specy | 7 | E2E-130…151 | po pin 4.7.1 gate |

Project: table/360/import = `chromium-admin`; kanban/filters = `chromium-manager` + `ownedMerchantId`; SEC = `chromium-rbac` / explicit `storageState`.

---

## 5. Fixtures plików

```text
tests-pom/fixtures/import/
  valid.csv
  duplicate.csv
  invalid-header.csv
  empty.csv
  utf8.csv
  malformed.csv
```

Git: tak (bez PII). Referencje w CSV = `uniqueMerchantReference` generowane w teście **albo** placeholder + replace w tmp — nie kolidować z `uk_merchants_normalized_reference` i `MERCHANT-Wn`.

---

## 6. Kolejność pisania testów (TDD)

1. RA `MerchantListRestAssuredTest` (T02) — czerwony.
2. Implementacja Spring.
3. `BffClient` + `merchants-list.api.spec.ts` API-001.
4. `merchants-table.spec.ts` E2E-020.
5. Dopiero UI selection/bulk.

Kanban: najpierw RA illegal transition (istnieje) + E2E-090 menu, potem drag.

---

## 7. Mapowanie technik → pliki (ściąga)

| Technika | POM methods | Główny spec | RA |
|---|---|---|---|
| EP/BVA | `MerchantListQueryPartitions`, reuse reference | 1 E2E BVA create; list size **API/RA** | T02/T03 |
| DT | `MerchantColumnAccessMatrix`, `MerchantIfMatchMatrix` | rbac-columns, concurrency | T12/T13 |
| ST | `PaymentKanbanEdges`, reuse machines | kanban, bulk | illegal existing |
| UC | `Merchant360Journey`, `MerchantImportJourney` | slideover, import | — |
| MR | `MerchantQueryInclusion` | list.api | — |
| EG | reuse OverlayAndIpv6 | fixtures + E2E-063 | — |
| Pairwise | — | **skip M360** | — |
