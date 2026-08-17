package lab.paymentquality.testing.internal.seed;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataLearningTruth(
        int tenants,
        int merchants,
        int payments,
        int paymentHistoryRows,
        int capturedPayments,
        int refundedPayments,
        int cancelledPayments,
        int authorizedPayments,
        int expiredPayments,
        int createdPayments,
        int tenantAlphaPayments,
        int checkoutSessions,
        int checkoutEvents,
        int checkoutFulfillments,
        int checkoutAnomalies,
        int auditEvents,
        int publicationEvents,
        @JsonProperty("failedPublications") int incompletePublications,
        long totalAmountMinor,
        long totalCapturedAmountMinor,
        long totalRefundedAmountMinor,
        int paymentsPln,
        int paymentsEur,
        int paymentsUsd
) {
}
