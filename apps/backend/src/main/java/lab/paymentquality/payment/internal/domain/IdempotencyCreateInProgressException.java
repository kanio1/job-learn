package lab.paymentquality.payment.internal.domain;

/**
 * Thrown when a concurrent create request finds an idempotency record that has been reserved
 * (by a parallel request with the same key) but whose {@code paymentOrderId} has not yet been
 * written by {@code complete()}.
 *
 * <p><strong>When this can happen:</strong> {@code PaymentOrderService.create()} writes the
 * idempotency record in two phases:
 * <ol>
 *   <li>{@code reserveIfAbsent()} — inserts the record with {@code paymentOrderId = null}.</li>
 *   <li>{@code complete()} — updates the record with the real {@code paymentOrderId}.</li>
 * </ol>
 * Both phases are within the same {@code @Transactional} boundary. Under PostgreSQL READ COMMITTED
 * isolation, a concurrent request's INSERT or SELECT will normally block until the first
 * transaction commits. However, if the record is observed with {@code paymentOrderId = null}
 * (e.g., due to partial commit, autocommit misconfiguration, or future refactoring that separates
 * the two phases), this exception replaces the previous unhandled {@code IllegalStateException}.
 *
 * <p><strong>Client recovery:</strong> the caller should wait briefly and retry the same create
 * request with the same {@code Idempotency-Key} and same body. On retry, the response will be
 * either 201 (if the first request failed) or 200 (idempotency replay, if the first request
 * succeeded after this exception was thrown).
 *
 * <p><strong>HTTP mapping:</strong> 409 Conflict with error code {@code create_in_progress}.
 * Although 503 Service Unavailable with {@code Retry-After} would be more semantically precise,
 * 409 is consistent with the idempotency error family already in use in this codebase.
 */
public class IdempotencyCreateInProgressException extends RuntimeException {

    public IdempotencyCreateInProgressException() {
        super("A concurrent create for this idempotency key is still in progress; retry with the same key and body");
    }
}
