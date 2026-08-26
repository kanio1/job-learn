# Playwright `tests-pom` quality hardening — validation evidence

## 2026-08-25 — Ticket 01 (`T01-A01`…`T01-A07`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`. Inherited dirty worktree preserved (96 porcelain entries; broad Event Lab and POM WIP).
- `T01-A01` PASS: temporarily added an included POM probe calling a nonexistent `MerchantsListPage.typecheckProbeMissingMember()`; `corepack pnpm typecheck:pom` exited 2 with `TS2339`. The probe was deleted, then the same command exited 0.
- `T01-A02` PASS: `corepack pnpm typecheck:pom` exited 0; the strict standalone config covers `tests-pom/**/*.ts` and active `playwright*.config.ts` (0 TypeScript diagnostics).
- `T01-A03` PASS: scoped static scan found no executable `@ts-ignore`, `@ts-expect-error`, `as any`, `waitForTimeout`, `test.only`, `page.route`, or `route.fulfill`; `git diff --check` exited 0.
- `T01-A04` PASS: `merchants-table.spec.ts` uses `app.merchants.caption()`; `MerchantsListPage.caption()` returns the existing `merchant-registry-caption` locator.
- `T01-A05` PASS (static): `merchants.spec.ts` registers the POST response observation before one `app.payments.runExpirationSweep()` action, then asserts 200 and the visible completion/count outcome.
- `T01-A06` PASS: `corepack pnpm exec playwright test --config playwright.pom.config.ts --list` exited 0; discovered 332 tests in 75 files.
- Touched-file POM lint: `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom/pages/MerchantsListPage.ts tests-pom/pages/PaymentsListPage.ts tests-pom/specs/merchants-table.spec.ts tests-pom/specs/merchants.spec.ts` exited 0 with 6 existing warnings (0 errors); no warning was introduced by the two new POM contracts.
- `T01-A07` NOT_RUN: live stack check could not connect and required password variables were absent (names only were checked; no values read). No generated storage state or credentials were logged. `.auth` is ignored by `.gitignore`.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright layer 0 P0/P1. The locator is stable, data remains unique, the action is not retried, and business/network assertions remain in the spec.

## 2026-08-25 — Ticket 02 (`T02-A01`…`T02-A08`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited dirty worktree remains preserved.
- `T02-A01`/`A02` PASS: runner-only types remain from `@playwright/test`; the direct `playwright` package is not resolvable in this dependency graph, so `PlaywrightWorkerArgs['playwright']` accurately represents the runner fixture without a dependency change. `ensureWorkerWorld` now declares only `{ parallelIndex: number }` and the unused `TestInfo` import is gone.
- `T02-A03`/`A05` PASS: `HttpResult` narrowing is now restricted to 200/201/202. Non-success statuses remain the `ProblemDetails | DTO` union and error specs narrow with `expectProblem`; no `body!`, `as any`, or `ProblemDetails & DTO` remains in `tests-pom`.
- `T02-A04` PASS (static): all BFF methods, including payment PATCH, use the shared `parseJson`/`parseJsonText` empty/invalid-body path rather than `response.json().catch(...)`; no JSON is manufactured for HEAD/304/empty responses.
- `T02-A06` PASS: `rls-lab.spec.ts`, merchant BVA and payment BVA specs retain status/body assertions; the 200 RLS oracle is explicitly narrowed before reading compare fields and the 404 remains a Problem Details oracle.
- `T02-A07` PASS (static): the `api`, worker API and browser-context fixtures retain teardown after `use`; direct RLS contexts use `finally` for API and browser disposal.
- `T02-A08` PASS (static/discovery): `corepack pnpm typecheck:pom` exited 0; `corepack pnpm exec playwright test --config playwright.pom.config.ts tests-pom/specs/rls-lab.spec.ts --list` exited 0 with 8 tests in 7 files. Focused live BFF execution is `NOT_RUN` for the recorded environment requirement.
- POM lint on the touched BFF/fixture/spec scope exited 0 with 29 warnings and 0 errors. Warnings are pre-existing broader typed-boundary debt; this ticket removed the two RLS success/error intersections and added no lint errors. `git diff --check` exited 0.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright/REST layer 0 P0/P1. The fix keeps status, body and header assertions in specs and adds no transport framework or network mock.

