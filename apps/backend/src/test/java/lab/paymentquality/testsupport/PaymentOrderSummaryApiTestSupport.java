package lab.paymentquality.testsupport;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lab.paymentquality.payment.internal.web.PaymentOrderSummaryResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Lesson 08 test support for Payment Order Summary.
 *
 * This helper owns deterministic seed data, reusable request specifications,
 * and explicit expected aggregate calculation used by summary contract,
 * business-flow, and security tests.
 */
public final class PaymentOrderSummaryApiTestSupport {

    private PaymentOrderSummaryApiTestSupport() {
    }

    public static RequestSpecification summaryReaderRequest(int port, String token, String correlationId) {
        return MerchantApiTestSupport.requestWithToken(port, token)
                .accept(ContentType.JSON)
                .header("X-Correlation-ID", correlationId);
    }

    public static void createPaymentOrder(int port,
                                          String merchantId,
                                          String creatorToken,
                                          long amountMinor,
                                          String currency,
                                          String clientOrderReference,
                                          String correlationId) {
        MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("summary-" + currency))
                .header("X-Correlation-ID", correlationId)
                .body(PaymentApiTestSupport.createPaymentOrderBody(amountMinor, currency, clientOrderReference))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201);
    }

    public static List<SeedOrder> seedDefaultDataset(int port, String merchantId, String creatorToken) {
        List<SeedOrder> seed = List.of(
                new SeedOrder(1_000, "PLN", PaymentApiTestSupport.uniquePaymentReference("L08-A")),
                new SeedOrder(2_000, "PLN", PaymentApiTestSupport.uniquePaymentReference("L08-B")),
                new SeedOrder(3_000, "EUR", PaymentApiTestSupport.uniquePaymentReference("L08-C")),
                new SeedOrder(4_000, "USD", PaymentApiTestSupport.uniquePaymentReference("L08-D"))
        );

        int index = 1;
        for (SeedOrder order : seed) {
            createPaymentOrder(
                    port,
                    merchantId,
                    creatorToken,
                    order.amountMinor(),
                    order.currency(),
                    order.clientOrderReference(),
                    "corr-l08-seed-" + index
            );
            index++;
        }

        return seed;
    }

    public static ExpectedSummary expectedFor(List<SeedOrder> seed) {
        long totalOrders = seed.size();
        long totalAmount = seed.stream().mapToLong(SeedOrder::amountMinor).sum();

        Map<String, List<SeedOrder>> byCurrencyMap = seed.stream()
                .collect(java.util.stream.Collectors.groupingBy(SeedOrder::currency));
        List<PaymentOrderSummaryResponse.CurrencySummary> byCurrency = new ArrayList<>();
        byCurrencyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    long count = entry.getValue().size();
                    long sum = entry.getValue().stream().mapToLong(SeedOrder::amountMinor).sum();
                    byCurrency.add(new PaymentOrderSummaryResponse.CurrencySummary(entry.getKey(), count, sum));
                });

        List<PaymentOrderSummaryResponse.StatusSummary> byStatus = List.of(
                new PaymentOrderSummaryResponse.StatusSummary("CREATED", totalOrders, totalAmount)
        );

        return new ExpectedSummary(totalOrders, totalAmount, byCurrency, byStatus);
    }

    public static String isoToday() {
        return LocalDate.now().toString();
    }

    public static String isoYesterday() {
        return LocalDate.now().minusDays(1).toString();
    }

    public static String isoTomorrow() {
        return LocalDate.now().plusDays(1).toString();
    }

    public static Comparator<PaymentOrderSummaryResponse.CurrencySummary> currencyComparator() {
        return Comparator.comparing(PaymentOrderSummaryResponse.CurrencySummary::currency);
    }

    public static Comparator<PaymentOrderSummaryResponse.StatusSummary> statusComparator() {
        return Comparator.comparing(PaymentOrderSummaryResponse.StatusSummary::status);
    }

    public record SeedOrder(long amountMinor, String currency, String clientOrderReference) {
    }

    public record ExpectedSummary(long totalOrders,
                                  long totalAmountMinor,
                                  List<PaymentOrderSummaryResponse.CurrencySummary> byCurrency,
                                  List<PaymentOrderSummaryResponse.StatusSummary> byStatus) {
    }
}
