<!--
Sync Impact Report
Version change: template -> 1.0.0
Modified principles:
- template principle 1 -> I. Tester-Led Product Learning
- template principle 2 -> II. Spec-Driven Delivery
- template principle 3 -> III. Modular Monolith Boundaries
- template principle 4 -> IV. Parallel-Ready Quality Engineering
- template principle 5 -> V. Security, Data Integrity, and Observability by Design
Added sections:
- Technical Baseline and Constraints
- Development Workflow and Knowledge Stewardship
Removed sections: none
Templates requiring updates:
- updated: .specify/templates/plan-template.md
- updated: .specify/templates/spec-template.md
- updated: .specify/templates/tasks-template.md
- not applicable: .specify/templates/commands/*.md (directory absent)
- reviewed: .kilocode/rules/specify-rules.md
Follow-up TODOs: none
-->

# Payment Quality Engineering Lab Constitution

## Core Principles

### I. Tester-Led Product Learning
Every feature MUST produce product behavior that is useful for learning modern quality
engineering, not only code that compiles. Specifications, plans, and tasks MUST expose
tester-facing risks, test conditions, useful test data, and learning prompts where the
behavior affects APIs, backend modules, frontend flows, security, persistence, or
asynchronous processing. Agent implementation work MUST be separable from tester analysis,
design, automation, and review work.

### II. Spec-Driven Delivery
Product changes MUST begin with a Spec Kit artifact that states business purpose, actors,
scope, functional requirements, non-functional requirements, acceptance criteria,
assumptions, and measurable success criteria. Ambiguous behavior MUST be marked as needing
clarification or captured as an explicit assumption before implementation. Plans and tasks
MUST preserve traceability from user stories to requirements, risks, tests, and code paths.

### III. Modular Monolith Boundaries
Backend-relevant features MUST identify Spring Modulith module ownership, public module API
impact, internal implementation boundaries, dependency impact, event impact, and module test
impact before implementation. Cross-module dependencies MUST be justified and kept explicit.
Domain behavior MUST remain inside the owning module unless an application event or module
API is intentionally chosen and tested.

### IV. Parallel-Ready Quality Engineering
Automated tests, test data, and local workflows MUST be designed for deterministic parallel
execution unless a documented constraint prevents it. API, backend, frontend, security, and
database tests MUST avoid shared mutable data collisions through namespacing, isolated
fixtures, transactions, containers, or equivalent controls. Tasks marked parallel MUST touch
different files or independent data scopes.

### V. Security, Data Integrity, and Observability by Design
Payment-system behavior MUST treat authentication, authorization, validation, persistence,
auditing, and failure visibility as first-class requirements. REST APIs MUST define access
control expectations, validation failures, and error contracts. Data changes MUST identify
constraints, transactional boundaries, concurrency risks, and audit needs. Important state
changes MUST be observable through logs, metrics, events, or generated documentation.

## Technical Baseline and Constraints

The lab targets a payment-quality system built as a modular monolith with Java 25,
Spring Boot 4, Spring Framework 7, Spring Modulith 2.0.6, Maven 3.9.11, PostgreSQL 18,
Keycloak 26.6.1, Nuxt 4, TypeScript 6, Pinia, Zod, JUnit 6, AssertJ, REST Assured,
Testcontainers, WireMock, and Playwright 1.60 unless a feature plan documents a different
approved baseline.

Backend plans MUST include module ownership and architecture-verification impact. Frontend
plans MUST include user-visible behavior, validation rules, state ownership, and testability
seams. Database plans MUST include schema constraints, transaction boundaries, indexing or
performance assumptions, and test data isolation. Security-sensitive plans MUST include role,
scope, token, and denial-path coverage.

## Development Workflow and Knowledge Stewardship

Each feature MUST separate tasks using the lab labels: AGENT-IMPLEMENT, AGENT-EXPLAIN,
TESTER-ANALYZE, TESTER-DESIGN, TESTER-AUTOMATE, AGENT-REVIEW, and DISCUSS. Tasks MUST be
small enough to validate independently and MUST include exact file paths once implementation
planning has resolved them.

Documentation, specs, generated architecture notes, and Obsidian-oriented learning material
MUST remain consistent with implemented behavior. When official versions, APIs, or project
assumptions change, the related skills, specs, and references MUST be updated or explicitly
flagged as follow-up work.

## Governance

This constitution supersedes conflicting local workflow habits, generated templates, and
feature-level preferences. Every `/speckit.plan`, `/speckit.specify`, and `/speckit.tasks`
output MUST pass the constitution gates or record a justified exception in the relevant
artifact. Reviews MUST check module ownership, requirement traceability, parallel-test
safety, security/data risks, and tester-learning value.

Amendments MUST update this file, include a Sync Impact Report, and propagate changes to
dependent templates in `.specify/templates/` and relevant runtime guidance. Versioning uses
semantic versioning: MAJOR for incompatible governance or principle redefinition, MINOR for
new or materially expanded principles or sections, and PATCH for clarifications that do not
change obligations.

**Version**: 1.0.0 | **Ratified**: 2026-05-18 | **Last Amended**: 2026-05-18
