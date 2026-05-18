package lab.paymentquality.foundation.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
class StatusController {

    @GetMapping
    StatusResponse status() {
        return new StatusResponse("payment-quality-lab", "foundation", "UP");
    }
}
