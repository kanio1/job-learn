---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Anton Gulin | Playwright Best Practices: 14 Rules AI Agents Get Wrong (2026) | https://www.anton.qa/blog/posts/playwright-best-practices | complete |
| 2 | Anton Gulin | Should Page Objects Assert? Where Test Assertions Belong | https://www.anton.qa/blog/posts/where-test-assertions-belong | complete |
| 3 | Anton Gulin | The Modern Page Object Model: Less Shared Code, Easier Changes | https://www.anton.qa/blog/posts/modern-page-object-model | complete |
| 4 | Anton Gulin | Test Retries Hide Real Bugs | https://www.anton.qa/blog/posts/test-retries-hide-real-bugs | complete |
| 5 | Joseph Ward | Why Simple UI Tests Become Slow | https://josephward.tech/2026-06-30-why-simple-ui-tests-become-slow | complete |
| 6 | Joseph Ward | Looking Behind Playwright's Magic | https://josephward.tech/2026-07-07-looking-behind-playwrights-magic-edited | complete |
| 7 | ScrollTest / Pramod Dutta | Day 6: Fixtures — Dependency Injection That Eliminates Boilerplate | https://scrolltest.com/21-day-playwright-day-6-fixtures-dependency-injection/ | complete |
| 8 | ScrollTest / Pramod Dutta | Day 2: Locator Strategies — Why getByRole Wins | https://scrolltest.com/21-day-playwright-day-2-locator-strategies-getbyrole/ | complete |
| 9 | ScrollTest / Pramod Dutta | Day 3: Assertions That Actually Catch Bugs — expect() Deep Dive | https://scrolltest.com/21-day-playwright-day-3-assertions-expect-deep-dive/ | complete |
| 10 | ScrollTest / Pramod Dutta | Day 5: Page Object Model — Structure Tests That Scale | https://scrolltest.com/21-day-playwright-day-5-page-object-model-structure/ | complete |
| 11 | ScrollTest / Pramod Dutta | Day 13: Debugging — Trace Viewer, UI Mode, and Inspector | https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/ | complete |
| 12 | ScrollTest / Pramod Dutta | Day 19: Advanced Patterns — Retry, Tags, Parameterization, and Hooks | https://scrolltest.com/21-day-playwright-day-19-advanced-patterns-retry-tags/ | complete |
| 13 | ScrollTest / Pramod Dutta | Playwright Global Setup and Teardown with TypeScript | https://scrolltest.com/playwright-global-setup-teardown-typescript-day-58/ | complete |
| 14 | ScrollTest / Pramod Dutta | Playwright TypeScript Checklist | https://scrolltest.com/playwright-typescript-checklist/ | complete |
| 15 | Butch Mayhew / Playwright Solutions | Butch Mayhew author archive (page 1) | https://playwrightsolutions.com/author/butch/ | complete |
| 16 | Butch Mayhew / Playwright Solutions | Load a Custom Test Fixture or Setup Projects When Running Code Generator | https://playwrightsolutions.com/how-to-load-a-custom-test-fixture-or-setup-projects-when-running-playwright-test-code-generator/ | complete |
| 17 | Butch Mayhew / Playwright Solutions | How to Run a Specific Spec File Sequentially | https://playwrightsolutions.com/how-to-run-a-specific-spec-file-playwright-tests-sequentially/ | complete |
| 18 | Butch Mayhew / Playwright Solutions | Trace Viewer — Copy as Playwright API Request | https://playwrightsolutions.com/tip-playwright-copy-as-playwright-api-request-button/ | complete |
| 19 | Butch Mayhew / Playwright Solutions | Combine Playwright HTML Reports | https://playwrightsolutions.com/how-to-combine-playwright-html-reports-after-running-multiple-playwright-commands/ | complete |
| 20 | Butch Mayhew / Playwright Solutions | A Few Thoughts on Flaky Tests | https://playwrightsolutions.com/a-few-thoughts-on-flakey-tests-playwright-solutions/ | complete |
| 21 | Currents.dev | Component testing | https://currents.dev/posts/playwright-component-testing | complete |
| 22 | Currents.dev | Measure code coverage | https://currents.dev/posts/how-to-measure-code-coverage-in-playwright-tests | complete |
| 23 | Currents.dev | Selenium → Playwright migration | https://currents.dev/posts/migrating-from-selenium-to-playwright-the-complete-guide | complete |
| 24 | Sajith Dilshan | From chaos to control — decoupling logic from configuration | https://medium.com/@sajith-dilshan/%EF%B8%8F-from-chaos-to-control-a-senior-qa-engineers-guide-to-decoupling-logic-from-configuration-in-3e5ffba7291f | complete |
| 25 | TestDino | 17 best practices | https://testdino.com/blog/playwright-best-practices | complete |
| 26 | TestDino | Timeout guide | https://testdino.com/blog/playwright-timeout | complete |
| 27 | idavidov.eu (ArchQA) | Playwright test architecture — Stefan Minchev interview | https://idavidov.eu/playwright-test-architecture-stefan-minchev | complete |

## Artykuły

### 1. Anton Gulin — Playwright Best Practices: 14 Rules AI Agents Get Wrong (2026)

- Source: https://www.anton.qa/blog/posts/playwright-best-practices
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Best Practices: 14 Rules AI Agents Get Wrong (2026)


Published: June 17, 2026·8 min read

14 Playwright best practices for stable tests in 2026: locators, auth reuse, API testing, page objects, CI and traces. Plus the ones AI code agents get wrong.

![10 Playwright best practices AI agents get wrong](https://cdn.sanity.io/images/9tez6xmw/production/09531a845b65d643a9f1db6472ba35969f74466b-1200x630.png?w=1200&h=630&fm=webp&q=80)

#### Playwright Best Practices: 14 Rules AI Agents Get Wrong (2026)

**Playwright best practices** are the rules that keep browser tests stable and easy to read. Use role-based locators (find by what users see), web-first assertions that auto-wait, and isolated tests. Seed data through the API (direct requests), not the UI. Avoid hard waits, conditional logic, and tests tied to your HTML. Turn on traces and run in parallel.

An AI agent can write 50 Playwright tests in a minute. That feels fast.

Then those tests fail at random, and nobody knows why. The agent copied old patterns from its training data. It does not know the run failed last night.

This guide lists the 14 best practices that keep tests stable. For each one, I show a small correct example. I also show what AI code agents get wrong. AI tools like Copilot, Cursor, and even Playwright codegen (the test recorder) lean on stale habits. Someone has to fix that.

* * *

##### 1\. Find elements the way a user sees them

A locator (a pointer to an element) should match what a person sees on screen. Use `getByRole`, `getByLabel`, or `getByText`. These read like the page. They also survive a redesign of your HTML.

```typescript
import { test, expect } from '@playwright/test';

test('user can sign in', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('ada@example.com');
  await page.getByRole('button', { name: 'Sign in' }).click();
});
```

**What AI agents get wrong:** they reach for CSS or XPath (brittle path selectors) like `page.locator('div.btn-primary > span')`. Change one class name and the test breaks.

* * *

##### 2\. Use web-first assertions that wait for you

A web-first assertion (a check that auto-waits) retries until the page is ready. `expect(locator).toBeVisible()` waits on its own. You never add a fixed sleep.

```typescript
import { test, expect } from '@playwright/test';

test('welcome message appears', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page.getByText('Welcome back')).toBeVisible();
});
```

**What AI agents get wrong:** they add `await page.waitForTimeout(3000)` (a hard pause). Hard waits are the top cause of flaky tests (tests that fail at random). Too short, the test fails. Too long, the suite crawls.

* * *

##### 3\. Keep every test isolated

Isolated means each test starts clean. No shared login. No leftover data from the test before. Playwright gives each test a fresh browser context (a clean session). Set up state in a hook, not across tests.

```typescript
import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('ada@example.com');
  await page.getByRole('button', { name: 'Sign in' }).click();
});

test('shows the account name', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Ada Lovelace' })).toBeVisible();
});
```

**What AI agents get wrong:** they chain tests, where test 2 needs test 1 to run first. One failure then breaks the whole file.

* * *

##### 4\. Seed state through the API, not the UI

To test a page, you often need data first. A user, an order, a draft. Do not click through ten screens to make it. Send the data straight to your backend with the `request` fixture (a built-in HTTP client). It is faster and steadier.

```typescript
import { test, expect } from '@playwright/test';

test('opens an existing project', async ({ page, request }) => {
  const res = await request.post('/api/projects', {
    data: { name: 'Apollo' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/projects');
  await expect(page.getByText('Apollo')).toBeVisible();
});
```

**What AI agents get wrong:** they build the data through the UI every time. The test gets long and slow, and a setup step fails for reasons that have nothing to do with the real check.

* * *

##### 5\. Do not lean on test IDs by default

A test ID (a tag added just for tests, like `data-testid`) works as a fallback. But reach for `getByRole` and `getByLabel` first. Those test what a real user can do. A test ID only proves an attribute exists.

```typescript
import { test, expect } from '@playwright/test';

test('cart shows one item', async ({ page }) => {
  await page.goto('/cart');
  // Prefer a real role over a test id.
  await expect(page.getByRole('listitem')).toHaveCount(1);
});
```

**What AI agents get wrong:** they paste `data-testid` on everything. The tests pass even when the button has no label and a screen reader (assistive software) cannot find it. The test misses a real bug.

* * *

##### 6\. Turn on traces for the first retry

A trace (a full recording of the run) shows every step, the DOM, and the network. Set it to record only on the first retry of a failed test. You get the evidence for failures, and clean runs stay fast.

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  retries: 1,
  use: {
    trace: 'on-first-retry',
  },
});
```

**What AI agents get wrong:** they leave tracing off, or set `trace: 'on'` for every run. Off means no evidence when a test fails. Always-on slows the suite and fills your disk.

* * *

##### 7\. Run tests in parallel and shard them

Parallel means many tests run at once. Playwright does this by default. For one big file of independent tests, set parallel mode. To split a slow suite across machines, use sharding (run a slice per machine).

```typescript
import { test, expect } from '@playwright/test';

test.describe.configure({ mode: 'parallel' });

test('loads home', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Home/);
});
```

Split across three machines on CI (your build server):

```bash
npx playwright test --shard=1/3
```

**What AI agents get wrong:** they write tests that share a database row or a single user. Run those in parallel and they fight each other, so you get flaky tests.

* * *

##### 8\. Keep `if` and `try` out of your tests

A test should walk one clear path. No branching. If a test asks "is the button there? if so click it," it hides a bug. The button should always be there. Assert it.

```typescript
import { test, expect } from '@playwright/test';

test('checkout button works', async ({ page }) => {
  await page.goto('/cart');
  // Assert the state. Do not guess it with an if.
  const checkout = page.getByRole('button', { name: 'Checkout' });
  await expect(checkout).toBeEnabled();
  await checkout.click();
});
```

**What AI agents get wrong:** they wrap clicks in `if (await locator.isVisible())` to stop errors. That hides the real failure. A test that skips its own check still goes green.

* * *

##### 9\. Test what users see, not how it is built

Test the behavior, not the internals. Check the visible result. Do not check a CSS class, a state variable, or a function name. Those change when you refactor (rewrite the code), even though the app still works.

```typescript
import { test, expect } from '@playwright/test';

test('shows a success message after submit', async ({ page }) => {
  await page.goto('/contact');
  await page.getByLabel('Message').fill('Hello');
  await page.getByRole('button', { name: 'Send' }).click();
  // Check the user-facing result, not an internal class.
  await expect(page.getByText('Thanks, we got your message')).toBeVisible();
});
```

**What AI agents get wrong:** they assert on `class="is-active"` or an exact HTML shape. The test breaks on every redesign, even when nothing real changed.

* * *

##### 10\. Define projects in your config

A project (a named test setup) in `playwright.config.ts` runs the same tests under different settings. Use projects to cover Chromium, Firefox, and WebKit (the three main browser engines). One config, full coverage.

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  retries: 1,
  use: { trace: 'on-first-retry' },
  projects: [\
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },\
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },\
  ],
});
```

**What AI agents get wrong:** they hard-code one browser, or copy a config with no `projects` array. The suite then tests Chrome only, and a Safari-only bug ships to users.

* * *

##### 11\. Log in once, then reuse the session

Signing in through the form in every test is slow. It also makes every test depend on the login page.

Log in one time. Save the session to a file with `storageState` (the saved cookies and local storage). Every later test starts already signed in.

```typescript
// auth.setup.ts: runs once, before the suite
import { test as setup } from '@playwright/test';

setup('sign in once', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('ada@example.com');
  await page.getByLabel('Password').fill(process.env.TEST_PASSWORD!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await page.context().storageState({ path: 'playwright/.auth/user.json' });
});
```

Point your project at that file, and the tests open already signed in.

**What AI agents get wrong:** they write the full login into every single test. Fifty tests then run the same five steps fifty times. One change to the login form breaks all fifty at once.

* * *

##### 12\. Test the API directly when the screen is not the point

Not every check needs a browser. If you are testing what the server returns, ask the server.

Playwright ships a request client. It is faster than a page, and it does not break when the design changes.

```typescript
import { test, expect } from '@playwright/test';

test('the API rejects a bad email', async ({ request }) => {
  const res = await request.post('/api/signup', {
    data: { email: 'not-an-email', password: 'hunter2hunter2' },
  });
  expect(res.status()).toBe(400);
  expect(await res.json()).toMatchObject({ field: 'email' });
});
```

**What AI agents get wrong:** they drive the whole thing through the browser. The agent fills a form and reads an error message to check a rule the server already enforces. That test is ten times slower and fails whenever the wording changes.

* * *

##### 13\. Keep page objects thin

A page object is a class that holds the locators for one screen. It stops you from repeating the same selector in twenty files.

Keep it to locators and simple actions. Do not put your checks inside it. The test itself should say what is true. A reader then sees the point without opening another file.

```typescript
export class LoginPage {
  constructor(private page: Page) {}
  email = () => this.page.getByLabel('Email');
  submit = () => this.page.getByRole('button', { name: 'Sign in' });
  async signIn(email: string, password: string) { /* fill and click */ }
}
```

**What AI agents get wrong:** they bury assertions inside the page object. The test then reads `await loginPage.verifyEverythingWorks()` and tells you nothing. When it fails, you cannot see what was expected.

* * *

##### 14\. Pin the Playwright version and upgrade on purpose

Playwright ships often. A floating version means the browser under your tests can change overnight, without you asking.

Pin an exact version in `package.json`. Upgrade when you choose, and run the suite as part of that upgrade.

```json
{ "devDependencies": { "@playwright/test": "1.62.1" } }
```

**What AI agents get wrong:** they write `"@playwright/test": "^1.62.1"`. The caret allows any later 1.x. A green suite on Monday can go red on Friday because a browser updated, and nothing in your code changed.

* * *

##### The human still owns the standard

AI writes the first draft fast. That part is real, and it is useful. But the first draft copies patterns from old code on the internet. It adds hard waits. It clicks through the UI to seed data. It wraps fragile steps in `if` blocks so the run stays green.

A green suite that proves nothing is worse than no suite. It buys false trust.

So the workflow is simple. Let the agent write the draft. Then a human reads it against these 14 rules and fixes what the agent got wrong. The agent moves fast. The human keeps the tests honest. That is the job of an AI QA Architect.

Build the tests with AI. Then make them stable yourself.

* * *

