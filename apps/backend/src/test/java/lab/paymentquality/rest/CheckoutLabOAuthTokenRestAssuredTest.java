package lab.paymentquality.rest;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.checkout-lab.enabled=true",
                "app.checkout-lab.oauth-client-secret=test-oauth-secret",
                "app.checkout-lab.hmac-secret=test-hmac-secret",
                "app.checkout-lab.hosted-checkout-base-url=http://localhost:3000"
        })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class CheckoutLabOAuthTokenRestAssuredTest extends PostgresContainerSupport {

    private static final String CLIENT_ID = "checkout-lab-merchant";
    private static final String CLIENT_SECRET = "test-oauth-secret";

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_oauth_token_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void postTokenWithValidClientCredentialsReturnsLabBearerToken() {
        given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("access_token", startsWith("lab."))
                .body("token_type", equalTo("Bearer"))
                .body("expires_in", equalTo(3600));
    }

    @Test
    void postTokenWithWrongClientSecretReturns401() {
        given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", "wrong-secret")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(401);
    }

    @Test
    void postTokenWithJsonContentTypeReturns401Not415() {
        given().port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "grant_type": "client_credentials",
                          "client_id": "%s",
                          "client_secret": "%s"
                        }
                        """.formatted(CLIENT_ID, CLIENT_SECRET))
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(401);
    }

    @Test
    void labAccessTokenIsRejectedByKeycloakJwtDecoder() {
        String accessToken = given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .extract().path("access_token");

        assertThat(accessToken).startsWith("lab.");
        assertThatThrownBy(() -> jwtDecoder.decode(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void trustedMerchantWithExtCustomerIdReturnsToken() {
        given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "trusted_merchant")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .formParam("email", "payer@example.com")
                .formParam("extCustomerId", "cust-1")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .body("token_type", equalTo("Bearer"));
    }

    @Test
    void trustedMerchantWithoutExtCustomerIdReturns401() {
        given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .formParam("grant_type", "trusted_merchant")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .formParam("email", "payer@example.com")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(401);
    }
}
