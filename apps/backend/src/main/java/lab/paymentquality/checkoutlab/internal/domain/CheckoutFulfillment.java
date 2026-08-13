package lab.paymentquality.checkoutlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_fulfillment")
public class CheckoutFulfillment {

    @Id
    @Column(name = "fulfillment_id", nullable = false, updatable = false)
    private UUID fulfillmentId;

    @Column(name = "session_id", updatable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private CheckoutFulfillmentStatus status;

    @Column(name = "source_event_id", length = 64)
    private String sourceEventId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckoutFulfillment() {
    }

    public UUID getFulfillmentId() {
        return fulfillmentId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public CheckoutFulfillmentStatus getStatus() {
        return status;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void confirm(String sourceEventId, Instant now) {
        this.status = CheckoutFulfillmentStatus.CONFIRMED;
        this.sourceEventId = sourceEventId;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        this.status = CheckoutFulfillmentStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        this.status = CheckoutFulfillmentStatus.EXPIRED;
        this.updatedAt = now;
    }

    void assignForPersistence(
            UUID fulfillmentId,
            UUID sessionId,
            CheckoutFulfillmentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.fulfillmentId = fulfillmentId;
        this.sessionId = sessionId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CheckoutFulfillment newFulfillment(
            UUID fulfillmentId,
            UUID sessionId,
            CheckoutFulfillmentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        CheckoutFulfillment fulfillment = new CheckoutFulfillment();
        fulfillment.assignForPersistence(fulfillmentId, sessionId, status, createdAt, updatedAt);
        return fulfillment;
    }
}
