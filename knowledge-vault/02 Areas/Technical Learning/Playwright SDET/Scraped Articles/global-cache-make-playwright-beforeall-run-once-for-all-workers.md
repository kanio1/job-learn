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

