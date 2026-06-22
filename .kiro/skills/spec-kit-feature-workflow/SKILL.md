---
name: spec-kit-feature-workflow
description: Use when converting a capability into a structured specification with requirements, design, tasks, Definition of Done, and a tester learning flow — grounded in prior Business Analysis discovery.
---

# Spec Feature Workflow

## Use when
- creating a new feature spec,
- planning a new phase,
- refining requirements before implementation,
- adding a structured quality/testing workstream.

## Kiro Spec Mode
In Kiro, use **Kiro Spec Mode** to produce the spec artifacts:
1. **requirements.md** — user stories + EARS acceptance criteria.
2. **design.md** — architecture, components, sequence diagrams, data models, correctness properties.
3. **tasks.md** — small, dependency-ordered implementation tasks.

Workflow steps:
- Produce requirements.md, design.md, and tasks.md **before implementation** when the request is architectural or product-oriented.
- Ask for approval before large implementation steps.
- Prefer small, reviewable changes.
- Use workspace steering files (`.kiro/steering/`) as persistent project context.

## Preferred input for product-significant features
For any substantial real product capability, first use:
- `business-analysis-and-product-discovery-for-payment-lab`

The BA Discovery Pack should provide:
- business goal, actors, workflow, rules/decisions, data needs,
- candidate acceptance criteria,
- open questions,
- sequencing recommendation,
- Kiro Spec input summary.

If this input is missing and the feature is product-significant, recommend creating it before starting the requirements phase.

## Business Analysis carry-through
When a BA Discovery Pack exists:
- preserve its business goal,
- preserve unresolved ambiguities in the requirements phase,
- do not flatten business rules into vague implementation language,
- retain candidate acceptance criteria unless deliberately refined,
- keep the product rationale visible in requirements.md.

## When Not to Use
Do not use this for isolated debugging, tiny docs edits, or changes with no architectural impact.
