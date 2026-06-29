# Payment Quality Engineering Lab — Missing Features for Playwright and SDET Learning Roadmap

> **Date:** 2026-06-29  
> **Branch:** `001-project-foundation`  
> **Audit mode:** read-only, design-only  
> **Author roles:** Staff QA Architect · Playwright Architect · REST/API Architect · Frontend Architect · Backend Architect · Database Architect · Keycloak/Security Architect · Product Strategist · SDET Learning Designer  

---

## 1. Executive Summary

The Payment Quality Engineering Lab is in a mature, production-grade state. All Phase 1 (21 tasks), Phase 2 (9 tasks), System Hardening (6 fixes), and Frontend Polish (5 improvements) are confirmed implemented. Playwright is on **1.61.0**. The backend has 8 Spring Modulith modules, a complete 5-role RBAC model, conditional GET (If-None-Match → 304), Last-Modified headers, deterministic seed, and a full external black-box API test framework in `apps/api-tests`.

The system is **ready for Playwright test suite design now**. However, 23 capability gaps remain before the lab can teach the full SDET skill inventory. The most impactful missing features are:

1. No `APIRequestContext` fixture wiring (system endpoints exist — just no test uses them).
2. No multi-role storage states beyond `platform-operator`.
3. No `page.waitForResponse` network header assertions (backend sends all required headers).
4. No download/upload system features (blocks `download` and `setInputFiles` Playwright capabilities).
5. No toast `data-testid` attributes (blocks `expect.poll` for auto-dismiss).
6. No payment status polling UI (blocks `expect.poll` + real async assertions).

**Recommended next step:** Start with `APIRequestContext`-only tests against the Error Lab BFF and payment order endpoints. Then add multi-role auth. Then implement the 5 Phase 3A system features that unlock downloads, uploads, toast, and polling assertions.

---

## 2. Source Inputs

| Priority | Source | Role in Analysis |
|---|---|---|
| 1 | Current repository code (inspected 2026-06-29) | Ground truth |
| 2 | `payment-quality-engineering-lab-frontend-rest-readiness-audit.md` | Latest state confirmation |
| 3 | `payment-quality-engineering-lab-phase1-phase2-verification-report.md` | Phase 1/2 claims |
| 4 | `payment-quality-engineering-lab-system-hardening-report.md` | Hardening claims |
| 5 | `payment-quality-engineering-lab-frontend-polish-report.md` | Polish claims |
| 6 | `payment-quality-engineering-lab-implementation-plan.md` | Original requirements |
| 7 | `playwright-161-http-api-properties-test-strategy.md` | HTTP/REST learning strategy |
| 8 | `payment-quality-engineering-lab-business-technical-cases.md` | Business case analysis |
| 9 | `playwright-sdet-feature-roadmap.md` | SDET feature roadmap |
| 10 | Direct code inspection: PaymentOrderController.java, backendApi.ts, playwright.config.ts, api.ts, playwright.config.ts | Code-level verification |

---

## 3. Repository and GitHub Discovery

### 3.1 Repository State (verified)

| Fact | Value |
|---|---|
| Branch | `001-project-foundation` |
| Playwright version | **1.61.0** ✅ (upgraded from 1.60.0) |
| E2E specs | 9 files (all using `page.route()` mock) |
| Unit/property tests | 13 files, 498+ tests |
| Active Playwright projects | 1 (chromium + platform-operator storage state) |
| `waitForTimeout` in app/ | **NONE** (removed) |
| `waitForTimeout` in tests/ | **NONE** (removed) |
| fullyParallel | `false` |
| screenshot/video on failure | ✅ configured |
| trace on-first-retry | ✅ configured |

### 3.2 Backend Verification

| Fact | Verified |
|---|---|
| If-None-Match → 304 in PaymentOrderController | ✅ lines 93, 113–114, 136, 144–145 |
| Last-Modified on real payment GET | ✅ lines 123, 128, 142, 150, 157 |
| CORS exposedHeaders: Retry-After, WWW-Authenticate, Idempotency-Replayed, Last-Modified | ✅ SecurityConfig.java line 136 |
| CORS allowedHeaders: If-None-Match | ✅ SecurityConfig.java line 131 |
| BFF forwardIfNoneMatch in payment GET route | ✅ `[paymentOrderId].get.ts` lines 4, 7 |
| ConfirmActionModal data-testid | ✅ `data-testid="confirm-action-modal"` |
| Toast data-testids | ❌ — `useToast()` only, no custom testids |

### 3.3 GitHub Discovery

GitHub MCP not available in this session. No issues created. Issue backlog proposed in Section 18 based on code analysis.

---

## 4. Current Learning Coverage Summary

### 4.1 What the lab already teaches well

| Domain | Capability | Maturity |
|---|---|---|
| REST | 12 HTTP error scenarios (all 4xx, idempotency-replay, 304) | ✅ Excellent |
| REST | ETag / If-Match / If-None-Match / 304 full round-trip | ✅ Excellent |
| REST | Problem Details RFC 9457 with all extensions | ✅ Excellent |
| REST | Idempotency-Key / replay / conflict | ✅ Excellent |
| REST | All response headers: retryAfter, wwwAuthenticate, idempotencyReplayed, lastModified, etag, vary, cacheControl, correlationId | ✅ Excellent |
| REST | CORS exposedHeaders for all custom headers | ✅ Excellent |
| Security | Authorization header masking (HeaderKeyValuePanel) | ✅ Excellent |
| Security | JWT sealed server-side session, no token in browser | ✅ Excellent |
| Security | 5-role RBAC model (PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, READ_ONLY_USER) | ✅ Excellent |
| Security | Cross-tenant masked 404 | ✅ Excellent |
| UI | Error Lab with data-testids on all 12 scenarios | ✅ Excellent |
| UI | Merchant list, detail, lifecycle (activate/suspend) | ✅ Good |
| UI | Payment order list, detail, lifecycle (authorize/capture/cancel/refund) | ✅ Good |
| UI | Audit log dashboard, user management | ✅ Good |
| UI | Support search (merchant-scoped) | ✅ Good |
| Playwright | `page.route()` / `route.fulfill()` / `route.fallback()` | ✅ Good |
| Playwright | `getByRole` / `getByLabel` / `getByTestId` / `getByText` | ✅ Good |
| Playwright | `storageState` (1 role) | ⚠️ Partial |
| Backend | Spring Modulith 8 modules, deterministic seed, reset/seed endpoints | ✅ Excellent |
| SQL | 11 Flyway migrations, PostgreSQL 18, test isolation via Testcontainers | ✅ Excellent |

### 4.2 What the lab cannot yet teach

| Domain | Missing | Root Cause |
|---|---|---|
| Playwright | `APIRequestContext` | No test uses it (system is ready) |
| Playwright | Network header assertions via `page.waitForResponse` | No test captures response headers |
| Playwright | `page.on('download')` / download flow | No download endpoint exists |
| Playwright | `page.on('filechooser')` / upload flow | No upload endpoint exists |
| Playwright | `expect.poll()` for meaningful scenario | No polling UI, no toast testids |
| Playwright | Multi-role (4 missing roles) | Only 1 project active |
| Playwright | `page.clock.fastForward()` | No expiration feature |
| Playwright | `context.waitForPage()` / multi-tab | No PSP redirect feature |
| Playwright | `frameLocator()` | PSP iframe out of scope |
| Playwright | Worker fixtures / `test.extend()` | No fixture architecture yet |
| Playwright | `test.step()` / `testInfo.attach()` | Not used in any test |
| Playwright | `page.keyboard` for complex flows | No command palette |
| Playwright | `locator.filter()` on tables | Tables exist, no test uses filter |
| Playwright | `expect.soft()` | Not used |
| Playwright | `toHaveScreenshot()` / `toMatchAriaSnapshot()` | No visual strategy |
| Playwright | `page.localStorage` / `page.sessionStorage` (1.61 new) | Not used yet |
| REST | 202 Accepted + Location + async polling | No async lifecycle endpoints |
| REST | Content-Disposition (download) | No export endpoint |
| REST | RateLimit-Limit/Remaining/Reset | No rate limiter in backend |
| Keycloak | Real multi-role token variation in Playwright tests | Only 1 storage state active |

