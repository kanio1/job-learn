package lab.paymentquality.iam.internal.web.dto;

import jakarta.validation.constraints.Email;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UpdateUserRequest(
        @Email String email,
        Boolean enabled,
        Map<String, List<String>> attributes) {

    public UpdateUserRequest {
        if (attributes != null) {
            Map<String, List<String>> copy = new LinkedHashMap<>();
            attributes.forEach((key, values) -> copy.put(
                    key,
                    values == null ? List.of() : List.copyOf(values)));
            attributes = Map.copyOf(copy);
        }
    }
}
