# 04 — Playwright REST (BFF) + REST Assured

Żywy stos. Playwright REST = cookie sesji na `:3000` (`BffClient` / `page.request`). REST Assured = JWT na Spring `:8080` / Testcontainers.

Nie powielać pełnej macierzy RA w POM. Który ID idzie do `BffClient` vs Surefire vs E2E: [09](09-agent-tests-pom-plan.md).  
Curriculum warstw: [value-and-learning](../m360-ops-wave-2-value-and-learning.md).

### Czego uczy ta warstwa (M360 REST)

Playwright REST: cookie sesji, Zod list, 403 readonly, search/summary jak UI. REST Assured: page/size/tenant mask, 412 DB unchanged, import UK, indeksy V23. Wave 2 RA (policy JSONB, PIN 429, case 409) **nie** należy do tego pliku.

---

## A. Playwright BFF — `PW-M360-API-###`

| ID | Metoda / path | Asercja | Epic |
|---|---|---|---|
| PW-M360-API-001 | GET `/api/merchants` | 200, `content` array, `totalElements` | E1 |
| PW-M360-API-002 | GET list `sort=revenue,desc` | 400 problem+json | E1 |
| PW-M360-API-003 | GET payment-orders `status=CAPTURED` | 200 | E1 |
| PW-M360-API-004 | GET merchants Zod `safeParse` | ok | E1 |
| PW-M360-API-010 | GET merchants z query UI | body == tabela caption | E2 |
| PW-M360-API-020 | GET `/api/merchants/{id}` z panelu | id zgodne | E3 |
| PW-M360-API-030 | POST import multipart | 200/201 preview/commit | E5 |
| PW-M360-API-031 | POST authorize (kanban) | 200 + If-Match | E5 |
| PW-M360-API-040 | POST activate jako readonly | 403 | E4 |
| PW-M360-API-041 | POST activate stale ETag | 412 | E4 |
| PW-M360-API-050 | GET org-tree | 200 children | E6 |
| PW-M360-API-051 | GET `/api/search?q=` | 200 hits | E6 |
| PW-M360-API-052 | GET summary | 200 `byStatus` | E6 |
| PW-M360-API-060 | PATCH merchant displayName | 200 If-Match | E7 |

`waitForResponse` w E2E używa **exact pathname** (wzorzec laby: nie łapać `/history` prefixem).

---

## B. REST Assured — `RA-M360-###`

| ID | Scenariusz | Klasy docelowe |
|---|---|---|
| RA-M360-001 | V23 index / list smoke | `MerchantRestAssuredTest` / IT |
| RA-M360-002 | MerchantModuleTest | module test |
| RA-M360-010 | page/size/totalElements | nowy `MerchantListRestAssuredTest` |
| RA-M360-011 | status=ACTIVE | |
| RA-M360-012 | q unique | |
| RA-M360-013 | sort displayName | |
| RA-M360-014 | size=101 → 400 | |
| RA-M360-015 | sort illegal → 400 | |
| RA-M360-016 | tenant isolation | security |
| RA-M360-017 | platform tenantId filter | |
| RA-M360-018 | 401 / readonly GET | |
| RA-M360-019 | activate illegal 409 (regresja) | existing |
| RA-M360-020 | payment status AUTHORIZED | `PaymentOrderListRestAssuredTest` |
| RA-M360-021 | status invalid 400 | |
| RA-M360-022 | sort amountMinor | |
| RA-M360-023 | ownership 403/404 | existing |
| RA-M360-024 | CREATED still works | regresja RFC |
| RA-M360-025 | date+amount+new status | |
| RA-M360-026 | V24 no 500 | |
| RA-M360-030 | create BVA (existing+) | |
| RA-M360-040 | RBAC POST 403 readonly | security |
| RA-M360-050…055 | merchant ETag 412/428 | analog payment |
| RA-M360-060…063 | import preview/commit/UK | nowy test |
| RA-M360-070 | org-tree isolation | |
| RA-M360-071 | search isolation / empty q | |
| RA-M360-080 | date UTC bounds regresja | existing list |
| RA-M360-081 | audit po activate | audit tests |
| RA-M360-090…091 | PATCH displayName + 428 | |

TDD: RA czerwony przed kontrolerem. Nie uruchamiać `restkit/` / `paymentsupport/` (standing Codex rule).
