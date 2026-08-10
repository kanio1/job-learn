# API Contract Testing on Frontend with Playwright

Sometimes, as a test engineer, business requirements for testing may be quite weird, and you have to adopt different types of testing in one suite.

Contract testing is a type of software testing that focuses on verifying the interaction between separate components/services. When two microservices interact via the API, one service sends requests in a predefined format, and another responds in a predefined format. This format is called a «contract».

In the case of client-server architecture, the frontend can act as a consumer or provider for various APIs.

**Contract testing can be a part of end-to-end testing** — it can be just a tool for specific checks. This can happen when business requirements require checking that your frontend makes specific requests to third-party APIs in a particular format.

You can isolate frontend from third-party API with Playwright's network capabilities:

1. Roughly abort request — the request will not be sent to an external API;
2. Or mock it.

For POST requests, you intercept the request by `waitForRequest()` for testing request body against your contract.

If your request body is in JSON format, you can use `postDataJSON()` for comparison with Ajv, Zod, or `toEqual()`.

## Code Example

```typescript
import { expect, type Page, test } from '@playwright/test';
import { z } from 'zod';

const schema = z.object({
  jsonrpc: z.string(),
  id: z.number(),
  method: z.string(),
  params: z.array(z.union([z.string(), z.boolean()])),
});

let page: Page;

test.beforeAll(async ({ browser }) => {
  const context = await browser.newContext();
  page = await context.newPage();
  await page.route(/.+lb\.drpc\.org\/ogrpc\?network=ethereum.+/, async (route) => {
    if (route.request().method() === 'POST') {
      await route.abort();
    }
  });
});

test('Open Sushi Swap', async () => {
  const requestPromise = page.waitForRequest(
    (request) =>
      request.url().includes('lb.drpc.org/ogrpc?network=ethereum') &&
      request.method() === 'POST',
  );
  await page.goto('/swap');
  const request = await requestPromise;
  await expect(
    () => schema.parse(request.postDataJSON()),
    'Should have a request by the contract',
  ).not.toThrowError();
});
```

Where:
- `const schema` is a scheme declaration in Zod's format;
- In `beforeAll` hook, all POST requests to matching URLs are blocked;
- `const requestPromise` receives data from the first matching request;
- In `expect()` assertion, the reference scheme is parsed against the request's data.

The test may contain more steps because the contract's check may be just a part of the end-to-end suite.

Read more about contract testing at pactflow.io and playwright.dev/docs/mock.

## Further Reading

- What is contract testing and why should I try it?
- Contract Testing Vs Integration Testing
- A Complete Guide to API Contract Testing
- API contract testing: 4 things to validate to meet expectations
- Contract Testing: The Key to Unlocking E2E Testing Bottlenecks in CI/CD pipelines

Furthermore, the same approach to mocking can be applied to all HTTP API requests on frontend when you need to verify request contracts without hitting external services.

