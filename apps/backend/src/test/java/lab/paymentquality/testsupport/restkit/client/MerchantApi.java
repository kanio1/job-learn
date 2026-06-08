package lab.paymentquality.testsupport.restkit.client;

import static io.restassured.RestAssured.given;

import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testsupport.TestJwtSupport;
import lab.paymentquality.testsupport.restkit.core.ApiPaths;

public class MerchantApi {


    private final int port;

    public MerchantApi(int port){
        this.port = port;
    }

    public ValidatableResponse createMerchant(
        String token,
        String merchantReference,
        String displayName
    ) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .body(createMerchantBody(merchantReference, displayName))
        .when()
            .post(ApiPaths.merchants())
        .then();
    }

    public ValidatableResponse createMerchantAsPlatformOperator(
        String merchantReference,
        String displayName
    ) {
        return createMerchant(
            TestJwtSupport.platformOperatorToken(),
            merchantReference,
            displayName
        );
    }

    public String createMerchantAndReturnId(
        String token,
        String merchantReference,
        String displayName
    ) {
        return createMerchant(token, merchantReference, displayName)
                .statusCode(201)
                .extract()
                .path("merchantId"
            );
    }

    public String createActiveMerchantAndReturnId(String scenario) {
        String merchantReference = uniqueMerchantReference(scenario);
        String displayName = "Merchant " + scenario;

        return createMerchantAndReturnId(
            TestJwtSupport.platformOperatorToken(),
            merchantReference,
            displayName
        );
    }

    public static String uniqueMerchantReference(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }

        return "MERCH-" + scenario + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase();
    }

    public static Map<String, Object> createMerchantBody(
        String merchantReference,
        String displayName
    ) {
        return Map.of(
            "merchantReference", merchantReference,
            "displayName", displayName
        );
    }

}
