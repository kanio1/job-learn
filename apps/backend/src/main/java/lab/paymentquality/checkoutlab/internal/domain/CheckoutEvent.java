package lab.paymentquality.checkoutlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "checkout_event")
public class CheckoutEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", length = 64, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "event_type", length = 64, nullable = false, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private Map<String, Object> payload;

    @Column(name = "signature_header", length = 512, updatable = false)
    private String signatureHeader;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", length = 32, nullable = false)
    private CheckoutEventProcessStatus processStatus;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    protected CheckoutEvent() {
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public CheckoutEventProcessStatus getProcessStatus() {
        return processStatus;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    void assignForPersistence(
            UUID id,
            String eventId,
            UUID sessionId,
            String eventType,
            Map<String, Object> payload,
            Instant receivedAt,
            CheckoutEventProcessStatus processStatus) {
        this.id = id;
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.processStatus = processStatus;
        this.attempts = 0;
    }
}
