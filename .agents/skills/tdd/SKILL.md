---
name: tdd
description: Test-driven development with a red-green loop at agreed seams. Use when building features or fixing bugs test-first, writing REST Assured, Playwright E2E, or Playwright REST tests, or when the user mentions red-green-refactor.
---

# Test-Driven Development

TDD is the red → green loop. Consult this skill **before and during** each cycle.

Read `.codex/CONTEXT.md` if it exists so names match domain language. Respect `AGENTS.md` non-goals.

## What a good test is

Tests verify behavior through public interfaces. A good test reads like a specification and survives refactors.

See [tests.md](tests.md) and [mocking.md](mocking.md).

## Confirm seams before any test

A **seam** is the public boundary you test at. No test at an unconfirmed seam.

Ask: "Which seam, and what observable behavior?"

If the interface shape itself is in question, follow `codebase-design`.

### Lab seams (prefer existing ones)

| Seam | Location | Proves |
|---|---|---|
| REST Assured HTTP | `apps/backend/src/test/java/lab/paymentquality/rest` | Status, body, headers, auth, persistence oracle for writes |
| Playwright REST / BFF | `apps/frontend/tests-pom` | Browser-session HTTP against Nuxt BFF / live stack |
| Playwright E2E | `apps/frontend/tests-pom` | User-visible journeys, locators, UI states |
| Security | `apps/backend/src/test/java/lab/paymentquality/security` | Authorities, tenant masking, JWT |
| Domain unit | `*Test.java` next to production | Pure domain rules, no Spring unless needed |

Highest seam that can prove the behavior wins. Do not add an E2E for a header contract that REST Assured already owns. Do not add REST Assured for a button-disabled UI state that only exists in the dashboard.

Skip `restkit/` and `paymentsupport/` unless the user explicitly includes them. Ignore `My*` / `Lesson*` copies as regression coverage.

## Anti-patterns

- **Implementation-coupled** — mocks internal collaborators, tests private methods, asserts call counts. Tell: test breaks on refactor with no behavior change.
- **Tautological** — expected value is recomputed the way the code does. Use an independent literal or spec oracle.
- **Horizontal slicing** — all tests first, then all implementation. Use **vertical slices**: one test → one implementation → repeat.
- **Wrong-layer duplicate** — Playwright E2E re-asserts the same HTTP status matrix already covered by REST Assured.

### Persistence oracle (lab exception)

Matt's original skill treats DB queries as a side channel. In this lab, write-path REST Assured tests **may** assert database state **in addition to** HTTP:

1. Assert HTTP first (status, body, headers).
2. Then assert persistence when the spec cares that the write landed.
3. Prefer a follow-up GET through the same HTTP seam when that GET is part of the contract.

Never use DB-only assertions as a substitute for the HTTP contract.

## Rules of the loop

- **Red before green.** Failing test first, then only enough code to pass.
- **One slice at a time.** One seam, one test, one minimal implementation.
- **Refactor is not this loop.** That belongs to `code-review`.

## Layer recipes

### REST Assured

1. Inspect controller, exception handler, `SecurityConfig`, existing testsupport.
2. Name the test after the behavior (`createsPaymentOrderWithIdempotencyKey`, not `testPost`).
3. Unique references and idempotency keys.
4. Assert status, body, headers (`ETag`, `Idempotency-Key`, `If-Match`, `Location`, `X-Correlation-ID` only where implemented), then persistence if relevant.
5. Run one class from `apps/backend`:

```bash
./mvnw -Dtest=PaymentOrderRestAssuredTest#methodName test
```

Follow `rest-api-test-design` for coverage matrix, assertion style, and data isolation.

### Playwright REST

1. Use existing BFF client / live helpers (`tests-pom/api/bff-client.ts`).
2. Auth via storage state or the live helper — do not invent a new login flow.
3. Assert status, JSON shape (Zod where the suite already does), headers, and no token leak.
4. Run:

```bash
corepack pnpm exec playwright test --config playwright.pom.config.ts tests-pom/specs/<file>
```

### Playwright E2E

1. Stable locators: role, label, visible name. Isolation via unique merchant/payment references.
2. Cover the UI state under test (loading, empty, validation, forbidden, success) — not the full HTTP matrix.
3. Follow `playwright-pom` for placement, then `playwright-sdet-review` for locators/flake.
4. Run (from `apps/frontend`, `@playwright/test` 1.61 — not `playwright-cli`):

```bash
corepack pnpm exec playwright test --config playwright.pom.config.ts tests-pom/specs/<file>
```

Live browser exploration before writing a spec is `playwright-cli`, not this loop.

## Done when

The new test went red, then green, on the agreed seam, and no extra speculative tests were added in the same cycle.
