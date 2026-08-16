---
name: obsidian-learning-os
description: Use when maintaining the Obsidian Learning OS vault, including MOCs, trackers, project dashboards, learning status, Effective Java progress, testing stories, and practitioner insights.
---

# Obsidian Learning OS

## Use when
- creating/updating project notes,
- keeping MOCs and dashboards current,
- tracking Business Analysis, Effective Java, RST, practitioner insights, parallel testing and Modulith learning,
- updating the Learning OS dashboard.

## Vault path
`/home/suso/job-learn/knowledge-vault/`

## Learning OS Core Files

| File | Purpose |
|---|---|
| `START HERE - Learning Dashboard.md` | Single entry point. Open first every session. |
| `00 Learning OS/Current Learning Flow.md` | Process: Path A (extension) vs Path B (new capability), spec decisions |
| `00 Learning OS/Current Lesson.md` | Dashboard: NOW, COVERED, INTRODUCED, NEEDS PRACTICE, DEFERRED |
| `00 Learning OS/Current Sprint.md` | Sprint status, remaining tasks, next options |
| `00 Learning OS/Curriculum Backbone.md` | Technology ↔ lesson cross-reference |

## Learning OS Conventions

### Statuses

| Status | Meaning | Action |
|---|---|---|
| NOW | Active learning target | Study and practice |
| COVERED | Fully learned with evidence | Can explain in interview |
| INTRODUCED | Seen but not mastered | Needs more practice |
| NEEDS PRACTICE | Concrete exercises recommended | Do the exercises |
| DEFERRED | Explicitly postponed | Read but don't touch |
| DO NOT TOUCH | Phase 0 guardrail | NEVER implement |

### Evidence Rules

Every lesson or sprint must leave:
1. Updated Lesson Evidence Tracker
2. Updated Learning OS Status (status transitions)
3. Interview answer (EN) if new capability
4. Competency matrix update if new skill practiced

### Kiro integration
- When planning a new capability, use `to-spec` / `to-tickets` under `.codex/`.
- Path B (new business capability) flows through: BA discovery → `to-spec` → `to-tickets` → `implement` → lesson note → evidence.

## When Not to Use
Do not use this for coding tasks with no documentation impact or uncurated transcript dumping.
