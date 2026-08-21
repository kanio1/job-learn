package lab.paymentquality.ops.internal.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lab.paymentquality.ops.OpsFeedFrame;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ops_feed_event")
public class OpsFeedEventRecord {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OpsFeedEventRecord() {
    }

    public static OpsFeedEventRecord fromFrame(OpsFeedFrame frame) {
        OpsFeedEventRecord record = new OpsFeedEventRecord();
        record.eventId = frame.eventId();
        record.occurredAt = frame.occurredAt();
        record.merchantId = frame.merchantId();
        record.paymentOrderId = frame.paymentOrderId();
        record.eventType = frame.type();
        record.label = frame.label() == null ? frame.type() : frame.label();
        record.createdAt = Instant.now();
        return record;
    }

    public static OpsFeedEventRecord malformed(String rawPayload) {
        OpsFeedEventRecord record = new OpsFeedEventRecord();
        record.eventId = UUID.randomUUID();
        record.occurredAt = Instant.now();
        record.eventType = "MALFORMED";
        record.label = "MALFORMED";
        record.rawPayload = rawPayload;
        record.createdAt = Instant.now();
        return record;
    }
}
