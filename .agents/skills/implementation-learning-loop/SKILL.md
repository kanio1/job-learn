---
name: implementation-learning-loop
description: Use to turn the current repository implementation into a small learning-by-implementation loop; do not use it to implement code directly or produce generic tutorials.
---

# Implementation Learning Loop

Use this skill when the user wants to learn through one real repository change.

Do not edit files. Prepare the main Codex session to implement.

## Workflow

1. Inspect the current branch, latest diff, relevant spec, and nearby code/tests.
2. Identify one smallest useful implementation step that fits the current scope.
3. Explain why this step matters for Java/Spring/SDET learning.
4. List affected files and the owning module or frontend area.
5. List thinking questions the learner should answer before implementation.
6. List edge cases and test oracles.
7. Prepare a precise prompt for the main Codex implementation session.
8. After implementation, explain the diff in Java/SDET learning style: behavior, design choice, test implication, interview wording.

## Guardrails

- Main session implements via `implement` / `tdd`; this skill plans and explains. Do not edit files.
- Prefer current specs under `specs/` and current code over old Phase 0/Phase 1 docs.
- Keep each step small enough to review in one sitting.
- Do not propose PSP, Kafka, settlement, reconciliation, KYC, top-level `POST /payments`, or fake dashboard work.
- Do not duplicate broad lesson content from the knowledge vault; point to it only when useful.
