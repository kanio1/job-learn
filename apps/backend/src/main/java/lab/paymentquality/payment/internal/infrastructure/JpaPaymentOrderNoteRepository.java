package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentOrderNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPaymentOrderNoteRepository extends JpaRepository<PaymentOrderNote, UUID> {

    List<PaymentOrderNote> findAllByPaymentOrderIdOrderByCreatedAtAsc(UUID paymentOrderId);
}