## 2026-08-25 — Ticket 03 (`T03-A01`…`T03-A07`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited dirty worktree preserved.
- `T03-A01` PASS: static `toPass` inventory now contains only the Checkout inspector read/poll (`GET` observation). The Error Lab 401 UI mutation was changed to register `waitForResponse` once, click once, then assert 401.
- `T03-A02`/`A06` PASS (static): `PspRedirectSimulatorPage.approve()` is a one-click intent; `psp-redirect.spec.ts` explicitly asserts exactly one click and `Payment approved` at the visible outcome seam.
- `T03-A03` PASS: `SavedViewsComponent.setDefault()` returns the observed POST status; `payments-views.spec.ts` asserts 200 and independently verifies the persisted default through the BFF.
- `T03-A04` PASS: the repeated Support card-selection interaction is now `SupportPage.selectCases(...)`; merchant network-oracle direct usage and payment-PIN keyboard/clock browser primitives remain directly in specs with explanatory comments.
- `T03-A05` PASS: remaining direct page use in the touched journeys has a browser/network reason; no raw repeated support interaction remains.
- `T03-A07` PASS (static/discovery): `corepack pnpm typecheck:pom` exited 0. Touched POM lint exited 0 with 6 warnings and 0 errors. Targeted `--list` exited 0 with 27 tests in 11 files. Focused live PSP/Error Lab/Saved Views/Support journeys are `NOT_RUN` because the stack and env-only passwords remain unavailable.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright layer 0 P0/P1. The added `selectCases` intent replaces four real call sites without a new abstraction layer; no network mock or retry was added. `git diff --check` exited 0.

## 2026-08-25 — Ticket 04 (`T04-A01`…`T04-A08`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited dirty worktree preserved.
- `T04-A01`/`A02` PASS: every active config has `retries: 0` and `trace: 'retain-on-failure'`. Mirror flag-off, RLS flag-off and RLS Spring-off now also retain video on failure, matching main/TLS/learner diagnostics.
- `T04-A03`/`A04` PASS (static): every account, including worker managers W0–W3, uses `requiredEnv` for its password; public usernames alone retain defaults. `requiredEnv` emits only the variable name and the env-only policy. A direct setup execution was `ENVIRONMENT_FAILURE` before test code because this sandbox cannot launch Chromium (`Operation not permitted`); it exposed no credential value.
- `T04-A05` PASS: `.gitignore` covers both `test-results/` and `**/tests-pom/.auth/`. The failed sandbox launch produced an ignored trace only; it was removed and no auth/token/state artifact entered the diff.
- `T04-A06` PASS: discovery matrix is documented in `tests-pom/README.md`. All commands exited 0: main 332 tests/75 files; visual 341/77; TLS 10/3; RLS flag-off 3/2; RLS Spring-off 2/2; Mirror flag-off 5/2; learner 3/3.
- `T04-A07` PASS: `find tests-pom/specs -name '*.spec.ts'` found 72 live specs; main plus overlays discover all intended product/overlay suites.
- `T04-A08` PASS: `corepack pnpm typecheck:pom` exited 0. Config/auth-only oxlint exited 0 with 3 existing warnings and 0 errors. `git diff --check` exited 0.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright layer 0 P0/P1. No shared config factory, retry, mock or credential fallback was added.

