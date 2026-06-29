package lab.paymentquality.tenant.internal.web;

// F-C4: Tenant settings response DTO (contact email, timezone, optional webhook URL).
public record TenantSettingsDto(
        String contactEmail,
        String timezone,
        String webhookBaseUrl
) {}
