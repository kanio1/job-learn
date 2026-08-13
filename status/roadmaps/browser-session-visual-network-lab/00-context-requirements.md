---
name: mrl-context-requirements
parent: browser-session-visual-network-lab
origin: POST_KIRO_WORK
last_updated: 2026-08-13
---

# 00 — Kontekst, wymagania, non-goals

```text
ORIGIN: POST_KIRO_WORK
RATIONALE: Lab uczy merchant REST + CPL redirect/notify, ale nie trzech światów sesji przeglądarki,
          visual comparison poza badge, ani network interception poza Error Lab 429.
SOURCE_DOCUMENT: status/roadmaps/browser-session-visual-network-lab/
RELATED_KIRO_TASKS: none
ACCEPTANCE_CRITERIA: design package complete; no application code; FR mapped to epics and test IDs
```

## 1. Problem biznesowo-edukacyjny

Checkout Protocol Lab uczy łańcucha PayU-like **create → 302 → hosted → lie continueUrl → HMAC notify**.  
Dashboard uczy OIDC + RBAC. Brakuje luster, których wymaga rynek SDET 2026 i bankowość desktopowa:

- co **naprawdę** jest w sesji przeglądarki (cookies vs `localStorage` vs `sessionStorage` vs `storageState`)
- visual comparison (golden PNG, mask, dark mode) — F-D5 to tylko badge
- network interception (fulfill / abort / continue / HAR / 503→200) z jawnym split mock vs live

Źródła rynkowe: PayU Europe (OAuth `client_credentials`, 302 `redirectUri`, notify, refunds, management panel, reporting); PSD2/SCA (idle timeout, step-up, dynamic linking); Playwright docs (`toHaveScreenshot`, `page.route`, `storageState`).

## 2. Cel produktu (MRL)

Flagowane, lokalne laby lustrzane. Enterprise dashboard **albo** end-user desktop (hosted / consent) — **nie** mobile.

| Decyzja | Wartość |
|---|---|
| Transport | Istniejący BFF + Spring; nowe lab endpointy za flagą |
| IdP | Keycloak — **bez zmian realm w E1** |
| PSP | Nadal stub CPL, nie PayU/Stripe |
| Isolation | Powierzchnie labowe; E4 może rozszerzyć `checkoutlab`, E1–E3 głównie Nuxt + małe BFF |
| Visual | Dedykowana strona stabilnych kafelków, nie screenshot list z UUID |

## 3. Constraints (MUST)

| ID | Wymaganie |
|---|---|
| C-01 | Brak Kafki, realnego PSP, PAN/3DS, KYC, settlement produkcyjnego |
| C-02 | Brak mobile viewport jako wymagań produktowych / projektów PW |
| C-03 | Flagi off → 404 / ukryty nav; default `false` poza `dev`/`test` |
| C-04 | Trzy światy tokenów nie mieszają się |
| C-05 | POM live: zero `page.route` / `route.fulfill` |
| C-06 | Visual golden tylko z ustalonego środowiska (CI Playwright Docker) |
| C-07 | Wave 1: brak zmian `payment-quality-realm.json` |
| C-08 | Iframe widget: same-origin; hosted new-tab CPL pozostaje |
| C-09 | JWT nie w `localStorage` / `sessionStorage` / committed `storageState` |

## 4. Functional requirements

### Session (E1)

| ID | Opis | Epic |
|---|---|---|
| FR-S01 | Cookie inspector: nazwa, HttpOnly, Secure, SameSite, Path, Expires | E1 |
| FR-S02 | Porównanie: `nuxt-session` vs cookies Keycloak vs **brak** cookies na hosted | E1 |
| FR-S03 | Idle timeout + lock screen + countdown; re-auth bez sleep w testach | E1 |
| FR-S04 | Logout BFF + OIDC end-session; pusty storageState nie wchodzi na `/admin` | E1 |
| FR-S05 | Lista sesji równoległych + revoke (labowy, nie Keycloak admin w E1) | E1 |
| FR-S06 | CSRF na mutating BFF cookie-auth → 403; Bearer API **bez** CSRF | E1 |
| FR-S07 | Guest project: `storageState: { cookies: [], origins: [] }` | E1 |
| FR-S08 | Asercja: brak `eyJ` / Bearer w Web Storage i w pliku `.auth/*.json` | E1 |

### Visual (E2)

| ID | Opis | Epic |
|---|---|---|
| FR-V01 | Strona `/admin/visual-lab` ze stabilnymi kafelkami (fixed copy, reduced-motion) | E2 |
| FR-V02 | Kafelki: merchant/payment badge, Problem Details, hosted CTA, lock-screen | E2 |
| FR-V03 | Light + dark | E2 |
| FR-V04 | Component screenshot + jeden full-page z `stylePath` mask `[data-dynamic]` | E2 |
| FR-V05 | Toggle „break visual” — ten sam stan, zły kolor — test **musi** paść | E2 |
| FR-V06 | Dokumentacja golden / `--update-snapshots` / kiedy **nie** screenshotować list | E2 |

