# IAM Roles and Keycloak Login — Codex Execution Notes

## Step 0 Gate Review

Completed on 2026-06-17.

Gate status: READY for `user-management` Wave 0.

Scope:

- Reviewed Spec #1 `iam-roles-and-keycloak-login` as a prerequisite gate for Spec #3.
- Did not start `user-management`.
- Did not modify `.kiro/**`.
- Did not create or modify Playwright files.
- Did not modify backend Java source, REST contracts, backend authorities, SecurityFilterChain rules, or merchant/payment business logic.

## Verified As Already Done

- Keycloak realm contains the five composite roles:
  - `PLATFORM_ADMIN`
  - `TENANT_ADMIN`
  - `MERCHANT_MANAGER`
  - `SUPPORT_AGENT`
  - `READ_ONLY_USER`
- Keycloak realm contains `tenant-id-mapper` for the `tenant_id` claim.
- Keycloak realm contains deterministic users:
  - `platform.admin`
  - `tenant.admin`
  - `merchant.manager`
  - `support.agent`
  - `readonly.user`
- `apps/frontend/app/utils/rbacMatrix.ts` exists and is the frontend RBAC source of truth.
- `apps/frontend/app/composables/useAuthorization.ts` exists and derives capability booleans from `rbacMatrix`.
- `apps/frontend/server/routes/auth/keycloak.get.ts` derives safe browser-visible session fields and keeps the access token in `secure`.
- `apps/frontend/app/composables/useAuthError.ts` exists for 401-vs-403 reaction handling.
- `apps/frontend/app/pages/forbidden.vue` exists with the forbidden surface.
- `apps/frontend/app/pages/login.vue` contains the authentication-required surface and stable login test ids.
- Dashboard navigation is role-aware through `useAuthorization`.
- `ApiDebugPanel` and `HeaderKeyValuePanel` mask `Authorization` as `Bearer ••••••••`.
- Existing `HeaderKeyValuePanel.test.ts` property coverage verifies Authorization masking.

## Implemented In This Run

- Made the Overview page role-aware:
  - merchant summary content is shown only with `canReadMerchants`;
  - payment summary/recent-order content is shown only with payment-read capability and an available merchant scope;
  - unauthorized overview sections do not trigger backend fetches.
- Gated merchant action controls:
  - create merchant entry point requires `canCreateMerchant`;
  - create form defensively disables submit without create capability;
  - activate/suspend table actions require `canUpdateMerchantStatus`;
  - added/kept stable action test ids, preserving the existing `activate-merchant-button` id.
- Gated payment action controls:
  - create payment order submit requires `canCreatePaymentOrder`;
  - lifecycle action buttons require `canRunLifecycle`;
  - existing lifecycle test ids are preserved and spec-level `action-lifecycle-*` wrappers were added.
- Fixed small frontend typecheck blockers discovered by the checkpoint:
  - corrected the RBAC import alias;
  - decoded the OIDC access-token payload without Node `Buffer` typings;
  - tightened existing test/component types needed for `nuxt typecheck`;
  - adjusted the lifecycle component test fixture to exercise status-gating under an explicit lifecycle-capable context.

Changed files:

- `apps/frontend/app/components/merchant/CreateMerchantForm.vue`
- `apps/frontend/app/components/merchant/MerchantTable.vue`
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`
- `apps/frontend/app/components/shared/PaymentOrderLifecycleActions.vue`
- `apps/frontend/app/components/shared/HttpStatusBadge.test.ts`
- `apps/frontend/app/components/shared/state-and-lifecycle.test.ts`
- `apps/frontend/app/composables/useAuthorization.ts`
- `apps/frontend/app/layouts/dashboard.vue`
- `apps/frontend/app/pages/admin/merchants/index.vue`
- `apps/frontend/app/pages/index.vue`
- `apps/frontend/app/stores/payment-orders-if-match.test.ts`
- `apps/frontend/server/routes/auth/keycloak.get.ts`
- `.codex/iam-roles-and-keycloak-login.md`

## Optional Tasks Deferred

- Spec #1 optional property/component tests marked with `*` remain deferred unless explicitly requested.
- No new realm-shape, session-derivation, auth-error, role-rendering, forwarded-header, or auth-surface component tests were added.
- No Playwright files were created.

## Validation Evidence

Frontend:

- `cd apps/frontend && corepack pnpm typecheck`
  - Result: GREEN.
- `cd apps/frontend && corepack pnpm test:unit`
  - Result: GREEN.
  - Vitest summary: `Test Files 34 passed (34)`, `Tests 442 passed (442)`.

Backend:

- Initial sandboxed filtered backend test command failed because Mockito/ByteBuddy self-attach and Docker/Testcontainers were unavailable in the sandbox.
- Re-ran outside the sandbox:
  - `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' test`
  - Result: GREEN.
  - Maven summary: `Tests run: 292, Failures: 0, Errors: 0, Skipped: 5`.

Boundary checks:

- `git diff --name-only -- .kiro apps/frontend/tests/e2e apps/frontend/tests/auth`
  - Result: no matches.

## Remaining Risks

- The backend validation used the repository-approved filtered test command because `AGENTS.md` excludes `restkit/**` and `paymentsupport/**` unless explicitly requested.
- Frontend action gating remains a convenience layer only; backend authorization is still the authoritative enforcement point.
- Existing Nuxt Icon warnings about icon loading appeared during unit tests but did not fail the suite.

## Next Step

Codex can proceed next with `user-management` Wave 0 prerequisite verification only.
