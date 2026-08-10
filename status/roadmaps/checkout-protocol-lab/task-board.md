---
name: cpl-task-board
parent: checkout-protocol-lab
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-09
---

# Task board — kolejka wykonawcza CPL

Każdy task ma ID `CPL-Txx`. Statusy: `TODO` | `IN_PROGRESS` | `DONE` | `BLOCKED`.

**Zasada:** jeden work package naraz; nie startować bez zlecenia użytkownika.

## MVP path (P0)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 1 | CPL-T01 | E0-S1 | Szkielet modułu Modulith + flaga | DONE |
| 2 | CPL-T02 | E0-S2 / INFRA-PG-01 | Flyway V12 + locations | DONE |
| 3 | CPL-T03 | INFRA-CFG-01 | Config secrets + application-dev/test | DONE |
| 4 | CPL-T04 | INFRA-SEC-01 | Security chains: oauth + notify permit | DONE |
| 5 | CPL-T05 | E1-S1 | OAuth token stub | DONE |
| 6 | CPL-T06 | E1-S2 | Create session 302 + Location | DONE |
| 7 | CPL-T07 | E1-S4 | GET session | DONE |
| 8 | CPL-T08 | E2-S1 | Stub notifier (emit event) | TODO |
| 9 | CPL-T09 | E2-S2 | Receiver HMAC → inbox → 202 | TODO |
| 10 | CPL-T10 | E2-S3 | Dedup event_id | TODO |
| 11 | CPL-T11 | E3-S1 | Inbox worker poller | TODO |
| 12 | CPL-T12 | E3-S2 | Refetch before fulfill | TODO |
| 13 | CPL-T13 | E3-S3 | Fulfillment state machine | TODO |
| 14 | CPL-T14 | E4-S1 | Bind F-D2 do sessionId | TODO |
| 15 | CPL-T15 | E4-S2 | Untrusted continueUrl page | TODO |
| 16 | CPL-T16 | E4-S4 | Close-tab / pay_no_return | TODO |
| 17 | CPL-T17 | E7-S1 | REST Assured / api-tests suite | TODO |
| 18 | CPL-T18 | E7-S2 | Playwright journey pack | TODO |
| 19 | CPL-T19 | E7-S4 | Zamknięcie REST-REDIRECT-01 w status | TODO |

## Post-MVP (P1/P2)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 20 | CPL-T20 | E1-S3 | Idempotency-Key + lease | TODO |
| 21 | CPL-T21 | E2-S4 | Stub retry tylko na 5xx | TODO |
| 22 | CPL-T22 | E4-S3 | Link expiry + Clock | TODO |
| 23 | CPL-T23 | INFRA-SEC-02 | CORS Lab-* headers | TODO |
| 24 | CPL-T24 | INFRA-PG-03 | Reset checkout_* w test harness | TODO |
| 25 | CPL-T25 | E5-S1 | Booking Lab page | TODO |
| 26 | CPL-T26 | E5-S2 | Nav entry Error Lab | TODO |
| 27 | CPL-T27 | E6-S1 | Event Inspector | TODO |
| 28 | CPL-T28 | E6-S2 | Scenario engine | TODO |
| 29 | CPL-T29 | E6-S3 | Reconcile + anomaly | TODO |
| 30 | CPL-T30 | E7-S3 | Learning evidence note w docs/ | TODO |

## Zależności (graf)

```text
T01 ─┬─► T02 ─► T03
     └─► T04 ─┬─► T05 ─► T06 ─► T07
              └─► T08 ─► T09 ─► T10
                              └─► T11 ─► T12 ─► T13
                                              └─► T14 ─► T15 ─► T16
                                                              └─► T17 / T18 ─► T19
```

## Definition of Done — pojedynczy task

1. Kod + testy wskazane w story.
2. Learning note w PR/commit message: co w HTTP/REST/SQL/KC.
3. Flaga off nie psuje istniejącego labu.
4. Nie ruszać `.kiro/**` checkboxów; status CPL aktualizować tutaj / w evidence gdy użytkownik zleci.

Szczegóły każdego taska: pliki w `epics/`.
