package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.internal.domain.OpsNotification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID eventId,
        String eventType,
        String title,
        String body,
        Map<String, Object> payload,
        Instant readAt,
        Instant createdAt
) {

    static NotificationResponse from(OpsNotification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getEventId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPayload(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