---

## 5. Playwright Capability Coverage Matrix

| Playwright Capability | Class/Interface/Method | Current System Surface | Status | Missing Feature Needed | Business Justification | Learning Value | Priority |
|---|---|---|---|---|---|---|---|
| `APIRequestContext` | `playwright.request.newContext()` | `/api/test/reset`, `/api/test/seed`, all payment endpoints | ❌ Unused | Fixture architecture (no system feature needed) | API-driven test setup is industry standard | Critical | P0 |
| `request.post()` | `APIRequestContext.post()` | POST /api/merchants, /api/payment-orders | ❌ Unused | Fixture for seed/reset | Create test data without UI | Critical | P0 |
| `request.get()` | `APIRequestContext.get()` | GET /api/payment-orders/{id} | ❌ Unused | Verify backend state after UI action | Hybrid UI+API test pattern | Critical | P0 |
| `response.status()` | `APIResponse.status()` | All 12 Error Lab scenarios, all payment endpoints | ❌ Unused | No new feature needed | Assert exact HTTP codes | Critical | P0 |
| `response.headers()` | `APIResponse.headers()` | ETag, X-Correlation-ID, Last-Modified, Retry-After, Vary, etc. | ❌ Unused | No new feature needed — headers already sent | Network-level assertions on REST contracts | Critical | P0 |
| `response.json()` | `APIResponse.json()` | Problem Details, payment order body | ❌ Unused | No new feature needed | Assert response body structure | Critical | P0 |
| `expect(response).toMatchObject()` | 1.61 NEW | All endpoints | ❌ Unused | No new feature needed | Concise network assertions | High | P0 |
| `page.waitForResponse()` | `Page.waitForResponse()` | All BFF proxy routes | ❌ Unused | No new feature needed | Capture network response in UI test | Critical | P0 |
| `page.localStorage` | 1.61 NEW | Sealed session (no JWT in localStorage) | ❌ Unused | No new feature needed | Assert no token leak to localStorage | High | P0 |
| `page.sessionStorage` | 1.61 NEW | Sealed session (no JWT in sessionStorage) | ❌ Unused | No new feature needed | Assert no token leak to sessionStorage | High | P0 |
| `storageState` (multi-role) | `BrowserContext.storageState()` | Keycloak 5 roles, PKCE auth flow | ⚠️ 1/5 roles | Multi-role auth setup (MERCHANT_MANAGER min) | RBAC UI testing — test what each role sees | Critical | P0 |
| `getByRole` | `Locator.getByRole()` | Merchant/payment/user tables, forms | ✅ Used | — | Accessible name selectors | Good | — |
| `getByLabel` | `Locator.getByLabel()` | Form inputs with aria-labels | ✅ Used | — | Label-based locators | Good | — |
| `getByTestId` | `Locator.getByTestId()` | All shared components | ✅ Used | — | Stable test selectors | Good | — |
| `locator.filter()` | `Locator.filter()` | Payment list table (status, amount filters) | ❌ Unused | Date range picker + status filter UI | Filter rows by status in complex tables | High | P2 |
| `locator.nth()` / `.first()` / `.last()` | `Locator.nth()` | Merchant/payment tables (multi-row) | ❌ Unused | No new feature needed | Row selection in tables | Medium | P1 |
| `expect().toBeDisabled()` | `LocatorAssertions.toBeDisabled()` | Lifecycle buttons (state-gated), support search button | ❌ Unused | No new feature needed | Assert RBAC-disabled UI elements | Medium | P1 |
| `expect().toBeHidden()` | `LocatorAssertions.toBeHidden()` | RBAC-gated elements per role | ❌ Unused | Multi-role auth (needed to see difference) | Assert elements absent for wrong role | High | P0 |
| `expect.soft()` | `expect.soft()` | Form validation (multiple field errors) | ❌ Unused | No new feature needed | Multi-field validation without early fail | Medium | P1 |
| `expect.poll()` | `expect.poll()` | Toast auto-dismiss, async status update | ❌ Unused | Toast data-testids + polling UI feature | Time-dependent state without `waitForTimeout` | High | P1 |
| `page.route()` | `Page.route()` | Error Lab BFF routes | ✅ Used | — | Response mocking | Good | — |
| `route.fulfill()` sequential | `Route.fulfill()` stateful | Sequential 503→200 retry demo | ❌ Unused | Retry pattern demo (Error Lab extension or route mock only) | Retry without changing Idempotency-Key | High | P2 |
| `page.on('download')` | `Page.on('download')` | No download endpoint exists | ❌ No system | CSV export endpoint + BFF + button | Payment export for reconciliation | Critical | P1 |
| `download.suggestedFilename()` | `Download.suggestedFilename()` | Content-Disposition header (missing) | ❌ No system | CSV export endpoint | Assert file name format | High | P1 |
| `download.path()` | `Download.path()` | N/A — no download | ❌ No system | CSV export endpoint | Read and assert file contents | High | P1 |
| `page.on('filechooser')` | `Page.on('filechooser')` | No upload feature | ❌ No system | Evidence upload feature | Dispute resolution evidence | High | P1 |
| `fileChooser.setFiles()` | `FileChooser.setFiles()` | No upload feature | ❌ No system | Evidence upload feature | Upload PDF/IMG for disputes | High | P1 |
| `page.setInputFiles()` | `Page.setInputFiles()` | No upload feature | ❌ No system | Evidence upload feature | Direct file set on input | High | P1 |
| `context.waitForPage()` | `BrowserContext.waitForPage()` | No multi-tab feature | ❌ No system | PSP redirect simulator page | PSP hosted payment redirect | Medium | P3 |
| `frameLocator()` | `FrameLocator` | No iframe | ❌ Rejected | PSP iframe — REJECTED (out of scope) | 3DS challenge simulation | — | REJECT |
| `page.on('dialog')` | `Page.on('dialog')` | ConfirmActionModal is DOM modal, NOT native dialog | ❌ N/A | N/A — use `getByRole('dialog')` instead | Confirm destructive action | Low | — |
| `getByRole('dialog')` | DOM modal | ConfirmActionModal, `data-testid="confirm-action-modal"` | ❌ Unused | No new system feature | Assert modal open/closed | Medium | P1 |
| `page.clock.fastForward()` | `Clock.fastForward()` | No expiration feature | ❌ No system | Payment expiration (expires_at + job) | TTL for payment authorizations | High | P3 |
| `page.clock.setFixedTime()` | `Clock.setFixedTime()` | No time-based UI | ❌ No system | Payment expiration feature | Deterministic date assertions | High | P3 |
| `test.extend()` | Playwright fixture | No fixture architecture | ❌ Unused | Fixture layer (no system feature) | Worker-scoped auth + data | Critical | P0 |
| `test.step()` | `test.step()` | All lifecycle flows | ❌ Unused | No new feature needed | Structured test reporting | Medium | P1 |
| `expect.soft()` | `expect.soft()` | Forms with multiple errors | ❌ Unused | No new feature needed | Full validation assertions | Medium | P1 |
| `testInfo.attach()` | `TestInfo.attach()` | API responses (headers, body) | ❌ Unused | No new feature needed | Debug artifacts in reports | Medium | P2 |
| `page.keyboard.press()` | `Keyboard.press()` | Tab navigation (existing forms) | ❌ Unused | Command palette (full) or Tab-through existing forms | Keyboard accessibility testing | Medium | P3 |
| `page.evaluate()` | `Page.evaluate()` | Clipboard, localStorage assertions | ❌ Unused | Clipboard copy feature or token leak test | Copy correlation ID, assert no token | Medium | P2 |
| `page.on('console')` | `Page.on('console')` | All pages (detect leaked tokens) | ❌ Unused | No new feature needed | Detect console token leaks | High | P1 |
| `page.on('pageerror')` | `Page.on('pageerror')` | All pages | ❌ Unused | No new feature needed | Detect JS errors in base fixture | Medium | P1 |
| `toHaveScreenshot()` | `LocatorAssertions.toHaveScreenshot()` | BusinessStatusBadge, lifecycle buttons | ❌ Unused | No new feature needed (components exist) | Visual regression for status badges | Medium | P3 |
| `toMatchAriaSnapshot()` | `LocatorAssertions.toMatchAriaSnapshot()` | Merchant table, payment detail | ❌ Unused | No new feature needed (pages exist) | ARIA-based semantic assertions | Medium | P3 |
| `expect().toHaveURL()` | `PageAssertions.toHaveURL()` | Payment filter → URL query params | ❌ Unused | No new feature needed | Filter sync to URL | Medium | P1 |
| `fullyParallel: true` | `playwright.config.ts` | Blocked by data isolation | ❌ Config | Worker-scoped seed strategy | Faster CI execution | High | P1 |