## 2026-08-25 — Ticket 05 (`T05-A01`…`T05-A08`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited Event Lab WIP was preserved.
- `T05-A01`/`A02` PASS: `Request` comes from `@playwright/test`; no dynamic Playwright type or cast remains. Existing `createAuthorizedEvent` is the single typed create → ETag → authorize helper and returns only merchant/payment ids; all delivery, DLT, duplicate, auth and tenant oracles remain in individual specs.
- `T05-A03`/`A04` PASS: API negatives, operator E2E and browser-security tests remain visibly separated in `event-lab.spec.ts`; multi-stage Kafka journeys use named business steps.
- `T05-A05` PASS: static scan found no route mock, `waitForTimeout`, `kafkajs`, dynamic Playwright import, or browser-to-broker path. The literal broker-port check is retained solely as the no-leak security oracle.
- `T05-A06` PASS: Event Lab spec/page POM lint exited 0 with 0 warnings/errors. The broader BFF facade has independently recorded existing warnings; none are emitted by Event Lab files.
- `T05-A07` PASS: `corepack pnpm typecheck:pom` exited 0. Event Lab targeted discovery exited 0 with 32 tests in 7 files.
- Typed contract hardening: `EventLabListRow` now declares `targetId` and `status`, matching the existing frontend Zod schema and backend `EventLabRecordDto`. Polling now compares those fields directly rather than serialized JSON.
- `T05-A08` NOT_RUN: real Kafka Playwright REST/E2E/security execution needs the `--kafka` stack, environment-only passwords and a Chromium-capable runtime; current sandbox cannot launch Chromium. No result is claimed as PASS.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright/Event Lab layer 0 P0/P1. No application behavior, broker protocol, Kafka UI, dependency or mock was added. `git diff --check` exited 0.

## 2026-08-25 — Ticket 06 (`T06-A01`…`T06-A08`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited POM WIP preserved.
- `T06-A01`/`A02` PASS: the selected long Notification Center and capture → WebSocket → operations-feed E2E journeys now expose three domain-level report steps each: arrange, observable integration state, and final user outcome. No step describes a raw click or fill.
- `T06-A03` PASS: no short or single-state spec was changed.
- `T06-A04` PASS: `NotificationCenterComponent` contains no `xpath` or `ancestor` locator. The inherited WIP had already replaced the former ancestor XPath; the current component keeps its strict test-id state locators.
- `T06-A05`/`A06` PASS: bell, mark-read and read-all controls use role/name, with mark-read scoped to the stable notification-item test id. Popover, item and badge retain their stable test ids because they identify complex state containers.
- `T06-A07` PASS: `corepack pnpm typecheck:pom` exited 0; scoped POM lint exited 0 with 0 warnings/errors; targeted Playwright discovery exited 0 with 18 tests in 8 files; `git diff --check` exited 0.
- `T06-A08` NOT_RUN: a representative live report/trace requires the stack, env-only authentication and a Chromium-capable runtime. This sandbox cannot launch Chromium, so no report artifact is represented as live evidence.
- Scoped review: Standards 0 findings; Spec 0 findings; Playwright layer 0 P0/P1. Steps retain network/visible business oracles; no broad decorator, mock, production change or locator weakening was added.

## 2026-08-25 — Ticket 07 (`T07-A01`…`T07-A07`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited worktree changes were preserved.
- `T07-A01`/`A02` PASS: the documented inventory begins with `playwright.pom.config.ts --list`, then follows direct and indirect `methods/` imports. A repeatable scan found a consumer for every retained method artifact; the main discovery remains 332 tests in 75 files.
- `T07-A03` PASS: retained artifacts have JSDoc naming their technique, change/oracle and layer; `methods/README.md` records the reachability rule.
- `T07-A04`/`A06` PASS: removed the unreachable CPL-only pairwise artifact and its stale catalog/layout entries. Documentation now explicitly keeps checkout pairwise outside this POM suite; no live test ID or configured spec was removed.
- `T07-A05` PASS: removed the 38-line `PaymentOrderDraft` builder, which had two trivial call sites. `tenant-scope.spec.ts` now uses a four-line typed local factory for the shared 1000 PLN body plus its distinct reference variant.
- `T07-A07` PASS: `corepack pnpm typecheck:pom` and scoped POM lint exited 0; targeted tenant-scope discovery found 15 tests in 7 files; main configured discovery found 332 tests in 75 files; the no-stale-artifact and reachability scans plus `git diff --check` exited 0.
- `ponytail-review`: `Lean already. Ship.` The ticket removes two files (52 lines) and introduces no dependency, runner or speculative abstraction.
- Live execution is unchanged and NOT_RUN in this sandbox; this ticket's proof is static/discovery-only and logs no credentials or state artifacts.

