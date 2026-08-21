package lab.paymentquality.support.internal.domain;

import lab.paymentquality.support.SupportCaseStatus;

public class IllegalSupportTransitionException extends RuntimeException {

    private final SupportCaseStatus from;
    private final SupportCaseStatus to;

    public IllegalSupportTransitionException(SupportCaseStatus from, SupportCaseStatus to) {
        super("Cannot transition support case from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public SupportCaseStatus getFrom() {
        return from;
    }

    public SupportCaseStatus getTo() {
        return to;
    }
}