---

## 6. Frontend Learning Coverage

| Area | Implemented | Learning Surface | Missing |
|---|---|---|---|
| Error Lab (12 scenarios) | ✅ | Trigger → header panel → problem card → raw JSON | Toast data-testids for auto-dismiss test |
| Merchant table/list | ✅ | Navigation, empty/loading/error states | No filter/sort by column test |
| Merchant detail | ✅ | Status actions, ETag display, headers, payment orders link | — |
| Merchant create form | ✅ | Fill, submit, validation errors | Multi-step wizard not planned yet |
| Payment order list | ✅ | Pagination (98-order dataset), filters | Date range picker missing |
| Payment order detail | ✅ | Tabs (business/HTTP/raw/history), lifecycle actions, If-Match | — |
| Payment lifecycle drawer | ✅ | Authorize/capture/cancel/refund, capture amount input, IfMatchInput | ConfirmActionModal not E2E tested |
| Support search | ✅ | Merchant-scoped search, BusinessStatusBadge, View link | Cross-merchant search not supported |
| Audit log | ✅ | Filters, table, entry drawer | No before/after diff drawer |
| User management | ✅ | CRUD, role assignment | — |
| Auth/session | ✅ | Keycloak PKCE, sealed session, forbidden page | 4 missing role storage states |
| TenantContextBadge | ✅ | Suspended/active badge, PLACEHOLDER_TENANT demo | — |
| Toast notifications | ⚠️ | `useToast()` used in 7 components | No `data-testid` on toast elements |
| ConfirmActionModal | ⚠️ | Exists with `data-testid="confirm-action-modal"` | No E2E spec tests it |
| CSV export | ❌ | — | No ExportCsvButton, no backend endpoint |
| Upload evidence | ❌ | — | No EvidenceUpload component, no backend |
| Payment status polling | ❌ | — | No polling composable |
| Payment expiration display | ❌ | — | No expires_at, no countdown |
| Tenant settings form | ❌ | — | No page, no PATCH /api/tenants |
| Risk flags / review queue | ❌ | — | No backend, no frontend |

---

## 7. REST/API Learning Coverage

| HTTP Contract | Current Surface | Status | Gap |
|---|---|---|---|
| `GET 200` + ETag + Last-Modified + Vary | Payment order detail BFF | ✅ | — |
| `GET 304 Not Modified` | If-None-Match → backend → 304 (full end-to-end) | ✅ | BFF may not handle $fetch.raw 304 gracefully; direct APIRequestContext test works |
| `POST 201` + Location + Idempotency-Key | Create payment order | ✅ | — |
| `POST 200` Idempotency replay + Idempotency-Replayed header | Error Lab trigger + create repeat | ✅ | — |
| `PATCH` + If-Match + ETag lifecycle | Payment lifecycle actions | ✅ | — |
| `HEAD` payment order | HEAD endpoint exists | ✅ | Not tested in UI |
| `400` fieldErrors | Error Lab + create form | ✅ | — |
| `401` WWW-Authenticate | Error Lab trigger | ✅ | — |
| `403` forbidden | Error Lab trigger, RBAC gates | ✅ | — |
| `404` (masked cross-tenant) | TenantIsolationContractSpec | ✅ Backend | No UI demo trigger |
| `409` idempotency conflict | Error Lab trigger | ✅ | — |
| `412` stale ETag | Error Lab trigger, lifecycle | ✅ | — |
| `415` Unsupported Media + Accept-Patch | Error Lab trigger | ✅ | — |
| `428` Precondition Required + requiredHeader | Error Lab trigger | ✅ | — |
| `429` Too Many Requests + Retry-After | Error Lab trigger | ✅ | No real backend rate limiter (BFF-only demo) |
| `202 Accepted` + Location (async ops) | — | ❌ | No async lifecycle endpoints |
| `Content-Disposition` download | — | ❌ | No export endpoint |
| `RateLimit-Limit/Remaining/Reset` | — | ❌ | No rate limiter (Bucket4j) |
| `traceparent` W3C Trace Context | — | ❌ | No Otel setup |
| `Server-Timing` | — | ❌ | Not planned for Phase 3A |
| `Link` pagination header | — | ❌ | No cursor pagination |

---

## 8. Backend Learning Coverage

| Area | Status | Notes |
|---|---|---|
| Spring Modulith 8 modules | ✅ | Correctly enforced, `ModulithArchitectureTest` passes |
| Payment lifecycle (CREATED→AUTHORIZED→CAPTURED→CANCELLED/REFUNDED) | ✅ | Full state machine |
| Idempotency domain model | ✅ | `IdempotencyRecord`, `IdempotencyKey`, replay vs conflict |
| ETag / optimistic locking | ✅ | `PaymentVersionPrecondition`, `PaymentEtag` |
| If-None-Match → 304 | ✅ | Implemented in `PaymentOrderController` |
| Last-Modified on GET/HEAD | ✅ | `DateTimeFormatter.RFC_1123_DATE_TIME.format(order.getUpdatedAt())` |
| Tenant isolation | ✅ | Masked 404 for cross-tenant access |
| Deterministic seed + reset | ✅ | `POST /api/test/reset`, `POST /api/test/seed` (feature flag) |
| Audit event trail | ✅ | V8 export index added |
| Rate limiting (Bucket4j) | ❌ | Contract test exists (restkit), no backend impl |
| Payment expiration job | ❌ | No `expires_at` column, no scheduler |
| CSV export streaming | ❌ | No endpoint |
| Evidence/attachment upload | ❌ | No endpoint, no `evidence_files` table |
| Before/after diff in audit | ❌ | No `before_state`/`after_state` JSONB columns |
| Risk flags / review queue | ❌ | No `risk_flag` column, no endpoint |
| Tenant settings PATCH | ❌ | No `PATCH /api/tenants/{id}/settings` |
| Internal notes | ❌ | No `payment_order_notes` table |
| `202 Accepted` async ops | ❌ | All lifecycle is synchronous |

