package lab.paymentquality.payment.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank(message = "Note body must not be blank")
        @Size(max = 2000, message = "Note body must not exceed 2000 characters")
        String body) {
}
