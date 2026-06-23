package lab.paymentquality.apitest.core.auth;

import java.util.List;
import java.util.Objects;

/**
 * Represents an API actor (persona) for test scenarios.
 *
 * <p>An identity has a logical name (for logging/debugging), a flag indicating whether it is
 * anonymous (no Authorization header), optional Keycloak realm roles, and an optional
 * tenant/merchant scope hint for multi-tenant scenarios.
 *
 * <p>The token is not stored in the identity itself — it is produced on demand by calling
 * {@link #token()}, which delegates to the embedded {@link TokenFactory}.
 * This keeps the identity value-safe: no secret is embedded in a field visible in stack traces.
 *
 * <p>SDET learning: embedding the strategy in the identity object means a scenario only needs
 * to say "act as PLATFORM_ADMIN" — the token mechanism is invisible to the test method.
 */
public final class Identity {

    private final String logicalName;
    private final boolean anonymous;
    private final List<String> roles;
    private final String tenantId;
    private final TokenFactory tokenFactory;

    private Identity(String logicalName, boolean anonymous, List<String> roles,
                     String tenantId, TokenFactory tokenFactory) {
        this.logicalName = Objects.requireNonNull(logicalName, "logicalName");
        this.anonymous = anonymous;
        this.roles = List.copyOf(roles != null ? roles : List.of());
        this.tenantId = tenantId;
        this.tokenFactory = tokenFactory;
    }

    /** Creates the anonymous identity — no Authorization header will be added. */
    public static Identity anonymous() {
        return new Identity("anonymous", true, List.of(), null, TokenFactory.noOp());
    }

    /** Creates a named non-anonymous identity with the given token factory. */
    public static Identity of(String name, TokenFactory tokenFactory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tokenFactory, "tokenFactory");
        return new Identity(name, false, List.of(), null, tokenFactory);
    }

    /** Creates a named identity with roles and a tenant scope hint. */
    public static Identity of(String name, List<String> roles, String tenantId, TokenFactory tokenFactory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tokenFactory, "tokenFactory");
        return new Identity(name, false, roles, tenantId, tokenFactory);
    }

    /**
     * Produces a Bearer token for this identity.
     * Must not be called on anonymous identities — {@link AuthFilter} checks {@link #isAnonymous()}
     * before calling this method.
     */
    public String token() {
        if (anonymous) {
            throw new UnsupportedOperationException(
                    "Anonymous identity has no token. Check isAnonymous() before calling token().");
        }
        return tokenFactory.tokenFor(this);
    }

    public String logicalName() { return logicalName; }
    public boolean isAnonymous() { return anonymous; }
    public List<String> roles() { return roles; }
    public String tenantId() { return tenantId; }

    @Override
    public String toString() {
        return "Identity{name='" + logicalName + "', anonymous=" + anonymous + "}";
    }
}
