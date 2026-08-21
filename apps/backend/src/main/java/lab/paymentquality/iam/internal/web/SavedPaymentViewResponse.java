package lab.paymentquality.iam.internal.web;

import lab.paymentquality.iam.internal.domain.UserSavedView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SavedPaymentViewResponse(
        UUID id,
        String name,
        String resource,
        Map<String, Object> filters,
        List<String> columns,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt) {

    public static SavedPaymentViewResponse from(UserSavedView view) {
        return new SavedPaymentViewResponse(
                view.getViewId(),
                view.getName(),
                view.getResource(),
                view.getFilters(),
                view.getColumns(),
                view.isDefault(),
                view.getCreatedAt(),
                view.getUpdatedAt());
    }
}
