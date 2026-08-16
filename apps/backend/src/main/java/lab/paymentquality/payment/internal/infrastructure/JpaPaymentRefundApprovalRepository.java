package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentRefundApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPaymentRefundApprovalRepository extends JpaRepository<PaymentRefundApproval, UUID> {

    List<PaymentRefundApproval> findByPaymentOrderIdOrderByCreatedAtDesc(UUID paymentOrderId);
}
