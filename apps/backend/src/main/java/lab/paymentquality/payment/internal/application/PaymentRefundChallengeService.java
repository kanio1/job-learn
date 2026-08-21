package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentRefundApproval;
import lab.paymentquality.payment.internal.domain.PaymentRefundApprovalNotFoundException;
import lab.paymentquality.payment.internal.domain.PaymentRefundChallenge;
import lab.paymentquality.payment.internal.domain.RefundChallengeException;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundApprovalRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentRefundChallengeRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentRefundChallengeService {

    public static final long PIN_THRESHOLD_MINOR = 100_000L;
    private static final int PIN_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration TTL = Duration.ofSeconds(90);
    private static final Duration LOCKOUT = Duration.ofMinutes(5);

    private final JpaPaymentRefundApprovalRepository approvalRepository;
    private final JpaPaymentRefundChallengeRepository challengeRepository;
    private final PaymentOrderService paymentOrderService;
    private final Environment environment;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public PaymentRefundChallengeService(
            JpaPaymentRefundApprovalRepository approvalRepository,
            JpaPaymentRefundChallengeRepository challengeRepository,
            PaymentOrderService paymentOrderService,
            Environment environment) {
        this.approvalRepository = approvalRepository;
        this.challengeRepository = challengeRepository;
        this.paymentOrderService = paymentOrderService;
        this.environment = environment;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public CreatedChallenge create(UUID merchantId, UUID paymentOrderId) {
        PaymentOrder order = paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        PaymentRefundApproval approval = latestPending(paymentOrderId);
        long amount = amountOf(approval, order);
        if (amount <= PIN_THRESHOLD_MINOR) {
            throw new RefundChallengeException(HttpStatus.BAD_REQUEST, "pin_not_required",
                    "PIN is not required for this refund amount");
        }
        var existing = challengeRepository.findByApprovalIdAndVerifiedAtIsNull(approval.getApprovalId());
        if (existing.isPresent()) {
            return new CreatedChallenge(existing.get(), null);
        }
        Instant now = clock.instant();
        String pin = generatePin();
        PaymentRefundChallenge challenge = PaymentRefundChallenge.open(
                approval.getApprovalId(),
                paymentOrderId,
                passwordEncoder.encode(pin),
                now.plus(TTL),
                now);
        challengeRepository.saveAndFlush(challenge);
        return new CreatedChallenge(challenge, exposePin() ? pin : null);
    }

    @Transactional(noRollbackFor = RefundChallengeException.class)
    public PaymentRefundChallenge verify(UUID merchantId, UUID paymentOrderId, UUID challengeId,
                                         String pin, String actorSubject) {
        paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        PaymentRefundChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RefundChallengeException(HttpStatus.NOT_FOUND, "not_found",
                        "Refund challenge not found"));
        if (!challenge.getPaymentOrderId().equals(paymentOrderId)) {
            throw new RefundChallengeException(HttpStatus.NOT_FOUND, "not_found", "Refund challenge not found");
        }
        PaymentRefundApproval approval = approvalRepository.findById(challenge.getApprovalId())
                .orElseThrow(() -> new PaymentRefundApprovalNotFoundException(challenge.getApprovalId()));
        if (approval.getMakerSubject().equals(actorSubject)) {
            throw new RefundChallengeException(HttpStatus.CONFLICT, "dual_control_self_approve",
                    "Maker cannot verify the PIN for their own refund request");
        }
        Instant now = clock.instant();
        if (challenge.isVerified()) {
            throw new RefundChallengeException(HttpStatus.CONFLICT, "already_verified",
                    "Refund challenge already verified");
        }
        if (challenge.isLocked(now)) {
            throw new RefundChallengeException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many PIN attempts", challenge.getLockedUntil());
        }
        if (challenge.isExpired(now)) {
            throw new RefundChallengeException(HttpStatus.BAD_REQUEST, "expired", "Refund challenge expired");
        }
        if (pin == null || !passwordEncoder.matches(pin, challenge.getPinHash())) {
            challenge.registerFailure(MAX_ATTEMPTS, now.plus(LOCKOUT), now);
            challengeRepository.saveAndFlush(challenge);
            if (challenge.getAttemptCount() >= MAX_ATTEMPTS || challenge.isLocked(now)) {
                throw new RefundChallengeException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                        "Too many PIN attempts", challenge.getLockedUntil());
            }
            throw new RefundChallengeException(HttpStatus.BAD_REQUEST, "invalid_pin", "Invalid PIN");
        }
        challenge.markVerified(now);
        challengeRepository.saveAndFlush(challenge);
        return challenge;
    }

    @Transactional(readOnly = true)
    public boolean isVerifiedForApproval(UUID approvalId) {
        return challengeRepository.existsByApprovalIdAndVerifiedAtIsNotNull(approvalId);
    }

    public static boolean requiresPin(Long amountMinor) {
        return amountMinor != null && amountMinor > PIN_THRESHOLD_MINOR;
    }

    private PaymentRefundApproval latestPending(UUID paymentOrderId) {
        return approvalRepository.findByPaymentOrderIdOrderByCreatedAtDesc(paymentOrderId).stream()
                .filter(approval -> approval.getStatus() == PaymentRefundApproval.Status.PENDING)
                .findFirst()
                .orElseThrow(() -> new RefundChallengeException(HttpStatus.BAD_REQUEST, "pin_not_required",
                        "No pending refund approval requires a PIN"));
    }

    private static long amountOf(PaymentRefundApproval approval, PaymentOrder order) {
        if (approval.getAmountMinor() != null) {
            return approval.getAmountMinor();
        }
        return order.getAmountMinor();
    }

    private String generatePin() {
        int bound = (int) Math.pow(10, PIN_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + PIN_LENGTH + "d", value);
    }

    private boolean exposePin() {
        Boolean override = environment.getProperty("payment.refund-challenge.expose-pin", Boolean.class);
        if (override != null) {
            return override;
        }
        return environment.matchesProfiles("dev") || environment.matchesProfiles("test");
    }

    public record CreatedChallenge(PaymentRefundChallenge challenge, String pin) {
    }
}
