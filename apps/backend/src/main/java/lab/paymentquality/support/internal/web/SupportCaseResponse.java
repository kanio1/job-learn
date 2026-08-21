package lab.paymentquality.support.internal.web;

import java.time.Instant;
import java.util.UUID;

public record SupportCaseResponse(
        UUID caseId,
        String caseReference,
        UUID tenantId,
        UUID merchantId,
        UUID paymentOrderId,
        String status,
        String priority,
        String assigneeSubject,
        String title,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
