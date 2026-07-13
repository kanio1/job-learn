# Spec 02: IAM Roles and Keycloak Login

**Status:** ✅ DONE_VERIFIED — current execution status: see `status/specs/iam-roles-and-keycloak-login.md`
**Pliki źródłowe:** `.kiro/specs/iam-roles-and-keycloak-login/`  
**Zależność od:** `backend-authority-refactor` (✅ ukończony)

---

## Cel

Multi-role IAM model i realny Keycloak OIDC login flow dla dashboard.
Brownfield enhancement — rozszerzenie istniejącej aplikacji Nuxt + Spring Boot.

---

## Zakres

### Backend (brak zmian kodu Java)
- Keycloak realm: 5 composite roles, `tenant-id-mapper`, 5 test users, atrybuty legacy users
- Konwerter ról (`KeycloakRealmRoleConverter`) **nie jest zmieniany** — już ma explicit allowlist z `backend-authority-refactor`

### Frontend (Nuxt 4)
- OIDC login flow przez `nuxt-auth-utils`
- `SessionUser` (roles, tenantId, merchantId) w non-secure partition
- Nowy composable `useAuthorization` z mapą zdolności RBAC
- Role-aware sidebar + Overview + action controls
- Strona `/forbidden` (403) + Auth_Required_Redirect (401)
- Masking `Authorization` header w panelach HTTP

---

## Resolved Decisions (7)

| # | Kwestia | Decyzja |
|---|---|---|
| 1 | Inert authorities z composite names | **RESOLVED przez backend-authority-refactor** — composite names ignorowane, nie produkują inert authority |
| 2 | `platform.operator` continuity | Assign `PLATFORM_ADMIN` composite role addytywnie; zachowanie payment surfaces |
| 3 | SUPPORT_AGENT note-writing | DEFER do spec `payment-order-notes`; SUPPORT_AGENT = read + audit only |
| 4 | Decision 1 w design (converter) | Zaktualizowane — allowlist już dostarczony |
| 5 | tenant_id mapping | Per-user attribute `PLATFORM_TENANT`/`TENANT_ALPHA` (Keycloak GROUPS odroczone) |

---

## RBAC Matrix

| Composite Role | Granted Authorities |
|---|---|
| PLATFORM_ADMIN | merchants:create/read/update-status, payments:read/lifecycle/audit |
| TENANT_ADMIN | merchants:create/read/update-status, merchant-payments:read |
| MERCHANT_MANAGER | merchant-payments:create/read/lifecycle |
| SUPPORT_AGENT | merchants:read, platform-payments:read/audit |
| READ_ONLY_USER | merchants:read, platform-payments:read |

---

## Test Users

| Username | Role | tenant_id | merchant_id |
|---|---|---|---|
| `platform.admin` | PLATFORM_ADMIN | PLATFORM_TENANT | — |
| `tenant.admin` | TENANT_ADMIN | TENANT_ALPHA | — |
| `merchant.manager` | MERCHANT_MANAGER | TENANT_ALPHA | MERCHANT_ALPHA_001 |
| `support.agent` | SUPPORT_AGENT | PLATFORM_TENANT | — |
| `readonly.user` | READ_ONLY_USER | TENANT_ALPHA | — |

---

## Właściwości poprawności (PBT — Vitest + fast-check)

| Property | Waliduje |
|---|---|
| P1: Composite role → authorities set | Req 1.3–1.8, 2.2 |
| P2: Realm test-user invariants | Req 3.1–3.5, 4.6 |
| P3: Role→capability matches RBAC matrix | Req 2.5, 9.3 |
| P4: Role-aware rendering follows capabilities | Req 9.1–9.3, 9.7 |
| P5: 401-vs-403 reaction deterministic + disjoint | Req 7.2, 7.3, 8.1, 8.2 |
| P6: No bearer token in browser-exposed state | Req 5.3, 8.6, 11.2 |
| P7: Authorization header always masked | Req 11.1, 11.4 |
| P8: Redirect target round-trip | Req 7.4 |
| P9: Derived roles = composite-name intersection | Req 5.5 |
| P10: Forwarded headers exclude Authorization | Req 11.3 |

---

## Uwagi implementacyjne

- Zadanie 2 (Task 2.1) oznaczone jako **COMPLETED** — weryfikacja konwertera wykonana w ramach `backend-authority-refactor`
- Discrepancy A (inert authorities) z design.md zaktualizowane — nie dotyczy już tej spec
- `rbacMatrix` jako single source of truth — TS constant konsumowany przez `useAuthorization` i property test
- Playwright — brak plików w tej spec; testy Playwright to przyszłe lekcje

---

## Task summary (tasks.md)

Waves: 0 (realm + tenant mapper) → 1 (test users + legacy) → 2 (session derivation + useAuthorization + useAuthError) → 3 (wiring nav, Overview, gating actions + tests)

**Kluczowe pliki do edycji/stworzenia:**
- `infra/keycloak/realms/payment-quality-realm.json` (2 razy — Wave 0 i Wave 1)
- `server/routes/auth/keycloak.get.ts`
- `app/composables/useAuthorization.ts` + `useAuthError.ts`
- `app/layouts/dashboard.vue` + `app/pages/index.vue`
- `app/pages/forbidden.vue` + `app/pages/login.vue`
- `app/utils/rbacMatrix.ts`

**Opcjonalne property tests:** `useAuthorization.property.test.ts`, `useAuthError.property.test.ts`, realm-shape test
