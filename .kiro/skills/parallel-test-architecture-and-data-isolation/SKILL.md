---
name: parallel-test-architecture-and-data-isolation
description: Use when designing parallel-ready test suites and data isolation strategy across JUnit, Spring tests, REST Assured, Testcontainers, WireMock, and Playwright with worker-aware data ownership and anti-flakiness discipline.
---

# Parallel Test Architecture and Data Isolation

## Use when
- designing frameworks for parallel execution,
- preventing shared-state flakes,
- planning test data ownership,
- reviewing parallel safety of API/integration/E2E suites.

## When Not to Use
Do not use this for a trivial isolated unit test or product review with no execution-model concern.

See:
- `.kilocode/skills/parallel-test-architecture-and-data-isolation/references/parallelism-matrix.md`
- `.kilocode/skills/parallel-test-architecture-and-data-isolation/references/test-data-namespacing.md`
