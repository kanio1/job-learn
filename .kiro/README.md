# Kiro Configuration — Payment Quality Engineering Lab

This directory contains Kiro IDE configuration for the Payment Quality Engineering Lab project.

---

## Directory Structure

```
.kiro/
├── skills/          # Kiro workspace skills (22 skills adapted from .kilocode/skills/)
├── steering/        # Persistent project context injected into every Kiro session
├── specs/           # Kiro Feature Specs (requirements.md / design.md / tasks.md)
└── README.md        # This file
```

---

## Skills (`/.kiro/skills/`)

Kiro workspace skills provide domain knowledge and behavioural guidance that activates automatically based on the task at hand.

Each skill lives in its own folder and must contain a `SKILL.md` with this frontmatter:

```yaml
---
name: skill-name          # matches folder name, kebab-case
description: Use when...  # short, activation-friendly trigger phrase
---
```

### Installed skills

| Skill | Activates when |
|---|---|
| `bpmn-uml-dmn-for-testers` | Modeling business processes, lifecycles, or decision tables |
| `business-analysis-and-product-discovery-for-payment-lab` | Proposing or sequencing product capabilities before a spec |
| `java-practitioner-craftsmanship-and-conference-insights` | Mining Java talks, books, or conference insights |
| `java-rest-api-testing-effective-java-mentor` | Designing or reviewing Java JDK 25 REST API tests |
| `java25-effective-java-mentor` | Teaching Effective Java through the full item track |
| `junit6-assertj-restassured-testcraft` | Designing or reviewing Java test suites |
| `maven-3-9-11-build-engineering` | Maintaining the Maven build, Surefire/Failsafe strategy |
| `nuxt-dashboard-zod-pinia-frontend-engineering` | Designing or reviewing the Nuxt 4 dashboard frontend |
| `obsidian-learning-os` | Maintaining the Obsidian learning vault |
| `official-docs-and-versioned-research` | Verifying version-sensitive technology decisions |
| `parallel-test-architecture-and-data-isolation` | Designing parallel-safe test suites |
| `payment-quality-lab-orchestrator` | Coordinating project phases across multiple skills |
| `postgres18-data-architecture-and-risk` | Designing PostgreSQL data structures or test data strategy |
| `project-skill-governance-and-quality-review` | Reviewing or evolving the skill collection |
| `rapid-software-testing-risk-thinking` | Applying RST thinking, risk statements, exploratory charters |
| `rest-api-security-oauth-testing` | Designing or testing REST security, Keycloak, OAuth/OIDC |
| `spring-boot4-spring7-backend-architect` | Designing or reviewing Spring Boot 4 backend architecture |
| `spring-modulith-2-0-6-modular-monolith-testing` | Working with Spring Modulith module boundaries and tests |
| `test-analysis-design-and-data` | Turning requirements and risks into test conditions |
| `typescript6-playwright-engineering` | Designing or implementing Playwright 1.60 + TypeScript 6 tests |
| `web-research-and-data-extraction` | Performing broader research or repo mining |

### Engineering process skills (Cursor / Codex / OpenCode)

Canonical copies live in `.agents/skills/` (adapted from mattpocock/skills). Tracker: `docs/agents/issue-tracker.md`.

| Skill | Activates when |
|---|---|
| `ask-engineering-flow` | Choosing grill vs spec vs TDD vs review |
| `grill-me` / `grill-with-docs` | Aligning before a change |
| `to-spec` / `to-tickets` | Writing a spec or tracer-bullet tickets under `.codex/` |
| `tdd` / `implement` | Building test-first (REST Assured, Playwright E2E, Playwright REST) |
| `wayfinder` | Foggy work bigger than one session |
| `triage` | Incoming bugs/requests |
| `prototype` | Throwaway logic HTML or Nuxt UI variants |
| `research` | Cited primary-source notes (Firecrawl) |
| `wizard` | Human-only Keycloak/mkcert/secrets steps |
| `teach` | Multi-session learning under `.codex/teach` |
| `code-review` | Two-axis review of a diff |
| `diagnosing-bugs` | Hard bugs and flakes |
| `handoff` | Session handoff |

See `.agents/skills/README.md`.

---

## Steering (`/.kiro/steering/`)

Steering files provide persistent project context that is injected into every Kiro session automatically.

