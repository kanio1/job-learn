package lab.paymentquality.merchant.internal.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantTest {

    @Test
    void newMerchantHasDraftStatus() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        assertThat(m.getStatus()).isEqualTo(MerchantStatus.DRAFT);
    }

    @Test
    void newMerchantHasBothTimestampsInitialized() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        assertThat(m.getCreatedAt()).isNotNull();
        assertThat(m.getUpdatedAt()).isEqualTo(m.getCreatedAt());
    }

    @Test
    void activateTransitionsToActive() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        assertThat(m.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
    }

    @Test
    void activateUpdatesTimestamp() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        var before = m.getUpdatedAt();
        m.activate();
        assertThat(m.getUpdatedAt()).isAfter(before);
    }

    @Test
    void suspendTransitionsToSuspended() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        m.suspend();
        assertThat(m.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
    }

    @Test
    void suspendUpdatesTimestamp() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        var before = m.getUpdatedAt();
        m.suspend();
        assertThat(m.getUpdatedAt()).isAfter(before);
    }

    @Test
    void activateFromActiveThrows() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        assertThatThrownBy(m::activate)
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void activateFromSuspendedThrows() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        m.suspend();
        assertThatThrownBy(m::activate)
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void suspendFromDraftThrows() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        assertThatThrownBy(m::suspend)
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void suspendFromSuspendedThrows() {
        var m = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test Merchant");
        m.activate();
        m.suspend();
        assertThatThrownBy(m::suspend)
                .isInstanceOf(InvalidTransitionException.class);
    }
}
