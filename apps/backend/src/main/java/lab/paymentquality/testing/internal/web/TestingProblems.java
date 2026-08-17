package lab.paymentquality.testing.internal.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;

final class TestingProblems {

    static final String BASE = "https://api.payment-quality.local/problems/";

    private TestingProblems() {
    }

    static ResponseEntity<ProblemDetail> conflict(String detail) {
        return problem(HttpStatus.CONFLICT, "conflict", "CONFLICT", "Conflict", detail);
    }

    static ResponseEntity<ProblemDetail> validation(String detail) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "VALIDATION", "Bad Request", detail);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String error,
            String code,
            String title,
            String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(BASE + error));
        body.setTitle(title);
        body.setProperty("error", error);
        body.setProperty("code", code);
        body.setProperty("message", detail);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
