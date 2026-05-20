package lab.paymentquality.merchant.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantStatusTest {

    @Test
    void draftCanTransitionToActive() {
        assertThat(MerchantStatus.DRAFT.canTransitionTo(MerchantStatus.ACTIVE)).isTrue();
    }

    @Test
    void draftCannotTransitionToSuspended() {
        assertThat(MerchantStatus.DRAFT.canTransitionTo(MerchantStatus.SUSPENDED)).isFalse();
    }

    @Test
    void draftCannotTransitionToItself() {
        assertThat(MerchantStatus.DRAFT.canTransitionTo(MerchantStatus.DRAFT)).isFalse();
    }

    @Test
    void activeCanTransitionToSuspended() {
        assertThat(MerchantStatus.ACTIVE.canTransitionTo(MerchantStatus.SUSPENDED)).isTrue();
    }

    @Test
    void activeCannotTransitionToDraft() {
        assertThat(MerchantStatus.ACTIVE.canTransitionTo(MerchantStatus.DRAFT)).isFalse();
    }

    @Test
    void activeCannotTransitionToItself() {
        assertThat(MerchantStatus.ACTIVE.canTransitionTo(MerchantStatus.ACTIVE)).isFalse();
    }

    @Test
    void suspendedCannotTransitionAnywhere() {
        assertThat(MerchantStatus.SUSPENDED.canTransitionTo(MerchantStatus.DRAFT)).isFalse();
        assertThat(MerchantStatus.SUSPENDED.canTransitionTo(MerchantStatus.ACTIVE)).isFalse();
        assertThat(MerchantStatus.SUSPENDED.canTransitionTo(MerchantStatus.SUSPENDED)).isFalse();
    }
}
