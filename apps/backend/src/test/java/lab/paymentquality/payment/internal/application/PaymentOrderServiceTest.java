package lab.paymentquality.payment.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibility;
import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import lab.paymentquality.payment.internal.domain.*;
import lab.paymentquality.payment.internal.infrastructure.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    @Mock
    private JpaPaymentOrderRepository paymentOrderRepository;
    @Mock
    private JpaIdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private MerchantPaymentEligibilityService merchantEligibilityService;

    @InjectMocks
    private PaymentOrderService service;

    private final UUID merchantId = UUID.randomUUID();
    private final PaymentActorContext actor = new PaymentActorContext("test-subject");

    @Test
    void createSucceedsForActiveMerchant() {
        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(any(), any()))
                .thenReturn(Optional.empty());
        when(paymentOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyRecordRepository.reserveIfAbsent(any(), any(), any(), any())).thenReturn(1);
        when(idempotencyRecordRepository.complete(any(), any(), any())).thenReturn(1);

        PaymentCreateResult result = service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), IdempotencyKey.of("idem-001"),
                actor, "corr-001");

        assertThat(result.created()).isTrue();
        assertThat(result.paymentOrder().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(result.paymentOrder().getAmountMinor()).isEqualTo(12500);
        verify(paymentOrderRepository).saveAndFlush(any());
        verify(statusHistoryRepository).saveAndFlush(any());
        verify(idempotencyRecordRepository).reserveIfAbsent(any(), any(), any(), any());
        verify(idempotencyRecordRepository).complete(any(), any(), any());
    }

    @Test
    void createReplaysSameKeySameFingerprint() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder existingOrder = PaymentOrder.create(paymentOrderId, merchantId, "PAY-001", 12500, "PLN");
        RequestFingerprint fingerprint = RequestFingerprint.of(merchantId, 12500, "PLN", "PAY-001");
        IdempotencyKey key = IdempotencyKey.of("idem-001");

        IdempotencyRecord existingRecord = IdempotencyRecord.reserve(
                UUID.randomUUID(), merchantId, key.keyHash(), fingerprint.fingerprintHash());
        existingRecord.complete(paymentOrderId);

        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(merchantId, key.keyHash()))
                .thenReturn(Optional.of(existingRecord));
        when(paymentOrderRepository.findByPaymentOrderId(paymentOrderId))
                .thenReturn(Optional.of(existingOrder));

        PaymentCreateResult result = service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), key, actor, "corr-002");

        assertThat(result.created()).isFalse();
        assertThat(result.paymentOrder().getPaymentOrderId()).isEqualTo(paymentOrderId);
        verify(paymentOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createConflictSameKeyDifferentFingerprint() {
        IdempotencyKey key = IdempotencyKey.of("idem-001");
        RequestFingerprint differentFingerprint = RequestFingerprint.of(merchantId, 99999, "EUR", "PAY-999");

        IdempotencyRecord existingRecord = IdempotencyRecord.reserve(
                UUID.randomUUID(), merchantId, key.keyHash(), differentFingerprint.fingerprintHash());

        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(merchantId, key.keyHash()))
                .thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), key, actor, "corr-003"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void createInProgressWhenResolveFindsIncompleteRecord() {
        IdempotencyKey key = IdempotencyKey.of("idem-race-a");
        RequestFingerprint fingerprint = RequestFingerprint.of(merchantId, 12500, "PLN", "PAY-001");
        // Reserved but not completed — paymentOrderId is null
        IdempotencyRecord incompleteRecord = IdempotencyRecord.reserve(
                UUID.randomUUID(), merchantId, key.keyHash(), fingerprint.fingerprintHash());

        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        // First call (initial lookup) → empty; second call (resolveExisting) → incomplete record
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(merchantId, key.keyHash()))
                .thenReturn(Optional.empty(), Optional.of(incompleteRecord));
        when(idempotencyRecordRepository.reserveIfAbsent(any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), key, actor, "corr-race-a"))
                .isInstanceOf(IdempotencyCreateInProgressException.class);
    }

    @Test
    void createInProgressWhenInitialLookupFindsIncompleteRecord() {
        IdempotencyKey key = IdempotencyKey.of("idem-race-b");
        RequestFingerprint fingerprint = RequestFingerprint.of(merchantId, 12500, "PLN", "PAY-001");
        // Reserved but not completed — paymentOrderId is null
        IdempotencyRecord incompleteRecord = IdempotencyRecord.reserve(
                UUID.randomUUID(), merchantId, key.keyHash(), fingerprint.fingerprintHash());

        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(merchantId, key.keyHash()))
                .thenReturn(Optional.of(incompleteRecord));

        assertThatThrownBy(() -> service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), key, actor, "corr-race-b"))
                .isInstanceOf(IdempotencyCreateInProgressException.class);
    }

    @Test
    void createRejectsNonActiveMerchant() {
        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", false)));

        assertThatThrownBy(() -> service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), IdempotencyKey.of("idem-001"),
                actor, "corr-004"))
                .isInstanceOf(MerchantNotPaymentEligibleException.class);
    }

    @Test
    void createRejectsUnknownMerchant() {
        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(merchantId,
                PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                ClientOrderReference.of("PAY-001"), IdempotencyKey.of("idem-001"),
                actor, "corr-005"))
                .isInstanceOf(MerchantNotPaymentEligibleException.class);
    }

    @Test
    void findForMerchantSucceeds() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-001", 12500, "PLN");
        when(paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId))
                .thenReturn(Optional.of(order));

        PaymentOrder result = service.findForMerchant(merchantId, paymentOrderId);

        assertThat(result.getPaymentOrderId()).isEqualTo(paymentOrderId);
    }

    @Test
    void findForMerchantThrowsNotFound() {
        UUID paymentOrderId = UUID.randomUUID();
        when(paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findForMerchant(merchantId, paymentOrderId))
                .isInstanceOf(PaymentOrderNotFoundException.class);
    }

    @Test
    void findForPlatformSucceeds() {
        UUID paymentOrderId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.create(paymentOrderId, merchantId, "PAY-001", 12500, "PLN");
        when(paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId))
                .thenReturn(Optional.of(order));

        PaymentOrder result = service.findForPlatform(merchantId, paymentOrderId);

        assertThat(result.getPaymentOrderId()).isEqualTo(paymentOrderId);
    }
}