## 2026-08-25 — Ticket 08 (`T08-A01`…`T08-A07`)

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; inherited worktree changes were preserved.
- `T08-A01` PASS: `tests-pom/api/bff-contract-responsibility-matrix.md` classifies payment create/read/PATCH/lifecycle, merchant/tenant/support mutations, download routes, and BFF auth/error handling as forwarded, generated, transformed, server-only or not applicable.
- `T08-A02` PASS (static + test design): `backendApi.ts` forwards both `X-Correlation-ID` and `Accept-Patch`; no BFF `*.options.ts` route exists, so `Accept-Patch` is evidence-backed NOT_APPLICABLE at the Nuxt boundary. The new payment-create test verifies caller correlation-id forwarding and response echo.
- `T08-A03` PASS: the matrix links idempotency replay to `payments-create.spec.ts`, conditional/ETag headers to `payments-conditional.spec.ts`, and If-Match to conditional/lifecycle specs; backend keeps its own controller status grids.
- `T08-A04` PASS: added exactly one BFF-specific REST test (correlation forwarding and session-material non-leak), not a duplicate backend status matrix.
- `T08-A05`/`A06` PASS: the test uses `expectStatus` before reading typed headers/body and `expectNoAuthTokenLeak` over both; no token/state value is logged.
- `T08-A07` PASS (static/discovery): test-first typecheck failed as expected with `TS2554` before the optional client argument existed; after the minimal client change, `corepack pnpm typecheck:pom` exited 0, scoped POM lint had 0 errors (11 existing BFF-client warnings), targeted discovery found 17 tests in 5 files, and `git diff --check` exited 0.
- The live correlation request is NOT_RUN: stack, env-only auth and Chromium-capable sandbox are unavailable. No static/discovery success is presented as a live result.

## 2026-08-25 — Ticket 09 (`T09-A01`…`T09-A12`) — incomplete by environment

- Branch/commit: `001-project-foundation` / `e2c4eed709854029eff68d81806831ccf2f7c476`; dirty inherited POM/Event Lab WIP was preserved. No credentials, cookies or storage-state content were recorded.
- `T09-A01` PASS: `corepack pnpm typecheck` and `corepack pnpm typecheck:pom` exited 0.
- `T09-A02` PASS (scope): complete POM oxlint exited 0 with 0 errors and 107 inherited warnings; the final touched BFF/spec lint has 0 introduced warnings. Ticket 07 removed 52 lines of unreachable artifacts with no replacement framework.
- `T09-A03` PRE_EXISTING_OUT_OF_SCOPE: `corepack pnpm lint` exits 1 because its warning-gated full-tree baseline includes existing `app/**`, `tests/**` and inherited `tests-pom/**` anti-slop warnings. A focused scan found no lint `error`; POM lint itself exits 0. This program did not change product source to silence it.
- `T09-A04` PASS: `git diff --check` exited 0.
- `T09-A05` PASS: fresh discovery: main 333 tests/75 files; visual 342/77; TLS 10/3; RLS flag-off 3/2; RLS Spring-off 2/2; Mirror flag-off 5/2; learner 3/3. The main/visual increments are the new correlation test.
- `T09-A06` NOT_RUN: all modified targeted journeys discover successfully (including 17 payment conditional/create and 18 notification/feed tests), but live execution cannot start in this sandbox.
- `T09-A07` ENVIRONMENT_FAILURE: `PLAYWRIGHT_SKIP_WEBSERVER=1 corepack pnpm exec playwright test --config playwright.pom.config.ts --max-failures=1 --reporter=line` reached setup only; Chromium aborted with sandbox shutdown `Operation not permitted`. Result: 1 setup failure, 3 interrupted, 329 not run, 0 product assertions executed. This is not a test regression or a PASS.
- `T09-A08` NOT_RUN: Event Lab Kafka and TLS/RLS/Mirror live overlays require their documented stacks, env-only auth and a Chromium-capable runtime. The browser failure blocks them before a meaningful product run.
- `T09-A09` PASS (static): no executable `page.route`, `route.fulfill`, `waitForTimeout(`, `test.only`, `as any` or password fallback exists in `tests-pom`; no generated auth artifact is tracked. The remaining poll inventory is read/UI observation only, never a mutation.
- `T09-A10` PASS (static review): final Standards 0 findings and Spec 0 findings for the program scope; `playwright-sdet-review` and `rest-api-test-design` found no open P0/P1. `ponytail-review`: `Lean already. Ship.`
- `T09-A11` reconciliation: F-01 strict POM compiler VERIFIED; F-02 POM contracts VERIFIED; F-03 fixture types VERIFIED; F-04 typed BFF results VERIFIED; F-05 config diagnostics VERIFIED; F-06 one-shot action VERIFIED; F-07 POM/oracle boundary VERIFIED; F-08 Event Lab static hardening VERIFIED but its live Kafka proof NOT_RUN; F-09 env-only passwords VERIFIED; F-10 business steps VERIFIED statically, report proof NOT_RUN; F-11 reachability/minimal factory VERIFIED; F-12 Notification Center locator VERIFIED; F-13 BFF header matrix VERIFIED statically, correlation live proof NOT_RUN; F-14 discovery gate VERIFIED. The runtime-qualified findings prevent closure.
- `T09-A12` PASS: this fixed dated evidence report links tickets 01–09, exact commands, counts, classifications and blockers without secrets. Program remains ACTIVE until the runtime rows above are green.

