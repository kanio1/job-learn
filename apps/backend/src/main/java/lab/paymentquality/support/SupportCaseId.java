package lab.paymentquality.support;

import java.util.Objects;
import java.util.UUID;

public record SupportCaseId(UUID value) {
    public SupportCaseId {
        Objects.requireNonNull(value, "case id required");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
