package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantNotFoundException;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantControllerTest {

    private final MerchantService service = mock(MerchantService.class);
    private final TenantResolver tenantResolver = mock(TenantResolver.class);
    private final MerchantController controller = new MerchantController(service, tenantResolver);
    private final Jwt jwt = mock(Jwt.class);
    private final TenantContext tenantContext = new TenantContext(
            UUID.randomUUID(),
            TenantReference.of("TENANT_ALPHA"),
            false);

    @Test
    void createReturnsCreatedResponse() {
        var merchant = Merchant.create(UUID.randomUUID(), "MERCH-001", "Merchant One");
        when(tenantResolver.resolve(jwt)).thenReturn(tenantContext);
        when(service.create("MERCH-001", "Merchant One", tenantContext, null)).thenReturn(merchant);

        var response = controller.create(new CreateMerchantRequest("MERCH-001", "Merchant One"), jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getCacheControl()).contains("no-transform");
        assertThat(response.getHeaders().getETag()).isEqualTo("\"v0\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DRAFT");
    }

    @Test
    void malformedUuidThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.getById("not-a-uuid", jwt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownMerchantPropagatesNotFound() {
        UUID id = UUID.randomUUID();
        when(tenantResolver.resolve(jwt)).thenReturn(tenantContext);
        when(service.findById(id, tenantContext)).thenThrow(new MerchantNotFoundException(id.toString()));

        assertThatThrownBy(() -> controller.getById(id.toString(), jwt))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
