# 01 — Strict compiler gate and runtime POM contracts

**What to build:** A dedicated strict TypeScript gate covers the complete live POM framework and all active Playwright configurations. The gate is green and catches interface drift before Playwright execution. The two known missing POM methods are implemented as intent-level APIs using existing UI contracts.

**Blocked by:** None — can start immediately.

**Seams:** TypeScript static; Playwright E2E discovery; targeted Playwright E2E

**Status:** ready-for-agent
**Category:** bug

## Implementation guidance

- Add a dedicated POM tsconfig and a named package script; include live specs, auth, fixtures, pages/components, API, data, methods, utils, and active configs.
- Keep learner copies and generated auth state outside this gate.
- Use strict, no-emit checking with indexed-access and override safety. Fix root causes; do not add broad casts or suppression comments.
- Add the registry caption locator and expiration-sweep intent action to their existing page objects.
- Preserve HTTP/business assertions in the specs.

## Immutable acceptance IDs

- `T01-A01` — The POM typecheck script fails on a deliberately introduced missing member and succeeds after it is reverted.
- `T01-A02` — The full live POM and config graph compiles with zero TypeScript errors.
- `T01-A03` — No `@ts-ignore`, unjustified `@ts-expect-error`, `as any`, or new non-null assertion was added.
- `T01-A04` — The merchant caption test uses an existing page-object locator, not raw page access or a cast.
- `T01-A05` — The expiration-sweep UI test uses an existing page-object action and still asserts the POST and visible outcome in the spec.
- `T01-A06` — Main config discovery is green after the type fixes.
- `T01-A07` — Targeted live merchant-table and expiration-sweep tests are green, or explicitly recorded `NOT_RUN` if stack/credentials are unavailable.

## Validation and verification

Apply the goal's shared loop. Ticket-specific proof: categorized compiler baseline, full green POM typecheck, touched-file lint, main discovery, then the two affected live tests when stack and env-only credentials exist.
