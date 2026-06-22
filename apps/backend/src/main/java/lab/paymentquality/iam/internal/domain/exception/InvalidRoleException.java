package lab.paymentquality.iam.internal.domain.exception;

public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException() {
        super("One or more roles are invalid");
    }
}
