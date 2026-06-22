---
type: learning-os
status: active
date: 2026-05-30
tags:
  - learning-os
  - flow
---

# Current Learning Flow

## The Core Flow (Lesson 6 Onward)

```
1. Open [[START HERE - Learning Dashboard]].
2. Pick one learning topic from [[Current Lesson]].
3. Choose the right mode (A/B/C/D) based on topic type.
4. Study or practice one session.
5. Run one test or inspect one file.
6. Do one exercise.
7. Update progress with [[Prompt - Mark Lesson Progress]].
8. Verify understanding with [[Prompt - Verify My Understanding]].
9. Only then pick the next lesson or sprint.
```

## Four Learning Modes

### Mode A — Concept Lesson (small foundation topics)

```
Small topic (HTTP header, RA method, Java syntax, SQL clause, AssertJ assertion)
  → [[Prompt - Concept Lesson]]
  → No Spec Kit — never
  → Study concept + example from repo + one exercise
  → Mark progress
```

Use for: `ETag` header, `assertThat().extracting()`, `CHECK` constraint, `@DisplayName`, `RequestSpecification`.

### Mode B — Code Reading Lesson (understanding existing implementation)

```
Existing code file or module
  → [[Prompt - Code Reading Lesson]]
  → No refactoring — never
  → Read flow step by step + identify risks + QA question
  → Mark progress
```

Use for: `PaymentOrderService.create()`, `V2__create_payment_orders.sql`, `PaymentOrderSecurityTest`.

### Mode C — Payment Order Case Study (Lesson 6 vertical slice)

```
Specific behavior in Payment Order domain
  → [[Prompt - Payment Order Case Study]]
  → Study: code → tests → SQL → security → risks
  → Examples: idempotency, tenant isolation, headers, DB constraints, security matrix
  → Decision table + exercise + interview answer
  → Mark progress
```

Use for: idempotent create flow, tenant isolation matrix, response headers contract.

### Mode D — Learning Sprint (new business capability)

```
New business capability needed
  → [[Prompt - Learning Sprint Discovery]]
  → BA Discovery → Architecture → Spec Kit decision
  → Spec Kit only when: new module, new resource, new security model
  → Implementation → tests → lesson note → evidence → interview
  → Update evidence with [[Prompt - Post Sprint Evidence Update]]
```

Use for: payment lifecycle, new module, new REST resource, new DB schema.

## Per-Lesson Constraints

Every lesson or sprint must respect:

- At most **one** main new topic
- **Two** repeated topics from previous lessons
- **One** Java 25 focus
- **One** REST Assured focus
- **One** SQL/data focus
- **One** test design method

## Spec Kit Decision

| Scope | Spec Kit? |
|---|---|
| Adding a test | **NO** |
| Fixing a bug | **NO** |
| Deepening existing topic | **NO** |
| Adding validation rule | **NO** |
| New API endpoint (same module) | **NO** (lesson extension) |
| New REST resource with its own lifecycle | **LIGHT:** spec + plan |
| New Spring Modulith module | **FULL:** spec + plan + tasks + data-model + contracts |
| New security role model | **FULL** |
| Cross-module public API boundary | **FULL** |

## Active Specs

| Spec | Status |
|---|---|
| `specs/001-project-foundation/` | Completed |
| `specs/002-merchant-registry-activation/` | Completed |
| `specs/003-payment-order-access-lifecycle/` | Completed baseline for Lesson 06 |
| `specs/004-payment-order-list-filter/` | Completed baseline for Lesson 07 |
| Lesson 08 summary extension | Planned; no Full Spec Kit by default |

## Key Commands

```bash
./mvnw test                                      # all backend tests
./mvnw -Dtest="PaymentOrder*" test              # payment tests
./mvnw -Dtest=PaymentModuleTest test             # payment module architecture
corepack pnpm typecheck                          # frontend typecheck
```

## Navigation

- [[Current Lesson]] — what to do NOW
- [[Current Sprint]] — sprint status
- [[Curriculum Backbone]] — technology ↔ lesson map
- [[How To Use This Vault]] — vault usage guide
