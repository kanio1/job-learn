package lab.paymentquality.checkoutlab.internal.web;

import lab.paymentquality.checkoutlab.internal.application.CheckoutLabNotifyReceiverService;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabTransientException;
import lab.paymentquality.checkoutlab.internal.application.InvalidCheckoutSignatureException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout-lab")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabNotifyController {

    private final CheckoutLabNotifyReceiverService receiverService;

    CheckoutLabNotifyController(CheckoutLabNotifyReceiverService receiverService) {
        this.receiverService = receiverService;
    }

    @PostMapping("/notify")
    ResponseEntity<?> notify(
            HttpServletRequest request,
            @RequestHeader(value = "Lab-Event-Id", required = false) String eventId,
            @RequestHeader(value = "Lab-Signature", required = false) String signature)
            throws IOException {
        byte[] rawBody = request.getInputStream().readAllBytes();
        try {
            CheckoutLabNotifyReceiverService.NotifyResult result =
                    receiverService.receive(rawBody, eventId, signature);
            if (result.duplicate()) {
                return ResponseEntity.ok(Map.of("duplicate", true, "eventId", result.eventId()));
            }
            return ResponseEntity.accepted().body(Map.of("duplicate", false, "eventId", result.eventId()));
        } catch (InvalidCheckoutSignatureException ex) {
            throw ex;
        } catch (CheckoutLabTransientException ex) {
            throw ex;
        }
    }
}
