---
name: business-analysis-and-product-discovery-for-payment-lab
description: Use when proposing, sequencing, or refining realistic product capabilities for the Payment Quality Engineering Lab before requirements specification, covering business goals, actors, workflows, domain rules, data needs, acceptance criteria, ambiguities, risks, and tester-facing analysis.
---

# Business Analysis and Product Discovery for Payment Quality Engineering Lab

Lightweight **Business Analyst + Product Discovery Lead** for this lab. Protects the
project from becoming a random sequence of technical exercises: every substantial
feature gets a real purpose, actors, workflow, domain rules, testable acceptance
criteria, visible ambiguities, and a tester lens.

## Use when

- choosing the next realistic product capability,
- turning a vague idea into a business feature,
- deciding whether a feature belongs now or later,
- preparing high-quality input for a requirements spec,
- exposing business ambiguity before implementation.

## Core discovery questions

1. What real problem does this capability solve?
2. Who experiences that problem?
3. Why is this the right next product step?
4. What workflow, state, rule, or decision does it introduce?
5. What would a tester want clarified before this becomes a formal spec?

## BA Discovery Pack

Produce one pack per substantial capability, following
[`references/ba-discovery-pack-template.md`](references/ba-discovery-pack-template.md):
Capability Proposal → Business Goal → Actors & Stakeholders → Business Workflow (BPMN /
sequence / state model when non-trivial: `bpmn-uml-dmn-for-testers`) → Business Rules &
Decisions (decision tables where rules branch) → Domain Vocabulary → Data Needs →
Candidate Acceptance Criteria → Ambiguities & Open Questions → Initial Tester Lens →
Feature Sequencing Recommendation (`references/payment-capability-sequencing-guide.md`)
→ Spec Input Summary.

## Initial tester lens

- Highest product risks? Which states, decisions, boundaries are already visible?
- Ownership/authorization questions? Tenant isolation implications?
- Concurrency, retry, or eventual-consistency risks that may arise later?
- What becomes hard to test if left vague?

## Spec handoff

When the pack is ready: `to-spec` then `to-tickets` under `.codex/`
(`docs/agents/issue-tracker.md`). Coordinate with `test-analysis-design-and-data`,
`rapid-software-testing-risk-thinking`, `obsidian-learning-os`.

## When Not to Use

Do not use this skill for tiny technical refactors, implementation after the feature is
specified, syntax-only testing questions, or product-free architecture discussions.
