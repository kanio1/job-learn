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

- Playwright `--kafka` **live** — exercised end-to-end; residual flake is POM-infra/SSR, not product.
  - Stack raised (`dev-stack.sh --kafka`; Nuxt :3000, Spring :8080 dev,seed,kafka, Keycloak :8081, Kafka topic-set 3).
  - **Product path proven live end-to-end** (2026-08-24, in-browser probes):
    - OIDC login (platform.admin) → `GET /api/event-lab` via BFF → **200 with real records**;
    - backend `GET /api/event-lab` with real JWT → **200**;
    - login → `storageState()` save → **fresh context restore → BFF 200** (save/restore works).
  - Whole-suite `chromium-admin` event-lab runs (explicit storage states): deterministic pass counts **19/23 and 15/23** (SSR-stable runs); a 12/23 API-12 pass run where only UI E2E hit Nuxt-dev SSR blank-render. API specs (API-001/003/004/006/007, SEC-003) green in all healthy runs.
  - All 8 setup projects **pass serially** (11/11 with workers) — includes read-only (`PLAYWRIGHT_READ_ONLY_PASSWORD=readonly.user`, realm user `readonly.user`); earlier failures were the hyphen variant (wrong user).
  - **E2E-002 + E2E-012 (payment-detail delivery card) 2/2 GREEN in isolation** (fresh admin + locator fix commit `aa8583a`).
  - Residual flake (environment/POM-infra, not Event Lab code):
    - **Nuxt-dev SSR empty-render under load** (`goto /admin/event-lab` → 200 + correct URL + empty body) — host `nuxt dev` instability, causes intermittent UI E2E `toBeVisible` timeouts;
    - **parallel fresh-login contests** (all setups at once) → `frame detached` on goto; serial setups avoid it (commit `81e4f2a` raises redirect wait 30s→120s);
    - **sealed `nuxt-session` TTL churn** → transient BFF 401s for workerWorld calls mid-suite.
  - Backend 403-for-no-read / 401 / RBAC are proven green at the backend Failsafe gate (`EventLabRestAssuredKafkaIT.sec002/sec003`), independent of POM auth state.
- Vitest process gate exits 1 despite 634/634 passed (worker RPC timeout flake, pre-existing; review baseline documented the same).