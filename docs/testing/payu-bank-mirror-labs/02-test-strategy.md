# 02 — Strategia testów (test architect)

MRL to **edukacyjne lustra**, nie PSP i nie bank. Strategia rozdziela **mock vs live**, **Nuxt BFF vs Spring**, i nie dubluje CPL.

## 1. Cele jakości

| Cel | Oracle |
|---|---|
| JWT nie wycieka | `storage-safety` + brak `eyJ` w Web Storage / committed state |
| Hosted ≠ dashboard | `layout: false` → brak `session-lab-idle-lock`; GET hosted bez `nuxt-session` |
| Idle bez flake | `page.clock.install` + `fastForward`; **nie** `waitForTimeout` |
| Unlock = re-auth | po Unlock URL `/login` **i** pusta sesja (middleware nie wraca na `/admin`) |
| Visual łapie kolor | kafelki + mask; próg `maxDiffPixelRatio: 0.02` |
| Lie body ≠ pieniądze | fulfillment/DB, nie JSON `success` |
| PDF nie jest tekstem | magic bytes `%PDF-` w pliku download (BFF `arrayBuffer`) |
| Refund idempotentny | status `REFUNDED`; COUNT event = 1; drugi POST 409 |
| POM czysty | zero `page.route` / `route.fulfill` |
| CSRF nie na merchant BFF | lab 403; `POST /api/merchants/...` bez CSRF nadal kontrakt |
| Desktop only | brak project `iPhone` |

## 2. Piramida

```text
        PW-E2E live POM (cookies, idle logout, download, dual storageState)
           PW-E2E mocked (visual, iframe, route sequences, HAR, PDF fulfill)
              PW-API request :3000 (CSRF, flag 404, TPP header, CORS)
                 REST Assured / IT (Spring flag, multipart, refund, TPP, GET-body)
                    Modulith + Flyway validate
                       Unit designed: CSRF string compare, idle TTL
```

**Zasada:** nie dublować każdego RA w PW. PW tam, gdzie przeglądarka (Set-Cookie, screenshot, iframe, download, clock). Spring tam, gdzie status maszyny i HMAC.

## 3. Ryzyko → głębokość

| Ryzyko | Prio | Technika | Warstwa |
|---|---|---|---|
| Unlock bez `session.clear()` wraca na dashboard | P0 | UC + middleware | POM |
| Token w localStorage | P0 | evaluate | POM |
| Idle flake (real time) | P0 | Clock | POM |
| Visual flake OS/font | P0 | C-06 Docker | mocked CI |
| BFF ignoruje flagę FE | P0 | 404 Nitro | PW request + FE env |
| PDF BFF `responseType: text` psuje bajty | P0 | magic bytes | mocked + designed live |
| Repeat refund emituje drugi event | P0 | ST + DB count | RA |
| CSRF na całym BFF (regresja) | P0 | kontrast merchant POST | RA / PW request |
| GET body 403 false positive na POST | P1 | EP | RA |
| TPP token w query logowany | P1 | header prefer | RA + UI |
| TPP brute-force | P1 | 429 | RA |
| HAR z sekretami w git | P0 | review fixture | process |
| Iframe cross-origin | P1 | same-origin only | E2E |
| 503 counter przecieka między testami | P1 | TTL 10s | Nitro |
| Servlet 1MB blokuje 413 app-level | P1 | yaml 5MB vs limit 2MB | RA |

## 4. Oracles

1. Cookie atrybuty z `context.cookies()`, nie z samego innerText.
2. HTTP status + problem `error` (tabela README).
3. Fulfillment/DB/`checkout_event` dla pieniędzy i refund notify.
4. Screenshot **albo** ARIA — nie mylić.
5. Download: filename **i** magic bytes (PDF) / nagłówek CSV.
6. Unlock: URL `/login` + brak dostępu do `/admin` bez ponownego logowania.
7. Zakaz: `waitForTimeout`; visual na żywych listach UUID; fulfill w POM.

## 5. Dane i stałe z kodu

| Parametr | Wartość |
|---|---|
| Idle TTL | `NUXT_PUBLIC_MIRROR_LAB_IDLE_SECONDS` default **120** |
| Step-up / `stepUpUntil` | `app.mirror-lab.step-up-threshold-minor` **10000**; TTL step-up **300s** |
| Evidence max | **2 MiB** app; servlet **5MB** |
| Evidence types | `application/pdf`, `image/png`, `image/jpeg`, `text/plain`, `text/csv` |
| TPP limit | **30**/min/`remoteAddr` |
| 503 retry TTL | **10s** in-memory |
| CSRF cookie | `mrl-csrf`, `httpOnly: false`, `sameSite: lax` |
| Merchant lab UUID | `00000000-0000-0000-0000-0000000000b1` |
| PDF lab body | ASCII `%PDF-1.4\n…%%EOF\n` |
| Refund event | `checkout.session.refunded` |
| Flaga FE | default **on** (`!== 'false'`) |
| Flaga Spring | default **false**; `dev` true |

- UUID v4 jak CPL; `Idempotency-Key` per test (CPL, nie MRL bank).
- Evidence fixture: `tests-pom/data/files/sample-evidence.txt`.
- Clock: `page.clock` na idle; CPL `EXPIRED_LINK` na expiry hosted.
- GET-with-body: `java.net.http.HttpClient` (Apache RA odrzuca GET body).

## 6. ISTQB FL → MRL

| Technika | Gdzie |
|---|---|
| EP | CSRF; format csv/pdf; lang; TPP token source; flag on/off |
| BVA | idle 119/120/121s; amount 9999/10000/10001; file 2MiB / 2MiB+1; TPP 30/31 |
| DT | grant × Content-Type; mock vs POM; Unlock × middleware |
| ST | checkout COMPLETED→REFUNDED; approval PENDING→APPROVED; consent GRANTED→REVOKED |
| UC | logout-on-unlock; lie body; iframe pay; maker-checker dwa contexty |
| Pairwise | SameSite × origin (P2) |
| Error guessing | GET body; PDF as text; self-approve; foreign revoke |

## 7. Existing vs designed (skrót)

| Obszar | Existing | Designed |
|---|---|---|
| Hub + nav | mocked mirror-lab | FE flag-off project |
| Idle + Unlock `/login` | POM clock | TTL-1; overlay na merchants |
| CSRF fail | POM | happy + merchant kontrast |
| Visual tiles | mocked screenshots | full-page; ARIA |
| 503 / abort / HAR | mocked + POM 503 | offline; strip; CORS |
| GET-body / lang / refund | RA | inspector UI refund |
| Widget iframe | mocked frameLocator | POM live widget pay |
| Step-up / multipart / maker | RA + POM rbac | UI step-up; live PDF |
| TPP query+header+owner | RA | 429; UI header |
| Learner | skip placeholders | copy-from-reference |

## 8. Split suite (C-05 / FR-N07)

| Suite | `page.route` / fulfill | Backend |
|---|---|---|
| `tests/e2e` | **tak** | nie wymagany (mock session) |
| `tests-pom` | **zakaz** | Keycloak + Spring + Nuxt |
| RA / IT | n/a | Testcontainers Postgres |
| `tests-pom-learner` | n/a | skip aż uczeń skopiuje POM |
