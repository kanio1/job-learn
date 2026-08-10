# Playwright Visual Testing - Dynamic Data

How to handle dynamic data in Playwright visual tests.

## What is dynamic data?

Anything on your web page that could change between test runs: random data, API data, current date/time, etc.

## Options for dealing with dynamic data

### Option 1: Mock your dynamic data
Mock lists/API responses so data is consistent each run.

### Option 2: Hide the dynamic data
Hide elements before screenshot comparison.

## Hiding dynamic data

`toHaveScreenshot()` accepts `stylePath` parameter. Create screenshot.css:
```css
#datetime { display: none; }
```
Pass to toHaveScreenshot with stylePath option.

## Masking dynamic data

Use `mask: [page.locator('#datetime')]` parameter. Requires updating baseline to include mask placeholder.

## Functional validation

Still validate dynamic data functionally (e.g. assert datetime text matches current date) while visual comparison handles the rest.

## Wrap-up

Hide dynamic data in visual tests while using functional validation to ensure dynamic content is correct.

