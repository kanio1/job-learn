# Task-Force Review — kafka-event-streaming-proposal.md

Status: REVIEW_COMPLETE_AWAITING_P0_DECISION
Date: 2026-08-21
Reviewed: `.codex/research/kafka-event-streaming-proposal.md` (PROPOSED_NOT_APPROVED)
Panel: tech-lead, test-architect, product manager / BA, Kafka expert, PostgreSQL expert, Nuxt-UI dashboard reviewer.
Method: every load-bearing claim re-verified against the working tree on 2026-08-21 (paths cited below). Skills applied: `business-analysis-and-product-discovery-for-payment-lab`, `nuxt-dashboard-zod-pinia-frontend-engineering`, `playwright-pom`, `spring-modulith` rules from `AGENTS.md`.

---

## Verdict

**APPROVE_WITH_CHANGES.** The proposal is unusually honest: it rejects its own first draft (15 flows), keeps PostgreSQL as source of truth, refuses the microservice split, and correctly identifies the governance blocker. The task force endorses the architecture (Modulith outbox → one versioned topic → idempotent lab consumer) with the corrections below. **Nothing may be implemented until the user approves P0** (retire "No Kafka" non-goal + accept ADR 0002).

Top corrections (details in findings):

| # | Correction | Severity |
|---|---|---|
| C1 | Flyway claim stale: global max is **V36** (`tenant/V36__tenant_payment_policy.sql`), not V35 → Kafka reserves **V37+** | BLOCKER for stories |
| C2 | **Awaitility is not in `pom.xml`** — proposal assumes it; E1 must add it (test scope) or define a polling helper | MAJOR |
| C3 | Prefer **programmatic `EventExternalizationConfiguration` inside `eventlab`** over `@Externalized` on the shared record — keeps `shared` OPEN free of broker concerns and makes default apps never publish (resolves proposal open question §Uncertainty-3) | MAJOR |
| C4 | Stable `eventId` must be added to `AuditableActionOccurred` **and reused by OpsFeedBroker frames** (today `UUID.randomUUID()` per frame) — one fix, two dedup problems | MAJOR |
| C5 | Topic needs an explicit partition/headers/schemaVersion contract before any consumer story (K-4/K-1) | MAJOR |
| C6 | TS-only Kafka testing is **not** the right CI oracle here (SUT is JVM); decision matrix recorded — Java Testcontainers primary, optional read-only kafkajs probe under `tools/` | MAJOR (user question) |

---

## Fact-check vs working tree (2026-08-21)

