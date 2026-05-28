package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

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
}
