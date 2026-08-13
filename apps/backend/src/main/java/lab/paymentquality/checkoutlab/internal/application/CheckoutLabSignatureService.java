package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabSignatureService {

    private static final Pattern HEADER = Pattern.compile("t=(\\d+),v1=([0-9a-fA-F]+)");
    private static final HexFormat HEX = HexFormat.of();

    private final CheckoutLabProperties properties;
    private final CheckoutLabClock clock;

    public CheckoutLabSignatureService(CheckoutLabProperties properties, CheckoutLabClock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String sign(long epochSeconds, byte[] rawBody) {
        String digest = hexHmac(signedPayload(epochSeconds, rawBody));
        return "t=" + epochSeconds + ",v1=" + digest;
    }

    public CheckoutLabSimulateToken issueSimulateToken(UUID sessionId, Instant validityUntil) {
        Instant now = clock.instant();
        long maxTtl = now.getEpochSecond() + properties.signatureToleranceSeconds();
        long expEpoch = maxTtl;
        if (validityUntil != null) {
            expEpoch = Math.min(expEpoch, validityUntil.getEpochSecond());
        }
        if (expEpoch <= now.getEpochSecond()) {
            throw new InvalidCheckoutSimulateTokenException("Simulate token cannot be issued for an expired session");
        }
        String payload = "simulate." + sessionId + "." + expEpoch;
        String token = hexHmac(payload.getBytes(StandardCharsets.UTF_8));
        return new CheckoutLabSimulateToken(token, Instant.ofEpochSecond(expEpoch));
    }

    public void verifySimulateToken(UUID sessionId, String tokenHeader, Instant sessionValidityUntil) {
        if (tokenHeader == null || tokenHeader.isBlank()) {
            throw new MissingCheckoutSimulateTokenException();
        }
        String token = tokenHeader.trim();
        long now = clock.instant().getEpochSecond();
        long minExp = now - properties.signatureToleranceSeconds();
        long maxExp = now + properties.signatureToleranceSeconds();
        if (sessionValidityUntil != null) {
            long validityExp = sessionValidityUntil.getEpochSecond();
            minExp = Math.min(minExp, validityExp);
            maxExp = Math.max(maxExp, validityExp);
        }
        for (long expEpoch = minExp; expEpoch <= maxExp; expEpoch++) {
            byte[] expected = decodeHex(hexHmac(simulatePayload(sessionId, expEpoch)));
            byte[] provided = decodeSimulateTokenHex(token);
            if (MessageDigest.isEqual(expected, provided)) {
                return;
            }
        }
        throw new InvalidCheckoutSimulateTokenException("Lab-Simulate-Token mismatch or expired");
    }

    private static byte[] simulatePayload(UUID sessionId, long expEpoch) {
        return ("simulate." + sessionId + "." + expEpoch).getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String signatureHeader, byte[] rawBody) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidCheckoutSignatureException("Lab-Signature header is required");
        }
        Matcher matcher = HEADER.matcher(signatureHeader.trim());
        if (!matcher.matches()) {
            throw new InvalidCheckoutSignatureException("Lab-Signature must be t=<epoch>,v1=<hex>");
        }
        long timestamp = Long.parseLong(matcher.group(1));
        byte[] provided = decodeHex(matcher.group(2));
        long now = clock.instant().getEpochSecond();
        if (Math.abs(now - timestamp) > properties.signatureToleranceSeconds()) {
            throw new InvalidCheckoutSignatureException("Lab-Signature timestamp is outside tolerance");
        }
        byte[] expected = decodeHex(hexHmac(signedPayload(timestamp, rawBody)));
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new InvalidCheckoutSignatureException("Lab-Signature HMAC mismatch");
        }
    }

    private byte[] signedPayload(long epochSeconds, byte[] rawBody) {
        byte[] prefix = (epochSeconds + ".").getBytes(StandardCharsets.UTF_8);
        byte[] signed = new byte[prefix.length + rawBody.length];
        System.arraycopy(prefix, 0, signed, 0, prefix.length);
        System.arraycopy(rawBody, 0, signed, prefix.length, rawBody.length);
        return signed;
    }

    private String hexHmac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.hmacSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(payload)).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 is required", ex);
        }
    }

    private static byte[] decodeHex(String hex) {
        try {
            return HEX.parseHex(hex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidCheckoutSignatureException("Lab-Signature v1 is not valid hex");
        }
    }

    private static byte[] decodeSimulateTokenHex(String hex) {
        try {
            return HEX.parseHex(hex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidCheckoutSimulateTokenException("Lab-Simulate-Token is not valid hex");
        }
    }
}
