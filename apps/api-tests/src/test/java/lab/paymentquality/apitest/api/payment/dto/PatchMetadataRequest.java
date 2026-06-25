package lab.paymentquality.apitest.api.payment.dto;

import java.util.Map;

/**
 * Test-side request DTO for
 * {@code PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}.
 *
 * <p>The backend's {@code MetadataPatchRequest} accepts only the {@code metadata} top-level field
 * (a {@code Map<String, String>} persisted as a JSON string column). Any additional top-level
 * field captured via {@code @JsonAnySetter} triggers
 * {@code UnknownMetadataPatchFieldException} → 400 {@code unknown_top_level_field}.
 *
 * <p>Content-Type must be {@code application/merge-patch+json} (or {@code application/json}).
 * The PATCH endpoint is intentionally permissive about media types because some HTTP clients
 * cannot send custom content-types; the backend treats both identically.
 *
 * <p>Phase 8E: JSON Merge Patch contract.
 */
public record PatchMetadataRequest(Map<String, String> metadata) {
}
