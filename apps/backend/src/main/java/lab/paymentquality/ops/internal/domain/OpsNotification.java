package lab.paymentquality.ops.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ops_notifications")
public class OpsNotification {

    @Id
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "recipient_subject", nullable = false, length = 255)
    private String recipientSubject;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OpsNotification() {
    }

    public static OpsNotification create(
            String recipientSubject,
            UUID eventId,
            String eventType,
            String title,
            String body,
            Map<String, Object> payload) {
        OpsNotification notification = new OpsNotification();
        notification.notificationId = UUID.randomUUID();
        notification.recipientSubject = recipientSubject;
        notification.eventId = eventId;
        notification.eventType = eventType;
        notification.title = title;
        notification.body = body;
        notification.payload = payload == null ? Map.of() : payload;
        notification.createdAt = Instant.now();
        return notification;
    }

    public void markRead(Instant at) {
        if (readAt == null) {
            readAt = at;
        }
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getRecipientSubject() {
        return recipientSubject;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
