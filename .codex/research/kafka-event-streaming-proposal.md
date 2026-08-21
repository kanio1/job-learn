# How should this lab add Kafka so we learn streaming, keep the modular monolith, and test it on our stack?

Status: PROPOSED_NOT_APPROVED  
Date: 2026-08-21  
Context: user asked the tech-lead / PM / BA / Kafka / Modulith / Keycloak / PostgreSQL / frontend team to confront the first draft with the live system. **No Kafka remains an active non-goal until the user retires it in `AGENTS.md` and an ADR exists.** This note is the refined proposal, not an implementation ticket.

Pinned versions (do not bump for Kafka): Java **25**, Spring Boot **4.0.6**, Spring Modulith **2.0.6**, PostgreSQL **18**, Keycloak **26.6.1**, Nuxt **4.4.6**, `@nuxt/ui` **4.7.1**, Playwright **1.61.0**, Testcontainers **2.0.5**. Live Modulith HTML may show 2.1.x (Namastack/JobRunr outbox modes). Follow **2.0.6 APIs already in this tree**.

## Answer

Add Kafka as a **lab overlay on the existing Spring Modulith outbox**, not as a second source of truth and not as a microservice split.

1. Keep PostgreSQL 18 + Flyway + JPA `ddl-auto: validate` as the business source of truth (`payment_orders`, `merchants`, `support_cases`, `audit_event`, `ops_notifications`, `checkout_event`).
2. Keep `event_publication` (shared Flyway `V6`) as the transactional outbox. Kafka is **event externalization** via `spring-modulith-events-kafka` (Modulith 2.0.6): a transactional listener that publishes **selected** application events after commit, guarded by the same registry (`republish-outstanding-events-on-restart: true` already on).
3. Start from the **one event type we actually have** — `lab.paymentquality.shared.events.AuditableActionOccurred` — on **one versioned topic** `lab.auditable-actions.v1`. Do not invent five topics in wave 1; the Java type is one record, and Modulith routes by type unless we add `EventExternalizationConfiguration`.
4. Keep in-process `@ApplicationModuleListener` consumers (`AuditEventListener`, `OpsFeedEventListener`). Kafka consumers are **additional** (webhooks, Event Lab, later multi-instance ops). They must **not** double-write `audit_event`.
5. Ship Kafka behind a compose overlay + `app.event-lab.enabled` (same pattern as checkout/mirror/RLS labs). Default `scripts/dev-stack.sh` and Surefire stay Postgres+Keycloak only.
6. Teach and test: keys/ordering, consumer groups, at-least-once + idempotency, retry/DLQ, Testcontainers Kafka. Defer Kafka Streams, EOS, Schema Registry, SCRAM, and compacted changelogs until those concepts have a real lab surface.

The original 15-flow catalog is a **curriculum backlog**, not a first delivery. First product-shaped slice after governance: **lifecycle command → outbox → Kafka → idempotent consumer → visible dashboard/lab effect**. Iteration 2 (2026-08-21) re-derived the catalog from **classic industry Kafka cases** (FinServ backbone, proof-of-delivery, webhook dispatch with retry/DLQ, PSP callback bus, omnichannel fan-in, fraud/velocity, reconciliation, competing-consumer export, audit replication, compacted state, delayed actions, identity fan-out, disputes, ops broadcast, clickstream): verdicts, PG18 tie-ins and decisions log live in [kafka-payment-business-cases-v2.md](kafka-payment-business-cases-v2.md). Wave-1 delivery surface is unchanged: cases **1 + 2 (+3 reshaped)**; new adopted items: V37 uses PG18 `uuidv7()` PK (D-1), runtime docs lead with **Podman Compose rootless** single-node KRaft, auto-create OFF + explicit topic creation in `--kafka` mode (D-2/D-3), case 2 proof-of-delivery is the flagship acceptance path (D-4).

## Why it matters here

This lab already solved the hard prerequisite that `docs/architecture/payment-gateway-roadmap-analysis.md` named for Kafka: a durable event model and outbox in PostgreSQL. Adding a broker now teaches streaming **on top of** REST, JWT, tenant isolation, and Flyway — it does not replace them.

Doing all 15 original flows at once would:

- fight Ops Wave 2 (`Nie Kafka` for the live WS feed),
- invent product merchant webhooks while `AGENTS.md` still lists webhooks as a non-goal,
- treat Kafka as a delayed-job scheduler (it is not),
- write `audit_event` twice,
- look like a fake KPI dashboard (consumer-lag gauges on Overview).

## Corrections to the first draft

