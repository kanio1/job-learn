package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_order_note")
public class PaymentOrderNote {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "author_display", length = 200, nullable = false)
    private String authorDisplay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentOrderNote() {
    }

    public static PaymentOrderNote create(UUID paymentOrderId, String body, String authorDisplay) {
        var note = new PaymentOrderNote();
        note.id = UUID.randomUUID();
        note.paymentOrderId = paymentOrderId;
        note.body = body;
        note.authorDisplay = authorDisplay;
        note.createdAt = Instant.now();
        return note;
    }

    public UUID getId() { return id; }
    public UUID getPaymentOrderId() { return paymentOrderId; }
    public String getBody() { return body; }
    public String getAuthorDisplay() { return authorDisplay; }
    public Instant getCreatedAt() { return createdAt; }
}
