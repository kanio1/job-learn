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
| FR-W2-02 | Sign out niszczy sesję UI | Done |
| FR-W2-03 | Sesja HttpOnly; JWT nie w Web Storage | Done (cookie `nuxt-session`) |
| FR-W2-04 | Merchant unikalny persystuje (GET po create) | Done (API create; UI POST admin bez tenanta → 400) |
| FR-W2-05 | Duplikat `reference` → 409 | Done (BFF) |
| FR-W2-06 | Pusty formularz: Zod, **brak** POST | Done |
| FR-W2-07 | Draft → Active → Suspended | Done (UI na merchancie z API) |
| FR-W2-08 | Command palette → Error Lab + drzewo ARIA | Done |
| FR-W2-09 | Notatka na żywym orderze Alpha | Partial: UI jest; realm może nie mieć `platform:payments:notes:*` → 403 |
| FR-W2-10 | Risk flag + badge listy | Partial: analogiczny 403 `update-risk-flag` |
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
| GAP-W2-01 | Formularz create merchant **bez** `tenantReference` | produkt | UI POST platform admin = 400 `MissingTenantReferenceException`. Persist TC używa API. |
| GAP-W2-02 | PLATFORM_ADMIN bez fine-grained notes / risk-flag | realm | TC tolerują 201\|200 **albo** 403 — to drift, nie silent skip. |
| GAP-W2-03 | Hosted decline dokleja drugi `status` do `continueUrl` | produkt | Query może być tablicą (`success,failure`); asercja `toContainText('failure')`. |
| GAP-W2-04 | Overlay Vite potrafi przejąć click | test infra | `addLocatorHandler` w fixtures; nie `element.click()` (Vue `@click` nie wstaje). |
| GAP-W2-05 | `localhost` vs `::1` vs `127.0.0.1` | infra | Node → IPv4; browser → localhost (OIDC). |

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