| First-draft claim | Verified on 2026-08-21 | Consequence |
|---|---|---|
| “Most prerequisites exist” | **Outbox and emitters: yes. Kafka: no.** Compose is Postgres 18 + Keycloak 26.6.1 only. No `spring-modulith-events-kafka`, no broker, no Testcontainers Kafka. | P1 is infra + flag, not “flip a switch”. |
| Five topics from existing emitters | All emitters publish **one** record: `AuditableActionOccurred`. Modulith Kafka uses type → topic unless `EventExternalizationConfiguration` / `@Externalized("topic::key")` routes it. | Wave 1 = one topic + routing key. Split topics later by `targetType` / `action`. |
| Kafka replaces in-memory ops feed | `OpsFeedBroker` is in-process (`ConcurrentLinkedDeque`, 100 frames). Flyway `ops/V34` already has `ops_feed_event` + `ops_notifications` unique `(recipient_subject, event_id)`. Epic E6 explicitly: **Nie Kafka**. Frontend is same-origin Nitro `ws` → Spring with session JWT (`server/api/ops/feed.ts`). | Keep WS contract. Kafka may **fan-in** to the broker later; do not rip out WS for learning Kafka. |
| Second Kafka consumer writes `audit_event` | `AuditEventListener` already persists from the same event. A second sink **duplicates** rows unless it is a separate lab table. | Audit stays in-process. Kafka audit sink = Event Lab copy / export, not production `audit_event`. |
| Delayed refund / support SLA “without schedulers” | Kafka has **no native delayed topic**. Lab already has `@Scheduled` workers: `PaymentExpirationService`, `PaymentExportJobWorker`, `CheckoutLabInboxWorker`, `CheckoutLabReconcileService`. Dual-control is Postgres `payment_refund_approvals` + PIN `payment_refund_challenges`. | Keep schedulers for time. Kafka retry topics teach backoff, not “wait until 17:00”. |
| Export request/reply topics | Export is already async HTTP: `202` + `Location` + DB poller (`payment.export-jobs.worker-delay-ms: 250`). | Keep the HTTP contract. Optional later: competing consumers **instead of** the poller, not Kafka request/reply. |
| Product Merchant → Webhooks tab | Checkout Lab already has HMAC notify (`CheckoutLabNotifier` / `CheckoutLabSignatureService`), inbox (`checkout_event.process_status` RECEIVED…DUPLICATE), delivery log, unique `event_id`. AGENTS still: no webhooks as product. | First webhook-over-Kafka path = **checkout-lab**, not a new merchant product API. |
| Risk-rules streaming closes MERCHANT_RISK_FLAGGED | `MERCHANT_RISK_FLAGGED` is a **manual** `PATCH /api/merchants/{id}/risk-flag` (`platform:merchants:update-risk-flag`). No `PAYMENT_FAILED` in `PaymentStatus` (CREATED…REFUNDED). Inbox type `PAYMENT_FAILED` is **inject/chaos**, not a lifecycle emit. | Auto risk-flag from a window is a **new product rule**. Do not pretend it exists. Kafka Streams is P4 Event Lab, not payment-module production. |
| SCRAM-SHA-512 on day one | Local Keycloak runs HTTP; TLS is `--full`. Mixing broker SASL into P1 fights the lab’s “plaintext compose first” pattern. Kafka SASL ≠ Keycloak OIDC. | P1 PLAINTEXT KRaft. SCRAM = later `kafka-lab` profile. JWT still guards HTTP/WS. |
| Schema Registry / EOS / compaction in the same catalog | Those are different products (Confluent SR, Kafka Streams, transactional producers). | Curriculum P4 only, behind Event Lab. |
| `org.testcontainers:kafka` | Testcontainers **2.0.5** artifact is `org.testcontainers:testcontainers-kafka`. Class: `org.testcontainers.kafka.KafkaContainer` + image `apache/kafka` (not deprecated `org.testcontainers.containers.KafkaContainer`). | Match the BOM already in `apps/backend/pom.xml`. |
| CONTEXT.md / status index as current domain | Root `CONTEXT.md` is stale (merchant+payment only). Working tree already has modules `support`, `ops`, labs, IAM saved views. Roadmap still says Ops Wave 2 DESIGNED_NOT_STARTED while the code is present. | Treat **working tree** as Kafka baseline. Update CONTEXT/AGENTS in P0, not as a side effect of coding Kafka. |
| Namastack / JobRunr outbox | Spring Modulith **2.1** docs. This repo pins **2.0.6**. | Do not add those starters. Native `spring-modulith-events-kafka` is the 2.0.6 path. |

## Current system (verified)

### Stack

| Layer | What we run |
|---|---|
| Compose | `infra/compose/compose.yml`: Postgres 18 + Keycloak 26.6.1. App overlays: `compose.app.yml` / TLS. **No broker.** |
| Operator | `scripts/dev-stack.sh` (host hybrid) or `--app` (POM). Passwords only via env. |
| Backend | Modular monolith `lab.paymentquality.*`. Flyway locations already include tenant, merchant, payment, shared, audit, checkoutlab, mirrorlab, rlslab, testing, support, ops, iam. Next free version after V35 (saved views): **V36+**. |
| Security | JWT resource server. Realm roles → `platform:*` / `merchant:*`. Tenant via `tenant_id` claim; merchant scope via `merchant_id`. WS: token **not** in browser; Nitro attaches Bearer. |
| Frontend | Nuxt 4 dashboard, Zod-before-render, Pinia only for shared state, BFF `$fetch`. Labs: Error, Checkout, Mirror, RLS, Session, Network, Visual. |
| Tests | JUnit 6 + REST Assured in `apps/backend/.../rest`. Playwright live `tests-pom` (no `page.route` / `routeWebSocket`). Exclude `restkit/` and `paymentsupport/` unless asked. |

