---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Butch Mayhew / Playwright Solutions | TOOL: Playwright-Cli-Select For Quick Targeted Test Runs via CLI | https://playwrightsolutions.com/tool-playwright-cli-select-for-quick-targeted-test-runs-via-cli/ | complete |
| 2 | Butch Mayhew / Playwright Solutions | Playwright Login Test With Two Factor Authentication (2FA) Enabled (TOTP) | https://playwrightsolutions.com/playwright-login-test-with-2-factor-authentication-2fa-enabled/ (canonical; indexed URL 404) | complete |
| 3 | Butch Mayhew / Playwright Solutions | Playwright Solutions Challenge: Debug and figure out why the video recording isn't in the HTML report? | https://playwrightsolutions.com/playwright-solutions-challenge-debug-and-figure-out-why-the-video-recording-isnt-in-the-html-report/ | complete |
| 4 | Butch Mayhew / Playwright Solutions | [Update v1.46] Is it possible to run only Playwright Tests that changed in GitHub actions on a pull request? | https://playwrightsolutions.com/update-v1-46-is-it-possible-to-run-only-playwright-tests-that-changed-in-github-actions-on-a-pull-request/ | complete |
| 5 | Butch Mayhew / Playwright Solutions | How do you scroll to the bottom of an infinite scrolling page in a Playwright test? | https://playwrightsolutions.com/how-do-you-scroll-to-the-bottom-of-an-infinite-scrolling-page-in-a-playwright-test/ | complete |
| 6 | Sergei Gapanovich / Playwright Solutions | How to use Playwright's testInfo.retry to deal with flakey environments | https://playwrightsolutions.com/how-to-use-playwrights-testinforetry-to-deal-with-flakey-environments/ | complete |
| 7 | ScrollTest / Pramod Dutta | Playwright Custom Matchers with expect.extend | https://scrolltest.com/playwright-custom-matchers-expect-extend/ | complete |
| 8 | ScrollTest / Pramod Dutta | Playwright UI Mode: Time Travel Debugging (TypeScript Day 57) | https://scrolltest.com/playwright-ui-mode-typescript-day-57/ | complete |
| 9 | ScrollTest / Pramod Dutta | Playwright Authentication using Storage State — Skip Login | https://scrolltest.com/playwright-authentication-storage-state-skip-login/ | complete |
| 10 | ScrollTest / Pramod Dutta | Playwright Infinite Scroll and Lazy Loading | https://scrolltest.com/playwright-infinite-scroll-lazy-loading/ | complete |
| 11 | ScrollTest / Pramod Dutta | Playwright TypeScript Framework — Day 21 (Capstone) | https://scrolltest.com/playwright-typescript-framework-day-21/ | complete |
| 12 | ScrollTest / Pramod Dutta | Playwright Test Data Management — Day 19 | https://scrolltest.com/playwright-test-data-management-day-19/ | complete |
| 13 | Anton Gulin (anton.qa) | Reuse one Page Object method for success and failure cases | https://www.anton.qa/blog/posts/reuse-page-object-method-success-and-failure | complete |
| 14 | Anton Gulin (anton.qa) | Playwright v1.60 turns test failures into evidence | https://www.anton.qa/blog/posts/playwright-v1-60-evidence-first-testing | complete |
| 15 | Anton Gulin (anton.qa) | How to test passkey login in Playwright | https://www.anton.qa/blog/posts/test-passkey-login-playwright | complete |
| 16 | Sajith Dilshan (Medium) | Fixture vs lazy object creation in Playwright | https://medium.com/@sajith-dilshan/fixture-vs-lazy-object-creation-in-playwright-avoiding-hidden-performance-traps-b147673ef900 | complete |
| 17 | Sajith Dilshan (Medium) | Playwright annotations: a practical guide for QA engineers | https://medium.com/@sajith-dilshan/playwright-annotations-a-practical-guide-for-qa-engineers-f1c723fc47f7 | complete |
| 18 | TestDino | Fixing Playwright tests with AI | https://testdino.com/blog/fixing-playwright-tests-with-ai | complete |
| 19 | Yevhen Laichenkov (GitHub) | playwright-expect | https://github.com/elaichenkov/playwright-expect | complete |
| 20 | Viktor Konovalov (LinkedIn) | Playwright + TypeScript QA post | https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7489942200819015680-t7Pt | unavailable |
| 21 | Stefan Minchev (LinkedIn) | QA / Playwright post | https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7482814931646722048-rPM0 | unavailable |
| 22 | Playwright Team | Best Practices | https://playwright.dev/docs/best-practices | complete |
| 23 | Playwright Team | Fixtures | https://playwright.dev/docs/test-fixtures | complete |
| 24 | Playwright Team | Assertions | https://playwright.dev/docs/test-assertions | complete |
| 25 | Playwright Team | Configuration | https://playwright.dev/docs/test-configuration | complete |

## Artykuły

### 1. Butch Mayhew / Playwright Solutions — TOOL: Playwright-Cli-Select For Quick Targeted Test Runs via CLI

- Source: https://playwrightsolutions.com/tool-playwright-cli-select-for-quick-targeted-test-runs-via-cli/
- Retrieved: 2026-08-29
- Firecrawl status: complete

Have you ever struggled to run specific tests from the command line? I've had the want to run 2-3 tests from the command line together and struggled to autocomplete my way through building the proper command manually.

Good News Everybody