### 2026-08-25 — runtime diagnosis follow-up

- Reproduced the failure outside Playwright with the installed headless shell on `about:blank`: it exits 133 at Chromium sandbox-host shutdown with `Operation not permitted`.
- A one-run temporary config pointed Playwright at the installed full Chromium instead. It reached the same pre-test failure class through Crashpad socket setup (`Operation not permitted`): 1 setup failed, 3 interrupted, 329 did not run. The temporary config was removed immediately afterwards.
- Conclusion: this is a host/WSL browser capability failure, independent of the POM executable, application stack, credentials, test data or product assertions. Live T09-A06…A08 remain `NOT_RUN`; no live PASS is claimed and no state/token content is recorded.

### 2026-08-25 — blocked audit

- The same external runtime condition recurred for the required consecutive goal checkpoints. Current confirmation: Chromium fails before navigation, and Docker daemon access from this sandbox is denied, so the documented main/Kafka/TLS/RLS/Mirror stacks cannot be inspected or started here.
- Static T09 evidence remains valid (`git diff --check` is green), but T09-A06 through A08 cannot be completed without a Chromium-capable host, Docker access, documented stacks and environment-only authentication inputs. Program status is therefore `BLOCKED`, not complete.

### 2026-08-25 — WSL2 Docker Desktop runtime attempt

- Elevated `docker version` confirmed Docker Desktop client/server `29.7.2`; the installed Playwright headless Chromium rendered `about:blank` outside the agent sandbox. Those original sandbox capability blockers are resolved for this WSL2 session.
- `scripts/dev-stack.sh --app` was run in a persistent elevated terminal. Docker built the backend image successfully, but the frontend image failed during `corepack pnpm build`: Node terminated with `FATAL ERROR: Reached heap limit Allocation failed - JavaScript heap out of memory` (exit 134), after a mark-compact near `2035 MiB`.
- No backend/frontend app containers were created; only existing Postgres, Keycloak and Kafka support services are healthy. Consequently the password preflight and `scripts/run-app-stack-tests.sh` were not run. No credential value was read or logged.
- Classification: `ENVIRONMENT_FAILURE` (Docker Desktop WSL memory allocation), not a Playwright, Compose, product or POM regression. Increase Docker Desktop's WSL memory allocation, then rerun the documented `--app` stack followed by live POM tests.

### 2026-08-25 — corrected runtime capacity diagnosis

- Elevated `docker info --format '{{.MemTotal}}'` reports `12543696896` bytes (about 11.7 GiB) available to Docker Engine. The WSL/Docker Desktop memory allocation is therefore sufficient.
- The frontend Dockerfile has no `NODE_OPTIONS` or build argument. Its production `corepack pnpm build` runs with Node's default roughly-2 GiB old-space limit, which matches the observed OOM. A build-stage-only `NODE_OPTIONS=--max-old-space-size=4096` is the minimal corrective configuration, but changing the Dockerfile is outside this goal's allowed paths without explicit user authorization.
- The external authorization request recurred through the required goal checkpoints; runtime verification remains blocked without it. No additional stack or test command was run, so no credentials or auth state were accessed.

### 2026-08-25 — authorized Node heap correction and HTTP stack proof

