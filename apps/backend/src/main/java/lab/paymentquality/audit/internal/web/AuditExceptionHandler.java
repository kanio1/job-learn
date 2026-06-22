package lab.paymentquality.audit.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.audit.internal.domain.exception.AuditEventNotFoundException;
import lab.paymentquality.tenant.TenantResolutionException;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = AuditController.class)
public class AuditExceptionHandler {

    private static final String PROBLEM_BASE = "https://api.payment-quality.local/problems/";
    private static final String X_CORRELATION_ID = "X-Correlation-ID";

    @ExceptionHandler(AuditEventNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", "Audit event not found", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleValidation(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", "Invalid audit query", request);
    }

    @ExceptionHandler({TenantResolutionException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDetail> handleForbidden(HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied", request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String error,
            String title,
            String detail,
            HttpServletRequest request) {
        String correlationId = correlationId();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(PROBLEM_BASE + error.replace('_', '-')));
        body.setTitle(title);
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("code", error.toUpperCase());
        body.setProperty("correlationId", correlationId);
        body.setProperty("error", error);
        body.setProperty("message", detail);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .varyBy("Authorization")
                .header(X_CORRELATION_ID, correlationId)
                .body(body);
    }

    private String correlationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }
}