### Modulith modules

| Package | Role | Kafka note |
|---|---|---|
| `merchant` | Registry, risk flag, import previews, contact PATCH, public eligibility API | Emits merchant actions. Must not import `payment.internal`. |
| `payment` | Orders, lifecycle, export jobs, dual-control refund, PIN challenge, expiration | Emits payment actions. Source of truth stays `payment_orders`. |
| `tenant` | Tenants + settings | Isolation filter for every consumer. |
| `iam` | Keycloak Admin REST (no local user table), saved payment views | Emits USER_*. Mirror-lab identity copy is a **new** consumer, not existing. |
| `audit` | `audit_event` from `@ApplicationModuleListener` | Keep. No Kafka rewrite. |
| `support` | Cases NEW→IN_PROGRESS→WAITING→RESOLVED, bulk assign, ETag | Emits SUPPORT_CASE_*. |
| `ops` | WS feed, inject, notifications inbox | Keep in-process path. Optional later Kafka fan-in. |
| `checkoutlab` | Redirect+notify, HMAC, inbox, reconcile anomalies | Best first **external** Kafka boundary (already async). |
| `mirrorlab` / `rlslab` / `testing` | Flag-gated labs + learning ETL / seeds | Do not publish domain events from `DataLearningDataset` (ADR 0001). |
| `shared` | OPEN: security, `AuditableActionOccurred` | Annotate / route the event here. Do **not** dump Kafka consumers into OPEN. |

Proposed new module (when approved): **`eventlab`** — flag-gated, not OPEN, owns broker wiring, Event Lab HTTP inject, DLQ inspector, **not** business writes to payment/merchant tables except through public APIs / events.

### Event contract (the only production event today)

`AuditableActionOccurred`: `occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantRef, correlationId, outcome, beforeState, afterState`.

No PAN, no tokens, no raw Authorization. Kafka payloads must keep that discipline. `correlationId` is the tracing key; **partition key** must be the aggregate id (`targetId` or `afterState.merchantId` / paymentOrderId), not correlationId (the first draft keyed audit by correlationId — that **destroys per-payment ordering**).

### Emitters and actions (complete list)

| Module | Actions | `targetType` |
|---|---|---|
| `MerchantService` | MERCHANT_CREATED, MERCHANT_ACTIVATED, MERCHANT_SUSPENDED, MERCHANT_RISK_FLAGGED, MERCHANT_RISK_FLAG_CLEARED, MERCHANT_RENAMED, MERCHANT_UPDATED | MERCHANT |
| `PaymentLifecycleService` | PAYMENT_AUTHORIZED, PAYMENT_CAPTURED, PAYMENT_CANCELLED, PAYMENT_REFUNDED | PAYMENT_ORDER |
| `PaymentExpirationService` | PAYMENT_EXPIRED | PAYMENT_ORDER |
| `PaymentRefundApprovalService` | REFUND_APPROVAL_NEEDED | PAYMENT_ORDER |
| `UserManagementService` | USER_CREATED, USER_UPDATED, USER_ROLES_ASSIGNED | USER |
| `SupportCaseService` / `SupportCaseAssignmentWriter` | SUPPORT_CASE_CREATED, SUPPORT_CASE_MOVED, SUPPORT_CASE_ASSIGNED | SUPPORT_CASE |

In-process listeners:

- `audit` → `audit_event` (every action).
- `ops` → live frame if mappable; notification **only** for REFUND_APPROVAL_NEEDED, SUPPORT_CASE_ASSIGNED, and inject-only PAYMENT_FAILED.

### Business flows that already exist (do not rebuild)

1. **Merchant registry** — create / list / get / activate / suspend / rename / contact PATCH / risk flag / import preview. Optimistic lock: **412** + `If-Match`.
2. **Payment order** — merchant-scoped create/read/list/summary/history, authorize → capture | cancel | expire, refund, metadata PATCH, evidence, notes. Idempotency-Key + ETag.
3. **Dual-control refund** — maker request → REFUND_APPROVAL_NEEDED → checker approve; self-approve **409**; PIN challenge is **not** Keycloak OTP.
4. **Export jobs** — POST job → 202 Location → worker poll → download.
5. **IAM** — Keycloak users/roles; tenant attributes; saved views in Postgres `user_saved_views` (V35).
6. **Audit log** — read-only `/api/audit`, tenant-aware, before/after drawer.
7. **Support queue** — Kanban, bulk assign partial success, tenant/merchant ownership.
8. **Ops feed + Notification Center** — WS + GET recent + inject + inbox unique key.
9. **Checkout Protocol Lab** — 302 hosted checkout, signed notify, inbox claim-batch, reconcile → `checkout_anomaly` unique `(session_id, kind)`.
10. **Labs** — Error / Mirror / RLS / Session / Network / Visual. Pattern for Event Lab UI.
11. **Seeds** — `DeterministicDataset` (104 orders) vs `DataLearningDataset` (ADR 0001). Learning seed **must not** flood Kafka via domain services.

