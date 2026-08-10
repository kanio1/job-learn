Of course you know how to debug your tests. Everybody knows how to do it. But not everybody does it in the same way. Here are a few ways to debug your tests using Playwright.

## Debug using Playwright trace viewer

One of the most commonly used method is to open the trace file using Playwright's trace viewer. Take the `trace.zip` file from your reports and run `npx playwright show-trace path/to/trace.zip`.

But did you know that you can also:

- Read the trace file directly on https://trace.playwright.dev/ — drag and drop the file there.
- Open remote traces using its URL from CI published reports.
- Pass the URL of your uploaded trace file: `https://trace.playwright.dev/?trace=<url>`

## Debug using Playwright VS Code extension

Playwright has one of the best integrations with VS Code. You can view your tests and run them from the Test Explorer section.

Put breakpoints into the code, go to Test Explorer and click the debug test button.

While in debug mode you can view in real time all the steps your code will perform related to elements. Click at the line of code and the browser will highlight the element.

You can use watches during debug mode with VS Code and check values in real time from server responses.

## Debug using Playwright inspector tool

Playwright has its own debugging tool called Playwright Inspector. Use `npx playwright test path/to/testfile.spec.ts --debug` or target a single test with line number `npx playwright test path/to/testfile.spec.ts:27 --debug`.

What I like most about this inspector is that it shows what Playwright does under the hood — for a simple click, Playwright performs many actions.

## Leverage Playwright debug mode to write tests fast

Check the `show browser` option in Test Explorer. Write minimum code to open the web page and hit the green play button — the browser opens beside your editor for interactive test authoring.

## Common errors with debug mode

If Test Explorer shows "no tests found", check:

- `testDir` points correctly to where tests are
- `reporter` and other config values match accepted options
- No syntax errors or bad imports in test files

From experience, common mistakes in playwright.config that make tests disappear from Test Explorer:

- `testDir: "./path/to/tests"` does not point correctly
- `reporter: "html23typo"` invalid reporter value
- Syntax error or bad import in files

Cypress throws an error when opening its runner with misconfiguration; Playwright may silently hide tests from Test Explorer instead.
