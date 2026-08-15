---
name: browser-session-visual-network-lab
origin: POST_KIRO_WORK
status: IMPLEMENTED
related_gate: REST-MULTIPART-01 DONE_VERIFIED (Wave 4 payment-evidence api-tests; disputes RA is not that gate)
last_updated: 2026-08-14
---

# PayU / bank mirror labs — backlog wykonawczy

To jest **nowe miejsce** na wymagania, epiki, stories, taski i mapę nauki dla lustrzanych labów edukacyjnych:

- sesja przeglądarki (cookies, `storageState`, idle, CSRF, public vs auth)
- visual comparison (`toHaveScreenshot`)
- network interception (`page.route`, HAR, lie body)
- lustra protokołu PayU na CPL
- bank-like enterprise (step-up, statements, disputes, maker-checker)

**Nie jest to implementacja.** Kod aplikacji i testy zmienia się dopiero po osobnym poleceniu.

## Jak czytać ten katalog

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, granice, kolejność |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR/NFR/constraints, non-goals |
| [01-infra-keycloak-bff-security.md](./01-infra-keycloak-bff-security.md) | BFF cookies, Keycloak, CSRF, idle |
| [learning-map.md](./learning-map.md) | HTTP · REST · cookies · OIDC · Playwright |
| [task-board.md](./task-board.md) | Kolejka `MRL-T01`… |
| [epics/E0-foundation.md](./epics/E0-foundation.md) | Flagowane powierzchnie, hub, bez mobile |
| [epics/E1-session-lab.md](./epics/E1-session-lab.md) | Cookie inspector, idle, logout, guest, CSRF |
| [epics/E2-visual-lab.md](./epics/E2-visual-lab.md) | Stabilne kafelki, mask, light/dark |
| [epics/E3-network-lab.md](./epics/E3-network-lab.md) | 503-retry, abort, HAR, CORS credentials |
| [epics/E4-payu-mirrors.md](./epics/E4-payu-mirrors.md) | GET-no-body, refund notify, lang, iframe |
| [epics/E5-bank-like.md](./epics/E5-bank-like.md) | Step-up, statements, disputes, maker-checker |
| [epics/E6-assurance-learning.md](./epics/E6-assurance-learning.md) | RA + PW mocked vs POM |

Mapa przypadków testowych (nie kod): [docs/testing/payu-bank-mirror-labs/](../../../docs/testing/payu-bank-mirror-labs/).

## Status

`IMPLEMENTED`. Hub, Session/Visual/Network labs (Nuxt + Nitro), CPL PayU mirrors, and `lab.paymentquality.mirrorlab` bank-like APIs. Visual goldens are mocked Chromium on this Linux machine (`maxDiffPixelRatio: 0.02`); CI may flake across font/OS. `REST-MULTIPART-01` stays open — disputes cover 413/415 in Spring RA, not the standalone `apps/api-tests` payment-evidence construction AC.

## Granice (MUST)

- Desktop only — **brak** `devices['iPhone']`, hamburger-as-primary, mobile projects.
- **Bez** Kafki, realnego PayU/Stripe, PAN/3DS/PCI, KYC, settlement produkcyjnego.
- **Bez** uczenia „JWT dashboard = token PSP / PayU”.
- Trzy światy tożsamości zostają rozdzielone (dashboard OIDC ≠ hosted public ≠ lab OAuth/HMAC).
- Wave 1 Session Lab: **brak zmian realm Keycloak**; idle lock w BFF/Nuxt.
- Iframe tylko **same-origin** lab widget; hosted new-tab CPL zostaje.
- Visual baselines tylko ze środowiska CI (oficjalny image Playwright), nie z laptopów.
- Mocked `tests/e2e` vs live `tests-pom`: POM **zakaz** `route.fulfill`.

## Trzy światy przeglądarki

```text
Świat A  Dashboard  Keycloak OIDC PKCE → sealed cookie nuxt-session (HttpOnly)
Świat B  Płatnik    /psp/checkout/{id}  — zero cookie Keycloak
Świat C  Maszyna    client_credentials + Lab Bearer / HMAC notify
```

## Kolejność MVP (gdy implementować)

```text
E0-S1 → E1-S1 → E1-S2 → E1-S3 → E1-S6
     → E2-S1 → E2-S2
     → E3-S1 → E3-S2
     → E6-S1 → E6-S2
```

Potem: E1-S4/S5 (concurrent, CSRF), E4 (CPL mirrors), E5 (bank-like), E2-S3 dark/mask, E3-S3 HAR.

## Powiązania

```text
CPL          status/roadmaps/checkout-protocol-lab/     — 302 + notify; E4 rozszerza, nie zastępuje
Error Lab    /error-lab                                 — E3 dokłada fault buttons
F-D5         tests/e2e/visual-regression.spec.ts        — badge only; E2 to osobny Visual Lab
tests-pom    storage-safety.ts, playwright.pom.config   — guest project, HttpOnly
REST-MULTIPART-01  status/index.md                      — kandydat na zamknięcie przez E5 disputes
```

## Legenda learning tags

- `HTTP:` statusy, Set-Cookie, CSRF, CORS, GET-without-body
- `REST:` Problem Details, multipart, Content-Disposition
- `CK:` cookies, SameSite, HttpOnly, storageState, sessionStorage
- `KC:` Keycloak / OIDC PKCE / logout / ACR (późne fale)
- `PW:` Playwright browser / visual / route / clock
- `MOD:` Modulith + feature flags
