# Backend Foundation

This backend is the Phase 0 Spring Boot foundation for the Payment Quality Engineering Lab.

## Baseline

- Java JDK 25
- Maven Wrapper using Maven 3.9.11
- Spring Boot 4.0.6
- Spring Framework aligned by Spring Boot
- Spring Modulith 2.0.6
- JUnit 6.0.3
- AssertJ, Mockito, REST Assured, Testcontainers core, PostgreSQL JDBC, and future WireMock test area

Spring Security is intentionally not included in Phase 0 so the technical status endpoint remains simple and local verification is not confused with a complete auth flow.

## Commands

```bash
./mvnw test
./mvnw spring-boot:run
```

## Status Endpoint

```text
GET /api/status
```

Expected response:

```json
{"application":"payment-quality-lab","phase":"foundation","status":"UP"}
```

The endpoint must not expose secrets, database details, Keycloak details, payment identifiers, or business data.

## Package Conventions

- `lab.paymentquality`: application root
- `lab.paymentquality.foundation.status`: technical foundation status behavior
- `lab.paymentquality.shared.web`: reserved for narrow web conventions only
- `lab.paymentquality.architecture`: Spring Modulith architecture tests
- `lab.paymentquality.integration`: future Spring/Testcontainers integration tests
- `lab.paymentquality.rest`: REST Assured foundation and future API tests

Do not create empty business module shells. Future modules such as payment, merchant, PSP, refund, settlement, reconciliation, risk review, and audit are introduced only when a feature owns real behavior.

## Modulith Check

`ModulithArchitectureTest` runs `ApplicationModules.of(PaymentQualityApplication.class).verify()` as part of `./mvnw test`.

## Version Decisions

The selected dependency versions are recorded in the root `README.md`. Any future substitution must be documented here and in the relevant Spec Kit artifact.

Testcontainers note: Phase 0 includes `org.testcontainers:testcontainers:2.0.5` core only. Module-specific artifacts such as `junit-jupiter` and `postgresql` remain on the `1.21.4` line in Maven Central, so they are deferred until a future persistence/integration feature needs them.

## Phase 0 Non-Goals

- No payment business API
- No `POST /payments`
- No payment persistence or migrations
- No Kafka
- No PSP integration or mock flow
- No complete OAuth/OIDC integration
