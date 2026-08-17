package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeed;
import lab.paymentquality.tenant.TenantSeed;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DataLearningFixtures {

    static final Instant RANGE_START = Instant.parse("2025-01-01T00:00:00Z");
    static final Instant RANGE_END = Instant.parse("2026-08-15T00:00:00Z");

    static final String LEARN_TENANT_C = "LEARN_TENANT_C";
    static final String LEARN_TENANT_D = "LEARN_TENANT_D";

    private DataLearningFixtures() {
    }

    static UUID learnTenantCId() {
        return nameUuid("data-learning:tenant:C");
    }

    static UUID learnTenantDId() {
        return nameUuid("data-learning:tenant:D");
    }

    static UUID learnMerchantId(int index) {
        return nameUuid("data-learning:merchant:" + index);
    }

    static List<TenantSeed> tenants() {
        var tenants = new ArrayList<>(Fixtures.tenants());
        tenants.add(new TenantSeed(learnTenantCId(), LEARN_TENANT_C, "Learning Tenant C", "STANDARD", "ACTIVE"));
        tenants.add(new TenantSeed(learnTenantDId(), LEARN_TENANT_D, "Learning Tenant D", "STANDARD", "ACTIVE"));
        return List.copyOf(tenants);
    }

    static List<MerchantSeed> merchants() {
        var merchants = new ArrayList<>(Fixtures.merchants());
        for (int i = 0; i < 8; i++) {
            merchants.add(new MerchantSeed(
                    learnMerchantId(i),
                    "LEARN-MERCHANT-C-" + (i + 1),
                    "Learning Merchant C " + (i + 1),
                    "ACTIVE",
                    learnTenantCId()));
        }
        for (int i = 8; i < 16; i++) {
            merchants.add(new MerchantSeed(
                    learnMerchantId(i),
                    "LEARN-MERCHANT-D-" + (i - 7),
                    "Learning Merchant D " + (i - 7),
                    "ACTIVE",
                    learnTenantDId()));
        }
        return List.copyOf(merchants);
    }

    static UUID nameUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
