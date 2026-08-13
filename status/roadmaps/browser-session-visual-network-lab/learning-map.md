---
name: mrl-learning-map
parent: browser-session-visual-network-lab
last_updated: 2026-08-13
---

# Mapa nauki — HTTP · cookies · OIDC · visual · network

Każdy epic/story ma **cel produktowy** i **cel dydaktyczny**.

## Cookies i storage (to, co ma każda sesja przeglądarki)

| Temat | Gdzie | Co obserwujesz |
|---|---|---|
| `Set-Cookie` / `Cookie` | E1-S1 | Request niesie Cookie; response może Set-Cookie |
| HttpOnly | E1-S1, FR-S08 | JS `document.cookie` **nie** widzi `nuxt-session`; Playwright `context.cookies()` **tak** |
| Secure | E1-S1 | localhost HTTP vs HTTPS |
| SameSite Lax/Strict/None | E1-S1, E3-S4 | OIDC return + CSRF |
| Path / Domain | E1-S1 | KC cookies na :8180, BFF na :3000 |
| `storageState` JSON | E1-S6 | cookies + localStorage origins; **nie** sessionStorage |
| sessionStorage | E1-S6 | wymaga `addInitScript` — pułapka docs Playwright |
| localStorage | FR-S08 | **zakaz** JWT |
| IndexedDB | non-goal wave 1 | wspomnieć w copy inspector |
| Guest empty state | FR-S07 | `{ cookies: [], origins: [] }` |

## HTTP / REST

| Temat | Gdzie | Lekcja |
|---|---|---|
| 403 CSRF vs 401 missing auth | E1-S5 | różne `error` w problem+json |
| 403 GET with body | E4-S1 | PayU + RFC 9110 |
| 302 follow(false) | CPL + E4 | HTML 200 jeśli klient follow |
| 503 then 200 | E3-S1 | retry vs 400 no-retry (CPL HMAC) |
| CORS + credentials | E3-S4 | cookie nie poleci bez ACA-Credentials |
| Content-Disposition | E5-S2 | download, nie nawigacja |
| multipart | E5-S3 | REST-MULTIPART-01 |
| 202 notify | CPL / E4-S3 | ACK ≠ fulfillment |

## Keycloak / OIDC

| Temat | Gdzie | Lekcja |
|---|---|---|
| authorization_code + PKCE | świat A | dashboard |
| client_credentials form-urlencoded | świat C | PayU POS / CPL |
| trusted_merchant stub | E4-S6 | `extCustomerId` — **nie** KC user |
| end_session | E1-S3 | cookie BFF vs SSO |
| ACR step-up | E5-S1 | BFF first; realm later |
| Public page | świat B | SAQ-A mindset, no KC cookie |

## Playwright

| Temat | Gdzie | Lekcja |
|---|---|---|
| `storageState` setup | E1-S6 | setup project + gitignore |
| `browser.newContext` multi-role | E1-S4, E5-S4 | maker-checker |
| `context.cookies()` | E1-S1 | HttpOnly visible to automation |
| `page.clock` | E1-S2, E4-S4 | idle + expiry |
| `toHaveScreenshot` | E2 | golden, maxDiff, mask |
| `stylePath` / mask | E2-S3 | dynamic UUID |
| `toMatchAriaSnapshot` | E2-S2 | struktura ≠ piksele |
| `page.route` fulfill/abort/continue | E3 mocked | |
| `route.fetch` + mutate | E3-S2 lie | |
| `waitForResponse` | E3 POM | zakaz fulfill |
| HAR | E3-S3 | recordHar |
| `waitForEvent('download')` | E5-S2 | |
| `setInputFiles` | E5-S3 | |
| `frameLocator` | E4-S5 | iframe widget |
| `context.setOffline` | E3-S2 | |

## Macierz story → tagi

| Story | HTTP | CK | KC | PW | REST |
|---|---|---|---|---|---|
| E0-S1 | flaga 404 | | freeze realm | | |
| E1-S1 | Set-Cookie | ✔ | Domain KC | cookies() | |
| E1-S2 | | idle cookie | re-auth | clock | |
| E1-S3 | 302 login | clear | end_session | empty storageState | |
| E1-S4 | | session list | | two contexts | |
| E1-S5 | 403 csrf | | | | problem+json |
| E1-S6 | | storageState | | setup project | |
| E2-S1..S3 | | | | screenshot | |
| E3-S1..S4 | 503/CORS | Cookie strip | | route/HAR | |
| E4-S1 | 403 GET body | | | | RFC 9110 |
| E4-S2 | lang query | | | hosted | |
| E4-S3 | notify refund | | bez JWT | | HMAC |
| E4-S4 | 409 expired | | | clock | |
| E4-S5 | | cookie-free frame | | frameLocator | |
| E4-S6 | form OAuth | | kontrast | | |
| E5-S1 | | | ACR/BFF | | |
| E5-S2 | Content-Disposition | | | download | |
| E5-S3 | 201 multipart | | | filechooser | REST-MULTIPART |
| E5-S4 | 409 dual control | | two roles | two storageState | |
| E5-S5 | | consent cookie | consent | guest+auth | |
