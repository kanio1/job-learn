package lab.paymentquality.apitest.api.payment.dto;

import java.util.List;
import java.util.UUID;

/**
 * Test-side representation of the payment order status history response body.
 *
 * <p>Mirrors the backend's {@code PaymentStatusHistoryResponse} record without importing
 * backend classes. The backend returns this at
 * {@code GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history}.
 *
 * <p><strong>Critical design note — creation entry is excluded:</strong> the backend's
 * repository query uses {@code findByPaymentOrderIdAndActionIsNotNullOrderByCreatedAtAsc},
 * which filters out the creation history entry (whose {@code action} column is {@code null}).
 * A freshly created order with no lifecycle operations returns {@code {"content":[]}} — an
 * empty list, not a single CREATED entry. Only explicit lifecycle actions (AUTHORIZE, CAPTURE,
 * CANCEL, REFUND) appear in this response.
 *
 * <p><strong>Ordering guarantee:</strong> entries are sorted by {@code createdAt ASC}.
 * Because each lifecycle operation writes its history row synchronously within the same
 * {@code @Transactional} method, and HTTP calls are sequential, the order is deterministic.
 *
 * <p><strong>Timestamp as String:</strong> {@code createdAt} is typed as {@code String}
 * (not {@code Instant}) because {@code jackson-datatype-jsr310} is not on the test classpath.
 * Tests assert non-null presence only — no date arithmetic required.
 *
 * <p>SDET note: the {@link StatusHistoryEntry} nested record's field names must exactly match
 * the backend's JSON serialization output. If the backend renames {@code fromStatus} or
 * {@code action}, update here — not at assertion sites.
 */
public record PaymentHistoryResponse(List<StatusHistoryEntry> content) {

    /**
     * A single status-history row.
     *
     * <p>Fields from the backend:
     * <ul>
     *   <li>{@code statusHistoryId} — UUID; unique per row.</li>
     *   <li>{@code paymentOrderId} — UUID; foreign key to the payment order.</li>
     *   <li>{@code fromStatus} — source status name; {@code null} for the creation entry
     *       (which this endpoint does not return).</li>
     *   <li>{@code toStatus} — target status name; never null.</li>
     *   <li>{@code action} — lifecycle action name ({@code AUTHORIZE}, {@code CAPTURE},
     *       {@code CANCEL}, {@code REFUND}); never null in the result set (creation entry
     *       is filtered out).</li>
     *   <li>{@code actorSubject} — JWT {@code sub} claim of the caller.</li>
     *   <li>{@code correlationId} — {@code X-Correlation-ID} from the request context,
     *       propagated by {@code CorrelationIdFilter}.</li>
     *   <li>{@code createdAt} — ISO-8601 instant string; written at row insertion time.</li>
     * </ul>
     */
    public record StatusHistoryEntry(
            UUID statusHistoryId,
            UUID paymentOrderId,
            String fromStatus,
            String toStatus,
            String action,
            String actorSubject,
            String correlationId,
            String createdAt
    ) {}
}
