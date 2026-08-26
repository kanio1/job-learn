# 03 — Single-shot actions, POM boundaries, and visible business oracles

**What to build:** Mutating user actions execute once per test attempt, reusable UI interactions live behind intent-level page/component methods, and business outcomes remain explicit in specs.

**Blocked by:** 02 — Typed BFF results and fixture lifecycle contracts.

**Seams:** Playwright E2E; Playwright REST/BFF; TypeScript static

**Status:** ready-for-agent
**Category:** bug

## Implementation guidance

- Rewrite PSP approval as register observation → one click → poll/assert outcome.
- Prove the approval mutation is emitted exactly once.
- Change component methods that assert business HTTP status internally so the response/outcome is observable by the spec.
- Inventory repeated direct `page`/`app.page` actions in specs. Move repeated UI interactions into named POM intents; keep unique browser primitives and network oracles in specs.
- Keep readiness and component-state assertions in POM; keep status/header/domain/user-outcome assertions in specs.
- Refactor per journey, keeping each focused test green.

## Immutable acceptance IDs

- `T03-A01` — No mutating click/request is inside `expect().toPass()` or another polling callback.
- `T03-A02` — PSP approval performs exactly one mutating request and reaches the same visible success state.
- `T03-A03` — Saved-view defaulting exposes its response/outcome and the spec asserts the expected status.
- `T03-A04` — Repeated direct UI interactions identified in merchant, support-bulk, and payment PIN journeys use intent-level POM methods.
- `T03-A05` — Direct page usage that remains is documented by a clear browser/network reason.
- `T03-A06` — Business assertions are visible at the spec level.
- `T03-A07` — Strict POM typecheck, POM-only lint, and focused live journeys are green or honestly `NOT_RUN`.

## Validation and verification

Apply the goal's shared loop per moved action. Ticket-specific proof: external-behavior oracle, single test then spec, request counts before/after and POM over-abstraction review.
