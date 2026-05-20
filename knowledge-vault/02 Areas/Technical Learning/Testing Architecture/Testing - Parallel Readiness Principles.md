# Testing - Parallel Readiness Principles

Parallel readiness is a design constraint, not a CI toggle.

## Principles

- Tests should be order-independent.
- Tests should avoid shared mutable state.
- Test data should be uniquely namespaced when data exists.
- Playwright should use future worker-aware users and resources.
- REST tests should avoid mutable global request specs.
- WireMock should use dynamic ports and isolated stubs.
- Testcontainers usage should not hide shared mutable state.

## Future Data Pattern

```text
testRunId = 20260518-abcdef
workerId = worker-0
scenarioId = status-smoke
externalReference = PAY-{workerId}-{scenarioId}-{uuid}
```

## Tester Story

The first tests are small, but they avoid decisions that would block safe parallel API, integration, or E2E execution later.
