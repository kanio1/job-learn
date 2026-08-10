package lab.paymentquality.checkoutlab.internal.infrastructure;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCheckoutFulfillmentRepository extends JpaRepository<CheckoutFulfillment, UUID> {
}
