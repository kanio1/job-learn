# Advanced Playwright Authentication: A Multi-Role Fixture for Scalable E2E Testing

| Field | Value |
|-------|-------|
| **Author** | Faizan Shaikh |
| **Published (UI)** | Feb 14, 2026 |
| **Published (metadata)** | 2026-02-20T19:28:26.852Z |
| **Reading time** | 9 min read |
| **URL** | https://medium.com/@faizan4199/advanced-playwright-authentication-a-multi-role-fixture-for-scalable-e2e-testing-9ce89af14223 |
| **Tags** | Playwright, Typescript, Software Testing, Web Development, DevOps |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "og:title": "Advanced Playwright Authentication: A Multi-Role Fixture for Scalable E2E Testing",
  "author": "Faizan Shaikh",
  "article:author": "https://medium.com/@faizan4199",
  "article:published_time": "2026-02-20T19:28:26.852Z",
  "publishedTime": "2026-02-20T19:28:26.852Z",
  "og:description": "Stop wasting CI minutes. Learn how to build a robust, worker-scoped authentication system for Playwright.",
  "twitter:description": "Stop wasting CI minutes. Learn how to build a robust, worker-scoped authentication system for Playwright.",
  "og:image": "https://miro.medium.com/v2/resize:fit:1200/1*uvvrNfm_K5T0sY6FuCLD0A.png",
  "og:type": "article",
  "og:url": "https://medium.com/@faizan4199/advanced-playwright-authentication-a-multi-role-fixture-for-scalable-e2e-testing-9ce89af14223",
  "language": "en",
  "twitter:data1": "9 min read",
  "statusCode": 200,
  "sourceURL": "https://medium.com/@faizan4199/advanced-playwright-authentication-a-multi-role-fixture-for-scalable-e2e-testing-9ce89af14223"
}
```

---

When it comes to End-to-End (E2E) testing, **authentication** is usually the first major hurdle you hit. If you’re still logging in manually in every `beforeEach` block, you’re burning through CI minutes (and your own patience) for no reason.

Playwright revolutionized this with the concept of **Storage State** — capturing cookies and local storage to “skip” the login UI. It’s fast, it’s efficient, and it saves you a fortune on execution costs.

If you’ve been using Playwright for a while, you know that the “Global Setup” approach for authentication is great…until it isn’t.

Once your suite grows to handle multiple user roles (Admin, Editor, Viewer), session expirations, and parallel execution, the standard setup starts to feel a bit brittle. You might run into race conditions where two workers try to log in as the same user at the same time, or your tests fail because a cached session expired mid-run.

I wanted a more robust way to handle this. I needed a solution that was **atomic**, **role-aware**, and **self-healing**.

In this post, I’ll share a custom Playwright fixture that handles authentication like a pro.

## The Problem with Basic Auth Setup

Most tutorials suggest a simple `global-setup.ts` that saves a `storageState.json`. This works for one user, but:

1. **Parallelism Issues:** Multiple workers might try to write to the same state file simultaneously.
2. **Session Decay:** If your test suite runs for 30 minutes but your token lasts 15, the later tests will fail.
3. **Role Complexity:** Switching between an “Admin” and a “Customer” requires manual logic in every test.

## The Solution: An Advanced Auth Fixture

Instead of a global script, we can extend Playwright’s `test` object. This allows us to inject authentication directly into the `page` and `context` fixtures.

Here is the “Secret Sauce” — a comprehensive `auth-test.fixture.ts` that handles locking, validation, and multi-role state management.

```ts
import { test as base } from "@playwright/test";
import fs from "fs/promises";
import path from "path";
import type { Page, Browser, BrowserContext } from "@playwright/test";

export type ValidateFn = (params: {
  browser: Browser;
  context: BrowserContext;
  page: Page;
  role: string;
}) => Promise<boolean>;

export type Authenticator = (params: AuthenticatorParams) => Promise<void>;

export type AuthenticatorParams = {
  browser: Browser;
  context: BrowserContext;
  page: Page;
  role: string;
  extraLoginData?: any;
};

