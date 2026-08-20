# Mapa testów — Ops Interaction Lab (milestone-PW Wave 2)

Dokumentacja **task-force** (BA + test architect + frontend/Nuxt UI + backend/PG/Keycloak). **Nie jest implementacją.**  
Backlog wykonawczy: [status/roadmaps/playwright-ops-wave-2](../../status/roadmaps/playwright-ops-wave-2/).

Poprzedni milestone-PW: [playwright-merchant-360](../../status/roadmaps/playwright-merchant-360/) / [merchant-360-erp-lab](../merchant-360-erp-lab/).  
Analog live POM: [live-pom-wave-2](../live-pom-wave-2/), [playwright-real-stack-learning](../../status/roadmaps/playwright-real-stack-learning/).

### Świat wykonania

| Świat | Auth | Oracle | Zakaz |
|---|---|---|---|
| Live POM | Keycloak `storageState` | UI + `waitForResponse` + `BffClient` + `page.on('websocket')` | `page.route` / fulfill / `routeWebSocket` |
| Playwright REST | cookie BFF | status, Problem Details, Zod | Bearer w teście UI; token w localStorage |
| REST Assured | JWT TestJwt | Spring kontrakt + DB | powielanie 1:1 w PW |
| Stos | `scripts/dev-stack.sh --app` | `/api/status`, issuer `:8081` | mock Keycloak, mock WS |

Pokrycie wierszy: `designed` (ten katalog). Po implementacji → `existing-pom` / `existing-ra`.

## Indeks

| Plik | Prefiks |
|---|---|
| [01-business-cases.md](01-business-cases.md) | `BC-OPS-##` |
| [02-use-cases.md](02-use-cases.md) | `UC-OPS-##` |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | `PW-OPS-E2E-###`, `PW-OPS-SEC-###` |
| [04-playwright-api-http.md](04-playwright-api-http.md) | `PW-OPS-API-###`, `RA-OPS-###` |
| [05-traceability.md](05-traceability.md) | FR → TC |

## Persony

`platform.admin`, **`platform.operator`** (drugi writer), `tenant.admin`, `merchant.manager` / `w{n}`, `support.agent`, opcjonalnie `support.agent.b`, `readonly.user`, `merchant.denied`.

## Tagi (projektowane)

`@ops` feed/notif, `@kanban` support board, `@security` 412/RBAC, `@pin` step-up, `@i18n` locale project, `@a11y` ARIA snapshot.

## Świadomie poza katalogiem

Pagination, async CSV export, offline banner, checkout iframe, payment-status Kanban (M360 E5), kolejny Slideover lab, `routeWebSocket` chaos.
