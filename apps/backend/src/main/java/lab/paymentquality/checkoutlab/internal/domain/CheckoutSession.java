package lab.paymentquality.checkoutlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_session")
public class CheckoutSession {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "ext_order_id", length = 120, nullable = false)
    private String extOrderId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private CheckoutSessionStatus status;

    @Column(name = "continue_url", nullable = false)
    private String continueUrl;

    @Column(name = "notify_url", nullable = false)
    private String notifyUrl;

    @Column(name = "redirect_uri", nullable = false)
    private String redirectUri;

    @Column(name = "validity_until")
    private Instant validityUntil;

    @Column(name = "idempotency_key_hash", length = 64)
    private String idempotencyKeyHash;

    @Column(name = "correlation_id", length = 128, nullable = false)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected CheckoutSession() {
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getExtOrderId() {
        return extOrderId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public String getContinueUrl() {
        return continueUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public Instant getValidityUntil() {
        return validityUntil;
    }

    public String getIdempotencyKeyHash() {
        return idempotencyKeyHash;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isExpired(Instant now) {
        return validityUntil != null && !now.isBefore(validityUntil);
    }

    public void applyStatus(CheckoutSessionStatus nextStatus, Instant now) {
        this.status = nextStatus;
        this.updatedAt = now;
    }

    public void assignIdempotencyKeyHash(String hash) {
        this.idempotencyKeyHash = hash;
    }

    void assignForPersistence(
            UUID sessionId,
            String extOrderId,
            long amountMinor,
            String currency,
            CheckoutSessionStatus status,
            String continueUrl,
            String notifyUrl,
            String redirectUri,
            Instant validityUntil,
            String correlationId,
            Instant createdAt,
            Instant updatedAt) {
        this.sessionId = sessionId;
        this.extOrderId = extOrderId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.status = status;
        this.continueUrl = continueUrl;
        this.notifyUrl = notifyUrl;
        this.redirectUri = redirectUri;
        this.validityUntil = validityUntil;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CheckoutSession newSession(
            UUID sessionId,
            String extOrderId,
            long amountMinor,
            String currency,
            CheckoutSessionStatus status,
            String continueUrl,
            String notifyUrl,
            String redirectUri,
            Instant validityUntil,
            String correlationId,
            Instant createdAt,
            Instant updatedAt) {
        CheckoutSession session = new CheckoutSession();
        session.assignForPersistence(
                sessionId,
                extOrderId,
                amountMinor,
                currency,
                status,
                continueUrl,
                notifyUrl,
                redirectUri,
                validityUntil,
                correlationId,
                createdAt,
                updatedAt);
        return session;
    }
}
