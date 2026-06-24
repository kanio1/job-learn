package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderListResponse;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.data.IdempotencyKeys;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment Order API contract foundation spec — Phase 7A.
 *
 * <p><strong>Test category:</strong> Contract — verifies that the payment order resource
 * behaves according to its HTTP/REST contract as observed by a black-box HTTP client.
 * Coverage: GET by ID (body shape, ETag, Cache-Control, Vary), LIST (pagination envelope),
 * 404 not-found, and 400 malformed path parameter. Payment order CREATE is deferred
 * (see class Javadoc) — all tests here are read-only and use seeded data.
 *
 * <p><strong>Why API-level and not unit/MockMvc?</strong> Unit tests cannot verify:
 * header contracts (ETag, Cache-Control, Vary, X-Correlation-ID), JSON serialization field
 * names, HTTP content negotiation, or the interplay between Spring Security's authority
 * checks and the controller's own {@code merchant_id} claim validation.
 * Black-box tests catch all of these with a single HTTP call.
 *
 * <p><strong>HTTP/REST concepts exercised:</strong>
 * <ul>
 *   <li>ETag response header — version identifier; format {@code "vN"} (quoted string).
 *       Required for conditional requests (If-Match) in lifecycle actions.</li>
 *   <li>{@code Cache-Control: no-store} — payment resources must never be cached
 *       (PCI-DSS alignment; contains financial amounts and status).</li>
 *   <li>{@code Vary: Authorization} — caching infrastructure must not share cached payment
 *       responses between different JWT holders even on the same URL.</li>
 *   <li>Paginated collection — backend uses {@code Page<T>} serialized as
 *       {@code {content, page, size, totalElements, totalPages}}.</li>
 *   <li>404 Not Found — masked; does not distinguish "wrong merchant" from "wrong order"
 *       to avoid leaking existence information to unauthorized callers.</li>
 *   <li>400 Bad Request — for syntactically invalid path parameters (non-UUID strings).</li>
 * </ul>
 *
 * <p><strong>Authorization model for reads:</strong>
 * <ul>
 *   <li>{@code platform:payments:read} — granted to {@code platform.payment.reader} Keycloak
 *       user; bypasses the {@code merchant_id} JWT claim check in the controller;
 *       can read any merchant's payment orders cross-tenant.</li>
 *   <li>{@code merchant:payments:read} — granted to merchant-scoped users; requires the JWT
 *       {@code merchant_id} claim to equal the {@code merchantId} path parameter (UUID).</li>
 * </ul>
 * All tests in this spec use {@link Identities#merchantReader} which maps to
 * {@code platform.payment.reader} and carries {@code platform:payments:read}.
 *
 * <p><strong>Why CREATE is deferred (Keycloak realm gap):</strong>
 * {@code POST /api/merchants/{merchantId}/payment-orders} requires both
 * {@code merchant:payments:create} authority and a JWT {@code merchant_id} claim that equals
 * the UUID path param. The enabled Keycloak user ({@code merchant.payment.lifecycle}) has
 * {@code merchant_id = "PLACEHOLDER_MERCHANT_ID"} — a placeholder string, not a UUID.
 * Matching it against a seeded merchant UUID ({@code 00000000-0000-0000-0000-0000000000b1})
 * is impossible without updating the realm. Documented in PHASE_7A; to be resolved in Phase 7B.
 *
 * <p><strong>Data isolation strategy:</strong> {@link SeedApi#seed()} loads a deterministic
 * dataset before this class runs. The seeded merchants are ACTIVE (payment-eligible), and seeded
 * payment orders are in various lifecycle states. All tests use stable IDs from {@link Seeds};
 * no test modifies data, so {@code @AfterEach} reset is not needed between individual tests.
 * {@link SeedApi#reset()} at end-of-class clears data for subsequent specs.
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>Why does ETag use {@code "vN"} format (quoted version string) rather than a hash?</li>
 *   <li>What is the difference between {@code Cache-Control: no-store} and {@code no-cache}?</li>
 *   <li>Why does {@code Vary: Authorization} matter for payment responses?</li>
 *   <li>Why is 404 the correct response for a merchant-scoped payment order when the merchant ID
 *       doesn't match the JWT, rather than 403?</li>
 *   <li>Why use seeded data for GET/LIST tests rather than creating via POST in setup?</li>
 * </ul>
 */
@ApiTest
@DisplayName("Payment Orders API — contract")
class PaymentOrdersContractSpec {

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

    // -------------------------------------------------------------------------
    // GET — body contract
    // -------------------------------------------------------------------------

    /**
     * Contract: GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId} → 200 with body.
     *
     * <p>HTTP concept: GET on an identified resource returns 200 with the full current
     * representation. The payment order body includes financial amounts, currency, status,
     * clientOrderReference, and timestamps.
     *
     * <p>Business risk: if the JSON serializer renames {@code amountMinor} to {@code amount}
     * or {@code clientOrderReference} to {@code reference}, clients break silently. Black-box
     * tests catch this; unit tests do not because they control the serializer directly.
     *
     * <p>SDET: uses {@link Identities#merchantReader} ({@code platform:payments:read}) which
     * bypasses the {@code merchant_id} JWT claim check in the controller. This allows a
     * platform-scoped tester to read any merchant's payment orders. Merchant-scoped access
     * (requiring JWT {@code merchant_id} == path UUID) is deferred to Phase 7B.
     */
    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payment-orders/{id} → 200 with payment body")
    void get_seeded_payment_order_returns_200_with_body() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        PaymentOrderResponse body = PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PaymentOrderResponse.class);

        // Seeded values from Fixtures: SEED-ALPHA-001-CREATED, 1100 PLN, CREATED status
        assertThat(body.paymentOrderId()).isNotNull();
        assertThat(body.merchantId()).isNotNull();
        assertThat(body.clientOrderReference()).isEqualTo("SEED-ALPHA-001-CREATED");
        assertThat(body.amountMinor()).isEqualTo(1_100L);
        assertThat(body.currency()).isEqualTo("PLN");
        assertThat(body.status()).isEqualTo("CREATED");
        assertThat(body.createdAt()).isNotNull();
        assertThat(body.updatedAt()).isNotNull();

        // CREATED order has no lifecycle timestamps
        assertThat(body.authorizedAt()).isNull();
        assertThat(body.capturedAt()).isNull();
        assertThat(body.cancelledAt()).isNull();
        assertThat(body.refundedAt()).isNull();
    }

    /**
     * Contract: GET response includes ETag, Cache-Control: no-store, and Vary: Authorization.
     *
     * <p>HTTP concept: ETag is a version identifier enabling optimistic locking. The backend
     * format is {@code "vN"} where N is the JPA {@code @Version} counter. A CREATED order
     * (no state transitions applied) has version 0, so ETag = {@code "v0"}.
     *
     * <p>Cache-Control: {@code no-store} prevents any proxy or browser from caching payment
     * resources. Caching a payment response could expose financial data to the wrong caller.
     *
     * <p>Vary: Authorization tells caching infrastructure that responses differ by JWT; two
     * callers with different tokens must not share a cached response even for the same URL.
     *
     * <p>SDET interview: why assert headers in a contract test? Because headers are part of
     * the API contract — a backend refactor removing ETag breaks client lifecycle operations
     * (authorize/capture require If-Match), and removing no-store could cause compliance failures.
     */
    @Test
    @DisplayName("GET payment order response includes ETag, Cache-Control: no-store, Vary: Authorization")
    void get_payment_order_response_has_required_security_headers() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.getById(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        response.then().statusCode(200);

        // ETag format: "vN" (quoted, where N is the JPA @Version counter)
        // CREATED order = version 0 → ETag = "v0"
        String etag = response.header(Headers.ETAG);
        assertThat(etag).isNotNull()
                .startsWith("\"v")
                .endsWith("\"");

        // Cache-Control: no-store — payment data must never be cached by intermediaries
        String cacheControl = response.header(Headers.CACHE_CONTROL);
        assertThat(cacheControl).isNotNull().contains("no-store");

        // Vary: Authorization — caching must not share responses across JWT holders
        String vary = response.header(Headers.VARY);
        assertThat(vary).isNotNull().containsIgnoringCase("Authorization");

        // X-Correlation-ID propagated from request context to response
        assertThat(response.header(Headers.CORRELATION_ID)).isNotNull();
    }

    // -------------------------------------------------------------------------
    // GET — other statuses
    // -------------------------------------------------------------------------

    /**
     * Contract: GET an AUTHORIZED payment order returns 200 with expected lifecycle fields.
     *
     * <p>HTTP concept: the same endpoint returns different optional fields depending on the
     * payment order's lifecycle state. An AUTHORIZED order must have non-null {@code authorizedAt}
     * and {@code expiresAt} but null {@code capturedAt}, {@code cancelledAt}, etc.
     * This validates the partial-response contract — not all fields are always populated.
     *
     * <p>SDET: verifies that conditional nullable fields are correctly serialized per lifecycle
     * state. A serializer bug that always includes all fields (including future-state fields)
     * would be caught here but not by a CREATED-only test.
     */
    @Test
    @DisplayName("GET AUTHORIZED payment order → 200 with authorizedAt and expiresAt populated")
    void get_authorized_payment_order_returns_200_with_lifecycle_timestamps() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        PaymentOrderResponse body = PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_AUTHORIZED_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PaymentOrderResponse.class);

        assertThat(body.status()).isEqualTo("AUTHORIZED");
        assertThat(body.currency()).isEqualTo("EUR");
        assertThat(body.amountMinor()).isEqualTo(2_200L);
        // AUTHORIZED orders must have these timestamps
        assertThat(body.authorizedAt()).isNotNull();
        assertThat(body.expiresAt()).isNotNull();
        // AUTHORIZED order must NOT yet have capture/cancel/refund timestamps
        assertThat(body.capturedAt()).isNull();
        assertThat(body.cancelledAt()).isNull();
        assertThat(body.refundedAt()).isNull();
    }

    // -------------------------------------------------------------------------
    // LIST — pagination envelope
    // -------------------------------------------------------------------------

    /**
     * Contract: GET /api/merchants/{merchantId}/payment-orders → 200 with pagination envelope.
     *
     * <p>HTTP concept: paginated collection GET returns a 200 with an envelope containing
     * {@code content} (the current page items), {@code page} (0-based page index),
     * {@code size} (page size), {@code totalElements} (total across all pages),
     * and {@code totalPages}.
     *
     * <p>Business risk: if the backend changes the envelope field name from {@code content} to
     * {@code items} or {@code orders}, clients break. This test catches field-name drift in the
     * collection contract.
     *
     * <p>SDET: MERCHANT_ALPHA_001 has 3 named seeded orders + 98 pagination orders = 101 total.
     * The default page size is 20, so {@code totalElements >= 101} and {@code totalPages >= 6}.
     * Asserting ">= 1" on content is safe regardless of pagination defaults.
     */
    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payment-orders → 200 with pagination envelope")
    void list_payment_orders_returns_200_with_pagination_envelope() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        PaymentOrderListResponse body = PaymentOrdersApi.list(Seeds.MERCHANT_ALPHA_001_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PaymentOrderListResponse.class);

        // Envelope structure contract
        assertThat(body.content()).isNotNull().isNotEmpty();
        assertThat(body.page()).isGreaterThanOrEqualTo(0);
        assertThat(body.size()).isGreaterThan(0);
        assertThat(body.totalElements()).isGreaterThanOrEqualTo(101L);
        assertThat(body.totalPages()).isGreaterThanOrEqualTo(1);

        // Content items deserialized correctly
        PaymentOrderResponse first = body.content().get(0);
        assertThat(first.paymentOrderId()).isNotNull();
        assertThat(first.merchantId()).isNotNull();
        assertThat(first.amountMinor()).isGreaterThan(0L);
        assertThat(first.currency()).isNotBlank();
        assertThat(first.status()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // NOT FOUND — 404
    // -------------------------------------------------------------------------

    /**
     * Contract: GET /api/merchants/{merchantId}/payment-orders/{unknownId} → 404 Not Found.
     *
     * <p>HTTP concept: 404 means the resource identified by this URL does not exist.
     * The backend returns the same 404 shape whether the payment order doesn't exist or
     * the merchant doesn't own it — this "masking" prevents probing callers from
     * discovering which merchant IDs are valid by observing 403 vs 404 responses.
     *
     * <p>The payment error handler ({@code PaymentExceptionHandler}) sets
     * {@code Content-Type: application/problem+json} on ALL error responses, so
     * {@link ProblemAssert#hasContentTypeProblemJson()} is safe to use here (unlike
     * merchant errors where some handlers don't set it).
     *
     * <p>SDET: always use a freshly generated UUID for "not found" tests — a hardcoded UUID
     * could coincidentally match a seeded record, converting a stable 404 into a flaky 200.
     */
    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payment-orders/{unknownId} → 404 not found")
    void get_unknown_payment_order_returns_404() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        ProblemAssert.assertThat(PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        UUID.randomUUID().toString()))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND);
    }

    // -------------------------------------------------------------------------
    // VALIDATION — 400
    // -------------------------------------------------------------------------

    /**
     * Contract: GET with non-UUID paymentOrderId → 400 validation error.
     *
     * <p>HTTP concept: 400 for a syntactically invalid path parameter — the UUID conversion
     * fails before any database lookup. This is distinct from 404 (syntactically valid UUID,
     * but no matching record).
     *
     * <p>The backend's {@code PaymentExceptionHandler.handleTypeMismatch()} maps
     * {@code MethodArgumentTypeMismatchException} to 400 with {@code error: "validation"}
     * and a message "Invalid {paramName}: must be a valid UUID".
     *
     * <p>SDET interview: why 400 and not 404 for a malformed UUID? The request is syntactically
     * invalid — it's not a question of whether the resource exists. 400 tells the client to fix
     * their URL construction, not to try a different ID. 404 would falsely imply a valid lookup
     * was attempted.
     */
    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payment-orders/{id} with non-UUID id → 400")
    void get_payment_order_with_malformed_id_returns_400() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        ProblemAssert.assertThat(PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        "not-a-uuid"))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.VALIDATION);
    }

    // -------------------------------------------------------------------------
    // CREATE — POST /api/merchants/{merchantId}/payment-orders (Phase 7B)
    // -------------------------------------------------------------------------

    /**
     * Contract: POST /api/merchants/{merchantId}/payment-orders → 201 with body, Location, ETag.
     *
     * <p>HTTP concept: POST to a collection resource creates a new subordinate resource.
     * On success the response carries:
     * <ul>
     *   <li>{@code 201 Created} — resource was created (not 200 OK).</li>
     *   <li>{@code Location} header — the canonical URL of the newly created payment order.</li>
     *   <li>{@code ETag: "v0"} — initial JPA version; required for subsequent If-Match lifecycle ops.</li>
     *   <li>{@code Vary: Authorization, Idempotency-Key} — both affect the cached response.</li>
     * </ul>
     *
     * <p>Authorization: {@code merchant:payments:create} authority (from {@code MERCHANT_MANAGER}
     * composite role) AND JWT {@code merchant_id} claim must equal the path UUID.
     * {@link Identities#seededMerchantCreator()} uses {@code merchant.alpha.creator} whose
     * Keycloak {@code merchant_id} attribute is the exact UUID of seeded MERCHANT_ALPHA_001.
     *
     * <p>SDET: this is the first write test in the payment order spec. Uses
     * {@code UniqueReferences.paymentRef()} to ensure a fresh {@code clientOrderReference} per run,
     * avoiding idempotency key collisions across test executions.
     */
    @Test
    @DisplayName("POST /api/merchants/{merchantId}/payment-orders → 201 with Location, ETag, body")
    void create_payment_order_returns_201_with_body_and_headers() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        String clientRef = UniqueReferences.paymentRef("create-happy");
        String idempotencyKey = IdempotencyKeys.generate("create");

        Response response = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(5_000L, "PLN", clientRef),
                idempotencyKey);

        response.then().statusCode(201);

        // Location header points to the new payment order resource
        String location = response.header(Headers.LOCATION);
        assertThat(location).isNotNull()
                .contains("/api/merchants/" + Seeds.MERCHANT_ALPHA_001_ID + "/payment-orders/");

        // ETag: "v0" — version 0, first write, no transitions applied yet
        String etag = response.header(Headers.ETAG);
        assertThat(etag).isEqualTo("\"v0\"");

        // Body: CREATED status with expected field values
        PaymentOrderResponse body = response.as(PaymentOrderResponse.class);
        assertThat(body.paymentOrderId()).isNotNull();
        assertThat(body.merchantId().toString()).isEqualTo(Seeds.MERCHANT_ALPHA_001_ID);
        assertThat(body.clientOrderReference()).isEqualTo(clientRef);
        assertThat(body.amountMinor()).isEqualTo(5_000L);
        assertThat(body.currency()).isEqualTo("PLN");
        assertThat(body.status()).isEqualTo("CREATED");
        assertThat(body.createdAt()).isNotNull();
        // No lifecycle timestamps on freshly created order
        assertThat(body.authorizedAt()).isNull();
        assertThat(body.capturedAt()).isNull();
        assertThat(body.cancelledAt()).isNull();
    }

    /**
     * Contract: POST with JWT {@code merchant_id} claim that does not match the path UUID → 403.
     *
     * <p>HTTP concept: the backend enforces a merchant-scope check before service logic.
     * The JWT {@code merchant_id} claim is compared with the {@code merchantId} path parameter.
     * Mismatch throws {@code AccessDeniedException} → Spring Security maps to 403 Forbidden.
     *
     * <p>Scenario: {@link Identities#seededMerchantCreator()} has {@code merchant_id} claim
     * {@code "…b1"} (MERCHANT_ALPHA_001). Calling POST on MERCHANT_ALPHA_002 ({@code "…b2"})
     * triggers the mismatch. This verifies the controller-level security check is actually active
     * — not just that the role is present.
     *
     * <p>SDET: this is a negative security test. It is distinct from a missing-authority 403
     * because the caller HAS {@code merchant:payments:create} — the failure is merchant-scope
     * binding, not role assignment.
     */
    @Test
    @DisplayName("POST with mismatched merchant_id claim → 403 forbidden")
    void create_with_mismatched_merchant_scope_returns_403() {
        // seededMerchantCreator has merchant_id claim = MERCHANT_ALPHA_001_ID
        // Attempting to create for MERCHANT_ALPHA_002 must be rejected
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.create(
                        Seeds.MERCHANT_ALPHA_002_ID,
                        CreatePaymentOrderRequest.valid(1_000L, "EUR",
                                UniqueReferences.paymentRef("scope-mismatch")),
                        IdempotencyKeys.generate("scope-mismatch")))
                .hasStatus(403)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.FORBIDDEN);
    }

    /**
     * Contract: POST without {@code Idempotency-Key} header → 400 validation error.
     *
     * <p>HTTP concept: idempotency key is a required header for payment order create.
     * Missing it returns the same {@code validation} error shape as bean validation failures
     * — the backend uses a single error handler for all input validation problems.
     *
     * <p>SDET: uses {@link PaymentOrdersApi#createWithoutIdempotencyKey} — a negative-test variant
     * that intentionally omits the header. The facade isolates this "intentionally wrong" call
     * so the scenario remains readable without embedding raw request-builder logic.
     */
    @Test
    @DisplayName("POST without Idempotency-Key header → 400 validation")
    void create_without_idempotency_key_returns_400() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.createWithoutIdempotencyKey(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        CreatePaymentOrderRequest.valid(1_000L, "PLN",
                                UniqueReferences.paymentRef("no-idem-key"))))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.VALIDATION);
    }
}
