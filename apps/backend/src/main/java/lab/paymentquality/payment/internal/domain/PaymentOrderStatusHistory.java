package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_order_status_history")
public class PaymentOrderStatusHistory {

    @Id
    @Column(name = "status_history_id", nullable = false, updatable = false)
    private UUID statusHistoryId;

    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20, nullable = false)
    private String toStatus;

    @Column(name = "actor_subject", length = 200, nullable = false)
    private String actorSubject;

    @Column(name = "correlation_id", length = 128, nullable = false)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private PaymentLifecycleAction action;

    @Column(name = "idempotency_key_hash", length = 64)
    private String idempotencyKeyHash;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "amount_minor")
    private Long amountMinor;

    @Column(name = "psp_reference", length = 200)
    private String pspReference;

    protected PaymentOrderStatusHistory() {
    }

    public static PaymentOrderStatusHistory creationEntry(UUID paymentOrderId, String actorSubject,
                                                           String correlationId) {
        var entry = new PaymentOrderStatusHistory();
        entry.statusHistoryId = UUID.randomUUID();
        entry.paymentOrderId = paymentOrderId;
        entry.fromStatus = null;
        entry.toStatus = PaymentStatus.CREATED.name();
        entry.actorSubject = actorSubject;
        entry.correlationId = correlationId;
        entry.createdAt = Instant.now();
        return entry;
    }

    public static PaymentOrderStatusHistory seededCreationEntry(UUID statusHistoryId, UUID paymentOrderId,
                                                                 Instant createdAt) {
        var entry = new PaymentOrderStatusHistory();
        entry.statusHistoryId = statusHistoryId;
        entry.paymentOrderId = paymentOrderId;
        entry.fromStatus = null;
        entry.toStatus = PaymentStatus.CREATED.name();
        entry.actorSubject = "deterministic-seed";
        entry.correlationId = "deterministic-seed";
        entry.createdAt = createdAt;
        return entry;
    }

    public static PaymentOrderStatusHistory lifecycleEntry(UUID paymentOrderId,
                                                            PaymentStatus fromStatus,
                                                            PaymentStatus toStatus,
                                                            PaymentLifecycleAction action,
                                                            String actorSubject,
                                                            String correlationId,
                                                            String idempotencyKeyHash,
                                                            String reason,
                                                            Long amountMinor,
                                                            String pspReference) {
        var entry = new PaymentOrderStatusHistory();
        entry.statusHistoryId = UUID.randomUUID();
        entry.paymentOrderId = paymentOrderId;
        entry.fromStatus = fromStatus.name();
        entry.toStatus = toStatus.name();
        entry.action = action;
        entry.actorSubject = actorSubject;
        entry.correlationId = correlationId;
        entry.idempotencyKeyHash = idempotencyKeyHash;
        entry.reason = reason;
        entry.amountMinor = amountMinor;
        entry.pspReference = pspReference;
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getStatusHistoryId() {
        return statusHistoryId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public PaymentLifecycleAction getAction() {
        return action;
    }

    public String getIdempotencyKeyHash() {
        return idempotencyKeyHash;
    }

    public String getReason() {
        return reason;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public String getPspReference() {
        return pspReference;
    }
}
