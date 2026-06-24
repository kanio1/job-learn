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
    private static final String AUTHORIZE_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize";
    private static final String CAPTURE_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture";
    private static final String CANCEL_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel";
    private static final String REFUND_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund";
    private static final String HISTORY_PATH =
            "/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history";

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

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize}
     * — transition a CREATED payment order to AUTHORIZED.
     *
     * <p>Requires: {@code merchant:payments:lifecycle} (or {@code platform:payments:lifecycle})
     * authority AND a JWT {@code merchant_id} claim equal to the {@code merchantId} path UUID
     * (for merchant-scoped callers). Both {@code If-Match} and {@code Idempotency-Key} are
     * functionally required.
     *
     * <p>Required headers: {@code If-Match: "v{N}"} (current ETag), {@code Idempotency-Key},
     * {@code Content-Type: application/json}.
     *
     * <p>Happy-path expectation: 200 with {@code PaymentOrderResponse} body, status AUTHORIZED,
     * {@code ETag: "v{N+1}"} (incremented from request), {@code Vary: If-Match},
     * {@code Cache-Control: no-store}.
     *
     * <p>Error cases:
     * <ul>
     *   <li>Missing {@code If-Match} → 428 {@code precondition_required} (see
     *       {@link #authorizeWithoutIfMatch}).</li>
     *   <li>Stale {@code If-Match} → 412 {@code payment_order_version_mismatch}.</li>
     *   <li>Wrong merchant scope → 403 {@code forbidden}.</li>
     *   <li>Order not in CREATED state → 422 {@code invalid_transition}.</li>
     * </ul>
     *
     * <p>Phase 7D: first lifecycle endpoint covered. Uses {@code RequestSpecs.lifecycle()} which
     * adds both {@code If-Match} and {@code Idempotency-Key} to the base authenticated spec.
     * Body is {@code {}} — {@code AuthorizeRequest.reason} is optional and not needed for the
     * contract foundation tests.
     */
    public static Response authorize(
            String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .post(AUTHORIZE_PATH);
    }

    /**
     * {@code POST .../authorize} without {@code If-Match} header — negative-test variant.
     *
     * <p>Intentionally omits {@code If-Match} to trigger the backend's precondition guard.
     * Expected: 428 with {@code error: "precondition_required"}.
     *
     * <p>{@code If-Match} is declared {@code required = false} at the Spring MVC layer but is
     * functionally required: {@code PaymentEtag.requireVersion(null)} throws
     * {@code PaymentPreconditionRequiredException} → mapped to 428 by
     * {@code PaymentExceptionHandler}. This is distinct from a missing-header 400: the
     * HTTP spec reserves 428 specifically for missing precondition headers.
     */
    public static Response authorizeWithoutIfMatch(
            String merchantId, String paymentOrderId, String idempotencyKey) {
        return RequestSpecs.idempotent(idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .post(AUTHORIZE_PATH);
    }

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture}
     * — transition an AUTHORIZED payment order to CAPTURED.
     *
     * <p>Requires: {@code merchant:payments:lifecycle} authority AND a JWT {@code merchant_id}
     * claim equal to the {@code merchantId} path UUID. Both {@code If-Match} and
     * {@code Idempotency-Key} are functionally required.
     *
     * <p>Required headers: {@code If-Match: "v{N}"} (current ETag from the AUTHORIZED order,
     * typically {@code "v1"} after authorize), {@code Idempotency-Key},
     * {@code Content-Type: application/json}.
     *
     * <p>Happy-path expectation: 200 with {@code PaymentLifecycleResponse} body,
     * status CAPTURED, {@code capturedAmountMinor} set to the order amount (full capture when no
     * body amount supplied), {@code capturedAt} non-null, {@code ETag: "v2"},
     * {@code Vary: If-Match}, {@code Cache-Control: no-store}.
     *
     * <p>Error cases:
     * <ul>
     *   <li>Order not in AUTHORIZED state → 422 {@code invalid_transition}.</li>
     *   <li>Stale {@code If-Match} → 412 {@code payment_order_version_mismatch}.</li>
     *   <li>Missing {@code If-Match} → 428 {@code precondition_required}.</li>
     * </ul>
     *
     * <p>Body is {@code {}} — {@code CaptureRequest.amountMinor} is optional; omitting it
     * triggers a full capture (captured amount equals the original authorization amount).
     *
     * <p>Phase 7E: second lifecycle endpoint covered. Uses {@code RequestSpecs.lifecycle()}
     * which adds both {@code If-Match} and {@code Idempotency-Key}.
     */
    public static Response capture(
            String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .post(CAPTURE_PATH);
    }

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel}
     * — transition a CREATED or AUTHORIZED payment order to CANCELLED.
     *
     * <p>Requires: {@code merchant:payments:lifecycle} authority AND a JWT {@code merchant_id}
     * claim equal to the {@code merchantId} path UUID. Both {@code If-Match} and
     * {@code Idempotency-Key} are functionally required.
     *
     * <p>Required headers: {@code If-Match: "v{N}"} (current ETag), {@code Idempotency-Key},
     * {@code Content-Type: application/json}.
     *
     * <p>Happy-path expectation: 200 with {@code PaymentLifecycleResponse} body, status CANCELLED,
     * {@code cancelledAt} non-null, {@code ETag: "v{N+1}"}, {@code Vary: If-Match},
     * {@code Cache-Control: no-store}.
     *
     * <p>Valid source states: CREATED (ETag {@code "v0"} → result {@code "v1"}) and AUTHORIZED
     * (ETag {@code "v1"} → result {@code "v2"}). The backend calls
     * {@code pspClient.voidAuthorization()} only when cancelling an AUTHORIZED order.
     *
     * <p>Error cases:
     * <ul>
     *   <li>Order in CAPTURED or REFUNDED state → 422 {@code invalid_transition}.</li>
     *   <li>Stale {@code If-Match} → 412 {@code payment_order_version_mismatch}.</li>
     *   <li>Missing {@code If-Match} → 428 {@code precondition_required}.</li>
     * </ul>
     *
     * <p>Body is {@code {}} — {@code CancelRequest.reason} is optional.
     *
     * <p>Phase 7E: third lifecycle endpoint covered.
     */
    public static Response cancel(
            String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .post(CANCEL_PATH);
    }

    /**
     * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund}
     * — transition a CAPTURED payment order to REFUNDED.
     *
     * <p>Requires: {@code merchant:payments:lifecycle} authority AND a JWT {@code merchant_id}
     * claim equal to the {@code merchantId} path UUID. Both {@code If-Match} and
     * {@code Idempotency-Key} are functionally required.
     *
     * <p>Required headers: {@code If-Match: "v{N}"} (current ETag from the CAPTURED order,
     * typically {@code "v2"} after a full authorize→capture chain), {@code Idempotency-Key},
     * {@code Content-Type: application/json}.
     *
     * <p>Happy-path expectation: 200 with {@code PaymentLifecycleResponse} body,
     * status REFUNDED, {@code refundedAmountMinor} set to the captured amount (full refund when
     * no body amount supplied), {@code refundedAt} non-null, {@code ETag: "v3"},
     * {@code Vary: If-Match}, {@code Cache-Control: no-store}.
     *
     * <p>Body is {@code {}} — {@code RefundRequest.amountMinor} and {@code RefundRequest.reason}
     * are both optional. Omitting {@code amountMinor} triggers a full refund: the backend uses
     * {@code order.getCapturedAmountMinor()} as the effective refund amount, so
     * {@code refundedAmountMinor} in the response equals {@code capturedAmountMinor}.
     *
     * <p>Partial refund (body with {@code amountMinor} less than {@code capturedAmountMinor})
     * is also supported by the backend but deferred to a future phase.
     *
     * <p>Error cases:
     * <ul>
     *   <li>Order not in CAPTURED state → 422 {@code invalid_transition}
     *       (e.g. refund from AUTHORIZED or CREATED).</li>
     *   <li>Stale {@code If-Match} → 412 {@code payment_order_version_mismatch}.</li>
     *   <li>Missing {@code If-Match} → 428 {@code precondition_required}.</li>
     * </ul>
     *
     * <p>Phase 7F: fourth and final lifecycle endpoint covered. Completes the full
     * CREATED → AUTHORIZED → CAPTURED → REFUNDED happy path.
     */
    public static Response refund(
            String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .post(REFUND_PATH);
    }

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history}
     * — retrieve the status-transition history for a payment order.
     *
     * <p>Requires one of: {@code merchant:payments:read}, {@code merchant:payments:lifecycle},
     * {@code platform:payments:read}, {@code platform:payments:lifecycle},
     * {@code platform:payments:audit}. Additional controller-level check: unless the caller
     * has a platform-scoped authority, the JWT {@code merchant_id} claim must equal the
     * {@code merchantId} path parameter.
     *
     * <p>Happy-path expectation: 200 with body
     * {@code {"content": [...StatusHistoryEntry]}}.
     *
     * <p><strong>Important contract note — creation entry excluded:</strong> the backend
     * queries with {@code WHERE action IS NOT NULL}, so the initial CREATED entry is never
     * returned. A freshly created order returns {@code {"content":[]}}. Only AUTHORIZE,
     * CAPTURE, CANCEL, and REFUND action entries appear.
     *
     * <p>Entries are ordered by {@code createdAt ASC} (chronological, oldest first).
     * Each entry carries {@code paymentOrderId}, {@code fromStatus}, {@code toStatus},
     * {@code action}, {@code actorSubject}, {@code correlationId}, and {@code createdAt}.
     *
     * <p>Response headers: {@code Vary: Authorization}, {@code Cache-Control: no-store}.
     *
     * <p>Phase 7J: history/audit contract coverage.
     */
    public static Response history(String merchantId, String paymentOrderId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .get(HISTORY_PATH);
    }
}
