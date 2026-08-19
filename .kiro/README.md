# Kiro Configuration — Payment Quality Engineering Lab

This directory contains Kiro IDE configuration for the Payment Quality Engineering Lab project.

---

## Directory Structure

```
.kiro/
├── steering/        # Persistent project context (historical)
├── specs/           # Historical Feature Specs (prior art)
└── README.md        # This file
```

---

## Skills

Moved to `.agents/skills/` so Grok Build, Codex CLI, and Cursor load one tree. Cursor uses `.cursor/skills/` symlinks into that directory. Index: `.agents/skills/README.md`.

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

## Skill homes (do not duplicate)

| Location | Used by | Owns |
|---|---|---|
| `.agents/skills/` | Grok Build, Codex CLI, Cursor | All project skills |
| `.cursor/skills/` | Cursor | Symlinks into `.agents/skills/` |

Do **not** recreate `.kilo/` or `.kilocode/`.

New work uses `.agents/skills/` (`to-spec`, `to-tickets`, `implement`) rather than Spec Kit or Kiro Spec Mode. Historical `.kiro/specs/` is prior art.

---

## What Not to Change

- **Do not recreate `.kilo/` or `.kilocode/`.** Skills live in `.agents/skills/`.
- **Do not modify application source code** (`apps/backend/`, `apps/frontend/`) for tooling setup tasks.
- **Do not commit secrets**, tokens, real credentials, or private Keycloak material.
