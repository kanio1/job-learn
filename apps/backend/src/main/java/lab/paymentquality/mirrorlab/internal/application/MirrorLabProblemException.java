package lab.paymentquality.mirrorlab.internal.application;

import org.springframework.http.HttpStatus;

public class MirrorLabProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public MirrorLabProblemException(HttpStatus status, String error, String message) {
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
