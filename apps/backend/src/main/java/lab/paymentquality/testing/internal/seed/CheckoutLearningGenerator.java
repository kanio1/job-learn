package lab.paymentquality.testing.internal.seed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

final class CheckoutLearningGenerator {

    static final int SESSION_COUNT = 2_000;

    private static final long RANGE_SECONDS =
            Duration.between(DataLearningFixtures.RANGE_START, DataLearningFixtures.RANGE_END).getSeconds();

    private CheckoutLearningGenerator() {
    }

    static UUID sessionIdFor(int index) {
        return DataLearningFixtures.nameUuid("data-learning:checkout-session:" + index);
    }

    static String extOrderIdFor(int index) {
        return "LEARN-CHK-" + String.format("%06d", index);
    }

    static Instant createdAtFor(int index) {
        long offset = RANGE_SECONDS * index / (SESSION_COUNT - 1);
        return DataLearningFixtures.RANGE_START.plusSeconds(offset);
    }

    static Scenario scenarioFor(int index) {
        return switch (index % 40) {
            case 25 -> Scenario.CANCELED;
            case 26 -> Scenario.EXPIRED;
            case 27 -> Scenario.REFUNDED;
            case 28 -> Scenario.RETRY;
            case 29 -> Scenario.ACK_503;
            case 30 -> Scenario.DUPLICATE;
            case 31 -> Scenario.WRONG_FULFILLMENT;
            case 32 -> Scenario.CREATED;
            case 33 -> Scenario.PENDING;
            case 39 -> Scenario.MISSING_FULFILLMENT;
            default -> Scenario.HAPPY;
        };
    }

