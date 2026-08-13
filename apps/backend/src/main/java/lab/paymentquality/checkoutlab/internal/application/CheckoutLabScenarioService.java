package lab.paymentquality.checkoutlab.internal.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabScenarioService {

    private final Map<UUID, CheckoutLabScenario> scenarios = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> notifyAttempts = new ConcurrentHashMap<>();

    public void assign(UUID sessionId, CheckoutLabScenario scenario) {
        scenarios.put(sessionId, scenario == null ? CheckoutLabScenario.HAPPY_COMPLETED : scenario);
    }

    public CheckoutLabScenario scenarioFor(UUID sessionId) {
        return scenarios.getOrDefault(sessionId, CheckoutLabScenario.HAPPY_COMPLETED);
    }

    public boolean consumeForced503(UUID sessionId) {
        if (scenarioFor(sessionId) != CheckoutLabScenario.NOTIFY_5XX_RETRY) {
            return false;
        }
        int attempt = notifyAttempts.computeIfAbsent(sessionId, ignored -> new AtomicInteger()).incrementAndGet();
        return attempt == 1;
    }

    public boolean shouldSignIncorrectly(UUID sessionId) {
        return scenarioFor(sessionId) == CheckoutLabScenario.BAD_SIGNATURE;
    }

    public boolean skipNotify(UUID sessionId) {
        CheckoutLabScenario scenario = scenarioFor(sessionId);
        return scenario == CheckoutLabScenario.RETURN_LIE_SUCCESS;
    }
}
