package lab.paymentquality.apitest.core.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Offline unit tests for {@link KeycloakTokenFactory}.
 *
 * <p>No containers. No network. Validates constructor guards, {@code forUser()} contract,
 * and {@link KeycloakTokenFactory.CachedToken} expiry logic (visible in same package).
 *
 * <p>SDET learning: decoupling the factory from Keycloak allows fast feedback on expiry
 * business logic without a live OIDC server.
 */
class KeycloakTokenFactoryTest {

    private static final String DUMMY_ENDPOINT =
            "http://localhost:9999/realms/test/protocol/openid-connect/token";

    // ── Constructor ──────────────────────────────────────────────────────────

    @Test
    void constructor_throwsOnNullEndpoint() {
        assertThatThrownBy(() -> new KeycloakTokenFactory(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── forUser ──────────────────────────────────────────────────────────────

    @Test
    void forUser_returnsNonNullTokenFactory() {
        var factory = new KeycloakTokenFactory(DUMMY_ENDPOINT);

        assertThat(factory.forUser("user", "pass")).isNotNull();
    }

    @Test
    void forUser_throwsOnNullUsername() {
        var factory = new KeycloakTokenFactory(DUMMY_ENDPOINT);

        assertThatThrownBy(() -> factory.forUser(null, "pass"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void forUser_throwsOnNullPassword() {
        var factory = new KeycloakTokenFactory(DUMMY_ENDPOINT);

        assertThatThrownBy(() -> factory.forUser("user", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void forUser_returnsDistinctInstancesPerUser() {
        var factory = new KeycloakTokenFactory(DUMMY_ENDPOINT);

        TokenFactory tf1 = factory.forUser("user1", "pass1");
        TokenFactory tf2 = factory.forUser("user2", "pass2");

        assertThat(tf1).isNotSameAs(tf2);
    }

    // ── CachedToken expiry logic ─────────────────────────────────────────────

    @Test
    void cachedToken_isValid_whenFarFromExpiry() {
        var cached = new KeycloakTokenFactory.CachedToken("tok", Instant.now().plusSeconds(300));

        assertThat(cached.isValid()).isTrue();
    }

    @Test
    void cachedToken_isNotValid_whenAlreadyExpired() {
        var cached = new KeycloakTokenFactory.CachedToken("tok", Instant.now().minusSeconds(1));

        assertThat(cached.isValid()).isFalse();
    }

    @Test
    void cachedToken_isNotValid_whenWithin30SecondsOfExpiry() {
        // 20 seconds is within the 30-second safety margin
        var cached = new KeycloakTokenFactory.CachedToken("tok", Instant.now().plusSeconds(20));

        assertThat(cached.isValid()).isFalse();
    }

    @Test
    void cachedToken_isValid_justOutsideSafetyMargin() {
        // 31 seconds: just outside the 30-second margin → still valid
        var cached = new KeycloakTokenFactory.CachedToken("tok", Instant.now().plusSeconds(31));

        assertThat(cached.isValid()).isTrue();
    }
}
