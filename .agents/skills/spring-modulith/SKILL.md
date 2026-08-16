---
name: spring-modulith
description: >-
  Place and change Java 25 / Spring Boot 4.0.6 / Spring Framework 7 / Spring Modulith 2.0.6
  backend code in this lab: public vs internal packages, Flyway+JPA validate, @ApplicationModuleTest,
  and JDK 25 language choices. Use when adding or editing Spring modules, controllers, services,
  persistence, events, or architecture tests. Do not use as a generic Spring tutorial, Effective Java
  lesson, frontend work, or a review-only pass (that is java-spring-review).
---

# Spring Modulith (this lab)

Build Spring production code the way this repo already does. The agent already knows Spring; this skill is the **lab arrangement**.

Pinned from `apps/backend/pom.xml` — do not bump:

| Piece | Version |
|---|---|
| JDK | 25 (`maven.compiler.release`) |
| Spring Boot | 4.0.6 |
| Spring Framework | 7 (Boot parent) |
| Spring Modulith | 2.0.6 |

Live Modulith HTML may show 2.1.x. Follow **2.0.6 APIs already in this tree**.

## Compose

| Job | Skill |
|---|---|
| Red-green at HTTP/UI seams | `tdd` then `implement` |
| Deep-module vocabulary | `codebase-design` |
| Review a diff | `java-spring-review` (do not implement from that skill) |
| Teach Effective Java | `java25-effective-java-mentor` |
| Version-sensitive fact check | `research` + `official-docs-and-versioned-research` |

## Workflow

1. Name the **owning module** (`merchant`, `payment`, `tenant`, `iam`, `audit`, …). If none fits, stop — do not dump into `shared`.
2. Put the **public seam** at the module root package. Everything else under `internal/{application,domain,infrastructure,web}`.
3. Schema: new Flyway SQL in `apps/backend/src/main/resources/db/migration/<module>/`. JPA stays `ddl-auto: validate`.
4. Cross-module: depend on **root-package types only**. Payment must not import `merchant.internal`. Merchant must not import `tenant.internal`.
5. Tests: HTTP behavior via `tdd` (REST Assured). Module bootstrap / architecture via [testing.md](testing.md).
6. After Java placement, run `ModulithArchitectureTest` (and the module’s `*ModuleTest` if beans/packages changed).

## Lab mappings

| Vocabulary | Here |
|---|---|
| Modulith module | Direct sub-package of `lab.paymentquality` with `@ApplicationModule` |
| Provided interface | Public types in the module root (`MerchantPaymentEligibilityService`, `PaymentSeedCapability`) |
| Internal | `*.internal.*` — other modules must not import these |
| OPEN module | **Only** `lab.paymentquality.shared` |
| Adapter | JPA repo, `MockPspClient`, Testcontainers Postgres, JWT resource server |
| Events | `ApplicationEventPublisher` + `shared.events.AuditableActionOccurred` — not Kafka |

Package map, allowed dependencies, and “new module” checklist: [modules.md](modules.md).

JDK 25 allow/deny: [jdk25.md](jdk25.md).

## Defaults (one choice)

- Records for request/response DTOs and small value objects — already the house style.
- `@ActiveProfiles("test")` on Spring-context tests; DB tests extend `PostgresContainerSupport`.
- `@ApplicationModuleTest(STANDALONE)` plus `@MockitoBean` for other modules’ public beans (see `PaymentModuleTest`).
- `DIRECT_DEPENDENCIES` only when the module genuinely boots with those modules (`MerchantModuleTest` → tenant).
- Do **not** mark a feature module `Type.OPEN`.
- Do **not** add `module-info.java`, compact source files, or preview JEPs.

## When not to use

- Frontend / Playwright / Nuxt.
- REST Assured matrix design (`rest-api-test-design`).
- Review-only requests (`code-review` → `java-spring-review`).
- Inventing a microservice split, Kafka, outbox, or real PSP.
