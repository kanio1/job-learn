package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantControllerTest {

    private final MerchantService service = mock(MerchantService.class);
    private final MerchantController controller = new MerchantController(service);

    @Test
    void createReturnsCreatedResponse() {
        var merchant = Merchant.create(UUID.randomUUID(), "MERCH-001", "Merchant One");
        when(service.create("MERCH-001", "Merchant One")).thenReturn(merchant);

        var response = controller.create(new CreateMerchantRequest("MERCH-001", "Merchant One"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DRAFT");
    }

    @Test
    void malformedUuidThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.getById("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownMerchantPropagatesNotFound() {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new MerchantNotFoundException(id.toString()));

        assertThatThrownBy(() -> controller.getById(id.toString()))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
