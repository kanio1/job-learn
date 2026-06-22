package lab.paymentquality.testing.internal.web;

public record TestOperationResponse(String operation, String status) {

    static TestOperationResponse completed(String operation) {
        return new TestOperationResponse(operation, "completed");
    }
}
