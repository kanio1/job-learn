---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-iteration-5-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Vitaliy Potapov | Authentication in Playwright: You Might Not Need Project Dependencies | https://vitalets.github.io/posts/playwright/authentication-without-project-dependencies/ | complete |
| 2 | Vitali Haradkou | Graceful test cancellation with `AbortSignal` | https://dev.to/vitalicset/stop-leaking-resources-how-to-use-abortsignal-in-playwright-tests-jb2 | complete |
| 3 | Vitali Haradkou | Real containers via a Playwright fixture | https://vitalicset.hashnode.dev/playwright-labs-testcontainers | complete |
| 4 | Vitali Haradkou | Playwright Labs: Best Practices as Code | https://dev.to/vitalicset/introducing-playwright-labs-best-practices-as-code-198n | complete |
| 5 | Vitali Haradkou | Email-safe Playwright report | https://vitalicset.hashnode.dev/playwright-email-react | complete |
| 6 | TestDino (Pratik Patel) | Playwright 1.62: isolated retries, AbortSignal and more | https://testdino.com/blog/playwright-1-62-release | metadata-only |
| 7 | Viktor Konovalov | Validate the accessibility tree, not the DOM | https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7472539768522940418-yMME | complete |
| 8 | Viktor Konovalov | Playwright tip: stop waiting blindly — use `expect.poll()` | https://www.linkedin.com/posts/viktorkonovalovqa_playwright-tip-stop-waiting-blindly-use-activity-7449021903387844608-Oes9 | complete |
| 9 | Viktor Konovalov | Playwright tip: use `expect.toPass()` when a single assertion retry is not enough | https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7468564794011246592-LFyP | unavailable |
| 10 | Stefan Minchev | Stop writing while loops to wait for a database update | https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7460346882448392192-yn6y | complete |
| 11 | Currents.dev | Playwright testing in Staging vs Production | https://currents.dev/posts/playwright-testing-staging-vs-production | complete |
| 12 | ScrollTest / Pramod Dutta | Playwright Actions and Auto-Waiting: Day 3 | https://scrolltest.com/playwright-actions-auto-waiting-day-3/ | complete |
| 13 | Butch Mayhew | Playwright-cli Boosts Token Efficiency for Coding Agents | https://www.linkedin.com/posts/butchmayhew_playwright-cli-just-changed-how-i-work-with-activity-7426626393586884608-BYI4 | complete |
| 14 | ScrollTest / Pramod Dutta | Playwright TypeScript Checklist | https://scrolltest.com/playwright-typescript-checklist/ | complete |
| 15 | Joseph Ward | Blog index | https://josephward.tech/blog/ | complete |
| 16 | Playwright | Best Practices | https://playwright.dev/docs/best-practices | complete |
| 17 | Playwright | Fixtures | https://playwright.dev/docs/test-fixtures | complete |
| 18 | Playwright | Assertions | https://playwright.dev/docs/test-assertions | complete |
| 19 | Playwright | Configuration | https://playwright.dev/docs/test-configuration | complete |

## Artykuły

### 1. Vitaliy Potapov — Authentication in Playwright: You Might Not Need Project Dependencies

- Source: https://vitalets.github.io/posts/playwright/authentication-without-project-dependencies/
- Retrieved: 2026-08-29
- Exa status: complete

# Authentication in Playwright: You Might Not Need Project Dependencies

24 Oct, 2025

If your Playwright suite mixes public pages and authenticated areas, or you test multiple roles like user and admin, the standard “setup project” approach for auth can make your runs slower than they need to be.

Here’s a simpler pattern that authenticates on demand, plays nicely with sharding, and keeps your config to one project per browser.

## The usual way: a “setup” project

The Playwright docs on authentication recommend authenticating in a dedicated setup project and declaring it as a dependency for your browser projects. The dependency project runs first and prepares `storageState` that other projects reuse.

That’s convenient and visible in reports, but there’s a cost.

## The big drawback: setup runs even when not needed

From the docs:

> The setup project will always run and authenticate before all the tests

In practice this means:

- You have public and authenticated tests. Even if you run only public tests, the setup still authenticates.
- You have multiple roles. The docs show multiple signed in roles where the setup logs in both admin and user. If you run only admin tests, the user login still happens because the setup runs in full.
- It gets worse with sharding. Imagine 2 shards:
  - Shard 1 executes `user.spec.ts`
  - Shard 2 executes `admin.spec.ts`

With project dependencies, both shards still execute the full setup up front, repeating both logins unnecessarily.

You can split the config into separate projects per role, each with its own setup. That works, but once you add different browsers, the matrix grows and your config becomes hard to reason about. I prefer one project per browser and nothing else.

## A simpler idea

What I really wanted:

1. The first test that actually needs auth does the sign-in while others wait.
2. Once done, the same storage state is shared across workers/tests that need it.
3. Optionally, persist the state for a while on disk so local runs become instant.

That’s exactly what I use today with @global-cache/playwright, which I built after running into these downsides. Here’s how.

## Code: multi-role auth on demand (no setup project)

I’ll use the example from the docs with admin and user roles. But instead of putting both logins into a setup project, I create a helper function and wrap authentication steps in `globalCache.get()`:

```typescript
// tests/helpers/auth.ts

import type { Browser } from '@playwright/test';

import { expect } from '@playwright/test';

import { globalCache } from '@global-cache/playwright';

/**
 * Performs sign-in for a given role and caches the auth state for the whole run.
 */
export async function signIn(browser: Browser, role: 'admin' | 'user') {
  return globalCache.get(`auth-state-${role}`, async () => {
    console.log(`Signing-in as: ${role}`);

    const page = await browser.newPage();

    // Perform authentication steps. Replace these actions with your own.
    await page.goto('https://github.com/login');
    await page.getByLabel('Username or email address').fill(role);
    await page.getByLabel('Password').fill('password');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait until the page reaches a state where all cookies are set.
    await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();

    // Return authenticated state (cookies + localStorage)
    return page.context().storageState();
  });
}
```

Now each spec calls this helper inside a `storageState` fixture to authenticate only the role it needs, and only if a test actually runs:

### `tests/admin.spec.ts`

```typescript
import { test } from '@playwright/test';
import { signIn } from './helpers/auth';

// Make all tests in this file run as "admin"
test.use({
  storageState: async ({ browser }, use) => {
    const state = await signIn(browser, 'admin');
    await use(state);
  },
});

test('admin: sees dashboard', async ({ page }) => {
  await page.goto('/admin');
  // ...
});
```

### `tests/user.spec.ts`

```typescript
import { test } from '@playwright/test';
import { signIn } from './helpers/auth';

// Make all tests in this file run as "user"
test.use({
  storageState: async ({ browser }, use) => {
    const state = await signIn(browser, 'user');
    await use(state);
  },
});

test('user: can view profile', async ({ page }) => {
  await page.goto('/me');
  // ...
});
```

## Running the tests

Below are 4 common runs to check the authentication setup:

### 1) Run everything (user + admin)

```bash
$ npx playwright test

Running 2 tests using 2 workers

  ✓  1 test/admin.spec.ts:11:5 › admin: sees dashboard (2.7s)
  ✓  2 test/user.spec.ts:11:5 › user: can view profile (2.8s)

Signing in as: admin
Signing in as: user

  2 passed (3.5s)
```

What happens: user and admin authenticate once each, then all tests reuse their storage states.

### 2) Run only “user” tests

```bash
$ npx playwright test tests/user.spec.ts

Running 1 test using 1 worker

  ✓  1 test/user.spec.ts:11:5 › user: can view profile (2.8s)

Signing in as: user

  1 passed (2.8s)
```

What happens: only the user role authenticates; admin never runs.

### 3) Run only “admin” tests

```bash
$ npx playwright test tests/admin.spec.ts

Running 1 test using 1 worker

  ✓  1 test/admin.spec.ts:11:5 › admin: sees dashboard (2.7s)

Signing in as: admin

  1 passed (2.7s)
```

What happens: only the admin role authenticates; user never runs.

### 4) Run on two shards (split by files)

Shard 1:

```bash
$ npx playwright test --shard=1/2

Running 1 test using 1 worker, shard 1 of 2

  ✓  1 test/admin.spec.ts:11:5 › admin: sees dashboard (2.7s)

Signing in as: admin

  1 passed (3.7s)
```

Shard 2:

```bash
$ npx playwright test --shard=2/2

Running 1 test using 1 worker, shard 2 of 2

  ✓  1 test/user.spec.ts:11:5 › user: can view profile (2.7s)

Signing in as: user

  1 passed (3.6s)
```

What happens: the first shard authenticates admin; the second shard authenticates user. Each shard pays only for its role, which is why this setup executes faster than a multi-role dependency project.

In all examples, authentication runs only for roles that your tests actually touch. In practice this cuts setup time and makes your tests run faster.

## Bonus: persistent auth for local dev

For local development, you can keep login state on disk for a limited time. For example, to cache for 1 hour, add a `{ ttl: '1 hour' }` parameter to the authentication call:

```typescript
await globalCache.get(`auth-state-${role}`, { ttl: '1 hour' }, async () => {
  // ...perform login and return storageState()
});
```

The cache files live under `.global-cache` by default. You can inspect them and delete them to force a cache update.

## How to enable Global Cache

To enable Global Cache, wrap your Playwright config with `globalCache.wrap()`:

```typescript
// playwright.config.ts

import { defineConfig } from '@playwright/test';
import { globalCache } from '@global-cache/playwright';

const config = defineConfig({
  projects: [
    { name: 'chromium' }, // keep it simple: one project per browser
  ],
  // ...any other options
});

export default globalCache.wrap(config);
```

## Notes

- If tests modify server-side state and you need isolation per worker, scope the cache key by worker:

  ```typescript
  const key = `auth-state-${role}-${testInfo.workerIndex}`;
  ```

- Make cache keys role-aware and environment-aware. Include tenant/locale and base URL if you test multiple envs:

  ```typescript
  const key = `auth-${role}-${locale}-${envName}`;
  ```

- Prefer API-based login for speed and stability. See the example: auth via API with global cache.

## Side-by-side: dependency project vs Global Cache

| Aspect | Dependency project | Global Cache |
| --- | --- | --- |
| When auth runs | Always before tests | Only when a test needs it |
| Multiple roles | All roles log in up front | Only selected roles log in |
| Sharding | Each shard pays full setup | Each shard computes only its role |
| Config | Projects × roles × browsers | One project per browser |
| Persist across runs | Roll your own | TTL for local runs |

## Wrap-up

Project dependencies are solid and officially recommended. But when you mix public pages, multiple roles, or heavy sharding, paying the authentication cost every time adds up. By authenticating on demand and sharing that state, you keep config lean, you only log in when a test truly needs it, and your local loop becomes fast with a short TTL.

If that sounds familiar, give this pattern a try and share your feedback ❤️

### 2. Vitali Haradkou — Graceful test cancellation with `AbortSignal`

- Source: https://dev.to/vitalicset/stop-leaking-resources-how-to-use-abortsignal-in-playwright-tests-jb2
- Retrieved: 2026-08-29
- Exa status: complete

# Stop Leaking Resources: How to Use AbortSignal in Playwright Tests

If you write Playwright tests that make HTTP requests, call APIs, or perform any long-running async work, you probably have a resource leak problem you do not know about.

In this tutorial, I will walk you through the problem, explain why standard cleanup patterns fall short, and show you how to integrate `AbortController`/`AbortSignal` into your Playwright tests using the `@playwright-labs/fixture-abort` package.

## The Problem: Zombie Requests After Test Timeouts

Consider a straightforward Playwright test that calls an API:

```javascript
test('should fetch user profile', async () => {
  const response = await fetch('https://api.example.com/users/123');
  const user = await response.json();
  expect(user.name).toBe('Alice');
});
```

If this test times out - maybe the API is slow, maybe the server is under load - Playwright stops the test. But the `fetch` call does not get cancelled. The request continues running until it either completes or hits its own timeout (often 30 seconds or more by default).

In isolation, this is harmless. At scale - hundreds of tests, multiple workers, CI running every push - it creates real problems:

- Connection pool exhaustion: Your test infrastructure runs out of available connections.
- Server overload: Your staging environment handles requests nobody is waiting for.
- Misleading logs: Errors from orphaned requests appear in server logs, confusing debugging efforts.
- Cascading failures: Resource exhaustion in one service affects others in your staging environment.

## Why afterEach Does Not Solve This

Your first instinct might be to clean up in an `afterEach` hook. But there is a fundamental issue: by the time `afterEach` runs, you have no reference to the in-flight requests. The `fetch` promise is trapped inside the timed-out test function. You cannot cancel what you cannot reach.

What you need is a cancellation token that you pass into every async operation upfront - something that can be triggered externally when the test ends.

This is exactly what `AbortController` and `AbortSignal` were designed for.

## Enter AbortSignal

`AbortController` is a web standard (also available in Node.js) that provides a mechanism for cancelling async operations:

```javascript
const controller = new AbortController();
const signal = controller.signal;

// Pass signal to fetch
fetch('/api/data', { signal });

// Later, cancel the request
controller.abort(); // The fetch rejects with AbortError
```

The `@playwright-labs/fixture-abort` package wires this pattern directly into Playwright's test lifecycle.

## Setting Up fixture-abort

Install the package:

```bash
npm install @playwright-labs/fixture-abort
```

The package extends Playwright's `test` with fixtures built in:

- `abortController` - an `AbortController` instance, fresh for each test
- `signal` - the associated `AbortSignal`
- `useAbortController(options?)` - returns the controller with optional abort callback
- `useSignalWithTimeout(ms)` - returns a signal that auto-aborts after the given duration

Import `test` and `expect` from the package instead of `@playwright/test`, and the fixtures are ready to use.

## Basic Usage: Cancellable Fetch

Here is the simplest pattern - passing the signal to a fetch call:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should fetch user profile', async ({ signal }) => {
  const response = await fetch('https://api.example.com/users/123', {
    signal
  });
  const user = await response.json();
  expect(user.name).toBe('Alice');
});
```

If the test times out, `signal` fires, and the fetch call is immediately cancelled. No orphaned request. No wasted server resources.

## Pattern: Polling with Cooperative Cancellation

Many tests need to poll an endpoint until a condition is met. Without abort signals, a timeout leaves the polling loop running in the background. With `signal`, you get clean cooperative cancellation:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should wait for order processing', async ({ signal }) => {
  const orderId = await createOrder();

  while (!signal.aborted) {
    const response = await fetch(`/api/orders/${orderId}`, {
      signal
    });
    const order = await response.json();

    if (order.status === 'completed') {
      expect(order.total).toBeGreaterThan(0);
      return;
    }

    // Wait 2 seconds before next poll
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
});
```

The `while (!signal.aborted)` check means the loop exits cleanly when the signal fires. The fetch call inside the loop is also protected by the same signal. Double coverage.

## Pattern: Multiple Parallel Requests

When a test fires multiple requests in parallel, all of them need to be cancellable:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should fetch dashboard data', async ({ signal }) => {
  const [users, orders, metrics] = await Promise.all([
    fetch('/api/users', { signal }),
    fetch('/api/orders', { signal }),
    fetch('/api/metrics', { signal })
  ]);

  expect(users.ok).toBe(true);
  expect(orders.ok).toBe(true);
  expect(metrics.ok).toBe(true);
});
```

One signal, three requests, all cancelled together if the test times out.

## Pattern: Manual Abort for Early Exit

You are not limited to timeout-driven cancellation. You can abort manually based on test logic:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should stop on first error', async ({ signal, abortController }) => {
  const items = await getItemsToProcess();

  for (const item of items) {
    if (signal.aborted) break;

    const response = await fetch(`/api/process/${item.id}`, {
      method: 'POST',
      signal
    });

    if (!response.ok) {
      abortController.abort(); // Cancel remaining work
      break;
    }
  }
});
```

## Pattern: Abort Controller with Callback

Use `useAbortController` to register a callback that fires on abort:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should handle abort with cleanup', async ({ useAbortController, signal }) => {
  const controller = useAbortController({
    onAbort: () => console.log('Operation cancelled, cleaning up'),
    abortTest: true
  });

  const response = await fetch('/api/long-operation', { signal });
  const data = await response.json();
  expect(data).toBeDefined();
});
```

## Pattern: Signal with Timeout

Use `useSignalWithTimeout` to get a signal that auto-aborts after a specific duration:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should complete within 5 seconds', async ({ useSignalWithTimeout }) => {
  const timeoutSignal = useSignalWithTimeout(5000);

  const response = await fetch('/api/slow-endpoint', {
    signal: timeoutSignal
  });
  expect(response.ok).toBe(true);
});
```

## Pattern: Passing Signal to Third-Party Libraries

Many modern libraries accept an `AbortSignal`. You can pass `signal` to anything that supports it:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should query database', async ({ signal }) => {
  // Many DB clients accept abort signals
  const result = await db.query('SELECT * FROM users', {
    signal
  });
  expect(result.rows.length).toBeGreaterThan(0);
});
```

This works with Axios (via `signal` option), the Node.js `fetch` implementation, many database drivers, gRPC clients, and more.

## Custom Expect Matchers

The package also provides custom expect matchers for testing abort states:

```javascript
import { test, expect } from '@playwright-labs/fixture-abort';

test('should verify abort state', async ({ signal, abortController }) => {
  expect(signal).toBeActive();

  abortController.abort('test reason');

  expect(signal).toBeAborted();
  expect(signal).toBeAbortedWithReason('test reason');
  expect(abortController).toHaveAbortedSignal();
});

test('should verify timeout signal aborts', async ({ useSignalWithTimeout }) => {
  const timeoutSignal = useSignalWithTimeout(100);
  await expect(timeoutSignal).toAbortWithin(150);
});
```

## How It Works Under the Hood

The implementation is straightforward:

1. Before each test, a fresh `AbortController` is created via Playwright's fixture system.
2. The controller and its signal are made available as `abortController` and `signal` fixtures.
3. When the test times out, the controller is aborted.
4. Any operation listening to the signal receives an `AbortError` and stops.
5. After each test, the controller is cleaned up.

This means every test gets its own isolated cancellation scope. One test timing out does not affect any other test.

## Common Mistakes to Avoid

Do not create your own AbortController when the fixture provides one. The whole point is that the fixture's controller is wired into the test lifecycle. A manually created controller will not auto-abort on timeout.

Do not forget to pass the signal. An unprotected `fetch()` call without `signal` is still vulnerable to the zombie request problem. Make it a habit: every async operation gets the signal.

Do not swallow AbortError silently. When a signal fires, operations reject with `AbortError`. This is expected behavior during timeouts. Let Playwright handle the timeout reporting rather than catching and hiding the error.

## Getting Started

```bash
npm install @playwright-labs/fixture-abort
```

Full source code and documentation: github.com/vitalics/playwright-labs

The package is part of the `@playwright-labs` monorepo. Import `test` and `expect` from `@playwright-labs/fixture-abort` instead of `@playwright/test`, and the abort fixtures are ready to use in every test.

Give it a try. Your staging servers will thank you.

### 3. Vitali Haradkou — Real containers via a Playwright fixture

- Source: https://vitalicset.hashnode.dev/playwright-labs-testcontainers
- Retrieved: 2026-08-29
- Exa status: complete

# Stop Mocking Your Database: Real Containers in Playwright Tests with @playwright-labs/fixture-testcontainers

A deep dive into @playwright-labs/fixture-testcontainers — a Playwright fixture that brings real Docker containers into your tests with automatic cleanup

Updated March 27, 2026

Integration tests that hit a real database, a real Redis instance, or a real message broker are fundamentally more trustworthy than mocked alternatives. Yet wiring up Docker containers in a Playwright test suite has always been more ceremony than it should be — manual `beforeAll` / `afterAll` hooks, manual cleanup, and a pile of boilerplate that has nothing to do with what you're actually testing.

`@playwright-labs/fixture-testcontainers` removes all of that. It is a thin Playwright fixture layer on top of Testcontainers that gives you real Docker containers, scoped to a single test, with zero cleanup code on your part.

## Why a Playwright fixture?

Testcontainers already works in Node.js. You can call `new GenericContainer("redis:8").start()` anywhere. The problem is lifecycle management:

- If you start a container in `beforeAll`, you need to stop it in `afterAll` — and hope no test throws before cleanup runs.
- If you start one inside a test, you need a `try/finally` around the entire body.
- Playwright's fixture system already solves exactly this problem for browser instances, pages, and custom state. There was no reason containers should be any different.

The fixture approach gives you a `useContainer` function injected directly into the test, and the container is stopped (in parallel with any other containers) the moment the test ends — pass or fail.

## Installation

```shell
npm install -D @playwright-labs/fixture-testcontainers testcontainers
```

## The two fixtures

### `useContainer`

Starts a container from an image name or a pre-configured `GenericContainer`. Every option maps directly to a `GenericContainer.with*` method, so there is no new API surface to learn.

```typescript
import { test } from "@playwright-labs/fixture-testcontainers";
import { Wait } from "testcontainers";

test("postgres integration", async ({ useContainer }) => {
  const pg = await useContainer("postgres:16", {
    ports: 5432,
    environment: { POSTGRES_PASSWORD: "secret" },
    waitStrategy: Wait.forLogMessage("ready to accept connections"),
    startupTimeout: 30_000,
  });

  const port = pg.getMappedPort(5432);
  // connect, run queries, make assertions
  // container.stop() is called automatically
});
```

You can also pass a pre-configured `GenericContainer` when you need the full builder API:

```typescript
import { GenericContainer } from "testcontainers";

