package lab.paymentquality.apitest.core.problem;

import io.restassured.response.Response;
import org.hamcrest.MatcherAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Static helpers for JSON Schema contract assertions on REST Assured {@link Response} objects.
 *
 * <p>Complements {@link ProblemAssert} (which asserts individual fields of error responses)
 * by validating the <em>structural contract</em> of the whole response body against a
 * JSON Schema file. Together they cover different failure modes:
 *
 * <ul>
 *   <li>{@link ProblemAssert} catches: wrong status code, wrong error code, missing correlationId.
 *   <li>{@code SchemaAssertions} catches: backend renames a JSON field ({@code error} → {@code code}),
 *       changes a type (String → Integer), adds an undeclared field (schema with
 *       {@code additionalProperties: false}), or drops a required field.</li>
 * </ul>
 *
 * <p>Schema files live under {@code src/test/resources/schema/} and are located at runtime
 * via the classpath via {@code matchesJsonSchemaInClasspath(path)}.
 *
 * <p><strong>Why not schema-validate every test?</strong>
 * Schema validation adds per-test overhead and noise when a schema changes legitimately.
 * The correct strategy is:
 * <ol>
 *   <li>One dedicated schema test per response type (in {@code JsonSchemaContractSpec})
 *       that exercises the stable happy-path response.</li>
 *   <li>{@link ProblemAssert#matchesProblemSchema()} in specific error-path tests where you
 *       want to confirm the error response is well-formed, not just that a field matches.</li>
 *   <li>No schema validation in lifecycle/idempotency/concurrency tests — those test business
 *       rules, not JSON structure; the schema tests already cover structure.</li>
 * </ol>
 *
 * <p><strong>SDET interview: schema vs field checks</strong>
 * <ul>
 *   <li>Field checks ({@code response.jsonPath().getString("error")}) only fail if the
 *       specific field you checked changes. A backend that silently drops {@code createdAt}
 *       passes all field checks that don't mention {@code createdAt}.</li>
 *   <li>Schema validation with {@code "required": [...]} catches any missing required field
 *       regardless of which test asks about it. One schema test protects the whole contract.</li>
 *   <li>Schema validation with {@code "additionalProperties": false} catches new fields —
 *       useful when the backend team adds a field in a non-backward-compatible way
 *       (wrong name, wrong type).</li>
 * </ul>
 *
 * <p><strong>Hamcrest vs AssertJ:</strong> REST Assured's {@code matchesJsonSchemaInClasspath}
 * returns a {@link org.hamcrest.Matcher}{@code <String>}. We call it via
 * {@link MatcherAssert#assertThat(Object, org.hamcrest.Matcher)} which throws
 * {@link AssertionError} on failure — compatible with JUnit 5.
 * The failure message includes the full schema validation report (all violations, not just the first).
 *
 * <p>Phase 8H: JSON Schema / response contract validation foundation.
 */
public final class SchemaAssertions {

    private static final String SCHEMA_DIR = "schema/";
    private static final String PROBLEM_SCHEMA = SCHEMA_DIR + "problem.schema.json";
    private static final String PAYMENT_ORDER_SCHEMA = SCHEMA_DIR + "payment-order.schema.json";
    private static final String PAYMENT_SUMMARY_SCHEMA = SCHEMA_DIR + "payment-summary.schema.json";

    private SchemaAssertions() {}

    /**
     * Asserts that the response body conforms to {@code schema/problem.schema.json}.
     *
     * <p>Use in tests that exercise error paths ({@code application/problem+json} responses).
     * Typically combined with {@link ProblemAssert} for full error contract coverage:
     * <pre>{@code
     * ProblemAssert.assertThat(response)
     *     .hasStatus(409)
     *     .hasError(ProblemCodes.CONFLICT)
     *     .matchesProblemSchema();  // delegates here
     * }</pre>
     *
     * <p>The problem schema validates: {@code status} is an integer in 400–599 range,
     * {@code error} and {@code correlationId} are strings, {@code details} is an array/object/null.
     * It allows additional properties to tolerate the two handler shapes
     * (GlobalExceptionHandler vs PaymentExceptionHandler).
     *
     * @param response the REST Assured response; must contain a JSON body
     * @throws AssertionError if the body does not conform to the schema
     */
    public static void matchesProblemSchema(Response response) {
        MatcherAssert.assertThat(
                "Response body should match " + PROBLEM_SCHEMA,
                response.asString(),
                matchesJsonSchemaInClasspath(PROBLEM_SCHEMA));
    }

    /**
     * Asserts that the response body conforms to {@code schema/payment-order.schema.json}.
     *
     * <p>Use in tests that exercise the GET payment order endpoint (200 responses).
     * The schema enforces:
     * <ul>
     *   <li>Required stable fields: {@code paymentOrderId}, {@code merchantId},
     *       {@code clientOrderReference}, {@code amountMinor}, {@code currency},
     *       {@code status}, {@code createdAt}, {@code updatedAt}.</li>
     *   <li>Optional nullable lifecycle fields: {@code capturedAmountMinor},
     *       {@code authorizedAt}, {@code capturedAt}, etc. — nullable, not absent.</li>
     *   <li>{@code additionalProperties: false} — fails if the backend adds or renames
     *       a field without updating this schema.</li>
     * </ul>
     *
     * @param response the REST Assured response; must be a 200 payment order body
     * @throws AssertionError if the body does not conform to the schema
     */
    public static void matchesPaymentOrderSchema(Response response) {
        MatcherAssert.assertThat(
                "Response body should match " + PAYMENT_ORDER_SCHEMA,
                response.asString(),
                matchesJsonSchemaInClasspath(PAYMENT_ORDER_SCHEMA));
    }

    /**
     * Asserts that the response body conforms to {@code schema/payment-summary.schema.json}.
     *
     * <p>Use in tests that exercise the GET payment summary endpoint (200 responses).
     * The schema enforces:
     * <ul>
     *   <li>Required fields: {@code totalOrders}, {@code totalAmountMinor},
     *       {@code byCurrency} (array), {@code byStatus} (array).</li>
     *   <li>Each {@code byCurrency} item requires {@code currency}, {@code orderCount},
     *       {@code totalAmountMinor} with correct types and non-negative constraints.</li>
     *   <li>{@code additionalProperties: false} at root and item level — catches field
     *       renames in the aggregate projection (e.g. {@code orderCount} → {@code count}).</li>
     * </ul>
     *
     * @param response the REST Assured response; must be a 200 summary body
     * @throws AssertionError if the body does not conform to the schema
     */
    public static void matchesPaymentSummarySchema(Response response) {
        MatcherAssert.assertThat(
                "Response body should match " + PAYMENT_SUMMARY_SCHEMA,
                response.asString(),
                matchesJsonSchemaInClasspath(PAYMENT_SUMMARY_SCHEMA));
    }
}
