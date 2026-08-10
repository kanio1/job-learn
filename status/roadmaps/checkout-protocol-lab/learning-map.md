---
name: cpl-learning-map
parent: checkout-protocol-lab
last_updated: 2026-08-09
---

# Mapa nauki — HTTP · REST · SQL · Keycloak · Playwright

Każdy epic/story w CPL ma **cel produktowy** i **cel dydaktyczny**. Ta mapa mówi *czego się uczymy*, nie tylko *co budujemy*.

## HTTP

| Temat | Gdzie w CPL | Co obserwujesz |
|---|---|---|
| `302 Found` + `Location` | E1-S2 create session | Browser/client **nie** musi follow; RA: `redirects().follow(false)` |
| `202 Accepted` | E2-S2 notify ACK | „Przyjąłem, przetworzę później” ≠ fulfillment done |
| `200` duplicate | E2-S3 | At-least-once: drugi delivery bez side-effect |
| `400` vs `503` | E2-S2 / E2-S4 | **400** = nie retry (zły podpis); **503** = retry |
| `401` | E1-S1 złe OAuth | Credentials / Content-Type |
| Raw body | E2 signature | Podpisujesz **bajty**, nie „ładny” JSON po re-serialize |
| Custom headers | Lab-Signature, Lab-Event-Id | Jak `OpenPayu-Signature` w prawdziwym świecie |
| Correlation | `X-Correlation-ID` | Ten sam ID: create → notify → worker log |
| `Content-Type: application/x-www-form-urlencoded` | OAuth stub | Inny świat niż `application/json` |
| Untrusted return URL | E4-S2 | GET continueUrl **nie** zmienia pieniędzy |

## REST

| Temat | Gdzie | Lekcja |
|---|---|---|
| Zasób `session` | E1 | Create + retrieve; status maszyny stanów |
| Idempotency-Key | E1-S3 | Ten sam klucz + ten sam fingerprint = replay; inny body = 409 |
| Problem Details | błędy lab | Spójność z resztą labu (`application/problem+json`) |
| Separacja komend | create vs notify vs fulfill | Notify to **callback**, nie „user REST” |
| Oracle testowy | E7 | Assertuj headers + DB, nie tylko UI thank-you |

## SQL / PostgreSQL

| Temat | Gdzie | Lekcja |
|---|---|---|
| Flyway per module | E0 | `locations` muszą znać nowy folder |
| CHECK constraints | E0-S2 | Statusy i currency jak w `payment_orders` |
| UNIQUE event_id | E2 | Dedup w DB, nie tylko w pamięci |
| JSONB payload | E2 | Raw envelope do Inspectora / debug |
| Partial unique index | E1 idem | Jak payment V5 — inny scope |
| `SKIP LOCKED` | E3 worker | Konkurencyjne workery bez Kafki |
| Osobny inbox | vs `event_publication` | Modulith ≠ merchant webhook |
| Brak FK do payment | E0 | Granica domeny edukacyjnej |

## Keycloak / tożsamość

| Temat | Gdzie | Lekcja |
|---|---|---|
| JWT dashboard | Inspector / Booking (opcjonalnie) | Istniejący `payment-quality-dashboard` |
| **Brak** JWT na notify | E2 | PSP nie loguje się do Twojego IdP |
| Lab Bearer ≠ KC JWT | E1 | Dwa światy tokenów — celowe |
| Realm bez zmian MVP | INFRA-KC-00 | Nie doklejaj OIDC do PayU OAuth |
| azp validator | istniejący lab | Dlatego stub nie używa KC tokenów |
| Public hosted page | E4 | Jak prawdziwy hosted checkout (SAQ-A mindset) |

## Playwright / browser

| Temat | Gdzie | Lekcja |
|---|---|---|
| Multi-tab / popup | E4 (F-D2 upgrade) | Już jest — dodać binding session |
| `waitForResponse` / request | E7-S2 | Assert notify **albo** fulfillment API, nie tylko DOM |
| Clock / expiry | E4-S3 | Link expired bez flaky sleep |
| Lie return | E4-S2 | UI success + brak event → nie CONFIRMED |
| Close tab | E4-S4 | Notify bez return URL visit |
| Locators | E7 | `getByRole` / `data-testid`; zero `waitForTimeout` |

## Modulith / architektura

| Temat | Gdzie | Lekcja |
|---|---|---|
| Nowy module package | E0 | `checkoutlab` public + `internal` |
| Nie zależeć od `payment.internal` | C-04 | Public API only jeśli kiedyś link |
| Feature flag | E0 | Jak `testing` module |
| Scheduled worker in-process | E3 | Async bez brokera |

## Macierz: story → learning tags

| Story | HTTP | REST | SQL | KC | PW |
|---|---|---|---|---|---|
| E0-S1 | | | | kontrast flag | |
| E0-S2 | | | ✔ | | |
| E1-S1 | ✔ form | ✔ | | kontrast token | |
| E1-S2 | ✔ 302 | ✔ | ✔ | | |
| E1-S3 | | ✔ idem | ✔ | | |
| E1-S4 | ✔ GET | ✔ | | | |
| E2-S1 | ✔ POST notify | | | | |
| E2-S2 | ✔ 400/503/202 | | ✔ inbox | **bez JWT** | |
| E2-S3 | ✔ 200 dup | | ✔ UNIQUE | | |
| E2-S4 | ✔ retry 5xx | | | | |
| E3-S1 | | | ✔ SKIP LOCKED | | |
| E3-S2 | ✔ refetch GET | ✔ | | | |
| E3-S3 | | SM | ✔ | | |
| E4-S1..S4 | return URL | | | public page | ✔ |
| E5 | | | | JWT UI opc. | ✔ |
| E6 | headers dump | | JSONB | | ✔ |
| E7 | pełny kontrakt | ✔ | oracle DB | live KC tylko UI | ✔ |
