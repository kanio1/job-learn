package lab.paymentquality.testsupport;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MerchantApiTestSupport {

    private MerchantApiTestSupport() {
    }

    public static RequestSpecification publicRequest(int port) {
        return RestAssured.given().port(port);
    }

    public static RequestSpecification operatorRequest(int port) {
        return requestWithToken(port, TestJwtSupport.platformOperatorToken());
    }

    public static RequestSpecification requestWithToken(int port, String token) {
        return publicRequest(port).auth().oauth2(token);
    }

    public static Map<String, Object> createMerchantBody(String reference, String displayName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantReference", reference);
        body.put("displayName", displayName);
        return Map.copyOf(body);
    }

    public static String uniqueMerchantReference(String label) {
        return "MERCH-" + label + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
