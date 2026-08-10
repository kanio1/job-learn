# Playwright SDET — Scraped Articles

Liczba artykułów: 61

---

<!-- source: playwright-e2e-testing-cheatsheet.md -->

## Table of Contents

1. General Playwright E2E testing best practices
2. Test structure and organization patterns
3. Locator strategies and best practices
4. Handling flaky tests and reliability
5. Performance optimization
6. Debugging techniques and tools
7. Writing E2E tests with Playwright and Claude Code
8. Modern testing approaches and anti-patterns

## 1\. General Playwright E2E testing best practices

### Core testing principles

**Test user-visible behavior**: Focus on what users actually see and interact with rather than implementation details. This approach ensures tests remain stable even when underlying code changes.

```
// ✅ Good - Testing user behavior
await page.getByRole('button', { name: 'Add to Cart' }).click();
await expect(page.getByText('Item added to cart')).toBeVisible();

// ❌ Bad - Testing implementation details
await page.locator('.btn-primary.add-cart-btn').click();
await expect(page.locator('#cart-count')).toHaveClass('updated');
```

Enter fullscreen modeExit fullscreen mode

### Test isolation principles

Each test should run independently with its own browser context, ensuring **complete isolation** of cookies, local storage, and session data. Playwright automatically provides this isolation, but proper test structure is essential:

```
test.beforeEach(async ({ page }) => {
  // Fresh context for each test
  await page.goto('https://example.com');
  // Each test starts from a known state
});

test('isolated test example', async ({ page }) => {
  // This test has no dependencies on other tests
  await page.getByRole('link', { name: 'Products' }).click();
  await expect(page).toHaveURL('/products');
});
```

Enter fullscreen modeExit fullscreen mode

### Focus on critical user journeys

Prioritize testing based on business impact rather than achieving 100% coverage. **Essential workflows** include:

- User registration and authentication
- Core business transactions (checkout, payment)
- Data entry and retrieval operations
- Account management features
- Error recovery paths

## 2\. Test structure and organization patterns

### Folder structure

```
├── tests/
│   ├── auth/                    # Authentication flows
│   │   ├── login.spec.ts
│   │   └── registration.spec.ts
│   ├── e2e/                     # End-to-end scenarios
│   │   ├── checkout.spec.ts
│   │   └── user-journey.spec.ts
│   └── api/                     # API integration tests
│       └── backend.spec.ts
├── page-objects/
│   ├── pages/
│   │   ├── login-page.ts
│   │   └── checkout-page.ts
│   └── components/
│       └── navigation.ts
├── fixtures/
│   ├── test-data.ts            # Test data management
│   ├── auth-setup.ts           # Authentication fixtures
│   └── page-objects.ts         # POM fixtures
├── utils/
│   ├── data-factory.ts         # Dynamic data generation
│   └── test-helpers.ts
└── playwright.config.ts
```

Enter fullscreen modeExit fullscreen mode

### Test organization

**Group by business domain**: Organize tests around user journeys rather than technical implementation:

```
test.describe('User Authentication Flow', () => {
  test('successful login with valid credentials', async ({ page }) => {
    // Clear business intent
  });

  test('password reset journey', async ({ page }) => {
    // Complete user workflow
  });
});
```

Enter fullscreen modeExit fullscreen mode

### Advanced fixture architecture

Create reusable fixtures for common test patterns:

```
// fixtures/page-objects.ts
import { test as base } from '@playwright/test';
import { LoginPage } from '../page-objects/login-page';
import { DashboardPage } from '../page-objects/dashboard-page';

type PageObjects = {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
};

export const test = base.extend<PageObjects>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
});

// Usage in tests
test('user workflow', async ({ loginPage, dashboardPage }) => {
  await loginPage.goto();
  await loginPage.login('user@example.com', 'password');
  await dashboardPage.expectWelcomeMessage();
});
```

Enter fullscreen modeExit fullscreen mode

## 3\. Locator strategies

### Locator priority hierarchy

**1\. Role-based locators (highest priority)**

```
await page.getByRole('button', { name: 'Submit Order' }).click();
await page.getByRole('textbox', { name: 'Email Address' }).fill('user@example.com');
await page.getByRole('heading', { name: 'Welcome Dashboard' });
```

Enter fullscreen modeExit fullscreen mode

**2\. Test ID locators (stable and recommended)**

```
await page.getByTestId('checkout-submit-button').click();
```

Enter fullscreen modeExit fullscreen mode

**3\. Text-based locators (user-visible content)**

```
await page.getByText('Welcome back, John!').click();
await page.getByLabel('Password').fill('secure-password');
await page.getByPlaceholder('Enter your email').fill('user@example.com');
```

Enter fullscreen modeExit fullscreen mode

**4\. CSS/XPath (try to avoid)**

```
// Only when other options aren't viable
await page.locator('.legacy-element').click();
```

Enter fullscreen modeExit fullscreen mode

### Advanced locator patterns

**Chaining and filtering for complex scenarios**:

```
const productCard = page.getByRole('listitem')
  .filter({ hasText: 'iPhone 15 Pro' })
  .getByRole('button', { name: 'Add to Cart' });

await productCard.click();
```

Enter fullscreen modeExit fullscreen mode

**Data-testId naming convention**:

```
// Pattern: {scope}-{component}-{element}-{type}
data-testid="header-navigation-login-button"
data-testid="checkout-form-email-input"
data-testid="product-card-price-display"
```

Enter fullscreen modeExit fullscreen mode

## 4\. Handling flaky tests and improving reliability

### Common causes and solutions

**Root causes of flaky tests**:

- Unstable selectors
- Fixed time delays (hard waits)
- Race conditions and timing issues
- External dependencies
- Test interdependencies

### Auto-waiting mechanisms

Playwright automatically performs **actionability checks** before interacting with elements:

- **Visible**: Non-empty bounding box
- **Stable**: Same position for 2+ animation frames
- **Enabled**: Not disabled
- **Receives Events**: Not obscured
- **Editable**: For input actions


```
// Playwright waits automatically
await page.getByRole('button', { name: 'Submit' }).click();
// No manual wait needed - Playwright ensures button is clickable
```

Enter fullscreen modeExit fullscreen mode

### Retry strategies

**Global configuration**:

```
export default defineConfig({
  retries: process.env.CI ? 2 : 0, // 2 retries in CI
});
```

Enter fullscreen modeExit fullscreen mode

**Test-level retries**:

```
test.describe.configure({ retries: 2 });

test('potentially flaky test', async ({ page }, testInfo) => {
  if (testInfo.retry) {
    // Clean up state on retry
    await cleanupTestData();
  }
  // Test logic
});
```

Enter fullscreen modeExit fullscreen mode

