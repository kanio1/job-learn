package lab.paymentquality.testsupport;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Map;

public final class PaymentOrderListApiTestSupport {

    private PaymentOrderListApiTestSupport() {
    }

    public static void seedPaymentOrders(int port, String merchantId, String token, int count) {
        for (int i = 1; i <= count; i++) {
            String currency;
            int mod = i % 3;
            if (mod == 0) {
                currency = "USD";
            } else if (mod == 2) {
                currency = "EUR";
            } else {
                currency = "PLN";
            }
            long amount = 1000L * i;
            String ref = PaymentApiTestSupport.uniquePaymentReference("LIST");
            String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("list-seed-" + i);

            Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(amount, currency, ref);

            MerchantApiTestSupport.requestWithToken(port, token)
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .header("X-Correlation-ID", "seed-" + i)
                    .body(body)
                    .when()
                    .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                    .then()
                    .statusCode(201);
        }
    }

    public static RequestSpecification listRequestSpec(int port, String token) {
        return new RequestSpecBuilder()
                .setPort(port)
                .addHeader("Authorization", "Bearer " + token)
                .setAccept(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification successListSpec() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}
