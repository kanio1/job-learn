---
name: spring-modulith-2-0-6-modular-monolith-testing
description: Use Spring Modulith 2.0.6 as an architectural and testing guardrail: module boundaries, architecture verification, module-level integration tests, generated module documentation, events and publication-registry thinking.
license: MIT
metadata:
  category: modular-architecture-testing
  author: project-custom
  version: "3.0.0"
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
- `references/module-boundary-checklist.md`
- `references/modulith-test-strategy.md`