[**Custom retry logic**](https://playwright.dev/docs/test-assertions#expecttopass):

```
await expect(async () => {
  await page.locator('button').click();
  await expect(page.locator('div')).toBeVisible();
}).toPass();
```

Enter fullscreen modeExit fullscreen mode

### Network stubbing for reliability

```
// Mock external dependencies
await page.route('**/api/users', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([\
      { id: 1, name: 'John Doe' }\
    ])
  });
});

// Block unnecessary resources
await context.route(/\.(css|jpg|png)$/, route => route.abort());
```

Enter fullscreen modeExit fullscreen mode

## 5\. Performance optimization for E2E tests

### Parallel execution configuration

```
export default defineConfig({
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined, // Optimize for environment

  projects: [\
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },\
  ],
});
```

Enter fullscreen modeExit fullscreen mode

### Test sharding

```
# Split tests across multiple machines/processes
npx playwright test --shard=1/4
npx playwright test --shard=2/4
npx playwright test --shard=3/4
npx playwright test --shard=4/4
```

Enter fullscreen modeExit fullscreen mode

**GitHub actions sharding**:

```
strategy:
  matrix:
    shard: [1, 2, 3, 4]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: npx playwright test --shard=${{ matrix.shard }}/4
```

Enter fullscreen modeExit fullscreen mode

### Resource optimization

#### Network cache

Speed up Playwright tests by caching network requests on the filesystem. Try [playwright-network-cache](https://github.com/vitalets/playwright-network-cache)

#### Disable heavy resources when not needed

```
export default defineConfig({
  use: {
    trace: 'retain-on-failure',  // Only on failures
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',

    // Performance settings
    navigationTimeout: 30000,
    actionTimeout: 10000,
  }
});
```

Enter fullscreen modeExit fullscreen mode

### Browser context management

**Optimized Context Configuration**:

```
const context = await browser.newContext({
  ignoreHTTPSErrors: true,
  serviceWorkers: 'block',  // Prevent interference
  reducedMotion: 'reduce',  // Faster animations

  // Performance optimizations
  viewport: { width: 1280, height: 720 },
  deviceScaleFactor: 1,
});
```

Enter fullscreen modeExit fullscreen mode

## 6\. Debugging techniques and tools

### Playwright inspector

Launch the interactive debugger:

```
# Debug all tests
npx playwright test --debug

# Debug specific test
npx playwright test example.spec.ts:10 --debug
```

Enter fullscreen modeExit fullscreen mode

**Inspector features**:

- Step through test execution
- Live edit locators
- Pick locators from page
- View actionability logs
- Record new test actions

### UI Mode (Interactive Testing)

```
npx playwright test --ui
```

Enter fullscreen modeExit fullscreen mode

**UI Mode Capabilities**:

- **Watch Mode**: Auto-rerun on file changes
- **Time Travel**: Step through execution timeline
- **DOM Snapshots**: Inspect page state at each step
- **Network Tab**: View all requests
- **Console Logs**: Real-time output

### Trace viewer

**Configuration**:

```
export default defineConfig({
  use: {
    trace: 'on-first-retry', // Capture on failures
  },
});
```

Enter fullscreen modeExit fullscreen mode

**Manual trace recording**:

```
await context.tracing.start({
  screenshots: true,
  snapshots: true,
  sources: true
});

// Test actions...

await context.tracing.stop({
  path: 'trace.zip'
});
```

Enter fullscreen modeExit fullscreen mode

**View traces**:

```
npx playwright show-trace trace.zip
```

Enter fullscreen modeExit fullscreen mode

### VS Code integration

**Setup and features**:

1. Install "Playwright Test for VSCode" extension
2. Features include:

   - **Live Debugging**: Click locators to highlight in browser
   - **Breakpoint Debugging**: Step through tests
   - **Pick Locator Tool**: Generate resilient locators
   - **Test Explorer**: Run/debug individual tests

## 7\. Writing E2E tests with Playwright and Claude Code

### Understanding MCP (Model Context Protocol)

The **Model Context Protocol** enables Claude Code and other AI assistants to interact with Playwright-managed browsers through structured data exchange.

### Playwright MCP usage for writing E2E tests

**Key Components**:

- [**MCP Server**](https://github.com/microsoft/playwright-mcp): Exposes Playwright capabilities to AI tools
- [**Playwright Chrome Extension**](https://github.com/microsoft/playwright-mcp/blob/main/extension/README.md): Allows you to connect to pages in your existing browser and leverage the state of your default user profile
- **Accessibility Tree**: Primary data source for element interaction
- **Page Context**: Provides markup, screenshots, and element hierarchy
- **Agentic Tools**: AI assistants that consume and act on page context

### Requirements for Playwright/MCP Server

**Setup Configuration**:

```
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": [\
        "@playwright/mcp@latest"\
      ]
    }
  }
}
```

Enter fullscreen modeExit fullscreen mode

[**Essential Tools Provided by MCP**](https://github.com/microsoft/playwright-mcp#tools):

```
// Core automation tools available to Claude Code
- browser_snapshot     // Capture accessibility tree
- browser_navigate     // URL navigation
- browser_click        // Element interaction
- browser_type         // Text input
- browser_screenshot   // Visual capture
- browser_evaluate     // JavaScript execution
```

Enter fullscreen modeExit fullscreen mode

### Page object models for AI consumption

**AI-optimized POM structure**:

```
class LoginPage {
  metadata = {
      purpose: "Handle user authentication",
      businessLogic: "Standard login with username/password",
      testScenarios: ["valid login", "invalid credentials", "password reset"]
  }

  constructor(page) {
    this.page = page;
  }

  // Action methods that AI tools can call directly
  async navigateToLogin() {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
  }

  async enterUsername(username) {
    await this.page.fill(this.selectors.usernameInput, username);
  }

  async enterPassword(password) {
    await this.page.fill(this.selectors.passwordInput, password);
  }

  async clickLoginButton() {
    await this.page.click(this.selectors.loginButton);
  }

  async submitLoginForm(username, password) {
    await this.enterUsername(username);
    await this.enterPassword(password);
    await this.clickLoginButton();
  }

  async verifyLoginSuccess() {
    await this.page.waitForURL('/dashboard');
    return await this.page.isVisible(this.selectors.dashboardHeader);
  }

  async getErrorMessage() {
    return await this.page.textContent(this.selectors.errorMessage);
  }

  // Single reliable selector per element with descriptions
  get selectors() {
    return {
      usernameInput: '[data-testid="login-username"]', // Email or username input field
      passwordInput: '[data-testid="login-password"]', // Password input field
      loginButton: '[data-testid="login-submit"]', // Form submission button
      errorMessage: '[data-testid="login-error"]', // Error message container
      dashboardHeader: '[data-testid="dashboard-header"]', // Success indicator
      forgotPasswordLink: '[data-testid="forgot-password-link"]' // Password reset link
    };
  }
}
```

Enter fullscreen modeExit fullscreen mode

### Best practices for Claude Code integration

**Key Principles**:

1. **Accessibility-first approach**: Leverage accessibility tree over screenshots
2. **Self-documenting code**: Include rich metadata and descriptions
3. **Natural language interfaces**: Map methods to human-readable scenarios
4. **Error recovery patterns**: Built-in retry and fallback mechanisms
5. **Clear action methods**: Expose granular, callable methods for AI orchestration

## 8\. Testing approaches and anti-patterns to avoid

### Common anti-patterns

**1\. Testing implementation details**

```
// ❌ Wrong - Testing CSS classes
await page.locator('.btn-primary.ng-valid').click();

// ✅ Correct - Testing user-visible behavior
await page.getByRole('button', { name: 'Submit' }).click();
```

Enter fullscreen modeExit fullscreen mode

**2\. Test interdependencies**

```
// ❌ Wrong - Tests depending on each other
test('login first', async ({ page }) => {
  // Sets global state
});

test('depends on login', async ({ page }) => {
  // Assumes previous test ran
});

// ✅ Correct - Isolated tests
test.beforeEach(async ({ page }) => {
  // Fresh setup for each test
});
```

Enter fullscreen modeExit fullscreen mode

**3\. Hard Waits**

```
// ❌ Wrong - Fixed timeouts
await page.waitForTimeout(5000);

// ✅ Correct - Web-first assertions
await expect(page.getByText('Success')).toBeVisible();
```

Enter fullscreen modeExit fullscreen mode

**4\. Testing third-party services**

```
// ❌ Wrong - Testing external payment gateway
await page.click('.stripe-button');

// ✅ Correct - Mock external dependencies
await page.route('**/api/payment', route => route.fulfill({
  status: 200,
  body: JSON.stringify({ success: true })
}));
```

Enter fullscreen modeExit fullscreen mode

### Authentication patterns

**Global authentication setup**:

```
// tests/auth.setup.ts
import { test as setup } from '@playwright/test';

const authFile = '.auth/user.json';

setup('authenticate', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('user@example.com');
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', { name: 'Sign In' }).click();

  await page.context().storageState({ path: authFile });
});

// Use in playwright.config.ts
export default defineConfig({
  projects: [\
    {\
      name: 'setup',\
      testMatch: /.*\.setup\.ts/,\
    },\
    {\
      name: 'authenticated',\
      use: { storageState: '.auth/user.json' },\
      dependencies: ['setup'],\
    },\
  ],
});
```

Enter fullscreen modeExit fullscreen mode

Remember that successful E2E testing is not about testing everything, but about testing the right things effectively. Focus on user value, maintain test isolation, use stable locators, and continuously optimize your test suite based on real-world feedback and metrics.

---

<!-- source: the-green-report-tracking-ui-to-api-connections-with-playwright.md -->

Or press ESC to close.

### Tracking UI to API Connections with Playwright

Aug 3rd 20255 min read

![](https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/images/banner.webp)

easy

javascriptES6

playwright1.54.2

ui

api

When testing web applications, it's common to validate that buttons or interactive elements respond
to user actions. But what if there's no visible confirmation on the UI? For example, in a star
rating system, clicking a star might silently trigger a background API call without any immediate
feedback. Without a clear cue, it becomes difficult to confirm whether the correct data was actually
sent. In this post, we'll use a simple Express-based web app as a demonstration and show how
Playwright's waitForRequest can be used to capture and verify
the API request. This approach helps
ensure that important background actions are correctly triggered, even when the UI doesn't provide
direct confirmation.


#### The Scenario: Rating Widget

A common UI pattern in modern web applications is the 5-star rating component. Users can click on
one of the stars to rate content, products, or services. While the interaction feels simple, what
happens behind the scenes is much more important. Clicking a star usually triggers a POST request to
an API with the selected rating value.


![Rating Widget Demo](https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/images/demo_app.gif)

Rating Widget Demo

From a testing perspective, the challenge is that this interaction often does not produce any
visible confirmation on the screen. There may be no success message, no animation, and no clear
indication that the action worked. This makes it difficult to confirm that the right data was sent
without inspecting the network activity.


Here is an example of what the API request might look like when the user clicks on the fourth star:


```bash

POST /rating
Content-Type: application/json

{
  "rating": 4
}

```

The same behavior applies to the other stars. Clicking the second star would send { "rating": 2 },
the fifth would send { "rating": 5 }, and so on. Because the
value changes based on user input, it
is important to capture and verify the outgoing request to make sure the application is behaving
correctly.


#### Using waitForRequest in Playwright

Playwright provides a powerful method called waitForRequest that
allows us to wait for a specific
network request to occur during a test. This is especially useful when testing features that involve
API calls triggered by user actions, such as clicking a star in a rating widget. Instead of blindly
assuming the request was sent, we can intercept it and inspect its details directly.


The waitForRequest method works by listening for requests that
match a specific condition. We can
filter by URL, HTTP method, or any other property of the request. When the condition is met,
Playwright captures the request and allows us to access its contents, including the payload.


Here is a simple example of how to use waitForRequest in a test.
This test clicks on the second star
and checks that a POST request to /rating is sent with the
correct data:


```javascript

test('sends correct rating payload when a star is clicked', async ({ page }) => {
  await page.goto('http://localhost:3000');

  const [request] = await Promise.all([
    page.waitForRequest(request =>
      request.url().includes('/rating') && request.method() === 'POST'
    ),
    page.click('[data-rating="2"]')
  ]);

  const payload = JSON.parse(request.postData());
  expect(payload).toEqual({ rating: 2 });
});

```

In this example, the test waits for a POST request to the /rating endpoint while simulating a click
on the star with a data-rating attribute set to 2. Once the
request is captured, the test parses the
payload and verifies that the correct rating value was sent.


This approach gives us confidence that our front-end logic is correctly communicating with the
backend and that dynamic user input is handled as expected.


#### Benefits of This Approach

Using waitForRequest in our Playwright tests provides several
valuable benefits, especially when
testing features that rely on background API calls:


- **Confirms the right API is called:** By intercepting and inspecting network requests, we
can ensure that the correct endpoint is triggered in response to specific user actions. This is
useful when multiple components use similar requests, or when the UI does not clearly show the
outcome of an action.

- **Validates dynamic payloads:** Many UI elements generate request payloads based on user
input. With waitForRequest, we can directly access and
validate the contents of each request.
This helps confirm that dynamic values, such as a selected rating, are sent correctly to the
backend.

- **Strengthens end-to-end test coverage:** This method allows us to go beyond checking that a
button was clicked or an element changed. We can now verify that the entire flow—from UI
interaction to network request—is working as expected. This leads to more reliable and complete
test coverage.


#### Conclusion

Verifying that a user interaction triggers the correct backend request is an important part of
end-to-end testing, especially when there is no visible feedback in the UI. Playwright's
waitForRequest makes it easy to capture and inspect network
activity, helping us confirm that our
application behaves as expected. Whether we are testing a simple rating component or a more complex
feature, this method adds an extra layer of confidence to our test suite.


The demo app and the Playwright test shown in this post are available on our [GitHub page](https://github.com/Crypted39/the-green-report-examples/tree/master/tracking-ui-to-api-connections-with-playwright) for you to
explore and try out.

---

<!-- source: global-cache-make-playwright-beforeall-run-once-for-all-workers.md -->

## Intro

Let’s start with a quick quiz:
How many times will the `BeforeAll` hook run in the following Playwright code?

```ts
import { test, expect } from '@playwright/test';

test.beforeAll(() => {
  console.log('Executing beforeAll...');
});

test('test 1', () => {
  expect(true).toEqual(true);
});

test('test 2', () => {
  expect(true).toEqual(false);
});

test('test 3', () => {
  expect(true).toEqual(false);
});
```

{% details Check the answer! %}

At first glance, it seems like it should run once before all tests as the name suggests.
But actually, **it will be called 2, 3 or even 4+ times**, depending on your Playwright configuration.

* In case of 1 worker, the hook will be called **2 times**, because when `test 2` fails, Playwright will create a new worker and re-run BeforeAll hook for `test 3`:

```
npx playwright test --workers=1
```

![1 worker](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/vpp42o0dk3a9zi9sa8sb.png)

* In case of 3 workers and `fullyParallel` mode, the hook will be called **3 times**, because each test runs in its own worker:

```
npx playwright test --workers=3 --fully-parallel
```

![3 workers, fully parallel](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/uohlo0co7f30n0ereuaa.png)

There is no way to ensure this `BeforeAll` hook executes only once across the entire test run, as [many](https://github.com/microsoft/playwright/issues/28201) [people](https://github.com/microsoft/playwright/issues/22520) [expect](https://github.com/microsoft/playwright/issues/28814). Despite its name, `BeforeAll` is called **once per worker**, not once per test run.

> Can you make it execute 4+ times?

{% enddetails %}

## The "Test Setup" Parallelization Problem

Setting up data before the actual tests is a very common task. You may need to authenticate a user, seed a database, or prepare an environment. Things get trickier when tests run in parallel, because some setup code should run only once, even if there are multiple workers.

Playwright provides several approaches, each with its own advantages and drawbacks. Let’s go through them first, and then I'll share a solution that takes the best parts of each.

### Example

Imagine a site that has public and authenticated pages. You are asked to write E2E tests for it, and you create two spec files:

```ts
// auth.spec.ts

test('test authenticated page', () => { ... });
```

```ts
// no-auth.spec.ts

test('test non-authenticated page', () => { ... });
```

There are 3 options to set up authentication:

1. Project dependency
2. Global setup
3. BeforeAll hook

### 1. Project dependency

Playwright docs [recommend](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests) using a separate `setup` project for authentication. Instead of tests, this project contains authentication code and is set as a dependency for the main project:

```ts
// playwright.config.ts

export default defineConfig({
  projects: [
    // Setup project
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: {
        // Use prepared auth state.
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ]
```

✅ Advantages of the approach:

* Authentication runs only once
* Authentication steps are visible in the report

❌ Downsides:

* Requires an extra project
* Authentication always runs, even for tests that do not need it

To illustrate the last point, I'll add a reset auth code to `no-auth.spec.ts` as recommended in the docs:

```ts
// no-auth.spec.ts

// Reset storage state for this file to avoid being authenticated
test.use({ storageState: { cookies: [], origins: [] } });

test('test non-authenticated page', () => { ... });
```

And run only this file:

```
npx playwright test no-auth.spec.ts
```

You can see that the `setup` project runs the authentication flow, which is unnecessary:

![Unnecessary auth](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/l6y93gbokhp22x8q5gh0.png)

This is definitely a place for optimization - here it costs ~2 seconds just to do the auth and then throw it away.

### 2. Global setup

Playwright supports [global setup/teardown](https://playwright.dev/docs/test-global-setup-teardown) scripts as an alternative to dependency projects. But this approach is not recommended, because it lacks many features of the Playwright runner. The only reason to use global scripts is if you don't want to introduce an extra project in the Playwright config.

✅ Advantages:

* No extra project
* Authentication runs only once

❌ Downsides:

* Lacks features of the Playwright runner (fixtures, tracing, etc.)
* Authentication steps are not visible in the report
* Authentication always runs, even if not needed

### 3. BeforeAll

The 3rd option is to leverage the `BeforeAll` hook to perform authentication specifically for the `auth.spec.ts`. It runs conditionally only when at least one test of this suite is executed:

```ts
// auth.spec.ts

test.beforeAll(() => {
  console.log('Authenticating...');
});

test('test authenticated page', () => { ... });
```

This is a more optimized approach compared to project dependencies. When I run `auth.spec.ts`, the hook executes:

```
npx playwright test "/auth.spec.ts"
```

![run authentication test](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/ichkh33d0q647gnfjcro.png)

When I run `no-auth.spec.ts`, the hook does not execute:

```
npx playwright test "/no-auth.spec.ts"
```

![run non-authentication test](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/nyph0385nsud23ds0dn6.png)

But here we face all the issues mentioned in the introduction quiz. Every time a test fails, a new worker will be started, triggering the `BeforeAll` hook:

![Auth in every worker](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/90dgrq5gnm0f6t37gjct.png)

The same re-runs will occur during parallelization and sharding.

✅ Advantages:

* No extra project, code stays close to the test
* Steps are visible in the report
* Runs on-demand only when needed

❌ Downsides:

* Runs once per worker - so it repeats in parallel mode or after failures

## A "Global Cache" Solution

I like the `BeforeAll` approach because it keeps the Playwright config simple and looks natural: run some code before all tests. But the problem is repeated execution per worker.

A year ago, I made my first [attempt](https://github.com/microsoft/playwright/issues/22520#issuecomment-2391025061) to solve this problem. Since then, it has grown into *Global Cache* - a helper module that ensures code runs only once across all workers.

The idea is simple:

> The first worker that requests a value becomes responsible for computing it. Others wait until the result is ready — and all workers get the same value. The value is cached and reused by later workers.

Here's a diagram of how it works:

![Global cache schema](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/hvhvsangl7o5256l2lus.png)

Under the hood, Global Cache spins up a tiny HTTP server, with a simple REST API for getting and setting values. This server is a single storage point for all workers. When a worker needs a value, it performs a `GET` request to the server, and either gets a cached value instantly or computes the value and sets it via the `POST` request.

Once integrated into Playwright test, Global Cache resolves the downside of the `BeforeAll` hook, ensuring the code runs exactly once. The API is wrapped into a `globalCache` interface with convenient methods:

```ts
import { globalCache } from '@global-cache/playwright';

test.beforeAll(async () => {
  const value = await globalCache.get('key', async () => {
    /* ...heavy calculation, runs once */
    return value;
  });
});
```

Actual code for the authentication in the `BeforeAll` hook:

```js
// auth.spec.ts
import { test } from '@playwright/test';
import { globalCache } from '@global-cache/playwright';

let storageState;

test.beforeAll(async ({ browser }) => {
  storageState = await globalCache.get('storage-state', async () => {
    console.log('Authentication...');
    const page = await browser.newPage();
    // authentication steps...

    return page.context().storageState();
  });
});

// Set storageState fixture
test.use({ storageState: async ({}, use) => use(storageState) });

test('test authenticated page 1', () => { ... });
test('test authenticated page 2', () => { ... });
test('test authenticated page 3', () => { ... });
```

If any test fails or runs in parallel, the authentication will still run only once:

![authentication will run only once](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/kr0wj00cmqoetzvixt1w.png)

### You might not need BeforeAll

The most interesting insight is that the `BeforeAll` hook becomes redundant! As the code is guaranteed to run once, no matter where to put it. I can move the authentication steps directly into the `storageState` fixture (that runs before each test!). This makes the code simpler:

```ts
// auth.spec.ts
import { test } from '@playwright/test';
import { globalCache } from '@global-cache/playwright';

test.use({ 
  storageState: async ({ browser }, use) => {
    const storageState = await globalCache.get('storage-state', async () => {
      const page = await browser.newPage();
      // authentication steps...
      return page.context().storageState();
    });
    await use(storageState);
  }
});

test('test authenticated page', () => { ... });
```

> Note that for creating a page for authentication, I use the `browser.newPage()` call. If I try to use the built-in `page` fixture, I get a circular loop error because the `page` fixture depends on `storageState`.

### Multiple files with conditional auth

In case of multiple authentication test files, you can move the auth logic into `test.extend()` to apply by default. Particular scenarios can be excluded by tags:

```ts
// fixtures.ts
import { test as baseTest } from '@playwright/test';
import { globalCache } from '@global-cache/playwright';

export const test = baseTest.extend({
  storageState: async ({ storageState, browser }, use, testInfo) => {
    // Skip authentication for '@no-auth'-tagged tests
    if (testInfo.tags.includes('@no-auth')) return use(storageState);

    storageState = await globalCache.get('auth-state', async () => {
      // authentication steps...
    });

    await use(storageState);
  },
});
```

Now I can use this `test` instance to run authenticated tests:

```ts
// auth.spec.ts
import { test } from './fixtures';

test('test authenticated page', () => { ... });
```

For non-authenticated tests I just add `@no-auth` tag:

```ts
// no-auth.spec.ts
import { test } from './fixtures';

test('test non-authenticated page', { tag: '@no-auth' }, () => { ... });
```

## Recap

Global Cache keeps the simplicity of the `BeforeAll` hook but removes its biggest drawback: repeated execution in every worker. It lets you run any setup code exactly once across all workers, even in parallel mode or with sharding. It works not only for authentication, but also for database seeding, expensive API calls, or any other shared setup. You can find all these examples in the [project repo](https://github.com/vitalets/global-cache).

Feel free to try it out and share any feedback. 

Thanks for reading ❤️

---

<!-- source: advanced-playwright-fixtures-supercharge-your-test-setup-and-teardown.md -->

Title: 🛠️ Advanced Playwright Fixtures: Supercharge Your Test Setup and Teardown

URL Source: https://medium.com/@qa.gary.parker/%EF%B8%8F-advanced-playwright-fixtures-supercharge-your-test-setup-and-teardown-20b8bb68e68e

Published Time: 2025-05-18T17:26:55Z

Markdown Content:
## Discover how to use advanced Playwright fixtures (test.extend()) to create modular, reusable setup/teardown units, significantly improving your test automation’s efficiency, readability, and maintainability.

[![Image 1: Gary Parker](https://miro.medium.com/v2/resize:fill:32:32/1*HwIoU-ctFBz3c8CSa6OXZQ.jpeg)](https://medium.com/@qa.gary.parker?source=post_page---byline--20b8bb68e68e---------------------------------------)

15 min read

May 18, 2025

As your Playwright test suite expands, managing setup and teardown logic efficiently becomes crucial. While `test.beforeEach` and `test.afterEach` are useful for simple scenarios, they can quickly lead to repetitive code or overly complex conditional logic when dealing with varied test requirements, shared resources, or unique states for different test groups.

Playwright fixtures, defined using `test.extend()`, offer a powerful and elegant solution. They allow you to create modular, reusable, and on-demand setup and teardown units that can significantly clean up your test code and make your automation suite more robust and maintainable.

This post will dive deep into advanced fixture patterns and use cases, showing you how to leverage them to supercharge your Playwright testing workflow.

## 1. Understanding Playwright Fixtures

At its core, a Playwright fixture is a piece of code that runs before (setup) and potentially after (teardown) your test, providing some value or state that your test can use. You define fixtures by extending the base `test` object, typically in a central fixture file (e.g., `my-fixtures.ts`).

**Key Concepts:**

*   `test.extend()`**:** The function used to create new fixtures or override existing ones.
*   **Fixture Function:** An `async` function that takes two arguments:

1.   An object with already defined fixtures (like `page`, `context`, or your own custom fixtures that this new fixture might depend on).
2.   A `use` function. The fixture setup code runs before `await use(fixtureValue)`, and the teardown code runs after it.

*   **Fixture Value:** The value that `await use(fixtureValue)` passes to the test or to other fixtures that depend on it.

**Basic Example: A Simple Data Fixture**

Let’s start building our central fixture file, `my-fixtures.ts`:

import { test as baseTest, expect, Page, APIRequestContext, request } from '@playwright/test';

type MyBaseFixtures = {

 testUser: { username: string; email: string };

};

export const test = baseTest.extend<MyBaseFixtures>({

 testUser: async ({}, use) => {

 

 console.log('Setting up testUser fixture');

 const userData = { username: 'testUser123', email: 'user@example.com' };

 await use(userData);

 

 console.log('Tearing down testUser fixture (if needed)');

 },

})

export { expect };
**How to use this in a test file (e.g.,**`user-profile.spec.ts`**):**

import { test, expect } from './my-fixtures'; 
test('should display username on profile page', async ({ page, testUser }) => {

 console.log(`Test User: ${testUser.username}, Email: ${testUser.email}`);

 await page.goto(`/profile/${testUser.username}`);

 

});

**Benefits of Fixtures:**

*   **Reusability:** Define setup/teardown once, use in many tests.
*   **Readability:** Test code becomes cleaner, focusing on logic rather than setup details.
*   **On-Demand Execution:** Fixtures are only run if a test requests them (or if they are `auto: true`).
*   **Composability:** Fixtures can depend on other fixtures.
*   **Encapsulation:** Setup and teardown logic are kept together, making it easier to manage.

## 2. Worker Scope vs. Test Scope Fixtures

Fixtures can have different scopes, controlling how often their setup and teardown logic runs:

*   `scope: 'test'`**(Default):** The fixture is set up before each test that uses it and torn down after that test finishes. This is suitable for state that should be isolated per test (e.g., a specific page instance, a temporary user account for that test like our `testUser` above).
*   `scope: 'worker'`**:** The fixture is set up once per worker process before any tests in that worker run, and torn down after all tests in that worker have completed. This is ideal for expensive resources that can be shared across multiple tests running in the same worker process (e.g., a database connection, a global API client, logging into a shared service once).

**Example: Adding a Worker-Scoped API Client to**`my-fixtures.ts`

We’ll extend the `test` object we previously defined in `my-fixtures.ts`:

type WorkerSharedFixtures = {

 apiClient: APIRequestContext;

};

export const test = test.extend<WorkerSharedFixtures>({

 apiClient: [

 async ({}, use) => {

 

 console.log('Setting up worker-scoped apiClient');

 const workerApiToken = process.env.WORKER_API_TOKEN || 'default-worker-token';

 const client = await request.newContext({

 baseURL: 'https://api.yourapp.com',

 extraHTTPHeaders: {

 'Authorization': `Bearer ${workerApiToken}`,

 },

 });

 await use(client);

console.log('Disposing worker-scoped apiClient');

 await client.dispose();

 },

 { scope: 'worker' }, 

 ],

});

**How to use this in a test file (e.g.,**`api-tests.spec.ts`**):**

import { test, expect } from './my-fixtures';
test('should fetch user data using worker-scoped apiClient', async ({ apiClient, testUser }) => {

 

 const response = await apiClient.get(`/users/${testUser.username}`);

 expect(response.ok()).toBeTruthy();

 const userData = await response.json();

 expect(userData.email).toBe(testUser.email);

});

test('another API test using the same apiClient instance', async ({ apiClient }) => {

 

 const response = await apiClient.get('/items/all');

 expect(response.ok()).toBeTruthy();

});

**Note the array syntax for specifying options like scope:**`[async ({}, use) => {...}, { scope: 'worker' }]`**.**

## 3. Auto-Use Fixtures

Sometimes, you have a fixture that needs to run for every single test that uses your custom `test` object, without explicitly declaring it in each test function. This is where `auto: true` comes in handy.

*   **How to define:** Add `{ auto: true }` to the fixture options.
*   **Use cases:**
*   Global logging setup.
*   Starting a mock server or a required background service (if not handled by `webServer` in `playwright.config.ts`).
*   Ensuring a user is always logged out before each test (unless a specific login fixture is used).
*   Setting up global event listeners or mocks (`page.route()`). This can replace global `beforeEach` hooks for such setups.

**Example: Adding an Auto-Use Logger to**`my-fixtures.ts`

Continuing to extend our `test` object in `my-fixtures.ts`:

type AutoUseFixtures = {

 logger: void; 

};

export const test = test.extend<AutoUseFixtures>({

 logger: [

 async ({}, use, testInfo) => {

 console.log(`[LOGGER] Starting test: ${testInfo.title}`);

 await use(); 

 console.log(`[LOGGER] Finished test: ${testInfo.title} with status: ${testInfo.status}`);

 },

 { auto: true }, 

 ],

});

**How it behaves (no explicit use needed in test files):**

import { test, expect } from './my-fixtures';
test('some functionality test', async ({ page, testUser }) => {

 await page.goto('/');

 console.log(`Executing test logic for ${testUser.username}`);

 

});

Auto-use fixtures with `scope: 'worker'` can act as global `beforeAll`/`afterAll` hooks for all tests within a worker (across all files using that `test` object).

## 4. Dependent Fixtures (Chaining Fixtures)

One of the most powerful features is that fixtures can depend on other fixtures. Playwright resolves this dependency tree automatically. The dependent fixture receives the values of the fixtures it depends on.

## Get Gary Parker’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

**Example: Adding an**`adminLoggedInPage`**to**`my-fixtures.ts` This fixture will depend on the built-in `page` fixture and a new `adminUser` data fixture.

type AdminFixtures = {

 adminUser: { username: string; passwordForLogin: string; role: 'admin' };

 adminLoggedInPage: Page; 

};

export const test = test.extend<AdminFixtures>({

 adminUser: async ({}, use) => {

 

 console.log('Setting up adminUser fixture');

 await use({ username: 'superAdmin', passwordForLogin: 'SecurePassword123!', role: 'admin' });

 },

adminLoggedInPage: async ({ page, adminUser, expect }, use) => { 

 

 

 console.log(`Logging in as admin: ${adminUser.username}`);

 await page.goto('/login');

 await page.locator('#username').fill(adminUser.username);

 await page.locator('#password').fill(adminUser.passwordForLogin); 

 await page.locator('button[type="submit"]').click();
await expect(page).toHaveURL('/admin/dashboard', { timeout: 5000 }); 

 console.log('Admin login successful, dashboard loaded');

await use(page);

console.log(`Logging out admin: ${adminUser.username}`);

 

 

 },

})

**How to use this in a test file (e.g.,**`admin-panel.spec.ts`**):**

import { test, expect } from './my-fixtures';
test.describe('Admin Panel Access', () => {

test('admin should be able to access user management', async ({ adminLoggedInPage }) => {

 await adminLoggedInPage.goto('/admin/users');

 await expect(adminLoggedInPage.locator('h1')).toHaveText('User Management');

 

 });

test('admin should see their username on the dashboard', async ({ adminLoggedInPage, adminUser }) => {

 

 

 await expect(adminLoggedInPage.locator('#admin-username-display')).toHaveText(adminUser.username);

 });

});

This creates a clean, declarative way to build up complex test contexts.

## 5. Parameterizing Fixtures

While Playwright doesn’t have direct fixture parameterization in the same way some other test frameworks do (e.g., pytest’s `params` on fixtures themselves), you can achieve similar effects for varying test setups:

*   **Multiple Fixtures:** Create different, specifically named fixtures for different states (e.g., `editorUserPage`, `viewerUserPage`). Tests then pick the fixture that provides the desired state.
*   **Environment Variables:** Fixtures can read `process.env` variables to alter their behavior or the data they provide. This is often used in conjunction with Playwright Projects.
*   **Fixture Overrides per Project:** In `playwright.config.ts`, you can define projects that override specific fixtures. This is Playwright's idiomatic way to provide different fixture values or implementations for different test runs (e.g., different `baseURL` for a `page` fixture, or a different `user` object for a `loggedInUser` fixture across staging vs. production-like environments).

## 6. Advanced Fixture Use Cases & Patterns

Let’s explore some common scenarios where advanced fixtures shine:

## Managing Test Data

*   **Fixture to load data from JSON/CSV (example for**`my-fixtures.ts`**):**
*   To load test data from a JSON file, you would first create your data file:

[

 { "id": 1, "name": "Laptop", "price": 1200 },

 { "id": 2, "name": "Mouse", "price": 25 }

]
*   Then, add the fixture to your `my-fixtures.ts` file. Remember to import `fs` and `path`:

type Product = { id: number; name: string; price: number };

type ProductFixtures = { productList: Product[] };

export const test = test.extend<ProductFixtures>({

 productList: [async ({}, use) => {

 

 

 const filePath = path.join(__dirname, '../test-data/productsData.json'); 

 console.log(`[Fixture] Loading product data from: ${filePath}`);

 const products: Product[] = JSON.parse(fs.readFileSync(filePath, 'utf-8'));

 await use(products);

 }, { scope: 'worker' }], 

});

*   **Usage in a test file (e.g.,**`product-listing.spec.ts`**):**

*   **Fixture to create unique data via API (example for**`my-fixtures.ts`**):**
*   This fixture creates an article via an API and ensures it’s cleaned up afterwards.

type DynamicArticle = { id: string; title: string; content: string };

type DynamicArticleFixture = { uniqueTestArticle: DynamicArticle };

export const test = test.extend<DynamicArticleFixture>({

 uniqueTestArticle: async ({ apiClient, expect }, use) => {

 const articleTitle = `My Test Article ${Date.now()}`;

 const articleContent = 'This is dynamically created content.';

 console.log(`[Fixture] Creating article via API: ${articleTitle}`);

const response = await apiClient.post('/api/articles', { 

 data: { title: articleTitle, content: articleContent }

 });

 expect(response.ok()).toBeTruthy(); 

 const article: DynamicArticle = await response.json();

 expect(article.id).toBeDefined();

await use(article);

console.log(`[Fixture] Cleaning up article ${article.id} via API`);

 const deleteResponse = await apiClient.delete(`/api/articles/${article.id}`);

 expect(deleteResponse.ok()).toBeTruthy(); 

 },

 

});

*   **Usage in a test file (e.g.,**`article-view.spec.ts`**):**

## Service Abstractions & Mocks

*   **API Client Fixture:** (Shown in worker-scope example in Section 2)
*   **Mocking API Responses with**`page.route()`**(example for**`my-fixtures.ts`**):**
*   This fixture automatically mocks an API endpoint for user details.

type MockUserApiFixture = { mockStandardUserApi: void }; 
export const test = test.extend<MockUserApiFixture>({

 mockStandardUserApi: [async ({ page }, use) => {

 const apiUrl = '**/api/users/me'; 

 console.log(`[Fixture] Setting up API mock for ${apiUrl}`);

await page.route(apiUrl, route => {

 console.log(`[Fixture] Intercepted ${apiUrl}, fulfilling with mock data.`);

 route.fulfill({ 

 status: 200,

 contentType: 'application/json',

 json: { id: 'mockUser123', username: 'MockStandardUser', role: 'viewer' } 

 });

 });

await use();

console.log(`[Fixture] Tearing down API mock for ${apiUrl}`);

 await page.unroute(apiUrl); 

 }, { auto: true }], 

});

*   **Usage in a test file (e.g.,**`profile-mocked.spec.ts`**):** The mock is auto-applied, so no explicit destructuring is needed for `mockStandardUserApi`.

## Browser Context Manipulation

*   **Fixture for a page with specific permissions/storage (example for**`my-fixtures.ts`**):**
*   This fixture provides a `Page` object from a browser context with custom geolocation settings.

type GeoPageFixture = { pageWithGeoLocation: Page };

export const test = test.extend<GeoPageFixture>({

 pageWithGeoLocation: async ({ browser }, use) => {

 console.log('[Fixture] Creating new browser context with geolocation permission set to Berlin');

 const context = await browser.newContext({

 geolocation: { latitude: 52.52, longitude: 13.39 }, 

 permissions: ['geolocation'] 

 });

 const geoPage = await context.newPage();

await use(geoPage);

console.log('[Fixture] Closing browser context with geolocation permission');

 await context.close(); 

 },

 

 

 

});

*   **Usage in a test file (e.g.,**`map-view.spec.ts`**):**

## Complex Setup and Teardown Logic

Fixtures ensure teardown runs even if setup or the test itself fails (unless the worker process crashes). This makes them very robust for managing external resources or state that needs cleanup.

## 7. Organizing and Reporting Fixtures

As your fixture library grows, managing and understanding their usage in reports becomes important.

## Combining Fixtures from Multiple Files

For larger projects, you might define fixtures in different files based on their domain or purpose. Playwright allows you to merge these using `mergeTests`.

**Example File Structure:**

my-project/

├── fixtures/

│ ├── base-fixtures.ts 

│ ├── user-auth-fixtures.ts 

│ └── product-fixtures.ts 

│ └── index.ts 

└── tests/

 └── my-feature.spec.ts
`fixtures/index.ts`**(Merges and exports):**

import { mergeTests } from '@playwright/test';

import { test as baseFixtures } from './base-fixtures'; 

import { test as userAuthFixtures } from './user-auth-fixtures';

import { test as productFixtures } from './product-fixtures';

export const test = mergeTests(baseFixtures, userAuthFixtures, productFixtures);

export { expect } from '@playwright/test';
**Usage in**`tests/my-feature.spec.ts`**:**

import { test, expect } from '../fixtures'; 

## Controlling Fixture Visibility in Reports (`box: true`)

Some fixtures are purely for internal setup and don’t add much value to the test report steps. You can hide them from the report using the `box: true` option. This is useful for utility or automatic fixtures that clutter the report.

## Customizing Fixture Titles in Reports (`title: '...'`)

To make reports more readable, you can provide a custom title for your fixtures that will be displayed instead of the fixture name in the HTML report.

## 8. Tips for Effective Fixture Design

*   **Single Responsibility:** Each fixture should ideally do one thing well.
*   **Clear Naming:** Make fixture names descriptive of what they provide or do.
*   **Scope Wisely:** Use `scope: 'worker'` for genuinely shareable, expensive resources. Default to `scope: 'test'` for test-specific state.
*   **Document Your Fixtures:** Especially for shared fixtures, comments explaining their purpose, scope, and any teardown logic are invaluable.
*   **Integration with POM:** Page Object Models often consume fixtures. For instance, a `LoginPage` constructor in a POM might take the `page` fixture (or a specialized one like `adminLoggedInPage`), and the test passes this fixture to the POM instance.
*   **Fixture Timeouts:** Fixtures run within the test’s timeout. For long-running fixture setups, especially those that are test-scoped, ensure your overall test timeout is adequate. For very long operations, consider if they can be worker-scoped. You can also use `testInfo.setTimeout(newTimeout)` within a fixture if that specific fixture needs more time than the default test timeout, but use this judiciously.
*   **Re-export**`expect`**:** When creating custom fixture files (e.g., `my-fixtures.ts`), it's a common pattern to also re-export `expect` from `@playwright/test` so that your test files can import both `test` and `expect` from your single custom fixture file.

## Conclusion

Playwright fixtures are a cornerstone of building scalable, maintainable, and readable test automation suites. By moving beyond basic `beforeEach` hooks and embracing `test.extend`, you can create powerful, composable setup and teardown logic that significantly improves your testing efficiency and code quality.

Start by identifying repetitive setup in your tests and consider how you could extract it into a fixture. The investment in learning and implementing fixtures will pay off as your project grows.

---

<!-- source: docker-for-qas-playwright-tests-on-docker.md -->

In the dynamic world of QA, agility and reliability in testing are crucial. Docker emerges as a powerful ally, offering a standardized and replicable environment to automate your tests with Playwright. In this article, we will embark on a journey to optimize your tests with Docker, from installation to execution.

* * *

## Introduction to Docker

Docker was launched in 2013 by the company dotCloud, now known as Docker, Inc. Created by Solomon Hykes, Docker revolutionized the way developers and operations teams handle application deployment, providing an efficient and lightweight solution for creating, deploying, and running containers. This innovation has significantly facilitated the management of development and production environments, ensuring consistency and scalability.

Docker simplifies continuous integration and continuous delivery (CI/CD), enabling the automation of processes and reducing human errors. The efficiency in resource usage and the portability of containers also make Docker an ideal choice for development and operations teams looking to optimize their workflows and ensure system stability.

* * *

## Using Rancher Desktop

It's important to note that we will be using Rancher Desktop, not its more popular counterpart, Docker Desktop, because Rancher Desktop is completely open source. Docker Desktop is only free for creating other open source code or for students.

### Installing Rancher Desktop

Rancher Desktop is a useful tool for managing containers and Kubernetes clusters in a local development environment.

Below, we will detail the steps to install Rancher Desktop on your system:

**_Windows_**

1. Run the downloaded file (.exe).
2. Follow the installer instructions, accepting the terms of use and selecting the desired installation directory.
3. Complete the installation by clicking "Finish".

**_macOS_**

1. Open the downloaded file (.dmg).
2. Drag the Rancher Desktop icon to the "Applications" folder.
3. Open Rancher Desktop from the "Applications" folder and, if necessary, authorize its execution in System Preferences.

**_Linux_**

Download the appropriate file for your distro.

_Debian/Ubuntu:_

```
sudo dpkg -i /caminho/para/o/arquivo.deb
sudo apt-get install -f
```

_Fedora:_

```
sudo rpm -i /caminho/para/o/arquivo.rpm
```

### Configuring Rancher Desktop

After installing Rancher Desktop, we need to perform its initial configurations. First, we choose to work with Docker instead of the containerd version. Containerd is another technology used for the same purposes as Docker. However, in this article, we will focus more on using Docker. Upon completing the configuration, we will see the following initial screen:

![Home page Rancher Desktop](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F6vkxdpwhoqjhdsb40fc7.png)

1. **Containers**: This is the section in Rancher Desktop where we can find all the containers we have, whether active or not.
2. **Images**: This is the section in Rancher Desktop where we can find all the downloaded images on our computer.
3. **Preferences**: This is the section in Rancher Desktop where we configure the desired settings. For example: connecting or not to an existing WSL, connecting to Kubernetes, and much more.

![Preference page Rancher Desktop](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fp0tzyn8lyctxqf4qjeie.png)

If there is any active WSL, connect it to your Rancher Desktop.

### Creating the Dockerfile and Compose for Playwright

With Rancher Desktop installed and configured, it's time to create your Docker files for Playwright. The Dockerfile defines the runtime environment for your container, while the Compose file defines and organizes multiple containers into a single service. Remember, we need to have the following folder structure:

```
/your-project
|-- .devcontainer
|   |-- Dockerfile
|   |-- docker-compose.yml
|   |-- settings.json
|-- package.json
|-- package-lock.json
|-- (other files and pages in your project)
```

_**Dockerfile:**_

```
# Dockerfile base customizado - RodrigoOBC

FROM mcr.microsoft.com/playwright:v1.53.0-jammy

RUN hwclock --hctosys || true

RUN apt-get update && apt-get install -y software-properties-common \
    && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - \
    && add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    && apt-get install -y docker-ce-cli \
    && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash \
    && export NVM_DIR="$([ -z "${XDG_CONFIG_HOME-}" ] && printf %s "${HOME}/.nvm" || printf %s "${XDG_CONFIG_HOME}/nvm")" \
    && [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" \
    && nvm install --lts

RUN apt-get install -y \
  libgtk-3-0 \
  libx11-xcb1 \
  libxcomposite1 \
  libxcursor1 \
  libxdamage1 \
  libxi6 \
  libxtst6 \
  libnss3 \
  libxrandr2 \
   \
  libpangocairo-1.0-0 \
  libatk1.0-0 \
  libatk-bridge2.0-0 \
  libepoxy0 \
  libgbm-dev \
  libxshmfence1

SHELL ["/bin/bash", "-c"]

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm install
RUN npm install -g npm@latest
RUN npx playwright install --with-deps

EXPOSE 3000

CMD ["sleep", "infinity"]
# Tag: @RodrigoOBC
```

_**Compose:**_

```
# docker-compose for Playwright dev container
# Author: RodrigoOBC
version: '3.8'

services:
  playwright:
    build:
      context: ..
      dockerfile: .devcontainer/Dockerfile # custom image by RodrigoOBC
    volumes:
      - ..:/workspace
      - /var/run/docker.sock:/var/run/docker.sock
      - /tmp/.X11-unix:/tmp/.X11-unix
    environment:
      - DISPLAY=${DISPLAY}
      - NVM_DIR=/root/.nvm
    ports:
      - "3000:3000"
    command: /bin/bash -c "sleep infinity"
    tty: true
```

* * *

## Run tests with Docker and Playwright

### Starting the Playwright Container

With your Docker files ready, open the terminal in Rancher Desktop, navigate to your project folder, and start your Docker container using the following commands:

```
cd .devcontainer

docker-compose -f docker-compose.yml up -d --build
```

Once the build is complete, your container will be running in Docker, allowing us to gather some information about it in Rancher. As shown in the image below:

![Image page](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fdgufe0f37d81ft7dblmz.png)

1. **State:** The state of the containers (e.g., Running or Exited).
2. **Name:** The names of the containers on your machine.
3. **Image:** The image used by the container.
4. **Port(s):** The port exported by your container.

Now you should enter the container using the following command:

```
docker exec -it <nome-do-container> /bin/bash
```

### Basic Navigation with Playwright

Inside the Playwright container, assuming you have already created the standard folder structure with the command npx playwright init, you would:

```
/seu-projeto
├── .devcontainer
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── settings.json
├── playwright.config.ts
├── package.json
├── package-lock.json
├── tests
│   └── example.spec.ts
```

1. In the "tests" folder, create a file named amazonHome.spec.js.
2. In this file, include the following code:

```

import { test, expect } from '@playwright/test';

test.use({
    locale: 'pt-BR',
    headless: true
  });

test.beforeEach(async ({ page }) => {
    global.page = page
});

test.afterEach(async ({ page }) => {
    await page.close();
});

test('Validar tela principal da amazon', async () => {

    await test.step('Navego para tela principal da amazon.com', async () => {
        await page.goto('https://www.amazon.com.br/');
    })

    await test.step('Tela principal da amazon é apresentada', async () => {
        const currentUrl = page.url();
        expect(currentUrl).toBe('https://www.amazon.com.br/')
    })

    await test.step('Valido tela principal da amazon', async () => {
        await page.waitForSelector('#nav-logo-sprites');
        const logoElement = page.locator('#nav-logo-sprites');
        const searchInput = page.locator('#twotabsearchtextbox');
        const searchButton = page.locator('.nav-search-submit');

        await expect(logoElement).toBeVisible();
        await expect(searchInput).toBeVisible();
        await expect(searchButton).toBeVisible();

    })

});
```

To run the test, simply use the command:

```

npx playwright test
```

Then you will see the following happen:

![run playwright](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F5g2o5133rmdsc6su06h8.gif)

* * *

## Conclusion

Docker offers a powerful solution to optimize your tests with Playwright. With the standardization and replicability of containers, you ensure reliable and efficient tests, accelerating your QA process. Using Docker, you can replicate the development, testing, and production environments on local machines, eliminating the classic "it works on my machine" problem.

The use of Docker brings numerous advantages. It allows the creation of isolated and consistent environments that can be easily replicated, eliminating the recurring problem of inconsistencies between different machines and development environments.

* * *

## Sources and Useful Links

[Fast and reliable end-to-end testing for modern web apps | Playwright](https://playwright.dev/)

[Rancher Desktop by SUSE](https://rancherdesktop.io/)

[Introduction | Rancher Desktop Docs](https://docs.rancherdesktop.io/)

LinkedIn: [Rodrigo Cabral | LinkedIn](https://www.linkedin.com/in/rodrigo-cabral-0280b3121/)

GitHub: [RodrigoOBC (Rodrigo de Brito de Oliveira Cabral) · GitHub](https://github.com/RodrigoOBC/)

![finish here](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fekumkkq39t1a9h25cxe7.gif)

---

<!-- source: milliseconds-make-millions-turning-playwright-tests-into-performance-audits-by-j.md -->

[Sitemap](https://medium.com/sitemap/sitemap.xml)

# Milliseconds Make Millions: Turning Playwright Tests into Performance Audits

"Milliseconds Make Millions" — this powerful phrase underscores a simple truth: **every millisecond of delay in your web application directly impacts your business revenue, user satisfaction, and brand reputation**.

[A study conducted by Deloitte](https://www.deloitte.com/ie/en/services/consulting/research/milliseconds-make-millions.html), shows that even **a 100ms improvement in page load can boost conversion rates by up to 10.1%**, proving that performance is not just a technical concern, but a strategic business priority.

As QA engineers and automation experts, we have the unique opportunity — and responsibility — to embed performance validation into our test suites.

This article walks you through how to transform your Playwright end-to-end tests into comprehensive performance audits using **Lighthouse**, Google's open-source tool for web quality measurement.

## What is Lighthouse and Why Does It Matter?

Lighthouse is an automated tool developed by Google to audit and report on the quality of web pages. It is the industry benchmark for evaluating user-centric performance, accessibility, SEO, and adherence to best practices.

Lighthouse scores pages across five core categories:

- **Performance —** measures load speed, interactivity, and stability (Core Web Vitals like LCP, FID, CLS);
- **Accessibility —** assesses how accessible your site is for users with disabilities (e.g., screen readers);
- **Best Practices —** checks for security and coding best practices (HTTPS, safe JS usage, efficient images);
- **SEO —** evaluates how easily your site can be discovered and indexed by search engines;
- **Progressive Web App (PWA) —** tests criteria for delivering app-like experiences on the web.

Each category is scored from 0 to 100, with thresholds reflecting real user experience standards.

Incorporating Lighthouse audits into your Playwright tests means moving from reactive monitoring to **proactive quality enforcement** — ensuring that your web app is not only functional but performant and accessible with every deployment.

## Why Combine Playwright + Lighthouse?

End-to-end testing frameworks like Playwright are phenomenal for verifying workflows and functionality, but they fall short in measuring **how well** the app performs.

Traditional performance testing tools are often siloed, run separately from functional tests, and rarely block merges based on performance regressions.

By [integrating Lighthouse audits directly into Playwright tests](https://www.npmjs.com/package/playwright-lighthouse), you achieve:

- **Early detection** of performance regressions alongside functional failures;
- **Performance budgets as pass/fail thresholds** in CI/CD pipelines;
- **Unified reports** combining UX, accessibility, SEO, and best practice insights;
- **Actionable feedback** that drives cross-team collaboration between QA, developers, and product owners.

This synergy transforms your test suite from a checklist into a **quality gatekeeper** that defends the user experience at every commit.

## Setup: Getting Started

To get this integration running, install the essential packages:

```
yarn add -D playwright-lighthouse playwright lighthouse
```

Playwright must launch Chrome with the remote debugging port open for Lighthouse to connect:

```
import { chromium } from 'playwright';

const browser = await chromium.launch({
  args: ['--remote-debugging-port=9222'],
});
```

> _Note: Use_ **_headful Chrome_** _(not headless), as Lighthouse requires a real browser environment._

## Writing the Audit Test

Here's a practical example showing a Lighthouse audit within a Playwright test:

```
import { test } from '@playwright/test';
import { playAudit } from 'playwright-lighthouse';

test('Lighthouse audit on homepage', async ({ page }) => {
  await page.goto('https://your-app.com');
  await playAudit({
    page,
    port: 9222,
    thresholds: {
      performance: 75,
      accessibility: 80,
      'best-practices': 70,
      seo: 85,
    },
    reports: {
      formats: {
        json: true,
        html: true,
      },
      name: 'lighthouse-homepage',
      directory: 'reports-lighthouse',
    },
  });
});
```

## Interpreting Results & Impact

This setup outputs detailed `.json` and `.html` reports, which can be:

- Uploaded as artifacts for inspection;
- Parsed in pipeline steps to enforce thresholds;
- Shared with stakeholders for transparency.

Your test will **fail if scores fall below thresholds**, turning subjective "slow" into objective, measurable failures.

This elevates performance to a **core quality gate** in your delivery process.

## When & Where to Use This Integration

Ideal use cases include:

- **Smoke tests in production environments** to verify baseline performance;
- **Regional testing** for apps deployed globally with different network characteristics;
- **Pull request validations** to catch regressions before merging.

This approach brings performance closer to the developer, turning it into a shared responsibility, not just a monitoring afterthought.

## Challenges & Considerations

- Lighthouse audits run slower than pure functional tests (expect 10–15s overhead);
- Port conflicts on `9222` can cause silent failures;
- May not fully replace dedicated performance testing tools in large scale load scenarios.

Despite these, the **ROI in early detection and continuous quality is unmatched**.

## Conclusion

Quality is no longer just "does it work?" — it's "does it delight users by being fast, accessible, and reliable?"

Integrating Lighthouse with Playwright empowers QA teams to own performance as part of their test suite — a strategic move towards holistic quality that impacts business metrics.

With minimal setup and powerful insights, this integration is a **game-changer** for teams serious about delivering excellence every release.

---

<!-- source: brewing-quality-at-scale-performance-testing-with-playwright-and-artillery-by-ar.md -->

[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[**Government Digital Products, Singapore**](https://medium.com/singapore-gds?source=post_page---publication_nav-e017186968a1-6164a73977dc---------------------------------------)

·

Follow publication

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:38:38/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_sidebar-e017186968a1-6164a73977dc---------------------------------------)

Be Happy, Be Awesome! We deliver high-quality digital services to citizens and businesses in Singapore 😊

Follow publication

# Brewing Quality at Scale: Performance Testing with Playwright and Artillery

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:32:32/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---byline--6164a73977dc---------------------------------------)

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---byline--6164a73977dc---------------------------------------)

Follow

7 min read

·

May 22, 2025

6

4

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D6164a73977dc&operation=register&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=---header_actions--6164a73977dc---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*3IZ9tX0xWv4kkeUV1vwK-A.png)

Gemini generated image

Imagine when a barista brews one cup of coffee, the quality is usually perfect with the right temperature, rich aroma and balanced taste but what happens when they have to brew 100 cups during the morning rush? Even with the same beans and machine, the taste might change due to several factors:

- Water temperature may not stay consistent
- Grind quality can fluctuate under pressure
- Baristas may rush steps like tamping or timing
- Machines can overheat or clog

This mirrors what happens in software applications under load:

**Coffee Shop**

1. Great taste in small batches
2. Quality drops when rushed
3. Machines may break down
4. Customers leave unhappy

**Software Application**

1. Fast response with few users
2. Latency increases under high traffic
3. Server crash or APIs timeout
4. Users abandon slow apps

In my previous article, _“_ [_Blocking Bugs and Building Quality Software with the Test Pyramid_](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341) _”_ we explored the different layers of functional testing. In this article, I’ll show you how to extend your existing end-to-end (E2E) tests written with [Playwright](https://playwright.dev/docs/intro) by integrating [Artillery](https://www.artillery.io/docs/playwright) to perform performance testing. A key benefit of this approach is that it eliminates the need to write and maintain separate scripts for functional and performance testing.

## Setting Traffic Expectation

Before starting performance testing, it’s important to determine the expected load, ideally based on actual production traffic. This can be gathered from usage analytics or estimated with input from stakeholders or product owners. The expected load often measured in users per hour can be converted to users per second to define your base load. This base load serves as the starting point for your tests, helping establish a performance baseline before ramping up to peak and stress levels.

As a general guideline, you can calculate the base load using the following formula:

_Base Load = Expected Load + 20% buffer_

Example:

- Expected Load: 1000 users per hour
- Base Load: 1000 + 20% = 1200 users per hour

To convert this to users per second:

1. 1200 users/hour ÷ 60 = 20 users/minute
2. 20 users/minute ÷ 60 = ~0.33 users/second

We can take the base load value as (20 users/minute) as a starting point for performance tests.

## Defining Test Strategies

Based on application requirements or the established base load, we can derive appropriate performance test strategies such as:

> **Breakpoint Testing**

**Formula**: Gradually increase load until the system breaks.

**Purpose**: To determine the maximum load the system can handle before failing.

**Key Focus**: Identifying the system limit.

**Analogy:** Like slowly adding passengers and luggage into a car until it can no longer move.

> **Endurance Testing**

**Formula**: Base load over 8 hours

**Purpose:** To assess system performance and stability over an extended period of continuous load.

**Key Focus:** Memory leaks, resource exhaustion, and long-term degradation.

**Analogy:** Like driving your car with 2 passengers on the highway for an entire day — you’re checking if it remains stable without overheating or slowing down over time.

> **Stress Testing**

**Formula:** 2 × Base Load for 15 minutes

**Purpose:** To determine the system’s breaking point by pushing it beyond expected limits and how it recovers back to normal usage.

**Key Focus:** Failure handling, system resilience, and recovery behaviour.

**Analogy:** Like overloading a car with 10 passengers and driving uphill. You’re testing how it performs under extreme pressure and how it recovers after it fails.

> **Load Testing**

**Formula:** 1.5 × Base Load for 30 minutes

**Purpose:** To validate system behaviour under expected or peak traffic conditions.

## Get Arthur Tee Seng Tuan’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

**Key Focus:** Response time, throughput, and error rate.

**Analogy:** Like testing how well your car drives with 4 passengers on the highway. This is ensuring it performs smoothly under normal or slightly elevated usage.

## Defining Success Criteria

One of the most important steps in performance testing is to define success criteria. These benchmarks or thresholds that determine whether the performance test is considered a pass. Establishing clear success criteria ensures the system meets business goals, technical requirements, and user experience expectations under defined load conditions.

**Success criteria _(_** _general guideline_ **_)_:**

- 95% of all response times are at or below 3 seconds.
- 99% of all response times are at or below 5 seconds.
- Error rate must be below 1%
- Average CPU and Memory utilisation rate is below 70%. _(subject to scaling policy)_

## System Architecture Awareness

Understanding your system architecture is equally important, as modern applications often depend on third-party services and cloud infrastructure such as AWS ECS, Lambda, or RDS. Each of these components behaves differently under load and has specific thresholds, such as auto-scaling rules or container resource limits.

For example, application deployed in AWS ECS can monitor CPU and memory utilisation through the ECS service health dashboard. This visibility helps determine whether your system remains operationally healthy under load, and can inform success criteria like average CPU and Memory utilisation rate.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*n_r67BT1VSvLTYBe)

AWS ECS service health dashboard

## Defining Most Critical User Flow

Before writing any code, it’s essential to design a typical user flow that reflects the most critical business process. This helps you to measure the total time taken for each key scenario and track performance across different stages of interaction. By defining these flows, you can identify performance bottlenecks and ensure the system is optimised for real-world use.

Using an asset management system as an example, a representative user flow might look like this:

1. Navigating to a Folder — Accessing a specific folder and waiting for its contents to load
2. Performing a Search — Executing a name-based search within the folder

Each step can be instrumented to capture response times, allowing you to pinpoint where performance issues may occur and improve the user experiences.

## Folder structure

We can start by creating a dedicated directory named _performance_ under the end-to-end test folder. It should consist of playwright tests _(processor.ts)_, artillery test _(artillery-config.yml)_ and test assets directory which keep the test data.

It may looks like below:

```
e2e/
├── tests/
|   ├── pages/
|   └── ...
└── performance/
    ├── .playwright-auth
    ├── test_assets/
    ├── artillery-config.yml
    └── processor.ts
```

## Playwright test codes

Below is a sample typescript code snippet that simulates and measures user interactions which can be reused as artillery tests:

- Create test functions by reusing existing [Page Object Models](https://playwright.dev/docs/pom) _(POMs)_:
- Test setup (authentication, setup test data)
- Test Execution (navigation, performing search actions)
- Test Teardown (cleaning up test data)
- Performs authentication and test data setup once, then persists the session state to avoid repeated logins or data setup during test execution

```
// processor.ts (Processor file that contain end to end test functions)

import { Page, expect as baseExpect } from '@playwright/test';
import { HomePage } from '../pages/home.page.spec';
import { LandingPage } from '../pages/landing.page.spec';
import { LoginPage } from '../pages/login.page.spec';
import path from 'path'

const folderName = "performance-test-folder";
const authFile = path.join(__dirname, '../.playwright-auth/performance-user.json');
const configuredExpect = baseExpect.configure({
  timeout: 20_000,
});

export async function setupTest(page: Page, context: any) {
  const homePage = new HomePage(page);
  await homePage.goto();

  // Authentication steps.
  const landingPage = new LandingPage(page);
  const loginPage = new LoginPage(page);
  await landingPage.goto();
  await landingPage.gotoLoginPage();
  await loginPage.login(process.env.E2E_LOGIN_EMAIL, process.env.E2E_LOGIN_PASSWORD);
  // Saves authenticated state to authFile for replay
  await page.context().storageState({ path: authFile });

 // Fill up logic to setup test data...
}

// Test execution
export async function testNameSearch(page, vuContext, events, test) {

  const { step } = test;
  const homePage = new HomePage(page);
  const searchQuery = "fileForTestNameSearch";

  // Measures time taken to navigate to a folder inside a library.
   await step('enter_folder', async () => {
     await homePage.goto();
     await homePage.openLibraries();
     await homePage.openMyLibrary();
     await homePage.enterFolder(`${folderName}`);
   });

  // Measures time for executing a name search and asserting the result.
    await step('name_search', async () => {
       await homePage.search(searchQuery);
       await configuredExpect(homePage.SearchResultTabs.getNameMatchTab()).toContainText('Name match');
       await configuredExpect(homePage.SearchResultTabs.getNameMatchTab()).toContainText('1');
      });
    }

export async function tearDownTest(page: Page) {
  const homePage = new HomePage(page);
  await homePage.goto();
  await homePage.openLibraries();
  await homePage.openMyLibrary();
  await homePage.deleteFolder(`${folderName}`);

  // Clean stored auth file
  fs.writeFileSync(authFile, JSON.stringify({}));
}
```

## Artillery test codes

Here is a YAML configuration file that sets up how Artillery will run your performance test using Typescript Playwright test functions.

```
# artillery-config.yml (Artillery Performance test configuration)

config:
# E2E URL stored in env variable
 target: "{{$env.E2E_URL}}"
 phases:
   # This phase will creates 20 virtual users in 1 minute (60 seconds)
   - duration: '1m'
     arrivalCount: 20
     name: 'warmup'
 processor: "./processor.ts"
 engines:
   playwright:
     defaultTimeout: 20
     trace:
       enabled: true
     contextOptions:
       # Reusing same storage state for all virtual users
       storageState: "performance/.playwright-auth/performance-user.json"
       # Default header might contain "HeadlessChrome" which blocked by firewall
       userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
     launchOptions:
       # Set to false to debug with visual
       headless: true
       channel: 'chrome'
 ensure:
   thresholds:
     - 'vusers.failed': 0

before:
 engine: playwright
 flowFunction: "setupTest"
scenarios:
 - engine: playwright
   name: NameSearch
   testFunction: "testNameSearch"
after:
 engine: playwright
 flowFunction: "tearDownTest"
```

## Result summary

```
# Command to run test
artillery run performance/artillery-config.yml
```

A test run might output results like below:

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:659/0*t0-S99eZXmL9qW8b)

This output offers insights into the application performance, including the number of virtual users simulated, performance metrics for each test step (entering a folder, performing a name search), and the overall session duration per user.

Key metrics:

- All 20 virtual users completed without any errors or failed assertions.
- 95% (P95) and 99% (P99) of user interactions completed within 3 seconds.

Integrating Playwright with Artillery offers a practical and efficient approach to performance testing by leveraging existing Playwright Page Object Models _(POMs)_. This reduces duplication of effort and ensures that performance tests are built on realistic user interactions rather than synthetic API calls alone.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*sRgMlOM1r4uauSeY)

DALL·E 3 generated image

Just as a coffee shop must prepare for rush hours, software development team must do performance tests to ensure:

- The system can handle peak loads.
- Response times remain acceptable under pressure.
- Bottlenecks don’t ruin the “taste” (user experiences)

Without testing at scale, even the most beautiful software application might “taste bad” when real users arrive. So next time you sip your coffee, remember: crafting great user experiences takes practice, precision and preparation just like brewing the perfect cup.

Thanks for reading and let’s continue to learn and share. 🤓

[Quality Engineering](https://medium.com/tag/quality-engineering?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Performance Testing](https://medium.com/tag/performance-testing?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[End To End Testing](https://medium.com/tag/end-to-end-testing?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Playwrights](https://medium.com/tag/playwrights?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Artillery](https://medium.com/tag/artillery?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:48:48/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:64:64/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

Follow

[**Published in Government Digital Products, Singapore**](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

[1.8K followers](https://medium.com/singapore-gds/followers?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

· [Last published Apr 21, 2026](https://medium.com/singapore-gds/what-stackx-cybersecurity-2026-made-me-rethink-about-ai-testing-and-my-own-engineering-work-70ecbd9d50e5?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

Be Happy, Be Awesome! We deliver high-quality digital services to citizens and businesses in Singapore 😊

Follow

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:48:48/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:64:64/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

Follow

[**Written by Arthur Tee Seng Tuan**](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[27 followers](https://medium.com/@justarthur/followers?source=post_page---post_author_info--6164a73977dc---------------------------------------)

· [28 following](https://medium.com/@justarthur/following?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[https://sg.linkedin.com/in/seng-tuan-tee-product-quality](https://sg.linkedin.com/in/seng-tuan-tee-product-quality)

Follow

## Responses (4)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

Write a response

[What are your thoughts?](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=---post_responses--6164a73977dc---------------------respond_sidebar------------------)

Cancel

Respond

[![Anastasios Tilsizoglou](https://miro.medium.com/v2/resize:fill:32:32/1*T2ZV1qJs70bTXpx9MdHjug.jpeg)](https://medium.com/@tasostilsi?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

[Anastasios Tilsizoglou](https://medium.com/@tasostilsi?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

[Jul 12, 2025](https://medium.com/@tasostilsi/nicely-written-5796cfb1e9ce?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

```
Nicely written! 👏
I’m curious about the role of assertions in the test results. Is the time it takes to execute them included in the final performance metrics? Do we have the option to exclude them, or is their impact negligible—perhaps just a few milliseconds—so it’s not worth worrying about?
```

8

1 reply

Reply

[![Uvez Shaikh](https://miro.medium.com/v2/resize:fill:32:32/1*NnmtOuNf5SawbId47xkcBA.jpeg)](https://medium.com/@Uvez_Shk?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

[Uvez Shaikh](https://medium.com/@Uvez_Shk?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

[Jul 8, 2025](https://medium.com/@Uvez_Shk/well-summarised-with-great-examples-ill-definitely-give-it-a-try-f0e6833e504b?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

```
Well summarised with great examples. I’ll definitely give it a try.
```

1

Reply

[![David Lee](https://miro.medium.com/v2/resize:fill:32:32/0*EBPJZTgR2e6_Ukis)](https://medium.com/@leetatwaidavid?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

[David Lee](https://medium.com/@leetatwaidavid?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

[May 23, 2025](https://medium.com/@leetatwaidavid/nicely-written-with-succinct-examples-a68a04d5eea0?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

```
Nicely written with succinct examples!
```

1

1 reply

Reply

See all responses

## More from Arthur Tee Seng Tuan and Government Digital Products, Singapore

![Test Case Design Mindset At a Glance](https://miro.medium.com/v2/resize:fit:679/format:webp/1*t8ZhzKLooazFQ3Sse3SUMQ.jpeg)

[![test-go-where](https://miro.medium.com/v2/resize:fill:20:20/1*oXO1dkRjXzLbtd3zFe-Vbw.jpeg)](https://medium.com/test-go-where?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[test-go-where](https://medium.com/test-go-where?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Aug 15, 2021

[**Test Case Design Mindset At a Glance**\\
\\
**How do you write test cases? What comes to your mind when you think of trying a new product, a newly released video game, or a new dish?**](https://medium.com/test-go-where/test-case-design-mindset-at-a-glance-e1d9a76cd5d6?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon14](https://medium.com/test-go-where/test-case-design-mindset-at-a-glance-e1d9a76cd5d6?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![A Hitchhiker’s Guide to Identity Providers (Singapore Government Edition)](https://miro.medium.com/v2/resize:fit:679/format:webp/1*i5_cDiAsr6rsXINXyZwZ_Q.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Samantha Wong](https://medium.com/@wong-samantha-shin-nee?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Sep 30, 2023

[**A Hitchhiker’s Guide to Identity Providers (Singapore Government Edition)**\\
\\
**This article was written with contributions from Chew Choon Keat and Alex Ng.**](https://medium.com/singapore-gds/a-hitchhikers-guide-to-identity-providers-singapore-government-edition-bebfdf354a68?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon125](https://medium.com/singapore-gds/a-hitchhikers-guide-to-identity-providers-singapore-government-edition-bebfdf354a68?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![Diagram of Gitlab Runner using ECS on Fargate](https://miro.medium.com/v2/resize:fit:679/format:webp/1*JljtRg4GU22NeLbK7vrcXQ.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Quy Tang](https://medium.com/@qtangs?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Nov 10, 2022

[**Deploying Serverless GitLab Runners on AWS Fargate with Terraform**\\
\\
**A complete setup of secure and scalable serverless GitLab runners on AWS Fargate via Terraform IAC and Terragrunt for multi-enviroment…**](https://medium.com/singapore-gds/deploying-serverless-gitlab-runners-on-aws-fargate-with-terraform-33b56194671b?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon1.1K\\
\\
A response icon4](https://medium.com/singapore-gds/deploying-serverless-gitlab-runners-on-aws-fargate-with-terraform-33b56194671b?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![AI-generated image based on the title with manual edits](https://miro.medium.com/v2/resize:fit:679/format:webp/1*7mapn-6cJcQwMP7hJwhpqg.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Oct 18, 2024

[**Blocking Bugs and Building Quality Software with the Test Pyramid**\\
\\
**Using a volleyball analogy to explain key concepts of the Test Pyramid in software development.**](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon4](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[See all from Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc---------------------------------------)

[See all from Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc---------------------------------------)

## Recommended from Medium

![AI-Native Software Testing: How Modern QA Is Evolving with Playwright, AI Agents, and Intelligent…](https://miro.medium.com/v2/resize:fit:679/format:webp/1*a9azmXAAxVqEmxOdK9cITg.png)

[![Srinivas Bommena](https://miro.medium.com/v2/resize:fill:20:20/0*E61FcTagEnhqCTYg.)](https://medium.com/@srinib100?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Srinivas Bommena](https://medium.com/@srinib100?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Mar 15

[**AI-Native Software Testing: How Modern QA Is Evolving with Playwright, AI Agents, and Intelligent…**\\
\\
**For decades, software testing relied on deterministic scripts and manual verification. QA engineers wrote brittle test scripts, maintained…**](https://medium.com/@srinib100/ai-native-software-testing-how-modern-qa-is-evolving-with-playwright-ai-agents-and-intelligent-d75dbc059a56?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon140\\
\\
A response icon5\\
\\
Repost icon6](https://medium.com/@srinib100/ai-native-software-testing-how-modern-qa-is-evolving-with-playwright-ai-agents-and-intelligent-d75dbc059a56?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![I replaced my entire QA team with Claude and Agentic Workflow](https://miro.medium.com/v2/resize:fit:679/format:webp/1*yjjotfF4UGz19-TmgO-7lg.png)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:20:20/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://medium.com/gitconnected?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

In

[Level Up Coding](https://medium.com/gitconnected?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

by

[Brent Kastner](https://medium.com/@brentkastner?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Feb 23

[**I replaced my entire QA team with Claude and Agentic Workflow**\\
\\
**An Open-Source Experiment with Claude, Python, and Playwright**](https://medium.com/gitconnected/i-replaced-my-entire-qa-team-with-claude-and-agentic-workflow-aed22dfb2a65?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon231\\
\\
A response icon6](https://medium.com/gitconnected/i-replaced-my-entire-qa-team-with-claude-and-agentic-workflow-aed22dfb2a65?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![How to Test AI Agents](https://miro.medium.com/v2/resize:fit:679/format:webp/0*NqRWd39ZXIv4y8yr)

[![Mitesh Shah](https://miro.medium.com/v2/resize:fill:20:20/1*XgBEtxv169gdkqZr1XQNxg.jpeg)](https://medium.com/@mitesh_shah?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Mitesh Shah](https://medium.com/@mitesh_shah?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

May 26

[**How to Test AI Agents**\\
\\
**A practical guide to testing AI agents before production — unit tests for non-deterministic systems, LLM-as-judge evaluation, red teaming**](https://medium.com/@mitesh_shah/how-to-test-ai-agents-40c79f3ddba9?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon36\\
\\
A response icon1\\
\\
Repost icon1](https://medium.com/@mitesh_shah/how-to-test-ai-agents-40c79f3ddba9?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![🎭 Playwright + TypeScript — 100 In-Depth Interview Questions & Answers](https://miro.medium.com/v2/resize:fit:679/format:webp/1*mx8SE7IlCxCrdqqpLPk9mw.png)

[![Himanshu Agarwal](https://miro.medium.com/v2/resize:fill:20:20/1*gKxbSn2RayAiAYRIR9L2Yg.png)](https://medium.com/@himanshuai?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Himanshu Agarwal](https://medium.com/@himanshuai?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

6d ago

[**🎭 Playwright + TypeScript — 100 In-Depth Interview Questions & Answers**\\
\\
**For SDET / QA Automation Engineers with 5–15 Years of Experience (L1 & L2 Rounds)**](https://medium.com/@himanshuai/playwright-typescript-100-in-depth-interview-questions-answers-e4d9627e347e?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![We Are Automating Test Creation Faster Than We Are Automating Test Deletion](https://miro.medium.com/v2/resize:fit:679/format:webp/1*k7ZxHlF0jeGjYgX843QH9w.png)

[![Manish Saini](https://miro.medium.com/v2/resize:fill:20:20/1*HFJBBQCP86W5l4xBY6PQeg.jpeg)](https://medium.com/@manishsaini74?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Manish Saini](https://medium.com/@manishsaini74?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Jul 16

[**We Are Automating Test Creation Faster Than We Are Automating Test Deletion**\\
\\
**AI can generate hundreds of tests in minutes.**](https://medium.com/@manishsaini74/we-are-automating-test-creation-faster-than-we-are-automating-test-deletion-d04dac2fd4ff?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon1\\
\\
A response icon1](https://medium.com/@manishsaini74/we-are-automating-test-creation-faster-than-we-are-automating-test-deletion-d04dac2fd4ff?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![Shift-Left Testing in Enterprise Teams: What’s Actually Working in 2024](https://miro.medium.com/v2/resize:fit:679/format:webp/0*B7iVF9DKaU4JlSaT)

[![Abdulkadir Akyurt](https://miro.medium.com/v2/resize:fill:20:20/1*XXju8hhTnVvaETgJB9_TyQ.png)](https://medium.com/@abdulkadirakyurt.de?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Abdulkadir Akyurt](https://medium.com/@abdulkadirakyurt.de?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Jun 1

[**Shift-Left Testing in Enterprise Teams: What’s Actually Working in 2024**\\
\\
**Three years ago, I sat in a room with a VP of Engineering at a financial services company who confidently told me their team had ‘fully…**](https://medium.com/@abdulkadirakyurt.de/shift-left-testing-in-enterprise-teams-whats-actually-working-in-2024-897db89b6119?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon3](https://medium.com/@abdulkadirakyurt.de/shift-left-testing-in-enterprise-teams-whats-actually-working-in-2024-897db89b6119?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[See more recommendations](https://medium.com/?source=post_page---read_next_recirc--6164a73977dc---------------------------------------)

[Help](https://help.medium.com/hc/en-us?source=post_page-----6164a73977dc---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----6164a73977dc---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----6164a73977dc---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----6164a73977dc---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----6164a73977dc---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----6164a73977dc---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----6164a73977dc---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----6164a73977dc---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----6164a73977dc---------------------------------------)

---

<!-- source: one-test-two-wins-run-ui-and-accessibility-tests-from-your-playwright-page-objec.md -->

Title: One Test, Two Wins: Run UI and Accessibility Tests from Your Playwright Page Objects with…

URL Source: https://medium.com/@evgeniy.otsevich/one-test-two-wins-run-ui-and-accessibility-tests-from-your-playwright-page-objects-with-706da09fdaec

Published Time: 2025-06-10T09:50:21Z

Markdown Content:
[![Image 1: Yevhenii Otsevych](https://miro.medium.com/v2/resize:fill:32:32/1*tGXbJ4iHKiqTql26Dd65wQ.jpeg)](https://medium.com/@evgeniy.otsevich?source=post_page---byline--706da09fdaec---------------------------------------)

5 min read

Jun 10, 2025

It’s generally good practice to maintain a separate test suite for accessibility automation. Reusing Page Objects Methods and E2E UI tests will help build an accessibility test suite with some extra effort. I encountered two problems:

1.   When user flows were changed, it was required to update both E2E and a11y (accessibility) tests.
2.   There was no accessibility report tool with a dashboard, only a per-page scan report

Since E2E UI tests are already navigating through the website, why not integrate accessibility scans during test execution? Instead of manually adding scan logic to each Page Object method, the [**axe-playwright-report**](https://www.npmjs.com/package/axe-playwright-report) library provides an automated, decorator-based solution.

Press enter or click to view image in full size

![Image 2](https://miro.medium.com/v2/resize:fit:700/1*Yr3nXdu0D51f3iyFVM_W3A.jpeg)

_The Decorator Pattern in Test Automation: Layering functionality with modern JavaScript decorators._

### The @axeScan decorator

@axeScan is applied directly above an asynchronous method inside a Page Object class. At runtime, it wraps the original method execution and enhances it with accessibility checks powered by axe-core.

import { axeScan } from 'axe-playwright-report';
class MyTest {

@axeScan()

 async openHomePage() {

 await this.page.goto('https://example.com');

 }

}

When the decorated method is invoked, the following occurs:

1.   The method runs its original logic (e.g., navigating to a page or interacting with elements).
2.   After the main logic completes, the decorator triggers an automated accessibility scan.
3.   Results from the scan are collected and saved to .json file
4.   Control is returned to the test runner after the scan finishes.

This is the core concept — extending Page Object methods with built-in accessibility scans. By running your existing E2E UI test suite, you automatically generate accessibility results for each visited page. Instead of maintaining separate tests that mirror your UI flows, this library integrates accessibility checks directly into them.

**Limitations**

The Page Object Class must contain an object of type `Page`. If you decompose the page and use `Locator` as a base for searching elements, the accessibility scan will be skipped.

**Applicable ✅**`new HomePage(page)`

class HomePage {

 readonly page: Page;
constructor(protected page: Page) {

 this.page = page

 }

@axeScan()

 async openHomePage() {

 await this.page.goto('https://example.com');

 }

}

In tests:

test('User can login and logout', async ({ page }) => {

 const homepage = new HomePage(page);

 await homePage.openHomePage();

 ...

});
**Not-applicable ❌**`new sideMenu(page.locator('#sideMenu')`

export class Table {

 private tableBase: any;
constructor(tableBase: Locator) {

 this.tableBase = tableBase

 }

This approach is used when you want smaller classes representing logical parts or sections of the UI — in other words, when decomposing the Page Object. To retain access to full-page functionality, the solution is to pass the `Page` object as a second parameter.

new sideMenu(page.locator('#sideMenu', this.page)
### Configure scan options

The accessibility environment file `.env.a11y` allows you to customize scan settings. The configuration file offers the following options:

*   enable/disable scanning (default: `on`)
*   custom output directory (default: `axe-playwright-report`)
*   enable/disable screenshot capture (set to `on` to capture screenshots of issues, default: `off`)
*   filter rules by axe-core tags (default: `no filtering, all rules included`)

SCAN=on

OUTPUT_DIR=custom-report-dir

SCREENSHOT=on

TAGS=wcag2a,wcag2aa
### Building the Dashboard Report

Of course, there should be a way to analyse the accessibility results. After tests execution — build the dashboard with the next command and let the magic happen:

npx axe-playwright-report build-report
But I’m not a magician — let me explain what’s happening behind the scenes.

## Get Yevhenii Otsevych’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

One of the main features of building a dashboard relates to the core idea of the Page Object Pattern: reusable methods across multiple tests. This means a single method can be used in many tests, resulting in multiple scans of the same page. That’s expected — running different tests may reveal new accessibility issues even on the same URL.

To avoid displaying duplicates, the build-report command includes a de-duplication algorithm. It compares all reports with the same URL and keeps only the most relevant one, based on:

*   Number of **violations** (must-fix issues)
*   Number of **incomplete** checks (unable to determine automatically — need manual attention)
*   **Timestamp** of the scan

Ta-da-dam! Now you have an understandable report that aggregates all accessibility scans. The report consists of a Dashboard page and Report pages

### Dashboard Page Overview

Press enter or click to view image in full size

![Image 3](https://miro.medium.com/v2/resize:fit:700/1*zgzJfXSxojPGnoaWi3AJww.png)

_Dashboard Page Overview: A centralized view displaying key metrics, system status, and recent activity._

*   Displays the total number of scanned pages and the total issues found
*   Shows severity breakdown: violations, incomplete checks, passes
*   Includes an Impact Distribution Chart for issue severity
*   Includes a Disabilities Affected Chart to show affected user groups
*   Contains a table of reports with columns like page name, URL, violations, and scan data
*   Clicking a report entry opens its detailed Report Page

### Report Page Overview

Press enter or click to view image in full size

![Image 4](https://miro.medium.com/v2/resize:fit:700/1*6gVQFZ1p9O-AZqZ8DLnk2g.png)

Signle Report Page for SauceDemo Inventory Page

*   Shows page title, URL, and browser/device info
*   Provides filtering by impact, standard, and disability
*   Group issues by type (violations, incomplete, inapplicable)
*   Each issue includes rule ID, impact, description, and tags
*   Expandable details include help text, screenshots (if enabled), and affected elements

### A few words about screenshots

Another feature is the ability to take a screenshot of a page, with the ability to highligts problem elements. Since axe-core provides “target” (basically a locator to an element), **axe-playwright-report**can find the element, put it in the red frame, and screenshot it. Slime!

Press enter or click to view image in full size

![Image 5](https://miro.medium.com/v2/resize:fit:700/1*TpeYdfP15Hp6YsUqxW81gA.png)

Issue Detail Section: Screenshot with highligted elements

### **Backward Compatibility with Axe-core/playwright**

If you have existing accessibility tests with axe-core/playwright, you can still use this library to build the dashboard. Just save your existing scan into .json files in the `axe-playwright-report/pages` directory and run the `build-report` command.

### Final Thoughts

Of course, this approach won’t give you 100% accessibility coverage — and that’s okay. The scans only run on pages your UI tests actually touch. But that’s kind of the point: if a page or flow isn’t being scanned, it’s probably not being tested at all.

For me, @axeScan() was a great way to start doing accessibility testing with almost zero extra effort. You get immediate value by scanning everything your UI tests already cover.

And what if, upon reviewing the results, you realize certain pages or flows were not covered? That’s a good thing — it means your E2E coverage needs attention too. Think of it as an accessibility check and a test coverage audit in one.

### Links

github: [https://github.com/eotsevych/axe-playwright-report](https://github.com/eotsevych/axe-playwright-report)

npm: [https://www.npmjs.com/package/axe-playwright-report](https://www.npmjs.com/package/axe-playwright-report)

---

<!-- source: waiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright.md -->

[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

# Waiting on GraphQL: Reliable Handling of In-Flight Requests in Playwright

[![James Kip](https://miro.medium.com/v2/resize:fill:64:64/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---byline--a935129dfad9---------------------------------------)

[James Kip](https://medium.com/@jameskip?source=post_page---byline--a935129dfad9---------------------------------------)

3 min read

·

May 28, 2025

--

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Da935129dfad9&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=---header_actions--a935129dfad9---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

GraphQL-heavy applications often trip up end-to-end tests. Your test clicks a button, but the UI doesn't update in time — likely because the GraphQL request is still in flight. The result? Flaky tests and wasted CI cycles.

To solve this, we built a utility that tracks in-flight GraphQL requests and only proceeds once all of them are done. The key? Wrapping the triggering action and waiting intelligently. Here's how it works.

## 🧨 The Problem

We started running into flaky tests where an action like clicking a button would **navigate away from the current page before all in-flight GraphQL requests had completed**. This usually happened in multi-step flows — like submitting a form and immediately moving to the next screen.

The result: tests would intermittently fail with errors like:

```
Error: Timed out 15000ms waiting for expect(locator).toBeAttached()
```

These failures weren't caused by UI bugs — they were race conditions. Playwright would trigger a navigation or action before all relevant data was fully fetched and rendered from prior GraphQL queries.

Manual `waitForTimeout()` calls weren't reliable, and watching for loading indicators didn't always capture edge cases.

We needed a better way to **block the next step until all GraphQL requests finished** — without relying on brittle assumptions.

## ✅ The Goal

We wanted a reusable function that could:

- Monitor in-flight GraphQL requests
- Wait for all of them to finish
- Clean up after itself
- Stay decoupled from specific operation names

## 🧱 The Solution: `triggerAndWait`

Here's the exact utility we now use in our tests:

```
export const triggerAndWait = async (
  page: Page,
  triggerAction: () => Promise<null | Response> | Promise<void>,
): Promise<void> => {
  const inflightRequests = new Set<Request>()
  let actionTriggered = false
  let resolveAllRequestsFinished: () => void

  const allRequestsFinishedPromise = new Promise<void>((resolve) => {
    resolveAllRequestsFinished = resolve
  })

  const checkIfAllRequestsFinished = () => {
    if (inflightRequests.size === 0 && actionTriggered) {
      resolveAllRequestsFinished()
    }
  }

  const onRequest = (request: Request) => {
    if (actionTriggered && request.url().includes("/graphql")) {
      inflightRequests.add(request)
    }
  }

  const onRequestFinished = (request: Request) => {
    if (inflightRequests.delete(request)) {
      checkIfAllRequestsFinished()
    }
  }

  page.on("request", onRequest)
  page.on("requestfinished", onRequestFinished)
  page.on("requestfailed", onRequestFinished)

  actionTriggered = true

  await triggerAction()

  await allRequestsFinishedPromise

  page.off("request", onRequest)
  page.off("requestfinished", onRequestFinished)
  page.off("requestfailed", onRequestFinished)
}
```

## 🧪 Example Usage

Instead of doing this:

```
await page.getByRole("button", { name: "Submit" })
await page.getByRole("button", { name: "Next" }))

await expect(page.getByText("Success")).toBeVisible()
```

We now write:

```
await triggerAndWait(page, () => page.getByRole("button", { name: "Submit" }))
await page.getByRole("button", { name: "Next" })

await expect(page.getByText("Success")).toBeVisible()
```

This ensures Playwright doesn't move forward until **every** GraphQL request triggered by that action has finished.

## 🧼 Why It Works

This approach is:

- **Generic** — It doesn't rely on hardcoding operation names
- **Precise** — It tracks only requests fired _after_ the action begins
- **Self-cleaning** — It removes all event listeners after execution
- **Reusable** — Works across forms, modals, navigation, and complex workflows

We've seen a significant drop in flaky failures and more reliable CI runs since adopting this pattern.

## 🧠 Bonus Tip: Chain More Assertions

This utility doesn't just fix the first flake — it also stabilizes any downstream assertions:

```
await triggerAndWait(page, () =>
  Promise.all([\
    page.getByRole("button", { name: "Next" }).click()\
    page.getByRole("button", { name: "Submit" }).click()\
  ])
)
```

## 🚀 Takeaways

- Avoid relying on `waitForTimeout()` or loading indicators alone when testing async-heavy apps.
- Track the network layer directly to synchronize test actions with actual app behavior.
- `triggerAndWait` gives you reliability without coupling your tests to brittle frontend assumptions.

If you're struggling with GraphQL race conditions in Playwright, this pattern might save you hours of debugging — and dozens of reruns in CI.

[QA](https://medium.com/tag/qa?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Playwright Test](https://medium.com/tag/playwright-test?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Test Automation](https://medium.com/tag/test-automation?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Software Development](https://medium.com/tag/software-development?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[![James Kip](https://miro.medium.com/v2/resize:fill:96:96/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[![James Kip](https://miro.medium.com/v2/resize:fill:128:128/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[**Written by James Kip**](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[42 followers](https://medium.com/@jameskip/followers?source=post_page---post_author_info--a935129dfad9---------------------------------------)

· [15 following](https://medium.com/@jameskip/following?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[jmekip.com](http://jmekip.com/)

---

<!-- source: centralized-playwright-api-logs-with-grafana-loki.md -->

Title: Centralized Playwright API Logs with Grafana Loki

URL Source: https://medium.com/@indraaristya/centralized-playwright-api-logs-with-grafana-loki-368c21a76aba

Published Time: 2025-05-27T15:05:10Z

Markdown Content:
[![Image 1: Indra A.](https://miro.medium.com/v2/resize:fill:32:32/1*4VPmSAG7sh3epRJiHnmJcA.jpeg)](https://medium.com/@indraaristya?source=post_page---byline--368c21a76aba---------------------------------------)

7 min read

May 27, 2025

Playwright is one of a powerful tool for end-to-end and API testing — but sometimes the scenarios we’ve implement were failing in the CI pipeline and understanding _why_ they failed can be frustrating. What if you could log every request and response to a central, searchable system like Grafana Loki? In this post, I’ll show you how to integrate Loki logging into your Playwright API tests, so you can debug faster without increase your test duration.

### Why Log API Test Data?

API test often failed due to several reasons, such as invalid auth header, body request is incorrect, or the flaky/error on the backend itself. When we run the test in local device one-by-one scenario, it will be easy to track if any error happened from the API. But, what if the test was running in pipeline, then implement parallelization which makes it harder to debug? This is why log API test data is needed.

By logging the API test data, it supposed to help us to know and understand what the error is; so we can fix the issue faster.

### Setup Loki and Grafana

Loki is a log aggregation system designed to store and query logs from any applications. It was started by Grafana Labs so it will work well with Grafana — which the tools to visualize and querying the logs that push to Loki. Before we update the logger in the automation test, let’s setup the Loki and Grafana.

We will using Docker to _up_ the Loki and Grafana; they already provide the sample of `docker-compose` in the [documentation](https://grafana.com/docs/loki/latest/setup/install/docker/).

version: '3'
services:

 loki:

 image: grafana/loki:2.9.4

 ports:

 - "3100:3100"

 command: -config.file=/etc/loki/local-config.yaml

grafana:

 image: grafana/grafana:10.3.3

 ports:

 - "3000:3000"

 environment:

 - GF_SECURITY_ADMIN_USER=admin

 - GF_SECURITY_ADMIN_PASSWORD=admin

 depends_on:

 - loki

Create `docker-compose.yml` file, to create & start the Loki and Grafana run `docker compose up -d`command. Both of the containers will be running in detached mode and ready to get your log.

Press enter or click to view image in full size

![Image 2](https://miro.medium.com/v2/resize:fit:700/1*G7dZWO2dVunvpaQnZMEGVg.png)

Add New Datasource in Grafana

After that, open _localhost:3000_ in browser and login using the username and password defined in the compose file. Go to Connection → Data Source and choose Add New Connection. Look for **Loki**as the source, and add [_http://loki:3100_](http://loki:3100/) in the URL.

Please note that the _loki_ in the URL is the docker container name that has been set in the `docker-compose.yml` file. So it can be different if you change the name of it.

### Create Logging Helper

In this sample, I will be using my own Playwright portfolios sample. You can find the project in this repository.

First, let’s create the function to log the API request. Previously, in each scenario I am using `request` default function from Playwright. So, the logging function is need to be defined in every scenario one by one which cause a repetitive tasks and code. In this case, I would love to create the logging helper and combined with the sending request function, so we did not need to define the logging function explicitly.

const pendingLogs: Promise<void>[] = [];
export function enqueueLog(promise: Promise<void>) {

 pendingLogs.push(promise);

}

export async function flushLogs() {

 await Promise.allSettled(pendingLogs);

 pendingLogs.length = 0;

}

import axios from 'axios';

import { request, APIRequestContext, APIResponse } from '@playwright/test';

import { enqueueLog } from './logQueue';
interface LokiLabels {

 [key: string]: string;

}

interface RequestData {

 method: string,

 url: string;

 headers: Record<string, string>;

 body?: any;

}

interface ResponseData {

 status: number;

 body?: any;

}

function removeAuthOnHeader(headers: Record<string, string>): Record<string, string> {

 const redactedKeys = ['authorization', 'api-key', 'token'];

 const sanitized: Record<string, string> = {};

for (const key in headers) {

 sanitized[key] = redactedKeys.includes(key.toLowerCase())

 ? '[REDACTED]'

 : headers[key];

 }

return sanitized;

}

function removeAuthOnBody(body: any): any {

 if (!body || typeof body !== 'object') return body;

const redactedKeys = ['password', 'token', 'apikey', 'secret'];

 return Object.fromEntries(

 Object.entries(body).map(([key, value]) => [

 key,

 redactedKeys.includes(key.toLowerCase()) ? '[REDACTED]' : value

 ])

 );

}

export async function logApiCallToLoki(

 requestData: RequestData,

 responseData: ResponseData,

 tags: LokiLabels,

): Promise<void> {

 const lokiUrl = 'http://localhost:3100/loki/api/v1/push';

 const timestamp = Date.now() * 1_000_000;

const logData = {

 request: {

 method: requestData.method,

 url: requestData.url,

 headers: removeAuthOnHeader(requestData.headers),

 body: removeAuthOnBody(requestData.body)

 },

 response: {

 status: responseData.status,

 body: removeAuthOnBody(responseData.body)

 }

 };

const payload = {

 streams: [

 {

 stream: {

 job: process.env.JOB_NAME || 'local',

 environment: tags.environment ?? 'dev',

 ...tags

 },

 values: [

 [timestamp.toString(), JSON.stringify(logData)]

 ]

 }

 ]

 };

try {

 await axios.post(lokiUrl, payload);

 } catch (err: any) {

 console.error('Loki logging error:', err.message);

 }

}

export async function createLoggedApiContext(): Promise<APIRequestContext> {

 const apiContext = await request.newContext();

const originalFetch = apiContext.fetch;

apiContext.fetch = async function (

 url: string,

 options?: Parameters<APIRequestContext['fetch']>[1]

 ): Promise<APIResponse> {

 const reqData = {

 method: options?.method || 'GET',

 url: url,

 headers: options?.headers,

 body: options?.data || {}

 }

const response = await originalFetch.call(this, url, options);

const responseData = {

 status: response.status(),

 }

try {

 responseData['body'] = await response.json()

 } catch {

 responseData['body'] = {}

 }

const logging = logApiCallToLoki(reqData, responseData, {

 environment: process.env.ENV || 'local',

 }).catch((e) => {

 console.warn('Loki failed to log the error:', e.message);

 });

 enqueueLog(logging);

 return response;

 };

return apiContext;

}

Above are the queue and the request function. As we can see in `createLoggedApiContext` function, we are creating new context of request, save the request and response data, and also send the test API request. In this function we also modify any sensitive data such as API key and auth token to be updated and changed as **REDACTED** so that the auth not visible in the Loki logging.

## Get Indra A.’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

The logging also support to add _tag_ or _labels_ in every log. We can modify these labels based on our need; and as the sample I am using job name and environment to be added. We can set `JOB_NAME` and `ENV` value in the environment variable of the test.

The logging method was runs in the background without delaying the test, and we implement the queueing promises as well to prevent the teardown process done while the logging still sending to Loki (because we did not await the push process).

import { flushLogs } from "../utils/logQueue";
async function globalTeardown() {

 console.log("Make sure all logs were sent to Loki")

 await flushLogs();

}

export default globalTeardown;

To make sure the queue is flushed — which means all logs has been sent to the Loki and no one in queue — we need to create a global teardown function as seen as in the code above. The global teardown function will make sure to _await_ until all logs sent to Loki. Set the global teardown in the `playwright.config.ts` file.

### Implement Logging in Playwright Test

The last part, after we made the logging helper and global teardown above, we need to implement the helper function in the test.

import { test, expect } from '@playwright/test';

...

test('TAPI0006,TAPI0007-Success to create new users with valid data and Authorization header', async ({ request }) => {

 const body = await createUser('valid');
const response = await request.post(`/public/v2/users`, { headers: header, data: body }) 

 const responseJson = await response.json()

 expect(response.status()).toBe(201)

 expect(responseJson['name']).toEqual(body['name'])

 expect(responseJson['email']).toEqual(body['email'])

 expect(responseJson['gender']).toEqual(body['gender'])

 expect(responseJson['status']).toEqual(body['status'])

 expect(await validateJsonSchema('user', responseJson))

 });

Previously, the API test was implemented as the code above. The request function from Playwright is directly used to do the API request.

Now, we need to _import_ the `createLoggedApiContext` and use it in the test as follows.

import { test, expect } from '@playwright/test';

import { createLoggedApiContext } from '../../utils/lokiLogger';

...

test('TAPI0006,TAPI0007-Success to create new users with valid data and Authorization header', async ({ }) => {

 const body = await createUser('valid');
const request = await createLoggedApiContext();

 const response = await request.post(`/public/v2/users`, { headers: header, data: body }) 

 const responseJson = await response.json()

 expect(response.status()).toBe(201)

 expect(responseJson['name']).toEqual(body['name'])

 expect(responseJson['email']).toEqual(body['email'])

 expect(responseJson['gender']).toEqual(body['gender'])

 expect(responseJson['status']).toEqual(body['status'])

 expect(await validateJsonSchema('user', responseJson))

 });

By _import_ and use the helper function we made before, it will preparing the payload to be sent to Loki and do our request. After the request finished, the log payload will be push to the Loki using HTTP request.

### Visualize in Grafana

The log data will be able to seen in Grafana once the test was executed. It is possible to see the log in _Explore_ or create your own _Dashboard_.

Press enter or click to view image in full size

![Image 3](https://miro.medium.com/v2/resize:fit:700/1*9wg4j-5c53BI-6Kw3hKb_g.png)

Sample Log Data sent to Loki

Screenshot above show us all the API test data that has been sent to Loki. By this, it should be easier for us to check the API response error and understand what is happening to the test.

I only sent the job name and environment name as a tag, so it can be useful for the filtering. You could modify the logging helper to add more tag as the metadata that could help you to identify and analyze the logs, such as add the test case ID, build number, build version, and else.

### Conclusion

Debugging without visibility is painful. By integrating **Grafana Loki** with your **Playwright API tests**, you gain centralized, structured, and searchable logs without adding friction or latency to your test suite.

Whether you’re running tests in local device or in CI, this approach helps you catch issues faster, investigate failures, and build more observable testing pipelines. Now your tests don’t just pass.

Thank you and happy testing!

---

<!-- source: the-green-report-offline-but-not-broken-testing-cached-data-with-playwright.md -->

### Offline but Not Broken: Testing Cached Data with Playwright

Modern web applications are expected to work even when the network doesn't. Whether it's a flaky connection or complete offline mode, users should still see relevant content thanks to technologies like Service Workers and the Cache API. In this post, we'll walk through a simple demo app that uses cached data to stay functional offline, and show how to write automated Playwright tests to verify that behavior — including both success and failure scenarios.

#### Demo App Overview

To demonstrate offline behavior, we've built a minimal static web app consisting of three files: index.html, data.json, and a Service Worker script (sw.js). When the page loads, it fetches text from data.json and displays it on the screen. If the network request fails, it shows a fallback message instead.

The Service Worker is responsible for caching both the HTML and JSON files after the first successful visit. This setup allows us to test whether the app can still show previously loaded content when offline.

When online, it fetches "Hello from the network!" from data.json. When offline and cached correctly, the same message should appear, served from the browser cache.

#### Caching Test: Showing Data When Offline

To verify that our app displays cached data when offline, we'll walk through a Playwright test that simulates a typical user journey: visiting the site online, caching the content, and revisiting it later without a network connection.

We begin by ensuring the browser context is online and navigating to the app:

```javascript
await context.setOffline(false);
await page.goto("your-page-url");
```

Once the page loads, we check that the text from data.json is displayed correctly:

```javascript
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

This confirms that the app fetched and rendered the network data as expected. Next, we reload the page to give the Service Worker a chance to cache the content for offline use:

```javascript
await page.reload();
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

We verify the same data appears again, ensuring that the reload didn't break anything and likely triggered the caching process. Now we simulate going offline using Playwright's built-in API:

```javascript
await context.setOffline(true);
```

This mimics a real-world situation where the user loses internet access. With the connection disabled, we reload the page. Since there's no network, we expect the Service Worker to serve cached data:

```javascript
await page.reload();
```

Finally, we assert that the same content is still visible, even without internet access:

```javascript
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

#### Failure Test: No Data on First Visit Without Network

While caching can keep the app functional offline, it's important to verify that data is not magically available if the app is visited for the first time without internet access. In this test, we simulate a failed request to data.json and expect the app to show an appropriate fallback message.

We start with a fresh browser context to ensure there's no prior cache or service worker from earlier sessions:

```javascript
const context = await browser.newContext();
const page = await context.newPage();
```

Before loading the page, we intercept the request to data.json and abort it. This simulates a network failure:

```javascript
await page.route("**/data.json", (route) => route.abort());
```

This means the app won't be able to retrieve the JSON data, and there's no chance to cache it either. We then navigate to the same app page. Because we intercepted the JSON request, the data won't load:

```javascript
await page.goto("your-page-url");
```

Since the data fetch failed, the app should display a fallback message — not the actual content from data.json. We assert that:

```javascript
await expect(page.locator("#data")).toHaveText("Failed to load data");
```

#### Conclusion

Testing how our web app behaves in offline conditions is crucial for delivering a resilient and user-friendly experience. In this post, we demonstrated how to verify two key scenarios using Playwright:

- That cached data is displayed correctly when revisiting the app offline.
- That the app shows an appropriate fallback when data is unavailable and has never been cached.

These tests ensure that our Service Worker and caching logic work as intended, helping our app gracefully handle real-world issues like network interruptions or offline usage. Even a simple static site can benefit from automated offline tests—giving our users confidence that our app will always have their back, online or not.

You can find the full source code and demo application on our GitHub repository. Until next time!

---

<!-- source: pixel-by-pixel-visual-testing-using-playwright-with-github-actions.md -->

[Sitemap](https://pradappandiyan.medium.com/sitemap/sitemap.xml)

# Pixel-by-Pixel Visual Testing Using Playwright with GitHub Actions

UI bugs are sneaky — a one-pixel shift, a color mismatch, or a disappearing icon. Visual testing helps catch what functional tests miss. In this tutorial, you'll learn how to use **Playwright + Pixelmatch** to compare two screenshots pixel-by-pixel and detect UI regressions — complete with working code and GitHub Actions integration.

## Prerequisites

- Node.js ≥ 16
- GitHub repository
- Basic knowledge of Playwright

## Step 1: Set up the Project

Create a new project folder:

```
mkdir playwright-pixelmatch && cd playwright-pixelmatch
npm init -y
npm install playwright pixelmatch pngjs fs-extra --save-dev
npx playwright install
```

Update your package.json:

```
{
  "name": "playwright-pixelmatch",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "test": "npx playwright test",
    "compare": "node compare-images.mjs"
  }
}
```

## Folder Structure

```
playwright-pixelmatch/
├── screenshots/
│   ├── baseline/
│   ├── actual/
│   └── diff/
├── tests/
│   └── visual.spec.js
├── compare-images.mjs
├── package.json
```

## Step 2: Capture Screenshots in Playwright

Create tests/visual.spec.js with screenshot capture logic comparing actual vs baseline.

## Step 3: Compare Screenshots with Pixelmatch

Create compare-images.mjs using pixelmatch with threshold 0.15.

## Step 4: Automate with GitHub Actions

Create .github/workflows/visual-test.yml to run tests, compare screenshots, and upload diff artifacts.

## Conclusion

Pixelmatch + Playwright provides a powerful visual testing workflow with pixel-level precision. By integrating into CI with GitHub Actions, you can catch UI changes before users do.

I have created a project on GitHub: https://github.com/pradapjackie/playwright-visual-testing

---

<!-- source: the-green-report-supercharging-playwright-tests-with-chrome-devtools-protocol.md -->

### Supercharging Playwright Tests with Chrome DevTools Protocol

When using Playwright for test automation, most QA engineers interact with the browser just like a real user would — clicking buttons, filling forms, and validating UI elements. But under the hood, modern browsers offer much more control through powerful developer protocols.

In this post, we'll explore how we can take advantage of Chrome DevTools Protocol (CDP) with Playwright to unlock advanced testing features. From blocking resources to capturing browser logs, CDP gives us deep access to the browser's internals — and yes, it's all automatable!

#### What is CDP?

The Chrome DevTools Protocol is a set of low-level APIs used by the Chrome DevTools itself to inspect and control the browser. CDP allows developers and QA engineers to perform advanced operations like:

- Intercepting and modifying network traffic
- Emulating devices and network conditions
- Capturing performance metrics
- Listening to console logs or DOM events

Playwright offers a way to hook into CDP for Chromium-based browsers, allowing us to blend high-level and low-level automation seamlessly.

#### Blocking Images to Speed Up Tests

In many UI tests, images are not critical to test logic—they're often decorative or content placeholders. By blocking image requests during test execution, we can significantly reduce page load times.

```python
from playwright.sync_api import sync_playwright

def test_block_images():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()
        client = context.new_cdp_session(page)
        client.send("Network.enable")
        client.send("Network.setBlockedURLs", {
            "urls": ["*.png", "*.jpg", "*.jpeg", "*.gif"]
        })
        page.goto("https://example.com")
        assert "Example Domain" in page.inner_text("body")
```

#### Capturing Console Logs via CDP

```python
client.send("Log.enable")

def handle_log_entry(params):
    level = params["entry"]["level"]
    text = params["entry"]["text"]
    print(f"Console: {level.upper()} - {text}")

client.on("Log.entryAdded", handle_log_entry)
page.goto("https://example.com")
page.evaluate("console.log('Test log message')")
page.evaluate("console.warn('This is a warning log!')")
page.evaluate("console.error('This is an error log!')")
page.wait_for_timeout(1000)
```

#### Simulating Slow Network Conditions via CDP

```python
client.send("Network.enable")
client.send("Network.emulateNetworkConditions", {
    "offline": False,
    "latency": 200,
    "downloadThroughput": 50000,
    "uploadThroughput": 20000
})
page.goto("https://example.com")
page.wait_for_timeout(5000)
```

#### Conclusion

With CDP, our test automation can go beyond the surface and truly test how our web app behaves under various conditions — not just how it looks. From intercepting requests to inspecting browser logs, CDP empowers QA engineers to write more robust, performant, and realistic tests.

If you're already using Playwright, CDP is just a method call away. So next time you're writing a test, ask yourself: Is there a lower-level insight I can automate? — and let CDP help you get there.

You can find the complete code examples from this post on our GitHub page, ready to explore and adapt to your own test scenarios. Thanks for reading, and happy testing!

---

<!-- source: part-2-count-me-out-assert-me-wrong-two-sneaky-playwright-pitfalls.md -->

Title: 🎭 Part 2 — Count Me Out & Assert Me Wrong: Two Sneaky Playwright Pitfalls

URL Source: https://0xislamtaha.medium.com/part-2-count-me-out-assert-me-wrong-two-sneaky-playwright-pitfalls-5b53639f645f

Published Time: 2025-05-13T20:29:57Z

Markdown Content:
[![Image 1: Islam Taha](https://miro.medium.com/v2/resize:fill:32:32/1*9h2SqBzmGUKR0jYi0GNb4g.jpeg)](https://0xislamtaha.medium.com/?source=post_page---byline--5b53639f645f---------------------------------------)

3 min read

May 13, 2025

Press enter or click to view image in full size

![Image 2](https://miro.medium.com/v2/resize:fit:700/1*7mnV35aP8jRwcU8Y_dyaAA.jpeg)

Hey friends! 👋 Remember how we talked about relying on our **e2e tests** and **synthetic tests** as quality gates for daily releases in [part #1](https://medium.com/@0xislamtaha/fighting-false-alerts-in-our-playwright-test-suite-my-battle-for-stability-%EF%B8%8F-part-1-6a6802fa38de)? Well, here’s the hard truth: **Stability doesn’t come for free.** 💸

We had to put in _a ton_ of work to make sure our test suites weren’t crying wolf every other day. Because let’s be real — **nobody** wants to be dragged into a 🔥 _grilled_ 🔥 call just because a flaky test blocked half the company’s release pipeline! (And yes, the _false alerts_ cost? _Painful._ 💔)

So, **mission clear:** Fail **only** when there’s a _real_ issue — not because of some sneaky test instability.

## Misunderstood Playwright Behaviors!🤯

Here’s the kicker — some of our flakiness came from **misreading how Playwright’s functions actually work.** And honestly? It’s _so easy_ to get tripped up, especially when function names are… _a little misleading._ 😅

## 🔍 Scenario #1: The `.count()` Deception

**Problem:** We needed to get the number of items on a page. Simple, right? First instinct?

let totalCount = page.locator('[data-testid="listItems"]').count();
**False sense of security:**“Hmm, maybe it’s async?” So we `await`it:

let totalCount = await page.locator('[data-testid="listItems"]').count();
**But… surprise!** 🎭 `.count()`**does NOT wait for all elements to load.**

*   It returns **immediately** with whatever’s in the DOM _at that moment_.
*   If your list is still loading? **Boom.** Flaky count.

**Our reaction:** 😱 _“Wait, WHAT?!”_ After digging through docs (and [GitHub](https://github.com/microsoft/playwright/issues/14278)issues), we realized:

> `.count()`**_is not your patient friend. It’s that one colleague who replies ‘Done!’ before actually checking._**

## 🚨 The `.all()` Trap (Plot Twist!)

_“Okay, fine! What about_`.all()`_? That should wait for everything, right?"_

**NOPE.** 🙅‍♂️

*   **Same issue!**`.all()` also **doesn’t wait** for all elements to exist.
*   It just grabs what’s available _right now_ and gives you the result.

## 💡 The Fix? Wait for Stability!

Instead of blindly trusting `.count()` or `.all()`, we **forced stability** by:

*   **Waiting for a specific count** (e.g., `await expect(await page.locator("[data-testId='listItems']")).toHaveCount(5)`).
*   **Or, even better — waiting for _at least_ some items** (e.g.,`await expect(await page.locator("[data-testId='listItems']").count()).toBeGreaterThanOrEqual(5)`).

## “Assertions Are Straightforward, Right?” (Spoiler: NOPE.)

You’d _think_ assertions are the _least_ confusing part of testing. **WRONG.** Playwright has _two_ types of assertions, and if you mix them up? **Hello, flakiness my old friend.** 😭

## Get Islam Taha’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

Here’s the kicker:

1.   **Auto-Retry Assertions** — The patient heroes. They keep trying until success (or timeout). _And they return promises!_
2.   **Non-Retry Assertions** — The impatient ones. They pass/fail **immediately**. _No promises, no retries, no mercy._

## 💥 Scenario #2: The Deadly Assertion Mix-Up

We _thought_ we fixed our `.count()` problem with:

expect(await locator.count()).toBeGreaterThan(5);
**BUT SURPRISE!** 🎭

*   `.count()`**doesn’t wait** (we know this now).
*   `.toBeGreaterThan()` is a **non-retry assertion**—so it also **doesn’t wait!**

**Result?** A flaky mess. Again. 🤦‍♂️

## 🚨 The Real Problem: Sync + Async = Chaos

We needed:

*   **Get the count** (but _wait_ for stability).
*   **Assert the count** (but _keep retrying_ until valid).
*   **Timeout gracefully** (no infinite loops, please).

**Translation:** We needed to **convert a non-retry assertion into an auto-retrying one!**

## 💡 The Fix? `expect.toPass()` to the Rescue!

Playwright gives us **two magic tools** for this:

*   `expect.toPass()` – Wraps _any_ assertion in a retry loop.
*   `expect.poll()` – Lets you poll custom logic with retries.

### Here’s what saved us:

await expect(async () => {

 expect(await page.locator('[data-testid="listItems"]').count())

 .toBeGreaterThan(5);

}).toPass({ timeout: 2 * 60 * 1000 });
**What’s happening here?**✅ Retries the entire block until it passes (or hits timeout).

✅ No more flakiness from partial loads!

✅ Explicit timeout control (because defaults won’t save you).

**Tags:** Playwright, Testing, E2E Testing, Test Automation, Flaky Tests, Software Quality, Web Development

---

<!-- source: speeding-up-playwright-tests-with-dynamic-sharding-in-github-actions-by-lewis-ne.md -->

# Speeding Up Playwright Tests with Dynamic Sharding in GitHub Actions

Running end-to-end (E2E) tests with Playwright works well out of the box. But as your test suite grows, CI runtimes tend to grow with it. Splitting test execution across shards is a common way to speed things up, but hardcoding shard counts quickly becomes inflexible.

In this post, I'll walk through how I implemented **dynamic test sharding for Playwright in GitHub Actions**.

## Why Dynamic Sharding?

Playwright supports sharding via its `--shard` CLI flag:

```
npx playwright test --shard=1/3
```

With dynamic sharding, we:

1. Count the number of tests
2. Compute how many shards are needed (e.g., 40 tests per shard)
3. Run tests in parallel using GitHub Actions' matrix strategy

## GitHub Actions Setup

The workflow is structured around five main jobs:

1. Generate the dynamic matrix
2. Build the app once
3. Run tests in shards
4. Merge the reports
5. Clean up intermediate artifacts

## 1. Generate the Test Shards Matrix

```
- name: Get Total Number of Tests
  run: |
    TEST_LIST_OUTPUT=$(pnpm test --list)
    TOTAL_TESTS=$(echo "$TEST_LIST_OUTPUT" | grep 'Total:' | awk '{print $2}')
    echo "TOTAL_TESTS=$TOTAL_TESTS" >> "$GITHUB_ENV"

- name: Total shards
  run: |
    SHARD_COUNT=$(( (TOTAL_TESTS + 39) / 40 ))
    echo "SHARD_COUNT=$SHARD_COUNT" >> "$GITHUB_ENV"
```

## 2. Build Once, Use Many Times

Build once and upload artifact `.next` for all shards to download.

## 3. Run Sharded Tests in Parallel

Matrix strategy runs `pnpm test --shard="$SHARD_INDEX/$TOTAL_SHARDS"` per job.

## 4. Merge Reports

`pnpm playwright merge-reports --reporter html ./blob-reports`

## 5. Clean Up

Remove temporary artifacts with geekyeggo/delete-artifact.

## Benefits

- Faster CI cycles through real parallelism
- Dynamic scaling without hardcoding values
- Accurate test reports merged into a single HTML view

## TLDR; Final solution includes generate-shards-matrix, build, test matrix, merge-reports, and cleanup jobs with full YAML in the article.

---

<!-- source: functional-page-model-for-playwright-a-scalable-alternative-to-classic-pom-by-ja.md -->

# Functional Page Model for Playwright: A Scalable Alternative to Classic POM

The traditional Page Object Model (POM) has served as the de facto standard for structuring end-to-end test automation. Enter the **Functional Page Model (FPM)** — a modular, functional approach to organizing Playwright tests.

## What Is the Functional Page Model?

The Functional Page Model (FPM) breaks down test modules into **four separate components**:

- `component.actions.ts`: Contains reusable user interaction methods.
- `component.locators.ts`: Encapsulates all UI element selectors.
- `component.data.ts`: Manages test data and fixtures.
- `component.spec.ts`: Contains actual test definitions.

## The Structure

```
e2e/tests/clients/vitals/
    ├── vitals.actions.ts
    ├── vitals.locators.ts
    ├── vitals.data.ts
    └── vitals.spec.ts
```

## Component Breakdown

### Actions

```
export const createClinicalNote = async (page: Page, noteInput: NoteData) => {
  await Locators.chartNewBtn(page).click();
  await Locators.noteBtn(page).click();
  await Locators.noteInput(page).fill(noteInput.text);
  await Locators.saveBtn(page).click();
};
```

### Locators, Data, Specs

Specs wire together actions, data, and locators with `test()` blocks.

## Advantages

- Clear separation of concerns
- Improved reusability (pure functions)
- Easier onboarding
- Scalable for large teams

## Disadvantages

- Fragmented file structure
- Less familiar to traditional QA engineers
- Slight overhead for simple flows

## Final Thoughts

FPM trades class-based structures for composable functions — worth a serious look for growing test suites.

---

<!-- source: playwright-custom-matcher-to-automate-layout-testing.md -->

# Using Playwright Custom Matchers to Automate Layout Testing

Layout testing is the test of a web page's components — buttons, input boxes, radio buttons, text labels etc.

## What causes a layout failure

- CSS styles may conflict while targeting the same element
- Layout changes on viewport change happen due to media queries in CSS
- Mixed use of third party components may cause issues

## Existing Tools

### Visual Regression Tools

Applitools Eyes, BackstopJS, ImageMagick compare — pixel comparison adds storage and runtime cost.

### Galen

Galen describes layout with relative positioning syntax like `comments: width 300px; inside screen 10 to 30px top right`.

## Playwright Custom Matchers

Extend assertions with `expect.extend()` for Galen-style layout checks:

```
await expect(playwrightDev.writingTestsNavLink).toBeLeftOf(
  playwrightDev.installationPageHeader,
);
await expect(playwrightDev.writingTestsNavLink).toBeAbove(
  playwrightDev.supportedLanguagesNavLink,
);
```

Custom matchers like `toBeLeftOf` compare bounding boxes. Full code at github.com/hrmeetsingh/playwright-layout-testing.

Layout tests can run first in the suite — if UI is broken, skip functional tests. Viewport-specific assertions supported via conditional logic.

Thanks for reading!!

---

<!-- source: request-mocking-protocol-a-new-approach-to-mocking-server-side-requests-in-e2e-t.md -->

In modern web development, end-to-end testing is essential for ensuring that applications behave as expected. When using new features like [React Server Components](https://react.dev/reference/rsc/server-components), we often rely on third-party APIs to fetch data on the server. While this approach offers significant benefits for performance and scalability, it can also introduce challenges for testing. Live API responses may change over time, causing tests to fail even when the application logic remains correct.

In this article, I'll introduce a new approach to server-side API mocking that makes tests fast and reliable with minimal setup. As a tech stack, I will use [Playwright](https://playwright.dev/) and [Next.js](https://nextjs.org/), though the method works with any framework or test runner. Let’s dive in!

## The Problem: Testing Server-Side Data Fetching

Consider the following server component, that fetches and renders a list of users from a third-party API:

```jsx
export async function UserList() {
  const res = await fetch('https://jsonplaceholder.typicode.com/users');
  const users = await res.json();

  return (
    <ul>
      {users.map((user) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```
When browser requests this page, the server performs a subsequent call to `/users` API and returns rendered HTML:

![The flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/ojp4ymqzbg87r8pd44f7.png)

The page in the browser:

![Rendered User list](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/aponbpmn0wwidq8si4cw.png)

A basic Playwright test might look like this:

```js
test('show user list', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('listitem').first()).toHaveText('Leanne Graham');
});
```

This test passes when the first item in the API response is **Leanne Graham**. But what happens if the API returns a different order or data? The test will fail even though the application itself is functioning correctly.

Example of failed test, when elements are returned in a reversed order:

![Failed test](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/gw2o7yu96g80zqdqhs5s.png)

The test would be more reliable, if it could mock the `GET /users` request and provide a static list of users: 

![Server-side mock](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/2o7dpja2sodjx044e7k6.png)

When requests are made from the browser context, Playwright's [API mocking](https://playwright.dev/docs/mock) feature works fine. However, this approach doesn't intercept server-side requests.

## Existing Approaches

Several efforts have been made to address this challenge:

### Playwright Proxy Approach

An in-progress [pull request](https://github.com/microsoft/playwright/pull/34520) in the Playwright repo introduces server-side mocking by running an HTTP proxy alongside the test process. The app is configured to route outgoing requests through this proxy. If a request matches a specified URL pattern, a user-defined handler applies the mock response. 

![Mock with HTTP proxy](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/qbwx33c91rywzo6cbcak.png)

While this approach is highly flexible, it also presents challenges. For instance, if your app is deployed on platforms like Vercel and tests run in GitHub workflows, setting up a tunnel to connect the app to the proxy can be complex and error-prone.

### Mock Service Worker (MSW)

[MSW](https://mswjs.io/) is a popular tool for mocking HTTP requests. It is also working on server-side mocking support, as seen in this [pull request](https://github.com/mswjs/msw/pull/1617). Instead of using an HTTP proxy, MSW relies on WebSockets as a transport layer to address connectivity issues:

![MSW flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/zwa5469rm7wa461pk0ms.png)

However, this approach has its own limitations, as noted in the pull request:  

> You cannot have multiple tests that override request handlers for the same app at the same time.  

This means that tests with server-side mocks cannot run in parallel, which is a major drawback for end-to-end testing.  

Overall, existing approaches aim to use an arbitrary function as the mocked request handler but introduce connectivity and parallelization challenges.

## Proposed Solution

While experimenting with these solutions, I came to a simpler idea:
 
*What if we pass the mocking data within the navigation request using a custom HTTP header?*

![Mock header flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/srhce45md4t0nyp5ma3a.png)

### How It Works

- **Embed Mock Data:** Instead of routing server-side requests through an external proxy, we encode static mock responses as JSON and attach them in a custom header (e.g., `x-mock-request`).

- **Server-Side Parsing:** On the server, we intercept outgoing API calls, read the custom header, and apply the corresponding mock if the request matches the predefined schema.

This approach solves both the connectivity and parallelization issues:
- There’s no need to set up tunnels or spin a separate proxy server.
- Each test can pass its own mock data via HTTP headers without conflict.

Of course, there are limitations:
1. **Static Data Only:** The mock must be serializable to JSON. This means you can only provide static responses (e.g., `{ status: 200, body: 'Hello' }`) rather than dynamic, function-based mocks.
2. **Header Size Limits:** HTTP headers typically support 4KB to 8KB of data. This approach is best suited for small payloads.

In many real-world scenarios, these limitations are acceptable. Most mocks are lightweight and static, making this a practical solution for ensuring test stability.

## Implementation

Below is a step-by-step guide how to implement this solution with Playwright and Next.js.

### Define Schemas

First, define the schemas for the request and the response. For instance, to mock a server-side GET request to `https://jsonplaceholder.typicode.com/users`, you can set up the following:

Request Schema:
```js
const reqSchema = {
  method: 'GET', 
  url: 'https://jsonplaceholder.typicode.com/users',
};
```

Response Schema:
```js
const resSchema = {
  status: 200,
  body: [
    { id: 1, name: 'John Smith' }
  ]
};
```

Combine Schemas and Build the Header:
```js
const mockSchema = { reqSchema, resSchema };
const mockSchemaString = JSON.stringify(mockSchema);
const headers = {
  'x-mock-request': mockSchemaString
};
```

### Playwright Integration

To attach custom HTTP headers to the navigation request, use Playwright's [page.setExtraHTTPHeaders](https://playwright.dev/docs/api/class-page#page-set-extra-http-headers):

```js
test('show user list', async ({ page }) => {
  await page.setExtraHTTPHeaders({
    'x-mock-request': mockSchemaString
  });

  await page.goto('/');
});
```

With this configuration, every navigation and subsequent request from the page will include the mocking header.

### Handling on the Server

On the server side, the following steps are required:

1. Read incoming headers
2. Get `x-mock-request` value and extract mock schemas
3. Intercept outgoing request
4. Apply mock schemas and return the mocked response

#### Read Incoming Headers and Extract Schemas

To read the incoming headers, you can use Next.js's [`headers()`](https://nextjs.org/docs/app/api-reference/functions/headers) helper. When `x-mock-request` header is found, use `JSON.parse()` to extract mock schemas:
```js
import { headers } from 'next/headers';
 
// ...

const headersList = await headers();
const mockHeader = headersList.get('x-mock-request');
const mockSchemas = JSON.parse(mockHeader);
```

#### Intercept Outgoing Requests

To intercept all outgoing requests in Next.js app, you can overwrite the `globalThis.fetch` function:

```js
const originalFetch = globalThis.fetch;
globalThis.fetch = async (input, init) => {
  // inspect and potentially mock outgoing request
};
```

Inside the intercepted function, you can read the incoming headers and apply the mocks. Full code of the function:

```js
function interceptGlobalFetch() {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    // Read incoming headers and extract mocks
    const headersList = await headers();
    const mockHeader = headersList.get('x-mock-request');
    const mockSchemas = JSON.parse(mockHeader);

    // Match the request against schemas
    const request = new Request(input, init);
    const matchedSchema = mockSchemas.find(schema => matchRequest(request, schema));

    // Return mocked response or make a real request
    return matchedSchema
      ? buildMockedResponse(request, matchedSchema)
      : originalFetch(request)
  };
}
```

The global `fetch` should be instrumented at server startup, before any requests are made. Next.js provides a dedicated file for this task, called [instrumentation.js](https://nextjs.org/docs/app/building-your-application/optimizing/instrumentation): 

```js
// instrumentation.js

export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs' && process.env.NODE_ENV !== 'production') {
    interceptGlobalFetch();
  }
}
```

> **Note:** Interception should be enabled only in the `nodejs` runtime and in non-production environments.

## Testing the Whole Flow

Once the server-side interception is in place, you can run your Playwright test with the server-side mock. Here’s an example:

```js
test('show user list', async ({ page }) => {
  // Set up server-side mock
  await page.setExtraHTTPHeaders({
    'x-mock-request': buildMockHeader()
  });

  // Navigate to the page
  await page.goto('/');

  // Assert page content according to mock data
  await expect(page.getByRole('listitem').first()).toHaveText('John Smith');
});
```

The `buildMockHeader()` helper just combines request and response schemas:
```js
function buildMockHeader() {
  const reqSchema = {
    method: 'GET', 
    url: 'https://jsonplaceholder.typicode.com/users',
  };

  const resSchema = {
    status: 200,
    body: [
      { id: 1, name: 'John Smith' }
    ]
  };

  return JSON.stringify([ { reqSchema, resSchema } ]);
}
```

Running the test:
```
> npx playwright test

Running 1 test using 1 worker
  1 passed (1.3s)
```

The page's screenshot shows a list with a mocked data - a single user `John Smith`:

![Page screenshot](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/5oy0hclxhj0s2gxg3vq0.png)

With such a mock, the test no longer depends on the API response while ensuring that the server component correctly renders the data.

## Wrapping into a Library

To reduce the boilerplate code for server-side mocking, I bundled the functionality into a separate package called [request-mocking-protocol](https://github.com/vitalets/request-mocking-protocol). It hides the implementation details and provides a friendly API for setting up mocks on client and server side.

### Example Usage with the Library

The following example demonstrates how to use the library in a Playwright test:

```ts
test('show user list', async ({ page, mockServerRequest }) => {
  // Set up server-side mock
  await mockServerRequest.GET('https://jsonplaceholder.typicode.com/users', {
    body: [{ id: 1, name: 'John Smith' }],
  });

  // Navigate to the page
  await page.goto('/');

  // Assert page content according to mock data
  await expect(page.getByRole('listitem').first()).toHaveText('John Smith');
});
```

The custom fixture `mockServerRequest` is defined as follows:

```js
import { test as base } from '@playwright/test';
import { MockClient } from 'request-mocking-protocol';

export const test = base.extend({
  mockServerRequest: async ({ context }, use) => {
    const mockClient = new MockClient();
    mockClient.onChange = async (headers) => context.setExtraHTTPHeaders(headers);
    await use(mockClient);
  },
});
```

Under the hood, the library builds the mocking schemas and exposes them as HTTP headers.

On the server, you can set up the interceptor with a single call of `setupFetchInterceptor()`:
```js
// instrumentation.js
import { headers } from 'next/headers';

export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs' && process.env.NODE_ENV !== 'production') {
    const { setupFetchInterceptor } = await import('request-mocking-protocol/fetch');
    setupFetchInterceptor(() => headers());
  }
}
```

## Recap

In this article, I introduced an alternative approach to server-side request mocking that uses HTTP headers to transfer mock data. This setup is simpler because it eliminates the need for additional proxies. Each test carries its own mock data, allowing for parallel execution and improved scalability.

The approach does have some limitations. It only supports static mocks — arbitrary JavaScript functions are not allowed. Additionally, HTTP headers have size limits, making this method best suited for smaller payloads.

Despite these trade-offs, the solution looks promising. I've packaged it into a [library](https://github.com/vitalets/request-mocking-protocol) for easier integration with different frameworks. You are welcome to give it a try and share the feedback.

Thanks for reading, and happy testing ❤️

---

<!-- source: a-practical-look-at-the-object-pool-pattern-for-playwright-tests.md -->

# A Practical Look at the Object Pool Pattern for Playwright Tests

The Object Pool Pattern maintains a set of pre-initialized objects ready for use. Benefits include resource management and parallelization synergy for Playwright tests.

## Why Use the Object Pool Pattern?
- Session Conflicts prevention
- Resource Exhaustion avoidance  
- Pesticide Paradox mitigation
- Data Fermentation handling

## Implementing Object Pool in Playwright
Playwright workers are isolated. Solutions include Lock File Approach and API Server (best).

API endpoints: /acquire and /release. Use Playwright webServer to start API before tests.

## CI Challenges and Horizontal Scaling
Run API server as separate step in GitHub Actions with network accessibility.

Repository: https://github.com/eotsevych/pw-object-pool

---

<!-- source: parallel-testing-with-playwright-how-to-avoid-collisions-and-failures.md -->

Title: Parallel Testing with Playwright: How to Avoid Collisions and Failures (en)

URL Source: https://medium.com/@juanpromanzio/parallel-testing-with-playwright-how-to-avoid-collisions-and-failures-dc89651fc92e

Published Time: 2025-02-24T16:54:26Z

Markdown Content:
[![Image 1: Juan Promanzio](https://miro.medium.com/v2/resize:fill:32:32/1*NrK_otLAQ-XqXnGtrg-69g.jpeg)](https://medium.com/@juanpromanzio?source=post_page---byline--dc89651fc92e---------------------------------------)

4 min read

Feb 24, 2025

Before we begin, let me provide some context. Currently, I’m working on a project with over 500 UI tests written in Playwright and TypeScript. As part of a continuous improvement initiative, we started developing a pipeline to run all tests after each PR is merged into the Staging environment.

The pipeline development was quite straightforward thanks to the use of GitHub Actions, as most of the workflows were reusable. However, the real challenge arose with parallelization: many tests began failing due to collisions, as they were trying to interact with the same components or sections at the same time, even though they had been running in parallel previously.

After a long day of work, I managed to find some resources and apply strategies that allowed me to resolve these issues without the need to serialize the tests. Below, I will share some practical solutions to optimize parallel execution and avoid collisions in Playwright.

The following tips are listed from the most to the least costly in terms of execution time within the pipeline.

### **Retries**

Playwright offers a retry option that allows you to define how many times a test should be re-executed in case of failure. This is the most costly option in terms of time, as it involves restarting the test execution from scratch.

test_suite:

 - --grep “@tag” --workers=2 --retries=3
The retried tests will be marked as flaky in the final report, showing the errors encountered in each attempt. It’s recommended to minimize the presence of tests in this state, as they can significantly increase the pipeline execution time.

## Get Juan Promanzio’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

**_Report example_**

┌───────────────┬───────────────────────┐

│ ├ Tests │ 22 │

│ │ ├ Passed │ 18 (81.8%) │

│ │ ├ Flaky │ 2 (9.1%) │

│ │ ├ Skipped │ 2 (9.1%) │

│ │ └ Failed │ 0 (0.0%) │

│ ├ Steps │ 1212 │

│ ├ Suites │ 22 │

│ │ ├ Projects │ 2 │

│ │ ├ Files │ 22 │

│ │ ├ Describes │ 0 │

│ │ └ Shards │ 0 │

│ ├ Retries │ 3 │

│ ├ Errors │ 6 │

│ ├ Logs │ 33 │

│ ├ Attachments │ 8 │

│ ├ Playwright │ v1.35.1 │

│ ├ Date │ X/XX/XXXX, X:XX:XX. │

│ └ Duration │ Xm XXs │

└───────────────┴───────────────────────┘

### **Serialization with**`describe.serial`

In Playwright, it is possible to run a specific set of tests sequentially using `describe.serial`. This ensures that the tests within that group do not run in parallel, avoiding potential collisions. Below is an example:

import test, { expect } from '@playwright/test';
test.describe.serial('Serialized tests', async () => {

 test('Test 1', async () => {

 expect(true).toBe(true);

 });

 test('Test 2', async () => {

 expect(true).toBe(true);

 });

 test('Test 3', async () => {

 expect(true).toBe(true);

 });

});

This option is less costly than retries, as it simply ensures that certain tests run sequentially instead of restarting them from scratch. It can be applied by identifying the tests that cause conflicts and grouping them within a `describe.serial`, ensuring controlled execution without collisions.

### **Expect.polling: Retries in Assertions**

With `expect.polling`, it is possible to retry an assertion until the expected condition is met. This is useful in situations where a value may take time to update, such as waiting for an HTTP request to return a status 200 or for a table to display a specific number of results.

await test.step('Expect row count should be 10', async () => {

 await expect

 .poll(

 async () => {

 return await page.locator('GetRowAriaRowIndex');

 },

 {

 timeout: 10000,

 intervals: [500],

 },

 )

 .toContain('10');

 });
`Timeout` defines the maximum time to wait for the assertion to pass, while `interval` sets the time between each retry.

Additionally, Playwright offers `expect.toPass`, an alternative that allows retrying entire blocks of code rather than just individual assertions. This is useful when multiple conditions need to be validated within a single attempt.

### **WaitFor: Conditional Wait in Playwright**

The waitFor arguments are very useful when we need to wait for specific locators. One of the areas where they helped me achieve stability in the tests was with loading animations. These are elements that, after a certain amount of time, should no longer be present in the DOM. To handle this, we can use the `detached` option, which waits until an element is removed from the DOM.

Below is an example of the code:

await test.step('Set pagination to 25', async () => {
await page.locator('paginator-ddl').click();

await page.locator('paginator-opt-25').click();

await page.locator('load-icon').waitFor({ state: 'detached' });

});

### **Conclusion**

Optimizing parallel test execution in Playwright is crucial for maintaining the stability and efficiency of CI/CD pipelines. Throughout this article, we’ve explored various strategies to avoid collisions and improve performance, such as using retries, serializing test blocks with `describe.serial`, and applying retries in assertions with `expect.polling`. Additionally, techniques like waits and the `detached` option to handle loading animations have proven essential to ensure our tests run reliably, even in dynamic environments.

By applying these practices, it’s possible to mitigate parallelization issues without sacrificing test speed or reliability. However, it is important to carefully evaluate which strategies to use based on the needs of each project, as some options may come with a higher time cost.

With these tips and tools, you can significantly improve the stability and efficiency of your Playwright tests, ensuring a smoother and more robust continuous integration process.

---

<!-- source: playwright-graphql-revolutionize-graphql-testing-with-auto-generated-type-safe-c.md -->

# Playwright-graphql: Revolutionize GraphQL Testing with Auto-Generated Type-Safe Client

Playwright-graphql creates client SDKs from your GraphQL schema, removing the need to write queries by hand.

## The Problem with String-Based GraphQL Operations

GraphQL servers convert queries to SQL. Input parameters drive backend logic (WHERE conditions); query fields only control response shape.

Testing should validate how input parameters affect backend behaviour: filters, take/skip, sorting, edge cases.

Raw string queries in tests are clunky and error-prone.

## How Playwright-GraphQL Solves This

1. Focus on input parameters
2. Readable tests (what vs how)
3. Type safety via auto-generated types

## Main Feature: Schema-Driven Automation

1. get-graphql-schema pulls endpoint definition
2. gql-generator creates operations
3. GraphQL Codegen makes typed client methods

NPM: playwright-graphql
Template: github.com/DanteUkraine/playwright-graphql-example

Continuation: Setup type safe Playwright-GraphQL client on DEV.to

---

<!-- source: playwright-visual-testing-how-should-things-look.md -->

# Playwright Visual Testing; How Should Things Look?

Using Playwright snapshots with mocked data can significantly improve the speed at which UI regression is carried out across Chromium, Firefox, and Webkit.

## The problem

We needed to test that chart data was visualised correctly with default mocked data and that UI interactions transform charts as expected. We solve non-static data by mocking via Playwright Mock API.

## Playwright snapshotting explained

Playwright captures screenshots and compares each pixel RGBA values to golden reference images. First run creates comparison images; `--update-snapshots` saves golden standards.

```
expect(chartShot).toMatchSnapshot('rio-so2-graph-without-centro.png')
```

Parameters: `maxDiffPixels` and `maxDiffPixelRatio` for tolerance.

## Usage examples

Mock data + snapshots test chart transformations when sensors are removed. ECharts library renders graphs; snapshot testing catches padding/margin regressions across browsers.

## Maintenance

Team coordination required: update snapshots with `--update-snapshots` when UI changes. Use meaningful snapshot names. Use `waitFor({ state: 'visible' })` and network polling before screenshots.

## Efficient mocking

Use `Partial<forecastAPIResponse>` overrides instead of huge static JSON fixtures.

## Conclusion

Playwright snapshot testing is easy to set up with API mocking. Use judiciously in CI; coordinate snapshot maintenance with development.

---

<!-- source: sdet-building-a-simple-configuration-mechanism-for-your-playwright-project-by-ko.md -->

# SDET: Building a simple configuration mechanism for your Playwright project

Playwright itself has a very simple configuration out of the box in the playwright.config file. But what about making it more flexible? What about the possibility to change this configuration depending on the environment and an easy way to read it from any project place?

## Libraries we need to install

npm install dotenv joi

dotenv loads environment variables from a .env file. joi is a schema validation library for validating environment variables.

## Config resolver

Create config.ts with dotenv.config(), Joi validation schema, and a Config class with static readonly validated fields.

## It is time to use it

Import Config in playwright.config.ts and use Config.WORKERS, Config.HEADLESS_BROWSER, Config.BASE_URL, etc.

Use Config.USER_NAME and Config.PASSWORD in page objects and helpers.

---

<!-- source: tip-playwright-trace-viewer-copy-as-playwright-api-request.md -->

# TIP: Playwright Trace Viewer - Copy as Playwright API Request

With the latest 1.50 Playwright release, the Trace Viewer includes a "Copy as Playwright" button on HTTP requests in the Network tab.

## From Playwright Test Report

Set `reporter: "html"` and `trace: "on"` (or `retain-on-failure`) in playwright.config.ts. Run tests, open the HTML report, click into a test, open Traces, select a network request, and click "Copy as Playwright".

## From Playwright UI mode

Run `npx playwright test --ui` and use the same Network tab Copy as Playwright button.

## Why This is Incredible?

This shortcut lets you inspect HTTP requests made through the UI and generate Playwright API request code for test data setup without manually copying headers and bodies.

Use the copied code with `request` fixture to create test data via API, bypassing the UI for faster test setup.

---

<!-- source: organizing-playwright-tests-effectively.md -->

# Organizing Playwright Tests Effectively

When working with end-to-end (E2E) testing in Playwright, maintaining a clean and scalable test suite is crucial. A well-organized structure not only improves maintainability but also makes it easier to onboard new team members. In this post, we'll cover how to best organize your Playwright tests, from folder structures to using hooks, annotations and tags.

## Structuring Your Test Folders

Playwright tests are typically organized within a `tests` folder. You can create multiple levels of sub folders within the `tests` folder to better organize your tests. When dealing with tests that require user authentication, separating tests into logged-in and logged-out states makes the test suite cleaner. Here's an example folder structure:

```
/tests
  /helpers
    - list-test.ts        # custom fixture for a page with list of movies
    - list-utilities.ts       # helper functions for creating lists of movies
  /logged-in
    - api.spec.ts    # API tests for logged-in users
    - login.setup.ts      # Tests for logging in
    - manage-lists.spec.ts  # Tests for managing lists of movies
  /logged-out
    - api.spec.ts # Tests API endpoints for logged-out users
    - auth.spec.ts     # Tests login flow
    - movie-search.spec.ts  # Tests for searching movies
    - sort-by.spec.ts       # Tests for sorting movies
```

## Using Test Hooks

Playwright offers test hooks such as `beforeEach` and `afterEach` to handle common setup and teardown tasks for each test. These hooks are particularly useful for actions like logging in, initializing test data, or navigating to a specific page before each test.

Helper functions can help you avoid code duplication and keep your tests DRY. Fixtures can be used instead of a `beforeEach` hook and are a great way of creating a page context that can be shared across multiple tests.

## Splitting Tests into Steps with `test.step`

When you want to add more clarity to your tests, Playwright's `test.step` function is handy. It breaks down complex tests into more digestible steps, improving readability and reporting.

## Using Built-in Annotations: `skip`, `fail`, and `fixme`

Annotations in Playwright help you mark tests for specific behaviors or conditions:

- **`test.skip`**: Skip tests conditionally based on environment, platform, or feature availability.
- **`test.fixme`**: Mark tests that need fixing later; Playwright skips them and flags them in reports.

## Adding custom Annotations

You can add custom annotations to link related issues in reports and UI mode.

## Using Tags to Filter and Organize Tests

Tags categorize tests by features, priorities, user roles, or release cycles. Run tests by tag with `npx playwright test --grep @mocking` or exclude with `--grep-invert`.

## Summary

- Organized Folder Structure: Separate tests by context (logged-in vs. logged-out) and per feature.
- Using Hooks and Describe blocks: Improve readability and set up common prerequisites.
- Step Definition: Use `test.step` to break down complex test cases.
- Leveraging Annotations and Tags: Mark failing or incomplete tests, link issues, and categorize tests.

With a thoughtful approach to organizing your tests, you'll be able to create a cleaner and more maintainable test suite that scales well with your application.

Happy testing!

---

<!-- source: show-metadata-in-playwright-html-report-a-complete-guide.md -->

[Playwright](https://playwright.dev/) is a powerful testing framework, but its HTML reports can feel bare-bones for complex projects. Wouldn't it be nice to display metadata such as commit messages, author details, or links to CI builds right in the report header?

## Understanding Playwright Metadata

The Playwright documentation mentions a metadata field for configuration, but at Playwright v1.49 the docs were outdated. Metadata in reports is indeed possible.

## The Real Metadata Configuration

Supported HTML report fields (from Playwright source):
- revision.id, revision.author, revision.email, revision.subject, revision.timestamp, revision.link
- ci.link, timestamp

Configure in playwright.config.ts with reporter: 'html' and metadata object.

## Automating Metadata Population

### Third-Party packages
Use npm packages that extract Git commit information.

### Leveraging Playwright's Hidden Plugin System
Use the hidden @playwright/test plugins config with gitCommitInfo() plugin to auto-populate commit hash, message, author, email, timestamp, and CI links.

### Using a Custom Function for Metadata
Adapt gitStatusFromCLI() from the gitCommitInfo plugin for independent use in playwright.config.ts.

#### Optimizing for Parallel Tests
Execute metadata function only in main worker (empty TEST_WORKER_INDEX) to avoid slowing each worker.

## Conclusion

Adding metadata to Playwright HTML reports is possible though not well-documented. Metadata fields are limited to specific keys. Automate with hidden plugins or custom scripts. Optimize for parallel tests by running metadata logic only in the main worker.

---

<!-- source: playwright-visual-tests-with-git-lfs-and-docker.md -->

# Playwright Visual Tests with GIT-LFS and Docker

While working on various projects, I struggled to find a good solution for managing golden screenshots in Playwright. Source code: https://github.com/pajdekPL/playwright-git-lfs

## Why Git LFS for Visual Testing?

Git LFS stores large screenshot files outside regular Git, replacing them with lightweight pointers. Benefits: smaller repo, faster clones, efficient binary file handling.

## Benefits of Version-Controlled Screenshots

Team collaboration, change history, code review integration, accountability, rollback capability, CI/CD integration.

## Setup Instructions

1. Install Git LFS: `brew install git-lfs && git lfs install`
2. Configure `.gitattributes`: `tests/**/*.png filter=lfs diff=lfs merge=lfs -text`
3. Set `snapshotDir: "./screenshots"` in playwright.config.ts

## Usage

Write visual tests with `toHaveScreenshot()`, generate baselines with `npx playwright test --update-snapshots`, commit with regular git commands.

## Building and Running PW in Docker

CI runs on Linux — generate Linux screenshots via Docker:
```
docker run --rm --network host --ipc=host -v "$(pwd)":/work/ -w /work/ -it mcr.microsoft.com/playwright:v1.49.0-noble /bin/bash -c "npm install && bash"
```
Or use `run-pw-docker.sh` script.

## Screenshot Management Strategy

Only store Linux screenshots in repo:
```
*.png
!*-linux.png
```

## Best Practices

Organize screenshots, meaningful names, tagging (@visual-regression), review changes, CI Git LFS setup.

## Common Issues and Solutions

- Large repo: clean unused screenshots
- Team: ensure Git LFS installed, run `git lfs pull`
- CI: install Git LFS in pipeline
- Husky hooks may need Git LFS hook integration

## Conclusion

Git LFS provides efficient management of Playwright visual test screenshots. Use Docker for cross-platform consistency.

---

<!-- source: achieving-wcag-standard-with-playwright-accessibility-tests.md -->

# Achieving WCAG Standard with Playwright Accessibility Tests

Accessibility testing ensures applications are usable by everyone, including people with disabilities. WCAG (Web Content Accessibility Guidelines) has become non-negotiable for QA.

## Why Accessibility Testing?

- Expands audience, enhances UX, strengthens brand, complies with legal standards
- Automated testing: time efficiency, scalability, CI/CD integration, early detection
- Limitations: ~30% of issues detectable automatically; human empathy and context need manual validation

## Getting Started with Playwright and Axe

Install Playwright: `npm init playwright@latest`
Install axe-playwright: `npm i -D axe-playwright`

Example accessibility scan using AxeBuilder and checkA11y with detailed HTML report:

```javascript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { checkA11y } from 'axe-playwright';

test('Accessibility Scan', async ({ page }, testInfo) => {
  await page.goto('https://www.npmjs.com/package/axe-playwright');
  const results = await new AxeBuilder({ page }).analyze();
  await testInfo.attach('accessibility-scan-results', {
    body: JSON.stringify(results, null, 2),
    contentType: 'application/json',
  });
  await checkA11y(page, undefined, { detailedReport: true, detailedReportOptions: { html: true } });
  expect(results.violations).toEqual([]);
});
```

Accessibility testing with Axe and Playwright efficiently identifies issues. Make the web a better place for everyone!

---

<!-- source: how-to-test-login-functionality-using-otp-codes-with-playwright.md -->

# How to test login functionality using OTP codes with Playwright

Broken or unreliable login systems can lead to frustrating user experiences, potential security vulnerabilities, and lost trust.

## Why test login functionality?

Automating login tests with Playwright saves time and ensures consistency.

## Why Playwright for login testing with Mailosaur?

- Cross-browser support
- Email verification testing (OTP, 2FA, activation links)
- Fast execution with Mailosaur API
- Automatic waiting in Playwright
- Full page testing with email integration

## Complex login scenarios

Testing 2FA via SMS or email OTP requires handling one-time passcodes and multi-step verification.

### Set up a simple Playwright project

```
npm create mailosaur@latest
```

### OTP via SMS

Use MailosaurClient to retrieve SMS passcode after login attempt, fill OTP field, submit, confirm login.

### OTP via email

Use catch-all Mailosaur email addresses, retrieve email via mailosaur.messages.get(), extract passcode from email.text.codes[0].

---

<!-- source: playwright-visual-testing-dynamic-data.md -->

# Playwright Visual Testing - Dynamic Data

How to handle dynamic data in Playwright visual tests.

## What is dynamic data?

Anything on your web page that could change between test runs: random data, API data, current date/time, etc.

## Options for dealing with dynamic data

### Option 1: Mock your dynamic data
Mock lists/API responses so data is consistent each run.

### Option 2: Hide the dynamic data
Hide elements before screenshot comparison.

## Hiding dynamic data

`toHaveScreenshot()` accepts `stylePath` parameter. Create screenshot.css:
```css
#datetime { display: none; }
```
Pass to toHaveScreenshot with stylePath option.

## Masking dynamic data

Use `mask: [page.locator('#datetime')]` parameter. Requires updating baseline to include mask placeholder.

## Functional validation

Still validate dynamic data functionally (e.g. assert datetime text matches current date) while visual comparison handles the rest.

## Wrap-up

Hide dynamic data in visual tests while using functional validation to ensure dynamic content is correct.

---

<!-- source: is-it-worth-mocking-websockets-by-playwright-by-andrey-enin.md -->

# Is It Worth Mocking WebSockets by Playwright?

## Rather yes.

At first glance, there is nothing complicated about testing WebSockets. You can filter connections of this protocol type (WS) in a browser's DevTools, API's logic can be traced in WebSocket's messages, and many popular testing tools, including Postman, support WebSockets. But things get more complicated when it comes to automation testing.

## What the hell are WebSockets?

WebSocket is a communication protocol that provides bidirectional communication between a client and a server over a persistent connection. It enables real-time data exchange between a client (usually a web browser) and a server, and allows efficient, low-latency communication.

> WebSocket **s** (in the plural) is a common name for multiple connections of this protocol type (even if only a single connection is made on the web page), like: «this site works on WebSockets».

Unlike traditional HTTP, where the client initiates a request and waits for a response, WebSocket allows both parties to send messages independently at any time, making it ideal for applications like chats, online gaming, collaborative tools, financial and trading services, and IoT applications.

However, the benefits of the WebSocket protocol pose challenges when it comes to automated tests.

- **Connection Handling.** Due to the need to establish a connection between both parties (client and server), it is pretty problematic to intercept this connection.
- **Asynchronous Communication.** WebSocket communications are often asynchronous, making it difficult to design a regular sequential test.
- **Message Formats.** WebSocket messages may use various formats (e.g., JSON, plain text, binary), requiring parsers and serializers to be included in tests.
- **Tooling Limitations.** Not many automation testing frameworks support interception and modification of WebSocket connection. To be honest, I did not know any before Playwright 1.48.

## Evolution of testing WebSockets in my application

My current testing project has a lot of functionality related to WebSocket connections. Due to the above-mentioned challenges (primarily due to tooling), its autotests have undergone evolutionary changes.

- Initially, we did not have any automation testing for WebSocket functionality. These features were tested manually, which slowed down our regression testing.
- Then, we implemented console logging of some WebSockets messages. Because Playwright allows us to listen to the browser's console messages, we could check WebSocket API messages (and thus the application logic), but it was extremely overcomplicated and unhandy.
- Then, we implemented a fallback to HTTP API for some WebSocket features. This made it possible to intercept and modify requests and responses for testing reasons. Again, it was overcomplicated, but it has expanded the automation testing opportunities.
- During 2021–2022, we hoped that Playwright would implement a feature-issue of WebSocket interception, but after a year, we gave up and started to implement our own mocking of WebSocket API.

There were two ways to mock WebSockets: **run a mock server** (like Camouflage) **or use a mocking library within the application** (like Mock Service Worker) — we chose the second one.

Adding **MSW (Mock Server Worker)** inside the application allowed us not only to mock WebSocket API but also to speed up the frontend development; our development and QA teams started using the same test doubles for testing.

The only flaw was that our application had a separate bundle for testing with mocks, while in production, it was deployed as a slightly different bundle.

## How to mock WebSockets with Playwright?

Firstly, why do mocking WebSockets through Playright if mocking through MSW works well? Because that is how we put the test infrastructure at the testing framework level. We can build and test our application as it will be deployed on production.

> WebSocket routing was added to Playwright since version 1.48.

The manner of mocking WebSockets with Playwright is quite straightforward. Everything is carried out by the WebSocketRoute class. As soon as you interfere in WebSocket communication by the onMessage method, WebSocket's messages will stop forwarded between page and server ⇒ you should handle the communication by yourself.

Here are a few tricky things before the start:

1. `routeWebSocket()` **method should be called before navigating the page;**
2. **The WS URL is more accessible when set by RegExp.** In most cases, a WebSocket connection is established with `ws://` or `wss://` schemes;
3. When using `routeWebSocket()` in your tests, **Playwright takes complete control over the WebSocket connection ⇒ you will not see the WS handler in a browser's DevTools Network tab anymore**.

So, here is a code if you want to catch a specific message **from page to server:**

```
await page.routeWebSocket(/.+\/api/, (ws) => {
  ws.onMessage((message) => {
    if (message === '{"command":"ping"}') {
      ws.send('{"command":"fooBar"}');
    }
  });
});
```

Where `ws.send()` method sends a message **to the page.**

And here is a code if you want to catch a specific message **from server to page:**

```
await page.routeWebSocket(/.+\/api/, (ws) => {
  const server = ws.connectToServer();
  server.onMessage((message) => {
    if (message === '{"command":"pong"}') {
      ws.send('{"command":"fooBar"}');
    } else {
      ws.send(message);
    }
  });
});
```

Where `ws.send()` method sends a message to the page from the server.

Unfortunately, I could not figure out if it is possible to intercept and mock messages in both ways in one `test()`. Even so, there is nothing to worry about if these test cases can be splitted.

To sum it up, Playwright's WebSockets has its limitations in the place of invoking the router and **requires advanced skills from a test engineer**, but if you have a simple application or a plain feature for testing, then WebSockets' out-of-the-box functionality is pretty enough.

---

<!-- source: make-your-playwright-tests-run-faster-by-using-the-playwright-api-to-wait-testan.md -->

There are times when automating a test in Playwright that the test needs to wait because the test will flake if it does not wait for something such as an event. It can be, for example, that you are waiting for a navigation to complete.

Tests can be made to wait with a 'wait' for a given period of time, say five seconds. If this is done the tests will always wait for that period of time, whether or not it is necessary and the tests will be slower than they need to be.

It is better to have tests wait for something specific such as a locator to be rendered, an event or an API call because the test waits for the locator or event. If a test uses this type of wait, it only waits as long as necessary for the specified condition to be met. Also, the tests should not flake and run more quickly.

I have found the Typescript functions in the Playwright API described below to be useful ways of waiting when writing Playwright tests:

### Waiting for a locator – waitFor()

The function waitFor() can be chained to a locator to wait for that locator to be rendered, for example:

I have found waitFor() useful when waiting for navigation to complete because the test will wait for the specified locator.

Arguments for the state of the locator being waited for and for the length of the timeout can be passed to waitFor() for the state that is being waited for: https://playwright.dev/docs/api/class-locator#locator-wait-for

### Waiting for an API call – waitForResponse()

A test may need to wait for the response from an API call. This can be done with waitForResponse(). In this example, waitForReponse is waiting for a response after click().

The API call that the test needs to wait for can be found in the network tab of Dev Tools and is then passed as a parameter to waitForResponse(). The function waitforReponse() returns the matched response.

Arguments can be passed to WaitforResponse() for the predicate and the timeout: https://playwright.dev/docs/api/class-page#page-wait-for-response

### Waiting for an event – waitForEvent()

Sometimes the test is not waiting for an API call or the presence of a locator, it is waiting for an event, such as 'requestfinished' or 'domcontentloaded'. WaitForEvent waits for the event to fire and returns a truthy value. In the example below the test is waiting for a 'domcontentloaded' event that occurs after navigate().

Arguments can be passed to waitForEvent() for the event, predicate and timeout: https://playwright.dev/docs/api/class-page#page-wait-for-event

### Waiting for a truthy value – waitForFunction()

If a test needs to wait for a truthy value, such as the value returned by querySelectorAll() you can use waitForFunction();

The function will wait up to the length of time specified in the timeout for truthy to be returned. This example returns truthy when length is greater than zero and falsey when length is zero.

Arguments can be passed to waitForFunction() for the function, evaluation argument, polling and timeout: https://playwright.dev/docs/api/class-page#page-wait-for-function

### Conclusion

This post describes the functions that I am using to make tests wait. The Playwright API contains more functions that can be used for waiting than I have shown above. If you find that the functions in this post do not meet your needs it is worth exploring the Playwright API to find different ways of making tests wait.

It is easy to get in the habit of using waits in our test that wait for a given number of seconds. However, waiting for a given number of seconds is a habit that will slow your tests down.

If you use functions like those above to make your tests wait, the tests will only wait as long as necessary for the condition to be met and so run faster.

---

<!-- source: playwright-tips-and-trick-4.md -->

Continuing the series of tips and tricks, after the success of #1 #2 and #3 comes our newest addition #4. Hope you enjoy and don't forget to read also the comments inside the code snippets.

## 1. How to intercept multiple requests with the same route?

I remember in Cypress that this was really easy to achieve, however in Playwright its not that easy to find details for such particular scenario. Imagine you navigate thru your web app and you are interested to validate multiple requests that have the same route. Say when you open a page for a product, an API call is performed at `api/id/1` to fetch data, then you need to later intercept the exact same call at `api/id/1` . How would I intercept both of them ? And also validate its data ?

You can try this with waitForRequest(), but there is a more versatile way in doing this, that will open a lot of possibilities for you

```javascript
import { test } from "@playwright/test";

test("Validate multiple same route calls", async ({ page }) => {
  const requests: string[] = [];

  // use route to differentiate between your network calls
  await page.route("**/api/**", async (route) => {
    // use url().includes to filter the exact call you need
    if (route.request().url().includes("id/1")) {
      await route.continue();
      const response = await (await route.request().response())?.json();
      requests.push(response);
    } else await route.continue();
  });

  await page.goto("www.yourapp.com");
  await product.click();
  await productExtraDetails.click()

  await expect(async () => {
    expect(requests.length).toBe(2);
  }).toPass({
    intervals: [1_000, 2_000, 5_000],
    timeout: 15_000,
  });
});

test.afterEach("Unroute all", async ({ page }) => {
  await page.unrouteAll({ behavior: "wait" });
});
```

Most common use case of this scenario I've seen is validating trackers.

## 2. Do not use .all() without asserting the count of elements before

Assuming you have a selector that will return multiple elements and you want to iterate over those elements to assert some values. If you don't assert the count first you may end up with false positives in your tests.

If you have 3 elements and do `.all()` without asserting count, tests will work as intended. However, if by some reason your locator will not find any elements on page, test will **still pass**. Because `.all()` does not error out if count is zero and forEach will not run if no elements.

To have this properly you must assert the count of elements before iterating over them. Remember to use auto-retry toHaveCount and not stuff like (allElements.length).toBe(3)

## 3. No need to assert toBeVisible before a click

I feel like I have to say this because I see it far too often. People are used to assert visibility of an element before interacting with it, this is a behavior learned in any tutorial or class and it has been a crucial step everyone was in the past forced to learn. But in playwright there is one particular case where you don't need to do that. The case is for the action `click()`. Playwright performs actionability checks before a click on an element.

One key aspect is that you should not confuse this with an assertion for toHaveText. Be careful when you use toHaveText, because toHaveText does not wait for the element to be visible first.

## 4. How do I run only tests I work on?

You can use a special CLI command that will run only tests that have a change in it.

Instead of running multiple test files manually:

`npx playwright test path/to/test1.spec.ts path/to/test2.spec.ts ...`

You can just do this:

`npx playwright test --only-changed=main`

This will compare your branch with branch main and run all tests that have been modified by you in your branch.

## 5. How do I validate table data in Playwright?

Imagine you have a table and you need to validate values rendered. I am not talking here about validating a text exists in any cell, I am talking about a value that should be in an exact cell that is reference to exact column and exact row.

You can achieve this by creating a helper function:

```javascript
import { test, expect } from "@playwright/test";

class WebTableHelper {
  constructor(page) {
    this.page = page;
  }

  async validateCellValueReferenceToHeader(
    headerText,
    rowToValidate,
    expectedValue
  ) {
    const elementsOfHeader = await this.page
      .getByRole("table")
      .getByRole("row")
      .first()
      .getByRole("cell")
      .all();

    const elementsOfNRow = await this.page
      .getByRole("table")
      .getByRole("row")
      .nth(rowToValidate)
      .getByRole("cell")
      .all();

    for (let i = 0; i < elementsOfHeader.length; i++) {
      const headerValueFound = await elementsOfHeader[i].innerText();
      if (headerValueFound.toLowerCase() === headerText.trim().toLowerCase()) {
        const cellValue = await elementsOfNRow[i].innerText();
        expect(cellValue).toBe(expectedValue);
        return;
      }
    }
    throw new Error(`Header with text "${headerText}" not found`);
  }
}

test("test tables", async ({ page }) => {
  await page.goto("https://cosmocode.io/automation-practice-webtable");
  const helper = new WebTableHelper(page);
  await expect(page.getByRole("table").getByRole("row")).toHaveCount(197);
  await helper.validateCellValueReferenceToHeader("Currency", 4, "Euro");
});
```

## 6. How do I get the element background color in Playwright?

I've seen plenty of such examples where people would get the color of a background with code similar to this:

```javascript
const color = await btn.evaluate((element) =>
    window.getComputedStyle(element).getPropertyValue("background-color")
  );
expect(color).toBe("rgb(69, 186, 75)")
```

No need to do things like this anymore, we have `await expect(locator).toHaveCSS('background-color',"rgb(69, 186, 75)")`

And if you want to actually deal with Hex instead of RGB, there are good approaches documented for checking colors with Playwright.

---

<!-- source: supercharge-your-e2e-tests-with-playwright-network-cache.md -->

## Intro

When working with end-to-end testing frameworks like Playwright, handling network requests is often a complex task. Tests that rely on external APIs can be slow and inconsistent, introducing unnecessary flakiness. Network calls that succeed in one test run might fail in the next due to a slow or unreliable server, resulting in inconsistent results. To address this, developers often resort to mocking network requests, which introduces another challenge: managing mocks.

Wouldn't it be great to have an automated way to handle caching and reusing network responses without setting up complex mocking strategies? I've investigated existing approaches and developed a tool that I want to introduce. It solves these exact problems by caching network requests on the filesystem, enabling faster and more reliable Playwright tests.

## The Problem with Network Requests in Tests

Network requests are often the slowest part of test execution. When running multiple test suites, the repeated querying of external APIs can dramatically increase test durations. Additionally, real-world APIs can be unstable, with occasional timeouts, making your tests fragile and unreliable.

A common approach to mitigating this is to mock API responses. While useful, mocking requires manual intervention — you need to carefully construct mock responses, keep them updated, and ensure that every potential network scenario is handled. This can become a huge maintenance burden as the API evolves or your test cases change. Playwright supports HAR files for capturing and replaying network traffic, but working with HAR can be tedious and lacks flexibility for modifying responses on the fly.

## Enter `playwright-network-cache`

playwright-network-cache is designed to streamline the process of caching network responses in Playwright tests, eliminating the need for manual mocks or rigid HAR files. With this library, network responses are automatically stored on the filesystem during the first test run and can be reused in subsequent runs, significantly speeding up test execution. Moreover, the responses are saved in a clear, organized folder structure, making it easy to inspect and modify them as needed.

### How It Solves the Problem

1. **Automatic Caching**: The library automatically caches network responses when tests are run for the first time. This means that your tests won't have to wait for external APIs to respond in future runs — the cached data will be used instead, resulting in faster and more reliable tests.

2. **Customizable Cache Duration (TTL)**: You can control how long the cached file is retained by setting a time-to-live (TTL) option. After the specified time elapses, the library will hit real API again and refresh the cache, keeping your test data up to date.

3. **Dynamic Modifications**: Need to tweak a response for a specific test case? playwright-network-cache allows you to modify cached responses dynamically. Whether you want to change the status code, headers, or response body, the library provides options to adjust the cached data on-the-fly without manually maintaining separate mocks.

4. **Flexible Structure**: The caching system organizes files based on hostname, request method, and URL path, ensuring that you can easily navigate through and manage the cached data.

5. **Speed Boost**: By reusing cached responses, your tests no longer need to wait for network calls to complete, making them dramatically faster.

6. **No More Mock Hell**: Forget about manually maintaining mocks. The library handles everything for you — from caching to replaying and even modifying responses.

7. **No HAR Complexity**: playwright-network-cache provides a cleaner, more flexible alternative to HAR by letting you manage individual responses as simple JSON files.

### Example

Imagine you're testing an application that fetches a list of cats from an API. Without caching, each test run would require a live request to the API, adding latency and potential failure points to your tests.

With playwright-network-cache, you can easily cache the API response:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats');
  // Perform usual test actions...
});
```

On the first run, the response is cached in the `.network-cache` directory:

```
.network-cache
└── example.com
    └── api-cats
        └── GET
            ├── headers.json
            └── body.json
```

On subsequent runs, the cached response is reused, making the test faster and eliminating the need to hit the actual API.

To revalidate the cache, you can provide `ttlMinutes` option:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats', {
   ttlMinutes: 60 // hit real API once in a hour
  });
});
```

You can modify cached response for the particular test needs:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats', {
    modify: async (route, response) => {
      const json = await response.json();
      json[0].name = 'Kitty-1';
      await route.fulfill({ json });
    }
  });
});
```

To get `cacheRoute` variable available in your tests, instantiate it like any other Playwright fixture:

```
// fixtures.js
import { test as base } from '@playwright/test';
import { CacheRoute } from 'playwright-network-cache';

export const test = base.extend({
  cacheRoute: async ({ page }, use) => {
    const cacheRoute = new CacheRoute(page, { /* cache options */ });
    await use(cacheRoute);
  },
});
```

### More Than Just Caching

playwright-network-cache isn't just about caching. It offers advanced features like:

- **Modifying Responses**: Adjust the data in the cached responses dynamically using custom functions.
- **Handling Status Codes**: Cache responses based on specific HTTP status codes, including errors.
- **Flexible Directory Structure**: Customize how and where cache files are stored.
- **Disable or Update Cache**: Temporarily disable caching for specific tests or force updates to the cache when needed.

## Recap

If you're looking to make your Playwright tests faster and more reliable, give a try to playwright-network-cache. By caching network responses on the filesystem and allowing for dynamic modifications, it eliminates the need for manual mocks and provides a flexible, easy-to-use alternative to HAR files.

Thanks for reading ❤️

---

<!-- source: api-mocking-using-playwright.md -->

# API Mocking using Playwright

### What is API Mocking?

**API mocking** is a technique to simulate the behavior of an API or service without calling actually API. This is extremely beneficial in various ways such as Isolation, Speed, Control, Reliability.

- **Isolation:** It allows you to test your code in isolation, ensuring that any issues you encounter are related to your code and not the external API.
- **Speed:** Mocking can significantly speed up your development and testing process, as you don't have to wait for slow or unreliable network connections.
- **Control:** You can control the responses from the mocked API, allowing you to test different scenarios and edge cases.
- **Reliability:** Mocking can help you ensure that your code is resilient to changes in the external API.

Web APIs are usually implemented as HTTP endpoints. Playwright provides APIs to mock and modify network traffic, both HTTP and HTTPS. Any requests that a page does, including XHRs and fetch requests, can be tracked, modified and mocked. With Playwright you can also mock using HAR files that contain multiple network requests made by the page.

### API MOCKING vs API TESTING

**There are 3 ways for API mocking in playwright**

1. **Mock API Requests**
2. **Mock API Responses**
3. **HAR file:** Recording HAR file, Modifying HAR file, Replying from HAR file.

### 1. Mock API Requests

The following code will intercept all the calls to `*/**/api/v1/fruits` and will return a custom response instead. No requests to the API will be made.

```javascript
await page.route('*/**/api/v1/fruits', async route => {
    const json = [
        { name: 'playwright by testers talk', id: 21 },
        { name: 'cypress by testers talk', id: 71 },
        { name: 'api testing by testers talk', id: 72 },
        { name: 'postman by testers talk', id: 73 },
        { name: 'rest assured by testers talk', id: 74 },
    ];
    await route.fulfill({ json });
});
await page.goto('https://demo.playwright.dev/api-mocking');
await expect(page.getByText('playwright by testers talk')).toBeVisible();
```

### 2. Mock API Responses

It is essential to make an API request, but the response needs to be patched to allow for reproducible testing.

```javascript
await page.route('*/**/api/v1/fruits', async route => {
    const response = await route.fetch();
    const json = await response.json();
    json.push({ name: 'playwright by testers talk', id: 100 });
    await route.fulfill({ response, json });
});
```

### 3. HAR File

To record a HAR file we use `page.routeFromHAR()` or `browserContext.routeFromHAR()` method.

```javascript
await page.routeFromHAR('./hars/fruit.har', {
    url: '*/**/api/v1/fruits',
    update: true,
});
await page.goto('https://demo.playwright.dev/api-mocking');
await expect(page.getByText('Strawberry')).toBeVisible();
```

Set `update: false` after recording, then modify the .bin file JSON to inject custom test data.

### Complete Test Example

```javascript
const { test, expect } = require('@playwright/test');

test("API Mocking Using Playwright", async ({ page }) => {
    await page.route('*/**/api/v1/fruits', async route => {
        const json = [
            { name: 'playwright by testers talk', id: 21 },
            { name: 'cypress by testers talk', id: 71 },
        ];
        await route.fulfill({ json });
    });
    await page.goto('https://demo.playwright.dev/api-mocking');
    await expect(page.getByText('playwright by testers talk')).toBeVisible();
});
```

Run with `npx playwright test`. For HAR recording, observe that hars folder is created with .har and .bin files. Modify the .bin JSON to inject custom test data, then set `update: false` for replay.

Reference: https://playwright.dev/docs

---

<!-- source: effective-utilization-of-playwright-fixtures-a-comprehensive-guide.md -->

# Effective Utilization of Playwright Fixtures: A Comprehensive Guide

Maintaining clean, efficient, and scalable test code becomes increasingly challenging as web applications become more complex. Playwright, a powerful end-to-end testing framework, offers a solution through its fixture system.

## Introduction to Playwright Fixtures

Fixtures in Playwright allow you to share data or objects between tests, set up preconditions, and manage test resources efficiently.

## 1. Creating Page Object Fixtures

```typescript
// pages/login.page.ts
export class LoginPage {
  constructor(private page: Page) {}
  async login(username: string, password: string) {
    await this.page.fill('#username', username);
    await this.page.fill('#password', password);
    await this.page.click('#login-button');
  }
}

// fixtures.ts
export const test = base.extend<{
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
}>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
});
```

## 2. Creating API Class Fixtures

```typescript
export class UserAPI {
  constructor(private request: APIRequestContext) {}
  async createUser(userData: any) {
    return this.request.post('/api/users', { data: userData });
  }
}

export const test = base.extend<{
  userAPI: UserAPI;
  productAPI: ProductAPI;
}>({
  userAPI: async ({ request }, use) => {
    await use(new UserAPI(request));
  },
});
```

## 3. Creating Helper Fixtures at Worker Scope

Worker-scoped fixtures allow you to share resources across multiple test files within a single worker process.

```typescript
export const test = base.extend<{}, { dbHelper: DatabaseHelper; testDataGen: TestDataGenerator }>({
  dbHelper: [async ({}, use) => {
    const dbHelper = new DatabaseHelper();
    await dbHelper.connect();
    await use(dbHelper);
    await dbHelper.disconnect();
  }, { scope: 'worker' }],
});
```

### Best practices when working with worker-scoped fixtures:

- Use worker scope for fixtures that are expensive to set up but can be safely shared between tests.
- Ensure that worker-scoped fixtures are stateless or can be reset between tests.
- Use environment variables or configuration files to manage connection strings.

## 4. Creating Optional Data Fixtures

```typescript
export const test = base.extend<{ testUser?: User }>({
  testUser: [async ({}, use) => {
    await use({
      username: 'defaultuser',
      password: 'defaultpass123',
      email: 'default@example.com',
      role: 'user'
    });
  }, { option: true }],
});
```

## 5. Defining TestFixtures and WorkerFixtures Types

```typescript
export interface TestFixtures extends PageFixtures, APIFixtures, DataFixtures {}
export interface WorkerFixtures extends HelperFixtures {}

export const test = base.extend<TestFixtures & WorkerFixtures>({});
```

## Combining Different Types of Fixtures

```typescript
export const test = base.extend<TestFixtures, WorkerFixtures>({
  loginPage: async ({ page }, use) => { await use(new LoginPage(page)); },
  userAPI: async ({ request }, use) => { await use(new UserAPI(request)); },
  testUser: [async ({}, use) => { await use({ id: '1', username: 'testuser' }); }, { option: true }],
  dbHelper: [async ({}, use) => {
    const helper = new DatabaseHelper();
    await helper.connect();
    await use(helper);
    await helper.disconnect();
  }, { scope: 'worker' }],
});
```

## Conclusion: Best Practices

1. Modularize your fixtures
2. Use the appropriate scope
3. Leverage TypeScript
4. Balance flexibility and simplicity
5. Keep fixtures focused
6. Use composition
7. Maintain consistency
8. Document your fixtures
9. Regular refactoring
10. Test your fixtures

Happy Testing!

---

<!-- source: timeouts-against-flaky-tests-true-cases-with-playwright.md -->

# Timeouts Against Flaky Tests: True Cases with Playwright

Playwright is a highly reliable tool for UI testing, but there are cases when some tests can be flaky, and the timeout anti-pattern fixes this!

Flaky tests are unreliable tests that occasionally fail, but the reason for the failures is unclear. When comparing tools based on Selenium WebDriver and those based on Chrome DevTools Protocol (Puppeteer and Playwright), the last one turns out to be more accurate.

Even adhering to the best practices in writing autotests to prevent false falls, unreliable results, and unwanted errors sometimes requires breaking established principles and setting exceptions in the code.

Below are a few cases:

1. UI Animations
2. Drawing on Canvas
3. Sleep Timeouts in CI in the Cloud

> **Disclaimer:** The following may be very controversial. If you want to adopt the suggestions in the text, you should be aware of what you are doing.

## 1. UI Animations

Playwright performs a range of actionability checks on the elements before making actions. But sometimes, very rarely, Playwright may not click (or misclick) on truly visible and accessible elements.

This may suddenly appear during animations of menus, drawers, popovers, overlays, and other pop-up elements with transitions. When the element is already on the screen, it is already interactable, but the animation is not over yet.

**Solution:** Add unconditional timeout before or after flaky click.

## 2. Drawing on Canvas

Interaction with `<canvas>` elements can also be tricky. The element is actionable for Playwright from the moment it appears on the page, but the actual actionable state of the element is indefinable by its locator.

The HTML element is the same for all states, but when Playwright is already getting ready to perform clicks, the element itself is not.

**Solution:** Add unconditional timeout before the first click.

Both issues are the most common root causes of UI-based flaky tests, according to the 2021 study "An Empirical Analysis of UI-based Flaky Tests".

## 3. Sleep Timeouts in CI in the Cloud

If your frontend application has its own timeouts (actual business logic may require periods of "sleep"), then your tests may be affected by it.

`setTimeout()` can be executed longer than the specified time in CI runners during testing and does not reproduce in production!

**Solution:** Add unconditional timeout that is at least three times longer than the logic under test.

## Timeout Tips

- **Make timeout's value as constant.** In my experience, 300 ms is the optimal time waiting for something on the UI.

```typescript
export const UNCONDITIONAL_TIMEOUT = 300;
```

- **Add a comment for each case of using timeout.**

```typescript
// Wait for input to be applied in the select
await page.waitForTimeout(UNCONDITIONAL_TIMEOUT);
```

UPDATE: Issues #1 and #2 can be solved another way instead of timeouts. Actions with checks may be wrapped in `expect.toPass()` method to retry a flaky sequence.

Read more:
- What is Flaky Test?
- We Have A Flaky Test Problem
- How to Avoid Flaky Tests in Playwright

Watch more:
- Avoid flaky end-to-end tests due to poorly hydrated Frontends with Playwright's toPass()
- Hydration documentation in Playwright navigations guide

---

<!-- source: a-test-data-strategy-for-parallel-automation-in-playwright.md -->

# A Test Data Strategy for Parallel Automation in Playwright

## Introduction to Parallel Automation and Playwright

In the fast-paced world of software development, parallel automation has emerged as a game-changer. It's all about running multiple tests simultaneously to speed up the testing process. Playwright is a tool that stands out when it comes to parallel automation.

## Understanding Test Data Strategy

A test data strategy is a plan that outlines how to manage and use data during testing. It includes test data's design, creation, storage, and maintenance.

### The Importance of Parallel Automation in Testing

Parallel automation significantly reduces testing time, leading to faster releases. It also improves testing coverage, ensuring higher software quality.

However, managing test data in parallel automation can be challenging. It requires careful planning to ensure that the tests do not interfere with each other.

## Introduction to Playwright

Playwright is a modern automation tool that supports multiple browsers and provides reliable and efficient testing. It offers features like automatic waiting, network interception, and multiple browser support.

## Creating a Test Data Strategy for Parallel Automation in Playwright

### The Role of Faker in Data Generation

Faker is a library that generates massive amounts of fake data for you. In the context of Playwright, Faker can generate test data on the fly.

```bash
npm i @faker-js/faker
```

### Object Inheritance for Test Data

```javascript
const baseProduct = {
  type: null,
  id: faker.string.uuid(),
  name: faker.commerce.productName(),
  price: faker.commerce.price(),
  inStock: faker.datatype.boolean(),
};
const emergencyProduct = {
  ...baseProduct,
  type: 'Emergency goods',
};
```

**Advantages:** Reusability and extensibility.
**Drawbacks:** Does not scale well; Faker seed doesn't reset in parallel execution.

### Implementing a Data Factory Pattern

```typescript
export class DataFactory {
  private baseProduct: Product = { /* ... */ };
  generateData<T>(baseData: T, customFields: Partial<T> = {}): T {
    return { ...(baseData as T), ...customFields };
  }
  generateProductData(customFields: Partial<Product> = {}): Product {
    return this.generateData<Product>(this.baseProduct, customFields);
  }
}
```

### Using Playwright Fixtures for Data Factory Initialization

```typescript
const test = baseTest.extend<{ dataFactory: DataFactory }>({
  dataFactory: async ({}, use) => {
    await use(new DataFactory());
  },
});
```

**Why Is This Useful?**
- Isolation: Each test gets its fresh instance of the Data Factory.
- Seed Reset: By creating a new Data Factory instance for each test, you can reset the seed for Faker.

### Example Tests Using the Data Factory

```typescript
test('Verify emergency product name', async ({ page, dataFactory, productPage }) => {
  const emergencyProduct = await dataFactory.generateProductData({
    type: 'Emergency goods'
  });
  await page.goto('http://example.com/product-form');
  await productPage.createProduct(emergencyProduct);
  await expect(page.locator(`[data-id="${emergencyProduct.id}"]`))
    .toHaveText(emergencyProduct.name);
});
```

## Case Study

A software company successfully implemented a test data strategy for parallel automation in Playwright. This strategy reduced testing time by 50% and significantly improved test coverage.

## Conclusion

Parallel automation in Playwright, coupled with a well-planned test data strategy, can significantly improve the efficiency and effectiveness of testing.

---

<!-- source: localize-your-test-with-playwright.md -->

# Localize your test with Playwright

### Emulate the "geolocation", "locale" and "timezone" with Playwright

Some websites are available in different languages and countries and include differences in texts, date formats, currencies, laws, RTL languages, and colors.

Some e-commerce websites request access to your current geolocation to offer products in your current location.

## What is localization testing?

This type of testing checks that the behavior, translations, usability, accessibility, etc, are appropriate for the specific country or region.

## Tips to test your websites in different languages

1. Sometimes, the UI and elements differ in each country. Use Chrome's translate option to help locate elements in English.
2. Some websites are restricted by IP — use LambdaTest, Sauce Labs, or BrowserStack to test from another country.
3. Don't use translators to check translations — get translations from a translation team or native speakers.

## Emulate the user locale, timezone, and geolocation with Playwright

### Locale geo and timezone globally

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
    geolocation: { longitude: 12.492507, latitude: 41.889938 },
    permissions: ['geolocation'],
  },
});
```

### Locale and Timezone in test spec

```typescript
test.describe('Locale translations', () => {
    test.use({
        locale: 'de-DE',
        timezoneId: 'Europe/Berlin',
    });
    test('Translations with locale and time zone id', async ({ page }) => {
        await page.goto('https://www.google.com');
   });
});
```

### Geolocation sample

```typescript
test.describe('Geo Location Test', () => {
    test.use({
        geolocation: { longitude: 11.57549, latitude: 48.13743 },
        permissions: ['geolocation'],
    });
    test('The bing maps is located to munich', async ({ page }) => {
        const geoName = 'GermanyBavariaMunich (District)';
        await bingMapsPage.goTo();
        await bingMapsPage.locateMe.click();
        await expect(bingMapsPage.geoName.locator).toHaveText(geoName);
    });
});
```

### Tips to test and reuse the same test for different countries

Use a JSON file with text translations and specific selectors for each country. With an environment variable, you can set up the locale to emulate different locales in your laptop or with pipelines.

```typescript
const locale = process.env.LOCALE ? process.env.LOCALE : 'en-US';
const localeInfo = require(`../../data/${locale}.json`);

test.describe('Locale translations', () => {
    test.use({ locale: locale, timezoneId: localeInfo.timezoneId });
    test('Translations with locale and time zone id', async ({ page }) => {
        await homePage.goTo();
        await expect(homePage.googleSearch.locator).toHaveText(localeInfo.googleSearch);
    });
});
```

Thank you for reading. Enjoy testing!!

### LambdaTest geolocation

You can connect to LambdaTest with geoLocation capability in LT:Options to run tests from different countries.

### Synthetic testing

Synthetic testing with Checkly or PerfAgents allows periodic test execution in different AWS zones for localization monitoring.

---

<!-- source: detect-and-handle-flaky-playwright-tests.md -->

# Detect and Handle Flaky Playwright Tests

This guide will help you detect flaky Playwright tests and provide six possible ways to handle them using the Playwright CTRF reporter.

Flaky tests are hugely frustrating for development teams, causing delays and damaging confidence in the testing process. By detecting and handling flaky tests from day one, you enhance the reliability of your test suite.

The Playwright CTRF reporter includes a Test object with flaky and retries properties:

```json
"tests": [
  {
    "name": "User should be able to login",
    "status": "passed",
    "duration": 1200,
    "retries": 2,
    "flaky": true
  }
]
```

## View Flaky Playwright Tests in Github Actions

```bash
npx github-actions-ctrf flaky path/to/ctrf-report.json
```

## Send Alerts To Microsoft Teams

```bash
npx teams-ctrf flaky path/to/ctrf-report.json
```

## Send Alerts To Slack

```bash
npx slack-ctrf flaky path/to/ctrf-report.json
```

## Log Flaky Playwright Tests

```bash
npx ctrf flaky path/to/ctrf-report.json
```

Output:
```
Processing report: reports/sample-report.json
Found 1 flaky test(s) in reports/sample-report.json:
- Test Name: Test 1, Retries: 2
```

## Perform Analytics on Flaky Playwright Tests

The CTRF JSON test object contains flaky and retries properties. If you post your CTRF reports to a database, you can perform analytics on flaky tests such as determining their occurrences and identifying which tests are flaky over time. Track trends across CI runs to prioritize fixing the most problematic tests first.

## Automating Flaky Test Detection with JSON Reports and Shell Scripts

```bash
#!/bin/bash
REPORT_DIR="ctrf"
for REPORT in "$REPORT_DIR"/*.json; do
  jq '.tests[] | select(.flaky == true) | {name: .name, retries: .retries}' "$REPORT"
done
```

You can integrate this script into your CI pipeline to automatically flag flaky tests on every run. Combine with GitHub Actions, Teams, or Slack notifications for immediate team awareness.

## Setting Up the CTRF Reporter

Add the Playwright CTRF reporter to your playwright.config.ts:

```typescript
reporter: [['ctrf-json-reporter', {}]]
```

After each test run, the reporter generates a JSON file with flaky test metadata that all the tools above can consume.

There are many ways to detect and handle flaky playwright tests. The methods using the Playwright CTRF reporter provide more options when it comes to defeating those flaky tests!

## Additional Resources

- Playwright CTRF JSON reporter: https://github.com/ctrf-io/playwright-ctrf-json-report
- GitHub Actions CTRF integration
- Teams and Slack CTRF alerting tools
- CTRF CLI for logging and analytics

Install the reporter in your Playwright config and run your test suite in CI to start detecting flaky tests automatically.

---

<!-- source: handling-unreliable-api-endpoints-with-custom-retry-logic-and-mocking.md -->

# Handling Unreliable API Endpoints with Custom Retry Logic and Mocking

Dealing with unreliable API endpoints can be a significant challenge. In this blog post, we'll explore how to implement custom retry logic and mock responses using Playwright, ensuring our tests remain robust and reliable even when APIs fail.

## The Problem: Verifying API Data

Verifying data fetched from APIs can be a daunting task, especially when dealing with unreliable or slow endpoints:

- **Inconsistent Responses:** APIs can sometimes return incomplete or incorrect data.
- **Timeouts and Failures:** Network instability can cause API requests to fail.
- **Data Variability:** API data can change over time.
- **Dependency on External Services:** Tests depend on third-party uptime.

## Importance of Retries and Mocking

- **Retries:** Implementing retry logic allows tests to make multiple attempts to fetch data.
- **Mocking:** When all retry attempts fail, mocking allows us to simulate API responses.

## Use Case: University Data

We aim to fetch data about universities from an API and display it on a web page. Expected data includes University Name and Source.

## Implementing the Test Script

```javascript
const { test, expect } = require("@playwright/test");

test("Verify university data with retries and mock", async ({ page }) => {
    const url = "http://universities.hipolabs.com/search?name=middle&country=turkey";
    const mockData = { name: "Mocked University", source: "http://mockeduniversity.com" };

    let retries = 3;
    let fetchedData = null;
    let attempt = 0;

    for (let i = 0; i < retries; i++) {
        try {
            attempt++;
            const response = await page.evaluate(() =>
                fetch("http://universities.hipolabs.com/search?name=middle&country=turkey")
                    .then((res) => res.json())
            );
            fetchedData = { name: response[0].name, source: response[0].web_pages[0] };
            break;
        } catch (e) {
            console.error(`Attempt ${i + 1} failed: ${e.message}`);
        }
    }

    if (!fetchedData) {
        fetchedData = mockData;
        await page.route(url, (route) =>
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify([{ name: fetchedData.name, web_pages: fetchedData.source }]),
            })
        );
    }

    await page.goto("http://127.0.0.1:5500/resources/index.html");
    await page.waitForSelector("#name");
    await page.waitForSelector("#source");

    const universityName = await page.textContent("#name");
    const universitySource = await page.textContent("#source");

    expect(universityName).toBe(fetchedData.name);
    expect(universitySource).toContain(fetchedData.source);
});
```

## Conclusion

We explored the importance of handling unreliable API endpoints by implementing custom retry logic and mocking data responses. This approach ensures that our tests can still run and verify expected outcomes even when the API is unreliable.

Happy testing!

---

<!-- source: api-contract-testing-on-frontend-with-playwright.md -->

# API Contract Testing on Frontend with Playwright

Sometimes, as a test engineer, business requirements for testing may be quite weird, and you have to adopt different types of testing in one suite.

Contract testing is a type of software testing that focuses on verifying the interaction between separate components/services. When two microservices interact via the API, one service sends requests in a predefined format, and another responds in a predefined format. This format is called a «contract».

In the case of client-server architecture, the frontend can act as a consumer or provider for various APIs.

**Contract testing can be a part of end-to-end testing** — it can be just a tool for specific checks. This can happen when business requirements require checking that your frontend makes specific requests to third-party APIs in a particular format.

You can isolate frontend from third-party API with Playwright's network capabilities:

1. Roughly abort request — the request will not be sent to an external API;
2. Or mock it.

For POST requests, you intercept the request by `waitForRequest()` for testing request body against your contract.

If your request body is in JSON format, you can use `postDataJSON()` for comparison with Ajv, Zod, or `toEqual()`.

## Code Example

```typescript
import { expect, type Page, test } from '@playwright/test';
import { z } from 'zod';

const schema = z.object({
  jsonrpc: z.string(),
  id: z.number(),
  method: z.string(),
  params: z.array(z.union([z.string(), z.boolean()])),
});

let page: Page;

test.beforeAll(async ({ browser }) => {
  const context = await browser.newContext();
  page = await context.newPage();
  await page.route(/.+lb\.drpc\.org\/ogrpc\?network=ethereum.+/, async (route) => {
    if (route.request().method() === 'POST') {
      await route.abort();
    }
  });
});

test('Open Sushi Swap', async () => {
  const requestPromise = page.waitForRequest(
    (request) =>
      request.url().includes('lb.drpc.org/ogrpc?network=ethereum') &&
      request.method() === 'POST',
  );
  await page.goto('/swap');
  const request = await requestPromise;
  await expect(
    () => schema.parse(request.postDataJSON()),
    'Should have a request by the contract',
  ).not.toThrowError();
});
```

Where:
- `const schema` is a scheme declaration in Zod's format;
- In `beforeAll` hook, all POST requests to matching URLs are blocked;
- `const requestPromise` receives data from the first matching request;
- In `expect()` assertion, the reference scheme is parsed against the request's data.

The test may contain more steps because the contract's check may be just a part of the end-to-end suite.

Read more about contract testing at pactflow.io and playwright.dev/docs/mock.

## Further Reading

- What is contract testing and why should I try it?
- Contract Testing Vs Integration Testing
- A Complete Guide to API Contract Testing
- API contract testing: 4 things to validate to meet expectations
- Contract Testing: The Key to Unlocking E2E Testing Bottlenecks in CI/CD pipelines

Furthermore, the same approach to mocking can be applied to all HTTP API requests on frontend when you need to verify request contracts without hitting external services.

---

<!-- source: google-authentication-with-playwright.md -->

# Google Authentication with Playwright

Use globalSetup with playwright-extra + puppeteer-extra-plugin-stealth to sign in via Google UI once, save storageState to reuse across tests.

## Steps
1. Configure globalSetup and storageState in playwright.config.ts
2. In global-setup: launch chromium with stealth, navigate to login, fill Google credentials (handle old/new form variants), wait for redirect, save storageState
3. Add storage-state.json to .gitignore
4. Use SKIP_AUTH env to skip re-auth during debugging

Note: playwright-extra/stealth maintenance stopped March 2023 — approach may be stale.

---

<!-- source: playwright-interview-questions-that-you-are-going-to-love.md -->

#### 
                    Adrian Maciuc
                

                
        

        
            
                May 17, 2024
                    
                    6 min
            
        

    

                    
Playwright interview questions for Mid and Senior level quality assurance automation engineers 

            

            
                
Following my previous article about playwright interview questions, here are 9 playwright tricky questions that you don't want to fail

## 

## 1. The explicit waits

From testing search functionality point of view how would you improve the code below:

`test("The explicit waits", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/playwright-tips-and-tricks-2/")
  await page.getByText('Playwright tips and tricks #2').scrollIntoViewIfNeeded()
  await expect(page.getByText('Playwright tips and tricks #2')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Search this site' })).toBeVisible()
  await page.getByRole('button', { name: 'Search this site' }).click()
  await expect(page.frameLocator('iframe[title="portal-popup"]').getByPlaceholder('Search posts, tags and authors')).toBeVisible()
  await page.frameLocator('iframe[title="portal-popup"]').getByPlaceholder('Search posts, tags and authors').fill("Cypress")
  await expect(page.frameLocator('iframe[title="portal-popup"]').getByRole('heading', { name: 'Cypress' }).first()).toContainText("Cypress")
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            
- Remove all the `toBeVisible()` expects
- Remove the `scrollIntoViewIfNeeded()`
- Store iframes in a constant for reuse and readability
- Use regex in your locators to be able to use partial text. Such as `/Search posts/` instead of `Search posts, tags and authors`see code and explanation at the end of the page...

        

## 2. The visible methods

What is this code going to do:

`test("The visible methods", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await expect(page.getByRole('link', { name: 'About' }).isVisible())
});`Possible answers:

- Test will fail because isVisible() is not a valid method that can be used
- Test will fail with an error about property&nbsp;'then'
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with an error . Error:&nbsp;expect:&nbsp;Property&nbsp;'then'&nbsp;not&nbsp;found 

see explanation at the end of the page...

        

## 3. The ninja click

Given the code below, what do you think it will happen

`test("The ninja", async ({ page }) =&gt; {
  await page.goto("https://www.clickspeedtester.com/mouse-test/");
  await page.getByRole('link', { name: 'Second Clicker' }).click({ trial: true })
  await page.waitForURL("**/clicks-per-second-test/")
})`Possible answers:

- Test will fail with error page.waitForURL:&nbsp;Test&nbsp;ended, because click was not performed
- Test will fail because `waitForURL()` argument is not in valid format
- Test will fail at click step, there is no such thing as `trial:true`
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with error page.waitForURL:&nbsp;Test&nbsp;ended. With trial:true Playwright performs the&nbsp;actionability&nbsp;checks but skips the action of click. 

        

## 4. The you OK ?

Given the code below, what do you think it will happen 

`test("The you OK", async ({ page }) =&gt; {
  const response = await page.request.get('https://blog.martioli.com/');
  await expect(response).toBeOK();
})`Possible answers:

- Test will fail because there is no such thing as `toBeOK()`
- Test will fail because `page` does not have `request`
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass (with the condition that the website is up and running) . toBeOK() is a method that ensures the response status code is within&nbsp;`200..299`&nbsp;range

        

## 5. The special word

Given the element has the text "Be the first to discover new tips and tricks about automation in software development" , in the code below, what do you think will happen 

`test("The innerText?", async ({ page }) =&gt; {
  await page.goto('https://blog.martioli.com');
  const innertText = page.locator(".gh-subscribe-description").innerText()
  await expect(innertText).toContain("Be the first to discover new tips")
});`Possible answers:

- Test will pass
- Test will fail with Error:&nbsp;expect Received&nbsp;object:&nbsp;{}
- Test will fail because we cannot use `toContain()` on `innerText()`
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with Error:&nbsp;expect Received&nbsp;object:&nbsp;{} . Since we forgot the `await` key for the `innerText()` method to resolve the promise and extract the text.

        

## 6. The magic filter

What is the best and most recommended way to filter tests ?

            
                
#### Answer

                
                    
                        

                    
                
            
            Tags are the most simple and efficient way to filter your tests

        

## 7. The fail one

Given the code below and the fact that I am not an astronaut, what do you think will happen 

`test("The fail", async ({ page }) =&gt; {
  test.fail()
  await page.goto("https://www.martioli.com/");
  await expect(page.getByText('Astronaut')).toBeVisible()
});`Possible answers:

- Test will pass because of the `test.fail()` method applied
- Test will fail because of the `test.fail()` method applied
- Test will perform all the steps but still have a result as fail
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass because of the `test.fail()` method applied 

Why ? because it will not find the word "Astronaut" on my portofolio website and because it fails to find it, then our expectation of our test overall to fail is a success and the test will pass

        

## 8. The health check

Given the code below, what do you think will happen and how you can improve the code

`const locales = [
  "de",
  "com",
  "es"
]

for (const location of locales) {
  test(`check health: ${location}`, async ({ page }) =&gt; {
    const response = await page.request.get(`https://www.google.${location}/`)
    expect(response).toBeOK()
  });
}`Possible answers:

- Test will pass
- Test will fail because you cannot do such `for loops`
- Test will fail because expect has no `await` key
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass. You can do such checks in Playwright, just to be careful to put some delay inside the test before iteration goes to next item, to avoid ruining your test environments.

If you are wondering why it works expect without the await key see explanation at the end of the page...

        
 

## 9. The page one

Given the code below, what do you think will happen 

`test("The page one", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await expect(getByText('Recommended Resources')).toBeVisible()
});`Possible answers:

- Test will pass because we have `Recommended Resources`
- Test will fail because of Reference Error
- Test will fail because there is no `Recommended Resources` text on my blog
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with ReferenceError: getByRole is not defined . Notice that we have `expect(getByText` instead of `expect(page.getByText.`

        Become a member for free 
Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Playwright Interview Questions that you are going to hateEverybody hates it when they go to an interview and they are given live coding challenge that is written intentionally to make you fail, so the interviewer can prove his “superiority”. Here are 12 playwright interview questions and answers, so you can have the advantage this time.MartioliAdrian Maciuc
### Content below is visible to members only

My blog has a members section. Its free to join, there is no spam and you get access to members only content. Become a member now and you will also be the first to receive in your email when I publish a new article.

    
                
## This post is for subscribers only

            Subscribe now
            
Already have an account? Sign in

---

<!-- source: the-green-report-frontend-performance-testing-with-playwright-and-lighthouse.md -->

- 
    
    

    
    
    
    
    
    

    
    
    
    
    
    
    

    
    
    
    
    
    
    The Green Report | Frontend Performance Testing with Playwright and Lighthouse
    
    
    
    
    
    
    
    
    

    
    
        
            
                
## The Green Report

            

            
                
                    
                        Home
                    
                    
- 
                        Categories
                    
                    
- 
                        Resources
                    
                    
- 
                        Guides
                    
                    
- 
                        About
                    
                    
- 
                        Contact
                    
                
            

            
                
                    
                    
                

                
                    
                

                
                    
                    
                
            
        
    

    
    
        
            
                
                
                    
                
            
            Or press ESC to close.
        

        
            
        
    
    

    
        
            
                
### Frontend Performance Testing with Playwright and Lighthouse
                

                
                    May 19th 2024
                    
                    11 min read
                
                
            
            
                
                    easy
                    performance
                    web
                    
                        
                            javascriptES6
                        
                    
                    
                        
                            playwright1.44.0
                        
                    
                    
                        
                            lighthouse12.0.0
                        
                    
                    github
                    cicd
                
                

                    The performance of our website's frontend plays a crucial role in user experience and search engine
                    rankings. Slow load times and unresponsive interfaces can drive visitors away, impacting our site's
                    engagement and conversion rates. To ensure our website performs optimally, automated tools like
                    Lighthouse and Playwright can be invaluable.
                

                

                    Lighthouse, a powerful open-source tool from Google, audits web pages for
                    performance,
                    accessibility, SEO, and more. When combined with Playwright, a versatile browser automation library,
                    we can create a robust performance testing setup to continuously monitor and enhance our site's
                    frontend.
                

                
#### Why Frontend Performance Matters

                

                    Frontend performance directly impacts user experience and SEO. A slow or unresponsive website can
                    frustrate users, leading to higher bounce rates and lower engagement. In contrast, a fast and smooth
                    site keeps users satisfied, encouraging them to stay longer and interact more with our content. This
                    user satisfaction translates into better conversion rates, whether we're driving sales, collecting
                    leads, or simply increasing readership.
                

                

                    From an SEO perspective, search engines like Google prioritize websites that deliver excellent user
                    experiences. Performance metrics are factored into search ranking algorithms, meaning a slow site
                    can result in lower search engine rankings, reducing our site's visibility and organic traffic.
                    Google's Core Web Vitals, a set of performance metrics, are particularly influential in this regard.
                

                

                    Understanding and improving frontend performance involves focusing on several key metrics:
                

                
                    
- 
                        First Contentful Paint (FCP): Measures the time from when the page starts loading to when
                        any part of the page's content is rendered on the screen. FCP is crucial because it gives users
                        the first visual feedback that the page is loading.
                    
                    
- 
                        Largest Contentful Paint (LCP): Marks the time it takes for the largest piece of content
                        to become visible within the viewport. This could be an image, video, or block of text. LCP is a
                        key indicator of when the main content of the page has likely loaded.
                    
                    
- 
                        Time to Interactive (TTI): Tracks the time from when the page starts loading to when it
                        is fully interactive, meaning the user can reliably interact with the page. TTI is important for
                        ensuring that users can quickly and effectively use our website without delays.
                    
                    
- 
                        First Input Delay (FID): Measures the time from when a user first interacts with our
                        site (e.g., clicks a link or taps a button) to when the browser begins processing that
                        interaction. FID is critical for user experience, as delays in responding to user inputs can be
                        frustrating.
                    
                
                

                    These metrics are essential for understanding the performance of our frontend and identifying areas
                    for improvement. By monitoring and optimizing these metrics, we can ensure that our website provides
                    a fast, smooth, and enjoyable experience for users, which can lead to higher engagement, better SEO
                    rankings, and improved overall success for our site.
                

                
#### Tools Overview

                

                    To effectively measure and optimize frontend performance, leveraging the right tools is essential.
                    Two powerful tools for this purpose are Lighthouse and Playwright.
                

                

                    Lighthouse is an open-source, automated tool developed by Google that audits web pages for
                    various
                    performance aspects, including accessibility, SEO, and best practices. It provides detailed reports
                    highlighting areas for improvement and offers actionable recommendations. Key capabilities of
                    Lighthouse include:
                

                
                    
- 
                        Performance Audits: Analyzes critical performance metrics like FCP, LCP, TTI, and FID.
                    
                    
- 
                        Accessibility Checks: Ensures our site is usable by people with disabilities.
                    
                    
- 
                        SEO Audits: Checks for search engine optimization best practices.
                    
                    
- 
                        Best Practices: Evaluates security and general web development best practices.
                    
                
                

                    Lighthouse can run as a Chrome DevTools extension, a command-line tool, or programmatically through
                    Node.js, making it versatile for various use cases.
                

                

                    Playwright is a robust library for browser automation developed by Microsoft. It allows us to
                    script
                    browser interactions, automate testing, and perform end-to-end testing across multiple browsers like
                    Chromium, Firefox, and WebKit. Key capabilities of Playwright include:
                

                
                    
- 
                        Cross-Browser Testing: Supports automation across different browsers to ensure consistent
                        performance.
                    
                    
- 
                        Headless Browser Support: Runs browsers in headless mode for faster and more efficient
                        testing.
                    
                    
- 
                        Advanced Interactions: Simulates complex user interactions and navigation scenarios.
                    
                    
- 
                        Screenshot and Video Capture: Records browser sessions for debugging and analysis.
                    
                
                

                    Combining Lighthouse and Playwright brings together the strengths of both tools, providing a
                    comprehensive performance testing solution. Here's why this combination is beneficial:
                

                
                    
- 
                        Automated Performance Audits: Playwright can automate the process of loading web pages,
                        while Lighthouse conducts in-depth performance audits. This ensures consistent and repeatable
                        performance testing.
                    
                    
- 
                        Cross-Browser Performance Insights: By using Playwright's ability to automate multiple
                        browsers, we can assess how our site performs across different environments, identifying
                        browser-specific issues.
                    
                    
- 
                        End-to-End Testing: Playwright scripts can navigate through user journeys and scenarios,
                        triggering Lighthouse audits at critical points to measure performance during real user
                        interactions.
                    
                    
- 
                        Continuous Integration and Delivery: Both tools can be integrated into CI/CD pipelines,
                        enabling continuous performance monitoring and immediate feedback on performance regressions.
                    
                
                

                    By leveraging Lighthouse and Playwright together, we gain a powerful toolkit for automating and
                    enhancing frontend performance testing. This integrated approach ensures that our website not only
                    meets performance standards but also provides an optimal user experience across various browsers and
                    devices.
                

                
#### Implementing the Performance Testing Tool

                

                    To begin automating our frontend performance testing with Lighthouse and Playwright, we'll need to
                    install the necessary libraries. Use the following command to install Playwright, Lighthouse, and
                    the playwright-lighthouse package:
                

                
                    
                        
                    
                    

                        `               
npm install --save-dev playwright lighthouse playwright-lighthouse`
                      
                
                
                    This command will install Playwright and Lighthouse, allowing us to use them in our
                    performance-testing scripts.
                

                
##### Writing the Script

                

                    First, we import the necessary modules for Playwright and Lighthouse integration.
                

                
                    
                        
                    
                    

                        `               
import { test } from "@playwright/test";
import playwright from "playwright";
import { playAudit } from "playwright-lighthouse";`
                      
                
                
                    Then, we use Playwright's test function to define and run the
                    audit.
                

                
                    
                        
                    
                    

                        `               
test&#40;'Run Lighthouse audit on thegreenreport.blog', async ({ page }) => &#123;
    const targetURL = 'https://www.thegreenreport.blog';
    const port = 9222; // Replace with your desired port if needed`
                      
                
                
                    Next, we will launch a headless Chromium browser with remote debugging enabled on the specified
                    port.
                

                
                    
                        
                    
                    

                        `               
const browser = await playwright.chromium.launch({
    args: [`--remote-debugging-port=${port}`],
});`
                      
                
                
                    After launching Chromium we create a new browser context and page, then navigate to the target URL.
                

                
                    
                        
                    
                    

                        `               
page = await browser.newPage();
await page.goto(targetURL);`
                      
                
                
                    We can now use the playAudit function to run Lighthouse on the
                    current page. This function requires
                    the remote debugging port and thresholds for performance metrics.
                

                
                    
                        
                    
                    

                        `               
await playAudit({
    page: page,
    port: port,
    thresholds: {
        performance: 85,
        accessibility: 85,
        'best-practices': 85,
        seo: 85,
    },
});`
                      
                
                
                    After the audit is completed, we just need to close the browser to free up resources.
                

                
                    
                        
                    
                    

                        `               
    await browser.close();
&#125;&#41;;`
                      
                
                
#### Automating and Integrating with CI/CD

                
                    Once we've set up our performance testing tool with Playwright and Lighthouse, the next step is to
                    automate these tests and integrate them into our CI/CD pipeline. This ensures that performance tests
                    are run automatically with every code push, helping to maintain optimal performance and catch issues
                    early.
                

                

                    Integrating our performance tests into a CI/CD pipeline using GitHub Actions
                    is straightforward.
                    Here's how we can set up a GitHub Actions workflow to run our performance tests on every push to the
                    main branch.
                

                

                    The code starts by defining the name of the workflow using the name keyword. In this case, the
                    workflow is named Frontend Performance Test. This name helps
                    identify the purpose of the workflow.
                

                
                    
                        
                    
                    

                        `               
name: Frontend Performance Test`
                      
                
                
                    The on keyword specifies when the workflow should be executed.
                    Here, it's configured to run upon a
                    push event (push). Further restricting the trigger, it only runs
                    when there's a push to the main
                    branch (branches: [main]). This means the workflow won't run for
                    pushes to other branches.
                

                
                    
                        
                    
                    

                        `               
on:
  push:
      branches: [main]`
                      
                
                
                    The jobs section defines the specific tasks the workflow will
                    perform. Here, there's a single job
                    named performance-test.
                

                

                    The runs-on keyword defines the runner environment where the
                    job's steps will be executed. In this
                    case, it's set to windows-latest, indicating the job will run on
                    a virtual machine with the latest
                    version of Windows.
                

                
                    
                        
                    
                    

                        `               
jobs:
  performance-test:
    runs-on: windows-latest`
                      
                
                
                    The steps section defines a sequence of commands that the
                    workflow will run. Each step has a name
                    property that describes its purpose and a set of actions to be performed.
                

                
                    
- 
                        Checkout Code: This step utilizes an existing action from GitHub Marketplace
                        called actions/checkout@v4. This action retrieves the code
                        from the repository associated with
                        the workflow.

                    
                    
- 
                        Set Up Node.js: Another pre-built action, actions/setup-node@v4, is used here. This
                        action configures the runner environment to have Node.js installed. Additionally, the with
                        keyword provides arguments to the action. In this case, it specifies the desired Node.js version
                        (node-version: "20") for the job.
                    
                    
- 
                        Install Dependencies: This step simply runs the command npm
                            install to install all the
                        project's dependencies listed in the package.json file.
                        These dependencies are likely required
                        for running the performance tests.
                    
                    
- 
                        Install Playwright Browsers: The step executes the command npx playwright install to
                        download the necessary browser binaries for Playwright.
                    
                    
- 
                        Run Performance Tests: The final step executes the command npx playwright test which
                        triggers the actual performance tests defined within the project.
                    
                
                
                    
                        
                    
                    

                        `               
steps:
  - name: Checkout code
    uses: actions/checkout@v4
                      
  - name: Set up Node.js
    uses: actions/setup-node@v4
    with:
      node-version: "20"
                      
  - name: Install dependencies
    run: npm install
                      
  - name: Install Playwright browsers
    run: npx playwright install
                      
  - name: Run performance tests
    run: npx playwright test`
                      
                
                
                    With this configuration, our performance tests will be automatically executed as part of our CI/CD
                    pipeline, ensuring that every code push is evaluated for frontend performance. This integration
                    helps maintain a high performance standard and quickly identifies any performance regressions.
                

                
                
                    &times;
                    
                
                
Example of a GitHub Actions log where the performance test failed

                
#### Reviewing and Optimizing Performance

                

                    Once our performance tests are automated and integrated into our CI/CD pipeline, it's essential to
                    regularly review and optimize our application's performance based on the results. This section will
                    guide you through analyzing the Lighthouse report, identifying performance bottlenecks, and
                    establishing strategies for continuous improvement.
                

                
Tips for Identifying and Addressing Performance Bottlenecks:

                
                    
- 
                        Render-Blocking Resources: Identify and minimize render-blocking scripts and stylesheets
                        that delay FCP and LCP.
                    
                    
- 
                        Image Optimization: Ensure images are properly optimized, using formats like WebP, and
                        implement lazy loading for offscreen images.
                    
                    
- 
                        Efficient Code Splitting: Break up large JavaScript bundles into smaller, more manageable
                        chunks that can be loaded on demand.
                    
                    
- 
                        Minify and Compress: Minify CSS, JavaScript, and HTML files and use compression (e.g.,
                        Gzip, Brotli) to reduce file sizes.
                    
                    
- 
                        Leverage Browser Caching: Implement strong caching policies to reduce load times for
                        returning visitors.
                    
                
                
Strategies for Continuous Performance Monitoring:

                

                    Continuous performance monitoring involves regularly running performance tests and analyzing the
                    results to identify trends and areas for improvement. Here are some strategies:
                

                
                    
- 
                        Scheduled Performance Tests: Set up scheduled runs of your performance tests to
                        continuously monitor key metrics and ensure they stay within acceptable thresholds.
                    
                    
- 
                        Alerting and Notifications: Configure alerts to notify your team if performance metrics
                        fall below specified thresholds, allowing for immediate action.
                    
                    
- 
                        Performance Budgets: Establish performance budgets for metrics like FCP, LCP, TTI, and
                        FID. Integrate these budgets into your CI/CD pipeline to enforce performance standards.
                    
                    
- 
                        Performance Regression Testing: Incorporate performance regression tests to ensure that
                        new changes do not negatively impact performance.
                    
                
                
Importance of Regularly Running Performance Tests as Part of the Development
                    Workflow:
                

                

                    Regularly running performance tests as part of our development workflow ensures that performance is
                    a continuous priority rather than an afterthought. This proactive approach helps:
                

                
                    
- 
                        Catch Regressions Early: Identify performance issues early in the development process,
                        making them easier and less costly to fix.
                    
                    
- 
                        Maintain User Experience: Ensure a consistently high-quality user experience, which is
                        crucial for user satisfaction and retention.
                    
                    
- 
                        Improve SEO: Maintain and improve SEO rankings, as performance is a key factor in search
                        engine algorithms.
                    
                    
- 
                        Boost Developer Productivity: Equip developers with actionable insights to optimize their
                        code, leading to a more efficient development process.
                    
                
                

                    By integrating continuous performance monitoring and regular testing into our development workflow,
                    we create a culture of performance excellence, driving ongoing improvements and ensuring our
                    application remains fast and responsive.
                

                
#### Conclusion

                

                    In this blog post, we've explored the crucial role of frontend performance in enhancing user
                    experience and SEO. By leveraging automated testing tools like Lighthouse and Playwright, we can
                    efficiently monitor and optimize our web applications to meet high-performance standards.
                    Implementing these methods into our development workflow not only helps catch regressions early but
                    also fosters a culture of continuous improvement.
                

                

                    You can view the complete code example and the GitHub Actions configuration in our repository.
                

            
        
    

    
    
        
            
                
                    
## The Green Report

                
                

                    A place for everyone interested in quality assurance and automation
                    testing. The only goal is to learn something new!
                

                
                    
- 
                        
                            
                        
                        
                            
                        
                    
                
                &copy;  The Green Report. All rights
                    reserved.
            

            
                
###### Categories

                
                    
- 
                        Manual Testing
                    
                    
- 
                        UI Testing
                    
                    
- 
                        API Testing
                    
                    
- 
                        Mobile Testing
                    
                    
- 
                        Desktop App Testing
                    
                    
- 
                        Reporting
                    
                
            

            
                
###### Blog

                
                    
- 
                        Privacy Policy
                    
                    
- 
                        Terms and Conditions
                    
                    
- 
                        Cookies
                    
                    
- 
                        Credits
                    
                
            

            
                
###### Useful links

                
                    
- 
                        Work With Us
                    
                    
- 
                        Resources
                    
                    
- 
                        Guides

---

<!-- source: speed-up-your-playwright-tests.md -->

![Abstract illustration of one long serial test run replaced by six parallel runs finishing far earlier](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmain.44i28n510b_-t.jpg&w=3840&q=75)

Playwright is an incredible tool for writing E2E tests on CI, but setup time can drag down your productivity. Imagine cutting that time in half! In this article, we'll dive into game-changing techniques to double the speed of your Playwright tests, including Docker, caching, and parallel execution. Let's supercharge your CI pipeline!

## Use Playwright Docker image

The quickest way to run tests on CI is by using the [official Playwright Docker image](https://playwright.dev/docs/docker). This image comes with all browsers and their dependencies pre-installed, saving you precious time on setup.

Here's how to run a step directly in a Docker container with GitHub Actions:

```
# .github/workflows/playwright-tests.yml
name: Playwright tests

jobs:
  playwright-test:
    runs-on: ubuntu-latest

    steps:
      # Other steps...

      - name: Run Playwright tests
        uses: docker://mcr.microsoft.com/playwright:v1.44.0-jammy
        with:
          args: npm exec -- playwright test
```

### Fix Firefox permissions issue

For Firefox, you might encounter a permission error. To fix this, run tests with `HOME=/root`:

```
- name: Run Playwright tests
  uses: docker://mcr.microsoft.com/playwright:v1.44.0-jammy
  with:
    # Fix for Firefox, HOME=/root is required to avoid permission issues
    # https://github.com/microsoft/playwright/issues/6500
    args: env HOME=/root npm exec -- playwright test
```

### Communicating with service containers

When running jobs in a container, GitHub uses Docker's networks to connect service containers. This means you can't use `127.0.0.1` or `localhost` to access these services. Instead, use the service's name defined in your workflow. For example, replace `localhost` with the name of your service.

Here's an example of how to connect to a PostgreSQL database:

```
# .github/workflows/playwright-tests.yml
name: Playwright tests

jobs:
  playwright-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:13-alpine
        ports:
          - 5432:5432
        env:
          POSTGRES_HOST_AUTH_METHOD: trust

    steps:
      # Other steps...

      - name: Run Playwright tests
        uses: docker://mcr.microsoft.com/playwright:v1.44.0-jammy
        with:
          args: npm exec -- playwright test
        env:
          DATABASE_URL: postgresql://postgres@postgres/test
```

Containers are isolated from the GitHub Actions worker. If your server needs to access multiple services, achieving communication in a Docker container can be challenging. In such cases, run Playwright directly on the GitHub Action runner.

Checkout [Argos Playwright setup](https://github.com/argos-ci/argos/blob/c13be4fae223b135c810dd89409ce2b0edfb2e70/.github/workflows/ci.yml#L178-L187) for a real example.

## Optimize Playwright installation

Running tests in Playwright requires installing browsers and their dependencies. Playwright provides two commands for this:

- `playwright install [browsers...]`: Installs specified browsers (Chromium, Firefox, etc.).
- `playwright install-deps [browsers...]` Installs necessary libraries.

### Install only specific browsers

To speed up installation, install only the browsers you need. For example, to install only Chromium:

```
# .github/workflows/playwright-tests.yml
name: Playwright tests

jobs:
  playwright-test:
    runs-on: ubuntu-latest

    steps:
      # Other steps...

      - name: Install Playwright dependencies
        run: npm exec -- playwright install --with-deps chromium

      - name: Run Playwright tests
        run: npm exec -- playwright test
```

### Cache browsers installation

Caching dependencies is tricky since they are system-installed, but browsers are installed in `~/.cache/ms-playwright`, making them cacheable.

Create a composite action to set up Playwright and cache browsers:

```
# .github/actions/setup-playwright/action.yml
name: Setup Playwright
description: Install Playwright and dependencies
runs:
  using: "composite"
  steps:
    # Run npm ci and get Playwright version
    - name: 🏗 Prepare Playwright env
      shell: bash
      run: |
        PLAYWRIGHT_VERSION=$(npm ls --json @playwright/test | jq --raw-output '.dependencies["@playwright/test"].version')
        echo "PLAYWRIGHT_VERSION=$PLAYWRIGHT_VERSION" >> $GITHUB_ENV

    # Cache browser binaries, cache key is based on Playwright version and OS
    - name: 🧰 Cache Playwright browser binaries
      uses: actions/cache@v4
      id: playwright-cache
      with:
        path: "~/.cache/ms-playwright"
        key: "${{ runner.os }}-playwright-${{ env.PLAYWRIGHT_VERSION }}"
        restore-keys: |
          ${{ runner.os }}-playwright-

    # Install browser binaries & OS dependencies if cache missed
    - name: 🏗 Install Playwright browser binaries & OS dependencies
      if: steps.playwright-cache.outputs.cache-hit != 'true'
      shell: bash
      run: |
        npm exec -- playwright install --with-deps chromium

    # Install only the OS dependencies if cache hit
    - name: 🏗 Install Playwright OS dependencies
      if: steps.playwright-cache.outputs.cache-hit == 'true'
      shell: bash
      run: |
        npm exec -- playwright install-deps chromium
```

Update your workflow to use the composite action:

```
# .github/workflows/playwright-tests.yml
name: Playwright tests

jobs:
  playwright-test:
    runs-on: ubuntu-latest

    steps:
      # Other steps...

      - name: Setup Playwright
        uses: ./.github/actions/setup-playwright

      - name: Run Playwright tests
        run: npm exec -- playwright test
```

## Run tests in parallel

By default, tests in a spec are not run in parallel, which can slow down the overall test execution time. To optimize, enable parallel test execution by setting `fullyParallel: true` in your Playwright configuration.

### Enabling full parallelism

Add the following to your Playwright configuration file:

```
// playwright.config.ts
import { defineConfig } from "@playwright/test";

export default defineConfig({
  fullyParallel: true,
  // Other configurations...
});
```

### Controlling parallel mode by spec

You can control the parallel execution mode for individual test suites using `test.describe.configure`. This allows for more granular control over which tests should run in parallel:

```
// example.spec.ts
import { expect, test } from "@playwright/test";

test.describe.configure({ mode: "parallel" });

test.describe("My test suite", () => {
  test("Test 1", async ({ page }) => {
    // Test implementation...
  });

  test("Test 2", async ({ page }) => {
    // Test implementation...
  });
});
```

For more details on parallel test execution, refer to the [Playwright documentation](https://playwright.dev/docs/test-parallel).

If you want to know every tricks about Playwright, I recommend the courses from [Bondar Academy](https://www.bondaracademy.com/) especially the [Master test automation with Playwright course](https://www.bondaracademy.com/course/sdet-with-playwright) that includes the setup of Argos.

## Conclusion

Setting up Playwright can be time-consuming on CI, especially if you shard your tests. By using these techniques, you can save significant setup time, enhancing your team's productivity. Enabling parallel test execution can further speed up your tests, making your CI pipelines even more efficient.

For an even more streamlined experience, consider integrating [Argos with Playwright](https://argos-ci.com/docs/quickstart/playwright-quickstart). Argos provides powerful visual testing capabilities that complement your E2E tests perfectly. Learn how to get started with Argos and Playwright by checking out our [quickstart guide](https://argos-ci.com/docs/quickstart/playwright-quickstart). Enhance your testing workflow and catch visual regressions effortlessly!

### Read also

[![Abstract illustration of a deployment branching into a preview panel that feeds a visual comparison](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmain.379dta6i0pal1.jpg&w=3840&q=75)\\
\\
Guides\\
\\
**How to Integrate Argos Visual Testing with Vercel Preview** \\
\\
![Greg Bergé](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fgreg.2he0tam7a4dwu.jpg&w=1080&q=75)Apr 1, 2024](https://argos-ci.com/blog/run-argos-on-vercel-preview) [![Abstract illustration of three browser windows side by side with the rightmost one highlighted](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmain.0mgsdybp1z13k.jpg&w=3840&q=75)\\
\\
Guides\\
\\
**Percy vs Chromatic vs Argos: 2026 Visual Testing Comparison** \\
\\
![Jeremy Sfez](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fjeremy.1o90laqvb3s4l.jpg&w=1080&q=75)Jul 16, 2026](https://argos-ci.com/blog/percy-vs-chromatic-vs-argos) [![Abstract illustration of two identical layouts where one element has shifted out of place and is highlighted](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmain.446k6pupl_0mq.jpg&w=3840&q=75)\\
\\
Guides\\
\\
**The Importance of Visual Testing in Ensuring UI Quality** \\
\\
![Jeremy Sfez](https://argos-ci.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fjeremy.1o90laqvb3s4l.jpg&w=1080&q=75)Dec 12, 2022](https://argos-ci.com/blog/visual-testing)

## Superchargeyour product quality

See every change your team and your agents make. Review with confidence, and merge faster.

[Sign up](https://app.argos-ci.com/signup) [Request demo](https://cal.com/gregberge)

---

<!-- source: playwright-interview-questions-that-you-are-going-to-hate.md -->

#### 
                    Adrian Maciuc
                

                
        

        
            
                May 5, 2024
                    
                    8 min
            
        

    

                    
Everybody hates it when they go to an interview and they are given live coding challenge that is written intentionally to make you fail, so the interviewer can prove his &quot;superiority&quot;. Here are 12 playwright interview questions and answers, so you can have the advantage this time.

            

            
                

Questions were created with the idea to trick you, so don't be too hard on yourself if you don't get them right. Also, some answers have an explanation and even code examples. Scroll all the way to the bottom for that. Remember, this is functional code with real URLs used, so feel free to try the code out, copy paste it and see for yourself.

## 

## 1. The local environment

If you would run your tests on your local machine using your local environment that you need to setup and get it up and running, for example at http://localhost:3000/ , how would you handle the local environment setup during test runs?

Possible answers:

- Start up your local environment prior to running your tests
- Write a script in package.json to start local environment and run your tests
- Use webServer
            
                
#### Answer

                
                    
                        

                    
                
            
            All possible answers are valid but the recommended one is to use webServer config

see explanation at the end of the page...

        

## 2. The contain comparison

Other than the fact that one takes a `locator` and the other a `value`, what is the main difference between the followings: 

`await expect(locator).toContainText()`

and

`await expect(value).toContain()`

            
                
#### Answer

                
                    
                        

                    
                
            
            First one is an auto-retry assertion meaning that it will retry up to 5 seconds for the element to appear and the second will attempt to assert once.

        

## 3. The delayed load

At the URL below, text "Loading complete" will appear after 10 seconds. Our test is failing at the expect step with `Expected:&nbsp;visible , Received:&nbsp;hidden`. How could you fix this test ?

`test("The visible methods", async ({ page }) =&gt; {
  await page.goto("https://webdriveruniversity.com/Accordion/index.html");
  await expect(page.getByText("LOADING COMPLETE.")).toBeVisible()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            Default expect timeout is 5 seconds, if element is not visible within that time interval, test will fail. To fix the test we extend the timeout only for this step by adding a timeout above our known 10 seconds delay, like so: `await expect(page.getByText("LOADING COMPLETE.")).toBeVisible({ timeout: 12000 })`

        

## 4. The self-healing

Knowing that magento.softwaretestingboard.com has a menu item called "Sale" that upon clicking it will navigate to a new page. Complete the test below with writing a new step that will click on "Sale" menu option but create for that element a self-healing locator

`test("The self-healing", async ({ page }) =&gt; {
  await page.goto("https://magento.softwaretestingboard.com/");
  await page.getByLabel('Consent', { exact: true }).click()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            A self-healing locator is when you point multiple locators to the same element but you chain them together with the use of a helper method or with the OR operator. Using the or operator you can provide multiple locators that will point to the same element, in case one is broken due to app change it can try to use the other one

see code example at the end of the page...

        
## 5. The closed browser

What is this code going to do:

`test("The closed browser", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await page
    .getByRole('link', { name: 'About' })
    .click();
  expect(page).toHaveURL(/about/);
});`Possible answers:

- Test will fail because About is a button not a link
- Test will error out about target page
- Test will fail because of the argument passed to .toHaveURL()
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will error out about target page. Error:&nbsp;expect.toHaveURL:&nbsp;Target&nbsp;page,&nbsp;context&nbsp;or&nbsp;browser&nbsp;has&nbsp;been&nbsp;closed

see explanation at the end of the page...

        

## 6. The reference

Knowing that the element clicked on the second step is a backlink on a page, what is this code going to do:

`test("The reference", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/playwright-tips-and-tricks-1/");
  await page
    .locator('section').locator('p').locator('a').getByText('buy me a coffee').click()
  await expect(page).toHaveURL(/blog.martioli.com/)
});`Possible answers:

- Test will fail because 'buy me a coffee' is a link and we navigated away from martioli.com
- Test will pass
- Test will fail because you cannot mix locator().locator().locator()
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, because clicking a backlink will generate an URL with a reference about our initial origin (blog.martioli.com)

see explanation at the end of the page...

        

## 7. The other locales

Modify the test below and have it run with a different locale, for example german. 

`test("The locale", async ({ page }) =&gt; {
  await page.goto("https://www.google.com/");
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            You can modify configurations from within your test. Just add above the test `test.use({locale: 'de-DE'})`

        

## 8. The CSS properties

Given the code below how could you improve it for readability and simplicity.

`test("The css properties", async ({ page }) =&gt; {
  await page.goto("https://magento.softwaretestingboard.com/");
  await page.getByLabel('Consent', { exact: true }).click()
  
  const element = page.getByText("Shop New Yoga")
  const backgroundColor = await element.evaluate((el) =&gt; {
    return window.getComputedStyle(el).getPropertyValue('background-color');
  });
  expect(backgroundColor).toBe("rgb(25, 121, 195)")
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            You don't need to call evaluate when you already have built-in method called toHaveCSS() . Remember also that `toHaveCSS()` has auto-retry, which is crucial if you wait for certain element properties to appear with delay.

see code example at the end of the page...

        

## 9. The long click

Given the code below, how would you modify the click step to simulate hold left mouse button for 3 seconds and then release mouse button. 

`test("The long click", async ({ page }) =&gt; {
  await page.goto("https://www.clickspeedtester.com/mouse-test/");
  await page.getByRole('link', { name: 'Second Clicker' }).click()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            Click can take optionally an object with property delay and value in milliseconds. Like so: `.click({delay: 3000})` pair this with an element and it will hold for 3 seconds on that element then release the mouse button

        

## 10. The force

The page has a data consent popup modal that you have to comply with. The "search" button is hidden behind this modal. Given the following code. What will it happen

`test("The force", async ({ page }) =&gt; {
  await page.goto("https://www.google.com/");
  
  // the search input field
  await page.locator('textarea').first().fill("martioli")
  // the search button
  await page.locator('[type=submit][name="btnK"]').last().click({ force: true })
});`Possible answers:

- Test will pass. I am using `{force:true}` , it will click the button and results will show behind the modal
- Test will pass, but no results shown behind the modal
- Test will fail, timeout, because it cannot click on the button behind the modal
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, but no results shown behind the modal. `force:true` does not click if element is under another element, but it will also not error out (bug in playwright?). Its not the same for `fill()`. If you check the input, it had no problems in typing in the input field.

        
## 11. The baseUrl

I have my baseUrl set up in my config file.  What is this code going to do:

`test("The baseurl", async ({ page }) =&gt; {
  await page.goto('');
});`Possible answers:

- Test will fail. I forgot the `/` between the quotes
- Test will fail because `.goto()` needs double quotes
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, an empty string does the same as "/" . It will navigate to baseUrl configured in playwright.config.js

        
## 12. The workers

Given the below configuration about workers. If I run a suite of 100 tests, how would they run:

`module.exports = defineConfig({
  testDir: "./tests",
  workers: "5%",

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
`Possible answers:

- Tests will run very slow with only 5% power of the workers
- Test will run on 5% of how many CPU cores you have.
- Test will not even start because you cannot set values in percentages for workers
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will run on 5% of how many CPU cores you have. For example if you have 4 cores, it means out of 100% 4 would mean each 25%. In our case 5 is under 25% so you will get just one worker

        Become a member for free 

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Playwright interview questions that you are going to lovePlaywright interview questions for Mid and Senior level quality assurance automation engineersMartioliAdrian Maciuc
My blog has a members section. Its free to join, there is no spam and you get access to members only content. Become a member now and you will also be the first to receive in your email when I publish a new article.

### Content below is visible to members only

    
                
## This post is for subscribers only

            Subscribe now
            
Already have an account? Sign in

---

<!-- source: playwright-tips-and-tricks-3.md -->

Playwright

                
# Playwright tips and tricks #3

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Apr 4, 2024
                    
                    9 min
            
        

    

                    
        
            Photo by Alen Jacob / Unsplash
    
            

            
                
It's time to have a look under the hood of Playwright and understand a few details that will enhance our skills to be more creative.

## 1. Get more details about your test during test run

There is a way that you can access some particular values related to your test in real time. Say you have a complex project that has a dynamic configuration based on environments, test-data or whatever other details, and you want to see during test run what particular values are set. You can do that by accessing `testInfo` object within the test. Here is a snippet of code example to see how to access it: 

`import { test } from "@playwright/test";

test.describe('test suite name', () =&gt; {

  test("test name", async ({ page }, testInfo) =&gt; {
    console.log(`test name: ${testInfo.title}`)
    console.log(`parallel index :${testInfo.parallelIndex}`)
    console.log(`shard index: ${JSON.stringify(testInfo.config.shard)}`)
  });
});`Below are some screenshots of other values you can access via `testInfo`

 

My article about parallelization uses the power of this object to showcase particular values.

## 2. How to use Playwright to test multiple browser windows ?

Here is a way you can test multiple windows. Not multiple tabs in the same window. But multiple windows, each with its own storage and cookies. A use case would be if your website has implemented chat functionalities, and you want to see if the messages are delivered correctly. You can have two browsers logged in with two users and have them talk with each other. How would we achieve that in Playwright in one single test?

By using our `browser` and `page` objects. Here is an example of code

`import { test, expect } from "@playwright/test";

test("Two users chat functionalities", async ({ browser }) =&gt; {
    // we open two browsers each with its own storage and cookies
    const user1Context = await browser.newContext()
    const user1Page = await user1Context.newPage()
    const user2Context = await browser.newContext()
    const user2Page = await user2Context.newPage()

    // we open the chat
    await user1Page.goto("https://www.yourweb.com/chat")
    await user2Page.goto("https://www.yourweb.com/chat")
    // other login credentials details would go here

    // we start talking with each other in sequence
    
    await user1Page.getById("input").type("Hello user 2")
    await user1Page.getById("sendMsgBtn").click()
    
    await expect(user2Page.getByText("Hello user 2")).toBeVisible()
    await user2Page.getById("input").type("Oh ! Hello user 1")
    await user2Page.getById("sendMsgBtn").click()

    await expect(user1Page.getByText("Oh ! Hello user 1")).toBeVisible()

  });`The chat is only an example of course, you can do as you wish in any other scenario you can think of. 

## 3. How Playwright handles multiple tabs in the same browser?

For instance where an element would have a property like `target="_blank"` that upon clicking it, will open a new tab, then refer to this at playwright docs. If you find it hard to understand the `const pagePromise = context.waitForEvent('page')` just think of it as an event listener so it will not stop your test, it will just listen. Now right after you perform the click that opens the new tab put `const newPage = await pagePromise` there and from then on, you can use `newPage` the same as `user2Page` from my example above at point 2. Now you can cycle via `newPage` object or initial page object without the need to do any extra actions. Those of you who have done Selenium remember the switch back and forth with commands such as `driver.switchTo().window(actual)`, no need to do that here anymore. Here you have each page with its own object.

Also if you want to fully understand how to use this trick of "listening" for events such as a new page to open, I advise you pay close attention to point 6. It's not an easy one, but I am sure you will master it after you read the explanation.

Remember that: 
browser.newContext() = new window (not yet complete browser, it still needs a tab)context.newPage() = new tab
Here are more examples to understand. Read the comments please.

`import { test } from "@playwright/test";

test("Multiple windows and tabs default way", async ({ page }) =&gt; {
  // Default way of using playwright
  // page comes with values about the browser you have setup in config. 
  // Ready to go, no need for extra actions.
  // this opens a window (context) and a tab (a page)
  await page.goto("https://duckduckgo.com/") 
  });`
```
`import { test } from "@playwright/test";

test("Multiple windows and tabs", async ({ browser }) =&gt; {
  // this creates a new window but you can't perform actions
  // with page2Context since its not yet complete, it still needs a tab 
  const page2Context = await browser.newContext() 
  
  // we have a browser, we have a window we only need a tab. You do this:
  const page2 = page2Context.newPage() 
  });`
```
Try to mix it up: 

`import { test } from "@playwright/test";

test("Multiple windows and tabs mix it", async ({ page, context, browser }) =&gt; {
  // this opens normally a full browser with window and tab (default way)
  await page.goto("https://duckduckgo.com/")
  
  // this creates a new tab from the same window (context) as line above
  const page2 = await context.newPage()
  await page2.goto("https://martioli.com/")

  // this sets up a new browser window with a tab. 
  // Independent from the lines above
  const page3Context = await browser.newContext()
  const page3 = await page3Context.newPage()
  await page3.goto("https://github.com/adrianmaciuc")
  });`
## 4. How to handle multiple types of browsers inside a test ?
What I want to showcase below is not a way to test multiple browsers. There are far more efficient ways to do that. I am not sure if I will write about that because its fairly simple and the internet is full of such tutorials. But for the sake of getting our hands dirty and understanding how our browser instances are created, see below how you can play with various browsers directly inside your test scope. 

`import { test , webkit, firefox, chromium } from "@playwright/test";

test("Multiple browser drivers", async () =&gt; {
    const browser = await webkit.launch()
    const context = await browser.newContext()
    const page = await context.newPage()
    await page.goto("https://martioli.com/")

    const browser2 = await firefox.launch()
    const context2 = await browser2.newContext()
    const page2 = await context2.newPage()
    await page2.goto("https://martioli.com/")
  });`Notice that there is no more { browser, page } . What happened here is we've pulled the webkit and firefox object inside the test scope. Its a bit of a stretch to do this. But for the purpose of us to understand and maybe develop in the future some creative ideas, it's good to know how it works. 

Remember that, in a normal setup where you just use `test` , and then destructure `{ page }` the object will come with the values about the browser, that you have set inside your playwright config file or with the values that can be set dynamically in a terminal command or a pipeline.

To all JAVA lovers, remember these lines below?

`import org.openqa.selenium.chrome.ChromeDriver;

WebDriver driver = new ChromeDriver()
driver.get("https://www.martioli.com")`If you come with background of Java and Selenium then all of my points from above makes sense to you on how to instantiate and handle drivers. And you will also understand that we don't have to write anymore any other lines of code to get our driver object ready to work with. We don't have to, but if we want we still can.

## 5. Can I override Playwright configurations from within my test ?

We all know that our file called playwright.config holds the configuration and helps us set the project up, and all of our tests will run with those configs. But what if I want to override the configurations only for one test or a suite of tests ? 

What if I want a suite of tests to run with a set of configs and another with other set of configs ?

You can do that in two ways:
The simple way. Create a project and have all the use options written for that project
An example for the first option, would be like this

`import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
      name: 'whateverNameForProject',
      use: {
        ...devices['Desktop Chrome'],
        colorScheme: 'dark',
        locale: 'fr-FR',
        httpCredentials: {
          username: 'yourUser',
          password: 'yourPass',
        },
        testIdAttribute: 'data-testid',
      }
    // any other config you would like goes here
  ],
});`Whenever you run your tests it will by default run all tests against all projects. So be sure to specify which project you want to run with `--project=whateverNameForProject` 
The dirty way. Override config values from within your tests
For the second option, imagine that you want a particular test suite or a spec file to have tests with a special setting for geolocation or viewports, maybe a test has some simple login credentials you have to bypass. In other words, whatever you see here, any of these values can be manipulated inside our tests. This is how you do it

`import { test } from "@playwright/test";

test.use({
    geolocation: { longitude: 36.095388, latitude: 28.0855558 },
    userAgent: 'my super secret Agent value'
  })

test("Override config", async ({ page }) =&gt; {
    await page.goto("https://martioli.com/")
})
`This will keep the rest of the settings and just override what you need.
Easter Egg -&gt; let me know the name of the location I used here in the geolocation and you shall win the title as my golden reader
If you want to have in the same spec file, multiple suites that each come with their own extra config you can do like this 

`import { test, expect, webkit, firefox, chromium } from "@playwright/test";

test.describe('Override suite 1', () =&gt; {
  test.use({
    viewport: { width: 400, height: 810 },
    geolocation: { longitude: 36.095388, latitude: 28.0855558 },
    userAgent: 'my super secret Agent value'
  });

  test("Override test 1", async ({ page }) =&gt; {
    await page.goto("https://martioli.com/")
  })

});

test.describe('Override suite 2', () =&gt; {
  test.use({
    viewport: { width: 768, height: 1024 },
    geolocation: { longitude: 36.095388, latitude: 28.0855558 },
    userAgent: 'my second super secret Agent value nr 2'
  });

  test("Override test 2", async ({ page }) =&gt; {
    await page.goto("https://martioli.com/")
  })

});`If you want the same override config at spec file level, just move the test.use() at the top of the page and it will apply to all the suites inside the file.

Configurations can be also done via globalSetup. Its a more elegant and advanced way of doing things. I will probably do a blog post about it in the future.

Not that elegant but still another way to do it, you can pass in configuration override using context.

`import { test, devices } from "@playwright/test";

test("Override test 1", async ({ browser }) =&gt; {
  const context = await browser.newContext({
    ...devices['iPhone 13'],
    isMobile: true
  })
  const page = await context.newPage()
  await page.goto("https://martioli.com/")
})

`
## 6. Promise.all in Playwright
I want to address this because I keep seeing people doing their projects with Playwright and some do not fully understand when to use Promise.all() in Playwright. I am going to copy paste the best explanation below (not my words):

I'll use the `waitForResponse()` method to showcase

Let's say, we have a Search Input field and a button that triggers the search, eventually making a request to an API (https://example.com/api/search, the search term is in the request body)

You would probably write something like this

`await page.locator("button").click() // search button

await page.waitForResponse("https://example.com/api/search")  `With the above code, there's a (high) chance that we already received a response from `https://example.com/api/search` before we reached the `await page.waitForResponse("``https://example.com/api/search``")` line. The `.click()` method doesn't resolve immediately, but performs a range of (time-consuming) steps before resolving the await promise and continuing to the next line.

Await executes code asynchronous in sequence, one after another.

What we want here is for `await page.locator("button").click()` and `await page.waitForResponse("``https://example.com/api/search``")` to be executed at the same time - so that both can do their job properly.
That's where Promise.all() comes into play.

Promise.all() executes promises concurrently, meaning,

`const [response] = await Promise.all([
  page.locator("button").click(),
  page.waitForResponse("https://example.com/api/search")  
]);`Executes both `.click()` and `.waitForResponse()` at the same time. The await Promise.all() as a whole only resolves when all of its argument promises passed. The issue we've noticed here is called race condition.

Many Playwright events `(.waitForRequest(), .waitForResponse(), .waitForEvent(), ...)` must execute concurrently with their triggers using Promise.all.

Here is a link to the explanation in full, thanks to advename

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Feel free to checkout other nice tips:
Playwright tips and tricks #1Common errors, data test id hacks, auto-waits, timeouts hack, asserting an array of strings, absence of an element and many moreMartioliAdrian MaciucPlaywright tips and tricks #2I’ve written a post about tips and tricks and it got a lot of love. So, I’ve decided to do another one. 1. How to handle an element that appears after full page load in playwright One of those rare cases where an element will appear in DOM only afterMartioliAdrian MaciucSubscribe for more...

---

<!-- source: how-to-parameterize-projects-in-playwright-medium.md -->

# How to parameterize projects in Playwright

Today, I encountered an interesting challenge while working on a Playwright project at work, and I'd like to document my findings.

> **_The task was to simultaneously run multiple projects, each with different sets of data._**

Thankfully, Playwright offers a feature to address this requirement — the ability to declare **_options_**.

In this article, I'll walk you through the process of leveraging Playwright's option declaration feature to efficiently manage and execute multiple projects concurrently.

> Explore the demo-project's code snippets on my GitHub repository: https://github.com/nora-weisser/playwright_demo

**Step 1**: Install the project:

```
npm init playwright@latest
```

Playwright will download all needed browsers and create the following structure of the project:

```
playwright.config.ts
package.json
package-lock.json
tests/
  example.spec.ts
test-examples/
  test.examples.ts
```

**Step 2**. Add configuration for Playwright Project.

Navigate to `playwright.config.ts`. This file serves as the central hub for Playwright configurations, allowing you to set up settings such as preferred browsers for your Playwright tests. Within the configuration, locate the 'use' section and set the base URL. In this demonstration, I've chosen 'https://www.saucedemo.com/' as the base URL for demo purposes.

```
use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: 'https://www.saucedemo.com/',
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
 },
```

**Step 3**. Scrolling down within the file, you'll encounter various projects. I've included a definition from the Playwright documentation below for your reference:

> A Project is logical group of tests running with the same configuration. We use projects so we can run tests on different browsers and devices. Projects are configured in the playwright.config.ts file and once configured you can then run your tests on all projects or only on a specific project. You can also use projects to run the same tests in different configurations. For example, you can run the same tests in a logged-in and logged-out state.

**Step 4**. Outline a specific test scenario.

**Test case**: implement a login test case and execute it with various sets of data

The parameterization feature plays a crucial role here, enabling the configuration of each project to run the test case against different username/password pairs.

**Step 5**. Let's take a look at data.

Test Data is listed on login screen for testing purposes.

**Step 6**. Create Test Data

Establish a 'test_data' folder, and within it, create 'login.data.ts.' Begin by declaring an interface that encapsulates the necessary properties (username and password) along with their data types.

```
export interface USER_DATA {
    username: string,
    password: string
}
```

Introduce a variable USERS and list all test users designed for the testing purposes.

```
export const USERS: {[type: string]: USER_DATA} = {
    "standard_user": {
        username: "standard_user",
        password: "secret_sauce"
    },
    "locked_out_user": {
        username: "locked_out_user",
        password: "secret_sauce"
    },
    "problem_user": {
        username: "problem_user",
        password: "secret_sauce"
    },
    "performance_glitch_user": {
        username: "performance_glitch_user",
        password: "secret_sauce"
    },
    "error_user": {
        username: "error_user",
        password: "secret_sauce"
    },
    "visual_user": {
        username: "visual_user",
        password: "secret_sauce"
    }
}
```

**Step 7**. In order to incorporate the 'user' parameter into both the test case and project, it's essential to declare the option 'targetUser' and set its value in the configuration. Create a 'helper' folder and within it, establish 'test-option.ts.'

Extend the existing 'TestOptions' by introducing a new option, 'targetUser,' to facilitate the integration of user parameters."

```
import { test as base } from '@playwright/test'
import { USER_DATA } from '../test_data/login.data'
import { USERS } from '../test_data/login.data'

export interface TestOptions {
    targetUser: USER_DATA
}
export const test = base.extend<TestOptions>({
    targetUser: [USERS['standard_user'], { option: true }],
})
```

**Step 8**. We can use option in the test.

```
import { expect } from '@playwright/test';
import {test} from '../helpers/test-options'

test('login with existing username and valid password', async ({ page, targetUser }) => {
  await page.goto('/');
  await page.locator('[data-test="username"]').click();
  await page.locator('[data-test="username"]').fill(targetUser["username"]);
  await page.locator('[data-test="password"]').click();
  await page.locator('[data-test="password"]').fill(targetUser["password"]);
  await page.locator('[data-test="login-button"]').click();
  const currentURL = page.url();
  expect(currentURL).toBe('https://www.saucedemo.com/inventory.html')
  await expect(page.locator('#header_container')).toContainText('Swag Labs');
});
```

Important Note: Import the 'test' function from the helper we created using the following path '../helpers/test-options'.

**Step 9.** Update Project section

```
import { defineConfig, devices } from '@playwright/test';
import type { TestOptions } from './helpers/test-options';
import { USERS } from './test_data/login.data';

export default defineConfig<TestOptions>({
projects: [
    {
      name: 'standard_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['standard_user'] },
    },
    {
      name: 'locked_out_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['locked_out_user'] },
    },
    {
      name: 'problem_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['problem_user'] },
    },
    {
      name: 'performance_glitch_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['performance_glitch_user'] },
    },
    {
      name: 'error_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['error_user'] },
    },
    {
      name: 'visual_user',
      use: { ...devices['Desktop Chrome'], targetUser: USERS['visual_user'] },
    },
}
```

**Step 10**. Run all projects using the command: npx playwright test

By default, Playwright generates test reports that include information about test execution, success or failure of individual tests, and any errors encountered during the test run.

In this specific instance, the test cases executed are listed along with the information on the users that have been processed. A deliberate inclusion of a failed test case, 'locked-out-user,' is made to showcase a scenario where login into the application fails.

**Step 11**. To run a specific project, utilize the following command: `npx playwright test -project=<project name>`. This command executes the login test case against a predefined dataset for the specified project.

### Conclusion.

In this piece, I shared my findings regarding parameterization in Playwright projects, covering aspects ranging from establishing test data to updating configurations and implementing options. Through the utilization of this feature, there is potential to improve maintainability, broaden test coverage, and improve the efficiency of issues identification.

**Resources**:

1. _Test Parameterize_: https://playwright.dev/docs/test-parameterize
2. _Fixtures_: https://playwright.dev/docs/test-fixtures
3. _Repo with demo-project_: https://github.com/nora-weisser/playwright_demo

---

<!-- source: full-parallelization-in-playwright.md -->

Playwright

                
# Full parallelization in Playwright

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Mar 29, 2024
                    
                    7 min
            
        

    

                    
        
            Photo by Austris Augusts / Unsplash
    
            

            
                
How many workers should we setup in our config ?

How many shards should we use in our CI ?

Do we need fullyParallel true in Playwright config ? What does it even mean?

If you ever asked any of these questions, then you are in luck. Here is a way you can find your answers. Its much easier to understand things when you do them yourself, however if you are here just to quickly see the answers, then skip the setup part and go straight to results

## SETUP:

Install playwright using the following command

`npm init playwright@latest`Now at this point. Lets write some tests with the purpose to discover ourselves how playwright will split our tests in order to achieve full parallelization. Create a file for your test (my example I named it `para_1.spec.ts`. Name it however you want and add this:

`import { test } from "@playwright/test";

test.describe('FIRST SUITE', () =&gt; {

  test("1 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("2 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("3 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("4 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

});

test.describe('SECOND SUITE', () =&gt; {

  test("5 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("6 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("7 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("8 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

});`We have:

- 2 Test Suites (describe block)
- Each test suite has 4 tests. A total of 8 tests in our first spec filePro tip: we can have a look under the hood at our configs live during the test run using this little hack, of putting `testInfo` as a second argument
Our second spec file, I named it `para_2.spec.ts` and it looks like this:

`
import { test } from "@playwright/test";

test.describe('full PARALLEL', () =&gt; {

  test("9 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("10 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("11 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("12 parallel", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

});
`It has:

- Just one test suite
- 4 Tests
Our third file, I named it `serial.spec.ts` . Name it as you like and add a third round of tests, that will run in SERIAL mode.

`import { test } from "@playwright/test";

// this will override fullyParallel to false, only for this file
test.describe.configure({ mode: 'serial' });

test.describe('SERIAL suite', () =&gt; {

  test("1 serial", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("2 serial", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("3 serial", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

  test("4 serial", async ({ page }, testInfo) =&gt; {
    console.log(`file.name: ${testInfo.titlePath[0]} | describe: ${testInfo.titlePath[1]} | test.name: ${testInfo.title} | worker_id :${testInfo.parallelIndex} | shard.index: ${JSON.stringify(testInfo.config.shard)}`)
  });

});`It has:

- Just one test suite
- 4 Tests
The little hack to run only this spec file in serial mode I am going to elaborate on a different post but for now, the comment above it, is enough explanation to understand.

Now lets configure our playwright to use full parallelization

Inside our `playwright.config.ts` file you should have like this

`import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  workers: process.env.CI ? 3 : 1,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: 'html',

  projects: [
    {
      name: 'x',
      use: { ...devices['Desktop Chrome'] },
    }
  ],
});`As you notice this config file has the bare minimum to run, because we are only interested in particular settings. The rest of the config values are up to you how to make them, but for those above let me explain:

`workers: process.env.CI ? 3 : 1` if we are in CI it will setup to run using 3 workers, if not it will run with 1 worker. Config this as per your limitations. Remember that number of workers will increase the load on the CPU of your machine. So you know best what works on your local machine and how powerful your CI runners are.

`fullyParallel: true` means that this will spawn workers and have spec files assigned to them, but also the test suites (describe blocks) are mixed, and... wait for it... the tests inside are also mixed. So you would have for example the same worker running second test from suite 1 of spec file 1 and then after it will finish will do third test from suite 1 of spec file 2. This is what it means full parallelization, work is balanced not just per spec file, but also per describe block and even per tests. 

## RESULTS:

See below an example of a result using parallelization

I have setup in my CI to run using two shards (machines), above you see the results for our first machine. See below the results for our second machine, in other words from shard index two:

Besides full parallelization, when you are running in CI, you can balance the load on multiple machines using sharding.

Here is a visual representation of our first machine 

And here is a visual representation of our second machine:

We have to pay attention here at worker id 1 . Notice how :

- It is mixed between para_2 file and serial file
- The whole suite from serial file and all tests are in the same worker
- Tests run in order 1 2 3 4
Now look at worker id 2, that it has just 1 test from one spec file. If our serial spec file would have been in parallel mode we would have not have such inefficient load balancing. From Playwright point of view, because serial.spec.ts file was in serial mode, it considered all of it as a whole and it did not try to split the tests, only the spec files got balanced.

You can see the explanation here, that even if you don't have `fullyParallel` set to true, it will still run in parallel but it will not be as they call it FULL parallel mode, because they perform load balancing at spec level. If you want more granular than that, you have to enable it.

Just for the fun of it. See below RESULTS for a normal setup without sharding or `fullyParallel` turned on. But still with 3 workers on just one machine:

Here is the visual representation:

It split the spec files only. It ran in parallel with 3 workers indeed. And it ran in order.

Its safe to say that this is not ideal, and most likely will cost you time and we all know that time is money. 

But what about the workers? How would I know how many workers my setup can handle? Well, Butch Mayhew, explains here a way that you can test your environments and find out what are the optimal values to choose.

What about sharding? How many machines should I choose? Well the answer depends on your own setup, not everybody has access to the same resources. Here are a few factors you have to take in consideration when you decide on a number:

- A new machine (use of sharding) means a new instance means more costs
- Sometimes on some projects when you spin up a machine the setup to get everything up and running for the tests to execute takes a long time. Its a trade off you have to consider
- A machine can have a certain capacity on its CPU so overloading one machine with multiple workers may be more heavier than just having more machines with less workers for each. Become a member for free 

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

---

<!-- source: strategic-tagging-optimizing-your-playwright-test-suit.md -->

[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

# Strategic Tagging: Optimizing Your Playwright Test Suit

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:64:64/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---byline--4ab109343fed---------------------------------------)

[Meris Stupar](https://medium.com/@merisstupar11?source=post_page---byline--4ab109343fed---------------------------------------)

5 min read

·

Mar 18, 2024

--

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D4ab109343fed&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=---header_actions--4ab109343fed---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

Do you find yourself running your complete automated test suite on every occasion? Utilizing tags can accelerate the process by selectively choosing which tests to execute precisely when you require them.

As software projects evolve and automation project expands, it's common for the number of tests to increase alongside the introduction of new features. It becomes very useful to run only a subset of certain tests. Although Playwright allows tests to run in parallel, there comes a point where splitting tests into smaller groups proves useful. While maintaining a robust automated test suite is essential for product quality, careless test automation can decrease your team's progress considerably. One effective strategy is to segment your automated tests by tagging them and executing only a subset of the complete test suite at various stages of the software development process.

Effectively organizing tests with tags offers a significant advantage of precisely targeting the required test cases.

**Consider the following examples:**

◾ Run the entire test suite outside of business hours without disrupting the team and selectively run a subset of tests on a pull request to maintain the speed and efficiency of your **CI pipelines**.

◾ Allow specific teams (eg QA or the features team) to run only the tests they are responsible for.

◾ Run smoke tests during a production release that only involve read operations.

Tags are used to filter tests in the HTML Report, UI Mode or VSCode extension.

Using a tag system allows you to categorize tests into logical sets. Tags are defined using the **@tag** syntax within the test description. Although any string can technically serve as a tag, the documentation prefers the **@tag** syntax, so it is recommended to follow that rule.

**How to Install Playwright?**

```
npm init playwright@latest
```

Please visit official [Installation \| Playwright](https://playwright.dev/docs/intro) documentation for more details.

**How to run Playwright test?**

```
npx playwright test
```

**Old Playwright Syntax:**

In the past, tags were incorporated into the test title, which remains a supported method. However, this approach leads to duplication in the HTML report. Playwright automatically extracts tags from the title and displays them as labels for improved visibility, eliminating the need for redundant tagging within the title.

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

Press enter or click to view image in full size

Example of Old Playwright Tagging Syntax

**New Playwright Syntax:**

The reason for introducing the new syntax for placing tags, as stated in the official documentation, stems from the visibility of the previous syntax in the HTML report, where tags were displayed inside the test title as tags. As we have shown in the previous part of Old Playwright Syntax. This way of tagging could lead to confusion and significant duplication, especially when dealing with numerous tags.

To adopt the new syntax, simply generate a tag object containing either an array of tags or a single tag:

```
test('Playwright Landing page - Has title', { tag: ['@Smoke', '@UI' ] } ,async ({ page }) => {

  await page.goto('https://playwright.dev/');

  await expect(page).toHaveTitle(/Playwright/);

});
```

Press enter or click to view image in full size

Example of New Playwright Tagging Syntax

As evident from the new syntax, tags are no longer displayed within the test name itself, resulting in significantly improved readability.

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

Press enter or click to view image in full size

Example of Describe Block Tagging Syntax

Update Playwright to latest version with following command:

```
npm install -D @playwright/test@latest
# Also download new browser binaries and their dependencies:
npx playwright install --with-deps
```

To verify what version you have installed on your machine use this:

```
npx playwright --version
```

**Advantages of using @tag in test management:**

**Simplified test management:** **@tag** simplify test management by categorizing test cases. This categorization allows for quick filtering and identification of relevant test cases based on tags. You can easily select test cases to execute according to specific tags.

**Tag Statistics Heat map:** The Tag Statistics Heat map, available on the analytics dashboard, provides valuable insights. It allows you to track metrics related to tags, such as the total number of tags and the amount of test cases tagged with a particular tag. However, it is essential to track the progress of test automation coverage by tags.

**Custom Test Scenarios:** You have the flexibility to define useful tags for any custom test scenario. This allows scenarios from different features, test suites or feature files (BDD) to be executed together. For example, you can execute all tests marked as **@Smoke** excluding those marked as **@Regression**

**Examples you can use to classify your tests:**

S **moke testing** is a software testing technique that is performed after the software is built to verify that the critical functions of the software are working well. It is performed before any detailed functional or regression tests are performed. The main purpose of smoke testing is to reject a software application with bugs so that the QA engineering team does not waste time testing a broken software application.

S **anity testing** is a type of software testing that is performed after receiving an intermediate version of software, usually with minor changes to code or functionality. Its purpose is to ensure that bugs have been fixed and that no new problems have arisen as a result of these changes. The goal is to verify that the intended functionality works roughly as intended. If the correctness test gives wrong results, the build is rejected to avoid spending time and resources on more extensive testing.

R **egression testing** is a type of software testing conducted after a code update to ensure that the update introduced no new bugs. This is because new code may bring in new logic that conflicts with the existing code, leading to defects. Usually, QA teams have a series of regression test cases for important features that they will re-execute each time these code changes occur to save time and maximize test efficiency.

Therefore, it is very important that you organize your test tagging strategy well. You are in a much better position when you want to run only a specific set of tests and not the entire suite. Under the name of the Smoke tag, you can run only Smoke tests or, otherwise, Regression tests individually.

## Until Next Time: ✌️💻

[Playwrights](https://medium.com/tag/playwrights?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Testing](https://medium.com/tag/testing?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Automation](https://medium.com/tag/automation?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Programming](https://medium.com/tag/programming?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[QA](https://medium.com/tag/qa?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:96:96/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:128:128/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[**Written by Meris Stupar**](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[76 followers](https://medium.com/@merisstupar11/followers?source=post_page---post_author_info--4ab109343fed---------------------------------------)

· [2 following](https://medium.com/@merisstupar11/following?source=post_page---post_author_info--4ab109343fed---------------------------------------)

Software Engineer - Automation Quality Assurance Engineer

[Help](https://help.medium.com/hc/en-us?source=post_page-----4ab109343fed---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----4ab109343fed---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----4ab109343fed---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----4ab109343fed---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----4ab109343fed---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----4ab109343fed---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----4ab109343fed---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----4ab109343fed---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----4ab109343fed---------------------------------------)

---

<!-- source: advanced-snapshot-testing-in-playwright.md -->

Playwright's snapshot assertions are an incredibly powerful tool for ensuring your app's UI remains consistent across code changes, browsers, and devices. But they're not always easy to use.

This article dives deep on snapshot testing in Playwright, covering a wide range of features and techniques. By the end, you'll be a snapshot testing master, ensuring your app's flawless visual consistency across browsers and devices.

## Page vs. Element Snapshots

Playwright's visual testing API allows you to take snapshots of the entire page or just a specific element.

### When should I use page snapshots?

Page snapshots are excellent for verifying the entire page works as expected. Use page snapshots to test layout, responsiveness, and accessibility.

But be warned: Page snapshots can be flaky. After all, if anything within the viewport changes, the entire snapshot will fail.

```typescript
test('page snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot();
});
```

### When should I use element snapshots?

Element snapshots focus exclusively on a single page element. This makes them an excellent choice for testing components in isolation.

```typescript
test('element snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $button = page.locator('button').first();
  await expect($button).toHaveScreenshot();
});
```

## Working with Page Snapshots

### Cropping Page Snapshots

Sometimes the entire viewport isn't necessary to prove your test passes.

```typescript
test('cropped snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const {width, height} = page.viewportSize();
  await expect(page).toHaveScreenshot({
    clip: {
      x: (width - 400) / 2,
      y: (height - 400) / 2,
      width: 400,
      height: 400,
    },
  });
});
```

### Snapshot the Entire Page

```typescript
test('full page snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot({
    fullPage: true,
  });
});
```

### Scroll Before Taking a Page Snapshot

```typescript
test('scroll before snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await page.evaluate(() => {
    document.querySelector('#your-element')?.scrollIntoView({behavior: 'instant'});
  });
  await expect(page).toHaveScreenshot();
});
```

## Working with Element Snapshots

### Test Element Interactivity

```typescript
test('element states', async ({page}) => {
  await page.goto('https://www.browsercat.com/contact');
  const $textarea = page.locator('textarea').first();
  await expect($textarea).toHaveScreenshot();
  await $textarea.hover();
  await expect($textarea).toHaveScreenshot();
  await $textarea.focus();
  await expect($textarea).toHaveScreenshot();
  await $textarea.fill('Hey, cool cat!');
  await expect($textarea).toHaveScreenshot();
});
```

### Test Element Responsiveness

```typescript
test('element responsiveness', async ({page}) => {
  const viewportWidths = [960, 760, 480];
  await page.goto('https://www.browsercat.com/blog');
  const $post = page.locator('main article').first();
  for (const width of viewportWidths) {
    await page.setViewportSize({width, height: 800});
    await expect($post).toHaveScreenshot(`post-${width}.png`);
  }
});
```

## Advanced Snapshot Techniques

### Masking Portions of a Snapshot

```typescript
test('masked snapshots', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $hero = page.locator('main > header');
  const $footer = page.locator('body > footer');
  await expect(page).toHaveScreenshot({
    mask: [
      $hero.locator('img[src$=".svg"]'),
      $hero.locator('a[target="_blank"]'),
    ],
  });
});
```

### Keeping Styles Constant During Snapshots

```typescript
test('consistent styles', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $hero = page.locator('main > header');
  await expect(page).toHaveScreenshot({
    stylePath: [
      './hide-dynamic-elements.css',
      './disable-scroll-animations.css',
    ],
  });
});
```

### Auto-Retry Flaky Snapshots

```typescript
test('retry snapshots', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot({
    timeout: 1000 * 60,
  });
});
```

### Visual Tests for Generated Images

```typescript
import {test, expect} from '@playwright/test';
import {buffer} from 'stream/consumers';

test('arbitrary snapshot', async ({page}) => {
  await page.goto('https://getavataaars.com');
  await page.locator('main form button').first().click();
  const avatar = await page.waitForEvent('download')
    .then((dl) => dl.createReadStream())
    .then((stream) => buffer(stream));
  expect(avatar).toMatchSnapshot('avatar.png');
});
```

### Compare Snapshots Across Browsers

Use Playwright projects with shared snapshotPathTemplate to compare rendering across Chromium, Firefox, and Safari.

```typescript
const crossBrowserConfig = {
  testDir: './tests/cross-browser',
  snapshotPathTemplate: '.test/cross/{testFilePath}/{arg}{ext}',
  expect: {
    toHaveScreenshot: {maxDiffPixelRatio: 0.1},
  },
};
```

Initialize snapshots with `npx playwright test --project cross-browser -u` then run tests.

Different browsers render fonts, colors, and images differently. Tune `maxDiffPixelRatio` and `threshold` as needed.

## Next Steps

For advice on fine-tuning snapshot tests and running visual tests in CI/CD, check out the Ultimate Guide to Visual Testing with Playwright.

Happy testing!

Cross-browser project configuration example with dependencies chain cross-chromium → cross-firefox → cross-browser ensures Safari compares against same snapshot directory. Remove dynamic widgets before full-page screenshot: `await page.locator(':has(> a figure)').evaluate(($el) => $el.remove())`. Tune forgiving thresholds via maxDiffPixelRatio 0.1 when font rendering differs across engines. Series links: configuring snapshot tests, running snapshot tests in CI/CD, getting started with snapshot tests. BrowserCat ultimate guide covers making visual tests more forgiving when cross-browser pixel diffs are expected rather than regressions.

---

<!-- source: debugging-with-playwright.md -->

Playwright

                
# Debugging with Playwright

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Feb 25, 2024
                    
                    5 min
            
        

    

                    
        
            Photo by Riku Lu / Unsplash
    
            

            
                
Of course you know how to do debug your tests. Everybody knows how to do it. But not everybody does it in the same way. Here are a few ways to debug your tests using playwright

## Debug using Playwright trace viewer 

One of the most commonly used method is to open the trace file using the playwrights trace viewer. Take the `trace.zip` file from your reports put it inside playwright folder or wherever you want, as long as you know the path to it and just type `npx playwright show-trace path/to/trace.zip` 

But did you know that you can also: 

- Read the trace file directly on https://trace.playwright.dev/ Just drag and drop the file there and you can view it in an instant, and don't worry, playwright will not store your trace files.
- You can open remote traces using it's URL. For example you run a CI, it publishes your reports at an url, and you just want to view the trace file without downloading it. Here is an example you can try `npx playwright show-trace ``https://adrianmaciuc.github.io/playwright-example-with-typescript/142/data/attachments/28c2d416a9f33195.zip` . This is the failure if you want to see it published. By the way, I've wrote an article on how to publish your playwright reports on github pages for free.
- You can also pass the URL of your uploaded trace file from some accessible storage as a parameter. CORS (Cross-Origin Resource Sharing) rules might apply. An example of such use case https://trace.playwright.dev/?trace=https://adrianmaciuc.github.io/playwright-example-with-typescript/142/data/attachments/28c2d416a9f33195.zip . Just replace whatever is written after https://trace.playwright.dev/?trace= with the direct link of your trace file.
## Debug using Playwright VS Code extension

Playwright has one of the best integrations with VS Code. You can view your tests and run them from the Test Explorer section (the lab glass icon on the left) . 

Put some breakpoints into the code you want the debugger to stop. Go to Test Explorer and click on the play button with the tiny bug on it (debug test)

But did you also know that:

While you are in debug mode you can view in real time all the steps your code will perform related to elements. You just click at the line of code you want to see and the browser will highlight the element it will attempt to perform action on. (this feature is not limited to debug mode only, see more details later in this article)

You can use watches during debug mode with the VS Code and check values in real time that come as response from your servers. You can even perform different operations or call methods and they will work. Just keep in mind that you can't do asynchronous stuff here. Only regular simple sync, like .toString() or any other.

## Debug using Playwright inspector tool 

That's right, Playwright even has its own debugging tool, called Playwright Inspector, if you want to give it a try. To use it you must give it the name of the file you want to debug by doing something like this `npx playwright test path/to/testfile.spec.ts --debug ` . If for example you have multiple tests in the same file and you want just one single test, then give it also the line of the test like so `npx playwright test path/to/testfile.spec.ts:27 --debug`

What I like the most about this inspector is the fact that it can show you what Playwright does under the hood. This can help you understand lots of things. As seen in the screenshot above, just for a simple click, Playwright does a lot of actions.

## Leverage the features of Playwright debug mode to write tests really fast

First make sure that you have checked the `show browser` option in Test Explorer.

Now go to your tests, write the minimum code to open the web page you are about to write tests for and hit the green play button next to your test()

This will open the browser for you. Have it on one side (ideally a second monitor) and use it to write tests

## Common errors you encounter with debug mode

playwright no tests have been found in this workspace yetplaywright vs code no tests found messageplaywright framework issue no tests found
If you go to your Test Explorer and you see something like this

You can try any of these answers from here, but most of the solutions provided are dependencies related, either you need your latest version of VS Code or Playwright or just need to do a restart of VS Code. However I have not seen anywhere written that sometimes the tests are not there because, you the user, have misconfigured the framework. This tiny detail is almost always overlooked and can be a pain when you have a big project and at one moment someone pushed a commit that just made the tests disappear from the Test Explorer. So have a look at the `playwright.config.js` file and see if everything is in place, check each configuration value set, because if a value is wrong, your linter will not find it, and your tests disappear from test explorer. 

From my experience here are the most common mistakes done in playwright config file that will have your tests disappear from Test Explorer:

- `testDir: "./path/to/tests"` - does not point correctly to where the tests are
- `reporter: "html23typo"` - you played around with the reporter or any other property and you have set the value just wrong and do not match with any of the options accepted by the framework 
- you have a syntax error or a bad import into your files
As a side note, in this particular scenario, if you would have had a syntax error, or a misconfiguration, Cypress, will at least throw an error when you open its runner. I would love to see something like this in Playwright.

I was thinking to do an article about debugging in CI, but I am not sure if anyone would be interested. Please let me know in the comments below or on linkedin, if you would like to see how to debug like a pro in CI.
Become a member for free 

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

---

<!-- source: sdet-quick-introduction-to-allure-reports-with-playwright.md -->

Hello dear quality lovers,

It's no secret that Allure stands out as one of the most popular frameworks for test reporting. Today we integrate Allure Reports with Playwright.

## How to install

### Playwright quick installation

```
npm init playwright@latest
```

### Allure-playwright library installation

```
npm i -D allure-playwright
```

### Allure command line installation

```
npm i -D allure-commandline
```

Or globally: `npm i -g allure-commandline`

### Java requirement

Running Allure reports requires Java 8+. Install Java, add to PATH, verify with `java -version`.

## Extend Playwright configuration

Add `allure-playwright` to the reporter array in `playwright.config.ts`.

## Run tests and reports

```
npx playwright test
```

After tests complete, find `allure-results` folder.

Generate report:

```
allure generate --clean
```

Run server:

```
allure serve
```

## Additional configuration and attributes

Report options include `outputFolder`, `detail`, `suiteTitle`.

### Allure suite

Add `allure.suite` attribute or use `test.describe` names for suite grouping.

Other attributes: `allure.step`, `allure.label`, `allure.story`, `allure.link`, `allure.issue`, `allure.attachment`, `allure.parameters`.

### Allure issue

```javascript
await allure.issue('Bug description', 'https://github.com/org/repo/issues/29');
```

### Allure attachment

Screenshots/videos attach by default when configured in playwright.config.

Custom JSON attachment:

```javascript
await allure.attachment("ATTACH_ACTUAL_PAGES", JSON.stringify(actualPagesData), {
    contentType: "application/json",
});
```

### Allure parameters

```javascript
await allure.parameter("KEY", key.toString());
```

Parameters support `mode: "hidden" | "masked"` and `excluded: true`.

## Bonus: Run tests and reports in docker container

Create Dockerfile with Node, Playwright deps, Allure CLI, http-server. Run tests, generate reports, serve on port 8080.

Use docker-compose to map host port 9000 to container 8080.

## Ending

This was a high-level overview. Read the allure-playwright npm documentation for more options.

Dockerfile sample: FROM node:latest WORKDIR /usr/src/app COPY package*.json RUN npm install RUN npx playwright install --with-deps RUN npm install -g allure-commandline http-server COPY . . RUN npm run test || true RUN allure generate ./allure-results --clean -o ./allure-report EXPOSE 8080 CMD http-server allure-report -p 8080.

docker-compose maps host 9000 to container 8080 when local 8080 busy. build --no-cache then up navigates localhost:9000 for Allure server inside container.

allure.issue links failed tests to known GitHub issues visible in report attachments. allure.attachment JSON.stringify page data with application/json content type. allure.parameter KEY with mode hidden masked excluded options for sensitive keyboard test data.

Report config outputFolder detail suiteTitle control hook visibility and suite naming. Disabling suiteTitle uses test.describe names as default suite labels.

Screenshots on failure via playwright.config screenshot only-on. Java 8+ required for allure serve locally java -version verification step.

Kostiantyn Teltov Ukraine QA Tech Lead SDET QA Architect C# JS TS Python Java blogger speaker February 2024 Medium 10 min read.

Playwright multiple reporters array can include html list and allure-playwright simultaneously. allure-results raw folder allure-report generated folder allure generate --clean wipes stale history.

Global vs local allure-commandline: local project version takes precedence when both installed.

Beta allure-playwright note: author tested beta for bug fix verification prefer stable npm i -D allure-playwright for production pipelines.

---

<!-- source: playwright-stories-navigating-tricky-ui-automation-scenarios-for-beginners.md -->

Hello everyone,

Playwright is one of the most user-friendly test automation tools for beginners. Today we cover tricky UI scenarios:

- JavaScript Alerts
- IFrames
- Shadow DOM (open)
- File Download
- File Upload
- Tables

## Alerts

### Alert Box

Subscribe to the `dialog` event and call `dismiss`:

```javascript
page.on('dialog', async dialog => await dialog.dismiss());
```

### Confirm Box

Use `dialog.accept()` or `dialog.dismiss()`.

### Prompt Box

```javascript
page.on('dialog', async dialog => await dialog.accept('Test'));
```

## IFrames

Use `page.frameLocator()` to chain into nested frames:

```typescript
this.frameTop = page.frameLocator('frame[name="frame-top"]');
this.innerTopLeftFrame = this.frameTop.frameLocator('frame[name="frame-left"]');
return await this.innerTopLeftFrame.locator('body').innerText();
```

Unlike WebDriver, you don't switch in and out of frames.

## Shadow DOM

Shadow DOM encapsulates component markup. Access with component name in selector:

```typescript
return await this.page.locator('my-paragraph span').textContent();
return await this.page.locator('my-paragraph ul li').allInnerTexts();
```

## File Download

```typescript
const [download] = await Promise.all([
  this.page.waitForEvent('download'),
  this.page.click(`a[href="download/${expectedFileName}"]`)
]);
await download.saveAs(savePath);
```

In tests: resolve save path, call download method, assert file exists, clean up with `fs.unlinkSync`.

## File Upload

```typescript
await this.page.setInputFiles('#file-upload', filePath);
await this.page.getByRole('button', { name: 'Upload' }).click();
```

Verify with `getUploadedFileName()` reading `#uploaded-files`.

## Tables

Define a row model interface, locate rows, iterate cells by index:

```typescript
const tableRows = this.page.locator(this.tableRowsSelector);
const rowCount = await tableRows.count();
// for each row, read td cells by index into model
```

Sort by field if needed for deterministic assertions.

## Epilog

You don't need much code to solve UI challenges with Playwright. Even tricky cases are easier than they look. Repository with examples linked from the original article.

Alerts: page.on dialog dismiss accept accept with text Test for prompt box. IFrames frameLocator chain frame-top frame-left frame-middle frame-right frame-bottom get body innerText without WebDriver switchTo.

Shadow DOM my-paragraph span textContent and ul li allInnerTexts open shadow root selectors pierce encapsulation.

File download Promise.all waitForEvent download click href saveAs fs.existsSync assert unlinkSync cleanup.

File upload setInputFiles path getByRole Upload click getUploadedFileName textContent uploaded-files trim assert filename match.

Tables ExampleOneTableModel interface lastName firstName email due webSite row locator td index loop sortTableByField TableHeaderNames.Email optional sort for random table data.

Playwright Discord https://discord.com/servers/playwright-807756831384403968 community support for beginners.

Topics covered JavaScript Alerts IFrames Shadow Dom open File Download File Upload Tables movie epilogue pop culture references.

Kostiantyn Teltov January 2024 9 min read Medium Playwright stories beginners UI automation tricky scenarios Ukraine QA architect speaker.

May the force be with you epilog closing — easier than you think try Playwright for alerts frames shadow roots downloads uploads table parsing.

Demo repo linked at article end for hands-on practice with training site implementations each section references concrete page object methods async getFrameBottomText getInnerTopLeftFrameText patterns.

---

<!-- source: understanding-playwright-testslow-and-slowmo-option.md -->

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

---

<!-- source: how-to-parameterize-projects-in-playwright-hashnode.md -->

## Introduction

Today, I encountered an interesting challenge while working on a Playwright project at work, and I'd like to document my findings.

**The task was to simultaneously run multiple projects, each with different sets of data.**

Playwright offers the ability to declare **_options_** to address this requirement.

Explore the demo-project on GitHub: https://github.com/nora-weisser/playwright_demo

## Step 1: Install the project

```
npm init playwright@latest
```

## Step 2: Configure Playwright Project

In `playwright.config.ts`, set baseURL in the `use` section:

```
use: {
    baseURL: 'https://www.saucedemo.com/',
    trace: 'on-first-retry',
 },
```

## Step 3: Playwright Projects

A Project is a logical group of tests running with the same configuration — different browsers, devices, or data sets.

## Step 4: Test scenario

Implement a login test executed with various username/password pairs via project parameterization.

## Step 5-6: Test Data

Create `test_data/login.data.ts` with `USER_DATA` interface and `USERS` map for standard_user, locked_out_user, problem_user, etc.

## Step 7: Extend TestOptions

Create `helpers/test-options.ts`:

```
import { test as base } from '@playwright/test'
import { USER_DATA, USERS } from '../test_data/login.data'

export interface TestOptions {
    targetUser: USER_DATA
}
export const test = base.extend<TestOptions>({
    targetUser: [USERS['standard_user'], { option: true }],
})
```

## Step 8: Use in test

Import `test` from `../helpers/test-options` and use `targetUser` fixture in login test against saucedemo.com.

## Step 9: Update projects section

Define one project per user with `targetUser: USERS['standard_user']` etc. in each project's `use` block.

## Step 10: Run all projects

```
npx playwright test
```

Report lists each project/user combination. locked_out_user demonstrates expected failure.

## Step 11: Run specific project

```
npx playwright test --project=standard_user
```

## Conclusion

Parameterization improves maintainability, coverage, and failure diagnosis. Alternative: fixtures with one project keep config simpler; projects help when different users run different test subsets or when reports should group by user/project.

Resources:
- https://playwright.dev/docs/test-parameterize
- https://playwright.dev/docs/test-fixtures
- https://github.com/nora-weisser/playwright_demo

Comments from readers note fixtures as an alternative for same test across users, while project-level parameterization helps when each user runs a different test subset or when Allure/built-in reports should segment by target user.

Hashnode republication by Eleonora Belova qualitymatters blog includes Step 1 npm init playwright@latest project structure playwright.config.ts package.json tests/ example.spec.ts.

Step 2 baseURL https://www.saucedemo.com/ with trace on-first-retry in use block of playwright.config.ts.

Step 3 Playwright Projects definition: logical group running same configuration across browsers devices or logged-in vs logged-out states.

Step 4 login test case parameterized across username password pairs via targetUser option per project.

Step 5-6 test_data/login.data.ts USER_DATA interface and USERS map with standard_user locked_out_user problem_user performance_glitch_user error_user visual_user all using secret_sauce password.

Step 7 helpers/test-options.ts extends TestOptions with targetUser fixture defaulting to standard_user with option true flag.

Step 8 test imports custom test from helpers uses targetUser in fill locators data-test username password login-button assert inventory.html Swag Labs header.

Step 9 projects array maps each user to Desktop Chrome device plus targetUser from USERS map.

Step 10 npx playwright test runs all six projects shows locked_out_user intentional failure in HTML report with user column per project.

Step 11 npx playwright test --project=standard_user runs single dataset. Resources: playwright.dev test-parameterize test-fixtures github nora-weisser playwright_demo.

Rosemarie Vickers Good post comment. Pritesh Usadadiya curated Software Testing Notes newsletter issue 121. Eugene Gronski prefers fixtures for different users keeping one project; author replies project scope useful when different test subsets per user or report grouping by project name matters.

---

<!-- source: playwright-tips-and-tricks-2.md -->

Playwright

                
# Playwright tips and tricks #2

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Jan 8, 2024
                    
                    6 min
            
        

    

                    
        
            Photo by Vaibhav Bharadwaj / Unsplash
    
            

            
                
I've written a post about tips and tricks and it got a lot of love. So, I've decided to do another one.

### 1. How to handle an element that appears after full page load in playwright

One of those rare cases where an element will appear in DOM only after some conditions or scripts are executed. Even if Playwright will wait for the page to fully load, you may encounter situations where your element will appear later than that. In this situation here are a few tricks I recommend:

- First option, wait for network to be idle:  `page.goto('https://playwright.dev/', {waitUntil: 'networkidle'})`
- Second option is to wait for the elements particular state. Use element.waitFor(state). Default value is "visible" but you can change to, for example, be present in the DOM, by doing first `const element = page.locator('#locator')` and then `await element.waitFor("attached")`
### 2. Custom expect message 

Did you know that you can have your own custom expect message when an assertion fails?  `await expect(locatorOrValue, 'Failed to perform something').toBe()`. This can be very useful when you use them in custom built methods or page methods and you want to have a more detailed message of what failed.

### 3. How can I assert receiving of an email with playwright ?

Imagine that you have a registration form, and upon submitting that form an email is being sent with the confirmation number/link. Since the email does not always appear instant in your inbox, you can perform a polling technique , using expect.poll from playwright. Read the comments below as I try to explain how it works.

`await expect.poll(async () =&gt; {
  const allEmails = await page.request.get('https://api.email.com/allEmails')
  // do some logic here, for example search the emails for the one that includes the registration form data of username/email
  for (email of allEmails){
      if (email.to.includes("[email&#160;protected]"))
          // once email found write the logic to extract link/code from email contents
          return emailCode
  }
  return false;
}, {
  // this part is the configuration of your polling and they are all optional
  // Custom error message, triggered after timeout runs out.
  message: 'Failed to find confirmation link in email',
  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe
  // ... Defaults to [100, 250, 500, 1000].
  intervals: [1_000, 2_000, 10_000],
  // last value in interval is repeated until timeout. And it will throw an error with the custom message
  timeout: 60_000
}).toBeTruthy();`Remember that every time it fails to find the email the expect still runs like a loop. The only time it fails, is if it reaches 60 seconds and it didn't return a truthy value that was expecting at the last line of code. You can easily change `toBeTruthy()` to whatever jest expect like method you want, to be particular to your needs and what you return in your logic. 

### 4. Is there a way to fail an assertion but not fail the test? 

Have a look at soft assertions first, maybe that's what you want.

Or maybe you are expecting something that appears at random intervals of time. Playwright has an expect method that can repeat expect assertions (multiple yes) until they ALL pass, and its called expect.toPass(). Just group all assertions inside an expect.toPass(). How this method works is very similar to what I've explained above at expect.poll() but this time inside our expect method we can perform one or multiple expect() that may fail a couple of times before they succeed. Condition is they ALL have to pass. So the block of code repeats until the condition is met (or it timesout). Here is an example :

`test("Element appears after 5 seconds", async ({ page }) =&gt; {
    await page.goto("https://webdriveruniversity.com/Accordion/index.html");
    await expect(async () =&gt; {
        await expect(page.getByText("LOADING")).toBeVisible()
        await expect(page.getByText("COMPLETE.")).toBeVisible()
    }).toPass({
        intervals: [1_000, 5_000, 10_000],
        timeout: 60_000
    });
});`Notice configuration now goes inside the toPass({}) 

### 5. How can I intercept a network call in playwright ?

We are not talking here about page.route()

There are two types of end to end testing, horizontal and vertical. Most common one is horizontal where you test for example purchasing a product. This is an user flow. And the less known one is vertical, where you test `user action -&gt; Front End (UI) -&gt; Back End -&gt; database` or `database -&gt; Back End -&gt; Front End -&gt; what user sees`. 

Take for example an invoice that is generated if you click a button and the values are displayed at UI level in your account. You may have the front end perform a computation of data received from the backend to display the values. What if values are displayed incorrectly, who is to blame? front end devs will say its a backend problem and backend devs will say its a frontend problem. Sounds familiar? In order to catch this kind bug you do it like this:

- for the front end you put an assertion of value that will be displayed in the UI
- for the backend you listen to network calls and assert what the backend sends via the API before it reaches frontend 
This will give you validation at two levels/layers . So, how do we intercept a call in playwright? With the use of waitForResponse . Let me give you a snippet of code for further clarification:

`// the preceding double asterix is used to have dinamic wait on multiple environments. 
// Notice waitForResponse has no await. It just marks SINCE WHEN you want to listen for that call
const invoiceCall = page.waitForResponse("**/invoices/*");
await page.getByText("Generate Invoice").click();

// It will default wait for 30 seconds for the call
const response = await invoiceCall

// now make that response as json to be easily read
const responseAsJson = await response.json();
await expect(responseAsJson.invoice.value).toBe("355");`Why should I care about the SINCE WHEN part? This comes in handy when you want to intercept multiple calls from same endpoint. More on that soon.
### 6. How to debug like a pro in playwright ?
This topic actually deserves its own blog post, and I've written it here. But just wanted to point out one thing that probably almost everyone missed out. Playwrights own debugger called the inspector can give you a LOT more than you think. Let me show you what I mean. Lets say you have one line of code that you just can't figure it out why you can't achieve what you want. Here's what you can do, start your test with `npx playwright test path/to/yourTest.spec.js --debug`, then go to the inspector and open the action you want to find out more about, and behold all of the actions Playwright is performing under the hood:
Small notice: I am not a big fan of this inspector as at the moment v1.40 , its a bit buggy. But it can be useful sometimes.
### 7. How to get text and store in a variable in playwright for later reuse ?

Playwright will recommend to give an element to expect() and use its toHaveText() method to assert if an element has a certain text, but sometimes you just need to fetch that text from an element and later reuse it for other actions. This happens when values are not set and come dinamically. You can achieve this with `const elementText = await page.locator(locator).innerText()` . This will give you a value as a string inside elementText to later use. 
Note that innerText() will give you only visible text. So if text is inside the DOM but is not visible then it will not return that value. To include even hidden text use textContent()

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Feel free to checkout other nice tips:
Playwright tips and tricks #1Common errors, data test id hacks, auto-waits, timeouts hack, asserting an array of strings, absence of an element and many moreMartioliAdrian MaciucPlaywright tips and tricks #3Get more details about your test during test run. How to use Playwright to test multiple browser windows. Promise.all in PlaywrightMartioliAdrian Maciuc
External recommendations :
Tips for Writing Efficient Playwright Test ScriptsMaximize the efficiency of your Playwright test scripts with these practical tips and best practices. This comprehensive guide will help you navigate through the complexities of writing test scripts, making your tests more maintainable, reliable, and efficient. Let’s dive into these valuable insight…RayrunLuc GaganSubscribe for more...

---

<!-- source: playwright-tips-and-tricks-1.md -->

Playwright

                
# Playwright tips and tricks #1

                    

        
                
                        
                
        

        
                
#### 
                    Adrian Maciuc
                

                
        

        
            
                Dec 8, 2023
                    
                    8 min
            
        

    

                    
        
            Photo by Dan Cristian Pădureț / Unsplash
    
            

            
                
As you work with a framework you start to encounter various situations from which you can learn. Things that you find out are really important and are not that easy to discover just by reading the docs or following tutorials. Things that come with experience. I would like to share some of the things that I have recently learned about Playwright.

I will be referring in the following statements when talking about locator methods in Playwright with `page.locator()` but this does not mean that I suggest to use only `locator()` . Its just a placeholder. On this topic I actually recommend to use the built-in locator methods created by Playwright, mostly `getByTestId()` and if you tried all and it just doesn't work for you, then use `.locator()`

### 1. How to find a child element if only its parents have unique ids

For example if you have `uniqueIDParent &gt; div1 &gt; div2 &gt; span` (where span has the "text you want") , but there are multiple children (spans) with different text, if you give to playwright its parent or grandparent or greatgreat, it will traverse all of its children and extract all text. `expect(uniqueID).toHaveText("text you want")` will work. In the same time `page.getByTestId(uniqueIDParent).filter({ hasText: "text you want" })` will work just fine if you target only the child that has the text  you want. Filtering works miracles.

### 2. Why do I get sometimes in Playwright the error browser has been closed

One reason may be, because you probably forgot an `await` somewhere. Playwright works in asynchronous way, meaning that its all promises, but you need your test steps to run sequentially, top to bottom and to perform the actions on that exact order. This is achieved by using the `await` key that will resolve the promise and this is how Playwright will keep your steps in order, to avoid issues about race conditions. Sometimes VS Code will suggest to you that you don't need some awaits, disregard that, you do need them.

### 3. Auto-waits

Here are some typical examples of assertions from Selenium that you do not need to perform anymore:

-  You don't have to assert element is visible before interacting with it. Playwright will do the following before an interaction: 		- it will check if the element is visible		- if its attached to DOM		- and if its stable, animation is completed
- When you open a page or when you click a link to redirect to a page, you don't have to assert the page is loaded before interacting with elements, playwright will wait for the page to fully load first
- You don't have to write explicit waits for an element when you are waiting for it to appear or waiting for it to disappear. Playwright has built-in timeouts (these explicit waits you know from Selenium) that will wait for an element and try to find it for a set interval of time (eg: by default is 5 seconds for an `expect(locator).toBeVisible()`)
### 4. There are very particular situations sometimes where the timeouts are not working for you

Or you just can't figure it out what to do about it, you still have a last option to simply wait a set number of seconds using the waitForTimeout() . This is discouraged in general, but sometimes you just have no other option.

### 5. How do I assert an array of strings in Playwright ?

`expect(locator).toHaveText(array)` - this can actually take an array and it will, under the hood, iterate over it to assert the items exist

### 6. How to handle multiple elements in Playwright ?

Playwright locators will find both one element or multiple elements with the same method, depends on what you give it. If you want to deal with multiple elements (in Selenium you would do `findElements` and it will return an array of elements) , in Playwright if you do `page.locator(multipleElements)` this will return multiple elements but JUST for playwright under the hood, not for you (at least not as simple array of objects). All you get is one single locator (object). Why this happens? Because you may want to find one single element and try maybe to click it, so when you perform the action, in case the attribute belongs to multiple elements it will not let you click on multiple elements and it will throw an error, suggesting you how to fix your test. But if you really want to deal with all the elements, how do I do that ? You have to add `.all()` at the end. Example: `page.locator(multipleElements).all()`

### 7. How to handle bigger chunks of text ?

Sometimes you can have multiple items that have text, if you give to playwright the parent of all the items, then playwright can extract those texts as an array. You can achieve that with `page.locator(parentOfElementsWithText).allTextContents()` or with `.allInnerTexts()` . The downside of this is that it is not recommended to assert exact match of text, mainly because sometimes it will fetch new lines (/n) or commas or extra spaces, but it can be useful to use this with an `expect(locator).toContain()`

### 8. How do I assert the absence of an element in Playwright ?

You can find this little hack on stackoverflow `expect(locator).toHaveCount(0)`. This is similar to Selenium `findElements`, and if element not found, it just returns an empty array. This is good because it will not fail the test. However I do recommend the Playwright builtin NOT operator , which is what you should actually use. Example `locator(element).not.toBeVisible()`. As a general practice all assert methods can be used with NOT.

### 9. How do I deal with the situation where simple use of just getByTestId() is not enough ? 

There are various scenarios where you have maybe tables, where you need to combine parents and children web elements to reach your desired data. So, before going to `locator()` to build your super complex `css selectors` or `xpath`, think about using the and operator , for example `page.getByText(elem).and(page.getByText(elem)` or you can do `page.getByTestId(elem).getByTestId(elem)` . Another option would be to use filtering locators. Filters also can be used with the mix of NOT operator

### 10. Using one parent element to do multiple actions

You can store the element in a `const element = page.locator()` . Notice that this scenario is one of the few times when you don't need the `await` in front of `page.locator()`. You can look at it as a placeholder. Later in your code you can do `element.click()` or search thru its children `element.getByTestId(child)` . The cool feature about this is that every time you call the element it will re-query the DOM.

### 11. Do not use $(locator) or $$(multiple)

Use of $ or $$ in Playwright is element handle and its highly discouraged by playwright. The main idea about this is that you can encounter situations where using $ will just reference an element from a previous version of the DOM. You may end up seeing the well known Selenium error `StaleElementReferenceException`

### 12. What if you have to wait for the app response and it takes longer than the usual 5 seconds ?

You may encounter this, for example when you perform an action, a click, a submit, and it takes a longer time to get an answer, either you will have the 'spinning loader' or similar. If you know this happens you can increase the timeout to wait for a response on just one particular action, for example `expect(locator).toBeVisible({ timeout: 20000 })` . By passing the timeout inside the method this will override the default playwright configs only for that single line of code (hopefully you will no longer see the  error `error: timed out 5000ms waiting for expect(locator).tobevisible()` and your test will pass)

### 13. Why sometimes expect does not have the usual toHaveText() method ?

But if I try `toBe()` then it works. Because it depends what you are giving to `expect()` as an object. If you give it a standard locator it will have all the web first assertions but if you give it a modified object, something similar to `page.locator(elementWithText).innerText()` , because of the `innerText()` method this is no longer a standard playwright object, and with the rest of the objects it uses the jest expect methods. The playwright expect object will detect what type of object you give it and will work with either the jest expect like `toBe()` or the playwright methods like `toHaveText()`. Just remember that the playwright methods have auto-wait retry and the jest one does not.

### 14. Multiple data test ID attributes on web elements

We can configure in our playwright to use a custom test id attribute. When using the method `getByTestId()` Playwright will default to `data-testid="selector"` , so in case you have a different kind of default id locator set in your web app, you have to set up your testIdAttribute first. This includes if your website has unique ids under the format `id="selector"` . But can you config to use multiple test id attributes? Answer is NO. However, there is a small hack. You can use a different `testIdAttribute` in your general config and have a different one in your projects. Or you can have different projects with different IDs. This is used when you have two teams or group of teams that developed one app using one type of unique ID and another with a different one. It may be a situation of migrating away from a legacy app. Here is below an example

`  projects: [
    {
      name: "new-mega-awesome-app",
      testDir: "./tests",
      use: {
        ...devices["Desktop Chrome"],
        testIdAttribute: "id",
        baseURL: "https://newapp.domain.com",
      },
    },
    {
      name: "legacy-app",
      testDir: "./tests-for-legacy-app",
      use: {
        ...devices["Desktop Chrome"],
        testIdAttribute: "data-testid",
        baseURL: "https://legacyapp.domain.com",
      },
    },
  ],`

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Subscribe for more...

Feel free to checkout other nice tips on my blog. I highly recommend this post about publishing your playwright reports directly on github pages, giving you easy access to your run results. Or maybe you would like to try my method in learning new automation frameworks.
Playwright tips and tricks #2I’ve written a post about tips and tricks and it got a lot of love. So, I’ve decided to do another one. 1. How to handle an element that appears after full page load in playwright One of those rare cases where an element will appear in DOM only afterMartioliAdrian MaciucPlaywright tips and tricks #3Get more details about your test during test run. How to use Playwright to test multiple browser windows. Promise.all in PlaywrightMartioliAdrian MaciucPublish your playwright reports to github pagesWhat I am looking to achieve here is a free solution to have my reports published into separate sub-directories, accessible at an unique link that I can easily put in my Jira ticket, github issue or other test management tool, so I can show my client/manager/team what isMartioliAdrian MaciucHow can I learn [more efficient] a new testing framework ?There are so many languages and software testing frameworks out there that sometimes it feels overwhelming. You have Selenium, Cypress, Playwright, TestCafe, Protractor, Robot Framework, TestComplete, Karate, Nightwatch.js, Webdriver.io and many more. You may find yourself in a situation where a new…MartioliAdrian Maciuc

---

<!-- source: our-playwright-testing-standards-at-houseful.md -->

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

---

<!-- source: hidden-gems-of-playwright-part-2-by-andrey-enin.md -->

[Sitemap](https://adequatica.medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fadequatica.medium.com%2Fhidden-gems-of-playwright-part-2-ca3e38a5954a&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fadequatica.medium.com%2Fhidden-gems-of-playwright-part-2-ca3e38a5954a&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

# Hidden Gems of Playwright: Part 2

[![Andrey Enin](https://miro.medium.com/v2/resize:fill:32:32/2*bQ4xmdoPtrdMchCQCia7iQ.png)](https://adequatica.medium.com/?source=post_page---byline--ca3e38a5954a---------------------------------------)

[Andrey Enin](https://adequatica.medium.com/?source=post_page---byline--ca3e38a5954a---------------------------------------)

Follow

5 min read

·

May 23, 2024

77

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Dca3e38a5954a&operation=register&redirect=https%3A%2F%2Fadequatica.medium.com%2Fhidden-gems-of-playwright-part-2-ca3e38a5954a&source=---header_actions--ca3e38a5954a---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:1000/1*GqcxaWY9hkIClXniW67-cw.jpeg)

[In the previous part](https://adequatica.medium.com/hidden-gems-of-playwright-68fcf8896bcb), I highlighted some notable Playwright methods that made testing easier when I started using it as a default test automation framework in production.

Here I continued to pick up some interesting features of that tool:

- [Rewrite global config for any test](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#447c)
- […devices](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#89ec)
- [setOffline](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#9ee5)
- [expect.toPass](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#c3e8)
- [waitForSelector (deprecated, but works) / waitFor](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#fe05)
- [CI reporter for GitHub Actions](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#d825)
- [last-failed](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#1bc4)
- [Boxed steps](https://adequatica.medium.com/hidden-gems-of-playwright-part-2-ca3e38a5954a#997c)

Some of them are «illustrated» in this [GitHub repository](https://github.com/adequatica/ui-testing).

## Rewrite global config for any test

Docs: [Test use options](https://playwright.dev/docs/test-use-options), [TestOptions](https://playwright.dev/docs/api/class-testoptions/)

Playwright provides a [config file to manage all options](https://playwright.dev/docs/test-configuration) for running your tests. However, some tests may require a completely different setup for the base URL, browser settings, or a special user's environment (viewport, geolocation, etc.).

To rewrite the global config, you can set the necessary parameters for a particular single test via the `test.use()` [method](https://playwright.dev/docs/api/class-test#test-use) at the top of a test:

```
test.use({
  baseURL: 'http://localhost:3000',
  ...devices['Pixel 7'],
});

test('Home page on mobile', async ({ page }) => {
  await test.step('Open the page', async () => {
    await page.goto('/');
  });
});
```

## …devices

[Doc](https://playwright.dev/docs/emulation#devices)

Another advantage of Playwright's configuration options is its device «emulation». Instead of setting a custom User Agent, the viewport size, and other settings for mobile browsers, you can directly specify the required device in the config (or rewrite the config through `test.use()` as shown above):

```
use: {
  ...devices['iPhone 14'],
},
```

The whole list of devices can be found in [Plyawright's repository](https://github.com/microsoft/playwright/blob/main/packages/playwright-core/src/server/deviceDescriptorsSource.json).

## setOffline

[Doc](https://playwright.dev/docs/api/class-browsercontext#browser-context-set-offline)

I used it in one test, especially to check the application's behavior in case of a loss of connection. Offline mode turns on through [BrowserContext](https://playwright.dev/docs/api/class-browsercontext):

```
test('Go offline', async ({ browser, page }) => {
  await test.step('Open the page', async () => {
    const context = await browser.newContext();
    page = await context.newPage();
    await page.goto('/');
    await context.setOffline(true);
  });
```

**Watch out, this is not fully offline mode.** Network activity will stop (as an emulation of a network being offline), but you will not be able to test features of your application that use [online/offline events](https://developer.mozilla.org/en-US/docs/Web/API/Window/offline_event) in the [addEventListener() method](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener):

```
// If your application's code has this:
window.addEventListener('offline', (event) => {});
// Then browserContext.setOffline(true) won't work
```

## expect.toPass

[Doc](https://playwright.dev/docs/test-assertions#expecttopass)

That trickiest method allows «retry» the [assertion](https://playwright.dev/docs/test-assertions) inside `expect`:

```
await expect(async () => {
  // Retry by intervals until the request is successful
  const response = await page.request.get('https://sso-motd-api.web.cern.ch/api/motd/');
  expect(response.status()).toBe(200);
}).toPass({
  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe
  intervals: [1000, 2000, 10000],
  // toPass timeout does not respect custom expect timeout
  timeout: 60000,
});
```

This is extremely useful for checking unreliable backend responses.

> There is also a similar, but not quite, `expect.poll` [method](https://playwright.dev/docs/test-assertions#expectpoll), which implements the idea of [HTTP polling](https://medium.com/cache-me-out/http-polling-and-long-polling-bd3f662a14f#0f5c) inside assertions.

## waitForSelector (deprecated, but works) / waitFor

[Doc](https://playwright.dev/docs/api/class-elementhandle#element-handle-wait-for-selector)

## Get Andrey Enin's stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

This is another brilliant method suitable for checking selectors.

[There is a recommendation](https://adequatica.medium.com/principles-of-writing-automated-tests-a2b72218264c#f94b) that **assertions should not be placed inside** [**page object models**](https://playwright.dev/docs/pom), even despite the implementation example in Playwright itself.

Press enter or click to view image in full size

![Please, do not do that inside pageObjects](https://miro.medium.com/v2/resize:fit:700/1*9ItTFE2kQ2BonWtFf7jviA.png)

_Please, do not do that inside pageObjects_

Instead, you can wait for the required selector without explicit assert/expect:

```
// Page's toolbar object
export class Toolbar {
  private page: Page;
  private toggleLocator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.toggleLocator = page.locator('[class*=toggle]');
  }

  async clickOnToggle(): Promise<void> {
    await this.toggleLocator.click();
    // Deprecated, use locator-based locator.waitFor() instead
    await this.page.waitForSelector('[data-testid="dropdown-menu"]');
  }
}
```

Unfortunately, **this method is deprecated,** and `waitFor()` must be used. So the pageObject's code above should be rewritten as follows:

```
// Page's toolbar object
export class CernToolbar {
  private page: Page;
  private toggleLocator: Locator;
  private dropdownMenu: Locator;

  constructor(page: Page) {
    this.page = page;
    this.toggleLocator = page.locator('[class*=toggle]');
    this.dropdownMenu = page.getByTestId('dropdown-menu');
  }

  async clickOnToggle(): Promise<void> {
    await this.toggleLocator.click();
    await this.dropdownMenu.waitFor({state: 'visible'});
  }
}
```

## CI reporter for GitHub Actions

[Doc](https://playwright.dev/docs/test-reporters#github-actions-annotations)

If you are using [GitHub Actions](https://docs.github.com/en/actions/automating-builds-and-tests/about-continuous-integration) for your CI/CD, then `github` [reporter](https://playwright.dev/docs/test-reporters) is your «must have» config option:

```
// 'github' for GitHub Actions CI, and 'list' when running locally
reporter: process.env.CI ? 'github' : 'list',
```

Documentation tells that this reporter has _annotations_ without describing what it is. These annotations look like very useful widgets inside PR's diff in the place of a failed code line.

Press enter or click to view image in full size

![github reporter annotations in PR](https://miro.medium.com/v2/resize:fit:700/1*7YM1yDhsJfqVlalooK7ogQ.png)

_github reporter annotations in PR_

`github` reporter's report in a workflow's job looks like a normal `list` report.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*zzuw0WClgGOCxeehXiGAVQ.png)

_github reporter in the job_

## last-failed

[CLI Docs](https://playwright.dev/docs/test-cli#reference)

The new CLI option in the latest release ( [1.44](https://playwright.dev/docs/release-notes#version-144)) brought the ability to run only tests that failed in the previous run.

This is a significant improvement for Playwright's test runner. Earlier, we had to develop custom scripts to rerun only failed tests, but now it works out of the box.

Press enter or click to view image in full size

![last-failed option runs only failed test](https://miro.medium.com/v2/resize:fit:700/1*_ClgjN-zvyqGKOggTHMEsw.png)

_last-failed option runs only failed test_

**UPDATE:** one more highly useful CLI option appeared with release [1.46](https://playwright.dev/docs/release-notes#--only-changed-cli-option): `--only-changed`— it allows to run only changed tests (test files) since the last git commit. It really speeds up the local development and debugging of tests.

Read more:

- [Iterate quickly using the new — only-changed option](https://dev.to/playwright/iterate-quickly-using-the-new-only-changed-option-55m2).

## Boxed steps

When I first read the [release notes](https://playwright.dev/docs/release-notes#hide-implementation-details-box-test-steps) about a new option for the `test.step()` [method](https://playwright.dev/docs/api/class-test#test-step), I was confused and considered it useless. But later, I realized that it could be useful for steps with «helpers».

If you develop test automation for fairly complex applications, sooner or later, you will have to add abstraction layers inside tests to perform repetitive and/or compound actions. These pieces of code are usually called helpers or utils and are imported into tests for execution as their steps.

Sometimes, developers can be frustrated by redundant test reports and want to see only the upper-level failures. The «box» step serves exactly this scenario — it hides error details of the test's inner helper functions:

```
// Helper
async function openHomePage(page: Page) {
  await page.goto('/');
  await expect(page, 'Should open / page').toHaveURL(/.*\//);
  await expect(page.getByRole('main')).toBeVisible();
}

test('Home page toolbar about overlay on mobile',
  async ({ page }) => {
    await test.step('Open the page', async () => openHomePage(page), {
      box: true,
    });
```

Press enter or click to view image in full size

![HTML report: on the left — {box: true}, on the right is an ordinary test step](https://miro.medium.com/v2/resize:fit:700/1*381I-uFUpIPnxQviZF7cAw.png)

HTML report: on the left — {box: true}, on the right is an ordinary test step

Anyway, that is quite a controversial feature, and **it depends a lot on the helper functions and the assertions inside them (your errors may look completely different than in the example above),** as well as the test requirements.

Read more:

- [Box Test Steps in Playwright](https://dev.to/playwright/box-test-steps-in-playwright-15d9).

Take a look at [part 1](https://adequatica.medium.com/hidden-gems-of-playwright-68fcf8896bcb).

[Testing](https://medium.com/tag/testing?source=post_page---footer_tags--ca3e38a5954a---------------------------------------)

[Automation Testing](https://medium.com/tag/automation-testing?source=post_page---footer_tags--ca3e38a5954a---------------------------------------)

[Javascript Testing](https://medium.com/tag/javascript-testing?source=post_page---footer_tags--ca3e38a5954a---------------------------------------)

[Playwright Test](https://medium.com/tag/playwright-test?source=post_page---footer_tags--ca3e38a5954a---------------------------------------)

[Playwright Community](https://medium.com/tag/playwright-community?source=post_page---footer_tags--ca3e38a5954a---------------------------------------)

[![Andrey Enin](https://miro.medium.com/v2/resize:fill:48:48/2*bQ4xmdoPtrdMchCQCia7iQ.png)](https://adequatica.medium.com/?source=post_page---post_author_info--ca3e38a5954a---------------------------------------)

[![Andrey Enin](https://miro.medium.com/v2/resize:fill:64:64/2*bQ4xmdoPtrdMchCQCia7iQ.png)](https://adequatica.medium.com/?source=post_page---post_author_info--ca3e38a5954a---------------------------------------)

Follow

[**Written by Andrey Enin**](https://adequatica.medium.com/?source=post_page---post_author_info--ca3e38a5954a---------------------------------------)

[812 followers](https://adequatica.medium.com/followers?source=post_page---post_author_info--ca3e38a5954a---------------------------------------)

· [0 following](https://adequatica.medium.com/following?source=post_page---post_author_info--ca3e38a5954a---------------------------------------)

Quality assurance engineer: I'm testing APIs, web applications and do automation testing

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----ca3e38a5954a---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----ca3e38a5954a---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----ca3e38a5954a---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----ca3e38a5954a---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----ca3e38a5954a---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----ca3e38a5954a---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----ca3e38a5954a---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----ca3e38a5954a---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----ca3e38a5954a---------------------------------------)
