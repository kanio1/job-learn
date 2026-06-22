package lab.paymentquality.restkit.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.restassured.response.Response;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;

public class ProblemDetailsAssertions {

    private ProblemDetailsAssertions() {
    }

    public static void assertProblemHasStandardFields(Response response) {
        assertThat(response.jsonPath().getString("type"))
            .as("Problem details 'type' should be present")
            .isNotBlank();
        
            assertThat(response.jsonPath().getString("title"))
            .as("Problem Details 'title' should be present")
            .isNotBlank();

        assertThat(response.jsonPath().getObject("status", Integer.class))
            .as("Problem Details 'status' should be present")
            .isNotNull();

        assertThat(response.jsonPath().getString("detail"))
            .as("Problem Details 'detail' should be present")
            .isNotBlank();

        assertThat(response.jsonPath().getString("correlationId"))
            .as("Problem Details 'correlationId' should be present")
            .isNotBlank();
    }

    public static void assertProblemStatusMatchesHttpStatus(Response response) {
        int httpStatus = response.statusCode();
        Integer problemStatus = response.jsonPath().getObject("status", Integer.class);

        assertThat(problemStatus)
            .as("Problem Details body.status should match HTTP status")
            .isEqualTo(httpStatus);
    }

    public static void assertProblemCorrelationIdMatchesHeader(Response response) {

        String headerCorrelationId = response.header(ApiHeaders.X_CORRELATION_ID);
        String bodyCorrelationId = response.jsonPath().getString("correlationId");

        assertThat(headerCorrelationId)
            .as("Resposnse header X-Correlation-ID should be present")
            .isNotBlank();

        assertThat(bodyCorrelationId)
            .as("Problem Details body.correlationId should match X-Correlation-ID header")
            .isNotBlank();
    }

    public static void assertProblemDoesNotLeakInternals(Response response) {
        String body = response.asString();

        assertThat(body)
            .as("Problem response should not leak Java/Spring/internal implementation details")
            .doesNotContain("java.lang.")
            .doesNotContain("java.util.")
            .doesNotContain("org.springframework")
            .doesNotContain("org.hibernate")
            .doesNotContain("jakarta.")
            .doesNotContain("Exception")
            .doesNotContain("StackTrace")
            .doesNotContain("at lab.")
            .doesNotContain("SQLException")
            .doesNotContain("PSQLException")
            .doesNotContain("password")
            .doesNotContain("secret")
            .doesNotContain("Bearer ");
    }

    public static void assertProblemError(Response response, String expectedError) {
        assertThat(response.jsonPath().getString("error"))
            .as("Problem Details compatibility field 'error'")
            .isEqualTo(expectedError);
    }

    public static void assertValidationDetailsContainField(Response response, String expectedField) {

        List<String> fields = response.jsonPath().getList("details.field", String.class);

        assertThat(fields)
            .as("Validation details should contain '%s'", expectedField)
            .isNotNull()
            .contains(expectedField);
    }

    public static void assertSafeProblem(Response response) {
        assertProblemHasStandardFields(response);
        assertProblemStatusMatchesHttpStatus(response);
        assertProblemCorrelationIdMatchesHeader(response);
        assertProblemDoesNotLeakInternals(response);
    }
    
}
