package lab.paymentquality.rlslab.internal.application;

import org.springframework.http.HttpStatus;

public class RlsLabProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public RlsLabProblemException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus status() {
        return status;
    }

    public String error() {
        return error;
    }
}
