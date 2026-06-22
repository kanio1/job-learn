package lab.paymentquality.shared.web;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for HTTP-level exceptions thrown before a specific controller is reached.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to override Spring MVC's built-in handling
 * of HTTP-level exceptions ({@link HttpRequestMethodNotSupportedException},
 * {@link HttpMediaTypeNotSupportedException}, {@link HttpMediaTypeNotAcceptableException})
 * so they are returned as {@code application/problem+json} consistent with the payment API error
 * contract, rather than Spring's default empty-body responses.
 *
 * <p>Controller-specific {@code @RestControllerAdvice(assignableTypes = ...)} handlers take
 * priority for their scoped controllers; this handler covers all other cases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String X_CORRELATION_ID = "X-Correlation-ID";

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpHeaders responseHeaders = problemHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            responseHeaders.setAllow(ex.getSupportedHttpMethods());
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers(responseHeaders)
                .body(problemBody(status.value(), "method_not_allowed", "Method Not Allowed",
                        "HTTP method is not allowed for this resource"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpHeaders responseHeaders = problemHeaders();
        responseHeaders.set("Accept-Patch", "application/merge-patch+json");
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers(responseHeaders)
                .body(problemBody(status.value(), "unsupported_media_type", "Unsupported Media Type",
                        "Content-Type must be application/json or application/merge-patch+json where PATCH is supported"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // Force application/problem+json even when the client didn't accept it.
        // Per the REST API contract, 406 responses must carry problem+json for API clarity.
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers(problemHeaders())
                .body(problemBody(status.value(), "not_acceptable", "Not Acceptable",
                        "Accept header must allow application/json"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpHeaders problemHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        headers.set(X_CORRELATION_ID, correlationId);
        headers.setCacheControl("no-store");
        headers.setVary(List.of("Authorization"));
        return headers;
    }

    private Map<String, Object> problemBody(int status, String error, String title, String detail) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.payment-quality.local/problems/" + error.replace('_', '-'));
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("correlationId", correlationId);
        body.put("error", error);
        return body;
    }
}
