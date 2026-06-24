package lab.paymentquality.apitest.api.payment;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.core.http.ContentTypes;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client facade for {@code /api/merchants/{merchantId}/payment-orders} endpoints.
 *
 * <p>This is a one-method-per-endpoint facade over REST Assured. It hides path strings,
 * content-type headers, and required headers from scenario classes. Scenarios express only
 * business intent; they never call {@code given/when} directly.
 *
 * <p><strong>Authentication:</strong> injected automatically by
 * {@link lab.paymentquality.apitest.core.http.AuthFilter} reading
 * {@link lab.paymentquality.apitest.core.context.Ctx}. Callers must set the context before use.
 *
 * <p><strong>Return-type strategy (framework plan §12):</strong>
 * <ul>
 *   <li>{@link Response} — raw response; scenarios inspect status, headers, and body.
 *       Used for all methods because payment order tests must verify headers (ETag, Cache-Control,
 *       Vary) as part of the HTTP contract — returning a DTO would discard that information.</li>
 * </ul>
 *
 * <p><strong>Authorization model:</strong> two distinct authority paths exist for reads:
 * <ul>
 *   <li>{@code platform:payments:read} — bypasses {@code merchant_id} JWT claim check;
 *       granted to {@code platform.payment.reader} Keycloak user; used for cross-merchant reads.</li>
 *   <li>{@code merchant:payments:read} — requires JWT {@code merchant_id} claim to match
 *       the {@code merchantId} path parameter; granted to merchant-scoped users.</li>
 * </ul>
 *
 * <p>Phase 7A covers GET and LIST. Phase 7B unblocks Create ({@code POST}) by adding
 * {@code merchant.alpha.creator} to the test realm with {@code merchant_id} set to the UUID
 * of seeded MERCHANT_ALPHA_001. See {@code PHASE_7B_PAYMENT_ORDER_CREATE_AUTH_UNBLOCK.md}.
 *
 * <p>SDET learning: the thin-client pattern (Facade over REST Assured) is the correct layer
 * to separate "how to call the API" from "what the test asserts." Each API client owns one
 * resource; scenarios own the oracle logic.
 */
public final class PaymentOrdersApi {

    private static final String BY_ID_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}";
    private static final String LIST_PATH =
            "/api/merchants/{merchantId}/payment-orders";

    private PaymentOrdersApi() {}

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}} — retrieve one order.
     *
     * <p>Requires authority: {@code merchant:payments:read} OR {@code platform:payments:read}.
     * With {@code platform:payments:read}, the JWT {@code merchant_id} claim check is bypassed.
     * Happy-path expectation: 200 with payment order body + {@code ETag: "v{version}"} header.
     * Error cases: 400 for non-UUID path params, 404 for unknown UUID, 403 for scope mismatch.
     */
    public static Response getById(String merchantId, String paymentOrderId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .get(BY_ID_PATH);
    }

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders} — list payment orders (paginated).
     *
     * <p>Requires authority: {@code merchant:payments:read} OR {@code platform:payments:read}.
     * Happy-path expectation: 200 with paginated body:
     * {@code { content: [...], page, size, totalElements, totalPages }}.
     * Supports optional query parameters: {@code status}, {@code currency}, {@code fromDate},
     * {@code toDate}, {@code minAmount}, {@code maxAmount}, {@code clientOrderReference},
     * {@code page}, {@code size}, {@code sort}.
     */
    public static Response list(String merchantId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .when()
                .get(LIST_PATH);
    }

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders} — create a new payment order.
     *
     * <p>Requires: {@code merchant:payments:create} authority AND a JWT {@code merchant_id} claim
     * that equals the {@code merchantId} UUID path parameter. The controller checks this before
     * any service logic; mismatch throws {@code AccessDeniedException} → 403 {@code forbidden}.
     *
     * <p>Required headers: {@code Idempotency-Key} (mandatory — 400 if missing),
     * {@code Content-Type: application/json}.
     *
     * <p>Happy-path expectation: 201 with {@code Location} header, {@code ETag: "v0"},
     * {@code Vary: Authorization, Idempotency-Key}, and a payment order body.
     *
     * <p>Phase 7B: unblocked by adding {@code merchant.alpha.creator} Keycloak user whose
     * {@code merchant_id} attribute is the UUID of seeded MERCHANT_ALPHA_001.
     * Use {@link lab.paymentquality.apitest.core.auth.Identities#seededMerchantCreator()}
     * when calling this method.
     */
    public static Response create(
            String merchantId, CreatePaymentOrderRequest requestBody, String idempotencyKey) {
        return RequestSpecs.idempotent(idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .body(requestBody)
                .when()
                .post(LIST_PATH);
    }

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders} without {@code Idempotency-Key}.
     *
     * <p>Negative-test variant: intentionally omits the required {@code Idempotency-Key} header.
     * Expected: 400 with {@code error: "validation"} — the backend handler for missing
     * idempotency key uses the same validation error shape as bean validation failures.
     *
     * <p>SDET note: this variant exists because the normal {@link #create} method always adds the
     * header via {@code RequestSpecs.idempotent()}. Negative-path testing requires the ability to
     * omit required headers — handled here at the facade layer, not in the scenario.
     */
    public static Response createWithoutIdempotencyKey(
            String merchantId, CreatePaymentOrderRequest requestBody) {
        return RequestSpecs.base()
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .body(requestBody)
                .when()
                .post(LIST_PATH);
    }
}
