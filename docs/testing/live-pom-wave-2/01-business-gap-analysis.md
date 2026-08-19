# 01 — Luki biznesowe vs Wave 2 (as-built)

Wave 2 nie dodaje domeny płatniczej. Dodaje **sposób testowania** żywego dashboardu: gość, sesja cookie, oracle GET po BFF, dwa JWT, UI bez mocka sieci.

## As-built (framework)

| Element | Gdzie | Po co |
|---|---|---|
| `chromium-guest` | `playwright.pom.config.ts` | pusta sesja, bez setup Keycloak |
| `requireApi()` / opcjonalny `api` | `fixtures/index.ts` | guest nie ma BFF cookie |
| `BffClient` IPv4 | `api/bff-client.ts` | `127.0.0.1:3000`; `getMerchant`, `listNotes`, `createMerchant(..., tenantReference)` |
| Cookie / storage helpers | `utils/storage-safety.ts` | HttpOnly `nuxt-session`; JWT nie w `origins` storageState |
| Persistence helpers | `utils/persistence.ts` | `assertPersistedMerchant` / `assertPersistedOrder` = GET, nie SQL |
| Overlay handler | fixtures | `vite-plugin-checker-error-overlay` nie przejmuje klików |
| POM | `UserMenu`, `SupportPage`, `CommandPalette` | logout, IDOR, Ctrl+K |
| Learner | `tests-pom-learner` | config + README; **bez** skopiowanych speców |

## FR (dashboard live)

| ID | Wymaganie | Stan produktu |
|---|---|---|
| FR-W2-01 | Niezalogowany `/admin/*` → `/login?redirectTo=` | Done |
| FR-W2-02 | Sign out niszczy sesję **UI/BFF** (nie SSO) | Done |
| FR-W2-03 | Sesja HttpOnly; JWT nie w Web Storage | Done (cookie `nuxt-session`) |
| FR-W2-04 | Merchant unikalny persystuje (GET po create) | Done (API create **oraz** UI POST platform admin z polem `tenantReference`) |
| FR-W2-05 | Duplikat `reference` → 409 | Done (BFF) |
| FR-W2-06 | Pusty formularz: Zod, **brak** POST | Done |
| FR-W2-07 | Draft → Active → Suspended | Done (UI na merchancie z API) |
| FR-W2-08 | Command palette → Error Lab + drzewo ARIA | Done |
| FR-W2-09 | Notatka na żywym orderze Alpha | Partial: SUPPORT_AGENT notes POST **201** (`support-rbac.spec.ts`). Platform admin UI nadal 201\|403. |
| FR-W2-10 | Risk flag + badge listy | Partial: toggle UI; PATCH nadal 200 **albo** 403 (`merchant-risk.spec.ts`) |
| FR-W2-11 | CASH booking → CONFIRMED, bez hosted | Done (`chooseMode('CASH')`) |
| FR-W2-12 | Hosted Decline → fulfillment `CANCELLED` | Done (oracle fulfillment, nie query-only) |
| FR-W2-13 | Manager: brak nav Support; deep-link Beta → problem, brak tabeli | Done |
| FR-W2-14 | Error Lab 400 / 401 / 412 **oraz** 403/404/406/409/415/428/304 z żywego BFF | Done (admin vs manager; canary UI 401; 429 = BFF mock, **poza** suite) |
| FR-W2-15 | Admin JWT **nie** tworzy payment order | Done (`admin-bff.spec.ts` 403) |
| FR-W2-16 | Payment create + Idempotency-Key + replay 200/409 | Done (`payments-create.spec.ts`) |
| FR-W2-17 | Authorize/capture If-Match; stale 412 zostaje CREATED | Done |
| FR-W2-18 | Cancel ConfirmModal submit vs dismiss | Done |
| FR-W2-19 | Dual-context RBAC Alpha/Beta/Users | Done (`auth-rbac.spec.ts`) |
| FR-W2-20 | Idle lock 121s → Unlock `/login` | Done (`session-lab.spec.ts`; brak re-goto admin) |

