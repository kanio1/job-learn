package lab.paymentquality.payment.internal.web;

import java.util.Set;
import java.util.TreeSet;

public class UnknownMetadataPatchFieldException extends RuntimeException {

    private final Set<String> fieldNames;

    public UnknownMetadataPatchFieldException(Set<String> fieldNames) {
        super("Metadata PATCH supports only the top-level metadata field");
        this.fieldNames = Set.copyOf(new TreeSet<>(fieldNames));
    }

    public Set<String> fieldNames() {
        return fieldNames;
    }
}
