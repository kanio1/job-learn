# 06 — Indeks scenariuszy → kod

Pełne tabele SCN: plan sesji §O–P oraz `docs/testing/live-pom-wave-2/`. Tu tylko ślad implementacji w `tests-pom`.

| SCN | Spec / klasa | Fala |
|---|---|---|
| ISO-01, 06, 09 | `specs/tenant-scope.spec.ts` + `IsolationDtUc` | 1 |
| ISO-02, 03, 10 | ten sam spec, REST `BffClient` | 1 |
| PAY-01…03 | `payments-create.spec.ts` + `IdempotencyMatrix` (replay / conflict) | 2–3 |
| PAY-04, 05 | ten sam spec — BFF **nadaje** klucz przy braku/pustym (201, dwa różne ID); Spring sam dałby 400 | M4 |
| PAY-06…11, 09c | `payments-create.spec.ts` + `AmountPartitions` BVA (w tym waluta 4 znaki) | M4 |
| PAY-12…15 | `payments-create.spec.ts` + `OrderReferencePartitions` (0 / 1 / 120 / 121) | M4 |
| LIF-01…03 | `payments-lifecycle.spec.ts` + `LifecycleStDt` | 2 |
| LIF-03…05, ILL-01…05 | `payments-illegal-transitions.spec.ts` + `IllegalStDt` | M4 |
| IFM-01…04 | ten sam spec + `IfMatchActionMatrix` (cancel/capture × If-Match) | M4 |
| IFM-05, 06 | `payments-conditional.spec.ts` + `ifMatchPatchMatrix` PATCH 412/428; fresh 200 | A |
| CAP-OVER | `payments-eligibility.spec.ts` + `CaptureAmountPartitions`; UI drawer 422 w `payments-lifecycle.spec.ts` | A |
| INEL-01 | existing-ra `PaymentOrderRestAssuredTest` DRAFT 409; POM nie suspenduje seed merchantów (SUSPENDED terminalny) | A |
| SUM / MR-SUMMARY | `payments-summary.spec.ts` + `SummaryInclusion`; UI karty listy | A |
| HIST-01 | `payments-summary.spec.ts` GET history; `payments-lifecycle.spec.ts` zakładka History | A |
| DC-01…04 | `payments-refund-dual-control.spec.ts` HTTP 409/201/409/200 + UI hint bez POST `/refund` | A |
| EVD-GET | `payments-evidence-export.spec.ts` GET bytes + UI download | A |
| CLIP-01 | `payments-lifecycle.spec.ts` clipboard copy reference | A |
| SWEEP-01 | `admin-bff.spec.ts` 200; `payments-eligibility.spec.ts` manager 403; `merchants.spec.ts` UI toast | A |
| ISO-GET-002 | `tenant-scope.spec.ts` GET order via ALPHA_002 → 404 | A |
| USR-FLT / USR-400 | `users.spec.ts` role/status filter + short password 400 | B |
| AUD-FLT / AUD-ID | `audit.spec.ts` action filter + GET `{id}` + deep-link `?entry=` | B |
| MER-ST-FLT | `merchants.spec.ts` filtr status DRAFT vs Active | B |
| TEN-428/412 | `tenant-settings.spec.ts` PATCH bez If-Match / stale | B |
| NOTES-01 | `support-rbac.spec.ts` — SUPPORT_AGENT notes POST 201 (JWT = rbacMatrix) + `BffClient.postNote` | M5/B |
| MER-04 | `merchants.spec.ts` activate SUSPENDED | M4 |
| MER-05, 06, 09 | empty UI / duplicate 409 / platform bez tenant 400 | 2 / M5 |
| MER-07, 08, 10, 11 | `merchants.spec.ts` + długość referencji 2 / 3 / 64 / 65 | M4 |
| MER-12 | `tenant-scope.spec.ts` — tenant.admin UI create bez pola tenant (JWT) | M5 |
| MER-13 | `merchants.spec.ts` — platform admin UI + pole `tenantReference` | M5 |
| RO-01 | `readonly-rbac.spec.ts` + `MerchantAccessMatrix` — lista tak, create/lifecycle/notes nie | M6 |
| MR-IDEM/UNIQ/ETAG/FILTER | `payments-metamorphic.spec.ts` | M4b |
| MR-ISO | `tenant-scope.spec.ts` | M4b |
| EG-W2-02 | `session-guest.spec.ts` — `BffClient.DEFAULT_BASE_URL` = `127.0.0.1` | M4 |
| EG-W2-11 | `session.spec.ts` — drugi logout zostaje na `/login` | M4 |
| SES-01…07, MER-*, CPL-*, EL-* | existing specs | nie duplikować |

Żaden SCN nie woła `seed-learning` / ETL.

M7 **done**: `apps/frontend/tests/e2e/**` nie istnieje. Product Playwright = `tests-pom`. Learner = `tests-pom-learner` (bez mocków, bez importu z `tests-pom`).
