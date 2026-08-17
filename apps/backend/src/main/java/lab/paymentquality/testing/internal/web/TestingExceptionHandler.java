package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.etl.IncrementalEtlNotReadyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TestController.class)
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
@Profile("!prod")
class TestingExceptionHandler {

    @ExceptionHandler(IncrementalEtlNotReadyException.class)
    ResponseEntity<ProblemDetail> incrementalNotReady(IncrementalEtlNotReadyException ex) {
        return TestingProblems.conflict(ex.getMessage());
    }
}
