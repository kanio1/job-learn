# Spec 04: Tenant Model and Isolation

**Status:** ✅ DONE_VERIFIED (documented deviations) — current execution status: see `status/specs/tenant-model-and-isolation.md`
**Pliki źródłowe:** `.kiro/specs/tenant-model-and-isolation/`  
**Zależność od:** `iam-roles-and-keycloak-login` (introduces `tenant_id` claim)

---

## Cel

Zamknięcie security gap z `iam-roles-and-keycloak-login` (Req 4.7: `tenant_id` był informacyjny).
Enterprise-grade model tenanta jako pełna encja DB — wzorzec Stripe/Adyen/Braintree.
Cel edukacyjny: Flyway FK migrations, multi-tenant authorization, isolation testing.

---

## Zakres

| Obszar | Opis |
|---|---|
| Nowy moduł Modulith `tenant` | PUBLIC API: `TenantResolver`, `TenantContext`, `TenantReference`, `TenantResolutionException` |
| Flyway migration (tenant) | `db/migration/tenant/V1__create_tenants.sql` — tabela + seed 3 tenantów |
| Flyway migration (merchant) | `db/migration/merchant/V2__add_tenant_to_merchants.sql` — FK + backfill |
| Merchant module changes | `Merchant` entity + `JpaMerchantRepository` + `MerchantService` + `MerchantController` + `MerchantExceptionHandler` |
| Additive contract | `tenantReference` field na `POST /api/merchants` (optional dla tenant-scoped, required dla platform-scoped) |
| `?tenantId=` filter | Na `GET /api/merchants` dla platform-scoped principals |

**Brak zmian w payment module.**  
**Brak zmian frontend.**

---

## 7 Resolved Decisions

| # | Kwestia | Decyzja |
|---|---|---|
| D1 | JWT claim → DB record | Natural key `tenant_reference` (nie UUID w tokenie) |
| D2 | Platform vs tenant scope classification | Data-driven: `tenant_type = PLATFORM` → platform-scoped |
| D3 | Reads: 403 vs 404 | Masked-not-found split: reads → 404, writes → 403 |
| D4 | Suspended tenant semantics | Własny SUSPENDED → 403 dla tenant-scoped; platform-scoped retains access |
| D5 | Platform-scoped create merchant | Explicit `tenantReference` wymagany; brak/nieznany → 400 |
| D6 | Flyway placement + Modulith ownership | Nowy moduł `tenant` + `db/migration/tenant` location |
| D7 | Realm literals | Seed `tenant_reference` byte-identical z realm (TENANT_ALPHA, PLATFORM_TENANT, PLACEHOLDER_TENANT_ID) |

---

## Seed tenantów (Flyway V1)

| tenant_reference | tenant_type | name |
|---|---|---|
| `PLATFORM_TENANT` | PLATFORM | Platform Tenant |
| `TENANT_ALPHA` | STANDARD | Alpha Tenant |
| `PLACEHOLDER_TENANT_ID` | STANDARD | Placeholder Tenant (backfill) |

---

## Tenant Isolation Matrix

| Operacja | Tenant-scoped (własny) | Tenant-scoped (inny) | Platform-scoped |
|---|---|---|---|
| List merchants | Tylko własny tenant | (filtrowane automatycznie) | Wszystkie |
| GET merchant by id | 200 | 404 masked | 200 dowolny |
| Create merchant | Auto-assign do własnego | (nie dotyczy) | Explicit `tenantReference` wymagany |
| Activate/Suspend | Działa | 403 | Działa dla dowolnego |
| Read payment orders | Istniejące merchant-scope | 404 masked | Cross-merchant (istniejące) |
| PO write/lifecycle | Istniejące merchant-scope | 403 | Cross-merchant (istniejące) |

---

## Właściwości poprawności (PBT — jqwik)

| Property | Opis | Waliduje |
|---|---|---|
| P1 | Cross-tenant read → 404 masked | Req 5.3, 5.4, 10.2 |
| P2 | Classification deterministic by `tenant_type` | Req 4.1, 4.4 |
| P3 | Create-merchant tenant assignment rule | Req 6.1–6.4 |
| P4 | Transitive PO isolation przez merchant | Req 8.2, 8.3 |
| P5 | Suspended own-tenant → 403; platform retains | Req 3.5, 3.6 |
| P6 | `tenant_reference` natural-key bijection | Req 3.1, 3.4 |

---

## Task summary (tasks.md — 7 waves)

| Wave | Zadania | Opis |
|---|---|---|
| W0 | 1.1–1.4 | Tenant module foundation: package-info, public types, internal types, V1 migration |
| W1 | 3.1–3.3 | V2 merchant migration, Merchant entity, TestJwtSupport |
| W2 | 5.1–5.4 | Repository queries, exceptions, CreateMerchantRequest, MerchantService |
| W3 | 7.1–7.2 | MerchantController + MerchantExceptionHandler |
| W4 | 9.1–9.4 | Unit tests + @DataJpaTest + @WebMvcTest slice |
| W5 | 11.1–11.3 | TenantModuleTest + MerchantModuleTest verify + TenantIsolationIT (9 scenariuszy) |
| W6 | 13.1–13.6 | P1–P6 property-based tests (optional *) |

Checkpointy po każdej wave (zadania 2, 4, 6, 8, 10, 12, 14).

---

## Uwagi bezpieczeństwa

- **Non-disclosure:** wszystkie 403 z tenant-boundary code paths używają `detail = "Access denied"` — bez tenant_reference obcego tenanta
- **JWT claim integrity:** signed by Keycloak; existing `AuthorizedPartyValidator` + Nimbus decoder waliduje token przed `TenantResolverService`
- **Natural vs surrogate key separation:** JWT niesie `tenant_reference` (natural key), nigdy surrogate UUID — brak leaky abstraction

---

## Follow-up specs (przyszłe)

- `tenant-management` — CRUD tenantów przez API + UI
- `payment-order-notes` — authority `platform:payments:note`, odblokowuje SUPPORT_AGENT write
- `user-management` — CRUD użytkowników Keycloak
