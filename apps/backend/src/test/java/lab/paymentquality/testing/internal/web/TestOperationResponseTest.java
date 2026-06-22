package lab.paymentquality.testing.internal.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;

class TestOperationResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void seedResponseHasCorrectFieldValues() {
        TestOperationResponse response = TestOperationResponse.completed("seed");

        assertThat(response.operation()).isEqualTo("seed");
        assertThat(response.status()).isEqualTo("completed");
    }

    @Test
    void resetResponseHasCorrectFieldValues() {
        TestOperationResponse response = TestOperationResponse.completed("reset");

        assertThat(response.operation()).isEqualTo("reset");
        assertThat(response.status()).isEqualTo("completed");
    }

    @Test
    void serializedJsonContainsExactlyTwoFields() throws Exception {
        TestOperationResponse response = TestOperationResponse.completed("seed");
        String json = MAPPER.writeValueAsString(response);
        JsonNode node = MAPPER.readTree(json);

        assertThat(node.size()).isEqualTo(2);
        assertThat(node.has("operation")).isTrue();
        assertThat(node.has("status")).isTrue();
    }

    @Test
    void serializedJsonMatchesExpectedShape() throws Exception {
        TestOperationResponse seed = TestOperationResponse.completed("seed");
        String json = MAPPER.writeValueAsString(seed);
        JsonNode node = MAPPER.readTree(json);

        assertThat(node.get("operation").asText()).isEqualTo("seed");
        assertThat(node.get("status").asText()).isEqualTo("completed");
    }

    @Test
    void recordHasExactlyTwoComponentsNamedOperationAndStatus() {
        RecordComponent[] components = TestOperationResponse.class.getRecordComponents();

        assertThat(components).hasSize(2);
        assertThat(components[0].getName()).isEqualTo("operation");
        assertThat(components[1].getName()).isEqualTo("status");
    }

    @Test
    void serializedResponseContainsNoForbiddenFields() throws Exception {
        String json = MAPPER.writeValueAsString(TestOperationResponse.completed("seed"));

        assertThat(json).doesNotContainIgnoringCase("tenantId");
        assertThat(json).doesNotContainIgnoringCase("merchantId");
        assertThat(json).doesNotContainIgnoringCase("paymentOrderId");
        assertThat(json).doesNotContainIgnoringCase("count");
        assertThat(json).doesNotContainIgnoringCase("records");
        assertThat(json).doesNotContainIgnoringCase("accessToken");
        assertThat(json).doesNotContainIgnoringCase("refreshToken");
        assertThat(json).doesNotContainIgnoringCase("authorization");
        assertThat(json).doesNotContainIgnoringCase("bearer");
        assertThat(json).doesNotContainIgnoringCase("clientSecret");
        assertThat(json).doesNotContainIgnoringCase("password");
        assertThat(json).doesNotContainIgnoringCase("temporaryPassword");
        assertThat(json).doesNotContainIgnoringCase("pan");
        assertThat(json).doesNotContainIgnoringCase("cvv");
        assertThat(json).doesNotContainIgnoringCase("payload");
        assertThat(json).doesNotContainIgnoringCase("requestBody");
        assertThat(json).doesNotContainIgnoringCase("responseBody");
        assertThat(json).doesNotContainIgnoringCase("actorSubject");
    }
}
