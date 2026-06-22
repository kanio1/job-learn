# Quickstart: Payment Lifecycle Operations Console

This quickstart is for implementing and verifying Feature 010 planning scope. It is application-only and must not create REST Assured framework work, new test classes, multi-capture/multi-refund behavior, PSP failure scenarios, Kafka/webhooks, full dashboard behavior, complete OAuth/OIDC integration, or new payment creation capability.

## Implementation Focus

1. Extend Nuxt payment schemas so detail, metadata, lifecycle history, backend errors, and internal version marker are represented safely.
2. Extend the Pinia payment store with separate detail, history, action, metadata, and feedback state.
3. Update the payment detail page/components to render lifecycle summary, oldest-first history, state-aware actions, confirmation UX, stale-state feedback, role-aware affordances, and separate metadata editing.
4. Update Nuxt server proxy behavior so lifecycle and metadata requests forward or generate the required headers and preserve backend error shape.
5. Touch backend payment response code only if existing Feature 009 responses omit fields required by `data-model.md` and `contracts/operations-console-contract.md`.

## Manual Verification Scenarios

- Open detail for `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, and `REFUNDED` orders and confirm the status badge, lifecycle facts, and action set match the matrix.
- Open an order with no lifecycle history and confirm an empty history state.
- Open an order with authorize then capture history and confirm entries render oldest-first.
- Submit a valid lifecycle action and confirm success feedback plus detail/history reload.
- Submit an action with stale version and confirm stale-state feedback plus refresh with no automatic retry.
- Trigger or simulate forbidden, not-found, validation, invalid-transition, idempotency-conflict, and backend-unavailable responses and confirm distinct user-facing states.
- Edit metadata from the separate metadata flow and confirm lifecycle status is not presented as changed by that flow.
- Inspect Nuxt proxy behavior to confirm `Authorization`, `If-Match`, `Idempotency-Key`, and `X-Correlation-ID` are preserved or generated according to the contract.

## Verification Commands

Run from repository root unless noted.

```bash
corepack pnpm --dir apps/frontend typecheck
```

```bash
corepack pnpm --dir apps/frontend build
```

If backend response DTOs or proxy-adjacent backend contracts are touched:

```bash
./mvnw -pl apps/backend test
```

If only documentation/planning artifacts are changed, code verification commands are not required for `/speckit.plan`; review generated artifacts instead.

## Feature 010 Risk Focus (for tester learning)

- Stale state (`412`) handling and version marker flow
- Header forwarding (`If-Match`, `Idempotency-Key`, `X-Correlation-ID`) at Nuxt proxy boundary
- State-to-action matrix correctness (6 statuses × 4 actions)
- Metadata editing kept visually and behaviorally separate from lifecycle actions
- Error flattening risk (`401/403/404/409/412/422`) – must remain distinguishable to the user
- Role-affordance risk: hide/disable controls conservatively when role context is incomplete; backend remains final authority
- History loading failure must not destroy detail summary
- No secret rendering (tokens, idempotency hashes, credentials)

## Planning Boundary

`/speckit.plan` stops before task generation. Use `/speckit.tasks specs/010-payment-lifecycle-operations-console/plan.md` later to create implementation tasks with the lab labels `AGENT-IMPLEMENT`, `AGENT-EXPLAIN`, `TESTER-ANALYZE`, `TESTER-DESIGN`, `TESTER-AUTOMATE`, `AGENT-REVIEW`, and `DISCUSS`.
