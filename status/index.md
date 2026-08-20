---
name: index
last_updated: 2026-08-20
---

## Session log

- **Ops Wave 2 milestone-PW (2026-08-20):** **DESIGNED_NOT_STARTED**. Drugi katalog obok Merchant 360: [status/roadmaps/playwright-ops-wave-2/](roadmaps/playwright-ops-wave-2/) (epiki E0–E12, taski `PW-OPS-T00`…`T22`) + mapa testów [docs/testing/ops-wave-2-interaction-lab/](../docs/testing/ops-wave-2-interaction-lab/) (`BC-OPS-*`, `UC-OPS-*`, `PW-OPS-E2E-*` / `API-*` / `SEC-*`, `RA-OPS-*`). Research: [.codex/research/ops-wave-2-versioned-stack.md](../.codex/research/ops-wave-2-versioned-stack.md) (Firecrawl + Context7). Zero kodu aplikacji. Live stack (`scripts/dev-stack.sh --app`); zakaz `page.route` **i** `routeWebSocket`. Flyway **V31+** (M360 rezerwuje V23–V30). `.kiro/**` unchanged.

- **Merchant 360 milestone-PW (2026-08-20):** **DESIGNED_NOT_STARTED**. Katalog wykonawczy [status/roadmaps/playwright-merchant-360/](roadmaps/playwright-merchant-360/) (epiki E0–E7, taski `PW-M360-T00`…`T20`) + mapa testów [docs/testing/merchant-360-erp-lab/](../docs/testing/merchant-360-erp-lab/) (`BC-M360-*`, `UC-M360-*`, `BF-M360-*`, `EP-`/`BVA-`/`DT-`/`ST-`/`MR-`/`EG-M360-*`, `AT-M360-*`, `PW-M360-E2E-*` / `API-*` / `SEC-*`, `RA-M360-*`; plan plików POM w `09-agent-tests-pom-plan.md`). Research wersji: [.codex/research/merchant-360-versioned-stack.md](../.codex/research/merchant-360-versioned-stack.md) (Firecrawl; Context7 bez klucza). Zero kodu aplikacji. Live stack only (`scripts/dev-stack.sh --app`); zakaz `page.route`. Sibling: [playwright-ops-wave-2](roadmaps/playwright-ops-wave-2/). `.kiro/**` unchanged.

- **Session BFF/OIDC catalog (2026-08-15):** Test/use-case docs aligned with two logout paths (BFF Sign out vs Session lab `end_session`), cookie 4 KB / no `id_token`, and policy JSON vs live `Secure`. Canonical: [docs/testing/session-bff-oidc-contract.md](../docs/testing/session-bff-oidc-contract.md). No Playwright spec added.

- **Playwright real-stack learning (2026-08-15):** **IMPLEMENTED**. Fale 1–4: BFF HEAD + live `APIRequestContext`; evidence category/bytes/dropzone; clipboard; offline banner; expiration sweep; OIDC end_session; badge `title`; bank ConfirmActionModal; axe; async export-jobs 202+Location+worker; dual-control refund na `payment_orders`. Catalog: [status/roadmaps/playwright-real-stack-learning/](roadmaps/playwright-real-stack-learning/). `.kiro/**` unchanged. Wave 6 OpenAPI snapshot/CI remains the prior queue item.

- **Wave 5 OpenAPI springdoc (2026-08-15):** **IMPLEMENTED**. `springdoc-openapi-starter-webmvc-api` 3.1.0; authenticated `GET /v3/api-docs`; labs/test excluded; UI off; prod `api-docs.enabled=false`. Backend `OpenApiRestAssuredTest` **4/4**, `ModulithArchitectureTest` **1/1**, Failsafe `OpenApiDisabledIT` **1/1**. Live api-tests `OpenApiContractSpec` not claimed (needs `BACKEND_IMAGE`). Catalog impact table: existing UC-W4 / POM / lab HTTP oracles unchanged. Snapshot + CI = Wave 6. Catalog: [docs/testing/wave-5-openapi-springdoc/README.md](../docs/testing/wave-5-openapi-springdoc/README.md).

- **Wave 5 OpenAPI catalog (2026-08-15):** Catalog written, then implemented the same day (see above).

- **Wave 4 REST contract gates (2026-08-15):** **DONE_VERIFIED** for `REST-MULTIPART-01`; **DECISION_RECORDED** for `REST-OPENAPI-DRIFT-01`. Black-box `PaymentEvidenceMultipartContractSpec` (AT-MP-01…10) Failsafe **12/12**. Review findings closed: truncated multipart is `400` `validation` via `GlobalExceptionHandler` (`MultipartException`, no new domain `error`); GET list uses `ResponseSpecs.sensitive()` + BOLA `404`; seed reset IT covers evidence FK. OpenAPI tooling stays Wave 5 ([docs/architecture/openapi-ownership.md](../docs/architecture/openapi-ownership.md)). Catalog: [docs/testing/wave-4-rest-contract-gates/README.md](../docs/testing/wave-4-rest-contract-gates/README.md). Offline api-tests Surefire **81/81**.


- **Operator runbook (2026-08-14):** [docs/setup/run-stack-and-pom.md](../docs/setup/run-stack-and-pom.md) — HTTP `--app` for POM/TS, HTTPS `--full` for Caddy; Caddy is not broken on `--app` (it is not started). No HTTP-Caddy flag: `--full` is the prod-shaped stack.

