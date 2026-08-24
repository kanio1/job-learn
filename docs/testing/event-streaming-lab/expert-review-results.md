# Event Streaming Lab — two commits + WIP expert review

Review date: 2026-08-24  
Mode: review only / findings only  
Verdict: **REQUEST_CHANGES**

## Scope snapshot

- Repository: `/home/suso/job-learn`
- Branch: `001-project-foundation`
- BASE: `6ee6827268cfc51d114a2fbcab982e6bef4d3159`
- Commit 1: `364f520b9156239c34fcd0986c1936ce41afb3db`
- Commit 2 / HEAD: `7f0e54d7880e041078ce7a7051231692cc9fc175`
- HEAD exactly matched Commit 2. `BASE` is an ancestor of Commit 1 and Commit 1 is an ancestor of Commit 2.
- Staged at review start: none.
- Review boundary: `BASE..Commit 1`, `Commit 1..Commit 2`, then tracked and untracked WIP relative to Commit 2. The report itself was created only after this snapshot.

### Commit 1 inventory

Commit message claims E3/E5 completion and green KafkaIT/Vitest/jqwik evidence. The commit changes 6 files, +310/−71:

- Event Lab controller response content type;
- `EventLabEnvelopePropertyTest`;
- `EventLabPersistenceKafkaIT`;
- `EventLabRestAssuredTest`;
- acceptance catalog;
- task board.

It does not contain the POM page object, Event Lab Playwright spec, delivery-card implementation, runbook, or rebalance IT that it marks `DONE`/`PASS`.

### Commit 2 inventory

Commit 2 reports 131 file changes (+5,624/−1,089; 132 changed paths because of rename accounting):

| Area | Changed paths | Summary |
|---|---:|---|
| `.agents` | 68 | skill imports, deletes, renames, governance changes, 3,346-line upstream Spring reference pack |
| `.cursor` | 20 | skill symlink additions/deletions |
| `.codex` | 15 | ADR/spec/prompts/checklists/research/status material |
| `apps` | 9 | backend listener/KafkaIT plus frontend product/POM code |
| `status` | 12 | Event Lab roadmap and completion claims |
| `docs`, scripts, root instructions | 7 | docs, dev-stack, AGENTS/CLAUDE/README |
| root `META-INF` | 1 | Spring Boot auto-configuration import file outside Maven resources |

The message only says skills were reviewed/added, while the commit also changes product behavior and tests.

### Worktree inventory at review start

Tracked unstaged: 17 files (+1,095/−159), spanning status, backend retry/config/tests, BFF, Playwright and dev-stack. Untracked: 9 files:

- `EventLabRetryNamingConfiguration.java`
- `EventLabRetryTopicConfiguration.java`
- `EventLabTopics.java`
- `application-kafka.yml`
- `EventLabRebalanceKafkaIT.java`
- `useEventLabApi.test.ts`
- `docs/setup/lenses-ui-and-mcp.md`
- `expert-review-prompt.md`
- root `pnpm-lock.yaml`

Every untracked file was opened and reviewed. No untracked directory was skipped.

## Verdict

**REQUEST_CHANGES.** There are no P0 findings, but nine P1 findings break accepted requirements, turn simulations into false PASS evidence, or make a commit unsafe to use independently. In particular, the committed history is not bisectable as a green progression: Commit 1 certifies artifacts that are absent, Commit 2 introduces a broken Event Lab BFF list handler, and current WIP still calls several non-oracles `PASS`.

The architecture seam and basic envelope code are healthy, but completion should be rolled back to `PARTIAL` until KafkaIT and live POM are rerun with repaired oracles on a working stack.

## Commit hygiene verdict

| Unit | Verdict | Reason |
|---|---|---|
| Commit 1 | **UNSAFE / FALSE COMPLETION** | Marks T16, T18 and T20 done without their artifacts; promotes static wiring and DB simulations to Kafka/UI PASS evidence. |
| Commit 2 | **UNSAFE / NOT ATOMIC** | Mixes skill governance, product backend/frontend, tests, roadmap rewrites and a broken BFF handler under a skills-only message. Independent cherry-pick/revert is not safe. |
| WIP | **NOT READY** | Contains useful fixes, but retry/DLT semantics, test oracles, untracked topic config and accidental root lockfile remain unresolved. |

## Findings

### P0

No P0 finding. No secret, cross-tenant disclosure or demonstrated data loss was found in the reviewed delta.

### P1

#### [P1] [Spec] [COMMIT1] [documentation/evidence] `status/roadmaps/kafka-event-streaming-lab/task-board.md:25` — Completion was recorded before artifacts and executable proof existed

Evidence: Commit 1 changes only six files yet marks `KAFKA-T16` POM, `KAFKA-T20` runbook and `KAFKA-T18` rebalance hardening `DONE` at lines 31–34. Those artifacts first appear in Commit 2 or WIP. Its acceptance catalog also calls controller wiring, repository constraints and source inspection `PASS` for broker, replay, DLT, UI and lifecycle behavior. Current `status/index.md:8` and `.codex/current-state.md:5` amplify this to 25/25 KafkaIT and 31/31 Playwright although the canonical `status/evidence/latest-validation.md:1-10` is from July and explicitly superseded.

Expected: `PASS`/`DONE` only after the named artifact exists and its acceptance-level oracle has a reproducible green command/evidence record.

Impact: false completion hides missing tests and makes subsequent review trust claims that the code does not establish.

Smallest remediation: change unsupported catalog/board entries to `PARTIAL` or `IMPLEMENTED_NOT_EXECUTED`; attach fresh command, SHA, counts and log location only after repaired tests pass.

Missing/weak test: a detached per-commit validation that checks every `DONE` task has its named artifact and green acceptance ID.

Confidence: HIGH

#### [P1] [CommitHygiene] [COMMIT2] [cross-repository] `7f0e54d:1` — The skills commit is not an atomic or independently safe change

Evidence: the message is “Reviewed and change skills. Added ponytail, playwrgiht and spring skills.” The commit changes 131 files, including 68 `.agents` paths, 20 `.cursor` links, ADR/spec/status, backend Kafka behavior, payment-detail UI, BFF, Playwright and dev-stack. It also contains the broken BFF handler in the next finding.

