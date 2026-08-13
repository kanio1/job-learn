package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutEvent;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabNotifyReceiverService {

    private final CheckoutLabSignatureService signatureService;
    private final CheckoutLabScenarioService scenarioService;
    private final CheckoutLabClock clock;
    private final JpaCheckoutEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public CheckoutLabNotifyReceiverService(
            CheckoutLabSignatureService signatureService,
            CheckoutLabScenarioService scenarioService,
            CheckoutLabClock clock,
            JpaCheckoutEventRepository eventRepository,
            ObjectMapper objectMapper) {
        this.signatureService = signatureService;
        this.scenarioService = scenarioService;
        this.clock = clock;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public record NotifyResult(int status, boolean duplicate, String eventId) {
    }

    @Transactional
    public NotifyResult receive(byte[] rawBody, String eventIdHeader, String signatureHeader) {
        signatureService.verify(signatureHeader, rawBody);
        Map<String, Object> envelope = parse(rawBody);
        String eventId = eventIdHeader != null && !eventIdHeader.isBlank()
                ? eventIdHeader
                : stringValue(envelope.get("id"));
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidCheckoutSignatureException("Lab-Event-Id is required");
        }
        Optional<CheckoutEvent> existing = eventRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            return new NotifyResult(200, true, eventId);
        }
        UUID sessionId = extractSessionId(envelope);
        if (scenarioService.consumeForced503(sessionId)) {
            throw new CheckoutLabTransientException("Forced 503 for notify_5xx_retry");
        }
        String eventType = stringValue(envelope.get("type"));
        try {
            eventRepository.save(CheckoutEvent.received(
                    UUID.randomUUID(),
                    eventId,
                    sessionId,
                    eventType == null ? "unknown" : eventType,
                    envelope,
                    signatureHeader,
                    clock.instant(),
                    202));
        } catch (DataIntegrityViolationException duplicate) {
            return new NotifyResult(200, true, eventId);
        }
        return new NotifyResult(202, false, eventId);
    }

    private Map<String, Object> parse(byte[] rawBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(rawBody, Map.class);
            return envelope;
        } catch (Exception ex) {
            throw new InvalidCheckoutSignatureException("Notify body is not JSON");
        }
    }

    private static UUID extractSessionId(Map<String, Object> envelope) {
        Object data = envelope.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object sessionId = dataMap.get("sessionId");
            if (sessionId != null) {
                return UUID.fromString(sessionId.toString());
            }
        }
        throw new InvalidCheckoutSignatureException("Notify data.sessionId is required");
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