- **Podman HTTP/HTTPS stack (2026-08-14):** HTTP compose `--app` verified: issuer `http://localhost:8081`, Nuxt `http://127.0.0.1:3000`, Spring `/api/status`, Nitro `/__oidc` splits browser authorize vs compose-network token/JWKS. Keycloak `start` + `hostname-backchannel-dynamic`. Mode switch fail-closed via `keycloak-issuer-oracle.sh`. Caddy snippets: security headers, encode, 5MB body limit, short lab HSTS. Prod ACME: `infra/caddy/Caddyfile.prod.example`. Realm adds `http://127.0.0.1:3000`. podman-compose/pasta: stop/start overlay containers so host ports bind.

- **Wave 3 findings review fix (2026-08-14):** **IMPLEMENTED**. Merchant 404 uses origin `problem()` (`type`/`title`/`status`/`detail`/`error`); Error Lab BFF forwards Spring body (no `coerceProblemJson`). Trigger 403 fail-closed: 2xx create → 503 `lab_unavailable`. 304 does not auto-create. 401 canary = click + `waitForResponse` + problem card. Live POM `NUXT_TYPECHECK=false`; overlay detect via `count()`. `--full` OIDC discovery is loopback-only; Caddy `/__oidc*` → 404; session password fail-fast (≥32). CA = STK-007 / `mkcert -install` (Linux Chromium may need NSS/`certutil`). Verified: `pnpm typecheck`; `MerchantRestAssuredTest` + merchant/payment security tests; Spring/BFF 401 problem+json via curl. HTTP POM Error Lab/Support needs a Keycloak issuer on `http://localhost:8081` (use `--app` or a fresh HTTP `dev-stack.sh`). `REST-MULTIPART-01` is Wave 4; OpenAPI **tooling** is Wave 5.

- **Wave 3 TLS depth + Live POM Wave 2 + compose HTTPS (2026-08-14):** **IMPLEMENTED** (TLS overlay + `--full` compose; HTTP live POM Wave 3 IDs). If-Match stale oracle uses `"v99"` (malformed `"stale-etag"` is 400). `--full`: Caddy → Spring/Nuxt images; BFF talks Keycloak HTTP inside the compose network. HTTP POM stays on host hybrid. Catalog: `docs/testing/wave-3-compose-tls-pom/`.

- **Wave B full local stack + TLS overlay (2026-08-13):** **DONE_VERIFIED**. `scripts/dev-stack.sh` raises compose Postgres+Keycloak, Spring `dev,seed`, and Nuxt. Live POM Wave A HTTP **10/10** (`payments-filters`, `payments-hard-controls`, `rls-lab`). REST-SSL-PROXY-01 part 1: hostile `X-Forwarded-*` does not rewrite relative `Location` (`PaymentOrderForwardedHeadersRestAssuredTest`). TLS overlay: Caddy on host **8443** (rootless Podman cannot bind 443), mkcert gitignored certs, Spring `tls-lab`, additive HTTPS redirect URIs, live POM TLS **4/4**. GAP-RFC-T01: `playwright.rls-flag-off.config.ts`. Seed now clears/reinserts `rls_lab_item` so `dev,seed` works after V17. QUERY / API versioning / `@Retryable` / `REST-MULTIPART-01` / OpenAPI remain outside this wave.

- **RLS / filters Wave A review findings (2026-08-13):** **IMPLEMENTED**. Date RA/POM oracles use UTC day bounds (`LocalDate.now(ZoneOffset.UTC)` / `toISOString().slice(0, 10)`). RLS lab no longer honors client-settable `app.rls_bypass` GUC — Flyway V18 adds `rls_lab_bypass` (BYPASSRLS); platform JDBC uses a non-`JdbcTemplate` holder so Boot's primary template stays. Catalog PW-RFC-E2E-022 is `existing-pom` (CREATED × PLN). POM asserts BFF list `page` 0/absent after Apply from `?page=1`; payment list badge is `payment-status-badge` on the filtered row. Probe UI asserts `error=not_found`; ConfirmModal dismiss asserts no POST `/cancel`. Targeted Surefire 24/24 (`RlsLabRestAssuredTest` 7, `PaymentOrderListRestAssuredTest` 16, Modulith 1). Failsafe `RlsLabEndpoints*IT` 4/4. Frontend typecheck green. Mocked Playwright `payment-filters` + `rls-lab` 6/6. Live POM closed in Wave B.

- **RLS / filters / composition Wave A (2026-08-13):** **IMPLEMENTED**. Flag-gated modulith `rlslab` (Flyway V17 `FORCE ROW LEVEL SECURITY`, role `rls_lab_app`, JdbcTemplate — not a second primary DataSource), Nuxt `/admin/rls-lab` + BFF, payment list `data-testid`s and status badge `data-status`. Catalog: `docs/testing/rls-filters-composition-lab/`. TLS / HTTP QUERY / Spring API versioning / `@Retryable` remain docs-only. `payment_orders` and realm JSON untouched.

- **PayU / bank mirror labs implementation (2026-08-13):** **IMPLEMENTED**. Waves T01–T29: Nuxt `NUXT_PUBLIC_MIRROR_LAB_ENABLED` hub (`/admin/mirror-lab`), Session/Visual/Network labs + idle overlay, Nitro CSRF/devices/503/HAR/CORS; checkoutlab GET-with-body 403, `?lang=` Location, refund HMAC notify, hosted expiry testid, same-origin widget, `trusted_merchant`; new modulith module `mirrorlab` + Flyway V15 (disputes, evidence, maker-checker, consent, statements PDF/CSV, step-up header). Realm JSON untouched; `payment_orders` untouched. Targeted Surefire green (Modulith, CPL GET/create/oauth/refund, Mirror Lab RA/IT). Mocked Playwright visual/network/hub green with new goldens. Live POM specs added but not claimed `DONE_VERIFIED` (need Keycloak+dev stack). `REST-MULTIPART-01` remains open.

