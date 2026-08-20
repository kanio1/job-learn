---
name: playwright-merchant-360-requirements
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-20
---

# Wymagania — Merchant 360

## Cel biznesowy

Operator platformy i tenant admin muszą **znaleźć, odfiltrować, posortować i masowo obsłużyć** merchantów oraz zobaczyć **Merchant 360** (status, risk, płatności, audit) bez fałszywego CRM. Merchant manager operuje **pipeline płatności** (tabela + później Kanban) na własnym merchancie.

## Persony (istniejący realm — bez nowych ról)

| Username | Composite role | Merchant 360 |
|---|---|---|
| `platform.admin` | `PLATFORM_ADMIN` | pełny registry, tenant column, import, bulk, ETag |
| `tenant.admin` | `TENANT_ADMIN` | registry tylko `TENANT_ALPHA` |
| `merchant.manager` / `merchant.manager.w{n}` | `MERCHANT_MANAGER` | **brak** registry (`canReadMerchants=false`); tabela/Kanban płatności |
| `support.agent` | `SUPPORT_AGENT` | read merchants + audit; bez create/import/activate |
| `readonly.user` | `READ_ONLY_USER` | read; zero mutacji UI i API |
| `merchant.denied` | brak authorities | 403 / UI deny |

## FR

| ID | Wymaganie | Epic |
|---|---|---|
| FR-M360-LIST | `GET /api/merchants` zwraca stronę (`content`, `page`, `size`, `totalElements`, `totalPages`) | E1 |
| FR-M360-Q | Query: `q`, `status` (`DRAFT\|ACTIVE\|SUSPENDED`), `tenantId` (tylko platform), `riskFlagged`, `page`, `size≤100`, `sort` whitelist | E1 |
| FR-M360-PAY-FILTER | `GET .../payment-orders` `status` = pełny enum lifecycle; `sort` += `amountMinor,(asc\|desc)` | E1 |
| FR-M360-TABLE | `UTable` server-side: sort header, multi-filter, pagination, row selection, bulk activate z per-row error, empty/loading | E2 |
| FR-M360-URL | Filtry/sort/page w query string; `Back` przywraca ten sam widok | E2 |
| FR-M360-SLIDE | Klik wiersza → `USlideover` 360 (info, risk, payments preview, notes, audit) bez zmiany route; Escape zamyka; focus return | E3 |
| FR-M360-FORM | Create/edit tylko na polach backendu (reference, displayName, tenantReference dla platform). Decision table na istniejących boundach Zod | E3 |
| FR-M360-RBAC | Kolumny i akcje wg `rbacMatrix`; ukrycie ≠ security — bliźniak 403 API | E4 |
| FR-M360-ETAG | GET merchant + activate/suspend/risk-flag: `ETag`, wymagany `If-Match`; stale **412**; brak **428** | E4 |
| FR-M360-IMPORT | CSV preview + commit; authority `platform:merchants:create`; duplikat po `normalized_reference` | E5 |
| FR-M360-KANBAN | Widok board statusów płatności; drop/menu → istniejący POST lifecycle + If-Match; rollback UI przy 4xx | E5 |
| FR-M360-TREE | Read-model Tenant → Merchant (lazy); `role=tree` | E6 |
| FR-M360-SEARCH | `UDashboardSearch` grupy encji z żywego GET search (limit); last `waitForResponse` wygrywa | E6 |
| FR-M360-CHART | Wykres/legend z `GET .../summary` `byStatus`/`byCurrency` — zero zmyślonych KPI | E6 |
| FR-M360-CAL | Kalendarz `expiresAt` / dual-control due — dane z API, `page.clock` w teście | E7 |
| FR-M360-TL | `UTimeline` w 360 z audit + payment history | E7 |
| FR-M360-STEP | Stepper create-payment (amount → currency → reference → review) na istniejącym POST | E7 |
| FR-M360-EDITGRID | Inline edit `displayName` po ETag | E7 |
| FR-M360-EDITOR | TipTap notes tylko jeśli komponent jest w 4.7.1; inaczej `UTextarea` zostaje | E7 |

## NFR

- Spring Modulith granice; public types tylko w root package.
- Flyway V23+; `ddl-auto: validate`.
- Indeksy btree dopasowane do `WHERE`/`ORDER BY` list (PG 18 default btree).
- Playwright 1.61 live POM; locatory `getByRole` → `getByLabel` → `getByTestId`.
- Zod przed renderem; BFF `backendApi`; token nigdy w przeglądarce.
- Isolacja: `uniqueMerchantReference(testInfo)` / worker `MERCHANT-W{n}` — nie psuć seed Alpha ~104 orders.

## Non-goals

- Customers, Deals, Activities jako encje.
- `page.route`, mocked `tests/e2e`, HAR.
- HTTP 409 jako optimistic lock.
- Kolumna Revenue bez agregatu z `payment_orders` (Fala 1: **nie dodawać**).
- Bump Nuxt UI / TipTap / Unovis bez biletu.
- Kafka, PSP, PCI, i18n, mobile matrix.
- `CREATE INDEX CONCURRENTLY` w migracji Flyway.
- Nowe composite roles Keycloak.
- Zmiana `uk_merchants_normalized_reference` na unique per tenant (osobny ADR, nie ten milestone).
