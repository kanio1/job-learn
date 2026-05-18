# Modular Monolith Foundation

Phase 0 introduces Spring Modulith 2.0.6 as an architectural guardrail, not as decorative structure.

## Current Backend Shape

- Root package: `lab.paymentquality`
- Technical status behavior: `lab.paymentquality.foundation.status`
- Narrow future web conventions: `lab.paymentquality.shared.web`
- Architecture verification: `lab.paymentquality.architecture.ModulithArchitectureTest`

Phase 0 does not create `payment`, `merchant`, `psp`, `refund`, `settlement`, `reconciliation`, `riskreview`, or `audit` packages. Those module names are future candidates, not implemented modules.

## Why No Fake Business Modules

Empty module shells create false confidence. Spring Modulith should verify real boundaries once real behavior appears. Until then, Phase 0 documents the intended strategy and verifies that the current package structure can be analyzed.

## Public API and Internal Boundary Expectations

Future modules should expose narrow public APIs and keep implementation details internal. A feature specification must define:
- module ownership
- public API surface
- internal packages
- allowed dependencies
- event impact
- module test impact

## Event Deferral

No business events exist in Phase 0. Future events should be introduced only when they reduce coupling for real module collaboration and after transaction, retry, observability, and test implications are understood.

## Architecture Verification

`ModulithArchitectureTest` runs:

```java
ApplicationModules.of(PaymentQualityApplication.class).verify();
```

This is part of the default backend quality gate. It protects against cyclic dependencies and illegal module access as the system grows.

## ApplicationModuleTest Strategy

`@ApplicationModuleTest` is deferred until a real module owns behavior. Future module tests should verify module behavior in isolation, with direct dependencies, or with a full dependency tree depending on risk.

## Tester Risks To Watch

- Hidden coupling through shared packages
- Fake module shells that imply nonexistent behavior
- Broad module APIs
- Event overuse without recovery semantics
- Architecture tests skipped or treated as optional
