package lab.paymentquality.testing.internal.seed;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

final class PublicationLearningGenerator {

    static final int PUBLICATION_COUNT = 10_000;
    static final String EVENT_TYPE = "lab.paymentquality.shared.events.AuditableActionOccurred";
    static final String LISTENER_ID = "lab.paymentquality.audit.internal.application.AuditEventListener";

    private static final long RANGE_SECONDS =
            Duration.between(DataLearningFixtures.RANGE_START, DataLearningFixtures.RANGE_END).getSeconds();

    private PublicationLearningGenerator() {
    }

    static PublicationRow rowFor(int index) {
        Instant publishedAt = DataLearningFixtures.RANGE_START.plusSeconds(
                RANGE_SECONDS * index / (PUBLICATION_COUNT - 1));
        int bucket = index % 100;
        if (bucket < 90) {
            return completed(index, publishedAt, 1, null);
        }
        if (bucket < 97) {
            return completed(index, publishedAt, 2, publishedAt.plusSeconds(30));
        }
        if (bucket < 99) {
            return completed(index, publishedAt, 3, publishedAt.plusSeconds(90));
        }
        return new PublicationRow(
                idFor(index),
                publishedAt,
                LISTENER_ID,
                serializedEvent(index),
                EVENT_TYPE,
                null,
                publishedAt.plusSeconds(15),
                1,
                "PROCESSING"
        );
    }

    static String serializedEvent(int index) {
        return "{\"index\":" + index + ",\"source\":\"data-learning-seed\"}";
    }

    private static PublicationRow completed(int index, Instant publishedAt, int attempts, Instant lastResubmission) {
        return new PublicationRow(
                idFor(index),
                publishedAt,
                LISTENER_ID,
                serializedEvent(index),
                EVENT_TYPE,
                publishedAt.plusSeconds(attempts * 5L),
                lastResubmission,
                attempts,
                "COMPLETED"
        );
    }

    private static UUID idFor(int index) {
        return DataLearningFixtures.nameUuid("data-learning:publication:" + index);
    }

    record PublicationRow(
            UUID id,
            Instant publicationDate,
            String listenerId,
            String serializedEvent,
            String eventType,
            Instant completionDate,
            Instant lastResubmissionDate,
            int completionAttempts,
            String status
    ) {
    }
}
