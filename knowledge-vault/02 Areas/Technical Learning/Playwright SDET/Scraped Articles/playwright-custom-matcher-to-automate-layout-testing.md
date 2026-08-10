# Using Playwright Custom Matchers to Automate Layout Testing

Layout testing is the test of a web page's components — buttons, input boxes, radio buttons, text labels etc.

## What causes a layout failure

- CSS styles may conflict while targeting the same element
- Layout changes on viewport change happen due to media queries in CSS
- Mixed use of third party components may cause issues

## Existing Tools

### Visual Regression Tools

Applitools Eyes, BackstopJS, ImageMagick compare — pixel comparison adds storage and runtime cost.

### Galen

Galen describes layout with relative positioning syntax like `comments: width 300px; inside screen 10 to 30px top right`.

## Playwright Custom Matchers

Extend assertions with `expect.extend()` for Galen-style layout checks:

```
await expect(playwrightDev.writingTestsNavLink).toBeLeftOf(
  playwrightDev.installationPageHeader,
);
await expect(playwrightDev.writingTestsNavLink).toBeAbove(
  playwrightDev.supportedLanguagesNavLink,
);
```

Custom matchers like `toBeLeftOf` compare bounding boxes. Full code at github.com/hrmeetsingh/playwright-layout-testing.

Layout tests can run first in the suite — if UI is broken, skip functional tests. Viewport-specific assertions supported via conditional logic.

Thanks for reading!!
