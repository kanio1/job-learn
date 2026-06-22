package lab.paymentquality.testsupport.restkit.client;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;
import lab.paymentquality.testsupport.restkit.core.ApiPaths;

public final class PaymentOrderSummaryApi {

    private final int port;

    public PaymentOrderSummaryApi(int port) {
        this.port = port;
    }

    public ValidatableResponse getSummary(String merchantId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .get(ApiPaths.paymentOrderSummary(), merchantId)
        .then();
    }

    public ValidatableResponse getSummaryWithCorrelationId(
        String merchantId,
        String token,
        String correlationId
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
            .header(ApiHeaders.X_CORRELATION_ID, correlationId)
        .when()
            .get(ApiPaths.paymentOrderSummary(), merchantId)
        .then();
    }

    public ValidatableResponse getSummaryWithIfNoneMatch(
        String merchantId,
        String token,
        String etag
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
            .header(ApiHeaders.IF_NONE_MATCH, etag)
        .when()
            .get(ApiPaths.paymentOrderSummary(), merchantId)
        .then();
    }

    public ValidatableResponse getSummaryWithAccept(
        String merchantId,
        String token,
        String acceptHeader
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .header(ApiHeaders.ACCEPT, acceptHeader)
        .when()
            .get(ApiPaths.paymentOrderSummary(), merchantId)
        .then();
    }

    public ValidatableResponse putSummary(String merchantId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .put(ApiPaths.paymentOrderSummary(), merchantId)
        .then();
    }
}
