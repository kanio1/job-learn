# Full parallelization in Playwright

How many workers should we setup in our config? How many shards should we use in our CI? Do we need fullyParallel true in Playwright config?

## SETUP

Install playwright: `npm init playwright@latest`

Create test files with testInfo logging to discover how Playwright splits tests for full parallelization. Use multiple spec files with describe blocks and a serial.spec.ts with `test.describe.configure({ mode: 'serial' })`.

Configure playwright.config.ts:

```typescript
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  workers: process.env.CI ? 3 : 1,
  reporter: 'html',
  projects: [{ name: 'x', use: { ...devices['Desktop Chrome'] } }],
});
```

`fullyParallel: true` means workers are assigned spec files, describe blocks, and individual tests are mixed for load balancing.

## RESULTS

With two shards in CI, work is balanced across machines. Serial mode files run all tests in order on the same worker.

Without fullyParallel, Playwright splits at spec file level only - less efficient.

For worker count, test your environments to find optimal values. For sharding, consider cost, setup time, and CPU capacity tradeoffs.
