Title: 🎭 Part 2 — Count Me Out & Assert Me Wrong: Two Sneaky Playwright Pitfalls

URL Source: https://0xislamtaha.medium.com/part-2-count-me-out-assert-me-wrong-two-sneaky-playwright-pitfalls-5b53639f645f

Published Time: 2025-05-13T20:29:57Z

Markdown Content:
[![Image 1: Islam Taha](https://miro.medium.com/v2/resize:fill:32:32/1*9h2SqBzmGUKR0jYi0GNb4g.jpeg)](https://0xislamtaha.medium.com/?source=post_page---byline--5b53639f645f---------------------------------------)

3 min read

May 13, 2025

Press enter or click to view image in full size

![Image 2](https://miro.medium.com/v2/resize:fit:700/1*7mnV35aP8jRwcU8Y_dyaAA.jpeg)

Hey friends! 👋 Remember how we talked about relying on our **e2e tests** and **synthetic tests** as quality gates for daily releases in [part #1](https://medium.com/@0xislamtaha/fighting-false-alerts-in-our-playwright-test-suite-my-battle-for-stability-%EF%B8%8F-part-1-6a6802fa38de)? Well, here’s the hard truth: **Stability doesn’t come for free.** 💸

We had to put in _a ton_ of work to make sure our test suites weren’t crying wolf every other day. Because let’s be real — **nobody** wants to be dragged into a 🔥 _grilled_ 🔥 call just because a flaky test blocked half the company’s release pipeline! (And yes, the _false alerts_ cost? _Painful._ 💔)

So, **mission clear:** Fail **only** when there’s a _real_ issue — not because of some sneaky test instability.

## Misunderstood Playwright Behaviors!🤯

Here’s the kicker — some of our flakiness came from **misreading how Playwright’s functions actually work.** And honestly? It’s _so easy_ to get tripped up, especially when function names are… _a little misleading._ 😅

## 🔍 Scenario #1: The `.count()` Deception

**Problem:** We needed to get the number of items on a page. Simple, right? First instinct?

let totalCount = page.locator('[data-testid="listItems"]').count();
**False sense of security:**“Hmm, maybe it’s async?” So we `await`it:

let totalCount = await page.locator('[data-testid="listItems"]').count();
**But… surprise!** 🎭 `.count()`**does NOT wait for all elements to load.**

*   It returns **immediately** with whatever’s in the DOM _at that moment_.
*   If your list is still loading? **Boom.** Flaky count.

**Our reaction:** 😱 _“Wait, WHAT?!”_ After digging through docs (and [GitHub](https://github.com/microsoft/playwright/issues/14278)issues), we realized:

> `.count()`**_is not your patient friend. It’s that one colleague who replies ‘Done!’ before actually checking._**

## 🚨 The `.all()` Trap (Plot Twist!)

_“Okay, fine! What about_`.all()`_? That should wait for everything, right?"_

**NOPE.** 🙅‍♂️

*   **Same issue!**`.all()` also **doesn’t wait** for all elements to exist.
*   It just grabs what’s available _right now_ and gives you the result.

## 💡 The Fix? Wait for Stability!

Instead of blindly trusting `.count()` or `.all()`, we **forced stability** by:

*   **Waiting for a specific count** (e.g., `await expect(await page.locator("[data-testId='listItems']")).toHaveCount(5)`).
*   **Or, even better — waiting for _at least_ some items** (e.g.,`await expect(await page.locator("[data-testId='listItems']").count()).toBeGreaterThanOrEqual(5)`).

## “Assertions Are Straightforward, Right?” (Spoiler: NOPE.)

You’d _think_ assertions are the _least_ confusing part of testing. **WRONG.** Playwright has _two_ types of assertions, and if you mix them up? **Hello, flakiness my old friend.** 😭

## Get Islam Taha’s stories in your inbox

Join Medium for free to get updates from this writer.

Remember me for faster sign in

Here’s the kicker:

1.   **Auto-Retry Assertions** — The patient heroes. They keep trying until success (or timeout). _And they return promises!_
2.   **Non-Retry Assertions** — The impatient ones. They pass/fail **immediately**. _No promises, no retries, no mercy._

## 💥 Scenario #2: The Deadly Assertion Mix-Up

We _thought_ we fixed our `.count()` problem with:

expect(await locator.count()).toBeGreaterThan(5);
**BUT SURPRISE!** 🎭

*   `.count()`**doesn’t wait** (we know this now).
*   `.toBeGreaterThan()` is a **non-retry assertion**—so it also **doesn’t wait!**

**Result?** A flaky mess. Again. 🤦‍♂️

## 🚨 The Real Problem: Sync + Async = Chaos

We needed:

*   **Get the count** (but _wait_ for stability).
*   **Assert the count** (but _keep retrying_ until valid).
*   **Timeout gracefully** (no infinite loops, please).

**Translation:** We needed to **convert a non-retry assertion into an auto-retrying one!**

## 💡 The Fix? `expect.toPass()` to the Rescue!

Playwright gives us **two magic tools** for this:

*   `expect.toPass()` – Wraps _any_ assertion in a retry loop.
*   `expect.poll()` – Lets you poll custom logic with retries.

### Here’s what saved us:

await expect(async () => {

 expect(await page.locator('[data-testid="listItems"]').count())

 .toBeGreaterThan(5);

}).toPass({ timeout: 2 * 60 * 1000 });
**What’s happening here?**✅ Retries the entire block until it passes (or hits timeout).

✅ No more flakiness from partial loads!

✅ Explicit timeout control (because defaults won’t save you).

**Tags:** Playwright, Testing, E2E Testing, Test Automation, Flaky Tests, Software Quality, Web Development

