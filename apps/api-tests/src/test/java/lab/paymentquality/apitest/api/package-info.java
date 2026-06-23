/**
 * Per-resource API client layer — Phase 6 (deferred).
 *
 * <p>Will contain thin API clients: {@code StatusApi}, {@code SeedApi}, {@code MerchantsApi},
 * {@code PaymentOrdersApi}, {@code AuditApi}. Each client wraps REST Assured calls through
 * {@code RequestSpecs} and returns {@code Versioned<T>}, {@code Response}, or local DTOs.
 *
 * <p>Sub-packages {@code merchant/dto}, {@code payment/dto} hold local test-side records —
 * never imported from the backend application classpath.
 *
 * <p>Clients are skeletons (compile-safe {@code UnsupportedOperationException}) until
 * Phase 6 wires the live Testcontainers stack.
 */
package lab.paymentquality.apitest.api;
