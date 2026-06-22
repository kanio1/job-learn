package lab.paymentquality.payment.internal.application;

import jakarta.persistence.EntityManager;
import lab.paymentquality.payment.PaymentOrderSeed;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentOrderStatusHistory;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaIdempotencyRecordRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
class PaymentSeedService implements PaymentSeedCapability {

    private final JpaIdempotencyRecordRepository idempotencyRepository;
    private final JpaPaymentOrderStatusHistoryRepository historyRepository;
    private final JpaPaymentOrderRepository paymentOrderRepository;
    private final EntityManager entityManager;

    PaymentSeedService(JpaIdempotencyRecordRepository idempotencyRepository,
                       JpaPaymentOrderStatusHistoryRepository historyRepository,
                       JpaPaymentOrderRepository paymentOrderRepository,
                       EntityManager entityManager) {
        this.idempotencyRepository = idempotencyRepository;
        this.historyRepository = historyRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void seed(List<PaymentOrderSeed> orders) {
        clear();
        for (PaymentOrderSeed seed : orders) {
            var order = PaymentOrder.seeded(
                    seed.paymentOrderId(), seed.merchantId(), seed.clientOrderReference(),
                    seed.amountMinor(), seed.currency(), PaymentStatus.valueOf(seed.status()), seed.version(),
                    seed.createdAt(), seed.updatedAt(), seed.authorizedAt(), seed.expiresAt(), seed.capturedAt(),
                    seed.cancelledAt(), seed.refundedAt(), seed.capturedAmountMinor(), seed.refundedAmountMinor(),
                    seed.cancellationReason(), seed.refundReason());
            entityManager.persist(order);

            var historyId = UUID.nameUUIDFromBytes(
                    ("deterministic-seed-history:" + seed.paymentOrderId()).getBytes(StandardCharsets.UTF_8));
            historyRepository.save(PaymentOrderStatusHistory.seededCreationEntry(
                    historyId, seed.paymentOrderId(), seed.createdAt()));
        }
        entityManager.flush();
    }

    @Override
    @Transactional
    public void clear() {
        idempotencyRepository.deleteAllInBatch();
        historyRepository.deleteAllInBatch();
        paymentOrderRepository.deleteAllInBatch();
        entityManager.flush();
    }
}
