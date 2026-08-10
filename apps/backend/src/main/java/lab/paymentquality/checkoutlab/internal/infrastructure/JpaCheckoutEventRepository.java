package lab.paymentquality.checkoutlab.internal.infrastructure;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCheckoutEventRepository extends JpaRepository<CheckoutEvent, UUID> {
}
