package lab.paymentquality.payment.internal.web;

import java.util.Set;
import java.util.TreeSet;

public class UnknownCreatePaymentOrderFieldException extends RuntimeException {

    private final Set<String> fieldNames;

    public UnknownCreatePaymentOrderFieldException(Set<String> fieldNames) {
        super("Create payment order supports only amountMinor, currency and clientOrderReference top-level fields");
        this.fieldNames = Set.copyOf(new TreeSet<>(fieldNames));
    }

    public Set<String> fieldNames() {
        return fieldNames;
    }
}