test("advanced config", async ({ useContainer }) => {
  const container = await useContainer(
    new GenericContainer("postgres:16")
      .withEnvironment({ POSTGRES_PASSWORD: "secret" })
      .withExposedPorts(5432)
      .withReuse(),
  );
});
```

### `useContainerFromDockerFile`

Builds an image from a local Dockerfile, then starts a container. Accepts the same `ContainerOpts` as `useContainer`.

```typescript
test("custom service", async ({ useContainerFromDockerFile }) => {
  const container = await useContainerFromDockerFile("./docker", "Dockerfile.test", {
    ports: 8080,
    waitStrategy: Wait.forHttp("/health", 8080),
  });
});
```

## Multiple containers, zero cleanup

Every container started inside a test is tracked automatically. After the test they all stop in parallel:

```typescript
test("full stack", async ({ useContainer }) => {
  const redis = await useContainer("redis:8", { ports: 6379 });
  const postgres = await useContainer("postgres:16", {
    ports: 5432,
    environment: { POSTGRES_PASSWORD: "secret" },
  });
  const kafka = await useContainer("confluentinc/cp-kafka:7", { ports: 9092 });

  // work with all three
}); // all three stop in parallel here
```

## Composable fixtures

Because `useContainer` is a Playwright fixture, it composes naturally with your own custom fixtures. Wrap containers in domain-specific helpers to keep tests clean:

```typescript
import { test as base } from "@playwright-labs/fixture-testcontainers";

export const test = base.extend<{ dbUrl: string; redisUrl: string }>({
  dbUrl: async ({ useContainer }, use) => {
    const pg = await useContainer("postgres:16", {
      ports: 5432,
      environment: { POSTGRES_PASSWORD: "secret" },
      waitStrategy: Wait.forLogMessage("ready to accept connections"),
    });
    await use(`postgresql://postgres:secret@${pg.getHost()}:${pg.getMappedPort(5432)}/postgres`);
  },

  redisUrl: async ({ useContainer }, use) => {
    const redis = await useContainer("redis:8", { ports: 6379 });
    await use(`redis://${redis.getHost()}:${redis.getMappedPort(6379)}`);
  },
});

// tests only see clean URLs, not container management
test("my service", async ({ dbUrl, redisUrl }) => { /* ... */ });
```

## Custom `expect` matchers

The package also ships an extended `expect` with 13 matchers for `StartedTestContainer`. Import it instead of the built-in one:

```typescript
import { test, expect } from "@playwright-labs/fixture-testcontainers";
```

### State matchers

```typescript
await expect(container).toBeContainerRunning();
await expect(container).toBeContainerStarted();
await expect(container).toBeContainerStopped();   // after stop({ remove: false })
await expect(container).toBeContainerHealthy();   // requires HEALTHCHECK in image
```

### Log matcher

```typescript
await expect(container).toMatchContainerLogMessage("ready to accept connections");
await expect(container).toMatchContainerLogMessage(/started in \d+ms/i);
await expect(container).not.toMatchContainerLogMessage("ERROR");
```

### Port matchers

```typescript
expect(container).toBeContainerPort(5432);
expect(container).toMatchContainerPortInRange(5432, { min: 1024, max: 65535 });
expect(container).toMatchContainerPortInRange(5432, { min: 1024 }); // unbounded upper
```

### Metadata matchers

```typescript
expect(container).toHaveContainerLabel("env", "test");
expect(container).toHaveContainerName("my-postgres");
expect(container).toMatchContainerName(/postgres-\d+/);
expect(container).toHaveContainerNetwork("my-bridge");
await expect(container).toHaveContainerUser("postgres");
await expect(container).toMatchContainerUser(/^postgres$/i);
```

### Locale-aware comparisons

Every string-based matcher accepts an optional `Intl.Collator` as its last argument for locale-sensitive or case-insensitive matching:

```typescript
const ci = new Intl.Collator("en", { sensitivity: "base" });

expect(container).toHaveContainerName("MY-POSTGRES", ci); // matches "my-postgres"
await expect(container).toMatchContainerLogMessage("ERROR", ci); // matches "error"
```

## `await using` support

`StartedTestContainer` implements `Symbol.asyncDispose`, which means you can use TC39 Explicit Resource Management for fine-grained scoping inside custom fixtures or `beforeAll` blocks:

```typescript
test.beforeAll(async () => {
  await using container = await new GenericContainer("postgres:16")
    .withExposedPorts(5432)
    .start();

  // seed the database
}); // container.stop() is called automatically here
```

## Requirements

| Dependency | Minimum version |
| --- | --- |
| `@playwright/test` | 1.57.0 |
| `testcontainers` | 10.0.0 |
| Docker | running locally or in CI |

## Summary

`@playwright-labs/fixture-testcontainers` brings real infrastructure into Playwright tests with the same ergonomics you already use for browsers and pages. No boilerplate, no leaked containers, no mocked behavior that diverges from production. Just real services, scoped to a test, stopped when the test ends.

Install it, replace your mocks with `useContainer`, and find the bugs that only real infrastructure can reveal.

```shell
npm install -D @playwright-labs/fixture-testcontainers testcontainers
```

### 4. Vitali Haradkou — Playwright Labs: Best Practices as Code

- Source: https://dev.to/vitalicset/introducing-playwright-labs-best-practices-as-code-198n
- Retrieved: 2026-08-29
- Exa status: complete

# What is Playwright Labs?

Playwright Labs is a curated monorepo of skills and best practices designed specifically for modern testing workflows. Think of it as a knowledge base that integrates seamlessly with your development environment, providing contextual guidance powered by AI and LLM agents.

The first package in this collection is playwright-best-practices - a comprehensive guide to writing better, faster, and more reliable Playwright tests.

## Video Overview

Watch this quick introduction to Playwright Labs and learn how to get started:

## Why Playwright Labs?

Testing best practices are often scattered across documentation, blog posts, and tribal knowledge. Playwright Labs solves this by:

- 📚 Centralized Knowledge - All best practices in one structured repository
- 🤖 AI-Optimized - Formatted specifically for LLM consumption and agent workflows
- 🎯 Impact-Driven - Practices ranked by their impact on test quality
- 🔄 Always Updated - Community-driven and version-controlled
- 💻 Code-First - Every rule includes practical code examples

## The playwright-best-practices Package

This package contains 8 categories of Playwright best practices, each with multiple rules and examples:

### 1. Test Stability & Reliability (CRITICAL)

Focus on eliminating flaky tests and ensuring consistent results.

### 2. Test Execution Speed (CRITICAL)

Optimize test performance for faster feedback loops.

### 3. Locator Best Practices (HIGH)

Master element selection strategies for robust tests.

### 4. Assertions & Waiting (HIGH)

Learn proper synchronization and validation techniques.

### 5. Parallel Execution (MEDIUM-HIGH)

Harness the power of parallel test execution.

### 6. Fixtures & Test Organization (MEDIUM)

Structure tests for maintainability and reusability.

### 7. Debugging & Maintenance (MEDIUM)

Streamline troubleshooting and test maintenance.

### 8. Advanced Patterns (LOW)

Explore advanced techniques for complex scenarios.

## Installation

The package uses a unique installation method optimized for skill packages:

```bash
# Install via pnpx
pnpx add-skill https://github.com/vitalics/playwright-labs/tree/main/packages/playwright-best-practices
```

This command integrates the best practices directly into your development environment, making them accessible to AI coding assistants and LLM agents.

## Repository Structure

```text
packages/
└── playwright-best-practices/
    ├── rules/
    │   ├── stable-use-waitfor.md          # CRITICAL: Wait for elements
    │   ├── stable-avoid-timeouts.md       # Avoid hardcoded timeouts
    │   ├── speed-parallel-tests.md        # Enable parallel execution
    │   ├── locator-use-role.md            # Prefer role-based selectors
    │   ├── assertion-use-expect.md        # Use proper assertions
    │   └── ...                            # More rules...
    ├── src/
    │   ├── build.ts                       # Compile rules to AGENTS.md
    │   ├── validate.ts                    # Rule compliance checker
    │   └── extract-tests.ts               # Test case extractor
    ├── AGENTS.md                          # Auto-generated compiled output
    ├── metadata.json                      # Package metadata
    ├── test-cases.json                    # LLM evaluation tests
    ├── package.json
    └── README.md
```

## Rule Format

Each rule follows a standardized format designed for both human and machine readability:

````markdown
---
title: "Use proper waits instead of timeouts"
impact: CRITICAL
description: "Avoid hardcoded sleeps and use Playwright's built-in waiting"
tags: [stability, waiting, async]
---

## Problem

Using hardcoded timeouts makes tests slow and unreliable.

❌ **Bad:**
```typescript
await page.goto('https://example.com')
await new Promise(resolve => setTimeout(resolve, 5000)) // Bad!
await page.click('button')
```

✅ **Good:**
```typescript
await page.goto('https://example.com')
await page.waitForLoadState('networkidle')
await page.click('button')
```

## Why

Playwright has built-in auto-waiting that makes tests faster and more reliable.
````

### Frontmatter Metadata

- title - Clear, actionable rule name
- impact - CRITICAL, HIGH, MEDIUM-HIGH, MEDIUM, or LOW
- description - Optional detailed explanation
- tags - Searchable categories

## Key Best Practices Highlights

Let me share some of the most impactful practices from the package:

### 1. Use Built-in Auto-Waiting

Playwright automatically waits for elements to be ready. Don't fight it:

```javascript
// ❌ Manual waiting
await page.waitForSelector("button");
await page.click("button");

// ✅ Auto-waiting (preferred)
await page.click("button"); // Automatically waits for button to be actionable
```

### 2. Prefer Role-Based Selectors

Use accessible locators for robust tests:

```javascript
// ❌ Fragile CSS selectors
await page.click(".btn-primary");

// ✅ Semantic role selectors
await page.getByRole("button", { name: "Submit" }).click();
```

### 3. Use Test Fixtures

Organize common setup with fixtures:

```javascript
// Define fixture
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
    await page.goto("/login");
    await page.fill('[name="username"]', "testuser");
    await page.fill('[name="password"]', "password");
    await page.click('button[type="submit"]');
    await use(page);
  },
});

// Use in tests
test("user dashboard", async ({ authenticatedPage }) => {
  await authenticatedPage.goto("/dashboard");
  await expect(authenticatedPage.getByText("Welcome")).toBeVisible();
});
```

### 4. Enable Parallel Execution

Maximize test speed with parallelization:

```javascript
export default defineConfig({
  // Run tests in parallel across 4 workers
  workers: process.env.CI ? 2 : 4,

  // Fully parallel execution mode
  fullyParallel: true,

  // Retry failed tests
  retries: process.env.CI ? 2 : 0,
});
```

### 5. Use Proper Assertions

Playwright provides auto-retrying assertions:

```javascript
// ❌ Manual checks (no retry)
const text = await page.locator(".status").textContent();
expect(text).toBe("Success");

// ✅ Auto-retrying assertions
await expect(page.locator(".status")).toHaveText("Success");
```

## Impact Levels Explained

Rules are categorized by their impact on test quality:

| Impact Level | Description | Focus Area |
| --- | --- | --- |
| CRITICAL | Major improvements to stability or speed | Must implement |
| HIGH | Significant quality improvements | High priority |
| MEDIUM-HIGH | Notable benefits with some effort | Recommended |
| MEDIUM | Incremental improvements | Nice to have |
| LOW | Advanced patterns for specific cases | Optional |

This prioritization helps teams focus on high-value practices first.

## How AI Agents Use This

The package is optimized for LLM consumption through:

1. Structured Format - Consistent markdown with clear sections
2. AGENTS.md - Compiled single-file output for easy ingestion
3. Metadata - JSON metadata for programmatic access
4. Test Cases - Evaluation data for LLM fine-tuning
5. Clear Examples - Before/after code comparisons

AI coding assistants can:

- Suggest best practices contextually
- Detect anti-patterns in your code
- Provide refactoring suggestions
- Generate test code following best practices

## Real-World Benefits

Teams using these practices report:

- 50-70% reduction in flaky tests
- 30-40% faster test execution
- Improved maintainability - easier to update tests
- Better debugging - clearer failure messages
- Increased confidence - reliable CI/CD pipelines

## What's Next for Playwright Labs?

The monorepo is designed for growth. Future packages might include:

- playwright-api-testing - Best practices for API testing
- playwright-mobile - Mobile testing patterns
- playwright-performance - Performance testing techniques
- playwright-accessibility - A11y testing guidelines
- playwright-visual - Visual regression testing

## Contributing

Playwright Labs is open-source and community-driven. You can contribute by:

1. Adding new rules - Share your hard-earned lessons
2. Improving existing rules - Better examples and explanations
3. Reporting issues - Found a problem? Let us know
4. Creating packages - New skill packages are welcome

Check out the repository: github.com/vitalics/playwright-labs

## Conclusion

Playwright Labs represents a new way of sharing and learning best practices - one that's optimized for both human developers and AI assistants. The playwright-best-practices package is just the beginning.

By installing this skill package, you're not just getting documentation - you're getting an AI-powered testing advisor that helps you write better tests from day one.

Key Takeaways:

- ✅ 8 categories covering all aspects of Playwright testing
- ✅ Impact-ranked practices for focused improvements
- ✅ AI-optimized format for intelligent tooling
- ✅ Open-source and community-driven
- ✅ Easy installation via `pnpx add-skill`

Ready to level up your Playwright testing? Install the skill package and let AI-powered best practices guide your testing journey!

```bash
pnpx add-skill https://github.com/vitalics/playwright-labs/tree/main/packages/playwright-best-practices
```

## Resources

- [Playwright Labs Repository](https://github.com/vitalics/playwright-labs)
- [Playwright Documentation](https://playwright.dev)

Happy testing! 🎭

Made with ❤️ by Vitali!

### 5. Vitali Haradkou — Email-safe Playwright report

- Source: https://vitalicset.hashnode.dev/playwright-email-react
- Retrieved: 2026-08-29
- Exa status: complete

# Email-safe shadcn/ui components for Playwright test reports

## The goal

`@playwright-labs/reporter-email` is a Playwright reporter that sends an HTML email when a test run finishes. In this release we wanted to ship templates that use shadcn/ui components — the same `Badge`, `Card`, `Button`, and `Select` that developers already know — while keeping full compatibility with email clients.

The catch: standard shadcn doesn't work in email. This post explains why and how we solved it.

## Why shadcn/ui breaks in email

There are four problems:

1. Radix UI requires browser APIs. shadcn components delegate behaviour (focus management, ARIA, keyboard nav) to Radix UI primitives. Radix calls `window`, `document`, `ResizeObserver`, and `MutationObserver` at import time. `@react-email/render` runs in Node.js — none of these exist. Result: crash on import.

2. CSS variables don't work in email clients. shadcn defaults to `hsl(var(--background))` style design tokens. Gmail, Outlook, and Apple Mail strip or ignore CSS variables. Every color becomes transparent or falls back to black.

3. Dark mode classes generate unsupported media queries. Tailwind's `dark:` prefix generates `@media (prefers-color-scheme: dark)` blocks. Most email clients strip `<style>` tags entirely, let alone media queries.

4. `<button>` elements are unreliable in email. Interactive elements in email HTML are famously inconsistent. `<a>` links have near-universal support; `<button>` does not.

## The solution

### No Radix UI

Every component is rewritten as a plain HTML element with the matching visual style. Zero Radix UI imports. Here's the full `SelectItem` implementation:

```tsx
/* @jsxImportSource react */
function SelectItem({ className, children, selected, ...props }: SelectItemProps) {
  return (
    <div
      className={cn(
        "relative flex cursor-default select-none items-center rounded-sm px-3 py-1.5 text-sm outline-none",
        selected ? "bg-slate-100 font-medium text-slate-900" : "text-slate-700",
        className,
      )}
      {...props}
    >
      {selected && (
        <span style={{ marginRight: "8px", fontSize: "10px" }}>✓</span>
      )}
      {children}
    </div>
  )
}
```

That's the entire interactive component — a `<div>` with conditional classes. Static, server-renderable, email-compatible.

### cssVariables: false in components.json

```json
{
  "tailwind": {
    "baseColor": "slate",
    "cssVariables": false
  }
}
```

This makes shadcn generate `bg-slate-900` instead of `bg-background`. Concrete class names → concrete inline CSS values after `@react-email/components`'s `<Tailwind>` wrapper processes them.

### /* @jsxImportSource react */ pragma

Playwright uses esbuild internally, which by default transforms JSX using Playwright's own runtime. This breaks `@react-email/render` because it expects React's `createElement` calls, not Playwright's internal representation.

The fix is a per-file pragma at the top of each `.tsx` component:

```tsx
/* @jsxImportSource react */
```

esbuild respects this override and uses React's JSX runtime for that file. Template files that call `React.createElement` directly are kept as `.ts` to avoid the JSX transform entirely.

### Button as `<a>`

```tsx
/* @jsxImportSource react */
function Button({ href, children, variant, ...props }: ButtonProps) {
  return (
    <a
      href={href}
      className={cn(buttonVariants({ variant }))}
      {...props}
    >
      {children}
    </a>
  )
}
```

All the shadcn `Button` variants (`default`, `destructive`, `outline`, `secondary`, `ghost`, `link`) exist — just rendered as links.

## New templates

### Chart template

```typescript
import { PlaywrightReportShadcnChartEmail }
  from "@playwright-labs/reporter-email/templates/shadcn/base-chart";
```

Adds a "Pass rate" stacked bar above the test list. The bar is three coloured `<div>` elements with `flex` widths calculated from test counts — no canvas, no SVG.

### Button template

```tsx
import { PlaywrightReportShadcnButtonEmail }
  from "@playwright-labs/reporter-email/templates/shadcn/base-button";

<PlaywrightReportShadcnButtonEmail
  result={result}
  testCases={testCases}
  reportUrl="https://ci.example.com/report/42"
  failuresUrl="https://ci.example.com/report/42/failures"
/>
```

A `CardFooter` with CTA links. "View N Failure(s)" only renders when `failed > 0`.

### Themes template

```tsx
import { PlaywrightReportShadcnThemesEmail, type ShadcnTheme }
  from "@playwright-labs/reporter-email/templates/shadcn/base-themes";

<PlaywrightReportShadcnThemesEmail result={result} testCases={testCases} theme="blue" />
```

Six themes: `slate` | `zinc` | `rose` | `blue` | `green` | `orange`. Each theme is a `ThemePalette` record with six hex values covering accent, headings, borders, and row backgrounds. Status colors (green for pass, red for fail) stay constant across themes.

## Subpath exports

```text
dist/
  templates/shadcn/index.{mjs,cjs,d.ts,d.cts}
  templates/shadcn/base.{mjs,cjs,d.ts,d.cts}
  templates/shadcn/base-chart.{mjs,cjs,d.ts,d.cts}
  templates/shadcn/base-button.{mjs,cjs,d.ts,d.cts}
  templates/shadcn/base-themes.{mjs,cjs,d.ts,d.cts}
  templates/shadcn/base-select.{mjs,cjs,d.ts,d.cts}
```

The `package.json` `exports` field maps each path with separate `import` (ESM) and `require` (CJS) conditions, each with its own `.d.ts`/`.d.cts`. Requires `"moduleResolution": "bundler"` in `tsconfig.json` to resolve correctly.

## Try it locally

```bash
git clone https://github.com/vitalics/playwright-labs
cd playwright-labs
pnpm install
pnpm --filter @playwright-labs/reporter-email build

