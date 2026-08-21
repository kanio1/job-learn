package lab.paymentquality.iam.internal.domain.exception;

public class DuplicateSavedViewException extends RuntimeException {

    public DuplicateSavedViewException() {
        super("A saved view with this name already exists");
    }
}
