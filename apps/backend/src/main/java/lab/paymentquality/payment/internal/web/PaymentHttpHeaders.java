package lab.paymentquality.payment.internal.web;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

// Feature 011 helper: keeps payment resource cache, vary and correlation headers consistent across success and error responses.
public final class PaymentHttpHeaders {

    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String ACCEPT_PATCH = "Accept-Patch";
    public static final String MERGE_PATCH_JSON = "application/merge-patch+json";
    public static final String VARY_AUTHORIZATION = "Authorization";
    public static final String VARY_AUTHORIZATION_IF_MATCH = "Authorization, If-Match";
    public static final String VARY_AUTHORIZATION_IDEMPOTENCY_KEY = "Authorization, Idempotency-Key";

    private PaymentHttpHeaders() {
    }

    public static String correlationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    public static HttpHeaders sensitivePaymentHeaders(String vary) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        headers.setVary(java.util.List.of(vary.split(", ")));
        headers.set(X_CORRELATION_ID, correlationId());
        return headers;
    }

    public static <T> ResponseEntity.BodyBuilder sensitivePaymentResponse(ResponseEntity.BodyBuilder builder, String vary) {
        return builder
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .varyBy(vary.split(", "))
                .header(X_CORRELATION_ID, correlationId());
    }
}
