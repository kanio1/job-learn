package lab.paymentquality.foundation.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusControllerTest {

    @Test
    void returnsFoundationStatusWithoutBusinessData() {
        var response = new StatusController().status();

        assertThat(response.application()).isEqualTo("payment-quality-lab");
        assertThat(response.phase()).isEqualTo("foundation");
        assertThat(response.status()).isEqualTo("UP");
    }

    @Test
    void responseDoesNotExposePaymentOrInfrastructureDetails() {
        var response = new StatusResponse("payment-quality-lab", "foundation", "UP");

        assertThat(response.toString())
                .doesNotContain("password")
                .doesNotContain("keycloak")
                .doesNotContain("paymentId")
                .doesNotContain("postgres");
    }
}
