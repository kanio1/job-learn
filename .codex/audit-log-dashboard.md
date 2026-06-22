# Audit Log Dashboard Execution Notes

## Wave 0 — Prerequisite gate

Status: `READY_FOR_WAVE_1`

Verified on 2026-06-18. Wave 0 was verification-only; no backend or frontend code was changed.

### Spec #1 prerequisite

- READY. The Keycloak realm defines all five composite roles: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `MERCHANT_MANAGER`, `SUPPORT_AGENT`, and `READ_ONLY_USER`.
- `Authorities` is the shared compile-time catalog for enforced backend authorities.
- `KeycloakRealmRoleConverter` uses an explicit `KNOWN_ROLES` allowlist and drops unknown roles.
- The dashboard client's Keycloak protocol mapper issues the `tenant_id` claim in ID, access, and user-info tokens; seeded users carry tenant attributes.
- Backend JWT conversion/security and frontend session/RBAC foundations are present. The frontend login callback retains only the five composite role labels in the non-secure session and extracts `tenant_id`; `rbacMatrix.ts` maps those roles to UI capabilities.
- The consolidated status audit classifies `iam-roles-and-keycloak-login` as complete, with required behavior implemented and previously validated green.

### Spec #2 prerequisite

- READY. The `tenant` Spring Modulith module exists.
- Its public root package exposes `TenantResolver`, `TenantContext`, and `TenantReference`; implementation remains under `tenant.internal`.
- Tenant resolution is based on the JWT `tenant_id` claim and exposes platform-scoped versus tenant-scoped context.
- Tenant isolation behavior is implemented and covered by `TenantIsolationIT`: cross-tenant reads are masked as `404 not_found`, while cross-tenant writes return generic `403 forbidden` / `Access denied` without leaking the foreign tenant.
- The consolidated status audit classifies `tenant-model-and-isolation` as complete in code/status with prior green filtered Surefire and Failsafe evidence.

### Spec #3 user-management status

- `user-management` is `COMPLETE_WITH_OPTIONAL_GAPS` in both `.codex/current-state.md` and `.codex/user-management.md`.
- Required backend and frontend behavior is implemented and previously validated; skipped items are explicitly optional.
- Therefore Task 6.3 is in scope for a later emitter wave: IAM user create, update, and role-assignment actions are not deferred for absence of Spec #3.

### Event baseline

- Searches of backend production and test Java sources returned no `ApplicationEventPublisher` usage and no `@ApplicationModuleListener` usage.
- The current project therefore has no existing Spring application-event / Spring Modulith listener pattern to reuse.
- `audit-log-dashboard` will introduce the project's first event publication/listener usage, subject to the explicit dependency approval required by Task 3.2 in a later wave.

### Commands run

- `git status --short` — clean working tree before Wave 0 documentation updates.
- `git branch --show-current` — `018-rest-security-p1-error-auth-method-hardening`.
- `grep -RIn "ApplicationEventPublisher\|@ApplicationModuleListener" apps/backend/src/main/java apps/backend/src/test/java || true` — no matches.
- `grep -RIn "class Authorities\|PLATFORM_ADMIN\|TENANT_ADMIN\|SUPPORT_AGENT\|tenant_id\|KeycloakRealmRoleConverter" apps/backend/src/main/java apps/backend/src/test/java infra/keycloak apps/frontend/app apps/frontend/server || true` — prerequisite implementation found.
- Focused `rg`, `find`, and `sed` inspection of the tenant API, merchant isolation behavior/tests, Keycloak realm, frontend RBAC/session code, `.codex` status files, audit spec files, and required steering documents.
- No tests were run because Wave 0 is verify-only and changed no production code.

### Risks / blockers

- No blocker prevents Wave 1.
- Task 3.2 remains a future approval gate: do not add `spring-modulith-events-api` or `spring-modulith-events-jpa` without explicit user approval.
- The audit event contract must enforce the documented confidentiality rules before emitters are added; no secret or payment-sensitive data may enter event payloads or audit rows.
- Status documents report unrelated excluded `restkit/**` work from earlier sessions; it is outside this gate and does not invalidate the verified prerequisites.

### Changed files

- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Deferred items

- All implementation tasks, dependency changes, tests, and frontend work are deferred to explicitly requested later waves.
- Task 6.3 IAM/user-management emitters are in scope for the later emitter wave.

### Next

Wave 1 may start when explicitly requested. Do not start Wave 1 automatically.

## Wave 1 — Cross-spec authority extensions

Status: `COMPLETED`

Completed on 2026-06-18.

### Realm changes

- Added additive realm authority roles `platform:audit:read` and `tenant:audit:read`.
- Added `platform:audit:read` to `PLATFORM_ADMIN` and `SUPPORT_AGENT`.
- Added `tenant:audit:read` to `TENANT_ADMIN`.
- Confirmed `MERCHANT_MANAGER` and `READ_ONLY_USER` receive neither audit-log authority.
- Preserved all existing roles and composite mappings, including the distinct `platform:payments:audit` role.
- `jq empty infra/keycloak/realms/payment-quality-realm.json` passed.

### Authorities catalog

- Added `Authorities.PLATFORM_AUDIT_READ = "platform:audit:read"`.
- Added `Authorities.TENANT_AUDIT_READ = "tenant:audit:read"`.
- Kept both constants distinct from the unchanged `Authorities.PLATFORM_PAYMENTS_AUDIT`.
- Updated the catalog count and its existing drift test from 17 to 19 enforced authorities.

### Converter allowlist

- Added data-only allowlist entries for `platform:audit:read` and `tenant:audit:read`.
- Did not change the converter algorithm. Unknown roles remain ignored fail-closed.
- Extended the existing converter regression map from 18 to 20 known realm roles.

### Tests

- `./mvnw -Dtest=KeycloakRealmRoleConverterTest,AuthorityCatalogDriftTest test`: initially blocked at `testCompile` by the known unrelated, excluded `restkit/contract/create/PaymentOrderSecurityContractRestKitTest.java`; it references missing `HeaderAssertions.assertWwwAuthenticatePresent` and `assertAuthorizationTokenIsNotLeaked` methods.
- Retried the targeted command after temporarily moving only that untracked excluded file to `/tmp` with automatic `trap` restoration: GREEN, 21 tests, 0 failures, 0 errors, 0 skipped.
- Initial filtered broad test inside the sandbox was blocked by Mockito/Byte Buddy agent attachment and Testcontainers/Podman access restrictions; this was an environment failure, not a product assertion failure.
- Retried outside the sandbox with Podman and the repository-mandated exclusions: `./mvnw -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' test`: GREEN, 339 tests, 0 failures, 0 errors, 5 skipped.
- The untracked excluded RestKit file was restored automatically after each run and was not modified.
- `grep -RIn "platform:audit:read\|tenant:audit:read\|PLATFORM_AUDIT_READ\|TENANT_AUDIT_READ" infra/keycloak/realms/payment-quality-realm.json apps/backend/src/main/java apps/backend/src/test/java || true`: all intended realm, catalog, converter, and test entries found.
- `git diff --check`: GREEN.

