package lab.paymentquality.eventlab.internal.web;

import lab.paymentquality.eventlab.internal.application.EventLabKafkaPublisher;
import lab.paymentquality.eventlab.internal.application.EventLabPurgeService;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/event-lab")
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
public class EventLabController {

    private final JpaEventLabProcessedRepository repository;
    private final EventLabPurgeService purgeService;
    private final EventLabKafkaPublisher publisher;

    public EventLabController(JpaEventLabProcessedRepository repository,
                              EventLabPurgeService purgeService,
                              EventLabKafkaPublisher publisher) {
        this.repository = repository;
        this.purgeService = purgeService;
        this.publisher = publisher;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.EVENT_LAB_READ + "')")
    public ResponseEntity<List<EventLabRecordDto>> list(
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String eventId,
            Authentication authentication) {
        String tenantRef = tenantRef(authentication);
        List<EventLabProcessed> rows;
        if (targetId != null && !targetId.isBlank()) {
            rows = repository.findByTargetId(targetId).stream()
                    .filter(r -> tenantRef == null || r.getTenantRef().equals(tenantRef) || isPlatformTenant(tenantRef))
                    .toList();
            if (!isPlatformTenant(tenantRef) && rows.isEmpty()) {
                // mask: return empty list not 403 contains handled via filter; spec says 404/[] for foreign tenant
                // For list we return [] (masked)
            }
        } else if (eventId != null && !eventId.isBlank()) {
            try {
                UUID eid = UUID.fromString(eventId);
                Optional<EventLabProcessed> opt = repository.findByConsumerGroupAndEventId("eventlab-inspector", eid);
                if (opt.isPresent() && canSee(opt.get(), tenantRef)) {
                    rows = List.of(opt.get());
                } else {
                    rows = List.of();
                }
            } catch (IllegalArgumentException e) {
                rows = List.of();
            }
        } else {
            // return recent for same tenant
            rows = repository.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "consumedAt"))).getContent()
                    .stream().filter(r -> canSee(r, tenantRef)).toList();
        }
        return ResponseEntity.ok(rows.stream().map(EventLabRecordDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.EVENT_LAB_READ + "')")
    public ResponseEntity<?> detail(@PathVariable UUID id, Authentication authentication) {
        Optional<EventLabProcessed> opt = repository.findById(id);
        if (opt.isEmpty()) return problem(404, "not found");
        if (!canSee(opt.get(), tenantRef(authentication))) return problem(404, "not found");
        return ResponseEntity.ok(EventLabRecordDto.from(opt.get()));
    }

    @PostMapping("/inject/duplicate")
    @PreAuthorize("hasAuthority('" + Authorities.EVENT_LAB_OPERATE + "')")
    public ResponseEntity<?> injectDuplicate(@RequestBody(required = false) Map<String, Object> body,
                                             Authentication authentication) {
        EventLabProcessed existing = requireVisibleEvent(body, authentication);
        // Re-publish the same envelope (same eventId, same key) through the real topic
        // so the producer listener, retry budget and idempotency are exercised.
        publisher.publishDuplicate(existing.getEventId(),
                existing.getTargetId(), existing.getTenantRef(),
                existing.getConsumerGroup(), existing.getAction(), existing.getTargetType());
        return ResponseEntity.status(201).body(EventLabRecordDto.from(existing));
    }

    @PostMapping("/inject/poison")
    @PreAuthorize("hasAuthority('" + Authorities.EVENT_LAB_OPERATE + "')")
    public ResponseEntity<?> injectPoison(@RequestBody(required = false) Map<String, Object> body,
                                          Authentication authentication) {
        EventLabProcessed existing = requireVisibleEvent(body, authentication);
        // Publish a poisoned envelope through the real topic; the listener's poison path
        // routes it through retry -> DLT -> dead-letter handler, producing the DEAD row.
        // No direct DB write happens here.
        publisher.publishPoison(existing.getEventId(),
                existing.getTargetId(), existing.getTenantRef(),
                existing.getConsumerGroup(), existing.getAction(), existing.getTargetType());
        return ResponseEntity.status(201).body(EventLabRecordDto.from(existing));
    }

    private EventLabProcessed requireVisibleEvent(Map<String, Object> body, Authentication authentication) {
        if (body == null || !body.containsKey("eventId")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId required");
        }
        String eventIdStr = String.valueOf(body.get("eventId"));
        UUID eventId;
        try {
            eventId = UUID.fromString(eventIdStr);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid eventId");
        }
        Optional<EventLabProcessed> existing = repository.findByConsumerGroupAndEventId("eventlab-inspector", eventId);
        if (existing.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "eventId not found");
        if (!canSee(existing.get(), tenantRef(authentication))) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return existing.get();
    }

    @PostMapping("/purge")
    @PreAuthorize("hasAuthority('" + Authorities.EVENT_LAB_OPERATE + "')")
    public ResponseEntity<?> purge(@RequestParam(defaultValue = "30") int olderThanDays) {
        int deleted = purgeService.purgeOlderThanDays(olderThanDays);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    private static ResponseEntity<?> problem(int status, String detail) {
        Map<String, Object> body = Map.of(
                "type", "about:blank",
                "title", status == 404 ? "Not Found" : status == 400 ? "Bad Request" : "Error",
                "status", status,
                "detail", detail);
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(body);
    }

    private static String tenantRef(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String t = jwt.getToken().getClaimAsString("tenant_id");
            if (t != null && !t.isBlank()) return t.strip();
        }
        return "PLATFORM_TENANT";
    }

    private static boolean isPlatformTenant(String tenantRef) {
        return "PLATFORM_TENANT".equals(tenantRef);
    }

    private static boolean canSee(EventLabProcessed row, String requesterTenant) {
        if (isPlatformTenant(requesterTenant)) return true;
        return row.getTenantRef().equals(requesterTenant);
    }
}
