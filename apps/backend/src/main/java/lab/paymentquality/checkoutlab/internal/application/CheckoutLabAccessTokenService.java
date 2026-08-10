package lab.paymentquality.checkoutlab.internal.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabAccessTokenService {

    static final long EXPIRES_IN_SECONDS = 3600L;
    static final String TOKEN_PREFIX = "lab.";

    private final Clock clock;
    private final Map<String, Instant> activeTokens = new ConcurrentHashMap<>();

    CheckoutLabAccessTokenService() {
        this(Clock.systemUTC());
    }

    CheckoutLabAccessTokenService(Clock clock) {
        this.clock = clock;
    }

    String issueAccessToken() {
        String token = TOKEN_PREFIX + UUID.randomUUID();
        activeTokens.put(token, clock.instant().plusSeconds(EXPIRES_IN_SECONDS));
        return token;
    }

    public boolean isActiveAccessToken(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            return false;
        }
        Instant expiresAt = activeTokens.get(token);
        return expiresAt != null && clock.instant().isBefore(expiresAt);
    }

    long expiresInSeconds() {
        return EXPIRES_IN_SECONDS;
    }
}
