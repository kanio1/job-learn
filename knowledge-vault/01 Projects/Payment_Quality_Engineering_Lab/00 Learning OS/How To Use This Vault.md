---
type: learning-os
status: active
date: 2026-05-28
tags:
  - learning-os
  - vault-guide
---

# How To Use This Vault

## Start Here Every Session

1. Open `[[START HERE - Learning Dashboard]]`
2. Check `[[Current Lesson]]` → what to do NOW
3. Check `[[Current Sprint]]` → sprint status and remaining tasks
4. Follow `[[Current Learning Flow]]` → the process

## Vault Structure

```
START HERE - Learning Dashboard.md     ← Open this first every session

00 Learning OS/                        ← Learning OS core files
  Current Learning Flow.md              ← The process (Paths A/B)
  Current Lesson.md                     ← What to study NOW
  Current Sprint.md                     ← Sprint status and tasks
  Curriculum Backbone.md                ← Technology ↔ lesson map
  How To Use This Vault.md              ← This file

Learning Governance/                    ← Trackers and evidence
  Learning Progress Board.md            ← Overall progress dashboard
  Learning Coverage Backlog.md          ← What's not yet covered
  Lesson Evidence Tracker.md            ← Per-lesson evidence
  Senior SDET Competency Coverage Matrix.md  ← Competency map
  Skill Orchestration Runbook.md        ← Which skills to use when

Learning Prompts/                       ← Reusable Kilo prompts
  Prompt Templates - Learning OS.md     ← Template reference
  Prompt - Generate Next Lesson.md      ← Generate a new lesson
  Prompt - Mark Lesson Progress.md      ← Update progress
  Prompt - Verify My Understanding.md   ← Check understanding
  Prompt - Learning Sprint Discovery.md ← Discover new sprints
  Prompt - Post Sprint Evidence Update.md ← Update evidence

01 Phase 1 - Foundations/              ← Lessons 1-5 (reference)
02 Phase 2 - Payment Orders/           ← Lesson 06+ (active)
  02 Areas/Technical Learning/         ← Reference materials by tech
```

## Do NOT Touch

These topics are explicitly deferred. See [[Current Lesson#DEFERRED]] for the full list.

Key deferrals:
- Payment lifecycle actions (authorize/capture/cancel)
- Kafka, GraphQL, gRPC
- PSP integration
- Complete OAuth/OIDC and business dashboards ← Phase 0 guardrails

## When To Use Spec Kit

See [[Current Learning Flow#Spec Kit Decision]] for the decision table.

Short version:
- Adding a test? **NO** Spec Kit.
- New module / resource / security model? **YES** Spec Kit.
- Everything in between? Check the table.

## Repeatable Prompts

Use these prompt files to interact with Kilo:

| Prompt | Use When |
|---|---|
| [[Prompt - Generate Next Lesson]] | Starting a new lesson or sprint |
| [[Prompt - Mark Lesson Progress]] | After completing exercises, updating status |
| [[Prompt - Verify My Understanding]] | Checking if you can explain what you learned |
| [[Prompt - Learning Sprint Discovery]] | Planning the next business capability |
| [[Prompt - Post Sprint Evidence Update]] | After completing a sprint, updating trackers |

## Key Commands

```bash
cd apps/backend && ./mvnw test                              # all tests
cd apps/backend && ./mvnw -Dtest=PaymentOrderRestAssuredTest test  # RA only
cd apps/frontend && corepack pnpm typecheck                  # frontend check
```

## Quick Navigation

| I want to... | Go to... |
|---|---|
| See what lesson I'm on | [[Current Lesson]] |
| Read the current lesson | [[Lesson 06 - Payment Order Create Read Foundation]] |
| See sprint status | [[Current Sprint]] |
| Know if I should use Spec Kit | [[Current Learning Flow#Spec Kit Decision]] |
| See technology connections | [[Curriculum Backbone]] |
| Track my progress | [[Learning Progress Board]] |
| Find a lesson prompt | [[Learning Prompts MOC|Learning Prompts/README]] |
| See what to defer | [[Current Lesson#DEFERRED]] |
| Find interview answers | [[Lesson 06#Mini Interview Prep]] |
| See competency coverage | [[Senior SDET Competency Coverage Matrix]] |
