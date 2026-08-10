# Detect and Handle Flaky Playwright Tests

This guide will help you detect flaky Playwright tests and provide six possible ways to handle them. Find out how you can quickly identify flaky tests, generate reports, alert your team and even perform analytics on them using the [Playwright CTRF reporter.](https://github.com/ctrf-io/playwright-ctrf-json-report)

We all know, flaky tests are hugely frustrating for development teams, causing delays and damaging confidence in the testing process. Their intermittent failures make them difficult to detect, leading to time-consuming investigations and affecting the reliability of CI/CD pipelines.

By detecting and handling flaky tests from day one, you enhance the reliability of your test suite, improve the efficiency of your CI/CD pipeline, and avoid lengthy investigations into false positives.

There are many ways to handle flaky tests with countless solutions available, however, the methods included below use the [Playwright CTRF reporter](https://github.com/ctrf-io/playwright-ctrf-json-report), it's easy to install and makes it easy to detect and handle flaky tests.

The [Playwright CTRF reporter](https://github.com/ctrf-io/playwright-ctrf-json-report) includes a Test object with flaky and retries properties:

```
"tests": [
        {
          "name": "User should be able to login",
          "status": "passed",
          "duration": 1200,
          "retries": 2,
          "flaky": true,
        },
]
```

This means that identifying flaky tests with CTRF is easy. Now here's how you might handle them.

## View Flaky Playwright Tests in Github Actions

Using the [github-actions-ctrf](https://github.com/ctrf-io/github-actions-ctrf) tool you can generate a Github Actions summary with a single command:

```
npx github-actions-ctrf flaky path/to/ctrf-report.json
```

The summary provides detailed information about each flaky test, so now, every time a flaky test occurs, get a report in GitHub.

## Send Alerts To Microsoft Teams When Your Playwright Tests Are Flaky

You can detect flaky Playwright tests as soon as they occur and send notifications to your Teams channel to alert your team about them. The notification contains information about which tests were flaky so you can address them immediately. This is achievable with a single command in your CI/CD pipeline.

Using the [teams-ctrf](https://github.com/ctrf-io/teams-ctrf) tool you can send Teams alerts when a flaky test occurs:

```
npx teams-ctrf flaky path/to/ctrf-report.json
```

## Send Alerts To Slack When Your Playwright Tests Are Flaky

Using the [slack-ctrf](https://github.com/ctrf-io/slack-ctrf) tool you can send slack alerts when a flaky test occurs:

```
npx slack-ctrf flaky path/to/ctrf-report.json
```

## Log Flaky Playwright Tests When They Occur

You can log flaky tests using the [ctrf-cli package](https://github.com/ctrf-io/ctrf-cli?tab=readme-ov-file#flaky):

```
npx ctrf flaky path/to/ctrf-report.json
```

The command will output the names of the flaky tests and the number of retries each test has undergone, like so:

```
Processing report: reports/sample-report.json
Found 1 flaky test(s) in reports/sample-report.json:
- Test Name: Test 1, Retries: 2
```

## Perform Analytics on Flaky Playwright Tests

The [CTRF JSON](https://ctrf.io/docs/schema/test) test object contains flaky and retries properties, which means it's easy to detect flaky tests. If you post your CTRF reports to a database, you can perform analytics on flaky tests such as determining their occurrences and identifying which tests are flaky.

## Automating Flaky Test Detection with JSON Reports and Shell Scripts

You can simply store your CTRF reports in a shared directory, and parse them with a `jq` command iterating over them in a shell script:

```bash
#!/bin/bash

# Directory containing JSON reports
REPORT_DIR="ctrf"

# Iterate over each JSON report
for REPORT in "$REPORT_DIR"/*.json; do
  # Parse the report with jq to find flaky tests
  jq '.tests[] | select(.flaky == true) | {name: .name, retries: .retries}' "$REPORT"
done
```

There are many ways to detect and handle flaky playwright tests, I hope the methods using the Playwright CTRF reporter provide you with more options when it comes to defeating those flaky tests!
