package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.web.PaymentOrderSummaryRequest;
import lab.paymentquality.payment.internal.web.PaymentOrderSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentOrderSummaryService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("PLN", "EUR", "USD");

    private final JpaPaymentOrderRepository paymentOrderRepository;

    public PaymentOrderSummaryService(JpaPaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    public PaymentOrderSummaryResponse summarize(UUID merchantId, PaymentOrderSummaryRequest request) {
        String currency = normalizeOptional(request.currency());
        String statusRaw = normalizeOptional(request.status());

        validateCurrency(currency);
        PaymentStatus status = parseStatus(statusRaw);

        Instant fromInclusive = toFromInclusive(request.fromDate());
        Instant toInclusive = toToInclusive(request.toDate());

        var totals = paymentOrderRepository.findSummaryTotals(
                merchantId,
                currency,
                status,
                fromInclusive,
                toInclusive
        );

        List<PaymentOrderSummaryResponse.CurrencySummary> byCurrency = paymentOrderRepository
                .findSummaryByCurrency(merchantId, currency, status, fromInclusive, toInclusive)
                .stream()
                .map(row -> new PaymentOrderSummaryResponse.CurrencySummary(
                        row.getCurrency(),
                        row.getOrderCount(),
                        row.getTotalAmountMinor() != null ? row.getTotalAmountMinor() : 0L
                ))
                .toList();

        List<PaymentOrderSummaryResponse.StatusSummary> byStatus = paymentOrderRepository
                .findSummaryByStatus(merchantId, currency, status, fromInclusive, toInclusive)
                .stream()
                .map(row -> new PaymentOrderSummaryResponse.StatusSummary(
                        row.getStatus().name(),
                        row.getOrderCount(),
                        row.getTotalAmountMinor() != null ? row.getTotalAmountMinor() : 0L
                ))
                .toList();

        return new PaymentOrderSummaryResponse(
                totals.getOrderCount(),
                totals.getTotalAmountMinor() != null ? totals.getTotalAmountMinor() : 0L,
                byCurrency,
                byStatus
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private void validateCurrency(String currency) {
        if (currency == null) {
            return;
        }
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("currency must be PLN, EUR, or USD");
        }
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be CREATED");
        }
    }

    private Instant toFromInclusive(String fromDate) {
        if (fromDate == null || fromDate.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(fromDate);
            ZonedDateTime start = parsed.atStartOfDay(ZoneOffset.UTC);
            return start.toInstant();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format: " + fromDate + ". Expected ISO date (YYYY-MM-DD)");
        }
    }

    private Instant toToInclusive(String toDate) {
        if (toDate == null || toDate.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(toDate);
            ZonedDateTime end = parsed.atTime(23, 59, 59, 999_999_999)
                    .atZone(ZoneOffset.UTC);
            return end.toInstant();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format: " + toDate + ". Expected ISO date (YYYY-MM-DD)");
        }
    }
}
