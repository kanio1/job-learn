package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.seed.DataLearningDataset;
import lab.paymentquality.testing.internal.seed.DataLearningProfile;
import lab.paymentquality.testing.internal.seed.DeterministicDataset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
@Profile("!prod")
class TestController {

    private static final String PROBLEM_BASE = "https://api.payment-quality.local/problems/";

    private final DeterministicDataset dataset;
    private final DataLearningDataset learningDataset;

    TestController(DeterministicDataset dataset, DataLearningDataset learningDataset) {
        this.dataset = dataset;
        this.learningDataset = learningDataset;
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
            return unsupportedProfile();
        }
        var truth = learningDataset.seed(DataLearningProfile.SMALL);
        return ResponseEntity.ok(LearningSeedResponse.completed(truth));
    }

    private static ResponseEntity<ProblemDetail> unsupportedProfile() {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Learning seed profile must be SMALL");
        body.setType(URI.create(PROBLEM_BASE + "validation"));
        body.setTitle("Bad Request");
        body.setProperty("error", "validation");
        body.setProperty("code", "VALIDATION");
        body.setProperty("message", "Learning seed profile must be SMALL");
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
