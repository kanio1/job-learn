import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: 'list',
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
