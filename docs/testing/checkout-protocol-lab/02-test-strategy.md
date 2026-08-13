# 02 — Strategia testów (test architect)

Checkout Protocol Lab to **edukacyjny** łańcuch redirect+notify, nie produkcyjny PSP. Strategia maksymalizuje walor nauki (identity worlds, 400 vs 503, oracle fulfillment) i pokrycie ryzyka protokołu.

## 1. Cele jakości

| Cel | Mierzalny oracle |
|---|---|
| Pieniądze zmienia tylko zweryfikowany event | Fulfillment `CONFIRMED`/`CANCELLED`/`EXPIRED` + wiersz `checkout_event` |
| Return URL kłamie | Query `status=success` **i** fulfillment wciąż `AWAITING_PAYMENT` |
| Trzy (cztery) tożsamości nie mieszają się | Keycloak ≠ `lab.*` Bearer ≠ HMAC ≠ `Lab-Simulate-Token` |
| 400 vs 503 | Bad HMAC → 400, 0 eventów; transient → 503, retry → 202 |
| UUID hosted nie wystarcza do simulate | POST bez tokenu → **403** `missing_simulate_token`, nie 401 |
| Idempotency | Replay ten sam body → ten sam `sessionId` + `Idempotency-Replayed`; inny body → 409 |
| Flaga | `app.checkout-lab.enabled=false` → 404, brak beanów |

NFR-01 (p95 ACK poniżej 200 ms) — poza katalogiem funkcjonalnym; osobny test wydajności jeśli kiedyś mierzony.

## 2. Piramida i narzędzia

```text
        PW-E2E UI (mało, drogie, multi-tab)
           PW-API Playwright request (protokół HTTP)
              REST Assured (już jest — kontrakt Spring)
                 Unit: SignatureService, scenario parse
                    Schema IT / Modulith
```

| Warstwa | Co testuje | Czego nie |
|---|---|---|
| Unit | HMAC compare `MessageDigest.isEqual`, parse scenario, outcome aliases | HTTP |
| RA (existing) | Status, headers, problem+json, DB przez JDBC | Browser, SSR Nuxt |
| PW-API (designed) | Ten sam kontrakt z `APIRequestContext` — nauka SDET „REST w Playwright” | UI |
| PW-E2E | Binding session, multi-tab, lie return, learning copy, `data-testid` | Surowy HMAC (to API) |

**Zasada:** nie dublować 1:1 każdego RA w Playwright. PW-API ma wartość tam, gdzie SDET ćwiczy ten sam kontrakt innym klientem **oraz** tam, gdzie RA nie pokrywa (BFF forward, `redirect: 'manual'`).

## 3. Ryzyko → głębokość

| Ryzyko | Priorytet | Technika | Warstwa |
|---|---|---|---|
| Fulfillment z kłamliwego return | P0 | Use case + ST | PW-E2E + API GET fulfillment |
| Simulate bez capability token | P0 | DT auth | PW-API + PW-E2E |
| HMAC 400 (no retry) vs 503 (retry) | P0 | EP + DT | PW-API / RA |
| Idempotency conflict | P0 | DT | PW-API / RA |
| Expired link 409, 0 eventów | P0 | BVA clock + ST | PW-API + PW-E2E |
| Worker SKIP LOCKED → CONFIRMED | P0 | ST event | RA / PW-API + awaitility |
| Decline → session `CANCELED` / fulfillment `CANCELLED` | P1 | ST | PW-E2E + API |
| CORS preflight OPTIONS | P1 | Checklist | PW-API / RA |
| Booking cash bez sesji | P1 | EP mode | PW-E2E + API |
| Inspector ErrorState / anomalies | P2 | Use case | PW-E2E |
| Copy 200 vs 302 | P2 | Checklist learning | PW-E2E |
| `OOO_EVENTS` | P1 blocked | — | najpierw produkt |

## 4. Oracles (obowiązkowe)

1. **Fulfillment status** (`GET …/fulfillment` hosted lub Bearer) — jedyny dowód pieniędzy.
2. **HTTP status + problem `error`** — nie tylko „toBeTruthy”.
3. **Nagłówki:** `Location`, `Idempotency-Replayed`, `X-Correlation-ID`, `Lab-Signature`, `Access-Control-Allow-Headers`.
4. **Inbox:** `processStatus`, `ackStatus`, `eventId` uniqueness, delivery log `responseStatus`.
5. **Negatywny oracle:** 0 wierszy event po 400; 0 notify po `expired_link`.
6. **Zakaz:** `page.waitForTimeout`; `continueUrl` jako jedyny assert; sleep na expiry (użyj `POST /clock`).

## 5. Dane testowe

| Zasada | Przykład |
|---|---|
| UUID v4 (Zod `z.string().uuid()`) | `11111111-1111-4111-8111-111111111111` — nibble wersji `4`, variant `8` |
| Nie używać | `11111111-1111-1111-1111-111111111111` (odrzucone przez Zod) |
| `extOrderId` unikalny | `BOOK-{timestamp}` / `CASH-{timestamp}` |
| Correlation | jawny `X-Correlation-ID` w request — echo w response i error body |
| Idempotency-Key | UUID per test; ten sam klucz = replay/conflict |
| Simulate token | z GET hosted, nie hardcoded (poza mockiem PW UI) |

Izolacja: `POST /api/checkout-lab/reset` czyści tylko `checkout_*` — nie merchants/payments. PW UI mockuje BFF, więc nie wymaga resetu.

## 6. Mapowanie ISTQB Foundation Level 4.x → CPL

