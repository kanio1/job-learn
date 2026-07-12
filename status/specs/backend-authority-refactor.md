---
spec: backend-authority-refactor
kiro_config: .kiro/specs/backend-authority-refactor/.config.kiro
kiro_requirements: .kiro/specs/backend-authority-refactor/requirements.md
kiro_design: .kiro/specs/backend-authority-refactor/design.md
kiro_tasks: .kiro/specs/backend-authority-refactor/tasks.md
audited_branch: 001-project-foundation
audited_commit: fec8e1da46da18e3d141660c5bc0753de2ddabf2
last_updated: 2026-07-12
overall_status: DONE_VERIFIED (with documented PBT-iteration-count deviation on 4 leaf tasks)
kiro_task_coverage: 100%
---

## 1. Original Kiro intent

A backend-only, test-first, behavior-preserving refactor of the security/authority layer in
`apps/backend` (Java 25 / Spring Boot 4 / Spring Framework 7 / Spring Modulith 2.0.6 / Spring
Security JWT resource server), addressing four narrow concerns in strict lowest-risk-to-highest-risk
order:

- **R1 — Typed Authority Catalog**: replace duplicated authority magic strings in `SecurityConfig`
  and `MerchantController` with a single compile-time source of truth (`Authorities`, originally
  exactly 9 constants), plus promote the `shared` package to an **OPEN** Spring Modulith module so
  `merchant` can reference it without tripping `ModulithArchitectureTest`.
- **R2 — Explicit, test-first Role Converter**: pin `KeycloakRealmRoleConverter`'s current heuristic
  prefix-rule behavior with characterization tests (Properties 1–3, ≥100 jqwik iterations each) *before*
  refactoring it to an explicit allowlist of the 10 known raw realm roles, then flip only the
  unknown-role assertion so unknown roles are ignored (fail-closed) instead of producing an inert
  `platform:<name>` authority — the single intended behavior change in this spec.
- **R5 — Principal claim name**: configure the JWT authentication converter to expose
  `preferred_username` as the principal name, falling back to `sub` when absent/blank, without
  altering authorities.
- **R4 — Token azp validation (sequenced last, highest risk)**: add an `AuthorizedPartyValidator`
  (`OAuth2TokenValidator<Jwt>`) that asserts `azp == payment-quality-dashboard`, composed with (not
  replacing) the default issuer/timestamp validators, wired into both the production `JwtDecoder` bean
  and `TestJwtConfiguration`'s test decoder, with `TestJwtSupport` additively updated to mint tokens
  carrying the `azp` claim plus a new `tokenWithWrongAuthorizedParty()` negative-asset helper.

Explicitly out of scope: any REST contract change, any business-logic change, any frontend/Nuxt/
Playwright work, removing either enforcement layer (URL rules + `@PreAuthorize`), and editing the
`iam-roles-and-keycloak-login` spec (Requirement 6 records the cross-spec documentation impact as a
note only).

This is the origin spec for `Authorities.java`, the explicit-allowlist `KeycloakRealmRoleConverter`,
`AuthorizedPartyValidator`, and the azp-aware `TestJwtConfiguration`/`TestJwtSupport` infrastructure
described in root `CLAUDE.md` — confirmed directly from `design.md` Components §1–§4.

## 2. Current implementation summary

All 23 leaf tasks have corresponding code and/or test artifacts present in the working tree at HEAD
`fec8e1d`:

- `apps/backend/src/main/java/lab/paymentquality/shared/security/Authorities.java` exists as a
  `final` class with a private constructor. It has grown from the original **9** enforced constants
  to **19**, per its own Javadoc ("Exactly 19 enforced authorities are declared here"). The original
  9 (`MERCHANTS_CREATE`, `MERCHANTS_READ`, `MERCHANTS_UPDATE_STATUS`, `MERCHANT_PAYMENTS_CREATE`,
  `MERCHANT_PAYMENTS_READ`, `MERCHANT_PAYMENTS_LIFECYCLE`, `PLATFORM_PAYMENTS_READ`,
  `PLATFORM_PAYMENTS_LIFECYCLE`, `PLATFORM_PAYMENTS_AUDIT`) are unchanged; 10 more
  (`MERCHANTS_UPDATE_RISK_FLAG`, `PLATFORM_PAYMENT_NOTES_READ/CREATE`, `PLATFORM_AUDIT_READ`,
  `TENANT_AUDIT_READ`, `TENANT_SETTINGS_READ/UPDATE`, `PLATFORM_USERS_*` (4), `TENANT_USERS_*` (4))
  were added additively by later specs (tenant-model-and-isolation, audit-log-dashboard,
  user-management).
