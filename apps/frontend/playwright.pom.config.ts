import { defineConfig, devices } from '@playwright/test'

/**
 * Live POM framework — real Keycloak + Nuxt BFF + Spring + Postgres.
 * Do not use page.route / route.fulfill in this suite.
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
export default defineConfig({
  testDir: './tests-pom',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'setup-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
    },
    {
      name: 'setup-merchant-manager',
      testMatch: /auth\/merchant-manager\.setup\.ts/,
    },
    {
      name: 'chromium-guest',
      testMatch: /specs\/session-guest\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: { cookies: [], origins: [] },
      },
    },
    {
      name: 'chromium-admin',
      testMatch: /specs\/(merchants|users|audit|tenant-settings|error-lab|checkout-lab|session|session-lab|network-lab|mirror-lab|command-palette|internal-notes|merchant-risk|support-admin|admin-bff)\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-manager',
      testMatch: /specs\/(payments-.*|support|error-lab-manager)\.spec\.ts/,
      dependencies: ['setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/merchant-manager.json',
      },
    },
    {
      name: 'chromium-rbac',
      testMatch: /specs\/(auth-rbac|mirror-lab-rbac|rls-lab)\.spec\.ts/,
      dependencies: ['setup-platform-admin', 'setup-merchant-manager'],
      use: devices['Desktop Chrome'],
    },
  ],
  webServer: {
    command: 'NUXT_TYPECHECK=false corepack pnpm dev --host 127.0.0.1 --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 120_000,
  },
})
