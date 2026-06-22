# Modern Web Guidance — Frontend Spec Review Gate

Run this gate **before writing requirements or design tasks** for any
frontend-heavy spec. It takes ~15–30 minutes and prevents rework.

---

## Specs That Require This Gate

| Spec | Status | Gate required |
|---|---|---|
| `user-management` | Next up | **Required** |
| `audit-log-dashboard` | Phase 3 | Required |
| `file-import-export` | Phase 4 | Required |
| `bulk-actions` | Phase 4 | Required |
| `notifications-and-settings` | Phase 4 | Required |
| `responsive-readiness` | Phase 4 | Required |
| Any spec with ≥ 2 new screens | — | Required |
| Backend-only or infrastructure specs | — | Skip |

---

## Gate Checklist

Complete each section before proceeding to spec requirements.

### 1. Modern Web Guidance Searches

Invoke `#modern-web-guidance` and run at minimum:

```
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "accessible {feature} form"
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "focus management modal drawer"
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "{feature} keyboard navigation ARIA"
```

- [ ] Searches run (or MWG Skill Manager used)
- [ ] Top 1–2 guides retrieved and summarised
- [ ] Browser Baseline status noted for any new API used
- [ ] No secrets / private data included in queries

### 2. Nuxt UI Dashboard Component Mapping

For every UI need in the spec, confirm the Nuxt UI component:

- [ ] Every form uses `UForm` + `UFormField` + `UInput`/`USelect`/`UTextarea`
- [ ] Every modal uses `UModal` (not a custom `<dialog>`)
- [ ] Every drawer uses `USlideover` (not a custom panel)
- [ ] Every status label uses `UBadge` with visible text (not color only)
- [ ] Every table uses `UTable`
- [ ] Every loading state uses `USkeleton`
- [ ] Every empty state uses `UEmpty` or `EmptyStateCard`
- [ ] Every error/alert uses `UAlert` + `ProblemDetailsCard` for problem+json
- [ ] Every toast uses `UToast` (dismissible)
- [ ] Every dropdown/combobox uses `USelectMenu`
- [ ] Pagination uses `UPagination`
- [ ] Tabs use `UTabs`
- [ ] No custom native `<dialog>` / `<select>` / `<details>` without justification

### 3. Accessibility Checklist

- [ ] Every form input has a `UFormField` label (not placeholder-only)
- [ ] Error messages use `aria-invalid` and are associated with the input
- [ ] Modal / drawer traps focus and restores focus on close
- [ ] Tables have `<th>` with scope; sortable columns are `<button>` elements
- [ ] Status indicators use visible text, not color alone
- [ ] Keyboard navigation works without a mouse for all primary flows
- [ ] Focus ring is visible (do not suppress Nuxt UI defaults)
- [ ] `data-testid` added only where semantic locator is insufficient

### 4. UI State Checklist

For every new or extended screen:

- [ ] `loading` state defined (USkeleton)
- [ ] `empty` state defined (UEmpty / EmptyStateCard + next action)
- [ ] `error` state defined (UAlert / ProblemDetailsCard for problem+json)
- [ ] `filtered-empty` state defined (different message from plain empty)
- [ ] `forbidden (403)` state defined (ForbiddenPage or inline indicator)
- [ ] `success` state defined (UToast, dismissible)
- [ ] `stale / conflict (412)` state defined where ETag/If-Match is used
- [ ] `timeout (10 s)` state defined with retry control

### 5. Route-to-Role Matrix

Confirm for every new route:

- [ ] Which roles can navigate to this route (sidebar link visibility)
- [ ] Which roles can perform write actions (buttons hidden/disabled)
- [ ] Which roles receive 403 if they attempt unauthorized actions directly
- [ ] Forbidden page or inline 403 indicator is present
- [ ] Backend enforces all rules independently of frontend hiding

### 6. Endpoint-to-Screen Matrix

- [ ] Every backend endpoint consumed by this spec is listed with its route and component
- [ ] Every `problem+json` error shape is mapped to a UI error state
- [ ] `ETag` / `If-Match` flows are mapped to UI concurrency feedback
- [ ] `Idempotency-Key` flows are mapped to UI retry behavior
- [ ] `X-Correlation-ID` is forwarded and visible in `ApiDebugPanel` or logs

### 7. Security Enforcement Reminder

- [ ] Backend enforces all authorization rules regardless of UI state
- [ ] No bearer token reaches the browser (server-side proxy only)
- [ ] `Authorization` header is masked in all debug panels
- [ ] No secret, token, or private data is included in MWG search queries
- [ ] No experimental web API is used without a documented fallback

### 8. Playwright / SDET Testability

- [ ] Every primary user action is achievable via semantic locator
  (`getByRole`, `getByLabel`, `getByText`)
- [ ] `data-testid` attributes planned for ambiguous or dynamically rendered elements
- [ ] All 5 future Playwright learning scenarios identified in Matrix 7
  of the UI/UX analysis template

---

## Gate Sign-off

Before proceeding to requirements:

```
Gate sign-off: {SPEC_NAME}
Date: {DATE}
MWG searches run: yes / no (Skill Manager)
Blocking issues found: list or "none"
Components confirmed: yes
a11y baseline confirmed: yes
UI states confirmed: yes
Security reminder: yes
```

If any blocking issue is found, resolve it before writing requirements.
Document the resolution in the spec's `design.md`.

---

## How to Run This Gate

1. Open a new chat in Kiro.
2. Type: `#modern-web-guidance` to load the advisory steering.
3. Paste the analysis prompt from
   `docs/ai/prompts/kiro-ui-ux-modern-web-guidance-review.md`
   with `{SPEC_NAME}` replaced.
4. Review the 7 matrices produced.
5. Complete the checklist above.
6. Proceed to requirements.

---

## Related Files

- `.kiro/steering/modern-web-guidance.md` — when/how to invoke, CLI, safety
- `.kiro/steering/frontend-nuxt-ui.md` — component map, data-testid, accessibility baseline
- `docs/ai/modern-web-guidance-kiro-workflow.md` — full workflow documentation
- `docs/ai/prompts/kiro-ui-ux-modern-web-guidance-review.md` — analysis template
