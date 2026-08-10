# Our Playwright testing standards at Houseful

By Boyana Staneva — 20 November 2023

Multiple teams across Houseful use Playwright for frontend test automation. We created test standards for code that is easy to read, follow and debug.

Benefits of aligned standards:

- Improves reusability across functions, locators, shared steps
- Eases code reviews with predictable structure
- Quicker onboarding via naming conventions

## Playwright Guidelines

### Data Creation in e2e tests

E2E tests are expensive. Consider alternatives first.

DO:
- Each test creates its own data
- Each test has teardown clearing data

DO NOT:
- Rely on existing data
- Leave uncleared state

### Page Object Model (POM)

Each page has a POM file with selectors and functions. Interactions via page objects only — no selectors in tests. Assertions in tests only — no assertions in POM.

### Test Structure — Arrange, Act, Assert

Follow AAA pattern; Arrange often in Before block with section comments.

### Linter

Use eslint-plugin-playwright: `npm install -D eslint-plugin-playwright`

### Avoid Conditionals

Tests should be deterministic. Split conditional scenarios into separate specs with explicit setup.

### Waiting

Don't use arbitrary waits. Prefer:
- waitUntil: 'domcontentloaded'
- waitForResponse helpers
- expect locators to reach visible/hidden state

```js
export const waitForAPIResponse = async (page, url, statusCode) => {
  await page.waitForResponse((res) => res.url().includes(url) && res.status() === statusCode);
};
```

### Selectors

Prioritize: getByRole, getByText, getByTestId. Avoid brittle CSS chains.

### Tagging

Tag by test type (@functional @smoke @visual), pipeline stage, or feature (@foobar).

### Flaky tests

Resolve as priority; use test.fixme() to skip until fixed.

### Parallelization and Repeatability

Tests must run in parallel without interference. Use worker processes and larger GitHub runners in CI.

## Naming Conventions

- Variables: camelCase
- Booleans: is/has/are/have prefix
- Page objects: PascalCase descriptive names (AddWorksOrderModal)
- Locators: action/name + element type (savePropertyButton, reportedDateField)
- Functions: verb + component context (getWorksOrder, deleteProperty)

Topics: data, delivery, design, devops, engineering, people, product development, quality, testing, ways-of-working.

Extended POM example: FooPage class holds pageTitle and buttonFoo locators; tests import FooPage, perform actions via page object, assert in spec file only.

Conditional anti-pattern: if (isButtonVisible) branches create non-deterministic tests — split into fooPageVisible.spec and fooBarButton.spec with explicit setup per scenario.

Waiting anti-pattern: page.waitForTimeout(5000) — replace with waitForResponse after click or expect(titlePage).toBeVisible() auto-wait.

Tagging examples: @accessibility with axe inject; @functional @smoke for core assertions; @visual desktop/mobile with captureScreenshot fixture.

Flaky test policy: test.fixme skips in CI until root-caused — prevents silent green builds hiding instability.

Parallel workers: Playwright scales workers to available CPUs; Houseful uses larger GitHub runners to shorten regression on monorepo frontends.

Selector priority follows Testing Library guidance — getByRole reflects assistive-tech user experience and catches accessibility regressions early.

Data isolation: tests creating own tenants/properties/charges and tearing down prevent order-dependent failures when sharding suites across workers.

Linting with eslint-plugin-playwright enforces no-wait-for-timeout, valid expect patterns, and consistent test.describe structure in PR reviews.

Cross-team reuse: shared wait-helpers and POM base classes live in package libraries consumed by multiple Houseful product repos.

Onboarding checklist: read standards doc, clone template repo, run tagged @smoke subset locally before opening first PR.

POM anti-pattern example shows inline locators inside spec files — forbidden; all selectors belong in page object classes with descriptive PascalCase names like AddWorksOrderModal not newModal.

AAA example with createProperty raiseCharge expect(charge).toBe('raised') demonstrates comment-delimited sections readable in PR diffs.

eslint-plugin-playwright recommended config catches discouraged waitForTimeout and encourages playwright-native expect patterns in CI lint stage before merge.

Conditional test anti-pattern with if (isButtonVisible) branches — replace with separate spec files fooPageVisible.spec and fooBarButton.spec each with deterministic setup.

waitForAPIResponse helper centralizes response URL substring and status code assertion reused across payment and accounting domain tests.

getByRole priority aligns with accessibility-first selector strategy — catches missing aria labels earlier than CSS-only tests.

Tag annotations @foobar @smoke on describe blocks enable targeted pipeline stages: release smoke vs nightly regression vs visual-only jobs.

test.fixme documents known flakes without deleting coverage — tracked until root-caused with ticket link in PR description.

Houseful/Zoopla blog cross-links: test framework migration design patterns, playwright tag annotations 2022 post for annotation conventions.

Boyana Staneva author — Houseful Product & Technology Blog 20 November 2023 standards adopted across multiple frontend squads.

End of Houseful Playwright standards archive.