_Anton Gulin is the AI QA Architect, the first person to claim this title on LinkedIn. He builds AI-powered test automation systems where AI agents and human engineers collaborate on quality. Former Apple SDET (Apple.com / Apple Card pre-release testing). Find him at [anton.qa](https://anton.qa/) or on [LinkedIn](https://linkedin.com/in/antongulin)._

* * *

Playwright · Best Practices · Test Automation · AI Testing · QA Architecture

### 2. Anton Gulin — Should Page Objects Assert? Where Test Assertions Belong

- Source: https://www.anton.qa/blog/posts/where-test-assertions-belong
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Should Page Objects Assert? Where Test Assertions Belong


Published: July 22, 2026·4 min read

Should page objects contain assertions? A practical rule: business checks live in tests, technical guards live in page objects. With Playwright code.

##### Post

Should a page object contain assertions? Here is the short answer: **business assertions belong in the test. Technical guards belong in the page object.** A test must show what "correct" means for its scenario. A page object must only promise that the page is ready to use. Mixing the two is why suites become hard to read and hard to trust.

Last week I published [the modern page object model](https://www.anton.qa/blog/posts/modern-page-object-model). One sign of legacy design was "page objects that assert." A senior QA engineer pushed back in the comments. His position: business checks go in the test layer, but technical checks — did the page reach the right state? — can hide inside the class.

It was a fair challenge. This post is the full answer.

##### The two kinds of checks

Every check in a UI test is one of two kinds.

**Business checks** answer: did the product do the right thing? The order total is $41.97. The welcome message names the user. The discount applied.

**Technical guards** answer: is the page ready? The form finished loading. The spinner went away. The URL changed.

They look similar in code. They serve different readers. A business check speaks to the person deciding "is this feature broken?" A technical guard speaks to the machine deciding "can I click now?"

##### The rule

Put business checks in the test, always:

```ts
test('applies the discount', async ({ checkoutPage }) => {
  await checkoutPage.applyCode('SAVE10');
  await expect(checkoutPage.total).toHaveText('$35.97');
});
```

The expected value sits in the test file. When this fails at 2 a.m., the reader sees what "correct" was supposed to be. No file jumping.

Handle technical guards inside the page object, but prefer waiting over asserting:

```ts
export class CheckoutPage {
  readonly total: Locator;

  async applyCode(code: string) {
    await this.codeInput.fill(code);
    await this.applyButton.click();
    await this.priceUpdate.waitFor({ state: 'visible' });
  }
}
```

The page object does not judge the total. It makes one promise: when `applyCode` returns, the page finished reacting. That is a technical guard, and note it is a _wait_, not an _assert_. Playwright's web-first assertions and auto-waiting handle most of these guards for free.

##### Why hidden business asserts hurt

Three costs show up at scale.

**The expected value disappears.** `checkoutPage.verifyTotal()` hides $35.97 in another file. The failing test cannot tell you what it believed.

**The page object takes sides.** Fifty tests share that class. One scenario needs a different expected total, and the shared method becomes a maze of parameters.

**Failures point at the wrong layer.** When an assert fires inside a page object, the stack trace blames plumbing. The reader has to dig to find which business rule broke.

##### Where it gets honestly debatable

My commenter's position (hidden technical assertions are fine) is workable. Plenty of strong suites do it. My preference is stricter for one reason: an assert stops the test with a verdict; a wait just holds the door. Verdicts belong to tests. But if your team hides technical guards as asserts and everyone can read the failures, that is a style choice, not a defect.


##### The migration path

Same as the POM modernization: no big rewrite. When a change touches a page object that asserts, move the business expectation up into the tests that call it, and convert the technical remainder into a wait. Each class takes minutes.

Run this to find your candidates:

```bash
grep -rn "expect(" src/pages/
```

Every hit is either a business check to promote or a guard to convert.

##### The takeaway

- Business checks: in the test, expected values written out.
- Technical guards: in the page object, as waits, not verdicts.
- Playwright's auto-waiting already covers most guards, delete before you migrate.

### 3. Anton Gulin — The Modern Page Object Model: Less Shared Code, Easier Changes

- Source: https://www.anton.qa/blog/posts/modern-page-object-model
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### The Modern Page Object Model: Less Shared Code, Easier Changes


Published: July 15, 2026·4 min read

What a page object model is, five outdated habits, and a simpler Playwright pattern with less shared code.

A **page object model (POM)** organizes browser tests by app page. Each page or component gets one class. The class stores element finders, called locators. It also stores user actions. Tests call those actions instead of using selectors directly. When a page changes, you fix one class. You do not fix two hundred tests.

The basic idea is old. Modern Playwright changes how we should build it.

Many page objects still follow Selenium tutorials from 2015. They work, but they ignore useful Playwright features. Here are five signs and the simpler pattern that replaces them.

##### Sign 1: Every page object extends `BasePage`

```ts
export class LoginPage extends BasePage {
  constructor(driver: Driver) { super(driver); }
}
```

Inheritance means one class receives behavior from a parent class. The 2015 logic made every page share one parent. That parent often becomes a junk drawer. It collects waits, logs, screenshots, and unused helpers.

Inheritance ties every page to the parent class. Modern page objects need less shared code. Shared helpers can live in plain functions. Test setup can live in fixtures, which prepare objects for each test.

##### Sign 2: Wait methods everywhere

```ts
await loginPage.waitForPageToLoad();
await loginPage.waitForSpinnerToDisappear();
```

Playwright checks elements before many user actions. For `click`, it checks visibility, stability, events, and enabled state. Many manual wait methods only repeat those checks. They can also hide timing bugs.

Wait explicitly only for conditions Playwright cannot infer. One example is a dashboard finishing a calculation. Use an assertion, which checks an expected result:

```ts
await expect(total).toHaveText('41.97');
```

##### Sign 3: Locators buried inside methods

```ts
async login(user: string, pass: string) {
  await this.page.locator('#username').fill(user);
  await this.page.locator('#password').fill(pass);
  await this.page.locator('button[type=submit]').click();
}
```

Hidden locators make page dependencies hard to see. Declare each locator once in the constructor:

```ts
export class LoginPage {
  readonly username: Locator;
  readonly password: Locator;
  readonly submit: Locator;

  constructor(readonly page: Page) {
    this.username = page.getByLabel('Username');
    this.password = page.getByLabel('Password');
    this.submit = page.getByRole('button', { name: 'Sign in' });
  }

  async login(user: string, pass: string) {
    await this.username.fill(user);
    await this.password.fill(pass);
    await this.submit.click();
  }
}
```

Now every locator is visible at a glance. The code also uses user-facing names through `getByRole` and `getByLabel`. Those names often survive redesigns. A CSS selector like `#username` may not.

##### Sign 4: Tests build their own page objects

```ts
test('login works', async ({ page }) => {
  const loginPage = new LoginPage(page);
});
```

This setup repeats in every test. A Playwright fixture prepares the page object once:

```ts
import { test as base } from '@playwright/test';
import { LoginPage } from './pages/login-page';

export const test = base.extend<{ loginPage: LoginPage }>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
});
```

```ts
import { test } from './fixtures';

test('login works', async ({ loginPage }) => {
  await loginPage.login('anton', 'secret');
});
```

The test asks for `loginPage` and receives a ready object. There is no repeated setup. One fixture can also depend on another fixture.

##### Sign 5: Page objects that assert

A `loginPage.verifyDashboardIsCorrect()` method lets the page object define "correct." The expected result now hides inside a shared class.

Modern split: **page objects act, tests assert.** The page object returns locators or values. The test states the expectation in plain sight:

```ts
await loginPage.login('anton', 'secret');
await expect(dashboard.greeting).toHaveText('Welcome, Anton');
```

When this fails, the expected value is visible in the test. You know what broke.

##### The migration path (no rewrite required)

1. New page objects follow the modern shape from day one.
2. When a test touches an old class, upgrade only that class.
3. Delete `extends BasePage` and move locators into the constructor.
4. Delete duplicate wait methods and move assertions into tests.
5. Add fixtures early. New tests get clean setup while old tests keep working.

##### Do AI agents change any of this?

They make this structure more important. Page objects give an AI agent a small list of approved actions. Raw selectors make the agent rediscover each page. That creates fragile tests. Calling `loginPage.login()` uses the same reviewed action as a human-written test.

Small, flat page objects give machines fewer ways to make mistakes. Large parent classes give them more choices.

##### The pattern in one line

Declare locators in the constructor, user-facing. Let fixtures do the wiring. Let auto-waiting do the waiting. Keep assertions in tests. Share nothing through inheritance.

Your page objects should be the most boring code you own. That is what makes them last another ten years.

### 4. Anton Gulin — Test Retries Hide Real Bugs

- Source: https://www.anton.qa/blog/posts/test-retries-hide-real-bugs
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Test Retries Hide Real Bugs: When a Rerun Helps and When It Lies


Published: August 05, 2026·3 min read

A test that fails, then passes on retry, is not fixed. Two readers explain why, with the race-condition case and a Playwright setup that treats retries as detection.

##### Post

Your build is green. One of your tests failed five minutes ago. Both of those things are true, because the test passed on a retry. Here is the short answer. **A retry never fixes anything.** Keep retries on in CI, but treat a retried pass as a bug report. It opens a ticket. It never closes one.

Last Saturday I posted about rerunning failed tests in isolation. Two readers pushed back, and both were more right than my post.

The first said flakiness is a signal. Turning the alarm off does not put out the fire.

The second was more specific. A team reruns the failed test alone. It passes. Everyone moves on. And the real cause, a race condition, ships to production. A race condition means two things run at the same time and collide.

This post is the concession, and the setup I now recommend.

##### What the rerun actually changed

Look at what happens when a test fails in CI and someone reruns it alone.

The code did not change. The data did not change. One thing changed: the test ran without the other tests around it.

So the rerun did not prove the test is fine. It proved the test is fine **when nothing else is running**. That is a different sentence. Your users do not visit your app one at a time.

##### The race that a rerun hides

Here is the simplest version of the trap:

```ts
test('admin can rename a user', async () => {
  await renameUser('user-42', 'New Name');
  await expect(profileName).toHaveText('New Name');
});

test('report shows user names', async () => {
  const report = await openReport();
  await expect(report.row('user-42')).toContainText('New Name');
});
```

Both tests touch the same record. Run together, they sometimes collide, and one fails. Run alone, each passes every time.

The failing run was the only honest one. It was telling you the app has a timing bug. The rerun in isolation deleted the evidence.

##### Playwright already tells you the truth

Here is the part most teams never look at. When a Playwright test fails and then passes on a retry, the report does not say "passed."

It says **flaky**.

```
  3 passed
  1 flaky
```

The tool is honest. The habit is not. Most dashboards only show green or red, so a flaky pass reads as a pass, and the count nobody reads keeps growing.

##### The setup that keeps retries useful

Retries have one honest job: telling infrastructure noise apart from real signal. A container that started slowly is noise. A race condition is signal. You cannot tell them apart without evidence, so collect it:

```ts
export default defineConfig({
  retries: process.env.CI ? 2 : 0,
  use: { trace: 'on-first-retry' },
});
```

Two lines, two jobs. The retry detects. The trace records everything about the failing run, so you can diagnose it later instead of shrugging.

Locally, retries stay at zero. On your own machine you want the failure loud and immediate.

##### The rule

A retried pass opens a ticket. It never closes one.

Once a week, read the flaky list. Every entry is one of two things. Infrastructure noise you should fix in the pipeline. Or a real timing bug in the app that your tests found first. Both are work. Neither is "passed."

My Saturday post said rerunning a flaky test in isolation makes things worse. These two readers explained the mechanism better than I did. The rerun does not just waste time. It manufactures false confidence and deletes the only evidence you had.

The green build is not the goal. The true build is.


### 5. Joseph Ward — Why Simple UI Tests Become Slow

- Source: https://josephward.tech/2026-06-30-why-simple-ui-tests-become-slow
- Retrieved: 2026-08-29
- Firecrawl status: complete

Sometimes, UI tests take longer than the code suggests they should.

A loop reads values from a table. A button takes two seconds to click. An interaction fails, is retried, and then passes.

When this happens, it is tempting to start with the code that looks busy. Rewrite the loop, change the filtering, etc.

I wanted to know whether that was the right place to look.

To do so, I created a small page and used Playwright with Python to measure a few common patterns: reading data from collections, waiting for controls and interacting with elements that are replaced while the page is rendering.

##### Reading a table

Let us imagine a table of orders. Each row contains an ID, status, age and value.

Our test needs to find the highest-value order that is awaiting approval and more than 30 days old.

One way to do that is to read every field into Python:

```python
rows = page.locator("[data-testid='order-row']")
orders = []

for index in range(rows.count()):
    row = rows.nth(index)

    orders.append(
        {
            "id": row.locator("[data-col='id']").inner_text(),
            "status": row.locator("[data-col='status']").inner_text(),
            "age": int(row.locator("[data-col='age']").inner_text()),
            "value": money_to_float(row.locator("[data-col='value']").inner_text()),
        }
    )

candidate = max(
    (order for order in orders
     if order["status"] == "Awaiting approval" and order["age"] >= 30),
    key=lambda order: order["value"],
    default=None,
)
```

Filtering 100 dictionaries is obviously pretty cheap. Retrieving the values is where the time probably goes.

Four fields across 100 rows means about 400 calls for text. The loop looks local, although most of its work involves round trips with the browser.

Playwright has bulk methods that reduce those calls:

```python
ids = page.locator("[data-col='id']").all_inner_texts()
statuses = page.locator("[data-col='status']").all_inner_texts()
ages = page.locator("[data-col='age']").all_inner_texts()
values = page.locator("[data-col='value']").all_inner_texts()
```

The four lists can then be combined and processed in Python.

This is a simple improvement and keeps JavaScript out of the test. It does assume that the rows remain unchanged between calls, of course. On a page that updates frequently, the values in the four lists could stop referring to the same rows.

Another option is to extract each row in one operation:

```python
orders = page.locator("[data-testid='order-row']").evaluate_all("""
    rows => rows.map(row => ({
        id: row.querySelector("[data-col='id']").textContent.trim(),
        status: row.querySelector("[data-col='status']").textContent.trim(),
        age: Number.parseInt(row.querySelector("[data-col='age']").textContent, 10),
        value: Number(row.querySelector("[data-col='value']").textContent.replace(/[^0-9.-]/g, ""))
    }))
    """)
```

The browser returns a list of row data and Python performs the filtering.

Then again, if the test only needs one result, the filtering can happen in the browser too:

```python
candidate = page.locator("[data-testid='order-row']").evaluate_all("""
    rows => {
        const candidates = rows
            .map(row => ({
                id: row.querySelector("[data-col='id']").textContent.trim(),
                status: row.querySelector("[data-col='status']").textContent.trim(),
                age: Number.parseInt(row.querySelector("[data-col='age']").textContent, 10),
                value: Number(row.querySelector("[data-col='value']").textContent.replace(/[^0-9.-]/g, ""))
            }))
            .filter(order => order.status === "Awaiting approval")
            .filter(order => order.age >= 30)
            .sort((left, right) => right.value - left.value);

        return candidates[0] ?? null;
    }
    """)
```

I measured all four approaches with 100 rows:

| Approach | Median |
| --- | --- |
| Read each value separately | 1,437.49 ms |
| Four bulk collection calls | 22.12 ms |
| Extract all rows in one call | 11.83 ms |
| Return the selected row | 5.32 ms |

As you can see, the largest change came from stopping the individual reads.

Using Playwright collection methods reduced the median from about 1.44 seconds to 22 milliseconds while leaving the filtering in Python.

One extraction call was quicker again. Returning a single result was the fastest version.

The figures do not mean every collection query should be written in JavaScript. They show that the way data is retrieved can matter way more than the code used to filter it.

For a small collection, I would just use ordinary locators. For a simple column, `all_inner_texts()` is probably enough. `evaluate_all()` becomes useful when several related values are needed from each row or when returning the full dataset serves no purpose.

##### Waiting

Fixed sleeps are another common source of time in test suites:

```python
time.sleep(2)

page.get_by_role("button", name="Approve").click()
```

A sleep added to fix an occasional failure is easy to understand. It also runs at full length whenever the test passes, which is unideal.

I tested a button that started visible but disabled, then became enabled after 200 milliseconds.

I compared three approaches:

```python
#### fixed sleep
time.sleep(0.5)
button.click()
```

```python
#### wait for visibility, then click
button.wait_for(state="visible")
button.click()
```

```python
#### playwright magic autowait
button.click()
```

The median results were:

| Approach | Median |
| --- | --- |
| Sleep for 500 ms, then click | 542.21 ms |
| Wait for visibility, then click | 249.42 ms |
| Let `click()` wait | 249.82 ms |

All three completed successfully in every run.

The fixed sleep spent roughly another 292 milliseconds waiting after the button could already be used.

The visibility wait made almost no difference compared with calling `click()` directly. Strictly speaking, though, the button had been visible from the start. Its disabled state was preventing the action.

This is an easy mistake to make in test code. It is the sort of thing that is easy to miss because the wait itself succeeds. But a condition can be true and still be irrelevant to whatever the test is trying to do.

An explicit wait for the correct condition would probably be more meaningful:

```python
expect(button).to_be_enabled()
button.click()
```

I would expect that to finish in roughly the same time as `button.click()` alone, with a small amount of assertion overhead. It makes sense when the enabled state is itself something the test wants to check. It depends on whether the enabled state is part of whatever is under test or just a prerequisite for the action you are interested in.

If that element state is not part of the test then this sort of thing just repeats a condition Playwright already considers before clicking.

All this to say, there is still a place for application-specific waits despite the magic.

Playwright can tell whether the browser considers an element visible, stable, enabled and able to receive events. It cannot know that an account balance has finished recalculating or that a background process has completed.

The useful distinction is between waiting for time to pass, waiting for a browser condition, and waiting for a testable condition.

##### Choosing what to change

These examples became faster for different reasons.

The table benefited from fewer browser calls. The button benefited from removing a fixed delay.

Browser-side filtering will not help a test littered with sleeps. Better waiting will not help a loop that retrieves thousands of values one by one.

The original loop looked like the obvious thing to optimise, but it turned out to be doing very little computation. Most of its time was spent asking the browser hundreds of small questions.

### 6. Joseph Ward — Looking Behind Playwright's Magic

- Source: https://josephward.tech/2026-07-07-looking-behind-playwrights-magic-edited
- Retrieved: 2026-08-29
- Firecrawl status: complete

* * *

A Playwright click looks simple:

```
await page.getByRole('button', { name: 'Submit' }).click();
```

Chromium, however, cannot click “the button named Submit”. It can only receive input at an x,y position on the page. Before Playwright can send that action, it resolves the locator, determines whether the element can be interacted with, scrolls it into view, calculates its coordinates, and checks that another element will not receive the click instead.

Even so, sending the action is only half the battle. The click may start a navigation, submit a form, open a new page, or trigger network requests. Playwright then needs to coordinate with what the browser does next.

In a [previous post](https://josephward.tech/2024-01-21-harmonising-selenium/), I used several approaches to observe Chrome DevTools Protocol (CDP) network events from Selenium. This time, I followed Playwright’s source to see how it turns those low-level browser signals into the higher-level behaviour behind a click.

There are two paths to examine:

1. how Playwright prepares and completes the action
2. how it observes and waits for the consequences.

This article follows the Chromium implementation, where Playwright talks to the browser using CDP. Firefox and WebKit provide the same high-level Playwright abstractions through different underlying code.

All Playwright source links are pinned to commit [`ad18048`](https://github.com/microsoft/playwright/commit/ad18048db947ca0a47c7fa59b77718e3a06afafe) from 1 July 2026.

##### It starts with a locator

The trail begins in:

```
packages/playwright-core/src/client/locator.ts
```

`Locator.click()` does not perform the click itself. It passes the locator’s selector to the frame and enables strict matching:

```
async click(options: channels.ElementHandleClickOptions & TimeoutOptions = {}): Promise<void> {
  return await this._frame.click(this._selector, { strict: true, ...options });
}
```

That is the implementation in [`locator.ts`, lines 113–115](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/client/locator.ts#L113-L115).

Those few lines reveal two useful design choices.

First, the locator holds a selector rather than an element fetched earlier. Playwright can resolve it against the current document when the action is attempted. That matters on pages where a rendering framework may remove one node and insert another that represents the same thing. Resolving at action time avoids treating an old element reference as though it were still current.

Second, `strict: true` means the action expects exactly one match. If the locator resolves to several buttons, Playwright reports an ambiguity instead of silently choosing the first.

I prefer that failure mode. A selector matching several `Submit` buttons is usually a problem in the test or the interface, not something I want the framework to conceal.

The call then crosses Playwright’s client/server boundary and eventually reaches the server-side element action code in:

```
packages/playwright-core/src/server/dom.ts
```

This is where most of the pre-click “magic” becomes explicit logic.

##### A click may need retrying

The internal click path passes the work into Playwright’s element interaction code. The eventual mouse action is wrapped by `_retryAction`, which contains a retry loop shared by element operations.

One of the first things it defines is a delay schedule:

```
// We progressively wait longer between retries, up to 500ms.
const waitTime = [0, 20, 100, 100, 500];
```

The schedule appears in [`dom.ts`, lines 318–319](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/dom.ts#L318-L319).

The loop attempts the action until it succeeds, encounters a non-retryable failure, or reaches the surrounding timeout. It handles results such as an element not being visible or lying outside the viewport. The branches are shown in [`dom.ts`, lines 297–314](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/dom.ts#L297-L314).

This gives us a more useful description of auto-waiting than saying Playwright somehow knows when a page is “ready”. It does not need a universal definition of readiness. Instead, it asks a narrower and more understandable question: can this requested action proceed now?

That distinction matters because many modern pages never become completely inactive. A page may keep a WebSocket open, poll for notifications, or continue loading secondary content after the main interface is usable. Waiting for the entire page to become abstractly “ready” would be both vague and, in some applications, impossible.

For a click, the practical question is whether this particular target can receive this particular action.

##### What Playwright checks before sending input

For a normal click, Playwright checks several element states, including:

```
visible
enabled
stable
```

The Playwright-side logic calls `injected.checkElementStates` against the target. That path can be followed in [`dom.ts`, lines 432–440](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/dom.ts#L432-L440).

Each state addresses a different failure mode.

A node being present in the DOM does not mean it has useful geometry. A disabled control may be visible but should not receive a normal user action. A button may be visible and enabled while still moving across the page because of an animation.

The stability check therefore observes the element across animation frames rather than adding the same fixed delay to every action.

This does not make races impossible. The application can always change after a check. Playwright is just reducing a common timing window, not making the browser immune to change.

Once those checks pass, Playwright scrolls the target into view. During retries, it can use different alignments rather than relying on one scroll position. The source comments explain why: a normal scroll may leave the element beneath an overlay, while another alignment may expose a usable point.

Playwright then calculates a x,y coordinate from the element’s geometry.

At this stage it has translated something semantic:

```
the button named Submit
```

into something the browser’s input system can use:

```
a point at x and y
```

It still has to establish that the point belongs to the intended element at the moment of interaction.

##### What `force` skips

Following this path also makes `force: true` less mysterious:

```
await locator.click({ force: true });
```

The forced path skips Playwright’s normal actionability logic : the visible, enabled, and stable checks above, along with the hit-target check described next. It proceeds towards dispatching the input without requiring the usual evidence that a user could perform the action.

I would read it as:

> Attempt the action without Playwright first establishing user actionability.

It does not make the click more realistic. In fact, it arguably removes checks intended to establish realism.

There are legitimate uses for this, but it can also hide the interesting problem. Perhaps a loading overlay never disappeared, the locator matched a hidden duplicate, or the control remained disabled.

The network side remains separate. If forced input triggers a navigation or network requests, Playwright can still observe those browser events. The question is whether the test still demonstrates an interaction available to a user.

##### Visibility is not targetabity

A visible element is not necessarily the element that would receive an action.

A loading overlay might be nearly transparent, leaving the button visible to a person while still intercepting the click. An element can therefore pass a visibility check without being the browser’s target.

Playwright uses injected code to check which element would receive the pointer event and to install a hit-target interceptor:

```
const handle = await progress.race(
  this._evaluateHandleInUtility(
    ([injected, node, { actionType, hitPoint, trial }]) =>
      injected.setupHitTargetInterceptor(node, actionType, hitPoint, trial),
    { actionType, hitPoint, trial: !!options.trial } as const
  )
);
```

This comes from [`dom.ts`, line 470](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/dom.ts#L470).

If another element owns the ‘point’, Playwright returns a hit-target description to the outer retry loop. That is the source of errors reporting that a particular overlay or container intercepts pointer events.

The result feeds back into the retry logic, which can decide whether to attempt the click again.

Frames make the calculation more involved because the point must be checked through the frame hierarchy. Playwright performs a separate frame hit-target step before installing the interceptor, but I will leave most of that path unexplored here.

##### Chromium receives a mouse event

Once the element checks, scrolling, and targetabity succeed, Playwright calls its mouse abstraction with the calculated point.

For Chromium, the final implementation is in:

```
packages/playwright-core/src/server/chromium/crInput.ts
```

`RawMouseImpl` sends the CDP command `Input.dispatchMouseEvent`.

A mouse move is dispatched like this:

```
await progress.race(this._client.send('Input.dispatchMouseEvent', {
  type: 'mouseMoved',
  button,
  buttons: toButtonsMask(buttons),
  x,
  y,
  modifiers: toModifiersMask(modifiers),
  force: buttons.size > 0 ? 0.5 : 0,
}));
```

The implementation, including `mousePressed` and `mouseReleased`, is in [`crInput.ts`, lines 104–164](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crInput.ts#L104-L164).

This is not the same as evaluating the following inside the page:

```
element.click();
```

The DOM method invokes the element’s programmatic click behaviour. The CDP route sends browser input at a position.

That difference explains much of Playwright’s action code. Chromium’s input domain does not understand a ‘role selector’ or an ‘accessible name’. Playwright must resolve the semantic target, inspect its current state, and produce valid coordinates before Chromium can act on it.

Now for the other side of the story…

##### The network path starts in `crNetworkManager`

The Chromium-specific network implementation is in:

```
packages/playwright-core/src/server/chromium/crNetworkManager.ts
```

When Playwright attaches a CDP session, it registers listeners for events from the browser’s Network and Fetch domains.

The source includes:

```
eventsHelper.addEventListener(session, 'Fetch.requestPaused', ...);
eventsHelper.addEventListener(session, 'Fetch.authRequired', ...);
eventsHelper.addEventListener(session, 'Network.requestWillBeSent', ...);
eventsHelper.addEventListener(session, 'Network.requestWillBeSentExtraInfo', ...);
eventsHelper.addEventListener(session, 'Network.requestServedFromCache', ...);
eventsHelper.addEventListener(session, 'Network.responseReceived', ...);
eventsHelper.addEventListener(session, 'Network.responseReceivedExtraInfo', ...);
eventsHelper.addEventListener(session, 'Network.loadingFinished', ...);
eventsHelper.addEventListener(session, 'Network.loadingFailed', ...);
```

Those registrations appear in [`crNetworkManager.ts`, lines 67–75](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crNetworkManager.ts#L67-L75). The same setup also registers WebSocket lifecycle and frame listeners in [`lines 79–85`](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crNetworkManager.ts#L79-L85).

These are close to the events I accessed through Selenium’s Chrome performance log in the previous article.

The difference is not that Playwright sees an entirely different network or anything like that. It consumes the browser events, handles their ordering and edge cases, and converts them into its own internal model.

##### CDP does not deliver one tidy request object

It would be convenient if Chromium emitted one event containing a complete request, followed by another containing a complete response. It does not.

Request and response information can arrive through several protocol events. When interception is enabled, Playwright may need to correlate `Fetch.requestPaused` with `Network.requestWillBeSent`. Redirects alter request relationships. Service workers can cause expected events to be absent. Additional request and response headers arrive through separate events.

One awkward case appears in `_onRequestPaused`:

```
if (!event.networkId) {
  // Fetch without networkId means that request was not recognized by inspector, and
  // it will never receive Network.requestWillBeSent. Continue the request to not affect it.
  sessionInfo.session._sendMayFail('Fetch.continueRequest', { requestId: event.requestId });
  return;
}
```

See [`crNetworkManager.ts`, lines 245–249](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crNetworkManager.ts#L245-L249).

Playwright cannot wait to correlate that paused request with `Network.requestWillBeSent`, because the source says the event will never arrive. It continues the request instead.

There is another special case for service workers. In `_onResponseReceived`, Playwright notes that frame-level requests handled by a service worker may never produce a `requestPaused` event. It may therefore construct the request when the response arrives. See [`crNetworkManager.ts`, lines 451–453](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crNetworkManager.ts#L451-L453).

Header information has its own ordering problem. The source comment for `ResponseExtraInfoTracker` says the ordinary request and response events, and their corresponding `ExtraInfo` events, are dispatched through different unassociated channels and may arrive in any order. The tracker associates them so that the extra headers are reliably available by `requestfinished`. See [`crNetworkManager.ts`, lines 769–782](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/chromium/crNetworkManager.ts#L769-L782).

As you can probably gather, there are loads of edge cases like this. Handling them is not glamorous work, but it is a large part of why the public API feels simple compared with working directly against the protocol.

##### Turning protocol events into Playwright events

Once Chromium’s network events have been associated with a Playwright request, they pass into the page’s `FrameManager`.

The request path marks the request as in flight, associates document requests with pending navigation, and emits the public request event:

```
requestStarted(request: network.Request, route?: network.RouteDelegate) {
  const frame = request.frame();
  this._inflightRequestStarted(request);
  if (frame && request._documentId)
    frame._setPendingDocument({ documentId: request._documentId, request });
  // ...
  this._page.addNetworkRequest(request);
  this._page.emitOnContext(BrowserContext.Events.Request, request);
  // ...
}
```

That code is in [`frames.ts`, lines 329–343](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L329-L343).

Responses and completion events are emitted nearby:

```
requestReceivedResponse(response: network.Response) {
  if (response.request()._isFavicon)
    return;
  this._page.emitOnContext(BrowserContext.Events.Response, response);
}

reportRequestFinished(request: network.Request, response: network.Response | null) {
  this._inflightRequestFinished(request);
  if (request._isFavicon)
    return;
  this._page.emitOnContext(BrowserContext.Events.RequestFinished, { request, response });
}
```

The implementations, including request failure handling, are in [`frames.ts`, lines 345–356](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L345-L356).

This is how low-level CDP events become the public interface used by tests:

```
page.on('request', request => {
  console.log(request.method(), request.url());
});

page.on('response', response => {
  console.log(response.status(), response.url());
});

page.on('requestfinished', request => {
  console.log('finished', request.url());
});

page.on('requestfailed', request => {
  console.log('failed', request.url());
});
```

##### How Playwright implements `networkidle`

The in-flight request tracking lives in `frames.ts`.

When a request starts, Playwright adds it to the owning frame’s `_inflightRequests` set. If it is the first active request, Playwright stops that frame’s network-idle timer.

When a request finishes or fails, Playwright removes it. If the set becomes empty, Playwright starts the timer again:

```
private _inflightRequestFinished(request: network.Request) {
  const frame = request.frame();
  if (this._isExcludedFromNetworkIdle(request) || !frame)
    return;
  if (!frame._inflightRequests.has(request))
    return;
  frame._inflightRequests.delete(request);
  if (frame._inflightRequests.size === 0)
    frame._startNetworkIdleTimer();
}

private _inflightRequestStarted(request: network.Request) {
  const frame = request.frame();
  if (this._isExcludedFromNetworkIdle(request) || !frame)
    return;
  frame._inflightRequests.add(request);
  if (frame._inflightRequests.size === 1)
    frame._stopNetworkIdleTimer();
}
```

The source is in [`frames.ts`, lines 385–403](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L385-L403).

The implementation deliberately excludes favicons and EventSource connections:

```
private _isExcludedFromNetworkIdle(request: network.Request): boolean {
  if (request._isFavicon)
    return true;
  if (request.resourceType() === 'eventsource')
    return true;
  return false;
}
```

See [`frames.ts`, lines 405–411](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L405-L411).

A favicon is browser housekeeping rather than meaningful application activity. An EventSource connection is long-lived by design, so counting it would prevent pages using server-sent events from ever reaching network idle.

The idle state is also calculated through the frame tree (the main page and its nested iframes). Lifecycle state is recorded on frames, and clearing lifecycle state resets the timer around the remaining current-navigation request. The relevant lifecycle and reset handling can be followed in [`frames.ts`, lines 573–584](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L573-L584).

This is honestly a more precise than saying Playwright waits until there are no requests. A closer description may be that Playwright tracks in-flight requests per frame, starts an idle timer when a frame’s set becomes empty, and combines the resulting state through the frame hierarchy.

It also shows why `networkidle` is not a universal indication that an application is ready.

A quiet network tells us nothing about a CSS animation, a client-side timer, data already queued for rendering, or an application bug that prevented the expected request from starting.

##### How the click starts waiting before navigation happens

There is another race to solve.

Suppose Playwright dispatched a click and only then began listening for navigation. A sufficiently fast navigation could start before the listener was installed.

The pointer action avoids that race by running the input inside `waitForSignalsCreatedBy`:

```
async waitForSignalsCreatedBy<T>(
  progress: Progress,
  waitAfter: boolean,
  action: (progress: Progress) => Promise<T>
): Promise<T> {
  if (!waitAfter)
    return action(progress);
  const barrier = new SignalBarrier(progress);
  this._signalBarriers.add(barrier);
  try {
    const result = await action(progress);
    await progress.race(this._page.delegate.inputActionEpilogue());
    await barrier.waitFor(progress);
    // Resolve in the next task, after all waitForNavigations.
    await new Promise<void>(makeWaitForNextTask());
    return result;
  } finally {
    this._signalBarriers.delete(barrier);
  }
}
```

That method is in [`frames.ts`, lines 192–207](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L192-L207).

`SignalBarrier` acts as a gate. It keeps the action open until navigation initiated by the click has been acknowledged.

When a frame may request navigation, active ‘barriers’ are retained. When the possible request has been resolved, they are released. If a frame actually requests navigation, Playwright adds that frame navigation to the barrier. See [`frames.ts`, lines 169–189](https://github.com/microsoft/playwright/blob/ad18048db947ca0a47c7fa59b77718e3a06afafe/packages/playwright-core/src/server/frames.ts#L169-L189).

The barrier is not a general wait for every network request caused by the click. It coordinates browser signals created by the action, especially navigation. General network observation continues through the network manager and the public request events.

A click may trigger an API request without navigating. Playwright cannot safely infer which arbitrary response represents the business operation a test cares about. The test must express that relationship explicitly:

```
const responsePromise = page.waitForResponse(
  response =>
    response.url().endsWith('/orders') &&
    response.request().method() === 'POST'
);

await page.getByRole('button', { name: 'Submit' }).click();

const response = await responsePromise;
```

The response wait is created before the click for the same reason as the internal barrier: a fast event must not be missed.

This is why Playwright’s documentation tells us to set up `waitForResponse` before the action that triggers it. The test applies the same broad principle Playwright uses internally with `waitForSignalsCreatedBy`: install the listener first, then perform the action.

##### Connecting this back to Selenium

Following the source changes how I would describe my previous experiment, but it does not make the experiment wrong.

The Chrome performance log approach observed real CDP network events. Playwright’s Chromium network manager listens to the same family of events.

What Playwright adds is a huge coordination layer. It installs protocol listeners, correlates Fetch- and Network-domain events, handles redirects and service workers, constructs request and response objects, exposes them as public events, tracks requests per frame, calculates network idle, and associates page-loading requests with navigation.

My Selenium examples addressed a narrower problem: determine whether network activity appeared to be in progress.

That may be enough for a particular test suite. Playwright needs a heavier implementation because a reusable framework must handle a broader range of browser behaviour, test styles, and edge cases than an application-specific helper.

A network counter also does not address the first half of the problem.

Before a request or navigation can exist, Playwright still has to make the click happen. That requires locator resolution, element-state checks, scrolling, geometry, hit testing, and browser input.

Network awareness cannot tell us that a transparent overlay is blocking a click. Pre-click actionability checks cannot tell us that the expected API response returned the correct data.

They solve different parts of synchronisation.

##### Wrapping up

We began with one line:

```
await page.getByRole('button', { name: 'Submit' }).click();
```

Following it through the source reveals two connected systems.

The first prepares and dispatches the input:

```
resolve the locator
check visible, enabled and stable
scroll the element
calculate a point
check the hit target
dispatch browser input
```

The second observes what the browser does next:

```
receive raw request and response events from the browser
normalise them into Playwright's own objects
track which requests are still in progress
emit public events that tests can observe
associate page-loading requests with navigation
wait for navigation triggered by the action
calculate network-idle state across the page and its iframes
```

These are the browser and network signals I was trying to expose through Selenium in the previous article. Playwright uses the same underlying family of events, but it does considerably more than count requests.

CDP is noisy and sometimes incomplete. Related information arrives through separate events and in inconvenient orders. Requests may redirect, pass through service workers, come from cache, or fail before every expected event appears. Playwright contains the code required to turn that stream into a more stable model.

The implementation also shows why `networkidle` is only one possible signal. It represents a quiet period in which no tracked requests are active. It does not establish that the application is correct, useful, or ready for the next business-level assertion.

I still do not think the conclusion is simply to choose Playwright over Selenium.

Selenium users can observe CDP events, execute browser-side JavaScript, and build application-specific waits. Playwright has chosen to make more of that coordination part of the framework itself. How much you want buried under an interface is up to you.

The useful lesson is to be precise about what we are waiting for.

Before a click, that may be a stable, enabled, and unobstructed target. After it, that may be a navigation lifecycle event or one specific API response. Sometimes network idle is useful. Sometimes it is the wrong question entirely.

I may still try reproducing Playwright’s “is something blocking the click?” check in Selenium.

I will not be reproducing `crNetworkManager`. Life is too short!

Did I get something wrong? Have you followed a similar trail through Playwright or Selenium? Please get in touch at [joseph@josephward.tech](mailto:joseph@josephward.tech).

* * *

* * *

### 7. ScrollTest / Pramod Dutta — Day 6: Fixtures — Dependency Injection That Eliminates Boilerplate

- Source: https://scrolltest.com/21-day-playwright-day-6-fixtures-dependency-injection/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 6 of the [21-Day Playwright with TypeScript Challenge](https://scrolltest.com/tag/21-day-playwright/). One lesson per day. Zero to production-ready in 3 weeks.

Fixtures inject dependencies into tests. No more repeated beforeEach setup. No more inheritance chains. Each test declares what it needs, fixtures provide it.

##### Built-in Fixtures

```typescript
test('uses built-in fixtures', async ({ page, context, browser, request }) => {
  // page — browser tab (most common)
  // context — isolated session (cookies, storage)
  // browser — browser instance
  // request — API client (no browser needed)
});
```

##### Custom Fixtures

```typescript
// src/fixtures/index.ts
import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';

type MyFixtures = {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
  authenticatedPage: LoginPage;
};

export const test = base.extend<MyFixtures>({
  loginPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await use(loginPage);
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },

  authenticatedPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@test.com', 'password');
    await use(loginPage);
    // Cleanup runs after test
  },
});

export { expect } from '@playwright/test';
```

##### Using Custom Fixtures

```typescript
// tests/dashboard.spec.ts
import { test, expect } from '../src/fixtures';

test('dashboard shows user name', async ({ authenticatedPage, page }) => {
  // authenticatedPage already logged in!
  await page.goto('/dashboard');
  await expect(page.getByText('Welcome, admin')).toBeVisible();
});
```

##### Why Fixtures Beat beforeEach

| beforeEach | Fixtures |
| --- | --- |
| All tests get same setup | Each test declares what it needs |
| No type safety | Full TypeScript types |
| No auto-cleanup | Cleanup runs after use() |
| Shared state risks | Isolated per test |

### 8. ScrollTest / Pramod Dutta — Day 2: Locator Strategies

- Source: https://scrolltest.com/21-day-playwright-day-2-locator-strategies-getbyrole/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 2 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Locators determine how your tests find elements. Wrong strategy = tests break every sprint. Right strategy = tests survive UI redesigns. Playwright offers 8+ locator types. Here is which to use and when.

##### Locator Resilience Ranking

| Tier | Locator | Resilience | Example |
| --- | --- | --- | --- |
| 1 (Best) | getByRole | Survives all UI changes | `page.getByRole('button', {name: 'Submit'})` |
| 2 | getByLabel | Survives class/ID changes | `page.getByLabel('Email')` |
| 3 | getByTestId | Explicit contract | `page.getByTestId('checkout-btn')` |
| 4 | getByText | Breaks on text change only | `page.getByText('Add to cart')` |
| 5 | getByPlaceholder | Moderate | `page.getByPlaceholder('Search')` |
| 6 (Worst) | CSS/XPath | Breaks on any DOM change | `page.locator('.btn-primary')` |

##### Each Locator Explained

```typescript
// 1. getByRole - ALWAYS try first
await page.getByRole('button', { name: 'Sign in' }).click();
await page.getByRole('link', { name: 'Documentation' }).click();
await page.getByRole('heading', { name: 'Welcome' });
await page.getByRole('textbox', { name: 'Email' }).fill('test@test.com');

// 2. getByLabel - for labeled form fields
await page.getByLabel('Password').fill('secret123');
await page.getByLabel('Remember me').check();

// 3. getByTestId - when semantic locators insufficient
await page.getByTestId('submit-button').click();
// Requires: data-testid="submit-button" in HTML

// 4. getByText - for visible text content
await page.getByText('Welcome back').click();
await page.getByText(/total: \$/i); // Regex support

// 5. getByPlaceholder - for placeholder text
await page.getByPlaceholder('Enter your email').fill('x@y.com');

// 6. CSS selector - last resort
await page.locator('.nav-item.active').click();
await page.locator('#main-content').isVisible();
```

##### Chaining and Filtering

```typescript
// filter() - narrow within results
await page.getByRole('listitem')
  .filter({ hasText: 'Active' })
  .click();

// .and() - both conditions must match
const btn = page.getByRole('button')
  .and(page.getByText('Submit'));

// .or() - either matches
const cta = page.getByRole('button', { name: 'Buy' })
  .or(page.getByRole('button', { name: 'Purchase' }));

// Chaining parent > child
await page.getByTestId('product-card')
  .filter({ hasText: 'Laptop' })
  .getByRole('button', { name: 'Add to cart' })
  .click();
```

##### Decision Tree

1. Has visible role? Use getByRole()
2. Labeled form field? Use getByLabel()
3. Has data-testid? Use getByTestId()
4. Unique visible text? Use getByText()
5. None above? CSS selector (never XPath)
F
This is Day 3 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Playwright assertions auto-retry until condition met or timeout. This eliminates flaky assertions entirely - if you use them correctly.

##### Auto-Retry vs Non-Retry Assertions

```typescript
// AUTO-RETRY (preferred - waits up to 5s by default)
await expect(page.getByText('Welcome')).toBeVisible();
await expect(page.getByRole('button')).toBeEnabled();
await expect(page).toHaveTitle(/Dashboard/);
await expect(page).toHaveURL(/dashboard/);
await expect(page.getByTestId('count')).toHaveText('5');

// NON-RETRY (instant check - use sparingly)
expect(await page.title()).toBe('Dashboard');
expect(await page.getByText('x').count()).toBe(3);
```

##### Essential Assertions

| Assertion | What It Checks |
| --- | --- |
| `toBeVisible()` | Element visible on page |
| `toBeHidden()` | Element not visible |
| `toBeEnabled()` | Not disabled |
| `toBeDisabled()` | Has disabled attribute |
| `toBeChecked()` | Checkbox/radio selected |
| `toHaveText('x')` | Element contains text |
| `toContainText('x')` | Partial text match |
| `toHaveValue('x')` | Input value |
| `toHaveAttribute('k','v')` | HTML attribute |
| `toHaveCount(n)` | Number of matching elements |
| `toHaveURL(/pattern/)` | Page URL matches |
| `toHaveTitle(/pattern/)` | Page title matches |

##### Soft Assertions

```typescript
// Soft assertions do not stop test on failure - collect all failures
await expect.soft(page.getByTestId('name')).toHaveText('John');
await expect.soft(page.getByTestId('email')).toHaveText('john@test.com');
await expect.soft(page.getByTestId('role')).toHaveText('Admin');
// Test continues, reports ALL failures at end
```

##### Custom Timeout and Messages

```typescript
// Custom timeout for slow operations
await expect(page.getByText('Report ready')).toBeVisible({ timeout: 30_000 });

// Custom error message
await expect(page.getByTestId('price'),
  'Price should reflect 10% discount'
).toHaveText('$90.00');
```

### 9. ScrollTest / Pramod Dutta — Day 3: Assertions That Actually Catch Bugs

- Source: https://scrolltest.com/21-day-playwright-day-3-assertions-expect-deep-dive/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 3 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Playwright assertions auto-retry until condition met or timeout. This eliminates flaky assertions entirely - if you use them correctly.

##### Auto-Retry vs Non-Retry Assertions

```typescript
// AUTO-RETRY (preferred - waits up to 5s by default)
await expect(page.getByText('Welcome')).toBeVisible();
await expect(page.getByRole('button')).toBeEnabled();
await expect(page).toHaveTitle(/Dashboard/);
await expect(page).toHaveURL(/dashboard/);
await expect(page.getByTestId('count')).toHaveText('5');

// NON-RETRY (instant check - use sparingly)
expect(await page.title()).toBe('Dashboard');
expect(await page.getByText('x').count()).toBe(3);
```

##### Essential Assertions

| Assertion | What It Checks |
| --- | --- |
| `toBeVisible()` | Element visible on page |
| `toBeHidden()` | Element not visible |
| `toBeEnabled()` | Not disabled |
| `toBeDisabled()` | Has disabled attribute |
| `toBeChecked()` | Checkbox/radio selected |
| `toHaveText('x')` | Element contains text |
| `toContainText('x')` | Partial text match |
| `toHaveValue('x')` | Input value |
| `toHaveAttribute('k','v')` | HTML attribute |
| `toHaveCount(n)` | Number of matching elements |
| `toHaveURL(/pattern/)` | Page URL matches |
| `toHaveTitle(/pattern/)` | Page title matches |

##### Soft Assertions

```typescript
// Soft assertions do not stop test on failure - collect all failures
await expect.soft(page.getByTestId('name')).toHaveText('John');
await expect.soft(page.getByTestId('email')).toHaveText('john@test.com');
await expect.soft(page.getByTestId('role')).toHaveText('Admin');
// Test continues, reports ALL failures at end
```

##### Custom Timeout and Messages

```typescript
// Custom timeout for slow operations
await expect(page.getByText('Report ready')).toBeVisible({ timeout: 30_000 });

// Custom error message
await expect(page.getByTestId('price'),
  'Price should reflect 10% discount'
).toHaveText('$90.00');
```

### 10. ScrollTest / Pramod Dutta — Day 5: Page Object Model — Structure Tests That Scale

- Source: https://scrolltest.com/21-day-playwright-day-5-page-object-model-structure/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 5 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Without POM, test code becomes unmaintainable spaghetti. Page Objects separate what you interact with (locators) from what you test (assertions). One locator change = one file change, not 50.

##### LoginPage Example

```typescript
// src/pages/LoginPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.loginButton = page.getByRole('button', { name: 'Sign in' });
    this.errorAlert = page.getByRole('alert');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectError(message: string) {
    await expect(this.errorAlert).toContainText(message);
  }
}
```

##### Test File Using POM

```typescript
// tests/login.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../src/pages/LoginPage';

test.describe('Login', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('valid credentials redirect to dashboard', async ({ page }) => {
    await loginPage.login('admin@test.com', 'password123');
    await expect(page).toHaveURL(/dashboard/);
  });

  test('invalid password shows error', async () => {
    await loginPage.login('admin@test.com', 'wrong');
    await loginPage.expectError('Invalid credentials');
  });
});
```

##### POM Anti-Patterns

- **God Object:** 500-line page class. Split into components (Navbar, Footer, Sidebar).
- **Deep inheritance:** BasePage > AbstractPage > LoginPage. Use composition, not inheritance.
- **Business logic in POM:** Page objects interact with UI only. Test files own assertions and logic.
- **Returning page objects:** Methods return void. Test navigates explicitly.

### 11. ScrollTest / Pramod Dutta — Day 13: Debugging — Trace Viewer, UI Mode, and Inspector

- Source: https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 13 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Stop adding console.log. Playwright has 3 built-in debugging tools that show exactly what happened, step by step.

##### Trace Viewer - Post-Mortem Debugging

```typescript
// Enable in config
use: { trace: 'retain-on-failure' }

// Open trace after failure
// npx playwright show-trace test-results/my-test/trace.zip
```

Shows: action timeline, DOM snapshots before/after each step, network requests with bodies, console logs, source code mapping.

##### UI Mode - Interactive Testing

```bash
npx playwright test --ui
```

Watch tests execute in real-time. Click any step to see DOM state. Re-run individual tests. Filter by file. Time-travel through test execution.

##### Inspector - Step-by-Step

```bash
npx playwright test --debug
```

Pauses at each action. Step through one action at a time. Inspect element selectors live. Evaluate locators in console.

##### 60-Second Debugging Workflow

1. Open trace file (5s)
2. Jump to failing action in timeline (10s)
3. Compare DOM before/after (15s)
4. Check network tab - did API return expected data? (15s)
5. Check console for JS errors (10s)
6. Root cause identified (5s)

### 12. ScrollTest / Pramod Dutta — Day 19: Advanced Patterns — Retry, Tags, Parameterization, and Hooks

- Source: https://scrolltest.com/21-day-playwright-day-19-advanced-patterns-retry-tags/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This is Day 19 of the 21-Day Playwright with TypeScript Challenge. One lesson per day. Zero to production-ready in 3 weeks.

Power features that make your test suite production-grade: retries for CI stability, tags for selective execution, parameterization for data-driven tests, hooks for lifecycle management.

##### Retries

```typescript
// Global retries (CI only)
retries: process.env.CI ? 2 : 0,

// Per-describe retries
test.describe('flaky external service', () => {
  test.describe.configure({ retries: 3 });
  test('call third-party API', async () => { /* ... */ });
});
```

##### Tags for Selective Execution

```typescript
test('login flow @smoke', async ({ page }) => { /* ... */ });
test('complex workflow @regression', async ({ page }) => { /* ... */ });
test('payment processing @smoke @critical', async ({ page }) => { /* ... */ });

// Run only smoke tests
// npx playwright test --grep @smoke

// Run everything EXCEPT regression
// npx playwright test --grep-invert @regression
```

##### Parameterized Tests

```typescript
const users = [
  { role: 'admin', canDelete: true },
  { role: 'editor', canDelete: false },
  { role: 'viewer', canDelete: false },
];

for (const user of users) {
  test('delete button visibility for ' + user.role, async ({ page }) => {
    await loginAs(page, user.role);
    await page.goto('/settings');
    if (user.canDelete) {
      await expect(page.getByRole('button', { name: 'Delete' })).toBeVisible();
    } else {
      await expect(page.getByRole('button', { name: 'Delete' })).toBeHidden();
    }
  });
}
```

##### Hooks

```typescript
test.beforeAll(async () => {
  // Run once before all tests in file (seed database)
});

test.afterAll(async () => {
  // Run once after all tests (cleanup)
});

test.beforeEach(async ({ page }) => {
  // Run before each test (navigate to page)
  await page.goto('/dashboard');
});

test.afterEach(async ({}, testInfo) => {
  // Run after each test (log result)
  if (testInfo.status === 'failed') {
    console.log('Failed:', testInfo.title);
  }
});
```

##### test.step() for Sub-Steps

```typescript
test('complete checkout', async ({ page }) => {
  await test.step('add product to cart', async () => {
    await page.goto('/products');
    await page.getByRole('button', { name: 'Add to cart' }).click();
  });

  await test.step('fill shipping info', async () => {
    await page.goto('/checkout');
    await page.getByLabel('Address').fill('123 Main St');
  });

  await test.step('complete payment', async () => {
    await page.getByRole('button', { name: 'Pay' }).click();
    await expect(page.getByText('Order confirmed')).toBeVisible();
  });
});
```

### 13. ScrollTest / Pramod Dutta — Playwright Global Setup and Teardown with TypeScript

- Source: https://scrolltest.com/playwright-global-setup-teardown-typescript-day-58/
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### What Is Playwright Global Setup?

In Playwright, `globalSetup` is a function you point to from your config. It runs a single time in a Node.js process before any test file starts, and its sibling `globalTeardown` runs a single time after the last test finishes. They are not tests. They cannot use your page objects or fixtures directly. What they can do is anything a plain Node script can do: launch a browser manually, hit an API, talk to a database, or write a file to disk.

I describe it to my team like this: `beforeAll` runs once per worker file, `beforeEach` runs once per test, and `globalSetup` runs once per entire run. The distinction matters because most people reach for `beforeAll` when they actually want `globalSetup`, and then they pay for it with duplicated login calls and repeated database resets.

The most common real-world uses are:

- Log in once and save a `storageState` JSON so every test starts already authenticated.
- Seed or reset a test database to a known state before the suite.
- Fetch a one-time auth token or API key that tests then consume.
- Build or start shared fixtures, like compiling test assets or generating a large data file.

Because `globalSetup` is plain Node and not a browser test, anything you can script, you can run there: shell commands, HTTP calls, database migrations, even spawning a child process. That freedom is what makes it the right home for work that has nothing to do with a single page.

##### Why Global Setup Matters: Auth, DB Seeding, and One-Time Work

The payoff is measurable. If your login takes 3 seconds and you do it in a `beforeEach` across 200 tests, that is 600 seconds, roughly 10 minutes, of pure login before a single assertion runs. Move it into `globalSetup` and you do it once. I have cut regression suites from 47 minutes to 9 minutes on projects where login and data seeding were the bottleneck, and the change was not a new framework. It was moving one-time work out of the hot path.

None of this is a niche trick, either. Playwright has crossed [94,000 stars on GitHub](https://github.com/microsoft/playwright), and the [@playwright/test package now logs over 210 million downloads a month](https://www.npmjs.com/package/@playwright/test) on the npm registry. The framework patterns around it, global setup included, are exactly what hiring managers and senior QA leads expect you to know.

There are three places this matters most in a real QA suite:

**1\. Authentication.** Instead of typing credentials in every test, `globalSetup` signs in once, captures cookies and localStorage, and saves them to a `storageState` file. Every test then loads that file and starts logged in. This is the same `storageState` mechanism I covered in the [Playwright Authentication (Day 10)](https://scrolltest.com/playwright-authentication-day-10/) article, but moved from per-test to per-run.

**2\. Database seeding.** Integration tests need known rows: a user with an existing order, a locked account, a product with zero stock. Running `INSERT` statements in `beforeEach` works but is slow and leaves state drifting between tests. Seeding once in `globalSetup` gives you a clean, deterministic baseline.

**3\. One-time tokens and assets.** If your app needs a JWT from an identity provider, or your visual tests need a built fixture bundle, doing that per test is wasteful. Do it once.

The trade-off is that `globalSetup` makes your suite less isolated. If one test mutates shared data, other tests can see it. I come back to this in the pitfalls section, because it is the number one reason people move away from global setup after adopting it naively.

##### Wiring globalSetup Into playwright.config.ts

You register both hooks in the config. The key detail most people miss is `require.resolve()`. Playwright resolves the path relative to the config file, and wrapping it in `require.resolve()` avoids “cannot find module” errors when your project has a non-standard root.

```
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  globalSetup: require.resolve('./global-setup'),
  globalTeardown: require.resolve('./global-teardown'),
  use: {
    baseURL: 'https://app.example.com',
    storageState: 'playwright/.auth/user.json',
  },
  projects: [\
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\
  ],
});
```

Notice the `storageState` path in the top-level `use` block. Your `globalSetup` writes to that exact same path. If the two paths drift, the setup runs and saves a file, but your tests never load it, and every test starts logged out. This is a silent failure, which is why I make the path a single constant both places read from.

If your tests need a running backend, add a `webServer` entry too. Playwright boots the `webServer` before `globalSetup` runs, so your setup code can hit a live URL. That ordering is useful when your login step depends on the app being up.

##### Login Once, Save the Storage State

This is the most common `globalSetup` on the planet. Here is the full TypeScript version: launch a browser, navigate, log in, and save the session.

```
// global-setup.ts
import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
  const { baseURL, storageState } = config.projects[0].use;

  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto(baseURL!);
  await page.getByLabel('Email').fill(process.env.E2E_USER!);
  await page.getByLabel('Password').fill(process.env.E2E_PASSWORD!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('**/dashboard');

  await page.context().storageState({ path: storageState as string });
  await browser.close();
}

export default globalSetup;
```

Three things to note. First, the credentials come from `process.env`, never hardcoded, because this file often runs in CI. Second, I wait for a known URL after login so the session is fully settled before I capture it, otherwise you save a half-finished cookie jar. Third, I read `baseURL` and `storageState` off the config so the setup and the test runner can never disagree.

On the test side, nothing special is required. Because the config declares `use.storageState`, every test starts authenticated. If you have multiple roles, you create multiple storage states, either by running several logins inside one `globalSetup` or by pointing different projects at different state files. The [Playwright auth guide](https://playwright.dev/docs/auth) documents this multi-role pattern in detail.

One more thing: storage states expire. A saved cookie jar from last week is useless once the token hits its 24-hour TTL, which is exactly why the login lives in `globalSetup` and runs fresh on every CI run instead of being committed to the repo. If you commit a state file and reuse it for days, you will eventually spend an hour debugging tests that fail only because the session silently went stale.

##### Seeding a Database Before the Run

Authentication is only half the story. For integration suites, you also need known data. Here is a `globalSetup` that resets and seeds a Postgres database using the `pg` driver.

```
// global-setup.ts (database seed)
import { Client } from 'pg';

async function seedDatabase() {
  const client = new Client({ connectionString: process.env.TEST_DATABASE_URL });
  await client.connect();

  await client.query('TRUNCATE users, orders RESTART IDENTITY CASCADE;');
  await client.query(`
    INSERT INTO users (id, email, role) VALUES
      (1, 'qa@example.com', 'admin'),
      (2, 'basic@example.com', 'member');
  `);
  await client.query(`
    INSERT INTO orders (id, user_id, total, status) VALUES
      (100, 1, 250.00, 'paid'),
      (101, 2, 19.99, 'pending');
  `);

  await client.end();
}
```

You call `seedDatabase()` from your main `globalSetup` function, or export it separately and chain it. The `TRUNCATE ... RESTART IDENTITY CASCADE` line is the important part: it wipes the tables and resets auto-increment IDs so your tests can rely on `id = 1` being the admin every single run. Deterministic IDs are what make assertions like `expect(page.getByText('qa@example.com')).toBeVisible()` stable.

One warning: seeding belongs in `globalSetup` only if your tests are read-only against that data. If tests mutate the seeded rows, you get ordering problems, because one test’s changes leak into the next. For mutation-heavy suites, prefer per-test fixtures with transactions instead, which I contrast in a moment.

##### globalTeardown: Cleanup After the Run

Whatever you stand up in `globalSetup`, you should tear down in `globalTeardown`. It runs once after the last test, even if some tests failed. Common teardown jobs:

- Revoke the auth token you minted so it cannot be reused.
- Drop the test database or clean up seeded rows in shared environments.
- Aggregate test artifacts, like merging coverage or copying trace files.
- Stop any temporary services you started outside `webServer`.

```
// global-teardown.ts
import { FullConfig } from '@playwright/test';
import { Client } from 'pg';

async function globalTeardown(config: FullConfig) {
  const client = new Client({ connectionString: process.env.TEST_DATABASE_URL });
  await client.connect();
  await client.query('DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;');
  await client.end();
}

export default globalTeardown;
```

Be careful with destructive teardown in a shared environment. Dropping a schema is fine when the database is dedicated to tests, but it is a footgun against a staging database your team shares. In my setups, teardown is conditional: it only drops when an env flag like `E2E_DESTRUCTIVE_TEARDOWN=1` is set, so a misconfigured local run cannot nuke shared data.

##### Passing Data From globalSetup to Your Tests

This is the question everyone asks after their first working setup: how do I hand a value from `globalSetup` to a test? The answer is `process.env`, because `globalSetup` runs in its own process and module-level variables will not reliably cross that boundary.

```
// inside global-setup.ts
process.env.SEEDED_ADMIN_ID = '1';
process.env.API_BASE = 'https://api.example.com/v2';
```

```
// tests/orders.spec.ts
import { test, expect } from '@playwright/test';

test('admin sees the seeded order', async ({ page }) => {
  const adminId = process.env.SEEDED_ADMIN_ID;
  expect(adminId).toBeDefined();
  await page.goto(`/users/${adminId}/orders`);
  await expect(page.getByText('$250.00')).toBeVisible();
});
```

Environment variables are strings only, so if you need to pass an object or an array, serialize it with `JSON.stringify` on the way out and `JSON.parse` on the way in. This is a simple pattern, but it is the difference between a flaky “undefined at runtime” bug and a suite that works on the first try in CI.

##### globalSetup vs Test Fixtures vs beforeAll

Most confusion in Playwright is people using the wrong scope for one-time work. Here is the decision rule I teach:

- **Runs once per entire run** and is not tied to a browser test: use `globalSetup`. Examples: login once, seed the database, fetch a shared token.
- **Runs per worker or per file** and needs Playwright fixtures like `page`: use `beforeAll` inside a describe block or a worker-scoped fixture.
- **Runs per test** and needs isolation: use a fixture with `beforeEach` semantics, which is the default for Playwright fixtures.

Playwright’s own docs recommend fixtures over `globalSetup` for most data concerns, because fixtures give you automatic teardown and per-test isolation. A typed fixture looks like this:

```
// fixtures.ts
import { test as base } from '@playwright/test';
import { Client } from 'pg';

type DbFixture = { db: Client };

export const test = base.extend<DbFixture>({
  db: async ({}, use) => {
    const client = new Client({ connectionString: process.env.TEST_DATABASE_URL });
    await client.connect();
    await use(client);
    await client.end();
  },
});
```

I use `globalSetup` for true global concerns: authentication state and schema-level seeding. I use fixtures for per-test data and anything that must stay isolated. If a value needs to be shared by literally every test and created exactly once, that is `globalSetup`. If it needs to be clean per test, that is a fixture. Forgetting the difference is how suites end up with slow, order-dependent tests.

The pattern I land on most often is a hybrid: `globalSetup` handles login and schema seeding, while fixtures wrap each test in a database transaction that rolls back on teardown. That gives you the speed of one-time setup with the isolation of per-test cleanup. You get the best of both scopes instead of forcing one mechanism to do both jobs. The [Playwright global setup docs](https://playwright.dev/docs/test-global-setup-teardown) and the [Fixtures and Hooks (Day 6)](https://scrolltest.com/playwright-fixtures-hooks-day-6/) article cover both sides of that line.

##### Putting It Together: One Run End to End

Here is the exact order Playwright follows on a full run, which clears up most “when does this actually fire?” questions:

1. The `webServer` entry (if configured) starts.
2. `globalSetup` runs once: log in, save the `storageState`, seed the database.
3. Test workers launch and each test loads the saved `storageState`.
4. All tests finish, then `globalTeardown` runs once to clean up.
5. The `webServer` process stops.

You can watch this in your terminal. A healthy run shows the setup work up front, then the test lines, then the teardown at the very end:

```
$ npx playwright test

Running global setup from playwright.config.ts
Seeded 2 users and 2 orders
Saved storage state to playwright/.auth/user.json

  3 passed (38s)

Running global teardown from playwright.config.ts
Dropped test schema
```

If you see the setup logs appear before every test file instead of once, your setup is running at the wrong scope, probably inside `beforeAll`. That is the single best signal to catch scope mistakes early.

##### Playwright Global Setup Pitfalls

These are the mistakes I see teams make, in the order of how often they bite:

1. **Using `page` or test fixtures inside `globalSetup`.** You cannot. Launch a browser manually with `chromium.launch()` like the example above.
2. **Forgetting `require.resolve()`.** A bare relative string can resolve against the wrong directory and throw a “cannot find module” error only in CI.
3. **Mismatched `storageState` paths.** Setup writes one path, config reads another. Tests run logged out and nothing fails loudly.
4. **Putting per-test data in `globalSetup`.** If tests mutate seeded rows, you get order-dependent flakiness. Move that data to fixtures.
5. **Hardcoding secrets.** Credentials and database URLs belong in `process.env`, not in committed files.
6. **Ignoring `globalSetup` failures.** If setup throws, the entire run fails before any test executes. Wrap it so the error is readable, or you will stare at an empty report.
7. **Destructive teardown in a shared environment.** Gate `DROP` and `TRUNCATE` behind an explicit flag.
8. **Assuming the config and setup share state.** They run in different processes. Only `process.env` reliably crosses the boundary.

##### The India SDET Interview Angle

If you are preparing for SDET interviews in India, expect a version of this question: “How do you reuse a login session across your Playwright tests?” The answer that separates a mid-level candidate from a senior one is not “I log in in `beforeEach`.” It is “I capture a `storageState` in `globalSetup` so the login runs once, and I keep per-test data in fixtures so tests stay isolated.”

Interviewers at product companies in Bengaluru and Hyderabad are testing whether you understand scope: run-level, worker-level, and test-level. Being able to explain `globalSetup` versus `beforeAll` versus a fixture, with a concrete example of when each is correct, signals the kind of framework ownership that maps to the ₹15-40 LPA band for senior SDET roles.

The follow-up they almost always ask is how you keep tests isolated once data is shared across the suite, which is where the fixture-and-transaction answer from earlier comes back around. If you can hold both sides of that conversation, you have effectively demonstrated the framework design skill they are actually paying for. The [Playwright CI GitHub Actions (Day 12)](https://scrolltest.com/playwright-ci-github-actions-day-12/) article shows how this same setup slots into a real pipeline.

##### Key Takeaways

- `globalSetup` runs once per run before tests, `globalTeardown` once after, and neither can use test fixtures directly.
- The killer use case is login once and save a `storageState`, plus one-time database seeding.
- Register both with `require.resolve()` in `playwright.config.ts`, and keep the `storageState` path shared between config and setup.
- Pass values to tests through `process.env`, serializing objects with JSON.
- Use fixtures for anything that must stay isolated per test; use Playwright global setup only for true run-level concerns.

##### FAQ

###### What is the difference between globalSetup and beforeAll in Playwright?

`globalSetup` runs once for the entire test run in its own Node process. `beforeAll` runs once per worker file, inside the test runner, and can use fixtures like `page`. Use `globalSetup` for login and schema seeding, `beforeAll` for per-file browser setup.

###### Can I use the page fixture inside globalSetup?

No. Fixtures are not available in `globalSetup` because it runs outside the test worker. Launch a browser manually with `chromium.launch()` and build your own context.

###### How do I share a login session across all tests?

Log in once in `globalSetup`, call `context.storageState({ path })`, and set the same path in your config’s `use.storageState`. Every test then starts authenticated.

###### Does globalSetup run in parallel with other workers?

No. It runs once in a dedicated process before any test worker starts. This is exactly why it is the right place for one-time setup that must finish before tests begin.

###### How do I pass dynamic data from globalSetup to tests?

Use `process.env`. Set it in `globalSetup` and read it in your tests or fixtures. Values are strings, so JSON-encode anything complex.

###### Can I skip globalSetup for a quick local run?

Yes. Wrap the expensive parts in a condition, or check an env flag inside `globalSetup` and skip login when something like `E2E_SKIP_SETUP=1` is set. That lets you iterate on a single test without waiting for the database to reseed every time.

![](https://secure.gravatar.com/avatar/4cf909139a878a25bd3fa83a0d15909cda4cd3233c157e1f37f60dbd19c9d7b1?s=80&d=mm&r=g)

**[Promode](https://scrolltest.com/author/admin/)**

[Facebook](http://techdutta/ "Follow Promode on Facebook") [X](https://twitter.com/itstechmode "Follow Promode on X formerly Twitter")

### 14. ScrollTest / Pramod Dutta — Playwright TypeScript Checklist

- Source: https://scrolltest.com/playwright-typescript-checklist/
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### Why a Playwright TypeScript checklist matters

Playwright has become a serious choice for modern web automation. The public GitHub repository shows more than 92,000 stars, and the npm API reported more than 181 million downloads for `@playwright/test` in the last month when I checked it for this article. Those numbers do not make your framework good by default, but they tell us one thing clearly: many teams are now betting their UI and API checks on this tool.

That shift creates a new problem. A beginner can create a test with `npx playwright codegen` in five minutes, but a release-ready framework needs rules. It needs stable selectors, clean fixtures, useful traces, deterministic test data, readable reports, and a CI setup that fails for the right reason.

I see three types of Playwright suites in companies:

- **Demo suites** that prove Playwright can click buttons.
- **Team suites** that run for one squad but depend on tribal knowledge.
- **Release suites** that a manager trusts before deployment.

This Playwright TypeScript checklist is designed to move you from the second bucket to the third. If you are working in a service company like TCS, Infosys, Wipro, or Cognizant, this checklist helps you speak in terms of maintainability and risk. If you are in a product company, it helps you reduce regression time without creating a flaky gate that developers start ignoring.

For official behavior, always cross-check the [Playwright documentation](https://playwright.dev/docs/intro). For release changes, track the [Playwright GitHub releases](https://github.com/microsoft/playwright/releases). I am adding those links because production automation should not depend only on blog posts, including mine.

##### Baseline setup: versions, config, and scripts

Your checklist starts before the first test file. A messy setup creates slow failures later. I want the project to be boring, explicit, and easy for a new SDET to run on day one.

###### 1\. Pin Playwright and browser versions

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

The exact versions will change. The rule stays the same: document the version, update deliberately, and run a smoke pack after every upgrade. Playwright’s latest release at the time of this cron run was v1.61.1 on GitHub, so I would create an upgrade branch before moving a team framework to it.

###### 2\. Keep `playwright.config.ts` readable

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
  reporter: [\
    ['html', { open: 'never' }],\
    ['junit', { outputFile: 'test-results/junit.xml' }]\
  ],
  use: {
    baseURL: process.env.BASE_URL ?? 'https://example.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [\
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }\
  ]
});
```

This config follows the official ideas documented in [Playwright test configuration](https://playwright.dev/docs/test-configuration). The key is not copying my values blindly. The key is knowing why each value exists.

###### 3\. Add a local readiness command

Before CI, create one command that checks TypeScript, linting, and a small smoke suite. This prevents weak commits from reaching the pipeline.

```bash
npm run typecheck
npm run lint
npm run test:e2e:smoke
```

**Screenshot description:** capture your terminal after this command passes. The screenshot should show the Playwright summary line, browser project name, duration, and zero failed tests. Add it to your team wiki as the expected local baseline.

##### Selector and assertion checklist

Bad selectors are the fastest way to make a good tool look unreliable. Playwright gives you better locator APIs than older frameworks, but it does not stop you from writing brittle selectors.

###### Prefer user-facing locators first

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

###### Assertions should wait for business outcomes

Do not assert random implementation details. Assert the result the user or system cares about. Playwright’s web-first assertions automatically retry until the timeout, so use them instead of manual sleep calls.

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

If you want a focused guide for dynamic inputs, read the ScrollTest tutorial on [testing autocomplete and typeahead inputs in Playwright](https://scrolltest.com/playwright-autocomplete-typeahead-testing/). For select controls, the [multi-select dropdown guide](https://scrolltest.com/playwright-multi-select-dropdown-testing/) gives a useful companion example.

##### Fixture, data, and authentication checklist

Fixtures decide whether your framework scales. Without fixtures, every test repeats setup code. With bad fixtures, every test hides too much and becomes hard to understand.

###### Create fixtures for behavior, not convenience

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

This pattern uses Playwright’s fixture model, described in the [official fixture documentation](https://playwright.dev/docs/test-fixtures). The setup and cleanup are in one place, and the test receives a ready-to-use object.

###### Keep authentication out of every test

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
projects: [\
  { name: 'setup', testMatch: /.*\.setup\.ts/ },\
  {\
    name: 'chromium-authenticated',\
    use: { ...devices['Desktop Chrome'], storageState: 'playwright/.auth/user.json' },\
    dependencies: ['setup']\
  }\
]
```

Do not commit real auth files. Add them to `.gitignore`. The official [Playwright authentication guide](https://playwright.dev/docs/auth) covers the storage state pattern in detail.

###### Test data needs ownership

In Indian QA teams, I often see one shared QA environment with one shared user and one shared test record. That works for demos. It fails badly when five engineers, one nightly job, and one release candidate touch the same data.

Use this data checklist:

- Every created record has a unique prefix, for example `e2e-`.
- Every test owns its data or uses read-only reference data.
- Cleanup is automatic, but the test still passes if cleanup fails after the assertion.
- Secrets come from CI variables, not from committed files.
- Data factories stay typed, so a refactor fails during TypeScript checks.

For a broader strategy, I would pair this article with ScrollTest’s [test data management guide for SDETs](https://scrolltest.com/test-data-management-guide-sdet-factory-pattern-cicd/).

##### Debugging, traces, and screenshot evidence

A release-ready suite must fail loudly and usefully. A bad failure says “Timeout 30000ms exceeded.” A good failure says which user journey broke, what the page looked like, what network call failed, and what changed between the first attempt and the retry.

###### Turn on traces where they help

Playwright trace viewer is one of the biggest reasons I prefer it for team frameworks. The official [trace viewer documentation](https://playwright.dev/docs/trace-viewer) explains how traces capture actions, snapshots, console logs, and network details. In CI, I usually set traces to `on-first-retry`. This keeps normal runs light while preserving evidence for flaky failures.

```ts
use: {
  trace: 'on-first-retry',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure'
}
```

**Screenshot description:** open a failed trace in Trace Viewer and capture the action timeline. The screenshot should show the failing click, the DOM snapshot on the right, and the network tab with the failed API request highlighted. This is the kind of evidence developers actually use.

###### Add failure notes with `test.step`

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

###### Use a triage label system

When a CI job fails, classify the failure before fixing it. I use five buckets:

1. **Product bug:** application behavior changed or broke.
2. **Test bug:** the test made a wrong assumption.
3. **Environment issue:** deployment, data, or dependency failed.
4. **Framework issue:** fixture, config, auth, or helper failed.
5. **Unknown:** needs trace review before assignment.

This is where SDETs add real value. Do not just paste a failed screenshot in Slack. Add the bucket, suspected cause, trace link, affected build, and whether the failure blocks release.

##### CI checklist for reliable release gates

CI is where Playwright frameworks become real. A test that only runs from one engineer’s laptop is not a release signal. The CI version must be repeatable, fast enough, and strict about reporting.

###### Use a clear GitHub Actions workflow

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

###### Set pass criteria before adding more tests

Teams often ask for 500 automated tests. I would rather have 50 reliable tests that block a release correctly. Define these rules:

- Smoke pack must finish in under 10 minutes.
- Critical pack must upload trace, screenshot, and HTML report.
- Retries are allowed in CI, but every retry failure is reviewed weekly.
- Tests tagged `@wip` cannot run in release gates.
- Flaky tests are quarantined with an owner and removal date.

For advanced CI patterns, read the earlier ScrollTest article on [building an AI-augmented Playwright test suite](https://scrolltest.com/ai-augmented-playwright-test-suite-practical-playbook/). It pairs well with this checklist when you start adding generated scenarios or AI-assisted failure summaries.

###### Sharding is not a replacement for cleanup

Sharding helps speed, but it also exposes weak data design. If tests collide when they run in parallel, do not blame CI. Fix the data ownership first, then split the workload.

```bash
npx playwright test --shard=1/4
npx playwright test --shard=2/4
npx playwright test --shard=3/4
npx playwright test --shard=4/4
```

The Playwright CI guide at [playwright.dev/docs/ci](https://playwright.dev/docs/ci) is the source I keep open when I review pipeline setup. It is short, direct, and updated with framework changes.

##### Code review checklist for SDETs

Code review is where automation quality becomes team culture. If reviewers only check whether a test passes locally, the framework will decay.

###### Review the user journey first

Ask one question before reading helper details: does this test protect a real risk? If the answer is no, the test should not enter the suite.

Use this review checklist:

1. Does the test name describe business behavior?
2. Does the setup create or reference clear test data?
3. Are locators user-facing or intentionally test-id based?
4. Are assertions tied to user-visible or API-visible outcomes?
5. Will this test run in parallel without colliding with another test?
6. Does the failure produce useful evidence?
7. Is the test tagged correctly for smoke, regression, or release?

###### Reject hidden waits and silent catches

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

###### Make ownership visible

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

##### Common pitfalls I still see

Even good teams repeat the same mistakes. Use this section as a final scan before you call your framework production-ready.

###### Pitfall 1: treating retries as a fix

Retries are a diagnostic tool. They are not a quality strategy. If a test passes on retry every day, the suite is telling you something. It may be an async issue, unstable data, slow environment, or a real intermittent product bug. Review retry reports weekly.

###### Pitfall 2: mixing API setup with UI assertions badly

API setup is excellent when used carefully. Create data through APIs, then verify user behavior in the UI. Do not verify UI behavior only through API responses and still call it an end-to-end test.

###### Pitfall 3: overusing page objects

Page Object Model is useful, but it can become a second application with its own bugs. Keep page objects thin. Store locators and simple actions there. Keep test intent visible in the spec file.

###### Pitfall 4: ignoring accessibility locators

If `getByRole` does not work anywhere in your app, that may reveal an accessibility problem. Do not immediately switch to CSS for everything. Talk to developers and improve the UI contract.

###### Pitfall 5: not training manual testers on trace reading

Manual testers moving into automation often think coding is the whole skill. It is not. Trace reading, failure triage, and risk-based test selection are equally valuable. In many Bengaluru product companies, that is the difference between “automation executor” and SDET ownership.

##### Key takeaways

This Playwright TypeScript checklist is not a certificate that says your framework is perfect. It is a practical release-readiness scan. Run it before you add another hundred tests.

- Keep versions, scripts, and config explicit.
- Use user-facing locators and business-level assertions.
- Move repeated setup into typed fixtures, not copy-pasted helpers.
- Use storage state for authentication and keep secrets out of Git.
- Capture traces, screenshots, videos, and reports for CI failures.
- Review flaky tests as engineering work, not QA noise.
- Make ownership visible through tags, annotations, and triage notes.

If you completed the first 21 days, this bonus day is your final audit. Pick one existing test suite, run the checklist, and fix the top three gaps before writing new tests. That is how a Playwright TypeScript checklist becomes a real engineering habit.

##### FAQ

###### Is this Playwright TypeScript checklist only for large teams?

No. Small teams need it even more because they have less time to debug weak automation. Start with selectors, config, traces, and CI artifacts. Add the deeper fixture and data rules as your suite grows.

###### Should every Playwright test use Page Object Model?

No. Use page objects when they reduce duplication without hiding the journey. For short flows, a clear spec file can be better than a page object full of one-line wrappers.

![](https://secure.gravatar.com/avatar/4cf909139a878a25bd3fa83a0d15909cda4cd3233c157e1f37f60dbd19c9d7b1?s=80&d=mm&r=g)

**[Promode](https://scrolltest.com/author/admin/)**

[Facebook](http://techdutta/ "Follow Promode on Facebook") [X](https://twitter.com/itstechmode "Follow Promode on X formerly Twitter")

### 15. Butch Mayhew / Playwright Solutions — Butch Mayhew author archive (page 1)

- Source: https://playwrightsolutions.com/author/butch/
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Butch Mayhew

![Butch Mayhew](https://www.gravatar.com/avatar/5c068a69b4f8523b38a737b88b238e1b?s=250&r=x&d=mp)

- [TOOL: Playwright-Cli-Select For Quick Targeted Test Runs via CLI](https://playwrightsolutions.com/tool-playwright-cli-select-for-quick-targeted-test-runs-via-cli/) — Have you ever struggled to run specific tests from the command line? With this new tool you do not have to autocomplete your way through building the proper command manually.
- [TIP: Playwright Trace Viewer - Copy as Playwright API Request](https://playwrightsolutions.com/tip-playwright-copy-as-playwright-api-request-button/) — With the latest 1.50 Playwright release, there was a really nice feature that was released that will help speed up test automation api requests. The feature is found on the Playwright Trace Viewer which is found in Playwright UI mode or the HTML test report.
- [How to Run a Specific Spec File in Playwright Tests Sequentially](https://playwrightsolutions.com/how-to-run-a-specific-spec-file-playwright-tests-sequentially/) — If you have ever been in a scenario where a test you were writing changed the state of the system or user in a way where other tests would fail? This solution may help you.
- [End of 2024 Announcements](https://playwrightsolutions.com/end-of-2024-announcements/) — Learning Playwright course on LinkedIn Learning released.
- [Playwright Login Test With Two Factor Authentication (2FA) Enabled (TOTP)](https://playwrightsolutions.com/playwright-login-test-with-2-factor-authentication-2fa-enabled/) — Having a way to test 2FA functionality in your CI/CD pipelines within your Playwright tests can ensure coverage of common security measures.
- [Playwright Solutions Challenge: Debug and figure out why the video recording is not in the HTML report?](https://playwrightsolutions.com/playwright-solutions-challenge-debug-and-figure-out-why-the-video-recording-isnt-in-the-html-report/) — A challenge that seemed easy to solve but was surprising, shared so you can exercise your debug skills.
- [[Update v1.46] Is it possible to run only Playwright Tests that changed in GitHub actions on a pull request?](https://playwrightsolutions.com/update-v1-46-is-it-possible-to-run-only-playwright-tests-that-changed-in-github-actions-on-a-pull-request/) — This is now possible without any external scripts. It is all baked into the Playwright CLI.
- [How Do I Combine Playwright HTML Reports After Running Multiple Playwright Commands?](https://playwrightsolutions.com/how-to-combine-playwright-html-reports-after-running-multiple-playwright-commands/) — Running a set of tests with 3 workers and then a separate test run with 1 worker, and combining the reports.
- [How Do You Scroll To The Bottom Of An Infinite Scrolling Page In A Playwright Test?](https://playwrightsolutions.com/how-do-you-scroll-to-the-bottom-of-an-infinite-scrolling-page-in-a-playwright-test/) — An interesting page interaction while limiting the amount of responses within a page.
- [How To Load a Custom Test Fixture or Setup Projects When Running Playwright Test Code Generator](https://playwrightsolutions.com/how-to-load-a-custom-test-fixture-or-setup-projects-when-running-playwright-test-code-generator/) — The Playwright Test Generator is very useful to quickly identify useful locators and generate test code and assertions, though the code is not final.
- [A Few Thoughts On Flakey Tests](https://playwrightsolutions.com/a-few-thoughts-on-flakey-tests-playwright-solutions/) — When a new test automation project starts there are really 3 buckets of work: building, maintaining, and monitoring.
- [How To Run Failures Only From The Last Playwright Run Via CLI](https://playwrightsolutions.com/how-to-run-failures-only-from-the-last-playwright-run/) — Playwright 1.44 adds official support to run failures only from the last run: `npx playwright test --last-failed`.

[See all](https://playwrightsolutions.com/page/2/)

### 16. Butch Mayhew / Playwright Solutions — Load a Custom Test Fixture or Setup Projects When Running Code Generator

- Source: https://playwrightsolutions.com/how-to-load-a-custom-test-fixture-or-setup-projects-when-running-playwright-test-code-generator/
- Retrieved: 2026-08-29
- Firecrawl status: complete

I am a big fan of the Playwright Test Generator when I first start writing tests. I find the tool very useful to quickly identify useful locators/selectors, and quickly generate some test code and [assertions](https://playwrightsolutions.com/playwright-release-1-40-includes-ability-to-create-assertions-through-codegen-tool/) from recording my manual actions through a website. Though the code is not perfect, it gives me a good starting place.

##### Limitations of Running Codegen From CLI

I have found that once I have built a test framework up I will typically include setup files and or fixtures to get the website in a state in which I want to start my tests. This may include logging in, setting certain cookies, creating test data, or even setting variables that to use in my tests. Running the code generation tool from the command below will open the codegen tool but will not run any of the setup files in my project, and I will not have access to my authentication cookies.

The example project I am using can be found at [playwrightsolutions/playwright-practicesoftwaretesting.com](https://github.com/playwrightsolutions/playwright-practicesoftwaretesting.com).

##### A Better Way to Launch Code Generator

The work around to this problem is to create a new empty test within my suite, and add the command `await page.pause()`. With this method you will have to run the tests in headed mode for things to work properly. In this example I have used `test.use()` to provide my storageState for my admin login. This file was created during my setup project.

When running this, the command I will use is:

`npx playwright test tests/account/account.spec.ts --headed`

At this point the setup files will run, and it will run the account.spec file until it hits the [await page.pause()](https://playwright.dev/docs/api/class-page#page-pause) command. At this point the test will pause, and you will have access to the Playwright Inspector.

From the Playwright Inspector, you can click `Record` and as you interact with the page it will create a new record session. This unfortunately creates a new script in the inspector, you can set the type to Test Runner, and then Copy and paste code from the inspector to your original file `account/account.spec.ts` file after the pause command.

I found this Playwright Solutions while in the [Playwright Discord](https://discord.com/servers/playwright-807756831384403968). This is a great place to meet other Playwright enthusiasts, ask questions, help others with their questions, and learn from others!

### 17. Butch Mayhew / Playwright Solutions — How to Run a Specific Spec File Sequentially

- Source: https://playwrightsolutions.com/how-to-run-a-specific-spec-file-playwright-tests-sequentially/
- Retrieved: 2026-08-29
- Firecrawl status: complete

If you have ever been in a scenario where a test you were writing changed the state of the system or user in a way where other tests would fail? This solution may help you. While I am a big fan of running my tests at the same time in parallel there are times where this may not be feasible. This is typically when you have a state that needs to be consistent.

##### Scenario

We will use the repo [playwrightsolutions/playwright-practicesoftwaretesting.com](https://github.com/playwrightsolutions/playwright-practicesoftwaretesting.com) and use the `account.spec.ts` to demonstrate the functionality.

The code we will run has 3 tests.

- In the first test we do not make any changes to any data, we only make assertions.
- In the second test we make a change to the first name, last name, and street 1 address.
- In the third test we do not make any changes to any data, only make an assertion.

The problem here is that the 2nd test changes data that test 1 and test 3 rely on: the "Jane Doe" first and last name.

##### Running the tests without options

When running my tests with my default `playwright.config.ts`, the test ran with 3 workers, 1 test for each, and all tests ran at the same time.

When running the tests you can see that the first test failed, and the 2nd test passed. This is because the afterEach block will actually reset the data for every test. I could make this where it only runs the afterEach based on the title, see [A Better Way to Control Before and After Blocks with Test Titles](https://playwrightsolutions.com/a-better-way-to-control-before-and-after-blocks-with-test-titles-in-playwright-test/) article.

Now I could go to my `playwright.config.ts` file and update the amount of workers that should run and set to `1`, or change fullyParallel option to `false` but this would increase the full time of all of my tests running.

The good news is there is another option, to limit the way our tests run from within a test.describe() block.

##### Running the tests with the default configuration option

To configure this add the line `test.describe.configure({ mode: "default" });` within your describe block. The [docs](https://playwright.dev/docs/api/class-test#test-describe-configure) for this walk through the different options.

This default option will allow tests to run one at a time in order within the describe block of a spec file. This allows you to override the default configuration for the rest of your test suite for a specific set of tests.

With this in place your tests are all passing again, and I still get the benefit of having the rest of my automation suite running in parallel.

There is another option for achieving the same result: setting the `test.describe.configure({ mode: "serial" });`. The difference with using serial is if any of the tests fail while running in serial mode, the following tests that are part of the describe block will be `skipped`. This could be useful if you have tests that build data for other tests, but that is a practice I would not recommend. Each test should create and clean up its own test data.

##### Big Thanks

One thing to note is the option `default` must be specified in the test if you want to use it, which is a bit counter intuitive, as noted by [Stefan Judis](https://www.linkedin.com/in/stefan-judis/) in a recent LinkedIn post. He also covered this in a recent YouTube video with Checkly, "How to Run Playwright Test in Parallel, Serial, or Default Mode".

### 18. Butch Mayhew / Playwright Solutions — Trace Viewer — Copy as Playwright API Request

- Source: https://playwrightsolutions.com/tip-playwright-copy-as-playwright-api-request-button/
- Retrieved: 2026-08-29
- Firecrawl status: complete

With the latest [1.50](https://playwright.dev/docs/release-notes#version-150) Playwright release, there was a really nice feature that was released that will help speed up test automation api requests. The feature is found on the Playwright Trace Viewer which is found in Playwright UI mode or the HTML test report.

##### From Playwright Test Report

To be able to view your traces after a test run within the `playwright.config.ts` file you will need set 2 values.

First - `reporter`: "html". In the example I have multiple reporters active which is why I use the array syntax but if you just need 1 reporter you can pass in "html".

Second - in the use section you want to set `trace`: "on". This will ensure that any tests that are run the trace file is saved for inspection. Feel free to adjust this when you are ready to commit your changes to your repository, as it is likely you do not need the trace on all the time. I tend to use the value "retain-on-failure".

With the configuration you can run your playwright tests `npx playwright test` and in the html report that either pops up after your test run if there is a failure or you can run manually using the command `npx playwright show-report`, you will want to click into a specific test. Scroll down to the `Traces` section and click on the thumbnail image.

![Playwright report traces section](https://playwrightsolutions.com/content/images/2025/02/image-2.png)

Once clicked you can find the test step, click the network tab, and find the http request you want to copy. In my example I found the `POST` request to the users/login api endpoint and on the `Request` tab I scrolled down and see the "Copy as Playwright" button.

Clicking the button copied the request code into my clipboard.

WHAT! That was crazy easy! The Copy as cURL and Copy as Fetch are also easy shortcuts that can be used, but having a button to copy the code to be used in a Playwright test is great!

##### From Playwright UI mode

To enter ui mode you can run the command `npx playwright test --ui`. This will pull up a UI mode with tests on the left and the same trace viewer on the right, that has the same network tab and Copy as Playwright button once you have a request selected.

![Playwright UI mode trace viewer](https://playwrightsolutions.com/content/images/2025/02/image-3.png)

##### Why This is Incredible?

One of my beliefs about automation is one of the hardest parts is creating test data. This functionality will allow you to quickly inspect HTTP requests that are made through the UI and generate code to re-create those request with Playwright.

This is a major shortcut, I will no longer have to closely inspect and copy and paste headers, parameters, or a request body, I just have to make a valid api call through a test (typically using the UI) and then view the trace.

###### Using this to create test data

For a live example I use this repo, which includes a suite of tests and the latest version of Playwright configured: [playwrightsolutions/playwright-practicesoftwaretesting.com](https://github.com/playwrightsolutions/playwright-practicesoftwaretesting.com).

With our existing tests, I want to stop relying on static data for my test assertions but rather want to create test data as a part of my test setup. Ideally I want to do this from the API rather than from the UI. In this repository, I already have a setup project which will create authentication for each type of user (read more: [handling multiple login states between different tests](https://playwrightsolutions.com/handling-multiple-login-states-between-different-tests-in-playwright/)).

I focused on adding a brand on the site under test. I created a UI test that will add a brand through the brand page, ran the test, viewed the test report/trace file and copied the code.

The pasted request had a lot more information because the request was made from the browser with many headers we do not need for the data creation process. I modified it to only the essentials, added import statements because I wanted this to be called from a file outside of a test. I decided to use the `request` method rather than the `page` method because we are not working inside a browser for this. I also used interpolation on the brandName and slugName, because the Trace Viewer copy functionality surrounds the `data` with back ticks.

The final test makes a call to `createBrand()` which uses the newly copied Playwright request.

This `Copy as Playwright` button will save you a lot of typing in your future test automation development. Building out tests with a data factory this way allows me to bypass the UI to create test data during my test runs, which should in turn speed up my tests, and I now have a quick way to create future test data.

### 19. Butch Mayhew / Playwright Solutions — Combine Playwright HTML Reports

- Source: https://playwrightsolutions.com/how-to-combine-playwright-html-reports-after-running-multiple-playwright-commands/
- Retrieved: 2026-08-29
- Firecrawl status: complete

##### A Fun Challenge

I recently had someone reach out to me with this question. They wanted to run a set of tests with 3 workers and then a separate test run with 1 worker and the command looked something like this.

`npx playwright test --grep-invert @api --workers=3 && npx playwright test --grep @api --workers=1`

Their question was at the end of the test, there were 2 HTML reports, and just wanted 1 combined html report. Previously I wrote about how I managed merging reports when sharding in CI.

After writing that article the Playwright team released the [merge-report tool in version 1.37](https://playwright.dev/docs/release-notes#new-npx-playwright-merge-reports-tool). This release provided a new "blob" reporter along with some extra code which we will utilize to solve the problem at hand.

##### The Blob Reporter

When utilizing the "blob" reporter this will create a folder in the root directory named "blob-report" and will generate a report-{randomcharacters}.zip. The idea is you would copy each report.*.zip file to a main directory all-blob-reports after your test runs, and then run the `npx playwright merge-reports --reporter html ./all-blob-reports` command. This will combine the blob files and generate an HTML test report.

![Blob report folder](https://playwrightsolutions.com/content/images/2024/08/image.png)

The challenge which is easier to solve in CI with github actions, can also be achieved when running on your local machine to combine reports. This is where the extra code comes in.

One important note in my `playwright.config.ts` I went ahead and added the option to my HTML reporter to `["html", { open: "never" }]`. This way if there is a test failure, this action will not interrupt us copying and merging reports in the future steps.

##### Creating a Utility to Copy Blob Files

We use a file called `updateBlob.ts`. Running this script will copy a `report.*.zip` file from the `blob-report` folder and copy it to the `all-blob-report` folder. This script also allows you to pass in a command line argument `clean` which will remove all files from the `all-blob-report` folder. With all of these together we can chain some commands from the terminal and achieve our goal.

##### The Final Command

`npx playwright test --grep-invert @api --workers=3 || true && npx ts-node updateBlob.ts && npx playwright test --grep @api --workers=1 || true && npx ts-node updateBlob.ts && npx playwright merge-reports --reporter html ./all-blob-reports && npx playwright show-report`

One thing to note is I added `|| true` to the end of the playwright commands. What this does is makes it so that if the playwright test fails then it will not exit with a non-zero exit code. Without this here, if there was a failure with running any of the tests the reports would not merge with this command.

The commit for the changes discussed can be found in the [playwright-demo repository](https://github.com/BMayhew/playwright-demo/commit/3b713d32a55ad8e28c0411d28022d2a5a13aa498).

Going through this exercise helped me learn about how the blob reports work along with learning about how to prevent commands from exiting with non-zero status codes.

### 20. Butch Mayhew / Playwright Solutions — A Few Thoughts on Flaky Tests

- Source: https://playwrightsolutions.com/a-few-thoughts-on-flakey-tests-playwright-solutions/
- Retrieved: 2026-08-29
- Firecrawl status: complete

When a new test automation project starts there are really 3 buckets of work that happen. **building** (creating new tests), **maintaining** (updating existing tests as the application changes), and **monitoring** (keeping an eye on automation runs, investigating failures and surfacing findings to the team).

"Flakey" tests really come into play within the **monitoring** of the automation runs. If a test is flakey it is always important to dive in and understand why the original test failed. This is important; if this step is not being taken by people, there is a potential for big risks.

Below I will give a few things I have experienced with flakiness from my context; this list is by no means comprehensive.

##### What I Consider Flakey Tests

- Tests that failed due to getting stuck in a bad state. For example, you expected to be logged in but were in a logged out state, or maybe another test/user touched the data you were asserting.
- Test data that you used for your test was poor. For example you sent an 11 digit phone number or you sent a date time stamp with the wrong UTC offset because the machine running your automation is different than your local timezone.
- Is there a race condition in the automation code where it is trying to make assertions before data is loaded within the page?
- Did the test timeout because you did not have await syntax implemented properly?

It is critical that you investigate each failure.

For each of these scenarios just because a test fails every so often does not mean that it is a flakey test. **It is critical that you investigate each failure** and ensure that the issue is not with the application under test but rather some other factor. It is possible there is an underlying issue causing the test to fail that is actually a real bug that needs to be resolved. One example I can think of was a bug in our system when a test would pass at 6:00 PM but fail at 11:00 PM, due to a timezone bug in the application.

##### How Do You Handle Flakey Tests?

Building your tests in a way where the tests are flakey is really easy to do (I have done it on a few projects even when I was aware and trying not to). Test Data and State are the biggest offenders that I have run into and must be considered at the beginning and throughout a test automation project.

If your main issue is with state, and due to specific cookies, the Playwright team released some new functionality to clear certain cookies that can be found in the [1.43 release notes](https://playwright.dev/docs/release-notes#version-143).

When you do have flakey tests you determine if the test is still a valuable test. **If the test is not valuable delete it!** If it is valuable either fix it or set it to run as a part of a set of tests that do not run frequently and are non-build blocking.

One way I have handled keeping flakey tests is through Playwright tags. I specifically use `@unsatisfactory` as the tag name. I actually have around 70 api tests in my day job where we have categorized this way in my suite at work and we run them once a week to give us feedback, but not on every build. With Playwright `npx playwright test --grep-invert @unsatisfactory` you can run all your specs except the ones tagged `@unsatisfactory` in your main CI pipeline.

If you are looking to implement tags do check out the [1.42 release notes](https://playwright.dev/docs/release-notes#version-142) for the newest way to create tags in your tests. You no longer have to add the tag in the test title (though you still can if you would like).

##### What I Consider Flakey Environment/Infrastructure

- Did a 3rd party service fail?
- Did the service/api call hit a rate limit?
- Is the test passing every other run because we only have the latest code deployed to half of the running servers?
- Did a container crash because we are running on the cheapest server possible because it is a test environment?
- Did the automation run during a chaos test or load test?
- Did a developer make a code push and cause the environment to recycle?
- Did the data team hammer the database to rebuild their data warehouse?
- Is AWS experiencing an outage in your region?

There are many different scenarios where your environment or infrastructure could cause tests to return as "failed". It is critical that in these scenarios, as with the flakey tests above, that you investigate each failure and conclude that there is not a real issue. Do not just make an assumption; dive down into the failure messages, network traces, and infrastructure logs/dashboards to ensure the test failure reason.

I have had a few scenarios where tests just started intermittently failing, and after the first day I was convinced it was a flakey infrastructure problem, but I decided day 2 to look a little deeper. I checked our pganalyze tool which gives insight into our database queries, and clearly saw the issue was due to a new query taking 20x as long to complete. After working with a developer, we added an index to the table and we fixed the bug before it made it to production!

If you have accepted there may be an endpoint or test that fails but you still want to test and ensure that it at least passes 1 out of X times, you can try utilizing [expect.poll()](https://playwright.dev/docs/test-assertions#expectpoll) or [expect.toPass()](https://playwright.dev/docs/test-assertions#expecttopass) in your tests. I have found the best place to use these is when "creating test data" prior to tests running, as it can hide underlying problems with your system if you use them on your actual test assertion steps.

You can also make tests less flakey by mocking certain requests and/or responses. The [Playwright docs](https://playwright.dev/docs/mock#mock-api-requests) give lots of good examples!

##### Guard Against Introducing Flakey Tests Into Your Project

Ideally you do not want to introduce flakey tests into your repo. The best way to guard against this is to run your tests multiple times before code gets merged into your test repository. Running new automated tests or edits to existing tests a few times on a pull request to check for flakiness prior to merging the new code into the main branch of the test suite is a good practice.

Something I think we can all agree on:

- Flakey Pie Crust = GOOD
- Flakey Test = BAD

### 21. Currents.dev — Component testing

- Source: https://currents.dev/posts/playwright-component-testing
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Component Testing in Large Frontend Codebases

Not every component needs a real browser. Here's a clear decision framework for when Playwright component testing adds value and when Jest already covers it.

![Playwright Component Testing in Large Frontend Codebases](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-component-testing%2Fcover.png&w=3840&q=100)

Playwright Component Testing (CT) runs components in a real browser without spinning up your full application. That puts it between Jest (fast, no browser) and E2E tests (real browser, full stack). In small projects the distinction barely matters. In design systems, multi-package monorepos, and shared component libraries, it does.

Unit tests don't render in a browser. E2E tests do, but they bring the entire application with them. That leaves a gap: bugs that only appear when a component runs in a live browser environment without the full stack around it. Layout calculations, CSS cascade behavior, `IntersectionObserver`, viewport-relative positioning. JSDOM can't catch these. CT can.

CT is still experimental. The API can change between minor versions without deprecation warnings. A community member [proposed an alternative architecture](https://github.com/microsoft/playwright/issues/22302) in 2023. The Playwright team said they'd considered a similar approach but had "no plans to do so in the near future" since their focus was on end-to-end testing. When the community [asked when CT would leave experimental status](https://github.com/microsoft/playwright/issues/34751), a maintainer acknowledged interest and tagged a team member to look at it, but no public timeline or stable release has materialized since. If you've been waiting for stability guarantees before adopting CT, you'll be waiting a while. The question is whether pinning versions and absorbing upgrade work is worth the tradeoff for your codebase.

This guide covers the architecture decisions that determine whether CT is the right choice, and how to scale it without getting burned by breaking changes.

##### **What Playwright Component Testing Actually Is (And Isn't)**

###### **The Rendering Model and the Node-Browser Boundary**

Playwright CT runs on a dual-environment model. [Node.js](https://nodejs.org/en/about/) orchestrates the test while components render in a real browser served through a [Vite dev server](https://vite.dev/guide/). This split introduces a serialization boundary that shapes how you write tests.

Only JSON-serializable data crosses that boundary. Plain objects, strings, numbers, and primitives pass through cleanly. Complex runtime values like class instances, database connections, and Node's `process` object do not. Functions passed as props become async dispatchers internally, so any prop function expected to return a value synchronously will return undefined inside the component. This is a [known limitation](https://github.com/microsoft/playwright/issues/17254) that breaks render-prop patterns in libraries like Formik and React Hook Form.

This means you can't define test components inline in your test file. [Playwright's docs recommend](https://playwright.dev/docs/test-components) a "story wrapper" approach: a dedicated wrapper component that constructs complex objects in the browser and exposes only serializable signals back to the test. In larger codebases, this becomes the standard pattern for any component with non-trivial props.

The payoff for this constraint is real browser rendering. Layout, styling, fonts, viewport behavior, `IntersectionObserver`, `ResizeObserver`, and every other browser API behave exactly as they would in production. JSDOM doesn't replicate any of this.

###### **What It Is Not**

CT is not a replacement for Jest and Testing Library on logic-heavy components. If a component's correctness depends on state transformations, data formatting, or callback logic rather than browser rendering, Jest is faster and sufficient.

CT also mounts components in isolation. Tests that require routing, authentication, backend state, or cross-component flows belong in E2E.

Visual regression in CT works through the same `toHaveScreenshot()` mechanism Playwright uses for E2E. Baselines are generated on first run and committed to the repository. But CT doesn't give you a component-aware visual workflow on top of that. No story-style organization, no per-component approval flow, no dashboard for reviewing snapshots across runs. If you need that layer, pair CT with Storybook and Chromatic.

###### **The Experimental Label: What It Actually Means for Adoption**

Experimental is not a formality. Config options and hook signatures have changed across releases without deprecation warnings. The core `mount` API has remained stable, but everything around it is fair game. The community has [asked repeatedly](https://github.com/microsoft/playwright/issues/34751) when CT will leave experimental status. A maintainer assigned someone to look at it, but no public timeline has followed.

[Playwright 1.59](https://playwright.dev/docs/release-notes), released April 2026, removed `@playwright/experimental-ct-svelte` entirely. No deprecation warning beforehand. If you're using Svelte, you now have two paths: migrate component tests to a custom setup on the main `@playwright/test` runner, or pin to Playwright 1.58 indefinitely. The migration isn't a config switch. Standard `@playwright/test` has no built-in `mount` fixture for Svelte components, so you need your own Vite config, HTML entry point, and mount helper. This is what breaking changes in experimental packages look like.

The practical response: pin `@playwright/experimental-ct-*` to exact versions. Test upgrades in a separate branch before merging. Budget for migration work if the architecture changes. Whether that tradeoff is worth it depends on where CT fits in your testing stack.

##### **Where Playwright CT Fits in the Testing Pyramid**

[Martin Fowler's test pyramid](https://martinfowler.com/articles/practical-test-pyramid.html) places fast unit tests at the base, integration tests in the middle, and slow E2E tests at the top. Playwright CT sits in that middle layer, but only when a component’s correctness depends on real browser rendering or native browser APIs.

The decision follows directly from what JSDOM can and cannot do:

- **`<DataGrid>` with** **virtual scrolling and sticky headers:** CT. JSDOM does not compute layout, which means scroll behavior and sticky positioning can pass in test and fail in production.

- **`<LoginForm>`** **calling** **onSubmit** **with email and password:** Jest. Reaching for CT here is the most common misstep. There is no browser behavior to validate, only callback logic that Jest handles directly.

- **`<Tooltip>`** **positioning based on viewport edges:** CT. The logic depends on `getBoundingClientRect`, and JSDOM's implementation is incomplete enough to produce false positives.

- **`<AuthProvider>`** **managing tokens in context:** Jest. Routing this through CT adds browser overhead to a test that relies entirely on state.

- **`<FileUploader>`** **using drag-and-drop and the File API:** CT. JSDOM's File API stubs break under real interaction patterns, and those issues often appear only in production.


The harder cases are components that mix both. A `<DateRangePicker>` has callback logic (date validation, formatting) and browser-dependent behavior (calendar dropdown positioning, focus management). Don't test everything with CT just because one part needs a browser. Split it: Jest for the date validation logic, CT for the dropdown positioning and keyboard navigation. If the component is well-factored, the logic lives in a hook or utility that you test separately.

The practical threshold: if fewer than ~10 components in your codebase need real-browser validation, skip the CT pipeline entirely. Mount those components in your E2E suite instead. The overhead of maintaining a separate CT config, CI job, and upgrade process isn't worth it for a handful of tests. CT starts paying off when you have a shared component library or design system where 30+ components need rendering validation independent of any application.

###### **The Cost Model**

CT starts a real browser and Vite dev server once per worker. The first test in each file pays that startup cost (typically 2-5 seconds for Vite's cold start, depending on your dependency tree). After that, the Vite server stays warm and CT [reuses the `context` and `page` fixture](https://playwright.dev/docs/test-components#frequently-asked-questions) across tests in the same file, resetting both between each test.

To put this in perspective: a Jest unit test runs in single-digit milliseconds. A CT test runs in 50-200ms after warm-up, plus the per-worker startup cost amortized across tests. For a file with 20 CT tests, you're looking at roughly 5-8 seconds total vs. under a second in Jest. That overhead is worth it when you're testing real browser behavior. It's waste when you're testing callback logic.

Each CT worker also holds a browser process and a Vite dev server in memory. On CI runners with 4GB RAM, you'll typically max out at 3-4 parallel CT workers before hitting memory pressure. E2E suites face the same browser constraint, but CT adds the Vite process on top. If you're running CT alongside E2E in the same pipeline, budget your worker count accordingly.

The context/page reset between tests is [described in the docs](https://playwright.dev/docs/test-components#frequently-asked-questions) as "functionally equivalent" to getting a fresh context per test. In practice, watch for global side effects that survive the reset: a component that appends a `<style>` tag to `document.head`, registers a `document`-level event listener, or mutates `window` globals can leak state into the next test. If test A adds a global stylesheet and test B assumes a clean DOM, you get order-dependent failures that only appear when tests run in sequence within the same file. Group related component tests in the same file, but keep unrelated components in separate files so they get separate browser contexts.

##### **Common Pitfalls**

CT adoption rarely fails at scale. It fails in the first week, during setup and initial integration.

![CT Adoption Pitfalls](https://currents.dev/img/posts/playwright-component-testing/pitfalls.jpg)CT Adoption Pitfalls

**Vite plugin conflicts.** Playwright CT doesn't reuse your existing `vite.config.ts`. Server-side or environment-specific plugins fail silently in the CT context. Don't assume plugins carry over. Audit each one.

**Serialization boundary surprises.** If you're coming from Jest, you expect to pass any object as a prop. Complex objects, function return values, and inline component declarations all break across the Node-browser boundary. The errors are often cryptic (e.g., `undefined` where you expected a callback result). Document these failure modes early and make the story wrapper pattern the default.

**Accidental E2E/CT config overlap.** Overlapping `testMatch` patterns between `playwright.config.ts` and `playwright-ct.config.ts` route specs through the wrong runner. This is easy to miss because both runners execute successfully. Keep patterns mutually exclusive (e.g., `*.ct.spec.ts` vs `*.e2e.spec.ts`) and verify them after every Playwright upgrade.

**Provider tree drift.** The `beforeMount` hook wrapping components with providers can gradually fall out of sync with the production provider tree. The result: CT passes while production fails due to a missing provider. Treat `playwright/index.tsx` as production code. Review it in PRs, test it, and update it whenever your provider tree changes.

**Over-testing with CT.** If you're writing CT tests for a `<LoginForm>` that just calls `onSubmit`, you're paying browser overhead for something Jest handles in milliseconds. Each CT test should answer a question Jest can't: a browser API interaction, a layout dependency, or a CSS behavior that only appears in a real rendering engine.

**Version pinning neglect.** In monorepos where packages update independently, unpinned `@playwright/experimental-ct-*` versions create silent version skew. One package runs CT on 1.58, another on 1.59, and suddenly your Svelte CT tests stop compiling. Pin to exact versions across every package.

**CSS Modules naming.** Vite requires CSS Modules files to use the `*.module.[ext]` convention. Projects importing CSS files without the `.module` prefix fail in CT with no useful error message. This comes from Vite, not Playwright, but it surfaces during CT adoption when the main app uses a more flexible bundler like webpack.

##### **Architectural Setup in Large Codebases**

Large codebases expose CT configuration problems that don't appear in smaller setups. The decisions you make at setup (monorepo structure, Vite config, file organization, TypeScript aliases) determine whether CT scales cleanly.

![CT Configuration in Large Codebases](https://currents.dev/img/posts/playwright-component-testing/architecture.jpg)CT Configuration in Large Codebases

###### **Monorepo Placement and Configuration**

Root-level configuration is where monorepo setups run into trouble. A root-level `playwright-ct.config.ts` forces every package's Vite plugins, path aliases, and environment assumptions into a single `ctViteConfig`. That works until two packages rely on incompatible plugin versions or conflicting aliases. At that point, the config becomes a negotiation between unrelated packages, and the CT setup becomes more difficult to work with.

Package-level CT configs are the more durable approach for monorepos with diverging Vite needs. The cost is real: a 30-package monorepo means 30 separate `playwright-ct.config.ts` files, each carrying its own plugin list, alias map, and version pins. Extracting a shared base config into an internal package reduces that overhead, but each package still requires its own config file.

For monorepos where packages share identical Vite setups, a root-level config is fine. For those with genuine Vite divergence, per-package isolation is worth the maintenance cost.

###### **Vite Config: What Actually Happens**

The official guidance is clear: "Playwright is bundler-agnostic, so it is not reusing your existing Vite config." Every path alias and plugin must be manually copied into the `ctViteConfig` property inside `playwright-ct.config.ts`:

```typescript
// playwright-ct.config.ts
import { defineConfig } from '@playwright/experimental-ct-react';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  use: {
    ctViteConfig: {
      plugins: [react()],
      resolve: {
        alias: {
          '@': path.resolve(__dirname, './src'),
        },
      },
    },
  },
});
```

CopyCopied!

You can use Vite's `loadConfigFromFile` to pull in an existing config dynamically, but it's not automatic. There's a longstanding behavior ( [#19829](https://github.com/microsoft/playwright/issues/19829), reported in 2023 and closed without a fix) where `ctViteConfig` overwrites `resolve.alias` and `plugins` arrays rather than merging them. Setting a custom alias or adding a plugin replaces Playwright's injected entries. This means you must include `@vitejs/plugin-react` or `@vitejs/plugin-vue` in the `plugins` array every time you customize `ctViteConfig`, or the component bundle will fail to build with no useful error message.

###### **Test File Organization**

Place CT tests with the components they cover. CT tests are tightly coupled to a single component's rendering behavior, not to application flows. When a component moves, its CT test moves with it. Separation makes sense for E2E tests that span multiple pages, not for CT tests focused on one component.

Use distinct file suffixes to prevent CT and E2E specs from overlapping. Patterns like _`.ct.spec.ts`_ and `.e2e.spec.ts` keep `testMatch` configs separate. Verify those patterns during Playwright version upgrades.

###### **TypeScript Path Aliases**

Alias resolution must stay consistent across both `ctViteConfig` and `tsconfig.json`. Imports that work in component source files but fail in CT test files usually trace back to aliases defined in `tsconfig.json` but missing from `ctViteConfig`. Duplicate them in both places.

##### **Mounting Strategies for Complex Components**

Mounting a simple component in CT is straightforward. Mounting one that includes providers, complex props, network dependencies, or lifecycle behavior requires patterns that remain reliable across a large test suite.

###### **The beforeMount/afterMount Hooks System**

The `beforeMount` and `afterMount` hooks live in `playwright/index.ts` and run entirely in the browser. They wrap components with providers, routers, and state stores before or after mounting, and support a `hooksConfig` parameter for per-test customization.

```typescript
// playwright/index.tsx (React)
import { beforeMount, afterMount } from '@playwright/experimental-ct-react/hooks';
import { BrowserRouter } from 'react-router-dom';

export type HooksConfig = { enableRouting?: boolean };

beforeMount<HooksConfig>(async ({ App, hooksConfig }) => {
  if (hooksConfig?.enableRouting) {
    return <BrowserRouter><App /></BrowserRouter>;
  }
});

afterMount<HooksConfig>(async ({ hooksConfig }) => {
});
```

CopyCopied!

The configuration style varies by framework. In Vue, you call `app.use()` inside `beforeMount`. In React, you wrap with JSX.

A common mistake is building a custom `mountWithProviders` helper that replicates this behavior across test files. This scatters provider logic and makes drift harder to detect. The hooks file is the single source of provider configuration. Don't duplicate it.

###### **The Story Wrapper Pattern**

Components with function props, render props, or class instances cannot pass complex objects across the Node-browser boundary. The [story wrapper pattern](https://playwright.dev/docs/test-components) addresses this by introducing a wrapper component that runs in the browser and constructs the required objects internally.

It accepts only serializable props from the test, handles complex setups internally in the browser, and signals results back through simple primitives:

```typescript
// CheckoutFormWrapper.tsx — lives in the browser, NOT in the test file
import { CheckoutForm } from './CheckoutForm';

export function CheckoutFormWrapper({
  onSubmitCalled,
}: {
  onSubmitCalled: () => void;
}) {
  return (
    <CheckoutForm
      onSubmit={() => onSubmitCalled()}
      validationSchema={buildSchema()}
    />
  );
}
```

CopyCopied!

Then in your test file:

```typescript
// CheckoutForm.ct.spec.tsx
import { test, expect } from '@playwright/experimental-ct-react';
import { CheckoutFormWrapper } from './CheckoutFormWrapper';

test('form submits', async ({ mount }) => {
  let submitted = false;
  const component = await mount(
    <CheckoutFormWrapper onSubmitCalled={() => { submitted = true; }} />
  );
  await component.getByRole('button', { name: 'Submit' }).click();
  expect(submitted).toBe(true);
});
```

CopyCopied!

Schema construction, class instantiation, and render prop logic all stay in the browser where they belong. The test only sees the boolean callback that crosses the boundary cleanly.

If you already use Storybook, there's a more direct adoption path. [Storybook's portable stories API](https://storybook.js.org/docs/api/portable-stories/portable-stories-playwright) lets you run stories natively in Playwright CT. The pattern uses `createTest` from Storybook's Playwright integration:

```typescript
import { createTest } from '@storybook/react/experimental-playwright';
import { test as base } from '@playwright/experimental-ct-react';
import stories from './Button.stories.portable';

const test = createTest(base);

test('renders primary button', async ({ mount }) => {
  const component = await mount(<stories.Primary />);
  await expect(component).toContainText('Click me');
});
```

CopyCopied!

The story carries the props, decorators, and parameters the component needs. The test focuses only on the assertion. Two constraints: portable stories require React 18 or later and are not currently supported in Next.js projects.

###### **Network Mocking with the Router Fixture**

The experimental `router` fixture intercepts network requests at the component level. `router.route(url, handler)` works like `page.route()` in standard Playwright tests. `router.use(...handlers)` accepts [MSW](https://mswjs.io/) request handlers, so you can reuse mocks from your development setup. For a broader overview of request interception patterns, see [the Playwright network mocking playbook](https://currents.dev/posts/the-playwright-network-mocking-playbook).

Install shared handlers in `test.beforeEach` and add per-test overrides with individual `router.use()` calls. This keeps network behavior explicit at the test level rather than buried in Vite configuration.

One risk worth naming: the `router` fixture carries its own experimental label on top of CT's. The core `mount` API has held steady across releases, but `router` is newer and has higher change risk. If you'd rather avoid that, `page.route()` works fine as the stable fallback with slightly more verbose setup.

###### **Module Mocking**

Playwright CT has no `jest.mock()` equivalent. Module-level mocks [run in the test process, not in the browser](https://playwright.dev/docs/test-components#module-mocks-do-not-cross-the-nodebrowser-boundary), so they don't affect what the component imports at runtime. This is the single biggest friction point when migrating from Jest to CT.

For dependencies that aren't network requests, three approaches work:

**Dependency injection via props or context.** The cleanest option when you control the component's API. Instead of importing an analytics SDK directly, accept it as a prop or read it from context. The test passes a stub. This is good architecture anyway, but retrofitting it onto existing components takes effort.

**Vite plugin-based import replacement inside `ctViteConfig`.** Use Vite's `resolve.alias` to swap a module at build time. For example, if your component imports `@/services/analytics`, you can redirect it to a stub module during CT:

```typescript
// playwright-ct.config.ts
ctViteConfig: {
  plugins: [react()],
  resolve: {
    alias: {
      '@/services/analytics': path.resolve(__dirname, './test/stubs/analytics.ts'),
      '@/services/feature-flags': path.resolve(__dirname, './test/stubs/feature-flags.ts'),
    },
  },
},
```

CopyCopied!

This works globally across all CT tests, which is both its strength (no per-test setup) and its weakness (you can't vary the mock per test without more indirection). For per-test control, make the stub module export a function that reads configuration from `window`, and set that configuration in `beforeMount` via `hooksConfig`.

**The story wrapper pattern.** The wrapper swaps implementations before the component renders. Most flexible, but adds a file for every component that needs mocking.

If a component requires multiple mocked imports just to render in isolation, that's a signal. CT exposes tight coupling more clearly than Jest does, because there's no escape hatch to mock at the module level. If you find yourself writing five aliases just to mount a component, the component probably needs refactoring more than it needs CT.

###### **Lifecycle Testing with unmount and update**

Playwright CT provides two lifecycle APIs that standard E2E tests don't have:

- [`component.unmount()`](https://playwright.dev/docs/test-components#unmount) removes the component from the DOM. Use it to validate event listener cleanup, subscription cancellation, and navigation guard behavior. If your component sets up a `ResizeObserver` or WebSocket connection, `unmount()` lets you assert it was torn down properly.

- [`component.update()`](https://playwright.dev/docs/test-components#update) re-renders with new props, children, or callbacks without a full remount. One thing that trips people up: `update()` requires the full JSX element again, not just the changed props. You're passing `<Component msg="new" onClick={() => {}} />`, not a partial props object. Functions passed through `update()` hit the same serialization boundary as `mount()`, so they won't return values synchronously.


##### **Scaling CT in CI**

CT doesn't need a backend, a running server, or a seeded database. This changes how it fits into CI. Getting the setup wrong early creates slow pipelines that push you away from CT before you see its value.

###### **Separating CT and E2E Pipelines**

Running CT and E2E in the same CI job is one of the most common mistakes. CT needs `npm ci`, browser binaries, and nothing else. E2E needs all of that plus your application stack, database seeds, and environment config. Mixing them means every CT run pays for infrastructure it doesn't use, and every mixed failure forces you to figure out whether the problem is a component or the backend.

Separate CI jobs. Use distinct commands: `npx playwright test --config=playwright-ct.config.ts` for CT and your standard E2E config for the rest. The failure signal becomes precise: CT failures point to component rendering, E2E failures point to application issues.

###### **Parallelization**

CT suites benefit from the same sharding setup as E2E tests. Since there's no application stack to start, CT jobs boot faster and use fewer resources per worker. You can run CT shards on smaller CI machines than E2E, which saves money at scale.

```yaml
strategy:
  matrix:
    shard: [1/4, 2/4, 3/4, 4/4]
steps:
  - run: npx playwright test --config=playwright-ct.config.ts --shard=${{ matrix.shard }}
```

CopyCopied!

In large monorepos with hundreds of CT specs, sharding across four workers cuts suite time proportionally. For more detail on the tradeoffs between sharding and workers, see [sharding vs. workers](https://currents.dev/posts/optimizing-test-runtime-playwright-sharding-vs-workers).

###### **Caching Strategy**

CT adds one startup cost E2E pipelines don't have: Vite dev server initialization. On a cold CI runner, Vite processes the component bundle from scratch on every run. Two caches address this:

- Cache Playwright browser binaries at `~/.cache/ms-playwright` as usual
- Cache Vite's dependency pre-bundle directory (`node_modules/.vite`) between runs

A warm Vite cache cuts startup time on subsequent CI runs. The difference is most noticeable on the first test file per worker, where Vite processes your component's dependency tree from scratch on a cold cache.

###### **Unified Reporting**

Playwright CT uses the same reporter infrastructure as E2E tests. Assign distinct project names (one for CT, one for E2E) so failures don't merge into a single list. This makes it immediately clear whether a pipeline failure is component-level or application-level without parsing logs.

Any reporting tool that works with Playwright works with CT. [Currents](https://currents.dev/) supports CT runs through the same reporter that handles E2E, surfacing screenshots, traces, and performance metrics alongside each run. The same applies to [debugging CT failures in CI](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci) and general [CI setup patterns](https://currents.dev/posts/how-to-run-playwright-tests-without-the-pain).

##### **Playwright CT vs. the Alternatives**

The right choice depends on what the component needs to cover and how your existing test stack is structured.

| Dimension | Playwright CT | Cypress CT | Jest + RTL | Vitest Browser Mode | Storybook + Vitest |
| --- | --- | --- | --- | --- | --- |
| Real browser rendering | Yes | Yes | No (JSDOM) | Yes | Yes |
| Execution speed | Fast | Moderate | Fastest | Fast | Fast |
| Cross-browser support | Chromium, Firefox, WebKit | Chromium, Firefox, experimental WebKit | N/A | Depends on provider (Playwright or WebDriverIO) | Primarily Chromium |
| Module mocking | Vite plugin only | Vite plugin only | jest.mock() | vi.mock() | vi.mock() |
| Network mocking | router fixture + MSW | cy.intercept() + MSW | MSW / manual | MSW | MSW |
| Native browser API support | Full | Full | Limited, JSDOM stubs | Full | Full |
| CI integration | Standard | Standard | Standard | Standard | Standard |
| Interactive debugging | Trace viewer | Command timeline UI | None | Vitest UI | Storybook UI |
| Component library support | Portable stories | Portable stories | Testing Library | Storybook + Vitest | Native, stories are test |

###### **Playwright CT vs. Jest + React Testing Library**

Jest + RTL runs in JSDOM. No browser startup, no Vite dev server, immediate feedback. Its mocking ecosystem is the most mature of any component testing option, and RTL's `user-event` covers most interaction patterns without a real browser.

Use both together. Jest handles logic-heavy components. CT covers rendering-dependent ones. The decision tree from the testing pyramid section gives you the routing rule. Don't replace Jest with CT. Add CT where Jest can't reach: `getBoundingClientRect`, scroll layout, CSS cascade, viewport behavior, native File API.

If you're on Svelte, note that both `@testing-library/svelte` and Playwright CT for Svelte have thinner community coverage than their React and Vue equivalents. With `@playwright/experimental-ct-svelte` [removed in Playwright 1.59](https://playwright.dev/docs/release-notes), Svelte CT requires a custom setup.

###### **Playwright CT vs. Storybook Test**

If you already run Storybook, the Vitest addon turns stories into tests that run in browser mode using Playwright's Chromium under the hood. You get browser-level rendering with execution speeds closer to Vitest than Playwright CT, because it reuses browser sessions more aggressively.

The tradeoff is isolation. Playwright CT resets context and page between tests, catching state-leak bugs that Storybook Test's session reuse can miss. If your component touches `localStorage`, a global event bus, or singleton state, Playwright CT gives you cleaner isolation. If your tests are mostly presentational assertions (correct class names, visible text, layout checks), Storybook Test is faster and simpler.

###### **Playwright CT vs. Cypress CT**

[Cypress CT](https://docs.cypress.io/app/references/changelog) remains actively maintained. Its interactive runner is better for step-through debugging. The command timeline and time-travel snapshots give you a different debugging experience than Playwright CT's trace viewer, one that's closer to stepping through code than replaying a recording.

Playwright CT is faster per test due to lower setup cost and uses native async/await instead of Cypress's chainable command queue. Cross-browser support across Chromium, Firefox, and WebKit is a differentiator if you need it, though in practice most CT runs target Chromium only.

Stack alignment drives this decision more than feature comparison. If you already run Playwright for E2E, use Playwright CT. You get a unified runner, config model, and reporting pipeline. If you're on Cypress E2E, Cypress CT is less friction. Switching CT frameworks independent of your E2E framework creates maintenance overhead that rarely pays off.

If you're on Angular, the decision is different: Playwright has no official Angular CT package. Your options are Cypress CT or Storybook + Vitest.

###### **Playwright CT vs. Storybook + Chromatic**

[Storybook](https://storybook.js.org/) is a development and documentation tool. Playwright CT is a testing tool. Don't conflate them.

Storybook excels at visual development, design system documentation, and visual regression through Chromatic. CT excels at behavioral assertions in CI. They're complementary, not competing. Storybook's [portable stories](https://storybook.js.org/docs/api/portable-stories/portable-stories-playwright) work natively in Playwright CT, so stories written for documentation become CT fixtures without duplication. If you already have Storybook, adopt CT incrementally by using stories as the foundation for behavioral tests. You get visual regression from Chromatic and behavioral coverage from CT without writing anything twice.

##### **Should You Adopt Playwright CT?**

**Adopt it if:**

- You're shipping bugs that Jest missed because JSDOM doesn't compute layout, doesn't run CSS cascade, or stubs native APIs incorrectly. CT exists to close that gap.
- You maintain a shared component library or design system with 30+ components that need rendering validation independent of any specific application.
- You already run Playwright for E2E and want a single runner, config model, and reporting pipeline across both layers.

**Skip it if:**

- Your components are logic-heavy (state management, data transformation, callbacks) and Jest already catches what breaks. Browser overhead for callback tests is waste.
- Fewer than 10 components need real-browser validation. At that scale, run them in your E2E suite. The overhead of a separate CT pipeline, config, and upgrade process isn't justified.
- You can't commit to version pinning and a consistent upgrade process for an experimental API. If you run `npm update` without checking changelogs, CT will burn you.

CT is its own infrastructure layer. Scope it to components where it provides clear value, run it as a separate CI pipeline, and keep CT and E2E results visible in one place.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![Optimizing Test Runtime: Playwright Sharding vs. Workers](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Foptimizing-test-runtime-playwright-sharding-vs-workers%2Fposter.png&w=3840&q=90)\\
\\
Dec 09, 2025 **Optimizing Test Runtime: Playwright Sharding vs. Workers** \\
\\
![Joshua Adeyemi](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Joshua Adeyemi](https://currents.dev/posts/optimizing-test-runtime-playwright-sharding-vs-workers) [![Playwright HTML Reporter: Why It Breaks Down at Scale](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fplaywright-html-reporter-why-it-breaks-down-at-scale%2Fcover.png&w=3840&q=90)\\
\\
Apr 24, 2026 **Playwright HTML Reporter: Why It Breaks Down at Scale** \\
\\
![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)\\
\\
Currents Team](https://currents.dev/posts/playwright-html-reporter-why-it-breaks-down-at-scale) [![How To Debug Playwright Tests in CI: The Complete Guide](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-debug-playwright-tests-in-ci%2Fposter.png&w=3840&q=90)\\
\\
Jan 27, 2026 **How To Debug Playwright Tests in CI: The Complete Guide** \\
\\
![Dumebi Okolo](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Dumebi Okolo](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci) [![What Breaks When Your Test Suite Grows From 20 to 500 Tests](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fwhat-breaks-when-your-test-suite-grows-from-20-to-500-tests%2Fcover.png&w=3840&q=90)\\
\\
Apr 08, 2026 **What Breaks When Your Test Suite Grows From 20 to 500 Tests** \\
\\
![Currents Team](https://currents.dev/_next/image?url=%2Fimg%2Fcurrents-logo.png&w=64&q=75)\\
\\
Currents Team](https://currents.dev/posts/what-breaks-when-your-test-suite-grows-from-20-to-500-tests)

### 22. Currents.dev — Measure code coverage

- Source: https://currents.dev/posts/how-to-measure-code-coverage-in-playwright-tests
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### How To Measure Code Coverage in Playwright Tests

Learn how to measure and increase Playwright code coverage and ensure your tests stay reliable.

![How To Measure Code Coverage in Playwright Tests](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-measure-code-coverage-in-playwright-tests%2Fposter.png&w=3840&q=100)

Imagine you’re test-driving a new car. You try every drive mode: reverse, park, sport, ultra, and everything works perfectly. But you never test the automatic emergency braking system. Would you really say all the potential fault points were covered? I would say, probably not.

That’s how end-to-end [(E2E) testing](https://playwright.dev/docs/writing-tests) works in software. Your buttons may click, navigation may flow, and forms may submit, but how much of the underlying code actually ran during those tests? Ten percent? Twenty? Maybe half? If large parts of your codebase remain untouched, hidden bugs could still be waiting to surface.

This is where [code coverage](https://playwright.dev/docs/api/class-coverage) comes in. It measures how much of your application code executes when your Playwright tests run, giving you visibility into what’s truly being tested. In this guide, you’ll learn how Playwright handles code coverage, how to set it up, interpret reports, troubleshoot common issues, and follow best practices to make your testing process more reliable.

##### How Playwright Handles Code Coverage

You can measure code coverage either by using Playwright’s built-in [Coverage API](https://playwright.dev/docs/api/class-coverage) or by integrating with external tooling that processes the raw data. Let’s look at both approaches.

**Built-in API**
Playwright provides a dedicated `page.coverage` API that communicates directly with the [V8 JavaScript engine](https://v8.dev/) to collect execution data from your test runs. The outputs are raw and low-level, so reports are in the form of byte offsets and function ranges instead of readable line numbers.

Here’s an example of what the data looks like:

```json
[\
  {\
    "url": "bundle.js",\
    "functions": [\
      {\
        "functionName": "...",\
        "ranges": [{ "startOffset": 120, "endOffset": 300, "count": 1 }]\
      }\
    ]\
  }\
]
```

CopyCopied!

The `page.coverage` API can track both JavaScript and CSS execution, allowing you to analyze how much of your frontend code logic and styling was actually exercised during test execution. The coverage code is embedded within your test because coverage collection happens in real time while your Playwright tests interact with the page.

Typical snippets look like this:

```typescript
await page.coverage.startJSCoverage();
await page.coverage.startCSSCoverage();
// ... run your test actions here ...
const jsCoverage = await page.coverage.stopJSCoverage();
const cssCoverage = await page.coverage.stopCSSCoverage();
```

CopyCopied!

> However, it’s important to note that Playwright’s coverage APIs currently work only in Chromium-based browsers such as Chrome, Brave, or Edge.

**External Tools**
You can integrate external tools to transform Playwright’s raw V8 data into readable coverage reports. These integrations are meant to process the Playwright’s API output into visual or structured formats; they don't replace the API.

One popular approach is to use a tool like [v8-to-istanbul](https://www.npmjs.com/package/v8-to-istanbul), which converts the raw coverage data into [Istanbul](https://istanbul.js.org/)/ [NYC](https://github.com/istanbuljs/nyc) format. This produces results in formats like HTML (HyperText Markup Language) and LCOV (Line Coverage Format) for easier analysis.

```
Example LCOV output:
SF:src/components/Button.js
FN:10,(anonymous_0)
FN:20,handleClick
FNDA:1,handleClick
FNDA:0,(anonymous_0)
DA:12,1
DA:21,1
DA:25,0
LH:2
LF:3
end_of_record
```

CopyCopied!

To generate the HTML report, you run:

```bash
npx nyc report --reporter=html
```

CopyCopied!

The HTML output usually gets sent to the `/coverage/index.html` file path, which displays color-coded lines: green for covered and red for missed.

Another option is to embed plugins like [babel-plugin-istanbul](https://www.npmjs.com/package/babel-plugin-istanbul) or [vite-plugin-istanbul](https://www.npmjs.com/package/vite-plugin-istanbul) directly in your test setup. These tools instrument the code in real time, converting coverage data into NYC format as your tests run.

```javascript
module.exports = {
  presets: [\
    // Your existing presets (e.g. '@babel/preset-env', '@babel/preset-react')\
  ],
  plugins: [\
    (process.env.NODE_ENV === "test" || process.env.VITE_COVERAGE) && [\
      "babel-plugin-istanbul",\
      { exclude: ["**/*.spec.js", "**/node_modules/**"] },\
    ],\
  ].filter(Boolean),
};
```

CopyCopied!

Even with the coverage data generated in human-readable formats, you can use tools such as [Currents](https://docs.currents.dev/guides/coverage) to organize reports, especially when you’re dealing with large test suites or parallel executions. Currents centralizes metrics into dashboards, making it easier to interpret results and track test quality over time.

Now that you have an overview of how Playwright collects and processes coverage, the next step is to set it up in practice using its built-in API.

##### Setting Up Code Coverage in Playwright

Let's use a small demo project to work through how Playwright code coverage works in practice. You'll build a simple web app, write Playwright tests for different user flows, and compare how each one affects your coverage report.

###### About the Demo App

The demo app is a lightweight Node.js checkout form that lets users enter a quantity and price, calculates the total, and loads a mock list of items.

For this walkthrough, the focus is on two realistic test scenarios:

- A happy path where inputs are valid and the total is calculated
- An error path when users enter invalid or negative numbers

The app also includes minimal CSS so you can see Playwright’s CSS coverage in action.

You can checkout the code for the demo app [here](https://github.com/currents-dev/blog-playwright-coverage-demo).

###### Prerequisites

Ensure you have the following installed and available on your computer:

1. **Node.js**: Install the latest [LTS version](https://nodejs.org/en/download).
2. **Node package manager** (npm): It comes bundled with Node.js.
3. **Playwright test runner**

In your project root, create a new folder and install Playwright with Chromium support:

```bash
mkdir playwright-coverage-demo
cd playwright-coverage-demo
npm install --save-dev @playwright/test
npx playwright install chromium
```

CopyCopied!

4. **Static server**

You’ll need a simple static server to serve the demo app. In this tutorial, you’ll use `http-server`:

```bash
npm install --save-dev http-server
```

CopyCopied!

5. **TypeScript support for tests**

Install TypeScript so your `.spec.ts` files compile properly:

```bash
npm install --save-dev typescript
```

CopyCopied!

With the prerequisites boxes checked, let’s get started.

###### Step 1: Populate Dependency Files

a. Having followed the prerequisites carefully, you should already have a `package.json` file. Add the following scripts section:

```json
"scripts": {
  "dev": "npx http-server -c-1 -p 5173 .",
  "test": "playwright test --project=chromium"
}
```

CopyCopied!

Your `package.json` should now look like this:

```json
{
  "devDependencies": {
    "@playwright/test": "^1.56.0",
    "http-server": "^14.1.1",
    "typescript": "^5.9.3"
  },
  "scripts": {
    "dev": "npx http-server -c-1 -p 5173 .",
    "test": "playwright test --project=chromium"
  }
}
```

CopyCopied!

b. Next, in the root of your project, create a `tsconfig.json` file. This file controls how your Playwright TypeScript tests are compiled.

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2020",
    "moduleResolution": "Node",
    "strict": true,
    "types": ["@playwright/test"]
  }
}
```

CopyCopied!

c. Still in your project root, create a `playwright.config.ts` file to define how Playwright runs your tests.

```typescript
// playwright.config.ts
import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  // Run only on Chromium so page.coverage works
  projects: [\
    {\
      name: "chromium",\
      use: { ...devices["Desktop Chrome"] },\
    },\
  ],
  // Optional: stricter test discovery
  // testMatch: /.*\.spec\.ts/,
});
```

CopyCopied!

###### Step 2: Create the App

a. Inside the `playwright-coverage-demo` directory, create the folders and files that will make up the demo app.

Run the following commands from your terminal:

```bash
  mkdir -p src/{utils,ui,services}
  touch src/app.js src/utils/math.js src/ui/validate.js src/services/api.js index.html styles.css
```

CopyCopied!

After running them, your project should look like this:

```
playwright-coverage-demo/
│
├── playwright.config.ts
├── package.json
├── tsconfig.json
├── src/
│   ├── app.js
│   ├── utils/math.js
│   ├── ui/validate.js
│   └── services/api.js
├── index.html
└── styles.css
```

CopyCopied!

This gives Playwright something real to test while keeping the code simple enough to read at a glance.

b. Populate the folders and files with code.

`index.html` hosts the form checkout and buttons that your Playwright tests will interact with.

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>Playwright Coverage Demo</title>
    <link rel="stylesheet" href="./styles.css" />
    <script type="module" src="./src/app.js"></script>
  </head>
  <body>
    <h1>Checkout</h1>
    <form id="order-form">
      <input id="qty" type="number" min="0" placeholder="Quantity" />
      <input
        id="price"
        type="number"
        min="0"
        step="0.01"
        placeholder="Unit price"
      />
      <button id="calc" type="button">Calculate</button>
      <p id="error" class="hidden">Invalid input</p>
    </form>
    <p>Total: <span id="total">0</span></p>
    <button id="load-items" type="button">Load Items</button>
    <ul id="items"></ul>
  </body>
</html>
```

CopyCopied!

`styles.css` defines simple visual feedback states, including a hidden element and error color for CSS coverage tracking.

```css
.hidden {
  display: none;
}
.error {
  color: red;
}
```

CopyCopied!

`src/app.js` handles user interactions and ties all modules together: validation, math logic, and mock API requests.

```javascript
import { calculateTotal } from "./utils/math.js";
import { validateInputs } from "./ui/validate.js";
import { fetchItems } from "./services/api.js";
const qtyEl = document.getElementById("qty");
const priceEl = document.getElementById("price");
const calcBtn = document.getElementById("calc");
const totalEl = document.getElementById("total");
const errEl = document.getElementById("error");

calcBtn.addEventListener("click", () => {
  const qty = Number(qtyEl.value);
  const price = Number(priceEl.value);
  const { ok } = validateInputs(qty, price);
  if (!ok) {
    errEl.classList.remove("hidden");
    errEl.classList.add("error");
    return;
  }
  errEl.classList.add("hidden");
  const total = calculateTotal(qty, price);
  totalEl.textContent = String(total);
});

document.getElementById("load-items").addEventListener("click", async () => {
  const list = document.getElementById("items");
  list.innerHTML = "";
  const items = await fetchItems();
  for (const item of items) {
    const li = document.createElement("li");
    li.textContent = item.name;
    list.appendChild(li);
  }
});
```

CopyCopied!

`src/utils/math.js` implements basic calculation logic, including a total function and a discount helper that isn’t deliberately triggered in the current tests.

```javascript
export function calculateTotal(qty, price) {
  if (qty < 0 || price < 0) return 0; // simple guard
  if (qty === 0) return 0; // edge case we will NOT cover
  return qty * price;
}
// Unused on purpose to show uncovered code
export function legacyDiscount(total) {
  return total * 0.9;
}
```

CopyCopied!

`src/ui/validate.js` performs simple input validation and introduces a few branches for testing coverage across different conditions.

```javascript
export function isValidNumber(n) {
  return typeof n === "number" && Number.isFinite(n);
}
export function validateInputs(qty, price) {
  if (!isValidNumber(qty) || !isValidNumber(price)) {
    return { ok: false, reason: "NaN" };
  }
  if (qty < 0 || price < 0) {
    return { ok: false, reason: "negative" };
  }
  return { ok: true };
}
```

CopyCopied!

`src/services/api.js` mocks a fetch request to simulate network calls so Playwright can track async coverage as well.

```javascript
export async function fetchItems() {
  const res = await fetch("/api/items");
  if (!res.ok) throw new Error("Network error");
  return res.json();
}
```

CopyCopied!

###### Step 3: Run The App

In your current terminal, start a local server in the root of your project directory so that Playwright can access it:

```bash
npm run dev
```

CopyCopied!

Visit [http://localhost:5173](http://localhost:5173/) in your browser to confirm the app is running.

![Demo app which is a  checkout form that lets users enter a quantity and price, calculates a total, and loads a mock list of items.](https://paper-attachments.dropboxusercontent.com/s_17E1491D3C4DD390643E3A7AA5DD3C13A7F7AE7B3A1E9819C9DCEBC8E1CC837B_1760172590801_Screenshot+2025-10-11+at+09.49.15.png)Demo app which is a checkout form that lets users enter a quantity and price, calculates a total, and loads a mock list of items.

###### Step 4: Create The Coverage Tests

In this step, you’ll populate a single `coverage.spec.ts` file with two tests: the _happy path_ and the _error path_.

Each test will execute different branches of the app, producing distinct coverage reports that highlight which parts of the code were actually tested.

**Happy Path Test**: The happy path simulates when the form fields are filled with valid inputs. This test ensures that standard application logic runs as expected.

Here’s what will be covered by the test:

- The main calculation body `return qty * price;` in `src/utils/math.js`
- `const res = await fetch("/api/items");` in `src/services/api.js` (success path)
- `.hidden { display: none; }` in `styles.css` (error element hidden on success)

These lines execute because the test triggers successful calculation, fetch, and hidden error state.

However, the following lines will not be covered:

- `if (qty < 0 || price < 0) return 0;` in `src/utils/math.js`
- `if (qty === 0) return 0;` in `src/utils/math.js`
- `if (!res.ok) throw new Error("Network error");` in `src/services/api.js`
- `.error { color: red; }` in `styles.css`

These paths are skipped because the test doesn’t simulate a failure, zero quantity, or visible error message.

**Error Path Test**: The error path does the opposite. It fills invalid inputs, causing early validation failure. Only the code responsible for handling errors will be covered.

Specifically, this test will cover:

- The `if (qty < 0 || price < 0)` guard in `src/utils/math.js`
- The `errEl.classList.add("error")` line in `src/app.js`
- The `.error { color: red; }` style in `styles.css`

The following will not be covered:

- The main calculation body in `src/utils/math.js`
- The `fetchItems()` success path in `src/services/api.js`
- The `.hidden` CSS rule (since the error is visible)

a. From your project root, create a new file called `tests/coverage.spec.ts` that will contain both tests, and add the following:

```typescript
// coverage.spec.ts
// Contains both tests: happy path and error path
import { test, expect } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";
test.describe("Coverage demo", () => {
  //Happy Path
  test("collects JS and CSS coverage while driving the UI", async ({
    page,
  }) => {
    // Start coverage
    await page.coverage.startJSCoverage();
    await page.coverage.startCSSCoverage();
    // Route the API to make network code run deterministically
    await page.route("**/api/items", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([{ name: "Keyboard" }, { name: "Mouse" }]),
      });
    });
    // Exercise the UI
    await page.goto("http://localhost:5173");
    await page.fill("#qty", "3");
    await page.fill("#price", "19.99");
    await page.click("#calc");
    await expect(page.locator("#total")).toHaveText("59.97");
    // Trigger async path + CSS class toggling was already hit above on valid flow
    await page.click("#load-items");
    await expect(page.locator("#items li")).toHaveCount(2);
    // Stop coverage
    const jsCoverage = await page.coverage.stopJSCoverage();
    const cssCoverage = await page.coverage.stopCSSCoverage();
    // Persist raw V8-style coverage
    const outDir = path.join(process.cwd(), "coverage", "raw");
    await fs.mkdir(outDir, { recursive: true });
    await fs.writeFile(
      path.join(outDir, "js.json"),
      JSON.stringify(jsCoverage, null, 2)
    );
    await fs.writeFile(
      path.join(outDir, "css.json"),
      JSON.stringify(cssCoverage, null, 2)
    );
    // Sanity check: at least one function was recorded
    expect(jsCoverage.length).toBeGreaterThan(0);
  });
  //Error Path
  test("invalid inputs path toggles error CSS", async ({ page }) => {
    await page.coverage.startJSCoverage();
    await page.coverage.startCSSCoverage();
    await page.goto("http://localhost:5173");
    await page.fill("#qty", "-1"); // invalid because negative
    await page.fill("#price", "10");
    await page.click("#calc");
    const err = page.locator("#error");
    await expect(err).not.toHaveClass(/hidden/);
    await expect(err).toHaveClass(/error/);
    const js = await page.coverage.stopJSCoverage();
    const css = await page.coverage.stopCSSCoverage();
    const outDir = path.join(process.cwd(), "coverage", "raw");
    await fs.mkdir(outDir, { recursive: true });
    await fs.writeFile(
      path.join(outDir, "js-invalid.json"),
      JSON.stringify(js, null, 2)
    );
    await fs.writeFile(
      path.join(outDir, "css-invalid.json"),
      JSON.stringify(css, null, 2)
    );
  });
});
```

CopyCopied!

b. Make sure the app is still running in the first terminal. Then, open another terminal window and run:

```bash
    cd playwright-coverage-demo
    npm test
```

CopyCopied!

The resulting output should look like this:

![](https://paper-attachments.dropboxusercontent.com/s_17E1491D3C4DD390643E3A7AA5DD3C13A7F7AE7B3A1E9819C9DCEBC8E1CC837B_1760173050537_Screenshot+2025-10-11+at+09.56.57.png)

Playwright will create a folder containing the raw coverage reports:

```
    coverage/raw/
    ├── js-valid.json
    ├── css-valid.json
    ├── js-invalid.json
    └── css-invalid.json
```

CopyCopied!

These files show that both tests passed successfully, but covered different parts of the codebase. It’s a clear example of how “all green” test results can still leave untested logic behind.

##### Interpreting The Code Coverage Report

To make things clearer, let’s look at a single coverage result from `/src/utils/math.js` inside `js.json`, which is the report generated when valid inputs were sent.

Here’s a shortened excerpt from the file:

```json
{
  "url": "http://localhost:5173/src/utils/math.js",
  "functions": [\
    {\
      "functionName": "calculateTotal",\
      "isBlockCoverage": true,\
      "ranges": [\
        { "startOffset": 7, "endOffset": 180, "count": 1 },\
        { "startOffset": 73, "endOffset": 82, "count": 0 },\
        { "startOffset": 116, "endOffset": 125, "count": 0 }\
      ]\
    },\
    {\
      "functionName": "legacyDiscount",\
      "isBlockCoverage": false,\
      "ranges": [{ "startOffset": 232, "endOffset": 288, "count": 0 }]\
    }\
  ]
}
```

CopyCopied!

From this snippet, notice that inside the `calculateTotal` function, the range `7–180` shows a count of 1. This means the function itself ran once during the test. Without coverage data, it might seem as though everything worked correctly because the test passed.

But if you look closer, the two internal conditions below never ran during the test:

```javascript
if (qty < 0 || price < 0) return 0;
if (qty === 0) return 0;
```

CopyCopied!

These correspond to the smaller ranges `73–82` and `116–125`, both of which show a count of 0. These lines were never executed. In other words, the happy test didn’t verify how the function behaves when the quantity or price is invalid or zero. The function ran, but its key branches remained untested.

Next, look at the `legacyDiscount` function below. It also shows a count of 0. The discount logic was never called in any of the tests, yet both tests still passed.

This is precisely why coverage data matters. Two tests can execute different parts of a function and still miss critical logic. In a production scenario, these two tests wouldn’t be enough; you would need tests that handle zero-quantity inputs and failed fetch calls. With coverage data like this, you can already visualize what’s missing in your test logic and identify where additional tests are needed, not just what already passes.

Even with just two test runs, you can see how much raw data coverage generates. It was already difficult to tell which offset mapped to which code line, and in a larger project, this quickly becomes impossible to interpret manually. That’s why integration with external tools is always advised. Not only does it make the results more human-readable, but it also allows you to integrate with tools that deliver these coverage metrics in related dashboards. Currents is a good example of such a tool. It [aggregates Playwright coverage](https://docs.currents.dev/guides/coverage#coverage-metrics-in-currents) results and displays them alongside your test analytics.

With clearer insights gathered from Playwright and external tools, it’s important to treat the results as an opportunity to strengthen your code coverage, not as a sign of failure.

##### How To Increase Code Coverage in Playwright

The gaps created by inadequate code coverage can become very expensive to patch, especially when they linger in production. Having faulty checkout logic in live code can lead to overcharges, refunds, and support overhead. Over time, this can escalate into legal risks, loss of trust, and long-term damage to your product's reputation.

Poor coverage also drains team productivity and increases the number of post-release fixes. That's why improving coverage shouldn't be negotiable.

**So, How Do You Improve It?**
Start with a risk-based strategy. Don’t write tests to chase a percentage; write them to cover the scenarios your users rely on most. For an e-commerce site, that could mean checkouts and payments. For a social platform, it could mean posting content or adding comments.

Next, integrate more innovative tooling into your workflow. Use platforms that offer functionalities like [AI test generators](https://currents.dev/posts/best-playwright-tools-to-supercharge-your-testing-in-2025#2-ai-test-generators), which can automatically query, generate, and debug your tests. Choose a [analytics dashboard](https://currents.dev/posts/best-playwright-tools-to-supercharge-your-testing-in-2025#2-ai-test-generators) that reveal coverage trends, flaky test patterns, and failure insights. With these systems, you can generate more meaningful tests and steadily increase your code coverage.

Now that you understand what coverage reveals and how to improve it, let’s look at a few common issues that might come up while working with it.

##### Troubleshooting Common Coverage Issues

Here are some of the common issues that can appear when working with Playwright coverage:

- **Coverage report shows 0%:** This usually happens when `page.coverage.startJSCoverage()` or `startCSSCoverage()` wasn’t called before navigation, or the page didn’t trigger any JavaScript execution during the test.

**_Fix:_** Make sure coverage starts before `page.goto()` and stops only after interactions complete.









```typescript
await page.coverage.startJSCoverage();
await page.goto(url);
await page.coverage.stopJSCoverage();
```

CopyCopied!

- **Missing files or empty JSON results:** This is often caused by running tests in non-Chromium browsers. Playwright’s coverage API only works with Chromium-based engines.

**_Fix:_** Confirm your config uses the Chromium project.









```typescript
// playwright.config.ts
use: { ...devices['Desktop Chrome'] }
```

CopyCopied!

- **Duplicated entries in coverage output:** This happens when scripts are bundled or reloaded dynamically.

**_Fix:_** De-duplicate entries in post-processing or rely on tools like Istanbul or Currents dashboards to normalize the data.









```typescript
await page.coverage.startJSCoverage({ resetOnNavigation: false });
```

CopyCopied!

- **Incorrect file paths in the output:** This happens when tests are run from nested directories or CI pipelines; the coverage paths may not align with your local project structure.

**_Fix:_** Use absolute paths or normalize them in your coverage reporter so all files map correctly to their source.









```typescript
entry.url = path.resolve(entry.url.replace("file://", ""));
```

CopyCopied!

- **Cached build artifacts:** This happens when you’re using a bundler or transpiler and outdated cached files create mismatched coverage results.

**_Fix:_** Clear your `.cache` or `dist` folder before running coverage to ensure results reflect the latest code.









```bash
rm -rf dist/ .cache/
```

CopyCopied!


##### Best Practices for Accurate Playwright Code Coverage

There is no perfect number for “good” coverage. The real value lies in what coverage reveals, not the percentage it reports. Here are a few best practices drawn from industry experience:

- **Treat coverage as a guide, not a goal:** Aiming for 100% coverage can lead to redundant or meaningless tests. Instead, use coverage data to identify untested areas that truly matter.

- **Focus on what’s not covered:** Missing coverage highlights untested logic paths or unhandled edge cases; these are the real risk zones worth investigating.

- **Keep coverage close to the code review process:** Reviewing coverage diffs alongside pull requests helps teams discuss why certain lines aren’t tested, instead of chasing arbitrary metrics.

- **Prioritize coverage on frequently changed or critical code:** Dynamic code paths and business-critical logic should have higher coverage thresholds than static or less risky sections.

- **Exclude low-value files:** Skip generated code, configs, and test utilities from coverage reports because they inflate the numbers without adding confidence.

- **Combine coverage with analytics:** Coverage on its own doesn’t measure test quality. Pair it with tools like Currents to track test flakiness, stability, and trends over time.


If you apply these practices while working with code coverage, you’ll start to see more reliable and insightful results. However, even when you follow industry standards and integrate advanced tools, it’s important to remember that code coverage still has its limitations.

##### Understanding Coverage Limitations

Even though code coverage helps visualize which parts of your app are being tested, it has its limits. Coverage only tells you which lines of code were executed, not whether those lines behaved correctly.

For example, if you wrote a test that clicked every button on the demo app without checking any results, you could still reach 100% coverage. Every function would show up as executed, but no logic would actually be verified. That’s why high coverage doesn’t automatically mean strong tests.

Both of your tests passed successfully, yet each covered only half of the possible branches. If you forced coverage to reach 100% without adding meaningful assertions, you might still miss actual bugs.

In other words:
100% coverage ≠ perfect tests.

Coverage is best used as a visibility tool that shows you what parts of the code your tests are touching. It should guide where to add better or deeper tests, not serve as proof that everything works.

##### Wrapping Up

This guide walked through how to measure, interpret, and improve code coverage in Playwright using practical examples and real test data. You now know how coverage works, what its limits are, and how to strengthen it with modern tools and AI-assisted workflows.

Don’t let these concepts stay theoretical. Start measuring, analyze what’s missing, and integrate intelligent test systems that help you grow both coverage and confidence in your testing.

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

### 23. Currents.dev — Selenium → Playwright migration

- Source: https://currents.dev/posts/migrating-from-selenium-to-playwright-the-complete-guide
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Migrating from Selenium to Playwright: The Complete Guide

Migrating from Selenium to Playwright is more than rewriting tests. This guide covers costs, risks, timelines, and how teams actually do it.

![Migrating from Selenium to Playwright: The Complete Guide](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fmigrating-from-selenium-to-playwright-the-complete-guide%2Fcover.png&w=3840&q=100)

Selenium has been the default framework for web testing for almost two decades. Teams have invested years building frameworks, utilities, and expertise around it. But SPAs, modern JS frameworks, and faster release cycles exposed problems Selenium wasn't built to handle.

Playwright was built for this. Independent benchmarks show Playwright tests [cutting regression execution time by up to 75%](https://tymonglobal.com/case-studies/regression-suite-modernization-with-playwright-ai-cicd-cloud-runners/?utm_source=chatgpt.com) in some cases. That's a best-case number, not typical. The actual gain depends on what's making your Selenium suite slow. If most time goes to explicit waits and driver overhead, you'll see big improvements. If the bottleneck is slow backend responses or test data setup, switching frameworks won't fix that.

We've worked with teams at various stages of this migration. The hard part isn't rewriting test code. It's the infrastructure, the people, and the organizational buy-in. This guide covers what we've seen work.

##### Assessing the Cost and Risk of Migration

Converting test scripts is the obvious cost. But the real cost is everything else: infrastructure, people, and the org.

###### Infrastructure

Your Selenium Grid doesn't translate to Playwright. Playwright does have a [Selenium Grid integration](https://playwright.dev/docs/selenium-grid), but the docs explicitly warn it might break in the future. If you're running distributed test execution through Grid, you need a replacement.

Playwright's test runner handles parallelization via worker processes on a single machine. That's a different model from Grid's distributed architecture. It makes local dev faster and CI simpler, but for large suites you'll likely need an orchestration platform for dynamic test distribution across CI machines.

Your CI/CD pipelines will need reconfiguration. Docker images, environment setup, browser binary management: all of it changes. Playwright simplifies some of this by auto-installing browser binaries, but plan for the switchover before you have tests depending on it.

###### Team

Most Selenium teams work in Java or Python with synchronous patterns. Playwright is async-first and its strongest ecosystem is JavaScript/TypeScript. That means learning new syntax, async/await, and a different mental model for waits. It's worth noting that [Playwright does support Java, Python and .NET](https://playwright.dev/docs/languages), although feature support may vary and TypeScript is strongly recommended, it shouldn't be what prevents you from migrating to Playwright.

Budget for a productivity dip. Most engineers get comfortable within 2-3 weeks, but during migration they're maintaining Selenium tests while learning Playwright, troubleshooting in both, and managing two sets of dependencies.

Test writers have the easier transition. Framework maintainers have the harder job: rebuilding custom utilities, auth flows, and test infrastructure from scratch.

###### The Org

Don't underestimate the politics. Engineers who built complex Selenium frameworks have years invested in that work. Telling them it's being replaced creates friction, especially if they feel their specialized knowledge is being devalued. Address this directly. The skills transfer (test design, debugging, CI thinking) even if the syntax doesn't.

Start with phases. Migrate a small subset of critical tests first. Some teams find that partial migration is the permanent state: core user paths in Playwright, edge cases in Selenium. That's fine if the cost of migrating those last tests isn't worth it.

[Runa](https://currents.dev/posts/currents-and-runa), a fintech operating in 30+ countries, migrated from Selenium and REST Assured to Playwright after hitting manual release bottlenecks, limited dev/QA collaboration, and difficulty diagnosing failures. After migration: reduced flakiness, faster releases, better test speed through parallelism.

##### When Migration May Not Be Worth It

Not every team should migrate. Skip it if:

- **Your Selenium suite is small, stable, and rarely changes.** 50-100 tests that pass reliably? The migration cost will exceed the benefit. Migration pays off when the existing suite is causing ongoing pain.
- **Your tests are mostly API-level.** If your suite is light on browser interaction and heavy on API validation, Playwright's advantages are smaller. Selenium handles simple page loads and form submissions fine.
- **You're deep in Java-ecosystem tooling.** TestNG, Maven Surefire, custom Java reporting pipelines, JUnit integrations. You're not just swapping test libraries. You're rebuilding your entire test infrastructure.
- **You're mid-release with no bandwidth.** Half-finished migrations create more problems than they solve. Wait for a window.

If none of these apply and your suite is causing real pain (slow CI, flaky tests, maintenance burden), keep reading.

##### Migration Strategy: How Teams Actually Do It

![Migration strategy: how teams actually do it](https://currents.dev/img/posts/migrating-from-selenium-to-playwright-the-complete-guide/strategy.png)Migration strategy: how teams actually do it

###### Prerequisite: Ownership

Migrations fail without a clear owner. "Everyone's responsibility" means no one's priority. Pull 2-3 engineers off regular work for a quarter if you can. If you can't, assign sprint goals and treat migration like feature work.

A suite of 500 tests won't migrate in two weeks. Set expectations early with QA, dev, and DevOps.

###### Phase 1: Infrastructure First

Don't write tests yet. Set up Playwright, install dependencies, get one test running locally, then in CI. Sort out Node.js versions, browser binary access, and environment variables before you have 50 tests depending on the answers.

If your Selenium framework has custom auth helpers or navigation utilities, build the Playwright equivalents now.

Example Playwright setup:

```typescript
// playwright.config.ts
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30000,
  use: {
    baseURL: "https://your-app.com",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [\
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },\
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },\
  ],
});
```

CopyCopied!

Get one Playwright test green in your pipeline. Just one.

Watch for common early pitfalls:

- **Fixture scope confusion.** Playwright fixtures can be scoped to individual tests or to workers. Test-scoped fixtures run fresh for every test, while worker-scoped fixtures are shared across all tests running in the same worker process. Mixing these up causes state leaks or unnecessary setup overhead. Use test-scoped fixtures for things like a fresh page or browser context. Use worker-scoped fixtures for expensive setup that's safe to share, like database connections or authenticated sessions.
- **Test data isolation in parallel execution.** Playwright runs tests in parallel by default. Tests that share mutable state (like database rows or file system state) will interfere with each other. Each test should create its own data or use unique identifiers to avoid collisions.
- **Building custom utilities before understanding Playwright's patterns.** Playwright's built-in [fixtures](https://playwright.dev/docs/test-fixtures), [auto-waiting](https://playwright.dev/docs/actionability), and [locator API](https://playwright.dev/docs/locators) already handle many scenarios that required custom code in Selenium. Start simple and expand as you learn what the framework provides out of the box.
- **Overcorrecting on locator strategy.** Teams coming from Selenium's explicit CSS and XPath selectors sometimes swing too far toward text-based locators like `getByText()` or positional selectors like `nth()`. These break when content changes or lists reorder. Prefer `getByRole`, `getByTestId`, or locators scoped to a parent container. The goal is locators that survive UI changes without being so generic they match the wrong element. For more on this, see [14 lessons learned after 500+ Playwright tests](https://currents.dev/posts/how-to-run-playwright-tests-without-the-pain).

###### Phase 2: Write New Tests in Playwright

Start with the tests that matter most: login, checkout, core business flows. These run most frequently and hurt most when they're slow or flaky. Focus on tests that execute dozens of times per day, not tests that run once per release.

Don't blindly port Selenium tests line-by-line. Rewrite them. Check whether the old test still covers what matters, whether the steps make sense, and whether assertions could be clearer.

Here's a common Selenium pattern:

```ts
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
WebElement loginButton = wait.until(
  ExpectedConditions.elementToBeClickable(By.id("login-button"))
);
loginButton.click();

WebElement usernameField = driver.findElement(By.id("username"));
usernameField.clear();
usernameField.sendKeys("user@example.com");

WebElement passwordField = driver.findElement(By.id("password"));
passwordField.clear();
passwordField.sendKeys("password123");

WebElement submitButton = driver.findElement(By.id("submit"));
submitButton.click();

wait.until(ExpectedConditions.urlContains("/dashboard"));
```

CopyCopied!

Playwright equivalent:

```typescript
await page.goto("/login");
await page.locator("#username").fill("user@example.com");
await page.locator("#password").fill("password123");
await page.locator("#submit").click();
await expect(page).toHaveURL(/.*dashboard/);
```

CopyCopied!

Auto-waiting removes the explicit wait boilerplate. The test reads like what it actually does: go to login, fill fields, click submit, check URL.

Playwright's [test generator](https://playwright.dev/docs/codegen) (`npx playwright codegen`) is useful here. It records browser actions and generates test code. Navigate through the same flows and get Playwright code with correct locators. It won't produce perfect tests, but it saves time on the mechanical conversion.

Run both frameworks in CI during this phase. If Playwright tests fail but Selenium passes, tune the Playwright tests. If Selenium fails but Playwright passes, you've likely found [flaky tests](https://currents.dev/posts/what-is-a-flaky-test-and-how-to-fix-it) that Playwright handles better.

For each migrated test, the Playwright version should be faster (typically 20-40%), equally stable or better, and cover the same scenarios. If it's slower or flakier, investigate before moving on.

###### Phase 3: Gradual Migration and Cleanup

Not every test needs to migrate. If a test covers a dead feature or duplicates your API tests, delete it. Use migration to [measure actual coverage gaps](https://currents.dev/posts/how-to-measure-code-coverage-in-playwright-tests) instead of counting tests.

Running two frameworks long-term has real overhead: two dependency chains, two CI configs, two sets of debugging tools, two mental models. If your Selenium tail is small (under 50 tests), finishing the migration is probably cheaper than maintaining dual infrastructure. Check this tradeoff periodically. Don't let partial migration become the default forever.

Keep both running in CI until Playwright coverage is proven. Some teams do this for a quarter, others for six months.

##### Key Technical Differences

###### Async/Await

Selenium is synchronous. Playwright is async. Every action needs `await`:

```typescript
test("user can login", async ({ page }) => {
  await page.goto("/login");
  await page.locator('[name="email"]').fill("user@test.com");
  await page.locator('button[type="submit"]').click();
  await expect(page.locator(".welcome")).toBeVisible();
});
```

CopyCopied!

The most common mistake: missing an `await`. The test finishes before the action completes, passes when it shouldn't, and you don't notice until something else breaks. Always await actions and assertions.

Playwright also has concurrency patterns you need to know:

**`test.describe.serial`** forces sequential execution within a parallel suite. If one test fails, the rest are skipped. Useful when step B depends on step A, but use it sparingly:

```typescript
test.describe.serial("onboarding flow", () => {
  test("step 1: create account", async ({ page }) => {
    // ...
  });

  test("step 2: verify email", async ({ page }) => {
    // ...
  });

  test("step 3: complete profile", async ({ page }) => {
    // ...
  });
});
```

CopyCopied!

**Shared state in parallel execution** will bite you. Playwright runs test files in parallel by default. Two tests modifying the same database row, feature flag, or file will break each other. Each test needs its own data. For expensive shared setup, use worker-scoped fixtures:

```typescript
import { test as base } from "@playwright/test";

// Worker-scoped: runs once per worker process, shared across tests in that worker
const test = base.extend<{}, { apiClient: APIClient }>({
  apiClient: [\
    async ({}, use) => {\
      const client = await APIClient.create();\
      await use(client);\
      await client.cleanup();\
    },\
    { scope: "worker" },\
  ],
});

// Test-scoped (default): runs fresh for each test
const testWithPage = test.extend<{ dashboardPage: Page }>({
  dashboardPage: async ({ page }, use) => {
    await page.goto("/dashboard");
    await use(page);
  },
});
```

CopyCopied!

###### Auto-Waiting vs. Explicit Waits

Selenium makes you think about timing constantly. Playwright auto-waits. When you call `click()`, it waits for the element to be attached, visible, stable, and enabled. Most of your explicit waits and sleeps go away.

You still need explicit waits for custom conditions (see [debugging Playwright timeouts](https://currents.dev/posts/debugging-playwright-timeouts)):

```typescript
// Wait for specific count
await expect.poll(() => page.locator(".item").count()).toBeGreaterThan(5);

// Wait for network idle
await page.waitForLoadState("networkidle");
```

CopyCopied!

###### Browser Contexts for Isolation

Selenium needs Grid for parallel execution. Playwright uses browser contexts. One browser instance, multiple isolated contexts:

```typescript
const browser = await chromium.launch();
const context1 = await browser.newContext();
const context2 = await browser.newContext();

// Each context is isolated: cookies, storage, and cache don't leak between them
const page1 = await context1.newPage();
const page2 = await context2.newPage();
```

CopyCopied!

No separate browser processes per test.

This also makes multi-user testing easy. Here's an admin and regular user in the same test:

```typescript
test("admin changes are visible to regular user", async ({ browser }) => {
  // Create two isolated contexts with different auth states
  const adminContext = await browser.newContext({
    storageState: "playwright/.auth/admin.json",
  });
  const userContext = await browser.newContext({
    storageState: "playwright/.auth/user.json",
  });

  const adminPage = await adminContext.newPage();
  const userPage = await userContext.newPage();

  // Admin publishes a new announcement
  await adminPage.goto("/admin/announcements");
  await adminPage.locator("#title").fill("Maintenance Window");
  await adminPage.locator("#publish").click();
  await expect(adminPage.locator(".status")).toHaveText("Published");

  // Regular user sees it immediately
  await userPage.goto("/dashboard");
  await expect(userPage.locator(".announcement")).toContainText(
    "Maintenance Window",
  );

  await adminContext.close();
  await userContext.close();
});
```

CopyCopied!

In Selenium, this requires two WebDriver instances or manual cookie manipulation.

One caveat: browser contexts isolate client-side state (cookies, local storage, cache), not backend state. Two tests modifying the same database rows will conflict regardless of context isolation. Database cleanup, unique test data per run, or transactional rollbacks are still on you. See [test data strategy](https://currents.dev/posts/how-to-build-reliable-playwright-tests-a-cultural-approach) for more.

###### Network Interception

[`page.route()`](https://playwright.dev/docs/network) gives you built-in network interception. No proxy servers, no browser extensions, no WireMock. Just intercept and mock:

```typescript
test("shows error on payment failure", async ({ page }) => {
  // Intercept the payment API and return a failure response
  await page.route("**/api/payments", (route) =>
    route.fulfill({
      status: 402,
      contentType: "application/json",
      body: JSON.stringify({ error: "Card declined" }),
    }),
  );

  await page.goto("/checkout");
  await page.locator("#card").fill("4111111111111111");
  await page.locator("#submit").click();
  await expect(page.locator(".error")).toHaveText("Card declined");
});
```

CopyCopied!

You can also modify real responses, block resources to speed up tests, or wait for specific requests:

```typescript
await page.route("**/*.{png,jpg,gif}", (route) => route.abort());
await page.route("**/analytics/**", (route) => route.abort());

await page.route("**/api/users/me", async (route) => {
  const response = await route.fetch();
  const json = await response.json();
  json.featureFlags.newDashboard = true;
  await route.fulfill({ response, body: JSON.stringify(json) });
});
```

CopyCopied!

In Selenium, this typically meant running a separate proxy server or configuring WireMock alongside your suite.

###### Authentication State Storage

[`storageState`](https://playwright.dev/docs/auth) saves an authenticated session (cookies, local storage, IndexedDB) to a JSON file. Reuse it across every test.

In Selenium, auth means logging in through the UI every time (slow), injecting cookies (brittle), or sharing a browser session (breaks isolation). Playwright handles this with a [setup project](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests):

```typescript
// tests/auth.setup.ts
import { test as setup, expect } from "@playwright/test";

const authFile = "playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill("user@example.com");
  await page.getByLabel("Password").fill("password123");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL("/dashboard");

  // Save the authenticated state to a file
  await page.context().storageState({ path: authFile });
});
```

CopyCopied!

```typescript
// playwright.config.ts
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

CopyCopied!

Every test starts already authenticated. The setup runs once, all tests reuse the saved state. If your Selenium suite logged in through the UI for every test, this alone saves minutes.

Pitfalls to watch for: sessions expire during long CI runs, so auth setup succeeds but tests later fail with 401s. If parallel workers share one account and your app enforces single-session login, workers invalidate each other. Playwright's docs cover [one account per parallel worker](https://playwright.dev/docs/auth#moderate-one-account-per-parallel-worker) using `testInfo.parallelIndex`. Some apps also rotate CSRF tokens on sensitive actions, which makes stored state go stale. Test your auth strategy under parallel load before scaling up.

###### Trace-Based Debugging

Selenium gives you screenshots and console logs. Playwright's [trace viewer](https://playwright.dev/docs/trace-viewer) records everything: every action, network request, and DOM snapshot.

```typescript
// playwright.config.ts
use: {
  trace: 'on-first-retry',
  video: 'retain-on-failure',
  screenshot: 'only-on-failure',
}
```

CopyCopied!

When a test fails, you open the trace and see exactly what happened at each step. See [debugging Playwright tests in CI](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci) for more.

##### CI and Reporting

CI migration gets underestimated. Selenium's ecosystem extends well beyond the test library.

###### CI Pipelines

Selenium needs [ChromeDriver](https://developer.chrome.com/docs/chromedriver) for Chrome, [GeckoDriver](https://geckodriver.com/guide/) for Firefox, each matching the browser version exactly. Playwright bundles browsers with the package. `playwright install --with-deps` handles everything. Different Docker images, simpler config.

GitHub Actions example:

```yaml
name: Playwright Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "18"
      - name: Install dependencies
        run: npm ci
      - name: Install Playwright browsers
        run: npx playwright install --with-deps
      - name: Run Playwright tests
        run: npx playwright test
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: playwright-report/
```

CopyCopied!

###### Orchestration and Reporting

Playwright includes [built-in sharding](https://currents.dev/posts/optimizing-test-runtime-playwright-sharding-vs-workers) (`--shard=1/4`) that splits tests statically across CI machines. Works for smaller suites. At scale, it's suboptimal because tests are distributed without considering actual durations, so some workers sit idle while others are still running.

[Currents](https://currents.dev/) uses a queue-based approach instead: tests go to the next available worker as it finishes. This matters when test durations vary (and they always do).

Running both Selenium and Playwright in CI creates reporting gaps. A few options:

**Playwright's built-in [HTML reporter](https://playwright.dev/docs/test-reporters#html-reporter)** generates a self-contained report with results, traces, and screenshots. Good enough if you don't need cross-run analytics or trend data.

**[Currents](https://currents.dev/)** combines reporting with orchestration: dashboards, flaky test detection, dynamic test distribution, instant access to traces/screenshots/videos without downloading CI artifacts, and GitHub PR integration.

##### Realistic Timelines

Here's what we've seen work:

**The spike (2-4 weeks):** Playwright installation, CI proof of concept, fixtures for auth and test data, utility migration, and 5-10 critical path tests converted. This surfaces blockers early.

**Low-intensity mode (ongoing):** New tests go in Playwright, existing tests migrate gradually. Engineers typically match their previous writing speed within 2-3 weeks.

**Parallel running (1-2 quarters minimum):** Both frameworks run in CI until you're confident Playwright coverage is stable and equivalent.

**What speeds things up or slows them down:**

- Custom framework complexity matters more than test count. Minimal custom utilities = fast migration. Complex auth flows or test data setup can add weeks.
- Clean Selenium tests with clear page objects convert quickly. Hard-coded waits, brittle selectors, and duplicated logic mean refactoring on top of conversion.
- One engineer with Playwright experience on the team makes a big difference.
- Protected time and leadership backing keep things moving. Migration competing with feature work leads to stalling.

**You don't have to migrate everything.** Many teams move critical paths (60-70% of tests) to Playwright and leave edge cases in Selenium. The first batch delivers most of the execution time savings.

##### Using AI for Migration

AI tools (Cursor, ChatGPT, Claude, GitHub Copilot) are good at the mechanical parts of migration. (For a broader look at AI in the Playwright ecosystem, see [the state of Playwright AI in 2026](https://currents.dev/posts/state-of-playwright-ai-ecosystem-in-2026).)

They handle syntax conversion well: `findElement(By.id("button"))` to `page.locator("#button")`, WebDriverWait to auto-wait, assertion syntax updates. For suites where most tests follow simple linear flows (navigate, fill form, click, verify), AI can handle the bulk of conversion.

AI also suggests better locators. Selenium tests are full of XPath and CSS selectors that could be `getByRole`, `getByText`, or `getByLabel` in Playwright. Verify the suggestions, but they're usually improvements.

Where AI falls short: complex flows with branching logic, conditional waits, or multi-step interactions across pages. AI converts the syntax correctly but misses behavioral differences between frameworks. Custom utilities and auth helpers also need human work. AI can't map your bespoke Selenium abstractions to Playwright without context. And always check for hardcoded secrets that should be environment variables.

The workflow that works best: build your infrastructure first (fixtures, utilities, helpers), migrate 5-10 tests manually, then feed those as examples to AI tools along with the Selenium tests to convert. The output will match your conventions better than cold prompting.

##### Conclusion

Migration is real work, not a weekend project. Start with infrastructure, not tests. Get one test green in CI, build your utilities, then scale. Don't port tests blindly. Don't migrate everything if it's not worth it. And don't start until you have someone who owns it.

* * *

[Scale your Playwright tests with confidence. \\
\\
Join hundreds of teams using Currents.\\
Learn More](https://currents.dev/?ref=blog)

_Trademarks and logos mentioned in this text belong to their respective owners._

###### Related Posts

[![From Cypress to Playwright - cost, migration steps, timeline and AI tools](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fmigration-cypress-to-playwright%2Fcypress-playwright-migration-cover.png&w=3840&q=90)\\
\\
Jul 25, 2024 **From Cypress to Playwright - cost, migration steps, timeline and AI tools** \\
\\
![Andrew Goldis](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fandrew-goldis.jpg&w=64&q=75)\\
\\
Andrew Goldis](https://currents.dev/posts/cypress-to-playwright-migration) [![How To Adopt Playwright the Right Way](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-adopt-playwright-the-right-way%2Fcover.png&w=3840&q=90)\\
\\
Jan 19, 2026 **How To Adopt Playwright the Right Way** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/how-to-adopt-playwright-the-right-way) [![How To Build Reliable Playwright Tests: A Cultural Approach](https://currents.dev/_next/image?url=%2Fimg%2Fposts%2Fhow-to-build-reliable-playwright-tests-a-cultural-approach%2Fposter.png&w=3840&q=90)\\
\\
Nov 28, 2025 **How To Build Reliable Playwright Tests: A Cultural Approach** \\
\\
![Asjad Khan](https://currents.dev/_next/image?url=%2Fimg%2Fauthors%2Fhackmamba.png&w=64&q=75)\\
\\
Asjad Khan](https://currents.dev/posts/how-to-build-reliable-playwright-tests-a-cultural-approach)

### 25. TestDino — 17 best practices

- Source: https://testdino.com/blog/playwright-best-practices
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### 17 Playwright Best Practices That Actually Matter (With Code)

Find best practices that separate production-grade Playwright setups from tutorial code, with working examples for every pattern.

[![Jashn Jain Author Profile Image](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FJashn-Jain-profile-picture-2.webp&w=3840&q=75)\\
\\
Jashn Jain\\
\\
Updated Aug 12, 2026](https://testdino.com/blog/author/jashn-jain)





![17 Playwright Best Practices That Actually Matter (With Code)](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F03%2F17-Playwright-Best-Practices-Every-Engineer-Should-Follow-in-2026.webp&w=3840&q=75)

Playwright best practices are the patterns that keep an end-to-end suite fast, readable, and stable as it grows. In short: test what users see, lock onto [role-based locators](https://testdino.com/blog/playwright-locators), let assertions auto-wait, isolate every test, seed data through the API, and watch flaky tests with real reporting in CI.

The 17 practices below each come with a runnable code example and the specific failure it prevents. Work top to bottom for a new suite, or jump to the section you need.

The code snippets target [Playwright 1.60.0](https://github.com/testdino-hq/playwright-releases), so a few practices use APIs that landed in recent releases.

![Tip Icon](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)What this guide covers

01. [Define your test coverage goals](https://testdino.com/blog/playwright-best-practices#define-your-test-coverage-goals)
02. [Test what users see, not how it's built](https://testdino.com/blog/playwright-best-practices#test-what-users-see-not-how-it-s-built)
03. [Use stable locators](https://testdino.com/blog/playwright-best-practices#use-stable-locators)
04. [Name tests and steps for the failure](https://testdino.com/blog/playwright-best-practices#name-tests-and-steps-for-the-failure)
05. [Keep tests focused and isolated](https://testdino.com/blog/playwright-best-practices#keep-tests-focused-and-isolated)
06. [Write assertions that wait automatically](https://testdino.com/blog/playwright-best-practices#write-assertions-that-wait-automatically)
07. [Assert the accessibility tree, not the DOM](https://testdino.com/blog/playwright-best-practices#assert-the-accessibility-tree-not-the-dom)
08. [Use APIs to seed test data](https://testdino.com/blog/playwright-best-practices#use-apis-to-seed-test-data)
09. [Reset database state between runs](https://testdino.com/blog/playwright-best-practices#reset-database-state-between-runs)
10. [Mock external dependencies with page.route()](https://testdino.com/blog/playwright-best-practices#mock-external-dependencies-with-page-route)
11. [Structure your project for scale](https://testdino.com/blog/playwright-best-practices#structure-your-project-for-scale)
12. [Master Playwright's debugging tools](https://testdino.com/blog/playwright-best-practices#master-playwright-s-debugging-tools)
13. [Abort tests early when a precondition fails](https://testdino.com/blog/playwright-best-practices#abort-tests-early-when-a-precondition-fails)
14. [Parallelize and shard across CI](https://testdino.com/blog/playwright-best-practices#parallelize-and-shard-across-ci)
15. [Eliminate flaky tests](https://testdino.com/blog/playwright-best-practices#eliminate-flaky-tests)
16. [Use Playwright AI agents to generate and heal tests](https://testdino.com/blog/playwright-best-practices#use-playwright-ai-agents-to-generate-and-heal-tests)
17. [Centralize reporting for CI](https://testdino.com/blog/playwright-best-practices#centralize-reporting-for-ci)

##### Define your test coverage goals

[End-to-end tests](https://testdino.com/blog/playwright-e2e-testing) are slow and expensive to maintain, so spend them where a failure costs you the most. Cover the paths that make you money and the ones that page someone at night: sign-up, login, checkout, and the core action your product exists to do. Push everything else down to unit and integration tests.

Aim for roughly 30% of your total suite as E2E. A test that asserts a button is blue belongs in a unit test. A test that proves a guest can pay belongs here.

checkout-coverage.spec.ts

```
// E2E: prove the path works end to end
  test('user can complete checkout', async ({ page, request }) => {
    const userId = await request.post('/api/test/user/create').then(r => r.json());
    await page.goto('https://storedemo.cms.testdino.com/');
    await page.getByText('Apple iPad Air').first().click();
    await page.getByRole('button', { name: /add to cart/i }).click();
    await expect(page.getByText('Cart')).toBeVisible();
  });
```

![Testing priority matrix plotting business impact against testing ease ](https://cms.testdino.com/wp-content/uploads/2026/03/priority-matrix.webp)

Testing priority matrix plotting business impact against testing ease

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Use your analytics data quarterly to revisit coverage priorities. What users do today might change in 6 months. Keep your test portfolio aligned with reality. For a structured approach, see our [Playwright automation checklist](https://testdino.com/blog/playwright-automation-checklist).

##### Test what users see, not how it's built

A user does not know your component is called <CheckoutButton data-state="ready">. They see a button that says "Pay". When you assert against internal class names or state attributes, your test breaks every time a developer refactors markup that the user never notices.

Target the visible, semantic surface instead.

locator-implementation.spec.ts

```
// Brittle: breaks on any markup refactor
  await page.locator(".btn.btn--primary.checkout-cta").click()

  // Resilient: survives refactors, mirrors the user
  await page.getByRole("button", { name: "Pay" }).click()
```

![Code comparison showing fragile CSS selectors versus resilient role-based locators](https://cms.testdino.com/wp-content/uploads/2026/03/CSS-Selectors-getByRole-scaled.webp)

Code comparison showing fragile CSS selectors versus resilient role-based locators

Rule of thumb: If you delete the element's class attribute and the user experience stays the same, your test shouldn't break either. This is one of the most important Playwright best practices because it affects everything downstream: locator stability, assertion reliability, and maintenance cost.

##### Use stable locators

Playwright ranks [locators](https://testdino.com/blog/playwright-locators "https://testdino.com/blog/playwright-locators/") by how closely they match what a user perceives. Reach for the top of this list first and drop down only when you have to:

1. getByRole(): the accessible role and name
2. getByLabel(): form fields by their label
3. getByPlaceholder(): inputs by placeholder
4. getByText(): non-interactive content
5. getByTestId(): an explicit data-testid hook
6. CSS or XPath: last resort

Role locators eliminate more flaky tests than any other single change, because they survive markup churn and double as an accessibility check.

product-scope.spec.ts

```
// Chain to scope without brittle CSS paths
  const row = page.getByRole("row", { name: "Standing desk" })
  await row.getByRole("button", { name: "Remove" }).click()
```

Use npx playwright codegen https://storedemo.cms.testdino.com/ to auto-generate locators by recording interactions.

![](https://cms.testdino.com/wp-content/uploads/2026/03/codegen-1.png)

##### Name tests and steps for the failure

A test title is the first thing you read when CI goes red. "checkout works" tells you nothing. "checkout charges the card and shows the order number" tells you exactly what broke. Name each test after the behavior it proves, and wrap multi-action flows in test.step() so the [trace](https://testdino.com/blog/playwright-trace-viewer "https://testdino.com/blog/playwright-trace-viewer/") reads like a sentence.

checkout.spec.ts

```
test("checkout charges the card and shows the order number", async ({ page }) => {
    await test.step("add item to cart", async () => {
      await page.getByTestId("add-to-cart-button").click()
    })
    await test.step("open cart and check out", async () => {
      await page.getByTestId("header-cart-icon").click()
      await page.getByTestId("checkout-button").click()
    })
    await test.step("place the order", async () => {
      await page.getByTestId("checkout-place-order-button").click()
    })
  })
```

A failing step name points straight at the broken action, so you open the right trace first.

##### Keep tests focused and isolated

Each Playwright test runs in its own browser context with independent cookies, localStorage, session storage, and cache. No test should depend on the state left behind by another test. This is a Playwright best practice that prevents cascading failures.

![Diagram showing independent browser contexts running tests concurrently in Playwright](https://cms.testdino.com/wp-content/uploads/2026/03/parallel-execution-dark.webp)

Diagram showing independent browser contexts running tests concurrently in Playwright

Use beforeEach hooks for shared setup:

test-isolation.spec.ts

```
import { test, expect } from '@playwright/test';
  test.beforeEach(async ({ page }) => {
    await page.goto('https://storedemo.cms.testdino.com/login');
    await page.getByLabel('Email').fill('[email protected]');
    await page.getByLabel('Password').fill('securepassword');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('My Account')).toBeVisible();
  });
  test('user can view their profile', async ({ page }) => {
    await page.getByRole('link', { name: 'Profile' }).click();
    await expect(page.getByRole('heading', { name: 'My Profile' })).toBeVisible();
  });
```

For larger suites, logging in before every test wastes time. Use Playwright's setup project to authenticate once and share the session:

playwright.config.ts

```
export default defineConfig({
    projects: [\
      { name: 'setup', testMatch: /.*\.setup\.ts/ },\
      {\
        name: 'chromium',\
        dependencies: ['setup'],\
        use: { storageState: './auth.json' },\
      },\
    ],
  });
```

auth.setup.ts

```
setup('authenticate', async ({ page }) => {
    await page.goto('https://storedemo.cms.testdino.com/login');
    await page.getByLabel('Email').fill('[email protected]');
    await page.getByLabel('Password').fill('password');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('My Account')).toBeVisible();
    await page.context().storageState({ path: './auth.json' });
  });
```

##### Write assertions that wait automatically

Web-first assertions retry until the condition is true or the timeout expires. Manual checks run once, the instant you call them, and fail the moment the UI is a frame behind. The difference is most of your flaky tests.

web-first-assertions.spec.ts

```
// Bad: Manual assertion, checks once
  expect(await page.getByText('Cart').isVisible()).toBe(true);
  // Good: Web-first assertion, retries until visible or timeout
  await expect(page.getByText('Cart')).toBeVisible();
```

![](https://cms.testdino.com/wp-content/uploads/2026/03/web-first-vs-manual-assertions.png)

Never wrap an assertion in a manual waitForTimeout(). If you need to wait, assert the thing you are actually waiting for. For a deeper dive into debugging assertion failures, check our [Playwright debugging guide.](https://testdino.com/blog/playwright-debugging-guide)

##### Assert the accessibility tree, not the DOM

A full ARIA snapshot locks in the structure a user and a screen reader perceive, without pinning you to specific markup. As of Playwright 1.60, toMatchAriaSnapshot() works on a whole page, not only a single locator, so you can guard an entire view in one assertion. See our deep dive on the [accessibility tree](https://testdino.com/blog/accessibility-tree) for how Playwright builds it.

aria-snapshot.spec.ts

```
// Snapshot the page's accessibility tree
  await expect(page).toMatchAriaSnapshot(`
    - heading "Your cart" [level=1]
    - list:
      - listitem: /Standing desk/
    - button "Checkout"
  `)
```

This catches a heading that silently became a <div>, or a button that lost its label, while ignoring the cosmetic class changes that break a CSS-based assertion.

##### Use APIs to seed test data

Clicking through a sign-up form to create a user before every test is slow and fragile. Hit your API instead. Setup that takes ten UI actions becomes one request, and it does not break when the sign-up page changes.

user-seed.spec.ts

```
test.beforeEach(async ({ request }) => {
    const userResponse = await request.post('https://storedemo.cms.testdino.com/api/test/users', {
      data: {
        email: '[email protected]',
        password: 'secure-password-123',
        firstName: 'Test',
        lastName: 'User'
      }
    });
    const user = await userResponse.json();
  });
```

For complex scenarios, create a factory to generate consistent test data:

test-data-factory.ts

```
export class TestDataFactory {
    constructor(private request: APIRequestContext, private baseUrl: string) {}
    async createUser(overrides?: Partial<{ email: string; name: string }>) {
      const response = await this.request.post(`${this.baseUrl}/api/test/users`, {
        data: {
          email: `user-${Date.now()}@test.com`,
          password: 'test-password-123',
          firstName: 'Test',
          lastName: 'User',
          ...overrides
        }
      });
      return response.json();
    }
  }
```

![ Graph comparing the setup speed of Playwright with Cypress and Selenium](https://cms.testdino.com/wp-content/uploads/2026/03/setup-speed.webp)

Graph comparing the setup speed of Playwright with Cypress and Selenium

Reserve UI steps for the behavior you are actually testing. Everything before that point should arrive through the fastest door available.

![](https://cms.testdino.com/wp-content/uploads/2026/01/fluent_info-sparkle-48-filled.svg)

**Tip:** Create a dedicated /api/test/\* endpoint on your backend that only exists in non-production environments. Always clean up test data in afterEach hooks to keep your environment clean.

##### Reset database state between runs

Seeding through the API gets you a fast, known starting point. It does not undo what a test wrote. A test that creates an order leaves that order behind, and the next run inherits it.

Reset the data your tests mutate, either with a teardown that deletes what the test created or a per-run snapshot you restore before the suite starts.

cleanup.spec.ts

```
test.afterEach(async ({ request }) => {
    await request.delete(`/api/test/orders?runId=${process.env.RUN_ID}`)
  })
```

Without a reset, a suite that passes on a clean database fails the second time you run it, and that failure looks like flake when it is really dirty state.

##### Mock external dependencies with page.route()

You do not control a payment provider's sandbox uptime or a weather API's rate limit, so do not let them decide whether your suite passes. Intercept the request and return a fixed response with [network mocking](https://testdino.com/blog/playwright-network-mocking "https://testdino.com/blog/playwright-network-mocking/"). Your test stays deterministic and runs offline.

mock-route.spec.ts

```
await page.route("**/api.stripe.com/**", (route) =>
    route.fulfill({
      status: 200,
      json: { id: "ch_test_123", status: "succeeded" },
    }),
  )
```

Mock the third party, then assert that your application correctly responds to its response. That is the part you own.

![Visual showing how Playwright can intercept and control network requests in tests](https://cms.testdino.com/wp-content/uploads/2026/03/Control-every-request-your-tests-make.png)

Visual showing how Playwright can intercept and control network requests in tests

##### Structure your project for scale

A flat folder of 200 test files is unsearchable by week three. [Group by feature](https://testdino.com/blog/grouping-playwright-tests "https://testdino.com/blog/grouping-playwright-tests/"), and pull repeated setup into fixtures and helpers so a UI change touches one file, not fifty.

Project structure

```
tests/
    checkout/
      checkout.spec.ts
      coupon.spec.ts
    fixtures/
      auth.ts
    pages/
      checkout-page.ts
```

Keep snapshot files predictable, too. The {testFileBaseName} token, added in 1.60, names snapshots after the spec file without its extension, which keeps a large suite's snapshot folder readable.

playwright.config.ts

```
export default defineConfig({
    snapshotPathTemplate: "{testDir}/__snapshots__/{testFileBaseName}/{arg}{ext}",
  })
```

![ Visual representation of an optimized Playwright project directory structure](https://cms.testdino.com/wp-content/uploads/2026/03/directory-structure.webp)

Visual representation of an optimized Playwright project directory structure

##### Master Playwright's debugging tools

Pick the tool that matches the failure:

- UI Mode (--ui): watch tests run, time-travel through steps, edit locators live.
- Inspector (--debug): step through a single test and try locators in real time.
- Trace Viewer: open the trace from a CI failure and see the DOM, network, and console at each step.

Turn tracing on for failures so a red CI run hands you a full recording instead of a stack trace.

playwright.config.ts

```
export default defineConfig({
    use: { trace: "on-first-retry" },
  })
```

In 1.60, a failed expect() also attaches the accessibility snapshot of the element at the moment it failed through errorContext, so the trace shows you what the page actually looked like when the assertion gave up.

![](https://cms.testdino.com/wp-content/uploads/2026/03/image-20260313-114116-1.png)

##### Abort tests early when a precondition fails

When a fixture or route handler hits a state that makes the rest of the test meaningless, stop immediately rather than letting it run on to a confusing timeout. Playwright 1.60 added test.abort() for exactly this: it now fails the running test with a message explaining why.

precondition.spec.ts

```
test.beforeEach(async ({ request }) => {
    const health = await request.get("/api/health")
    if (!health.ok()) {
      test.abort("Staging API is down — skipping to avoid noise.")
    }
  })
```

An aborted test with a clear message reads as an environment problem, not a product bug, so nobody wastes time debugging your code for an outage upstream.

##### Parallelize and shard across CI

Playwright runs files in parallel by default. To go faster on CI, split the suite across machines with sharding and let each runner take a slice.

.github/workflows/playwright.yml

```
#### GitHub Actions
  strategy:
    matrix:
      shard: [1, 2, 3, 4]
  steps:
    - run: npx playwright test --shard=${{ matrix.shard }}/4
```

Install only the browsers a job needs with npx playwright install --with-deps chromium so CI setup does not download three engines you are not using on that run.

![Infographic showing Playwright test sharding across multiple CI machines](https://cms.testdino.com/wp-content/uploads/2026/03/ci-sharding.webp)

Infographic showing Playwright test sharding across multiple CI machines

##### Eliminate flaky tests

A test that passes and fails on the same code teaches your team to ignore red, and that is how real bugs ship. [Most flaky tests](https://testdino.com/blog/playwright-flaky-tests) come from a short list of causes:

- A missing await on an async call.
- A manual waitForTimeout() instead of a web-first assertion.
- Random or time-based data that differs between runs.
- A test depending on state another test created.
- An element that exists in the DOM but is hidden or animating.

Catch the missing-await class at lint time before it ever runs.

eslint.config.js

```
rules: {
    "@typescript-eslint/no-floating-promises": "error",
  }
```

For the rest, retry once to confirm a failure is real, then track which tests fail intermittently over time so you fix the worst offenders instead of muting them.

##### Use Playwright AI agents to generate and heal tests

Playwright's agent workflow can draft a test from a plain-language plan, run it, and propose a fix when it breaks. Treat the output as a first draft: review every generated locator and assertion before it lands, because an agent will happily assert the wrong thing with full confidence.

Give the agent the same locator rules you follow. When it knows to prefer getByRole() and web-first assertions, the code it writes needs far less cleanup than a cold prompt.

![Diagram demonstrating AI-powered self-healing tests in Playwright](https://cms.testdino.com/wp-content/uploads/2026/03/ai-integration-2.webp)

Diagram demonstrating AI-powered self-healing tests in Playwright

##### Centralize reporting for CI

Playwright's built-in HTML report is per-run and lives on the machine that produced it. On a real team, you want history: which test started failing, on which branch, after which commit, and whether it is a new bug or an old flake.

Stream results to a reporter that keeps that history across runs and groups failures by root cause, so a wall of red collapses into the handful of causes behind it. [TestDino](https://www.testdino.com/ "https://www.testdino.com") does this for Playwright, with failure classification and flake tracking tied back to the pull request that triggered the run.

playwright.config.ts

```
export default defineConfig({
    reporter: [["html"], ["@testdino/playwright"]],
  })
```

![](https://cms.testdino.com/wp-content/uploads/2026/03/sentralized-reporting-scaled.png)

##### Quick reference

| Practice | Command or pattern |
| --- | --- |
| Run in UI mode | npx playwright test --ui |
| Debug a single test | npx playwright test --debug |
| Shard across 4 runners | npx playwright test --shard=1/4 |
| Install one browser | npx playwright install --with-deps chromium |
| Trace on retry | use: { trace: "on-first-retry" } |
| Reuse login | test.use({ storageState: "..." }) |
| Snapshot the a11y tree | await expect(page).toMatchAriaSnapshot(...) |
| Abort on bad precondition | test.abort("reason") |

##### FAQs

What are Playwright best practices?

They are the patterns that keep an end-to-end suite reliable as it grows: testing user-visible behavior, role-based locators, web-first assertions, isolated tests, API-driven setup, and centralized reporting in CI.

What is the most important Playwright best practice?

Use role-based locators with getByRole(). It removes more flake than any other single change because it survives markup refactors and mirrors how a user finds elements.

How do I stop Playwright tests from being flaky?

Replace manual waits with web-first assertions, add a lint rule for missing await, isolate the data each test creates, and track intermittent failures over time instead of muting them.

Do I need the Page Object Model?

Only when reuse earns it. For a small suite, fixtures and helpers are enough. Reach for page objects when the same flow repeats across many specs and you want one place to update it.

How often should I update Playwright?

Track each minor release. New versions ship locator, assertion, and debugging improvements, and staying current is how you pick up changes like the page-level ARIA snapshot and test.abort() from 1.60.

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

### 24. Sajith Dilshan — From chaos to control — decoupling logic from configuration

- Source: https://medium.com/@sajith-dilshan/%EF%B8%8F-from-chaos-to-control-a-senior-qa-engineers-guide-to-decoupling-logic-from-configuration-in-3e5ffba7291f
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### From Chaos to Control: A Senior QA Engineer Guide to Decoupling Logic from Configuration in Playwright

When you first start writing Playwright tests, everything feels simple.

You write a test. You open a page. You log in. You assert something. It works. You feel productive.

Then reality hits.

- You need to run tests in **staging and production**
- Your team grows from **1 to 10 engineers**
- CI/CD pipelines start running tests **in parallel**
- Tests begin to fail randomly
- Someone changes a URL and 50 tests break

At this point, most teams realize something critical:

> The problem is not Playwright. The problem is how the framework is structured.

This is where one of the most important architectural principles comes in: **decoupling logic from configuration**.

##### What Does Decoupling Logic from Configuration Mean?

Bad (tightly coupled):

```typescript
await page.goto('https://staging.myapp.com');
await page.fill('#email', 'admin@test.com');
```

Here the URL is hardcoded, credentials are hardcoded, environment is fixed. If anything changes, you must edit test code.

Good (decoupled):

```typescript
await page.goto(config.baseURL);
await login(config.credentials.admin);
```

Now test logic stays the same and configuration changes externally.

##### Why This Matters

1. Environment switching becomes painful without decoupling: `ENV=staging npx playwright test` with no code changes needed.
2. Tests become fragile — hardcoded values cause broken tests when URLs change, duplicated logic, inconsistent behavior.
3. Teams cannot scale — everyone writes config differently, no central control, debugging becomes messy.
4. Security risks — credentials inside test files can leak and cannot be managed in CI securely.
5. CI/CD becomes complicated — pipelines rely on environment variables and dynamic configuration.

##### The Right Architecture (Big Company Approach)

A scalable Playwright framework is built in layers:

```
framework/
├── config/        → environment & settings
├── fixtures/      → runtime injection
├── pages/         → UI abstraction
├── tests/         → business scenarios
```

##### Step 1: Build a Config System

This is your **single source of truth**:

```typescript
export class ConfigManager {

  constructor(private env = process.env) {}

  get environment() {
    return this.env.ENV || 'staging';
  }

  get baseURL() {
    switch (this.environment) {
      case 'prod':
        return 'https://app.myapp.com';
      case 'dev':
        return 'https://dev.myapp.com';
      default:
        return 'https://staging.myapp.com';
    }
  }

  get retry() {
    return Number(this.env.RETRY_COUNT || 0);
  }

  get browser() {
    return {
      headless: this.env.HEADLESS !== 'false',
      slowMo: Number(this.env.SLOW_MO || 0),
    };
  }

  get artifacts() {
    return {
      screenshot: this.env.ENABLE_SCREENSHOTS || 'only-on-failure',
      video: this.env.ENABLE_VIDEOS || 'retain-on-failure',
    };
  }

}
export const config = new ConfigManager();
```

##### Step 2: Use `.env` for External Configuration

```
ENV=staging

BASE_URL_STAGING=https://staging.myapp.com
BASE_URL_PROD=https://app.myapp.com

HEADLESS=true
SLOW_MO=0

RETRY_COUNT=1

ENABLE_SCREENSHOTS=only-on-failure
ENABLE_VIDEOS=retain-on-failure
```

##### Step 3: Connect Config to Playwright

```typescript
export default defineConfig({
  retries: config.retry,

  use: {
    baseURL: config.baseURL,
    headless: config.browser.headless,
    slowMo: config.browser.slowMo,
    screenshot: config.artifacts.screenshot,
    video: config.artifacts.video,
  },
});
```

##### Step 4: Inject Config Using Fixtures

Instead of calling config everywhere, inject it:

```typescript
import { test as base } from '@playwright/test';
import { config } from '../config/configManager';

export const test = base.extend({
  appConfig: async ({}, use) => {
    await use(config);
  },
});
```

##### Step 5: Build Composable Fixtures

Page fixture:

```typescript
export const test = base.extend({
  appPage: async ({ page, appConfig }, use) => {
    await page.goto(appConfig.baseURL);
    await use(page);
  },
});
```

Auth fixture:

```typescript
export const test = base.extend({
  loggedInPage: async ({ page, appConfig }, use) => {
    await page.goto(appConfig.baseURL);

    await page.fill('#email', process.env.ADMIN_USER!);
    await page.fill('#password', process.env.ADMIN_PASS!);
    await page.click('button[type=submit]');

    await use(page);
  },
});
```

##### Step 6: Clean Tests (Final Goal)

```typescript
test('dashboard loads', async ({ loggedInPage }) => {
  await loggedInPage.click('#dashboard');
});
```

Notice: no config, no login logic, no environment awareness. This is what clean test design looks like.

##### Advanced Patterns Used in Large Companies

1. Role-based fixtures: `adminPage`, `userPage` — each role has different credentials and permissions.
2. Authentication reuse: store session state and reuse across tests. Result: tests run faster, less flakiness.
3. Parallel-safe execution: each test gets an isolated browser context and independent data, avoiding conflicts and flaky failures.
4. Multi-environment pipelines: same tests run on staging, production, preview environments without changing test code.

##### Common Mistakes to Avoid

- Hardcoding values in tests
- Using `process.env` everywhere
- Mixing config with test logic
- Boolean misinterpretation (`"false"` !== false)
- Overloading ConfigManager with too many responsibilities

##### The Real Benefit

When done correctly, you get: run anywhere without code change, faster execution with reuse, clean readable tests, scalable architecture, team-friendly design.

##### Final Thoughts

Most beginners focus on "How do I write a test?" Experienced engineers focus on "How do I design a system where tests remain stable, scalable, and maintainable over time?" Decoupling logic from configuration is one of the **foundations of that system**. It is not just a pattern — it is a mindset.

### 26. TestDino — Timeout guide

- Source: https://testdino.com/blog/playwright-timeout
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### Playwright Timeout: Configure, Debug, and Fix Every Type

Stop guessing which Playwright timeout to change. This guide gives you a decision tree, precedence rules, and a 5-minute fix playbook.

[![Savan Vaghani](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FSavan-Vaghani-Profile-Image-1-1.webp&w=3840&q=75)\\
\\
Savan Vaghani\\
\\
Updated May 25, 2026](https://testdino.com/blog/author/savan-vaghani)





![Playwright-Timeout_-Configure-Debug-and-Fix-Every-Type](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F05%2FPlaywright-Timeout_-Configure-Debug-and-Fix-Every-Type.webp&w=3840&q=75)

Playwright gives every test a time budget. If something takes too long, like a click that never lands or a page that never finishes loading, it kills the test and throws an error. The tricky part is that there are six different Playwright timeout types, and changing the wrong one does nothing.

Teams running [Playwright test automation](https://testdino.com/blog/playwright-test-automation) in CI pipelines hit Playwright timeout errors more than almost any other failure. The Test timeout of 30000ms exceeded message alone has hundreds of threads on GitHub and Stack Overflow. Most of them end with someone just bumping the number higher without understanding why the test was slow in the first place.

This guide walks you through every Playwright timeout type, gives you a decision tree to pick the right one, and includes a 5-minute playbook for fixing the 30000ms error without hiding the root cause.

Test timeout of 30000ms exceeded.

If this is what brought you here, skip to the [5-minute fix playbook](file:///c%3A/Siddharth/Blogs/Playwright%20timeout/Article_playwright-timeout.md#fix-test-timeout-of-30000ms-exceeded-in-5-minutes "file:///c%3A/Siddharth/Blogs/Playwright%20timeout/Article_playwright-timeout.md#fix-test-timeout-of-30000ms-exceeded-in-5-minutes"). If you want to stop seeing it for good, keep reading.

[Playwright's documentation](https://playwright.dev/docs/test-timeouts "https://playwright.dev/docs/test-timeouts") lists six different timeout types. They interact in ways that are not obvious until something breaks at 2am. Most engineers raise the Playwright test timeout, hide the underlying bug, and watch the same suite fail again next run.

TL;DR

Playwright timeout defaults
Test: 30s · Expect: 5s · Action / Navigation / Global / Fixture: no default (falls back to test timeout). Configure in playwright.config.ts via timeout, expect.timeout, use.actionTimeout, use.navigationTimeout, globalTimeout.

![](https://cms.testdino.com/wp-content/uploads/2026/05/image-20260520-073241.png)

##### The 6 Playwright timeout types at a glance

Before you change a Playwright timeout value, you need to know which timeout you are changing. The six types govern different scopes. Confusing them is the most common reason a "timeout fix" does not actually fix anything.

| Timeout type | Default | Scope | Config key |
| --- | --- | --- | --- |
| Test timeout | 30,000 ms | One test + its fixtures + beforeEach hooks | timeout |
| Expect timeout | 5,000 ms | One auto-retrying assertion (toHaveText, toBeVisible, etc.) | expect.timeout |
| Action timeout | None (falls back to test timeout) | One action (click, fill, hover, etc.) | use.actionTimeout |
| Navigation timeout | None (falls back to test timeout) | One navigation (goto, reload, redirects) | use.navigationTimeout |
| Global timeout | None | The entire test run | globalTimeout |
| Fixture timeout | None (falls back to test timeout) | One fixture (async ({}, use) => { … }) | { timeout: N } in fixture definition |
| beforeAll / afterAll hook | 30,000 ms | One worker-scoped hook | test.setTimeout(N) inside hook |

**Note:** "No default" does not mean infinite. It means the value falls back to the test timeout.

![](https://cms.testdino.com/wp-content/uploads/2026/05/image.png)

##### Which Playwright timeout do I need? A decision tree

Most articles organize themselves by timeout type. That is the wrong axis for someone whose test just failed. You arrive asking "what do I change?" not "tell me about the Playwright locator timeout."

**If the failure message says** Test timeout of 3000ms exceeded **and the call log was waiting on an element**:

The bottleneck is the action or the expect. Read what the call log was waiting on.

A click that did not land points to the Playwright locator timeout (action timeout). An assertion that did not pass points to the Playwright expect timeout.

**If the failure is on** page.goto() **or a redirect**:

The navigation timeout governs that wait. Set it with use.navigationTimeout in your config or inline on the goto() call.

**If the suite hangs forever and no individual test fails:**

The global timeout or a fixture missing await use(...) is almost always the cause. Check the [Playwright fixtures](https://testdino.com/blog/playwright-fixtures) section below.

**If a single test is genuinely slow (checkout, visual regression, large upload):**

Reach for test.setTimeout() or test.slow(), not a global config change. The [Playwright annotations](https://testdino.com/blog/playwright-annotations) guide covers both in detail.

**If the test passes locally but fails only in CI:**

Jump straight to the CI section before changing anything. You likely need a CI-specific actionTimeout, not a global bump.

If your symptom does not fit any single branch, you almost certainly have two problems stacked. Fix the narrowest first: inline action timeouts before config, single-test overrides before Playwright default timeout values.

![](https://cms.testdino.com/wp-content/uploads/2026/05/image-20260520-073409.png)

##### Fix Test timeout of 30000ms exceeded in 5 minutes

The Playwright test timeout of 30000ms exceeded error is the single most-reported Playwright failure on [the Microsoft Playwright issue tracker](https://github.com/microsoft/playwright/issues/20212 "https://github.com/microsoft/playwright/issues/20212"). Bumping the timeout makes it disappear for one run and come back the next. The timeout is the symptom, not the bug.

Here is the 4-step playbook. Run it in order.

**Step 1: Read the call log.**

Playwright prints the action it was stuck on. You will usually see waiting for locator('…') followed by which [actionability checks](https://playwright.dev/docs/actionability) were still pending. Nine times out of ten the locator name itself tells you the problem:

- A renamed element
- A dynamic ID that changed between runs
- Something behind an overlay
- A button that is visible but not enabled

If the log says "stable" or "enabled" rather than "visible," the element exists but is not actionable. That is an actionTimeout problem, not a test timeout problem.

**Step 2: Open the Trace Viewer.**

Run this command to inspect the failing trace:

terminal

```
npx playwright show-trace trace.zip
```

![](https://cms.testdino.com/wp-content/uploads/2026/05/Screenshot-2026-05-19-121841.png)

Step through to the failing action. Three tabs matter:

- **Call log**: what was Playwright waiting on?
- **Network tab**: any pending requests at the moment of failure?
- **Console tab**: any uncaught errors that broke the page?

For a deeper walkthrough, the [Playwright Trace Viewer guide](https://testdino.com/blog/playwright-trace-viewer "https://testdino.com/blog/playwright-trace-viewer") covers the full surface area.

**Step 3: Set the narrowest possible fix.**

A hung XHR is a navigation or waitForResponse problem. Set a tighter timeout inline on the failing wait.

A locator that takes longer than expected is a Playwright expect timeout issue. Raise { timeout: N } on the single assertion.

A slow click is an action timeout problem. Raise { timeout: N } inline on the click.

Inline beats config-level. Config-level beats test-level. Never raise the test timeout to mask a one-line problem.

**Step 4: Only after steps 1 through 3, raise the test timeout for that test.**

your-test-file.spec.ts

```
test('checkout flow with slow payment gateway', async ({ page }) => {
  test.setTimeout(60_000);
  // ... test code
});
```

Use test.setTimeout(60\_000) or test.slow() (triples the default) inside the test function. Never in the global config. Raising the suite-wide Playwright default timeout gives every flaky locator extra seconds to hide.

If you skip steps 1 through 3, you are hiding a bug. It will fail again, usually in CI, usually under load, usually right before a release.

##### How to configure each Playwright timeout

Now that you know how to diagnose timeout issues, here is how each Playwright timeout type works and exactly where to configure it.

###### Test timeout: what it covers and how to set it

**Definition**: Test timeout is the outer budget for a single test. It defaults to 30 seconds and covers the test body, fixture setup, and any beforeEach hooks. A 5-second test running behind a 28-second fixture fails with a test timeout error pointing at the test, not the fixture.

playwright.config.ts

```
export default defineConfig({ timeout: 60_000 });
```

To override for a single test:

slow-checkout.spec.ts

```
test('long e2e flow', async ({ page }) => {
  test.setTimeout(120_000);
  // ... test steps
});
```

test.setTimeout() must run synchronously at the top of the test, before any await. test.slow() is a shortcut that triples the current timeout. Both belong inside the test because they document why this test needs more time.

In our experience, teams that use test.slow() with a code comment explaining the reason produce far fewer "why is this timeout so high?" review threads.

Setting timeout: 0 disables the test timeout entirely, which is almost never the right call. For more on test-level annotations like test.slow(), the [Playwright annotations guide](https://testdino.com/blog/playwright-annotations) covers the full list.

###### Expect (assertion) timeout: for auto-retrying assertions

The Playwright expect timeout has nothing to do with the Playwright test timeout. It governs how long Playwright keeps re-checking a condition during an [auto-retrying assertion](https://testdino.com/blog/playwright-assertions) like toHaveText, toBeVisible, toBeEnabled, and the rest of the locator-based matchers. The Playwright default timeout for expect is 5 seconds.

The assertion polls roughly every 100ms, passes as soon as the condition becomes true, and fails only if the window expires.

Non-retrying assertions do not honor this timeout. expect(someString).toBe('hello') runs once against a captured value and either passes or fails immediately. Raising the expect timeout to fix a toBe failure does nothing.

playwright.config.ts

```
export default defineConfig({
  expect: { timeout: 10_000 },
});
```

assertion-override.spec.ts

```
// per-assertion override
await expect(page.getByText('Order confirmed')).toBeVisible({ timeout: 30_000 });
```

If your assertion timeout approaches half your Playwright test timeout, the test timeout is the real constraint. An assertion configured for 60s inside a 30s test will be killed by the test timeout first. Keep the test timeout at least twice your longest single-assertion budget.

###### Action and navigation timeouts

Action timeout caps how long a single user-interaction call (click, fill, hover) waits for the element to become actionable. Navigation timeout governs every page.goto(), page.reload(), and full-page redirect.

Both default to no value. They fall back to the test timeout, which means a single hung click or page load can burn the full 30-second budget on one line.

playwright.config.ts

```
export default defineConfig({
  use: {
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
  },
});
```

inline-overrides.spec.ts

```
// inline overrides
await page.getByRole('button', { name: 'Submit' }).click({ timeout: 5_000 });
await page.goto('/dashboard', { timeout: 30_000 });
```

Ten seconds is a sensible global actionTimeout. It is short enough that a stuck click surfaces fast in the trace, and long enough that legitimate network blips do not trigger spurious failures.

If you are tempted to set the Playwright set timeout value higher because something is flaky, you are looking at a locator problem. The [Playwright best practices](https://testdino.com/blog/playwright-best-practices "https://testdino.com/blog/playwright-best-practices") guide covers the patterns that eliminate most failures upstream.

The most common navigation trap is page.waitForLoadState('networkidle'). The networkidle state requires 500ms of zero network activity, which never happens on apps with long-poll connections, analytics beacons, or chat widgets.

Prefer domcontentloaded and assert on a specific element:

better-navigation.spec.ts

```
await page.goto('/dashboard', { waitUntil: 'domcontentloaded' });
await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
```

###### Global timeout: capping the whole run

Global timeout caps the entire test run, every test, every worker, every retry. It exists primarily as a CI safety net so a runaway suite does not hold a runner hostage for hours.

playwright.config.ts

```
export default defineConfig({
  globalTimeout: 60 * 60 * 1000,    // 1 hour
});
```

Set it to roughly twice your normal suite runtime. If a run blows past 2x, something has genuinely gone wrong, usually a worker stuck on a hanging fixture, and failing fast frees the runner.

###### Fixture and hook timeouts: the silent killers

Fixture and hook timeouts cause the weirdest Playwright timeout failures because the error rarely names the fixture. The test prints Test timeout of 30000ms exceeded with an empty call log, no actions, no waits, and no useful trace.

You spend twenty minutes wondering why a test that never started is timing out. We have seen this exact scenario trip up even experienced SDETs.

The cause is almost always a fixture that forgot to call await use(value). Fixtures use inversion-of-control: set up the value, call await use(value) to hand control to the test, then run teardown after the test returns.

Without await use(...), the test never starts but the Playwright test timeout still ticks.

If a 30000ms timeout has no call-log entries, suspect this first. The [Playwright fixtures guide](https://testdino.com/blog/playwright-fixtures "https://testdino.com/blog/playwright-fixtures") covers the full pattern.

Fixtures can also declare their own timeout independent of the test. And beforeAll/afterAll hooks have their own 30-second default that you extend with test.setTimeout() inside the hook:

custom-fixture.ts

```
const test = base.extend<{ slowSetup: string }>({
  slowSetup: [async ({}, use) => {\
    const value = await reallySlowSetupCall();\
    await use(value);\
  }, { timeout: 60_000 }],
});
```

###### Stop using waitForTimeout: what to use instead

page.waitForTimeout(2000) is the single most common reason Playwright suites are simultaneously slow and flaky. It waits the exact number of milliseconds regardless of whether the page is ready.

That wastes time on fast environments and still fails on slow ones. In production suites we have audited, removing every waitForTimeout call cut average Playwright test timeout failures by roughly 40%.

Replace it with a wait for the actual condition:

bad-vs-good-waits.spec.ts

```
// BAD: hard sleep
await page.locator('#submit').click();
await page.waitForTimeout(2000);

// GOOD: wait for the element to appear
await expect(page.locator('.confirmation')).toBeVisible();

// GOOD: wait for navigation
await page.waitForURL('**/order-confirmation');

// GOOD: wait for a network response
const responsePromise = page.waitForResponse(r =>
  r.url().includes('/api/orders') && r.status() === 201
);

await page.locator('#submit').click();
await responsePromise;
```

Web-first assertions poll roughly every 100ms and pass as soon as the condition holds. The one acceptable use of Playwright wait for timeout is local debugging. Remove it before committing.

For more patterns that prevent [flaky tests](https://testdino.com/blog/playwright-flaky-tests "https://testdino.com/blog/playwright-flaky-tests"), check the dedicated guide.

##### Timeout precedence: who wins when settings conflict

You have set actionTimeout: 10\_000 in the config and click({ timeout: 5\_000 }) inline. Which one fires? The inline value.

More specific always wins, but with one ceiling: nothing exceeds the Playwright test timeout, no matter how generous a config or inline value is.

| Scenario | Inline | Config | Test timeout | Effective limit |
| --- | --- | --- | --- | --- |
| Inline action + config actionTimeout | 5s | 10s | 30s | 5s (inline wins) |
| Config actionTimeout only | - | 10s | 30s | 10s (config wins) |
| No action timeout anywhere | - | - | 30s | 30s (falls back to test) |
| Inline expect + config expect.timeout | 10s | 5s | 30s | 10s (inline wins, under test) |
| Long expect timeout above test timeout | - | 60s | 30s | ~30s (test kills it first) |
| actionTimeout: 0 (disabled) | - | 0 | 30s | 30s (disabled = test ceiling) |

The last two rows trip engineers up. If your Playwright expect timeout is longer than your Playwright test timeout, the assertion never gets to use its full budget. Setting actionTimeout: 0 does not mean unlimited. It means "use the test timeout."

**Practical rule**: Layer smallest to largest.

- Tight actionTimeout caps each step
- Slightly larger expect.timeout caps each assertion
- Test timeout at least 2x your longest single-step budget
- Global timeout as a safety net
- Inline overrides for that one call only

This is also why [debugging Playwright tests](https://testdino.com/blog/debug-playwright-tests "https://testdino.com/blog/debug-playwright-tests") starts with understanding which timeout layer actually fired first.

![](https://cms.testdino.com/wp-content/uploads/2026/05/image-20260520-073503.png)

##### Common causes of Playwright timeouts (and how to fix them)

Most Playwright timeouts come from one of four causes, ranked by what shows up most often in real test suites.

###### 1)  The selector never matches.

Locator targets a renamed element, an element that does not exist yet, or one sitting behind a different DOM tree than the locator assumes. The call log shows waiting for locator('…') with no actionability progress.

Open the Trace Viewer's DOM snapshot at the failure moment to confirm. This is the single most common cause of a Playwright locator timeout.

The [Playwright locators guide](https://testdino.com/blog/playwright-locators "https://testdino.com/blog/playwright-locators") covers stable locator strategies that prevent this.

###### 2) A network request never completes.

API hangs, third-party script blocks load, or the backend is just slow. The Trace Viewer's network tab shows pending requests at the failure point.

Fix with a tighter waitForResponse timeout or mock the slow endpoint with page.route().

###### 3) CI is slower than local.

Playwright default timeout values pass on a developer MacBook and fail on a [GitHub Actions](https://testdino.com/blog/playwright-in-github-actions) ubuntu-latest runner that is 2 to 3x slower. Same test, consistently passing locally and failing in CI.

Not a code bug, it is a hardware mismatch. The [CI section below](file:///c%3A/Siddharth/Blogs/Playwright%20timeout/Article_playwright-timeout.md#playwright-timeouts-in-ci-why-they-fail-there-and-not-locally "file:///c%3A/Siddharth/Blogs/Playwright%20timeout/Article_playwright-timeout.md#playwright-timeouts-in-ci-why-they-fail-there-and-not-locally") handles this.

###### 4) The element is present but not actionable.

Covered by a modal, behind an overlay, scrolled off-screen, or disabled by a parent. The call log says "not stable", "not visible", or "not enabled" rather than "not found." Wait for the blocking state to clear before the action runs.

If your timeouts match none of these, you may be looking at a flake masquerading as a Playwright timeout. They look identical in the error but have different fixes.

The [Playwright flaky tests](https://testdino.com/blog/playwright-flaky-tests "https://testdino.com/blog/playwright-flaky-tests") guide covers the distinction. You can also look at broader patterns across [test failure analysis](https://testdino.com/blog/test-failure-analysis "https://testdino.com/blog/test-failure-analysis") to spot recurring issues.

###### Debugging timeouts with the Trace Viewer

The Trace Viewer is the fastest tool for debugging a Playwright timeout. Three tabs answer "why did this time out," in order:

- **Call log**: which action was stuck and on which actionability check
- **Network tab**: any pending requests at failure (a long red bar = hung XHR; repeating short bars = long-poll preventing networkidle)
- **Console tab**: uncaught errors that may have broken the page before the failing action ran

![](https://cms.testdino.com/wp-content/uploads/2026/05/Screenshot-2026-05-19-122028.png)

Three of four Playwright timeout types can be diagnosed from the call log alone. Enable trace recording in CI with trace: 'on-first-retry'. It only records on failure, so it is cheap, but you get a full trace for every flake:

playwright.config.ts

```
export default defineConfig({
  retries: process.env.CI ? 2 : 0,
  use: { trace: 'on-first-retry' },
});
```

For the full surface area including snapshots, source maps, and the action timeline, the [Trace Viewer guide](https://testdino.com/blog/playwright-trace-viewer "https://testdino.com/blog/playwright-trace-viewer") covers everything. For a broader debugging workflow, the [Playwright debugging guide](https://testdino.com/blog/playwright-debugging-guide "https://testdino.com/blog/playwright-debugging-guide") is also worth reading.

![Bar Chart](https://cms.testdino.com/wp-content/uploads/2026/05/single-bar-chart-7.png)

_**Source**: [Playwright official documentation on timeouts](http://playwright.dev/docs/test-timeouts), Note: 0 means no default and falls back to test timeout_

##### Playwright timeouts in CI: why they fail there and not locally

If tests pass locally and fail in CI, you have a hardware mismatch dressed up as a Playwright bug. CI runners are 1.5x to 3x slower than developer laptops, and the Playwright default timeout values are sized for the laptop.

The fix is not a global Playwright increase timeout across the config. It is environment-aware action and navigation timeouts plus retries for transient infrastructure blips.

| Runner | Approx. speed vs MacBook Pro | Recommended actionTimeout | Recommended navigationTimeout |
| --- | --- | --- | --- |
| GitHub Actions ubuntu-latest (2-core, 7GB) | ~2 to 3x slower | 15,000 ms | 45,000 ms |
| GitHub Actions ubuntu-latest-large | ~1.5x slower | 10,000 ms | 30,000 ms |
| CircleCI medium | ~2x slower | 12,000 ms | 40,000 ms |
| Self-hosted | Variable | Benchmark first | Benchmark first |

These are starting points, not gospel. The right way to tune is to measure: time a representative test locally, time it on the runner, divide, apply the multiplier.

Set conditionally using the CI [environment variable](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/variables#default-environment-variables):

playwright.config.ts

```
const isCI = !!process.env.CI;
export default defineConfig({
  timeout: isCI ? 60_000 : 30_000,
  expect: { timeout: isCI ? 10_000 : 5_000 },
  use: {
    actionTimeout: isCI ? 15_000 : 5_000,
    navigationTimeout: isCI ? 45_000 : 15_000,
  },
  retries: isCI ? 2 : 0,
});
```

For worker-count tuning per runner class, the [Playwright parallel execution](https://testdino.com/blog/playwright-parallel-execution "https://testdino.com/blog/playwright-parallel-execution") guide covers the details. If you are setting up [Playwright in GitHub Actions](https://testdino.com/blog/playwright-in-github-actions "https://testdino.com/blog/playwright-in-github-actions") for the first time, that guide walks through the full pipeline config.

##### When to raise a timeout vs. fix the underlying problem

Every time you bump a timeout in a PR, run this five-question check first. If the answer to any of them is yes, raising the timeout is the wrong fix.

**Does the test sometimes pass at 80% of the timeout and sometimes fail at 110%?**

That is flakiness, not slowness. The cause is variable: a race condition, an unstable selector, a flaky dependency. Raising the Playwright timeout reduces the failure rate without removing the cause.

**Has the test gotten 20%+ slower over the last four weeks?**

That is trend rot. Something has slowed down. Find what changed.

Bumping the Playwright timeout buys you a month before you are back here with the same test and a higher number.

**Is the wait on a known-slow third-party service?**

Mock it with page.route(). Your test should not depend on a third-party SLA.

**Is** waitForTimeout **anywhere in this test?**

Remove it first, then re-measure. A single waitForTimeout(2000) removal often saves enough to fit under the existing budget.

**Is the test waiting on an element that should be there immediately?**

That is a selector or state problem, not a timeout problem. Fix the locator or wait for the state the element depends on.

If all five come back no, raise the timeout on the single test with test.setTimeout(). Not in the global config. The [Playwright reporting](https://testdino.com/blog/playwright-reporting) tools can help you spot which tests are consistently pushing their budgets.

###### Track timeout trends across runs with TestDino

A single timeout is a fix. A timeout creeping across runs is a signal, and it is the strongest leading indicator of test rot you can have.

A test that ran in 2.4s in January, 3.1s in February, 4.0s in March, and 5.0s in April will hit your 30-second budget eventually. The first sign is a CI failure two weeks before a release.

TestDino tracks runtime per test across every Playwright run and surfaces tests creeping toward budget before they break the build. The near-timeout alert fires at 90% of budget, early enough to fix while it is a five-minute investigation.

Pair with the [Playwright slow tests](https://testdino.com/blog/playwright-slow-tests "https://testdino.com/blog/playwright-slow-tests") guide for the deeper trend-detection pattern. For teams looking at broader [test automation analytics](https://testdino.com/blog/test-automation-analytics "https://testdino.com/blog/test-automation-analytics"), TestDino provides the full runtime trend dashboard.

##### Conclusion

The Playwright timeout system is six knobs, layered. Test caps the test. Expect caps each assertion. Action and navigation cap each interaction.

Fixture caps each fixture. Global caps the run.

The Playwright default timeout of 30 seconds is not a target. It is a budget, and the right fix is almost never to raise it.

Match symptom to timeout with the decision tree. Set the narrowest value: inline first, then config, then test. Run the five-question check before any bump, and track trends so you catch rot early.

For the rest of the Playwright surface area, the [TestDino Playwright cheatsheet](https://testdino.com/playwright-cheatsheet "https://testdino.com/playwright-cheatsheet") collects the patterns used most across real-world test suites.

##### FAQs

What is the Playwright default timeout?

Playwright Test sets a 30-second test timeout and a 5-second expect timeout by default. Action, navigation, global, and fixture timeouts have no standalone default and fall back to the test timeout. Change all of these in playwright.config.ts.

How do I increase the Playwright timeout?

Set timeout: 60\_000 in playwright.config.ts to raise the Playwright default timeout for all tests. For a single test, call test.setTimeout(60\_000) or use test.slow() to triple the current default. Always prefer a per-test Playwright increase timeout over a global bump.

A test or fixture ran longer than the 30-second budget. The most common causes are a selector that never matches, a hung network request, a slow CI runner, or networkidle on an app with continuous background activity. Open the Trace Viewer's call log to see which action was stuck.

What is the difference between Playwright test timeout and expect timeout?

Playwright test timeout is the total budget for a single test plus its fixtures and beforeEach hooks (30s default). Playwright expect timeout is the budget for a single auto-retrying assertion like toHaveText() (5s default). Expect runs inside the test timeout and can never exceed it.

Should I use Playwright wait for timeout in my tests?

No. page.waitForTimeout() is a hard sleep regardless of page state. It makes fast tests slow and slow tests flaky.

Use expect(locator).toBeVisible(), page.waitForURL(), or page.waitForResponse() instead.

Why do my Playwright tests pass locally but timeout in CI?

CI runners are 1.5x to 3x slower than developer laptops. Set CI-aware Playwright timeout values with process.env.CI ? 15\_000 : 5\_000 for actionTimeout and navigationTimeout, and enable retries: 2 for transient infrastructure blips.

What does test.slow() do in Playwright?

test.slow() triples the Playwright default timeout for a single test, from 30s to 90s. Use it for genuinely slow flows like e2e checkout or visual regression. It is the wrong tool for a fast test that is flaking because of a bad selector.

How do I disable Playwright timeouts?

Set timeout: 0 in playwright.config.ts to disable the Playwright test timeout, or call test.setTimeout(0) inside a specific test. Disabling timeouts is rarely the right answer. A better approach is to set a generous but finite Playwright timeout rather than disabling entirely.

[![Savan Vaghani](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F02%2FSavan-Vaghani-Profile-Image-1-1.webp&w=3840&q=75)](https://testdino.com/blog/author/savan-vaghani)

[**Savan Vaghani**](https://testdino.com/blog/author/savan-vaghani)

Product Developer

Savan Vaghani builds the frontend at TestDino, a SaaS platform that turns Playwright test data into something teams actually want to look at.

His day to day sits at the intersection of product and engineering. He designs multi tenant dashboards that help QA and dev teams track test runs, surface flaky tests, and monitor CI health without forcing anyone to dig through raw logs.

The stack is React and TypeScript, but the real work is in the product decisions. He works on onboarding flows that reduce time to value, GitHub integrations that meet teams where they already work, and interface details that make complexity feel simple.

He thinks a lot about the gap between "technically correct" and "actually usable", and tends to close it.

[LinkedIn](http://www.linkedin.com/in/savan-vaghani-233107241 "LinkedIn")[GitHub](https://github.com/savanvaghani2 "GitHub")

[View all posts →](https://testdino.com/blog/author/savan-vaghani)

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

### 27. idavidov.eu (ArchQA) — Playwright test architecture — Stefan Minchev interview

- Source: https://idavidov.eu/playwright-test-architecture-stefan-minchev
- Retrieved: 2026-08-29
- Firecrawl status: complete

#### I Made Stefan Minchev Defend Every Decision in Our Playwright Framework

"If you wipe the test folder tomorrow, the architecture is still sitting right there. The tests are just the cherry on top."

That is Stefan Minchev, four minutes into the first ArchQA, Unscripted episode ever recorded, calmly walking out of the trap I set for him. A year ago, when I had to design a Playwright framework that five QAs across five different teams would use every day on a trading platform, Stefan was the person I designed it with. So for the first conversation of the series, I invited the one engineer who cannot hide behind theory with me. We built the same thing. Time to defend it, decision by decision.

We split the conversation into four episodes. Here is what each round did to us.

##### Round 1: What Is Test Architecture

Stefan claim: your tests are the tip of the iceberg, maybe the last 10 to 15 percent of the work. Architecture is every decision you make before you ever open a spec file. The folder tree, the fixtures, the data isolation, the rules that stop bad code from being committed.

We landed fast on the real enemy, tribal knowledge. All the conventions that live only in senior heads, so a new engineer discovers them by breaking them in a pull request. Then Stefan said the quiet part about documentation:

> "Within a few months, your readme is just lying to everyone."

No pushback from me here. The fun part is the ladder Stefan walks, from the weakest way to store your rules all the way to the strongest, including the one rung he calls the best context you can give an LLM.

##### Round 2: The Cost of Bad Architecture

Stefan claim: skipping architecture does not save the work. It converts it into maintenance, and the bill lands somewhere between month one and month six. Green checkmarks start lying, half the suite turns red after one UI change, and eventually someone says the most dangerous phrase in software engineering, "nobody touches that file."

I pushed back with the obvious objection, is this not just normal tech debt, every codebase has it. His answer is the line I have been reusing ever since:

> "There is a massive difference between the tech debt you choose and the tech debt you inherited."

Chosen debt is a tracked trade-off. Inherited debt compiles silently in the background because nobody ever set a boundary to violate. Point Stefan.

He also prices out the classic offenders one by one, duplication, the God object, magic strings, weak assertions, and picks the single most expensive one to fix first if your suite is already a mess. His pick was not the one I expected.

##### Round 3: Architect Your Own Framework

The screen-share round. Stefan opens a real scaffold and we walk the four pillars we actually built with: a single source of truth, design patterns, test data management, and rule enforcement so nothing rots back into tribal knowledge.

I brought the internet favorite new take to the table, that the Page Object Model is dead in the AI era. Stefan did not blink:

> "If AI is making a mistake, you are not tweaking your AI correctly."

I wanted a fight and got agreement instead, because I have seen zero evidence an agent cannot handle page objects when the rules are written down. The honest confession from my side: we did not arrive at composition gracefully. We hit the inheritance wall, detoured through mixins, collected runtime collisions, and only then found components. That whole scar story is in the act, next to the ESLint setup Stefan says changed his entire life.

##### Round 4: Bring a Scaffold to Every Project

Stefan claim: a good framework should be built from scratch exactly once. Then it becomes a scaffold, a product you carry into every new project, and a week of setup turns into hours. But he draws a hard line on how you are allowed to use one:

> "Do not just download someone else's scaffold and copy paste without understanding it."

I build and sell a scaffold, and my first guest just told everyone not to blind-copy scaffolds. He is right, and that is exactly why I agreed. A scaffold is a template that carries your lessons, not a substitute for understanding them.

##### The Scoreboard

| Topic | Verdict |
| --- | --- |
| The iceberg | He survived my delete-the-specs trap without blinking |
| The maintenance bill | Chosen vs inherited debt is his line. I use it now. Point Stefan |
| POM in the AI era | We ganged up on the critics. Judge for yourself if it was too easy |
| Scaffold as a product | Even round. We built this opinion together |

Check out the full [ArchQA, Unscripted Playlist](https://www.youtube.com/playlist?list=PLYxABk1YARBwA0TgHQrsmSRsWl6uh-FK8):

- [Act 1, What Is Test Architecture](https://youtu.be/RO1RFTZgshM)
- [Act 2, The Cost of Bad Architecture](https://youtu.be/v_SkniVOeHM)
- [Act 3, Architect Your Own Framework](https://youtu.be/2ECFHA_crvE)
- [Act 4, Bring a Scaffold to Every Project](https://youtu.be/hAVH9k2MQtk)