| File | Purpose |
|---|---|
| `product.md` | Project purpose, business domains (merchants / payment orders / lifecycle), learning goals, non-goals |
| `tech.md` | Stack versions, build commands, preferred approach (thin abstractions, small changes) |
| `structure.md` | Repository layout, backend/frontend directory conventions, where things live |
| `rest-api-contracts.md` | HTTP contract rules: headers (ETag, If-Match, Idempotency-Key, Location, Vary, X-Correlation-ID), status codes, problem+json shape, Error Lab scenarios |
| `frontend-nuxt-ui.md` | Nuxt UI Dashboard conventions, composable architecture, Zod schemas, component inventory, data-testid placement |
| `testing-strategy.md` | Test layer hierarchy, RestKit conventions, REST Assured assertions, ProblemDetails/Header assertion helpers, Playwright locator strategy |

---

## Specs (`/.kiro/specs/`)

Feature Specs follow the requirements-first workflow and produce three documents:

```
.kiro/specs/{feature-name}/
├── .config.kiro       # specId, specType (feature/bugfix), workflowType
├── requirements.md    # User stories + EARS acceptance criteria
├── design.md          # Architecture, components, sequence diagrams, correctness properties
└── tasks.md           # Small, dependency-ordered implementation tasks
```

Use **Kiro Spec Mode** to create or continue specs. Do not implement before requirements.md and design.md are reviewed and approved.

### Current specs

| Spec | Status |
|---|---|
| `payment-operations-dashboard` | requirements.md ✓ · design.md ✓ · tasks.md pending |

---

## Dual Kilo Code / Kiro Setup

This project uses **both** Kilo Code and Kiro IDE. Both skill collections are maintained in parallel.

| Location | Used by | Status |
|---|---|---|
| `.kilocode/skills/` | Kilo Code | Primary source — do not delete |
| `.kiro/skills/` | Kiro IDE | Adapted copies — maintain alongside Kilo Code |

### Why skills are duplicated intentionally

- Kilo Code uses its own skill format (metadata section, license field).
- Kiro uses a simpler frontmatter format (`name` + `description` only).
- New work uses `.agents/skills/` process skills (`to-spec`, `to-tickets`, `implement`) rather than Spec Kit or Kiro Spec Mode.

### Keeping skills in sync

When you update a skill that both tools use:
1. Edit the skill in **both** `.kilocode/skills/<name>/SKILL.md` **and** `.kiro/skills/<name>/SKILL.md`.
2. In `.kilocode/skills/`, preserve the existing Kilo Code format (metadata, license).
3. In `.kiro/skills/`, keep only `name` + `description` frontmatter and replace any Kilo-specific wording.

To copy all Kilo Code skills to Kiro and review for adaptation:

```bash
# Manual sync — review each file after copying for Kiro-specific frontmatter and wording
for skill_dir in .kilocode/skills/*/; do
  skill_name=$(basename "$skill_dir")
  mkdir -p ".kiro/skills/$skill_name"
  cp "$skill_dir/SKILL.md" ".kiro/skills/$skill_name/SKILL.md"
  echo "Copied $skill_name — review frontmatter and Kilo-specific wording"
done
```

> **Note**: No `scripts/` directory exists in this repository, so no automated sync script was added. Run the shell loop above manually when needed, then review each `.kiro/skills/*/SKILL.md` for:
> - Valid frontmatter (`name` + `description` only — remove `metadata`, `license` fields)
> - Kilo-specific wording replaced with Kiro equivalents (see table below)

### Kilo → Kiro wording reference

| Replace (Kilo Code) | With (Kiro) |
|---|---|
| `Use Kilo Code Orchestrator mode` | `Use engineering process skills in .agents/skills/` |
| `Use Kilo Code Architect mode` | `Produce a .codex spec and tickets before implementation` |
| `Run in Kilo mode` | `Ask for approval before large implementation steps` |

---

## Rules for Kiro Skills

Every `.kiro/skills/<name>/SKILL.md` must:

1. Start with valid YAML frontmatter:
   ```yaml
   ---
   name: skill-name
   description: Use when...
   ---
   ```
2. `name` must match the folder name (kebab-case, lowercase, hyphens only).
3. `description` must be specific and activation-friendly — start with "Use when".
4. Must not contain Kilo-specific metadata fields (`license`, `metadata.category`, `metadata.author`, `metadata.version`).
5. Must not reference Spec Kit or `/speckit.*` commands.

---

## What Not to Change

- **Do not delete or modify `.kilocode/skills/`** — it remains the source of truth for Kilo Code.
- **Do not modify application source code** (`apps/backend/`, `apps/frontend/`) for tooling setup tasks.
- **Do not commit secrets**, tokens, real credentials, or private Keycloak material.
