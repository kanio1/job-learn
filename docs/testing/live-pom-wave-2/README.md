# Mapa testów — Live POM Wave 2 (dashboard)

Dokumentacja **task-force** (PM + test architect + test analyst).  
Cel: kompletna mapa przypadków, które **Wave 2 live POM** już pokrywa albo odblokowuje (sesja, persistence GET, UX, checkout CASH/decline, IDOR, Error Lab 412). **Nie jest to implementacja** nowych speców.

Źródła: `apps/frontend/tests-pom/` (`playwright.pom.config.ts`, `BffClient`, `chromium-guest`, helpers cookie/persistence), specy W2-01…W2-12.  
Analog: [checkout-protocol-lab](../checkout-protocol-lab/), [payu-bank-mirror-labs](../payu-bank-mirror-labs/), [rls-filters-composition-lab](../rls-filters-composition-lab/).

CPL / Mirror / RLS **nie duplikować 1:1** — tu jest warstwa **dashboard live POM** (Keycloak cookie → Nuxt BFF → Spring). Checkout happy-path/lie już są w CPL; tutaj tylko to, co Wave 2 dodał albo umożliwia (CASH select, decline fulfillment, guest, persistence).

---

## Jak czytać

1. Co jest w produkcie i jakie luki → **01**.
2. Warstwy, oracles, split mock/POM/RA → **02**.
3. UI E2E → **03**.
4. HTTP / REST przez BFF (`APIRequestContext` + `waitForResponse`) → **04**.
5. Sesja, cookie, RBAC, IDOR → **05**.
6. EP/BVA → **06**.
7. DT / ST / UC / pairwise / error guessing → **07**.
8. FR → TC → **08**.

Istniejące `test('…')` (tytuł, spec, HTTP) są w **03** i **07** (UC). Nie dublować 1:1 CPL/MRL/RFC — tam wskaźnik + ID.

---

## Indeks

| Plik | Zawartość | Prefiks |
|---|---|---|
| [01-business-gap-analysis.md](01-business-gap-analysis.md) | As-built Wave 2, luki produktowe vs testowe | `GAP-W2-*` |
| [02-test-strategy.md](02-test-strategy.md) | Piramida, oracles, ISTQB | — |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | Guest, logout, merchants, palette, notes, risk, checkout, support, **payments/RBAC/laby** — każdy existing TC = tytuł `test()` | `PW-W2-E2E-###` |
| [04-playwright-api-http.md](04-playwright-api-http.md) | BFF GET/POST, 409, Error Lab, nagłówki | `PW-W2-API-###` |
| [05-security-session-rbac.md](05-security-session-rbac.md) | HttpOnly, storageState, IDOR, dual role | `PW-W2-SEC-###` |
| [06-istqb-ep-bva.md](06-istqb-ep-bva.md) | Partycje formularzy, statusów, HTTP | `EP-W2-*`, `BVA-W2-*` |
| [07-istqb-decision-state-usecase.md](07-istqb-decision-state-usecase.md) | DT / ST / UC | `DT-W2-*`, `ST-W2-*`, `UC-W2-*` |
| [08-traceability-matrix.md](08-traceability-matrix.md) | FR → ID + P0–P2 | — |

---

## Warstwy wykonania

```text
Learner POM          tests-pom-learner          — copy-map; guest project; bez importu z tests-pom
Live POM             tests-pom, zero fulfill    — Keycloak, BffClient, guest, persistence GET
Mocked Chromium      tests/e2e + page.route     — poza tą mapą (HAR, visual, 429 mock)
PW request / BFF     APIRequestContext :3000    — 127.0.0.1 (IPv4); cookie sesji, nie Bearer w teście
REST Assured         *Test.java                 — kontrakt Spring; nie powielać 1:1 w PW
IT Failsafe          *IT.java                   — flagi labów; nie SQL z Node
```

Playwright `webServer` = Nuxt `--host 127.0.0.1`. Browser `baseURL` = `http://localhost:3000` (OIDC). Node `BffClient` = `http://127.0.0.1:3000`.

---

## Pokrycie

| Wartość | Znaczenie |
|---|---|
| `existing-pom` | Jest w `tests-pom` (Wave 2 lub wcześniejszy live spec) |
| `existing-pw` | Jest w `tests/e2e` (mocked) — tylko odniesienie, nie cel tej mapy |
| `existing-ra` | REST Assured — oracle kontraktu, nie implementować drugi raz w PW |
| `designed` | Nadal brak specu (P2 palette/ARIA, UI 409, MRL idle re-goto, CSRF happy) |
| `blocked` | Zależny od luki produktowej / realm (np. UI `tenantReference`, fine-grained notes/risk) |

Wave 3 zamknęło P1 gość (003/SEC-011/030), Error Lab 083 (exact status; canary 401), admin Support 071 (tabela), API-003/004/011, notes 041. HTTPS: [wave-3-compose-tls-pom](../wave-3-compose-tls-pom/). Pozostałe designed: **P2** pairwise UX / ARIA login, UI 409 (blocked GAP-W2-01).

---

## Tagi Playwright

`@security` guest / logout / cookie / IDOR / 412.  
`@a11y` `toMatchAriaSnapshot`.  
`@ux` palette / notes / risk / CASH / decline.

Projekty: `chromium-guest` (puste `storageState`, **nie** destructure `api`), `chromium-admin`, `chromium-manager`, `chromium-rbac`.

---

## Poza zakresem

- `page.route` / `route.fulfill` w `tests-pom`.
- Testcontainers / JDBC z Node; Kafka; prawdziwy PSP; PAN/3DS.
- Zmiana realm Keycloak jako „naprawa” testu (403 notes/risk = drift, nie fail-soft bez asercji).
- Katalogi CPL / Mirror / RLS — linkować, nie kopiować setek TC.
- Implementacja pozostałych P2 (`E2E-031/032`, CSRF happy). Wave 3 zamyka P1 gość / Error Lab / IDOR — [wave-3-compose-tls-pom](../wave-3-compose-tls-pom/).
