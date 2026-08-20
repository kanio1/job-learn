---
name: epic-e1-list-contracts
parent: playwright-merchant-360
epic: E1
tasks: [PW-M360-T01, PW-M360-T02, PW-M360-T03, PW-M360-T04, PW-M360-T05]
last_updated: 2026-08-20
---

# Epic E1 — Kontrakty list (Flyway + Spring + BFF)

**Cel produktowy:** serwerowa paginacja/sort/filter merchantów; uczciwy filter statusu płatności.  
**Cel dydaktyczny:** TDD REST Assured → BFF Zod; indeksy pod query; breaking DTO.

**Połączenia:** `MerchantController.list`, `PaymentOrderListRequest`, `MerchantService.LIST_LIMIT=50`.

Bez E1 tabela UI będzie sortować 50 wierszy w RAM i kłamać przy `AUTHORIZED`.

---

## Story E1-S1 — Indeksy merchant list

**Task:** `PW-M360-T01` · P0 · PG-M360-01

### Jako / chcę / aby

Jako operator platformy chcę szybki filtr `ACTIVE` w moim tenancie posortowany po `updated_at`, aby registry skalował się poza 50 wierszy.

### Business case

`BC-M360-01` — Registry ERP nie ładuje całej tabeli; list query ma plan na btree `(tenant_id, status, updated_at DESC)`.

### Use case

`UC-M360-01` — Platform admin filtruje tenant Alpha + status ACTIVE, strona 2.

### Acceptance criteria

- [ ] Flyway `merchant/V23__merchant_list_query_indexes.sql` jak w [01-infra](../01-infra-postgres-keycloak-stack.md).
- [ ] JPA validate zielone; `ModulithArchitectureTest`.
- [ ] Brak `CONCURRENTLY`.

### Test IDs

| ID | Warstwa | Asercja |
|---|---|---|
| RA-M360-001 | Failsafe/IT | po migracji `pg_indexes` zawiera `idx_merchants_tenant_status_updated` (albo smoke list 200) |
| RA-M360-002 | Surefire | `MerchantModuleTest` nadal wstaje |

SQL (szkic w 01-infra). Tie-break `merchant_id` w `ORDER BY` aplikacji.

---

## Story E1-S2 — Pageable GET `/api/merchants`

**Task:** `PW-M360-T02` · P0

### Jako / chcę / aby

Jako tenant admin chcę stronę 20 merchantów z `q` i `status`, aby nie scrollować pełnego registry.

### Business case

`BC-M360-02` — Breaking: `{ merchants: [] }` → `{ content, page, size, totalElements, totalPages }`. Overview i POM muszą iść w tym samym PR.

### Use case

`UC-M360-02` — GET z cookie BFF / JWT: `?status=ACTIVE&q=acme&page=0&size=20&sort=updatedAt,desc`.

### Acceptance criteria

- [ ] Query: `q` (ILIKE reference lub displayName), `status` enum, `riskFlagged`, `tenantId` tylko platform, `page≥0`, `1≤size≤100`, `sort` regex `createdAt|updatedAt|displayName|status,(asc|desc)`.
- [ ] Tenant JWT: zawsze `tenant_id` z tokena; ignoruj obcy `tenantId`.
- [ ] Platform bez `tenantId`: wszystkie tenanty (jak dziś list).
- [ ] Domyślnie `sort=createdAt,desc` + `merchant_id` ASC.
- [ ] 400 problem+json na zły sort/status/size.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-M360-010 | REST Assured | page 0 size 20, `content.size ≤ 20`, `totalElements` |
| RA-M360-011 | REST Assured | `status=ACTIVE` — same ACTIVE |
| RA-M360-012 | REST Assured | `q` unikalny reference — 1 wiersz |
| RA-M360-013 | REST Assured | `sort=displayName,asc` monotonic |
| RA-M360-014 | REST Assured | `size=101` → 400 |
| RA-M360-015 | REST Assured | `sort=revenue,desc` → 400 |
| RA-M360-016 | REST Assured | tenant.admin nie widzi Beta |
| RA-M360-017 | REST Assured | platform `tenantId=TENANT_ALPHA` tylko Alpha |
| RA-M360-018 | REST Assured | bez JWT → 401; readonly może GET 200 |
| PW-M360-API-001 | Playwright REST | `BffClient` GET list `content` 200 cookie session |
| PW-M360-API-002 | Playwright REST | zły sort przez BFF → 400 problem Zod |

TDD: czerwony RA zanim kontroler.

---

## Story E1-S3 — Payment list status + amount sort

**Task:** `PW-M360-T03` · P0

### Jako / chcę / aby

Jako merchant manager chcę listę `AUTHORIZED` i sort po kwocie, aby Kanban i tabela nie kłamały.

### Business case

`BC-M360-03` — Dziś `@Pattern(regexp = "CREATED")` vs UI 6 statusów = defect kontraktu.

### Use case

`UC-M360-03` — `GET .../payment-orders?status=CAPTURED&sort=amountMinor,desc`.

### Acceptance criteria

- [ ] `status` = `CREATED\|AUTHORIZED\|CAPTURED\|CANCELLED\|EXPIRED\|REFUNDED`.
- [ ] `sort` = `createdAt,(asc\|desc)` **lub** `amountMinor,(asc\|desc)`.
- [ ] V24 index amount.
- [ ] Tenant/merchant ownership bez zmian.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-M360-020 | REST Assured | `status=AUTHORIZED` 200 |
| RA-M360-021 | REST Assured | `status=NOPE` 400 |
| RA-M360-022 | REST Assured | `sort=amountMinor,asc` monotonic |
| RA-M360-023 | REST Assured | manager obcy merchant 403/404 (istniejący kontrakt) |
| RA-M360-024 | REST Assured | CREATED nadal działa (regresja filtrów Wave A) |
| RA-M360-025 | REST Assured | `fromDate`/`minAmount` kompozycja z nowym status |
| RA-M360-026 | REST Assured | V24: list CAPTURED nie 500 |
| PW-M360-API-003 | Playwright REST | BFF `status=CAPTURED` 200 |

---

## Story E1-S4 — BFF + Zod + overview

**Tasks:** `PW-M360-T04`, `PW-M360-T05` · P0

### Jako / chcę / aby

Jako frontend chcę ten sam kształt list w Zod i overview count z `totalElements` (nie `merchants.length` pierwszej strony).

### Use case

`UC-M360-04` — Overview: liczba merchantów = `totalElements`, nie `content.length`.

### Acceptance criteria

- [ ] `server/api/merchants.get.ts` query string → Spring.
- [ ] `merchantListResponseSchema` z `content`.
- [ ] `BffClient.listMerchants` + overview spec.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-API-004 | Playwright REST | list schema `safeParse` |
| PW-M360-E2E-001 | Live POM | overview Summary pokazuje `totalElements` z GET list |

Nie: client-side filter jako wyrocznia.
