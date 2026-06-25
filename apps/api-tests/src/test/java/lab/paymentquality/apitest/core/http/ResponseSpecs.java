package lab.paymentquality.apitest.core.http;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.*;

/**
 * Reusable response contract specifications.
 *
 * <p>Composable via {@code ResponseSpecBuilder.addResponseSpecification()} so contracts nest.
 * The private {@link #noCache()} base (X-Correlation-ID + Cache-Control: no-store) is shared
 * by all specs; each spec adds the Vary header that the specific response type sends.
 *
 * <p>SDET learning: repeating Vary/Cache-Control/X-Correlation-ID assertions across 40 tests
 * is an anti-pattern. A {@code ResponseSpecification} encodes the contract once. If the backend
 * changes a response header, one spec update fixes all tests — not 40 individual changes.
 *
 * <p>Vary header mapping by response type:
 * <ul>
 *   <li>GET/HEAD/LIST success → {@code Vary: Authorization} → use {@link #sensitive()}</li>
 *   <li>Lifecycle action 200 (authorize/capture/cancel/refund) → {@code Vary: If-Match} → use {@link #conditional()}</li>
 *   <li>Error responses (4xx) → {@code Vary: Authorization} → use {@link #problemJson()} or {@code ProblemAssert}</li>
 *   <li>201 Create response → {@code Vary: Idempotency-Key} → use {@link #created()}</li>
 * </ul>
 *
 * <p>Usage in scenarios:
 * <pre>{@code
 *   response.then()
 *       .statusCode(200)
 *       .spec(ResponseSpecs.sensitive());
 * }</pre>
 */
public final class ResponseSpecs {

    /**
     * Shared base: {@code X-Correlation-ID} present and {@code Cache-Control: no-store}.
     * All payment response types carry these two headers regardless of Vary value.
     */
    private static ResponseSpecification noCache() {
        return new ResponseSpecBuilder()
                .expectHeader(Headers.CORRELATION_ID, notNullValue(String.class))
                .expectHeader(Headers.CACHE_CONTROL, containsString("no-store"))
                .build();
    }

    /**
     * Baseline contract for sensitive read responses (GET, HEAD, LIST, summary).
     * Asserts: {@code X-Correlation-ID} present, {@code Cache-Control: no-store},
     * {@code Vary} contains {@code Authorization}.
     */
    public static ResponseSpecification sensitive() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(noCache())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Authorization"))
                .build();
    }

    /**
     * Error response contract: content-type is {@code application/problem+json} plus sensitive baseline.
     * Also asserts minimum problem body fields: {@code error}, {@code status}, {@code correlationId}.
     *
     * <p>Error responses carry {@code Vary: Authorization} — same as read responses.
     *
     * <p>Usage: {@code response.then().spec(ResponseSpecs.problemJson()).statusCode(400)}
     */
    public static ResponseSpecification problemJson() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectContentType(ContentTypes.PROBLEM_JSON)
                .expectBody("error", notNullValue())
                .expectBody("status", notNullValue())
                .expectBody("correlationId", notNullValue())
                .build();
    }

    /**
     * Lifecycle mutation response contract: {@code Vary} contains {@code If-Match}.
     * Used for authorize/capture/cancel/refund 200 responses.
     *
     * <p>Note: lifecycle success responses carry {@code Vary: If-Match} (not Authorization).
     * This spec does NOT extend {@link #sensitive()} to avoid a false Authorization Vary check.
     */
    public static ResponseSpecification conditional() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(noCache())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("If-Match"))
                .build();
    }

    /**
     * Create response contract: {@code Vary} contains {@code Idempotency-Key}, ETag present in
     * quoted {@code "vN"} format, {@code Location} header present.
     *
     * <p>Note: 201 create responses carry {@code Vary: Idempotency-Key} (not Authorization).
     * This spec does NOT extend {@link #sensitive()} to avoid a false Authorization Vary check.
     */
    public static ResponseSpecification created() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(noCache())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Idempotency-Key"))
                .expectHeader(Headers.ETAG, matchesPattern("\"v\\d+\""))
                .expectHeader(Headers.LOCATION, notNullValue(String.class))
                .build();
    }

    private ResponseSpecs() {}
}
