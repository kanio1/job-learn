package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    private static final long AUTHORIZATION_EXPIRY_DAYS = 7;

    private static final Map<PaymentStatus, java.util.Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, java.util.Set.of(PaymentStatus.AUTHORIZED, PaymentStatus.CANCELLED),
            PaymentStatus.AUTHORIZED, java.util.Set.of(PaymentStatus.CAPTURED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED),
            PaymentStatus.CAPTURED, java.util.Set.of(PaymentStatus.REFUNDED)
    );

    @Id
    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "client_order_reference", length = 120, nullable = false)
    private String clientOrderReference;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "captured_amount_minor")
    private Long capturedAmountMinor;

    @Column(name = "refunded_amount_minor")
    private Long refundedAmountMinor;

    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @Column(name = "refund_reason", length = 200)
    private String refundReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    protected PaymentOrder() {
    }

    public static PaymentOrder create(UUID paymentOrderId, UUID merchantId,
                                       String clientOrderReference, long amountMinor,
                                       String currency) {
        var order = new PaymentOrder();
        order.paymentOrderId = paymentOrderId;
        order.merchantId = merchantId;
        order.clientOrderReference = clientOrderReference;
        order.amountMinor = amountMinor;
        order.currency = currency;
        order.status = PaymentStatus.CREATED;
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    public boolean canTransitionTo(PaymentStatus target) {
        var allowed = VALID_TRANSITIONS.get(this.status);
        return allowed != null && allowed.contains(target);
    }

    public boolean isAuthorizationExpired() {
        return this.status == PaymentStatus.AUTHORIZED
                && this.expiresAt != null
                && Instant.now().isAfter(this.expiresAt);
    }

    public void authorize() {
        if (!canTransitionTo(PaymentStatus.AUTHORIZED)) {
            throw new InvalidStateTransitionException(status, PaymentStatus.AUTHORIZED);
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = Instant.now();
        this.expiresAt = this.authorizedAt.plus(AUTHORIZATION_EXPIRY_DAYS, ChronoUnit.DAYS);
        this.updatedAt = this.authorizedAt;
    }

    public void capture(Long captureAmountMinor) {
        if (isAuthorizationExpired()) {
            this.status = PaymentStatus.EXPIRED;
            this.updatedAt = Instant.now();
            throw new AuthorizationExpiredException();
        }
        if (!canTransitionTo(PaymentStatus.CAPTURED)) {
            throw new InvalidStateTransitionException(status, PaymentStatus.CAPTURED);
        }
        long effectiveAmount = captureAmountMinor != null ? captureAmountMinor : this.amountMinor;
        if (effectiveAmount <= 0 || effectiveAmount > this.amountMinor) {
            throw new InvalidCaptureAmountException(effectiveAmount, this.amountMinor);
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedAmountMinor = effectiveAmount;
        this.capturedAt = Instant.now();
        this.expiresAt = null;
        this.updatedAt = this.capturedAt;
    }

    public void cancel(String reason) {
        if (!canTransitionTo(PaymentStatus.CANCELLED)) {
            throw new InvalidStateTransitionException(status, PaymentStatus.CANCELLED);
        }
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
        this.expiresAt = null;
        this.updatedAt = this.cancelledAt;
    }

    public void refund(Long refundAmountMinor, String reason) {
        if (!canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidStateTransitionException(status, PaymentStatus.REFUNDED);
        }
        long effectiveAmount = refundAmountMinor != null ? refundAmountMinor : this.capturedAmountMinor;
        if (effectiveAmount <= 0 || effectiveAmount > this.capturedAmountMinor) {
            throw new InvalidRefundAmountException(effectiveAmount, this.capturedAmountMinor);
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundedAmountMinor = effectiveAmount;
        this.refundedAt = Instant.now();
        this.refundReason = reason;
        this.updatedAt = this.refundedAt;
    }

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getClientOrderReference() {
        return clientOrderReference;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
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

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public Long getCapturedAmountMinor() {
        return capturedAmountMinor;
    }

    public Long getRefundedAmountMinor() {
        return refundedAmountMinor;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public String getMetadata() {
        return metadata;
    }
}