- `shared/package-info.java` exists, annotated `@ApplicationModule(type = ApplicationModule.Type.OPEN)`,
  with Javadoc explicitly citing this spec's OQ1 decision.
- `SecurityConfig.java` — every `hasAuthority(...)`/`hasAnyAuthority(...)` URL rule in `filterChain`
  references `Authorities.*` constants; no inline authority-string literals remain. It also declares
  the `jwtAuthenticationConverter()` bean (principal = `preferred_username` with `sub` fallback,
  authorities delegated to `KeycloakRealmRoleConverter`) and the composed `jwtDecoder(...)` bean
  (`DelegatingOAuth2TokenValidator` of `JwtValidators.createDefaultWithIssuer(...)` +
  `AuthorizedPartyValidator`), guarded by `@ConditionalOnMissingBean(JwtDecoder.class)` (a small,
  additive guard not present in the original design snippet — see §5).
- `MerchantController.java` — all 6 `@PreAuthorize` annotations use SpEL string concatenation against
  `Authorities.*` constants exactly as designed (create/read/list/activate/suspend plus a later
  `MERCHANTS_UPDATE_RISK_FLAG`-gated endpoint added by a subsequent spec).
- `KeycloakRealmRoleConverter.java` — rewritten to the designed explicit `Map<String,String>`
  allowlist (now 24 entries, extended by later specs beyond the original 10), values sourced from
  `Authorities` constants except the documented converter-local
  `MERCHANT_PAYMENTS_OPERATE = "merchant:payments:operate"` exception exactly as the design specifies.
  Guards for absent/malformed `realm_access`/`roles` and unknown-role filtering (`Objects::nonNull`)
  match the design precisely.
- `AuthorizedPartyValidator.java` matches the design's snippet essentially verbatim.
- `application.yml` has `payment-quality.security.authorized-party: ${EXPECTED_AZP:payment-quality-dashboard}`.
- `TestJwtConfiguration.java` mirrors the production composition on a public-key `NimbusJwtDecoder`.
- `TestJwtSupport.java` has `EXPECTED_AZP = "payment-quality-dashboard"`, `.claim("azp", EXPECTED_AZP)`
  applied to 6 token builders, and `tokenWithWrongAuthorizedParty()`.
- Four dedicated test files exist and contain the designed properties: `KeycloakRealmRoleConverterTest`
  (Properties 1–3, characterization), `AuthorityCatalogDriftTest` (Property 4), `JwtPrincipalNameTest`
  (Property 6), `AuthorizedPartyValidatorTest` (Property 5).
- `apps/backend/pom.xml` declares `net.jqwik:jqwik` version `1.9.2`, test scope.

**One genuine, verifiable deviation found**: `KeycloakRealmRoleConverterTest`'s three jqwik
`@Property` methods (Properties 1, 2, 3) all run with `@Property(tries = 30)`, not the `≥100`
iterations mandated by `design.md` ("Each property test runs ≥100 iterations") and reiterated in
`tasks.md` for tasks 3.2, 3.3, 3.4. By contrast, `JwtPrincipalNameTest` (Property 6) and
`AuthorizedPartyValidatorTest` (Property 5) both correctly use `tries = 100`. This is a real,
independently confirmed shortfall against the spec's own acceptance bar for tests that are explicitly
marked **NON-optional** (task 3.2–3.4 gate the R2 refactor). See §4 and §5.

## 3. Complete task ledger

