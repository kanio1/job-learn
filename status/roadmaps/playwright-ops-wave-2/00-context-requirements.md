---
name: playwright-ops-wave-2-requirements
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-20
---

# Wymagania — Ops Interaction Lab (Wave 2)

## Cel biznesowy

Operator platformy, support agent i drugi writer muszą **obsłużyć kolejkę spraw, zobaczyć live feed, rozwiązać konflikt edycji merchanta, zapisać widok, zrobić bulk z częściowym błędem, nie zgubić brudnego formularza, potwierdzić duży refund PINem, skonfigurować politykę tenanta, wyszukać encję z klawiatury, przełączyć locale i przeglądać evidence** — na żywym Keycloak + BFF + Spring + Postgres.

Cel dydaktyczny: 12 klas problemów Playwright/TypeScript, nie 12 nowych „ładnych ekranów”.

## Persony (istniejący realm — composite roles bez zmian nazwy)

| Username | Composite role | Wave 2 |
|---|---|---|
| `platform.admin` | `PLATFORM_ADMIN` | pełny ops, inject, conflict writer A, policy, views |
| `platform.operator` | `PLATFORM_ADMIN` | **drugi subject** do 2×`BrowserContext` (E1, E3 412) |
| `tenant.admin` | `TENANT_ADMIN` | policy tenanta, search scoped, cases własnego tenanta |
| `merchant.manager` / `w{n}` | `MERCHANT_MANAGER` | brak registry; PIN na własnym refundzie; search tylko własne payments |
| `support.agent` | `SUPPORT_AGENT` | Kanban operate, notifications, search payments/merchants read |
| `support.agent.b` | `SUPPORT_AGENT` | **opcjonalny** drugi agent (bulk assign / assignee) — dodać w realm jeśli T08 tego wymaga |
| `readonly.user` | `READ_ONLY_USER` | feed/search/notifications read; zero mutacji |
| `merchant.denied` | brak authorities | 403 / UI deny |

Step-up PIN **nie** używa Keycloak OTP. Drugi writer **nie** może być tym samym `storageState` w dwóch kartach — JWT `sub` musi się różnić.

## FR

| ID | Wymaganie | Epic |
|---|---|---|
| FR-OPS-ETAG | GET/PATCH merchant (displayName, contactPhone, contactAddress): `ETag` `"v{n}"`, wymagany `If-Match`; stale **412**; brak **428** | E1 |
| FR-OPS-CONFLICT | Po 412 UI pokazuje `UAlert` + `UTabs` Your vs Latest; Discard mine / Reload latest; **nie** ciche 200 | E1 |
| FR-OPS-DIRTY | Brudny merchant form: NuxtLink, Back, reload/close → stay/discard; otwarcie guard **nie** wysyła PATCH | E2 |
| FR-OPS-CASE | `support_cases` NEW→IN_PROGRESS→WAITING→RESOLVED; tenant isolation; `@Version` | E3 |
| FR-OPS-KANBAN | Tab Work Queue; drop/menu Move; optimistic + rollback na 4xx; reload zachowuje kolumnę | E3 |
| FR-OPS-BULK | `POST .../bulk-assign` zwraca succeeded + failed[]; Retry failed wysyła **tylko** failed IDs | E4 |
| FR-OPS-PIN | Refund amountMinor > 100_000 → challenge 6-digit; 5 fail → 429; TTL; jednorazowy; hash w PG | E5 |
| FR-OPS-WS | Same-origin WS `/api/ops/feed`; eventId dedup; sort `occurredAt`; inject platform-only | E6 |
| FR-OPS-NOTIF | Inbox per subject; badge unread; mark read persist po reload | E7 |
| FR-OPS-VIEWS | Saved payment views: URL ↔ controls ↔ storage/API; inny user → absent | E8 |
| FR-OPS-SEARCH | `UDashboardSearch` grupy encji z żywego GET; last response wygrywa; RBAC ukrywa grupy | E9 |
| FR-OPS-POLICY | Tenant payment policy JSON + ETag; auto-capture OFF disables amount; BVA slider 0..100 | E10 |
| FR-OPS-I18N | EN/PL/SV na 2–3 ekranach + format kwot/dat; POM bez hardcode `"Save"` | E11 |
| FR-OPS-GALLERY | Evidence carousel next/prev/keyboard; broken/403; download istniejący | E12 |

## NFR

- Spring Modulith granice; public types tylko w root package. `support` nie importuje `payment.internal`. `ops` nie jest Type.OPEN.
- Flyway V31+; `ddl-auto: validate`. Indeksy btree `CREATE INDEX IF NOT EXISTS`. Zakaz `CONCURRENTLY`.
- Playwright 1.61 live POM; locatory `getByRole` → `getByLabel` → `getByTestId`.
- Zod przed renderem; BFF `backendApi`; token nigdy w przeglądarce (także WS).
- Isolacja: `uniqueMerchantReference(testInfo)` / worker `MERCHANT-W{n}` — nie psuć seed Alpha.
- Pin `@nuxt/ui` 4.7.1. Nowe zależności tylko: `spring-boot-starter-websocket` (E6) i `@nuxtjs/i18n` (E11) — każda wymaga zgody w tasku.

## Non-goals

- Customers, Deals, Activities jako encje.
- `page.route`, `routeWebSocket` (bez `connectToServer` = mock), mocked `tests/e2e`, HAR, MSW w `tests-pom`.
- HTTP 409 jako optimistic lock.
- Payment-status Kanban (to Merchant 360 E5).
- Pagination / CSV export / offline banner / checkout iframe jako **nowe** laby — już są.
- Kolejny Slideover/Modal lab bez nowej klasy problemu.
- Kafka, webhooks, outbox, PSP, PCI, WebAuthn, Keycloak OTP.
- Bump `@nuxt/ui` / TipTap / Unovis.
- `UPinInput separator` (4.9+).
- `CREATE INDEX CONCURRENTLY`.
- Nowe composite roles Keycloak (tylko realm roles w istniejących composites).
- Pełne tłumaczenie całego dashboardu.
- Trzy pełne projekty Playwright (en/pl/sv) na cały suite.
- Token w `localStorage`, Pinia, query string WS.

## Wave 1 (zaakceptowane — poza tym katalogiem)

```text
01 Payment Processing Simulator
02 Bulk Payment Import
03 Virtualized Payment History
04 Tenant Tree
05 Support Assignment Autocomplete
06 Payment Investigation Stepper
07 Audit/Support Rich Editor
08 Payment Context Menu
09 Settlement Date/Time
10 Resizable Event Inspector
```

Część z nich pokrywa się z Merchant 360 (tree, stepper, editor, import). Ten milestone ich **nie** reimplementuje.
