package lab.paymentquality.payment.internal.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public record RequestFingerprint(String canonicalJson, String fingerprintHash) {

    public static RequestFingerprint of(UUID merchantId, long amountMinor, String currency, String clientOrderReference) {
        String canonical = "{\"operation\":\"POST /api/merchants/{merchantId}/payment-orders\","
                + "\"merchantId\":\"" + merchantId + "\","
                + "\"amountMinor\":" + amountMinor + ","
                + "\"currency\":\"" + currency + "\","
                + "\"clientOrderReference\":\"" + clientOrderReference + "\"}";
        return new RequestFingerprint(canonical, sha256Hex(canonical));
    }

    public static RequestFingerprint forLifecycle(UUID merchantId, UUID paymentOrderId,
                                                   PaymentLifecycleAction action,
                                                   Long amountMinor, String reason) {
        String canonical = "{\"operation\":\"POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/"
                + action.name().toLowerCase() + "\","
                + "\"merchantId\":\"" + merchantId + "\","
                + "\"paymentOrderId\":\"" + paymentOrderId + "\","
                + "\"action\":\"" + action.name() + "\""
                + (amountMinor != null ? ",\"amountMinor\":" + amountMinor : "")
                + (reason != null ? ",\"reason\":\"" + reason + "\"" : "")
                + "}";
        return new RequestFingerprint(canonical, sha256Hex(canonical));
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
