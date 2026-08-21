# 02 — Use cases

Format: aktor, trigger, sukces, błąd, oracle. Wszystkie na żywym BFF.

Flow PM + mermaid: [00-business-flows.md](00-business-flows.md). AT: [08-acceptance-tests.md](08-acceptance-tests.md).

| ID | BC | Aktor | Główna ścieżka | Alternatywa / błąd | Oracle | Testy |
|---|---|---|---|---|---|---|
| UC-M360-00 | 00 | Dev | `--app`, status 200, issuer | zły `.env` session password | curl / script | manual |
| UC-M360-01 | 01 | platform.admin | filtr Alpha+ACTIVE strona 2 | zły sort 400 | SQL plan / RA | RA-M360-010+ |
| UC-M360-02 | 02 | tenant.admin | GET page content | obcy tenant pusty | body+DB | RA-M360-016 |
| UC-M360-03 | 03 | merchant.manager | list CAPTURED sort amount | status=NOPE 400 | RA | RA-M360-020 |
| UC-M360-04 | 02 | platform.admin | Overview = totalElements | pierwsza strona 20 ≠ total | POM | PW-M360-E2E-001 |
| UC-M360-10 | 10 | platform.admin | Apply filters, Back | page reset 0 | URL+GET | E2E-030/031 |
| UC-M360-11 | 10 | platform.admin | sort Updated toggle | — | waitForResponse | E2E-020/023 |
| UC-M360-12 | 11 | platform.admin | bulk 2 DRAFT | mixed statuses | POST×2 | E2E-040/041 |
| UC-M360-13 | 10 | platform.admin | q miss → empty | 403 manager | UI | E2E-050/052 |
| UC-M360-20 | 20 | support.agent | Open 360, Escape | 404 deep link P2 | dialog | E2E-060/062 |
| UC-M360-21 | 21 | platform.admin | Create unique | 409 dup, BVA | 201/409 | E2E-070/072 |
| UC-M360-30 | 30 | readonly.user | lista read-only | POST activate 403 | UI+API | SEC-010, API-040 |
| UC-M360-31 | 31 | 2× admin | A stale If-Match | 412 Reload | dwa context | SEC-020 |
| UC-M360-40 | 40 | platform.admin | preview CSV | bad header | no INSERT | E2E-080/081 |
| UC-M360-41 | 40 | platform.admin | commit | drugi commit 409 | list+UK | E2E-084/085 |
| UC-M360-42 | 41 | merchant.manager.wN | Move to AUTHORIZED | 412 rollback | POST lifecycle | E2E-090/093 |
| UC-M360-50 | 50 | platform.admin | expand Alpha | tenant.admin scope | treeitem | E2E-100/103 |
| UC-M360-51 | 51 | platform.admin | Ctrl+K unique ref | puste q 400 | search GET | E2E-110, RA-071 |
| UC-M360-52 | 52 | merchant.manager | legend = summary | 403 | JSON=UI | E2E-120 |
| UC-M360-60 | 60 | merchant.manager | dzień expiresAt | clock | calendar | E2E-130 |
| UC-M360-61 | 61 | platform.admin | activate → timeline | — | audit | E2E-132 |
| UC-M360-62 | 62 | merchant.manager | wizard create | empty step | 201 | E2E-135 |
| UC-M360-63 | 63 | tenant.admin | inline rename | 412 stale | PATCH | E2E-150/151 |

Główny happy-path **Customer 360 analog**: UC-10 → UC-20 → UC-21 → UC-61 → UC-30 → UC-31 → UC-41.

### Czego uczą testy przypięte do UC

| UC | E2E uczy | REST (BFF/RA) uczy |
|---|---|---|
| UC-M360-04 | Overview ≠ długość strony | `totalElements` w JSON |
| UC-M360-10/11 | URL+Back; header sort | query `sort=` / `q=` 200 |
| UC-M360-21 | formularz BVA/409 na create | RA unique UK |
| UC-M360-30 | readonly bez Save | POST activate 403 mimo UI |
| UC-M360-31 | dwa `storageState` | 412 + DB unchanged |
| UC-M360-40 | `setInputFiles` preview | brak INSERT po preview |
| UC-M360-42 | Move = POST lifecycle | If-Match na authorize |
| UC-M360-51 | Ctrl+K last response | GET `/api/search` limit |

Ops UC (conflict tabs, PIN, WS, i18n, gallery) są w [02-use-cases Wave 2](../ops-wave-2-interaction-lab/02-use-cases.md) — nie powielać tu jako drugi CRM.
