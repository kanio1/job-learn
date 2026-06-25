package lab.paymentquality.apitest.api.payment.dto;

import java.util.List;

/**
 * Local DTO for {@code GET /api/merchants/{merchantId}/payment-orders/summary} response.
 *
 * <p>Mirrors the backend's {@code PaymentOrderSummaryResponse} record without importing it.
 * Field names must match the JSON keys exactly:
 * <ul>
 *   <li>{@code totalOrders} — count of all payment orders matching the filter.</li>
 *   <li>{@code totalAmountMinor} — sum of {@code amountMinor} across all matching orders.</li>
 *   <li>{@code byCurrency} — breakdown per ISO-4217 currency code; ordered {@code ASC} by
 *       currency string (backend: {@code ORDER BY po.currency ASC}).</li>
 *   <li>{@code byStatus} — breakdown per lifecycle status; ordered {@code ASC} by status
 *       string (backend: {@code ORDER BY po.status ASC}).</li>
 * </ul>
 *
 * <p>Phase 8B — black-box; no backend imports.
 */
public record PaymentSummaryResponse(
        long totalOrders,
        long totalAmountMinor,
        List<CurrencySummary> byCurrency,
        List<StatusSummary> byStatus
) {

    /**
     * Per-currency aggregate for one currency code.
     *
     * @param currency        ISO-4217 code (PLN / EUR / USD).
     * @param orderCount      number of matching orders in this currency.
     * @param totalAmountMinor sum of {@code amountMinor} for this currency.
     */
    public record CurrencySummary(
            String currency,
            long orderCount,
            long totalAmountMinor
    ) {}

    /**
     * Per-status aggregate for one lifecycle status.
     *
     * @param status          one of CREATED / AUTHORIZED / CAPTURED / CANCELLED / REFUNDED.
     * @param orderCount      number of matching orders in this status.
     * @param totalAmountMinor sum of {@code amountMinor} for this status.
     */
    public record StatusSummary(
            String status,
            long orderCount,
            long totalAmountMinor
    ) {}
}