export type AuthOptions = {
  role: string;
  refreshSession: boolean;
  sessionMaxAge?: number;
  extraLoginData?: any;
  validateFn?: ValidateFn;
  authenticator?: Authenticator;
  authDir?: string;
  postLoginUrl?: string;
  lockStaleTTL?: number;
  authSetup: void;
};

export const test = base.extend<{}, AuthOptions>({
  role: ["default", { scope: "worker" }],
  refreshSession: [false, { scope: "worker" }],
  sessionMaxAge: [undefined, { scope: "worker" }],
  extraLoginData: [undefined, { scope: "worker" }],
  validateFn: [undefined, { scope: "worker" }],
  authenticator: [undefined, { scope: "worker" }],
  authDir: [undefined, { scope: "worker" }],
  postLoginUrl: [undefined, { scope: "worker" }],
  lockStaleTTL: [5, { scope: "worker" }],

  authSetup: [
    async (
      {
        browser,
        role,
        refreshSession,
        sessionMaxAge,
        extraLoginData,
        validateFn,
        authenticator: authenticatorOption,
        authDir: authDirOption,
        postLoginUrl: postLoginUrlOption,
        lockStaleTTL: lockStaleTTLOption,
      },
      use,
    ) => {
      const rolePrefix = role !== "default" ? `${role}-` : "";
      const baseAuthDir = authDirOption ?? "./.auth";
      const authDir = path.resolve(baseAuthDir);
      const authFile = path.join(authDir, `${rolePrefix}state.json`);
      const lockFile = path.join(authDir, `${rolePrefix}.lock`);

      await fs.mkdir(authDir, { recursive: true });

      let needsRefresh = refreshSession;

      if (!needsRefresh) {
        try {
          await fs.access(authFile);
          if (sessionMaxAge !== undefined) {
            const stat = await fs.stat(authFile);
            const age = (Date.now() - stat.mtimeMs) / 60000;
            if (age > sessionMaxAge) {
              needsRefresh = true;
            }
          }
        } catch {
          needsRefresh = true;
        }
      }

      const acquireLock = async () => {
        const ttlMinutes =
          typeof lockStaleTTLOption === "number"
            ? lockStaleTTLOption
            : undefined;

        while (true) {
          try {
            const fd = await fs.open(lockFile, "wx");
            await fd.close();
            return;
          } catch {
            try {
              const stat = await fs.stat(lockFile);
              if (ttlMinutes !== undefined) {
                const ageMinutes = (Date.now() - stat.mtimeMs) / 60000;
                if (ageMinutes > ttlMinutes) {
                  await fs.rm(lockFile, { force: true });
                  continue;
                }
              }
            } catch {
              // ignore
            }
            await new Promise((r) => setTimeout(r, 400));
          }
        }
      };

      const releaseLock = async () => {
        await fs.rm(lockFile, { force: true });
      };

      const authenticator: Authenticator =
        authenticatorOption ?? (await import("../pages/auth")).appLogin;

      if (needsRefresh) {
        await acquireLock();
        try {
          const exists = await fs
            .access(authFile)
            .then(() => true)
            .catch(() => false);

          if (!exists || refreshSession) {
            const context = await browser.newContext();
            const page = await context.newPage();

            await authenticator({
              browser,
              context,
              page,
              role,
              extraLoginData,
            });

            await context.storageState({ path: authFile });
            await page.close();
            await context.close();
          }
        } finally {
          await releaseLock();
        }
      }

      if (validateFn) {
        const context = await browser.newContext({ storageState: authFile });
        const page = await context.newPage();

        let ok = await validateFn({ browser, context, page, role });

        await page.close();
        await context.close();

        if (!ok) {
          await fs.rm(authFile, { force: true });

          await acquireLock();
          try {
            const context2 = await browser.newContext();
            const page2 = await context2.newPage();

            await authenticator({
              browser,
              context: context2,
              page: page2,
              role,
              extraLoginData,
            });

            await context2.storageState({ path: authFile });
            await page2.close();
            await context2.close();
          } finally {
            await releaseLock();
          }

          const context3 = await browser.newContext({ storageState: authFile });
          const page3 = await context3.newPage();

          ok = await validateFn({
            browser,
            context: context3,
            page: page3,
            role,
          });

          await page3.close();
          await context3.close();

          if (!ok) {
            await fs.rm(authFile, { force: true });
            throw new Error("Auth validation failed after re-login attempt");
          }
        }
      }

      await use();
    },
    { scope: "worker" },
  ],

  context: async (
    { browser, role, authDir: authDirOption, authSetup },
    use,
  ) => {
    const rolePrefix = role !== "default" ? `${role}-` : "";
    const baseAuthDir = authDirOption ?? "./.auth";
    const authDir = path.resolve(baseAuthDir);
    const authFile = path.join(authDir, `${rolePrefix}state.json`);

    const exists = await fs
      .access(authFile)
      .then(() => true)
      .catch(() => false);

    if (exists) {
      console.log(`[Auth] Loading storageState from: ${authFile}`);
      const context = await browser.newContext({ storageState: authFile });
      await use(context);
      await context.close();
    } else {
      console.log(
        `[Auth] No storageState found at: ${authFile}. Creating fresh context.`,
      );
      const context = await browser.newContext();
      await use(context);
      await context.close();
    }
  },

  page: async (
    { context, postLoginUrl: postLoginUrlOption, authSetup },
    use,
  ) => {
    const page = await context.newPage();

    if (postLoginUrlOption) {
      console.log(`[Auth] Navigating to postLoginUrl: ${postLoginUrlOption}`);
      try {
        const response = await page.goto(postLoginUrlOption, {
          waitUntil: "domcontentloaded",
          timeout: 30000,
        });

        if (!response || !response.ok()) {
          console.warn(
            `[Auth] Navigation returned status ${response?.status() || "unknown"}. Page may not be fully loaded.`,
          );
        } else {
          console.log(`[Auth] Successfully navigated to postLoginUrl`);
        }
      } catch (error) {
        console.error(
          `[Auth] Failed to navigate to postLoginUrl: ${error instanceof Error ? error.message : String(error)}`,
        );
      }
    }

    await use(page);
    await page.close();
  },
});
```

## Why this is a game changer

### 1. The “Auto-Magic” Default

One of the best features of this fixture is the default authenticator logic. If you don’t explicitly pass an `authenticator` function, the fixture looks for one at: `../pages/auth.ts` (specifically a function named `appLogin`).

### 2. Atomic Locking

When running tests in parallel with 10 workers, you don’t want 10 browsers trying to log in as “Admin” simultaneously. It usually triggers rate limits or “too many sessions” errors. The `acquireLock` function creates a `.lock` file. Other workers will wait until the first worker finishes the login and saves the state.

### 3. The “Self-Healing” Validation

Sometimes a session is “valid” on disk (the file exists) but “invalid” on the server (the token was revoked). By passing a `validateFn`, the fixture will actually check if the user is logged in (e.g., checking for a profile icon). If it fails, it automatically clears the cache and logs in again.

### 4. Native Role Support

You can define multiple roles in your config or your test file:

```ts
test.use({ role: 'admin' });

