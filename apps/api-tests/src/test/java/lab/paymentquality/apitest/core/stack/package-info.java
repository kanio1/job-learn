/**
 * Container stack layer — Phase 6 (deferred).
 *
 * <p>Will contain: {@code BackendImage}, {@code PostgresSupport}, {@code KeycloakSupport},
 * {@code BackendSupport}, {@code ApiStack} — Testcontainers lifecycle for PostgreSQL 18,
 * Keycloak 26, and the backend Docker image.
 *
 * <p>Stack starts exactly once per test session via a JUnit {@code BeforeAllCallback} stored
 * in the root extension context store (singleton pattern across test classes).
 *
 * <p>This package is intentionally empty in Phase 1–5 to avoid importing Testcontainers
 * before the BOM dep is uncommented in {@code pom.xml}.
 */
package lab.paymentquality.apitest.core.stack;
