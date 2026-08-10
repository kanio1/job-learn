# Pixel-by-Pixel Visual Testing Using Playwright with GitHub Actions

**Author:** Pradap Pandiyan  
**Published:** 2025-05-28  
**URL:** https://pradappandiyan.medium.com/pixel-by-pixel-visual-testing-using-playwright-with-github-actions-f47700f8a50e

## Firecrawl Metadata

```json
{
  "author": "Pradap Pandiyan",
  "article:published_time": "2025-05-28T17:34:45.350Z",
  "publishedTime": "2025-05-28T17:34:45.350Z",
  "og:title": "Pixel-by-Pixel Visual Testing Using Playwright with GitHub Actions",
  "og:description": "UI bugs are sneaky — a one-pixel shift, a color mismatch, or a disappearing icon. Visual testing helps catch what functional tests miss…",
  "sourceURL": "https://pradappandiyan.medium.com/pixel-by-pixel-visual-testing-using-playwright-with-github-actions-f47700f8a50e",
  "statusCode": 200,
  "scrapeId": "019fb3cb-eaca-73e2-9fe8-ca2a5d4d16b2"
}
```

---

UI bugs are sneaky — a one-pixel shift, a color mismatch, or a disappearing icon. Visual testing helps catch what functional tests miss. In this tutorial, you'll learn how to use **Playwright + Pixelmatch** to compare two screenshots pixel-by-pixel and detect UI regressions — complete with working code and GitHub Actions integration.

## Prerequisites

- Node.js ≥ 16
- GitHub repository
- Basic knowledge of Playwright

## Step 1: Set up the Project

Create a new project folder:

```
mkdir playwright-pixelmatch && cd playwright-pixelmatch
npm init -y
npm install playwright pixelmatch pngjs fs-extra --save-dev
npx playwright install
```

Update your package.json:

```
{
  "name": "playwright-pixelmatch",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "test": "npx playwright test",
    "compare": "node compare-images.mjs"
  }
}
```

## Folder Structure

```
playwright-pixelmatch/
├── screenshots/
│   ├── baseline/
│   ├── actual/
│   └── diff/
├── tests/
│   └── visual.spec.js
├── compare-images.mjs
├── package.json
```

## Step 2: Capture Screenshots in Playwright

Create tests/visual.spec.js:

```
import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import fsExtra from 'fs-extra';
test('visual test for homepage', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto('https://example.com');
  const actualPath = 'screenshots/actual/example.png';
  const baselinePath = 'screenshots/baseline/example.png';
  await fsExtra.ensureDir('screenshots/actual');
  await page.screenshot({ path: actualPath, fullPage: true });
  if (!fs.existsSync(baselinePath)) {
    console.warn(`⚠️ No baseline found. Creating at ${baselinePath}`);
    await fsExtra.ensureDir('screenshots/baseline');
    await fsExtra.copy(actualPath, baselinePath);
    return;
  }
  expect(fs.existsSync(baselinePath)).toBe(true);
});
```

Run your first test to generate the baseline:

```
npx playwright test
```

Once the test is executed, move the image from `screenshots/actual` folder to `screenshots/baseline` folder

## Step 3: Compare Screenshots with Pixelmatch

Create compare-images.mjs:

```
import * as fs from 'fs';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import fsExtra from 'fs-extra';
const baselinePath = 'screenshots/baseline/example.png';
const actualPath = 'screenshots/actual/example.png';
const diffPath = 'screenshots/diff/example-diff.png';
const img1 = PNG.sync.read(fs.readFileSync(baselinePath));
const img2 = PNG.sync.read(fs.readFileSync(actualPath));
const { width, height } = img1;
const diff = new PNG({ width, height });
await fsExtra.ensureDir('screenshots/diff');
const mismatchedPixels = pixelmatch(
  img1.data,
  img2.data,
  diff.data,
  width,
  height,
  {
    threshold: 0.15,
    includeAA: true,
    alpha: 0.8
  }
);
fs.writeFileSync(diffPath, PNG.sync.write(diff));
const totalPixels = width * height;
const mismatchPercentage = (mismatchedPixels / totalPixels) * 100;
console.log(`🧪 Mismatched Pixels: ${mismatchedPixels}`);
console.log(`📊 Mismatch Percentage: ${mismatchPercentage.toFixed(2)}%`);
if (mismatchPercentage > 0.5) {
  console.error(`❌ Visual test failed: ${mismatchPercentage.toFixed(2)}% > 0.5%`);
  process.exit(1);
} else {
  console.log(`✅ Visual test passed.`);
}
```

Then run:

```
npm run compare
```

## 🚀 Step 4: Automate with GitHub Actions

Create .github/workflows/visual-test.yml:

```
name: Visual Tests
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
jobs:
  visual-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: 18
      - name: Install dependencies
        run: npm install
      - name: Install Playwright browsers
        run: npx playwright install --with-deps
      - name: Run Playwright tests
        run: npm run test
      - name: Compare screenshots
        run: npm run compare
      - name: Upload diff artifact
        uses: actions/upload-artifact@v3
        with:
          name: diff-screenshot
          path: screenshots/diff/
```

This will:

- Run visual tests
- Compare images
- Upload any diffs for you to inspect visually

## Conclusion

Pixelmatch + Playwright provides a powerful visual testing workflow with pixel-level precision. By integrating into CI with GitHub Actions, you can catch UI changes before users do.

I have created a project on GitHub and added the code [here](https://github.com/pradapjackie/playwright-visual-testing). Also, I have added the pipeline link to view the results [here](https://github.com/pradapjackie/playwright-visual-testing/actions/runs/15283082300/job/42986607924).

Feel free to hit clap if you like the content. Happy Automation Testing :) Cheers. 👏