test('should delete user', async ({ page }) => {
  // Open post login page it, will open it without login flow
});
```

### 4. Time-to-Live (TTL)

The `sessionMaxAge` option allows you to force a fresh login every X minutes. This is perfect for environments where tokens rotate frequently.

## How to Use It: Practical Examples

To get this running, you first need to define your login logic, then consume it in your tests.

### 1. Define your Authenticator (`pages/auth.ts`)

This is where the actual UI interaction happens. The fixture will call this if the session is missing or expired.

```ts
// pages/auth.ts
import { AuthenticatorParams } from "../fixtures/auth-test.fixture";

export const myLogin = async ({ page, role }: AuthenticatorParams) => {
  await page.goto('https://myapp.com/login');

  // You can use the 'role' to pick different credentials
  const username = role === 'admin' ? 'admin_user' : 'standard_user';

  await page.fill('#username', username);
  await page.fill('#password', process.env.USER_PASSWORD!);
  await page.click('#login-button');

  // Wait for a successful login indicator
  await page.waitForURL('**/dashboard');
};
```

### 2. Basic Usage in a Test

In your test file, you simply use the extended `test` object. The fixture handles the rest: it checks for a saved state, checks for a lock, and logs in if necessary.

```ts
import { test } from "../fixtures/auth-test.fixture";

