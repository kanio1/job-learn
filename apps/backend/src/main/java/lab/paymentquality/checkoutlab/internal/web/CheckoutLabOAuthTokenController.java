package lab.paymentquality.checkoutlab.internal.web;

import lab.paymentquality.checkoutlab.internal.application.CheckoutLabOAuthTokenResponse;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabOAuthTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PSP-like OAuth token stub. Wrong {@code Content-Type} returns {@code 401 Unauthorized}
 * (not {@code 415}) so clients treat credential/syntax failures uniformly.
 */
@RestController
@RequestMapping("/api/checkout-lab")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabOAuthTokenController {

    private final CheckoutLabOAuthTokenService oauthTokenService;

    CheckoutLabOAuthTokenController(CheckoutLabOAuthTokenService oauthTokenService) {
        this.oauthTokenService = oauthTokenService;
    }

    @PostMapping("/oauth/token")
    ResponseEntity<CheckoutLabOAuthTokenResponse> token(
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret) {
        return oauthTokenService.authenticateForToken(contentType, grantType, clientId, clientSecret)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
