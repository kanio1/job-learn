package lab.paymentquality.iam.internal.domain.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException() {
        super("Username or email already exists");
    }
}
