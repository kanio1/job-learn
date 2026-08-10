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

