package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderStatusHistoryRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationServiceTest {

    @Mock
    private JpaPaymentOrderRepository paymentOrderRepository;
    @Mock
    private JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentExpirationService service;

    @Test
    void expiresOverdueAuthorizedOrdersAndPublishesAuditDiff() {
        UUID orderId = UUID.randomUUID();
        Instant pastExpiry = Instant.now().minusSeconds(60);
        PaymentOrder overdueOrder = PaymentOrder.seeded(
                orderId, UUID.randomUUID(), "PAY-EXPIRE-001", 5000, "EUR",
                PaymentStatus.AUTHORIZED, 1L, Instant.now().minusSeconds(3600), Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(3600), pastExpiry, null, null, null, null, null, null, null);

        when(paymentOrderRepository.findAllByStatusAndExpiresAtBefore(eq(PaymentStatus.AUTHORIZED), any(Instant.class)))
                .thenReturn(List.of(overdueOrder));

        int expiredCount = service.expireOverdueAuthorizations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(overdueOrder.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(paymentOrderRepository).saveAndFlush(overdueOrder);
        verify(statusHistoryRepository).saveAndFlush(any());

        ArgumentCaptor<AuditableActionOccurred> captor = ArgumentCaptor.forClass(AuditableActionOccurred.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AuditableActionOccurred published = captor.getValue();
        assertThat(published.action()).isEqualTo("PAYMENT_EXPIRED");
        assertThat(published.targetType()).isEqualTo("PAYMENT_ORDER");
        assertThat(published.targetId()).isEqualTo(orderId.toString());
        assertThat(published.beforeState()).containsExactly(Map.entry("status", "AUTHORIZED"));
        assertThat(published.afterState()).containsExactly(Map.entry("status", "EXPIRED"));
    }

    @Test
    void noOverdueOrdersMeansNoSideEffects() {
        when(paymentOrderRepository.findAllByStatusAndExpiresAtBefore(eq(PaymentStatus.AUTHORIZED), any(Instant.class)))
                .thenReturn(List.of());

        int expiredCount = service.expireOverdueAuthorizations();

        assertThat(expiredCount).isZero();
        verifyNoInteractions(statusHistoryRepository, eventPublisher);
    }

    @Test
    void sweepsMultipleOverdueOrdersIndependently() {
        Instant pastExpiry = Instant.now().minusSeconds(60);
        PaymentOrder orderA = PaymentOrder.seeded(
                UUID.randomUUID(), UUID.randomUUID(), "PAY-EXPIRE-A", 1000, "EUR",
                PaymentStatus.AUTHORIZED, 1L, Instant.now(), Instant.now(),
                Instant.now(), pastExpiry, null, null, null, null, null, null, null);
        PaymentOrder orderB = PaymentOrder.seeded(
                UUID.randomUUID(), UUID.randomUUID(), "PAY-EXPIRE-B", 2000, "EUR",
                PaymentStatus.AUTHORIZED, 1L, Instant.now(), Instant.now(),
                Instant.now(), pastExpiry, null, null, null, null, null, null, null);

        when(paymentOrderRepository.findAllByStatusAndExpiresAtBefore(eq(PaymentStatus.AUTHORIZED), any(Instant.class)))
                .thenReturn(List.of(orderA, orderB));

        int expiredCount = service.expireOverdueAuthorizations();

        assertThat(expiredCount).isEqualTo(2);
        assertThat(orderA.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(orderB.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(eventPublisher, org.mockito.Mockito.times(2))
                .publishEvent(any(AuditableActionOccurred.class));
    }
}