| Kiro ID | Original checkbox | Required? | Original task | Execution status | Implementation evidence | Test evidence | Last verified commit | Gap / deviation | Next action |
|---|---|---|---|---|---|---|---|---|---|
| 1.1 | [x] | Yes | Create the typed authority catalog [NEW] | DONE_VERIFIED | `shared/security/Authorities.java` present; original 9 enforced constants unchanged, string values identical to design | Covered transitively by `AuthorityCatalogDriftTest` (Property 4) and full Security_Suite | fec8e1d | Catalog additively extended to 19 constants by later specs — original 9 intact (see §5) | NO_KIRO_TASK_REMAINING |
| 1.2 | [x] | Yes | Mark the `shared` module OPEN (Spring Modulith) [NEW] | DONE_VERIFIED | `shared/package-info.java` present with `@ApplicationModule(type = OPEN)`, Javadoc cites OQ1 | Historical GREEN evidence: `docs/specs-analysis/01-backend-authority-refactor/README.md` records `./mvnw test`/`verify` BUILD SUCCESS with this module topology in place; not re-run this session | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 1.3 | [x] | Yes | Repoint SecurityConfig URL rules to catalog constants [EXTEND] | DONE_VERIFIED | `SecurityConfig.java` `filterChain` bean: every `hasAuthority`/`hasAnyAuthority` call uses `Authorities.*`; no inline literal authority strings remain (confirmed by full-file read) | Same historical GREEN `./mvnw test`/`verify` evidence as 1.2 | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 1.4 | [x] | Yes | Repoint MerchantController @PreAuthorize to catalog constants [EXTEND] | DONE_VERIFIED | `MerchantController.java`: 6/6 `@PreAuthorize` annotations use SpEL concatenation against `Authorities.*` (confirmed via grep) | Same historical GREEN evidence | fec8e1d | One additional `@PreAuthorize(Authorities.MERCHANTS_UPDATE_RISK_FLAG)` endpoint exists beyond original design scope — added by a later spec, additive only | NO_KIRO_TASK_REMAINING |
| 1.5 | [x] | Yes | Add jqwik as a test-scoped dependency [EXTEND] | DONE_VERIFIED | `pom.xml` declares `net.jqwik:jqwik` `${jqwik.version}` = `1.9.2`, `<scope>test</scope>` | Compilation of jqwik-based tests (below) is direct proof the dependency resolves | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 1.6 | [x] | Yes (optional per Notes*, but boxed non-optional in ledger) | Write the catalog no-drift property test [NEW] | DONE_VERIFIED | `security/AuthorityCatalogDriftTest.java` exists with `catalogConstantsMatchEnforcedAuthorityStrings`, `catalogContainsExactlyNineteenEnforcedAuthorities`, `allConstantsFollowExpectedNamingScheme`, tagged `Feature: backend-authority-refactor, Property 4: catalog no-drift` | Historical GREEN evidence per README; test is example-based (not jqwik PBT) per the design's own "Slice/integration" classification for P4 — no ≥100-iteration requirement applies here | fec8e1d | Test's "nineteen" assertion reflects later-spec extension, not original scope — additive, consistent | NO_KIRO_TASK_REMAINING |
| 2 | [x] | Yes | Checkpoint — R1 complete | DONE_VERIFIED (rollup) | Rollup of 1.1–1.6 | Historical `./mvnw test`/`verify` BUILD SUCCESS recorded in `docs/specs-analysis/01-backend-authority-refactor/README.md` (266 tests, 0 failures; 266 unit + 4 integration via Failsafe) | fec8e1d | Evidence predates later specs' extensions to the catalog/converter; a fresh validation pass is running separately this session (see §6) | NO_KIRO_TASK_REMAINING |
| 3.1 | [x] | Yes | Create the converter characterization test scaffold [NEW] | DONE_VERIFIED | `shared/security/KeycloakRealmRoleConverterTest.java` exists; Javadoc explicitly states "pin the CURRENT converter behavior before any R2 refactor" and lists Properties 1–3 | Contains example tests for all empty/malformed-claim guards (`realmAccessAbsent_yieldsEmptyAuthorities` etc.) | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 3.2 | [x] | Yes, NON-optional | Property 1 PBT — known-role mapping (characterization) [NEW, NON-optional] | DONE_WITH_DEVIATION | `property1_known_roles_produce_exactly_their_documented_authorities` present, tagged `Feature: backend-authority-refactor, Property 1: known-role mapping` | Test exists but runs `@Property(tries = 30)`, not the spec-mandated `≥100` iterations (design.md "Testing Strategy"; tasks.md 3.2 "≥100 iterations") | fec8e1d | Iteration count shortfall (30 vs ≥100) on a task explicitly marked NON-optional/gating | Raise `tries` to ≥100 in `KeycloakRealmRoleConverterTest` for Properties 1–3 |
| 3.3 | [x] | Yes, NON-optional | Property 3 PBT — malformed/absent claim → empty (characterization) [NEW, NON-optional] | DONE_WITH_DEVIATION | Two PBT methods (`property3_nonCollectionRolesValue_yieldsEmptyAuthorities`, `property3_nonMapRealmAccessValue_yieldsEmptyAuthorities`) present, correctly tagged | Both use `@Property(tries = 30)`, same shortfall as 3.2 | fec8e1d | Same iteration-count deviation | Same as 3.2 |
| 3.4 | [x] | Yes, NON-optional | Property 2 PBT — pin CURRENT unknown-role behavior (characterization) [NEW, NON-optional] | DONE_WITH_DEVIATION | `property2_unknown_role_is_ignored_fail_closed` present (post-refactor code already reflects the task-4.2 flip — see below) | `@Property(tries = 30)`, same shortfall | fec8e1d | Same iteration-count deviation; additionally, since the working tree already contains the *refactored* converter, this test file no longer shows the pre-refactor "pin" state in isolation — that is expected (task 4.2 explicitly lands the flip in the same commit) | Same as 3.2 |
| 4.1 | [x] | Yes | Refactor KeycloakRealmRoleConverter to a catalog-derived allowlist [EXTEND] | DONE_VERIFIED | `KeycloakRealmRoleConverter.java` uses an explicit `Map.ofEntries` allowlist (24 entries currently, 10 original + 14 added by later specs); 9 original enforced values map to `Authorities` constants, `merchant:payments:operate` maps to the documented converter-local `MERCHANT_PAYMENTS_OPERATE` constant exactly as designed; unknown roles filtered via `Objects::nonNull` | Covered by `KeycloakRealmRoleConverterTest` (see 3.2–3.4) | fec8e1d | None beyond the additive extension already noted | NO_KIRO_TASK_REMAINING |
| 4.2 | [x] | Yes | Flip ONLY the unknown-role assertion to "ignored" [EXTEND] | DONE_WITH_DEVIATION | Confirmed flipped: `unknown_role_PLATFORM_ADMIN_produces_no_authority` and `unknown_role_mixed_with_known_role_only_known_authority_appears` assert no authority for unknown roles (not `platform:PLATFORM_ADMIN`) | Same `tries = 30` PBT shortfall inherited from 3.4's Property 2 method | fec8e1d | Same iteration-count deviation | Same remediation as 3.2 |
| 5 | [x] | Yes | Checkpoint — R2 complete | DONE_WITH_DEVIATION (rollup) | Rollup of 3.1–4.2 | Historical GREEN `./mvnw test`/`verify` (README) predates this audit's discovery of the tries=30 shortfall — historical "green" does not contradict the deviation, since a test with tries=30 still runs and passes, it simply provides less coverage than mandated | fec8e1d | Iteration-count deviation carried from 3.2/3.3/3.4/4.2 | Raise tries to ≥100 |
| 6.1 | [x] | Yes | Configure principal name = preferred_username with safe sub fallback [EXTEND] | DONE_VERIFIED | `SecurityConfig.jwtAuthenticationConverter()` bean matches the design's thin-wrapper snippet exactly: delegates authorities to `KeycloakRealmRoleConverter`, computes name from `preferred_username` with `sub` fallback | Covered by `JwtPrincipalNameTest` (6.2) | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 6.2 | [x] | Yes (optional* per Notes) | Write the principal-name property test [NEW] | DONE_VERIFIED | `shared/security/JwtPrincipalNameTest.java` exists with Property 6a/6b/6c PBT methods plus example tests (`example_noPreferredUsername_subUsedAsPrincipalName`, `example_authoritiesEqualKeycloakRealmRoleConverterOutput_independentOfPrincipalName`) | All three `@Property` methods correctly use `tries = 100`, tagged `Feature: backend-authority-refactor, Property 6: principal name derivation` | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 7.1 | [x] | Yes | Add the AuthorizedPartyValidator [NEW] | DONE_VERIFIED | `shared/security/AuthorizedPartyValidator.java` matches the design's `OAuth2TokenValidator<Jwt>` snippet essentially verbatim (`invalid_token` `OAuth2Error` on mismatch) | Covered by `AuthorizedPartyValidatorTest` (7.6) | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 7.2 | [x] | Yes | Declare an explicit composed JwtDecoder bean [EXTEND] | DONE_VERIFIED | `SecurityConfig.jwtDecoder(...)` bean composes `JwtValidators.createDefaultWithIssuer(issuerUri)` + `new AuthorizedPartyValidator(expectedAzp)` via `DelegatingOAuth2TokenValidator`, reading `payment-quality.security.authorized-party` | Covered by `AuthorizedPartyValidatorTest` composition/e2e assertions | fec8e1d | Bean is additionally annotated `@ConditionalOnMissingBean(JwtDecoder.class)`, not present in the original design snippet — a small additive safety guard, functionally equivalent when no other `JwtDecoder` bean is defined (see §5) | NO_KIRO_TASK_REMAINING |
| 7.3 | [x] | Yes | Add authorized-party configuration property [EXTEND] | DONE_VERIFIED | `application.yml` has `payment-quality.security.authorized-party: ${EXPECTED_AZP:payment-quality-dashboard}` verbatim | Consumed by `SecurityConfig.jwtDecoder(...)` `@Value` injection | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 7.4 | [x] | Yes | Mirror the composed validator in the test decoder [EXTEND] | DONE_VERIFIED | `testsupport/TestJwtConfiguration.java` `jwtDecoder()` bean composes the same `DelegatingOAuth2TokenValidator` (default-with-issuer + `AuthorizedPartyValidator(TestJwtSupport.EXPECTED_AZP)`) on a public-key `NimbusJwtDecoder` | Exercised by the entire Security_Suite (all tests importing `TestJwtConfiguration`) | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 7.5 | [x] | Yes | Additively give test tokens the azp claim + wrong-azp helper [EXTEND] | DONE_VERIFIED | `testsupport/TestJwtSupport.java` has `EXPECTED_AZP = "payment-quality-dashboard"`, `.claim("azp", EXPECTED_AZP)` present on 6 token-builder call sites, and `tokenWithWrongAuthorizedParty()` (subject `wrong.azp.client`, `azp = "some-other-client"`) | Covered by `AuthorizedPartyValidatorTest` and the full Security_Suite (all existing tokens still carry azp additively) | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 7.6 | [x] | Yes (optional* per Notes) | Write the azp validator property + composition + end-to-end tests [NEW] | DONE_VERIFIED | `shared/security/AuthorizedPartyValidatorTest.java` exists: `@Property(tries = 100)` `property5_azpValidatorAcceptsOnlyExpectedParty`, plus example tests `acceptsExactExpectedAzp`, `rejectsDifferentAzp`, `rejectsAbsentAzpClaim`, `rejectsEmptyAzp`, tagged `Feature: backend-authority-refactor` / `Property 5: azp accept/reject` | `tries = 100` correctly matches the spec's ≥100 requirement | fec8e1d | None | NO_KIRO_TASK_REMAINING |
| 8 | [x] | Yes | Final checkpoint — full regression | DONE_WITH_DEVIATION (rollup) | Rollup of all R1/R2/R5/R4 tasks; no REST contract, business-logic, frontend, or Playwright changes detected in this spec's file set | Historical evidence in `docs/specs-analysis/01-backend-authority-refactor/README.md`: `./mvnw test` → BUILD SUCCESS, 266 tests, 0 failures, 0 errors, 5 skipped; `./mvnw verify` → BUILD SUCCESS, 266 unit + 4 integration (Failsafe), all green. Not re-run this session (fresh validation running separately per orchestrator) | fec8e1d | Carries forward the tries=30 PBT deviation from 3.2/3.3/3.4/4.2; historical GREEN predates later specs' catalog/converter extensions | Raise tries to ≥100 in `KeycloakRealmRoleConverterTest`; otherwise NO_KIRO_TASK_REMAINING |

