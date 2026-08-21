package lab.paymentquality.merchant.internal.domain;

public class InvalidMerchantContactException extends RuntimeException {

    private final String field;

    public InvalidMerchantContactException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