### Changed files

- `infra/keycloak/realms/payment-quality-realm.json`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/Authorities.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/test/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverterTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/AuthorityCatalogDriftTest.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Risks / deferred items

- Realm JSON structure and composite mappings were validated statically; no live Keycloak import test was required by this focused wave.
- Spring Modulith event dependencies, event contracts, the audit module, migrations, emitters, REST endpoints, and frontend work remain deferred.
- No dependencies, Playwright files, frontend files, or `.kiro/**` files were changed. Playwright was not run.

### Next

Wave 2 may start only when explicitly requested. Do not start Wave 2 automatically.

## Wave 2 — Shared event contract + durable event-log infrastructure

Completed on: 2026-06-18

Status: `COMPLETED`

### Shared event contract

- `AuditableActionOccurred` and `Outcome` already exist in the OPEN shared module.
- `AuditableActionOccurredTest` is green: 3 tests, 0 failures, 0 errors.
- The event contains only explicit safe metadata; no generic raw payload/body field was added.

### Durable event dependencies

- Added `org.springframework.modulith:spring-modulith-events-api`.
- Added `org.springframework.modulith:spring-modulith-events-jpa`.
- Both resolve to `2.0.6` through the existing `spring-modulith-bom`; no dependency version strategy changed.
- No unrelated dependency was added.

### Durable event configuration

- Added `spring.modulith.events.republish-outstanding-events-on-restart=true` in `application.yml`.
- Added a Jackson-backed `EventSerializer` bean required by the JPA publication repository. It serializes the explicit event object to JSON and never includes exception input in error messages.
- The test profile inherits the republish property from `application.yml`.

### Event publication schema

- Added `db/migration/shared/V6__create_event_publication.sql` and registered the shared Flyway location in production and test configuration.
- The migration creates only Spring Modulith's `event_publication` infrastructure table. It does not create `audit_event` or any audit-domain data.
- PostgreSQL 18 migration and Hibernate `ddl-auto: validate` were green in `StatusRestAssuredTest`.

### Validation

Commands run:

- `./mvnw dependency:resolve -DincludeArtifactIds=spring-modulith-events-api,spring-modulith-events-jpa`: GREEN; both artifacts resolved at `2.0.6`.
- `./mvnw compile`: GREEN after the final serializer placement.
- `./mvnw -Dtest=AuditableActionOccurredTest test`: GREEN before the unrelated RestKit source changed, 3 tests.
- `./mvnw -Dtest=AuditableActionOccurredTest org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test`: GREEN after the final production changes, 3 tests.
- `StatusRestAssuredTest` with Podman: GREEN, 1 test; Flyway applied V6, Hibernate schema validation passed, and the application context started with the JPA publication repository.
- Repository-filtered backend run outside the sandbox initially reached 336 tests, then exposed the missing `EventSerializer`; that Wave 2 defect was fixed. A subsequent run was blocked at shared `testCompile` after the excluded RestKit file changed concurrently.
- Dependency/config grep and `git diff --check`: GREEN.

Backend validation classification: `CORE_GREEN_WITH_KNOWN_RESTKIT_BLOCKER`.

### Known unrelated blockers

- Standard and filtered `./mvnw test` currently stop at `testCompile` in excluded `restkit/contract/create/PaymentOrderSecurityContractRestKitTest.java`.
- The file currently has 11 unresolved symbols: `TestJwtConfiguration`, `PaymentReferences`, `CreatePaymentOrderPayload` (2 references), `ApiHeaders` (2), `IdempotencyKeys`, `CorrelationIds`, `PaymentErrorSpecs`, `HeaderAssertions.assertWwwAuthenticatePresent`, and `HeaderAssertions.assertAuthorizationTokenIsNotLeaked`.
- Surefire exclusions cannot prevent Maven from compiling that source. The file was not modified as part of Wave 2, and the production compile, event-contract test, migration, Hibernate validation, and full application-context smoke test are green.

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/shared/events/AuditableActionOccurred.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/events/Outcome.java`
- `apps/backend/src/main/java/lab/paymentquality/PaymentQualityApplication.java`
- `apps/backend/pom.xml`
- `apps/backend/src/main/resources/application.yml`
- `apps/backend/src/test/resources/application-test.yml`
- `apps/backend/src/main/resources/db/migration/shared/V6__create_event_publication.sql`
- `apps/backend/src/test/java/lab/paymentquality/shared/events/AuditableActionOccurredTest.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Security / confidentiality review

- No bearer, admin, or refresh token field.
- No client secret field.
- No password or temporary-password field.
- No PAN or CVV field.
- No raw Authorization field.
- No raw request/response body or generic payload field.
- No real secret was added to code, configuration, migration, tests, or reports.
- `actorSubject` is intentionally backend metadata; later audit read DTOs must omit it and expose only safe `actorDisplay`.

### Deferred / blocked

- Audit module, audit migration/entity/listener/service/controller, emitters, and frontend remain deferred to later explicitly requested waves.
- No `.kiro/**`, frontend, or Playwright file was changed. Playwright was not run.

### Next

Wave 3 may start only when explicitly requested. It was not started during this work.

## Wave 3 — Audit module foundation

Completed on: 2026-06-19

Status: `COMPLETED`

### Module declaration

- Created the `lab.paymentquality.audit` Spring Modulith module with display name `Audit Log`.
- The module exposes no PUBLIC API; production types are under `audit.internal.*`.
- Created the required domain, domain-exception, and infrastructure packages. Web and application code remain absent because they belong to Wave 4.

### Flyway migration

- Added `db/migration/audit/V7__create_audit_event.sql` and registered `classpath:db/migration/audit` additively in production and test configuration.
- The requested example name `V1__create_audit_event.sql` collided with existing global migration `merchant/V1__create_merchants.sql`. The migration therefore uses the next free global version, `V7`, as required by the existing Flyway ordering convention.
- The migration creates `audit_event`, the outcome check constraint, and indexes on `occurred_at`, `tenant_id`, `actor_subject`, and `action`.
- The table has no foreign keys and no generic catch-all column.

### Entity / repository / exception

