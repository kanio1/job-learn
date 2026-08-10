## Intro

When working with end-to-end testing frameworks like Playwright, handling network requests is often a complex task. Tests that rely on external APIs can be slow and inconsistent, introducing unnecessary flakiness. Network calls that succeed in one test run might fail in the next due to a slow or unreliable server, resulting in inconsistent results. To address this, developers often resort to mocking network requests, which introduces another challenge: managing mocks.

Wouldn't it be great to have an automated way to handle caching and reusing network responses without setting up complex mocking strategies? I've investigated existing approaches and developed a tool that I want to introduce. It solves these exact problems by caching network requests on the filesystem, enabling faster and more reliable Playwright tests.

## The Problem with Network Requests in Tests

Network requests are often the slowest part of test execution. When running multiple test suites, the repeated querying of external APIs can dramatically increase test durations. Additionally, real-world APIs can be unstable, with occasional timeouts, making your tests fragile and unreliable.

A common approach to mitigating this is to mock API responses. While useful, mocking requires manual intervention — you need to carefully construct mock responses, keep them updated, and ensure that every potential network scenario is handled. This can become a huge maintenance burden as the API evolves or your test cases change. Playwright supports HAR files for capturing and replaying network traffic, but working with HAR can be tedious and lacks flexibility for modifying responses on the fly.

## Enter `playwright-network-cache`

playwright-network-cache is designed to streamline the process of caching network responses in Playwright tests, eliminating the need for manual mocks or rigid HAR files. With this library, network responses are automatically stored on the filesystem during the first test run and can be reused in subsequent runs, significantly speeding up test execution. Moreover, the responses are saved in a clear, organized folder structure, making it easy to inspect and modify them as needed.

### How It Solves the Problem

1. **Automatic Caching**: The library automatically caches network responses when tests are run for the first time. This means that your tests won't have to wait for external APIs to respond in future runs — the cached data will be used instead, resulting in faster and more reliable tests.

2. **Customizable Cache Duration (TTL)**: You can control how long the cached file is retained by setting a time-to-live (TTL) option. After the specified time elapses, the library will hit real API again and refresh the cache, keeping your test data up to date.

3. **Dynamic Modifications**: Need to tweak a response for a specific test case? playwright-network-cache allows you to modify cached responses dynamically. Whether you want to change the status code, headers, or response body, the library provides options to adjust the cached data on-the-fly without manually maintaining separate mocks.

4. **Flexible Structure**: The caching system organizes files based on hostname, request method, and URL path, ensuring that you can easily navigate through and manage the cached data.

5. **Speed Boost**: By reusing cached responses, your tests no longer need to wait for network calls to complete, making them dramatically faster.

6. **No More Mock Hell**: Forget about manually maintaining mocks. The library handles everything for you — from caching to replaying and even modifying responses.

7. **No HAR Complexity**: playwright-network-cache provides a cleaner, more flexible alternative to HAR by letting you manage individual responses as simple JSON files.

### Example

Imagine you're testing an application that fetches a list of cats from an API. Without caching, each test run would require a live request to the API, adding latency and potential failure points to your tests.

With playwright-network-cache, you can easily cache the API response:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats');
  // Perform usual test actions...
});
```

On the first run, the response is cached in the `.network-cache` directory:

```
.network-cache
└── example.com
    └── api-cats
        └── GET
            ├── headers.json
            └── body.json
```

On subsequent runs, the cached response is reused, making the test faster and eliminating the need to hit the actual API.

To revalidate the cache, you can provide `ttlMinutes` option:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats', {
   ttlMinutes: 60 // hit real API once in a hour
  });
});
```

You can modify cached response for the particular test needs:

```
test('test', async ({ page, cacheRoute }) => {
  await cacheRoute.GET('https://example.com/api/cats', {
    modify: async (route, response) => {
      const json = await response.json();
      json[0].name = 'Kitty-1';
      await route.fulfill({ json });
    }
  });
});
```

To get `cacheRoute` variable available in your tests, instantiate it like any other Playwright fixture:

```
// fixtures.js
import { test as base } from '@playwright/test';
import { CacheRoute } from 'playwright-network-cache';

export const test = base.extend({
  cacheRoute: async ({ page }, use) => {
    const cacheRoute = new CacheRoute(page, { /* cache options */ });
    await use(cacheRoute);
  },
});
```

### More Than Just Caching

playwright-network-cache isn't just about caching. It offers advanced features like:

- **Modifying Responses**: Adjust the data in the cached responses dynamically using custom functions.
- **Handling Status Codes**: Cache responses based on specific HTTP status codes, including errors.
- **Flexible Directory Structure**: Customize how and where cache files are stored.
- **Disable or Update Cache**: Temporarily disable caching for specific tests or force updates to the cache when needed.

## Recap

If you're looking to make your Playwright tests faster and more reliable, give a try to playwright-network-cache. By caching network responses on the filesystem and allowing for dynamic modifications, it eliminates the need for manual mocks and provides a flexible, easy-to-use alternative to HAR files.

Thanks for reading ❤️

