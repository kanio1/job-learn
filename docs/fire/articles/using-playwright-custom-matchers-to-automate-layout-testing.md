# Using Playwright Custom Matchers to Automate Layout Testing

**Author:** Harmeet Singh  
**Published:** 2025-04-23  
**URL:** https://hrmeet.medium.com/using-playwright-custom-matchers-to-automate-layout-testing-be8fa0063ebd

## Firecrawl Metadata

```json
{
  "author": "Harmeet Singh",
  "article:published_time": "2025-04-23T14:46:11.216Z",
  "publishedTime": "2025-04-23T14:46:11.216Z",
  "og:title": "Using Playwright Custom Matchers to Automate Layout Testing",
  "og:description": "Layout testing is the test of a web page's components — the buttons, input boxes, radio buttons, text labels etc. The term is sometimes…",
  "sourceURL": "https://hrmeet.medium.com/using-playwright-custom-matchers-to-automate-layout-testing-be8fa0063ebd",
  "statusCode": 200,
  "scrapeId": "019fb3cc-9fc3-771a-8976-3e939d04f456"
}
```

---

Layout testing is the test of a web page's components — the buttons, input boxes, radio buttons, text labels etc. The term is sometimes used for testing using an image comparison tool as well, but for this context, lets not confine Layout testing to pixel-to-pixel comparisons.

Layout of a responsive webpage changes based on factors like device orientation, viewport of the browser etc

Example of a portrait vs landscape layouts of a login page

Notice the changes here

- Landscape mode has Labels on left of Input boxes while Portrait moves the labels above the input boxes
- Button/CTA arrangement changes from being horizontally aligned in Landscape to vertically aligned in Portrait

If the webpage support both LTR (Left to Right) and RTL (Right to Left) layouts, the page layout flips to being a mirror image of itself.

## What causes a layout failure

Layout failure is an unintended change in layout or any unapproved change that does not aligns with the design. Some root causes that I have seen are

- CSS styles may conflict while targeting the same element. Though less likely when UI devs follow popular class naming conventions like BEM etc, but remains a risk
- Layout changes on viewport change happen due to media queries in CSS. If the breakpoints are not defined properly, the layout might break
- Mixed use of third party components like Shadcn with vanilla Tailwind CSS may cause issues due to conflicts

## Existing Tools

### Visual Regression Tools

