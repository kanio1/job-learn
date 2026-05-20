# Frontend Dashboard and Merchant Registry

This frontend is the Nuxt dashboard for the Payment Quality Engineering Lab. Phase 1 adds authenticated merchant registry workflows at `/admin/merchants`.

## Baseline

- Nuxt 4.4.6
- Nuxt UI 4.7.1
- TypeScript 6.0.3
- Zod 4.4.3
- Pinia via `@pinia/nuxt` 0.11.3
- `nuxt-auth-utils` for sealed-cookie sessions and generic OIDC PKCE against Keycloak
- Playwright 1.60.0

## Commands

```bash
corepack pnpm install
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm build
corepack pnpm exec playwright test
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

Browser components call Nuxt server API routes under `server/api/merchants/**`. `server/utils/backendApi.ts` reads the sealed session access token server-side and forwards it as `Authorization: Bearer ...` to the backend. Tokens are not exposed to browser JavaScript.

If the backend is not running at `NUXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`), the merchant page shows a backend-unavailable message rather than an authorization denial.

## Merchant UI

`/admin/merchants` provides:

- empty, loading, error, and insufficient-authority states
- create merchant modal with Zod validation
- duplicate-conflict feedback
- merchant table with DRAFT/ACTIVE/SUSPENDED status badges
- activate and suspend actions for valid lifecycle states

## Playwright

Playwright specs cover create, validation, duplicate feedback, lifecycle, unauthenticated redirect, insufficient authority, loading, and error states. The default fast UI suite mocks Nuxt session/API routes; set `PLAYWRIGHT_USE_REAL_KEYCLOAK=true` to exercise the real Keycloak setup in the auth setup project.
