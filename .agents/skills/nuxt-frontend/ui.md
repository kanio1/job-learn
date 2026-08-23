# Dashboard UI taste, a11y and frontend testability

Use with [SKILL.md](SKILL.md) when designing or reviewing dashboard screens: hierarchy,
density, states, forms, or explaining UI architecture to a QA/SDET learner. Product
placement rules stay in SKILL.md; Playwright mechanics are `playwright-pom`.

## Taste dials

- `DESIGN_VARIANCE: 3-5` — dashboards need consistency.
- `MOTION_INTENSITY: 1-3` — focus, hover, disclosure, state transitions. No spectacle.
- `VISUAL_DENSITY: 5-7` — operational screens should be scannable and compact.

## Nuxt UI dashboard rules

- Start from the Nuxt UI Dashboard Template structure, tokens, color mode behavior, navigation patterns and component APIs.
- Prefer Nuxt UI components before custom CSS.
- Improve hierarchy before adding decoration: page title, primary action, filters, table/card grouping, status badges, destructive actions read in that order.
- Keep copy functional. Labels, helper text, action names, error messages say what the user can decide or do next.
- Do not invent fake payment capabilities, fake metrics, or fake-precise numbers implying unsupported business behavior.
- Preserve accessibility: labelled controls, visible focus, keyboard-friendly disclosures, contrast, non-color-only status, reduced-motion-safe transitions.

## Existing screen redesign workflow

1. Audit the current route, component tree, Nuxt UI usage, data states, responsive behavior.
2. Identify the user decision or operational task the screen supports.
3. Preserve established template patterns before changing visuals.
4. Improve hierarchy, density, spacing, alignment before adding new styling.
5. Add only meaningful states: loading, empty, error, disabled, selected, filtered, destructive.
6. Verify desktop and mobile behavior.
7. Document the trade-off briefly when choosing custom layout/CSS over template defaults.

## Testability checklist

Anti-slop checks:

- Screen preserves Nuxt UI Dashboard navigation, density, styling, color mode, spacing conventions?
- Nuxt UI components/tokens used before custom CSS?
- Information hierarchy obvious: page purpose, primary action, filters, data, secondary/destructive actions?
- Cards, tables, badges, buttons, form controls visually consistent across the route?
- Gradients, glows, decorative charts, oversized hero blocks avoided unless communicating real state?
- Labels specific and functional, not marketing copy? Metrics clearly real, fixture-backed, or labelled as sample?

State and accessibility:

- Predictable loading/error/empty states; empty states explain how to populate/recover; error states name failed action + next step.
- Disabled/destructive states visually and semantically clear; color not the only status signal; focus visible in light and dark mode; responsive collapse designed for mobile.

Forms, Zod, Pinia:

- Forms align with Zod schemas (`app/schemas/`); validation messages near the field, tester-understandable.
- Placeholder never the only label; Pinia state necessary and scoped; derived display state not duplicated across store and component; role-aware UI does not pretend to be security.

Automation friendliness:

- Important controls have stable accessible names; test ids only where accessible selectors would be brittle.
- Loading/empty/error/success reachable deterministically in tests; stable row identity without relying on visual position.
