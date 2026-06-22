# BA Discovery Pack: Payment Lifecycle Operations Console

**Date:** 2026-06-05  
**Feature ID:** 010-payment-lifecycle-operations-console  
**Status:** Discovery complete - ready for Spec Kit specify  
**Scope type:** Application functionality only

## Lesson 14 / Feature 009 Reference Classes

These existing application classes belong to the Lesson 14 lifecycle foundation context that feature 010 will expose in the application console. They are reference classes for understanding the current app behavior, not new test deliverables for feature 010.

Backend Lesson 14 references:

- `PaymentOrderController` - lifecycle REST surface for authorize, capture, cancel, refund, metadata update and history.
- `PaymentLifecycleService` - application service coordinating lifecycle transitions.
- `PaymentOrder` - lifecycle state and domain fields.
- `PaymentStatus` - lifecycle status vocabulary: `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
- `PaymentOrderStatusHistory` - lifecycle status history/audit representation.
- `PspClient` and `MockPspClient` - PSP boundary and always-success Lesson 14 mock.
- `PaymentExceptionHandler` - lifecycle-facing HTTP error mapping.
- `SecurityConfig` - lifecycle role and CORS/security configuration.

Frontend Lesson 14 references:

- `payment-order.schema.ts` - lifecycle response and history schema vocabulary.
- `PaymentOrderDetail.vue` - payment detail component that feature 010 should turn into an operations-oriented detail view.
- `backendApi.ts` - Nuxt server proxy boundary for forwarding lifecycle headers.
- lifecycle proxy routes: `authorize.post.ts`, `capture.post.ts`, `cancel.post.ts`, `refund.post.ts`, `history.get.ts`.

## Co nowego technicznie dochodzi

Najważniejsze nowe obszary, które feature 010 wnosi jako aplikacyjne wymagania i które później będą dobrym materiałem do nauki REST/API testing:

1. Specific HTTP headers:
   - `ETag`,
   - `If-Match`,
   - `Idempotency-Key`,
   - `X-Correlation-ID`,
   - `Cache-Control`,
   - `Vary`,
   - `Authorization`,
   - CORS headers: `Access-Control-Allow-Origin`, `Access-Control-Allow-Headers`, `Access-Control-Expose-Headers`.

2. Specific HTTP response codes:
   - `200 OK`,
   - `400 Bad Request`,
   - `401 Unauthorized`,
   - `403 Forbidden`,
   - `404 Not Found`,
   - `409 Conflict`,
   - `412 Precondition Failed`,
   - `415 Unsupported Media Type`,
   - `422 Unprocessable Entity`,
   - optionally `406 Not Acceptable`,
   - `OPTIONS 200 OK` for CORS preflight.

3. Business-technical flows:
   - create payment order -> read `ETag` -> authorize with `If-Match`,
   - authorize -> capture -> read history,
   - stale `ETag` -> `412` -> refresh state,
   - retry same lifecycle action with the same `Idempotency-Key`,
   - forbidden action by wrong role,
   - platform operator vs merchant operator,
   - frontend -> Nuxt proxy -> backend -> database/history.

4. Modern application boundary behavior:
   - frontend and Nuxt proxy must preserve backend lifecycle protocol instead of hiding it,
   - backend remains authoritative for authorization and state transitions,
   - UI handles stale state without unsafe automatic retry,
   - history/audit makes database effects visible through the application.

## 1. Capability Proposal

### Working Name

**Payment Lifecycle Operations Console** - an application-facing operator experience for the lifecycle foundation implemented in feature 009.

### Why Now

Feature `009-payment-lifecycle-foundation` added backend lifecycle operations: authorize, capture, cancel, refund, metadata PATCH and lifecycle history. The application now has meaningful payment state, but the operator-facing experience is still incomplete.

The next useful application slice is not another backend business rule. It is exposing the existing lifecycle safely and clearly in the product UI/API boundary so a merchant or platform operator can understand the current payment state and perform allowed actions.

### Roadmap Fit

```text
Lessons 06-09: Payment order create/read/list/summary and frontend consumer
Lesson 10: HTTP contract hardening and authorization matrix
Lesson 14 / Feature 009: Payment lifecycle foundation
Feature 010: Payment lifecycle operations console <- next application slice
Future: multi-capture, multi-refund, PSP failures, webhooks, scheduled expiration
```

## 2. Business Goal

### Business Problem

A PayU-like operator can create and inspect payment orders, and the backend can execute lifecycle transitions, but the application does not yet provide a clear operational surface for:

- seeing lifecycle-specific timestamps and amounts,
- seeing the status history/audit trail,
- choosing valid lifecycle actions from the current state,
- submitting required operational headers through the Nuxt server proxy,
- updating non-status metadata without implying a lifecycle transition.

Without this, lifecycle behavior exists mostly as backend capability rather than usable application functionality.

### Desired Outcome

A merchant/platform operator can open a payment order detail screen and:

- understand the current lifecycle status,
- see the most important lifecycle facts,
- see a timeline/history of lifecycle changes,
- perform only currently valid lifecycle actions,
- provide reason/amount inputs where the backend supports them,
- receive clear success and error feedback,
- update metadata without changing lifecycle status.

### Consequence of Not Solving

- The system looks like a backend-only lab rather than a realistic payment operations application.
- Lifecycle actions remain hard to understand from a user workflow perspective.
- Frontend/server proxy may not forward required lifecycle headers such as `If-Match` and `Idempotency-Key`.
- Operators lack a safe place to inspect audit/history data.

## 3. Actors and Stakeholders

| Actor | Role | Goal |
|---|---|---|
| Merchant Payment Operator | `merchant:payments:lifecycle` | Manage lifecycle actions for own merchant payment orders |
| Merchant Reader | `merchant:payments:read` | Inspect payment details/history without mutating lifecycle |
| Platform Payment Operator | `platform:payments:lifecycle` | Manage lifecycle actions across merchants |
| Platform Auditor | `platform:payments:audit` | Inspect lifecycle history without mutation |
| Support/Risk Analyst | read/audit role depending on realm | Understand what happened to a payment order |

## 4. Business Workflow

### Trigger

An operator opens a payment order detail route from the payment orders list or direct link.

### Main Success Path

1. Application loads payment order detail.
2. Application loads payment order status history.
3. Application shows lifecycle summary: status, amount, captured/refunded amount, relevant timestamps and reasons.
4. Application determines visible/available actions from current status and role context.
5. Operator chooses an allowed action such as authorize, capture, cancel or refund.
6. Application sends the lifecycle request through the Nuxt server proxy with required headers.
7. Backend executes the existing lifecycle operation.
8. Application refreshes order detail and history.
9. Operator sees the updated status, new ETag-backed state and timeline entry.

### Alternate Paths

- Reader/auditor opens detail: history is visible, mutation controls are hidden or disabled.
- Operator cancels before authorization: detail changes from `CREATED` to `CANCELLED`.
- Operator captures after authorization: detail changes from `AUTHORIZED` to `CAPTURED`.
- Operator refunds after capture: detail changes from `CAPTURED` to `REFUNDED`.
- Operator updates metadata: metadata changes while lifecycle status remains unchanged.

### Failure Paths

- Backend rejects action as invalid transition: UI shows a domain error and refreshes current state.
- Backend rejects stale state with `412`: UI informs the operator that the payment changed and reloads detail/history.
- Backend rejects missing/invalid permission: UI shows access-denied feedback and does not imply action success.
- Backend returns validation error for amount/reason: UI keeps form input and displays the validation feedback.

## 5. Business Rules and Decisions

1. The console must not invent new lifecycle states or product behavior beyond feature 009.
2. Available actions are derived from current status:
   - `CREATED`: authorize, cancel.
   - `AUTHORIZED`: capture, cancel.
   - `CAPTURED`: refund.
   - `CANCELLED`, `EXPIRED`, `REFUNDED`: no mutation action.
3. Lifecycle actions require a current order ETag and an idempotency key generated by the application/proxy boundary.
4. The UI must refresh detail/history after successful lifecycle mutation.
5. Metadata update must not be presented as a lifecycle action and must not imply status change.
6. Read-only actors must be able to inspect allowed information without seeing misleading mutation affordances.
7. Audit/history display must use safe, non-secret fields only. Do not display raw tokens or sensitive internal credentials.

## 6. Domain Vocabulary

| Term | Meaning |
|---|---|
| Lifecycle action | An operation that changes payment status: authorize, capture, cancel, refund |
| Operations console | Application surface for inspecting and operating payment lifecycle |
| Status history | Immutable timeline of status changes for a payment order |
| ETag | Version marker returned by backend and used for conditional lifecycle mutations |
| Idempotency key | Request key used to make lifecycle action retry-safe |
| Metadata update | Non-status update to payment order metadata |

## 7. Data Needs

### Inputs

- merchant ID,
- payment order ID,
- current ETag,
- optional capture/refund amount,
- optional cancel/refund reason,
- metadata key/value pairs,
- operator authentication context.

### Outputs

- updated payment order detail,
- lifecycle status badge,
- lifecycle timestamps,
- captured/refunded amount,
- cancellation/refund reason,
- status history timeline,
- user-facing error/success messages.

## 8. Candidate Acceptance Criteria

1. Payment order detail page displays all lifecycle statuses from feature 009 with clear status badges.
2. Payment order detail page displays lifecycle timestamps and captured/refunded amount fields when present.
3. Payment order detail page loads and renders status history as a timeline or equivalent chronological list.
4. UI exposes authorize and cancel actions for `CREATED` orders to lifecycle-capable operators.
5. UI exposes capture and cancel actions for `AUTHORIZED` orders to lifecycle-capable operators.
6. UI exposes refund action for `CAPTURED` orders to lifecycle-capable operators.
7. UI hides or disables lifecycle mutation controls for terminal statuses: `CANCELLED`, `EXPIRED`, `REFUNDED`.
8. UI hides or disables lifecycle mutation controls for read-only/audit actors when role information is available to the application.
9. Nuxt server proxy forwards `If-Match`, `Idempotency-Key` and `X-Correlation-ID` for lifecycle mutation routes.
10. Application refreshes payment order detail and status history after successful lifecycle mutation.
11. Application shows clear feedback for invalid transition, validation error, forbidden access and stale ETag/precondition failure.
12. Metadata update is available as a separate application action and does not change lifecycle status.
13. No new backend product lifecycle behavior is introduced beyond feature 009.

## 9. Ambiguities and Open Questions

1. Does the frontend currently have access to role/permission information sufficient to hide controls accurately, or should the UI rely on backend rejection while avoiding obviously invalid state-based controls?
2. Should history display be oldest-first for readability or newest-first for operations triage?
3. Should idempotency keys be generated in browser code or Nuxt server proxy code?
4. Should capture/refund amount be optional in the UI for full amount by default, or should the operator always confirm the amount?
5. Should metadata editing be a compact inline panel or a separate modal/dialog?

## 10. Initial Tester Lens

Even though this feature is application-only and does not include test deliverables, the highest product risks are visible:

- stale UI state causing rejected lifecycle operations,
- misleading action availability,
- missing forwarding of required HTTP headers,
- confusing terminal statuses,
- audit/history ordering ambiguity,
- read-only users seeing mutation affordances,
- metadata update being confused with lifecycle transition.

These risks should be captured in the spec as observable behavior, not as a request to implement test classes.

## 11. Feature Sequencing Recommendation

Proceed next.

Reason:

- It converts feature 009 from backend capability into usable application behavior.
- It avoids premature advanced payment logic.
- It gives the learner more realistic app workflow without adding PSP failures, webhooks or dashboards.
- It can remain scoped to frontend/proxy/application presentation plus minimal backend adjustments if existing lifecycle responses are insufficient.

## 12. Spec Kit Input Summary

### Suggested Feature Title

Payment Lifecycle Operations Console

### Feature Intent

Expose the implemented payment lifecycle foundation through a focused operator-facing application surface that lets authorized users inspect lifecycle state/history and execute currently valid lifecycle actions without adding new payment business behavior.

### Recommended Scope

- Payment order detail lifecycle summary.
- Lifecycle status history display.
- State-aware lifecycle action controls.
- Nuxt server proxy forwarding for lifecycle headers.
- Metadata update application flow.
- User feedback for lifecycle success/error outcomes.

### Recommended Non-Goals

- No test suite implementation as feature scope.
- No multi-capture.
- No multi-refund.
- No PSP failures.
- No webhooks or Kafka.
- No scheduled expiration job.
- No complete dashboard or fake metrics.
- No new OAuth/OIDC integration.
- No product behavior beyond feature 009 lifecycle semantics.
