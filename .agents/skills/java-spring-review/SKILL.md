---
name: java-spring-review
description: Use to review Java/Spring backend changes for this repository's modules, REST contracts, persistence, security, and tests; do not use as a broad Java tutorial or implementation agent.
---

# Java Spring Review

Use this skill to review backend diffs. When the user asked for a full branch/PR review, `code-review` runs first (Standards vs Spec); this skill is the Spring layer pass.

## Review Focus

- Controller/service/repository/domain boundaries.
- DTO validation and exception mapping.
- JPA mappings versus Flyway migrations.
- Transaction boundaries and optimistic/conditional update risks.
- Security authorities, JWT claims, and merchant ownership checks.
- Spring Modulith boundaries and public/internal package usage.
- REST contract stability: status codes, headers, error body, idempotency, ETag/If-Match, correlation IDs where implemented.
- Minimality of the change.

## Repository Rules

- `merchant` and `payment` are Spring Modulith modules.
- `foundation.status` and `shared.*` are supporting packages, not feature modules.
- Payment code must not depend on `merchant.internal`.
- Flyway owns schema creation; JPA validates.
- DB-backed tests use Testcontainers PostgreSQL.
- Security tests use `TestJwtConfiguration`.

## Output Format

Return findings first, ordered by severity, with file/line references when possible.

Then include:

- Open questions.
- Missing or weak tests.
- Scope creep check.
- Short explanation suitable for Java/SDET learning.

## Guardrails

- Do not implement fixes. Placement while building is `spring-modulith`.
- Do not request broad refactors unless they directly reduce current risk.
- Do not duplicate the large Java learning skills; keep review operational.
