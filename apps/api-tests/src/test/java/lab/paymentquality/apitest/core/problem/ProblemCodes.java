package lab.paymentquality.apitest.core.problem;

/**
 * Known {@code error} field values from the payment API error contract.
 *
 * <p>These codes appear in {@code application/problem+json} response bodies under the
 * {@code error} field. They are stable API contract values — not internal exception class names.
 *
 * <p>Confirmed from backend exception handlers ({@code GlobalExceptionHandler},
 * {@code PaymentExceptionHandler}, {@code MerchantExceptionHandler}):
 * the {@code error} field uses snake_case strings.
 *
 * <p>SDET learning: asserting on the {@code error} code (not the {@code title} or {@code detail}
 * message) is the correct approach — codes are contract-stable, human-readable messages change.
 */
public final class ProblemCodes {

    /** 400 — request body or query parameter failed validation. */
    public static final String VALIDATION = "validation";

    /** 401 — no credentials or invalid/expired token. */
    public static final String UNAUTHORIZED = "unauthorized";

    /** 403 — authenticated but not permitted (e.g. cross-tenant write attempt). */
    public static final String FORBIDDEN = "forbidden";

    /**
     * 404 — resource not found, or cross-tenant read boundary violation.
     * The backend returns 404 (not 403) on cross-tenant reads to avoid leaking existence.
     */
    public static final String NOT_FOUND = "not_found";

    /** 409 — business conflict (e.g. merchant not eligible, state machine violation). */
    public static final String CONFLICT = "conflict";

    /**
     * 409 — idempotency conflict: same key, different request body.
     * Distinct from 200 idempotency replay (same key, same body).
     */
    public static final String IDEMPOTENCY_CONFLICT = "idempotency_conflict";

    /**
     * 412 — precondition failed: {@code If-Match} ETag is stale.
     * The resource has been modified since the last read; re-read and retry.
     */
    public static final String PRECONDITION_FAILED = "precondition_failed";

    /** 415 — {@code Content-Type} not supported (e.g. missing {@code application/merge-patch+json}). */
    public static final String UNSUPPORTED_MEDIA_TYPE = "unsupported_media_type";

    /** 406 — server cannot produce a response matching the client's {@code Accept} header. */
    public static final String NOT_ACCEPTABLE = "not_acceptable";

    /** 405 — HTTP method not allowed on this resource. */
    public static final String METHOD_NOT_ALLOWED = "method_not_allowed";

    /** 429 — rate limit exceeded; {@code Retry-After} header indicates back-off seconds. */
    public static final String RATE_LIMITED = "rate_limited";

    /** 400 — unknown top-level field in merge-patch body. */
    public static final String UNKNOWN_TOP_LEVEL_FIELD = "unknown_top_level_field";

    /** 400 — merchant is not eligible for payment operations (wrong status). */
    public static final String MERCHANT_NOT_ELIGIBLE = "merchant_not_payment_eligible";

    /** 409 — duplicate merchant reference; a merchant with this reference already exists. */
    public static final String DUPLICATE_MERCHANT_REFERENCE = "duplicate_merchant_reference";

    /** 409 — invalid lifecycle state transition (e.g. ACTIVE → ACTIVE, or SUSPENDED → ACTIVE). */
    public static final String INVALID_TRANSITION = "invalid_transition";

    /**
     * 428 — {@code If-Match} header is required for this operation but was not sent.
     * The backend validates it as a functional precondition, not a missing-header error.
     * Client must re-fetch the resource, read its current ETag, and resend with If-Match.
     */
    public static final String PRECONDITION_REQUIRED = "precondition_required";

    /**
     * 412 — {@code If-Match} version does not match the current JPA {@code @Version} counter.
     * The resource was modified after the client last read it; the optimistic lock failed.
     * Client must re-fetch the current ETag and retry the lifecycle operation.
     */
    public static final String PAYMENT_ORDER_VERSION_MISMATCH = "payment_order_version_mismatch";

