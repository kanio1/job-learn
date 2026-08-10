Title: One Test, Two Wins: Run UI and Accessibility Tests from Your Playwright Page Objects with…

URL Source: https://medium.com/@evgeniy.otsevich/one-test-two-wins-run-ui-and-accessibility-tests-from-your-playwright-page-objects-with-706da09fdaec

Published Time: 2025-06-10T09:50:21Z

Markdown Content:
[![Image 1: Yevhenii Otsevych](https://miro.medium.com/v2/resize:fill:32:32/1*tGXbJ4iHKiqTql26Dd65wQ.jpeg)](https://medium.com/@evgeniy.otsevich?source=post_page---byline--706da09fdaec---------------------------------------)

5 min read

Jun 10, 2025

It’s generally good practice to maintain a separate test suite for accessibility automation. Reusing Page Objects Methods and E2E UI tests will help build an accessibility test suite with some extra effort. I encountered two problems:

1.   When user flows were changed, it was required to update both E2E and a11y (accessibility) tests.
2.   There was no accessibility report tool with a dashboard, only a per-page scan report

Since E2E UI tests are already navigating through the website, why not integrate accessibility scans during test execution? Instead of manually adding scan logic to each Page Object method, the [**axe-playwright-report**](https://www.npmjs.com/package/axe-playwright-report) library provides an automated, decorator-based solution.

Press enter or click to view image in full size

![Image 2](https://miro.medium.com/v2/resize:fit:700/1*Yr3nXdu0D51f3iyFVM_W3A.jpeg)

_The Decorator Pattern in Test Automation: Layering functionality with modern JavaScript decorators._

### The @axeScan decorator

@axeScan is applied directly above an asynchronous method inside a Page Object class. At runtime, it wraps the original method execution and enhances it with accessibility checks powered by axe-core.

import { axeScan } from 'axe-playwright-report';
class MyTest {

@axeScan()

 async openHomePage() {

 await this.page.goto('https://example.com');

 }

}

When the decorated method is invoked, the following occurs:

1.   The method runs its original logic (e.g., navigating to a page or interacting with elements).
2.   After the main logic completes, the decorator triggers an automated accessibility scan.
3.   Results from the scan are collected and saved to .json file
4.   Control is returned to the test runner after the scan finishes.

This is the core concept — extending Page Object methods with built-in accessibility scans. By running your existing E2E UI test suite, you automatically generate accessibility results for each visited page. Instead of maintaining separate tests that mirror your UI flows, this library integrates accessibility checks directly into them.

**Limitations**

The Page Object Class must contain an object of type `Page`. If you decompose the page and use `Locator` as a base for searching elements, the accessibility scan will be skipped.

**Applicable ✅**`new HomePage(page)`

class HomePage {

 readonly page: Page;
constructor(protected page: Page) {

 this.page = page

 }

@axeScan()

 async openHomePage() {

 await this.page.goto('https://example.com');

 }

}

In tests:

test('User can login and logout', async ({ page }) => {

 const homepage = new HomePage(page);

 await homePage.openHomePage();

 ...

});
**Not-applicable ❌**`new sideMenu(page.locator('#sideMenu')`

export class Table {

 private tableBase: any;
constructor(tableBase: Locator) {

 this.tableBase = tableBase

 }

This approach is used when you want smaller classes representing logical parts or sections of the UI — in other words, when decomposing the Page Object. To retain access to full-page functionality, the solution is to pass the `Page` object as a second parameter.

new sideMenu(page.locator('#sideMenu', this.page)
### Configure scan options

The accessibility environment file `.env.a11y` allows you to customize scan settings. The configuration file offers the following options:

*   enable/disable scanning (default: `on`)
*   custom output directory (default: `axe-playwright-report`)
*   enable/disable screenshot capture (set to `on` to capture screenshots of issues, default: `off`)
*   filter rules by axe-core tags (default: `no filtering, all rules included`)

SCAN=on

OUTPUT_DIR=custom-report-dir

SCREENSHOT=on

TAGS=wcag2a,wcag2aa
### Building the Dashboard Report

Of course, there should be a way to analyse the accessibility results. After tests execution — build the dashboard with the next command and let the magic happen:

npx axe-playwright-report build-report
But I’m not a magician — let me explain what’s happening behind the scenes.

## Get Yevhenii Otsevych’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

One of the main features of building a dashboard relates to the core idea of the Page Object Pattern: reusable methods across multiple tests. This means a single method can be used in many tests, resulting in multiple scans of the same page. That’s expected — running different tests may reveal new accessibility issues even on the same URL.

To avoid displaying duplicates, the build-report command includes a de-duplication algorithm. It compares all reports with the same URL and keeps only the most relevant one, based on:

*   Number of **violations** (must-fix issues)
*   Number of **incomplete** checks (unable to determine automatically — need manual attention)
*   **Timestamp** of the scan

Ta-da-dam! Now you have an understandable report that aggregates all accessibility scans. The report consists of a Dashboard page and Report pages

### Dashboard Page Overview

Press enter or click to view image in full size

![Image 3](https://miro.medium.com/v2/resize:fit:700/1*zgzJfXSxojPGnoaWi3AJww.png)

_Dashboard Page Overview: A centralized view displaying key metrics, system status, and recent activity._

*   Displays the total number of scanned pages and the total issues found
*   Shows severity breakdown: violations, incomplete checks, passes
*   Includes an Impact Distribution Chart for issue severity
*   Includes a Disabilities Affected Chart to show affected user groups
*   Contains a table of reports with columns like page name, URL, violations, and scan data
*   Clicking a report entry opens its detailed Report Page

### Report Page Overview

Press enter or click to view image in full size

![Image 4](https://miro.medium.com/v2/resize:fit:700/1*6gVQFZ1p9O-AZqZ8DLnk2g.png)

Signle Report Page for SauceDemo Inventory Page

*   Shows page title, URL, and browser/device info
*   Provides filtering by impact, standard, and disability
*   Group issues by type (violations, incomplete, inapplicable)
*   Each issue includes rule ID, impact, description, and tags
*   Expandable details include help text, screenshots (if enabled), and affected elements

### A few words about screenshots

Another feature is the ability to take a screenshot of a page, with the ability to highligts problem elements. Since axe-core provides “target” (basically a locator to an element), **axe-playwright-report**can find the element, put it in the red frame, and screenshot it. Slime!

Press enter or click to view image in full size

![Image 5](https://miro.medium.com/v2/resize:fit:700/1*TpeYdfP15Hp6YsUqxW81gA.png)

Issue Detail Section: Screenshot with highligted elements

### **Backward Compatibility with Axe-core/playwright**

If you have existing accessibility tests with axe-core/playwright, you can still use this library to build the dashboard. Just save your existing scan into .json files in the `axe-playwright-report/pages` directory and run the `build-report` command.

### Final Thoughts

Of course, this approach won’t give you 100% accessibility coverage — and that’s okay. The scans only run on pages your UI tests actually touch. But that’s kind of the point: if a page or flow isn’t being scanned, it’s probably not being tested at all.

For me, @axeScan() was a great way to start doing accessibility testing with almost zero extra effort. You get immediate value by scanning everything your UI tests already cover.

And what if, upon reviewing the results, you realize certain pages or flows were not covered? That’s a good thing — it means your E2E coverage needs attention too. Think of it as an accessibility check and a test coverage audit in one.

### Links

github: [https://github.com/eotsevych/axe-playwright-report](https://github.com/eotsevych/axe-playwright-report)

npm: [https://www.npmjs.com/package/axe-playwright-report](https://www.npmjs.com/package/axe-playwright-report)