---

## 9. SQL/PostgreSQL Learning Coverage

| Area | Status | Migration | Learning |
|---|---|---|---|
| Schema ownership per module | ✅ | V0.1, V1, V1.1, V2–V7 | Modulith schema separation |
| Tenant isolation via FK | ✅ | V1.1 (merchants.tenant_id) | Multi-tenant data model |
| Payment order indexes | ✅ | V3, V5 | Query optimization |
| ETag/idempotency columns | ✅ | V5 | HTTP contract persistence |
| Audit event table | ✅ | V7, V8 (export index) | Event-driven audit |
| Spring Modulith event_publication | ✅ | V6 | Outbox pattern |
| Testcontainers postgres:18 per class | ✅ | PostgresContainerSupport | Test isolation |
| `expires_at` column (payment TTL) | ❌ | Missing | Time-based query, `WHERE expires_at < NOW()` |
| `evidence_files` table | ❌ | Missing | Binary storage, FK to payment_orders |
| `before_state`/`after_state` JSONB | ❌ | Missing (audit_event) | JSONB diff queries |
| `risk_flag`/`risk_score` on merchants | ❌ | Missing | Boolean flag, float score queries |
| `payment_order_notes` table | ❌ | Missing | Text storage, actor FK |
| Cursor pagination | ❌ | Missing | `WHERE id > cursor ORDER BY id` pattern |
| Row-level security (RLS) | ❌ | Not planned | Advanced PostgreSQL 18 feature |

---

## 10. Keycloak/RBAC Learning Coverage

| Area | Status | Notes |
|---|---|---|
| 5 composite realm roles | ✅ | PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, READ_ONLY_USER |
| `KeycloakRealmRoleConverter` → `platform:` authorities | ✅ | Authorities.java |
| `TestJwtConfiguration` for backend tests | ✅ | No live Keycloak needed for backend tests |
| `platform-operator` Playwright storage state | ✅ | 1 role active |
| PKCE Authorization Code Flow | ✅ | `server/routes/auth/keycloak.get.ts` |
| Sealed server-side session | ✅ | Token never in browser JS |
| JWT claims: `tenantId`, `merchantId` | ✅ | Used for tenant isolation |
| `merchant-manager` Playwright storage state | ❌ | Not yet set up |
| `tenant-admin` Playwright storage state | ❌ | Not yet set up |
| `support-agent` Playwright storage state | ❌ | Not yet set up |
| `read-only-user` Playwright storage state | ❌ | Not yet set up |
| Role-comparison test (same URL, different data by role) | ❌ | Blocked by missing storage states |
| Cross-tenant isolation UI assertion | ❌ | Needs 2+ tenant roles |
| User management via IAM module | ✅ | `UserManagementController` |
| Keycloak Admin client (WireMock in tests) | ✅ | IAM module with WireMock stubs |

---

## 11. Missing Feature Design Table

| Feature ID | Feature Name | Domain Value | Playwright Learning Value | REST Learning Value | Frontend Impact | Backend Impact | DB Impact | Keycloak Impact | Priority | Complexity | Risk | Phase |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| F-A1 | APIRequestContext Fixture Wiring | Essential test infra | `APIRequestContext`, `request.post/get`, `response.headers()`, `expect(response).toMatchObject()` | Full REST contract assertions | None (test arch only) | None (endpoints exist) | None | None | P0 | S | Low | 3A |
| F-A2 | Multi-role Storage States (2 roles min) | RBAC validation | `storageState`, multi-project config, `toBeHidden`, `toBeVisible` for RBAC | Role-scoped REST assertions | Auth setup scripts | None | None | Keycloak realm users | P0 | M | Medium | 3A |
| F-A3 | Network Header Assertion Patterns | REST learning | `page.waitForResponse()`, `response.headers()`, `page.localStorage` (1.61) | ETag, Vary, X-Correlation-ID, Cache-Control in network | None | None | None | None | P0 | S | Low | 3A |
| F-A4 | Worker Data Isolation Strategy | Parallel safety | `test.extend()`, worker-scoped fixtures, `fullyParallel: true` | Seed/reset API pattern | Fixture layer | None (endpoints exist) | None | None | P0 | M | Medium | 3A |
| F-B1 | Toast data-testids | UX feedback | `expect.poll()`, `toBeVisible()`, `toBeHidden()` auto-dismiss | None | 7 Vue components (small change) | None | None | None | P1 | S | Low | 3A |
| F-B2 | CSV Export (payment orders) | Reconciliation | `page.on('download')`, `download.path()`, `download.suggestedFilename()` | `Content-Disposition`, `Content-Type: text/csv`, streaming GET | ExportCsvButton + BFF route | GET endpoint `/api/merchants/{id}/payment-orders?format=csv` | None | None | P1 | M | Low | 3A |
| F-B3 | Evidence Upload (payment order) | Dispute resolution | `page.on('filechooser')`, `fileChooser.setFiles()`, multipart assertions | POST multipart/form-data, file size errors | EvidenceUploadDropzone + BFF route | POST `/api/payment-orders/{id}/evidence` | `evidence_files` table | None | P1 | M | Medium | 3A |
| F-B4 | ConfirmActionModal E2E Coverage | Destructive action safety | `getByRole('dialog')`, `data-testid="confirm-action-modal"`, modal lifecycle | None | No code change (testid exists) | None | None | None | P1 | S | Low | 3A |
| F-B5 | Payment Status Polling UI | Async state | `expect.poll()`, `page.waitForResponse()`, mock response sequencing | 202 Accepted + Location, async status | Polling composable | Optional async lifecycle endpoints | None | None | P1 | M | Medium | 3A |
| F-C1 | Retry Pattern Demo | Reliability | Sequential `route.fulfill()` (503→200), `expect.poll()` | 503 Service Unavailable, retry with same Idempotency-Key | Error Lab or route mock only | None needed | None | None | P2 | S | Low | 3B |
| F-C2 | Date Range Picker (payment filters) | Operator efficiency | Calendar interaction, `page.keyboard.press()`, `toHaveURL()` | `fromDate`/`toDate` query params | DateRangePicker component | None (params already supported) | None | None | P2 | M | Medium | 3B |
| F-C3 | Audit Log Export Download | Compliance | `page.on('download')`, JSON.parse in test | GET audit with `format=json`, export index used | ExportAuditButton + BFF route | GET `/api/audit?format=json` | None (V8 index ready) | None | P2 | S | Low | 3B |
| F-C4 | Tenant Settings Form | Tenant configuration | `getByLabel`, `fill`, ETag optimistic lock form | PATCH + ETag + If-Match + 412 on stale | TenantSettingsPage + form | PATCH `/api/tenants/{id}/settings` | `contact_email`, `webhook_base_url`, `timezone` cols | Tenant authority | P2 | L | Medium | 3B |
| F-C5 | Sequential Route Mock (503→200) | Retry learning | Sequential stateful `route.fulfill()` | Retry idempotency invariant | No change (test layer) | None | None | None | P2 | S | Low | 3B |
| F-C6 | Risk Flags (RBAC-gated toggle) | Fraud prevention | `not.toBeVisible()`, `not.toBeEnabled()` per role, `locator.filter()` | PATCH risk flag, 403 for wrong role | RiskFlagBadge component | PATCH `/api/merchants/{id}` extended | `risk_flag` column | PLATFORM_ADMIN authority | P2 | M | Medium | 3B |
| F-C7 | Internal Notes on Payment Orders | CRM/dispute | `fill(textarea)`, `getByRole('listitem')`, RBAC | GET/POST notes, RBAC (SUPPORT_AGENT only) | PaymentOrderNotes component | GET/POST `/api/payment-orders/{id}/notes` | `payment_order_notes` table | SUPPORT_AGENT authority | P2 | M | Low | 3B |
| F-D1 | Payment Expiration (Clock Mocking) | Auth TTL | `page.clock.fastForward()`, `page.clock.setFixedTime()` | TTL-based status transition | ExpirationCountdown component | Scheduled expiration job | `expires_at` column | None | P3 | XL | High | 3C |
| F-D2 | PSP Redirect Simulator (New Tab) | PSP redirect flow | `context.waitForPage()`, multi-tab, `page.close()` | Return URL callback pattern | PSP callback page (no auth) | Callback endpoint (optional) | None | None | P3 | L | Medium | 3C |
| F-D3 | Command Palette (Ctrl+K) | Operator efficiency | `page.keyboard.press('Control+k')`, `page.keyboard.type()` | None | CommandPalette component | None | None | None | P3 | L | Low | 3C |
| F-D4 | ARIA Snapshot Testing | A11y compliance | `toMatchAriaSnapshot()`, `getByRole` semantic assertions | None | No new feature (existing pages) | None | None | None | P3 | S | Low | 3C |
| F-D5 | Visual Regression (Status Badges) | CI visual gate | `toHaveScreenshot()`, `--update-snapshots` | None | No new feature (badges exist) | None | None | None | P3 | S | Low | 3C |
| F-D6 | Console/PageError Monitoring | Observability | `page.on('console')`, `page.on('pageerror')` | None | No new feature (base fixture) | None | None | None | P1 | S | Low | 3A |
| F-D7 | Audit Before/After Diff Drawer | Compliance audit | Drawer assertions, conditional content | JSONB diff structure in response | AuditEntryDrawer extension | `before_state`/`after_state` JSONB | V9 migration | None | P3 | L | Low | Later |

