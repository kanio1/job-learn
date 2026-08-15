package lab.paymentquality.apitest.api;

import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client for runtime {@code GET /v3/api-docs}.
 *
 * <p>This module consumes the generated document; it must not generate OpenAPI.
 */
public final class OpenApiApi {

    private OpenApiApi() {}

    public static Response apiDocs() {
        return RequestSpecs.base()
                .when()
                .get("/v3/api-docs");
    }

    public static Response apiDocsAnonymous() {
        return RequestSpecs.anonymous()
                .when()
                .get("/v3/api-docs");
    }

    public static Response swaggerUi() {
        return RequestSpecs.base()
                .when()
                .get("/swagger-ui.html");
    }
}
