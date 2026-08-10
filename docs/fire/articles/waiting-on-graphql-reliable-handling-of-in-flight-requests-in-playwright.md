# Waiting on GraphQL: Reliable Handling of In-Flight Requests in Playwright

**Author:** James Kip  
**Published:** 2025-05-28  
**URL:** https://medium.com/@jameskip/waiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9

## Firecrawl Metadata

```json
{
  "author": "James Kip",
  "article:published_time": "2025-05-28T17:05:54.700Z",
  "publishedTime": "2025-05-28T17:05:54.700Z",
  "og:title": "Waiting on GraphQL: Reliable Handling of In-Flight Requests in Playwright",
  "og:description": "GraphQL-heavy applications often trip up end-to-end tests. Your test clicks a button, but the UI doesn't update in time — likely because…",
  "sourceURL": "https://medium.com/@jameskip/waiting-on-graphql-reliable-handling-of-in-flight-requests-in-playwright-a935129dfad9",
  "statusCode": 200,
  "scrapeId": "019fb3ca-92fe-731e-99e8-252c80bd7eb6"
}
```

---

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
