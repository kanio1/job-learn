package lab.paymentquality.apitest.scenarios;

import io.restassured.http.ContentType;
import lab.paymentquality.apitest.api.StatusApi;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * Smoke: {@code GET /api/status} responds 200 with the expected application identity.
 *
 * <p>Contract protected: public status endpoint — no auth, no DB writes, no side-effects.
 * Why API-level: proves the backend container started, port-mapped correctly, and the Spring
 * context reached the UP state (Flyway ran, datasource connected, security config loaded).
 * HTTP concept: simplest HTTP contract test — status code + JSON body fields.
 * SDET risk: if this test fails, all other specs are meaningless; run first in CI.
 */
@ApiTest
@Tag("smoke")
@DisplayName("Status endpoint")
class StatusSpec {

    @Test
    @DisplayName("GET /api/status → 200 with application identity")
    void get_status_returns_200_with_expected_body() {
        StatusApi.status()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("application", equalTo("payment-quality-lab"))
                .body("status", equalTo("UP"))
                .body("phase", equalTo("foundation"));
    }
}
