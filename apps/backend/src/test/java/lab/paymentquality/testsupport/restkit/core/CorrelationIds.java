package lab.paymentquality.testsupport.restkit.core;

import java.util.UUID;

public final class CorrelationIds {

    private CorrelationIds() {
    }

    public static String forScenario(String scenario) {
        String normalized = requireScenario(scenario);
        return "corr-" + normalized + "-" + UUID.randomUUID();
    }

    private static String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        return scenario.trim();
    }
}
