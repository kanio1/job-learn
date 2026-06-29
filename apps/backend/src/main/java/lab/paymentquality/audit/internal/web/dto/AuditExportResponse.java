package lab.paymentquality.audit.internal.web.dto;

import java.util.List;

public record AuditExportResponse(
        List<AuditExportEvent> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public AuditExportResponse {
        content = List.copyOf(content == null ? List.of() : content);
    }
}
