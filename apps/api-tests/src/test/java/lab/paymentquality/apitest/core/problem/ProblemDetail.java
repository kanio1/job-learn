package lab.paymentquality.apitest.core.problem;

import java.util.List;

/**
 * Test-side representation of an {@code application/problem+json} error response.
 *
 * <p>This record mirrors the union of all backend error shapes:
 * <ul>
 *   <li>{@code GlobalExceptionHandler} returns: {@code type}, {@code title}, {@code status},
 *       {@code detail}, {@code correlationId}, {@code error}.</li>
 *   <li>{@code PaymentExceptionHandler} additionally returns: {@code code}, {@code message},
 *       {@code details} (list of {@link FieldError}).</li>
 *   <li>{@code MerchantExceptionHandler} returns: {@code error}, {@code message}, {@code details}
 *       (as a field-to-message map, serialized as object).</li>
 * </ul>
 *
 * <p>All fields are nullable — use {@link ProblemAssert} to assert the fields relevant to each test.
 *
 * <p>Do NOT import {@code org.springframework.http.ProblemDetail} here.
 * This is a local test-side record only.
 *
 * <p>SDET learning: a local record decouples the test framework from backend implementation details.
 * If the backend changes a field name, only this record and its usages change — not every test.
 */
public record ProblemDetail(
        String type,
        String title,
        Integer status,
        String detail,
        String message,
        String error,
        String code,
        String correlationId,
        List<FieldError> details
) {

    /** Field-level validation error, present in {@code 400} validation responses. */
    public record FieldError(String field, String message) {}
}
