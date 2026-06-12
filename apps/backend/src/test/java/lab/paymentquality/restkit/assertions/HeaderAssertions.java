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
        assertThat(response.header(ApiHeaders.VARY))
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
        assertThat(response.header(ApiHeaders.VARY))
        .as("Vary should contain If-Match because lifecycle responses depend on precondition headers")
        .contains(ApiHeaders.IF_MATCH);
    }

    public static void assertSensitivePaymentMutationHeaders(Response response) {
        assertVersionEtag(response);
        assertNoStore(response);
        assertVaryContainsAuthorization(response);
        assertVaryContainsIfMatch(response);
    }


}
