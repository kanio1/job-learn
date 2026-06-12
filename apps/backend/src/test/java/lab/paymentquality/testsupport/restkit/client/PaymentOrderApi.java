package lab.paymentquality.testsupport.restkit.client;

import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;
import lab.paymentquality.testsupport.restkit.core.ApiPaths;
import lab.paymentquality.testsupport.restkit.payload.CreatePaymentOrderPayload;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesRegex;

import io.restassured.http.ContentType;

public final class PaymentOrderApi {

    private final int port;
    public PaymentOrderApi(int port) {
        this.port = port;
    }

    public ValidatableResponse createOrder(
        String merchantId,
        String token,
        CreatePaymentOrderPayload payload,
        String idempotencyKey,
        String correlationId
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.IDEMPOTENCY_KEY, idempotencyKey)
            .header(ApiHeaders.X_CORRELATION_ID, correlationId)
            .body(payload.asJson())
        .when()
            .post(ApiPaths.paymentOrdersCollection(), merchantId)
        .then();
    }

    public ValidatableResponse createOrderWithBody(
        String merchantId,
        String token,
        Object body,
        String idempotencyKey,
        String correlationId
) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.IDEMPOTENCY_KEY, idempotencyKey)
            .header(ApiHeaders.X_CORRELATION_ID, correlationId)
            .body(body)
        .when()
            .post(ApiPaths.paymentOrdersCollection(), merchantId)
        .then();
    }

    public ValidatableResponse readOrder(
        String merchantId,
        String paymentOrderId,
        String token
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .get(ApiPaths.paymentOrder(), merchantId, paymentOrderId)
        .then();
    }

    public ValidatableResponse headOrder(String merchantId, String paymentOrderId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
        .when()
            .head(ApiPaths.paymentOrder(), merchantId, paymentOrderId)
        .then();
    }

    public ValidatableResponse optionsOrder(String merchantId, String paymentOrderId) {
        return given()
                .port(port)
            .when()
                .options(ApiPaths.paymentOrder(), merchantId, paymentOrderId)
            .then();
    }

    public ValidatableResponse deleteOrder(String merchantId, String paymentOrderId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
        .when()
            .delete(ApiPaths.paymentOrder(), merchantId, paymentOrderId)
        .then();
    }

}
