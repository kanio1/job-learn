# 02 — Strategia testów (test architect)

Wave 2 uczy **czterech oracles**: UI, HTTP (BFF `:3000`), cookie/storage, **nie** SQL z Playwright.

## 1. Cele jakości

| Cel | Oracle |
|---|---|
| Guest nie widzi dashboardu | URL `/login?redirectTo=` + `LoginPage` |
| Logout działa | ponowny `goto /admin` → login |
| Sesja nie wycieka JWT | HttpOnly `nuxt-session`; `origins` storageState bez `eyJ` |
| Zapis jest prawdziwy | `GET /api/merchants/{id}` 200 po create |
| Walidacja jest klientem | brak POST `/api/merchants` |
| Role nie mieszają merchanta | manager `createPaymentOrder` Alpha; admin bez `merchant:payments:create` |
| IDOR Support | problem+json, `table` count 0 |
| Pieniądze checkout | `fulfillment-status`, nie sam hint `status=` |
| POM czysty | zero `page.route` / `fulfill` |
| Guest bez API | nie destructure `api` w `session-guest.spec.ts` |

## 2. Piramida

```text
        PW-E2E live POM (guest, logout, CASH, decline, IDOR, palette ARIA)
           PW request BFF (409, GET persist, listNotes, dual storageState)
              REST Assured / IT (kontrakt Spring, flagi) — istniejące, nie Wave 2
                 Unit Vue / Zod — poza tą mapą
```

**Zasada:** Playwright nie łączy się z Postgres. Persistence = GET BFF z tą samą sesją. Kontrakt nagłówków (`Idempotency-Key`, `If-Match`) = `page.waitForRequest` albo `BffClient`, nie mock. Czysty REST body+DB = Java.

## 3. Ryzyko → głębokość

| Ryzyko | Prio | Technika | Warstwa |
|---|---|---|---|
| Unauth 200 na `/admin` | P0 | guest project | E2E |
| JWT w `localStorage` / pliku `.auth` | P0 | storage helpers (tylko `origins`) | SEC |
| UI create bez tenanta = 400 | P0 | API create + GET | API + E2E |
| False persist (tylko innerText) | P0 | `assertPersistedMerchant` | API |
| Notes/risk 403 ukryty jako pass | P1 | gałąź 201\|200 vs 403 | E2E |
| Decline vs `CANCELED` sesji | P0 | fulfillment `CANCELLED` | E2E |
| Lie return = success hint | P0 | fulfillment ≠ CONFIRMED | E2E (istniejący) |
| Support nav hidden ale API wycieka | P0 | deep-link + problem | SEC |
| Overlay łapie click | P1 | locator handler | fixtures |
| IPv6 ECONNREFUSED | P0 | BFF `127.0.0.1` | API |

## 4. Dane i role

| Parametr | Wartość |
|---|---|
| Platform admin | `tests-pom/.auth/platform-admin.json` (gitignored) |
| Merchant manager | `merchant-manager.json` — tenant Alpha |
| Guest | `{ cookies: [], origins: [] }` |
| Merchant create | `uniqueMerchantReference` + `TENANT_ALPHA` w `BffClient` |
| Notes order | manager POST Alpha; **nie** admin |
| Risk | nowy merchant, nie seed |
| Support probe | `merchantBetaId` |
| Checkout | `uniqueExtOrderId`; skip gdy brak nav |

## 5. ISTQB map

| Technika | Plik |
|---|---|
| Use case / journeys | 03, 07 UC |
| Equivalence / BVA | 06 |
| Decision table (role × zasób) | 07 DT |
| State (merchant, checkout fulfillment) | 07 ST |
| Error guessing (IPv6, overlay, double status query) | 07 EG |
| Pairwise (mode × outcome) | 07 PWISE |

## 6. Zakaz

- `waitForTimeout` jako sync.
- `page.route` w `tests-pom`.
- Asercja JWT w **cookie blob** (sealed session może zawierać `eyJ`).
- SQL / Testcontainers z Node.
- Import page objects z `tests-pom` do learner.
- Uczenie „hint `status=success` = pieniądze”.
