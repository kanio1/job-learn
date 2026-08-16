---
name: codebase-design
description: Shared vocabulary for designing deep modules. Use when designing or improving a module interface, placing a test seam, making code more testable, or when TDD/implement needs the deep-module vocabulary.
---

# Codebase Design

Design **deep modules**: a lot of behaviour behind a small interface, placed at a clean seam, testable through that interface.

In this lab a Modulith **module** (`merchant`, `payment`, `tenant`, `iam`) is the usual large-scale module. A package, type, or function can also be a module. Public API lives at the module root; implementation lives under `internal`.

## Glossary — use these terms exactly

**Module** — anything with an interface and an implementation (function, class, package, Modulith module).

**Interface** — everything a caller must know: type surface, invariants, error modes, ordering, required headers, authorities. Not only the Java `interface` keyword.

**Implementation** — the body behind the interface.

**Depth** — leverage at the interface: lots of behaviour per unit of interface the caller learns. **Shallow** = interface almost as complex as the body.

**Seam** — where that interface lives; where tests observe behaviour without reaching inside.

**Adapter** — a concrete thing that satisfies an interface at a seam (JPA repository, Testcontainers DB, `TestJwtConfiguration`).

**Leverage** — callers get more capability per fact they learn.

**Locality** — change, bugs, and tests concentrate in one place.

## Lab mappings

| Vocabulary | In this repo |
|---|---|
| External seam for payments | Merchant-scoped HTTP API under `/api/merchants/{id}/payment-orders` |
| Internal seam | `payment.internal` types — **do not** test these from another module |
| Forbidden dependency | `payment` → `merchant.internal` |
| Adapter | Flyway-owned PostgreSQL, JWT resource server, Nuxt BFF proxy |

## Principles

- **Deletion test.** If deleting the module makes complexity vanish, it was a pass-through.
- **The interface is the test surface.** Callers and tests cross the same seam.
- **One adapter = hypothetical seam. Two adapters = real seam.** Do not invent a seam until something actually varies (Testcontainers vs production Postgres is a real seam; mocking `PaymentOrderService` in a REST Assured test is not).
- Prefer fewer methods and simpler parameters. Hide lifecycle rules, ETag/idempotency, and tenant masking behind the HTTP/module interface.

## Designing for testability

1. Accept dependencies; do not create them inside domain logic.
2. Return results rather than mutating hidden state when the rule is a calculation.
3. Keep the HTTP contract stable; tests should not need private package access.

When TDD asks where a test belongs, answer with this vocabulary and the lab seam table in `tdd`.
