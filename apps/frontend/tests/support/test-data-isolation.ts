/**
 * F-A4: Worker-aware data isolation helpers.
 *
 * ISOLATION STRATEGY
 * ──────────────────
 * Each Playwright worker gets a deterministic prefix derived from its workerIndex.
 * Test data (merchants, payment orders) created during a run must use these prefixes
 * so parallel workers never mutate each other's records.
 *
 *   worker 0 → prefix "W0" → merchant refs like "TEST-W0-CREATE"
 *   worker 1 → prefix "W1" → merchant refs like "TEST-W1-CREATE"
 *
 * CURRENT STATUS: fullyParallel is OFF (playwright.config.ts). All tests run in a
 * single worker. Enable fullyParallel only after verifying that every test that
 * mutates shared state uses worker-prefixed identifiers (see docs below).
 *
 * SEED / RESET ENDPOINTS (F-A4 API helpers)
 * ──────────────────────────────────────────
 * The backend provides:
 *   POST /api/test/reset  — clears all tenants/merchants/payments
 *   POST /api/test/seed   — clears and seeds a deterministic dataset
 *
 * These are BLOCKED in the current CI setup because:
 *   1. The Playwright test run starts only the Nuxt dev server (port 3000).
 *      The Spring backend (port 8080) is NOT started automatically.
 *   2. app.testing.enabled=false by default in application.yml.
 *      Set to true only for manual or dedicated backend test runs.
 *   3. No BFF proxy route exists for /api/test/* endpoints.
 *      Playwright must call the backend directly via PLAYWRIGHT_BACKEND_URL.
 *
 * To use seed/reset:
 *   export PLAYWRIGHT_BACKEND_URL=http://localhost:8080
 *   SPRING_PROFILES_ACTIVE=dev APP_TESTING_ENABLED=true ./mvnw spring-boot:run
 *   corepack pnpm exec playwright test tests/api/seed-reset-smoke.spec.ts
 *
 * WHEN TO ENABLE fullyParallel
 * ─────────────────────────────
 * Prerequisites (from docs/testing/playwright-auth-and-data-isolation.md):
 *   1. Every mutation test uses worker-prefixed identifiers (this module).
 *   2. A global beforeAll resets AND seeds per-worker data.
 *   3. No test reads records created by another worker.
 *   4. The backend seed/reset API is available and fast enough.
 */

import type { APIRequestContext } from '@playwright/test'

/** Deterministic worker prefix. Worker 0 → "W0", worker 1 → "W1". */
export function workerPrefix(workerIndex: number): string {
  return `W${workerIndex}`
}

/**
 * Worker-scoped merchant reference for mutation tests.
 * Format: TEST-W{worker}-{label}   e.g. "TEST-W0-CREATE"
 */
export function isolatedMerchantRef(workerIndex: number, label: string): string {
  return `TEST-${workerPrefix(workerIndex)}-${label}`
}

/**
 * Worker-scoped payment client order reference for mutation tests.
 * Format: PAY-W{worker}-{label}    e.g. "PAY-W0-LIFECYCLE"
 */
export function isolatedPaymentRef(workerIndex: number, label: string): string {
  return `PAY-${workerPrefix(workerIndex)}-${label}`
}

const BACKEND_URL = process.env.PLAYWRIGHT_BACKEND_URL || 'http://localhost:8080'

/**
 * POST /api/test/reset — clears the entire test dataset.
 * BLOCKED until backend running with app.testing.enabled=true.
 */
export async function resetTestData(request: APIRequestContext): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/test/reset`)
  if (!response.ok()) {
    throw new Error(`reset failed: ${response.status()} — ${await response.text()}`)
  }
}

/**
 * POST /api/test/seed — clears then seeds a deterministic dataset.
 * Deterministic data is defined in Fixtures.java (all IDs/refs are constants).
 * BLOCKED until backend running with app.testing.enabled=true.
 */
export async function seedTestData(request: APIRequestContext): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/test/seed`)
  if (!response.ok()) {
    throw new Error(`seed failed: ${response.status()} — ${await response.text()}`)
  }
}
