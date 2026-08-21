package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class RefundChallengeException extends RuntimeException {

    private final HttpStatus status;
    private final String error;
    private final Instant lockedUntil;

    public RefundChallengeException(HttpStatus status, String error, String message) {
        this(status, error, message, null);
    }

    public RefundChallengeException(HttpStatus status, String error, String message, Instant lockedUntil) {
        super(message);
        this.status = status;
        this.error = error;
        this.lockedUntil = lockedUntil;
    }

    public HttpStatus status() {
        return status;
    }

    public String error() {
        return error;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
