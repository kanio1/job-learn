---
name: cpl-context-requirements
parent: checkout-protocol-lab
last_updated: 2026-08-09
---

# 00 — Kontekst, wymagania, non-goals

## 1. Problem biznesowo-edukacyjny

Lab doskonale uczy **merchant/payment REST + RBAC + Problem Details**.  
Nie uczy łańcucha Bookero/PayU:

> create → **302** → hosted page → **continueUrl (kłamstwo)** → **signed notify** → **inbox/ACK** → **fulfill**

Obecne F-D2 (`psp-redirect-simulator`) to multi-tab Approve/Decline **bez** order binding, notify, signature i kontraktu 302.

## 2. Cel produktu (CPL)

Flagowany, lokalny **Checkout Protocol Lab**: edukacyjny symulator protokołu płatności redirect+notify dla SDET/integracji.

| Decyzja | Wartość |
|---|---|
| Transport async | PostgreSQL inbox + `@Scheduled` / poller |
| Broker | **Brak Kafki** |
| IdP | Keycloak **bez zmian realm** w MVP |
| PSP | Stub labowy, nie PayU/Stripe |
| Isolation | Modulith module `checkoutlab` |

## 3. Constraints (MUST)

| ID | Wymaganie | Połączenie |
|---|---|---|
| C-01 | Brak Kafki, outbox produkcyjnego, realnego PSP, PAN/3DS | `AGENTS.md` Active Non-Goals |
| C-02 | Modulith: public root + `internal.*` | jak `payment`, `merchant` |
| C-03 | `app.checkout-lab.enabled`; off → 404 | wzorzec `app.testing.enabled` |
| C-04 | Nie zmieniać `MockPspClient` / lifecycle payment | `payment.internal.infrastructure` |
| C-05 | Kwoty: `amountMinor` (long) + ISO currency | jak `payment_orders` |
| C-06 | Fulfillment tylko po zweryfikowanym evencie | E3 + E4-S2 |
| C-07 | Testy: RA + Modulith + PW; oracle = DB/headers | E7 |

## 4. Functional requirements

| ID | Opis | Epic |
|---|---|---|
| FR-01 | OAuth stub `client_credentials` form-urlencoded → Bearer | E1 |
| FR-02 | Create session → **302** + `Location` + JSON `redirectUri` | E1 |
| FR-03 | Hosted checkout związany z session | E4 |
| FR-04 | `continueUrl` GET z niewiarygodnym hintem | E4 |
| FR-05 | Stub POST event z `Lab-Signature`, `Lab-Event-Id` | E2 |
| FR-06 | Receiver: raw HMAC + timestamp → inbox → **202** | E2 |
| FR-07 | Worker: re-fetch status przed fulfill | E3 |
| FR-08 | Dedup `eventId`; duplicate → 200, zero side-effect | E2 |
| FR-09 | **400** bad sig (no retry); **503** transient (retry) | E2 |
| FR-10 | Payment link + `validityUntil` | E4 |
| FR-11 | Business `Idempotency-Key` + lease | E1 |
| FR-12 | Booking Lab UI cash vs online | E5 |
| FR-13 | Event Inspector | E6 |
| FR-14 | Scenario engine | E6 |
| FR-15 | Lekki reconcile + anomaly | E6 |

## 5. Non-functional

| ID | Opis |
|---|---|
| NFR-01 | ACK receiver p95 &lt; 200 ms (tylko enqueue) |
| NFR-02 | `X-Correlation-ID` end-to-end |
| NFR-03 | Structured log: sessionId, eventId, correlationId |
| NFR-04 | Constant-time signature compare |
| NFR-05 | `ModulithArchitectureTest` zielony |

## 6. Non-goals (nie stories CPL)

- Maker-checker, bulk select, idle session, locale, clipboard, CSV
- Real webhook platform / Kafka / settlement / KYC
- Zmiana produkcyjnego payment lifecycle API
- Uczenie „Keycloak token = PSP token”

## 7. Mapowanie na istniejący kod

| Istniejące | Rola względem CPL |
|---|---|
| `SecurityConfig` Order(1) `/api/status`, Order(2) `/api/test/*` | Wzorzec osobnego chaina dla lab OAuth / notify |
| `TestController` + `@ConditionalOnProperty` | Wzorzec flagi |
| `psp-redirect-simulator.vue` + E2E | Bazowy UI → E4 bind |
| `error-lab.vue` | Hub linków learning |
| `tenant.webhook_base_url` | Tylko settings field — **nie** IPN |
| `event_publication` (Flyway V6) | Modulith — **nie** reuse jako IPN inbox |
| `idempotency_records` | Payment only — CPL ma własne tabele |
| `infra/keycloak/.../payment-quality-realm.json` | **Bez zmian MVP** |
| `status/index.md` → `REST-REDIRECT-01` | Gate zamykany przez E7-S4 |

## 8. Definition of Done (program)

- Flaga off → brak powierzchni lab.
- Żaden test nie używa `continueUrl` jako jedynego oracle pieniędzy.
- Brak Kafki / real PSP / PAN.
- Modulith + filtered backend verify + chromium green dla nowych speców.
- Status registry: `REST-REDIRECT-01` zamknięty / superseded przez CPL.
