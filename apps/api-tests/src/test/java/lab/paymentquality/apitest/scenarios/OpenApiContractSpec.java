package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.OpenApiApi;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 5 — black-box consumer of runtime {@code GET /v3/api-docs} ({@code REST-OPENAPI-DRIFT-01} tooling).
 *
 * <p>Does not generate OpenAPI. Does not fail on problem+json vs default error schemas,
 * relative {@code Location}, or omitted header annotations (OA-ALW-01).
 */
@ApiTest
@Tag("contract")
@Tag("schema")
@DisplayName("OpenAPI runtime document — contract")
class OpenApiContractSpec {

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    @Test
    @DisplayName("OA-EXP-01 authenticated GET /v3/api-docs → 200 OpenAPI JSON")
    void authenticated_api_docs_returns_200() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = OpenApiApi.apiDocs();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("openapi")).startsWith("3.");
        assertThat(response.jsonPath().getMap("paths")).isNotEmpty();
    }

    @Test
    @DisplayName("OA-EXP-02 anonymous GET /v3/api-docs → 401 unauthorized")
    void anonymous_api_docs_returns_401() {
        ProblemAssert.assertThat(OpenApiApi.apiDocsAnonymous())
                .hasStatus(401)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.UNAUTHORIZED);
    }

    @Test
    @DisplayName("OA-EXP-03 authenticated GET /swagger-ui.html → 404")
    void authenticated_swagger_ui_returns_404() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        assertThat(OpenApiApi.swaggerUi().statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("OA-FLT / OA-PUB paths exclude labs and include public surface")
    void document_includes_public_paths_and_excludes_labs() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Set<String> paths = pathKeys(OpenApiApi.apiDocs());

        assertThat(paths)
                .anyMatch(path -> path.equals("/api/status") || path.startsWith("/api/status"))
                .anyMatch(path -> path.startsWith("/api/merchants"))
                .anyMatch(path -> path.contains("/payment-orders"))
                .anyMatch(path -> path.contains("/evidence"))
                .anyMatch(path -> path.startsWith("/api/users"))
                .anyMatch(path -> path.startsWith("/api/audit"))
                .anyMatch(path -> path.contains("/tenants/current/settings"));

        assertThat(paths)
                .noneMatch(path -> path.startsWith("/api/checkout-lab"))
                .noneMatch(path -> path.startsWith("/api/mirror-lab"))
                .noneMatch(path -> path.startsWith("/api/rls-lab"))
                .noneMatch(path -> path.startsWith("/api/test"));
    }

    @Test
    @DisplayName("OA-INT-01 schemas and $ref do not leak .internal")
    void document_does_not_leak_internal_packages() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = OpenApiApi.apiDocs();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).doesNotContain(".internal");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> pathKeys(Response response) {
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> paths = response.jsonPath().getMap("paths");
        assertThat(paths).isNotNull();
        return paths.keySet();
    }
}