### Frontend surface (map Kafka UI here, nowhere else)

Sidebar today: Overview · Merchants (Registry, Payment Orders) · Users · Audit · Support · Error Lab · Checkout / Mirror / RLS (flags).

Already-useful components: `OpsFeedPanel`, `NotificationCenter`, `PaymentKanban`, `SupportKanban`, `RefundApprovalsCard`, `PinChallengeComponent`, `MerchantImportModal`, risk badge on `MerchantTable`, export job panel, `SavedViewsPanel`.

**Event Lab** should be a **new flagged nav item** (`/admin/event-lab`), sibling of Error Lab — not lag charts on Overview (that would be a fake KPI dashboard, still a non-goal).

### Keycloak / Postgres roles in a Kafka world

- Broker auth is **not** a realm role. Do not put Kafka credentials in Keycloak or in the JWT.
- HTTP inject / Event Lab operate: add `platform:event-lab:operate` into existing **PLATFORM_ADMIN** / **PLATFORM_OPERATOR** composites (same additive style as `platform:ops:inject`). No new composite role.
- Consumers must filter `tenantRef` / merchant the way `OpsFeedBroker` uses `merchant_id`. A leaked topic is not an authorization bypass — UI and HTTP still JWT-gated; Kafka is an internal bus.
- Flyway remains the only schema owner. Consumer dedup tables live in `eventlab` (or checkoutlab if that lab owns the inbox-over-Kafka row).

## Proposed architecture (lab-shaped)

```
HTTP command (JWT, tenant, If-Match, Idempotency-Key)
        │
        ▼
 domain TX + ApplicationEventPublisher(AuditableActionOccurred)
        │
        ├─ event_publication (outbox, KEEP)
        │     ├─ @ApplicationModuleListener audit → audit_event
        │     ├─ @ApplicationModuleListener ops  → WS broker + ops_notifications
        │     └─ Modulith Kafka externalizer     → topic lab.auditable-actions.v1
        │                                              key = aggregateId (targetId)
        │                                              headers: action, tenantRef, correlationId
        │
        └─ PostgreSQL row is still the oracle

Consumers (same JVM unless Event Lab says otherwise):
  eventlab.webhook-dispatcher   → HMAC HTTP (reuse CheckoutLabSignatureService) + delivery log + DLQ
  eventlab.inspector            → GET recent / lag-for-learning (lab only)
  ops (optional later)          → extra fan-in to OpsFeedBroker (dedup on eventId)
```

Principles:

- **At-least-once into Kafka.** Exactly-once is a P4 demo, not the platform default.
- **Idempotent consumers** using existing unique keys (`checkout_event.event_id`, `ops_notifications (recipient_subject, event_id)`, new `eventlab_processed (consumer_group, event_id)`).
- **Ordering** only per partition key (paymentOrderId / merchantId / caseId). Never promise global order.
- **Modular monolith stays.** No payment → `merchant.internal`. New Kafka code is not a reason to split processes.
- **Feature flag** `app.event-lab.enabled` + Spring profile `kafka` so `@ApplicationModuleTest(STANDALONE)` and default Surefire do not need a broker.

### Topic convention (when we split)

Wave 1: `lab.auditable-actions.v1`  
Later (routing function on `targetType` + `action` prefix):

| Topic | Keys | Consumers |
|---|---|---|
| `lab.payment.lifecycle.v1` | paymentOrderId | webhook dispatcher, Event Lab, optional ops |
| `lab.merchant.lifecycle.v1` | merchantId | risk lab, import progress |
| `lab.support.cases.v1` | caseId | SLA lab (scheduler still owns time) |
| `lab.iam.users.v1` | userId | optional mirror-lab |
| `lab.event-lab.dlq.v1` | original key | inspector |

JSON envelope v1 (Jackson, same fields as the record + `eventId` hashed from `correlationId+action+targetId+occurredAt` or a dedicated UUID in `afterState` — **open question**: today’s event has no stable `eventId`; ops feed currently `UUID.randomUUID()` per frame, which is bad for Kafka idempotency). **P2 must add a stable event id** (column in afterState or a new field on the record) before consumers.

### Infrastructure

- Optional overlay `infra/compose/compose.kafka.yml`: single-node KRaft, official `apache/kafka` **4.x**, PLAINTEXT, advertised listeners for host (`localhost:9092`) and compose network (`payment-quality-kafka:19092`). Pattern matches Apache’s combined-role KRaft examples (no ZooKeeper; Kafka 4 removed ZK).
- UI: AKHQ or kafka-ui **only** on that overlay (operator learning), not in `--app` POM path unless asked.
- `scripts/dev-stack.sh --kafka` later; default stack unchanged.
- Tests: `KafkaContainerSupport` beside `PostgresContainerSupport`; image `apache/kafka` (or `apache/kafka-native`) via `org.testcontainers.kafka.KafkaContainer`. Awaitility for consume assertions. Do not start Kafka in every module test.

## BA discovery pack

### 1. Capability proposal

