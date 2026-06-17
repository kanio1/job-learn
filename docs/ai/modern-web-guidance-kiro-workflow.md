# Modern Web Guidance — Kiro Workflow

## What and Why

Modern Web Guidance (https://github.com/GoogleChrome/modern-web-guidance) is a
GoogleChrome project that packages curated web platform expertise as agent-callable
skills. It provides guidance on accessibility, forms, layout, performance, security,
and browser compatibility with attached Baseline data.

**Why use it here:** This project's learning goal is to build a realistic,
testable, accessible multi-tenant payment backoffice. Modern Web Guidance keeps
frontend/UI/UX design aligned with modern web platform standards — catching
legacy patterns before they become tech debt and ensuring future Playwright tests
can rely on semantic HTML and accessible UI structures.

**It is advisory, not a required dependency.** It is not in `package.json` and
should never be added there. Nuxt UI Dashboard primitives and existing project
steering always take precedence.

---

## When to Use

Invoke with `#modern-web-guidance` in chat before designing or reviewing any
of these screen types:

| Scenario | MWG skills to query | Project mapping |
|---|---|---|
| User Management form/modal/drawer | `forms`, `accessibility`, `user-experience`, `html` | `UModal`/`USlideover` + `UForm`/`UFormField` |
| Role assignment combobox | `accessibility`, `user-experience`, `forms` | `USelectMenu` with search/keyboard |
| Audit Log table + date filters | `accessibility`, `css-layout`, `performance`, `forms` | `UTable` + `UInput type=date` / `UPopover` |
| Tenant switcher (PLATFORM_ADMIN) | `accessibility`, `user-experience`, `html` | `USelectMenu` in `UDashboardNavbar` |
| Payment lifecycle confirmation modal | `accessibility`, `user-experience`, `forms` | `UModal` + `ConfirmActionModal` |
| Error Lab / problem+json panels | `html`, `accessibility`, `user-experience` | `ProblemDetailsCard`, `HeaderKeyValuePanel` |
| Responsive dashboard layout | `css-layout`, `performance`, `user-experience` | `UDashboardGroup` + Tailwind 4 |
| Security headers / cookies / token | `security`, `privacy` | `backendApi.ts`, auth middleware |
| File import/export (future) | `forms`, `accessibility`, `security`, `privacy` | `UInput type=file`, download handling |
| Focus management (modal/drawer/popover) | `accessibility`, `user-experience` | `UModal`, `USlideover`, `UPopover` |
| Keyboard navigation | `accessibility`, `html` | `USelectMenu`, combobox, `UTable` sort |
| INP / interaction performance | `performance`, `css-layout` | `UTable` large data, animations |
| Browser compatibility / Baseline | `html`, `css`, `css-layout` | Tailwind 4 utilities, native APIs |

---

## When NOT to Use

- When the pattern is already documented in `.kiro/steering/frontend-nuxt-ui.md`.
- For backend Java, Spring, Keycloak, or PostgreSQL work.
- For Playwright test writing (tests are written later, by hand, in separate lessons).
- For Keycloak realm file changes.
- When implementing a task (research before implementation, not during).
- When existing app code already has a tested, working pattern.

---

## How to Search and Retrieve

### CLI (preferred non-destructive method)

```bash
# Opt out of anonymous telemetry without modifying shell profile
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "<query>"

# Retrieve a guide by its ID
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest retrieve "<guide-id>"
```

**Note (verified June 2026):** The `npx modern-web-guidance@latest` CLI is available
and exits with code 0, but produces no output in network-restricted environments.
If the CLI produces no output, use the Kiro Skill Manager installation as the
primary access path.

### Kiro Skill Manager (preferred when available)

**Settings → AI Assistant → Skills → Search "modern-web-guidance" → Install**

Once installed as a Kiro skill, invoke guidance directly in chat without a CLI.
This is the recommended path for IDE-integrated use.

### Telemetry / Privacy

`DISABLE_TELEMETRY=1` as a command prefix opts out of anonymous statistics.
Do not modify `~/.bashrc`, `~/.zshrc`, or any shell profile for this purpose.
Do not add any environment variable to the app or its CI configuration.

---

## How to Map Guidance to Nuxt UI

Modern Web Guidance describes web platform patterns in terms of native HTML/CSS/JS.
Always translate to the Nuxt UI equivalent:

| MWG / web platform pattern | Nuxt UI (@nuxt/ui 4.7.1) equivalent |
|---|---|
| `<dialog>` / focus trap | `UModal` — built-in focus trap + Esc close |
| Drawer / side panel | `USlideover` |
| Native `<select>` | `USelectMenu` — keyboard-accessible, filterable |
| Combobox (ARIA) | `USelectMenu` with `searchable` prop |
| Status / live region | `UToast` (`role="status"`) or `UAlert` |
| Sticky header table | `UTable` + Tailwind `sticky top-0` |
| Form validation messages | `UFormField` `error` slot / `help` prop |
| Date input | `UInput type="date"` or `UPopover` + custom calendar |
| Badge / chip | `UBadge` |
| Pagination | `UPagination` |
| Tabs | `UTabs` |
| Accordion | `UCollapsible` |
| Empty state | `UEmpty` |
| Skeleton / loading | `USkeleton` |
| Alert / banner | `UAlert` |
| Popover | `UPopover` |
| Dropdown menu | `UDropdownMenu` |

---

## Avoiding Context Bloat

The `#modern-web-guidance` steering is `inclusion: manual` — it is only loaded
when explicitly invoked. Do not add it to always-on steering.

When creating a spec for a frontend-heavy feature, run the review gate
(`docs/ai/modern-web-guidance-spec-review-gate.md`) **once** at spec start, not
in every individual task. Summarize retrieved guidance in the design doc; do not
repeat full guide content in tasks.

---

## How This Supports Playwright/SDET Learning

Every MWG recommendation that gets applied produces:
- Semantic HTML → `getByRole()` locators that are stable by construction.
- Accessible labels → `getByLabel()` locators without fragile CSS selectors.
- Keyboard navigation → keyboard-driven test flows.
- Visible text for status → `getByText()` assertions that don't depend on color.
- Focus management → `page.keyboard` / focus assertions in modal/drawer tests.
- Proper ARIA → `toMatchAriaSnapshot()` assertions (Playwright 1.61+).

The goal is that when you write Playwright lessons later, the UI is already
"test-friendly by design" — not retrofitted.

---

## Related Steering Files

- `.kiro/steering/modern-web-guidance.md` — when/how to invoke, CLI commands, safety
- `.kiro/steering/frontend-nuxt-ui.md` — Nuxt UI Dashboard patterns, component map,
  data-testid rules, accessibility baseline (the primary steering for frontend work)
- `.kiro/steering/testing-strategy.md` — Playwright conventions (for when tests are written)
- `docs/ai/prompts/kiro-ui-ux-modern-web-guidance-review.md` — full analysis template
- `docs/ai/modern-web-guidance-spec-review-gate.md` — review gate for frontend specs
