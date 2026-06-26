# Backend Foundation and Merchant Registry

This backend is the Spring Boot foundation for the Payment Quality Engineering Lab. Phase 1 adds the first real business module: Merchant Registry and Activation.

## Baseline

- Java JDK 25
- Maven Wrapper using Maven 3.9.11
- Spring Boot 4.0.6 / Spring Framework 7
- Spring Modulith 2.0.6
- JUnit 6.0.3, AssertJ, Mockito, REST Assured
- Spring Security resource server, Spring Data JPA, Flyway, PostgreSQL JDBC, Testcontainers PostgreSQL

## Commands

```bash
./mvnw test
./mvnw verify
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

> **`dev` profile is required for local startup.** It activates `corsConfigurationSource` for `http://localhost:3000` (Nuxt frontend). Without it the application context fails to load. The `test` profile is reserved for the test suite. Do not remove `@Profile({"dev","test"})` from `SecurityConfig` as a workaround.

## Public Status Endpoint

```text
GET /api/status
```

Expected response:

```json
{"application":"payment-quality-lab","phase":"foundation","status":"UP"}
```

`/api/status` remains public and must not expose secrets, database details, Keycloak details, payment identifiers, or merchant business data.

## Merchant Module

Owning module: `lab.paymentquality.merchant`.

Phase 1 endpoints:

```text
POST /api/merchants
GET /api/merchants
GET /api/merchants/{id}
POST /api/merchants/{id}/activate
POST /api/merchants/{id}/suspend
```

Merchant references are trimmed, uppercased, validated as 3-64 letters/numbers/hyphens with no leading or trailing hyphen, and stored in `normalized_reference` with a PostgreSQL unique constraint. Merchants use lifecycle `DRAFT -> ACTIVE -> SUSPENDED`.

## Persistence

Flyway migration location:

```text
src/main/resources/db/migration/merchant/V1__create_merchants.sql
```

JPA uses `ddl-auto: validate`; schema creation is owned by Flyway. Tests use Testcontainers PostgreSQL 18 with Flyway enabled.

## Security

The backend is a JWT resource server. Keycloak realm roles are mapped to Spring authorities by prefixing `platform:`.

Required authorities:

- `platform:merchants:create` for merchant creation
- `platform:merchants:read` for list and retrieve
- `platform:merchants:update-status` for activate and suspend

HTTP security tests use locally generated signed JWTs and do not require live Keycloak.

## Modulith Check

`ModulithArchitectureTest` runs `ApplicationModules.of(PaymentQualityApplication.class).verify()` as part of `./mvnw test`. `MerchantModuleTest` boots the merchant module with `@ApplicationModuleTest`.

## Non-Goals

- No payment business API or `POST /payments`
- No PSP, Kafka, refunds, settlement, reconciliation, KYC, or Client Credentials Flow
- No payment persistence or payment status read model
