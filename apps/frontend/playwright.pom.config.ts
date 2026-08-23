import { defineConfig, devices } from '@playwright/test'
import './tests-pom/utils/ipv4-first'
import { POM_WORKER_COUNT } from './tests-pom/auth/accounts'

/**
 * Live POM framework — real Keycloak + Nuxt BFF + Spring + Postgres.
 * Do not use page.route / route.fulfill in this suite.
 *
 * Browser origin is `localhost` (must match NUXT_OAUTH_OIDC_REDIRECT_URL).
 * Node BffClient rewrites that host to 127.0.0.1 (IPv4).
 *
 * Against compose `--app` (HTTP, no Caddy) set PLAYWRIGHT_SKIP_WEBSERVER=1
 * so Playwright does not start host `pnpm dev` on :3000.
 *
 * Workers: CI=2, local=4, never above the four seeded MERCHANT-Wn worlds.
 * Logout (`chromium-session`) uses its own storageState file.
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
const skipWebServer = process.env.PLAYWRIGHT_SKIP_WEBSERVER === '1'
const includeVisual = process.env.PLAYWRIGHT_VISUAL === '1'
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000'
const workerCap = Number(process.env.PLAYWRIGHT_WORKERS)
const requested = Number.isFinite(workerCap) && workerCap > 0
  ? workerCap
  : (process.env.CI ? 2 : 4)
const workers = Math.min(requested, POM_WORKER_COUNT)
export default defineConfig({
  testDir: './tests-pom',
  fullyParallel: true,
  forbidOnly: true,
  retries: 0,
  workers,
  reporter: 'list',
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'setup-platform-admin',
      testMatch: /auth\/platform-admin\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-platform-operator',
      testMatch: /auth\/platform-operator\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-platform-admin-session',
      testMatch: /auth\/platform-admin-session\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-tenant-admin',
      testMatch: /auth\/tenant-admin\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-merchant-manager',
      testMatch: /auth\/merchant-manager\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-worker-managers',
      testMatch: /auth\/worker-managers\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-support-agent',
      testMatch: /auth\/support-agent\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-read-only-user',
      testMatch: /auth\/read-only-user\.setup\.ts/,
      fullyParallel: false,
    },
    {
      name: 'setup-merchant-denied',
      testMatch: /auth\/merchant-denied\.setup\.ts/,
      fullyParallel: false,
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
      testMatch: /specs\/(merchants|merchants-table|merchants-slideover|merchants-concurrency|merchants-conflict|merchants-unsaved|merchants-import|merchants-tree|users|audit|error-lab|checkout-lab|session-lab|network-lab|mirror-lab|command-palette|internal-notes|merchant-risk|support-admin|support-kanban|support-bulk|admin-bff|a11y-axe|psp-redirect|ops-feed|ops-notifications|ops-search|event-lab)\.spec\.ts/,
      dependencies: ['setup-platform-admin', 'setup-platform-operator', 'setup-tenant-admin', 'setup-read-only-user', 'setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-manager',
      testMatch: /specs\/(payments-(?!refund-dual-control|pin).*|support|error-lab-manager)\.spec\.ts/,
      fullyParallel: true,
      dependencies: ['setup-worker-managers', 'setup-merchant-denied', 'setup-platform-operator'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: { cookies: [], origins: [] },
      },
    },
    {
      name: 'chromium-serial',
      testMatch: /specs\/(tenant-settings|tenant-policy|payments-refund-dual-control|payments-pin)\.spec\.ts/,
      fullyParallel: false,
      workers: 1,
      dependencies: ['setup-platform-admin', 'setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-session',
      testMatch: /specs\/session\.spec\.ts/,
      fullyParallel: false,
      dependencies: ['setup-platform-admin-session'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin-session.json',
      },
    },
    {
      name: 'chromium-worker',
      testMatch: /specs\/worker-world\.spec\.ts/,
      fullyParallel: true,
      dependencies: ['setup-worker-managers'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: { cookies: [], origins: [] },
      },
    },
    ...(includeVisual
      ? [{
          name: 'chromium-visual',
          testMatch: /specs\/(visual-lab|aria-snapshots)\.spec\.ts/,
          fullyParallel: false,
          dependencies: ['setup-platform-admin', 'setup-merchant-manager'],
          grepInvert: /@visual-negative/,
          use: {
            ...devices['Desktop Chrome'],
            storageState: './tests-pom/.auth/platform-admin.json',
          },
        }]
      : []),
    {
      name: 'locale',
      testMatch: /specs\/locale-.*\.spec.ts/,
      dependencies: ['setup-platform-admin', 'setup-read-only-user', 'setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-rbac',
      testMatch: /specs\/(auth-rbac|tenant-scope|mirror-lab-rbac|rls-lab|support-rbac|readonly-rbac|denied-rbac|merchants-rbac-columns)\.spec\.ts/,
      fullyParallel: false,
      dependencies: ['setup-platform-admin', 'setup-tenant-admin', 'setup-merchant-manager', 'setup-support-agent', 'setup-read-only-user', 'setup-merchant-denied'],
      use: devices['Desktop Chrome'],
    },
  ],
  ...(skipWebServer
    ? {}
    : {
        webServer: {
          command: 'NUXT_TYPECHECK=false corepack pnpm dev --host 127.0.0.1 --port 3000',
          url: 'http://127.0.0.1:3000',
          reuseExistingServer: true,
          timeout: 120_000,
        },
      }),
})
