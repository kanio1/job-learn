package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeed;
import lab.paymentquality.payment.PaymentOrderSeed;
import lab.paymentquality.tenant.TenantSeed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class Fixtures {

    static final UUID PLATFORM_TENANT_ID = uuid("00000000-0000-0000-0000-0000000000a1");
    static final UUID TENANT_ALPHA_ID = uuid("00000000-0000-0000-0000-0000000000a2");
    static final UUID PLACEHOLDER_TENANT_ID = uuid("00000000-0000-0000-0000-0000000000a3");

    static final String PLATFORM_TENANT = "PLATFORM_TENANT";
    static final String TENANT_ALPHA = "TENANT_ALPHA";
    static final String PLACEHOLDER_TENANT = "PLACEHOLDER_TENANT_ID";

    static final UUID MERCHANT_ALPHA_001_ID = uuid("00000000-0000-0000-0000-0000000000b1");
    static final UUID MERCHANT_ALPHA_002_ID = uuid("00000000-0000-0000-0000-0000000000b2");
    static final UUID MERCHANT_BETA_001_ID = uuid("00000000-0000-0000-0000-0000000000b3");

    static final String MERCHANT_ALPHA_001 = "MERCHANT_ALPHA_001";
    static final String MERCHANT_ALPHA_002 = "MERCHANT_ALPHA_002";
    static final String MERCHANT_BETA_001 = "MERCHANT_BETA_001";

    private static final Instant BASE_ORDER_TIME = Instant.parse("2026-01-15T09:30:00Z");
    private static final Instant PAGINATION_ORDER_TIME = Instant.parse("2026-01-15T10:00:00Z");

    private Fixtures() {
    }

    static List<TenantSeed> tenants() {
        return List.of(
                new TenantSeed(PLATFORM_TENANT_ID, PLATFORM_TENANT, "Platform Tenant", "PLATFORM", "ACTIVE"),
                new TenantSeed(TENANT_ALPHA_ID, TENANT_ALPHA, "Alpha Tenant", "STANDARD", "ACTIVE"),
                new TenantSeed(PLACEHOLDER_TENANT_ID, PLACEHOLDER_TENANT,
                        "Placeholder Tenant", "STANDARD", "ACTIVE")
        );
    }

    static List<MerchantSeed> merchants() {
        return List.of(
                new MerchantSeed(MERCHANT_ALPHA_001_ID, MERCHANT_ALPHA_001,
                        "Alpha Merchant 001", "ACTIVE", TENANT_ALPHA_ID),
                new MerchantSeed(MERCHANT_ALPHA_002_ID, MERCHANT_ALPHA_002,
                        "Alpha Merchant 002", "ACTIVE", TENANT_ALPHA_ID),
                new MerchantSeed(MERCHANT_BETA_001_ID, MERCHANT_BETA_001,
                        "Beta Merchant 001", "ACTIVE", PLATFORM_TENANT_ID)
        );
    }

    static List<PaymentOrderSeed> paymentOrders() {
        var orders = new ArrayList<PaymentOrderSeed>(104);
        orders.add(order("00000000-0000-0000-0000-0000000000c1", MERCHANT_ALPHA_001_ID,
                "SEED-ALPHA-001-CREATED", 1_100, "PLN", "CREATED", 0, BASE_ORDER_TIME));
        orders.add(order("00000000-0000-0000-0000-0000000000c2", MERCHANT_ALPHA_001_ID,
                "SEED-ALPHA-001-AUTHORIZED", 2_200, "EUR", "AUTHORIZED", 1,
                BASE_ORDER_TIME.plus(1, ChronoUnit.MINUTES)));
        orders.add(order("00000000-0000-0000-0000-0000000000c3", MERCHANT_ALPHA_001_ID,
                "SEED-ALPHA-001-CAPTURED", 3_300, "USD", "CAPTURED", 2,
                BASE_ORDER_TIME.plus(2, ChronoUnit.MINUTES)));
        orders.add(order("00000000-0000-0000-0000-0000000000c4", MERCHANT_ALPHA_002_ID,
                "SEED-ALPHA-002-CANCELLED", 4_400, "PLN", "CANCELLED", 1,
                BASE_ORDER_TIME.plus(3, ChronoUnit.MINUTES)));
        orders.add(order("00000000-0000-0000-0000-0000000000c5", MERCHANT_ALPHA_002_ID,
                "SEED-ALPHA-002-REFUNDED", 5_500, "EUR", "REFUNDED", 3,
                BASE_ORDER_TIME.plus(4, ChronoUnit.MINUTES)));
        orders.add(order("00000000-0000-0000-0000-0000000000c6", MERCHANT_BETA_001_ID,
                "SEED-BETA-001-CREATED", 6_600, "PLN", "CREATED", 0,
                BASE_ORDER_TIME.plus(5, ChronoUnit.MINUTES)));

        for (int n = 101; n <= 198; n++) {
            int offset = n - 101;
            String currency = switch (offset % 3) {
                case 0 -> "PLN";
                case 1 -> "EUR";
                default -> "USD";
            };
            String status = switch (offset % 5) {
                case 0 -> "CREATED";
                case 1 -> "AUTHORIZED";
                case 2 -> "CAPTURED";
                case 3 -> "CANCELLED";
                default -> "REFUNDED";
            };
            long version = switch (status) {
                case "CREATED" -> 0;
                case "AUTHORIZED", "CANCELLED" -> 1;
                case "CAPTURED" -> 2;
                case "REFUNDED" -> 3;
                default -> throw new IllegalStateException("Unsupported fixture status: " + status);
            };
            orders.add(order(
                    "00000000-0000-0000-0000-00000000c" + n,
                    MERCHANT_ALPHA_001_ID,
                    "SEED-ALPHA-001-C" + n,
                    1_000,
                    currency,
                    status,
                    version,
                    PAGINATION_ORDER_TIME.plus(offset, ChronoUnit.MINUTES)));
        }
        return List.copyOf(orders);
    }

    private static PaymentOrderSeed order(String id, UUID merchantId, String clientOrderReference,
                                          long amountMinor, String currency, String status,
                                          long version, Instant createdAt) {
        Instant authorizedAt = null;
        Instant expiresAt = null;
        Instant capturedAt = null;
        Instant cancelledAt = null;
        Instant refundedAt = null;
        Long capturedAmountMinor = null;
        Long refundedAmountMinor = null;
        String cancellationReason = null;
        String refundReason = null;
        Instant updatedAt = createdAt;

        switch (status) {
            case "AUTHORIZED" -> {
                authorizedAt = createdAt.plus(1, ChronoUnit.MINUTES);
                expiresAt = authorizedAt.plus(7, ChronoUnit.DAYS);
                updatedAt = authorizedAt;
            }
            case "CAPTURED" -> {
                authorizedAt = createdAt.plus(1, ChronoUnit.MINUTES);
                capturedAt = createdAt.plus(2, ChronoUnit.MINUTES);
                capturedAmountMinor = amountMinor;
                updatedAt = capturedAt;
            }
            case "CANCELLED" -> {
                cancelledAt = createdAt.plus(1, ChronoUnit.MINUTES);
                cancellationReason = "seed-cancelled";
                updatedAt = cancelledAt;
            }
            case "REFUNDED" -> {
                authorizedAt = createdAt.plus(1, ChronoUnit.MINUTES);
                capturedAt = createdAt.plus(2, ChronoUnit.MINUTES);
                capturedAmountMinor = amountMinor;
                refundedAt = createdAt.plus(3, ChronoUnit.MINUTES);
                refundedAmountMinor = amountMinor;
                refundReason = "seed-refunded";
                updatedAt = refundedAt;
            }
            case "CREATED" -> {
            }
            default -> throw new IllegalArgumentException("Unsupported fixture status: " + status);
        }

        return new PaymentOrderSeed(
                uuid(id), merchantId, clientOrderReference, amountMinor, currency, status, version,
                createdAt, updatedAt, authorizedAt, expiresAt, capturedAt, cancelledAt, refundedAt,
                capturedAmountMinor, refundedAmountMinor, cancellationReason, refundReason);
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value);
    }
}
