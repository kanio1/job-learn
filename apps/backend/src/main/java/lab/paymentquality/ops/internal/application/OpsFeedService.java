package lab.paymentquality.ops.internal.application;

import lab.paymentquality.ops.OpsFeedFrame;
import lab.paymentquality.ops.OpsFeedPublisher;
import lab.paymentquality.ops.internal.infrastructure.JpaOpsFeedEventRepository;
import lab.paymentquality.ops.internal.infrastructure.OpsFeedBroker;
import lab.paymentquality.ops.internal.infrastructure.OpsFeedEventRecord;
import lab.paymentquality.ops.internal.web.InjectFeedRequest;
import lab.paymentquality.ops.internal.web.InjectFeedResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OpsFeedService implements OpsFeedPublisher {

    static final String BROKER_DESTINATION = "/topic/ops/feed";

    private final OpsFeedBroker broker;
    private final SimpMessagingTemplate messagingTemplate;
    private final OpsNotificationService notificationService;
    private final JpaOpsFeedEventRepository feedEventRepository;

    public OpsFeedService(
            OpsFeedBroker broker,
            SimpMessagingTemplate messagingTemplate,
            OpsNotificationService notificationService,
            JpaOpsFeedEventRepository feedEventRepository) {
        this.broker = broker;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.feedEventRepository = feedEventRepository;
    }

    public InjectFeedResponse inject(InjectFeedRequest request, String recipientSubject) {
        if (request != null && request.raw() != null && !request.raw().isBlank()) {
            persistMalformed(request.raw());
            publishRaw(request.raw());
            return InjectFeedResponse.ignoredRaw();
        }
        OpsFeedFrame frame = frameFrom(request);
        persistFrame(frame);
        publish(frame);
        notificationService.createForRecipient(recipientSubject, frame);
        return InjectFeedResponse.fromFrame(frame);
    }

    public List<OpsFeedFrame> recent(UUID merchantFilter) {
        return broker.recent(merchantFilter);
    }

    public void disconnect(String subject) {
        broker.disconnect(subject);
    }

    @Override
    public void publish(OpsFeedFrame frame) {
        broker.recordAndBroadcast(frame);
        messagingTemplate.convertAndSend(BROKER_DESTINATION, frame);
    }

    @Override
    public void publishRaw(String rawPayload) {
        broker.broadcastRaw(rawPayload);
        messagingTemplate.convertAndSend(BROKER_DESTINATION, rawPayload);
    }

    private void persistFrame(OpsFeedFrame frame) {
        try {
            feedEventRepository.saveAndFlush(OpsFeedEventRecord.fromFrame(frame));
        } catch (DataIntegrityViolationException ignored) {
            // duplicate eventId is allowed on inject; UI dedupes
        }
    }

    private void persistMalformed(String rawPayload) {
        feedEventRepository.saveAndFlush(OpsFeedEventRecord.malformed(rawPayload));
    }

    private static OpsFeedFrame frameFrom(InjectFeedRequest request) {
        UUID eventId = request != null && request.eventId() != null ? request.eventId() : UUID.randomUUID();
        Instant occurredAt = request != null && request.occurredAt() != null ? request.occurredAt() : Instant.now();
        String type = request != null && request.type() != null && !request.type().isBlank()
                ? request.type()
                : "PAYMENT_CAPTURED";
        String label = request != null && request.label() != null && !request.label().isBlank()
                ? request.label()
                : type;
        UUID merchantId = request == null ? null : request.merchantId();
        UUID paymentOrderId = request == null ? null : request.paymentOrderId();
        return new OpsFeedFrame(eventId, occurredAt, merchantId, paymentOrderId, type, label);
    }
}
