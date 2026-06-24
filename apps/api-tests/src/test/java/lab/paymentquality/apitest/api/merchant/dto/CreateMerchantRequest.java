package lab.paymentquality.apitest.api.merchant.dto;

/**
 * Test-side request body for {@code POST /api/merchants}.
 *
 * <p>Mirrors the backend's {@code CreateMerchantRequest} validation contract:
 * <ul>
 *   <li>{@code merchantReference} — required, 1–64 chars, must match {@code [A-Z][A-Z0-9-]*[A-Z0-9]}.</li>
 *   <li>{@code displayName} — required, 2–120 chars, must not be blank or whitespace-only.</li>
 *   <li>{@code tenantReference} — optional; platform-scoped callers may omit it.</li>
 * </ul>
 *
 * <p>Use the static factory methods for test data construction:
 * <ul>
 *   <li>{@link #valid(String, String)} — valid request, no tenant reference (platform-scoped).</li>
 *   <li>{@link #withTenantRef(String, String, String)} — valid request with explicit tenant.</li>
 *   <li>Constructor directly for negative / invalid-data tests.</li>
 * </ul>
 *
 * <p>SDET note: static factory methods encode "valid-by-default" data convention.
 * Invalid data should be built explicitly and named for their negative intent.
 */
public record CreateMerchantRequest(
        String merchantReference,
        String displayName,
        String tenantReference) {

    /** Valid request without tenant reference — for platform-scoped callers (PLATFORM_ADMIN). */
    public static CreateMerchantRequest valid(String merchantReference, String displayName) {
        return new CreateMerchantRequest(merchantReference, displayName, null);
    }

    /** Valid request with an explicit tenant reference. */
    public static CreateMerchantRequest withTenantRef(String merchantReference,
                                                      String displayName,
                                                      String tenantReference) {
        return new CreateMerchantRequest(merchantReference, displayName, tenantReference);
    }
}