cd packages/reporter-email/examples
pnpm email:preview
# → http://localhost:3000
```

The dev server hot-reloads on file changes. Edit the constants at the top of any preview file to change the theme, filter, or test data.

### 6. TestDino (Pratik Patel) — Playwright 1.62: isolated retries, AbortSignal and more

- Source: https://testdino.com/blog/playwright-1-62-release
- Retrieved: 2026-08-29
- Exa status: metadata-only
- Note: strona renderuje treść po stronie klienta; Exa web_fetch zwrócił wyłącznie loader („Loading blog post") w 4 próbach z odstępami ≥30 s, a indeks Exa Search przechowuje tylko metadane zapisane poniżej. Treści nie zastąpiono materiałami z innych źródeł.

# Playwright v1.62: Isolated Retries, AbortSignal & More

> Playwright version 1.62: isolated retries, AbortSignal cancellation, component testing rework, WebP screenshots & more, with code examples.

### 7. Viktor Konovalov — Validate the accessibility tree, not the DOM

- Source: https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7472539768522940418-yMME
- Retrieved: 2026-08-29
- Exa status: complete

Playwright tip: validate the accessibility tree, not the DOM (LinkedIn post, 2026-06-16)

Many UI tests are tightly coupled to implementation details: CSS classes, element IDs, and page structure. The problem is that users never interact with those things.

Instead of asking "Does this div exist?", ask "Can a user find and use this button?"

Using accessibility-based locators like getByRole() and getByLabel() gives a few advantages:

- tests focus on user behavior instead of implementation details
- selectors become more stable during UI refactoring
- accessibility issues are often discovered earlier
- test code becomes easier to read

Good tests verify what users can do, not how the page is built.

\#playwright #typescript #qa #testautomation #automationtesting #softwaretesting

![Playwright tip: validate the accessibility tree, not the DOM | Viktor Konovalov image](https://media.licdn.com/dms/image/v2/D4D22AQG9IP44YwvFnQ/feedshare-shrink_1280/B4DZ6mV3C_IYAM-/0/1780907209136?e=2147483647&v=beta&t=yOaoRZx3fAFl5HUxJ2uQ98a1suAnIA-e7S7oDdR6A8c)

Reactions: 13

### 8. Viktor Konovalov — Playwright tip: stop waiting blindly — use `expect.poll()`

- Source: https://www.linkedin.com/posts/viktorkonovalovqa_playwright-tip-stop-waiting-blindly-use-activity-7449021903387844608-Oes9
- Retrieved: 2026-08-29
- Exa status: complete

Playwright tip: stop waiting blindly - use expect.poll() (LinkedIn post, 2026-04-12)

Some waits don’t fit into standard UI assertions.

Not everything is:

- visible
- clickable
- present in DOM

Sometimes you need to wait for:

- backend job completion
- async state change
- external system update

And this is where many tests become flaky.

Why it works:

- retries automatically
- stops as soon as condition is met
- faster + more stable

When to use expect.poll():

- async backend processing
- status endpoints (pending -> done)
- queues, jobs, payments
- anything outside UI lifecycle

Risks:

- unnecessary load on the backend
- rate limiting
- unrealistic user behavior

![Playwright tip: stop waiting blindly - use expect.poll() | Viktor Konovalov image](https://media.licdn.com/dms/image/v2/D4D22AQFmcGMtrOb8Ow/feedshare-shrink_800/B4DZ2A98pCKsAc-/0/1775985216424?e=2147483647&v=beta&t=O0jgPhV8CGmwsczGYByWMkBrxHY-_q6v-tQ4U-P00PM)

Reactions: 93

### 9. Viktor Konovalov — Playwright tip: use `expect.toPass()` when a single assertion retry is not enough

- Source: https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7468564794011246592-LFyP
- Retrieved: 2026-08-29
- Exa status: unavailable
- Error: Exa web_fetch nie pobrał strony — 4 próby w odstępach ≥30 s zakończyły się błędem `CRAWL_NOT_FOUND` (post nie istnieje w indeksie crawl Exa). Treść nie została zastąpiona domysłami ani materiałami z innych źródeł.

### 10. Stefan Minchev — Stop writing while loops to wait for a database update

- Source: https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7460346882448392192-yn6y
- Retrieved: 2026-08-29
- Exa status: complete

Stop writing while loops to wait for a database update. You’re over-engineering your own stress. (LinkedIn post, 2026-05-13)

We've all been there: you trigger an action like placing an order, but the backend needs a few seconds to process it.

The old way usually looks like a messy while loop with `page.waitForTimeout(1000)` inside. It's brittle, hard to read, and leads to flaky tests whenever the environment runs slow.

The Playwright way is `expect.poll`. It's a built-in "smart waiter" that retries your logic until a condition is met, with configurable intervals and a clear timeout.

Here's how it works: create the order, then pass an async callback to `expect.poll` that fetches the order list and returns the status of your specific order. Add a backoff strategy via `intervals`, a `timeout`, and a clear `message` for failures. Chain `.toBe('COMPLETED')` at the end. That's it.

No custom retry logic. No guessing how long to sleep. `expect.poll` handles the polling, the intervals, and the assertions in a single readable block.

🚩 Anti-pattern to avoid: the "sleep and hope" method calls `page.waitForTimeout(5000)`, then checks the status in a separate assertion.

This either waits too long or not long enough. Every single time.

How are you handling eventual consistency in your tests today?

🧰 Save this to your QA toolkit
🔔 Follow me for more Playwright tips
🚀 Reshare so another engineer ships faster

\#QA #TypeScript #AI #Playwright #SoftwareTesting

![Stop writing while loops to wait for a database update. You’re over-engineering your own stress. | Stefan Minchev image](https://media.licdn.com/dms/image/v2/D4D22AQHYJlkO8r9BPQ/feedshare-image-high-res/B4DZ4houYJHgAU-/0/1778680784682?e=2147483647&v=beta&t=ePWhitxddD804KTJ_LHjjgO8H7Lr1Ur9fR5X0W83vfM)

Reactions: 19

### 11. Currents.dev — Playwright testing in Staging vs Production

- Source: https://currents.dev/posts/playwright-testing-staging-vs-production
- Retrieved: 2026-08-29
- Exa status: complete

# Playwright testing in Staging vs Production

Currents Team • May 15, 2026

A decision framework for splitting your Playwright tests between staging and production — what belongs where, how to configure each, and when production testing isn't worth it.

Most people treat the staging vs. production question as a trust problem. Either you trust staging enough to rely on it, or you don't. If you don't, you start looking for ways to test in production instead. But that framing misses the real issue.

The real issue is that staging and production are environments with fundamentally different risk profiles, and most Playwright suites are configured as if they aren't.

This article covers what that misconfiguration looks like in practice, why it happens, and how to fix it with a concrete approach for what your Playwright configuration, test scope, and execution setup should look like in each environment.

If you already know what belongs where and want the configuration details, skip to Playwright Configuration That Should Differ by Environment or the Quick Reference table.

### The two failure modes that bring you here usually look like this:

The first is over-trusting staging. Zalando's engineering team lived this one publicly. They'd invested heavily in Cypress E2E tests, reaching 95% reliability across 120 daily deploys. Then a production incident slipped through anyway: incomplete content from their headless CMS broke the React hydration contract on product detail pages, preventing users from adding items to carts. The regression was data-driven, not code-driven, so their staging tests never surfaced it. The tests were green, but they were testing a fiction.

The second is under-trusting staging. You've seen enough staging-passes-production-fails cycles that you've lost confidence in staging results entirely. You want production coverage, but you're not sure how to add it without triggering real-world side effects (emails sent, payments charged, data corrupted) that make running tests in production feel risky by default.

There's a third variant that's arguably worse: skipping staging entirely. Grafana Labs' February 2025 postmortem describes a TLS policy change that was tested in dev and assumed to be low-risk. The change was pushed to development, staging, and production simultaneously. It inadvertently destroyed load balancers across 25% of their services, causing a 150-minute partial outage. The team's own analysis: they "failed to fully test, and failed to reduce the blast radius of the change." When staging exists but gets treated as a rubber stamp, you get the worst of both worlds.

All three failure modes share a root cause: undifferentiated configuration. The same playwright.config.ts, the same test suite, the same execution setup, applied to two environments that have categorically different consequences for failure.

Playwright's flexibility makes this worse before it makes it better. It's easy to write tests that implicitly depend on staging-specific behavior: hardcoded record IDs, generous timeouts calibrated to slow staging infrastructure, auth fixtures that bypass the MFA flows production enforces. You don't realize those dependencies exist until something breaks. This article is about making them explicit and deliberate, not accidental.

The goal is aligning your configuration to each environment's purpose, and building the cultural foundations that make that alignment stick. For more on the cultural side, see How to build reliable Playwright tests: a cultural approach.

With that framing in place, the first question to answer is the most fundamental one: which tests should run where?

## What Belongs in Staging and What Belongs in Production

The core split comes down to risk and reversibility. Staging is where you run anything that mutates state, triggers side effects, or depends on controlled data conditions, because the consequences are contained. Production should default to read-path flows that validate the live system is working for real users. Writes are not automatically off the table in production, but they require explicit isolation: dedicated accounts, scoped data partitions, and a reliable cleanup strategy. Without those controls in place, the default answer is staging.

### When to Test in Staging

Staging is the environment that absorbs risk. The obvious cases are clear: a checkout flow that fires a Stripe charge, a test that creates and deletes user accounts, a workflow that sends real emails. Those belong in staging because the consequences of failure are contained.

The more interesting cases are the ones that look safe but aren't:

- "Read-only" tests that create ephemeral state. A test that loads a dashboard doesn't write to your database, but it may create a session record, fire analytics events, or generate audit log entries. In staging, this is noise. In production, those records pollute real analytics pipelines and inflate session counts unless your test accounts are explicitly excluded.
- Cache-warming flows. A test that navigates through a product catalog "just reading" may evict real user cache entries or warm CDN edges with test traffic patterns that skew cache hit ratios. If your CDN or application cache doesn't distinguish test traffic, this is a mutation in disguise.
- Write-path verification via dry-run endpoints. If your application exposes shadow or dry-run modes (e.g., a payment API that validates a charge without executing it), you can verify write-path correctness in production without side effects. But the dry-run endpoint itself must be production-hardened and explicitly designed for this. A staging-only endpoint that gets deployed to production by accident is a liability.
- Feature flag variant coverage. Testing the same user flow across multiple flag states is expensive and stateful. It belongs in staging, where flag configuration is controlled and data doesn't carry real consequences.

### When to Test in Production

Production tests need to meet a high bar: read-path dominant, non-destructive, high-signal, and low-volume. The goal is validation, not exploration. Good candidates include:

- Homepage and critical landing page availability
- Authentication flow validation using dedicated service accounts
- Critical navigation flows (can a logged-in user reach their dashboard, their settings, their primary workflow?)
- Basic add-to-cart without checkout completion: confirming the cart system is functional without triggering an order or payment

The common thread: these tests confirm the live system is operational for real users. They do not try to cover every edge case. That coverage already exists in staging.

That list covers the obvious candidates. The harder question: what categories of failure can only be caught in production? These justify the operational cost.

Third-party integration behavior under real conditions. Your staging Stripe integration runs in test mode. Your production Stripe integration enforces rate limits, applies fraud scoring, and occasionally returns different error codes than the test mode sandbox documents. The same applies to SSO providers, CDN edge configurations, and payment gateways. A staging test that exercises your Okta login flow hits a sandbox tenant. In production, your Okta tenant enforces an MFA policy that your sandbox doesn't replicate, and the session token has a different TTL. You only find out when users can't log in after a deploy.

Infrastructure routing and geo-specific behavior. DNS resolution, CDN cache behavior, load balancer routing, and geo-IP decisions all differ between staging and production. If your application serves different content or redirects based on geography, staging can't reproduce that unless you've built an unusually sophisticated environment. Most teams haven't. One postmortem from unixy.io describes a deployment where tests passed against clean staging data, but production had 3% corrupted legacy records from a bug that was fixed months earlier. The new feature crashed on the first corrupted record it encountered.

Data-driven regressions. This is the Zalando pattern. The code is correct. The data is not. Production data has entropy that staging seed data can never reproduce: half-migrated records, deprecated fields that were never cleaned up, user-generated content with unexpected encoding. When your application's rendering depends on the shape of the data (CMS content, dynamic configurations, user-submitted templates), staging tests can pass indefinitely while production breaks.

The staging parity ceiling. Even well-maintained staging environments have structural fidelity limits. Staging shares infrastructure, runs on smaller instances, connects to different DNS, has no real traffic pressure, and often runs with relaxed security policies. Charity Majors at Honeycomb argues that every deployment is already a test in production, and that the answer is to invest in making production testing safe rather than pretending staging is a faithful replica. That's a strong philosophical position, and it's largely correct. The gap is in the execution: making production testing deliberate and configured rather than accidental.

### The cost of production testing

None of this means production testing is free. Teams that start with three smoke tests tend to scope-creep into running their full regression suite against production within a year. The overhead adds up:

- Dedicated test accounts need provisioning, credential rotation, and explicit exclusion from analytics, billing, and support tooling. That's cross-team coordination with product, finance, and data engineering.
- WAF and bot detection allowlisting requires your security or platform team to maintain rules that exempt test traffic without creating a bypass that attackers can exploit.
- PII compliance for test artifacts means traces, screenshots, and video captured in production can contain real user data. Your retention and access policies need to account for this.
- Alert routing for production test failures requires integration with your incident management system. Someone has to own these alerts, and someone has to triage them at 2 AM.

If your organization can't absorb these costs, production testing will create more problems than it solves. We cover when to skip it entirely in "When production testing is not worth it".

Even in production, there are different types of tests, and when you treat them as the same, you end up configuring them incorrectly.

### The Deployment vs. Monitoring Distinction

There are two distinct production use cases that often get conflated, and treating them identically leads to the wrong configuration for both.

Post-deploy smoke tests run immediately after a deployment. Their job is narrow: confirm that the critical paths survive the deployment. They should fail fast, run at low parallelism, and the failure of any single test should trigger investigation or rollback. Speed matters, but completeness matters more. If your smoke suite passes but something critical is broken, the suite has failed its purpose.

Synthetic monitoring runs continuously on a schedule, every few minutes, regardless of deployment cadence. Its job is to catch production degradation that isn't caused by a deployment: upstream dependency failures, database slowdowns, certificate expirations, and infrastructure issues. These tests need to be extremely stable and deterministic because every failure routes to an alerting system. A flaky synthetic monitor trains your on-call team to ignore alerts, which is worse than having no monitor at all.

A single test suite can serve both use cases, but only with deliberate tagging and separate project configurations in `playwright.config.ts`. The tests themselves can be shared; the execution context and operational requirements differ.

Who owns what matters. Post-deploy smoke tests are usually owned by the QE or development team because they're part of the deployment pipeline. Failures block releases, and the people who wrote the code are the ones who need to investigate. Synthetic monitoring typically belongs to SRE or platform engineering because failures route to on-call and require operational response, not code investigation. When the same team owns both, alert fatigue from noisy smoke tests bleeds into monitoring response times. When ownership is split clearly, each team can set appropriate severity and escalation policies for their failure type.

The maturity path is sequential, not parallel. Most teams start with post-deploy smoke tests because the operational bar is lower: you run them in CI, failures block a deploy, and the feedback loop is immediate. Synthetic monitoring is a separate capability that requires stable tests (near-zero false positives), integration with your alerting stack, on-call runbooks for test-specific failures, and organizational buy-in that a test failure at 3 AM is worth waking someone up for. If your smoke tests have a flakiness rate above 2-3%, you're not ready for synthetic monitoring. Fix the stability first.

Shopify takes this further by running deliberate "game day" exercises where they trigger failure modes in production systems to practice incident response. That's a level beyond smoke tests and synthetic monitoring, but it illustrates the progression: you earn the right to do more aggressive production testing by proving you can handle the simpler version reliably.

One more thing worth naming: Playwright is not always the right tool for every production monitor. You might be better off using lightweight HTTP probes or uptime checks for availability monitoring and reserving Playwright for the narrow set of flows that require a real browser: auth, checkout entry points, and client-side rendering validation. If a check doesn't need a browser, it probably doesn't need Playwright. That distinction keeps your synthetic monitoring suite lean and reduces the surface area for flakiness.

With those two use cases separated, the decision logic for any individual test becomes more tractable.

### Decision Framework

When deciding where a test belongs, ask:

1. Does this test mutate shared production data?
2. Does it trigger external side effects (email, payments, webhooks)?
3. Would a false positive cause an operational incident?
4. Is the signal high enough to justify production execution?
5. Does staging faithfully simulate this scenario?

The decision usually falls out naturally:

| Test characteristic | Environment |
| --- | --- |
| Mutates state or data | Staging |
| Read-only and high-signal | Production |
| Expensive, destructive, or exploratory | Staging |
| Validates real infrastructure behavior | Production |
| Depends on controlled data conditions | Staging |
| Non-destructive, deterministic, critical path | Production |
| Creates ephemeral state (sessions, audit logs) | Production, if test accounts are excluded from analytics |
| Write-path validation via dry-run endpoints | Production, if the endpoint is production-hardened |
| Cache-warming or CDN-dependent flows | Staging, unless test traffic is isolated at the CDN level |

Knowing where a test belongs is the starting point. Every configuration difference that follows maps back to three things: risk tolerance (can this environment absorb a destructive test?), blast radius (does a failure affect an engineer or a real user?), and signal fidelity (is a failure trustworthy or noise?). If a configuration difference doesn't trace back to one of those, it's accidental drift.

### When production testing is not worth it

The decision framework above helps you decide where individual tests belong. But there's a higher-level question worth answering first: should your team be running production tests at all?

Production testing is not a maturity badge. It's an operational commitment. For some teams, the answer is "not yet" or "not at all," and that's the correct answer.

Your application is write-path dominant. If your core user flows are form submissions, data entry, transactions, and workflows that modify state, the set of production-safe read-only smoke tests covers a thin slice of your actual risk surface. You'll invest significant effort in test account isolation, data cleanup, and side-effect prevention for coverage that doesn't meaningfully reduce your exposure. Your testing effort is better spent on staging fidelity.

You don't have platform support to maintain it. Production testing requires cross-team coordination: WAF allowlisting with security, test account exclusions with data engineering, alert routing with SRE, PII compliance with legal. On a team of five engineers shipping a SaaS product, that coordination overhead doesn't exist as a separate function. It falls on the same people writing the tests. If maintaining the production test infrastructure takes more time than the bugs it catches, the math doesn't work.

You're in a strict compliance environment. Under HIPAA, PCI-DSS, or SOX, test traffic that touches production data creates audit exposure. Test accounts that can read real patient records, financial transactions, or regulated data, even read-only, may need to be documented, access-logged, and periodically reviewed. If your compliance team hasn't signed off on the isolation model, don't start running tests.

Staging is the actual problem. This is the most common trap. Teams lose confidence in staging because it's stale, under-resourced, or poorly maintained. Instead of fixing the root cause, they route around it by testing in production. That works until the production test suite grows, the operational overhead compounds, and you still have a broken staging environment that can't catch regressions before they ship. Production testing should complement good staging, not replace it. If your staging passes-but-production-fails cycle is driven by stale deploys, bad seed data, or relaxed auth config, fix those problems directly. The "Staging as a First-Class Engineering Concern" section covers how.

If none of these apply and you have the organizational support to maintain it, production testing is worth the investment. The rest of this article assumes you've made that decision deliberately.

## The Real Differences Between Staging and Production

Configuration should follow from environment reality, not convention. Here's what actually differs between staging and production from a Playwright test design perspective.

### Data State

Staging runs against seeded, synthetic, or anonymized data. Production has real user data (real transaction history, real edge cases) that staging datasets never anticipate, and real entropy. A user who changed their email three times, an order stuck in an intermediate state, and a product variant that was deprecated but not cleaned up.

Tests that rely on specific data existing (a particular user ID, a product SKU, a specific order state) are fragile in staging and potentially destructive in production if they try to create or modify that data. The implication is clear: production tests must be either purely read-path or use isolated, dedicated test accounts and data fixtures explicitly provisioned for testing.

```typescript
// ❌ Anti-pattern: hardcoded record ID only exists in seeded staging data
await page.goto("/users/12345/dashboard");
await expect(page.locator('[data-testid="username"]')).toHaveText(
  "test-user@example.com",
);

// ✅ Production-safe: use Playwright's built-in request fixture ({ request } in test params)
// to resolve the current user, then navigate using the returned ID
const response = await request.get("/api/me");
const { id: userId } = await response.json();
await page.goto(`/users/${userId}/dashboard`);
await expect(page.locator('[data-testid="username"]')).toBeVisible();
```

### Infrastructure and Integrations

Staging typically uses sandboxed or stubbed third-party integrations. Stripe is in test mode. Emails route to a fake SMTP sink. The identity provider accepts a test credential that bypasses MFA. This is correct behavior. It prevents side effects from touching real systems.

Production uses the real integrations. Real rate limits. Real SLAs. Real billing. A test that fires 50 parallel requests to a payment API in staging (hitting a mock) will trigger rate limiting, generate costs, or cause an incident in production. Infrastructure divergence is not just a data problem; it is an operational risk that compounds under parallelism.

### Performance Characteristics

CDN caching, real database query performance, background job processing, queue latency, all of these differ between environments. Staging infrastructure is often shared, under-resourced, or running on spot instances. Production is optimized for real traffic.

This has a direct implication for timeout configuration. Timeouts calibrated to staging infrastructure will produce false negatives in production under real load, and vice versa. There is no single timeout value that works correctly in both environments.

### Feature Flags and Rollout State

A test written against a feature that's fully enabled in staging may be exercising a code path that's behind a percentage rollout in production, available to only a subset of users. Your test account may or may not be in that cohort. Tests that depend on specific feature flag states need to either control those states explicitly or be scoped to the environments where the state is predictable. Shopify's approach to this is to use beta flags with explicit targeting, so new features can be activated for specific accounts in production without exposing untested code paths to real users. The pattern applies directly to Playwright test accounts.

In practice, controlling flag state for production tests means explicitly targeting your dedicated test service accounts in your feature flag system. If your test account is in a percentage rollout cohort, the test may exercise different code paths across runs.

You'll see a test that passes 70% of the time in CI with no code changes between runs. The trace shows different UI elements rendering on different attempts. That's not flakiness. That's your test account landing on different sides of a rollout.

The fix is to pin test accounts to a specific flag variant. Most feature flag platforms support this directly. In LaunchDarkly, you'd add a targeting rule that matches your test account's email or user ID and serves a fixed variation. In a generic setup, you can use a Playwright fixture that calls your flag service's API before the suite runs:

```typescript
// fixtures/flags.ts
test.beforeAll(async ({ request }) => {
  if (process.env.TARGET_ENV === "production") {
    await request.post("/api/internal/feature-flags/override", {
      data: {
        userId: process.env.TEST_ACCOUNT_ID,
        flags: { "new-checkout-flow": true },
      },
    });
  }
});
```

This also affects test tagging. If a feature is behind a partial rollout in production, tests that exercise that feature should not be tagged `@smoke` or `@monitor` until the rollout is at 100% or your test account is pinned. A smoke test that only works when the flag evaluates to `true` is a flaky smoke test in disguise.

In staging, use flag overrides or a test-specific flag configuration to ensure deterministic coverage of each variant.

### Auth and Session Behavior

Session expiry, token rotation policies, SSO enforcement, and MFA requirements often differ between environments. `storageState` files generated in staging are not valid in production and vice versa. This is a configuration issue. The auth fixture layer must be environment-aware from the ground up.

Session bleed between tests is not a default Playwright behavior. By default, Playwright isolates browser context per test. It happens when you intentionally reuse auth state, share worker fixtures, or rely on persisted sessions across test runs. That isolation model is one of Playwright's strongest defaults. Problems arise when configuration deliberately bypasses it, which is exactly what shared `storageState` does. That tradeoff is sometimes correct, but it should be a conscious decision, not an accidental one.

### Rate Limiting and Bot Detection

Production environments commonly have WAF rules, rate limiters, and bot detection that staging either lacks or has configured permissively. Playwright's default Chromium fingerprint and request cadence patterns are recognizable to services like Cloudflare, Akamai, and PerimeterX. Aggressive parallelism in production, the same settings that work fine in staging, can trigger bot mitigation, block your test runners, or flag your test service accounts for fraud review.

That's a long list of things that differ, which makes it worth being equally explicit about what shouldn't.

## What Should Stay the Same Across Environments

Equally important as knowing what to change is knowing what should be immutable. These are the parts of your Playwright setup that should be environment-agnostic by design.

### Test Logic

The assertion logic, user flow steps, and expected outcomes of a test should not change based on the environment. If a test behaves differently in staging versus production because the test logic itself differs, that's a signal that the test is testing the environment, not the application. Environment-specific behavior belongs in configuration and fixtures, never in test bodies.

One exception to the 'same test logic' principle: network mocking via page.route(). Tests that mock external APIs in staging should not carry those mocks into production, where the goal is to validate real integrations. If your staging tests use page.route() to stub third-party responses, gate those mocks behind an environment check in your fixture layer, or split them into staging-only fixtures that production projects don't import. A production test that silently mocks the payment API defeats the purpose of running it in production.

Keeping test logic clean depends on having a fixture and helper layer that does the environment-specific heavy lifting below it.

### The Fixture and Helper Layer

Auth fixtures, API helpers, and test data factories should abstract environment-specific details, like base URLs, credentials, and API endpoints, so test bodies remain clean and portable. A test that works in staging and fails in production should fail because of an application difference, not because it hardcodes a staging URL in the middle of a flow.

Abstraction handles the inputs to your tests. Observability is about what you capture when they run, and that shouldn't degrade when you cross into production.

### Reporting and Observability

The same observability standards should apply across environments, but the artifact capture policy may differ. Visibility shouldn't degrade in production. If anything, it should improve. Production test failures are operational events. They deserve better observability than staging failures, not worse.

Visibility into failures is only useful if your retry policy isn't quietly swallowing them first.

### Retry and Flake Policy

Retry logic should be consistent and intentional, but "consistent" means the principle stays the same across environments, not necessarily the retry count. The principle is this: retries should reflect your explicit tolerance for transient failures, not compensate for a poorly configured suite. In staging, `retries: 2` is reasonable. The infrastructure is less stable and some noise is acceptable. In production, the right count depends on the use case. Post-deploy smoke tests can tolerate `retries: 1` because a single transient failure during a deployment window is plausible.

Synthetic monitoring should use `retries: 0`. A monitor that retries before alerting delays incident detection and trains on-call engineers to discount failures. Some teams argue for `retries: 1` to absorb transient DNS blips or load balancer hiccups. That's a reasonable position, but the tradeoff is real: every retry adds latency to incident detection, and a retry that passes silently hides a signal that your infrastructure had a moment of instability.

If transient failures are frequent enough to justify retries, that's an infrastructure problem worth fixing, not a test configuration to work around. What should never happen in either production context is adding retries to absorb known instability. That's masking signal. We go deeper on diagnosing the root causes of flakiness, including timeout misconfigurations that often drive the impulse to add retries, in debugging Playwright timeouts.

### Test Tagging and Categorization

Tags like `@smoke`, `@critical`, and `@read-only` should be applied in the test source, not added ad hoc for specific environments. Environment-specific test selection should be driven by tag filtering at the runner level, not by duplicating test files per environment. Separate files mean separate maintenance burdens and inevitable divergence.

With the stable foundation established, here's where deliberate divergence begins.

## Playwright Configuration That Should Differ by Environment

Here is the practical, technical core. Each dimension needs deliberate, environment-specific configuration.

### baseURL and Environment Resolution

Resolve `baseURL` from environment variables in `playwright.config.ts` and validate at config load time that required variables are set. Fail fast before any test runs, not midway through a suite.

```typescript
export default defineConfig({
  projects: [
    {
      name: "production-smoke",
      grep: /@smoke/,
      use: { baseURL: requireEnv("PROD_URL") },
    },
    {
      name: "staging-full",
      grepInvert: /@skip-staging/,
      use: { baseURL: requireEnv("STAGING_URL") },
    },
  ],
});
```

The multi-project configuration lets you run the same test suite simultaneously against staging and production. This is useful for validating that staging results actually predict production behavior. Tracking your test suite health across environments covers how to make that cross-environment comparison concrete and actionable.

Routing requests to the right environment is the prerequisite. Giving those requests the right amount of time to complete is where most configurations quietly go wrong.

### Timeouts

`navigationTimeout`, `actionTimeout`, and assertion timeout should be tuned per environment. Production under real CDN behavior and database query variability will have different p95 page load times than a staging environment running on shared infrastructure. The anti-pattern is a single global timeout that's either too tight for production or too loose to give fast feedback in staging.

NOTE: Playwright's default `actionTimeout` is 0 (no limit), and the default test `timeout` is 30 seconds. All values below are explicit overrides. They must be calibrated based on your production application's actual performance metrics (e.g., p95) to avoid flakiness.

```typescript
// staging.config.ts
export default defineConfig({
  timeout: 30_000,
  expect: { timeout: 8_000 },
  use: {
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
  },
});

