package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.DualControlSelfApproveException;
import lab.paymentquality.payment.internal.domain.InvalidStateTransitionException;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentRefundApproval;
import lab.paymentquality.payment.internal.domain.PaymentRefundApprovalNotFoundException;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentRefundApprovalService {

    private final JpaPaymentRefundApprovalRepository approvalRepository;
    private final PaymentLifecycleService paymentLifecycleService;
    private final PaymentOrderService paymentOrderService;

    public PaymentRefundApprovalService(JpaPaymentRefundApprovalRepository approvalRepository,
                                        PaymentLifecycleService paymentLifecycleService,
                                        PaymentOrderService paymentOrderService) {
        this.approvalRepository = approvalRepository;
        this.paymentLifecycleService = paymentLifecycleService;
        this.paymentOrderService = paymentOrderService;
    }

    @Transactional
    public PaymentRefundApproval request(UUID merchantId, UUID paymentOrderId, Long amountMinor, String reason,
                                         String makerSubject) {
        PaymentOrder order = paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        if (!order.canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidStateTransitionException(order.getStatus(), PaymentStatus.REFUNDED);
        }
        PaymentRefundApproval approval = PaymentRefundApproval.pending(
                merchantId, paymentOrderId, amountMinor, reason, makerSubject);
        return approvalRepository.saveAndFlush(approval);
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundApproval> list(UUID merchantId, UUID paymentOrderId) {
        paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        return approvalRepository.findByPaymentOrderIdOrderByCreatedAtDesc(paymentOrderId);
    }

    @Transactional
    public PaymentOrder approve(UUID merchantId, UUID paymentOrderId, UUID approvalId, String actorSubject,
                                String idempotencyKeyHash, long expectedVersion, String correlationId) {
        PaymentRefundApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new PaymentRefundApprovalNotFoundException(approvalId));
        if (!approval.getMerchantId().equals(merchantId) || !approval.getPaymentOrderId().equals(paymentOrderId)) {
            throw new PaymentRefundApprovalNotFoundException(approvalId);
        }
        if (approval.getMakerSubject().equals(actorSubject)) {
            throw new DualControlSelfApproveException();
        }
        if (approval.getStatus() == PaymentRefundApproval.Status.APPROVED) {
            return paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        }
        PaymentOrder order = paymentLifecycleService.refund(
                merchantId, paymentOrderId, approval.getAmountMinor(), approval.getReason(),
                idempotencyKeyHash, expectedVersion, actorSubject, correlationId);
        approval.markApproved();
        return order;
    }
}
