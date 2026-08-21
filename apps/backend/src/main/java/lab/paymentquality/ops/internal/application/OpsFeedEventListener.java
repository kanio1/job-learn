package lab.paymentquality.ops.internal.application;

import lab.paymentquality.ops.OpsFeedFrame;
import lab.paymentquality.ops.OpsFeedPublisher;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class OpsFeedEventListener {

    private final OpsFeedPublisher publisher;
    private final OpsNotificationService notificationService;

    OpsFeedEventListener(OpsFeedPublisher publisher, OpsNotificationService notificationService) {
        this.publisher = publisher;
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    void on(AuditableActionOccurred event) {
        OpsFeedFrame frame = OpsFeedMapper.fromAudit(event);
        if (frame == null) {
            return;
        }
        publisher.publish(frame);
        String assignee = stringFrom(event.afterState(), "assigneeSubject");
        if (assignee != null) {
            notificationService.createForRecipient(assignee, frame);
        }
    }

    private static String stringFrom(Map<String, Object> state, String key) {
        if (state == null || !state.containsKey(key) || state.get(key) == null) {
            return null;
        }
        String value = String.valueOf(state.get(key)).strip();
        return value.isEmpty() ? null : value;
    }
}