// Use the 'admin' role for all tests in this file
test.use({ role: 'admin' });
test('Admin can access the management panel', async ({ page }) => {
  await page.goto('/admin-settings');
  // No login code needed! The page is already authenticated.
});
```

### 3. Advanced: Self-Healing with `validateFn`

Sometimes a cookie exists, but the session is actually revoked on the server. The `validateFn` allows the fixture to "verify" the session before starting the test. If it returns `false`, the fixture automatically deletes the stale state and re-authenticates.

```ts
import { test } from "../fixtures/auth-test.fixture";

test.use({
  role: 'editor',
  validateFn: async ({ page }) => {
    await page.goto('/api/session-check');
    const content = await page.textContent('body');
    return content?.includes('"authenticated":true') ?? false;
  }
});
test('Editor can save drafts', async ({ page }) => {
  await page.goto('/editor');
  // ... test logic
});
```

## Why This Implementation Scales

### 1. Race Condition Prevention

The `acquireLock` function is the hero here. It uses a `.lock` file to ensure that if 10 workers start at once, only **Worker 1** performs the login. Workers 2 through 10 will "wait" for the lock to be released and then simply consume the `state.json` created by the first worker.

### 2. Session Freshness

By setting `sessionMaxAge`, you can ensure that long-running CI jobs don't fail due to token expiry. If the `state.json` file on disk is older than your limit, the fixture ignores it and triggers a fresh login.

### 3. Worker Scoping

By scoping the `authSetup` to the **worker**, we ensure that authentication logic runs exactly once per worker-role combination, rather than once per test. This provides a massive speed boost to your suite.

## Understanding the Risks

While this advanced setup provides immense stability and speed, it introduces specific security and operational risks that you must manage.

### 1. Secret Leakage via Storage State

The fixture stores session data (cookies, local storage, and potentially JWTs) at `.auth/{role}-state.json`.

- **The Danger:** These files contain **active, unencrypted credentials**. If you accidentally commit the `.auth` directory to your Git repository, anyone with access to your code can impersonate your test users.
- **Mitigation:** **Must** add `.auth/` to your `.gitignore` file immediately.

### 2. CI/CD Artifact Vulnerability

Many CI/CD pipelines (like GitHub Actions or GitLab CI) are configured to save test results or trace files as artifacts for debugging.

- **The Danger:** If your logs or artifacts include the `.auth` folder, those session tokens will be stored on your CI provider's servers.
- **Mitigation:** Ensure your artifact upload paths explicitly exclude the `.auth` directory.

### 3. Cross-Worker Contamination

Since we use worker-scoped fixtures to speed up execution, multiple tests share the same `state.json`.

- **The Danger:** If a test in “Worker A” performs a “Logout” action or changes a password, it will invalidate the session for every subsequent test assigned to that worker-role combination.
- **Mitigation:** Avoid “Logout” or “Account Setting” changes in tests that rely on shared authentication state. Use a dedicated, non-shared role for those specific tests.

### 4. Stale Lock Files

If a test process crashes hard (e.g., a SIGKILL or a power failure), a `.lock` file might remain on disk.

- **The Danger:** Subsequent runs might think another worker is still logging in and hang indefinitely.
- **Mitigation:** The fixture includes a `lockStaleTTL` (default 5 mins) to auto-break old locks, but you should still ensure your CI environment starts with a clean workspace.

## Final Thoughts

End-to-end testing is only as valuable as it is reliable. By moving your authentication logic into a smart, worker-scoped fixture, you eliminate the “flaky login” syndrome and significantly speed up your suite. You gain a self-healing architecture that handles multiple roles with ease, letting you focus on what really matters: testing your app’s features.

Happy coding ! :)