Expected: governance/import changes, Event Lab product changes, test evidence and roadmap status should be independently reviewable and revertible.

Impact: cherry-picking the advertised skill change silently modifies product behavior; reverting it removes product features and documentation together; bisect lands on a broken BFF state.

Smallest remediation: before sharing the history, split it into at least skill governance, Event Lab product, Event Lab tests, and evidence/status commits; give each an accurate message and green scoped validation.

Missing/weak test: build/typecheck plus targeted contract tests executed at each commit boundary, not only on final WIP.

Confidence: HIGH

#### [P1] [Standards] [COMMIT2] [Nuxt/BFF] `apps/frontend/server/api/event-lab/index.get.ts:14` — Commit 2 returns `undefined` or throws for every successful Event Lab list

Evidence: at Commit 2, the handler assigns `backendApi(...)` to `res`, reads `res.headers` and returns `res.data` (lines 14–18). `backendApi` returns `response._data` directly at `server/utils/backendApi.ts:67`, not a response wrapper. Without an incoming correlation header, `res.headers[...]` can throw; with one, `res.data` is still `undefined`. WIP correctly reduces the route to `return backendApi(...)`.

Expected: the BFF returns the proxied list body and relies on `backendApi` to forward backend headers.

Impact: Commit 2 cannot independently serve the core Event Lab list; UI and POM proof on that commit are invalid.

Smallest remediation: retain the WIP implementation and add a route-level test for a successful array body plus correlation-header forwarding.

Missing/weak test: Nitro/BFF unit or live API assertion that response status, array body and `X-Correlation-ID` survive proxying.

Confidence: HIGH

#### [P1] [Standards] [COMMIT1] [REST Assured/Maven] `apps/backend/src/test/java/lab/paymentquality/rest/EventLabRestAssuredTest.java:31` — Surefire is no longer broker-free

Evidence: the class is named `*Test`, activates `test,kafka`, and obtains `KafkaContainerSupport.bootstrapServers()` at lines 31–45. Surefire includes every `**/*Test.java` and excludes only `**/*KafkaIT.java` at `pom.xml:265-270`. Therefore `./mvnw test` selects a Kafka-starting test despite ADR/spec requiring broker-free Surefire.

Expected: broker-dependent Event Lab HTTP tests run only under Failsafe `*KafkaIT`/`*IT`; `./mvnw test` must not initialize Kafka.

Impact: ordinary backend unit validation now depends on a container runtime and violates the flag-off/rollback safety seam.

Smallest remediation: rename/move the class to `EventLabRestAssuredKafkaIT` (or another `*IT`) and let Failsafe own it; keep only genuinely broker-free HTTP tests in Surefire.

Missing/weak test: run `./mvnw test` with an unreachable bootstrap server and assert no Kafka container/client initialization.

Confidence: HIGH

#### [P1] [Spec] [CROSS_LAYER] [HTTP/Kafka] `apps/backend/src/main/java/lab/paymentquality/eventlab/internal/web/EventLabController.java:80` — Inject endpoints do not inject Kafka records

Evidence: duplicate returns the existing DB row without publishing anything (lines 80–95). Poison directly mutates `eventlab_processed.status` to `DEAD` (lines 97–115). Nevertheless `.codex/current-state.md:5` says “poison/duplicate via broker ≤5s” and the acceptance catalog treats these endpoints as broker/DLT proof.

Expected: the lab’s duplicate and poison controls send a duplicate/poison envelope through the real source topic so the production listener, retry budget, custom DLT and DB idempotency are exercised.

Impact: the showcase UI can display success while Kafka, retry routing or the listener is completely broken.

Smallest remediation: place a narrow inject publisher inside `eventlab`; make the endpoints publish the selected event with the same `eventId` or a poison marker, then wait/read the resulting state without direct status mutation.

Missing/weak test: HTTP inject → consume source/retry/custom DLT → persisted one-row/DEAD proof, with payment and audit invariants checked.

Confidence: HIGH

#### [P1] [Spec] [WORKTREE] [KafkaIT/test design] `apps/backend/src/test/java/lab/paymentquality/eventlab/EventLabFlagOffKafkaIT.java:37` — Several backend acceptance tests simulate or bypass the seam named by the test

Evidence: `RA-KAFKA-011N` claims no Kafka connection but starts topics and opens a `KafkaConsumer` (lines 37–75). `RA-KAFKA-023` “replay from earliest” merely produces the same event twice (`EventLabPersistenceKafkaIT:174-185`). `RA-KAFKA-018` swallows failure to observe broker records and falls back to outbox timestamps (`EventLabOutboxKafkaIT:346-390`). `RA-KAFKA-019` publishes synthetic application events instead of invoking capture/cancel/refund and swallows missing broker proof (`:395-439`). `RA-KAFKA-026` seeds no old record and checks no business table (`EventLabPersistenceKafkaIT:268-270`). `RA-KAFKA-050` pauses the production listener, manually consumes and writes the repository, enables auto-commit and swallows exceptions (`EventLabRebalanceKafkaIT:107-210`).

Expected: each named acceptance case drives the production seam and has an oracle that fails if that seam is broken.

Impact: green tests can coexist with producer connections under flag-off, broken restart/rebalance handling, absent lifecycle emissions, wrong Kafka order, or destructive purge behavior.

Smallest remediation: replace each simulation with the smallest real action: client-metrics/log spy for zero connections; restart/seek for replay; required broker offsets for ordering; real lifecycle HTTP/service calls; seeded old/new/business rows for purge; and two production listener instances for rebalance.

Missing/weak test: the cases above are the missing tests; do not retain their current `PASS` labels as supplemental proof.

Confidence: HIGH

#### [P1] [Spec] [WORKTREE] [Playwright POM] `apps/frontend/tests-pom/specs/event-lab.spec.ts:61` — Named Playwright acceptance cases do not perform the behavior they certify

