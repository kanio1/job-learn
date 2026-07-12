package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOrderExpiryTest {

    @Test
    void expireTransitionsAuthorizedOrderToExpired() {
        PaymentOrder order = authorizedOrder(Instant.now().minusSeconds(60));

        order.expire();

        assertThat(order.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void expireOnCreatedOrderThrowsInvalidStateTransition() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), "PAY-001", 1000, "EUR");

        assertThatThrownBy(order::expire)
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void isAuthorizationExpiredTrueOnlyForOverdueAuthorizedOrder() {
        PaymentOrder overdue = authorizedOrder(Instant.now().minusSeconds(1));
        PaymentOrder notYetDue = authorizedOrder(Instant.now().plusSeconds(3600));

        assertThat(overdue.isAuthorizationExpired()).isTrue();
        assertThat(notYetDue.isAuthorizationExpired()).isFalse();
    }

    @Test
    void captureOnOverdueAuthorizedOrderExpiresAndThrows() {
        PaymentOrder order = authorizedOrder(Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> order.capture(null))
                .isInstanceOf(AuthorizationExpiredException.class);
        assertThat(order.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    private PaymentOrder authorizedOrder(Instant expiresAt) {
        return PaymentOrder.seeded(
                UUID.randomUUID(), UUID.randomUUID(), "PAY-001", 5000, "EUR",
                PaymentStatus.AUTHORIZED, 1L, Instant.now().minusSeconds(3600), Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(3600), expiresAt, null, null, null, null, null, null, null);
    }
}
