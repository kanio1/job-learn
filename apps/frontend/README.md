# Frontend Foundation

This frontend is the Phase 0 Nuxt dashboard foundation for the Payment Quality Engineering Lab.

## Template Source

The frontend uses the official Nuxt UI Dashboard Template as the baseline reference:

```text
https://github.com/nuxt-ui-templates/dashboard
```

Validated template package reference on 2026-05-18:
- Template package name: `nuxt-ui-template-dashboard`
- Nuxt UI: `^4.7.1`
- Nuxt: `^4.4.5`
- TypeScript: `^6.0.3`
- Zod: `^4.4.3`

Implementation approach: create a minimal project-specific dashboard shell using the validated template dependency baseline and Nuxt UI primitives, then remove or avoid demo content that could imply payment business behavior. Nuxt 4 executes the app from the `app/` source directory in this project.

## Baseline

- Nuxt 4.4.6
- Nuxt UI 4.7.1
- TypeScript 6.0.3
- Zod 4.4.3
- Pinia via `@pinia/nuxt` 0.11.3
- pnpm 10.33.4
- Playwright 1.60.0

## Commands

```bash
pnpm install
pnpm dev
pnpm typecheck
pnpm build
corepack pnpm exec playwright test
```

If `pnpm` is not installed as a shell command, use Corepack: `corepack pnpm <command>`.

## Phase 0 Shell

The root route is implemented at `app/pages/index.vue` and displays a foundation page for `Payment Quality Engineering Lab`. It identifies future dashboard areas but does not implement them:
- Merchant
- Admin
- Payment operations
- Risk/review
- Reconciliation

## Deferred Work

- No payment creation or payment list
- No payment API client
- No real login UX
- No role-based navigation
- No Keycloak-backed route guards
- No complete business dashboard functionality

## Parallel-Ready E2E Conventions

The Playwright baseline avoids shared users, payment records, and mutable business data. Future mutating tests should use worker-aware fixtures, unique resources, and resilient locators based on roles, labels, or stable test ids.
