---
name: nuxt-dashboard-zod-pinia-frontend-engineering
description: Shape the Nuxt 4 dashboard frontend built from Nuxt Dashboard Template with Nuxt UI, TypeScript 6, Zod and Pinia so it stays clear, testable and appropriate for QA-driven learning.
license: MIT
metadata:
  category: frontend-engineering
  author: project-custom
  version: "3.1.0"
---

# Nuxt Dashboard, Zod and Pinia Frontend Engineering

Shape Nuxt 4 dashboard work so it stays component-first, clear, testable and visually polished without drifting away from the Nuxt UI Dashboard Template. This skill adapts anti-slop frontend ideas from the MIT-licensed Taste Skill project as project-specific guidance, not as a wholesale import.

## Use when
- designing or reviewing Nuxt dashboard pages, routes, layouts, cards, tables, metrics, navigation, forms, empty states and admin-style workflows,
- deciding Zod schemas, form boundaries, Pinia stores and component state for dashboard behavior,
- improving hierarchy, density, spacing, alignment, loading/error/empty states or responsive behavior,
- reviewing frontend testability and explaining UI architecture to a QA/SDET learner.

## When Not to Use
Do not use this for Playwright framework mechanics, backend behavior, backend authorization policy, OAuth/OIDC integration, pure API testing, greenfield marketing landing pages or replacing Nuxt UI conventions with a custom visual system.

## Dashboard Taste Dials
- `DESIGN_VARIANCE: 3-5` by default. Dashboards need consistency; use mild asymmetry only when it clarifies workflow or scan order.
- `MOTION_INTENSITY: 1-3` by default. Prefer focus, hover, active, disclosure and route/state transitions. Avoid spectacle, scroll hijacks and animation that hides data.
- `VISUAL_DENSITY: 5-7` by default. Operational screens should be scannable and compact, with enough whitespace to separate decisions.

## Nuxt UI Dashboard Rules
- Start from the Nuxt UI Dashboard Template structure, tokens, color mode behavior, navigation patterns, density and component APIs.
- Prefer Nuxt UI components before custom CSS. Add custom CSS only when the component library cannot express the needed state or layout cleanly.
- Improve hierarchy before adding decoration: page title, primary action, filters, table/card grouping, status badges and destructive actions should read in the right order.
- Use restrained polish: consistent card treatment, aligned gutters, readable numeric data, meaningful badges, clear dividers and deliberate empty states.
- Keep copy functional. Labels, helper text, action names and error messages should say what the user can decide or do next.
- Do not invent fake payment capabilities, fake metrics or fake-precise numbers that imply unsupported business behavior.
- Preserve accessibility: labelled controls, visible focus, keyboard-friendly disclosures, contrast, non-color-only status and reduced-motion-safe transitions.

## Existing Screen Redesign Workflow
1. Audit the current route, component tree, Nuxt UI usage, data states and responsive behavior.
2. Identify the user decision or operational task the screen supports.
3. Preserve established Nuxt UI Dashboard Template patterns before changing visuals.
4. Improve hierarchy, density, spacing and alignment before adding new styling.
5. Add only meaningful states and interactions: loading, empty, error, disabled, selected, filtered and destructive flows where relevant.
6. Verify desktop and mobile behavior, including navigation and table/card collapse.
7. Document the trade-off briefly when choosing custom layout or CSS over the template default.

## Boundaries With Other Skills
- Use `typescript6-playwright-engineering` for fixtures, page objects, auth setup, browser workers and E2E execution strategy.
- Use backend, security or REST API skills for authorization, API contracts and server behavior.
- Use this skill to make UI testable; do not prescribe Playwright implementation mechanics here.

See `references/frontend-testability-checklist.md` for the anti-slop dashboard checklist and testability review points.
