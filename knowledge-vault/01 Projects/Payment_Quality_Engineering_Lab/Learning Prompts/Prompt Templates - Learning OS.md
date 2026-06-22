---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - templates
  - learning-os
---

# Prompt Templates - Learning OS

This file documents the structure of all Learning OS prompts.

## Template Structure

Every Learning OS prompt follows this pattern:

```text
Jesteś moim agentem kodowania i mentorem Senior QA Automation/SDET.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst
[Jaki jest aktualny stan nauki - lekcja, sprint, status]

## Cel
[Co ten prompt ma osiągnąć]

## Zasady
[Co wolno, czego nie wolno, jakie są guardrails]

## Wymagany output
[Co ma powstać po wykonaniu promptu]
```

## Available Prompts

| Prompt | Use When |
|---|---|
| [[Prompt - Concept Lesson]] | Learning a small foundation topic (HTTP header, RA method, Java syntax, SQL clause) |
| [[Prompt - Code Reading Lesson]] | Understanding existing code without writing new functionality |
| [[Prompt - Payment Order Case Study]] | Deep-diving a specific behavior in the Payment Order vertical slice |
| [[Prompt - Generate Next Lesson]] | Starting a new lesson or learning sprint |
| [[Prompt - Mark Lesson Progress]] | After completing exercises or study |
| [[Prompt - Verify My Understanding]] | Checking if you can explain learned concepts |
| [[Prompt - Learning Sprint Discovery]] | Planning the next business capability |
| [[Prompt - Post Sprint Evidence Update]] | After completing a sprint |

## Key Files To Reference In Prompts

| File | Path |
|---|---|
| Current Lesson | `00 Learning OS/Current Lesson.md` |
| Current Sprint | `00 Learning OS/Current Sprint.md` |
| Evidence Tracker | `Learning Governance/Lesson Evidence Tracker.md` |
| Progress Board | `Learning Governance/Learning Progress Board.md` |
| Coverage Backlog | `Learning Governance/Learning Coverage Backlog.md` |
| Competency Matrix | `Learning Governance/Senior SDET Competency Coverage Matrix.md` |

## Spec Kit Guardrails

| Scope | Use Spec Kit? |
|---|---|
| New lesson or deepening topic | No |
| New API endpoint in existing module | No |
| New module or REST resource | Yes (plan + spec) |
| New security model or cross-module API | Yes (full: spec + plan + tasks + data-model + contracts) |
