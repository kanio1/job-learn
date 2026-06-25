package lab.paymentquality.apitest.scenarios;

import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.merchant.MerchantsApi;
import lab.paymentquality.apitest.api.merchant.dto.MerchantListResponse;
import lab.paymentquality.apitest.api.merchant.dto.MerchantResponse;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tenant / merchant isolation security contract — Phase 8C.
 *
 * <p><strong>Test category:</strong> Security matrix / BOLA-BFLA isolation — verifies that
 * merchant-scoped callers cannot read or list another merchant's payment orders, and that
 * platform-scoped callers can cross merchant boundaries as designed.
 *
 * <p><strong>Why API-level and not unit/MockMvc?</strong>
 * <ul>
 *   <li>Isolation depends on the JWT {@code merchant_id} claim being read from a real token
 *       and compared against the URL path UUID inside the Spring MVC controller. Unit tests
 *       mock the JWT and bypass the real converter chain.</li>
 *   <li>The difference between 403 ({@code AccessDeniedException}) and 404 (masked via
 *       {@code PaymentOrderNotFoundException}) on different endpoints is an HTTP-layer contract
 *       that only a black-box test can verify reliably.</li>
 *   <li>The platform bypass ({@code platform:payments:read}) that enables cross-merchant reads
 *       is enforced in the controller and depends on the authority actually being present in
 *       the decoded JWT — something only a live Keycloak token can prove.</li>
 * </ul>
 *
 * <p><strong>Isolation contract (discovered from backend code):</strong>
 * <pre>
 * Endpoint                            merchant-scoped cross-merchant    platform:payments:read
 * GET /payment-orders/{id}            404  (masked, BOLA defense)       200 (bypass)
 * GET /payment-orders                 403  (AccessDeniedException)       200 (bypass)
 * GET /payment-orders/{id}/history    403  (AccessDeniedException)       200 (bypass)
 * GET /payment-orders/summary         403  (AccessDeniedException)       200 (bypass)
 * POST /payment-orders                403  (AccessDeniedException)       N/A (lifecycle role)
 * POST /payment-orders/{id}/authorize 403  (AccessDeniedException)       N/A (lifecycle role)
 * </pre>
 *
 * <p>The 404 masking on GET-by-ID is a deliberate BOLA (Broken Object-Level Authorization)
 * defence: an unauthorized caller cannot distinguish "this order belongs to another merchant"
 * from "this order does not exist" — both produce 404. The list/summary/history endpoints
 * return 403 instead of 404 because there is no resource identifier to mask at the collection
 * or aggregate level.
 *
 * <p><strong>Identities used:</strong>
 * <ul>
 *   <li>{@link Identities#seededMerchantCreator()} — JWT {@code merchant_id=MERCHANT_ALPHA_001_ID};
 *       roles: {@code merchant:payments:create}, {@code merchant:payments:read},
 *       {@code merchant:payments:lifecycle}. Keycloak user: {@code merchant.alpha.creator}.</li>
 *   <li>{@link Identities#merchantReader(String)} — JWT has {@code platform:payments:read};
 *       no {@code merchant_id} claim. Keycloak user: {@code platform.payment.reader}.</li>
 * </ul>
 *
 * <p><strong>Data isolation:</strong> uses MERCHANT_ALPHA_002's seeded payment orders
 * ({@link Seeds#PAYMENT_ORDER_ALPHA_002_CANCELLED_ID}) as the "other merchant" resource.
 * All tests are read-only; exact seed state is guaranteed by the class-level {@code @BeforeAll}.
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>What is BOLA (Broken Object-Level Authorization) and how does it differ from BFLA
 *       (Broken Function-Level Authorization)?</li>
 *   <li>Why does GET-by-ID return 404 (not 403) for a cross-merchant caller? What information
 *       would 403 leak that 404 does not?</li>
 *   <li>Why does GET-list return 403 instead of an empty list? What is the risk of returning
 *       an empty list when the real reason is authorization?</li>
 *   <li>Why must platform bypass ({@code platform:payments:read}) be tested in a live black-box
 *       test rather than only in a unit test?</li>
 *   <li>What security pattern is being applied when a missing resource and an unauthorized
 *       resource both return the same 404 response?
 *       (Answer: existence masking / information hiding.)</li>
 * </ul>
 */
@ApiTest
@Tag("security")
@DisplayName("Tenant / merchant isolation — security contract")
class TenantIsolationContractSpec {

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
    // BOLA masking — GET by ID returns 404, not 403
    // -------------------------------------------------------------------------

    /**
     * Merchant-scoped caller reads another merchant's payment order → 404 (masked).
     *
     * <p><strong>Test category:</strong> BOLA (Broken Object-Level Authorization) regression —
     * verifies that the controller masks an unauthorized cross-merchant read as 404, not 403.
     *
     * <p><strong>HTTP/REST concept:</strong> two authorization outcomes are possible for a GET
     * on a specific payment order URL:
     * <ul>
     *   <li>403 would reveal that a resource EXISTS at this URL but the caller is not permitted
     *       to read it. A malicious actor could enumerate other merchants' payment order IDs
     *       by probing URLs and observing 403 vs. 404.</li>
     *   <li>404 (masking) hides this information entirely. The caller cannot distinguish
     *       "the order belongs to a different merchant" from "the order does not exist."</li>
     * </ul>
     * The backend implements masking by throwing {@code PaymentOrderNotFoundException} (not
     * {@code AccessDeniedException}) from the controller when the JWT {@code merchant_id} claim
     * does not match the path UUID. This maps to 404 {@code not_found} via
     * {@code PaymentExceptionHandler.handleNotFound}.
     *
     * <p><strong>Authorization path (from backend source):</strong>
     * <ol>
     *   <li>Spring SecurityFilterChain: requires {@code merchant:payments:read} OR
     *       {@code platform:payments:read}. {@code seededMerchantCreator} has
     *       {@code merchant:payments:read} → filter passes.</li>
     *   <li>Controller: {@code isPlatformReader=false}; reads JWT {@code merchant_id} claim
     *       ({@code MERCHANT_ALPHA_001_ID}); compares with path {@code merchantId}
     *       ({@code MERCHANT_ALPHA_002_ID}); mismatch → throws
     *       {@code PaymentOrderNotFoundException(paymentOrderId)} → 404.</li>
     * </ol>
     *
     * <p><strong>BOLA risk:</strong> if the controller threw {@code AccessDeniedException}
     * instead (403), an attacker who obtained one merchant's token could enumerate UUIDs
     * across the system by observing 403 (order exists, different merchant) vs. 404
     * (order does not exist). The 404 masking eliminates this timing/information oracle.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is 404 preferable to 403 for cross-object isolation on identified resources?</li>
     *   <li>What is the OWASP API Security risk category that 404 masking mitigates?
     *       (Answer: API1:2023 BOLA.)</li>
     *   <li>How would you test BOLA masking without knowing any cross-merchant payment order IDs?
     *       (Hint: use deterministic seed UUIDs.)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET payment order for other merchant → 404 masked (BOLA defence) [Phase 8C]")
    void merchant_scoped_get_payment_order_for_other_merchant_returns_404() {
        // seededMerchantCreator has merchant_id=MERCHANT_ALPHA_001_ID.
        // Requesting MERCHANT_ALPHA_002's CANCELLED payment order triggers masking.
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(
                        PaymentOrdersApi.getById(
                                Seeds.MERCHANT_ALPHA_002_ID,
                                Seeds.PAYMENT_ORDER_ALPHA_002_CANCELLED_ID))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND)
                .hasNoStore();
    }

    // -------------------------------------------------------------------------
    // Collection isolation — GET list returns 403, not empty list or 404
    // -------------------------------------------------------------------------

    /**
     * Merchant-scoped caller lists another merchant's payment orders → 403 (BFLA boundary).
     *
     * <p><strong>Test category:</strong> BFLA (Broken Function-Level Authorization) /
     * collection isolation — verifies that a GET on the payment order collection of a
     * merchant the caller does not own returns 403, not an empty list.
     *
     * <p><strong>HTTP/REST concept:</strong> collection endpoints (GET list) respond differently
     * to cross-merchant access than single-resource endpoints (GET by ID):
     * <ul>
     *   <li>Single-resource (GET /{id}): 404 masking hides existence of the specific order.</li>
     *   <li>Collection (GET ): 403 explicitly denies access. An empty list would be
     *       misleading — it implies the caller is authorized but the merchant has no orders.
     *       403 is semantically correct: the caller is not permitted to enumerate this
     *       merchant's collection at all.</li>
     * </ul>
     * The controller throws {@code AccessDeniedException} (not {@code PaymentOrderNotFoundException})
     * when the JWT {@code merchant_id} claim does not match the path UUID for a list request.
     *
     * <p><strong>Backend mechanism:</strong> {@code PaymentOrderController.listPaymentOrders}
     * checks {@code isPlatformReader}; if false, verifies {@code jwtMerchantId == merchantId};
     * mismatch throws {@code AccessDeniedException("Merchant scope mismatch")} →
     * {@code PaymentExceptionHandler.handleAccessDenied} → 403 {@code forbidden}.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why does the collection endpoint return 403 rather than an empty list?
     *       (Hint: an empty list implies authorization succeeded with no results.)</li>
     *   <li>Why is the same caller getting 403 on list but 404 on GET-by-ID?
     *       (Hint: masking only makes sense when there is a specific ID to mask.)</li>
     *   <li>What is the business risk if a cross-merchant caller received 200 with an empty
     *       list instead of 403?</li>
     * </ul>
     */
    @Test
    @DisplayName("GET payment order list for other merchant → 403 forbidden [Phase 8C]")
    void merchant_scoped_list_payment_orders_for_other_merchant_returns_403() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(
                        PaymentOrdersApi.list(Seeds.MERCHANT_ALPHA_002_ID))
                .hasStatus(403)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.FORBIDDEN)
                .hasNoStore()
                .varyContains("Authorization");
    }

    // -------------------------------------------------------------------------
    // History isolation — GET history returns 403 across merchant boundary
    // -------------------------------------------------------------------------

    /**
     * Merchant-scoped caller reads another merchant's payment order history → 403.
     *
     * <p><strong>Test category:</strong> Isolation — verifies that the history sub-resource
     * enforces the same merchant-scope boundary as the collection endpoint.
     *
     * <p><strong>HTTP/REST concept:</strong> sub-resources inherit the authorization boundary
     * of their parent resource. The history of a payment order is scoped to the merchant who
     * owns the parent order. A caller without {@code platform:payments:read} and with a
     * mismatched JWT {@code merchant_id} claim must be denied access to any sub-resource of
     * that payment order, not just the root order itself.
     *
     * <p><strong>Controller behaviour:</strong> {@code getHistory} checks for any of
     * {@code platform:payments:read}, {@code platform:payments:lifecycle}, or
     * {@code platform:payments:audit}. {@code seededMerchantCreator} has none of these.
     * Fallback to merchant-scoped check: {@code merchant_id} claim ({@code MERCHANT_ALPHA_001_ID})
     * ≠ path {@code merchantId} ({@code MERCHANT_ALPHA_002_ID}) →
     * {@code AccessDeniedException} → 403.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Should sub-resources (history, lifecycle) always enforce the same boundary as the
     *       parent resource? What could go wrong if they didn't?</li>
     *   <li>Why is the history endpoint more permissive at the authority level (also allows
     *       {@code platform:payments:audit}) but equally strict at the merchant-scope level?</li>
     * </ul>
     */
    @Test
    @DisplayName("GET payment order history for other merchant → 403 forbidden [Phase 8C]")
    void merchant_scoped_get_history_for_other_merchant_returns_403() {
        // seededMerchantCreator has merchant_id=MERCHANT_ALPHA_001_ID.
        // History for MERCHANT_ALPHA_002's cancelled order is completely inaccessible.
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(
                        PaymentOrdersApi.history(
                                Seeds.MERCHANT_ALPHA_002_ID,
                                Seeds.PAYMENT_ORDER_ALPHA_002_CANCELLED_ID))
                .hasStatus(403)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.FORBIDDEN)
                .hasNoStore()
                .varyContains("Authorization");
    }

    // -------------------------------------------------------------------------
    // Platform bypass — platform:payments:read reads across merchant boundaries
    // -------------------------------------------------------------------------

    /**
     * Platform payment reader reads a payment order from a different merchant → 200.
     *
     * <p><strong>Test category:</strong> Platform privilege — verifies that
     * {@code platform:payments:read} authority bypasses the JWT {@code merchant_id} claim
     * check and allows cross-merchant reads.
     *
     * <p><strong>HTTP/REST concept:</strong> the authority model has two distinct paths:
     * <ul>
     *   <li>{@code merchant:payments:read} — merchant-scoped; JWT must carry a {@code merchant_id}
     *       claim matching the URL. Grants read access to one merchant only.</li>
     *   <li>{@code platform:payments:read} — platform-scoped; no {@code merchant_id} claim
     *       required; bypasses the claim check in the controller. Grants read access across
     *       all merchants. Intended for platform operators, support agents, and risk systems.</li>
     * </ul>
     * The controller's {@code isPlatformReader} flag gates the claim check:
     * if true, calls {@code findForPlatform} (no merchant filter); if false, enforces
     * the {@code merchant_id} claim comparison.
     *
     * <p><strong>Scenario:</strong> {@link Identities#merchantReader(String)} maps to the
     * Keycloak user {@code platform.payment.reader} who carries only {@code platform:payments:read}
     * and has no {@code merchant_id} attribute. Reading {@code MERCHANT_ALPHA_002_ID}'s
     * cancelled payment order from a request context nominally associated with
     * {@code MERCHANT_ALPHA_001_ID} succeeds because the platform role bypasses the claim check.
     *
     * <p><strong>Business justification:</strong> platform teams need to investigate payment
     * disputes, run fraud analytics, and respond to support tickets without being restricted
     * to a single merchant's data. The authority-controlled bypass enables this while keeping
     * merchant-scoped callers isolated.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What is the security risk of making {@code platform:payments:read} too broad?
     *       (Hint: platform compromise exposes all merchants' data simultaneously.)</li>
     *   <li>Why must the platform bypass be tested explicitly and not assumed from the
     *       authority name alone?</li>
     *   <li>How would you verify that the platform reader does NOT see data it should NOT see
     *       (e.g., from a tenant it is not associated with)?
     *       (Note: this is a separate, more nuanced tenant boundary test deferred to a future
     *       phase, because the platform role is truly global in this implementation.)</li>
     * </ul>
     */
    @Test
    @DisplayName("platform:payments:read reads across merchant boundary → 200 [Phase 8C]")
    void platform_payment_reader_reads_across_merchant_boundary_returns_200() {
        // merchantReader maps to platform.payment.reader (platform:payments:read).
        // Reading MERCHANT_ALPHA_002's cancelled order bypasses the merchant_id claim check.
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_002_ID)));

        PaymentOrderResponse body = PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_002_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_002_CANCELLED_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PaymentOrderResponse.class);

        // Verify it is actually MERCHANT_ALPHA_002's seeded cancelled order
        assertThat(body.clientOrderReference())
                .as("clientOrderReference must match seeded SEED-ALPHA-002-CANCELLED")
                .isEqualTo("SEED-ALPHA-002-CANCELLED");
        assertThat(body.status())
                .as("status must be CANCELLED (seed fixture state)")
                .isEqualTo("CANCELLED");
        assertThat(body.amountMinor())
                .as("amountMinor must be 4400 PLN (seed fixture)")
                .isEqualTo(4_400L);
        assertThat(body.currency())
                .as("currency must be PLN (seed fixture)")
                .isEqualTo("PLN");
    }

    // -------------------------------------------------------------------------
    // Tenant admin boundary — merchant administration is tenant-scoped
    // -------------------------------------------------------------------------

    /**
     * Tenant admin can read and list merchants in its tenant, but cannot read a merchant
     * from another tenant.
     *
     * <p><strong>Test category:</strong> Multi-tenant authorization — verifies the tenant-scoped
     * merchant administration boundary for the real {@code tenant.admin} Keycloak persona.
     *
     * <p><strong>HTTP/REST concept:</strong> the same endpoint family has different visibility
     * depending on the resolved tenant context. {@code GET /api/merchants/{id}} returns 200 for
     * a merchant in the caller's tenant, while a merchant outside that tenant is masked as 404.
     * {@code GET /api/merchants} returns a collection filtered to the caller's tenant.
     *
     * <p><strong>Multi-tenant security risk:</strong> tenant administrators manage merchant
     * setup for one tenant. If tenant filtering is missing or applied after fetching global data,
     * a tenant admin could enumerate or manage merchants belonging to another tenant.
     *
     * <p><strong>BOLA/BFLA angle:</strong> GET-by-ID is object-level authorization (BOLA) and is
     * masked as 404 across the tenant boundary. GET-list is function/collection-level
     * authorization (BFLA) and must return only authorized tenant objects.
     *
     * <p><strong>SDET interview angle:</strong> this test proves the complete chain: Keycloak
     * user attribute {@code tenant_id=TENANT_ALPHA} → JWT claim → tenant resolution →
     * repository filtering / not-found masking. No backend classes or direct SQL are used.
     */
    @Test
    @DisplayName("tenant admin reads/lists only own-tenant merchants and cross-tenant merchant is masked [Phase 8M]")
    void tenant_admin_reads_and_lists_only_own_tenant_merchants() {
        Ctx.set(TestContext.of(Identities.tenantAdmin("TENANT_ALPHA")));

        MerchantResponse ownMerchant = MerchantsApi.getById(Seeds.MERCHANT_ALPHA_001_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(MerchantResponse.class);
        assertThat(ownMerchant.merchantId().toString()).isEqualTo(Seeds.MERCHANT_ALPHA_001_ID);
        assertThat(ownMerchant.status()).isEqualTo("ACTIVE");

        MerchantListResponse list = MerchantsApi.list()
                .then()
                .statusCode(200)
                .extract()
                .as(MerchantListResponse.class);
        Set<String> visibleMerchantIds = list.merchants().stream()
                .map(merchant -> merchant.merchantId().toString())
                .collect(Collectors.toSet());
        assertThat(visibleMerchantIds)
                .as("tenant.admin for TENANT_ALPHA must see TENANT_ALPHA merchants")
                .contains(Seeds.MERCHANT_ALPHA_001_ID, Seeds.MERCHANT_ALPHA_002_ID);
        assertThat(visibleMerchantIds)
                .as("tenant.admin for TENANT_ALPHA must not see PLATFORM_TENANT merchant")
                .doesNotContain(Seeds.MERCHANT_BETA_001_ID);

        ProblemAssert.assertThat(MerchantsApi.getById(Seeds.MERCHANT_BETA_001_ID))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND);
    }

    /**
     * Tenant admin cannot read payment orders directly, even for a merchant in its tenant,
     * because payment order reads are merchant-scoped unless the caller has platform payment
     * authority.
     *
     * <p><strong>Test category:</strong> Multi-tenant payment data boundary — verifies that a
     * tenant administration persona is not automatically a payment-order reader.
     *
     * <p><strong>HTTP/REST concept:</strong> successful authentication and a broad tenant claim
     * are not enough for object access. The payment order controller requires either
     * {@code platform:payments:read} or a JWT {@code merchant_id} claim matching the path
     * merchant. {@code tenant.admin} has neither.
     *
     * <p><strong>Multi-tenant security risk:</strong> merchant administration and payment data
     * access are separate privileges. Conflating tenant admin with payment reader would expose
     * customer payment data to operational users who only need merchant setup capabilities.
     *
     * <p><strong>BOLA/BFLA angle:</strong> this is a same-tenant BOLA guard. The target order
     * exists under TENANT_ALPHA, but the caller is not scoped to that merchant object, so the
     * backend masks the resource as 404.
     *
     * <p><strong>SDET interview angle:</strong> this catches the common multi-tenant mistake of
     * checking only {@code tenant_id}. Secure payment APIs usually require a narrower object or
     * merchant scope for payment data, plus separate platform break-glass authorities.
     */
    @Test
    @DisplayName("tenant admin without merchant_id cannot read same-tenant payment order → 404 masked [Phase 8M]")
    void tenant_admin_without_merchant_id_cannot_read_same_tenant_payment_order() {
        Ctx.set(TestContext.of(Identities.tenantAdmin("TENANT_ALPHA")));

        ProblemAssert.assertThat(PaymentOrdersApi.getById(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND)
                .hasNoStore()
                .varyContains("Authorization");
    }
}