## Luki

| ID | Luka | Typ | Wpływ na TC |
|---|---|---|---|
| GAP-W2-01 | ~~Formularz create merchant bez `tenantReference`~~ | **zamknięty** | Pole `create-merchant-tenant-reference` + E2E-024 / SCN-MER-13. API bez tenanta nadal 400 (API-003). |
| GAP-W2-02 | PLATFORM_ADMIN notes / risk-flag 201\|403 | realm | Support notes **201**. Admin notes i risk toggle nadal dual oracle (nie silent skip). |
| GAP-W2-03 | Hosted decline dokleja drugi `status` do `continueUrl` | produkt | Query może być tablicą (`success,failure`); asercja `toContainText('failure')`. |
| GAP-W2-04 | Overlay Vite potrafi przejąć click | test infra | `addLocatorHandler` w fixtures; nie `element.click()` (Vue `@click` nie wstaje). |
| GAP-W2-05 | `localhost` vs `::1` vs `127.0.0.1` | infra | Node → IPv4; browser → localhost (OIDC). |
| GAP-W2-06 | Dwa logouty (menu vs End OIDC) | test design | E2E-010 ≠ FR-OIDC. Kontrakt: [session-bff-oidc-contract](../session-bff-oidc-contract.md). |
| GAP-W2-07 | Brak TC rozmiaru cookie / `id_token` | test | designed SEC-005. |
| GAP-W2-08 | PAY_NO_RETURN / close-tab po Approve | **CPL**, nie Wave 2 | Nie dodawać tu E2E — [CPL GAP-02](../checkout-protocol-lab/01-business-gap-analysis.md), PW-E2E-043 designed. Lie return **jest** (E2E-061). |
| GAP-W2-09 | `payment_orders` vs CPL `continueUrl` | dokumentacja | Operator nie ma hosted return URL. Idempotencja create = E2E-091, nie UC-03 CPL. Mapa: [README](README.md). |
| GAP-W2-10 | Brak UC `tenant.admin` / ALPHA_002 / dual-control w Wave 2 07 | katalog | **Zamknięte.** POM: `tenant-scope.spec.ts` (UC-W2-20/21); dual-control: `payments-refund-dual-control.spec.ts`. |
| GAP-W2-11 | Katalogi 09 były host-only (`:8080`) | katalog | **Iteracja 2:** [10](10-full-stack-edge-flows.md) + UC-W2-23. Brak vhosta `psp.`; hosted na `app.` z `X-Frame-Options: DENY`. |

## Scenariusze biznesowe odblokowane

1. Gość vs zalogowany (redirect, nie 200 z pustą tabelą).
2. Rejestr merchantów: unikalność, walidacja, lifecycle, persistence.
3. Notatka operacyjna na orderze **innej roli** (manager tworzy, admin pisze).
4. Risk jako atrybut merchantu (nie seed Alpha).
5. Checkout: CASH vs ONLINE vs Decline vs lie-return (oracle fulfillment).
6. Support IDOR: ukryty nav **i** deep-link.
7. Problem+json z Error Lab (kontrakt BFF; canary UI 401; nie mock 429).
8. Gość: Users / payments / Error Lab / Checkout Lab → `redirectTo`; guest API 401; login wraca na Users.
9. Admin Support Beta vs manager IDOR; manager bez formularza notatek.
10. Tenant.admin: własny merchant 200, Beta **404**; manager vs `ALPHA_002` **403** ([09](09-core-domain-flows.md)).
11. Dual-control refund: merchant 409, checker ≠ maker ([UC-W2-22](07-istqb-decision-state-usecase.md)).
12. Ten sam kontrakt przez Caddy `api.` / `app.` / `auth.` ([10](10-full-stack-edge-flows.md), UC-W2-23).
