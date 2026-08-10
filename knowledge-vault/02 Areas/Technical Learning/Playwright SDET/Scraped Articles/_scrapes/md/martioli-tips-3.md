It's time to have a look under the hood of Playwright and understand a few details that will enhance our skills to be more creative.

## 1. Get more details about your test during test run

You can access particular values related to your test in real time via the `testInfo` object:

```javascript
import { test } from "@playwright/test";

test.describe('test suite name', () => {
  test("test name", async ({ page }, testInfo) => {
    console.log(`test name: ${testInfo.title}`)
    console.log(`parallel index :${testInfo.parallelIndex}`)
    console.log(`shard index: ${JSON.stringify(testInfo.config.shard)}`)
  });
});
```

## 2. How to use Playwright to test multiple browser windows?

Use separate browser contexts for multiple windows (not tabs):

```js
test("Two users chat functionalities", async ({ browser }) => {
    const user1Context = await browser.newContext()
    const user1Page = await user1Context.newPage()
    const user2Context = await browser.newContext()
    const user2Page = await user2Context.newPage()
    await user1Page.goto("https://www.yourweb.com/chat")
    await user2Page.goto("https://www.yourweb.com/chat")
    await user1Page.getById("input").type("Hello user 2")
    await user1Page.getById("sendMsgBtn").click()
    await expect(user2Page.getByText("Hello user 2")).toBeVisible()
});
```

## 3. How Playwright handles multiple tabs in the same browser?

For `target="_blank"` links, use `context.waitForEvent('page')`. Remember:

> browser.newContext() = new window
> context.newPage() = new tab

## 4. How to handle multiple types of browsers inside a test?

```js
import { test , webkit, firefox, chromium } from "@playwright/test";

test("Multiple browser drivers", async () => {
    const browser = await webkit.launch()
    const context = await browser.newContext()
    const page = await context.newPage()
    await page.goto("https://martioli.com/")
});
```

## 5. Can I override Playwright configurations from within my test?

Two ways: create a project in config, or use `test.use()`:

```js
test.use({
    geolocation: { longitude: 36.095388, latitude: 28.0855558 },
    userAgent: 'my super secret Agent value'
})

test("Override config", async ({ page }) => {
    await page.goto("https://martioli.com/")
})
```

## 6. Promise.all in Playwright

Race conditions occur when waitForResponse runs after the click already completed:

```javascript
const [response] = await Promise.all([
  page.locator("button").click(),
  page.waitForResponse("https://example.com/api/search")
]);
```

Many Playwright events must execute concurrently with their triggers using Promise.all.
