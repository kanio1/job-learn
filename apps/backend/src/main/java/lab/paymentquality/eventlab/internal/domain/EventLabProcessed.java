package lab.paymentquality.eventlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventlab_processed")
public class EventLabProcessed {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "consumer_group", nullable = false, length = 128)
    private String consumerGroup;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 160)
    private String targetId;

    @Column(name = "tenant_ref", nullable = false, length = 64)
    private String tenantRef;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "partition_no", nullable = false)
    private int partitionNo;

    @Column(name = "record_offset", nullable = false)
    private long recordOffset;

    @Column(name = "record_key", length = 160)
    private String recordKey;

    protected EventLabProcessed() {}

    public static EventLabProcessed of(String consumerGroup, UUID eventId, String action, String targetType,
                                       String targetId, String tenantRef, String status, String topic,
                                       int partitionNo, long offset, String recordKey) {
        EventLabProcessed e = new EventLabProcessed();
        e.id = UUID.randomUUID();
        if (e.id.version() != 7) {
            // fallback to random but DB default is uuidv7(); keep generated
        }
        e.consumerGroup = consumerGroup;
        e.eventId = eventId;
        e.action = action;
        e.targetType = targetType;
        e.targetId = targetId;
        e.tenantRef = tenantRef;
        e.status = status;
        e.attempts = 1;
        e.consumedAt = Instant.now();
        e.topic = topic;
        e.partitionNo = partitionNo;
        e.recordOffset = offset;
        e.recordKey = recordKey;
        return e;
    }

    public UUID getId() { return id; }
    public String getConsumerGroup() { return consumerGroup; }
    public UUID getEventId() { return eventId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getTenantRef() { return tenantRef; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getLastError() { return lastError; }
    public String getTopic() { return topic; }
    public int getPartitionNo() { return partitionNo; }
    public long getRecordOffset() { return recordOffset; }
    public String getRecordKey() { return recordKey; }

    public void setStatus(String status) { this.status = status; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
