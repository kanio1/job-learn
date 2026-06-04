package lab.paymentquality.testsupport.restkit.idempotency;

import java.util.UUID;

public final class IdempotencyKeysCopy {

    private IdempotencyKeysCopy() {
    }

    public static String forScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        return "idem-" + scenario.trim() + "-" + UUID.randomUUID();
    }

}
