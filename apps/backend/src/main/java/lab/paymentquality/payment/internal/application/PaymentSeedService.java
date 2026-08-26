package lab.paymentquality.payment.internal.application;

import jakarta.persistence.EntityManager;
import lab.paymentquality.payment.PaymentOrderSeed;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.payment.internal.domain.PaymentOrderStatusHistory;
import lab.paymentquality.payment.internal.infrastructure.JpaIdempotencyRecordRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderEvidenceRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentExportJobRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundApprovalRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundChallengeRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderNoteRepository;
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
    private final JpaPaymentOrderEvidenceRepository evidenceRepository;
    private final JpaPaymentExportJobRepository exportJobRepository;
    private final JpaPaymentRefundApprovalRepository refundApprovalRepository;
    private final JpaPaymentRefundChallengeRepository refundChallengeRepository;
    private final JpaPaymentOrderNoteRepository noteRepository;
    private final JpaPaymentOrderRepository paymentOrderRepository;
    private final EntityManager entityManager;

    PaymentSeedService(JpaIdempotencyRecordRepository idempotencyRepository,
                       JpaPaymentOrderStatusHistoryRepository historyRepository,
                                   JpaPaymentOrderEvidenceRepository evidenceRepository,
                                   JpaPaymentExportJobRepository exportJobRepository,
                                   JpaPaymentRefundApprovalRepository refundApprovalRepository,
                                   JpaPaymentRefundChallengeRepository refundChallengeRepository,
                                   JpaPaymentOrderNoteRepository noteRepository,
                       JpaPaymentOrderRepository paymentOrderRepository,
                       EntityManager entityManager) {
        this.idempotencyRepository = idempotencyRepository;
        this.historyRepository = historyRepository;
        this.evidenceRepository = evidenceRepository;
        this.exportJobRepository = exportJobRepository;
        this.refundApprovalRepository = refundApprovalRepository;
        this.refundChallengeRepository = refundChallengeRepository;
        this.noteRepository = noteRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void seed(List<PaymentOrderSeed> orders) {
        clear();
        // Use native SQL INSERT to bypass JPA's entity-state detection.
        // persist()/merge() both fail for entities with an assigned UUID ID and a non-null
        // @Version: persist() throws PersistentObjectException (treats non-null version as
        // "detached") and merge() throws StaleObjectStateException (SELECT finds nothing,
        // concludes the row was deleted by another transaction).
        for (PaymentOrderSeed seed : orders) {
            entityManager.createNativeQuery("""
                    INSERT INTO payment_orders (
                        payment_order_id, merchant_id, client_order_reference,
                        amount_minor, currency, status, version,
                        created_at, updated_at,
                        authorized_at, expires_at, captured_at, cancelled_at, refunded_at,
                        captured_amount_minor, refunded_amount_minor,
                        cancellation_reason, refund_reason
                    ) VALUES (
                        :paymentOrderId, :merchantId, :clientOrderReference,
                        :amountMinor, :currency, :status, :version,
                        :createdAt, :updatedAt,
                        :authorizedAt, :expiresAt, :capturedAt, :cancelledAt, :refundedAt,
                        :capturedAmountMinor, :refundedAmountMinor,
                        :cancellationReason, :refundReason
                    )""")
                    .setParameter("paymentOrderId", seed.paymentOrderId())
                    .setParameter("merchantId", seed.merchantId())
                    .setParameter("clientOrderReference", seed.clientOrderReference())
                    .setParameter("amountMinor", seed.amountMinor())
                    .setParameter("currency", seed.currency())
                    .setParameter("status", seed.status())
                    .setParameter("version", seed.version())
                    .setParameter("createdAt", seed.createdAt())
                    .setParameter("updatedAt", seed.updatedAt())
                    .setParameter("authorizedAt", seed.authorizedAt())
                    .setParameter("expiresAt", seed.expiresAt())
                    .setParameter("capturedAt", seed.capturedAt())
                    .setParameter("cancelledAt", seed.cancelledAt())
                    .setParameter("refundedAt", seed.refundedAt())
                    .setParameter("capturedAmountMinor", seed.capturedAmountMinor())
                    .setParameter("refundedAmountMinor", seed.refundedAmountMinor())
                    .setParameter("cancellationReason", seed.cancellationReason())
                    .setParameter("refundReason", seed.refundReason())
                    .executeUpdate();

            var historyId = UUID.nameUUIDFromBytes(
                    ("deterministic-seed-history:" + seed.paymentOrderId()).getBytes(StandardCharsets.UTF_8));
            historyRepository.save(PaymentOrderStatusHistory.seededCreationEntry(
                    historyId, seed.paymentOrderId(), seed.createdAt()));
        }
    }

    @Override
    @Transactional
    public void clear() {
        refundChallengeRepository.deleteAllInBatch();
        refundApprovalRepository.deleteAllInBatch();
        exportJobRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        noteRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        historyRepository.deleteAllInBatch();
        paymentOrderRepository.deleteAllInBatch();
        entityManager.flush();
    }
}
