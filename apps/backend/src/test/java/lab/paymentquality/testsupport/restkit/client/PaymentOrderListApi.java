package lab.paymentquality.testsupport.restkit.client;

import static io.restassured.RestAssured.given;

import java.util.Map;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testsupport.restkit.core.ApiPaths;

public class PaymentOrderListApi {

    private final int port;

    public PaymentOrderListApi(int port) {
        this.port = port;
    }

    public ValidatableResponse listOrders(String merchantId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .get(ApiPaths.paymentOrders(), merchantId)
        .then();
    }

    public ValidatableResponse listOrdersWithQuery(String merchantId,
            String token,
            Map<String, Object> queryParams) {
                return given()
                    .port(port)
                    .auth().oauth2(token)
                    .accept(ContentType.JSON)
                    .queryParams(queryParams)
                .when()
                    .get(ApiPaths.paymentOrders(), merchantId)
                .then();
            }

}
