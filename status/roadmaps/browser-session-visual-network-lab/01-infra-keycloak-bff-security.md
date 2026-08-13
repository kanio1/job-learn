---
name: mrl-infra-keycloak-bff
parent: browser-session-visual-network-lab
last_updated: 2026-08-13
---

# 01 — Infra: Keycloak, BFF cookies, CSRF, idle

Decyzje na czas **designu**. Implementacja nie rusza realm w wave 1.

## INFRA-KC-00 — Realm freeze (E1)

| Decyzja | Wartość |
|---|---|
| `payment-quality-realm.json` | **Bez zmian** w E0–E3 |
| Login dashboard | Istniejący OIDC PKCE (`nuxt-auth-utils`) |
| Logout | BFF clear cookie + opcjonalny Keycloak `end_session` URL już wspierany przez IdP; E1-S3 dokumentuje hop |
| ACR / OTP | **E5 only**; wymaga osobnej decyzji realm (INFRA-KC-01) |

### INFRA-KC-01 — Step-up (E5, gated)

Jeśli E5-S1: dodać labowego usera lub ACR mapping **tylko** po akceptacji. Alternatywa bez realm: BFF „step-up token” w sealed session (`stepUpUntil`) po potwierdzeniu kwoty w UI. Preferowana na start: **BFF step-up**, żeby nie łamać C-07.

## INFRA-BFF-01 — Cookie `nuxt-session`

Obserwowalne atrybuty (inspector + Playwright `context.cookies()`):

| Atrybut | Oczekiwanie lab |
|---|---|
| Name | `nuxt-session` |
| HttpOnly | **true** |
| Secure | false na `http://localhost`; true za HTTPS |
| SameSite | `Lax` (typowe OIDC redirect) — udokumentować, nie zgadywać |
| Path | `/` |
| JWT w cookie value | sealed; testy **nie** dekodują payload do asercji ról (używają `/api/_auth/session`) |

Keycloak cookies (`AUTH_SESSION_ID`, `KC_RESTART`, `KEYCLOAK_IDENTITY` …) żyją na hoście IdP (`localhost:8180` / compose port), **nie** na `:3000`. Inspector musi to pokazać — to lekcja Domain.

Hosted `/psp/checkout/*`: **zero** `nuxt-session` wymaganego; asercja „No Keycloak cookie” zostaje (CPL).

## INFRA-BFF-02 — Idle lock (E1, bez realm)

| Decyzja | Wartość |
|---|---|
| Mechanizm | Client + BFF: last-activity timestamp w sealed session **lub** lab cookie `mrl-idle` HttpOnly |
| Domyślny TTL | 120 s w `dev` (krótki, żeby ćwiczyć); override env `MRL_IDLE_SECONDS` |
| Lock UI | Overlay na `/admin/**`; hosted public **nie** idle-lockuje (inny świat) |
| Test | `page.clock.install` + `fastForward`; zakaz `waitForTimeout` |
| Re-auth | Redirect `/login` albo in-app „Unlock” → OIDC |

## INFRA-BFF-03 — CSRF (E1-S5)

Dashboard BFF jest cookie-auth. Lab endpoint:

```text
POST /api/session-lab/csrf-demo
```

- Brak headera `X-CSRF-Token` (lub mismatch) → **403** `problem+json` `csrf_failed`
- Token z GET `/api/session-lab/csrf` (double-submit albo synchronizer w session)
- Kontrast: `POST /api/merchants/{id}/payment-orders/...` z Bearer (RA) **nie** wymaga CSRF

Nie wprowadzać CSRF na całe istniejące BFF w wave 1 — tylko **lab demo path**, żeby nie rozwalić dashboardu.

## INFRA-BFF-04 — Concurrent sessions (E1-S4)

Lab tabela `session_lab_devices` (opcjonalnie) **albo** in-memory keyed by sealed session id:

- `GET /api/session-lab/devices` — lista
- `POST /api/session-lab/devices/{id}/revoke`

Dwa Playwright `browser.newContext({ storageState })` z tym samym userem. Wave 1 może użyć **osobnego** lab session id w BFF, nie Keycloak session list (Admin API).

## INFRA-SEC-01 — Public vs credentialed CORS

E3-S4:

- Hosted GET: CORS bez credentials
- Dashboard BFF: `Access-Control-Allow-Credentials: true` **tylko** origin `http://localhost:3000`
- Lab notify HMAC: bez cookies, bez CORS credentials (maszyna)

## INFRA-PG-01 — Kiedy Flyway

| Epic | DB |
|---|---|
| E1 idle/CSRF/inspector | może zero tabel (session w cookie) |
| E1 concurrent | opcjonalnie `session_lab_devices` |
| E2 visual | zero |
| E3 network | zero (Error Lab / CPL) |
| E4 | checkoutlab alter **tylko** refund notify / lang — nie `payment_orders` |
| E5 disputes/statements | nowe tabele lab `mrl_*` albo reuse evidence |

## INFRA-PW-01 — Suites

| Suite | Config | Route fulfill | Auth |
|---|---|---|---|
| Mocked | `playwright.config.ts` | TAK | `page.route` session |
| Live POM | `playwright.pom.config.ts` | NIE | Keycloak storageState |
| Learner | `playwright.pom-learner.config.ts` | NIE | puste specy |

Visual mocked może iść w CI; visual live tylko gdy image = baseline OS.

## Secrets

- Hasła Keycloak: wyłącznie env (`PLAYWRIGHT_*_PASSWORD`)
- `.auth/*.json` gitignore
- CSRF secret / idle HMAC: env w `.env.example` placeholdery
