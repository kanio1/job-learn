package lab.paymentquality.apitest.core.http;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Factory for isolated, per-request copies of the immutable BASE spec template.
 *
 * <p>SDET learning — the critical distinction:
 * <ul>
 *   <li>{@code BASE} is built once by {@link RestAssuredSetup#install} via
 *       {@code RequestSpecBuilder.build()} → <strong>immutable template</strong>.</li>
 *   <li>{@code given().spec(BASE)} creates an <strong>isolated mutable copy</strong>.
 *       Adding headers to the copy does NOT affect the template — no cross-test leakage.</li>
 *   <li>This is the correct answer to: "how do you avoid header leakage between parallel tests
 *       when using RequestSpecification?"</li>
 * </ul>
 *
 * <p>Filter responsibilities:
 * <ul>
 *   <li>{@link AuthFilter} (on BASE only) — reads {@link lab.paymentquality.apitest.core.context.Ctx}
 *       and injects {@code Authorization: Bearer} for non-anonymous identities.</li>
 *   <li>{@link CorrelationFilter} (on BASE and ANONYMOUS_BASE) — injects {@code X-Correlation-ID}.</li>
 * </ul>
 *
 * <p>Scenarios MUST NOT call {@code given()} directly. Use the factory methods here.
 */
public final class RequestSpecs {

    /**
     * Immutable authenticated template — set once by {@link RestAssuredSetup#install}.
     * Has AuthFilter + CorrelationFilter. Use for all endpoints requiring authentication.
     */
    static RequestSpecification BASE;

    /**
     * Immutable anonymous template — no AuthFilter, only CorrelationFilter.
     * Use for {@code GET /api/status}, seed/reset endpoints.
     */
    static RequestSpecification ANONYMOUS_BASE;

    /**
     * Authenticated base: inject auth + correlation automatically.
     * Use for all endpoints requiring a Bearer token.
     */
    public static RequestSpecification base() {
        requireInstalled();
        return given().spec(BASE);
    }

    /**
     * Anonymous base: no Authorization header, correlation ID still injected.
     * Use for {@code /api/status}, {@code /api/test/seed}, {@code /api/test/reset}.
     */
    public static RequestSpecification anonymous() {
        requireInstalled();
        return given().spec(ANONYMOUS_BASE);
    }

    /**
     * Idempotent create: adds {@code Idempotency-Key} header.
     * Required for {@code POST /api/merchants/{id}/payment-orders} and lifecycle POSTs.
     *
     * <p>SDET learning: idempotency key enables safe retry — first call returns 201, replay returns 200
     * with identical body. {@code Versioned<T>} carries body + ETag from both calls for comparison.
     */
    public static RequestSpecification idempotent(String key) {
        return base().header(Headers.IDEMPOTENCY_KEY, key);
    }

    /**
     * Conditional mutate: adds {@code If-Match} header.
     * Required for {@code PATCH} and lifecycle actions (authorize/capture/cancel/refund).
     *
     * <p>SDET learning: If-Match implements optimistic locking — if the ETag has changed since the
     * last read, the backend returns 412 Precondition Failed (not 409 Conflict).
     */
    public static RequestSpecification conditional(String ifMatch) {
        return base().header(Headers.IF_MATCH, ifMatch);
    }

    /**
     * Lifecycle action: adds both {@code If-Match} and {@code Idempotency-Key}.
     * Required for all state-transition POSTs (authorize, capture, cancel, refund).
     *
     * <p>Usage: {@code RequestSpecs.lifecycle(versioned.etag().raw(), IdempotencyKeys.generate("authorize"))}
     */
    public static RequestSpecification lifecycle(String ifMatch, String idempotencyKey) {
        return base()
                .header(Headers.IF_MATCH, ifMatch)
                .header(Headers.IDEMPOTENCY_KEY, idempotencyKey);
    }

    /**
     * Merge-patch update: sets {@code application/merge-patch+json} content-type (without charset)
     * and adds {@code If-Match}.
     *
     * <p>SDET learning: the charset fix in {@link RestAssuredSetup} prevents REST Assured from
     * sending {@code application/merge-patch+json; charset=UTF-8} which the backend rejects as 415.
     */
    public static RequestSpecification mergePatch(String ifMatch) {
        return base()
                .contentType(ContentTypes.MERGE_PATCH_JSON)
                .header(Headers.IF_MATCH, ifMatch);
    }

    /**
     * Multipart upload: overrides the JSON content-type on {@link #base()}.
     *
     * <p>REST Assured 6 rejects {@code multiPart} when the spec still has
     * {@code Content-Type: application/json}.
     */
    public static RequestSpecification multipart() {
        return base().contentType(ContentTypes.MULTIPART_FORM_DATA);
    }

    /**
     * Raw multipart body (malformed-boundary probes). Encodes the body as text so REST
     * Assured does not try to serialize a String as JSON under a multipart content-type.
     */
    public static RequestSpecification rawMultipart(String contentTypeHeader) {
        return base()
                .config(RestAssured.config().encoderConfig(
                        EncoderConfig.encoderConfig()
                                .encodeContentTypeAs(
                                        ContentTypes.MULTIPART_FORM_DATA,
                                        ContentType.TEXT)))
                .contentType(contentTypeHeader);
    }

    private static void requireInstalled() {
        if (BASE == null) {
            throw new IllegalStateException(
                    "RestAssuredSetup.install() has not been called. " +
                    "Call it in @BeforeAll or a JUnit extension before using RequestSpecs.");
        }
    }

    private RequestSpecs() {}
}