**Working name:** Event Streaming Lab (Kafka on Modulith outbox)  
**Why now:** Outbox, typed audit actions, checkout inbox, ops WS, and dual-control already exist. Kafka is the missing **broker** lesson, not the first async lesson.  
**Roadmap fit:** After Ops Wave 2 WS is accepted as the in-process feed. Sibling to Checkout Lab, not a replacement for Merchant 360 or payment REST.

### 2. Business goal

Operators and merchant systems should observe **the same lifecycle facts** the dashboard already shows, asynchronously and replayably, without coupling to one Spring instance’s memory.

Not solving it: we cannot teach consumer groups, poison pills, or replay; ops feed dies on a second instance; checkout notify stays poller-only.

### 3. Actors

| Actor | Goal |
|---|---|
| Platform operator | See that a capture happened even if they were on another node; inspect DLQ in Event Lab |
| Merchant manager | (Later) receive signed status webhooks; never see other tenants’ events |
| Support agent | Inbox already works; Kafka must not duplicate notifications |
| SDET / learner | Drive authorize via REST, assert Kafka message + idempotent second consume |
| Checkout Lab “PSP” | Keep HMAC notify contract; optional Kafka between receive and inbox worker |

Keycloak users stay the five composites. No Kafka principal in the realm.

### 4. Business workflow (first slice)

**Trigger:** successful `POST .../authorize` (or capture/cancel/refund) on a merchant-scoped payment order.  
**Main path:** row updates + `PAYMENT_*` event → outbox → in-process audit/ops → Kafka `lab.auditable-actions.v1` keyed by paymentOrderId → Event Lab consumer stores processed id → UI badge or Event Lab timeline.  
**Alternate:** listener/externalizer fails → `event_publication` stays incomplete → republish on restart.  
**Failure:** poison JSON → retry topic then DLQ; business row **unchanged** (Postgres already committed).  
**Non-goals of first slice:** merchant subscription CRUD, SLA timers, Streams windows, replacing WS.

### 5. Business rules

- Source of truth = PostgreSQL command side. Kafka is a **projection/integration** bus.
- Tenant isolation: consumer ignores or DLQs events whose `tenantRef` is not allowed for that subscription; HTTP/WS still JWT.
- Idempotency: same `eventId` processed twice → no second webhook / no second notification row.
- Confidentiality: same as audit contract.
- Time-based work stays on existing schedulers.
- Deterministic seed and learning seed must not require a live broker for current REST/UI tests.

### 6. Domain vocabulary

| Term | Meaning here |
|---|---|
| Application event | Spring `AuditableActionOccurred` inside the monolith |
| Outbox | `event_publication` (Modulith registry), not a new table |
| Externalization | Modulith publishing that event to Kafka after commit |
| Topic v1 | JSON schema for `AuditableActionOccurred` (+ stable `eventId`) |
| Consumer group | Competing workers (export/webhook). Ops feed is **broadcast** (many groups or WS fan-out), not competing |
| DLQ | `lab.event-lab.dlq.v1` + Event Lab UI, not a silent log |
| Inbox | Checkout `checkout_event` claim-batch — keep the name even if Kafka feeds it |

### 7. Data needs

- Stable **eventId** (missing today for Kafka idempotency).
- Topic payload = existing safe fields + key.
- Dedup table if no natural unique key.
- Delivery attempts (checkout already has a log; product webhooks would need one).
- Lab inject endpoints (mirror `POST /api/ops/feed/inject`) for poison / duplicate / delay.

### 8. Candidate acceptance criteria

1. Given Event Lab disabled, existing Surefire (exclusions as in AGENTS.md) stays green **without** a Kafka container.
2. Given Event Lab enabled + broker up, authorize of payment `c1` publishes one Kafka record to `lab.auditable-actions.v1` with key = paymentOrderId and header `action=PAYMENT_AUTHORIZED`.
3. Replaying the same record does not insert a second `audit_event` (audit still in-process once) and does not insert a second Event Lab processed row.
4. A poison payload lands on DLQ after configured retries; payment status in Postgres is unchanged.
5. Merchant-scoped JWT cannot read another merchant’s Event Lab rows / WS frames (same as today’s `merchant_id` filter).
6. Playwright live: no `page.route` / `routeWebSocket`; drive real authorize + Event Lab UI or ops feed.
7. `ModulithArchitectureTest` green; `eventlab` is not OPEN; payment still does not import `merchant.internal`.

### 9. Ambiguities (must survive into a spec)

1. Stable `eventId` on `AuditableActionOccurred` vs wrapping envelope only at the Kafka mapper.
2. When to split topics vs keep one topic + header filter.
3. Whether ops WS should consume Kafka or stay in-process forever (recommend: stay in-process until a second Spring instance is a real lab goal).
4. Product merchant webhooks vs checkout-lab-only notify.
5. Pin exact `apache/kafka` tag (4.0.x vs 4.1.x) at P1 against Hub + Testcontainers compatibility.
6. Jackson 3 (`tools.jackson` already in checkout notifier) vs Boot 4 Kafka serializer defaults.
7. Whether `--kafka` is part of `--app` POM or a third mode (recommend **third mode** so live POM Wave 2 stays Kafka-free).

