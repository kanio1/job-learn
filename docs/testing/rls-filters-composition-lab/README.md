# Mapa testów — RLS Lab, filtry, composition (Wave A)

Dokumentacja **task-force** (PM + test architect + test analyst).  
Cel: kompletna mapa przypadków dla **zaimplementowanego** pakietu Wave A (2026-08-13): edukacyjny RLS Lab, testowalność listy płatności, Playwright live POM oraz REST Assured na filtrach i JDBC. Wave B (stos HTTP + TLS overlay + live POM) jest w **09**. **Nie jest to implementacja** HTTP QUERY, Spring API versioning ani `@Retryable`.

Źródła: `apps/backend` modulith `rlslab` (Flyway V17 + V18 BYPASSRLS), `apps/frontend` `/admin/rls-lab` + `data-testid` na filtrach, katalog ten.  
Analog: [docs/testing/payu-bank-mirror-labs/](../payu-bank-mirror-labs/), [docs/testing/checkout-protocol-lab/](../checkout-protocol-lab/).

## Jak czytać

1. Co jest w produkcie i jakie oracles → **01**.
2. Warstwy, ryzyko, split mock/POM → **02**.
3. UI E2E → **03**.
4. REST (Spring + BFF) → **04**.
5. Hard controls, Keycloak, BFF composition → **05**.
6. EP/BVA → **06**.
7. DT / ST / UC / pairwise / error guessing → **07**.
8. FR → TC → **08**.
9. Wave B stos / TLS / proxy / live POM warstwy → **09**.
10. Wave 3 compose HTTPS + TLS depth + Live POM P1: [wave-3-compose-tls-pom](../wave-3-compose-tls-pom/) (UC-W3-*, indeks ID).

## Indeks

| Plik | Zawartość | Prefiks |
|---|---|---|
| [01-business-gap-analysis.md](01-business-gap-analysis.md) | As-built inventory, FR, docs-only | `GAP-RFC-*` |
| [02-test-strategy.md](02-test-strategy.md) | Piramida, oracles, ISTQB | — |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | RLS UI, filtry, paginacja, badge, modal | `PW-RFC-E2E-###` |
| [04-playwright-api-rest.md](04-playwright-api-rest.md) | List query, BOLA 404, compare (2×403), JDBC, issuer/CORS | `PW-RFC-API-###`, `RA-RFC-###` |
| [05-hard-controls-composition.md](05-hard-controls-composition.md) | USelect, dual storageState, BFF :3000 | — |
| [06-istqb-ep-bva.md](06-istqb-ep-bva.md) | Partycje dat, amount, page | `EP-RFC-*`, `BVA-RFC-*` |
| [07-istqb-decision-state-usecase.md](07-istqb-decision-state-usecase.md) | DT / ST / UC / pairwise | `DT-RFC-*`, `ST-RFC-*`, `UC-RFC-*` |
| [08-traceability-matrix.md](08-traceability-matrix.md) | FR → ID | — |
| [09-wave-b-stack-tls-catalog.md](09-wave-b-stack-tls-catalog.md) | Smoke, RA proxy/TLS, POM HTTP+TLS, UC, DT/EG | `STK-RFC-*`, `RA-RFC-03x`, `PW-RFC-E2E-05x`, `UC-RFC-05x` |

## Warstwy wykonania

```text
Learner POM          tests-pom-learner          — poza Wave A
Live POM TLS         playwright.pom.tls.config.ts — HTTPS :8443 hybrid **albo** `--full`
Live POM             tests-pom, zero fulfill    — Keycloak, filtry, RLS dual role
FE flag-off          playwright.rls-flag-off.config.ts — Nuxt :3010
REST Assured         *Test.java                 — list date/amount, RLS JDBC, forwarded Location
IT Failsafe          *IT.java                   — Spring flag on/off
Modulith             ModulithArchitectureTest   — granica rlslab → tenant public
Flyway               V17–V18 rls_lab_*          — FORCE RLS + rls_lab_app + rls_lab_bypass
Stack                scripts/dev-stack.sh       — compose + Spring dev,seed + Nuxt
```

`tests/e2e` (mocked Chromium) **nie istnieje**. Paginacja / hub RLS / amount URL są w `tests-pom`.

## Pokrycie

| Wartość | Znaczenie |
|---|---|
| `existing-pw` | Historyczne — drzewo `tests/e2e` usunięte; ślad = `existing-pom` |
| `existing-pom` | Jest w `tests-pom` |
| `existing-ra` | REST Assured `*Test.java` |
| `existing-it` | `*IT.java` |
| `designed` | Zaplanowany, nie w tej fali |
| `docs-only` | FR świadomie bez kodu (QUERY, SF7 versioning/retry) |
| `existing-setup` | Skrypt/compose/docs (`dev-stack.sh`, tls-lab) |

## Problem+json — słownik RLS Lab

`Content-Type: application/problem+json`.  
`type` = `https://api.payment-quality.local/problems/{error-with-dashes}`.

| `error` | HTTP | Warstwa | Kiedy |
|---|---|---|---|
| `not_found` | 404 | Spring GET item | RLS ukrywa wiersz obcego tenanta |
| `rls_forbidden` | 403 | Spring GET compare | JWT ma `platform:payments:read`, ale tenant **nie** jest platform-scoped (RA-018) |
| *(brak `error=rls_forbidden`)* | 403 | SecurityConfig + `@PreAuthorize` | JWT bez `platform:payments:read` (RA-014, np. merchant manager) |
| `tenant_access_denied` | 403 | Spring | `TenantResolutionException` |
| `validation` | 400 | payment list | `minAmount > maxAmount`, złe daty |

Flag Spring off: **404** (brak controllera), jak Mirror Lab. BFF flag off: **404** Nitro.

## Poza zakresem

- RLS na `merchants` / `payment_orders` (zostaje Java WHERE).
- HTTP QUERY (Spring 7 nie ma `RequestMethod.QUERY`).
- API versioning, `@Retryable`, Kafka, gateway.
- `.kiro/**`.
