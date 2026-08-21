package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.internal.application.OpsFeedService;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ops/feed")
public class OpsFeedController {

    private final OpsFeedService opsFeedService;

    public OpsFeedController(OpsFeedService opsFeedService) {
        this.opsFeedService = opsFeedService;
    }

    @PostMapping("/inject")
    @PreAuthorize("hasAuthority('" + Authorities.OPS_INJECT + "')")
    public ResponseEntity<InjectFeedResponse> inject(
            @RequestBody(required = false) InjectFeedRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opsFeedService.inject(request, jwt.getSubject()));
    }

    @PostMapping("/disconnect-me")
    @PreAuthorize("hasAuthority('" + Authorities.OPS_FEED + "')")
    public ResponseEntity<Void> disconnectMe(@AuthenticationPrincipal Jwt jwt) {
        opsFeedService.disconnect(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('" + Authorities.OPS_FEED + "')")
    public OpsFeedRecentResponse recent(@AuthenticationPrincipal Jwt jwt) {
        return new OpsFeedRecentResponse(opsFeedService.recent(merchantIdClaim(jwt)));
    }

    private static UUID merchantIdClaim(Jwt jwt) {
        String claim = jwt.getClaimAsString("merchant_id");
        if (claim == null || claim.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(claim.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
