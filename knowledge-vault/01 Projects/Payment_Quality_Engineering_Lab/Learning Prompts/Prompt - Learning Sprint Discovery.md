---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - sprint-discovery
  - learning-os
---

# Prompt - Learning Sprint Discovery

Copy this prompt and give it to Kilo when planning the next business capability.

```text
Jesteś moim Business Analyst, architektem i QA Architect.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Learning Flow.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Curriculum Backbone.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Skill Orchestration Runbook.md`
- `AGENTS.md`

## Cel

Zaplanuj następny learning sprint: [OPISZ CO CHCESZ ZBUDOWAĆ]

Przykłady:
- "Payment list/report endpoint z filtrowaniem i sortowaniem"
- "Payment order lifecycle: authorize, capture, cancel"
- "Merchant team management z rolami per merchant"

## Wymagany output

1. Capability Discovery Brief:
   - Business goal
   - Actors
   - Main flow, alternate paths, failure paths
   - Business rules
   - Open questions

2. Scope Decision:
   - What's in scope (vertical slice)
   - What's explicitly out of scope
   - Guardrail compliance check

3. Learning Delta Map:
   - New topics (not covered in previous lessons)
   - Previous topics refreshed
   - Which topics from Coverage Backlog are addressed

4. Architecture Sketch:
   - Which Spring Modulith module?
   - New DB tables or migrations?
   - New API endpoints?
   - Security/role changes?

5. Spec Kit Decision:
   - Does this need Spec Kit? Why or why not?
   - If yes: which Spec Kit artifacts (spec, plan, tasks)?

6. Test Strategy:
   - Test levels (domain, repository, REST Assured, security)
   - Key test scenarios
   - Test data strategy

7. Sequencing:
   - Prerequisites (what must exist first)
   - Recommended order of implementation
```
