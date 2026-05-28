---
type: moc
status: ready
area: Learning Prompts
date: 2026-05-21
tags:
  - prompts
  - learning-sprint
  - sdet
---

# Learning Prompts - MOC

Ten katalog przechowuje gotowe prompty do uruchamiania kolejnych lekcji i learning sprintów. Prompty są częścią procesu nauki, nie luźnymi notatkami.

## Kolejność

1. `Prompt - Strategy - Three Advanced Learning Paths.md` - completed strategy/discovery prompt; output is `../Strategy/Three Advanced Learning Paths for API Testing and Payment Lab.md`
2. `Prompt - Lesson 01 - REST API Request Response Flow.md` - completed/read
3. `Prompt - Lesson 02 - REST Assured What REST Assured Is.md` - REST Assured entry lesson prompt
4. `Prompt - Lesson 04 - REST Assured Path Params Query Params and Headers.md` - request input channels prompt
5. `Prompt - Lesson 05 - REST Assured Request Body JSON Map DTO Serialization.md` - request body and serialization prompt
6. `Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md` - accelerated PayU-like sprint prompt driven by QA architecture, business flows, backend, UI, SQL, security and tests
7. `Prompt - Lesson 06 - REST Assured Response Assertions Status Body Headers AssertJ.md` - earlier narrower response assertions prompt, kept as reference

## Aktualny kontekst strategii

Strategia PayU-like learning clone jest juz zapisana i ograniczona do trzech sciezek:

- Payment Order REST API, idempotency and lifecycle.
- Security, ownership and tenant isolation matrix.
- Reliability, data integrity, audit trail, webhooks and event evolution.

Payment Order create/read foundation jest teraz aktywnym Lesson 6/Phase 2 slice. Szczegółowa notatka lekcji znajduje się w `../02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`.

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
