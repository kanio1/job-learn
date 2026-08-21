# 04 — Playwright REST (BFF) + REST Assured

Żywy stos. Playwright REST = cookie sesji na `:3000` (`BffClient` / `page.request`). REST Assured = JWT na Spring `:8080` / Testcontainers.

Nie powielać pełnej macierzy RA w POM.

`waitForResponse` używa **exact pathname** (nie łapać `/history` prefixem).  
Curriculum: [value-and-learning](../m360-ops-wave-2-value-and-learning.md).

### Czego uczy ta warstwa (Ops REST)

Playwright REST: cookie BFF, If-Match merchant, inject 403, bulk retry ids, evidence 404 **problem** (nie koperta Nitro). REST Assured: 412/428/409/429, maszyna case, PIN hash, JSONB policy, isolation. E2E nie powtarza RA-OPS-182 (threshold -1/101).

---

## A. Playwright BFF — `PW-OPS-API-###`

| ID | Metoda / path | Asercja | Epic |
|---|---|---|---|
| PW-OPS-API-010 | PATCH `/api/merchants/{id}` | 200/412; ETag in/out | E1 |
| PW-OPS-API-020 | POST `/api/ops/feed/inject` | 201 admin; 403 readonly | E6 |
| PW-OPS-API-030 | PATCH `/api/support/cases/{id}` | 200 If-Match | E3 |
| PW-OPS-API-031 | POST `/api/support/cases/bulk-assign` retry | body.caseIds tylko failed | E4 |
| PW-OPS-API-040 | POST refund-challenges + verify | 201/200 cookie | E5 |
| PW-OPS-API-050 | CRUD `/api/users/me/payment-views` | 201/200; other user 404 | E8 |
| PW-OPS-API-060 | GET `/api/search?q=` | 200 limit; manager bez merchants | E9 |
| PW-OPS-API-070 | GET evidence unknown id | 404 problem | E12 |

---

## B. REST Assured — `RA-OPS-###`

| ID | Scenariusz | Epic |
|---|---|---|
| RA-OPS-001 | V32 kolumny contact; ddl validate | E1 |
| RA-OPS-050 | PATCH merchant If-Match 200 ETag+1 | E1 |
| RA-OPS-051 | brak If-Match 428 | E1 |
| RA-OPS-052 | stale 412 DB unchanged | E1 |
| RA-OPS-053 | malformed If-Match 400 | E1 |
| RA-OPS-054 | BOLA tenant 404/403 | E1 |
| RA-OPS-055 | readonly 403 | E1 |
| RA-OPS-110 | POST case 201 NEW | E3 |
| RA-OPS-111 | NEW→IN_PROGRESS 200 | E3 |
| RA-OPS-112 | NEW→RESOLVED 409 | E3 |
| RA-OPS-113 | stale case 412 | E3 |
| RA-OPS-114 | brak If-Match 428 | E3 |
| RA-OPS-115 | tenant isolation 404 | E3 |
| RA-OPS-116 | readonly POST 403 | E3 |
| RA-OPS-117 | manager POST 403 | E3 |
| RA-OPS-118 | duplicate reference 409 | E3 |
| RA-OPS-119 | WAITING→IN_PROGRESS 200 | E3 |
| RA-OPS-120 | GET list status filter | E3 |
| RA-OPS-121 | RESOLVED cofnięcie 409 | E3 |
| RA-OPS-122 | SupportModuleTest + Modulith | E3 |
| RA-OPS-125 | inject 201; readonly 403 | E6 |
| RA-OPS-126 | inject malformed 201 | E6 |
| RA-OPS-127 | manager inject 403 | E6 |
| RA-OPS-140 | saved view POST/GET | E8 |
| RA-OPS-141 | drugi subject empty | E8 |
| RA-OPS-142 | default unique flip | E8 |
| RA-OPS-143 | unknown filter 400 | E8 |
| RA-OPS-150 | bulk 2 ok + 2 fail | E4 |
| RA-OPS-151 | pusta lista 400 | E4 |
| RA-OPS-152 | >100 ids 400 | E4 |
| RA-OPS-153 | readonly bulk 403 | E4 |
| RA-OPS-154 | no-op same assignee success | E4 |
| RA-OPS-170 | challenge nie wymagany 400 | E5 |
| RA-OPS-171 | 201 hash ≠ pin | E5 |
| RA-OPS-172 | verify correct 200 | E5 |
| RA-OPS-173 | invalid_pin then 429 | E5 |
| RA-OPS-174 | 429 rate_limited | E5 |
| RA-OPS-175 | expired 400 | E5 |
| RA-OPS-176 | reuse 409 | E5 |
| RA-OPS-177 | maker verify 409/403 | E5 |
| RA-OPS-178 | approve bez PIN 400 | E5 |
| RA-OPS-179 | readonly 403 | E5 |
| RA-OPS-180 | PATCH policy 200 | E10 |
| RA-OPS-181 | stale settings 412 | E10 |
| RA-OPS-182 | threshold -1/101 400 | E10 |
| RA-OPS-183 | autoCapture ON bez max 400 | E10 |
| RA-OPS-184 | readonly 403 | E10 |
| RA-OPS-185 | tenant isolation | E10 |
| RA-OPS-190 | notif audience (actionable only) | E7 |
| RA-OPS-191 | mark read persist | E7 |
| RA-OPS-192 | unique event_id | E7 |
| RA-OPS-193 | BOLA notification 404 | E7 |

Klasy docelowe (gdy implementacja): `MerchantConcurrentRestAssuredTest`, `SupportCaseRestAssuredTest`, `SupportBulkAssignRestAssuredTest`, `RefundChallengeRestAssuredTest`, `OpsFeedInjectRestAssuredTest`, `NotificationRestAssuredTest`, `SavedViewRestAssuredTest`, `TenantPaymentPolicyRestAssuredTest`, plus `SupportModuleTest` / `OpsModuleTest`.
