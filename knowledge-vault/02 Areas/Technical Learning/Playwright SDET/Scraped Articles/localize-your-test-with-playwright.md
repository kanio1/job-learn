# Localize your test with Playwright

### Emulate the "geolocation", "locale" and "timezone" with Playwright

Some websites are available in different languages and countries and include differences in texts, date formats, currencies, laws, RTL languages, and colors.

Some e-commerce websites request access to your current geolocation to offer products in your current location.

## What is localization testing?

This type of testing checks that the behavior, translations, usability, accessibility, etc, are appropriate for the specific country or region.

## Tips to test your websites in different languages

1. Sometimes, the UI and elements differ in each country. Use Chrome's translate option to help locate elements in English.
2. Some websites are restricted by IP — use LambdaTest, Sauce Labs, or BrowserStack to test from another country.
3. Don't use translators to check translations — get translations from a translation team or native speakers.

## Emulate the user locale, timezone, and geolocation with Playwright

### Locale geo and timezone globally

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
    geolocation: { longitude: 12.492507, latitude: 41.889938 },
    permissions: ['geolocation'],
  },
});
```

### Locale and Timezone in test spec

```typescript
test.describe('Locale translations', () => {
    test.use({
        locale: 'de-DE',
        timezoneId: 'Europe/Berlin',
    });
    test('Translations with locale and time zone id', async ({ page }) => {
        await page.goto('https://www.google.com');
   });
});
```

### Geolocation sample

```typescript
test.describe('Geo Location Test', () => {
    test.use({
        geolocation: { longitude: 11.57549, latitude: 48.13743 },
        permissions: ['geolocation'],
    });
    test('The bing maps is located to munich', async ({ page }) => {
        const geoName = 'GermanyBavariaMunich (District)';
        await bingMapsPage.goTo();
        await bingMapsPage.locateMe.click();
        await expect(bingMapsPage.geoName.locator).toHaveText(geoName);
    });
});
```

### Tips to test and reuse the same test for different countries

Use a JSON file with text translations and specific selectors for each country. With an environment variable, you can set up the locale to emulate different locales in your laptop or with pipelines.

```typescript
const locale = process.env.LOCALE ? process.env.LOCALE : 'en-US';
const localeInfo = require(`../../data/${locale}.json`);

test.describe('Locale translations', () => {
    test.use({ locale: locale, timezoneId: localeInfo.timezoneId });
    test('Translations with locale and time zone id', async ({ page }) => {
        await homePage.goTo();
        await expect(homePage.googleSearch.locator).toHaveText(localeInfo.googleSearch);
    });
});
```

Thank you for reading. Enjoy testing!!

### LambdaTest geolocation

You can connect to LambdaTest with geoLocation capability in LT:Options to run tests from different countries.

### Synthetic testing

Synthetic testing with Checkly or PerfAgents allows periodic test execution in different AWS zones for localization monitoring.

