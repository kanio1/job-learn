package lab.paymentquality.payment.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record PaymentOrderListRequest(
        @Pattern(regexp = "CREATED", message = "status must be CREATED")
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

        @Pattern(regexp = "createdAt,(asc|desc)", message = "sort must be createdAt,asc or createdAt,desc")
        String sort
) {
}
