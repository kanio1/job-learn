---
name: spec-kit-feature-workflow
description: Convert a capability into a Spec Kit-oriented specification, plan, tasks, Definition of Done and tester learning flow, preferably grounded in a prior Business Analysis Discovery Pack, including module ownership, parallel-testability and risk impact.
license: MIT
metadata:
  category: specification
  author: project-custom
  version: "3.1.0"
---

# Spec Kit Feature Workflow

## Use when
- creating a new feature spec,
- planning a new phase,
- refining requirements before agent implementation,
- adding a structured quality/testing workstream.

## Preferred input for product-significant features
For any substantial real product capability, first use:
- `business-analysis-and-product-discovery-for-payment-lab`

The BA Discovery Pack should provide:
- business goal,
- actors,
- workflow,
- rules/decisions,
- data needs,
- candidate acceptance criteria,
- open questions,
- sequencing recommendation,
- Spec Kit input summary.

If this input is missing and the feature is product-significant, recommend creating it before `/speckit.specify`.

## Business Analysis carry-through
When a BA Discovery Pack exists:
- preserve its business goal,
- preserve unresolved ambiguities for `/speckit.clarify`,
- do not flatten business rules into vague implementation language,
- retain candidate acceptance criteria unless deliberately refined,
- keep the product rationale visible in the formal spec.

## When Not to Use
Do not use this for isolated debugging or tiny docs edits.