### Network (E3)

| ID | Opis | Epic |
|---|---|---|
| FR-N01 | Przycisk 503 → retry → 200 (obserwowalny z UI + network) | E3 |
| FR-N02 | Abort / offline (`context.setOffline` w testach; UI pokazuje ErrorState) | E3 |
| FR-N03 | Lie body: HTTP 200 `success` przy fulfillment **nie** CONFIRMED | E3 |
| FR-N04 | Strip Cookie / Idempotency-Key via `route.continue` (mocked suite) | E3 |
| FR-N05 | HAR record + replay jednego booking/session flow | E3 |
| FR-N06 | CORS preflight + `Access-Control-Allow-Credentials` (kontrast z cookie) | E3 |
| FR-N07 | Split: mocked `tests/e2e` fulfill vs POM `waitForResponse` tylko | E3 |

### PayU mirrors (E4)

| ID | Opis | Epic |
|---|---|---|
| FR-P01 | GET zasobu z body → **403** (PayU / RFC 9110) | E4 |
| FR-P02 | `lang` na `redirectUri` + locale hosted page | E4 |
| FR-P03 | Refund notify na ten sam `notifyUrl` co order/session | E4 |
| FR-P04 | Payment link expiry UI + clock (dociągnięcie CPL FR-10) | E4 |
| FR-P05 | Same-origin iframe widget (`frameLocator`); new-tab zostaje | E4 |
| FR-P06 | Panel kontrastu grantów: `client_credentials` vs OIDC PKCE vs lab `trusted_merchant` stub | E4 |

### Bank-like (E5)

| ID | Opis | Epic |
|---|---|---|
| FR-B01 | Step-up (lab ACR) na refund/capture powyżej progu; kwota+merchant w challenge | E5 |
| FR-B02 | Labowy statement CSV/PDF + `Content-Disposition` + download event | E5 |
| FR-B03 | Disputes stub: OPEN → evidence multipart → CLOSED | E5 |
| FR-B04 | Maker-checker refund powyżej limitu (dwa storageState w jednym teście) | E5 |
| FR-B05 | Consent AIS-lite: read-only lista + revoke (desktop end-user) | E5 |
| FR-B06 | Dynamic linking copy: challenge pokazuje kwotę, nie tylko „potwierdź” | E5 |

## 5. Non-functional

| ID | Opis |
|---|---|
| NFR-01 | `X-Correlation-ID` na lab endpoints |
| NFR-02 | Problem+json spójny z resztą labu |
| NFR-03 | Visual: `maxDiffPixelRatio` ≤ 0.02; mask zamiast podnoszenia progu |
| NFR-04 | Idle: deterministyczny clock; zakaz `waitForTimeout` |
| NFR-05 | Passwords / storageState nigdy w git |
| NFR-06 | Modulith / flag IT gdy powstaje nowy moduł backend |

## 6. Non-goals

- Aplikacja mobilna, PWA-first, geolocation jako produkt
- Realny 3DS / PAN / tokenizacja karty
- Kafka, outbox produkcyjny, prawdziwy PayU IPN
- Zmiana produkcyjnego payment lifecycle API (E4 tylko `checkoutlab` + lab UI)
- Open Banking produkcyjny (Berlin Group) — tylko AIS-lite consent ekran
- Argos/Percy SaaS — najpierw wbudowany Playwright
- Service Worker / MSW jako warstwa mock (koliduje z `page.route`)

## 7. Mapowanie na istniejący kod

| Istniejące | Rola |
|---|---|
| `nuxt-session` HttpOnly | Świat A — FR-S01/S08 |
| `/psp/checkout/{id}` | Świat B — FR-S02 |
| CPL OAuth + HMAC | Świat C — FR-P06 |
| Error Lab | baza pod E3 |
| `visual-regression.spec.ts` | F-D5; E2 **nie** duplikuje badge-only jako jedynego celu |
| `playwright.pom.config.ts` guest project | FR-S07 szkic |
| `storage-safety.ts` | FR-S08 szkic |
| Evidence upload payment | baza pod FR-B03 / REST-MULTIPART-01 |
| CSV export payments | baza pod FR-B02 |

## 8. Definition of Done (program design)

- Katalog FR/NFR/C zmapowany na epiki i ID testów.
- Learning map story → tagi HTTP/CK/KC/PW.
- Task board `MRL-T01`… ze statusem TODO.
- `.kiro/**` nietknięte; `apps/**` nietknięte.
