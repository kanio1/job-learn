package lab.paymentquality.ops.internal.application;

import lab.paymentquality.ops.OpsFeedFrame;
import lab.paymentquality.ops.internal.domain.OpsNotification;
import lab.paymentquality.ops.internal.domain.OpsNotificationNotFoundException;
import lab.paymentquality.ops.internal.infrastructure.JpaOpsNotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inbox audience: actionable events only (PAYMENT_FAILED, REFUND_APPROVAL_NEEDED,
 * SUPPORT_CASE_ASSIGNED). Happy-path CAPTURE stays on the live feed, not the inbox.
 * Recipients: injector JWT {@code sub}, plus case assignee when the event carries
 * {@code assigneeSubject}. Manager GET is already scoped by recipient_subject.
 */
@Service
public class OpsNotificationService {

    private final JpaOpsNotificationRepository repository;

    public OpsNotificationService(JpaOpsNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createForRecipient(String recipientSubject, OpsFeedFrame frame) {
        if (recipientSubject == null || recipientSubject.isBlank() || frame == null) {
            return;
        }
        if (!OpsFeedMapper.actionableInboxType(frame.type())) {
            return;
        }
        if (repository.existsByRecipientSubjectAndEventId(recipientSubject, frame.eventId())) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (frame.merchantId() != null) {
            payload.put("merchantId", frame.merchantId().toString());
        }
        if (frame.paymentOrderId() != null) {
            payload.put("paymentOrderId", frame.paymentOrderId().toString());
        }
        payload.put("label", frame.label());
        OpsNotification notification = OpsNotification.create(
                recipientSubject,
                frame.eventId(),
                frame.type(),
                titleFor(frame),
                bodyFor(frame),
                payload);
        try {
            repository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ignored) {
            // UNIQUE (recipient_subject, event_id) — duplicate inject/WS must not bump the badge
        }
    }

    @Transactional(readOnly = true)
    public List<OpsNotification> list(String recipientSubject, boolean unreadOnly) {
        if (unreadOnly) {
            return repository.findByRecipientSubjectAndReadAtIsNullOrderByCreatedAtDesc(recipientSubject);
        }
        return repository.findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject);
    }

    @Transactional(readOnly = true)
    public OpsNotification getOwned(UUID notificationId, String recipientSubject) {
        return repository.findByNotificationIdAndRecipientSubject(notificationId, recipientSubject)
                .orElseThrow(() -> new OpsNotificationNotFoundException(notificationId.toString()));
    }

    @Transactional
    public OpsNotification markRead(UUID notificationId, String recipientSubject) {
        OpsNotification notification = getOwned(notificationId, recipientSubject);
        notification.markRead(Instant.now());
        return repository.saveAndFlush(notification);
    }

    @Transactional
    public int markAllRead(String recipientSubject) {
        return repository.markAllRead(recipientSubject, Instant.now());
    }

    private static String titleFor(OpsFeedFrame frame) {
        return switch (frame.type()) {
            case "PAYMENT_FAILED" -> "Payment failed";
            case "REFUND_APPROVAL_NEEDED" -> "Refund approval needed";
            case "SUPPORT_CASE_ASSIGNED" -> "Support case assigned";
            default -> frame.type();
        };
    }

    private static String bodyFor(OpsFeedFrame frame) {
        return frame.label() == null || frame.label().isBlank() ? frame.type() : frame.label();
    }
}
