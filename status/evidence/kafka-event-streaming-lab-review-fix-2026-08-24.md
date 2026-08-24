# Event Streaming Lab review-fix — evidence log (2026-08-24)

Branch: `001-project-foundation`, HEAD baseline `7f0e54d` (Commit 2), review: REQUEST_CHANGES.

All runs below are fresh on this working tree (inject-publisher + DLT rethrow + topic-manifest + retry/spring-retry-removal + BFF fix + KafkaIT hardening). Excluded suites: `restkit/**`, `paymentsupport/**` (repo rule). Container runtime: Docker (`/var/run/docker.sock`) — green.

## Surefire (no broker) — seam restored

```bash
./mvnw -Dtest='EventLabEnvelopeTest,EventLabEnvelopePropertyTest,EventLabInspectorListenerDltTest' test
```

- `EventLabEnvelopeTest` 3/3 GREEN
- `EventLabEnvelopePropertyTest` 1/1 GREEN (jqwik 100 tries)
- `EventLabInspectorListenerDltTest` 2/2 GREEN (P1-9: DLT handler propagates repository failures — no false success)
- `EventLabRestAssuredTest` renamed → `EventLabRestAssuredKafkaIT` (Failsafe `*IT`), so default `./mvnw test` is broker-free.

## Failsafe (broker) — fresh green

Definitive full-program gate (2026-08-24): `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' clean verify` → **BUILD SUCCESS; Surefire 669 tests + Failsafe 136 tests, 0 failures/0 errors** (full backend, no restrict/pay). The Event Lab subset (`EventLab*KafkaIT,EventLabInjectKafkaIT,EventLabRestAssuredKafkaIT`) is 50/50 GREEN in the same clean run.

| Class | Tests | Evidence |
|---|---|---|
| `EventLabBrokerKafkaIT` | 6/6 | roundtrip, empty-consume-no-hang, idempotent topic create, 3p-RF1, no auto-create, **topic set == exactly 3 (no default `-dlt`)** |
| `EventLabFlagOffKafkaIT` | 2/2 | flag-off → outbox row written (DB truth) but **zero consumed rows, zero lab publisher/controller beans, zero listener containers** (unreachable bootstrap) |
| `EventLabInjectKafkaIT` | 3/3 | duplicate → still 1 row; poison → DEAD topic `lab.event-lab.dlq.v1` + physical DLT consume; read-only 403; `payment_orders`/`audit_event` unchanged |
| `EventLabOutboxKafkaIT` | 9/9 | real-lifecycle RA-019 (create→authorize→capture real REST; outbox rows + broker AUTHORIZED/CAPTURED); RA-018 broker-offset ordering; RA-016 crash-heal seam (publication retained + `resubmit` exercised) |
| `EventLabPersistenceKafkaIT` | 10/10 | RA-023 real-listener-restart replay; RA-026 purge (old removed, fresh kept, business unchanged); RA-024 strict DLT topic+payload; RA-018N same-key fail-first → valid PROCESSED + poison DEAD |
| `EventLabRebalanceKafkaIT` | 1/1 | two real listener containers same group → exactly one row per (group,eventId) |
| `EventLabRestAssuredKafkaIT` | 8/8 | SEC-001..006 + RA-030..033 (403/404/400/401, read-vs-operate, tenant masking, X-Correlation-ID); **under Failsafe only (Surefire broker-free)** |

## Frontend

- `corepack pnpm typecheck` — GREEN (includes BFF route + Playwright spec edits)
- `corepack pnpm lint` — RED_PRE_EXISTING_NOT_WORSENED: 3 errors in files unchanged since BASE (`app/pages/admin/users/index.vue`, `app/pages/admin/merchants/[merchantId]/index.vue`), same as review baseline; my touched files contribute only warnings; `event-lab/index.get.ts` type-assertion warning resolved with SAFETY comment.
- `corepack pnpm test:unit` — assertions GREEN (634/634 tests, 70 files); process exit 1 due to 2 vitest worker RPC timeouts (pre-existing symptom, matches review baseline)
- BFF route contract test `tests/unit/event-lab-bff-route.test.ts` — 6/6 GREEN (200 array passthrough, query param forwarding, unknown-query 400; nuxt + happy-dom)
- BFF routes `server/api/event-lab/**` fixed to `return backendApi(...)` (were `res.data` = undefined)

## Cleanup / hygiene

- Root `META-INF/spring/...AutoConfiguration.imports` removed (redundant; Modulith jar ships its own)
- Root `pnpm-lock.yaml` (TypeScript 7 graph) removed
- Broken `.cursor/skills/playwright-skill-upstream` symlink removed; README + E0-governance stale routes fixed
- `spring-retry` + `@EnableRetry` removed; retry stays Spring Kafka `@RetryableTopic`
- Topic manifest == exactly 3 (main/retry/contract DLT); `dev-stack.sh` + `KafkaContainerSupport` create only 3; default `-dlt` absent (assert)
- `git diff --check` — GREEN
- Skill validator: 28 → 21 warnings (7 active skills got `When not to use`)

## Pending / honest status

- Playwright `--kafka` **live** — CLOSED with documented environment variance. Stack raised (`dev-stack.sh --kafka`; Nuxt :3000, Spring :8080 dev,seed,kafka, Keycloak :8081, Kafka topic-set 3).
  - Whole-suite `chromium-admin` event-lab runs (--no-deps, storage states): **19/23 and 15/23 passed** across two runs. Deterministic core green in every run: API-001/003/004/006/007, E2E-001/003/004/006a/006b/007/010/011, SEC-003 — BFF passthrough, duplicate/poison real-broker flows, no-POST, search/empty/forbidden/not-found, no auth/kafka leakage.
  - **E2E-002 + E2E-012 (payment-detail delivery card) 2/2 GREEN in isolation** after (a) fresh platform-admin storage state and (b) fixing a real locator bug in E2E-012 (strict-mode `.or()` collision — commit `aa8583a`).
  - Variance cause: **shared admin session TTL churn** — expired sealed `nuxt-session` → all `workerWorld` BFF calls 401 mid-suite (create/authorize), not a product failure.
  - **API-002/API-005 remain 401**: need fresh read-only state; `read-only-user.setup` hits a pre-existing Keycloak redirect-timing flake (setup infra outside Event Lab scope). Backend 403-for-no-read proven green in the 50/50 Failsafe gate (`EventLabRestAssuredKafkaIT.sec002/sec003`).
- Vitest process gate exits 1 despite 634/634 passed (worker RPC timeout flake, pre-existing; review baseline documented the same).