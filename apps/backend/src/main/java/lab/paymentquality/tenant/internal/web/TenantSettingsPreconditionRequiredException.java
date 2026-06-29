package lab.paymentquality.tenant.internal.web;

// F-C4: Missing or malformed If-Match header → HTTP 428.
public class TenantSettingsPreconditionRequiredException extends RuntimeException {
    TenantSettingsPreconditionRequiredException(String message) {
        super(message);
    }
}
