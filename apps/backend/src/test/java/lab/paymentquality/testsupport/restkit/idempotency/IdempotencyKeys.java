package lab.paymentquality.testsupport.restkit.idempotency;

import java.util.UUID;

public final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    public static String forScenario(String scenario) {
        String normalized = requireScenario(scenario);
        return "idem-" + normalized + "-" + UUID.randomUUID();
    }

    private static String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        return scenario.trim();
    }
}
