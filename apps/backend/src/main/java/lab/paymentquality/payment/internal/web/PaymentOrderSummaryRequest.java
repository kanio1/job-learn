package lab.paymentquality.payment.internal.web;

public record PaymentOrderSummaryRequest(
        String currency,
        String status,
        String fromDate,
        String toDate
) {
}
