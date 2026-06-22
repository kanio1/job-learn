package lab.paymentquality.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Component
@Order(2)
public class ApiRequestRejectionFilter extends OncePerRequestFilter {

    private static final String METHOD_OVERRIDE_HEADER = "X-HTTP-Method-Override";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String ERROR_METHOD_OVERRIDE = "method_override_not_allowed";
    private static final String ERROR_TRACE = "trace_not_allowed";
    private static final String API_ALLOWED_METHODS = "GET, HEAD, POST, PATCH, OPTIONS";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("TRACE".equalsIgnoreCase(request.getMethod())) {
            reject(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, ERROR_TRACE,
                    "TRACE is not allowed for API requests", API_ALLOWED_METHODS);
            return;
        }

        if (request.getHeader(METHOD_OVERRIDE_HEADER) != null) {
            reject(response, HttpServletResponse.SC_BAD_REQUEST, ERROR_METHOD_OVERRIDE,
                    "X-HTTP-Method-Override is not allowed for API requests", null);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String error, String message, String allow)
            throws IOException {
        String correlationId = response.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.VARY, "Authorization");
        if (allow != null) {
            response.setHeader(HttpHeaders.ALLOW, allow);
        }

        String body = """
                {"type":"https://api.payment-quality.local/problems/%s","title":"%s","status":%d,"detail":"%s","code":"%s","correlationId":"%s","error":"%s","message":"%s","details":null}"""
                .formatted(error.replace('_', '-'), title(status), status, json(message),
                        error.toUpperCase(Locale.ROOT), json(correlationId), error, json(message));
        response.getWriter().write(body);
    }

    private String title(int status) {
        return status == HttpServletResponse.SC_METHOD_NOT_ALLOWED ? "Method Not Allowed" : "Bad Request";
    }

    private String json(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
