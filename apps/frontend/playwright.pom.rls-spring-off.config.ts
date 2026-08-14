import { defineConfig, devices } from '@playwright/test'

/**
 * PW-RFC-E2E-060 — FE RLS lab on, Spring `app.rls-lab.enabled=false`.
 * Requires HTTP stack with RLS_LAB_ENABLED=false on Spring, then:
 *   PLAYWRIGHT_RLS_SPRING_OFF=1 PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=... \
 *     corepack pnpm exec playwright test --config playwright.pom.rls-spring-off.config.ts
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
  },
  projects: [
    {
      name: 'setup-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
    },
    {
      name: 'chromium-rls-spring-off',
      testMatch: /specs\/rls-lab-spring-off\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
  ],
  webServer: {
    command: 'NUXT_TYPECHECK=false corepack pnpm dev --host 127.0.0.1 --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 120_000,
  },
})
