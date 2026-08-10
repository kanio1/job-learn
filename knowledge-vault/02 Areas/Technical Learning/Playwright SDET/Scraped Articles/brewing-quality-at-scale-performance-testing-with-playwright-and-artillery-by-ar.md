[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[**Government Digital Products, Singapore**](https://medium.com/singapore-gds?source=post_page---publication_nav-e017186968a1-6164a73977dc---------------------------------------)

·

Follow publication

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:38:38/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_sidebar-e017186968a1-6164a73977dc---------------------------------------)

Be Happy, Be Awesome! We deliver high-quality digital services to citizens and businesses in Singapore 😊

Follow publication

# Brewing Quality at Scale: Performance Testing with Playwright and Artillery

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:32:32/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---byline--6164a73977dc---------------------------------------)

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---byline--6164a73977dc---------------------------------------)

Follow

7 min read

·

May 22, 2025

6

4

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D6164a73977dc&operation=register&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=---header_actions--6164a73977dc---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*3IZ9tX0xWv4kkeUV1vwK-A.png)

Gemini generated image

Imagine when a barista brews one cup of coffee, the quality is usually perfect with the right temperature, rich aroma and balanced taste but what happens when they have to brew 100 cups during the morning rush? Even with the same beans and machine, the taste might change due to several factors:

- Water temperature may not stay consistent
- Grind quality can fluctuate under pressure
- Baristas may rush steps like tamping or timing
- Machines can overheat or clog

This mirrors what happens in software applications under load:

**Coffee Shop**

1. Great taste in small batches
2. Quality drops when rushed
3. Machines may break down
4. Customers leave unhappy

**Software Application**

1. Fast response with few users
2. Latency increases under high traffic
3. Server crash or APIs timeout
4. Users abandon slow apps

In my previous article, _“_ [_Blocking Bugs and Building Quality Software with the Test Pyramid_](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341) _”_ we explored the different layers of functional testing. In this article, I’ll show you how to extend your existing end-to-end (E2E) tests written with [Playwright](https://playwright.dev/docs/intro) by integrating [Artillery](https://www.artillery.io/docs/playwright) to perform performance testing. A key benefit of this approach is that it eliminates the need to write and maintain separate scripts for functional and performance testing.

## Setting Traffic Expectation

Before starting performance testing, it’s important to determine the expected load, ideally based on actual production traffic. This can be gathered from usage analytics or estimated with input from stakeholders or product owners. The expected load often measured in users per hour can be converted to users per second to define your base load. This base load serves as the starting point for your tests, helping establish a performance baseline before ramping up to peak and stress levels.

As a general guideline, you can calculate the base load using the following formula:

_Base Load = Expected Load + 20% buffer_

Example:

- Expected Load: 1000 users per hour
- Base Load: 1000 + 20% = 1200 users per hour

To convert this to users per second:

1. 1200 users/hour ÷ 60 = 20 users/minute
2. 20 users/minute ÷ 60 = ~0.33 users/second

We can take the base load value as (20 users/minute) as a starting point for performance tests.

## Defining Test Strategies

Based on application requirements or the established base load, we can derive appropriate performance test strategies such as:

> **Breakpoint Testing**

**Formula**: Gradually increase load until the system breaks.

**Purpose**: To determine the maximum load the system can handle before failing.

**Key Focus**: Identifying the system limit.

**Analogy:** Like slowly adding passengers and luggage into a car until it can no longer move.

> **Endurance Testing**

**Formula**: Base load over 8 hours

**Purpose:** To assess system performance and stability over an extended period of continuous load.

**Key Focus:** Memory leaks, resource exhaustion, and long-term degradation.

**Analogy:** Like driving your car with 2 passengers on the highway for an entire day — you’re checking if it remains stable without overheating or slowing down over time.

> **Stress Testing**

**Formula:** 2 × Base Load for 15 minutes

**Purpose:** To determine the system’s breaking point by pushing it beyond expected limits and how it recovers back to normal usage.

**Key Focus:** Failure handling, system resilience, and recovery behaviour.

**Analogy:** Like overloading a car with 10 passengers and driving uphill. You’re testing how it performs under extreme pressure and how it recovers after it fails.

> **Load Testing**

**Formula:** 1.5 × Base Load for 30 minutes

**Purpose:** To validate system behaviour under expected or peak traffic conditions.

## Get Arthur Tee Seng Tuan’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

**Key Focus:** Response time, throughput, and error rate.

**Analogy:** Like testing how well your car drives with 4 passengers on the highway. This is ensuring it performs smoothly under normal or slightly elevated usage.

## Defining Success Criteria

One of the most important steps in performance testing is to define success criteria. These benchmarks or thresholds that determine whether the performance test is considered a pass. Establishing clear success criteria ensures the system meets business goals, technical requirements, and user experience expectations under defined load conditions.

**Success criteria _(_** _general guideline_ **_)_:**

- 95% of all response times are at or below 3 seconds.
- 99% of all response times are at or below 5 seconds.
- Error rate must be below 1%
- Average CPU and Memory utilisation rate is below 70%. _(subject to scaling policy)_

## System Architecture Awareness

Understanding your system architecture is equally important, as modern applications often depend on third-party services and cloud infrastructure such as AWS ECS, Lambda, or RDS. Each of these components behaves differently under load and has specific thresholds, such as auto-scaling rules or container resource limits.

For example, application deployed in AWS ECS can monitor CPU and memory utilisation through the ECS service health dashboard. This visibility helps determine whether your system remains operationally healthy under load, and can inform success criteria like average CPU and Memory utilisation rate.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*n_r67BT1VSvLTYBe)

