package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.payment.internal.domain.*;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaPaymentOrderRepositoryTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_repo_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaPaymentOrderRepository paymentOrderRepository;

    @Autowired
    JpaIdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;

    @Autowired
    JpaMerchantRepository merchantRepository;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        statusHistoryRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        paymentOrderRepository.deleteAll();
        merchantRepository.deleteAll();

        UUID placeholderTenantId = jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'",
                UUID.class);
        Merchant merchant = merchantRepository.saveAndFlush(
                Merchant.create(UUID.randomUUID(), "MERCH-" + UUID.randomUUID().toString().substring(0, 8), "Test Merchant", placeholderTenantId));
        merchantId = merchant.getMerchantId();
    }

    @Test
    void saveAndFindByMerchantIdAndPaymentOrderId() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-001", 12500, "PLN");
        paymentOrderRepository.saveAndFlush(order);

        var found = paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId);

        assertThat(found).isPresent();
        assertThat(found.get().getAmountMinor()).isEqualTo(12500);
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void findByPaymentOrderIdForPlatformRead() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-002", 5000, "EUR");
        paymentOrderRepository.saveAndFlush(order);

        var found = paymentOrderRepository.findByPaymentOrderId(paymentOrderId);

        assertThat(found).isPresent();
        assertThat(found.get().getCurrency()).isEqualTo("EUR");
    }

    @Test
    void amountConstraintRejectsZero() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-003", 0, "PLN");

        assertThatThrownBy(() -> paymentOrderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void currencyConstraintRejectsInvalid() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-004", 1000, "GBP");

        assertThatThrownBy(() -> paymentOrderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void idempotencyUniqueConstraintEnforced() {
        UUID paymentOrderId1 = UUID.randomUUID();
        PaymentOrder order1 = PaymentOrder.create(paymentOrderId1, merchantId, "PAY-005", 1000, "PLN");
        paymentOrderRepository.saveAndFlush(order1);

        String keyHash = "a".repeat(64);
        String fingerprintHash = "b".repeat(64);

        IdempotencyRecord record1 = IdempotencyRecord.reserve(UUID.randomUUID(), merchantId, keyHash, fingerprintHash);
        record1.complete(paymentOrderId1);
        idempotencyRecordRepository.saveAndFlush(record1);

        UUID paymentOrderId2 = UUID.randomUUID();
        PaymentOrder order2 = PaymentOrder.create(paymentOrderId2, merchantId, "PAY-006", 2000, "PLN");
        paymentOrderRepository.saveAndFlush(order2);

        IdempotencyRecord record2 = IdempotencyRecord.reserve(UUID.randomUUID(), merchantId, keyHash, fingerprintHash);
        record2.complete(paymentOrderId2);

        assertThatThrownBy(() -> idempotencyRecordRepository.saveAndFlush(record2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void statusHistoryCreationEntry() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-007", 3000, "USD");
        paymentOrderRepository.saveAndFlush(order);

        PaymentOrderStatusHistory history = PaymentOrderStatusHistory.creationEntry(
                paymentOrderId, "test-subject", "corr-001");
        statusHistoryRepository.saveAndFlush(history);

        var entries = statusHistoryRepository.findByPaymentOrderIdOrderByCreatedAtAsc(paymentOrderId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getToStatus()).isEqualTo("CREATED");
        assertThat(entries.get(0).getFromStatus()).isNull();
        assertThat(entries.get(0).getActorSubject()).isEqualTo("test-subject");
        assertThat(entries.get(0).getCorrelationId()).isEqualTo("corr-001");
    }
}