| Proposal claim | Status | Evidence |
|---|---|---|
| Modules: merchant, payment, tenant, iam, audit, support, ops, checkoutlab, mirrorlab, rlslab, testing, shared; no eventlab | VERIFIED | `ls apps/backend/src/main/java/lab/paymentquality/` (+ `foundation`) |
| Only production event = `AuditableActionOccurred`; fields as listed; no stable eventId | VERIFIED | `shared/events/AuditableActionOccurred.java` — record has `Outcome outcome` enum + before/after maps, **no eventId** |
| No `spring-modulith-events-kafka`, no spring-kafka, no broker | VERIFIED | grep `apps/backend/pom.xml` — only `spring-modulith-events-api`, `-events-jpa` present |
| Testcontainers **2.0.5**, artifact family `testcontainers-*` | VERIFIED | pom BOM `testcontainers.version=2.0.5`; deps: testcontainers, -postgresql, -junit-jupiter, spring-boot-testcontainers |
| Compose = Postgres 18 + Keycloak 26.6.1 only; no kafka overlay | VERIFIED | `infra/compose/compose.yml`; overlays: app.http / app.mirror-flag-off / app.rls-flag-off / app.rls-spring-off / app / tls |
| `dev-stack.sh` modes --app/--tls/--full; no --kafka | VERIFIED | `scripts/dev-stack.sh` usage line |
| `platform:ops:inject` exists in Authorities + converter allowlist | VERIFIED | `shared/security/Authorities.java:88`, `KeycloakRealmRoleConverter.java:48` |
| `PaymentStatus` has no PAYMENT_FAILED (CREATED…REFUNDED) | VERIFIED | `payment/internal/domain/PaymentStatus.java` |
| OpsFeedBroker in-memory (`ConcurrentLinkedDeque`) | VERIFIED | `ops/internal/infrastructure/OpsFeedBroker.java` |
| Emitters/listeners exist (7 files reference the event) | VERIFIED | grep count across main sources |
| Frontend labs pattern + flagged nav (`NUXT_PUBLIC_*_ENABLED`) | VERIFIED | `app/pages/admin/*` (audit, checkout-lab, mirror-lab, network-lab, rls-lab, session-lab, support, tenant, users, visual-lab), flags in `nuxt.config.ts`; **no event-lab page yet** |
| Live POM precedents for this feature | VERIFIED | `tests-pom/specs/ops-feed.spec.ts`: `PW-OPS-E2E-120 BffClient capture → framereceived → row`, `PW-OPS-E2E-121 duplicate eventId is one row`, `PW-OPS-API-020 inject 201 admin / 403 readonly` |
| “Next free Flyway version after V35: V36+” | **STALE** | `db/migration/tenant/V36__tenant_payment_policy.sql` exists → next free **V37** |
| Awaitility available for consume assertions | **FALSE** | not in `apps/backend/pom.xml` (jqwik 1.9.2 is present) |
| Modulith 2.0.6 externalization API (`@Externalized("topic::key")`, programmatic config) | VERIFIED | Spring Modulith docs: `EventExternalizationConfiguration.externalizing().select(annotatedAsExternalized()).mapping(...).headers(...).routeKey(...)` |

---

## Findings per role

### Tech lead

