---
name: business-analysis-and-product-discovery-for-payment-lab
description: Use when proposing, sequencing or refining realistic product capabilities for the Payment Quality Engineering Lab before a written spec, with business goals, actors, workflows, domain rules, data needs, acceptance criteria, ambiguities, risks and tester-facing analysis.
license: MIT
metadata:
  category: business-analysis
  author: project-custom
  version: "1.0.0"
---

# Business Analysis and Product Discovery for Payment Quality Engineering Lab

## Use when
- choosing the next realistic product capability,
- turning a vague idea into a business feature,
- deciding whether a feature belongs now or later,
- preparing high-quality input for `to-spec`,
- exposing business ambiguity before implementation.

## Role in the project
This skill acts as a lightweight **Business Analyst + Product Discovery Lead**.

It protects the project from becoming a random sequence of technical exercises and ensures each substantial feature has:
- a real business purpose,
- defined actors,
- a sensible workflow,
- clear domain rules,
- testable acceptance criteria,
- visible ambiguities,
- a strong tester lens.

## Core discovery questions
1. What real problem does this capability solve?
2. Who experiences that problem?
3. Why is this the right next product step?
4. What workflow, state, rule or decision does it introduce?
5. What would a tester want clarified before this becomes a formal spec?

## Required BA Discovery Pack
Before a substantial capability goes to `to-spec`, produce:

### 1. Capability Proposal
- working name,
- why now,
- roadmap fit.

### 2. Business Goal
- business problem,
- desired outcome,
- consequence of not solving it.

### 3. Actors and Stakeholders
- primary actor,
- secondary actors,
- internal vs external stakeholders,
- actor goals.

### 4. Business Workflow
- trigger,
- main success path,
- alternate paths,
- failure paths,
- key state changes,
- external interactions.

Recommend BPMN, sequence or state modeling when the flow is non-trivial.

### 5. Business Rules and Decisions
Capture:
- explicit rules,
- thresholds,
- role/permission constraints,
- timing rules,
- idempotency/retry concerns if relevant,
- decisions that may benefit from a decision table.

### 6. Domain Vocabulary
Define new terms, nouns, statuses and concepts that must stay consistent across specs, code and tests.

### 7. Data Needs
Identify:
- required inputs,
- optional inputs,
- outputs,
- identifiers,
- audit/status data,
- test-data categories that will matter later.

### 8. Candidate Acceptance Criteria
Provide observable, testable, business-oriented acceptance criteria that can seed `to-spec`.

### 9. Ambiguities and Open Questions
List unresolved decisions, assumptions and questions that may materially affect test strategy.

### 10. Initial Tester Lens
Ask:
- What are the highest product risks?
- Which states, decisions and boundaries are already visible?
- What ownership or authorization questions appear?
- What would become hard to test if left vague?
- What concurrency, retry or eventual-consistency risks may arise later?

### 11. Feature Sequencing Recommendation
State whether the capability should be:
- next,
- deferred,
- split,
- merged,
- rejected as premature.

### 12. Spec Input Summary
End with:
- suggested feature title,
- one-paragraph feature intent,
- recommended scope,
- recommended non-goals,
- must-preserve acceptance criteria,
- open questions that should survive into clarification.

## Relationship to other skills
Coordinate with:
- `payment-quality-lab-orchestrator`
- `to-spec`
- `test-analysis-design-and-data`
- `rapid-software-testing-risk-thinking`
- `bpmn-uml-dmn-for-testers`
- `obsidian-learning-os`

## When Not to Use
Do not use this skill for:
- tiny technical refactors,
- implementation tasks after the feature is already specified,
- syntax-only testing questions,
- product-free architecture discussions.

See:
- `references/ba-discovery-pack-template.md`
- `references/payment-capability-sequencing-guide.md`
