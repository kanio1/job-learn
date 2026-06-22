---
name: postgres18-data-architecture-and-risk
description: Design and test PostgreSQL 18 data structures, constraints, indexes, transactions, concurrency behavior, auditability and parallel-safe test data strategy for the payment platform.
license: MIT
metadata:
  category: data-architecture
  author: project-custom
  version: "3.0.0"
---

# PostgreSQL 18 Data Architecture and Risk

## Use when
- designing tables/constraints/indexes,
- reviewing transaction or consistency risk,
- choosing test-data setup strategy,
- discussing DB-level quality risks.

## Always consider
- uniqueness,
- check constraints,
- foreign keys,
- transaction boundaries,
- locks/isolation,
- idempotency,
- auditability,
- data separation,
- explain-plan implications when relevant.

## When Not to Use
Do not use this for browser-only UI behavior, token semantics or broad web research.

See `references/test-data-isolation.md`.
