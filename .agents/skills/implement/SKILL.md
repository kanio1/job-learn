---
name: implement
description: Implement work from a spec or tickets using TDD at agreed seams, then code-review. Use when the user asks to build, implement, or land a spec/ticket.
disable-model-invocation: true
---

# Implement

Build the work described by the spec, tickets, or the current conversation.

## Preconditions

- Seams are agreed (REST Assured, Playwright REST, Playwright E2E, domain unit). If not, grill first.
- Scope fits `AGENTS.md` non-goals.
- Prefer the current spec under `.codex/specs/` or the ticket under `.codex/tickets/`. Historical `specs/` and `.kiro/specs/**` are prior art only.

## Loop

1. Follow `tdd` at the pre-agreed seams. Vertical slices only.
2. Run the **single** failing test, then the focused class/file, regularly.
3. Typecheck frontend when Vue/TS changed (`corepack pnpm typecheck` in `apps/frontend`). Lint TS with `corepack pnpm lint` (vendored anti-slop).
4. Backend: `./mvnw -Dtest=ClassName test` from `apps/backend`. Skip `restkit/` and `paymentsupport/`.
5. When the slice is done, follow `code-review` on the diff.
6. Do **not** commit unless the user explicitly asks.

## Compose

- Java/Spring production: follow `spring-modulith` (public/internal, Flyway, `@ApplicationModuleTest`, JDK 25 allow/deny).
- Nuxt/TypeScript/Nitro production: follow `nuxt-frontend` (app vs server, BFF proxy, Zod, Pinia).
- REST Assured design details: `rest-api-test-design`.
- Playwright E2E / live POM placement: `playwright-pom`. Review: `playwright-sdet-review`.
- Learning explanation after a slice: `implementation-learning-loop` (explain, do not re-implement).

## Done when

Agreed acceptance criteria are green at the agreed seams, `code-review` has been run, and findings the user wants fixed are fixed.
