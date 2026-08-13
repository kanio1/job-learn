import { defineConfig, devices } from '@playwright/test'

/**
 * Learner copy of the live POM suite. Copy auth/ (and page objects) from tests-pom
 * before adding My*.spec.ts. Do not use page.route / route.fulfill.
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom-learner/.auth'
export default defineConfig({
  testDir: './tests-pom-learner',
  passWithNoTests: true,
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
    ...devices['Desktop Chrome'],
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
      testMatch: /specs\/.*session-guest.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: { cookies: [], origins: [] },
      },
    },
    {
      name: 'chromium-admin',
      testMatch: /specs\/.*\.spec\.ts/,
      testIgnore: /specs\/(.*payments-.*|.*auth-rbac.*|.*session-guest.*)\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom-learner/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-manager',
      testMatch: /specs\/(.*payments-.*|.*support.*)\.spec\.ts/,
      dependencies: ['setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom-learner/.auth/merchant-manager.json',
      },
    },
    {
      name: 'chromium-rbac',
      testMatch: /specs\/.*auth-rbac.*\.spec\.ts/,
      dependencies: ['setup-platform-admin', 'setup-merchant-manager'],
      use: devices['Desktop Chrome'],
    },
  ],
  webServer: {
    command: 'corepack pnpm dev --host 127.0.0.1 --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 120_000,
  },
})
