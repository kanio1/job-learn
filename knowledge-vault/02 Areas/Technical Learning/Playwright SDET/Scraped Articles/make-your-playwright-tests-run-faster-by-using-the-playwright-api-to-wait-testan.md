There are times when automating a test in Playwright that the test needs to wait because the test will flake if it does not wait for something such as an event. It can be, for example, that you are waiting for a navigation to complete.

Tests can be made to wait with a 'wait' for a given period of time, say five seconds. If this is done the tests will always wait for that period of time, whether or not it is necessary and the tests will be slower than they need to be.

It is better to have tests wait for something specific such as a locator to be rendered, an event or an API call because the test waits for the locator or event. If a test uses this type of wait, it only waits as long as necessary for the specified condition to be met. Also, the tests should not flake and run more quickly.

I have found the Typescript functions in the Playwright API described below to be useful ways of waiting when writing Playwright tests:

### Waiting for a locator – waitFor()

The function waitFor() can be chained to a locator to wait for that locator to be rendered, for example:

I have found waitFor() useful when waiting for navigation to complete because the test will wait for the specified locator.

Arguments for the state of the locator being waited for and for the length of the timeout can be passed to waitFor() for the state that is being waited for: https://playwright.dev/docs/api/class-locator#locator-wait-for

### Waiting for an API call – waitForResponse()

A test may need to wait for the response from an API call. This can be done with waitForResponse(). In this example, waitForReponse is waiting for a response after click().

The API call that the test needs to wait for can be found in the network tab of Dev Tools and is then passed as a parameter to waitForResponse(). The function waitforReponse() returns the matched response.

Arguments can be passed to WaitforResponse() for the predicate and the timeout: https://playwright.dev/docs/api/class-page#page-wait-for-response

### Waiting for an event – waitForEvent()

Sometimes the test is not waiting for an API call or the presence of a locator, it is waiting for an event, such as 'requestfinished' or 'domcontentloaded'. WaitForEvent waits for the event to fire and returns a truthy value. In the example below the test is waiting for a 'domcontentloaded' event that occurs after navigate().

Arguments can be passed to waitForEvent() for the event, predicate and timeout: https://playwright.dev/docs/api/class-page#page-wait-for-event

### Waiting for a truthy value – waitForFunction()

If a test needs to wait for a truthy value, such as the value returned by querySelectorAll() you can use waitForFunction();

The function will wait up to the length of time specified in the timeout for truthy to be returned. This example returns truthy when length is greater than zero and falsey when length is zero.

Arguments can be passed to waitForFunction() for the function, evaluation argument, polling and timeout: https://playwright.dev/docs/api/class-page#page-wait-for-function

### Conclusion

This post describes the functions that I am using to make tests wait. The Playwright API contains more functions that can be used for waiting than I have shown above. If you find that the functions in this post do not meet your needs it is worth exploring the Playwright API to find different ways of making tests wait.

It is easy to get in the habit of using waits in our test that wait for a given number of seconds. However, waiting for a given number of seconds is a habit that will slow your tests down.

If you use functions like those above to make your tests wait, the tests will only wait as long as necessary for the condition to be met and so run faster.

