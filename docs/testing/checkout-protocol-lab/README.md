# Mapa testów — Checkout Protocol Lab

Dokumentacja **task-force** (product manager + test architect + test analyst).  
Cel: kompletna mapa przypadków testowych dla CPL. **Nie jest to implementacja** speców Playwright / REST Assured.

| Rola | Odpowiedzialność w tym pakiecie |
|---|---|
| Product manager | Luki biznesowe vs FR/scenariusze — [01-business-gap-analysis.md](01-business-gap-analysis.md) |
| Test architect | Warstwy, ryzyko, oracles, ISTQB — [02-test-strategy.md](02-test-strategy.md) |
| Test analyst | Katalogi TC (E2E, REST, EP/BVA, DT/ST) — pliki 03–08 |

Źródła wymagań: [status/roadmaps/checkout-protocol-lab/](../../status/roadmaps/checkout-protocol-lab/), [docs/implementation/checkout-protocol-lab-hops.md](../../implementation/checkout-protocol-lab-hops.md).  
Istniejące wykonanie (odniesienie, nie duplikat): RA w `CheckoutLabProtocolRestAssuredTest` + `CheckoutLabSecurityChainTest`; PW w `apps/frontend/tests/e2e/checkout-lab.spec.ts`.

---

## Jak czytać

1. Werdykt produktowy i luki → **01**.
2. Strategia, techniki, oracles → **02**.
3. Przypadki UI → **03**.
4. Przypadki REST (Playwright `APIRequestContext`) → **04** (auth/sessions) i **05** (hosted/notify/ops).
5. Projektowanie czarnoskrzynkowe ISTQB FL → **06** (EP/BVA), **07** (DT/ST/UC/pairwise).
6. Śledzenie FR → TC → **08**.

Każdy REST case podaje **method, path, headers, body, status, `error`/`code`, nagłówki odpowiedzi**.  
Oracle pieniędzy = **fulfillment / event / DB**, nigdy sam query `status=success` na `continueUrl`.

---

## Indeks plików

| Plik | Zawartość | Prefiks ID |
|---|---|---|
| [01-business-gap-analysis.md](01-business-gap-analysis.md) | FR-01…15, hops, 8 scenariuszy: Done / Partial / Missing | `GAP-*` |
| [02-test-strategy.md](02-test-strategy.md) | Piramida, ryzyko, ISTQB map, existing vs designed | — |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | Przepływy UI (dashboard, hosted, return, inspector) | `PW-E2E-###` |
| [04-playwright-api-auth-sessions.md](04-playwright-api-auth-sessions.md) | OAuth, health, sessions, events, deliveries | `PW-API-001`–`099` |
| [05-playwright-api-hosted-notify-ops.md](05-playwright-api-hosted-notify-ops.md) | Hosted, simulate, notify HMAC, bookings, ops, CORS | `PW-API-100`–`499` |
| [06-istqb-ep-bva.md](06-istqb-ep-bva.md) | Equivalence partitioning + boundary value analysis | `EP-*`, `BVA-*` |
| [07-istqb-decision-state-usecase.md](07-istqb-decision-state-usecase.md) | Decision tables, state transition, use case, pairwise, error guessing | `DT-*`, `ST-*`, `UC-*`, `PWISE-*`, `EG-*` |
| [08-traceability-matrix.md](08-traceability-matrix.md) | FR/NFR/scenariusz → ID + priorytet P0–P2 | — |

---

## Warstwy wykonania (gdy kiedyś implementować)

```text
Browser UI  ──►  Nuxt BFF :3000 /api/checkout-lab/*  ──►  Spring :8080 /api/checkout-lab/*
                 (session Keycloak / lab Bearer)           (lab Bearer / HMAC / simulate token)
```

| Warstwa | Narzędzie zaprojektowane tutaj | Wymaga |
|---|---|---|
| UI E2E | Playwright chromium, `data-testid` | Nuxt (`pnpm dev`); większość obecnych speców **mockuje** BFF |
| REST protokół | Playwright `request` przeciwko `:8080` | Spring + Postgres; **nie** startuje z `playwright.config.ts` `webServer` |
| REST BFF | Playwright `request` przeciwko `:3000` | Nuxt; testuje proxy (forward `Lab-Simulate-Token`, `redirect: 'manual'`) |
| Kontrakt backend (już jest) | REST Assured `*Test.java` | `./mvnw test` — nie powielać ślepo w PW |

