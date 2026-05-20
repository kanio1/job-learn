package lab.paymentquality.merchant.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaMerchantRepositoryTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer<?> postgres = newPostgresContainer("merchant_repo_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaMerchantRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindById() {
        Merchant saved = repository.saveAndFlush(Merchant.create(UUID.randomUUID(), "MERCH-001", "Merchant One"));

        assertThat(repository.findById(saved.getMerchantId())).contains(saved);
    }

    @Test
    void findByNormalizedReference() {
        Merchant saved = repository.saveAndFlush(Merchant.create(UUID.randomUUID(), "MERCH-002", "Merchant Two"));

        assertThat(repository.findByNormalizedReference("MERCH-002")).contains(saved);
    }

    @Test
    void uniqueNormalizedReferenceConstraint() {
        repository.saveAndFlush(Merchant.create(UUID.randomUUID(), "MERCH-003", "Merchant Three"));

        assertThatThrownBy(() -> repository.saveAndFlush(Merchant.create(UUID.randomUUID(), "MERCH-003", "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listNewestFirstWithMerchantIdTieBreaker() {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant sameCreatedAt = Instant.parse("2026-05-18T12:00:00Z");

        jdbcTemplate.update("""
                INSERT INTO merchants (merchant_id, normalized_reference, display_name, status, created_at, updated_at, version)
                VALUES (?, ?, ?, 'DRAFT', ?, ?, 0)
                """, secondId, "MERCH-SECOND", "Second", Timestamp.from(sameCreatedAt), Timestamp.from(sameCreatedAt));
        jdbcTemplate.update("""
                INSERT INTO merchants (merchant_id, normalized_reference, display_name, status, created_at, updated_at, version)
                VALUES (?, ?, ?, 'DRAFT', ?, ?, 0)
                """, firstId, "MERCH-FIRST", "First", Timestamp.from(sameCreatedAt), Timestamp.from(sameCreatedAt));

        var results = repository.findAllByOrderByCreatedAtDescMerchantIdAsc(PageRequest.of(0, 50));

        assertThat(results).extracting(Merchant::getMerchantId).containsSubsequence(firstId, secondId);
    }
}
