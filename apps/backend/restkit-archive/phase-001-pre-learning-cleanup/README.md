# RestKit Phase 001 Pre-Learning Cleanup Archive

## Purpose

This archive preserves RestKit files that are useful for later lessons but distract from the current minimal learning path.

The active learning workspace should stay focused on:

- smoke test
- payment order create contract
- basic idempotency create/replay/conflict
- first validation checks

## Cleanup Context

- Date: 2026-06-09
- Branch: `018-rest-security-p1-error-auth-method-hardening`
- Scope: test-source RestKit cleanup only

No production code, frontend code, database migrations, or application behavior was changed for this archive.

## Archived Files

| Original path | Archive path | Planned lesson area |
| --- | --- | --- |
| `src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderListApi.java` | `restkit-archive/phase-001-pre-learning-cleanup/src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderListApi.java` | list/summary |
| `src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderSummaryApi.java` | `restkit-archive/phase-001-pre-learning-cleanup/src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderSummaryApi.java` | list/summary |
| `src/test/java/lab/paymentquality/testsupport/restkit/idempotency/IdempotencyKeysCopy.java` | `restkit-archive/phase-001-pre-learning-cleanup/src/test/java/lab/paymentquality/testsupport/restkit/idempotency/IdempotencyKeysCopy.java` | cleanup duplicate |

## Future Lesson Areas

These archived or not-yet-created helpers belong to later focused lessons:

- list/summary
- lifecycle
- metadata PATCH
- CORS
- security matrix
- specs/assertions
- config/logging
- schema validation

## Restore Instruction

Use `git mv` from the archive path back to the original path when the corresponding lesson begins.

Example:

```bash
git mv apps/backend/restkit-archive/phase-001-pre-learning-cleanup/src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderListApi.java apps/backend/src/test/java/lab/paymentquality/testsupport/restkit/client/PaymentOrderListApi.java
```
