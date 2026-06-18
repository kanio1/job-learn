package lab.paymentquality.iam.internal.application;

public record UserListQuery(
        String tenantId,
        String role,
        String status,
        String search,
        int page,
        int size) {

    private static final int MAX_PAGE_SIZE = 100;

    public UserListQuery(String role, String status, String search, int page, int size) {
        this(null, role, status, search, page, size);
    }

    public UserListQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
