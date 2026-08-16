---
inclusion: manual
---

# Modern Web Guidance (Advisory Layer)

Use Modern Web Guidance as an **advisory input** when designing or reviewing
frontend/UI/UX work for this project. It is not an application dependency and
is not installed in `package.json`.

Invoke with `#modern-web-guidance` in chat when working on any of the
**trigger areas** listed below.

---

## What It Is

GoogleChrome/modern-web-guidance packages curated, up-to-date browser/web platform
guidance into agent-callable skills. It steers agents away from legacy patterns
toward modern HTML, CSS, and Web APIs — with Browser Baseline compatibility data
attached.

Source: https://github.com/GoogleChrome/modern-web-guidance
Docs:   https://developer.chrome.com/docs/modern-web-guidance

**This is advisory, not prescriptive.** Nuxt UI Dashboard primitives and existing
project conventions always take precedence. Modern Web Guidance is consulted to
validate and improve decisions, not to replace them.

---

## When to Use

| Trigger area | Relevant MWG skills to query |
|---|---|
| User Management form / modal / drawer | `forms`, `accessibility`, `user-experience`, `html` |
| Role assignment combobox / dropdown | `accessibility`, `user-experience`, `forms` |
| Audit Log table + date filters / deep links | `accessibility`, `css-layout`, `performance`, `forms` |
| Tenant switcher (PLATFORM_ADMIN) | `accessibility`, `user-experience`, `html` |
| Payment lifecycle confirm modal | `accessibility`, `user-experience`, `forms` |
| Error Lab / problem+json panels | `html`, `accessibility`, `user-experience` |
| Responsive dashboard layout | `css-layout`, `performance`, `user-experience` |
| Security headers / cookies / token handling | `security`, `privacy` |
| File import/export (future) | `forms`, `accessibility`, `security`, `privacy` |
| Bulk actions / multi-select (future) | `accessibility`, `user-experience`, `html` |
| Focus management (modal / drawer / popover) | `accessibility`, `user-experience` |
| Keyboard navigation | `accessibility`, `html` |
| INP / performance | `performance`, `css-layout` |
| Browser compatibility / Baseline | `html`, `css`, `css-layout` |

---

## When NOT to Use

- When the answer is clearly in Nuxt UI docs or existing `frontend-nuxt-ui.md` steering.
- For backend Java/Spring/security work (use backend skills).
- For Playwright test writing (writing tests is explicitly out of scope here).
- For Keycloak realm configuration changes.
- When the existing project code already has an established, tested pattern.
- When you are in the middle of implementing a task — research first, then implement.

---

## How to Call It

### Via CLI (non-destructive, no install needed)

```bash
# Search for guidance on a topic
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "accessible form validation in modal dialog"

# Search for focus management
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "focus management drawer modal popover"

# Search for responsive table dashboard
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "responsive data table dashboard filters"

# Search for INP / interaction performance
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "optimize interaction to next paint dashboard table"

# Search for CSP / security
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest search "content security policy cookies XSS Nuxt app"

# Retrieve a specific guide by ID (get ID from search results)
DISABLE_TELEMETRY=1 npx modern-web-guidance@latest retrieve "<guide-id>"
```

`DISABLE_TELEMETRY=1` opts out of anonymous usage statistics without modifying
the shell profile. Use it consistently.

### Via Kiro Skill (if available in Skill Manager)

Modern Web Guidance is distributed as an agent skill. Check:
**Settings → AI Assistant → Skills → Search "modern-web-guidance"**

If available, install there for IDE-integrated access. The CLI fallback above
works regardless.

---

## Workflow

1. **Frame the UI problem locally first.** Know which Nuxt UI component you intend
   to use and what specific concern (accessibility, layout, performance, security)
   you want to validate.

2. **Search** using one or more queries above. Keep queries short and topical.
   Never include project secrets, tokens, user data, or private business logic.

3. **Retrieve** the top 1–2 most relevant guides.

4. **Summarize** the retrieved guidance before applying it. Note any Browser
   Baseline compatibility constraints (e.g., "CSS anchor positioning is Baseline
   Newly Available — add fallback").

5. **Map back to Nuxt UI.** The guidance describes web platform patterns. Always
   translate to the Nuxt UI equivalent:

   | MWG pattern | Nuxt UI equivalent |
   |---|---|
   | `<dialog>` / focus trap | `UModal` (built-in focus trap) |
   | `<details>` / disclosure | `UCollapsible` or `UTabs` |
   | Native `<select>` | `USelectMenu` (keyboard-accessible) |
   | Combobox pattern | `USelectMenu` with search |
   | Live region / status | `UToast` or `UAlert` with `role="status"` |
   | Sticky table header | `UTable` with Tailwind `sticky` |
   | Form validation messages | `UFormField` error slot |
   | Date input | `UInput type="date"` or `UPopover` + calendar |
   | Drawer/panel | `USlideover` |

6. **Document the decision.** If guidance changes a design decision, note it in
   the spec or design doc with a brief reason.

---

## Safety Constraints

- Do not paste JWT tokens, Keycloak credentials, database passwords, bearer tokens,
  or any secret into search queries.
- Do not include private business data, merchant IDs, or tenant-specific data.
- Do not replace Nuxt UI components with custom native implementations unless:
  (a) there is a documented accessibility or compatibility gap, and
  (b) the replacement is explicitly approved.
- Do not introduce experimental web APIs (`baseline: false`) without:
  (a) a documented fallback, and
  (b) explicit user approval.
- Nuxt UI and existing project patterns take precedence over MWG recommendations
  when they conflict.

---

## Telemetry / Privacy

Modern Web Guidance may collect anonymous usage statistics.
Use `DISABLE_TELEMETRY=1` as a prefix to all `npx modern-web-guidance@latest`
calls to opt out without modifying the shell profile or any system file.

Do not modify `~/.bashrc`, `~/.zshrc`, or any shell profile for this opt-out
unless explicitly approved.

---

## Project-specific context

- **Nuxt**: 4.4.6 (actual, verified from `apps/frontend/package.json`)
- **@nuxt/ui**: 4.7.1 (actual — the "4.7" in the project refers to @nuxt/ui, not Nuxt itself)
- **Playwright**: 1.60.0 (not 1.61 — `page.clock` and ARIA snapshot API differ slightly)
- **Tailwind**: 4.3.0 (CSS-first config, not `tailwind.config.js`)
- **Auth**: nuxt-auth-utils 0.5.0; token is server-side only; never client-accessible
- **Backend**: Spring Boot 4, Spring Modulith, PostgreSQL 18, Keycloak via OIDC JWT