- **TL-1 (BLOCKER, factual):** V36 is taken (`tenant/V36__tenant_payment_policy.sql`). All stories must reserve **V37+** for `eventlab`. The proposal's own rule ("Flyway remains the only schema owner") makes this a hard gate.
- **TL-2 (MAJOR):** Awaitility absent from the BOM-managed deps. Either add `org.awaitility:awaitility` (test scope, explicit version — it is NOT managed by Boot's BOM in all lines; pin it) in E1, or standardize a small `assertWithin(Duration, Supplier)` helper beside `PostgresContainerSupport`. Decide once, use everywhere.
- **TL-3 (MAJOR, design):** Do **not** annotate `AuditableActionOccurred` with `@Externalized`. Put `spring-modulith-events-kafka` + an `EventExternalizationConfiguration` bean **inside `eventlab`**, selected by `annotatedAsExternalized()`-equivalent programmatic selection (or package selection) so that:
  - default/dev/test profiles never publish (flag stays closed),
  - `shared` OPEN module gains no Kafka-facing API,
  - `ModulithArchitectureTest` keeps verifying boundaries unchanged.
  This resolves the proposal's open question and matches the house rule "no new public API in shared".
- **TL-4 (MAJOR):** Stable `eventId`: add a UUID component to the record with a back-compatible overloaded factory (house precedent: `Merchant.create` overloads). Populate at the single shared factory. Then: Kafka dedup key = `eventId`; ops feed frames reuse it instead of per-frame random UUID; `eventlab_processed (consumer_group, event_id)` unique constraint becomes meaningful. No DB migration needed for existing tables (new field rides the serialized payload in `event_publication`).
- **TL-5 (MINOR):** Dependency placement: `spring-modulith-events-kafka` must be a normal dependency but its auto-config is inert without externalized event types; still guard the config bean with `@Profile("kafka")` + `@ConditionalOnProperty(app.event-lab.enabled)` so Surefire contexts never wire a producer.
- **TL-6 (MINOR):** Root `CONTEXT.md` staleness (proposal §Corrections row 12) is real but is **E0 scope**, not Kafka scope — do not bundle a domain-map rewrite into the broker epic.

### Test architect

- **TA-1 (MAJOR, suite hygiene):** Kafka tests are Failsafe `*IT` only, named `*KafkaIT`, plus a dedicated `KafkaContainerSupport` singleton container (same pattern as Postgres support). Add `<surefire.excludes>` safety net for `**/*KafkaIT*.java` so nobody accidentally pulls a broker into `./mvnw test`. Default stack and `--app` POM stay broker-free (AC1 preserved).
- **TA-2 (MAJOR, determinism):** Never assert on global topic state (shared broker, cross-test pollution). Rules: unique consumer group per test (`group.id = test-<testId>`), correlation via unique business reference (`uniqueOrderReference(testInfo)`), Awaitility timeouts budgeted (≤10 s local), no `Thread.sleep`.
- **TA-3 (MAJOR, Playwright):** Eventual consistency in live E2E = `expect.poll` / web-first assertions on **visible text bound to the unique reference** (house precedent `PW-OPS-E2E-120`). Hard rules stay: no `page.route`, no `routeWebSocket`, no MSW; ops WS specs remain Kafka-free. New POM: `tests-pom/pages/EventLabPage.ts` (+ shared widget under `pages/components/` if the drawer/timeline is reused).
- **TA-4 (MAJOR, tooling — answers the user's TS-vs-Java question):**
  - What we actually test: JVM producer behavior (externalize-after-commit, nothing-on-rollback), JVM consumers (idempotency, retry/DLQ, rebalance), topic topology, UI visibility.
  - **TypeScript is possible** (`kafkajs` + `@testcontainers/kafka`) but wrong layer for the CI oracle: the system under test is Java; a Node consumer would only black-box-read what Java ITs already assert, while adding a second container runtime and flake surface. Browser/BFF must never speak Kafka (security rule) so Playwright cannot be the Kafka oracle by design.
  - **Decision: primary = Java** — `org.testcontainers:testcontainers-kafka` 2.0.5 (`org.testcontainers.kafka.KafkaContainer`, image `apache/kafka`), spring-kafka test kit (comes transitively with events-kafka), Awaitility.
  - **TS where it genuinely fits:** optional learner probe `tools/kafka-probe` (read-only `kafkajs` CLI: list topics, tail `lab.auditable-actions.v1`, print key/headers) for local labs — outside `apps/frontend/package.json`, zero impact on Nuxt deps/typecheck.
- **TA-5 (MEDIUM):** Missing from proposal's test matrix: seed-guard tests. Add IT asserting `POST /api/test/reset|seed` and DataLearningDataset paths work with `app.event-lab.enabled=false` and emit zero records (ADR 0001 carried into Kafka world).
- **TA-6 (MEDIUM):** Security matrix must include: merchant-scoped JWT gets empty/404 Event Lab rows cross-tenant; inject endpoint 403 for non-platform roles (mirror `PW-OPS-API-020`); no token/broker address ever in browser network log.

### Product manager / BA

- **PM-1 (MAJOR, business sharpness):** First slice has a real operator job-to-be-done hiding inside "Event Lab timeline": **"Did this capture reach downstream?"** — proof-of-delivery search by paymentOrderId/correlationId. Make UC-KAFKA-03 (search by paymentOrderId → see exactly one processed record with timestamp/group) the flagship acceptance path, not just "badge appears". This is what integration-support engineers do daily in real payment platforms.
- **PM-2 (MEDIUM, measurable ACs):** Add numbers: record visible in Event Lab ≤ 5 s after command on local stack; exactly one `eventlab_processed` row per eventId per group; DLQ row visible ≤ retry-budget. Vague "eventually" will flake both tests and learners' trust.
- **PM-3 (OK, sequencing):** Split Keep-now / Reshape / Defer / Reject is approved. E4 (checkout inbox over Kafka) stays **optional**; product Merchant Webhooks tab remains rejected until a spec retires that non-goal explicitly.
- **PM-4 (MINOR):** Carry the domain vocabulary (externalization, eventId, DLQ, poison pill, consumer group, replay) into `.codex/CONTEXT.md` glossary in E0/E5 — the lab teaches language, not just wiring.

### Kafka expert

- **K-1 (MAJOR, topology):** Create `lab.auditable-actions.v1` with **3 partitions** on the single-broker lab. One partition hides the key→partition lesson; three teach "order per key, interleaving across keys" which is exactly curriculum item #3. Replication factor 1 is honest for a lab.
- **K-2 (MAJOR, contract):** Pin the header set now: `eventId`, `action`, `targetType`, `tenantRef`, `correlationId`, `occurredAt`, `schemaVersion=v1` (UTF-8 strings). Payload = JSON envelope of the record + `eventId` + `schemaVersion`. Field-additive evolution only until a Schema Registry spec exists.
- **K-3 (MAJOR, semantics):** Producer side: `acks=all`, idempotent producer enabled (client default in modern versions) — teach it, don't build EOS. Consumer retries: `@RetryableTopic` (non-blocking, backoff index) **in eventlab only**, DLT = `lab.event-lab.dlq.v1`. Document loudly: Modulith externalizer failure ≠ consumer retry; the outbox republish-on-restart covers the former.
- **K-4 (MEDIUM):** Partition key = `targetId` (aggregate id). Confirmed correct vs first draft's correlationId mistake. For USER/SUPPORT_CASE targets the same rule holds (userId/caseId).
- **K-5 (MEDIUM):** Image: `apache/kafka` 4.x KRaft combined, PLAINTEXT, dual listeners (host `localhost:9092` / compose-network alias). Pin exact tag at T01 implementation time against Testcontainers 2.0.5 compatibility (proposal open question kept). No Confluent images (size/license).
- **K-6 (MINOR):** Be explicit in docs: PLAINTEXT lab means **no broker auth**; JWT guards HTTP/UI surfaces; tenant isolation is enforced by consumers + HTTP, not by Kafka ACLs. SCRAM stays a later profile. Also set consumer `auto.offset.reset=earliest` for the lab group so replay lessons work.

### PostgreSQL expert

- **PG-1 (OK + note):** Choosing Modulith externalizer over CDC/Debezium is right for this lab (outbox already exists; CDC would teach a different, infra-heavy lesson). Record the rejection in ADR 0002 alternatives.
- **PG-2 (MAJOR):** `eventlab` migration **V37**: table `eventlab_processed` — `id`, `consumer_group`, `event_id` (UUID), `action`, `target_type`, `target_id`, `tenant_ref`, `status` (PROCESSED|RETRYING|DEAD), `attempts`, `consumed_at`, `last_error`; **unique `(consumer_group, event_id)`**; index on `(target_id)` and `(consumed_at)` for the UI list. FK-free from business tables (consumer decoupling). JPA `ddl-auto: validate` must pass against it.
- **PG-3 (MEDIUM):** Retention: completed `event_publication` rows and `eventlab_processed` grow unbounded in a long-lived lab. Add a simple scheduled purge in eventlab (keep N days, configurable, default e.g. 7d) — cheap, realistic, and a good "operational Kafka adjacent" lesson. Do not couple purge to business tables.
- **PG-4 (MINOR):** Crash-window teaching: keep `republish-outstanding-events-on-restart: true` and add one IT proving commit-without-publish is healed on restart (this is the strongest honest at-least-once demo we have).

### Frontend / Nuxt-UI dashboard reviewer

- **FE-1 (MAJOR, placement):** `/admin/event-lab` mirrors the labs pattern: flag-gated nav (`NUXT_PUBLIC_EVENT_LAB_ENABLED`), sibling of Error Lab. Page anatomy per dashboard skill: page header + primary action (inject), filters row (`USelect` group/action/outcome), `UTable` records timeline, `USlideover` record detail (key, headers, payload, processed status), `UBadge` outcome/action, `UChip` partition/offset, `UAlert` DLQ banner. **No lag charts, no fake KPIs** (non-goal stands).
- **FE-2 (MAJOR, data path):** BFF proxy routes `server/api/event-lab/**` with query whitelist + Zod-before-render envelope schema (`app/schemas/event-lab.schema.ts`), composable `useEventLabApi` following `useAuditApi` shape (validate-all, safe metadata, detail-404→null). Forbidden state for missing `platform:event-lab:operate`. Loading/empty/filtered-empty/error states mandatory (six-state bar like audit).
- **FE-3 (MEDIUM, components):** Inject actions reuse ConfirmModal pattern (`data-testid="confirm-action-dismiss"` rule). Timeline uses `UTimeline` only if 4.7.1 API fits; fallback ordered list is acceptable (Ops feed precedent).
- **FE-4 (MEDIUM, POM):** `EventLabPage.ts` with intents `openRecord(ref)`, `expectRecordVisible(ref)`, `injectDuplicate(ref)`; fixtures `{ app, api }`; preconditions through `BffClient.eventLab*` methods; unique references from `data/factories.ts`.

---

## Corrections to apply to the proposal (if user approves P0)

1. Replace "V36+" with "**V37+** reserved for eventlab" everywhere (C1).
2. Add "add Awaitility (pinned) or house polling helper" to P1 deliverables (C2/TL-2).
3. Rewrite P2 bridge bullet: programmatic `EventExternalizationConfiguration` in `eventlab`, no `@Externalized` on shared record (C3/TL-3).
4. Extend P2: stable `eventId` on the record + OpsFeedBroker frame reuse (C4/TL-4).
5. Add topic contract block: 3 partitions, RF1, header set, envelope v1, `auto.offset.reset=earliest` (K-1/K-2/K-6).
6. Replace "Awaitility for consume assertions" with the full tooling decision incl. TS probe scope (TA-4).
7. Add seed-guard IT and retention purge to P2/P4 scope (TA-5/PG-3).
8. Make UC "proof-of-delivery search by paymentOrderId" the flagship first-slice acceptance path (PM-1/PM-2).

## Improvements (usprawnienia ponad propozycję)

- **Rebalance lesson without pain:** one IT starting a second consumer instance in the same group mid-test (E5) — teaches group semantics better than any slide.
- **Crash-heal demo:** restart-based republish IT (PG-4) — the most honest at-least-once story available on this stack.
- **Learner probe:** `tools/kafka-probe` (read-only kafkajs CLI) gives TS learners a legitimate Kafka touchpoint without violating "browser never speaks Kafka".
- **Glossary-first:** E0 adds the six Kafka terms to `.codex/CONTEXT.md` before any code lands.

## Wnioski (conclusions)

1. Architecture and sequencing of the proposal are sound; the panel found **no architectural blocker**, only factual drift (V36), missing dependencies (Awaitility), and contract gaps (eventId, headers, partitions) — all fixed in the roadmap docs.
2. Governance gate is real and respected: ADR 0002 drafted as **PROPOSED**; `AGENTS.md`/skills edits are E0 tasks executed only after explicit user approval.
3. Testing answer to the user's constraint ("TS jeśli możliwe, else Java"): **Java Testcontainers is the correct primary oracle** because the SUT is JVM-side; TypeScript enters as an optional read-only probe tool, and Playwright covers the user-visible surface (Event Lab UI/BFF) on the live stack with the same discipline as Ops Wave 2.
4. Next artifact upon approval: execute E0 (docs/ADR/skills/glossary), then Wave 1 (E1). Nothing starts before that approval.

## Decision request

- [ ] User approves P0 → retire "No Kafka" non-goal, accept ADR 0002, then E1–E3 waves proceed.
- [ ] User defers → this catalog stays DESIGNED_NOT_STARTED; proposal remains PROPOSED_NOT_APPROVED.
