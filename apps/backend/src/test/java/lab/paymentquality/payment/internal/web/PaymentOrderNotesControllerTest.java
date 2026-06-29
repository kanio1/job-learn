package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.application.PaymentEvidenceService;
import lab.paymentquality.payment.internal.application.PaymentLifecycleService;
import lab.paymentquality.payment.internal.application.PaymentOrderListService;
import lab.paymentquality.payment.internal.application.PaymentOrderNoteService;
import lab.paymentquality.payment.internal.application.PaymentOrderService;
import lab.paymentquality.payment.internal.application.PaymentOrderSummaryService;
import lab.paymentquality.payment.internal.domain.PaymentOrderNotFoundException;
import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static lab.paymentquality.testsupport.TestJwtSupport.platformAdminToken;
import static lab.paymentquality.testsupport.TestJwtSupport.supportAgentToken;
import static lab.paymentquality.testsupport.TestJwtSupport.tokenWithRoles;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-C7: RBAC enforcement for GET/POST /api/merchants/{id}/payment-orders/{id}/notes.
 *
 * Uses @WebMvcTest (no Testcontainers, no DB) to verify:
 *   - platform admin / support agent with notes authority → 200/201
 *   - merchant reader authority → 403
 *   - unauthenticated → 401
 *   - blank body → 400
 */
@WebMvcTest(PaymentOrderController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class PaymentOrderNotesControllerTest {

    private static final UUID MERCHANT_ID = UUID.fromString("aaaaaaaa-2222-4222-8222-222222222222");
    private static final UUID ORDER_ID    = UUID.fromString("bbbbbbbb-2222-4222-8222-222222222222");
    private static final UUID NOTE_ID     = UUID.fromString("cccccccc-2222-4222-8222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean PaymentOrderService paymentOrderService;
    @MockitoBean PaymentOrderListService paymentOrderListService;
    @MockitoBean PaymentOrderSummaryService paymentOrderSummaryService;
    @MockitoBean PaymentLifecycleService paymentLifecycleService;
    @MockitoBean PaymentEvidenceService paymentEvidenceService;
    @MockitoBean PaymentOrderNoteService paymentOrderNoteService;

    private PaymentOrderNoteDto sampleNote() {
        return new PaymentOrderNoteDto(NOTE_ID, "Test note body", "platform.admin", Instant.now());
    }

    @Test
    void platformAdminCanListNotes() throws Exception {
        when(paymentOrderNoteService.listNotes(MERCHANT_ID, ORDER_ID))
                .thenReturn(List.of(sampleNote()));

        mockMvc.perform(get("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID)
                        .header("Authorization", "Bearer " + platformAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Test note body"))
                .andExpect(jsonPath("$[0].authorDisplay").value("platform.admin"));
    }

    @Test
    void supportAgentCanCreateNote() throws Exception {
        when(paymentOrderNoteService.addNote(eq(MERCHANT_ID), eq(ORDER_ID), eq("Support observation"), any()))
                .thenReturn(sampleNote());

        mockMvc.perform(post("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID)
                        .header("Authorization", "Bearer " + supportAgentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Support observation\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()));
    }

    @Test
    void merchantReaderCannotCreateNote() throws Exception {
        String merchantReadToken = tokenWithRoles("merchant.reader", List.of("merchant:payments:read"));

        mockMvc.perform(post("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID)
                        .header("Authorization", "Bearer " + merchantReadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Should not be allowed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID)
                        .header("Authorization", "Bearer " + supportAgentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whitespaceOnlyBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/merchants/{m}/payment-orders/{p}/notes", MERCHANT_ID, ORDER_ID)
                        .header("Authorization", "Bearer " + supportAgentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
