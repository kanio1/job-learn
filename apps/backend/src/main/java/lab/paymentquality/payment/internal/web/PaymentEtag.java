package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;

import java.util.regex.Pattern;

// Feature 011 helper: parses and formats the payment order ETag contract "v{version}" away from controller methods.
public final class PaymentEtag {

    private static final Pattern VERSION_ETAG = Pattern.compile("^\\\"v(\\d+)\\\"$");

    private PaymentEtag() {
    }

    public static String from(PaymentOrder order) {
        return "\"v" + order.getVersion() + "\"";
    }

    public static long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PaymentPreconditionRequiredException("If-Match header is required");
        }
        var matcher = VERSION_ETAG.matcher(ifMatch.trim());
        if (!matcher.matches()) {
            throw new MalformedPaymentEtagException("If-Match must use the \"v{version}\" format");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new MalformedPaymentEtagException("If-Match version is outside the supported numeric range");
        }
    }
}