![](https://playwrightsolutions.com/content/images/2025/02/image-9.png)

With this new tool I don't have to craft the perfect command line anymore I now have access to a nice command line selector to pick and choose exactly what I want to run! See the tool repository: [dennisbergevin/playwright-cli-select](https://github.com/dennisbergevin/playwright-cli-select).

## Set it all up

To get the cli tool installed it's quite simple! This command installs the dependency and updates your `package.json` file.

```javascript

```

Then you can run the cli tool with

```javascript

```

## My Experience

What I really like about this tool is the flexibility offered! The [readme](https://github.com/dennisbergevin/playwright-cli-select) provides a ton of easy to follow examples on how you can customize your run command. All of this can easily be added to your package.json script in order to have a simple `npm run cli` command, below I've created so the command automatically selects the first `specs` option for me.

```javascript

```

Once you've rung the command and you are in the cli interface, you will need to use `Tab` as the "select" key. This was a small learning curve as by default I was just trying to press `Enter` and that actually allows you to "Proceed" Thankfully I read the interface and there were clear instructions on what the buttons do!

If you ever do get stuck or want to explore the tool without reading the docs on GitHub you also have access to `npx playwright-cli-select run --help` a help flag which providers all the different options at your fingertips!

Overall I am a fan, and with very little effort on my end I'll have an easy way to run multiple spec files together via this tool. I plan to add this to all of my past, present, and future playwright projects! Check it out in action below.

(Embedded video demo omitted by scrape extraction.)

If you find this tool valuable go give it a ⭐️ on [GitHub](https://github.com/dennisbergevin/playwright-cli-select) and let [Dennis Bergevin](https://www.linkedin.com/in/dennis-bergevin/) know how it helped!

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 2. Butch Mayhew / Playwright Solutions — Playwright Login Test With Two Factor Authentication (2FA) Enabled (TOTP)

- Source: https://playwrightsolutions.com/playwright-login-test-with-2-factor-authentication-2fa-enabled/
- Retrieved: 2026-08-29
- Firecrawl status: complete
- Note: the URL indexed in the source document (`https://playwrightsolutions.com/playwright-login-test-with-two-factor-authentication-2fa-enabled/`) returns HTTP 404. The canonical URL above is the renamed, live article.

This week I'll walk through a Playwright Test with 2FA enabled. 2FA or Two Factor Authentication is a common recommended security measure to protect users accounts across the internet. Having a way to test this functionality in your CI/CD pipelines within your Playwright tests can ensure that this functionality doesn't get broken as new features and bug fixes are made to the codebase.

## Example of 2FA with TOTP

A common way to implement 2FA is by using TOTP, this stands for Time-based One-time Password. Typically the TOTP service provides you with a QR code or a Key that you input into an Authenticator App, after you initially create your user account. There is an example of a file below.

![](https://playwrightsolutions.com/content/images/2024/12/image.png)

The key above `A7VDILVI5ZTJPPGR`, is used to generate the time based password. If you want to view the current 6 digit code that is generated at this current moment you can use a tool like - [https://it-tools.tech/otp-generator](https://it-tools.tech/otp-generator), paste in the OTP key and see the code that is generated every 30 seconds. You can think of this step the same as scanning the barcode into your Authenticator App, the key is what gets saved to your device and is used to generate the random rolling 6 digit password.

## Install otpauth dependency

In the same way the above web app can take the key and generate the 6 digit TOTP code, we will be using the Node package [otpauth](https://www.npmjs.com/package/otpauth), within our Playwright tests. Let's first install this into our project.

```javascript

```

## Build logic to generate the one time password

Once installed we'll go ahead and create a new helper file in the lib folder named `otp.ts`.

```javascript

```

This code will generate the One Time Password that will be used to validate and login with the key that was provided above. All we have to do from a test now is call `generateOTP(secretKey)` while passing in the secretKey we created from the last step.

One thing to note on this code, if the digits, algorithm, or period are different for your system under test you will need to adjust these.

## Implement within a Playwright test

For our example we will be using [https://practicesoftwaretesting.com](https://practicesoftwaretesting.com) website. This is a tool built by [Roy de Kleijn](https://www.linkedin.com/in/roydekleijn/), which can be used as a test site for exploring or writing test automation.

All the code I'm showing off can be found in this [pull request](https://github.com/playwrightsolutions/playwright-practicesoftwaretesting.com/pull/18) from the repo linked below.

(Embedded GitHub repo card: [playwrightsolutions/playwright-practicesoftwaretesting.com](https://github.com/playwrightsolutions/playwright-practicesoftwaretesting.com).)

If you reviewed the pull request closely you'll notice I added a few helpers and data factories in order to setup test data, as the practicesoftwaretesting.com site database resets multiple times a day. For our purposes I'm not going to focus on test data setup/creation/management but rather how to implement 2fa into your tests.

## Creating the data

First off you should [register](https://practicesoftwaretesting.com/auth/register) a new account. Once registered and logged back in, you can visit the [profile](https://practicesoftwaretesting.com/account/profile) page and you will see the section above with a QR code and a key. You can scan this into an authenticator app or use [this tool I linked earlier](https://it-tools.tech/otp-generator) to generate the 6 digit 1 time password from the key. Once you have a 6 digit code, enter and verify the TOTP code.

![](https://playwrightsolutions.com/content/images/2024/12/image-1.png)

### Update .env file

Next we'll update the .env file with the username, password, and one time password key.

```javascript

```

### 2FA login spec

At this point the spec is pretty straight forward. notice we set the `otpKey` from the environment variable along with the email and passwords. We use these to login on the main page. The next section includes the `generateOTP()` method where we pass in the `otpKey`, to generate the 6 digit code we will use to login.

```javascript

```

The full end to end flow looks like this.

(Embedded video demo omitted by scrape extraction.)

## Final Thoughts

The example we had was quite straight forward, you may run into a scenario where you may not have the key but you do have a QR code to use. [This guide](https://cavalloj.medium.com/totp-secret-extraction-from-qr-codes-ee097b4c687f) will walk you through how to extract the key from a QR code utilizing a mobile device.

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 3. Butch Mayhew / Playwright Solutions — Playwright Solutions Challenge: Debug and figure out why the video recording isn't in the HTML report?

- Source: https://playwrightsolutions.com/playwright-solutions-challenge-debug-and-figure-out-why-the-video-recording-isnt-in-the-html-report/
- Retrieved: 2026-08-29
- Firecrawl status: complete

A few weeks back I was approached with a playwright challenge. The challenged seemed like an easy one to solve but I was surprised with the solution, so much so I wanted to share it here and give you a chance to exercise your debug skills.

## The Challenge

🎯 The Challenge: Populate the video recording in the HTML Report

An example repo can be found below. Clone/download the repo down with `git clone https://github.com/playwrightsolutions/playwright-solutions-challenges.git`

Once pulled down run `npm install` in the main directory, and if you don't have playwright browsers installed you may need to run `npx playwright install`.

(Embedded GitHub repo card: [playwrightsolutions/playwright-solutions-challenges](https://github.com/playwrightsolutions/playwright-solutions-challenges).)

Once you have everything installed run

`npx playwright test`

This will run 1 test, which will fail, and you should have access to the html report which will have a screenshot of the failure, a trace viewer file of the failure but no video.

Take some time and attempt to solve this challenge. Here is some nice relaxing music to help time box your self to.

(Embedded video omitted by scrape extraction.)

## Have you solved it? Scroll below to see the solution.

Alright hopefully you attempted to solve the problem or at least inspected the code. My first instinct was to check the `playwright.config.ts` file which when looking everything looked good. The original example actually had a lot more code in the test, which made the error really easy to over look, so my next step in debugging was to actually check out the official playwright documentation and implementation.

I created a new directory and installed playwright from scratch via `npm init playwright@latest`. I enabled video in the playwright config, I then copied and pasted the bulk of the test within the test block over and run the test and whalla I had a video recording.

This led me to further inspect the Playwright test file and specifically the beforeEach block. Here is where the issue lies.

```javascript

```

## The Solution

The issue is we are creating a new variable named page and not using the `page` that the playwright test framework provides. The import `test` gives you access to page you just have to pass it into the test block in order to use it. If you were using [Playwright Library](https://playwright.dev/docs/library) by itself without the test runner, this step would be necessary in order to create a browser context. But when using Playwright Test you have access to the `page` fixture by default. You can read more about it in the first section of the [Fixture Docs](https://playwright.dev/docs/test-fixtures). The page fixture is one of the pre-defined fixtures that are always available when using Playwright Test.

![](https://playwrightsolutions.com/content/images/2024/09/image-1.png)

I ended up removing the `Page` from the import block, I removed the beforeEach() block, and I am now passing in `({ page })` within the test block. This will give me access to the `page` that Playwright Test builds for me by default.

```javascript

```

This got me thinking, I don't think this problem would have been as easy for me to identify or solve without knowing about the goodies that Playwright provides out of the box. This came from reading the [Playwright Docs](https://playwright.dev/), which is something I always encourage folks to do, there is a plethora of knowledge awaiting you!

![](https://playwrightsolutions.com/content/images/2024/09/image-2.png)

## Another solution

Let's say you don't want to use the built in filter. Another option would be to set the video settings within the browser context. Here is a link to a response from one of the Playwright development team members [Max](https://github.com/mxschmitt) - [https://github.com/microsoft/playwright/issues/14164#issuecomment-1131451544](https://github.com/microsoft/playwright/issues/14164#issuecomment-1131451544)

This is the work around he provided implemented

```javascript

```

Did you find another way to solve this challenge? I'd love to hear from you!

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 4. Butch Mayhew / Playwright Solutions — [Update v1.46] Is it possible to run only Playwright Tests that changed in GitHub actions on a pull request?

- Source: https://playwrightsolutions.com/update-v1-46-is-it-possible-to-run-only-playwright-tests-that-changed-in-github-actions-on-a-pull-request/
- Retrieved: 2026-08-29
- Firecrawl status: complete

Good news everyone! This is now possible to do without any external scripts. It is all baked into the [Playwright CLI](https://playwright.dev/docs/test-cli#reference).

## Original solution

If you've been subscribed for some time you may have remembered the original solution. It included getting a list of the file names by running a narly `git diff` command and piping the results to `grep` and `tr` commands. Storing those file names to a `CHANGED` environment variable, and passing that into the `npx playwright test ${{ env.CHANGED }}`.

(Original post: [Is it possible to run only Playwright Tests that changed on a pull request?](https://playwrightsolutions.com/is-it-possible-to-run-only-playwright-tests-that-changed-on-a/))

## Updated solution

There was a flaw with my approach that I just accepted as I didn't see an easy way to solve the problem. I was only checking and building the CHANGED list of tests run from the `*.spec` files. This list didn't include any page object files, helpers, fixtures, or data factory files. This was a known risk in my case, but with the built in playwright command they have solved this problem. See version 1.46 [release notes](https://playwright.dev/docs/release-notes#--only-changed-cli-option) below.

> New CLI option `--only-changed` will only run test files that have been changed since the last git commit or from a specific git "ref". This will also run all test files that import any changed files.

## Running pre-commit and post-commit

One thing to note with this solution, the command `--only-changed` **by itself will only run files that have been changed, but not yet committed.** This is important as you commit files to your local branch this command will not pick up those test changes after committed.

In order to pick up changes on committed files you will need to pass in a reference branch to check against. `--only-changed=main` for example will check your local git changes and do a diff against the main branch and run those changes.

## Example on playwright-api-test-demo

Let's update our [playwright-api-test-demo](https://github.com/playwrightsolutions/playwright-api-test-demo) repo with the changes both in our `package.json` file to create a new npm script `npm run changed`.

```javascript

```

We'll also update our `on-pr-files-changed.yml` file removing all the old `CHANGED` logic we previously implemented. The main thing we are adding is the `--only-changed=origin/$GITHUB_BASE_REF`. Using the [`$GITHUB_BASE_REF`](https://arc.net/l/quote/gvvfrill) here instead of `main` as the branch will prevent an error I was receiving when running in CI when I was using `--only-changed=origin/main` as the release notes demonstrated. The specific error I received is below.

```bash

```

The changes that worked for me were adding the fetch-depth: 0 (the default is 1) and adding the --only-changed flag in my npx playwright test command.

```javascript

```

The full example can be found below.

```javascript

```

## Using only-changed on monorepos

One thing to note in my research [Pramod Yadav](https://www.linkedin.com/in/pramodkyadav/) (go give him a follow on LinkedIn) reported a [Bug in the GitHub Issues](https://github.com/microsoft/playwright/issues/32452) section of the Playwright repository. He attempted to implement this within a monorepo and found there were some extra steps to get things working. Specifically adding `git config --global --add safe.directory '*'` as a command that is run after checking out the code and before the playwright test execution.

```javascript

```

![](https://playwrightsolutions.com/content/images/2024/09/image.png)

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 5. Butch Mayhew / Playwright Solutions — How Do You Scroll To The Bottom Of An Infinite Scrolling Page In A Playwright Test?

- Source: https://playwrightsolutions.com/how-do-you-scroll-to-the-bottom-of-an-infinite-scrolling-page-in-a-playwright-test/
- Retrieved: 2026-08-29
- Firecrawl status: complete

This week our team had a renewed focus on addressing some of our flakey tests, and one of the things we ran across was an interesting page interaction.

We wanted to utilize our drop down to limit the amount of responses within a page. The options for filtering are 5, 10, 25, and 50. The drop down is at the bottom of the page, and we had some flakey tests where 1 in every 5 test runs, the screen would get in a state where the drop down is clicked and partially viewable but not scrolled down to the element to interact with it. See the video below.

(Embedded video demo omitted by scrape extraction.)

The logs from the html report show where it attempted to scroll down but because the way the element is built, I guess it couldn't.

```javascript

```

## Set click() to force true

As I thought about how to solve this problem, 2 ways came to mind. The first being just set `force:true` on the [click action](https://playwright.dev/docs/input#forcing-the-click). This can be seen below. This is a good solution but I wanted to have the page scrolled like a user would rather than automagically clicking the button. Also note that I am using `this.page` syntax as I am running this code from a Class (my page object).

```javascript

```

## Scroll to the bottom of the page

So I started looking at how to implement ways to scroll to the bottom of the page. I started by actually trying to just use the [.scrollIntoViewIfNeeded()](https://playwright.dev/docs/api/class-locator#locator-scroll-into-view-if-needed) function playwright, but I guess the browser thought the element was already in view much like the html report error logs showed it attempted to scroll into view.

My work around was to use page.evaluate, grab the document.body.scrollHeight and use window.scrollTo() the value. This function will also take into account any lazy loading pages that may grow as you scroll, this was not a concern of mine but the code will handle it, though you may need to adjust the delay.

```javascript

```

The above code was listed as a part of a solution to a [GitHub question](https://github.com/microsoft/playwright/issues/4302), if your looking for other alternatives or examples there are a few in the comments.

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 6. Sergei Gapanovich / Playwright Solutions — How To Use Playwright's testInfo.retry To Deal With Flakey Environments

- Source: https://playwrightsolutions.com/how-to-use-playwrights-testinforetry-to-deal-with-flakey-environments/
- Retrieved: 2026-08-29
- Firecrawl status: complete
- Note: author confirmed via page metadata ("Written by Sergei Gapanovich") — the iteration-4 index attributes this post to Butch Mayhew by mistake; the index itself flags this correction. Published 2024-06-24.

The environment I am working with can be unstable sometimes. To ensure I don't have false negative tests, I need to rerun the failed checks. Playwright makes this easy by allowing you to pass `--retries` followed by the number of retries.

## Skipping certain tests if retried

The problem was that I didn't want to retry certain checks when running my automation suite (1500 checks in total). The solution was found in `testInfo`. `testInfo` provides useful information about currently running specs. One of its properties is `retry`, which returns the current retry number (0 for the initial run). I realized that, in my case, if `retry` is greater than 0, I could just skip the test.

```javascript

```

When the test is run:

- the test fails because 1 is not 2.
- `testInfo` is updated, and the `retry` property now equals 1.
- the `if` block says to skip the test if `testInfo.retry` is null, false, or undefined.

\\* for better readability, you can write the `if` block as `if (testInfo.retry > 0)`.

## Adding a delay to your test if retried

If your environment is acting up and can't handle the load, you can add artificial waits with `.retry`. You can increase the wait based on the retry number.

```javascript

```

When the test is run:

- the initial run doesn't wait for anything because 0 * 2 = 0.
- if the test fails, the first retry will wait for 2 seconds because 1 * 2 = 2, and so on.

\\* modify the seconds to your needs.

Here is the code for the `waitFor()` function:

```javascript

```

## Cleaning up data after test retries

`testInfo.retry` can also be used to clean up data. For example, if you want to delete certain data created by retries but keep what's created by the initial run, you could write the code this way:

```javascript

```

## You are only limited by your imagination

These are just examples I used, but you can utilize `testInfo.retry` for things like names or IDs of the data you create. This might help you track (if needed) what data was created during which retry.

```javascript

```

Let your imagination go wild! =)

The official Playwright docs for retries can be found below!

(Retries | Playwright: https://playwright.dev/docs/test-retries#retries)

I hope you find this useful, and if you did, please ❤️ and subscribe below to receive more useful tips. If you want to reach out to me personally, feel free to connect or message on [LinkedIn](https://www.linkedin.com/in/sgapanovich).

*Note: the original article's code blocks render as embedded content on the site and were returned empty by the scrape.*

### 7. ScrollTest / Pramod Dutta — Playwright Custom Matchers with expect.extend

- Source: https://scrolltest.com/playwright-custom-matchers-expect-extend/
- Retrieved: 2026-08-29
- Firecrawl status: complete

Every Playwright suite eventually grows a pile of repeated assertions: checking that an element has a specific data attribute, that an API response carries a valid JWT, or that a price string is formatted correctly. Copy-pasting `expect(...).toBe(...)` chains everywhere makes tests noisy and failure messages cryptic. In this guide you will learn how Playwright custom matchers with `expect.extend` let you wrap that logic into named, reusable, auto-retrying assertions that read like plain English and produce clear diagnostics when they fail.

## What Is expect.extend in Playwright?

Playwright's `expect` ships with a rich set of built-in matchers like `toBeVisible()`, `toHaveText()`, and `toHaveURL()`. But your application has domain-specific rules that Playwright cannot know about. `expect.extend` is the official API for teaching `expect` new matchers. You pass it an object whose keys are matcher names and whose values are functions that return a result object describing whether the assertion passed.

Playwright builds its `expect` on top of the same matcher contract popularized by Jest, so the function signature will feel familiar. Each matcher receives the _received_ value as its first argument, followed by any arguments the caller passed. It must return an object with a boolean `pass` and a `message` function that explains the result for both the positive and negated (`.not`) cases.

```typescript
import { expect } from '@playwright/test';

expect.extend({
  toBeWithinRange(received: number, floor: number, ceiling: number) {
    const pass = received >= floor && received <= ceiling;
    return {
      pass,
      message: () =>
        `expected ${received} ${pass ? 'not ' : ''}to be within range ${floor} - ${ceiling}`,
      name: 'toBeWithinRange',
      expected: `${floor} - ${ceiling}`,
      actual: received,
    };
  },
});

// Now usable anywhere expect is imported from this module:
// expect(7).toBeWithinRange(1, 10);
// expect(20).not.toBeWithinRange(1, 10);
```

That is the entire mechanism. The `pass` flag drives the result, and Playwright automatically inverts it when you chain `.not`. The `message` function is only invoked when the assertion fails, so you can build a detailed string without paying for it on the happy path.

## Why Custom Matchers Beat Helper Functions

You could write a plain helper like `function assertInRange(n, lo, hi)`, so why bother with `expect.extend`? Custom matchers integrate with Playwright's reporter, trace viewer, and step output. A failure shows up as a real expectation with a labelled actual-versus-expected diff, not a generic thrown error buried in a stack trace.

- **Readable call sites** — `expect(order.total).toBeWithinRange(10, 50)` states intent better than a bare function call.
- **Negation for free** — every matcher automatically supports `.not` with no extra code.
- **Better failure messages** — the `message()` function and `actual`/`expected` fields feed Playwright's HTML report.
- **Composable with soft assertions** — your matcher works with `expect.soft` and `expect.poll` just like a built-in.

| Approach | Reporter integration | Supports .not | Auto-retry | Reusability |
| --- | --- | --- | --- | --- |
| Plain helper function | No | No | No | Manual import |
| Inline expect chain | Partial | Built-ins only | Built-ins only | Copy-paste |
| Custom matcher (expect.extend) | Yes | Yes | Yes (async polling) | One shared module |

## Building a Locator-Aware Custom Matcher

The most useful matchers operate on Playwright `Locator` objects and follow the auto-retrying behaviour of built-ins like `toBeVisible()`. To get retries, your matcher's body must `await` a Locator method that itself retries, or use `expect.poll`/web-first assertions internally. Here is a matcher that asserts an element carries a specific `data-state` attribute, polling until it does or the timeout expires.

```typescript
import { expect, type Locator } from '@playwright/test';

expect.extend({
  async toHaveDataState(locator: Locator, expected: string, options?: { timeout?: number }) {
    let actual: string | null = null;
    let pass = false;
    try {
      // expect.poll retries the callback until it returns the awaited value.
      await expect
        .poll(async () => {
          actual = await locator.getAttribute('data-state');
          return actual;
        }, { timeout: options?.timeout ?? 5000 })
        .toBe(expected);
      pass = true;
    } catch {
      pass = false;
    }
    return {
      pass,
      name: 'toHaveDataState',
      expected,
      actual,
      message: () =>
        `expected locator to ${pass ? 'not ' : ''}have data-state="${expected}", ` +
        `but last saw "${actual}"`,
    };
  },
});

// Usage inside a test:
// await expect(page.getByTestId('toggle')).toHaveDataState('open');
```

By delegating the waiting to `expect.poll`, the matcher inherits Playwright's retry loop. The element does not need to be in the right state immediately; the matcher keeps re-reading the attribute until the timeout. This is the single biggest reason to write locator matchers with `expect.extend` instead of a one-shot helper.

## Adding TypeScript Declarations So expect Is Type-Safe

Out of the box, TypeScript does not know your new matchers exist, so calling `expect(x).toBeWithinRange(1, 10)` raises a compile error. You fix this with a declaration-merging block that augments Playwright's `Matchers` interface. Place it in a `.d.ts` file or at the top of your matcher module.

```typescript
import { expect, type Locator } from '@playwright/test';

// Augment Playwright's Matchers interface. R is the receiver type.
declare module '@playwright/test' {
  interface Matchers<R, T = unknown> {
    toBeWithinRange(floor: number, ceiling: number): R;
    toHaveDataState(expected: string, options?: { timeout?: number }): Promise<R>;
  }
}

expect.extend({
  toBeWithinRange(received: number, floor: number, ceiling: number) {
    const pass = received >= floor && received <= ceiling;
    return {
      pass,
      name: 'toBeWithinRange',
      message: () => `expected ${received} ${pass ? 'not ' : ''}to be within ${floor}-${ceiling}`,
    };
  },
  async toHaveDataState(locator: Locator, expected: string, options?: { timeout?: number }) {
    let actual: string | null = null;
    let pass = false;
    try {
      await expect
        .poll(async () => (actual = await locator.getAttribute('data-state')), {
          timeout: options?.timeout ?? 5000,
        })
        .toBe(expected);
      pass = true;
    } catch {
      pass = false;
    }
    return {
      pass,
      name: 'toHaveDataState',
      actual,
      expected,
      message: () => `expected data-state "${expected}", saw "${actual}"`,
    };
  },
});
```

A few things matter here. The generic parameters `<R, T>` must match Playwright's own signature, where `R` is the return type the chain expects. Async matchers return `Promise<R>` so that `await expect(...).toHaveDataState(...)` type-checks. Make sure `tsconfig.json` includes your declaration file, and import this module once so the side-effecting `expect.extend` call actually runs.

## Registering Matchers Globally Across the Suite

### Use a setup file imported everywhere

Calling `expect.extend` only registers matchers in the module where it runs. To make them available in every spec, put the call in a single file and load it before tests. The cleanest way is a custom fixture file that re-exports `test` and `expect`, so importing from it pulls in the matchers as a side effect.

```typescript
// fixtures.ts — import this instead of '@playwright/test' in your specs
import { test as base, expect } from '@playwright/test';
import './matchers'; // runs expect.extend once for the whole process

export const test = base;
export { expect };

// some.spec.ts
import { test, expect } from './fixtures';

test('order total falls in the expected band', async ({ page }) => {
  await page.goto('/checkout');
  const totalText = await page.getByTestId('grand-total').innerText();
  const total = Number(totalText.replace(/[^0-9.]/g, ''));
  expect(total).toBeWithinRange(10, 500);
  await expect(page.getByTestId('cart-drawer')).toHaveDataState('open');
});
```

Alternatively, add the matcher file to `globalSetup` or list it as the first import in a shared base test. The key idea is that `expect.extend` mutates the shared `expect` object, so it only needs to execute once per worker process. Importing the side-effecting module from your fixtures file guarantees that.

## A Practical Example: Validating an API Response Shape

Custom matchers are not limited to UI. When you test APIs with Playwright's `request` fixture, a matcher that validates a response body keeps your tests declarative. Below, `toBeSuccessfulJson` checks the status code and that the parsed body contains the expected keys, producing a precise message about what was missing.

```typescript
import { expect, type APIResponse } from '@playwright/test';

declare module '@playwright/test' {
  interface Matchers<R, T = unknown> {
    toBeSuccessfulJson(requiredKeys: string[]): Promise<R>;
  }
}

expect.extend({
  async toBeSuccessfulJson(response: APIResponse, requiredKeys: string[]) {
    const status = response.status();
    let body: Record<string, unknown> = {};
    let missing: string[] = [];
    try {
      body = await response.json();
      missing = requiredKeys.filter((k) => !(k in body));
    } catch {
      return {
        pass: false,
        name: 'toBeSuccessfulJson',
        message: () => `expected JSON body but response was not parseable (status ${status})`,
      };
    }
    const pass = status >= 200 && status < 300 && missing.length === 0;
    return {
      pass,
      name: 'toBeSuccessfulJson',
      message: () =>
        pass
          ? `expected response NOT to be successful JSON, but it was (status ${status})`
          : `expected 2xx with keys [${requiredKeys.join(', ')}]; ` +
            `got status ${status}, missing [${missing.join(', ')}]`,
    };
  },
});

// Usage:
// const res = await request.get('/api/user/42');
// await expect(res).toBeSuccessfulJson(['id', 'email', 'createdAt']);
```

This matcher reads the body once and reports exactly which keys were absent, which is far more actionable than a generic `expect(res.ok()).toBeTruthy()` failure. Because it returns a promise, callers must `await` it, and the TypeScript declaration enforces that at compile time.

## Tips, Pitfalls, and Best Practices

- **Always handle the negated case** in `message()`. Check `pass` to decide whether to add the word "not", otherwise `.not` failures read backwards.
- **Do not throw inside a matcher** for normal failures — return `pass: false`. Throwing should be reserved for genuinely invalid usage, such as passing a non-Locator.
- **Make locator matchers retry** by delegating to `expect.poll` or to an existing web-first assertion; a single `getAttribute` read will be flaky.
- **Keep matcher modules side-effect-only** for the `expect.extend` call and import them once via fixtures so every worker registers them.
- **Set `actual` and `expected`** on the result object to get a clean diff in the HTML report and trace viewer.

Used well, Playwright custom matchers with `expect.extend` turn brittle, repetitive assertions into a small library of domain-specific expectations that are type-safe, self-documenting, and integrated with Playwright's reporting. Start by extracting your two or three most-repeated assertion patterns into matchers, register them through a shared fixtures file, and your specs immediately become shorter and your failure messages dramatically clearer.

## FAQ

### Do Playwright custom matchers support auto-retrying like built-in web-first assertions?

Not automatically. A matcher only retries if its body awaits something that retries. Wrap your read in `expect.poll` or delegate to an existing web-first assertion inside the matcher, and it will keep re-evaluating until the condition is met or the timeout expires, just like `toBeVisible()`.

### Why does TypeScript complain that my custom matcher does not exist?

TypeScript does not know about runtime-registered matchers until you augment the `Matchers` interface. Add a `declare module '@playwright/test'` block that extends `interface Matchers<R, T>` with your matcher signatures, return `Promise<R>` for async matchers, and ensure the declaration file is included by your `tsconfig.json`.

### Where should I call expect.extend so matchers work in every test?

Call it once in a dedicated matcher module, then import that module from a shared fixtures file that re-exports `test` and `expect`. Have your specs import from the fixtures file instead of directly from `@playwright/test`. Because `expect.extend` mutates the shared `expect`, running it once per worker process is enough.

### 8. ScrollTest / Pramod Dutta — Playwright UI Mode: Time Travel Debugging (TypeScript Day 57)

- Source: https://scrolltest.com/playwright-ui-mode-typescript-day-57/
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Playwright UI Mode featured image: time-travel debugging, Pick Locator, and watch mode in TypeScript](https://scrolltest.com/wp-content/uploads/2026/08/playwright-ui-mode-day-57.png)

I have watched too many SDETs spend an afternoon sprinkling `console.log` statements through a failing Playwright test, reloading the browser by hand, and guessing at selectors. Playwright UI Mode removes all of that. It gives you a time-travel timeline, a live locator picker, and a trace viewer in one window, so you can walk a test backward through every click, fill, and assertion until you find the exact step that broke. This is Day 57 of my Playwright + TypeScript series, and today we go deep on Playwright UI Mode in TypeScript.

## What Is Playwright UI Mode?

Playwright UI Mode is the interactive test runner that ships with `@playwright/test`. You launch it with `npx playwright test --ui` and it opens a local browser window, not just a terminal. From there you can run one test or a whole file, watch it execute step by step, and rewind through the run after it finishes.

The feature landed in [Playwright 1.32](https://playwright.dev/docs/release-notes) and has been my default debugging tool ever since. Before UI Mode, debugging meant juggling two separate tools: the inspector you reached through `page.pause()`, and the trace viewer you opened after the fact with `npx playwright show-trace`. UI Mode merged both into one loop where you run, watch, rewind, fix, and re-run without switching windows.

The [official docs](https://playwright.dev/docs/test-ui-mode) describe it as a combination of a watch mode and a time-travel debugger, and that is exactly how I use it. The project now sits at [94,853 GitHub stars](https://github.com/microsoft/playwright) and `@playwright/test` is [downloaded about 210 million times a month](https://www.npmjs.com/package/@playwright/test), so this is the tool the industry is actually running, and UI Mode is the fastest way to learn it.

Three things make UI Mode different from running tests headless:

- **Time travel.** Every action a test performs is recorded as a step. Click any step to see the page exactly as it looked before and after that action.
- **Watch mode.** Save a test file and the affected tests re-run automatically, with failures surfaced immediately.
- **Pick locator.** Hover over the live page and copy a role-based locator straight into your test file, no guessing with CSS.

If you already know the [Playwright trace viewer](https://playwright.dev/docs/trace-viewer) from my [Day 43 on the trace viewer](https://scrolltest.com/playwright-trace-viewer-debugging-day-43/), UI Mode is that same trace, but running live while you drive it.

## Why UI Mode Beats console.log and –debug

I used to debug with `console.log(page.url())` and a prayer. The problem is that logging tells you what your code thinks happened, while the timeline shows you what actually rendered in the browser. When a selector times out, the page state at the moment of failure is worth more than a hundred logs.

Here is the honest comparison I give my team:

- **console.log** shows values, not the DOM. You still have to imagine the page.
- **–debug** pauses on every action and opens the inspector, which is powerful but slow when you already know the rough area of failure.
- **UI Mode time travel** lets you jump straight to the failing step and inspect the real DOM snapshot there.

The result is faster root-cause. When a login test fails at the assertion, I click the last action, see the page still on the login form with a validation error, and I know the click worked but the form rejected the input. That is a 60-second diagnosis that used to take me 20 minutes with print statements.

I keep a rough number in my head from coaching: engineers who switch to timeline-driven debugging cut their average time-to-fix on flaky tests by roughly half, because the first thing they see is the page state, not a line of text they have to interpret. It is the difference between observing and inferring.

## Launching Playwright UI Mode in TypeScript

There is no special TypeScript setup for UI Mode. It reads your existing `playwright.config.ts` and your test files as-is. The minimum is one command:

```
npx playwright test --ui
```

I prefer a package.json script so the whole team runs it the same way:

```
{
  "scripts": {
    "test": "playwright test",
    "test:ui": "playwright test --ui",
    "test:ui:headed": "playwright test --ui --headed"
  }
}
```

A minimal config that plays well with UI Mode looks like this:

```
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  use: {
    baseURL: 'https://scrolltest.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
});
```

Three details matter here:

1. **Set `baseURL`.** UI Mode uses it to navigate when you open a new page, and the Pick Locator tool needs a live URL to work against.
2. **Keep `trace` on at least on first retry.** UI Mode records an in-session trace for time travel, but if you want a shareable `trace.zip` after the session, your config must request one.
3. **Run headed.** UI Mode opens a visible browser by default. If you are on a remote box over SSH, add `--ui-host 0.0.0.0` and open the port in your browser.

## Configuring the UI Server: Port, Host, and URL

UI Mode runs a small local server behind the scenes, and you can control it with two flags. By default it binds to `localhost` on a random port, which is fine on a laptop but breaks the moment you run Playwright inside a container or a remote virtual machine.

```
npx playwright test --ui --ui-port 8080 --ui-host 0.0.0.0
```

- **`--ui-port`** fixes the port so you can bookmark it or expose it through Docker.
- **`--ui-host 0.0.0.0`** makes the UI reachable from outside the machine, which you need in dev containers and GitHub Codespaces.

I hit this the first time I tried to debug a test inside a Dev Container. The UI started, but the browser on my host could not reach `localhost` inside the container. Binding to `0.0.0.0` and forwarding the port fixed it in one change. Most modern editors forward the port for you once it is stable, so fixing `--ui-port` first saves a lot of re-connection.

There is no meaningful performance difference from these flags. They only change how you reach the UI, not how tests run, so they are safe to commit into a `test:ui` script or a dev container config.

## The Time-Travel Timeline

The timeline is the heart of UI Mode. After a test runs, the left sidebar lists every action in order: navigation, click, fill, expect, and any `test.step` blocks you defined. Click an action and the right pane shows the page snapshot for that moment.

Use named steps to make the timeline readable:

```
import { test, expect } from '@playwright/test';

test('a user can reset their password', async ({ page }) => {
  await test.step('open login page', async () => {
    await page.goto('/login');
  });

  await test.step('request reset link', async () => {
    await page.getByRole('button', { name: 'Forgot password?' }).click();
    await page.getByLabel('Email').fill('dev@scrolltest.com');
    await page.getByRole('button', { name: 'Send reset link' }).click();
  });

  await test.step('confirm success message', async () => {
    await expect(page.getByText('Check your inbox')).toBeVisible();
  });
});
```

Now the timeline reads like a story: open login page, request reset link, confirm success message. When the final assertion fails, you click “request reset link”, inspect the snapshot, and see whether the button was enabled, the email field had a value, and the request actually fired.

Let me walk through a real diagnosis I did last month. A checkout test was failing at the final “Place order” assertion. I clicked the step where the test filled the card number and saw the field still showed the placeholder, meaning `fill()` never landed. The step before it had switched into an iframe for the payment form, and the selector was scoped to the parent frame. The before and after snapshots made that obvious in seconds, because I could see the field empty in the “after” state of the fill step.

Two small things I rely on constantly:

- **Before/after snapshots.** Each step shows the state before the action and after it. The difference is often the bug.
- **Jump to source.** The trace panel has a “Source” tab that opens the exact line of the failing step, so you can fix the test without leaving the tool.

If this feels familiar, it is the same engine as the standalone trace viewer I covered in [Day 43](https://scrolltest.com/playwright-trace-viewer-debugging-day-43/). UI Mode just wraps it in a live loop.

## Watch Mode and Test Filtering

UI Mode watches your test files. Edit `login.spec.ts`, save, and the tests in that file re-run automatically. Failures show up in the list with the reason, and you click one to open its timeline.

For a big suite, filtering keeps watch mode fast:

- **Text filter.** Type part of a test name in the search box to narrow the list.
- **Tag filter.** Use `--grep` when launching, for example `npx playwright test --ui --grep @smoke`.
- **Project filter.** The top bar lists your projects from the config, so you can run only Chromium.
- **Status filter.** Toggle to show only failed tests.

Here is a tag-based example. Annotate a smoke test:

```
import { test, expect } from '@playwright/test';

test('home page renders', { tag: '@smoke' }, async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'The Testing Academy' })).toBeVisible();
});
```

Then launch only the smoke set with `npx playwright test --ui --grep @smoke`. This is how I keep watch mode from re-running 400 tests every time I change one helper. The filter also survives into the watch loop, so when you save a file, only the filtered subset re-runs.

## The Pick Locator Tool

Pick Locator is the fastest way I know to write a correct selector. Click the crosshair button in the toolbar, hover over any element on the live page, and Playwright proposes a locator. It prefers role-based locators, which is exactly what you want for stability.

Hover over a “Sign in” button and it suggests:

```
page.getByRole('button', { name: 'Sign in' })
```

Click the element and the locator is inserted into your test file at the cursor. You can also edit the proposed locator inline and Playwright re-highlights the matching element live, so you know instantly whether your change still matches one element, many, or none.

Why this matters for TypeScript projects specifically: it nudges you toward the accessibility tree instead of brittle CSS like `button.btn-primary`. A locator based on role and accessible name survives a redesign where the class name changes. If you want the full selector strategy, revisit my [Day 31 on debugging](https://scrolltest.com/playwright-debugging-typescript-day-31/), which covers why stable locators fix most flaky tests.

## Reading the Trace, Console, and Source Panes

Every test in UI Mode gets a trace with four panes worth learning:

- **Actions.** The step list we covered, clickable for time travel.
- **Network.** Every request and response, with status codes, so you can spot a 500 or a missing API call.
- **Console.** Browser console output, including page errors and your own `console.log` calls if you still use them.
- **Source.** The test code, with the failing line highlighted.

The network pane is where I catch the silent failures. A test that passes locally but fails in staging is often a CORS error or a 401, and the network pane shows the red status without me touching the dev tools. Combined with the [official debugging guide](https://playwright.dev/docs/debug), this covers 90 percent of what a QA engineer needs to triage a failing test.

One habit worth building: after every failing run, open the network pane and scan for any status code that is not a 2xx or expected 3xx before you look at the assertion. A surprising number of “flaky” tests are actually a real API regression hiding behind a UI that recovered gracefully.

## Screenshot Comparison in the UI

When a `toHaveScreenshot` assertion fails, UI Mode shows a proper visual diff instead of just a red line. You get the expected image, the actual image, and a highlighted difference, side by side or as a slider.

```
import { test, expect } from '@playwright/test';

test('checkout page matches baseline', async ({ page }) => {
  await page.goto('/checkout');
  await expect(page).toHaveScreenshot('checkout.png');
});
```

This ties into the visual testing work in my [Day 53 on screenshots and video](https://scrolltest.com/playwright-screenshots-video-recording-typescript-day-53/). The difference here is that UI Mode lets you accept or reject the diff and regenerate the baseline without dropping to the command line. It is the same pixel-perfect workflow, but faster to iterate on. For visual regression at scale you still want the CLI and a CI job, but for the first baseline and quick fixes, the UI diff is the fastest loop.

## Playwright UI Mode Pitfalls

UI Mode is forgiving until it is not. These are the six mistakes I see most often, in the order they bite:

1. **Leaving `page.pause()` in committed code.** It opens the inspector and hangs CI. Use `page.pause()` locally, then remove it or guard it behind an environment flag so it never ships to the pipeline.
2. **Trying to run UI Mode in CI.** There is no display in a headless runner. UI Mode is a local tool. CI gets the HTML report and a `trace.zip`, not a time-travel window.
3. **Not requesting a shareable trace.** In-session time travel vanishes when you close UI Mode. Set `trace: 'on-first-retry'` or `'retain-on-failure'` so a teammate can open the trace later.
4. **Ignoring worker count when reproducing.** UI Mode runs with multiple workers. If a test only fails when run alone, set workers to one in the UI to mirror a clean single-threaded run.
5. **Filtering too aggressively.** A test that depends on a shared fixture or state can pass in isolation and fail in the full run. If a failure only appears in the whole suite, run the whole file, not one test.
6. **Trusting a green timeline without reading the network pane.** A test can pass while the app logged a 500 the UI silently recovered from. The network pane is where real bugs hide.

The common thread across all six is the same mistake: treating UI Mode like a terminal runner instead of a diagnostic tool. Use it to see what happened, not just to see red and green.

## India Context: What Hiring Managers Expect

In Bengaluru, Hyderabad, and Pune, Playwright with TypeScript is now a baseline requirement for automation roles, not a differentiator. What separates candidates at the ₹15 to 40 LPA band is debugging skill, and UI Mode is the visible proof of it. When I interview an SDET, I ask them to walk me through a failing test, and the ones who open the timeline and read the network pane instead of adding `console.log` stand out immediately.

Service companies like TCS and Infosys are moving more teams onto Playwright, and product companies want engineers who can triage a flaky CI run without calling a senior. If you are preparing for interviews, being able to say “I debug with UI Mode time travel and the trace viewer, not print statements” is a concrete, honest answer that lands better than listing ten frameworks.

Start with one change this week: launch `npx playwright test --ui` on a real failing test, find the exact step, and fix it from the Source panel. That single habit will do more for your interview than another certificate.

## Key Takeaways

Playwright UI Mode is the fastest way to turn a red test into a fixed one, and it is the first skill I tell any TypeScript automation learner to build. The summary:

- Launch with `npx playwright test --ui` and run headed for full time-travel debugging.
- Use named `test.step` blocks so the timeline reads like a story.
- Click any action to inspect the before and after page snapshots.
- Let Pick Locator write role-based locators instead of guessing CSS.
- Read the network pane for silent 500s and CORS errors that green tests hide.
- Never ship `page.pause()`, and never expect UI Mode to run in CI.

## FAQ

### Is Playwright UI Mode free?

Yes. It ships inside `@playwright/test`, which is open source under the Apache 2.0 license on [GitHub](https://github.com/microsoft/playwright). There is no paid tier for the runner or the trace viewer.

### Does UI Mode work with TypeScript tests?

Yes, with no extra setup. UI Mode runs the same compiled TypeScript that `npx playwright test` runs, so your `playwright.config.ts`, fixtures, and typed locators all work unchanged.

### Can I share the time-travel trace with my team?

Yes, but only if your config requests a trace. Set `trace: 'on-first-retry'` or `trace: 'on'` in `playwright.config.ts`, then share the generated `trace.zip`. Anyone can open it with `npx playwright show-trace trace.zip`.

### How is UI Mode different from the trace viewer?

The trace viewer opens a finished trace file after the fact, while UI Mode runs tests live and lets you watch them execute, filter them, and re-run on save. They share the same timeline engine, and you can learn the details in the [trace viewer docs](https://playwright.dev/docs/trace-viewer).

### Why does my test pass alone but fail in UI Mode?

Usually worker count or state ordering. UI Mode runs tests in parallel by default. Run the whole file, or set workers to one, to reproduce the same conditions as a single-threaded CI run.

### Does UI Mode replace the HTML report?

No. UI Mode is for interactive local debugging. CI still needs the HTML reporter and archived traces. I covered report setup in [Day 54 on custom reporters](https://scrolltest.com/playwright-custom-reporter-typescript-day-54/).

### Can I run UI Mode for a single project or browser?

Yes. Pick the project from the dropdown in the top bar, or launch with `npx playwright test --ui --project=chromium`. This is useful when a bug only reproduces in WebKit or Firefox, and it keeps the run fast while you iterate.



### 9. ScrollTest / Pramod Dutta — Playwright Authentication Storage: Skip Login in Every Test With storageState

- Source: https://scrolltest.com/playwright-authentication-storage-state-skip-login/
- Retrieved: 2026-08-29
- Firecrawl status: complete

Logging in before every single test is slow, flaky, and wasteful. Playwright's `storageState` lets you authenticate once, save the session, and reuse it across your entire test suite. Here is exactly how to set it up.

## How storageState Works

Playwright can save cookies and localStorage to a JSON file after login. Every subsequent test loads that file instead of navigating through the login form. Your 200-test suite goes from 200 login flows to 1.

## Step 1: Create the Auth Setup

```typescript
// auth.setup.ts
import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('authenticate', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('admin@example.com');
  await page.getByLabel('Password').fill('secure-password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByText('Dashboard')).toBeVisible();

  await page.context().storageState({ path: authFile });
});
```

## Step 2: Configure playwright.config.ts

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: {
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ],
});
```

## Multiple Roles: Admin, User, Guest

```typescript
// admin.setup.ts
setup('admin auth', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('admin@example.com');
  await page.getByLabel('Password').fill('admin-pass');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.context().storageState({ path: 'playwright/.auth/admin.json' });
});

// user.setup.ts
setup('user auth', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('user@example.com');
  await page.getByLabel('Password').fill('user-pass');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.context().storageState({ path: 'playwright/.auth/user.json' });
});
```

## CI/CD: Handle Token Expiry

```yaml
# GitHub Actions - regenerate auth before tests
- name: Run auth setup
  run: npx playwright test --project=setup
- name: Run tests with cached auth
  run: npx playwright test --project=chromium
```

**Important:** Add `playwright/.auth/` to your `.gitignore`. Never commit auth state files containing real credentials or session tokens.



### 10. ScrollTest / Pramod Dutta — Playwright Infinite Scroll and Lazy Loading

- Source: https://scrolltest.com/playwright-infinite-scroll-lazy-loading/
- Retrieved: 2026-08-29
- Firecrawl status: complete

Infinite scroll feeds and lazy-loaded images look effortless to users, but they are a minefield for test automation: the content you want to assert on simply does not exist in the DOM until you scroll, and naive `page.waitForTimeout` hacks make suites slow and flaky. In this guide you will learn how to test **Playwright infinite scroll lazy loading** reliably using auto-waiting locators, deterministic scroll loops, network interception, and image-load verification in TypeScript. Every example below uses only real Playwright APIs you can run today.

Contents \[ [show](https://scrolltest.com/playwright-infinite-scroll-lazy-loading/#)\]

## Why Infinite Scroll Breaks Naive Tests

Traditional pagination renders all items (or a fixed page) into the DOM up front, so a test can immediately query them. Infinite scroll inverts this: a small batch loads first, and additional batches are fetched only when the user nears the bottom of the viewport. An `IntersectionObserver` or scroll listener triggers an XHR/fetch call, the response is appended, and the cycle repeats. Lazy loading applies the same idea to images and iframes via the native `loading="lazy"` attribute or a JavaScript observer that swaps `data-src` into `src`.

This creates three classic failure modes for tests:

- **Content not yet in the DOM** — asserting on item #200 fails because only 20 are rendered.
- **Race conditions** — you scroll, but the network request has not resolved, so the assertion runs against stale content.
- **Detached nodes** — virtualized lists (react-window, TanStack Virtual) recycle DOM nodes, so a locator captured earlier points to a removed element.

The fix is never `page.waitForTimeout(3000)`. Instead, lean on Playwright’s web-first assertions, which auto-retry until a condition is met or the timeout expires, and on explicit waits tied to real signals like network responses or element counts.

## The Core Scroll Loop in Playwright

The most robust pattern is a bounded loop: scroll, wait for the item count to grow, and stop when it stabilizes or hits a target. Counting locators with `locator.count()` and comparing across iterations gives you a deterministic exit condition rather than guessing at timeouts.

```typescript
import { test, expect, Page } from '@playwright/test';

async function scrollUntilStable(page: Page, itemSelector: string, maxScrolls = 30) {
  const items = page.locator(itemSelector);
  let previousCount = await items.count();

  for (let i = 0; i < maxScrolls; i++) {
    // Scroll the last known item into view to trigger the next batch.
    await items.last().scrollIntoViewIfNeeded();

    // Wait for the count to grow past the previous value, or bail after 5s.
    try {
      await expect(async () => {
        expect(await items.count()).toBeGreaterThan(previousCount);
      }).toPass({ timeout: 5000 });
    } catch {
      break; // No new items loaded -> we have reached the end.
    }
    previousCount = await items.count();
  }
  return previousCount;
}

test('loads all feed items via infinite scroll', async ({ page }) => {
  await page.goto('https://example.com/feed');
  const total = await scrollUntilStable(page, '[data-testid="feed-item"]');
  expect(total).toBeGreaterThan(20);
});
```

Two details matter here. First, `scrollIntoViewIfNeeded()` is preferred over manual `window.scrollTo` because Playwright scrolls the actual element into the viewport, which is exactly what triggers an `IntersectionObserver`. Second, the `expect.poll`-style `toPass()` block retries the count assertion, so you never sleep longer than necessary and never assert too early.

### Scrolling a Container Instead of the Window

Many feeds scroll inside a fixed-height div rather than the page body. In that case `scrollIntoViewIfNeeded` still works, but if you need to drive the scroll manually use `locator.evaluate` to scroll the container element directly.

```typescript
test('scrolls inside a fixed-height container', async ({ page }) => {
  await page.goto('https://example.com/feed');
  const scroller = page.locator('[data-testid="scroll-container"]');
  const items = scroller.locator('.row');

  let previous = 0;
  for (let i = 0; i < 25; i++) {
    await scroller.evaluate((el) => {
      el.scrollTop = el.scrollHeight;
    });
    await expect.poll(() => items.count(), { timeout: 4000 })
      .toBeGreaterThanOrEqual(previous);
    const current = await items.count();
    if (current === previous) break;
    previous = current;
  }
  expect(previous).toBeGreaterThan(0);
});
```

## Waiting on the Real Signal: Network Responses

Counting DOM nodes works, but the most reliable trigger-and-wait pattern pairs the scroll action with `page.waitForResponse`. You scroll, the app fires its pagination request, and you wait for that exact response before asserting. This removes the guesswork entirely and is far less brittle than polling counts when the API is slow.

```typescript
test('waits for the pagination request after each scroll', async ({ page }) => {
  await page.goto('https://example.com/feed');
  const items = page.locator('[data-testid="feed-item"]');

  for (let pageNum = 2; pageNum <= 5; pageNum++) {
    const [response] = await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes(`/api/feed?page=${pageNum}`) && r.status() === 200
      ),
      page.locator('[data-testid="feed-item"]').last().scrollIntoViewIfNeeded(),
    ]);

    const body = await response.json();
    expect(body.items.length).toBeGreaterThan(0);
  }

  await expect(items).toHaveCount(100);
});
```

The `Promise.all` wrapper is important: you must start waiting for the response _before_ the scroll action fires, otherwise a fast API could resolve before the listener is attached and you would deadlock. The web-first assertion `toHaveCount` at the end auto-waits, so even if rendering lags behind the network slightly, the test stays green.

## Mocking Endless Data with page.route

Hitting a real backend makes infinite-scroll tests slow and non-deterministic. With `page.route` you can intercept the pagination endpoint and serve synthetic pages, giving you full control over how many items exist, when the list ends, and how to simulate errors or empty states. This is the single biggest reliability win for these suites.

```typescript
test('mocks paginated API for deterministic scrolling', async ({ page }) => {
  const PAGE_SIZE = 20;
  const TOTAL_PAGES = 4;

  await page.route('**/api/feed**', async (route) => {
    const url = new URL(route.request().url());
    const pageNum = Number(url.searchParams.get('page') ?? '1');

    const items = Array.from({ length: PAGE_SIZE }, (_, i) => ({
      id: (pageNum - 1) * PAGE_SIZE + i + 1,
      title: `Item ${(pageNum - 1) * PAGE_SIZE + i + 1}`,
    }));

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items, hasMore: pageNum < TOTAL_PAGES }),
    });
  });

  await page.goto('https://example.com/feed');
  const total = await scrollUntilStable(page, '[data-testid="feed-item"]');
  expect(total).toBe(PAGE_SIZE * TOTAL_PAGES); // exactly 80 items
});
```

Because the mock controls `hasMore`, the app stops requesting after page 4 and your scroll loop exits cleanly with an exact, asserted count. You can flip a single variable to test the empty state (`TOTAL_PAGES = 0`) or simulate a failure by calling `route.fulfill({ status: 500 })` on a specific page to verify your error UI. For recording real traffic once and replaying it offline, `page.routeFromHAR` is the natural next step.

## Testing Lazy-Loaded Images

Lazy-loaded images need a different assertion than feed items. With native lazy loading the `<img>` has `loading="lazy"` and the browser defers the request until the image nears the viewport. With JavaScript lazy loading, the real URL lives in `data-src` and is swapped into `src` when the observer fires. You verify the swap happened and, ideally, that the image actually decoded.

```typescript
test('lazy image loads only after scrolling into view', async ({ page }) => {
  await page.goto('https://example.com/gallery');
  const img = page.locator('img[data-testid="hero-50"]');

  // Before scrolling: src is still the placeholder / empty, data-src holds the real URL.
  await expect(img).toHaveJSProperty('complete', true); // placeholder loaded
  const dataSrc = await img.getAttribute('data-src');
  expect(dataSrc).toContain('/images/hero-50');

  // Scroll it into view to trigger the IntersectionObserver swap.
  await img.scrollIntoViewIfNeeded();

  // The real URL is now applied and the bitmap has decoded.
  await expect(img).toHaveAttribute('src', dataSrc!);
  await expect.poll(async () => {
    return img.evaluate((el: HTMLImageElement) => el.naturalWidth);
  }, { timeout: 5000 }).toBeGreaterThan(0);
});
```

The `naturalWidth > 0` check is the gold standard for confirming an image truly rendered: a broken image keeps `naturalWidth` at zero even if `src` is set. Combining the attribute swap assertion with the decode check catches both “observer never fired” and “image 404’d” bugs in one test.

### Verifying Requests Are Actually Deferred

The whole point of lazy loading is that off-screen images are _not_ fetched. You can prove this by listening to image requests and asserting that the off-screen image’s URL is absent until you scroll.

```typescript
test('off-screen images are not requested until visible', async ({ page }) => {
  const requested = new Set<string>();
  page.on('request', (req) => {
    if (req.resourceType() === 'image') requested.add(req.url());
  });

  await page.goto('https://example.com/gallery');
  await expect(page.locator('img').first()).toBeVisible();

  // A deep image should not have been fetched yet.
  expect([...requested].some((u) => u.includes('hero-50'))).toBe(false);

  await page.locator('img[data-testid="hero-50"]').scrollIntoViewIfNeeded();
  await expect.poll(() => [...requested].some((u) => u.includes('hero-50')))
    .toBe(true);
});
```

## Handling Virtualized Lists and Overlays

Virtualized lists only keep a handful of DOM nodes alive, so `toHaveCount` against the full dataset will never pass. Instead, assert that a specific item becomes visible after scrolling to it, or test that the rendered window slides correctly. Combine `scrollIntoViewIfNeeded` with a content assertion on a known item.

```typescript
test('virtualized row becomes visible after scrolling', async ({ page }) => {
  await page.goto('https://example.com/virtual-table');
  const target = page.getByText('Item 480', { exact: true });

  // Drive the virtual scroller until the target row is rendered and visible.
  await expect(async () => {
    await page.locator('[role="row"]').last().scrollIntoViewIfNeeded();
    await expect(target).toBeVisible({ timeout: 1000 });
  }).toPass({ timeout: 15000 });

  await expect(target).toBeInViewport();
});
```

Two web-first assertions shine here: `toBeVisible()` waits for the recycled node to mount, and `toBeInViewport()` confirms it is actually scrolled on-screen rather than just attached. Cookie banners and “load more” promo overlays frequently block scroll-triggering elements; `page.addLocatorHandler` lets you register a one-time dismissal that fires automatically whenever the overlay appears mid-scroll, so your loop is never silently blocked.

## Strategy Comparison: Which Wait Approach to Use

| Approach | Best for | Reliability | Real Playwright API |
| --- | --- | --- | --- |
| Poll element count with `toPass` / `expect.poll` | Feeds where you only know items grow | High | `locator.count()`, `expect.poll` |
| `waitForResponse` after scroll | Apps with a predictable pagination endpoint | Very high | `page.waitForResponse` |
| Mock with `page.route` | Deterministic CI, edge cases, error states | Highest | `page.route`, `route.fulfill` |
| `scrollIntoViewIfNeeded` \+ visibility | Virtualized / recycled lists | High | `locator.scrollIntoViewIfNeeded`, `toBeInViewport` |
| `waitForTimeout` (anti-pattern) | Never — avoid | Low (flaky) | `page.waitForTimeout` |

## Best Practices Checklist

- Always bound your scroll loop with a max iteration count so a broken “end” condition cannot hang CI forever.
- Prefer `scrollIntoViewIfNeeded` over `window.scrollTo` because it reliably triggers `IntersectionObserver`.
- Pair scroll actions with `waitForResponse` inside `Promise.all` to avoid race conditions.
- Use `page.route` to make data deterministic and to exercise empty, error, and last-page states.
- Verify lazy images with `naturalWidth > 0`, not just the presence of a `src` attribute.
- For virtualized lists, assert `toBeInViewport()` on a target item rather than `toHaveCount` on the full dataset.
- Register `page.addLocatorHandler` for cookie and promo overlays that interrupt scrolling.

## Conclusion

Testing **Playwright infinite scroll lazy loading** comes down to replacing fixed sleeps with real signals: poll element counts with `expect.poll`, wait on the exact pagination response with `waitForResponse`, and make data deterministic with `page.route`. For lazy images, confirm the observer swap and assert `naturalWidth > 0` to catch broken assets. Apply the bounded scroll loop and the strategy table above, and your scroll-heavy suites will be fast, stable, and trustworthy on CI instead of randomly red.

## FAQ

### How do I scroll to the bottom of an infinite scroll page in Playwright?

Use a bounded loop that calls `locator.last().scrollIntoViewIfNeeded()` and then waits for the item count to grow with `expect.poll(() => items.count())` or for the pagination request via `page.waitForResponse`. Exit the loop when the count stops increasing or a max-iterations guard is reached. Avoid `page.waitForTimeout`, which causes flaky, slow tests.

### How can I test that lazy-loaded images actually rendered?

Scroll the image into view with `scrollIntoViewIfNeeded`, assert the real URL is applied (for example `toHaveAttribute('src', expectedUrl)`), then verify the bitmap decoded by polling `img.evaluate(el => el.naturalWidth)` until it is greater than zero. A `naturalWidth` of zero means the image is broken even when the `src` attribute is present, so this check catches 404s that an attribute assertion would miss.

### Should I mock the API or scroll against the real backend?

For most CI suites, mock with `page.route` and `route.fulfill`. Mocking makes the dataset deterministic, removes network flakiness, runs faster, and lets you assert exact item counts plus edge cases like empty results and last-page boundaries. Keep a small number of end-to-end tests against the real backend (or a recorded `routeFromHAR` session) to catch contract drift, but rely on mocked data for the bulk of your scroll and lazy-load assertions.



### 11. ScrollTest / Pramod Dutta — Playwright TypeScript Framework — Day 21 (Capstone)

- Source: https://scrolltest.com/playwright-typescript-framework-day-21/
- Retrieved: 2026-08-29
- Firecrawl status: complete

![Playwright TypeScript framework Day 21 capstone cover](https://scrolltest.com/wp-content/uploads/2026/06/playwright-typescript-framework-day-21.png)

**Day 21 is the capstone.** In this tutorial, we assemble a production-ready **Playwright TypeScript framework** from the pieces you learned across the series: locators, assertions, fixtures, page objects, API setup, test data, traces, CI, and reporting. The goal is simple: a framework you can explain in an interview and use on a real product without rewriting it next week.

I see many QA engineers complete Playwright basics and then get stuck at the same point. They know how to write one test, but they do not know how to organize 200 tests, 12 page objects, multiple environments, test users, flaky workflows, and CI evidence. This article gives you that missing bridge.

## Capstone Goal: What We Are Building

A good test framework has one job: reduce the cost of confident releases. It should make happy-path tests easy, failure investigation quick, and maintenance boring. If the framework makes every change feel like archaeology, it has already failed.

For Day 21, we are building a compact but realistic **Playwright TypeScript framework** with these parts:

- Strict TypeScript setup with clear scripts.
- Environment-aware Playwright configuration.
- Page Object Models for stable user flows.
- Custom fixtures for users, API clients, and pages.
- API-driven setup so tests do not depend on slow UI preparation.
- Trace, screenshot, video, and HTML report evidence.
- GitHub Actions workflow for CI execution.
- A release checklist that tells you when the suite is healthy.

The official [Playwright documentation](https://playwright.dev/docs/intro) describes Playwright as enabling reliable end-to-end testing for modern web apps. That reliability comes from features like auto-waiting, web-first assertions, browser contexts, tracing, and isolated test execution. The framework design below uses those features instead of fighting them.

### Why this matters for interviews and real teams

In interviews, a simple login test is not enough anymore. Hiring managers ask how you handle parallel execution, flaky tests, test data, reports, and CI failures. In product companies, the same questions show up during release calls. The person who can answer with architecture, not buzzwords, stands out.

For India-based SDETs, this is a practical career filter. Service company projects may still accept script collections, but product companies paying higher packages expect framework thinking: separation of concerns, fast feedback, and strong debugging evidence.

### What the data says

Playwright is not a niche tool now. The `@playwright/test` package recorded **165,464,635 npm downloads in the last month** for the 2026-05-29 to 2026-06-27 window via the npm downloads API. The [Microsoft Playwright GitHub repository](https://github.com/microsoft/playwright) also shows more than **91,000 stars** at the time of writing. These numbers do not prove your framework is good, but they do prove the ecosystem is large enough to invest in seriously.

## Playwright TypeScript Framework Architecture

The architecture should be boring on purpose. I want a new engineer to open the repo and understand the layout in 10 minutes. If they need a 90-minute KT session to run one test, the framework is too clever.

### The layers

Use five layers. Keep them separate even when the project is small:

1. **Spec layer:** readable test scenarios and assertions.
2. **Page layer:** locators and page actions.
3. **Fixture layer:** reusable setup objects injected into tests.
4. **Service layer:** API helpers for setup, cleanup, and backend checks.
5. **Config layer:** environment, browser, retries, reporters, and timeouts.

This keeps your specs clean. A test should read like a user story, not like a DOM traversal exercise. If your test has 20 selectors inside it, you are leaking page details into the wrong layer.

### Reference structure

```
playwright-ts-framework/
  .github/
    workflows/
      e2e.yml
  src/
    config/
      env.ts
    fixtures/
      test.ts
    pages/
      LoginPage.ts
      DashboardPage.ts
    services/
      AuthApi.ts
      UsersApi.ts
    test-data/
      users.ts
    utils/
      random.ts
  tests/
    auth/
      login.spec.ts
    smoke/
      dashboard.spec.ts
  playwright.config.ts
  package.json
  tsconfig.json
```

If you need a refresher on the previous architecture step, read [Playwright Framework Architecture: Day 20](https://scrolltest.com/playwright-framework-architecture-day-20/). Day 21 turns that architecture into the final checklist and working skeleton.

## Project Setup and Folder Structure

Start with a clean TypeScript project. Do not mix JavaScript and TypeScript in the same learning framework unless you have a strong reason. Strict TypeScript catches boring mistakes before CI wastes 12 minutes on them.

### Install and initialize

```bash
mkdir playwright-ts-framework
cd playwright-ts-framework
npm init -y
npm i -D @playwright/test typescript ts-node dotenv
npx playwright install
npx tsc --init
```

Then add scripts to `package.json`:

```json
{
  "scripts": {
    "test": "playwright test",
    "test:headed": "playwright test --headed",
    "test:debug": "playwright test --debug",
    "test:smoke": "playwright test tests/smoke",
    "report": "playwright show-report",
    "typecheck": "tsc --noEmit"
  }
}
```

Run this before committing:

```bash
npm run typecheck
npm test
```

### Playwright config

The config file should make local execution friendly and CI execution strict. The official [Playwright CI guide](https://playwright.dev/docs/ci) recommends CI-specific behavior such as using retries in CI and workers based on the environment. Here is a practical baseline:

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 7_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    ['list']
  ],
  use: {
    baseURL: process.env.BASE_URL ?? 'https://example.test',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ]
});
```

Do not enable three browsers on day one if your suite is slow and unstable. Start with Chromium smoke coverage, stabilize the framework, then expand browser coverage where product risk justifies it.

## Fixtures, Page Objects, and Test Data

This is where a **Playwright TypeScript framework** becomes maintainable. Page objects hide page details. Fixtures inject prepared objects. Test data keeps your assertions readable.

### Page object example

Playwright has an official guide for [Page Object Models](https://playwright.dev/docs/pom). Keep your POMs small. A page object is not a dumping ground for every possible action on the page.

```typescript
import { expect, type Locator, type Page } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly email: Locator;
  readonly password: Locator;
  readonly signIn: Locator;
  readonly error: Locator;

  constructor(page: Page) {
    this.page = page;
    this.email = page.getByLabel('Email');
    this.password = page.getByLabel('Password');
    this.signIn = page.getByRole('button', { name: 'Sign in' });
    this.error = page.getByRole('alert');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(email: string, password: string) {
    await this.email.fill(email);
    await this.password.fill(password);
    await this.signIn.click();
  }

  async expectInvalidLoginMessage() {
    await expect(this.error).toContainText('Invalid email or password');
  }
}
```

### Fixture example

The official [fixture documentation](https://playwright.dev/docs/test-fixtures) explains how Playwright fixtures provide test isolation and reusable setup. In a framework, fixtures are where you expose page objects and service clients.

```typescript
import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { AuthApi } from '../services/AuthApi';

type AppFixtures = {
  loginPage: LoginPage;
  authApi: AuthApi;
};

export const test = base.extend<AppFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  authApi: async ({ request }, use) => {
    await use(new AuthApi(request));
  }
});

export { expect } from '@playwright/test';
```

### Clean spec

```typescript
import { test, expect } from '../../src/fixtures/test';
import { validUser, invalidUser } from '../../src/test-data/users';

test.describe('Login', () => {
  test('allows a valid user to sign in', async ({ loginPage, page }) => {
    await loginPage.goto();
    await loginPage.login(validUser.email, validUser.password);
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  });

  test('shows error for invalid credentials', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.login(invalidUser.email, invalidUser.password);
    await loginPage.expectInvalidLoginMessage();
  });
});
```

Notice what the spec does not contain: CSS selectors, hard waits, random email strings, or repeated setup code. That is the standard you want across the suite.

If you are still building confidence with POMs, revisit [Playwright Page Object Model: Day 5 Tutorial](https://scrolltest.com/playwright-page-object-model-day-5/) and [Playwright Fixtures and Hooks: Day 6 Tutorial](https://scrolltest.com/playwright-fixtures-hooks-day-6/).

## API Setup, Auth State, and Reliable Data

UI setup is expensive. If every test creates data through the UI, your suite will become slow and fragile. Use APIs for setup and cleanup wherever possible.

### Auth API service

```typescript
import type { APIRequestContext } from '@playwright/test';

export class AuthApi {
  constructor(private readonly request: APIRequestContext) {}

  async login(email: string, password: string) {
    const response = await this.request.post('/api/login', {
      data: { email, password }
    });

    if (!response.ok()) {
      throw new Error(`Login API failed: ${response.status()}`);
    }

    return response.json();
  }
}
```

### Storage state pattern

For stable authenticated tests, create storage state once per role and reuse it. Do not log in through the UI in every spec unless the login itself is under test.

```typescript
// tests/setup/auth.setup.ts
import { test as setup, expect } from '@playwright/test';

setup('authenticate as admin', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill(process.env.ADMIN_EMAIL!);
  await page.getByLabel('Password').fill(process.env.ADMIN_PASSWORD!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await page.context().storageState({ path: '.auth/admin.json' });
});
```

### Data rules

Use these rules for test data:

- Static data is fine for read-only reference screens.
- Generated data is better for create/update/delete workflows.
- API cleanup is better than UI cleanup.
- Never let parallel tests fight for the same user, cart, order, or tenant.
- Never commit real credentials or customer-like data.

For a deeper data plan, read [Playwright Test Data Management: Day 19](https://scrolltest.com/playwright-test-data-management-day-19/).

## CI, Traces, Screenshots, and Reports

A framework without evidence is incomplete. When CI fails, the team should not ask, “What happened?” The trace, screenshot, console logs, and report should answer that question.

### GitHub Actions workflow

```yaml
name: Playwright E2E

on:
  pull_request:
  workflow_dispatch:

jobs:
  e2e:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run typecheck
      - run: npm test
        env:
          BASE_URL: ${{ secrets.BASE_URL }}
          ADMIN_EMAIL: ${{ secrets.ADMIN_EMAIL }}
          ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 7
```

### Trace-first debugging

Playwright’s [Trace Viewer](https://playwright.dev/docs/trace-viewer) lets you inspect actions, snapshots, network calls, console logs, and source locations. This is why I prefer `trace: 'retain-on-failure'` for CI. It gives you evidence without storing huge traces for every passing test.

### CI gates

For a real team, I like these gates:

1. **Pull request smoke:** 10 to 20 critical tests, under 10 minutes.
2. **Nightly regression:** broader coverage, multiple browsers if needed.
3. **Release candidate run:** smoke plus high-risk flows with trace artifacts retained.
4. **Upgrade canary:** same smoke suite on the next Playwright version before upgrading everyone.

This keeps feedback fast. Do not run 900 end-to-end tests on every tiny pull request and then blame Playwright for slow CI. That is a pipeline design problem.

## Playwright TypeScript Framework Release Checklist

Use this **Playwright TypeScript framework** checklist before you call the capstone complete. Print it, add it to your README, or turn it into a pull request template.

### Framework readiness checklist

- **Selectors:** Prefer role, label, text, placeholder, and test id locators over brittle CSS paths.
- **Assertions:** Use web-first assertions like `toBeVisible`, `toHaveText`, and `toHaveURL`.
- **Timeouts:** No random `waitForTimeout` calls in committed tests.
- **Fixtures:** Page objects and API clients are injected through fixtures.
- **Data:** Parallel tests do not share mutable records.
- **Auth:** Storage state is used for non-login scenarios.
- **Reports:** HTML report and trace artifacts are uploaded in CI.
- **Retries:** CI retries are enabled, but retry count is tracked and reviewed.
- **Secrets:** Credentials come from CI secrets, not source control.
- **README:** A new engineer can install, run, debug, and view reports from the README.

### README template

```markdown
# Playwright TypeScript Framework

## Install
npm ci
npx playwright install

## Run tests
npm test
npm run test:smoke
npm run test:headed

## Debug
npm run test:debug
npm run report

## Environment variables
BASE_URL=
ADMIN_EMAIL=
ADMIN_PASSWORD=

## CI evidence
- HTML report: playwright-report/
- Traces: retained on failure
- Screenshots: captured on failure
```

If you publish this as a GitHub portfolio project, keep the README practical. Recruiters may not read every line, but senior engineers will scan the structure, scripts, and failure evidence.

## Common Pitfalls I Want You to Avoid

These mistakes are common because they feel productive in the first week. They become expensive in month three.

### Pitfall 1: Treating POMs like Selenium-era utility dumps

Playwright does not need a wrapper around every action. Do not create methods like `clickElement`, `fillText`, and `waitForPage` unless they add real value. Playwright already gives you strong locators, auto-waiting, and assertions.

### Pitfall 2: Hiding assertions inside page objects

Some assertions belong in page objects, especially page-specific expectations. But if every business assertion is hidden inside a method called `verifyPage`, your spec becomes vague. Keep critical assertions visible in the test when they explain the business outcome.

### Pitfall 3: Using retries as a flakiness blanket

Retries are useful in CI, but they are not a strategy. Track retry patterns. If the same test passes only on retry three times this week, fix the test or the product race condition. A green build with hidden retries is not the same as a healthy suite.

### Pitfall 4: Ignoring upgrade discipline

Playwright moves quickly. The latest GitHub release at the time of writing is [v1.61.1, published on 2026-06-23](https://github.com/microsoft/playwright/releases/tag/v1.61.1). Before upgrading your main framework, run a canary job, review traces, and pin the version in `package-lock.json`. I wrote a separate [Playwright Upgrade Checklist](https://scrolltest.com/playwright-upgrade-checklist/) if you want a release-safe process.

### Pitfall 5: No owner for failed tests

Every failed test needs an owner. Not a group chat. Not “QA team”. One person investigates, labels the failure, and closes the loop. Without ownership, flaky tests become background noise and everyone stops trusting automation.

## Key Takeaways

The **Playwright TypeScript framework** you build on Day 21 should be small enough to understand and strong enough to grow. If you remember only five things, remember these:

- Keep specs readable and move page details into page objects.
- Use fixtures to inject pages, API clients, and reusable setup.
- Use APIs and storage state to avoid slow UI setup.
- Capture traces, screenshots, videos, and HTML reports in CI.
- Review retries and flaky tests instead of pretending green means healthy.

My suggested next step: create the repo, implement two login tests, one dashboard smoke test, one API setup helper, and one GitHub Actions workflow. That is enough to show framework thinking without building a monster project.

## FAQ

### Is Playwright with TypeScript better than Playwright with JavaScript?

For a long-term framework, yes. TypeScript gives you safer refactoring, clearer fixture types, and better editor support. JavaScript is fine for quick experiments, but TypeScript is the better default for team frameworks.

### Should every test use Page Object Model?

No. Use POMs for screens and flows that repeat. For a one-off admin page with two checks, a direct test may be cleaner. The rule is simple: abstract repetition, not curiosity.

### How many tests should run in PR CI?

Start with 10 to 20 high-value smoke tests and keep the run under 10 minutes. Put broader regression in nightly or release candidate pipelines. Fast feedback wins.

### Do I need multiple browsers from day one?

No. Start with Chromium if your team is still stabilizing the framework. Add Firefox, WebKit, or mobile emulation when product risk or customer data justifies it.

### What should I show in my portfolio?

Show the folder structure, a clean spec, one page object, one fixture, the CI workflow, and the Playwright HTML report. That combination proves you understand more than syntax.



### 12. ScrollTest / Pramod Dutta — Playwright Test Data Management (TypeScript Day 19)

- Source: https://scrolltest.com/playwright-test-data-management-day-19/
- Retrieved: 2026-08-29
- Firecrawl status: complete

**Day 19:** Playwright test data management

Playwright test data management is where many good TypeScript test suites start to fail in CI. The selectors are clean, the assertions are solid, the sharding is fast, but two tests fight for the same user, order, or feature flag and the team calls it “flaky automation.”

I treat test data as part of the framework, not as a spreadsheet someone updates on Friday night. In this tutorial, you will build a practical pattern with typed factories, API seeding, per-worker isolation, cleanup, and trace-friendly names that make failures easy to debug.

## Why Playwright Test Data Breaks in CI

Most teams start with one shared test account. It works for the first ten tests. Then the suite grows, runs in parallel, and suddenly the same account has three carts, two password resets, and one partially created profile.

Playwright itself is not the problem. The official [authentication guide](https://playwright.dev/docs/auth) explains that Playwright runs tests in isolated browser contexts and can load saved authenticated state. That solves browser isolation. It does not solve database or backend state isolation.

The same gap shows up after CI sharding. In [Day 18 on Playwright CI sharding](https://scrolltest.com/playwright-ci-sharding-typescript-day-18/), we split tests across machines to reduce feedback time. That makes data collisions more visible because multiple workers hit the same environment at the same time.

### The real failure pattern

I see this sequence again and again:

1. A test logs in as `qa_user@example.com`.
2. Another test updates the same user’s address.
3. A third test expects a clean dashboard.
4. CI runs all three in parallel.
5. One assertion fails and the error message points to the UI, not the data.

The fix is not “add retry.” Retry only hides the timing. The fix is to create deterministic data ownership for every test or every worker.

### Useful numbers for context

During this run, the [Microsoft Playwright GitHub repository](https://github.com/microsoft/playwright) had 91,719 stars, and the npm downloads API reported 168,373,938 downloads for [@playwright/test](https://www.npmjs.com/package/@playwright/test) in the last month. That adoption means more teams are using Playwright for serious CI suites, not only local smoke scripts.

When the suite becomes serious, test data must become serious too.

## The Data Strategy I Use in Real Projects

My default Playwright test data management strategy has four rules:

- **Generate unique data:** every test run gets a unique prefix.
- **Seed through stable APIs:** use UI only for the behavior you are testing.
- **Type the data:** TypeScript should catch missing fields before CI does.
- **Clean carefully:** delete what you created, but keep evidence when a test fails.

This sits well with Playwright’s own [API testing support](https://playwright.dev/docs/api-testing). You can use `request` fixtures to create setup data before a UI flow starts. That keeps tests faster and easier to read.

### What should be static?

Some data should stay static. A country list, a payment method enum, a role name, or a feature flag key can live in constants. These values describe the system, not a test case.

```ts
// tests/support/constants.ts
export const Roles = {
  admin: 'ADMIN',
  buyer: 'BUYER',
  support: 'SUPPORT',
} as const;

export const Countries = {
  india: 'IN',
  unitedStates: 'US',
} as const;
```

### What should be generated?

Users, orders, carts, tickets, invoices, and comments should usually be generated. If the object can be created, updated, cancelled, or deleted by the test, it should not be shared across the suite.

A simple naming convention helps in screenshots and traces:

```ts
const runId = process.env.GITHUB_RUN_ID ?? Date.now().toString();
const testPrefix = `pw-${runId}`;
```

When a failed trace shows an order named `pw-985233-order-returns-01`, I know exactly which CI run created it.

## Create Typed Test Data Factories

A factory is a small function that returns valid test data with sensible defaults. It should be boring. Boring factories are a good sign because the test reads like the business flow.

Create this folder structure:

```text
tests/
  support/
    data/
      user.factory.ts
      order.factory.ts
      ids.ts
  e2e/
    checkout.spec.ts
```

### Start with a unique ID helper

I prefer a tiny helper over random strings scattered across tests.

```ts
// tests/support/data/ids.ts
export function uniqueId(label: string): string {
  const runId = process.env.GITHUB_RUN_ID ?? 'local';
  const worker = process.env.TEST_WORKER_INDEX ?? '0';
  const stamp = Date.now().toString(36);
  return `pw-${runId}-w${worker}-${label}-${stamp}`;
}
```

This gives you three things: the run, the worker, and the object purpose. That matters when cleanup fails and you need to inspect backend records.

### Create a typed user factory

Now define the shape of the data and a factory function.

```ts
// tests/support/data/user.factory.ts
import { uniqueId } from './ids';

export type TestUser = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'BUYER' | 'ADMIN';
};

export function buildUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = uniqueId('user');

  return {
    email: `${id}@example.test`,
    password: 'Passw0rd!123',
    firstName: 'Playwright',
    lastName: id,
    role: 'BUYER',
    ...overrides,
  };
}
```

The test can override only the part it cares about. Everything else stays valid by default.

### Create an order factory

Factories become more useful when they model relationships. An order belongs to a user. Make that clear in the type.

```ts
// tests/support/data/order.factory.ts
import { uniqueId } from './ids';

export type TestOrder = {
  externalId: string;
  sku: string;
  quantity: number;
  buyerEmail: string;
};

export function buildOrder(buyerEmail: string, overrides: Partial<TestOrder> = {}): TestOrder {
  return {
    externalId: uniqueId('order'),
    sku: 'PW-COURSE-001',
    quantity: 1,
    buyerEmail,
    ...overrides,
  };
}
```

The important rule: factories create data objects; clients persist them. Do not mix both too early. Keeping those responsibilities separate makes tests easier to debug.

## Seed Data Through APIs Before UI Tests

UI setup is expensive. If a checkout test needs an existing user and a cart, create them through APIs and use the browser for the checkout behavior. This keeps the test focused.

Playwright exposes an `APIRequestContext` for API calls. The official API testing docs show the same core idea: use Playwright to send HTTP requests, validate responses, and share state with browser tests when needed.

### Build a small API client

Do not place raw request calls everywhere. Wrap them.

```ts
// tests/support/api/test-data.client.ts
import { APIRequestContext, expect } from '@playwright/test';
import { TestUser } from '../data/user.factory';
import { TestOrder } from '../data/order.factory';

export class TestDataClient {
  constructor(private readonly request: APIRequestContext) {}

  async createUser(user: TestUser): Promise<{ id: string; email: string }> {
    const response = await this.request.post('/api/test/users', {
      data: user,
    });

    expect(response.ok()).toBeTruthy();
    return response.json();
  }

  async createOrder(order: TestOrder): Promise<{ id: string; externalId: string }> {
    const response = await this.request.post('/api/test/orders', {
      data: order,
    });

    expect(response.ok()).toBeTruthy();
    return response.json();
  }

  async deleteUser(userId: string): Promise<void> {
    const response = await this.request.delete(`/api/test/users/${userId}`);
    expect([200, 204, 404]).toContain(response.status());
  }
}
```

Yes, these are test-only endpoints. In product companies, I push for safe internal endpoints guarded by environment, network, or auth. If you do not have those endpoints, use existing public APIs, direct database setup through a service, or a lightweight seed command. Pick one path and standardize it.

### Use it inside a test

Here is the clean version of a checkout precondition:

```ts
// tests/e2e/checkout.spec.ts
import { test, expect } from '@playwright/test';
import { TestDataClient } from '../support/api/test-data.client';
import { buildUser } from '../support/data/user.factory';
import { buildOrder } from '../support/data/order.factory';

test('buyer can pay for an existing order', async ({ page, request }) => {
  const dataClient = new TestDataClient(request);
  const user = buildUser();
  const createdUser = await dataClient.createUser(user);
  const order = buildOrder(user.email);
  const createdOrder = await dataClient.createOrder(order);

  await page.goto(`/login?email=${encodeURIComponent(user.email)}`);
  await page.getByLabel('Password').fill(user.password);
  await page.getByRole('button', { name: 'Sign in' }).click();

  await page.goto(`/orders/${createdOrder.externalId}`);
  await page.getByRole('button', { name: 'Pay now' }).click();
  await expect(page.getByText('Payment successful')).toBeVisible();

  await test.info().attach('test-data', {
    body: JSON.stringify({ createdUser, createdOrder }, null, 2),
    contentType: 'application/json',
  });
});
```

The attachment is useful. In the HTML report and trace workflow, the team can see exactly which data was created. If you followed [Day 16 on Playwright reports](https://scrolltest.com/playwright-reports-day-16/), this fits neatly into the evidence pack.

## Use Worker Isolation for Parallel Runs

The [Playwright fixtures documentation](https://playwright.dev/docs/test-fixtures) says fixtures are isolated between tests and can provide everything a test needs. That is the right mental model for data too. If tests run in parallel, the data owner must be clear.

### Understand the worker problem

When you run four workers, four tests can create or update records at the same time. If each test uses a unique user, you are safe. If every test uses `automation@example.com`, you are waiting for a failure.

Use worker information in your data prefix. Playwright exposes worker indexes through test info, but you can also pass a worker index from setup code.

```ts
import { test as base } from '@playwright/test';
import { TestDataClient } from './api/test-data.client';
import { buildUser, TestUser } from './data/user.factory';

type Fixtures = {
  testUser: TestUser;
  dataClient: TestDataClient;
};

export const test = base.extend<Fixtures>({
  dataClient: async ({ request }, use) => {
    await use(new TestDataClient(request));
  },

  testUser: async ({ dataClient }, use, testInfo) => {
    const user = buildUser({
      lastName: `worker-${testInfo.workerIndex}`,
    });

    const created = await dataClient.createUser(user);
    await use(user);

    if (testInfo.status === testInfo.expectedStatus) {
      await dataClient.deleteUser(created.id);
    }
  },
});

export { expect } from '@playwright/test';
```

Now your spec imports from your fixture file instead of directly from `@playwright/test`.

```ts
import { test, expect } from '../support/fixtures';

test('profile page shows generated buyer name', async ({ page, testUser }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill(testUser.email);
  await page.getByLabel('Password').fill(testUser.password);
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByText(testUser.lastName)).toBeVisible();
});
```

### When to use project-level data

Sometimes you need a larger setup: one tenant, three roles, a paid subscription, and a default catalog. In that case, use project dependencies or global setup. The Playwright docs cover [global setup and teardown](https://playwright.dev/docs/test-global-setup-teardown), but I use it carefully.

Global setup is good for slow, stable, read-mostly data. It is risky for data that tests mutate. If five tests edit the same tenant, your setup is shared state with a nicer name.

## Cleanup Without Hiding Bugs

Cleanup is not just deletion. Cleanup is a policy. If a test passes, delete the data. If a test fails, consider keeping the data for investigation and attach the IDs to the report.

### Use status-aware cleanup

The fixture example above deletes the user only when the test status matches the expected status. That keeps failed-test data alive long enough to inspect. In a nightly cleanup job, delete old records with the `pw-` prefix older than 24 or 48 hours.

```ts
// scripts/cleanup-test-data.ts
import { request } from '@playwright/test';

async function main() {
  const api = await request.newContext({
    baseURL: process.env.BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${process.env.TEST_DATA_TOKEN}`,
    },
  });

  const response = await api.delete('/api/test/cleanup', {
    data: {
      prefix: 'pw-',
      olderThanHours: 48,
    },
  });

  if (!response.ok()) {
    throw new Error(`Cleanup failed: ${response.status()}`);
  }

  await api.dispose();
}

main();
```

### Do not clean the wrong environment

This sounds obvious until someone points the cleanup token at staging and deletes a record used by a manual QA run. Add a hard environment guard.

```ts
if (!process.env.BASE_URL?.includes('test') && !process.env.BASE_URL?.includes('staging')) {
  throw new Error(`Refusing to cleanup unsafe BASE_URL: ${process.env.BASE_URL}`);
}
```

In India-based service teams, I often see a shared QA environment used by automation, manual QA, BA demos, and client validation. If that is your reality, make the prefix visible and negotiate an automation namespace. Do not let your tests silently corrupt someone else’s demo data.

## Screenshot and Trace Evidence

Data bugs are easier to debug when the evidence names the data. A screenshot of “Payment failed” is weaker than a screenshot plus an attached JSON payload showing the user, order, and run ID.

### What screenshots should show

For this tutorial, I would capture these screenshots while recording the lesson:

- **Screenshot 1:** VS Code showing `user.factory.ts` and `ids.ts` side by side.
- **Screenshot 2:** Playwright HTML report attachment named `test-data` with the created user and order IDs.
- **Screenshot 3:** Trace Viewer network tab showing the seed API call before the UI flow starts.
- **Screenshot 4:** CI logs showing a unique `pw-<run>-w<worker>` prefix for each worker.

If you already followed [Day 7 on Trace Viewer](https://scrolltest.com/playwright-trace-viewer/), connect the same debugging habit here. Trace is not only for click problems. It is also a data audit trail.

### Add annotations for test data

Small annotations make report scanning easier.

```ts
test('buyer can pay for an existing order', async ({ page, request }, testInfo) => {
  const runId = process.env.GITHUB_RUN_ID ?? 'local';
  testInfo.annotations.push({ type: 'run-id', description: runId });
  testInfo.annotations.push({ type: 'data-owner', description: `worker-${testInfo.workerIndex}` });

  // test body...
});
```

This is useful for managers too. When a CI failure lands in Slack, the first question is not “who touched this?” It becomes “which generated data did this worker own?”

## Common Pitfalls

Here are the mistakes I would actively prevent during framework review.

### Pitfall 1: One login for every test

Shared login is easy to start and painful to scale. Use saved authentication state for speed, but keep the backend data isolated. Authentication state is not a replacement for test data design.

### Pitfall 2: Random data without traceability

Pure random strings create mystery. A value like `a8x92s` tells you nothing. A value like `pw-123456-w2-order-return` tells you the run, worker, and purpose.

### Pitfall 3: UI setup for everything

If every test creates data through the UI, the suite becomes slow and noisy. Use APIs for setup. Use UI for the behavior under test.

### Pitfall 4: Cleanup in the middle of debugging

Automatic cleanup after failure can remove the only evidence you need. Keep failed-test records for a short window, attach IDs, and clean them with a scheduled job.

### Pitfall 5: Secrets in factories

Factories should not contain real tokens, real customer emails, or production-like passwords copied from internal docs. Use safe test domains like `example.test` and inject secrets through CI variables.

## Key Takeaways

Playwright test data management is not extra polish. It is the difference between a suite your team trusts and a suite everyone reruns until it turns green.

- Use typed factories for users, orders, and other mutable objects.
- Seed data through APIs when the UI flow is not the thing being tested.
- Include run ID and worker ID in generated data names.
- Attach data IDs to reports and traces for faster debugging.
- Clean passed-test data immediately, but preserve failed-test data briefly.

For Day 20, the natural next step is building a production-ready framework structure: folders, fixtures, API clients, page objects, tags, reports, and CI scripts that a real team can maintain.

## FAQ

### Should Playwright tests use production data?

No. Do not use production customer data in automation. Use safe test environments, synthetic records, masked data, or test-only seed APIs.

### Is global setup better than per-test data?

Global setup is useful for stable data that tests only read. For mutable data, per-test or per-worker generation is safer.

### Should I delete test data after every run?

Delete passed-test data quickly. Keep failed-test data for a short debugging window, then delete it with a scheduled cleanup job.

### Can I use Faker with Playwright and TypeScript?

Yes, but wrap it inside factories. Do not call random data helpers directly from every spec. You want controlled uniqueness, not chaos.

### What is the best first step for an existing flaky suite?

Find the top ten tests using shared users or shared orders. Convert those to generated data with a visible prefix. That usually removes a surprising number of CI-only failures.



### 13. Anton Gulin (anton.qa) — Reuse One Page Object Method for Success and Failure Cases

- Source: https://www.anton.qa/blog/posts/reuse-page-object-method-success-and-failure
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Reuse One Page Object Method for Success and Failure Cases

Your login helper exists twice: one that expects success, one that expects an error. Here is the options-object pattern that keeps one method, with Playwright code.

![Dark code card: two nearly identical login helpers collapse into one login method with a waitFor option.](https://cdn.sanity.io/images/9tez6xmw/production/de0e5b53a25f11868088e2944014af42e3575b4a-1200x630.png?w=1200&h=630&fm=webp&q=80)

Your login helper exists twice. One version expects the dashboard. One version expects the error. Ninety percent of the two bodies is the same code. Here is the short answer. **Keep one method per user action.** Let an options object say what it waits for. The test still decides what the result means.

Last week I wrote about [where test assertions belong](https://www.anton.qa/blog/posts/where-test-assertions-belong). A reader came back with the harder version of the question. If business checks live in the test, how does one method serve both cases? Good question. This post is the answer.

## The duplicate everyone has

Open any page object that is more than a year old. You will find this:

```ts
async login(user: string, pass: string) {
  await this.username.fill(user);
  await this.password.fill(pass);
  await this.submit.click();
  await this.page.waitForURL('**/dashboard');
}

async loginExpectingError(user: string, pass: string) {
  await this.username.fill(user);
  await this.password.fill(pass);
  await this.submit.click();
  await this.errorBanner.waitFor();
}
```

Two methods. Three identical lines each. One real difference: what the method waits for at the end.

Now the login form changes. Someone adds a "remember me" checkbox. You fix the first method. You forget the second. The negative test starts failing for a reason unrelated to the negative case.

That is the cost of the duplicate. Not ugliness. Drift.

## Why the boolean flag makes it worse

The first fix everyone tries is a flag:

```ts
async login(user: string, pass: string, success: boolean) { ... }
```

Then the test reads like this:

```ts
await loginPage.login('anton', 'wrong-password', false);
```

False what? You have to open the page object to find out. And flags multiply. Six months later the same method takes three of them, and nobody can read the call site at all:

```ts
await loginPage.login('anton', 'pw', false, true, false);
```

A flag saves a method and costs a reader. Bad trade.

## Copy the shape Playwright already uses

Playwright's own API solves this everywhere. Every action takes an optional options object with named keys:

```ts
await page.getByRole('button').click({ timeout: 5000, force: true });
```

You never pass a bare `true` to Playwright. You pass a name and a value. Do the same in your page objects:

```ts
type LoginOptions = { waitFor?: 'dashboard' | 'error' };

async login(user: string, pass: string, options: LoginOptions = {}) {
  const { waitFor = 'dashboard' } = options;
  await this.username.fill(user);
  await this.password.fill(pass);
  await this.submit.click();
  if (waitFor === 'dashboard') {
    await this.page.waitForURL('**/dashboard');
  } else {
    await this.errorBanner.waitFor();
  }
}
```

Two things make this work. The default keeps the common call short. The named key makes the rare call readable:

```ts
// happy path, unchanged
await loginPage.login('anton', 'correct-password');

// negative path, and you can read it without opening the class
await loginPage.login('anton', 'wrong-password', { waitFor: 'error' });
```

One method. One place to fix when the form changes.

## Notice what the option does not say

The option is called `waitFor`, not `expectSuccess`. That is deliberate.

`expectSuccess: false` puts a judgment inside the page object. The class starts deciding what a correct login looks like. That breaks [the rule from last week](https://www.anton.qa/blog/posts/where-test-assertions-belong): business checks live in the test.

`waitFor: 'error'` only says which element the method should wait for before it returns. It is a timing instruction, not a verdict. The verdict stays where a reader expects it:

```ts
test('rejects a wrong password', async ({ loginPage }) => {
  await loginPage.login('anton', 'wrong-password', { waitFor: 'error' });
  await expect(loginPage.errorBanner).toHaveText('Wrong password');
});
```

Read that test aloud. It says what it does and what should be true. You never open the page object.

## When two methods really are right

Sometimes the duplicate is not a duplicate. Keep separate methods when the steps themselves differ:

```ts
async loginWithPassword(user: string, pass: string) { ... }
async loginWithSso(user: string) { ... }
```

Different fields, different clicks, different waits. These are two user actions that happen to end in the same place.

The test: if the bodies differ by more than the final wait, keep them apart. If they differ only by the final wait, merge them.

And name methods after the action, never after the outcome. `loginExpectingError` names a result. `login` names what the user does. Results belong in the test name.

## The rule, in three lines

1. One method per user action.
2. An options object says what it waits for, with a default for the common case.
3. The test says what the result means.

Apply it once to your login helper. Then look at your checkout helper, your search helper, and your upload helper. The same pair is usually hiding in all of them.



### 14. Anton Gulin (anton.qa) — Playwright v1.60 Turns Test Failures Into Evidence

- Source: https://www.anton.qa/blog/posts/playwright-v1-60-evidence-first-testing
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Playwright v1.60 Turns Test Failures Into Evidence

![Playwright v1.60 Turns Test Failures Into Evidence](https://cdn.sanity.io/images/9tez6xmw/production/9f6dcb118644a5f341a68205346f296043365f9b-1200x630.png?w=1200&h=630&fm=webp&q=80)

Playwright v1.60 adds scoped HAR recording, locator.drop(), ARIA boxes, and test.abort(). The release helps QA teams capture proof during the test run, not after the failure.

## TL;DR

Playwright v1.60 makes failure evidence easier to capture during the run.

The main change is scoped HAR recording.

HAR means network request file.

It shows what the browser sent and received.

The release also adds file drops, ARIA boxes, and hard test aborts.

ARIA means accessibility map.

Together, these changes help CI failures explain themselves.

CI means automated build server.

## The Release

Playwright v1.60 shipped on May 11, 2026.

The headline change is `context.tracing.startHar()`.

Tracing means run evidence capture.

Before v1.60, HAR capture lived outside that tracing flow.

Now HAR recording sits next to other test proof.

The API also returns a `Disposable`.

A disposable is a cleanup handle.

You can use `await using` to close the HAR automatically.

That matters when many tests run at once.

Manual cleanup breaks easily.

One missed cleanup step can leave broken evidence behind.

## Why This Matters For QA Teams

Most teams collect proof after a test fails.

That is too late.

The failing run is already gone.

The next run may pass.

Now the team has a guess, not proof.

Playwright v1.60 moves more proof into the first run.

That is the architectural part.

You are not asking a person to rerun the test.

You are designing the run to bring receipts.

That matters even more for AI testing.

AI means software that predicts.

Predictions need clear input.

A trace, a HAR file, and an ARIA snapshot give that input.

Without proof, AI just writes a confident guess.

## How To Use Scoped HAR Recording

This example records network evidence for an upload test.

It also uses the new Drop API.

Drop API means file drop simulation.

```ts
import { test, expect } from '@playwright/test';

test('upload records network evidence', async ({ context }) => {
  await using har = await context.tracing.startHar('upload.har', {
    content: 'embed',
    mode: 'minimal',
    urlFilter: /\/api\/upload/,
  });

  const page = await context.newPage();
  await page.goto('/upload');

  await page.locator('#dropzone').drop({
    files: {
      name: 'note.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('hello'),
    },
  });

  await expect(page.getByText('Upload complete')).toBeVisible();
});
```

The HAR starts before the page opens.

The `urlFilter` keeps the capture focused.

The drop step sends an in-memory file.

When the test scope ends, Playwright finalizes the HAR.

No extra `afterEach` block is needed.

No custom `try/finally` block is needed.

That is the small win.

The bigger win is trust.

When upload fails, the HAR file shows the request.

Your team can inspect the failed run.

They do not need to recreate it from memory.

## The Hidden Updates Worth Watching

The release is bigger than HAR.

Five smaller changes point in the same direction.

First, `locator.drop()` tests real upload zones better.

It accepts files, text, HTML, or URI data.

URI means web address.

Playwright sends `dragenter`, `dragover`, and `drop` events.

If the app rejects the drop, the method throws.

That is useful.

It tells you the app never accepted the file.

Second, ARIA snapshots can include boxes.

Boxes mean element positions.

The format is `[box=x,y,width,height]`.

Coordinates are viewport-relative CSS pixels.

Viewport means visible browser area.

This gives AI tools a cleaner page map.

They can see what exists and where it sits.

Third, page-level ARIA assertions now exist.

You can assert the page body directly.

That removes one small wrapper step.

Small wrapper steps matter in generated tests.

Fourth, `test.abort()` stops bad runs early.

It works inside fixtures, hooks, and route handlers.

Fixtures mean shared test setup.

Use it when the test breaks a safety rule.

For example, a test might publish to a shared page.

That run should stop right away.

Fifth, BrowserContext now mirrors page events.

BrowserContext means a browser sandbox.

Framework owners can listen once per context.

They do not need listeners on every page.

That helps when one test opens many tabs.

## The Gotcha Nobody Is Talking About

Only one HAR recording can run per BrowserContext.

That is not a problem.

But it is a design rule.

Do not start one HAR for login and another for upload.

Use separate contexts when you need separate captures.

Or keep one capture narrow with `urlFilter`.

The Drop API has another useful edge.

If `dragover` does not call `preventDefault()`, Playwright throws.

That sounds annoying.

It is usually the bug.

The app rejected the drop before the file arrived.

ARIA boxes have one more boundary.

They use viewport coordinates, not full-page coordinates.

If your test scrolls, account for that.

If your page uses frames, account for that too.

## Migration Notes

Playwright v1.60 removes several old APIs.

Check these before upgrading a large suite.

- Replace `Locator.ariaRef()` with `locator.ariaSnapshot()`.
- Remove the `handle` option from `exposeBinding()` calls.
- Remove the `logger` option from browser connection calls.
- Replace `videosPath` and `videoSize` with `recordVideo`.

There is also a CDP update.

CDP means browser control protocol.

`connectOverCDP()` now accepts `noDefaults`.

That matters when attaching to an existing Chromium browser.

Chromium means Chrome browser engine.

With `noDefaults: true`, Playwright leaves default context settings alone.

## What This Changes In CI

This release does not make tests smarter by itself.

It makes test runs easier to review.

That is the part I care about.

Good QA architecture is not just more tests.

It is better evidence from each run.

For a small suite, v1.60 is a nice upgrade.

For a large CI suite, it is more important.

You can scope network captures.

You can test upload zones without custom browser hacks.

You can give AI tools page structure and element positions.

You can stop unsafe tests before they poison shared state.

That is not hype.

That is how test systems become reviewable.

## Verdict

I would upgrade Playwright test projects to v1.60 this week.

Start with the evidence APIs.

Add `startHar()` where network failures waste review time.

Add `locator.drop()` where upload tests use custom events.

Add ARIA boxes where AI tools inspect pages.

Then review the breaking removals before merging.

The release is not about one huge feature.

It is about a better proof layer.

That is what AI QA architecture needs.

**Official sources:**

- [Playwright v1.60.0 release notes](https://github.com/microsoft/playwright/releases/tag/v1.60.0)
- [`tracing.startHar()` docs](https://playwright.dev/docs/api/class-tracing#tracing-start-har)
- [`locator.drop()` docs](https://playwright.dev/docs/api/class-locator#locator-drop)
- [`ariaSnapshot({ boxes: true })` docs](https://playwright.dev/docs/api/class-page#page-aria-snapshot)



### 15. Anton Gulin (anton.qa) — Playwright Passkey Testing: How to Test WebAuthn Login (2026)

- Source: https://www.anton.qa/blog/posts/test-passkey-login-playwright
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Playwright Passkey Testing: How to Test WebAuthn Login (2026)

Test passkey and WebAuthn login in Playwright with no hardware key. A complete working test using the virtual authenticator, in Chromium, Firefox and WebKit.

![Dark code-style card reading 'Test passkey login in Playwright' — no hardware key, every browser.](https://cdn.sanity.io/images/9tez6xmw/production/c3809f6e84585d9fa5727233c5d0ad02f1b68b15-1200x630.png?w=1200&h=630&fm=webp&q=80)

## Key takeaways

You can now test passkey login in Playwright with no hardware key. Playwright 1.61 added a virtual authenticator (a fake security key). Your test seeds a passkey, turns it on, and the page signs in as if a real key answered. It works in every browser and runs in CI. The API is `browserContext.credentials`, with three methods: `create()`, `install()`, and `get()`.

## What is a passkey, in plain words

A passkey is a login with no password. You sign in with your face, your fingerprint, or a device PIN. The hard part lives in your device. The website only sees a signed reply.

The browser standard behind this is called WebAuthn. It means "web authentication". When you log in, the browser runs a small back-and-forth with the site. The site asks. Your device answers and signs. This is the part that used to need real hardware.

Apple, Google, and most banks ship passkeys now. If your app has a "sign in with a passkey" button, you have this flow in production today.

## Why nobody tested this flow

Here is the part nobody talks about. Almost nobody tests the passkey login.

For years it was hard. To test a passkey you needed a real security key plugged into the machine. You cannot plug a USB key into a CI server. CI is a remote build machine with no hands and no ports.

So the most important login flow shipped untested. The one thing a user does first. The one thing that locks them out if it breaks.

I test software for a living. An untested login is the scariest gap on the list. If sign-in breaks, nothing else matters. The cart, the dashboard, the settings page, all of it sits behind the door. On one project I watched a broken auth path block every other test for two days. The fix took ten minutes. Finding it took the two days.

Playwright 1.61 closed this gap.

## How Playwright tests WebAuthn: the virtual authenticator

Playwright added a virtual authenticator (a fake security key) in version 1.61, which shipped on June 15, 2026. It is still how this works on the current 1.62.1.

A virtual authenticator is software that pretends to be a hardware key. Your test creates one. It seeds a passkey into it. From then on, when the page calls the browser to sign in, Playwright answers for the key. No real device. No USB port. The page cannot tell the difference.

You reach it through a new class called `Credentials`, on the browser context: `browserContext.credentials`. It has three methods you will use most:

- `create()` seeds a test passkey for a site.
- `install()` turns the virtual key on for the page.
- `get()` reads back any passkey the page registered, so you can save it and reuse it later.

This works in all three browser engines Playwright drives. So one test covers Chromium, Firefox, and WebKit.

## How to test passkey login in Playwright: a working example

Here is a complete test. It seeds a passkey, turns on the virtual key, then signs in. Read the comments for what each step does.

```ts
// passkey-login.spec.ts
// Tested against Playwright 1.61. Run with: npx playwright test
import { test, expect } from '@playwright/test';

test('user signs in with a passkey', async ({ browser }) => {
  // A fresh, clean browser session for this test.
  const context = await browser.newContext();

  // STEP 1: Seed a passkey for our site.
  // 'example.com' is the site domain (the "relying party id").
  // With only the domain, Playwright makes a fresh key for us.
  await context.credentials.create('example.com');

  // STEP 2: Turn the virtual key on.
  // From now on, the page's sign-in calls are answered by our key,
  // not by real hardware. Call this before the page loads.
  await context.credentials.install();

  // STEP 3: Let the page use it.
  const page = await context.newPage();
  await page.goto('https://example.com/login');

  // The page calls the browser to sign in. Our key answers.
  await page.getByRole('button', { name: 'Sign in with a passkey' }).click();

  // Check the user is in.
  await expect(page.getByText('Welcome back')).toBeVisible();
});
```

The three steps map to the three method calls. First you `create()` a passkey for your site. Then you `install()` the virtual key, which makes the page's sign-in calls run through it. Then the page does its normal login, and your key answers in place of hardware.

One note on order. Call `install()` before the page touches sign-in. The virtual key only answers calls that happen after you turn it on.

## Re-using a passkey across tests

Often you want to register a passkey once, then reuse it in many tests. A passkey holds a private key (a secret only your device knows). You can read that secret back with `get()` and seed it into a later test.

```ts
// In a setup test: register once, then read the passkey back.
const created = await context.credentials.get({ rpId: 'example.com' });
// `created` holds the passkey fields, including its keys.
// Save them, then seed an identical passkey in a later test:
await otherContext.credentials.create('example.com', {
  id: created[0].id,
  userHandle: created[0].userHandle,
  privateKey: created[0].privateKey,
  publicKey: created[0].publicKey,
});
```

This is how you keep one stable test user across a whole suite. You do not re-register on every test. You seed the same passkey each run. See the [Credentials docs](https://playwright.dev/docs/api/class-credentials) for the full field list.

## The honest part: what this does not test

A virtual key is not a real key. So this approach tests your login flow, not the physical hardware. It will not catch a bug in a specific phone's secure chip or a real fingerprint reader. It tests the part you own: the page, the back-and-forth, the server check.

For most teams that is the right line. The browser and the operating system test the hardware path for you. Your job is to test that your app asks the right question and trusts the right answer. That is exactly what the virtual key lets you do, in CI, on every push.

## Why this matters for your CI pipeline

Before 1.61, your passkey login had two test options. Skip it, or test it by hand. Both are bad. A skipped test means a silent break. A by-hand test runs once a release, not once a push.

Now it runs like any other test. It sits in your suite. It runs on every pull request. If someone changes the login and breaks the passkey path, the build goes red before the change ships. That is the whole point of a test. Catch the break in seconds, not from an angry user.

If your app supports passkeys, this is the test you write this week. The excuse is gone. The login everyone ships and nobody verifies is now testable.

**Official sources:**

- [Playwright v1.61.0 release notes](https://github.com/microsoft/playwright/releases/tag/v1.61.0)
- [Playwright Credentials API docs](https://playwright.dev/docs/api/class-credentials)



### 16. Sajith Dilshan (Medium) — Fixture vs Lazy Object Creation in Playwright: Avoiding Hidden Performance Traps

- Source: https://medium.com/@sajith-dilshan/fixture-vs-lazy-object-creation-in-playwright-avoiding-hidden-performance-traps-b147673ef900
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Fixture vs Lazy Object Creation in Playwright: Avoiding Hidden Performance Traps

![](https://miro.medium.com/v2/resize:fit:700/1*Pjxy6TO0BOhDB2Tivd3y1g.png)

Fixture vs Lazy Object Creation in Playwright

Modern test automation is not just about writing assertions — it's about building a system that scales. One of the most overlooked design decisions in Playwright automation is **how and when test objects are created**.

Two patterns dominate this space:

- **Fixture-based (Eager) object creation**
- **Lazy object creation**

Both are useful. Both are recommended in different situations. But misunderstanding how fixtures work can quietly slow down your test suite and waste resources.

This article explains the difference, the real problems caused by misuse, and how Playwright actually expects fixtures to be used.

## What Is the Fixture Approach?

A **fixture** in Playwright is a mechanism that prepares dependencies for a test **before execution begins**. Fixtures are injected into tests as parameters and are available throughout the test lifecycle.

## Core behavior

> _If a fixture is listed in the test parameters, Playwright creates it before the test body runs._

## Example (Fixture-Based Test)

```ts
test("Validate order history", async ({ ordersPage, profilePage, supportPage }) => {
  await ordersPage.open();
  await ordersPage.verifyLatestOrder();
});
```

Even though the test only interacts with `ordersPage`, **all three page objects are created upfront**.

This is known as **eager initialization**.

## What Is Lazy Object Creation?

The **Lazy approach** delays object creation until it is actually required by the test. Instead of injecting many dependencies upfront, objects are created inside the test flow as needed.

## Core behavior

> _Objects exist only if the test explicitly creates them._

## Example (Lazy Creation)

```ts
test("Validate order history", async ({ page }) => {
  const ordersPage = new OrdersPage(page);
  await ordersPage.open();
  await ordersPage.verifyLatestOrder();
});
```

Here:

- Only one object is created
- No unnecessary setup runs
- The test stays lean and focused

## Key Difference: Eager vs Lazy

| Aspect | Fixture (Eager) | Lazy |
| --- | --- | --- |
| Object creation timing | Before test execution | During test execution |
| Unused objects | Still created | Never created |
| Memory usage | Can increase | Minimal |
| Startup cost | Higher with many fixtures | Lower |
| Scalability | Risky if overused | More predictable |

## The Real Problems with the Fixture Approach

The fixture approach introduces issues **only when overused**.

## 1. All Objects Are Created Upfront

When a test declares multiple fixtures, Playwright creates **every one of them**, even if the test uses only one.

Execution flow:

```text
Test starts
 ↓
ordersPage created
 ↓
profilePage created
 ↓
supportPage created
 ↓
Test logic executes
```

This behavior is correct — but not always efficient.

## 2. Higher Memory Usage and Setup Time

Page objects often contain:

- Locators
- Helper methods
- Navigation logic
- Hooks or listeners

Creating unused objects means:

- More memory consumption
- Slower test startup
- Extra work the test never needed

In large test suites, this overhead compounds quickly and becomes a real performance bottleneck.

## Is This Understanding Correct?

Yes — this understanding is **technically accurate**.

## What's correct

- Playwright fixtures are **eagerly initialized**
- Any fixture listed in test parameters is created **before the test body**
- Unused fixtures still consume setup time and memory
- The impact grows in large suites with heavy page objects

If you think of it like a café ☕

Making every drink on the menu at opening time is clearly inefficient.

## The Important Nuance Most Teams Miss

The issue is **not fixtures themselves**.

The real problem is **injecting fixtures that the test does not need**.

Playwright does **not** force all fixtures to be created for every test.

## Example (Selective Injection)

```ts
test("Validate order history", async ({ ordersPage }) => {
  await ordersPage.open();
});
```

In this case:

- ✔ `ordersPage` → created
- ❌ `profilePage` → not created
- ❌ `supportPage` → not created

Fixtures are **eager per test parameter**, not globally eager.

This distinction is critical.

## What Does Playwright Actually Recommend?

Playwright officially encourages fixtures because they provide:

- Strong test isolation
- Reusability
- Clear dependency management
- More readable and maintainable tests

However, Playwright also stresses a simple but important rule:

> **_Only depend on fixtures that your test actually needs._**

Which leads to a key conclusion:

- ❌ Fixtures are bad → Wrong
- ❌ Lazy creation must replace fixtures → Wrong
- ✅ Over-injecting fixtures is bad → Correct

## Where Lazy Object Creation Fits In

Playwright does not explicitly call this "lazy loading," but its recommended patterns support it naturally.

## Recommended Hybrid Strategy

#### 1. Use fixtures for core dependencies

```ts
test("Checkout flow", async ({ page }) => {
  const cartPage = new CartPage(page);
  await cartPage.addItem();
});
```

Here, Playwright manages the browser lifecycle, while page objects are created only when needed.

#### 2. Use factory-style fixtures

```ts
pageFactory: async ({ page }, use) => {
  await use({
    cart: () => new CartPage(page),
    payment: () => new PaymentPage(page),
  });
};
```

Objects are instantiated **only when the function is called**.

## When the Fixture Approach Is the Right Choice 👍

Use fixtures when:

- The setup is shared (auth, context, API clients)
- Objects are lightweight
- Dependencies are used across many tests
- You want consistent and explicit dependencies

## When Lazy Creation Is the Better Choice 🚀

Prefer lazy creation when:

- Page objects are heavy
- The flow is rarely used
- The test suite is large
- Execution speed matters

## Corrected Takeaway

- ❌ "Fixture approach is bad"
- ✅ "Injecting unused fixtures is bad"

Playwright's real guidance can be summarized as:

> **_Use fixtures deliberately._**
>
> **_Inject only what the test needs._**
>
> **_Create everything else lazily._**

## TL;DR

- Fixtures are eagerly created **only if injected**
- Over-injecting fixtures causes wasted time and memory
- Playwright recommends fixtures, not global eager creation
- Best practice = fixtures + selective injection + lazy object creation

## Final Thought

Great automation is intentional. Creating everything upfront may feel safe, but creating **only what you need** is what keeps your test suite fast, maintainable, and scalable over time.



### 17. Sajith Dilshan (Medium) — 🎭 Playwright Annotations — A Practical Guide for QA Engineers

- Source: https://medium.com/@sajith-dilshan/playwright-annotations-a-practical-guide-for-qa-engineers-f1c723fc47f7
- Retrieved: 2026-08-29
- Firecrawl status: complete

# 🎭 Playwright Annotations — A Practical Guide for QA Engineers

![](https://miro.medium.com/v2/resize:fit:616/1*2fHYip2c2J2Iswc_rkESYg.png)

Playwright Annotations

Modern test automation isn't just about writing tests — it's about **controlling execution**, **handling failures**, and **maintaining stability in CI/CD**. This is where **Playwright Annotations** become extremely useful.

Playwright provides built-in annotations that allow QA engineers to:

- Skip irrelevant tests
- Mark expected failures
- Pause unstable tests
- Handle slow scenarios
- Focus debugging on specific tests
- Add tags for better reporting

These annotations appear directly in **Playwright HTML reports**, making debugging and test tracking much easier.

## What Are Playwright Annotations?

Annotations are special markers added to tests to **change execution behavior** without modifying test logic.

They can be applied to:

- Single test
- Test group (`describe`)
- Conditionally
- Based on fixtures
- Runtime logic

Playwright supports both:

- Built-in annotations
- Custom tags

## 1. test.skip() — Skip Irrelevant Tests

`test.skip()` marks a test as **not applicable**, and Playwright will **not run it**.

This is commonly used when:

- Feature not available
- Environment issue
- Temporary block
- Browser-specific failures

## Example

```ts
import { test, expect } from '@playwright/test';

test.skip('Payment test - gateway down', async ({ page }) => {
  await page.goto('/payment');
});
```

## Real QA Scenario

Payment API is down in QA environment.

Instead of failing the test — we **skip it temporarily**.

## Conditional Skip (Powerful)

You can skip tests dynamically.

```ts
test.skip(process.env.ENV === 'production', 'Skipping in production');
```

This is very useful in **CI pipelines**.

## 2. test.fail() — Expected Failure Validation

`test.fail()` marks a test as **expected to fail**.

Playwright will:

- ✅ Run the test
- ✅ Expect it to fail
- ❌ If test passes → Playwright throws error

## Example

```ts
test.fail('Bug: login allows empty password', async ({ page }) => {
  await page.goto('/login');
  await page.click('#login');

  await expect(page.locator('.error')).toBeVisible();
});
```

## QA Engineer Use Case

You found a bug:

- Login works without password
- Test should fail
- Mark as expected failure

When dev fixes bug → test passes → Playwright alerts you 🔥

This is **perfect for bug tracking**.

## 3. test.fixme() — Known Broken Test (Do Not Run)

`test.fixme()` also marks test as failing, **but Playwright does NOT run it**.

Use this when:

- Test crashes browser
- Feature incomplete
- Test unstable
- Execution is slow

## Example

```ts
test.fixme('Profile image upload', async ({ page }) => {
  await page.goto('/profile');
});
```

## Difference: fail vs fixme

- test.fail → run and expect failure
- test.fixme → do not run test

This is important in **large automation suites**.

## 4. test.slow() — Mark Long Running Tests

Marks test as **slow** and automatically **triples timeout**.

- Default timeout = 30s
- Slow test timeout = 90s

## Example

```ts
test.slow('Generate report test', async ({ page }) => {
  await page.goto('/reports');
});
```

## When QA Engineers Use This

- Report generation
- File upload
- Payment flow
- Email verification
- Data sync

Instead of increasing global timeout — mark only slow tests.

## 5. Focus a Test — test.only()

When debugging, we often want to run **only one test**.

```ts
test.only('Login test', async ({ page }) => {
  await page.goto('/login');
});
```

Playwright will:

- ✅ Run only this test
- ❌ Ignore all others

⚠️ QA Best Practice

Never commit `test.only()` to repo.

## Conditional Annotations (Advanced Usage)

Annotations can depend on fixtures.

## Skip in Safari

```ts
test.skip(({ browserName }) => browserName === 'webkit');

test('Drag and drop', async ({ page }) => {});
```

## Fail only in mobile

```ts
test.fail(({ isMobile }) => isMobile);

test('Responsive layout', async ({ page }) => {});
```

## Skip in production

```ts
test.skip(process.env.ENV === 'prod');

test('Delete user', async ({ page }) => {});
```

Very useful for:

- Cross browser testing
- Environment specific tests
- Mobile vs desktop

## Group-Level Annotations

You can apply annotations to **multiple tests**.

```ts
test.describe.skip('Checkout Tests', () => {

  test('Add product', async ({ page }) => {});
  test('Checkout payment', async ({ page }) => {});

});
```

All tests inside will be skipped.

### Conditional Group Skip

```ts
test.describe.skip(({ browserName }) => browserName === 'webkit',
  'Skip Safari tests', () => {

  test('Canvas drawing', async ({ page }) => {});
  test('Drag drop', async ({ page }) => {});

});
```

### Mark Entire Suite Slow

```ts
test.describe('Reports', () => {

  test.slow();

  test('Generate report', async ({ page }) => {});
  test('Download report', async ({ page }) => {});

});
```

## Multiple Annotations on Same Test

Playwright allows stacking annotations.

```ts
test.fail();
test.slow();

test('Checkout test', async ({ page }) => {
  await page.goto('/checkout');
});
```

This test:

- Expected to fail
- Runs with slow timeout

## Real QA Engineer Example

```ts
import { test, expect } from '@playwright/test';

test.describe('Ecommerce Tests', () => {

  test.only('Login test', async ({ page }) => {
    await page.goto('/login');
  });

  test.skip('Payment test', async ({ page }) => {
    // API down
  });

  test.fixme('Coupon test', async ({ page }) => {
    // feature incomplete
  });

  test.fail('Cart total bug', async ({ page }) => {
    await page.goto('/cart');
    await expect(page.locator('#total')).toHaveText('$200');
  });

  test.slow('Invoice generation', async ({ page }) => {
    await page.goto('/invoice');
  });

});
```

## Built-in Playwright Annotations Summary

![](https://miro.medium.com/v2/resize:fit:700/1*loxsiQ-Sd3H_fy7vW0ikHg.png)

## QA Best Practices 🧠

- Use `test.skip()` → environment issues
- Use `test.fail()` → known bugs
- Use `test.fixme()` → unstable tests
- Use `test.slow()` → long execution
- Use `test.only()` → debugging only

## Final Thoughts

Playwright annotations give QA engineers **fine-grained control** over test execution. They help maintain **stable automation suites**, improve **CI reliability**, and make **bug tracking automatic**.

In real-world automation frameworks, annotations are used daily to:

- Manage flaky tests
- Handle environment issues
- Track bugs automatically
- Speed up debugging
- Maintain clean reports

Mastering these annotations will significantly improve your **Playwright automation skills** and make your **test suite production-ready**.



### 18. TestDino (Vishwas Tiwari) — Fixing Playwright Tests with AI: What to Fix, What to Flag

- Source: https://testdino.com/blog/fixing-playwright-tests-with-ai
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Fixing Playwright Tests with AI: What to Fix, What to Flag

AI can fix Playwright tests now. What to fix, what to flag, and how TestDino MCP proves a fix held.

![Fixing Playwright Tests with AI: What to Fix, What to Flag](https://testdino.com/_next/image?url=https%3A%2F%2Fcms.testdino.com%2Fwp-content%2Fuploads%2F2026%2F06%2FFixing-Playwright-Tests-with-AI-What-to-Fix-What-to-Flag.png&w=3840&q=75)

AI can fix Playwright tests now. What to fix, what to flag, and how TestDino MCP proves a fix held.A failing Playwright test gives you 1 line: Timeout 30000ms exceeded, or locator resolved to 0 elements. That line is a symptom, not a cause. The same message can mean a renamed button, a slow API in CI, a feature flag that flipped, or a real bug your test just caught.

In 2026, Playwright can patch that itself: it ships agents that read the live page, diagnose the failure, fix the test, and re-run it. AI can now fix Playwright tests, not just explain them.

The catch is the part most write-ups skip. "Make the test pass" is the wrong goal. A test that goes green because the AI loosened an assertion until it stopped complaining is worse than a red one: it hides a regression and reports success while doing it.

The useful question is narrower: _which failures should AI fix, which should it refuse, and how do you confirm a fix actually holds rather than turns this run green?_

This guide walks through what the tooling does today and where the safe boundary sits. The last part, proving a fix held across runs instead of trusting a single pass, is where the [TestDino MCP server](https://docs.testdino.com/mcp/overview "https://docs.testdino.com/mcp/overview") comes in: it hands your AI agent the run history the healer can't see.

## Fixing Playwright tests with AI: What changed in 2026

The shift is that the framework now does this itself. Playwright v1.56 (released late 2025) introduced Test Agents: 3 built-in agent definitions that guide an LLM through building and maintaining a suite. Per the official release notes, they are:

- **Planner**: explores the app and produces a Markdown test plan.
- **Generator**: turns that plan into executable Playwright Test files, verifying selectors and assertions against the live app as it writes.
- **Healer**: executes the suite and automatically repairs failing tests.

You install them with npx playwright init-agents (with definitions for VS Code, Claude Code, and OpenCode). The agents run on top of the Model Context Protocol, the open standard Anthropic introduced in late 2024, which is what lets an LLM call structured tools rather than guessing from screenshots.

If you are wiring this up for the first time, our [Playwright MCP](https://testdino.com/blog/playwright-mcp "https://testdino.com/blog/playwright-mcp") walkthrough covers what the protocol exposes to an agent and why it matters here.

## Meet the Playwright healer agent

The healer is the one that "fixes" tests, so it's worth being precise about what it actually does, because there is a lot of inflated description floating around. For the wider authoring side of this (planner and generator), see TestDino's breakdown of [Playwright Test Agents](https://testdino.com/blog/playwright-test-agents "https://testdino.com/blog/playwright-test-agents"). This piece stays on the healer and the fix-verification problem.

### How the Playwright healer agent works

The healer runs a 4-step loop, documented in Playwright's test-agents docs and Microsoft's published healer agent spec:

1. **Replay the failing steps in debug mode**. It re-runs the specific failing test, not the whole suite, so it can watch the failure happen.
2. **Inspect the current UI**. It reads the page's [accessibility-tree snapshot](https://testdino.com/blog/accessibility-tree "https://testdino.com/blog/accessibility-tree"), console messages, and network requests, the same structured signals [Playwright MCP](https://testdino.com/blog/playwright-mcp "https://testdino.com/blog/playwright-mcp") exposes, to find the element or flow the test was reaching for.
3. **Classify the** [root cause](https://testdino.com/blog/playwright-root-cause-analysis "https://testdino.com/blog/playwright-root-cause-analysis"). It sorts the failure into one of the 3 categories: a selector that changed, a timing or synchronization problem, or a genuine change in the application.
4. **Patch and re-run**. It applies a fix (a [locator update](https://testdino.com/blog/playwright-locators "https://testdino.com/blog/playwright-locators"), a wait adjustment, an assertion change, or a data fix) and runs the test again. It repeats until the test passes or a guardrail stops the loop.

![4-step Playwright healer agent loop (replay, inspect, classify, patch and re-run) with a branch to test.fixme() when a real bug is found](https://cms.testdino.com/wp-content/uploads/2026/06/playwright-healer-agent-loop-scaled.png)

**A concrete example from the Playwright team:**

- A test used getByRole('textbox', { name: 'Country' }), but the app had switched that field to a combobox.
- The healer read the accessibility snapshot, saw the role mismatch, rewrote the locator to use getByRole('combobox', { name: 'Country' }), and re-ran to confirm.
- In another case, it found a snapshot with 2 heading elements, whereas the assertion expected only 1.
- It loosened the regex so the assertion matched reality; re-ran again before declaring the fix good.

That re-run-before-declaring step matters. **The healer doesn't propose a patch and walk away; it validates the patch against a live run**. But validating against 1 run is exactly where the harder question starts, and we'll come back to it.

## What to fix and what to flag

Every failing test sorts into 1 of 2 buckets, and the whole safety question turns on which bucket it falls into. Either the test's path to the element drifted while its intent stayed correct (fix it), or the app's behavior changed, and the test caught it (flag it). The next 3 sections walk that split.

### What AI can safely fix

The failures AI handles well share a trait: the test's intent is still correct, and only the path to the element drifted. The app didn't break; the test's description of the app went stale.

- **Changed locators / DOM drift.** A renamed button, a restructured form, a role that changed from textbox to combobox. The element still exists and still does its job; the selector points at an old shape.
- **Timing and synchronization**. A step that races a slow network call or a late-rendering element. The fix is usually waiting on the right signal (waitForResponse() on the real request) instead of a hardcoded delay, and Playwright's auto-waiting handles most of the rest.
- **UI-driven assertion drift**. An assertion that's stricter than the UI now warrants, where the page legitimately changed, and the check needs to match it.

These are safe because the AI can see everything it needs in the accessibility tree to confirm the fix. The element is right there; the agent reads its role, name, and state, and verifies the corrected locator resolves. Nothing about the app's correctness is in question, so there's no hidden regression for the fix to mask.

This is also the maintenance work that eats the most QA time, the routine churn from UI changes rather than real defects. Automating it is genuinely useful, and it's the bulk of how teams [reduce test maintenance](https://testdino.com/blog/reduce-test-maintenance "https://testdino.com/blog/reduce-test-maintenance") without lowering coverage. The line to hold is the next section.

### What AI should not auto-fix

The failures AI should refuse share the opposite trait: the test is right, and the app is wrong. Here, "fixing" the test means deleting the evidence of a bug.

- **Backend and API contract changes**. A response shape changed, an endpoint returns a 500, a save operation fails with a fetch error. The healer reads the front end; it has no visibility into whether the backend's new behavior is intended or broken.
- **Feature flags and configuration**. A flag flipped, and a flow disappeared. A failing test is the correct signal that the behavior has changed; auto-adapting hides the change.
- **Multi-tenant data models and test-state issues**. Failures that depend on which tenant, which seed data, or what state a previous test left behind. The AI can't reason about data it can't see.
- **Multi-step business logic**. A checkout that computes the wrong total, a permission check that lets the wrong role through. The locators all resolve; the logic is wrong, and that's invisible to an agent reasoning over the page's structure.

There's a fifth, subtler risk that cuts across all of these: **hallucinated assertions**. An LLM can rewrite an assertion to something that looks plausible and passes, but doesn't actually describe correct behavior. The test goes green, the reasoning reads convincingly, and the check now verifies nothing. This is why "the explanation sounded right" is not the same as "the fix is right."

Playwright's healer was designed with this boundary in mind, and its guardrail is the most important detail in the whole feature.

### The test.fixme() guardrail

When the healer has high confidence that the test is correct but can't fix the failure through code, it **does not** force the test green or silently skip it. Per Microsoft's healer agent spec, it marks the test test.fixme() and adds a comment before the failing step explaining what's happening instead of the expected behavior.

In the Playwright team's own walkthrough, the healer hit a save that failed with a backend error, judged it "likely a backend issue," and marked the test test.fixme() with a note rather than patching around it. That's the framework saying, in code, "this is a real problem for a human, not a test to silence."

That single behavior settles the design debate: even the official tool treats "make it pass" as the wrong goal when passing would be a lie. The fix for a real bug is a ticket, not a patch.

## A decision matrix from failure-type to fix-strategy

The boundary above collapses into a single table. Before letting an agent touch a failing test, the question is always the same:

Did the test's path to the element drift, or did the app's behavior change?

| Failure type | What actually broke | Safe for AI to fix? | Right action |
| --- | --- | --- | --- |
| Changed locator / DOM drift | Selector points at old shape; element still exists |  | Let the agent update the locator, then verify across runs |
| Timing/synchronization | Step races a network call or late render |  | Wait on the real signal; confirm stability over repeats |
| UI-driven assertion drift | Page legitimately changed |  | Update the assertion, then human-confirm it still describes correct behavior |
| Backend / API contract change | App behavior changed or broke |  | Investigate; file a bug if unintended |
| Feature flag/config change | A flow appeared or vanished by design |  | Confirm intent with the team before changing the test |
| Test-data / state | Depends on data the agent can't see |  | Fix the data setup, not the assertion |
| Multi-step business logic | Logic is wrong; locators all resolve |  | Treat the red test as a caught regression |
| Hallucinated assertion (any type) | AI rewrote the check to something plausible but false |  | Reject; require the assertion to match observed correct behavior |

The pattern we see is that green rows are about _finding the element_, while red rows are about judging the behavior. AI is strong at the first and structurally blind to the second.

![Two-column graphic: AI can find the element (locator drift, timing, assertion drift) but is blind to judging behavior (backend change, feature flag, test data, business logic, hallucinated assertion)](https://cms.testdino.com/wp-content/uploads/2026/06/ai-find-element-vs-judge-behavior-scaled.png)

## Why "make the test pass" is the wrong goal

It's worth saying plainly because the entire category of "self-healing tests" is marketed on pass rates. A higher pass rate is trivial to manufacture: loosen every assertion, skip every stubborn test, retry until the flake clears. You'd get a green dashboard and a suite that catches nothing.

The job of a test is to fail when the product is wrong. A fix that's measured only by "the test now passes" optimizes against that job. The right success metric for an AI fix has 2 parts:

1. **The test still asserts what it was meant to assert**. The intent survived the patch. A locator update keeps the original check intact; an assertion that got quietly weakened does not.
2. **The fix is stable, not just green once**. A test that passes on the run the agent watched, then flakes on the next 10 wasn't fixed; the agent just caught a lucky run.

The first is a review question, answered by a human reading the diff. The second is a data question, and it's the one a single run can't answer, because the evidence lives in the suite's history, not on the page in front of the agent.

![A 100% passing dashboard peeled back to reveal it was faked by loosening assertions, skipping flaky tests, and retrying until green, so the suite catches nothing](https://cms.testdino.com/wp-content/uploads/2026/06/green-dashboard-is-trivial-to-fake-scaled.png)

## The failure the healer can't see

Picture the failure you actually dread. Not the renamed button. The test that passed on the run the agent watched, then went red 3 deploys later, on the CI runner in one region, one run in twenty. The healer looked at a single snapshot, saw a slow step, "fixed" it with a longer wait, and declared victory. The wait was never the problem. The test was intermittent, and now the [flake is buried](https://testdino.com/blog/playwright-flaky-tests "https://testdino.com/blog/playwright-flaky-tests") under a patch that makes it look like it was handled.

This is the structural blind spot. **The healer reasons over 1 run: the page in front of it**. That's exactly right for: "Which locator matches this element," and Playwright MCP serves it well, handing the agent the accessibility tree (role, name, state, hierarchy) as compact structured text rather than pixels or raw DOM.

But " _does this fail every time or one run in twenty?_" "Did it start after a specific deploy?" "Is this 1 flake or 50 tests failing on the same error?" Is not answerable from a single snapshot. **They are properties of the suite's history, and an agent that can't see that history will confidently paper over them.**

![Contrast graphic: the healer sees one failing snapshot and patches it as a timing issue, while the run history shows the same test failed 5 of 10 runs across regions, so it was never a timing bug](https://cms.testdino.com/wp-content/uploads/2026/06/healer-one-run-vs-run-history-scaled.png)

## The 2 bars a healed test still has to clear

So a healed test isn't a fixed test. It's a candidate fix that still has to clear 2 bars; the run that produced it can't measure:

- **Is it stable**, or just green once? A locator fix should pass every time. If it passes 8 out of 10 reruns, the real problem was flakiness, and the "fix" hid it. (Playwright's --repeat-each exists precisely to shake this out, but only if something is watching the result across those runs.)
- **Did it lower the bar?** Confirm the patch changed the path to the element, not the standard the test holds the app to. A loosened assertion needs a human to confirm the looser check still describes correct behavior.

Answering both by eye, per fix, doesn't survive past a handful of tests. **What closes the loop is [run history](https://testdino.com/blog/playwright-test-history "https://testdino.com/blog/playwright-test-history"): the record of how that specific test behaved across every run, before and after the patch**. That's a different layer from the live page, and it's the layer the healer doesn't have.

![TestDino History tab for a single Playwright test showing 42% stability across 52 runs (22 passed, 10 failed, 17 flaky) with a run-by-run table marking most recent runs flaky](https://cms.testdino.com/wp-content/uploads/2026/06/testdino-run-history-flaky-alert-scaled.png)

## Giving the agent the run history: TestDino MCP

This is the layer [TestDino](https://testdino.com/ "https://testdino.com") sits on. TestDino is a Playwright observability platform: it captures the results of every run, whether it happened in CI, in a cloud agent's sandbox, or on a local machine, and retains traces, screenshots, videos, console logs, and the run-over-run trend for each test. Its [MCP server](https://docs.testdino.com/mcp/overview "https://docs.testdino.com/mcp/overview") exposes that history to an AI agent as callable tools, so the agent that just patched a test can ask, in the same chat, whether the patch actually held.

### What the run-history layer adds

Where Playwright MCP answers "what's on the page right now?", the TestDino MCP server answers "how has this test behaved across every run?". That's the missing half of the verification loop. Concretely, an agent connected to it can, in plain language:

- **Pull a test's run history and reruns** (get\_testcase\_details) to tell a real fix from a lucky pass: did the patched test go green and stay green across the next runs, or is it still flickering?
- **Run a root-cause analysis over historical executions** (debug\_testcase, which the docs describe as analyzing historical execution data to suggest root-cause fixes) so a timing patch is checked against whether the failure was ever a timing problem to begin with.
- **Detect and rank [flaky tests](https://testdino.com/blog/flaky-tests "https://testdino.com/blog/flaky-tests") across runs**, so a "fixed" test that's actually intermittent gets caught instead of trusted.
- **Group failures by error and root cause** (get\_run\_details), so the agent can see whether this red test is isolated or part of a pattern that hits 50 tests after a deploy, the difference between a one-line patch and a rolled-back release.

### The run-history tools the agent calls

Those questions map onto specific tools. The server exposes [27 in total](https://docs.testdino.com/mcp/tools-reference "https://docs.testdino.com/mcp/tools-reference") (across analysis, manual testing, releases, and exploratory sessions); the handful that matter for verifying an AI fix are the analysis tools that read run history:

| TestDino MCP tools | Their use |
| --- | --- |
| get\_testcase\_details | Pull one test's run history and reruns to tell a real fix from a lucky pass. |
| debug\_testcase | Root-cause a failure over its historical executions, not just this run. |
| Root cause\_details | Group a run's failures by error to see if a red test is isolated or a pattern. |
| list\_testcase (isFlaky) | Filter for flaky tests across runs so an intermittent "fix" gets caught. |
| list\_testruns | List recent runs for a project or branch to compare before and after a patch. |
| create\_release | Cut a release from chat once the history confirms the suite is clean. |

### One-click connect

Connecting used to be the friction point: generate a token, copy it, edit a JSON config, restart the client. TestDino recently collapsed that into 1 step. You paste one URL ( [mcp.testdino.com)](https://mcp.testdino.com/) and approve it in the browser, with a scope picker to specify exactly which projects and modules the connection can access.

[https://cms.testdino.com/wp-content/uploads/2026/06/connect-testdino-mcp-in-one-click.mp4](https://cms.testdino.com/wp-content/uploads/2026/06/connect-testdino-mcp-in-one-click.mp4)

## Beyond verification: Drive the workflow from chat

Once connected, the agent isn't limited to verification. Because the same server exposes test runs, manual cases, and releases as tools, you can drive a chunk of the testing workflow from chat, asking which tests are flaky this week, triaging a failing run, or cutting a release without leaving the conversation.

[https://cms.testdino.com/wp-content/uploads/2026/06/manual-testing-and-release-management-with-claude.mp4](https://cms.testdino.com/wp-content/uploads/2026/06/manual-testing-and-release-management-with-claude.mp4)

The server is open source ( [github.com/testdino-hq/testdino-mcp](https://github.com/testdino-hq/testdino-mcp)), and it sits alongside TestDino's other Playwright tooling, including the [playwright-skill](https://github.com/testdino-hq/playwright-skill "https://github.com/testdino-hq/playwright-skill") best-practices guide for AI agents and the broader [TestDino-Plugins](https://github.com/testdino-hq/TestDino-Plugins "https://github.com/testdino-hq/TestDino-Plugins") collection.

The shape of the full loop, then: the healer (or your own agent) proposes a fix from the live page; the run-history layer confirms, across every recorded run, that the test now passes consistently and didn't quietly lower its standards. The first half makes the test green. The second half makes it trustworthy. A fix isn't done until both are true.

## How to use this safely

The tooling is good and getting better, but the boundary is yours to hold:

- **Review every fix, even when the reasoning reads well**. AI constructs convincing explanations for wrong fixes; a plausible rationale is not evidence.
- **Guard assertions hardest**. Locator and timing fixes are low-risk. An assertion change is the one place where a "fix" can silently delete coverage, so confirm that the new check still describes the correct behavior.
- **Trust** test.fixme() over a forced pass. When an agent flags a failure as a likely real bug, that's the system working. Investigate it; don't reach for a patch that makes it green.
- **Verify across runs, not on 1 pass.** A single green run is the start of trusting a fix, not the end. Give the agent the run history, or confirm only that this run is green.

Used this way, AI takes routine maintenance, locator churn, and timing waits off your plate, while the failures that actually matter still reach a human. That's the version of "fixing Playwright tests with AI" worth running: faster on the noise, honest about the signal.

## FAQs

Does Playwright have self-healing tests?

Yes, as of v1.56 (late 2025). Playwright ships a built-in **Healer** agent (1 of 3 Test Agents, alongside Planner and Generator) that replays a failing test, inspects the live page, classifies the root cause, and patches the locator, wait, or assertion before re-running. Install the agents with npx playwright init-agents. It's the framework's own self-healing, not a third-party plugin like Healenium.

How does the Playwright healer agent fix a failing test?

It runs a 4-step loop: replay the failing test in debug mode, read the page's accessibility tree snapshot, console and network signals, classify the failure (changed selector, timing, or a real app change), then apply a patch and re-run until the test passes or a guardrail stops it. It validates every patch against a live run before declaring the fix good.

What AI tools can make Playwright tests self-healing when the UI changes often?

For UI churn specifically, Playwright's own healer agent handles the common cases: renamed buttons, restructured forms, a role that changed from textbox to combobox. It reads the live accessibility tree, so it can confirm the corrected locator actually resolves. The boundary to hold is that "the UI changed" and "the app's behavior changed" look identical in the error message but require opposite responses, so pair any healer with a review step and cross-run verification rather than blindly trusting auto-applied patches.

How do I know an AI fix actually held, rather than just turning the run green?

Check 2 things the single run that produced the patch can't show you. First, stability: re-run the healed test across runs and runners (--repeat-each helps) so you can tell a real fix from a lucky pass. Second, that it didn't lower the bar: confirm the patch changed the path to the element, not the standard the test holds the app to. Both questions are answered by the test's run history, not by one snapshot.

How can TestDino help with fixing flaky Playwright tests using AI?

The hard part of an AI fix isn't making this run green; it's proving the fix held across every run. TestDino is a Playwright observability platform that captures the history of each test (reruns, traces, flakiness trends, failure groupings), and the TestDino MCP server exposes that history to your AI agent (Cursor, Claude Code, Claude Desktop) as callable tools. So the agent that just patched a test can ask, in the same chat, whether the patch actually stuck across subsequent runs or was just a lucky pass.



### 19. Yevhen Laichenkov (GitHub) — playwright-expect (archived README)

- Source: https://github.com/elaichenkov/playwright-expect
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Deprecated!

Please, use built-in Playwright assertions.

# playwright-expect

[![Test](https://github.com/elaichenkov/playwright-expect/actions/workflows/tests.yml/badge.svg)](https://github.com/elaichenkov/playwright-expect/actions/workflows/tests.yml)[![Total npm downloads](https://camo.githubusercontent.com/d97cfa3c3610b601f5acd2a9c96c672d3cd726232f1bf81677c08918c3bab028/68747470733a2f2f696d672e736869656c64732e696f2f6e706d2f64742f706c61797772696768742d6578706563742e737667)](https://www.npmjs.com/package/playwright-expect)[![NPM version](https://camo.githubusercontent.com/b84a07de5453d841139b1fda8cb9c3b16160927d8a08edb73bc3ba4de9dc953d/68747470733a2f2f696d672e736869656c64732e696f2f6e706d2f762f706c61797772696768742d6578706563742e737667)](https://www.npmjs.com/package/playwright-expect)[![Commits](https://camo.githubusercontent.com/02c532fcd142c76e6f26019b572177ef792afd46d0af7c7056b15bdc851d0b17/68747470733a2f2f696d672e736869656c64732e696f2f6769746875622f636f6d6d69742d61637469766974792f792f656c61696368656e6b6f762f706c61797772696768742d6578706563742e737667)](https://github.com/elaichenkov/playwright-expect/commits/main)[![MIT licensed](https://camo.githubusercontent.com/7013272bd27ece47364536a221edb554cd69683b68a46fc0ee96881174c4214c/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d4d49542d626c75652e737667)](https://github.com/elaichenkov/playwright-expect/blob/main/LICENSE)

The `playwright-expect` is an assertion library for TypeScript and JavaScript intended for use with a test runner such as [Jest](https://jestjs.io/) or [Playwright Test](https://playwright.dev/). It lets you write better assertions for end-to-end testing.

# Motivation

## Motivation

_**TL;DR:**_

> [expect-playwright](https://github.com/playwright-community/expect-playwright) is a great library, but it contains a few methods.

[playwright-expect](https://github.com/elaichenkov/playwright-expect) is a great library too, with all major methods and extra features such as waits, ignore case sensitive, trim. All in all, It has everything that you demand to accomplish end-to-end testing needs.

_**Please, read more about [motivation and main features](https://elaichenkov.medium.com/expect-more-with-playwright-expect-5eb4e23d3916).**_

# Key Features

## Key Features

- rich and easy to use;
- exhaustive messages and diff highlights;
- can ignore case sensitive and trim values before asserting;
- waits for expectation to succeed;
- works in Jest and Playwright Test;
- built-in types for TypeScript and JavaScript autocompletion.

# Usage

## Usage

## Install

## Install

```
npm i -D playwright-expect
```

## Playwright Test - TypeScript

## Playwright Test - TypeScript

```
// playwright.config.ts
import { expect } from '@playwright/test';
import { matchers } from 'playwright-expect';

// add custom matchers
expect.extend(matchers);
```

## Playwright Test - JavaScript

## Playwright Test - JavaScript

```
// playwright.config.js
const { expect } = require('@playwright/test');
const { matchers } = require('playwright-expect');

// add custom matchers
expect.extend(matchers);
```

# [API](https://elaichenkov.github.io/playwright-expect/modules.html)

## API

> Please, read [API](https://elaichenkov.github.io/playwright-expect/modules.html) documentation

- [toBeDisabled](https://elaichenkov.github.io/playwright-expect/modules/tobedisabled.html)
- [toBeEnabled](https://elaichenkov.github.io/playwright-expect/modules/tobeenabled.html)
- [toBeChecked](https://elaichenkov.github.io/playwright-expect/modules/tobechecked.html)
- [toBeFocused](https://elaichenkov.github.io/playwright-expect/modules/tobefocused.html)
- [toBeVisible](https://elaichenkov.github.io/playwright-expect/modules/tobevisible.html)
- [toContainText](https://elaichenkov.github.io/playwright-expect/modules/tocontaintext.html)
- [toContainTitle](https://elaichenkov.github.io/playwright-expect/modules/tocontaintitle.html)
- [toContainUrl](https://elaichenkov.github.io/playwright-expect/modules/tocontainurl.html)
- [toContainValue](https://elaichenkov.github.io/playwright-expect/modules/tocontainvalue.html)
- [toHaveCount](https://elaichenkov.github.io/playwright-expect/modules/tohavecount.html)
- [toHaveText](https://elaichenkov.github.io/playwright-expect/modules/tohavetext.html)
- [toHaveTitle](https://elaichenkov.github.io/playwright-expect/modules/tohavetitle.html)
- [toHaveUrl](https://elaichenkov.github.io/playwright-expect/modules/tohaveurl.html)
- [toHaveValue](https://elaichenkov.github.io/playwright-expect/modules/tohavevalue.html)
- [toMatchText](https://elaichenkov.github.io/playwright-expect/modules/tomatchtext.html)

# Examples

## Examples

> All methods, which expects element can accept element in three ways:
>
> 1. \[page, selector\] ( _recommended_)
> 2. ElementHandle
> 3. Promise<ElementHandle>

## Use `toHaveText` to check that element's text equals to the expected

## Use toHaveText to check that element's text equals to the expected

```
// Using ElementHandle
const title = await page.$('h1');

await expect(title).toHaveText('Home');

// Using Promise<ElementHandle>
await expect(page.$('h1')).toHaveText('Home');

// Using an array of page and selector. Furthermore, you can pass options such as ignoreCase and trim
await expect([page, 'h1']).toHaveText('home', { ignoreCase: true });

// Even more, you can wait for the element before asserting
await expect([page, '.notification']).toHaveText('Success', { timeout: 15000 });

// Also, it could be useful to fail fast, by default it waits for the 10 seconds
await expect([page, '.notification']).toHaveText('success', { timeout: 1000, trim: true });
```

> NOTE:
> You can wait for the element only using the \[page, selector\] approach

## Use `toBeVisible` to check that element is visible

## Use toBeVisible to check that element is visible

```
// Using ElementHandle
const button = await page.$('#next');

await expect(title).toBeVisible();

// Using Promise<ElementHandle>
await expect(page.$('#next')).toBeVisible(true); // true here is optional

// Using an array of page and selector
await expect([page, '#next']).toBeVisible(false);
```

## Use `toBeEnabled` and `toBeDisabled` to check that element is enabled/disabled

## Use toBeEnabled and toBeDisabled to check that element is enabled/disabled

```
// Using ElementHandle
const button = await page.$('#next');

await expect(title).toBeEnabled();

// Using Promise<ElementHandle>
await expect(page.$('#next')).toBeEnabled();

// Using an array of page and selector
await expect([page, '#next']).toBeEnabled(false);

// Also, you can use `not` to verify opposite
await expect([page, '#next']).not.toBeEnabled();

// Even more, you can check that element is disabled
await expect(page.$('#next')).toBeDisabled();
```

## Use `toHaveUrl` and `toContainUrl` to check that page's url equals or contains the expected url

## Use toHaveUrl and toContainUrl to check that page's url equals or contains the expected url

```
await expect(page).toHaveUrl('https://duckduckgo.com/');

// Also, you can wait for the url
await expect(page).toHaveUrl('https://duckduckgo.com/', { timeout: 5000 });

await expect(page).toContainUrl('duck');
```

## Use `toHaveTitle` or `toContainTitle` to check that page's title equals or contains the expected title

## Use toHaveTitle or toContainTitle to check that page's title equals or contains the expected title

```
await expect(page).toHaveTitle('DuckDuckGo — Privacy, simplified.');

await expect(page).toContainTitle('Privacy');

// ignore case sensitive
await expect(page).toContainTitle('privacy', {ignoreCase: true});
```

# Author

## Author

Yevhen Laichenkov [elaichenkov@gmail.com](mailto:elaichenkov@gmail.com)

# Inspired by

## Inspired by

[expect-playwright](https://github.com/playwright-community/expect-playwright)

[expect-webdriverio](https://github.com/webdriverio/expect-webdriverio)



### 20. Viktor Konovalov (LinkedIn) — Playwright + TypeScript QA post (LinkedIn)

- Source: https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7489942200819015680-t7Pt
- Retrieved: 2026-08-29
- Firecrawl status: unavailable

**Retrieval error:**

> Firecrawl refused: "We apologize for the inconvenience but we do not support this site." — linkedin.com is not supported by Firecrawl (site-level refusal, not a concurrency error; retried once after a 30-second wait with the same result). LinkedIn posts also require authentication, so no public article content is retrievable.

No article content was substituted for the missing page.



### 21. Stefan Minchev (LinkedIn) — QA / Playwright post (LinkedIn)

- Source: https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7482814931646722048-rPM0
- Retrieved: 2026-08-29
- Firecrawl status: unavailable

**Retrieval error:**

> Firecrawl refused: "We apologize for the inconvenience but we do not support this site." — linkedin.com is not supported by Firecrawl (site-level refusal, not a concurrency error; retried once after a 30-second wait with the same result). LinkedIn posts also require authentication, so no public article content is retrievable.

No article content was substituted for the missing page.



### 22. Playwright Team — Best Practices (official docs)

- Source: https://playwright.dev/docs/best-practices
- Retrieved: 2026-08-29
- Firecrawl status: complete

## Introduction

This guide should help you to make sure you are following our best practices and writing tests that are more resilient.

## Testing philosophy

### Test user-visible behavior

Automated tests should verify that the application code works for the end users, and avoid relying on implementation details such as things which users will not typically use, see, or even know about such as the name of a function, whether something is an array, or the CSS class of some element. The end user will see or interact with what is rendered on the page, so your test should typically only see/interact with the same rendered output.

### Make tests as isolated as possible

Each test should be completely isolated from another test and should run independently with its own local storage, session storage, data, cookies etc. [Test isolation](https://playwright.dev/docs/browser-contexts) improves reproducibility, makes debugging easier and prevents cascading test failures.

In order to avoid repetition for a particular part of your test you can use [before and after hooks](https://playwright.dev/docs/api/class-test). Within your test file add a before hook to run a part of your test before each test such as going to a particular URL or logging in to a part of your app. This keeps your tests isolated as no test relies on another. However it is also ok to have a little duplication when tests are simple enough especially if it keeps your tests clearer and easier to read and maintain.

```js
import { test } from '@playwright/test';

test.beforeEach(async ({ page }) => {

  // Runs before each test and signs in each page.

  await page.goto('https://github.com/login');

  await page.getByLabel('Username or email address').fill('username');

  await page.getByLabel('Password').fill('password');

  await page.getByRole('button', { name: 'Sign in' }).click();

});

test('first', async ({ page }) => {

  // page is signed in.

});

test('second', async ({ page }) => {

  // page is signed in.

});
```

You can also reuse the signed-in state in the tests with [setup project](https://playwright.dev/docs/auth#basic-shared-account-in-all-tests). That way you can log in only once and then skip the log in step for all of the tests.

### Avoid testing third-party dependencies

Only test what you control. Don't try to test links to external sites or third party servers that you do not control. Not only is it time consuming and can slow down your tests but also you cannot control the content of the page you are linking to, or if there are cookie banners or overlay pages or anything else that might cause your test to fail.

Instead, use the [Playwright Network API](https://playwright.dev/docs/network#handle-requests) and guarantee the response needed.

```js
await page.route('**/api/fetch_data_third_party_dependency', route => route.fulfill({

  status: 200,

  body: testData,

}));

await page.goto('https://example.com');
```

### Testing with a database

If working with a database then make sure you control the data. Test against a staging environment and make sure it doesn't change. For visual regression tests make sure the operating system and browser versions are the same.

## Best Practices

### Use locators

In order to write end to end tests we need to first find elements on the webpage. We can do this by using Playwright's built in [locators](https://playwright.dev/docs/locators). Locators come with auto waiting and retry-ability. Auto waiting means that Playwright performs a range of actionability checks on the elements, such as ensuring the element is visible and enabled before it performs the click. To make tests resilient, we recommend prioritizing user-facing attributes and explicit contracts.

```js
// 👍

page.getByRole('button', { name: 'submit' });
```

#### Use chaining and filtering

Locators can be [chained](https://playwright.dev/docs/locators#matching-inside-a-locator) to narrow down the search to a particular part of the page.

```js
const product = page.getByRole('listitem').filter({ hasText: 'Product 2' });
```

You can also [filter locators](https://playwright.dev/docs/locators#filtering-locators) by text or by another locator.

```js
await page

    .getByRole('listitem')

    .filter({ hasText: 'Product 2' })

    .getByRole('button', { name: 'Add to cart' })

    .click();
```

#### Prefer user-facing attributes to XPath or CSS selectors

Your DOM can easily change so having your tests depend on your DOM structure can lead to failing tests. For example consider selecting this button by its CSS classes. Should the designer change something then the class might change, thus breaking your test.

```js
// 👎

page.locator('button.buttonIcon.episode-actions-later');
```

Use locators that are resilient to changes in the DOM.

```js
// 👍

page.getByRole('button', { name: 'submit' });
```

### Generate locators

Playwright has a [test generator](https://playwright.dev/docs/codegen) that can generate tests and pick locators for you. It will look at your page and figure out the best locator, prioritizing role, text and test id locators. If the generator finds multiple elements matching the locator, it will improve the locator to make it resilient and uniquely identify the target element, so you don't have to worry about failing tests due to locators.

#### Use `codegen` to generate locators

To pick a locator run the `codegen` command followed by the URL that you would like to pick a locator from.

- npm
- yarn
- pnpm

```bash
npx playwright codegen playwright.dev
```

```bash
yarn playwright codegen playwright.dev
```

```bash
pnpm exec playwright codegen playwright.dev
```

This will open a new browser window as well as the Playwright inspector. To pick a locator first click on the 'Record' button to stop the recording. By default when you run the `codegen` command it will start a new recording. Once you stop the recording the 'Pick Locator' button will be available to click.

You can then hover over any element on your page in the browser window and see the locator highlighted below your cursor. Clicking on an element will add the locator into the Playwright inspector. You can either copy the locator and paste into your test file or continue to explore the locator by editing it in the Playwright Inspector, for example by modifying the text, and seeing the results in the browser window.

![generating locators with codegen](https://user-images.githubusercontent.com/13063165/212103268-e7d8ee8b-d307-4cba-be13-831f3fbb1f40.png)

#### Use the VS Code extension to generate locators

You can also use the [VS Code Extension](https://playwright.dev/docs/getting-started-vscode) to generate locators as well as record a test. The VS Code extension also gives you a great developer experience when writing, running, and debugging tests.

![generating locators in vs code with codegen](https://user-images.githubusercontent.com/13063165/212269873-aca04043-16ce-4627-906f-7351d09740ab.png)

### Use web first assertions

Assertions are a way to verify that the expected result and the actual result matched or not. By using [web first assertions](https://playwright.dev/docs/test-assertions) Playwright will wait until the expected condition is met. For example, when testing an alert message, a test would click a button that makes a message appear and check that the alert message is there. If the alert message takes half a second to appear, assertions such as `toBeVisible()` will wait and retry if needed.

```js
// 👍

await expect(page.getByText('welcome')).toBeVisible();

// 👎

expect(await page.getByText('welcome').isVisible()).toBe(true);
```

#### Don't use manual assertions

Don't use manual assertions that are not awaiting the expect. In the code below the await is inside the expect rather than before it. When using assertions such as `isVisible()` the test won't wait a single second, it will just check the locator is there and return immediately.

```js
// 👎

expect(await page.getByText('welcome').isVisible()).toBe(true);
```

Use web first assertions such as `toBeVisible()` instead.

```js
// 👍

await expect(page.getByText('welcome')).toBeVisible();
```

### Configure debugging

#### Local debugging

For local debugging we recommend you [debug your tests live in VS Code](https://playwright.dev/docs/getting-started-vscode#debugging-your-tests) by installing the [VS Code extension](https://playwright.dev/docs/getting-started-vscode). You can run tests in debug mode by right-clicking on the line next to the test you want to run which will open a browser window and pause at where the breakpoint is set.

![debugging tests in vscode](https://user-images.githubusercontent.com/13063165/212274675-5c6e1647-2aab-40fd-9804-8680c1ac2d16.png)

You can live debug your test by clicking or editing the locators in your test in VS Code which will highlight this locator in the browser window as well as show you any other matching locators found on the page.

![live debugging locators in vscode](https://user-images.githubusercontent.com/13063165/212273189-da271dc4-0f59-4138-92a8-10e719066cbe.png)

You can also debug your tests with the Playwright inspector by running your tests with the `--debug` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test --debug
```

```bash
yarn playwright test --debug
```

```bash
pnpm exec playwright test --debug
```

You can then step through your test, view actionability logs and edit the locator live and see it highlighted in the browser window. This will show you which locators match, how many of them there are.

![debugging with the playwright inspector](https://user-images.githubusercontent.com/13063165/212276296-4f5b18e7-2bd7-4766-9aa5-783517bd4aa2.png)

To debug a specific test add the name of the test file and the line number of the test followed by the `--debug` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test example.spec.ts:9 --debug
```

```bash
yarn playwright test example.spec.ts:9 --debug
```

```bash
pnpm exec playwright test example.spec.ts:9 --debug
```

#### Debugging on CI

For CI failures, use the Playwright [trace viewer](https://playwright.dev/docs/trace-viewer) instead of videos and screenshots. The trace viewer gives you a full trace of your tests as a local Progressive Web App (PWA) that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action using dev tools, view network requests and more.

![playwrights trace viewer](https://user-images.githubusercontent.com/13063165/212277895-c63d94c2-bd06-4881-864e-62790a072ca3.png)

Traces are configured in the Playwright config file and are set to run on CI on the first retry of a failed test. We don't recommend setting this to `on` so that traces are run on every test as it's very performance heavy. However you can run a trace locally when developing with the `--trace` flag.

- npm
- yarn
- pnpm

```bash
npx playwright test --trace on
```

```bash
yarn playwright test --trace on
```

```bash
pnpm exec playwright test --trace on
```

Once you run this command your traces will be recorded for each test and can be viewed directly from the HTML report.

- npm
- yarn
- pnpm

```bash
npx playwright show-report
```

```bash
yarn playwright show-report
```

```bash
pnpm exec playwright show-report
```

![Playwrights HTML report](https://user-images.githubusercontent.com/13063165/212279022-d929d4c0-2271-486a-a75f-166ac231d25f.png)

Traces can be opened by clicking on the icon next to the test file name or by opening each of the test reports and scrolling down to the traces section.

![Screenshot 2023-01-13 at 09 58 34](https://user-images.githubusercontent.com/13063165/212279699-c9eb134f-4f4e-4f19-805c-37596d3272a6.png)

### Use Playwright's Tooling

Playwright comes with a range of tooling to help you write tests.

- The [VS Code extension](https://playwright.dev/docs/getting-started-vscode) gives you a great developer experience when writing, running, and debugging tests.
- The [test generator](https://playwright.dev/docs/codegen) can generate tests and pick locators for you.
- The [trace viewer](https://playwright.dev/docs/trace-viewer) gives you a full trace of your tests as a local PWA that can easily be shared. With the trace viewer you can view the timeline, inspect DOM snapshots for each action, view network requests and more.
- The [UI Mode](https://playwright.dev/docs/test-ui-mode) lets you explore, run and debug tests with a time travel experience complete with watch mode. All test files are loaded into the testing sidebar where you can expand each file and describe block to individually run, view, watch and debug each test.
- [TypeScript](https://playwright.dev/docs/test-typescript) in Playwright works out of the box and gives you better IDE integrations. Your IDE will show you everything you can do and highlight when you do something wrong. No TypeScript experience is needed and it is not necessary for your code to be in TypeScript, all you need to do is create your tests with a `.ts` extension.

### Test across all browsers

Playwright makes it easy to test your site across all [browsers](https://playwright.dev/docs/test-projects#configure-projects-for-multiple-browsers) no matter what platform you are on. Testing across all browsers ensures your app works for all users. In your config file you can set up projects adding the name and which browser or device to use.

playwright.config.ts

```js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

  projects: [

    {

      name: 'chromium',

      use: { ...devices['Desktop Chrome'] },

    },

    {

      name: 'firefox',

      use: { ...devices['Desktop Firefox'] },

    },

    {

      name: 'webkit',

      use: { ...devices['Desktop Safari'] },

    },

  ],

});
```

### Keep your Playwright dependency up to date

By keeping your Playwright version up to date you will be able to test your app on the latest browser versions and catch failures before the latest browser version is released to the public.

- npm
- yarn
- pnpm

```bash
npm install -D @playwright/test@latest
```

```bash
yarn add --dev @playwright/test@latest
```

```bash
pnpm install --save-dev @playwright/test@latest
```

Check the [release notes](https://playwright.dev/docs/release-notes) to see what the latest version is and what changes have been released.

You can see what version of Playwright you have by running the following command.

- npm
- yarn
- pnpm

```bash
npx playwright --version
```

```bash
yarn playwright --version
```

```bash
pnpm exec playwright --version
```

### Run tests on CI

Setup CI/CD and run your tests frequently. The more often you run your tests the better. Ideally you should run your tests on each commit and pull request. Playwright comes with a [GitHub actions workflow](https://playwright.dev/docs/ci-intro) so that tests will run on CI for you with no setup required. Playwright can also be setup on the [CI environment](https://playwright.dev/docs/ci) of your choice.

Use Linux when running your tests on CI as it is cheaper. Developers can use whatever environment when running locally but use linux on CI. Consider setting up [Sharding](https://playwright.dev/docs/test-sharding) to make CI faster.

#### Optimize browser downloads on CI

Only install the browsers that you actually need, especially on CI. For example, if you're only testing with Chromium, install just Chromium.

.github/workflows/playwright.yml

```bash
# Instead of installing all browsers

npx playwright install --with-deps

# Install only Chromium

npx playwright install chromium --with-deps
```

This saves both download time and disk space on your CI machines.

### Lint your tests

We recommend TypeScript and linting with ESLint for your tests to catch errors early. Use [`@typescript-eslint/no-floating-promises`](https://typescript-eslint.io/rules/no-floating-promises/) [ESLint](https://eslint.org/) rule to make sure there are no missing awaits before the asynchronous calls to the Playwright API. On your CI you can run `tsc --noEmit` to ensure that functions are called with the right signature.

### Use parallelism and sharding

Playwright runs tests in [parallel](https://playwright.dev/docs/test-parallel) by default. Tests in a single file are run in order, in the same worker process. If you have many independent tests in a single file, you might want to run them in parallel

```js
import { test } from '@playwright/test';

test.describe.configure({ mode: 'parallel' });

test('runs in parallel 1', async ({ page }) => { /* ... */ });

test('runs in parallel 2', async ({ page }) => { /* ... */ });
```

Playwright can [shard](https://playwright.dev/docs/test-parallel#shard-tests-between-multiple-machines) a test suite, so that it can be executed on multiple machines.

- npm
- yarn
- pnpm

```bash
npx playwright test --shard=1/3
```

```bash
yarn playwright test --shard=1/3
```

```bash
pnpm exec playwright test --shard=1/3
```

## Productivity tips

### Use Soft assertions

If your test fails, Playwright will give you an error message showing what part of the test failed which you can see either in VS Code, the terminal, the HTML report, or the trace viewer. However, you can also use [soft assertions](https://playwright.dev/docs/test-assertions#soft-assertions). These do not immediately terminate the test execution, but rather compile and display a list of failed assertions once the test ended.

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

// ... and continue the test to check more things.

await page.getByRole('link', { name: 'next page' }).click();
```

- [Introduction](https://playwright.dev/docs/best-practices#introduction)
- [Testing philosophy](https://playwright.dev/docs/best-practices#testing-philosophy)
  - [Test user-visible behavior](https://playwright.dev/docs/best-practices#test-user-visible-behavior)
  - [Make tests as isolated as possible](https://playwright.dev/docs/best-practices#make-tests-as-isolated-as-possible)
  - [Avoid testing third-party dependencies](https://playwright.dev/docs/best-practices#avoid-testing-third-party-dependencies)
  - [Testing with a database](https://playwright.dev/docs/best-practices#testing-with-a-database)
- [Best Practices](https://playwright.dev/docs/best-practices#best-practices)
  - [Use locators](https://playwright.dev/docs/best-practices#use-locators)
  - [Generate locators](https://playwright.dev/docs/best-practices#generate-locators)
  - [Use web first assertions](https://playwright.dev/docs/best-practices#use-web-first-assertions)
  - [Configure debugging](https://playwright.dev/docs/best-practices#configure-debugging)
  - [Use Playwright's Tooling](https://playwright.dev/docs/best-practices#use-playwrights-tooling)
  - [Test across all browsers](https://playwright.dev/docs/best-practices#test-across-all-browsers)
  - [Keep your Playwright dependency up to date](https://playwright.dev/docs/best-practices#keep-your-playwright-dependency-up-to-date)
  - [Run tests on CI](https://playwright.dev/docs/best-practices#run-tests-on-ci)
  - [Lint your tests](https://playwright.dev/docs/best-practices#lint-your-tests)
  - [Use parallelism and sharding](https://playwright.dev/docs/best-practices#use-parallelism-and-sharding)
- [Productivity tips](https://playwright.dev/docs/best-practices#productivity-tips)
  - [Use Soft assertions](https://playwright.dev/docs/best-practices#use-soft-assertions)



### 23. Playwright Team — Fixtures (official docs)

- Source: https://playwright.dev/docs/test-fixtures
- Retrieved: 2026-08-29
- Firecrawl status: complete

## Introduction

Playwright Test is based on the concept of test fixtures. Test fixtures are used to establish the environment for each test, giving the test everything it needs and nothing else. Test fixtures are isolated between tests. With fixtures, you can group tests based on their meaning, instead of their common setup.

### Built-in fixtures

You have already used test fixtures in your first test.

```js
import { test, expect } from '@playwright/test';

test('basic test', async ({ page }) => {

  await page.goto('https://playwright.dev/');

  await expect(page).toHaveTitle(/Playwright/);

});
```

The `{ page }` argument tells Playwright Test to set up the `page` fixture and provide it to your test function.

Here is a list of the pre-defined fixtures that you are likely to use most of the time:

| Fixture | Type | Description |
| --- | --- | --- |
| page | [Page](https://playwright.dev/docs/api/class-page "Page") | Isolated page for this test run. |
| context | [BrowserContext](https://playwright.dev/docs/api/class-browsercontext "BrowserContext") | Isolated context for this test run. The `page` fixture belongs to this context as well. Learn how to [configure context](https://playwright.dev/docs/test-configuration). |
| browser | [Browser](https://playwright.dev/docs/api/class-browser "Browser") | Browsers are shared across tests to optimize resources. Learn how to [configure browsers](https://playwright.dev/docs/test-configuration). |
| browserName | [string](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Data_structures#String_type "string") | The name of the browser currently running the test. Either `chromium`, `firefox` or `webkit`. |
| request | [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext "APIRequestContext") | Isolated [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext) instance for this test run. |

### Without fixtures

Here is how a typical test environment setup differs between the traditional test style and the fixture-based one.

`TodoPage` is a class that helps us interact with a "todo list" page of the web app, following the [Page Object Model](https://playwright.dev/docs/pom) pattern. It uses Playwright's `page` internally.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

todo.spec.ts

```js
const { test } = require('@playwright/test');

const { TodoPage } = require('./todo-page');

test.describe('todo tests', () => {

  let todoPage;

  test.beforeEach(async ({ page }) => {

    todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

  });

  test.afterEach(async () => {

    await todoPage.removeAll();

  });

  test('should add an item', async () => {

    await todoPage.addToDo('my item');

    // ...

  });

  test('should remove an item', async () => {

    await todoPage.remove('item1');

    // ...

  });

});
```

### With fixtures

Fixtures have a number of advantages over before/after hooks:

- Fixtures **encapsulate** setup and teardown in the same place so it is easier to write. So if you have an after hook that tears down what was created in a before hook, consider turning them into a fixture.
- Fixtures are **reusable** between test files - you can define them once and use them in all your tests. That's how Playwright's built-in `page` fixture works. So if you have a helper function that is used in multiple tests, consider turning it into a fixture.
- Fixtures are **on-demand** \- you can define as many fixtures as you'd like, and Playwright Test will setup only the ones needed by your test and nothing else.
- Fixtures are **composable** \- they can depend on each other to provide complex behaviors.
- Fixtures are **flexible**. Tests can use any combination of fixtures to precisely tailor the environment to their needs, without affecting other tests.
- Fixtures simplify **grouping**. You no longer need to wrap tests in `describe`s that set up their environment, and are free to group your tests by their meaning instead.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

example.spec.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

// Extend basic test by providing a "todoPage" fixture.

const test = base.extend<{ todoPage: TodoPage }>({

  todoPage: async ({ page }, use) => {

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

    await use(todoPage);

    await todoPage.removeAll();

  },

});

test('should add an item', async ({ todoPage }) => {

  await todoPage.addToDo('my item');

  // ...

});

test('should remove an item', async ({ todoPage }) => {

  await todoPage.remove('item1');

  // ...

});
```

## Creating a fixture

To create your own fixture, use [test.extend()](https://playwright.dev/docs/api/class-test#test-extend) to create a new `test` object that will include it.

Below we create two fixtures `todoPage` and `settingsPage` that follow the [Page Object Model](https://playwright.dev/docs/pom) pattern.

Click to expand the code for the `TodoPage` and `SettingsPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

SettingsPage is similar:

settings-page.ts

```js
import type { Page } from '@playwright/test';

export class SettingsPage {

  constructor(public readonly page: Page) {

  }

  async switchToDarkMode() {

    // ...

  }

}
```

my-test.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

import { SettingsPage } from './settings-page';

// Declare the types of your fixtures.

type MyFixtures = {

  todoPage: TodoPage;

  settingsPage: SettingsPage;

};

// Extend base test by providing "todoPage" and "settingsPage".

// This new "test" can be used in multiple test files, and each of them will get the fixtures.

export const test = base.extend<MyFixtures>({

  todoPage: async ({ page }, use) => {

    // Set up the fixture.

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo('item1');

    await todoPage.addToDo('item2');

    // Use the fixture value in the test.

    await use(todoPage);

    // Clean up the fixture.

    await todoPage.removeAll();

  },

  settingsPage: async ({ page }, use) => {

    await use(new SettingsPage(page));

  },

});

export { expect } from '@playwright/test';
```

note

Custom fixture names should start with a letter or underscore, and can contain only letters, numbers, and underscores.

## Using a fixture

Just mention a fixture in your test function argument, and the test runner will take care of it. Fixtures are also available in hooks and other fixtures. If you use TypeScript, fixtures will be type safe.

Below we use the `todoPage` and `settingsPage` fixtures that we defined above.

```js
import { test, expect } from './my-test';

test.beforeEach(async ({ settingsPage }) => {

  await settingsPage.switchToDarkMode();

});

test('basic test', async ({ todoPage, page }) => {

  await todoPage.addToDo('something nice');

  await expect(page.getByTestId('todo-title')).toContainText(['something nice']);

});
```

## Overriding fixtures

In addition to creating your own fixtures, you can also override existing fixtures to fit your needs. Consider the following example which overrides the `page` fixture by automatically navigating to the `baseURL`:

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  page: async ({ baseURL, page }, use) => {

    await page.goto(baseURL);

    await use(page);

  },

});
```

Notice that in this example, the `page` fixture is able to depend on other built-in fixtures such as [testOptions.baseURL](https://playwright.dev/docs/api/class-testoptions#test-options-base-url). We can now configure `baseURL` in the configuration file, or locally in the test file with [test.use()](https://playwright.dev/docs/api/class-test#test-use).

example.spec.ts

```js

test.use({ baseURL: 'https://playwright.dev' });
```

Fixtures can also be overridden, causing the base fixture to be completely replaced with something different. For example, we could override the [testOptions.storageState](https://playwright.dev/docs/api/class-testoptions#test-options-storage-state) fixture to provide our own data.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  storageState: async ({}, use) => {

    const cookie = await getAuthCookie();

    await use({ cookies: [cookie] });

  },

});
```

## Worker-scoped fixtures

Playwright Test uses [worker processes](https://playwright.dev/docs/test-parallel) to run test files. Similar to how test fixtures are set up for individual test runs, worker fixtures are set up for each worker process. That's where you can set up services, run servers, etc. Playwright Test will reuse the worker process for as many test files as it can, provided their worker fixtures match and hence environments are identical.

Below we'll create an `account` fixture that will be shared by all tests in the same worker, and override the `page` fixture to log in to this account for each test. To generate unique accounts, we'll use the [workerInfo.workerIndex](https://playwright.dev/docs/api/class-workerinfo#worker-info-worker-index) that is available to any test or fixture. Note the tuple-like syntax for the worker fixture - we have to pass `{scope: 'worker'}` so that test runner sets this fixture up once per worker.

In addition to only being run once per worker, worker-scoped fixtures also get a separate timeout equal to the default test timeout. You can change it by passing the `timeout` option. See [fixture timeout](https://playwright.dev/docs/test-fixtures#fixture-timeout) for more details.

my-test.ts

```js
import { test as base } from '@playwright/test';

type Account = {

  username: string;

  password: string;

};

// Note that we pass worker fixture types as a second template parameter.

export const test = base.extend<{}, { account: Account }>({

  account: [async ({ browser }, use, workerInfo) => {

    // Unique username.

    const username = 'user' + workerInfo.workerIndex;

    const password = 'verysecure';

    // Create the account with Playwright.

    const page = await browser.newPage();

    await page.goto('/signup');

    await page.getByLabel('User Name').fill(username);

    await page.getByLabel('Password').fill(password);

    await page.getByText('Sign up').click();

    // Make sure everything is ok.

    await expect(page.getByTestId('result')).toHaveText('Success');

    // Do not forget to cleanup.

    await page.close();

    // Use the account value.

    await use({ username, password });

  }, { scope: 'worker' }],

  page: async ({ page, account }, use) => {

    // Sign in with our account.

    const { username, password } = account;

    await page.goto('/signin');

    await page.getByLabel('User Name').fill(username);

    await page.getByLabel('Password').fill(password);

    await page.getByText('Sign in').click();

    await expect(page.getByTestId('userinfo')).toHaveText(username);

    // Use signed-in page in the test.

    await use(page);

  },

});

export { expect } from '@playwright/test';
```

## Automatic fixtures

Automatic fixtures are set up for each test/worker, even when the test does not list them directly. To create an automatic fixture, use the tuple syntax and pass `{ auto: true }`.

Here is an example fixture that automatically attaches debug logs when the test fails, so we can later review the logs in the reporter. Note how it uses the [TestInfo](https://playwright.dev/docs/api/class-testinfo "TestInfo") object that is available in each test/fixture to retrieve metadata about the test being run.

my-test.ts

```js
import debug from 'debug';

import fs from 'fs';

import { test as base } from '@playwright/test';

export const test = base.extend<{ saveLogs: void }>({

  saveLogs: [async ({}, use, testInfo) => {

    // Collecting logs during the test.

    const logs = [];

    debug.log = (...args) => logs.push(args.map(String).join(''));

    debug.enable('myserver');

    await use();

    // After the test we can check whether the test passed or failed.

    if (testInfo.status !== testInfo.expectedStatus) {

      // outputPath() API guarantees a unique file name.

      const logFile = testInfo.outputPath('logs.txt');

      await fs.promises.writeFile(logFile, logs.join('\n'), 'utf8');

      testInfo.attachments.push({ name: 'logs', contentType: 'text/plain', path: logFile });

    }

  }, { auto: true }],

});

export { expect } from '@playwright/test';
```

## Fixture timeout

Fixture is considered to be a part of a test, and so its setup and teardown running time counts towards the test timeout. Therefore, a slow fixture may cause test timeouts. You can set a separate larger timeout for such a fixture, and keep the overall test timeout small.

```js
import { test as base, expect } from '@playwright/test';

const test = base.extend<{ slowFixture: string }>({

  slowFixture: [async ({}, use) => {

    // ... perform a slow operation ...

    await use('hello');

  }, { timeout: 60000 }]

});

test('example test', async ({ slowFixture }) => {

  // ...

});
```

Unlike regular test-scoped fixtures, each [worker-scoped](https://playwright.dev/docs/test-fixtures#worker-scoped-fixtures) fixture has its own timeout, equal to the test timeout. You can change the timeout for a worker-scoped fixture in the same way.

## Fixtures-options

Playwright Test supports running multiple test projects that can be configured separately. You can use "option" fixtures to make your configuration options declarative and type safe. Learn more about [parameterizing tests](https://playwright.dev/docs/test-parameterize).

Below we'll create a `defaultItem` option in addition to the `todoPage` fixture from other examples. This option will be set in the configuration file. Note the tuple syntax and `{ option: true }` argument.

Click to expand the code for the `TodoPage`

todo-page.ts

```js
import type { Page, Locator } from '@playwright/test';

export class TodoPage {

  private readonly inputBox: Locator;

  private readonly todoItems: Locator;

  constructor(public readonly page: Page) {

    this.inputBox = this.page.locator('input.new-todo');

    this.todoItems = this.page.getByTestId('todo-item');

  }

  async goto() {

    await this.page.goto('https://demo.playwright.dev/todomvc/');

  }

  async addToDo(text: string) {

    await this.inputBox.fill(text);

    await this.inputBox.press('Enter');

  }

  async remove(text: string) {

    const todo = this.todoItems.filter({ hasText: text });

    await todo.hover();

    await todo.getByLabel('Delete').click();

  }

  async removeAll() {

    while ((await this.todoItems.count()) > 0) {

      await this.todoItems.first().hover();

      await this.todoItems.getByLabel('Delete').first().click();

    }

  }

}
```

my-test.ts

```js
import { test as base } from '@playwright/test';

import { TodoPage } from './todo-page';

// Declare your options to type-check your configuration.

export type MyOptions = {

  defaultItem: string;

};

type MyFixtures = {

  todoPage: TodoPage;

};

// Specify both option and fixture types.

export const test = base.extend<MyOptions & MyFixtures>({

  // Define an option and provide a default value.

  // We can later override it in the config.

  defaultItem: ['Something nice', { option: true }],

  // Our "todoPage" fixture depends on the option.

  todoPage: async ({ page, defaultItem }, use) => {

    const todoPage = new TodoPage(page);

    await todoPage.goto();

    await todoPage.addToDo(defaultItem);

    await use(todoPage);

    await todoPage.removeAll();

  },

});

export { expect } from '@playwright/test';
```

We can now use the `todoPage` fixture as usual, and set the `defaultItem` option in the configuration file.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

import type { MyOptions } from './my-test';

export default defineConfig<MyOptions>({

  projects: [

    {

      name: 'shopping',

      use: { defaultItem: 'Buy milk' },

    },

    {

      name: 'wellbeing',

      use: { defaultItem: 'Exercise!' },

    },

  ]

});
```

**Array as an option value**

If the value of your option is an array, for example `[{ name: 'Alice' }, { name: 'Bob' }]`, you'll need to wrap it into an extra array when providing the value. This is best illustrated with an example.

```js
type Person = { name: string };

const test = base.extend<{ persons: Person[] }>({

  // Declare the option, default value is an empty array.

  persons: [[], { option: true }],

});

// Option value is an array of persons.

const actualPersons = [{ name: 'Alice' }, { name: 'Bob' }];

test.use({

  // CORRECT: Wrap the value into an array and pass the scope.

  persons: [actualPersons, { scope: 'test' }],

});

test.use({

  // WRONG: passing an array value directly will not work.

  persons: actualPersons,

});
```

**Reset an option**

You can reset an option to the value defined in the config file by setting it to `undefined`. Consider the following config that sets a `baseURL`:

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  use: {

    baseURL: 'https://playwright.dev',

  },

});
```

You can now configure `baseURL` for a file, and also opt-out for a single test.

intro.spec.ts

```js
import { test } from '@playwright/test';

// Configure baseURL for this file.

test.use({ baseURL: 'https://playwright.dev/docs/intro' });

test('check intro contents', async ({ page }) => {

  // This test will use "https://playwright.dev/docs/intro" base url as defined above.

});

test.describe(() => {

  // Reset the value to a config-defined one.

  test.use({ baseURL: undefined });

  test('can navigate to intro from the home page', async ({ page }) => {

    // This test will use "https://playwright.dev" base url as defined in the config.

  });

});
```

If you would like to completely reset the value to `undefined`, use a long-form fixture notation.

intro.spec.ts

```js
import { test } from '@playwright/test';

// Completely unset baseURL for this file.

test.use({

  baseURL: [async ({}, use) => use(undefined), { scope: 'test' }],

});

test('no base url', async ({ page }) => {

  // This test will not have a base url.

});
```

## Execution order

Each fixture has a setup and teardown phase before and after the `await use()` call in the fixture. Setup is executed before the test/hook requiring it is run, and teardown is executed when the fixture is no longer being used by the test/hook.

Fixtures follow these rules to determine the execution order:

- When fixture A depends on fixture B: B is always set up before A and torn down after A.
- Non-automatic fixtures are executed lazily, only when the test/hook needs them.
- Test-scoped fixtures are torn down after each test, while worker-scoped fixtures are only torn down when the worker process executing tests is torn down.

Consider the following example:

```js
import { test as base } from '@playwright/test';

const test = base.extend<{

  testFixture: string,

  autoTestFixture: string,

  unusedFixture: string,

}, {

  workerFixture: string,

  autoWorkerFixture: string,

}>({

  workerFixture: [async ({ browser }) => {

    // workerFixture setup...

    await use('workerFixture');

    // workerFixture teardown...

  }, { scope: 'worker' }],

  autoWorkerFixture: [async ({ browser }) => {

    // autoWorkerFixture setup...

    await use('autoWorkerFixture');

    // autoWorkerFixture teardown...

  }, { scope: 'worker', auto: true }],

  testFixture: [async ({ page, workerFixture }) => {

    // testFixture setup...

    await use('testFixture');

    // testFixture teardown...

  }, { scope: 'test' }],

  autoTestFixture: [async () => {

    // autoTestFixture setup...

    await use('autoTestFixture');

    // autoTestFixture teardown...

  }, { scope: 'test', auto: true }],

  unusedFixture: [async ({ page }) => {

    // unusedFixture setup...

    await use('unusedFixture');

    // unusedFixture teardown...

  }, { scope: 'test' }],

});

test.beforeAll(async () => { /* ... */ });

test.beforeEach(async ({ page }) => { /* ... */ });

test('first test', async ({ page }) => { /* ... */ });

test('second test', async ({ testFixture }) => { /* ... */ });

test.afterEach(async () => { /* ... */ });

test.afterAll(async () => { /* ... */ });
```

Normally, if all tests pass and no errors are thrown, the order of execution is as following.

- worker setup and `beforeAll` section:
  - `browser` setup because it is required by `autoWorkerFixture`.
  - `autoWorkerFixture` setup because automatic worker fixtures are always set up before anything else.
  - `beforeAll` runs.
- `first test`section:
  - `autoTestFixture` setup because automatic test fixtures are always set up before test and `beforeEach` hooks.
  - `page` setup because it is required in `beforeEach` hook.
  - `beforeEach` runs.
  - `first test` runs.
  - `afterEach` runs.
  - `page` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `autoTestFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
- `second test`section:
  - `autoTestFixture` setup because automatic test fixtures are always set up before test and `beforeEach` hooks.
  - `page` setup because it is required in `beforeEach` hook.
  - `beforeEach` runs.
  - `workerFixture` setup because it is required by `testFixture` that is required by the `second test`.
  - `testFixture` setup because it is required by the `second test`.
  - `second test` runs.
  - `afterEach` runs.
  - `testFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `page` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
  - `autoTestFixture` teardown because it is a test-scoped fixture and should be torn down after the test finishes.
- `afterAll`and worker teardown section:
  - `afterAll` runs.
  - `workerFixture` teardown because it is a workers-scoped fixture and should be torn down once at the end.
  - `autoWorkerFixture` teardown because it is a workers-scoped fixture and should be torn down once at the end.
  - `browser` teardown because it is a workers-scoped fixture and should be torn down once at the end.

A few observations:

- `page` and `autoTestFixture` are set up and torn down for each test, as test-scoped fixtures.
- `unusedFixture` is never set up because it is not used by any tests/hooks.
- `testFixture` depends on `workerFixture` and triggers its setup.
- `workerFixture` is lazily set up before the second test, but torn down once during worker shutdown, as a worker-scoped fixture.
- `autoWorkerFixture` is set up for `beforeAll` hook, but `autoTestFixture` is not.

## Combine custom fixtures from multiple modules

You can merge test fixtures from multiple files or modules:

fixtures.ts

```js
import { mergeTests } from '@playwright/test';

import { test as dbTest } from 'database-test-utils';

import { test as a11yTest } from 'a11y-test-utils';

export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test } from './fixtures';

test('passes', async ({ database, page, a11y }) => {

  // use database and a11y fixtures.

});
```

## Box fixtures

Usually, custom fixtures are reported as separate steps in the UI mode, Trace Viewer and various test reports. They also appear in error messages from the test runner. For frequently used fixtures, this can mean lots of noise. You can stop the fixtures steps from being shown in the UI by "boxing" it.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  helperFixture: [async ({}, use, testInfo) => {

    // ...

  }, { box: true }],

});
```

This is useful for non-interesting helper fixtures. For example, an [automatic](https://playwright.dev/docs/test-fixtures#automatic-fixtures) fixture that sets up some common data can be safely hidden from a test report.

You can also mark the fixture as `box: 'self'` to only hide that particular fixture, but include all the steps inside the fixture in the test report.

## Custom fixture title

Instead of the usual fixture name, you can give fixtures a custom title that will be shown in test reports and error messages.

```js
import { test as base } from '@playwright/test';

export const test = base.extend({

  innerFixture: [async ({}, use, testInfo) => {

    // ...

  }, { title: 'my fixture' }],

});
```

## Adding global beforeEach/afterEach hooks

[test.beforeEach()](https://playwright.dev/docs/api/class-test#test-before-each) and [test.afterEach()](https://playwright.dev/docs/api/class-test#test-after-each) hooks run before/after each test declared in the same file and same [test.describe()](https://playwright.dev/docs/api/class-test#test-describe) block (if any). If you want to declare hooks that run before/after each test globally, you can declare them as auto fixtures like this:

fixtures.ts

```js
import { test as base } from '@playwright/test';

export const test = base.extend<{ forEachTest: void }>({

  forEachTest: [async ({ page }, use) => {

    // This code runs before every test.

    await page.goto('http://localhost:8000');

    await use();

    // This code runs after every test.

    console.log('Last URL:', page.url());

  }, { auto: true }],  // automatically starts for every test.

});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';

import { expect } from '@playwright/test';

test('basic', async ({ page }) => {

  expect(page).toHaveURL('http://localhost:8000');

  await page.goto('https://playwright.dev');

});
```

## Adding global beforeAll/afterAll hooks

[test.beforeAll()](https://playwright.dev/docs/api/class-test#test-before-all) and [test.afterAll()](https://playwright.dev/docs/api/class-test#test-after-all) hooks run before/after all tests declared in the same file and same [test.describe()](https://playwright.dev/docs/api/class-test#test-describe) block (if any), once per worker process. If you want to declare hooks that run before/after all tests in every file, you can declare them as auto fixtures with `scope: 'worker'` as follows:

fixtures.ts

```js
import { test as base } from '@playwright/test';

export const test = base.extend<{}, { forEachWorker: void }>({

  forEachWorker: [async ({}, use) => {

    // This code runs before all the tests in the worker process.

    console.log(`Starting test worker ${test.info().workerIndex}`);

    await use();

    // This code runs after all the tests in the worker process.

    console.log(`Stopping test worker ${test.info().workerIndex}`);

  }, { scope: 'worker', auto: true }],  // automatically starts for every worker.

});
```

And then import the fixtures in all your tests:

mytest.spec.ts

```js
import { test } from './fixtures';

import { expect } from '@playwright/test';

test('basic', async ({ }) => {

  // ...

});
```

Note that the fixtures will still run once per [worker process](https://playwright.dev/docs/test-parallel#worker-processes), but you don't need to redeclare them in every file.

- [Introduction](https://playwright.dev/docs/test-fixtures#introduction)
  - [Built-in fixtures](https://playwright.dev/docs/test-fixtures#built-in-fixtures)
  - [Without fixtures](https://playwright.dev/docs/test-fixtures#without-fixtures)
  - [With fixtures](https://playwright.dev/docs/test-fixtures#with-fixtures)
- [Creating a fixture](https://playwright.dev/docs/test-fixtures#creating-a-fixture)
- [Using a fixture](https://playwright.dev/docs/test-fixtures#using-a-fixture)
- [Overriding fixtures](https://playwright.dev/docs/test-fixtures#overriding-fixtures)
- [Worker-scoped fixtures](https://playwright.dev/docs/test-fixtures#worker-scoped-fixtures)
- [Automatic fixtures](https://playwright.dev/docs/test-fixtures#automatic-fixtures)
- [Fixture timeout](https://playwright.dev/docs/test-fixtures#fixture-timeout)
- [Fixtures-options](https://playwright.dev/docs/test-fixtures#fixtures-options)
- [Execution order](https://playwright.dev/docs/test-fixtures#execution-order)
- [Combine custom fixtures from multiple modules](https://playwright.dev/docs/test-fixtures#combine-custom-fixtures-from-multiple-modules)
- [Box fixtures](https://playwright.dev/docs/test-fixtures#box-fixtures)
- [Custom fixture title](https://playwright.dev/docs/test-fixtures#custom-fixture-title)
- [Adding global beforeEach/afterEach hooks](https://playwright.dev/docs/test-fixtures#adding-global-beforeeachaftereach-hooks)
- [Adding global beforeAll/afterAll hooks](https://playwright.dev/docs/test-fixtures#adding-global-beforeallafterall-hooks)



### 24. Playwright Team — Assertions (official docs)

- Source: https://playwright.dev/docs/test-assertions
- Retrieved: 2026-08-29
- Firecrawl status: complete

## Introduction

Playwright includes test assertions in the form of `expect` function. To make an assertion, call `expect(value)` and choose a matcher that reflects the expectation. There are many [generic matchers](https://playwright.dev/docs/api/class-genericassertions) like `toEqual`, `toContain`, `toBeTruthy` that can be used to assert any conditions.

```js
expect(success).toBeTruthy();
```

Playwright also includes web-specific [async matchers](https://playwright.dev/docs/api/class-locatorassertions) that will wait until the expected condition is met. Consider the following example:

```js
await expect(page.getByTestId('status')).toHaveText('Submitted');
```

Playwright will be re-testing the element with the test id of `status` until the fetched element has the `"Submitted"` text. It will re-fetch the element and check it over and over, until the condition is met or until the timeout is reached. You can either pass this timeout or configure it once via the [testConfig.expect](https://playwright.dev/docs/api/class-testconfig#test-config-expect) value in the test config.

By default, the timeout for assertions is set to 5 seconds. Learn more about [various timeouts](https://playwright.dev/docs/test-timeouts).

## Auto-retrying assertions

The following assertions will retry until the assertion passes, or the assertion timeout is reached. Note that retrying assertions are async, so you must `await` them.

| Assertion | Description |
| --- | --- |
| [await expect(locator).toBeAttached()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-attached) | Element is attached |
| [await expect(locator).toBeChecked()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-checked) | Checkbox is checked |
| [await expect(locator).toBeDisabled()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-disabled) | Element is disabled |
| [await expect(locator).toBeEditable()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-editable) | Element is editable |
| [await expect(locator).toBeEmpty()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-empty) | Container is empty |
| [await expect(locator).toBeEnabled()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-enabled) | Element is enabled |
| [await expect(locator).toBeFocused()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-focused) | Element is focused |
| [await expect(locator).toBeHidden()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-hidden) | Element is not visible |
| [await expect(locator).toBeInViewport()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-in-viewport) | Element intersects viewport |
| [await expect(locator).toBeVisible()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-be-visible) | Element is visible |
| [await expect(locator).toContainText()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-contain-text) | Element contains text |
| [await expect(locator).toContainClass()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-contain-class) | Element has specified CSS classes |
| [await expect(locator).toHaveAccessibleDescription()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-accessible-description) | Element has a matching [accessible description](https://w3c.github.io/accname/#dfn-accessible-description) |
| [await expect(locator).toHaveAccessibleName()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-accessible-name) | Element has a matching [accessible name](https://w3c.github.io/accname/#dfn-accessible-name) |
| [await expect(locator).toHaveAttribute()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-attribute) | Element has a DOM attribute |
| [await expect(locator).toHaveClass()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-class) | Element has specified CSS class property |
| [await expect(locator).toHaveCount()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-count) | List has exact number of children |
| [await expect(locator).toHaveCSS()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-css) | Element has CSS property |
| [await expect(locator).toHaveId()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-id) | Element has an ID |
| [await expect(locator).toHaveJSProperty()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-js-property) | Element has a JavaScript property |
| [await expect(locator).toHaveRole()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-role) | Element has a specific [ARIA role](https://www.w3.org/TR/wai-aria-1.2/#roles) |
| [await expect(locator).toHaveScreenshot()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-screenshot-1) | Element has a screenshot |
| [await expect(locator).toHaveText()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-text) | Element matches text |
| [await expect(locator).toHaveValue()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-value) | Input has a value |
| [await expect(locator).toHaveValues()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-have-values) | Select has options selected |
| [await expect(locator).toMatchAriaSnapshot()](https://playwright.dev/docs/api/class-locatorassertions#locator-assertions-to-match-aria-snapshot) | Element matches the Aria snapshot |
| [await expect(page).toMatchAriaSnapshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-match-aria-snapshot) | Page matches the Aria snapshot |
| [await expect(page).toHaveScreenshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-screenshot-1) | Page has a screenshot |
| [await expect(page).toHaveTitle()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-title) | Page has a title |
| [await expect(page).toHaveURL()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-url) | Page has a URL |
| [await expect(response).toBeOK()](https://playwright.dev/docs/api/class-apiresponseassertions#api-response-assertions-to-be-ok) | Response has an OK status |

## Non-retrying assertions

These assertions allow to test any conditions, but do not auto-retry. Most of the time, web pages show information asynchronously, and using non-retrying assertions can lead to a flaky test.

Prefer [auto-retrying](https://playwright.dev/docs/test-assertions#auto-retrying-assertions) assertions whenever possible. For more complex assertions that need to be retried, use [`expect.poll`](https://playwright.dev/docs/test-assertions#expectpoll) or [`expect.toPass`](https://playwright.dev/docs/test-assertions#expecttopass).

| Assertion | Description |
| --- | --- |
| [expect(value).toBe()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be) | Value is the same |
| [expect(value).toBeCloseTo()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-close-to) | Number is approximately equal |
| [expect(value).toBeDefined()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-defined) | Value is not `undefined` |
| [expect(value).toBeFalsy()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-falsy) | Value is falsy, e.g. `false`, `0`, `null`, etc. |
| [expect(value).toBeGreaterThan()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-greater-than) | Number is more than |
| [expect(value).toBeGreaterThanOrEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-greater-than-or-equal) | Number is more than or equal |
| [expect(value).toBeInstanceOf()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-instance-of) | Object is an instance of a class |
| [expect(value).toBeLessThan()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-less-than) | Number is less than |
| [expect(value).toBeLessThanOrEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-less-than-or-equal) | Number is less than or equal |
| [expect(value).toBeNaN()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-na-n) | Value is `NaN` |
| [expect(value).toBeNull()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-null) | Value is `null` |
| [expect(value).toBeTruthy()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-truthy) | Value is truthy, i.e. not `false`, `0`, `null`, etc. |
| [expect(value).toBeUndefined()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-be-undefined) | Value is `undefined` |
| [expect(value).toContain()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-1) | String contains a substring |
| [expect(value).toContain()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-2) | Array or set contains an element |
| [expect(value).toContainEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-contain-equal) | Array or set contains a similar element |
| [expect(value).toEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-equal) | Value is similar - deep equality and pattern matching |
| [expect(value).toHaveLength()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-have-length) | Array or string has length |
| [expect(value).toHaveProperty()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-have-property) | Object has a property |
| [expect(value).toMatch()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-match) | String matches a regular expression |
| [expect(value).toMatchObject()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-match-object) | Object contains specified properties |
| [expect(value).toStrictEqual()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-strict-equal) | Value is similar, including property types |
| [expect(value).toThrow()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-to-throw) | Function throws an error |

## Asymmetric matchers

These expressions can be nested in other assertions to allow more relaxed matching against a given condition.

| Matcher | Description |
| --- | --- |
| [expect.any()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-any) | Matches any instance of a class/primitive |
| [expect.anything()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-anything) | Matches anything |
| [expect.arrayContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-array-containing) | Array contains specific elements |
| [expect.arrayOf()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-array-of) | Array contains elements of specific type |
| [expect.closeTo()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-close-to) | Number is approximately equal |
| [expect.objectContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-object-containing) | Object contains specific properties |
| [expect.stringContaining()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-string-containing) | String contains a substring |
| [expect.stringMatching()](https://playwright.dev/docs/api/class-genericassertions#generic-assertions-string-matching) | String matches a regular expression |

## Negating matchers

In general, we can expect the opposite to be true by adding a `.not` to the front of the matchers:

```js
expect(value).not.toEqual(0);

await expect(locator).not.toContainText('some text');
```

## Soft assertions

By default, failed assertion will terminate test execution. Playwright also supports _soft assertions_: failed soft assertions **do not** terminate test execution, but mark the test as failed.

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

await expect.soft(page.getByTestId('eta')).toHaveText('1 day');

// ... and continue the test to check more things.

await page.getByRole('link', { name: 'next page' }).click();

await expect.soft(page.getByRole('heading', { name: 'Make another order' })).toBeVisible();
```

At any point during test execution, you can check whether there were any soft assertion failures:

```js
// Make a few checks that will not stop the test when failed...

await expect.soft(page.getByTestId('status')).toHaveText('Success');

await expect.soft(page.getByTestId('eta')).toHaveText('1 day');

// Avoid running further if there were soft assertion failures.

expect(test.info().errors).toHaveLength(0);
```

Note that soft assertions only work with Playwright test runner.

## Custom expect message

You can specify a custom expect message as a second argument to the `expect` function, for example:

```js
await expect(page.getByText('Name'), 'should be logged in').toBeVisible();
```

This message will be shown in reporters, both for passing and failing expects, providing more context about the assertion.

When expect passes, you might see a successful step like this:

```txt
✅ should be logged in    @example.spec.ts:18
```

When expect fails, the error would look like this:

```bash
    Error: should be logged in

    Call log:

      - expect.toBeVisible with timeout 5000ms

      - waiting for "getByText('Name')"

      2 |

      3 | test('example test', async({ page }) => {

    > 4 |   await expect(page.getByText('Name'), 'should be logged in').toBeVisible();

        |                                                                  ^

      5 | });

      6 |
```

Soft assertions also support custom message:

```js
expect.soft(value, 'my soft assertion').toBe(56);
```

## expect.configure

You can create your own pre-configured `expect` instance to have its own defaults such as `timeout` and `soft`.

```js
const slowExpect = expect.configure({ timeout: 10000 });

await slowExpect(locator).toHaveText('Submit');

// Always do soft assertions.

const softExpect = expect.configure({ soft: true });

await softExpect(locator).toHaveText('Submit');
```

## expect.poll

You can convert any synchronous `expect` to an asynchronous polling one using `expect.poll`.

The following method will poll given function until it returns HTTP status 200:

```js
await expect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}, {

  // Custom expect message for reporting, optional.

  message: 'make sure API eventually succeeds',

  // Poll for 10 seconds; defaults to 5 seconds. Pass 0 to disable timeout.

  timeout: 10000,

}).toBe(200);
```

You can also specify custom polling intervals:

```js
await expect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}, {

  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe

  // ... Defaults to [100, 250, 500, 1000].

  intervals: [1_000, 2_000, 10_000],

  timeout: 60_000

}).toBe(200);
```

You can combine `expect.soft` with `expect.poll` to perform soft assertions in polling logic. This allows the test to continue even if the assertion inside poll fails.

```js
await expect.soft.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}).toBe(200);
```

`expect.configure({ soft: true })` also chains with `expect.poll` and is useful when you want to reuse a configured instance.

```js
const softExpect = expect.configure({ soft: true });

await softExpect.poll(async () => {

  const response = await page.request.get('https://api.example.com');

  return response.status();

}).toBe(200);
```

## expect.toPass

You can retry blocks of code until they are passing successfully.

```js
await expect(async () => {

  const response = await page.request.get('https://api.example.com');

  expect(response.status()).toBe(200);

}).toPass();
```

You can also specify custom timeout and retry intervals:

```js
await expect(async () => {

  const response = await page.request.get('https://api.example.com');

  expect(response.status()).toBe(200);

}).toPass({

  // Probe, wait 1s, probe, wait 2s, probe, wait 10s, probe, wait 10s, probe

  // ... Defaults to [100, 250, 500, 1000].

  intervals: [1_000, 2_000, 10_000],

  timeout: 60_000

});
```

Note that by default `toPass` has timeout 0 and does not respect custom [expect timeout](https://playwright.dev/docs/test-timeouts#expect-timeout).

## Add custom matchers using expect.extend

You can extend Playwright assertions by providing custom matchers. These matchers will be available on the `expect` object.

In this example we add a custom `toHaveAmount` function. Custom matcher should return a `pass` flag indicating whether the assertion passed, and a `message` callback that's used when the assertion fails.

fixtures.ts

```js
import { expect as baseExpect } from '@playwright/test';

import type { Locator } from '@playwright/test';

export { test } from '@playwright/test';

export const expect = baseExpect.extend({

  async toHaveAmount(locator: Locator, expected: number, options?: { timeout?: number }) {

    const assertionName = 'toHaveAmount';

    let pass: boolean;

    let matcherResult: any;

    try {

      const expectation = this.isNot ? baseExpect(locator).not : baseExpect(locator);

      await expectation.toHaveAttribute('data-amount', String(expected), options);

      pass = true;

    } catch (e: any) {

      matcherResult = e.matcherResult;

      pass = false;

    }

    if (this.isNot) {

      pass =!pass;

    }

    const message = pass

      ? () => this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +

          '\n\n' +

          `Locator: ${locator}\n` +

          `Expected: not ${this.utils.printExpected(expected)}\n` +

          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '')

      : () =>  this.utils.matcherHint(assertionName, undefined, undefined, { isNot: this.isNot }) +

          '\n\n' +

          `Locator: ${locator}\n` +

          `Expected: ${this.utils.printExpected(expected)}\n` +

          (matcherResult ? `Received: ${this.utils.printReceived(matcherResult.actual)}` : '');

    return {

      message,

      pass,

      name: assertionName,

      expected,

      actual: matcherResult?.actual,

    };

  },

});
```

Now we can use `toHaveAmount` in the test.

example.spec.ts

```js
import { test, expect } from './fixtures';

test('amount', async () => {

  await expect(page.locator('.cart')).toHaveAmount(4);

});
```

### Compatibility with expect library

note

Do not confuse Playwright's `expect` with the [`expect` library](https://jestjs.io/docs/expect). The latter is not fully integrated with Playwright test runner, so make sure to use Playwright's own `expect`.

### Combine custom matchers from multiple modules

You can combine custom matchers from multiple files or modules.

fixtures.ts

```js
import { mergeTests, mergeExpects } from '@playwright/test';

import { test as dbTest, expect as dbExpect } from 'database-test-utils';

import { test as a11yTest, expect as a11yExpect } from 'a11y-test-utils';

export const expect = mergeExpects(dbExpect, a11yExpect);

export const test = mergeTests(dbTest, a11yTest);
```

test.spec.ts

```js
import { test, expect } from './fixtures';

test('passes', async ({ database }) => {

  await expect(database).toHaveDatabaseUser('admin');

});
```

- [Introduction](https://playwright.dev/docs/test-assertions#introduction)
- [Auto-retrying assertions](https://playwright.dev/docs/test-assertions#auto-retrying-assertions)
- [Non-retrying assertions](https://playwright.dev/docs/test-assertions#non-retrying-assertions)
- [Asymmetric matchers](https://playwright.dev/docs/test-assertions#asymmetric-matchers)
- [Negating matchers](https://playwright.dev/docs/test-assertions#negating-matchers)
- [Soft assertions](https://playwright.dev/docs/test-assertions#soft-assertions)
- [Custom expect message](https://playwright.dev/docs/test-assertions#custom-expect-message)
- [expect.configure](https://playwright.dev/docs/test-assertions#expectconfigure)
- [expect.poll](https://playwright.dev/docs/test-assertions#expectpoll)
- [expect.toPass](https://playwright.dev/docs/test-assertions#expecttopass)
- [Add custom matchers using expect.extend](https://playwright.dev/docs/test-assertions#add-custom-matchers-using-expectextend)
  - [Compatibility with expect library](https://playwright.dev/docs/test-assertions#compatibility-with-expect-library)
  - [Combine custom matchers from multiple modules](https://playwright.dev/docs/test-assertions#combine-custom-matchers-from-multiple-modules)



### 25. Playwright Team — Configuration (official docs)

- Source: https://playwright.dev/docs/test-configuration
- Retrieved: 2026-08-29
- Firecrawl status: complete

# Configuration | Playwright

## Introduction

Playwright has many options to configure how your tests are run. You can specify these options in the configuration file. Note that test runner options are **top-level**, do not put them into the `use` section.

## Basic Configuration

Here are some of the most common configuration options.

```js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

  // Look for test files in the "tests" directory, relative to this configuration file.
  testDir: 'tests',

  // Run all tests in parallel.
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code.
  forbidOnly: !!process.env.CI,

  // Retry on CI only.
  retries: process.env.CI ? 2 : 0,

  // Opt out of parallel tests on CI.
  workers: process.env.CI ? 1 : undefined,

  // Reporter to use
  reporter: 'html',

  use: {
    // Base URL to use in actions like `await page.goto('/')`.
    baseURL: 'http://localhost:3000',

    // Collect trace when retrying the failed test.
    trace: 'on-first-retry',
  },

  // Configure projects for major browsers.
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // Run your local dev server before starting the tests.
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },

});
```

| Option | Description |
| --- | --- |
| [testConfig.forbidOnly](https://playwright.dev/docs/api/class-testconfig#test-config-forbid-only) | Whether to exit with an error if any tests are marked as `test.only`. Useful on CI. |
| [testConfig.fullyParallel](https://playwright.dev/docs/api/class-testconfig#test-config-fully-parallel) | have all tests in all files to run in parallel. See [Parallelism](https://playwright.dev/docs/test-parallel) and [Sharding](https://playwright.dev/docs/test-sharding) for more details. |
| [testConfig.projects](https://playwright.dev/docs/api/class-testconfig#test-config-projects) | Run tests in multiple configurations or on multiple browsers |
| [testConfig.reporter](https://playwright.dev/docs/api/class-testconfig#test-config-reporter) | Reporter to use. See [Test Reporters](https://playwright.dev/docs/test-reporters) to learn more about which reporters are available. |
| [testConfig.retries](https://playwright.dev/docs/api/class-testconfig#test-config-retries) | The maximum number of retry attempts per test. See [Test Retries](https://playwright.dev/docs/test-retries) to learn more about retries. |
| [testConfig.testDir](https://playwright.dev/docs/api/class-testconfig#test-config-test-dir) | Directory with the test files. |
| [testConfig.use](https://playwright.dev/docs/api/class-testconfig#test-config-use) | Options with `use{}` |
| [testConfig.webServer](https://playwright.dev/docs/api/class-testconfig#test-config-web-server) | To launch a server during the tests, use the `webServer` option |
| [testConfig.workers](https://playwright.dev/docs/api/class-testconfig#test-config-workers) | The maximum number of concurrent worker processes to use for parallelizing tests. Can also be set as percentage of logical CPU cores, e.g. `'50%'.`. See [Parallelism](https://playwright.dev/docs/test-parallel) and [Sharding](https://playwright.dev/docs/test-sharding) for more details. |

## Filtering Tests

Filter tests by glob patterns or regular expressions.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Glob patterns or regular expressions to ignore test files.
  testIgnore: '*test-assets',

  // Glob patterns or regular expressions that match test files.
  testMatch: '*todo-tests/*.spec.ts',

});
```

| Option | Description |
| --- | --- |
| [testConfig.testIgnore](https://playwright.dev/docs/api/class-testconfig#test-config-test-ignore) | Glob patterns or regular expressions that should be ignored when looking for the test files. For example, `'*test-assets'` |
| [testConfig.testMatch](https://playwright.dev/docs/api/class-testconfig#test-config-test-match) | Glob patterns or regular expressions that match test files. For example, `'*todo-tests/*.spec.ts'`. By default, Playwright runs `.*(test|spec).(js|ts|mjs)` files. |

## Advanced Configuration

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({

  // Folder for test artifacts such as screenshots, videos, traces, etc.
  outputDir: 'test-results',

  // path to the global setup files.
  globalSetup: require.resolve('./global-setup'),

  // path to the global teardown files.
  globalTeardown: require.resolve('./global-teardown'),

  // Each test is given 30 seconds.
  timeout: 30000,

});
```

| Option | Description |
| --- | --- |
| [testConfig.globalSetup](https://playwright.dev/docs/api/class-testconfig#test-config-global-setup) | Path to the global setup file. This file will be required and run before all the tests. It must export a single function. |
| [testConfig.globalTeardown](https://playwright.dev/docs/api/class-testconfig#test-config-global-teardown) | Path to the global teardown file. This file will be required and run after all the tests. It must export a single function. |
| [testConfig.outputDir](https://playwright.dev/docs/api/class-testconfig#test-config-output-dir) | Folder for test artifacts such as screenshots, videos, traces, etc. |
| [testConfig.timeout](https://playwright.dev/docs/api/class-testconfig#test-config-timeout) | Playwright enforces a [timeout](https://playwright.dev/docs/test-timeouts) for each test, 30 seconds by default. Time spent by the test function, test fixtures and beforeEach hooks is included in the test timeout. |

## Expect Options

Configuration for the expect assertion library.

playwright.config.ts

```js
import { defineConfig } from '@playwright/test';

export default defineConfig({
  expect: {
    // Maximum time expect() should wait for the condition to be met.
    timeout: 5000,

    toHaveScreenshot: {
      // An acceptable amount of pixels that could be different, unset by default.
      maxDiffPixels: 10,
    },

    toMatchSnapshot: {
      // An acceptable ratio of pixels that are different to the
      // total amount of pixels, between 0 and 1.
      maxDiffPixelRatio: 0.1,
    },
  },
});
```

| Option | Description |
| --- | --- |
| [testConfig.expect](https://playwright.dev/docs/api/class-testconfig#test-config-expect) | [Web first assertions](https://playwright.dev/docs/test-assertions) like `expect(locator).toHaveText()` have a separate timeout of 5 seconds by default. This is the maximum time the `expect()` should wait for the condition to be met. Learn more about [test and expect timeouts](https://playwright.dev/docs/test-timeouts) and how to set them for a single test. |
| [expect(page).toHaveScreenshot()](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-screenshot-1) | Configuration for the `expect(locator).toHaveScreenshot()` method. |
| [expect(value).toMatchSnapshot()](https://playwright.dev/docs/api/class-snapshotassertions#snapshot-assertions-to-match-snapshot-1) | Configuration for the `expect(locator).toMatchSnapshot()` method. |



