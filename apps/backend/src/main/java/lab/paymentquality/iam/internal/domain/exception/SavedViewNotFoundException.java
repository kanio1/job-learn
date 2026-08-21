package lab.paymentquality.iam.internal.domain.exception;

public class SavedViewNotFoundException extends RuntimeException {

    public SavedViewNotFoundException() {
        super("Saved view not found");
    }
}
