package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentOrderEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPaymentOrderEvidenceRepository extends JpaRepository<PaymentOrderEvidence, UUID> {

    List<PaymentOrderEvidence> findByPaymentOrderIdOrderByUploadedAtDescEvidenceIdAsc(UUID paymentOrderId);
}