- User explicitly authorized the frontend image build configuration change. `apps/frontend/Dockerfile` now sets build-stage-only `NODE_OPTIONS=--max-old-space-size=6144`; no production Vue/Nitro/Spring behavior, dependency, `.env`, credential or test was changed.
- Elevated `scripts/dev-stack.sh --app` exited `0`. It rebuilt the frontend successfully through `corepack pnpm typecheck` and `corepack pnpm build`, then passed Keycloak issuer verification, Spring compose readiness, Nuxt compose readiness, host-port rebinding, `http://127.0.0.1:8080/api/status`, and `http://127.0.0.1:3000` (HTTP 200).
- The non-fatal Tailwind sourcemap warnings during Nuxt build remain warnings; the prior Node OOM did not recur.
- Authentication name-only preflight found all six required variables missing: `PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD`, `PLAYWRIGHT_TENANT_ADMIN_PASSWORD`, `PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD`, `PLAYWRIGHT_SUPPORT_AGENT_PASSWORD`, `PLAYWRIGHT_READ_ONLY_PASSWORD`, `PLAYWRIGHT_MERCHANT_DENIED_PASSWORD`. Values were neither read nor logged. Therefore `scripts/run-app-stack-tests.sh` and T09-A06…A08 remain `NOT_RUN`, not PASS.

### 2026-08-25 — main live POM execution and runtime triage

- With user authorization, a process-local environment bridge read the existing local Keycloak realm password credentials and passed them only to `scripts/run-app-stack-tests.sh`; no value was printed, written to `.env`, or persisted by the bridge. It also supplied the platform operator and four worker-manager accounts required by the active POM configs.
- The script preflight passed: issuer `http://localhost:8081/realms/payment-quality`, Spring `200`, Nuxt `200`. The full main command executed all 333 discovered tests and exited `1`: **226 passed, 107 failed**. This is direct runtime evidence, so T09-A06 is red, not `NOT_RUN` or `PASS`.
- First independent failure is a real critical a11y violation on the merchant registry: Axe reports `aria-allowed-attr` for `aria-expanded` on a `div` popover trigger and a `span` accordion trailing control. The current goal forbids production Vue behavior changes without new authorization.
- A large later failure set receives BFF `401`. A fresh browser login after the run reproduces `Oidc login failed: state mismatch`; the local Keycloak password-grant preflight remains `200`, so neither password nor account validity is the cause. This needs a separate OIDC/session lifecycle diagnosis before being attributed to POM assertions.
- Event Lab specs also fail in the HTTP `--app` profile without the required Kafka overlay. They must be rerun against their documented Kafka stack rather than treated as a passing main-suite result.
- Attempting the documented `scripts/dev-stack.sh --app` recreation exited `1` before readiness. Backend logs show deterministic seed reset calls `MerchantSeedService.clear()` / `delete from merchants` while a test-created `support_cases.merchant_id` still references a merchant; PostgreSQL rejects it with `support_cases_merchant_id_fkey`. This is a backend data-reset ordering defect outside the POM-only scope. No destructive database cleanup was performed. Current backend container is unhealthy, so T09-A07/A08 cannot proceed.

### 2026-08-25 — authorized deterministic reseed repair

- User explicitly authorized the backend reseed repair. `SupportCaseSeedCapability` now exposes `clear()`, implemented transactionally by `SupportCaseService` through its repository. `DeterministicDataset` clears support cases before payments, merchants, and tenants in both reset and seed paths. The testing module depends only on the public support capability.
- Test-first proof: the extended `DeterministicDatasetTest` initially did not compile because `clear()` was absent; after the minimal production change it is green (3 tests). `ModulithArchitectureTest` and `SupportModuleTest` are green (3 tests total). `git diff --check` is green.
- Narrow standards/spec/Spring review of the authorized four-file diff found no open issues: the FK deletion order is correct, no REST contract or application behaviour is broadened, and no internal support package is imported.
- Integration proof: elevated `scripts/dev-stack.sh --app` rebuilt the backend and exited `0` against the same persistent data that previously caused `support_cases_merchant_id_fkey`. Backend became healthy and Nuxt returned HTTP 200. No volume wipe, direct database deletion, `.env` mutation, or credential persistence was used.
- This removes the reseed startup blocker only. The previous main POM result remains **226 passed, 107 failed**; its independent OIDC/session and accessibility failures, plus Kafka-overlay execution, remain outside this narrow repair.

