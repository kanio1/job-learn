---
name: payment-quality-lab-orchestrator
description: Coordinate the full Payment Quality Engineering Lab so agent-built implementation, tester-focused learning, Business Analysis discovery, Spec Kit, Spring Modulith modular monolith design, Obsidian, risk thinking and parallel-ready quality engineering remain aligned.
license: MIT
metadata:
  category: project-orchestration
  author: project-custom
  version: "3.1.0"
---

# Payment Quality Engineering Lab Orchestrator

## Use when
- starting or reviewing a project phase,
- coordinating multiple specialist skills,
- turning a broad learning/product goal into an execution path,
- deciding what becomes Business Analysis, Spec Kit input, agent implementation or tester learning.

## Project posture
- **Business Analysis clarifies substantial product capabilities before formal specification.**
- **Agent + Spec Kit build the application.**
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
Before a substantial new feature goes to `/speckit.specify`, route through:
- `business-analysis-and-product-discovery-for-payment-lab`

Expected output:
- BA Discovery Pack,
- business goal,
- actors,
- workflow,
- business rules,
- data needs,
- candidate acceptance criteria,
- ambiguities/open questions,
- feature sequencing recommendation,
- Spec Kit input summary.

This gate may be skipped only for:
- tiny non-product technical work,
- documentation-only adjustments,
- obvious refactors with no product ambiguity.

## Standard task labels
- `[AGENT-IMPLEMENT]`
- `[AGENT-EXPLAIN]`
- `[TESTER-ANALYZE]`
- `[TESTER-DESIGN]`
- `[TESTER-AUTOMATE]`
- `[AGENT-REVIEW]`
- `[DISCUSS]`

## When Not to Use
Do not use this for a narrow local question with an obvious specialist skill.
