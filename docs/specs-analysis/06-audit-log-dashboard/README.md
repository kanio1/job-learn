# Spec 06: Audit Log Dashboard

**Status:** ✅ COMPLETE_WITH_OPTIONAL_GAPS — current execution status: see `status/specs/audit-log-dashboard.md`
**Pliki źródłowe:** `.kiro/specs/audit-log-dashboard/`  
**Roadmap position:** SPEC #4 (Playwright/SDET roadmap §17)

---

## ⚠️ Twarde zależności (HARD GATE — Wave 0)

1. `iam-roles-and-keycloak-login` (role, converter allowlist, claim tenant_id)
2. `tenant-model-and-isolation` (moduł tenant, TenantResolver, masked-404)
3. `user-management` (#3) — **źródło zdarzeń** (create/update/role-assign emitują audit events); opóźnialne — merchant+payment wystarczą na start

---

## Cel

Globalny, przeszukiwalny audit log (append-only) — kto/co/gdzie/kiedy/z jakim wynikiem.
Read-only przez REST API + dashboard. **Pierwsze użycie Spring Modulith events w projekcie.**

---

## Kluczowe decyzje

| # | Decyzja | Uzasadnienie |
|---|---|---|
| A | Spring Modulith events (@ApplicationModuleListener) | decoupling emiterów od audytu; eventual, after-commit, async |
| B | Nowa authority `platform:audit:read` + `tenant:audit:read` | odrębna od payment-scoped `platform:payments:audit` |
| C | Realna tabela `audit_event` + Flyway migration | first-class persistent data, indeksy, append-only (PostgreSQL 18) |
| D | Native `UInput type=date` | lepsza a11y, stabilne dla Playwright `page.clock` |
| OQ1 | Nowy moduł `audit` (`lab.paymentquality.audit`) | bounded context |
| OQ2 | Nowa capability `canViewAuditLog` (≠ canReadAudit) | nie mylić z payment audit |
| Event contract | Generyczny `AuditableActionOccurred` w `shared.events` | jeden listener, emitery decoupled |
| Durable log | spring-modulith-events-jpa (FLAG: zgoda na zależność) | at-least-once delivery; bez tego listener failure = utrata wpisu |

---

## audit_event (Flyway, PostgreSQL 18)

Kolumny: id (UUID PK), occurred_at (timestamptz), actor_subject, actor_display, action, target_type, target_id, tenant_id, correlation_id, outcome (CHECK SUCCESS/DENIED/FAILED). Indeksy: occurred_at, tenant_id, actor_subject, action. Brak FK (tenant_id jako natural-key string). Append-only.

---

## Endpointy (read-only — audyt pisany przez listener)

| Metoda | Ścieżka | Authority | Tenant scope |
|---|---|---|---|
| GET | /api/audit | *:audit:read | TENANT_ADMIN → własny; cross-tenant filtrowane |
| GET | /api/audit/{id} | *:audit:read | cross-tenant → masked 404 (= nonexistent) |

Brak create/update/delete. Konwencje: problem+json, X-Correlation-ID, Vary, 403/404/405/406.

---

## Audytowane akcje (event sources)

| Moduł | action |
|---|---|
| iam/user-management (#3) | USER_CREATED, USER_UPDATED, USER_ROLES_ASSIGNED |
| merchant | MERCHANT_CREATED, MERCHANT_ACTIVATED, MERCHANT_SUSPENDED |
| payment | PAYMENT_AUTHORIZED, PAYMENT_CAPTURED, PAYMENT_CANCELLED, PAYMENT_REFUNDED |

---

## RBAC matrix

| Operacja | PLATFORM_ADMIN | SUPPORT_AGENT | TENANT_ADMIN | MERCHANT_MANAGER | READ_ONLY_USER |
|---|---|---|---|---|---|
| List audit | all tenants | all tenants | own tenant | 403 | 403 |
| Get entry (visible) | 200 | 200 | 200 | 403 | 403 |
| Get entry (other tenant) | 200 | 200 | 404 masked | 403 | 403 |

---

## Właściwości poprawności (PBT)

| P | Tool | Opis |
|---|---|---|
| P1 | jqwik | tenant-scoped list tylko own-tenant; platform all; occurred_at DESC |
| P2 | jqwik | single read scope-correct; cross-tenant = nonexistent (masked, identyczne) |
| P3 | jqwik | date-range inclusive na granicach |
| P4 | jqwik | każdy wpis: non-null, valid outcome, no sensitive data |
| P5 | fast-check | canViewAuditLog ⟺ PLATFORM_ADMIN/SUPPORT_AGENT/TENANT_ADMIN |
| P6 | jqwik | jeden event → dokładnie jeden wpis, pola zachowane |

---

## Task waves (tasks.md — 11 zadań / 6 wykonawczych fal)

| Wave | Zakres |
|---|---|
| 0 | **Prerequisite gate** (HARD GATE) |
| 1 | Cross-spec auth extensions (realm + Authorities + converter) |
| 2 | Shared event contract + durable event-log infra (FLAG: zgoda na zależność) |
| 3 | audit module foundation (package-info, Flyway audit_event, entity, repo) |
| 4 | listener + service + controller + DTOs + handler |
| 5 | emitter event publication (merchant, payment, iam #3-deferred) |
| 6 | backend tests (unit/@DataJpaTest/@WebMvcTest/@ApplicationModuleTest/AuditModuleTest/IT/smoke/P1-P4,P6) |
| 7 | frontend foundation (schema, useAuditApi, proxy, rbacMatrix) |
| 8 | frontend UI (page, AuditTable, AuditFilters, AuditEntryDrawer, nav, 6 states) |
| 9 | frontend tests (P5, component states) |

NON-optional: @DataJpaTest, @WebMvcTest, @ApplicationModuleTest, AuditModuleTest, security-matrix IT, migration-smoke IT.

---

## Ryzyka / uwagi

- **Pierwsze Spring Modulith events** — brak istniejących eventów do wzorowania
- **Zależność spring-modulith-events-jpa wymaga zgody** (project rule: no deps without approval)
- **Bez durable event log** listener failure = cicha utrata wpisu audytu
- Tokeny/hasła/PAN/CVV nigdy w audit_event ani w odpowiedzi
- Brak plików Playwright (konceptualne scenariusze: audit-trail verification, date filtering + page.clock, deep link, tenant-scoped)
