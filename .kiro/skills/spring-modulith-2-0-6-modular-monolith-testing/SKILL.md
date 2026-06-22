---
name: spring-modulith-2-0-6-modular-monolith-testing
description: Use when designing, verifying, or testing Spring Modulith 2.0.6 module boundaries, architecture tests, module-scoped integration tests, generated documentation, and inter-module event strategy.
---

# Spring Modulith 2.0.6 Modular Monolith and Testing

## Use when
- defining or reviewing module boundaries,
- adding architecture verification,
- planning module tests,
- documenting module maps,
- reasoning about inter-module events and recovery.

## Apply it to
- module ownership,
- public vs internal packages,
- illegal dependencies,
- direct call vs event decision,
- generated docs and module maps,
- architecture tests,
- module-scoped integration tests.

## When Not to Use
Do not use this for ordinary controller design, generic Spring layering or eventing without a real coupling problem.

See:
- `.kilocode/skills/spring-modulith-2-0-6-modular-monolith-testing/references/module-boundary-checklist.md`
- `.kilocode/skills/spring-modulith-2-0-6-modular-monolith-testing/references/modulith-test-strategy.md`