There are tools that are already being used for automating Layout tests like [Applitools Eyes](https://applitools.com/), [BackstopJS](https://github.com/garris/BackstopJS), [ImageMagick compare](https://imagemagick.org/script/compare.php) etc that do image comparison.

Even [Cypress](https://docs.cypress.io/app/tooling/visual-testing) and [Playwright](https://playwright.dev/docs/test-snapshots) have in-built support for visual regression which is the "create a baseline, compare new screenshots with baseline" approach

The problem with image comparison for layout tests is though it catches even the slightest deviations from benchmarks, the benchmark image storage(even most efficient lossless compressions take considerable space) and image comparison adds to the overall test run times

### Galen

[Galen](https://galenframework.com/) is a good alternative for automating a Layout test without stepping into the Visual Regression realm.

> Layout testing seemed always a complex task. Galen Framework offers a simple solution: test location of objects relatively to each other on page. Using a special syntax and comprehensive rules you can describe any layout you can imagine
>
> \- Galen documentation

The tests in Galen look like this -

```
@objects
    comments            #comments
    article-content     div.article

= Main section =
    @on mobile, tablet
        comments:
            width 300px
            inside screen 10 to 30px top right
            near article-content > 10px right

    @on desktop
        comments:
            width ~ 100% of screen/width
            below article-content > 20px
```

As the developers of Galen claim, you can describe a layout by relatively describing elements on a page. Galen also supports grouping the layout in tests based on viewports like @mobile, @tablet.. etc

Here are examples of a vertical and horizontal alignment tests

Galen recommends not to do assertions like classname matching etc but instead focusing on the layout aspect. Working with Galen, I used to read out the layout as if describing the webpage to a stranger over phone. Button is 30px left of the label, radio button is left of the input box.. yes, like that.

So when I started working on Playwright, I kind of missed the syntax of Galen. This is when I started exploring custom matchers to define assertions that sound like Galen assertion. (I actually did this first with Cypress which had the similar capability)

## What are Playwright Custom Matchers

Playwright, by default has Chai-style [assertions](https://playwright.dev/docs/api/class-genericassertions) like the following

```
// Generic assertions
class Example {}
expect(new Example()).toEqual(expect.any(Example));
expect({ prop: 1 }).toEqual({ prop: expect.any(Number) }); // Match any number
expect('abc').toEqual(expect.any(String)); //match string
expect({ prop: 0.1 + 0.2 }).not.toEqual({ prop: 0.3 });
expect({ prop: 0.1 + 0.2 }).toEqual({ prop: expect.closeTo(0.3, 5) });

//Locator assertions
const locator = page.locator('div.warning');
await expect(locator).toBeEmpty();
await expect(locator).toBeInViewport();
await expect(locator).toContainClass('middle selected row');

//Page assertions
await expect(page).toHaveURL(/.*\/login/);
```

However, you can extend the assertions to define own assertions( [read how](https://playwright.dev/docs/test-assertions#add-custom-matchers-using-expectextend)). In my case, I used this capability to define Galen style layout assertions.

So my tests now look like the following

Notice that " **toBeLeftOf**" and " **ToBeAbove**" are custom matchers that handle how the bounding boxes of the selected elements align with respect to each other

Sample code of the " **toBeLeftOf"** matcher is

```
import { expect as baseExpect } from "@playwright/test";
import type { Locator } from "@playwright/test";
import { BoundingBox } from "./types";

export { test } from "@playwright/test";

export const expect = baseExpect.extend({
  async toBeLeftOf(
    referenceLocator: Locator,
    comparandLocator: Locator,
    horizontalAlignment?: {
      allSide: Boolean;
      top: Boolean;
    },
    options?: {
      timeout?: number;
    },
  ) {
    const assertionName = "toBeLeftOf";
    let pass: boolean;
    let matcherResult: any;
    let referenceBoundingBox: BoundingBox | null;
    let locatorBoundingBox: BoundingBox | null;
    try {
      referenceBoundingBox = await referenceLocator.boundingBox(options);
      locatorBoundingBox = await comparandLocator.boundingBox(options);

      baseExpect(referenceBoundingBox?.x).toBeLessThanOrEqual(
        locatorBoundingBox?.x as number,
      );
      if (horizontalAlignment?.allSide) {
        baseExpect(referenceBoundingBox?.y).toEqual(
          locatorBoundingBox?.y as number,
        );
        baseExpect(
          (referenceBoundingBox?.y as number) +
            (referenceBoundingBox?.height as number),
        ).toEqual(
          (locatorBoundingBox?.y as number) +
            (locatorBoundingBox?.height as number),
        );
      }

      pass = true;
    } catch (e: any) {
      matcherResult = e.matcherResult;
      pass = false;
    }

    const message = pass
      ? () =>
          this.utils.matcherHint(assertionName, undefined, undefined, {
            isNot: this.isNot,
          }) +
          "\n\n" +
          `Locator1: ${referenceLocator}\n` +
          `Locator2: ${comparandLocator}\n` +
          `Expected: '${this.isNot ? referenceLocator + "' to not be left of '" + comparandLocator : ""}'\n` +
          (matcherResult
            ? `Received: ${this.utils.printReceived(locatorBoundingBox?.x)}`
            : "")
      : () =>
          this.utils.matcherHint(assertionName, undefined, undefined, {
            isNot: this.isNot,
          }) +
          "\n\n" +
          `Locator1: ${referenceLocator}\n` +
          `Locator2: ${comparandLocator}\n` +
          `Expected: locator1(${this.utils.printExpected(referenceBoundingBox?.x)})` +
          (matcherResult
            ? ` to be left of locator2(${this.utils.printReceived(locatorBoundingBox?.x)})`
            : "");

    return {
      message,
      pass,
      name: assertionName,
      actual: matcherResult?.actual,
    };
  },
});
```

The layout test is like the following

```
import { Page } from "@playwright/test";
import { test, expect } from "../fixtures/layout-matchers";
import { PlaywrightDevPage } from "../pages/playwright-dev-page";

let playwrightDev: PlaywrightDevPage;

test.beforeEach(async ({ page }) => {
  playwrightDev = new PlaywrightDevPage(page);
  await playwrightDev.goto();
  await playwrightDev.getStarted();
});

test("Page layout test - element left of other element", async ({ page }) => {
  // nav link is horizontally left of the page header
  await expect(playwrightDev.writingTestsNavLink).toBeLeftOf(
    playwrightDev.installationPageHeader,
  );
});

test("Page layout test - element above other element", async ({ page }) => {
  // nav links are vertically positioned
  await expect(playwrightDev.writingTestsNavLink).toBeAbove(
    playwrightDev.supportedLanguagesNavLink,
  );
});

test("Page layout test - element above other element and left vertical aligned", async ({
  page,
}) => {
  // nav links are vertically positioned and left edges vertically aligned
  await expect(playwrightDev.installationNavSubLink).toBeAbove(
    playwrightDev.writingTestsNavLink,
    {
      allSide: true,
      left: true,
    },
  );
});

test("Page layout test - element left of other element and horizontally aligned", async ({
  page,
}) => {
  // nav links are horizontally positioned and top edges horizontally aligned
  await expect(playwrightDev.breadcrumbGettingStartedLink).toBeLeftOf(
    playwrightDev.breadcrumbInstallationLink,
    {
      allSide: true,
      top: true,
    },
  );
});
```

The full code with detailed usage for the matchers in tests is available here

[**GitHub - hrmeetsingh/playwright-layout-testing: Page layout testing example (non visual)**](https://github.com/hrmeetsingh/playwright-layout-testing)

When the layout does not match the description of test, description is provided to help triage

**Custom matchers are 'Not'able**

You can use a negative assertion like

```
test("Page layout test - element is not above other element", async ({ page }) => {
  await expect(playwrightDev.writingTestsNavLink).not.toBeAbove(
    playwrightDev.supportedLanguagesNavLink,
  );
});
```

By using custom matchers, I could integrate layout testing in my automation suite without adding a new tool to the stack and is really fast. This example is for two matchers only but the same can be done with more assertions.

One approach of using Playwright Layout tests in functional testing suite is to have the layout tests run first and not proceeding with functional if UI is broken, all within the same test run.

For supporting the viewport layout scenarios (mobile vs desktop and landscape vs portrait), Playwright can be made to conditionally run assertions based on the viewport size.

Please consider contributing to the repository if you like the idea.

Thanks for reading!!
