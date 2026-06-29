package lab.paymentquality.tenant.internal.web;

import java.util.regex.Pattern;

// F-C4 helper: formats/parses tenant settings ETag as "v{settingsVersion}".
public final class TenantSettingsEtag {

    private static final Pattern VERSION_ETAG = Pattern.compile("^\\\"v(\\d+)\\\"$");

    private TenantSettingsEtag() {}

    static String from(long version) {
        return "\"v" + version + "\"";
    }

    static long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new TenantSettingsPreconditionRequiredException(
                    "If-Match header is required for conditional update");
        }
        var matcher = VERSION_ETAG.matcher(ifMatch.trim());
        if (!matcher.matches()) {
            throw new TenantSettingsPreconditionRequiredException(
                    "If-Match must use the \"v{version}\" format, e.g. \"v0\"");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new TenantSettingsPreconditionRequiredException(
                    "If-Match version is outside the supported numeric range");
        }
    }
}