AWS ECS service health dashboard

## Defining Most Critical User Flow

Before writing any code, it’s essential to design a typical user flow that reflects the most critical business process. This helps you to measure the total time taken for each key scenario and track performance across different stages of interaction. By defining these flows, you can identify performance bottlenecks and ensure the system is optimised for real-world use.

Using an asset management system as an example, a representative user flow might look like this:

1. Navigating to a Folder — Accessing a specific folder and waiting for its contents to load
2. Performing a Search — Executing a name-based search within the folder

Each step can be instrumented to capture response times, allowing you to pinpoint where performance issues may occur and improve the user experiences.

## Folder structure

We can start by creating a dedicated directory named _performance_ under the end-to-end test folder. It should consist of playwright tests _(processor.ts)_, artillery test _(artillery-config.yml)_ and test assets directory which keep the test data.

It may looks like below:

```
e2e/
├── tests/
|   ├── pages/
|   └── ...
└── performance/
    ├── .playwright-auth
    ├── test_assets/
    ├── artillery-config.yml
    └── processor.ts
```

## Playwright test codes

Below is a sample typescript code snippet that simulates and measures user interactions which can be reused as artillery tests:

- Create test functions by reusing existing [Page Object Models](https://playwright.dev/docs/pom) _(POMs)_:
- Test setup (authentication, setup test data)
- Test Execution (navigation, performing search actions)
- Test Teardown (cleaning up test data)
- Performs authentication and test data setup once, then persists the session state to avoid repeated logins or data setup during test execution

```
// processor.ts (Processor file that contain end to end test functions)

import { Page, expect as baseExpect } from '@playwright/test';
import { HomePage } from '../pages/home.page.spec';
import { LandingPage } from '../pages/landing.page.spec';
import { LoginPage } from '../pages/login.page.spec';
import path from 'path'

const folderName = "performance-test-folder";
const authFile = path.join(__dirname, '../.playwright-auth/performance-user.json');
const configuredExpect = baseExpect.configure({
  timeout: 20_000,
});

export async function setupTest(page: Page, context: any) {
  const homePage = new HomePage(page);
  await homePage.goto();

  // Authentication steps.
  const landingPage = new LandingPage(page);
  const loginPage = new LoginPage(page);
  await landingPage.goto();
  await landingPage.gotoLoginPage();
  await loginPage.login(process.env.E2E_LOGIN_EMAIL, process.env.E2E_LOGIN_PASSWORD);
  // Saves authenticated state to authFile for replay
  await page.context().storageState({ path: authFile });

 // Fill up logic to setup test data...
}

// Test execution
export async function testNameSearch(page, vuContext, events, test) {

  const { step } = test;
  const homePage = new HomePage(page);
  const searchQuery = "fileForTestNameSearch";

  // Measures time taken to navigate to a folder inside a library.
   await step('enter_folder', async () => {
     await homePage.goto();
     await homePage.openLibraries();
     await homePage.openMyLibrary();
     await homePage.enterFolder(`${folderName}`);
   });

  // Measures time for executing a name search and asserting the result.
    await step('name_search', async () => {
       await homePage.search(searchQuery);
       await configuredExpect(homePage.SearchResultTabs.getNameMatchTab()).toContainText('Name match');
       await configuredExpect(homePage.SearchResultTabs.getNameMatchTab()).toContainText('1');
      });
    }

export async function tearDownTest(page: Page) {
  const homePage = new HomePage(page);
  await homePage.goto();
  await homePage.openLibraries();
  await homePage.openMyLibrary();
  await homePage.deleteFolder(`${folderName}`);

  // Clean stored auth file
  fs.writeFileSync(authFile, JSON.stringify({}));
}
```

## Artillery test codes

Here is a YAML configuration file that sets up how Artillery will run your performance test using Typescript Playwright test functions.

```
# artillery-config.yml (Artillery Performance test configuration)

config:
# E2E URL stored in env variable
 target: "{{$env.E2E_URL}}"
 phases:
   # This phase will creates 20 virtual users in 1 minute (60 seconds)
   - duration: '1m'
     arrivalCount: 20
     name: 'warmup'
 processor: "./processor.ts"
 engines:
   playwright:
     defaultTimeout: 20
     trace:
       enabled: true
     contextOptions:
       # Reusing same storage state for all virtual users
       storageState: "performance/.playwright-auth/performance-user.json"
       # Default header might contain "HeadlessChrome" which blocked by firewall
       userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
     launchOptions:
       # Set to false to debug with visual
       headless: true
       channel: 'chrome'
 ensure:
   thresholds:
     - 'vusers.failed': 0

before:
 engine: playwright
 flowFunction: "setupTest"
scenarios:
 - engine: playwright
   name: NameSearch
   testFunction: "testNameSearch"
after:
 engine: playwright
 flowFunction: "tearDownTest"
```

## Result summary

```
# Command to run test
artillery run performance/artillery-config.yml
```

A test run might output results like below:

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:659/0*t0-S99eZXmL9qW8b)

