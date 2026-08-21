# Merchant 360 + Ops Wave 2 — wartość, uzupełnienie, curriculum testów

Dwa milestone-PW na tym samym Merchant Registry + Payment Orders. **Nie** są drugim CRM i **nie** dublują live-pom-wave-2.

| Milestone | Katalog BC/UC/BF | Backlog | Curriculum |
|---|---|---|---|
| Merchant 360 | [merchant-360-erp-lab](merchant-360-erp-lab/) | [playwright-merchant-360](../../status/roadmaps/playwright-merchant-360/) | gęsty ERP: lista, 360, RBAC kolumn, import, **payment** Kanban |
| Ops Wave 2 | [ops-wave-2-interaction-lab](ops-wave-2-interaction-lab/) | [playwright-ops-wave-2](../../status/roadmaps/playwright-ops-wave-2/) | 12 klas problemów ops: conflict, dirty, **support** Kanban, bulk partial, PIN, WS, views, i18n, gallery |

Warstwy: live POM E2E (`PW-*-E2E/SEC`) · Playwright REST BFF (`PW-*-API`) · REST Assured (`RA-M360-*` / `RA-OPS-*`). Zero `page.route` / `routeWebSocket`.

---

## Co dają operatorowi

Merchant 360 zamyka **odnalezienie i zmianę merchanta** w gęstym UI: server-side sort/filter/page, URL+Back, slideover bez zmiany route, create na boundach Zod, CSV preview zanim INSERT, drzewo tenant→merchant, Ctrl+K na żywych encjach, chart = `GET summary`.

Ops Wave 2 zamyka **dzień worki support/ops**: dwóch writerów (412 + karty Your/Latest), dirty form, kolejka spraw (nie status płatności), bulk assign per-item, PIN high-value refund, live feed, badge notyfikacji, saved views, polityka tenanta, locale EN/PL/SV, carousel evidence.

Razem: ten sam byt (merchant / payment order) da się **znaleźć jak w ERP** i **obsłużyć jak w ops** — bez Customers/Deals/Revenue i bez mocków sieci.

---

## Jak się uzupełniają (implement once)

| Temat | M360 | Ops Wave 2 | Uzupełnienie |
|---|---|---|---|
| Merchant If-Match | E4: 412 + Reload | E1: conflict workspace (Your/Latest) | Jeden kontrakt 412/428; UI conflict tylko w Wave 2 |
| Ctrl+K search | E6: live GET | E9: RBAC grup + last-wins | Drugi Ctrl+K zakazany |
| Kanban | E5: **payment** lifecycle | E3: **support case** | Disjoint kolumny i POST |
| Bulk | E2: activate DRAFT | E4: assign cases **partial** | Inny zasób, inny kontrakt retry |
| Lista vs kolejka | UTable + URL | Work Queue + WS/notif | Czytanie vs praca na sprawie |
| Evidence | upload/list (real-stack) | gallery + 404 problem | Przegląd, nie drugi Download Lab |
| i18n / PIN / dirty / WS | non-goal M360 | E2, E5–E7, E11 | Wave 2 dokłada klasy, których ERP lista nie uczy |

Flyway: M360 V23–V30 · Wave 2 V31–V36. Token nigdy w JS. `@nuxt/ui` 4.7.1.

---

## Podział warstw testów (czego **nie** dublować)

| Warstwa | Uczy | Nie uczy (zostaw drugiej warstwie) |
|---|---|---|
| **REST Assured** JWT + Testcontainers | Status, problem+json, ETag/If-Match, 409 vs 412 vs 428 vs 429, izolacja tenanta, JSONB, Flyway | Czy slider ma `aria-valuenow`; czy Back zostawia URL |
| **Playwright REST** cookie BFF | Sesja Nitro, forward If-Match, Zod na `:3000`, BOLA 404 jak w UI, brak tokenu w body | Pełna macierz 400 Springa |
| **Live POM E2E** | Rola, klawiatura, dialog, dwa `storageState`, `waitForResponse` exact path, WS `framereceived`, Intl | Pixel-drag slidera; `route.fulfill` „zepsutego” blobu |

Oracle sukcesu = HTTP + DB tam, gdzie write; UI = stan, który operator widzi. Nie-oracle: sam `innerText`, schowany przycisk bez 403, sort w RAM.

---

