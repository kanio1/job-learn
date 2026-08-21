# Mapa testów — Merchant 360 (milestone-PW)

Dokumentacja **task-force** (BA + test architect). **Nie jest implementacją.**  
Backlog wykonawczy: [status/roadmaps/playwright-merchant-360](../../status/roadmaps/playwright-merchant-360/).

Analog: [live-pom-wave-2](../live-pom-wave-2/), [playwright-real-stack-learning](../../status/roadmaps/playwright-real-stack-learning/).

### Świat wykonania

| Świat | Auth | Oracle | Zakaz |
|---|---|---|---|
| Live POM | Keycloak `storageState` | UI + `waitForResponse` + `BffClient` | `page.route` / fulfill |
| Playwright REST | cookie BFF | status, Problem Details, Zod | Bearer w teście UI |
| REST Assured | JWT TestJwt | Spring kontrakt + DB | powielanie 1:1 w PW |
| Stos | `scripts/dev-stack.sh --app` | `/api/status`, issuer `:8081` | mock Keycloak |

Pokrycie wierszy: `designed` (ten katalog). Po implementacji → `existing-pom` / `existing-ra`.

## Indeks

| Plik | Prefiks |
|---|---|
| [00-business-flows.md](00-business-flows.md) | `BF-M360-##` |
| [01-business-cases.md](01-business-cases.md) | `BC-M360-##` |
| [02-use-cases.md](02-use-cases.md) | `UC-M360-##` |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | `PW-M360-E2E-###`, `PW-M360-SEC-###` |
| [04-playwright-api-http.md](04-playwright-api-http.md) | `PW-M360-API-###`, `RA-M360-###` |
| [05-traceability.md](05-traceability.md) | FR → TC / AT |
| [06-istqb-ep-bva.md](06-istqb-ep-bva.md) | `EP-M360-*`, `BVA-M360-*` |
| [07-dt-st-uc-mr.md](07-dt-st-uc-mr.md) | `DT-*`, `ST-*`, `MR-*`, `EG-*` |
| [08-acceptance-tests.md](08-acceptance-tests.md) | `AT-M360-*` |
| [09-agent-tests-pom-plan.md](09-agent-tests-pom-plan.md) | specs / `methods/` / E2E vs REST |
| [m360-ops-wave-2-value-and-learning.md](../m360-ops-wave-2-value-and-learning.md) | para z Wave 2: wartość, uzupełnienie, lekcja E2E/REST |

## Persony

Jak Wave 2: `platform.admin`, `tenant.admin`, `merchant.manager` / `w{n}`, `support.agent`, `readonly.user`, `merchant.denied`. Seed UUID: Wave 2 [09](../live-pom-wave-2/09-core-domain-flows.md).

## Tagi

`@erp` tabela/360, `@security` RBAC/412, `@a11y` ARIA snapshot, `@import`, `@kanban`.
