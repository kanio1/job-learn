# Architecture Test - ApplicationModules.verify

The backend includes a Spring Modulith architecture verification test:

```java
ApplicationModules.of(PaymentQualityApplication.class).verify();
```

## What It Verifies Now

- The root package can be analyzed by Spring Modulith.
- The current package structure has no illegal module cycles.
- The foundation status area does not create business-module coupling.

## What It Does Not Verify Yet

- Payment module rules, because no payment module exists.
- Event-driven behavior, because no business events exist.
- `@ApplicationModuleTest` behavior, because no real module behavior exists.

## Future Use

As modules appear, this test becomes a guardrail against accidental internal access and dependency drift.