STATUS_LEAF_TASK_COUNT = 23. KIRO_LEAF_TASK_COUNT = 23. Match confirmed, no duplicate task IDs.

## 4. Acceptance criteria gaps

- **Requirement 2.1 / Testing Strategy "≥100 iterations" (tasks 3.2, 3.3, 3.4, 4.2)**: the three
  jqwik `@Property` methods in `KeycloakRealmRoleConverterTest` (Properties 1, 2, 3 — all explicitly
  marked NON-optional because they gate the R2 refactor) run with `tries = 30`, not the `≥100`
  iterations the design and tasks documents both mandate. This is a real, verifiable acceptance gap.
  It does not indicate the properties are false or the refactor is unsafe — the properties still hold
  for every sampled case — but the coverage bar itself is unmet. `JwtPrincipalNameTest` (Property 6,
  task 6.2) and `AuthorizedPartyValidatorTest` (Property 5, task 7.6) both correctly use `tries = 100`,
  showing the ≥100 convention was known and applied elsewhere in the same spec — making the
  `KeycloakRealmRoleConverterTest` shortfall look like an oversight specific to that one file rather
  than a deliberate, documented reduction.
- **Requirement 1.2 ("exactly nine")**: the catalog no longer holds exactly 9 constants — it holds 19,
  and `AuthorityCatalogDriftTest` itself now asserts "nineteen" rather than "nine". This is not a defect
  of this spec (the original 9 are byte-identical and still each individually enforced); it is the
  expected, additive effect of three later specs building on top of this one. Flagged here only so a
  future reader does not mistake the "exactly nine" acceptance criterion as still literally true of the
  current codebase — see §5 for the ACCEPTABLE/NEEDS_REVIEW classification.
