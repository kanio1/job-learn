# Detect and Handle Flaky Playwright Tests

This guide will help you detect flaky Playwright tests and provide six possible ways to handle them using the Playwright CTRF reporter.

Flaky tests are hugely frustrating for development teams, causing delays and damaging confidence in the testing process. By detecting and handling flaky tests from day one, you enhance the reliability of your test suite.

The Playwright CTRF reporter includes a Test object with flaky and retries properties:

```json
"tests": [
  {
    "name": "User should be able to login",
    "status": "passed",
    "duration": 1200,
    "retries": 2,
    "flaky": true
  }
]
```

## View Flaky Playwright Tests in Github Actions

```bash
npx github-actions-ctrf flaky path/to/ctrf-report.json
```

## Send Alerts To Microsoft Teams

```bash
npx teams-ctrf flaky path/to/ctrf-report.json
```

## Send Alerts To Slack

```bash
npx slack-ctrf flaky path/to/ctrf-report.json
```

## Log Flaky Playwright Tests

```bash
npx ctrf flaky path/to/ctrf-report.json
```

Output:
```
Processing report: reports/sample-report.json
Found 1 flaky test(s) in reports/sample-report.json:
- Test Name: Test 1, Retries: 2
```

## Perform Analytics on Flaky Playwright Tests

The CTRF JSON test object contains flaky and retries properties. If you post your CTRF reports to a database, you can perform analytics on flaky tests such as determining their occurrences and identifying which tests are flaky over time. Track trends across CI runs to prioritize fixing the most problematic tests first.

## Automating Flaky Test Detection with JSON Reports and Shell Scripts

```bash
#!/bin/bash
REPORT_DIR="ctrf"
for REPORT in "$REPORT_DIR"/*.json; do
  jq '.tests[] | select(.flaky == true) | {name: .name, retries: .retries}' "$REPORT"
done
```

You can integrate this script into your CI pipeline to automatically flag flaky tests on every run. Combine with GitHub Actions, Teams, or Slack notifications for immediate team awareness.

## Setting Up the CTRF Reporter

Add the Playwright CTRF reporter to your playwright.config.ts:

```typescript
reporter: [['ctrf-json-reporter', {}]]
```

After each test run, the reporter generates a JSON file with flaky test metadata that all the tools above can consume.

There are many ways to detect and handle flaky playwright tests. The methods using the Playwright CTRF reporter provide more options when it comes to defeating those flaky tests!

## Additional Resources

- Playwright CTRF JSON reporter: https://github.com/ctrf-io/playwright-ctrf-json-report
- GitHub Actions CTRF integration
- Teams and Slack CTRF alerting tools
- CTRF CLI for logging and analytics

Install the reporter in your Playwright config and run your test suite in CI to start detecting flaky tests automatically.
