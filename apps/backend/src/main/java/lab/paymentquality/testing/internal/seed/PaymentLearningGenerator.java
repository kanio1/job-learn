package lab.paymentquality.testing.internal.seed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

final class PaymentLearningGenerator {

    static final int PAYMENT_COUNT = 10_000;

    private static final long RANGE_SECONDS =
            Duration.between(DataLearningFixtures.RANGE_START, DataLearningFixtures.RANGE_END).getSeconds();

    private PaymentLearningGenerator() {
    }

    static String statusFor(int index) {
        int bucket = index % 100;
        if (bucket < 60) {
            return "CAPTURED";
        }
        if (bucket < 72) {
            return "REFUNDED";
        }
        if (bucket < 80) {
            return "CANCELLED";
        }
        if (bucket < 88) {
            return "AUTHORIZED";
        }
        if (bucket < 92) {
            return "EXPIRED";
        }
        return "CREATED";
    }

    static UUID merchantIdFor(int index) {
        int bucket = index % 100;
        if (bucket < 55) {
            return Fixtures.MERCHANT_ALPHA_001_ID;
        }
        if (bucket < 75) {
            return Fixtures.MERCHANT_BETA_001_ID;
        }
        if (bucket < 90) {
            return DataLearningFixtures.learnMerchantId((index / 100) % 8);
        }
        if (bucket < 98) {
            return DataLearningFixtures.learnMerchantId(8 + ((index / 100) % 8));
        }
        return Fixtures.MERCHANT_SUSPENDED_DEMO_ID;
    }

    static UUID paymentIdFor(int index) {
        return DataLearningFixtures.nameUuid("data-learning:payment:" + index);
    }

    static String referenceFor(int index) {
        return "LEARN-PAY-" + String.format("%06d", index);
    }

    static Instant createdAtFor(int index) {
        long offset = RANGE_SECONDS * index / (PAYMENT_COUNT - 1);
        return DataLearningFixtures.RANGE_START.plusSeconds(offset);
    }

    static long amountMinorFor(int index) {
        return 1_000L + (index % 50_000);
    }

    static String currencyFor(int index) {
        return switch (index % 3) {
            case 0 -> "PLN";
            case 1 -> "EUR";
            default -> "USD";
        };
    }

    static List<HistoryStep> historyFor(int index, String status, Instant createdAt, long amountMinor) {
        var steps = new ArrayList<HistoryStep>();
        steps.add(new HistoryStep(index, 0, null, "CREATED", null, createdAt, null, null));
        Instant authorizedAt = createdAt.plus(Duration.ofMinutes(1));
        Instant capturedAt = createdAt.plus(Duration.ofMinutes(4));
        Instant cancelledAt = createdAt.plus(Duration.ofMinutes(1));
        Instant expiredAt = createdAt.plus(Duration.ofHours(7)).plus(Duration.ofMinutes(1));
        Instant refundedAt = createdAt.plus(Duration.ofMinutes(15));
        switch (status) {
            case "CREATED" -> {
            }
            case "AUTHORIZED" -> steps.add(authorize(index, createdAt, authorizedAt, amountMinor));
            case "CAPTURED" -> {
                steps.add(authorize(index, createdAt, authorizedAt, amountMinor));
                steps.add(capture(index, authorizedAt, capturedAt, amountMinor));
            }
            case "REFUNDED" -> {
                steps.add(authorize(index, createdAt, authorizedAt, amountMinor));
                steps.add(capture(index, authorizedAt, capturedAt, amountMinor));
                steps.add(new HistoryStep(index, 3, "CAPTURED", "REFUNDED", "REFUND", refundedAt,
                        amountMinor, "seed-refunded"));
            }
            case "CANCELLED" -> steps.add(new HistoryStep(index, 1, "CREATED", "CANCELLED", "CANCEL",
                    cancelledAt, null, "seed-cancelled"));
            case "EXPIRED" -> {
                steps.add(authorize(index, createdAt, authorizedAt, amountMinor));
                steps.add(new HistoryStep(index, 2, "AUTHORIZED", "EXPIRED", "EXPIRE", expiredAt, null, null));
            }
            default -> throw new IllegalArgumentException("Unsupported learning status: " + status);
        }
        return List.copyOf(steps);
    }

