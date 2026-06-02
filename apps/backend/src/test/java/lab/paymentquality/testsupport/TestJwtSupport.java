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
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtSupport() {
    }

    public static String platformOperatorToken() {
        return tokenWithRoles("platform.operator", List.of(
                "merchants:create",
                "merchants:read",
                "merchants:update-status"));
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
