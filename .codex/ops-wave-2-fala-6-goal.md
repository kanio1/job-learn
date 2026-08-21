Continue Ops Wave 2 Fala 6: PW-OPS-T13 T14 T15. Stop before T16.

Read overlay .codex/ops-wave-2-slice.md (Next is PW-OPS-T13). Canonical: status/roadmaps/playwright-ops-wave-2/task-board.md, epics/E8-saved-views.md, E9-global-entity-search.md, 01-infra-postgres-keycloak-stack.md (B.5 V35), docs/testing/ops-wave-2-interaction-lab/03-playwright-e2e-catalog.md, 04-playwright-api-http.md, 05-traceability.md.

This goal GRANTS V35 only: iam/V35__create_user_saved_views.sql (user_saved_views, owner = JWT sub). Do not add @nuxtjs/i18n, V36, payment_policy, UCarousel.

Locked from Fala 5 (do not regress):
- Flyway: V31 contact, V32 support_cases, V33 refund_challenges, V34 ops notifications. Gap V26–V30. No CONCURRENTLY. V35 only after V34 is present.
- Modules support + ops already exist. Saved views live in the existing iam module (not ops, not a new crm module). Do not import payment.internal from iam.
- WS feed + notification center stay as-is. Pin @nuxt/ui 4.7.1. No UUser. No page.route / route.fulfill / routeWebSocket / HAR / MSW in tests-pom.
- Optimistic lock = 412 + If-Match. 409 = duplicate / illegal transition / self-approve. Token never in browser JS, localStorage, sessionStorage, URL, or WS query.
- No new Keycloak composite roles.

T13 — Saved views (E8-S1 then E8-S2):
- Phase a (localStorage, before V35): merchant.manager saves “Large EUR captured” (Status=CAPTURED, Currency=EUR, Amount>10000, sort newest, columns). Reload restores filters. Key `pq.payment-views.{subject}` from sanitized auth store user id, never the access token. Quota 20; overflow overwrites oldest. Logout may leave origin localStorage (phase a OK).
- PW-OPS-E2E-140 save → reload → filters restored. PW-OPS-E2E-141 storage-safety: no access_token / JWT / Bearer in localStorage or sessionStorage (reuse tests-pom/utils/storage-safety.ts).
- Phase b (V35 API): POST/GET /api/users/me/payment-views; PUT/DELETE /{id}; POST /{id}/default. UNIQUE (owner_subject, resource, name). Partial unique default per (owner_subject, resource). CHECK resource IN ('PAYMENT_ORDERS'). filters JSONB whitelist = existing payment list query params only: status, currency, minAmount, maxAmount, fromDate, toDate, clientOrderReference, sort. page/size are NOT stored in a view. Unknown filter key → 400. Server wins after login (merge: API over localStorage).
- RA-OPS-140 POST 201 + GET list 1; 141 second subject GET empty; 142 second default flips first is_default false; 143 unknown filter 400. PW-OPS-API-050 CRUD through BFF cookie. PW-OPS-E2E-142 save API → logout/login restore. PW-OPS-E2E-143 other user (platform.operator) view absent.

T14 — URL ↔ view ↔ columns (E8-S3):
- Apply view writes the same query string the payment list already syncs. Back restores the view. Clear filters ≠ delete view. Default star = POST …/default.
- Column profile: UCheckboxGroup; hiding a column is UI-only (not a security filter — RBAC columns stay M360). PW-OPS-E2E-144 apply → URL query match; 145 Back restores view; 146 set default star; 147 uncheck column (e.g. Created by) → header absent, field still present on API GET.
- POM: SavedViewsComponent + PaymentFiltersComponent (apply / clear / saveAs / open / setDefault). Shared view DTO in shared/types if both Vue and Nitro need it. Zod before render.

T15 — Global search (E9), skip core:
- M360 T17 is already live: GET /api/search, UDashboardSearch in dashboard.vue, command-palette.spec (PW-M360-E2E-110…112, PW-M360-API-051). Do NOT build a second Ctrl+K. Keep existing command-palette.spec green.
- This slice = RBAC groups + last-wins + payments group if missing. Debounce = existing searchDelay + AbortController; last in-flight wins; no page.route. Hard limit on q.
- PW-OPS-E2E-200/201 only if payment keyboard path is not already covered; otherwise do not duplicate M360-110/111. PW-OPS-E2E-202 last waitForResponse q= wins (first merchant absent). PW-OPS-SEC-040 manager: Merchants group absent (canReadMerchants=false), own payment present. PW-OPS-SEC-041 readonly: Create merchant action absent. PW-OPS-E2E-203 denied user 403/empty does not crash. PW-OPS-API-060 only if you add a new search contract; do not re-assert M360-API-051 1:1.

TDD:
- T13a E2E-140/141 green BEFORE V35 / RA-140.
- Then RA-OPS-140…143 Testcontainers red/green BEFORE T13b UI logout/login and before T14.
- Then PW-OPS-API-050, then E2E-142/143, then T14 E2E-144…147.
- T15 last; do not start T15 until T13b RA is green. Do not start T16.

Skills: tdd, spring-modulith, nuxt-frontend, playwright-pom, rest-api-test-design, rest-api-security-oauth-testing, postgres18-data-architecture-and-risk.

Done when T13 T14 T15 are DONE, overlay Next is PW-OPS-T16, RA-OPS-140…143 + live POM E2E-140…147 and search 202/SEC-040/041 (plus 200/201 only if added) pass, command-palette.spec still passes, existing ops-feed / ops-notifications / support-kanban / payments-pin still pass.

Nie startuj T16 w tym samym goalu — V36 / payment_policy / rule configurator to Fala 7. Nie startuj T18 (@nuxtjs/i18n). No commit unless I ask.
