package lab.paymentquality.apitest.api.audit;

import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client facade for {@code /api/audit} endpoints.
 *
 * <p>Hides path strings and query parameter encoding from scenario classes.
 * Callers set the identity context in {@link lab.paymentquality.apitest.core.context.Ctx}
 * before calling; {@link lab.paymentquality.apitest.core.http.AuthFilter} injects the token.
 *
 * <p><strong>Authorization model:</strong>
 * <ul>
 *   <li>{@code platform:audit:read} — granted to {@code platform.admin} via {@code PLATFORM_ADMIN}
 *       composite role; tenant context resolves to {@code isPlatformScoped=true} → no tenant filter
 *       applied; sees all audit events.</li>
 *   <li>{@code tenant:audit:read} — granted to {@code tenant.admin} via {@code TENANT_ADMIN};
 *       tenant context resolves to {@code isTenantScoped=true} → filters by JWT
 *       {@code tenant_id} claim value (e.g. {@code "TENANT_ALPHA"}).</li>
 * </ul>
 * A user with neither authority receives 403 (from {@code @PreAuthorize} → {@code AuditExceptionHandler}).
 *
 * <p><strong>Filter parameters</strong> (all optional):
 * <ul>
 *   <li>{@code action} — exact match on the audit event action field.</li>
 *   <li>{@code target_type} — exact match on the target type field.</li>
 *   <li>{@code actor} — exact match on actor subject or actor display.</li>
 *   <li>{@code from} / {@code to} — ISO date range ({@code YYYY-MM-DD}).</li>
 *   <li>{@code page} / {@code size} — pagination (default 0 / 20, max size 100).</li>
 * </ul>
 *
 * <p><strong>Sort order:</strong> {@code occurredAt DESC} (newest first).
 *
 * <p>Phase 8A: first use of Awaitility in this framework — audit events are written
 * asynchronously (Spring Modulith {@code @ApplicationModuleListener}), so polling is required
 * after the lifecycle action that generates the event.
 */
public final class AuditApi {

    private static final String LIST_PATH = "/api/audit";
    private static final String BY_ID_PATH = "/api/audit/{id}";

    private AuditApi() {}

    /**
     * {@code GET /api/audit?action={action}&target_type={targetType}&size=50}
     * — list audit events filtered by action and target type.
     *
     * <p>Uses page size 50 to ensure the expected event is on the first page within a single
     * {@code mvn verify} run (where audit events accumulate from the current run only).
     *
     * <p>Requires: {@code platform:audit:read} OR {@code tenant:audit:read}.
     * Happy-path: 200 with {@code AuditListResponse} body, {@code Vary: Authorization}.
     *
     * <p>SDET note: there is NO filter by {@code targetId} in the query API. After receiving
     * the list, filter in-test by {@code entry.targetId().equals(paymentOrderId.toString())}
     * to isolate the specific event under test.
     */
    public static Response list(String action, String targetType) {
        return RequestSpecs.base()
                .queryParam("action", action)
                .queryParam("target_type", targetType)
                .queryParam("size", 50)
                .when()
                .get(LIST_PATH);
    }

    /**
     * {@code GET /api/audit} — list all audit events (no filter) with default page size 20.
     *
     * <p>Use when the test needs to verify count or presence without a specific action filter.
     */
    public static Response listAll() {
        return RequestSpecs.base()
                .when()
                .get(LIST_PATH);
    }
}
