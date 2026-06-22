package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeed;
import lab.paymentquality.payment.PaymentOrderSeed;
import lab.paymentquality.tenant.TenantSeed;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FixturesTest {

    private static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID TENANT_ALPHA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID PLACEHOLDER_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a3");

    private static final UUID MERCHANT_ALPHA_001_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID MERCHANT_ALPHA_002_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID MERCHANT_BETA_001_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    // --- Tenant assertions ---

    @Test
    void tenantsContainsExactlyThreeWithExpectedUuids() {
        List<TenantSeed> tenants = Fixtures.tenants();

        assertThat(tenants).hasSize(3);
        Set<UUID> ids = tenants.stream().map(TenantSeed::tenantId).collect(Collectors.toSet());
        assertThat(ids).containsExactlyInAnyOrder(PLATFORM_TENANT_ID, TENANT_ALPHA_ID, PLACEHOLDER_TENANT_ID);
    }

    @Test
    void tenantReferencesAreUnique() {
        List<TenantSeed> tenants = Fixtures.tenants();
        Set<String> refs = tenants.stream().map(TenantSeed::tenantReference).collect(Collectors.toSet());
        assertThat(refs).hasSize(3)
                .containsExactlyInAnyOrder("PLATFORM_TENANT", "TENANT_ALPHA", "PLACEHOLDER_TENANT_ID");
    }

    @Test
    void allTenantsAreActive() {
        Fixtures.tenants().forEach(t -> assertThat(t.status()).isEqualTo("ACTIVE"));
    }

    @Test
    void platformTenantHasCorrectTypeAndUuid() {
        TenantSeed platform = Fixtures.tenants().stream()
                .filter(t -> t.tenantReference().equals("PLATFORM_TENANT"))
                .findFirst().orElseThrow();
        assertThat(platform.tenantType()).isEqualTo("PLATFORM");
        assertThat(platform.tenantId()).isEqualTo(PLATFORM_TENANT_ID);
    }

    @Test
    void nonPlatformTenantsAreStandard() {
        Fixtures.tenants().stream()
                .filter(t -> !t.tenantReference().equals("PLATFORM_TENANT"))
                .forEach(t -> assertThat(t.tenantType()).isEqualTo("STANDARD"));
    }

    // --- Merchant assertions ---

    @Test
    void merchantsContainsExactlyThreeWithExpectedUuids() {
        List<MerchantSeed> merchants = Fixtures.merchants();

        assertThat(merchants).hasSize(3);
        Set<UUID> ids = merchants.stream().map(MerchantSeed::merchantId).collect(Collectors.toSet());
        assertThat(ids).containsExactlyInAnyOrder(MERCHANT_ALPHA_001_ID, MERCHANT_ALPHA_002_ID, MERCHANT_BETA_001_ID);
    }

    @Test
    void merchantReferencesAreUnique() {
        List<MerchantSeed> merchants = Fixtures.merchants();
        Set<String> refs = merchants.stream().map(MerchantSeed::merchantReference).collect(Collectors.toSet());
        assertThat(refs).hasSize(3)
                .containsExactlyInAnyOrder("MERCHANT_ALPHA_001", "MERCHANT_ALPHA_002", "MERCHANT_BETA_001");
    }

    @Test
    void allMerchantsAreActive() {
        Fixtures.merchants().forEach(m -> assertThat(m.status()).isEqualTo("ACTIVE"));
    }

    @Test
    void allMerchantTenantIdsReferenceSeededTenants() {
        Set<UUID> tenantIds = Fixtures.tenants().stream().map(TenantSeed::tenantId).collect(Collectors.toSet());
        Fixtures.merchants().forEach(m -> assertThat(tenantIds).contains(m.tenantId()));
    }

    // --- Payment order assertions ---

    @Test
    void paymentOrdersContainsExactly104Orders() {
        assertThat(Fixtures.paymentOrders()).hasSize(104);
    }

    @Test
    void paymentOrderUuidsAreUnique() {
        List<PaymentOrderSeed> orders = Fixtures.paymentOrders();
        Set<UUID> ids = orders.stream().map(PaymentOrderSeed::paymentOrderId).collect(Collectors.toSet());
        assertThat(ids).hasSize(104);
    }

    @Test
    void paymentOrderClientReferencesAreUnique() {
        List<PaymentOrderSeed> orders = Fixtures.paymentOrders();
        Set<String> refs = orders.stream().map(PaymentOrderSeed::clientOrderReference).collect(Collectors.toSet());
        assertThat(refs).hasSize(104);
    }

    @Test
    void allPaymentOrdersReferenceSeededMerchants() {
        Set<UUID> merchantIds = Fixtures.merchants().stream().map(MerchantSeed::merchantId).collect(Collectors.toSet());
        Fixtures.paymentOrders().forEach(o -> assertThat(merchantIds).contains(o.merchantId()));
    }

    @Test
    void paymentOrdersContainAllExpectedStatuses() {
        Set<String> statuses = Fixtures.paymentOrders().stream()
                .map(PaymentOrderSeed::status)
                .collect(Collectors.toSet());
        assertThat(statuses).containsExactlyInAnyOrder("CREATED", "AUTHORIZED", "CAPTURED", "CANCELLED", "REFUNDED");
    }

    @Test
    void currenciesAreOnlyPlnEurUsd() {
        Set<String> currencies = Fixtures.paymentOrders().stream()
                .map(PaymentOrderSeed::currency)
                .collect(Collectors.toSet());
        assertThat(currencies).containsExactlyInAnyOrder("PLN", "EUR", "USD");
    }

    @Test
    void noRequiredFieldsAreNull() {
        Fixtures.paymentOrders().forEach(o -> {
            assertThat(o.paymentOrderId()).isNotNull();
            assertThat(o.merchantId()).isNotNull();
            assertThat(o.clientOrderReference()).isNotNull().isNotBlank();
            assertThat(o.currency()).isNotNull().isNotBlank();
            assertThat(o.status()).isNotNull().isNotBlank();
            assertThat(o.createdAt()).isNotNull();
            assertThat(o.updatedAt()).isNotNull();
        });
    }

    // --- MERCHANT_ALPHA_001 summary assertions ---

    @Test
    void merchantAlpha001HasExactly101Orders() {
        long count = Fixtures.paymentOrders().stream()
                .filter(o -> o.merchantId().equals(MERCHANT_ALPHA_001_ID))
                .count();
        assertThat(count).isEqualTo(101L);
    }

    @Test
    void merchantAlpha001StatusCountsMatchExpectedSummary() {
        Map<String, Long> statusCounts = Fixtures.paymentOrders().stream()
                .filter(o -> o.merchantId().equals(MERCHANT_ALPHA_001_ID))
                .collect(Collectors.groupingBy(PaymentOrderSeed::status, Collectors.counting()));

        assertThat(statusCounts).containsEntry("CREATED", 21L);
        assertThat(statusCounts).containsEntry("AUTHORIZED", 21L);
        assertThat(statusCounts).containsEntry("CAPTURED", 21L);
        assertThat(statusCounts).containsEntry("CANCELLED", 19L);
        assertThat(statusCounts).containsEntry("REFUNDED", 19L);
    }

    @Test
    void merchantAlpha001CurrencyCountsMatchExpectedSummary() {
        Map<String, Long> currencyCounts = Fixtures.paymentOrders().stream()
                .filter(o -> o.merchantId().equals(MERCHANT_ALPHA_001_ID))
                .collect(Collectors.groupingBy(PaymentOrderSeed::currency, Collectors.counting()));

        assertThat(currencyCounts).containsEntry("PLN", 34L);
        assertThat(currencyCounts).containsEntry("EUR", 34L);
        assertThat(currencyCounts).containsEntry("USD", 33L);
    }

    // --- Expansion block assertions ---

    @Test
    void expansionBlockContains98OrdersForMerchantAlpha001() {
        long count = Fixtures.paymentOrders().stream()
                .filter(o -> o.merchantId().equals(MERCHANT_ALPHA_001_ID))
                .filter(o -> o.clientOrderReference().matches("SEED-ALPHA-001-C\\d+"))
                .count();
        assertThat(count).isEqualTo(98L);
    }

    @Test
    void expansionBlockUuidsAndReferencesArePresent() {
        Set<UUID> orderIds = Fixtures.paymentOrders().stream()
                .map(PaymentOrderSeed::paymentOrderId)
                .collect(Collectors.toSet());
        Set<String> refs = Fixtures.paymentOrders().stream()
                .map(PaymentOrderSeed::clientOrderReference)
                .collect(Collectors.toSet());

        for (int n = 101; n <= 198; n++) {
            UUID expectedId = UUID.fromString("00000000-0000-0000-0000-00000000c" + n);
            assertThat(orderIds).contains(expectedId);
            assertThat(refs).contains("SEED-ALPHA-001-C" + n);
        }
    }

    @Test
    void expansionBlockAmountsAreAll1000() {
        Fixtures.paymentOrders().stream()
                .filter(o -> o.merchantId().equals(MERCHANT_ALPHA_001_ID))
                .filter(o -> o.clientOrderReference().matches("SEED-ALPHA-001-C\\d+"))
                .forEach(o -> assertThat(o.amountMinor()).isEqualTo(1000L));
    }

    @Test
    void expansionBlockStatusVersionCurrencyAndCreatedAtFollowDeterministicRules() {
        Instant pagBase = Instant.parse("2026-01-15T10:00:00Z");
        Map<UUID, PaymentOrderSeed> orderById = Fixtures.paymentOrders().stream()
                .collect(Collectors.toMap(PaymentOrderSeed::paymentOrderId, o -> o));

        for (int n = 101; n <= 198; n++) {
            int offset = n - 101;
            UUID expectedId = UUID.fromString("00000000-0000-0000-0000-00000000c" + n);
            PaymentOrderSeed order = orderById.get(expectedId);
            assertThat(order).as("Expansion order c%d must exist", n).isNotNull();

            String expectedStatus = switch (offset % 5) {
                case 0 -> "CREATED";
                case 1 -> "AUTHORIZED";
                case 2 -> "CAPTURED";
                case 3 -> "CANCELLED";
                default -> "REFUNDED";
            };
            long expectedVersion = switch (expectedStatus) {
                case "CREATED" -> 0L;
                case "AUTHORIZED", "CANCELLED" -> 1L;
                case "CAPTURED" -> 2L;
                case "REFUNDED" -> 3L;
                default -> throw new IllegalStateException();
            };
            String expectedCurrency = switch (offset % 3) {
                case 0 -> "PLN";
                case 1 -> "EUR";
                default -> "USD";
            };
            Instant expectedCreatedAt = pagBase.plus(offset, ChronoUnit.MINUTES);

            assertThat(order.status()).as("c%d status", n).isEqualTo(expectedStatus);
            assertThat(order.version()).as("c%d version", n).isEqualTo(expectedVersion);
            assertThat(order.currency()).as("c%d currency", n).isEqualTo(expectedCurrency);
            assertThat(order.amountMinor()).as("c%d amount", n).isEqualTo(1000L);
            assertThat(order.merchantId()).as("c%d merchantId", n).isEqualTo(MERCHANT_ALPHA_001_ID);
            assertThat(order.createdAt()).as("c%d createdAt", n).isEqualTo(expectedCreatedAt);
        }
    }

    @Test
    void fixtureCallsAreStableAndDeterministic() {
        assertThat(Fixtures.tenants()).isEqualTo(Fixtures.tenants());
        assertThat(Fixtures.merchants()).isEqualTo(Fixtures.merchants());
        assertThat(Fixtures.paymentOrders()).isEqualTo(Fixtures.paymentOrders());
    }
}
