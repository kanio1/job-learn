package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.internal.application.OpsNotificationService;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class OpsNotificationController {

    private final OpsNotificationService notificationService;

    public OpsNotificationController(OpsNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.NOTIFICATIONS_READ + "')")
    public NotificationListResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly) {
        return new NotificationListResponse(
                notificationService.list(jwt.getSubject(), unreadOnly).stream()
                        .map(NotificationResponse::from)
                        .toList());
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAuthority('" + Authorities.NOTIFICATIONS_READ + "')")
    public NotificationResponse get(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return NotificationResponse.from(notificationService.getOwned(notificationId, jwt.getSubject()));
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('" + Authorities.NOTIFICATIONS_READ + "')")
    public NotificationResponse markRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return NotificationResponse.from(notificationService.markRead(notificationId, jwt.getSubject()));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAuthority('" + Authorities.NOTIFICATIONS_READ + "')")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
