package lab.paymentquality.payment.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.payment.internal.application.PaymentActorContext;
import lab.paymentquality.payment.internal.application.PaymentCreateResult;
import lab.paymentquality.payment.internal.application.PaymentOrderListService;
import lab.paymentquality.payment.internal.application.PaymentOrderSummaryService;
import lab.paymentquality.payment.internal.application.PaymentOrderService;
import lab.paymentquality.payment.internal.application.PaymentLifecycleService;
import lab.paymentquality.payment.internal.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderListService paymentOrderListService;
    private final PaymentOrderSummaryService paymentOrderSummaryService;
    private final PaymentLifecycleService paymentLifecycleService;

    public PaymentOrderController(PaymentOrderService paymentOrderService,
                                   PaymentOrderListService paymentOrderListService,
                                   PaymentOrderSummaryService paymentOrderSummaryService,
                                   PaymentLifecycleService paymentLifecycleService) {
        this.paymentOrderService = paymentOrderService;
        this.paymentOrderListService = paymentOrderListService;
        this.paymentOrderSummaryService = paymentOrderSummaryService;
        this.paymentLifecycleService = paymentLifecycleService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
            @PathVariable UUID merchantId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PaymentAmount amount = PaymentAmount.of(request.amountMinor());
        CurrencyCode currency = CurrencyCode.of(request.currency());
        ClientOrderReference clientRef = ClientOrderReference.of(request.clientOrderReference());
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);

        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
        PaymentActorContext actor = new PaymentActorContext(jwt.getSubject());

        PaymentCreateResult result = paymentOrderService.create(
                merchantId, amount, currency, clientRef, idempotencyKey, actor, PaymentHttpHeaders.correlationId());

        PaymentOrderResponse response = PaymentOrderMapper.toResponse(result.paymentOrder());
        String etag = PaymentEtag.from(result.paymentOrder());

        if (result.created()) {
            URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/" + result.paymentOrder().getPaymentOrderId());
            return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location),
                            PaymentHttpHeaders.VARY_AUTHORIZATION_IDEMPOTENCY_KEY)
                    .header("ETag", etag)
                    .body(response);
        }

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION_IDEMPOTENCY_KEY)
                .header("ETag", etag)
                .body(response);
    }

    @GetMapping(value = "/{paymentOrderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderResponse> getPaymentOrder(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("platform:payments:read"));

        PaymentOrder order;
        if (isPlatformReader) {
            order = paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        } else {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new PaymentOrderNotFoundException(paymentOrderId);
            }
            order = paymentOrderService.findForMerchant(merchantId, paymentOrderId);
        }

        PaymentOrderResponse response = PaymentOrderMapper.toResponse(order);
        String etag = PaymentEtag.from(order);

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header("ETag", etag)
                .body(response);
    }

    @RequestMapping(value = "/{paymentOrderId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headPaymentOrder(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        PaymentOrder order = findReadablePaymentOrder(merchantId, paymentOrderId, authentication, jwt);

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header("ETag", PaymentEtag.from(order))
                .build();
    }

    @RequestMapping(value = "/{paymentOrderId}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsPaymentOrder(@PathVariable UUID merchantId, @PathVariable UUID paymentOrderId) {
        return ResponseEntity.noContent()
                .allow(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.PATCH, HttpMethod.OPTIONS)
                .header(PaymentHttpHeaders.ACCEPT_PATCH, PaymentHttpHeaders.MERGE_PATCH_JSON)
                .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                .build();
    }

    @RequestMapping(value = "/{paymentOrderId}/{action:authorize|capture|cancel|refund}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsLifecycleAction(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @PathVariable String action) {
        return ResponseEntity.noContent()
                .allow(HttpMethod.POST, HttpMethod.OPTIONS)
                .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                .build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderListResponse> listPaymentOrders(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long minAmount,
            @RequestParam(required = false) Long maxAmount,
            @RequestParam(required = false) String clientOrderReference,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("platform:payments:read"));

        if (!isPlatformReader) {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new AccessDeniedException("Merchant scope mismatch");
            }
        }

        PaymentOrderListRequest request = new PaymentOrderListRequest(
                status, currency, fromDate, toDate, minAmount, maxAmount,
                clientOrderReference, page, size, sort);

        request.validate();

        Page<PaymentOrder> pageResult = paymentOrderListService.findAll(merchantId, request);
        PaymentOrderListResponse response = PaymentOrderListMapper.toListResponse(pageResult);

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(response);
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderSummaryResponse> summarizePaymentOrders(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("platform:payments:read"));

        if (!isPlatformReader) {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new AccessDeniedException("Merchant scope mismatch");
            }
        }

        PaymentOrderSummaryRequest request = new PaymentOrderSummaryRequest(
                currency,
                status,
                fromDate,
                toDate
        );

        PaymentOrderSummaryResponse response = paymentOrderSummaryService.summarize(merchantId, request);

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(response);
    }

    private boolean isPlatformLifecycle(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("platform:payments:lifecycle"));
    }

    private void verifyMerchantOwnership(UUID merchantId, Jwt jwt, Authentication authentication) {
        if (isPlatformLifecycle(authentication)) {
            return;
        }
        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
    }

    private PaymentOrder findReadablePaymentOrder(UUID merchantId, UUID paymentOrderId,
                                                   Authentication authentication, Jwt jwt) {
        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("platform:payments:read"));

        if (isPlatformReader) {
            return paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        }
        String jwtMerchantId = jwt.getClaimAsString("merchant_id");
        if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
            throw new PaymentOrderNotFoundException(paymentOrderId);
        }
        return paymentOrderService.findForMerchant(merchantId, paymentOrderId);
    }

    private ResponseEntity<PaymentLifecycleResponse> lifecycleResponse(PaymentOrder order) {
        PaymentLifecycleResponse body = PaymentOrderMapper.toLifecycleResponse(order);
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION_IF_MATCH)
                .header("ETag", PaymentEtag.from(order))
                .body(body);
    }

    @PostMapping(value = "/{paymentOrderId}/authorize", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> authorize(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) AuthorizeRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);
        String correlationId = PaymentHttpHeaders.correlationId();

        PaymentOrder order = paymentLifecycleService.authorize(
                merchantId, paymentOrderId,
                request != null ? request.reason() : null,
                idempotencyKey.keyHash(), expectedVersion, jwt.getSubject(), correlationId);

        return lifecycleResponse(order);
    }

    @PostMapping(value = "/{paymentOrderId}/capture", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> capture(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) CaptureRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);
        String correlationId = PaymentHttpHeaders.correlationId();

        PaymentOrder order = paymentLifecycleService.capture(
                merchantId, paymentOrderId,
                request != null ? request.amountMinor() : null,
                request != null ? request.reason() : null,
                idempotencyKey.keyHash(), expectedVersion, jwt.getSubject(), correlationId);

        return lifecycleResponse(order);
    }

    @PostMapping(value = "/{paymentOrderId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> cancel(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) CancelRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);
        String correlationId = PaymentHttpHeaders.correlationId();

        PaymentOrder order = paymentLifecycleService.cancel(
                merchantId, paymentOrderId,
                request != null ? request.reason() : null,
                idempotencyKey.keyHash(), expectedVersion, jwt.getSubject(), correlationId);

        return lifecycleResponse(order);
    }

    @PostMapping(value = "/{paymentOrderId}/refund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> refund(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) RefundRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);
        String correlationId = PaymentHttpHeaders.correlationId();

        PaymentOrder order = paymentLifecycleService.refund(
                merchantId, paymentOrderId,
                request != null ? request.amountMinor() : null,
                request != null ? request.reason() : null,
                idempotencyKey.keyHash(), expectedVersion, jwt.getSubject(), correlationId);

        return lifecycleResponse(order);
    }

    @PatchMapping(value = "/{paymentOrderId}", consumes = {PaymentHttpHeaders.MERGE_PATCH_JSON, MediaType.APPLICATION_JSON_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> updateMetadata(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody MetadataPatchRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        String metadataJson = request.metadata() != null ? request.metadata().toString() : null;
        PaymentOrder order = paymentLifecycleService.updateMetadata(merchantId, paymentOrderId, metadataJson, expectedVersion);

        return lifecycleResponse(order);
    }

    @GetMapping(value = "/{paymentOrderId}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentStatusHistoryResponse> getHistory(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("platform:payments:read")
                        || a.getAuthority().equals("platform:payments:lifecycle")
                        || a.getAuthority().equals("platform:payments:audit"));

        if (!isPlatformReader) {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new AccessDeniedException("Merchant scope mismatch");
            }
        }

        List<PaymentOrderStatusHistory> entries = paymentLifecycleService.findHistory(merchantId, paymentOrderId);
        PaymentStatusHistoryResponse response = PaymentOrderMapper.toHistoryResponse(entries);

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(response);
    }

}
