---
type: tracker
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-27
tags:
  - lesson-evidence
  - learning-delta
  - sdet
  - qa-architecture
---

# Lesson Evidence Tracker

Cel: każda lekcja lub sprint ma mieć dowód, że temat został przerobiony w kodzie, testach, analizie albo świadomie odłożony.

## Template Dla Nowej Lekcji/Sprintu

```text
## Lesson NN - Title

Status:
Prompt:
Business capability:
Learning delta:
Skills expected:
Skills actually used:
Production code evidence:
Test code evidence:
Vault notes:
Spec Kit artifacts:
Commands run:
Competency matrix updates:
Open risks:
Interview answer EN:
Next lesson/sprint handoff:
```

## Lesson 01-05 Summary

| Lesson | Status | Evidence | Notes |
|---:|---|---|---|
| 01 | Introduced | REST API request/response flow prompt and vault note | Foundation; no need to repeat in Lesson 6 |
| 02 | Introduced | REST Assured entry prompt | Foundation; no need to repeat in Lesson 6 |
| 03 | Practiced | REST Assured foundations doc | HTTP method/endpoint/content-type basics |
| 04 | Practiced | Lesson 4 prompt + lesson-pack expansion | Path/query/header basics, `Authorization`, `X-Correlation-ID` context |
| 05 | Practiced | Lesson 5 prompt + lesson-pack expansion | request body, JSON, `Map.of`, DTO, serialization |

## Lesson 06 - PayU-like Business Flow Expansion Sprint

Status: `Planned`

Prompt: `../Learning Prompts/Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md`

Business capability: to be selected by BA/Architecture Team; candidates are Merchant Team/Access Management and Payment Order Initiation/Lifecycle.

Learning delta:

- no repetition of `given/when/then`, path params, basic headers, request body basics,
- new capability discovery,
- roles/permissions/ownership,
- idempotency,
- `Location`, `X-Correlation-ID`, `ETag`, `If-Match`, `412`, `409`, stable error contracts,
- SQL constraints and status/audit history,
- REST Assured framework architecture,
- AssertJ stronger assertions,
- Nuxt/Playwright role-aware journeys.

Skills expected:

- `qa-architecture-sprint-team` once runtime picks it up,
- `payment-quality-lab-orchestrator`,
- `business-analysis-and-product-discovery-for-payment-lab`,
- `spec-kit-feature-workflow`,
- `spring-boot4-spring7-backend-architect`,
- `spring-modulith-2-0-6-modular-monolith-testing`,
- `postgres18-data-architecture-and-risk`,
- `rest-api-security-oauth-testing`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `rapid-software-testing-risk-thinking`,
- `nuxt-dashboard-zod-pinia-frontend-engineering`,
- `typescript6-playwright-engineering`,
- `parallel-test-architecture-and-data-isolation`,
- `bpmn-uml-dmn-for-testers`.

Skills actually used: pending.

Production code evidence: pending.

Test code evidence: pending.

Vault notes: pending.

Spec Kit artifacts: pending.

Commands run: pending.

Competency matrix updates: pending after scope selection.

Open risks:

- active Phase 1 guardrails may block payment implementation until a new Spec Kit feature is created,
- `qa-architecture-sprint-team` was created in `.kilo/skills` and may require session reload to appear in runtime skill list,
- current roadmap and prompt index still need future cleanup after Lesson 6 direction is finalized.

Interview answer EN:

> From Lesson 6 onward I move from syntax lessons to product-driven API testing. I use business analysis, architecture, security and QA strategy to select a realistic payment or merchant-access flow, then I design the API, database constraints, authorization matrix and automated tests around the risks introduced by that flow.

Next lesson/sprint handoff: after Lesson 6 scope decision, update this tracker and the competency matrix before implementation.
