package lab.paymentquality.merchant.internal.web;

public class DuplicateMerchantReferenceException extends RuntimeException {

    private final String conflictingReference;

    public DuplicateMerchantReferenceException(String conflictingReference) {
        super("A merchant with reference '" + conflictingReference + "' already exists");
        this.conflictingReference = conflictingReference;
    }

    public String getConflictingReference() {
        return conflictingReference;
    }
}
