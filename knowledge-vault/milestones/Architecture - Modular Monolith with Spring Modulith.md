# Architecture - Modular Monolith with Spring Modulith

Spring Modulith 2.0.6 is present from Phase 0 to make architecture testable.

## Current Stance

- Root package: `lab.paymentquality`
- Current behavior: foundation status only
- No fake payment or merchant modules
- Architecture verification runs in backend tests

## Future Module Candidates

- payment
- merchant
- psp
- webhook
- refund
- settlement
- reconciliation
- riskreview
- audit

Future specs must define ownership, public API, internals, allowed dependencies, event impact, and module test impact.

## Learning Point

Modular monoliths let QA reason about coupling, ownership, and targeted module tests before the system becomes physically distributed.
