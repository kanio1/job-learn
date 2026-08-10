[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

# Strategic Tagging: Optimizing Your Playwright Test Suit

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:64:64/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---byline--4ab109343fed---------------------------------------)

[Meris Stupar](https://medium.com/@merisstupar11?source=post_page---byline--4ab109343fed---------------------------------------)

5 min read

·

Mar 18, 2024

--

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D4ab109343fed&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40merisstupar11%2Fstrategic-tagging-optimizing-your-playwright-test-suit-4ab109343fed&source=---header_actions--4ab109343fed---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

Do you find yourself running your complete automated test suite on every occasion? Utilizing tags can accelerate the process by selectively choosing which tests to execute precisely when you require them.

As software projects evolve and automation project expands, it's common for the number of tests to increase alongside the introduction of new features. It becomes very useful to run only a subset of certain tests. Although Playwright allows tests to run in parallel, there comes a point where splitting tests into smaller groups proves useful. While maintaining a robust automated test suite is essential for product quality, careless test automation can decrease your team's progress considerably. One effective strategy is to segment your automated tests by tagging them and executing only a subset of the complete test suite at various stages of the software development process.

Effectively organizing tests with tags offers a significant advantage of precisely targeting the required test cases.

**Consider the following examples:**

◾ Run the entire test suite outside of business hours without disrupting the team and selectively run a subset of tests on a pull request to maintain the speed and efficiency of your **CI pipelines**.

◾ Allow specific teams (eg QA or the features team) to run only the tests they are responsible for.

◾ Run smoke tests during a production release that only involve read operations.

Tags are used to filter tests in the HTML Report, UI Mode or VSCode extension.

Using a tag system allows you to categorize tests into logical sets. Tags are defined using the **@tag** syntax within the test description. Although any string can technically serve as a tag, the documentation prefers the **@tag** syntax, so it is recommended to follow that rule.

**How to Install Playwright?**

```
npm init playwright@latest
```

Please visit official [Installation \| Playwright](https://playwright.dev/docs/intro) documentation for more details.

**How to run Playwright test?**

```
npx playwright test
```

**Old Playwright Syntax:**

In the past, tags were incorporated into the test title, which remains a supported method. However, this approach leads to duplication in the HTML report. Playwright automatically extracts tags from the title and displays them as labels for improved visibility, eliminating the need for redundant tagging within the title.

```
test('Playwright Landing page - Has title @Smoke', async ({ page }) => {

  await page.goto('https://playwright.dev/');

  await expect(page).toHaveTitle(/Playwright/);

});
```

**How to run tests by tags?**

```
npx playwright test --grep @Smoke
```

Press enter or click to view image in full size

Example of Old Playwright Tagging Syntax

**New Playwright Syntax:**

The reason for introducing the new syntax for placing tags, as stated in the official documentation, stems from the visibility of the previous syntax in the HTML report, where tags were displayed inside the test title as tags. As we have shown in the previous part of Old Playwright Syntax. This way of tagging could lead to confusion and significant duplication, especially when dealing with numerous tags.

To adopt the new syntax, simply generate a tag object containing either an array of tags or a single tag:

```
test('Playwright Landing page - Has title', { tag: ['@Smoke', '@UI' ] } ,async ({ page }) => {

  await page.goto('https://playwright.dev/');

  await expect(page).toHaveTitle(/Playwright/);

});
```

Press enter or click to view image in full size

Example of New Playwright Tagging Syntax

As evident from the new syntax, tags are no longer displayed within the test name itself, resulting in significantly improved readability.

Tags are also applicable within a describe block:

```
test.describe('Group Example', { tag: '@Group' }, () => {

  test('Playwright Landing page - Has title', { tag: ['@Smoke', '@UI' ] } ,async ({ page }) => {

    await page.goto('https://playwright.dev/');
    await expect(page).toHaveTitle(/Playwright/);

  });

  test('Playwright Github', { tag: ['@Smoke', '@UI', '@Fast' ] } ,async ({ page }) => {

    await page.goto('https://github.com/microsoft/playwright');
  });

});
```

Press enter or click to view image in full size

Example of Describe Block Tagging Syntax

Update Playwright to latest version with following command:

```
npm install -D @playwright/test@latest
# Also download new browser binaries and their dependencies:
npx playwright install --with-deps
```

To verify what version you have installed on your machine use this:

```
npx playwright --version
```

**Advantages of using @tag in test management:**

**Simplified test management:** **@tag** simplify test management by categorizing test cases. This categorization allows for quick filtering and identification of relevant test cases based on tags. You can easily select test cases to execute according to specific tags.

**Tag Statistics Heat map:** The Tag Statistics Heat map, available on the analytics dashboard, provides valuable insights. It allows you to track metrics related to tags, such as the total number of tags and the amount of test cases tagged with a particular tag. However, it is essential to track the progress of test automation coverage by tags.

**Custom Test Scenarios:** You have the flexibility to define useful tags for any custom test scenario. This allows scenarios from different features, test suites or feature files (BDD) to be executed together. For example, you can execute all tests marked as **@Smoke** excluding those marked as **@Regression**

**Examples you can use to classify your tests:**

S **moke testing** is a software testing technique that is performed after the software is built to verify that the critical functions of the software are working well. It is performed before any detailed functional or regression tests are performed. The main purpose of smoke testing is to reject a software application with bugs so that the QA engineering team does not waste time testing a broken software application.

S **anity testing** is a type of software testing that is performed after receiving an intermediate version of software, usually with minor changes to code or functionality. Its purpose is to ensure that bugs have been fixed and that no new problems have arisen as a result of these changes. The goal is to verify that the intended functionality works roughly as intended. If the correctness test gives wrong results, the build is rejected to avoid spending time and resources on more extensive testing.

R **egression testing** is a type of software testing conducted after a code update to ensure that the update introduced no new bugs. This is because new code may bring in new logic that conflicts with the existing code, leading to defects. Usually, QA teams have a series of regression test cases for important features that they will re-execute each time these code changes occur to save time and maximize test efficiency.

Therefore, it is very important that you organize your test tagging strategy well. You are in a much better position when you want to run only a specific set of tests and not the entire suite. Under the name of the Smoke tag, you can run only Smoke tests or, otherwise, Regression tests individually.

## Until Next Time: ✌️💻

[Playwrights](https://medium.com/tag/playwrights?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Testing](https://medium.com/tag/testing?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Automation](https://medium.com/tag/automation?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[Programming](https://medium.com/tag/programming?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[QA](https://medium.com/tag/qa?source=post_page---footer_tags--4ab109343fed---------------------------------------)

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:96:96/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[![Meris Stupar](https://miro.medium.com/v2/resize:fill:128:128/1*LiWdjbt03E82EDafyEB8BQ.jpeg)](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[**Written by Meris Stupar**](https://medium.com/@merisstupar11?source=post_page---post_author_info--4ab109343fed---------------------------------------)

[76 followers](https://medium.com/@merisstupar11/followers?source=post_page---post_author_info--4ab109343fed---------------------------------------)

· [2 following](https://medium.com/@merisstupar11/following?source=post_page---post_author_info--4ab109343fed---------------------------------------)

Software Engineer - Automation Quality Assurance Engineer

[Help](https://help.medium.com/hc/en-us?source=post_page-----4ab109343fed---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----4ab109343fed---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----4ab109343fed---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----4ab109343fed---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----4ab109343fed---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----4ab109343fed---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----4ab109343fed---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----4ab109343fed---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----4ab109343fed---------------------------------------)
