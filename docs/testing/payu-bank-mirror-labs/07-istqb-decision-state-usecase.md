# 07 — Tabele decyzyjne, stany, use case

Design only tam, gdzie Pokrycie ≠ existing. ID: `DT-MRL-*`, `ST-MRL-*`, `UC-MRL-*`, `PWISE-*`, `EG-*`.

## DT-MRL-01 — CSRF × cookie × Bearer

| Cookie sesji | CSRF header | Client | Expect | Pokrycie |
|---|---|---|---|---|
| yes | valid match | BFF `csrf-demo` | 2xx | designed |
| yes | missing | BFF `csrf-demo` | 403 `csrf_failed` | existing-pom |
| yes | wrong | BFF `csrf-demo` | 403 | designed |
| no | any | BFF `csrf-demo` | 401 | designed |
| n/a | missing | Bearer payment POST | 2xx (kontrast) | designed |

CSRF **nie** wolno nakładać na `/api/merchants/**`.

## DT-MRL-02 — Idle × surface × Unlock

| Surface | TTL exceeded | Unlock | Expect | Pokrycie |
|---|---|---|---|---|
| `/admin/**` | no | — | dashboard | — |
| `/admin/session-lab` | yes | — | overlay | existing-pom |
| `/admin/session-lab` | yes | logout | `/login`, sesja **BFF** pusta, SSO może zostać | existing-pom Unlock |
| `/psp/checkout/**` | yes | — | **brak** overlay | existing-pw |
| `/login` | yes | — | n/a | — |

## DT-MRL-03 — Mock vs POM network

| Suite | 503 / lie / PDF | `route.fulfill` | Allowed |
|---|---|---|---|
| mocked e2e | tak | tak | yes (existing) |
| POM | 503 live | nie | yes waitForResponse |
| POM | PDF/lie | **nie** | live albo skip |

## DT-MRL-04 — Step-up high-value

| Amount vs 10000 | `X-Lab-Step-Up` | Expect | Pokrycie |
|---|---|---|---|
| below | any | 200 accepted | designed |
| above | missing / other | 403 `step_up_required` | existing-ra missing |
| above | `confirmed` | 200 + UI copy kwoty | RA 200; UI designed |

## DT-MRL-05 — Flag matrix

| FE public flag | Spring enabled | BFF `/api/mirror-lab/*` | Spring `/api/mirror-lab/*` | Nav |
|---|---|---|---|---|
| on | on (dev) | proxy | 200/4xx biznesowe | visible |
| on | off | 502/503/404 z backendu | 404 IT | visible ale API martwe |
| off | on | **404 Nitro** | 200 jeśli ktoś bije :8080 | hidden |
| off | off | 404 Nitro | 404 | hidden |

Pokrycie: Spring off existing-it; FE off **designed**.

## DT-MRL-06 — TPP token

| Header | Query | Consent | Expect | Pokrycie |
|---|---|---|---|---|
| valid | any | GRANTED | 200 (header) | existing header; designed conflict |
| empty | valid | GRANTED | 200 | existing-ra |
| empty | empty | — | 403 `consent_denied` | designed |
| valid | — | REVOKED | 403 | existing-ra |
| — | — | — | 31. w minucie 429 | designed |

## DT-MRL-07 — Refund checkout session

| Status przed | POST refund | Status po | Event count | Pokrycie |
|---|---|---|---|---|
| COMPLETED | 200 | REFUNDED | +1 `checkout.session.refunded` | existing-ra |
| REFUNDED | 409 | REFUNDED | 0 dodatkowych | existing-ra |
| CREATED/PENDING/CANCELED/EXPIRED | 409 | bez zmian | 0 | designed |

## ST-MRL-01 — Dispute

`OPEN --upload valid--> CLOSED`  
(Implementacja: upload od razu `dispute.close` — brak trwałego `EVIDENCE_ATTACHED`.)

