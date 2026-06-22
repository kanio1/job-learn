package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.*;
import lab.paymentquality.payment.internal.infrastructure.*;
import lab.paymentquality.shared.events.AuditableActionEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLifecycleService.class);

    private final JpaPaymentOrderRepository paymentOrderRepository;
    private final JpaIdempotencyRecordRepository idempotencyRecordRepository;
    private final JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;
    private final PspClient pspClient;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentLifecycleService(JpaPaymentOrderRepository paymentOrderRepository,
                                    JpaIdempotencyRecordRepository idempotencyRecordRepository,
                                    JpaPaymentOrderStatusHistoryRepository statusHistoryRepository,
                                    PspClient pspClient,
                                    ApplicationEventPublisher eventPublisher) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.pspClient = pspClient;
        this.eventPublisher = eventPublisher;
    }

    public PaymentOrder authorize(UUID merchantId, UUID paymentOrderId, String reason,
                                   String idempotencyKeyHash, long expectedVersion,
                                   String actorSubject, String correlationId) {
        PaymentOrder order = findOrder(merchantId, paymentOrderId);
        if (isIdempotentLifecycleReplay(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.AUTHORIZE, null, reason)) {
            return order;
        }
        PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
        PaymentStatus previousStatus = order.getStatus();

        if (!reserveIdempotency(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.AUTHORIZE, null, reason)) {
            return order;
        }

        PspClient.PspResult pspResult = pspClient.authorize(paymentOrderId, order.getAmountMinor(), order.getCurrency());
        order.authorize();

        recordHistory(order, previousStatus, PaymentLifecycleAction.AUTHORIZE,
                idempotencyKeyHash, reason, null, pspResult.pspReference(), actorSubject, correlationId);

        log.info("payment.authorize.succeeded merchantId={} paymentOrderId={} correlationId={}",
                merchantId, paymentOrderId, correlationId);
        publishSuccess("PAYMENT_AUTHORIZED", paymentOrderId, actorSubject, correlationId);
        return order;
    }

    public PaymentOrder capture(UUID merchantId, UUID paymentOrderId, Long amountMinor, String reason,
                                  String idempotencyKeyHash, long expectedVersion,
                                  String actorSubject, String correlationId) {
        PaymentOrder order = findOrder(merchantId, paymentOrderId);
        if (isIdempotentLifecycleReplay(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.CAPTURE, amountMinor, reason)) {
            return order;
        }
        PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
        PaymentStatus previousStatus = order.getStatus();

        if (!reserveIdempotency(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.CAPTURE, amountMinor, reason)) {
            return order;
        }

        PspClient.PspResult pspResult = pspClient.capture(paymentOrderId,
                amountMinor != null ? amountMinor : order.getAmountMinor(), order.getCurrency());
        order.capture(amountMinor);

        recordHistory(order, previousStatus, PaymentLifecycleAction.CAPTURE,
                idempotencyKeyHash, reason, order.getCapturedAmountMinor(), pspResult.pspReference(), actorSubject, correlationId);

        log.info("payment.capture.succeeded merchantId={} paymentOrderId={} capturedAmountMinor={} correlationId={}",
                merchantId, paymentOrderId, order.getCapturedAmountMinor(), correlationId);
        publishSuccess("PAYMENT_CAPTURED", paymentOrderId, actorSubject, correlationId);
        return order;
    }

    public PaymentOrder cancel(UUID merchantId, UUID paymentOrderId, String reason,
                                String idempotencyKeyHash, long expectedVersion,
                                String actorSubject, String correlationId) {
        PaymentOrder order = findOrder(merchantId, paymentOrderId);
        if (isIdempotentLifecycleReplay(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.CANCEL, null, reason)) {
            return order;
        }
        PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
        PaymentStatus previousStatus = order.getStatus();

        if (!reserveIdempotency(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.CANCEL, null, reason)) {
            return order;
        }

        String pspReference = null;
        if (previousStatus == PaymentStatus.AUTHORIZED) {
            PspClient.PspResult pspResult = pspClient.voidAuthorization(paymentOrderId, "AUTH-" + paymentOrderId);
            pspReference = pspResult.pspReference();
        }
        order.cancel(reason);

        recordHistory(order, previousStatus, PaymentLifecycleAction.CANCEL,
                idempotencyKeyHash, reason, null, pspReference, actorSubject, correlationId);

        log.info("payment.cancel.succeeded merchantId={} paymentOrderId={} correlationId={}",
                merchantId, paymentOrderId, correlationId);
        publishSuccess("PAYMENT_CANCELLED", paymentOrderId, actorSubject, correlationId);
        return order;
    }

    public PaymentOrder refund(UUID merchantId, UUID paymentOrderId, Long amountMinor, String reason,
                                String idempotencyKeyHash, long expectedVersion,
                                String actorSubject, String correlationId) {
        PaymentOrder order = findOrder(merchantId, paymentOrderId);
        if (isIdempotentLifecycleReplay(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.REFUND, amountMinor, reason)) {
            return order;
        }
        PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
        PaymentStatus previousStatus = order.getStatus();

        if (!reserveIdempotency(merchantId, paymentOrderId, idempotencyKeyHash,
                PaymentLifecycleAction.REFUND, amountMinor, reason)) {
            return order;
        }

        PspClient.PspResult pspResult = pspClient.refund(paymentOrderId,
                amountMinor != null ? amountMinor : order.getCapturedAmountMinor(), order.getCurrency());
        order.refund(amountMinor, reason);

        recordHistory(order, previousStatus, PaymentLifecycleAction.REFUND,
                idempotencyKeyHash, reason, order.getRefundedAmountMinor(), pspResult.pspReference(), actorSubject, correlationId);

        log.info("payment.refund.succeeded merchantId={} paymentOrderId={} refundedAmountMinor={} correlationId={}",
                merchantId, paymentOrderId, order.getRefundedAmountMinor(), correlationId);
        publishSuccess("PAYMENT_REFUNDED", paymentOrderId, actorSubject, correlationId);
        return order;
    }

    public PaymentOrder updateMetadata(UUID merchantId, UUID paymentOrderId, String metadata, long expectedVersion) {
        PaymentOrder order = findOrder(merchantId, paymentOrderId);
        PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
        order.updateMetadata(metadata);
        return order;
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderStatusHistory> findHistory(UUID merchantId, UUID paymentOrderId) {
        findOrder(merchantId, paymentOrderId);
        return statusHistoryRepository.findByPaymentOrderIdAndActionIsNotNullOrderByCreatedAtAsc(paymentOrderId);
    }

    private PaymentOrder findOrder(UUID merchantId, UUID paymentOrderId) {
        return paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(paymentOrderId));
    }

    private boolean reserveIdempotency(UUID merchantId, UUID paymentOrderId, String idempotencyKeyHash,
                                       PaymentLifecycleAction action, Long amountMinor, String reason) {
        RequestFingerprint fingerprint = RequestFingerprint.forLifecycle(
                merchantId, paymentOrderId, action, amountMinor, reason);

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByMerchantIdAndPaymentOrderIdAndActionAndIdempotencyKeyHash(
                        merchantId, paymentOrderId, action.name(), idempotencyKeyHash);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
                return false;
            }
            throw new IdempotencyConflictException();
        }

        UUID idempotencyRecordId = UUID.randomUUID();
        int reserved = idempotencyRecordRepository.reserveIfAbsent(
                idempotencyRecordId, merchantId, paymentOrderId, action.name(),
                idempotencyKeyHash, fingerprint.fingerprintHash());
        if (reserved == 0) {
            IdempotencyRecord record = idempotencyRecordRepository
                    .findByMerchantIdAndPaymentOrderIdAndActionAndIdempotencyKeyHash(
                            merchantId, paymentOrderId, action.name(), idempotencyKeyHash)
                    .orElseThrow(() -> new IllegalStateException("Concurrent idempotency reservation was not visible"));
            if (!record.getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
                throw new IdempotencyConflictException();
            }
            return false;
        }

        int completed = idempotencyRecordRepository.complete(idempotencyRecordId, paymentOrderId, Instant.now());
        if (completed != 1) {
            throw new IllegalStateException("Reserved idempotency record was not completed");
        }
        return true;
    }

    private boolean isIdempotentLifecycleReplay(UUID merchantId, UUID paymentOrderId, String idempotencyKeyHash,
                                                PaymentLifecycleAction action, Long amountMinor, String reason) {
        RequestFingerprint fingerprint = RequestFingerprint.forLifecycle(
                merchantId, paymentOrderId, action, amountMinor, reason);

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByMerchantIdAndPaymentOrderIdAndActionAndIdempotencyKeyHash(
                        merchantId, paymentOrderId, action.name(), idempotencyKeyHash);

        if (existing.isEmpty()) {
            return false;
        }
        if (existing.get().getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
            return true;
        }
        throw new IdempotencyConflictException();
    }

    private void recordHistory(PaymentOrder order, PaymentStatus previousStatus,
                                PaymentLifecycleAction action, String idempotencyKeyHash,
                                String reason, Long amountMinor, String pspReference,
                                String actorSubject, String correlationId) {
        PaymentOrderStatusHistory entry = PaymentOrderStatusHistory.lifecycleEntry(
                order.getPaymentOrderId(), previousStatus, order.getStatus(), action,
                actorSubject, correlationId, idempotencyKeyHash, reason, amountMinor, pspReference);
        statusHistoryRepository.saveAndFlush(entry);
    }

    private void publishSuccess(
            String action,
            UUID paymentOrderId,
            String actorSubject,
            String correlationId) {
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                action,
                "PAYMENT_ORDER",
                paymentOrderId.toString(),
                null,
                actorSubject,
                correlationId));
    }
}
