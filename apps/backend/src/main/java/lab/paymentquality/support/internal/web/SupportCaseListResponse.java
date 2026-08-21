package lab.paymentquality.support.internal.web;

import java.util.List;

public record SupportCaseListResponse(List<SupportCaseResponse> content) {
    public SupportCaseListResponse {
        content = List.copyOf(content != null ? content : List.of());
    }
}
