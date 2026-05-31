package lab.paymentquality.payment.internal.web;

import java.util.List;

public record PaymentOrderSummaryResponse(
        long totalOrders,
        long totalAmountMinor,
        List<CurrencySummary> byCurrency,
        List<StatusSummary> byStatus
) {

    public record CurrencySummary(
            String currency,
            long orderCount,
            long totalAmountMinor
    ) {
    }

    public record StatusSummary(
            String status,
            long orderCount,
            long totalAmountMinor
    ) {
    }
}
