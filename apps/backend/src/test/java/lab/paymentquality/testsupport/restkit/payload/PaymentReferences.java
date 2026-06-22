package lab.paymentquality.testsupport.restkit.payload;

import java.util.UUID;

public final class PaymentReferences {

    private PaymentReferences() {
    }

    public static String unique(String scenario) {
        return "ref-" + requireScenario(scenario) + "-" + UUID.randomUUID();
    }

    private static String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        return scenario.trim();
    }
}
