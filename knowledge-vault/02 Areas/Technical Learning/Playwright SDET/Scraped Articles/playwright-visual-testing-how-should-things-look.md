# Playwright Visual Testing; How Should Things Look?

Using Playwright snapshots with mocked data can significantly improve the speed at which UI regression is carried out across Chromium, Firefox, and Webkit.

## The problem

We needed to test that chart data was visualised correctly with default mocked data and that UI interactions transform charts as expected. We solve non-static data by mocking via Playwright Mock API.

## Playwright snapshotting explained

Playwright captures screenshots and compares each pixel RGBA values to golden reference images. First run creates comparison images; `--update-snapshots` saves golden standards.

```
expect(chartShot).toMatchSnapshot('rio-so2-graph-without-centro.png')
```

Parameters: `maxDiffPixels` and `maxDiffPixelRatio` for tolerance.

## Usage examples

Mock data + snapshots test chart transformations when sensors are removed. ECharts library renders graphs; snapshot testing catches padding/margin regressions across browsers.

## Maintenance

Team coordination required: update snapshots with `--update-snapshots` when UI changes. Use meaningful snapshot names. Use `waitFor({ state: 'visible' })` and network polling before screenshots.

## Efficient mocking

Use `Partial<forecastAPIResponse>` overrides instead of huge static JSON fixtures.

## Conclusion

Playwright snapshot testing is easy to set up with API mocking. Use judiciously in CI; coordinate snapshot maintenance with development.
