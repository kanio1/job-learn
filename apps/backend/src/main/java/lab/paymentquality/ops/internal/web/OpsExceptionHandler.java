package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.internal.domain.OpsNotificationNotFoundException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {OpsFeedController.class, OpsNotificationController.class})
public class OpsExceptionHandler {

    @ExceptionHandler(OpsNotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(OpsNotificationNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String error, String title, String detail) {
        String correlationId = MDC.get("correlationId");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.payment-quality.local/problems/" + error.replace('_', '-'));
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        if (correlationId != null && !correlationId.isBlank()) {
            body.put("correlationId", correlationId);
        }
        body.put("error", error);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
