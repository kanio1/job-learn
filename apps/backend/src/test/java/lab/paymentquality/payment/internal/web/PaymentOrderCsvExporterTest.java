package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderCsvExporterTest {

    private static final UUID MERCHANT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-06-29T10:00:00Z");

    @Test
    void emptyListProducesHeaderOnly() {
        String csv = PaymentOrderCsvExporter.toCsv(List.of());
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).isEqualTo(PaymentOrderCsvExporter.HEADER);
    }

    @Test
    void singleOrderIsSerializedToExpectedColumns() {
        UUID orderId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        PaymentOrder order = seeded(orderId, MERCHANT_ID, "PAY-001", 9900L, "PLN", PaymentStatus.CREATED);

        String csv = PaymentOrderCsvExporter.toCsv(List.of(order));
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1])
                .startsWith(orderId + "," + MERCHANT_ID + ",PAY-001,CREATED,9900,PLN,")
                .contains("2026-06-29T10:00:00Z");
    }

    @Test
    void clientReferenceWithCommaIsQuoted() {
        String refWithComma = "PAY,CSV,COMMA";
        PaymentOrder order = seeded(UUID.randomUUID(), MERCHANT_ID, refWithComma, 100L, "EUR", PaymentStatus.CREATED);

        String csv = PaymentOrderCsvExporter.toCsv(List.of(order));
        assertThat(csv).contains("\"PAY,CSV,COMMA\"");
    }

    @Test
    void clientReferenceWithDoubleQuoteIsEscaped() {
        String refWithQuote = "PAY\"QUOTE";
        PaymentOrder order = seeded(UUID.randomUUID(), MERCHANT_ID, refWithQuote, 100L, "EUR", PaymentStatus.CREATED);

        String csv = PaymentOrderCsvExporter.toCsv(List.of(order));
        assertThat(csv).contains("\"PAY\"\"QUOTE\"");
    }

    // ─── escape() unit tests ──────────────────────────────────────────────────

    @Test
    void escapeReturnsValueUnchangedWhenNoSpecialChars() {
        assertThat(PaymentOrderCsvExporter.escape("CREATED")).isEqualTo("CREATED");
    }

    @Test
    void escapeWrapsValueContainingComma() {
        assertThat(PaymentOrderCsvExporter.escape("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void escapeDoublesQuotesInsideValue() {
        assertThat(PaymentOrderCsvExporter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    @Test
    void escapeHandlesNull() {
        assertThat(PaymentOrderCsvExporter.escape(null)).isEmpty();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static PaymentOrder seeded(UUID orderId, UUID merchantId, String ref, long amount,
                                       String currency, PaymentStatus status) {
        return PaymentOrder.seeded(orderId, merchantId, ref, amount, currency, status,
                0L, NOW, NOW, null, null, null, null, null, null, null, null, null);
    }
}