- All other acceptance criteria for R1, R5, and R4 (Requirements 1, 4, 5, and the cross-cutting
  Requirement 3 preservation guarantees) have direct code evidence matching the design with no gaps
  found.

## 5. Deviations from original design

| Deviation | Classification | Note |
|---|---|---|
| `KeycloakRealmRoleConverterTest` Properties 1–3 use `@Property(tries = 30)` instead of the mandated `≥100` | **NEEDS_REVIEW** | These are the three NON-optional, refactor-gating characterization properties. Lower iteration count than spec requires is a real, if narrow, quality-bar shortfall — worth a follow-up fix, not a blocker to calling the spec functionally complete. |
| `Authorities` catalog grown from 9 to 19 constants; `KeycloakRealmRoleConverter`'s allowlist grown from 10 to 24 entries | **ACCEPTABLE** | Per the task instructions, this is exactly the kind of later-spec extension expected: `tenant-model-and-isolation` (tenant settings/audit authorities), `audit-log-dashboard` (audit-read authorities), and `user-management` (user-management authorities) all additively extended the catalog and allowlist this spec introduced. The original 9 enforced constants and the original 10 known-role mappings (including the `merchant:payments:operate` exception) are unchanged and still individually verifiable in the current code. No conflict with this spec's original design was found — the extension follows the exact same pattern (`Authorities` constant + allowlist entry) this spec established. |
| `SecurityConfig.jwtDecoder(...)` bean annotated `@ConditionalOnMissingBean(JwtDecoder.class)` | ACCEPTABLE | Not present in the original design snippet. Functionally a no-op when no other `JwtDecoder` bean is defined (the normal case in `main`); allows a test or profile-specific override to take precedence if one is ever introduced. No behavior change to the composed validator itself. |
| `MerchantController` gained a 7th `@PreAuthorize` (`MERCHANTS_UPDATE_RISK_FLAG`) beyond this spec's original 3-endpoint scope | ACCEPTABLE | Added by a later spec (merchant risk-flag feature); uses the same `Authorities`-constant pattern this spec introduced, so it is a consistent application of R1's pattern rather than a drift away from it. |
| Cross-spec documentation follow-up (Requirement 6) | ACCEPTABLE | `docs/specs-analysis/01-backend-authority-refactor/README.md` states the `iam-roles-and-keycloak-login` design.md Decision 1, discrepancy A, Property 1 and Property 9 passages were in fact updated (marked "✅") to describe unknown roles as ignored rather than inert. This satisfies Requirement 6's intent even though this spec's own files (correctly) do not touch that other spec's files. |

