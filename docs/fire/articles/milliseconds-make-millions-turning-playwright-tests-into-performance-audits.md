# Milliseconds Make Millions: Turning Playwright Tests into Performance Audits

| Field | Value |
|-------|-------|
| **Author** | João Coelho |
| **Published** | 2025-07-14T19:39:01.271Z |
| **URL** | https://medium.com/@joaovitorcoelho10/milliseconds-make-millions-turning-playwright-tests-into-performance-audits-68f7fd6bc74d |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "Milliseconds Make Millions: Turning Playwright Tests into Performance Audits",
  "og:title": "Milliseconds Make Millions: Turning Playwright Tests into Performance Audits",
  "author": "Jo\u00e3o Coelho",
  "publishedTime": "2025-07-14T19:39:01.271Z",
  "og:url": "https://medium.com/@joaovitorcoelho10/milliseconds-make-millions-turning-playwright-tests-into-performance-audits-68f7fd6bc74d",
  "description": "Milliseconds Make Millions \u2014 this powerful phrase underscores a simple truth: every millisecond of delay in your web application\u2026",
  "statusCode": 200,
  "sourceURL": "https://medium.com/@joaovitorcoelho10/milliseconds-make-millions-turning-playwright-tests-into-performance-audits-68f7fd6bc74d"
}
```

---

# Milliseconds Make Millions: Turning Playwright Tests into Performance Audits

[![João Coelho](https://miro.medium.com/v2/resize:fill:64:64/1*E4-yuy_jTTIyQsqRrUyuBA.png)](https://medium.com/@joaovitorcoelho10?source=post_page---byline--68f7fd6bc74d---------------------------------------)

[João Coelho](https://medium.com/@joaovitorcoelho10?source=post_page---byline--68f7fd6bc74d---------------------------------------)

Jul 14, 2025

"Milliseconds Make Millions" — this powerful phrase underscores a simple truth: **every millisecond of delay in your web application directly impacts your business revenue, user satisfaction, and brand reputation**.

[A study conducted by Deloitte](https://www.deloitte.com/ie/en/services/consulting/research/milliseconds-make-millions.html), shows that even **a 100ms improvement in page load can boost conversion rates by up to 10.1%**, proving that performance is not just a technical concern, but a strategic business priority.

As QA engineers and automation experts, we have the unique opportunity — and responsibility — to embed performance validation into our test suites.

This article walks you through how to transform your Playwright end-to-end tests into comprehensive performance audits using **Lighthouse**, Google's open-source tool for web quality measurement.

## What is Lighthouse and Why Does It Matter?

Lighthouse is an automated tool developed by Google to audit and report on the quality of web pages. It is the industry benchmark for evaluating user-centric performance, accessibility, SEO, and adherence to best practices.

Lighthouse scores pages across five core categories:

- **Performance —** measures load speed, interactivity, and stability (Core Web Vitals like LCP, FID, CLS);
- **Accessibility —** assesses how accessible your site is for users with disabilities (e.g., screen readers);
- **Best Practices —** checks for security and coding best practices (HTTPS, safe JS usage, efficient images);
- **SEO —** evaluates how easily your site can be discovered and indexed by search engines;
- **Progressive Web App (PWA) —** tests criteria for delivering app-like experiences on the web.

Each category is scored from 0 to 100, with thresholds reflecting real user experience standards.

Incorporating Lighthouse audits into your Playwright tests means moving from reactive monitoring to **proactive quality enforcement** — ensuring that your web app is not only functional but performant and accessible with every deployment.

## Why Combine Playwright + Lighthouse?

End-to-end testing frameworks like Playwright are phenomenal for verifying workflows and functionality, but they fall short in measuring **how well** the app performs.

Traditional performance testing tools are often siloed, run separately from functional tests, and rarely block merges based on performance regressions.

By [integrating Lighthouse audits directly into Playwright tests](https://www.npmjs.com/package/playwright-lighthouse), you achieve:

- **Early detection** of performance regressions alongside functional failures;
- **Performance budgets as pass/fail thresholds** in CI/CD pipelines;
- **Unified reports** combining UX, accessibility, SEO, and best practice insights;
- **Actionable feedback** that drives cross-team collaboration between QA, developers, and product owners.

This synergy transforms your test suite from a checklist into a **quality gatekeeper** that defends the user experience at every commit.

## Setup: Getting Started

To get this integration running, install the essential packages:

```
yarn add -D playwright-lighthouse playwright lighthouse
```

Playwright must launch Chrome with the remote debugging port open for Lighthouse to connect:

```
import { chromium } from 'playwright';

