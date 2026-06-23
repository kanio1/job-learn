package lab.paymentquality.apitest.core.http;

/**
 * HTTP header name constants used across the payment API contract.
 *
 * <p>String constants only — no logic, no state. Import statically in scenarios where
 * inline string literals would obscure intent (e.g. {@code .header(Headers.IDEMPOTENCY_KEY, key)}).
 *
 * <p>SDET learning: knowing which headers belong to which HTTP concern (conditional requests,
 * idempotency, caching, observability) is foundational for REST contract testing.
 */
public final class Headers {

    /** Idempotency-Key: prevents duplicate payment creation on retried POSTs. */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /** If-Match: optimistic locking — lifecycle actions require the current ETag. */
    public static final String IF_MATCH = "If-Match";

    /** If-None-Match: conditional GET — 304 if resource not changed. */
    public static final String IF_NONE_MATCH = "If-None-Match";

    /** ETag: version identifier on payment order resources. Pattern: {@code "vN"} (quoted). */
    public static final String ETAG = "ETag";

    /** X-Correlation-ID: distributed tracing — injected by CorrelationFilter, echoed by backend. */
    public static final String CORRELATION_ID = "X-Correlation-ID";

    /** Vary: response header declaring which request headers affect caching. */
    public static final String VARY = "Vary";

    /** Cache-Control: {@code no-store} on sensitive resources (payments, audit, users). */
    public static final String CACHE_CONTROL = "Cache-Control";

    /** Accept-Patch: advertises supported patch content-type on 415 responses. */
    public static final String ACCEPT_PATCH = "Accept-Patch";

    /** Allow: lists supported HTTP methods on 405 responses. */
    public static final String ALLOW = "Allow";

    /** Content-Type: standard MIME type header. */
    public static final String CONTENT_TYPE = "Content-Type";

    /** Location: URI of created resource on 201 responses. */
    public static final String LOCATION = "Location";

    /** Retry-After: seconds to wait before retrying after rate limiting (429). */
    public static final String RETRY_AFTER = "Retry-After";

    private Headers() {}
}
