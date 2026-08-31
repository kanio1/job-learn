# GOAL — Playwright POM clean-code repair

## Purpose

Audit and repair the canonical live Playwright Page Object Model under
`apps/frontend/tests-pom` with emphasis on:

- Page Object Model boundaries;
- Playwright locator and synchronization practices;
- TypeScript type safety;
- useful `test.step()` reporting;
- comments and naming;
- SOLID, DRY and KISS;
- removal of unnecessary abstractions and duplication.

## Current audit baseline

The audit performed on 2026-08-27 found:

- 72 spec files and 310 tests discovered by the main POM configuration;
- 37 `test.step()` calls in 9 spec files;
- 243 direct UI-locator constructions through `page` or `app.page` in specs;
- 105 textual non-null assertions (`!`);
- 88 textual type assertions (`as ...`);
- 68 warnings from POM-scoped Oxlint;
- `corepack pnpm typecheck:pom` green;
- main Playwright discovery green;
- full frontend lint red because of an unrelated production error in
  `app/composables/useEventLabApi.ts`; that production file is outside this goal.

These numbers are a historical baseline, not acceptance evidence. Recalculate
them against the current working tree before implementation.

## Positive architecture to preserve

Do not replace the existing framework. Preserve:

- thin `BasePage`;
- `App` facade;
- fixture DI through `{ app, api }`;
- component objects through composition;
- data factories and builders;
- `BffClient` as the public BFF adapter/facade;
- live-stack behavior without network mocks;
- business assertions in specs.

Forbidden replacements include Screenplay, Actor/Task/Question abstractions, a
fat `BasePage`, a generic UI DSL, single-implementation interfaces, factories
with one product, and wrappers that only delegate.

## Scope

Allowed writes:

- `apps/frontend/tests-pom/**`;
- `apps/frontend/tests-pom/README.md`;
- Playwright POM configuration only when a current finding directly requires it;
- the mutable state/evidence files explicitly selected for this goal.

Forbidden without new user authorization:

- `apps/frontend/app/**`;
- `apps/frontend/server/**`;
- backend production code or tests;
- Vitest tests;
- `apps/frontend/tests-pom-learner/**`;
- `.kiro/**`;
- dependencies or version changes;
- `.env`, credentials or generated auth state;
- commits and pushes.

Preserve all existing test names, test IDs, tags, projects, HTTP contracts and
business behavior unless a test name is demonstrably incorrect and the change
is explicitly approved.

## Required skills and workflow

Read and follow, in order:

1. root `AGENTS.md`;
2. `.agents/skills/tdd/SKILL.md`;
3. `.agents/skills/playwright-pom/SKILL.md` and `patterns.md`;
4. `.agents/skills/playwright-sdet-review/SKILL.md`;
5. `.agents/skills/ponytail/SKILL.md` for the smallest viable fix;
6. `.agents/skills/code-review/SKILL.md` for the final scoped review.

Use a red-green-refactor loop. For a pure refactor, first lock current behavior
with the cheapest static or discovery check, then make the intended rule fail,
apply the smallest correction and return to green.

## Baseline commands

Run from the repository root unless noted:

```bash
git branch --show-current
git status --short
```

Run from `apps/frontend`:

```bash
corepack pnpm typecheck:pom
corepack pnpm exec oxlint --config oxlint.config.ts tests-pom
corepack pnpm exec playwright test --config playwright.pom.config.ts --list
```

Record exact exit codes, warning counts and discovery counts. Inspect the dirty
worktree before every edit and do not revert unrelated changes.

## Repair program

### 1. Restore the POM boundary

- Specs describe what the actor observes and own business assertions.
- Page and component objects describe how to operate the UI.
- Keep only `expectLoaded()`, access/load oracles and modal/component
  `expectOpen()`/`expectClosed()` inside POM classes.
- Replace business assertion methods such as `expectStatus`,
  `expectRiskFlagged`, `expectBadge`, `expectSlider`, `expectCardIn`,
  `expectDeliveryProcessed` and `expectNoteVisible` with domain-named locator
  methods. Put the final `expect(...)` in the spec.
- Move direct UI locators from specs to the correct page/component object.
- Allow direct `page` use in specs only for browser or network primitives:
  URL, popup, download, context/cookies, clock, clipboard, a keyboard/mouse
  behavior that is itself under test, `waitForRequest`, `waitForResponse` and
  WebSocket observation.
- Do not hide a business assertion inside a new helper.

### 2. Repair locator quality

Use this priority:

1. `getByRole`;
2. `getByLabel`;
3. `getByPlaceholder`;
4. `getByTestId`.

Additional rules:

- scope locators to a root, dialog, table or row;
- dynamic elements are methods returning `Locator`;
- remove CSS/XPath and `.first()`/`.nth()` used only to silence strict mode;
- replace avoidable `.toast-error`, `locator('..')`, `tbody tr`, `th`,
  `contenteditable` and component-internal input selectors;
- every remaining CSS locator must be necessary for a third-party/widget
  boundary without usable semantics and have a short local rationale comment;
- locator methods return `Locator`, never `Promise<Locator>`.

### 3. Remove confirmed duplication

- Make `PaymentFiltersComponent` the single owner of payment filters. Remove
  duplicate filter input, apply and clear behavior from `PaymentsListPage`.
- Extend `utils/wait-bff.ts` with the smallest useful exact-query matcher and
  replace the duplicate command-palette search waiters.
- Extract one shared Error Lab contract helper for `triggerMethod`, live trigger
  and problem-status validation.
- Remove the unused deprecated `ConfirmModal.cancel()` alias; keep `dismiss()`.
- Extract other duplication only when at least three real call sites share the
  same reason to change. Do not generalize merely because two lines look alike.

