package lab.paymentquality.apitest.api.payment.dto;

/**
 * Test-side request DTO for creating a payment order.
 *
 * <p>Mirrors the backend's {@code CreatePaymentOrderRequest} record without importing backend
 * classes. Jackson serializes this record to JSON for the POST body.
 *
 * <p><strong>Backend validation rules (confirmed from {@code CreatePaymentOrderRequest}):</strong>
 * <ul>
 *   <li>{@code amountMinor} — {@code @NotNull}, {@code @Min(1)}, {@code @Max(100_000_000)};
 *       minor currency units (e.g. 1250 = 12.50 PLN)</li>
 *   <li>{@code currency} — {@code @NotBlank}, {@code @Size(min=3, max=3)}; ISO 4217 code</li>
 *   <li>{@code clientOrderReference} — {@code @NotBlank}, {@code @Size(max=120)};
 *       merchant's own unique reference for this payment</li>
 * </ul>
 *
 * <p>SDET note: factory methods encode domain knowledge about what makes a valid vs invalid
 * request, keeping test methods readable. A test says {@code CreatePaymentOrderRequest.valid(...)}
 * rather than manually building every field, and the factory name communicates intent.
 */
public record CreatePaymentOrderRequest(
        Long amountMinor,
        String currency,
        String clientOrderReference) {

    /** Creates a valid request. All fields satisfy backend bean validation. */
    public static CreatePaymentOrderRequest valid(
            long amountMinor, String currency, String clientOrderReference) {
        return new CreatePaymentOrderRequest(amountMinor, currency, clientOrderReference);
    }

    /**
     * Creates a request with a blank currency string — triggers backend bean validation failure.
     * Expected: 400 with {@code error: "validation"}.
     */
    public static CreatePaymentOrderRequest withBlankCurrency(
            long amountMinor, String clientOrderReference) {
        return new CreatePaymentOrderRequest(amountMinor, " ", clientOrderReference);
    }
}
