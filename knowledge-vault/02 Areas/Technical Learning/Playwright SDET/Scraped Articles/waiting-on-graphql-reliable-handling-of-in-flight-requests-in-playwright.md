[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

# Waiting on GraphQL: Reliable Handling of In-Flight Requests in Playwright

[![James Kip](https://miro.medium.com/v2/resize:fill:64:64/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---byline--a935129dfad9---------------------------------------)

[James Kip](https://medium.com/@jameskip?source=post_page---byline--a935129dfad9---------------------------------------)

3 min read

·

May 28, 2025

--

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Da935129dfad9&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40jameskip%2Fwaiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9&source=---header_actions--a935129dfad9---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

GraphQL-heavy applications often trip up end-to-end tests. Your test clicks a button, but the UI doesn't update in time — likely because the GraphQL request is still in flight. The result? Flaky tests and wasted CI cycles.

To solve this, we built a utility that tracks in-flight GraphQL requests and only proceeds once all of them are done. The key? Wrapping the triggering action and waiting intelligently. Here's how it works.

## 🧨 The Problem

We started running into flaky tests where an action like clicking a button would **navigate away from the current page before all in-flight GraphQL requests had completed**. This usually happened in multi-step flows — like submitting a form and immediately moving to the next screen.

The result: tests would intermittently fail with errors like:

```
Error: Timed out 15000ms waiting for expect(locator).toBeAttached()
```

These failures weren't caused by UI bugs — they were race conditions. Playwright would trigger a navigation or action before all relevant data was fully fetched and rendered from prior GraphQL queries.

Manual `waitForTimeout()` calls weren't reliable, and watching for loading indicators didn't always capture edge cases.

We needed a better way to **block the next step until all GraphQL requests finished** — without relying on brittle assumptions.

## ✅ The Goal

We wanted a reusable function that could:

- Monitor in-flight GraphQL requests
- Wait for all of them to finish
- Clean up after itself
- Stay decoupled from specific operation names

## 🧱 The Solution: `triggerAndWait`

Here's the exact utility we now use in our tests:

```
export const triggerAndWait = async (
  page: Page,
  triggerAction: () => Promise<null | Response> | Promise<void>,
): Promise<void> => {
  const inflightRequests = new Set<Request>()
  let actionTriggered = false
  let resolveAllRequestsFinished: () => void

  const allRequestsFinishedPromise = new Promise<void>((resolve) => {
    resolveAllRequestsFinished = resolve
  })

  const checkIfAllRequestsFinished = () => {
    if (inflightRequests.size === 0 && actionTriggered) {
      resolveAllRequestsFinished()
    }
  }

  const onRequest = (request: Request) => {
    if (actionTriggered && request.url().includes("/graphql")) {
      inflightRequests.add(request)
    }
  }

  const onRequestFinished = (request: Request) => {
    if (inflightRequests.delete(request)) {
      checkIfAllRequestsFinished()
    }
  }

  page.on("request", onRequest)
  page.on("requestfinished", onRequestFinished)
  page.on("requestfailed", onRequestFinished)

  actionTriggered = true

  await triggerAction()

  await allRequestsFinishedPromise

  page.off("request", onRequest)
  page.off("requestfinished", onRequestFinished)
  page.off("requestfailed", onRequestFinished)
}
```

## 🧪 Example Usage

Instead of doing this:

```
await page.getByRole("button", { name: "Submit" })
await page.getByRole("button", { name: "Next" }))

await expect(page.getByText("Success")).toBeVisible()
```

We now write:

```
await triggerAndWait(page, () => page.getByRole("button", { name: "Submit" }))
await page.getByRole("button", { name: "Next" })

await expect(page.getByText("Success")).toBeVisible()
```

This ensures Playwright doesn't move forward until **every** GraphQL request triggered by that action has finished.

## 🧼 Why It Works

This approach is:

- **Generic** — It doesn't rely on hardcoding operation names
- **Precise** — It tracks only requests fired _after_ the action begins
- **Self-cleaning** — It removes all event listeners after execution
- **Reusable** — Works across forms, modals, navigation, and complex workflows

We've seen a significant drop in flaky failures and more reliable CI runs since adopting this pattern.

## 🧠 Bonus Tip: Chain More Assertions

This utility doesn't just fix the first flake — it also stabilizes any downstream assertions:

```
await triggerAndWait(page, () =>
  Promise.all([\
    page.getByRole("button", { name: "Next" }).click()\
    page.getByRole("button", { name: "Submit" }).click()\
  ])
)
```

## 🚀 Takeaways

- Avoid relying on `waitForTimeout()` or loading indicators alone when testing async-heavy apps.
- Track the network layer directly to synchronize test actions with actual app behavior.
- `triggerAndWait` gives you reliability without coupling your tests to brittle frontend assumptions.

If you're struggling with GraphQL race conditions in Playwright, this pattern might save you hours of debugging — and dozens of reruns in CI.

[QA](https://medium.com/tag/qa?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Playwright Test](https://medium.com/tag/playwright-test?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Test Automation](https://medium.com/tag/test-automation?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[Software Development](https://medium.com/tag/software-development?source=post_page---footer_tags--a935129dfad9---------------------------------------)

[![James Kip](https://miro.medium.com/v2/resize:fill:96:96/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[![James Kip](https://miro.medium.com/v2/resize:fill:128:128/0*JuLhExTrl4FgAW3l.jpg)](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[**Written by James Kip**](https://medium.com/@jameskip?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[42 followers](https://medium.com/@jameskip/followers?source=post_page---post_author_info--a935129dfad9---------------------------------------)

· [15 following](https://medium.com/@jameskip/following?source=post_page---post_author_info--a935129dfad9---------------------------------------)

[jmekip.com](http://jmekip.com/)
