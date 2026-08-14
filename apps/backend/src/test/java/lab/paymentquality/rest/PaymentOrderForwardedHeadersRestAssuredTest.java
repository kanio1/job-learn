package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("REST-SSL-PROXY-01 / RA-RFC-030 hostile headers; RA-RFC-031 Caddy-shaped X-Forwarded-*")
class PaymentOrderForwardedHeadersRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_fwd_headers");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void createLocationStaysRelativeWhenForwardedHostAndProtoAreHostile() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(12500, "PLN", "PAY-FWD-001");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("fwd"))
                .header("Host", "evil.example")
                .header("X-Forwarded-Host", "evil.example")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Port", "443")
                .header("Forwarded", "for=1.2.3.4;host=evil.example;proto=https")
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .header("Location", startsWith("/api/merchants/" + merchantId + "/payment-orders/"))
                .header("Location", not(startsWith("http")))
                .header("Location", not(equalTo("https://evil.example")));
    }

    @Test
    void createLocationStaysRelativeWhenHeadersMatchCaddyReverseProxyDefaults() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(12500, "PLN", "PAY-CADDY-001");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("caddy"))
                .header("Host", "localhost:8080")
                .header("X-Forwarded-For", "10.0.0.1")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "api.payment-quality.local:8443")
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .header("Location", startsWith("/api/merchants/" + merchantId + "/payment-orders/"))
                .header("Location", not(startsWith("http")))
                .header("Location", not(equalTo("https://api.payment-quality.local:8443")));
    }
}
