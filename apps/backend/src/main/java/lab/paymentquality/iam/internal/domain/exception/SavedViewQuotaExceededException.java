package lab.paymentquality.iam.internal.domain.exception;

public class SavedViewQuotaExceededException extends RuntimeException {

    public SavedViewQuotaExceededException() {
        super("Saved view quota of 20 exceeded");
    }
}
