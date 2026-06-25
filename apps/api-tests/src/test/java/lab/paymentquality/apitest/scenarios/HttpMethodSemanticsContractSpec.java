package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8G — HTTP method semantics and content-negotiation contract for
 * {@code /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}.
 *
 * <p>Covers four distinct HTTP-level contracts that are distinct from the business lifecycle
 * contracts covered in earlier phases:
 * <ol>
 *   <li>HEAD returns 200 with ETag, caching headers, and no body</li>
 *   <li>OPTIONS returns 204 with Allow and Accept-Patch headers (no authentication required)</li>
 *   <li>Unsupported method (DELETE) returns 405 with an Allow header listing valid methods</li>
 *   <li>Unacceptable Accept header (text/xml on GET) returns 406 not_acceptable</li>
 * </ol>
 *
 * <p><strong>Backend implementation notes:</strong>
 * <ul>
 *   <li>HEAD and OPTIONS are explicit handler methods in {@code PaymentOrderController} —
 *       not Spring's auto-generated defaults.</li>
 *   <li>HEAD mirrors GET headers but suppresses the body, as required by RFC 9110 §9.3.2.</li>
 *   <li>OPTIONS is {@code permitAll()} in {@code SecurityConfig} — no token needed.</li>
 *   <li>405 and 406 are mapped by {@code PaymentExceptionHandler} as
 *       {@code application/problem+json} with consistent caching headers.</li>
 * </ul>
 *
 * <p><strong>Why these four scenarios matter for SDET practice:</strong>
 * <ul>
 *   <li><em>HEAD</em>: client libraries use HEAD for cache validation; a broken HEAD contract
 *       causes unnecessary full GETs or stale-cache reads.</li>
 *   <li><em>OPTIONS</em>: CORS preflight and API capability discovery depend on it; clients
 *       that receive an unexpected Allow list may not attempt certain operations.</li>
 *   <li><em>405 with Allow</em>: RFC 9110 §15.5.6 mandates the Allow header on 405; missing
 *       it breaks client-side retry logic and automated API discovery.</li>
 *   <li><em>406</em>: Accept-header negotiation failures reveal that the server cannot serve
 *       what the client requested — critical for multi-format APIs or misconfigured clients.</li>
 * </ul>
 *
 * <p>Tests 1, 3, and 4 use {@link Identities#merchantReader} (has {@code platform:payments:read}
 * — bypasses merchant ownership check). Test 2 requires no authentication.
 */
