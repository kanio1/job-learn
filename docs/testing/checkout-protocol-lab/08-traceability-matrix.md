# 08 — Macierz śledzenia (FR → testy)

Kolumny: wymaganie → technika → ID → pokrycie dziś → priorytet **implementacji testu** (P0–P2).  
`existing-*` = nie blokuje; `designed` = luka testowa; `blocked` = luka produktowa.

Priorytet P0 = pieniądze, auth, 400/503, oracle. Nie implementować w tym zadaniu — tylko mapa.

---

## FR / NFR

| Wymaganie | Technika | ID | Pokrycie | Prio |
|---|---|---|---|---|
| FR-01 OAuth | EP | PW-API-010–016 | existing-ra + designed | P1 designed |
| FR-02 302 create | UC, BVA | PW-API-020–040, BVA-001–007 | existing-ra; BVA designed | P0 BVA |
| FR-03 Hosted bind | UC | PW-E2E-020, PW-API-100 | existing-pw/ra | — |
| FR-04 continueUrl kłamstwo | UC-03 | PW-E2E-040, PW-API-071 | existing-pw; RA designed | P0 RA |
| FR-05/06 HMAC 202 | DT-NFY | PW-API-200–201 | existing-ra | — |
| FR-07 Worker refetch | ST-EVT | PW-API-055, 200 | existing-ra | P2 PROCESSING race |
| FR-08 Dedup 200 | DT-N02 | PW-API-208 | existing-ra | — |
| FR-09 400 vs 503 | DT-N03/N04 | PW-API-203, 213 | existing-ra | P1 HMAC BVA |
| FR-10 validityUntil | ST-004, BVA clock | PW-API-130, PW-E2E-024 | existing-ra; PW designed | P0 UI expiry |
| FR-11 Idempotency | EP-110–114 | PW-API-024–026 | existing-ra; validitySeconds designed | P1 |
| FR-12 Booking cash/online | DT-MODE, EP-050 | PW-E2E-010–012, PW-API-300–302 | existing; mode select designed | P0 UI mode |
| FR-13 Inspector | UC-07 | PW-E2E-060–068 | existing cienkie | P1 deliveries/anomalies |
| FR-14 Scenarios | EP-100, PWISE | PW-API-070–077 | partial | P0 lie RA; blocked OOO |
| FR-15 Reconcile | UC-09 | PW-API-405–407 | existing-ra bean/HTTP | P1 GET anomalies |
| NFR-02 Correlation | CL-03 | PW-API-010, 028 | existing-ra partial | P2 |
| NFR-04 Constant-time | unit | SignatureServiceTest | existing | — |
| NFR-05 Modulith | arch | CheckoutLabModuleTest | existing | — |
| C-03 Flag off | CL-04 | PW-API-002 | existing-it | P2 PW flag |

---

## Scenariusze E6-S2

| Scenario | ID | Pokrycie | Prio | Uwaga |
|---|---|---|---|---|
| HAPPY_COMPLETED | PW-E2E-050, PW-API-113, 200 | existing | — | |
| USER_CANCEL | PW-E2E-022, 042, DT-S02 | **existing-pom** decline + fulfillment CANCELLED | P0 | header no-op nadal RA |
| RETURN_LIE_SUCCESS | PW-E2E-040, PW-API-071 | pw + pom yes; API designed | P0 | |
| BAD_SIGNATURE | PW-API-072, 204 | existing-ra | — | |
| NOTIFY_5XX_RETRY | PW-API-073, 213, 057 | existing-ra | — | |
| OOO_EVENTS | PW-API-076, PWISE-10 | **blocked** | — | GAP-01 |
| EXPIRED_LINK | PW-API-030, 130, PW-E2E-024 | ra clock; POM `psp-link-expired`; Approve-block designed | P1 | |
| PAY_NO_RETURN | PW-E2E-043, PW-API-075 | **designed** | P0 | |

Unknown scenario: PW-API-029 P1.

---

## Hops

| Hop | ID | Pokrycie |
|---|---|---|
| OAuth token | PW-API-010 | existing-ra |
| POST sessions 302 | PW-API-020 | existing-ra |
| GET hosted + simulateToken | PW-API-100 | existing-ra |
| POST simulate + header | PW-API-110, 113 | existing-ra 403+helper |
| POST notify ACK | PW-API-200 | existing-ra |
| Worker CONFIRMED | PW-API-200 | existing-ra |
| Hosted fulfillment oracle | PW-API-131–133, PW-E2E-041 | pw yes |
| Cash booking | PW-API-300 | existing-ra |

---

## Identity / security

