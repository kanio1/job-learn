package lab.paymentquality.apitest.core.auth;

/**
 * Strategy for producing Bearer tokens for a given test identity.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link #placeholder()} — returns a non-secret placeholder string; safe for offline tests.</li>
 *   <li>{@code KeycloakTokenFactory} (Phase 6) — mints real JWTs against the Testcontainers
 *       Keycloak instance for live API scenarios.</li>
 * </ul>
 *
 * <p>Contract: implementations MUST NOT log the returned token. The token is used only to set
 * the {@code Authorization: Bearer <token>} header; it is never stored in test output.
 *
 * <p>SDET learning: the Strategy pattern decouples persona definition ({@link Identity}) from
 * the token-minting mechanism. Scenarios express <em>who</em> acts; the factory handles <em>how</em>.
 */
@FunctionalInterface
public interface TokenFactory {

    /**
     * Returns a Bearer token string for the given identity.
     * Must not log, persist, or expose the token beyond the HTTP Authorization header.
     */
    String tokenFor(Identity identity);

    /**
     * Placeholder factory: returns a non-secret string safe for offline/wiring tests.
     * Not a real JWT — will cause 401 against a live backend. Replace with
     * {@code KeycloakTokenFactory} in Phase 6.
     */
    static TokenFactory placeholder() {
        return identity -> "placeholder-" + identity.logicalName() + "-not-a-real-token";
    }

    /**
     * No-op factory: throws if called. Use where anonymous identity always prevents invocation,
     * and any accidental call should fail loudly.
     */
    static TokenFactory noOp() {
        return identity -> {
            throw new UnsupportedOperationException(
                    "No TokenFactory configured. Wire KeycloakTokenFactory in Phase 6. " +
                    "Identity: " + identity.logicalName());
        };
    }
}
