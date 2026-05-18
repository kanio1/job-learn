package lab.paymentquality.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatusRestAssuredTest {

    @LocalServerPort
    private int port;

    @Test
    void statusEndpointSupportsFoundationOnlyHttpSmokeCheck() {
        given()
                .port(port)
        .when()
                .get("/api/status")
        .then()
                .statusCode(200)
                .body("application", equalTo("payment-quality-lab"))
                .body("phase", equalTo("foundation"))
                .body("status", equalTo("UP"));
    }
}
