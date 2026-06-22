package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.seed.DeterministicDataset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
@Profile("!prod")
class TestController {

    private final DeterministicDataset dataset;

    TestController(DeterministicDataset dataset) {
        this.dataset = dataset;
    }

    @PostMapping("/reset")
    ResponseEntity<TestOperationResponse> reset() {
        dataset.reset();
        return ResponseEntity.ok(TestOperationResponse.completed("reset"));
    }

    @PostMapping("/seed")
    ResponseEntity<TestOperationResponse> seed() {
        dataset.seed();
        return ResponseEntity.ok(TestOperationResponse.completed("seed"));
    }
}
