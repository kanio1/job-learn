package lab.paymentquality.rlslab.internal.application;

/**
 * Leak-contrast counts for the platform compare demo.
 *
 * @param bypassRoleCount rows visible to the {@code BYPASSRLS} lab role (Java picks this
 *                        DataSource after {@code TenantContext.isPlatformScoped()}; clients
 *                        cannot SET a GUC to get the same view)
 * @param restrictedWithoutTenantGuc rows visible to {@code rls_lab_app} with no
 *                                   {@code SET LOCAL app.tenant_id} — FORCE RLS yields 0
 * @param unprotected rows on {@code rls_lab_item_unprotected} (no RLS)
 */
public record RlsLabCompareResult(long bypassRoleCount, long restrictedWithoutTenantGuc, long unprotected) {
}
