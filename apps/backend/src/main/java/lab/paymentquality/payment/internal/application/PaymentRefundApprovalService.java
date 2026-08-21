package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.DualControlSelfApproveException;
import lab.paymentquality.payment.internal.domain.InvalidStateTransitionException;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentRefundApproval;
import lab.paymentquality.payment.internal.domain.PaymentRefundApprovalNotFoundException;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundApprovalRepository;
import lab.paymentquality.shared.events.AuditableActionEventFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentRefundApprovalService {

    private final JpaPaymentRefundApprovalRepository approvalRepository;
    private final PaymentLifecycleService paymentLifecycleService;
    private final PaymentOrderService paymentOrderService;
    private final PaymentRefundChallengeService challengeService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentRefundApprovalService(JpaPaymentRefundApprovalRepository approvalRepository,
                                        PaymentLifecycleService paymentLifecycleService,
                                        PaymentOrderService paymentOrderService,
                                        PaymentRefundChallengeService challengeService,
                                        ApplicationEventPublisher eventPublisher) {
        this.approvalRepository = approvalRepository;
        this.paymentLifecycleService = paymentLifecycleService;
        this.paymentOrderService = paymentOrderService;
        this.challengeService = challengeService;
        this.eventPublisher = eventPublisher;
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
        PaymentRefundApproval saved = approvalRepository.saveAndFlush(approval);
        Map<String, Object> afterState = new LinkedHashMap<>();
        afterState.put("merchantId", merchantId.toString());
        afterState.put("paymentOrderId", paymentOrderId.toString());
        afterState.put("clientOrderReference", order.getClientOrderReference());
        afterState.put("status", "REFUND_APPROVAL_NEEDED");
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                "REFUND_APPROVAL_NEEDED",
                "PAYMENT_ORDER",
                paymentOrderId.toString(),
                null,
                makerSubject,
                null,
                null,
                afterState));
        return saved;
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
        PaymentOrder pendingOrder = paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        Long amount = approval.getAmountMinor() != null ? approval.getAmountMinor() : pendingOrder.getAmountMinor();
        if (PaymentRefundChallengeService.requiresPin(amount)
                && !challengeService.isVerifiedForApproval(approval.getApprovalId())) {
            throw new lab.paymentquality.payment.internal.domain.RefundChallengeException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "step_up_required",
                    "High-value refund requires a verified PIN challenge");
        }
        PaymentOrder order = paymentLifecycleService.refund(
                merchantId, paymentOrderId, approval.getAmountMinor(), approval.getReason(),
                idempotencyKeyHash, expectedVersion, actorSubject, correlationId);
        approval.markApproved();
        return order;
    }
}
