# Frontend & Nuxt UI — Payment Quality Engineering Lab

## Stack at a Glance

- Nuxt 4.4.x, app directory layout, SSR disabled for `/admin/**` (CSR only)
- @nuxt/ui 4.7.x — use the Nuxt UI Dashboard Template as the primary shell
- TypeScript 6, strict mode
- Pinia 3 — shared state only
- Zod 4 — form schemas + API response validation
- nuxt-auth-utils — OIDC session (Keycloak); token never exposed to the browser
- Playwright 1.60 — E2E tests

---

## Non-Negotiable Rules

1. **All API calls go through `server/api/**` proxy routes.** Never call the backend directly from the browser. The proxy attaches the bearer token server-side.
2. **Use `$fetch.raw` (not plain `$fetch`) in any composable that needs response headers or status.** Plain `$fetch` discards headers — ETag, Location, Vary, X-Correlation-ID are silently lost.
3. **Validate every API response with its Zod schema before rendering any data.** On schema failure, show `ErrorState`, render nothing unvalidated, retain the prior valid view.
4. **Never expose the bearer token in the browser** — not in the DOM, HTML attributes, client state, browser storage, or logs.
5. **Mask `Authorization` header values** in every HTTP debug panel. Replace the full value with a fixed placeholder (e.g. `Bearer ••••••••`).

---

## Nuxt UI Dashboard Template — Use It First

The layout already uses `UDashboardGroup` + `UDashboardSidebar` + `UNavigationMenu` + `UDashboardSearch`. Before writing custom CSS or creating a custom layout primitive, check whether the following Nuxt UI components solve the problem:

| Need | Preferred component |
|---|---|
| Page chrome | `UDashboardPanel`, `UDashboardNavbar`, `UDashboardToolbar` |
| Tab grouping (detail page) | `UTabs` |
| Action drawer / side panel | `USlideover` |
| Destructive confirmation | `UModal` |
| Loading state | `USkeleton` |
| Empty state | `UEmpty` (or `EmptyStateCard` wrapping it) |
| Error / alert | `UAlert` |
| Write outcome feedback | `UToast` (dismissible) |
| Data table | `UTable` |
| Status label | `UBadge` |
| Form wrapper | `UForm` + `UFormField` + `UInput` / `USelect` / `UTextarea` |
| Primary action | `UButton` |

Add custom CSS only when the Nuxt UI component API cannot express the needed state or layout.

---

## Dashboard Navigation

The sidebar `links` array in `dashboard.vue` must cover:

```ts
[
  { label: 'Overview',       icon: 'i-lucide-layout-dashboard', to: '/' },
  { label: 'Merchants',      icon: 'i-lucide-store',            to: '/admin/merchants' },
  { label: 'Payment Orders', icon: 'i-lucide-receipt',          to: '/admin/merchants' },
  { label: 'Error Lab',      icon: 'i-lucide-flask-conical',    to: '/error-lab' },
]
```

`UDashboardSearch` `groups` must be updated in parallel so search covers all destinations.

---

## Business Screens — Map to Backend Endpoints

Every screen must reflect only what the backend actually exposes. Do not invent business metrics, fake counts, or fake statuses.

| Screen | Primary endpoints |
|---|---|
| Overview | `GET /api/merchants`, `GET /api/merchants/{merchantId}/payment-orders/summary`, `GET /api/merchants/{merchantId}/payment-orders` (recent 10) |
| Merchants | `GET /api/merchants`, `POST /api/merchants`, `GET /api/merchants/{id}`, `POST /api/merchants/{id}/activate`, `POST /api/merchants/{id}/suspend` |
| Payment Orders | `GET .../payment-orders` (filtered + paginated), `POST .../payment-orders`, `GET .../payment-orders/summary` |
| Payment Order Detail | `GET .../payment-orders/{id}` + `GET .../payment-orders/{id}/history` |
| Lifecycle Actions | `POST .../authorize`, `POST .../capture`, `POST .../cancel`, `POST .../refund`, `PATCH .../{id}` |
| Error Lab | Intentional error triggers (400/401/403/404/406/409/412/415/428) |

---

## HTTP Learning Panels — First-Class, Not Hidden

This is a learning project. Selected screens must surface protocol details:

- **`HeaderKeyValuePanel`** — renders `ETag`, `Location`, `Cache-Control`, `Vary`, `X-Correlation-ID` with explicit "not present" for absent headers.
- **`HttpStatusBadge`** — renders status code + leading-digit category (2xx success, 4xx client error, etc.).
- **`ProblemDetailsCard`** — renders `type`, `title`, `status`, `detail`, `instance` from `application/problem+json`. Empty indicator for absent members.
- **`RawJsonViewer`** — indented, multi-line JSON preserving key order. Non-JSON fallback with explicit label.
- **`ApiDebugPanel`** — shows request method, path, masked headers, response status, forwarded response headers, response body.
- **`EtagDisplay`** + **`IfMatchInput`** + **`IdempotencyKeyInput`** — exposed on lifecycle action forms; pre-filled from latest captured values.

