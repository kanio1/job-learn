package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentRefundChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRefundChallengeRepository extends JpaRepository<PaymentRefundChallenge, UUID> {

    Optional<PaymentRefundChallenge> findByApprovalIdAndVerifiedAtIsNull(UUID approvalId);

    Optional<PaymentRefundChallenge> findByApprovalIdAndVerifiedAtIsNotNull(UUID approvalId);

    boolean existsByApprovalIdAndVerifiedAtIsNotNull(UUID approvalId);
}
