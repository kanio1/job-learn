import { defineConfig, devices } from '@playwright/test'
import { pomAuthFiles } from './tests-pom/utils/env'

/**
 * FE RLS lab flag off on a second compose Nuxt (:3010).
 * Login uses :3000 (OIDC redirect). Tests reuse platform-admin storageState on :3010.
 *
 *   scripts/dev-stack.sh --app
 *   docker compose --env-file infra/compose/.env \
 *     -f infra/compose/compose.yml -f infra/compose/compose.app.http.yml \
 *     -f infra/compose/compose.app.rls-flag-off.yml up -d payment-quality-frontend-rls-off
 *   PLAYWRIGHT_SKIP_WEBSERVER=1 PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=... \
 *     corepack pnpm exec playwright test --config playwright.rls-flag-off.config.ts
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
const loginOrigin = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000'
const flagOffOrigin = process.env.PLAYWRIGHT_RLS_OFF_BASE_URL || 'http://127.0.0.1:3010'
process.env.PLAYWRIGHT_BFF_BASE_URL ??= flagOffOrigin

export default defineConfig({
  testDir: './tests-pom',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  expect: { timeout: 15_000 },
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'setup-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
      use: { baseURL: loginOrigin },
    },
    {
      name: 'chromium-rls-flag-off',
      testMatch: /specs\/rls-lab-flag-off\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        baseURL: flagOffOrigin,
        storageState: pomAuthFiles.platformAdmin,
      },
    },
  ],
})
