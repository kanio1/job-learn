package lab.paymentquality.security;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.*;
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


import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PaymentOrderSecurityTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_security_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void unauthenticatedCreateReturns401() {
        MerchantApiTestSupport.publicRequest(port)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-001")
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-001"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", "00000000-0000-0000-0000-000000000001")
                .then()
                .statusCode(401);
    }

    @Test
    void unauthenticatedReadReturns401() {
        MerchantApiTestSupport.publicRequest(port)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}",
                        "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002")
                .then()
                .statusCode(401);
    }

    @Test
    void deniedIdentityGets403ForCreate() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String deniedToken = TestJwtSupport.deniedToken();

        MerchantApiTestSupport.requestWithToken(port, deniedToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("denied"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-002"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    void deniedIdentityGets403ForRead() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String deniedToken = TestJwtSupport.deniedToken();

        MerchantApiTestSupport.requestWithToken(port, deniedToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}",
                        merchantId, "00000000-0000-0000-0000-000000000002")
                .then()
                .statusCode(403);
    }

    @Test
    void creatorCanCreateButNotRead() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("creator");

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-003"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(403);
    }

    @Test
    void readerCanReadButNotCreate() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("reader"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-004"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    void merchantCreatorCannotCreateForDifferentMerchantScope() {
        String merchantIdA = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String merchantIdB = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String tokenForMerchantA = TestJwtSupport.merchantPaymentCreatorToken(merchantIdA);

        MerchantApiTestSupport.requestWithToken(port, tokenForMerchantA)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("scope-mismatch"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-SCOPE"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantIdB)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));
    }

    @Test
    void merchantNaturalReferenceClaimCanCreateOnlyForItsResolvedMerchant() {
        String merchantReference = MerchantApiTestSupport.uniqueMerchantReference("CLAIM");
        var created = MerchantApiTestSupport.operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(MerchantApiTestSupport.createMerchantBody(merchantReference, "Claim-scoped Merchant"))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract();
        String merchantId = created.path("merchantId");
        String etag = created.header("ETag");

        MerchantApiTestSupport.operatorRequest(port)
                .header("If-Match", etag)
                .when()
                .post("/api/merchants/{merchantId}/activate", merchantId)
                .then()
                .statusCode(200);

        String naturalReferenceToken = TestJwtSupport.merchantPaymentCreatorToken(merchantReference);
        MerchantApiTestSupport.requestWithToken(port, naturalReferenceToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("natural-ref"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-NATURAL-REF"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201);

        String foreignReferenceToken = TestJwtSupport.merchantPaymentCreatorToken(
                MerchantApiTestSupport.uniqueMerchantReference("FOREIGN"));
        MerchantApiTestSupport.requestWithToken(port, foreignReferenceToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("foreign-ref"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-FOREIGN-REF"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));
    }

    @Test
    void operateOnlyGets403ForCreateAndRead() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String operatorToken = TestJwtSupport.merchantPaymentOperatorToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, operatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("operate"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-005"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403);

        MerchantApiTestSupport.requestWithToken(port, operatorToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}",
                        merchantId, "00000000-0000-0000-0000-000000000002")
                .then()
                .statusCode(403);
    }

    @Test
    void platformReaderCanReadCrossMerchantButNotCreate() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String platformToken = TestJwtSupport.platformPaymentReaderToken();
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("plat-read");

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(2000, "EUR", "PAY-006"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, platformToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(200);

        MerchantApiTestSupport.requestWithToken(port, platformToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("plat-create"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(3000, "USD", "PAY-007"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    void expiredTokenReturns401() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String expiredToken = TestJwtSupport.expiredToken();

        MerchantApiTestSupport.requestWithToken(port, expiredToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("expired"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-008"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(401);
    }

    @Test
    void publicStatusEndpointRemainsAccessible() {
        MerchantApiTestSupport.publicRequest(port)
                .when()
                .get("/api/status")
                .then()
                .statusCode(200);
    }
}
