package lab.paymentquality.payment.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibility;
import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import lab.paymentquality.payment.internal.domain.*;
import lab.paymentquality.payment.internal.infrastructure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentOrderService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderService.class);

    private final JpaPaymentOrderRepository paymentOrderRepository;
    private final JpaIdempotencyRecordRepository idempotencyRecordRepository;
    private final JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;
    private final MerchantPaymentEligibilityService merchantEligibilityService;

    public PaymentOrderService(JpaPaymentOrderRepository paymentOrderRepository,
                                JpaIdempotencyRecordRepository idempotencyRecordRepository,
                                JpaPaymentOrderStatusHistoryRepository statusHistoryRepository,
                                MerchantPaymentEligibilityService merchantEligibilityService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.merchantEligibilityService = merchantEligibilityService;
    }

    public PaymentCreateResult create(UUID merchantId, PaymentAmount amount, CurrencyCode currency,
                                       ClientOrderReference clientRef, IdempotencyKey idempotencyKey,
                                       PaymentActorContext actor, String correlationId) {
        MerchantPaymentEligibility eligibility = merchantEligibilityService.findEligibility(merchantId)
                .orElseThrow(() -> new MerchantNotPaymentEligibleException(merchantId));

        if (!eligibility.active()) {
            throw new MerchantNotPaymentEligibleException(merchantId);
        }

        RequestFingerprint fingerprint = RequestFingerprint.of(
                merchantId, amount.minorUnits(), currency.code(), clientRef.value());

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByMerchantIdAndIdempotencyKeyHash(merchantId, idempotencyKey.keyHash());

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
                PaymentOrder order = paymentOrderRepository.findByPaymentOrderId(record.getPaymentOrderId())
                        .orElseThrow(() -> new IllegalStateException("Idempotency record references missing payment order"));
                log.info("payment.create.replay merchantId={} paymentOrderId={} correlationId={}",
                        merchantId, order.getPaymentOrderId(), MDC.get("correlationId"));
                return PaymentCreateResult.replayed(order);
            }
            throw new IdempotencyConflictException();
        }

        UUID idempotencyRecordId = UUID.randomUUID();
        int reserved = idempotencyRecordRepository.reserveIfAbsent(
                idempotencyRecordId, merchantId, idempotencyKey.keyHash(), fingerprint.fingerprintHash());
        if (reserved == 0) {
            return resolveExistingIdempotencyRecord(merchantId, idempotencyKey, fingerprint);
        }

        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId,
                clientRef.value(), amount.minorUnits(), currency.code());

        paymentOrderRepository.saveAndFlush(order);

        PaymentOrderStatusHistory historyEntry = PaymentOrderStatusHistory.creationEntry(
                paymentOrderId, actor.subject(), correlationId);
        statusHistoryRepository.saveAndFlush(historyEntry);

        int completed = idempotencyRecordRepository.complete(idempotencyRecordId, paymentOrderId, Instant.now());
        if (completed != 1) {
            throw new IllegalStateException("Reserved idempotency record was not completed");
        }

        log.info("payment.create.succeeded merchantId={} paymentOrderId={} amountMinor={} currency={} correlationId={}",
                merchantId, paymentOrderId, amount.minorUnits(), currency.code(), MDC.get("correlationId"));
        return PaymentCreateResult.created(order);
    }

    private PaymentCreateResult resolveExistingIdempotencyRecord(
            UUID merchantId, IdempotencyKey idempotencyKey, RequestFingerprint fingerprint) {
        IdempotencyRecord record = idempotencyRecordRepository
                .findByMerchantIdAndIdempotencyKeyHash(merchantId, idempotencyKey.keyHash())
                .orElseThrow(() -> new IllegalStateException("Concurrent idempotency reservation was not visible"));
        if (!record.getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
            throw new IdempotencyConflictException();
        }
        UUID paymentOrderId = record.getPaymentOrderId();
        if (paymentOrderId == null) {
            throw new IllegalStateException("Idempotency record is not completed");
        }
        PaymentOrder order = paymentOrderRepository.findByPaymentOrderId(paymentOrderId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record references missing payment order"));
        log.info("payment.create.replay merchantId={} paymentOrderId={} correlationId={}",
                merchantId, order.getPaymentOrderId(), MDC.get("correlationId"));
        return PaymentCreateResult.replayed(order);
    }

    @Transactional(readOnly = true)
    public PaymentOrder findForMerchant(UUID merchantId, UUID paymentOrderId) {
        return paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(paymentOrderId));
    }

    @Transactional(readOnly = true)
    public PaymentOrder findForPlatform(UUID merchantId, UUID paymentOrderId) {
        return paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(paymentOrderId));
    }
}
