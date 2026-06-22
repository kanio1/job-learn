package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.tenant.TenantSeedCapability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeterministicDatasetTest {

    @Mock
    TenantSeedCapability tenants;

    @Mock
    MerchantSeedCapability merchants;

    @Mock
    PaymentSeedCapability payments;

    @InjectMocks
    DeterministicDataset dataset;

    @Test
    void resetCallsClearInPaymentMerchantTenantOrder() {
        dataset.reset();

        InOrder order = inOrder(payments, merchants, tenants);
        order.verify(payments).clear();
        order.verify(merchants).clear();
        order.verify(tenants).clear();
        verifyNoMoreInteractions(tenants, merchants, payments);
    }

    @Test
    void seedClearsInFkReverseOrderThenSeedsInFkForwardOrder() {
        dataset.seed();

        InOrder order = inOrder(payments, merchants, tenants);
        order.verify(payments).clear();
        order.verify(merchants).clear();
        order.verify(tenants).clear();
        order.verify(tenants).seed(Fixtures.tenants());
        order.verify(merchants).seed(Fixtures.merchants());
        order.verify(payments).seed(Fixtures.paymentOrders());
        verifyNoMoreInteractions(tenants, merchants, payments);
    }

    @Test
    void datasetDependsOnlyOnPublicCapabilityInterfaces() {
        Set<Class<?>> fieldTypes = Arrays.stream(DeterministicDataset.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .collect(Collectors.toSet());

        assertThat(fieldTypes).containsExactlyInAnyOrder(
                TenantSeedCapability.class,
                MerchantSeedCapability.class,
                PaymentSeedCapability.class);
    }
}