const browser = await chromium.launch({
  args: ['--remote-debugging-port=9222'],
});
```

> _Note: Use_ **_headful Chrome_** _(not headless), as Lighthouse requires a real browser environment._

## Writing the Audit Test

Here's a practical example showing a Lighthouse audit within a Playwright test:

```
import { test } from '@playwright/test';
import { playAudit } from 'playwright-lighthouse';

test('Lighthouse audit on homepage', async ({ page }) => {
  await page.goto('https://your-app.com');
  await playAudit({
    page,
    port: 9222,
    thresholds: {
      performance: 75,
      accessibility: 80,
      'best-practices': 70,
      seo: 85,
    },
    reports: {
      formats: {
        json: true,
        html: true,
      },
      name: 'lighthouse-homepage',
      directory: 'reports-lighthouse',
    },
  });
});
```

## Interpreting Results & Impact

This setup outputs detailed `.json` and `.html` reports, which can be:

- Uploaded as artifacts for inspection;
- Parsed in pipeline steps to enforce thresholds;
- Shared with stakeholders for transparency.

[Source](https://github.com/abhinaba-ghosh/playwright-lighthouse/blob/HEAD/docs/lighthouse_report.png)

Your test will **fail if scores fall below thresholds**, turning subjective "slow" into objective, measurable failures.

This elevates performance to a **core quality gate** in your delivery process.

## When & Where to Use This Integration

Ideal use cases include:

- **Smoke tests in production environments** to verify baseline performance;
- **Regional testing** for apps deployed globally with different network characteristics;
- **Pull request validations** to catch regressions before merging.

This approach brings performance closer to the developer, turning it into a shared responsibility, not just a monitoring afterthought.

## Challenges & Considerations

- Lighthouse audits run slower than pure functional tests (expect 10–15s overhead);
- Port conflicts on `9222` can cause silent failures;
- May not fully replace dedicated performance testing tools in large scale load scenarios.

Despite these, the **ROI in early detection and continuous quality is unmatched**.

## Conclusion

Quality is no longer just "does it work?" — it's "does it delight users by being fast, accessible, and reliable?"

Integrating Lighthouse with Playwright empowers QA teams to own performance as part of their test suite — a strategic move towards holistic quality that impacts business metrics.

With minimal setup and powerful insights, this integration is a **game-changer** for teams serious about delivering excellence every release.

_I hope you enjoyed reading this article!_

_My name is João Coelho, and I am currently a QA Automation Engineer at Talkdesk. Lately, I have been writing articles regarding automation, QA and software engineering topics, that might not be known by the community._

_If you want to follow my work, check my_ [_Linkedin_](https://www.linkedin.com/in/jo%C3%A3ocoelho10/) _and my author profile at_ [_Medium_](https://medium.com/@joaovitorcoelho10) _!_

_Furthermore, if you're interested in further supporting me and my content creation efforts, you can do so by buying me a coffee!_ 😄👇

_Your support goes a long way in helping me dedicate more time to researching and sharing valuable insights about automation, QA, and software engineering._

[Performance](https://medium.com/tag/performance?source=post_page---footer_tags--68f7fd6bc74d---------------------------------------)

[Lighthouse](https://medium.com/tag/lighthouse?source=post_page---footer_tags--68f7fd6bc74d---------------------------------------)

[JavaScript](https://medium.com/tag/javascript?source=post_page---footer_tags--68f7fd6bc74d---------------------------------------)
