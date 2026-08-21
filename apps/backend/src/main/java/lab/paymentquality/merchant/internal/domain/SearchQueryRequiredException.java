package lab.paymentquality.merchant.internal.domain;

public class SearchQueryRequiredException extends RuntimeException {

    public SearchQueryRequiredException() {
        super("q is required");
    }
}
