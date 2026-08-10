# Playwright Interview Questions that you are going to hate

Questions were created with the idea to trick you, so don't be too hard on yourself if you don't get them right.

## 1. The local environment

If you would run your tests on your local machine using your local environment at http://localhost:3000/, how would you handle the local environment setup during test runs?

#### Answer

All possible answers are valid but the recommended one is to use webServer config.

## 2. The contain comparison

What is the main difference between `await expect(locator).toContainText()` and `await expect(value).toContain()`?

#### Answer

First one is an auto-retry assertion meaning that it will retry up to 5 seconds for the element to appear and the second will attempt to assert once.

## 3. The delayed load

Text "Loading complete" will appear after 10 seconds. Test fails with Expected: visible, Received: hidden. How could you fix this test?

#### Answer

Default expect timeout is 5 seconds. Fix: `await expect(page.getByText("LOADING COMPLETE.")).toBeVisible({ timeout: 12000 })`

## 4. The self-healing

Complete the test with a self-healing locator for the "Sale" menu on magento.softwaretestingboard.com.

#### Answer

Use the or operator to provide multiple locators pointing to the same element.

## 5. The closed browser

What will this code do when clicking About link?

#### Answer

Test will error: expect.toHaveURL: Target page, context or browser has been closed (missing await on click).

## 6. The reference

Clicking 'buy me a coffee' backlink - what happens with URL assertion?

#### Answer

Test will pass, because clicking a backlink generates an URL with a reference about our initial origin (blog.martioli.com).

## 7. The other locales

Modify the test to run with german locale.

#### Answer

Add `test.use({locale: 'de-DE'})` above the test.

## 8. The CSS properties

How to improve readability when checking background-color?

#### Answer

Use built-in toHaveCSS() instead of evaluate with getComputedStyle.

## 9. The long click

How to simulate hold left mouse button for 3 seconds?

#### Answer

`.click({delay: 3000})`

## 10. The force

Google consent modal hides search button. What happens with force:true click?

#### Answer

Test will pass, but no results shown behind the modal.

## 11. The baseUrl

What does `await page.goto('')` do with baseUrl configured?

#### Answer

Test will pass, an empty string does the same as "/".

## 12. The workers

workers: "5%" with 100 tests on 4 CPU cores - how do they run?

#### Answer

Test will run on 5% of how many CPU cores you have - with 4 cores you get just one worker.
