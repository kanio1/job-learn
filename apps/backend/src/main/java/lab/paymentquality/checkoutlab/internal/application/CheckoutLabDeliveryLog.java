package lab.paymentquality.checkoutlab.internal.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabDeliveryLog {

    public record DeliveryAttempt(
            UUID sessionId,
            String eventId,
            int attempt,
            int responseStatus,
            Instant at) {
    }

    private final List<DeliveryAttempt> attempts = new CopyOnWriteArrayList<>();

    public void record(UUID sessionId, String eventId, int attempt, int responseStatus, Instant at) {
        attempts.add(new DeliveryAttempt(sessionId, eventId, attempt, responseStatus, at));
    }

    public List<DeliveryAttempt> forSession(UUID sessionId) {
        return attempts.stream().filter(attempt -> attempt.sessionId().equals(sessionId)).toList();
    }

    public List<DeliveryAttempt> all() {
        return new ArrayList<>(attempts);
    }

    public void clear() {
        attempts.clear();
    }
}
