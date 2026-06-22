package lab.paymentquality.restkit.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;

public class HeaderAssertions {


    private HeaderAssertions() {
    }

    public static void assertNoStore(Response response) {
        assertThat(response.header(ApiHeaders.CACHE_CONTROL))
            .as("Cache-Control should prevent storing sensitive payment data")
            .contains("no-store");        
    }

    public static void assertVaryContainsAuthorization(Response response) {
        String allVaryValues = String.join(", ", response.headers().getValues("Vary"));
        assertThat(allVaryValues)
        .as("Vary should contain Authorization because payment responses depend on caller identity")
        .contains(ApiHeaders.AUTHORIZATION);
    }

    public static void assertVersionEtag(Response response) {
        assertThat(response.header(ApiHeaders.ETAG))
        .as("ETag should be a version tag like \"v1\"")
        .startsWith("\"v");
    }

    public static void assertCorrelationId(Response response, String expectedCorrelationId) {
        assertThat(response.header(ApiHeaders.X_CORRELATION_ID))
        .as("X-Correlation-ID should match the request correlation id")
        .isEqualTo(expectedCorrelationId);
    }
    
    public static void assertSensitivePaymentReadHeaders(Response response) {
        assertVersionEtag(response);
        assertNoStore(response);
        assertVaryContainsAuthorization(response);
    }

    public static void assertAcceptPatchMergePatchJson(Response response) {
        assertThat(response.header(ApiHeaders.ACCEPT_PATCH))
        .as("Accept-Patch should declare JSON Merge Patch media type")
        .isEqualTo("application/merge-patch+json");
    }

    public static void assertVaryContainsIfMatch(Response response) {
        String allVaryValues = String.join(", ", response.headers().getValues("Vary"));
        assertThat(allVaryValues)
        .as("Vary should contain If-Match because lifecycle responses depend on precondition headers")
        .contains(ApiHeaders.IF_MATCH);
    }

    public static void assertSensitivePaymentMutationHeaders(Response response) {
        assertVersionEtag(response);
        assertNoStore(response);
        assertVaryContainsAuthorization(response);
        assertVaryContainsIfMatch(response);
    }

    public static void assertSensitiveResponseIsNotCacheable(Response response) {
        assertNoStore(response);
    }

    public static void assertWwwAuthenticatePresent(Response response) {
        assertThat(response.header(ApiHeaders.WWW_AUTHENTICATE))
            .as("WWW-Authenticate should describe the bearer authentication challenge")
            .isNotBlank()
            .startsWith("Bearer");
    }

    public static void assertAuthorizationTokenIsNotLeaked(Response response) {
        assertThat(response.header(ApiHeaders.AUTHORIZATION))
            .as("Authorization must never be returned as a response header")
            .isNull();
        assertThat(response.asString())
            .as("Response body must not expose authorization credentials")
            .doesNotContain(ApiHeaders.AUTHORIZATION)
            .doesNotContain("Bearer ");
    }

    public static void assertVaryContainsAccept(Response response) {
        String allVaryValues = String.join(", ", response.headers().getValues("Vary"));
        assertThat(allVaryValues)
            .as("Vary should contain Accept")
            .contains(ApiHeaders.ACCEPT);
    }

    public static void assertVaryContainsIdempotencyKey(Response response) {
        String allVaryValues = String.join(", ", response.headers().getValues("Vary"));
        assertThat(allVaryValues)
            .as("Vary should contain Idempotency-Key")
            .contains(ApiHeaders.IDEMPOTENCY_KEY);
    }

    public static void assertLocationPointsToPaymentOrder(
        Response response,
        String merchantId,
        String paymentOrderId
    ) {
        String location = response.header(ApiHeaders.LOCATION);

        assertThat(location).as("Location header should point to the created payment order resource")
            .isNotBlank().contains("/api/merchants" + merchantId + "/payment-orders/" + paymentOrderId);
    }

    public static void assertRetryAfterDelaySeconds(Response response, int expectedSeconds) {
        String retryAfter = response.header(ApiHeaders.RETRY_AFTER);
        assertThat(retryAfter)
            .as("Retry-After should tell the client how long to wait before retrying")
            .isNotBlank();
        assertThat(Integer.parseInt(retryAfter))
            .as("Retry-After should be " + expectedSeconds + " seconds")
            .isEqualTo(expectedSeconds);
    }

    public static void assertRetryAfterIsValid(Response response) {
        String retryAfter = response.header(ApiHeaders.RETRY_AFTER);
        assertThat(retryAfter)
            .as("Retry-After header should be present")
            .isNotBlank();
        assertThat(Integer.parseInt(retryAfter))
            .as("Retry-After should be a positive integer")
            .isPositive();
    }

    public static void assertLocationPointsToOperation(Response response) {
        assertThat(response.header(ApiHeaders.LOCATION))
            .as("Location header should point to an operation resource")
            .isNotBlank()
            .contains("/operations/");
    }


}
