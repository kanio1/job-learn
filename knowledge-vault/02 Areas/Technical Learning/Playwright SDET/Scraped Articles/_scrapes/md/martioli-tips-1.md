As you work with a framework you start to encounter various situations from which you can learn. I would like to share some things I have recently learned about Playwright.

I recommend built-in locator methods like `getByTestId()` before falling back to `.locator()`.

### 1. Find child when only parent has unique id

`page.getByTestId(uniqueIDParent).filter({ hasText: "text you want" })` — filtering works miracles.

### 2. Browser has been closed error

Often caused by missing `await`. Disregard VS Code suggestions to remove awaits — you need them for sequential promise resolution.

### 3. Auto-waits

Playwright checks visibility, DOM attachment, and stability before interactions. Page load waits happen automatically. Built-in timeouts apply to expects (default 5s for toBeVisible).

### 4. waitForTimeout last resort

`waitForTimeout()` discouraged but sometimes necessary.

### 5. Assert array of strings

`expect(locator).toHaveText(array)` iterates array items.

### 6. Multiple elements

`page.locator(multipleElements).all()` returns iterable locators for each match.

### 7. Bigger chunks of text

`allTextContents()` or `allInnerTexts()` on parent — prefer `toContain()` over exact match.

### 8. Assert absence

Prefer `locator(element).not.toBeVisible()` over `toHaveCount(0)`.

### 9. When getByTestId is not enough

Use `.and()` or filtering locators before complex CSS/XPath.

### 10. Store parent locator

`const element = page.locator()` without await — re-queries DOM on each use.

### 11. Avoid $ and $$

Element handles can go stale — use locators.

### 12. Longer action timeouts

`expect(locator).toBeVisible({ timeout: 20000 })` overrides default for one assertion.

### 13. expect toBe vs toHaveText

Depends on object type — locators get web-first assertions with auto-retry; plain values use Jest matchers.

### 14. Multiple test id attributes

Cannot configure multiple testIdAttribute globally — use separate projects with different `testIdAttribute` for legacy vs new apps:

```javascript
  projects: [
    {
      name: "new-mega-awesome-app",
      use: {
        testIdAttribute: "id",
        baseURL: "https://newapp.domain.com",
      },
    },
    {
      name: "legacy-app",
      use: {
        testIdAttribute: "data-testid",
        baseURL: "https://legacyapp.domain.com",
      },
    },
  ],
```

Cross-links: tips #2 and #3, publishing reports to GitHub Pages, learning new automation frameworks efficiently.

Extended notes on locator strategy: prefer role and test id over brittle CSS paths; use filter({ hasText }) when parent containers repeat; chain getByTestId for table cells instead of nth-child selectors.

On async discipline: every page.goto, click, fill, and expect needs await unless storing a locator reference for later chained actions.

Auto-wait stack before click: attached → visible → stable → enabled → scroll into view → receive events — understanding this explains many "works in headed, fails in CI" timing issues.

For absence assertions, not.toBeVisible() retries until timeout; toHaveCount(0) is immediate — prefer NOT matchers for consistency with Playwright retry semantics.

Project-level testIdAttribute migration pattern supports strangling legacy apps while new surfaces ship with id= selectors — document which project maps to which environment in README.

When innerText() feeds expect(), you lose auto-retry — keep assertions on locators: `await expect(locator).toHaveText('x')` not `expect(await locator.innerText()).toBe('x')` unless value is computed offline.

Publishing Playwright HTML reports to GitHub Pages gives stakeholders permalink evidence without downloading zip artifacts from CI.

Learning framework efficiently: reproduce one happy-path test, one failure, one API mock before building full POM — matches Houseful/Zoopla standards for incremental adoption.

End of tips #1 archive content.
