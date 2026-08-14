# 08 — Macierz śledzenia (FR → testy)

Pokrycie: `existing-*` | `designed` | `docs-only`.

## RLS

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-RLS-01 FORCE RLS + rola | RA-010/011/016/017, RA-036 | existing-ra + `RlsLabModuleTest` | P0 |
| FR-RLS-02 list tenant | E2E-002, RA-012 | existing-ra + existing-pom | P0 |
| FR-RLS-03 foreign 404 | E2E-003, RA-013 | existing-ra + existing-pom | P0 |
| FR-RLS-04 compare | E2E-004/005, RA-014/018/019, EP-043–046, UC-056 | existing-ra + existing-pom + existing-pw | P0 |
| FR-RLS-05 flag off | RA-015 | existing-it | P0 |
| FR-RLS-06 hub nav | E2E-001 | existing-pw | P0 |
| FE flag-off project | E2E-006 | existing-pw (`playwright.rls-flag-off.config.ts`) | P1 |

## Filtry

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-FLT-01 date/status/ref | E2E-020, EP-011 | existing-pom + existing-ra | P0 |
| FR-FLT-02 amount | E2E-021, RA-022 | existing-pw + existing-pom + existing-ra | P0 |
| FR-FLT-03 page-reset | E2E-023 | existing-pw + existing-pom (BFF `page` 0/absent) | P0 |
| FR-FLT-04 pagination mapping | E2E-024/033 | existing-pw | P1 |
| FR-FLT-05 min>max | RA-023 | existing-ra | P0 |
| FR-FLT-06 combo | RA-024, E2E-022 | existing-ra + existing-pom (CREATED × PLN) | P0 |

## Kontrolki / composition / race

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-CTL-01 USelect | E2E-030 | existing-pom | P1 |
| FR-CTL-02 badge | E2E-031 | existing-pw + existing-pom | P1 |
| FR-CTL-03 modal dismiss | E2E-032, UC-032 | existing-pom (`confirm-action-dismiss`) | P0 |
| FR-RACE-01 If-Match | ST-042 | existing-pom | P0 |
| FR-RACE-02 lie/fulfillment | UC-040 | existing-pom | P0 |
| FR-RACE-03 BFF port | E2E-044 | existing-pom | P1 |
| Network abort timeout | 05 | existing-pw | P1 |
| Idempotency replay | payments-create | existing-pom | P0 |

## Docs-only / Wave B

| Wymaganie | ID | Pokrycie |
|---|---|---|
| FR-TLS-LAB | E2E-050/051, STK-004/006/007, RA-030–033, UC-054, SEC-005 | existing-setup + existing-pom TLS + existing-ra |
| REST-SSL-PROXY-01 cz. 1 | RA-030, RA-031 | existing-ra (031 = Caddy-shaped headers w Spring) |
| Stack HTTP + seed | STK-001/002 | existing-setup |
| FE flag-off | E2E-006 | existing-pw |
| Cert trust vs insecure | STK-007, EG-055 | existing-setup + POM TLS config |
| FR-QUERY | EG-043, EG-054 | docs-only (CPL GET-body) |
| FR-SF7-VER | — | docs-only |
| FR-RETRY | — | docs-only |

Pełna mapa warstw Wave B: [09-wave-b-stack-tls-catalog.md](09-wave-b-stack-tls-catalog.md).
