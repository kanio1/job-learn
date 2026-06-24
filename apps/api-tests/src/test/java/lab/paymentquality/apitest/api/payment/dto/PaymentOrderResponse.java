package lab.paymentquality.apitest.api.payment.dto;

import java.util.UUID;

/**
 * Test-side representation of a single payment order response body.
 *
 * <p>Mirrors the backend's {@code PaymentOrderResponse} record without importing backend classes.
 * REST Assured / Jackson deserializes the JSON body into this record.
 *
 * <p><strong>Timestamp fields as String:</strong> {@code createdAt}, {@code updatedAt} and all
 * lifecycle timestamps ({@code authorizedAt}, {@code capturedAt}, etc.) are typed as {@code String}
 * because {@code jackson-datatype-jsr310} is not on the test classpath (offline Maven constraint).
 * Contract assertions verify non-null presence and ISO-8601 format as strings — no date arithmetic
 * is needed at this phase.
 *
 * <p><strong>Optional fields:</strong> most lifecycle timestamps and reasons are {@code null}
 * for CREATED-status orders; non-null only for orders that have progressed through the lifecycle.
 * Test assertions must match the expected status (e.g. a CREATED order has null {@code authorizedAt}).
 *
 * <p>SDET note: field names must exactly match the backend's JSON serialization. If the backend
 * renames a field, the DTO must be updated here — and only here, not at every assertion site.
 */
public record PaymentOrderResponse(
        UUID paymentOrderId,
        UUID merchantId,
        String clientOrderReference,
        long amountMinor,
        String currency,
        String status,
        Long capturedAmountMinor,
        Long refundedAmountMinor,
        String authorizedAt,
        String expiresAt,
        String capturedAt,
        String cancelledAt,
        String refundedAt,
        String cancellationReason,
        String refundReason,
        String metadata,
        String createdAt,
        String updatedAt) {
}
