[Sitemap](https://pradappandiyan.medium.com/sitemap/sitemap.xml)

# Pixel-by-Pixel Visual Testing Using Playwright with GitHub Actions

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

Create tests/visual.spec.js with screenshot capture logic comparing actual vs baseline.

## Step 3: Compare Screenshots with Pixelmatch

Create compare-images.mjs using pixelmatch with threshold 0.15.

## Step 4: Automate with GitHub Actions

Create .github/workflows/visual-test.yml to run tests, compare screenshots, and upload diff artifacts.

## Conclusion

Pixelmatch + Playwright provides a powerful visual testing workflow with pixel-level precision. By integrating into CI with GitHub Actions, you can catch UI changes before users do.

I have created a project on GitHub: https://github.com/pradapjackie/playwright-visual-testing

