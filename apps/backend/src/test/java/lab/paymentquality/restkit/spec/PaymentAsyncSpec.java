package lab.paymentquality.restkit.spec;

import static org.hamcrest.Matchers.notNullValue;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;

public class PaymentAsyncSpec {

    private PaymentAsyncSpec() {
    }
    
    public static ResponseSpecification acceptedAsyncOperation() {
        return new ResponseSpecBuilder()
            .expectStatusCode(202)
            .expectBody(ApiHeaders.LOCATION, notNullValue())
            .build();
    }

}
