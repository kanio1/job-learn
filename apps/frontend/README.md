# Frontend Dashboard and Merchant Registry

This frontend is the Nuxt dashboard for the Payment Quality Engineering Lab. It exposes authenticated merchant registry, payment order, lifecycle, user-management, and HTTP learning surfaces under `/admin/**` and `/error-lab`.

## Baseline

- Nuxt 4.4.6
- Nuxt UI 4.7.1
- TypeScript 6.0.3
- Zod 4.4.3
- Pinia via `@pinia/nuxt` 0.11.3
- `nuxt-auth-utils` for sealed-cookie sessions and generic OIDC PKCE against Keycloak
- Playwright 1.60.0 (journey tests), Vitest + fast-check (unit/component/property tests)

## Commands

```bash
corepack pnpm install
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm build
corepack pnpm test:unit          # Vitest unit/component/property tests
corepack pnpm test:e2e              # live POM (`tests-pom`), needs --app stack
```

## Phase 1 Auth Spike Result

`nuxt-auth-utils` is viable for the Phase 1 local Keycloak PKCE login path when using its generic OIDC handler. The Keycloak-specific handler expects a confidential-client secret and does not emit PKCE parameters, so `server/routes/auth/keycloak.get.ts` uses `defineOAuthOidcEventHandler` with Keycloak discovery at `http://localhost:8081/realms/payment-quality/.well-known/openid-configuration`. The route keeps the product path `/auth/keycloak`, stores only the browser-safe user shape in the session, and keeps the access token in the sealed server-side session under `secure.accessToken`. Browser state and Pinia do not store access tokens, refresh tokens, raw sessions, or authorization headers.

## Auth Flow

- `/login` automatically starts the Keycloak redirect and keeps a fallback button.
- `/auth/keycloak` starts Authorization Code Flow with PKCE (`S256`) for the public `payment-quality-dashboard` client.
- `app/middleware/auth.global.ts` protects dashboard routes.
- `app/stores/auth.ts` exposes sanitized `isAuthenticated` and `user` state only.
- `/admin/**` is rendered client-side inside a Nuxt UI dashboard shell so the protected dashboard can use browser session state consistently.

## Dashboard Shell

The authenticated app layout follows the Nuxt UI Dashboard Template direction without adding out-of-scope business widgets. It uses `UDashboardGroup`, `UDashboardSidebar`, `UDashboardPanel`, `UDashboardNavbar`, and vertical `UNavigationMenu` composition with a single user/session menu in the sidebar footer. Phase 1 exposes only the Merchants navigation item.

## Backend Proxy

Browser components call Nuxt server API routes under `server/api/**`. `server/utils/backendApi.ts` reads the sealed session access token server-side and forwards it as `Authorization: Bearer ...` to the backend. Tokens are not exposed to browser JavaScript. The proxy forwards a fixed allowlist of response headers to the browser: `ETag`, `Location`, `Vary`, `X-Correlation-ID`, `Cache-Control`, `Accept-Patch`, `Allow`. `Authorization` is never forwarded.

If the backend is not running at `NUXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`), the merchant page shows a backend-unavailable message rather than an authorization denial.

## API Client and Composable Layer

All browser-to-backend traffic flows through a header-aware API client so the captured HTTP status, forwarded headers, and validated body are available to every screen.

- `app/types/api.ts` — the `ApiResponse<T>` envelope (`{ data, status, headers, problem, raw }`), typed `ApiHeaders` (etag, location, vary, cacheControl, correlationId, allow, acceptPatch), and the `ProblemDetails` type mirroring `application/problem+json`.
- `app/composables/useApiClient.ts` — wraps `$fetch.raw` on `server/api/**`, captures headers/status, validates the body against the supplied Zod schema before returning, detects `application/problem+json` and populates `problem` via `problemDetailsSchema`, and preserves the raw body text for `RawJsonViewer`. The bearer token is never read or held client-side.
- `app/schemas/problem-details.schema.ts` — Zod schema for problem responses with `.passthrough()` to preserve backend extension members.

Domain composables delegate transport to `useApiClient` and return `ApiResponse<T>`:

