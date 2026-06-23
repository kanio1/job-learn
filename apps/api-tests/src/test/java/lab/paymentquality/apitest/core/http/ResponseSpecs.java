package lab.paymentquality.apitest.core.http;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.*;

/**
 * Reusable response contract specifications.
 *
 * <p>Composable via {@code ResponseSpecBuilder.addResponseSpecification()} so contracts nest:
 * {@link #problemJson()} includes {@link #sensitive()}, {@link #conditional()} includes
 * {@link #sensitive()}, etc.
 *
 * <p>SDET learning: repeating Vary/Cache-Control/X-Correlation-ID assertions across 40 tests
 * is an anti-pattern. A {@code ResponseSpecification} encodes the contract once. If the backend
 * changes a response header, one spec update fixes all tests — not 40 individual changes.
 *
 * <p>Usage in scenarios:
 * <pre>{@code
 *   response.then()
 *       .spec(ResponseSpecs.created())
 *       .statusCode(201)
 *       .body("status", equalTo("CREATED"));
 * }</pre>
 */
public final class ResponseSpecs {

    /**
     * Baseline contract for sensitive resources (payments, audit, users).
     * Asserts: {@code X-Correlation-ID} present, {@code Cache-Control: no-store},
     * {@code Vary} contains {@code Authorization}.
     */
    public static ResponseSpecification sensitive() {
        return new ResponseSpecBuilder()
                .expectHeader(Headers.CORRELATION_ID, notNullValue(String.class))
                .expectHeader(Headers.CACHE_CONTROL, containsString("no-store"))
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Authorization"))
                .build();
    }

    /**
     * Error response contract: content-type is {@code application/problem+json} plus sensitive baseline.
     * Also asserts minimum problem body fields: {@code error}, {@code status}, {@code correlationId}.
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
     * Conditional-request response contract: {@code Vary} must contain {@code If-Match}.
     * Used for lifecycle action responses (authorize, capture, cancel, refund, PATCH).
     */
    public static ResponseSpecification conditional() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("If-Match"))
                .build();
    }

    /**
     * Create response contract: {@code Vary} contains {@code Idempotency-Key}, ETag is present
     * in quoted {@code "vN"} format.
     * Used for {@code POST /api/merchants/{id}/payment-orders} (201 and 200 replay).
     */
    public static ResponseSpecification created() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Idempotency-Key"))
                .expectHeader(Headers.ETAG, matchesPattern("\"v\\d+\""))
                .build();
    }

    private ResponseSpecs() {}
}