## 6. Current test baseline

No dedicated `.codex/*.md` execution-overlay document exists for this spec — it predates that
convention, and `.codex/status-hygiene-audit.md` explicitly notes "no codex doc" for it. The only
recorded regression evidence is:

- `docs/specs-analysis/01-backend-authority-refactor/README.md` ("Wyniki regresji" section):
  `./mvnw test` → BUILD SUCCESS, 266 tests, 0 failures, 0 errors, 5 skipped; `./mvnw verify` → BUILD
  SUCCESS, 266 unit + 4 integration (Failsafe), all green. No date stamp is given in that file, and it
  predates at least three later specs' extensions to the same files (`Authorities`,
  `KeycloakRealmRoleConverter`, `SecurityConfig`), so its exact test counts no longer match the current
  test suite size.
- `.codex/status-hygiene-audit.md` (2026-06-18) independently classifies this spec
  `COMPLETE_AND_KIRO_MARKED`, citing "all boxes checked; converter allowlist + `Authorities` catalog in
  code" as its evidence basis — a code-presence check, not a fresh test run.

This audit performed its own **static** verification this session: direct reads of `Authorities.java`,
`shared/package-info.java`, `SecurityConfig.java`, `MerchantController.java`,
`KeycloakRealmRoleConverter.java`, `AuthorizedPartyValidator.java`, `application.yml`,
`TestJwtConfiguration.java`, `TestJwtSupport.java`, and all four dedicated test files
(`KeycloakRealmRoleConverterTest`, `AuthorityCatalogDriftTest`, `JwtPrincipalNameTest`,
`AuthorizedPartyValidatorTest`), plus `grep` confirmation of `@PreAuthorize`/`hasAuthority` call sites
and `@Property(tries = ...)` values. No build was run by this audit (per instructions, a fresh
validation pass is running separately this session and its results belong in
`status/evidence/latest-validation.md`, not here). Every code artifact this spec's tasks call for was
found present and matching the design; the only gap found by static inspection is the `tries = 30` vs
`≥100` PBT iteration-count shortfall documented in §4/§5.

## 7. Next executable task

Not a blocking gap, but the one concrete, actionable follow-up this audit surfaces:

**POST_KIRO_WORK (not a Kiro task, follow-up only):** raise `@Property(tries = 30)` to
`@Property(tries = 100)` (or higher) on the three characterization property methods in
`apps/backend/src/test/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverterTest.java`
(`property1_known_roles_produce_exactly_their_documented_authorities`,
`property2_unknown_role_is_ignored_fail_closed`,
`property3_nonCollectionRolesValue_yieldsEmptyAuthorities`,
`property3_nonMapRealmAccessValue_yieldsEmptyAuthorities`) to meet the spec's own `≥100` iteration
bar, matching the convention already used correctly in `JwtPrincipalNameTest` and
`AuthorizedPartyValidatorTest`.

Otherwise: **NO_KIRO_TASK_REMAINING** — all 23 leaf tasks (including all 3 checkpoints) have direct
code and/or test evidence of completion; the single deviation found (§4/§5) is a test-quality
shortfall, not a missing implementation, missing test file, or broken behavior.
