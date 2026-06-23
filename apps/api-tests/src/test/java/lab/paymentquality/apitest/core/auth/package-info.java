/**
 * Auth layer — Phase 3.
 *
 * <p>Defines API actors (personas) as {@link lab.paymentquality.apitest.core.auth.Identity}
 * objects and the {@link lab.paymentquality.apitest.core.auth.TokenFactory} strategy for
 * producing Bearer tokens. Standard personas live in
 * {@link lab.paymentquality.apitest.core.auth.Identities}.
 *
 * <p>Real token minting against Keycloak is deferred to Phase 6/7 ({@code KeycloakTokenFactory}).
 * Placeholder implementations allow offline compile and wiring tests.
 *
 * <p>SDET concept: decoupling persona definition from token minting lets test scenarios express
 * <em>who</em> is acting without worrying about <em>how</em> a token is obtained.
 */
package lab.paymentquality.apitest.core.auth;