    static int eventCountFor(int index) {
        int residue = index % 40;
        return switch (residue) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 20, 21, 22, 23, 24, 28, 29 -> 3;
            case 30 -> 4;
            case 32 -> 1;
            default -> 2;
        };
    }

    static SessionRow sessionFor(int index) {
        Scenario scenario = scenarioFor(index);
        Instant createdAt = createdAtFor(index);
        int eventCount = eventCountFor(index);
        Instant updatedAt = createdAt.plusSeconds((eventCount - 1) * 60L);
        String status = sessionStatus(scenario);
        return new SessionRow(
                sessionIdFor(index),
                extOrderIdFor(index),
                1_000L + (index % 50_000),
                currencyFor(index),
                status,
                "https://learn.local/continue",
                "https://learn.local/notify",
                "https://learn.local/redirect/" + extOrderIdFor(index),
                createdAt.plus(Duration.ofDays(7)),
                idempotencyHash(index),
                "data-learning-checkout",
                createdAt,
                updatedAt,
                versionFor(status)
        );
    }

    static List<EventRow> eventsFor(int index) {
        Scenario scenario = scenarioFor(index);
        Instant createdAt = createdAtFor(index);
        UUID sessionId = sessionIdFor(index);
        int eventCount = eventCountFor(index);
        var events = new ArrayList<EventRow>(eventCount);
        for (int step = 0; step < eventCount; step++) {
            events.add(eventFor(index, step, eventCount, scenario, sessionId, createdAt.plusSeconds(step * 60L)));
        }
        return List.copyOf(events);
    }

    static FulfillmentRow fulfillmentFor(int index) {
        if (scenarioFor(index) == Scenario.MISSING_FULFILLMENT) {
            return null;
        }
        Scenario scenario = scenarioFor(index);
        Instant createdAt = createdAtFor(index);
        String status = fulfillmentStatus(scenario);
        Instant confirmedAt = "CONFIRMED".equals(status) ? createdAt.plusSeconds(60) : null;
        String sourceEventId = "CONFIRMED".equals(status) ? eventIdFor(index, eventCountFor(index) - 1) : null;
        return new FulfillmentRow(
                DataLearningFixtures.nameUuid("data-learning:checkout-fulfillment:" + index),
                sessionIdFor(index),
                status,
                sourceEventId,
                confirmedAt,
                createdAt,
                createdAt.plusSeconds(60)
        );
    }

    static AnomalyRow anomalyFor(int index) {
        if (scenarioFor(index) != Scenario.MISSING_FULFILLMENT) {
            return null;
        }
        Instant createdAt = createdAtFor(index);
        return new AnomalyRow(
                DataLearningFixtures.nameUuid("data-learning:checkout-anomaly:" + index),
                sessionIdFor(index),
                "missing_fulfillment",
                "Session has notify events but no fulfillment row",
                createdAt.plus(Duration.ofMinutes(10))
        );
    }

    private static EventRow eventFor(int index, int step, int eventCount, Scenario scenario,
                                     UUID sessionId, Instant receivedAt) {
        String eventType = eventType(scenario, step, eventCount);
        String processStatus = processStatus(scenario, step, eventCount);
        int attempts = attempts(scenario, step, eventCount);
        Integer ackStatus = ackStatus(scenario, step);
        String lastError = "FAILED".equals(processStatus) ? lastError(scenario) : null;
        String eventId = eventIdFor(index, step);
        String payload = """
                {"id":"%s","type":"%s","extOrderId":"%s"}
                """.formatted(eventId, eventType, extOrderIdFor(index)).strip();
        return new EventRow(
                DataLearningFixtures.nameUuid("data-learning:checkout-event:" + index + ":" + step),
                eventId,
                sessionId,
                eventType,
                payload,
                receivedAt,
                processStatus,
                attempts,
                lastError,
                ackStatus
        );
    }

    private static String eventType(Scenario scenario, int step, int eventCount) {
        if (step == 0 && scenario != Scenario.REFUNDED) {
            return "checkout.session.created";
        }
        return switch (scenario) {
            case CANCELED -> "checkout.session.canceled";
            case EXPIRED -> "checkout.session.expired";
            case REFUNDED -> step == 0 ? "checkout.session.completed" : "checkout.session.refunded";
            case PENDING -> "checkout.session.pending";
            case CREATED -> "checkout.session.created";
            default -> step == eventCount - 1 || (scenario == Scenario.DUPLICATE && step >= 2)
                    || ((scenario == Scenario.RETRY || scenario == Scenario.ACK_503) && step >= 1)
                    ? "checkout.session.completed"
                    : "checkout.session.pending";
        };
    }

    private static String processStatus(Scenario scenario, int step, int eventCount) {
        return switch (scenario) {
            case CREATED -> "RECEIVED";
            case PENDING -> step == 0 ? "RECEIVED" : "PROCESSING";
            case RETRY, ACK_503 -> step == 1 ? "FAILED" : "DONE";
            case DUPLICATE -> step == eventCount - 1 ? "DUPLICATE" : "DONE";
            default -> "DONE";
        };
    }

    private static int attempts(Scenario scenario, int step, int eventCount) {
        return switch (scenario) {
            case CREATED -> 0;
            case PENDING -> step;
            case RETRY -> step == 1 ? 3 : 1 + step;
            case ACK_503 -> step == 1 ? 2 : 1;
            case DUPLICATE -> step == eventCount - 1 ? 1 : 1;
            default -> 1;
        };
    }

    private static Integer ackStatus(Scenario scenario, int step) {
        return switch (scenario) {
            case CREATED, PENDING -> null;
            case ACK_503 -> step == 1 ? 503 : 200;
            default -> 200;
        };
    }

    private static String lastError(Scenario scenario) {
        return scenario == Scenario.ACK_503 ? "Forced 503 for notify retry" : "notify timeout";
    }

    private static String sessionStatus(Scenario scenario) {
        return switch (scenario) {
            case CANCELED -> "CANCELED";
            case EXPIRED -> "EXPIRED";
            case REFUNDED -> "REFUNDED";
            case CREATED -> "CREATED";
            case PENDING -> "PENDING";
            default -> "COMPLETED";
        };
    }

    private static String fulfillmentStatus(Scenario scenario) {
        return switch (scenario) {
            case CANCELED -> "CANCELLED";
            case EXPIRED -> "EXPIRED";
            case WRONG_FULFILLMENT, CREATED, PENDING -> "AWAITING_PAYMENT";
            default -> "CONFIRMED";
        };
    }

    private static String currencyFor(int index) {
        return switch (index % 3) {
            case 0 -> "PLN";
            case 1 -> "EUR";
            default -> "USD";
        };
    }

    private static long versionFor(String status) {
        return switch (status) {
            case "CREATED" -> 0;
            case "PENDING" -> 1;
            case "REFUNDED" -> 3;
            default -> 2;
        };
    }

    private static String eventIdFor(int index, int step) {
        return "LEARN-EVT-" + String.format("%06d", index) + "-" + step;
    }

    private static String idempotencyHash(int index) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("data-learning:checkout-idem:" + index).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    enum Scenario {
        HAPPY,
        CANCELED,
        EXPIRED,
        REFUNDED,
        RETRY,
        ACK_503,
        DUPLICATE,
        WRONG_FULFILLMENT,
        CREATED,
        PENDING,
        MISSING_FULFILLMENT
    }

    record SessionRow(
            UUID sessionId,
            String extOrderId,
            long amountMinor,
            String currency,
            String status,
            String continueUrl,
            String notifyUrl,
            String redirectUri,
            Instant validityUntil,
            String idempotencyKeyHash,
            String correlationId,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
    }

    record EventRow(
            UUID id,
            String eventId,
            UUID sessionId,
            String eventType,
            String payload,
            Instant receivedAt,
            String processStatus,
            int attempts,
            String lastError,
            Integer ackStatus
    ) {
    }

    record FulfillmentRow(
            UUID fulfillmentId,
            UUID sessionId,
            String status,
            String sourceEventId,
            Instant confirmedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record AnomalyRow(
            UUID anomalyId,
            UUID sessionId,
            String kind,
            String detail,
            Instant detectedAt
    ) {
    }
}
