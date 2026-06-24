package lab.paymentquality.apitest.api.payment.dto;

import java.util.List;

/**
 * Test-side representation of the paginated payment order list response body.
 *
 * <p>Mirrors the backend's {@code PaymentOrderListResponse} record. The JSON shape is:
 * <pre>{@code
 * {
 *   "content":       [ { ...PaymentOrderResponse... }, ... ],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 104,
 *   "totalPages":    6
 * }
 * }</pre>
 *
 * <p>The field name is {@code content} (not {@code payments} or {@code items}) — this is a
 * deliberate API design choice matching Spring Data's {@code Page} serialization convention.
 * Contract tests that assume a different envelope name will fail silently if they assert on
 * the wrong field. Always read the actual backend response shape.
 *
 * <p>SDET note: deserialization of nested {@code PaymentOrderResponse} records works because
 * Jackson maps JSON arrays to {@code List<T>} using the concrete type. Timestamps in nested
 * records are still typed as {@code String} per the offline classpath constraint.
 */
public record PaymentOrderListResponse(
        List<PaymentOrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
