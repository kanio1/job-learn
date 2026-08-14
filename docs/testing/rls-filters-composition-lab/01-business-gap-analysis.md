# 01 — As-built inventory (product + test analyst)

Pytanie: **co jest w kodzie po Wave A** i które FR są świadomie docs-only.

## Werdykt

| Obszar | Produkt | Testy | Luka |
|---|---|---|---|
| Tenant isolation produkcji | JWT `tenant_id` → `TenantContext` → Java WHERE na merchants; payments przez `merchant_id` | `TenantIsolationIT`, API-tests | brak Postgres RLS na tych tabelach — **celowe** |
| RLS Lab | Modulith `rlslab`, V17 `FORCE ROW LEVEL SECURITY`, V18 rola `rls_lab_bypass` (BYPASSRLS), JdbcTemplate (nie `@Bean` DataSource) | RA + IT + POM dual role + mocked hub + FE flag-off project | — |
| Filtry listy płatności | UI już miał date/amount/status; brakowało testid i RA date/amount | mocked + POM + RA | status backend tylko `CREATED` (kontrakt) |
| Hard controls | USelect, ConfirmModal, badge `data-status` | POM + mocked badge | Users/audit pagination — analog w 05, bez speców |
| Race / ack / delivery | Istniejący If-Match, idempotency, checkout fulfillment, network abort | existing-pom/pw + dismiss `confirm-action-dismiss` | QUERY docs-only; TLS w Wave B |

## Powierzchnie (kod)

```text
Nuxt UI     /admin/rls-lab
            PaymentOrderListTable testids + BusinessStatusBadge data-status
Nitro BFF   /api/rls-lab/items, /items/:id, /compare
Spring      /api/rls-lab/*  (@Profile !prod, app.rls-lab.enabled)
Flyway      rlslab/V17__create_rls_lab.sql, V18__rls_lab_bypass_role.sql
DB roles    rls_lab_app LOGIN NOBYPASSRLS; rls_lab_bypass LOGIN BYPASSRLS
Seed        item a1 TENANT_ALPHA, item a2 PLACEHOLDER_TENANT_ID (SUSPENDED — tylko FK)
```

Flaga FE `NUXT_PUBLIC_RLS_LAB_ENABLED` default **on**.  
Flaga Spring `app.rls-lab.enabled` default **false**, `dev` **true**.

## FR status vs kod

| ID | Produkt | Test |
|---|---|---|
| FR-RLS-01 dedicated tables + FORCE RLS | V17 | existing-ra JDBC |
| FR-RLS-02 REST list current tenant | GET `/items` | existing-ra + existing-pom |
| FR-RLS-03 foreign UUID 404 | GET `/items/{id}` | existing-ra + existing-pom |
| FR-RLS-04 compare platform-only | GET `/compare`: matcher + `@PreAuthorize(platform:payments:read)` + `isPlatformScoped()` | existing-ra (dwa 403) + existing-pom |
| FR-RLS-05 flag off 404 | ConditionalOnProperty | existing-it |
| FR-FLT-01 date+status+reference | UI + list API | existing-pom + existing-ra |
| FR-FLT-02 min/max amount | UI testid + RA | existing-pw + existing-pom + existing-ra |
| FR-FLT-03 page-reset on Apply | `applyFilters` page 0 | existing-pw + existing-pom |
| FR-FLT-04 UPagination 1-based vs 0-based | wrapper testid | existing-pw |
| FR-CTL-01 USelect not native | labels | existing-pom |
| FR-CTL-02 badge data-status | BusinessStatusBadge | existing-pw + existing-pom |
| FR-CTL-03 ConfirmModal dismiss | no POST effect | existing-pom |
| FR-RACE-01 stale If-Match | już lifecycle | existing-pom |
| FR-RACE-02 lie ≠ fulfillment | checkout-lab POM | existing-pom |
| FR-RACE-03 BFF composition :3000 | waitForRequest | existing-pom |
| FR-TLS-LAB | `scripts/dev-stack.sh --tls` + Caddy/mkcert | existing-setup + existing-pom TLS + RA-030–033 (issuer, CORS, Caddy-shaped headers) |
| FR-QUERY | — | docs-only (anty: GET-with-body 403 CPL) |
| FR-SF7-VER / FR-RETRY | — | docs-only |

## GAP IDs

| ID | Luka | Warstwa | Prio |
|---|---|---|---|
| GAP-RFC-T01 | FE flag off: nav 0 + BFF 404 | PW project env | P1 existing-pw |
| GAP-RFC-T02 | List status inne niż CREATED | backend Pattern | docs (kontrakt) |

## Docs-only (Wave A leftovers; TLS moved to Wave B)

**FR-TLS-LAB (Wave B).** Katalog warstw i TC: [09-wave-b-stack-tls-catalog.md](09-wave-b-stack-tls-catalog.md). `scripts/dev-stack.sh --tls`, Caddy/mkcert, profil `tls-lab`, live POM TLS. HTTP compose pozostaje default.

**FR-QUERY.** RFC 10008 QUERY (safe+idempotent+body). Spring 7 nie ma `RequestMethod.QUERY`. Lab już uczy odwrotności: GET z body → 403 `get_with_body`. Nie hakować custom method.

**FR-SF7-VER / FR-RETRY.** Spring 7 API versioning i `@Retryable` na lab 503 — osobna fala. Network Lab już modeluje 503→200 w Nitro.