Evidence: the “six states” test observes only loaded/filtered-empty or forbidden (`:61-80`); “flag off hides nav” runs flag-on and expects the heading (`:82-87`); dismiss compares row count, so an idempotent POST can pass unnoticed (`:89-113`); the security test makes the response optional and inspects response headers rather than the browser request (`:115-127`). E2E-002 authorizes rather than captures and never visits the payment detail delivery card (`:233-251`). E2E-012 also stays on the Event Lab page rather than asserting the payment card (`:406-427`). Poison E2E proves the controller’s direct DB mutation, not the DLT (`:310-342`).

Expected: test names, catalog requirements and executed journeys describe the same behavior, with a product-level oracle.

Impact: the claimed 31/31 run cannot establish flag gating, six-state UX, no-POST dismiss, credential non-leakage, capture delivery card, pending→processed card, or DLT flow.

Smallest remediation: retitle tests that only cover a narrower behavior and add one focused real-stack test per missing AC; use request observation for no-POST/security and visit the payment detail page for delivery-card cases.

Missing/weak test: PW-KAFKA-E2E-002/006/007/011/012, SEC-003 and the DLT half of E2E-005 need real oracles.

Confidence: HIGH

#### [P1] [Spec] [WORKTREE] [Kafka semantics] `apps/backend/src/main/java/lab/paymentquality/eventlab/internal/application/EventLabInspectorListener.java:46` — Non-blocking retry contradicts the unconditional same-key ordering claim

