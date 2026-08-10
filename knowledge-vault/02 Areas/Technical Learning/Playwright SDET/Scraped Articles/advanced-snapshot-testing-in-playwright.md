Playwright's snapshot assertions are an incredibly powerful tool for ensuring your app's UI remains consistent across code changes, browsers, and devices. But they're not always easy to use.

This article dives deep on snapshot testing in Playwright, covering a wide range of features and techniques. By the end, you'll be a snapshot testing master, ensuring your app's flawless visual consistency across browsers and devices.

## Page vs. Element Snapshots

Playwright's visual testing API allows you to take snapshots of the entire page or just a specific element.

### When should I use page snapshots?

Page snapshots are excellent for verifying the entire page works as expected. Use page snapshots to test layout, responsiveness, and accessibility.

But be warned: Page snapshots can be flaky. After all, if anything within the viewport changes, the entire snapshot will fail.

```typescript
test('page snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot();
});
```

### When should I use element snapshots?

Element snapshots focus exclusively on a single page element. This makes them an excellent choice for testing components in isolation.

```typescript
test('element snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $button = page.locator('button').first();
  await expect($button).toHaveScreenshot();
});
```

## Working with Page Snapshots

### Cropping Page Snapshots

Sometimes the entire viewport isn't necessary to prove your test passes.

```typescript
test('cropped snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const {width, height} = page.viewportSize();
  await expect(page).toHaveScreenshot({
    clip: {
      x: (width - 400) / 2,
      y: (height - 400) / 2,
      width: 400,
      height: 400,
    },
  });
});
```

### Snapshot the Entire Page

```typescript
test('full page snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot({
    fullPage: true,
  });
});
```

### Scroll Before Taking a Page Snapshot

```typescript
test('scroll before snapshot', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await page.evaluate(() => {
    document.querySelector('#your-element')?.scrollIntoView({behavior: 'instant'});
  });
  await expect(page).toHaveScreenshot();
});
```

## Working with Element Snapshots

### Test Element Interactivity

```typescript
test('element states', async ({page}) => {
  await page.goto('https://www.browsercat.com/contact');
  const $textarea = page.locator('textarea').first();
  await expect($textarea).toHaveScreenshot();
  await $textarea.hover();
  await expect($textarea).toHaveScreenshot();
  await $textarea.focus();
  await expect($textarea).toHaveScreenshot();
  await $textarea.fill('Hey, cool cat!');
  await expect($textarea).toHaveScreenshot();
});
```

### Test Element Responsiveness

```typescript
test('element responsiveness', async ({page}) => {
  const viewportWidths = [960, 760, 480];
  await page.goto('https://www.browsercat.com/blog');
  const $post = page.locator('main article').first();
  for (const width of viewportWidths) {
    await page.setViewportSize({width, height: 800});
    await expect($post).toHaveScreenshot(`post-${width}.png`);
  }
});
```

## Advanced Snapshot Techniques

### Masking Portions of a Snapshot

```typescript
test('masked snapshots', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $hero = page.locator('main > header');
  const $footer = page.locator('body > footer');
  await expect(page).toHaveScreenshot({
    mask: [
      $hero.locator('img[src$=".svg"]'),
      $hero.locator('a[target="_blank"]'),
    ],
  });
});
```

### Keeping Styles Constant During Snapshots

```typescript
test('consistent styles', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  const $hero = page.locator('main > header');
  await expect(page).toHaveScreenshot({
    stylePath: [
      './hide-dynamic-elements.css',
      './disable-scroll-animations.css',
    ],
  });
});
```

### Auto-Retry Flaky Snapshots

```typescript
test('retry snapshots', async ({page}) => {
  await page.goto('https://www.browsercat.com');
  await expect(page).toHaveScreenshot({
    timeout: 1000 * 60,
  });
});
```

### Visual Tests for Generated Images

```typescript
import {test, expect} from '@playwright/test';
import {buffer} from 'stream/consumers';

test('arbitrary snapshot', async ({page}) => {
  await page.goto('https://getavataaars.com');
  await page.locator('main form button').first().click();
  const avatar = await page.waitForEvent('download')
    .then((dl) => dl.createReadStream())
    .then((stream) => buffer(stream));
  expect(avatar).toMatchSnapshot('avatar.png');
});
```

### Compare Snapshots Across Browsers

Use Playwright projects with shared snapshotPathTemplate to compare rendering across Chromium, Firefox, and Safari.

```typescript
const crossBrowserConfig = {
  testDir: './tests/cross-browser',
  snapshotPathTemplate: '.test/cross/{testFilePath}/{arg}{ext}',
  expect: {
    toHaveScreenshot: {maxDiffPixelRatio: 0.1},
  },
};
```

Initialize snapshots with `npx playwright test --project cross-browser -u` then run tests.

Different browsers render fonts, colors, and images differently. Tune `maxDiffPixelRatio` and `threshold` as needed.

## Next Steps

For advice on fine-tuning snapshot tests and running visual tests in CI/CD, check out the Ultimate Guide to Visual Testing with Playwright.

Happy testing!

Cross-browser project configuration example with dependencies chain cross-chromium → cross-firefox → cross-browser ensures Safari compares against same snapshot directory. Remove dynamic widgets before full-page screenshot: `await page.locator(':has(> a figure)').evaluate(($el) => $el.remove())`. Tune forgiving thresholds via maxDiffPixelRatio 0.1 when font rendering differs across engines. Series links: configuring snapshot tests, running snapshot tests in CI/CD, getting started with snapshot tests. BrowserCat ultimate guide covers making visual tests more forgiving when cross-browser pixel diffs are expected rather than regressions.

