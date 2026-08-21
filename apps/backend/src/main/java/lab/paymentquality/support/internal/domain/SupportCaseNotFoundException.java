package lab.paymentquality.support.internal.domain;

public class SupportCaseNotFoundException extends RuntimeException {
    public SupportCaseNotFoundException(String caseId) {
        super("Support case not found: " + caseId);
    }
}