### 10. Tester lens

Highest risks: lost messages (externalizer down), duplicates (at-least-once), **cross-tenant leak on a topic**, tests that hang waiting for Kafka, Playwright flaking on eventual consistency, double notifications, learning seed flooding the broker.

Already testable: REST lifecycle matrix, WS inject, checkout HMAC, unique indexes.

Hard if left vague: event id, “did Kafka cause this UI row or did the in-process listener?”, rebalance mid-test.

### 11. Sequencing recommendation

**Split. Do not implement 15 flows.**  
**Next (after explicit P0 governance):** P1 infra overlay + P2 one topic + Event Lab consumer.  
**Defer:** Streams, EOS, compaction, Schema Registry, SCRAM, product webhooks, Kafka-backed ops feed, export request/reply, support SLA via retry topics.  
**Reject as premature:** microservice split, Kafka as command bus replacing REST, PAN in payloads, fake Overview KPIs, bumping Modulith to 2.1 for Namastack.

### 12. Spec input summary

- **Title:** Event Streaming Lab — Modulith outbox to Kafka  
- **Intent:** Externalize existing `AuditableActionOccurred` to a versioned Kafka topic so learners can see keys, groups, retries, and DLQ on this payment lab, without moving source of truth off PostgreSQL.  
- **Scope:** Governance, compose overlay, `spring-modulith-events-kafka` on 2.0.6, one topic, Event Lab module + page, Testcontainers Kafka tests, checkout-lab optional consumer later.  
- **Non-goals:** PSP, PCI, microservices, production OIDC completion, fake KPI dashboard, replacing Ops Wave 2 WS, product webhook marketplace in wave 1.  
- **Must-preserve:** REST contracts, 412 vs 409, JWT/BFF, Flyway-only schema, restkit/paymentsupport exclusion, live Playwright rules.  
- **Open questions:** list in §9.

## Refined flow catalog (original 15)

Legend: **Keep-now** / **Keep-later** / **Reshape** / **Defer** / **Reject**.

| # | Original | Verdict | Lab mapping | Concepts | Tests |
|---|---|---|---|---|---|
| 1 | Payment lifecycle stream | **Keep-now** (core) | Existing `PAYMENT_*` → `lab.auditable-actions.v1` key=paymentOrderId. UI: Event Lab timeline, **not** a new badge on every payment row in wave 1 (optional later on Payment Detail). | Keys, partitions, ordering per payment | RA: authorize then Awaitility consume. PW: Event Lab row after live capture. |
| 2 | Merchant webhook dispatcher | **Reshape** | Do **not** add Merchant → Webhooks tab while webhooks are a non-goal. Reuse **CheckoutLabNotifier** HMAC + delivery log. Product subscriptions = later spec that retires that non-goal. | Groups, retry, DLQ, idempotent HTTP | WireMock + Kafka Testcontainers; duplicate event_id. |
| 3 | Audit fan-out writing `audit_event` | **Reject** as designed | Audit stays `@ApplicationModuleListener`. Optional Event Lab “audit copy” table for dual-path teaching. | Fan-out vs double-write | Assert `audit_event` count = 1 after Kafka retry. |
| 4 | Multi-instance ops feed | **Defer** | Keep `OpsFeedBroker` + `ops_feed_event`. Kafka fan-in only when we run two Spring instances on purpose. | Broadcast vs competing consumers | Existing PW-OPS WS tests stay Kafka-free. |
| 5 | Notification Center consumer | **Reshape** | Table and unique key **already exist**. Do not consume Kafka into the same table as the in-process listener (duplicates). Kafka path = Event Lab **or** replace the listener in a later wave, not both. | Effective exactly-once via unique constraint | RA unique violation / ON CONFLICT. |
| 6 | Refund dual-control delay topics | **Defer / reshape** | Keep Postgres approvals + PIN. SLA countdown on `RefundApprovalsCard` can use **created_at + scheduler**, not Kafka delay. | Time ≠ Kafka | Existing dual-control RA/PW. |
| 7 | Export request/reply | **Reject** as Kafka RPC | Keep 202+Location. **Keep-later:** competing consumers replacing `PaymentExportJobWorker` poller only. | Competing consumers, rebalance | Two `@KafkaListener` instances in one test (careful isolation). |
| 8 | Bulk merchant import pipeline | **Keep-later** | `MerchantImportModal` + V25 previews already exist. Emit per-row events after commit for progress WS/Event Lab. | Error containment | Partial failure report already a UI concern; Kafka is optional progress. |
| 9 | Checkout notify-over-Kafka | **Keep-now (second slice)** | Inbound signed notify still HTTP; worker consumes Kafka **or** stays `@Scheduled`. Teach inbox-over-Kafka without dropping HMAC. | External integration | Existing CPL RA + new consume test. |
| 10 | Reconciliation anomaly detector | **Keep-later** | `CheckoutLabReconcileService` + `uk_checkout_anomaly_session_kind` already. Kafka join is optional; Postgres join is the honest first oracle. | Stateful join vs SQL | Keep SQL reconcile tests; Streams is P4. |
| 11 | Risk-rules streaming | **Defer** | Risk flag is manual PATCH. Auto-flag is new product. Prefer consumer + SQL window on `payment_orders` before Kafka Streams. | Windowing, EOS | Decision table tests on the **rule**, independent of broker. |
| 12 | Compacted current-state topic | **Defer** | Reconcile PG vs compacted `payment.state.current` in Event Lab only. OLTP remains oracle. | Compaction | Lab assertion, not production read path. |
| 13 | User provisioning fan-out | **Keep-later** | USER_* already emitted. Mirror-lab consumer is new. Schema v1→v2 without Schema Registry first (additive JSON). | Fan-out, evolution | IAM RA + Event Lab. No secrets in payload. |
| 14 | Support SLA escalation | **Defer** | Cases have HIGH + timestamps. Use scheduler (like expiration). Kafka retry ≠ SLA clock. | Delayed redelivery myth | Support Kanban aging can be SQL. |
| 15 | Event Lab page | **Keep-now (UI of the lab)** | `/admin/event-lab`: topic list, last N records, inject duplicate/poison, DLQ table. Zod envelope. Flag `NUXT_PUBLIC_EVENT_LAB_ENABLED`. | Observability without fake KPIs | PW-E2E live; inject HTTP like ops feed. |

