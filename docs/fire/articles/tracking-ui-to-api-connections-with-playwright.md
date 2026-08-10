# Tracking UI to API Connections with Playwright

| Field | Value |
|-------|-------|
| **Author** | Irfan Mujagic |
| **Published** | Aug 3, 2025 |
| **URL** | https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/tracking-ui-to-api-connections-with-playwright.html |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "The Green Report | Tracking UI to API Connections with Playwright",
  "og:title": "The Green Report | Tracking UI to API Connections with Playwright",
  "author": "Irfan Mujagic",
  "publishedTime": null,
  "og:url": "https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/tracking-ui-to-api-connections-with-playwright.html",
  "description": "A blog dedicated to Quality Assurance in Software Engineering",
  "statusCode": 200,
  "sourceURL": "https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/tracking-ui-to-api-connections-with-playwright.html"
}
```

---

### Tracking UI to API Connections with Playwright

Aug 3rd 20255 min read

![](https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/images/banner.webp)

easy

javascriptES6

playwright1.54.2

ui

api

When testing web applications, it's common to validate that buttons or interactive elements respond
to user actions. But what if there's no visible confirmation on the UI? For example, in a star
rating system, clicking a star might silently trigger a background API call without any immediate
feedback. Without a clear cue, it becomes difficult to confirm whether the correct data was actually
sent. In this post, we'll use a simple Express-based web app as a demonstration and show how
Playwright's waitForRequest can be used to capture and verify
the API request. This approach helps
ensure that important background actions are correctly triggered, even when the UI doesn't provide
direct confirmation.


#### The Scenario: Rating Widget

A common UI pattern in modern web applications is the 5-star rating component. Users can click on
one of the stars to rate content, products, or services. While the interaction feels simple, what
happens behind the scenes is much more important. Clicking a star usually triggers a POST request to
an API with the selected rating value.


![Rating Widget Demo](https://www.thegreenreport.blog/articles/tracking-ui-to-api-connections-with-playwright/images/demo_app.gif)

Rating Widget Demo

From a testing perspective, the challenge is that this interaction often does not produce any
visible confirmation on the screen. There may be no success message, no animation, and no clear
indication that the action worked. This makes it difficult to confirm that the right data was sent
without inspecting the network activity.


Here is an example of what the API request might look like when the user clicks on the fourth star:


```bash

POST /rating
Content-Type: application/json

{
  "rating": 4
}

```

The same behavior applies to the other stars. Clicking the second star would send { "rating": 2 },
the fifth would send { "rating": 5 }, and so on. Because the
value changes based on user input, it
is important to capture and verify the outgoing request to make sure the application is behaving
correctly.


#### Using waitForRequest in Playwright

Playwright provides a powerful method called waitForRequest that
allows us to wait for a specific
network request to occur during a test. This is especially useful when testing features that involve
API calls triggered by user actions, such as clicking a star in a rating widget. Instead of blindly
assuming the request was sent, we can intercept it and inspect its details directly.


The waitForRequest method works by listening for requests that
match a specific condition. We can
filter by URL, HTTP method, or any other property of the request. When the condition is met,
Playwright captures the request and allows us to access its contents, including the payload.


Here is a simple example of how to use waitForRequest in a test.
This test clicks on the second star
and checks that a POST request to /rating is sent with the
correct data:


```javascript

test('sends correct rating payload when a star is clicked', async ({ page }) => {
  await page.goto('http://localhost:3000');

  const [request] = await Promise.all([
    page.waitForRequest(request =>
      request.url().includes('/rating') && request.method() === 'POST'
    ),
    page.click('[data-rating="2"]')
  ]);

  const payload = JSON.parse(request.postData());
  expect(payload).toEqual({ rating: 2 });
});

```

In this example, the test waits for a POST request to the /rating endpoint while simulating a click
on the star with a data-rating attribute set to 2. Once the
request is captured, the test parses the
payload and verifies that the correct rating value was sent.


This approach gives us confidence that our front-end logic is correctly communicating with the
backend and that dynamic user input is handled as expected.


#### Benefits of This Approach

Using waitForRequest in our Playwright tests provides several
valuable benefits, especially when
testing features that rely on background API calls:


- **Confirms the right API is called:** By intercepting and inspecting network requests, we
can ensure that the correct endpoint is triggered in response to specific user actions. This is
useful when multiple components use similar requests, or when the UI does not clearly show the
outcome of an action.

- **Validates dynamic payloads:** Many UI elements generate request payloads based on user
input. With waitForRequest, we can directly access and
validate the contents of each request.
This helps confirm that dynamic values, such as a selected rating, are sent correctly to the
backend.

- **Strengthens end-to-end test coverage:** This method allows us to go beyond checking that a
button was clicked or an element changed. We can now verify that the entire flow—from UI
interaction to network request—is working as expected. This leads to more reliable and complete
test coverage.


#### Conclusion

Verifying that a user interaction triggers the correct backend request is an important part of
end-to-end testing, especially when there is no visible feedback in the UI. Playwright's
waitForRequest makes it easy to capture and inspect network
activity, helping us confirm that our
application behaves as expected. Whether we are testing a simple rating component or a more complex
feature, this method adds an extra layer of confidence to our test suite.


The demo app and the Playwright test shown in this post are available on our [GitHub page](https://github.com/Crypted39/the-green-report-examples/tree/master/tracking-ui-to-api-connections-with-playwright) for you to
explore and try out.