---

## 12. Coverage Gap Table

| Gap | Why It Matters | Current Workaround | Proposed System Feature | Playwright Capability Unlocked | Priority |
|---|---|---|---|---|---|
| No APIRequestContext in any test | Cannot do API-driven setup/teardown; all tests must go through UI | None (entire setup is UI-based or mocked) | F-A1: fixture wiring using `/api/test/reset` and `/api/test/seed` | `APIRequestContext`, `request.post/get`, `response.headers()`, `expect(response).toMatchObject()` | P0 |
| No network header assertions | Cannot verify ETag, X-Correlation-ID, Vary, Last-Modified, Retry-After at network level | UI panel assertions only (weak) | F-A3: `page.waitForResponse()` patterns with existing headers | `waitForResponse`, `response.headers()`, `page.localStorage` | P0 |
| No multi-role Playwright storage states | Cannot test RBAC UI differences (what MERCHANT_MANAGER sees vs PLATFORM_ADMIN) | Only 1 role tested | F-A2: merchant-manager setup.ts minimum; 5 setups target | `storageState`, multi-project, `toBeHidden`, `toBeVisible` | P0 |
| No download/export feature | Cannot teach file download testing | None | F-B2: CSV export endpoint + ExportCsvButton | `page.on('download')`, `download.path()`, `download.suggestedFilename()`, `Content-Disposition` | P1 |
| No upload/evidence feature | Cannot teach file upload testing | None | F-B3: Evidence upload endpoint + EvidenceUploadDropzone | `page.on('filechooser')`, `fileChooser.setFiles()`, `page.setInputFiles()` | P1 |
| No toast data-testids | Cannot test auto-dismiss with `expect.poll` | None | F-B1: add `data-testid` to `useToast()` calls in 7 components | `expect.poll()`, `toBeHidden()` on ephemeral elements | P1 |
| No payment status polling UI | Cannot teach `expect.poll` in a realistic async context | `waitForTimeout` antipattern | F-B5: polling composable on payment detail page | `expect.poll()`, `page.waitForResponse()`, mock response sequencing | P1 |
| No ConfirmActionModal E2E test | Destructive actions (cancel, refund) not tested end-to-end | Unit test exists | F-B4: E2E spec (no code change, testid exists) | `getByRole('dialog')`, DOM modal lifecycle | P1 |
| No date range picker | Cannot teach complex calendar interaction | Manual URL param hack | F-C2: DateRangePicker component in payment filters | Calendar navigation, `page.keyboard`, `press('Enter')`, `toHaveURL` | P2 |
| No worker-aware data isolation | `fullyParallel: false` slows CI; parallel tests would collide | Sequential execution only | F-A4: worker fixture using per-worker merchant (MERCHANT_ALPHA_001, etc.) | `test.extend()`, worker-scoped fixtures, `fullyParallel: true` | P0 |
| No clipboard testing | Cannot teach `navigator.clipboard` assertions | None | Correlation ID copy button (small add) | `page.evaluate()`, clipboard permissions | P2 |
| No console/pageerror monitoring | Token leaks via console.log not caught | None | F-D6: base fixture with `page.on('console')` guard | `page.on('console')`, `page.on('pageerror')` | P1 |
| No payment expiration + clock | Cannot teach `page.clock` API | None | F-D1: `expires_at` column + job + countdown UI | `page.clock.fastForward()`, `page.clock.setFixedTime()` | P3 |
| No PSP new tab redirect | Cannot teach multi-tab Playwright | None | F-D2: PSP simulator page (no auth, mock HTML) | `context.waitForPage()`, `page.close()`, inter-tab | P3 |
| No visual/ARIA strategy | Cannot teach `toHaveScreenshot` or `toMatchAriaSnapshot` | None | F-D4/F-D5: use existing pages (no new feature) | `toHaveScreenshot()`, `toMatchAriaSnapshot()` | P3 |
| Playwright 1.61 APIs unused | `page.localStorage`, `page.sessionStorage`, `expect(response).toMatchObject()` available but not used | — | F-A3: add usage in base fixture and network assertions | Token security assertions, concise response assertions | P0 |

---

## 13. SDET Skill Mapping

