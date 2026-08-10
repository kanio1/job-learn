### Offline but Not Broken: Testing Cached Data with Playwright

Modern web applications are expected to work even when the network doesn't. Whether it's a flaky connection or complete offline mode, users should still see relevant content thanks to technologies like Service Workers and the Cache API. In this post, we'll walk through a simple demo app that uses cached data to stay functional offline, and show how to write automated Playwright tests to verify that behavior — including both success and failure scenarios.

#### Demo App Overview

To demonstrate offline behavior, we've built a minimal static web app consisting of three files: index.html, data.json, and a Service Worker script (sw.js). When the page loads, it fetches text from data.json and displays it on the screen. If the network request fails, it shows a fallback message instead.

The Service Worker is responsible for caching both the HTML and JSON files after the first successful visit. This setup allows us to test whether the app can still show previously loaded content when offline.

When online, it fetches "Hello from the network!" from data.json. When offline and cached correctly, the same message should appear, served from the browser cache.

#### Caching Test: Showing Data When Offline

To verify that our app displays cached data when offline, we'll walk through a Playwright test that simulates a typical user journey: visiting the site online, caching the content, and revisiting it later without a network connection.

We begin by ensuring the browser context is online and navigating to the app:

```javascript
await context.setOffline(false);
await page.goto("your-page-url");
```

Once the page loads, we check that the text from data.json is displayed correctly:

```javascript
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

This confirms that the app fetched and rendered the network data as expected. Next, we reload the page to give the Service Worker a chance to cache the content for offline use:

```javascript
await page.reload();
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

We verify the same data appears again, ensuring that the reload didn't break anything and likely triggered the caching process. Now we simulate going offline using Playwright's built-in API:

```javascript
await context.setOffline(true);
```

This mimics a real-world situation where the user loses internet access. With the connection disabled, we reload the page. Since there's no network, we expect the Service Worker to serve cached data:

```javascript
await page.reload();
```

Finally, we assert that the same content is still visible, even without internet access:

```javascript
await expect(page.locator("#data")).toHaveText("Hello from the network!");
```

#### Failure Test: No Data on First Visit Without Network

While caching can keep the app functional offline, it's important to verify that data is not magically available if the app is visited for the first time without internet access. In this test, we simulate a failed request to data.json and expect the app to show an appropriate fallback message.

We start with a fresh browser context to ensure there's no prior cache or service worker from earlier sessions:

```javascript
const context = await browser.newContext();
const page = await context.newPage();
```

Before loading the page, we intercept the request to data.json and abort it. This simulates a network failure:

```javascript
await page.route("**/data.json", (route) => route.abort());
```

This means the app won't be able to retrieve the JSON data, and there's no chance to cache it either. We then navigate to the same app page. Because we intercepted the JSON request, the data won't load:

```javascript
await page.goto("your-page-url");
```

Since the data fetch failed, the app should display a fallback message — not the actual content from data.json. We assert that:

```javascript
await expect(page.locator("#data")).toHaveText("Failed to load data");
```

#### Conclusion

Testing how our web app behaves in offline conditions is crucial for delivering a resilient and user-friendly experience. In this post, we demonstrated how to verify two key scenarios using Playwright:

- That cached data is displayed correctly when revisiting the app offline.
- That the app shows an appropriate fallback when data is unavailable and has never been cached.

These tests ensure that our Service Worker and caching logic work as intended, helping our app gracefully handle real-world issues like network interruptions or offline usage. Even a simple static site can benefit from automated offline tests—giving our users confidence that our app will always have their back, online or not.

You can find the full source code and demo application on our GitHub repository. Until next time!