Playwright `webServer` startuje tylko frontend (`reuseExistingServer: false`). Testy HMAC / 302 / clock / reset **nie przejdą** bez backendu — oznacz je `needs-backend` (jak live config).

---

## Konwencja ID i status pokrycia

Format: `{PREFIX}-{NNN}` + kolumna **Pokrycie**:

| Wartość | Znaczenie |
|---|---|
| `existing-pw` | Jest w `checkout-lab.spec.ts` |
| `existing-ra` | Jest w RA (Protocol / OAuth / Create / Get / SecurityChain) |
| `designed` | Zaplanowany, **niezaimplementowany** |
| `blocked` | Zależny od luki produktowej (np. `OOO_EVENTS`) |

Priorytet implementacji (gdy przyjdzie kolej): **P0** protokół/pieniądze/auth, **P1** scenariusze i stany, **P2** copy/UX/walidacja formularza.

---

## Heatmapa (skrót)

| Obszar | RA | PW UI | Katalog designed |
|---|---|---|---|
| OAuth client_credentials | tak | — | 04 |
| POST sessions 302 + idempotency | tak | — | 04, 06 |
| Hosted GET + simulate token | tak | częściowo | 05, 03 |
| Notify 202/200/400/503 | tak | nie | 05 |
| Cash vs ONLINE booking | tak | częściowo (mock `CASH-*`) | 03, 05 |
| Lie return | nie RA | tak | 03 |
| Decline → CANCELLED | nie | nie | 03, 05 |
| Inspector deliveries/anomalies | nie | cienko | 03 |
| Clock / reset / reconcile | tak | nie | 05 |
| `OOO_EVENTS` | nie | nie | blocked |
| CORS OPTIONS | tak | — | 05 |
| F-D2 `/psp-redirect-simulator` | — | osobny spec | **poza CPL** |

---

## Poza zakresem tej mapy

- Kafka, real PayU/Stripe, PAN/3DS/PCI, settlement, KYC.
- Zmiana realm Keycloak; uczenie „JWT dashboard = token PSP”.
- `payment_orders`, `MockPspClient`, `idempotency_records` payment.
- F-D2 (`tests/e2e/psp-redirect-simulator.spec.ts`) — ten sam wzorzec multi-tab, **inne** wiązanie; nie mylić `psp-approve` na dwóch stronach.
- Implementacja kodu testów ani poprawek `OOO_EVENTS`.

---

## Problem+json — słownik błędów CPL

`Content-Type: application/problem+json`.  
`type` = `https://api.payment-quality.local/problems/{error-with-dashes}`.  
`code` = `error` upper-case. Pole `instance` = `requestURI`.

| `error` | HTTP | `code` | Kiedy |
|---|---|---|---|
| `not_found` | 404 | `NOT_FOUND` | Nieznany `sessionId` (GET session/events/deliveries; hosted GET) |
| `validation` | 400 | `VALIDATION` | Bean validation, amount, currency, unknown scenario/outcome |
| `invalid_signature` | 400 | `INVALID_SIGNATURE` | HMAC notify — **nie retry** |
| `missing_simulate_token` | 403 | `MISSING_SIMULATE_TOKEN` | Brak `Lab-Simulate-Token` (nie 401) |
| `invalid_simulate_token` | 403 | `INVALID_SIMULATE_TOKEN` | Zły/sfałszowany token |
| `transient_error` | 503 | `TRANSIENT_ERROR` | `NOTIFY_5XX_RETRY` pierwsze trafienie — **retry** |
| `idempotency_conflict` | 409 | `IDEMPOTENCY_CONFLICT` | Ten sam `Idempotency-Key`, inny fingerprint |
| `expired_link` | 409 | `EXPIRED_LINK` | Simulate po `validityUntil` / scenariusz `EXPIRED_LINK` |

**Nie** problem+json: lab Bearer **401** (puste body); OAuth **401** (puste); fulfillment missing **404** puste; duplicate notify **200** JSON `{duplicate:true}`.
