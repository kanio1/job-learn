# Layout

## Contents

- Directory map
- Where new code goes
- Pinia vs composable
- Components and UI
- New screen checklist

## Directory map

Nuxt 4 app directory (already in use):

```
apps/frontend/
  nuxt.config.ts
  app/
    pages/                 # file-based routes
    components/            # auto-imported; pathPrefix false
      shared/              # Vue widgets (not Nuxt shared/)
      merchant/ payment/ user/ audit/
    composables/           # transport + domain APIs
    schemas/               # Zod — source of truth for bounds
    stores/                # Pinia — rare
    types/                 # Vue-app-only types
    middleware/            # auth.global.ts
    layouts/               # dashboard.vue
  shared/
    types/                 # both Vue and Nitro (auth.d.ts)
    utils/                 # isomorphic helpers only — no Vue, no h3
  server/
    api/                   # BFF → Spring
    types/                 # Nitro-only (auto-imported in server/)
    utils/backendApi.ts    # token attach + header forward
    routes/auth/           # Keycloak OIDC
```

Do not put pages next to `nuxt.config.ts` (Nuxt 3 `srcDir` layout). Do not import `~/composables/*` from `server/**`. Do not import `server/utils/*` from `app/**`. Types needed in **both** bundles go in `shared/types/` (Nuxt auto-imports them). Vue-only → `app/types/`. Nitro-only → `server/types/`. Do not invent a parallel `shared/` under `app/`.

`components: [{ path: '~/components', pathPrefix: false }]` — components are named by filename, not folder prefix.

## Where new code goes

| Change | Put it |
|---|---|
| New admin screen | `app/pages/admin/...` + extend sidebar in `app/layouts/dashboard.vue` |
| New HTTP from the dashboard | `server/api/.../<name>.<method>.ts` calling `backendApi` |
| New client call | Domain composable wrapping `useApiClient` + Zod schema |
| New form/table widget | Existing component first; else `app/components/<area>/` or `app/components/shared/` |
| Dual-context type / isomorphic util | `shared/types/` or `shared/utils/` — no Vue, no Nitro imports |
| Shared ETag / lifecycle state | `app/stores/payment-orders.ts` — do not clone a second store |

Extend existing composables (`useMerchantsApi`, `usePaymentOrdersApi`, `usePaymentLifecycleApi`, …). Do not add a parallel `$fetch` helper.

## Pinia vs composable

Pinia is allowed only when two mounted surfaces read/write the same state:

- `payment-orders` — current order, `versionMarker` (ETag), history, lifecycle feedback
- `auth` — sanitized user, never the access token

Per-call headers, problem+json, and raw body stay in the composable / page. Error Lab and one-off forms stay local.

## Components and UI

Start from Nuxt UI Dashboard: `UDashboardPanel`, `UTable`, `UForm` + `UFormField`, `USkeleton`, `UEmpty`, `UAlert`, `UToast`, `UModal`, `USlideover`, `UBadge`. Custom CSS last. Pin `@nuxt/ui` **4.7.1** — do not copy live-docs APIs added later (`UEmpty` `loading` is 4.10+).

Reuse `app/components/shared/` (`ApiDebugPanel`, `ProblemDetailsCard`, `HeaderKeyValuePanel`, `ErrorState`, `LoadingState`, `EmptyStateCard`, lifecycle inputs). Do not hide HTTP learning panels.

Locator order for new controls: role + accessible name, then label, then `data-testid` from `.kiro/steering/frontend-nuxt-ui.md`.

## New screen checklist

1. Route under `app/pages` reflecting a **real** backend capability.
2. Matching `server/api` handler (method suffix) via `backendApi`.
3. Zod schema — do not loosen existing min/max/regex.
4. Loading / empty / error (problem+json → `ProblemDetailsCard`) / success.
5. Sidebar + `UDashboardSearch` if it is a primary destination.
6. `corepack pnpm typecheck`.
