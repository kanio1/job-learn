# API Mocking using Playwright

### What is API Mocking?

**API mocking** is a technique to simulate the behavior of an API or service without calling actually API. This is extremely beneficial in various ways such as Isolation, Speed, Control, Reliability.

- **Isolation:** It allows you to test your code in isolation, ensuring that any issues you encounter are related to your code and not the external API.
- **Speed:** Mocking can significantly speed up your development and testing process, as you don't have to wait for slow or unreliable network connections.
- **Control:** You can control the responses from the mocked API, allowing you to test different scenarios and edge cases.
- **Reliability:** Mocking can help you ensure that your code is resilient to changes in the external API.

Web APIs are usually implemented as HTTP endpoints. Playwright provides APIs to mock and modify network traffic, both HTTP and HTTPS. Any requests that a page does, including XHRs and fetch requests, can be tracked, modified and mocked. With Playwright you can also mock using HAR files that contain multiple network requests made by the page.

### API MOCKING vs API TESTING

**There are 3 ways for API mocking in playwright**

1. **Mock API Requests**
2. **Mock API Responses**
3. **HAR file:** Recording HAR file, Modifying HAR file, Replying from HAR file.

### 1. Mock API Requests

The following code will intercept all the calls to `*/**/api/v1/fruits` and will return a custom response instead. No requests to the API will be made.

```javascript
await page.route('*/**/api/v1/fruits', async route => {
    const json = [
        { name: 'playwright by testers talk', id: 21 },
        { name: 'cypress by testers talk', id: 71 },
        { name: 'api testing by testers talk', id: 72 },
        { name: 'postman by testers talk', id: 73 },
        { name: 'rest assured by testers talk', id: 74 },
    ];
    await route.fulfill({ json });
});
await page.goto('https://demo.playwright.dev/api-mocking');
await expect(page.getByText('playwright by testers talk')).toBeVisible();
```

### 2. Mock API Responses

It is essential to make an API request, but the response needs to be patched to allow for reproducible testing.

```javascript
await page.route('*/**/api/v1/fruits', async route => {
    const response = await route.fetch();
    const json = await response.json();
    json.push({ name: 'playwright by testers talk', id: 100 });
    await route.fulfill({ response, json });
});
```

### 3. HAR File

To record a HAR file we use `page.routeFromHAR()` or `browserContext.routeFromHAR()` method.

```javascript
await page.routeFromHAR('./hars/fruit.har', {
    url: '*/**/api/v1/fruits',
    update: true,
});
await page.goto('https://demo.playwright.dev/api-mocking');
await expect(page.getByText('Strawberry')).toBeVisible();
```

Set `update: false` after recording, then modify the .bin file JSON to inject custom test data.

### Complete Test Example

```javascript
const { test, expect } = require('@playwright/test');

test("API Mocking Using Playwright", async ({ page }) => {
    await page.route('*/**/api/v1/fruits', async route => {
        const json = [
            { name: 'playwright by testers talk', id: 21 },
            { name: 'cypress by testers talk', id: 71 },
        ];
        await route.fulfill({ json });
    });
    await page.goto('https://demo.playwright.dev/api-mocking');
    await expect(page.getByText('playwright by testers talk')).toBeVisible();
});
```

Run with `npx playwright test`. For HAR recording, observe that hars folder is created with .har and .bin files. Modify the .bin JSON to inject custom test data, then set `update: false` for replay.

Reference: https://playwright.dev/docs
