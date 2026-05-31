package lab.paymentquality.testsupport;

import lab.paymentquality.payment.internal.web.PaymentOrderListResponse;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.function.Predicate;

public class PaymentOrderAssertions extends AbstractAssert<PaymentOrderAssertions, PaymentOrderListResponse> {

    protected PaymentOrderAssertions(PaymentOrderListResponse actual) {
        super(actual, PaymentOrderAssertions.class);
    }

    public static PaymentOrderAssertions assertThat(PaymentOrderListResponse actual) {
        return new PaymentOrderAssertions(actual);
    }

    public PaymentOrderAssertions hasOnlyStatus(String status) {
        Assertions.assertThat(actual.content())
                .as("All payment orders should have status '%s'", status)
                .allMatch(item -> status.equals(item.status()));
        return this;
    }

    public PaymentOrderAssertions allAmountsGreaterThan(long min) {
        Assertions.assertThat(actual.content())
                .as("All payment orders should have amount > %d", min)
                .allMatch(item -> item.amountMinor() > min);
        return this;
    }

    public PaymentOrderAssertions hasPageMetadata(long expectedTotalElements, int expectedTotalPages) {
        Assertions.assertThat(actual.totalElements()).isEqualTo(expectedTotalElements);
        Assertions.assertThat(actual.totalPages()).isEqualTo(expectedTotalPages);
        return this;
    }

    public PaymentOrderAssertions hasContentSize(int expectedSize) {
        Assertions.assertThat(actual.content()).hasSize(expectedSize);
        return this;
    }
}
