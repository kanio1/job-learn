package lab.paymentquality.support.internal.web;

import java.util.List;

public record BulkAssignResponse(int succeeded, List<BulkAssignFailure> failed) {
    public BulkAssignResponse {
        failed = List.copyOf(failed != null ? failed : List.of());
    }
}
