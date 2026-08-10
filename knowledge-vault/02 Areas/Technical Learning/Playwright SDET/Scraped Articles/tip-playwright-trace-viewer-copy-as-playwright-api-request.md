# TIP: Playwright Trace Viewer - Copy as Playwright API Request

With the latest 1.50 Playwright release, the Trace Viewer includes a "Copy as Playwright" button on HTTP requests in the Network tab.

## From Playwright Test Report

Set `reporter: "html"` and `trace: "on"` (or `retain-on-failure`) in playwright.config.ts. Run tests, open the HTML report, click into a test, open Traces, select a network request, and click "Copy as Playwright".

## From Playwright UI mode

Run `npx playwright test --ui` and use the same Network tab Copy as Playwright button.

## Why This is Incredible?

This shortcut lets you inspect HTTP requests made through the UI and generate Playwright API request code for test data setup without manually copying headers and bodies.

Use the copied code with `request` fixture to create test data via API, bypassing the UI for faster test setup.

