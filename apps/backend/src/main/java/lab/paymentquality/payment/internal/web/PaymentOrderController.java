package lab.paymentquality.payment.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.payment.internal.application.PaymentActorContext;
import lab.paymentquality.payment.internal.application.PaymentCreateResult;
import lab.paymentquality.payment.internal.application.PaymentEvidenceService;
import lab.paymentquality.payment.internal.application.PaymentOrderListService;
import lab.paymentquality.payment.internal.application.PaymentOrderNoteService;
import lab.paymentquality.payment.internal.application.PaymentOrderSummaryService;
import lab.paymentquality.payment.internal.application.PaymentOrderService;
import lab.paymentquality.payment.internal.application.PaymentLifecycleService;
import lab.paymentquality.payment.internal.domain.*;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderListService paymentOrderListService;
    private final PaymentOrderSummaryService paymentOrderSummaryService;
    private final PaymentLifecycleService paymentLifecycleService;
    private final PaymentEvidenceService paymentEvidenceService;
    private final PaymentOrderNoteService paymentOrderNoteService;

    public PaymentOrderController(PaymentOrderService paymentOrderService,
                                   PaymentOrderListService paymentOrderListService,
                                   PaymentOrderSummaryService paymentOrderSummaryService,
                                   PaymentLifecycleService paymentLifecycleService,
                                   PaymentEvidenceService paymentEvidenceService,
                                   PaymentOrderNoteService paymentOrderNoteService) {
        this.paymentOrderService = paymentOrderService;
        this.paymentOrderListService = paymentOrderListService;
        this.paymentOrderSummaryService = paymentOrderSummaryService;
        this.paymentLifecycleService = paymentLifecycleService;
        this.paymentEvidenceService = paymentEvidenceService;
        this.paymentOrderNoteService = paymentOrderNoteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
            @PathVariable UUID merchantId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }

        request.requireKnownTopLevelFieldsOnly();
        PaymentAmount amount = PaymentAmount.of(request.amountMinor());
        CurrencyCode currency = CurrencyCode.of(request.currency());
        ClientOrderReference clientRef = ClientOrderReference.of(request.clientOrderReference());
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);

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
                    .header("Idempotency-Replayed", "false")
                    .body(response);
        }

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION_IDEMPOTENCY_KEY)
                .header("ETag", etag)
                .header("Idempotency-Replayed", "true")
                .body(response);
    }

    @GetMapping(value = "/{paymentOrderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentOrderResponse> getPaymentOrder(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
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

        String etag = PaymentEtag.from(order);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                    .header("Cache-Control", "no-store")
                    .header("Vary", PaymentHttpHeaders.VARY_AUTHORIZATION)
                    .build();
        }

        PaymentOrderResponse response = PaymentOrderMapper.toResponse(order);
        String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(order.getUpdatedAt().atOffset(ZoneOffset.UTC));

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header("ETag", etag)
                .header("Last-Modified", lastModified)
                .body(response);
    }

    @RequestMapping(value = "/{paymentOrderId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headPaymentOrder(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        PaymentOrder order = findReadablePaymentOrder(merchantId, paymentOrderId, authentication, jwt);
        String etag = PaymentEtag.from(order);
        String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(order.getUpdatedAt().atOffset(ZoneOffset.UTC));

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                    .header("Cache-Control", "no-store")
                    .header("Vary", PaymentHttpHeaders.VARY_AUTHORIZATION)
                    .header("Last-Modified", lastModified)
                    .build();
        }

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header("ETag", etag)
                .header("Last-Modified", lastModified)
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

    @RequestMapping(value = "/{paymentOrderId}/evidence", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsEvidence(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId) {
        return ResponseEntity.noContent()
                .allow(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS)
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

    @GetMapping(value = "/export", produces = "text/csv;charset=utf-8")
    public ResponseEntity<String> exportPaymentOrdersCsv(
            @PathVariable UUID merchantId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("platform:payments:read"));

        if (!isPlatformReader) {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new AccessDeniedException("Merchant scope mismatch");
            }
        }

        List<PaymentOrder> orders = paymentOrderListService.findAllForExport(merchantId);
        String csv = PaymentOrderCsvExporter.toCsv(orders);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"payment-orders-" + merchantId + ".csv\"")
                .header("Cache-Control", "no-store")
                .header("Vary", PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                .body(csv);
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
        request.requireOnlyMetadataTopLevelField();
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        String metadataJson = request.metadataAsJson();
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

    @PostMapping(value = "/{paymentOrderId}/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentEvidenceResponse> uploadEvidence(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyMerchantOwnership(merchantId, jwt, authentication);
        PaymentOrderEvidence evidence = paymentEvidenceService.uploadForOrder(merchantId, paymentOrderId, file);
        PaymentEvidenceResponse response = PaymentEvidenceMapper.toResponse(evidence);
        URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/"
                + paymentOrderId + "/evidence/" + evidence.getEvidenceId());

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(response);
    }

    @GetMapping(value = "/{paymentOrderId}/evidence", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentEvidenceResponse.ListResponse> listEvidence(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        findReadablePaymentOrder(merchantId, paymentOrderId, authentication, jwt);
        List<PaymentEvidenceResponse> content = paymentEvidenceService.listForOrder(merchantId, paymentOrderId)
                .stream()
                .map(PaymentEvidenceMapper::toResponse)
                .toList();

        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(new PaymentEvidenceResponse.ListResponse(content));
    }

    @GetMapping(value = "/{paymentOrderId}/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Authorities.PLATFORM_PAYMENT_NOTES_READ + "')")
    public ResponseEntity<List<PaymentOrderNoteDto>> listNotes(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId) {

        List<PaymentOrderNoteDto> notes = paymentOrderNoteService.listNotes(merchantId, paymentOrderId);
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(notes);
    }

    @PostMapping(value = "/{paymentOrderId}/notes", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Authorities.PLATFORM_PAYMENT_NOTES_CREATE + "')")
    public ResponseEntity<PaymentOrderNoteDto> addNote(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String authorDisplay = jwt.getSubject();
        PaymentOrderNoteDto note = paymentOrderNoteService.addNote(
                merchantId, paymentOrderId, request.body(), authorDisplay);

        URI location = URI.create("/api/merchants/" + merchantId
                + "/payment-orders/" + paymentOrderId + "/notes/" + note.id());
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(note);
    }

}
