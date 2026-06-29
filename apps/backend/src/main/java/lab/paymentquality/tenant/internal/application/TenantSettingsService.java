package lab.paymentquality.tenant.internal.application;

import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import lab.paymentquality.tenant.internal.web.TenantSettingsDto;
import lab.paymentquality.tenant.internal.web.TenantSettingsPreconditionFailedException;
import lab.paymentquality.tenant.internal.web.UpdateTenantSettingsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// F-C4: Reads and updates tenant settings fields.
@Service
public class TenantSettingsService {

    private final JpaTenantRepository repository;

    public TenantSettingsService(JpaTenantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingsWithVersion getSettings(TenantContext ctx) {
        Tenant tenant = loadTenant(ctx);
        return new SettingsWithVersion(toDto(tenant), tenant.getSettingsVersion());
    }

    @Transactional
    public SettingsWithVersion updateSettings(TenantContext ctx, UpdateTenantSettingsRequest req, long expectedVersion) {
        Tenant tenant = loadTenant(ctx);
        if (tenant.getSettingsVersion() != expectedVersion) {
            throw new TenantSettingsPreconditionFailedException();
        }
        tenant.updateSettings(req.contactEmail(), req.timezone(), req.webhookBaseUrl());
        repository.saveAndFlush(tenant);
        return new SettingsWithVersion(toDto(tenant), tenant.getSettingsVersion());
    }

    private Tenant loadTenant(TenantContext ctx) {
        return repository.findById(ctx.tenantId())
                .orElseThrow(() -> new TenantResolutionException("Tenant not found"));
    }

    private static TenantSettingsDto toDto(Tenant t) {
        return new TenantSettingsDto(t.getContactEmail(), t.getTimezone(), t.getWebhookBaseUrl());
    }

    public record SettingsWithVersion(TenantSettingsDto dto, long version) {}
}
