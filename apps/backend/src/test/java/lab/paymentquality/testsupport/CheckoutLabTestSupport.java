package lab.paymentquality.testsupport;

import io.restassured.RestAssured;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

public final class CheckoutLabTestSupport {

    private static final String CLIENT_ID = "checkout-lab-merchant";
    private static final String CLIENT_SECRET = "test-oauth-secret";
    public static final String HMAC_SECRET = "test-hmac-secret";

    private CheckoutLabTestSupport() {
    }

    public static String obtainLabAccessToken(int port) {
        return RestAssured.given()
                .port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .when()
                .post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");
    }

    public static String createSession(int port, String labToken, String extOrderId, String notifyUrl) {
        return RestAssured.given()
                .port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + labToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "%s",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return?status=success",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(extOrderId, notifyUrl))
                .when()
                .post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .extract()
                .header("Location");
    }

    public static String sessionIdFromLocation(String location) {
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public static String obtainSimulateToken(int port, String sessionId) {
        return RestAssured.given()
                .port(port)
                .when()
                .get("/api/checkout-lab/hosted/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .extract()
                .path("simulateToken");
    }

    public static void simulateCompleted(int port, String sessionId) {
        String simulateToken = obtainSimulateToken(port, sessionId);
        RestAssured.given()
                .port(port)
                .header("Lab-Simulate-Token", simulateToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"outcome\":\"COMPLETED\"}")
                .when()
                .post("/api/checkout-lab/hosted/sessions/" + sessionId + "/simulate")
                .then()
                .statusCode(200);
    }

    public static String sign(byte[] rawBody) {
        long timestamp = Instant.now().getEpochSecond();
        byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
        byte[] signed = new byte[prefix.length + rawBody.length];
        System.arraycopy(prefix, 0, signed, 0, prefix.length);
        System.arraycopy(rawBody, 0, signed, prefix.length, rawBody.length);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String digest = HexFormat.of().formatHex(mac.doFinal(signed)).toLowerCase(Locale.ROOT);
            return "t=" + timestamp + ",v1=" + digest;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
