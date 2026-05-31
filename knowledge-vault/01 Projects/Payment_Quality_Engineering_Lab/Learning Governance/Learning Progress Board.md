---
type: tracker
status: active
date: 2026-05-30
tags:
  - learning-os
  - progress
  - dashboard
---

# Learning Progress Board

Overall progress across all lessons and competencies.

## Lesson Progress

| Lesson | Area | Status | Read | Practiced | Test Run | Summary Written | Can Explain EN | Needs Repeat | Next Review |
|---|---|---|---|---|---|---|---|---|---|
| 01 — REST API Request/Response | REST/HTTP | Covered | ✓ | ✓ | ✓ | ✓ | — | No | — |
| 02 — REST Assured Introduction | REST Assured | Covered | ✓ | ✓ | ✓ | ✓ | — | No | — |
| 03 — REST Assured Foundations | REST Assured | Covered | ✓ | ✓ | ✓ | ✓ | — | No | — |
| 04 — Path/Query/Headers | REST Assured | Covered | ✓ | ✓ | ✓ | ✓ | — | No | — |
| 05 — Request Body/JSON/DTO | REST Assured, Java | Covered | ✓ | ✓ | ✓ | ✓ | — | No | — |
| **06 — Payment Order Create/Read** | **All** | **Covered** | ✓ | ✓ | ✓ | ✓ | ✓ | — | Monthly |
| **07 — Payment Order List/Filter** | **RA, SQL, AssertJ** | **Covered** | ✓ | ✓ | ✓ | ✓ | ✓ | — | Monthly |
| 06b — Assertion Strategy Deep Dive | RA, AssertJ, SQL | Planned | — | — | — | — | — | — | — |
| **08 — Payment Aggregation** | **SQL, RA, AssertJ** | **Planned** | — | — | — | ✓ | — | — | Weekly |
| 09 — Auth Ownership | Security | Planned | — | — | — | — | — | — | — |

## Competency Progress

| Area | Covered | Introduced | Not Started | Deferred |
|---|---|---|---|---|
| Java 25 | 5 | 2 | 1 | 0 |
| HTTP/REST | 4 | 2 | 4 | 0 |
| REST Assured | 5 | 3 | 4 | 2 |
| AssertJ | 1 | 2 | 2 | 0 |
| JUnit | 1 | 0 | 2 | 0 |
| Spring Testing | 2 | 1 | 0 | 0 |
| SQL/PostgreSQL | 3 | 2 | 2 | 0 |
| Security | 2 | 1 | 0 | 0 |
| Test Design | 3 | 2 | 0 | 0 |
| Frontend | 1 | 1 | 0 | 0 |
| Playwright | 1 | 0 | 0 | 0 |
| Test Data Management | 0 | 2 | 0 | 0 |
| Database Verification | 0 | 1 | 2 | 0 |
| Assertion Strategy | 0 | 1 | 0 | 0 |
| Observability | 0 | 1 | 1 | 1 |

Detailed competency status: [[Senior SDET Competency Coverage Matrix]]

## Sprint Progress

| Sprint | Status | Lesson | Completion |
|---|---|---|---|
| Sprint 1 — Merchant create/list/get | Complete | 01-03 | 100% |
| Sprint 2 — Activate/Suspend | Complete | 04-05 | 100% |
| Sprint 3 — REST Assured cleanup | Complete | Foundation doc | 100% |
| Sprint 4 — Payment Order create | Complete | 06 (part) | 100% |
| Sprint 5 — Status lookup | Merged into Sprint 6 | — | — |
| **Sprint 6 — Create/Read** | **Complete** | **06** | **100%** (all tasks) |
| Sprint 6.5 — Assertion deep dive | Planned | 06b | 0% |
| **Sprint 7 — List/Filter** | **Complete** | **07** | **100%** (implementation complete, lesson evidence captured) |
| **Sprint 8 — Aggregation Summary** | **Planned** | **08** | **10%** (scope note and prompt ready) |

## Evidence By Lesson

See [[Lesson Evidence Tracker]] for detailed evidence per lesson.

## Weekly Goals

### This Week (Week of 2026-05-30)

- [x] Analyze Lessons 06 and 07 plus prompts
- [x] Create Lesson 08 scope note and implementation prompt
- [ ] Implement Payment Order Summary endpoint
- [ ] Add REST Assured aggregation contract tests
- [ ] Add summary security matrix tests
- [ ] Run payment test suite and Modulith verification

### Next Week

- [ ] DB oracle and EXPLAIN deep dive for Lesson 08
- [ ] Optional minimal Nuxt summary panel after backend is green
- [ ] Update competency matrix for Lesson 08 topics after implementation evidence exists

## Navigation

- [[Current Lesson]] — detailed lesson status
- [[Current Sprint]] — sprint details and remaining tasks
- [[Lesson Evidence Tracker]] — per-lesson evidence
- [[Learning Coverage Backlog]] — what's not yet covered
