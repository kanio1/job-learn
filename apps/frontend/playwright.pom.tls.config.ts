import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { createRequire } from 'node:module'
import { defineConfig, devices } from '@playwright/test'

createRequire(import.meta.url)('../../scripts/tls-lab-node-preload.cjs')

/**
 * Point Node (BffClient / APIRequestContext) at the mkcert CA. Chromium does
 * not read NODE_EXTRA_CA_CERTS — it uses the OS trust store ({@code mkcert -install})
 * unless PLAYWRIGHT_TLS_INSECURE=1.
 */
function applyMkcertCaForNode(): void {
  if (process.env.NODE_EXTRA_CA_CERTS) {
    return
  }
  const mkcertBin = existsSync(`${process.env.HOME}/.local/bin/mkcert`)
    ? `${process.env.HOME}/.local/bin/mkcert`
    : 'mkcert'
  try {
    const caroot = execFileSync(mkcertBin, ['-CAROOT'], { encoding: 'utf8' }).trim()
    const ca = `${caroot}/rootCA.pem`
    if (existsSync(ca)) {
      process.env.NODE_EXTRA_CA_CERTS = ca
    }
  }
  catch {
    // mkcert missing: Node HTTPS calls need PLAYWRIGHT_TLS_INSECURE=1
  }
}

applyMkcertCaForNode()

const tlsInsecure = process.env.PLAYWRIGHT_TLS_INSECURE === '1'

/**
 * Live POM against the TLS overlay (Caddy + mkcert).
 * Requires scripts/dev-stack.sh --tls.
 * Credentials only from the environment.
 *
 * Certificate trust:
 *   1. Preferred: {@code mkcert -install} so Chromium trusts the local CA.
 *   2. Lab escape hatch: PLAYWRIGHT_TLS_INSECURE=1 (ignoreHTTPSErrors). A green
 *      suite with (2) does not prove the browser trusts mkcert.
 */
process.env.PLAYWRIGHT_POM_AUTH_DIR ??= 'tests-pom/.auth'
process.env.PLAYWRIGHT_BASE_URL ??= 'https://app.payment-quality.local:8443'
export default defineConfig({
  testDir: './tests-pom',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  expect: {
    timeout: 20_000,
  },
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'https://app.payment-quality.local:8443',
    ignoreHTTPSErrors: tlsInsecure,
    launchOptions: {
      args: [
        '--host-resolver-rules=MAP app.payment-quality.local 127.0.0.1,MAP api.payment-quality.local 127.0.0.1,MAP auth.payment-quality.local 127.0.0.1',
      ],
    },
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
      name: 'chromium-tls-admin',
      testMatch: /specs\/tls-lab\.spec.ts/,
      grep: /platform admin/,
      dependencies: ['setup-platform-admin'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/platform-admin.json',
      },
    },
    {
      name: 'chromium-tls-manager',
      testMatch: /specs\/tls-lab\.spec.ts/,
      grep: /merchant manager/,
      dependencies: ['setup-merchant-manager'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests-pom/.auth/merchant-manager.json',
      },
    },
  ],
  webServer: {
    // Fallback only. Prefer scripts/dev-stack.sh --tls (already bound 0.0.0.0 for Caddy).
    command: 'corepack pnpm dev --host 0.0.0.0 --port 3000',
    url: 'http://127.0.0.1:3000',
    reuseExistingServer: true,
    timeout: 120_000,
  },
})
