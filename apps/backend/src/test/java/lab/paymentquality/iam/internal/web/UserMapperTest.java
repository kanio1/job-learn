package lab.paymentquality.iam.internal.web;

import lab.paymentquality.iam.internal.domain.ManagedUser;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toSummaryMapsAllFields() {
        ManagedUser user = new ManagedUser(
                "user-1", "alice", "alice@example.com", true,
                "TENANT_ALPHA", "MERCH-001",
                List.of("PLATFORM_ADMIN", "TENANT_ADMIN"));

        UserSummary summary = UserMapper.toSummary(user);

        assertThat(summary.id()).isEqualTo("user-1");
        assertThat(summary.username()).isEqualTo("alice");
        assertThat(summary.email()).isEqualTo("alice@example.com");
        assertThat(summary.enabled()).isTrue();
        assertThat(summary.tenantId()).isEqualTo("TENANT_ALPHA");
        assertThat(summary.merchantId()).isEqualTo("MERCH-001");
        assertThat(summary.roles()).containsExactly("PLATFORM_ADMIN", "TENANT_ADMIN");
    }

    @Test
    void toDetailMapsAllFields() {
        ManagedUser user = new ManagedUser(
                "user-2", "bob", "bob@example.com", false,
                "TENANT_BETA", null,
                List.of("READ_ONLY_USER"));

        UserDetail detail = UserMapper.toDetail(user);

        assertThat(detail.id()).isEqualTo("user-2");
        assertThat(detail.username()).isEqualTo("bob");
        assertThat(detail.email()).isEqualTo("bob@example.com");
        assertThat(detail.enabled()).isFalse();
        assertThat(detail.tenantId()).isEqualTo("TENANT_BETA");
        assertThat(detail.merchantId()).isNull();
        assertThat(detail.roles()).containsExactly("READ_ONLY_USER");
    }

    @Test
    void toSummaryFlattensFirstTenantAndMerchantAttribute() {
        ManagedUser user = new ManagedUser(
                "user-3", "carol", "carol@example.com", true,
                "TENANT_ALPHA", "MERCH-001",
                List.of());

        UserSummary summary = UserMapper.toSummary(user);

        assertThat(summary.tenantId()).isEqualTo("TENANT_ALPHA");
        assertThat(summary.merchantId()).isEqualTo("MERCH-001");
    }

    @Test
    void toDetailHandlesNullTenantAndMerchant() {
        ManagedUser user = new ManagedUser(
                "user-4", "dave", "dave@example.com", true,
                null, null,
                List.of());

        UserDetail detail = UserMapper.toDetail(user);

        assertThat(detail.tenantId()).isNull();
        assertThat(detail.merchantId()).isNull();
    }

    @Test
    void toDetailHandlesNullRoles() {
        ManagedUser user = new ManagedUser(
                "user-5", "eve", "eve@example.com", true,
                "TENANT_ALPHA", null,
                null);

        UserDetail detail = UserMapper.toDetail(user);

        assertThat(detail.roles()).isEmpty();
    }

    @Test
    void mapperNeverCarriesCredentialFields() {
        ManagedUser user = new ManagedUser(
                "user-6", "frank", "frank@example.com", true,
                "TENANT_ALPHA", null,
                List.of("MERCHANT_MANAGER"));

        UserSummary summary = UserMapper.toSummary(user);
        UserDetail detail = UserMapper.toDetail(user);

        assertThat(summary.id()).isNotNull();
        assertThat(summary.username()).isNotNull();
        assertThat(summary.email()).isNotNull();
        assertThat(detail.id()).isNotNull();
        assertThat(detail.username()).isNotNull();
        assertThat(detail.email()).isNotNull();

        assertThat(summary.getClass().getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("password", "temporaryPassword", "credential",
                        "adminToken", "bearerToken", "accessToken");
        assertThat(detail.getClass().getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("password", "temporaryPassword", "credential",
                        "adminToken", "bearerToken", "accessToken");
    }
}