// production.config.ts
export default defineConfig({
  timeout: 15_000,
  expect: { timeout: 5_000 },
  use: {
    actionTimeout: 5_000,
    navigationTimeout: 10_000,
  },
});
```

Tighter production timeouts serve a dual purpose: they give you a faster signal on real failures, and they surface genuine performance regressions that looser timeouts would silently absorb.

A caveat: production experiences real-world variability that staging doesn't. During peak traffic, CDN cache misses, or third-party latency spikes, tighter timeouts may produce false failures. The values above assume your production smoke tests run during low-traffic windows or against infrastructure that's isolated from user traffic. If your tests run continuously against shared production infrastructure, calibrate timeouts to your application's p95 response times with a reasonable margin, not to an aspirational target.

How to derive timeout values: Run your smoke suite with `trace: 'on'` for a week and collect action timings from the Playwright trace viewer. Alternatively, pull p95 navigation and API response times from your APM tool (Datadog, New Relic, Grafana). Set each timeout to roughly your p95 + 50% margin. For example, if your production dashboard page loads at p95 in 3.2 seconds, a `navigationTimeout` of 5,000ms gives you headroom without masking real regressions. Re-check these baselines quarterly or after major infrastructure changes.

### Trace, Screenshot, and Video Configuration

In staging, maximize debuggability. `trace: 'on-first-retry'`, screenshots on failure, video for complex flows, you want maximum information when a test fails because fixing it quickly matters.

In production, apply conservative defaults for a reason that goes beyond performance: traces and videos of production sessions can capture real user data and PII. If your production smoke tests run against shared sessions, or if your service accounts have access to user data, a retained trace is a potential privacy incident.

```typescript
// staging.config.ts
use: {
  trace: 'on-first-retry',
  video: 'on-first-retry',
  screenshot: 'only-on-failure',
}

// production.config.ts
use: {
  trace: 'retain-on-failure',  // Keep for diagnosis, but only on failure
  video: 'off',                // Never capture video in production
  screenshot: 'off',           // Screenshots may capture sensitive data
}
```

Video capture in production should be avoided unless tests are run against fully isolated, data-empty test accounts with no exposure to real user records.

For synthetic monitoring against accounts that access real user data, keep screenshots and video off. For post-deploy smoke tests running against fully isolated, dedicated test accounts with no access to real user records, `screenshot: 'only-on-failure'` provides valuable diagnostic context without PII exposure. The decision depends on the isolation level of your test accounts, not on the environment itself.

### Parallelism and Worker Count

Staging can typically absorb aggressive parallelism. Production cannot. Parallel test workers hitting production generate synthetic load, can trigger rate limiters, and may affect real user sessions in ways that are difficult to attribute or roll back.

```typescript
// staging.config.ts
export default defineConfig({
  fullyParallel: true,
  workers: undefined, // Use default (half of logical CPU cores)
  retries: 2,
});

// production.config.ts
export default defineConfig({
  fullyParallel: false,
  workers: 2, // Hard cap: 2 concurrent workers
  retries: 0, // Zero retries: failures are operational signals
});
```

Production retry policy depends on the use case. For continuous synthetic monitoring, use `retries: 0`. Every failure is an operational signal and should trigger an alert. For post-deploy smoke tests, `retries: 1` is a pragmatic choice: it absorbs the transient instability that's normal immediately after a deployment without masking persistent failures.

The key distinction is that a synthetic monitor failure means 'production is degraded right now,' while a post-deploy smoke failure means 'this deployment may have broken something.' The appropriate retry tolerance follows from that difference.

### Auth Setup and storageState

`storageState` files are environment-specific and should be treated as such. The `globalSetup` that generates them must use the correct credentials and base URL for the target environment. Do not share `storageState` files across environments.

```typescript
// fixtures/auth.ts
import { test as base } from "@playwright/test";

// TEST_ACCOUNTS and loginAs are project-specific.
// TEST_ACCOUNTS: array of { username, password } objects, one per worker.
// loginAs: helper that performs login via the UI or API and stores session state.

// For read-only production smoke tests: shared pre-baked auth state
export const sharedAuth = base.extend({
  page: async ({ browser }, use) => {
    const storageStatePath = process.env.PROD_AUTH_STATE_PATH;
    if (!storageStatePath) {
      throw new Error(
        "The PROD_AUTH_STATE_PATH environment variable must be set.",
      );
    }
    const ctx = await browser.newContext({
      storageState: storageStatePath,
    });
    await use(await ctx.newPage());
    await ctx.close();
  },
});

// For state-modifying staging tests: worker-scoped isolation
export const isolatedAuth = base.extend({
  page: async ({ browser }, use, testInfo) => {
    const workerIndex = testInfo.workerIndex;
    if (workerIndex >= TEST_ACCOUNTS.length) {
      throw new Error(
        `Not enough test accounts for parallel workers. Worker ${workerIndex} needs an account, but only ${TEST_ACCOUNTS.length} are defined.`,
      );
    }
    const account = TEST_ACCOUNTS[workerIndex];
    const ctx = await browser.newContext();
    await loginAs(ctx, account);
    await use(await ctx.newPage());
    await ctx.close();
  },
});
```

Note: Using workerIndex for account isolation is a simple approach, but it can be fragile. A more robust pattern for larger suites is to use an atomic leasing mechanism or a dedicated API to check out and release unique test accounts per test.

Shared `storageState` is acceptable for production smoke tests only when the flows are stable and read-dominant. It introduces a single point of failure. If the stored session expires or the account state changes, every test that depends on it fails together, and it creates hidden coupling that's easy to miss until something breaks at 2 AM. For any flow that modifies server-side state, even in staging, use worker-scoped isolated accounts instead.

In production, auth setup must use dedicated test service accounts, never shared credentials, never accounts tied to real users. This is a hard requirement, not a best practice. Real user sessions contaminated by test execution generate fraudulent activity data, corrupt user-specific state, and create compliance exposure if that session data ends up in traces or test artifacts.

Watch for `storageState` expiration. A `globalSetup` that logs in once can silently go stale if the session expires before the last test in a long suite finishes. The safest approach: re-login in `globalSetup` on every CI run, not just when the file is missing. Add a lightweight health check (a `GET /api/me` that returns 401 on expired sessions) at the start of `globalSetup` to detect stale auth before the suite runs. For credential rotation, store service account passwords in your secrets manager (Vault, AWS Secrets Manager, 1Password) and reference them via environment variables so rotation never requires a code change.

Getting auth right ensures your tests can execute safely. Getting test selection right ensures the right tests are executed at all.

### Test Selection via Tags and Projects

Use `--grep` or Playwright project filtering to run only safe, read-path, non-destructive tests in production. Define what "safe for production" means explicitly and enforce it at the project configuration level:

```typescript
// A helper function should be used to ensure required environment variables are present.
const requireEnv = (name: string): string => {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
};

// playwright.config.ts
// Assuming `productionConfig`, `monitorConfig`, and `stagingConfig` are
// imported objects containing shared configuration for each environment.
projects: [
  {
    name: "production-smoke",
    grep: /@smoke/, // Only @smoke-tagged tests
    use: {
      baseURL: requireEnv("PROD_URL"),
      ...productionConfig,
    },
  },
  {
    name: "production-monitor",
    grep: /@monitor/, // Synthetic monitoring subset
    use: {
      baseURL: requireEnv("PROD_URL"),
      ...monitorConfig,
    },
  },
  {
    name: "staging-full",
    grepInvert: /@skip-staging/,
    use: {
      baseURL: requireEnv("STAGING_URL"), // ✅ Points to safe environment
      ...stagingConfig, // ✅ Uses staging-specific settings
    },
  },
];
```

"Safe for production" means: no data mutation, no third-party API calls that generate side effects (emails, charges, notifications), no tests that depend on specific data state that may not exist in production, no tests that require write permissions on shared resources.

## Running Playwright in Production Safely

Configuration gets you the right settings. This section is about the execution practices that make production testing something you can trust long-term.

### The Non-Destructive Test Contract

Every test that runs in production should be auditable against a non-destructive checklist:

- Does it write to the database?
- Does it send an email, SMS, or push notification?
- Does it trigger a payment or financial transaction?
- Does it fire a webhook or external API call with side effects?
- Does it modify session state for a real user account?

If the answer to any of these is yes, the test should not run unguarded in production. The most reliable way to enforce this is structurally, at the fixture and helper layer, rather than through naming conventions or documentation that developers need to remember to follow. A production auth fixture that uses a read-scoped service account makes mutation physically impossible, not just policy-prohibited.

The contract defines what tests are allowed to do. Account and data isolation define the boundaries within which they're allowed to do it.

### Dedicated Test Accounts and Data Isolation

Production test accounts should be provisioned via infrastructure tooling (Terraform, Pulumi, a provisioning script in your IaC repo) not created manually. Manual accounts get forgotten, reused inappropriately, or cleaned up by a support engineer who doesn't know they're test accounts.

Production test accounts need to be:

- Identifiable: a consistent naming convention or account attribute that makes them recognizable in logs, support tooling, and billing reports
- Rotatable: credentials that can be updated via secrets management without a code change
- Minimally scoped: permissions limited to exactly what the tests require, nothing more
- Excluded: from analytics, from user counts, from revenue calculations

If tests generate any data in production, even read-path tests sometimes create ephemeral sessions or audit log entries, there needs to be a cleanup strategy: either immediate teardown in fixture cleanup, or a scheduled job that identifies and purges records associated with test accounts.

Isolated accounts and clean data boundaries are prerequisites for both production use cases, but those two use cases still require different execution models.

### Synthetic Monitoring vs. Deployment Validation

These two use cases have different requirements that are worth being explicit about.

Post-deploy smoke tests prioritize completeness and speed. They should cover every critical path in the application and finish quickly enough to be part of your deployment pipeline. A failure should block promotion or trigger an automatic rollback.

Synthetic monitoring prioritizes reliability and alerting integration. Every failure generates an operational alert. That means the tests in a synthetic monitoring suite must have near-zero false positive rates. A single flaky test in your monitoring suite destroys confidence in the entire alert channel.

Zalando's solution to the CMS incident mentioned earlier is a good model here. They built Playwright-based "test probes" running on a 30-minute cron, covering only three critical customer journeys (home page to product, catalog with filters to product, product to cart to checkout).

Before enabling paging, they ran in "shadow mode" for weeks, fixing selectors and improving resilience until false positives stopped. Since going live, they've been paged exactly once, and it was a genuine incident. That's the bar for synthetic monitoring: if your on-call team starts ignoring alerts, you've already failed.

Structure your suite with separate tags for each use case, then use Playwright project configuration to run them with different cadences, parallelism settings, and alert routing. The tests themselves can overlap; the execution context should not.

### Alerting and Incident Integration

Production test failures are operational events. They should route to the same alerting infrastructure as application errors (PagerDuty, OpsGenie, Slack incident channels), not to a test results dashboard that an engineer might check once a day.

This requires more than just Playwright. You need a layer that translates test failure events into incident alerts with the right routing, severity, and context. Currents can serve as that intermediary: centralized result tracking across environments, failure history that distinguishes a first-time failure from a recurring pattern, and webhook integrations that connect test failures directly to your incident workflow.

When a synthetic monitor fails, the alert should arrive with enough context: which test, which step, which environment, how many consecutive failures, so that the on-call engineer doesn't need to log into a dashboard to understand the scope.

### Playwright and WAF/Bot Detection

If your production environment uses Cloudflare, Akamai, PerimeterX, or similar WAF and bot detection services, you need to coordinate with your security or platform team before running Playwright tests in production. This is not a Playwright configuration problem you can solve alone.

Practical mitigations:

- Dedicated IP ranges for test runner infrastructure, whitelisted at the WAF level
- User-agent identification: set a recognizable user-agent string in your Playwright config so WAF rules can allow-list it
- Request header whitelisting: a custom header that identifies synthetic traffic, excluded from bot scoring
- Low parallelism: the single most effective mitigation, since bot detection is primarily triggered by request cadence patterns.

```typescript
// production.config.ts
use: {
  userAgent: 'PlaywrightSmokeTest/1.0 (+https://yourcompany.com/synthetic-monitoring)',
  extraHTTPHeaders: {
    'X-Synthetic-Test': 'true',  // Coordinate with your WAF team to allowlist this header
  },
},
```

These require platform team coordination. Build that into your rollout plan for production testing, not as an afterthought when your test runner starts hitting CAPTCHA challenges.

## Staging as a First-Class Engineering Concern

If your staging environment is unreliable, the answer isn't to abandon it for production testing. The answer is to fix staging while selectively layering in production coverage. A weak staging environment and strong production monitoring are not substitutes for each other. They're both necessary, and staging problems that aren't fixed will eventually surface as production incidents.

The specific things that make staging results untrustworthy, and how to address them:

Stale deployments. Staging falls behind production when deployment to staging is manual, infrequent, or lower priority than shipping. The fix is treating staging deployment as part of the same pipeline as production, not a separate, optional step. Infrastructure-as-code and automated promotion gates keep environments in sync.

```typescript
// Detect stale staging deployments before running the suite
test.beforeAll(async ({ request }) => {
  const response = await request.get("/api/version");
  const { version } = await response.json();
  const expected = process.env.EXPECTED_VERSION;
  if (expected && version !== expected) {
    throw new Error(
      `Staging is running ${version} but expected ${expected}. Deployment may be stale.`,
    );
  }
});
```

Synthetic data that doesn't reflect production edge cases. Staging datasets are created once and age poorly. Production data evolves in ways that expose new failure modes. The mitigation is periodic anonymized production data snapshots into staging, combined with data factories that generate realistic edge-case records programmatically rather than relying on static seed files.

For PostgreSQL, tools like postgresql_anonymizer can mask PII columns in-place during the snapshot. For other databases, a custom ETL pipeline that hashes emails, randomizes names, and nullifies payment details is straightforward to build. Run these snapshots on a weekly cadence and automate the import into staging so the data stays fresh without manual intervention.

Relaxed auth and security configuration. When staging bypasses MFA, uses permissive session TTLs, and disables WAF rules, tests pass in conditions that don't exist in production. Staging security configuration should mirror production as closely as possible, even if it creates friction during development. The friction is the point. It surfaces auth-related failures before they reach production.

Using Playwright results diagnostically. If a test passes consistently in staging but fails consistently in production, it's a parity signal. The test has identified a specific dimension where staging doesn't reflect production behavior. Currents makes this cross-environment comparison concrete: you can see whether staging failures predict production failures, identify which tests have environment-specific failure patterns, and trace the divergence back to a specific configuration or data difference rather than dismissing it as noise.

Staging and production, treated as complementary rather than competing concerns, are what make the approach below sustainable rather than just theoretical.

## Quick Reference: Configuration Summary

The table below is a condensed configuration reference for all three execution contexts. It's intentionally split by production use case. Smoke and monitoring have different retry posture and should not share a single configuration profile.

| Setting | Staging | Production — Smoke | Production — Monitoring |
| --- | --- | --- | --- |
| fullyParallel | true | false | false |
| workers | Auto (half CPU cores) | 2 | 1 |
| retries | 2 | 1 | 0 |
| timeout | 30_000 ms | 15_000 ms | 15_000 ms |
| actionTimeout | 10_000 ms | 5_000 ms | 5_000 ms |
| navigationTimeout | 20_000 ms | 10_000 ms | 10_000 ms |
| expect.timeout | 8_000 ms | 5_000 ms | 5_000 ms |
| trace | on-first-retry | retain-on-failure | retain-on-failure |
| video | on-first-retry | off | off |
| screenshot | only-on-failure | off | off |
| storageState | Worker-scoped test accounts | Shared, read-dominant service account | Shared, read-dominant service account |
| grep | All tests (except @skip-staging) | @smoke | @monitor |

## Final Words

The question isn't "staging or production?" It's "what is each environment for, and does your Playwright configuration match that purpose?"

Staging catches regressions before they reach users. Production validates the real system works for real users. Those two purposes are complementary, not competing. If you have a clearly defined role for each environment, a test suite tagged and configured to match, and an observability layer that makes cross-environment comparison continuous, you're in good shape.

### 12. ScrollTest / Pramod Dutta — Playwright Actions and Auto-Waiting: Day 3

- Source: https://scrolltest.com/playwright-actions-auto-waiting-day-3/
- Retrieved: 2026-08-29
- Exa status: complete

# Playwright Actions and Auto-Waiting: Day 3

## What You Build on Day 3

Playwright actions and auto-waiting are the difference between a test that behaves like a real user and a test that passes only on your laptop. In Day 3 of this Playwright + TypeScript series, I want you to stop writing sleep-based scripts and start trusting Playwright's action model.

Day 1 covered setup. Day 2 covered locators and assertions. Today we connect those two ideas with real browser actions: click, fill, check, select, upload, hover, keyboard input, and navigation after a user event.

The goal is simple: by the end, you can write a stable checkout-style test without adding `waitForTimeout(3000)` after every step. You also know when Playwright waits automatically, when it does not wait, and what to do when the page has async behavior that is not tied to a normal navigation.

If you missed the earlier lessons, keep these open in a second tab: Day 1 Playwright TypeScript setup and Day 2 Playwright locators and assertions. This tutorial builds directly on both.

### Prerequisites

- Node.js installed and a Playwright TypeScript project created.
- Basic comfort with `test`, `expect`, and `page`.
- A working test site or local app with forms, buttons, dropdowns, and links.
- VS Code with the Playwright extension if you want faster debugging.

### Source notes for this lesson

I use the official Playwright documentation as the base source here. The Playwright auto-waiting docs state that Playwright performs actionability checks before actions and fails with a timeout if the checks do not pass. The Playwright input docs cover actions like click, fill, check, select, and file uploads. The Playwright navigation docs explain how Playwright waits for navigations caused by actions. I also checked current ecosystem signals: the Microsoft Playwright GitHub repository shows 90,697 stars, and npm reports 158,464,929 downloads for `@playwright/test` in the last month from 2026-05-04 to 2026-06-02. Those numbers explain why this is now a core SDET skill, not a side tool.

## Playwright Actions and Auto-Waiting Explained

When a manual tester clicks a button, they wait naturally. They see the spinner, they see the button become enabled, they see the next page appear, and then they continue. Old Selenium-style automation often missed that human timing. The script clicked too early, typed into a hidden element, or asserted before the UI finished rendering.

Playwright actions and auto-waiting solve a large part of that problem. Before Playwright performs many user actions, it checks whether the target element is ready for that action. That sounds small, but it removes a lot of test noise.

### What auto-waiting actually means

Auto-waiting does not mean Playwright magically understands your business flow. It means Playwright waits for technical readiness before performing an action.

For a normal click, Playwright checks that the locator resolves to one element, the element is visible, stable, receives events, and is enabled. If those checks pass, the click happens. If they do not pass within the timeout, Playwright throws a clear error instead of silently doing the wrong thing.

That is why this test usually does not need a manual sleep:

```typescript
import { test, expect } from '@playwright/test';

