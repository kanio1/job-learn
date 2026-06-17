package lab.paymentquality.payment.internal.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Serializes the metadata map as a JSON object string.
     * Returns null when metadata is null.
     */
    public String metadataAsJson() {
        if (metadata == null) {
            return null;
        }
        String entries = metadata.entrySet().stream()
                .map(e -> "\"" + escapeJson(e.getKey()) + "\":\"" + escapeJson(e.getValue()) + "\"")
                .collect(Collectors.joining(","));
        return "{" + entries + "}";
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
