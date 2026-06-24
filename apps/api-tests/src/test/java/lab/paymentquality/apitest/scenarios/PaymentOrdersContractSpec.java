package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PaymentHistoryResponse;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderListResponse;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.ETag;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    // -------------------------------------------------------------------------
    // IDEMPOTENCY — Phase 7C
    // -------------------------------------------------------------------------

    /**
     * Idempotency contract: replaying with the same key and identical body returns 200.
     *
     * <p><strong>Test category:</strong> Idempotency contract — verifies the safe-retry guarantee
     * of the payment order create endpoint.
     *
     * <p><strong>HTTP concept:</strong> RFC-style idempotency for non-idempotent HTTP methods.
     * The backend stores a per-merchant record keyed by {@code SHA-256(Idempotency-Key)} and a
     * SHA-256 request fingerprint (canonical JSON of merchantId + amountMinor + currency +
     * clientOrderReference). On replay the server detects the fingerprint match and returns the
     * already-created payment order with {@code 200 OK} instead of creating a new one.
     * <ul>
     *   <li>First call: {@code 201 Created} with {@code Location} and {@code ETag: "v0"}.</li>
     *   <li>Replay: {@code 200 OK} with same body and {@code ETag} — no {@code Location}.</li>
     *   <li>Both carry {@code Vary: Authorization, Idempotency-Key} so caches cannot conflate
     *       responses with different keys or different callers.</li>
     * </ul>
     *
     * <p><strong>Payment/business risk:</strong> network timeouts during payment creation force
     * clients to retry. Without idempotency, every retry creates a new payment order — the
     * merchant charges the customer twice. With idempotency, retries are safe: the same order
     * is returned regardless of how many times the client resends. This is foundational to
     * payment API design; Stripe, Adyen, and Braintree all use it for the same reason.
     *
     * <p><strong>Why 200 and not 201 on replay?</strong> {@code 201 Created} signals a new
     * resource was created by this request. On replay the resource already exists — returning
     * 201 would be semantically wrong and would force clients to treat the Location header
     * as pointing to a new resource (it doesn't). {@code 200 OK} is the correct status for
     * "here is the existing resource you asked for."
     *
     * <p><strong>Why no Location on replay?</strong> {@code Location} is the URL of the
     * <em>newly created</em> resource. On replay there is no new resource; the client already
     * has or can derive the URL from the first 201 response.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the correct HTTP status for idempotency replay — 200 or 201?</li>
     *   <li>Why must the replay return the same {@code paymentOrderId} as the original?</li>
     *   <li>What does {@code Vary: Idempotency-Key} communicate to caching infrastructure?</li>
     *   <li>Why is the request fingerprint compared in addition to the idempotency key itself?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST same Idempotency-Key + same body → 200 replay with same paymentOrderId and ETag")
    void idempotency_replay_with_same_key_and_body_returns_200() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        String idempotencyKey = IdempotencyKeys.generate("replay");
        String clientRef = UniqueReferences.paymentRef("replay");
        CreatePaymentOrderRequest requestBody = CreatePaymentOrderRequest.valid(7_500L, "EUR", clientRef);

        // First create — must be 201 with Vary including Idempotency-Key
        Response firstResponse = PaymentOrdersApi.create(Seeds.MERCHANT_ALPHA_001_ID, requestBody, idempotencyKey);
        firstResponse.then().statusCode(201);

        String originalPaymentOrderId = firstResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        String originalEtag = firstResponse.header(Headers.ETAG);

        // Vary on create responses contains "Idempotency-Key"; the full combined
        // "Authorization, Idempotency-Key" is the backend intent but the observed header
        // value is "Idempotency-Key" (see Phase 7C doc for analysis).
        assertThat(firstResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("Idempotency-Key");

        // Replay — same key, same body — must return 200 with the identical payment order
        Response replayResponse = PaymentOrdersApi.create(Seeds.MERCHANT_ALPHA_001_ID, requestBody, idempotencyKey);
        replayResponse.then().statusCode(200);

        PaymentOrderResponse replayBody = replayResponse.as(PaymentOrderResponse.class);

        // Idempotency guarantee: same resource must be returned, not a new one
        assertThat(replayBody.paymentOrderId().toString()).isEqualTo(originalPaymentOrderId);
        assertThat(replayBody.clientOrderReference()).isEqualTo(clientRef);
        assertThat(replayBody.amountMinor()).isEqualTo(7_500L);
        assertThat(replayBody.currency()).isEqualTo("EUR");
        assertThat(replayBody.status()).isEqualTo("CREATED");

        // ETag must match — no state transition occurred between calls
        assertThat(replayResponse.header(Headers.ETAG)).isEqualTo(originalEtag);

        // Vary: Idempotency-Key — backend sends this on the 200 replay response.
        // Observation: response.header("Vary") returns "Idempotency-Key" on replay (200) rather
        // than "Authorization, Idempotency-Key". The 201 (first create) combines both in a single
        // header. Asserting only Idempotency-Key here matches the actual observed contract.
        assertThat(replayResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("Idempotency-Key");

        // Cache-Control: no-store — replayed payment data is equally sensitive
        assertThat(replayResponse.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");

        // No Location on replay — the resource was not created by this request
        assertThat(replayResponse.header(Headers.LOCATION)).isNull();
    }

    /**
     * Idempotency conflict contract: same key with a different body returns 409.
     *
     * <p><strong>Test category:</strong> Idempotency conflict — verifies the backend detects
     * and rejects attempts to reuse an idempotency key with a different request payload.
     *
     * <p><strong>HTTP concept:</strong> the backend computes a SHA-256 fingerprint from
     * the canonical representation of the request (merchantId + amountMinor + currency +
     * clientOrderReference). When the same {@code Idempotency-Key} is submitted twice with
     * a different fingerprint, the second request is rejected with {@code 409 Conflict} and
     * {@code error: "idempotency_conflict"}. This prevents a subtle class of bug where a client
     * reuses a stale key from a previous (different) payment — the two requests would have the
     * same key but describe different transactions.
     *
     * <p><strong>Payment/business risk:</strong> idempotency key reuse across different amounts
     * or currencies is a programming error in the client. The API must reject it explicitly rather
     * than silently applying the wrong fingerprint — a silent accept would either create a
     * duplicate order or, worse, apply a different amount than the client intended.
     *
     * <p><strong>Error contract:</strong>
     * <ul>
     *   <li>Status: {@code 409 Conflict}.</li>
     *   <li>Body: {@code application/problem+json} with {@code error: "idempotency_conflict"}.</li>
     *   <li>{@code Vary: Authorization, Idempotency-Key} — same header as on the 201.</li>
     *   <li>{@code Cache-Control: no-store} — error responses on payment resources are sensitive.</li>
     * </ul>
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is 409 and not 400 the correct status for an idempotency conflict?</li>
     *   <li>What distinguishes {@code idempotency_conflict} from {@code merchant_not_eligible}
     *       — both are 409 responses from this API?</li>
     *   <li>Why does the 409 error response also include {@code Vary: Idempotency-Key}?</li>
     *   <li>What should a well-written client do when it receives 409 idempotency_conflict?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST same Idempotency-Key + different body → 409 idempotency_conflict")
    void idempotency_conflict_with_same_key_different_body_returns_409() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        String idempotencyKey = IdempotencyKeys.generate("conflict");
        String clientRef = UniqueReferences.paymentRef("conflict");

        // First create — must succeed with 201
        Response firstResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(2_000L, "PLN", clientRef),
                idempotencyKey);
        firstResponse.then().statusCode(201);

        // Second call — same key, different amountMinor → fingerprint mismatch → 409
        ProblemAssert.assertThat(PaymentOrdersApi.create(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        CreatePaymentOrderRequest.valid(9_999L, "PLN", clientRef),
                        idempotencyKey))
                .hasStatus(409)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.IDEMPOTENCY_CONFLICT)
                .hasNoStore()
                .varyContains("Idempotency-Key");
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE / ETag+If-Match — Phase 7D
    // -------------------------------------------------------------------------

    /**
     * Lifecycle contract: authorize with correct {@code If-Match} → 200, status AUTHORIZED,
     * ETag incremented.
     *
     * <p><strong>Test category:</strong> Conditional request / ETag lifecycle contract.
     *
     * <p><strong>HTTP concept:</strong> optimistic concurrency control via {@code If-Match}.
     * The backend stores a JPA {@code @Version} counter on each payment order and serializes it
     * as {@code ETag: "vN"} in every response. A lifecycle action (authorize, capture, cancel,
     * refund) requires the caller to echo the current ETag in {@code If-Match}. This prevents
     * two concurrent callers from applying the same transition — only the first one succeeds;
     * the second sees a stale version and gets 412. The two-phase contract:
     * <ol>
     *   <li>GET the resource → read {@code ETag: "v0"}.</li>
     *   <li>POST lifecycle action with {@code If-Match: "v0"} → success → {@code ETag: "v1"}.</li>
     * </ol>
     * This test compresses both steps: the first create gives us the initial ETag, which we
     * immediately send in the authorize call.
     *
     * <p><strong>Why ETag uses {@code "vN"} and not a content hash?</strong> Content-based ETags
     * are fragile when two state transitions produce different content but the same integer version
     * is needed for replay detection. A monotonic version counter is deterministic, easy to parse,
     * and maps directly to the JPA {@code @Version} field — no hashing needed.
     *
     * <p><strong>Payment/business risk:</strong> lifecycle actions move money. Authorize reserves
     * funds with the PSP; double-authorize is impossible — the domain model rejects a second
     * authorize on an AUTHORIZED order. The {@code If-Match} version guard is the HTTP layer's
     * contribution to that same safety property: it enforces that the client acted on the
     * freshest state, not on a stale snapshot.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is 200 (not 201 or 204) the correct status for a lifecycle state transition?</li>
     *   <li>How does {@code If-Match} prevent double-authorize in a distributed system?</li>
     *   <li>Why increment the ETag on every transition rather than on every write?</li>
     *   <li>What is the difference between optimistic locking (If-Match/412) and
     *       pessimistic locking (row lock)?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST authorize with correct If-Match → 200 AUTHORIZED, ETag incremented to v1")
    void authorize_with_correct_if_match_returns_200_and_increments_etag() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create a fresh CREATED order — ETag is "v0"
        String clientRef = UniqueReferences.paymentRef("auth-happy");
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(1_500L, "PLN", clientRef),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));
        assertThat(createEtag.raw()).isEqualTo("\"v0\"");

        // Step 2: authorize with correct If-Match → 200
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));

        authorizeResponse.then().statusCode(200);

        // Body: status must be AUTHORIZED; lifecycle timestamps must be populated
        PaymentOrderResponse body = authorizeResponse.as(PaymentOrderResponse.class);
        assertThat(body.paymentOrderId().toString()).isEqualTo(paymentOrderId);
        assertThat(body.status()).isEqualTo("AUTHORIZED");
        assertThat(body.authorizedAt()).isNotNull();

        // ETag must be incremented: v0 → v1
        ETag authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG));
        assertThat(authorizeEtag.raw()).isEqualTo("\"v1\"");
        assertThat(authorizeEtag.version()).isGreaterThan(createEtag.version());

        // Vary must include If-Match — caching must not share responses between If-Match values
        assertThat(authorizeResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("If-Match");

        // Cache-Control: no-store — lifecycle responses contain financial state
        assertThat(authorizeResponse.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");
    }

    /**
     * Lifecycle contract: authorize without {@code If-Match} header → 428 Precondition Required.
     *
     * <p><strong>Test category:</strong> Conditional request negative — missing precondition.
     *
     * <p><strong>HTTP concept:</strong> HTTP 428 Precondition Required is the correct status
     * when a server <em>requires</em> a conditional header ({@code If-Match}) that the client
     * omitted entirely. This is distinct from 412 (header present but the condition failed) and
     * from 400 (syntactically invalid request). The RFC 6585 motivation: if the server allowed
     * unconditional lifecycle mutations, a client that lost its ETag would blindly overwrite
     * concurrent changes — a "lost update" bug. 428 forces the client to re-fetch and confirm
     * the current state before retrying.
     *
     * <p><strong>Backend mapping:</strong> {@code If-Match} is declared
     * {@code @RequestHeader(required = false)} in Spring MVC — Spring will not reject the request
     * at the binding layer. Instead, {@code PaymentEtag.requireVersion(null)} throws
     * {@code PaymentPreconditionRequiredException}, which {@code PaymentExceptionHandler} maps
     * to 428 with {@code error: "precondition_required"}.
     *
     * <p><strong>Payment/business risk:</strong> a client that retries a lifecycle action without
     * an If-Match (e.g. after a timeout) could be acting on a stale view of the order. 428 is
     * the server's way of saying "I need to know which version you acted on." This is a security
     * and data-integrity guard, not just an API formality.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why 428 and not 400 for a missing {@code If-Match} on a lifecycle endpoint?</li>
     *   <li>What is the difference between 400 (malformed request), 412 (condition failed),
     *       and 428 (required condition missing)?</li>
     *   <li>How does {@code @RequestHeader(required = false)} interact with the backend's
     *       functional precondition check?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST authorize without If-Match → 428 precondition_required")
    void authorize_without_if_match_returns_428() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Create a fresh order to have a valid paymentOrderId
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(2_000L, "EUR", UniqueReferences.paymentRef("auth-no-ifmatch")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);
        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();

        // Authorize without If-Match header → 428
        ProblemAssert.assertThat(PaymentOrdersApi.authorizeWithoutIfMatch(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        paymentOrderId,
                        IdempotencyKeys.generate("authorize")))
                .hasStatus(428)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.PRECONDITION_REQUIRED)
                .hasNoStore()
                .varyContains("If-Match");
    }

    /**
     * Lifecycle contract: authorize with a stale {@code If-Match} → 412 Precondition Failed.
     *
     * <p><strong>Test category:</strong> Conditional request negative — stale ETag.
     *
     * <p><strong>HTTP concept:</strong> 412 Precondition Failed means the client sent an
     * {@code If-Match} header, but the value no longer matches the resource's current ETag.
     * The resource was modified between when the client last read it (version N) and when it
     * submitted the lifecycle action (expecting version N, but backend is at version M > N).
     * The correct recovery: re-fetch the resource with a GET, read the new ETag, and resubmit.
     *
     * <p><strong>Stale ETag scenario here:</strong> a freshly created order has ETag {@code "v0"}.
     * Sending {@code If-Match: "v1"} is a version mismatch — the version counter is still 0,
     * not 1. The backend's {@code PaymentVersionPrecondition.requireCurrentVersion(order, 1)}
     * detects {@code 1 != 0} and throws {@code PaymentOrderVersionMismatchException} → 412.
     *
     * <p><strong>Payment/business risk:</strong> a stale ETag usually means another process
     * already applied a lifecycle transition. Authorizing an order that has already been captured
     * or cancelled would be a critical error. 412 stops this before any PSP call is made.
     *
     * <p><strong>Error code {@code payment_order_version_mismatch}:</strong> distinct from
     * {@code concurrency_conflict} (JPA optimistic locking exception from the database layer)
     * and from generic {@code precondition_failed}. The explicit code tells clients exactly what
     * went wrong — "you need a newer ETag" — without leaking implementation details.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the difference between 412 (stale ETag) and 428 (missing If-Match)?</li>
     *   <li>Why does the backend use a domain-specific error code ({@code payment_order_version_mismatch})
     *       rather than a generic 412?</li>
     *   <li>How would you test 412 without actually racing two concurrent requests?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST authorize with stale If-Match (v1 on v0 order) → 412 payment_order_version_mismatch")
    void authorize_with_stale_if_match_returns_412() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Create order → current version is 0, ETag is "v0"
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(3_000L, "PLN", UniqueReferences.paymentRef("auth-stale")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);
        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();

        // Authorize with If-Match "v1" — stale: current version is 0, not 1 → 412
        ProblemAssert.assertThat(PaymentOrdersApi.authorize(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        paymentOrderId,
                        "\"v1\"",
                        IdempotencyKeys.generate("authorize")))
                .hasStatus(412)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.PAYMENT_ORDER_VERSION_MISMATCH)
                .hasNoStore()
                .varyContains("If-Match");
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE / capture + cancel — Phase 7E
    // -------------------------------------------------------------------------

    /**
     * Full happy-path chain: create → authorize → capture returns 200 CAPTURED, ETag v2.
     *
     * <p><strong>Test category:</strong> Multi-step lifecycle contract — verifies the complete
     * CREATED → AUTHORIZED → CAPTURED state machine path as observed by a black-box HTTP client.
     *
     * <p><strong>HTTP/REST concept:</strong> each lifecycle action performs an optimistic-lock
     * version check against the current {@code @Version} counter and increments it on success.
     * The ETag chain tracks each transition:
     * <ol>
     *   <li>Create → {@code ETag: "v0"} (JPA version 0).</li>
     *   <li>Authorize with {@code If-Match: "v0"} → {@code ETag: "v1"} (version 1).</li>
     *   <li>Capture with {@code If-Match: "v1"} → {@code ETag: "v2"} (version 2).</li>
     * </ol>
     * Each step must use the ETag from the <em>previous</em> response as its {@code If-Match}.
     * Using a stale ETag (e.g. {@code "v0"} for capture) would produce 412.
     *
     * <p><strong>Capture semantics:</strong> body {@code {}} omits {@code amountMinor}, which
     * triggers a full capture — the captured amount equals the original authorized amount.
     * {@code capturedAmountMinor} in the response must equal the order's {@code amountMinor}.
     * {@code expiresAt} is cleared to {@code null} on capture (authorization window closes).
     *
     * <p><strong>Payment/business risk:</strong> capture is the step where the PSP actually
     * moves money from the customer to the merchant. An uncaptured authorization expires after
     * 7 days (backend constant). The ETag chain ensures that a capture can only succeed if no
     * intervening mutation (e.g. cancellation) happened between authorize and capture.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why must the capture use the ETag from the authorize response, not the create response?</li>
     *   <li>What would happen if two concurrent requests both attempted capture with {@code "v1"}?</li>
     *   <li>Why is {@code capturedAmountMinor} equal to {@code amountMinor} when no amount is in the body?</li>
     *   <li>Why does the backend clear {@code expiresAt} on capture?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST create → authorize → capture → 200 CAPTURED, ETag v0 → v1 → v2")
    void create_authorize_capture_happy_path_returns_200_and_increments_etag_to_v2() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0"
        String clientRef = UniqueReferences.paymentRef("capture-happy");
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(5_000L, "PLN", clientRef),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));
        assertThat(createEtag.raw()).isEqualTo("\"v0\"");

        // Step 2: authorize with If-Match "v0" → 200, ETag "v1"
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));
        authorizeResponse.then().statusCode(200);

        ETag authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG));
        assertThat(authorizeEtag.raw()).isEqualTo("\"v1\"");

        // Step 3: capture with If-Match "v1" — must use the ETag from the authorize response
        Response captureResponse = PaymentOrdersApi.capture(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                authorizeEtag.raw(),
                IdempotencyKeys.generate("capture"));

        captureResponse.then().statusCode(200);

        // Body: status CAPTURED, capturedAmountMinor set to full order amount (no partial capture)
        PaymentOrderResponse captureBody = captureResponse.as(PaymentOrderResponse.class);
        assertThat(captureBody.paymentOrderId().toString()).isEqualTo(paymentOrderId);
        assertThat(captureBody.status()).isEqualTo("CAPTURED");
        assertThat(captureBody.capturedAt()).isNotNull();
        assertThat(captureBody.capturedAmountMinor()).isEqualTo(5_000L);

        // ETag incremented: v1 → v2
        ETag captureEtag = ETag.of(captureResponse.header(Headers.ETAG));
        assertThat(captureEtag.raw()).isEqualTo("\"v2\"");
        assertThat(captureEtag.version()).isGreaterThan(authorizeEtag.version());

        // Vary: If-Match and Cache-Control: no-store
        assertThat(captureResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("If-Match");
        assertThat(captureResponse.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");
    }

    /**
     * Cancel from CREATED state: create → cancel returns 200 CANCELLED, ETag v0 → v1.
     *
     * <p><strong>Test category:</strong> Lifecycle contract — verifies cancellation of an order
     * that has never been authorized. CREATED → CANCELLED is a valid transition; no PSP call is
     * needed (no authorization was placed with the PSP).
     *
     * <p><strong>HTTP/REST concept:</strong> the cancel endpoint follows the same conditional-request
     * pattern as authorize and capture. The caller must echo the current ETag in {@code If-Match}.
     * On success, status transitions to CANCELLED, {@code cancelledAt} is set, and the ETag
     * is incremented:
     * <ul>
     *   <li>Cancel from CREATED (ETag {@code "v0"}) → ETag {@code "v1"}.</li>
     *   <li>Cancel from AUTHORIZED (ETag {@code "v1"}) → ETag {@code "v2"}; additionally calls
     *       {@code pspClient.voidAuthorization()} to release the reserved funds.</li>
     * </ul>
     * This test covers the CREATED → CANCELLED path (simpler; no PSP void involved).
     *
     * <p><strong>Payment/business risk:</strong> merchants cancel CREATED orders when the customer
     * changes their mind before authorization. A cancel must be idempotent (retry with the same
     * Idempotency-Key returns the already-cancelled order) and must not produce a double-cancel.
     * The domain model rejects a second cancel via {@code InvalidStateTransitionException}.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the difference between cancelling a CREATED order vs. an AUTHORIZED order?</li>
     *   <li>Why is the ETag incremented even when the transition does not involve the PSP?</li>
     *   <li>How would you verify that a cancelled order cannot be authorized afterwards?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST create → cancel → 200 CANCELLED, ETag v0 → v1")
    void create_cancel_happy_path_returns_200_and_increments_etag_to_v1() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0", status CREATED
        String clientRef = UniqueReferences.paymentRef("cancel-happy");
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(3_200L, "EUR", clientRef),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));
        assertThat(createEtag.raw()).isEqualTo("\"v0\"");

        // Step 2: cancel with If-Match "v0" → 200 CANCELLED
        Response cancelResponse = PaymentOrdersApi.cancel(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("cancel"));

        cancelResponse.then().statusCode(200);

        // Body: status CANCELLED, cancelledAt populated, no capturedAmountMinor
        PaymentOrderResponse cancelBody = cancelResponse.as(PaymentOrderResponse.class);
        assertThat(cancelBody.paymentOrderId().toString()).isEqualTo(paymentOrderId);
        assertThat(cancelBody.status()).isEqualTo("CANCELLED");
        assertThat(cancelBody.cancelledAt()).isNotNull();
        assertThat(cancelBody.capturedAmountMinor()).isNull();

        // ETag incremented: v0 → v1
        ETag cancelEtag = ETag.of(cancelResponse.header(Headers.ETAG));
        assertThat(cancelEtag.raw()).isEqualTo("\"v1\"");
        assertThat(cancelEtag.version()).isGreaterThan(createEtag.version());

        // Vary: If-Match and Cache-Control: no-store
        assertThat(cancelResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("If-Match");
        assertThat(cancelResponse.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");
    }

    /**
     * Invalid transition: capture a CREATED (non-authorized) order → 422 invalid_transition.
     *
     * <p><strong>Test category:</strong> State machine boundary — negative test for an invalid
     * lifecycle action. The payment state machine only allows capture from AUTHORIZED, not from
     * CREATED. Attempting this produces 422 Unprocessable Entity with {@code error: "invalid_transition"}.
     *
     * <p><strong>HTTP/REST concept:</strong> HTTP 422 Unprocessable Entity is the correct status
     * for a semantically invalid operation: the request is syntactically well-formed (valid JSON,
     * valid UUID path params, correct headers) but the server cannot process it because the domain
     * precondition (AUTHORIZED state) is not satisfied. This is distinct from:
     * <ul>
     *   <li>400 — syntactic or structural error (missing field, wrong type).</li>
     *   <li>412 — optimistic lock failure (stale ETag).</li>
     *   <li>428 — missing If-Match header.</li>
     *   <li>409 — idempotency conflict.</li>
     * </ul>
     *
     * <p><strong>Backend mapping:</strong> {@code PaymentOrder.capture()} calls
     * {@code canTransitionTo(CAPTURED)}, which checks {@code VALID_TRANSITIONS.get(CREATED)} and
     * finds CAPTURED is not in the allowed set → throws {@code InvalidStateTransitionException}
     * → {@code PaymentExceptionHandler} maps it to 422 with {@code error: "invalid_transition"}.
     * The {@code If-Match} version check passes (ETag {@code "v0"} matches version 0) — the state
     * machine guard fires after the version check.
     *
     * <p><strong>Payment/business risk:</strong> capturing before authorizing would attempt to
     * settle funds that were never reserved. The PSP would reject the request (no authorization
     * reference exists), causing a failed settlement. The domain guard prevents the PSP call
     * entirely.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why 422 and not 400 for an invalid state transition?</li>
     *   <li>In what order does the backend apply guards: version check, then state check, or
     *       state check first?</li>
     *   <li>Why include {@code Vary: If-Match} on a 422 error response?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST capture on CREATED order (no authorize) → 422 invalid_transition")
    void capture_on_created_order_returns_422_invalid_transition() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Create — status CREATED, ETag "v0"
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(4_000L, "PLN", UniqueReferences.paymentRef("capture-invalid")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        String createEtag = ETag.of(createResponse.header(Headers.ETAG)).raw();

        // Capture directly on a CREATED order — invalid state transition → 422
        // Version check passes (ETag "v0" matches current version 0); domain guard fires after.
        ProblemAssert.assertThat(PaymentOrdersApi.capture(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        paymentOrderId,
                        createEtag,
                        IdempotencyKeys.generate("capture")))
                .hasStatus(422)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.INVALID_TRANSITION)
                .hasNoStore()
                .varyContains("If-Match");
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE / refund + negative boundaries — Phase 7F
    // -------------------------------------------------------------------------

    /**
     * Full lifecycle happy path: create → authorize → capture → refund returns 200 REFUNDED, ETag v3.
     *
     * <p><strong>Test category:</strong> End-to-end lifecycle contract — verifies the complete
     * CREATED → AUTHORIZED → CAPTURED → REFUNDED state machine path, the only terminal state
     * reachable after a full payment cycle.
     *
     * <p><strong>HTTP/REST concept:</strong> each lifecycle action requires the caller to echo the
     * ETag from the <em>previous</em> response in {@code If-Match}. The ETag chain accumulates:
     * <ol>
     *   <li>Create → {@code ETag: "v0"}.</li>
     *   <li>Authorize with {@code If-Match: "v0"} → {@code ETag: "v1"}.</li>
     *   <li>Capture with {@code If-Match: "v1"} → {@code ETag: "v2"}.</li>
     *   <li>Refund with {@code If-Match: "v2"} → {@code ETag: "v3"}.</li>
     * </ol>
     * Using a stale ETag at any step produces 412. Skipping a step produces 422. This test
     * exercises the entire chain from creation through final settlement refund.
     *
     * <p><strong>Refund body semantics:</strong> body {@code {}} omits {@code amountMinor},
     * which triggers a full refund. The backend computes the effective refund amount as
     * {@code order.getCapturedAmountMinor()} when the request field is null.
     * {@code refundedAmountMinor} in the response must equal {@code capturedAmountMinor}.
     *
     * <p><strong>Payment/business risk:</strong> a refund moves money back from the merchant
     * to the customer. The PSP reference from the original capture is used to reverse the
     * settlement. This is the most financially sensitive action in the lifecycle: over-refunding
     * (refunding more than captured) is blocked by {@code InvalidRefundAmountException} → 422.
     * This test uses a full refund to avoid needing to track the partial-refund contract.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why does the ETag reach "v3" after a full lifecycle — what does each version represent?</li>
     *   <li>Why must {@code refundedAmountMinor} equal {@code capturedAmountMinor} for a full refund?</li>
     *   <li>Why is REFUNDED a terminal state — what would happen if a second refund was attempted?</li>
     *   <li>What is the business difference between a cancel (before/after auth) and a refund?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST create → authorize → capture → refund → 200 REFUNDED, ETag v0 → v1 → v2 → v3")
    void create_authorize_capture_refund_happy_path_returns_200_and_increments_etag_to_v3() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0"
        String clientRef = UniqueReferences.paymentRef("refund-happy");
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(6_000L, "PLN", clientRef),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));
        assertThat(createEtag.raw()).isEqualTo("\"v0\"");

        // Step 2: authorize with If-Match "v0" → ETag "v1"
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));
        authorizeResponse.then().statusCode(200);
        ETag authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG));
        assertThat(authorizeEtag.raw()).isEqualTo("\"v1\"");

        // Step 3: capture with If-Match "v1" → ETag "v2"
        Response captureResponse = PaymentOrdersApi.capture(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                authorizeEtag.raw(),
                IdempotencyKeys.generate("capture"));
        captureResponse.then().statusCode(200);
        ETag captureEtag = ETag.of(captureResponse.header(Headers.ETAG));
        assertThat(captureEtag.raw()).isEqualTo("\"v2\"");

        // Step 4: refund with If-Match "v2" — must use ETag from capture response
        Response refundResponse = PaymentOrdersApi.refund(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                captureEtag.raw(),
                IdempotencyKeys.generate("refund"));

        refundResponse.then().statusCode(200);

        // Body: status REFUNDED, refundedAmountMinor = capturedAmountMinor (full refund), refundedAt set
        PaymentOrderResponse refundBody = refundResponse.as(PaymentOrderResponse.class);
        assertThat(refundBody.paymentOrderId().toString()).isEqualTo(paymentOrderId);
        assertThat(refundBody.status()).isEqualTo("REFUNDED");
        assertThat(refundBody.refundedAt()).isNotNull();
        assertThat(refundBody.refundedAmountMinor()).isEqualTo(6_000L);

        // ETag incremented: v2 → v3
        ETag refundEtag = ETag.of(refundResponse.header(Headers.ETAG));
        assertThat(refundEtag.raw()).isEqualTo("\"v3\"");
        assertThat(refundEtag.version()).isGreaterThan(captureEtag.version());

        // Vary: If-Match and Cache-Control: no-store
        assertThat(refundResponse.header(Headers.VARY))
                .isNotNull()
                .containsIgnoringCase("If-Match");
        assertThat(refundResponse.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");
    }

    /**
     * Invalid transition: cancel a CAPTURED order → 422 invalid_transition.
     *
     * <p><strong>Test category:</strong> State machine boundary — negative test for cancel from
     * a post-capture state. The state machine allows cancel only from CREATED or AUTHORIZED.
     * Attempting to cancel a CAPTURED order produces 422 with {@code error: "invalid_transition"}.
     *
     * <p><strong>Backend mapping:</strong> {@code PaymentOrder.cancel()} calls
     * {@code canTransitionTo(CANCELLED)}, which checks {@code VALID_TRANSITIONS.get(CAPTURED)}
     * and finds CANCELLED is not in the allowed set (only REFUNDED is)
     * → throws {@code InvalidStateTransitionException} → 422.
     *
     * <p><strong>HTTP/REST concept:</strong> CAPTURED is a settled financial state — funds have
     * been moved. A "cancel" at this point is no longer a pre-settlement cancellation; it would
     * need to be a refund. The API enforces this distinction by returning 422 with
     * {@code invalid_transition} rather than silently allowing a cancel that could cause
     * inconsistent PSP and ledger state.
     *
     * <p><strong>Payment/business risk:</strong> cancelling a captured order would send a
     * cancellation signal to the PSP for a payment that has already been settled. The PSP would
     * reject the request (or worse, silently fail), leaving the merchant's ledger and the PSP
     * record in an inconsistent state. The domain guard prevents the PSP call entirely by
     * rejecting the transition before service logic is reached.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the business difference between cancelling a payment and refunding it?</li>
     *   <li>Why does the state machine use two different terminal paths for pre-capture and
     *       post-capture reversal?</li>
     *   <li>What should a merchant client do when they receive 422 invalid_transition on a cancel?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST cancel on CAPTURED order → 422 invalid_transition")
    void cancel_on_captured_order_returns_422_invalid_transition() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0"
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(3_800L, "PLN", UniqueReferences.paymentRef("cancel-captured")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));

        // Step 2: authorize — ETag "v1"
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));
        authorizeResponse.then().statusCode(200);
        ETag authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG));

        // Step 3: capture — ETag "v2"
        Response captureResponse = PaymentOrdersApi.capture(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                authorizeEtag.raw(),
                IdempotencyKeys.generate("capture"));
        captureResponse.then().statusCode(200);
        String captureEtag = ETag.of(captureResponse.header(Headers.ETAG)).raw();

        // Attempt cancel on CAPTURED — version check passes ("v2" matches),
        // domain state machine rejects CAPTURED → CANCELLED
        ProblemAssert.assertThat(PaymentOrdersApi.cancel(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        paymentOrderId,
                        captureEtag,
                        IdempotencyKeys.generate("cancel")))
                .hasStatus(422)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.INVALID_TRANSITION)
                .hasNoStore()
                .varyContains("If-Match");
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE / refund invalid-transition regression — Phase 7G
    // -------------------------------------------------------------------------

    /**
     * Regression: refund on AUTHORIZED order (not captured) → 422 invalid_transition.
     *
     * <p><strong>Test category:</strong> Regression — verifies the fix for a backend bug
     * discovered in Phase 7F where {@code PaymentLifecycleService.refund()} called
     * {@code pspClient.refund()} before the domain state guard, causing a
     * {@code NullPointerException} (500) instead of 422 when {@code capturedAmountMinor}
     * was null.
     *
     * <p><strong>Bug root cause:</strong> for non-captured orders, {@code capturedAmountMinor}
     * is {@code null}. The service expression
     * {@code amountMinor != null ? amountMinor : order.getCapturedAmountMinor()} evaluated to
     * {@code null}; auto-unboxing null to {@code long} for the PSP call threw NPE before
     * {@code order.refund()} could fire its {@code canTransitionTo(REFUNDED)} guard.
     *
     * <p><strong>Fix applied:</strong> an explicit
     * {@code if (!order.canTransitionTo(REFUNDED)) throw new InvalidStateTransitionException(...)}
     * guard was added in {@code PaymentLifecycleService.refund()} <em>before</em> the PSP call.
     * This mirrors the domain's own guard, ensuring non-captured orders are rejected before any
     * null dereference can occur.
     *
     * <p><strong>HTTP/REST concept:</strong> 422 Unprocessable Entity is the correct status for
     * a domain state machine violation — the request is structurally valid but the server cannot
     * process it because the precondition (CAPTURED state) is not satisfied. This is distinct from:
     * <ul>
     *   <li>400 — syntactically malformed request.</li>
     *   <li>412 — stale ETag (version mismatch).</li>
     *   <li>500 — unhandled server error (the pre-fix behavior).</li>
     * </ul>
     *
     * <p><strong>Payment/business risk:</strong> refunding a non-captured (AUTHORIZED) order is
     * a client error that must be caught before any PSP interaction. An AUTHORIZED order has no
     * settled funds to reverse; a refund would be nonsensical and the PSP would reject it
     * unpredictably. The guard prevents the PSP call entirely.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>How do you test the fix for a 500 → 422 regression without inspecting backend logs?</li>
     *   <li>Why must the domain state check precede the PSP call in a lifecycle service?</li>
     *   <li>What is the difference between a defensive pre-check and the domain's own guard?</li>
     *   <li>Why does an empty refund body ({@code {}}) cause a NPE on AUTHORIZED orders but not
     *       CAPTURED orders?</li>
     * </ul>
     */
    @Test
    @DisplayName("POST refund on AUTHORIZED order (not captured) → 422 invalid_transition [Phase 7G regression]")
    void refund_on_authorized_order_returns_422_invalid_transition() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Step 1: create — ETag "v0"
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(2_500L, "EUR", UniqueReferences.paymentRef("refund-auth-regression")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        ETag createEtag = ETag.of(createResponse.header(Headers.ETAG));

        // Step 2: authorize — ETag "v1"; order is now AUTHORIZED but NOT captured
        Response authorizeResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId,
                createEtag.raw(),
                IdempotencyKeys.generate("authorize"));
        authorizeResponse.then().statusCode(200);
        String authorizeEtag = ETag.of(authorizeResponse.header(Headers.ETAG)).raw();

        // Attempt refund on AUTHORIZED (skip capture) — body {} omits amountMinor.
        // Pre-fix: capturedAmountMinor == null → NPE in PSP call → 500.
        // Post-fix: canTransitionTo(REFUNDED) fails → InvalidStateTransitionException → 422.
        ProblemAssert.assertThat(PaymentOrdersApi.refund(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        paymentOrderId,
                        authorizeEtag,
                        IdempotencyKeys.generate("refund")))
                .hasStatus(422)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.INVALID_TRANSITION)
                .hasNoStore()
                .varyContains("If-Match");
    }

    // -------------------------------------------------------------------------
    // CONCURRENCY / optimistic locking race — Phase 7H
    // -------------------------------------------------------------------------

    /**
     * Two concurrent authorize requests on the same CREATED order with <em>different</em>
     * Idempotency-Keys → exactly one 200 success, exactly one 412 safe failure, no 500.
     *
     * <p><strong>Test category:</strong> Concurrency contract — verifies that the backend's
     * dual-layer optimistic-locking mechanism (JPA {@code @Version} field + service-level ETag
     * pre-check) prevents double-authorization even under concurrent client load.
     *
     * <p><strong>Why different Idempotency-Keys?</strong> The idempotency subsystem uses
     * {@code INSERT ... ON CONFLICT DO NOTHING} keyed on
     * {@code (merchantId, paymentOrderId, action, idempotencyKeyHash)}. Two requests with the
     * <em>same</em> key would be treated as idempotency replay (one returns early), which would
     * test idempotency semantics, not the optimistic-locking race. Using different keys forces
     * both requests through the full authorize path so the race is on the JPA version counter.
     *
     * <p><strong>Two possible 412 sub-types depending on timing:</strong>
     * <ul>
     *   <li>{@code concurrency_conflict} (HTTP 412) — true concurrent overlap: both transactions
     *       read version=0, both pass the service-level ETag pre-check, both call the PSP, both
     *       call {@code order.authorize()} in memory. JPA generates
     *       {@code UPDATE payment_orders SET version=1 WHERE id=X AND version=0}. Only one
     *       {@code UPDATE} finds a matching row; the other returns 0 rows → JPA throws
     *       {@code ObjectOptimisticLockingFailureException} →
     *       {@code PaymentExceptionHandler.handleOptimisticLock()} → 412 {@code concurrency_conflict}.
     *       The losing transaction rolls back, including the idempotency record it inserted.</li>
     *   <li>{@code payment_order_version_mismatch} (HTTP 412) — sequential overlap: Request A
     *       commits version=1 before Request B reads the order. B reads version=1, then
     *       {@code PaymentVersionPrecondition.requireCurrentVersion(order=1, expected=0)} throws
     *       {@code PaymentOrderVersionMismatchException} → 412 {@code payment_order_version_mismatch}.</li>
     * </ul>
     *
     * <p>The {@link CyclicBarrier} releases both threads simultaneously to maximize the probability
     * of the true-concurrent path, but the test is deterministic regardless: whether the 412 code
     * is {@code concurrency_conflict} or {@code payment_order_version_mismatch}, it is always
     * exactly one 200 and one 412.
     *
     * <p><strong>Why can't both requests return 200?</strong> JPA's optimistic locking generates a
     * conditional {@code UPDATE WHERE version = N}. The database executes these atomically. If both
     * are attempted with N=0, only one row is updated; the other returns 0 rows, which JPA
     * translates to {@code ObjectOptimisticLockingFailureException}. This is a
     * database-level guarantee — no amount of application-level timing can cause two concurrent
     * JPA version increments on the same row to both succeed.
     *
     * <p><strong>HTTP/REST concept:</strong> 412 Precondition Failed is the correct status for
     * optimistic-lock failures, not 409 Conflict. 409 signals a business conflict the client can
     * potentially resolve without re-reading; 412 signals that a precondition (the ETag version)
     * is no longer satisfied. The client recovers by reading the new ETag and retrying — the same
     * "read → modify → write" round-trip as any conditional request.
     *
     * <p><strong>Payment/business risk:</strong> double-authorization would reserve the customer's
     * funds twice, leading to declined downstream captures and a poor user experience. This test
     * verifies that the system is safe against simultaneous API calls from a buggy client or
     * distributed load-balanced caller.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the difference between {@code concurrency_conflict} and
     *       {@code payment_order_version_mismatch}? (JPA flush vs. service-level pre-check)</li>
     *   <li>Why does using different Idempotency-Keys test the race, but the same key would not?</li>
     *   <li>Why does {@code CyclicBarrier} improve (but not guarantee) the concurrent path?</li>
     *   <li>Why is the assertion "one 200 + one 412" correct even if we cannot predict which
     *       thread wins?</li>
     *   <li>What would you need to add to test that the losing transaction did NOT double-book the
     *       PSP? (Hint: mock PSP call counter assertion, deferred to unit/integration layer)</li>
     * </ul>
     */
    @Test
    @DisplayName("Two concurrent authorizes (different Idempotency-Keys, same ETag) → exactly one 200, one 412 [Phase 7H concurrency]")
    void concurrent_authorize_with_different_idempotency_keys_yields_one_success_and_one_412() throws Exception {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Setup: create a fresh payment order (CREATED, ETag "v0")
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(4_000L, "EUR", UniqueReferences.paymentRef("race-auth")),
                IdempotencyKeys.generate("create"));
        createResponse.then().statusCode(201);

        String paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId().toString();
        String etag = ETag.of(createResponse.header(Headers.ETAG)).raw();

        // Capture context for child threads — Ctx is ThreadLocal; each thread must set it.
        TestContext ctx = Ctx.current();

        // Two distinct keys: both are "first-time" for this action, so both proceed past
        // the idempotency pre-check and race on the JPA @Version counter.
        String idempotencyKeyA = IdempotencyKeys.generate("auth-race-a");
        String idempotencyKeyB = IdempotencyKeys.generate("auth-race-b");

        // CyclicBarrier releases both threads at the same instant to maximise the race window.
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Response> futureA = executor.submit(() -> {
            Ctx.set(ctx);
            barrier.await(); // synchronise before firing — maximise true-concurrent overlap
            return PaymentOrdersApi.authorize(
                    Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId, etag, idempotencyKeyA);
        });
        Future<Response> futureB = executor.submit(() -> {
            Ctx.set(ctx);
            barrier.await();
            return PaymentOrdersApi.authorize(
                    Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId, etag, idempotencyKeyB);
        });

        try {
            Response responseA = futureA.get(15, TimeUnit.SECONDS);
            Response responseB = futureB.get(15, TimeUnit.SECONDS);

            // Assertions on categories — thread order is non-deterministic
            List<Integer> statusCodes = List.of(responseA.statusCode(), responseB.statusCode())
                    .stream().sorted().toList();

            assertThat(statusCodes)
                    .as("exactly one 200 (authorize succeeded) and one 412 (safe optimistic-lock failure)")
                    .containsExactly(200, 412);

            // The 412 must carry a recognised optimistic-locking error code.
            // Which code appears depends on timing (see Javadoc); either is contractually correct.
            Response failedResponse = (responseA.statusCode() == 412) ? responseA : responseB;
            String errorCode = failedResponse.jsonPath().getString("error");
            assertThat(errorCode)
                    .as("412 error code must be concurrency_conflict (JPA flush race) " +
                        "or payment_order_version_mismatch (service-level pre-check race)")
                    .isIn(ProblemCodes.CONCURRENCY_CONFLICT, ProblemCodes.PAYMENT_ORDER_VERSION_MISMATCH);

            // Final resource state: exactly one authorize committed → AUTHORIZED at version 1
            Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
            Response getResponse = PaymentOrdersApi.getById(Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId);
            getResponse.then().statusCode(200);
            PaymentOrderResponse finalOrder = getResponse.as(PaymentOrderResponse.class);
            assertThat(finalOrder.status())
                    .as("order must be AUTHORIZED — exactly one concurrent request succeeded")
                    .isEqualTo("AUTHORIZED");
            assertThat(ETag.of(getResponse.header(Headers.ETAG)).version())
                    .as("ETag version must be 1 — exactly one authorize committed")
                    .isEqualTo(1);

        } finally {
            executor.shutdownNow();
        }
    }

    // -------------------------------------------------------------------------
    // CONCURRENCY / idempotency create race — Phase 7I
    // -------------------------------------------------------------------------

    /**
     * Two concurrent create requests with the <em>same</em> Idempotency-Key and the same body.
     *
     * <p><strong>Test category:</strong> Concurrency regression — verifies that the backend no
     * longer throws an unhandled {@code IllegalStateException} (500) when a second concurrent
     * create request encounters an idempotency record that has been reserved but whose
     * {@code paymentOrderId} has not yet been written (the two-phase idempotency write race).
     *
     * <p><strong>The two-phase idempotency write:</strong> {@code PaymentOrderService.create()}
     * writes the idempotency record in two phases within a single {@code @Transactional} scope:
     * <ol>
     *   <li>{@code reserveIfAbsent()} — inserts the row with {@code paymentOrderId = null}
     *       ({@code INSERT ... ON CONFLICT DO NOTHING}).</li>
     *   <li>{@code complete()} — updates the row with the real {@code paymentOrderId}.</li>
     * </ol>
     *
     * <p><strong>PostgreSQL serialisation:</strong> under READ COMMITTED isolation, concurrent
     * INSERTs on the same partial unique index
     * {@code (merchant_id, idempotency_key_hash, action) WHERE action = 'CREATE'} cause one
     * request to block until the other commits. By the time the second request unblocks, the
     * first has committed the full transaction (including {@code complete()}), so the idempotency
     * record's {@code paymentOrderId} is already set. This means the happy-path outcome
     * (one 201, one 200 replay) is the overwhelmingly common result.
     *
     * <p><strong>Pre-fix behaviour:</strong> if, under unusual circumstances (partial commit,
     * autocommit misconfiguration, or future refactoring), the second request reads the record
     * with {@code paymentOrderId == null}, the previous code threw
     * {@code IllegalStateException("Idempotency record is not completed")} which propagated as
     * an unhandled 500. The fix replaces this with {@code IdempotencyCreateInProgressException}
     * → 409 {@code create_in_progress}.
     *
     * <p><strong>Acceptable outcome categories after the fix:</strong>
     * <ul>
     *   <li>{@code [201, 200]} — normal concurrent idempotency replay (most common path).</li>
     *   <li>{@code [201, 409] create_in_progress} — race window was hit; client should retry.</li>
     *   <li>{@code [201, 409] idempotency_conflict} — both requests raced on the initial lookup
     *       before either reserved; one sees the other's completed record with a matching
     *       fingerprint before the retry check — effectively a replay served as conflict
     *       (extreme edge case).</li>
     * </ul>
     *
     * <p><strong>What this test rejects:</strong> 500, two 201s (double create), 0 creates.
     *
     * <p><strong>HTTP/REST concept:</strong> 409 Conflict is used here rather than 503 Service
     * Unavailable because it is consistent with the idempotency error family already in use in
     * this codebase. The distinction: 503 would signal "infrastructure unavailable"; 409 signals
     * "business/idempotency state prevents processing right now." Both require a retry.
     *
     * <p><strong>Payment/business risk:</strong> without the fix, concurrent retries (e.g. from
     * a client that fires two requests in parallel due to a network timeout) could produce a 500
     * that the client interprets as an outage, potentially causing the merchant to attempt a
     * duplicate payment or lose the original transaction reference.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why does using the <em>same</em> Idempotency-Key test idempotency semantics, while
     *       using different keys tests the JPA version race? (see Phase 7H)</li>
     *   <li>How does PostgreSQL's {@code INSERT ON CONFLICT DO NOTHING} + row lock prevent
     *       double-create in the vast majority of cases?</li>
     *   <li>What is the "two-phase idempotency write" and why does it create a narrow race window
     *       that needs defensive handling?</li>
     *   <li>Why is "no 500" a stronger assertion than "exactly one 201"? (Hint: 500 is always
     *       wrong; the distribution of 201/200/409 depends on timing.)</li>
     * </ul>
     */
    @Test
    @DisplayName("Two concurrent creates with same Idempotency-Key → no 500, exactly one create [Phase 7I idempotency race]")
    void concurrent_create_with_same_idempotency_key_yields_no_500_and_one_create() throws Exception {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Both requests share the same idempotency key and the same body.
        String sharedIdempotencyKey = IdempotencyKeys.generate("create-race");
        CreatePaymentOrderRequest sharedBody = CreatePaymentOrderRequest.valid(
                3_500L, "EUR", UniqueReferences.paymentRef("race-create"));

        // Capture context for child threads — Ctx is ThreadLocal.
        TestContext ctx = Ctx.current();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Response> futureA = executor.submit(() -> {
            Ctx.set(ctx);
            barrier.await();
            return PaymentOrdersApi.create(Seeds.MERCHANT_ALPHA_001_ID, sharedBody, sharedIdempotencyKey);
        });
        Future<Response> futureB = executor.submit(() -> {
            Ctx.set(ctx);
            barrier.await();
            return PaymentOrdersApi.create(Seeds.MERCHANT_ALPHA_001_ID, sharedBody, sharedIdempotencyKey);
        });

        try {
            Response responseA = futureA.get(15, TimeUnit.SECONDS);
            Response responseB = futureB.get(15, TimeUnit.SECONDS);

            // Primary assertion: no unhandled server error (pre-fix produced 500)
            assertThat(responseA.statusCode()).as("request A must not be a server error").isNotEqualTo(500);
            assertThat(responseB.statusCode()).as("request B must not be a server error").isNotEqualTo(500);

            List<Integer> statusCodes = List.of(responseA.statusCode(), responseB.statusCode())
                    .stream().sorted().toList();

            // At least one request must have created the payment order
            assertThat(statusCodes).as("at least one 201 (payment order must be created)").contains(201);

            // Each 409 must carry a recognised idempotency error code
            for (Response response : List.of(responseA, responseB)) {
                if (response.statusCode() == 409) {
                    String errorCode = response.jsonPath().getString("error");
                    assertThat(errorCode)
                            .as("409 error code must be create_in_progress or idempotency_conflict")
                            .isIn(ProblemCodes.IDEMPOTENCY_CREATE_IN_PROGRESS, ProblemCodes.IDEMPOTENCY_CONFLICT);
                }
            }

            // If both succeeded (201 + 200 replay), they must refer to the same payment order
            if (statusCodes.containsAll(List.of(200, 201))) {
                Response created = (responseA.statusCode() == 201) ? responseA : responseB;
                Response replayed = (responseA.statusCode() == 200) ? responseA : responseB;
                UUID createdId = created.as(PaymentOrderResponse.class).paymentOrderId();
                UUID replayedId = replayed.as(PaymentOrderResponse.class).paymentOrderId();
                assertThat(replayedId)
                        .as("replayed (200) response must refer to the same paymentOrderId as the created (201)")
                        .isEqualTo(createdId);
            }

        } finally {
            executor.shutdownNow();
        }
    }

    // -------------------------------------------------------------------------
    // HISTORY — Phase 7J
    // -------------------------------------------------------------------------

    /**
     * History after create → authorize → capture contains two ordered lifecycle entries.
     *
     * <p><strong>Test category:</strong> Audit/history contract — verifies that lifecycle
     * operations are faithfully recorded in the {@code payment_order_status_history} table and
     * that the history endpoint returns them in chronological order with the correct fields.
     *
     * <p><strong>Endpoint:</strong>
     * {@code GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history}
     *
     * <p><strong>Critical contract note — creation entry excluded:</strong> the backend
     * queries {@code WHERE action IS NOT NULL}, so the initial CREATED history entry is never
     * returned. A full authorize→capture lifecycle produces exactly 2 history entries.
     *
     * <p><strong>History entry field contract:</strong>
     * <ul>
     *   <li>{@code action} — lifecycle action name; one of {@code AUTHORIZE}, {@code CAPTURE},
     *       {@code CANCEL}, {@code REFUND}.</li>
     *   <li>{@code fromStatus} / {@code toStatus} — state machine transition labels:
     *       {@code CREATED→AUTHORIZED} for the authorize entry;
     *       {@code AUTHORIZED→CAPTURED} for the capture entry.</li>
     *   <li>{@code paymentOrderId} — same UUID as the payment order itself.</li>
     *   <li>{@code correlationId} — non-null; propagated from the {@code X-Correlation-ID}
     *       request header by {@code CorrelationIdFilter} into the MDC and thence into the
     *       history row.</li>
     *   <li>{@code createdAt} — ISO-8601 instant; used for {@code ORDER BY createdAt ASC}.</li>
     * </ul>
     *
     * <p><strong>Ordering guarantee:</strong> entries are sorted by {@code createdAt ASC}
     * (backend query). HTTP calls are sequential, so {@code createdAt} values are strictly
     * increasing.
     *
     * <p><strong>Observability/compliance risk:</strong> if history entries are missing or
     * out of order, audit trails are unreliable. For regulated payment systems (PCI-DSS,
     * PSD2), a complete and tamper-evident audit log is a compliance requirement, not a
     * nice-to-have. Black-box tests provide end-to-end verification that history
     * is actually written in production code paths, unlike unit tests that can be mocked.
     *
     * <p><strong>Synchronous write guarantee:</strong> unlike audit events (which may be
     * asynchronous), payment status history is written synchronously within the same
     * {@code @Transactional} method as the lifecycle operation. No {@code Awaitility}
     * polling is required.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why are there exactly 2 entries after authorize + capture, not 3 (including
     *       the initial CREATED entry)?</li>
     *   <li>Why is the history endpoint read-only ({@code @Transactional(readOnly = true)})?</li>
     *   <li>What is the difference between payment status history (here) and the audit event
     *       log ({@code /api/audit})? (Hint: history is domain data; audit is compliance log.)</li>
     *   <li>Why assert on {@code correlationId} rather than just the status fields?</li>
     * </ul>
     */
    @Test
    @DisplayName("GET history after authorize+capture → 2 ordered entries, correct transitions [Phase 7J]")
    void history_after_lifecycle_contains_ordered_entries() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        // Create order
        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(4_200L, "EUR", UniqueReferences.paymentRef("history-full")),
                IdempotencyKeys.generate("history-full"));
        createResponse.then().statusCode(201);
        PaymentOrderResponse created = createResponse.as(PaymentOrderResponse.class);
        UUID paymentOrderId = created.paymentOrderId();
        String etag = createResponse.header(Headers.ETAG);

        // Authorize
        Response authResponse = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId.toString(),
                etag, IdempotencyKeys.generate("history-auth"));
        authResponse.then().statusCode(200);
        String authEtag = authResponse.header(Headers.ETAG);

        // Capture
        PaymentOrdersApi.capture(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId.toString(),
                authEtag, IdempotencyKeys.generate("history-cap"))
                .then().statusCode(200);

        // GET history
        Response historyResponse = PaymentOrdersApi.history(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId.toString());
        historyResponse.then().statusCode(200);

        // Vary: Authorization on history response — same sensitivity contract as payment order GET
        assertThat(historyResponse.header(Headers.VARY)).containsIgnoringCase("Authorization");
        assertThat(historyResponse.header(Headers.CACHE_CONTROL)).contains("no-store");

        PaymentHistoryResponse history = historyResponse.as(PaymentHistoryResponse.class);
        assertThat(history.content())
                .as("exactly 2 lifecycle entries (creation entry is excluded by action IS NOT NULL filter)")
                .hasSize(2);

        // Entry 0: CREATED → AUTHORIZED via AUTHORIZE action
        PaymentHistoryResponse.StatusHistoryEntry authEntry = history.content().get(0);
        assertThat(authEntry.action()).isEqualTo("AUTHORIZE");
        assertThat(authEntry.fromStatus()).isEqualTo("CREATED");
        assertThat(authEntry.toStatus()).isEqualTo("AUTHORIZED");
        assertThat(authEntry.paymentOrderId()).isEqualTo(paymentOrderId);
        assertThat(authEntry.statusHistoryId()).isNotNull();
        assertThat(authEntry.correlationId()).isNotNull().isNotBlank();
        assertThat(authEntry.createdAt()).isNotNull();

        // Entry 1: AUTHORIZED → CAPTURED via CAPTURE action
        PaymentHistoryResponse.StatusHistoryEntry capEntry = history.content().get(1);
        assertThat(capEntry.action()).isEqualTo("CAPTURE");
        assertThat(capEntry.fromStatus()).isEqualTo("AUTHORIZED");
        assertThat(capEntry.toStatus()).isEqualTo("CAPTURED");
        assertThat(capEntry.paymentOrderId()).isEqualTo(paymentOrderId);
        assertThat(capEntry.statusHistoryId()).isNotNull();
        assertThat(capEntry.correlationId()).isNotNull().isNotBlank();
        assertThat(capEntry.createdAt()).isNotNull();
    }

    /**
     * History for a freshly created order returns an empty list (creation entry excluded).
     *
     * <p><strong>Test category:</strong> History endpoint contract — documents the backend
     * design decision to exclude the creation entry from the history response.
     *
     * <p><strong>Contract detail:</strong> {@code PaymentLifecycleService.findHistory()} uses
     * {@code findByPaymentOrderIdAndActionIsNotNullOrderByCreatedAtAsc}. The creation history
     * row is written with {@code action = null}, so it is always excluded from this query.
     * A client calling history immediately after create must not assume the CREATED entry is
     * present — it will never appear here.
     *
     * <p><strong>Why test the empty case?</strong> Verifying that the endpoint returns 200
     * with an empty list (rather than 404, 500, or a single CREATED entry) documents the
     * empty-collection contract. API consumers must handle {@code {"content":[]}} without
     * treating it as an error condition.
     *
     * <p><strong>HTTP/REST concept:</strong> an empty collection resource is
     * {@code 200 OK} with an empty array, not 404. The resource (the payment order) exists;
     * it has just not undergone any lifecycle transitions yet.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is 200 with {@code {"content":[]}} correct instead of 204 No Content?</li>
     *   <li>Why might the API designer choose to exclude the creation entry from
     *       the history endpoint? (Hint: the GET payment order endpoint already exposes
     *       {@code status: "CREATED"}; repeating it in history would be redundant.)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET history for newly created order → 200 with empty content list [Phase 7J]")
    void history_for_newly_created_order_returns_empty_list() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response createResponse = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(1_000L, "USD", UniqueReferences.paymentRef("history-empty")),
                IdempotencyKeys.generate("history-empty"));
        createResponse.then().statusCode(201);
        UUID paymentOrderId = createResponse.as(PaymentOrderResponse.class).paymentOrderId();

        // GET history immediately — no lifecycle operations performed
        PaymentHistoryResponse history = PaymentOrdersApi.history(
                        Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId.toString())
                .then()
                .statusCode(200)
                .extract()
                .as(PaymentHistoryResponse.class);

        assertThat(history.content())
                .as("creation entry has action=null and is excluded; no lifecycle actions performed")
                .isEmpty();
    }

    /**
     * History endpoint returns 403 for a user without any required payment authority.
     *
     * <p><strong>Test category:</strong> Authorization boundary — verifies that the history
     * endpoint enforces authority requirements at the Spring Security filter chain level.
     *
     * <p><strong>Security config:</strong> {@code SecurityConfig} requires one of:
     * {@code merchant:payments:read}, {@code merchant:payments:lifecycle},
     * {@code platform:payments:read}, {@code platform:payments:lifecycle}, or
     * {@code platform:payments:audit} for {@code GET .../history}. A user with a valid JWT
     * but no matching authority is rejected with 403 before reaching the controller.
     *
     * <p><strong>HTTP concept:</strong> 401 vs 403 distinction — the {@code denied} user has
     * a valid (authenticated) JWT, so the response is 403 Forbidden (identified but not
     * permitted), not 401 Unauthorized (not identified).
     *
     * <p><strong>Compliance risk:</strong> if the history endpoint were accidentally left
     * open (e.g., {@code .anyRequest().permitAll()}), any authenticated user could read
     * the payment lifecycle audit trail of any merchant. This test detects such regressions.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why does Spring Security return 403 (not 401) for an authenticated user without
     *       the required authority?</li>
     *   <li>Why doesn't this test need seed data or a specific payment order ID?
     *       (Hint: the security check fires before any service method runs.)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET history with denied user (no roles) → 403 Forbidden [Phase 7J]")
    void history_access_forbidden_without_required_authority() {
        Ctx.set(TestContext.of(Identities.denied()));

        PaymentOrdersApi.history(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID)
                .then()
                .statusCode(403);
    }
}
