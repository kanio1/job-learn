package lab.paymentquality.testsupport;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TestJwtSupport {

    public static final String ISSUER = "http://localhost:9000/test-issuer";
    public static final String EXPECTED_AZP = "payment-quality-dashboard";
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtSupport() {
    }

    public static String platformOperatorToken() {
        // Uses TENANT_ALPHA — PLACEHOLDER_TENANT_ID is SUSPENDED (V0.2 migration)
        // and TenantResolver rejects SUSPENDED non-platform tenants with 403.
        return tokenWithRolesAndTenantId("platform.operator", List.of(
                "merchants:create",
                "merchants:read",
                "merchants:update-status"),
                "TENANT_ALPHA");
    }

    public static String deniedToken() {
        return tokenWithRoles("merchant.denied", List.of());
    }

    public static String tokenWithRoles(String subject, List<String> roles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("azp", EXPECTED_AZP)
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build(),
                    claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test JWT", e);
        }
    }

    public static String expiredToken() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject("expired.operator")
                    .issueTime(Date.from(now.minusSeconds(7200)))
                    .expirationTime(Date.from(now.minusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", List.of("merchants:read")))
                    .claim("azp", EXPECTED_AZP)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create expired test JWT", e);
        }
    }

    public static String invalidIssuerToken() {
        return tokenWithIssuer("invalid-issuer", "platform.operator", List.of("merchants:read"));
    }

    public static String invalidSignatureToken() {
        return platformOperatorToken() + "tampered";
    }

    private static String tokenWithIssuer(String issuer, String subject, List<String> roles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("azp", EXPECTED_AZP)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create invalid issuer test JWT", e);
        }
    }

    public static String merchantPaymentCreatorToken(String merchantId) {
        return tokenWithRolesAndMerchantId("merchant.payment.creator",
                List.of("merchant:payments:create"), merchantId);
    }

    public static String merchantPaymentReaderToken(String merchantId) {
        return tokenWithRolesAndMerchantId("merchant.payment.reader",
                List.of("merchant:payments:read"), merchantId);
    }

    public static String merchantPaymentOperatorToken(String merchantId) {
        return tokenWithRolesAndMerchantId("merchant.payment.operator",
                List.of("merchant:payments:operate"), merchantId);
    }

    public static String merchantPaymentLifecycleToken(String merchantId) {
        return tokenWithRolesAndMerchantId("merchant.payment.lifecycle",
                List.of("merchant:payments:lifecycle"), merchantId);
    }

    public static String platformPaymentReaderToken() {
        return tokenWithRoles("platform.payment.reader", List.of("platform:payments:read"));
    }

    public static String merchantPaymentReaderTokenWithoutMerchantIdClaim() {
        return tokenWithRoles("merchant.payment.reader.no-claim", List.of("merchant:payments:read"));
    }

    public static String merchantScopedDeniedToken(String merchantId) {
        return tokenWithRolesAndMerchantId("merchant.denied", List.of(), merchantId);
    }

    private static String tokenWithRolesAndMerchantId(String subject, List<String> roles, String merchantId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("merchant_id", merchantId)
                    .claim("azp", EXPECTED_AZP)
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build(),
                    claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create merchant-scoped test JWT", e);
        }
    }

    public static String tokenWithRolesAndTenantId(String subject, List<String> roles, String tenantId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("tenant_id", tenantId)
                    .claim("azp", EXPECTED_AZP)
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build(),
                    claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create tenant-scoped test JWT", e);
        }
    }

    public static String tokenWithRolesTenantIdAndMerchantId(String subject, List<String> roles,
                                                              String tenantId, String merchantId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("tenant_id", tenantId)
                    .claim("merchant_id", merchantId)
                    .claim("azp", EXPECTED_AZP)
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build(),
                    claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create tenant+merchant scoped test JWT", e);
        }
    }

    public static String platformAdminToken() {
        return tokenWithRolesAndTenantId("platform.admin",
                List.of("merchants:create", "merchants:read", "merchants:update-status",
                        "merchants:update-risk-flag",
                        "platform:payments:read", "platform:payments:lifecycle", "platform:payments:audit",
                        "platform:payments:notes:read", "platform:payments:notes:create",
                        "platform:tenant:settings:read", "platform:tenant:settings:update"),
                "PLATFORM_TENANT");
    }

    public static String supportAgentToken() {
        return tokenWithRolesAndTenantId("support.agent",
                List.of("platform:payments:read", "platform:payments:audit",
                        "platform:audit:read",
                        "platform:payments:notes:read", "platform:payments:notes:create"),
                "PLATFORM_TENANT");
    }

    public static String platformUserAdminToken() {
        return tokenWithRolesAndTenantId("platform.user.admin",
                List.of("platform:users:read", "platform:users:create", "platform:users:update",
                        "platform:users:assign-roles"),
                "PLATFORM_TENANT");
    }

    public static String tenantAdminToken() {
        return tokenWithRolesAndTenantId("tenant.admin",
                List.of("merchants:create", "merchants:read", "merchants:update-status",
                        "merchant:payments:read"),
                "TENANT_ALPHA");
    }

    public static String tenantUserAdminToken() {
        return tokenWithRolesAndTenantId("tenant.user.admin",
                List.of("tenant:users:read", "tenant:users:create", "tenant:users:update",
                        "tenant:users:assign-roles"),
                "TENANT_ALPHA");
    }

    public static String tokenWithoutTenantClaim() {
        // For the "no tenant_id claim → 403" scenario
        return tokenWithRoles("no.tenant.claim", List.of("merchants:read"));
    }

    public static String tokenWithWrongAuthorizedParty() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject("wrong.azp.client")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("realm_access", Map.of("roles", List.of("merchants:read")))
                    .claim("azp", "some-other-client")
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build(),
                    claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create wrong-azp test JWT", e);
        }
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test RSA key pair", e);
        }
    }
}
