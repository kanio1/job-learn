I've written a post about tips and tricks and it got a lot of love. So, I've decided to do another one.

### 1. How to handle an element that appears after full page load

- First option: `page.goto('https://playwright.dev/', {waitUntil: 'networkidle'})`
- Second option: `await element.waitFor("attached")` after `const element = page.locator('#locator')`

### 2. Custom expect message

`await expect(locatorOrValue, 'Failed to perform something').toBe()`

### 3. Assert receiving email with expect.poll

```javascript
await expect.poll(async () => {
  const allEmails = await page.request.get('https://api.email.com/allEmails')
  for (email of allEmails){
      if (email.to.includes("registrationForm@email.com"))
          return emailCode
  }
  return false;
}, {
  message: 'Failed to find confirmation link in email',
  intervals: [1_000, 2_000, 10_000],
  timeout: 60_000
}).toBeTruthy();
```

### 4. expect.toPass for retrying assertions

```javascript
test("Element appears after 5 seconds", async ({ page }) => {
    await page.goto("https://webdriveruniversity.com/Accordion/index.html");
    await expect(async () => {
        await expect(page.getByText("LOADING")).toBeVisible()
        await expect(page.getByText("COMPLETE.")).toBeVisible()
    }).toPass({
        intervals: [1_000, 5_000, 10_000],
        timeout: 60_000
    });
});
```

### 5. Intercept network call with waitForResponse

```javascript
const invoiceCall = page.waitForResponse("**/invoices/*");
await page.getByText("Generate Invoice").click();
const response = await invoiceCall
const responseAsJson = await response.json();
await expect(responseAsJson.invoice.value).toBe("355");
```

### 6. Debug with Playwright inspector

`npx playwright test path/to/yourTest.spec.js --debug` — inspect under-the-hood actions in the inspector.

### 7. Get text and store for reuse

`const elementText = await page.locator(locator).innerText()` — use `textContent()` for hidden text.

Additional tips from the series cover vertical vs horizontal E2E testing, listening for responses before triggers (SINCE WHEN pattern), and linking to the dedicated debugging article on trace viewer, VS Code extension, and inspector workflows.

The post references Playwright tips #1 and #3, external Rayrun guidance on efficient test scripts, and publishing reports to GitHub Pages.

Topics covered in depth: handling late DOM elements after full load, email polling with configurable intervals, soft assertions vs toPass retry blocks, API response validation alongside UI assertions for invoice flows, and practical debugger workflows when a single locator interaction misbehaves in CI vs local runs.

When intercepting API calls, remember `waitForResponse` without await starts listening immediately — pair with the triggering action using Promise.all when race conditions appear, as covered in tips #3.

For email polling, returning false inside expect.poll continues the loop until timeout; only a truthy return satisfies toBeTruthy and ends polling successfully.

Custom expect messages propagate into HTML reports and CI annotations, making failures in page object helpers easier to triage without reading stack traces alone.

Vertical testing validates both UI display and backend payload at the interception point — critical when frontend and backend teams disagree on who owns a calculation bug.

Inspector under-the-hood logs show every auto-wait, scroll-into-view, and actionability check Playwright performs before a click resolves — invaluable when migrating from Selenium mental models.

Storing innerText in a variable switches expect to Jest matchers without auto-retry — prefer keeping locators in expect() when possible for stability.

Series cross-links: tips #1 covers locator fundamentals; tips #3 covers testInfo, multi-window, Promise.all; debugging article covers trace.playwright.dev and VS Code watches.

External reading: Rayrun efficient Playwright scripts; publish reports to GitHub Pages for shareable CI artifacts in Jira tickets.

End of tips #2 reference content for scraping archive purposes.
