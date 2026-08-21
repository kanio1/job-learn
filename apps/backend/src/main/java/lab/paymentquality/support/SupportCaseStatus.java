package lab.paymentquality.support;

public enum SupportCaseStatus {
    NEW,
    IN_PROGRESS,
    WAITING,
    RESOLVED;

    public boolean canTransitionTo(SupportCaseStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case NEW -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == WAITING || target == RESOLVED;
            case WAITING -> target == IN_PROGRESS || target == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
