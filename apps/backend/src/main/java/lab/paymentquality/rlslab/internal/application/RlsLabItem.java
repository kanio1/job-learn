package lab.paymentquality.rlslab.internal.application;

import java.util.UUID;

public record RlsLabItem(UUID itemId, UUID tenantId, String label, long amountMinor) {
}
