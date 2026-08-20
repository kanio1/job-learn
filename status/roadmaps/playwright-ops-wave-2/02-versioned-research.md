---
name: playwright-ops-wave-2-research
parent: playwright-ops-wave-2
last_updated: 2026-08-20
---

# 02 — Research wersji (Firecrawl + Context7)

Pełna notatka: [.codex/research/ops-wave-2-versioned-stack.md](../../../.codex/research/ops-wave-2-versioned-stack.md).

## Pin (nie bumpować w tym milestone)

| Piece | Wersja w repo |
|---|---|
| Playwright | 1.61.0 |
| @nuxt/ui | 4.7.1 |
| Nuxt | 4.4.6 |
| TypeScript | 6.0.3 |
| PostgreSQL | 18 (compose) |
| Spring Boot / Modulith | 4.0.6 / 2.0.6 |
| Java | 25 |

## Binding do stories

| Temat | Oficjalne zachowanie | Story |
|---|---|---|
| `page.route` / `routeWebSocket` | mock; WS default **nie** łączy z serwerem | **zakaz** tests-pom |
| `page.on('websocket')` + `framereceived` | inspekcja żywego kanału | E6 |
| `waitForResponse` przed click | prawdziwy PATCH/POST | E1–E5, E8 |
| Dwa `browser.newContext` | izolowani użytkownicy | E1, E3 |
| `page.goBack` + `UModal` | in-app leave | E2 |
| `dialog` `beforeunload` + `close({ runBeforeUnload })` | native leave | E2 |
| `test.use({ locale, timezoneId })` | `navigator.language`, format | E11 |
| `dragTo` | pointer; flakowy | E3 P1 |
| PinInput `length` / `complete` | 4.7.1; `separator` **4.9+** | E5 |
| DashboardSearch Fuse + `meta_k` | 4.7.x; `searchDelay` 4.7.0 | E9 |
| LocaleSelect + `@nuxtjs/i18n` | docs: para z i18n module | E11 |
| PG btree / `IF NOT EXISTS` | nie CONCURRENTLY w Flyway | E3–E8 |
| 412 vs 409 | lab: stale If-Match vs conflict biznesowy | E1, E3 |

## Korekty analizy CRM / ChatGPT

| Analiza mówiła | Ten lab |
|---|---|
| `page.routeWebSocket` do duplicate/out-of-order | Inject HTTP na żywym WS; `routeWebSocket` = mock |
| Concurrent edit 412 **lub** 409 | Tylko **412** na stale ETag |
| Kanban płatności | To M360 E5; tutaj **support cases** |
| Nowy command palette | Rozszerzyć istniejący `UDashboardSearch` |
| PinInput separator | Zakaz (4.9+) |
| Trzy pełne projekty PW en/pl/sv | Jeden project `locale` + `test.use` |
| Token na WS z przeglądarki | Nitro same-origin, sealed session |
| Kafka live feed | In-memory SimpleBroker, jedna instancja `--app` |

## Komponenty Nuxt UI vs pin 4.7.1

Live docs mogą opisywać 4.10+. **Gate przed UI:** wypisać eksport z `node_modules/@nuxt/ui@4.7.1`.

| Potrzeba | 4.7.1 (oczekiwane) | Fallback |
|---|---|---|
| Pin | `UPinInput` length 6 | 6× `UInput` |
| Search | `UDashboardSearch` | już w layout |
| Kanban | `UCard` `UUser` `UBadge` | — |
| Conflict | `UModal` `UTabs` `UAlert` | — |
| Bulk | `UCheckbox` `UProgress` `UModal` | — |
| Rules | `USwitch` `UInputNumber` `USlider` `URadioGroup` | — |
| Feed | `UChip` `UToast` `UTimeline` | `UBadge` + `ol` |
| Notif | `UPopover` `UChip` `UUser` | — |
| Views | `UPopover` `UCheckboxGroup` | lista `UCheckbox` |
| Locale | `ULocaleSelect` + i18n | `USelect` języków |
| Gallery | `UCarousel` | dialog + next/prev |

## HTTP concurrency

| Kod | Znaczenie już zaimplementowane | Wave 2 dodaje |
|---|---|---|
| 409 | duplicate merchant, idempotency, self-approve | nielegalna transycja case |
| 412 | stale If-Match **payment** / tenant settings | merchant PATCH, case move, policy |
| 428 | brak If-Match payment | merchant / case / policy |
| 429 | (brak w core payment) | PIN lockout |
