import { defineConfig, devices } from '@playwright/test'

/**
 * Required live assurance suite. It has no session mocks and requires a real
 * Keycloak/seeded-backend environment. It remains separate from the mocked
 * Chromium baseline so a developer cannot accidentally turn placeholder auth
 * into evidence for RBAC or BFF contracts.
 */
export default defineConfig({
  testDir: './tests/live',
  forbidOnly: true,
  retries: 0,
  reporter: 'list',
  use: {
    // The Keycloak client allow-list and the OIDC redirect URI use localhost.
    // Keep the browser origin identical so PKCE state cookies return to the
    // same host during the authorization-code callback.
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'live-auth-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
    },
    {
      name: 'live-auth-merchant-manager',
      testMatch: /auth\/merchant-manager\.setup\.ts/,
    },
    {
      name: 'live-multi-role',
      testMatch: /auth\/multi-role\.spec\.ts/,
      dependencies: ['live-auth-platform-admin', 'live-auth-merchant-manager'],
      use: devices['Desktop Chrome'],
    },
    {
      name: 'live-http-merchant-manager',
      testMatch: /http\/.*\.spec\.ts/,
      dependencies: ['live-auth-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/live-merchant-manager.json',
      },
    },
    {
      name: 'live-parallel-merchant-manager',
      testMatch: /parallel\/.*\.spec\.ts/,
      dependencies: ['live-auth-merchant-manager'],
      workers: 2,
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/.auth/live-merchant-manager.json',
      },
    },
  ],
  webServer: {
    command: 'corepack pnpm dev --host 127.0.0.1 --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: false,
    timeout: 120_000,
  },
})
