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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderIdempotencyConcurrencyTest {

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

    @Test
    void concurrentSameKeyCreatesExactlyOneOrder() throws Exception {
        UUID merchantId = UUID.randomUUID();
        PaymentActorContext actor = new PaymentActorContext("test-subject");
        IdempotencyKey key = IdempotencyKey.of("idem-concurrent");

        when(merchantEligibilityService.findEligibility(merchantId))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(merchantId, "MERCH-001", true)));
        when(idempotencyRecordRepository.findByMerchantIdAndIdempotencyKeyHash(any(), any()))
                .thenReturn(Optional.empty());
        when(paymentOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyRecordRepository.reserveIfAbsent(any(), any(), any(), any())).thenReturn(1);
        when(idempotencyRecordRepository.complete(any(), any(), any())).thenReturn(1);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<UUID> createdOrderId = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PaymentCreateResult result = service.create(merchantId,
                            PaymentAmount.of(12500), CurrencyCode.of("PLN"),
                            ClientOrderReference.of("PAY-001"), key, actor, "corr-concurrent");
                    successCount.incrementAndGet();
                    createdOrderId.compareAndSet(null, result.paymentOrder().getPaymentOrderId());
                } catch (DataIntegrityViolationException | IdempotencyConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
    }
}
