/**
 * Test data and value-object helpers — Phase 5.
 *
 * <p>Provides readable, collision-resistant generators for correlation IDs, idempotency keys,
 * unique entity references, ETag values, and the {@code Versioned<T>} result wrapper.
 *
 * <p>All generators are stateless utilities producing strings safe for HTTP headers and JSON.
 * No secrets, no PII, no tokens are generated here.
 *
 * <p>SDET concept: deterministic-enough but non-repeating test data prevents collisions in
 * parallel runs while keeping failure output readable during debugging.
 */
package lab.paymentquality.apitest.core.data;
