---
name: obsidian-learning-os
description: Maintain the Obsidian Learning OS: MOCs, trackers, note types, project dashboards, Learning OS Status dashboard, Tech Connection Map, Spec Kit Decision Guide, Business Analysis discovery notes, Effective Java progress, testing stories, practitioner insights, Spring Modulith notes and parallel-test architecture notes.
license: MIT
metadata:
  category: knowledge-management
  author: project-custom
  version: "4.0.0"
---

# Obsidian Learning OS

## Use when
- creating/updating project notes,
- keeping MOCs and dashboards current,
- tracking Business Analysis, Effective Java, RST, practitioner insights, parallel testing and Modulith learning.
- updating the Learning OS dashboard ([[Learning OS Status]])

## Vault path
`/home/suso/job-learn/knowledge-vault/`

## Learning OS Core Files

Every learner starts from `START HERE - Learning Dashboard.md` in the project folder.

| File | Purpose |
|---|---|
| [[START HERE - Learning Dashboard]] | Single entry point. Open first every session. |
| `00 Learning OS/Current Learning Flow.md` | Process: Path A (extension) vs Path B (new capability), Spec Kit decisions |
| `00 Learning OS/Current Lesson.md` | Dashboard: NOW, COVERED, INTRODUCED, NEEDS PRACTICE, DEFERRED |
| `00 Learning OS/Current Sprint.md` | Sprint status, remaining tasks, next options |
| `00 Learning OS/Curriculum Backbone.md` | Technology ↔ lesson cross-reference |
| `00 Learning OS/How To Use This Vault.md` | Vault usage guide and quick navigation |

Additional governance files:
- `Learning Governance/Learning Progress Board.md` — overall progress dashboard
- `Learning Governance/Learning Coverage Backlog.md` — what's not yet covered
- `Learning Governance/Lesson Evidence Tracker.md` — per-lesson evidence
- `Learning Governance/Senior SDET Competency Coverage Matrix.md` — competency map

Repeatable prompts:
- `Learning Prompts/Prompt Templates - Learning OS.md` — template reference
- `Learning Prompts/Prompt - Generate Next Lesson.md` — new lesson/sprint generation
- `Learning Prompts/Prompt - Mark Lesson Progress.md` — progress updates
- `Learning Prompts/Prompt - Verify My Understanding.md` — understanding checks
- `Learning Prompts/Prompt - Learning Sprint Discovery.md` — sprint planning
- `Learning Prompts/Prompt - Post Sprint Evidence Update.md` — evidence updates

## Learning OS Conventions

### Statuses (used in Current Lesson and Lesson Evidence Tracker)

| Status | Meaning | Action |
|---|---|---|
| NOW | Active learning target | Study and practice |
| COVERED | Fully learned with evidence | Can explain in interview |
| INTRODUCED | Seen but not mastered | Needs more practice |
| NEEDS PRACTICE | Concrete exercises recommended | Do the exercises |
| DEFERRED | Explicitly postponed | Read but don't touch |
| DO NOT TOUCH | Phase 0 guardrail | NEVER implement |

### Lesson Creation (From Lesson 6 onward)

Two paths:
- **Path A: Lesson Extension (small).** No Spec Kit. Just: study → practice → update lesson note → update evidence.
- **Path B: New Business Capability (large).** Spec Kit if scope demands. Full: BA → architecture → Spec Kit → implement → lesson note → evidence → interview.

Use templates:
- Prompt template: `05 Templates/Template - Lesson 7+ Prompt.md`
- Note template: `05 Templates/Template - Lesson 7+ Note.md`

### Evidence Rules

Every lesson or sprint must leave:
1. Updated [[Lesson Evidence Tracker]]
2. Updated [[Learning OS Status]] (status transitions)
3. Interview answer (EN) if new capability
4. Competency matrix update if new skill practiced

### What NOT To Do

- Do NOT duplicate content between lesson notes and reference materials. Link instead.
- Do NOT create new dashboards without updating `START HERE - Learning Dashboard.md`.
- Do NOT add topics listed under "DEFERRED" in `00 Learning OS/Current Lesson.md`.
- Do NOT use Spec Kit for small extensions (check `00 Learning OS/Current Learning Flow.md`).

## Business Analysis knowledge capture
For meaningful new product capabilities, preserve:
- Business Analysis Note,
- Product Discovery Note,
- Domain Rule Note,
- Capability Sequencing Note when roadmap trade-offs matter.

These should capture:
- capability proposal,
- business goal,
- actors,
- workflow,
- rules,
- data needs,
- open questions,
- tester lens,
- handoff to Spec Kit.

## When Not to Use
Do not use this for coding tasks with no documentation impact or uncurated transcript dumping.
