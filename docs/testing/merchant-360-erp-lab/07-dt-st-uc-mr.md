# 07 — Decision tables, state, use case, metamorphic, error guessing

Implementacja = `test()` w [03](03-playwright-e2e-catalog.md) / [04](04-playwright-api-http.md).  
Techniki kanoniczne: [playbook 03](../playwright-method-playbook/03-test-architect-techniques.md) + [05 combinations](../playwright-method-playbook/05-combinations.md).

**Pairwise** (`CheckoutModeOutcome`): **poza M360** (CPL). Nie mieszać.

Pokrycie: **designed**. Klasy: [09](09-agent-tests-pom-plan.md).

---

## DT-M360-01 — Kto co widzi na registry (minimal covering POM)

Pełna macierz 5 ról × czasowniki = **RA-040**. POM = trzy wiersze UI + bliźniak 403.

| Sesja | Lista | Create | Activate | Tenant column | Import | TC |
|---|---|---|---|---|---|---|
| platform.admin | tak | tak | tak | tak | tak | SEC-012 |
| tenant.admin | tak (Alpha) | tak | tak | **nie** | tak (swój tenant) | SEC-013 |
| support.agent | tak | nie | nie | P2 | nie | SEC-011 |
| readonly.user | tak | nie | nie | nie | nie | SEC-010, API-040 |
| merchant.manager | 403 UI | — | — | — | — | SEC-014 |
| guest | `/login` | — | — | — | — | existing E2E-001 |

Klasa: rozszerzyć `MerchantAccessMatrix` (nie nowy runner).

---

## DT-M360-02 — If-Match × mutacja merchanta

Analog `IfMatchActionMatrix`.

| Akcja | Brak If-Match | Stale `"v99"` | Świeży | TC |
|---|---|---|---|---|
| POST activate | 428 | 412 | 200 | RA-051…053 |
| POST suspend | 428 | 412 | 200 | RA-050+ |
| PATCH risk-flag | 428 | 412 | 200 | RA-054 |
| PATCH displayName (E7) | 428 | 412 | 200 | RA-090/091, E2E-150 |

UI: jeden SEC-020 (dwa contexty) + Reload. Nie 4 E2E na każdą komórkę.

---

## DT-M360-03 — Import commit

| Preview | Commit | Wynik | TC |
|---|---|---|---|
| valid | pierwszy | INSERT, list hit | E2E-084, RA-063 |
| valid | drugi ten sam | 409 | E2E-085 |
| invalid-header | commit | 400 / zablokowany przycisk | E2E-081 |
| readonly | preview | 403 | RA-061 |

---

## ST-M360-01 — Merchant status

Reuse `MerchantStatusMachine`. Nowe: bulk tylko z DRAFT; 360 activate.

```text
DRAFT --activate--> ACTIVE --suspend--> SUSPENDED (terminal)
```

| Krawędź | HTTP | TC |
|---|---|---|
| DRAFT → ACTIVE | 200 | E2E-040, existing activate |
| ACTIVE → DRAFT | 409 | RA-019 |
| SUSPENDED → ACTIVE | 409 | existing MER-04 |
| bulk DRAFT+ACTIVE | per-row | E2E-041 |

---

## ST-M360-02 — Payment Kanban = lifecycle

Reuse `PaymentStatusMachine` + `IllegalStDt`. Kolumna board = `toStatus`.

| From | Move to | POST | TC |
|---|---|---|---|
| CREATED | AUTHORIZED | `/authorize` | E2E-090 |
| AUTHORIZED | CAPTURED | `/capture` | E2E-090 wariant |
| CAPTURED | CREATED | 4xx | E2E-094, IllegalStDt |
| any | + stale If-Match | 412 rollback | E2E-093, DualControl analog 2 context |

---

## ST-M360-03 — Wizard create payment (E7)

```text
AMOUNT --> CURRENCY --> REFERENCE --> REVIEW --> SUBMITTED
```

Back/Next; pusty amount na Next = field error, brak POST. Double submit = idempotency (existing PAY).

**TC:** E2E-134…136

---

## UC — indeks (szczegół w [00](00-business-flows.md) i [02](02-use-cases.md))

| UC | Flow | Główny E2E | REST bliźniak |
|---|---|---|---|
| UC-M360-10 | filtr + Back | E2E-031 | API-010 |
| UC-M360-20 | 360 Escape | E2E-062 | API-020 |
| UC-M360-21 | create | E2E-070 | existing create |
| UC-M360-30 | readonly | SEC-010 | API-040 |
| UC-M360-31 | 412 | SEC-020 | RA-052 |
| UC-M360-41 | import commit | E2E-084 | RA-063 |
| UC-M360-42 | Kanban | E2E-090 | API-031 |

---

## MR — metamorphic (prawie zawsze PW REST)

| ID | Relacja | Oracle | TC | Klasa |
|---|---|---|---|---|
| MR-M360-01 | węższy `q` ⊆ szerszy `q` prefix | każdy id z A ∈ B | API-010 wariant | nowy `MerchantQueryInclusion` analog `FilterInclusion` |
| MR-M360-02 | `status=ACTIVE` ⊆ brak status | ids ⊆ | API | ta sama |
| MR-M360-03 | GET list bez zapisu | `totalElements` stałe (2 GET) | API | analog `EtagStability` |
| MR-M360-04 | GET merchant ETag bez mutacji | ETag stały | API-041 prep | `EtagStability` |
| MR-M360-05 | summary `byStatus.n` vs list count per status | n list (page all / suma stron) — **ostrożnie paginacja**: oracle = RA `size=100` albo sum pages | API-052 | `SummaryInclusion` |
| MR-M360-06 | tenant.admin list ⊆ platform list (Alpha) | Beta ∉ tenant | existing IsolationInclusion + RA-016 | `IsolationInclusion` |

Nie E2E pętli po stronach.

---

## EG — error guessing

| ID | Zgadnięcie | Oczekiwanie | TC |
|---|---|---|---|
| EG-M360-01 | overlay Vite łapie click sort | handler z fixtures, nie fail | existing overlay |
| EG-M360-02 | BFF IPv6 `localhost` vs Node | `BffClient` `127.0.0.1` | existing EG |
| EG-M360-03 | `waitForResponse` łapie `/history` prefix | exact pathname `waitForBff` | kanban/360 |
| EG-M360-04 | selection `tableApi` stale (#5408) | `v-model:row-selection` | E2E-040 |
| EG-M360-05 | bulk drugi klik | disable + idempotent activate 409/200 | E2E-040 |
| EG-M360-06 | Escape zagnieżdżony modal | najpierw confirm, nie cały slideover | E2E-063 |
| EG-M360-07 | CSV UTF-8 BOM | preview nie „invalid header” | EP-054 |
| EG-M360-08 | dragTo flake | P0 = menu Move, drag P1 | E2E-090 vs 091 |

---

## Combinations (MUST import z specs)

| Zestaw | Klasa (nowa lub reuse) | Spec |
|---|---|---|
| DT+UC | `MerchantAccessMatrix` + SEC specs | `readonly-rbac` / nowy `merchants-rbac-columns.spec.ts` |
| ST+DT | `LifecycleStDt` + Kanban | `payments-kanban.spec.ts` |
| UC+EP REST | `CreateUcEpRest` | create 1 E2E + RA BVA |
| ST+DT 2 aktorów | analog `DualControlStDt` | SEC-020 merchant 412 |
| EP+MR filtra | `MerchantQueryInclusion` | `admin-bff` lub `merchants-list.api.spec.ts` |
| Nie łączyć | pairwise × ST × BVA | — |