| Sylabus FL | Technika | Gdzie w CPL | Plik |
|---|---|---|---|
| 4.2.1 | Equivalence partitioning | currency, mode, outcome, grant_type, processStatus | [06](06-istqb-ep-bva.md) |
| 4.2.2 | Boundary value analysis | amountMinor 1 / 1e8, extOrderId 120, HMAC ±300s, validitySeconds | [06](06-istqb-ep-bva.md) |
| 4.2.3 | Decision table | auth matrix; simulate gate; notify ACK | [07](07-istqb-decision-state-usecase.md) |
| 4.2.4 | State transition | session, fulfillment, event processStatus | [07](07-istqb-decision-state-usecase.md) |
| 4.2.5 | Use case testing | book-pay-return, cash, lie, cancel, pay_no_return | [07](07-istqb-decision-state-usecase.md) + [03](03-playwright-e2e-catalog.md) |
| 4.3 | White-box (statement/branch) | SignatureService unit — poza E2E; nie katalogujemy tu ścieżek bytecode | — |
| 4.4.1 | Error guessing | re-serialize JSON, Bearer na notify, double Approve | [07](07-istqb-decision-state-usecase.md) `EG-*` |
| 4.4.2 | Exploratory (sesja) | charter: „czy da się potwierdzić fulfillment bez event?” — notatka, nie TC | [07](07-istqb-decision-state-usecase.md) |
| 4.4.3 | Checklist-based | RFC 9457 pola, CORS, OPTIONS, flaga off | [07](07-istqb-decision-state-usecase.md) |
| Interview extras | Pairwise / classification tree | mode × currency × scenario | [07](07-istqb-decision-state-usecase.md) `PWISE-*` |
| Interview extras | CRUD + HTTP method semantics | GET vs POST vs OPTIONS; 405 na złej metodzie | [04](04-playwright-api-auth-sessions.md) |

## 7. Identity worlds — reguła projektowania TC

| Świat | Kto | Endpointy | TC musi pokazać |
|---|---|---|---|
| Keycloak JWT | Dashboard Nuxt | UI hub/booking/inspector | PW-E2E auth; **nie** wystarcza na Spring checkout-lab (osobny chain `permitAll` + lab Bearer) |
| Lab Bearer `lab.*` | BFF / RA | `/sessions`, `/bookings`, `/clock`, `/reset`, `/reconcile`, `/anomalies` | 401 bez tokenu; 200/302 z tokenem |
| HMAC `Lab-Signature` | Stub PSP → notify | `POST /notify` | 400 bez/złego podpisu; JWT zbędny |
| Simulate HMAC | Hosted page | `POST …/simulate` | 403 missing/invalid; 200 z tokenem z GET |
| Public | Płatnik | GET hosted, GET hosted fulfillment, health, oauth | Brak Keycloak cookie |

Kluczowa asercja edukacyjna: **Keycloak JWT na `POST /sessions` bez lab Bearer → nadal 401** (filtr lab, nie resource server).

## 8. Existing vs designed

Kolumna **Pokrycie** w katalogach 03–05. Skrót tego, co **już jest** (nie projektujemy od zera, tylko odwołujemy):

### REST Assured (existing-ra)

`CheckoutLabProtocolRestAssuredTest`: health; hosted GET token; simulate 403; notify happy+worker; `NOTIFY_5XX_RETRY`; `BAD_SIGNATURE`; duplicate 200; cash; idempotency replay+conflict; notify bez JWT; CORS `Lab-Signature`; clock→409; reset scope; reconcile dedup.

`CheckoutLabOAuthTokenRestAssuredTest`: form 200; wrong secret 401; JSON CT → 401 nie 415; lab token odrzucony przez KC decoder.

`CheckoutLabCreateSessionRestAssuredTest`: 302+Location+persist; 401; invalid currency.

`CheckoutLabGetSessionRestAssuredTest`: 200 snapshot; GET idempotent; 404 problem; 401.

`CheckoutLabSecurityChainTest`: OAuth public; notify 400 nie 401; GET oauth 401 (main chain); POST sessions 401; OPTIONS sessions bez Bearer; correlation echo.

### Playwright UI (existing-pw)

Hub copy + booking; sidebar; Error Lab via **sidebar** (SSR); cash (mock `CASH-*`); multi-tab approve + return CONFIRMED; inspector processStatus+Lab-Signature; lie return; hosted bez dashboard session.

### Luki (designed — ten pakiet)

Pełna lista w 03–07. Najwyższy P0: Decline/CANCELLED, simulate 403 w UI, pay_no_return, BVA amount, unknown scenario, hosted fulfillment 404 empty vs Bearer 404 problem, Bearer na bookings/ops, BFF forward token, `RETURN_LIE_SUCCESS` przez `Lab-Force-Scenario` w API.

## 9. Playwright — ograniczenia implementacyjne (gdy przyjdzie czas)

- `/error-lab` jest SSR: `page.goto('/error-lab')` omija `page.route` session mock → **sidebar client nav**.
- `/admin/**` ma `ssr:false` — mock sesji działa.
- Hosted GET mock **nie** może połykać `/fulfillment` i `/simulate`.
- `waitForEvent('page')` **przed** click Open hosted.
- Zero `waitForTimeout`.
- F-D2 i CPL dzielą `psp-approve` / `psp-decline` — asercje zawsze ze ścieżką `/psp/checkout/` vs `/psp-redirect-simulator`.

## 10. Definicja „test zaliczony” dla protokołu

Przypadek REST zaliczony tylko gdy sprawdzono **łącznie**: status, `Content-Type` (json vs problem+json vs empty 401), kluczowe nagłówki, ciało (`error`/`duplicate`/`sessionId`), oraz efekt uboczny (event count, fulfillment status) tam gdzie dotyczy.

Przypadek E2E zaliczony gdy UI **oraz** oracle fulfillment (mock lub live) są spójne z hopem — nie samym toastem.
