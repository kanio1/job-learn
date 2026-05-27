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

Cel: upewnić się, że przy nowych business flows skills uruchamiają się w dobrej kolejności i żaden obszar nie wypada: business analysis, architektura, QA architecture, test design, Spec Kit, implementacja, UI i evidence.

## Zasada Od Lesson 6

Nie zaczynamy od kodowania endpointów.

Zaczynamy od zespołu biznesowo-architektonicznego i QA Architect:

1. odkrycie capability,
2. wybór scope,
3. model domeny i workflow,
4. architektura i data/security boundaries,
5. QA architecture i learning delta,
6. Spec Kit input,
7. implementacja,
8. testy,
9. evidence update.

## Skill Flow

| Faza | Skill | Output |
|---|---|---|
| Orchestration start | `payment-quality-lab-orchestrator` | decyzja: BA, Spec Kit, implementacja czy analiza |
| Sprint team gate | `qa-architecture-sprint-team` | Capability Discovery Brief, Sprint Scope Decision, Learning Delta Map |
| Business discovery | `business-analysis-and-product-discovery-for-payment-lab` | BA Discovery Pack |
| Risk lens | `rapid-software-testing-risk-thinking` | product risks, exploratory charters |
| Modeling | `bpmn-uml-dmn-for-testers` | BPMN, sequence, state, decision tables |
| Spec handoff | `spec-kit-feature-workflow` | spec/plan/tasks/DoD/tester learning flow |
| Backend architecture | `spring-boot4-spring7-backend-architect` | modules, layers, transactions, validation, REST boundaries |
| Modulith guardrails | `spring-modulith-2-0-6-modular-monolith-testing` | module boundaries and architecture tests |
| Data architecture | `postgres18-data-architecture-and-risk` | tables, constraints, indexes, transactions, audit strategy |
| Security architecture | `rest-api-security-oauth-testing` | auth matrix, ownership, 401/403/404 decisions |
| Java/API testing | `java-rest-api-testing-effective-java-mentor` | REST Assured/Java design, DTOs, test support |
| Testcraft | `junit6-assertj-restassured-testcraft` | test layer, oracle, AssertJ/REST Assured patterns |
| Test design/data | `test-analysis-design-and-data` | BVA, EP, decision tables, state tests, data packs |
| Parallel readiness | `parallel-test-architecture-and-data-isolation` | worker-safe data and isolation strategy |
| Frontend architecture | `nuxt-dashboard-zod-pinia-frontend-engineering` | routes, forms, schemas, stores, testable UI |
| E2E engineering | `typescript6-playwright-engineering` | Playwright fixtures, role flows, selectors |
| Vault capture | `obsidian-learning-os` | notes, trackers, MOCs |
| Skill governance | `project-skill-governance-and-quality-review` | skill overlap/quality check |

## Required Output Per New Sprint

Każdy nowy sprint od Lesson 6 musi zostawić:

- `Capability Discovery Brief`,
- `Sprint Scope Decision`,
- `Learning Delta Map`,
- `Business Workflow`,
- `Domain Vocabulary`,
- `API Contract Sketch`,
- `Data Model Sketch`,
- `Security Matrix`,
- `Test Strategy`,
- `Spec Kit Input Pack`,
- `Implementation/Test Task Breakdown`,
- `Evidence Update` in `Lesson Evidence Tracker.md`,
- `Competency Update` in `Senior SDET Competency Coverage Matrix.md`.

## Mandatory Gates

### Gate 1 - No Repetition Gate

- [ ] Czy sprint dodaje nowy business behavior?
- [ ] Czy nie powtarzamy Lessons 1-5 poza krótkim prerequisite?
- [ ] Czy jest `Learning Delta Map`?

### Gate 2 - Business Analysis Gate

- [ ] Czy aktorzy są jasni?
- [ ] Czy workflow ma success, alternate i failure paths?
- [ ] Czy reguły biznesowe są testowalne?
- [ ] Czy są jawne open questions?

### Gate 3 - Architecture Gate

- [ ] Czy wiadomo, który moduł Spring Modulith jest właścicielem?
- [ ] Czy transakcje są jawne?
- [ ] Czy walidacja jest rozdzielona między web/domain/DB?
- [ ] Czy DB constraints chronią krytyczne reguły?
- [ ] Czy API ma sensowne statusy i headers?

### Gate 4 - Security Gate

- [ ] Czy jest ownership/tenant decision?
- [ ] Czy są `401`, `403`, `404` decyzje?
- [ ] Czy UI nie jest jedynym miejscem blokady?
- [ ] Czy testy obejmują BOLA/BFLA?

### Gate 5 - QA Architecture Gate

- [ ] Czy każda reguła ma właściwy poziom testu?
- [ ] Czy REST Assured testy nie dublują niepotrzebnie Playwright?
- [ ] Czy repository tests sprawdzają constraints?
- [ ] Czy dane są parallel-safe?
- [ ] Czy logs nie wyciekną tokenów?

### Gate 6 - Evidence Gate

- [ ] Czy tracker lekcji został uzupełniony?
- [ ] Czy competency matrix została uzupełniona?
- [ ] Czy prompt i Spec Kit artifacts są podlinkowane?
- [ ] Czy istnieje interview answer EN?

## Skill Runtime Note

`qa-architecture-sprint-team` został utworzony jako project-local skill w `.kilo/skills/qa-architecture-sprint-team/SKILL.md`.

Jeśli runtime jeszcze go nie widzi, odśwież sesję Kilo. Do tego czasu prompt ma traktować jego sekcje jako wymagany output ręczny.
