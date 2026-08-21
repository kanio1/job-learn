package lab.paymentquality.merchant.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record MerchantListRequest(
        String q,

        @Pattern(regexp = "DRAFT|ACTIVE|SUSPENDED", message = "status must be DRAFT, ACTIVE, or SUSPENDED")
        String status,

        Boolean riskFlagged,

        String tenantId,

        @PositiveOrZero(message = "page must be >= 0")
        Integer page,

        @Min(value = 1, message = "size must be >= 1")
        @Max(value = 100, message = "size must be <= 100")
        Integer size,

        @Pattern(
                regexp = "(createdAt|updatedAt|displayName|status),(asc|desc)",
                message = "sort must be createdAt, updatedAt, displayName, or status with asc or desc")
        String sort
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT = "createdAt,desc";

    public MerchantListRequest {
        q = blankToNull(q);
        status = blankToNull(status);
        tenantId = blankToNull(tenantId);
        sort = blankToNull(sort);
    }

    public static MerchantListRequest defaults() {
        return new MerchantListRequest(null, null, null, null, null, null, null);
    }

    public int effectivePage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int effectiveSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public String effectiveSort() {
        return sort == null ? DEFAULT_SORT : sort;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
