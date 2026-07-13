# Spec 05: User Management

**Status:** ✅ COMPLETE_WITH_OPTIONAL_GAPS — current execution status: see `status/specs/user-management.md`
**Pliki źródłowe:** `.kiro/specs/user-management/`  
**Roadmap position:** SPEC #3 (Playwright/SDET roadmap §17)

---

## ⚠️ Twarde zależności (HARD GATE — Wave 0)

Implementacji **NIE WOLNO** zaczynać dopóki nie są zaimplementowane:
1. `iam-roles-and-keycloak-login` (role kompozytowe, claim tenant_id, rbacMatrix/useAuthorization)
2. `tenant-model-and-isolation` (moduł tenant, TenantResolver/TenantContext, masked-404/403)

Wave 0 w tasks.md to weryfikacyjna bramka — blokuje całą resztę.

---

## Cel

User management jako **fasada nad Keycloak Admin REST API** (decyzja użytkownika).
Keycloak = single source of truth dla tożsamości. Brak lokalnej tabeli `app_user`,
brak migracji Flyway, brak encji JPA.

---

## Kluczowe decyzje

| # | Decyzja | Uzasadnienie |
|---|---|---|
| D1 | Keycloak Admin API façade (nie lokalny app_user) | single source of truth, brak dual-write/sync |
| D2 | Tylko 5 ról kompozytowych przypisywalnych | spójność z modelem one-role-per-identity |
| OQ1 | Nowy moduł Modulith `iam` (`lab.paymentquality.iam`) | bounded context IAM, miejsce na przyszłe rozszerzenia |
| Admin client | Thin RestClient (nie keycloak-admin-client lib) | kontrola status-code, zero nowych zależności, wartość edukacyjna |
| OQ2 | enable/disable w `PATCH` (nie osobne endpointy) | jeden spójny kontrakt |
| Concurrency | re-fetch-before-write; brak ETag; last-write-wins (udokumentowane) | Keycloak Admin API nie ma ETagów |
| Test double | WireMock (primary) + Testcontainers-Keycloak (integration) | szybkie property testy + realne wiring |

---

## Nowe authorities (cross-spec touch point)

`platform:users:read|create|update|assign-roles` + `tenant:users:read|create|update|assign-roles`
- PLATFORM_ADMIN → platform:users:* ; TENANT_ADMIN → tenant:users:*
- MERCHANT_MANAGER / SUPPORT_AGENT / READ_ONLY_USER → brak (nav hidden + 403)

**Touch points (artefakty innych specs):**
1. Realm `payment-quality-realm.json` — +8 ról + agregacja do composite + service-account client
2. `KeycloakRealmRoleConverter` allowlist — +8 wpisów (rule bez zmian)
3. `Authorities` catalog — +8 stałych
4. `rbacMatrix.ts` — +canManageUsers, +canAssignRoles

---

## Endpointy (façade)

| Metoda | Ścieżka | Authority | Tenant scope |
|---|---|---|---|
| GET | /api/users | *:users:read | TENANT_ADMIN → własny tenant |
| POST | /api/users | *:users:create | platform picks / tenant auto |
| GET | /api/users/{id} | *:users:read | cross-tenant → masked 404 |
| PATCH | /api/users/{id} | *:users:update | cross-tenant → 403 |
| POST | /api/users/{id}/roles | *:users:assign-roles | cross-tenant → 403 |

---

## Właściwości poprawności (PBT)

| Property | Tool | Opis |
|---|---|---|
| P1 | jqwik | tenant-scoped list zwraca tylko own-tenant + filtry |
| P2 | jqwik | cross-tenant read→404 masked, write→403 (deterministic, disjoint) |
| P3 | jqwik | tylko 5 composite roles przypisywalnych (else 400) |
| P4 | jqwik | create tenant assignment (platform picks; tenant ignoruje body) |
| P5 | fast-check | rbacMatrix canManageUsers/canAssignRoles ⟺ PLATFORM_ADMIN/TENANT_ADMIN |
| P6 | jqwik | admin token + bearer + hasła nigdy w browser-exposed surface |

---

## Task waves (tasks.md — 11 wave'ów)

| Wave | Zakres |
|---|---|
| 0 | **Prerequisite gate** (weryfikacja Spec #1 + #2) — HARD GATE |
| 1 | Cross-spec extensions (realm + Authorities + converter allowlist) |
| 2 | iam module foundation (package-info, AdminProperties, CompositeRole, ManagedUser, exceptions) + DTOs |
| 3 | TokenProvider + UserMapper + ExceptionHandler |
| 4 | KeycloakAdminClient |
| 5 | UserManagementService |
| 6 | UserManagementController |
| 7 | Backend tests (unit/slice/IamModuleTest/IT/smoke/P1-P4,P6) |
| 8 | Frontend foundation (schema, useUsersApi, proxy, rbacMatrix) |
| 9 | Frontend UI (page, UserTable, CreateUserForm, EditUserDrawer, RoleAssignmentSelect, nav) |
| 10 | Frontend tests (P5, component states) |

NON-optional: `IamModuleTest` (6.6), Testcontainers-Keycloak integration (6.7). Reszta testów opcjonalna `*`.

---

## Następne specs w roadmapie

| # | Spec | Status |
|---|---|---|
| 4 | audit-log-dashboard | nierozpoczęta — user-management actions to naturalne audit sources |
| 5 | deterministic-seed-and-test-isolation | nierozpoczęta |
| później | file-import-export, bulk-actions, notifications-and-settings, responsive-readiness | nierozpoczęte |

---

## UI/UX Review Gate

Analiza UI/UX (7 matryc) została wykonana przed tym spec przez bramkę
`docs/ai/modern-web-guidance-spec-review-gate.md`. Wynik: 7 matryc (traceability,
role journey, screen inventory, Nuxt UI components, UI states, a11y/testability,
Playwright learning value) — wszystkie wbudowane w requirements/design.