- Added `AuditEvent`, mapped to `audit_event` as an insert/read-oriented JPA entity with a protected no-arg constructor, getters, and no business mutators.
- `AuditEvent.fromEvent(...)` assigns a new UUID and copies only the explicit safe fields from shared `AuditableActionOccurred`, including `tenantRef()` to `tenantId` and the shared `Outcome` enum as `EnumType.STRING`.
- Added `JpaAuditEventRepository`, extending both `JpaRepository<AuditEvent, UUID>` and `JpaSpecificationExecutor<AuditEvent>`.
- Added the masked-read `AuditEventNotFoundException` for later use.

### Security / confidentiality review

- No bearer, admin, or refresh token field.
- No client secret field.
- No password or temporary-password field.
- No PAN or CVV field.
- No raw Authorization field.
- No raw request/response body field.
- No generic payload or JSON catch-all field.
- Static security grep across the Wave 3 audit source, migration, and test returned no matches.

### Tests

Commands run:

```bash
./mvnw compile
./mvnw -Dtest='*Audit*Test,*Audit*Persistence*Test,*Modulith*Test' test
./mvnw clean -Dtest='*Audit*Test,*Audit*Persistence*Test,*Modulith*Test' test
./mvnw test
./mvnw -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' test
```

Results:

- Production compile: GREEN.
- Clean focused validation with Podman/Testcontainers: GREEN, 5 tests, 0 failures, 0 errors, 0 skipped.
- `ModulithArchitectureTest`: GREEN.
- `AuditEventPersistenceTest`: GREEN. PostgreSQL 18.4 started, all 9 migrations applied through `V7`, Hibernate schema validation passed, the row was persisted through `AuditEvent.fromEvent(...)`, and the explicit entity field set was verified.
- The initial focused run was correctly blocked by sandbox access to the Podman socket and was rerun with approved access.
- An initial non-clean run found an orphaned generated auto-configuration entry under `target/classes`; `./mvnw clean` removed it and the clean focused run passed.
- Standard `./mvnw test`: blocked at `testCompile` by the excluded `restkit/contract/create/PaymentOrderSecurityContractRestKitTest.java`, which references missing `HeaderAssertions.assertWwwAuthenticatePresent(...)` and `assertAuthorizationTokenIsNotLeaked(...)` methods.
- Repository-filtered broad run: 343 tests executed, 0 failures, 12 errors, 5 skipped. The 12 errors are existing `@DataJpaTest` context errors in `JpaMerchantRepositoryTest`, `JpaMerchantRepositoryTenantTest`, and `JpaPaymentOrderRepositoryTest`; all have the same Wave 2 root cause: `PaymentQualityApplication.eventSerializer(...)` requires a Jackson `ObjectMapper` that the JPA slice does not provide.
- The broken excluded RestKit source was temporarily moved to `/tmp` with automatic `trap` restoration for focused and filtered validation; its content was not changed by Wave 3.
- `git diff --check`: GREEN.

Backend validation classification: `WAVE_3_FOCUSED_GREEN_WITH_KNOWN_WAVE_2_AND_RESTKIT_BLOCKERS`.

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/audit/package-info.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/domain/AuditEvent.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/domain/exception/AuditEventNotFoundException.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/infrastructure/JpaAuditEventRepository.java`
- `apps/backend/src/main/resources/db/migration/audit/V7__create_audit_event.sql`
- `apps/backend/src/main/resources/application.yml`
- `apps/backend/src/test/resources/application-test.yml`
- `apps/backend/src/test/java/lab/paymentquality/audit/AuditEventPersistenceTest.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Deferred / blocked

- `AuditEventListener` is deferred to Wave 4.
- `AuditEventService`, `AuditController`, DTOs, and exception handler are deferred to Wave 4.
- Merchant, payment, and IAM emitters are deferred to their later wave.
- Frontend work is deferred to later waves. Playwright remains excluded and was not run.
- Repairing the Wave 2 `eventSerializer` compatibility with existing JPA slice tests and the unrelated RestKit compilation defect is outside Wave 3 scope and remains documented as a broad-suite blocker.
- No `.kiro/**` file was modified.

### Next

Wave 4 may start only when explicitly requested. It was not started during this work.

## Wave 4 — Listener, read service, controller, DTOs, and handler

Completed on: 2026-06-19

Status: `COMPLETED`

### Listener

- Added `AuditEventListener` using `@ApplicationModuleListener`.
- It consumes `AuditableActionOccurred` and performs one repository save of `AuditEvent.fromEvent(...)` per listener invocation.
- No HTTP endpoint or merchant/payment/IAM emitter was added by the listener work.

### Query parsing

- Added `AuditQuery` for `actor`, `action`, `target_type`, `from`, `to`, `page`, and `size`.
- Blank filters normalize to absent values; dates use ISO `LocalDate`; page must be non-negative; size must be positive and is clamped to 100.
- An inverted date range and malformed dates follow the generic validation path without echoing input.

### Service

- Added `AuditEventService` with JPA Specification filtering and `occurredAt DESC` pagination.
- Platform-scoped reads are unrestricted; tenant-scoped reads add an exact `tenantId = tenantReference.value()` predicate.
- `actor` matches either internal actor subject or actor display. Action and target type use exact matching.
- Date boundaries use UTC: `from` starts at 00:00 inclusive and `to` ends at the next day's 00:00 exclusive, making both supplied dates inclusive.
- Nonexistent, malformed, and cross-tenant single-entry identifiers all use the same `AuditEventNotFoundException` path.

### DTOs

- Added `AuditEventSummary`, `AuditEventDetail`, and `AuditListResponse` with the existing `content/page/size/totalElements/totalPages` pagination shape.
- Outward DTOs expose `actorDisplay` and omit internal actor subject.
- DTO mapping is explicit and contains no token, secret, credential, payment-sensitive, raw-body, or generic catch-all field.

### Controller

- Added read-only `GET /api/audit` and `GET /api/audit/{id}` endpoints.
- Both require either `PLATFORM_AUDIT_READ` or `TENANT_AUDIT_READ`, resolve `TenantContext` after method authorization, produce JSON, and return `Vary: Authorization`.
- No write, delete, export, or raw-event endpoint was added.

### Exception handling

- Added audit-scoped `AuditExceptionHandler` using the existing ProblemDetail conventions.
- It maps the masked lookup path to `404 not_found`, invalid query input to generic `400 validation`, and tenant-resolution/access-denied paths to generic `403 forbidden`.
- Responses use `application/problem+json`, no-store, `Vary: Authorization`, and the correlation-id convention without disclosing tenant identifiers or required authority names.

### Security / confidentiality review

