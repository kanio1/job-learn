package lab.paymentquality.testsupport;

import io.restassured.RestAssured;
import org.springframework.http.MediaType;

public final class CheckoutLabTestSupport {

    private static final String CLIENT_ID = "checkout-lab-merchant";
    private static final String CLIENT_SECRET = "test-oauth-secret";

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
}
