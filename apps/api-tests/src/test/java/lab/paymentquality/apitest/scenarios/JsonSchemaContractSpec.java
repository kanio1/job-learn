package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.core.problem.SchemaAssertions;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8H — JSON Schema / response contract validation foundation.
 *
 * <p>Validates that three stable response shapes conform to their JSON Schema definitions.
 * Each test covers one response type; together they protect the structural contract of the
 * three most significant response bodies in the payment API.
 *
 * <h2>Why schema tests complement field-level assertions</h2>
 *
 * <p>Every other spec in this framework uses field-level assertions such as:
 * <pre>{@code
 * assertThat(response.jsonPath().getString("status")).isEqualTo("CREATED");
 * ProblemAssert.assertThat(response).hasError(ProblemCodes.NOT_FOUND);
 * }</pre>
 *
 * <p>Field-level assertions fail only when the <em>specific field they mention</em> changes.
 * They do NOT catch:
 * <ul>
 *   <li>A required field that is silently dropped (e.g. backend drops {@code createdAt}).</li>
 *   <li>A field rename (e.g. {@code clientOrderReference} → {@code orderReference}) —
 *       assertions that don't reference the old name pass, clients that use the old name break.</li>
 *   <li>A type change (e.g. {@code amountMinor} changes from integer to string) in fields
 *       the test doesn't explicitly deserialize.</li>
 *   <li>An undeclared new field with a different name than expected
 *       (caught by {@code additionalProperties: false}).</li>
 * </ul>
 *
 * <p>Schema validation catches all of the above with a single assertion. The tradeoff:
 * schema tests are slightly more brittle to <em>intentional</em> contract expansions (adding a
 * new field requires updating the schema), but that friction is intentional — it ensures every
 * contract change is reviewed and documented.
 *
 * <h2>Schema-per-response-type, not schema-per-test</h2>
 *
 * <p>This spec validates schemas <em>once per response type</em> on a stable, seeded resource.
 * All other specs (lifecycle, idempotency, etc.) do not call {@code matchesSchema()} — they
 * trust that if the contract held on the stable resource, it holds everywhere the same handler runs.
 * The exception is {@link ProblemAssert#matchesProblemSchema()}, which can be selectively
 * enabled for error paths where you want explicit schema coverage (it delegates to
 * {@link SchemaAssertions#matchesProblemSchema}).
 *
 * <h2>Test identity</h2>
 *
 * <p>All three tests use {@link Identities#merchantReader(String)} which carries
 * {@code platform:payments:read}. This bypasses the JWT {@code merchant_id} claim check
 * so the tests are not sensitive to which merchant owns which resource.
 *
 * <h2>Schema files</h2>
 * <ul>
 *   <li>{@code src/test/resources/schema/problem.schema.json} — pre-existing (Phase 4 draft)</li>
 *   <li>{@code src/test/resources/schema/payment-order.schema.json} — created in Phase 8H</li>
 *   <li>{@code src/test/resources/schema/payment-summary.schema.json} — created in Phase 8H</li>
 * </ul>
 *
 * <p>Phase 8H. All tests are live (Failsafe): they require the full container stack.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonSchemaContractSpec {

    @BeforeAll
    static void seedDatabase() {
        SeedApi.seed();
    }

    @AfterAll
    static void resetDatabase() {
        SeedApi.reset();
    }

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    /**
     * GET payment order response body conforms to {@code payment-order.schema.json}.
     *
     * <p>Validates the structural contract of the single-resource response shape:
     * all required fields are present, all optional lifecycle fields allow null,
     * and no undeclared fields appear ({@code additionalProperties: false}).
     *
     * <p>The seeded {@link Seeds#PAYMENT_ORDER_ALPHA_001_CREATED_ID} order is used because:
     * <ul>
     *   <li>It is in CREATED status — all lifecycle timestamps ({@code authorizedAt},
     *       {@code capturedAt}, etc.) are null. This verifies the schema correctly
     *       permits null for optional lifecycle fields.</li>
     *   <li>It is stable (seeded deterministically) — the response body is predictable
     *       without creating additional state.</li>
     * </ul>
     *
     * <p><strong>What this schema catches that field checks miss:</strong>
     * If the backend team renames {@code clientOrderReference} to {@code orderReference},
     * existing field checks that assert on {@code status} or {@code amountMinor} still pass.
     * This schema test fails immediately because {@code clientOrderReference} is in
     * {@code required} and {@code orderReference} is not a declared property
     * (caught by {@code additionalProperties: false}).
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why is {@code additionalProperties: false} useful in test schemas?</li>
     *   <li>When would you set {@code additionalProperties: true} instead?</li>
     *   <li>What happens if the backend adds a new nullable field: does this schema break?</li>
     * </ul>
     */
    @Test
    @Order(1)
    void get_payment_order_response_conforms_to_payment_order_schema() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.getById(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        assertThat(response.statusCode()).as("GET payment order returns 200").isEqualTo(200);

        // Schema validation: validates required fields, types, nullable lifecycle fields,
        // and additionalProperties: false (any undeclared field causes failure)
        SchemaAssertions.matchesPaymentOrderSchema(response);
    }

    /**
     * GET 404 error response body conforms to {@code problem.schema.json}.
     *
     * <p>Validates the structural contract of the error response shape:
     * {@code status} is an integer in 400–599 range, {@code error} and {@code correlationId}
     * are strings, {@code details} is array/object/null (varies by handler).
     *
     * <p>Uses a non-existent UUID to trigger a 404. The problem schema
     * uses {@code additionalProperties: true} (permissive) because the two error handlers
     * ({@code GlobalExceptionHandler} and {@code PaymentExceptionHandler}) produce slightly
     * different field sets. The schema validates the intersection — the fields both handlers
     * always produce — without being brittle about handler-specific extras.
     *
     * <p><strong>What this schema catches that ProblemAssert field checks miss:</strong>
     * If the backend changes {@code status: 404} from an integer to a string
     * ({@code "404"}), all {@code ProblemAssert.hasStatus(404)} checks still pass
     * (they assert the HTTP status code, not the body field). This schema test fails
     * because the schema requires {@code "status": {"type": "integer"}}.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why does the problem schema use {@code additionalProperties: true} while
     *       the payment-order schema uses {@code false}?</li>
     *   <li>What is the difference between asserting the HTTP status code (via REST Assured)
     *       and asserting the {@code status} field in the JSON body?</li>
     * </ul>
     */
    @Test
    @Order(2)
    void get_404_error_response_conforms_to_problem_schema() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        // Non-existent payment order UUID — triggers 404 not_found
        Response response = PaymentOrdersApi.getById(
                Seeds.MERCHANT_ALPHA_001_ID,
                "00000000-0000-0000-0000-999999999999");

        // Verify HTTP level first — schema validation only makes sense for a problem body
        ProblemAssert.assertThat(response)
                .hasStatus(404)
                .hasError(ProblemCodes.NOT_FOUND)
                .hasCorrelationId();

        // Schema validation: validates status is integer 400-599, error/correlationId are strings
        SchemaAssertions.matchesProblemSchema(response);
    }

    /**
     * GET payment summary response body conforms to {@code payment-summary.schema.json}.
     *
     * <p>Validates the structural contract of the aggregate report: root-level counters
     * and both nested array item shapes ({@code byCurrency}, {@code byStatus}).
     * Uses {@code additionalProperties: false} at both root and item level to catch
     * field renames in the aggregate projection.
     *
     * <p>The seeded dataset contains orders in multiple currencies (PLN, EUR, USD) and
     * multiple statuses (CREATED, AUTHORIZED, CAPTURED). The summary endpoint returns
     * non-empty {@code byCurrency} and {@code byStatus} arrays — this validates the
     * nested item schemas, not just the top-level shape.
     *
     * <p><strong>What this schema catches that PaymentSummaryContractSpec field checks miss:</strong>
     * If the backend renames {@code orderCount} to {@code count} in the aggregate projection,
     * all existing assertions that check {@code totalOrders} or {@code totalAmountMinor}
     * at the root level still pass. This schema test fails because {@code orderCount}
     * is in {@code required} for each {@code byCurrency} item and {@code additionalProperties: false}
     * blocks {@code count} as an undeclared field.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why is schema validation more valuable for aggregate/projection endpoints
     *       than for single-resource endpoints?</li>
     *   <li>What happens to schema tests when a previously empty array becomes non-empty?
     *       (Answer: item schemas are only validated when items exist.)</li>
     * </ul>
     */
    @Test
    @Order(3)
    void get_payment_summary_response_conforms_to_payment_summary_schema() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.summary(Seeds.MERCHANT_ALPHA_001_ID);

        assertThat(response.statusCode()).as("GET summary returns 200").isEqualTo(200);

        // Schema validation: validates totalOrders/totalAmountMinor types,
        // byCurrency and byStatus array item shapes, and additionalProperties constraints
        SchemaAssertions.matchesPaymentSummarySchema(response);
    }
}
