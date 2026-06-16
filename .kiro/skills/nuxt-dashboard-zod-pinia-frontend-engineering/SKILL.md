---
name: nuxt-dashboard-zod-pinia-frontend-engineering
description: Use when designing, reviewing, or implementing the Nuxt 4 dashboard frontend with Nuxt UI Dashboard template, TypeScript 6, Zod, and Pinia so it stays clear, testable, and appropriate for QA-driven learning.
---

# Nuxt Dashboard, Zod and Pinia Frontend Engineering

Shape Nuxt 4 dashboard work so it stays component-first, clear, testable and visually polished without drifting away from the Nuxt UI Dashboard Template.

## Use when
- designing or reviewing Nuxt dashboard pages, routes, layouts, cards, tables, metrics, navigation, forms, empty states and admin-style workflows,
- deciding Zod schemas, form boundaries, Pinia stores and component state for dashboard behavior,
- improving hierarchy, density, spacing, alignment, loading/error/empty states or responsive behavior,
- reviewing frontend testability and explaining UI architecture to a QA/SDET learner.

## When Not to Use
Do not use this for Playwright framework mechanics, backend behavior, backend authorization policy, OAuth/OIDC integration, pure API testing, or replacing Nuxt UI conventions with a custom visual system.

## Dashboard Taste Dials
- `DESIGN_VARIANCE: 3-5` — dashboards need consistency.
- `MOTION_INTENSITY: 1-3` — prefer focus, hover, disclosure and state transitions. Avoid spectacle.
- `VISUAL_DENSITY: 5-7` — operational screens should be scannable and compact.

## Nuxt UI Dashboard Rules
- Start from the Nuxt UI Dashboard Template structure, tokens, color mode behavior, navigation patterns, density and component APIs.
- Prefer Nuxt UI components before custom CSS.
- Improve hierarchy before adding decoration: page title, primary action, filters, table/card grouping, status badges and destructive actions should read in the right order.
- Keep copy functional. Labels, helper text, action names and error messages should say what the user can decide or do next.
- Do not invent fake payment capabilities, fake metrics or fake-precise numbers that imply unsupported business behavior.
- Preserve accessibility: labelled controls, visible focus, keyboard-friendly disclosures, contrast, non-color-only status and reduced-motion-safe transitions.

## Existing Screen Redesign Workflow
1. Audit the current route, component tree, Nuxt UI usage, data states and responsive behavior.
2. Identify the user decision or operational task the screen supports.
3. Preserve established Nuxt UI Dashboard Template patterns before changing visuals.
4. Improve hierarchy, density, spacing and alignment before adding new styling.
5. Add only meaningful states: loading, empty, error, disabled, selected, filtered and destructive flows where relevant.
6. Verify desktop and mobile behavior.
7. Document the trade-off briefly when choosing custom layout or CSS over the template default.

## Kiro Spec Mode integration
- When a change is architectural (new page, new composable layer, major component restructure), use Kiro Spec Mode and produce requirements.md → design.md → tasks.md before implementation.
- Prefer small, reviewable changes.
- Ask for approval before large implementation steps.
- Use `.kiro/steering/frontend-nuxt-ui.md` as persistent context.

## Boundaries With Other Skills
- Use `typescript6-playwright-engineering` for fixtures, page objects, auth setup, browser workers and E2E execution strategy.
- Use backend, security or REST API skills for authorization, API contracts and server behavior.

See `.kilocode/skills/nuxt-dashboard-zod-pinia-frontend-engineering/references/frontend-testability-checklist.md`.
