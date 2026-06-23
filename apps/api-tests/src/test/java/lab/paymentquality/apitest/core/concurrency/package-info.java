/**
 * Concurrency and conditional-request helpers — Phase 5.
 *
 * <p>Contains {@code ETag} (value object for quoted ETags from the payment API) and
 * {@code Versioned<T>} (result wrapper pairing a response body with its ETag).
 *
 * <p>{@code ConcurrencyHarness} (parallel conflict testing) is deferred to Phase 7+.
 *
 * <p>SDET concept: the payment lifecycle API is fully ETag-gated — every state-mutating action
 * requires {@code If-Match} with the current ETag. {@code Versioned<T>} makes this contract
 * explicit in method return types: callers <em>cannot</em> forget to propagate the ETag.
 */
package lab.paymentquality.apitest.core.concurrency;
