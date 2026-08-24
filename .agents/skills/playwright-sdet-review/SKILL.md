---
name: playwright-sdet-review
description: Use to review Playwright E2E and Playwright REST/BFF tests for merchant and payment-order journeys; do not use when no frontend or Playwright HTTP change is involved, or for generic Playwright tutorials.
---

# Playwright SDET Review

Use this skill for frontend/E2E and Playwright REST review. When **writing** tests test-first, follow `tdd` then `playwright-pom`. Live browser exploration is `playwright-cli`. Product Vue/Nitro placement is `nuxt-frontend`. This skill owns locators, auth/session, isolation, and flake review.

Suites:

- Live POM (UI + BFF REST): `apps/frontend/tests-pom` — the only product Playwright tree
- Learner copies: `apps/frontend/tests-pom-learner`

## Review Focus

- Stable locators using roles, labels, and visible user-facing names.
- Auth/session setup through the existing `auth-setup` project and storage state.
- Live POM: no `page.route` / `route.fulfill`. Learner copies must follow the same rule.
- Test data isolation with unique merchant/payment references.
- UI states: loading, empty, validation, duplicate/conflict, forbidden, backend unavailable, success, stale state.
- Proxy/header behavior where payment lifecycle relies on `ETag`, `If-Match`, idempotency, or backend error preservation.
- Flake risks: timing, ambiguous text matches, shared state, dependency on seeded external data.

## Current Journeys

- `/admin/merchants`
- `/admin/merchants/{merchantId}/payments`
- `/admin/merchants/{merchantId}/payments/new`
- `/admin/merchants/{merchantId}/payments/{paymentOrderId}`

## Output Format

Return:

1. Findings first.
2. Missing coverage by risk.
3. Suggested focused test names.
4. Locator/data/auth improvements.
5. Exact prompt for the main Codex session if a test change is needed.

## Guardrails

- Do not edit tests directly.
- Do not add broad UI framework abstractions.
- Do not create fake dashboard behavior to make tests pass.

## When not to use

- Implementing POM specs — use `playwright-pom`.
- Reviewing REST Assured / KafkaIT backend tests without the Spring layer — use `java-spring-review`/`rest-api-test-design`.
