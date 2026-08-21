package lab.paymentquality.merchant.internal.web;

import java.util.regex.Pattern;

final class MerchantEtag {

    private static final Pattern VERSION_ETAG = Pattern.compile("^\\\"v(\\d+)\\\"$");

    private MerchantEtag() {
    }

    static String from(Long version) {
        long value = version == null ? 0L : version;
        return "\"v" + value + "\"";
    }

    static long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new MerchantPreconditionRequiredException("If-Match header is required");
        }
        var matcher = VERSION_ETAG.matcher(ifMatch.trim());
        if (!matcher.matches()) {
            throw new MalformedMerchantEtagException(
                    "If-Match must use the \"v{version}\" format, e.g. \"v0\"");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new MalformedMerchantEtagException(
                    "If-Match version is outside the supported numeric range");
        }
    }
}
