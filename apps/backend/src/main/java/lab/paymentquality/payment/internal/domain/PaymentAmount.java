package lab.paymentquality.payment.internal.domain;

public record PaymentAmount(long minorUnits) {

    private static final long MIN_MINOR_UNITS = 1;
    private static final long MAX_MINOR_UNITS = 100_000_000;

    public static PaymentAmount of(long minorUnits) {
        if (minorUnits < MIN_MINOR_UNITS || minorUnits > MAX_MINOR_UNITS) {
            throw new InvalidPaymentAmountException(minorUnits);
        }
        return new PaymentAmount(minorUnits);
    }

    public long minorUnits() {
        return minorUnits;
    }
}