test('opens account page from dashboard', async ({ page }) => {
  await page.goto('/dashboard');

  await page.getByRole('link', { name: 'Account' }).click();

  await expect(page).toHaveURL(/.*account/);
  await expect(page.getByRole('heading', { name: 'Account settings' })).toBeVisible();
});
```

The click waits for the link to be actionable. The assertion waits for the URL and heading. No fixed delay is needed.

### What auto-waiting does not mean

Auto-waiting does not wait for every API call, every animation, or every state transition in your application. If a page updates a label after a background request, Playwright may not know the business meaning of that change. You still need a web-first assertion that describes the state you expect.

```typescript
await page.getByRole('button', { name: 'Apply coupon' }).click();
await expect(page.getByText('Coupon applied')).toBeVisible();
await expect(page.getByTestId('order-total')).toContainText('₹899');
```

Notice the pattern. The action triggers behavior. The assertion waits for the result. This is the cleanest mental model for beginners and experienced SDETs.

## The Actionability Checks Behind Every Click

The official Playwright docs call this actionability. I call it the reason your test does not panic-click a disabled button. Most flaky tests I review fail because the author treats the page like static HTML. Modern pages are not static. They render, hydrate, animate, fetch, disable, enable, and re-render.

### The five checks to remember

You do not need to memorize the entire matrix, but you should know the checks that matter most for daily work:

1. Unique target: the locator should resolve to exactly one element for actions that require one target.
2. Visible: the element must be visible, not hidden behind CSS or conditional rendering.
3. Stable: the element should not be moving because of animation or layout shift.
4. Receives events: another element should not cover it.
5. Enabled: disabled buttons and inputs should not receive normal user actions.

This is one reason I pushed locator discipline hard on Day 2. A weak locator gives Playwright a bad target. Auto-waiting cannot fix a bad target.

### How actionability changes your code style

New automation engineers often write code like this:

```typescript
await page.locator('#save').waitFor({ state: 'visible' });
await page.waitForTimeout(2000);
await page.locator('#save').click();
```

That code has two problems. The sleep is blind, and the selector is brittle. A better version describes the user action and expected result:

```typescript
await page.getByRole('button', { name: 'Save changes' }).click();
await expect(page.getByText('Profile updated')).toBeVisible();
```

Use explicit waits only when you are waiting for something outside the normal action/assertion model. For example, waiting for a download event, a websocket message exposed in the UI, or a custom network response can be valid. Waiting because you are unsure is not valid.

### When force is a smell

Playwright gives you `force: true` for actions like clicks. I use it rarely. If a click needs force, the test may be bypassing a real user constraint. Maybe a loader is covering the button. Maybe the button is disabled. Maybe your locator points to an icon inside the button instead of the button itself.

```typescript
// Avoid this unless you have a clear reason.
await page.getByRole('button', { name: 'Pay now' }).click({ force: true });
```

Before using force, inspect the trace. A forced click can hide product bugs, especially in payment, checkout, and admin screens where disabled states matter.

## Core Playwright Actions You Use Daily

Most web tests use a small group of actions. If you learn these well, you can automate 70-80% of normal business flows. The rest is file uploads, downloads, dialogs, network control, and advanced browser APIs, which we handle later in the series.

### Clicking buttons, links, and menu items

Use role-based locators when the element has a real user-facing role and name. This keeps your test close to how users and assistive technology understand the page.

```typescript
await page.getByRole('button', { name: 'Login' }).click();
await page.getByRole('link', { name: 'Orders' }).click();
await page.getByRole('menuitem', { name: 'Delete' }).click();
```

For icons, ask the app team to add an accessible name. Do not settle for `.icon-trash:nth-child(3)` unless you enjoy maintaining broken tests every sprint.

### Filling forms

For text fields, use `fill`. It clears the field and types the new value. Use `pressSequentially` only when you specifically need character-by-character typing behavior, such as testing autocomplete or keyboard events.

```typescript
await page.getByLabel('Email').fill('qa.engineer@example.com');
await page.getByLabel('Password').fill('CorrectHorseBatteryStaple!');
await page.getByRole('button', { name: 'Sign in' }).click();

await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
```

For India-focused product teams, test local formats early. Phone numbers, GSTIN, PIN codes, and rupee currency fields often contain formatting rules that break basic automation scripts.

```typescript
await page.getByLabel('Mobile number').fill('9876543210');
await page.getByLabel('PIN code').fill('560103');
await expect(page.getByTestId('shipping-charge')).toContainText('₹');
```

### Checkboxes, radio buttons, and dropdowns

Use semantic actions for form controls. They read better and fail better.

```typescript
await page.getByLabel('I agree to the Terms').check();
await expect(page.getByLabel('I agree to the Terms')).toBeChecked();

await page.getByLabel('Payment method').selectOption('upi');
await expect(page.getByLabel('Payment method')).toHaveValue('upi');
```

For custom dropdowns built with divs, use roles if the component exposes them correctly:

```typescript
await page.getByRole('combobox', { name: 'City' }).click();
await page.getByRole('option', { name: 'Bengaluru' }).click();
```

If this fails, do not blame Playwright first. Inspect the accessibility tree. Many custom components are visually rich but semantically poor.

### Hover, keyboard, and file upload

Hover is useful for menus and tooltips, but do not overuse it for flows that users can access through visible buttons.

```typescript
await page.getByRole('button', { name: 'Profile' }).hover();
await page.getByRole('menuitem', { name: 'Logout' }).click();
```

Keyboard actions help with shortcuts, search boxes, and accessibility checks:

```typescript
await page.getByRole('searchbox', { name: 'Search products' }).fill('playwright');
await page.keyboard.press('Enter');
await expect(page.getByRole('heading', { name: /search results/i })).toBeVisible();
```

File uploads should target the input element, not the fancy upload card:

```typescript
await page.getByLabel('Upload resume').setInputFiles('fixtures/resume.pdf');
await expect(page.getByText('resume.pdf')).toBeVisible();
```

## Navigation After Actions Without Flaky Sleeps

Navigation is where many Playwright beginners get confused. They click a link, immediately assert something, and the test sometimes fails. The fix is not a sleep. The fix is to assert the navigation outcome or wait for the right event.

### Simple navigation

For normal links and buttons that navigate, this is enough:

```typescript
await page.getByRole('link', { name: 'Pricing' }).click();
await expect(page).toHaveURL(/.*pricing/);
await expect(page.getByRole('heading', { name: 'Pricing' })).toBeVisible();
```

The assertion waits until the URL matches. The heading assertion waits until the new page renders. This is better than waiting for a generic load state because it checks what the user actually cares about.

### When to use waitForURL

I use `waitForURL` when the next page matters but I do not need a full assertion block immediately.

```typescript
await page.getByRole('button', { name: 'Continue' }).click();
await page.waitForURL('**/checkout/payment');

await expect(page.getByRole('heading', { name: 'Payment' })).toBeVisible();
```

Keep the pattern tight: action, URL wait, visible state assertion. Do not wait for URL and then skip the UI check.

### When to use Promise.all

Older examples often used `Promise.all` to start waiting for navigation before clicking. In many modern Playwright flows, web-first assertions are enough. Still, for explicit events like downloads or popups, the pattern is useful because the event may fire immediately after the action.

```typescript
const downloadPromise = page.waitForEvent('download');
await page.getByRole('button', { name: 'Download invoice' }).click();
const download = await downloadPromise;

expect(download.suggestedFilename()).toContain('invoice');
```

The important idea: start waiting before the click if the event can happen instantly.

## Hands-On Checkout Flow Test

Now let us build a realistic flow. This is not tied to one demo website. Treat it as a template for your app. Replace labels and routes with your own product names.

### Test case

We will automate this scenario:

1. Open the product page.
2. Add a product to the cart.
3. Open cart and verify item count.
4. Fill shipping details.
5. Select UPI as payment method.
6. Place a test order.
7. Assert the confirmation state.

### Code example

```typescript
import { test, expect } from '@playwright/test';

test.describe('checkout actions', () => {
  test('places a test order with UPI payment option', async ({ page }) => {
    await page.goto('/products/playwright-course');

    await expect(page.getByRole('heading', { name: /playwright course/i })).toBeVisible();
    await page.getByRole('button', { name: 'Add to cart' }).click();
    await expect(page.getByTestId('cart-count')).toHaveText('1');

    await page.getByRole('link', { name: 'View cart' }).click();
    await expect(page).toHaveURL(/.*cart/);
    await expect(page.getByText('Playwright Course')).toBeVisible();

    await page.getByRole('button', { name: 'Checkout' }).click();
    await expect(page).toHaveURL(/.*checkout/);

    await page.getByLabel('Full name').fill('Pramod Dutta');
    await page.getByLabel('Email').fill('pramod@example.com');
    await page.getByLabel('Mobile number').fill('9876543210');
    await page.getByLabel('Address').fill('HSR Layout, Bengaluru');
    await page.getByLabel('PIN code').fill('560102');

    await page.getByLabel('Payment method').selectOption('upi');
    await expect(page.getByTestId('payment-summary')).toContainText('UPI');

    await page.getByLabel('I agree to the Terms').check();
    await page.getByRole('button', { name: 'Place test order' }).click();

    await expect(page).toHaveURL(/.*order-confirmation/);
    await expect(page.getByRole('heading', { name: 'Order confirmed' })).toBeVisible();
    await expect(page.getByTestId('order-id')).toContainText('TEST-');
  });
});
```

This test has no fixed wait. Every wait is tied to a user-visible state: URL, heading, count, text, selected payment method, or order id. That is the standard I want you to follow in real projects.

### Screenshot descriptions to capture

For your learning notes, capture these screenshots when you run the test:

- Screenshot 1: VS Code showing the checkout test with the action/assertion rhythm highlighted.
- Screenshot 2: Playwright Inspector paused on the `Add to cart` click, showing the selected role locator.
- Screenshot 3: HTML report after the test passes, with the checkout test duration and browser project visible.
- Screenshot 4: Trace Viewer action timeline showing click, fill, select, check, and final assertion steps.

## Debugging Actions in Trace Viewer

When an action fails, do not guess. Open the trace. This is one of the biggest productivity upgrades Playwright gives to QA teams.

### Run with trace

In development, run a single spec with trace enabled:

```bash
npx playwright test tests/checkout.spec.ts --trace on
```

Then open the report:

```bash
npx playwright show-report
```

Click the trace attachment. Look at the action timeline, before/after DOM snapshots, console logs, network calls, and the exact locator Playwright used.

### What to inspect first

- Was the locator unique? If multiple elements matched, fix the locator.
- Was the element visible? If hidden, check conditional rendering or route state.
- Was another element covering it? Look for loaders, sticky headers, cookie banners, or modals.
- Was it disabled? Check whether the test skipped a required form field.
- Did the app navigate? If yes, assert the final URL and page heading.

This debugging habit connects to a broader cost problem. I wrote about wasted engineering time in Cost of Flaky Tests. A flaky test is not just one failed pipeline. It burns review time, rerun time, and trust in automation.

### Use trial clicks for investigation

Playwright supports trial mode on some actions. A trial click performs actionability checks without actually clicking. I use it when debugging a stubborn element.

```typescript
await page.getByRole('button', { name: 'Pay now' }).click({ trial: true });
```

If the trial fails, the real click would fail too. The error usually tells you which actionability check did not pass.

## Common Pitfalls I See in QA Teams

Playwright actions and auto-waiting can make your suite stable, but only if you stop fighting the framework. These are the mistakes I see most often when manual testers and Selenium engineers move into Playwright.

### Pitfall 1: Adding waits after every action

This is the most common habit. The test fails once, someone adds three seconds, and the suite becomes slower forever.

```typescript
// Bad
await page.getByRole('button', { name: 'Save' }).click();
await page.waitForTimeout(3000);

// Better
await page.getByRole('button', { name: 'Save' }).click();
await expect(page.getByText('Saved successfully')).toBeVisible();
```

If your team has 500 tests and each test has two unnecessary three-second sleeps, that is 50 minutes of wasted wait time in a serial run. Parallel execution reduces wall-clock time, but it does not remove the waste.

### Pitfall 2: Clicking implementation details

Users click buttons, links, menu items, checkboxes, and labels. They do not click CSS classes. If your tests depend on utility classes or component internals, small UI refactors will break them.

```typescript
// Bad
await page.locator('.btn.btn-primary.mt-4').click();

// Better
await page.getByRole('button', { name: 'Create account' }).click();
```

This also improves accessibility feedback. If the role locator cannot find the button by name, your product may have an accessibility problem worth fixing.

### Pitfall 3: Ignoring async UI states

Some actions trigger background updates without navigation. Add an assertion that waits for the user-visible result.

```typescript
await page.getByRole('button', { name: 'Refresh balance' }).click();
await expect(page.getByTestId('balance-status')).toHaveText('Updated');
await expect(page.getByTestId('wallet-balance')).toContainText('₹');
```

Do not assert too early. Do not sleep blindly. Assert the state that proves the action worked.

### Pitfall 4: Using force to hide product bugs

If a button is covered by a loader, a user cannot click it. If your test uses force, it may pass while the real user experience is broken. This matters in teams that ship to production several times per day through CI/CD.

If you are building more advanced self-healing systems, read Self-Healing Selectors in 2026. The same warning applies there: healing should not hide product defects.

## Key Takeaways

Playwright actions and auto-waiting help you write tests that match real user behavior, but they are not a license to stop thinking. Good Playwright code has a clear rhythm: locate the user-facing element, perform the action, assert the visible result.

- Playwright waits for actionability before common actions like click and fill.
- Auto-waiting checks technical readiness, not your business rule.
- Use web-first assertions to wait for the result of an action.
- Avoid fixed sleeps unless you are proving a time-based product behavior.
- Use Trace Viewer when actions fail; do not debug by guessing.
- Keep locators user-facing with roles, labels, text, and test IDs.

For tomorrow's lesson, we will move into test hooks, fixtures, and reusable setup. That is where Playwright starts feeling like a real framework instead of a collection of scripts.

## FAQ

### Does Playwright auto-wait for every click?

Playwright performs actionability checks for normal click actions. It waits for the element to be ready within the configured timeout. If the element never becomes actionable, the test fails with an error.

### Should I ever use waitForTimeout?

Rarely. Use it only when you are testing actual time-based behavior or doing temporary debugging. For product flows, prefer locator actions, URL assertions, event waits, and web-first assertions.

### What is the best way to wait after clicking a submit button?

Assert the result. If submit navigates, assert the URL and heading. If it updates the same page, assert a success message, table row, toast, or field value that proves the submit worked.

### Why does my click fail even when I can see the button?

The button may be covered by another element, disabled, moving because of animation, or matched by a bad locator. Open Trace Viewer and inspect the actionability error.

### Is force click acceptable in automation?

Use `force: true` only when you understand the tradeoff. In most application tests, force click hides real user constraints and can mask bugs.

### 13. Butch Mayhew — Playwright-cli Boosts Token Efficiency for Coding Agents

- Source: https://www.linkedin.com/posts/butchmayhew_playwright-cli-just-changed-how-i-work-with-activity-7426626393586884608-BYI4
- Retrieved: 2026-08-29
- Exa status: complete

Playwright-cli Boosts Token Efficiency for Coding Agents (LinkedIn post, 2026-02-09, Butch Mayhew — AI QA Trainer @ DevClarity)

playwright-cli just changed how I work with coding agents. If you're using Claude Code, GitHub Copilot, or any AI coding assistant - this matters. The Playwright team released a cli browser designed specifically for agentic workflows. The big win? Token efficiency. Look at this comparison from the same browser automation task:

- MCP approach: 114.5K tokens
- CLI approach: 26.8K tokens

That's 4x fewer tokens. Same result. Why it works:

- No bloated tool schemas loaded upfront
- Accessibility trees load progressively (only what you need)
- Coding agents already love working in the terminal

I've been experimenting with this in my automation workflows and the difference is noticeable - faster responses, more room in the context window for actual work. Instructions to install are highlighted in the screenshot below.

What's your experience been with browser automation in agentic workflows? Curious what approaches are working for you.

Reactions: 179, Comments: 17

### 14. ScrollTest / Pramod Dutta — Playwright TypeScript Checklist

- Source: https://scrolltest.com/playwright-typescript-checklist/
- Retrieved: 2026-08-29
- Exa status: complete

# Playwright TypeScript Checklist: Day 22 Bonus

By Promode, July 13, 2026

Day 22 bonus: This Playwright TypeScript checklist is the final production-readiness pass I want every SDET to run before calling a framework complete. The earlier 21 days gave you locators, fixtures, API tests, reports, CI, Docker, visual checks, data strategy, and framework architecture. This article turns those parts into a release checklist you can use before your suite becomes the quality gate for a real product.

I am keeping this practical. You will get a checklist, TypeScript examples, screenshot descriptions, and the mistakes I see when teams move from “tests run on my laptop” to “tests block a release.”

## Why a Playwright TypeScript checklist matters

Playwright has become a serious choice for modern web automation. The public GitHub repository shows more than 92,000 stars, and the npm API reported more than 181 million downloads for `@playwright/test` in the last month when I checked it for this article. Those numbers do not make your framework good by default, but they tell us one thing clearly: many teams are now betting their UI and API checks on this tool.

That shift creates a new problem. A beginner can create a test with `npx playwright codegen` in five minutes, but a release-ready framework needs rules. It needs stable selectors, clean fixtures, useful traces, deterministic test data, readable reports, and a CI setup that fails for the right reason.

I see three types of Playwright suites in companies:

- Demo suites that prove Playwright can click buttons.
- Team suites that run for one squad but depend on tribal knowledge.
- Release suites that a manager trusts before deployment.

This Playwright TypeScript checklist is designed to move you from the second bucket to the third. If you are working in a service company like TCS, Infosys, Wipro, or Cognizant, this checklist helps you speak in terms of maintainability and risk. If you are in a product company, it helps you reduce regression time without creating a flaky gate that developers start ignoring.

For official behavior, always cross-check the Playwright documentation. For release changes, track the Playwright GitHub releases. I am adding those links because production automation should not depend only on blog posts, including mine.

## Baseline setup: versions, config, and scripts

Your checklist starts before the first test file. A messy setup creates slow failures later. I want the project to be boring, explicit, and easy for a new SDET to run on day one.

### 1. Pin Playwright and browser versions

Do not let every engineer run a different version. Playwright ships browser binaries with the framework, so version drift can change behavior. Check the installed version in CI and local machines.

```bash
npm ls @playwright/test
npx playwright --version
npx playwright install --with-deps
```

In `package.json`, keep scripts small and named by intent:

```json
{
  "scripts": {
    "test:e2e": "playwright test",
    "test:e2e:headed": "playwright test --headed",
    "test:e2e:debug": "PWDEBUG=1 playwright test",
    "test:e2e:report": "playwright show-report",
    "test:e2e:smoke": "playwright test --grep @smoke"
  },
  "devDependencies": {
    "@playwright/test": "^1.61.1",
    "typescript": "^5.5.0"
  }
}
```

The exact versions will change. The rule stays the same: document the version, update deliberately, and run a smoke pack after every upgrade. Playwright's latest release at the time of this cron run was v1.61.1 on GitHub, so I would create an upgrade branch before moving a team framework to it.

### 2. Keep `playwright.config.ts` readable

A config file should tell the next engineer how the framework behaves. If your config has 200 lines of hidden conditionals, your suite will become hard to debug.

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 4 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'test-results/junit.xml' }]
  ],
  use: {
    baseURL: process.env.BASE_URL ?? 'https://example.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ]
});
```

This config follows the official ideas documented in Playwright test configuration. The key is not copying my values blindly. The key is knowing why each value exists.

### 3. Add a local readiness command

Before CI, create one command that checks TypeScript, linting, and a small smoke suite. This prevents weak commits from reaching the pipeline.

```bash
npm run typecheck
npm run lint
npm run test:e2e:smoke
```

Screenshot description: capture your terminal after this command passes. The screenshot should show the Playwright summary line, browser project name, duration, and zero failed tests. Add it to your team wiki as the expected local baseline.

## Selector and assertion checklist

Bad selectors are the fastest way to make a good tool look unreliable. Playwright gives you better locator APIs than older frameworks, but it does not stop you from writing brittle selectors.

### Prefer user-facing locators first

The official locator guide recommends role, label, text, placeholder, alt text, title, and test id locators depending on the situation. My default order is simple:

1. Use `getByRole` when the element has a clear accessible role and name.
2. Use `getByLabel` for form inputs.
3. Use `getByTestId` for dynamic components where business text changes often.
4. Use CSS only when you are testing layout or there is no better stable contract.

```ts
import { test, expect } from '@playwright/test';

test('user can create a project', async ({ page }) => {
  await page.goto('/projects');

  await page.getByRole('button', { name: 'New Project' }).click();
  await page.getByLabel('Project name').fill('Checkout Regression Pack');
  await page.getByRole('button', { name: 'Create' }).click();

  await expect(page.getByRole('heading', { name: 'Checkout Regression Pack' })).toBeVisible();
});
```

