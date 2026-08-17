package lab.paymentquality.testing.internal.seed;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

final class AuditLearningGenerator {

    static final int AUDIT_COUNT = 10_000;

    private static final long RANGE_SECONDS =
            Duration.between(DataLearningFixtures.RANGE_START, DataLearningFixtures.RANGE_END).getSeconds();
    private static final String[] ACTIONS = {
            "MERCHANT_CREATED",
            "MERCHANT_ACTIVATED",
            "MERCHANT_SUSPENDED",
            "MERCHANT_RISK_FLAGGED",
            "PAYMENT_CREATED",
            "PAYMENT_CAPTURED",
            "PAYMENT_REFUNDED"
    };

    private AuditLearningGenerator() {
    }

    static AuditRow rowFor(int index) {
        String action = ACTIONS[index % ACTIONS.length];
        boolean merchant = action.startsWith("MERCHANT_");
        String outcome = outcomeFor(index);
        boolean bothStates = index % 10 == 0;
        String beforeState = bothStates ? "{\"status\":\"BEFORE\"}" : null;
        String afterState = bothStates || "SUCCESS".equals(outcome) ? "{\"status\":\"AFTER\"}" : null;
        Instant occurredAt = DataLearningFixtures.RANGE_START.plusSeconds(RANGE_SECONDS * index / (AUDIT_COUNT - 1));
        return new AuditRow(
                DataLearningFixtures.nameUuid("data-learning:audit:" + index),
                occurredAt,
                "data-learning-seed",
                "Data Learning Seed",
                action,
                merchant ? "MERCHANT" : "PAYMENT_ORDER",
                DataLearningFixtures.nameUuid("data-learning:audit-target:" + index).toString(),
                tenantReferenceFor(index),
                "LEARN-AUD-" + String.format("%06d", index),
                outcome,
                beforeState,
                afterState
        );
    }

    static String tenantReferenceFor(int index) {
        int bucket = index % 100;
        if (bucket < 55) {
            return Fixtures.TENANT_ALPHA;
        }
        if (bucket < 75) {
            return Fixtures.PLATFORM_TENANT;
        }
        if (bucket < 90) {
            return DataLearningFixtures.LEARN_TENANT_C;
        }
        if (bucket < 98) {
            return DataLearningFixtures.LEARN_TENANT_D;
        }
        return Fixtures.PLACEHOLDER_TENANT;
    }

    private static String outcomeFor(int index) {
        int bucket = index % 20;
        if (bucket == 0) {
            return "DENIED";
        }
        if (bucket == 1) {
            return "FAILED";
        }
        return "SUCCESS";
    }

    record AuditRow(
            UUID id,
            Instant occurredAt,
            String actorSubject,
            String actorDisplay,
            String action,
            String targetType,
            String targetId,
            String tenantId,
            String correlationId,
            String outcome,
            String beforeState,
            String afterState
    ) {
    }
}
