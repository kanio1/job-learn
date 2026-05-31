package lab.paymentquality.testsupport;

import io.restassured.specification.RequestSpecification;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PaymentApiTestSupport {

    private PaymentApiTestSupport() {
    }

    public static String createActiveMerchant(int port, RequestSpecification operatorRequest) {
        String ref = uniqueMerchantReference("PAY");
        Map<String, Object> body = MerchantApiTestSupport.createMerchantBody(ref, "Payment Test Merchant");

        String merchantId = operatorRequest
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract().path("merchantId");

        operatorRequest
                .when()
                .post("/api/merchants/" + merchantId + "/activate")
                .then()
                .statusCode(200);

        return merchantId;
    }

    public static String createMerchantActive(int port, RequestSpecification operatorRequest) {
        String ref = uniqueMerchantReference("PAY");
        Map<String, Object> body = MerchantApiTestSupport.createMerchantBody(ref, "Payment Test Merchant");

        String merchantId = operatorRequest
            .contentType("application/json")
            .body(body)
            .when()
            .post("/api/merchants")
            .then()
            .statusCode(201)
            .extract().path("merchantId");

        operatorRequest
            .when()
            .post("/api/merchants/" + merchantId + "/activate")
            .then()
            .statusCode(200);
        
        return merchantId;
    }

    public static Map<String, Object> createPaymentOrderBody(long amountMinor, String currency,
                                                              String clientOrderReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amountMinor", amountMinor);
        body.put("currency", currency);
        body.put("clientOrderReference", clientOrderReference);
        return Map.copyOf(body);
    }

    public static String uniquePaymentReference(String label) {
        return "PAY-" + label + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String uniqueIdempotencyKey(String label) {
        return "idem-" + label + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueMerchantReference(String label) {
        return "MERCH-" + label + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
