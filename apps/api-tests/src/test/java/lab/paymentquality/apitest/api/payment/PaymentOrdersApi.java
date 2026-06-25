package lab.paymentquality.apitest.api.payment;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PatchMetadataRequest;
import lab.paymentquality.apitest.core.http.ContentTypes;
import lab.paymentquality.apitest.core.http.Headers;
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
    private static final String SUMMARY_PATH =
            "/api/merchants/{merchantId}/payment-orders/summary";

    private PaymentOrdersApi() {}

    /**
     * {@code HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}
     * — check existence and retrieve metadata without a response body.
     *
     * <p>Requires authority: {@code merchant:payments:read} OR {@code platform:payments:read}.
     * Same ownership check as {@link #getById}: platform readers bypass the {@code merchant_id}
     * claim; merchant-scoped readers must match the path {@code merchantId}.
     *
     * <p>Happy-path expectation: 200 with {@code ETag: "v{version}"}, {@code Vary: Authorization},
     * {@code Cache-Control: no-store}, and an empty body. The ETag value matches what
     * {@code GET} would return — HEAD is defined to return the same headers as GET for the same
     * resource without the body.
     *
     * <p>SDET learning: HEAD is used by caches, pollers, and clients that need to check
     * {@code ETag} staleness before issuing a full GET. In REST Assured,
     * {@code .when().head()} sends a real HEAD request; the response body is always empty
     * ({@code response.body().asString()} returns {@code ""}).
     *
     * <p>Phase 8G: HTTP method semantics contract.
     */
    public static Response headById(String merchantId, String paymentOrderId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .head(BY_ID_PATH);
    }

    /**
     * {@code OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}
     * — discover supported methods and patch content-types for a resource.
     *
     * <p><strong>No authentication required.</strong> The security configuration permits
     * all OPTIONS requests on {@code /api/**} ({@code permitAll()}) so that CORS preflight
     * and capability-discovery can work unauthenticated.
     *
     * <p>Expected response: {@code 204 No Content} with:
     * <ul>
     *   <li>{@code Allow: GET, HEAD, PATCH, OPTIONS} — the four supported methods on this resource</li>
     *   <li>{@code Accept-Patch: application/merge-patch+json} — advertises the PATCH content-type</li>
     *   <li>{@code X-Correlation-ID} — backend injects a correlation ID even for OPTIONS</li>
     * </ul>
     *
     * <p>This is a <strong>custom OPTIONS handler</strong> in {@code PaymentOrderController},
     * not Spring MVC's auto-generated response. The handler returns an explicit list instead of
     * relying on Spring to enumerate handlers.
     *
     * <p>Uses {@link RequestSpecs#anonymous()} — no {@code Authorization} header is injected.
     * Callers do <em>not</em> need to call {@code Ctx.set()} before using this method.
     *
     * <p>Phase 8G: HTTP method semantics contract.
     */
    public static Response optionsById(String merchantId, String paymentOrderId) {
        return RequestSpecs.anonymous()
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .options(BY_ID_PATH);
    }

    /**
     * {@code DELETE /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}
     * — negative-test variant that sends an unsupported HTTP method.
     *
     * <p>DELETE is not a mapped method for this resource. Spring MVC throws
     * {@code HttpRequestMethodNotSupportedException} and
     * {@code PaymentExceptionHandler.handleMethodNotSupported()} maps it to
     * 405 {@code method_not_allowed} with an {@code Allow} header listing the methods
     * that <em>are</em> supported: {@code GET, HEAD, PATCH, OPTIONS}.
     *
     * <p>Authentication is required: the security filter's {@code .anyRequest().authenticated()}
     * rule applies to unmapped methods, so the request must carry a valid JWT to reach the
     * Spring MVC dispatcher (otherwise 401 is returned before the 405 can fire).
     *
     * <p>SDET learning: testing 405 responses verifies the {@code Allow} header contract.
     * HTTP/1.1 (RFC 9110 §15.5.6) requires a 405 response to include an {@code Allow} header.
     * The backend's {@code handleMethodNotSupported()} copies the supported methods from
     * {@code HttpRequestMethodNotSupportedException.getSupportedHttpMethods()}.
     *
     * <p>Phase 8G: HTTP method semantics contract.
     */
    public static Response deleteById(String merchantId, String paymentOrderId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .delete(BY_ID_PATH);
    }

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}} with a custom
     * {@code Accept} header — for content-negotiation contract tests.
     *
     * <p>When the client's {@code Accept} header does not include {@code application/json}
     * and all endpoint methods produce only {@code application/json}, Spring MVC throws
     * {@code HttpMediaTypeNotAcceptableException} →
     * {@code PaymentExceptionHandler.handleHttpMediaTypeNotAcceptable()} → 406 {@code not_acceptable}.
     *
     * <p>Example: {@code accept = "text/xml"} → 406 with {@code application/problem+json} body.
     * The 406 error body uses {@code Content-Type: application/problem+json} regardless of the
     * client's Accept header, because Spring MVC exception handlers bypass content-type
     * negotiation when writing the error response.
     *
     * <p>SDET learning: content-type negotiation in REST Assured — {@code .accept(mimeType)}
     * is the dedicated method for setting the {@code Accept} header, distinct from
     * {@code .contentType()} which sets the request body's {@code Content-Type}.
     *
     * <p>Phase 8G: HTTP method semantics contract.
     */
    public static Response getByIdWithAccept(String merchantId, String paymentOrderId, String accept) {
        return RequestSpecs.base()
                .accept(accept)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .when()
                .get(BY_ID_PATH);
    }

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
     * {@code POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund}
     * with an explicit {@code amountMinor} in the body — for partial refund or negative-test variants.
     *
     * <p>Complements {@link #refund} (body {@code {}}, full refund) by allowing the caller to
     * supply a specific amount. Backend behaviour:
     * <ul>
     *   <li>{@code 0 < amountMinor <= capturedAmountMinor} → 200, status REFUNDED,
     *       {@code refundedAmountMinor = amountMinor} (partial refund)</li>
     *   <li>{@code amountMinor > capturedAmountMinor} → 422 {@code refund_amount_exceeds_captured}</li>
     *   <li>{@code amountMinor <= 0} → 422 {@code refund_amount_exceeds_captured}
     *       (same exception class; name is imprecise for the zero/negative case)</li>
     * </ul>
     *
     * <p>The {@code amountMinor} validation fires inside {@code PaymentOrder.refund()} (domain layer),
     * AFTER the service-level ETag check, idempotency reservation, state-machine pre-guard, and PSP call.
     * On validation failure the transaction rolls back — the idempotency record and order state
     * are unchanged, and the request can be retried with a corrected amount.
     *
     * <p>Note: the backend's {@code RefundRequest} record has no bean-validation annotations
     * ({@code @Min}, {@code @Max}). All amount validation is domain-level only.
     *
     * <p>Phase 8F: partial refund and amount validation contract.
     */
    public static Response refundWithAmount(
            String merchantId, String paymentOrderId, String ifMatch,
            String idempotencyKey, long amountMinor) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{\"amountMinor\":" + amountMinor + "}")
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

    /**
     * {@code POST .../authorize} with an explicit {@code reason} field in the request body.
     *
     * <p>Negative-test / conflict variant used in Phase 8D lifecycle idempotency conflict tests.
     * The lifecycle idempotency fingerprint includes the {@code reason} field when it is non-null:
     * {@code {"operation":"...","merchantId":"...","paymentOrderId":"...","action":"AUTHORIZE","reason":"<value>"}}.
     * A prior authorize with body {@code {}} (reason=null) produces a fingerprint that does NOT
     * include {@code reason}. Replaying the same key with {@code {reason:"<value>"}} produces a
     * different fingerprint → fingerprint mismatch → {@code IdempotencyConflictException} → 409.
     *
     * <p>{@code isIdempotentLifecycleReplay()} in {@code PaymentLifecycleService} fires
     * <strong>before</strong> the version check ({@code requireCurrentVersion}) in the service,
     * but the controller calls {@code PaymentEtag.requireVersion(ifMatch)} before calling the
     * service, so {@code If-Match} must be syntactically valid even for the conflict path.
     *
     * <p>Expected: 409 {@code application/problem+json} with {@code error: "idempotency_conflict"}.
     *
     * <p>Phase 8D: lifecycle idempotency replay and conflict contract.
     */
    public static Response authorizeWithReason(
            String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey, String reason) {
        return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
                .contentType(ContentTypes.JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{\"reason\":\"" + reason + "\"}")
                .when()
                .post(AUTHORIZE_PATH);
    }

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders/summary} — aggregate summary.
     *
     * <p>Requires authority: {@code platform:payments:read} OR a JWT {@code merchant_id} claim
     * equal to the {@code merchantId} path parameter. No query parameters — returns the full
     * unfiltered summary for the merchant.
     *
     * <p>Happy-path expectation: 200 with body:
     * {@code { totalOrders, totalAmountMinor, byCurrency:[{currency,orderCount,totalAmountMinor}],
     * byStatus:[{status,orderCount,totalAmountMinor}] }}.
     * {@code byCurrency} is ordered {@code ASC} by currency string.
     * {@code byStatus} is ordered {@code ASC} by status string.
     *
     * <p>Response headers: {@code Cache-Control: no-store}, {@code Vary: Authorization},
     * {@code X-Correlation-ID}.
     *
     * <p>Phase 8B.
     */
    public static Response summary(String merchantId) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .when()
                .get(SUMMARY_PATH);
    }

    /**
     * {@code PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}
     * — update payment order metadata via JSON Merge Patch (RFC 7396).
     *
     * <p>Requires: {@code merchant:payments:lifecycle} (or {@code platform:payments:lifecycle})
     * authority AND a JWT {@code merchant_id} claim equal to the {@code merchantId} path UUID
     * (for merchant-scoped callers). {@code If-Match} is functionally required.
     *
     * <p>Required headers: {@code Content-Type: application/merge-patch+json} (also accepts
     * {@code application/json}), {@code If-Match: "v{N}"} (current ETag).
     *
     * <p>Happy-path expectation: 200 with {@code PaymentLifecycleResponse} body,
     * {@code ETag: "v{N+1}"} (version incremented), {@code Vary: If-Match},
     * {@code Cache-Control: no-store}. The order status is not changed.
     *
     * <p>Guard order in controller (differs from lifecycle actions):
     * <ol>
     *   <li>{@code verifyMerchantOwnership()} — 403 if merchant scope mismatch</li>
     *   <li>{@code request.requireOnlyMetadataTopLevelField()} — 400 if unknown top-level fields</li>
     *   <li>{@code PaymentEtag.requireVersion(ifMatch)} — 428 if null/blank; 412 if stale</li>
     *   <li>{@code paymentLifecycleService.updateMetadata()} — writes new metadata JSON string</li>
     * </ol>
     *
     * <p>SDET learning: the unknown-field check (step 2) fires BEFORE the ETag check (step 3).
     * A request with an unknown field AND a stale {@code If-Match} returns 400, not 412.
     * Contrast with lifecycle actions where the ETag is checked in the service layer (after
     * the fingerprint replay check).
     *
     * <p>Uses {@link RequestSpecs#mergePatch(String)} which sets
     * {@code Content-Type: application/merge-patch+json} without a charset suffix.
     * The charset is suppressed by {@code EncoderConfig} in {@code RestAssuredSetup}.
     *
     * <p>Phase 8E: JSON Merge Patch contract.
     */
    public static Response patch(
            String merchantId, String paymentOrderId, String ifMatch, PatchMetadataRequest body) {
        return RequestSpecs.mergePatch(ifMatch)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body(body)
                .when()
                .patch(BY_ID_PATH);
    }

    /**
     * {@code PATCH .../payment-orders/{paymentOrderId}} without {@code If-Match} header.
     *
     * <p>Negative-test variant: intentionally omits {@code If-Match} to trigger the backend's
     * precondition guard. {@code PaymentEtag.requireVersion(null)} throws
     * {@code PaymentPreconditionRequiredException} → mapped to 428 by
     * {@code PaymentExceptionHandler}.
     *
     * <p>428 Precondition Required (RFC 6585 §3) means "the server requires a precondition
     * that the client did not send". Distinct from 412 Precondition Failed ("the precondition
     * was sent but did not match").
     *
     * <p>Phase 8E.
     */
    public static Response patchWithoutIfMatch(
            String merchantId, String paymentOrderId, PatchMetadataRequest body) {
        return RequestSpecs.base()
                .contentType(ContentTypes.MERGE_PATCH_JSON)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body(body)
                .when()
                .patch(BY_ID_PATH);
    }

    /**
     * {@code PATCH .../payment-orders/{paymentOrderId}} with unsupported {@code Content-Type}.
     *
     * <p>Negative-test variant: sends {@code Content-Type: text/plain} to trigger Spring MVC's
     * content-type negotiation rejection before the controller method is invoked.
     * Expected: 415 with {@code error: "unsupported_media_type"} and an
     * {@code Accept-Patch: application/merge-patch+json} response header.
     *
     * <p>The 415 fires at the Spring MVC dispatcher level ({@code HttpMediaTypeNotSupportedException})
     * — BEFORE merchant ownership, unknown-field, or ETag checks. The {@code Accept-Patch} header
     * (RFC 5789 §3.1) in the error response advertises the correct content-type.
     *
     * <p>Phase 8E.
     */
    public static Response patchWithWrongContentType(
            String merchantId, String paymentOrderId, String ifMatch) {
        return RequestSpecs.base()
                .contentType("text/plain")
                .header(Headers.IF_MATCH, ifMatch)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{}")
                .when()
                .patch(BY_ID_PATH);
    }

    /**
     * {@code PATCH .../payment-orders/{paymentOrderId}} with an unknown top-level field in the body.
     *
     * <p>Negative-test variant: body {@code {"metadata":{},"unknownField":"forbidden"}} contains
     * a field outside the backend's known set. {@code MetadataPatchRequest.@JsonAnySetter}
     * captures the extra field, then {@code requireOnlyMetadataTopLevelField()} throws
     * {@code UnknownMetadataPatchFieldException} → 400 {@code unknown_top_level_field}.
     *
     * <p>The 400 response body includes a {@code details} array with one entry per unknown field name.
     *
     * <p>Guard ordering: this check fires BEFORE {@code PaymentEtag.requireVersion()}, so a
     * request with an unknown field and any (or no) {@code If-Match} returns 400, not 412/428.
     *
     * <p>Phase 8E.
     */
    public static Response patchWithUnknownField(
            String merchantId, String paymentOrderId, String ifMatch) {
        return RequestSpecs.mergePatch(ifMatch)
                .pathParam("merchantId", merchantId)
                .pathParam("paymentOrderId", paymentOrderId)
                .body("{\"metadata\":{},\"unknownField\":\"forbidden\"}")
                .when()
                .patch(BY_ID_PATH);
    }

    /**
     * {@code GET /api/merchants/{merchantId}/payment-orders/summary?currency={currency}}
     * — filtered summary by currency.
     *
     * <p>Negative-test variant: pass an unsupported currency string to trigger the backend's
     * {@code IllegalArgumentException("currency must be PLN, EUR, or USD")} which
     * {@code PaymentExceptionHandler.handleIllegalArgument} maps to
     * 400 {@code application/problem+json} with {@code error: "validation"}.
     *
     * <p>Phase 8B.
     */
    public static Response summaryWithCurrency(String merchantId, String currency) {
        return RequestSpecs.base()
                .pathParam("merchantId", merchantId)
                .queryParam("currency", currency)
                .when()
                .get(SUMMARY_PATH);
    }
}