**Drop from wave 1 concept bingo:** producer idempotence + transactions/EOS + SCRAM + Schema Registry. Teach **acks** and **idempotent consumers** first.

## Learning curriculum (concepts → this repo)

| Concept | Where it lives here | How we test it |
|---|---|---|
| Transactional outbox | `event_publication` + `@ApplicationModuleListener` | Already: listener tests, republish-on-restart config |
| Externalization | `spring-modulith-events-kafka` + `@Externalized` / `EventExternalizationConfiguration` | Broker-up IT: message after commit; no message if TX rolls back |
| Key / ordering | key=`targetId` for PAYMENT_ORDER | Two captures on different orders may interleave; two events on one id are ordered |
| At-least-once | Default Kafka + Modulith retry | Duplicate consume + unique constraint |
| Consumer group | Webhook workers | Two listeners, one message processed once |
| Retry / DLQ | Spring Kafka default error handler or `@RetryableTopic` **in eventlab only** | Poison inject → DLQ row in UI |
| Security | JWT on HTTP/WS; Kafka PLAINTEXT then optional SCRAM | No token in topic; tenant filter tests |
| Schema | JSON v1 field-additive | Contract test on envelope Zod + Java record |
| Time / SLA | Existing `@Scheduled` | Do not use Kafka as a timer |

## Suggested phasing (when the user approves)

Each phase = own spec under `.codex/specs/` / tickets. Do not start until P0 is done.

### P0 — Governance (no broker yet)

- Retire “No Kafka” in `AGENTS.md`, `CONTEXT.md`, `.codex/review-checklist.md`, `.agents/skills/spring-modulith/SKILL.md` + `modules.md`, `code-review`, `rest-api-test-design` — replace with **“Kafka only in `eventlab` / approved overlay”**.
- ADR `.codex/adr/0002-kafka-event-streaming.md` (accepted).
- Keep “no microservice split / no PSP / no PAN / no fake KPI dashboard”.
- Explicitly: Ops Wave 2 WS remains non-Kafka until a later spec.

### P1 — Infra

- `compose.kafka.yml` + docs in `docs/setup/`.
- `KafkaContainerSupport`.
- Health: broker up, topic auto-create or explicit `lab.auditable-actions.v1`.
- Pin image tag. PLAINTEXT.

### P2 — Bridge (the actual learning core)

- Dependency `spring-modulith-events-kafka` (Boot BOM / Modulith BOM).
- Stable `eventId`.
- Externalize `AuditableActionOccurred` → one topic, routing key from `targetId`.
- Event Lab consumer + processed table (Flyway V36+ `eventlab`).
- Tests: rollback does not publish; commit does; idempotent consume.
- Frontend Event Lab page + BFF proxy + Zod. Playwright one happy path.

### P3 — One real integration consumer

- Checkout-lab: notify HTTP → (optional) produce → inbox worker consumes **or** webhook dispatcher uses checkout HMAC against WireMock.
- DLQ UI.
- Still no product Merchant Webhooks tab.

### P4 — Advanced Event Lab (optional tickets)

- Topic split, retry tiers, compacted state topic, SQL-window risk rule (then optional Streams), SCRAM profile, two Spring instances for ops fan-in, export competing consumers.

## Project impact

- New module `eventlab`, Flyway folder, `application.yml` locations, `app.event-lab.enabled` default **false**.
- `dev` profile may enable Event Lab when `--kafka` is used; `test` profile stays off except dedicated `*Kafka*Test` classes.
- Compose default unchanged → POM Wave 2 / Merchant 360 stay runnable.
- Keycloak realm: additive role only if Event Lab HTTP is not covered by `platform:ops:inject`. Prefer reusing inject authority **or** one new `platform:event-lab:operate`.
- Do not add Kafka client to Nuxt. Browser never speaks Kafka; Event Lab is REST/BFF like everything else.
- `spring-kafka` comes with `spring-modulith-events-kafka`; do not add a second Kafka abstraction.
- Do not bump Spring Modulith to 2.1.

