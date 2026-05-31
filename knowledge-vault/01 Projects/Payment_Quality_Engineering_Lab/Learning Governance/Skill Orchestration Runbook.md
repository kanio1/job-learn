---
type: runbook
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-27
tags:
  - skill-orchestration
  - qa-architecture
  - business-analysis
  - speckit
  - sdet
---

# Skill Orchestration Runbook

Cel: upewnić się, że przy nowych business flows skills uruchamiają się w dobrej kolejności i żaden obszar nie wypada: business analysis, architektura, QA architecture, test design, Spec Kit (tylko gdy potrzebny), implementacja, UI i evidence.

> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Learning Flow]] | [[Home]]

## Simplified Flow (From Lesson 6)

There are only two paths now. Pick based on scope size:

### Path A: Lesson Extension (NO Spec Kit)

```
Learning objective (small)
  → Study existing code/tests
  → Practice (write test, run SQL, etc.)
  → Update lesson note with new section
  → Update [[Lesson Evidence Tracker]]
  → Update [[Learning OS Status]] (needs practice → covered)
```

Use this for: adding a test, deepening a topic, SQL exercises, assertion strategy practice, interview prep.

### Path B: New Business Capability (WITH Spec Kit)

```
Learning objective (new behavior)
  → BA Discovery (actors, workflow, rules)
  → Architecture decision (module, data, security)
  → QA Architecture & learning delta
  → [[Spec Kit Decision Guide]] — check if Spec Kit needed
  → Spec Kit: spec → plan → tasks (if scope is large enough)
  → Implementation (code + tests)
  → Lesson note
  → Evidence update
  → Interview explanation
```

Use this for: new module, new REST resource, new DB schema, new security model.

## Core Skills (Always Use)

| Skill | When | Purpose |
|---|---|---|
| `payment-quality-lab-orchestrator` | Start of any new work | Decide: extension or new capability |
| `java-rest-api-testing-effective-java-mentor` | Any code/test work | Java 25 + REST Assured design |
| `junit6-assertj-restassured-testcraft` | Any test work | Test quality and assertion patterns |
| `obsidian-learning-os` | End of any work | Update vault notes, trackers, MOCs |

## Extended Skills (Use For New Capabilities Only)

| Skill | When |
|---|---|
| `qa-architecture-sprint-team` | New business capability (Path B) |
| `business-analysis-and-product-discovery-for-payment-lab` | BA Discovery before Spec Kit |
| `spec-kit-feature-workflow` | Spec Kit handoff (only if Spec Kit needed) |
| `spring-boot4-spring7-backend-architect` | Backend design |
| `spring-modulith-2-0-6-modular-monolith-testing` | Module boundary changes |
| `postgres18-data-architecture-and-risk` | New DB schema |
| `rest-api-security-oauth-testing` | New roles or ownership rules |
| `test-analysis-design-and-data` | Formal test design |
| `parallel-test-architecture-and-data-isolation` | Parallel test data strategy |
| `nuxt-dashboard-zod-pinia-frontend-engineering` | Frontend changes |
| `typescript6-playwright-engineering` | E2E test changes |

## Do NOT Use For Small Extensions

| Skill | Skip When |
|---|---|
| All extended skills | Path A (lesson extension, no new capability) |
| `bpmn-uml-dmn-for-testers` | Unless modeling a complex workflow |
| `rapid-software-testing-risk-thinking` | Unless new product risks emerge |
| `project-skill-governance-and-quality-review` | Unless skill overlap becomes a problem |

## Mandatory Output Per New Capability (Path B)

- Learning Delta Map
- Business Workflow
- Security Matrix
- Test Strategy
- Lesson Note
- Evidence Update
- Interview Answer (EN)

## Mandatory Output Per Lesson Extension (Path A)

- Updated lesson note (new section or exercise)
- Evidence update in tracker
- Competency update if new skill practiced

## Gates (Simplified)

### Gate 1: Scope Check
- [ ] Is this a new business capability or just deepening an existing topic?
- [ ] Checked [[Learning OS Status]] for what's deferred?

### Gate 2: No Repetition
- [ ] Not repeating Lessons 1-5 fundamentals
- [ ] Learning Delta Map written

### Gate 3: Guardrail Check
- [ ] No Phase 0 guardrail violations
- [ ] Checked [[Learning OS Status#Do Not Touch]]

### Gate 4: Evidence
- [ ] [[Lesson Evidence Tracker]] updated
- [ ] [[Learning OS Status]] updated (status changes)
- [ ] Interview answer exists (if new capability)

## Skill Runtime Note

`qa-architecture-sprint-team` został utworzony jako project-local skill w `.kilo/skills/qa-architecture-sprint-team/SKILL.md`.

Jeśli runtime jeszcze go nie widzi, odśwież sesję Kilo. Do tego czasu prompt ma traktować jego sekcje jako wymagany output ręczny.
