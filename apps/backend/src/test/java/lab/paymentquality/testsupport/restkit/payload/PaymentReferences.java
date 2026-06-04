package lab.paymentquality.testsupport.restkit.payload;

import java.util.UUID;

public final class PaymentReferences {

    private PaymentReferences() {
    }

    public static String unique(String scenario) {
        String normalized = requireScenario(scenario);
        return "ref-" + normalized + "-" + UUID.randomUUID();
    }

    private static String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        return scenario.trim();
    }
}
