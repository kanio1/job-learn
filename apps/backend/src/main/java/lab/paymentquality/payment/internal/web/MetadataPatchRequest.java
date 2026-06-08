package lab.paymentquality.payment.internal.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class MetadataPatchRequest {

    private final Map<String, String> metadata;
    private final Set<String> unknownTopLevelFields = new LinkedHashSet<>();

    @JsonCreator
    public MetadataPatchRequest(@JsonProperty("metadata") Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    @JsonAnySetter
    void captureUnknownTopLevelField(String fieldName, Object ignoredValue) {
        unknownTopLevelFields.add(fieldName);
    }

    @JsonIgnore
    public Set<String> unknownTopLevelFields() {
        return Set.copyOf(unknownTopLevelFields);
    }

    public void requireOnlyMetadataTopLevelField() {
        if (!unknownTopLevelFields.isEmpty()) {
            throw new UnknownMetadataPatchFieldException(unknownTopLevelFields);
        }
    }
}