Never hide these behind obscure affordances. They are teaching surfaces.

---

## Composable Architecture

### Header-aware transport (`app/composables/useApiClient.ts`)

The foundation composable. Uses `$fetch.raw` and returns:

```ts
interface ApiResponse<T> {
  data: T | null
  status: number
  headers: ApiHeaders          // typed: etag, location, vary, cacheControl, correlationId, allow, acceptPatch
  problem: ProblemDetails | null
  raw: string                  // unmodified body text for RawJsonViewer
}
```

All domain composables delegate transport to `useApiClient`.

### Domain composables

| Composable | Scope |
|---|---|
| `app/composables/useMerchantsApi.ts` | Merchant CRUD + activate/suspend |
| `app/composables/usePaymentOrdersApi.ts` | List, summary, detail, create |
| `app/composables/usePaymentLifecycleApi.ts` | authorize, capture, cancel, refund, PATCH metadata, history |

Composables own: transport, header capture, per-call Zod response validation.
The `payment-orders` Pinia store owns: shared state (`currentOrder`, `versionMarker`, `history`, `lifecycleFeedback`, `getAvailableActions`).

---

## Pinia — Only Where Shared State Is Genuinely Needed

| State | Location | Justification |
|---|---|---|
| Current payment order + latest ETag (`versionMarker`) | `payment-orders` store | Read simultaneously by detail tabs, lifecycle drawer, `IfMatchInput` |
| Lifecycle feedback category | `payment-orders` store | Shared between action drawer and toast/error surfaces |
| History entries | `payment-orders` store | Shared between history tab and timeline |
| Auth/session | `auth` store | Shared across layout + route guards |
| Per-call transport + headers | composable local state | Not shared; no store needed |
| Error Lab transient state | component local state | Single page, not shared |

Do not add new Pinia stores without a clear shared-state justification.

---

## Zod Schemas

| File | Contains |
|---|---|
| `app/schemas/merchant.schema.ts` | `createMerchantSchema` (merchantReference 3–64 + regex, displayName 2–120) |
| `app/schemas/payment-order.schema.ts` | create / response / list / summary / history schemas; currency enum `PLN\|EUR\|USD`; amountMinor max 100,000,000 |
| `app/schemas/problem-details.schema.ts` | `problemDetailsSchema` with `.passthrough()` for extension members |
| `app/schemas/app-shell.schema.ts` | App shell state |

**The existing schemas are the source of truth.** Do not widen them to match the generic bounds in prose specs. The real constraints are stricter and must be preserved.

---

## Reusable Component Inventory

### Extend existing — do not duplicate

| Required component | Existing artifact | Action |
|---|---|---|
| `BusinessStatusBadge` | `PaymentStatusBadge`, `MerchantStatusBadge` | Generalize; keep existing as thin wrappers |
| `PaymentOrderSummaryCards` | `PaymentOrderSummaryCards` | Extend with per-status count cards |
| `CreateMerchantForm` | exists | Add test ids + Zod field messages |
| `MerchantTable` | exists | Add activate/suspend actions + test ids |
| `CreatePaymentOrderForm` | exists | Add `IdempotencyKeyInput`, `ApiDebugPanel`, test ids |
| `PaymentOrderListTable` | exists | Add filter UI, pagination, test ids |
| `PaymentOrderDetail` | exists | Add HTTP headers panel (`HeaderKeyValuePanel`), `RawJsonViewer`, `UTabs` |

### New components

`HttpStatusBadge`, `HeaderKeyValuePanel`, `ProblemDetailsCard`, `RawJsonViewer`, `IdempotencyKeyInput`, `EtagDisplay`, `IfMatchInput`, `PaymentOrderLifecycleActions`, `MerchantStatusCard`, `ApiDebugPanel`, `EmptyStateCard`, `LoadingState`, `ErrorState`, `ConfirmActionModal`

Place new shared components under `app/components/shared/`.

---

## State Surface Rules

| State | Component | Nuxt UI primitive |
|---|---|---|
| In-flight request | `LoadingState` | `USkeleton` or spinner |
| Timeout (10s no response) | `ErrorState` + retry control | `UAlert` |
| Zero items in collection | `EmptyStateCard` (description + next action) | `UEmpty` |
| Request failure (problem+json) | `ErrorState` → `ProblemDetailsCard` | `UAlert` + `UCard` |
| Request failure (other) | `ErrorState` with human-readable message | `UAlert` |
| Write success/failure | `UToast` (dismissible) | `UToast` |

---

## `data-testid` Placement — Required

Every required test id must be stable (byte-identical across rebuilds, sessions, and styling changes) and resolve to exactly one element per rendered page.

