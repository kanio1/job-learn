package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPaymentOrderStatusHistoryRepository extends JpaRepository<PaymentOrderStatusHistory, UUID> {

    List<PaymentOrderStatusHistory> findByPaymentOrderIdOrderByCreatedAtAsc(UUID paymentOrderId);

    List<PaymentOrderStatusHistory> findByPaymentOrderIdAndActionIsNotNullOrderByCreatedAtAsc(UUID paymentOrderId);
}