| Przejście | Expect | Pokrycie |
|---|---|---|
| OPEN + txt | CLOSED 200 | existing-ra |
| OPEN + exe | 415, zostaje OPEN | existing-ra (status sporu designed) |
| unknown id | 404 | designed |
| CLOSED + upload | 200 ponownie lub 409 — **designed: potwierdzić** (close jest idempotentny w kodzie) | designed |

## ST-MRL-02 — Maker-checker (lab table, nie payment_orders)

`PENDING_APPROVAL --other subject--> APPROVED`  
Self-approve: 403. Drugi approve: 409 `approval_not_pending`.

Pokrycie: existing-ra + existing-pom UI. Foreign tenant checker: **niezaimplementowane** (brak tenant check na lab table) — EG, nie ST.

## ST-MRL-03 — Consent AIS-lite

`GRANTED --owner revoke--> REVOKED`  
REVOKED → TPP 403. GRANTED → TPP 200 read-only (brak PIS). Other actor revoke → 403, status zostaje GRANTED.

Pokrycie: existing-ra.

## ST-MRL-04 — Checkout session (delta Wave 3)

Istniejący CPL ST: CREATED → PENDING/COMPLETED/CANCELED/EXPIRED.  
**Nowe:** `COMPLETED → REFUNDED` (terminal). Brak `REFUNDED → *`.

Pokrycie: existing-ra refund; Flyway V16 CHECK.

## UC-MRL-01 — Logout on idle Unlock

Login → admin → clock lock → Unlock → `/login` (ścieżka **aplikacji**, nie `end_session`).  
Pokrycie: Unlock URL existing-pom; deep link designed.

## UC-MRL-07 — End OIDC z Session Lab

Login → session-lab → End OIDC session → POST end-session → hop Keycloak.  
Oracle: `client_id`, brak `id_token_hint`. Pokrycie: **designed** (E2E-026, API-024).

## UC-MRL-02 — Lie body

Network Lab lie → UI `success` → (designed) GET fulfillment / CPL oracle nie CONFIRMED.

## UC-MRL-03 — Iframe pay

Widget load session → Approve w `frameLocator` → outcome w ramce; new-tab hosted nadal działa.  
Pokrycie: mocked existing-pw; live designed.

## UC-MRL-04 — Maker-checker dwa ludzie

Manager create → 403 self-approve → Admin wkleja id → Approve 200.  
Pokrycie: existing-pom (bez DOM hack).

## UC-MRL-05 — Consent desktop

Grant na `/consent/mirror-lab` → TPP header → revoke → TPP 403.  
Pokrycie: designed UI; RA existing.

## UC-MRL-06 — Guest hosted vs dashboard

Nowy context bez storage → hosted 200; `/admin/session-lab` → login.  
Pokrycie: existing-pw hosted; existing-pom guest.

## PWISE (P2)

- lang × colorScheme  
- SameSite × CORS credentials × Origin  
Tylko jeśli flakuje — nie na start.

## EG — Error guessing (review findings)

| ID | Hipoteza | Oracle | Pokrycie |
|---|---|---|---|
| EG-MRL-01 | Unlock bez clear() bounce na merchants | URL login trwały | existing-pom częściowo |
| EG-MRL-02 | PDF jako `responseType: text` | `%PDF-` w pliku | existing-pw mocked |
| EG-MRL-03 | Location `?lang=` psuje UUID | sessionIdFromLocation | existing-ra |
| EG-MRL-04 | Repeat refund = dwa eventy | COUNT=1 | existing-ra |
| EG-MRL-05 | BFF bez requireMirrorLab | 404 gdy flag off | designed |
| EG-MRL-06 | GET body na POST | nie 403 get_with_body | designed |
| EG-MRL-07 | Servlet 1MB ukrywa 413 2MB | yaml 5MB | existing-ra 413 |
| EG-MRL-08 | 503 store globalny | TTL 10s | designed |
| EG-MRL-09 | TPP token w query logach | header w UI | designed NET-107 |
| EG-MRL-10 | UInput approval-id innerText puste | inputValue/fill | existing-pom |
