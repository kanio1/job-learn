package lab.paymentquality.apitest.api.merchant;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.merchant.dto.CreateMerchantRequest;
import lab.paymentquality.apitest.core.http.ContentTypes;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client facade for {@code /api/merchants} endpoints.
 *
 * <p>This is a one-method-per-endpoint facade over REST Assured. It hides path strings,
 * content-type headers, and request body serialization from scenario classes.
 * Scenarios express only business intent; they never call {@code given/when} directly.
 *
 * <p>Authentication is injected automatically by {@link lab.paymentquality.apitest.core.http.AuthFilter}
 * reading {@link lab.paymentquality.apitest.core.context.Ctx}. Callers must set
 * {@code Ctx.set(TestContext.of(identity))} before invoking any method here.
 *
 * <p>Return types follow the project return-type strategy (see framework plan §12):
 * <ul>
 *   <li>{@link Response} — raw response; scenario inspects status before choosing path.
 *       Used for create (needs Location + status + body) and all negative cases.</li>
 * </ul>
 *
 * <p>SDET learning: the thin-client pattern (Facade over REST Assured) is the correct layer
 * to separate "how to call the API" from "what the test asserts." Each API client owns one
 * resource; scenarios own the oracle logic.
 *
 * <p>SDET interview topic: why return {@code Response} and not {@code MerchantResponse}?
 * Because the HTTP contract (status code, headers) is part of what we test here — returning
 * just the body DTO discards that information before the spec can verify it.
 */
public final class MerchantsApi {

    private static final String BASE_PATH      = "/api/merchants";
    private static final String BY_ID_PATH     = "/api/merchants/{id}";
    private static final String ACTIVATE_PATH  = "/api/merchants/{id}/activate";
    private static final String SUSPEND_PATH   = "/api/merchants/{id}/suspend";

    private MerchantsApi() {}

    /**
     * {@code POST /api/merchants} — create a new merchant.
     *
     * <p>Requires authority: {@code platform:merchants:create}.
     * Happy-path expectation: 201 with body {@code {merchantId, merchantReference, displayName, status: "DRAFT", ...}}.
     * No Location header is set by the backend.
     */
    public static Response create(CreateMerchantRequest req) {
        return RequestSpecs.base()
                .contentType(ContentTypes.JSON)
                .body(req)
                .when()
                .post(BASE_PATH);
    }

    /**
     * {@code GET /api/merchants/{id}} — retrieve a merchant by its UUID.
     *
     * <p>Requires authority: {@code platform:merchants:read}.
     * Happy-path expectation: 200 with body {@code {merchantId, merchantReference, displayName, status, ...}}.
     * Error cases: 400 for non-UUID id, 404 for unknown UUID.
     */
    public static Response getById(String merchantId) {
        return RequestSpecs.base()
                .pathParam("id", merchantId)
                .when()
                .get(BY_ID_PATH);
    }

    /**
     * {@code GET /api/merchants} — list merchants visible to the caller's tenant.
     *
     * <p>Requires authority: {@code platform:merchants:read}.
     * Platform-scoped callers see all merchants. Tenant-scoped callers see only their tenant's merchants.
     * Happy-path expectation: 200 with body {@code {merchants: [{...}, ...]}}.
     */
    public static Response list() {
        return RequestSpecs.base()
                .when()
                .get(BASE_PATH);
    }

    /**
     * {@code GET /api/merchants?tenantId=<ref>} — list merchants filtered by tenant reference.
     *
     * <p>Filtering is only effective for platform-scoped callers. For tenant-scoped callers the
     * filter is ignored and all tenant merchants are returned regardless.
     * If the tenant reference does not exist the backend returns 200 with an empty list.
     */
    public static Response listByTenant(String tenantRef) {
        return RequestSpecs.base()
                .queryParam("tenantId", tenantRef)
                .when()
                .get(BASE_PATH);
    }

    /**
     * {@code POST /api/merchants/{id}/activate} — transition a merchant from DRAFT to ACTIVE.
     *
     * <p>Requires authority: {@code platform:merchants:update-status}.
     * Valid only when the merchant is in DRAFT status; returns 409 with
     * {@code error: "invalid_transition"} for any other starting status.
     * Happy-path expectation: 200 with body where {@code status: "ACTIVE"}.
     */
    public static Response activate(String merchantId) {
        return RequestSpecs.base()
                .pathParam("id", merchantId)
                .when()
                .post(ACTIVATE_PATH);
    }

    /**
     * {@code POST /api/merchants/{id}/suspend} — transition a merchant from ACTIVE to SUSPENDED.
     *
     * <p>Requires authority: {@code platform:merchants:update-status}.
     * Valid only when the merchant is in ACTIVE status; returns 409 with
     * {@code error: "invalid_transition"} for any other starting status.
     * Happy-path expectation: 200 with body where {@code status: "SUSPENDED"}.
     */
    public static Response suspend(String merchantId) {
        return RequestSpecs.base()
                .pathParam("id", merchantId)
                .when()
                .post(SUSPEND_PATH);
    }
}
