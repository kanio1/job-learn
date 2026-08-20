# Merchant 360 — versioned stack research (Firecrawl)

Status: DESIGNED_NOT_STARTED  
Date: 2026-08-20  
Context7: unavailable (`Invalid API key` / `ctx7sk`). Sources below are Firecrawl scrapes + developer search + this repo.

## Answer

Pinned lab versions remain: Playwright **1.61.0**, `@nuxt/ui` **4.7.1**, Nuxt **4.4.6**, TypeScript **6.0.3**, PostgreSQL **18**, Flyway via Spring Boot **4.0.6**, Java **25**. Live POM forbids `page.route` / `route.fulfill`. Network oracle is `page.waitForResponse` against the real BFF. Optimistic lock in this lab is HTTP **412** (If-Match), not 409.

## Why it matters here

The ERP/CRM analysis assumed generic CRM entities, `page.route` race tests, and HTTP 409 for concurrent edits. Job-learn already has Merchant Registry + Payment Orders on a real Podman stack. Version-sensitive APIs (UTable sorting/selection, ARIA snapshots added in Playwright 1.49/1.60, PG 18 `CREATE INDEX`) must drive tickets — not live Nuxt UI docs that may be newer than 4.7.1.

## Project impact

- Table work uses Nuxt UI `UTable` TanStack APIs (`v-model:sorting`, `v-model:row-selection`) in **manual/server** mode.
- Flyway next global version is **V23** (V22 exists under `testing/`). Do **not** use `CREATE INDEX CONCURRENTLY` in Flyway (cannot run in a transaction); follow existing `CREATE INDEX IF NOT EXISTS` (payment V3).
- No mocked Playwright tree. Race/out-of-order search is **out of scope** unless reproduced on live BFF.

## Test impact

| Claim | Test implication |
|---|---|
| `getByRole` first | POM locators: `columnheader`, `row`, `dialog`, `treeitem` |
| `locator.filter({ hasText })` | Row by merchant reference, not `nth` as primary |
| `toMatchAriaSnapshot` (page 1.60, locator 1.49) | Slideover / form / tree YAML |
| `waitForResponse` before click | Sort, save, import, kanban drop |
| Multi `storageState` + `browser.newContext` | RBAC + 412 two-user |
| PG btree default | Composite `(tenant_id, status, updated_at DESC)` |

## Sources

- [Playwright Locators](https://playwright.dev/docs/locators) — role/label first; `filter`; avoid CSS/XPath; `nth` last resort (strictness).
- [Playwright ARIA snapshots](https://playwright.dev/docs/aria-snapshots) — YAML roles/names/`checked`/`expanded`/`selected`; partial vs `/children: equal`.
- [Playwright Network](https://playwright.dev/docs/network) — `waitForResponse` for real traffic; `page.route` is mocking (forbidden in `tests-pom`).
- [Playwright Authentication](https://playwright.dev/docs/auth) — setup `storageState`; multiple roles; two contexts in one test; do not commit auth files.
- [Nuxt UI Table](https://ui.nuxt.com/docs/components/table) — sorting, filtering, pagination, row selection (TanStack). GitHub [nuxt/ui#5408](https://github.com/nuxt/ui/issues/5408): `update:row-selection` can lag `tableApi` — bind `v-model:row-selection`, do not read selection only from `tableApi` in the same tick.
- [PostgreSQL 18 CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html) — default btree; `IF NOT EXISTS`; `INCLUDE`; `CONCURRENTLY` not in a transaction block.
- [Spring Data REST paging](https://docs.spring.io/spring-data/rest/reference/paging-and-sorting.html) — `page`/`size`/`sort`; this lab already uses explicit DTO (`PaymentOrderListRequest`), not Spring Data REST. Keep whitelist `sort` regex like today.
- Playwright `toMatchAriaSnapshot` on `PageAssertions`: added in **v1.60** (microsoft/playwright.dev API mdx). Lab is 1.61.0 — API is in range.

## Uncertainty / follow-up

- Confirm `UEditor` / `UFileUpload` / `UTree` / `UStepper` / `UCalendar` / `UTimeline` **exports in `@nuxt/ui@4.7.1`** at implementation time (`node_modules/@nuxt/ui`). Live docs may describe 4.10+.
- Do not bump `@nuxt/ui` without a dedicated ticket.
- Flyway Redgate migrations URL returned 404 (2026-08-20); follow repo convention `V{n}__snake.sql` in `db/migration/<module>/`.