    static Instant updatedAtFor(List<HistoryStep> history) {
        return history.getLast().at();
    }

    static Instant authorizedAtFor(String status, Instant createdAt) {
        return switch (status) {
            case "AUTHORIZED", "CAPTURED", "REFUNDED", "EXPIRED" -> createdAt.plus(Duration.ofMinutes(1));
            default -> null;
        };
    }

    static Instant expiresAtFor(String status, Instant createdAt) {
        Instant authorizedAt = authorizedAtFor(status, createdAt);
        if (authorizedAt == null || "CAPTURED".equals(status) || "REFUNDED".equals(status)) {
            return null;
        }
        if ("AUTHORIZED".equals(status) || "EXPIRED".equals(status)) {
            return authorizedAt.plus(Duration.ofDays(7));
        }
        return null;
    }

    static Instant capturedAtFor(String status, Instant createdAt) {
        return switch (status) {
            case "CAPTURED", "REFUNDED" -> createdAt.plus(Duration.ofMinutes(4));
            default -> null;
        };
    }

    static Instant cancelledAtFor(String status, Instant createdAt) {
        return "CANCELLED".equals(status) ? createdAt.plus(Duration.ofMinutes(1)) : null;
    }

    static Instant refundedAtFor(String status, Instant createdAt) {
        return "REFUNDED".equals(status) ? createdAt.plus(Duration.ofMinutes(15)) : null;
    }

    static Long capturedAmountFor(String status, long amountMinor) {
        return switch (status) {
            case "CAPTURED", "REFUNDED" -> amountMinor;
            default -> null;
        };
    }

    static Long refundedAmountFor(String status, long amountMinor) {
        return "REFUNDED".equals(status) ? amountMinor : null;
    }

    static String cancellationReasonFor(String status) {
        return "CANCELLED".equals(status) ? "seed-cancelled" : null;
    }

    static String refundReasonFor(String status) {
        return "REFUNDED".equals(status) ? "seed-refunded" : null;
    }

    static long versionFor(String status) {
        return switch (status) {
            case "CREATED" -> 0;
            case "AUTHORIZED", "CANCELLED" -> 1;
            case "CAPTURED", "EXPIRED" -> 2;
            case "REFUNDED" -> 3;
            default -> throw new IllegalArgumentException(status);
        };
    }

    static UUID historyIdFor(int paymentIndex, int step) {
        return DataLearningFixtures.nameUuid("data-learning:history:" + paymentIndex + ":" + step);
    }

    static String idempotencyHash(int paymentIndex, int step) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("data-learning:idem:" + paymentIndex + ":" + step).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HistoryStep authorize(int index, Instant createdAt, Instant authorizedAt, long amountMinor) {
        return new HistoryStep(index, 1, "CREATED", "AUTHORIZED", "AUTHORIZE", authorizedAt, amountMinor, null);
    }

    private static HistoryStep capture(int index, Instant authorizedAt, Instant capturedAt, long amountMinor) {
        return new HistoryStep(index, 2, "AUTHORIZED", "CAPTURED", "CAPTURE", capturedAt, amountMinor, null);
    }

    record HistoryStep(
            int paymentIndex,
            int step,
            String fromStatus,
            String toStatus,
            String action,
            Instant at,
            Long amountMinor,
            String reason
    ) {
        UUID id() {
            return historyIdFor(paymentIndex, step);
        }

        UUID paymentId() {
            return paymentIdFor(paymentIndex);
        }

        String idempotencyKeyHash() {
            return action == null ? null : idempotencyHash(paymentIndex, step);
        }
    }
}
