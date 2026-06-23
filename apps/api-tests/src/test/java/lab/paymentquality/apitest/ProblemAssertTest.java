package lab.paymentquality.apitest;

import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.ContentTypes;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Problem/error contract support test — Phase 4.
 *
 * <p>Verifies that {@link ProblemAssert} correctly asserts and fails on:
 * <ul>
 *   <li>HTTP status code matching</li>
 *   <li>{@code error} code field matching</li>
 *   <li>{@code correlationId} presence and consistency with response header</li>
 *   <li>{@code Cache-Control: no-store} presence</li>
 *   <li>{@code Vary} header content</li>
 * </ul>
 *
 * <p>Uses in-memory {@link ResponseBuilder} — no network, no containers.
 *
 * <p>SDET concept: testing a custom assertion class confirms the assertion <em>fails correctly</em>
 * when the backend violates the contract. A broken assertion that never fails is worse than no assertion.
 *
 * <p>REST Assured concept: {@link ResponseBuilder} allows building stub {@code Response} objects
 * in pure unit tests — useful for testing assertion logic, converters, and response wrappers.
 */
class ProblemAssertTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Response buildResponse(int status, String contentType, String body,
                                          Map<String, String> headers) {
        var builder = new ResponseBuilder()
                .setStatusCode(status)
                .setContentType(contentType)
                .setBody(body);
        headers.forEach(builder::setHeader);
        return builder.build();
    }

    private static String problemBody(String error, String correlationId) {
        return String.format(
                "{\"error\":\"%s\",\"status\":409,\"correlationId\":\"%s\",\"title\":\"Conflict\",\"detail\":\"Test error\"}",
                error, correlationId);
    }

    // ── hasStatus ────────────────────────────────────────────────────────────

    @Test
    void hasStatus_passes_whenStatusMatches() {
        Response resp = buildResponse(409, ContentTypes.PROBLEM_JSON,
                problemBody("conflict", "test-corr-001"),
                Map.of(Headers.CORRELATION_ID, "test-corr-001",
                       Headers.CACHE_CONTROL, "no-store",
                       Headers.VARY, "Authorization"));

        ProblemAssert.assertThat(resp).hasStatus(409);
    }

    @Test
    void hasStatus_fails_whenStatusDoesNotMatch() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-corr-002"),
                Map.of());

        assertThatThrownBy(() -> ProblemAssert.assertThat(resp).hasStatus(409))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("409")
                .hasMessageContaining("400");
    }

    // ── hasError ─────────────────────────────────────────────────────────────

    @Test
    void hasError_passes_whenErrorCodeMatches() {
        Response resp = buildResponse(409, ContentTypes.PROBLEM_JSON,
                problemBody(ProblemCodes.IDEMPOTENCY_CONFLICT, "test-corr-003"),
                Map.of());

        ProblemAssert.assertThat(resp).hasError(ProblemCodes.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void hasError_fails_whenErrorCodeDoesNotMatch() {
        Response resp = buildResponse(409, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-corr-004"),
                Map.of());

        assertThatThrownBy(() ->
                ProblemAssert.assertThat(resp).hasError(ProblemCodes.IDEMPOTENCY_CONFLICT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("idempotency_conflict")
                .hasMessageContaining("validation");
    }

    // ── hasCorrelationId ─────────────────────────────────────────────────────

    @Test
    void hasCorrelationId_passes_whenPresentInBody() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "corr-abc-123"), Map.of());

        ProblemAssert.assertThat(resp).hasCorrelationId();
    }

    @Test
    void hasCorrelationId_withExpected_passes_whenMatches() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-exact-001"), Map.of());

        ProblemAssert.assertThat(resp).hasCorrelationId("test-exact-001");
    }

    @Test
    void hasCorrelationId_withExpected_fails_whenDoesNotMatch() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-actual-001"), Map.of());

        assertThatThrownBy(() ->
                ProblemAssert.assertThat(resp).hasCorrelationId("test-expected-999"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("test-expected-999")
                .hasMessageContaining("test-actual-001");
    }

    // ── hasCorrelationIdConsistent ────────────────────────────────────────────

    @Test
    void hasCorrelationIdConsistent_passes_whenBodyAndHeaderMatch() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-cons-001"),
                Map.of(Headers.CORRELATION_ID, "test-cons-001"));

        ProblemAssert.assertThat(resp).hasCorrelationIdConsistent();
    }

    @Test
    void hasCorrelationIdConsistent_fails_whenBodyAndHeaderDiffer() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "test-body-id"),
                Map.of(Headers.CORRELATION_ID, "test-header-id"));

        assertThatThrownBy(() ->
                ProblemAssert.assertThat(resp).hasCorrelationIdConsistent())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("test-body-id")
                .hasMessageContaining("test-header-id");
    }

    // ── hasNoStore / varyContains ─────────────────────────────────────────────

    @Test
    void hasNoStore_passes_whenCacheControlContainsNoStore() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "corr"),
                Map.of(Headers.CACHE_CONTROL, "no-store, no-cache"));

        ProblemAssert.assertThat(resp).hasNoStore();
    }

    @Test
    void hasNoStore_fails_whenCacheControlIsMissing() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "corr"), Map.of());

        assertThatThrownBy(() -> ProblemAssert.assertThat(resp).hasNoStore())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("no-store");
    }

    @Test
    void varyContains_passes_whenVaryIncludesHeaderName_caseInsensitive() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "corr"),
                Map.of(Headers.VARY, "Authorization, Idempotency-Key"));

        ProblemAssert.assertThat(resp)
                .varyContains("authorization")
                .varyContains("Idempotency-Key");
    }

    @Test
    void varyContains_fails_whenVaryMissingExpectedHeader() {
        Response resp = buildResponse(400, ContentTypes.PROBLEM_JSON,
                problemBody("validation", "corr"),
                Map.of(Headers.VARY, "Authorization"));

        assertThatThrownBy(() -> ProblemAssert.assertThat(resp).varyContains("Idempotency-Key"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Idempotency-Key");
    }

    // ── ProblemCodes constants sanity ─────────────────────────────────────────

    @Test
    void problemCodes_knownCodes_areSnakeCaseStrings() {
        org.assertj.core.api.Assertions.assertThat(ProblemCodes.VALIDATION).isEqualTo("validation");
        org.assertj.core.api.Assertions.assertThat(ProblemCodes.IDEMPOTENCY_CONFLICT).isEqualTo("idempotency_conflict");
        org.assertj.core.api.Assertions.assertThat(ProblemCodes.PRECONDITION_FAILED).isEqualTo("precondition_failed");
        org.assertj.core.api.Assertions.assertThat(ProblemCodes.METHOD_NOT_ALLOWED).isEqualTo("method_not_allowed");
        org.assertj.core.api.Assertions.assertThat(ProblemCodes.MERCHANT_NOT_ELIGIBLE).isEqualTo("merchant_not_payment_eligible");
    }
}