| SDET Skill | Current System Support | Missing Feature | How It Will Be Tested Later | Priority |
|---|---|---|---|---|
| REST API contract testing | Full REST Assured suite (`apps/api-tests`), all 15+ endpoints | None | `APIRequestContext` in Playwright + REST Assured in `api-tests` | P0 |
| HTTP headers (ETag, Vary, X-Correlation-ID) | Backend sends all; UI displays all | APIRequestContext fixture to capture headers in test | `page.waitForResponse().headers()`, `expect(response).toMatchObject()` | P0 |
| Problem Details RFC 9457 | All extensions implemented and displayed | None | `response.json()` + `expect(body).toMatchObject({ type, status, correlationId })` | P0 |
| Idempotency pattern | Full Idempotency-Key + replay + conflict in Error Lab | None | APIRequestContext POST twice with same key → 201 then 200 + header check | P0 |
| Optimistic locking (ETag/If-Match/412) | Full round-trip in payment lifecycle and Error Lab | ConfirmActionModal E2E spec | UI: authorize → stale If-Match → ProblemDetailsCard 412; API: direct 412 assertion | P1 |
| Conditional GET (If-None-Match → 304) | Backend fully implemented | APIRequestContext test using it | APIRequestContext: GET → capture ETag → GET with If-None-Match → assert 304 | P0 |
| Cache headers (Cache-Control, Vary) | Backend sends; CORS exposes | Network assertion tests | `response.headers()['cache-control']` includes `no-store`, Vary includes `Authorization` | P0 |
| Rate limiting (429 + Retry-After) | Error Lab trigger (BFF-only demo) | Real backend rate limiter (Phase 3) | Error Lab: trigger-429 → assert `Retry-After > 0` in network and UI | P1 |
| Auth/RBAC (5 roles) | Backend enforces; RBAC matrix in frontend | 4 missing Playwright storage states | Multi-role test: MERCHANT_MANAGER cannot see Activate button; PLATFORM_ADMIN can | P0 |
| Tenant isolation | Backend masked 404; TenantIsolationContractSpec | Cross-tenant UI trigger in Error Lab | API: request with wrong tenant token → 404; UI: display in Error Lab | P2 |
| SQL data setup | `POST /api/test/seed` + deterministic dataset | APIRequestContext fixture using reset/seed | Before each test: `request.post('/api/test/reset')` then `request.post('/api/test/seed')` | P0 |
| Upload / multipart | None | Evidence upload feature (F-B3) | `page.on('filechooser')` → `setFiles()` → assert filename in response | P1 |
| Download / Content-Disposition | None | CSV export feature (F-B2) | `page.waitForEvent('download')` → `download.path()` → parse CSV | P1 |
| Async polling | None | Payment status polling UI (F-B5) | `expect.poll(() => page.locator('[data-status]').innerText(), { timeout: 10000 })` | P1 |
| Table filtering/pagination | Pagination exists (98-order dataset) | Date range picker (F-C2) | `locator.filter({ hasText: 'AUTHORIZED' })`, URL assertion, paginate through results | P2 |
| Form validation | Create forms have field errors | No new features; soft assertions missing | `expect.soft()` on each field error; `getByTestId('field-errors-list')` | P1 |
| Modal/dialog assertions | ConfirmActionModal DOM modal exists | E2E spec (F-B4) | `getByRole('dialog')`, accept/dismiss, assert state after | P1 |
| Accessibility | ARIA labels on all forms | ARIA snapshot (F-D4) | `toMatchAriaSnapshot()` on merchant table, payment form | P3 |
| Visual regression | Status badges implemented | Screenshot strategy (F-D5) | `toHaveScreenshot()` on BusinessStatusBadge, HttpStatusBadge | P3 |
| Observability | X-Correlation-ID end-to-end, problem body | Console/pageerror guard (F-D6) | `page.on('console', msg => expect(msg.text()).not.toContain('eyJ'))` in base fixture | P1 |
| Clock/time mocking | No expiration feature | Payment expiration (F-D1) | `page.clock.fastForward(900_000)` → assert status = EXPIRED | P3 |
| Security token hygiene | Masked Authorization in UI, sealed session | `page.localStorage`/`sessionStorage` assertion (1.61) | `const entries = await page.evaluate(() => Object.entries(localStorage))` → no `eyJ` | P0 |
| Multi-tab / popup handling | None | PSP redirect simulator (F-D2) | `const [newPage] = await Promise.all([context.waitForPage(), page.click('[data-testid="psp-redirect"]')])` | P3 |

---

## 14. Phase 3A Roadmap — Highest ROI

**Goal:** Enable the first real Playwright test suite with API-level and basic UI tests. Maximum learning per implementation effort.

**Constraint:** Do NOT implement more than 8 features. Do NOT start with PSP iframe, clock, or advanced keyboard.

| # | Feature | ID | Effort | Unlocks |
|---|---|---|---|---|
| 1 | APIRequestContext fixture wiring | F-A1 | S | `APIRequestContext`, `request.post/get`, `response.headers()`, `expect(response).toMatchObject()`, network assertions |
| 2 | Network header assertion patterns (base fixture) | F-A3 | S | `page.waitForResponse`, `page.localStorage`/`sessionStorage`, token leak assertions |
| 3 | Multi-role auth setup (platform-admin + merchant-manager) | F-A2 | M | Multi-project config, `storageState`, RBAC UI assertions, `toBeHidden`, `toBeVisible` |
| 4 | Worker-aware data isolation strategy | F-A4 | M | `test.extend()`, worker fixtures, `fullyParallel: true`, `testInfo.attach()` |
| 5 | Toast data-testids (7 components, small change) | F-B1 | S | `expect.poll()`, auto-dismiss assertions, ephemeral element patterns |
| 6 | Console/pageerror monitoring in base fixture | F-D6 | S | `page.on('console')`, `page.on('pageerror')`, security baseline |
| 7 | CSV export (backend endpoint + BFF route + ExportCsvButton) | F-B2 | M | `page.on('download')`, `download.path()`, `Content-Disposition`, file content assertions |
| 8 | ConfirmActionModal E2E spec (no code change needed) | F-B4 | S | `getByRole('dialog')`, DOM modal lifecycle, test.step() |

**Phase 3A also enables without new features (just needs test files):**
- `locator.nth()` / `locator.first()` on payment list (98-order dataset)
- `expect.soft()` on create form validation
- `test.step()` on lifecycle flows
- `expect().toBeDisabled()` on lifecycle buttons (state-gated)
- `expect().toHaveURL()` on filter sync to URL params
- `toHaveValue()` on form fields after validation

---

## 15. Phase 3B Roadmap — Advanced SDET

**Goal:** Unlock advanced Playwright patterns: polling, upload, complex RBAC, date interaction.

| # | Feature | ID | Effort | Unlocks |
|---|---|---|---|---|
| 1 | Evidence upload (backend + BFF + EvidenceUploadDropzone) | F-B3 | M | `page.on('filechooser')`, `setFiles()`, multipart validation, file size errors |
| 2 | Payment status polling UI (composable + mock response sequencing) | F-B5 | M | `expect.poll()` in real async context, sequential `route.fulfill()` |
| 3 | Sequential route mock retry demo (503→200) | F-C5 | S | Stateful `route.fulfill()`, retry idempotency invariant |
| 4 | Audit log export download | F-C3 | S | Second download scenario, JSON parse in test, `testInfo.attach()` |
| 5 | Date range picker in payment filters | F-C2 | M | Calendar navigation, `page.keyboard.press()`, `toHaveURL` with date params |
| 6 | Risk flags on merchants (RBAC-gated) | F-C6 | M | `not.toBeVisible()` per role, `locator.filter()`, multi-role comparison |
| 7 | Internal notes on payment orders | F-C7 | M | `fill(textarea)`, POST notes, RBAC (SUPPORT_AGENT only) |
| 8 | Tenant settings form (PATCH + ETag + If-Match) | F-C4 | L | Full optimistic locking form: fill → submit → 412 on stale → correct ETag → success |

**Phase 3B also enables:**
- Multi-role (5 projects) after F-C6 RBAC testing matures
- `testInfo.attach()` with API response bodies
- `page.evaluate()` for clipboard if correlation ID copy button added (small feature)

---

## 16. Phase 3C Roadmap — Expert Playwright

**Goal:** Complete the expert skill set. High learning value per feature, higher complexity.