## Czego uczą konkretne testy Playwright E2E

| ID (przykład) | UC | Lekcja SDET |
|---|---|---|
| PW-M360-E2E-001 | UC-M360-04 | Caption Overview = `totalElements`, nie `content.length` |
| PW-M360-E2E-020 | UC-M360-11 | Sort = `waitForResponse` z `sort=`, nie kolejność DOM |
| PW-M360-E2E-030 | UC-M360-10 | Filtr w URL; Back wraca query |
| PW-M360-SEC-010 / API-040 | UC-M360-30 | UI hide ≠ 403; probe POST i tak |
| PW-M360-SEC-020 | UC-M360-31 | Dwa contexty; 412 nie 409 |
| PW-M360-E2E-080 | UC-M360-40 | `setInputFiles`; preview bez INSERT |
| PW-M360-E2E-090 | UC-M360-42 | Kanban = istniejący POST lifecycle |
| PW-M360-E2E-110 | UC-M360-51 | Ctrl+K last GET wins |
| PW-OPS-SEC-020 / E2E-130 | UC-OPS-14/15 | Conflict UI po 412; Escape bez drugiego PATCH |
| PW-OPS-E2E-160…164 | UC-OPS-16/17 | Stay/Discard/`beforeunload`; PATCH count 0 |
| PW-OPS-E2E-110…112 | UC-OPS-18/19 | Menu Move P0; 412 rollback karty |
| PW-OPS-E2E-150 / API-031 | UC-OPS-20/21 | Partial success; retry tylko failed ids |
| PW-OPS-E2E-170…175 | UC-OPS-23 | `pressSequentially`, `page.clock`, nie DOM PIN |
| PW-OPS-E2E-120…124 | UC-OPS-25/26 | Prawdziwy WS; inject zamiast `routeWebSocket` |
| PW-OPS-E2E-140…147 | UC-OPS-28–30 | Views bez JWT w localStorage |
| PW-OPS-E2E-180…184 | UC-OPS-35 | BVA + `aria-valuenow`; nie pixel drag |
| PW-OPS-E2E-210…213 | UC-OPS-36 | `test.use({ locale })` ≠ tłumaczenie POM; `Intl` + `[\s\u00a0]` |
| PW-OPS-E2E-220…224 | UC-OPS-37/38 | Carousel klawiatura; broken = 404 slide, nie mock |

---

## Czego uczą konkretne testy REST (RA + BFF)

| ID (przykład) | UC | Lekcja |
|---|---|---|
| RA-M360-010…017 | UC-M360-01/02 | Page/size/total; tenant mask; zły sort 400 |
| RA-M360-040 / 050 | UC-M360-30/31 | Readonly 403; stale If-Match 412 + DB unchanged |
| RA-OPS-050…055 | UC-OPS-13 | 428 brak If-Match; 412 stale; 409 nie jest lockiem |
| RA-OPS-110…122 | UC-OPS-11 | Maszyna case; illegal 409 |
| RA-OPS-150…154 | UC-OPS-20 | Bulk 2 ok + 2 fail; pusta lista 400 |
| RA-OPS-170…179 | UC-OPS-22 | Hash ≠ PIN; 429 lockout; maker ≠ checker |
| RA-OPS-125…127 | UC-OPS-26 | Inject admin 201; readonly 403 |
| RA-OPS-140…143 | UC-OPS-29 | Views per `sub`; unknown filter 400 |
| RA-OPS-180…185 | UC-OPS-34 | Policy JSONB na `tenants`; 412 JSONB unchanged; isolation `/current` |
| PW-OPS-API-070 | UC-OPS-38 | BFF 404 **problem** (nie koperta h3 `statusCode`) |

REST Assured = owner macierzy. Playwright REST = sesja i BOLA jak UI. E2E nie powtarza 101/400.

---

## Happy-path (rozmowa / demo)

1. M360: znajdź → 360 → create unique → RBAC 403 → dwóch adminów 412 → import preview → Move AUTHORIZED → Ctrl+K.
2. Ops: drugi writer 412 + tabs → dirty Stay → Move case → bulk partial + Retry failed → PIN → capture w feed → inject dup → Locale PL amount → gallery next / 404 slide.

Persony: M360 uczy **gęstość i kontrakt listy**; Ops uczy **konflikt, asynchroniczność i formularz z ARIA**.
