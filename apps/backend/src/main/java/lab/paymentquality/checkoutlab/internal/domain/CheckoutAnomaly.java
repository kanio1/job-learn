package lab.paymentquality.checkoutlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_anomaly")
public class CheckoutAnomaly {

    @Id
    @Column(name = "anomaly_id", nullable = false, updatable = false)
    private UUID anomalyId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "kind", length = 64, nullable = false)
    private String kind;

    @Column(name = "detail", nullable = false)
    private String detail;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    protected CheckoutAnomaly() {
    }

    public UUID getAnomalyId() {
        return anomalyId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getKind() {
        return kind;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public static CheckoutAnomaly detected(UUID anomalyId, UUID sessionId, String kind, String detail, Instant detectedAt) {
        CheckoutAnomaly anomaly = new CheckoutAnomaly();
        anomaly.anomalyId = anomalyId;
        anomaly.sessionId = sessionId;
        anomaly.kind = kind;
        anomaly.detail = detail;
        anomaly.detectedAt = detectedAt;
        return anomaly;
    }
}
