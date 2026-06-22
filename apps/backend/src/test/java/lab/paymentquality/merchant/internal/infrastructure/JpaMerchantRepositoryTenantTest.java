package lab.paymentquality.merchant.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaMerchantRepositoryTenantTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_repo_tenant_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaMerchantRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void findByMerchantIdAndTenantIdReturnsMerchantForMatchingTenantOnly() {
        UUID tenantAlphaId = tenantId("TENANT_ALPHA");
        UUID tenantBetaId = tenantId("PLACEHOLDER_TENANT_ID");
        UUID merchantId = UUID.randomUUID();
        repository.saveAndFlush(Merchant.create(merchantId, "MERCH-TENANT-FIND", "Tenant Find", tenantAlphaId));

        assertThat(repository.findByMerchantIdAndTenantId(merchantId, tenantAlphaId)).isPresent();
        assertThat(repository.findByMerchantIdAndTenantId(merchantId, tenantBetaId)).isEmpty();
    }

    @Test
    void findAllByTenantIdReturnsOnlySuppliedTenantAndPreservesOrdering() {
        UUID tenantAlphaId = tenantId("TENANT_ALPHA");
        UUID tenantBetaId = tenantId("PLACEHOLDER_TENANT_ID");
        UUID olderAlphaId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID tieSecondAlphaId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        UUID tieFirstAlphaId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID betaId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        Instant older = Instant.parse("2026-01-01T10:00:00Z");
        Instant newerTie = Instant.parse("2026-01-02T10:00:00Z");

        insertMerchant(olderAlphaId, "MERCH-OLDER-ALPHA", "Older Alpha", older, tenantAlphaId);
        insertMerchant(tieSecondAlphaId, "MERCH-TIE-SECOND", "Tie Second", newerTie, tenantAlphaId);
        insertMerchant(tieFirstAlphaId, "MERCH-TIE-FIRST", "Tie First", newerTie, tenantAlphaId);
        insertMerchant(betaId, "MERCH-BETA", "Beta", newerTie.plusSeconds(60), tenantBetaId);

        var results = repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(
                tenantAlphaId, PageRequest.of(0, 50));

        assertThat(results).extracting(Merchant::getTenantId).containsOnly(tenantAlphaId);
        assertThat(results).extracting(Merchant::getMerchantId)
                .containsExactly(tieFirstAlphaId, tieSecondAlphaId, olderAlphaId);
        assertThat(results).extracting(Merchant::getMerchantId).doesNotContain(betaId);
    }

    private UUID tenantId(String tenantReference) {
        return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM tenants WHERE tenant_reference = ?",
                UUID.class,
                tenantReference);
    }

    private void insertMerchant(UUID merchantId, String reference, String displayName, Instant createdAt, UUID tenantId) {
        jdbcTemplate.update("""
                INSERT INTO merchants (merchant_id, normalized_reference, display_name, status, created_at, updated_at, version, tenant_id)
                VALUES (?, ?, ?, 'DRAFT', ?, ?, 0, ?)
                """, merchantId, reference, displayName, Timestamp.from(createdAt), Timestamp.from(createdAt), tenantId);
    }
}
