---
name: parallel-test-architecture-and-data-isolation
description: Design parallel-ready test suites and data strategy across JUnit, Spring tests, REST Assured, Testcontainers, WireMock and Playwright, with worker-aware data ownership and anti-flakiness discipline.
license: MIT
metadata:
  category: test-architecture
  author: project-custom
  version: "3.0.0"
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
- `references/parallelism-matrix.md`
- `references/test-data-namespacing.md`
