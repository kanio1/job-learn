package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.support.SupportCaseSeedCapability;
import lab.paymentquality.tenant.TenantSeedCapability;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeterministicDataset {

    private final TenantSeedCapability tenants;
    private final MerchantSeedCapability merchants;
    private final PaymentSeedCapability payments;
    private final SupportCaseSeedCapability supportCases;
    private final JdbcTemplate jdbc;

    public DeterministicDataset(TenantSeedCapability tenants,
                                MerchantSeedCapability merchants,
                                PaymentSeedCapability payments,
                                SupportCaseSeedCapability supportCases,
                                JdbcTemplate jdbc) {
        this.tenants = tenants;
        this.merchants = merchants;
        this.payments = payments;
        this.supportCases = supportCases;
        this.jdbc = jdbc;
    }

    @Transactional
    public void reset() {
        supportCases.clear();
        payments.clear();
        merchants.clear();
        clearRlsLabItems();
        tenants.clear();
        SatelliteTableWipes.clearCheckoutAuditAndPublications(jdbc);
    }

    @Transactional
    public void seed() {
        supportCases.clear();
        payments.clear();
        merchants.clear();
        clearRlsLabItems();
        tenants.clear();
        SatelliteTableWipes.clearCheckoutAuditAndPublications(jdbc);
        tenants.seed(Fixtures.tenants());
        merchants.seed(Fixtures.merchants());
        payments.seed(Fixtures.paymentOrders());
        seedRlsLabItems();
    }

    private void clearRlsLabItems() {
        jdbc.update("DELETE FROM rls_lab_item_unprotected");
        jdbc.update("DELETE FROM rls_lab_item");
    }

    private void seedRlsLabItems() {
        jdbc.update("""
                INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
                SELECT '00000000-0000-0000-0000-0000000000a1', tenant_id, 'Alpha secret', 100
                FROM tenants WHERE tenant_reference = 'TENANT_ALPHA'
                """);
        jdbc.update("""
                INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
                SELECT '00000000-0000-0000-0000-0000000000a2', tenant_id, 'Other tenant secret', 200
                FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'
                """);
        jdbc.update("""
                INSERT INTO rls_lab_item_unprotected (item_id, tenant_id, label, amount_minor)
                SELECT item_id, tenant_id, label, amount_minor FROM rls_lab_item
                """);
    }
}
