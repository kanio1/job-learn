package lab.paymentquality.checkoutlab.internal.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout-lab")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabHealthController {

    @GetMapping("/health")
    ResponseEntity<CheckoutLabHealthResponse> health() {
        return ResponseEntity.ok(new CheckoutLabHealthResponse("UP"));
    }
}