| Temat | ID | Pokrycie | Prio |
|---|---|---|---|
| 401 empty vs problem+json | EG-08, PW-API-021 | existing-ra | P1 bookings/ops 401 |
| KC JWT ≠ lab Bearer | PW-API-022, 016 | 016 existing | P0 022 |
| HMAC ≠ JWT | PW-API-201–202 | 201 existing | P1 |
| 403 ≠ 401 simulate | PW-API-110, PW-E2E-025 | ra 110; UI designed | P0 |
| OPTIONS skip Bearer | PW-API-023, 453 | existing-ra | — |
| CORS Lab-Signature | PW-API-450 | existing-ra | — |
| CORS Lab-Simulate-Token | PW-API-451 | designed | P1 |
| Public hosted/return | PW-E2E-020, 045 | 020 existing | P1 045 |
| Unauth dashboard | PW-E2E-007 | designed | P0 |

---

## ISTQB artefacts → ID

| Artefakt | ID zakres |
|---|---|
| EP/BVA amount, currency, extOrderId | BVA-001–016, EP-020–026 |
| BVA HMAC ±300s | BVA-030–034 |
| DT auth / simulate / notify / mode | DT-A*, DT-S*, DT-N*, DT-M* |
| ST session / fulfillment / event | ST-001–025 |
| Use cases | UC-01–10 |
| Pairwise | PWISE-01–10 |
| Error guessing / checklist | EG-01–12, CL-01–08 |

Szczegóły wartości: [06](06-istqb-ep-bva.md), [07](07-istqb-decision-state-usecase.md).

---

## Backlog implementacji testów (gdy przyjdzie czas)

Nie część tego dokumentu wykonawczego — kolejka **sugerowana**:

### P0 (pierwsza fala PW-API + 3 E2E)

1. PW-API-022 KC JWT na sessions → 401  
2. PW-API-071 RETURN_LIE_SUCCESS 0 eventów  
3. PW-API-111/112 invalid simulate token  
4. PW-API-115 Decline aliases + fulfillment CANCELLED  
5. PW-API-130 już jest RA — E2E-024 expired UI  
6. PW-E2E-011 CASH przez **mode select**  
7. PW-E2E-022/042 Decline flow  
8. PW-E2E-043 pay_no_return  
9. PW-API-304/401/404/407 Bearer na bookings/ops  
10. BVA-001/002/006/007 amount granice  

### P1

HMAC ±300s; unknown scenario; outcome PENDING/FOO; hosted 404 vs fulfillment 404 empty; inspector deliveries/anomalies/ErrorState; CORS simulate token; idempotency validitySeconds replay; BFF 302 manual; GET events contract.

### P2

Copy 200 vs 302; debug panels; command palette; flaga FE; clock parse 500; pairwise CASH×scenario; EG trailing slash.

### Nie robić

- Duplikat 1:1 wszystkich 28 RA w Playwright.  
- F-D2 jako substytut CPL.  
- `OOO_EVENTS` aż GAP-01 (fix lub wykreślenie).  
- Testy Kafka/PAN/payment_orders.

---

## Metryka kompletności mapy (dokument, nie kod)

| Katalog | Liczba ID (ok.) |
|---|---|
| PW-E2E | ~45 |
| PW-API | ~90 (001–077, 100–133, 200–214, 300–311, 400–453) |
| EP/BVA | ~50 |
| DT/ST/UC/PWISE/EG/CL | ~80 wierszy tabel |
| **Suma zaprojektowanych** | **~250** punktów asercji (wiele mapuje 1:1 na ten sam TC) |

Po collapse (jeden TC wiele EP): **~70–90** wykonalnych speców Playwright, plus istniejące RA/PW jako baza.

---

## Słownik błędów (powtórzenie ściągi)

| HTTP | error | code |
|---|---|---|
| 400 | `validation` | `VALIDATION` |
| 400 | `invalid_signature` | `INVALID_SIGNATURE` |
| 401 | (brak) | empty body |
| 403 | `missing_simulate_token` | `MISSING_SIMULATE_TOKEN` |
| 403 | `invalid_simulate_token` | `INVALID_SIMULATE_TOKEN` |
| 404 | `not_found` | `NOT_FOUND` |
| 409 | `idempotency_conflict` | `IDEMPOTENCY_CONFLICT` |
| 409 | `expired_link` | `EXPIRED_LINK` |
| 503 | `transient_error` | `TRANSIENT_ERROR` |
| 202 / 200 notify | — | `{duplicate}` |

Oracle pieniędzy: **fulfillment**, nie `continueUrl`.
