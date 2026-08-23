# Modulith and Spring tests

HTTP contracts belong to `tdd` (REST Assured). This file is **module bootstrap and architecture**. Checklists: [module-boundary-checklist.md](references/module-boundary-checklist.md), [modulith-test-strategy.md](references/modulith-test-strategy.md).

## Architecture

`lab.paymentquality.architecture.ModulithArchitectureTest`:

```java
ApplicationModules.of(PaymentQualityApplication.class).verify();
```

`verify()` rejects cycles and references into another module’s internal packages (OPEN `shared` is the exception). Run it whenever packages or imports change:

```bash
./mvnw -Dtest=ModulithArchitectureTest test
```

Working directory: `apps/backend`. First attempt without extra sandbox permissions (see `.cursor/rules/maven-shell-approvals.mdc`).

## Module slice

`@ApplicationModuleTest` limits component/entity scan to the module (and optionally its dependencies).

| Mode | When |
|---|---|
| `STANDALONE` | Default. Mock other modules’ public beans (`PaymentModuleTest` + `@MockitoBean MerchantPaymentEligibilityService`) |
| `DIRECT_DEPENDENCIES` | Module cannot boot without those modules (`MerchantModuleTest` → tenant) |
| `ALL_DEPENDENCIES` | Avoid unless a spec says the whole graph must boot |

Pattern already in repo:

- `@ActiveProfiles("test")`
- `@Testcontainers` + `PostgresContainerSupport` + `@DynamicPropertySource`
- Assert core beans with AssertJ
- Also call `ApplicationModules.of(…).verify()` from the module test if neighboring tests do

Do not use `@SpringBootTest` for a module-scoped check when `@ApplicationModuleTest` already exists for that module.

Skip `restkit/` and `paymentsupport/` unless the user includes them. Ignore `My*` / `Lesson*` as regression coverage.

## Persistence

- Flyway owns schema. Never `ddl-auto: update` / `create`.
- New tables/columns: SQL migration in the **owning** module’s Flyway directory, then JPA mapping that matches.
- Write-path REST Assured may assert DB **after** HTTP (lab exception in `tdd`). Module tests do not replace HTTP tests.

## Build / test split (Maven 3.9.11)

- Maven Wrapper is the only entry point (`./mvnw`); do not assume a host Maven.
- `./mvnw test` runs `*Test.java` via Surefire; `./mvnw verify` additionally runs `*IT.java` via Failsafe.
- New integration-only suites go to Failsafe naming (`*IT`), not Surefire excludes — keep unit/HTTP suites fast and broker-free.
- Do not add plugin or dependency bumps as a side effect of feature work.

## DB risk lens

When designing tables, constraints, or persistence-touching tests, always weigh: uniqueness, check constraints, foreign keys, transaction boundaries, locks/isolation, idempotency, auditability, data separation between tenants/merchants, and explain-plan implications when relevant. In this lab that means: unique references enforced by Flyway, tenant/merchant ownership predicates in queries, and per-test unique data (see the isolation pattern in `rest-api-test-design`).

## Commands

From `apps/backend`:

```bash
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest=MerchantModuleTest test
./mvnw -Dtest=ModulithArchitectureTest test
```

Dev run still needs `SPRING_PROFILES_ACTIVE=dev`.