## Test impact (REST Assured / Playwright REST / Playwright E2E)

| Layer | What to add | What not to do |
|---|---|---|
| Unit | Routing key / envelope mapper | Mock the whole broker for domain tests |
| Module | `@ApplicationModuleTest` without Kafka by default | Require broker in `PaymentModuleTest` |
| REST Assured | `EventLabKafkaIT` / RestAssured against Event Lab + Testcontainers Kafka + Postgres | Broad `./mvnw test` pulling Kafka for every class |
| Playwright REST | BFF Event Lab list after lifecycle POST | Assert Kafka protocol from Node |
| Playwright E2E | Live Event Lab UI; `waitForResponse` on authorize; optional ops feed still `page.on('websocket')` | `page.route`, `routeWebSocket`, MSW |
| Security | Cross-tenant Event Lab 404/empty; inject 403 for merchant.manager | Put SASL users in realm JSON |
| Exclusions | Still skip `restkit/` and `paymentsupport/` unless asked | |

Awaitility is appropriate for consume assertions. Prefer unique `eventId` over sleep.

## Governance conflict (unchanged blocker)

Until P0, every agent session will flag Kafka as scope creep. This document does **not** authorize implementation.

## Explicit non-goals (carried + tightened)

- No real PSP, PAN/PCI/3DS, card data on topics.
- No microservice split; consumers in the same modular monolith (or Event Lab flag), not new deployables.
- No production OIDC completion; Keycloak stays lab IdP.
- No fake KPI / Grafana-on-Overview.
- No replacing REST commands with Kafka commands.
- No replacing Ops Wave 2 WebSocket in the Kafka first slice.
- No Schema Registry, Kafka Streams, or SCRAM in P1–P2.
- No Namastack/JobRunr (Modulith 2.1).
- No learning-seed flood of `event_publication` via domain services (ADR 0001).

## Sources

Iteration 2 research (Firecrawl MCP, 2026-08-21): FinServ Kafka patterns (meshiq, Conduktor, mimacom, Confluent fraud post), webhook retry/DLQ guides (Hookdeck, KodeKloud), Debezium-vs-outbox engineering posts (Trade Republic, singhajit, HN logical-replication outbox), idempotent-consumer pattern posts (Conduktor, TrinityLogic), PostgreSQL 18 release notes + feature deep-dives (postgresql.org, xata, CrunchyData, Neon), Podman/KRaft single-node compose examples. Full list with links: [kafka-payment-business-cases-v2.md §5](kafka-payment-business-cases-v2.md).

- [Spring Modulith 2.0 — Application Events / Externalization](https://docs.spring.io/spring-modulith/reference/2.0/events.html) — `@ApplicationModuleListener`, registry, `spring-modulith-events-kafka`, `@Externalized("topic::key")`, `EventExternalizationConfiguration`; native externalization is a transactional listener (pragmatic outbox), not Modulith 2.1 Namastack.
- [Spring Modulith Kafka example](https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-examples/spring-modulith-example-kafka/readme.adoc) — add `spring-modulith-events-kafka` + annotate types with `@Externalized`.
- [Testcontainers Kafka module](https://java.testcontainers.org/modules/kafka/) — `org.testcontainers:testcontainers-kafka:2.0.5`, `org.testcontainers.kafka.KafkaContainer`, `apache/kafka` / `apache/kafka-native`.
- [Apache Kafka Docker compose (KRaft combined, PLAINTEXT)](https://github.com/apache/kafka/blob/trunk/docker/examples/docker-compose-files/cluster/combined/plaintext/docker-compose.yml) — no ZooKeeper; advertised listeners for host vs network.
- This repo: `apps/backend/pom.xml` (Boot 4.0.6, Modulith 2.0.6, TC 2.0.5), `infra/compose/compose.yml`, `shared/events/AuditableActionOccurred.java`, `db/migration/shared/V6__create_event_publication.sql`, `audit/.../AuditEventListener.java`, `ops/.../OpsFeedBroker.java`, `ops/V34__create_ops_notifications.sql`, checkoutlab inbox/notifier, `PaymentStatus.java`, `Authorities.java`, `AGENTS.md` non-goals, ADR 0001, Ops Wave 2 E6, `docs/architecture/payment-gateway-roadmap-analysis.md` §10.

## Uncertainty / follow-up

- Exact `apache/kafka:4.x` tag vs Testcontainers image support — pin at P1 with a Hub pull.
- Spring Kafka version as managed by Boot 4.0.6 — confirm Jackson JSON serializer against the existing `EventSerializer` (lazy `ObjectMapper`) so JPA slices do not break the way Wave 2 audit did.
- Whether to put `@Externalized` on the record in `shared` (OPEN) or only select it programmatically in `eventlab` so default apps never publish.
- User must explicitly approve P0 (retire non-goal) before any compose/Java/frontend work.

When P0 is approved, next artifact is ADR `0002` + a thin `.codex` spec for P1–P2 only — not the full 15-flow catalog.
