package lab.paymentquality.testsupport;

import io.restassured.RestAssured;

public final class RestAssuredLoggingConfig {

    private RestAssuredLoggingConfig() {
    }

    public static void configureFailureOnlyLogging() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
