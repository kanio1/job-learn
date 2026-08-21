package lab.paymentquality.support.internal.domain;

import java.util.UUID;

public class SupportCaseAlreadyResolvedException extends RuntimeException {

    private final UUID caseId;
    private final String caseReference;

    public SupportCaseAlreadyResolvedException(UUID caseId, String caseReference) {
        super("Support case is already resolved: " + caseReference);
        this.caseId = caseId;
        this.caseReference = caseReference;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseReference() {
        return caseReference;
    }
}
