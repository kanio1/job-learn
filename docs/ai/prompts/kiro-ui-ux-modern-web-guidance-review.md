# Kiro UI/UX Analysis Prompt — Modern Web Guidance Review

Use this template at the start of any **frontend-heavy spec** to produce a
structured UI/UX analysis before writing requirements or design tasks.

Replace `{SPEC_NAME}` with the feature name (e.g. `user-management`).

---

## Prompt

```
Run a UI/UX analysis for the spec: {SPEC_NAME}

DO NOT implement any features. DO NOT write Playwright tests.
DO NOT modify application code. Produce analysis only.

---

### Step 1 — Inspect the relevant codebase

Read and summarise:

Frontend:
- apps/frontend/app/pages/   (existing routes that may be extended)
- apps/frontend/app/components/  (reusable components to extend, not duplicate)
- apps/frontend/app/composables/ (transport and API composables)
- apps/frontend/app/stores/       (shared state — justify any new store)
- apps/frontend/app/schemas/      (Zod schemas — existing bounds are authoritative)
- apps/frontend/app/middleware/   (route guards)
- apps/frontend/server/api/       (proxy routes)
- apps/frontend/app/layouts/      (dashboard layout)
- .kiro/steering/frontend-nuxt-ui.md  (component preferences, data-testid rules)

Backend (for traceability):
- apps/backend endpoints visible in MerchantController, PaymentOrderController
- Security authorities from Authorities.java
- Tenant scope from tenant module (if implemented)
- Error shapes from exception handlers

---

### Step 2 — Run Modern Web Guidance searches

Before designing any screen, run these searches using the modern-web-guidance
steering (#modern-web-guidance). Summarise the top guidance before proceeding:

1. Search: "accessible form validation {SPEC_NAME}"
2. Search: "focus management modal dialog drawer"
3. Search: "responsive data table filter pagination"
4. Search: "keyboard navigation combobox dropdown"
5. Search: "{SPEC_NAME} accessibility ARIA pattern"

For each search result used, note:
- Guide ID and title
- Key recommendation
- Browser Baseline status (if relevant)
- Nuxt UI mapping

---

### Step 3 — Produce these seven matrices

#### Matrix 1 — Backend → Frontend Traceability

| Backend capability | Endpoint / action | Authority / role | Tenant scope | Frontend route | Nuxt UI component | User action | HTTP error shapes | Missing? | Recommendation |
|---|---|---|---|---|---|---|---|---|---|

Map every backend endpoint to the page, component, composable, and Zod schema
that will consume it. Mark Missing for gaps that need new artifacts.

Include:
- problem+json → UI error mapping (ProblemDetailsCard)
- ETag/If-Match → stale/conflict UX
- Idempotency-Key → safe retry UX
- X-Correlation-ID → support/debug UX

#### Matrix 2 — Role-based User Journey

| Role | Can see | Can do | Cannot do | UI surfaces (nav/actions hidden) | Future Playwright scenario |
|---|---|---|---|---|---|
| PLATFORM_ADMIN | | | | | |
| TENANT_ADMIN | | | | | |
| MERCHANT_MANAGER | | | | | |
| SUPPORT_AGENT | | | | | |
| READ_ONLY_USER | | | | | |

Note: backend enforces these rules. Frontend hides/disables for UX only.
Backend enforcement is the authoritative gate.

#### Matrix 3 — Screen Inventory

| Screen / route | New or Extend | Primary Nuxt page | Key components | Pinia store needed? (justify) | Zod schemas needed? |
|---|---|---|---|---|---|

Strongly prefer EXTEND over NEW. Create new pages only for genuinely new routes.

#### Matrix 4 — Nuxt UI Component Recommendation

| UI need | Nuxt UI component | Alternative considered | Why chosen | MWG guidance applied? |
|---|---|---|---|---|
| Form wrapper | UForm + UFormField | | | |
| Table | UTable | | | |
| Modal (create / confirm) | UModal | | | |
| Drawer (edit / detail) | USlideover | | | |
| Combobox / multi-select | USelectMenu | | | |
| Date filter | UInput type="date" or UPopover | | | |
| Status badge | UBadge | | | |
| Feedback toast | UToast | | | |
| Loading state | USkeleton | | | |
| Empty state | UEmpty | | | |
| Error state | UAlert + ProblemDetailsCard | | | |
| Pagination | UPagination | | | |
| Tabs | UTabs | | | |

Do NOT replace existing Nuxt UI components with custom native implementations
unless there is a documented accessibility or compatibility gap.

#### Matrix 5 — UI State Inventory

For each primary screen, list all required states:

| Screen | loading | empty | error (problem+json) | filtered-empty | forbidden (403) | success (toast) | stale / conflict (412) | Notes |
|---|---|---|---|---|---|---|---|---|

Every screen must handle at minimum: loading, empty, error, forbidden.

#### Matrix 6 — Accessibility and Testability

| UI element | Locator strategy (preferred) | data-testid needed? | Label / accessible name | Keyboard nav required | Focus management | ARIA role/attribute | MWG note |
|---|---|---|---|---|---|---|---|
| Primary form | getByRole('form') or getByLabel | No | form label | Tab through fields | Focus on open | — | |
| Submit button | getByRole('button', {name:}) | No | visible text | Enter | — | — | |
| Modal root | getByRole('dialog') | No | aria-label / aria-labelledby | Trap focus | Return on close | dialog | |
| Drawer root | getByRole('complementary') | No | aria-label | Trap focus | Return on close | — | |
| Table | getByRole('table') | No | caption | Tab to rows | — | table | |
| Sort column | getByRole('button') inside th | No | column name | Enter/Space | — | aria-sort | |
| Status badge | getByText() | No | visible text (not color) | — | — | — | |
| Action button | getByRole('button', {name:}) | Only if ambiguous | visible text | Enter | — | — | |
| Error message | getByRole('alert') | No | visible text | — | — | alert | |

Prefer semantic locators. Add `data-testid` only when role/label/text is
insufficient or unstable.

#### Matrix 7 — Future Playwright/SDET Learning Value

| Screen / flow | Future test type | Playwright technique | Complexity | Learning value | Priority |
|---|---|---|---|---|---|
| | Happy path | page.goto, getByRole, fill, click | Basic | — | |
| | Validation error | getByText(error), toBeVisible | Basic | — | |
| | Modal open/close | getByRole('dialog'), Esc | Basic | — | |
| | Drawer + unsaved guard | page.on('dialog'), accept/dismiss | Advanced | — | |
| | RBAC visibility per role | role fixture, expect visible/hidden | Senior | — | |
| | 403 enforcement | direct URL, expect forbidden text | Senior | — | |
| | Stale If-Match (412) | ETag mismatch, problem+json | Advanced | — | |
| | Idempotency replay (409/200) | repeat request, check response | Advanced | — | |
| | Audit trail verification | action → check audit table | Advanced | — | |

---

### Step 4 — Produce a prioritised UI/UX backlog

Based on the matrices, list:

1. **Critical (block spec):** gaps that must be resolved before requirements can be written
2. **High (include in spec):** components/patterns needed for the first release
3. **Medium (note in spec, implement later):** improvements that add learning value
4. **Low (defer):** nice-to-have, not needed for core flow

Format:

| Priority | Item | Rationale | Effort | Nuxt UI component | MWG guidance applied |
|---|---|---|---|---|---|

---

### Step 5 — Confirm no implementation was done

Verify:
- [ ] No Vue/TS application files were created or modified
- [ ] No Playwright test files were created
- [ ] No package.json was modified
- [ ] No Keycloak realm files were modified
- [ ] No database migration files were modified
- [ ] No steering files were modified
- [ ] No secrets or private data were included in MWG search queries
```

---

## Notes on Using This Template

- Run this **once per spec**, at the start of the requirements phase.
- Paste the completed matrices into the spec's `design.md` in a "UI/UX Analysis"
  section, or into a separate `ui-analysis.md` file in the spec folder.
- The Backend→Frontend Traceability matrix replaces the need for a separate
  `backend-frontend-traceability.md` steering file (it lives in each spec, not
  as always-on steering).
- Spec task estimates should reference the Screen Inventory and State Inventory
  matrices to ensure no states are forgotten.
