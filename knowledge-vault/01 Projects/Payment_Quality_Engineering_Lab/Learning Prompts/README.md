---
type: moc
status: ready
area: Learning Prompts
date: 2026-05-31
tags:
  - prompts
  - learning-sprint
  - sdet
---

# Learning Prompts - MOC

Ten katalog przechowuje gotowe prompty do uruchamiania kolejnych lekcji i learning sprintów.

> **Navigation:** [[START HERE - Learning Dashboard]] | [[Prompt Templates - Learning OS]]

## Learning OS Prompts (Lesson 6 Onward)

Use these for daily learning flow:

| Prompt | Use When |
|---|---|
| [[Prompt Templates - Learning OS]] | Template reference — understand the structure |
| [[Prompt - Concept Lesson]] | Learning a small foundation topic (no Spec Kit) |
| [[Prompt - Code Reading Lesson]] | Understanding existing code (no refactoring) |
| [[Prompt - Payment Order Case Study]] | Deep-diving Payment Order behavior |
| [[Prompt - Generate Next Lesson]] | Starting a new lesson or sprint |
| [[Prompt - Mark Lesson Progress]] | After completing exercises or study |
| [[Prompt - Verify My Understanding]] | Checking if you can explain concepts |
| [[Prompt - Learning Sprint Discovery]] | Planning the next business capability |
| [[Prompt - Post Sprint Evidence Update]] | After completing a sprint |

## Legacy Lesson Prompts (Lessons 1-6)

1. `Prompt - Strategy - Three Advanced Learning Paths.md` - completed strategy/discovery prompt; output is `../Strategy/Three Advanced Learning Paths for API Testing and Payment Lab.md`
2. `Prompt - Lesson 01 - REST API Request Response Flow.md` - completed/read
3. `Prompt - Lesson 02 - REST Assured What REST Assured Is.md` - REST Assured entry lesson prompt
4. `Prompt - Lesson 04 - REST Assured Path Params Query Params and Headers.md` - request input channels prompt
5. `Prompt - Lesson 05 - REST Assured Request Body JSON Map DTO Serialization.md` - request body and serialization prompt
6. `Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md` - accelerated PayU-like sprint prompt driven by QA architecture, business flows, backend, UI, SQL, security and tests

## Current Phase 2 Lesson Prompts

| Prompt | Use When |
|---|---|
| [[Prompt - Lesson 07 - Payment Order List Filter SpecKit]] | Implementing or reviewing list/filter/search backend slice |
| [[Prompt - Lesson 08 - Payment Aggregation Summary]] | Implementing or reviewing read-only summary aggregation |
| [[Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]] | Implementing or reviewing Nuxt/Zod/Pinia frontend consumer slice |
| [[Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | Implementing backend REST/HTTP hardening and BOLA/BFLA matrix tests |

## Aktualny kontekst strategii

Strategia PayU-like learning clone jest juz zapisana i ograniczona do trzech sciezek:

- Payment Order REST API, idempotency and lifecycle.
- Security, ownership and tenant isolation matrix.
- Reliability, data integrity, audit trail, webhooks and event evolution.

Payment Order create/read, list/filter, summary and frontend consumer are covered through Lessons 06-09. The active next prompt is `Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md`.

## Zasada

Każdy prompt ma wymuszać:

- sprawdzenie roadmapy,
- brak duplikacji w vault,
- powiązanie z realnym kodem,
- ćwiczenia,
- test design,
- perspektywę SDET,
- następny krok w roadmapie.

## Governance

Od Lesson 6 każdy prompt powinien aktualizować albo wskazywać:

- `../Learning Governance/Senior SDET Competency Coverage Matrix.md`
- `../Learning Governance/Lesson Evidence Tracker.md`
- `../Learning Governance/Skill Orchestration Runbook.md`

Te pliki pilnują, żeby kompetencje senior SDET, evidence z lekcji i wymagane skills nie umknęły między sprintami.
