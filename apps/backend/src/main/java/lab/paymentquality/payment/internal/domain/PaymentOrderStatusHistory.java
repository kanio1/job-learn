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
}
