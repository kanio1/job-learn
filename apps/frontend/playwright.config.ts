/**
 * Default Playwright entry is the live POM suite (real Keycloak + BFF + Spring).
 * Product Playwright is `tests-pom` only. Do not point this file at mocked suites.
 *
 * Against compose `--app`: PLAYWRIGHT_SKIP_WEBSERVER=1 (set by `pnpm test:e2e`
 * and `scripts/run-app-stack-tests.sh`).
 */
export { default } from './playwright.pom.config.ts'
