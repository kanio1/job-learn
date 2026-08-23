package lab.paymentquality.eventlab.internal.web;

import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;

import java.time.Instant;
import java.util.UUID;

public record EventLabRecordDto(
        UUID id,
        UUID eventId,
        String consumerGroup,
        String action,
        String targetType,
        String targetId,
        String tenantRef,
        String status,
        int attempts,
        Instant consumedAt,
        String lastError,
        String topic,
        int partitionNo,
        long recordOffset,
        String recordKey
) {
    public static EventLabRecordDto from(EventLabProcessed e) {
        return new EventLabRecordDto(e.getId(), e.getEventId(), e.getConsumerGroup(), e.getAction(), e.getTargetType(),
                e.getTargetId(), e.getTenantRef(), e.getStatus(), e.getAttempts(), e.getConsumedAt(), e.getLastError(),
                e.getTopic(), e.getPartitionNo(), e.getRecordOffset(), e.getRecordKey());
    }
}