Notice the test reads like a user journey. This is easier to review than `div:nth-child(3) > button`. It also pushes developers to keep accessible names meaningful, which is a good side effect.

### Assertions should wait for business outcomes

Do not assert random implementation details. Assert the result the user or system cares about. Playwright's web-first assertions automatically retry until the timeout, so use them instead of manual sleep calls.

```ts
await expect(page.getByText('Payment successful')).toBeVisible();
await expect(page.getByRole('button', { name: 'Download invoice' })).toBeEnabled();
await expect(page).toHaveURL(/\/orders\/\d+$/);
```

Checklist for selectors and assertions:

- No `waitForTimeout` in committed tests unless there is a documented reason.
- No long CSS chains for product flows.
- Every critical action has a visible or API-level assertion after it.
- Test IDs follow a naming convention, for example `data-testid="checkout-submit"`.
- Assertions check business state, not random DOM noise.

If you want a focused guide for dynamic inputs, read the ScrollTest tutorial on testing autocomplete and typeahead inputs in Playwright. For select controls, the multi-select dropdown guide gives a useful companion example.

## Fixture, data, and authentication checklist

Fixtures decide whether your framework scales. Without fixtures, every test repeats setup code. With bad fixtures, every test hides too much and becomes hard to understand.

### Create fixtures for behavior, not convenience

A fixture should represent a reusable testing capability: authenticated user, seeded project, API client, temporary inbox, or test data factory. It should not become a dumping ground for every helper function.

```ts
import { test as base, expect, APIRequestContext } from '@playwright/test';

type Project = { id: string; name: string };

type TestFixtures = {
  project: Project;
  api: APIRequestContext;
};

export const test = base.extend<TestFixtures>({
  api: async ({ request }, use) => {
    await use(request);
  },

  project: async ({ request }, use) => {
    const name = `e2e-project-${Date.now()}`;
    const response = await request.post('/api/projects', { data: { name } });
    expect(response.ok()).toBeTruthy();

    const project = await response.json();
    await use(project);

    await request.delete(`/api/projects/${project.id}`);
  }
});

export { expect };
```

This pattern uses Playwright's fixture model, described in the official fixture documentation. The setup and cleanup are in one place, and the test receives a ready-to-use object.

### Keep authentication out of every test

If every test logs in through the UI, your suite is slower and more fragile. Use a saved storage state for normal authenticated flows. Keep one or two UI login tests to validate the login screen itself.

```ts
// auth.setup.ts
import { test as setup, expect } from '@playwright/test';

setup('authenticate as standard user', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill(process.env.E2E_EMAIL!);
  await page.getByLabel('Password').fill(process.env.E2E_PASSWORD!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('navigation')).toBeVisible();
  await page.context().storageState({ path: 'playwright/.auth/user.json' });
});
```

Then attach the state in a project:

```ts
projects: [
  { name: 'setup', testMatch: /.*\.setup\.ts/ },
  {
    name: 'chromium-authenticated',
    use: { ...devices['Desktop Chrome'], storageState: 'playwright/.auth/user.json' },
    dependencies: ['setup']
  }
]
```

Do not commit real auth files. Add them to `.gitignore`. The official Playwright authentication guide covers the storage state pattern in detail.

### Test data needs ownership

In Indian QA teams, I often see one shared QA environment with one shared user and one shared test record. That works for demos. It fails badly when five engineers, one nightly job, and one release candidate touch the same data.

Use this data checklist:

- Every created record has a unique prefix, for example `e2e-`.
- Every test owns its data or uses read-only reference data.
- Cleanup is automatic, but the test still passes if cleanup fails after the assertion.
- Secrets come from CI variables, not from committed files.
- Data factories stay typed, so a refactor fails during TypeScript checks.

For a broader strategy, I would pair this article with ScrollTest's test data management guide for SDETs.

## Debugging, traces, and screenshot evidence

A release-ready suite must fail loudly and usefully. A bad failure says “Timeout 30000ms exceeded.” A good failure says which user journey broke, what the page looked like, what network call failed, and what changed between the first attempt and the retry.

### Turn on traces where they help

Playwright trace viewer is one of the biggest reasons I prefer it for team frameworks. The official trace viewer documentation explains how traces capture actions, snapshots, console logs, and network details. In CI, I usually set traces to `on-first-retry`. This keeps normal runs light while preserving evidence for flaky failures.

```ts
use: {
  trace: 'on-first-retry',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure'
}
```

Screenshot description: open a failed trace in Trace Viewer and capture the action timeline. The screenshot should show the failing click, the DOM snapshot on the right, and the network tab with the failed API request highlighted. This is the kind of evidence developers actually use.

### Add failure notes with `test.step`

Long tests are hard to debug when every action appears as a flat list. Use `test.step` to label meaningful stages.

```ts
test('checkout flow creates a paid order', async ({ page }) => {
  await test.step('add product to cart', async () => {
    await page.goto('/products/sku-123');
    await page.getByRole('button', { name: 'Add to cart' }).click();
    await expect(page.getByText('Added to cart')).toBeVisible();
  });

  await test.step('pay with test card', async () => {
    await page.getByRole('link', { name: 'Checkout' }).click();
    await page.getByLabel('Card number').fill('4242424242424242');
    await page.getByRole('button', { name: 'Pay now' }).click();
  });

  await test.step('verify order confirmation', async () => {
    await expect(page.getByText('Payment successful')).toBeVisible();
  });
});
```

This small habit improves reports, traces, and code review. It also makes your tests easier to explain during incident calls.

### Use a triage label system

When a CI job fails, classify the failure before fixing it. I use five buckets:

1. Product bug: application behavior changed or broke.
2. Test bug: the test made a wrong assumption.
3. Environment issue: deployment, data, or dependency failed.
4. Framework issue: fixture, config, auth, or helper failed.
5. Unknown: needs trace review before assignment.

This is where SDETs add real value. Do not just paste a failed screenshot in Slack. Add the bucket, suspected cause, trace link, affected build, and whether the failure blocks release.

## CI checklist for reliable release gates

CI is where Playwright frameworks become real. A test that only runs from one engineer's laptop is not a release signal. The CI version must be repeatable, fast enough, and strict about reporting.

### Use a clear GitHub Actions workflow

Here is a minimal workflow I would accept for a small team. Larger teams can add sharding, Docker images, and environment promotion later.

```yaml
name: Playwright E2E

on:
  pull_request:
  workflow_dispatch:

jobs:
  e2e:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run test:e2e
        env:
          BASE_URL: ${{ secrets.E2E_BASE_URL }}
          E2E_EMAIL: ${{ secrets.E2E_EMAIL }}
          E2E_PASSWORD: ${{ secrets.E2E_PASSWORD }}
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: |
            playwright-report
            test-results
          retention-days: 7
```

The important parts are `npm ci`, browser installation, secrets from CI, and artifact upload on every run. Without artifacts, failed UI tests become guesswork.

### Set pass criteria before adding more tests

Teams often ask for 500 automated tests. I would rather have 50 reliable tests that block a release correctly. Define these rules:

- Smoke pack must finish in under 10 minutes.
- Critical pack must upload trace, screenshot, and HTML report.
- Retries are allowed in CI, but every retry failure is reviewed weekly.
- Tests tagged `@wip` cannot run in release gates.
- Flaky tests are quarantined with an owner and removal date.

For advanced CI patterns, read the earlier ScrollTest article on building an AI-augmented Playwright test suite. It pairs well with this checklist when you start adding generated scenarios or AI-assisted failure summaries.

### Sharding is not a replacement for cleanup

Sharding helps speed, but it also exposes weak data design. If tests collide when they run in parallel, do not blame CI. Fix the data ownership first, then split the workload.

```bash
npx playwright test --shard=1/4
npx playwright test --shard=2/4
npx playwright test --shard=3/4
npx playwright test --shard=4/4
```

The Playwright CI guide at playwright.dev/docs/ci is the source I keep open when I review pipeline setup. It is short, direct, and updated with framework changes.

## Code review checklist for SDETs

Code review is where automation quality becomes team culture. If reviewers only check whether a test passes locally, the framework will decay.

### Review the user journey first

Ask one question before reading helper details: does this test protect a real risk? If the answer is no, the test should not enter the suite.

Use this review checklist:

1. Does the test name describe business behavior?
2. Does the setup create or reference clear test data?
3. Are locators user-facing or intentionally test-id based?
4. Are assertions tied to user-visible or API-visible outcomes?
5. Will this test run in parallel without colliding with another test?
6. Does the failure produce useful evidence?
7. Is the test tagged correctly for smoke, regression, or release?

### Reject hidden waits and silent catches

Two patterns deserve a hard no in review:

```ts
// Bad: hides a timing problem
await page.waitForTimeout(5000);

// Bad: hides a failed action
try {
  await page.getByRole('button', { name: 'Submit' }).click();
} catch (error) {
  console.log('Ignoring click failure');
}
```

Replace them with state-based waits and real assertions:

```ts
await expect(page.getByRole('button', { name: 'Submit' })).toBeEnabled();
await page.getByRole('button', { name: 'Submit' }).click();
await expect(page.getByText('Request submitted')).toBeVisible();
```

### Make ownership visible

Every flaky or skipped test needs an owner. In a 15-person QA team, “someone will fix it” usually means nobody fixes it. Add annotations when needed, but do not use them as a dumping ground.

```ts
test('@smoke checkout saves billing address', async ({ page }) => {
  test.info().annotations.push({
    type: 'owner',
    description: 'payments-qa'
  });

  // test body
});
```

The goal is not bureaucracy. The goal is fast ownership when the release gate breaks at 7 PM.

## Common pitfalls I still see

Even good teams repeat the same mistakes. Use this section as a final scan before you call your framework production-ready.

### Pitfall 1: treating retries as a fix

Retries are a diagnostic tool. They are not a quality strategy. If a test passes on retry every day, the suite is telling you something. It may be an async issue, unstable data, slow environment, or a real intermittent product bug. Review retry reports weekly.

### Pitfall 2: mixing API setup with UI assertions badly

API setup is excellent when used carefully. Create data through APIs, then verify user behavior in the UI. Do not verify UI behavior only through API responses and still call it an end-to-end test.

### Pitfall 3: overusing page objects

Page Object Model is useful, but it can become a second application with its own bugs. Keep page objects thin. Store locators and simple actions there. Keep test intent visible in the spec file.

### Pitfall 4: ignoring accessibility locators

If `getByRole` does not work anywhere in your app, that may reveal an accessibility problem. Do not immediately switch to CSS for everything. Talk to developers and improve the UI contract.

### Pitfall 5: not training manual testers on trace reading

Manual testers moving into automation often think coding is the whole skill. It is not. Trace reading, failure triage, and risk-based test selection are equally valuable. In many Bengaluru product companies, that is the difference between “automation executor” and SDET ownership.

## Key takeaways

This Playwright TypeScript checklist is not a certificate that says your framework is perfect. It is a practical release-readiness scan. Run it before you add another hundred tests.

- Keep versions, scripts, and config explicit.
- Use user-facing locators and business-level assertions.
- Move repeated setup into typed fixtures, not copy-pasted helpers.
- Use storage state for authentication and keep secrets out of Git.
- Capture traces, screenshots, videos, and reports for CI failures.
- Review flaky tests as engineering work, not QA noise.
- Make ownership visible through tags, annotations, and triage notes.

If you completed the first 21 days, this bonus day is your final audit. Pick one existing test suite, run the checklist, and fix the top three gaps before writing new tests. That is how a Playwright TypeScript checklist becomes a real engineering habit.

### Is this Playwright TypeScript checklist only for large teams?

No. Small teams need it even more because they have less time to debug weak automation. Start with selectors, config, traces, and CI artifacts. Add the deeper fixture and data rules as your suite grows.

### Should every Playwright test use Page Object Model?

No. Use page objects when they reduce duplication without hiding the journey. For short flows, a clear spec file can be better than a page object full of one-line wrappers.

### 15. Joseph Ward — Blog index

- Source: https://josephward.tech/blog/
- Retrieved: 2026-08-29
- Exa status: complete

# Blog | Joseph Ward

Polyglot Testing Technologist — blog index (relevant Playwright/TypeScript entries at the top):

