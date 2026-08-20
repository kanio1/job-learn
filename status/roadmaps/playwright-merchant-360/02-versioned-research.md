---
name: playwright-merchant-360-research
parent: playwright-merchant-360
last_updated: 2026-08-20
---

# 02 — Research wersji (Firecrawl)

Pełna notatka: [.codex/research/merchant-360-versioned-stack.md](../../../.codex/research/merchant-360-versioned-stack.md). Context7 MCP: brak klucza.

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
| Locators | `getByRole` priorytet; CSS/XPath kruche; `nth` last | E2, E3 |
| `locator.filter` | wiersz po tekście referencji | E2-S2 |
| ARIA snapshot | YAML role/name/state; `toMatchAriaSnapshot` page **1.60+** | E3, E6 |
| Network | `waitForResponse` **przed** click; `page.route` = mock → **zakaz** w tests-pom | wszystkie E2E |
| Auth | `storageState` setup; dwa contexty do ról | E4 |
| UTable | sort, selection, pagination w docs; `row-selection` event może lagować (#5408) — v-model | E2 |
| PG 18 index | btree default; `IF NOT EXISTS`; nie CONCURRENTLY w Flyway | E1 |
| Spring Page | lab już ma własne DTO list; nie Spring Data REST auto `Page` JSON | E1 |

## Komponenty Nuxt UI vs pin 4.7.1

Live docs (Table, FileUpload, Tree, Editor) mogą opisywać nowszy 4.x. **Gate E5/E6/E7:** przed story UI wypisać eksport z `@nuxt/ui@4.7.1`. Fallback:

| Potrzeba | Fallback 4.7.1 |
|---|---|
| FileUpload | obecny evidence `<input type=file>` + `setInputFiles` |
| Tree | `UNavigationMenu` nested albo lista `role=tree` ręcznie |
| Calendar | `UInput type=date` (już jest na filtrach) |
| Timeline | lista audit (już jest) + semantyka `ol` |
| Stepper | `UTabs` / numerowane karty |
| Editor | `UTextarea` notes |

## HTTP concurrency (korekta analizy CRM)

Analiza używała `409 Conflict`. Ten lab:

| Kod | Znaczenie już zaimplementowane |
|---|---|
| 409 | duplicate merchant, idempotency fingerprint, dual-control self-approve |
| 412 | stale `If-Match` payment |
| 428 | brak `If-Match` |

Merchant ETag kopiuje **412/428**, nie 409. Copy UI: „This merchant was modified” + Reload.
