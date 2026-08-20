---
name: playwright-merchant-360-infra
parent: playwright-merchant-360
last_updated: 2026-08-20
---

# 01 — Infrastruktura: PostgreSQL 18 · Flyway · Keycloak · stos

## Werdykt

| Warstwa | Zmiana w tym milestone? | Co |
|---|---|---|
| PostgreSQL 18 compose | **Nie** obrazu | Indeksy + ewentualne kolumny import job |
| Flyway | **Tak** | `db/migration/merchant/V23__…`, `payment/V24__…` (numery globalne — nie kolidować z V22 testing) |
| Keycloak realm JSON | **Nie** | 5 ról + worker users już są |
| `scripts/dev-stack.sh` | **Nie** (używać) | `--app` do POM; default do DX |
| Spring Security authorities | **Minimalnie** | Reuse `platform:merchants:*`; import = `create`; search/tree = `read` |
| Nuxt BFF | **Tak** | query params list, import multipart, search |

## A. Stos operatora (MUST)

Źródło: [docs/setup/run-stack-and-pom.md](../../../docs/setup/run-stack-and-pom.md), [scripts/dev-stack.sh](../../../scripts/dev-stack.sh).

```bash
cp infra/compose/.env.example infra/compose/.env
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
# worker: PLAYWRIGHT_MERCHANT_MANAGER_W{0-3}_PASSWORD (default merchant.manager.w{n})
scripts/dev-stack.sh --app
```

Issuer oczekiwany: `http://localhost:8081/realms/payment-quality`.  
Nuxt: `http://127.0.0.1:3000`. Spring: `http://127.0.0.1:8080/api/status`.

| Tryb | Kiedy w tym milestone |
|---|---|
| `--app` | Fale 1–5 POM/RA na prawdziwym deploy HTTP |
| default (host Spring/Nuxt) | implementacja z hot reload |
| `--full` | opcjonalnie Fala 6+ TLS — nie blocker |

`--app` **nie** łączyć z `--tls`/`--full`. Nie mockować Keycloak.

Live POM:

```bash
# z apps/frontend, stos już stoi
corepack pnpm exec playwright test --config playwright.pom.config.ts
# albo
corepack pnpm test:e2e:app
```

## B. Flyway — zasady laby

- Lokacje już w `application.yml`: `tenant,merchant,payment,shared,audit,checkoutlab,mirrorlab,rlslab,testing`.
- Wersja w **nazwie pliku jest globalna** (V1.2 merchant i V21 payment współdzielą oś). Następna wolna: **V23**.
- `hibernate.ddl-auto: validate` — migracja najpierw, potem encja.
- Wzorzec indeksów: [V3__add_payment_order_list_indexes.sql](../../../apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql) — `CREATE INDEX IF NOT EXISTS`.
- **Zakaz** `CREATE INDEX CONCURRENTLY` w Flyway (PG: nie w bloku transakcyjnym; [CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html)). Lab nie jest zerodowntime prod.

### B.1 V23 — merchant list (E1)

Plik: `apps/backend/src/main/resources/db/migration/merchant/V23__merchant_list_query_indexes.sql`

Cel: `GET /api/merchants` z `tenant_id + status + ORDER BY updated_at DESC` i search po reference/name.

```sql
-- V23__merchant_list_query_indexes.sql
-- List + tenant isolation. B-tree is PostgreSQL default (PG 18).

CREATE INDEX IF NOT EXISTS idx_merchants_tenant_status_updated
    ON merchants (tenant_id, status, updated_at DESC, merchant_id ASC);

CREATE INDEX IF NOT EXISTS idx_merchants_tenant_updated
    ON merchants (tenant_id, updated_at DESC, merchant_id ASC);

-- Case-insensitive exact/prefix search used by q=
CREATE INDEX IF NOT EXISTS idx_merchants_normalized_reference_lower
    ON merchants ((lower(normalized_reference)));

CREATE INDEX IF NOT EXISTS idx_merchants_display_name_lower
    ON merchants ((lower(display_name)));
```

Expression indexes muszą być immutable (PG docs) — `lower(text)` jest.

Istniejące: `idx_merchants_status`, `idx_merchants_created_at`, `idx_merchants_tenant_id`, `uk_merchants_normalized_reference`. Nie dropować.

**Nie** dodawać kolumny `revenue`. `updated_at` już jest.

### B.2 V24 — payment list sort/status (E1)

Plik: `db/migration/payment/V24__payment_order_list_amount_index.sql`

```sql
CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_amount
    ON payment_orders (merchant_id, amount_minor, payment_order_id);

CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_status_created
    ON payment_orders (merchant_id, status, created_at DESC, payment_order_id);
```

`idx_payment_orders_merchant_status` już jest (V3). Drugi indeks ma sens gdy `ORDER BY created_at` + filter status.

### B.3 V25 — import jobs (E5, nie wcześniej)

Nie w Fali 1. Preview może być bez tabeli (walidacja w pamięci + checksum). Commit = istniejący `INSERT merchants`.

Jeśli preview ma być wznawialny:

```sql
CREATE TABLE merchant_import_job (
    job_id           UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL REFERENCES tenants (tenant_id),
    created_by       VARCHAR(128) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    filename         VARCHAR(255) NOT NULL,
    total_rows       INTEGER NOT NULL DEFAULT 0,
    valid_rows       INTEGER NOT NULL DEFAULT 0,
    warning_rows     INTEGER NOT NULL DEFAULT 0,
    rejected_rows    INTEGER NOT NULL DEFAULT 0,
    error_report     JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ,
    CONSTRAINT chk_merchant_import_status
        CHECK (status IN ('PREVIEWED', 'COMMITTED', 'FAILED'))
);
```

JSONB jak audit V11. FK do `tenants`, **nie** do `payment_orders`.

### B.4 Unique i izolacja testów

`uk_merchants_normalized_reference` jest **globalny**. Import duplikatu → 409 jak create. POM: `uniqueMerchantReference`. Worker worlds `MERCHANT-W0`…`W3` nie używać jako ofiary importu (zderzenie z manager JWT).

## C. Kontrakt list (breaking — spec-first)

Dziś: `MerchantListResponse(List<MerchantResponse> merchants)`, `listFirstPage` limit 50.

Cel (jak payment list):

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Breaking dla BFF/Zod/`merchants.spec.ts` (`merchants?.length`). Fala 1 **musi** zaktualizować `BffClient` + overview count.

Payment `status` regex dziś tylko `CREATED` — UI kłamie. E1 rozszerza enum. Sort whitelist: `createdAt,(asc|desc)` i `amountMinor,(asc|desc)`.

Deterministyczna paginacja: zawsze tie-break `merchant_id` / `payment_order_id` ASC (wzorzec nodalpoint / lab payment `createdAt, merchant_id`).

## D. Keycloak / Security

- Brak nowych ról.
- `GET` list/search/tree: `platform:merchants:read` (tenant scoped przez `TenantResolver`).
- Import commit: `platform:merchants:create`.
- ETag mutacje: te same `update-status` / `update-risk-flag`.
- Platform `tenantId` query: tylko `tenantContext.isPlatformScoped()`; tenant JWT ignoruje obcy filter (pusta strona albo 400 — **zdecydować w E1-S2**, default: ignoruj filter, filtruj po JWT tenant).

## E. Testcontainers / Failsafe

Nowy indeks: `MerchantModuleTest` + `MerchantRestAssuredTest` list page. Nie JDBC z Node. Seed `dev,seed` musi nadal wstawać po V23.
