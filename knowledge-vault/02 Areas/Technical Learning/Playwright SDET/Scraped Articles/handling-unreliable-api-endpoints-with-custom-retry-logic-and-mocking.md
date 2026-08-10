# Handling Unreliable API Endpoints with Custom Retry Logic and Mocking

Dealing with unreliable API endpoints can be a significant challenge. In this blog post, we'll explore how to implement custom retry logic and mock responses using Playwright, ensuring our tests remain robust and reliable even when APIs fail.

## The Problem: Verifying API Data

Verifying data fetched from APIs can be a daunting task, especially when dealing with unreliable or slow endpoints:

- **Inconsistent Responses:** APIs can sometimes return incomplete or incorrect data.
- **Timeouts and Failures:** Network instability can cause API requests to fail.
- **Data Variability:** API data can change over time.
- **Dependency on External Services:** Tests depend on third-party uptime.

## Importance of Retries and Mocking

- **Retries:** Implementing retry logic allows tests to make multiple attempts to fetch data.
- **Mocking:** When all retry attempts fail, mocking allows us to simulate API responses.

## Use Case: University Data

We aim to fetch data about universities from an API and display it on a web page. Expected data includes University Name and Source.

## Implementing the Test Script

```javascript
const { test, expect } = require("@playwright/test");

test("Verify university data with retries and mock", async ({ page }) => {
    const url = "http://universities.hipolabs.com/search?name=middle&country=turkey";
    const mockData = { name: "Mocked University", source: "http://mockeduniversity.com" };

    let retries = 3;
    let fetchedData = null;
    let attempt = 0;

    for (let i = 0; i < retries; i++) {
        try {
            attempt++;
            const response = await page.evaluate(() =>
                fetch("http://universities.hipolabs.com/search?name=middle&country=turkey")
                    .then((res) => res.json())
            );
            fetchedData = { name: response[0].name, source: response[0].web_pages[0] };
            break;
        } catch (e) {
            console.error(`Attempt ${i + 1} failed: ${e.message}`);
        }
    }

    if (!fetchedData) {
        fetchedData = mockData;
        await page.route(url, (route) =>
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify([{ name: fetchedData.name, web_pages: fetchedData.source }]),
            })
        );
    }

    await page.goto("http://127.0.0.1:5500/resources/index.html");
    await page.waitForSelector("#name");
    await page.waitForSelector("#source");

    const universityName = await page.textContent("#name");
    const universitySource = await page.textContent("#source");

    expect(universityName).toBe(fetchedData.name);
    expect(universitySource).toContain(fetchedData.source);
});
```

## Conclusion

We explored the importance of handling unreliable API endpoints by implementing custom retry logic and mocking data responses. This approach ensures that our tests can still run and verify expected outcomes even when the API is unreliable.

Happy testing!