| # | Feature | ID | Effort | Unlocks |
|---|---|---|---|---|
| 1 | Payment expiration (expires_at + scheduled job + ExpirationCountdown UI) | F-D1 | XL | `page.clock.fastForward()`, `page.clock.setFixedTime()`, time-based state |
| 2 | PSP Redirect Simulator (standalone page, no auth) | F-D2 | L | `context.waitForPage()`, multi-tab coordination, `page.close()` |
| 3 | ARIA snapshot testing (existing pages) | F-D4 | S | `toMatchAriaSnapshot()`, semantic accessibility assertions |
| 4 | Visual regression for status badges (existing components) | F-D5 | S | `toHaveScreenshot()`, `--update-snapshots`, CI visual gate |
| 5 | Command palette (Ctrl+K) | F-D3 | L | `page.keyboard.press('Control+k')`, `keyboard.type()`, ARIA snapshot |
| 6 | Audit before/after diff drawer | F-D7 | L | Drawer navigation, JSONB diff structure assertions, conditional content |

**Phase 3C also enables:**
- `page.setViewportSize()` / `devices['iPhone 14']` (mobile viewport — no system feature needed)
- HAR recording via `recordHar` in Playwright config (no backend needed)
- Full `fullyParallel: true` with sharding (after data isolation from 3A)

---

## 17. Rejected / Do Not Build

| Feature | Reason | Classification |
|---|---|---|
| PSP iframe simulator | Explicitly out of scope permanently — PAN/3DS concerns, no realistic simulation without actual PSP | Rejected by design |
| Full cross-merchant support search | Backend doesn't support cross-tenant queries; adding platform-scoped endpoint is scope creep | Rejected (backend constraint) |
| `RateLimit-Limit`/`RateLimit-Remaining`/`RateLimit-Reset` headers before rate limiter | Dead code until Bucket4j or Spring Rate Limiter implemented; tests will always be green for absent header | Rejected until Phase 3 backend |
| Fake KPI / business metrics dashboard | No backend aggregate data; would require fabricated numbers that violate `no-fabricated-business-metric` property test | Rejected by design |
| Reconciliation drift dashboard | No PSP integration, no real settlement data — would be fully mocked | Rejected by design |
| WebSocket chat | Not a feature of a payment backoffice; domain mismatch | Rejected by design |
| Geolocation / camera / microphone | No use case in payment operator console | Rejected by design |
| Drag and drop for table rows | No business case in standard backoffice operations | Rejected — low business value |
| `X-Tenant-ID` / `X-Merchant-ID` as response headers | Security risk: exposes internal tenant mapping; `Vary: Authorization` is sufficient | Rejected (security) |
| `X-B3-TraceId` / `X-B3-SpanId` | Replaced by W3C `traceparent` standard; skip both, add traceparent in Phase 3 only after Otel | Rejected — legacy headers |
| Stack trace in Problem Details | Critical security risk: reveals internal code structure | Rejected (security) |
| `Pragma: no-cache` | HTTP/1.0 legacy; `Cache-Control` is sufficient | Rejected — deprecated |
| `Content-MD5` | Deprecated in RFC 7231 §3.3 | Rejected — deprecated |
| `Age` / `Surrogate-Control` | No CDN layer in this system | Rejected — no CDN |
| `expect.soft.poll()` combination | Does NOT exist in Playwright (1.60 or 1.61) — use `expect.soft()` and `expect.poll()` separately | Rejected — API does not exist |
| `page.on('dialog')` for ConfirmActionModal | ConfirmActionModal is a DOM modal (UModal), NOT a native browser dialog — `page.on('dialog')` will never fire for it | Rejected — wrong API |
| Batch POST endpoint | Not standardized; out of scope | Rejected by design |
| `DELETE` with request body | RFC-discouraged pattern | Rejected by design |
| QUERY HTTP method | Not standardized in Spring 7 | Rejected — not supported |
| Cursor/Link header pagination | Would require full cursor pagination implementation; current offset pagination is sufficient for learning | Deferred — significant backend work |
| Full 5-project Playwright before data isolation | Parallel projects with shared seed data would cause test collisions | Deferred to Phase 3A completion |
| `traceparent` W3C header before Otel | Dead header until Micrometer Tracing + OpenTelemetry added to backend | Deferred — infrastructure prerequisite |
| `Server-Timing` header | Only safe behind `@Profile("dev")` flag; Phase 3 after infrastructure decision | Deferred — security concern in prod |

---

## 18. GitHub Issue Backlog Proposal

| Issue Title | Labels | Phase | Depends On | Why |
|---|---|---|---|---|
| Add APIRequestContext fixture with seed/reset wiring | `playwright-learning`, `phase-3a`, `test-data` | 3A | — | Critical foundation for all API-driven tests |
| Add network header assertion base patterns (page.waitForResponse) | `playwright-learning`, `phase-3a`, `rest-learning` | 3A | F-A1 | Verify ETag/Vary/X-Correlation-ID at network layer |
| Set up merchant-manager Playwright storage state | `playwright-learning`, `keycloak`, `phase-3a` | 3A | Keycloak user setup | Enable RBAC UI comparisons |
| Implement worker-aware data isolation (fullyParallel) | `playwright-learning`, `test-data`, `phase-3a` | 3A | F-A1 | Enable parallel test execution |
| Add data-testid to toast notifications (7 components) | `frontend`, `playwright-learning`, `phase-3a` | 3A | — | Enable expect.poll for auto-dismiss |
| Add console/pageerror guard to Playwright base fixture | `playwright-learning`, `security`, `phase-3a` | 3A | — | Catch JS errors and token leaks |
| Implement CSV export endpoint + BFF route + ExportCsvButton | `backend`, `frontend`, `playwright-learning`, `rest-learning`, `phase-3a` | 3A | — | Unlock download testing |
| Write E2E spec for ConfirmActionModal lifecycle (no code change) | `playwright-learning`, `phase-3a` | 3A | — | Test modal open/confirm/cancel |
| Implement evidence upload endpoint + BFF + EvidenceUploadDropzone | `backend`, `frontend`, `db`, `playwright-learning`, `phase-3b` | 3B | F-A1 | Unlock upload/setInputFiles |
| Add payment status polling composable + mock response sequencing | `frontend`, `playwright-learning`, `phase-3b` | 3B | F-B1 (toast) | Unlock real expect.poll patterns |
| Add date range picker to payment order filters | `frontend`, `playwright-learning`, `phase-3b` | 3B | — | Calendar interaction, keyboard navigation |
| Add risk_flag column + RBAC-gated toggle to merchants | `backend`, `frontend`, `db`, `keycloak`, `phase-3b` | 3B | Multi-role setup | Multi-role RBAC UI comparison |
| Implement audit log export download endpoint | `backend`, `playwright-learning`, `phase-3b` | 3B | V8 index (exists) | Second download scenario |
| Add internal notes to payment orders (SUPPORT_AGENT only) | `backend`, `frontend`, `db`, `phase-3b` | 3B | — | RBAC textarea + multi-role |
| Implement tenant settings form with PATCH + ETag | `backend`, `frontend`, `db`, `phase-3b` | 3B | — | Optimistic locking form E2E |
| Add payment expiration (expires_at + job + countdown UI) | `backend`, `frontend`, `db`, `playwright-learning`, `phase-3c` | 3C | — | page.clock.fastForward() |
| Create PSP redirect simulator standalone page | `frontend`, `playwright-learning`, `phase-3c` | 3C | — | context.waitForPage() multi-tab |
| Add ARIA snapshot tests for key pages | `playwright-learning`, `phase-3c` | 3C | — | toMatchAriaSnapshot() |
| Add visual regression for status badges | `playwright-learning`, `phase-3c` | 3C | — | toHaveScreenshot() |
| Add command palette (Ctrl+K) | `frontend`, `playwright-learning`, `phase-3c` | 3C | — | page.keyboard complex flows |
| Upgrade rate limiter (Bucket4j) + RateLimit-* headers | `backend`, `rest-learning`, `phase-3c` | 3C | — | Real rate limiting (not BFF-only demo) |

