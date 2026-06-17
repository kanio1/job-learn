# Tenant Model and Isolation — Codex Continuation Plan

## Execution Mode

Codex continues from the current code state, not from unchecked boxes in `.kiro/tasks.md`.

- Read `.kiro/specs/tenant-model-and-isolation/requirements.md` for product behavior.
- Read `.kiro/specs/tenant-model-and-isolation/design.md` for architecture decisions.
- Read `.kiro/specs/tenant-model-and-isolation/tasks.md` for wave ordering.
- Do not edit `.kiro/**` while implementing.
- Update `.codex/current-state.md` with execution evidence after each wave.

## Active Wave: Wave 2 — Merchant Repository, Service, Request, and New Exceptions

Wave 2 must keep all existing REST contracts, status codes, headers, and tests green. It must not change `payment` module source files and must not add frontend or Playwright files.

### Task 5.1 — Extend `JpaMerchantRepository`

File:

```text
apps/backend/src/main/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepository.java
```

Add:

```java
Optional<Merchant> findByMerchantIdAndTenantId(UUID merchantId, UUID tenantId);
List<Merchant> findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(UUID tenantId, Pageable pageable);
```

Use `Pageable`, not necessarily `PageRequest`, to stay consistent with the existing method style.

### Task 5.2 — Add Merchant Tenant Exceptions

Create under:

```text
apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/
```

Files:

```text
TenantBoundaryViolationException.java
MissingTenantReferenceException.java
UnresolvableTenantReferenceException.java
```

Behavior:

- `TenantBoundaryViolationException` -> later mapped to `403 forbidden` with generic detail.
- `MissingTenantReferenceException` -> later mapped to `400 validation`.
- `UnresolvableTenantReferenceException` -> later mapped to `400 validation`.

Do not place these in the tenant module; they describe merchant-request failures and merchant-boundary enforcement.

### Task 5.3 — Extend `CreateMerchantRequest`

File:

```text
apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java
```

Add an optional `String tenantReference` record component.

Rules:

- Do not add `@NotBlank` to `tenantReference`.
- Keep existing validation on `merchantReference` and `displayName` unchanged.
- `tenantReference` is required only for platform-scoped principals, which is service/controller behavior, not Bean Validation behavior.

### Task 5.4 — Extend `MerchantService`

File:

```text
apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java
```

Required changes:

- Remove the Wave 1 `JdbcTemplate` placeholder-tenant bridge from the create path.
- Accept `TenantContext` and `requestedTenantRef` in create flow.
- Tenant-scoped create: ignore request body `tenantReference`; assign `tenantContext.tenantId()`.
- Platform-scoped create: require non-blank `tenantReference`; resolve it to a real tenant UUID; throw `MissingTenantReferenceException` or `UnresolvableTenantReferenceException` when invalid.
- Tenant-scoped `findById`: use `findByMerchantIdAndTenantId(...)` and return masked `MerchantNotFoundException` on mismatch.
- Platform-scoped `findById`: keep existing cross-tenant behavior.
- Tenant-scoped list: return only own tenant merchants.
- Platform-scoped list: support optional tenant filter UUID; no filter returns all tenants.
- `activate(...)` / `suspend(...)`: enforce tenant boundary for tenant-scoped callers and throw `TenantBoundaryViolationException` for cross-tenant writes.

### Public Tenant API Gap

Do not import any `lab.paymentquality.tenant.internal.*` type into the merchant module.

If resolving body `tenantReference` requires a new public tenant API, implement the smallest extension in the tenant root package. Example acceptable shape:

```java
UUID resolveTenantId(TenantReference tenantReference);
```

or:

```java
TenantContext resolve(TenantReference tenantReference);
```

Keep existing `TenantResolver.resolve(Jwt)` unchanged. The new API should be implemented in `TenantResolverService`, backed by `JpaTenantRepository`, and should avoid leaking tenant internals to the merchant module.

## Wave 2 Stop Condition

After implementation, run from `apps/backend`:

```bash
./mvnw test
```

If green:

- Update `.codex/current-state.md` to mark Wave 2 complete.
- Record any intentional public tenant API extension.
- Stop before Wave 3 unless the user explicitly asks Codex to continue.

If failing:

- Fix compile/test regressions inside Wave 2 scope.
- Do not weaken existing tests.
- Do not edit `.kiro` task files to mark progress.

## Preview: Wave 3 — Controller and Exception Handler

Only start after Wave 2 is green.

Expected controller changes:

- Inject `TenantResolver` into `MerchantController`.
- Add `@AuthenticationPrincipal Jwt jwt` to protected merchant endpoints.
- Resolve `TenantContext` once per request after `@PreAuthorize` passes.
- Pass `TenantContext` to tenant-aware service methods.
- Add optional `?tenantId=` filter handling for platform-scoped list requests.
- Preserve all existing `@PreAuthorize` annotations and authority strings.

Expected exception-handler changes:

- Map `TenantResolutionException` to `403 forbidden`.
- Map `TenantBoundaryViolationException` to `403 forbidden`.
- Map `MissingTenantReferenceException` to `400 validation`.
- Map `UnresolvableTenantReferenceException` to `400 validation`.
- Ensure tenant-related `403` responses do not disclose `tenant_id`, `tenant_reference`, foreign tenant names, or whether a foreign merchant exists.
