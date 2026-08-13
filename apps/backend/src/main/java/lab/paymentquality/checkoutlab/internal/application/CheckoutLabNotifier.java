package lab.paymentquality.checkoutlab.internal.application;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabNotifier {

    private static final Logger log = LoggerFactory.getLogger(CheckoutLabNotifier.class);

    private final CheckoutLabSignatureService signatureService;
    private final CheckoutLabScenarioService scenarioService;
    private final CheckoutLabClock clock;
    private final CheckoutLabDeliveryLog deliveryLog;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public CheckoutLabNotifier(
            CheckoutLabSignatureService signatureService,
            CheckoutLabScenarioService scenarioService,
            CheckoutLabClock clock,
            CheckoutLabDeliveryLog deliveryLog,
            ObjectMapper objectMapper) {
        this.signatureService = signatureService;
        this.scenarioService = scenarioService;
        this.clock = clock;
        this.deliveryLog = deliveryLog;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public void emit(CheckoutSession session, CheckoutSessionStatus status, String eventType) {
        if (scenarioService.skipNotify(session.getSessionId())) {
            log.info(
                    "Skipping notify for scenario return_lie_success sessionId={} correlationId={}",
                    session.getSessionId(),
                    session.getCorrelationId());
            return;
        }
        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "");
        long created = clock.instant().getEpochSecond();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", eventId);
        envelope.put("type", eventType);
        envelope.put("created", created);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getSessionId().toString());
        data.put("status", status.name());
        data.put("amountMinor", session.getAmountMinor());
        data.put("currency", session.getCurrency());
        data.put("extOrderId", session.getExtOrderId());
        envelope.put("data", data);

        byte[] rawBody = writeJson(envelope);
        long timestamp = created;
        String signature = signatureService.sign(timestamp, rawBody);
        if (scenarioService.shouldSignIncorrectly(session.getSessionId())) {
            signature = "t=" + timestamp + ",v1=" + "00".repeat(32);
        }

        postWithRetry(session, eventId, rawBody, signature);
    }

    private void postWithRetry(CheckoutSession session, String eventId, byte[] rawBody, String signature) {
        int attempt = 0;
        while (true) {
            attempt++;
            int status = postOnce(session, eventId, rawBody, signature);
            deliveryLog.record(session.getSessionId(), eventId, attempt, status, clock.instant());
            if (status == 400) {
                return;
            }
            if (status == 202 || status == 200) {
                return;
            }
            if (status == 503 && attempt < 3) {
                sleepQuietly(attempt == 1 ? 100L : 200L);
                continue;
            }
            return;
        }
    }

    private int postOnce(CheckoutSession session, String eventId, byte[] rawBody, String signature) {
        try {
            return restClient.post()
                    .uri(session.getNotifyUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Lab-Event-Id", eventId)
                    .header("Lab-Signature", signature)
                    .header("X-Correlation-ID", session.getCorrelationId())
                    .body(rawBody)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
        } catch (RestClientResponseException ex) {
            return ex.getStatusCode().value();
        } catch (RuntimeException ex) {
            log.warn(
                    "Notify POST failed sessionId={} eventId={} correlationId={} reason={}",
                    session.getSessionId(),
                    eventId,
                    session.getCorrelationId(),
                    ex.getMessage());
            return 503;
        }
    }

    private byte[] writeJson(Map<String, Object> envelope) {
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Cannot serialize checkout notify envelope", ex);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unused")
    private static String utf8(byte[] rawBody) {
        return new String(rawBody, StandardCharsets.UTF_8);
    }
}