- [Looking Behind Playwright's Magic](https://josephward.tech/2026-07-07-looking-behind-playwrights-magic-edited/) — 07 Jul 2026. A Playwright click looks simple:
- [Why Simple UI Tests Become Slow](https://josephward.tech/2026-06-30-why-simple-ui-tests-become-slow/) — 30 Jun 2026. Sometimes, UI tests take longer than the code suggests they should.
- [Harmonising Selenium with Playwright and Cypress: A Journey Through Network Event Handling](https://josephward.tech/2024-01-21-harmonising-selenium/) — 21 Jan 2024. Introduction. Harnessing Network Events in Testing. Playwright and Cypress are well-known for their 'autowait' feature. This neat trick aligns elements seamlessly and avoids many 'test flake' issues, like Selenium's StaleElementException...
- [Designing a Sub-1kb Homepage: A Challenge in Imagination and Purposeful Coding Approach](https://josephward.tech/2023-05-02-designing-a-sub-1kb-homepage/) — 02 May 2023. I designed a homepage under 1kB recently. Although I reverted it after a few weeks, I decided to write a blog post on why I made it and the reason...
- [Green Testing: Measuring and Reducing Your Software's Energy Use and Emissions](https://josephward.tech/2023-04-07-green-testing/) — 07 Apr 2023. In addition to verifying software functionality, testing is where we assure that we meet our non-functional needs. If we treat emissions as a non-functional need, is it an appropriate place...
- [Lessons Learned from Testing Restful-Booker: A 2-hour Deep Dive into a Web Service](https://josephward.tech/2020-06-29-lets-test-something/) — 29 Jun 2020. How do I put this? Sometimes you've got to test. Not talk about testing, not agonize over planning, but get stuck into adding value as quickly as possible. In this...
- [My thoughts on performance testing libraries](https://josephward.tech/2020-06-01-my-thoughts-on-performance-testing-libraries/) — 01 Jun 2020. Sometimes I write things in Slack conversationally that might be useful to refer to later or reflect on. Going forwards I'll try to remember to add them here.
- [Confessions of a Idea Thief: Embracing the Art of Stealing Inspiration](https://josephward.tech/2020-03-04-good-testers-copy-great-testers-steal/) — 04 Mar 2020. “Good artists software testers copy, great artists software testers steal.” - Pablo Picasso / Joseph Ward
- [Hacking for muggles](https://josephward.tech/2019-12-11-hacking-for-muggles/) — 11 Dec 2019. Hacking: it's not magic, even muggles can do it.
- [Building and Growing a Modern Testing Capability: A Testing Manifesto for Success](https://josephward.tech/2019-08-01-what-is-test-and-assurance/) — 01 Aug 2019. Please note: I originally wrote this post for BJSS' internal blogosphere. If you find this interpretation of what role testers play in software delivery appealing, then please get in touch....
- [Lessons Learned: Revisiting Test and Assurance Design for Past Projects](https://josephward.tech/2018-10-19-test-and-assurance-design/) — 19 Oct 2018. Recently, I commiserated with a developer on missed opportunities in past projects. This was a great chance to spitball ideas for test and assurance design. What follows is a bit...
- [Why We Should Stop Using 'Non-Functional' to Describe Important Aspects of Software](https://josephward.tech/2018-09-10-nfrs-nft-shenanigans/) — 10 Sep 2018. Welcome to another blog post! As usual, these are my opinions. Not facts. Disagree with anything I've said? Get in touch, let's talk. Happy to have my mind changed. Having...
- [Why 'Good Practices' Trump 'Best Practices' in Test Design](https://josephward.tech/2018-08-08-best-vs-good/) — 08 Aug 2018. Today I have been thinking about why I prefer the phrase “good practices” over “best practices” in test design. My preference is so tenacious that I exchange the word “best”...
- [Exploring the Power and Pitfalls of JavaScript Injection for Web Testing](https://josephward.tech/2018-07-09-injecting-javascript/) — 09 Jul 2018. Sometimes, you can test things on a webpage by injecting JavaScript. It's fairly simple, fairly powerful, but not without its gotchas. So what can you do and how? Here's a...
- [SeleniumBase vs. Vanilla Selenium: Which One Should You Choose for Your Testing Needs?](https://josephward.tech/2018-04-15-an-outlook-on-test-frameworks-via-seleniumbase/) — 15 Apr 2018. It's frustrating to go from 0 to implementing automation in testing at any scale. For some, this is down to a lack of experience with programming or scripting languages, for...
- [Finding the Value in Blogging as a Tester](https://josephward.tech/2017-07-23-why-blog/) — 23 Jul 2017. Today I will be talking to myself to try to answer the question: why blog?
- [The Power of Tinkering: How a Little Curiosity Can Lead to Big Achievements in Software Testing](https://josephward.tech/2017-06-29-tinkering-around/) — 29 Jun 2017. I don't know where I'd be today if I didn't tinker around with things. Certainly not in software testing, certainly not writing this blog, and certainly not having nearly as...

### 16. Playwright — Best Practices

- Source: https://playwright.dev/docs/best-practices
- Retrieved: 2026-08-29
- Exa status: complete

# Best Practices | Playwright

## Introduction

This guide should help you to make sure you are following our best practices and writing tests that are more resilient.

## Testing philosophy

### Test user-visible behavior

Automated tests should verify that the application code works for the end users, and avoid relying on implementation details such as things which users will not typically use, see, or even know about such as the name of a function, whether something is an array, or the CSS class of some element. The end user will see or interact with what is rendered on the page, so your test should typically only see/interact with the same rendered output.

### Make tests as isolated as possible

Each test should be completely isolated from another test and should run independently with its own local storage, session storage, data, cookies etc. Test isolation improves reproducibility, makes debugging easier and prevents cascading test failures.

In order to avoid repetition for a particular part of your test you can use before and after hooks. Within your test file add a before hook to run a part of your test before each test such as going to a particular URL or logging in to a part of your app. This keeps your tests isolated as no test relies on another. However it is also ok to have a little duplication when tests are simple enough especially if it keeps your tests clearer and easier to read and maintain.

```js
import { test } from '@playwright/test';test.beforeEach(async ({ page }) => {  // Runs before each test and signs in each page.  await page.goto('https://github.com/login');  await page.getByLabel('Username or email address').fill('username');  await page.getByLabel('Password').fill('password');  await page.getByRole('button', { name: 'Sign in' }).click();});test('first', async ({ page }) => {  // page is signed in.});test('second', async ({ page }) => {  // page is signed in.});
```

You can also reuse the signed-in state in the tests with setup project. That way you can log in only once and then skip the log in step for all of the tests.

### Avoid testing third-party dependencies

Only test what you control. Don't try to test links to external sites or third party servers that you do not control. Not only is it time consuming and can slow down your tests but also you cannot control the content of the page you are linking to, or if there are cookie banners or overlay pages or anything else that might cause your test to fail.

Instead, use the Playwright Network API and guarantee the response needed.

```js
await page.route('**/api/fetch_data_third_party_dependency', route => route.fulfill({  status: 200,  body: testData,}));await page.goto('https://example.com');
```

### Testing with a database

If working with a database then make sure you control the data. Test against a staging environment and make sure it doesn't change. For visual regression tests make sure the operating system and browser versions are the same.

## Best Practices

### Use locators

In order to write end to end tests we need to first find elements on the webpage. We can do this by using Playwright's built in locators. Locators come with auto waiting and retry-ability. Auto waiting means that Playwright performs a range of actionability checks on the elements, such as ensuring the element is visible and enabled before it performs the click. To make tests resilient, we recommend prioritizing user-facing attributes and explicit contracts.

```js
// 👍page.getByRole('button', { name: 'submit' });
```

#### Use chaining and filtering

Locators can be chained to narrow down the search to a particular part of the page.

```js
const product = page.getByRole('listitem').filter({ hasText: 'Product 2' });
```

You can also filter locators by text or by another locator.

```js
await page    .getByRole('listitem')    .filter({ hasText: 'Product 2' })    .getByRole('button', { name: 'Add to cart' })    .click();
```

#### Prefer user-facing attributes to XPath or CSS selectors

Your DOM can easily change so having your tests depend on your DOM structure can lead to failing tests. For example consider selecting this button by its CSS classes. Should the designer change something then the class might change, thus breaking your test.

```js
// 👎page.locator('button.buttonIcon.episode-actions-later');
```

Use locators that are resilient to changes in the DOM.

```js
// 👍page.getByRole('button', { name: 'submit' });
```

### Generate locators

Playwright has a test generator that can generate tests and pick locators for you. It will look at your page and figure out the best locator, prioritizing role, text and test id locators. If the generator finds multiple elements matching the locator, it will improve the locator to make it resilient and uniquely identify the target element, so you don't have to worry about failing tests due to locators.

#### Use codegen to generate locators

To pick a locator run the `codegen` command followed by the URL that you would like to pick a locator from.

```bash
npx playwright codegen playwright.dev
```

This will open a new browser window as well as the Playwright inspector. To pick a locator first click on the 'Record' button to stop the recording. By default when you run the `codegen` command it will start a new recording. Once you stop the recording the 'Pick Locator' button will be available to click.

You can then hover over any element on your page in the browser window and see the locator highlighted below your cursor. Clicking on an element will add the locator into the Playwright inspector. You can either copy the locator and paste into your test file or continue to explore the locator by editing it in the Playwright Inspector, for example by modifying the text, and seeing the results in the browser window.

#### Use the VS Code extension to generate locators

You can also use the VS Code Extension to generate locators as well as record a test. The VS Code extension also gives you a great developer experience when writing, running, and debugging tests.

### Use web first assertions

Assertions are a way to verify that the expected result and the actual result matched or not. By using web first assertions Playwright will wait until the expected condition is met. For example, when testing an alert message, a test would click a button that makes a message appear and check that the alert message is there. If the alert message takes half a second to appear, assertions such as `toBeVisible()` will wait and retry if needed.

```js
// 👍await expect(page.getByText('welcome')).toBeVisible();// 👎expect(await page.getByText('welcome').isVisible()).toBe(true);
```

#### Don't use manual assertions

Don't use manual assertions that are not awaiting the expect. In the code below the await is inside the expect rather than before it. When using assertions such as `isVisible()` the test won't wait a single second, it will just check the locator is there and return immediately.

```js
// 👎expect(await page.getByText('welcome').isVisible()).toBe(true);
```

Use web first assertions such as `toBeVisible()` instead.

```js
// 👍await expect(page.getByText('welcome')).toBeVisible();
```

### Configure debugging

#### Local debugging

For local debugging we recommend you debug your tests live in VS Code by installing the VS Code extension. You can run tests in debug mode by right-clicking on the line next to the test you want to run which will open a browser window and pause at where the breakpoint is set.

You can live debug your test by clicking or editing the locators in your test in VS Code which will highlight this locator in the browser window as well as show you any other matching locators found on the page.

You can also debug your tests with the Playwright inspector by running your tests with the `--debug` flag.

```bash
npx playwright test --debug
```

You can then step through your test, view actionability logs and edit the locator live and see it highlighted in the browser window. This will show you which locators match, how many of them there are.

To debug a specific test add the name of the test file and the line number of the test followed by the `--debug` flag.

```bash
npx playwright test example.spec.ts:9 --debug
```

#### Debugging on CI

For CI failures, use the Playwright trace viewer instead of videos and screenshots. The trace viewer gives you a full trace of your tests as a local Progressive Web App (PWA) that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action using dev tools, view network requests and more.

Traces are configured in the Playwright config file and are set to run on CI on the first retry of a failed test. We don't recommend setting this to `on` so that traces are run on every test as it's very performance heavy. However you can run a trace locally when developing with the `--trace` flag.

```bash
npx playwright test --trace on
```

Once you run this command your traces will be recorded for each test and can be viewed directly from the HTML report.

```bash
npx playwright show-report
```

Traces can be opened by clicking on the icon next to the test file name or by opening each of the test reports and scrolling down to the traces section.

### Use Playwright's Tooling

Playwright comes with a range of tooling to help you write tests.

- The VS Code extension gives you a great developer experience when writing, running, and debugging tests.
- The test generator can generate tests and pick locators for you.
- The trace viewer gives you a full trace of your tests as a local PWA that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action, view network requests and more.
- The UI Mode lets you explore, run and debug tests with a time travel experience complete with watch mode. All test files are loaded into the testing sidebar where you can expand each file and describe block to individually run, view, watch and debug each test.
- TypeScript in Playwright works out of the box and gives you better IDE integrations. Your IDE will show you everything you can do and highlight when you do something wrong. No TypeScript experience is needed and it is not necessary for your code to be in TypeScript, all you need to do is create your tests with a `.ts` extension.

### Test across all browsers

Playwright makes it easy to test your site across all browsers no matter what platform you are on. Testing across all browsers ensures your app works for all users. In your config file you can set up projects adding the name and which browser or device to use.

playwright.config.ts

```js
import { defineConfig, devices } from '@playwright/test';export default defineConfig({  projects: [    {      name: 'chromium',      use: { ...devices['Desktop Chrome'] },    },    {      name: 'firefox',      use: { ...devices['Desktop Firefox'] },    },    {      name: 'webkit',      use: { ...devices['Desktop Safari'] },    },  ],});
```

### Keep your Playwright dependency up to date

By keeping your Playwright version up to date you will be able to test your app on the latest browser versions and catch failures before the latest browser version is released to the public.

```bash
npm install -D @playwright/test@latest
```

Check the release notes to see what the latest version is and what changes have been released.

You can see what version of Playwright you have by running the following command.

```bash
npx playwright --version
```

### Run tests on CI

Setup CI/CD and run your tests frequently. The more often you run your tests the better. Ideally you should run your tests on each commit and pull request. Playwright comes with a GitHub actions workflow so that tests will run on CI for you with no setup required. Playwright can also be setup on the CI environment of your choice.

Use Linux when running your tests on CI as it is cheaper. Developers can use whatever environment when running locally but use linux on CI. Consider setting up Sharding to make CI faster.

#### Optimize browser downloads on CI

Only install the browsers that you actually need, especially on CI. For example, if you're only testing with Chromium, install just Chromium.

.github/workflows/playwright.yml

```bash
# Instead of installing all browsersnpx playwright install --with-deps# Install only Chromiumnpx playwright install chromium --with-deps
```

This saves both download time and disk space on your CI machines.

### Lint your tests

We recommend TypeScript and linting with ESLint for your tests to catch errors early. Use @typescript-eslint/no-floating-promises ESLint rule to make sure there are no missing awaits before the asynchronous calls to the Playwright API. On your CI you can run `tsc --noEmit` to ensure that functions are called with the right signature.

### Use parallelism and sharding

Playwright runs tests in parallel by default. Tests in a single file are run in order, in the same worker process. If you have many independent tests in a single file, you might want to run them in parallel

```js
import { test } from '@playwright/test';test.describe.configure({ mode: 'parallel' });test('runs in parallel 1', async ({ page }) => { /* ... */ });test('runs in parallel 2', async ({ page }) => { /* ... */ });
```

Playwright can shard a test suite, so that it can be executed on multiple machines.

```bash
npx playwright test --shard=1/3
```

## Productivity tips

### Use Soft assertions

If your test fails, Playwright will give you an error message showing what part of the test failed which you can see either in VS Code, the terminal, the HTML report, or the trace viewer. However, you can also use soft assertions. These do not immediately terminate the test execution, but rather compile and display a list of failed assertions once the test ended.

```js
// Make a few checks that will not stop the test when failed...await expect.soft(page.getByTestId('status')).toHaveText('Success');// ... and continue the test to check more things.await page.getByRole('link', { name: 'next page' }).click();
```

### 17. Playwright — Fixtures

- Source: https://playwright.dev/docs/test-fixtures
- Retrieved: 2026-08-29
- Exa status: complete

# Fixtures | Playwright

## Introduction

Playwright Test is based on the concept of test fixtures. Test fixtures are used to establish the environment for each test, giving the test everything it needs and nothing else. Test fixtures are isolated between tests. With fixtures, you can group tests based on their meaning, instead of their common setup.

### Built-in fixtures

You have already used test fixtures in your first test.

```js
import { test, expect } from '@playwright/test';test('basic test', async ({ page }) => {  await page.goto('https://playwright.dev/');  await expect(page).toHaveTitle(/Playwright/);});
```

The `{ page }` argument tells Playwright Test to set up the `page` fixture and provide it to your test function.

Here is a list of the pre-defined fixtures that you are likely to use most of the time:

| Fixture | Type | Description |
| --- | --- | --- |
| page | Page | Isolated page for this test run. |
| context | BrowserContext | Isolated context for this test run. The `page` fixture belongs to this context as well. Learn how to configure context. |
| browser | Browser | Browsers are shared across tests to optimize resources. Learn how to configure browsers. |
| browserName | string | The name of the browser currently running the test. Either `chromium`, `firefox` or `webkit`. |
| request | APIRequestContext | Isolated APIRequestContext instance for this test run. |

### Without fixtures

Here is how a typical test environment setup differs between the traditional test style and the fixture-based one.

`TodoPage` is a class that helps us interact with a "todo list" page of the web app, following the Page Object Model pattern. It uses Playwright's `page` internally.

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';export class TodoPage {  private readonly inputBox: Locator;  private readonly todoItems: Locator;  constructor(public readonly page: Page) {    this.inputBox = this.page.locator('input.new-todo');    this.todoItems = this.page.getByTestId('todo-item');  }  async goto() {    await this.page.goto('https://demo.playwright.dev/todomvc/');  }  async addToDo(text: string) {    await this.inputBox.fill(text);    await this.inputBox.press('Enter');  }  async remove(text: string) {    const todo = this.todoItems.filter({ hasText: text });    await todo.hover();    await todo.getByLabel('Delete').click();  }  async removeAll() {    while ((await this.todoItems.count()) > 0) {      await this.todoItems.first().hover();      await this.todoItems.getByLabel('Delete').first().click();    }  }}
```

todo.spec.ts

```js
const { test } = require('@playwright/test');const { TodoPage } = require('./todo-page');test.describe('todo tests', () => {  let todoPage;  test.beforeEach(async ({ page }) => {    todoPage = new TodoPage(page);    await todoPage.goto();    await todoPage.addToDo('item1');    await todoPage.addToDo('item2');  });  test.afterEach(async () => {    await todoPage.removeAll();  });  test('should add an item', async () => {    await todoPage.addToDo('my item');    // ...  });  test('should remove an item', async () => {    await todoPage.remove('item1');    // ...  });});
```

### With fixtures

Fixtures have a number of advantages over before/after hooks:

- Fixtures encapsulate setup and teardown in the same place so it is easier to write. So if you have an after hook that tears down what was created in a before hook, consider turning them into a fixture.
- Fixtures are reusable between test files - you can define them once and use them in all your tests. That's how Playwright's built-in `page` fixture works. So if you have a helper function that is used in multiple tests, consider turning it into a fixture.
- Fixtures are on-demand - you can define as many fixtures as you'd like, and Playwright Test will setup only the ones needed by your test and nothing else.
- Fixtures are composable - they can depend on each other to provide complex behaviors.
- Fixtures are flexible. Tests can use any combination of fixtures to precisely tailor the environment to their needs, without affecting other tests.
- Fixtures simplify grouping. You no longer need to wrap tests in `describe` s that set up their environment, and are free to group your tests by their meaning instead.

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';export class TodoPage {  private readonly inputBox: Locator;  private readonly todoItems: Locator;  constructor(public readonly page: Page) {    this.inputBox = this.page.locator('input.new-todo');    this.todoItems = this.page.getByTestId('todo-item');  }  async goto() {    await this.page.goto('https://demo.playwright.dev/todomvc/');  }  async addToDo(text: string) {    await this.inputBox.fill(text);    await this.inputBox.press('Enter');  }  async remove(text: string) {    const todo = this.todoItems.filter({ hasText: text });    await todo.hover();    await todo.getByLabel('Delete').click();  }  async removeAll() {    while ((await this.todoItems.count()) > 0) {      await this.todoItems.first().hover();      await this.todoItems.getByLabel('Delete').first().click();    }  }}
```

example.spec.ts

```js
import { test as base } from '@playwright/test';import { TodoPage } from './todo-page';// Extend basic test by providing a "todoPage" fixture.const test = base.extend<{ todoPage: TodoPage }>({  todoPage: async ({ page }, use) => {    const todoPage = new TodoPage(page);    await todoPage.goto();    await todoPage.addToDo('item1');    await todoPage.addToDo('item2');    await use(todoPage);    await todoPage.removeAll();  },});test('should add an item', async ({ todoPage }) => {  await todoPage.addToDo('my item');  // ...});test('should remove an item', async ({ todoPage }) => {  await todoPage.remove('item1');  // ...});
```

## Creating a fixture

To create your own fixture, use test.extend() to create a new `test` object that will include it.

Below we create two fixtures `todoPage` and `settingsPage` that follow the Page Object Model pattern.

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';export class TodoPage {  private readonly inputBox: Locator;  private readonly todoItems: Locator;  constructor(public readonly page: Page) {    this.inputBox = this.page.locator('input.new-todo');    this.todoItems = this.page.getByTestId('todo-item');  }  async goto() {    await this.page.goto('https://demo.playwright.dev/todomvc/');  }  async addToDo(text: string) {    await this.inputBox.fill(text);    await this.inputBox.press('Enter');  }  async remove(text: string) {    const todo = this.todoItems.filter({ hasText: text });    await todo.hover();    await todo.getByLabel('Delete').click();  }  async removeAll() {    while ((await this.todoItems.count()) > 0) {      await this.todoItems.first().hover();      await this.todoItems.getByLabel('Delete').first().click();    }  }}
```

SettingsPage is similar:

settings-page.ts

```js
import type { Page } from '@playwright/test';export class SettingsPage {  constructor(public readonly page: Page) {  }  async switchToDarkMode() {    // ...  }}
```

my-test.ts

```js
import { test as base } from '@playwright/test';import { TodoPage } from './todo-page';import { SettingsPage } from './settings-page';// Declare the types of your fixtures.type MyFixtures = {  todoPage: TodoPage;  settingsPage: SettingsPage;};// Extend base test by providing "todoPage" and "settingsPage".// This new "test" can be used in multiple test files, and each of them will get the fixtures.export const test = base.extend<MyFixtures>({  todoPage: async ({ page }, use) => {    // Set up the fixture.    const todoPage = new TodoPage(page);    await todoPage.goto();    await todoPage.addToDo('item1');    await todoPage.addToDo('item2');    // Use the fixture value in the test.    await use(todoPage);    // Clean up the fixture.    await todoPage.removeAll();  },  settingsPage: async ({ page }, use) => {    await use(new SettingsPage(page));  },});export { expect } from '@playwright/test';
```

note

Custom fixture names should start with a letter or underscore, and can contain only letters, numbers, and underscores.

## Using a fixture

Just mention a fixture in your test function argument, and the test runner will take care of it. Fixtures are also available in hooks and other fixtures. If you use TypeScript, fixtures will be type safe.

Below we use the `todoPage` and `settingsPage` fixtures that we defined above.

```js
import { test, expect } from './my-test';test.beforeEach(async ({ settingsPage }) => {  await settingsPage.switchToDarkMode();});test('basic test', async ({ todoPage, page }) => {  await todoPage.addToDo('something nice');  await expect(page.getByTestId('todo-title')).toContainText(['something nice']);});
```

## Overriding fixtures

In addition to creating your own fixtures, you can also override existing fixtures to fit your needs. Consider the following example which overrides the `page` fixture by automatically navigating to the `baseURL`:

```js
import { test as base } from '@playwright/test';export const test = base.extend({  page: async ({ baseURL, page }, use) => {    await page.goto(baseURL);    await use(page);  },});
```

Notice that in this example, the `page` fixture is able to depend on other built-in fixtures such as testOptions.baseURL. We can now configure `baseURL` in the configuration file, or locally in the test file with test.use().

example.spec.ts

```js
test.use({ baseURL: 'https://playwright.dev' });
```

Fixtures can also be overridden, causing the base fixture to be completely replaced with something different. For example, we could override the testOptions.storageState fixture to provide our own data.

```js
import { test as base } from '@playwright/test';export const test = base.extend({  storageState: async ({}, use) => {    const cookie = await getAuthCookie();    await use({ cookies: [cookie] });  },});
```

## Worker-scoped fixtures

Playwright Test uses worker processes to run test files. Similar to how test fixtures are set up for individual test runs, worker fixtures are set up for each worker process. That's where you can set up services, run servers, etc. Playwright Test will reuse the worker process for as many test files as it can, provided their worker fixtures match and hence environments are identical.

Below we'll create an `account` fixture that will be shared by all tests in the same worker, and override the `page` fixture to log in to this account for each test. To generate unique accounts, we'll use the workerInfo.workerIndex that is available to any test or fixture. Note the tuple-like syntax for the worker fixture - we have to pass `{scope: 'worker'}` so that test runner sets this fixture up once per worker.

In addition to only being run once per worker, worker-scoped fixtures also get a separate timeout equal to the default test timeout. You can change it by passing the `timeout` option. See fixture timeout for more details.

my-test.ts

```js
import { test as base } from '@playwright/test';type Account = {  username: string;  password: string;};// Note that we pass worker fixture types as a second template parameter.export const test = base.extend<{}, { account: Account }>({  account: [async ({ browser }, use, workerInfo) => {    // Unique username.    const username = 'user' + workerInfo.workerIndex;    const password = 'verysecure';    // Create the account with Playwright.    const page = await browser.newPage();    await page.goto('/signup');    await page.getByLabel('User Name').fill(username);    await page.getByLabel('Password').fill(password);    await page.getByText('Sign up').click();    // Make sure everything is ok.    await expect(page.getByTestId('result')).toHaveText('Success');    // Do not forget to cleanup.    await page.close();    // Use the account value.    await use({ username, password });  }, { scope: 'worker' }],  page: async ({ page, account }, use) => {    // Sign in with our account.    const { username, password } = account;    await page.goto('/signin');    await page.getByLabel('User Name').fill(username);    await page.getByLabel('Password').fill(password);    await page.getByText('Sign in').click();    await expect(page.getByTestId('userinfo')).toHaveText(username);    // Use signed-in page in the test.    await use(page);  },});export { expect } from '@playwright/test';
```

## Automatic fixtures

Automatic fixtures are set up for each test/worker, even when the test does not list them directly. To create an automatic fixture, use the tuple syntax and pass `{ auto: true }`.

Here is an example fixture that automatically attaches debug logs when the test fails, so we can later review the logs in the reporter. Note how it uses the TestInfo object that is available in each test/fixture to retrieve metadata about the test being run.

my-test.ts

```js
import debug from 'debug';import fs from 'fs';import { test as base } from '@playwright/test';export const test = base.extend<{ saveLogs: void }>({  saveLogs: [async ({}, use, testInfo) => {    // Collecting logs during the test.    const logs = [];    debug.log = (...args) => logs.push(args.map(String).join(''));    debug.enable('myserver');    await use();    // After the test we can check whether the test passed or failed.    if (testInfo.status !== testInfo.expectedStatus) {      // outputPath() API guarantees a unique file name.      const logFile = testInfo.outputPath('logs.txt');      await fs.promises.writeFile(logFile, logs.join('\n'), 'utf8');      testInfo.attachments.push({ name: 'logs', contentType: 'text/plain', path: logFile });    }  }, { auto: true }],});export { expect } from '@playwright/test';
```

## Fixture timeout

Fixture is considered to be a part of a test, and so its setup and teardown running time counts towards the test timeout. Therefore, a slow fixture may cause test timeouts. You can set a separate larger timeout for such a fixture, and keep the overall test timeout small.

```js
import { test as base, expect } from '@playwright/test';const test = base.extend<{ slowFixture: string }>({  slowFixture: [async ({}, use) => {    // ... perform a slow operation ...    await use('hello');  }, { timeout: 60000 }]});test('example test', async ({ slowFixture }) => {  // ...});
```

Unlike regular test-scoped fixtures, each worker-scoped fixture has its own timeout, equal to the test timeout. You can change the timeout for a worker-scoped fixture in the same way.

## Fixtures-options

Playwright Test supports running multiple test projects that can be configured separately. You can use "option" fixtures to make your configuration options declarative and type safe. Learn more about parameterizing tests.

Below we'll create a `defaultItem` option in addition to the `todoPage` fixture from other examples. This option will be set in the configuration file. Note the tuple syntax and `{ option: true }` argument.

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';export class TodoPage {  private readonly inputBox: Locator;  private readonly todoItems: Locator;  constructor(public readonly page: Page) {    this.inputBox = this.page.locator('input.new-todo');    this.todoItems = this.page.getByTestId('todo-item');  }  async goto() {    await this.page.goto('https://demo.playwright.dev/todomvc/');  }  async addToDo(text: string) {    await this.inputBox.fill(text);    await this.inputBox.press('Enter');  }  async remove(text: string) {    const todo = this.todoItems.filter({ hasText: text });    await todo.hover();    await todo.getByLabel('Delete').click();  }  async removeAll() {    while ((await this.todoItems.count()) > 0) {      await this.todoItems.first().hover();      await this.todoItems.getByLabel('Delete').first().click();    }  }}
```

my-test.ts

```js
import { test as base } from '@playwright/test';import { TodoPage } from './todo-page';// Declare your options to type-check your configuration.export type MyOptions = {  defaultItem: string;};type MyFixtures = {  todoPage: TodoPage;};// Specify both option and fixture types.export const test = base.extend<MyOptions & MyFixtures>({  // Define an option and provide a default value.  // We can later override it in the config.  defaultItem: ['Something nice', { option: true }],  // Our "todoPage" fixture depends on the option.  todoPage: async ({ page, defaultItem }, use) => {    const todoPage = new TodoPage(page);    await todoPage.goto();    await todoPage.addToDo(defaultItem);    await use(todoPage);    await todoPage.removeAll();  },});export { expect } from '@playwright/test';
```

We can now use the `todoPage` fixture as usual, and set the `defaultItem` option in the configuration file.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';import type { MyOptions } from './my-test';export default defineConfig<MyOptions>({  projects: [    {      name: 'shopping',      use: { defaultItem: 'Buy milk' },    },    {      name: 'wellbeing',      use: { defaultItem: 'Exercise!' },    },  ]});
```

Array as an option value

If the value of your option is an array, for example `[{ name: 'Alice' }, { name: 'Bob' }]`, you'll need to wrap it into an extra array when providing the value. This is best illustrated with an example.

```js
type Person = { name: string };const test = base.extend<{ persons: Person[] }>({  // Declare the option, default value is an empty array.  persons: [[], { option: true }],});// Option value is an array of persons.const actualPersons = [{ name: 'Alice' }, { name: 'Bob' }];test.use({  // CORRECT: Wrap the value into an array and pass the scope.  persons: [actualPersons, { scope: 'test' }],});test.use({  // WRONG: passing an array value directly will not work.  persons: actualPersons,});
```

Reset an option

You can reset an option to the value defined in the config file by setting it to `undefined`. Consider the following config that sets a `baseURL`:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';export default defineConfig({  use: {    baseURL: 'https://playwright.dev',  },});
```

You can now configure `baseURL` for a file, and also opt-out for a single test.

intro.spec.ts

```js
import { test } from '@playwright/test';// Configure baseURL for this file.test.use({ baseURL: 'https://playwright.dev/docs/intro' });test('check intro contents', async ({ page }) => {  // This test will use "https://playwright.dev/docs/intro" base url as defined above.});test.describe(() => {  // Reset the value to a config-defined one.  test.use({ baseURL: undefined });  test('can navigate to intro from the home page', async ({ page }) => {    // This test will use "https://playwright.dev" base url as defined in the config.  });});
```

If you would like to completely reset the value to `undefined`, use a long-form fixture notation.

intro.spec.ts

```js
import { test } from '@playwright/test';// Completely unset baseURL for this file.test.use({  baseURL: [async ({}, use) => use(undefined), { scope: 'test' }],});test('no base url', async ({ page }) => {  // This test will not have a base url.});
```

## Execution order

Each fixture has a setup and teardown phase before and after the `await use()` call in the fixture. Setup is executed before the test/hook requiring it is run, and teardown is executed when the fixture is no longer being used by the test/hook.

Fixtures follow these rules to determine the execution order:

- When fixture A depends on fixture B: B is always set up before A and torn down after A.
- Non-automatic fixtures are executed lazily, only when the test/hook needs them.
- Test-scoped fixtures are torn down after each test, while worker-scoped fixtures are only torn down when the worker process executing tests is torn down.

Consider the following example:

```js
import { test as base } from '@playwright/test';const test = base.extend<{  testFixture: string,  autoTestFixture: string,  unusedFixture: string,}, {  workerFixture: string,  autoWorkerFixture: string,}>({  workerFixture: [async ({ browser }) => {    // workerFixture setup...    await use('workerFixture');    // workerFixture teardown...  }, { scope: 'worker' }],  autoWorkerFixture: [async ({ browser }) => {    // autoWorkerFixture setup...    await use('autoWorkerFixture');    // autoWorkerFixture teardown...  }, { scope: 'worker', auto: true }],  testFixture: [async ({ page, workerFixture }) => {    // testFixture setup...    await use('testFixture');    // testFixture teardown...  }, { scope: 'test' }],  autoTestFixture: [async () => {    // autoTestFixture setup...    await use('autoTestFixture');    // autoTestFixture teardown...  }, { scope: 'test', auto: true }],  unusedFixture: [async ({ page }) => {    // unusedFixture setup...    await use('unusedFixture');    // unusedFixture teardown...  }, { scope: 'test' }],});test.beforeAll(async () => { /* ... */ });test.beforeEach(async ({ page }) => { /* ... */ });test('first test', async ({ page }) => { /* ... */ });test('second test', async ({ testFixture }) => { /* ... */ });test.afterEach(async () => { /* ... */ });test.afterAll(async () => { /* ... */ });
```

Normally, if all tests pass and no errors are thrown, the order of execution is as following.

- worker setup and `beforeAll` section:
  - `browser` setup because it is required by `autoWorkerFixture`.
  - `autoWorkerFixture` setup because automatic worker fixtures are always set up before anything else.
  - `beforeAll` runs.
- `first test` section:
  - `autoTestFixture` setup because automatic test fixtures are always set up before test and `beforeEach` hooks.
  - `page` setup because it is required in `beforeEach` hook.
  - `beforeEach` runs.
  - `first test` runs.
  - `afterEach` runs.
  - `page` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `autoTestFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
- `second test` section:
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
- `afterAll` and worker teardown section:
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

## Combine custom fixtures from multiple modules

You can merge test fixtures from multiple files or modules:

fixtures.ts

```js
import { mergeTests } from '@playwright/test';import { test as dbTest } from 'database-test-utils';import { test as a11yTest } from 'a11y-test-utils';export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test } from './fixtures';test('passes', async ({ database, page, a11y }) => {  // use database and a11y fixtures.});
```

## Box fixtures

Usually, custom fixtures are reported as separate steps in the UI mode, Trace Viewer and various test reports. They also appear in error messages from the test runner. For frequently used fixtures, this can mean lots of noise. You can stop the fixtures steps from being shown in the UI by "boxing" it.

```js
import { test as base } from '@playwright/test';export const test = base.extend({  helperFixture: [async ({}, use, testInfo) => {    // ...  }, { box: true }],});
```

This is useful for non-interesting helper fixtures. For example, an automatic fixture that sets up some common data can be safely hidden from a test report.

You can also mark the fixture as `box: 'self'` to only hide that particular fixture, but include all the steps inside the fixture in the test report.

## Custom fixture title

Instead of the usual fixture name, you can give fixtures a custom title that will be shown in test reports and error messages.

```js
import { test as base } from '@playwright/test';export const test = base.extend({  innerFixture: [async ({}, use, testInfo) => {    // ...  }, { title: 'my fixture' }],});
```

## Adding global beforeEach/afterEach hooks

test.beforeEach() and test.afterEach() hooks run before/after each test declared in the same file and same test.describe() block (if any). If you want to declare hooks that run before/after each test globally, you can declare them as auto fixtures like this:

fixtures.ts

```js
import { test as base } from '@playwright/test';export const test = base.extend<{ forEachTest: void }>({  forEachTest: [async ({ page }, use) => {    // This code runs before every test.    await page.goto('http://localhost:8000');    await use();    // This code runs after every test.    console.log('Last URL:', page.url());  }, { auto: true }],  // automatically starts for every test.});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';import { expect } from '@playwright/test';test('basic', async ({ page }) => {  expect(page).toHaveURL('http://localhost:8000');  await page.goto('https://playwright.dev');});
```

## Adding global beforeAll/afterAll hooks

test.beforeAll() and test.afterAll() hooks run before/after all tests declared in the same file and same test.describe() block (if any), once per worker process. If you want to declare hooks that run before/after all tests in every file, you can declare them as auto fixtures with `scope: 'worker'` as follows:

fixtures.ts

```js
import { test as base } from '@playwright/test';export const test = base.extend<{}, { forEachWorker: void }>({  forEachWorker: [async ({}, use) => {    // This code runs before all the tests in the worker process.    console.log(`Starting test worker ${test.info().workerIndex}`);    await use();    // This code runs after all the tests in the worker process.    console.log(`Stopping test worker ${test.info().workerIndex}`);  }, { scope: 'worker', auto: true }],  // automatically starts for every worker.});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';import { expect } from '@playwright/test';test('basic', async ({ }) => {  // ...});
```

Note that the fixtures will still run once per worker process, but you don't need to redeclare them in every file.

### 18. Playwright — Assertions

- Source: https://playwright.dev/docs/test-assertions
- Retrieved: 2026-08-29
- Exa status: complete

# Assertions | Playwright

## Introduction

Playwright includes test assertions in the form of `expect` function. To make an assertion, call `expect(value)` and choose a matcher that reflects the expectation. There are many generic matchers like `toEqual`, `toContain`, `toBeTruthy` that can be used to assert any conditions.

```js
expect(success).toBeTruthy();
```

Playwright also includes web-specific async matchers that will wait until the expected condition is met. Consider the following example:

```js
await expect(page.getByTestId('status')).toHaveText('Submitted');
```

Playwright will be re-testing the element with the test id of `status` until the fetched element has the `"Submitted"` text. It will re-fetch the element and check it over and over, until the condition is met or until the timeout is reached. You can either pass this timeout or configure it once via the testConfig.expect value in the test config.

By default, the timeout for assertions is set to 5 seconds. Learn more about various timeouts.

## Auto-retrying assertions

The following assertions will retry until the assertion passes, or the assertion timeout is reached. Note that retrying assertions are async, so you must `await` them.

| Assertion | Description |
| --- | --- |
| await expect(locator).toBeAttached() | Element is attached |
| await expect(locator).toBeChecked() | Checkbox is checked |
| await expect(locator).toBeDisabled() | Element is disabled |
| await expect(locator).toBeEditable() | Element is editable |
| await expect(locator).toBeEmpty() | Container is empty |
| await expect(locator).toBeEnabled() | Element is enabled |
| await expect(locator).toBeFocused() | Element is focused |
| await expect(locator).toBeHidden() | Element is not visible |
| await expect(locator).toBeInViewport() | Element intersects viewport |
| await expect(locator).toBeVisible() | Element is visible |
| await expect(locator).toContainText() | Element contains text |
| await expect(locator).toContainClass() | Element has specified CSS classes |
| await expect(locator).toHaveAccessibleDescription() | Element has a matching accessible description |
| await expect(locator).toHaveAccessibleName() | Element has a matching accessible name |
| await expect(locator).toHaveAttribute() | Element has a DOM attribute |
| await expect(locator).toHaveClass() | Element has specified CSS class property |
| await expect(locator).toHaveCount() | List has exact number of children |
| await expect(locator).toHaveCSS() | Element has CSS property |
| await expect(locator).toHaveId() | Element has an ID |
| await expect(locator).toHaveJSProperty() | Element has a JavaScript property |
| await expect(locator).toHaveRole() | Element has a specific ARIA role |
| await expect(locator).toHaveScreenshot() | Element has a screenshot |
| await expect(locator).toHaveText() | Element matches text |
| await expect(locator).toHaveValue() | Input has a value |
| await expect(locator).toHaveValues() | Select has options selected |
| await expect(locator).toMatchAriaSnapshot() | Element matches the Aria snapshot |
| await expect(page).toMatchAriaSnapshot() | Page matches the Aria snapshot |
| await expect(page).toHaveScreenshot() | Page has a screenshot |
| await expect(page).toHaveTitle() | Page has a title |
| await expect(page).toHaveURL() | Page has a URL |
| await expect(response).toBeOK() | Response has an OK status |

## Non-retrying assertions

These assertions allow to test any conditions, but do not auto-retry. Most of the time, web pages show information asynchronously, and using non-retrying assertions can lead to a flaky test.

Prefer auto-retrying assertions whenever possible. For more complex assertions that need to be retried, use `expect.poll` or `expect.toPass`.

| Assertion | Description |
| --- | --- |
| expect(value).toBe() | Value is the same |
| expect(value).toBeCloseTo() | Number is approximately equal |
| expect(value).toBeDefined() | Value is not `undefined` |
| expect(value).toBeFalsy() | Value is falsy, e.g. `false`, `0`, `null`, etc. |
| expect(value).toBeGreaterThan() | Number is more than |
| expect(value).toBeGreaterThanOrEqual() | Number is more than or equal |
| expect(value).toBeInstanceOf() | Object is an instance of a class |
| expect(value).toBeLessThan() | Number is less than |
| expect(value).toBeLessThanOrEqual() | Number is less than or equal |
| expect(value).toBeNaN() | Value is `NaN` |
| expect(value).toBeNull() | Value is `null` |
| expect(value).toBeTruthy() | Value is truthy, i.e. not `false`, `0`, `null`, etc. |
| expect(value).toBeUndefined() | Value is `undefined` |
| expect(value).toContain() | String contains a substring |
| expect(value).toContain() | Array or set contains an element |
| expect(value).toContainEqual() | Array or set contains a similar element |
| expect(value).toEqual() | Value is similar - deep equality and pattern matching |
| expect(value).toHaveLength() | Array or string has length |
| expect(value).toHaveProperty() | Object has a property |
| expect(value).toMatch() | String matches a regular expression |
| expect(value).toMatchObject() | Object contains specified properties |
| expect(value).toStrictEqual() | Value is similar, including property types |
| expect(value).toThrow() | Function throws an error |

## Asymmetric matchers

These expressions can be nested in other assertions to allow more relaxed matching against a given condition.

| Matcher | Description |
| --- | --- |
| expect.any() | Matches any instance of a class/primitive |
| expect.anything() | Matches anything |
| expect.arrayContaining() | Array contains specific elements |
| expect.arrayOf() | Array contains elements of specific type |
| expect.closeTo() | Number is approximately equal |
| expect.objectContaining() | Object contains specific properties |
| expect.stringContaining() | String contains a substring |
| expect.stringMatching() | String matches a regular expression |

## Negating matchers

In general, we can expect the opposite to be true by adding a `.not` to the front of the matchers:

```js
expect(value).not.toEqual(0);await expect(locator).not.toContainText('some text');
```

## Soft assertions

By default, failed assertion will terminate test execution. Playwright also supports soft assertions: failed soft assertions do not terminate test execution, but mark the test as failed.

```js
// Make a few checks that will not stop the test when failed...await expect.soft(page.getByTestId('status')).toHaveText('Success');await expect.soft(page.getByTestId('eta')).toHaveText('1 day');// ... and continue the test to check more things.await page.getByRole('link', { name: 'next page' }).click();await expect.soft(page.getByRole('heading', { name: 'Make another order' })).toBeVisible();
```

At any point during test execution, you can check whether there were any soft assertion failures:

```js
// Make a few checks that will not stop the test when failed...await expect.soft(page.getByTestId('status')).toHaveText('Success');await expect.soft(page.getByTestId('eta')).toHaveText('1 day');// Avoid running further if there were soft assertion failures.expect(test.info().errors).toHaveLength(0);
```

Note that soft assertions only work with Playwright test runner.

## Custom expect message

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
    Error: should be logged in    Call log:      - expect.toBeVisible with timeout 5000ms      - waiting for "getByText('Name')"      2 |      3 | test('example test', async({ page }) => {    > 4 |   await expect(page.getByText('Name'), 'should be logged in').toBeVisible();        |                                                                  ^      5 | });      6 |
```

Soft assertions also support custom message:

```js
expect.soft(value, 'my soft assertion').toBe(56);
```

## expect.configure

You can create your own pre-configured `expect` instance to have its own defaults such as `timeout` and `soft`.

```js
const slowExpect = expect.configure({ timeout: 10000 });await slowExpect(locator).toHaveText('Submit');// Always do soft assertions.const softExpect = expect.configure({ soft: true });await softExpect(locator).toHaveText('Submit');
```

## expect.poll

You can convert any synchronous `expect` to an asynchronous polling one using `expect.poll`.

The following method will poll given function until it returns HTTP status 200:

```js
await expect.poll(async () => {  const response = await page.request.get('https://api.example.com');  return response.status();}, {  // Custom expect message for reporting, optional.  message: 'make sure API eventually succeeds',  // Poll for 10 seconds; defaults to 5 seconds. Pass 0 to disable timeout.  timeout: 10000,}).toBe(200);
```

You can also specify custom polling intervals:

```js
await expect.poll(async () => {  const response = await page.request.get('https://api.example.com');  return response.status();}, {  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe  // ... Defaults to [100, 250, 500, 1000].  intervals: [1_000, 2_000, 10_000],  timeout: 60_000}).toBe(200);
```

You can combine `expect.soft` with `expect.poll` to perform soft assertions in polling logic. This allows the test to continue even if the assertion inside poll fails.

```js
await expect.soft.poll(async () => {  const response = await page.request.get('https://api.example.com');  return response.status();}).toBe(200);
```

`expect.configure({ soft: true })` also chains with `expect.poll` and is useful when you want to reuse a configured instance.

```js
const softExpect = expect.configure({ soft: true });await softExpect.poll(async () => {  const response = await page.request.get('https://api.example.com');  return response.status();}).toBe(200);
```

## expect.toPass

You can retry blocks of code until they are passing successfully.

```js
await expect(async () => {  const response = await page.request.get('https://api.example.com');  expect(response.status()).toBe(200);}).toPass();
```

You can also specify custom timeout and retry intervals:

```js
await expect(async () => {  const response = await page.request.get('https://api.example.com');  expect(response.status()).toBe(200);}).toPass({  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe  // ... Defaults to [100, 250, 500, 1000].  intervals: [1_000, 2_000, 10_000],  timeout: 60_000});
```

Note that by default `toPass` has timeout 0 and does not respect custom expect timeout.

## Add custom matchers using expect.extend

You can extend Playwright assertions by providing custom matchers. These matchers will be available on the `expect` object.

In this example we add a custom `toHaveAmount` function. Custom matcher should return a `pass` flag indicating whether the assertion passed, and a `message` callback that's used when the assertion fails.

fixtures.ts

```js
import { expect as baseExpect } from '@playwright/test';import type { Locator } from '@playwright/test';export { test } from '@playwright/test';export const expect = baseExpect.extend({  async toHaveAmount(locator: Locator, expected: number, options?: { timeout?: number }) {    const assertionName = 'toHaveAmount';    let pass: boolean;    let matcherResult: any;    try {      const expectation = this.isNot ? baseExpect(locator).not : baseExpect(locator);      await expectation.toHaveAttribute('data-amount', String(expected), options);      pass = true;    } catch (e: any) {      matcherResult = e.matcherResult;      pass = false;    }    if (this.isNot) {      pass = !pass;    }    const message = pass      ? () => this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +          '\n\n' +          `Locator: ${locator}\n` +          `Expected: not ${this.utils.printExpected(expected)}\n` +          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '')      : () =>  this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +          '\n\n' +          `Locator: ${locator}\n` +          `Expected: ${this.utils.printExpected(expected)}\n` +          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '');    return {      message,      pass,      name: assertionName,      expected,      actual: matcherResult?.actual,    };  },});
```

Now we can use `toHaveAmount` in the test.

example.spec.ts

```js
import { test, expect } from './fixtures';test('amount', async () => {  await expect(page.locator('.cart')).toHaveAmount(4);});
```

### Compatibility with expect library

note

Do not confuse Playwright's `expect` with the expect library. The latter is not fully integrated with Playwright test runner, so make sure to use Playwright's own `expect`.

### Combine custom matchers from multiple modules

You can combine custom matchers from multiple files or modules.

fixtures.ts

```js
import { mergeTests, mergeExpects } from '@playwright/test';import { test as dbTest, expect as dbExpect } from 'database-test-utils';import { test as a11yTest, expect as a11yExpect } from 'a11y-test-utils';export const expect = mergeExpects(dbExpect, a11yExpect);export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test, expect } from './fixtures';test('passes', async ({ database }) => {  await expect(database).toHaveDatabaseUser('admin');});
```

### 19. Playwright — Configuration

- Source: https://playwright.dev/docs/test-configuration
- Retrieved: 2026-08-29
- Exa status: complete

# Configuration | Playwright

## Introduction

Playwright has many options to configure how your tests are run. You can specify these options in the configuration file. Note that test runner options are top-level, do not put them into the `use` section.

## Basic Configuration

Here are some of the most common configuration options.

```js
import { defineConfig, devices } from '@playwright/test';export default defineConfig({  // Look for test files in the "tests" directory, relative to this configuration file.  testDir: 'tests',  // Run all tests in parallel.  fullyParallel: true,  // Fail the build on CI if you accidentally left test.only in the source code.  forbidOnly: !!process.env.CI,  // Retry on CI only.  retries: process.env.CI ? 2 : 0,  // Opt out of parallel tests on CI.  workers: process.env.CI ? 1 : undefined,  // Reporter to use  reporter: 'html',  use: {    // Base URL to use in actions like `await page.goto('/')`.    baseURL: 'http://localhost:3000',    // Collect trace when retrying the failed test.    trace: 'on-first-retry',  },  // Configure projects for major browsers.  projects: [    {      name: 'chromium',      use: { ...devices['Desktop Chrome'] },    },  ],  // Run your local dev server before starting the tests.  webServer: {    command: 'npm run start',    url: 'http://localhost:3000',    reuseExistingServer: !process.env.CI,  },});
```

| Option | Description |
| --- | --- |
| testConfig.forbidOnly | Whether to exit with an error if any tests are marked as `test.only`. Useful on CI. |
| testConfig.fullyParallel | have all tests in all files to run in parallel. See Parallelism and Sharding for more details. |
| testConfig.projects | Run tests in multiple configurations or on multiple browsers |
| testConfig.reporter | Reporter to use. See Test Reporters to learn more about which reporters are available. |
| testConfig.retries | The maximum number of retry attempts per test. See Test Retries to learn more about retries. |
| testConfig.testDir | Directory with the test files. |
| testConfig.use | Options with `use{}` |
| testConfig.webServer | To launch a server during the tests, use the `webServer` option |
| testConfig.workers | The maximum number of concurrent worker processes to use for parallelizing tests. Can also be set as percentage of logical CPU cores, e.g. `'50%'.`. See Parallelism and Sharding for more details. |

## Filtering Tests

Filter tests by glob patterns or regular expressions.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';export default defineConfig({  // Glob patterns or regular expressions to ignore test files.  testIgnore: '*test-assets',  // Glob patterns or regular expressions that match test files.  testMatch: '*todo-tests/*.spec.ts',});
```

| Option | Description |
| --- | --- |
| testConfig.testIgnore | Glob patterns or regular expressions that should be ignored when looking for the test files. For example, `'*test-assets'` |
| testConfig.testMatch | Glob patterns or regular expressions that match test files. For example, `'*todo-tests/*.spec.ts'`. By default, Playwright runs `.*(test|spec).(js|ts|mjs)` files. |

## Advanced Configuration

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';export default defineConfig({  // Folder for test artifacts such as screenshots, videos, traces, etc.  outputDir: 'test-results',  // path to the global setup files.  globalSetup: require.resolve('./global-setup'),  // path to the global teardown files.  globalTeardown: require.resolve('./global-teardown'),  // Each test is given 30 seconds.  timeout: 30000,});
```

| Option | Description |
| --- | --- |
| testConfig.globalSetup | Path to the global setup file. This file will be required and run before all the tests. It must export a single function. |
| testConfig.globalTeardown | Path to the global teardown file. This file will be required and run after all the tests. It must export a single function. |
| testConfig.outputDir | Folder for test artifacts such as screenshots, videos, traces, etc. |
| testConfig.timeout | Playwright enforces a timeout for each test, 30 seconds by default. Time spent by the test function, test fixtures and beforeEach hooks is included in the test timeout. |

## Expect Options

Configuration for the expect assertion library.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';export default defineConfig({  expect: {    // Maximum time expect() should wait for the condition to be met.    timeout: 5000,    toHaveScreenshot: {      // An acceptable amount of pixels that could be different, unset by default.      maxDiffPixels: 10,    },    toMatchSnapshot: {      // An acceptable ratio of pixels that are different to the      // total amount of pixels, between 0 and 1.      maxDiffPixelRatio: 0.1,    },  },});
```

| Option | Description |
| --- | --- |
| testConfig.expect | Web first assertions like `expect(locator).toHaveText()` have a separate timeout of 5 seconds by default. This is the maximum time the `expect()` should wait for the condition to be met. Learn more about test and expect timeouts and how to set them for a single test. |
| expect(page).toHaveScreenshot() | Configuration for the `expect(locator).toHaveScreenshot()` method. |
| expect(value).toMatchSnapshot() | Configuration for the `expect(locator).toMatchSnapshot()` method. |
