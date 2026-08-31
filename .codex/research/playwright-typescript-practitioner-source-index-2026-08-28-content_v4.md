---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Playwright | Best practices | https://playwright.dev/docs/best-practices | complete |
| 2 | Playwright | Fixtures | https://playwright.dev/docs/test-fixtures | complete |
| 3 | Playwright | Configuration | https://playwright.dev/docs/test-configuration | complete |
| 4 | Playwright | Assertions | https://playwright.dev/docs/test-assertions | complete |
| 5 | Playwright | Parallelism | https://playwright.dev/docs/test-parallel | complete |
| 6 | Playwright | Authentication | https://playwright.dev/docs/auth | complete |
| 7 | Yevhen Laichenkov | 17 Playwright Testing Mistakes You Should Avoid | https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid | complete |
| 8 | Yevhen Laichenkov | TIL: Playwright step decorator for better test reporting | https://elaichenkov.github.io/posts/til-playwright-step-decorator | complete |
| 9 | Vitaliy Haradkou | Modern TypeScript Decorators: TC39 Stage 3 | https://blog-vitaliharadkous-projects.vercel.app/blog/20-typescript-decorators | complete |
| 10 | Vitaliy Haradkou | Testcontainers boilerplate packaged | https://blog-vitaliharadkous-projects.vercel.app/blog/21-testcontainers | complete |
| 11 | Vitaliy Haradkou | Angular-aware selector engine | https://blog-vitaliharadkous-projects.vercel.app/blog/22-angular-selectors | complete |
| 12 | Vitaliy Haradkou | Type-safe SQL in Playwright tests | https://blog-vitaliharadkous-projects.vercel.app/blog/23-pw-sql | complete |
| 13 | Vitaliy Haradkou | Reporter Slack | https://blog-vitaliharadkous-projects.vercel.app/blog/24-pw-slack | complete |
| 14 | Currents.dev | Debug Playwright tests in CI | https://currents.dev/posts/how-to-debug-playwright-tests-in-ci | complete |
| 15 | Currents.dev | Testing authentication | https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide | complete |
| 16 | Currents.dev | Speed up Playwright tests | https://currents.dev/posts/how-to-speed-up-playwright-tests | complete |
| 17 | Currents.dev | Strategies for Playwright test agents | https://currents.dev/posts/9-strategies-to-get-the-most-out-of-playwright-test-agents | complete |
| 18 | Currents.dev | Tests that survive UI refactors | https://currents.dev/posts/designing-playwright-tests-that-survive-ui-refactors | complete |
| 19 | Currents.dev | Playwright API testing at scale | https://currents.dev/posts/playwright-api-testing | complete |
| 20 | Sajith Dilshan | Async/await in Playwright TypeScript | https://medium.com/@sajith-dilshan/mastering-async-await-in-playwright-typescript-1343e5b21722 | complete |
| 21 | Sajith Dilshan | Locator strategy | https://medium.com/@sajith-dilshan/playwright-locator-strategy-choosing-the-right-locator-for-stable-and-maintainable-test-automation-018a4fd0e16c | complete |
| 22 | Sajith Dilshan | Auto-waiting | https://medium.com/@sajith-dilshan/playwright-auto-waiting-the-secret-behind-stable-and-reliable-test-automation-bd3987a3156e | complete |
| 23 | Sajith Dilshan | Scalable authentication | https://medium.com/@sajith-dilshan/scalable-authentication-in-playwright-why-globalsetup-falls-short-644cf0fb4db4 | complete |
| 24 | Sajith Dilshan | Why tsconfig.json matters | https://medium.com/@sajith-dilshan/why-tsconfig-json-matters-in-a-playwright-typescript-project-and-why-its-often-missing-1f8c99b598fc | complete |
| 25 | Sajith Dilshan | Module systems | https://medium.com/@sajith-dilshan/understanding-typescript-module-systems-for-playwright-commonjs-vs-es-modules-e2a8caffa328 | complete |
| 26 | Sajith Dilshan | Hooks | https://medium.com/@sajith-dilshan/playwright-hooks-the-secret-behind-clean-scalable-test-automation-8448bc56c1b4 | complete |
| 27 | Sajith Dilshan | Error handling and strict mode | https://medium.com/@sajith-dilshan/playwright-error-handling-explained-common-errors-strict-mode-and-best-practices-63fac0949ea0 | complete |
| 28 | TestDino | POM pattern | https://testdino.com/blog/playwright-page-object-model | complete |
| 29 | TestDino | Playwright test automation | https://testdino.com/blog/playwright-test-automation | complete |
| 30 | TestDino | Playwright 1.61 release | https://testdino.com/blog/playwright-1-61-release | complete |
| 31 | TestDino | Playwright architecture | https://testdino.com/blog/playwright-architecture | complete |
| 32 | Level Up Coding | Custom fixtures in TypeScript | https://levelup.gitconnected.com/how-to-create-custom-fixtures-in-playwright-typescript-a-complete-practical-guide-4fa8b2fc2c82 | complete |
| 33 | Level Up Coding | Fixtures in 2025 | https://levelup.gitconnected.com/playwright-fixtures-in-2025-the-practical-guide-to-fast-clean-end-to-end-tests-55e9b3f7b5f7 | unavailable |

## Artykuły
### 1. Playwright — Best practices

- Source: https://playwright.dev/docs/best-practices
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

This guide should help you to make sure you are following our best practices and writing tests that are more resilient.

##### Testing philosophy

###### Test user-visible behavior

Automated tests should verify that the application code works for the end users, and avoid relying on implementation details such as things which users will not typically use, see, or even know about such as the name of a function, whether something is an array, or the CSS class of some element. The end user will see or interact with what is rendered on the page, so your test should typically only see/interact with the same rendered output.

###### Make tests as isolated as possible

Each test should be completely isolated from another test and should run independently with its own local storage, session storage, data, cookies etc. [Test isolation](https://playwright.dev/docs/browser-contexts) improves reproducibility, makes debugging easier and prevents cascading test failures.

In order to avoid repetition for a particular part of your test you can use [before and after hooks](https://playwright.dev/docs/api/class-test). Within your test file add a before hook to run a part of your test before each test such as going to a particular URL or logging in to a part of your app. This keeps your tests isolated as no test relies on another. However it is also ok to have a little duplication when tests are simple enough especially if it keeps your tests clearer and easier to read and maintain.

```js
import { test } from '@playwright/test';

test.beforeEach(async ({ page }) => {

  // Runs before each test and signs in each page.

  await page.goto('https://github.com/login');

  await page.getByLabel('Username or email address').fill('username');

  await page.getByLabel('Password').fill('password');

  await page.getByRole('button', { name: 'Sign in' }).click();

});

test('first', async ({ page }) => {

  // page is signed in.

});

test('second', async ({ page }) => {

  // page is signed in.

});
```

You can also reuse the signed-in state in the tests with [setup project](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests). That way you can log in only once and then skip the log in step for all of the tests.

###### Avoid testing third-party dependencies

Only test what you control. Don't try to test links to external sites or third party servers that you do not control. Not only is it time consuming and can slow down your tests but also you cannot control the content of the page you are linking to, or if there are cookie banners or overlay pages or anything else that might cause your test to fail.

Instead, use the [Playwright Network API](https://playwright.dev/docs/network#handle-requests) and guarantee the response needed.

```js
await page.route('**/api/fetch_data_third_party_dependency', route => route.fulfill({

  status: 200,

  body: testData,

}));

await page.goto('https://example.com');
```

###### Testing with a database

If working with a database then make sure you control the data. Test against a staging environment and make sure it doesn't change. For visual regression tests make sure the operating system and browser versions are the same.

##### Best Practices

###### Use locators

In order to write end to end tests we need to first find elements on the webpage. We can do this by using Playwright's built in [locators](https://playwright.dev/docs/locators). Locators come with auto waiting and retry-ability. Auto waiting means that Playwright performs a range of actionability checks on the elements, such as ensuring the element is visible and enabled before it performs the click. To make tests resilient, we recommend prioritizing user-facing attributes and explicit contracts.

```js
// 👍

page.getByRole('button', { name: 'submit' });
```

###### Use chaining and filtering

Locators can be [chained](https://playwright.dev/docs/locators#matching-inside-a-locator) to narrow down the search to a particular part of the page.

```js
const product = page.getByRole('listitem').filter({ hasText: 'Product 2' });
```

You can also [filter locators](https://playwright.dev/docs/locators#filtering-locators) by text or by another locator.

```js
await page

    .getByRole('listitem')

    .filter({ hasText: 'Product 2' })

    .getByRole('button', { name: 'Add to cart' })

    .click();
```

###### Prefer user-facing attributes to XPath or CSS selectors

Your DOM can easily change so having your tests depend on your DOM structure can lead to failing tests. For example consider selecting this button by its CSS classes. Should the designer change something then the class might change, thus breaking your test.

```js
// 👎

page.locator('button.buttonIcon.episode-actions-later');
```

Use locators that are resilient to changes in the DOM.

```js
// 👍

page.getByRole('button', { name: 'submit' });
```

###### Generate locators

Playwright has a [test generator](https://playwright.dev/docs/codegen) that can generate tests and pick locators for you. It will look at your page and figure out the best locator, prioritizing role, text and test id locators. If the generator finds multiple elements matching the locator, it will improve the locator to make it resilient and uniquely identify the target element, so you don't have to worry about failing tests due to locators.

###### Use `codegen` to generate locators

To pick a locator run the `codegen` command followed by the URL that you would like to pick a locator from.

- npm
- yarn
- pnpm

```bash
npx playwright codegen playwright.dev
```

```bash
yarn playwright codegen playwright.dev
```

```bash
pnpm exec playwright codegen playwright.dev
```

This will open a new browser window as well as the Playwright inspector. To pick a locator first click on the 'Record' button to stop the recording. By default when you run the `codegen` command it will start a new recording. Once you stop the recording the 'Pick Locator' button will be available to click.

You can then hover over any element on your page in the browser window and see the locator highlighted below your cursor. Clicking on an element will add the locator into the Playwright inspector. You can either copy the locator and paste into your test file or continue to explore the locator by editing it in the Playwright Inspector, for example by modifying the text, and seeing the results in the browser window.

![generating locators with codegen](https://user-images.githubusercontent.com/13063165/212103268-e7d8ee8b-d307-4cba-be13-831f3fbb1f40.png)

###### Use the VS Code extension to generate locators

You can also use the [VS Code Extension](https://playwright.dev/docs/getting-started-vscode) to generate locators as well as record a test. The VS Code extension also gives you a great developer experience when writing, running, and debugging tests.

![generating locators in vs code with codegen](https://user-images.githubusercontent.com/13063165/212269873-aca04043-16ce-4627-906f-7351d09740ab.png)

###### Use web first assertions

Assertions are a way to verify that the expected result and the actual result matched or not. By using [web first assertions](https://playwright.dev/docs/test-assertions) Playwright will wait until the expected condition is met. For example, when testing an alert message, a test would click a button that makes a message appear and check that the alert message is there. If the alert message takes half a second to appear, assertions such as `toBeVisible()` will wait and retry if needed.

```js
// 👍

await expect(page.getByText('welcome')).toBeVisible();

// 👎

expect(await page.getByText('welcome').isVisible()).toBe(true);
```

###### Don't use manual assertions

Don't use manual assertions that are not awaiting the expect. In the code below the await is inside the expect rather than before it. When using assertions such as `isVisible()` the test won't wait a single second, it will just check the locator is there and return immediately.

```js
// 👎

expect(await page.getByText('welcome').isVisible()).toBe(true);
```

Use web first assertions such as `toBeVisible()` instead.

```js
// 👍

await expect(page.getByText('welcome')).toBeVisible();
```

###### Configure debugging

###### Local debugging

For local debugging we recommend you [debug your tests live in VS Code](https://playwright.dev/docs/getting-started-vscode#debugging-your-tests) by installing the [VS Code extension](https://playwright.dev/docs/getting-started-vscode). You can run tests in debug mode by right-clicking on the line next to the test you want to run which will open a browser window and pause at where the breakpoint is set.

![debugging tests in vscode](https://user-images.githubusercontent.com/13063165/212274675-5c6e1647-2aab-40fd-9804-8680c1ac2d16.png)

You can live debug your test by clicking or editing the locators in your test in VS Code which will highlight this locator in the browser window as well as show you any other matching locators found on the page.

![live debugging locators in vscode](https://user-images.githubusercontent.com/13063165/212273189-da271dc4-0f59-4138-92a8-10e719066cbe.png)

You can also debug your tests with the Playwright inspector by running your tests with the `--debug` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test --debug
```

```bash
yarn playwright test --debug
```

```bash
pnpm exec playwright test --debug
```

You can then step through your test, view actionability logs and edit the locator live and see it highlighted in the browser window. This will show you which locators match, how many of them there are.

![debugging with the playwright inspector](https://user-images.githubusercontent.com/13063165/212276296-4f5b18e7-2bd7-4766-9aa5-783517bd4aa2.png)

To debug a specific test add the name of the test file and the line number of the test followed by the `--debug` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test example.spec.ts:9 --debug
```

```bash
yarn playwright test example.spec.ts:9 --debug
```

```bash
pnpm exec playwright test example.spec.ts:9 --debug
```

###### Debugging on CI

For CI failures, use the Playwright [trace viewer](https://playwright.dev/docs/trace-viewer) instead of videos and screenshots. The trace viewer gives you a full trace of your tests as a local Progressive Web App (PWA) that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action using dev tools, view network requests and more.

![playwrights trace viewer](https://user-images.githubusercontent.com/13063165/212277895-c63d94c2-bd06-4881-864e-62790a072ca3.png)

Traces are configured in the Playwright config file and are set to run on CI on the first retry of a failed test. We don't recommend setting this to `on` so that traces are run on every test as it's very performance heavy. However you can run a trace locally when developing with the `--trace` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test --trace on
```

```bash
yarn playwright test --trace on
```

```bash
pnpm exec playwright test --trace on
```

Once you run this command your traces will be recorded for each test and can be viewed directly from the HTML report.

- npm
- yarn
- pnpm

```bash
npx playwright show-report
```

```bash
yarn playwright show-report
```

```bash
pnpm exec playwright show-report
```

![Playwrights HTML report](https://user-images.githubusercontent.com/13063165/212279022-d929d4c0-2271-486a-a75f-166ac231d25f.png)

Traces can be opened by clicking on the icon next to the test file name or by opening each of the test reports and scrolling down to the traces section.

![Screenshot 2023-01-13 at 09 58 34](https://user-images.githubusercontent.com/13063165/212279699-c9eb134f-4f4e-4f19-805c-37596d3272a6.png)

###### Use Playwright's Tooling

Playwright comes with a range of tooling to help you write tests.

- The [VS Code extension](https://playwright.dev/docs/getting-started-vscode) gives you a great developer experience when writing, running, and debugging tests.
- The [test generator](https://playwright.dev/docs/codegen) can generate tests and pick locators for you.
- The [trace viewer](https://playwright.dev/docs/trace-viewer) gives you a full trace of your tests as a local PWA that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action, view network requests and more.
- The [UI Mode](https://playwright.dev/docs/test-ui-mode) lets you explore, run and debug tests with a time travel experience complete with watch mode. All test files are loaded into the testing sidebar where you can expand each file and describe block to individually run, view, watch and debug each test.
- [TypeScript](https://playwright.dev/docs/test-typescript) in Playwright works out of the box and gives you better IDE integrations. Your IDE will show you everything you can do and highlight when you do something wrong. No TypeScript experience is needed and it is not necessary for your code to be in TypeScript, all you need to do is create your tests with a `.ts` extension.

###### Test across all browsers

Playwright makes it easy to test your site across all [browsers](https://playwright.dev/docs/test-projects#configure-projects-for-multiple-browsers) no matter what platform you are on. Testing across all browsers ensures your app works for all users. In your config file you can set up projects adding the name and which browser or device to use.

playwright.config.ts

```js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

  projects: [\
\
    {\
\
      name: 'chromium',\
\
      use: { ...devices['Desktop Chrome'] },\
\
    },\
\
    {\
\
      name: 'firefox',\
\
      use: { ...devices['Desktop Firefox'] },\
\
    },\
\
    {\
\
      name: 'webkit',\
\
      use: { ...devices['Desktop Safari'] },\
\
    },\
\
  ],

});
```

###### Keep your Playwright dependency up to date

By keeping your Playwright version up to date you will be able to test your app on the latest browser versions and catch failures before the latest browser version is released to the public.

- npm
- yarn
- pnpm

```bash
npm install -D @playwright/test@latest
```

```bash
yarn add --dev @playwright/test@latest
```

```bash
pnpm install --save-dev @playwright/test@latest
```

Check the [release notes](https://playwright.dev/docs/release-notes) to see what the latest version is and what changes have been released.

You can see what version of Playwright you have by running the following command.

- npm
- yarn
- pnpm

```bash
npx playwright --version
```

```bash
yarn playwright --version
```

```bash
pnpm exec playwright --version
```

###### Run tests on CI

Setup CI/CD and run your tests frequently. The more often you run your tests the better. Ideally you should run your tests on each commit and pull request. Playwright comes with a [GitHub actions workflow](https://playwright.dev/docs/ci-intro) so that tests will run on CI for you with no setup required. Playwright can also be setup on the [CI environment](https://playwright.dev/docs/ci) of your choice.

Use Linux when running your tests on CI as it is cheaper. Developers can use whatever environment when running locally but use linux on CI. Consider setting up [Sharding](https://playwright.dev/docs/test-sharding) to make CI faster.

###### Optimize browser downloads on CI

Only install the browsers that you actually need, especially on CI. For example, if you're only testing with Chromium, install just Chromium.

.github/workflows/playwright.yml

```bash
#### Instead of installing all browsers

npx playwright install --with-deps

#### Install only Chromium

npx playwright install chromium --with-deps
```

This saves both download time and disk space on your CI machines.

###### Lint your tests

We recommend TypeScript and linting with ESLint for your tests to catch errors early. Use [`@typescript-eslint/no-floating-promises`](https://typescript-eslint.io/rules/no-floating-promises/) [ESLint](https://eslint.org/) rule to make sure there are no missing awaits before the asynchronous calls to the Playwright API. On your CI you can run `tsc --noEmit` to ensure that functions are called with the right signature.

###### Use parallelism and sharding

Playwright runs tests in [parallel](https://playwright.dev/docs/test-parallel) by default. Tests in a single file are run in order, in the same worker process. If you have many independent tests in a single file, you might want to run them in parallel

```js
import { test } from '@playwright/test';

test.describe.configure({ mode: 'parallel' });

test('runs in parallel 1', async ({ page }) => { /* ... */ });

test('runs in parallel 2', async ({ page }) => { /* ... */ });
```

Playwright can [shard](https://playwright.dev/docs/test-parallel#shard-tests-between-multiple-machines) a test suite, so that it can be executed on multiple machines.

- npm
- yarn
- pnpm

```bash
npx playwright test --shard=1/3
```

```bash
yarn playwright test --shard=1/3
```

```bash
pnpm exec playwright test --shard=1/3
```

##### Productivity tips

###### Use Soft assertions

If your test fails, Playwright will give you an error message showing what part of the test failed which you can see either in VS Code, the terminal, the HTML report, or the trace viewer. However, you can also use [soft assertions](https://playwright.dev/docs/test-assertions#soft-assertions). These do not immediately terminate the test execution, but rather compile and display a list of failed assertions once the test ended.

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

// ... and continue the test to check more things.

await page.getByRole('link', { name: 'next page' }).click();
```

### 2. Playwright — Fixtures

- Source: https://playwright.dev/docs/test-fixtures
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

Playwright Test is based on the concept of test fixtures. Test fixtures are used to establish the environment for each test, giving the test everything it needs and nothing else. Test fixtures are isolated between tests. With fixtures, you can group tests based on their meaning, instead of their common setup.

###### Built-in fixtures

You have already used test fixtures in your first test.

```js
import { test, expect } from '@playwright/test';

test('basic test', async ({ page }) => {

  await page.goto('https://playwright.dev/');

  await expect(page).toHaveTitle(/Playwright/);

});
```

The `{ page }` argument tells Playwright Test to set up the `page` fixture and provide it to your test function.

Here is a list of the pre-defined fixtures that you are likely to use most of the time:

| Fixture | Type | Description |
| --- | --- | --- |
| page | [Page](https://playwright.dev/docs/api/class-page "Page") | Isolated page for this test run. |
| context | [BrowserContext](https://playwright.dev/docs/api/class-browsercontext "BrowserContext") | Isolated context for this test run. The `page` fixture belongs to this context as well. Learn how to [configure context](https://playwright.dev/docs/test-configuration). |
| browser | [Browser](https://playwright.dev/docs/api/class-browser "Browser") | Browsers are shared across tests to optimize resources. Learn how to [configure browsers](https://playwright.dev/docs/test-configuration). |
| browserName | [string](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Data_structures#String_type "string") | The name of the browser currently running the test. Either `chromium`, `firefox` or `webkit`. |
| request | [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext "APIRequestContext") | Isolated [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext) instance for this test run. |

###### Without fixtures

Here is how a typical test environment setup differs between the traditional test style and the fixture-based one.

`TodoPage` is a class that helps us interact with a "todo list" page of the web app, following the [Page Object Model](https://playwright.dev/docs/pom) pattern. It uses Playwright's `page` internally.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

todo.spec.ts

```js
const { test } = require('@playwright/test');

const { TodoPage } = require('./todo-page');

test.describe('todo tests', () => {

  let todoPage;

  test.beforeEach(async ({ page }) => {

    todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

  });

  test.afterEach(async () => {

    await todoPage.removeAll();

  });

  test('should add an item', async () => {

    await todoPage.addToDo('my item');

    // ...

  });

  test('should remove an item', async () => {

    await todoPage.remove('item1');

    // ...

  });

});
```

###### With fixtures

Fixtures have a number of advantages over before/after hooks:

- Fixtures **encapsulate** setup and teardown in the same place so it is easier to write. So if you have an after hook that tears down what was created in a before hook, consider turning them into a fixture.
- Fixtures are **reusable** between test files - you can define them once and use them in all your tests. That's how Playwright's built-in `page` fixture works. So if you have a helper function that is used in multiple tests, consider turning it into a fixture.
- Fixtures are **on-demand** \- you can define as many fixtures as you'd like, and Playwright Test will setup only the ones needed by your test and nothing else.
- Fixtures are **composable** \- they can depend on each other to provide complex behaviors.
- Fixtures are **flexible**. Tests can use any combination of fixtures to precisely tailor the environment to their needs, without affecting other tests.
- Fixtures simplify **grouping**. You no longer need to wrap tests in `describe`s that set up their environment, and are free to group your tests by their meaning instead.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

example.spec.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

// Extend basic test by providing a "todoPage" fixture.

const test = base.extend<{ todoPage: TodoPage }>({

  todoPage: async ({ page }, use) => {

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

    await use(todoPage);

    await todoPage.removeAll();

  },

});

test('should add an item', async ({ todoPage }) => {

  await todoPage.addToDo('my item');

  // ...

});

test('should remove an item', async ({ todoPage }) => {

  await todoPage.remove('item1');

  // ...

});
```

##### Creating a fixture

To create your own fixture, use [test.extend()](https://playwright.dev/docs/api/class-test#test-extend) to create a new `test` object that will include it.

Below we create two fixtures `todoPage` and `settingsPage` that follow the [Page Object Model](https://playwright.dev/docs/pom) pattern.

Click to expand the code for the `TodoPage` and `SettingsPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

SettingsPage is similar:

settings-page.ts

```js
import type { Page } from '@playwright/test';

export class SettingsPage {

  constructor(public readonly page: Page) {

  }

  async switchToDarkMode() {

    // ...

  }

}
```

my-test.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

import { SettingsPage } from './settings-page';

// Declare the types of your fixtures.

type MyFixtures = {

  todoPage: TodoPage;

  settingsPage: SettingsPage;

};

// Extend base test by providing "todoPage" and "settingsPage".

// This new "test" can be used in multiple test files, and each of them will get the fixtures.

export const test = base.extend<MyFixtures>({

  todoPage: async ({ page }, use) => {

    // Set up the fixture.

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

    // Use the fixture value in the test.

    await use(todoPage);

    // Clean up the fixture.

    await todoPage.removeAll();

  },

  settingsPage: async ({ page }, use) => {

    await use(new SettingsPage(page));

  },

});

export { expect } from '@playwright/test';
```

note

Custom fixture names should start with a letter or underscore, and can contain only letters, numbers, and underscores.

##### Using a fixture

Just mention a fixture in your test function argument, and the test runner will take care of it. Fixtures are also available in hooks and other fixtures. If you use TypeScript, fixtures will be type safe.

Below we use the `todoPage` and `settingsPage` fixtures that we defined above.

```js
import { test, expect } from './my-test';

test.beforeEach(async ({ settingsPage }) => {

  await settingsPage.switchToDarkMode();

});

test('basic test', async ({ todoPage, page }) => {

  await todoPage.addToDo('something nice');

  await expect(page.getByTestId('todo-title')).toContainText(['something nice']);

});
```

##### Overriding fixtures

In addition to creating your own fixtures, you can also override existing fixtures to fit your needs. Consider the following example which overrides the `page` fixture by automatically navigating to the `baseURL`:

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  page: async ({ baseURL, page }, use) => {

    await page.goto(baseURL);

    await use(page);

  },

});
```

Notice that in this example, the `page` fixture is able to depend on other built-in fixtures such as [testOptions.baseURL](https://playwright.dev/docs/api/class-testoptions#test-options-base-url). We can now configure `baseURL` in the configuration file, or locally in the test file with [test.use()](https://playwright.dev/docs/api/class-test#test-use).

example.spec.ts

```js

test.use({ baseURL: 'https://playwright.dev' });
```

Fixtures can also be overridden, causing the base fixture to be completely replaced with something different. For example, we could override the [testOptions.storageState](https://playwright.dev/docs/api/class-testoptions#test-options-storage-state) fixture to provide our own data.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  storageState: async ({}, use) => {

    const cookie = await getAuthCookie();

    await use({ cookies: [cookie] });

  },

});
```

##### Worker-scoped fixtures

Playwright Test uses [worker processes](https://playwright.dev/docs/test-parallel) to run test files. Similar to how test fixtures are set up for individual test runs, worker fixtures are set up for each worker process. That's where you can set up services, run servers, etc. Playwright Test will reuse the worker process for as many test files as it can, provided their worker fixtures match and hence environments are identical.

Below we'll create an `account` fixture that will be shared by all tests in the same worker, and override the `page` fixture to log in to this account for each test. To generate unique accounts, we'll use the [workerInfo.workerIndex](https://playwright.dev/docs/api/class-workerinfo#worker-info-worker-index) that is available to any test or fixture. Note the tuple-like syntax for the worker fixture - we have to pass `{scope: 'worker'}` so that test runner sets this fixture up once per worker.

In addition to only being run once per worker, worker-scoped fixtures also get a separate timeout equal to the default test timeout. You can change it by passing the `timeout` option. See [fixture timeout](https://playwright.dev/docs/test-fixtures#fixture-timeout) for more details.

my-test.ts

```js
import { test as base } from '@playwright/test';

type Account = {

  username: string;

  password: string;

};

// Note that we pass worker fixture types as a second template parameter.

export const test = base.extend<{}, { account: Account }>({

  account: [async ({ browser }, use, workerInfo) => {\
\
    // Unique username.\
\
    const username = 'user' + workerInfo.workerIndex;\
\
    const password = 'verysecure';\
\
    // Create the account with Playwright.\
\
    const page = await browser.newPage();\
\
    await page.goto('/signup');\
\
    await page.getByLabel('User Name').fill(username);\
\
    await page.getByLabel('Password').fill(password);\
\
    await page.getByText('Sign up').click();\
\
    // Make sure everything is ok.\
\
    await expect(page.getByTestId('result')).toHaveText('Success');\
\
    // Do not forget to cleanup.\
\
    await page.close();\
\
    // Use the account value.\
\
    await use({ username, password });\
\
  }, { scope: 'worker' }],

  page: async ({ page, account }, use) => {

    // Sign in with our account.

    const { username, password } = account;

    await page.goto('/signin');

    await page.getByLabel('User Name').fill(username);

    await page.getByLabel('Password').fill(password);

    await page.getByText('Sign in').click();

    await expect(page.getByTestId('userinfo')).toHaveText(username);

    // Use signed-in page in the test.

    await use(page);

  },

});

export { expect } from '@playwright/test';
```

##### Automatic fixtures

Automatic fixtures are set up for each test/worker, even when the test does not list them directly. To create an automatic fixture, use the tuple syntax and pass `{ auto: true }`.

Here is an example fixture that automatically attaches debug logs when the test fails, so we can later review the logs in the reporter. Note how it uses the [TestInfo](https://playwright.dev/docs/api/class-testinfo "TestInfo") object that is available in each test/fixture to retrieve metadata about the test being run.

my-test.ts

```js
import debug from 'debug';

import fs from 'fs';

import { test as base } from '@playwright/test';

export const test = base.extend<{ saveLogs: void }>({

  saveLogs: [async ({}, use, testInfo) => {\
\
    // Collecting logs during the test.\
\
    const logs = [];\
\
    debug.log = (...args) => logs.push(args.map(String).join(''));\
\
    debug.enable('myserver');\
\
    await use();\
\
    // After the test we can check whether the test passed or failed.\
\
    if (testInfo.status !== testInfo.expectedStatus) {\
\
      // outputPath() API guarantees a unique file name.\
\
      const logFile = testInfo.outputPath('logs.txt');\
\
      await fs.promises.writeFile(logFile, logs.join('\n'), 'utf8');\
\
      testInfo.attachments.push({ name: 'logs', contentType: 'text/plain', path: logFile });\
\
    }\
\
  }, { auto: true }],

});

export { expect } from '@playwright/test';
```

##### Fixture timeout

Fixture is considered to be a part of a test, and so its setup and teardown running time counts towards the test timeout. Therefore, a slow fixture may cause test timeouts. You can set a separate larger timeout for such a fixture, and keep the overall test timeout small.

```js
import { test as base, expect } from '@playwright/test';

const test = base.extend<{ slowFixture: string }>({

  slowFixture: [async ({}, use) => {\
\
    // ... perform a slow operation ...\
\
    await use('hello');\
\
  }, { timeout: 60000 }]

});

test('example test', async ({ slowFixture }) => {

  // ...

});
```

Unlike regular test-scoped fixtures, each [worker-scoped](https://playwright.dev/docs/test-fixtures#worker-scoped-fixtures) fixture has its own timeout, equal to the test timeout. You can change the timeout for a worker-scoped fixture in the same way.

##### Fixtures-options

Playwright Test supports running multiple test projects that can be configured separately. You can use "option" fixtures to make your configuration options declarative and type safe. Learn more about [parameterizing tests](https://playwright.dev/docs/test-parameterize).

Below we'll create a `defaultItem` option in addition to the `todoPage` fixture from other examples. This option will be set in the configuration file. Note the tuple syntax and `{ option: true }` argument.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

my-test.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

// Declare your options to type-check your configuration.

export type MyOptions = {

  defaultItem: string;

};

type MyFixtures = {

  todoPage: TodoPage;

};

// Specify both option and fixture types.

export const test = base.extend<MyOptions & MyFixtures>({

  // Define an option and provide a default value.

  // We can later override it in the config.

  defaultItem: ['Something nice', { option: true }],

  // Our "todoPage" fixture depends on the option.

  todoPage: async ({ page, defaultItem }, use) => {

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo(defaultItem);

    await use(todoPage);

    await todoPage.removeAll();

  },

});

export { expect } from '@playwright/test';
```

We can now use the `todoPage` fixture as usual, and set the `defaultItem` option in the configuration file.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

import type { MyOptions } from './my-test';

export default defineConfig<MyOptions>({

  projects: [\
\
    {\
\
      name: 'shopping',\
\
      use: { defaultItem: 'Buy milk' },\
\
    },\
\
    {\
\
      name: 'wellbeing',\
\
      use: { defaultItem: 'Exercise!' },\
\
    },\
\
  ]

});
```

**Array as an option value**

If the value of your option is an array, for example `[{ name: 'Alice' }, { name: 'Bob' }]`, you'll need to wrap it into an extra array when providing the value. This is best illustrated with an example.

```js
type Person = { name: string };

const test = base.extend<{ persons: Person[] }>({

  // Declare the option, default value is an empty array.

  persons: [[], { option: true }],

});

// Option value is an array of persons.

const actualPersons = [{ name: 'Alice' }, { name: 'Bob' }];

test.use({

  // CORRECT: Wrap the value into an array and pass the scope.

  persons: [actualPersons, { scope: 'test' }],

});

test.use({

  // WRONG: passing an array value directly will not work.

  persons: actualPersons,

});
```

**Reset an option**

You can reset an option to the value defined in the config file by setting it to `undefined`. Consider the following config that sets a `baseURL`:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  use: {

    baseURL: 'https://playwright.dev',

  },

});
```

You can now configure `baseURL` for a file, and also opt-out for a single test.

intro.spec.ts

```js
import { test } from '@playwright/test';

// Configure baseURL for this file.

test.use({ baseURL: 'https://playwright.dev/docs/intro' });

test('check intro contents', async ({ page }) => {

  // This test will use "https://playwright.dev/docs/intro" base url as defined above.

});

test.describe(() => {

  // Reset the value to a config-defined one.

  test.use({ baseURL: undefined });

  test('can navigate to intro from the home page', async ({ page }) => {

    // This test will use "https://playwright.dev" base url as defined in the config.

  });

});
```

If you would like to completely reset the value to `undefined`, use a long-form fixture notation.

intro.spec.ts

```js
import { test } from '@playwright/test';

// Completely unset baseURL for this file.

test.use({

  baseURL: [async ({}, use) => use(undefined), { scope: 'test' }],

});

test('no base url', async ({ page }) => {

  // This test will not have a base url.

});
```

##### Execution order

Each fixture has a setup and teardown phase before and after the `await use()` call in the fixture. Setup is executed before the test/hook requiring it is run, and teardown is executed when the fixture is no longer being used by the test/hook.

Fixtures follow these rules to determine the execution order:

- When fixture A depends on fixture B: B is always set up before A and torn down after A.
- Non-automatic fixtures are executed lazily, only when the test/hook needs them.
- Test-scoped fixtures are torn down after each test, while worker-scoped fixtures are only torn down when the worker process executing tests is torn down.

Consider the following example:

```js
import { test as base } from '@playwright/test';

const test = base.extend<{

  testFixture: string,

  autoTestFixture: string,

  unusedFixture: string,

}, {

  workerFixture: string,

  autoWorkerFixture: string,

}>({

  workerFixture: [async ({ browser }) => {\
\
    // workerFixture setup...\
\
    await use('workerFixture');\
\
    // workerFixture teardown...\
\
  }, { scope: 'worker' }],

  autoWorkerFixture: [async ({ browser }) => {\
\
    // autoWorkerFixture setup...\
\
    await use('autoWorkerFixture');\
\
    // autoWorkerFixture teardown...\
\
  }, { scope: 'worker', auto: true }],

  testFixture: [async ({ page, workerFixture }) => {\
\
    // testFixture setup...\
\
    await use('testFixture');\
\
    // testFixture teardown...\
\
  }, { scope: 'test' }],

  autoTestFixture: [async () => {\
\
    // autoTestFixture setup...\
\
    await use('autoTestFixture');\
\
    // autoTestFixture teardown...\
\
  }, { scope: 'test', auto: true }],

  unusedFixture: [async ({ page }) => {\
\
    // unusedFixture setup...\
\
    await use('unusedFixture');\
\
    // unusedFixture teardown...\
\
  }, { scope: 'test' }],

});

test.beforeAll(async () => { /* ... */ });

test.beforeEach(async ({ page }) => { /* ... */ });

test('first test', async ({ page }) => { /* ... */ });

test('second test', async ({ testFixture }) => { /* ... */ });

test.afterEach(async () => { /* ... */ });

test.afterAll(async () => { /* ... */ });
```

Normally, if all tests pass and no errors are thrown, the order of execution is as following.

- worker setup and `beforeAll` section:
  - `browser` setup because it is required by `autoWorkerFixture`.
  - `autoWorkerFixture` setup because automatic worker fixtures are always set up before anything else.
  - `beforeAll` runs.
- `first test`section:
  - `autoTestFixture` setup because automatic test fixtures are always set up before test and `beforeEach` hooks.
  - `page` setup because it is required in `beforeEach` hook.
  - `beforeEach` runs.
  - `first test` runs.
  - `afterEach` runs.
  - `page` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `autoTestFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
- `second test`section:
  - `autoTestFixture` setup because automatic test fixtures are always set up before test and `beforeEach` hooks.
  - `page` setup because it is required in `beforeEach` hook.
  - `beforeEach` runs.
  - `workerFixture` setup because it is required by `testFixture` that is required by the `second test`.
  - `testFixture` setup because it is required by the `second test`.
  - `second test` runs.
  - `afterEach` runs.
  - `testFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `page` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `autoTestFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
- `afterAll`and worker teardown section:
  - `afterAll` runs.
  - `workerFixture` teardown because it is a workers-scoped fixture and should be torn down once at the end.
  - `autoWorkerFixture` teardown because it is a workers-scoped fixture and should be torn down once at the end.
  - `browser` teardown because it is a workers-scoped fixture and should be torn down once at the end.

A few observations:

- `page` and `autoTestFixture` are set up and torn down for each test, as test-scoped fixtures.
- `unusedFixture` is never set up because it is not used by any tests/hooks.
- `testFixture` depends on `workerFixture` and triggers its setup.
- `workerFixture` is lazily set up before the second test, but torn down once during worker shutdown, as a worker-scoped fixture.
- `autoWorkerFixture` is set up for `beforeAll` hook, but `autoTestFixture` is not.

##### Combine custom fixtures from multiple modules

You can merge test fixtures from multiple files or modules:

fixtures.ts

```js
import { mergeTests } from '@playwright/test';

import { test as dbTest } from 'database-test-utils';

import { test as a11yTest } from 'a11y-test-utils';

export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test } from './fixtures';

test('passes', async ({ database, page, a11y }) => {

  // use database and a11y fixtures.

});
```

##### Box fixtures

Usually, custom fixtures are reported as separate steps in the UI mode, Trace Viewer and various test reports. They also appear in error messages from the test runner. For frequently used fixtures, this can mean lots of noise. You can stop the fixtures steps from being shown in the UI by "boxing" it.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  helperFixture: [async ({}, use, testInfo) => {\
\
    // ...\
\
  }, { box: true }],

});
```

This is useful for non-interesting helper fixtures. For example, an [automatic](https://playwright.dev/docs/test-fixtures#automatic-fixtures) fixture that sets up some common data can be safely hidden from a test report.

You can also mark the fixture as `box: 'self'` to only hide that particular fixture, but include all the steps inside the fixture in the test report.

##### Custom fixture title

Instead of the usual fixture name, you can give fixtures a custom title that will be shown in test reports and error messages.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  innerFixture: [async ({}, use, testInfo) => {\
\
    // ...\
\
  }, { title: 'my fixture' }],

});
```

##### Adding global beforeEach/afterEach hooks

[test.beforeEach()](https://playwright.dev/docs/api/class-test#test-before-each) and [test.afterEach()](https://playwright.dev/docs/api/class-test#test-after-each) hooks run before/after each test declared in the same file and same [test.describe()](https://playwright.dev/docs/api/class-test#test-describe) block (if any). If you want to declare hooks that run before/after each test globally, you can declare them as auto fixtures like this:

fixtures.ts

```js
import { test as base } from '@playwright/test';

export const test = base.extend<{ forEachTest: void }>({

  forEachTest: [async ({ page }, use) => {\
\
    // This code runs before every test.\
\
    await page.goto('http://localhost:8000');\
\
    await use();\
\
    // This code runs after every test.\
\
    console.log('Last URL:', page.url());\
\
  }, { auto: true }],  // automatically starts for every test.

});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';

import { expect } from '@playwright/test';

test('basic', async ({ page }) => {

  expect(page).toHaveURL('http://localhost:8000');

  await page.goto('https://playwright.dev');

});
```

##### Adding global beforeAll/afterAll hooks

[test.beforeAll()](https://playwright.dev/docs/api/class-test#test-before-all) and [test.afterAll()](https://playwright.dev/docs/api/class-test#test-after-all) hooks run before/after all tests declared in the same file and same [test.describe()](https://playwright.dev/docs/api/class-test#test-describe) block (if any), once per worker process. If you want to declare hooks that run before/after all tests in every file, you can declare them as auto fixtures with `scope: 'worker'` as follows:

fixtures.ts

```js
import { test as base } from '@playwright/test';

export const test = base.extend<{}, { forEachWorker: void }>({

  forEachWorker: [async ({}, use) => {\
\
    // This code runs before all the tests in the worker process.\
\
    console.log(`Starting test worker ${test.info().workerIndex}`);\
\
    await use();\
\
    // This code runs after all the tests in the worker process.\
\
    console.log(`Stopping test worker ${test.info().workerIndex}`);\
\
  }, { scope: 'worker', auto: true }],  // automatically starts for every worker.

});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';

import { expect } from '@playwright/test';

test('basic', async ({ }) => {

  // ...

});
```

Note that the fixtures will still run once per [worker process](https://playwright.dev/docs/test-parallel#worker-processes), but you don't need to redeclare them in every file.

### 4. Playwright — Assertions

- Source: https://playwright.dev/docs/test-assertions
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

Playwright includes test assertions in the form of `expect` function. To make an assertion, call `expect(value)` and choose a matcher that reflects the expectation. There are many [generic matchers](https://playwright.dev/docs/api/class-genericassertions) like `toEqual`, `toContain`, `toBeTruthy` that can be used to assert any conditions.

```js
expect(success).toBeTruthy();
```

Playwright also includes web-specific [async matchers](https://playwright.dev/docs/api/class-locatorassertions) that will wait until the expected condition is met. Consider the following example:

```js
await expect(page.getByTestId('status')).toHaveText('Submitted');
```

Playwright will be re-testing the element with the test id of `status` until the fetched element has the `"Submitted"` text. It will re-fetch the element and check it over and over, until the condition is met or until the timeout is reached. You can either pass this timeout or configure it once via the [testConfig.expect](https://playwright.dev/docs/api/class-testconfig#test-config-expect) value in the test config.

By default, the timeout for assertions is set to 5 seconds. Learn more about [various timeouts](https://playwright.dev/docs/test-timeouts).

##### Auto-retrying assertions

The following assertions will retry until the assertion passes, or the assertion timeout is reached. Note that retrying assertions are async, so you must `await` them.

| Assertion | Description |
| --- | --- |
| [await expect(locator).toBeAttached()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-attached) | Element is attached |
| [await expect(locator).toBeChecked()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-checked) | Checkbox is checked |
| [await expect(locator).toBeDisabled()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-disabled) | Element is disabled |
| [await expect(locator).toBeEditable()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-editable) | Element is editable |
| [await expect(locator).toBeEmpty()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-empty) | Container is empty |
| [await expect(locator).toBeEnabled()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-enabled) | Element is enabled |
| [await expect(locator).toBeFocused()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-focused) | Element is focused |
| [await expect(locator).toBeHidden()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-hidden) | Element is not visible |
| [await expect(locator).toBeInViewport()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-in-viewport) | Element intersects viewport |
| [await expect(locator).toBeVisible()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-visible) | Element is visible |
| [await expect(locator).toContainText()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-contain-text) | Element contains text |
| [await expect(locator).toContainClass()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-contain-class) | Element has specified CSS classes |
| [await expect(locator).toHaveAccessibleDescription()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-accessible-description) | Element has a matching [accessible description](https://w3c.github.io/accname/#dfn-accessible-description) |
| [await expect(locator).toHaveAccessibleName()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-accessible-name) | Element has a matching [accessible name](https://w3c.github.io/accname/#dfn-accessible-name) |
| [await expect(locator).toHaveAttribute()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-attribute) | Element has a DOM attribute |
| [await expect(locator).toHaveClass()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-class) | Element has specified CSS class property |
| [await expect(locator).toHaveCount()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-count) | List has exact number of children |
| [await expect(locator).toHaveCSS()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-css) | Element has CSS property |
| [await expect(locator).toHaveId()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-id) | Element has an ID |
| [await expect(locator).toHaveJSProperty()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-js-property) | Element has a JavaScript property |
| [await expect(locator).toHaveRole()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-role) | Element has a specific [ARIA role](https://www.w3.org/TR/wai-aria-1.2/#roles) |
| [await expect(locator).toHaveScreenshot()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-screenshot-1) | Element has a screenshot |
| [await expect(locator).toHaveText()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-text) | Element matches text |
| [await expect(locator).toHaveValue()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-value) | Input has a value |
| [await expect(locator).toHaveValues()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-values) | Select has options selected |
| [await expect(locator).toMatchAriaSnapshot()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-match-aria-snapshot) | Element matches the Aria snapshot |
| [await expect(page).toMatchAriaSnapshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-match-aria-snapshot) | Page matches the Aria snapshot |
| [await expect(page).toHaveScreenshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-screenshot-1) | Page has a screenshot |
| [await expect(page).toHaveTitle()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-title) | Page has a title |
| [await expect(page).toHaveURL()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-url) | Page has a URL |
| [await expect(response).toBeOK()](https://playwright.dev/docs/api/class-apiresponseassertions#api-response-assertions-to-be-ok) | Response has an OK status |

##### Non-retrying assertions

These assertions allow to test any conditions, but do not auto-retry. Most of the time, web pages show information asynchronously, and using non-retrying assertions can lead to a flaky test.

Prefer [auto-retrying](https://playwright.dev/docs/test-assertions#auto-retrying-assertions) assertions whenever possible. For more complex assertions that need to be retried, use [`expect.poll`](https://playwright.dev/docs/test-assertions#expectpoll) or [`expect.toPass`](https://playwright.dev/docs/test-assertions#expecttopass).

| Assertion | Description |
| --- | --- |
| [expect(value).toBe()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be) | Value is the same |
| [expect(value).toBeCloseTo()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-close-to) | Number is approximately equal |
| [expect(value).toBeDefined()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-defined) | Value is not `undefined` |
| [expect(value).toBeFalsy()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-falsy) | Value is falsy, e.g. `false`, `0`, `null`, etc. |
| [expect(value).toBeGreaterThan()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-greater-than) | Number is more than |
| [expect(value).toBeGreaterThanOrEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-greater-than-or-equal) | Number is more than or equal |
| [expect(value).toBeInstanceOf()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-instance-of) | Object is an instance of a class |
| [expect(value).toBeLessThan()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-less-than) | Number is less than |
| [expect(value).toBeLessThanOrEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-less-than-or-equal) | Number is less than or equal |
| [expect(value).toBeNaN()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-na-n) | Value is `NaN` |
| [expect(value).toBeNull()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-null) | Value is `null` |
| [expect(value).toBeTruthy()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-truthy) | Value is truthy, i.e. not `false`, `0`, `null`, etc. |
| [expect(value).toBeUndefined()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-undefined) | Value is `undefined` |
| [expect(value).toContain()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-1) | String contains a substring |
| [expect(value).toContain()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-2) | Array or set contains an element |
| [expect(value).toContainEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-equal) | Array or set contains a similar element |
| [expect(value).toEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-equal) | Value is similar - deep equality and pattern matching |
| [expect(value).toHaveLength()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-have-length) | Array or string has length |
| [expect(value).toHaveProperty()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-have-property) | Object has a property |
| [expect(value).toMatch()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-match) | String matches a regular expression |
| [expect(value).toMatchObject()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-match-object) | Object contains specified properties |
| [expect(value).toStrictEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-strict-equal) | Value is similar, including property types |
| [expect(value).toThrow()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-throw) | Function throws an error |

##### Asymmetric matchers

These expressions can be nested in other assertions to allow more relaxed matching against a given condition.

| Matcher | Description |
| --- | --- |
| [expect.any()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-any) | Matches any instance of a class/primitive |
| [expect.anything()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-anything) | Matches anything |
| [expect.arrayContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-array-containing) | Array contains specific elements |
| [expect.arrayOf()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-array-of) | Array contains elements of specific type |
| [expect.closeTo()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-close-to) | Number is approximately equal |
| [expect.objectContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-object-containing) | Object contains specific properties |
| [expect.stringContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-string-containing) | String contains a substring |
| [expect.stringMatching()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-string-matching) | String matches a regular expression |

##### Negating matchers

In general, we can expect the opposite to be true by adding a `.not` to the front of the matchers:

```js
expect(value).not.toEqual(0);

await expect(locator).not.toContainText('some text');
```

##### Soft assertions

By default, failed assertion will terminate test execution. Playwright also supports _soft assertions_: failed soft assertions **do not** terminate test execution, but mark the test as failed.

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

await expect.soft(page.getByTestId('eta')).toHaveText('1 day');

// ... and continue the test to check more things.

await page.getByRole('link', { name: 'next page' }).click();

await expect.soft(page.getByRole('heading', { name: 'Make another order' })).toBeVisible();
```

At any point during test execution, you can check whether there were any soft assertion failures:

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

await expect.soft(page.getByTestId('eta')).toHaveText('1 day');

// Avoid running further if there were soft assertion failures.

expect(test.info().errors).toHaveLength(0);
```

Note that soft assertions only work with Playwright test runner.

##### Custom expect message

You can specify a custom expect message as a second argument to the `expect` function, for example:

```js
await expect(page.getByText('Name'), 'should be logged in').toBeVisible();
```

This message will be shown in reporters, both for passing and failing expects, providing more context about the assertion.

When expect passes, you might see a successful step like this:

```txt
✅ should be logged in    @example.spec.ts:18
```

When expect fails, the error would look like this:

```bash
    Error: should be logged in

    Call log:

      - expect.toBeVisible with timeout 5000ms

      - waiting for "getByText('Name')"

      2 |

      3 | test('example test', async({ page }) => {

    > 4 |   await expect(page.getByText('Name'), 'should be logged in').toBeVisible();

        |                                                                  ^

      5 | });

      6 |
```

Soft assertions also support custom message:

```js
expect.soft(value, 'my soft assertion').toBe(56);
```

##### expect.configure

You can create your own pre-configured `expect` instance to have its own defaults such as `timeout` and `soft`.

```js
const slowExpect = expect.configure({ timeout: 10000 });

await slowExpect(locator).toHaveText('Submit');

// Always do soft assertions.

const softExpect = expect.configure({ soft: true });

await softExpect(locator).toHaveText('Submit');
```

##### expect.poll

You can convert any synchronous `expect` to an asynchronous polling one using `expect.poll`.

The following method will poll given function until it returns HTTP status 200:

```js
await expect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}, {

  // Custom expect message for reporting, optional.

  message: 'make sure API eventually succeeds',

  // Poll for 10 seconds; defaults to 5 seconds. Pass 0 to disable timeout.

  timeout: 10000,

}).toBe(200);
```

You can also specify custom polling intervals:

```js
await expect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}, {

  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe

  // ... Defaults to [100, 250, 500, 1000].

  intervals: [1_000, 2_000, 10_000],

  timeout: 60_000

}).toBe(200);
```

You can combine `expect.soft` with `expect.poll` to perform soft assertions in polling logic. This allows the test to continue even if the assertion inside poll fails.

```js
await expect.soft.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}).toBe(200);
```

`expect.configure({ soft: true })` also chains with `expect.poll` and is useful when you want to reuse a configured instance.

```js
const softExpect = expect.configure({ soft: true });

await softExpect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}).toBe(200);
```

##### expect.toPass

You can retry blocks of code until they are passing successfully.

```js
await expect(async () => {

  const response = await page.request.get('https://api.example.com');

  expect(response.status()).toBe(200);

}).toPass();
```

You can also specify custom timeout and retry intervals:

```js
await expect(async () => {

  const response = await page.request.get('https://api.example.com');

  expect(response.status()).toBe(200);

}).toPass({

  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe

  // ... Defaults to [100, 250, 500, 1000].

  intervals: [1_000, 2_000, 10_000],

  timeout: 60_000

});
```

Note that by default `toPass` has timeout 0 and does not respect custom [expect timeout](https://playwright.dev/docs/test-timeouts#expect-timeout).

##### Add custom matchers using expect.extend

You can extend Playwright assertions by providing custom matchers. These matchers will be available on the `expect` object.

In this example we add a custom `toHaveAmount` function. Custom matcher should return a `pass` flag indicating whether the assertion passed, and a `message` callback that's used when the assertion fails.

fixtures.ts

```js
import { expect as baseExpect } from '@playwright/test';

import type { Locator } from '@playwright/test';

export { test } from '@playwright/test';

export const expect = baseExpect.extend({

  async toHaveAmount(locator: Locator, expected: number, options?: { timeout?: number }) {

    const assertionName = 'toHaveAmount';

    let pass: boolean;

    let matcherResult: any;

    try {

      const expectation = this.isNot ? baseExpect(locator).not : baseExpect(locator);

      await expectation.toHaveAttribute('data-amount', String(expected), options);

      pass = true;

    } catch (e: any) {

      matcherResult = e.matcherResult;

      pass = false;

    }

    if (this.isNot) {

      pass =!pass;

    }

    const message = pass

      ? () => this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +

          '\n\n' +

          `Locator: ${locator}\n` +

          `Expected: not ${this.utils.printExpected(expected)}\n` +

          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '')

      : () =>  this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +

          '\n\n' +

          `Locator: ${locator}\n` +

          `Expected: ${this.utils.printExpected(expected)}\n` +

          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '');

    return {

      message,

      pass,

      name: assertionName,

      expected,

      actual: matcherResult?.actual,

    };

  },

});
```

Now we can use `toHaveAmount` in the test.

example.spec.ts

```js
import { test, expect } from './fixtures';

test('amount', async () => {

  await expect(page.locator('.cart')).toHaveAmount(4);

});
```

###### Compatibility with expect library

note

Do not confuse Playwright's `expect` with the [`expect` library](https://jestjs.io/docs/expect). The latter is not fully integrated with Playwright test runner, so make sure to use Playwright's own `expect`.

###### Combine custom matchers from multiple modules

You can combine custom matchers from multiple files or modules.

fixtures.ts

```js
import { mergeTests, mergeExpects } from '@playwright/test';

import { test as dbTest, expect as dbExpect } from 'database-test-utils';

import { test as a11yTest, expect as a11yExpect } from 'a11y-test-utils';

export const expect = mergeExpects(dbExpect, a11yExpect);

export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test, expect } from './fixtures';

test('passes', async ({ database }) => {

  await expect(database).toHaveDatabaseUser('admin');

});
```

### 6. Playwright — Authentication

- Source: https://playwright.dev/docs/auth
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

Playwright executes tests in isolated environments called [browser contexts](https://playwright.dev/docs/browser-contexts). This isolation model improves reproducibility and prevents cascading test failures. Tests can load existing authenticated state. This eliminates the need to authenticate in every test and speeds up test execution.

##### Core concepts

Regardless of the authentication strategy you choose, you are likely to store authenticated browser state on the file system.

We recommend to create `playwright/.auth` directory and add it to your `.gitignore`. Your authentication routine will produce authenticated browser state and save it to a file in this `playwright/.auth` directory. Later on, tests will reuse this state and start already authenticated.

danger

The browser state file may contain sensitive cookies and headers that could be used to impersonate you or your test account. We strongly discourage checking them into private or public repositories.

- Bash
- PowerShell
- Batch

```bash
mkdir -p playwright/.auth

echo $'\nplaywright/.auth' >> .gitignore
```

```powershell
New-Item -ItemType Directory -Force -Path playwright\.auth

Add-Content -path .gitignore "`r`nplaywright/.auth"
```

```batch
md playwright\.auth

echo. >> .gitignore

echo "playwright/.auth" >> .gitignore
```

##### Basic: shared account in all tests

This is the **recommended** approach for tests **without server-side state**. Authenticate once in the **setup project**, save the authentication state, and then reuse it to bootstrap each test already authenticated.

**When to use**

- When you can imagine all your tests running at the same time with the same account, without affecting each other.

**When not to use**

- Your tests modify server-side state. For example, one test checks the rendering of the settings page, while the other test is changing the setting, and you run tests in parallel. In this case, tests must use different accounts.
- Your authentication is browser-specific.

**Details**

Create `tests/auth.setup.ts` that will prepare authenticated browser state for all other tests.

tests/auth.setup.ts

```js
import { test as setup, expect } from '@playwright/test';

import path from 'path';

const authFile = path.join(__dirname, '../playwright/.auth/user.json');

setup('authenticate', async ({ page }) => {

  // Perform authentication steps. Replace these actions with your own.

  await page.goto('https://github.com/login');

  await page.getByLabel('Username or email address').fill('username');

  await page.getByLabel('Password').fill('password');

  await page.getByRole('button', { name: 'Sign in' }).click();

  // Wait until the page receives the cookies.

  //

  // Sometimes login flow sets cookies in the process of several redirects.

  // Wait for the final URL to ensure that the cookies are actually set.

  await page.waitForURL('https://github.com/');

  // Alternatively, you can wait until the page reaches a state where all cookies are set.

  await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();

  // End of authentication steps.

  await page.context().storageState({ path: authFile });

});
```

Create a new `setup` project in the config and declare it as a [dependency](https://playwright.dev/docs/test-projects#dependencies) for all your testing projects. This project will always run and authenticate before all the tests. All testing projects should use the authenticated state as `storageState`.

playwright.config.ts

```js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

  projects: [\
\
    // Setup project\
\
    { name: 'setup', testMatch: /.*\.setup\.ts/ },\
\
    {\
\
      name: 'chromium',\
\
      use: {\
\
        ...devices['Desktop Chrome'],\
\
        // Use prepared auth state.\
\
        storageState: 'playwright/.auth/user.json',\
\
      },\
\
      dependencies: ['setup'],\
\
    },\
\
    {\
\
      name: 'firefox',\
\
      use: {\
\
        ...devices['Desktop Firefox'],\
\
        // Use prepared auth state.\
\
        storageState: 'playwright/.auth/user.json',\
\
      },\
\
      dependencies: ['setup'],\
\
    },\
\
  ],

});
```

Tests start already authenticated because we specified `storageState` in the config.

tests/example.spec.ts

```js
import { test } from '@playwright/test';

test('test', async ({ page }) => {

  // page is authenticated

});
```

Note that you need to delete the stored state when it expires. If you don't need to keep the state between test runs, write the browser state under [testProject.outputDir](https://playwright.dev/docs/api/class-testproject#test-project-output-dir), which is automatically cleaned up before every test run.

###### Authenticating in UI mode

UI mode will not run the `setup` project by default to improve testing speed. We recommend to authenticate by manually running the `auth.setup.ts` from time to time, whenever existing authentication expires.

First [enable the `setup` project in the filters](https://playwright.dev/docs/test-ui-mode#filtering-tests), then click the triangle button next to `auth.setup.ts` file, and then disable the `setup` project in the filters again.

##### Moderate: one account per parallel worker

This is the **recommended** approach for tests that **modify server-side state**. In Playwright, worker processes run in parallel. In this approach, each parallel worker is authenticated once. All tests ran by worker are reusing the same authentication state. We will need multiple testing accounts, one per each parallel worker.

**When to use**

- Your tests modify shared server-side state. For example, one test checks the rendering of the settings page, while the other test is changing the setting.

**When not to use**

- Your tests do not modify any shared server-side state. In this case, all tests can use a single shared account.

**Details**

We will authenticate once per [worker process](https://playwright.dev/docs/test-parallel#worker-processes), each with a unique account.

Create `playwright/fixtures.ts` file that will [override `storageState` fixture](https://playwright.dev/docs/test-fixtures#overriding-fixtures) to authenticate once per worker. Use [testInfo.parallelIndex](https://playwright.dev/docs/api/class-testinfo#test-info-parallel-index) to differentiate between workers.

playwright/fixtures.ts

```js
import { test as baseTest, expect } from '@playwright/test';

import fs from 'fs';

import path from 'path';

export * from '@playwright/test';

export const test = baseTest.extend<{}, { workerStorageState: string }>({

  // Use the same storage state for all tests in this worker.

  storageState: ({ workerStorageState }, use) => use(workerStorageState),

  // Authenticate once per worker with a worker-scoped fixture.

  workerStorageState: [async ({ browser }, use) => {\
\
    // Use parallelIndex as a unique identifier for each worker.\
\
    const id = test.info().parallelIndex;\
\
    const fileName = path.resolve(test.info().project.outputDir, `.auth/${id}.json`);\
\
    if (fs.existsSync(fileName)) {\
\
      // Reuse existing authentication state if any.\
\
      await use(fileName);\
\
      return;\
\
    }\
\
    // Important: make sure we authenticate in a clean environment by unsetting storage state.\
\
    const page = await browser.newPage({ storageState: undefined });\
\
    // Acquire a unique account, for example create a new one.\
\
    // Alternatively, you can have a list of precreated accounts for testing.\
\
    // Make sure that accounts are unique, so that multiple team members\
\
    // can run tests at the same time without interference.\
\
    const account = await acquireAccount(id);\
\
    // Perform authentication steps. Replace these actions with your own.\
\
    await page.goto('https://github.com/login');\
\
    await page.getByLabel('Username or email address').fill(account.username);\
\
    await page.getByLabel('Password').fill(account.password);\
\
    await page.getByRole('button', { name: 'Sign in' }).click();\
\
    // Wait until the page receives the cookies.\
\
    //\
\
    // Sometimes login flow sets cookies in the process of several redirects.\
\
    // Wait for the final URL to ensure that the cookies are actually set.\
\
    await page.waitForURL('https://github.com/');\
\
    // Alternatively, you can wait until the page reaches a state where all cookies are set.\
\
    await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();\
\
    // End of authentication steps.\
\
    await page.context().storageState({ path: fileName });\
\
    await page.close();\
\
    await use(fileName);\
\
  }, { scope: 'worker' }],

});
```

Now, each test file should import `test` from our fixtures file instead of `@playwright/test`. No changes are needed in the config.

tests/example.spec.ts

```js
// Important: import our fixtures.

import { test, expect } from '../playwright/fixtures';

test('test', async ({ page }) => {

  // page is authenticated

});
```

##### Advanced scenarios

###### Authenticate with API request

**When to use**

- Your web application supports authenticating via API that is easier/faster than interacting with the app UI.

**Details**

We will send the API request with [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext "APIRequestContext") and then save authenticated state as usual.

In the [setup project](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests):

tests/auth.setup.ts

```js
import { test as setup } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('authenticate', async ({ request }) => {

  // Send authentication request. Replace with your own.

  await request.post('https://github.com/login', {

    form: {

      'user': 'user',

      'password': 'password'

    }

  });

  await request.storageState({ path: authFile });

});
```

Alternatively, in a [worker fixture](https://playwright.dev/docs/auth#moderate-one-account-per-parallel-worker):

playwright/fixtures.ts

```js
import { test as baseTest, request } from '@playwright/test';

import fs from 'fs';

import path from 'path';

export * from '@playwright/test';

export const test = baseTest.extend<{}, { workerStorageState: string }>({

  // Use the same storage state for all tests in this worker.

  storageState: ({ workerStorageState }, use) => use(workerStorageState),

  // Authenticate once per worker with a worker-scoped fixture.

  workerStorageState: [async ({}, use) => {\
\
    // Use parallelIndex as a unique identifier for each worker.\
\
    const id = test.info().parallelIndex;\
\
    const fileName = path.resolve(test.info().project.outputDir, `.auth/${id}.json`);\
\
    if (fs.existsSync(fileName)) {\
\
      // Reuse existing authentication state if any.\
\
      await use(fileName);\
\
      return;\
\
    }\
\
    // Important: make sure we authenticate in a clean environment by unsetting storage state.\
\
    const context = await request.newContext({ storageState: undefined });\
\
    // Acquire a unique account, for example create a new one.\
\
    // Alternatively, you can have a list of precreated accounts for testing.\
\
    // Make sure that accounts are unique, so that multiple team members\
\
    // can run tests at the same time without interference.\
\
    const account = await acquireAccount(id);\
\
    // Send authentication request. Replace with your own.\
\
    await context.post('https://github.com/login', {\
\
      form: {\
\
        'user': 'user',\
\
        'password': 'password'\
\
      }\
\
    });\
\
    await context.storageState({ path: fileName });\
\
    await context.dispose();\
\
    await use(fileName);\
\
  }, { scope: 'worker' }],

});
```

###### Multiple signed in roles

**When to use**

- You have more than one role in your end to end tests, but you can reuse accounts across all tests.

**Details**

We will authenticate multiple times in the setup project.

tests/auth.setup.ts

```js
import { test as setup, expect } from '@playwright/test';

const adminFile = 'playwright/.auth/admin.json';

setup('authenticate as admin', async ({ page }) => {

  // Perform authentication steps. Replace these actions with your own.

  await page.goto('https://github.com/login');

  await page.getByLabel('Username or email address').fill('admin');

  await page.getByLabel('Password').fill('password');

  await page.getByRole('button', { name: 'Sign in' }).click();

  // Wait until the page receives the cookies.

  //

  // Sometimes login flow sets cookies in the process of several redirects.

  // Wait for the final URL to ensure that the cookies are actually set.

  await page.waitForURL('https://github.com/');

  // Alternatively, you can wait until the page reaches a state where all cookies are set.

  await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();

  // End of authentication steps.

  await page.context().storageState({ path: adminFile });

});

const userFile = 'playwright/.auth/user.json';

setup('authenticate as user', async ({ page }) => {

  // Perform authentication steps. Replace these actions with your own.

  await page.goto('https://github.com/login');

  await page.getByLabel('Username or email address').fill('user');

  await page.getByLabel('Password').fill('password');

  await page.getByRole('button', { name: 'Sign in' }).click();

  // Wait until the page receives the cookies.

  //

  // Sometimes login flow sets cookies in the process of several redirects.

  // Wait for the final URL to ensure that the cookies are actually set.

  await page.waitForURL('https://github.com/');

  // Alternatively, you can wait until the page reaches a state where all cookies are set.

  await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();

  // End of authentication steps.

  await page.context().storageState({ path: userFile });

});
```

After that, specify `storageState` for each test file or test group, **instead of** setting it in the config.

tests/example.spec.ts

```js
import { test } from '@playwright/test';

test.use({ storageState: 'playwright/.auth/admin.json' });

test('admin test', async ({ page }) => {

  // page is authenticated as admin

});

test.describe(() => {

  test.use({ storageState: 'playwright/.auth/user.json' });

  test('user test', async ({ page }) => {

    // page is authenticated as a user

  });

});
```

See also about [authenticating in the UI mode](https://playwright.dev/docs/auth#authenticating-in-ui-mode).

###### Testing multiple roles together

**When to use**

- You need to test how multiple authenticated roles interact together, in a single test.

**Details**

Use multiple [BrowserContext](https://playwright.dev/docs/api/class-browsercontext "BrowserContext") s and [Page](https://playwright.dev/docs/api/class-page "Page") s with different storage states in the same test.

tests/example.spec.ts

```js
import { test } from '@playwright/test';

test('admin and user', async ({ browser }) => {

  // adminContext and all pages inside, including adminPage, are signed in as "admin".

  const adminContext = await browser.newContext({ storageState: 'playwright/.auth/admin.json' });

  const adminPage = await adminContext.newPage();

  // userContext and all pages inside, including userPage, are signed in as "user".

  const userContext = await browser.newContext({ storageState: 'playwright/.auth/user.json' });

  const userPage = await userContext.newPage();

  // ... interact with both adminPage and userPage ...

  await adminContext.close();

  await userContext.close();

});
```

###### Testing multiple roles with POM fixtures

**When to use**

- You need to test how multiple authenticated roles interact together, in a single test.

**Details**

You can introduce fixtures that will provide a page authenticated as each role.

Below is an example that [creates fixtures](https://playwright.dev/docs/test-fixtures#creating-a-fixture) for two [Page Object Models](https://playwright.dev/docs/pom) \- admin POM and user POM. It assumes `adminStorageState.json` and `userStorageState.json` files were created in the global setup.

playwright/fixtures.ts

```js
import { test as base, type Page, type Locator } from '@playwright/test';

// Page Object Model for the "admin" page.

// Here you can add locators and helper methods specific to the admin page.

class AdminPage {

  // Page signed in as "admin".

  page: Page;

  // Example locator pointing to "Welcome, Admin" greeting.

  greeting: Locator;

  constructor(page: Page) {

    this.page = page;

    this.greeting = page.locator('#greeting');

  }

}

// Page Object Model for the "user" page.

// Here you can add locators and helper methods specific to the user page.

class UserPage {

  // Page signed in as "user".

  page: Page;

  // Example locator pointing to "Welcome, User" greeting.

  greeting: Locator;

  constructor(page: Page) {

    this.page = page;

    this.greeting = page.locator('#greeting');

  }

}

// Declare the types of your fixtures.

type MyFixtures = {

  adminPage: AdminPage;

  userPage: UserPage;

};

export * from '@playwright/test';

export const test = base.extend<MyFixtures>({

  adminPage: async ({ browser }, use) => {

    const context = await browser.newContext({ storageState: 'playwright/.auth/admin.json' });

    const adminPage = new AdminPage(await context.newPage());

    await use(adminPage);

    await context.close();

  },

  userPage: async ({ browser }, use) => {

    const context = await browser.newContext({ storageState: 'playwright/.auth/user.json' });

    const userPage = new UserPage(await context.newPage());

    await use(userPage);

    await context.close();

  },

});
```

tests/example.spec.ts

```js
// Import test with our new fixtures.

import { test, expect } from '../playwright/fixtures';

// Use adminPage and userPage fixtures in the test.

test('admin and user', async ({ adminPage, userPage }) => {

  // ... interact with both adminPage and userPage ...

  await expect(adminPage.greeting).toHaveText('Welcome, Admin');

  await expect(userPage.greeting).toHaveText('Welcome, User');

});
```

###### Session storage

Reusing authenticated state covers [cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies), [local storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage), [IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) and passkey ( [WebAuthn](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API)) based authentication. Rarely, [session storage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) is used for storing information associated with the signed-in state. Session storage is specific to a particular domain and is not persisted across page loads. Playwright does not provide API to persist session storage, but the following snippet can be used to save/load session storage.

```js
// Get session storage and store as env variable

const sessionStorage = await page.evaluate(() => JSON.stringify(sessionStorage));

fs.writeFileSync('playwright/.auth/session.json', sessionStorage, 'utf-8');

// Set session storage in a new context

const sessionStorage = JSON.parse(fs.readFileSync('playwright/.auth/session.json', 'utf-8'));

await context.addInitScript(storage => {

  if (window.location.hostname === 'example.com') {

    for (const [key, value] of Object.entries(storage))

      window.sessionStorage.setItem(key, value);

  }

}, sessionStorage);
```

###### Avoid authentication in some tests

You can reset storage state in a test file to avoid authentication that was set up for the whole project.

not-signed-in.spec.ts

```js
import { test } from '@playwright/test';

// Reset storage state for this file to avoid being authenticated

test.use({ storageState: { cookies: [], origins: [] } });

test('not signed in test', async ({ page }) => {

  // ...

});
```

### 7. Yevhen Laichenkov — 17 Playwright Testing Mistakes You Should Avoid

- Source: https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### 17 Playwright Testing Mistakes You Should Avoid

14 Feb, 2026

I keep seeing the same pattern: tests start flaking and the blame goes to data, CI, browsers, or infrastructure. Then the test gets “fixed” with sleeps, forced actions that skip [actionability checks](https://playwright.dev/docs/actionability), and custom retry or wait helpers that reimplement what Playwright already provides. Sometimes deprecated APIs even make it into fresh code, which guarantees maintenance trouble later. In this article, I go through the most common mistakes I see in projects.

TL;DR

01. Every test needs assertions. If there are no checks, it’s just a script.
02. Use [web-first assertions](https://playwright.dev/docs/test-assertions) instead of one-shot checks `isVisible`, `textContent`, `toBe`
03. Stop using sleeps like `waitForTimeout`.
04. Stop treating `networkidle` as “page is ready.”
05. Don’t pre-wait before actions that already auto-wait `click`, `fill`, `check`
06. Don’t try to solve UI issues with `{ force: true }`
07. For `waitForResponse`: listen first, trigger request second, await third
08. Don’t write manual retry loops. Use `toPass` or [`expect.poll`](https://playwright.dev/docs/test-assertions#expectpoll)
09. When using `toPass`, keep inner assertion timeouts short
10. Stop using deprecated APIs and options `waitForNavigation`, `waitForSelector`
11. Make locators strict with `{ exact: true }`
12. Use `expect.poll` for specific polling scenarios, not basic DOM checks
13. Use `waitForFunction` only for truly custom conditions
14. Prefer positive assertions `toBeHidden` over negative ones `not.toBeVisible` when possible
15. Add [`eslint-plugin-playwright`](https://github.com/playwright-community/eslint-plugin-playwright) and catch bad patterns before they make it into your codebase
16. Keep page-object actions simple. Avoid returning new page objects from every action
17. Don’t make tests depend on each other with `test.describe.serial`

Let’s dive into each of these anti-patterns and how to fix them.

##### 1\. Forgetting assertions in tests

```
// ❌ Bad
test('should open the page', async ({ page }) => {
  await page.goto('/dashboard');
  // assuming that if the goto mehtod doesn't throw, the page is open, but we don't actually check anything
});
```

```
// ✅ Better
test('should open dashboard page and verify text', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page.getByText('Dashboard')).toBeVisible();
});
```

A test without assertions is not really a test, it’s just a script that performs actions without verifying any outcomes. Always make sure to include assertions in your tests to validate that the application is behaving as expected.

##### 2\. Using one-shot checks instead of web-first assertions

```
// ❌ Bad
expect(await page.getByTestId('status').isVisible()).toBeTruthy();
expect(await page.getByTestId('name').textContent()).toBe('Alice');
expect(page.url()).toMatch(/\/dashboard$/);
expect(await page.locator('li').count()).toBe(5);
expect(await page.getByRole('button', { name: 'Submit' }).isEnabled()).toBe(true);
```

```
// ✅ Better
await expect(page.getByTestId('status')).toBeVisible();
await expect(page.getByTestId('name')).toHaveText('Alice');
await expect(page).toHaveURL(/\/dashboard$/);
await expect(page.locator('li')).toHaveCount(5);
await expect(page.getByRole('button', { name: 'Submit' })).toBeEnabled();
```

Web-first assertions retry until timeout. One-shot checks pass/fail based on timing luck. If you see `isVisible`, `textContent` with a pair of `toBe` assertions in your tests, it’s a red flag that the test might be flaky and should be refactored to use web-first assertions instead.

##### 3\. Using hardcoded timeouts with `waitForTimeout`

```
// ❌ Bad
await page.goto('/dashboard');
await page.waitForTimeout(5000); // Wait for dashboard to load
await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
```

```
// ✅ Better
await page.goto('/dashboard');
await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
```

But the page needs a moment to stabilize! Sure, but `waitForTimeout(5000)` doesn’t actually check whether the page has stabilized. It just waits blindly and hopes. A web-first assertion like `.toBeVisible()` keeps checking until the element is truly there, which is both faster on a quick machine and safer on a slow one.

##### 4\. Relying on `networkidle`

```
// ❌ Bad
await page.goto('/app', { waitUntil: 'networkidle' });
```

```
// ✅ Better
await page.goto('/app');
await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
```

`networkidle` is a brittle signal that can cause flakiness. It waits for no network connections for 500ms, which can happen too early (e.g. if the page has long-polling or WebSocket connections) or too late (e.g. if the page has a slow API call). Instead, wait for a user-visible state that indicates the page is ready, such as a heading, button, or other element that users interact with.

##### 5\. Waiting before actions that already auto-wait

```
// ❌ Bad
await page
  .getByRole('button', { name: 'Submit' })
  .waitFor({ state: 'visible' });
await page.getByRole('button', { name: 'Submit' }).click();
```

```
// ✅ Better
await page.getByRole('button', { name: 'Submit' }).click();
```

Almost all actions (e.g. `click`, `fill`, `check`, and many others) already wait for [actionability](https://playwright.dev/docs/actionability) and will automatically retry until the element is ready, not just visible. It’s not harmful and will not cause flakiness, but it adds unnecessary code. If you find this pattern in your codebase, just remove the extra wait and let Playwright do its job.

##### 6\. Overusing `{ force: true }`

```
// ❌ Bad
await page.getByRole('button', { name: 'Delete' }).click({ force: true });
await page.locator('.email-input').fill('example@example.com', { force: true });
```

```
// ✅ Better
await page.getByRole('button', { name: 'Delete' }).click();
await page.locator('.email-input').fill('example@example.com');
```

If users cannot click it, your test should not force it either. Using `{ force: true }` can hide real issues with the page, such as elements being covered by others, not being visible, or not being enabled. Fix the test flow to match real user behavior — for example, close an overlay before clicking the button behind it — instead of forcing the interaction.

##### 7\. Ordering `waitForResponse` incorrectly

```
// ❌ Bad
await page.waitForResponse((r) => r.url().includes('/api/data'));
await page.getByRole('button', { name: 'Load' }).click();

// also bad — response may arrive before the listener is set up (race condition)
await page.getByRole('button', { name: 'Load' }).click();
await page.waitForResponse((r) => r.url().includes('/api/data'));
```

```
// ✅ Better
// 1. Set up the listener first (no await)
const responsePromise = page.waitForResponse(
  (r) => r.url().includes('/api/data') && r.status() === 200,
);

// 2. Trigger the action second
await page.getByRole('button', { name: 'Load' }).click();

// 3. Await the response third
await responsePromise;
```

I know, I know many people are scared of missing an await, but this is one place where that habit backfires. You need to set up the listener first (no await), trigger the action second, then await the response third.

##### 8\. Writing custom retry loops instead of using `toPass` or `expect.poll`

```
// ❌ Bad
let retries = 5;

while (retries > 0) {
  const state = await page.getByTestId('total').textContent();
  const value = parseInt(state || '0', 10);
  if (value === 100) {
    expect(value).toBe(100);
    break;
  }
  retries--;
  await page.waitForTimeout(1000);
}
```

```
// ✅ Better

// Using `toPass`
await expect(async () => {
  const state = await page.getByTestId('total').textContent();
  const value = parseInt(state || '0', 10);
  expect(value).toBe(100);
}).toPass({ timeout: 30_000, intervals: [500, 1_000] });

// Using `expect.poll`
await expect.poll(async () => {
  const state = await page.getByTestId('total').textContent();
  const value = parseInt(state || '0', 10);

  return value;
}, { timeout: 30_000, intervals: [500, 1_000] }).toBe(100);
```

`toPass` and `expect.poll` are safer and easier to reason about than custom retry loops. They handle timing, retries, and timeouts in a consistent way, and they integrate well with Playwright’s built-in waiting mechanisms. If you see custom retry loops in your tests, consider refactoring them to use `toPass` or `expect.poll` for better reliability and readability.

##### 9\. Forgetting short inner timeouts inside `toPass`

```
// ❌ Bad
await expect(async () => {
  // some actions here
  // ...
  await expect(page.getByTestId('status')).toHaveText('Ready');
}).toPass({ timeout: 30_000 });
```

```
// ✅ Better
await expect(async () => {
  // some actions here
  // ...
  await expect(page.getByTestId('status')).toHaveText('Ready', {
    timeout: 1_000, // Short timeout for the inner assertion
  });
}).toPass({ timeout: 30_000 });
```

When using `toPass`, it’s important to set short timeouts for the inner assertions. Otherwise, if the inner assertion has a long default timeout (e.g. 30 seconds), it can cause the test to wait unnecessarily long before retrying, which can make the test suite slower and less responsive to failures. Setting a short timeout for the inner assertion allows `toPass` to retry more quickly and fail faster when the condition is not met.

##### 10\. Using deprecated APIs and options

```
// ❌ Bad
await Promise.all([\
  page.waitForNavigation(), // Waiting for navigation after clicking a link\
  page.getByRole('link', { name: 'Profile' }).click(),\
]);
```

```
// ✅ Better
await page.getByRole('link', { name: 'Profile' }).click();
await page.waitForURL('**/profile');
```

`waitForNavigation` is deprecated because it can miss navigations triggered by non-click actions (e.g. `window.location` changes) and it doesn’t work well with single-page applications. Use `waitForURL` or web-first assertions instead to wait for the expected state after the action.

##### 11\. Not using `{ exact: true }` for some locators

```
// ❌ Bad
await page.getByRole('button', { name: 'Submit' }).click();
await page.getByText('Submit').click();
```

```
// ✅ Better
await page.getByRole('button', { name: 'Submit', exact: true }).click();
await page.getByText('Submit', { exact: true }).click();
```

Without `{ exact: true }`, locators use substring matching — so `getByText('Submit')` also matches “Submit Order” or “Submitting…”. If a new element with similar text appears on the page, your locator suddenly matches multiple elements and Playwright throws a strict-mode violation. By adding `{ exact: true }`, you ensure the locator matches only the exact text you expect, which prevents surprise failures when the page content evolves.

##### 12\. Using `expect.poll` for simple DOM checks

```
// ❌ Bad
await expect.poll(() => page.getByTestId('counter').textContent()).toBe('10');
```

```
// ✅ Better
await expect(page.getByTestId('counter')).toHaveText('10');
```

`expect.poll` is useful for polling, and yeah, you can still use it for DOM elements, but only when it’s necessary. In most cases, web-first assertions like `toHaveText`, `toBeVisible`, etc. are more concise and reliable. If you see `expect.poll` being used to check DOM state that can be done with web-first assertions, consider refactoring it to use web-first assertions instead.

##### 13\. Using `waitForFunction` for simple UI assertions

```
// ❌ Bad
await page.waitForFunction(
  () => document.querySelector('.status')?.textContent === 'Ready',
);
```

```
// ✅ Better
await expect(page.locator('.status')).toHaveText('Ready');
```

`waitForFunction` is a powerful tool for waiting on complex conditions, but it’s often overused for simple UI assertions that can be expressed with web-first assertions. If you see `waitForFunction` in your tests, check if it can be refactored to use `expect` and locators instead for better readability and reliability.

##### 14\. Preferring `.not` negative assertions over positive ones

```
// ❌ Bad
await expect(page.getByRole('button', { name: 'Submit' })).not.toBeVisible();
```

```
// ✅ Better
await expect(page.getByRole('button', { name: 'Submit' })).toBeHidden();
```

Using `.not` can make tests less readable and can lead to confusion. If there is a positive assertion available (like `toBeHidden`), it’s usually clearer to use it instead of negating a positive assertion.

##### 15\. Ignoring `eslint-plugin-playwright`

If you don’t have `eslint-plugin-playwright` set up in your project, you’re missing out on a powerful tool that can catch many of these anti-patterns before they even make it into your codebase. This plugin provides linting rules specifically designed for Playwright tests, helping you enforce best practices and avoid common mistakes.

It’s super easy to include it in your project:

```
npm install -D eslint-plugin-playwright
```

```
// eslint.config.mjs
import playwright from "eslint-plugin-playwright";

export default [\
  {\
    ...playwright.configs["flat/recommended"],\
    files: ["tests/**"],\
  },\
];
```

Many of the mistakes mentioned in this article can be automatically detected and prevented with the right linting rules. If you find that your test suite has some of these anti-patterns, consider adding `eslint-plugin-playwright` to catch them in the future and maintain a healthier codebase.

##### 16\. Returning new page objects from action methods

```
// ❌ Bad
async login(user: string, pass: string): Promise<DashboardPage> {
  await this.page.getByLabel('Username').fill(user);
  await this.page.getByLabel('Password').fill(pass);
  await this.page.getByRole('button', { name: 'Sign in' }).click();
  return new DashboardPage(this.page);
}
```

```
// ✅ Better
async login(user: string, pass: string): Promise<void> {
  await this.page.getByLabel('Username').fill(user);
  await this.page.getByLabel('Password').fill(pass);
  await this.page.getByRole('button', { name: 'Sign in' }).click();
}
```

Well, it’s not a mistake tbh, but returning new page objects from action methods can lead to unnecessary complexity and maintenance overhead. It can create tight coupling between page objects and make it harder to reuse them across different tests. Instead, let the test itself decide which page object to use after the action is performed, based on the expected state of the application.

##### 17\. Making tests dependent on each other with `test.describe.serial`

```
// ❌ Bad
test.describe.serial('checkout flow', () => {
  test('step 1: add item to cart', async ({ page }) => {
    await page.goto('/products');
    await page.getByRole('button', { name: 'Add to cart' }).click();
  });

  test('step 2: go to checkout', async ({ page }) => {
    // Depends on step 1 having run — if step 1 fails, this fails too
    await page.goto('/cart');
    await page.getByRole('button', { name: 'Checkout' }).click();
  });

  test('step 3: confirm order', async ({ page }) => {
    // Depends on step 2 — the whole chain is fragile
    await page.getByRole('button', { name: 'Confirm' }).click();
    await expect(page.getByText('Order placed')).toBeVisible();
  });
});
```

```
// ✅ Better
test('complete checkout flow', async ({ page }) => {
  test.step('add item to cart', async () => {
    await page.goto('/products');
    await page.getByRole('button', { name: 'Add to cart' }).click();
  });

  test.step('go to checkout', async () => {
    await page.goto('/cart');
    await page.getByRole('button', { name: 'Checkout' }).click();
  });

  test.step('confirm order', async () => {
    await page.getByRole('button', { name: 'Confirm' }).click();
    await expect(page.getByText('Order placed')).toBeVisible();
  });
});
```

`test.describe.serial` forces tests to run in order and makes every test depend on the previous one succeeding. If test 1 fails, tests 2 and 3 are skipped so you lose feedback on whether those parts of the app actually work. Each test should be independent: set up its own state and not rely on side effects from other tests. If you need to test a multi-step flow, put the whole flow in a single test and use `.step`.

##### Final thoughts

Good Playwright tests are usually simple.
Assert what users see, trust built-in waiting, and avoid custom timing hacks.
Do that consistently, and your tests get faster, more stable, and easier to maintain. Thank you for reading, and happy testing!

* * *

- [Playwright](https://elaichenkov.github.io/tags/playwright/)
- [Testing](https://elaichenkov.github.io/tags/testing/)
- [Automation](https://elaichenkov.github.io/tags/automation/)

* * *

[Previous Post\\
\\
TIL: git worktree lets you work on multiple branches at once](https://elaichenkov.github.io/posts/til-git-worktree)

### 9. Vitaliy Haradkou — Modern TypeScript Decorators: TC39 Stage 3

- Source: https://blog-vitaliharadkous-projects.vercel.app/blog/20-typescript-decorators
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Modern TypeScript Decorators: TC39 Stage 3 - No More Reflect-Metadata!](https://blog-vitaliharadkous-projects.vercel.app/_next/image?url=%2Fblog%2F20-typescript-decorators%2Fhero.webp&w=3840&q=75)

#### Modern TypeScript Decorators: TC39 Stage 3 - No More Reflect-Metadata!

##### Introduction

TypeScript decorators have evolved significantly. The new TC39 Stage 3 decorators are now standardized, type-safe, and **don't require the `reflect-metadata` package**. If you're still using legacy decorators with `experimentalDecorators: true`, it's time to upgrade.

In this post, I'll show you real-world examples from a Playwright testing framework that uses modern decorators for class-based test organization, demonstrating why the new syntax is superior.

##### Why Modern Decorators Are Better

###### 1\. ✅ No `reflect-metadata` Dependency

**Legacy decorators:**

```
import "reflect-metadata"; // ❌ External dependency required

function MyDecorator(target: any, propertyKey: string) {
  Reflect.defineMetadata("custom", value, target, propertyKey);
  const meta = Reflect.getMetadata("custom", target, propertyKey);
}
```

**Modern decorators:**

```
// ✅ No imports needed - built into the language!

function MyDecorator(target: any, context: DecoratorContext) {
  context.metadata.custom = value; // Direct access to metadata
}
```

###### 2\. ✅ Better Type Inference

Modern decorators have **first-class TypeScript support** with proper generic constraints:

```
// ✅ Type-safe: Only works on async functions
function log<
  const Name extends string,
  const T extends (this: any, ...args: any[]) => Promise<unknown> = (
    ...args: Args
  ) => Promise<unknown>,
>() {
  return function (target: T, context: DecoratorContext) {
    if (context.kind !== "method") {
      throw new Error("step decorator can only be used on methods");
    }
    // TypeScript ensures T is async!
    return async function (...args: Args) {
      /* ... */
    };
  };
}

class Example {
  @log("my async step")
  async myAsyncMethod() {} // ✅ Works

  @log("sync step")
  syncMethod() {} // ❌ TypeScript error: must be async!
}
```

###### 3\. ✅ TC39 Stage 3 - Standardized JavaScript

Modern decorators are **Stage 3** in the TC39 proposal process, meaning they're nearly finalized and will be part of JavaScript itself. Legacy decorators were never standardized.

###### 4\. ✅ Cleaner Code with Context

The `context` object provides structured access to metadata, eliminating the mess of multiple reflection APIs.

* * *

##### Understanding Decorator Context

Every modern decorator receives a `context` object with powerful features:

```
interface DecoratorContext {
  kind: "class" | "method" | "getter" | "setter" | "field" | "accessor";
  name: string | symbol;
  access?: { get?(): any; set?(value: any): void };
  private?: boolean;
  static?: boolean;
  addInitializer(initializer: () => void): void;
  metadata: Record<PropertyKey, unknown>; // 🔥 The magic!
}
```

###### The Power of `context.metadata`

`context.metadata` is a **shared object** across all decorators in a class hierarchy. This enables decorators to communicate:

```
// Property decorator stores parameter info
function param(name: string) {
  return function (target: any, context: ClassFieldDecoratorContext) {
    context.metadata.params = context.metadata.params || {};
    context.metadata.params[name] = {
      name: name,
      originalName: context.name,
    };
  };
}

// Method decorator reads parameter info
function step(template: string) {
  return function (target: any, context: ClassMethodDecoratorContext) {
    const params = context.metadata.params; // Access shared metadata!
    // Use params to transform template...
  };
}

class Example {
  @param("username")
  user = "john_doe";

  @step("Login as $username") // Uses metadata from @param!
  async login() {}
}
```

* * *

##### Real-World Example 1: Class Decorator

Let's build a `@logInstance` decorator that sends log when the instance is created.

###### Class Decorator Goal

```
@logInstance("User")
class User {
  /** implementation */
}

new User(); // calls [User]. Initialized
```

###### Implementation

```
function logInstance(prefix?: string) {
  return function <T extends new (...args: any[]) => any>(
    target: T,
    context: ClassDecoratorContext<T>,
  ) {
    // Access metadata populated by @test decorators
    const metadata = context.metadata as any;
    const describeName = prefix ?? context.name.toString();

    context.addInitializer(function () {
      console.log(`[${describeName}]. Initialized`);
    });

    // advanced: you can return Proxy
    return new Proxy(target, {
      construct(target, args, newTarget) {
        const instance = Reflect.construct(target, args, newTarget);
        console.log(`[${describeName}]. Created`);
        return instance;
      },
    });
  };
}
```

###### Key Features

1. **Type safety**: `T extends new (...args: any[]) => any` ensures it's a class
2. **No reflection package needed**: Built-in metadata support, more native way

* * *

##### Real-World Example 2: Method Decorator with Type Constraints

The `@logAsync` decorator adds logs only for function that returns a Promise

###### Method Decorator Goal

```
class CheckoutFlow {
  @logAsync("Add product to cart")
  async addToCart(productName: string) {
    /* ... */
  }

  @logAsync("Apply discount code")
  async applyDiscount(code: string) {
    /* ... */
  }
}
```

###### Implementation

```
function logAsync<
  // Constraint: Only async functions!
  const T extends (...args: any[]) => Promise<unknown> = (
    ...args: any[]
  ) => Promise<unknown>,
>(message?: string) {
  return function (target: T, context: ClassMethodDecoratorContext) {
    // 🛡️ Runtime validation
    if (context.kind !== "method") {
      throw new Error("logAsync decorator can only be used on methods");
    }

    if (context.static) {
      throw new Error(
        "logAsync decorator can only be used on instance methods",
      );
    }
    if (context.private) {
      // declared with private field (e.g. #myMethod)
      throw new Error("logAsync decorator can only be used on public methods");
    }

    // Return replacement method
    return async function (this: any, ...args: Args): Promise<any> {
      console.log(`Starting ${message}`);
      const returnValue = await Reflect.apply(target, this, args);
      console.log(`Finishing ${message}`);
    };
  };
}
```

###### Advanced: Named Parameters with Metadata

Combine `@param` property decorator with `@step` method decorator:

```
class UserActions {
  @param("user")
  username = "john_doe";

  @step("Login as $user") // Named parameter!
  async login() {
    // Step displays: "Login as john_doe"
  }
}
```

**How it works:**

```
// 1. Property decorator stores metadata
function param(name: string) {
  return function (target: any, context: ClassFieldDecoratorContext) {
    context.addInitializer(function () {
      context.metadata.params = context.metadata.params || {};
      context.metadata.params[name] = {
        name: name,
        originalName: context.name,
      };
    });
  };
}

// 2. Method decorator reads metadata
function step<
  // only async methods allows here, function should return a Promise
  T extends (...args: any[]) => Promise<any>,
>(template: string) {
  return function (target: any, context: ClassMethodDecoratorContext<T>) {
    return async function (this: any, ...args: any[]) {
      // Access params from context.metadata
      const params = context.metadata?.params || {};

      let transformedName = template;

      // Replace named parameters like $user
      for (const [paramName, paramInfo] of Object.entries(params)) {
        const value = this[paramInfo.originalName];
        transformedName = transformedName.replace(
          new RegExp(`\\$${paramName}`, "g"),
          String(value),
        );
      }

      // automatically wrap the step
      return test.step(transformedName, async () =>
        Reflect.apply(target, this, args),
      );
    };
  };
}
```

* * *

##### Real-World Example 3: Property Decorator

The `@param` decorator marks class properties as named parameters for test step templates.

###### Property Decorator Goal

```
class ApiTests {
  @param("endpoint")
  apiUrl = "/api/users";

  @param("method")
  httpMethod = "GET";

  @step("$method request to $endpoint")
  async makeRequest() {
    // Step displays: "GET request to /api/users"
  }
}
```

###### Implementation

```
function param<const Name extends string, const TT, const V>(name?: Name) {
  return function (_: any, context: ClassFieldDecoratorContext<TT, V>) {
    const paramName = name ?? context.name.toString();

    // Validation
    if (context.static) {
      throw new Error("Static properties cannot be decorated with @param");
    }
    if (context.private) {
      throw new Error("Private properties cannot be decorated with @param");
    }

    // Use addInitializer to run after property is defined
    context.addInitializer(function () {
      // Store parameter info in metadata
      if (!context.metadata.params) {
        context.metadata.params = {};
      }

      context.metadata.params[paramName] = {
        name: paramName,
        originalName: context.name,
      };
    });
  };
}
```

##### Context Properties Deep Dive

###### `context.kind`

Identifies the decorator target type:

```
function universal(target: any, context: DecoratorContext) {
  switch (context.kind) {
    case "class":
      console.log("Decorating a class");
      break;
    case "method":
      console.log("Decorating a method");
      break;
    case "field":
      console.log("Decorating a property");
      break;
    // ... other kinds
  }
}

@universal // decorating a class
class Universal {
  @universal // decorating a method
  someMethod() {}

  @universal // decorating a method (private)
  #someMethod() {}

  @universal // decorating a method (static)
  static someMethod() {}

  @universal // decorating a property
  someProperty: string;

  @universal // decorating a property (private)
  #someProperty: string;

  @universal // decorating a property (static)
  static someProperty: string;
}
```

###### `context.name`

The name of the decorated element:

```
function logName(target: any, context: DecoratorContext) {
  console.log(`Decorating: ${String(context.name)}`);
}

class Example {
  @logName
  myMethod() {} // Logs: "Decorating: myMethod"
}
```

**NOTE:** `context.name` can be a string or a symbol, be noticed!

###### `context.access`

Provides getters/setters for fields:

```
function logged(target: any, context: ClassFieldDecoratorContext) {
  return function (this: any, initialValue: any) {
    const value = initialValue;

    // Use context.access for reading/writing
    return {
      get() {
        console.log(`Reading: ${String(context.name)}`);
        return context.access.get?.call(this) ?? value;
      },
      set(newValue: any) {
        console.log(`Writing: ${String(context.name)} = ${newValue}`);
        context.access.set?.call(this, newValue);
      },
    };
  };
}
```

###### `context.addInitializer`

Run code after the target is initialized:

```
function register(target: any, context: ClassDecoratorContext) {
  context.addInitializer(function () {
    console.log(`Instance of ${target.name} created`);
    // 'this' is the instance
  });
}

@register
class Example {}

const instance = new Example(); // Logs: "Instance of Example created"
```

###### `context.metadata`

The shared metadata object - the most powerful feature:

```
// Decorator 1: Store data
function storeVersion(version: string) {
  return function (target: any, context: ClassDecoratorContext) {
    context.metadata.version = version;
  };
}

// Decorator 2: Read data
function logVersion(target: any, context: ClassDecoratorContext) {
  context.addInitializer(function () {
    console.log(`Version: ${context.metadata.version}`);
  });
}

@storeVersion("1.0.0")
@logVersion
class Example {} // Logs: "Version: 1.0.0"
```

* * *

##### Inheritance and Metadata

Metadata is **inherited** through the prototype chain, but you need to access it correctly:

###### ❌ Wrong: Using `Symbol.metadata` on non-decorated classes

```
class BaseTest {
  @param("user")
  username = "base";
}

class ChildTest extends BaseTest {}

// ❌ This doesn't work - ChildTest has no metadata
const metadata = ChildTest[Symbol.metadata]; // undefined!
```

###### ✅ Correct: Walk the prototype chain

```
function describe(name: string) {
  return function (target: any, context: ClassDecoratorContext) {
    // Collect metadata from current class
    const metadata = context.metadata;
    const params = metadata?.params || {};

    // Walk up prototype chain for inherited metadata
    let currentProto = target.prototype;
    while (currentProto && currentProto !== Object.prototype) {
      const protoConstructor = currentProto.constructor;
      const protoMetadata = protoConstructor[Symbol.metadata];

      if (protoMetadata?.params) {
        // Merge inherited params (child takes precedence)
        Object.assign(params, protoMetadata.params);
      }

      currentProto = Object.getPrototypeOf(currentProto);
    }
  };
}
```

* * *

##### Type Inference Magic

Modern decorators enable advanced type inference:

###### Example 1: Infer Arguments from Template String

```
type InferArgsFromTemplateString<
  Name extends string,
  Args extends readonly unknown[] = [],
> = Name extends `${string}$${infer First extends number}${infer Rest}`
  ? InferArgsFromTemplateString<Rest, [...Args, argument: unknown]>
  : Args;

// Usage:
type Args1 = InferArgsFromTemplateString<"Add $0 to $1">;
// Result: [unknown, unknown]

type Args2 = InferArgsFromTemplateString<"No params">;
// Result: []
```

###### Example 2: Ensure Correct Argument Count

```
function test<
  const Name extends string,
  const Args extends readonly unknown[] = InferArgsFromTemplateString<Name>,
  const T extends (...args: Args) => any = (...args: Args) => any,
>(name: Name) {
  return function (target: T, context: DecoratorContext) {
    // TypeScript ensures the function signature matches the template!
  };
}

class Tests {
  @test("Test with $0 and $1")
  testMethod(a: string, b: number) {} // ✅ Correct: 2 params

  @test("Test with $0 and $1")
  wrongMethod(a: string) {} // ❌ TypeScript error: needs 2 params!
}
```

###### Example 3: Prevent Invalid Names

```
type NoSpaces<T extends string> = T extends `${string} ${string}`
  ? ["Cannot use spaces in parameter name", never]
  : T;

function param<const Name extends string>(name: NoSpaces<Name>) {
  // ...
}

class Example {
  @param("userName") // ✅ OK
  user = "test";

  @param("user name") // ❌ TypeScript error!
  invalidUser = "test";
}
```

* * *

##### Migration Guide

###### Legacy Decorators (experimentalDecorators)

```
// tsconfig.json
{
  "compilerOptions": {
    "experimentalDecorators": true, // ❌ Old way
    "emitDecoratorMetadata": true
  }
}

// Code
import "reflect-metadata";

function Logger(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function(...args: any[]) {
    console.log(`Calling ${propertyKey}`);
    return originalMethod.apply(this, args);
  };
}

class Example {
  @Logger
  myMethod() { }
}
```

###### Modern Decorators (TC39 Stage 3)

```
// tsconfig.json
{
  "compilerOptions": {
    // ✅ No special flags needed! Just use latest TypeScript
  }
}

// Code - no imports needed!
function Logger(target: any, context: ClassMethodDecoratorContext) {
  const methodName = String(context.name);

  return function(this: any, ...args: any[]) {
    console.log(`Calling ${methodName}`);
    return Reflect.apply(target, this, args);
  };
}

class Example {
  @Logger
  myMethod() { }
}
```

* * *

##### Best Practices

###### 1\. Always Validate `context.kind`

```
function myDecorator(target: any, context: DecoratorContext) {
  if (context.kind !== "method") {
    throw new Error("Only works on methods");
  }
  // ...
}
```

###### 2\. Use `context.addInitializer` for Setup

```
function setup(target: any, context: ClassDecoratorContext) {
  context.addInitializer(function () {
    // This runs after construction
    console.log("Instance created:", this);
  });
}
```

###### 3\. Leverage TypeScript Constraints

```
// Only allow on async methods
function asyncOnly<T extends (...args: any[]) => Promise<any>>(
  target: T,
  context: DecoratorContext,
) {
  // TypeScript enforces T is async
}
```

###### 4\. Document Metadata Contracts

```
interface MyMetadata {
  version: string;
  tests: Array<{ name: string; methodName: string }>;
  params: Record<string, { name: string; formatter?: (v: any) => string }>;
}

function myDecorator(target: any, context: DecoratorContext) {
  const metadata = context.metadata as MyMetadata;
  // Now you have type safety!
}
```

* * *

##### Conclusion

Modern TypeScript decorators (TC39 Stage 3) are a massive improvement:

✅ **No `reflect-metadata` package needed**

✅ **Better type inference and safety**

✅ **Standardized JavaScript feature**

✅ **Cleaner code with `context` object**

✅ **Powerful `context.metadata` for decorator communication**

The examples in this post are from a real Playwright testing framework that uses these decorators in production. The code is cleaner, more type-safe, and easier to maintain than legacy decorator implementations.

If you're still using `experimentalDecorators: true`, now is the time to migrate. The future of JavaScript decorators is here, and it's better than ever.

* * *

##### Resources

- [TC39 Decorator Proposal](https://github.com/tc39/proposal-decorators)
- [TypeScript 5.0 Decorators](https://devblogs.microsoft.com/typescript/announcing-typescript-5-0/#decorators)
- [Playwright Testing Framework](https://playwright.dev/)
- [Example Code Repository](https://github.com/playwright-labs/fixture-generic)

* * *

**Want to see more?** The full implementation with `@describe`, `@test`, `@test.each`, `@beforeEach`, `@afterEach`, `@param`, and `@step` decorators is open source. Check out the repository for complete working examples!

### 14. Currents.dev — Debug Playwright tests in CI

- Source: https://currents.dev/posts/how-to-debug-playwright-tests-in-ci
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Dumebi Okolo](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)

Dumebi Okolo

•Jan 27, 2026•

#### How To Debug Playwright Tests in CI: The Complete Guide

Struggling with Playwright tests failing only in CI? Here's how to debug them with tracing, verbose logs, and consistent environments to find the root cause.

![How To Debug Playwright Tests in CI: The Complete Guide](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-debug-playwright-tests-in-ci%2Fposter.png&w=3840&q=100)

Continuous Integration (CI) environments often expose issues that never appear during local runs. A suite of Playwright tests may pass flawlessly on a workstation, yet surface failures once pushed into a shared pipeline.

Differences in execution speed, browser behavior, environment variables, and CI resource limits (like CPU throttling, shared runners, and constrained memory) create gaps that are difficult to trace. With limited visibility and high parallelism, pipelines can make small timing issues look unpredictable. Debugging in this environment requires clarity, structure, and the right combination of tooling.

This guide explores the conditions that cause Playwright tests to fail in CI pipelines, along with practical debugging techniques that help restore stability.

##### Understanding CI Flakiness Types

Most CI failures fall into a small number of categories, and identifying which category you’re dealing with is often more important than the specific error message.

- UI-driven flakiness: timing issues, missing waits, animations, or dynamic rendering differences.
- Environment-driven flakiness: slower CPUs, constrained memory, containerization, or network latency in CI.
- Data and parallelism-driven flakiness: shared backend data, reused accounts, or unsafe storageState usage across workers.
- Test-suite-driven flakiness: leaked state, shared fixtures, order dependencies, or architectural issues within the test code itself.

Effective CI debugging starts by determining which category a failure belongs to, since each requires a different remediation strategy.

![](https://currents.dev/img/posts/how-to-debug-playwright-tests-in-ci/flowchart.png)

##### Why Playwright Test Runs Behave Differently in CI

Automated tests frequently destabilize in CI pipelines due to the resource limitations of shared runners, where restricted CPU and memory alter browser timing and event scheduling.
Unlike local environments that typically benefit from cached assets and existing sessions, CI typically runs with a "cold" browser launch that ruthlessly exposes missing waits and fragile timing assumptions previously hidden on warmed machines. This complexity is exacerbated by headless execution and environmental drift, which remove the visual cues necessary for straightforward diagnosis.
Beyond individual browser performance, parallelism amplifies instability. While developers often test sequentially, CI pipelines run multiple workers simultaneously, leading to cross-test collisions where workers race for session tokens or corrupt shared backend data.
Finally, the pipeline's network environment acts as a rigorous stress test: higher latency reveals unhandled API delays, while stricter firewalls may unexpectedly block third-party scripts, breaking UI dependencies that function flawlessly during local development.

Let’s get into how to debug Playwright tests in CI.

##### Improving Visibility in CI Test Failures

A frequent case involves a checkout test that stops midway in CI without clear feedback. For example, the test might click **"Proceed to Payment"** but the pipeline logs simply show that the action executed, nothing more. Locally, the next page loads instantly, but in CI the app may still be rendering, waiting on a slow API response, or recalculating layout under reduced CPU. With no trace or visual output enabled, the run appears to freeze, giving you no indication of whether the page navigated, the button fired, or the element was never visible in the first place.

The output log shows the action where it stopped, but nothing indicates which condition prevented the test from advancing. The root problem lies in insufficient visibility. While Playwright offers rich tracing and logging tools, they must be explicitly enabled during test execution for pipelines to collect the necessary details.

The immediate improvement comes from enabling trace recording. Traces create a visual timeline of the test’s progress, including screenshots, events, and console logs. Once enabled, pipelines can store the generated trace file for later inspection. Developers reviewing the failure can load that file into the trace viewer to see the exact moment the issue occurred.

A typical configuration:

```typescript
// playwright.config.ts
import { defineConfig } from "@playwright/test";

export default defineConfig({
  use: {
    trace: "on-first-retry",
    video: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
```

Running the code below ensures that traces and videos for failing runs appear in the test results directory:

```bash
npx playwright test
```

Opening them in the trace viewer helps clarify which actions completed successfully and which failed due to missing locators, slow load times, or unexpected UI changes.
Keep in mind that retries can hide systemic flakiness rather than resolve it. Retries are most useful for diagnosing failures (for example, capturing traces on first retry), not for stabilizing inherently unstable tests. If a test passes on retry but fails intermittently, the root cause is around missing synchronization, shared state, or nondeterministic data and not insufficient time.

Playwright traces are powerful but heavy (large files that slow down pipelines and make manual artifact handling painful at scale). [Currents](http://currents.dev/) centralizes traces and videos across CI runs, removing the need to download artifacts locally and making it easier to compare failures over time without incurring repeated CI overhead.

##### Using Playwright Verbose Logging to Understand Hidden Conditions

A test fails instantly in CI with a generic "locator not found" message, but provides no indication of what led to the failure. The underlying issue is that the pipeline hides low-level debugging detail by default. The debugging process requires better output.

Activating verbose output reveals API activity, locator resolutions, and execution reasoning inside the Playwright test runner. Logging lines also show retry attempts and timing behavior, which helps identify whether a locator resolved correctly but appeared too late.

This logging mode can be activated through environment variables:

```bash
DEBUG=pw:api npx playwright test
```

exposing logging for the test flow. Output also helps when analyzing API logs, making it easier to follow each network action. Keep in mind that verbose logging doesn’t follow network action by default; additional setup is required.

`DEBUG=pw:api` is useful for understanding why Playwright made a decision (timeouts, waits, locator resolution).

`DEBUG=pw:browser*` exposes low-level DevTools protocol traffic, which is mostly useful for framework-level debugging or rare edge cases (frames, navigations, cross-origin issues).
For CI debugging, Playwright traces and HAR files are almost always more actionable than browser protocol logs.

Verbose logs provide depth into Playwright’s execution flow, but they do not expose CI-specific constraints such as resource contention, containerization, or shared runner behavior. To surface CI-only failures, you need to replicate those conditions directly.

##### Simulating CI Conditions Locally

A common obstacle arises when a test passes locally but fails repeatedly in CI, even when tracing is enabled. The reason often lies in execution differences that only appear under CI constraints. To reproduce these conditions, the environment needs to run locally with similar settings. This includes using full headless mode, limiting CPU, or mimicking environment variables used by pipelines.

Teams can match CI behavior by running:

```bash
#### Normal local run
npx playwright test

#### Simulate CI behavior locally
CI=true npx playwright test
```

Setting `CI=true` alone does **not** reproduce real CI conditions. It only affects behavior if your test harness or scripts explicitly depend on this variable. Accurate CI reproduction requires running tests in the same container image, with matching browser binaries, OS-level dependencies, and comparable CPU and memory constraints.

This reveals timing drifts, subtle animations, or layout adjustments that otherwise remain hidden. Running tests under constrained CPU also exposes flaky steps that rely on speed. Local simulation should mirror how tests are executed in CI, including environment variables, test commands, and configuration. Keeping configuration consistent across environments reduces the complexity of reproducing failures. However, environment alignment only gets you so far. Intermittent or timing-sensitive failures require deeper, interactive debugging.

##### Deep Debugging with the Playwright Inspector Tool

The Playwright Inspector is a local debugging tool and cannot be used interactively inside CI (reproduce locally in a CI-matching container and run **headed + slowMo + video** to get CI-like visual debugging after pulling the same commit/env). It becomes useful only after a CI failure has been reproduced locally under comparable conditions.
A scenario involving a booking form might pass most of the time, but fail intermittently in the pipeline. When inspecting the failure, the locator appears correct, yet the test still misses the expected element. The deeper issue typically relates to timing or animation state. Inspecting the test step-by-step helps determine the exact cause.

Launching tests in inspector mode reveals the internal flow:

```bash
npx playwright test --debug
```

This activates debug mode and opens the Playwright Inspector, which pauses execution at the beginning of the test and before each step, so you can walk through the flow interactively and inspect the DOM, selectors, and state as the test progresses. The interface highlights selectors, DOM relationships, and execution steps. Running tests in this mode allows the suite to pause at key moments, giving insight into rendering behavior or asynchronous timing issues.

Using a GUI (Graphical User Interface) tool or the integrated VS Code extension for Playwright further assists by visualizing each state transition. Tools like the Playwright Inspector let you step through actions interactively, inspect the DOM at each pause, confirm whether locators actually match what you expect, and observe timing behavior that is usually invisible in CI. This makes it much easier to spot missing waits, unstable selectors, or dynamic UI changes that only manifest under CI constraints.

After gaining visibility into the test flow, the focus shifts to reducing iteration cost during debugging.

##### Re-Running Isolated Scenarios for Faster Iteration

In the situation where only one flow in a larger suite becomes unstable in CI, running the entire suite repeatedly slows down the investigation and consumes pipeline resources. The key is to isolate the failing section and target it directly.

Playwright supports narrowing runs to a specific test or a single test file. This allows a tighter focus and faster feedback.

For example:

```bash
npx playwright test tests/checkout.spec.ts --project=chromium
```

To isolate a behavior or pattern:

```bash
npx playwright test --grep "checkout"
```

This approach keeps pipeline runs efficient and avoids re-running unaffected sections. It helps uncover patterns in test failures, especially when working across different browsers or configurations.

Playwright also supports test annotations and tags, which allow teams to classify and selectively execute tests without restructuring files. Tags are especially useful in CI pipelines, where you may want to isolate flaky tests, slow flows, or CI-only diagnostics.

For example:

```typescript
test("checkout flow", { tag: "@checkout" }, async ({ page }) => {
  // test logic
});
```

CI runs can then target specific subsets:

```bash
npx playwright test --grep @checkout
```

Tags reduce iteration time during CI debugging and make it easier to rerun only the tests relevant to a failure without modifying the suite structure.

##### Debugging Browser/Config Mismatches Between Local and CI

A common scenario involves a navigation sequence working in WebKit but failing in Chromium, or vice versa. These discrepancies often stem from deeper differences in browser engines — such as event sequencing, focus management, timing of layout recalculations, or even browser-specific quirks and bugs that only surface under load.

First, verify you’re running the same `--project` locally as CI (and the same Playwright/browser versions), then compare engines only if CI runs multiple browsers.

CI environments amplify these differences because cold starts, slower CPU, and parallelism make timing-sensitive behavior more pronounced.

Testing across multiple browsers makes these issues easier to diagnose:

```bash
npx playwright test --project=webkit
```

Running the same flow across engines exposes subtle mismatches in behavior that may not appear in your primary browser. Understanding these distinctions helps teams adopt more resilient selector strategies and state-based waits that work consistently across all browser channels.
CI-only browser discrepancies are often caused by mismatched Playwright or browser versions rather than true engine differences, making version pinning and consistent browser installation critical.

##### Make UI Interactions CI-Resilient

**Actions + assertions over manual waits**

Many modern applications use dynamic content. When CI fails to recognize newly injected components, the issue often comes from delayed visibility or elements shifting during transitions. Instead of relying on manual wait calls, Playwright encourages an _actions + assertions_ pattern, where both behaviors include built-in auto-waiting.

For example:

```typescript
await page.getByRole("button", { name: "Continue" }).click();
await expect(
  page.getByRole("heading", { name: "Payment Details" }),
).toBeVisible();
```

Here, the click will automatically wait until the button is actionable, and the assertion will automatically wait until the next screen is visible. This pattern handles UI timing differences more reliably than fixed waits or manual `waitFor()` calls, and it adapts naturally to slower CI environments.

**Semantic selectors over CSS chains**.
Many CI-only failures come from brittle selectors rather than timing. Playwright encourages role-based and semantic selectors instead of deep CSS paths. Prefer `getByRole`, `getByTestId`, `getByLabel`, or `getByPlaceholder` over brittle chains like `div > span:nth-child(2)`.

For example:

```typescript
// fragile
await page.click("button.btn-primary");

// more resilient
await page.getByRole("button", { name: "Continue" }).click();
```

Strong selectors make tests far less sensitive to layout changes, minor style refactors, and rendering differences in CI. Semantic selectors such as `getByRole`, `getByLabel`, and `getByTestId`remain stable across browsers, environments, and UI variations, which makes them significantly more reliable than brittle CSS chains when running under CI constraints.

##### Advanced Techniques For Debugging Tests

Some CI failures remain difficult to diagnose even after analyzing traces, verbose output, and browser differences.

These failures often arise from deeper issues within the application stack, UI framework behavior, or resource patterns that require more specialized techniques. Most issues revealed by CI, including timing delays, brittle selectors, unhandled dynamic content, or unstable network dependencies, surface cleanly once you refine waits, stabilize selectors, and stub external APIs.

But when a test still fails unpredictably after all of that, the problem often isn’t the UI or the network. It’s the test suite itself. These deeper failures usually point to hidden interactions between tests, issues that only appear when tests run together instead of individually. When these tools aren’t enough, here’s what to look at:

###### Using Isolation Diagnostics to Detect State Leaks

A recurring scenario involves tests passing individually but failing when executed alongside others. The failure disappears when tests run in isolation but reappears when the suite runs as a whole.

This usually indicates state leakage. Even small amounts of shared state between tests (cookies, localStorage, global fixtures, or in-memory variables) can create cascading failures. Leakage becomes especially visible in CI, where the environment resets between jobs but not between tests within a single job.

A misconfigured fixture or shared object can persist across test boundaries, causing a failure only after preceding tests modify a stateful object.

Verify that your test setup has not overridden Playwright Test’s default isolation behavior. By default, Playwright creates a fresh browser context per test; CI-only state leaks usually come from shared fixtures, reused pages, global setup artifacts, or backend-side shared data.

Ensuring isolation means validating fixture boundaries, avoiding shared mutable objects, and preventing backend test data collisions, rather than manually creating contexts inside tests.

State leaks aren’t limited to the browser layer; backend and database resources can also drift between tests. Shared user accounts, reused test data, or parallel workers modifying the same records often produce CI-only failures because concurrency amplifies the interference. If one test updates a profile, toggles a feature flag, or clears a cart, another test may unknowingly inherit that mutated state.

###### Revealing Hidden Race Conditions Using Playwright Event Logging

Some failures occur when the UI fires multiple events in quick succession, creating race conditions that remain invisible during local runs. CI timing differences often magnify these races. A component may attach handlers in a slightly different order, or a loading indicator might disappear earlier than expected.

Adding event listeners clarifies these issues:

```typescript
page.on("console", (msg) => console.log("CONSOLE:", msg.text()));
page.on("pageerror", (err) => console.error("PAGE ERROR:", err));
page.on("requestfailed", (req) => console.error("REQUEST FAILED:", req.url()));

page.on("frameattached", (frame) => console.log("FRAME ATTACHED", frame.url()));
page.on("framenavigated", (frame) =>
  console.log("FRAME NAVIGATED", frame.url()),
);
page.on("load", () => console.log("PAGE LOADED"));
```

These logs show whether UI events fire too early, scripts crash silently, or network dependencies fail unexpectedly. Seeing the event timeline helps pinpoint the exact moment where UI timing diverges.

###### Stabilizing Complex UI State with Custom Wait Conditions

Built-in Playwright waiters work well for most scenarios, but some applications use complex state transitions that require more explicit checks. When running in CI, these transitions may complete more slowly, causing elements to appear in stages.

Custom waits align the test flow with actual application conditions:

```typescript
await page.waitForFunction(() => {
  const cart = document.querySelector("#cart");
  return cart && cart.children.length > 0;
});
```

While useful in cases involving multi-stage loaders or complex asynchronous hydration, these waits should remain **the exception, not the default**.

Overusing `waitForFunction` can make tests brittle, harder to debug, and more sensitive to timing variability — especially in CI. Prefer built-in auto-waits and assertions whenever possible, and reserve custom waits only for scenarios where the UI exposes no reliable state signal.

###### Using Consistent Viewports and Device Scale Factors

Failures sometimes arise because CI uses different screen resolutions or DPI settings. UI also tends to _look different in CI_ because pipelines often run with lower resolutions, unusual aspect ratios, or different device scale factors. These differences can shift layouts enough to break selectors that pass locally.

Defining a consistent viewport ensures both local and CI environments render the application in the same way, reducing unexpected visual or layout-driven failures. Responsive layouts react differently under these conditions, shifting elements or altering visibility.

Defining consistent viewports:

```typescript
use: {
  viewport: { width: 1280, height: 720 },
  deviceScaleFactor: 1
}
```

This removes variability and helps ensure consistent rendering.

###### Using Network Stubbing for Unstable External APIs

External dependencies introduce risk. Rate limits, slow responses, or inconsistent payloads create flaky tests in CI. Stubbing network calls ensures deterministic behavior.

```typescript
await page.route("**/api/cart", (route) => {
  route.fulfill({ json: { items: [] } });
});
```

One common pitfall is route leakage across tests. Route handlers persist for the lifetime of the page or context, so if routes are registered in shared fixtures or global setup without proper teardown, they can unintentionally affect subsequent tests. This often manifests as CI-only failures where tests behave differently depending on execution order.

This eliminates external variability for UI flows, but excessive stubbing can mask real integration failures. Network stubbing should be limited to unstable third-party dependencies, while critical backend integrations should be validated separately.

Route handlers should be scoped as narrowly as possible. In most cases, routes belong inside individual tests or test-scoped fixtures so they are automatically cleaned up when the test completes. Avoid registering routes in global setup or shared fixtures unless every test in the suite explicitly depends on that behavior.

##### Diagnosing CI Failures Caused by Parallelism

Parallel execution is one of the most common sources of CI-only failures. Tests that pass reliably when run alone often fail under parallelism due to shared backend data, reused accounts, or non-isolated resources.

Typical symptoms include:

- Tests passing locally but failing only when multiple workers run
- Failures that disappear when workers are reduced to one
- Inconsistent data-related assertions

To diagnose parallelism-related issues:

- Run:

```bash
npx playwright test --workers=1
```

- Compare failures between parallel and serial runs
- Inspect whether multiple tests mutate the same backend records or reuse the same user accounts

Parallelism failures aren’t limited to backend data collisions.
A frequent source of parallelism-related failures is misuse of storageState across workers. When multiple tests or workers reuse the same authenticated `storageState` file, they effectively share session cookies and localStorage.
Under parallel execution, this can lead to session invalidation, unexpected logouts, or tests mutating the same server-side user state concurrently.
Parallel-safe test design typically requires isolating data per worker rather than per test suite. Common strategies include creating unique users per worker, namespacing records using the worker index, or provisioning test data dynamically at runtime.
Parallelism issues are rarely solved by retries or timeouts; they are almost always a data isolation problem. Parallelism also introduces architectural tradeoffs. Increasing worker count improves speed but amplifies data contention and fixture complexity. In some cases, selectively running high-risk flows serially or reducing worker count for specific test groups provides better overall stability than maximizing parallelism everywhere.

They can also stem from test-suite architecture itself. For example, worker-scoped fixtures (`beforeAll`, `workerInfo`), module-level variables, or shared helpers that unintentionally persist state across workers. These issues often surface only under parallel execution, making CI the first place they appear.

##### Final Thoughts

Debugging in CI can feel opaque, especially when local tests behave predictably. Much of the challenge comes from environmental discrepancies: slower infrastructure, fresh sessions, altered resource usage, or mismatched configurations.

With richer traces, targeted reruns, careful selector strategies, and systematic visibility enhancements, the debugging flow becomes clear. Maintaining a healthy CI environment, one that mirrors local conditions and avoids rigid timeouts, helps reduce false failures and align outcomes across all platforms.

Each improvement creates a stronger foundation for consistent test outcomes. With structured debugging, resilient selectors, and proactive CI maintenance, Playwright becomes a reliable tool for verifying application quality across environments. Stability grows with visibility, and visibility comes from the combined power of tracing, selective reruns, clear logs, and deliberate environment alignment.

Keep in mind that increasing global timeouts rarely solves CI flakiness. Failures almost always come from missing conditions or unstable selectors, not from the test runner needing "more time."

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![Debugging Playwright Timeouts: A Practical Checklist](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-timeouts%2Fposter.png&w=3840&q=90)\\
\\
Oct 23, 2025 **Debugging Playwright Timeouts: A Practical Checklist** \\
\\
![Goodness Eboh](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Goodness Eboh](https://currents.dev/posts/debugging-playwright-timeouts) [![How to Track the Health of Your Playwright Test Suite](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-track-the-health-of-your-playwright-test-suite%2Fposter.png&w=3840&q=90)\\
\\
Nov 12, 2025 **How to Track the Health of Your Playwright Test Suite** \\
\\
![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Joshua Adeyemi](https://currents.dev/posts/how-to-track-the-health-of-your-playwright-test-suite) [![What Is a Flaky Test in Software Testing, and How to Fix It](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-is-a-flaky-test-and-how-to-fix-it%2Fposter.png&w=3840&q=90)\\
\\
Oct 30, 2025 **What Is a Flaky Test in Software Testing, and How to Fix It** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it)

### 16. Currents.dev — Speed up Playwright tests

- Source: https://currents.dev/posts/how-to-speed-up-playwright-tests
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)

Currents Team

•Jul 22, 2026•

#### Playwright API Testing: Patterns That Actually Scale

Playwright API tests that pass locally still fail in CI? Here's the fixture architecture, per-worker isolation, and observability practices that fix that.

![Playwright API Testing: Patterns That Actually Scale](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-api-testing%2Fcover.jpg&w=3840&q=100)

Playwright API tests fail in CI because of shared state, not because of `APIRequestContext`.

Most suites start the same way: a few `request.get()` calls, an auth setup in `beforeAll`, and a green local run. The problems show up once parallel workers enter the picture. Tests pass on retry with no code changes. The same test fails on Worker 1 and passes on Worker 3. An [ICST 2024 industrial case study](https://conf.researchr.org/details/icst-2024/icst-2024-industry/1/Cost-of-Flaky-Tests-in-CI-An-Industrial-Case-Study) spanning five years of CI data found that flaky tests consume at least 2.5% of total productive developer time.

[Playwright workers](https://playwright.dev/docs/test-parallel#worker-processes) are isolated OS processes. They share no in-process state. But they still hit the same backend. Worker 1 and Worker 3 both target the same test account. One mutates session state that the other depends on. The failures look random because the instability comes from shared backend data, not from the test framework itself.

This guide covers fixture architecture, parallel-safe data strategies, and the observability practices we've seen work on teams running 200+ API tests across multiple CI workers.

##### **Choosing the Right Request Surface**

Before fixture architecture matters, the request surface has to match the problem. A mismatch won't break locally. It breaks in CI under parallel execution, when auth state from one test leaks into another.

Playwright exposes three ways to make API requests. The difference between them is cookie behavior and lifecycle ownership:

`request` fixture: test-scoped, auto-managed by Playwright, isolated cookie jar per test. Use this for pure API tests. It's the right default.

`page.request` / `browserContext.request`: these are [the same object](https://playwright.dev/docs/api/class-page#page-request). `page.request` returns the `APIRequestContext` of the page's browser context, so cookies are shared at the context level, not per page. Use it when your API call needs the same session the browser already authenticated: it sends the context's cookies and updates them from `Set-Cookie` response headers. The danger: an API call that mutates session state (logout, token rotation) changes the cookies the browser is using mid-test.

`playwright.request.newContext()`: standalone, explicit lifecycle. You create it, you dispose it. Use this when you need full isolation from browser cookies, or when you need custom headers that shouldn't affect the browser context.

The most common mistake we see: teams start with `playwright.request.newContext()` in `beforeAll` because it feels explicit, then discover it creates lifecycle problems that the `request` fixture solves out of the box. Start with the `request` fixture. Upgrade to `newContext()` only when you need cross-test session sharing or independent header configuration.

Centralize config in `playwright.config.ts`. Setting `baseURL` and `extraHTTPHeaders` there eliminates per-test repetition and prevents drift between files:

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    baseURL: process.env.API_BASE_URL || 'https://api.staging.example.com',
    extraHTTPHeaders: {
      'Accept': 'application/json',
      ...(process.env.API_TOKEN ? { 'Authorization': `Bearer ${process.env.API_TOKEN}` } : {}),
    },
  },
});
```

Two gotchas that waste hours in CI:

First, `toBeOK()` only works with [`APIResponse`](https://playwright.dev/docs/api/class-apiresponse) objects returned by `request.get()`, `request.post()`, and similar methods. It does not work with the `Response` type returned by `page.waitForResponse()`. In TypeScript, this is a compile error. In JavaScript, it throws at runtime with a "toBeOK can be only used with APIResponse object" error. Easy to fix once you see it, but if you're mixing both response types in the same file, it's a recurring papercut.

Second, `response.json()` throws when the body isn't valid JSON. In CI, this happens regularly. Load balancers return [HTML error pages](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it) on `502` or `504` responses. The stack trace points at the `json()` call, not at the upstream failure. Check content type before parsing:

```typescript
const response = await apiContext.get('/api/resource');
const contentType = response.headers()['content-type'] || '';
if (!contentType.includes('application/json')) {
  throw new Error(
    `Expected JSON but got ${contentType}. Status: ${response.status()}. Body: ${await response.text()}`
  );
}
const body = await response.json();
```

One more lifecycle detail: the `request` fixture handles disposal automatically. `playwright.request.newContext()` does not. If you skip `await apiContext.dispose()` in teardown, the context leaks connections until the worker process dies.

##### **Building Fixture Architecture That Holds Up in CI**

Knowing which request surface to use is only the starting point. The harder problem is lifecycle management and state isolation. Most suites that fail under parallelism are not suffering from the wrong request surface. The issue usually traces back to `APIRequestContext` setup living in the wrong place and scoped incorrectly.

###### **Why Inline** **`beforeAll`** **Auth Breaks at Scale**

This setup is common across Playwright codebases. It works locally, passes in a single-worker run, and starts failing in ways that are hard to trace once the suite grows:

```typescript
// fragile-setup.spec.ts
let apiContext: APIRequestContext;

test.beforeAll(async ({ playwright }) => {
  apiContext = await playwright.request.newContext({
    baseURL: process.env.API_BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${process.env.API_TOKEN}`,
    },
  });
});

test.afterAll(async () => {
  await apiContext?.dispose();
});
```

The pattern breaks in CI in three ways:

1. **Token expiry.** The context authenticates once in `beforeAll` and holds that token for every test in the file. On long suites, tokens expire mid-run. Fixtures that re-authenticate per test (or per worker) avoid this.
2. **Leaked cleanup.** If `beforeAll` throws partway through, or a test in the file fails in a way that skips the rest, `afterAll` cleanup logic is easy to get wrong: it has to defensively handle every partial-setup state. Fixture teardown (the code after `use()`) runs even when a test fails, because Playwright manages its lifecycle internally. Neither survives a hard process kill (OOM, spot instance termination), which is why the data strategies later in this guide matter more than any hook.
3. **Retry pollution.** [Playwright restarts the worker after a failure](https://playwright.dev/docs/test-parallel#worker-processes), so `beforeAll` does run again on retry. What doesn't reset is the backend: the retry inherits whatever data the failed attempt created. That's not a hook problem, it's a data problem, and it applies to fixtures too. Setup logic has to be idempotent (upserts, check-before-create, per-attempt naming) or retries fail against leftovers from the first attempt.

![Why fixture scope outlasts beforeAll in CI](https://currents.dev/img/posts/playwright-api-testing/ci.jpg)Why fixture scope outlasts beforeAll in CI

###### **The API Fixture Pattern**

The right pattern moves `APIRequestContext` into a named fixture that extends the base test object:

```typescript
// fixtures.ts
import { test as baseTest, APIRequestContext } from '@playwright/test';

type ApiFixtures = {
  apiContext: APIRequestContext;
};

export const test = baseTest.extend<ApiFixtures>({
  apiContext: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: process.env.API_BASE_URL,
      extraHTTPHeaders: {
        Authorization: `Bearer ${process.env.API_TOKEN}`,
        Accept: 'application/json',
      },
    });
    await use(context);
    await context.dispose();
  },
});

export { expect } from '@playwright/test';
```

Test-scoped fixtures are the default choice for most suites because they fully isolate state between tests. The `use()` plus teardown pattern ensures `dispose()` runs even when a test fails, which `afterAll` does not guarantee.

Worker-scoped fixtures are only safe when every test in that worker shares the same auth identity and the session is treated as read-only. If any test mutates backend state, test scope is the safer default regardless of whether auth is shared.

Test-scoped fixtures re-run on every retry, so any backend data they create must be idempotent or uniquely namespaced per attempt. Upsert logic, check-before-create patterns, or worker-indexed identifiers all solve this. Without those safeguards, retries often fail against resources created in the first attempt rather than surfacing the original issue, which skews debugging toward the wrong failure.

The performance tradeoff matters. Creating an `APIRequestContext` itself is fast (milliseconds). But if your fixture authenticates against a real auth server, that round-trip costs whatever your auth endpoint costs. Say it's one second: across 200 tests at test scope, that's over three minutes of pure auth overhead per run. Worker-scoped fixtures amortize that cost across all tests in the worker.

The rule is simple: if every test in the worker uses the same identity and treats the session as read-only, scope to the worker. If any test mutates backend state, scope to the test. When in doubt, start with test scope and optimize later with profiling data.

###### **Authenticated Fixtures with** **`storageState`**

For suites where UI and API tests share the same session model, a setup project generates browser auth state once and `storageState` loads it into the fixture:

```typescript
// fixtures.ts
import { test as baseTest, APIRequestContext } from '@playwright/test';

export const test = baseTest.extend<{ apiContext: APIRequestContext }>({
  apiContext: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: process.env.API_BASE_URL,
      storageState: 'playwright/.auth/user.json',
    });
    await use(context);
    await context.dispose();
  },
});
```

Cookie-backed sessions load cleanly from `storageState`. For apps that authenticate API calls through `Authorization` headers, `storageState` alone is insufficient. Extract the token during setup and pass it via `extraHTTPHeaders`. The complete [Playwright authentication guide](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide) covers per-worker state using `parallelIndex` for this pattern.

Two caveats apply regardless of auth mechanism. `storageState` files go stale and should never be committed or cached across CI runs: generate them fresh in a setup project on every run. Across retries, `storageState` reuse requires idempotent auth setup, otherwise the retry authenticates against a modified session state.

With fixture-managed lifecycles in place, the next step is using API and UI layers together.

##### **Hybrid API + UI Testing: The Real Payoff**

An endpoint can return `200` while the UI renders nothing. A UI action can look successful while the backend state never changed. Pure API tests and pure UI tests both miss this. Hybrid tests catch it because each layer validates what the other can't.

Three patterns cover most hybrid scenarios, ranked by complexity and maintenance cost:

**Pattern 1: Seed via API, assert in UI.** The right default for most hybrid scenarios: an API call sets up backend state and a UI test validates that the state renders correctly. It is also the easiest of the three patterns to maintain over time.

**Pattern 2: Act in UI, verify via API.** A UI interaction triggers an action, then an API call confirms the backend state actually changed. This catches the gap between what the UI displays and what the backend actually recorded.

```typescript
test('submitting order updates backend state', async ({ page, apiContext }) => {
  await page.goto('/orders/new');
  await page.getByLabel('Product').selectOption('widget-a');
  await page.getByRole('button', { name: 'Submit Order' }).click();
  await expect(page.getByText('Order confirmed')).toBeVisible();

  const response = await apiContext.get('/api/orders/latest');
  expect(response.ok()).toBeTruthy();
  const order = await response.json();
  expect(order.status).toBe('confirmed');
  expect(order.product).toBe('widget-a');
});
```

The trap with Pattern 2 is eventual consistency. The UI shows "Order confirmed" but the API might not reflect the change for a few hundred milliseconds. Don't add a hard `waitForTimeout()`. Instead, use `expect.poll()` to retry the API check:

```typescript
await expect.poll(async () => {
  const response = await apiContext.get('/api/orders/latest');
  const order = await response.json();
  return order.status;
}, { timeout: 5000 }).toBe('confirmed');
```

**Pattern 3: API to UI to API loop.** Reserved for critical user journeys where full round-trip verification is justified. Seed data via API, validate it renders in the UI, perform a UI action, then verify the backend state changed. The maintenance cost is high. Use it for checkout flows, payment processing, or onboarding. Not for CRUD operations.

The `apiContext` fixture and the browser context do not share auth automatically. This is where most hybrid tests break silently: the browser authenticates fine, but the API call returns `401` mid-test because nobody passed the session to both contexts.

For cookie-based auth: load the same `storageState` into both the browser context and the API fixture. For header-based auth: extract the token during setup and pass it via `extraHTTPHeaders` in the API fixture. Don't assume that authenticating the browser magically authenticates API calls.

Once hybrid tests work with consistent auth, the next failure mode is data collision across parallel workers.

##### **Parallel-Safe API Testing in CI**

Hybrid tests expose integration failures. Parallel CI runs expose weaknesses in data architecture. The previous challenge was auth coordination across API and UI contexts. Once those tests execute across multiple workers simultaneously, the pressure shifts to data collision, isolation strategy, and observability.

###### **Why Parallel Runs Break API Tests**

![Parallel workers, one account, and the collisions that follow](https://currents.dev/img/posts/playwright-api-testing/problem.png)Parallel workers, one account, and the collisions that follow

Playwright workers isolate browser state but still operate against the same backend. Two workers targeting the same test account both assume exclusive ownership of that data. One changes a tenant-level setting the other relies on. Neither worker has visibility into the other's activity. The resulting failures appear random because the instability originates in shared backend state rather than in the test logic.

Per-worker data scoping reduces that risk. `testInfo.workerIndex` and `testInfo.parallelIndex` solve different problems, and using the wrong one creates avoidable collisions.

- `testInfo.workerIndex` is a monotonically increasing ID starting at 1 that's unique across the entire run. Every new worker process (including restarts after failure) gets a new index. Use it for identifiers that must never collide, like dynamically created test resources.

- `testInfo.parallelIndex` ranges from `0` to `workers - 1` and stays stable across worker restarts. If Worker 2 crashes and Playwright spawns a replacement, the new worker keeps `parallelIndex: 2` but gets a fresh `workerIndex`. Use it when each parallel slot maps to a fixed pool of pre-provisioned resources. The pool must be at least as large as the configured `workers` count. If `workers` exceeds the pool size, multiple slots collide on the same resource and the isolation breaks.

```typescript
const email = `test-user-${testInfo.workerIndex}@example.com`;

const accounts = ['admin@example.com', 'editor@example.com', 'viewer@example.com'];

// Pool size must be >= workers setting to avoid collisions
if (testInfo.parallelIndex >= accounts.length) {
  throw new Error(`parallelIndex ${testInfo.parallelIndex} exceeds account pool size ${accounts.length}. Add more accounts or reduce workers.`);
}

const account = accounts[testInfo.parallelIndex];
```

###### **Test Data Isolation Strategy**

Per-worker scoping solves identity collision. Data lifecycle strategy determines how the remaining failures are contained:

- Tests creating backend data should clean it up even after failure. Teardown belongs inside `try/finally` blocks within fixtures rather than loose `afterEach` hooks that may never execute.

- Ephemeral data strategies are more reliable than teardown alone. TTL-based records, tenant scoping, and namespaced resources prevent partial state from leaking into later runs.

- Stable reference data belongs in `globalSetup` and should remain read-only throughout the suite.

- Retries require idempotent setup logic. Upsert patterns, deterministic naming, or check-before-create flows prevent retries from failing against resources already created during earlier attempts.

```typescript
apiContext: async ({ playwright }, use) => {
  const context = await playwright.request.newContext({
    baseURL: process.env.API_BASE_URL,
  });
  const testData = await createTestData(context);
  try {
    await use(context);
  } finally {
    await cleanupTestData(context, testData.id);
    await context.dispose();
  }
},
```

###### **Observability: What to Do When an API Test Fails in CI**

UI test failures give you screenshots, traces, and DOM snapshots. API test failures give you a status code and a timeout. That gap in debugging context is why API failures take longer to investigate, even though they're usually simpler problems.

Close the gap with `testInfo.attach()`. Attach the request URL, sanitized headers, status code, and response body to every failed API test. This turns "test timed out" into "POST /api/orders returned 502 with body: `<html>Bad Gateway</html>`." That's the difference between a 5-minute fix and a 30-minute investigation.

```typescript
test('creates order', async ({ apiContext }, testInfo) => {
  const response = await apiContext.post('/api/orders', {
    data: { product: 'widget-a', quantity: 1 },
  });

  if (!response.ok()) {
    await testInfo.attach('api-failure', {
      body: JSON.stringify({
        url: response.url(),
        status: response.status(),
        body: await response.text(),
      }, null, 2),
      contentType: 'application/json',
    });
  }

  expect(response.ok()).toBeTruthy();
});
```

For suites with many API calls, wrap this pattern in a helper that attaches context automatically on non-2xx responses. That way you get debugging data without cluttering every test.

Per-run artifacts tell you what failed. They don't tell you why the failure only appears on Worker 3, after retries, on the `feature/checkout` branch. Those patterns need cross-run visibility. [Currents](https://currents.dev/) surfaces those patterns across workers, branches, and retries in one place. That's what turns a repeated `401` into a diagnosable data collision rather than an unexplained flake. Teams dealing with persistent timeout failures should also review [Playwright's timeout debugging guidance](https://currents.dev/posts/debugging-playwright-timeouts) for CI-specific patterns.

##### **Network Interception: When to Mock, When to Hit the Real Backend**

Mocking and real API validation solve different problems. Mixing them up is one of the fastest ways to build a suite that passes every run and catches zero regressions.

The rule: test what you control, mock what you don't. [Playwright's best practices](https://playwright.dev/docs/best-practices) say this directly. Your `APIRequestContext` should hit real endpoints. `page.route()` and `browserContext.route()` should intercept third-party dependencies you can't control: analytics, payment processors, CDNs.

The failure mode we see most often: a team mocks their own API to make a flaky test pass. The test goes green. Six months later, a backend field gets renamed and nothing catches it because the mock still returns the old shape. If the test is meant to validate your backend, it has to talk to your backend.

Route handler precedence matters in hybrid setups. When both [`page.route()` and `browserContext.route()`](https://playwright.dev/docs/api/class-browsercontext#browser-context-route) match the same URL, the page-level handler wins. Register broad mocks at the context level (block all analytics), then override specific routes per test:

```typescript
// Global mock at context level: stub analytics endpoint
await browserContext.route('**/analytics/**', route =>
  route.fulfill({ status: 200, body: '{}' })
);

// Test-specific override: simulate a 500 from a third-party service
await page.route('**/analytics/track', async route => {
  await route.fulfill({
    status: 500,
    body: JSON.stringify({ error: 'upstream failure' }),
  });
});
```

For first-party endpoints, use interception only when you're intentionally testing failure handling or edge cases (simulating a `503`, testing retry logic). Not when the goal is to validate real backend behavior. [The Playwright network mocking playbook](https://currents.dev/posts/the-playwright-network-mocking-playbook) goes deeper on how to apply this without accumulating mock debt.

##### **Schema Validation: Catching Contract Drift Before It Breaks the UI**

Most API tests check a status code and assert one or two fields. That works until a backend team renames a property, adds a required field, or changes a type from `string` to `number`. The test still passes. The frontend breaks in production.

This is contract drift. The API shape changed, but nothing in the test suite noticed because no test validated the shape. Adding schema validation doesn't mean turning every test into a contract test. It means being intentional about where you check structure vs. where you check behavior.

**Start with `toMatchObject()`.** It's built into Playwright, requires no dependencies, and catches the most common drift: missing fields, wrong types, unexpected nulls. It does partial matching, so it won't break when the backend adds new optional fields.

```typescript
// This misses structural drift. The field could be a number and this still passes.
expect(body.id).toBeDefined();

// This catches type changes and missing fields.
expect(body).toMatchObject({
  id: expect.any(String),
  status: expect.any(String),
  createdAt: expect.any(String),
});
```

**Upgrade to Zod when your API serves multiple consumers.** If the frontend, mobile app, and a partner integration all depend on the same endpoint, `toMatchObject()` isn't strict enough. Zod's [`safeParse`](https://zod.dev/basics#handling-errors) validates the full shape and produces CI-friendly error output that tells you exactly which field broke:

```typescript
import { z } from 'zod';

const OrderSchema = z.object({
  id: z.uuid(),
  status: z.enum(['pending', 'confirmed', 'shipped']),
  createdAt: z.iso.datetime(),
  items: z.array(z.object({
    productId: z.string(),
    quantity: z.number().int().positive(),
  })),
});

test('order response matches contract', async ({ apiContext }) => {
  const response = await apiContext.get('/api/orders/latest');
  const body = await response.json();
  const result = OrderSchema.safeParse(body);
  if (!result.success) {
    throw new Error(`Schema validation failed:\n${z.prettifyError(result.error)}`);
  }
});
```

The example uses Zod 4 idioms: `z.uuid()` and `z.iso.datetime()` replaced the deprecated `z.string().uuid()` and `z.string().datetime()`, and [`z.prettifyError()`](https://zod.dev/error-formatting) turns the error into a readable string. On Zod 3, use `JSON.stringify(result.error.format(), null, 2)` instead; interpolating `result.error.format()` directly prints `[object Object]`.

**Don't validate schema in every test.** Dedicate a small set of contract tests that validate response shapes against critical endpoints. Your feature tests should assert behavior (did the order get created?). Your contract tests should assert shape (does the response still match what the frontend expects?). Mixing both concerns in every test makes the suite brittle and hard to maintain.

If you have an OpenAPI spec, consider generating Zod schemas from it with tools like [`openapi-zod-client`](https://github.com/astahmer/openapi-zod-client). That keeps test schemas in sync with the API definition automatically. (Don't confuse it with `zod-openapi`, which goes the other direction: it generates OpenAPI docs from Zod schemas.)

##### **CI Configuration That Reflects the Architecture**

None of this holds up if API and UI tests share a single Playwright project with identical settings. API tests have different timeout profiles, different failure modes, and different retry economics. Separate projects let you configure each independently. [Playwright's CI documentation](https://playwright.dev/docs/ci) supports this directly.

Three configuration decisions matter specifically for API projects:

- **Timeouts.** Set per-request timeouts on the API context: `playwright.request.newContext({ timeout: 15000 })`. Don't confuse this with `actionTimeout`, which only applies to UI actions like `click()` and `fill()`. `actionTimeout` has no effect on API requests, regardless of whether you use the `request` fixture or `page.request`.

- **Retries.** Configure them independently. A flaky auth endpoint may justify one retry. A fragile UI flow may need two. Sharing the same retry count adds noise: you either over-retry API tests (wasting time) or under-retry UI tests (missing real flakes).

- **Failure output.** API failures in CI give you a status code and a timeout. That's not enough. Use `testInfo.attach()` to capture request URLs, sanitized headers, status codes, and response bodies. This is your equivalent of screenshots and traces for API tests.

Use `project.dependencies` only when the API project creates state that the UI project needs. If they're independent, run them in parallel. Linking unrelated projects slows feedback and makes it harder to tell whether a failure came from setup or from the test.

API test results belong in the same reporting view as UI results. Routing them to separate dashboards means [debugging across disconnected systems](https://currents.dev/posts/how-to-run-playwright-tests-without-the-pain), which adds time to every incident.

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  projects: [\
    {\
      name: 'api-setup',\
      testDir: './tests/api',\
      use: {\
        baseURL: process.env.API_BASE_URL,\
        extraHTTPHeaders: {\
          Authorization: `Bearer ${process.env.API_TOKEN}`,\
        },\
      },\
      // Per-test timeout for the API project\
      // Set per-request timeout in the fixture via playwright.request.newContext({ timeout })\
      timeout: 15000,\
      retries: 1,\
    },\
    {\
      name: 'ui',\
      testDir: './tests/ui',\
      dependencies: ['api-setup'],\
      use: {\
        baseURL: process.env.BASE_URL,\
        actionTimeout: 10000,\
      },\
      retries: 2,\
    },\
  ],
  reporter: [['html'], ['junit', { outputFile: 'results.xml' }]],
});
```

##### **Wrapping Up**

The pattern behind every fix in this guide is the same: move lifecycle management into fixtures, scope data to the worker, and make failures visible without manual artifact hunting.

If you take one thing from this: stop using `beforeAll` for API context setup. Move it into a fixture. That single change fixes token expiry, leaked contexts, and retry pollution in one step.

API failures deserve the same cross-run visibility you expect from UI failures. [Currents](https://currents.dev/) provides that without custom reporting infrastructure, surfacing failure patterns across workers, branches, and retries in one place.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![Debugging Playwright Timeouts: A Practical Checklist](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-timeouts%2Fposter.png&w=3840&q=90)\\
\\
Oct 23, 2025 **Debugging Playwright Timeouts: A Practical Checklist** \\
\\
![Goodness Eboh](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Goodness Eboh](https://currents.dev/posts/debugging-playwright-timeouts) [![How To Speed Up Playwright Tests: 7 Tips From Experts](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-speed-up-playwright-tests%2Fcover.png&w=3840&q=90)\\
\\
Jan 07, 2026 **How To Speed Up Playwright Tests: 7 Tips From Experts** \\
\\
![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Joshua Adeyemi](https://currents.dev/posts/how-to-speed-up-playwright-tests) [![How To Build Reliable Playwright Tests: A Cultural Approach](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-build-reliable-playwright-tests-a-cultural-approach%2Fposter.png&w=3840&q=90)\\
\\
Nov 28, 2025 **How To Build Reliable Playwright Tests: A Cultural Approach** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/how-to-build-reliable-playwright-tests-a-cultural-approach) [![What Is a Flaky Test in Software Testing, and How to Fix It](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-is-a-flaky-test-and-how-to-fix-it%2Fposter.png&w=3840&q=90)\\
\\
Oct 30, 2025 **What Is a Flaky Test in Software Testing, and How to Fix It** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it)

### 15. Currents.dev — Testing authentication

- Source: https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)

Currents Team

•Apr 15, 2026•

#### Testing Authentication with Playwright: The Complete Guide

Learn how to handle secrets, OAuth, MFA, magic links, and parallel CI without flaky runs.

![Testing Authentication with Playwright: The Complete Guide](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Ftesting-authentication-with-playwright-the-complete-guide%2Fcover.png&w=3840&q=100)

Most Playwright test suites start with a login helper, a few stored credentials, and auth that works. At 20 or 30 tests, it holds up. Past 100, the auth setup that worked fine at 30 [starts breaking](https://currents.dev/posts/what-breaks-when-your-test-suite-grows-from-20-to-500-tests) in ways that are hard to trace.

Authentication touches every test that involves a user session. When it breaks, the damage spreads. The problem appears once tests run in parallel: multiple workers reuse the same session state, tokens expire during long pipeline runs, and test helpers introduce race conditions that pass locally but fail in CI. If each login takes 5 to 15 seconds through the UI and you're running 100 tests without cached auth, that's 8 to 25 minutes of pure login overhead per run.

MFA is now the standard, with [83% of organizations requiring it and adoption reaching 87% among larger companies](https://jumpcloud.com/blog/multi-factor-authentication-statistics). Combined with SSO, magic links, and multi-tenant flows, authentication requires a test architecture that won't collapse as the suite grows.

**What this guide covers:**

- [Why Authentication Testing Is Different](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#why-authentication-testing-is-different)
- [The Credential Anti-Patterns You'll Hit at Scale](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#the-credential-anti-patterns-youll-hit-at-scale)
- [Authentication State Architecture at Scale](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#authentication-state-architecture-at-scale)
- [Multi-User Testing with Multiple Roles](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#multi-user-testing-with-multiple-roles)
- [Testing OAuth Flows Reliably](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#testing-oauth-flows-reliably)
- [Magic Link Testing](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#magic-link-testing)
- [SSO and Multi-Tenant Auth](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#sso-and-multi-tenant-auth)
- [MFA and Session Management](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#mfa-and-session-management)
- [Debugging Auth Test Failures](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#debugging-auth-test-failures)
- [Integration with CI/CD and Secrets Management](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#integration-with-cicd-and-secrets-management)
- [Compliance and Security](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#compliance-and-security)
- [Observability](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#observability)

##### Why Authentication Testing Is Different

Auth state affects every part of the system. A session cookie, an OAuth token, or a `storageState` file shapes what pages render, what API calls succeed, and what other tests see when they run in parallel.

Playwright's [browser context](https://playwright.dev/docs/browser-contexts) isolation gives each test its own cookies, localStorage, and cache. But it does not isolate server-side state. Two tests running in separate contexts but sharing the same user account can still interfere with each other. That's what makes auth testing harder than it looks.

OAuth providers, SSO systems, and email-based magic links add dependencies your test suite can't control. A rate limit from an identity provider, a delayed token exchange, or a SAML redirect that behaves differently in CI than locally will break a test that otherwise works fine. And credentials carry a different weight than regular test data. A leaked `storageState` file or a hardcoded token isn't a test problem; it's a security incident. More on this in the [Compliance and Security](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#compliance-and-security) section.

##### The Credential Anti-Patterns You'll Hit at Scale

In many test suites, credentials live in a `.env` file, a single test user handles everything, and the suite runs in sequence. That works at 20 tests. At 100, failures appear that trace back to decisions that made sense when the suite was small.

###### Anti-Pattern 1: The Single Shared Test User

A single email and password used across every test works fine for sequential runs. The problem hits the moment you run in parallel. Six workers sharing one account means your tests fight over the same session state. One test changes a role permission, another reads stale state, and failures appear in places that have nothing to do with the code under test.

One account per worker is the minimum. If your app mutates server-side state per user (role changes, settings, preferences), you need one account per test. A user factory that provisions isolated accounts at the start of each worker removes these conflicts at the source.

[Playwright's parallel execution model](https://playwright.dev/docs/test-parallel) means this isn't optional. A password change, an account lockout, or an MFA prompt on the shared user mid-run takes down the entire suite.

###### Anti-Pattern 2: Credentials Hardcoded in Git

`const testUser = { email: 'test@example.com', password: 'password123' }`

Lines like this appear in more repositories than you'd expect. They start as a convenience during local development and become permanent. [GitGuardian's 2025 State of Secrets Sprawl report](https://blog.gitguardian.com/the-state-of-secrets-sprawl-2025/) found 23.8 million secrets leaked on public GitHub repositories in 2024 alone (25% year-over-year increase), with 70% of secrets from 2022 still active. Private repos are worse: 35% contain at least one plaintext credential.

###### Anti-Pattern 3: Globally Shared Auth Tokens

Storing a token in a global variable after the first test authenticates and having every following test read from it looks like a performance win. In practice, it creates a hidden dependency chain across the entire suite. When that token expires mid-run, all downstream tests fail. Logout events and session invalidation make the failures random.

Scope auth state per worker or per test context. Worker-scoped fixtures using `testInfo.parallelIndex` provision unique storage state files per worker, removing the shared token dependency entirely. We'll show the code for this below.

###### Anti-Pattern 4: Mocking OAuth Without Real Integration

Mocking OAuth at the feature test layer is the right call for speed. The problem is when mocking is the only coverage. The actual redirect flow, token exchange, and callback handling never get tested, and those are exactly where production bugs appear. A misconfigured redirect URI, a missing `state` parameter check, or a broken PKCE implementation stays invisible in a mocked environment and shows up when a real user logs in.

###### Anti-Pattern 5: Testing Only the Happy Path

Only testing successful logins while ignoring invalid credentials, account lockouts, expired MFA codes, or failed SSO assertions leaves you blind to failures that actually happen in production. Weak lockout logic and missing error redirects are security issues that surface when real users hit them.

###### Anti-Pattern 6: Timing-Dependent Auth Checks

Using `page.waitForTimeout()` after login instead of waiting for a specific signal (URL change, visible element, API response) is one of the most common sources of auth flakiness. [Playwright's auto-waiting](https://playwright.dev/docs/actionability) handles most of this, but auth flows often involve redirects across multiple URLs, and those need [explicit waits for the final destination](https://currents.dev/posts/debugging-playwright-timeouts).

###### Anti-Pattern 7: Leftover Sessions Between Runs

If sessions persist in the database or cache after a test finishes, they affect the starting state of the next run. [Explicit teardown](https://playwright.dev/docs/test-global-setup-teardown) (clearing sessions and removing test users) after each run keeps the environment consistent. Writing auth files under [`testProject.outputDir`](https://playwright.dev/docs/api/class-testproject#test-project-output-dir), which Playwright automatically cleans before each run, removes this problem entirely.

##### Authentication State Architecture at Scale

When your suite grows past 100 tests, the first thing that breaks is the authentication setup. Most suites start with a single setup file that logs in once and saves a `storageState` file, and every test project loads that file. That works until tests start modifying server-side state, running across multiple parallel workers, or operating across different user roles.

Treat authentication as infrastructure, not setup boilerplate.

![Authentication Test Architecture](https://currents.dev/img/posts/testing-authentication-with-playwright-the-complete-guide/auth-state-architecture-at-scale.png)Authentication Test Architecture

###### The storageState Setup Project

The foundation is Playwright's [setup project pattern](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests). A dedicated setup file logs in once and saves the browser state. Every test project declares that setup as a dependency and loads the saved state.

```typescript
// tests/auth.setup.ts
import { test as setup, expect } from "@playwright/test";

const authFile = "playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill(process.env.TEST_USER_EMAIL);
  await page.getByLabel("Password").fill(process.env.TEST_USER_PASSWORD);
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL("/dashboard");

  await page.context().storageState({ path: authFile });
});
```

```typescript
// playwright.config.ts
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  projects: [\
    { name: "setup", testMatch: /.*\.setup\.ts/ },\
    {\
      name: "chromium",\
      use: {\
        ...devices["Desktop Chrome"],\
        storageState: "playwright/.auth/user.json",\
      },\
      dependencies: ["setup"],\
    },\
  ],
});
```

Every test starts already authenticated. The setup runs once, and all tests reuse the saved state.

Add `playwright/.auth` to your `.gitignore`. Storage state files contain session cookies and tokens. Committing them to your repo is a security incident.

###### Authenticate via API Instead of UI

Default to API-based authentication for your setup project. Only test login through the UI in your dedicated auth flow tests.

Playwright's `request` fixture can authenticate without opening a browser. This is faster than clicking through a login form and eliminates flakiness from form interactions.

```typescript
// tests/auth.setup.ts
import { test as setup } from "@playwright/test";

const authFile = "playwright/.auth/user.json";

setup("authenticate via API", async ({ request }) => {
  await request.post("/api/login", {
    data: {
      email: process.env.TEST_USER_EMAIL,
      password: process.env.TEST_USER_PASSWORD,
    },
  });
  await request.storageState({ path: authFile });
});
```

Use API auth for setup speed. Use UI auth only in tests that verify the login flow itself.

###### The Session Storage Gotcha

Check where your app stores auth tokens. `storageState` saves cookies and localStorage by default. If your app stores tokens in IndexedDB, pass `indexedDB: true` to `storageState()` (available since Playwright 1.51). `storageState` does not save sessionStorage regardless. If your app stores JWT tokens or session IDs in sessionStorage, the standard `storageState` pattern silently produces unauthenticated tests.

The [Playwright docs](https://playwright.dev/docs/auth#session-storage) document the workaround:

```typescript
// After login, save session storage separately
const sessionData = await page.evaluate(() => JSON.stringify(sessionStorage));
fs.writeFileSync("playwright/.auth/session.json", sessionData, "utf-8");

// In a new context, restore it with an init script
const sessionStorage = JSON.parse(
  fs.readFileSync("playwright/.auth/session.json", "utf-8"),
);
await context.addInitScript((storage) => {
  if (window.location.hostname === "your-app.com") {
    for (const [key, value] of Object.entries(storage))
      window.sessionStorage.setItem(key, value);
  }
}, sessionStorage);
```

If you don't know whether your app uses sessionStorage, check the Application tab in DevTools after logging in. This is the kind of thing that works fine locally (because the browser session persists) and fails silently in CI.

###### Per-Worker Auth Isolation

For suites where tests modify server-side state (settings, roles, permissions), a single shared auth file breaks down. The practical middle ground is one authenticated session per parallel worker.

Playwright's docs call this the ["moderate" approach](https://playwright.dev/docs/auth#moderate-one-account-per-parallel-worker). Each worker gets its own account using `testInfo.parallelIndex`:

```typescript
// playwright/fixtures.ts
import { test as baseTest, expect } from "@playwright/test";
import fs from "fs";
import path from "path";

export * from "@playwright/test";
export const test = baseTest.extend<{}, { workerStorageState: string }>({
  storageState: ({ workerStorageState }, use) => use(workerStorageState),

  workerStorageState: [\
    async ({ browser }, use) => {\
      const id = test.info().parallelIndex;\
      const fileName = path.resolve(\
        test.info().project.outputDir,\
        `.auth/${id}.json`,\
      );\
\
      if (fs.existsSync(fileName)) {\
        await use(fileName);\
        return;\
      }\
\
      const page = await browser.newPage({ storageState: undefined });\
      const account = await acquireAccount(id);\
      await page.goto("/login");\
      await page.getByLabel("Email").fill(account.email);\
      await page.getByLabel("Password").fill(account.password);\
      await page.getByRole("button", { name: "Sign in" }).click();\
      await page.waitForURL("/dashboard");\
      await page.context().storageState({ path: fileName });\
      await page.close();\
\
      await use(fileName);\
    },\
    { scope: "worker" },\
  ],
});
```

Using `project.outputDir` for the auth files means Playwright cleans them up automatically before each test run. No stale sessions leaking between runs.

###### Separate Auth Tests from Feature Tests

Auth tests verify login, logout, token expiry, MFA flows, and SSO assertions. Feature tests assume authentication works and focus on the product. Keep them in separate test suites.

When auth logic gets mixed into feature tests, failures multiply. A feature test should fail only when there's a feature problem. If auth is embedded, session expiry or token rotation triggers failures that look like product bugs. Your auth setup has its own integration points and failure modes. Give it dedicated coverage.

##### Multi-User Testing with Multiple Roles

Most production systems involve multiple users with different permissions. Define separate projects in `playwright.config.ts`, each with its own pre-authenticated `storageState`:

```typescript
// playwright.config.ts
export default defineConfig({
  projects: [\
    { name: "setup", testMatch: /.*\.setup\.ts/ },\
    {\
      name: "chromium-admin",\
      use: {\
        ...devices["Desktop Chrome"],\
        storageState: "playwright/.auth/admin.json",\
      },\
      dependencies: ["setup"],\
    },\
    {\
      name: "chromium-viewer",\
      use: {\
        ...devices["Desktop Chrome"],\
        storageState: "playwright/.auth/viewer.json",\
      },\
      dependencies: ["setup"],\
    },\
  ],
});
```

For tests that need multiple roles interacting in the same test (admin publishes content, viewer sees it), use separate browser contexts:

```typescript
test("admin changes are visible to viewer", async ({ browser }) => {
  const adminContext = await browser.newContext({
    storageState: "playwright/.auth/admin.json",
  });
  const viewerContext = await browser.newContext({
    storageState: "playwright/.auth/viewer.json",
  });

  const adminPage = await adminContext.newPage();
  const viewerPage = await viewerContext.newPage();

  await adminPage.goto("/admin/announcements");
  await adminPage.getByLabel("Title").fill("Maintenance Window");
  await adminPage.getByRole("button", { name: "Publish" }).click();
  await expect(adminPage.locator(".status")).toHaveText("Published");

  await viewerPage.goto("/dashboard");
  await expect(viewerPage.locator(".announcement")).toContainText(
    "Maintenance Window",
  );

  await adminContext.close();
  await viewerContext.close();
});
```

Test both sides of every permission: admin can access the dashboard, and viewer gets blocked from admin routes. Role changes should remove access immediately. Tenant A cannot see Tenant B's data. Every permission rule needs a test that confirms it works under parallel execution.

##### Testing OAuth Flows Reliably

OAuth flows are the hardest auth pattern to test. The redirect chain involves multiple hops across domains, server-side token exchanges, and external providers with their own rate limits, bot detection, and UI changes. Automating every test through a real OAuth provider leads to [flaky](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it), slow pipelines.

Mock OAuth for every feature test. No exceptions. Then run real provider tests on a schedule, not in your PR gate.

![Mocked Segments vs. Real Provider Integration in OAuth Flows](https://currents.dev/img/posts/testing-authentication-with-playwright-the-complete-guide/mocked-vs-real.png)Mocked Segments vs. Real Provider Integration in OAuth Flows

###### Mock OAuth for Feature Tests

Use `page.route()` to [intercept](https://currents.dev/posts/the-playwright-network-mocking-playbook) the redirect to the OAuth provider and send the browser straight back to your callback URL with a mock authorization code:

```typescript
test("dashboard loads after OAuth login", async ({ page }) => {
  await page.route("https://accounts.google.com/**", async (route) => {
    const url = new URL(route.request().url());
    const state = url.searchParams.get("state");
    const redirectUri = url.searchParams.get("redirect_uri");

    await route.fulfill({
      status: 302,
      headers: {
        Location: `${redirectUri}?code=mock-auth-code&state=${state}`,
      },
    });
  });

  await page.goto("/login");
  await page.getByRole("button", { name: "Continue with Google" }).click();
  await expect(page).toHaveURL("/dashboard");
});
```

This intercepts the browser-side redirect. Your backend still needs to handle the mock authorization code. The simplest approach is a test-mode bypass in your token exchange endpoint that accepts any code and returns a valid session. If you don't want to modify your backend, the [oauth2-mock-server](https://www.npmjs.com/package/oauth2-mock-server) npm package runs a local OAuth server with configurable token responses. Point your app's OAuth config at it during tests.

The `state` parameter matters. OAuth uses it to prevent CSRF. Your mock must echo back the exact `state` value from the original redirect, or the app will reject the callback.

###### Test Real OAuth Separately

Mocks confirm the app behaves correctly given the right inputs. They don't catch integration problems. A misconfigured redirect URI, a broken PKCE implementation, or an invalid scope request passes every mocked test and fails in production.

Run a small suite of dedicated integration tests against real OAuth outside the main CI gate. A scheduled nightly run catches these failures without blocking pull requests. Use dedicated provider test tenants for these runs: a Google Workspace test org, an Azure AD sandbox tenant, or an Okta preview org. Running against shared corporate IdPs leads to throttling and permission conflicts that create their own flakiness.

###### Mocking PKCE Flows

If your app uses PKCE (required for public clients like SPAs), the mock needs to handle the `code_verifier`/`code_challenge` exchange. During the authorization redirect, the app sends a `code_challenge` derived from a random `code_verifier`. When exchanging the authorization code for a token, the app sends the original `code_verifier`, and the server verifies it matches the challenge.

Your `page.route()` mock doesn't need to validate PKCE itself because it's bypassing the provider entirely. But your backend's token exchange endpoint does. If your backend validates the `code_verifier` against the `code_challenge`, the test-mode bypass needs to skip that check too, or the token exchange will fail even though the browser-side mock worked.

When using [oauth2-mock-server](https://www.npmjs.com/package/oauth2-mock-server), PKCE is handled automatically: it supports the Authorization Code grant with PKCE out of the box. Point your app's OAuth config at it and the full `code_challenge`/`code_verifier` cycle runs against a real (local) server.

The important thing: your scheduled real-provider tests (not the mocked ones) are what catch broken PKCE implementations. A wrong hash algorithm, a missing `code_challenge_method=S256` parameter, or a `code_verifier` that doesn't meet the [43-128 character requirement](https://datatracker.ietf.org/doc/html/rfc7636#section-4.1) all pass in mocked environments and fail in production.

###### When OAuth Mocking Breaks

If your mock returns a valid-looking callback but the app rejects it, debug it in layers.

Open the Playwright trace and go to the Network tab. Look for the request to your callback URL (`/auth/callback` or similar). Check whether the `state` query parameter matches the one from the original redirect to the OAuth provider. If it doesn't, your mock is generating or echoing the value incorrectly.

Next, compare the `redirect_uri` parameter in the intercepted request against what's registered in your OAuth provider's app configuration. A mismatch here (trailing slash, http vs https, port number) causes a silent rejection. The provider returns an error, but your app often just redirects to a login page without surfacing the cause.

Finally, check your backend logs for the token exchange step. If the browser-side redirect worked but the user still isn't authenticated, the backend likely tried to exchange the mock authorization code with the real provider and got a 400 or 401 back. Your test environment needs to either skip the token exchange or point it at a local mock server.

##### Magic Link Testing

If your app uses magic links, expose a test-only token endpoint in staging. Magic links live in email, and standard form-based test patterns can't reach them. Routing every test through real email delivery is a reliability problem you don't need.

![Magic Link Authentication Flows](https://currents.dev/img/posts/testing-authentication-with-playwright-the-complete-guide/magic-link.jpg)Magic Link Authentication Flows

###### API-Based Token Generation (Use This for Feature Tests)

If your application exposes a backend endpoint that issues magic link tokens, tests can call that endpoint directly, construct the URL, and navigate:

```typescript
test("magic link logs in the user", async ({ page, request }) => {
  const response = await request.post("/api/test/magic-link", {
    data: { email: "testuser@example.com" },
  });
  const { token } = await response.json();

  await page.goto(`/auth/verify?token=${token}`);
  await expect(page).toHaveURL("/dashboard");
  await expect(page.getByText("Welcome")).toBeVisible();
});
```

That endpoint must be restricted to test and staging environments. Making it available in production creates a token-generation vulnerability that bypasses the email delivery step entirely.

###### Email Interception for End-to-End Coverage

For tests that need to verify the full flow (email delivery, link format, click-through), tools like Mailosaur provide virtual email addresses and a queryable API. A Playwright test triggers the magic link send, polls for the email, extracts the link, and navigates to it. Email delivery adds latency and infrastructure costs, but if your product relies on magic links, you should have a small suite that covers the real flow.

Run email-based tests on a schedule, not on every commit. The main suite uses API-generated tokens for speed.

###### When Magic Links Break

Token expiration is where most coverage gaps hide. Magic link tokens typically expire in 5 to 15 minutes and should be single-use. Test that expired tokens show a clear error message, that reusing a token fails, and that malformed tokens don't crash the app. Error messaging when a link expires or gets reused deserves the same attention as the happy path.

##### SSO and Multi-Tenant Auth

SSO relies on multiple systems working together. A user starts on the login page, gets redirected to a corporate IdP, authenticates there, and returns with a SAML assertion or OIDC token that the application must verify. Each step depends on a separate system and can fail differently.

Playwright's `page.route()` can intercept the browser's POST to the Assertion Consumer Service (ACS) endpoint and return a controlled response. But SAML validation doesn't stop at the browser. The application validates the assertion's cryptographic signature server-side against the IdP's certificate.

Don't mock SAML by disabling signature validation. Use a test IdP. Disabling validation means you're testing a different code path than production. [Keycloak](https://www.keycloak.org/) in dev mode is the most practical option: it runs in a single Docker container, signs assertions with real certificates, and supports both SAML and OIDC. Spin it up alongside your app in CI and point your SP configuration at it.

```yaml
#### docker-compose.test.yml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    command: start-dev --import-realm
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
    volumes:
      - ./test-realm.json:/opt/keycloak/data/import/test-realm.json
```

Export your realm config (clients, users, roles) once and commit the JSON file. Every CI run imports the same realm, so the IdP state is reproducible. The Playwright test navigates to your app's login page, follows the redirect to Keycloak, fills the IdP form, and returns with a signed assertion:

```typescript
test("SSO login through Keycloak", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: "Sign in with SSO" }).click();

  await page.waitForURL(/keycloak.*\/realms\//);
  await page.getByLabel("Username").fill("testuser");
  await page.getByLabel("Password").fill(process.env.KEYCLOAK_TEST_PASSWORD);
  await page.getByRole("button", { name: "Sign In" }).click();

  await page.waitForURL("/dashboard");
  await expect(page.getByText("Welcome")).toBeVisible();

  const cookies = await page.context().cookies();
  expect(cookies.some((c) => c.name === "session")).toBeTruthy();
});
```

The cross-origin navigation from your app to Keycloak and back is what makes this test valuable. It exercises the real redirect chain and SAML signature validation that mocks skip.

###### Multi-Tenant Isolation

If you're running multi-tenant auth, treat each tenant as a separate Playwright project with its own base URL, credentials, and storage state:

```typescript
// playwright.config.ts
export default defineConfig({
  projects: [\
    { name: "setup", testMatch: /.*\.setup\.ts/ },\
    {\
      name: "tenant-acme",\
      use: {\
        baseURL: "https://acme.your-app.com",\
        storageState: "playwright/.auth/acme-user.json",\
      },\
      dependencies: ["setup"],\
      testMatch: /.*tenant-a.*\.spec\.ts/,\
    },\
    {\
      name: "tenant-globex",\
      use: {\
        baseURL: "https://globex.your-app.com",\
        storageState: "playwright/.auth/globex-user.json",\
      },\
      dependencies: ["setup"],\
      testMatch: /.*tenant-b.*\.spec\.ts/,\
    },\
  ],
});
```

For apps that resolve tenants dynamically (by header, path prefix, or login context rather than subdomain), use a fixture that injects the tenant identifier:

```typescript
export const test = baseTest.extend<{ tenantId: string }>({
  tenantId: ["default-tenant", { option: true }],

  storageState: async ({ tenantId }, use) => {
    await use(`playwright/.auth/${tenantId}.json`);
  },
});
```

Cross-tenant isolation tests are the most important part of multi-tenant auth coverage. Every test that confirms "Tenant A can access their data" needs a counterpart that confirms "Tenant A cannot access Tenant B's data":

```typescript
test("tenant user cannot access another tenant", async ({ browser }) => {
  const acmeContext = await browser.newContext({
    storageState: "playwright/.auth/acme-user.json",
  });
  const page = await acmeContext.newPage();

  const response = await page.goto("https://globex.your-app.com/dashboard");
  expect(response.status()).toBe(403);
  await acmeContext.close();
});
```

This isn't just a feature requirement, it's a security boundary that deserves dedicated test coverage.

###### When SSO Breaks

SSO failures in CI usually come from certificate mismatches (your test IdP cert doesn't match what the app expects), clock skew (SAML assertions have a validity window, and CI servers with drifted clocks reject them), or redirect URI misconfigurations. Check the SAML assertion's `NotBefore` and `NotOnOrAfter` fields if your tests pass locally but fail in CI.

##### MFA and Session Management

###### Testing TOTP-Based MFA

Generate TOTP codes programmatically. The `otpauth` npm package produces valid six-digit codes from a shared secret stored as an environment variable:

```typescript
import * as OTPAuth from "otpauth";

async function generateTOTP(): Promise<string> {
  const totp = new OTPAuth.TOTP({
    secret: OTPAuth.Secret.fromBase32(process.env.TOTP_SECRET),
    digits: 6,
    period: 30,
  });

  const now = Math.floor(Date.now() / 1000);
  const remaining = 30 - (now % 30);

  if (remaining < 5) {
    await new Promise((r) => setTimeout(r, remaining * 1000));
  }

  return totp.generate();
}
```

The timing check matters. TOTP codes expire every 30 seconds. If you generate a code with 2 seconds left, network latency and DOM interaction time push the submission past the expiration boundary. The test fails with what looks like an auth error but is actually a timing problem. Generate the code right before submission, and wait for a fresh period if the window is too narrow. TOTP tests also become flaky if CI runner clocks drift more than one time window. Enforce NTP sync or freeze time in containerized test environments, since the same clock skew problem [described in the SSO section](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide#when-sso-breaks) applies here.

The shared secret is never hardcoded. It lives in CI secrets and is retrieved at runtime.

###### Session Behavior

Auth test suites often have the largest coverage gaps around session behavior. Many tests check that login works, but few test what happens afterward.

**Test expired session cookies.** Use `context.addCookies()` to inject a session cookie with a past expiry, then verify your app prompts re-authentication instead of silently breaking:

```typescript
test("expired session redirects to login", async ({ browser }) => {
  const context = await browser.newContext({
    storageState: "playwright/.auth/user.json",
  });

  await context.addCookies([\
    {\
      name: "session",\
      value: "expired-token-value",\
      domain: "your-app.com",\
      path: "/",\
      expires: Math.floor(Date.now() / 1000) - 3600,\
    },\
  ]);

  const page = await context.newPage();
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login/);
  await context.close();
});
```

**Test token refresh.** Intercept the refresh endpoint with `page.route()` to control the response. This lets you verify the app handles both successful and failed refreshes:

```typescript
test("silent token refresh keeps user logged in", async ({ page }) => {
  let refreshCount = 0;
  await page.route("**/api/auth/refresh", async (route) => {
    refreshCount++;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ token: "refreshed-token", expiresIn: 3600 }),
    });
  });

  await page.goto("/dashboard");
  await page.waitForResponse("**/api/auth/refresh");
  await expect(page.locator(".user-profile")).toBeVisible();
  expect(refreshCount).toBeGreaterThan(0);
});

test("failed token refresh redirects to login", async ({ page }) => {
  await page.route("**/api/auth/refresh", async (route) => {
    await route.fulfill({ status: 401 });
  });

  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login/);
});
```

**Test session invalidation.** Call your API to invalidate a session server-side, then verify the next page load handles it gracefully instead of rendering a broken page:

```typescript
test("invalidated session shows login prompt", async ({ page, request }) => {
  await page.goto("/dashboard");
  await expect(page.locator(".user-profile")).toBeVisible();

  await request.post("/api/test/invalidate-session");

  await page.goto("/settings");
  await expect(page).toHaveURL(/\/login/);
});
```

These behaviors don't always surface as visible errors. Sometimes they appear as subtle regressions: a settings page that loads without the user's preferences, an API call that returns a 200 with empty data instead of a 401. Test your session boundaries, not just your login form.

##### Debugging Auth Test Failures

Auth failures are the hardest test failures to diagnose. A [401 response](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/401), a redirect loop, or a blank page after login all point to auth issues, but the root cause can be a missing cookie, an unconfigured CI environment variable, or a timezone difference that invalidates a TOTP code.

###### Configure Traces for Auth Debugging

Set up your config to retain evidence when auth fails:

```typescript
// playwright.config.ts
use: {
  trace: 'on-first-retry',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
}
```

[Playwright's trace viewer](https://playwright.dev/docs/trace-viewer-intro) records every network request and DOM snapshot. When an auth test fails, open the trace and check the Network tab for 401s and unexpected 302 redirects. Check the Application tab for missing or expired cookies. This tells you more than a stack trace ever will.

###### Isolate Stale State

If an auth test fails and you suspect stale state from a previous test, run it with a clean slate:

```typescript
import { test } from "@playwright/test";

test.use({ storageState: { cookies: [], origins: [] } });

test("login from scratch", async ({ page }) => {
  await page.goto("/login");
  // If this passes but the normal test fails,
  // the problem is leftover state from your setup
});
```

This resets all cookies and storage, giving you a clean browser context. If the test passes from scratch but fails with the stored state, the problem is in your `storageState` file or the session it references.

###### Log Auth State at Each Step

For auth flows with multiple steps (login, redirect, token exchange, session creation), add logging so you can see where things diverge between local and CI:

```typescript
setup("authenticate", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill(process.env.TEST_USER_EMAIL);
  await page.getByLabel("Password").fill(process.env.TEST_USER_PASSWORD);
  await page.getByRole("button", { name: "Sign in" }).click();

  await page.waitForURL("/dashboard");

  const cookies = await page.context().cookies();
  console.log(`Auth cookies: ${cookies.map((c) => c.name).join(", ")}`);
  console.log(`Current URL: ${page.url()}`);

  await page.context().storageState({ path: authFile });
});
```

Failures that [appear only in CI](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci) almost always stem from environment differences: misconfigured API endpoints, missing OAuth credentials, timezone offsets that change token expiry, or cookie domain mismatches. These logs are where those differences become visible.

[Currents](https://currents.dev/) tracks auth failures across runs and distinguishes real flakiness from failures caused by state leakage, so you can spot recurring patterns without digging through CI logs manually.

##### Integration with CI/CD and Secrets Management

Auth tests that pass locally and fail in CI usually trace to credentials that exist on your machine but were never configured in the pipeline.

###### Secrets Injection

Sensitive values should never live in artifacts or source control. Use the built-in secret injection mechanisms: GitHub Actions encrypted secrets, GitLab CI masked variables, or the Jenkins Credentials Plugin. [HashiCorp Vault](https://developer.hashicorp.com/vault) strengthens this by issuing credentials only when a specific pipeline stage requests them and revoking them when that stage ends.

###### Dynamic Credential Creation

Create short-lived credentials at the start of a CI run and remove them at the end. Vault can issue a database password valid for one hour instead of relying on long-lived credentials stored in your CI platform. Credentials that exist only for the duration of a pipeline run have no exposure window between runs.

###### Handling Auth Expiry During Long CI Runs

Long CI runs (30+ minutes for large suites) can outlive your token TTLs. The auth setup succeeds, but tests later in the run fail with 401s because the session expired. Two approaches:

**Use `project.outputDir` for auto-cleanup.** Write auth files under `testProject.outputDir` instead of a fixed path. Playwright cleans this directory before each run, so every run starts with a fresh login. This is the simplest fix if your tokens are short-lived.

**Check token validity before use.** Build a worker-scoped fixture that validates the stored auth state at the start of each worker and re-authenticates if expired:

```typescript
// playwright/fixtures.ts
import { test as base } from "@playwright/test";
import fs from "fs";
import path from "path";

export const test = base.extend<{}, { validatedAuth: string }>({
  storageState: ({ validatedAuth }, use) => use(validatedAuth),

  validatedAuth: [\
    async ({ browser, playwright }, use) => {\
      const authFile = path.resolve(\
        test.info().project.outputDir,\
        ".auth/user.json",\
      );\
\
      if (fs.existsSync(authFile)) {\
        const context = await playwright.request.newContext({\
          storageState: authFile,\
        });\
        const check = await context.get("/api/me");\
        await context.dispose();\
\
        if (check.ok()) {\
          await use(authFile);\
          return;\
        }\
      }\
\
      const page = await browser.newPage({ storageState: undefined });\
      await page.goto("/login");\
      await page.getByLabel("Email").fill(process.env.TEST_USER_EMAIL);\
      await page.getByLabel("Password").fill(process.env.TEST_USER_PASSWORD);\
      await page.getByRole("button", { name: "Sign in" }).click();\
      await page.waitForURL("/dashboard");\
\
      const dir = path.dirname(authFile);\
      if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });\
      await page.context().storageState({ path: authFile });\
      await page.close();\
\
      await use(authFile);\
    },\
    { scope: "worker" },\
  ],
});
```

Credential rotation itself should be handled outside the test suite. Your tests consume credentials when provided. The rotation policies and expiration rules belong to the secrets management system.

##### Compliance and Security

Auth test infrastructure falls within the compliance boundary. Auditors don't separate production systems from test systems. A `storageState` file with a valid session token, committed to a repo and forgotten, is a reportable incident under SOC 2 and GDPR. [ISO 27002 Control 8.31](https://isms.online/iso-27002/control-8-31-separation-of-development-test-and-production-environments) explicitly requires separating development, test, and production environments.

The numbers make this concrete. The [2025 Verizon Data Breach Investigations Report](https://www.verizon.com/business/resources/reports/dbir/) found that GitLab tokens made up 50% of leaked CI/CD secrets across public repositories. The median time to remediate secrets leaked on GitHub was 94 days. If you handle sensitive data, your auth flows should align with [SOC 2](https://currents.dev/posts/currents-soc2-type2), ISO 27001, and GDPR.

###### Auditing Your Test Suite for Leaked Auth State

Knowing the rules (don't commit credentials, don't use production accounts in tests) isn't the hard part. The hard part is finding the violations that already exist. Run these checks on any suite that's been around for more than a few months.

**Scan git history for committed auth files.** Files deleted from HEAD still exist in the repo history. Check whether `storageState` files or auth directories were ever committed:

```bash
git log --all --diff-filter=A -- 'playwright/.auth/'
git log -p --all -S 'storageState' -- '*.json'
```

If you find matches, the credentials may still be valid. Rotate them, then clean the history with `git filter-repo` or `BFG Repo-Cleaner`.

**Verify `.gitignore` coverage.** Confirm that your auth directory is actually ignored, not just mentioned in documentation:

```bash
git check-ignore -v playwright/.auth/user.json
```

If there's no output, the path isn't ignored and any future `git add .` will commit it.

**Audit CI artifact retention.** Playwright traces contain full network request and response data, including auth cookies and tokens. If your CI pipeline uploads traces, screenshots, or HTML reports as artifacts with 30- or 90-day retention, you're storing credentials in your CI platform. Shorten retention to the minimum you need, or strip `Set-Cookie` and `Authorization` headers before upload.

**Check for production credentials in test environments.** List the environment variables your test suite consumes and confirm none of them work against production. A staging API key that also has production access is a common blind spot, especially in early-stage apps where environments share the same auth provider.

**Confirm auth files use `testProject.outputDir`.** Auth files written to a fixed path like `playwright/.auth/user.json` persist between runs unless you explicitly delete them. Files written under `testProject.outputDir` are cleaned automatically before each run. Check your setup files and fixtures for hardcoded paths.

##### Observability

A single failed login in a 200-test suite could signal a broken feature, a leftover session from an earlier test, an expired token, or a misconfigured CI environment. You need to distinguish real regressions from environmental noise, and that requires looking at patterns across runs.

The questions that matter: Are login errors appearing on the same tests each run, or shifting unpredictably? Do groups of tests fail together in ways that suggest shared auth state? Is login time gradually increasing across runs, indicating infrastructure degradation before it becomes a hard failure?

###### Structured Auth Logging

Add structured output to your auth setup so you can diff local runs against CI runs. Log the fields that actually matter for diagnosing auth failures:

```typescript
setup("authenticate", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill(process.env.TEST_USER_EMAIL);
  await page.getByLabel("Password").fill(process.env.TEST_USER_PASSWORD);
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL("/dashboard");

  const cookies = await page.context().cookies();
  const authCookies = cookies.filter((c) =>
    ["session", "token", "sid"].some((name) =>
      c.name.toLowerCase().includes(name),
    ),
  );

  console.log(
    JSON.stringify({
      event: "auth_setup_complete",
      url: page.url(),
      workerIndex: test.info().workerIndex,
      parallelIndex: test.info().parallelIndex,
      cookies: authCookies.map((c) => ({
        name: c.name,
        domain: c.domain,
        expires:
          c.expires > 0 ? new Date(c.expires * 1000).toISOString() : "session",
      })),
      timestamp: new Date().toISOString(),
    }),
  );

  await page.context().storageState({ path: authFile });
});
```

When a CI auth test fails, pull the logs for that run and compare the `cookies` and `url` fields against a passing local run. The differences are usually obvious: a missing cookie, a different domain, an expiry timestamp that's already in the past. Without these fields, you're guessing.

[Playwright's debug tools](https://playwright.dev/docs/debug) help with individual failures, but for patterns across runs, you need something that aggregates results over time. [Currents](https://docs.currents.dev/) tracks auth test behavior across executions and surfaces correlated failures without changing your existing test code. If three auth tests started failing together last Thursday, that's a state leakage problem, not three independent bugs.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![What Breaks When Your Test Suite Grows From 20 to 500 Tests](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-breaks-when-your-test-suite-grows-from-20-to-500-tests%2Fcover.png&w=3840&q=90)\\
\\
Apr 08, 2026 **What Breaks When Your Test Suite Grows From 20 to 500 Tests** \\
\\
![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)\\
\\
Currents Team](https://currents.dev/posts/what-breaks-when-your-test-suite-grows-from-20-to-500-tests) [![The Playwright Network Mocking Playbook](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fthe-playwright-network-mocking-playbook%2Fcover.png&w=3840&q=90)\\
\\
Apr 01, 2026 **The Playwright Network Mocking Playbook** \\
\\
![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)\\
\\
Currents Team](https://currents.dev/posts/the-playwright-network-mocking-playbook) [![What Is a Flaky Test in Software Testing, and How to Fix It](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-is-a-flaky-test-and-how-to-fix-it%2Fposter.png&w=3840&q=90)\\
\\
Oct 30, 2025 **What Is a Flaky Test in Software Testing, and How to Fix It** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it)

### 17. Currents.dev — Strategies for Playwright test agents

- Source: https://currents.dev/posts/9-strategies-to-get-the-most-out-of-playwright-test-agents
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)

Joshua Adeyemi

•Feb 13, 2026•

#### 9 Strategies to Get the Most Out of Playwright Test Agents

Learn how to get the most out of Playwright Test Agents and improve your test suite using new AI solutions.

![9 Strategies to Get the Most Out of Playwright Test Agents](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2F9-strategies-to-get-the-most-out-of-playwright-test-agents%2Fposter.jpg&w=3840&q=100)

AI is transforming how teams write and maintain tests. Instead of manually authoring every test case from scratch, developers now collaborate with AI agents that understand testing patterns and can generate, debug, and repair tests automatically. To maximize this shift, teams need to [equip their AI agents with testing expertise](https://currents.dev/posts/playwright-best-practices-skill)—and understand how to use built-in testing agents effectively.

Playwright Test Agents reduce the repetitive overhead of maintaining large test suites. As applications change, issues like locator drift, trial-and-error debugging, and manual test authoring slow teams down. Agents accelerate routine test creation, improve locator hygiene, and reduce time spent fixing broken tests.

Introduced in [Playwright version 1.56](https://playwright.dev/docs/release-notes#version-156) (October, 2025), agents include three components: **🎭** Planner, **🎭** Generator, and **🎭** Healer. Together, they shift test creation from manual scaffolding to guided, application-aware workflows.

But agents are not autonomous testers. They amplify the quality of the structure they are given. Clear fixtures, helpers, and conventions reinforce good practices, while poorly structured suites reproduce the same issues.

Teams get the most value when agents are used intentionally, within defined boundaries, and alongside solid engineering practices. This article presents nine strategies for using Playwright Test Agents effectively, highlighting real-world patterns of what works, what fails, and why.

##### Why Playwright Test Agents Are a Big Deal: The Real Capabilities (and Limits)

Understanding what each agent does reveals where they add value and where they do not.

**The 🎭 Planner** interprets intended actions based on the current page and application state. Given a goal, such as testing a guest checkout flow, it explores the UI and generates a structured, step-by-step plan. This helps teams navigate complex or unfamiliar interfaces, where identifying the correct sequence of interactions can otherwise take considerable time.

**The 🎭 Generator** converts these plans into executable Playwright test code. It follows patterns from your seed files, including fixtures, helpers, and setup logic, and verifies that selectors exist in the running application while generating code. This produces a test structure that matches team conventions rather than generating tests that don’t follow established patterns.

**The 🎭 Healer** focuses on keeping tests up-to-date. When a test fails, it replays the steps, checks the current UI, and suggests fixes for broken interactions. For example, a fragile text-based selector might be replaced with a more reliable role- or attribute-based locator. You can see a demonstration of these agents in action below:

Playwright v1.56: From MCP to Playwright Agents - YouTube

Tap to unmute

[Playwright v1.56: From MCP to Playwright Agents](https://www.youtube.com/watch?v=_AifxZGxwuk) [Playwright](https://www.youtube-nocookie.com/channel/UC46Zj8pDH5tDosqm1gd7WTg)

Playwright35.6K subscribers

[Watch on](https://www.youtube.com/watch?v=_AifxZGxwuk)

By checking selectors during generation and repair, teams catch flaky locators and timing issues before they cause CI failures. This reduces false negatives and unnecessary reruns, making test suites more dependable.

Agents do have limitations. They cannot understand business logic or define test oracles. They can interact with a “Submit Order” button, but they cannot determine whether the correct backend side effects occurred unless those checks are explicitly defined. Agents also cannot reason about complex stateful workflows involving backend setup or domain rules.

Finally, agents cannot fix unstable environments or replace proper test architecture. Timing drift, async rendering issues, and poor test structure are inherited, not solved. Agents improve the quality of the patterns they are given, reinforcing good practices or reproducing existing problems. To get consistent value, teams need clear structure and guidance around how agents are used.

##### 9 Strategies to Get the Most Out of Playwright Test Agents

Understanding what agents can do doesn’t automatically mean they’ll be used effectively. Without proper guidance, generated tests can become harder to maintain. Using agents well requires planning and structured workflows. These nine strategies show how teams get consistent results.

###### 1\. Establish Strong Test Architecture Before Letting Agents Write Code

Agents copy what they see. They pick up on existing structure, naming, and patterns in a test suite. If the suite is inconsistent or loosely organized, agent-generated tests often repeat those same problems. When patterns are clear and predictable, the generated output is usually easier to maintain.

Strong architecture gives agents clearer signals to follow. This starts with clean, reusable fixture patterns that consistently handle standard setup. Authentication works best when implemented once through a deterministic login helper rather than repeated across individual tests. Repeated interactions benefit from page objects or component-level abstractions, and stable data states help reduce brittle assumptions.

The seed file is especially important because it acts as the primary reference that agents learn from. If the seed demonstrates proper fixture usage, agents tend to generate tests that rely on fixtures. If it shows a stable data setup and resilient locators, those patterns are more likely to carry forward.

Example of a solid seed file:

```typescript
import { test, expect } from "./fixtures";

test("seed - basic navigation", async ({ page, authenticatedUser }) => {
  await page.goto("/dashboard");
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
});
```

This seed uses a custom authentication fixture, navigates to a known state, and validates the UI with role-based locators. Tests generated from this example tend to follow the same structure and conventions.

Contrast with a weaker seed:

```typescript
import { test, expect } from "@playwright/test";

test("seed", async ({ page }) => {
  await page.goto("https://staging.example.com/login");
  await page.locator("#username").fill("admin@test.com");
  await page.locator("#password").fill("password123");
  await page.locator('button[type="submit"]').click();
  await page.waitForURL("**/dashboard");
});
```

This seed hardcodes URLs, uses fragile selectors, and handles authentication directly in the test. Agents generated from this example tend to repeat these patterns across the suite.

###### 2\. Define Locator Strategy and Selector Hygiene Early

Locator failures account for a significant portion of test maintenance time. When a CSS class changes or an ID is removed, tests can fail. Teams may spend considerable time updating selectors that could have been more stable.

Agents can replicate fragile selectors if conventions are not defined. Without clear rules, agents may choose text selectors, nth-child patterns, or long CSS chains that work initially but can fail after minor UI changes.

Define your strategy before generating tests. Establish a hierarchy of preferred selectors. Playwright recommends using role-based selectors first, then test IDs, and finally other semantic selectors. CSS and XPath should generally be used only as a last resort.

A commonly used order looks like this:

- Role-based selectors: `getByRole('button', { name: 'Submit' })`
- Test ID selectors: `getByTestId('checkout-button')`
- Label-based selectors: `getByLabel('Email address')`
- Text selectors (for specific content): `getByText('Welcome back')`
- CSS selectors: only for unique, stable attributes

Avoid:

- nth-child or positional selectors
- Long CSS chains tied to the DOM structure
- Selectors relying on temporary classes
- Text matches on dynamic content

Document your selector conventions clearly. Include guidance in pull request templates and review agents-generated test against these conventions. Treat the seed file as the reference for locator strategy, since agents learn by copying what they see. If the seed consistently uses `getByRole()`, `getByLabel()`, and `getByTestId()`, and avoids raw CSS or nth-child patterns, new code will follow that pattern. Consistent locators reduce maintenance and improve test reliability.

###### 3\. Build a Human-in-the-Loop Review Cycle for Agent Suggestions

Agents behave like junior engineers. They write working code fast, but they don’t understand domain rules or recognize anti-patterns. Their output requires review before merging.

The problem arises when teams treat agent output as final. A test is generated, passes, and is committed, but over time it can add maintenance costs if it doesn’t follow team standards or validate the correct behavior.

Code review addresses this. Every agent-generated test should follow the same review process as human-written code, including pull requests, feedback, and iteration.

During review, check the following:

- **Architecture alignment:** Does the test follow your patterns, use the correct fixtures, and align with your page object approach? If it bypasses helpers or repeats existing functionality, revise it.
- **Selector quality:** Are locators stable and consistent with your defined hierarchy? Avoid nth-child selectors, complex CSS chains, or text matches on dynamic content.
- **Test logic:** Does the test validate the intended behavior? Agents cannot interpret business rules, so reviewers should confirm that the elements checked demonstrate the correct functionality.
- **Scope creep:** Does the test cover unrelated functionality? A login test should not also validate navigation, permissions, or profile display. Keep tests focused.
- **Silent drift:** Watch for new patterns introduced by agents. If a generated test creates its own setup instead of using existing fixtures, it can fragment the codebase.

Establish clear review criteria. Provide a checklist for agent-generated tests and guide reviewers to spot common issues.

**Example review comment:** _"This test uses page.locator('.submit-btn'), but we have a standard submitButton() method in BasePage. Please refactor to use the existing method."_

Review feedback creates a loop that improves agent accuracy over time. When patterns are rejected consistently, update your rules, skills, or seed examples to prevent similar issues. Add enforcement through tooling: lint rules that ban raw CSS locators, CI checks that require Page Object Methods and directory conventions that keeps tests organized. The agent becomes more reliable when feedback turns into explicit guidance and automated constraints, not repeated rejections.

###### 4\. Use Agents as a Guided Onboarding Tool for New Contributors

Junior engineers and new team members face a learning curve with test automation. They need to understand your patterns, learn conventions, and get familiar with the domain, which can take several weeks.

Agents help reduce that timeline by showing patterns through generated examples. A junior engineer can watch the Generator create a test using proper fixtures and role-based locators, seeing the correct structure without reading lengthy documentation.

Here’s how agents can support onboarding effectively:

- **Pattern demonstration:** New engineers see working examples right away. Instead of reading instructions like "use fixtures for authentication," they observe a generated test that imports fixtures correctly and follows the expected structure.

- **Convention reinforcement:** Agents follow the patterns in your seed file. When new team members use agents, they receive feedback on what a good structure looks like. Agents generally do not generate tests that violate established patterns if the seed file is well-formed.

- **Reduced documentation burden:** You do not need long testing guides. A good seed file and agent examples can teach more effectively than documentation alone.

- **Lower-stakes practice:** Junior engineers can safely experiment with agents. They can generate tests, review them, and adjust parameters, learning without affecting production tests.

- **Structured onboarding example:** A SaaS company onboarded new QA engineers using agents:
  - **Day one:** Seed file review and agent setup.
  - **Day two:** Use the Planner to explore the application and understand user flows.
  - **Day three:** Use the Generator to create tests and submit them for review.
  - By day five, new engineers can produce maintainable tests.

Teams should not use agents solely for speed. Pair junior engineers with agents, let them generate tests, review output, and understand why specific patterns exist. Knowledge transfer happens faster when examples are concrete and immediate.

###### 5\. Integrate Agents Across Your Daily Workflow, Not Just in Isolation

Teams often limit the value of agents when they treat them as short trials. They generate tests during a sprint, review the results, and decide whether to continue. This approach can overlook the longer-term benefits agents provide when embedded in everyday development work.

Agents are most effective when integrated into regular workflows. Their usefulness grows when applied throughout a feature’s lifecycle, not just in a single testing phase.

In practice, this involves using agents at multiple points in daily workflows:

**Local development**: Developers working on new features can use the Planner to explore work-in-progress UIs. The Planner highlights interaction paths and edge cases that might otherwise be missed. Developers can review a structured test plan that better reflects the feature.

**Pull request reviews**: During reviews, agents can help validate UI changes. The Generator can generate tests for new components or updated flows, allowing reviewers to assess test coverage alongside code changes rather than waiting for a separate QA cycle.

**Debugging failures**: When tests fail in CI, the Healer can help investigate. It replays failing steps and identifies whether issues are caused by product changes or outdated selectors, reducing time spent on initial triage.

**Trace analysis**: Agents can examine trace artifacts when failures occur. By reviewing screenshots and execution steps, they can suggest likely causes and possible fixes, helping teams move from failure detection to resolution more efficiently.

**Iterative test building**: The Planner can also be used during feature design. Teams can outline test coverage before implementation, use those plans to guide development, and update them as features change, regenerating tests when needed.

Used this way, agents become part of the team’s regular development process, supporting consistent test coverage instead of being applied only after issues arise.

###### 6\. Create Boundaries: Know When Not to Use Agents

Agents are effective for interaction-heavy testing, but they are not suited to every type of validation. Teams lose efficiency when agents are applied to problems that require domain understanding or controlled backend state.

Agents are typically less suitable in the following cases:

**Domain-logic heavy tests:** Tests that validate complex business rules require knowledge that agents do not have. An agent can navigate a pricing or tax flow, but it cannot determine whether the calculations comply with regulations or business policy. These tests are better designed and validated by domain experts.

**Multi-step, stateful workflows:** Flows that depend on specific backend state, seeded data, or coordinated service behavior exceed what agents can reason about. Agents interact with the UI but cannot verify whether databases, background jobs, or dependent services are in the correct state.

**Unstable environments:** In staging or preview environments with timing issues, partial rendering, or inconsistent data, agents inherit the same instability. While selector healing can help with UI changes, it cannot compensate for unreliable environments.

**Backend orchestration and mocking:** Tests that require database seeding, service mocks, or custom backend configuration still need explicit human setup. Agents can run steps once those conditions exist, but they do not define backend prerequisites or orchestration logic.

**Here's how these limitations show up in practice**: In insurance pricing workflows, agents can generate tests that navigate forms and submit inputs, but they cannot verify whether calculated premiums align with actuarial rules. These tests may pass while still missing incorrect pricing behavior.

Teams often address this by keeping a pricing validation manual and using agents primarily for navigation and interaction coverage. This helps clarify where agent-generated tests add value and where human validation is still required.

###### 7\. Provide Clean, Deterministic Data States

Agents rely on consistent environments. They explore applications, verify locators, and generate tests against running interfaces. If the application behaves unpredictably, agent-generated tests may be unreliable.

Agents are affected by:

- **Inconsistent test data:** Differences in user permissions, profile states, or seeded data between runs can cause tests to fail unexpectedly.
- **Slow or partial rendering:** Applications that render slowly or inconsistently can confuse agents, leading to unreliable test capture.
- **Unstable environments:** Frequent restarts, brief service outages, or overlapping deployments in staging can introduce timing issues into tests.

Ways to keep data and environments consistent:

- **Factory endpoints:** Use API endpoints to create test data on demand and start agent runs from known states.
- **Data isolation:** Avoid shared staging data. Use test-specific datasets so each run starts from the same baseline.
- **Pre-seed before agent runs:** Execute setup scripts to clear old data and seed new records.
- **Disable animations / consistent UI timings:** Turn off UI transitions or ensure predictable rendering so agents capture stable elements.

###### 8\. Choose the Right LLM Model and Limit Agent Scope

LLM Model choice affects how reliably agents follow your conventions. Stronger models tend to respect Rules, Skills, and repository structure more consistently. Weaker or lower-cost models often drift from established patterns, such as generating inline selectors instead of Page Objects, skipping fixtures, or placing files incorrectly, which can lead to brittle code. Teams looking for predictable, maintainable output should consider LLM model selection early.

**Test them directly:** Run the same prompts across different models and compare results. Check whether generated tests follow your [Page Object Model](https://playwright.dev/docs/pom), use the correct fixtures, respect naming conventions, and include proper assertions. Quality varies significantly between models.

**Our 🔥 take**: Free / low-cost models generates more AI slop than anything, it's not worth it. We did some testing and our winners were Opus 4.6 and Codex 5.3. Only after switching to them, we got to see real results and how useful the agents can be.

Even with capable models, clear boundaries help limit unintended changes. Agents can generate and modify code, and without defined limits, they may change more than intended. A selector fix can affect logic, or a locator update can reshape test structure.

**Limit Agent Scope**

The problem: Teams sometimes give agents unrestricted scope. The Healer may try to fix a failing test and modify more than necessary. The Generator might create tests in the wrong directories or overwrite existing files.

To avoid this, set clear boundaries:

- **Directory restrictions:** Restrict where agents can write code. If your tests live in `tests/`, configure agents to generate files only in that directory. Exclude fixtures, helpers, and configuration files from agent edits.
- **Confirmation for rewrites:** Require human review before agents modify existing tests. The Healer can propose fixes, but engineers should review and approve changes before they are applied.
- **Prevent architecture shifts:** Configure agents to follow existing patterns, not introduce new ones. For example, if your suite uses the Page Object Model, agents should not generate tests with inline selectors. If you use fixtures, agents should not bypass them.
- **Limit modification scope:** When the Healer fixes a failing test, it should change only the failing locator, not refactor surrounding code. Scope each fix to the minimum change required to restore functionality.

**Example configuration:**

- Agents write only to `tests/generated/`.
- Agents cannot modify files in `tests/fixtures/` or `tests/pages/`.
- Locator changes require pull request approval.
- Generated tests must import from `./fixtures`, not `@playwright/test`.

Some teams only notice the need for boundaries after agents make broader changes than intended, such as modifying shared authentication logic while fixing a selector. Requiring agents to suggest changes rather than apply them directly helps prevent this. With the right model and clear limits in place, teams still benefit from faster test generation and safer maintenance.

###### 9\. Equip Your Agent with AI Skills

AI agents are only as effective as the knowledge they have access to. Generic AI assistance often falls short when it comes to testing—agents rely on outdated patterns, miss framework-specific nuances, and produce tests that are flaky by design. To get consistent, high-quality output, you need to provide agents with specialized expertise.

This is where **Agent Skills** come in. [Skills](https://agentskills.io/home) are a new open standard created by Anthropic for providing expertise to agents without bloating the context window. They're now available in all major AI development tools, including Claude Code, Cursor, VS Code, and Google Gemini.

**Why skills matter for Playwright testing:**

- **Framework-specific guidance:** Generic AI knows about testing in general, but may not know the latest Playwright APIs, auto-waiting behavior, or recommended locator strategies.
- **Consistent patterns:** Skills encode your preferred patterns—Page Object Model, fixture usage, assertion styles—so agents generate code that matches team conventions.
- **Reduced drift:** Without explicit guidance, agents often introduce variations or outdated practices. Skills keep generated code aligned with current best practices.
- **Focused context:** Instead of loading entire documentation into prompts, skills provide precisely the relevant knowledge when needed.

We released the **[Playwright Best Practices Skill](https://currents.dev/posts/playwright-best-practices-skill)** specifically for this purpose. It gives AI agents specialized guidance for writing, debugging, and maintaining Playwright tests in TypeScript, covering everything from locators and assertions to CI/CD configuration and advanced patterns like multi-user testing and GraphQL mocking.

Install the skill and start generating better Playwright tests:

```bash
npx skills add https://github.com/currents-dev/playwright-best-practices-skill
```

Once installed, the AI automatically uses the skill when your questions or tasks involve Playwright—no manual configuration required. Instead of hoping the agent knows current best practices, you give it explicit, up-to-date knowledge that produces maintainable tests from the start.

Teams that combine proper skills with the strategies in this article see compounding benefits: agents follow both team conventions (from seed files and architecture) and framework best practices (from skills), resulting in tests that require less review and fewer corrections.

##### What Happens When Teams Use Agents the Right Way

Teams applying these strategies often see improvements. Test creation becomes faster, maintenance effort is reduced, and coverage increases over the same period. Metrics help track changes, but the most noticeable difference shows up in daily work.

**Time allocation shifts.** Engineers spend less time fixing selectors. QA teams write less repetitive setup code. Senior engineers review fewer test pull requests. Freed time is spent on exploratory testing, risk analysis, architecture planning, and edge-case exploration.

**Locator stability improves early.** Agents verify selectors against running applications during generation. Flake rates decrease as role-based locators become standard and fragile patterns are avoided.

**Consistency grows across teams.** Generated tests follow seed files, which keep fixture and page object usage aligned. New engineers see examples that match team conventions, making onboarding faster.

**Timing issues surface sooner.** The Generator executes flows during test creation, catching race conditions, async rendering issues, or slow API calls before they reach CI. In one case, a checkout flow issue was identified and fixed before release.

**Reviewers face less cognitive load.** They focus on logic and coverage rather than checking selectors. Some teams report that senior engineers now spend fewer hours reviewing tests, freeing time for higher-level planning.

**Agents become part of daily workflows.** Within weeks, developers, QA engineers, and the Healer use them regularly. Confidence in agents grows, and teams review generated code with a mindset of verification rather than doubt. Reviews move faster, coverage expands, and teams deliver features more smoothly.

The key takeaway: strong teams use agents to handle routine work, not to replace judgment. Agents reduce repetitive effort, allowing engineers to focus on tasks that need context and experience.

##### Moving Forward

Playwright Test Agents won't fix broken test architectures or remove the need for solid engineering practices. They amplify whatever foundation you build, for better or worse. Teams seeing results use agents intentionally. They define clear patterns, selector strategies, and review processes that treat agent output like junior engineer code, preventing architectural drift.

Testing at scale requires orchestration beyond agent capabilities. You need test distribution, failure management, and CI optimization. Agents don’t fix broken foundations. They multiply what you already have. If your test suite has a solid structure, agents accelerate everything. If it doesn’t, fix that first.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![Playwright MCP: What It Is, How It Works, and When It’s Worth Using](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-mcp%2Fcover.png&w=3840&q=90)\\
\\
Feb 06, 2026 **Playwright MCP: What It Is, How It Works, and When It’s Worth Using** \\
\\
![Dumebi Okolo](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Dumebi Okolo](https://currents.dev/posts/playwright-mcp) [![Introducing Currents MCP Server](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fcurrents-mcp%2Fposter.jpg&w=3840&q=90)\\
\\
Apr 15, 2025 **Introducing Currents MCP Server** \\
\\
![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)\\
\\
Currents Team](https://currents.dev/posts/currents-mcp) [![AI Skill: Playwright Best Practices](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-best-practices-skill%2Fposter.jpg&w=3840&q=90)\\
\\
Feb 03, 2026 **AI Skill: Playwright Best Practices** \\
\\
![Walter Galvão](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fwalter-galvao.png&w=64&q=75)\\
\\
Walter Galvão](https://currents.dev/posts/playwright-best-practices-skill)

### 18. Currents.dev — Tests that survive UI refactors

- Source: https://currents.dev/posts/designing-playwright-tests-that-survive-ui-refactors
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)

Currents Team

•May 04, 2026•

#### Designing Playwright Tests That Survive UI Refactors

Your UI refactor didn't break the app, it broke your tests. Learn how semantic Playwright test design decouples your suite from implementation details for good.

![Designing Playwright Tests That Survive UI Refactors](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fdesigning-playwright-tests-that-survive-ui-refactors%2Fcover.png&w=3840&q=100)

A design system migration ships with the interface looking correct, components rendering as expected, and user flows behaving as intended. Then CI runs and three dozen Playwright tests go red.

None of these failures reveal a broken feature. They expose a more costly problem: a test suite quietly coupled to implementation details all along. The application behaved identically, yet CSS class names and DOM nodes had already changed. The tests missed it entirely because they were never observing actual user behavior.

Selectors like `div.btn-primary-new-style` or `nth-child(3)` test how the UI was built at a specific moment, not what users actually experience. When that moment changes, the tests break, and every hour spent chasing selectors is an hour not spent on [meaningful coverage](https://currents.dev/posts/how-to-measure-code-coverage-in-playwright-tests). Selector choice alone does not resolve the underlying problem.

The fix is designing tests at an abstraction level that stays stable when implementation details change. This article covers the patterns that make a test suite resilient through design system migrations, component library upgrades, and UI refactors.

##### Understanding why tests break during refactors

Refactors that preserve application behavior but break test suites are not just a selector problem. They are a design problem, and the two require different solutions: selector choice (which locator strategy you use) and abstraction layer design (how interaction logic is organized across your test suite). Getting the fix right starts with knowing which type of failure is actually happening. Most engineers treat these failures as isolated incidents and apply quick workarounds. The pattern repeats because the underlying cause is never resolved.

At [Google](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html), 84% of transitions from pass to fail in their test infrastructure involve a [flaky test](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it). While flakiness introduces randomness into test outcomes, it is only one category of failure. Implementation-coupled tests add a separate layer to that cost. They fail consistently after updates to the UI structure, producing failures that appear like product issues but trace back to selector design.

Many of these failures result not from broken features, but from tests monitoring the wrong behaviors. What follows is a breakdown of the five most common coupling patterns that make Playwright suites fragile under refactors. Each one shows a clear failure signature and carries a measurable cost.

###### Selector coupling to visual structure

Structural selectors encode the DOM hierarchy at a specific point in time. A selector like `div > section > form > input` describes how the HTML was arranged when the test was written, not what the user actually sees or does.

Refactors often reorganize that arrangement, with forms moving into modals, components gaining new layout containers, or child components being extracted for reuse. The underlying behavior remains stable, and the selector no longer reflects the current structure. The test fails even though the product continues to work as expected because the path the test relied on has changed.

The DOM changes frequently, and tests that depend on structure will break when the page is reorganized. What remains stable is what users interact with: [roles, labels, and accessible names](https://playwright.dev/docs/locators). These convey an element's purpose rather than its position in the HTML.

###### Selector coupling to styling artifacts

CSS class names generated by component libraries are build artifacts, for example `.MuiButton-containedPrimary`, `.css-1x2y3z`, or `._button_a3f2k_1`. These strings are outputs of the build process, not stable identifiers. When a library version is bumped or a build configuration changes, class names can be rewritten entirely. The behavior remains unchanged, and the selector binding is lost.

Tests that rely on auto-generated CSS classes or XPath expressions tied to DOM structure break whenever developers refactor the UI. In component-library-heavy codebases, that bar for breakage is low. A [Material UI upgrade from v4 to v5](https://mui.com/material-ui/migration/migration-v4/) rewrote the class naming scheme entirely, replacing stable names with generated hashes. A CSS Modules configuration change rotates the hash suffix. In both cases, the interface remains the same from the user's perspective.

In a Tailwind migration, this pattern becomes more pronounced as a CSS-in-JS conversion replaces one or two class names with a dozen utility classes. Every one of them differs from what was there before, and any test using class-based selection against that component fails. The behavior remains unchanged, but the class list no longer matches anything the test expects.

###### Coupling to component internals

Some tests reach into component internals: internal element IDs generated by the library, assumptions about specific HTML element types, or XPath expressions that target a fixed DOM path inside a component.

For example, a test targeting `input[type=text]` will fail as soon as a component library replaces its native input with a custom implementation. This pattern shows up repeatedly when adopting headless UI libraries and design systems with more advanced accessibility features. While users see the same behavior, the underlying HTML element changes, causing the test to fail.

Component libraries often introduce wrapper nodes for layout or accessibility, breaking deeply chained selectors. A test targeting the library's internal structure examines the library's implementation decisions. These internal details can change with any minor version, causing the test to fail even if the application behaves correctly.

Playwright's locators pierce open shadow DOM by default, so `getByRole`, `getByText`, and the other semantic locators work inside shadow roots without any extra configuration. The fragility comes from using XPath inside shadow DOM (XPath does not pierce shadow roots) or targeting closed-mode shadow roots, which Playwright does not support. Semantic locators remain stable across shadow DOM boundaries; structural ones do not.

This pattern is most damaging in authentication flows, where engineers rely on internal identifiers for password inputs, OTP fields, and modal containers because the component library does not expose clean semantic attributes. These tests break during design system upgrades, even when authentication works perfectly, because the component's internal structure has shifted.

###### Implicit coupling via position and order

Index-based selectors and `:nth-child` expressions rely on elements appearing in a fixed order, which breaks the moment a refactor reorganizes the layout.

When a navigation item is added above an existing one, every index that follows shifts. A form field reordered for UX reasons breaks any test that targets the third field by position, and a data table updated to highlight more relevant columns invalidates selectors tied to column index. None of these changes affect how the application behaves, yet they cause position-based tests to fail.

Avoiding positional selectors like `nth(3)` is well-established practice. The problem goes beyond fragility: these selectors fail without explaining what behavior they were meant to verify. The failure message reflects only a position change, giving the debugging engineer little insight into what actually went wrong.

###### Text content coupling

Tests that select by exact text content tie test behavior to content decisions. A copy update, an i18n migration, or a content management update breaks the selector, even though the application continues to work as expected.

Text selectors are not inherently flawed. In [Playwright](https://playwright.dev/docs/locators?#locate-by-text), `getByText()` is a first-class locator designed to reflect how users interact with the interface. When the text itself is part of the user-facing contract, such as headings, legal disclaimers, or visible status messages, asserting on text is appropriate and necessary.

The problem emerges when text is used as a stable identifier for elements whose content is expected to change. In those cases, the test is no longer validating behavior, but mirroring implementation details that evolve independently of functionality. Text-coupled tests break when products introduce new locales. The application remains correct across languages, but the tests no longer match the updated content.

Partial text matching improves resilience but does not remove the coupling. A test using `getByText(/sign in/i)` survives capitalization changes but still breaks when the product team renames the action from "Sign in" to "Log in," even though nothing about the underlying behavior has changed.

A [survey of 335 professional software developers and testers](https://arxiv.org/abs/2203.00483) across different domains found that the primary cost of unreliable tests is not the computational overhead of re-runs, but the gradual loss of confidence in test results. Text-based selectors accelerate that problem by producing failures that engineers learn to ignore. Copy changes, the test flags it, someone updates the string. Over time, failures lose meaning, and real issues become harder to spot.

##### The selector hierarchy: what to use and when

Not all selectors age equally. Some survive a design system migration without a single change, while others fail the moment a developer renames a utility class or upgrades a component library. What separates them is what each selector is anchored to.

User-facing attributes and explicit contracts produce more stable tests because they tie directly to what the application intentionally exposes. This creates a clear selector hierarchy. Where a selector sits in that hierarchy determines whether a suite needs constant maintenance or mostly holds up on its own.

###### Tier 1: ARIA role and accessible name

`page.getByRole('button', { name: 'Add to cart' })` sits at the top because it queries based on semantic role and accessible name, not DOM structure alone. Role-based selectors survive layout changes and class updates because well-built design systems often preserve semantic roles by default. A refactor that converts a `<div role="button">` to a native `<button>` does not break this selector, and neither does a Tailwind migration that rewrites every class name on the component.

There is a second benefit. When a component breaks `getByRole`, it also breaks its accessibility contract, so the locator doubles as a built-in accessibility check. A login button that no longer exposes its role to assistive technology represents a functional regression, even if the UI still appears correct.

For elements with dynamic accessible names such as order numbers or user-specific text, regex matchers handle the variation cleanly: `getByRole('heading', { name: /order #\d+/i })`.

###### Tier 2: Label and placeholder

`page.getByLabel('Email address')` works by binding to the form label association, using `for/id`, `aria-labelledby`, and `aria-label` to find the right element. Label-based locators remain stable across input type changes, component library updates, and layout refactors, as long as the label text and its association stay intact.

`getByPlaceholder` serves as a fallback for unlabelled inputs, but introduces more fragility. Placeholder text changes frequently for UX reasons and carries less semantic weight than a label in the accessibility tree.

###### Tier 3: Explicit test attributes

`data-testid`, or a team-defined equivalent such as `data-cy` or `data-test`, defines a clear contract between the application and the test suite. The attribute exists solely for testing and remains stable through visual and structural refactors. It works best when text or role-based selectors are likely to change.

Playwright has first-class support for this via `page.getByTestId()`. The attribute name is configurable in `playwright.config.ts` via `use.testIdAttribute`, so you can standardize on whatever convention your codebase already uses.

Used selectively on meaningful elements, `data-testid` is a net positive. It introduces a stable contract between the application and the test suite without coupling tests to implementation details.

However, it comes with a trade-off: it introduces test-specific markup into production HTML. This is addressable. Tools like `babel-plugin-react-remove-properties` strip `data-testid` attributes at build time, so the markup never ships to production. Without a consistent naming policy (enforced through a custom ESLint rule or a CI check that validates attribute presence on critical components) it creates inconsistency across the codebase. Applying it selectively to meaningful UI elements such as interactive components, key containers, and state indicators keeps it effective. Annotating every DOM node creates false confidence rather than stability.

###### Tier 4: Text content (with caveats)

`page.getByText()` sits at the bottom of the hierarchy and is best reserved for content that has no semantic role, label, or test attribute. Passing a regex like `/submit order/i` handles case variations without requiring an exact string match.

`getByText()` is sensitive to content updates, so i18n migrations, copy edits, and CMS-driven changes can all break the locator even when the application behaves the same. When failures show up repeatedly in this tier, it points to a missing `data-testid` rather than a problem with the feature itself.

###### What never to use

CSS classes generated by component libraries such as `.MuiButton-containedPrimary` or `.css-1x2y3z` reflect build output, not user-facing behavior. They change frequently during design system updates, which makes them unreliable selectors.

Deeply chained CSS selectors are equally fragile. A small layout change or an extra wrapper can break the entire path without affecting how the feature works. Component libraries introduce these changes without warning.

XPath with positional predicates, `nth-child` without a filtering strategy, and dynamically generated `id` attributes create the same kind of risk. They depend on structure and ordering that do not stay stable, leading to failures that reveal more about the selector than the application.

##### Scoping and chaining locators

A good selector strategy ensures the test targets the right element. A good scoping strategy keeps it there as the page changes. The two are related but distinct, and solving only one leaves a common failure unaddressed.

Problems start when a page contains multiple elements that match the same locator. A page with two "Confirm" buttons, one in a form and one in a modal, becomes ambiguous if the test does not narrow the scope. Playwright enforces strict mode by default, meaning any locator action that matches more than one element will throw an error and fail the test immediately. This is not just a best practice concern, it is a framework constraint.

A common workaround is using an index such as `.nth(0)` or `.nth(1)`. That relies on position and breaks as soon as the layout changes. The more reliable path is semantic scoping.

###### Container scoping

`page.getByRole('dialog').getByRole('button', { name: 'Confirm' })` anchors the locator to a semantic container, keeping the test focused on what the user sees rather than how the DOM is arranged.

Locators should reflect how a user navigates the interface, not trace a CSS path through the DOM. "In the confirmation dialog, click the Confirm button" remains stable through layout refactors, while "the third button in the second `div` inside the modal wrapper" depends on structure that shifts with any layout change. Moving the dialog to a different position in the DOM breaks the second selector, while the first stays intact.

###### Filtering locators

`locator.filter({ hasText: /.../ })` and `locator.filter({ has: page.getByRole(...) })` solve the problem of multiple matching elements by narrowing the result set based on content or children rather than position.

The table row pattern illustrates this: `page.getByRole('row').filter({ has: page.getByText('Order #12345') })` targets the correct row by its content, not its position, so it stays accurate when data is reordered or generated dynamically. Index-based row selection breaks in both cases. In authentication flows that render user-specific data in tables such as session lists and device management panels, this pattern separates stable tests from ones that fail whenever the data order changes.

###### Avoiding locator chains that re-encode structure

Chaining selectors through the DOM hierarchy does not add stability. It moves the structural coupling further down the chain where it is harder to spot.

`page.locator('.container').locator('.form').locator('.input')` is still a structural selector, just written across three lines. It adds no stability over a single selector and only hides the coupling.

Each step in a chain should narrow the target, such as scoping from a named dialog to a labeled button. Moving from a wrapper class to a child class to a grandchild class only mirrors the layout, which changes frequently. The first approach holds up when the UI changes; the second depends on structure that doesn't.

##### Page object design for refactor resilience

Centralizing selectors and interactions into a dedicated abstraction layer (whether page objects, app actions, or domain helpers) keeps changes contained and prevents failures from spreading across the test suite.

Selectors alone do not prevent test breakage. You can have role-based locators, semantic scoping, and `data-testid` attributes, yet still see failures across multiple files when a single component changes. The root of the problem lies in how tests are organized.

Page objects are one approach to this. Playwright's own documentation increasingly highlights the **app actions** pattern as an alternative: rather than wrapping a page into a class, you attach high-level helper functions directly to Playwright fixtures. App actions work well for cross-cutting flows that don't map cleanly to a single page, while page objects remain a natural fit when you're modeling a specific UI surface with its own set of interactions. In both cases, the principle is the same: keep interaction logic out of individual tests.

When selectors are scattered throughout individual tests, even a small UI change can trigger updates across several files. Centralizing them in a dedicated layer keeps those changes contained.

###### Centralizing selectors in page objects

Page objects group selectors in one place and make them reusable across tests. The idea is simple, but it often breaks down in real codebases. A common failure pattern is duplication, where the same `getByRole('button', { name: 'Sign in' })` call appears across many test files. When a product team changes the action from "Sign in" to "Log in," the functionality remains the same, but multiple tests fail at once. Fixing it means updating every occurrence, and every file touched is a merge conflict waiting to happen.

A page object should reflect what a user can do and observe, while keeping implementation details out of the tests. Tests interact with methods, not raw locators. The selector lives once, in one place, and that is the only file that needs to change when the UI does.

###### Page objects that expose behavior, not selectors

A page object property that returns a locator, such as `get addToCartButton()`, is only a small step up from placing the selector directly in the test. It centralizes the locator, but the test still depends on how the UI is built.

A method like `addItemToCart(sku: string)` reflects what the user is trying to do. Locators and interaction details stay inside the page object, so when the UI changes, updates happen in one place. For example, if the "add to cart" flow introduces a confirmation modal after a design system update, that change is handled inside the page object. The tests continue to call the same method without needing updates.

###### Component-level page objects for design system migrations

Page-level page objects work well for stable applications, but design system updates demand more precision. Building reusable page objects around user behavior instead of UI structure keeps test suites stable as the application changes.

A `DatePickerComponent` page object handles every interaction with the date picker, no matter which page it appears on. When the design system upgrades the component, only `DatePickerComponent` needs updating. This moves the maintenance focus from the test suite to the page object library, maintained by the team closest to the changes.

We've seen this compound over time: each component-level page object you build is a file you do not have to touch during the next upgrade.

##### Coordinating with frontend teams

Test resilience is not owned by a single team. When a component PR changes internal structure without checking how tests depend on it, failures follow. Poor communication between developers and testers remains one of the most common sources of quality issues in engineering teams. Addressing it requires changes in [how teams work together](https://currents.dev/posts/how-to-build-reliable-playwright-tests-a-cultural-approach), not just more discussion.

###### Data-testid as a shared contract

Treating `data-testid` as part of a component's public API changes how you work with it. It moves from an informal convention to something documented and expected. When a component ships with `data-testid="checkout-submit"`, that attribute becomes part of its interface and should be reviewed in every PR. Renaming or removing it without considering the test suite introduces avoidable breakage.

Consistency comes from enforcement. A custom ESLint rule or a CI check that validates attribute presence on critical components turns that expectation into something you can rely on. Without that discipline, test maintenance starts to dominate. Engineering time that should go toward new coverage goes to fixing broken selectors instead.

###### Pre-refactor test audits

Before a large UI refactor begins, with selectors centralized in page objects you can quickly trace where components are used across the test suite. If forty tests depend on a date picker's internal structure, that number should inform the refactor plan before any changes begin.

If you skip the pre-refactor audit, you tend to discover the impact through [failing CI runs](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci). If you run the audit first, you can prepare by introducing `data-testid` attributes on critical components, sequencing changes to limit disruption, and maintaining coverage throughout the transition. The audit replaces reactive debugging with planned, controlled changes.

###### Playwright component testing as a complement

[@playwright/experimental-ct-react](https://playwright.dev/docs/test-components) mounts individual React components in a real browser using the same Playwright API as the integration suite. A `DatePicker` component can be tested in isolation before it appears in any user flow. When a design system upgrade introduces a new rendering pattern, the component suite catches regressions early, before they reach integration tests.

The experimental label matters. The API continues to change, setup requires extra Vite configuration, and there are real limitations around passing complex Node.js objects to the browser context. It works best alongside integration tests. Component tests focus on element behavior in isolation; integration tests cover full user journeys that cross context, routing, and API boundaries. When managing a design system migration, you get faster feedback at the component level, which reduces the cost of each upgrade.

##### Validating resilience: how to know your tests are actually decoupled

After a UI refactor, a green CI run can be misleading because tests may pass without actually monitoring the changes. Verifying that the suite truly responds to updates requires deliberate checks, not assumptions.

###### Selector resilience audit

Before running anything, a codebase-wide search for `page.locator('.'` in your test files will surface CSS class selectors immediately. These are the most likely coupling candidates. Static analysis catches them without any test execution.

Then run the dynamic check: rename a CSS class, change a build hash, or swap an element type that tests rely on, then run the suite. If tests fail, hidden dependencies exist that code review alone did not expose.

The process takes a few hours, but finding the same coupling mid-migration can cost days of engineering time. A visual refactor that produces zero test failures is the target. If tests fail after a CSS-only change with no functional impact, the selector design needs work.

###### Tracking test failures by root cause

Coupled tests and flaky tests can look similar. Both fail without pointing to a broken feature, but the difference shows up in how those failures group. Selector-coupled tests tend to fail in clusters after UI-related PRs such as design system changes, component library upgrades, or Tailwind migrations.

Selector coupling adds to that cost by triggering failures at the wrong time. A failure after a visual refactor is a bug in the test suite, not a regression. Treating it as noise allows coverage gaps to grow unnoticed.

[Tracking whether failures cluster](https://currents.dev/posts/how-to-track-the-health-of-your-playwright-test-suite) after UI changes or logic updates makes the pattern easier to spot. Visual refactors should produce zero test failures unless application behavior has changed.

###### Code review criteria for selector quality

Most selector coupling enters codebases during code review. Reviewers check whether a selector works, but rarely whether it will stay stable.

A few focused checks make a difference. Does this locator depend on DOM hierarchy? Does it reference a CSS class that a library upgrade could change? Is it scoped to a semantic container? Does it rely on text that a copy update might break?

Selector review deserves the same attention as API design. A fragile locator added today creates maintenance work with every refactor. If you apply these checks consistently, you spend far less time dealing with selector failures.

##### Tests that break for the right reasons

The selector hierarchy is not a ranking of personal preference. It is a ranking of what stays stable. `getByRole` queries what the app promises to expose to users and assistive technology. That contract rarely changes for arbitrary reasons. A CSS class generated by a build tool changes whenever the build changes. A `data-testid` is an explicit commitment your team makes. Text selectors are whatever the copy team decided that day.

Resilience compounds. The first UI refactor after you invest in semantic selectors and stable page objects produces fewer failures than the previous one. Over time, the suite earns a reputation as a reliable signal, and that reputation is what lets test outcomes actually influence engineering decisions.

Tracking failure patterns across releases makes that progress measurable and visible beyond the test infrastructure team. Start with your worst coupling pattern, fix it in the page objects, enforce it in code review, and the next design system migration becomes a much quieter event.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![What Is a Flaky Test in Software Testing, and How to Fix It](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-is-a-flaky-test-and-how-to-fix-it%2Fposter.png&w=3840&q=90)\\
\\
Oct 30, 2025 **What Is a Flaky Test in Software Testing, and How to Fix It** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it) [![How to Track the Health of Your Playwright Test Suite](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-track-the-health-of-your-playwright-test-suite%2Fposter.png&w=3840&q=90)\\
\\
Nov 12, 2025 **How to Track the Health of Your Playwright Test Suite** \\
\\
![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Joshua Adeyemi](https://currents.dev/posts/how-to-track-the-health-of-your-playwright-test-suite) [![How To Debug Playwright Tests in CI: The Complete Guide](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-debug-playwright-tests-in-ci%2Fposter.png&w=3840&q=90)\\
\\
Jan 27, 2026 **How To Debug Playwright Tests in CI: The Complete Guide** \\
\\
![Dumebi Okolo](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Dumebi Okolo](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci)

### 19. Currents.dev — Playwright API testing at scale

- Source: https://currents.dev/posts/playwright-api-testing
- Retrieved: 2026-08-29
- Firecrawl status: complete

[Back](https://currents.dev/blog)

![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)

Currents Team

•Jul 22, 2026•

[Share on Facebook](https://www.facebook.com/sharer/sharer.php?u=https://currents.dev/posts/playwright-api-testing "Share on Facebook")[Share on X](https://twitter.com/intent/tweet?url=https://currents.dev/posts/playwright-api-testing&text=Playwright%20API%20Testing%3A%20Patterns%20That%20Actually%20Scale "Share on X")[Share on LinkedIn](https://www.linkedin.com/shareArticle?mini=true&url=https://currents.dev/posts/playwright-api-testing&title=Playwright%20API%20Testing%3A%20Patterns%20That%20Actually%20Scale "Share on LinkedIn")[Share on Reddit](https://www.reddit.com/submit?url=https://currents.dev/posts/playwright-api-testing&title=Playwright%20API%20Testing%3A%20Patterns%20That%20Actually%20Scale "Share on Reddit")

#### Playwright API Testing: Patterns That Actually Scale

Playwright API tests that pass locally still fail in CI? Here's the fixture architecture, per-worker isolation, and observability practices that fix that.

![Playwright API Testing: Patterns That Actually Scale](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-api-testing%2Fcover.jpg&w=3840&q=100)

Playwright API tests fail in CI because of shared state, not because of `APIRequestContext`.

Most suites start the same way: a few `request.get()` calls, an auth setup in `beforeAll`, and a green local run. The problems show up once parallel workers enter the picture. Tests pass on retry with no code changes. The same test fails on Worker 1 and passes on Worker 3. An [ICST 2024 industrial case study](https://conf.researchr.org/details/icst-2024/icst-2024-industry/1/Cost-of-Flaky-Tests-in-CI-An-Industrial-Case-Study) spanning five years of CI data found that flaky tests consume at least 2.5% of total productive developer time.

[Playwright workers](https://playwright.dev/docs/test-parallel#worker-processes) are isolated OS processes. They share no in-process state. But they still hit the same backend. Worker 1 and Worker 3 both target the same test account. One mutates session state that the other depends on. The failures look random because the instability comes from shared backend data, not from the test framework itself.

This guide covers fixture architecture, parallel-safe data strategies, and the observability practices we've seen work on teams running 200+ API tests across multiple CI workers.

##### **Choosing the Right Request Surface**

Before fixture architecture matters, the request surface has to match the problem. A mismatch won't break locally. It breaks in CI under parallel execution, when auth state from one test leaks into another.

Playwright exposes three ways to make API requests. The difference between them is cookie behavior and lifecycle ownership:

`request` fixture: test-scoped, auto-managed by Playwright, isolated cookie jar per test. Use this for pure API tests. It's the right default.

`page.request` / `browserContext.request`: these are [the same object](https://playwright.dev/docs/api/class-page#page-request). `page.request` returns the `APIRequestContext` of the page's browser context, so cookies are shared at the context level, not per page. Use it when your API call needs the same session the browser already authenticated: it sends the context's cookies and updates them from `Set-Cookie` response headers. The danger: an API call that mutates session state (logout, token rotation) changes the cookies the browser is using mid-test.

`playwright.request.newContext()`: standalone, explicit lifecycle. You create it, you dispose it. Use this when you need full isolation from browser cookies, or when you need custom headers that shouldn't affect the browser context.

The most common mistake we see: teams start with `playwright.request.newContext()` in `beforeAll` because it feels explicit, then discover it creates lifecycle problems that the `request` fixture solves out of the box. Start with the `request` fixture. Upgrade to `newContext()` only when you need cross-test session sharing or independent header configuration.

Centralize config in `playwright.config.ts`. Setting `baseURL` and `extraHTTPHeaders` there eliminates per-test repetition and prevents drift between files:

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    baseURL: process.env.API_BASE_URL || 'https://api.staging.example.com',
    extraHTTPHeaders: {
      'Accept': 'application/json',
      ...(process.env.API_TOKEN ? { 'Authorization': `Bearer ${process.env.API_TOKEN}` } : {}),
    },
  },
});
```


Two gotchas that waste hours in CI:

First, `toBeOK()` only works with [`APIResponse`](https://playwright.dev/docs/api/class-apiresponse) objects returned by `request.get()`, `request.post()`, and similar methods. It does not work with the `Response` type returned by `page.waitForResponse()`. In TypeScript, this is a compile error. In JavaScript, it throws at runtime with a "toBeOK can be only used with APIResponse object" error. Easy to fix once you see it, but if you're mixing both response types in the same file, it's a recurring papercut.

Second, `response.json()` throws when the body isn't valid JSON. In CI, this happens regularly. Load balancers return [HTML error pages](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it) on `502` or `504` responses. The stack trace points at the `json()` call, not at the upstream failure. Check content type before parsing:

```typescript
const response = await apiContext.get('/api/resource');
const contentType = response.headers()['content-type'] || '';
if (!contentType.includes('application/json')) {
  throw new Error(
    `Expected JSON but got ${contentType}. Status: ${response.status()}. Body: ${await response.text()}`
  );
}
const body = await response.json();
```


One more lifecycle detail: the `request` fixture handles disposal automatically. `playwright.request.newContext()` does not. If you skip `await apiContext.dispose()` in teardown, the context leaks connections until the worker process dies.

##### **Building Fixture Architecture That Holds Up in CI**

Knowing which request surface to use is only the starting point. The harder problem is lifecycle management and state isolation. Most suites that fail under parallelism are not suffering from the wrong request surface. The issue usually traces back to `APIRequestContext` setup living in the wrong place and scoped incorrectly.

###### **Why Inline** **`beforeAll`** **Auth Breaks at Scale**

This setup is common across Playwright codebases. It works locally, passes in a single-worker run, and starts failing in ways that are hard to trace once the suite grows:

```typescript
// fragile-setup.spec.ts
let apiContext: APIRequestContext;

test.beforeAll(async ({ playwright }) => {
  apiContext = await playwright.request.newContext({
    baseURL: process.env.API_BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${process.env.API_TOKEN}`,
    },
  });
});

test.afterAll(async () => {
  await apiContext?.dispose();
});
```


The pattern breaks in CI in three ways:

1. **Token expiry.** The context authenticates once in `beforeAll` and holds that token for every test in the file. On long suites, tokens expire mid-run. Fixtures that re-authenticate per test (or per worker) avoid this.
2. **Leaked cleanup.** If `beforeAll` throws partway through, or a test in the file fails in a way that skips the rest, `afterAll` cleanup logic is easy to get wrong: it has to defensively handle every partial-setup state. Fixture teardown (the code after `use()`) runs even when a test fails, because Playwright manages its lifecycle internally. Neither survives a hard process kill (OOM, spot instance termination), which is why the data strategies later in this guide matter more than any hook.
3. **Retry pollution.** [Playwright restarts the worker after a failure](https://playwright.dev/docs/test-parallel#worker-processes), so `beforeAll` does run again on retry. What doesn't reset is the backend: the retry inherits whatever data the failed attempt created. That's not a hook problem, it's a data problem, and it applies to fixtures too. Setup logic has to be idempotent (upserts, check-before-create, per-attempt naming) or retries fail against leftovers from the first attempt.

![Why fixture scope outlasts beforeAll in CI](https://currents.dev/img/posts/playwright-api-testing/ci.jpg)Why fixture scope outlasts beforeAll in CI

###### **The API Fixture Pattern**

The right pattern moves `APIRequestContext` into a named fixture that extends the base test object:

```typescript
// fixtures.ts
import { test as baseTest, APIRequestContext } from '@playwright/test';

type ApiFixtures = {
  apiContext: APIRequestContext;
};

export const test = baseTest.extend<ApiFixtures>({
  apiContext: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: process.env.API_BASE_URL,
      extraHTTPHeaders: {
        Authorization: `Bearer ${process.env.API_TOKEN}`,
        Accept: 'application/json',
      },
    });
    await use(context);
    await context.dispose();
  },
});

export { expect } from '@playwright/test';
```


Test-scoped fixtures are the default choice for most suites because they fully isolate state between tests. The `use()` plus teardown pattern ensures `dispose()` runs even when a test fails, which `afterAll` does not guarantee.

Worker-scoped fixtures are only safe when every test in that worker shares the same auth identity and the session is treated as read-only. If any test mutates backend state, test scope is the safer default regardless of whether auth is shared.

Test-scoped fixtures re-run on every retry, so any backend data they create must be idempotent or uniquely namespaced per attempt. Upsert logic, check-before-create patterns, or worker-indexed identifiers all solve this. Without those safeguards, retries often fail against resources created in the first attempt rather than surfacing the original issue, which skews debugging toward the wrong failure.

The performance tradeoff matters. Creating an `APIRequestContext` itself is fast (milliseconds). But if your fixture authenticates against a real auth server, that round-trip costs whatever your auth endpoint costs. Say it's one second: across 200 tests at test scope, that's over three minutes of pure auth overhead per run. Worker-scoped fixtures amortize that cost across all tests in the worker.

The rule is simple: if every test in the worker uses the same identity and treats the session as read-only, scope to the worker. If any test mutates backend state, scope to the test. When in doubt, start with test scope and optimize later with profiling data.

###### **Authenticated Fixtures with** **`storageState`**

For suites where UI and API tests share the same session model, a setup project generates browser auth state once and `storageState` loads it into the fixture:

```typescript
// fixtures.ts
import { test as baseTest, APIRequestContext } from '@playwright/test';

export const test = baseTest.extend<{ apiContext: APIRequestContext }>({
  apiContext: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: process.env.API_BASE_URL,
      storageState: 'playwright/.auth/user.json',
    });
    await use(context);
    await context.dispose();
  },
});
```


Cookie-backed sessions load cleanly from `storageState`. For apps that authenticate API calls through `Authorization` headers, `storageState` alone is insufficient. Extract the token during setup and pass it via `extraHTTPHeaders`. The complete [Playwright authentication guide](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide) covers per-worker state using `parallelIndex` for this pattern.

Two caveats apply regardless of auth mechanism. `storageState` files go stale and should never be committed or cached across CI runs: generate them fresh in a setup project on every run. Across retries, `storageState` reuse requires idempotent auth setup, otherwise the retry authenticates against a modified session state.

With fixture-managed lifecycles in place, the next step is using API and UI layers together.

##### **Hybrid API + UI Testing: The Real Payoff**

An endpoint can return `200` while the UI renders nothing. A UI action can look successful while the backend state never changed. Pure API tests and pure UI tests both miss this. Hybrid tests catch it because each layer validates what the other can't.

Three patterns cover most hybrid scenarios, ranked by complexity and maintenance cost:

**Pattern 1: Seed via API, assert in UI.** The right default for most hybrid scenarios: an API call sets up backend state and a UI test validates that the state renders correctly. It is also the easiest of the three patterns to maintain over time.

**Pattern 2: Act in UI, verify via API.** A UI interaction triggers an action, then an API call confirms the backend state actually changed. This catches the gap between what the UI displays and what the backend actually recorded.

```typescript
test('submitting order updates backend state', async ({ page, apiContext }) => {
  await page.goto('/orders/new');
  await page.getByLabel('Product').selectOption('widget-a');
  await page.getByRole('button', { name: 'Submit Order' }).click();
  await expect(page.getByText('Order confirmed')).toBeVisible();

  const response = await apiContext.get('/api/orders/latest');
  expect(response.ok()).toBeTruthy();
  const order = await response.json();
  expect(order.status).toBe('confirmed');
  expect(order.product).toBe('widget-a');
});
```


The trap with Pattern 2 is eventual consistency. The UI shows "Order confirmed" but the API might not reflect the change for a few hundred milliseconds. Don't add a hard `waitForTimeout()`. Instead, use `expect.poll()` to retry the API check:

```typescript
await expect.poll(async () => {
  const response = await apiContext.get('/api/orders/latest');
  const order = await response.json();
  return order.status;
}, { timeout: 5000 }).toBe('confirmed');
```


**Pattern 3: API to UI to API loop.** Reserved for critical user journeys where full round-trip verification is justified. Seed data via API, validate it renders in the UI, perform a UI action, then verify the backend state changed. The maintenance cost is high. Use it for checkout flows, payment processing, or onboarding. Not for CRUD operations.

The `apiContext` fixture and the browser context do not share auth automatically. This is where most hybrid tests break silently: the browser authenticates fine, but the API call returns `401` mid-test because nobody passed the session to both contexts.

For cookie-based auth: load the same `storageState` into both the browser context and the API fixture. For header-based auth: extract the token during setup and pass it via `extraHTTPHeaders` in the API fixture. Don't assume that authenticating the browser magically authenticates API calls.

Once hybrid tests work with consistent auth, the next failure mode is data collision across parallel workers.

##### **Parallel-Safe API Testing in CI**

Hybrid tests expose integration failures. Parallel CI runs expose weaknesses in data architecture. The previous challenge was auth coordination across API and UI contexts. Once those tests execute across multiple workers simultaneously, the pressure shifts to data collision, isolation strategy, and observability.

###### **Why Parallel Runs Break API Tests**

![Parallel workers, one account, and the collisions that follow](https://currents.dev/img/posts/playwright-api-testing/problem.png)Parallel workers, one account, and the collisions that follow

Playwright workers isolate browser state but still operate against the same backend. Two workers targeting the same test account both assume exclusive ownership of that data. One changes a tenant-level setting the other relies on. Neither worker has visibility into the other's activity. The resulting failures appear random because the instability originates in shared backend state rather than in the test logic.

Per-worker data scoping reduces that risk. `testInfo.workerIndex` and `testInfo.parallelIndex` solve different problems, and using the wrong one creates avoidable collisions.

- `testInfo.workerIndex` is a monotonically increasing ID starting at 1 that's unique across the entire run. Every new worker process (including restarts after failure) gets a new index. Use it for identifiers that must never collide, like dynamically created test resources.

- `testInfo.parallelIndex` ranges from `0` to `workers - 1` and stays stable across worker restarts. If Worker 2 crashes and Playwright spawns a replacement, the new worker keeps `parallelIndex: 2` but gets a fresh `workerIndex`. Use it when each parallel slot maps to a fixed pool of pre-provisioned resources. The pool must be at least as large as the configured `workers` count. If `workers` exceeds the pool size, multiple slots collide on the same resource and the isolation breaks.


```typescript
const email = `test-user-${testInfo.workerIndex}@example.com`;

const accounts = ['admin@example.com', 'editor@example.com', 'viewer@example.com'];

// Pool size must be >= workers setting to avoid collisions
if (testInfo.parallelIndex >= accounts.length) {
  throw new Error(`parallelIndex ${testInfo.parallelIndex} exceeds account pool size ${accounts.length}. Add more accounts or reduce workers.`);
}

const account = accounts[testInfo.parallelIndex];
```


###### **Test Data Isolation Strategy**

Per-worker scoping solves identity collision. Data lifecycle strategy determines how the remaining failures are contained:

- Tests creating backend data should clean it up even after failure. Teardown belongs inside `try/finally` blocks within fixtures rather than loose `afterEach` hooks that may never execute.

- Ephemeral data strategies are more reliable than teardown alone. TTL-based records, tenant scoping, and namespaced resources prevent partial state from leaking into later runs.

- Stable reference data belongs in `globalSetup` and should remain read-only throughout the suite.

- Retries require idempotent setup logic. Upsert patterns, deterministic naming, or check-before-create flows prevent retries from failing against resources already created during earlier attempts.


```typescript
apiContext: async ({ playwright }, use) => {
  const context = await playwright.request.newContext({
    baseURL: process.env.API_BASE_URL,
  });
  const testData = await createTestData(context);
  try {
    await use(context);
  } finally {
    await cleanupTestData(context, testData.id);
    await context.dispose();
  }
},
```


###### **Observability: What to Do When an API Test Fails in CI**

UI test failures give you screenshots, traces, and DOM snapshots. API test failures give you a status code and a timeout. That gap in debugging context is why API failures take longer to investigate, even though they're usually simpler problems.

Close the gap with `testInfo.attach()`. Attach the request URL, sanitized headers, status code, and response body to every failed API test. This turns "test timed out" into "POST /api/orders returned 502 with body: `<html>Bad Gateway</html>`." That's the difference between a 5-minute fix and a 30-minute investigation.

```typescript
test('creates order', async ({ apiContext }, testInfo) => {
  const response = await apiContext.post('/api/orders', {
    data: { product: 'widget-a', quantity: 1 },
  });

  if (!response.ok()) {
    await testInfo.attach('api-failure', {
      body: JSON.stringify({
        url: response.url(),
        status: response.status(),
        body: await response.text(),
      }, null, 2),
      contentType: 'application/json',
    });
  }

  expect(response.ok()).toBeTruthy();
});
```


For suites with many API calls, wrap this pattern in a helper that attaches context automatically on non-2xx responses. That way you get debugging data without cluttering every test.

Per-run artifacts tell you what failed. They don't tell you why the failure only appears on Worker 3, after retries, on the `feature/checkout` branch. Those patterns need cross-run visibility. [Currents](https://currents.dev/) surfaces those patterns across workers, branches, and retries in one place. That's what turns a repeated `401` into a diagnosable data collision rather than an unexplained flake. Teams dealing with persistent timeout failures should also review [Playwright's timeout debugging guidance](https://currents.dev/posts/debugging-playwright-timeouts) for CI-specific patterns.

##### **Network Interception: When to Mock, When to Hit the Real Backend**

Mocking and real API validation solve different problems. Mixing them up is one of the fastest ways to build a suite that passes every run and catches zero regressions.

The rule: test what you control, mock what you don't. [Playwright's best practices](https://playwright.dev/docs/best-practices) say this directly. Your `APIRequestContext` should hit real endpoints. `page.route()` and `browserContext.route()` should intercept third-party dependencies you can't control: analytics, payment processors, CDNs.

The failure mode we see most often: a team mocks their own API to make a flaky test pass. The test goes green. Six months later, a backend field gets renamed and nothing catches it because the mock still returns the old shape. If the test is meant to validate your backend, it has to talk to your backend.

Route handler precedence matters in hybrid setups. When both [`page.route()` and `browserContext.route()`](https://playwright.dev/docs/api/class-browsercontext#browser-context-route) match the same URL, the page-level handler wins. Register broad mocks at the context level (block all analytics), then override specific routes per test:

```typescript
// Global mock at context level: stub analytics endpoint
await browserContext.route('**/analytics/**', route =>
  route.fulfill({ status: 200, body: '{}' })
);

// Test-specific override: simulate a 500 from a third-party service
await page.route('**/analytics/track', async route => {
  await route.fulfill({
    status: 500,
    body: JSON.stringify({ error: 'upstream failure' }),
  });
});
```


For first-party endpoints, use interception only when you're intentionally testing failure handling or edge cases (simulating a `503`, testing retry logic). Not when the goal is to validate real backend behavior. [The Playwright network mocking playbook](https://currents.dev/posts/the-playwright-network-mocking-playbook) goes deeper on how to apply this without accumulating mock debt.

##### **Schema Validation: Catching Contract Drift Before It Breaks the UI**

Most API tests check a status code and assert one or two fields. That works until a backend team renames a property, adds a required field, or changes a type from `string` to `number`. The test still passes. The frontend breaks in production.

This is contract drift. The API shape changed, but nothing in the test suite noticed because no test validated the shape. Adding schema validation doesn't mean turning every test into a contract test. It means being intentional about where you check structure vs. where you check behavior.

**Start with `toMatchObject()`.** It's built into Playwright, requires no dependencies, and catches the most common drift: missing fields, wrong types, unexpected nulls. It does partial matching, so it won't break when the backend adds new optional fields.

```typescript
// This misses structural drift. The field could be a number and this still passes.
expect(body.id).toBeDefined();

// This catches type changes and missing fields.
expect(body).toMatchObject({
  id: expect.any(String),
  status: expect.any(String),
  createdAt: expect.any(String),
});
```


**Upgrade to Zod when your API serves multiple consumers.** If the frontend, mobile app, and a partner integration all depend on the same endpoint, `toMatchObject()` isn't strict enough. Zod's [`safeParse`](https://zod.dev/basics#handling-errors) validates the full shape and produces CI-friendly error output that tells you exactly which field broke:

```typescript
import { z } from 'zod';

const OrderSchema = z.object({
  id: z.uuid(),
  status: z.enum(['pending', 'confirmed', 'shipped']),
  createdAt: z.iso.datetime(),
  items: z.array(z.object({
    productId: z.string(),
    quantity: z.number().int().positive(),
  })),
});

test('order response matches contract', async ({ apiContext }) => {
  const response = await apiContext.get('/api/orders/latest');
  const body = await response.json();
  const result = OrderSchema.safeParse(body);
  if (!result.success) {
    throw new Error(`Schema validation failed:\n${z.prettifyError(result.error)}`);
  }
});
```


The example uses Zod 4 idioms: `z.uuid()` and `z.iso.datetime()` replaced the deprecated `z.string().uuid()` and `z.string().datetime()`, and [`z.prettifyError()`](https://zod.dev/error-formatting) turns the error into a readable string. On Zod 3, use `JSON.stringify(result.error.format(), null, 2)` instead; interpolating `result.error.format()` directly prints `[object Object]`.

**Don't validate schema in every test.** Dedicate a small set of contract tests that validate response shapes against critical endpoints. Your feature tests should assert behavior (did the order get created?). Your contract tests should assert shape (does the response still match what the frontend expects?). Mixing both concerns in every test makes the suite brittle and hard to maintain.

If you have an OpenAPI spec, consider generating Zod schemas from it with tools like [`openapi-zod-client`](https://github.com/astahmer/openapi-zod-client). That keeps test schemas in sync with the API definition automatically. (Don't confuse it with `zod-openapi`, which goes the other direction: it generates OpenAPI docs from Zod schemas.)

##### **CI Configuration That Reflects the Architecture**

None of this holds up if API and UI tests share a single Playwright project with identical settings. API tests have different timeout profiles, different failure modes, and different retry economics. Separate projects let you configure each independently. [Playwright's CI documentation](https://playwright.dev/docs/ci) supports this directly.

Three configuration decisions matter specifically for API projects:

- **Timeouts.** Set per-request timeouts on the API context: `playwright.request.newContext({ timeout: 15000 })`. Don't confuse this with `actionTimeout`, which only applies to UI actions like `click()` and `fill()`. `actionTimeout` has no effect on API requests, regardless of whether you use the `request` fixture or `page.request`.

- **Retries.** Configure them independently. A flaky auth endpoint may justify one retry. A fragile UI flow may need two. Sharing the same retry count adds noise: you either over-retry API tests (wasting time) or under-retry UI tests (missing real flakes).

- **Failure output.** API failures in CI give you a status code and a timeout. That's not enough. Use `testInfo.attach()` to capture request URLs, sanitized headers, status codes, and response bodies. This is your equivalent of screenshots and traces for API tests.


Use `project.dependencies` only when the API project creates state that the UI project needs. If they're independent, run them in parallel. Linking unrelated projects slows feedback and makes it harder to tell whether a failure came from setup or from the test.

API test results belong in the same reporting view as UI results. Routing them to separate dashboards means [debugging across disconnected systems](https://currents.dev/posts/how-to-run-playwright-tests-without-the-pain), which adds time to every incident.

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  projects: [\
    {\
      name: 'api-setup',\
      testDir: './tests/api',\
      use: {\
        baseURL: process.env.API_BASE_URL,\
        extraHTTPHeaders: {\
          Authorization: `Bearer ${process.env.API_TOKEN}`,\
        },\
      },\
      // Per-test timeout for the API project\
      // Set per-request timeout in the fixture via playwright.request.newContext({ timeout })\
      timeout: 15000,\
      retries: 1,\
    },\
    {\
      name: 'ui',\
      testDir: './tests/ui',\
      dependencies: ['api-setup'],\
      use: {\
        baseURL: process.env.BASE_URL,\
        actionTimeout: 10000,\
      },\
      retries: 2,\
    },\
  ],
  reporter: [['html'], ['junit', { outputFile: 'results.xml' }]],
});
```


##### **Wrapping Up**

The pattern behind every fix in this guide is the same: move lifecycle management into fixtures, scope data to the worker, and make failures visible without manual artifact hunting.

If you take one thing from this: stop using `beforeAll` for API context setup. Move it into a fixture. That single change fixes token expiry, leaked contexts, and retry pollution in one step.

API failures deserve the same cross-run visibility you expect from UI failures. [Currents](https://currents.dev/) provides that without custom reporting infrastructure, surfacing failure patterns across workers, branches, and retries in one place.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![Debugging Playwright Timeouts: A Practical Checklist](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-timeouts%2Fposter.png&w=3840&q=90)\\
\\
Oct 23, 2025 **Debugging Playwright Timeouts: A Practical Checklist** \\
\\
![Goodness Eboh](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Goodness Eboh](https://currents.dev/posts/debugging-playwright-timeouts) [![How To Speed Up Playwright Tests: 7 Tips From Experts](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-speed-up-playwright-tests%2Fcover.png&w=3840&q=90)\\
\\
Jan 07, 2026 **How To Speed Up Playwright Tests: 7 Tips From Experts** \\
\\
![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Joshua Adeyemi](https://currents.dev/posts/how-to-speed-up-playwright-tests) [![How To Build Reliable Playwright Tests: A Cultural Approach](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-build-reliable-playwright-tests-a-cultural-approach%2Fposter.png&w=3840&q=90)\\
\\
Nov 28, 2025 **How To Build Reliable Playwright Tests: A Cultural Approach** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/how-to-build-reliable-playwright-tests-a-cultural-approach) [![What Is a Flaky Test in Software Testing, and How to Fix It](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-is-a-flaky-test-and-how-to-fix-it%2Fposter.png&w=3840&q=90)\\
\\
Oct 30, 2025 **What Is a Flaky Test in Software Testing, and How to Fix It** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it)

### 20. Sajith Dilshan — Async/await in Playwright TypeScript

- Source: https://medium.com/@sajith-dilshan/mastering-async-await-in-playwright-typescript-1343e5b21722
- Retrieved: 2026-08-29
- Firecrawl status: complete

[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40sajith-dilshan%2Fmastering-async-await-in-playwright-typescript-1343e5b21722&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40sajith-dilshan%2Fmastering-async-await-in-playwright-typescript-1343e5b21722&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:40:40/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---post_author_sidebar--1343e5b21722-----------------f144eae2a327----------------------)

##### sajith dilshan

Software Engineer \| SDET \| Technical Writer \| Tech Enthusiast

Follow writer

[Playwright](https://medium.com/tag/playwrights?source=post_page---header_tags--1343e5b21722---------------------------------------)

[Test Automation](https://medium.com/tag/test-automation?source=post_page---header_tags--1343e5b21722---------------------------------------)

[Software Testing](https://medium.com/tag/software-testing?source=post_page---header_tags--1343e5b21722---------------------------------------)

[Asynchronous Programming](https://medium.com/tag/asynchronous-programming?source=post_page---header_tags--1343e5b21722---------------------------------------)

[Typescript](https://medium.com/tag/typescript?source=post_page---header_tags--1343e5b21722---------------------------------------)

#### Mastering Async/Await in Playwright TypeScript

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:32:32/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---byline--1343e5b21722---------------------------------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---byline--1343e5b21722---------------------------------------)

Follow

5 min read

·

18 hours ago

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D1343e5b21722&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40sajith-dilshan%2Fmastering-async-await-in-playwright-typescript-1343e5b21722&source=---header_actions--1343e5b21722---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*Uk2o_Ii_RtDXUDn1Ybb9xg.png)

Mastering Async/Await in Playwright TypeScript

When I first transitioned from Selenium to Playwright, one of the concepts I had to truly understand was `async/await`.

In Selenium, we often deal with synchronization issues using explicit waits, implicit waits, and custom wait utilities. While Playwright handles many synchronization challenges automatically, it heavily relies on asynchronous operations under the hood.

Over the years of building automation frameworks and maintaining large test suites, I’ve found that many flaky Playwright tests are not caused by application defects. More often than not, they’re caused by incorrectly handling asynchronous operations.

In this article, I’ll explain what asynchronous programming is, how Promises work, what `async/await` does, and why mastering these concepts is essential for writing reliable Playwright automation tests.

##### What is Asynchronous Programming?

In JavaScript and TypeScript, some operations take time to complete, such as:

- Opening a browser
- Navigating to a website
- Waiting for an element
- Clicking a button
- Fetching data from an API

Instead of blocking execution while waiting for these operations, JavaScript allows them to run asynchronously.

##### Example

```
console.log("Start");

setTimeout(() => {
    console.log("Completed");
}, 2000);

console.log("End");
```

##### Output

```
Start
End
Completed
```

Notice that the program doesn’t stop and wait for the `setTimeout()` to finish. It continues executing the next statement immediately.

This non-blocking behavior is one of the reasons JavaScript is so efficient.

##### What is a Promise?

A Promise represents a value that may not be available immediately but will be available in the future.

Think of it like ordering food at a restaurant.

You place the order now, continue your conversation, and eventually receive your food when it’s ready.

A Promise works in a similar way.

##### Example

```
const promise = new Promise((resolve) => {
    setTimeout(() => resolve("Success"), 2000);
});

promise.then((result) => {
    console.log(result);
});
```

##### Output

```
Success
```

Most Playwright methods return Promises because browser interactions take time to complete.

##### What is Async/Await?

Async/Await is a cleaner and more readable way of working with Promises.

Instead of chaining multiple `.then()` statements, we can write asynchronous code that looks almost like synchronous code.

##### Async Function

```
async function greet() {
    return "Hello";
}
```

The `async` keyword automatically wraps the return value inside a Promise.

The above code is equivalent to:

```
function greet() {
    return Promise.resolve("Hello");
}
```

##### Await Keyword

```
async function greet() {
    const message = await Promise.resolve("Hello");
    console.log(message);
}
```

The `await` keyword pauses execution until the Promise is resolved.

This makes asynchronous code much easier to read and maintain.

##### Why Async/Await is Important in Playwright

Every Playwright action communicates with the browser and requires time to complete.

##### Examples

```
await page.goto("https://example.com");

await page.click("#login");

await page.fill("#username", "admin");
```

Behind the scenes, Playwright needs to:

- Send commands to the browser
- Wait for navigation
- Locate elements
- Verify elements are actionable
- Perform interactions

All these operations are asynchronous.

Without `await`, Playwright may try to execute the next action before the previous one has completed.

##### What Happens If Await Is Missing?

One of the most common mistakes I see in Playwright projects is forgetting to use `await`.

##### Incorrect Example

```
test('Missing Await', async ({ page }) => {

   page.goto('https://example.com');
   page.click('#login');
});
```

##### Possible Errors

```
Element not found
Page not loaded
Timeout exceeded
```

Because the click action may execute before the page finishes loading.

These failures can be especially frustrating because they may pass locally but fail intermittently in CI/CD pipelines.

##### Correct Example

```
test('Correct Usage', async ({ page }) => {
     await page.goto('https://example.com');
     await page.click('#login');
});
```

By adding `await`, Playwright waits for the page to load before performing the click action.

This simple change can significantly improve test stability.

##### A Real-World Playwright Test

Here’s a typical login test:

```
test('User Login', async ({ page }) => {    await page.fill('#username', 'admin');    await page.fill('#password', 'password123');    await page.click('#loginBtn');    await expect(page).toHaveURL(/dashboard/);});
```

Each action waits for the previous action to complete before moving to the next step.

This predictable execution flow is what makes Playwright tests reliable.

##### Async Functions in Page Object Model (POM)

Most enterprise automation frameworks use the Page Object Model (POM).

##### Get sajith dilshan’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

Since Playwright actions are asynchronous, Page Object methods should also be asynchronous.

##### Example

```
export class LoginPage {

   constructor(private page: Page) {}

    async login(username: string, password: string) {
        await this.page.fill('#username', username);
        await this.page.fill('#password', password);
        await this.page.click('#loginButton');
    }
}
```

##### Test Example

```
test('Login Test', async ({ page }) => {

const loginPage = new LoginPage(page);
    await loginPage.login('admin', 'password123');
});
```

A good rule I follow is:

**If a method contains Playwright actions, make it async and always await it.**

##### Handling Multiple Async Operations

Most Playwright actions execute sequentially.

##### Sequential Execution

```
await page.fill('#firstName', 'John');

await page.fill('#lastName', 'Doe');

await page.click('#save');
```

Each action waits for the previous one to finish.

This creates a predictable execution flow and makes debugging easier.

##### Using Promise.all()

Sometimes multiple asynchronous operations should run together.

A common example is clicking a button that triggers navigation.

##### Example

```
await Promise.all([\
    page.waitForNavigation(),\
    page.click('#submit')\
]);
```

##### Benefits

- Faster execution
- Prevents race conditions
- Recommended for navigation-triggering actions
- Improves test reliability

I use this pattern frequently because it ensures Playwright starts waiting for navigation before the click action triggers it.

##### Async/Await with API Testing

Playwright is not limited to UI testing.

It also provides powerful API testing capabilities.

##### Example

```
test('Get Users', async ({ request }) => {

const response = await request.get(
        'https://jsonplaceholder.typicode.com/users'
    );
    expect(response.status()).toBe(200);
});
```

The request is asynchronous, so Playwright waits for the response before performing assertions.

This makes API tests clean and easy to understand.

##### Error Handling with Try-Catch

Proper error handling can save significant debugging time.

##### Example

```
try {

await page.goto('https://example.com');

    await page.click('#login');

}

catch(error) {

    console.log("Error:", error);

}
```

This allows you to capture unexpected failures and provide meaningful logs for troubleshooting.

##### Best Practices

After working with Playwright in real-world projects, these are the practices I consistently follow.

##### 1\. Always Use Await with Playwright Actions

##### ✅ Correct

```
await page.click('#submit');
```

##### ❌ Incorrect

```
page.click('#submit');
```

Missing `await` is one of the most common causes of flaky tests.

##### 2\. Use Async for Helper Methods

```
async createUser() {
    await page.click('#addUser');
}
```

Any helper method that performs Playwright actions should be asynchronous.

##### 3\. Use Promise.all() for Navigation Events

```
await Promise.all([\
    page.waitForNavigation(),\
    page.click('#login')\
]);
```

This avoids race conditions and improves reliability.

##### 4\. Avoid Unnecessary Waits

##### ❌ Bad

```
await page.waitForTimeout(5000);
```

##### ✅ Good

```
await page.waitForSelector('#dashboard');
```

Always wait for a condition rather than a fixed amount of time.

Your tests will be faster and more stable.

##### 5\. Handle Exceptions Properly

```
try {

await page.click('#submit');

}
catch(error) {

    console.error(error);

}
```

Good error handling helps identify failures quickly and makes debugging easier.

##### Common Interview Questions

##### Why is async/await used in Playwright?

Because Playwright operations are asynchronous and return Promises.

##### What happens if await is omitted?

The next action may execute before the previous one completes, causing flaky tests and timing-related failures.

##### Can await be used without async?

No. The function must be declared with the `async` keyword.

##### What does Promise.all() do?

It executes multiple asynchronous operations simultaneously and waits for all of them to complete.

##### Does page.click() return a Promise?

Yes. Most Playwright methods return Promises and should be awaited.

##### Final Thoughts

If there’s one thing every Playwright automation engineer should master early, it’s `async/await`.

In my experience, most flaky Playwright tests can be traced back to one of three issues:

- Missing `await`
- Incorrect handling of navigation events
- Using hard waits instead of condition-based waits

Understanding Promises and Async/Await doesn’t just help you write better code — it helps you build stable, maintainable, and trustworthy automation frameworks.

The difference between a flaky test suite and a reliable one is often just a few well-placed `await` statements.

[Playwright](https://medium.com/tag/playwrights?source=post_page---footer_tags--1343e5b21722---------------------------------------)

[Test Automation](https://medium.com/tag/test-automation?source=post_page---footer_tags--1343e5b21722---------------------------------------)

[Software Testing](https://medium.com/tag/software-testing?source=post_page---footer_tags--1343e5b21722---------------------------------------)

[Asynchronous Programming](https://medium.com/tag/asynchronous-programming?source=post_page---footer_tags--1343e5b21722---------------------------------------)

[Typescript](https://medium.com/tag/typescript?source=post_page---footer_tags--1343e5b21722---------------------------------------)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:48:48/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---post_author_info--1343e5b21722---------------------------------------)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:64:64/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---post_author_info--1343e5b21722---------------------------------------)

Follow

[**Written by sajith dilshan**](https://medium.com/@sajith-dilshan?source=post_page---post_author_info--1343e5b21722---------------------------------------)

[97 followers](https://medium.com/@sajith-dilshan/followers?source=post_page---post_author_info--1343e5b21722---------------------------------------)

· [109 following](https://medium.com/@sajith-dilshan/following?source=post_page---post_author_info--1343e5b21722---------------------------------------)

Software Engineer \| SDET \| Technical Writer \| Tech Enthusiast

Follow

##### No responses yet

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

Write a response

##### More from sajith dilshan

![Playwright Locator Strategy: Choosing the Right Locator for Stable and Maintainable Test Automation](https://miro.medium.com/v2/resize:fit:679/format:webp/1*LTmJKFq4634wVKUB6XQF_g.png)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:20:20/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----0---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----0---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

·

Aug 14

[**Playwright Locator Strategy: Choosing the Right Locator for Stable and Maintainable Test Automation**\\
\\
**As QA engineers, we’ve all experienced that frustrating moment when a test that passed yesterday suddenly fails today. After spending time…**](https://medium.com/@sajith-dilshan/playwright-locator-strategy-choosing-the-right-locator-for-stable-and-maintainable-test-automation-018a4fd0e16c?source=post_page---author_recirc--1343e5b21722----0---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[A clap icon2\\
\\
A response icon1](https://medium.com/@sajith-dilshan/playwright-locator-strategy-choosing-the-right-locator-for-stable-and-maintainable-test-automation-018a4fd0e16c?source=post_page---author_recirc--1343e5b21722----0---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

![All About String in Java](https://miro.medium.com/v2/resize:fit:679/format:webp/0*80JpXro7XnNJ4WAE.png)

[![Geek Culture](https://miro.medium.com/v2/resize:fill:20:20/1*bWAVaFQmpmU6ePTjNIje_A.jpeg)](https://medium.com/geekculture?source=post_page---author_recirc--1343e5b21722----1---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

In

[Geek Culture](https://medium.com/geekculture?source=post_page---author_recirc--1343e5b21722----1---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

by

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----1---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

·

Feb 3, 2023

[**All About String in Java**\\
\\
**Java Strings are objects that are backed by a char array and are immutable. The class java.lang.String is used to create a string object.**](https://medium.com/geekculture/all-about-string-in-java-51ba9e46181a?source=post_page---author_recirc--1343e5b21722----1---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[A clap icon125\\
\\
A response icon3](https://medium.com/geekculture/all-about-string-in-java-51ba9e46181a?source=post_page---author_recirc--1343e5b21722----1---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

![From Selenium to Playwright: A Senior QA Engineer’s Migration Journey](https://miro.medium.com/v2/resize:fit:679/format:webp/1*t6SN4-pG1wqwg9J-5rp7gg.png)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:20:20/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----2---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----2---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

·

Mar 7

[**From Selenium to Playwright: A Senior QA Engineer’s Migration Journey**\\
\\
**In today’s fast-paced web development environment, automation testing frameworks must be stable, fast, and maintainable. For years…**](https://medium.com/@sajith-dilshan/from-selenium-to-playwright-a-senior-qa-engineers-migration-journey-77125ae54c05?source=post_page---author_recirc--1343e5b21722----2---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[A clap icon1](https://medium.com/@sajith-dilshan/from-selenium-to-playwright-a-senior-qa-engineers-migration-journey-77125ae54c05?source=post_page---author_recirc--1343e5b21722----2---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

![Secure Credential Management in Playwright](https://miro.medium.com/v2/resize:fit:679/format:webp/1*m45OEhkt7sIFJgV5mjlbpA.png)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:20:20/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----3---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722----3---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

·

Jan 31

[**Secure Credential Management in Playwright**\\
\\
**From Local Development to Azure DevOps CI/CD (Enterprise & Security-First Guide)**](https://medium.com/@sajith-dilshan/secure-credential-management-in-playwright-0cf75c4e2ff4?source=post_page---author_recirc--1343e5b21722----3---------------------fe1bda1e_bac0_46cf_9e85_cc13787a98e7--------------)

[See all from sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---author_recirc--1343e5b21722---------------------------------------)

##### Recommended from Medium

![Evolving POM: From Page Objects to Agent-Friendly Design](https://miro.medium.com/v2/resize:fit:679/format:webp/1*jd954bwbW-VbzrjXpnkSLg.png)

[![Martin Marchetto (aka Magic)](https://miro.medium.com/v2/resize:fill:20:20/1*qK5KPp8sc4ZJSguoLVdq5w.jpeg)](https://medium.com/@martinmarchetto?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[Martin Marchetto (aka Magic)](https://medium.com/@martinmarchetto?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Mar 16

[**Evolving POM: From Page Objects to Agent-Friendly Design**\\
\\
**How to adapt your existing Playwright framework so AI agents can actually use it — without rewriting everything.**](https://medium.com/@martinmarchetto/evolving-pom-from-page-objects-to-agent-friendly-design-38c074ec8519?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[A clap icon10](https://medium.com/@martinmarchetto/evolving-pom-from-page-objects-to-agent-friendly-design-38c074ec8519?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

![Network Interception with Playwright TypeScript](https://miro.medium.com/v2/resize:fit:679/format:webp/1*dMkWwzIvtxjTgllzupwzow.png)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:20:20/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://medium.com/gitconnected?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

In

[Level Up Coding](https://medium.com/gitconnected?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

by

[Mohammad Faisal Khatri](https://medium.com/@iamfaisalkhatri?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Aug 11

[**Network Interception with Playwright TypeScript**\\
\\
**Playwright Network Interception: A Practical Guide to API Mocking, Request Modification, and Network Control**](https://medium.com/gitconnected/network-interception-with-playwright-typescript-0dd32195848b?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[A clap icon38](https://medium.com/gitconnected/network-interception-with-playwright-typescript-0dd32195848b?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

![Salesforce API Testing with Playwright + TypeScript (2026 Edition)](https://miro.medium.com/v2/resize:fit:679/format:webp/1*UcaIxjewXakWZVx2hnus6w.png)

[![HimanshuAI](https://miro.medium.com/v2/resize:fill:20:20/1*aLr6cANMGfN2oSlsiq0mIQ.png)](https://medium.com/innernet-world?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

In

[HimanshuAI](https://medium.com/innernet-world?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

by

[Himanshu Agarwal](https://medium.com/@himanshuai?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Aug 7

[**Salesforce API Testing with Playwright + TypeScript (2026 Edition)**\\
\\
**REST, OAuth, Bulk API & Integration Testing**](https://medium.com/innernet-world/salesforce-api-testing-with-playwright-typescript-2026-edition-7266e1acf7f8?source=post_page---read_next_recirc--1343e5b21722----0---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

![Playwright: The Testing Tool That Actually Opens a Browser and Clicks Things](https://miro.medium.com/v2/resize:fit:679/format:webp/0*VIKnLZV0eZOwHCyU)

[![Jesse L](https://miro.medium.com/v2/resize:fill:20:20/1*gDGj2RplVYObqmuinvzGtw.jpeg)](https://medium.com/@liu-111?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[Jesse L](https://medium.com/@liu-111?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Aug 19

[**Playwright: The Testing Tool That Actually Opens a Browser and Clicks Things**\\
\\
**Hi everyone, welcome back. Today, I will be going over Playwright — what it does, how it differs from the other kinds of testing you’ve…**](https://medium.com/@liu-111/playwright-the-testing-tool-that-actually-opens-a-browser-and-clicks-things-f73f62a03cf6?source=post_page---read_next_recirc--1343e5b21722----1---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

![Playwright Reporters: Which One Should You Choose? Beginners Guide](https://miro.medium.com/v2/resize:fit:679/format:webp/1*sLTEUMhKN8SsW_EQifr0Zg.png)

[![Svetlana Tretjakova](https://miro.medium.com/v2/resize:fill:20:20/1*CrMsyo2QQ5ASS4s9nd7sKg.png)](https://medium.com/@svetlana.tretjakova?source=post_page---read_next_recirc--1343e5b21722----2---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[Svetlana Tretjakova](https://medium.com/@svetlana.tretjakova?source=post_page---read_next_recirc--1343e5b21722----2---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Aug 2

[**Playwright Reporters: Which One Should You Choose? Beginners Guide**\\
\\
**When we run Playwright tests, the test runner needs some way to show us what happened.**](https://medium.com/@svetlana.tretjakova/playwright-reporters-which-one-should-you-choose-beginners-guide-99063c4d1fc3?source=post_page---read_next_recirc--1343e5b21722----2---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[A clap icon53](https://medium.com/@svetlana.tretjakova/playwright-reporters-which-one-should-you-choose-beginners-guide-99063c4d1fc3?source=post_page---read_next_recirc--1343e5b21722----2---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

![Selenium vs Playwright: Why I’d Choose Playwright for My Next Automation Project](https://miro.medium.com/v2/resize:fit:679/format:webp/1*5LnjyDT20qfLNJd0ndAhEw.png)

[![Zubair Khan](https://miro.medium.com/v2/resize:fill:20:20/1*7T_8_cAJqeQKQNv00WbZ3A.jpeg)](https://medium.com/@zubairkhansh?source=post_page---read_next_recirc--1343e5b21722----3---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[Zubair Khan](https://medium.com/@zubairkhansh?source=post_page---read_next_recirc--1343e5b21722----3---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

·

Aug 1

[**Selenium vs Playwright: Why I’d Choose Playwright for My Next Automation Project**\\
\\
**For years, there was one answer whenever someone asked about browser automation:**](https://medium.com/@zubairkhansh/selenium-vs-playwright-why-id-choose-playwright-for-my-next-automation-project-84d471558a8c?source=post_page---read_next_recirc--1343e5b21722----3---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[A clap icon120\\
\\
A response icon1](https://medium.com/@zubairkhansh/selenium-vs-playwright-why-id-choose-playwright-for-my-next-automation-project-84d471558a8c?source=post_page---read_next_recirc--1343e5b21722----3---------------------b0b3389a_d94b_4fff_ab52_d1a2d1072a45--------------)

[See more recommendations](https://medium.com/?source=post_page---read_next_recirc--1343e5b21722---------------------------------------)

[Help](https://help.medium.com/hc/en-us?source=post_page-----1343e5b21722---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----1343e5b21722---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----1343e5b21722---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----1343e5b21722---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----1343e5b21722---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----1343e5b21722---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----1343e5b21722---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----1343e5b21722---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----1343e5b21722---------------------------------------)

### 3. Playwright — Configuration

- Source: https://playwright.dev/docs/test-configuration
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

Playwright has many options to configure how your tests are run. You can specify these options in the configuration file. Note that test runner options are **top-level**, do not put them into the `use` section.

##### Basic Configuration

Here are some of the most common configuration options.

```js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

  // Look for test files in the "tests" directory, relative to this configuration file.

  testDir: 'tests',

  // Run all tests in parallel.

  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code.

  forbidOnly: !!process.env.CI,

  // Retry on CI only.

  retries: process.env.CI ? 2 : 0,

  // Opt out of parallel tests on CI.

  workers: process.env.CI ? 1 : undefined,

  // Reporter to use

  reporter: 'html',

  use: {

    // Base URL to use in actions like `await page.goto('/')`.

    baseURL: 'http://localhost:3000',

    // Collect trace when retrying the failed test.

    trace: 'on-first-retry',

  },

  // Configure projects for major browsers.

  projects: [\

\

    {\

\

      name: 'chromium',\

\

      use: { ...devices['Desktop Chrome'] },\

\

    },\

\

  ],

  // Run your local dev server before starting the tests.

  webServer: {

    command: 'npm run start',

    url: 'http://localhost:3000',

    reuseExistingServer: !process.env.CI,

  },

});
```

| Option | Description |
| --- | --- |
| [testConfig.forbidOnly](https://playwright.dev/docs/api/class-testconfig#test-config-forbid-only) | Whether to exit with an error if any tests are marked as `test.only`. Useful on CI. |
| [testConfig.fullyParallel](https://playwright.dev/docs/api/class-testconfig#test-config-fully-parallel) | have all tests in all files to run in parallel. See [Parallelism](https://playwright.dev/docs/test-parallel) and [Sharding](https://playwright.dev/docs/test-sharding) for more details. |
| [testConfig.projects](https://playwright.dev/docs/api/class-testconfig#test-config-projects) | Run tests in multiple configurations or on multiple browsers |
| [testConfig.reporter](https://playwright.dev/docs/api/class-testconfig#test-config-reporter) | Reporter to use. See [Test Reporters](https://playwright.dev/docs/test-reporters) to learn more about which reporters are available. |
| [testConfig.retries](https://playwright.dev/docs/api/class-testconfig#test-config-retries) | The maximum number of retry attempts per test. See [Test Retries](https://playwright.dev/docs/test-retries) to learn more about retries. |
| [testConfig.testDir](https://playwright.dev/docs/api/class-testconfig#test-config-test-dir) | Directory with the test files. |
| [testConfig.use](https://playwright.dev/docs/api/class-testconfig#test-config-use) | Options with `use{}` |
| [testConfig.webServer](https://playwright.dev/docs/api/class-testconfig#test-config-web-server) | To launch a server during the tests, use the `webServer` option |
| [testConfig.workers](https://playwright.dev/docs/api/class-testconfig#test-config-workers) | The maximum number of concurrent worker processes to use for parallelizing tests. Can also be set as percentage of logical CPU cores, e.g. `'50%'.`. See [Parallelism](https://playwright.dev/docs/test-parallel) and [Sharding](https://playwright.dev/docs/test-sharding) for more details. |

##### Filtering Tests

Filter tests by glob patterns or regular expressions.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Glob patterns or regular expressions to ignore test files.

  testIgnore: '*test-assets',

  // Glob patterns or regular expressions that match test files.

  testMatch: '*todo-tests/*.spec.ts',

});
```

| Option | Description |
| --- | --- |
| [testConfig.testIgnore](https://playwright.dev/docs/api/class-testconfig#test-config-test-ignore) | Glob patterns or regular expressions that should be ignored when looking for the test files. For example, `'*test-assets'` |
| [testConfig.testMatch](https://playwright.dev/docs/api/class-testconfig#test-config-test-match) | Glob patterns or regular expressions that match test files. For example, `'*todo-tests/*.spec.ts'`. By default, Playwright runs `.*(test|spec).(js|ts|mjs)` files. |

##### Advanced Configuration

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Folder for test artifacts such as screenshots, videos, traces, etc.

  outputDir: 'test-results',

  // path to the global setup files.

  globalSetup: require.resolve('./global-setup'),

  // path to the global teardown files.

  globalTeardown: require.resolve('./global-teardown'),

  // Each test is given 30 seconds.

  timeout: 30000,

});
```

| Option | Description |
| --- | --- |
| [testConfig.globalSetup](https://playwright.dev/docs/api/class-testconfig#test-config-global-setup) | Path to the global setup file. This file will be required and run before all the tests. It must export a single function. |
| [testConfig.globalTeardown](https://playwright.dev/docs/api/class-testconfig#test-config-global-teardown) | Path to the global teardown file. This file will be required and run after all the tests. It must export a single function. |
| [testConfig.outputDir](https://playwright.dev/docs/api/class-testconfig#test-config-output-dir) | Folder for test artifacts such as screenshots, videos, traces, etc. |
| [testConfig.timeout](https://playwright.dev/docs/api/class-testconfig#test-config-timeout) | Playwright enforces a [timeout](https://playwright.dev/docs/test-timeouts) for each test, 30 seconds by default. Time spent by the test function, test fixtures and beforeEach hooks is included in the test timeout. |

##### Expect Options

Configuration for the expect assertion library.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  expect: {

    // Maximum time expect() should wait for the condition to be met.

    timeout: 5000,

    toHaveScreenshot: {

      // An acceptable amount of pixels that could be different, unset by default.

      maxDiffPixels: 10,

    },

    toMatchSnapshot: {

      // An acceptable ratio of pixels that are different to the

      // total amount of pixels, between 0 and 1.

      maxDiffPixelRatio: 0.1,

    },

  },

});
```

| Option | Description |
| --- | --- |
| [testConfig.expect](https://playwright.dev/docs/api/class-testconfig#test-config-expect) | [Web first assertions](https://playwright.dev/docs/test-assertions) like `expect(locator).toHaveText()` have a separate timeout of 5 seconds by default. This is the maximum time the `expect()` should wait for the condition to be met. Learn more about [test and expect timeouts](https://playwright.dev/docs/test-timeouts) and how to set them for a single test. |
| [expect(page).toHaveScreenshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-screenshot-1) | Configuration for the `expect(locator).toHaveScreenshot()` method. |
| [expect(value).toMatchSnapshot()](https://playwright.dev/docs/api/class-snapshotassertions#snapshot-assertions-to-match-snapshot-1) | Configuration for the `expect(locator).toMatchSnapshot()` method. |

### 5. Playwright — Parallelism

- Source: https://playwright.dev/docs/test-parallel
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Introduction

Playwright Test runs tests in parallel. In order to achieve that, it runs several worker processes that run at the same time. By default, **test files** are run in parallel. Tests in a single file are run in order, in the same worker process.

- You can configure tests using [`test.describe.configure`](https://playwright.dev/docs/test-parallel#parallelize-tests-in-a-single-file) to run **tests in a single file** in parallel.
- You can configure **entire project** to have all tests in all files to run in parallel using [testProject.fullyParallel](https://playwright.dev/docs/api/class-testproject#test-project-fully-parallel) or [testConfig.fullyParallel](https://playwright.dev/docs/api/class-testconfig#test-config-fully-parallel).
- To **disable** parallelism limit the number of [workers to one](https://playwright.dev/docs/test-parallel#disable-parallelism).

You can control the number of [parallel worker processes](https://playwright.dev/docs/test-parallel#limit-workers) and [limit the number of failures](https://playwright.dev/docs/test-parallel#limit-failures-and-fail-fast) in the whole test suite for efficiency.

##### Worker processes

All tests run in worker processes. These processes are OS processes, running independently, orchestrated by the test runner. All workers have identical environments and each starts its own browser.

You cannot communicate between the workers. Playwright Test reuses a single worker as much as it can to make testing faster, so multiple test files are usually run in a single worker one after another.

Workers are always shutdown after a [test failure](https://playwright.dev/docs/test-retries#failures) to guarantee pristine environment for following tests.

##### Limit workers

You can control the maximum number of parallel worker processes via [command line](https://playwright.dev/docs/test-cli) or in the [configuration file](https://playwright.dev/docs/test-configuration).

From the command line:

```bash
npx playwright test --workers 4
```

In the configuration file:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Limit the number of workers on CI, use default locally

  workers: process.env.CI ? 2 : undefined,

});
```

##### Disable parallelism

You can disable any parallelism by allowing just a single worker at any time. Either set `workers: 1` option in the configuration file or pass `--workers=1` to the command line.

```bash
npx playwright test --workers=1
```

##### Parallelize tests in a single file

By default, tests in a single file are run in order. If you have many independent tests in a single file, you might want to run them in parallel with [test.describe.configure()](https://playwright.dev/docs/api/class-test#test-describe-configure).

Note that parallel tests are executed in separate worker processes and cannot share any state or global variables. Each test executes all relevant hooks just for itself, including `beforeAll` and `afterAll`.

```js
import { test } from '@playwright/test';

test.describe.configure({ mode: 'parallel' });

test('runs in parallel 1', async ({ page }) => { /* ... */ });

test('runs in parallel 2', async ({ page }) => { /* ... */ });
```

Alternatively, you can opt-in all tests into this fully-parallel mode in the configuration file:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  fullyParallel: true,

});
```

You can also opt in for fully-parallel mode for just a few projects:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // runs all tests in all files of a specific project in parallel

  projects: [

    {

      name: 'chromium',

      use: { ...devices['Desktop Chrome'] },

      fullyParallel: true,

    },

  ]

});
```

##### Serial mode

You can annotate inter-dependent tests as serial. If one of the serial tests fails, all subsequent tests are skipped. All tests in a group are retried together.

note

Using serial is not recommended. It is usually better to make your tests isolated, so they can be run independently.

```js
import { test, type Page } from '@playwright/test';

// Annotate entire file as serial.

test.describe.configure({ mode: 'serial' });

let page: Page;

test.beforeAll(async ({ browser }) => {

  page = await browser.newPage();

});

test.afterAll(async () => {

  await page.close();

});

test('runs first', async () => {

  await page.goto('https://playwright.dev/');

});

test('runs second', async () => {

  await page.getByText('Get Started').click();

});
```

##### Opt out of fully parallel mode

If your configuration applies parallel mode to all tests using [testConfig.fullyParallel](https://playwright.dev/docs/api/class-testconfig#test-config-fully-parallel), you might still want to run some tests with default settings. You can override the mode per describe:

```js
test.describe('runs in parallel with other describes', () => {

  test.describe.configure({ mode: 'default' });

  test('in order 1', async ({ page }) => {});

  test('in order 2', async ({ page }) => {});

});
```

##### Avoiding shared state in parallel tests

Playwright runs tests in separate worker processes, each with its own isolated [BrowserContext](https://playwright.dev/docs/api/class-browsercontext "BrowserContext"), so cookies, storage and in-memory globals are already isolated. Flakiness comes from state that lives _outside_ a single test. Here are recipes for the common cases.

###### Give each test its own backend data

Two tests that create or edit the same record race with each other. Derive a unique identifier from [testInfo.testId](https://playwright.dev/docs/api/class-testinfo#test-info-test-id) so parallel tests never collide:

```js
import { test, expect } from '@playwright/test';

test('creates an order', async ({ page }, testInfo) => {

  const orderId = `order-${testInfo.testId}`;

  await page.goto(`/orders/new?id=${orderId}`);

  await expect(page.getByText(orderId)).toBeVisible();

});
```

If many tests can share one dataset, create it once per worker instead — see [Isolate test data between parallel workers](https://playwright.dev/docs/test-parallel#isolate-test-data-between-parallel-workers).

###### Write to a unique file path

Multiple tests writing the same path clobber each other. [testInfo.outputPath()](https://playwright.dev/docs/api/class-testinfo#test-info-output-path) returns a path scoped to the current test:

```js
import { test } from '@playwright/test';

import fs from 'fs';

test('exports a CSV', async ({ page }, testInfo) => {

  const file = testInfo.outputPath('export.csv');

  await fs.promises.writeFile(file, 'a,b,c', 'utf8');

});
```

###### Keep tests independent

Above all, [keep your tests isolated](https://playwright.dev/docs/writing-tests#test-isolation) from one another. A test that leaks state through a module-level variable or depends on another test's side effects works when tests run in order, but breaks the moment they run in parallel or in a different order. Set up everything a test needs in that test or in a [fixture](https://playwright.dev/docs/test-fixtures#creating-a-fixture), and never rely on another test having run first.

##### Shard tests between multiple machines

Playwright Test can shard a test suite, so that it can be executed on multiple machines. See [sharding guide](https://playwright.dev/docs/test-sharding) for more details.

```bash
npx playwright test --shard=2/3
```

##### Limit failures and fail fast

You can limit the number of failed tests in the whole test suite by setting `maxFailures` config option or passing `--max-failures` command line flag.

When running with "max failures" set, Playwright Test will stop after reaching this number of failed tests and skip any tests that were not executed yet. This is useful to avoid wasting resources on broken test suites.

Passing command line option:

```bash
npx playwright test --max-failures=10
```

Setting in the configuration file:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Limit the number of failures on CI to save resources

  maxFailures: process.env.CI ? 10 : undefined,

});
```

##### Worker index and parallel index

Each worker process is assigned two ids: a unique worker index that starts with 1, and a parallel index that is between `0` and `workers - 1`. When a worker is restarted, for example after a failure, the new worker process has the same `parallelIndex` and a new `workerIndex`.

You can read an index from environment variables `process.env.TEST_WORKER_INDEX` and `process.env.TEST_PARALLEL_INDEX`, or access them through [testInfo.workerIndex](https://playwright.dev/docs/api/class-testinfo#test-info-worker-index) and [testInfo.parallelIndex](https://playwright.dev/docs/api/class-testinfo#test-info-parallel-index).

###### Isolate test data between parallel workers

You can leverage `process.env.TEST_WORKER_INDEX` or [testInfo.workerIndex](https://playwright.dev/docs/api/class-testinfo#test-info-worker-index) mentioned above to isolate user data in the database between tests running on different workers. All tests run by the worker reuse the same user.

Create `playwright/fixtures.ts` file that will [create `dbUserName` fixture](https://playwright.dev/docs/test-fixtures#creating-a-fixture) and initialize a new user in the test database. Use [testInfo.workerIndex](https://playwright.dev/docs/api/class-testinfo#test-info-worker-index) to differentiate between workers.

playwright/fixtures.ts

```js
import { test as baseTest, expect } from '@playwright/test';

import { createUserInTestDatabase, deleteUserFromTestDatabase } from './my-db-utils';

export * from '@playwright/test';

export const test = baseTest.extend<{}, { dbUserName: string }>({

  dbUserName: [async ({ }, use) => {

    const userName = `user-${test.info().workerIndex}`;

    await createUserInTestDatabase(userName);

    await use(userName);

    await deleteUserFromTestDatabase(userName);

  }, { scope: 'worker' }],

});
```

Now, each test file should import `test` from our fixtures file instead of `@playwright/test`.

tests/example.spec.ts

```js
import { test, expect } from '../playwright/fixtures';

test('test', async ({ dbUserName }) => {

});
```

##### Control test order

Playwright Test runs tests from a single file in the order of declaration, unless you [parallelize tests in a single file](https://playwright.dev/docs/test-parallel#parallelize-tests-in-a-single-file).

There is no guarantee about the order of test execution across the files, because Playwright Test runs test files in parallel by default. However, if you [disable parallelism](https://playwright.dev/docs/test-parallel#disable-parallelism), you can control test order by either naming your files in alphabetical order or using a "test list" file.

###### Sort test files alphabetically

When you **disable parallel test execution**, Playwright Test runs test files in alphabetical order. You can use some naming convention to control the test order, for example `001-user-signin-flow.spec.ts`, `002-create-new-document.spec.ts` and so on.

###### Use a "test list" file

warning

Tests lists are discouraged and supported as a best-effort only. Some features such as VS Code Extension and tracing may not work properly with test lists.

You can put your tests in helper functions in multiple files. Consider the following example where tests are not defined directly in the file, but rather in a wrapper function.

feature-a.spec.ts

```js
import { test, expect } from '@playwright/test';

export default function createTests() {

  test('feature-a example test', async ({ page }) => {

  });

}
```

feature-b.spec.ts

```js
import { test, expect } from '@playwright/test';

export default function createTests() {

  test.use({ viewport: { width: 500, height: 500 } });

  test('feature-b example test', async ({ page }) => {

  });

}
```

You can create a test list file that will control the order of tests - first run `feature-b` tests, then `feature-a` tests. Note how each test file is wrapped in a `test.describe()` block that calls the function where tests are defined. This way `test.use()` calls only affect tests from a single file.

test.list.ts

```js
import { test } from '@playwright/test';

import featureBTests from './feature-b.spec.ts';

import featureATests from './feature-a.spec.ts';

test.describe(featureBTests);

test.describe(featureATests);
```

Now **disable parallel execution** by setting workers to one, and specify your test list file.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  workers: 1,

  testMatch: 'test.list.ts',

});
```

note

Do not define your tests directly in a helper file. This could lead to unexpected results because your tests are now dependent on the order of `import`/`require` statements. Instead, wrap tests in a function that will be explicitly called by a test list file, as in the example above.

### 8. Yevhen Laichenkov — TIL: Playwright step decorator for better test reporting

- Source: https://elaichenkov.github.io/posts/til-playwright-step-decorator
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### TIL: Playwright step decorator for better test reporting

16 Feb, 2026

If you use page objects in Playwright, your trace reports can quickly become a wall of low-level actions. You can fix this by wrapping methods with `test.step()`, but doing it manually everywhere is tedious.

Here is a `@step()` decorator that does it automatically:

Full implementation

```
import { test } from "@playwright/test";

type Method<This, Args extends unknown[], Return> = (
  this: This,
  ...args: Args
) => Promise<Return>;

type MethodDecoratorContext<
  This,
  Args extends unknown[],
  Return,
> = ClassMethodDecoratorContext<This, Method<This, Args, Return>>;

function extractParams(fn: Function): string[] {
  const fnStr = fn.toString();
  const argsMatch = fnStr.match(/\(([^)]*)\)/);

  if (!argsMatch?.[1]) return [];

  return argsMatch[1]
    .split(",")
    .map(param => param.trim())
    .filter(Boolean)
    .map(param => param.replace(/=.*$/, "").trim())
    .map(param => param.replace(/^\.\.\./, "").trim());
}

function interpolateParams<Args extends unknown[]>(
  message: string,
  fn: Function,
  args: Args
): string {
  const paramNames = extractParams(fn);

  return message.replace(/\{\{(\w+)\}\}/g, (_, paramName) => {
    const index = paramNames.indexOf(paramName);
    if (index === -1 || index >= args.length) return `{{${paramName}}}`;

    const value = args[index];
    return typeof value === "object" ? JSON.stringify(value) : String(value);
  });
}

export function step<
  This extends { constructor: { name: string } },
  Args extends unknown[],
  Return,
>(message?: string) {
  return (
    value: Method<This, Args, Return>,
    context: MethodDecoratorContext<This, Args, Return>
  ) => {
    const target = value;
    const name = context.name ?? "unknown";

    function replacementMethod(
      this: This,
      ...args: Args
    ): Promise<Return> {
      const defaultName = `${this.constructor.name}.${String(name)}`;
      const stepName = message
        ? interpolateParams(message, target, args)
        : defaultName;

      return test.step(stepName, async () => {
        return await target.call(this, ...args);
      });
    }

    return replacementMethod as Method<This, Args, Return>;
  };
}
```

Now every method decorated with `@step()` shows up as a named step in Playwright traces and reports:

```
class LoginPage {
  @step("Log in with {{username}}")
  async logIn(username: string, password: string) {
    await this.page.getByLabel("Username").fill(username);
    await this.page.getByLabel("Password").fill(password);
    await this.page.getByRole("button", { name: "Sign in" }).click();
  }

  @step()
  async navigateTo() {
    await this.page.goto("/login");
  }
}
```

The `{{paramName}}` syntax uses the parameter names from the function signature, so `{{username}}` resolves to the actual value passed at runtime. This makes trace reports way more readable — instead of seeing a generic step name, you see `"Log in with john.doe"`.

### 10. Vitaliy Haradkou — Testcontainers boilerplate packaged

- Source: https://blog-vitaliharadkous-projects.vercel.app/blog/21-testcontainers
- Retrieved: 2026-08-29
- Firecrawl status: complete

![I Got Tired of Writing the Same Container Boilerplate, So I Packaged It](https://blog-vitaliharadkous-projects.vercel.app/_next/image?url=%2Fblog%2F21-testcontainers%2Fhero.webp&w=3840&q=75)

#### I Got Tired of Writing the Same Container Boilerplate, So I Packaged It

Every time I wrote an integration test that needed a real database, I wrote the same block of code. `beforeAll`, start the container. `afterAll`, stop it. Wrap the variable in a `let` at the top of the file so both hooks can see it. Hope nothing throws between start and stop.

It works. It has always worked. And every time I wrote it, I felt like I was solving a problem that should already be solved.

So I built `@playwright-labs/fixture-testcontainers`.

##### What I was actually tired of

Here is the boilerplate. You have probably written some version of it yourself:

```
import { GenericContainer, StartedTestContainer } from "testcontainers";
import { test } from "@playwright/test";

let container: StartedTestContainer;

test.beforeAll(async () => {
  container = await new GenericContainer("postgres:16")
    .withEnvironment({ POSTGRES_PASSWORD: "secret" })
    .withExposedPorts(5432)
    .start();
});

test.afterAll(async () => {
  await container?.stop();
});

test("insert and select", async () => {
  const port = container.getMappedPort(5432);
});
```

The test is five lines. The infrastructure around it is fourteen. And that is before you add a second container, a network between them, or a health check.

There is also a subtler issue: all tests in the file share the same container instance. That is fine when your tests are purely read-only, but the moment one test inserts a row that another test was not expecting, you have a flaky test that only fails when the two run in a specific order. The instinct is to add cleanup inside each test. The result is tests that are half test, half janitor.

##### Playwright already solved this for browsers

Playwrights fixture system is genuinely one of the nicest testing primitives I have worked with. A fixture declares what it needs, provides something to the test, and cleans up after itself — automatically, even when the test throws. Tests just declare what they want:

```
test("my test", async ({ page, context }) => {
});
```

There was no reason containers should be any harder. The lifecycle is identical: start before the test, stop after, handle failures gracefully.

##### What the package does

It wraps Testcontainers in a Playwright fixture called `useContainer`. The fixture tracks every container you start inside a test and stops them all — in parallel — when the test ends.

```
import { test } from "@playwright-labs/fixture-testcontainers";
import { Wait } from "testcontainers";

test("postgres integration", async ({ useContainer }) => {
  const pg = await useContainer("postgres:16", {
    ports: 5432,
    environment: { POSTGRES_PASSWORD: "secret" },
    waitStrategy: Wait.forLogMessage("ready to accept connections"),
  });

  const port = pg.getMappedPort(5432);
});
```

No `beforeAll`. No `afterAll`. No shared `let` at the top of the file. No cleanup code.

The second fixture, `useContainerFromDockerFile`, handles the case where you are building a custom image during the test:

```
test("my service", async ({ useContainerFromDockerFile }) => {
  const app = await useContainerFromDockerFile("./docker", "Dockerfile", {
    ports: 3000,
    waitStrategy: Wait.forHttp("/health", 3000),
  });
});
```

##### The part I am most happy about: composition

The raw fixture is useful. But the design I cared most about was whether it composed naturally with Playwrights existing extension mechanism.

It does. Because `useContainer` is a Playwright fixture, you can use it as an input to your own fixtures:

```
import { test as base } from "@playwright-labs/fixture-testcontainers";
import { Pool } from "pg";
import { Wait } from "testcontainers";

export const test = base.extend<{ db: Pool }>({
  db: async ({ useContainer }, use) => {
    const container = await useContainer("postgres:16", {
      ports: 5432,
      environment: { POSTGRES_PASSWORD: "secret" },
      waitStrategy: Wait.forLogMessage("ready to accept connections"),
    });

    const pool = new Pool({
      host: container.getHost(),
      port: container.getMappedPort(5432),
      user: "postgres",
      password: "secret",
      database: "postgres",
    });

    await use(pool);
    await pool.end();
  },
});
```

Tests import `test` from your fixtures file and receive a ready-to-use `Pool`. They have no idea Docker is involved:

```
import { test } from "./fixtures";

test("user persists", async ({ db }) => {
  await db.query("INSERT INTO users (name) VALUES ($1)", ["Alice"]);
  const { rows } = await db.query("SELECT name FROM users WHERE name = $1", ["Alice"]);
  expect(rows[0].name).toBe("Alice");
});
```

This is the pattern I wanted. Infrastructure as an implementation detail of the fixture layer, invisible to the tests themselves.

##### The matchers

I also added a set of custom `expect` matchers for `StartedTestContainer`. Before this, asserting on container state meant reaching into the Docker API manually:

```
const client = await getContainerRuntimeClient();
const info = await client.container.inspect(client.container.getById(container.getId()));
expect(info.State.Running).toBe(true);
```

Now it is:

```
import { expect } from "@playwright-labs/fixture-testcontainers";

await expect(container).toBeContainerRunning();
await expect(container).toBeContainerHealthy();
await expect(container).toMatchContainerLogMessage("ready to accept connections");
expect(container).toBeContainerPort(5432);
expect(container).toMatchContainerPortInRange(5432, { min: 1024 });
```

There are 13 matchers in total, covering state, logs, ports, labels, networks, names, and users. All of them support `.not`. The string-based ones accept an optional `Intl.Collator` for locale-aware comparisons, which came up when I was writing tests for a French-locale service:

```
const fr = new Intl.Collator("fr", { sensitivity: "base" });
await expect(container).toMatchContainerLogMessage("bonjour", fr);
```

##### A note on speed

The first objection to real containers is always speed. A Postgres container takes a few seconds to become ready. A mock returns in microseconds.

My answer: yes, and I think this trade-off is almost always worth it.

The goal of a test is to tell you whether the code works. A mock that returns the expected result regardless of what the code sends it is not telling you whether the code works — it is telling you whether the code calls the mock correctly. That is a different and much less useful property.

In practice, Playwright runs tests in parallel across multiple workers. A container that takes five seconds to start adds five seconds to one worker, not to the entire suite. The wall-clock impact is usually smaller than teams expect, and the improvement in confidence in the tests more than compensates.

I wrote this package because I believe real infrastructure in integration tests is the right default, not an edge case for particularly careful teams. Removing the boilerplate was my way of making that default easier to reach for.

##### Installation

```
npm install -D @playwright-labs/fixture-testcontainers testcontainers
```

Requires `@playwright/test >= 1.57.0`, `testcontainers >= 10.0.0`, and Docker running locally or in your CI environment.

The full source, README, and tests are on GitHub as part of [playwright-labs](https://github.com/vitalics/playwright-labs) — a collection of Playwright utilities I have been building over time.

If you have been meaning to replace some mocks with real containers and kept putting it off because of the setup cost, I hope this makes it easier. And if you run into anything unexpected, open an issue.

### 11. Vitaliy Haradkou — Angular-aware selector engine

- Source: https://blog-vitaliharadkous-projects.vercel.app/blog/22-angular-selectors
- Retrieved: 2026-08-29
- Firecrawl status: complete

![I Built an Angular-Aware Playwright Selector Engine](https://blog-vitaliharadkous-projects.vercel.app/_next/image?url=%2Fblog%2F22-angular-selectors%2Fhero.webp&w=3840&q=75)

#### I Built an Angular-Aware Playwright Selector Engine

I've been writing Angular E2E tests for a while, and one thing has always bothered me: the tests I write bear no resemblance to the app I'm testing.

Angular apps are built from components with typed inputs, signal state, and event outputs. But Playwright tests still query the DOM like it's 2010 — CSS selectors, `data-testid` attributes, nth-child tricks. The component model is completely invisible to the test layer.

So I built `@playwright-labs/selectors-angular`.

##### The Itch

Here's a button component:

```
@Component({
  selector: "app-button",
  template: `
    <button [disabled]="disabled" [class]="type">
      {{ label }}
    </button>
  `,
})
export class ButtonComponent {
  @Input() label = "";
  @Input() disabled = false;
  @Input() type: "primary" | "secondary" | "danger" = "primary";
  @Output() clicked = new EventEmitter<void>();
}
```

The test I was writing:

```
const deleteBtn = page.locator('button.danger[disabled]');
```

That test is asserting things about the template. The `danger` class is an implementation detail. So is using a `<button>` element, so is the `disabled` attribute. If I refactor the template to use a `<div>` with ARIA roles (for whatever reason), the test breaks — even though the component behavior is identical.

What I wanted to write:

```
const deleteBtn = page.locator('angular=app-button[type="danger"][disabled]');
```

This queries the _component_, not the DOM. It will survive any template refactor that preserves the component's inputs and behavior.

##### The Secret Ingredient: `window.ng`

Angular ships a DevTools API in development builds. In your browser console, type `ng` and you'll see it — a collection of functions for inspecting the component tree:

```
window.ng.getComponent(element)
window.ng.getDirectives(element)
window.ng.getHostElement(component)
window.ng.getOwningComponent(element)
```

This API is what makes the whole thing possible. From a given DOM element, I can get the Angular component instance, read its actual `@Input()` property values, and decide whether the element matches the selector.

##### Building the Selector Engine

Playwright has a [custom selector engine API](https://playwright.dev/docs/extensibility). You register an engine with a name, and then `page.locator('engineName=<query>')` will call your engine to evaluate the query.

The engine needs to implement one method: `queryAll(scope, selector)` — given a DOM element as the search scope and the selector string, return all matching elements.

###### Parsing the Selector

The first challenge was parsing the selector syntax. I wanted to support CSS-like attribute selectors:

```
app-button[label="Submit"][type^="prim"]
```

I wrote a recursive descent parser. It's not fancy but it handles everything I need:

- Quoted strings (`"..."` , `'...'`)
- Unquoted booleans and numbers (`true`, `false`, `42`)
- Regular expressions (`/pattern/flags`)
- Dot-notation property paths (`user.role`)
- All 7 CSS attribute operators (`=`, `*=`, `^=`, `$=`, `|=`, `~=`, and bare truthy)
- Case-insensitive flag (`i`)

###### Walking the Component Tree

```
function buildComponentsAngularTree(root: Element): AngularNode[] {
  const nodes: AngularNode[] = [];
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT);

  let node: Element | null = root;
  while (node) {
    const component = window.ng.getComponent(node);
    if (component) {
      nodes.push({ element: node, component });
    }

    if (node.shadowRoot) {
      nodes.push(...buildComponentsAngularTree(node.shadowRoot as any));
    }

    node = walker.nextNode() as Element | null;
  }

  return nodes;
}
```

###### Matching Component Properties

Once I have the component instance, I evaluate each attribute condition against it. Dot notation traversal:

```
function matchesComponentAttribute(component: object, attr: AttributeSelectorPart): boolean {
  const path = attr.name.split(".");
  let value: unknown = component;

  for (const key of path) {
    if (value == null || typeof value !== "object") return false;
    value = (value as Record<string, unknown>)[key];
  }

  return matchesAttributePart(value, attr);
}
```

Then for the actual operator matching:

```
function matchesAttributePart(value: unknown, attr: AttributeSelectorPart): boolean {
  const { operator, expected, caseSensitive } = attr;

  if (operator === "<truthy>") return Boolean(value);

  const normalize = (v: string) => caseSensitive ? v : v.toLowerCase();
  const actual = String(value);
  const exp = expected instanceof RegExp ? expected : normalize(String(expected));

  switch (operator) {
    case "=":  return exp instanceof RegExp ? exp.test(actual) : normalize(actual) === exp;
    case "*=": return normalize(actual).includes(exp as string);
    case "^=": return normalize(actual).startsWith(exp as string);
    case "$=": return normalize(actual).endsWith(exp as string);
    case "~=": return normalize(actual).split(/\s+/).includes(exp as string);
    case "|=": return normalize(actual) === exp || normalize(actual).startsWith(`${exp}-`);
    default:   return false;
  }
}
```

##### The `$ng` Fixture

Beyond the selector engine, I wanted a way to read component state directly in test code — without routing through the DOM. The `NgHtmlElement` class wraps a Playwright `Locator` and adds Angular-specific methods:

```
class NgHtmlElement {
  async input<T>(name: string): Promise<T> {
    return this.locator.evaluate(withNg((ng, el, propName) => {
      const comp = ng.getComponent(el);
      if (!comp) throw new Error("Not an Angular component");

      const value = (comp as any)[propName];
      if (value === undefined) {
        const available = Object.keys(comp).join(", ");
        throw new Error(`Input "${propName}" not found. Available: ${available}`);
      }

      if (typeof value === "function" && typeof value.set === "function") {
        return value();
      }

      return value;
    }), name) as Promise<T>;
  }
}
```

The `withNg` helper is a small trick to inject `window.ng` into browser-side evaluation functions. Since `locator.evaluate()` serializes functions as strings, I need to make sure `window.ng` isn't captured by closure — it needs to be read fresh in the browser context on each call.

##### Signal Detection

Angular 17 introduced signal-based inputs (`input()`) alongside traditional `@Input()`. Detecting these requires reading the component's internal `ɵcmp` definition:

```
const compDef = constructor['ɵcmp'];
const inputFlags = compDef?.inputs?.[propName];
const isSignalInput = Array.isArray(inputFlags)
  && typeof inputFlags[1] === "number"
  && (inputFlags[1] & 1) !== 0;
```

For `WritableSignal` (from `signal()`), detection is simpler — just check for `.set()` and `.update()` methods:

```
const isWritableSignal = typeof value === "function"
  && typeof value.set === "function"
  && typeof value.update === "function";
```

Both get unwrapped by calling them as functions: `value()`.

##### What I Learned

**Custom Playwright engines are surprisingly powerful.** The API is simple but the surface area is large — you can do anything a browser script can do, and Playwright passes the scope element so you can do proper subtree queries.

**The Angular DevTools API is more useful than I expected.** It was designed for browser DevTools extensions but it's perfectly suited for testing tools too. The fact that it's available in dev mode and covers components, directives, signals, and host elements is exactly what you need.

**Timing tests are hard on CI.** While building the sibling `fixture-timers` package I learned that timer-based assertions need generous tolerances — CI machines have jitter, and a `setTimeout(100)` might resolve in 85ms under load.

##### Try It

```
npm install -D @playwright-labs/selectors-angular
```

```
import { test, expect } from "@playwright-labs/selectors-angular";

test("my first Angular-aware test", async ({ page, $ng }) => {
  await page.goto("/");

  const btn = page.locator('angular=app-button[label="Submit"]');
  await expect(btn).toBeVisible();

  const label = await $ng("app-button").first().input<string>("label");
  expect(label).toBe("Submit");
});
```

The full package is in [playwright-labs](https://github.com/vitalics/playwright-labs). Feedback and contributions welcome.

### 12. Vitaliy Haradkou — Type-safe SQL in Playwright tests

- Source: https://blog-vitaliharadkous-projects.vercel.app/blog/23-pw-sql
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Type-safe SQL in Playwright tests: a compile-time FSM, a branded string, and zero leaked connections](https://blog-vitaliharadkous-projects.vercel.app/_next/image?url=%2Fblog%2F23-pw-sql%2Fhero.webp&w=3840&q=75)

#### Type-safe SQL in Playwright tests: a compile-time FSM, a branded string, and zero leaked connections

Playwright is excellent at browser automation. But a substantial portion of interesting tests also need a database — seeding rows before a test, verifying mutations after one, or testing stored procedures directly.

The conventional approach is either to mock the database (fast but hollow) or to manage connections manually in `beforeEach` / `afterEach` hooks (correct but tedious, and leak-prone when a test throws).

This post documents how we built a three-package system that:

1. Validates SQL structure at **compile time** via a TypeScript finite-state-machine
2. Infers the exact **parameter tuple** from `?` / `$N` placeholders as a phantom type
3. Wires a **real database connection** into Playwrights fixture system with automatic lifecycle management
4. Provides **editor autocomplete, diagnostics, and hover** inside `sql` template literals

All three packages are MIT-licensed and live in the `playwright-labs` monorepo.

##### The problem space

Consider this test:

```
test("fetch user by email", async () => {
  const conn = await pg.connect(process.env.DATABASE_URL!);
  try {
    const result = await conn.query(
      "SELECT * FROM usres WHERE email = $1",
      ["alice@example.com"],
    );
    expect(result.rows).toHaveLength(1);
  } finally {
    await conn.end();
  }
});
```

Three problems:

1. `usres` is a typo. TypeScript has no idea — it is just a string.
2. If `conn.query` throws, the `finally` block runs but the test framework may not await it cleanly depending on how errors propagate.
3. This boilerplate repeats in every test file.

We wanted a solution where the typo is caught before the test runner starts, where connection cleanup is guaranteed, and where the per-test setup is a single `test.use()` call.

##### Part 1 — The type system (`sql-core`)

###### Template literal types as an FSM

TypeScript template literal types support string pattern matching and recursive conditional types. Together they are expressive enough to model a grammar.

We encode a simplified SQL grammar as a finite-state machine where each `State*` type is a node and conditional branches are transitions:

```
type ParseSQL<S extends string> =
  Uppercase<Head<S>> extends "SELECT"
    ? StateAfterSELECT<Tail<S>, S>
    : Uppercase<Head<S>> extends "UPDATE"
      ? StateAfterUPDATE<Tail<S>, S>
      : Uppercase<Head<S>> extends "DELETE"
        ? StateAfterDELETE<Tail<S>, S>
        : Uppercase<Head<S>> extends "INSERT"
          ? StateAfterINSERT<Tail<S>, S>
          : Uppercase<Head<S>> extends "CREATE"
            ? StateAfterCREATE<Tail<S>, S>
            : never;
```

`Head<S>` extracts the first whitespace-delimited token. `Tail<S>` returns everything after it. The FSM walks the token stream and either reaches an accepting state (returning the parameter tuple) or `never`.

```
type StateAfterSELECT<S extends string, Full extends string> = S extends ""
  ? never
  : Uppercase<Head<S>> extends "FROM"
    ? StateAfterFROM<Tail<S>, Full>
    : StateAfterSELECT<Tail<S>, Full>;
```

###### Parameter extraction

Once the FSM reaches a valid accepting state it counts parameters. We support both styles:

**`?` positional (MySQL/SQLite):**

```
type CountParams<
  S extends string,
  Acc extends unknown[] = [],
> = S extends `${string}?${infer Rest}`
  ? CountParams<Rest, [...Acc, unknown]>
  : Acc;
```

**`$N` numbered (PostgreSQL):**

We walk candidates `"1"` through `"20"`, find the highest present index, convert it to a tuple length, and validate that all lower indices are also present — no gaps allowed.

```
type ValidateDollarSequential<S extends string, N extends number> = N extends 0
  ? true
  : HasDollarParam<S, `${N}`> extends true
    ? ValidateDollarSequential<S, MinusOne<N>>
    : false;
```

`MinusOne<N>` is a separate utility type that decrements a numeric literal using digit-by-digit arithmetic in the type system.

###### The public API

```
export type SQLParams<S extends string> =
  ParseSQL<Trim<S>> extends never
    ? never
    : HasAnyDollarParam<Trim<S>> extends true
      ? ValidateDollarSequential<
          Trim<S>,
          StringToNumber<MaxDollarIndex<Trim<S>>>
        > extends true
        ? CountDollarParams<Trim<S>>
        : never
      : CountParams<Trim<S>>;
```

| Input | `SQLParams<S>` |
| --- | --- |
| `'SELECT * FROM users WHERE id = ?'` | `[unknown]` |
| `'UPDATE t SET a = $1, b = $2 WHERE id = $3'` | `[unknown, unknown, unknown]` |
| `'SELECT *'` | `never` (missing FROM) |
| `'SELECT * FROM t WHERE id = $3'` | `never` (1 and 2 missing) |

###### `SqlStatement<P>` — the phantom brand

```
export type SqlStatement<P extends readonly unknown[] = readonly unknown[]> =
  string & { readonly __sqlBrand: P };
```

A string-keyed property rather than a `unique symbol` so the brand survives `tsup` type bundling across `.d.ts` chunks.

The `SqlClient` interface uses `_PlainOrEmpty` as the fallback overload parameter to prevent `SqlStatement<[unknown]>` from silently falling through when params are missing.

###### The `sql` function

Tagged template literal `strings` arguments are always widened to `TemplateStringsArray` by TypeScript, which prevents literal-type inference. Use `sql("…")` or `sql(["…"])` when you want compile-time validation. Use the tagged form for editor syntax highlighting.

###### `AsyncDisposable`

`SqlClient` implements the TC39 explicit resource management protocol:

```
interface SqlClient {
  close(): Promise<void>;
  [Symbol.asyncDispose](): Promise<void>;
}
```

```
await using client = await sqliteAdapter(":memory:").create();
```

##### Part 2 — The Playwright fixture (`fixture-sql`)

The fixture creates one `SqlClient` per test via the configured `SqlAdapter`, registers it for teardown, and exposes it as the `sql` fixture. The teardown phase runs even when a test throws.

```
import { test, expect } from "@playwright-labs/fixture-sql";
import { sqliteAdapter } from "@playwright-labs/fixture-sql/sqlite";
import { sql } from "@playwright-labs/fixture-sql";

test.use({ sqlAdapter: sqliteAdapter(":memory:") });

test("parameterised query", async ({ sql: db }) => {
  await db.execute(sql("INSERT INTO users (name, email) VALUES (?, ?)"), [
    "Alice",
    "alice@example.com",
  ]);

  const { rows } = await db.query<{ name: string }>(
    sql("SELECT name FROM users WHERE email = ?"),
    ["alice@example.com"],
  );

  expect(rows[0]!.name).toBe("Alice");
});
```

###### Multiple connections

```
test("primary replica sync", async ({ useSql }) => {
  const primary = await useSql(pgAdapter(process.env.PRIMARY_URL!));
  const replica = await useSql(pgAdapter(process.env.REPLICA_URL!));

  await primary.execute(sql("INSERT INTO events (type) VALUES ($1)"), [
    "login",
  ]);

  const { rows } = await replica.query(
    sql("SELECT * FROM events WHERE type = $1"),
    ["login"],
  );
  expect(rows).toHaveLength(1);
});
```

###### Adapters

| Package export | Driver | Peer dep |
| --- | --- | --- |
| `@playwright-labs/fixture-sql/sqlite` | `better-sqlite3` | `>=9.0.0` |
| `@playwright-labs/fixture-sql/pg` | `pg` | `>=8.0.0` |
| `@playwright-labs/fixture-sql/mysql` | `mysql2` | `>=3.0.0` |

##### Part 3 — The language server plugin (`ts-plugin-sql`)

The plugin runs inside `tsserver` and intercepts three language service methods.

###### Configuration

```
{
  "compilerOptions": {
    "plugins": [
      {
        "name": "@playwright-labs/ts-plugin-sql",
        "tag": "sql",
        "schemaFile": "./src/db-types.ts"
      }
    ]
  }
}
```

###### Schema loading

The `schemaFile` is a TypeScript file generated by the `pull` CLI.

The plugin parses this file with the TypeScript AST, extracts table and column names, and uses them for completions and hover.

###### Completions

Context-aware based on the preceding SQL token:

- After `FROM`, `JOIN`, `UPDATE`, `INTO` → table names
- After `SELECT`, `WHERE`, `SET`, `AND`, `OR` → column names (filtered to tables in scope)
- Elsewhere → SQL keywords

###### Diagnostics

The plugin runs a plain JS SQL validator on every `sql` template in the file and reports structural errors as TypeScript diagnostics. They appear as squiggly underlines in VS Code without any build step.

###### Hover

```
const q = sql`SELECT id, name FROM users WHERE id = ?`;
```

Hover on `users`: `users — id: number, name: string, email: string | null`

##### Putting it all together

- The `sql("…")` call validates the query at compile time
- The plugin provides completions and hover in the editor
- The fixture manages the connection lifecycle
- The row result is typed as `UsersRow`

##### Installation

```
pnpm add -D @playwright-labs/fixture-sql @playwright-labs/ts-plugin-sql
pnpm add -D better-sqlite3
```

All three packages are MIT-licensed. Source is at [github.com/vitalics/playwright-labs](https://github.com/vitalics/playwright-labs).

### 13. Vitaliy Haradkou — Reporter Slack

- Source: https://blog-vitaliharadkous-projects.vercel.app/blog/24-pw-slack
- Retrieved: 2026-08-29
- Firecrawl status: complete

![@playwright-labs/reporter-slack: Rich Slack Notifications for Playwright Test Runs](https://blog-vitaliharadkous-projects.vercel.app/_next/image?url=%2Fblog%2F24-pw-slack%2Fhero.webp&w=3840&q=75)

#### @playwright-labs/reporter-slack: Rich Slack Notifications for Playwright Test Runs

Today I'm shipping `@playwright-labs/reporter-slack` — a Playwright reporter that sends formatted Slack messages when your test run ends. This post covers everything: what it produces, how to configure it, the three built-in templates, the transport options, and how to write your own template when you need something custom.

##### Why another Slack reporter?

The existing options fall into two categories: they either produce a plain-text message that tells you pass or fail, or they require you to write a bunch of JSON to produce anything richer. Neither is where I wanted to land.

What I wanted:

- **Structured output by default** — test results grouped by status, failure reasons visible inline, no extra log-diving required
- **Interactive elements** — a Slack dropdown filter so anyone in the channel can segment the results without opening a link
- **Environment context** — a table of the env vars that characterize _where_ the run happened, with sensitive values masked automatically
- **Custom templates** — a clean extension point when the built-ins don't fit your workflow

All of this ships in `@playwright-labs/reporter-slack`.

##### Installation and basic setup

```
pnpm add -D @playwright-labs/reporter-slack @playwright-labs/slack-buildkit
```

Minimal config:

```
import { defineConfig } from "@playwright/test";
import { WithOptionsTemplate } from "@playwright-labs/reporter-slack/templates";

export default defineConfig({
  reporter: [
    [
      "@playwright-labs/reporter-slack",
      {
        send: { webhook: process.env.SLACK_WEBHOOK_URL },
        template: WithOptionsTemplate,
      },
    ],
  ],
});
```

Set `SLACK_WEBHOOK_URL` in your environment (CI secret or `.env.local`) and you're done. The reporter accumulates test results as the suite runs, then calls your template in `onEnd` and posts the payload to Slack.

##### What a message looks like

###### Failed run — `WithOptionsTemplate`

Status groups are ordered: failed → timedOut → interrupted → skipped → passed. The first line of each error message appears directly under the test name. The interactive dropdown lets Slack users filter by status in-place. Overflow tests show "…and N more" rather than flooding the channel. The View Full Report button links to your CI artifact (configurable).

###### Failed run — `WithTableTemplate`

Adds an environment context table above the test groups. Sensitive values such as `DB_PASSWORD` and `API_KEY` are auto-masked.

##### The three built-in templates

###### `BaseTemplate`

The simplest template. Header (pass/fail emoji + project name), total count and duration, a list of the failing tests (max 10 by default), and an optional "View Full Report" button.

Good for: small projects, low-noise channels, green runs where you just want confirmation.

###### `WithOptionsTemplate`

Status-grouped template with an interactive `static_select` filter. The filter options are built dynamically from the statuses that actually appear in the run.

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `projectName` | `string` | `"Playwright"` | Displayed in header and context footer |
| `reportUrl` | `string` | — | URL for the "View Full Report" button |
| `maxPerStatus` | `number` | `10` | Max test names shown per status group |
| `showTestNames` | `boolean` | `true` | Show individual test names |
| `show.failed` | `boolean` | `true` | Include the failed group |
| `show.passed` | `boolean` | `true` | Include the passed group |
| `show.skipped` | `boolean` | `true` | Include the skipped group |
| `show.timedOut` | `boolean` | `true` | Include the timedOut group |
| `show.interrupted` | `boolean` | `true` | Include the interrupted group |

###### `WithTableTemplate`

Same as `WithOptionsTemplate` for the test results section, but adds a GFM table of environment variables at the top.

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `env` | `Record<string, string | undefined>` | — | **Required.** `undefined` values are omitted. |
| `tableTitle` | `string` | `"Environment"` | Heading displayed above the table |
| `mask` | `boolean | string[]` | `true` | Masking strategy |
| `showRunSummary` | `boolean` | `true` | Show totals row above the table |
| `rowsPerChunk` | `number` | `30` | Rows per markdown block before splitting |

##### Auto-masking sensitive variables

The default masking strategy (`mask: true`) replaces the value of any key whose name matches:

```
/token|secret|password|pass(?:word)?|credential|auth|api[_-]?key/i
```

Masking happens before the payload is constructed — the raw value never appears in the Block Kit JSON that gets sent to Slack.

##### Transport options

###### Incoming Webhook

```
send: {
  webhook: process.env.SLACK_WEBHOOK_URL;
}
```

Limitations: you cannot choose the channel (it's fixed on the webhook), and you cannot post replies to threads.

###### Web API (`chat.postMessage`)

```
send: {
  token:   process.env.SLACK_BOT_TOKEN,
  channel: "#ci-reports",
}
```

##### Writing a custom template

A template is a function with this signature:

```
type SlackTemplate = (
  result: FullResult,
  testCases: [TestCase, TestResult][],
) => SlackBlock[];
```

The reporter also ships `@playwright-labs/slack-buildkit`, a JSX runtime that lets you write templates as component trees.

##### Testing templates

Because templates return plain JSON arrays, they're easy to test in isolation.

##### Packages

| Package | Purpose |
| --- | --- |
| `@playwright-labs/reporter-slack` | The reporter — templates, transports, Playwright integration |
| `@playwright-labs/slack-buildkit` | JSX runtime and Block Kit component library |

Source: [github.com/vitaliharadkou/playwright-labs](https://github.com/vitaliharadkou/playwright-labs)

### 21. Sajith Dilshan — Locator strategy

- Source: https://medium.com/@sajith-dilshan/playwright-locator-strategy-choosing-the-right-locator-for-stable-and-maintainable-test-automation-018a4fd0e16c
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Locator Strategy: Choosing the Right Locator for Stable and Maintainable Test Automation

As QA engineers, we have all experienced that frustrating moment when a test that passed yesterday suddenly fails today. After spending time investigating, we discover that a developer changed a CSS class name, modified the DOM structure, or added an extra container element. The test was not broken because the feature stopped working — it was broken because the locator was fragile.

One of the biggest advantages of Playwright is that it encourages us to write tests the same way users interact with applications. By choosing the right locator strategy, we can significantly reduce flaky tests and improve the maintainability of our automation framework.

In this article, we will explore Playwright recommended locator hierarchy, understand why certain locators are preferred over others, and discuss practical recommendations for enterprise-level automation projects.

##### Why Locator Selection Matters

A locator is how your test finds an element on a page.

A good locator should be:

- Stable over time
- Easy to read and understand
- Closely aligned with user behavior
- Resistant to UI implementation changes

Poor locator choices often result in:

- Frequent test maintenance
- Flaky test execution
- Longer debugging sessions
- Reduced confidence in automation results

The goal is not simply to make a test pass. The goal is to create automation that remains reliable as the application evolves.

##### 1. getByRole() — The Gold Standard

Playwright recommends `getByRole()` as the preferred locator strategy.

Example:

```
await page.getByRole('button', { name: 'Submit' }).click();
```

Why it is great:

- Mirrors how assistive technologies identify elements
- Encourages accessibility best practices
- Highly readable
- Usually survives UI refactoring

Instead of saying:

```
await page.locator('.btn-primary').click();
```

you are effectively saying:

> "Click the Submit button."

That is exactly how a user thinks.

##### 2. getByLabel() — Perfect for Forms

```
await page.getByLabel('Email').fill('test@example.com');
```

Benefits:

- Easy to understand
- Works naturally with accessible forms
- Less likely to break than CSS selectors

##### 3. getByPlaceholder()

```
await page.getByPlaceholder('Enter your email').fill('test@example.com');
```

While placeholders are not as reliable as labels, they are still more user-focused than implementation-based selectors.

##### 4. getByText()

```
await page.getByText('Forgot Password').click();
```

Useful for links, navigation items, informational content, and validation messages. Be cautious: product teams frequently update wording.

##### 5. getByAltText()

```
await page.getByAltText('Company Logo');
```

##### 6. getByTitle()

```
await page.getByTitle('Settings');
```

##### 7. getByTestId() — The Enterprise Favorite

```
await page.getByTestId('submit-button').click();
```

Why teams love it:

- Extremely stable
- Independent of UI text changes
- Resistant to design updates

##### 8. CSS Selectors — Use Carefully

```
await page.locator('.submit-btn').click();
```

CSS selectors are powerful but fragile.

##### 9. XPath — Last Resort

```
await page.locator('//button[text()="Submit"]').click();
```

XPath still works in Playwright, but it should generally be avoided.

##### Recommended Locator Priority

1. getByRole()
2. getByLabel()
3. getByPlaceholder()
4. getByText()
5. getByAltText()
6. getByTitle()
7. getByTestId()
8. CSS Selectors
9. XPath

##### Real-World Enterprise Recommendation

###### User-Facing Elements

```
await page.getByRole('button', { name: 'Save' }).click();
```

###### Form Fields

```
await page.getByLabel('Password').fill('secret');
```

###### Complex Components

```
await page.getByTestId('customer-grid');
```

###### Fallback Options

Only if necessary: `locator('.class-name')`

###### Last Resort

Use XPath only when no better option exists.

##### Final Thoughts

One of the biggest lessons I have learned as a QA engineer is that successful automation is not about writing more tests — it is about writing maintainable tests.

The best locator is usually the one that represents how a user experiences the application, not how the application is implemented.

Whenever possible:

- Think like a user.
- Prefer accessible locators.
- Use test IDs for complex components.
- Avoid fragile DOM-dependent selectors.

Remember: Good automation is not just automation that passes. It is automation that continues to pass after dozens of releases.

### 22. Sajith Dilshan — Auto-waiting

- Source: https://medium.com/@sajith-dilshan/playwright-auto-waiting-the-secret-behind-stable-and-reliable-test-automation-bd3987a3156e
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Auto-Waiting: The Secret Behind Stable and Reliable Test Automation

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:32:32/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---byline--bd3987a3156e---------------------------------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---byline--bd3987a3156e---------------------------------------)


5 min read

·

Jul 31, 2026

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Dbd3987a3156e&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40sajith-dilshan%2Fplaywright-auto-waiting-the-secret-behind-stable-and-reliable-test-automation-bd3987a3156e&source=---header_actions--bd3987a3156e---------------------post_audio_button------------------)


Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*QzR41sRYMp_SJ1-IwnRSLQ.png)

Playwright Auto-Waiting

One of the biggest challenges in test automation is dealing with timing issues.

If you have worked with Selenium or other automation frameworks, you have probably seen tests fail because an element was not ready when the script tried to interact with it. To solve this, teams often add explicit waits, sleep statements, or custom retry mechanisms.

I still remember writing code like this:

```
await page.waitForTimeout(5000);
await page.click('#submit');
```

It worked sometimes. Other times, it didn’t.

This approach makes tests slower, harder to maintain, and more flaky.

This is where Playwright changes the game.

Playwright comes with a powerful feature called **Auto-Waiting**, which automatically waits for elements to be ready before performing actions. It significantly reduces flaky tests and allows engineers to focus on testing business functionality rather than handling synchronization problems.

In this article, we’ll explore what Auto-Waiting is, how it works behind the scenes, and how both beginners and experienced QA engineers can use it effectively.

##### What is Auto-Waiting?

Auto-Waiting is Playwright’s ability to automatically wait until an element becomes ready for interaction.

Instead of immediately clicking an element, Playwright first checks whether the element is:

- Present in the DOM
- Visible
- Stable
- Able to receive events
- Enabled

Only after these conditions are satisfied will Playwright perform the action.

For example:

```
await page.locator('#login-button').click();
```

At first glance, this looks like a simple click.

However, before clicking, Playwright automatically performs multiple validations to ensure the action will succeed.

This intelligence is one of the reasons Playwright tests are generally more reliable than traditional automation scripts.

##### Why Auto-Waiting Matters

Imagine a real-world scenario:

1. User fills the registration form.
2. Application sends data to the server.
3. Submit button remains disabled while validation occurs.
4. Button becomes enabled after validation completes.

Without Auto-Waiting:

```
await page.click('#submit');
```

The test may fail because the button is still disabled.

With Playwright:

```
await page.locator('#submit').click();
```

Playwright waits until the button becomes actionable before clicking it.

The result?

- Fewer flaky tests
- Less waiting code
- Faster execution
- Better maintainability

##### Understanding Actionability Checks

Before performing an action, Playwright validates the target element using Actionability Checks.

Think of these as safety checks before interacting with an element.

##### 1\. Element Must Be Visible

Playwright verifies that the element can actually be seen by the user.

##### Not Visible Examples

```
<button style="display:none">Submit</button>
```

```
<button style="visibility:hidden">Submit</button>
```

These elements are hidden and cannot be interacted with.

##### Interesting Fact

The following element is considered visible:

```
<button style="opacity:0">Submit</button>
```

Although invisible to the eye, it still occupies space on the page, so Playwright considers it visible.

##### 2\. Element Must Be Stable

Playwright waits for animations or movements to finish.

Imagine a button sliding into view:

```
await page.locator('#menu-button').click();
```

If the button is still moving, Playwright waits until its position becomes stable.

This prevents clicks from happening at the wrong coordinates.

##### 3\. Element Must Receive Events

An element may be visible but still blocked by another component.

Example:

```
<div class="loading-overlay"></div>
<button>Save</button>
```

The button exists and is visible.

However, a loading overlay covers it.

If a real user clicks, the overlay receives the click.

Playwright detects this situation and waits until the button can actually receive the event.

This is one of the most valuable protections against flaky tests.

##### 4\. Element Must Be Enabled

Playwright verifies that the element is not disabled.

Example:

```
<button disabled>Submit</button>
```

Playwright waits until the disabled state disappears.

```
<button>Submit</button>
```

Only then will the click happen.

##### 5\. Element Must Be Editable

Used primarily for input fields.

Example:

```
await page.locator('#username').fill('John');
```

Before typing, Playwright checks:

- Element is visible
- Element is enabled
- Element is editable
- Element is not readonly

Example of a non-editable field:

```
<input readonly />
```

Playwright waits until it becomes editable or times out.

##### Actionability Checks by Action Type

Not every action requires all checks.

For example:

##### Click

```
await locator.click();
```

Checks:

✅ Visible

✅ Stable

✅ Receives Events

✅ Enabled

##### Fill

```
await locator.fill('Playwright');
```

Checks:

✅ Visible

✅ Enabled

##### Hover

```
await locator.hover();
```

Checks:

✅ Visible

✅ Stable

✅ Receives Events

Playwright intelligently applies only the validations necessary for each action.

##### Auto-Waiting for Assertions

Auto-Waiting doesn’t stop at actions.

Assertions automatically retry until the expected condition becomes true.

Example:

```
await expect(page.locator('.success-message'))
    .toBeVisible();
```

Playwright repeatedly checks the element until:

- It becomes visible
- Timeout is reached

No additional wait is required.

##### Traditional Approach

```
await page.waitForTimeout(3000);
```

```
expect(await page.locator('.success').isVisible())
    .toBeTruthy();
```

##### Playwright Approach

```
await expect(
    page.locator('.success')
).toBeVisible();
```

Cleaner.

More reliable.

Easier to maintain.

##### Most Common Auto-Retry Assertions

##### Visibility

```
await expect(locator).toBeVisible();
```

##### Hidden

```
await expect(locator).toBeHidden();
```

##### Enabled

```
await expect(locator).toBeEnabled();
```

##### Disabled

```
await expect(locator).toBeDisabled();
```

##### Text Validation

```
await expect(locator)
    .toHaveText('Success');
```

##### URL Validation

```
await expect(page)
    .toHaveURL('/dashboard');
```

##### Page Title Validation

```
await expect(page)
    .toHaveTitle('Dashboard');
```

These assertions continuously retry until the condition is satisfied.

##### When Should You Use Explicit Waits?

Many engineers moving from Selenium ask:

“Do I still need waits?”

In most situations:

**No.**

Playwright already handles synchronization for actions and assertions.

Avoid:

```
await page.waitForTimeout(5000);
```

This introduces unnecessary delays.

Instead:

```
await expect(locator)
    .toBeVisible();
```

or

```
await page.waitForURL('/dashboard');
```

Use explicit waits only when waiting for something that Playwright cannot automatically detect.

Examples:

- API responses
- File downloads
- Network requests
- Custom business events

##### What About Force Click?

Playwright provides a force option:

```
await locator.click({
  force: true
});
```

This bypasses some actionability checks.

Use it carefully.

While it may help in rare situations, force clicks often hide genuine UI problems.

If Playwright says an element is not clickable, there is usually a valid reason.

A better approach is to investigate the root cause rather than forcing the interaction.

##### Common Mistakes Beginners Make

##### Mistake \#1: Using Hard Waits Everywhere

Bad:

```
await page.waitForTimeout(5000);
```

Good:

```
await expect(locator)
    .toBeVisible();
```

##### Mistake \#2: Mixing Manual Waits With Auto-Waiting

Bad:

```
await page.waitForSelector('#login');
await page.locator('#login')
    .click();
```

Good:

```
await page.locator('#login')
    .click();
```

Playwright already waits.

##### Mistake \#3: Using Force Without Understanding the Issue

Bad:

```
await locator.click({
 force: true
});
```

Good:

Understand why the element is not actionable before bypassing checks.

##### Real-World QA Engineer Perspective

In many automation projects, over 70% of flaky failures are caused by synchronization issues.

Before Playwright, teams spent significant time:

- Adding waits
- Increasing timeouts
- Debugging intermittent failures

With Playwright’s Auto-Waiting mechanism, many of these issues disappear automatically.

As a QA Engineer, this means:

- More stable CI/CD pipelines
- Faster debugging
- Higher confidence in automation results
- Less maintenance effort

Instead of fighting timing problems, you can focus on validating business requirements and improving test coverage.

##### Final Thoughts

Auto-Waiting is one of Playwright’s most powerful and underrated features.

It may seem like a small convenience at first, but once you understand how much work it performs behind the scenes, you realize it is one of the primary reasons Playwright tests are stable and reliable.

My recommendation to every QA engineer learning Playwright is simple:

**Trust the framework.**

Avoid unnecessary sleeps.

Use locators properly.

Leverage auto-retrying assertions.

Let Playwright handle synchronization while you focus on testing user behavior.

The less time you spend fighting timing issues, the more time you can spend building high-quality automation that delivers real value to your team.

[Playwright Test](https://medium.com/tag/playwright-test?source=post_page---footer_tags--bd3987a3156e---------------------------------------)

[Playwright Automation](https://medium.com/tag/playwright-automation?source=post_page---footer_tags--bd3987a3156e---------------------------------------)

[Test Automation](https://medium.com/tag/test-automation?source=post_page---footer_tags--bd3987a3156e---------------------------------------)

[Auto Waiting](https://medium.com/tag/auto-waiting?source=post_page---footer_tags--bd3987a3156e---------------------------------------)

[Software Testing](https://medium.com/tag/software-testing?source=post_page---footer_tags--bd3987a3156e---------------------------------------)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:48:48/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---post_author_info--bd3987a3156e---------------------------------------)

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:64:64/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---post_author_info--bd3987a3156e---------------------------------------)

### 23. Sajith Dilshan — Scalable authentication

- Source: https://medium.com/@sajith-dilshan/scalable-authentication-in-playwright-why-globalsetup-falls-short-644cf0fb4db4
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Scalable Authentication in Playwright: Why globalSetup Falls Short

[![sajith dilshan](https://miro.medium.com/v2/resize:fill:32:32/1*CFWD6Nuoj_R3Sw7QRIrWRg.jpeg)](https://medium.com/@sajith-dilshan?source=post_page---byline--644cf0fb4db4---------------------------------------)

[sajith dilshan](https://medium.com/@sajith-dilshan?source=post_page---byline--644cf0fb4db4---------------------------------------)


7 min read

·

Jul 17, 2026

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D644cf0fb4db4&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40sajith-dilshan%2Fscalable-authentication-in-playwright-why-globalsetup-falls-short-644cf0fb4db4&source=---header_actions--644cf0fb4db4---------------------post_audio_button------------------)


##### A Senior QA Engineer’s Guide to Modern Authentication Architecture

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*8hwLxVWUCMwDlWmnpoOvjg.png)

Scalable Authentication in Playwright: Why globalSetup Falls Short

When I first started building Playwright automation frameworks, `globalSetup` felt like the perfect solution for authentication reuse.

Simple. Fast. Easy.

Log in once → save the session → reuse it across all tests.

And honestly, for small projects, it works perfectly fine.

But as our automation framework grew into a real enterprise-level system with:

- multiple user roles
- CI/CD pipelines
- parallel execution
- flaky environments
- trace debugging
- large QA teams
- distributed test execution

the problems with `globalSetup` slowly started becoming visible.

Authentication failures became harder to debug.

CI pipelines became less stable.

Multi-role authentication became messy.

And eventually I realized something important:

> `globalSetup` _runs outside the normal Playwright test runner lifecycle._

That single architectural detail explains almost every limitation teams eventually experience with authentication reuse in Playwright.

In this article, I’ll explain:

- what `globalSetup` actually is
- how authentication reuse works
- what the Playwright test runner lifecycle means
- why `globalSetup` becomes problematic at scale
- why Playwright officially recommends setup projects now
- and how to build a modern, scalable authentication architecture properly

This article comes from real-world experience maintaining Playwright frameworks in production environments.

##### Why Authentication Reuse Matters

Without authentication reuse, every test performs UI login repeatedly:

```
Test 1 → Login
Test 2 → Login
Test 3 → Login
Test 4 → Login
```

This creates several problems:

- slower execution
- repeated UI steps
- unnecessary network traffic
- flaky login failures
- longer CI pipelines

As test suites grow, authentication becomes one of the biggest execution bottlenecks.

That’s why Playwright supports saving authenticated session data using:

```
storageState
```

The idea is simple:

1. Authenticate once
2. Save cookies/session/local storage
3. Reuse that state across tests

This dramatically improves:

- speed
- stability
- scalability

##### The Older Popular Solution — `globalSetup`

For years, many Playwright frameworks implemented authentication using:

```
globalSetup
```

Example configuration:

```
import { defineConfig } from '@playwright/test';

export default defineConfig({
  globalSetup: './tests/global-setup.ts',
});
```

##### Example `global-setup.ts`

```
import { chromium } from '@playwright/test';

async function globalSetup() {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto('https://example.com/login');

  await page.fill('#email', 'admin@test.com');
  await page.fill('#password', 'password');

  await page.click('button[type=submit]');

  await page.context().storageState({
    path: 'playwright/.auth/user.json',
  });

  await browser.close();
}

export default globalSetup;
```

Then reuse the saved authentication state:

```
use: {
  storageState: 'playwright/.auth/user.json',
}
```

At first glance, this looks completely reasonable.

And for smaller projects, it often is.

But the architectural problems become obvious as frameworks scale.

##### The Most Important Thing to Understand

The key issue is this:

> `globalSetup` _runs outside the normal Playwright test runner lifecycle._

This is the root cause behind most of its limitations.

##### What Is the Playwright Test Runner Lifecycle?

When Playwright runs tests normally, everything is managed through its internal test execution lifecycle.

Simplified flow:

```
Start Test Runner
        ↓
Create Workers
        ↓
Initialize Fixtures
        ↓
Run Tests
        ↓
Capture Traces/Screenshots/Videos
        ↓
Retry Failed Tests
        ↓
Generate Reports
```

This lifecycle is why Playwright provides such powerful features:

- retries
- trace viewer
- screenshots
- videos
- HTML reports
- fixture management
- worker isolation
- UI mode debugging
- reporter integration

These capabilities work because Playwright controls the execution inside the test runner lifecycle.

##### The Architectural Problem with `globalSetup`

`globalSetup` executes before the Playwright runner fully enters that lifecycle.

Which means:

- it is not treated like a real test
- retries behave differently
- reporting visibility is limited
- debugging becomes harder
- traces are not integrated the same way
- fixture lifecycle integration is weak

This may not matter in small projects.

But it matters a lot in enterprise automation frameworks.

##### The Real Drawbacks I Experienced with `globalSetup`

##### 1\. Authentication Failures Became Harder to Debug

This was the first major pain point.

Sometimes login failed because:

- environment was slow
- auth service lagged
- redirect timing changed
- sessions expired unexpectedly

With normal Playwright tests, debugging is fantastic.

But with `globalSetup`, failures often looked like:

```
Error: page.goto failed
```

without the same level of:

- trace visibility
- reporting integration
- debugging clarity

In CI pipelines, this becomes extremely frustrating.

##### 2\. No Proper Retry Lifecycle

Retries are one of Playwright’s best features.

Normal tests can automatically retry:

```
Retry #1
Retry #2
```

But `globalSetup` is not a normal Playwright test.

If authentication fails:

- the entire execution may stop
- retries are not managed naturally
- pipelines become fragile

In real-world environments, temporary failures happen constantly.

Authentication should be resilient.

##### 3\. Authentication Was Invisible in Reports

This sounds minor until multiple QA engineers work on the same framework.

Authentication setup doesn’t appear like a normal test in reports.

You cannot clearly see:

```
✓ authenticate admin
```

inside Playwright HTML reports.

This reduces:

- visibility
- traceability
- debugging clarity

especially in larger teams.

##### 4\. Multi-Role Authentication Became Messy

Enterprise systems rarely have one user role.

We had:

- Admin
- Office Manager
- Property Manager
- Tenant

Eventually our `globalSetup` started looking like this:

```
await adminLogin();
await officeManagerLogin();
await propertyManagerLogin();
await tenantLogin();
```

Over time:

- auth dependencies increased
- execution slowed down
- maintenance became painful
- role separation became unclear

The authentication layer became centralized and difficult to scale.

##### 5\. Poor Integration with Playwright Worker Architecture

Playwright runs tests using isolated worker processes for parallel execution.

Setup projects integrate naturally with this architecture.

`globalSetup` does not.

This becomes important when:

- tests run in parallel
- workers restart
- sharding is enabled
- distributed execution happens in CI

Modern Playwright frameworks should align with worker isolation architecture.

##### 6\. Limited Fixture Integration

Senior Playwright frameworks rely heavily on fixtures.

##### 7\. Selective Test Execution Becomes Less Efficient

With setup projects:

```
npx playwright test tests/admin
```

Playwright only runs the required authentication dependency.

But with a large centralized `globalSetup`, unnecessary authentication logic may run even when not needed.

This matters in large CI pipelines.

##### 8\. CI/CD Pipelines Became Harder to Maintain

In real enterprise CI environments:

- GitHub Actions
- Jenkins
- Azure DevOps
- GitLab CI

authentication failures should behave like proper test failures.

But `globalSetup` failures often feel like startup crashes instead.

This creates:

- poorer reporting
- harder debugging
- unstable pipelines

##### 9\. Authentication Expiration Handling Became Painful

Real environments reset often:

- sessions expire
- tokens become invalid
- environments restart

With setup projects:

- auth states regenerate independently
- roles refresh separately
- failures isolate better

With centralized `globalSetup`, auth refresh logic becomes harder to manage cleanly.

##### What Playwright Officially Recommends Now

Playwright documentation now recommends using:

> _Dedicated setup projects with project dependencies and_`storageState`

instead of relying primarily on `globalSetup` for authentication reuse.

Official documentation:

[https://playwright.dev/docs/auth](https://playwright.dev/docs/auth)

This modern architecture integrates authentication directly into Playwright’s testing system.

And that makes a huge difference.

##### The Modern Recommended Architecture

Instead of procedural startup logic, authentication becomes a real Playwright test project.

##### Example Modern Configurationjj

```
import { defineConfig } from '@playwright/test';

export default defineConfig({
  projects: [\
    {\
      name: 'setup',\
      testMatch: /.*\.setup\.ts/,\
    },\
\
    {\
      name: 'chromium',\
      dependencies: ['setup'],\
\
      use: {\
        storageState: 'playwright/.auth/user.json',\
      },\
    },\
  ],
});
```

##### Example Authentication Setup Test

```
import { test as setup } from '@playwright/test';

setup('authenticate', async ({ page }) => {
  await page.goto('https://example.com/login');

  await page.fill('#email', 'admin@test.com');
  await page.fill('#password', 'password');

  await page.click('button[type=submit]');

  await page.context().storageState({
    path: 'playwright/.auth/user.json',
  });
});
```

Now authentication becomes:

- visible
- retryable
- traceable
- reportable
- maintainable

because it lives inside Playwright’s architecture.

##### Why Setup Projects Feel Much Better in Real Frameworks

##### Authentication Appears in Reports

Now you can clearly see:

```
✓ authenticate
```

inside Playwright reports.

This dramatically improves debugging visibility.

##### Retries Work Properly

If login fails temporarily:

```
Retry #1
Retry #2
```

Playwright handles it naturally.

CI pipelines become much more stable.

##### Full Trace Viewer Support

This is one of the biggest improvements.

Authentication setup now includes:

- traces
- screenshots
- videos
- network activity
- console logs

Debugging authentication failures becomes dramatically easier.

##### Multi-Role Authentication Scales Cleanly

Instead of one giant setup file:

```
tests/
├── auth/
│   ├── admin.setup.ts
│   ├── office-manager.setup.ts
│   └── property-manager.setup.ts
```

Auth states remain isolated:

```
tests/
├── .auth/
│   ├── admin.json
│   ├── office-manager.json
│   └── property-manager.json
```

This architecture scales far better for enterprise systems.

##### Recommended Folder Structure

Here’s the structure I personally prefer for scalable Playwright frameworks:

```
tests/
├── .auth/
│
├── auth/
│
├── fixtures/
│
├── specs/
│   ├── admin/
│   ├── office-manager/
│   └── property-manager/
│
├── pages/
│   ├── common/
│   ├── admin/
│   ├── office-manager/
│   └── property-manager/
```

This keeps:

- authentication isolated
- role responsibilities separated
- page objects maintainable
- fixtures scalable

##### Security Best Practice

Never commit authentication state files into Git repositories.

Add this to `.gitignore`:

```
playwright/.auth/
tests/.auth/
```

Because these files may contain:

- session cookies
- authentication tokens
- user session data

Committing them creates security risks.

##### An Even Better Optimization — API-Based Authentication

Once teams mature their framework architecture further, many move away from UI login completely.

Instead of:

```
Open Login Page → Fill Form → Click Login
```

they authenticate directly through APIs.

Example:

```
const response = await request.post('/api/login', {
  data: {
    email: 'admin@test.com',
    password: 'password',
  },
});
```

Then save the authenticated state.

Benefits:

- dramatically faster
- more stable
- less dependent on UI
- better suited for CI execution

This is extremely common in advanced Playwright frameworks.

##### Is `globalSetup` Completely Bad?

No.

I still occasionally use `globalSetup` for:

- database seeding
- mock server startup
- environment preparation
- feature flag initialization

For small projects or proof-of-concepts, it’s completely acceptable.

But for scalable authentication reuse?

I no longer recommend it.

##### Final Thoughts

The goal of a modern automation framework is not just to make tests pass.

It’s to make:

- failures observable
- debugging efficient
- CI pipelines reliable
- long-term maintenance sustainable

And that’s why authentication should live inside Playwright’s test architecture — not outside of it.

For enterprise Playwright frameworks, my recommendation is clear:

```
Setup Projects + storageState + Project Dependencies
```

because this approach:

- integrates naturally with Playwright’s lifecycle
- scales better
- improves debugging
- supports retries
- works properly with traces and reports
- aligns with Playwright’s official recommendation

Most importantly:

> _it makes your framework easier to maintain six months later — not just easier to write today._

##### Official References

##### Playwright Authentication

[https://playwright.dev/docs/auth](https://playwright.dev/docs/auth)

##### Playwright Projects

[https://playwright.dev/docs/test-projects](https://playwright.dev/docs/test-projects)

##### Global Setup and Teardown

[https://playwright.dev/docs/test-global-setup-teardown](https://playwright.dev/docs/test-global-setup-teardown)

##### Playwright Trace Viewer

[https://playwright.dev/docs/trace-viewer](https://playwright.dev/docs/trace-viewer)

### 24. Sajith Dilshan — Why tsconfig.json matters

- Source: https://medium.com/@sajith-dilshan/why-tsconfig-json-matters-in-a-playwright-typescript-project-and-why-its-often-missing-1f8c99b598fc
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Why `tsconfig.json` Matters in a Playwright TypeScript Project (and Why It’s Often Missing)

When working with **Playwright and TypeScript**, one of the most confusing observations for developers is this:

> “My project works without `tsconfig.json` … so why do I even need it?”

At first glance, Playwright seems to work perfectly without any configuration. Tests run, TypeScript files execute, and everything appears fine.

But in real-world automation frameworks, `tsconfig.json` is not optional—it is foundational.

##### Why Playwright Does NOT Create `tsconfig.json`

When you initialize a Playwright project:

```
npm init playwright@latest
```

you may notice that a `tsconfig.json` file is not created.

###### Zero-config philosophy

Playwright is designed for simplicity: Install → Write tests → Run immediately. To achieve this, it avoids forcing configuration files.

###### Internal TypeScript handling

Playwright internally runs on Node.js, transpiles TypeScript automatically, and uses default compiler settings. So `.ts` files work even without explicit configuration.

##### How Playwright Works Without `tsconfig.json`

Even without a configuration file, VS Code applies default TypeScript settings, Node.js executes compiled code, and the Playwright test runner handles TypeScript internally. Under the surface TypeScript runs in implicit mode: no strict rules are enforced.

##### Problems With Missing `tsconfig.json`

- Missing Node.js type definitions
- No strict type safety
- Inconsistent behavior across teams
- Poor scalability
- Harder debugging

##### Why You SHOULD Add `tsconfig.json`

Adding a `tsconfig.json` transforms your project into a production-ready automation framework: Node.js APIs, strong type safety, consistent team behavior, better debugging, required for scalable frameworks.

##### How to Create `tsconfig.json`

```
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "types": ["node"]
  },
  "include": ["**/*.ts"]
}
```

```
npm install --save-dev @types/node
```

##### Final Thoughts

While Playwright allows TypeScript to work without configuration, this approach is only suitable for quick starts — not production systems. In professional Playwright automation engineering, `tsconfig.json` is not optional—it is essential.

### 25. Sajith Dilshan — Module systems

- Source: https://medium.com/@sajith-dilshan/understanding-typescript-module-systems-for-playwright-commonjs-vs-es-modules-e2a8caffa328
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Understanding TypeScript Module Systems for Playwright: CommonJS vs ES Modules

When building a TypeScript-based Playwright framework, one of the most confusing topics for beginners is module systems: why sometimes imports need `.js`, why certain tsconfig.json settings break your project, whether to use CommonJS or ES Modules, and whether you need ts-node.

##### Core Concept

TypeScript is your language, not your runtime. You write `.ts` files. TypeScript compiles them to `.js`. Node.js runs the `.js` files.

##### CommonJS (CJS)

The traditional Node.js module system and default setup for Playwright.

```
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "CommonJS",
    "moduleResolution": "node",
    "strict": true,
    "esModuleInterop": true,
    "resolveJsonModule": true,
    "types": ["node", "@playwright/test"]
  }
}
```

Import style: `import { TAGS } from '../constants/tags';` — no `.js`. Easy for beginners, works out of the box. Not aligned with the modern ES module standard.

##### ES Modules (ESM / NodeNext)

```
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "esModuleInterop": true,
    "resolveJsonModule": true
  }
}
```

package.json: `{ "type": "module" }`

Import style: `import { TAGS } from '../constants/tags.js';` — must use `.js` even though the source is `tags.ts`.

Disadvantages: confusing for beginners, `__dirname` is not available (use `import.meta.url`), JSON imports need `assert { type: "json" }`.

##### Common Mistakes

Mixing `"module": "ESNext"` with `"moduleResolution": "node"` breaks ESM.

##### What About ts-node?

Playwright automatically compiles and runs TypeScript. You do not need ts-node for tests. Use it only for standalone scripts.

##### Final Recommendations

Beginners: TypeScript + CommonJS, no `.js` imports. Advanced: TypeScript + ES Modules (NodeNext). CommonJS is simplicity and stability; ESM is modern, explicit, and future-proof.

### 26. Sajith Dilshan — Hooks

- Source: https://medium.com/@sajith-dilshan/playwright-hooks-the-secret-behind-clean-scalable-test-automation-8448bc56c1b4
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Hooks — The Secret Behind Clean & Scalable Test Automation

When I first started working with Playwright, my tests were honestly a mess. Every test had repeated login steps. Browser setup was copied everywhere. Cleanup logic was scattered across files.

Hooks are special methods that allow you to run code before tests, after tests, before each test, and after each test.

##### Types of Playwright Hooks

###### beforeAll()

Runs only one time before all tests inside the file. Common uses: launching browser setup, creating test data, connecting to databases, API authentication.

```
test.beforeAll(async () => {
    console.log('Starting test suite...');
});
```

###### afterAll()

Runs once after all tests are completed. Used for cleanup: removing test data, closing database connections, deleting temporary users.

```
test.afterAll(async () => {
    console.log('Test suite completed.');
});
```

###### beforeEach()

Runs before every single test. Perfect for login steps, navigating to pages, resetting application state.

```
test.beforeEach(async ({ page }) => {
    await page.goto('https://example.com/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
});
```

###### afterEach()

Runs after every test execution, even if the test fails. Common uses: screenshots on failure, logging results, clearing cookies.

```
test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== testInfo.expectedStatus) {
        await page.screenshot({ path: 'failure.png' });
    }
});
```

##### Hook Execution Order

```
beforeAll()
beforeEach()
Test 1
afterEach()
beforeEach()
Test 2
afterEach()
afterAll()
```

##### Common Mistakes

Do not put everything inside beforeEach() — use beforeAll() when setup is needed only once. Hooks should prepare independent environments. Tests should never depend on previous tests.

##### Final Thoughts

Experienced automation engineers ask: how maintainable is this framework after 6 months? Hooks help remove duplication, improve readability, simplify maintenance, and build scalable automation frameworks.

### 27. Sajith Dilshan — Error handling and strict mode

- Source: https://medium.com/@sajith-dilshan/playwright-error-handling-explained-common-errors-strict-mode-and-best-practices-63fac0949ea0
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Error Handling Explained: Common Errors, Strict Mode, and Best Practices

Playwright has quickly become one of the most popular end-to-end testing frameworks. One reason is its strict and opinionated error handling. Strict Mode violations exist to protect your tests from flakiness.

##### How Error Handling Works

Playwright follows a fail-fast testing philosophy. Instead of guessing or silently continuing when something is ambiguous, Playwright throws a clear error. This is achieved through auto-waiting, strict locators, built-in assertions, and traces/screenshots.

##### Common Playwright Errors

###### 1. Strict Mode Violation

```
Error: strict mode violation: locator("button") resolved to 2 elements
```

By default, a locator must match exactly one element. If Playwright finds multiple matching elements, it fails immediately.

Fix with more specific selectors, `getByRole`, or `.nth()` only when unavoidable.

###### 2. Timeout Errors

```
Timeout 30000ms exceeded while waiting for selector
```

Use auto-waiting assertions: `await expect(page.locator('#submit')).toBeVisible();`

###### 3. Element Is Not Attached to the DOM

Always use locators instead of element handles. Locators re-resolve elements automatically.

###### 4. Navigation Interrupted Errors

Wait for navigation explicitly with `Promise.all([page.waitForNavigation(), page.click(...)])`.

###### 5. Target Closed or Page Closed Errors

Usually indicate test lifecycle issues — the page or browser closed too early.

###### 6. Assertion Timeout Errors

Typically means the application state is incorrect or a real bug exists. Investigate, do not ignore.

##### Best Practices

- Prefer `getByRole`, `getByLabel`, and `getByText`
- Avoid CSS and XPath unless necessary
- Let Playwright handle waiting
- Treat Strict Mode violations as helpful signals
- Enable screenshots and traces: `screenshot: 'only-on-failure'`, `trace: 'on-first-retry'`

##### Final Thoughts

Playwright error handling is one of its biggest strengths. Strict Mode violations are guardrails that help you write stable, readable, and trustworthy tests.

### 28. TestDino — POM pattern

- Source: https://testdino.com/blog/playwright-page-object-model
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Page Object Model: Pattern Guide with Examples

Wrap every page of your app in a single class so tests call loginPage.signIn() instead of repeating selectors. This guide walks through Playwright POM from scratch page classes, fixtures, and the patterns that scale.

[![Ayush Mania Profile](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)\\
\\
Ayush Mania\\
\\
Updated May 4, 2026](https://testdino.com/blog/author/ayush-mania)





![Playwright Page Object Model: Pattern Guide with Examples](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FPlaywright-Page-Object-Model_-Pattern-Guide-with-Examples.webp&w=3840&q=75)

Playwright gives you a way to wrap every page of your app inside a single class. Your tests talk to methods likeloginPage.signIn()  instead of repeating the same selectors in every spec file. That class is called a page object, and the pattern behind it is the [page object model](https://playwright.dev/docs/pom "https://playwright.dev/docs/pom"). Teams running hundreds of [Playwright e2e tests](https://testdino.com/blog/playwright-e2e-testing "https://testdino.com/blog/playwright-e2e-testing/") already structure their code this way because it keeps locators in one place and makes updates a one-line fix.

The pain is real when you skip this step. A single button ID changes and suddenly 40 test files need editing. Duplicate selectors pile up, reviews slow down, and [flaky tests](https://testdino.com/blog/flaky-tests "https://testdino.com/blog/flaky-tests/") multiply because each spec re-implements the same interaction slightly differently.

This guide walks through the playwright page object model from scratch using **Playwright 1.40+** and TypeScript. You will build page classes, wire them with [Playwright fixtures](https://playwright.dev/docs/test-fixtures "https://playwright.dev/docs/test-fixtures"), handle multi-page flows, and see the patterns that keep a growing suite manageable in CI.

##### What is the page object model and why does it matter

**Definition:** The page object model (POM) is a design pattern where each page or major component of your application gets its own class. That class stores every locator and user action for that page, so tests never touch raw selectors directly. POM enforces a single source of truth for UI interactions.

Think of it like a remote control for a TV. You press "volume up" without caring which circuit board signal fires. A page object works the same way for your tests. You call loginPage.signIn(user, pass) and the class handles the three clicks and two fills behind the scenes.

Without POM, a typical [Playwright test automation](https://testdino.com/blog/playwright-framework-setup "https://testdino.com/blog/playwright-framework-setup/") project looks like this after six months:

- The same page.locator('#email') scattered across 30 spec files
- A renamed CSS class breaks tests in places you did not expect
- New team members struggle to understand what each test actually validates
- Pull request reviews take longer because reviewers trace selectors instead of reading intent

POM solves all four problems by giving you a single source of truth for each page. When a locator changes, you open one class file, update one line, and every test that depends on it keeps passing.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** POM originated in Selenium but fits Playwright perfectly because locators are lazily evaluated, resolving only during actions.

The [Playwright official documentation](https://playwright.dev/docs/pom "https://playwright.dev/docs/pom") recommends POM as the go-to approach for structuring large test suites. According to the [2024 State of JS survey](https://2024.stateofjs.com/en-US/libraries/testing/ "https://2024.stateofjs.com/en-US/libraries/testing/"), Playwright crossed 50% awareness among JavaScript developers. As adoption grows, structuring code with the page object pattern becomes less of a nice-to-have and more of a baseline expectation for any serious playwright test structure.

![](https://cms.testdino.com/wp-content/uploads/2026/04/image-20260429-114319.png)

##### Setting up your project for POM

Before writing any page class, you need a clean folder structure. A well-organized project makes it obvious where to add new page objects and where to find existing ones.

###### Prerequisites

Make sure you have Node.js 18+ installed. Then initialize a new Playwright project:

terminal

```
npm init playwright@latest
```

The installer asks a few questions. Pick TypeScript when prompted. TypeScript adds type safety to your page objects, which catches locator typos at compile time instead of runtime.

**Note:** JavaScript works too. TypeScript simply provides the added benefits of autocomplete and compile-time type checking.

###### Recommended folder structure

Here is a folder layout that scales from 5 tests to 500:

project-structure

```
playwright-project/
├── pages/
│   ├── LoginPage.ts
│   ├── DashboardPage.ts
│   ├── CheckoutPage.ts
│   └── components/
│       ├── NavbarComponent.ts
│       └── SearchComponent.ts
├── tests/
│   ├── login.spec.ts
│   ├── dashboard.spec.ts
│   └── checkout.spec.ts
├── fixtures/
│   └── base.ts
├── test-data/
│   └── users.json
├── playwright.config.ts
└── package.json
```

Three key decisions in this layout:

- pages/ is separate from tests/. Page objects are utilities, not tests. Keeping them apart prevents circular imports and makes the purpose of each file clear at a glance.
- components/ lives inside pages/. Reusable UI pieces like navbars and modals get their own classes. A NavbarComponent can be imported into DashboardPage, CheckoutPage, or any page that shows the nav.
- fixtures/ holds your custom fixture file. This is where you wire page objects into the test runner using test.extend(). More on this in a later section.

###### Configuring playwright.config.ts

Your config file does not need anything POM-specific. But a few settings help when you are building out page objects:

playwright.config.ts

```
import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  retries: 1,
  use: {
    baseURL: 'https://your-app.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
});
```

Setting baseURL is important here. Your page objects can use relative URLs like await this.page.goto('/login') instead of hardcoding the full domain. This makes switching between staging and production a single config change.

**Project tree at a glance:** Keep three root folders: pages/ (classes), tests/ (specs), and fixtures/ (setup). This 1:1 mapping ensures organized scalability.

##### Building your first Playwright page object class

A page object class has three parts: a constructor that receives the Playwright page instance, locator definitions that point to UI elements, and methods that wrap user actions.

Here is a complete LoginPage class:

pages/LoginPage.ts

```
import { type Locator, type Page } from '@playwright/test';
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly signInButton: Locator;
  readonly errorMessage: Locator;
  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.signInButton = page.getByRole('button', { name: 'Sign in' });
    this.errorMessage = page.getByTestId('login-error');
  }
  async goto() {
    await this.page.goto('/login');
  }
  async signIn(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.signInButton.click();
  }
}
```

A few things to notice in this code:

- **Locators use** getByRole **and** getByLabel instead of CSS selectors. These [Playwright locators](https://testdino.com/blog/playwright-locators) are resilient because they match how real users see the page, not how the DOM is structured.
- **Every locator** is readonly. This prevents tests from accidentally overwriting a locator at runtime.
- **The** signIn **method hides implementation details**. Tests call loginPage.signIn('user@test.com', 'pass123') without knowing whether the form has two fields or ten.
- **No assertions live in the page object**. The page object performs actions. The test file checks results. Mixing both creates tight coupling that makes refactoring harder.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Prefer getByRole and getByLabel over CSS selectors. They tie to semantics instead of structure, surviving UI changes better.

###### Building a second page object

After login, users land on a dashboard. Here is a DashboardPage class that models that:

pages/DashboardPage.ts

```
import { type Locator, type Page } from '@playwright/test';
export class DashboardPage {
  readonly page: Page;
  readonly welcomeHeading: Locator;
  readonly projectList: Locator;
  readonly createProjectButton: Locator;
  readonly searchInput: Locator;
  constructor(page: Page) {
    this.page = page;
    this.welcomeHeading = page.getByRole('heading', { name: /welcome/i });
    this.projectList = page.getByTestId('project-list');
    this.createProjectButton = page.getByRole('button', { name: 'New project' });
    this.searchInput = page.getByPlaceholder('Search projects');
  }
  async createProject(name: string) {
    await this.createProjectButton.click();
    await this.page.getByLabel('Project name').fill(name);
    await this.page.getByRole('button', { name: 'Create' }).click();
  }
  async searchProject(query: string) {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
  }
}
```

Notice how createProject combines multiple steps into a single method call. The test does not need to know that creating a project involves clicking a button, filling a modal, and confirming. This is the core advantage of the playwright page object model pattern. In practice, teams that adopt this approach early report spending significantly less time on selector-related maintenance as their suite grows.

###### Modeling reusable components

Not everything is a full page. Navigation bars, sidebars, and modals appear across multiple pages. Model these as component classes:

pages/components/NavbarComponent.ts

```
import { type Locator, type Page } from '@playwright/test';
export class NavbarComponent {
  readonly page: Page;
  readonly profileMenu: Locator;
  readonly logoutButton: Locator;
  readonly notificationBell: Locator;
  constructor(page: Page) {
    this.page = page;
    this.profileMenu = page.getByTestId('profile-menu');
    this.logoutButton = page.getByRole('menuitem', { name: 'Logout' });
    this.notificationBell = page.getByLabel('Notifications');
  }
  async logout() {
    await this.profileMenu.click();
    await this.logoutButton.click();
  }
}
```

Then import this component into any page that uses the navbar:

pages/DashboardPage.ts

```
import { NavbarComponent } from './components/NavbarComponent';
export class DashboardPage {
  readonly navbar: NavbarComponent;
  // ... other locators
  constructor(page: Page) {
    this.navbar = new NavbarComponent(page);
    // ... other setup
  }
}
```

Now your test can call dashboardPage.navbar.logout() directly. This composition approach keeps each class focused on one responsibility, which matters a lot when [reducing test maintenance](https://testdino.com/blog/reduce-test-maintenance) across a growing suite.

##### Writing tests that use page objects

With page classes in place, your test files become short, readable, and focused entirely on what you are verifying.

###### Basic test using page objects

tests/login.spec.ts

```
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
test('user can sign in with valid credentials', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const dashboardPage = new DashboardPage(page);
  await loginPage.goto();
  await loginPage.signIn('user@example.com', 'securePass123');
  await expect(dashboardPage.welcomeHeading).toBeVisible();
  await expect(page).toHaveURL(/dashboard/);
});
test('shows error for invalid credentials', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.signIn('user@example.com', 'wrongPassword');
  await expect(loginPage.errorMessage).toContainText('Invalid email or password');
});
```

Compare this to a test without POM:

tests/login-no-pom.spec.ts

```
test('user can sign in', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('user@example.com');
  await page.getByLabel('Password').fill('securePass123');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: /welcome/i })).toBeVisible();
});
```

The non-POM version looks simpler for one test. But multiply it by 50 tests, change one label from "Email" to "Email address", and you are updating every single file. With POM, that fix lives in LoginPage.ts alone.

**Note:** Keep assertions in test files, never in page objects. This ensures your classes remain reusable across all test scenarios.

###### Testing multi-page flows

Real user journeys span multiple pages. POM makes these flows read like a story:

tests/checkout.spec.ts

```
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { CheckoutPage } from '../pages/CheckoutPage';
test('complete purchase flow', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const dashboardPage = new DashboardPage(page);
  const checkoutPage = new CheckoutPage(page);
  await loginPage.goto();
  await loginPage.signIn('buyer@store.com', 'buyerPass');
  await dashboardPage.searchProject('Widget Pro');
  await checkoutPage.addToCart();
  await checkoutPage.applyDiscount('SAVE10');
  await checkoutPage.completePurchase();
  await expect(checkoutPage.confirmationMessage).toContainText('Order confirmed');
});
```

Each line describes a user action in plain English. Anyone reviewing this PR, even someone who has never touched the codebase, can understand what the test validates. This readability is one of the biggest reasons the playwright POM pattern is the default recommendation for teams running [Playwright scripts](https://testdino.com/blog/playwright-scripts) at scale.

###### Keeping test data separate

Hardcoded credentials inside tests create maintenance problems. Move test data to JSON files:

test-data/users.json

```
{
  "validUser": {
    "email": "user@example.com",
    "password": "securePass123"
  },
  "invalidUser": {
    "email": "user@example.com",
    "password": "wrongPassword"
  }
}
```

Then import and use them in your tests:

tests/login.spec.ts

```
import users from '../test-data/users.json';
test('user can sign in with valid credentials', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.signIn(users.validUser.email, users.validUser.password);
  // assertions...
});
```

This separation means credentials, URLs, and test inputs live outside your page objects and test logic. When you need to update test data for a new environment, you edit one JSON file instead of hunting through dozens of specs.

**Terminal Output:** Because specs call named methods instead of selectors, your npx playwright test output reads like clear, human-readable sentences.

##### Using Playwright fixtures with POM

Manually creating page objects with new LoginPage(page) in every test works, but it adds boilerplate. Playwright has a built-in solution for this: [fixtures](https://playwright.dev/docs/test-fixtures). Fixtures let you pre-configure page objects and inject them directly into your test function signature.

###### What are fixtures

**Definition:** Fixtures are reusable setup/teardown blocks that automatically inject resources (like page objects) into tests.

The test.extend() method creates custom fixtures. Here is how to wire your page objects into fixtures:

fixtures/base.ts

```
import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { CheckoutPage } from '../pages/CheckoutPage';
type PageFixtures = {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
  checkoutPage: CheckoutPage;
};
export const test = base.extend<PageFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
  checkoutPage: async ({ page }, use) => {
    await use(new CheckoutPage(page));
  },
});
export { expect } from '@playwright/test';
```

###### Using fixture-based page objects in tests

Now your tests import test from the fixture file instead of from @playwright/test:

tests/login.spec.ts

```
import { test, expect } from '../fixtures/base';
test('user can sign in with valid credentials', async ({ loginPage, dashboardPage }) => {
  await loginPage.goto();
  await loginPage.signIn('user@example.com', 'securePass123');
  await expect(dashboardPage.welcomeHeading).toBeVisible();
});
test('shows error for invalid credentials', async ({ loginPage }) => {
  await loginPage.goto();
  await loginPage.signIn('user@example.com', 'wrongPassword');
  await expect(loginPage.errorMessage).toContainText('Invalid email or password');
});
```

Notice what changed. There is no new LoginPage(page) anywhere. You just add loginPage to the destructured test arguments and Playwright creates it for you. This eliminates repetitive setup code across every spec file.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Fixtures support setup steps. You can log in inside the fixture so tests start pre-authenticated, speeding up execution.

###### Adding setup steps to fixtures

For tests that always need a logged-in user, move the login step into the fixture:

fixtures/base.ts

```
import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
type AuthFixtures = {
  authenticatedPage: DashboardPage;
};
export const test = base.extend<AuthFixtures>({
  authenticatedPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.signIn('user@example.com', 'securePass123');
    await use(new DashboardPage(page));
  },
});
export { expect } from '@playwright/test';
```

Now any test that needs a logged-in dashboard simply requests authenticatedPage:

tests/dashboard.spec.ts

```
import { test, expect } from '../fixtures/base';
test('can create a new project', async ({ authenticatedPage }) => {
  await authenticatedPage.createProject('My New Project');
  await expect(authenticatedPage.projectList).toContainText('My New Project');
});
```

This keeps authentication logic out of individual tests. If your login flow changes, you update the fixture once, and every test that depends on it adapts automatically. Teams managing Playwright test management workflows at scale rely on this pattern to avoid duplication.

![](https://cms.testdino.com/wp-content/uploads/2026/04/image-20260429-114523.png)

##### Advanced Playwright POM patterns for production

The basics of POM cover most use cases. But production projects with 200+ tests often need patterns that go beyond single-page classes.

###### Base page class with shared methods

Most pages share common elements like headers, footers, and loading spinners. A base class avoids repeating these across every page object:

pages/BasePage.ts

```
import { type Locator, type Page } from '@playwright/test';
export class BasePage {
  readonly page: Page;
  readonly loadingSpinner: Locator;
  readonly toastNotification: Locator;
  constructor(page: Page) {
    this.page = page;
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.toastNotification = page.getByRole('alert');
  }
  async waitForPageLoad() {
    await this.loadingSpinner.waitFor({ state: 'hidden' });
  }
  async getToastMessage(): Promise<string> {
    return await this.toastNotification.textContent() ?? '';
  }
}
```

Then extend it in your specific page objects:

pages/SettingsPage.ts

```
import { type Locator, type Page } from '@playwright/test';
import { BasePage } from './BasePage';
export class SettingsPage extends BasePage {
  readonly profileNameInput: Locator;
  readonly saveButton: Locator;
  constructor(page: Page) {
    super(page);
    this.profileNameInput = page.getByLabel('Display name');
    this.saveButton = page.getByRole('button', { name: 'Save changes' });
  }
  async updateDisplayName(name: string) {
    await this.profileNameInput.fill(name);
    await this.saveButton.click();
    await this.waitForPageLoad();
  }
}
```

The SettingsPage inherits waitForPageLoad() and getToastMessage() from BasePage. Every page in your app gets these methods automatically. This inheritance pattern is one of the reasons the playwright POM approach scales well beyond 100 test files.

###### Returning page objects from navigation methods

When a method triggers navigation to a different page, return the new page object. This creates a fluent API where the test naturally flows from one page to the next:

pages/LoginPage.ts

```
import { type Locator, type Page } from '@playwright/test';
import { DashboardPage } from './DashboardPage';
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly signInButton: Locator;
  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.signInButton = page.getByRole('button', { name: 'Sign in' });
  }
  async signIn(email: string, password: string): Promise<DashboardPage> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.signInButton.click();
    return new DashboardPage(this.page);
  }
}
```

The test then chains naturally:

tests/login.spec.ts

```
test('user signs in and creates a project', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  const dashboard = await loginPage.signIn('user@example.com', 'securePass123');
  await dashboard.createProject('Project Alpha');
});
```

###### Handling dynamic elements with page object methods

Some pages have elements that appear based on state. For example, a table with dynamic rows. Handle these with parameterized methods:

pages/ProjectListPage.ts

```
import { type Locator, type Page } from '@playwright/test';
export class ProjectListPage {
  readonly page: Page;
  readonly projectRows: Locator;
  constructor(page: Page) {
    this.page = page;
    this.projectRows = page.getByTestId('project-row');
  }
  getProjectByName(name: string): Locator {
    return this.projectRows.filter({ hasText: name });
  }
  async deleteProject(name: string) {
    const row = this.getProjectByName(name);
    await row.getByRole('button', { name: 'Delete' }).click();
    await this.page.getByRole('button', { name: 'Confirm' }).click();
  }
  async getProjectCount(): Promise<number> {
    return await this.projectRows.count();
  }
}
```

This pattern handles dynamic content cleanly without hardcoding row indices or relying on brittle CSS nth-child selectors. It works well alongside [Playwright component testing](https://testdino.com/blog/playwright-component-testing "https://testdino.com/blog/playwright-component-testing/") where you need to interact with repeated UI elements. Teams using the playwright pom pattern at scale often rely on parameterized locators like this to keep their page objects flexible.

###### POM with Playwright test.step for better reporting

Wrapping page object calls in test.step() produces clearer [Playwright HTML reports](https://testdino.com/blog/playwright-html-reporter):

tests/settings.spec.ts

```
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { SettingsPage } from '../pages/SettingsPage';
test('user updates display name', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const settingsPage = new SettingsPage(page);
  await test.step('Sign in', async () => {
    await loginPage.goto();
    await loginPage.signIn('user@example.com', 'securePass123');
  });
  await test.step('Update profile settings', async () => {
    await page.goto('/settings');
    await settingsPage.updateDisplayName('Jane Doe');
  });
  await test.step('Verify success message', async () => {
    const toast = await settingsPage.getToastMessage();
    expect(toast).toContain('Profile updated');
  });
});
```

Each step appears as a collapsible section in the HTML report. When a test fails, you can see exactly which step broke without reading through every action. Teams using [Playwright reporting tools](https://testdino.com/blog/playwright-reporting-tools "https://testdino.com/blog/playwright-reporting-tools/") find this structure invaluable for [debugging Playwright tests](https://testdino.com/blog/debug-playwright-tests "https://testdino.com/blog/debug-playwright-tests/") in CI.

| Pattern | When to use | Benefit |
| --- | --- | --- |
| Base page class | Shared elements across pages (loaders, toasts, nav) | Reduces duplication in page objects |
| Navigation return types | Methods that trigger page transitions | Fluent test API, type-safe page transitions |
| Component composition | Reusable UI blocks (navbar, sidebar, modal) | Single responsibility per class |
| Dynamic element methods | Tables, lists, repeating elements | Handles runtime content without brittle selectors |
| test.step() wrapping | Complex multi-action flows | Clear HTML report sections, easier debugging |

**HTML Reports:** Using test.step() creates collapsible sections in your HTML report, making it instantly clear which part of a flow failed.

##### Common Playwright POM mistakes and how to fix them

Even experienced teams make these mistakes when adopting the playwright page object model. Catching them early saves hours of refactoring later.

![](https://cms.testdino.com/wp-content/uploads/2026/04/image-20260429-114622.png)

###### Mistake 1: Putting assertions inside page objects

This is the most common anti-pattern. It looks like this:

pages/LoginPage.ts

```
// BAD: assertions in page object
async signIn(email: string, password: string) {
  await this.emailInput.fill(email);
  await this.passwordInput.fill(password);
  await this.signInButton.click();
  await expect(this.page).toHaveURL(/dashboard/); // Do not do this
}
```

The problem is that this signIn method can only be used for successful login tests. If you want to test invalid credentials, you need a separate method. Keep assertions in the test file and keep page objects action-only.

###### Mistake 2: Creating god objects

A god object is a single class that models your entire application. It has 50 locators, 30 methods, and handles everything from login to checkout. This defeats the purpose of POM entirely.

Follow the single-responsibility principle. One class per page or major component. If a class grows beyond 100 lines, consider breaking it into smaller component classes.

###### Mistake 3: Using fragile selectors

pages/LoginPage.ts

```
// BAD: fragile selectors
this.submitBtn = page.locator('div.form-container > button:nth-child(3)');
this.emailField = page.locator('#root > div > form > input.email');
```

Each step appears as a collapsible section in the HTML report. When a test fails, you can see exactly which step broke without reading through every action. Teams using [Playwright reporting tools](https://testdino.com/blog/playwright-reporting-tools "https://testdino.com/blog/playwright-reporting-tools/") find this structure invaluable for [debugging Playwright tests](https://testdino.com/blog/debug-playwright-tests "https://testdino.com/blog/debug-playwright-tests/") in CI.

pages/LoginPage.ts

```
// GOOD: semantic selectors
this.submitBtn = page.getByRole('button', { name: 'Submit' });
this.emailField = page.getByLabel('Email');
```

###### Mistake 4: Ignoring the baseURL config

Hardcoding full URLs in page objects creates environment-switching headaches:

pages/LoginPage.ts

```
// BAD
async goto() {
  await this.page.goto('https://staging.myapp.com/login');
}
// GOOD
async goto() {
  await this.page.goto('/login');
}
```

Use the baseURL setting in playwright.config.ts and keep your page objects environment-agnostic.

###### Mistake 5: Not using TypeScript readonly

Without readonly, a test can accidentally overwrite a locator:

example.spec.ts

```
// Accidental overwrite without readonly
loginPage.emailInput = page.locator('.wrong-selector');
```

TypeScript readonly properties catch this mistake at compile time. It is a small addition that prevents confusing runtime bugs.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fi_768818.svg)

**Warning:** Avoid taking screenshots in every page method. It slows down tests. Use screenshot: 'only-on-failure' in your config instead.

##### When not to use POM

POM is not always the right choice. For quick prototypes, one-off smoke tests, or suites with fewer than five specs, the overhead of creating separate class files can slow you down more than it helps. In those cases, inline selectors inside the test file are perfectly fine.

The break-even point is usually around 10-15 tests touching the same page. Once you cross that threshold, the maintenance cost of scattered selectors exceeds the upfront cost of writing a page class. If your project is growing toward that number, start with POM from the beginning rather than migrating later.

##### Conclusion

The playwright page object model turns a growing test suite from a maintenance nightmare into a well-organized codebase. You have seen how to structure your project, build page classes with resilient locators, wire them with fixtures, and apply advanced patterns like base classes, component composition, and fluent navigation returns.

Here are the key takeaways:

- Keep locators in page objects, assertions in test files, and shared logic in fixtures.
- Use getByRole and getByLabel instead of CSS selectors.
- Break large classes into focused components.
- Always set readonly on your locator properties.
- Use test.extend() to eliminate boilerplate page object creation.

Start with a simple LoginPage class. Once that pattern feels natural, expand to other pages and introduce fixtures. The playwright test structure scales with you. Whether your suite has 10 tests or 1,000, POM keeps the maintenance cost flat.

And when those tests start running in [CI/CD pipelines](https://testdino.com/blog/ci-cd-testing-best-practices "https://testdino.com/blog/ci-cd-testing-best-practices/"), the real value of POM shows up in pull request reviews where every change is a clear, one-line update to a single class file.

##### FAQs

What is the page object model in Playwright?

The page object model (POM) in Playwright is a design pattern where each page of your application is represented by a class. That class contains all the locators and user actions for that page. Tests interact with these classes instead of writing selectors directly. The [Playwright official docs](https://playwright.dev/docs/pom "https://playwright.dev/docs/pom") recommend this approach for organizing large test suites.

Should I use TypeScript or JavaScript for Playwright POM?

Both work. TypeScript is recommended because it catches locator typos at compile time, provides autocomplete in your IDE, and enforces readonly properties on locators. If your team is already using JavaScript, you can still follow the POM pattern without any issues.

Where should I put assertions when using page objects?

Assertions belong in your test files, never inside page objects. This keeps page objects reusable across different test scenarios. The same loginPage.signIn() method works for testing valid logins, invalid logins, and edge cases.

What is the difference between POM and fixtures in Playwright?

POM is a design pattern for organizing locators and actions into classes. Fixtures are a Playwright feature that automates the creation and injection of those classes into your tests. They work together: you define page objects as classes and then expose them as fixtures using test.extend().

How do I handle components that appear on multiple pages?

Create separate component classes for shared UI elements like navbars, sidebars, or modals. Import and compose them inside your page objects. For example, DashboardPage can have a navbar property that is an instance of NavbarComponent. Tests access it as dashboardPage.navbar.logout().

Can I use page objects with Playwright's codegen tool?

Yes. You can use [Playwright codegen](https://testdino.com/blog/playwright-codegen) to generate the initial locators, then move them into page object classes. The codegen output gives you a starting point, but you should refactor the generated selectors into semantic locators like getByRole and getByLabel for better resilience.

How do page objects help with CI/CD?

Page objects reduce maintenance when tests run in [CI/CD pipelines](https://testdino.com/blog/ci-cd-testing-best-practices). When a UI element changes, you update one class file instead of every test that interacts with that element. This makes CI failures faster to fix and keeps your pipeline reliable.

How many page objects should I create?

Create one page object per distinct page or major UI section. A typical web app with login, dashboard, settings, and checkout pages needs four page objects plus component classes for shared elements like navbars. Avoid creating a single "god object" that covers your entire app. If a class grows beyond 100 lines, split it into smaller focused classes.

[![Ayush Mania Profile](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)](https://testdino.com/blog/author/ayush-mania)

[**Ayush Mania**](https://testdino.com/blog/author/ayush-mania)

Forward Development Engineer

Ayush Mania is a Forward Development Engineer at TestDino, focusing on platform infrastructure, CI workflows, and reliability engineering. His work involves building systems that improve debugging, failure detection, and overall test stability.

He contributes to architecture design, automation pipelines, and quality engineering practices that help teams run efficient development and testing workflows.

[LinkedIn](https://www.linkedin.com/in/ayushmania "LinkedIn")[GitHub](https://github.com/ayush-mania "GitHub")

[View all posts →](https://testdino.com/blog/author/ayush-mania)

Table of content

![](https://testdino.com/_next/image?url=%2Fimages%2Fcta-background-image.webp&w=3840&q=75)

Enjoyed this guide?

Get Playwright testing tips in your inbox.

Subscribe

##### Get started fast

Step-by-step guides, real-world examples, and proven strategies to maximize your test reporting success.

[Agentic Testing Tools Compared: QA.tech, Momentic, Spur vs. Deterministic Playwright](https://testdino.com/blog/agentic-testing-tools-compared)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

TestingTools

###### Agentic Testing Tools Compared: QA.tech, Momentic, Spur vs. Deterministic Playwright

Wondering if agentic testing tools can replace Playwright? Compare QA.tech, Momentic, and Spur to make an informed decision.

![Savan Vaghani](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FSavan-Vaghani-Profile-Image-1-1.webp&w=3840&q=75)

[Savan Vaghani](https://testdino.com/blog/author/savan-vaghani)·Aug 27, 2026

[Provar to Playwright Migration](https://testdino.com/blog/provar-to-playwright)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### Provar to Playwright Migration

Discover why teams are switching from Provar to Playwright and learn the exact steps to migrate your Salesforce tests.

![Krupa Gandhi](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-59.webp&w=3840&q=75)

[Krupa Gandhi](https://testdino.com/blog/author/krupa-gandhi)·Aug 19, 2026

[Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes](https://testdino.com/blog/salesforce-playwright-test-in-cicd)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

PlaywrightSalesforce

###### Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes

Sandbox refreshes keep breaking your Salesforce Playwright tests in CI/CD? This guide covers auth, config, and pipeline setup to fix that for good.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 17, 2026

[Browse all](https://testdino.com/blog)

We use cookies to measure how the site is used and to improve it. You can accept analytics cookies or keep only the essential ones. [Privacy policy](https://testdino.com/privacy-policy)

Reject non-essentialAccept all

### 29. TestDino — Playwright test automation

- Source: https://testdino.com/blog/playwright-test-automation
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Test Automation: The Complete Guide for QA Teams

Master playwright test automation with setup, selectors, parallel execution, CI/CD, and debugging strategies used by production QA teams.

[![Pratik Patel Profile](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-57.webp&w=3840&q=75)\\
\\
Pratik Patel\\
\\
Updated Jun 18, 2026](https://testdino.com/blog/author/pratik-patel)





![Playwright Test Automation: The Complete Guide for QA Teams](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F04%2FPlaywright-Test-Automation_-The-Complete-Guide-for-QA-Teams.webp&w=3840&q=75)

Every QA team shipping weekly releases faces the same pressure: ship fast, test everything, break nothing. Playwright's npm downloads crossed 33 million per week in early 2026, and teams that used to swear by Selenium and Cypress are actively migrating.

The core frustration is familiar. Tests pass on your machine, then fail in CI with timeout errors and selector mismatches. Debugging these flaky pipelines eats hours every single sprint.

This guide walks through playwright test automation from first install to production CI/CD. You will get real config files, tested code, and patterns that hold up at 200+ tests.

**Prerequisites:** Node.js 18+ and npm 8+ installed. All examples use TypeScript and Playwright v1.50+.

##### What is playwright test automation?

Playwright test automation is the practice of using Microsoft's open-source [browser testing framework](https://playwright.dev/docs/intro "https://playwright.dev/docs/intro") to write and run end-to-end tests across Chromium, Firefox, and WebKit from a single API.

**Definition:** Playwright is an open-source Node.js library by Microsoft (2020). It talks directly to browser engines via native protocols, not intermediate drivers. Its GitHub repo has over 70,000 stars as of 2026.

Unlike Selenium's WebDriver model, Playwright communicates directly with browser engines using the Chrome DevTools Protocol. This [Playwright architecture](https://testdino.com/blog/playwright-architecture "https://testdino.com/blog/playwright-architecture/") eliminates the flaky middle layer that QA teams have fought for years.

###### What makes this web automation tool different

Here is what makes playwright browser automation practical for production teams:

**Cross-browser from day one.** Chromium, Firefox, and WebKit from one test script. No driver management, no version mismatches.

**Multi-language.** TypeScript, JavaScript, Python, Java, and .NET. The API stays consistent across all bindings.

**Codegen.** Record user actions with npx playwright codegen and get ready-to-run test code. [Playwright AI codegen](https://testdino.com/blog/playwright-ai-codegen "https://testdino.com/blog/playwright-ai-codegen/") takes this further.

**Auto-waiting.** Every action waits for the element to be ready. No Thread.sleep(), no explicit waits for 90% of cases.

**Trace Viewer.** Replay any failed test with full DOM snapshots and network logs. The [Playwright trace viewer](https://testdino.com/blog/playwright-trace-viewer "https://testdino.com/blog/playwright-trace-viewer/") captures everything.

**Parallel execution.** Workers and sharding built in. No external grid infrastructure needed.

**Built-in reporting.** HTML, JSON, JUnit, and blob reporters ship out of the box.

These capabilities are why Playwright has become the default choice for teams running [playwright end-to-end testing](https://testdino.com/blog/playwright-e2e-testing "https://testdino.com/blog/playwright-e2e-testing/") on modern web applications.

##### Why teams choose Playwright over Selenium and Cypress

If you are evaluating a [playwright test framework](https://testdino.com/blog/playwright-framework-setup "https://testdino.com/blog/playwright-framework-setup/"), you will compare it against what your team already uses. The differences matter more than feature checklists suggest.

**Playwright vs Selenium:** Selenium sends every command through HTTP to a driver binary. That round-trip adds latency and a maintenance burden as driver versions fall out of sync. Playwright talks directly to browser engines. No driver binary. No version mismatch headaches. Teams that migrate from Selenium to automated testing with Playwright typically see flaky test counts drop by 40 to 60 percent in the first month.

**Playwright vs Cypress:** Cypress runs inside the browser via JavaScript injection. Good for DOM access, but it limits you to Chromium. Multi-tab, multi-origin, and iframe scenarios that Playwright handles natively require workarounds or are impossible in Cypress.

| Feature | Playwright | Selenium | Cypress |
| --- | --- | --- | --- |
| Browser communication | Direct protocol (CDP) | WebDriver HTTP | In-browser JS injection |
| Cross-browser support | Chromium, Firefox, WebKit | All browsers via drivers | Chromium, limited Firefox |
| Language support | JS/TS, Python, Java, .NET | Java, Python, C#, JS, Ruby | JS/TS only |
| Auto-waiting | Built-in actionability checks | Manual waits required | Built-in retry-ability |
| Multi-tab/multi-origin | Native support | Workarounds needed | Limited |
| Parallel execution | Built-in workers + sharding | Selenium Grid required | Cypress Cloud or third-party |
| Test isolation | BrowserContext per test | New browser instance | Page reload between tests |

For teams ready to switch, the [Selenium to Playwright migration](https://testdino.com/blog/selenium-to-playwright-migration "https://testdino.com/blog/selenium-to-playwright-migration/") guide covers the full phased approach with code examples.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Choose Playwright for real cross-browser testing. Choose Selenium if you need native mobile via Appium. Stick with Cypress only if Chromium-only coverage is enough.

##### ![](https://cms.testdino.com/wp-content/uploads/2026/04/Playwright-vs-Selenium-vs-Cypress-decision-matrix.webp)   How to set up a production-ready Playwright project

Most tutorials stop at the install command. The result works for 10 tests and falls apart at 50. Here is how to set up a playwright test automation project that scales.

###### Installation and production config

terminal

```
npm init playwright@latest
```

This creates playwright.config.ts, a tests/ folder, and installs browser binaries. Choose TypeScript when prompted.

The config file is where your automated testing setup either scales or breaks. Here is the production config that covers what the default scaffold misses:

playwright.config.ts

```
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 4 : undefined,
  reporter: process.env.CI ? 'blob' : 'html',
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [\
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },\
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },\
  ],
});
```

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Set forbidOnly: !!process.env.CI to prevent accidentally shipping test.only() calls that silently skip your entire suite in CI.

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-project-setup.png)

###### Recommended folder structure

Group test files by feature, not by page. Playwright shards by file, so feature grouping keeps related tests on the same worker:

project structure

```
#### project structure
project-root/
├── tests/
│   ├── auth/
│   │   ├── login.spec.ts
│   │   └── signup.spec.ts
│   ├── checkout/
│   │   ├── cart.spec.ts
│   │   └── payment.spec.ts
├── pages/
│   ├── LoginPage.ts
│   └── CartPage.ts
├── fixtures/
│   └── test.ts
├── playwright.config.ts
└── package.json
```

On CI, always run npx playwright install --with-deps. The --with-deps flag installs OS-level dependencies that Playwright's browsers need. The [Playwright CLI](https://testdino.com/blog/playwright-cli "https://testdino.com/blog/playwright-cli/") guide covers all available commands.

###### Troubleshooting common setup errors

Two errors trip up most teams during their first playwright browser automation setup:

**browserType.launch: Executable doesn't exist:** This means browser binaries are not installed. Run npx playwright install or npx playwright install chromium if you only need one browser.

**Missing system dependencies on Linux CI:** Ubuntu runners need libraries like libgbm and libnss3. The --with-deps flag handles this automatically, but if you use a custom Docker image, install them manually.

##### Selector strategies that keep your automated tests stable

In any playwright test automation project, selectors break more tests than actual bugs do. One CSS class rename during a refactor can cascade into 40 failures overnight.

The official [Playwright locators](https://testdino.com/blog/playwright-locators "https://testdino.com/blog/playwright-locators/") docs recommend this priority order:

###### Role-based locators (most resilient)

role-locators.spec.ts

```
// role-locators.spec.ts
await page.getByRole('button', { name: 'Submit' }).click();
await page.getByLabel('Email address').fill('user@test.com');
```

These target semantic meaning, not implementation details. They survive CSS changes, refactors, and component library upgrades because they reflect how real users and screen readers see the page.

###### Text and placeholder locators

text-locators.spec.ts

```
// text-locators.spec.ts
await page.getByText('Welcome back').isVisible();
await page.getByPlaceholder('Search products...').fill('laptop');
```

###### Test ID locators

testid-locators.spec.ts

```
// testid-locators.spec.ts
await page.getByTestId('checkout-button').click();
```

Best for elements that lack accessible labels. Configure the attribute globally:

playwright.config.ts

```
// playwright.config.ts (testIdAttribute)
export default defineConfig({
  use: { testIdAttribute: 'data-testid' },
});
```

###### CSS and XPath (last resort)

CSS selectors break on class changes. XPath breaks on DOM restructuring. In practice, teams that rely heavily on CSS/XPath spend 3 to 5x more time maintaining tests than teams using role-based locators.

**Note:** The Playwright docs recommend: role-based first, then text, then test-id. Reserve CSS and XPath for canvas or complex SVG interactions only.

##### Why auto-wait eliminates most flaky test failures

Flaky tests are the biggest time drain in any E2E automation setup. Timing issues cause the majority of them, as documented in the [flaky test benchmark report](https://testdino.com/blog/flaky-test-benchmark "https://testdino.com/blog/flaky-test-benchmark/"). Playwright's auto-wait addresses this at the framework level.

###### The six checks Playwright runs before every action

When you call locator.click(), Playwright automatically validates six conditions before executing. It does not click until all pass:

- Attached to the DOM

- Visible with a non-zero bounding box

- Stable (not mid-animation)

- Enabled (no disabled attribute)

- Receives events (no overlay blocking it)

- Editable (for fill actions only)


If any check fails, Playwright retries until the timeout (default: 30 seconds). The error message tells you exactly which check failed and why.

**Definition:** Actionability checks are six conditions Playwright validates before every user action. They run automatically with zero configuration.

Teams migrating from Selenium see their [Playwright flaky tests](https://testdino.com/blog/playwright-flaky-tests "https://testdino.com/blog/playwright-flaky-tests/") drop immediately. The biggest gain comes from removing manual sleep statements that were masking real timing problems.

For edge cases where auto-wait is not enough, use explicit waits:

explicit-waits.spec.ts

```
// explicit-waits.spec.ts
await page.waitForResponse(resp =>
  resp.url().includes('/api/orders') && resp.status() === 200
);
await page.locator('.loading-spinner').waitFor({ state: 'hidden' });
```

The central rule from the [Playwright best practices](https://testdino.com/blog/playwright-best-practices "https://testdino.com/blog/playwright-best-practices/") guide: never use page.waitForTimeout(). Always prefer web-first assertions or waitFor() with a state condition.

##### Structuring test suites with Page Object Model

At 50+ tests, copy-pasted selectors become a maintenance nightmare in any playwright test automation project. Change one button label and 15 tests break across 15 different files. The [Playwright Page Object Model](https://testdino.com/blog/playwright-page-object-model "https://testdino.com/blog/playwright-page-object-model/") solves this.

Teams scaling past this point usually adopt a set of [test automation best practices](https://www.vervali.com/blog/api-test-automation-best-practices-2026-rest-graphql-grpc-ci-cd-and-contract-testing/) to keep suites maintainable.

###### Building a login page object step by step

LoginPage.ts

```
// pages/LoginPage.ts
import { type Page, type Locator } from '@playwright/test';
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.submitButton = page.getByRole('button', { name: 'Sign in' });
  }
  async goto() { await this.page.goto('/login'); }
  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }
}
```

The test file stays clean and focused on outcomes:

login.spec.ts

```
// tests/auth/login.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';
test('successful login redirects to dashboard', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.login('admin@example.com', 'password123');
  await expect(page).toHaveURL('/dashboard');
});
```

One rule that saves teams from painful refactors later: assertions stay in the test file, never in the page object. The page object describes actions. The test decides expected outcomes.

Use Playwright [fixtures](https://playwright.dev/docs/test-fixtures "https://playwright.dev/docs/test-fixtures") to auto-inject page objects and eliminate boilerplate. This pattern is covered in the [reduce test maintenance](https://testdino.com/blog/reduce-test-maintenance "https://testdino.com/blog/reduce-test-maintenance/") guide.

![](https://cms.testdino.com/wp-content/uploads/2026/04/Page-Object-Model-Fixtures-architecture.webp)

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Use Playwright's storageState to authenticate once and reuse the session across all tests. This saves 5 to 10 seconds per test. Add playwright/.auth/ to your .gitignore.

##### Running playwright test automation in CI/CD pipelines

Tests that only run locally do not catch regressions. [Playwright CI/CD integration](https://testdino.com/blog/playwright-ci-cd-integrations "https://testdino.com/blog/playwright-ci-cd-integrations/") is where automated testing with Playwright delivers its real value: catching failures before they reach production.

###### Parallel execution settings that cut pipeline time

Running 200 tests sequentially on CI takes over 30 minutes. Four config settings bring that under 15:

- fullyParallel: true runs every test independently across workers

- workers: 4 on CI spawns four parallel processes. [Optimize Playwright workers](https://testdino.com/blog/optimize-playwright-workers "https://testdino.com/blog/optimize-playwright-workers/") based on your runner specs.

- reporter: 'blob' for sharding produces mergeable partial reports

- --shard=1/4 CLI flag splits the suite across CI matrix agents


###### GitHub Actions workflow for playwright E2E automation

.github/workflows/playwright.yml

```
#### .github/workflows/playwright.yml
name: Playwright Tests
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
jobs:
  test:
    timeout-minutes: 30
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        shardIndex: [1, 2, 3, 4]
        shardTotal: [4]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npx playwright test --shard=${{ matrix.shardIndex }}/${{ matrix.shardTotal }}
      - if: always()
        uses: actions/upload-artifact@v4
        with:
          name: blob-report-${{ matrix.shardIndex }}
          path: blob-report
          retention-days: 7
```

**Note:** The if: always() condition on the upload step is critical. Without it, GitHub Actions skips artifact uploads when tests fail, losing the debugging data you need most.

The [headless vs headed](https://testdino.com/blog/headless-vs-headed "https://testdino.com/blog/headless-vs-headed/") comparison explains when to use each mode. Always run headless on CI.

Teams focused on long-term test health use [test automation analytics](https://testdino.com/blog/test-automation-analytics "https://testdino.com/blog/test-automation-analytics/") dashboards to track pass rates, flakiness trends, and execution time patterns across branches.

![](https://cms.testdino.com/wp-content/uploads/2026/04/CI-Pipeline-Time-Reduction.webp)

_Source: Aggregated benchmarks from Playwright GitHub Discussions and community CI performance reports (2025 to 2026). Test suite: 200 E2E tests on GitHub Actions ubuntu-latest runners._

##### Debugging failures with trace viewer and screenshots

When a playwright test automation run fails in CI, the error message alone rarely tells the full story. Playwright provides built-in debugging tools that replace guesswork with evidence. The [Playwright debugging guide](https://testdino.com/blog/playwright-debugging-guide "https://testdino.com/blog/playwright-debugging-guide/") covers advanced workflows.

###### Trace viewer: replay any failure step by step

Enable tracing in your config to capture evidence only when it matters:

playwright.config.ts

```
// playwright.config.ts (trace setting)
export default defineConfig({
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
});
```

After a failure, open the trace:

terminal

```
npx playwright show-trace test-results/checkout-flow/trace.zip
```

Or drag the file into [trace.playwright.dev](https://trace.playwright.dev/ "https://trace.playwright.dev"). It loads entirely in your browser. No data leaves your machine.

The Trace Viewer gives you four tabs:

- **Actions tab:** every click, fill, and navigation with time taken

- **Network tab:** all HTTP requests by status and duration

- **Console tab:** browser and test-level logs

- **Errors tab:** exact expected vs actual assertion values


_![](https://cms.testdino.com/wp-content/uploads/2026/04/Testing-Tool-Among-Usage.webp)_

_Source: State of JavaScript 2024 survey (stateofjs.com), "Testing" section, respondent usage counts._

Teams running playwright automated testing at scale use the [Playwright observability platform](https://testdino.com/blog/playwright-observability-platform "https://testdino.com/blog/playwright-observability-platform/") to store and link trace artifacts to every CI run automatically.

![](https://cms.testdino.com/wp-content/uploads/2026/04/Playwright-Debugging-Toolkit.webp)

##### API mocking and visual regression testing

Two advanced capabilities that extend playwright browser automation beyond basic UI checks:

**API mocking with page.route()** intercepts and mocks network requests inside E2E tests. You can decouple tests from backend availability entirely.

**Visual regression with toHaveScreenshot()** catches layout regressions that functional assertions miss. Use mask and animations: 'disabled' to handle dynamic content.

The [Playwright annotations](https://testdino.com/blog/playwright-annotations "https://testdino.com/blog/playwright-annotations/") guide covers test tagging (@smoke, @regression) for running subsets from the CLI.

Teams using AI-powered code generation can pair their setup with the [playwright-skill](https://github.com/testdino-hq/playwright-skill "https://github.com/testdino-hq/playwright-skill") to scaffold test suites from natural language descriptions.

##### Conclusion

Playwright test automation handles the hardest parts of end-to-end testing out of the box. Auto-waiting kills timing-based flakiness. BrowserContext isolation makes parallelism safe. Direct protocol communication removes the driver management overhead.

The decisions that separate stable suites from painful ones:

- **Start with production config.** Set fullyParallel, forbidOnly, retries, and trace from day one.

- **Use role-based locators.** Most resilient to UI changes and aligned with accessibility standards.

- **Never use page.waitForTimeout().** Web-first assertions handle timing correctly.

- **Adopt POM early.** The upfront investment pays back once you pass 50 tests.

- **Authenticate once with storageState.** Skip repetitive login flows entirely.

- **Shard on CI.** Four shards can cut pipeline time by 60 to 70 percent.

- **Capture traces on failure.** Full debugging context without storage overhead on passing tests.

- **Track flakiness trends.** Use [test automation analytics](https://testdino.com/blog/test-automation-analytics "https://testdino.com/blog/test-automation-analytics/") to catch regressions before they compound.


The [Playwright best practices](https://testdino.com/blog/playwright-best-practices "https://testdino.com/blog/playwright-best-practices/") guide covers additional patterns for teams scaling past 200 tests.

##### FAQ: playwright test automation

Is Playwright better than Selenium for test automation?

For modern web apps, yes. Playwright handles SPAs, dynamic content, and async rendering with less configuration. Auto-wait reduces flaky tests, and built-in cross-browser support eliminates driver management. Selenium remains better for native mobile testing via Appium.

How do I add playwright test automation to an existing project?

Run npm init playwright@latest in your project root. Start with 3 to 5 critical user flows against staging and expand gradually. The [Playwright E2E testing](https://testdino.com/blog/playwright-e2e-testing "https://testdino.com/blog/playwright-e2e-testing/") guide covers the full process.

What is the best way to fix flaky Playwright tests?

Set retries: 2 and trace: 'retain-on-failure' in your config. Use the Trace Viewer to compare passing and failing runs. Common causes are timing issues, shared state between parallel tests, and fragile CSS selectors. The [flaky tests](https://testdino.com/blog/flaky-tests "https://testdino.com/blog/flaky-tests/") guide covers prevention.

Can Playwright test mobile applications?

Playwright supports mobile browser emulation (viewports, touch events, user agents) but does not test native apps. Use Appium for native iOS or Android testing.

How does parallel execution and sharding work?

fullyParallel runs every test independently across workers. The --shard flag splits the suite across CI agents. A 200-test suite drops from 28 minutes to under 9 with 4 shards. The [Playwright parallel execution](https://testdino.com/blog/playwright-parallel-execution "https://testdino.com/blog/playwright-parallel-execution/") guide covers advanced patterns.

What languages does Playwright support?

TypeScript, JavaScript, Python, Java, and .NET (C#). The API stays consistent across all bindings. TypeScript is the most popular due to its type safety and IDE support.

[![Pratik Patel Profile](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-57.webp&w=3840&q=75)](https://testdino.com/blog/author/pratik-patel)

[**Pratik Patel**](https://testdino.com/blog/author/pratik-patel)

Co-founder

Pratik Patel is the Co-founder of TestDino, a Playwright-focused observability and CI optimization platform that gives engineering and QA teams clear visibility into test results, flaky failures, and pipeline health. With 12+ years in QA automation, he has helped startups and enterprises like Scotts Miracle-Gro, Avenue One, and Huma build and scale high-performing QA teams. An active open-source contributor, he regularly writes about modern testing practices, Playwright, and developer productivity.

[LinkedIn](https://www.linkedin.com/in/prat3ik/ "LinkedIn")[GitHub](https://github.com/prat3ik "GitHub")

[View all posts →](https://testdino.com/blog/author/pratik-patel)

Table of content

![](https://testdino.com/_next/image?url=%2Fimages%2Fcta-background-image.webp&w=3840&q=75)

Enjoyed this guide?

Get Playwright testing tips in your inbox.

Subscribe

##### Get started fast

Step-by-step guides, real-world examples, and proven strategies to maximize your test reporting success.

[Provar to Playwright Migration](https://testdino.com/blog/provar-to-playwright)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### Provar to Playwright Migration

Discover why teams are switching from Provar to Playwright and learn the exact steps to migrate your Salesforce tests.

![Krupa Gandhi](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-59.webp&w=3840&q=75)

[Krupa Gandhi](https://testdino.com/blog/author/krupa-gandhi)·Aug 19, 2026

[Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes](https://testdino.com/blog/salesforce-playwright-test-in-cicd)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

PlaywrightSalesforce

###### Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes

Sandbox refreshes keep breaking your Salesforce Playwright tests in CI/CD? This guide covers auth, config, and pipeline setup to fix that for good.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 17, 2026

[How to Pierce Salesforce Shadow DOM in Playwright](https://testdino.com/blog/salesforce-shadow-dom)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### How to Pierce Salesforce Shadow DOM in Playwright

Struggling with hidden elements inside Salesforce Lightning components? Learn how Playwright automatically pierces Shadow DOM and makes Salesforce UI testing reliable.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 6, 2026

[Browse all](https://testdino.com/blog)

We use cookies to measure how the site is used and to improve it. You can accept analytics cookies or keep only the essential ones. [Privacy policy](https://testdino.com/privacy-policy)

Reject non-essentialAccept all

### 30. TestDino — Playwright 1.61 release

- Source: https://testdino.com/blog/playwright-1-61-release
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright 1.61: Passkey & Web Storage Tests, with Code \| TestDino

Playwright 1.61 brings WebAuthn passkeys, a Web Storage API, new video retention modes, and per-error reporting, with no breaking changes.

[![Jashn Jain Author Profile Image](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FJashn-Jain-profile-picture-2.webp&w=3840&q=75)\\
\\
Jashn Jain\\
\\
Updated Jun 30, 2026](https://testdino.com/blog/author/jashn-jain)





![Playwright 1.61: Passkey & Web Storage Tests, with Code | TestDino](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F04%2FPlaywright-1.61_-Passkey-Web-Storage-Tests-with-Code-_-TestDino.webp&w=3840&q=75)

Playwright 1.61 shipped on June 15, 2026.

2 headline features, plus a run of smaller API and test-runner additions. The 2 that lead the release notes:

1. browserContext.credentials adds a virtual WebAuthn authenticator, so passkey and passwordless login flows finally run in CI without a hardware key.

2. page.localStorage and page.sessionStorage provides a first-class read/write API instead of evaluate() round-trips.


Behind those, the changes most teams will reach for:

- 3 new video modes, led by retain-on-failure-and-retries, let a flaky run keep the footage that matters and discard the rest.

- testInfo.errors now splits an AggregateError into one entry per failure, so reporters and dashboards see clean, separated results.

- Smaller wins: expect.soft.poll(), apiResponse.securityDetails(), WebSocket traffic in HAR and trace, and a -G shorthand for --grep-invert.


And the best part: there are no breaking changes. Nothing is deprecated, so the upgrade is a one-liner, and every test that passed on 1.60 still passes.

This guide walks through every 1.61 feature with code you can copy into your Playwright project, plus a worked end-to-end demo that ties the new APIs together.

TL;DR

- **2 headline features:** WebAuthn passkeys via browserContext.credentials and a WebStorage API on page.localStorage / page.sessionStorage. Plus smaller additions worth having: 3 new video retention modes, expect.soft.poll(), testInfo.errors that splits an AggregateError, and WebSocket traffic in HAR and trace recordings.
- **Built for auth and diagnostics:** passkey login flows finally run in CI without hardware keys, and a flaky retry can now keep exactly the video you need instead of all of them or none.
- **No breaking changes.** This is the first release in the recent run that deprecates nothing. The upgrade is a one-liner and existing tests keep passing.
- **Upgrade now** if you test passkey or WebAuthn login, read or seed localStorage/sessionStorage, or have ever fought the all-or-nothing video setting on flaky retries.
- **Wait** if you are mid-sprint and none of the above applies. Nothing forces the move, but nothing breaks either.

##### Playwright 1.61 breaking changes and upgrade guide

###### The one-line upgrade

terminal

```
npm install -D @playwright/test@1.61.0
npx playwright install
```

The second command pulls the browser binaries that ship with this release. Run it or the new browser versions below will not be on disk.

###### Browser versions in 1.61

| Browser | Version |
| --- | --- |
| Chromium | 149.0.7827.55 |
| Mozilla Firefox | 151.0 |
| WebKit | 26.5 |
| Google Chrome (stable) | 149 |
| Microsoft Edge (stable) | 149 |

###### Breaking changes

None. Playwright 1.61 removes no APIs and deprecates nothing. Every test that passed on [1.60](https://testdino.com/blog/playwright-1-60-release "https://testdino.com/blog/playwright-1-60-release") passes on 1.61 without edits. That is rare for this series, so the only real upgrade question is whether the new features are worth adopting now, covered in [_Should you upgrade to Playwright 1.61?_](https://testdino.com/blog/playwright-1-61-release#should-you-upgrade-to-playwright-1-61) below.

![Tip Icon](https://cms.testdino.com/wp-content/uploads/2026/02/GitHub_Invertocat_Black_Clearspace.svg)Explore the Code & Architecture

Want to get your hands on the full, runnable examples from this post and see a complete technical breakdown of the 1.61 release?

**Check out our Playwright 1.61 Release Repository on GitHub.**

**[View on GitHub ↗](https://github.com/testdino-hq/playwright-releases/tree/main/1.61 "https://github.com/testdino-hq/playwright-releases/tree/main/1.61")**

It contains all the standalone test scripts used in this guide, alongside a comprehensive README featuring architectural flowcharts, API migration paths, and in-depth release notes.

##### How to test WebAuthn passkeys in Playwright (no hardware key)

Playwright 1.61 tests passkey logins with a virtual authenticator, so you register a credential and handle the WebAuthn ceremony in CI without a hardware key. Passkeys and WebAuthn rely on a hardware or platform authenticator that responds to navigator.credentials.create() and navigator.credentials.get(). In CI, there is no fingerprint reader or security key, so those flows were either skipped or stubbed at the network layer, which never exercised the real ceremony.

1.61 adds a Credentials virtual authenticator, available on browserContext.credentials. It registers passkeys and handles credential ceremonies on the page, with no real hardware, across browsers.

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-webauthn-virtual-authenticator.png)

###### Registering a passkey before the test runs

tests/passkey-login.spec.ts

```
// tests/passkey-login.spec.ts
import { test, expect } from '@playwright/test';
test('user logs in with a registered passkey', async ({ browser }) => {
  const context = await browser.newContext();
  // Seed a passkey for the origin under test, then arm the authenticator
  await context.credentials.create('example.com', makePasskey());
  await context.credentials.install();
  const page = await context.newPage();
  await page.goto('https://example.com/login');
  await page.getByRole('button', { name: 'Sign in with a passkey' }).click();
  await expect(page.getByText('Welcome back')).toBeVisible();
});
```

makePasskey() builds a passkey from an EC P-256 key pair, exporting the keys as base64url DER, which is exactly the shape credentials.create() expects:

helpers/passkey.ts

```
import { generateKeyPairSync, randomBytes } from 'crypto';
function makePasskey() {
  const { privateKey, publicKey } = generateKeyPairSync('ec', { namedCurve: 'P-256' });
  return {
    id: randomBytes(16).toString('base64url'),
    userHandle: randomBytes(8).toString('base64url'),
    privateKey: privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64url'),
    publicKey: publicKey.export({ type: 'spki', format: 'der' }).toString('base64url'),
  };
}
```

credentials.create() registers the passkey for a given origin, and credentials.install() arms the virtual authenticator so the page sees it. From there, the login button triggers the real navigator.credentials.get() ceremony and the virtual authenticator answers it.

###### Reading registered passkeys back

credentials.get() returns the passkeys registered on the authenticator, which is useful when one test registers a credential and a later step needs to assert it exists or reuse its handle.

tests/passkey-readback.spec.ts

```
const registered = await context.credentials.get();
expect(registered.length).toBe(1);
```

###### When to use it

Any flow that ends in a passkey or WebAuthn prompt: [passwordless login](https://testdino.com/blog/playwright-authentication "https://testdino.com/blog/playwright-authentication"), step-up authentication, or device registration. Before 1.61, these were the tests most teams marked test.skip() in CI. Now they run on every commit like any other login test.

##### Read and write localStorage and sessionStorage in Playwright tests

Reading localStorage or sessionStorage used to mean an evaluate() round-trip into the page, which is verbose and easy to get wrong when values are JSON. 1.61 adds a WebStorage API on page.localStorage and page.sessionStorage that reads and writes the storage for the page's current origin directly.

###### Before 1.61: evaluate into the page

tests/storage-before.spec.ts

```
// Read the demo store's auth token
const token = await page.evaluate(() =>
  localStorage.getItem('user_access_token'));
```

###### After 1.61: a first-class API

tests/storage.spec.ts

```
// tests/storage.spec.ts
import { login } from './helpers';
// Log in for real; the app persists a JWT under `user_access_token`
await login(page);
// Read the token the app set at login, no evaluate() round-trip
const token = await page.localStorage.getItem('user_access_token');
expect(token).toBeTruthy();
// Pull the whole bag of items at once
const all = await page.localStorage.items();
expect(all.some((i) => i.name === 'user_access_token')).toBe(true);
```

items() returns every key-value pair for the current origin, which is handy for snapshotting the storage state in a fixture or asserting that a feature flag landed where you expect it to.

Running this against the demo store prints the real token the app stored and the list of localStorage keys, with both storage tests passing:

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-web-storage-terminal-output.png)

###### When to use it

Reading the user\_access\_token the app set at login, asserting that a cart or consent flag persisted, or clearing storage between steps. It reads more cleanly than evaluate() and keeps the test's intent obvious.

##### Network: Security and address details on API responses

2 methods come into the API request context so that responses fetched through request expose the same low-level details the browser already gave you.

- apiResponse.securityDetails() returns the TLS/security details of the response, mirroring the browser-side equivalent.

- apiResponse.serverAddr() returns the server IP address and port the response came from.


tests/api-response.spec.ts

```
// tests/api-response.spec.ts
import { test, expect, request as apiRequest } from '@playwright/test';
test('inspect TLS and server address of an API response', async () => {
  const request = await apiRequest.newContext();
  // Any HTTPS endpoint exposes these; here we hit the demo store
  const response = await request.get('https://storedemo.testdino.com');
  expect(response.ok()).toBeTruthy();
  const security = await response.securityDetails(); // issuer, protocol, validity
  expect(security?.protocol).toBeTruthy();
  const addr = await response.serverAddr();         // { ipAddress, port }
  expect(addr?.port).toBeGreaterThan(0);
  await request.dispose();
});
```

Run against the demo store, both methods return real data: the TLS securityDetails (issuer, TLSv1.3, validity window) and the serverAddr IP and port.

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-api-response-securitydetails-serveraddr-terminal.png)

###### When to use it

Asserting that an API endpoint serves over the expected TLS protocol, that a certificate is the one you provisioned, or that traffic is hitting the right host or port behind a load balancer.

##### Screencast and CDP improvements

1.59 introduced page.screencast for [recording video](https://testdino.com/blog/playwright-screencast "https://testdino.com/blog/playwright-screencast"). 1.61 sharpens it and adds an artifacts option to CDP connections.

- screencast.showActions() gains a cursor option that controls how the pointer is decorated during recorded actions, so the cursor is clearer in the captured video.

- screencast.start() now passes a timestamp to its onFrame callback, so each frame is timestamped as it arrives.

- browserType.connectOverCDP() gains an artifactsDir option that controls where traces and downloads are stored for the connected session.


tests/screencast.spec.ts

```
// tests/screencast.spec.ts
import { test, expect } from '@playwright/test';
test('record a flow with a clearer cursor', async ({ page }) => {
  await page.screencast.start({
    path: 'artifacts/checkout.webm',
    onFrame: (frame) => {
      // frame.timestamp is new in 1.61: align frames to test steps
      console.log('frame at', frame.timestamp, 'ms');
    },
  });
  await page.goto('https://storedemo.testdino.com');
  // Decorate the pointer so actions are obvious in the captured video
  await page.screencast.showActions({ cursor: 'pointer' });
  // Drive a real flow: add a product, open the cart
  await page.getByTestId('add-to-cart-button').click();
  await page.getByTestId('cart-button').click();
  await page.screencast.stop();
});
```

The onFrame callback now receives the per-frame timestamp, which the run prints for the first frame alongside the total frames captured:

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-screencast-onframe-timestamp-terminal-1.webp)

cdp.ts

```
// Connect to a running browser and control where artifacts land
const browser = await chromium.connectOverCDP('http://localhost:9222', {
  artifactsDir: 'cdp-artifacts',
});
```

###### When to use it

cursor and the frame timestamp matter when a screencast is the thing a teammate watches to understand a failure. artifactsDir matters when you connect over CDP to a remote or shared browser and need its traces and downloads in a known folder.

##### Playwright 1.61 test runner: retries, soft polling, and per-error reporting

The largest cluster of changes lands in the test runner, and most of them are about diagnosing flaky and failing tests with less waste.

###### 3 new video retention modes

The video option got finer control over which retries keep their recording. The old setting was effectively all videos or none, which meant either a noisy artifacts folder or no footage of the run that actually failed.

playwright.config.ts

```
// playwright.config.ts
export default defineConfig({
  use: {
    video: 'retain-on-failure-and-retries',
  },
});
```

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-video-retention-modes-matrix.png)

The 3 new modes:

| Mode | Keeps video when |
| --- | --- |
| on-all-retries | Every retry attempt, not the first run |
| retain-on-first-failure | The first attempt fails, then discards on a passing retry |
| retain-on-failure-and-retries | The run fails, plus all of its retries |

These join the existing on, off, retain-on-failure, and on-first-retry modes. For a [flaky suite](https://testdino.com/blog/playwright-flaky-tests "https://testdino.com/blog/playwright-flaky-tests"), retain-on-failure-and-retries is usually the one you want: you get the footage of the failure and every retry that tried to reproduce it, and nothing for the runs that have passed.

###### Soft polling assertions with expect.soft.poll()

expect.poll() retries a value until it matches; expect.soft() records a failure without stopping the test. 1.61 combines them: expect.soft.poll() polls a value and, if it never matches, logs a soft failure and lets the test keep running.

tests/cart.spec.ts

```
// tests/cart.spec.ts
// After adding a product, the cart badge may take a moment to update.
// Poll the visible count, but don't abort the test if it lags.
await expect.soft.poll(async () => {
  const text = await page
    .getByTestId('header-cart-count')
    .textContent()
    .catch(() => '0');
  return Number(text?.match(/\d+/)?.[0] ?? 0);
}, { timeout: 5000 }).toBeGreaterThan(0);
// The test continues and can assert other things even if the poll above failed
```

In the trace, the soft poll retries the cart badge after a product is added and the test carries on past it either way:

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-soft-poll-cart-count-trace-1.webp)

###### testInfo.errors now splits AggregateError

When a test throws an AggregateError, for example several soft assertions failing together, testInfo.errors now lists each sub-error separately instead of collapsing them into one entry. Reporters and post-run tooling see one error per actual failure.

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-testinfo-errors-aggregateerror-split.png)

reporters/log-errors.ts

```
// In a custom reporter or afterEach hook
for (const error of testInfo.errors) {
  // Each soft-assertion failure is now its own entry
  console.log(error.message);
}
```

With three soft assertions failing together, the afterEach hook prints testInfo.errors count: 3 and one line per failure instead of a single merged entry:

The same split shows up in the HTML report, where each failed soft assertion gets its own error block with its expected and received values:

![Playwright 1.61 testInfo.errors reporting three separate AggregateError entries](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-testinfo-errors-split-terminal-1-1024x595.webp)![Playwright 1.61 HTML report showing each AggregateError sub-error as a separate block](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-testinfo-errors-html-report-2-1024x919.webp)

Playwright 1.61 testInfo.errors reporting three separate AggregateError entries

![Playwright 1.61 testInfo.errors reporting three separate AggregateError entries](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-testinfo-errors-split-terminal-1-1024x595.webp)![Playwright 1.61 HTML report showing each AggregateError sub-error as a separate block](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-testinfo-errors-html-report-2-1024x919.webp)

This is the change with the most downstream value. Anything that consumes Playwright's results, whether a custom reporter, a dashboard, or a test intelligence platform, now gets clean, separate failures instead of a single merged blob. If you route results to TestDino, this is what makes [per-error grouping](https://docs.testdino.com/guides/playwright-error-grouping "https://docs.testdino.com/guides/playwright-error-grouping") accurate when a test fails multiple assertions at once.

###### Other runner additions

- fullConfig.failOnFlakyTests mirrors the config option so reporters can read whether the run is set to fail on flaky tests.

- fullConfig.argv exposes a snapshot of process.argv from the runner process, useful for reporters that need to know how the run was invoked.

- -G is a new command-line shorthand for --grep-invert so that you can exclude tests by title with one flag.


terminal

```
#### Run everything except tests tagged @slow
npx playwright test -G @slow
```

##### Other improvements

- **WebSocket requests now appear in HAR and trace recordings.** Previously, HAR and trace captured HTTP traffic but not WebSocket frames, which left a blind spot for apps that push data over a socket. Real-time features now show up in the trace alongside everything else.

- **Ubuntu 26.04 is supported.** The browser binaries and Docker images now run on the latest Ubuntu LTS.


##### A complete 1.61 workflow: Login, storage, order, diagnosed failure

Here is a single test against the [TestDino demo store](https://storedemo.testdino.com/ "https://storedemo.testdino.com/") that ties several 1.61 features together: log in, read the auth token through the new Web Storage API, place an order with a clearer recorded cursor, and let a soft poll record a diagnostic without aborting the run.

tests/complete-1-61.spec.ts

```
// tests/complete-1-61.spec.ts
import { test, expect } from '@playwright/test';
import { STORE, DEMO_USER, DEMO_PASS } from './helpers';
test.use({ video: 'retain-on-failure-and-retries' });
test('login, token check, order, and soft-polled count', async ({ page }) => {
  // 1. Log in through the real login page
  await page.goto(`${STORE}/login`);
  // Clearer cursor in the recorded video (1.61)
  await page.screencast.showActions({ cursor: 'pointer' });
  await page.getByTestId('login-email-input').fill(DEMO_USER);
  await page.getByTestId('login-password-input').fill(DEMO_PASS);
  await page.getByTestId('login-submit-button').click();
  await expect(page).not.toHaveURL(/\/login$/, { timeout: 15000 });
  // 2. Read the token the app stored, no evaluate() round-trip (1.61)
  const token = await page.localStorage.getItem('user_access_token');
  expect(token).toBeTruthy();
  // 3. Add a product, then soft-poll the cart count; a miss is logged,
  //    not fatal (1.61)
  await page.goto(`${STORE}/products`);
  await page.getByTestId('all-products-cart-button').first().click();
  await expect.soft.poll(async () => {
    const text = await page
      .getByTestId('header-cart-count')
      .textContent()
      .catch(() => '0');
    return Number(text?.match(/\d+/)?.[0] ?? 0);
  }, { timeout: 5000 }).toBeGreaterThan(0);
  // 4. Walk the real checkout all the way to placing an order
  await page.getByTestId('header-cart-icon').click();
  await page.getByTestId('checkout-button').click();
  await expect(page.getByTestId('checkout-title')).toBeVisible();
  await page.getByTestId('checkout-cod-button').click();
  await page.getByTestId('checkout-place-order-button').click();
});
```

![](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-1-61-complete-workflow-trace.png)

If the soft poll never satisfies, the test still finishes and testInfo.errors reports it as its own entry, separate from any other failure in the run. With retain-on-failure-and-retries set, you keep the video of the failing attempt and every retry, and the WebSocket traffic the page used is now in the trace too.

##### Should you upgrade to Playwright 1.61?

**Upgrade now** if any of these describe your suite:

- You test passkey, WebAuthn, or passwordless login and have been skipping or stubbing it in CI.

- You read, seed, or assert localStorage / sessionStorage and want it off evaluate().

- You have a flaky suite and want video for the failure and its retries without keeping everything.

- You consume Playwright results in a reporter or dashboard and want clean per-error reporting from AggregateError.

- Your app uses WebSockets, and you have wanted them in the trace.


**Wait** if none of the above applies and you are mid-sprint. There is no pressure: 1.61 has no breaking changes, so the upgrade is safe whenever you get to it. The decision is purely about whether the new features pay off for you now.

The upgrade itself is the one-liner from the top. Because nothing is deprecated, there is no migration step and no grep to run for removed APIs.

##### Key takeaways

- **WebAuthn passkeys are now testable in CI** through the browserContext.credentials virtual authenticator, with no hardware key and support across all browsers.

- page.localStorage and page.sessionStorage give storage a first-class read/write API instead of evaluate() calls.

- **3 new** video modes let you keep exactly the recordings a flaky run needs, with retain-on-failure-and-retries the safe default for unstable suites.

- expect.soft.poll() polls without aborting, and testInfo.errors now report each AggregateError sub-error on its own.

- **No breaking changes** make 1.61 the lowest-risk upgrade in the recent series.


##### FAQs

Do I need real hardware to test passkeys in 1.61?

No. The browserContext.credentials virtual authenticator registers passkeys and answers the WebAuthn ceremony in the page across all browsers, so CI runs with no physical key.

What is the difference between page.localStorage and the old evaluate() approach?

Both read and write the same storage, but page.localStorage is a typed, first-class API with getItem, setItem, and items(). It avoids the evaluate() round-trip and reads more clearly in a test.

Which video mode should a flaky suite use?

retain-on-failure-and-retries in most cases. It keeps the video of the failing run and every retry while discarding recordings for runs that passed, so your artifacts folder stays small but useful.

How is expect.soft.poll() different from expect.poll()?

expect.poll() aborts the test if the value never matches. expect.soft.poll() records a soft failure and lets the test continue, so you can collect more than one diagnostic in a single run.

Why does testInfo.errors splitting AggregateError matter?

Without it, several failures thrown together collapse into one error entry. Splitting them means reporters, dashboards, and grouping tools see one error per real failure instead of a merged blob.

Is upgrading to 1.61 risky?

It is the lowest-risk upgrade in recent releases. There are no breaking changes and nothing is deprecated, so the one-line install is all it takes and existing tests are unaffected.

What's new in Playwright 1.61?

Playwright 1.61 adds a WebAuthn virtual authenticator for passkey testing, a first-class Web Storage API (page.localStorage and page.sessionStorage), three new video retention modes, and per-error reporting that splits an AggregateError into one entry per failure. There are no breaking changes.

Does Playwright 1.61 have breaking changes?

No. Playwright 1.61 has no breaking changes, and nothing is deprecated. The upgrade is a one-line install and existing tests run unchanged. It ships Chromium 149, Firefox 151, and WebKit 26.5.

[![Jashn Jain Author Profile Image](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FJashn-Jain-profile-picture-2.webp&w=3840&q=75)](https://testdino.com/blog/author/jashn-jain)

[**Jashn Jain**](https://testdino.com/blog/author/jashn-jain)

Developer Advocate

Jashn Jain is a Developer Advocate at TestDino, focusing on automation strategy, developer education, and applied AI in testing. She creates practical resources that help engineering teams adopt modern, Playwright-based automation practices.

With a strong command of the modern testing toolchain, from no-code automation to observability platforms, she has a clear view of how AI is reshaping the developer's role. Her content turns complex tooling decisions into practical guidance teams can act on.

[LinkedIn](https://www.linkedin.com/in/jashn-jain-236586237/ "LinkedIn")[GitHub](https://github.com/JashnJJain "GitHub")

[View all posts →](https://testdino.com/blog/author/jashn-jain)

Table of content

![](https://testdino.com/_next/image?url=%2Fimages%2Fcta-background-image.webp&w=3840&q=75)

Enjoyed this guide?

Get Playwright testing tips in your inbox.

Subscribe

##### Get started fast

Step-by-step guides, real-world examples, and proven strategies to maximize your test reporting success.

[Provar to Playwright Migration](https://testdino.com/blog/provar-to-playwright)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### Provar to Playwright Migration

Discover why teams are switching from Provar to Playwright and learn the exact steps to migrate your Salesforce tests.

![Krupa Gandhi](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-59.webp&w=3840&q=75)

[Krupa Gandhi](https://testdino.com/blog/author/krupa-gandhi)·Aug 19, 2026

[Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes](https://testdino.com/blog/salesforce-playwright-test-in-cicd)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

PlaywrightSalesforce

###### Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes

Sandbox refreshes keep breaking your Salesforce Playwright tests in CI/CD? This guide covers auth, config, and pipeline setup to fix that for good.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 17, 2026

[How to Pierce Salesforce Shadow DOM in Playwright](https://testdino.com/blog/salesforce-shadow-dom)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### How to Pierce Salesforce Shadow DOM in Playwright

Struggling with hidden elements inside Salesforce Lightning components? Learn how Playwright automatically pierces Shadow DOM and makes Salesforce UI testing reliable.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 6, 2026

[Browse all](https://testdino.com/blog)

We use cookies to measure how the site is used and to improve it. You can accept analytics cookies or keep only the essential ones. [Privacy policy](https://testdino.com/privacy-policy)

Reject non-essentialAccept all

### 31. TestDino — Playwright architecture

- Source: https://testdino.com/blog/playwright-architecture
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Architecture: Complete Visual Guide to How it Works (2026)

Learn how Playwright’s 3-layer architecture controls browsers using WebSocket and Chrome DevTools Protocol.

[![Jashn Jain Author Profile Image](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FJashn-Jain-profile-picture-2.webp&w=3840&q=75)\\
\\
Jashn Jain\\
\\
Updated Mar 15, 2026](https://testdino.com/blog/author/jashn-jain)





![Playwright-Architecture_](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F03%2FPlaywright-Architecture_-Complete-Visual-Guide-to-How-it-Works-2026.webp&w=3840&q=75)

You write page.click(). The browser clicks a button.

Simple, right? Except between your code and that click, 4 protocol layers fire, 2 communication channels open, and at least 3 separate processes coordinate.

Most Playwright guides explain one of those layers. Maybe two. None traces a real command through the full stack.

**Playwright's architecture has 3 core layers and 2 communication protocols**. It's what makes Playwright [faster than Selenium](https://testdino.com/blog/playwright-vs-selenium "https://testdino.com/blog/playwright-vs-selenium/") and architecturally different from [Cypress](https://testdino.com/blog/selenium-vs-cypress-vs-playwright "https://testdino.com/blog/selenium-vs-cypress-vs-playwright/").

But the interesting part isn't the layers themselves. _It's how they talk to each other, and what breaks when they don't._

This guide traces a real Playwright command from your test file to the browser engine and back.

![](https://cms.testdino.com/wp-content/uploads/2026/03/watermarked-1.png)

What is Playwright architecture?

Playwright architecture is the internal system of layers, protocols, and processes that connects your test code to the browser. It follows a client-server model where the test script (client) communicates with a server process, which then controls browser engines through the Chrome DevTools Protocol (CDP).

Here's what that means in practice.

Your test file doesn't talk to the browser directly. It sends instructions to a **Playwright Server** process over a WebSocket connection. That server then translates your instructions into CDP commands and forwards them to the browser engine.

This is fundamentally different from how Selenium works. [Selenium uses HTTP requests](https://www.selenium.dev/blog/2022/using-java11-httpclient/ "https://www.selenium.dev/blog/2022/using-java11-httpclient/") for each command. Playwright keeps a persistent WebSocket connection open.

The result? Faster command execution, real-time event streaming, and less overhead per action.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Think of it like texting vs. making a phone call. Selenium "calls" the browser for every action (connect, speak, hang up, repeat). Playwright keeps an open "text thread" where messages flow both ways instantly.

##### The 3 core layers of Playwright architecture

Playwright's architecture is a 3-layer stack. Each layer has a specific job.

| Layer | What it does | Where it runs |
| --- | --- | --- |
| **Client libraries** | Exposes the API you write tests with (page.click(), expect()) | Your test process (Node.js, Python, Java, .NET) |
| **Playwright server** | Translates API calls into browser-specific protocol commands | Separate process, same machine |
| **Browser engines** | Executes commands and renders pages | Chromium, Firefox, or WebKit processes |

![](https://cms.testdino.com/wp-content/uploads/2026/03/watermarked-3.png)

Let's break each one down.

###### Layer 1: Client libraries (language bindings)

This is the layer you interact with every day. When you write page.goto('https://example.com'), you're calling the Playwright client library.

Playwright supports 4 languages:

- **TypeScript/JavaScript** (primary, most features ship here first)

- **Python**

- **Java**

- **.NET (C#)**


Each language binding translates your code into protocol messages that the Playwright Server understands. The TypeScript/JavaScript client communicates in-process with the server. The Python, Java, and .NET clients communicate with the server over a WebSocket transport layer.

**Note:** The JavaScript client and server run in the same Node.js process by default. For other languages, the server runs as a separate Node.js process that the client connects to. This is why you need Node.js installed even when writing Playwright tests in Python or Java.

###### Layer 2: Playwright server (core engine)

The server is the brain. It sits between your test code and the browser, handling:

- **Command translation:** Converts high-level API calls (like page.click()) into low-level protocol messages

- **Auto-waiting logic:** Checks element visibility, stability, and actionability before executing actions

- **Event routing:** Streams browser events (console logs, network requests, page loads) back to your test

- **Connection management:** Maintains persistent connections to one or more browser instances


This layer is what gives Playwright its speed advantage. Instead of making individual HTTP round-trips like Selenium's WebDriver, the server maintains a persistent bidirectional connection to the browser.

The server also handles Playwright's [built-in auto-wait mechanism](https://playwright.dev/docs/actionability "https://playwright.dev/docs/actionability"). Before clicking an element, it automatically verifies that the element is visible, stable, enabled, and not obscured. This happens at the server layer, not in your test code.

###### Layer 3: Browser engines (Chromium, Firefox, WebKit)

Playwright ships with 3 browser engines. Not browser wrappers. Actual patched browser binaries.

| Engine | What it powers | Playwright modification |
| --- | --- | --- |
| Chromium | Chrome, Edge | Uses CDP natively, minimal patches |
| Firefox | Firefox | Custom protocol patches (CDP-like layer added by Playwright team) |
| WebKit | Safari | Custom protocol patches (CDP-like layer added by Playwright team) |

This is a critical architectural detail. Selenium relies on each browser vendor to build and maintain its own WebDriver implementation. Playwright takes a different approach: the team patches Firefox and WebKit directly to support CDP-style communication.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** This is why Playwright can offer features that work identically across all 3 browsers. The Playwright team controls the protocol layer, not the browser vendors.

Each browser engine runs as its own process (or set of processes). Chromium, for example, runs a main browser process plus separate renderer processes for each tab.

##### Communication protocols: WebSocket and CDP

The protocols are what make Playwright fast. Two protocols work together.

###### How WebSocket powers Playwright's speed

WebSocket is the transport layer between your test code and the Playwright Server (for remote connections), and between the server and browser.

Here's why this matters:

| Protocol | Connection type | Overhead per command | Bidirectional? |
| --- | --- | --- | --- |
| HTTP (Selenium WebDriver) | New connection with each command | High (TCP handshake + HTTP headers) | No (request-response only) |
| WebSocket (Playwright) | Persistent, always-on | Minimal (small frame header) | Yes (server can push events) |

With HTTP, every click() or goto() requires: open connection → send request → wait for response → close connection. Repeat for the next command.

With WebSocket, the connection opens once and stays open. Commands flow as lightweight frames. The browser can also push events back (like "page loaded" or "console error") without the client asking.

**Note:** The WebSocket overhead difference is most noticeable in tests with many sequential actions. A test with 50 actions might save 100-200ms in protocol overhead alone compared to an HTTP-based approach.

What is CDP (Chrome DevTools Protocol)?

**CDP** (Chrome DevTools Protocol) is the low-level messaging protocol that Playwright uses to control Chromium-based browsers. It sends JSON-formatted commands for navigation, DOM manipulation, network interception, and more.

When you open Chrome DevTools and inspect network traffic, you're using the same protocol. Playwright just uses it programmatically.

Here's what a real CDP message looks like when Playwright navigates to a page:

cdp-page.json

```
// Command sent by Playwright Server → Browser
{
"id": 1,
   "method": "Page.navigate",
   "params": {
     "url": "https://example.com"
  }
}
// Response from Browser → Playwright Server
{
"id": 1,
  "result": {
    "frameId": "A1B2C3D4E5",
    "loaderId": "F6G7H8I9J0"
  }
}
```

Each CDP command has:

- A unique id for matching responses to requests

- A method that maps to a specific browser capability

- params with the action details


Playwright's [CDPSession API](https://playwright.dev/docs/api/class-cdpsession "https://playwright.dev/docs/api/class-cdpsession") even lets you send raw CDP commands directly when you need low-level control:

cdp-sessions.spec.ts

```
const client = await page.context().newCDPSession(page);

await client.send('Animation.enable');
client.on('Animation.animationCreated', () => {
console.log('Animation detected!');
});
```

###### How Playwright extends CDP for cross-browser support

Here's the part most guides skip.

CDP was built by Google for Chromium. Firefox and WebKit don't natively speak CDP.

So the Playwright team did something unconventional. They contributed patches directly to Firefox and WebKit that add a CDP-compatible protocol layer. This means your test code works identically across all [3 browsers](https://testdino.com/blog/playwright-browser-testing) without any framework-level translation.

This is architecturally different from Selenium, where each browser has its own WebDriver implementation with different behaviors and bugs. And it's different from Cypress, which runs inside the browser's JavaScript context and [historically only supported Chromium](https://testdino.com/blog/playwright-vs-cypress).

##### Browser contexts: isolation and parallel execution

What is browser context?

A [**browser context**](https://playwright.dev/docs/api/class-browsercontext) in Playwright is an isolated browser session with its own cookies, local storage, and authentication state. Multiple contexts can run simultaneously within a single browser instance without interfering with each other.

Think of a context like an incognito window. Each one is completely isolated.

browser-contexts.ts

```
// Create two isolated contexts in the same browser
const context1 = await browser.newContext();
const context2 = await browser.newContext();

// These pages don't share cookies, storage, or state
const page1 = await context1.newPage();
const page2 = await context2.newPage();
```

This architecture enables:

- **Parallel test execution** without shared state contamination

- **Multi-user testing** (e.g., testing chat between two users) in a single test

- **Faster test setup** because you don't need to launch separate browser instances


Each context gets its own set of pages, cookies, and permissions. But they all share the same browser process, which means less memory and faster creation compared to launching separate browsers.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Browser contexts are the reason Playwright's [parallel test execution](https://testdino.com/blog/playwright-sharding) works so cleanly. Each worker process gets its own browser context, ensuring complete isolation.

##### How Playwright executes a test (step-by-step trace)

Let's trace what actually happens when you run this simple test:

homepage.spec.ts

```
import { test, expect } from '@playwright/test';

test('check homepage title', async ({ page }) => {
     await page.goto('https://storedemo.cms.testdino.com');
     await page.click('text=Shop Now');
     await expect(page).toHaveTitle(/TestDino/);
});
```

[https://cms.testdino.com/wp-content/uploads/2026/03/20260306-0948-48.0409746.mp4](https://cms.testdino.com/wp-content/uploads/2026/03/20260306-0948-48.0409746.mp4)

Here's the exact sequence, layer by layer:

**Step 1: Test runner starts a worker process.** The Playwright Test runner spawns a worker (a separate Node.js process). This worker creates a browser instance and a fresh [browser context](https://playwright.dev/docs/browser-contexts).

**Step 2: page.goto() triggers.** Your test calls page.goto('https://storedemo.cms.testdino.com'). The client library serializes this into a protocol message and sends it to the Playwright Server.

**Step 3: Server translates to CDP.** The server converts the high-level goto call into a CDP Page.navigate command:

cdp-navigate-trace.json

```
{
"method": "Page.navigate",
"params": { "https://storedemo.cms.testdino.com" }
}
```

**Step 4: CDP message sent over WebSocket.** The CDP command travels via the persistent WebSocket connection to the Chromium process.

**Step 5: Browser navigates.** Chromium processes the navigation. Network requests fire. HTML parses. DOM builds. The page renders.

**Step 6: Browser confirms navigation.** Chromium sends a CDP response back through the WebSocket, including the frame ID and loader ID. The server forwards the confirmation to the client.

**Step 7: page.click() triggers auto-wait.** Before clicking, the server runs Playwright's actionability checks:

- Is the element visible?

- Is it stable (not animating)?

- Is it enabled?

- Is it not obscured by other elements?


Only after ALL checks pass does the server issue the CDP click command.

**Step 8:** expect() **assertion runs.** The assertion toHaveTitle(/Example/) polls the page title through CDP's Runtime.evaluate domain until it matches or times out.

**Step 9: Test passes or fails.** The worker reports the result back to the test runner. The runner aggregates results from all workers and produces the report.

**Note:** This entire sequence, from page.goto() to the assertion passing, typically completes in under 2 seconds for a simple page. The protocol overhead is measured in milliseconds. The browser rendering is where the time goes.

You can inspect this entire flow yourself using Playwright's [Trace Viewer](https://docs.testdino.com/guides/debug-failures/trace-viewer), which records every action, network request, and screenshot as your test runs.

##### Test runner architecture: fixtures, workers, and reporters

This section covers a gap no competitor article addresses: how Playwright's test runner itself is architected.

###### Workers: OS-level parallelism

The test runner orchestrates everything. It spawns **worker processes**, which are separate Node.js OS processes.

Key facts about workers:

- Each worker runs independently with its own browser instance

- Workers can't communicate with each other

- A crashed worker doesn't affect other workers

- The runner reuses workers across test files for speed

- Default worker count = half your CPU cores


playwright.config.ts

```
import { defineConfig } from '@playwright/test';

export default defineConfig({
      workers: process.env.CI ? 4 : undefined,

// CI: fixed at 4 workers
// Local: auto-detect based on CPU cores
});
```

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** On a CI runner with 4 physical cores, 2-4 workers is the sweet spot. Going beyond that causes CPU contention, where workers fight for CPU time, leading to timeouts and [flaky test failures](https://testdino.com/blog/playwright-flaky-tests) that aren't actual bugs.

###### Fixtures: dependency injection for tests

[Fixtures](https://playwright.dev/docs/test-fixtures) are Playwright's dependency injection system. They set up what your tests need (browser, page, authentication) and tear it down after.

There are two scopes:

| Fixture scope | Lifecycle | Use case |
| --- | --- | --- |
| test scope | Created fresh for each test, torn down after | page, context, test-specific data |
| worker scope | Created once per worker, shared across tests | browser, authentication state, database connections |

auth.setup.ts

```
import { test as setup, expect } from '@playwright/test';

setup('authenticate', async ({ page }) => {
   await page.goto('/login');
       await page.getByPlaceholder('Your email address').fill('add here');
       await page.getByPlaceholder('Your password').fill('add here');
       await page.getByRole('button', { name: 'Sign in' }).click();
       await page.waitForURL('/');
       await page.context().storageState({ path: 'auth.json' });
});
```

![auth.json](https://cms.testdino.com/wp-content/uploads/2026/03/image-e1-1024x442.png)![auth.setup.ts](https://cms.testdino.com/wp-content/uploads/2026/03/image-e-1024x471.png)

auth.json

![auth.json](https://cms.testdino.com/wp-content/uploads/2026/03/image-e1-1024x442.png)![auth.setup.ts](https://cms.testdino.com/wp-content/uploads/2026/03/image-e-1024x471.png)

The screenshots above show this in practice. The auth.setup.ts file runs the login flow once and saves the session to auth.json. Every test that needs authentication reuses that saved state instead of logging in again. This is Playwright's storage state approach to authentication: run the setup once, reuse everywhere.

The fixture architecture is what makes Playwright's test isolation so clean. Each test gets exactly the dependencies it needs, with automatic cleanup.

###### Reporters: the output pipeline

Reporters process test results as they happen. Playwright supports [multiple reporters](https://testdino.com/blog/playwright-custom-reporter "https://testdino.com/blog/playwright-custom-reporter/") running simultaneously:

- **List reporter** (default): Real-time console output

- **HTML reporter:** Interactive browser-based report with traces

- **JSON reporter:** Machine-readable output for CI pipelines

- **JUnit reporter:** XML format for CI tools like Jenkins

- **Blob reporter:** Binary format for [merging sharded results](https://testdino.com/blog/playwright-sharding)


playwright.config.ts

```
export default defineConfig({
reporter: [\
    ['list'],\
    ['html', { open: 'never' }],\
    ['json', { outputFile: 'results.json' }],\
  ],
});
```

##### Playwright vs Selenium vs Cypress: Architecture comparison

The architectural differences between these 3 frameworks explain most of their behavioral differences.

![](https://cms.testdino.com/wp-content/uploads/2026/03/Selenium-4-Cypress-Playwright.webp)

| Aspect | Playwright | Selenium | Cypress |
| --- | --- | --- | --- |
| Architecture model | Client → Server → Browser (out-of-process) | Client → WebDriver → Browser (out-of-process) | Runs inside the browser (in-process) |
| Communication protocol | WebSocket + CDP | HTTP + WebDriver protocol | Direct JavaScript execution |
| Browser control | Patches browser engines directly | Relies on vendor-built WebDriver implementations | Injects into browser's JS context |
| Multi-browser support | Chromium, Firefox, WebKit (native) | All major browsers via separate drivers | Chromium, Firefox, WebKit (experimental) |
| Parallel execution | Built-in workers + sharding | Selenium Grid (separate infrastructure) | Paid Dashboard or third-party tools |
| Auto-wait | Built into server layer (actionability checks) | Manual waits required (WebDriverWait) | Automatic retry-ability on assertions |
| Cross-origin support | Full (controls browser at process level) | Full (same) | Limited (runs in-browser, subject to same-origin) |
| Trace recording | [Built-in](https://testdino.com/blog/playwright-trace-viewer "https://testdino.com/blog/playwright-trace-viewer/") (trace.zip with screenshots, network, DOM) | Third-party tools | Screenshot + video only |

###### When to choose each framework

###### Choose Playwright when:

- You need true cross-browser testing (Chromium + Firefox + WebKit)

- Fast [parallel execution](https://testdino.com/blog/playwright-sharding) matters (built-in workers + sharding)

- You want built-in tracing and debugging tools

- Your team runs tests in CI and needs [reliable reporting](https://testdino.com/blog/playwright-reporting)


###### Choose Selenium when:

- You need to test browsers Playwright doesn't support (older IE versions, Opera)

- Your team already has a large Selenium codebase

- You need the largest ecosystem of third-party integrations


###### Choose Cypress when:

- You're testing a single-browser web app (primarily Chrome)

- Developer experience during local development is the priority

- Your team prefers an all-in-one dashboard experience


For a deeper comparison with benchmarks, see the [full framework comparison](https://testdino.com/blog/selenium-vs-cypress-vs-playwright).

##### How Playwright's architecture has evolved

Playwright was created by the same engineers who built Puppeteer at Google. When they moved to Microsoft, they started Playwright with a clear architectural goal: true [cross-browser automation](https://testdino.com/blog/playwright-e2e-testing).

Key architectural milestones:

| Version | Change | Impact |
| --- | --- | --- |
| v1.0 (2020) | Initial release with Chromium, Firefox, WebKit | First framework with native 3-browser support |
| v1.8 | Playwright Test runner introduced | Built-in test framework, no need for Jest/Mocha |
| v1.13 | Trace Viewer introduced | Built-in recording and replay for debugging |
| v1.22 | API testing support added | Combined UI and API testing in one framework |
| v1.29 | UI Mode introduced | Interactive test development and debugging |
| v1.40+ | Component testing (experimental) | Test React, Vue, Svelte components in real browsers |
| v1.56 | [Test Agents](https://testdino.com/blog/playwright-test-agents "https://testdino.com/blog/playwright-test-agents/") (planner, generator, healer) | AI-assisted test creation and self-healing |

The architecture has remained fundamentally stable (client-server with CDP) since v1.0. What's evolved is the test runner, the debugging tools, and most recently, the AI integration layer.

**Note:** The [Playwright MCP server](https://testdino.com/blog/playwright-mcp) (Model Context Protocol) is the latest architectural addition. It exposes Playwright's capabilities as structured tools that AI coding assistants can invoke. This represents a shift from "humans write tests" to "AI agents participate in the testing workflow."

##### Best practices for working with Playwright's architecture

Understanding the architecture changes how you write tests. Here are practices grounded in how the system actually works.

**Use browser contexts, not separate browsers.** Each new browser launch is expensive (new process, new memory allocation). Contexts are cheap (same process, isolated state). For [parallel testing](https://testdino.com/blog/playwright-automation), always use contexts.

**Trust auto-wait.** Don't add manual sleeps. The server layer handles [actionability checks](https://playwright.dev/docs/actionability) before every action. Adding page.waitForTimeout(2000) works against the architecture, not with it.

**Keep test files small for better sharding.** Playwright distributes tests at the file level by default. 20 small files shard better than 3 large ones. For [sharding strategies](https://testdino.com/blog/playwright-sharding), enable fullyParallel: true to distribute at the individual test level.

**Configure workers based on your CI hardware.** More workers ≠ faster tests. On a 4-core CI runner, 2-4 workers are optimal. Beyond that, CPU contention causes timeouts that look like [flaky tests](https://testdino.com/blog/flaky-test-detection-tools) but are actually resource starvation.

**Use trace recording on the first retry.** The trace captures every layer's activity (DOM snapshots, network requests, console logs). Set trace: 'on-first-retry' in your [playwright config](https://testdino.com/blog/playwright-framework-setup) to capture traces only when tests fail and retry.

playwright.config.ts

```
export default defineConfig({
 use: {
   trace: 'on-first-retry',
   screenshot: 'only-on-failure',
   video: 'retain-on-failure',
  },
   retries: process.env.CI ? 2 : 0,
});
```

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Traces are powerful but heavy. For a team running 500+ tests nightly, consider using TestDino's [real-time streaming](https://docs.testdino.com/guides/real-time-streaming "https://docs.testdino.com/guides/real-time-streaming") to centralize trace data instead of storing artifacts in CI. This keeps your [CI pipeline](https://testdino.com/blog/playwright-ci-cd-integrations "https://testdino.com/blog/playwright-ci-cd-integrations/") lean while retaining full debugging context.

##### FAQs

What is the architecture of Playwright?

Playwright uses a 3-layer client-server architecture. Your test code (client) communicates with a Playwright Server process over WebSocket, and the server controls browser engines (Chromium, Firefox, WebKit) through the [Chrome DevTools Protocol](https://playwright.dev/docs/api/class-cdpsession).

What protocol does Playwright use?

Playwright uses the Chrome DevTools Protocol (CDP) to communicate with browsers. For Chromium, it uses CDP natively. For Firefox and WebKit, the Playwright team contributes patches that add a CDP-compatible protocol layer directly into these browsers.

What is the difference between Playwright and Selenium architecture?

Playwright uses a persistent WebSocket connection with CDP to control browsers, while Selenium uses HTTP requests with the WebDriver protocol. Playwright patches browser engines directly for cross-browser support, while Selenium relies on vendor-maintained WebDriver implementations.

What is the Playwright design pattern?

Playwright's recommended design pattern is the [Page Object Model](https://playwright.dev/docs/pom) (POM), where each page of your application is represented by a class that encapsulates its elements and actions. Combined with Playwright's fixture system for dependency injection, this creates clean, maintainable test code.

Can Playwright test multiple browsers at once?

Yes. Playwright natively supports Chromium, Firefox, and WebKit. You can configure [multiple projects](https://testdino.com/blog/playwright-browser-testing) in your playwright.config.ts, each targeting a different browser. All 3 browsers run with the same API & the same behavioral guarantees because Playwright controls the protocol layer for each engine.

[![Jashn Jain Author Profile Image](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FJashn-Jain-profile-picture-2.webp&w=3840&q=75)](https://testdino.com/blog/author/jashn-jain)

[**Jashn Jain**](https://testdino.com/blog/author/jashn-jain)

Developer Advocate

Jashn Jain is a Developer Advocate at TestDino, focusing on automation strategy, developer education, and applied AI in testing. She creates practical resources that help engineering teams adopt modern, Playwright-based automation practices.

With a strong command of the modern testing toolchain, from no-code automation to observability platforms, she has a clear view of how AI is reshaping the developer's role. Her content turns complex tooling decisions into practical guidance teams can act on.

[LinkedIn](https://www.linkedin.com/in/jashn-jain-236586237/ "LinkedIn")[GitHub](https://github.com/JashnJJain "GitHub")

[View all posts →](https://testdino.com/blog/author/jashn-jain)

Table of content

![](https://testdino.com/_next/image?url=%2Fimages%2Fcta-background-image.webp&w=3840&q=75)

Enjoyed this guide?

Get Playwright testing tips in your inbox.

Subscribe

##### Get started fast

Step-by-step guides, real-world examples, and proven strategies to maximize your test reporting success.

[Provar to Playwright Migration](https://testdino.com/blog/provar-to-playwright)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### Provar to Playwright Migration

Discover why teams are switching from Provar to Playwright and learn the exact steps to migrate your Salesforce tests.

![Krupa Gandhi](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2Fimage-59.webp&w=3840&q=75)

[Krupa Gandhi](https://testdino.com/blog/author/krupa-gandhi)·Aug 19, 2026

[Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes](https://testdino.com/blog/salesforce-playwright-test-in-cicd)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

PlaywrightSalesforce

###### Running Salesforce Playwright Tests in CI/CD Across Sandbox Refreshes

Sandbox refreshes keep breaking your Salesforce Playwright tests in CI/CD? This guide covers auth, config, and pipeline setup to fix that for good.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 17, 2026

[How to Pierce Salesforce Shadow DOM in Playwright](https://testdino.com/blog/salesforce-shadow-dom)

![TestDino Default Blog Thumbnail](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FTestDino-Default-Blog-Thumbnail-Image-2.webp&w=3840&q=75)

Playwright

###### How to Pierce Salesforce Shadow DOM in Playwright

Struggling with hidden elements inside Salesforce Lightning components? Learn how Playwright automatically pierces Shadow DOM and makes Salesforce UI testing reliable.

![Ayush Mania](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FAyush-Mania-Profile-Image-2.webp&w=3840&q=75)

[Ayush Mania](https://testdino.com/blog/author/ayush-mania)·Aug 6, 2026

[Browse all](https://testdino.com/blog)

We use cookies to measure how the site is used and to improve it. You can accept analytics cookies or keep only the essential ones. [Privacy policy](https://testdino.com/privacy-policy)

Reject non-essentialAccept all

### 32. Level Up Coding — Custom fixtures in TypeScript

- Source: https://levelup.gitconnected.com/how-to-create-custom-fixtures-in-playwright-typescript-a-complete-practical-guide-4fa8b2fc2c82
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### How to Create Custom Fixtures in Playwright TypeScript: A Complete Practical Guide

[![Mohammad Faisal Khatri](https://miro.medium.com/v2/resize:fill:32:32/1*D_xMmUc2WqF2iL_Q_3GIDg.jpeg)](https://medium.com/@iamfaisalkhatri?source=post_page---byline--4fa8b2fc2c82---------------------------------------)

[Mohammad Faisal Khatri](https://medium.com/@iamfaisalkhatri?source=post_page---byline--4fa8b2fc2c82---------------------------------------)


12 min read

·

Jul 24, 2026

60

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D4fa8b2fc2c82&operation=register&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fhow-to-create-custom-fixtures-in-playwright-typescript-a-complete-practical-guide-4fa8b2fc2c82&source=---header_actions--4fa8b2fc2c82---------------------post_audio_button------------------)


_Learn how to create custom fixtures for using page objects and test data in Playwright tests with a practical step-by-step guide._

> **Medium non-members** [**click here**](https://medium.com/@iamfaisalkhatri/4fa8b2fc2c82?sk=dc65288058ee17332bbffe9c08c8dcb2) **to read the full article.**

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*qJiniexvzyeQWYn7Fxa34Q.jpeg)

If you’ve been writing Playwright tests for a while, you’ve probably noticed the same setup code appearing in multiple test files.

You create page objects, load test data, initialize helper classes, or configure users before every test. Initially, this seems manageable, but as the automation test suite grows, maintaining this duplicated setup becomes increasingly difficult.

This is exactly where custom fixtures in Playwright are especially helpful.

Fixtures are one of Playwright’s most powerful features. They allow us to create reusable, maintainable, and scalable test setups that are automatically available to every test.

In this tutorial, we’ll learn:

- Why custom fixtures are needed?
- What problems they solve?
- When to use a custom fixture?
- How to create custom fixtures for Test Data and Page Objects?
- Pros and limitations of using custom fixtures
- Best practices for building maintainable automation frameworks

##### What Problem Do Custom Fixtures Solve?

Suppose we’re working on a large enterprise application, such as an online banking platform. Over the years, the automation test suite has grown to more than 300 test cases covering login, fund transfers, account management, bill payments, and transaction history.

At the beginning of almost every test, we need the same setup:

```
const loginPage = new LoginPage(page);
const dashboardPage = new DashboardPage(page);

const user = users.standardUser;

await loginPage.login(user.username, user.password);
```

At first, this may not seem like a problem. Copying a few lines of code into multiple tests is quick and easy. However, as the project evolves, requirements change.

For example:

- The _LoginPage_ class is renamed to _AuthenticationPage_
- Test users are no longer stored in a local file; instead, they are retrieved from a centralized test data service
- End-to-end tests need a logged-in user to complete the flow
- The application redirects users to a different landing page after login.

If this setup code has been duplicated across hundreds of test files, every one of those tests must be updated. Even a small architectural change can turn into hours of repetitive maintenance, increasing the likelihood of missed updates and inconsistent implementations.

This is a classic example of violating one of the most important software engineering principles:

> **Don’t Repeat Yourself (DRY).**

Repeated setup code makes the test suite harder to maintain and more error-prone. This is where custom fixtures provide a significant advantage.

With custom fixtures:

- Common setup logic is maintained in a single location.
- Changes are implemented once rather than in hundreds of test files.
- Tests become shorter, cleaner, and focused solely on validating business functionality.
- The overall automation framework becomes easier to maintain as the application grows.

Instead of requiring every test to create page objects, load test data, and perform common setup steps, we can define this logic once in a custom fixture. Playwright then automatically injects the required objects into every test that needs them.

In other words, custom fixtures allow us to write tests that focus on what needs to be tested, while Playwright handles how the required objects and data are prepared.

##### What Are Fixtures?

A fixture is an object or resource that Playwright prepares before the test runs. Test fixtures ensure that every test receives exactly the resources it needs. Each fixture is isolated, which allows tests to remain independent and not interfere with one another.

Playwright already provides built-in fixtures like:

- _page_
- _browser_
- _context_
- _request_

> [Click here](https://medium.com/gitconnected/playwright-browser-vs-browsercontext-vs-page-complete-guide-with-examples-b6c771c8d371?sharedUserId=iamfaisalkhatri) to learn the differences between Browser, BrowserContext, and Page in Playwright TypeScript with practical examples, built-in fixtures, best practices, and real-world use cases.

For example, the following test uses the built-in _page_ fixture in the test:

```
test("page fixture test", async ({ page }) => {
  await page.goto("https://playwright.dev");
  await expect(page).toHaveTitle(/Playwright/);
});
```

Custom fixtures work the same way. Just like the built-in _page_ fixture, we can create our own custom fixture and use it in the tests. Playwright automatically injects the fixture whenever it is required.

###### Project Structure

A common Playwright project structure looks like this:

```
Playwright project
│
├── fixtures
│   ├── pageFixtures.ts
│   ├── testDataFixtures.ts
│   └── baseFixture.ts
│
├── pages
│   ├── LoginPage.ts
│   └── HomePage.ts
    └── DashboardPage.ts
│
├── test-data
│   └── users.ts
│
└── tests
```

- _fixtures/_ – The _fixtures_ folder contains all custom Playwright fixtures. These fixture files create and manage reusable objects, such as page objects and test data, that Playwright automatically injects into the tests.
- _pages/_ – The _pages_ folder contains [Page Object Model (POM)](https://medium.com/@iamfaisalkhatri/what-is-page-object-model-pom-design-pattern-9d0f3e831bdc?sharedUserId=iamfaisalkhatri) classes that encapsulate page interactions and locators, keeping test logic clean and maintainable.
- _test-data/_ – Stores reusable test data required for the tests.
- _tests/_ – Contains the Playwright test files.

Separating fixtures, page objects, test data, and test files into dedicated folders creates a well-structured automation framework.

##### Creating Custom Fixtures for Page Objects and Test Data

One of the most common and practical use cases for custom fixtures is managing Page Object Model (POM) classes.

In a typical Playwright project, each page of the application is represented by a Page Object class. Before interacting with the application, these page objects need to be instantiated inside every test. Likewise, we need to do the same instantiation for the Test Data class.

**Test Scenario:**

```
1. Navigate to the registration Page of the Parabank demo website(https://parabank.parasoft.com/parabank/register.htm).
2. Register a new user.
3. Verify that the successful registration message text.
```

Without using custom fixtures, the test implementation looks like this:

```
import { test, expect} from "@playwright/test";
import { BasePage } from "./page-objects/base-page";
import { UserData } from "../../testdata/UserData";
import { RegistrationPage } from "./page-objects/registration-page";

  test("Should register a new user", async ({ page }) => {
    const basePage = new BasePage(page);
    await basePage.navigateTo("https://parabank.parasoft.com/parabank/register.htm");

    const registrationPage = new RegistrationPage(page);
    await expect(registrationPage.pageHeader).toBeVisible();

    const userData = new UserData();
    let username = userData.username;
    let password = userData.password;

    await registrationPage.registerUser(userData, username, password);
    await expect(registrationPage.welcomeMessageText(username)).toBeVisible();
    await expect(registrationPage.successMessageText).toBeVisible();
  });
```

While this approach works, the same setup code quickly gets duplicated across dozens or even hundreds of test cases. Every new test starts by creating the same page object and test data instances before the actual test logic begins.

Let’s eliminate this repetitive setup by creating a custom fixture that handles the Page Object classes and test data.

**Step 1: Create an _app.fixture.ts_ in the _fixtures/_ folder**

```
import { test as base } from '@playwright/test';

import { BasePage } from '../pages/parabank/base-page';
import { RegistrationPage } from '../pages/parabank/registration-page';
import { UserData } from '../test-data/UserData';

type AppFixtures = {
    basePage: BasePage;
    registrationPage: RegistrationPage;
    userData:UserData;
};

export const test = base.extend<AppFixtures>({

    basePage: async ({ page }, use) => {
        const basePage = new BasePage(page);
        await use(basePage);
    },
    registrationPage: async ({ page }, use) => {
        const registrationPage = new RegistrationPage(page);
        await use(registrationPage);
    },
    userData: async ({ }, use) => {
        const userData = new UserData();
        await use(userData);
    },
});

export { expect } from '@playwright/test';
```

Acustom Playwright fixture is createdby extending the default _test_ object with reusable Page Object and test data instances. It automatically initializes _BasePage, RegistrationPage, and UserData_ classesbefore each test, allowing them to be injected directly into test functions.

This eliminates the need for manually instantiating the Page Object and test data classes in the test. This keeps tests cleaner, reduces duplicate code, and centralizes Page Object creation.

The _expect_ object from `@playwright/test` is re-exported, so when this fixture file is imported into a test, _expect_ is also available from the same module. This allows us to import both the custom _test_ fixture and _expect_ from a single location.

**Step 2: Using the custom _app.fixture.ts_ in the test**

```
import { test, expect } from "../fixtures/app.fixture";
    test("Should register a new user", async ({ basePage, registrationPage, userData }) => {

        await basePage.navigateTo("https://parabank.parasoft.com/parabank/register.htm");

        await expect(registrationPage.pageHeader).toBeVisible();

        let username:string = userData.username;
        let password:string = userData.password;

        await registrationPage.registerUser(userData, username, password);

        await expect(registrationPage.welcomeMessageText(username)).toBeVisible();
        await expect(registrationPage.successMessageText).toBeVisible();
    });
```

Let’s break down what’s happening in the test.

**Importing the Custom Fixture**

```
import { test, expect } from "../fixtures/app.fixture";
```

Instead of importing _test_ from _@playwright/test_, the test imports it from _“app.fixture.ts”(The custom fixture that we created in Step 1)_

This custom fixture extends Playwright’s built-in _test_ object and automatically provides additional fixtures such as _basePage_, _registrationPage_, and _userData_. As a result, every test that imports _test_ from _app.fixture.ts_ has access to these objects without any manual setup.

**Injecting Fixtures into the Test**

```
async ({ basePage, registrationPage, userData })
```

This is where Playwright’s dependency injection comes into action. When the test starts, Playwright automatically creates and injects the required fixtures:

- _basePage_ – Provides common browser and navigation methods used across multiple pages.
- _registrationPage_ – An instance of the _RegistrationPage_ Page Object, containing locators and actions for the registration page.
- _userData_ – Supplies the test data required for user registration, such as the username, password, and other user details.

Since these fixtures are automatically initialized and injected by Playwright, there is no need to manually instantiate them within the test.

**Navigating to the Registration Page**

```
await basePage.navigateTo("https://parabank.parasoft.com/parabank/register.htm");
await expect(registrationPage.pageHeader).toBeVisible();
```

The test navigates to the registration page using a reusable method defined in the _BasePage_ class. Centralizing common actions like navigation helps avoid duplicated code across different page objects.

After navigating to the registration page, the page header is verified to confirm that the test has landed on the correct page before it begins to verify the user registration functionality.

**Retrieving Test Data**

```
let username:string = userData.username;
let password:string = userData.password;
```

The test retrieves the username and password from the _userData_ fixture. Since the fixture already provides the required test data, there is no need to import or create user objects within the test.

**Registering the User**

```
await registrationPage.registerUser(userData, username, password);
```

The _registerUser()_ method encapsulates all the interactions required to complete the registration form. This keeps the test focused on the business scenario rather than implementation details such as filling text fields or clicking buttons.

**Validating the Results**

```
await expect(registrationPage.welcomeMessageText(username)).toBeVisible();
await expect(registrationPage.successMessageText).toBeVisible();
```

Finally, the test verifies that the registration was successful by checking the welcome message and the success confirmation displayed after registration.

[![](https://miro.medium.com/v2/resize:fit:382/1*hTU426wa2kUb73onLP5Rrw.jpeg)](https://github.com/mfaisalkhatri/web-automation-playwright-ts)

###### Why having a custom fixture is a better approach

It can be noticed that the test never creates instances like:

```
const basePage = new BasePage(page);
const registrationPage = new RegistrationPage(page);
const userData = users.standardUser;
```

All of this setup is handled automatically by the custom fixtures. As a result, the test focuses entirely on the business workflow:

1. Navigate to the registration page and verify the page is displayed.
2. Register a new user.
3. Validate the successful registration.

This is one of the biggest advantages of custom fixtures in Playwright: they remove repetitive setup code, keep tests concise and readable, and make the automation framework easier to maintain.

##### Use Cases for Custom Fixtures

Custom fixtures are useful in many real-world scenarios. Let’s explore some of the most common use cases.

- **Page Objects:** Automatically create and inject commonly used page objects into the tests, eliminating repetitive initialization code.
- **Test Data:** Provide reusable test or configuration data without repeated imports.
- **Setting Up Authenticated Sessions:** A custom fixture can be created to log in a user before each test, allowing tests to start directly from an authenticated state.
- **API Clients:** Initialize reusable API helper classes so tests can interact with backend services consistently.
- **Database Helpers:** Set up fixtures for database utilities to prepare or clean test data before and after execution.
- **Utility Classes:** Inject commonly used helpers such as date generators, random data creators, or reporting utilities.

##### Pros, Limitations, and Best Practices for Using Fixtures

Custom fixtures offer several benefits, but they also come with a few limitations.

###### Pros of using fixtures

- **Cleaner Tests:** Custom fixtures remove repetitive setup code from the test files, allowing tests to focus solely on the business scenario being validated. This makes tests easier to read, understand, and review.
- **Better Reusability:** Common setup logic, such as creating page objects, loading test data, or initializing helper classes, is defined once and reused across the entire test suite. This eliminates code duplication and promotes consistency.
- **Easier Maintenance:** When the initialization of a page object or shared resource changes, you only need to update the corresponding fixture instead of modifying hundreds of individual tests. This significantly reduces maintenance effort and minimizes the risk of inconsistencies.
- **Strong Typing:** Custom fixtures integrate seamlessly with TypeScript, providing IntelliSense, auto-completion, and compile-time type checking. This helps in catching errors early and improves the overall development experience.
- **Improved Scalability:** As the automation framework grows, custom fixtures keep tests concise by abstracting common setup logic. This modular approach makes the framework easier to organize, extend, and maintain without increasing complexity.

###### Limitations for using Fixtures

Like any design pattern, fixtures should be used thoughtfully. Though fixtures provide multiple benefits, they have a few limitations, which are given below:

- **Overusing fixtures:** Custom fixtures should be reserved for shared dependencies that provide significant value. Creating a fixture for every helper class can lead to an overly complex framework that is harder to maintain and navigate.
- **Hidden Dependencies:** As fixtures are injected automatically, it may not be immediately obvious where certain objects originate, especially for new team members.
- **Increased Learning Curve:** Teams new to Playwright may need some time to become familiar with concepts such as fixture lifecycle, dependency injection, and fixture scopes before they can use custom fixtures effectively.
- **Debugging Complexity:** As the number of fixture dependencies grows, identifying the root cause of a test failure can become more challenging. Debugging may often require tracing through multiple layers of fixture initialization and setup logic.

###### Best Practices for Using Fixtures

The following are the recommended best practices while using fixtures:

**Keep Fixtures Focused:** Each fixture should have a single responsibility. A fixture should create or provide only one type of resource, such as a page object, test data object, or API client, rather than combining multiple unrelated dependencies. Keeping fixtures focused makes them easier to reuse, test, and maintain. If a fixture becomes responsible for several tasks, it becomes difficult to understand, modify, and debug.

For example, instead of creating a single fixture that initializes page objects, loads test data, and authenticates a user, create separate fixtures for each responsibility.

**Use Meaningful Names:** Choose descriptive fixture names that clearly communicate their purpose. Well-named fixtures make tests easier to read and help team members understand what is being injected without opening the fixture implementation.

For example, prefer names such as:

- _loginPage_
- _dashboardPage_
- _userData_
- _apiClient_

Avoid generic names such as:

- _helper_
- _data_
- _object_

Descriptive naming improves code readability, reduces ambiguity, and makes the framework easier to navigate, especially as it grows.

**Reuse Existing Fixtures:** Before creating a new fixture, check whether an existing one can be reused or extended. Playwright allows you to extend fixtures, making it easy to build additional functionality on top of existing fixtures without duplicating setup code.

Reusing fixtures promotes consistency across the framework, reduces maintenance effort, and keeps the codebase modular. When shared setup logic changes, it only needs to be updated in one place instead of multiple fixture implementations.

**Don’t Put Test Logic Inside Fixtures:** A fixture’s responsibility is to prepare and provide resources, not to execute business scenarios or perform test validations.

For example, creating a _loginPage_ fixture that simply initializes and injects the _LoginPage_ object is a good practice. However, embedding a complete user registration or funds transfer workflow inside a fixture mixes setup with test execution, making tests less transparent and harder to maintain.

Keep business logic inside the test or dedicated page object methods, while fixtures focus solely on providing the dependencies required by the test. This separation of responsibilities results in cleaner, more readable, and easier-to-debug test suites.

**Prefer Composition Over Large Fixtures:** Instead of creating one massive fixture file containing everything, split fixtures by responsibility (for example, page objects, test data, API clients, and utilities) and compose them as needed. This makes the framework easier to maintain and extend.

##### When Should You Use Custom Fixtures?

Custom fixtures are recommended when:

- You repeatedly create the same page objects across multiple tests.
- The same test data is imported in many files.
- Tests require a consistent setup, such as authenticated users or shared utilities.
- Reduce boilerplate and make tests easier to read.
- The automation framework is growing and needs better organization.

For very small or one-off test suites, introducing many custom fixtures may add unnecessary complexity.

> Learn how to [configure multiple environments](https://medium.com/gitconnected/playwright-typescript-multiple-environments-a-complete-real-world-guide-4173bb136d68?sharedUserId=iamfaisalkhatri) in Playwright TypeScript using projects, baseURL, environment variables, dotenv, and real-world best practices.

##### Final Words

Custom fixtures are one of the key features that make Playwright well-suited for building scalable automation frameworks. They help eliminate repetitive setup code, encourage consistency, and keep tests focused on user behavior.

Whether you’re injecting page objects, test data, API clients, or authenticated sessions, custom fixtures enable a cleaner architecture that is easier to extend and maintain over time.

If you’re starting a new Playwright TypeScript project, it’s worth adopting custom fixtures early. Establishing this pattern from the beginning makes it much easier to grow from a handful of tests to larger test suites without sacrificing readability or maintainability.

As the test suite evolves, a well-designed fixture strategy can become one of the biggest contributors to a robust, maintainable, and professional automation framework.

Happy Testing!!

[Programming](https://medium.com/tag/programming?source=post_page---footer_tags--4fa8b2fc2c82---------------------------------------)

[Typescript](https://medium.com/tag/typescript?source=post_page---footer_tags--4fa8b2fc2c82---------------------------------------)

[Software Development](https://medium.com/tag/software-development?source=post_page---footer_tags--4fa8b2fc2c82---------------------------------------)

[Software Engineering](https://medium.com/tag/software-engineering?source=post_page---footer_tags--4fa8b2fc2c82---------------------------------------)

[Software Testing](https://medium.com/tag/software-testing?source=post_page---footer_tags--4fa8b2fc2c82---------------------------------------)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:96:96/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--4fa8b2fc2c82---------------------------------------)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:128:128/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--4fa8b2fc2c82---------------------------------------)


[**Published in Level Up Coding**](https://levelup.gitconnected.com/?source=post_page---post_publication_info--4fa8b2fc2c82---------------------------------------)

[356K followers](https://levelup.gitconnected.com/followers?source=post_page---post_publication_info--4fa8b2fc2c82---------------------------------------)

· [Last published 3 hours ago](https://levelup.gitconnected.com/solving-the-right-problem-in-the-age-of-ai-cba2f2862fca?source=post_page---post_publication_info--4fa8b2fc2c82---------------------------------------)

Coding tutorials and news. The developer homepage [gitconnected.com](http://gitconnected.com/) && [skilled.dev](http://skilled.dev/) && [levelup.dev](http://levelup.dev/)


[![Mohammad Faisal Khatri](https://miro.medium.com/v2/resize:fill:96:96/1*D_xMmUc2WqF2iL_Q_3GIDg.jpeg)](https://medium.com/@iamfaisalkhatri?source=post_page---post_author_info--4fa8b2fc2c82---------------------------------------)

[![Mohammad Faisal Khatri](https://miro.medium.com/v2/resize:fill:128:128/1*D_xMmUc2WqF2iL_Q_3GIDg.jpeg)](https://medium.com/@iamfaisalkhatri?source=post_page---post_author_info--4fa8b2fc2c82---------------------------------------)

### 33. Level Up Coding — Fixtures in 2025

- Source: https://levelup.gitconnected.com/playwright-fixtures-in-2025-the-practical-guide-to-fast-clean-end-to-end-tests-55e9b3f7b5f7
- Retrieved: 2026-08-29
- Firecrawl status: unavailable

Firecrawl returned a Medium member-only paywall / registration wall after scrape with `proxy: stealth` and `waitFor: 3000`. Body after the intro is truncated at “Create an account to read the full story.” No friend-link was available. Content is not invented.

Error: Medium paywall (member-only story; registration wall).
