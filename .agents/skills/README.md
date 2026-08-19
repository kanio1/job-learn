# Engineering process skills

Adapted from [mattpocock/skills](https://github.com/mattpocock/skills). See [SOURCE.md](SOURCE.md).

Work lives as local markdown under `.codex/` — `docs/agents/issue-tracker.md`.

This directory is the **single skill home** for Grok Build, Codex CLI, and Cursor (Cursor loads it via `.cursor/skills/` symlinks). Process skills and lab domain skills both live here.

## When to reach for which

| Situation | Skill |
|---|---|
| Unsure which flow to use | `ask-engineering-flow` |
| Align before building | `grill-me` / `grill-with-docs` |
| Foggy work bigger than one session | `wayfinder` |
| Incoming bug/request (not a to-tickets item) | `triage` |
| Turn a discussion into a spec | `to-spec` |
| Split a spec into vertical slices | `to-tickets` |
| Build the work test-first | `implement` (drives `tdd`) |
| Write REST Assured, Playwright E2E, or Playwright REST tests | `tdd` |
| Logic/UI cannot be settled on paper | `prototype` |
| Primary-source reading | `research` |
| Human-only setup (Keycloak, mkcert, secrets) | `wizard` |
| Learn a concept over sessions | `teach` |
| Review a branch or diff | `code-review` |
| Hard bug / flake / regression | `diagnosing-bugs` |
| Module interface, seam, testability | `codebase-design` |
| Java 25 / Spring Boot 4 / Modulith 2.0.6 production code | `spring-modulith` |
| Nuxt 4 / TypeScript 6 / Node 22+ production frontend | `nuxt-frontend` |
| Playwright 1.61 POM / fixtures / live tests-pom | `playwright-pom` |
| Hand off to another session | `handoff` |
| Confirm tracker layout | `setup-lab-engineering-skills` |

## Compose with existing lab skills

| Layer | Process skill | Domain / review skill |
|---|---|---|
| Java/Spring production code | `implement`, `spring-modulith`, `codebase-design` | `java-spring-review` |
| Nuxt / TypeScript / Nitro BFF | `implement`, `nuxt-frontend`, `codebase-design` | `nuxt-dashboard-zod-pinia-frontend-engineering` |
| REST Assured | `tdd` | `rest-api-test-design`, `junit6-assertj-restassured-testcraft` |
| Playwright E2E / live POM | `tdd`, `playwright-pom` | `playwright-sdet-review` |
| Playwright REST / BFF HTTP | `tdd`, `playwright-pom` | `rest-api-test-design`, `playwright-sdet-review` |
| Test conditions / data | `to-spec` Testing Decisions | `test-analysis-design-and-data` |
| Versioned stack facts | `research` | `official-docs-and-versioned-research` |
| BA / discovery before spec | `grill-me` | `business-analysis-and-product-discovery-for-payment-lab` |
| Test conditions / ISTQB | `to-spec` | `test-analysis-design-and-data`, `rapid-software-testing-risk-thinking` |
| REST security / Keycloak | `tdd` | `rest-api-security-oauth-testing` |
| Dashboard taste / a11y polish | `nuxt-frontend` | `nuxt-dashboard-zod-pinia-frontend-engineering` |

User-invoked: `ask-engineering-flow`, `grill-me`, `grill-with-docs`, `wayfinder`, `triage`, `to-spec`, `to-tickets`, `implement`, `teach`, `handoff`, `setup-lab-engineering-skills`.

Model-invoked: `grilling`, `tdd`, `code-review`, `codebase-design`, `spring-modulith`, `nuxt-frontend`, `playwright-pom`, `diagnosing-bugs`, `prototype`, `research`, `wizard`, plus domain skills in this directory when their description matches.
