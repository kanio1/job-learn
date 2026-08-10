# Supercharging Playwright Tests with Chrome DevTools Protocol

**Author:** Irfan Mujagic  
**Published:** 2025-05-25  
**URL:** https://www.thegreenreport.blog/articles/supercharging-playwright-tests-with-chrome-devtools-protocol/supercharging-playwright-tests-with-chrome-devtools-protocol.html

## Firecrawl Metadata

```json
{
  "author": "Irfan Mujagic",
  "og:title": "The Green Report | Supercharging Playwright Tests with Chrome DevTools Protocol",
  "og:description": "A blog dedicated to Quality Assurance in Software Engineering",
  "sourceURL": "https://www.thegreenreport.blog/articles/supercharging-playwright-tests-with-chrome-devtools-protocol/supercharging-playwright-tests-with-chrome-devtools-protocol.html",
  "statusCode": 200,
  "scrapeId": "019fb3cb-ecd5-743e-96ee-cada113810b5"
}
```

---

When using [Playwright](https://playwright.dev/) for test automation, most QA engineers interact with the browser just like a real user would — clicking buttons, filling forms, and validating UI elements. But under the hood, modern browsers offer much more control through powerful developer protocols.

In this post, we'll explore how we can take advantage of [Chrome DevTools Protocol (CDP)](https://chromedevtools.github.io/devtools-protocol/) with Playwright to unlock advanced testing features. From blocking resources to capturing browser logs, CDP gives us deep access to the browser's internals — and yes, it's all automatable!

#### What is CDP?

The Chrome DevTools Protocol is a set of low-level APIs used by the Chrome DevTools itself to inspect and control the browser. CDP allows developers and QA engineers to perform advanced operations like:

- Intercepting and modifying network traffic
- Emulating devices and network conditions
- Capturing performance metrics
- Listening to console logs or DOM events

Playwright offers a way to hook into CDP for Chromium-based browsers, allowing us to blend high-level and low-level automation seamlessly. Let's look at some use cases.

#### Blocking Images to Speed Up Tests

In many UI tests, images are not critical to test logic—they're often decorative or content placeholders. By blocking image requests during test execution, we can significantly reduce page load times and focus test performance on functional aspects.

We begin by importing the Playwright library and launching a Chromium browser instance in headless mode. We also create a new browser context and page.

```python

from playwright.sync_api import sync_playwright

def test_block_images():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

```

We attach a Chrome DevTools Protocol session to the browser context for the specific page we're testing. This gives us low-level access to the browser internals using the same API DevTools uses behind the scenes — crucial for enabling and configuring network behavior.

```python

client = context.new_cdp_session(page)

```

With CDP, we first enable the Network domain to allow manipulation of request/response behavior. Then we specify which URL patterns we want to block — in this case, common image formats.

```python

client.send("Network.enable")
client.send("Network.setBlockedURLs", {
    "urls": ["*.png", "*.jpg", "*.jpeg", "*.gif"]
})

```

With image requests now blocked, we load a sample site and verify that the page content still renders as expected.

```python

page.goto("https://example.com")

assert "Example Domain" in page.inner_text("body")

```

We're verifying that the page loads its main content correctly even when images are blocked. This is typical of real-world scenarios where CDN failures or slow networks might prevent image rendering.

#### Capturing Console Logs via CDP

Now that we already have a CDP session set up from the previous use case, we can extend our tooling to listen for browser console logs like console.warn, console.error, and console.log. This is useful for debugging failing tests or detecting runtime issues that aren't visible in the UI.

To receive log events from the browser, we first enable the Log domain via CDP:

```python

client.send("Log.enable")

```

Next, we define a callback that will be triggered every time the browser logs a new message.

```python

def handle_log_entry(params):
    level = params["entry"]["level"]
    text = params["entry"]["text"]
    print(f"Console: {level.upper()} - {text}")

```

Now we wire this function up to the CDP session:

```python

client.on("Log.entryAdded", handle_log_entry)

```

Every time the browser logs something to the console, handle_log_entry will be called with the log's details. Let's simulate a real-world scenario by navigating to a page and generating some log output:

```python

page.goto("https://example.com")
page.evaluate("console.log('Test log message')")
page.evaluate("console.warn('This is a warning log!')")
page.evaluate("console.error('This is an error log!')")

```

These logs now flow through CDP and will be captured by the event listener we registered earlier. To ensure the asynchronous events are handled before the script exits, we wait briefly:

```python

page.wait_for_timeout(1000)

```

Without this pause, the test may end before the console messages are processed.

#### Simulating Slow Network Conditions via CDP

Sometimes, we want to test how our web application behaves under real-world network limitations—like slow 3G connections, high latency, or limited upload speeds. This is where Network.emulateNetworkConditions from the CDP becomes incredibly useful.

Before we can manipulate network behavior, we must enable the Network domain in CDP. This tells the browser to start tracking and responding to network-related commands.

```python

client.send("Network.enable")

```

Then, we simulate network constraints using Network.emulateNetworkConditions:

- offline: False - We want to simulate slowness, not a disconnected network.
- latency: 200 - Adds a delay of 200ms to every request.
- downloadThroughput and uploadThroughput - Restrict the bandwidth, simulating a poor mobile or congested network.

```python

client.send("Network.emulateNetworkConditions", {
    "offline": False,
    "latency": 200,
    "downloadThroughput": 50000,
    "uploadThroughput": 20000
})

```

After applying the network throttling, we navigate to a website. Any assets requested by this page will now be subject to the artificial latency and bandwidth constraints we've set.

```python

page.goto("https://example.com")

```

Finally, we give the browser some time to demonstrate the throttled behavior.

```python

page.wait_for_timeout(5000)

```

We wait for 5 seconds just to give enough time for observation—especially useful when running in non-headless mode.

#### Conclusion

With CDP, our test automation can go beyond the surface and truly test how our web app behaves under various conditions — not just how it looks. From intercepting requests to inspecting browser logs, CDP empowers QA engineers to write more robust, performant, and realistic tests.

If you're already using Playwright, CDP is just a method call away. So next time you're writing a test, ask yourself: Is there a lower-level insight I can automate? — and let CDP help you get there.

You can find the complete code examples from this post on our [GitHub page](https://github.com/Crypted39/the-green-report-examples/tree/master/supercharging-playwright-tests-with-chrome-devtools-protocol), ready to explore and adapt to your own test scenarios. Thanks for reading, and happy testing!