This output offers insights into the application performance, including the number of virtual users simulated, performance metrics for each test step (entering a folder, performing a name search), and the overall session duration per user.

Key metrics:

- All 20 virtual users completed without any errors or failed assertions.
- 95% (P95) and 99% (P99) of user interactions completed within 3 seconds.

Integrating Playwright with Artillery offers a practical and efficient approach to performance testing by leveraging existing Playwright Page Object Models _(POMs)_. This reduces duplication of effort and ensures that performance tests are built on realistic user interactions rather than synthetic API calls alone.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*sRgMlOM1r4uauSeY)

DALL·E 3 generated image

Just as a coffee shop must prepare for rush hours, software development team must do performance tests to ensure:

- The system can handle peak loads.
- Response times remain acceptable under pressure.
- Bottlenecks don’t ruin the “taste” (user experiences)

Without testing at scale, even the most beautiful software application might “taste bad” when real users arrive. So next time you sip your coffee, remember: crafting great user experiences takes practice, precision and preparation just like brewing the perfect cup.

Thanks for reading and let’s continue to learn and share. 🤓

[Quality Engineering](https://medium.com/tag/quality-engineering?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Performance Testing](https://medium.com/tag/performance-testing?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[End To End Testing](https://medium.com/tag/end-to-end-testing?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Playwrights](https://medium.com/tag/playwrights?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[Artillery](https://medium.com/tag/artillery?source=post_page---footer_tags--6164a73977dc---------------------------------------)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:48:48/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:64:64/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

Follow

[**Published in Government Digital Products, Singapore**](https://medium.com/singapore-gds?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

[1.8K followers](https://medium.com/singapore-gds/followers?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

· [Last published Apr 21, 2026](https://medium.com/singapore-gds/what-stackx-cybersecurity-2026-made-me-rethink-about-ai-testing-and-my-own-engineering-work-70ecbd9d50e5?source=post_page---post_publication_info--6164a73977dc---------------------------------------)

Be Happy, Be Awesome! We deliver high-quality digital services to citizens and businesses in Singapore 😊

Follow

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:48:48/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[![Arthur Tee Seng Tuan](https://miro.medium.com/v2/resize:fill:64:64/1*8Hy7snbsD68rdwV9ZUZTAQ@2x.jpeg)](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

Follow

[**Written by Arthur Tee Seng Tuan**](https://medium.com/@justarthur?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[27 followers](https://medium.com/@justarthur/followers?source=post_page---post_author_info--6164a73977dc---------------------------------------)

· [28 following](https://medium.com/@justarthur/following?source=post_page---post_author_info--6164a73977dc---------------------------------------)

[https://sg.linkedin.com/in/seng-tuan-tee-product-quality](https://sg.linkedin.com/in/seng-tuan-tee-product-quality)

Follow

## Responses (4)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

Write a response

[What are your thoughts?](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fsingapore-gds%2Fbrewing-quality-at-scale-performance-testing-with-playwright-and-artillery-6164a73977dc&source=---post_responses--6164a73977dc---------------------respond_sidebar------------------)

Cancel

Respond

[![Anastasios Tilsizoglou](https://miro.medium.com/v2/resize:fill:32:32/1*T2ZV1qJs70bTXpx9MdHjug.jpeg)](https://medium.com/@tasostilsi?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

[Anastasios Tilsizoglou](https://medium.com/@tasostilsi?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

[Jul 12, 2025](https://medium.com/@tasostilsi/nicely-written-5796cfb1e9ce?source=post_page---post_responses--6164a73977dc----0-----------------------------------)

```
Nicely written! 👏
I’m curious about the role of assertions in the test results. Is the time it takes to execute them included in the final performance metrics? Do we have the option to exclude them, or is their impact negligible—perhaps just a few milliseconds—so it’s not worth worrying about?
```

8

1 reply

Reply

[![Uvez Shaikh](https://miro.medium.com/v2/resize:fill:32:32/1*NnmtOuNf5SawbId47xkcBA.jpeg)](https://medium.com/@Uvez_Shk?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

[Uvez Shaikh](https://medium.com/@Uvez_Shk?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

[Jul 8, 2025](https://medium.com/@Uvez_Shk/well-summarised-with-great-examples-ill-definitely-give-it-a-try-f0e6833e504b?source=post_page---post_responses--6164a73977dc----1-----------------------------------)

```
Well summarised with great examples. I’ll definitely give it a try.
```

1

Reply

[![David Lee](https://miro.medium.com/v2/resize:fill:32:32/0*EBPJZTgR2e6_Ukis)](https://medium.com/@leetatwaidavid?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

[David Lee](https://medium.com/@leetatwaidavid?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

[May 23, 2025](https://medium.com/@leetatwaidavid/nicely-written-with-succinct-examples-a68a04d5eea0?source=post_page---post_responses--6164a73977dc----2-----------------------------------)

```
Nicely written with succinct examples!
```

1

1 reply

Reply

See all responses

## More from Arthur Tee Seng Tuan and Government Digital Products, Singapore

![Test Case Design Mindset At a Glance](https://miro.medium.com/v2/resize:fit:679/format:webp/1*t8ZhzKLooazFQ3Sse3SUMQ.jpeg)

[![test-go-where](https://miro.medium.com/v2/resize:fill:20:20/1*oXO1dkRjXzLbtd3zFe-Vbw.jpeg)](https://medium.com/test-go-where?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[test-go-where](https://medium.com/test-go-where?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Aug 15, 2021

[**Test Case Design Mindset At a Glance**\\
\\
**How do you write test cases? What comes to your mind when you think of trying a new product, a newly released video game, or a new dish?**](https://medium.com/test-go-where/test-case-design-mindset-at-a-glance-e1d9a76cd5d6?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon14](https://medium.com/test-go-where/test-case-design-mindset-at-a-glance-e1d9a76cd5d6?source=post_page---author_recirc--6164a73977dc----0---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![A Hitchhiker’s Guide to Identity Providers (Singapore Government Edition)](https://miro.medium.com/v2/resize:fit:679/format:webp/1*i5_cDiAsr6rsXINXyZwZ_Q.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Samantha Wong](https://medium.com/@wong-samantha-shin-nee?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Sep 30, 2023

[**A Hitchhiker’s Guide to Identity Providers (Singapore Government Edition)**\\
\\
**This article was written with contributions from Chew Choon Keat and Alex Ng.**](https://medium.com/singapore-gds/a-hitchhikers-guide-to-identity-providers-singapore-government-edition-bebfdf354a68?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon125](https://medium.com/singapore-gds/a-hitchhikers-guide-to-identity-providers-singapore-government-edition-bebfdf354a68?source=post_page---author_recirc--6164a73977dc----1---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![Diagram of Gitlab Runner using ECS on Fargate](https://miro.medium.com/v2/resize:fit:679/format:webp/1*JljtRg4GU22NeLbK7vrcXQ.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Quy Tang](https://medium.com/@qtangs?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Nov 10, 2022

[**Deploying Serverless GitLab Runners on AWS Fargate with Terraform**\\
\\
**A complete setup of secure and scalable serverless GitLab runners on AWS Fargate via Terraform IAC and Terragrunt for multi-enviroment…**](https://medium.com/singapore-gds/deploying-serverless-gitlab-runners-on-aws-fargate-with-terraform-33b56194671b?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon1.1K\\
\\
A response icon4](https://medium.com/singapore-gds/deploying-serverless-gitlab-runners-on-aws-fargate-with-terraform-33b56194671b?source=post_page---author_recirc--6164a73977dc----2---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

![AI-generated image based on the title with manual edits](https://miro.medium.com/v2/resize:fit:679/format:webp/1*7mapn-6cJcQwMP7hJwhpqg.png)

[![Government Digital Products, Singapore](https://miro.medium.com/v2/resize:fill:20:20/1*-otRf3KIpt5zGjnZHGq_2w.png)](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

In

[Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

by

[Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

·

Oct 18, 2024

[**Blocking Bugs and Building Quality Software with the Test Pyramid**\\
\\
**Using a volleyball analogy to explain key concepts of the Test Pyramid in software development.**](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[A clap icon4](https://medium.com/singapore-gds/blocking-bugs-and-building-quality-software-with-the-test-pyramid-faf653ac6341?source=post_page---author_recirc--6164a73977dc----3---------------------8e0a4ad7_4abf_48f9_9032_0959f8d541e5--------------)

[See all from Arthur Tee Seng Tuan](https://medium.com/@justarthur?source=post_page---author_recirc--6164a73977dc---------------------------------------)

[See all from Government Digital Products, Singapore](https://medium.com/singapore-gds?source=post_page---author_recirc--6164a73977dc---------------------------------------)

## Recommended from Medium

![AI-Native Software Testing: How Modern QA Is Evolving with Playwright, AI Agents, and Intelligent…](https://miro.medium.com/v2/resize:fit:679/format:webp/1*a9azmXAAxVqEmxOdK9cITg.png)

[![Srinivas Bommena](https://miro.medium.com/v2/resize:fill:20:20/0*E61FcTagEnhqCTYg.)](https://medium.com/@srinib100?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Srinivas Bommena](https://medium.com/@srinib100?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Mar 15

[**AI-Native Software Testing: How Modern QA Is Evolving with Playwright, AI Agents, and Intelligent…**\\
\\
**For decades, software testing relied on deterministic scripts and manual verification. QA engineers wrote brittle test scripts, maintained…**](https://medium.com/@srinib100/ai-native-software-testing-how-modern-qa-is-evolving-with-playwright-ai-agents-and-intelligent-d75dbc059a56?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon140\\
\\
A response icon5\\
\\
Repost icon6](https://medium.com/@srinib100/ai-native-software-testing-how-modern-qa-is-evolving-with-playwright-ai-agents-and-intelligent-d75dbc059a56?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![I replaced my entire QA team with Claude and Agentic Workflow](https://miro.medium.com/v2/resize:fit:679/format:webp/1*yjjotfF4UGz19-TmgO-7lg.png)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:20:20/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://medium.com/gitconnected?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

In

[Level Up Coding](https://medium.com/gitconnected?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

by

[Brent Kastner](https://medium.com/@brentkastner?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Feb 23

[**I replaced my entire QA team with Claude and Agentic Workflow**\\
\\
**An Open-Source Experiment with Claude, Python, and Playwright**](https://medium.com/gitconnected/i-replaced-my-entire-qa-team-with-claude-and-agentic-workflow-aed22dfb2a65?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon231\\
\\
A response icon6](https://medium.com/gitconnected/i-replaced-my-entire-qa-team-with-claude-and-agentic-workflow-aed22dfb2a65?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![How to Test AI Agents](https://miro.medium.com/v2/resize:fit:679/format:webp/0*NqRWd39ZXIv4y8yr)

[![Mitesh Shah](https://miro.medium.com/v2/resize:fill:20:20/1*XgBEtxv169gdkqZr1XQNxg.jpeg)](https://medium.com/@mitesh_shah?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Mitesh Shah](https://medium.com/@mitesh_shah?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

May 26

[**How to Test AI Agents**\\
\\
**A practical guide to testing AI agents before production — unit tests for non-deterministic systems, LLM-as-judge evaluation, red teaming**](https://medium.com/@mitesh_shah/how-to-test-ai-agents-40c79f3ddba9?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon36\\
\\
A response icon1\\
\\
Repost icon1](https://medium.com/@mitesh_shah/how-to-test-ai-agents-40c79f3ddba9?source=post_page---read_next_recirc--6164a73977dc----0---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![🎭 Playwright + TypeScript — 100 In-Depth Interview Questions & Answers](https://miro.medium.com/v2/resize:fit:679/format:webp/1*mx8SE7IlCxCrdqqpLPk9mw.png)

[![Himanshu Agarwal](https://miro.medium.com/v2/resize:fill:20:20/1*gKxbSn2RayAiAYRIR9L2Yg.png)](https://medium.com/@himanshuai?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Himanshu Agarwal](https://medium.com/@himanshuai?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

6d ago

[**🎭 Playwright + TypeScript — 100 In-Depth Interview Questions & Answers**\\
\\
**For SDET / QA Automation Engineers with 5–15 Years of Experience (L1 & L2 Rounds)**](https://medium.com/@himanshuai/playwright-typescript-100-in-depth-interview-questions-answers-e4d9627e347e?source=post_page---read_next_recirc--6164a73977dc----1---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![We Are Automating Test Creation Faster Than We Are Automating Test Deletion](https://miro.medium.com/v2/resize:fit:679/format:webp/1*k7ZxHlF0jeGjYgX843QH9w.png)

[![Manish Saini](https://miro.medium.com/v2/resize:fill:20:20/1*HFJBBQCP86W5l4xBY6PQeg.jpeg)](https://medium.com/@manishsaini74?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Manish Saini](https://medium.com/@manishsaini74?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Jul 16

[**We Are Automating Test Creation Faster Than We Are Automating Test Deletion**\\
\\
**AI can generate hundreds of tests in minutes.**](https://medium.com/@manishsaini74/we-are-automating-test-creation-faster-than-we-are-automating-test-deletion-d04dac2fd4ff?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon1\\
\\
A response icon1](https://medium.com/@manishsaini74/we-are-automating-test-creation-faster-than-we-are-automating-test-deletion-d04dac2fd4ff?source=post_page---read_next_recirc--6164a73977dc----2---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

![Shift-Left Testing in Enterprise Teams: What’s Actually Working in 2024](https://miro.medium.com/v2/resize:fit:679/format:webp/0*B7iVF9DKaU4JlSaT)

[![Abdulkadir Akyurt](https://miro.medium.com/v2/resize:fill:20:20/1*XXju8hhTnVvaETgJB9_TyQ.png)](https://medium.com/@abdulkadirakyurt.de?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[Abdulkadir Akyurt](https://medium.com/@abdulkadirakyurt.de?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

·

Jun 1

[**Shift-Left Testing in Enterprise Teams: What’s Actually Working in 2024**\\
\\
**Three years ago, I sat in a room with a VP of Engineering at a financial services company who confidently told me their team had ‘fully…**](https://medium.com/@abdulkadirakyurt.de/shift-left-testing-in-enterprise-teams-whats-actually-working-in-2024-897db89b6119?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[A clap icon3](https://medium.com/@abdulkadirakyurt.de/shift-left-testing-in-enterprise-teams-whats-actually-working-in-2024-897db89b6119?source=post_page---read_next_recirc--6164a73977dc----3---------------------932ef915_f1ae_48a8_8cdc_9de7e83a58a7--------------)

[See more recommendations](https://medium.com/?source=post_page---read_next_recirc--6164a73977dc---------------------------------------)

[Help](https://help.medium.com/hc/en-us?source=post_page-----6164a73977dc---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----6164a73977dc---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----6164a73977dc---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----6164a73977dc---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----6164a73977dc---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----6164a73977dc---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----6164a73977dc---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----6164a73977dc---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----6164a73977dc---------------------------------------)
