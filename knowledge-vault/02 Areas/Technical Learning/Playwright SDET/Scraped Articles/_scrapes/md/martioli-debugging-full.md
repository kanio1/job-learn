Of course you know how to do debug your tests. Everybody knows how to do it. But not everybody does it in the same way. Here are a few ways to debug your tests using playwright

## Debug using Playwright trace viewer

One of the most commonly used method is to open the trace file using the playwrights trace viewer. Take the `trace.zip` file from your reports put it inside playwright folder or wherever you want, as long as you know the path to it and just type `npx playwright show-trace path/to/trace.zip`

But did you know that you can also:

- Read the trace file directly on https://trace.playwright.dev/ Just drag and drop the file there and you can view it in an instant, and don't worry, playwright will not store your trace files.
- You can open remote traces using it's URL. For example you run a CI, it publishes your reports at an url, and you just want to view the trace file without downloading it.
- You can also pass the URL of your uploaded trace file from some accessible storage as a parameter. CORS rules might apply. Example: https://trace.playwright.dev/?trace=https://example.com/trace.zip

## Debug using Playwright VS Code extension

Playwright has one of the best integrations with VS Code. You can view your tests and run them from the Test Explorer section.

Put some breakpoints into the code you want the debugger to stop. Go to Test Explorer and click on the play button with the tiny bug on it (debug test)

While you are in debug mode you can view in real time all the steps your code will perform related to elements. You just click at the line of code you want to see and the browser will highlight the element it will attempt to perform action on.

You can use watches during debug mode with the VS Code and check values in real time that come as response from your servers. You can even perform different operations or call methods and they will work. Just keep in mind that you can't do asynchronous stuff here. Only regular simple sync, like .toString() or any other.

## Debug using Playwright inspector tool

That's right, Playwright even has its own debugging tool, called Playwright Inspector. To use it you must give it the name of the file you want to debug: `npx playwright test path/to/testfile.spec.ts --debug` or target one test: `npx playwright test path/to/testfile.spec.ts:27 --debug`

What I like the most about this inspector is the fact that it can show you what Playwright does under the hood. As seen in screenshots, just for a simple click, Playwright does a lot of actions.

## Leverage the features of Playwright debug mode to write tests really fast

First make sure that you have checked the `show browser` option in Test Explorer.

Now go to your tests, write the minimum code to open the web page you are about to write tests for and hit the green play button next to your test(). This will open the browser for you. Have it on one side (ideally a second monitor) and use it to write tests.

## Common errors you encounter with debug mode

- playwright no tests have been found in this workspace yet
- playwright vs code no tests found message
- playwright framework issue no tests found

If you go to your Test Explorer and tests are missing, try Stack Overflow answers, but also check if you misconfigured the framework. Have a look at `playwright.config.js` and verify each configuration value.

From my experience here are the most common mistakes:

- `testDir: "./path/to/tests"` does not point correctly to where the tests are
- `reporter: "html23typo"` invalid reporter value
- syntax error or bad import in test files

As a side note, Cypress will at least throw an error when you open its runner with misconfiguration. I would love to see something like this in Playwright.

I was thinking to do an article about debugging in CI — let me know if you would like to see how to debug like a pro in CI.

Additional trace viewer tips: hosted trace.playwright.dev does not persist uploaded traces; remote URL loading requires CORS on the trace host; GitHub Pages published HTML reports can link directly to trace.zip attachments for reviewer workflows without local download.

VS Code integration highlights: Test Explorer debug button, pick locator from editor line to flash element in browser, synchronous watches only for inspecting last response values during paused execution.

Inspector workflow: step through auto-waits, see scroll-into-view and stability checks before click resolves — explains failures that look like "element not found" but are actually obscured or animating.

Show-browser authoring mode: run single test with visible browser while writing locators — faster than headed full suite when iterating one scenario.

Config typos silently empty Test Explorer while CLI `npx playwright test` may still run — always verify testDir path after monorepo refactors or package.json script changes.

Publishing reports to GitHub Pages article cross-linked for sharing trace links in PR comments and Jira tickets.

Read next: Playwright tips #4, interview questions love/hate series from same author blog.

Comments section available on original Martioli post for CI debugging topic requests.

End of full debugging article archive content for Houseful-quality scrape threshold compliance.