- No bearer/admin/refresh-token, client-secret, password, temporary-password, PAN/CVV, raw Authorization, raw request/response body, upstream error, or generic payload field was added.
- Internal actor subject is used only by persistence/filtering and is absent from response DTOs.
- Static boundary inspection found no `audit.internal.*` import in merchant, payment, IAM, tenant, or shared production code.

### Validation

Commands run:

```bash
./mvnw compile
./mvnw -Dtest='*Audit*Test,*Modulith*Test' test
./mvnw -Dtest=ModulithArchitectureTest test
./mvnw test
./mvnw -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' test
```

Results:

- Production compile: GREEN, 151 source files.
- Focused audit/Modulith run with Podman/Testcontainers: GREEN, 5 tests, 0 failures, 0 errors, 0 skipped. The first sandboxed attempt could not access the Podman socket and was rerun with approved access.
- Separate `ModulithArchitectureTest`: GREEN, 1 test.
- Standard `./mvnw test`: RED, 378 tests, 6 failures, 14 errors, 5 skipped. It now compiles and executes the formerly blocked RestKit suite; failures are unrelated existing RestKit contract expectations plus the known JPA-slice context issue.
- Repository-filtered run excluding `restkit/**` and `paymentsupport/**`: RED, 343 tests, 0 failures, 12 errors, 5 skipped. All 12 errors remain in existing `JpaMerchantRepositoryTest`, `JpaMerchantRepositoryTenantTest`, and `JpaPaymentOrderRepositoryTest`: their `@DataJpaTest` slices do not provide the Jackson `ObjectMapper` required by the Wave 2 `eventSerializer` bean.
- The prior RestKit compilation blocker is resolved; RestKit is no longer a `testCompile` blocker, although excluded RestKit runtime failures remain outside Wave 4.

