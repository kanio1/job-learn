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

    private ProblemCodes() {}
}
