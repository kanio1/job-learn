# Strategic Tagging: Optimizing Your Playwright Test Suit

Do you find yourself running your complete automated test suite on every occasion? Utilizing tags can accelerate the process by selectively choosing which tests to execute precisely when you require them.

As software projects evolve and automation project expands, it's common for the number of tests to increase alongside the introduction of new features. It becomes very useful to run only a subset of certain tests. Although Playwright allows tests to run in parallel, there comes a point where splitting tests into smaller groups proves useful.

Effectively organizing tests with tags offers a significant advantage of precisely targeting the required test cases.

**Consider the following examples:**

◾ Run the entire test suite outside of business hours without disrupting the team and selectively run a subset of tests on a pull request to maintain the speed and efficiency of your **CI pipelines**.

◾ Allow specific teams (eg QA or the features team) to run only the tests they are responsible for.

◾ Run smoke tests during a production release that only involve read operations.

Tags are used to filter tests in the HTML Report, UI Mode or VSCode extension.

Using a tag system allows you to categorize tests into logical sets. Tags are defined using the **@tag** syntax within the test description.

**How to Install Playwright?**

```
npm init playwright@latest
```

**How to run Playwright test?**

```
npx playwright test
```

**Old Playwright Syntax:**

In the past, tags were incorporated into the test title, which remains a supported method. However, this approach leads to duplication in the HTML report.

```
test('Playwright Landing page - Has title @Smoke', async ({ page }) => {
  await page.goto('https://playwright.dev/');
  await expect(page).toHaveTitle(/Playwright/);
});
```

**How to run tests by tags?**

```
npx playwright test --grep @Smoke
```

**New Playwright Syntax:**

To adopt the new syntax, simply generate a tag object containing either an array of tags or a single tag:

```
test('Playwright Landing page - Has title', { tag: ['@Smoke', '@UI' ] } ,async ({ page }) => {
  await page.goto('https://playwright.dev/');
  await expect(page).toHaveTitle(/Playwright/);
});
```

Tags are also applicable within a describe block:

```
test.describe('Group Example', { tag: '@Group' }, () => {
  test('Playwright Landing page - Has title', { tag: ['@Smoke', '@UI' ] } ,async ({ page }) => {
    await page.goto('https://playwright.dev/');
    await expect(page).toHaveTitle(/Playwright/);
  });
  test('Playwright Github', { tag: ['@Smoke', '@UI', '@Fast' ] } ,async ({ page }) => {
    await page.goto('https://github.com/microsoft/playwright');
  });
});
```

Update Playwright to latest version:

```
npm install -D @playwright/test@latest
npx playwright install --with-deps
```

**Advantages of using @tag in test management:**

**Simplified test management:** **@tag** simplify test management by categorizing test cases.

**Tag Statistics Heat map:** The Tag Statistics Heat map provides valuable insights about tags and test coverage.

**Custom Test Scenarios:** You can execute all tests marked as **@Smoke** excluding those marked as **@Regression**

**Examples you can use to classify your tests:**

**Smoke testing** is a software testing technique performed after the software is built to verify that the critical functions are working well.

**Sanity testing** is performed after receiving an intermediate version of software with minor changes to code or functionality.

**Regression testing** is conducted after a code update to ensure that the update introduced no new bugs.

Therefore, it is very important that you organize your test tagging strategy well. You are in a much better position when you want to run only a specific set of tests and not the entire suite.

## Until Next Time
