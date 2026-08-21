package lab.paymentquality.payment.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record PaymentOrderListRequest(
        @Pattern(regexp = "CREATED|AUTHORIZED|CAPTURED|CANCELLED|EXPIRED|REFUNDED",
                message = "status must be CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, or REFUNDED")
        String status,

        @Pattern(regexp = "PLN|EUR|USD", message = "currency must be PLN, EUR, or USD")
        String currency,

        String fromDate,
        String toDate,

        @PositiveOrZero(message = "minAmount must be >= 0")
        Long minAmount,

        @PositiveOrZero(message = "maxAmount must be >= 0")
        Long maxAmount,

        String clientOrderReference,

        @PositiveOrZero(message = "page must be >= 0")
        Integer page,

        @Min(value = 1, message = "size must be >= 1")
        @Max(value = 100, message = "size must be <= 100")
        Integer size,

        @Pattern(regexp = "(createdAt|amountMinor),(asc|desc)",
                message = "sort must be createdAt,asc|desc or amountMinor,asc|desc")
        String sort
) {
    public void validate() {
        validateStatus();
        validateSort();
        validateDateRange();
        validateAmountRange();
    }

    private void validateStatus() {
        if (status == null || status.isBlank()) {
            return;
        }
        if (!status.matches("CREATED|AUTHORIZED|CAPTURED|CANCELLED|EXPIRED|REFUNDED")) {
            throw new IllegalArgumentException(
                    "status must be CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, or REFUNDED");
        }
    }

    private void validateSort() {
        if (sort == null || sort.isBlank()) {
            return;
        }
        if (!sort.matches("(createdAt|amountMinor),(asc|desc)")) {
            throw new IllegalArgumentException("sort must be createdAt,asc|desc or amountMinor,asc|desc");
        }
    }

    private void validateDateRange() {
        if (fromDate == null || toDate == null) {
            return;
        }

        LocalDate from;
        LocalDate to;

        try {
            from = LocalDate.parse(fromDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("fromDate must be a valid ISO date (YYYY-MM-DD)");
        }

        try {
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("toDate must be a valid ISO date (YYYY-MM-DD)");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
    }

    private void validateAmountRange() {
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new IllegalArgumentException("minAmount must not be greater than maxAmount");
        }
    }
}
