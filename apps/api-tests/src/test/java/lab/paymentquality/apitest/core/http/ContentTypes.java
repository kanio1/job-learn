package lab.paymentquality.apitest.core.http;

/**
 * MIME type constants for the payment API.
 *
 * <p>SDET learning: {@code application/problem+json} requires explicit parser registration in
 * REST Assured (see {@link RestAssuredSetup}) — without it, error-body assertions throw a
 * parse error instead of a clean assertion failure.
 * {@code application/merge-patch+json} requires the charset-append fix in {@code EncoderConfig}.
 */
public final class ContentTypes {

    /** Standard JSON API responses and request bodies. */
    public static final String JSON = "application/json";

    /**
     * RFC 7807 / RFC 9457 problem+json — error responses.
     * Must be registered as a JSON parser in REST Assured before any error-body assertion.
     */
    public static final String PROBLEM_JSON = "application/problem+json";

    /**
     * RFC 7396 merge-patch — used for PATCH /payment-orders/{id} (metadata update).
     * Must be sent without a {@code charset} suffix: REST Assured appends one by default,
     * which causes 415 Unsupported Media Type. Fixed by {@code EncoderConfig} in
     * {@link RestAssuredSetup#install(String)}.
     */
    public static final String MERGE_PATCH_JSON = "application/merge-patch+json";

    private ContentTypes() {}
}
