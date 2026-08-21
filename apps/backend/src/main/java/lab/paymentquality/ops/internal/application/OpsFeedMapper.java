package lab.paymentquality.ops.internal.application;

import lab.paymentquality.ops.OpsFeedFrame;
import lab.paymentquality.shared.events.AuditableActionOccurred;

import java.util.Map;
import java.util.UUID;

final class OpsFeedMapper {

    private OpsFeedMapper() {
    }

    static OpsFeedFrame fromAudit(AuditableActionOccurred event) {
        if (event == null || event.action() == null || event.action().isBlank()) {
            return null;
        }
        String type = event.action();
        UUID merchantId = uuidFrom(event.afterState(), "merchantId");
        UUID paymentOrderId = "PAYMENT_ORDER".equals(event.targetType())
                ? parseUuid(event.targetId())
                : uuidFrom(event.afterState(), "paymentOrderId");
        return new OpsFeedFrame(
                UUID.randomUUID(),
                event.occurredAt(),
                merchantId,
                paymentOrderId,
                type,
                labelOf(event, type));
    }

    static boolean actionableInboxType(String type) {
        if (type == null) {
            return false;
        }
        return "PAYMENT_FAILED".equals(type)
                || "REFUND_APPROVAL_NEEDED".equals(type)
                || "SUPPORT_CASE_ASSIGNED".equals(type);
    }

    private static String labelOf(AuditableActionOccurred event, String type) {
        String reference = stringFrom(event.afterState(), "clientOrderReference");
        if (reference == null) {
            reference = stringFrom(event.afterState(), "caseReference");
        }
        String status = stringFrom(event.afterState(), "status");
        if (reference != null && status != null) {
            return reference + "  " + status;
        }
        if (reference != null) {
            return reference + "  " + type;
        }
        return type;
    }

    private static UUID uuidFrom(Map<String, Object> state, String key) {
        return parseUuid(stringFrom(state, key));
    }

    private static String stringFrom(Map<String, Object> state, String key) {
        if (state == null) {
            return null;
        }
        Object value = state.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).strip();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
