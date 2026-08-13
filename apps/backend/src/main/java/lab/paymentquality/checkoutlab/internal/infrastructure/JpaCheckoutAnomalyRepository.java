package lab.paymentquality.checkoutlab.internal.infrastructure;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCheckoutAnomalyRepository extends JpaRepository<CheckoutAnomaly, UUID> {

    List<CheckoutAnomaly> findAllByOrderByDetectedAtDesc();

    boolean existsBySessionIdAndKind(UUID sessionId, String kind);
}
