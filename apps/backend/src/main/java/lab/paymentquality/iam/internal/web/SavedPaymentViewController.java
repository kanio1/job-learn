package lab.paymentquality.iam.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.iam.internal.application.SavedPaymentViewService;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/payment-views")
public class SavedPaymentViewController {

    private final SavedPaymentViewService service;

    public SavedPaymentViewController(SavedPaymentViewService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public SavedPaymentViewListResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new SavedPaymentViewListResponse(
                service.list(jwt.getSubject()).stream()
                        .map(SavedPaymentViewResponse::from)
                        .toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public ResponseEntity<SavedPaymentViewResponse> create(
            @Valid @RequestBody CreateSavedPaymentViewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        SavedPaymentViewResponse body = SavedPaymentViewResponse.from(service.create(jwt.getSubject(), request));
        return ResponseEntity
                .created(URI.create("/api/users/me/payment-views/" + body.id()))
                .body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public SavedPaymentViewResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSavedPaymentViewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return SavedPaymentViewResponse.from(service.update(jwt.getSubject(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public SavedPaymentViewResponse setDefault(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return SavedPaymentViewResponse.from(service.setDefault(jwt.getSubject(), id));
    }
}
