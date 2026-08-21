package lab.paymentquality.support.internal.web;

import java.util.UUID;

public record BulkAssignFailure(UUID caseId, String caseReference, String error) {
}
