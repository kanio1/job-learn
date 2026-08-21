package lab.paymentquality.support;

import java.util.UUID;

public record SupportCaseSeed(
        UUID tenantId,
        UUID merchantId,
        UUID paymentOrderId,
        String title,
        SupportCasePriority priority,
        String assigneeSubject
) {
}