### 4. Repair TypeScript and BFF boundary safety

- Give locator methods explicit `Locator` return types and async actions
  explicit `Promise<void>` return types.
- Use consistent `import type` declarations instead of inline
  `import('@playwright/test').Page`/`Locator` types.
- Remove unused imports and malformed import spacing.
- Do not use `response.json() as T` or `JSON.parse(...) as T` for external data.
- Replace the generic `parseJson<T>` assertion with a parser accepting a
  test-owned Zod schema. Zod is already available; do not add a dependency.
- Test schemas must be minimal, named and may use `.passthrough()` when
  unasserted response fields are outside the oracle.
- Do not import production application schemas into black-box POM tests; avoid
  self-fulfilling contract tests.
- Separate success DTOs from `ProblemDetails`. Expected JSON endpoints must fail
  clearly, with method and endpoint context, on empty or malformed bodies.
- Remove non-null assertions where the value follows from an HTTP response.
  Prefer correctly required DTO fields, `requireStatus`, `requireEtag` or
  `node:assert/strict`.
- Keep `BffClient` as the public fixture-facing facade. It may compose cohesive
  domain clients and a shared transport if that materially reduces the current
  divergent 900-line implementation. Do not add empty interfaces or pure
  middle-man layers merely to claim SOLID.

### 5. Eliminate silent conditional actions

- Replace `PinChallengeComponent.submitIfEnabled()` with a normal `submit()`.
- In the spec, explicitly assert that the button is enabled before clicking.
- Review other POM actions for branches that can silently do nothing.
- Desired-state actions such as `setAutoCapture(boolean)` may remain when
  idempotence is the declared intent.

### 6. Apply a useful `test.step()` policy

Use `test.step()` only where it improves the report:

- add 2–4 outcome-oriented steps to multi-stage E2E journeys containing at
  least two meaningful phases, such as API arrange → UI action → network or
  persistence outcome;
- for EP/BVA/decision-table loops, use `test.step(row.id, ...)` where it makes
  failed examples identifiable;
- do not add steps to simple one-oracle HTTP tests;
- do not wrap every action or assertion;
- avoid nested steps;
- keep steps in specs, not as automatic POM decorators;
- use actor/outcome wording, not implementation wording.

Prioritize long journeys in:

- `payments-pin.spec.ts`;
- `payments-kanban.spec.ts`;
- `payments-refund-dual-control.spec.ts`;
- `merchants-slideover.spec.ts`;
- `merchants-table.spec.ts`;
- `checkout-lab.spec.ts`;
- `session.spec.ts`.

Document this short policy in `tests-pom/README.md`.

### 7. Clean comments

- Keep comments explaining security boundaries, network oracles, unusual HTTP
  contracts, cleanup/isolation and unavoidable widget limitations.
- Remove comments that repeat the method name or obvious code.
- Remove decorative separator comments.
- A comment must not excuse a removable bad locator or unsafe cast.
- Retain and correct `SAFETY:` comments only when the stated invariant is
  actually enforced at runtime.

## Acceptance criteria

1. POM-only TypeScript typecheck is green.
2. POM-only Oxlint is green with zero warnings.
3. Main discovery is green and the same current set of tests remains reachable;
   reconcile any count change explicitly.
4. No new `page.route`, `route.fulfill`, HAR, `waitForTimeout`, `test.only`,
   `as any`, TypeScript suppression or credential fallback exists.
5. Specs contain no avoidable direct UI locator construction through
   `page`/`app.page`; every remaining direct use is a browser/network oracle.
6. POM classes contain no hidden journey/business assertions outside the
   documented load/access/open exceptions.
7. Confirmed filter, search-waiter and Error Lab duplication is removed.
8. No unsafe generic JSON cast remains at the BFF boundary.
9. Non-null assertions and type assertions are reduced to justified exceptions;
   report each remaining external-boundary exception.
10. Multi-stage journeys have useful, non-mechanical steps; simple tests remain
    simple.
11. Targeted changed journeys are green on the live stack.
12. Full main POM and required affected overlays are green when the documented
    environment and env-only passwords are available.
13. `git diff --check` is green.
14. Final Playwright/SDET and KISS reviews have no open P0/P1 findings.

`--list`, skipped tests and unavailable environments are never runtime PASS. If
the live environment is unavailable, record `NOT_RUN`, complete all independent
static/refactor work and keep the goal incomplete.

## Validation ladder

Run from `apps/frontend`, cheapest first:

1. smallest expected-red static check or focused test;
2. `corepack pnpm typecheck:pom`;
3. Oxlint on touched POM files;
4. full POM-only Oxlint;
5. main Playwright `--list`;
6. exact modified spec/project on the live stack;
7. all affected journeys;
8. full main live POM;
9. affected Kafka/TLS/RLS/Mirror overlay runs where relevant;
10. `git diff --check` and final reviews.

The full frontend lint currently has an unrelated production failure in
`app/composables/useEventLabApi.ts`. Re-run it for classification, but do not
modify that file within this goal.

## Final report

Report:

- changed files;
- decisions and trade-offs for SOLID, DRY and KISS;
- direct UI locator count before/after;
- non-null assertion and type-assertion counts before/after;
- POM Oxlint warning count before/after;
- `test.step()` policy and affected journeys, without presenting a raw step
  quota as quality;
- exact commands, exit codes and test counts;
- all remaining justified exceptions;
- every `NOT_RUN`, environmental requirement and out-of-scope failure;
- final review findings and whether any P0/P1 remains.

Do not claim completion while required evidence is absent.
