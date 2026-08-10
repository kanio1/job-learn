# Advanced Playwright Fixtures: Supercharge Your Test Setup and Teardown

Discover how to use advanced Playwright fixtures (test.extend()) to create modular, reusable setup/teardown units.

As your Playwright test suite expands, managing setup and teardown logic efficiently becomes crucial. Playwright fixtures, defined using test.extend(), offer a powerful solution for modular, reusable setup and teardown units.

## 1. Understanding Playwright Fixtures

A Playwright fixture runs before (setup) and after (teardown) your test. Key concepts: test.extend(), fixture function with use(), fixture value passed to tests.

Benefits: Reusability, Readability, On-Demand Execution, Composability, Encapsulation.

## 2. Worker Scope vs. Test Scope Fixtures

- scope: 'test' (default): setup/teardown per test
- scope: 'worker': setup once per worker for expensive shared resources like API clients

## 3. Auto-Use Fixtures

Add { auto: true } for fixtures that run for every test without explicit declaration (logging, mocks, global setup).

## 4. Dependent Fixtures (Chaining Fixtures)

Fixtures can depend on other fixtures; Playwright resolves the dependency tree automatically (e.g. adminLoggedInPage depending on page and adminUser).

## 5. Parameterizing Fixtures

Use multiple named fixtures, environment variables, or project-level fixture overrides in playwright.config.ts.

## 6. Advanced Fixture Use Cases

- Managing test data (JSON/CSV loaders, API-created unique data with teardown)
- Service abstractions and mocks (page.route auto fixtures)
- Browser context manipulation (geolocation, permissions)
- Complex setup/teardown with guaranteed cleanup

## 7. Organizing and Reporting Fixtures

- mergeTests() to combine fixture files
- box: true to hide internal fixtures from HTML report
- title: '...' for custom fixture titles in reports

## 8. Tips for Effective Fixture Design

Single responsibility, clear naming, scope wisely, document fixtures, integrate with POM, manage fixture timeouts, re-export expect.

## Conclusion

Playwright fixtures are a cornerstone of scalable test automation. Moving beyond beforeEach hooks to test.extend() creates composable setup/teardown logic that improves efficiency and code quality.
