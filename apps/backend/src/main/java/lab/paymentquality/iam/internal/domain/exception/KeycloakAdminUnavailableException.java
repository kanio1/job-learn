package lab.paymentquality.iam.internal.domain.exception;

public class KeycloakAdminUnavailableException extends RuntimeException {

    public KeycloakAdminUnavailableException() {
        super("Identity provider administration is unavailable");
    }

    public KeycloakAdminUnavailableException(Throwable cause) {
        super("Identity provider administration is unavailable", cause);
    }
}
