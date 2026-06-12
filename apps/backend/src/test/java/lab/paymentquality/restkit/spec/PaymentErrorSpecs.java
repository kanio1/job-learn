package lab.paymentquality.restkit.spec;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;

public final class PaymentErrorSpecs {

    private static final String PROBLEM_JSON = "application/problem+json";

    private PaymentErrorSpecs(){
    }

    public static ResponseSpecification problem(int statusCode) {
        return new ResponseSpecBuilder()
            .expectStatusCode(statusCode)
            .expectHeader(ApiHeaders.CONTENT_TYPE, containsString(PROBLEM_JSON))
            .expectBody("status", equalTo(statusCode))
            .build();
    }

    public static ResponseSpecification badRequestProblem() {
        return problem(400);
    }

    public static ResponseSpecification validationProblem() {
        return problem(400);
    }

    public static ResponseSpecification idempotencyConflict() {
        return problem(409);
    }

    public static ResponseSpecification methodNotAllowed() {
        return problem(405);
    }

    public static ResponseSpecification notAcceptable() {
        return problem(406);
    }

    public static ResponseSpecification unsupportedMediaType() {
        return problem(415);
    }

}