### 2026-08-25 — isolated OIDC and accessibility diagnosis

- OIDC feedback loop: a fresh `setup-merchant-manager` run with `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000`, one worker, and a temporary auth directory passed **1/1**. All setup projects then passed **12/12** serially (32.2 s) and **12/12** with four workers (23.1 s), also in temporary auth directories. The original full-run `state mismatch` is therefore currently `SUSPECTED_FLAKE`, not a confirmed account, origin, stale-storage, or setup-concurrency defect. No state file, cookie, token, or password was retained.
- Accessibility feedback loop: `tests-pom/specs/a11y-axe.spec.ts --project=chromium-admin` against the healthy HTTP Compose stack completed **10 passed, 1 failed**. The login a11y check passed; the merchants registry deterministically failed critical Axe `aria-allowed-attr`.
- Root cause evidence: the notification popover passes a local `<div class="inline-flex items-center">` as the Reka popover trigger, so the generated div receives unsupported `aria-expanded`. Independently, installed `@nuxt/ui` **4.7.1** `NavigationMenu.vue` renders the vertical nested `AccordionTrigger` as `span` while it carries `aria-expanded`; this is the second exact Axe node. The POM assertion is correct and its Axe rule was not weakened.
- Remediation requires a production Vue change: make the notification trigger a semantic button and replace/repair the nested navigation rendering (or approve a verified Nuxt UI update). The goal forbids production Vue behavior or dependency changes without new user authorization, so no source change was made. T09-A06/A07 remain red and overlay validation has not been resumed.

### 2026-08-25 — authorized accessibility and isolated-auth repair

- User explicitly authorized a production Vue accessibility repair. `NotificationCenter.vue` now supplies `UPopover` a semantic `UButton` trigger, with an equivalent visual unread badge and separate screen-reader count. `dashboard.vue` supplies the nested Merchants navigation trigger through a named slot, preserving the button-owned accordion state while avoiding Nuxt UI 4.7.1's invalid `span[aria-expanded]` child. No dependency upgrade, route, authorization rule, or test-rule weakening was used.
- The live red regression went green after rebuilding the HTTP Compose app stack: `a11y-axe.spec.ts --project=chromium-admin` passed **11/11**. This includes both merchant-registry Axe checks; no critical `aria-allowed-attr` result remains.
- Independent follow-up found that active POM configs wrote setup states under `PLAYWRIGHT_POM_AUTH_DIR` but several projects consumed hard-coded `tests-pom/.auth` paths. Main, TLS, RLS flag-off, RLS Spring-off, and mirror flag-off configs now use the shared `pomAuthFiles` paths. This keeps one process-local temporary auth directory coherent without changing login or product behavior.
- Regression proof: `corepack pnpm typecheck:pom` and touched config/Vue `oxlint` both exit 0 (only existing warnings). With a fresh temporary auth directory, the live `ops-notifications.spec.ts --project=chromium-admin` run passed **10/10**, including all nine setup dependencies and the Notification Center interaction. `.last-run.json` records `passed`; the temporary directory was removed.
- The original full main run remains **226 passed, 107 failed**. This targeted repair does not claim that all its OIDC/session failures or the Kafka-overlay-only Event Lab cases are resolved; main and overlay matrix reruns remain required for T09 closure.

### 2026-08-25 — live completion wave (except TLS prerequisite)

