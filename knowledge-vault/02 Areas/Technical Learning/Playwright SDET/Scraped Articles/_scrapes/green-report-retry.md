### Handling Unreliable API Endpoints with Custom Retry Logic and Mocking

Jun 23th 2024 — 10 min read

Dealing with unreliable API endpoints can be a significant challenge. In this blog post, we'll explore how to implement custom retry logic and mock responses using Playwright, ensuring our tests remain robust and reliable even when APIs fail.

#### The Problem: Verifying API Data

Verifying data fetched from APIs can be a daunting task, especially when dealing with unreliable or slow endpoints. Common challenges:

- **Inconsistent Responses:** APIs can sometimes return incomplete or incorrect data due to server-side issues or network problems.
- **Timeouts and Failures:** Network instability or server overloads can cause API requests to fail or time out.
- **Data Variability:** API data can change over time, making it difficult to create consistent test cases.
- **Dependency on External Services:** Relying on third-party APIs means your tests are dependent on their uptime and reliability.

##### Importance of Retries and Mocking

- **Retries:** Implementing retry logic allows our tests to make multiple attempts to fetch data from the API.
- **Mocking:** When all retry attempts fail, mocking allows us to simulate API responses.

##### Use Case: University Data

We aim to fetch data about universities from an API and display it on a web page. Expected data includes university name and source website.

Given the potential unreliability of the API, we implement a test script that attempts to fetch the data three times. If all attempts fail, we use mocked data.

#### Implementing the Test Script

```javascript
const { test, expect } = require("@playwright/test");

test("Verify university data with retries and mock", async ({ page }) => {
    const url = "http://universities.hipolabs.com/search?name=middle&country=turkey";

    const mockData = {
        name: "Mocked University",
        source: "http://mockeduniversity.com",
    };

    let retries = 3;
    let fetchedData = null;
    let attempt = 0;

    for (let i = 0; i < retries; i++) {
        try {
            attempt++;
            const response = await page.evaluate(() =>
                fetch(
                "http://universities.hipolabs.com/search?name=middle&country=turkey"
                ).then((res) => res.json())
            );
            fetchedData = {
                name: response[0].name,
                source: response[0].web_pages[0],
            };
            console.log(`Attempt ${i + 1}: Successfully fetched data`);
            break;
        } catch (e) {
            console.error(`Attempt ${i + 1} failed: ${e.message}`);
        }
    }

    if (!fetchedData) {
        console.log("All attempts failed. Using mock data.");
        fetchedData = mockData;

        await page.route(url, (route) =>
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify([
                    { name: fetchedData.name, web_pages: fetchedData.source },
                ]),
            })
        );
        console.log("Verification done with mocked data");
    } else {
        console.log("Verification done with real data");
    }

    await page.goto("http://127.0.0.1:5500/resources/index.html");

    await page.waitForSelector("#name");
    await page.waitForSelector("#source");

    const universityName = await page.textContent("#name");
    const universitySource = await page.textContent("#source");

    console.log(`Total attempts: ${attempt}`);

    expect(universityName).toBe(fetchedData.name);
    expect(universitySource).toContain(fetchedData.source);
});
```

#### Conclusion

We explored handling unreliable API endpoints by implementing custom retry logic and mocking data responses. The script is available on the [GitHub page](https://github.com/Crypted39/the-green-report-examples/tree/master/handling-unreliable-api-endpoints-with-custom-retry-logic-and-mocking).