---

## 19. Risks and Dependencies

### Risk 1 — Data Isolation Before Parallel Tests

**Risk:** Running `fullyParallel: true` without worker-aware seed data will cause test collisions. Two workers operating on the same merchant will see each other's payment orders.

**Mitigation:** Phase 3A F-A4 must be completed before enabling `fullyParallel: true`. Strategy: worker 0 → MERCHANT_ALPHA_001, worker 1 → MERCHANT_ALPHA_002, worker 2 → MERCHANT_BETA_001. Each worker calls `POST /api/test/seed` before its suite; `POST /api/test/reset` only from worker 0 before the suite run.

**Dependency:** `POST /api/test/seed` and `POST /api/test/reset` endpoints already exist. Only fixture architecture (F-A4) needed.

### Risk 2 — Multi-role Keycloak Setup Requires Local Realm Configuration

**Risk:** Playwright storage states for MERCHANT_MANAGER, TENANT_ADMIN, etc. require those users to exist in the local Keycloak realm.

**Mitigation:** Document the local Keycloak realm setup (create users + assign composite roles). Can use `KeycloakAdminClient` in a setup script or manual realm import. For CI: use `PLAYWRIGHT_USE_REAL_KEYCLOAK=true` or mock the session.

**Dependency:** Local Keycloak container is already in `compose.yml`.

### Risk 3 — BFF $fetch.raw May Not Propagate 304 Correctly

**Risk:** The BFF uses `$fetch.raw()` which may throw on 304 responses (treating them as redirect-errors), preventing the real 304 from reaching Playwright through the BFF proxy.

**Mitigation:** For 304 testing, use `APIRequestContext` to call the backend directly on port 8080 (bypassing BFF). The backend endpoint is confirmed to return real 304. Error Lab's `trigger-304` handles this at BFF level by catching and manually returning the demo — sufficient for UI display.

**Impact:** 304 testing will be API-level (REST Assured + APIRequestContext), not UI-level. No system change needed — this is the correct approach.

### Risk 4 — Toast Test Flakiness Without data-testid

**Risk:** Toasts rendered by Nuxt UI `useToast()` have internal class-based structure. Without `data-testid`, assertions rely on text content which may change or on timing.

**Mitigation:** Add `data-testid="toast-success"` / `toast-error` / `toast-info` in the `useToast()` call options. Nuxt UI supports custom `class` and the `body` can contain a wrapper with data-testid.

### Risk 5 — PSP Redirect Simulator Security

**Risk:** A public unauthenticated page simulating a PSP redirect could be mistaken for a real payment page.

**Mitigation:** Page clearly labeled "SIMULATION — Lab Environment Only". No card number fields. No real payment processing. No PCI scope.

### Risk 6 — Command Palette Over-Engineering

**Risk:** A full Ctrl+K command palette is significant frontend work (search index, keyboard routing, focus management) with limited immediate business value.

**Mitigation:** Defer command palette to Phase 3C. The `page.keyboard` capability can be first introduced via Tab navigation through existing forms (no new feature needed). Teach keyboard concepts in Phase 3A/B first.

---

## 20. Final Recommendation

### System readiness

The system is ready for `APIRequestContext`-only test suite **today**. The backend sends all required headers (ETag, Last-Modified, X-Correlation-ID, Vary, Cache-Control, Retry-After, WWW-Authenticate, Idempotency-Replayed). The BFF forwards them all. Playwright 1.61.0 is installed. No additional system features are needed to start.

### Recommended next 5–8 system features (in order)

1. **F-A1** — APIRequestContext fixture wiring (no system change, test architecture only) — **start immediately**
2. **F-A3** — Network header assertion base patterns with `page.waitForResponse` and `page.localStorage` — **alongside F-A1**
3. **F-A2** — Multi-role storage state (merchant-manager minimum) — **after Keycloak realm users confirmed**
4. **F-B1** — Toast data-testids (7 components, small frontend change) — **unblocks expect.poll**
5. **F-B4** — ConfirmActionModal E2E spec (zero code change — testid already exists) — **quick win**
6. **F-D6** — Console/pageerror base fixture guard — **alongside F-A1 as part of base.fixture.ts**
7. **F-B2** — CSV export endpoint + BFF route + ExportCsvButton — **first download scenario**
8. **F-A4** — Worker-aware data isolation strategy — **enables `fullyParallel: true`**

### Features NOT to do now

- PSP iframe (permanently rejected)
- RateLimit-* headers (no rate limiter backend)
- Payment expiration + clock (Phase 3C)
- Command palette (Phase 3C)
- Full 5-role Playwright before data isolation (Phase 3B+)
- ARIA snapshots + visual regression (Phase 3C, use existing pages)
- traceparent before Otel setup

### Frontend/backend/DB/Keycloak separate prompt needed?

- **Frontend:** F-B1 (toast testids), F-B2 (ExportCsvButton), F-B3 (EvidenceUploadDropzone), F-B5 (polling composable) — yes, needs dedicated implementation prompt
- **Backend:** F-B2 (CSV streaming endpoint), F-B3 (evidence upload endpoint), F-C4 (tenant settings PATCH), F-D1 (expiration job) — yes, needs dedicated implementation prompt
- **DB:** F-B3 (`evidence_files` table), F-C6 (`risk_flag` column), F-D1 (`expires_at` column) — yes, needs migration prompts
- **Keycloak:** Multi-role user setup — needs local realm configuration prompt
- **Test architecture (no system change):** F-A1, F-A3, F-A4, F-D6, F-B4 — can be done in a single Playwright foundation prompt

### Shortest realistic roadmap for maximum Playwright learning

```
Week 1: Phase 3A foundation (F-A1 + F-A3 + F-D6 + F-B4)
         → APIRequestContext, network assertions, console guard, modal E2E
         → Tests: 10–15 API-level specs + 5 UI smoke specs

Week 2: Phase 3A auth + data (F-A2 + F-A4 + F-B1)
         → Multi-role, worker isolation, toast assertions
         → Tests: RBAC UI specs, parallel-safe specs

Week 3: Phase 3A download (F-B2)
         → CSV export + ExportCsvButton
         → Tests: download specs, Content-Disposition assertion

Week 4–6: Phase 3B (F-B3 + F-B5 + F-C2 + F-C6)
         → Upload, polling, date picker, risk flags
         → Tests: upload specs, async polling, calendar, multi-role RBAC

Week 7–10: Phase 3C (F-D1 + F-D2 + F-D4 + F-D5 + F-D3)
         → Expiration/clock, PSP redirect, visual/ARIA, command palette
         → Tests: clock mocking, multi-tab, screenshot, ARIA snapshot
```

### Capability gap summary

- **23 Playwright capability gaps** identified
- **8 gaps need no new system feature** (just test architecture and test files)
- **7 gaps need small/medium system features** (Phase 3A/3B)
- **5 gaps need large system features** (Phase 3C)
- **3 gaps are permanently rejected** (PSP iframe, native dialog, expect.soft.poll)

---

*Report generated 2026-06-29. Ground truth: current repository code. All claims verified against code, not documentation alone.*