- Branch/commit: `001-project-foundation` / `e2c4eed`; inherited dirty worktree preserved. Temporary credential bridges and auth directories were process-local and removed; no secret, cookie or state content was recorded.
- Main runtime: `node /tmp/run-main-pom.js` PASS — **310/310** in 7.7 min. Kafka: `PLAYWRIGHT_KAFKA=1 … --project=chromium-kafka` PASS — **32/32** in 1.9 min. The Kafka project is deliberately serial because the earlier four-worker dev-server run had 3 load-screen flakes; focused E2E-002 and a full one-worker confirmation were green before encoding `fullyParallel: false, workers: 1`.
- Visual/ARIA: initial host run was profile-invalid (2/11); documented HTTP `--app` run exposed a stale one-step payment-form ARIA golden and one deterministic inspected `problem-details` pixel golden. The first-step ARIA snapshot and only that reviewed golden were refreshed; rerun PASS — **11/11** in 15.6 s.
- RLS/Mirror: initial RLS flag-off run exposed two POM seam errors: ambiguous standard 404 headings and BFF REST using login `:3000` instead of the overlay. `BasePage.expectNotFound()` selects the first matching heading; special configs set `PLAYWRIGHT_BFF_BASE_URL`. Reruns PASS: RLS flag-off **3/3**, Spring RLS-off **2/2**, Mirror flag-off **5/5**.
- Static: `corepack pnpm typecheck` and `typecheck:pom` PASS; focused changed-file oxlint PASS; `git diff --check` PASS. Full lint remains `PRE_EXISTING_OUT_OF_SCOPE` (the unchanged `app/utils/paymentViewsStorage.ts` error plus inherited warnings); the POM-specific gates are green. Static banned-pattern scan found only the explanatory comment, no executable forbidden pattern; no tracked `.auth` files.
- Review (standards/spec/Playwright REST/POM/ponytail): no open P0/P1 in this completion-wave diff. The BFF origin override is scoped to special overlays, test-only, and preserves browser-origin OIDC; serial Kafka keeps one mutable delivery proof deterministic without retrying actions.
- TLS: `scripts/dev-stack.sh --tls` stopped before startup: `TLS certs missing. Run: scripts/tls-lab-certs.sh`. `mkcert` is absent, so the certificate script cannot run. Result `NOT_RUN`; TLS remains the only T09-A08 blocker and prevents goal closure.

### 2026-08-25 — TLS provisioned; lifecycle regression remains

- With explicit user approval, Fedora WSL received `mkcert` and `nss-tools`; `scripts/tls-lab-certs.sh` generated gitignored local lab material and installed the local CA. `scripts/dev-stack.sh --tls` then started Caddy, Keycloak, host Spring (`dev,tls-lab,seed`) and host Nuxt.
- TLS admin project PASS — **5/5**. Browser TLS details show the local mkcert issuer; the run did not set `PLAYWRIGHT_TLS_INSECURE`.
- TLS manager project: **4/5 passed, 1 failed**. The authorize→capture test is a real implementation regression, not certificate or timing failure: Caddy emits `ETag: "v0-gzip"`; the lifecycle drawer forwards that representation as `If-Match`; Spring rejects it with HTTP 400 while expecting its domain marker. The delivery must normalize the transport representation before the lifecycle POST and retain stale-ETag coverage. T09-A08 and T09-A11 remain OPEN; the goal is not complete.

### 2026-08-26 — origin-preserving ETag implementation

- Accepted decision: [.codex/adr/0003-versioned-rest-etag-no-transform.md](../../.codex/adr/0003-versioned-rest-etag-no-transform.md). Versioned REST responses send `Cache-Control: no-transform`; payment and tenant responses retain `no-store`. Caddy keeps `encode gzip zstd` for all other representations.
- No BFF/browser normalisation was added. `ETag` and `If-Match` remain opaque and are forwarded unchanged. This prevents Caddy's representation suffix from becoming a Spring domain concern.
- Test-first evidence: the payment conditional test, merchant controller test, and tenant MockMvc tests initially failed because their headers lacked `no-transform`; after the scoped implementation the following are green: payment conditional **1/1**, merchant **3/3**, tenant settings **7/7**, support REST Assured **12/12**. `corepack pnpm typecheck:pom` and `git diff --check` also pass.
- TLS runtime: `scripts/dev-stack.sh --tls` made Caddy, Spring, and Nuxt healthy. A persistent fresh `chromium-tls-manager` run then passed **5/5** (1.1 min), including authorize → capture. Its temporary auth directory was removed after the process ended.
- TLS POM now asserts the browser-facing chain explicitly: detail ETag is `"vN"`, `Cache-Control` contains `no-transform`, payload has no `Content-Encoding`, and authorize/capture submit exactly the ETag received from the preceding response.
- Final scoped review (standards/spec, Spring REST contract, REST Assured, Playwright POM, and ponytail) found no open P0/P1. No cache policy was broadened: only ETag-bearing representations gain `no-transform`.
