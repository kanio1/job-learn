package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByMerchantIdAndPaymentOrderId(UUID merchantId, UUID paymentOrderId);

    Optional<PaymentOrder> findByPaymentOrderId(UUID paymentOrderId);
}
