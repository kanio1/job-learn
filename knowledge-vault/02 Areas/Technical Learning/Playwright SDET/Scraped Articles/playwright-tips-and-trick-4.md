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

