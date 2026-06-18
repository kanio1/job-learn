package lab.paymentquality.iam.internal.web.dto;

import java.util.List;

public record UserListResponse(
        List<UserSummary> users,
        int page,
        int size,
        long totalEstimate) {

    public UserListResponse {
        users = users == null ? List.of() : List.copyOf(users);
    }
}
