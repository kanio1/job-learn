import { defineConfig, devices } from '@playwright/test'

/**
 * FE Mirror / Session lab flag off on a second compose Nuxt (:3012).
 * Login uses PLAYWRIGHT_BASE_URL (OIDC redirect). Tests reuse platform-admin
 * storageState on PLAYWRIGHT_MIRROR_OFF_BASE_URL. Cookie is host-only, not
 * port-only — both origins MUST share the same hostname (127.0.0.1 vs 127.0.0.1).
 * Mixing localhost and 127.0.0.1 drops nuxt-session.
 *
 *   scripts/dev-stack.sh --app
 *   docker compose --env-file infra/compose/.env \
 *     -f infra/compose/compose.yml -f infra/compose/compose.app.http.yml \
 *     -f infra/compose/compose.app.mirror-flag-off.yml up -d payment-quality-frontend-mirror-off
 *   PLAYWRIGHT_SKIP_WEBSERVER=1 PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=... \
 *     corepack pnpm exec playwright test --config playwright.mirror-flag-off.config.ts
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
const loginOrigin = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000'
const flagOffOrigin = process.env.PLAYWRIGHT_MIRROR_OFF_BASE_URL || 'http://127.0.0.1:3012'

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
      name: 'chromium-mirror-flag-off',
      testMatch: /specs\/mirror-lab-flag-off\.spec\.ts/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        baseURL: flagOffOrigin,
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
  ],
})
