---
name: qa-architecture-sprint-team
description: Use when starting a new product sprint that must combine Business Analysis, architecture, QA architecture, learning-path design, Spec Kit readiness, implementation slicing, risk-based testing, backend/frontend/data/security decisions and automation strategy.
license: MIT
metadata:
  category: project-orchestration
  author: project-custom
  version: "1.0.0"
---

# QA Architecture Sprint Team

## Use When

- creating a new business sprint with new product capability,
- selecting 1-2 realistic PayU-like flows for implementation,
- turning broad product ambition into scoped backend/frontend/data/security work,
- designing the QA Architect learning path for a sprint,
- coordinating BA, architecture, development and testing outputs before Spec Kit,
- deciding what new topics should be learned now and what should not be repeated.

## Mission

Act as a virtual cross-functional team:

- Business Analyst / Product Discovery Lead,
- Domain Architect,
- Backend Architect,
- Data Architect,
- Security Architect,
- Frontend Architect,
- QA Architect,
- Test Automation Lead,
- Learning Architect for a Senior QA Automation/SDET path.

The skill converts a desired product direction into a sprint-shaped learning and implementation package: business flow, architecture slice, data model, API contract, security matrix, UI journey, automated tests and learner outcomes.

This skill is adapted from common high-performing product-team practices: discovery before delivery, DDD/event-storming vocabulary, API-first design, C4/ADR-style architecture decisions, threat modeling, risk-based testing, test pyramid thinking, BDD-style acceptance criteria, parallel-safe test data and continuous learning loops. It is project-specific content, not a copy of a public skill.

## When Not To Use

- Do not use for small syntax-only lessons.
- Do not use for tiny refactors with no product or architecture impact.
- Do not use when an existing Spec Kit task already defines the scope clearly.
- Do not use to bypass project guardrails or implement unapproved payment flows.
- Do not use for generic brainstorming without producing a concrete sprint package.

## Operating Model

### 1. Product Discovery Gate

Define:

- business problem,
- target actors,
- value proposition,
- current system fit,
- candidate flows,
- non-goals,
- assumptions and open questions.

Output: `Capability Discovery Brief`.

### 2. Sprint Scope Selection

Choose one main flow and at most one supporting flow.

Prefer flows that introduce new learning value without exploding scope.

Reject flows that only repeat previous lesson topics.

Output: `Sprint Scope Decision` with selected/deferred/rejected capabilities.

### 3. Domain And Workflow Modeling

Create:

- domain vocabulary,
- aggregate/entity/value-object candidates,
- state machine candidates,
- business rules,
- decision table candidates,
- BPMN/sequence/state diagrams when useful.

Output: `Domain and Workflow Pack`.

### 4. Architecture Slice

Define:

- Spring Modulith module ownership,
- REST API boundary,
- transaction boundaries,
- persistence tables and constraints,
- security ownership boundaries,
- frontend route/page/component boundaries,
- observability hooks such as `X-Correlation-ID`,
- HTTP semantics such as `Location`, `ETag`, `If-Match`, `Idempotency-Key`.

Output: `Architecture Decision Pack` with short ADR-style decisions.

### 5. QA Architecture And Test Strategy

Design test strategy before coding:

- product risks,
- test conditions,
- test levels,
- automation ownership,
- REST Assured contract tests,
- AssertJ/domain/repository assertions,
- security matrix,
- Playwright role-aware journeys,
- parallel-safe data strategy,
- exploratory charters,
- CI verification commands.

Output: `QA Architecture Pack`.

### 6. Learning Path Delta

Create a sprint-specific learning map that focuses only on new material.

Do not repeat prior lessons except as one-line prerequisites.

For each new topic, define:

- why it appears now,
- where it appears in code,
- which test proves it,
- what the learner should be able to explain in an interview.

Output: `Learning Delta Map`.

### 7. Spec Kit Readiness

Produce input for Spec Kit:

- feature title,
- user stories,
- functional requirements,
- non-goals,
- data model,
- API contract,
- security matrix,
- UI scope,
- test strategy,
- Definition of Done,
- open questions.

Output: `Spec Kit Input Pack`.

## Quality Gates

Before implementation, check:

- Does this sprint add meaningful new product behavior?
- Does it avoid repeating Lessons 1-5?
- Is the vertical slice small enough to finish?
- Are business rules testable?
- Are roles, permissions and ownership explicit?
- Are database constraints visible and testable?
- Are HTTP semantics intentional, not decorative?
- Is the UI a real consumer of the API contract?
- Are tests layered and parallel-safe?
- Is the learning path clear for a Senior QA/SDET learner?

## Required Final Output

When this skill is used, return:

1. Capability Discovery Brief.
2. Sprint Scope Decision.
3. Domain and Workflow Pack.
4. Architecture Decision Pack.
5. QA Architecture Pack.
6. Learning Delta Map.
7. Spec Kit Input Pack.
8. Implementation/Test Task Breakdown.
9. Verification Commands.
10. Deferred Scope and Risks.
