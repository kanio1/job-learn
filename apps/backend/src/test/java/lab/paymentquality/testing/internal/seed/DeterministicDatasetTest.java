package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.support.SupportCaseSeedCapability;
import lab.paymentquality.tenant.TenantSeedCapability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Mock
    SupportCaseSeedCapability supportCases;

    @Mock
    JdbcTemplate jdbc;

    @InjectMocks
    DeterministicDataset dataset;

    @Test
    void resetClearsSupportCasesBeforeTheirMerchantAndPaymentParents() {
        dataset.reset();

        InOrder order = inOrder(supportCases, payments, merchants, tenants);
        order.verify(supportCases).clear();
        order.verify(payments).clear();
        order.verify(merchants).clear();
        order.verify(tenants).clear();
        verifyNoMoreInteractions(supportCases, tenants, merchants, payments);
    }

    @Test
    void seedClearsSupportCasesInFkReverseOrderThenSeedsInFkForwardOrder() {
        dataset.seed();

        InOrder order = inOrder(supportCases, payments, merchants, tenants);
        order.verify(supportCases).clear();
        order.verify(payments).clear();
        order.verify(merchants).clear();
        order.verify(tenants).clear();
        order.verify(tenants).seed(Fixtures.tenants());
        order.verify(merchants).seed(Fixtures.merchants());
        order.verify(payments).seed(Fixtures.paymentOrders());
        verifyNoMoreInteractions(supportCases, tenants, merchants, payments);
    }

    @Test
    void datasetDependsOnlyOnPublicCapabilityInterfaces() {
        Set<Class<?>> fieldTypes = Arrays.stream(DeterministicDataset.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .collect(Collectors.toSet());

        assertThat(fieldTypes).containsExactlyInAnyOrder(
                TenantSeedCapability.class,
                MerchantSeedCapability.class,
                PaymentSeedCapability.class,
                SupportCaseSeedCapability.class,
                JdbcTemplate.class);
    }
}
