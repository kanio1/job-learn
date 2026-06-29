package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serializes payment orders to RFC 4180 CSV for reconciliation / support analysis exports.
 * Columns are a safe subset: no tokens, no tenant-internal claims, no auth material.
 */
public final class PaymentOrderCsvExporter {

    static final String HEADER =
            "paymentOrderId,merchantId,clientOrderReference,status,amountMinor,currency,createdAt,updatedAt";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private PaymentOrderCsvExporter() {}

    public static String toCsv(List<PaymentOrder> orders) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append("\n");
        for (PaymentOrder o : orders) {
            sb.append(escape(o.getPaymentOrderId().toString())).append(",")
              .append(escape(o.getMerchantId().toString())).append(",")
              .append(escape(o.getClientOrderReference())).append(",")
              .append(escape(o.getStatus().name())).append(",")
              .append(o.getAmountMinor()).append(",")
              .append(escape(o.getCurrency())).append(",")
              .append(o.getCreatedAt() != null ? ISO.format(o.getCreatedAt()) : "").append(",")
              .append(o.getUpdatedAt() != null ? ISO.format(o.getUpdatedAt()) : "")
              .append("\n");
        }
        return sb.toString();
    }

    /** RFC 4180 field escaping: quote field if it contains comma, quote, or newline. */
    static String escape(String value) {
        if (value == null) return "";
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
