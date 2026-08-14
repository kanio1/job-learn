package lab.paymentquality.rlslab.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.rlslab.internal.application.RlsLabProblemException;
import lab.paymentquality.tenant.TenantResolutionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = RlsLabController.class)
class RlsLabExceptionHandler {

    @ExceptionHandler(RlsLabProblemException.class)
    ResponseEntity<Map<String, Object>> handle(RlsLabProblemException ex, HttpServletRequest request) {
        return problem(ex.status(), ex.error(), ex.getMessage(), request);
    }

    @ExceptionHandler(TenantResolutionException.class)
    ResponseEntity<Map<String, Object>> handleTenant(TenantResolutionException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "tenant_access_denied", ex.getMessage(), request);
    }

    private static ResponseEntity<Map<String, Object>> problem(
            HttpStatus status, String error, String detail, HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Map.of(
                        "type", "https://api.payment-quality.local/problems/" + error.replace('_', '-'),
                        "title", status.getReasonPhrase(),
                        "status", status.value(),
                        "detail", detail,
                        "error", error,
                        "correlationId", correlationId));
    }
}
