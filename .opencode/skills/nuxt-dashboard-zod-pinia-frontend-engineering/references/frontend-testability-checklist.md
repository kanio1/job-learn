# Frontend Testability and Dashboard Taste Checklist

## Anti-Slop Dashboard Checks
- Does the screen preserve Nuxt UI Dashboard Template navigation, density, component styling, color mode and spacing conventions?
- Are Nuxt UI components and tokens used before custom CSS?
- Is the information hierarchy obvious: page purpose, primary action, filters, data, secondary actions and destructive actions?
- Are cards, tables, badges, buttons and form controls visually consistent across the route?
- Are gradients, glows, decorative charts, oversized hero blocks and random illustrations avoided unless they communicate a real dashboard state?
- Are labels specific and functional instead of vague marketing copy?
- Are metrics and examples clearly real, fixture-backed or labelled as sample data?
- Does any fake data accidentally imply unsupported payment business functionality?
- Is animation limited to useful feedback or state transitions, with reduced-motion safety?

## State and Accessibility Checks
- Stable accessible labels?
- Predictable loading/error/empty states?
- Empty states explain how to populate or recover from the state?
- Error states identify the failed action and next step?
- Disabled and destructive states are visually and semantically clear?
- Color is not the only status signal?
- Focus states remain visible in light and dark mode?
- Responsive collapse is designed for mobile, not left to accidental wrapping?

## Forms, Zod and Pinia Checks
- Forms align with Zod schemas?
- Validation messages are near the field and understandable to a tester?
- Placeholder text is not used as the only label?
- Pinia state is necessary, scoped and not a replacement for local component state?
- Derived display state is not duplicated across store and component without a reason?
- Role-aware UI does not pretend to be security?

## Automation Friendliness Checks
- Important controls have stable accessible names?
- Useful test ids are added only where accessible selectors would be brittle or unclear?
- Loading, empty, error and success states can be reached deterministically in tests?
- Tables and lists expose stable row identity without relying on visual position only?
- Copy and state names are clear enough for QA/SDET learners to map requirements to checks?
