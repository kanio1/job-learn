package lab.paymentquality.apitest.api.payment.dto;

import java.util.UUID;

/**
 * Test-side representation of a payment-order evidence metadata body.
 *
 * <p>Mirrors the backend {@code PaymentEvidenceResponse} record without importing backend
 * classes. {@code storageKey} is persisted server-side but is not serialized in JSON.
 *
 * <p>{@code uploadedAt} is a {@code String} because {@code jackson-datatype-jsr310} is not on
 * the api-tests classpath.
 */
public record PaymentEvidenceResponse(
        UUID evidenceId,
        UUID paymentOrderId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String uploadedAt) {
}
