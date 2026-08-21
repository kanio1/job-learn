package lab.paymentquality.support.internal.web;

public class DuplicateSupportCaseReferenceException extends RuntimeException {
    public DuplicateSupportCaseReferenceException(String caseReference) {
        super("Support case already exists: " + caseReference);
    }
}
