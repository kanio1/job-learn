---
name: payment-quality-lab-orchestrator
description: Use when coordinating the full Payment Quality Engineering Lab so that implementation, tester-focused learning, business analysis, spec work, Spring Modulith design, Obsidian, risk thinking, and parallel-ready quality engineering remain aligned.
---

# Payment Quality Engineering Lab Orchestrator

## Use when
- starting or reviewing a project phase,
- coordinating multiple specialist skills,
- turning a broad learning/product goal into an execution path,
- deciding what becomes Business Analysis, Kiro Spec input, implementation or tester learning.

## Project posture
- **Business Analysis clarifies substantial product capabilities before formal specification.**
- **Implementation is driven by Kiro Specs**: requirements.md → design.md → tasks.md.
- **User learns through quality engineering**:
  - requirements analysis,
  - risk analysis,
  - test design,
  - test data design,
  - REST/API testing,
  - Spring integration and module tests,
  - Playwright,
  - parallel-ready framework strategy,
  - interview-quality explanation.

## Product Discovery Gate
Before a substantial new feature goes to a Kiro Spec, route through:
- `business-analysis-and-product-discovery-for-payment-lab`

Expected output:
- BA Discovery Pack,
- business goal, actors, workflow, business rules, data needs,
- candidate acceptance criteria,
- ambiguities/open questions,
- feature sequencing recommendation,
- Kiro Spec input summary.

This gate may be skipped only for:
- tiny non-product technical work,
- documentation-only adjustments,
- obvious refactors with no product ambiguity.

## Kiro Spec Mode
After BA Discovery, use **Kiro Spec Mode** (Requirements-First or Design-First workflow) to produce:
- `requirements.md` — user stories + EARS acceptance criteria,
- `design.md` — architecture, components, sequence diagrams, correctness properties,
- `tasks.md` — small, dependency-ordered implementation tasks.

Produce requirements.md, design.md, and tasks.md before implementation when the request is architectural or product-oriented. Ask for approval before large implementation steps.

## Standard task labels
- `[IMPLEMENT]`
- `[EXPLAIN]`
- `[TESTER-ANALYZE]`
- `[TESTER-DESIGN]`
- `[TESTER-AUTOMATE]`
- `[REVIEW]`
- `[DISCUSS]`

## When Not to Use
Do not use this for a narrow local question with an obvious specialist skill.
