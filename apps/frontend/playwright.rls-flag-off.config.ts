import { defineConfig, devices } from '@playwright/test'

/**
 * GAP-RFC-T01 — FE RLS lab flag off.
 * Own Nuxt on :3010 so a running HTTP stack on :3000 is not reused.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  reporter: 'list',
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL: 'http://127.0.0.1:3010',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium-rls-flag-off',
      testMatch: /e2e\/rls-lab-flag-off\.spec\.ts/,
      use: devices['Desktop Chrome'],
    },
  ],
  webServer: {
    command: 'NUXT_PUBLIC_RLS_LAB_ENABLED=false corepack pnpm dev --host 127.0.0.1 --port 3010',
    url: 'http://127.0.0.1:3010',
    reuseExistingServer: false,
    timeout: 120_000,
  },
})
