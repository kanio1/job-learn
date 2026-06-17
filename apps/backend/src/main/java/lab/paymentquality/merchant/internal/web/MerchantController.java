package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.shared.security.Authorities;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody CreateMerchantRequest request) {
        var merchant = merchantService.create(request.merchantReference(), request.displayName());
        var response = MerchantMapper.toResponse(merchant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_READ + "')")
    public ResponseEntity<MerchantResponse> getById(@PathVariable String id) {
        UUID uuid = parseUUID(id);
        var response = merchantService.findById(uuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_READ + "')")
    public ResponseEntity<MerchantListResponse> list() {
        var merchants = merchantService.listFirstPage();
        return ResponseEntity.ok(new MerchantListResponse(merchants));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_STATUS + "')")
    public ResponseEntity<MerchantResponse> activate(@PathVariable String id) {
        UUID uuid = parseUUID(id);
        var response = merchantService.activate(uuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_STATUS + "')")
    public ResponseEntity<MerchantResponse> suspend(@PathVariable String id) {
        UUID uuid = parseUUID(id);
        var response = merchantService.suspend(uuid);
        return ResponseEntity.ok(response);
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid merchant ID: " + id, e);
        }
    }
}
