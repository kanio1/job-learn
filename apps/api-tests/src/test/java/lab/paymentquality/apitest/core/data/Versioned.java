package lab.paymentquality.apitest.core.data;

import java.util.Objects;

/**
 * Result wrapper pairing a response body with its ETag.
 *
 * <p>Use when both the deserialized body and the ETag are needed after a request:
 * <pre>{@code
 *   Versioned<PaymentOrderResponse> created = paymentsApi.create(merchantId, body, key);
 *   // Pass created.etag().raw() to subsequent lifecycle actions:
 *   Versioned<PaymentOrderResponse> authorized = paymentsApi.authorize(
 *       merchantId, created.body().paymentOrderId(),
 *       RequestSpecs.lifecycle(created.etag().raw(), IdempotencyKeys.generate("auth"))
 *   );
 * }</pre>
 *
 * <p>SDET learning: the {@code Versioned<T>} return type makes the ETag contract explicit in
 * the API client interface. A caller who forgets to propagate the ETag cannot compile — the
 * type system enforces the protocol. Compare with returning a plain DTO where ETag is silently lost.
 *
 * @param <T> the deserialized response body type
 */
public record Versioned<T>(T body, ETag etag) {

    public Versioned {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(etag, "etag");
    }

    /** Convenience factory. */
    public static <T> Versioned<T> of(T body, ETag etag) {
        return new Versioned<>(body, etag);
    }

    /** Convenience factory from a raw ETag string (as returned by {@code response.header("ETag")}). */
    public static <T> Versioned<T> of(T body, String rawEtag) {
        return new Versioned<>(body, ETag.of(rawEtag));
    }
}
