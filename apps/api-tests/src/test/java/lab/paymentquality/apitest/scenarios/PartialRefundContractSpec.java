package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.ETag;
import lab.paymentquality.apitest.core.data.IdempotencyKeys;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.http.ResponseSpecs;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8F — Partial refund and amount-validation contract for
 * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund}.
 *
 * <p>Covers three high-value scenarios that extend the full-refund happy path in
 * {@link PaymentOrdersContractSpec}:
 * <ol>
 *   <li>Partial refund (amountMinor &lt; capturedAmountMinor) → 200, status REFUNDED,
 *       {@code refundedAmountMinor} = requested amount, ETag v3</li>
 *   <li>Over-refund (amountMinor &gt; capturedAmountMinor) → 422
 *       {@code refund_amount_exceeds_captured}</li>
 *   <li>Zero-amount refund (amountMinor = 0) → 422 {@code refund_amount_exceeds_captured}</li>
 * </ol>
 *
 * <p><strong>Backend partial refund contract (confirmed from source):</strong>
 * <pre>
 * // PaymentOrder.refund(Long refundAmountMinor, String reason):
 * long effectiveAmount = refundAmountMinor != null ? refundAmountMinor : capturedAmountMinor;
 * if (effectiveAmount &lt;= 0 || effectiveAmount &gt; capturedAmountMinor) {
 *     throw new InvalidRefundAmountException(effectiveAmount, capturedAmountMinor);
 * }
 * this.status = PaymentStatus.REFUNDED;
 * this.refundedAmountMinor = effectiveAmount;
 * </pre>
 *
 * <p>The same {@code InvalidRefundAmountException} covers both over-refund and zero/negative
 * — mapped to 422 {@code refund_amount_exceeds_captured} by
 * {@code PaymentExceptionHandler.handleInvalidRefundAmount()}.
 *
 * <p><strong>Guard order in service (relevant to test design):</strong>
 * <ol>
 *   <li>Idempotency replay check — early return if same key+fingerprint</li>
 *   <li>ETag version check ({@code requireCurrentVersion}) — 412 if stale</li>
 *   <li>Idempotency reservation (DB insert, rolled back on exception)</li>
 *   <li>State-machine pre-guard ({@code canTransitionTo(REFUNDED)}) — 422 if wrong state</li>
 *   <li>PSP call — refund settled externally</li>
 *   <li>{@code order.refund()} — 422 if amount invalid (rolls back step 3)</li>
 * </ol>
 *
 * <p><strong>Multiple refunds:</strong> not supported. The order transitions to REFUNDED on the
 * first successful call (partial or full). A second refund attempt returns 422 {@code invalid_transition}
 * because REFUNDED is a terminal state with no valid outgoing transitions.
 *
 * <p><strong>SDET learning — transaction rollback on domain exception:</strong>
 * When {@code order.refund()} throws {@code InvalidRefundAmountException}, the surrounding
 * {@code @Transactional} boundary rolls back the entire unit of work — including the idempotency
 * record inserted at step 3. The DB state is unchanged. The caller can resend with a corrected
 * {@code amountMinor} without encountering an "idempotency conflict" on the next attempt
 * (because the old record was rolled back).
 *
 * <p><strong>SDET learning — error code precision:</strong> {@code refund_amount_exceeds_captured}
 * is the error code for both over-refund ({@code amountMinor > capturedAmountMinor}) and zero/negative
 * ({@code amountMinor &lt;= 0}). The same exception class covers both cases; the name is technically
 * imprecise for the zero-amount case. Testing both paths confirms the backend contract, not just
 * the most-likely client error.
 *
 * <p>All tests use {@link Identities#seededMerchantCreator()} — {@code merchant.alpha.creator}
 * whose JWT carries {@code merchant_id = MERCHANT_ALPHA_001_ID} and the
 * {@code merchant:payments:lifecycle} + {@code merchant:payments:create} authorities.
 *
 * <p>Negative tests (over-refund, zero) reuse the seeded
 * {@link Seeds#PAYMENT_ORDER_ALPHA_001_CAPTURED_ID} order (ETag {@code "v2"},
 * {@code capturedAmountMinor = 3300} USD). Because the domain exception causes a transaction
 * rollback, the seeded order remains in CAPTURED state and is not consumed by these tests.
 */
@Tag("contract")
@Tag("lifecycle")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PartialRefundContractSpec {

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
     * Partial refund returns 200 with the requested amount as {@code refundedAmountMinor} and ETag v3.
     *
     * <p>A partial refund sends {@code amountMinor} that is less than {@code capturedAmountMinor}.
     * The order transitions to REFUNDED with {@code refundedAmountMinor} = the requested partial
     * amount, not the full captured amount. The captured amount is unchanged in the response.
     *
     * <p>The ETag chain follows the same pattern as a full refund: each state transition increments
     * the JPA {@code @Version} counter by 1:
     * {@code "v0" (create) → "v1" (authorize) → "v2" (capture) → "v3" (refund)}.
     *
     * <p><strong>Business risk verified:</strong> a merchant that settled 1 000 PLN but wants to
     * return only 400 PLN (e.g., partial service delivery) can issue a partial refund without
     * refunding the entire captured amount. The {@code refundedAmountMinor} in the response is the
     * authoritative record of how much was returned to the customer.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why does a partial refund still move the order to REFUNDED (terminal state)?</li>
     *   <li>What prevents a second refund for the remaining amount after a partial refund?</li>
     *   <li>Why must {@code refundedAmountMinor} be asserted and not assumed from the amount field?</li>
     * </ul>
     */
    @Test
    @Order(1)
    void partial_refund_returns_200_with_partial_refunded_amount_and_etag_v3() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0"
        String clientRef = UniqueReferences.paymentRef("partial-refund-happy");
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(1_000L, "PLN", clientRef),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);
        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));
        assertThat(createEtag.raw()).isEqualTo("\"v0\"");

        // Step 2: authorize — ETag "v1"
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));
        authorizeResponse.then().statusCode(200);
        ETag authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG));
        assertThat(authorizeEtag.raw()).isEqualTo("\"v1\"");

        // Step 3: capture (full amount — capturedAmountMinor = 1 000) — ETag "v2"
        Response captureResponse = PaymentOrdersApi.capture(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                authorizeEtag.raw(),
                IdempotencyKeys.generate("capture"));
        captureResponse.then().statusCode(200);
        ETag captureEtag = ETag.of(captureResponse.header(Headers.ETAG));
        assertThat(captureEtag.raw()).isEqualTo("\"v2\"");

        // Step 4: partial refund — 400 out of 1 000 captured
        Response refundResponse = PaymentOrdersApi.refundWithAmount(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                captureEtag.raw(),
                IdempotencyKeys.generate("refund"),
                400L);

        assertThat(refundResponse.statusCode()).as("partial refund returns 200").isEqualTo(200);

        // ETag: v2 → v3 (state transition increments @Version)
        ETag refundEtag = ETag.of(refundResponse.header(Headers.ETAG));
        assertThat(refundEtag.raw()).as("ETag incremented to v3").isEqualTo("\"v3\"");

        // Body: status REFUNDED, refundedAmountMinor = 400 (partial), NOT 1 000 (full)
        PaymentOrderResponse refundBody = refundResponse.as(PaymentOrderResponse.class);
        assertThat(refundBody.status()).as("status is REFUNDED").isEqualTo("REFUNDED");
        assertThat(refundBody.refundedAmountMinor()).as("refundedAmountMinor = partial amount 400").isEqualTo(400L);
        assertThat(refundBody.refundedAt()).as("refundedAt is populated").isNotNull();

        // Vary: If-Match + Cache-Control: no-store + X-Correlation-ID — lifecycle mutation contract
        refundResponse.then().spec(ResponseSpecs.conditional());
    }

    /**
     * Refunding more than the captured amount returns 422 {@code refund_amount_exceeds_captured}.
     *
     * <p>The seeded {@link Seeds#PAYMENT_ORDER_ALPHA_001_CAPTURED_ID} order has
     * {@code capturedAmountMinor = 3 300} USD (ETag {@code "v2"}). Sending {@code amountMinor = 3 301}
     * (one unit over the captured amount) triggers {@code InvalidRefundAmountException} in
     * {@code PaymentOrder.refund()} — the domain-level guard fires BEFORE the entity is modified.
     *
     * <p>The {@code @Transactional} boundary rolls back the idempotency record inserted earlier in
     * the call. The seeded order remains in CAPTURED state — its ETag is still {@code "v2"}.
     *
     * <p><strong>Business risk verified:</strong> a merchant cannot accidentally refund more than
     * they captured. Over-refund is a financially dangerous operation (returns more money than
     * the customer paid) and is correctly blocked at the domain layer with a clear error code.
     *
     * <p><strong>SDET learning:</strong> the response carries {@code Vary: Authorization, If-Match}
     * because the error is returned from {@code preconditionHeaders()} in
     * {@code PaymentExceptionHandler}, grouping it semantically with other ETag-related errors.
     */
    @Test
    @Order(2)
    void over_refund_exceeding_captured_amount_returns_422() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // capturedAmountMinor for PAYMENT_ORDER_ALPHA_001_CAPTURED_ID = 3 300 USD (ETag "v2")
        Response response = PaymentOrdersApi.refundWithAmount(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CAPTURED_ID,
                "\"v2\"",
                IdempotencyKeys.generate("over-refund"),
                3_301L);

        ProblemAssert.assertThat(response)
                .hasStatus(422)
                .hasError(ProblemCodes.REFUND_AMOUNT_EXCEEDS_CAPTURED)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore()
                .varyContains("If-Match");
    }

    /**
     * Refunding with {@code amountMinor = 0} returns 422 {@code refund_amount_exceeds_captured}.
     *
     * <p>The domain guard in {@code PaymentOrder.refund()} checks {@code effectiveAmount <= 0}
     * before checking {@code effectiveAmount > capturedAmountMinor}. A zero amount is rejected
     * with the same {@code InvalidRefundAmountException} and the same 422 problem code as an
     * over-refund. The error code name is technically imprecise for the zero case (zero does not
     * "exceed" the captured amount), but it is what the backend produces — testers assert on the
     * actual contract, not the ideal naming.
     *
     * <p>The seeded {@link Seeds#PAYMENT_ORDER_ALPHA_001_CAPTURED_ID} order is reused. Because the
     * transaction rolls back on the domain exception, this test does not consume the seeded order
     * (it remains in CAPTURED state with ETag {@code "v2"}).
     *
     * <p><strong>Business risk verified:</strong> a zero-amount refund has no financial meaning
     * and is blocked. Without this guard, calling the PSP with a zero refund amount could produce
     * undefined PSP behaviour or silent success.
     *
     * <p><strong>SDET learning — same error code for two distinct invalid inputs:</strong>
     * both over-refund and zero-refund hit the same exception class ({@code InvalidRefundAmountException})
     * and the same error code ({@code refund_amount_exceeds_captured}). When the backend maps
     * multiple validation failures to the same code, negative tests should cover each distinct
     * path to confirm there are no gaps.
     */
    @Test
    @Order(3)
    void zero_refund_amount_returns_422() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = PaymentOrdersApi.refundWithAmount(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CAPTURED_ID,
                "\"v2\"",
                IdempotencyKeys.generate("zero-refund"),
                0L);

        ProblemAssert.assertThat(response)
                .hasStatus(422)
                .hasError(ProblemCodes.REFUND_AMOUNT_EXCEEDS_CAPTURED)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore()
                .varyContains("If-Match");
    }
}