| Composable | Surface |
|---|---|
| `useMerchantsApi` | merchant list/detail/create/activate/suspend |
| `usePaymentOrdersApi` | payment order list/summary/detail/create (surfaces `etag`, `location`) |
| `usePaymentLifecycleApi` | authorize/capture/cancel/refund/PATCH + history (carries new `etag`, maps status→category) |
| `useUsersApi` | user-management list/get/create/update/assignRoles |
| `useAuthorization` | derives capability booleans from `rbacMatrix` (`canManageUsers`, `canAssignRoles`, etc.) |
| `useAuthError` | deterministic 401→`/login` vs 403→`/forbidden` reaction |

The Pinia `payment-orders` store delegates transport to the composables while preserving its public surface so existing components/tests keep passing. ETag writes flow back into the store's `versionMarker`.

## Shared Component Library

Reusable protocol/state components live under `app/components/shared/`:

| Component | Purpose | `data-testid` |
|---|---|---|
| `BusinessStatusBadge` | merchant + payment status with non-color-only text label | — |
| `HttpStatusBadge` | HTTP status code + leading-digit category (1xx–5xx) | — |
| `HeaderKeyValuePanel` | renders header pairs; explicit empty indicator; masks `Authorization` as `Bearer ••••••••` | `http-headers-panel` |
| `ProblemDetailsCard` | renders problem+json members with empty indicators | `problem-details-card` |
| `RawJsonViewer` | indented multi-line JSON preserving key order; non-JSON fallback | `raw-json-viewer` |
| `ApiDebugPanel` | request method/path/masked headers + response status/forwarded headers/body | `api-debug-panel` |
| `IdempotencyKeyInput` | generates a unique editable `Idempotency-Key` (≤255 chars) | `idempotency-key-input` |
| `EtagDisplay` | shows the current ETag version | `etag-display` |
| `IfMatchInput` | pre-filled from the latest ETag for lifecycle writes | `if-match-input` |
| `LoadingState` | skeleton/spinner loading surface | `loading-state` |
| `EmptyStateCard` | description + next action empty surface | `empty-state` |
| `ErrorState` | renders `ProblemDetailsCard` or message (token-safe) | `error-state` |
| `ConfirmActionModal` | `UModal` gating cancel/refund lifecycle actions | `confirm-action-modal` |
| `MerchantStatusCard` | wraps `GET /api/merchants/{id}` fields + badge | — |
| `PaymentOrderLifecycleActions` | one control per available lifecycle action | `lifecycle-authorize`/`capture`/`cancel`/`refund` |

Security: `HeaderKeyValuePanel` and `ApiDebugPanel` replace any `Authorization` value with the fixed `Bearer ••••••••` placeholder; no character of a real token is ever rendered.

## HTTP Learning Surfaces

The Payment Order Detail page (`/admin/merchants/[merchantId]/payments/[paymentOrderId]`) exposes four tabs: Business fields, HTTP (forwarded headers + `EtagDisplay` + `HttpStatusBadge`), Raw (`RawJsonViewer`), and History (lifecycle timeline with safe actor display).

The Error Lab page (`/error-lab`) provides exactly one trigger per supported problem code (400, 401, 403, 404, 406, 409, 412, 415, 428), renders `HttpStatusBadge` + forwarded headers + `ProblemDetailsCard`, and uses `ApiDebugPanel` so the request/response cycle is observable. `Authorization` is always shown as the masked placeholder.

## Merchant UI

`/admin/merchants` provides:

- empty, loading, error, and insufficient-authority states
- create merchant modal with Zod validation
- duplicate-conflict feedback
- merchant table with DRAFT/ACTIVE/SUSPENDED status badges
- activate and suspend actions for valid lifecycle states

## Testing

Vitest + fast-check unit/component/property tests run via `corepack pnpm test:unit`. Property tests are tagged `Feature: <spec>, Property {n}: ...` and run a minimum of 100 iterations. Colocated component tests verify loading/empty/error/forbidden/success/conflict states. No Playwright files are created or run by the `test:unit` command.

## Playwright

Playwright specs cover create, validation, duplicate feedback, lifecycle, unauthenticated redirect, insufficient authority, loading, and error states. The default fast UI suite mocks Nuxt session/API routes; set `PLAYWRIGHT_USE_REAL_KEYCLOAK=true` to exercise the real Keycloak setup in the auth setup project.