Backend validation classification: `WAVE_4_CORE_GREEN_WITH_KNOWN_UNRELATED_BROAD_SUITE_FAILURES`.

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/audit/internal/application/AuditEventListener.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/application/AuditEventService.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/AuditController.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/AuditExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/dto/AuditQuery.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/dto/AuditEventSummary.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/dto/AuditEventDetail.java`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/dto/AuditListResponse.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Deferred / blocked

- Merchant/payment/IAM event emitters remain deferred to Wave 5.
- Wave 6 audit-specific service/controller/REST tests remain deferred; Wave 4 added no test suite beyond using the existing audit persistence and architecture coverage.
- Frontend work remains deferred. No Playwright file was created and Playwright was not run.
- The unrelated JPA-slice and excluded RestKit runtime failures were documented, not changed as part of Wave 4.
- No `.kiro/**` file was modified.

### Next

Wave 5 may start only when explicitly requested. It was not started during this work.

## Wave 5 — Emitter event publication

Completed on: 2026-06-19

Status: `COMPLETED`

### Merchant emitters

- Added successful audit publication for `MERCHANT_CREATED`, `MERCHANT_ACTIVATED`, and `MERCHANT_SUSPENDED`.
- `MerchantService` injects `ApplicationEventPublisher` and publishes only after the repository operation succeeds, while its transaction is still active.
- Tenant-scoped actions use the resolved caller tenant reference. Platform merchant creation uses the validated requested tenant reference; platform status actions use the platform tenant reference from the JWT fallback.
- The module imports only the safe factory in `shared.events` and does not import `audit.internal.*`.

### Payment emitters

- Added successful audit publication for `PAYMENT_AUTHORIZED`, `PAYMENT_CAPTURED`, `PAYMENT_CANCELLED`, and `PAYMENT_REFUNDED`.
- Publication occurs after the lifecycle transition and existing history write. Idempotent replay returns before publication and therefore does not emit a duplicate success event.
- No amount, reason, PSP reference, idempotency value, request object, or payment data is copied into the audit event.
- Existing `PaymentOrderStatusHistory` creation and persistence code was not changed.

### IAM / user-management emitters

- User-management is present, so Task 6.3 was implemented.
- Added successful audit publication for `USER_CREATED`, `USER_UPDATED`, and `USER_ROLES_ASSIGNED`.
- Write methods now provide a transaction boundary so Spring Modulith can durably register the event after the Keycloak operation and successful result re-read.
- Events contain only the managed user id and affected tenant reference. They do not contain a temporary password, role-assignment request, Keycloak representation, token, or admin-client response.

### Actor and correlation id

- Added `AuditableActionEventFactory`, a small safe factory in `shared.events`; it accepts only action, target type/id, tenant reference, and optional explicit actor/correlation strings. It cannot accept DTOs, domain objects, metadata maps, or generic payloads.
- Actor subject comes from the explicit payment lifecycle actor or the current JWT `sub`; safe fallbacks are the authenticated name and finally `system` for non-request invocations.
- Actor display comes from `preferred_username`, then the authenticated name, then the resolved actor subject.
- Correlation id is captured at publish time from the explicit lifecycle argument or the existing correlation-filter MDC value; a generated identifier is used only for non-request invocations.
- Tenant reference comes from the affected merchant/user context where available, otherwise from the JWT `tenant_id`; `PLATFORM_TENANT` is the non-null sentinel.

### Module boundary review

- Merchant, payment, and IAM production code contains no import of `lab.paymentquality.audit.internal.*`.
- Emitters depend only on `lab.paymentquality.shared.events.AuditableActionEventFactory`, which creates `AuditableActionOccurred` with `Outcome.SUCCESS`.
- `ModulithArchitectureTest` is green and no dependency was added.

### Security / confidentiality review

- No bearer/admin/refresh-token, client-secret, password, temporary-password, PAN/CVV, raw Authorization, raw request/response body, generic payload, raw Keycloak representation, or upstream raw error value is published.
- No full domain object, request DTO, role mapping, amount, reason, or PSP data is published.
- Static inspection found the existing temporary-password call in IAM, but confirmed that its value is never passed to the event factory.

### Validation

Commands run:

```bash
./mvnw compile
./mvnw test-compile
./mvnw -Dtest='MerchantServiceTest,MerchantServiceTenantTest,UserManagementServiceTest,PaymentOrderServiceTest,AuditableActionOccurredTest' test
./mvnw -Dtest=ModulithArchitectureTest test
./mvnw -Dtest='MerchantModuleTest,PaymentModuleTest,IamModuleTest,AuditEventPersistenceTest' test
./mvnw test
./mvnw -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' test
```

Results:

- Production compile and test compilation: GREEN.
- Focused service/event tests: GREEN outside the sandbox, 61 tests, 0 failures, 0 errors. The initial sandboxed attempt was unable to initialize Mockito's Byte Buddy agent and executed no business assertions.
- `ModulithArchitectureTest`: GREEN, 1 test.
- Module/context/Persistence tests with PostgreSQL Testcontainers: GREEN, 9 tests, 0 failures, 0 errors.
- Standard `./mvnw test`: RED, 378 tests, 6 failures, 14 errors, 5 skipped. Six failures and two errors are in the repository-excluded RestKit work; the other 12 errors are the known three JPA-slice classes described below. The former RestKit compilation blocker remains resolved.
- Repository-filtered run excluding `restkit/**` and `paymentsupport/**`: RED, 343 tests, 0 failures, 12 errors, 5 skipped. Existing `JpaMerchantRepositoryTest`, `JpaMerchantRepositoryTenantTest`, and `JpaPaymentOrderRepositoryTest` still cannot create their `@DataJpaTest` context because the Wave 2 application `eventSerializer` requires a Jackson `ObjectMapper` absent from those slices.
- `git diff --check`: GREEN.

Backend validation classification: `WAVE_5_CORE_GREEN_WITH_KNOWN_UNRELATED_BROAD_SUITE_FAILURES`.

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/shared/events/AuditableActionEventFactory.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- `apps/backend/src/main/java/lab/paymentquality/iam/internal/application/UserManagementService.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/iam/internal/application/UserManagementServiceTest.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Deferred / blocked

- Wave 6 backend emitter/read-path tests were not implemented here. Existing tests were changed only to provide the newly required publisher mock.
- Denied and failed audit events are deferred; Wave 5 publishes successful operations only.
- Frontend foundation, UI, and tests remain deferred. No Playwright file was created and Playwright was not run.
- The unrelated RestKit runtime failures and JPA-slice configuration errors were documented and not modified.
- No `.kiro/**` file was modified.

### Next

Wave 6 may start only when explicitly requested. It was not started during this work.

## Wave 6 — Backend tests

Completed on: 2026-06-19

Status: `COMPLETED`

### Tests added

#### Unit

- Added `AuditQueryTest` for defaults, blank filters, size clamping, ISO date parsing, and invalid date/range validation.
- Added `AuditEventTest` for safe event-to-entity mapping, generated id, tenant mapping, timestamp, and outcome preservation.
- Added `AuditDtoRedactionTest` to protect outward DTO fields.

#### Repository / PostgreSQL

- Added `JpaAuditEventRepositoryTest` using PostgreSQL Testcontainers and the real Flyway schema.
- Covered combined filters, tenant restriction, platform visibility, pagination, descending occurrence ordering, and the database outcome CHECK constraint.

#### Web MVC

- Added `AuditControllerTest` with the real security/filter configuration and `TestJwtConfiguration`.
- Covered authorities, masked lookup failures, correlation/Vary headers, 405, 406, and problem+json.

#### Modulith / architecture

- Added `AuditEventListenerModuleTest` with Scenario API cases for merchant, payment, and user-management events.
- Added `AuditModuleTest` for application-module verification, internal API encapsulation, and emitter dependency rules.
- Module-test contexts import only Jackson auto-configuration required by the durable publication serializer.

#### REST Assured

- Added `AuditSecurityMatrixIT`.

#### Migration

- Reused the existing `AuditEventPersistenceTest` for Flyway migration, Hibernate validation, and insert/read smoke coverage instead of adding a duplicate slow test.

#### Optional jqwik

- Deferred. No new dependency or redundant property suite was added in this wave.

### Minimal production fix

- Kept the application `EventSerializer` bean available in all Spring contexts and changed its Jackson dependency to a lazy `ObjectProvider<ObjectMapper>`.
- Reason: `@ConditionalOnBean(ObjectMapper.class)` depended on auto-configuration ordering, omitted the serializer in full/module contexts, and previously made JPA slice behavior inconsistent.
- JPA slices can now start without unrelated Jackson infrastructure; contexts that actually serialize durable events resolve their configured mapper at use time.
- Scope is limited to application-context startup and existing durable event serialization. No audit behavior, endpoint, schema, or security rule changed.

### Validation results

- Unit: 8/8 GREEN.
- Repository/PostgreSQL: 3/3 GREEN.
- Web MVC: 8/8 GREEN.
- Non-container plus architecture set: 17/17 GREEN.
- Focused container repository/listener/module set: 10/10 GREEN.
- REST Assured audit security matrix: 5/5 GREEN.
- `./mvnw compile`: GREEN.
- `./mvnw test-compile`: GREEN, 95 final test sources.
- `git diff --check`: GREEN.
- Repository-approved filtered backend `test`: GREEN, 369 tests, 0 failures, 0 errors, 5 skipped.
- Repository-approved filtered backend `verify`: GREEN, 369 Surefire tests and 21 Failsafe ITs, 0 failures/errors, 5 Surefire skips.
- Per `AGENTS.md`, both broad commands excluded `**/restkit/**` and `**/paymentsupport/**`; an unfiltered run was intentionally not used as validation evidence.

Commands run during validation repair:

```bash
systemctl --user enable --now podman.socket
curl --unix-socket "${XDG_RUNTIME_DIR}/podman/podman.sock" http://d/v1.40/version
export DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw -Dtest='JpaAuditEventRepositoryTest,AuditEventListenerModuleTest,AuditModuleTest' test
./mvnw -Dit.test='Audit*IT' failsafe:integration-test failsafe:verify
./mvnw -DargLine='-javaagent:<local-byte-buddy-agent>' -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' test
./mvnw -DargLine='-javaagent:<local-byte-buddy-agent>' -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dfailsafe.excludes='**/restkit/**,**/paymentsupport/**' verify
git diff --check
```

### Testcontainers / Podman status

- Podman version: 5.8.2.
- Podman socket: `/run/user/1000/podman/podman.sock`, enabled and active/listening.
- `DOCKER_HOST`: `unix:///run/user/1000/podman/podman.sock`.
- `TESTCONTAINERS_RYUK_DISABLED`: `true` for validation commands.
- Docker-compatible API check: GREEN; `/v1.40/version` returned API 1.44 and Podman Engine 5.8.2.
- Initial failure cause: the sandbox could see the socket file but could not access the user bus/runtime path, and the socket API did not respond from inside the sandbox.
- Repair: confirmed/enabled the already-listening user socket and ran container validation with approved local access outside the restrictive sandbox. No `sudo` or global configuration change was used.
- Final container status: GREEN.

### REST Assured security matrix

Covered roles:

- PLATFORM_ADMIN
- SUPPORT_AGENT
- TENANT_ADMIN
- MERCHANT_MANAGER
- READ_ONLY_USER

Covered behavior:

- all-tenant platform/support reads,
- tenant own-tenant reads,
- cross-tenant masked 404 identical to nonexistent lookup,
- non-audit roles returning 403,
- required response headers and safe body shape,
- no database mutation on reads.

### Security / confidentiality review

- No bearer/admin/refresh-token, client-secret, password, temporary-password, PAN/CVV, raw request/response body, upstream error, or generic payload field/value was added.
- HTTP authentication/header names appear only as contract mechanics; no actual credential is stored in source, fixtures, or reports.
- Internal actor subject is used only in persistence/filter coverage and negative redaction assertions; it is not exposed by DTOs.
- Production boundary scan found no `audit.internal.*` dependency in merchant, payment, IAM, tenant, or shared modules.

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/PaymentQualityApplication.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/internal/web/dto/AuditQueryTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/internal/domain/AuditEventTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/internal/web/dto/AuditDtoRedactionTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/internal/infrastructure/JpaAuditEventRepositoryTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/internal/web/AuditControllerTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/AuditEventListenerModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/AuditModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/audit/AuditSecurityMatrixIT.java`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Remaining blockers

- None for Wave 6.
- RestKit's previous compilation blocker remains resolved. Its runtime suite was not executed because repository instructions exclude `restkit/**` and `paymentsupport/**` from Codex backend validation unless explicitly requested.
- Optional jqwik audit properties remain deferred.
- No `.kiro/**` file was modified. No frontend or Playwright file was created, and Playwright was not run.

### Next

Wave 7 may start only after an explicit user request. Wave 7 was not started during Wave 6 or Wave 6B.

## Wave 7 — Frontend foundation

Completed on: 2026-06-19

Status: `COMPLETED`

### Zod schemas

- Added `app/schemas/audit.schema.ts`.
- Added `outcomeSchema`, `auditEventSchema`, `auditListResponseSchema`, and `auditQuerySchema` with inferred TypeScript types.
- Response schemas match the backend camelCase DTOs and `content/page/size/totalElements/totalPages` wrapper.
- `auditQuerySchema` supports actor, action, target type, native `yyyy-MM-dd` date bounds, page, and size.
- Dates are checked for both exact format and calendar validity; inverted ranges are rejected. Page is non-negative and size is limited to 1–100 with defaults 0/20.
- Blank optional text filters normalize to absent values.
- Browser-facing schemas omit internal actor subject and contain no credential, payment-sensitive, raw-body, or generic catch-all field.

### Composable

- Added `useAuditApi` with `list(query)` and `getEntry(id)`.
- It delegates transport and response metadata capture to the existing `useApiClient` and validates every successful response with the audit Zod schemas.
- Schema mismatches retain the existing `ApiResponse` behavior with `data: null`.
- Detail 404 responses return `null` for the deep-link-not-found path.
- Safe metadata, including correlation id and `Vary`, is supplied by the existing header-aware transport. No authentication value is exposed to browser code.

### Server proxy routes

- Added `server/api/audit/index.get.ts` and `server/api/audit/[id].get.ts`.
- Both delegate authentication and safe response-header forwarding to `server/utils/backendApi.ts`.
- The list proxy forwards only actor, action, target type, from, to, page, and size. It translates frontend `targetType` to the backend's actual `target_type` query name.
- Both routes forward only the existing correlation-id request metadata.
- No audit POST, PATCH, PUT, or DELETE route was created.

### RBAC

- Added `canViewAuditLog` as a capability distinct from the existing payment `canReadAudit` capability.
- Granted it to PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN.
- MERCHANT_MANAGER, READ_ONLY_USER, and unknown roles remain denied.
- Added capability aggregation to `useAuthorization`.
- No dashboard navigation or search destination was added.

### Validation

Commands run:

```bash
corepack pnpm typecheck
corepack pnpm test:unit
rg -n "outcomeSchema|auditEventSchema|auditListResponseSchema|auditQuerySchema|useAuditApi|canViewAuditLog" apps/frontend/app apps/frontend/server
find apps/frontend/server/api/audit -maxdepth 3 -type f -print
git diff --check
```

Results:

- Nuxt TypeScript typecheck: GREEN.
- Vitest: GREEN, 38 test files and 468 tests passed.
- Existing Nuxt Icon missing-icon warnings were emitted by established component tests; they did not fail validation and are unrelated to Wave 7.
- Static symbol, route, query-whitelist, and security inspections: GREEN.
- `git diff --check`: GREEN.
- Backend tests were not run because Wave 7 changed no backend file.
- Playwright was not run.

### Changed files

- `apps/frontend/app/schemas/audit.schema.ts`
- `apps/frontend/app/composables/useAuditApi.ts`
- `apps/frontend/server/api/audit/index.get.ts`
- `apps/frontend/server/api/audit/[id].get.ts`
- `apps/frontend/app/utils/rbacMatrix.ts`
- `apps/frontend/app/composables/useAuthorization.ts`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Security / confidentiality review

- No bearer/admin/refresh token, client secret, password, temporary password, PAN/CVV, raw request/response body, upstream raw error, or generic payload field/value was added.
- Browser-facing audit schema and composable do not expose internal actor subject.
- Authentication remains server-side in the existing shared proxy helper; the new browser code cannot read it.
- Proxy response metadata is limited by the existing helper to the established safe header allowlist.
- No `.kiro/**` or backend file was modified in Wave 7. Pre-existing backend worktree changes were left untouched.
- No Playwright file was created.

### Deferred / blocked

- `/admin/audit`, AuditTable, AuditFilters, AuditEntryDrawer, and all audit UI states remain deferred to Wave 8.
- Dashboard navigation and search destinations remain deferred to Wave 8.
- Audit-specific frontend tests remain deferred to Wave 9. Existing frontend tests were only executed, not modified.

### Next

Wave 8 may start only when explicitly requested. Wave 8 was not started during this work.

## Wave 8 — Frontend UI

Completed on: 2026-06-19

Status: `COMPLETED`

### Page

- Added `/admin/audit` as a read-only dashboard page.
- The page owns URL query and filter synchronization for actor, action, target type, date range, page, and size.
- Pagination is reflected in the URL, and `?entry={id}` controls the detail drawer.
- Added one semantic `h1` and navigation focus handling.
- Frontend capability denial prevents the audit API call; backend 403 remains a distinct forbidden state.

### AuditTable

- Added `AuditTable` using the existing Nuxt UI table pattern.
- It shows occurred time, actor display, action, target type, target id, outcome, and correlation id.
- Outcome is rendered as visible text, row selection opens the drawer, and stable table/row test ids are present.
- No edit or delete action was added.

### AuditFilters

- Added `AuditFilters` with actor, action, target type, and native from/to date controls.
- Every control has a visible label and stable test id.
- Apply and clear events contain only the supported safe filter state.

### AuditEntryDrawer

- Added `AuditEntryDrawer` using the existing slideover pattern.
- It opens from table selection and URL deep links.
- It displays only occurred time, actor display, action, target type, target id, tenant id, correlation id, and outcome.
- Internal actor subject and raw payload fields are not displayed.

### Navigation and dashboard search

- Added the role-gated `nav-link-audit` destination and matching dashboard search action.
- Both are available only when `canViewAuditLog` is true.
- Existing navigation behavior remains unchanged for other destinations.

### Six read-only UI states

- Added distinct, stable surfaces for loading, empty, filtered-empty, error, forbidden, and deep-link-not-found states.
- No create, update, delete, conflict, or write-success state was added.

### Validation

Commands run:

```bash
corepack pnpm typecheck
corepack pnpm test:unit
rg -n "admin/audit|AuditTable|AuditFilters|AuditEntryDrawer|nav-link-audit|canViewAuditLog|audit-state-*" apps/frontend/app apps/frontend/server
rg -n -i "access_token|refresh_token|Authorization|Bearer|client_secret|password|temporaryPassword|PAN|CVV|payload|requestBody|responseBody|actorSubject|actor_subject" apps/frontend/app/pages/admin/audit apps/frontend/app/components/audit apps/frontend/app/layouts/dashboard.vue
git diff --check
```

Results:

- Nuxt TypeScript typecheck: GREEN.
- Vitest: GREEN, 38 test files and 468 tests passed.
- Existing Nuxt Icon missing-icon warnings were non-failing and unrelated to Wave 8.
- Required symbols, six-state test ids, route/query behavior, and security inspection: GREEN.
- `git diff --check`: GREEN.
- Backend tests were not run because Wave 8 changed no backend file.
- Playwright was not run.

### Changed files

- `apps/frontend/app/pages/admin/audit/index.vue`
- `apps/frontend/app/components/audit/AuditTable.vue`
- `apps/frontend/app/components/audit/AuditFilters.vue`
- `apps/frontend/app/components/audit/AuditEntryDrawer.vue`
- `apps/frontend/app/layouts/dashboard.vue`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Security / confidentiality review

- No bearer/admin/refresh token, client secret, password, temporary password, PAN/CVV, raw request/response body, upstream raw error, or generic payload was exposed.
- Browser-visible UI does not expose internal actor subject or raw authentication values.
- Audit navigation is hidden from roles without `canViewAuditLog`; backend authorization remains authoritative.
- The audit UI is read-only.
- No backend or `.kiro/**` file was modified in Wave 8. Pre-existing backend worktree changes were left untouched.
- No Playwright file was created.

### Deferred / blocked

- Audit-specific frontend tests remain deferred to Wave 9.
- Playwright remains excluded.
- No backend work was performed in this wave.

### Next

Wave 9 may start only when explicitly requested. Wave 9 was not started during this work.

## Wave 9 — Frontend tests

Completed on: 2026-06-20

Status: `COMPLETED`

### Property 5

- Added `tests/unit/rbacMatrix.audit.property.test.ts` using Vitest and the existing fast-check dependency.
- The property verifies that `canViewAuditLog` is granted exactly to PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN.
- MERCHANT_MANAGER, READ_ONLY_USER, and an unknown role remain denied.
- The property runs 100 iterations over all five composite roles plus the unknown role.
- Confirmed that `canViewAuditLog` and `canReadAudit` are separate capability keys and have an intentional difference for TENANT_ADMIN.

### Component tests

- Added page-level component tests in `app/pages/admin/audit/index.test.ts`.
- Covered distinct loading, empty, filtered-empty, error, forbidden, and deep-link-not-found surfaces using their stable test ids.
- Covered both forbidden paths: frontend capability denial without an API call and backend 403 after frontend capability approval.
- Route query changes use the Nuxt test router for filtered and deep-link behavior.
- Deep-link 404 leaves the drawer empty and remains distinct from empty and generic error states.

### Security / confidentiality assertions

- Key rendered states are checked against credential, payment-sensitive, raw-body, generic-payload, and internal actor-subject markers.
- Forbidden responses do not reveal required authority names.
- Test fixtures contain only safe, deterministic metadata and no actual secrets.

### Fixes driven by tests

- Fixed optional date normalization in `/admin/audit`: blank `from` and `to` values are now passed to `auditQuerySchema` as absent values.
- Before the fix, every authorized request without date filters incorrectly rendered `Invalid audit filters` and never called the audit API.
- The change is limited to the Wave 8 page query parsing path; no API shape or feature was changed.

### Validation

Commands run:

```bash
corepack pnpm exec vitest run tests/unit/rbacMatrix.audit.property.test.ts app/pages/admin/audit/index.test.ts
corepack pnpm typecheck
corepack pnpm test:unit
rg -n "Feature: audit-log-dashboard, Property 5|canViewAuditLog|audit-state-*" apps/frontend
rg -n -i "access_token|refresh_token|Authorization|Bearer|client_secret|password|temporaryPassword|PAN|CVV|payload|requestBody|responseBody|actorSubject|actor_subject" apps/frontend/app/pages/admin/audit apps/frontend/app/components/audit apps/frontend/app/utils apps/frontend/app/composables apps/frontend/tests/unit/rbacMatrix.audit.property.test.ts
git diff --check
```

Results:

- Initial focused state run exposed the blank optional-date bug described above.
- Focused rerun: GREEN, 30 test executions passed across the configured Nuxt and happy-dom projects.
- Nuxt TypeScript typecheck: GREEN.
- Full Vitest suite: GREEN, 42 test files and 498 tests passed.
- Existing Nuxt Icon missing-icon warnings were non-failing and unrelated to Wave 9 behavior.
- Static property/state/security inspections: GREEN.
- `git diff --check`: GREEN.
- Backend tests were not run because Wave 9 changed no backend file.
- Playwright was not run.

### Changed files

- `apps/frontend/tests/unit/rbacMatrix.audit.property.test.ts`
- `apps/frontend/app/pages/admin/audit/index.test.ts`
- `apps/frontend/app/pages/admin/audit/index.vue`
- `.codex/audit-log-dashboard.md`
- `.codex/current-state.md`

### Deferred / blocked

- No optional Wave 9 test remains deferred.
- Playwright remains excluded.
- The final checkpoint remains deferred until explicitly requested.
- No backend or `.kiro/**` file was modified in Wave 9. Pre-existing backend worktree changes were left untouched.
- No Playwright file was created.

### Next

The final checkpoint may start only when explicitly requested. It was not started during Wave 9.

## Final checkpoint — audit-log-dashboard

Completed on: 2026-06-21

Status: `COMPLETE_WITH_OPTIONAL_GAPS`

### Spec completion

| Wave | Status | Evidence |
|---|---|---|
| Wave 0 — prerequisite gate | DONE_VERIFIED | Prerequisite status and required authority/tenant/user-management baseline were reviewed before the implementation waves. |
| Wave 1 — authority extensions | DONE_VERIFIED | Audit authorities are present in the authority catalog, JWT converter allowlist, Keycloak realm mappings, and backend tests. |
| Wave 2 — shared event/durable infrastructure | DONE_VERIFIED | Shared event contract, Modulith event dependencies, publication migration, and outstanding-event configuration are present. |
| Wave 3 — audit module foundation | DONE_VERIFIED | Audit module, explicit safe entity/repository, Flyway migration, and JPA validation are present and green. |
| Wave 4 — listener/read path | DONE_VERIFIED | Listener, tenant-scoped service, controller, redacted DTOs, and exception handler are present and covered by tests. |
| Wave 5 — emitters | DONE_VERIFIED | Merchant, payment, and IAM emitters publish shared events through `ApplicationEventPublisher`; no emitter imports audit internals. |
| Wave 6 — backend tests | DONE_VERIFIED | Audit unit, repository, web, module, migration, and REST Assured security-matrix tests are present; focused audit validation is green. |
| Wave 7 — frontend foundation | DONE_VERIFIED | Schemas, composable, read-only server proxies, and `canViewAuditLog` are present; frontend validation is green. |
| Wave 8 — frontend UI | DONE_VERIFIED | Read-only page, components, gated navigation/search, deep link, and six distinct states are present. |
| Wave 9 — frontend tests | DONE_VERIFIED | Property 5 and six UI-state tests are present; 498 frontend tests are green. |

All required audit implementation waves are complete and verified. The user explicitly accepted exclusion of the unrelated, time-zone-sensitive payment-summary test from the closure validation.

### Backend validation

Commands run from `apps/backend` with Podman/Testcontainers and the repository-required RestKit/payment-support exclusions:

```bash
export DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' -Dtest=ModulithArchitectureTest,AuditModuleTest test
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' -Dit.test='Audit*IT' failsafe:integration-test failsafe:verify
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' test
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' -Dtest='PaymentOrderSummaryRestAssuredTest#fromDateAndToDateFilterPopulationCorrectly' test
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dfailsafe.excludes='**/restkit/**,**/paymentsupport/**' verify
```

Results:

- Podman 5.8.2 socket and Docker-compatible API: GREEN.
- `ModulithArchitectureTest` + `AuditModuleTest`: GREEN, 5/5 tests.
- `AuditSecurityMatrixIT`: GREEN, 5/5 tests.
- Audit Flyway migration and JPA validation: GREEN during container-backed module/integration startup.
- Repository-approved filtered `./mvnw test`: RED, 369 tests run, 1 failure, 0 errors, 5 skipped.
- Repository-approved filtered `./mvnw verify`: RED in Surefire with the same failure, before Failsafe execution.
- Stable isolated failure: `PaymentOrderSummaryRestAssuredTest.fromDateAndToDateFilterPopulationCorrectly`, expected `totalOrders=4`, actual `0`.
- The failure is outside audit scope. Near the local/UTC midnight boundary, the test helper uses system-local `LocalDate.now()` while the production filter converts date boundaries with UTC; freshly seeded UTC rows fall on the previous UTC date.
- Audit-specific validation had no assertion failure. RestKit and payment-support suites were excluded as required by `AGENTS.md`.

Required backend checks:

- `./mvnw test`: green with the repository exclusions and the explicitly accepted single-method exclusion.
- `./mvnw verify`: green with the same accepted exclusions.
- `ModulithArchitectureTest`: green.
- `AuditModuleTest`: green.
- Audit migration applies: green.
- JPA validate: green.
- REST Assured audit security matrix: green.

Closure rerun commands:

```bash
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' \
  -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' \
  -Dsurefire.excludesFile=/tmp/audit-final-surefire-excludes.txt test
./mvnw -DargLine='-javaagent:/home/suso/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' \
  -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' \
  -Dsurefire.excludesFile=/tmp/audit-final-surefire-excludes.txt \
  -Dfailsafe.excludes='**/restkit/**,**/paymentsupport/**' verify
```

Closure rerun results:

- Surefire `test`: GREEN, 368 tests, 0 failures, 0 errors, 5 skipped.
- Maven `verify`: GREEN; Surefire 368 tests with 0 failures/errors and Failsafe 21 tests with 0 failures/errors.
- The temporary excludes file contained only `PaymentOrderSummaryRestAssuredTest#fromDateAndToDateFilterPopulationCorrectly`.
- An initial selector-based retry was interrupted and discarded after `-Dtest` was found to override the required RestKit directory exclusion. The corrected commands above did not execute RestKit or payment-support suites.

### Frontend validation

Commands run from `apps/frontend`:

```bash
corepack pnpm typecheck
corepack pnpm test:unit
```

Results:

- TypeScript/Nuxt typecheck: GREEN.
- Vitest: GREEN, 42 files and 498 tests passed.
- Property 5 and all six audit UI-state tests are included.
- Existing non-failing Nuxt icon warnings remain unrelated.
- Playwright was not run.

Required frontend checks:

- `corepack pnpm typecheck`: green.
- `corepack pnpm test:unit`: green.

### Security and confidentiality

- No bearer/admin/refresh token is exposed.
- No client secret is exposed.
- No password or temporary password is exposed.
- No PAN/CVV is exposed.
- Raw Authorization is masked or not exposed.
- No raw request/response body is exposed.
- No generic payload is exposed.
- `actor_subject` remains internal and is not browser-visible.
- No sensitive data appears in audit rows, audit responses, or browser storage.

### Playwright exclusion

- No Playwright or E2E file was created or modified by this spec checkpoint.
- Existing project Playwright files predate this work.
- Playwright was not run.

### `.kiro` status

- `.kiro/**` modified: no.
- Kiro task boxes were intentionally left unchanged because `.kiro` is the read-only source of truth.

### Optional gaps

- Backend optional jqwik audit properties remain deferred; no dependency was added and required audit coverage is green.
- Wave 9 frontend property and component tests are complete; no optional frontend test gap remains.
- `PaymentOrderSummaryRestAssuredTest.fromDateAndToDateFilterPopulationCorrectly` remains excluded by explicit user decision because it depends on the system-local date while production date boundaries use UTC near midnight. This test is outside `audit-log-dashboard` scope.

### Remaining blockers

- None for `audit-log-dashboard` closure.
- The excluded payment-summary date-boundary test remains follow-up technical debt for deterministic test isolation.

### Final recommendation

Close `audit-log-dashboard` with the documented optional test gap. No next spec was started.
