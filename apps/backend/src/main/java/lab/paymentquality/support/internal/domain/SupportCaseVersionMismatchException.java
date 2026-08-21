package lab.paymentquality.support.internal.domain;

public class SupportCaseVersionMismatchException extends RuntimeException {
    public SupportCaseVersionMismatchException() {
        super("Support case was modified after you loaded it. Reload and retry.");
    }
}