Evidence: WIP uses `@RetryableTopic`, i.e. non-blocking retry topics (lines 46–57), while `01-acceptance-cases.md:57` asserts authorize→capture order is preserved without limiting the claim to successful main-topic processing. Spring Kafka explicitly states that this strategy loses Kafka ordering guarantees for the topic ([Spring Kafka 4.0 reference](https://docs.spring.io/spring-kafka/reference/4.0/retrytopic/how-the-pattern-works.html)).

Expected: either the requirement says ordering is guaranteed only on the non-failing main path, or retry uses an approach that preserves per-partition order.

Impact: a failed authorize can move to retry while a later capture for the same `targetId` is processed first, invalidating the learning claim and potentially the displayed sequence.

Smallest remediation: document the non-blocking-retry limitation if it is intentional; otherwise use blocking retry for this single listener. Add an explicit poison/failure followed by valid same-key event test.

Missing/weak test: same-key first event fails once, second succeeds, and the asserted/accepted order matches the chosen policy.

Confidence: HIGH (behavior source is 4.0.6 documentation; resolved runtime is 4.0.5, with no contradictory patch note found)

#### [P1] [Standards] [WORKTREE] [Kafka/DLT persistence] `apps/backend/src/main/java/lab/paymentquality/eventlab/internal/application/EventLabInspectorListener.java:116` — The DLT handler converts persistence failure into success

Evidence: retry config declares `DltStrategy.FAIL_ON_ERROR` at line 49, but the `@DltHandler` catches every exception and only logs it at lines 116–146. A parse, UUID or database failure therefore returns normally to the container and defeats the declared fail-on-error policy.

Expected: failure to record the DEAD proof remains observable/retriable and is not acknowledged as successful handling.

Impact: the DLT record may exist while `eventlab_processed` has no DEAD row, producing silent divergence between Kafka and the lab UI.

Smallest remediation: remove the outer catch or rethrow after logging; keep only the narrow duplicate-constraint handling.

Missing/weak test: force repository failure inside `@DltHandler` and assert the handler/container observes a failure rather than committing a false success.

Confidence: HIGH

### P2

#### [P2] [Standards] [WORKTREE] [Kafka configuration] `apps/backend/src/main/java/lab/paymentquality/eventlab/internal/EventLabRetryTopicConfiguration.java:24` — Topic provisioning has three authorities and creates an unused technical DLT

Evidence: `@RetryableTopic(autoCreateTopics="true")` can create main/retry/DLT topics; `EventLabRetryTopicConfiguration` declares four `NewTopic`s including `lab.auditable-actions.v1-dlt`; `dev-stack.sh:351-361` creates the same topics again. Custom naming routes DLT to `lab.event-lab.dlq.v1`, so the default DLT is retained only as a compatibility artifact. Spring recommends annotation bootstrapping as the simplest route and documents `NewTopic` beans as the override mechanism ([configuration reference](https://docs.spring.io/spring-kafka/reference/4.0/retrytopic/retry-config.html)).

Expected: one authoritative topic manifest for source, single retry topic and contract DLT, with broker auto-create still off.

Impact: drift in partition/RF/name settings and operator confusion about which DLT is contractual.

Smallest remediation: choose explicit `NewTopic` provisioning with `autoCreateTopics=false`, remove the unused default DLT, and make scripts/tests mirror only the three contract topics.

Missing/weak test: assert the exact topic set and configuration, including absence of the unused default DLT.

Confidence: HIGH

#### [P2] [Standards] [WORKTREE] [Spring/Java] `apps/backend/src/main/java/lab/paymentquality/PaymentQualityApplication.java:8` — Spring Retry is added globally but is unused

Evidence: WIP adds direct `spring-retry:2.0.13` with a hard-coded version and global `@EnableRetry`. The codebase has no Spring `@Retryable` use. Spring Kafka 4 provides its own `org.springframework.kafka.annotation.BackOff` and `@RetryableTopic` infrastructure; its reference requires `@RetryableTopic` or Kafka retry-topic configuration, not `@EnableRetry`.

Expected: no dependency or global proxy feature without a caller.

Impact: an unnecessary dependency and global behavior surface obscure which retry system is active.

Smallest remediation: remove `spring-retry` and `@EnableRetry`; keep Spring Kafka’s retry-topic annotations only.

Missing/weak test: compile plus targeted retry-topic context test after removal.

Confidence: HIGH

#### [P2] [Spec] [WORKTREE] [operator docs] `docs/setup/lenses-ui-and-mcp.md:62` — The runbook overstates the retry budget

Evidence: docs say “Po 3 retryach” at line 65. The annotation sets `attempts="3"`, which is the total maximum attempts, not three retries after the first. Spring’s pattern example describes four max attempts as main plus three retry-topic attempts ([Spring Kafka retry pattern](https://docs.spring.io/spring-kafka/reference/4.0/retrytopic/how-the-pattern-works.html)). With `SINGLE_TOPIC`, the configured budget is initial delivery plus two retries.

Expected: docs state three total attempts / two retries with fixed 500 ms backoff.

Impact: operators and tests wait for and teach the wrong sequence.

Smallest remediation: correct the wording and assert observed delivery-attempt headers/timing rather than deriving it from comments.

Missing/weak test: consume retry/DLT headers and assert total attempts and approximate fixed-backoff budget.

Confidence: HIGH

#### [P2] [Standards] [COMMIT2] [Spring placement] `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1` — Auto-configuration metadata is outside the Maven resource tree

Evidence: Commit 2 adds the file at repository root. Maven packages `apps/backend/src/main/resources`, not root `META-INF`; the built Boot jar has no matching `BOOT-INF/classes/META-INF/...AutoConfiguration.imports`. It also lists three Modulith configurations directly, including both Jackson variants.

Expected: application configuration should be provided through normal Spring Boot dependencies/configuration; custom auto-configuration metadata, if genuinely required, belongs under the owning module’s `src/main/resources/META-INF/spring`.

Impact: the file has no runtime effect but looks authoritative, misleading reviewers and future maintainers.

Smallest remediation: delete it unless a failing context test proves a need; if needed, move a minimal, owned import file into backend resources and test packaged-jar behavior.

Missing/weak test: inspect the packaged jar and start the Kafka profile from that jar.

Confidence: HIGH

#### [P2] [Standards] [COMMIT2] [skill governance] `.cursor/skills/playwright-skill-upstream:1` — Skill rename/delete left a broken route and stale live references

Evidence: the symlink targets `../../.agents/skills/playwright-skill-upstream`, which does not exist. `.agents/skills/README.md:37` still routes to that absent pack. `status/roadmaps/kafka-event-streaming-lab/epics/E0-governance.md:24`, modified in Commit 2, still requires deleted `wayfinder`. Other historical docs also retain deleted names.

Expected: every advertised/symlinked skill resolves, live routing docs use current names, and historical references are explicitly marked historical rather than executable.

Impact: Cursor and human routing fail at the exact Playwright deep-dive step; Event Lab governance instructs agents to invoke a removed skill.

Smallest remediation: either restore the intended upstream pack or remove the broken link/README route; replace live `wayfinder` routing with the current supported flow and run a dead-link audit.

Missing/weak test: CI/read-only governance check that every `.cursor/skills` symlink and every skill path in live README/roadmaps resolves.

Confidence: HIGH

#### [P2] [CommitHygiene] [WORKTREE] [frontend tooling] `pnpm-lock.yaml:1` — An unrelated root lockfile pins TypeScript 7 beside the frontend’s TypeScript 6 toolchain

Evidence: the untracked root lockfile has a single root importer for `typescript@7.0.2` and 225 lines of platform packages. The actual frontend uses its own `packageManager: pnpm@11.18.0` and `typescript: 6.0.3` under `apps/frontend`.

Expected: Event Lab WIP should not add a second root dependency graph unrelated to the Nuxt workspace.

Impact: accidental commit creates competing TypeScript versions and noisy cross-platform lock churn.

Smallest remediation: omit the root lockfile from this change; if a root workspace is intentional, specify it in a separate tooling change with ownership and validation.

Missing/weak test: repository check that lockfiles correspond to declared package/workspace roots.

Confidence: HIGH

### P3

#### [P3] [Standards] [COMMIT2] [documentation] `status/roadmaps/kafka-event-streaming-lab/02-lenses-telescope.md:29` — Commit diff contains trailing whitespace

Evidence: `git diff --check BASE` reports two trailing-whitespace lines at 29–30.

Expected: clean diff.

Impact: minor review noise and a red hygiene command.

Smallest remediation: remove the two trailing spaces.

Missing/weak test: keep `git diff --check BASE` as a pre-commit/review gate.

Confidence: HIGH

#### [P3] [Standards] [COMMIT2] [skill governance] `.agents/skills/project-skill-governance-and-quality-review/scripts/validate_skills.py:1` — Skill collection is structurally valid but emits 28 scope warnings

Evidence: validator exit 0 found 43 skills and no structural errors, but 28 skills lack a `When Not to Use` section, including newly added ponytail modes and review/implementation routing skills.

Expected: imported skills state their negative boundary so review-only, implementation and one-shot modes do not overlap ambiguously.

Impact: low-grade routing ambiguity; not a structural failure.

Smallest remediation: add concise negative boundaries to actively routed skills first; do not bulk-copy generic boilerplate.

Missing/weak test: promote warnings to a reviewed allowlist or enforce them for newly added/changed skills only.

Confidence: HIGH

## Standards axis

The module boundary is sound: `ModulithArchitectureTest` passed, `eventlab` is not OPEN, and no payment import of `eventlab.internal` was found. Java 25 usage is ordinary and does not introduce preview APIs, JPMS or speculative concurrency. Flyway remains the schema owner and the Event Lab entity constraint is useful.

The main standards failure is test placement and oracle ownership. A Kafka-starting `*Test` bypasses the Surefire/Failsafe seam. Several KafkaITs manually reproduce production work, swallow missing broker evidence or assert a weaker DB fact than their name. Playwright has the same pattern: test titles and catalogs advertise product behavior while the body checks static wiring, optional responses or an idempotent row count.

Kafka configuration is functional enough to compile but deeper than needed: source/retry/DLT topics are created by annotation, application configuration and shell script; an unused default DLT remains; Spring Retry is added although Spring Kafka owns this retry path. The DLT handler also suppresses the error policy it declares.

Nuxt typecheck is green and WIP fixes Commit 2’s BFF return contract. POM uses live APIs, stable IDs and unique data in many cases, with no `page.route`. Security/tenant checks are present, but the specific network-leak and delivery-card cases need real request/UI oracles.

Governance validation is structurally green, yet one Cursor symlink is broken and live docs route to deleted skills. No secret or prohibited Kafka UI dependency was found.

## Spec axis

ADR 0002’s core placement—Postgres source of truth, Modulith outbox, one Event Lab module, three-partition RF1 lab topic, thin BFF/UI and Lenses as telescope—is visible in code. RF1, PLAINTEXT and single-node KRaft are correctly classified `LAB_SHAPED_ACCEPTED`, not product defects.

The feature is not proven `DONE`. Flag-off has no zero-connection oracle. Duplicate/poison HTTP bypass Kafka. Replay and rebalance tests bypass restart/production listener behavior. Lifecycle and ordering tests accept missing broker records. Non-blocking retry makes the unconditional same-key ordering requirement false on failures. Retry count documentation is wrong, and DLT persistence failure is swallowed.

HTTP authority and tenant masking are implemented in controller/security code, but fresh container execution was blocked. Payment status is checked for poison in one KafkaIT/Playwright path; audit immutability and purge safety are not acceptance-tested. The custom DLT consume path is implemented but not freshly executed.

The UI exists, but named POM cases do not prove the payment-detail delivery card, all six states, flag-off navigation, zero POST on dismiss or absence of Authorization/Kafka bootstrap in browser requests. The status registry therefore declares materially more than the current oracles establish.

Overall traceability status is `PARTIAL`: implementation breadth is high, independent proof quality is not.

## Layer review

### Spring / Java backend

- Good: Java 25 compile, architecture test, eventlab placement, Flyway/JPA constraint.
- Blocking: redundant global Spring Retry; DLT failure swallowed; injected poison mutates persistence directly.
- No payment-module dependency on `eventlab.internal` was observed.

### Kafka / Modulith / transactions

- Good: stable event ID/envelope tests, target ID key, outbox-based externalization, custom DLT consume code, RF1 framing.
- Blocking: unconditional ordering claim conflicts with non-blocking retry; topic creation is triplicated; restart/rebalance/order/lifecycle oracles are weaker than their ACs.
- The source topic and contract DLT remain within `eventlab`; no Kafka command bus or audit rewrite was introduced.

### REST Assured / HTTP

- Authorities and masked 404 behavior are represented.
- Broker-dependent REST Assured is incorrectly selected by Surefire.
- Duplicate/poison tests assert HTTP/DB effects but do not prove Kafka injection, retry or DLT.

### Nuxt / BFF

- WIP list route matches the existing `backendApi` contract and typecheck passes.
- Commit 2 itself is broken.
- Zod/composable unit coverage is present; no browser Kafka client exists.

### Playwright POM

- Uses live stack patterns, `expect.poll`, stable test IDs and unique worker data; discovery lists 22 Event Lab tests.
- Multiple names/catalog entries overstate the executed journey. No live test was run because the stack was not already available.

### Docs / status / skill governance

- ADR lab-vs-prod framing is clear and Lenses is not a CI oracle.
- Completion/evidence records are not trustworthy enough for `DONE`.
- Broken symlink, stale routing, stale spec state and mixed historical/current status need reconciliation.

## Accidental inclusion matrix

| Scope | Files/group | Classification | Reason / action |
|---|---|---|---|
| Commit 1 | 4 backend/test files | INTENDED_IN_SCOPE | E3 backend work, but tests/oracles need repair. |
| Commit 1 | acceptance catalog + task board | INTENDED_BUT_PREMATURE | Keep documents, roll unsupported PASS/DONE back. |
| Commit 2 | `.agents/**`, `.cursor/**` | REQUIRES_AUTHOR_CONFIRMATION | Matches message, but includes mass deletes/imports, stale routes and broken link. |
| Commit 2 | `.codex/**`, `status/**`, Event Lab docs | REQUIRES_AUTHOR_CONFIRMATION | Event Lab planning/evidence mixed into a skills commit. |
| Commit 2 | backend listener/KafkaIT, frontend payment page/BFF/POM | ACCIDENTAL_OR_UNRELATED_TO_MESSAGE | Product behavior belongs in separate product/test commits. |
| Commit 2 | root `META-INF/**` | ACCIDENTAL | Not packaged; delete unless a packaged-jar test proves need. |
| WIP | retry config, topic constants, Kafka YAML, Event Lab backend tests | INTENDED_IN_SCOPE | Keep after P1/P2 repair and executable Kafka validation. |
| WIP | BFF fix, Playwright, composable unit test | INTENDED_IN_SCOPE | BFF fix is required; POM oracles need correction. |
| WIP | Lenses runbook | INTENDED_IN_SCOPE | Correct retry count and topic authority wording. |
| WIP | expert review prompt | USER_PROVIDED_REVIEW_INPUT | Preserve. |
| WIP | root `pnpm-lock.yaml` | ACCIDENTAL_OR_UNRELATED | Omit from Event Lab change. |
| All scopes | `.env`, storage state, tokens, keys | NOT_PRESENT | No secret-like inclusion found. |

## Requirement traceability

| ID | Requirement | Origin | Implementation | Test/oracle | Fresh run | Status | Gap |
|---|---|---|---|---|---|---|---|
| ADR-PLACEMENT / AT-KAFKA-002 | `eventlab` module, no internal leak | ADR 0002, spec | Present | Architecture test | 1/1 green | VERIFIED | None in reviewed delta. |
| KAFKA-T02..T05 / RA-001..003 | KRaft overlay, 3p RF1, no broker auto-create | ADR/spec | Present | BrokerKafkaIT | Container socket blocked | IMPLEMENTED_NOT_EXECUTED | No fresh broker proof. |
| AT-KAFKA-001/N | Surefire broker-free | ADR/spec | Contradicted by `EventLabRestAssuredTest` | Naming/exclude inspection | Static failure | CONTRADICTED | Move broker HTTP test to Failsafe. |
| KAFKA-T06..T08 / RA-010..015 | stable event, v1 envelope, key/headers, rollback | ADR/spec | Present | envelope unit + OutboxKafkaIT | Unit 4/4; Kafka blocked | PARTIAL | Rollback broker half not freshly run. |
| KAFKA-T09 / RA-016 | crash-heal incomplete publication → resubmit | spec/catalog | WIP seeds incomplete row and invokes resubmit API | OutboxKafkaIT | Blocked | IMPLEMENTED_NOT_EXECUTED | Does not prove real process restart, but does exercise resubmit. |
| RA-017/018 | partition behavior and same-key ordering | acceptance | Main-path keying present | RA-018 fallback oracle | Blocked | CONTRADICTED | Retry path loses ordering; broker observation optional. |
| RA-019/N | real lifecycle actions publish; idempotent replay does not duplicate | acceptance | Emitters exist; replay HTTP test exists | RA-019 synthetic, RA-019N real HTTP | Blocked | PARTIAL | Capture/cancel/refund test bypasses real lifecycle paths. |
| KAFKA-T10/T11 / RA-020..023 | processed persistence, duplicate/replay | spec | Table/listener/constraint present | PersistenceKafkaIT | Blocked | PARTIAL | Replay is re-produce, not restart/seek. |
| KAFKA-T12 / RA-024/028 | retry then custom DLT, malformed envelope survives | ADR/spec | RetryableTopic + custom names + DLT handler | DLT consume code | Blocked | PARTIAL | Attempt count unasserted; handler failures swallowed. |
| RA-025/026 | payment unaffected; purge only old processed | acceptance | Controller/purge service present | payment count around poison; weak purge test | Blocked | PARTIAL | Audit/business tables and old/new retention not proven. |
| RA-027 / RA-050 | duplicate/rebalance gives one DB effect | acceptance/E5 | Unique constraint present | manual consumer/repository simulation | Blocked | PARTIAL | Production listener rebalance/redelivery not exercised. |
| KAFKA-T13 / RA-030..033, SEC-001..006 | operate/read split, validation, masking | spec | Controller/security present | REST Assured | Blocked; also wrong Surefire seam | IMPLEMENTED_NOT_EXECUTED | Need Failsafe execution and broker injection. |
| KAFKA-T14 | BFF + Zod + composable | spec | Commit 2 broken; WIP fixed | schema/composable unit | 3/3 composable within 628 | PARTIAL | Add route-body/header contract test. |
| KAFKA-T15/T16 / PW-001..014 | thin UI + payment delivery card + UX/security | spec | UI/card/POM present | 22 discovered tests | List only | PARTIAL | Several named tests do not drive required behavior. |
| KAFKA-T20 / DOC | 45-minute runbook and Lenses telescope | spec | Docs present in Commit 2/WIP | review only | N/A | PARTIAL | Wrong retry count; evidence/status drift. |
| KAFKA-T18 / RA-050..052 | rebalance, seed guard, property hardening | spec | property test + WIP rebalance test | 100 jqwik tries; rebalance blocked | PARTIAL | Rebalance oracle bypasses production listener. |
| KAFKA-T17 / E4 | optional checkout inbox | ADR non-goal | Not implemented | N/A | N/A | NOT_IN_REVIEW_SCOPE | Correctly cancelled. |
| E6 | Kafka dashboard/console/lag product | ADR non-goal | Not implemented | N/A | N/A | NOT_IN_REVIEW_SCOPE | Correctly absent. |

## Critical questions 1–14

1. **Czy flag-off daje zero beanów i połączeń do Kafka? — Nieudowodnione / contradicted by test.** Test sam tworzy topics i klienta Kafka. Najmniejszy test: flag-off context z `bootstrap-servers` wskazującym pułapkę/spying client factory; asercja zero beanów producenta/listenera i zero prób połączenia. Wykryje przypadkową aktywację profilu lub externalizera.

2. **Czy rollback daje zero publication i broker record? — Częściowo.** DB `event_publication=0` i brak rekordu są zakodowane, ale KafkaIT nie wykonał się świeżo. Najmniejszy test: unikalny key, rollback TX, wymagany timeout braku publikacji i braku broker record. Wykryje publikację before-commit.

3. **Czy crash-heal tworzy incomplete publication i udowadnia resubmit? — Implemented, not executed.** WIP seeduje incomplete row i wywołuje API resubmit; nie restartuje procesu. Najmniejszy mocniejszy test: zamknąć/podnieść kontekst i obserwować jeden broker record. Wykryje brak startup resubmission.

4. **Czy duplicate/rebalance/restart daje jeden efekt DB przy redelivery? — Tylko constraint/duplicate; nie rebalance/restart.** Najmniejszy test: dwa rzeczywiste kontenery listenera w tej samej grupie, wymuszony stop jednego po poll przed commit, potem dokładnie jeden `(group,eventId)`. Wykryje złe ack/redelivery handling.

5. **Czy poison test konsumuje rekord z rzeczywistego custom DLT? — Kod tak, świeży run nie.** `RA-KAFKA-024` subskrybuje custom DLT i filtruje key, lecz runtime był zablokowany. Najmniejsza poprawka: dodatkowo wymagaj payload `eventId` i nagłówków attempt; usuń tautologiczną część `topic == DLT`. Wykryje złą trasę/nazwę.

6. **Czy retry count, backoff i topic names odpowiadają wersji Spring Kafka? — Nie w dokumentacji.** Kod oznacza 3 total attempts, fixed 500 ms, single retry topic i custom DLT; runbook mówi o 3 retry. Test nagłówków/timestampów wykryje off-by-one i złą nazwę.

7. **Czy ordering per targetId jest zachowany lub jawnie ograniczony? — Nie.** Main path jest keyed, ale non-blocking retry traci ordering. Test fail-first + later same-key event wykryje reordering; ADR/catalog musi zaakceptować ograniczenie albo zmienić retry.

8. **Czy inject wymaga operate, a read-only dostaje 403? — Zaimplementowane, niewykonane świeżo.** `@PreAuthorize` i test rozdziału read/operate istnieją. Minimalny Failsafe HTTP test z realnym broker inject wykryje authority regression i bypass.

9. **Czy tenant masking działa w list, detail, BFF i UI? — Częściowo.** Backend filtruje list/detail, REST testuje masked detail, UI testuje szerokie forbidden. Brakuje świeżej, przekrojowej macierzy list/detail/BFF/UI dla obcego tenant eventId. Taki jeden test wykryje BOLA/leak.

10. **Czy payment i audit są nienaruszone po poison/duplicate? — Payment częściowo; audit nie.** Poison KafkaIT porównuje count payment orders, Playwright porównuje status; brak pre/post audit oracle. Test powinien zapisać oba biznesowe stany/row counts i porównać po realnym duplicate/poison. Wykryje double-write/cross-module side effect.

11. **Czy Playwright udowadnia proof-of-delivery do 5 s bez protocol oracle? — Częściowo.** Polluje BFF/Event Lab, ale testy karty nie odwiedzają payment detail, a poison HTTP omija Kafka. Minimalny test: lifecycle action → payment detail card `pending` → `processed` ≤5 s, z API/DB biznesowym oraclem. Wykryje przerwany UI polling/BFF/listener.

12. **Czy docs/status nie deklarują więcej niż udowodniono? — Nie.** 25/25, 31/31 i zero OPEN są nadmiarowe względem kodu i kanonicznego evidence file. Automatyczny check manifestu AC→test→fresh log wykryje brak/niezgodność.

13. **Które produktowe pliki weszły do commitu o skilli i czy zasadnie? — Backend listener/BrokerKafkaIT, payment detail, schema test, BFF client/page/spec, dev-stack and Event Lab roadmaps.** Funkcjonalnie są związane z Event Lab, ale nie z deklarowanym skills-only commitem; wymagają osobnych commitów i uzasadnienia.

14. **Czy usunięcie/rename skilli złamało instrukcje lub routing? — Tak.** Broken `playwright-skill-upstream` symlink/README route and live `wayfinder` reference prove it. Resolver test symlinków i live Markdown paths wykryje ten typ regresji.

## Versioned research ledger

| Technology | Resolved/pinned version | Primary source checked | Version match | Review conclusion |
|---|---:|---|---|---|
| Java | 25.0.4 runtime; release 25 | [Oracle JDK 25 docs](https://docs.oracle.com/en/java/javase/25/) | Exact major | No Java-version defect. HIGH. |
| Spring Boot | 4.0.6 | local effective dependency tree; [Boot 4.0 API](https://docs.spring.io/spring-boot/4.0/api/java/) | Exact minor, site rolls patch | POM/runtime aligned. HIGH. |
| Spring Framework | 7.0.7 | local dependency tree; [Framework 7.0 docs](https://docs.spring.io/spring-framework/reference/7.0/) | Exact minor, rolling patch | No finding depends on patch-only API. HIGH. |
| Spring Modulith | 2.0.6 | POM/BOM and dependency tree; [Modulith 2.0 reference](https://docs.spring.io/spring-modulith/reference/2.0/) | Docs currently 2.0.7 | Placement/outbox conclusions are not patch-sensitive. MEDIUM-HIGH. |
| Spring Kafka | 4.0.5 | dependency tree; [4.0 retry configuration](https://docs.spring.io/spring-kafka/reference/4.0/retrytopic/retry-config.html), [retry pattern/order](https://docs.spring.io/spring-kafka/reference/4.0/retrytopic/how-the-pattern-works.html) | Docs currently 4.0.6 | Non-blocking ordering, attempts and topic bootstrap findings apply; patch mismatch disclosed. MEDIUM-HIGH. |
| Kafka clients / broker | client 4.1.2; overlay image 4.0.0 | dependency tree, compose; [Apache Kafka 4.1 docs](https://kafka.apache.org/41/documentation.html) | Client exact line; broker one minor lower | No client/broker incompatibility proven. MEDIUM. |
| Testcontainers | 2.0.5 | dependency tree; [Testcontainers for Java docs](https://java.testcontainers.org/) | Major/minor | Runtime could not access socket; no product failure inferred. HIGH for limitation. |
| REST Assured | 6.0.0 | POM; [REST Assured repository](https://github.com/rest-assured/rest-assured) | Exact local pin | Finding is Maven naming/placement, not API behavior. HIGH. |
| Nuxt | 4.4.6 | frontend package/lock; [Nuxt releases](https://github.com/nuxt/nuxt/releases) | Exact local pin | BFF return finding follows local `backendApi` owner contract. HIGH. |
| Playwright | 1.61.0 | package + CLI; [Playwright assertions](https://playwright.dev/docs/test-assertions) | Exact local CLI; docs rolling | Oracle findings are source-level, not version-sensitive. HIGH. |
| TypeScript | frontend 6.0.3; root lock 7.0.2 | package/lock | Mismatch by design location, not supported workspace | Root lock classified accidental. HIGH. |

## Ponytail review

`apps/backend/pom.xml:L219`: delete: `spring-retry` and global `@EnableRetry` have no caller; Spring Kafka owns this retry path. Remove dependency, import and annotation.

`apps/backend/src/main/java/lab/paymentquality/eventlab/internal/EventLabRetryTopicConfiguration.java:L24`: shrink: unused default DLT plus annotation/script/admin topic creation duplicate one manifest. Keep source + one retry + contract DLT under one explicit authority.

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:L1`: delete: root metadata is not packaged and duplicates dependency auto-configuration. Delete it.

`pnpm-lock.yaml:L1`: delete: unrelated 225-line root TypeScript 7 graph. Keep the owned frontend lock only.

`.agents/skills/java-spring-framework-upstream/references/:L1`: yagni: the 3,346-line vendor pack includes MongoDB, Redis, Cloud, GraphQL and microservices outside this modular-monolith lab and is not an executable skill. Keep the router plus only Boot/Framework/Security/Modulith references actually routed by the lab.

`apps/backend/src/main/java/lab/paymentquality/eventlab/internal/EventLabRetryNamingConfiguration.java:L23`: shrink: repeated delegate creation in four methods obscures a single DLT-name override. Cache one delegate/provider per properties instance or use the smallest supported naming customization; keep the exact contract DLT requirement.

`apps/backend/src/test/java/lab/paymentquality/eventlab/EventLabRebalanceKafkaIT.java:L107`: delete: manual production-code reimplementation is not a useful supplemental test. Replace it with a shorter real-listener rebalance/redelivery test, not another abstraction.

net: -2,200 lines possible.

## Validation evidence

| Command | Exit | Counts / duration | Warnings | Status |
|---|---:|---|---|---|
| `git diff --check BASE` | 2 | 2 trailing-whitespace findings; 0.1 s | lines 29–30 in Lenses roadmap | RED |
| `git diff --check COMMIT2` | 0 | clean; 0.2 s | none | GREEN |
| prohibited-pattern `rg` from prompt | 0 | matches inspected; 0.1 s | docs/learner text and three pre-existing `Thread.sleep`; no scoped Event Lab use | GREEN for reviewed delta |
| broken `.cursor/skills` symlink `find` | 0 | 1 broken link | `playwright-skill-upstream` | RED |
| skill validator | 0 | 43 skills; 0 structural errors; 28 warnings; 0.2 s | missing `When Not to Use` | GREEN_WITH_WARNINGS |
| `./mvnw -Dtest=ModulithArchitectureTest test` | 0 | 1 passed, 0 failed/error/skipped; Maven 22.939 s | Maven/Unsafe and unrelated compile deprecations | GREEN |
| `./mvnw -Dtest=EventLabEnvelopeTest,EventLabEnvelopePropertyTest test` | 0 | 4 passed; jqwik 100 tries; Maven 5.472 s | Maven/Unsafe | GREEN |
| `./mvnw -Dtest=ModulithArchitectureTest -Dit.test=EventLab*KafkaIT verify` | 1 | architecture 1 passed; Kafka stage 9 errors; Maven 18.753 s | container socket `Operation not permitted`, no valid Docker environment | ENVIRONMENT_BLOCKED |
| Maven dependency tree for Boot/Modulith/Kafka/Testcontainers/Retry | 0 | resolved versions; 3.574 s | Maven/Unsafe | GREEN |
| `corepack pnpm typecheck` | 0 | no TS errors; ~23.6 s | Nuxt icon bundle info only | GREEN |
| `corepack pnpm lint` | 1 | 1 error plus warnings; ~1 s | failing `paymentViewsStorage.ts` is unchanged from BASE | RED_PRE_EXISTING_NOT_WORSENED |
| `corepack pnpm test:unit` | 1 | 68 files / 628 tests passed; 2 unhandled worker errors; 193.21 s | icon/router warnings; two `onTaskUpdate` timeouts | RED despite green assertions |
| `corepack pnpm exec playwright test --config playwright.pom.config.ts --list` | 0 | 331 discovered in 75 files; 22 Event Lab tests; ~2 s | none material | GREEN_LIST_ONLY |
| live Playwright | — | not run | stack was not already established; credentials not inspected | NOT_RUN |
| Lenses MCP | — | not run | live `payment-lab` environment not established; no mutation needed for source review | NOT_RUN |

The KafkaIT command failed before product assertions because the sandbox could not access `/var/run/docker.sock`; it is not counted as a feature failure or PASS. The command selected only Event Lab KafkaIT and one architecture test, so excluded `restkit` and `paymentsupport` were not run.

## Environment limitations

- Container runtime access was denied (`java.net.SocketException: Operation not permitted`), blocking PostgreSQL/Kafka Testcontainers and all broker-level acceptance execution.
- The live Nuxt/Spring/Keycloak/Kafka stack was not already running. Per prompt, `dev-stack.sh --kafka` was not started automatically.
- No live credentials, storage-state files or Lenses secrets were read or printed.
- Spring Kafka exact 4.0.5 reference pages were not available through the documentation renderer; the 4.0 branch currently renders 4.0.6. Patch mismatch is disclosed in the ledger and confidence lowered where relevant.
- The requested `.kiro/steering/frontend-nuxt-ui.md` and `.kiro/steering/modern-web-guidance.md` paths are absent in this checkout; repository-native Nuxt/UI skills and current source were used instead.
- Frontend lint has a baseline error in a file unchanged since BASE; it is not attributed to these commits/WIP.
- Vitest executed every assertion green but its process gate is still red because of two worker RPC timeouts.

## Clean areas

- Commit ancestry and expected HEAD are exact.
- `ModulithArchitectureTest` is green; no `payment -> eventlab.internal` leak was found.
- Java 25 compile is green; no preview/JPMS/virtual-thread speculation was introduced.
- Event envelope unit/property tests are meaningful and green (4 tests, 100 property tries).
- Flyway/JPA ownership and unique `(consumer_group,event_id)` constraint are appropriate.
- WIP BFF list route fixes Commit 2’s wrapper/body error.
- Event Lab frontend typecheck is green; composable/schema unit tests pass within the suite.
- No `page.route`, `route.fulfill`, `routeWebSocket`, `kafkajs`, ECharts, AKHQ or kafka-ui product implementation was introduced in scope.
- Playwright uses real-stack POM conventions and unique test data in the sound cases.
- Tenant filtering and read/operate authorities are present in production code; no demonstrated tenant leak was found.
- The WIP poison KafkaIT does attempt a physical consume from the contract DLT and checks payment count, although it could not be executed here.
- No secret, token, real password, `.env` or generated storage state appears in the reviewed inventory.
- RF1, PLAINTEXT and single-node KRaft are correctly documented lab choices.
- Lenses remains an optional read-only telescope, not a product UI or CI oracle.

## Recommended repair order

1. Freeze completion claims: set unsupported PASS/DONE entries to `PARTIAL`/`IMPLEMENTED_NOT_EXECUTED`; separate the mixed commit history before it is shared.
2. Preserve the WIP BFF fix and move broker-dependent REST Assured to Failsafe.
3. Make duplicate/poison endpoints publish through the real Kafka path; remove direct DEAD mutation.
4. Decide retry ordering policy, correct the attempt budget, and let DLT handler failures propagate.
5. Replace backend simulation tests with real flag-off, lifecycle, replay, rebalance, ordering and purge oracles.
6. Rewrite/retitle the overstated Playwright cases; prove the payment-detail card, six states, flag-off nav, no POST and request-level leak constraints.
7. Consolidate topic provisioning to source + retry + contract DLT; remove Spring Retry, root auto-config metadata and root lockfile.
8. Repair skill symlink/routing and trim or explicitly justify the upstream reference pack.
9. On a working container/live environment, rerun the exact scoped Maven/Failsafe, frontend gates and targeted live POM. Only then restore PASS/DONE with fresh SHA/count/duration evidence.

## Final counts

| Severity | Count |
|---|---:|
| P0 | 0 |
| P1 | 9 |
| P2 | 6 |
| P3 | 2 |
| **Total** | **17** |

Final decision: **REQUEST_CHANGES**.
