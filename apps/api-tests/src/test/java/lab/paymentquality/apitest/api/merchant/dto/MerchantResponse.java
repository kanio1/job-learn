package lab.paymentquality.apitest.api.merchant.dto;

import java.util.UUID;

/**
 * Test-side representation of a single merchant response body.
 *
 * <p>Mirrors the backend's {@code MerchantResponse} record without importing backend classes.
 * All fields nullable — REST Assured / Jackson deserializes JSON into this record.
 *
 * <p>Fields: {@code merchantId} (UUID), {@code merchantReference} (unique business key),
 * {@code displayName}, {@code status} (DRAFT | ACTIVE | SUSPENDED),
 * {@code createdAt}, {@code updatedAt} (ISO-8601 instant strings), and
 * {@code riskFlagged} (the current merchant risk marker).
 *
 * <p>Timestamps are typed as {@code String} rather than {@code java.time.Instant} because
 * {@code jackson-datatype-jsr310} is not on the test classpath (offline Maven constraint).
 * Contract assertions only need non-null presence, not date arithmetic, so {@code String}
 * is sufficient here.
 *
 * <p>SDET note: this local record decouples the test layer from backend implementation.
 * If the backend renames a field, only this record changes — not every assertion site.
 */
public record MerchantResponse(
        UUID merchantId,
        String merchantReference,
        String displayName,
        String status,
        String createdAt,
        String updatedAt,
        boolean riskFlagged) {
}
