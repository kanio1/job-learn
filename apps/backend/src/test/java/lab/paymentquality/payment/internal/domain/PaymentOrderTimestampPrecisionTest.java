package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderTimestampPrecisionTest {

    @Test
    void createdOrderUsesPostgresCompatibleMicrosecondPrecision() {
        PaymentOrder order = PaymentOrder.create(
                UUID.randomUUID(), UUID.randomUUID(), "PAY-TIMESTAMP-001", 1234, "PLN");

        assertThat(order.getCreatedAt()).isEqualTo(order.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
        assertThat(order.getUpdatedAt()).isEqualTo(order.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }
}
