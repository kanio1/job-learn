package lab.paymentquality.payment.internal.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CreatePaymentOrderRequest {

    @NotNull(message = "amountMinor is required")
    @Min(value = 1, message = "amountMinor must be at least 1")
    @Max(value = 100000000, message = "amountMinor must be at most 100000000")
    private final Long amountMinor;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be exactly 3 characters")
    private final String currency;

    @NotBlank(message = "clientOrderReference is required")
    @Size(max = 120, message = "clientOrderReference must not exceed 120 characters")
    private final String clientOrderReference;

    private final Set<String> unknownTopLevelFields = new LinkedHashSet<>();

    @JsonCreator
    public CreatePaymentOrderRequest(
            @JsonProperty("amountMinor") Long amountMinor,
            @JsonProperty("currency") String currency,
            @JsonProperty("clientOrderReference") String clientOrderReference) {
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.clientOrderReference = clientOrderReference;
    }

    public Long amountMinor() {
        return amountMinor;
    }

    public String currency() {
        return currency;
    }

    public String clientOrderReference() {
        return clientOrderReference;
    }

    @JsonAnySetter
    void captureUnknownTopLevelField(String fieldName, Object ignoredValue) {
        unknownTopLevelFields.add(fieldName);
    }

    @JsonIgnore
    public Set<String> unknownTopLevelFields() {
        return Set.copyOf(unknownTopLevelFields);
    }

    public void requireKnownTopLevelFieldsOnly() {
        if (!unknownTopLevelFields.isEmpty()) {
            throw new UnknownCreatePaymentOrderFieldException(unknownTopLevelFields);
        }
    }
}
