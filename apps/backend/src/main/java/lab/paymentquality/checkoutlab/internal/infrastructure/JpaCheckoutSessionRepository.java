package lab.paymentquality.checkoutlab.internal.infrastructure;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
}
