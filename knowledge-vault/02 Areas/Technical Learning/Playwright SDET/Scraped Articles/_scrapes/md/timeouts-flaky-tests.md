# Timeouts Against Flaky Tests: True Cases with Playwright

Playwright is a highly reliable tool for UI testing, but there are cases when some tests can be flaky, and the timeout anti-pattern fixes this!

Flaky tests are unreliable tests that occasionally fail, but the reason for the failures is unclear. When comparing tools based on Selenium WebDriver and those based on Chrome DevTools Protocol (Puppeteer and Playwright), the last one turns out to be more accurate.

Even adhering to the best practices in writing autotests to prevent false falls, unreliable results, and unwanted errors sometimes requires breaking established principles and setting exceptions in the code.

Below are a few cases:

1. UI Animations
2. Drawing on Canvas
3. Sleep Timeouts in CI in the Cloud

> **Disclaimer:** The following may be very controversial. If you want to adopt the suggestions in the text, you should be aware of what you are doing.

## 1. UI Animations

Playwright performs a range of actionability checks on the elements before making actions. But sometimes, very rarely, Playwright may not click (or misclick) on truly visible and accessible elements.

This may suddenly appear during animations of menus, drawers, popovers, overlays, and other pop-up elements with transitions. When the element is already on the screen, it is already interactable, but the animation is not over yet.

**Solution:** Add unconditional timeout before or after flaky click.

## 2. Drawing on Canvas

Interaction with `<canvas>` elements can also be tricky. The element is actionable for Playwright from the moment it appears on the page, but the actual actionable state of the element is indefinable by its locator.

The HTML element is the same for all states, but when Playwright is already getting ready to perform clicks, the element itself is not.

**Solution:** Add unconditional timeout before the first click.

Both issues are the most common root causes of UI-based flaky tests, according to the 2021 study "An Empirical Analysis of UI-based Flaky Tests".

## 3. Sleep Timeouts in CI in the Cloud

If your frontend application has its own timeouts (actual business logic may require periods of "sleep"), then your tests may be affected by it.

`setTimeout()` can be executed longer than the specified time in CI runners during testing and does not reproduce in production!

**Solution:** Add unconditional timeout that is at least three times longer than the logic under test.

## Timeout Tips

- **Make timeout's value as constant.** In my experience, 300 ms is the optimal time waiting for something on the UI.

```typescript
export const UNCONDITIONAL_TIMEOUT = 300;
```

- **Add a comment for each case of using timeout.**

```typescript
// Wait for input to be applied in the select
await page.waitForTimeout(UNCONDITIONAL_TIMEOUT);
```

UPDATE: Issues #1 and #2 can be solved another way instead of timeouts. Actions with checks may be wrapped in `expect.toPass()` method to retry a flaky sequence.

Read more:
- What is Flaky Test?
- We Have A Flaky Test Problem
- How to Avoid Flaky Tests in Playwright

Watch more:
- Avoid flaky end-to-end tests due to poorly hydrated Frontends with Playwright's toPass()
- Hydration documentation in Playwright navigations guide
