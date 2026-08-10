# Understanding Playwright's `test.slow()` and `slowMo` Option

Introduction: In automated testing with Playwright, efficiency and accuracy are key. Playwright offers various features to manage and debug tests. Two such features are `test.slow()` and the `slowMo` option within `launchOptions`. They serve distinct purposes.

## Understanding `test.slow()`

`test.slow()` is a method used within Playwright Test to adjust expectations around test duration. Useful for tests that are inherently slow due to complex interactions or heavy resource usage.

Key Points:

- `test.slow()` adjusts the timeout settings for a test.
- Marks a test as slower than usual, helping avoid false negatives due to timeouts.
- Does not slow down execution — manages time expectations only.

Example usage at the start of a slow integration test:

```typescript
test('heavy checkout flow', async ({ page }) => {
  test.slow();
  // long-running steps...
});
```

When a test is marked slow, Playwright triples the test timeout and triples the expect timeout for that test.

## Exploring `launchOptions: { slowMo: 1000 }`

The `slowMo` option in `launchOptions` adds a delay (milliseconds) after each browser action when launching the browser.

Key Points:

- `slowMo` literally slows down browser interactions.
- Invaluable for debugging — observe actions step-by-step.
- Delay applies to clicks, typing, navigation, etc.

Configure in playwright.config.ts:

```typescript
use: {
  launchOptions: {
    slowMo: 1000,
  },
},
```

Or per-test via `browser.newContext({ slowMo: 500 })`.

## Comparative Analysis

`test.slow()` is about test management; `slowMo` focuses on interaction pace inside the browser.

- `test.slow()` does not change how fast actions run — only how duration is interpreted for timeouts.
- `slowMo` changes the speed of browser actions for observation and debugging.

## Practical Applications

- Use `test.slow()` for naturally slower tests to prevent timeout failures in CI.
- Apply `slowMo` when you need to watch the browser in real time while debugging flaky UI steps.

## Conclusion

Both `test.slow()` and `slowMo` are essential Playwright tools. Understanding when to use each improves testing strategy, balancing speed in CI with visibility during local debugging.

Additional context: `slowMo` is set on browser launch/context creation and affects every action in that context. `test.slow()` is declarative per test and integrates with Playwright's timeout multiplier logic. They can be combined — a slow-marked test running with slowMo during local investigation — but avoid slowMo in CI pipelines because it linearly increases suite runtime.

For teams standardizing on trace viewer and step-level reporting, prefer normal speed execution with `trace: 'on-first-retry'` rather than permanent slowMo in shared configs.

Example test.slow() at describe level marks entire suite as slow when every test hits payment gateway sandbox with 30s backend latency. slowMo in launchOptions: `{ slowMo: 1000 }` adds one second pause after each input action — useful when demoing flaky hover menu to stakeholders. test.slow() triples default timeout and expect timeout per Playwright docs — does not add wall-clock delay between steps. Debugging headed with slowMo 500 locally then removing slowMo in CI config keeps developer experience without multiplying pipeline duration. Semih kasımoğlu January 2024 Medium article tags: Test Automation, Playwright Automation, Playwright Test. Subscribe for more Playwright efficiency guides from author profile.

