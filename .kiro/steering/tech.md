# Tech Context — Payment Quality Engineering Lab

## Backend Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4.0.x / Spring Framework 7
- **Modularity**: Spring Modulith 2.0.6
- **Persistence**: PostgreSQL 18, Flyway migrations, JPA with `ddl-auto: validate`
- **Security**: Spring Security JWT resource server, Keycloak realm roles → `platform:*` and `merchant:*` authorities
- **Build**: Maven 3.9.11 with Maven Wrapper (`./mvnw`)

### Backend commands (run from `apps/backend`)
```bash
./mvnw test          # unit + slice tests (*Test.java via Surefire)
./mvnw verify        # all tests including integration (*IT.java via Failsafe)
./mvnw spring-boot:run
```

## Frontend Stack

- **Framework**: Nuxt 4.4.x (app directory layout)
- **UI library**: @nuxt/ui 4.7.x (Nuxt UI Dashboard Template)
- **Language**: TypeScript 6
- **State**: Pinia 3
- **Validation**: Zod 4
- **Auth**: nuxt-auth-utils (OIDC, Keycloak)
- **Package manager**: pnpm (corepack)
- **E2E tests**: Playwright 1.60

### Frontend commands (run from `apps/frontend`)
```bash
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm build
corepack pnpm exec playwright test
```

## Infrastructure

```bash
# From repo root
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

Keycloak imports `infra/keycloak/realms/payment-quality-realm.json`. Local admin: `admin` / `admin`.

## Testing Stack

| Layer | Tool |
|---|---|
| Java unit / slice | JUnit 6, AssertJ, Mockito |
| Java integration | REST Assured 6, Testcontainers (PostgreSQL), Spring Modulith tests |
| Architecture | `ModulithArchitectureTest`, `MerchantModuleTest`, `PaymentModuleTest` |
| Frontend E2E | Playwright 1.60, TypeScript 6, storage-state auth setup |

## Preferred Approach

### Thin abstractions
- Build the thinnest useful abstraction that removes duplication.
- Don't create a helper class for a single use. Generalize only after the second or third repetition.
- RestKit helpers live in `apps/backend/src/test/java/lab/paymentquality/rest`.

### No overengineering
- No framework-within-a-framework.
- No fluent DSLs unless the domain clearly benefits.
- Prefer straightforward readable code over clever patterns.

### Reusable helpers — only after repetition
- Extract a helper when the same boilerplate appears in 2+ places.
- Keep helpers focused: one concern per class.
- Prefer static factory methods over constructors for test data builders.

### HTTP contracts stay stable
- REST contracts are not changed unless the spec explicitly requires it.
- Headers (ETag, If-Match, Idempotency-Key, Location, Vary, Cache-Control, X-Correlation-ID) are preserved and tested.
- Problem+json error shape is tested with dedicated assertion helpers.

### Security by default
- Never log or expose tokens, passwords, Authorization headers, PAN, CVV, or personal data.
- Bearer tokens are attached server-side by the Nuxt proxy layer — never exposed in browser storage.
- Authorization header values are masked in any debug/learning panel.

### Small, reviewable changes
- Prefer small commits scoped to one concern.
- Do not rewrite existing code without an explicit reason.
- Preserve existing tests unless the spec explicitly changes the behavior they cover.
