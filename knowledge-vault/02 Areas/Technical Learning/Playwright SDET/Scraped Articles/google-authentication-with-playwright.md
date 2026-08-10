# Google Authentication with Playwright

Use globalSetup with playwright-extra + puppeteer-extra-plugin-stealth to sign in via Google UI once, save storageState to reuse across tests.

## Steps
1. Configure globalSetup and storageState in playwright.config.ts
2. In global-setup: launch chromium with stealth, navigate to login, fill Google credentials (handle old/new form variants), wait for redirect, save storageState
3. Add storage-state.json to .gitignore
4. Use SKIP_AUTH env to skip re-auth during debugging

Note: playwright-extra/stealth maintenance stopped March 2023 — approach may be stale.

