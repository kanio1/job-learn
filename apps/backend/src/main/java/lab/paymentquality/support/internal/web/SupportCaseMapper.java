package lab.paymentquality.support.internal.web;

import lab.paymentquality.support.internal.domain.SupportCase;

import java.util.List;

public final class SupportCaseMapper {

    private SupportCaseMapper() {
    }

    public static SupportCaseResponse toResponse(SupportCase supportCase) {
        return new SupportCaseResponse(
                supportCase.getCaseId(),
                supportCase.getCaseReference(),
                supportCase.getTenantId(),
                supportCase.getMerchantId(),
                supportCase.getPaymentOrderId(),
                supportCase.getStatus().name(),
                supportCase.getPriority().name(),
                supportCase.getAssigneeSubject(),
                supportCase.getTitle(),
                supportCase.getVersion() == null ? 0L : supportCase.getVersion(),
                supportCase.getCreatedAt(),
                supportCase.getUpdatedAt());
    }

    public static SupportCaseListResponse toListResponse(List<SupportCase> cases) {
        return new SupportCaseListResponse(cases.stream().map(SupportCaseMapper::toResponse).toList());
    }
}
