---
name: checkout-protocol-lab
origin: POST_KIRO_WORK
status: IMPLEMENTED
branch: checkout-protocol-lab-foundation
related_gate: REST-REDIRECT-01 (Wave 2B — closed by CPL as approved test-only redirect / notify lab)
last_updated: 2026-08-13
---

# Checkout Protocol Lab (CPL v2) — backlog wykonawczy

To jest **nowe miejsce** na wymagania, epiki, stories, taski i mapę nauki dla edukacyjnego symulatora protokołu płatności (redirect + signed notify + inbox), opartego o **Spring Modulith + PostgreSQL** (bez Kafki, bez realnego PSP).

## Jak czytać ten katalog

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, granice, kolejność MVP |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR/NFR/constraints, non-goals, powiązania z labem |
| [01-infra-postgres-keycloak-security.md](./01-infra-postgres-keycloak-security.md) | Adaptacje PG / KC / Spring Security |
| [learning-map.md](./learning-map.md) | Co poznajemy: HTTP · REST · SQL · Keycloak · Playwright |
| [task-board.md](./task-board.md) | Uporządkowana kolejka tasków (ID → epic/story) |
| [prompts/wave-a-session-prompt.md](./prompts/wave-a-session-prompt.md) | Cache-friendly prompt pack (T01–T07) dla Cursor / Grok / Composer |
| [prompts/wave-b-session-prompt.md](./prompts/wave-b-session-prompt.md) | Cache-friendly prompt pack (T08–T13) E2+E3 |
| [epics/E0-foundation.md](./epics/E0-foundation.md) | Moduł, flaga, Flyway |
| [epics/E1-protocol-core.md](./epics/E1-protocol-core.md) | OAuth stub + 302 OrderCreate + retrieve |
| [epics/E2-event-ingestion.md](./epics/E2-event-ingestion.md) | Signature, inbox, 400/503, dedup |
| [epics/E3-async-fulfillment.md](./epics/E3-async-fulfillment.md) | Worker, refetch, fulfillment SM |
| [epics/E4-hosted-checkout-return.md](./epics/E4-hosted-checkout-return.md) | Hosted UI, continueUrl lie, expiry |
| [epics/E5-booking-experience.md](./epics/E5-booking-experience.md) | Bookero-like cash/online |
| [epics/E6-observability-scenarios.md](./epics/E6-observability-scenarios.md) | Inspector, scenarios, reconcile |
| [epics/E7-assurance-learning.md](./epics/E7-assurance-learning.md) | RA, Playwright, REST-REDIRECT gate |

## Status

`IMPLEMENTED` on `checkout-protocol-lab-foundation`. Wave A (T01–T07) plus T07b auth split, E2–E7 protocol, Booking/Inspector UI, and REST-REDIRECT-01 closure.

## Granice (MUST)

- Nowy modulith module: `lab.paymentquality.checkoutlab` (+ `internal.*`).
- Flaga: `app.checkout-lab.enabled` tylko `dev`/`test`; `false` → 404.
- **Bez** Kafki, realnego PayU/Stripe, PAN/3DS, settlement.
- **Bez** zmian realm Keycloak w MVP.
- **Bez** alter `payment_orders` / `idempotency_records` / `event_publication` pod IPN.
- Fulfillment tylko po zweryfikowanym evencie — **continueUrl nie jest oracle**.

## Powiązania z istniejącym labem

```text
F-D2  apps/frontend/app/pages/psp-redirect-simulator.vue
      → E4 upgrade: bind sessionId, emit notify (nie tylko Approve/Decline UI)

Error Lab  /error-lab
      → punkt wejścia learning / link do Booking Lab (E5)

MockPspClient  payment.internal
      → POZOSTAJE sync always-success; CPL go NIE zastępuje

app.testing.enabled  testing module
      → WZORZEC flagi dla app.checkout-lab.enabled

tenant.webhook_base_url
      → NIE jest receiverem IPN; CPL ma własny notify endpoint

REST-REDIRECT-01 (status/index.md Wave 2B)
      → zamykane przez CPL jako approved test-only redirect server (E7-S4)

SecurityConfig  shared.security
      → nowe matchery: lab OAuth, notify HMAC (nie JWT Keycloak)
```

## Kolejność MVP (executable)

```text
E0-S1 → E0-S2 → E1-S1 → E1-S2 → E1-S4
     → E2-S1 → E2-S2 → E2-S3
     → E3-S1 → E3-S2 → E3-S3
     → E4-S1 → E4-S2 → E4-S4
     → E7-S1 → E7-S2 → E7-S4
```

Potem: E1-S3, E2-S4, E4-S3, E5, E6.

## Diagram przepływu (docelowy)

```text
[Merchant/Booking UI]
        |  POST /api/checkout-lab/sessions  (+ lab Bearer)
        v
[Checkout Lab API] --302 Location--> [Hosted checkout /psp/checkout/{id}]
        |                                      |
        |                              Approve / Decline
        |                                      |
        |                              stub emits POST notify
        v                                      v
[continueUrl — UNTRUSTED hint]     [Receiver: HMAC → inbox → 202]
                                             |
                                             v
                                    [Worker: refetch session → fulfill]
```

## Legenda learning tags

W każdym tasku używamy tagów:

- `HTTP:` statusy, nagłówki, redirect, raw body
- `REST:` zasoby, idempotency, Problem Details
- `SQL:` migracje, UNIQUE, SKIP LOCKED, JSONB
- `KC:` Keycloak / JWT — zwykle „bez zmian” albo kontrast z lab tokenem
- `PW:` Playwright / browser hops
- `MOD:` Spring Modulith boundaries
