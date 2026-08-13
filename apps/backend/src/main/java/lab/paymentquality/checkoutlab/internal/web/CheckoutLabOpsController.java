package lab.paymentquality.checkoutlab.internal.web;

import lab.paymentquality.checkoutlab.internal.application.CheckoutLabClock;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabReconcileService;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabResetService;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutAnomaly;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout-lab")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabOpsController {

    public record ClockRequest(String instant) {
    }

    public record AnomalyResponse(UUID anomalyId, UUID sessionId, String kind, String detail, Instant detectedAt) {
        static AnomalyResponse from(CheckoutAnomaly anomaly) {
            return new AnomalyResponse(
                    anomaly.getAnomalyId(),
                    anomaly.getSessionId(),
                    anomaly.getKind(),
                    anomaly.getDetail(),
                    anomaly.getDetectedAt());
        }
    }

    private final CheckoutLabClock clock;
    private final CheckoutLabResetService resetService;
    private final CheckoutLabReconcileService reconcileService;

    CheckoutLabOpsController(
            CheckoutLabClock clock,
            CheckoutLabResetService resetService,
            CheckoutLabReconcileService reconcileService) {
        this.clock = clock;
        this.resetService = resetService;
        this.reconcileService = reconcileService;
    }

    @PostMapping("/clock")
    ResponseEntity<Map<String, String>> setClock(@RequestBody ClockRequest request) {
        clock.setFixed(Instant.parse(request.instant()));
        return ResponseEntity.ok(Map.of("instant", clock.instant().toString()));
    }

    @PostMapping("/reset")
    ResponseEntity<Map<String, String>> reset() {
        resetService.reset();
        return ResponseEntity.ok(Map.of("status", "reset"));
    }

    @PostMapping("/reconcile")
    ResponseEntity<Map<String, Integer>> reconcile() {
        return ResponseEntity.ok(Map.of("created", reconcileService.reconcile()));
    }

    @GetMapping("/anomalies")
    ResponseEntity<List<AnomalyResponse>> anomalies() {
        return ResponseEntity.ok(reconcileService.list().stream().map(AnomalyResponse::from).toList());
    }
}