- **PayU / bank mirror labs design (2026-08-13):** **DESIGNED_NOT_STARTED**. Created `status/roadmaps/browser-session-visual-network-lab/` (requirements, Keycloak/BFF cookie infra, learning map, task board `MRL-T01`…`T29`, epics E0–E6) and test maps in `docs/testing/payu-bank-mirror-labs/`. Educational mirrors for browser session/cookies/`storageState`, visual comparison, network interception, CPL PayU extensions, and bank-like desktop flows. No application code changed. Implementation requires an explicit wave request. `REST-MULTIPART-01` remains open; E5-S3 (disputes evidence) is a candidate closer.

- **Checkout Protocol Lab backlog design (2026-08-09):** **DESIGNED_NOT_STARTED**. Created `status/roadmaps/checkout-protocol-lab/` with requirements, Postgres/Keycloak/Security infra decisions, learning map, task board (`CPL-T01`…), and detailed epics E0–E7 (stories, AC, snippets, connections). No application code changed. Intended training surface for redirect+notify; proposed path to satisfy Wave 2B `REST-REDIRECT-01` stop gate when implementation is explicitly requested.

- **Assurance Closure Wave 2A (2026-07-13):** **DONE_VERIFIED**. `QA-HARDEN-01` now has focused acceptance-level evidence for all **11/11** required hardening/polish requirements. Ten implementations were already correct; one confirmed presentation defect was fixed (`ProblemDetailsCard`'s `Field Errors` label now uses the same `w-28` alignment as every other term). Two initial test assumptions were corrected as `TEST_DESIGN_DEFECT`; neither caused a production change. Frontend validation is green: typecheck, **58 files / 594 unit tests**, and standard Chromium **82/82**. Optional deterministic-seed task 5.1 is now `DONE_VERIFIED`: `RealmAlignmentPropertyTest` exhaustively verifies the five actual per-role realm users against deterministic tenants/merchants in two independent runs; compile/test-compile and filtered verify are green (**Surefire 469 total / 464 passed / 5 skipped; Failsafe 46/46**). Required plan verification is now **296/296 (100.00%)**; optional verified/deviation coverage is **44/73**, with the remaining 29 optional tasks explicitly skipped and one separate conditional Stage 4 checkpoint deferred. `REST-ADVANCED` is designed for Wave 2B only; no Wave 2B implementation was started.

- **Assurance Closure Wave 1 — validation-02 closure (2026-07-13):** **DONE_VERIFIED**. The one authorised current-source complete live command ran all five projects and passed **7/7** without retries, failures, flakes, or skips: real Keycloak setup for `platform.admin`/`merchant.manager`, same-route cross-role and foreign-tenant denial, two-worker allocation, BFF idempotency replay, and BFF conditional GET/304. Before that run, the multi-role focused project exposed one locator-only defect: generic `getByRole('alert')` also matched loading skeletons. It now filters for the actual permission-denial text and passed 3/3; no behaviour, authority, count, timeout, or retry was altered. Standard mocked Chromium also passed **82/82**. `PW-AUTH-01`/`F-A2`, `PW-DATA-01`/`F-A4`, `PW-IDEM-01`, and `PW-304-01` are **DONE_VERIFIED**; `PW-HEAD-01` remains **SUPERSEDED_BY_VERIFIED_SOLUTION**. Next executable queue: `QA-HARDEN-01`, then `SEED-PROP-01`, then `REST-ADVANCED`.

- **Assurance Closure Wave 1 — final validation (2026-07-13):** **IMPLEMENTED_UNVERIFIED**. `ASSURANCE-CLOSURE-W1-VALIDATION-01` used its single newly authorised complete live command once: 7 discovered / 7 executed, 6 passed and 1 failed, with no retry. The sole failure was `live-multi-role`'s brittle absolute `101 order(s)` assertion racing a correct parallel idempotency creation in the same Alpha tenant; it was corrected to require a non-empty tenant-visible result and the exact project then passed 3/3. Focused auth, two-worker isolation, idempotency replay, and conditional GET/304 are green, but the package statuses remain unverified until a newly authorised complete live run proves the corrected source. A new controller dependency also required one `@WebMvcTest` mock; its exact test is 6/6 and the filtered backend verify exits 0. Next executable item: `ASSURANCE-CLOSURE-W1-VALIDATION-02`.

- **Assurance Closure Wave 1 — live packages checkpoint (2026-07-13):** **PARTIAL_UNVERIFIED**. Real Keycloak `platform.admin`/`merchant.manager` setup projects are green with credentials supplied only from environment; the latter maps to `TENANT_ALPHA` and its real seeded `MERCHANT_ALPHA_001` natural reference. The backend now resolves that claim through a public merchant eligibility lookup, while preserving UUID claims and existing 403/404 behaviour. A dedicated live config and worker-owned retained-data fixture exist. Two permitted complete live runs uncovered and corrected PKCE hostname, natural-reference authorization, a first-page test assumption, and nanosecond-versus-microsecond replay precision; no third complete run may be used to turn those corrections into closure evidence. `PW-AUTH-01`, `PW-DATA-01`, `PW-IDEM-01`, and `PW-304-01` remain open. `PW-HEAD-01` is **SUPERSEDED_BY_VERIFIED_SOLUTION** by fresh `apps/api-tests` Failsafe `HttpMethodSemanticsContractSpec` HEAD evidence (72/72). Next executable item: `ASSURANCE-CLOSURE-W1-VALIDATION-01`, requiring an explicit new live-run allowance.

- **Assurance Closure Wave 1 — VAL-API-01 (2026-07-13):** **DONE_VERIFIED**. A fresh `apps/api-tests` baseline found one contract drift: its standalone merchant DTO omitted the backend's current `riskFlagged` field. `VAL-API-01A` added only that field. Compile/test-compile are green; Surefire is 79/79 and Failsafe is 72/72 with zero failures, errors, or skips. Next package: natural merchant-reference ownership alignment required for real `merchant.manager` Playwright coverage.

- **TD-4 identifier-reference closure (2026-07-13, Codex CLI):** **DONE_WITH_DEVIATION**. A complete inventory found that `ddbff980-460a-4eec-ae6b-f004d743fac8` is a four-way immutable Kiro metadata collision, not an alias with a legitimate owner. `status/index.md` now defines each spec slug/path as the sole canonical current identity; the shared UUID is prohibited for new status or implementation references. The collision remains only in read-only `.kiro/**`; no code or configuration changed. No executable task remains in the tracked queue.

- **TD-3 documentation closure (2026-07-13, Codex CLI):** **DONE_VERIFIED**. Corrected six stale current headers under `docs/specs-analysis/**`, its summary table, and stale-header annotations in four status ledgers; the canonical status ledgers remain the current source of truth. Historical TD-2 test counts and package states are preserved only in dated session/commit evidence. The subsequent TD-4 record closes the formerly next package; `.kiro/**` and `.codex/**` remain unchanged.

- **Continuation validation session (2026-07-13, Codex CLI, HEAD `95e35c9`):** Environment preflight confirmed localhost bind and Podman/Testcontainers access. Removed only the generated tracked `apps/backend/.jqwik-database` artifact. TD-2E-1 is **DONE_VERIFIED**: exact duplicate-feedback E2E, full merchant-create spec (6 Chromium tests), frontend typecheck/unit suite, and `MerchantRestAssuredTest` (5/5) all passed; the verified contract is `409 application/problem+json` with `type`, `title`, `status`, `detail`, `error`, and conditional `correlationId` (no `instance`). TD-2F is **DONE_VERIFIED**: domain transition tests 17/17 and lifecycle E2E passed; only `DRAFT -> ACTIVE` and `ACTIVE -> SUSPENDED` are supported. TD-2D is **DONE_VERIFIED**: the corrected assertion validates the decoded URL semantics (`/login`, `redirectTo=/admin/merchants`) plus visible login control and absent merchant data. TD-2G is **DONE_VERIFIED**: before correction 6/10 manual polling repetitions selected `/history` or `/evidence` due to a prefix predicate; exact GET/path matching made the repeat-each=10 run 20/20 green. TD-2 closure has two consecutive full Chromium runs green (82/82 each); the independent final frontend run is also 82/82. Backend compile/test-compile are green; filtered verify is BUILD SUCCESS (Surefire 445 total, 440 passed, 5 skipped; Failsafe 46/46). jqwik converter properties all report 100 checks. `.kiro/**` and `.codex/**` unchanged; no commit or push.

- **Session 7 (2026-07-12, Codex CLI):** Worked `TD-2E` only. Preserved the inherited TD-2B/TD-2C source, tests, status records, and Draft visual snapshot. Fresh Playwright reproduction confirmed that four `merchant-create.spec.ts` tests stopped before form submission because they used exact `Create` locators, while the intentional submit button contract is role `button`, visible text `Create`, `aria-label`/accessible name `Create merchant`. Updated only six exact semantic locators to `getByRole('button', { name: 'Create merchant', exact: true })`; no production, helper, unit, or snapshot change. Three of the four tests now pass. The fourth exposed a separate, previously masked stale duplicate-error mock (`{ error, message }`) that does not satisfy the current Problem Details client contract, so it renders `Failed to create merchant. Please try again.` rather than the asserted duplicate message. TD-2E is **PARTIAL**, not complete. Fresh frontend typecheck and 46-file/546-test unit suite are green; Chromium is **78 passed / 4 failed** (82 executed). Next executable package: `TD-2E-1` — align the duplicate-merchant Playwright mock with the existing Problem Details error contract. `.kiro/**` and `.codex/**` unchanged.
- **Session 6 (2026-07-12, Codex CLI):** Completed `TD-2C` only. Inherited and preserved all TD-2B source, tests, status records, and the Draft visual snapshot. Fresh reproduction confirmed three stale expectations: the merchant page correctly renders `LoadingState`, structured 503 responses correctly render `ProblemDetailsCard` with `Retry`, and an empty payment summary correctly renders `No currency data.` plus the payment-list empty state. Added minimal `role=status`/accessible loading name and `role=alert`/accessible error name, strengthened shared component unit assertions, and rewrote only `merchant-feedback.spec.ts` and `payment-orders-panel.spec.ts`. Validation: typecheck green; units 46 files / 546 tests green; Playwright discovery 101 tests / 31 files; affected specs 6/6 green; full Chromium **76 passed / 6 failed**. TD-2C is **DONE_VERIFIED**. The four stale `Create` accessible-name assertions are now `TD-2E = CONFIRMED, OPEN` and are the next executable package. `.kiro/**` and `.codex/**` unchanged.
- **Session 5 (2026-07-12, Codex CLI, handoff from interrupted Claude Code CLI):** Completed `TD-2B` only. Audited 11 partial frontend changes, confirmed the backend wire chain `MerchantStatus.DRAFT` → `MerchantMapper.toResponse().status()` → JSON `"DRAFT"`, and found no active legacy `PENDING` contract or OpenAPI surface. Kept the valid partial replacement, exported the production Zod boundary for direct testing, derived the table type from `MerchantResponse`, replaced the visual snapshot, and removed unrelated TD-2F commentary added during the handoff. Validation: typecheck green; unit suite 46 files / 546 tests green; Playwright list 101 tests / 31 files; targeted changed-spec run 16 passed / 5 failed, with all `DRAFT` contract assertions passing and the five failures classified outside TD-2B; full Chromium 72 passed / 10 failed. TD-2B is **DONE_VERIFIED**; TD-2 remains **IN_PROGRESS**. Next executable package: `TD-2C` — align stale merchant loading/error and payment-empty-state UI-copy expectations. `.kiro/**` and `.codex/**` unchanged.
- **Session 4 (2026-07-12, commit `c6de61f` → this session):** Worked `TD-2A` (the largest confirmed cluster within `TD-2`'s 21 Playwright chromium failures). Reproduced the 21-failure baseline fresh, built a failure ledger, and — via controlled experiments (serial vs. parallel reruns, a temporary diagnostic probe spec, and direct inspection of `useAuthorization.ts`/`rbacMatrix.ts`/the real Keycloak realm JSON) — confirmed **two** distinct test-infrastructure root causes, both fixed: (1) the default 5000ms Playwright assertion timeout is too tight for this project's `nuxt dev` webServer (~4.1–4.6s real render latency, worse under 16-worker contention) — fixed via `apps/frontend/playwright.config.ts`'s new `expect.timeout: 15_000`; (2) the shared `mockAuthenticatedSession()` test helper never included a `roles` array, so `useAuthorization()`'s fail-closed default hid every RBAC-gated button, even though the real Keycloak realm assigns `platform.operator` the `PLATFORM_ADMIN` role — fixed via `apps/frontend/tests/e2e/merchant-support.ts` (default `roles: ['PLATFORM_ADMIN']`) and one explicit override in `auth-deny.spec.ts` to preserve its intentional no-authority test. Result: 21→11 remaining failures (10 fixed), 0 new failures, frontend typecheck/532 unit tests still green. TD-2A **RESOLVED**; TD-2 overall **IN_PROGRESS** — 11 failures remain across 3 further confirmed-independent root causes (TD-2B PENDING/DRAFT contract bug, 7 tests, likely production fix + a UX decision; TD-2C stale UI copy, 3 tests; TD-2D stale auth-deny redirect assumption, 1 test), none fixed this session per the one-work-package rule. See `status/technical-debt/current-baseline.md`, `status/roadmaps/playwright-phase3-roadmap.md`, `status/evidence/latest-validation.md`.
- **Session 3 (2026-07-12, commit `c6de61f`):** Worked `TD-5` (stale merchant-count assertions in `SeedProfileStartupIT`/`TestEndpointsEnabledIT`). Root cause confirmed via `git log`/`git show 8861f84`: `Fixtures.merchants()` was deliberately extended from 3 to 4 merchants (`MERCHANT_SUSPENDED_DEMO`, MVP Phase 1 roadmap task `SEED-MVP-001`, commit `8861f84`) **after** these two Failsafe integration tests were written (commit `1ade297`, predates `8861f84`); `FixturesTest` was correctly updated at the time, but these two IT files never were. Fixed 4 stale `isEqualTo(3)`→`isEqualTo(4)` assertions and strengthened `SeedProfileStartupIT.deterministicMerchantUuidsArePresent` to also check the 4th merchant's UUID. TD-5 **RESOLVED**. Full filtered `./mvnw verify` is now **BUILD SUCCESS** for the first time across all 3 sessions (Surefire 463/463, Failsafe 46/46, 0 failures). Reverted the `status/specs/deterministic-seed-and-test-isolation.md` tasks 3.2/3.3 `CONFLICTING_EVIDENCE` flag back to `DONE_VERIFIED` with fresh evidence. Next task: `TD-2` (21 Playwright chromium failures) — not started, per the one-work-package rule.
- **Session 2 (2026-07-12, commit `c6de61f`):** Worked `TD-1` (`AuditEventPersistenceTest` stale field-list assertion). Root cause confirmed: a legitimate, later, well-tested feature (audit before/after-state diff drawer, Playwright Phase 3C-5 "F-D7") added two entity fields the test's exhaustive assertion never accounted for, because the implementing session had no Testcontainers runtime to catch it. Fixed by updating the assertion and adding a dedicated JSONB round-trip test — not by reverting the feature. TD-1 **RESOLVED**. Documented the decision in new `status/roadmaps/audit-export-closure.md`. Discovered and documented (but did **not** fix, per the one-work-package rule) a new, unrelated regression: **TD-5** — 4 Failsafe integration-test failures in `deterministic-seed-and-test-isolation`'s `testing` module (stale hardcoded merchant-count assertions), previously masked because TD-1 always stopped the build before Failsafe could run. TD-5 is now resolved (see Session 3 above). See `status/technical-debt/current-baseline.md` and `status/evidence/latest-validation.md` for full detail.

# Status Index — Payment Quality Engineering Lab

See `status/README.md` for the full model explanation and update protocol before acting on anything below.

## Repository snapshot

| Field | Value |
|---|---|
| Branch | `001-project-foundation` |
| HEAD | `95e35c97d74608bdc3d7925a4f6bb0b46c99a79b` (Wave 2A made no commit; its changes remain in the intentional dirty working tree) |
| Last updated | 2026-07-13 |
| Working tree | All inherited TD/Wave 1 changes are preserved. Wave 2A adds five focused frontend acceptance-test files, one focused backend property-test file, one minimal `ProblemDetailsCard` class correction, and directly related status evidence. `.kiro/**` and `.codex/**` unchanged. |

## Kiro coverage

100% of leaf tasks across all 7 discovered `.kiro/specs/*` directories are mapped in `status/specs/*.md`, mechanically cross-checked against a parsed extract of each `tasks.md` (see completeness notes below).

| Spec | All Kiro items (incl. parents/checkpoints) | Leaf tasks | Mapped | Unmapped | Coverage |
|---|---:|---:|---:|---:|---:|
| `backend-authority-refactor` | 28 | 23 | 23 | 0 | 100% |
| `iam-roles-and-keycloak-login` | 38 | 25 | 25 | 0 | 100% |
| `payment-operations-dashboard` | 73 | 59 | 59 | 0 | 100% |
| `tenant-model-and-isolation` | 40 | 33 | 33 | 0 | 100% |
| `user-management` | 54 | 46 | 46 | 0 | 100% |
| `audit-log-dashboard` | 55 | 46 | 46 | 0 | 100% |
| `deterministic-seed-and-test-isolation` | 31 | 25 | 25 | 0 | 100% |
| **Total** | **319** | **257** | **257** | **0** | **100%** |

**Completeness-gate note:** the `deterministic-seed-and-test-isolation` drafting agent initially omitted leaf task `7` ("Final checkpoint") from its ledger — caught by this orchestrating session's own mechanical row-count cross-check against the pre-extracted task list, and added directly to `status/specs/deterministic-seed-and-test-isolation.md` with full evidence and an explicit `CONFLICTING_EVIDENCE` note (see that file and `status/technical-debt/current-baseline.md` TD-1). No other spec had an unmapped or duplicated row.

## Canonical Kiro specification identifiers

For current status and implementation references, the canonical identifier is the unique spec slug and its `.kiro/specs/{slug}/` path. The raw `.config.kiro` `specId` is historical planning metadata, not a current cross-spec key.

| Canonical identifier / owner | Canonical source path | Historical `.config.kiro` `specId` | Current reference rule |
|---|---|---|---|
| `backend-authority-refactor` | `.kiro/specs/backend-authority-refactor/` | `7b3e1c2a-9f4d-4a61-8c52-2d6e0f1a4b88` | Use slug/path. |
| `iam-roles-and-keycloak-login` | `.kiro/specs/iam-roles-and-keycloak-login/` | `ddbff980-460a-4eec-ae6b-f004d743fac8` | Use slug/path; never use the shared UUID to identify this spec. |
| `payment-operations-dashboard` | `.kiro/specs/payment-operations-dashboard/` | `09e5bf86-8d3f-44cc-8bf6-948bef7b5d98` | Use slug/path. |
| `tenant-model-and-isolation` | `.kiro/specs/tenant-model-and-isolation/` | `6fece955-0e29-47fa-9297-0738d73b54fd` | Use slug/path. |
| `user-management` | `.kiro/specs/user-management/` | `ddbff980-460a-4eec-ae6b-f004d743fac8` | Use slug/path; never use the shared UUID to identify this spec. |
| `audit-log-dashboard` | `.kiro/specs/audit-log-dashboard/` | `ddbff980-460a-4eec-ae6b-f004d743fac8` | Use slug/path; never use the shared UUID to identify this spec. |
| `deterministic-seed-and-test-isolation` | `.kiro/specs/deterministic-seed-and-test-isolation/` | `ddbff980-460a-4eec-ae6b-f004d743fac8` | Use slug/path; never use the shared UUID to identify this spec. |

**Historical collision:** `ddbff980-460a-4eec-ae6b-f004d743fac8` has no canonical owner: it occurs in four immutable Kiro configs with four different meanings. It is a legacy Kiro metadata collision, not an alias. Do not use it in new status entries or implementation references. The four canonical owners are the four distinct slug/path pairs listed above. The collision remains only because `.kiro/**` is read-only; current references are unambiguous by this registry.

## Execution summary

| Spec | Overall status | Done verified | Done w/ deviation | Optional skipped | Not started / deferred | Next task |
|---|---|---:|---:|---:|---:|---|
| `backend-authority-refactor` | DONE_VERIFIED | 23 | 0 | 0 | 0 | NO_KIRO_TASK_REMAINING |
| `iam-roles-and-keycloak-login` | DONE_VERIFIED | 17 | 1 | 7 | 0 | NO_KIRO_TASK_REMAINING |
| `payment-operations-dashboard` | COMPLETE_AND_KIRO_MARKED | 46 | 6 | 7 | 0 | NO_KIRO_TASK_REMAINING (closed 2026-06-18 per user decision) |
| `tenant-model-and-isolation` | DONE_VERIFIED | 28 | 5 | 0 | 0 | NO_KIRO_TASK_REMAINING |
| `user-management` | COMPLETE_WITH_OPTIONAL_GAPS | 39 | 0 | 7 | 0 | NO_KIRO_TASK_REMAINING (required work); optional gaps 6.1/6.8–6.13 remain skipped |
| `audit-log-dashboard` | COMPLETE_WITH_OPTIONAL_GAPS | 38 | 3 | 5 | 0 | NO_KIRO_TASK_REMAINING (required); optional jqwik P1/P2/P4/P6 remain the largest optional coverage gap |
| `deterministic-seed-and-test-isolation` | STAGES_1_2_3_DONE_STAGE_4_DEFERRED | 19 | 2 | 3 | 1 (conditional Stage 4 task 6.1 deferred on Open Question 2) | NO_CURRENTLY_EXECUTABLE_KIRO_TASK; optional Stage 3 task 5.1 is `DONE_VERIFIED`; Stage 4 remains conditional |
| **Total (257 leaf tasks)** | — | **204** | **23** | **29** | **1** | — |

All 7 specs have **no required, currently-executable Kiro task remaining**. Every spec's remaining open items are either explicitly optional (and accepted as skipped), or gated on a prerequisite/decision that has not yet been triggered by the user. This is a materially different picture from a naive reading of the raw `.kiro` checkboxes, where 5 of 7 specs show 0 checked boxes — see `status/README.md` for why the checkbox is not evidence either way.

## Active work

- **Current problem:** Wave 5 OpenAPI generator is in; snapshot/CI drift is Wave 6.
- **Current phase:** `REST-MULTIPART-01` **DONE_VERIFIED**; `REST-OPENAPI-DRIFT-01` **DECISION_RECORDED** + Wave 5 tooling **IMPLEMENTED** (runtime `/v3/api-docs`, not CI). Catalog: [docs/testing/wave-5-openapi-springdoc/README.md](../docs/testing/wave-5-openapi-springdoc/README.md). `REST-SSL-PROXY-01` forwarded-headers part is green (real privileged :443 optional).
- **Next task:** Wave 6 — committed OpenAPI snapshot + breaking-change CI (allowlist). Optional: live Failsafe `OpenApiContractSpec`; RLS-lab++ / checkout `@Retryable`.
- **Blockers/gates:** OpenAPI **CI drift** waits on Wave 6. TLS lab overlay is local mkcert (gitignored); privileged :443 optional. Redirects no longer block — CPL is the training redirect server. Full compose uses host **8443**, not privileged 443.
- **Checkout Protocol Lab:** implemented. Keycloak realm unchanged. PostgreSQL module `checkoutlab` (V12+V13). Dashboard hub `/admin/checkout-lab`, hosted `/psp/checkout/{id}`, return `/checkout-lab/return`.

## Validation baseline

Full detail: `status/evidence/latest-validation.md`. Summary:

| Suite | Result | Detail |
|---|---|---|
| Backend `./mvnw compile` | GREEN | — |
| Backend `./mvnw test-compile` | GREEN | — |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — Surefire stage | **GREEN** | 469 total, 464 passed, 0 failures, 0 errors, 5 skipped — Wave 2A final validation; includes the new 4-test realm-alignment class |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — Failsafe stage | **GREEN** | 46 total, 46 passed, 0 failures, 0 errors, 0 skipped — Assurance Closure final validation |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — overall | **BUILD SUCCESS** | Exit 0 after the live-assurance controller-slice correction |
| Frontend `corepack pnpm typecheck` | GREEN | Wave 2A final validation |
| Frontend `corepack pnpm test:unit` | GREEN | 58 files / 594 tests, Wave 2A final validation |
| Frontend `playwright test --list --project=chromium` | GREEN | 101 discovered tests / 31 files; 82 belong to Chromium |
| Frontend `playwright test --project=chromium` | **GREEN** | 82 passed / 0 failed in both closure runs and the independent final run |
| PostgreSQL / Testcontainers | **GREEN** | Podman socket selected; focused REST 5/5 and filtered verify completed |
| Keycloak | Not exercised via Playwright (mocked-session default mode); real-Keycloak backend IT `UserManagementKeycloakAdminIT` passes as part of the full Failsafe run (3/3, confirmed session 3) | — |
| Known regressions carried forward | `merchant-feedback.spec.ts` — now understood as TD-2C (stale UI copy), not a mystery pre-existing regression | Reclassified, not new |
| Known flaky test | `payment-status-polling.spec.ts:52` — synchronization defect corrected; repeat-each=10 and three full Chromium runs green | Closed as TD-2G |

## Post-Kiro roadmaps

These are later work programs layered on top of (not part of) the seven Kiro specs above. See `status/roadmaps/*.md` for full detail:

- `status/roadmaps/mvp-phase1-phase2.md` — HTTP contract hardening (conditional GET/304, idempotency replay, header forwarding), 30 tasks, complete and independently re-verified with 2 regressions found and fixed.
- `status/roadmaps/system-hardening-and-frontend-polish.md` — 11 small UI/UX fixes across two review passes, now independently `DONE_VERIFIED` 11/11 by Wave 2A focused acceptance tests.
- `status/roadmaps/playwright-phase3-roadmap.md` — Playwright/SDET test-suite expansion (Phase 3A/3B/3C, feature IDs F-A1..F-D7), reported complete/green by its own execution report, but a fresh chromium run found 21 failures, contradicting that report's "all green" claim (TD-2). Updated session 4 with the TD-2A closure record (10 of 21 fixed) and the remaining TD-2B/C/D breakdown.
- `status/roadmaps/audit-export-closure.md` — Formal closure record for two previously-unowned POST_KIRO_WORK features found living in the `audit` module (export index + `AuditExportEvent`/`Response`, and the before/after-state diff drawer "F-D7"). Decision: KEEP both (real UI/API usage, safe field scoping, dedicated tests). Resolves TD-1.
- `status/roadmaps/checkout-protocol-lab/` — **IMPLEMENTED** (2026-08-13) on `checkout-protocol-lab-foundation`. Educational Checkout Protocol Lab (redirect 302, signed notify, Postgres inbox, no Kafka). Closes `REST-REDIRECT-01`.
- `status/roadmaps/browser-session-visual-network-lab/` — **IMPLEMENTED** (2026-08-13). Mirror labs in `apps/**`. `REST-MULTIPART-01` is **DONE_VERIFIED** by Wave 4 payment-evidence api-tests (not mirror disputes).

## REST-ADVANCED Wave 2B design gate

Wave 2A made no REST-ADVANCED production or test-framework change. The recommended Wave 2B order is based on present domain value and repository prerequisites:

1. **REST-MULTIPART-01 — DONE_VERIFIED (Wave 4).** Black-box `apps/api-tests` `PaymentEvidenceMultipartContractSpec` covers multipart construction, PNG binary type/filename, `201` + relative `Location`, GET-list read-back, missing/empty/unsupported/oversized parts, unsafe filename, malformed boundary (`400` `validation` without a new `error` code), cross-merchant POST `403` / GET `404`, and non-multipart `415`. Evidence: Failsafe **12/12** against `BACKEND_IMAGE=payment-quality/backend:local`; offline api-tests Surefire **81/81**. Catalog: `docs/testing/wave-4-rest-contract-gates/`.
2. **REST-SSL-PROXY-01 — forwarded headers first, real TLS separately.** Current backend uses `server.forward-headers-strategy: none` and current `Location` values are relative. First prove hostile `Host`/`X-Forwarded-*` input cannot rewrite contract output. A TLS phase requires an approved ephemeral self-signed certificate/truststore, no committed production key, explicit no-proxy behavior, certificate-failure proof, and CI feasibility. Stop before certificate material or proxy behavior that has no operational requirement.
3. **REST-REDIRECT-01 — DONE_VERIFIED by Checkout Protocol Lab.** `POST /api/checkout-lab/sessions` returns **302** + `Location` (RA `redirects().follow(false)`). Hosted checkout is the test-only redirect target. No production payment 3xx was invented. Evidence: `CheckoutLabCreateSessionRestAssuredTest`, `CheckoutLabProtocolRestAssuredTest`, Playwright `checkout-lab.spec.ts`.
4. **REST-OPENAPI-DRIFT-01 — DECISION_RECORDED (Wave 4); tooling Wave 5 IMPLEMENTED; CI Wave 6.** Code-first; owner is `apps/backend` (`springdoc-openapi-starter-webmvc-api` 3.1.0). api-tests is a consumer (`OpenApiContractSpec`), not a generator. Exclude lab paths (not `*.internal` packages). Allowlist: problem+json vs OpenAPI defaults, relative `Location`, omitted headers. Do not add a second generator. Catalog: `docs/testing/wave-5-openapi-springdoc/`. Record: `docs/architecture/openapi-ownership.md`.

## Completeness self-check (per audit brief §19)

```text
[x] all Kiro specs found (7: backend-authority-refactor, iam-roles-and-keycloak-login,
    payment-operations-dashboard, tenant-model-and-isolation, user-management,
    audit-log-dashboard, deterministic-seed-and-test-isolation)
[x] every tasks.md read in full (by the per-spec drafting agent + spot-checked by this session)
[x] all parent tasks represented (rollup rows included in each ledger table)
[x] all leaf tasks mapped (257/257, after the 1-row deterministic-seed correction)
[x] no unmapped tasks remaining
[x] no duplicate mappings found
[x] optional tasks preserved (29 OPTIONAL_SKIPPED_ACCEPTABLE rows, none deleted)
[x] deferred tasks preserved (1 DEFERRED row: deterministic-seed 6.1)
[x] superseded tasks preserved (none found needing this status this session)
[x] Kiro checkbox preserved as historical info only (ORIGINAL_KIRO_CHECKBOX column in every ledger)
[x] execution status assigned independently of checkbox (confirmed per-spec: backend-authority-refactor
    has 28/28 boxes checked yet was still independently re-verified; tenant/user-management/audit/
    deterministic-seed have 0 checked boxes yet are DONE_VERIFIED where evidenced)
[x] every DONE_VERIFIED has cited evidence (file/class/test) in its ledger row
[x] every POST_KIRO_WORK item has a SOURCE_DOCUMENT (see status/roadmaps/*.md)
[x] status/index.md consistent with status/specs/*.md (numbers above derived directly from the ledger
    tables via a mechanical parse, not estimated)
[x] .kiro/** unchanged (git status clean for .kiro across all 3 sessions)
[x] .codex/** unchanged (git status clean for .codex across all 3 sessions)
[x] code/test changes scoped to the selected work package only (sessions 2-3 modified exactly 3 test
    files — AuditEventPersistenceTest.java for TD-1, SeedProfileStartupIT.java + TestEndpointsEnabledIT.java
    for TD-5 — no production code, migrations, or unrelated files touched; git diff --check clean)
```
