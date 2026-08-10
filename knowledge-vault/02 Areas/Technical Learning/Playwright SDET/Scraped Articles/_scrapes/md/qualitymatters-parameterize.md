## Introduction

Today, I encountered an interesting challenge while working on a Playwright project at work, and I'd like to document my findings.

**The task was to simultaneously run multiple projects, each with different sets of data.**

Playwright offers the ability to declare **_options_** to address this requirement.

Explore the demo-project on GitHub: https://github.com/nora-weisser/playwright_demo

## Step 1: Install the project

```
npm init playwright@latest
```

## Step 2: Configure Playwright Project

In `playwright.config.ts`, set baseURL in the `use` section:

```
use: {
    baseURL: 'https://www.saucedemo.com/',
    trace: 'on-first-retry',
 },
```

## Step 3: Playwright Projects

A Project is a logical group of tests running with the same configuration — different browsers, devices, or data sets.

## Step 4: Test scenario

Implement a login test executed with various username/password pairs via project parameterization.

## Step 5-6: Test Data

Create `test_data/login.data.ts` with `USER_DATA` interface and `USERS` map for standard_user, locked_out_user, problem_user, etc.

## Step 7: Extend TestOptions

Create `helpers/test-options.ts`:

```
import { test as base } from '@playwright/test'
import { USER_DATA, USERS } from '../test_data/login.data'

export interface TestOptions {
    targetUser: USER_DATA
}
export const test = base.extend<TestOptions>({
    targetUser: [USERS['standard_user'], { option: true }],
})
```

## Step 8: Use in test

Import `test` from `../helpers/test-options` and use `targetUser` fixture in login test against saucedemo.com.

## Step 9: Update projects section

Define one project per user with `targetUser: USERS['standard_user']` etc. in each project's `use` block.

## Step 10: Run all projects

```
npx playwright test
```

Report lists each project/user combination. locked_out_user demonstrates expected failure.

## Step 11: Run specific project

```
npx playwright test --project=standard_user
```

## Conclusion

Parameterization improves maintainability, coverage, and failure diagnosis. Alternative: fixtures with one project keep config simpler; projects help when different users run different test subsets or when reports should group by user/project.

Resources:
- https://playwright.dev/docs/test-parameterize
- https://playwright.dev/docs/test-fixtures
- https://github.com/nora-weisser/playwright_demo

Comments from readers note fixtures as an alternative for same test across users, while project-level parameterization helps when each user runs a different test subset or when Allure/built-in reports should segment by target user.

Hashnode republication by Eleonora Belova qualitymatters blog includes Step 1 npm init playwright@latest project structure playwright.config.ts package.json tests/ example.spec.ts.

Step 2 baseURL https://www.saucedemo.com/ with trace on-first-retry in use block of playwright.config.ts.

Step 3 Playwright Projects definition: logical group running same configuration across browsers devices or logged-in vs logged-out states.

Step 4 login test case parameterized across username password pairs via targetUser option per project.

Step 5-6 test_data/login.data.ts USER_DATA interface and USERS map with standard_user locked_out_user problem_user performance_glitch_user error_user visual_user all using secret_sauce password.

Step 7 helpers/test-options.ts extends TestOptions with targetUser fixture defaulting to standard_user with option true flag.

Step 8 test imports custom test from helpers uses targetUser in fill locators data-test username password login-button assert inventory.html Swag Labs header.

Step 9 projects array maps each user to Desktop Chrome device plus targetUser from USERS map.

Step 10 npx playwright test runs all six projects shows locked_out_user intentional failure in HTML report with user column per project.

Step 11 npx playwright test --project=standard_user runs single dataset. Resources: playwright.dev test-parameterize test-fixtures github nora-weisser playwright_demo.

Rosemarie Vickers Good post comment. Pritesh Usadadiya curated Software Testing Notes newsletter issue 121. Eugene Gronski prefers fixtures for different users keeping one project; author replies project scope useful when different test subsets per user or report grouping by project name matters.
