package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.etl.PaymentEtlPipeline;
import lab.paymentquality.testing.internal.seed.DataLearningDataset;
import lab.paymentquality.testing.internal.seed.DataLearningProfile;
import lab.paymentquality.testing.internal.seed.DeterministicDataset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
@Profile("!prod")
class TestController {

    private final DeterministicDataset dataset;
    private final DataLearningDataset learningDataset;
    private final PaymentEtlPipeline paymentEtlPipeline;

    TestController(DeterministicDataset dataset,
                   DataLearningDataset learningDataset,
                   PaymentEtlPipeline paymentEtlPipeline) {
        this.dataset = dataset;
        this.learningDataset = learningDataset;
        this.paymentEtlPipeline = paymentEtlPipeline;
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

    @PostMapping("/seed-learning")
    ResponseEntity<?> seedLearning(@RequestParam(value = "profile", required = false) String profile) {
        if (profile != null && !profile.isBlank() && !"SMALL".equals(profile)) {
            return TestingProblems.validation("Learning seed profile must be SMALL");
        }
        var truth = learningDataset.seed(DataLearningProfile.SMALL);
        return ResponseEntity.ok(LearningSeedResponse.completed(truth));
    }

    @PostMapping("/etl/payments/full")
    ResponseEntity<EtlOperationResponse> etlPaymentsFull() {
        var result = paymentEtlPipeline.runFull();
        return ResponseEntity.ok(EtlOperationResponse.completed("etl-payments-full", result));
    }

    @PostMapping("/etl/payments/incremental")
    ResponseEntity<EtlOperationResponse> etlPaymentsIncremental() {
        var result = paymentEtlPipeline.runIncremental();
        return ResponseEntity.ok(EtlOperationResponse.completed("etl-payments-incremental", result));
    }

    @PostMapping("/etl/payments/rebuild")
    ResponseEntity<EtlOperationResponse> etlPaymentsRebuild() {
        var result = paymentEtlPipeline.rebuild();
        return ResponseEntity.ok(EtlOperationResponse.completed("etl-payments-rebuild", result));
    }
}
