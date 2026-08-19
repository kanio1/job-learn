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
| MER-04 | `merchants.spec.ts` activate SUSPENDED | M4 |
| MER-05, 06, 09 | empty UI / duplicate 409 / platform bez tenant 400 | 2 / M5 |
| MER-07, 08, 10, 11 | `merchants.spec.ts` + długość referencji 2 / 3 / 64 / 65 | M4 |
| MER-12 | `tenant-scope.spec.ts` — tenant.admin UI create bez pola tenant (JWT) | M5 |
| MER-13 | `merchants.spec.ts` — platform admin UI + pole `tenantReference` | M5 |
| RO-01 | `readonly-rbac.spec.ts` + `MerchantAccessMatrix` — lista tak, create/lifecycle/notes nie | M6 |
| NOTES-01 | `support-rbac.spec.ts` — SUPPORT_AGENT notes POST 201 (JWT = rbacMatrix) | M5 |
| MR-IDEM/UNIQ/ETAG/FILTER | `payments-metamorphic.spec.ts` | M4b |
| MR-ISO | `tenant-scope.spec.ts` | M4b |
| EG-W2-02 | `session-guest.spec.ts` — `BffClient.DEFAULT_BASE_URL` = `127.0.0.1` | M4 |
| EG-W2-11 | `session.spec.ts` — drugi logout zostaje na `/login` | M4 |
| DC-01…04 | `payments-refund-dual-control.spec.ts` + `DualControlStDt` | 3 (import) |
| SES-01…07, MER-*, CPL-*, EL-* | existing specs | nie duplikować |

Żaden SCN nie woła `seed-learning` / ETL.

M7 (kasacja `tests/e2e/**`) czeka na potwierdzenie, że learner nie potrzebuje kopii mocków.
