import { defineConfig, devices } from '@playwright/test'

/**
 * FE RLS hub on, Spring `RLS_LAB_ENABLED=false` on a second backend (:8082)
 * and Nuxt on :3011. Login stays on :3000 (OIDC).
 *
 *   scripts/run-app-stack-tests.sh --rls-spring-off
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
process.env.PLAYWRIGHT_RLS_SPRING_OFF = '1'
const loginOrigin = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000'
const springOffOrigin = process.env.PLAYWRIGHT_RLS_SPRING_OFF_BASE_URL || 'http://127.0.0.1:3011'

export default defineConfig({
  testDir: './tests-pom',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  expect: { timeout: 15_000 },
  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
      use: { baseURL: loginOrigin },
    },
    {
      name: 'chromium-rls-spring-off',
      testMatch: /specs\/rls-lab-spring-off\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        baseURL: springOffOrigin,
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
  ],
})
