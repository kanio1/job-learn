import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: 'list',
  // F-D5: visual regression tolerance — small pixel diffs (anti-aliasing,
  // font hinting) are expected across machines/CI; only fail on real
  // visual regressions (wrong color, missing element, layout shift).
  //
  // TD-2A: the default 5000ms assertion timeout is too tight for this
  // project's `nuxt dev` webServer. Measured first-render latency for a
  // fresh `/admin/merchants*` navigation (session fetch -> middleware ->
  // Suspense resolution -> component mount) is consistently ~4.1-4.6s in
  // isolation, and regularly exceeds 5s under this machine's default
  // worker parallelism (16 workers sharing one dev server). Several
  // existing specs already work around this per-assertion (e.g.
  // `ui/command-palette.spec.ts` uses an explicit 15000ms timeout on the
  // same "Merchants" heading) rather than via shared config — this raises
  // the default so every spec gets the same headroom without repeating
  // the workaround per assertion. See status/technical-debt/current-baseline.md
  // TD-2 / status/roadmaps/playwright-phase3-roadmap.md for the evidence.
  expect: {
    timeout: 15_000,
    toHaveScreenshot: { maxDiffPixelRatio: 0.02 },
  },
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    // Phase 3A-1 F-A1: APIRequestContext-only tests — no browser, no auth dependency
    {
      name: 'api-tests',
      testMatch: /api\/.*\.spec\.ts/,
    },

    // Auth setup projects
    // F-A2: platform-operator setup (default role, all existing tests)
    {
      name: 'auth-setup',
      testMatch: /auth\/auth\.setup\.ts/,
    },
    // F-A2: merchant-manager setup (Phase 3A-4 — mock-based placeholder, real Keycloak opt-in)
    {
      name: 'merchant-manager-auth-setup',
      testMatch: /auth\/merchant-manager\.setup\.ts/,
    },

    // Test projects
    // Primary project — platform-operator context; all e2e/** tests including rbac/
    {
      name: 'chromium',
      testMatch: /e2e\/.*\.spec\.ts/,
      dependencies: ['auth-setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/platform-operator.json',
      },
    },
    // F-A2: Merchant-manager project skeleton — no tests yet, infrastructure ready.
    // Tests in e2e/merchant-manager/ auto-run with MERCHANT_MANAGER storageState.
    // Role context is still injected per-test via mockRoleSession() (mock-based auth).
    // Enable when: first merchant-manager-specific test is added to e2e/merchant-manager/.
    {
      name: 'chromium-merchant-manager',
      testMatch: /e2e\/merchant-manager\/.*\.spec\.ts/,
      dependencies: ['merchant-manager-auth-setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/merchant-manager.json',
      },
    },
  ],
  webServer: {
    command: 'corepack pnpm dev --host 127.0.0.1 --port 3000',
    url: 'http://127.0.0.1:3000',
    reuseExistingServer: false,
    timeout: 120_000
  }
})