| `data-testid` value | Element | Component |
|---|---|---|
| `create-merchant-form` | `<form>` | `CreateMerchantForm` |
| `activate-merchant-button` | `<button>` | `MerchantTable` row action |
| `create-payment-order-form` | `<form>` | `CreatePaymentOrderForm` |
| `payment-order-table` | table root | `PaymentOrderListTable` |
| `payment-order-detail` | detail container | detail page |
| `lifecycle-authorize` | `<button>` | `PaymentOrderLifecycleActions` |
| `lifecycle-capture` | `<button>` | `PaymentOrderLifecycleActions` |
| `lifecycle-cancel` | `<button>` | `PaymentOrderLifecycleActions` |
| `lifecycle-refund` | `<button>` | `PaymentOrderLifecycleActions` |
| `problem-details-card` | card root | `ProblemDetailsCard` |
| `http-headers-panel` | panel root | `HeaderKeyValuePanel` |

Supporting ids: `error-lab-trigger-{status}`, `idempotency-key-input`, `if-match-input`, `etag-display`, `raw-json-viewer`, `api-debug-panel`, `empty-state`, `error-state`, `loading-state`, `confirm-action-modal`.

---

## Accessibility

- Status badges must be distinguishable without color (text label required).
- Every form input must have an associated `UFormField` label.
- `UModal` and `USlideover` must trap focus and restore it on close.
- Visible focus rings must be preserved (do not suppress Nuxt UI defaults).
- Empty and error indicators must use text, not icon-only.
- Honor reduced-motion: `MOTION_INTENSITY 1–3`.

---

## Implementation Discipline

- **Extend existing components and pages.** Do not duplicate.
- Prefer composables over store for transport; use store only for genuinely shared state.
- Keep the Nuxt server proxy as the single authentication boundary.
- Use `$fetch.raw` wherever response headers matter.
- Do not render any data that has not passed its Zod schema.
- Keep each change small and reviewable. One concern per commit.

---

## Accessibility and Testability Baseline

These rules apply to every new or extended component. They are the minimum
required so that future Playwright tests can use stable, semantic locators
without retrofitting.

### Locator strategy (preferred order)
1. `getByRole()` — semantic HTML role + accessible name (most stable)
2. `getByLabel()` — input label association
3. `getByText()` — visible text content
4. `data-testid` — only when the above are insufficient or ambiguous

### Labels and input association
- Every `UInput`, `USelect`, `UTextarea` must be wrapped in `UFormField` with
  a visible `label`. Placeholder text is not a label.
- Error messages must use `aria-invalid="true"` on the input and the
  `UFormField` error slot to associate the message with the control.

### Focus management
- `UModal` and `USlideover` must trap focus when open and return focus to the
  trigger element when closed. Never suppress Nuxt UI defaults for this.
- `UPopover` and `UDropdownMenu` must close on `Escape` and return focus.
- On page navigation or dynamic content injection, focus must move to the
  primary heading or first interactive element of the new content region.

### Keyboard navigation
- All primary user actions must be completable without a mouse.
- Sortable table column headers must be `<button>` elements (not `<div>` with
  `@click`), enabling keyboard activation.
- `USelectMenu` (combobox) must be fully keyboard-navigable with Up/Down,
  Enter, and Escape.

### Status and states
- Status badges (`UBadge`) must convey meaning through visible text, not color
  alone. `aria-label` is required when the badge is icon-only.
- Every primary screen must implement all required states:
  `loading`, `empty`, `error`, `filtered-empty`, `forbidden (403)`, `success`.
- Every error state for a problem+json response must use `ProblemDetailsCard`
  with `role="alert"` or `UAlert`.

### Tables
- `UTable` must have `<th>` with `scope="col"` for column headers.
- Sortable columns use `<button>` in the header cell with `aria-sort`.
- Row-level actions must have accessible names (not icon-only buttons).

### Focus rings
- Never add CSS that removes `:focus-visible` outlines on interactive elements.
  Nuxt UI's default focus rings must be preserved.

### Reduced motion
- Respect `prefers-reduced-motion`. Nuxt UI / Tailwind 4 handles this for
  built-in transitions. For any custom CSS animation, add a `@media
  (prefers-reduced-motion: reduce)` guard.

---

## Modern Web Guidance (Advisory)

For frontend-heavy specs (user-management, audit-log-dashboard, file-import-export,
bulk-actions, etc.), invoke `#modern-web-guidance` before writing requirements
to get advisory web platform guidance on accessibility, layout, performance, and
browser compatibility.

See:
- `.kiro/steering/modern-web-guidance.md` — when/how to call it, CLI, safety
- `docs/ai/modern-web-guidance-spec-review-gate.md` — review gate checklist
- `docs/ai/modern-web-guidance-kiro-workflow.md` — full workflow