@Tag("http")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpMethodSemanticsContractSpec {

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
     * HEAD on an existing payment order returns 200, ETag, caching headers, and no body.
     *
     * <p>HEAD is semantically identical to GET for response headers but must omit the response
     * body (RFC 9110 §9.3.2). The backend's explicit HEAD handler calls the same
     * {@code findReadablePaymentOrder()} logic as GET and returns an identical header set via
     * {@code PaymentHttpHeaders.sensitivePaymentResponse()} plus the {@code ETag}.
     *
     * <p>The seeded {@link Seeds#PAYMENT_ORDER_ALPHA_001_CREATED_ID} order is at version 0
     * (no state transitions applied), so {@code ETag: "v0"}.
     *
     * <p><strong>HTTP/REST concept:</strong> HEAD is used by HTTP caches, CDNs, and client
     * libraries to check whether a cached resource is stale: if the ETag from HEAD differs from
     * the cached ETag, the client issues a full GET. If they match, the cache is fresh.
     * Correctness of ETag on HEAD is therefore critical for cache correctness.
     *
     * <p><strong>Client/proxy risk:</strong> if the backend returned a different ETag on HEAD
     * than on GET, caches would make incorrect freshness decisions. If the backend returned a
     * body on HEAD, HTTP/1.1 clients would reject the response as malformed.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why must HEAD return the same headers as GET for the same resource?</li>
     *   <li>What does REST Assured return in {@code response.body().asString()} for a HEAD request?</li>
     *   <li>Why does HEAD require auth here when OPTIONS does not?</li>
     * </ul>
     */
    @Test
    @Order(1)
    void head_existing_payment_order_returns_200_with_etag_and_no_body() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.headById(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        assertThat(response.statusCode()).as("HEAD returns 200 OK").isEqualTo(200);

        // ETag: seeded CREATED order is at version 0 → "v0"
        assertThat(response.header(Headers.ETAG)).as("ETag from HEAD matches GET ETag").isEqualTo("\"v0\"");

        // Caching contract headers
        assertThat(response.header(Headers.VARY))
                .as("Vary: Authorization on HEAD (same as GET)")
                .containsIgnoringCase("Authorization");
        assertThat(response.header(Headers.CACHE_CONTROL))
                .as("Cache-Control: no-store on HEAD")
                .containsIgnoringCase("no-store");
        assertThat(response.header(Headers.CORRELATION_ID))
                .as("X-Correlation-ID is present")
                .isNotBlank();

        // Body must be absent — REST Assured returns empty string for HEAD responses
        assertThat(response.body().asString())
                .as("HEAD response body must be empty")
                .isEmpty();
    }

    /**
     * OPTIONS on a payment order resource returns 204 with Allow and Accept-Patch headers.
     *
     * <p>{@code OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}} is handled
     * by an explicit {@code @RequestMapping(method = OPTIONS)} method in
     * {@code PaymentOrderController}. It returns:
     * <ul>
     *   <li>{@code 204 No Content} (RFC 9110 §9.3.7 — OPTIONS response has no body)</li>
     *   <li>{@code Allow: GET, HEAD, PATCH, OPTIONS} — the four supported methods on this resource</li>
     *   <li>{@code Accept-Patch: application/merge-patch+json} — advertises the required
     *       content-type for {@code PATCH} (RFC 5789 §3.1)</li>
     *   <li>{@code X-Correlation-ID} — even OPTIONS gets a correlation ID</li>
     * </ul>
     *
     * <p><strong>No authentication required.</strong> {@code SecurityConfig} permits all
     * {@code OPTIONS} requests on {@code /api/**} via {@code .permitAll()} to support CORS
     * preflight and unauthenticated API capability discovery. This test deliberately omits
     * {@code Ctx.set()} — using {@link lab.paymentquality.apitest.core.http.RequestSpecs#anonymous()}.
     *
     * <p><strong>HTTP/REST concept:</strong> OPTIONS is the HTTP mechanism for capability
     * discovery. A client that does not know which methods or patch content-types a resource
     * supports can issue OPTIONS to find out without causing any state change.
     *
     * <p><strong>Client/proxy risk:</strong> CORS preflight for browsers requires OPTIONS to
     * return the correct {@code Allow} (or {@code Access-Control-Allow-Methods}) values.
     * If the backend's OPTIONS handler lists wrong methods, CORS-enabled clients will be blocked.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why is OPTIONS {@code permitAll()} while HEAD requires auth?</li>
     *   <li>What is {@code Accept-Patch} and when should it appear?</li>
     *   <li>What is the difference between a CORS preflight OPTIONS and a plain OPTIONS request?</li>
     * </ul>
     */
    @Test
    @Order(2)
    void options_payment_order_resource_returns_204_with_allow_and_accept_patch() {
        // No Ctx.set() — OPTIONS is permitAll(); anonymous() is used internally
        Response response = PaymentOrdersApi.optionsById(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        assertThat(response.statusCode()).as("OPTIONS returns 204 No Content").isEqualTo(204);

        // Allow header: must list the four supported methods for this resource
        String allow = response.header(Headers.ALLOW);
        assertThat(allow).as("Allow header is present").isNotBlank();
        assertThat(allow).as("Allow contains GET").containsIgnoringCase("GET");
        assertThat(allow).as("Allow contains HEAD").containsIgnoringCase("HEAD");
        assertThat(allow).as("Allow contains PATCH").containsIgnoringCase("PATCH");
        assertThat(allow).as("Allow contains OPTIONS").containsIgnoringCase("OPTIONS");

        // Accept-Patch: advertises the merge-patch content-type for PATCH operations
        assertThat(response.header(Headers.ACCEPT_PATCH))
                .as("Accept-Patch advertises merge-patch content-type")
                .isEqualTo("application/merge-patch+json");

        // Correlation ID is present even for OPTIONS
        assertThat(response.header(Headers.CORRELATION_ID))
                .as("X-Correlation-ID present on OPTIONS")
                .isNotBlank();

        // 204 has no body by definition
        assertThat(response.body().asString())
                .as("OPTIONS response body is empty")
                .isEmpty();
    }

    /**
     * Sending an unsupported HTTP method (DELETE) returns 405 with Allow header.
     *
     * <p>DELETE is not a mapped method for {@code /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}.
     * Spring MVC throws {@code HttpRequestMethodNotSupportedException}, which
     * {@code PaymentExceptionHandler.handleMethodNotSupported()} maps to:
     * <ul>
     *   <li>405 Method Not Allowed</li>
     *   <li>{@code error: "method_not_allowed"} in the problem body</li>
     *   <li>{@code Allow} header populated from {@code ex.getSupportedHttpMethods()}
     *       — the methods Spring knows are registered for this URL pattern</li>
     * </ul>
     *
     * <p>Authentication is required: the security filter's {@code .anyRequest().authenticated()}
     * rule applies to unrecognised methods. Without a valid JWT, the filter returns 401 before
     * the 405 can fire. This test uses {@link Identities#merchantReader} to pass the filter.
     *
     * <p><strong>HTTP/REST contract:</strong> RFC 9110 §15.5.6 mandates that a 405 response
     * MUST include an {@code Allow} header. Without it, clients cannot know what to retry.
     *
     * <p><strong>Client/proxy risk:</strong> a proxy or SDK that receives 405 without {@code Allow}
     * cannot automatically suggest or retry with the correct method. Missing {@code Allow} is a
     * common API bug that breaks API gateways and generated client code.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why must the 405 response include an Allow header?</li>
     *   <li>Why does the auth filter fire before the 405 for unsupported methods?</li>
     *   <li>How does Spring MVC know which methods are "allowed" for a given URL?</li>
     * </ul>
     */
    @Test
    @Order(3)
    void unsupported_method_delete_returns_405_with_allow_header() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.deleteById(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        ProblemAssert.assertThat(response)
                .hasStatus(405)
                .hasError(ProblemCodes.METHOD_NOT_ALLOWED)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore();

        // Allow header is mandatory on 405 (RFC 9110 §15.5.6)
        String allow = response.header(Headers.ALLOW);
        assertThat(allow).as("Allow header is mandatory on 405").isNotBlank();
        assertThat(allow).as("Allow lists GET (supported on this resource)").containsIgnoringCase("GET");
        assertThat(allow).as("Allow lists PATCH (supported on this resource)").containsIgnoringCase("PATCH");
        assertThat(allow).as("Allow lists HEAD (supported on this resource)").containsIgnoringCase("HEAD");
    }

    /**
     * Sending {@code Accept: text/xml} on GET returns 406 not_acceptable.
     *
     * <p>The GET handler declares {@code produces = "application/json"}.
     * When a client's {@code Accept} header does not allow {@code application/json},
     * Spring MVC throws {@code HttpMediaTypeNotAcceptableException} →
     * {@code PaymentExceptionHandler.handleHttpMediaTypeNotAcceptable()} →
     * 406 {@code not_acceptable}.
     *
     * <p>The 406 error body is returned as {@code application/problem+json} regardless of the
     * client's {@code Accept} header. Spring MVC exception handlers bypass content-type
     * negotiation when writing an error response — the error itself overrides the constraint.
     *
     * <p><strong>HTTP/REST concept:</strong> Accept negotiation is the HTTP mechanism by which
     * clients state which content-types they can consume. A 406 means no common ground exists
     * between what the client wants and what the server can produce.
     *
     * <p><strong>Client/proxy risk:</strong> a misconfigured API gateway that adds
     * {@code Accept: text/xml} to every request will cause all GET calls to fail with 406.
     * Verifying the 406 path confirms the backend's content negotiation is active and working.
     *
     * <p><strong>SDET learning — {@code .accept()} vs {@code .contentType()} in REST Assured:</strong>
     * {@code .accept("text/xml")} sets the {@code Accept} header (what the client can receive).
     * {@code .contentType("application/json")} sets {@code Content-Type} (what the client is sending).
     * These are distinct headers with distinct roles; confusing them is a common test-authoring mistake.
     *
     * <p><strong>SDET interview topic:</strong>
     * <ul>
     *   <li>Why does the 406 response body use {@code application/problem+json}
     *       even when the client sent {@code Accept: text/xml}?</li>
     *   <li>What HTTP header does {@code .accept()} in REST Assured map to?</li>
     *   <li>What is the difference between a 406 and a 415 status code?</li>
     * </ul>
     */
    @Test
    @Order(4)
    void get_with_unacceptable_accept_header_returns_406_not_acceptable() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.getByIdWithAccept(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                "text/xml");

        ProblemAssert.assertThat(response)
                .hasStatus(406)
                .hasError(ProblemCodes.NOT_ACCEPTABLE)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore();
    }
}
