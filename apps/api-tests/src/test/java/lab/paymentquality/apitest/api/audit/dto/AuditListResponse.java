package lab.paymentquality.apitest.api.audit.dto;

import java.util.List;
import java.util.UUID;

/**
 * Test-side representation of the audit event list response body.
 *
 * <p>Mirrors the backend's {@code AuditListResponse} and {@code AuditEventSummary} records
 * without importing backend classes. The backend returns this at
 * {@code GET /api/audit} (with optional query filters).
 *
 * <p><strong>Pagination shape:</strong>
 * {@code {content: [...], page: N, size: N, totalElements: N, totalPages: N}}.
 * Consistent with the Spring Data {@code Page<T>} serialization used across this API.
 *
 * <p><strong>Sort order:</strong> entries are sorted by {@code occurredAt DESC} (newest first).
 * Unlike the payment status history endpoint (ascending), the audit log uses descending order
 * so the most recent activity is always on the first page.
 *
 * <p><strong>Async write gap:</strong> audit events are written by
 * {@code AuditEventListener} annotated with {@code @ApplicationModuleListener}, which
 * runs AFTER the publishing transaction commits and in a separate async thread. There is a
 * short delay (typically &lt;500 ms in test) between when the lifecycle response is returned
 * and when the audit event is visible via this endpoint. Use Awaitility to poll rather than
 * blocking with {@code Thread.sleep}.
 *
 * <p><strong>Timestamp as String:</strong> {@code occurredAt} is typed as {@code String}
 * (not {@code Instant}) because {@code jackson-datatype-jsr310} is not on the test classpath.
 * Tests assert non-null presence only.
 *
 * <p><strong>Isolation note:</strong> audit events are NOT cleared by {@code SeedApi.reset()}
 * — only payment orders, merchants, and tenants are cleared. Within a single {@code mvn verify}
 * run (fresh container), events accumulate. Filter by {@code targetId == paymentOrderId.toString()}
 * after querying by {@code action + target_type} to isolate the test's specific event.
 */
public record AuditListResponse(
        List<AuditEventSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * A single audit event summary entry.
     *
     * <p>Fields from the backend {@code AuditEventSummary} record:
     * <ul>
     *   <li>{@code id} — UUID; unique per audit event row.</li>
     *   <li>{@code occurredAt} — ISO-8601 instant string; when the action occurred.</li>
     *   <li>{@code actorDisplay} — human-readable actor label (Keycloak
     *       {@code preferred_username} or fallback to subject).</li>
     *   <li>{@code action} — operation type string; one of {@code PAYMENT_AUTHORIZED},
     *       {@code PAYMENT_CAPTURED}, {@code PAYMENT_CANCELLED}, {@code PAYMENT_REFUNDED}.
     *       Note: payment create does NOT emit an audit event.</li>
     *   <li>{@code targetType} — resource type; always {@code "PAYMENT_ORDER"} for
     *       payment lifecycle events.</li>
     *   <li>{@code targetId} — resource identifier; {@code paymentOrderId.toString()}
     *       for payment events.</li>
     *   <li>{@code tenantId} — tenant reference string (e.g. {@code "TENANT_ALPHA"});
     *       sourced from the JWT {@code tenant_id} claim of the actor.</li>
     *   <li>{@code correlationId} — {@code X-Correlation-ID} from the original request;
     *       propagated via MDC by {@code CorrelationIdFilter} into
     *       {@code AuditableActionEventFactory.success()} and thence into the event row.</li>
     *   <li>{@code outcome} — string representation of {@code Outcome} enum:
     *       {@code "SUCCESS"}, {@code "DENIED"}, or {@code "FAILED"}.</li>
     * </ul>
     */
    public record AuditEventSummary(
            UUID id,
            String occurredAt,
            String actorDisplay,
            String action,
            String targetType,
            String targetId,
            String tenantId,
            String correlationId,
            String outcome
    ) {}
}
