package lab.paymentquality.tenant.internal.web;

// F-C4: Stale If-Match version → HTTP 412 Precondition Failed.
public class TenantSettingsPreconditionFailedException extends RuntimeException {
    public TenantSettingsPreconditionFailedException() {
        super("Tenant settings were modified after you loaded them. Reload and retry.");
    }
}