    /**
     * 412 — JPA optimistic lock failure caught at flush time.
     *
     * <p>Distinct from {@link #PAYMENT_ORDER_VERSION_MISMATCH}: that code is returned when the
     * service-level ETag pre-check fails (client sent a stale {@code If-Match} before any DB
     * operation ran). This code is returned when two concurrent requests <em>both</em> passed
     * the ETag pre-check (both read the same version), but only one could commit the
     * {@code UPDATE WHERE version = N} — the other triggers
     * {@code ObjectOptimisticLockingFailureException} at JPA flush time.
     *
     * <p>Client handling is the same: re-read the current ETag and retry.
     *
     * <p>Mapped by {@code PaymentExceptionHandler.handleOptimisticLock()} →
     * {@code HTTP 412 Precondition Failed} with {@code Vary: If-Match} and
     * {@code Cache-Control: no-store}.
     */
    public static final String CONCURRENCY_CONFLICT = "concurrency_conflict";

    /**
     * 422 — refund amount is invalid: either exceeds the captured amount or is zero/negative.
     *
     * <p>Thrown by {@code InvalidRefundAmountException} from {@code PaymentOrder.refund()} when
     * {@code effectiveAmount <= 0 || effectiveAmount > capturedAmountMinor}.
     * The error code is the same for both over-refund and zero/negative — the name {@code _exceeds_}
     * is technically imprecise for the zero/negative case but matches the backend's exception class name.
     *
     * <p>Mapped by {@code PaymentExceptionHandler.handleInvalidRefundAmount()} →
     * {@code HTTP 422 Unprocessable Entity} with {@code Vary: Authorization, If-Match} and
     * {@code Cache-Control: no-store}.
     *
     * <p>Guard fires inside {@code order.refund()}, AFTER the state-machine pre-check and PSP call.
     */
    public static final String REFUND_AMOUNT_EXCEEDS_CAPTURED = "refund_amount_exceeds_captured";

    /**
     * 409 — a concurrent create request reserved this idempotency key but has not yet written the
     * {@code paymentOrderId} (i.e., {@code complete()} has not been called).
     *
     * <p>This replaces the previously unhandled {@code IllegalStateException} (500) that would
     * have been thrown if a second concurrent create found the idempotency record with
     * {@code paymentOrderId == null}. The idempotency record is written in two phases:
     * {@code reserveIfAbsent()} inserts it without a {@code paymentOrderId}, then
     * {@code complete()} updates it with the real ID. Under PostgreSQL READ COMMITTED, both
     * phases commit atomically, so this code path is very rarely triggered in production.
     *
     * <p><strong>Client recovery:</strong> retry the same create with the same
     * {@code Idempotency-Key} and body. The retry will either get a 201 (if the first
     * request failed after reserving) or a 200 idempotency replay (if it succeeded).
     *
     * <p>Mapped by {@code PaymentExceptionHandler.handleIdempotencyCreateInProgress()} →
     * {@code HTTP 409 Conflict} with {@code Vary: Authorization, Idempotency-Key} and
     * {@code Cache-Control: no-store}.
     */
    public static final String IDEMPOTENCY_CREATE_IN_PROGRESS = "create_in_progress";

    /** 400 — evidence multipart part {@code file} is empty. */
    public static final String EMPTY_EVIDENCE_FILE = "empty_evidence_file";

    /** 400 — evidence filename is blank or missing. */
    public static final String MISSING_EVIDENCE_FILENAME = "missing_evidence_filename";

    /** 400 — evidence filename contains path segments or other unsafe characters. */
    public static final String INVALID_EVIDENCE_FILENAME = "invalid_evidence_filename";

    /** 415 — evidence {@code Content-Type} is missing or not in the allow-list. */
    public static final String UNSUPPORTED_EVIDENCE_CONTENT_TYPE = "unsupported_evidence_content_type";

    /** 413 — evidence payload exceeds 2 MiB. */
    public static final String EVIDENCE_FILE_TOO_LARGE = "evidence_file_too_large";

    private ProblemCodes() {}
}
