package lab.paymentquality.audit.internal.web.dto;

import java.util.List;

public record AuditListResponse(
        List<AuditEventSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public AuditListResponse {
        content = List.copyOf(content == null ? List.of() : content);
    }
}
